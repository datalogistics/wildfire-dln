#!/usr/bin/env python3

import os
import time
import argparse
import socket
import threading
import logging
import subprocess

import wdln.settings as settings
from asyncio import TimeoutError
from wdln.settings import DEFAULT_BASE_CONFIG
from unis.models import Node, schemaLoader
from unis.runtime import Runtime
from unis.exceptions import ConnectionError
from wdln.ferry.gps import GPS
from wdln.ferry.base_sync import BaseFerrySync
from wdln.ferry.log import log
from wdln.config import MultiConfig

DLNFerry = schemaLoader.get_class(settings.FERRY_SERVICE)
GeoLoc = schemaLoader.get_class(settings.GEOLOC)

LOCAL_UNIS_PORT=settings.LOCAL_UNIS_PORT

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
    global LOCAL_UNIS_PORT
    
    logging.basicConfig(format='[%(asctime)-15s] [%(levelname)s] %(message)s')
    conf = MultiConfig(DEFAULT_BASE_CONFIG, "DLN basestation agent keeps the basestation record alive",
                       filevar="$WDLN_BASE_CONFIG")
    parser = argparse.ArgumentParser(description="DLN Base Station Agent")
    parser.add_argument('-H', '--remote.host', type=str, metavar="HOST",
                        help='UNIS instance host for registration and metadata')
    parser.add_argument('-P', '--remote.port', type=str, metavar="PORT",
                        help='UNIS instance port for registration and metadata')
    parser.add_argument('-n', '--name', type=str,
                        help='Set base node name (ignore system hostname)')
    conf = conf.from_parser(parser, include_logging=True)

    name = socket.gethostname()
    fqdn = socket.getfqdn()
    LOCAL_UNIS_PORT = conf['remote']['port']
    log.info("Base Station \"{}\" reporting for duty".format(name))
    if conf['name']:
        name = conf['name']
        log.info("Setting base name to \"{}\"".format(name))

    # use fqdn to determine local endpoints
    LOCAL_DEPOT={"ibp://{}:6714".format(fqdn): {"enabled": True}}
 
    # allow an alternative UNIS instance (non-ferry) in local mode
    remote, default_auth = [("http://" + ":".join([d['remote']['host'], d['remote']['port']])) for d in [conf, settings.DEFAULT_BASE_CONFIG]]
    if (conf['localonly'] and remote != default_auth):
        LOCAL_UNIS = remote
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
    
