#!/usr/bin/env python3

import bottle, socket, threading, subprocess
import argparse, os, time, logging

import libdlt
import wdln.settings as settings
from wdln.config import MultiConfig
from asyncio import TimeoutError
from unis.models import Node, schemaLoader
from unis.runtime import Runtime
from unis.exceptions import ConnectionError, UnisReferenceError
from unis.utils import asynchronous
from wdln.ferry.gps import GPS
from wdln.ferry.ibp_iface import IBPWatcher
from wdln.ferry.log import log

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
        log.debug(f"Connecting to UNIs instance(s): {', '.join([v['url'] for v in hosts])}")
        while not self.rt:
            try:
                self.rt = Runtime(hosts, **opts)
            except (ConnectionError, TimeoutError, UnisReferenceError) as e:
                log.warn(f"Could not contact UNIS servers {', '.join(r_urls)}, retrying...")
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
                    "serviceType": "datalogistics:wdln:ferry",
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
    def set_status(self, status, clear=False):
        msg = {"status": status}
        if clear: msg["new_exnodes"] = []
        asynchronous.make_async(self.rt.nodes._unis.put,
                                self.service.getSource(),
                                self.service.id, msg)

def agentloop(agent):
    def touch():
        err = 0
        agent.register()
        while err < agent.cfg['engine']['maxfail']:
            time.sleep(agent.cfg['engine']['interval'])
            try:
                agent.set_pos()
                agent.service.touch()
            except (ConnectionError, TimeoutError, UnisReferenceError) as e:
                log.warn("Could not update node/service resources")
                log.debug(f"-- {e}")
                err += 1

    while True:
        try:
            touch()
        except (ConnectionError, TimeoutError, UnisReferenceError) as e:
            time.sleep(agent.cfg['engine']['interval'])
            log.warn("Re-registering agent...")
            log.debug(f"-- {e}")
            

def configure_upload_server(agent):
    @bottle.route('/flist')
    def _list():
        return repr(os.listdir(agent.cfg['file']['upload']))
    @bottle.route('/upload', method='POST')
    def _files():
        with libdlt.Session(agent.rt, bs="5m", depots={agent.service.accessPoint: { "enabled": True}}, threads=1) as sess:
            for f in bottle.request.files.keys():
                try:
                    dat = bottle.request.files.get(f)
                    path = os.path.join(agent.cfg['file']['upload'], dat.filename)
                    dat.save(path, overwrite=True)
                    res = sess.upload(path)
                    if not hasattr(agent.service, 'uploaded_exnodes'):
                        agent.service.extendSchema('uploaded_exnodes', [])
                    agent.service.uploaded_exnodes.append(res.exnode)
                except ValueError as e:
                    log.warn(e)

def downloop(agent):
    def download_file(path, f, sess):
        log.info(f"Downloading: {f.name} ({f.size} bytes)")
        try:
            result, t, dsize = sess.download(f.selfRef, path)
            if dsize != result.size:
                log.warn(f"Incorrect file size {result.name}: Transferred {dsize} of {result.size}")
            else:
                log.info("{result.name} ({result.size} {result.size/1e6/t} MB/s) {result.selfRef}")
        except (ConnectionError, TimeoutError, AllocationError) as e:
            log.warn(f"Could not download file: {e}")

    with libdlt.Session(agent.rt, bs="5m", depots={agent.service.accessPoint: {"enabled": True}}, threads=1) as sess:
        while True:
            log.info(f"[{agent.service.status}] Waiting for update...")
            for _ in range(10):
                agent.service.reload()
                if agent.service.status == "UPDATE":
                    dl_list = [f for f in getattr(agent.service, 'new_exnodes', [])]
                    log.info(f"Caught UPDATE status with {len(dl_list)} new exnodes")
                    for f in dl_list:
                        path = os.path.join(agent.cfg['file']['download'], f.name)
                        if os.path.exists(path) and os.path.getsize(fpath) == f.size:
                            log.debug(f"File exists: {f.name}, skipping")
                        else:
                            download_file(path, f, sess)
                    agent.set_status("READY", clear=True)
                time.sleep(1)

def main():
    conf = MultiConfig(settings.DEFAULT_FERRY_CONFIG, "DLN ferry agent manages files hosted on the WDLN ferry",
                       filevar="$WDLN_FERRY_CONFIG")
    parser = argparse.ArgumentParser(description='')
    parser.add_argument('-H', '--remote.host', type=str, metavar="HOST",
                        help='Remote UNIS instance host for registration and metadata')
    parser.add_argument('-P', '--remote.port', type=str, metavar="PORT",
                        help='Remote UNIS instance port for registration and metadata')
    parser.add_argument('-p', '--local.port', type=str, metavar="PORT",
                        help='Local UNIS port')
    parser.add_argument('-n', '--name', type=str,
                        help='Set ferry node name (ignore system hostname)')
    parser.add_argument('-d', '--file.download', type=str, metavar="DOWNLOAD",
                        help='Set local download directory')
    parser.add_argument('-u', '--file.upload', type=str, metavar="UPLOAD",
                        help='Set local upload directory')
    parser.add_argument('-l', '--localonly', action='store_true',
                        help='Run using only local UNIS instance (on-ferry)')
    parser.add_argument('-i', '--ibp', action='store_true',
                        help='Update IBP config to reflect interface changes on system')
    conf = conf.from_parser(parser, include_logging=True)
    if conf['ibp']: IBPWatcher()

    try: os.makedirs(conf['file']['download'])
    except FileExistsError: pass
    except OSError as exp: raise exp

    try: os.makedirs(conf['file']['upload'])
    except FileExistsError: pass
    except OSError as exp: raise exp
    
    agent = Agent(conf)
    threading.Thread(
        name='dlnagent.mainloop',
        target=agentloop,
        daemon=True,
        args=(agent,),
    ).start()

    configure_upload_server(agent)
    threading.Thread(
        name='dlnagent.upload',
        target=bottle.run,
        daemon=True,
        kwargs={'host': '0.0.0.0', 'port': conf['file']['port'], 'debug': True}
    ).start()
    
    while True:
        try:
            downloop(agent)
        except (ConnectionError, TimeoutError, UnisReferenceError) as e:
            log.warn("Connection failure in main loop")
            log.debug(f"--{e}")
            tim.sleep(conf['engine']['interval'])

if __name__ == "__main__":
    main()
