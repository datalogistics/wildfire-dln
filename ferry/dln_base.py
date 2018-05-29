#!/usr/bin/env python3

import os
import time
import argparse
import socket
import threading
import logging
import subprocess

import ferry.settings as settings
from ferry.settings import UNIS_URL, LOCAL_UNIS_HOST, LOCAL_UNIS_PORT
from unis.models import Node, schemaLoader
from unis.runtime import Runtime
from ferry.gps import GPS
from ferry.base_sync import BaseFerrySync
from ferry.log import log

DLNFerry = schemaLoader.get_class(settings.FERRY_SERVICE)
GeoLoc = schemaLoader.get_class(settings.GEOLOC)

def register(rt, name, fqdn, **kwargs):
    n = rt.nodes.where({"name": name})
    try:
        n = next(n)
    except StopIteration:
        n = Node()
        n.name = name
        rt.insert(n, commit=True)

    s = rt.services.where({"runningOn": n})
    try:
        s = next(s)
    except StopIteration:
        s = DLNFerry()
        s.runningOn = n
        s.serviceType="datalogistics:wdln:base"
        s.name = name
        s.accessPoint = "ibp://{}:6714".format(fqdn)
        s.unis_url = "http://{}:{}".format(fqdn, LOCAL_UNIS_PORT)
        s.status = "READY"
        s.ttl = 600 # 10m
        rt.insert(s, commit=True)
    
    gps = GPS()
    
    # simply update the timestamps on our node and service resources
    def touch(n,s,gps):
        while True:
            time.sleep(settings.UPDATE_INTERVAL)
            try:
                (lat, lon) = gps.query()
                if lat and lon:
                    n.location.latitude = lat
                    n.location.longitude = lon
                    rt.flush()
                else:
                    n.touch()
                s.touch()
            except Exception as e:
                log.error("Could not update node/service resources: {}".format(e))
        
    th = threading.Thread(
        name='toucher',
        target=touch,
        daemon=True,
        args=(n,s,gps),
    )
    th.start()
    
    return (n,s)

def node_cb(node, event):
    #nstr = "http://"+node.name+":9000"
    #log.info("Updating {}/nodes".format(nstr))
    pass
    
def init_runtime(local):
    while True:
        try:
            opts = {"cache": { "preload": ["nodes", "services"] }, "proxy": { "defer_update": True }}
            urls = [{"default": True, "url": local}]
            log.debug("Connecting to UNIS instance(s): {}".format(local))
            rt = Runtime(urls, **opts)
            rt.nodes.addCallback(node_cb)
            return rt
        except Exception as e:
            import traceback
            traceback.print_exc()
            log.warn("Could not contact UNIS servers {}, retrying...".format(urls))
        time.sleep(5)

def run_base(n, s, rt):
    i=0
    while True:
        (i%5) or log.info("Waiting for something to do...")
        i+=1
        time.sleep(1)
                
def main():
    parser = argparse.ArgumentParser(description="DLN Base Station Agent")
    parser.add_argument('-H', '--host', type=str, default=UNIS_URL,
                        help='UNIS instance for registration and metadata')
    parser.add_argument('-n', '--name', type=str, default=None,
                        help='Set base node name (ignore system hostname)')
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
    log.info("Base Station \"{}\" reporting for duty".format(name))
    if args.name:
        name = args.name
        log.info("Setting base name to \"{}\"".format(name))

    # use fqdn to determine local endpoints
    LOCAL_DEPOT={"ibp://{}:6714".format(fqdn): { "enabled": True}}

    # allow an alternative UNIS instance (non-ferry) in local mode
    if (args.host != UNIS_URL):
        LOCAL_UNIS=args.host
    else:
        LOCAL_UNIS = "http://{}:{}".format(fqdn, LOCAL_UNIS_PORT)
    
    # get our initial UNIS-RT and libdlt sessions
    rt = init_runtime(LOCAL_UNIS)

    # Start the registration loop
    # returns handles to the node and service objects
    (n,s) = register(rt, name, fqdn)

    # start base-ferry sync
    BaseFerrySync(rt)
    
    run_base(n, s, rt)
    
if __name__ == "__main__":
    main()
    
