import socket, time

from asyncio import TimeoutError
from lace import logging
from unis.exceptions import ConnectionError, UnisReferenceError
from unis.models import Node, schemaLoader
from unis.runtime import Runtime
from unis.utils import asynchronous
from wdln.ferry.gps import GPS
from wdln import settings


log = logging.getLogger("wdln.agent")
DLNFerry = schemaLoader.get_class(settings.FERRY_SERVICE)
class Agent(object):
    def __init__(self, cfg):
        self.local = f"http://{cfg['local']['host'] or socket.getfqdn()}:{cfg['local']['port']}"
        self.rt, self._n, self._s = None, None, None
        self.cfg = cfg
        self.name = cfg['name'] or socket.gethostname()
        self.gps = GPS()
        hosts = [{"url": self.local}]
        if cfg["localonly"]:
            hosts[0]["default"] = True
            self.auth = hosts[0]['url']
        else:
            hosts.append({"url": f"http://{cfg['remote']['host']}:{cfg['remote']['port']}",
                          "default": True})
            self.auth = hosts[1]['url']
        self.connect(hosts)

    def connect(self, hosts):
        opts = {"cache": { "preload": ["nodes", "services"] },
                "proxy": { "subscribe": False, "defer_update": True }}
        log.debug(f"Connecting to UNIS instance(s): {', '.join([v['url'] for v in hosts])}")
        while not self.rt:
            try:
                self.rt = Runtime(hosts, **opts)
            except (ConnectionError, TimeoutError, UnisReferenceError) as e:
                log.warning(f"Could not contact UNIS servers {', '.join([v['url'] for v in hosts])}, retrying...")
                log.debug(f"-- {e}")
                time.sleep(self.cfg['engine']['interval'])

    def register(self):
        self.rt._update(self.node)
        self.rt._update(self.service)
        self.rt.flush()

    @property
    def node(self):
        if self._n is None:
            self._n = self.rt.nodes.first_where(lambda x: x.name == self.name) or \
                self.rt.insert(Node({'name': self.name}), track=True, publish_to=self.auth)
        return self._n
    
    @property
    def service(self):
        if self._s is None:
            self._s = self.rt.services.first_where(lambda x: x.runningOn == self.node) or \
                self.rt.insert(DLNFerry({
                    "runningOn": self.node,
                    "serviceType": f"datalogistics:wdln:{self.cfg['servicetype']}",
                    "name": self.name,
                    "accessPoint": f"ibp://{socket.getfqdn()}:6714",
                    "unis_url": self.local,
                    "status": "READY",
                    "ttl": 600
                }), track=True, publish_to=self.auth)
        return self._s

    def set_pos(self):
        lat, lon = self.gps.query()
        if lat and lon:
            res = {'id': self.node.id, 'location': {'latitude': lat, 'longitude': lon}}
            asynchronous.make_async(self.rt.nodes._unis.put,
                                    self.node.getSource(),
                                    self.node.id, res)
