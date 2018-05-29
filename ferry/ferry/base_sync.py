import time
import threading
from ferry.log import log

SYNC_INTERVAL=2

class BaseFerrySync:
    def __init__(self, rt):
        th = threading.Thread(
            name='base_sync',
            target=self._sync,
            daemon=True,
            args=(rt,)
        )
        th.start()

    def _sync(self, rt):
        while True:
            nds = []
            for n in rt.nodes:
                n5 = int((time.time() - SYNC_INTERVAL)*1e6)
                if n.ts > n5:
                    nds.append(n)
            if len(nds):
                log.info("Time to sync: {}".format(([n.name for n in nds])))
            time.sleep(SYNC_INTERVAL)
        
