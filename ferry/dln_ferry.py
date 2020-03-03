#!/usr/bin/env python3

import bottle
import os
import time
import argparse
import socket
import threading
import logging
import subprocess

import libdlt
import ferry.settings as settings
from ferry.config import MultiConfig
from asyncio import TimeoutError
from unis.models import Node, schemaLoader
from unis.runtime import Runtime
from unis.exceptions import ConnectionError
from ferry.gps import GPS
from ferry.ibp_iface import IBPWatcher
from ferry.log import log

# globals
DOWNLOAD_DIR=settings.DOWNLOAD_DIR
UPLOAD_DIR=settings.UPLOAD_DIR
LOCAL_UNIS_PORT=settings.LOCAL_UNIS_PORT
sess = None

DLNFerry = schemaLoader.get_class(settings.FERRY_SERVICE)
GeoLoc = schemaLoader.get_class(settings.GEOLOC)

# Global node and service objects
# so they can be refreshed if UNIS resets
n = None
s = None
slock = threading.Lock()

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
        except (StopIteration, AttributeError):
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
                    slock.acquire()
                    rt.delete(s)
                    do_register(rt, name, fqdn)
                    slock.release()
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

def run_uploader(d, port):
    @bottle.route('/flist')
    def _list():
        return repr(os.listdir(d))
    @bottle.route('/upload', method='POST')
    def _files():
        for f in bottle.request.files.keys():
            dat = bottle.request.files.get(f)
            path = os.path.join(d, dat.filename)
            dat.save(path, overwrite=True)
            LOCAL_DEPOT={s.accessPoint: { "enabled": True}}
            try:
                with libdlt.Session(s.unis_url, bs="5m", depots=LOCAL_DEPOT, threads=1) as sess:
                    res = sess.upload(path)
                if not hasattr(s, 'uploaded_exnodes'): s.extendSchema('uploaded_exnodes', [])
                s.uploaded_exnodes.append(res.exnode)
            except ValueError as e:
                log.warn(e)

    th = threading.Thread(name='uploader', target=bottle.run, daemon=True,
                          kwargs={'host': '0.0.0.0', 'port': port, 'debug': True})
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
        with slock:
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
    global LOCAL_UNIS_PORT
    global DOWNLOAD_DIR
    global sess

    logging.basicConfig(format='[%(asctime)-15s] [%(levelname)s] %(message)s')
    conf = MultiConfig(settings.DEFAULT_FERRY_CONFIG, "DLN ferry agent manages files hosted on the WDLN ferry",
                       filevar="$WDLN_FERRY_CONFIG")
    parser = argparse.ArgumentParser(description='')
    parser.add_argument('-H', '--remote.host', type=str, metavar="HOST",
                        help='Remote UNIS instance host for registration and metadata')
    parser.add_argument('-P', '--remote.port', type=str, metavar="PORT",
                        help='Remote UNIS instance port for registration and metadata')
    parser.add_argument('-p', '--local.port', type=str, metavar="PORT",
                        help='Local UNIS port')
    parser.add_argument('-n', '--name', type=str,
                        help='Set ferry node name (ignore system hostname)')
    parser.add_argument('-d', '--file.download', type=str, metavar="DOWNLOAD",
                        help='Set local download directory')
    parser.add_argument('-u', '--file.upload', type=str, metavar="UPLOAD",
                        help='Set local upload directory')
    parser.add_argument('-l', '--localonly', action='store_true',
                        help='Run using only local UNIS instance (on-ferry)')
    parser.add_argument('-i', '--ibp', action='store_true',
                        help='Update IBP config to reflect interface changes on system')
    conf = conf.from_parser(parser, include_logging=True)

    name = socket.gethostname()
    fqdn = socket.getfqdn()
    LOCAL_UNIS_PORT = conf['local']['port']

    log.info("Ferry \"{}\" reporting for duty".format(name))
    if conf['name']:
        name = conf['name']
        log.info("Setting ferry name to \"{}\"".format(name))

    DOWNLOAD_DIR = conf['file']['download']
    try:
        os.makedirs(DOWNLOAD_DIR)
    except FileExistsError:
        pass
    except OSError as exp:
        raise exp

    # use fqdn to determine local endpoints
    LOCAL_DEPOT={"ibp://{}:6714".format(fqdn): { "enabled": True}}

    # allow an alternative UNIS instance (non-ferry) in local mode
    remote, default_auth = [":".join([d['remote']['host'], d['remote']['port']]) for d in [conf, settings.DEFAULT_FERRY_CONFIG]]
    if (conf['localonly'] and remote != default_authority):
        LOCAL_UNIS = remote
    else:
        LOCAL_UNIS = "http://{}:{}".format(fqdn, LOCAL_UNIS_PORT)

    # get our initial UNIS-RT and libdlt sessions
    rt = init_runtime(remote, LOCAL_UNIS, conf['localonly'])
    sess = libdlt.Session(rt, bs="5m", depots=LOCAL_DEPOT, threads=1)

    # Start the registration loop
    register(rt, name, fqdn)

    # Start the iface watcher for IBP config
    if conf['ibp']:
        IBPWatcher()

    # Start uploader thread
    run_uploader(conf['file']['upload'], conf['file']['port'])
    
    # run our main loop
    if conf['localonly']:
        run_local(sess, rt)
    else:
        run_remote(sess, rt)

if __name__ == "__main__":
    main()
    
