'''
import libdlt
from unis.models import Node, schemaLoader
from unis.runtime import Runtime

DLNFerry = schemaLoader.get_class(settings.FERRY_SERVICE)
GeoLoc = schemaLoader.get_class(settings.GEOLOC)
'''

import os
import time
import threading
import subprocess
import Queue

MINION_NAME = 'bob'

# default values for the San Diego Convetion Center
SANDIEGO_LATITUDE = 32.707175 # ~ y-axis
SANDIEGO_LONGITUDE = -117.162417 # ~ x-axis

# default output, should parsing fail
DECODE_FAIL = [SANDIEGO_LATITUDE,SANDIEGO_LONGITUDE]

# utility to extract data from string S, whatever form it might take
def retrieve_coords(S):
    if '|' not in S:
        return DECODE_FAIL

    return map(lambda x: float(x), S.strip('\n').split('|')[1].split(','))
    
def begin():
    lora_q = Queue.Queue()

    '''
    # use fqdn to determine local endpoints
    name = socket.gethostname()
    fqdn = socket.getfqdn()

    LOCAL_DEPOT={"ibp://{}:6714".format(fqdn): { "enabled": True}}
    LOCAL_UNIS = "http://{}:{}".format(fqdn, LOCAL_UNIS_PORT)

    urls = [{"default": True, "url": LOCAL_UNIS}]
    opts = {"cache": { "preload": ["nodes", "services", "exnodes"]}}

    rt = Runtime(urls, **opts)
    sess = libdlt.Session(rt, bs="5m", depots=LOCAL_DEPOT, threads=1)

    # insert node here; MINION_NAME can be used as the node name
    nodes_present = []
    
    for n in rt.nodes:
        nodes_present.append(n.name)
        
    if MINION_NAME not in nodes_present:
        n = Node()
        n.name = MINION_NAME
        rt.insert(n,commit=True)
        rt.flush()
    '''

    while True:
        S = subprocess.check_output(['sudo','./rf95_server'])

        S0 = S.split('\n')
        for s in S0:
            if '|' in s:
                latitude,longitude = retrieve_coords(s)

                #n.location.latitude = latitude
                #n.location.longitude = longitude

                print 'node updated location is',latitude,longitude

        time.sleep(1)

if __name__ == "__main__":
    begin()
