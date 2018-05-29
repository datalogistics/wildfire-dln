import time
import json
import threading
import requests
from unis.models import Node
from ferry.log import log

SYNC_INTERVAL=2
SERVICE_THRESH=10
STYPES = ["datalogistics:wdln:ferry"]

class BaseFerrySync:
    def __init__(self, rt, myname):
        self.myname = myname

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
            for n in rt.nodes:
                n5 = int((time.time() - SYNC_INTERVAL)*1e6)
                if n.ts > n5:
                    nds.append(n)
            if len(nds):
                log.debug("Time to sync: {}".format(([n.name for n in nds])))
                for n in nds:
                    repr = n.to_JSON()
                    del repr['selfRef']
                    del repr['id']
                    #nn = Node(repr)
                    #rt.insert(nn)
                    #nn.commit(publish_to=ustr)
                    n10 = int((time.time() - SERVICE_THRESH)*1e6)
                    for s in rt.services:
                        if s.ts > n10 and s.serviceType in STYPES:
                            url = "http://{}:9000/nodes".format(s.name)
                            log.debug("Syncing to {}".format(url))
                            try:
                                headers = {"Content-Type": "application/perfsonar+json"}
                                requests.post(url, headers=headers, data=json.dumps(repr))
                            except Exception as e:
                                log.error("Could not update node at {}: {}".format(url, e))
                
            time.sleep(SYNC_INTERVAL)
        
