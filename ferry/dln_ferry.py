#!/usr/bin/env python3

import os
import time
import argparse
import socket
import threading
from lace import logging

import libdlt
from unis.models import Exnode, Service, Node, schemaLoader
from unis.runtime import Runtime

GEOLOC="http://unis.crest.iu.edu/schema/ext/dln/1/geoloc#"
FERRY_SERVICE="http://unis.crest.iu.edu/schema/ext/dln/1/ferry#"

UNIS_URL="http://localhost:8888"
LOCAL_UNIS_HOST="wdln-ferry-00"
LOCAL_UNIS_PORT=8891

# globals
DOWNLOAD_DIR=".cache"
log = None
sess = None

DLNFerry = schemaLoader.get_class(FERRY_SERVICE)
GeoLoc = schemaLoader.get_class(GEOLOC)

def register(rt, name, fqdn):
    n = rt.nodes.where({"name": name})
    try:
        n = next(n)
    except:
        n = Node();
        n.name = name
        rt.insert(n, commit=True)
        
    s = rt.services.where(lambda x: x.runningOn.href == n.selfRef)
    try:
        s = next(s)
        s.status = "READY"
    except:
        s = DLNFerry()
        s.runningOn = n
        s.serviceType="datalogistics:wdln:ferry"
        s.name = name
        s.accessPoint = "ibp://{}:6714".format(fqdn)
        s.unis_url = "http://{}:{}".format(fqdn, LOCAL_UNIS_PORT)
        s.status = "READY"
        rt.insert(s, commit=True)

    rt.flush()
    
    # simply update the timestamps on our node and service resources
    def touch(n,s):
        while True:
            time.sleep(5)
            try:
                n.touch()
                s.touch()
            except Exception as e:
                import traceback
                traceback.print_exc()
                log.error("Could not update node/service resources: {}".format(e))
        
    th = threading.Thread(
        name='toucher',
        target=touch,
        daemon=True,
        args=(n,s,),
    )
    th.start()
    
    return (n,s)
    
def init_runtime(remote, local, local_only):
    while True:
        try:
            if local_only:
                urls = [{"default": True, "url": local}]
                opts = {"preload": ["nodes", "services", "exnodes"], "subscribe": {"exnodes": file_cb}}
                log.debug("Connecting to UNIS instance(s): {}".format(local))
            else:
                urls = [{"default": True, "url": remote}, {"default": False, "url": local}]
                opts = {"preload": ["nodes", "services"]}
                log.debug("Connecting to UNIS instance(s): {}".format(remote+','+local))
            rt = Runtime(urls, **opts)
            return rt
        except Exception as e:
            import traceback
            traceback.print_exc()
            log.warn("Could not contact UNIS servers {}, retrying...".format(urls))
        time.sleep(5)

def file_cb(ex):
    local_download(sess, [ex])

def local_download(sess, exnodes):
    for f in exnodes:
        if not len(f.extents):
            continue
        log.info("Downloading: {} ({} bytes)".format(f.name, f.size))
        try:
            diff, res = sess.download(f.selfRef, "{}/{}".format(DOWNLOAD_DIR,f.name))
        except Exception as e:
            import traceback
            traceback.print_exc()
            log.error("Could not download file: {}".format(e))
            continue
            #if dsize != res.size:
            #    log.warn("WARNING: {}: transferred {} of {} bytes \
            #    (check depot file)".format(res.name,
            #                               dsize,
            #                               res.size))
            #else:
        log.info("{0} ({1} {2:.2f} MB/s) {3}".format(res.name, res.size,
                                                     res.size/1e6/diff,
                                                     res.selfRef))
    
def run_local(sess, n, s, rt):
    i=0
    while True:
        (i%5) or log.info("Waiting for some local action...")
        i+=1
        time.sleep(1)
        
def run_remote(sess, n, s, rt):
    i=0
    while True:
        (i%5) or log.info("Waiting for some remote action...{}".format(s.status))
        if s.status == "UPDATE":
            dl_list = s.new_exnodes
            log.info("Caught UPDATE status with {} new exnodes".format(len(dl_list)))
            local_download(sess, dl_list)
            time.sleep(1)
            s.status = "READY"
            rt.flush()
        i+=1
        time.sleep(1)
    
def init_logging(args):
    level = logging.DEBUG if args.verbose else logging.INFO
    level = logging.CRITICAL if args.quiet else level
    log = logging.getLogger("ferry")
    log.setLevel(level)
    return log
        
def main():
    global log
    global DOWNLOAD_DIR
    global sess
    
    parser = argparse.ArgumentParser(description="DLN Mobile Ferry Agent")
    parser.add_argument('-H', '--host', type=str, default=UNIS_URL,
                        help='UNIS instance for registration and metadata')
    parser.add_argument('-n', '--name', type=str, default=None,
                        help='Set ferry node name (ignore system hostname)')
    parser.add_argument('-d', '--download', type=str, default=DOWNLOAD_DIR,
                        help='Set local download directory')
    parser.add_argument('-l', '--local', action='store_true',
                        help='Run using only local UNIS instance (on-ferry)')
    parser.add_argument('-v', '--verbose', action='store_true',
                        help='Produce verbose output from the script')
    parser.add_argument('-q', '--quiet', action='store_true',
                        help='Quiet mode, no logging output')

    args = parser.parse_args()
    log = init_logging(args)
    
    name = socket.gethostname()
    fqdn = LOCAL_UNIS_HOST or socket.getfqdn()
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
    sess = libdlt.Session([{"default": True, "url": LOCAL_UNIS}],
                          bs="5m", depots=LOCAL_DEPOT, threads=1)
    
    # Start the registration loop
    # returns handles to the node and service objects
    (n,s) = register(rt, name, fqdn)
    
    # run our main loop
    if args.local:
        run_local(sess, n, s, rt)
    else:
        run_remote(sess, n, s, rt)
    
if __name__ == "__main__":
    main()
