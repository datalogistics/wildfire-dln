#!/usr/bin/env python3

import os
import time
import argparse
import socket
import threading
import logging
import subprocess

import ferry.settings as settings
from asyncio import TimeoutError
from ferry.settings import UNIS_URL, LOCAL_UNIS_HOST, LOCAL_UNIS_PORT
from unis.models import Node, schemaLoader
from unis.runtime import Runtime
from unis.exceptions import ConnectionError
from ferry.gps import GPS
from ferry.base_sync import BaseFerrySync
from ferry.log import log

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
            s.serviceType="datalogistics:wdln:base"
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
        except (ConnectionError, TimeoutError) as exp:
            log.warn("Could not contact UNIS servers {}, retrying...".format(urls))
        time.sleep(5)

def run_base(rt):
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
    register(rt, name, fqdn)

    # start base-ferry sync
    BaseFerrySync(rt, name)
    
    run_base(rt)
    
if __name__ == "__main__":
    main()
    
