#!/usr/bin/env python3

import os
import time
import argparse
import socket
import threading
import logging
import subprocess

import libdlt
import ferry.settings as settings
from ferry.settings import UNIS_URL, LOCAL_UNIS_HOST, LOCAL_UNIS_PORT
from asyncio import TimeoutError
from unis.models import Node, schemaLoader
from unis.runtime import Runtime
from unis.exceptions import ConnectionError
from ferry.gps import GPS
from ferry.ibp_iface import IBPWatcher
from ferry.watcher import UploadWatcher
from ferry.log import log

# globals
DOWNLOAD_DIR=settings.DOWNLOAD_DIR
UPLOAD_DIR=settings.UPLOAD_DIR
sess = None

DLNFerry = schemaLoader.get_class(settings.FERRY_SERVICE)
GeoLoc = schemaLoader.get_class(settings.GEOLOC)

# Global node and service objects
# so they can be refreshed if base resets
n = None
s = None

def register(rt, name, fqdn):
    def do_register(rt, name, fqdn):
        global n
        global s

        log.info("Registering Ferry Node")
        
        n = rt.nodes.where({"name": name})
        try:
            n = next(n)
        except StopIteration:
            n = Node()
            n.name = name
            rt.insert(n, commit=True)
            rt.flush()
        
        s = rt.services.where({"runningOn": n})
        try:
            s = next(s)
        except StopIteration:
            s = DLNFerry()
            s.runningOn = n
            s.serviceType="datalogistics:wdln:ferry"
            s.name = name
            s.accessPoint = "ibp://{}:6714".format(fqdn)
            s.unis_url = "http://{}:{}".format(fqdn, LOCAL_UNIS_PORT)
            s.status = "READY"
            s.ttl = 600 # 10m
            rt.insert(s, commit=True)
            rt.flush()

    # simply update the timestamps on our node and service resources
    def touch(rt, name, fqdn, gps):
        rcount = 0
        while True:
            time.sleep(settings.UPDATE_INTERVAL)
            try:
                (lat, lon) = gps.query()
                if lat and lon:
                    n.location.latitude = lat
                    n.location.longitude = lon
                    rt.flush()
                s.touch()

            except (ConnectionError, TimeoutError) as exp:
                #import traceback
                #traceback.print_exc()
                log.error("Could not update node/service resources: {}".format(exp))
                if rcount >= settings.RETRY_COUNT:
                    rt.delete(s)
                    do_register(rt, name, fqdn)
                    rcount = 0
                else:
                    rcount = rcount + 1

    # make sure we get an initial node and service object
    while not n or not s:
        do_register(rt, name, fqdn)
        time.sleep(settings.UPDATE_INTERVAL)
    
    gps = GPS()
    th = threading.Thread(
        name='toucher',
        target=touch,
        daemon=True,
        args=(rt, name, fqdn, gps),
    )
    th.start()
   
def init_runtime(remote, local, local_only):
    while True:
        try:
            opts = {"cache": { "preload": ["nodes", "services"] }, "proxy": { "defer_update": True }}
            if local_only:
                urls = [{"default": True, "url": local}]
                log.debug("Connecting to UNIS instance(s): {}".format(local))
            else:
                urls = [{"url": local}, {"default": True, "url": remote}]
                log.debug("Connecting to UNIS instance(s): {}".format(remote+','+local))
            rt = Runtime(urls, **opts)
            if local_only:
                rt.exnodes.addCallback(file_cb)
            return rt
        except (ConnectionError, TimeoutError) as exp:
            log.warn("Could not contact UNIS servers {}, retrying...".format(urls))
        time.sleep(5)

def file_cb(ex, event):
    if event == "new":
        time.sleep(2)
        local_download(sess, [ex])

