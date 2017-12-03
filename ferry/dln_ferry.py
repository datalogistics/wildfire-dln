#!/usr/bin/env python3

import os
import time
import argparse
import socket
import threading
import lace, logging

from libdlt.util import util
from libdlt.depot import Depot
from unis.models import Exnode, Service, Node, schemaLoader
from unis.runtime import Runtime

GEOLOC="http://unis.crest.iu.edu/schema/ext/dln/1/geoloc#"
FERRY_SERVICE="http://unis.crest.iu.edu/schema/ext/dln/1/ferry#"
UNIS_URL="http://localhost:8888"
log = None

DLNFerry = schemaLoader.get_class(FERRY_SERVICE)
GeoLoc = schemaLoader.get_class(GEOLOC)

def register(rt, name):
    n = rt.nodes.where({"name": name})
    try:
        n = next(n)
    except:
        n = Node();
        n.name = name
        rt.insert(n, commit=True)

    s = rt.services.where({"runningOn": n})
    try:
        s = next(s)
    except:
        s = DLNFerry()
        s.runningOn = n
        s.serviceType="datalogistics:wdln:ferry"
        s.name = name
        s.accessPoint = "ibp://{}:6714".format(name)
        s.status = "READY"
        rt.insert(s, commit=True)

    # simply update the timestamps on our node and service resources
    def touch(n,s):
        while True:
            time.sleep(5)
            try:
                n.poke()
                s.poke()
            except Exception as e:
                log.error("Could not update node/service resources: {}".format(e))
        
    th = threading.Thread(
        name='toucher',
        target=touch,
        daemon=True,
        args=(n,s,),
    )
    th.start()

    return (n,s)
    
def init_runtime(url):
    while True:
        try:
            rt = Runtime([{"default": True, "url": url}],
                         **{"preload": ["nodes", "services"]})
            return rt
        except:
            log.warn("Could not contact UNIS at {}, retrying...".format(url))
        time.sleep(5)
    
def run(n, s):
    i=0
    while True:
        (i%5) or log.info("Waiting for some action...")
        if s.status == "UPDATE":
            dl_list = s.new_exnodes
            log.debug("List of exnodes to download locally: {}".format(dl_list))
            time.sleep(1)
            s.status = "READY"
            s.commit()
            
        i+=1
        time.sleep(1)
    
def init_logging(args):
    level = logging.DEBUG if args.verbose else logging.INFO
    level = logging.CRITICAL if args.quiet else level
    log = lace.logging.getLogger("ferry")
    log.setLevel(level)
    return log
        
def main():
    parser = argparse.ArgumentParser(description="DLN Mobile Ferry Agent")
    parser.add_argument('-H', '--host', type=str, default=UNIS_URL,
                        help='UNIS instance for registration and metadata')
    parser.add_argument('-n', '--name', type=str, default=None,
                        help='Set ferry node name (ignore system hostname)')
    parser.add_argument('-v', '--verbose', action='store_true',
                        help='Produce verbose output from the script')
    parser.add_argument('-q', '--quiet', action='store_true',
                        help='Quiet mode, no logging output')

    global log
    args = parser.parse_args()
    log = init_logging(args)
    
    name = socket.gethostname()
    log.info("Ferry \"{}\" reporting for duty".format(name))
    if args.name:
        name = args.name
        log.info("Setting ferry name to \"{}\"".format(name))

    # get our initial UNIS-RT instance
    rt = init_runtime(args.host)
    
    # Start the registration loop
    # returns handles to the node and service objects
    (n,s) = register(rt, name)

    # run our main loop
    run(n,s)
    
if __name__ == "__main__":
    main()
