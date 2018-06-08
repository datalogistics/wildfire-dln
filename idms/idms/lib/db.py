import copy
import itertools

from asyncio import TimeoutError
from libdlt.sessions import Session
from libdlt.schedule import BaseUploadSchedule
from threading import RLock
from unis import Runtime
from unis.models import Exnode, Extent
from unis.exceptions import ConnectionError

from idms import settings, engine

class ForceUpload(BaseUploadSchedule):
    def __init__(self, sources):
        self._alt_ls = itertools.cycle(sources)
    def get(self, source):
        for depot in self._alt_ls:
            return depot

class DBLayer(object):
    def __init__(self, runtime, depots, viz):
        self._lock = RLock()
        self._flock = RLock()
        self._rt = runtime
        self._custom_depots = depots or {}
        self._active = []
        self._viz = viz
        self._local_files = []

    def _get_ferry_list(self):
        return self._rt.services.where({"serviceType": "datalogistics:wdln:ferry"})
    def _get_base_list(self):
        return self._rt.services.where({"serviceType": "datalogistics:wdln:base"})

    def move_files(self, exnodes, dst=None, ttl=None):
        with self._flock:
            depots=self.get_depot_list()
            with Session(self._rt, depots=depots, threads=settings.THREADS, viz_url=self._viz) as sess:
                for exnode in exnodes:
                    if exnode.selfRef not in self._local_files:
                        sess.download(exnode.selfRef, exnode.id)
                        self._local_files.append(exnode.selfRef)
            if dst:
                if isinstance(dst, str):
                    dst = next(self._rt.services.where({'accessPoint': dst}))
                remote = Runtime(dst.unis_url, name=dst.unis_url)
                dst.new_exnodes = []
                with Session(remote, depots=depots, threads=settings.THREADS, bs=settings.BS, viz_url=self._viz) as sess:
                    for exnode in exnodes:
                        result = sess.upload(exnode.id, exnode.name, copies=1, schedule=ForceUpload([dst.accessPoint]), duration=ttl)
                        dst.new_exnodes.append(result.exnode)
                        for alloc in result.exnode.extents:
                            new_alloc = alloc.clone()
                            new_alloc.parent = exnode
                            del new_alloc.getObject().__dict__['function']
                            self._rt.insert(new_alloc, commit=True)
                            exnode.extents.append(new_alloc)
                dst.status = "UPDATE"
                self._rt.update(exnode)
                self._rt.update(dst)
                remote.flush()
                self._rt.flush()

    def register_policy(self, policy):
        for p in self._active:
            if policy.to_JSON() == p.to_JSON():
                return
        for ex in self._rt.exnodes.where(lambda x: policy.match(x)):
            self.move_files([ex])
            policy.watch(ex)
        with self._lock:
            self._active.append(policy)
        return len(self._active) - 1

    def get_active_policies(self, exnode=None):
        with self._lock:
            if exnode:
                exnode = next(self._rt.exnodes.where({'id': exnode}))
                result = [p for p in self._active if p.match(exnode)]
            else:
                result = [p for p in self._active]
        for p in result:
            yield p

    def update_policies(self, resource):
        with self._lock:
            if isinstance(resource, Exnode):
                for p in self.get_active_policies():
                    if p.match(resource):
                        p.watch(resource)
                        p.dirty = True
            else:
                for p in self.get_active_policies():
                    p.dirty = True
        engine.dirty.set()

    def get_policies(self):
        dests = []
        for ferry in self._get_ferry_list():
            dests.append({"ferry_name": ferry.name, "data_lifetime": 108000})
        return dests

    def get_depots(self):
        depots = list(self._get_ferry_list())
        depots.extend(self._get_base_list())
        return depots
    def get_depot_list(self):
        depots = copy.deepcopy(self._custom_depots)
        for ferry in self._get_ferry_list():
            depots[ferry.accessPoint] = {"enabled": True}
        for depot in self._rt.services.where({"serviceType": "ibp_server"}):
            depots[depot.accessPoint] = {"enabled": True}
        for base in self._get_base_list():
            depots[base.accessPoint] = {"enabled": True}
        return depots

    def manage_depots(self, ref, attrs):
        service = next(self._rt.services.where({'id': ref}))
        for k,v in attrs.items():
            setattr(service, k, v)
        self._rt.flush()