def local_download(sess, exnodes):
    for f in exnodes:
        if not len(f.extents):
            continue
        fpath = os.path.join(DOWNLOAD_DIR, f.name)
        if os.path.exists(fpath) and os.path.getsize(fpath) == f.size:
            log.debug("File exists: {}, skipping!".format(f.name))
            continue
        log.info("Downloading: {} ({} bytes)".format(f.name, f.size))
        try:
            result = sess.download(f.selfRef, fpath)
            res, diff, dsize = result.exnode, result.time, result.t_size
        except Exception as e:
            log.error("Could not download file: {}".format(e))
            continue
        if dsize != res.size:
            log.warn("WARNING: {}: transferred {} of {} bytes \
            (check depot file)".format(res.name,
                                       dsize,
                                       res.size))
        else:
            log.info("{0} ({1} {2:.2f} MB/s) {3}".format(res.name, res.size,
                                                         res.size/1e6/diff,
                                                         res.selfRef))
            
def run_local(sess, rt):
    i=0
    while True:
        (i%5) or log.info("Waiting for some local action...")
        i+=1
        time.sleep(1)
        
def run_remote(sess, rt):
    i=0
    while True:
        (i%5) or log.info("[{}]Waiting for some remote action...".format(s.status))
        if s.status == "UPDATE":
            dl_list = s.new_exnodes
            log.info("Caught UPDATE status with {} new exnodes".format(len(dl_list)))
            local_download(sess, dl_list)
            time.sleep(1)
            s.status = "READY"
            rt.flush()
        i+=1
        time.sleep(1)
        
def main():
    global DOWNLOAD_DIR
    global sess
    
    parser = argparse.ArgumentParser(description="DLN Mobile Ferry Agent")
    parser.add_argument('-H', '--host', type=str, default=UNIS_URL,
                        help='UNIS instance for registration and metadata')
    parser.add_argument('-n', '--name', type=str, default=None,
                        help='Set ferry node name (ignore system hostname)')
    parser.add_argument('-d', '--download', type=str, default=DOWNLOAD_DIR,
                        help='Set local download directory')
    parser.add_argument('-u', '--upload', type=str, default=UPLOAD_DIR,
                        help='Set local upload directory')
    parser.add_argument('-l', '--local', action='store_true',
                        help='Run using only local UNIS instance (on-ferry)')
    parser.add_argument('-i', '--ibp', action='store_true',
                        help='Update IBP config to reflect interface changes on system')
    parser.add_argument('-v', '--verbose', action='store_true',
                        help='Produce verbose output from the script')
    parser.add_argument('-q', '--quiet', action='store_true',
                        help='Quiet mode, no logging output')

    args = parser.parse_args()

    # configure logging level
    level = logging.DEBUG if args.verbose else logging.INFO
    level = logging.CRITICAL if args.quiet else level
    log.setLevel(level)
    
    name = socket.gethostname()
    fqdn = socket.getfqdn()
    log.info("Ferry \"{}\" reporting for duty".format(name))
    if args.name:
        name = args.name
        log.info("Setting ferry name to \"{}\"".format(name))

    DOWNLOAD_DIR = args.download
    try:
        os.makedirs(DOWNLOAD_DIR)
    except FileExistsError:
        pass
    except OSError as exp:
        raise exp

    # use fqdn to determine local endpoints
    LOCAL_DEPOT={"ibp://{}:6714".format(fqdn): { "enabled": True}}

    # allow an alternative UNIS instance (non-ferry) in local mode
    if (args.local and args.host != UNIS_URL):
        LOCAL_UNIS=args.host
    else:
        LOCAL_UNIS = "http://{}:{}".format(fqdn, LOCAL_UNIS_PORT)
    
    # get our initial UNIS-RT and libdlt sessions
    rt = init_runtime(args.host, LOCAL_UNIS, args.local)
    sess = libdlt.Session(rt, bs="5m", depots=LOCAL_DEPOT, threads=1)    
    
    # Start the registration loop
    register(rt, name, fqdn)

    # Start the iface watcher for IBP config
    if args.ibp:
        IBPWatcher()

    # Start the upload dir watcher
    UploadWatcher(s, LOCAL_UNIS)
        
    # run our main loop
    if args.local:
        run_local(sess, rt)
    else:
        run_remote(sess, rt)
    
if __name__ == "__main__":
    main()
    
