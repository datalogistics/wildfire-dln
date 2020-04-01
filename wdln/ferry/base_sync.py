import time
import json
import threading
import requests
from unis.models import Node
from wdln.ferry.log import log

SYNC_INTERVAL=2
SERVICE_THRESH=10
STYPES = ["datalogistics:wdln:ferry"]

class BaseFerrySync:
    def __init__(self, rt, myname):
        self.myname = myname
        self.pushed = []
        
        rt.services.addCallback(self._srv_cb)
        
        th = threading.Thread(
            name='base_sync',
            target=self._sync,
            daemon=True,
            args=(rt,)
        )
        th.start()

    def _srv_cb(self, s, ev):
        log.debug("Service update: {}".format(s.name))
        
    def _sync(self, rt):
        while True:
            nds = []
            n5 = int((time.time() - SYNC_INTERVAL)*1e6)
            n10 = int((time.time() - SERVICE_THRESH)*1e6)

            for n in rt.nodes:
                if n.ts > n5:
                    nds.append(n)

            if len(nds):
                log.debug("Time to sync: {}".format(([n.name for n in nds])))
                for s in rt.services:
                    if s.ts > n10 and s.serviceType in STYPES:
                        # push all known nodes if we haven't
                        # seen this service before
                        if s.name not in self.pushed:
                            nds = rt.nodes

                        # build a list of nodes for a single post
                        nlist = []
                        for n in nds:
                            repr = n.to_JSON()
                            del repr['selfRef']
                            nlist.append(repr)
                            
                        # finally post the node list to the endpoint
                        url = "http://{}:9000/nodes".format(s.name)
                        log.debug("Syncing to {}".format(url))
                        try:
                            self._do_post(url, json.dumps(nlist))
                            self.pushed.append(s.name)
                        except:
                            pass
                
            time.sleep(SYNC_INTERVAL)
        
    def _do_post(self, url, data):
        try:
            headers = {"Content-Type": "application/perfsonar+json"}
            requests.post(url, headers=headers, data=data)
        except Exception as e:
            log.error("Could not update node at {}: {}".format(url, e))
            raise e
