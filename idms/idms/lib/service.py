import itertools
import requests

from unis import Runtime
from unis.models import Service, Exnode, Extent
from unis.services import RuntimeService
from collections import defaultdict
from libdlt import Session
from libdlt.schedule import BaseUploadSchedule, BaseDownloadSchedule
from lace import logging

from pprint import pprint

class IDMSService(RuntimeService):
    class ForceUpload(BaseUploadSchedule):
        def __init__(self, sources):
            self._ls = itertools.cycle(sources)
        def setSource(self, source):
            pass
    class ForceDownload(BaseDownloadSchedule):
        def __init__(self, target):
            self.target = target
        def setSource(self, source):
            self._ls = defaultdict(list)
            for ext in filter(lambda x: x.location != self.target, source):
                self._ls[ext.offset].append(ext)
    
    targets = [Service]
    lock = False
    
    def __init__(self, depots, policies, viz):
        super(IDMSService, self).__init__()
        self._depots = depots
        self._log = logging.getLogger()
        self._pending = defaultdict(list)
        self._policy_source = policies
        self._viz = viz
        
    def prepare(self, policy, exnodes):
        policy = requests.get(self._policy_source).json()[policy['ferry_name']]
        ferry = next(self.runtime.services.where({'name': policy['ferry_name']}))
        #if ferry.status == 'READY':
        #    self.copy(ferry, exnodes, policy['data_lifetime'])
        #else:
        self.download(ferry, exnodes, policy['data_lifetime'])
        print("Download complete")
        
    def copy(self, ferry, exnodes, lifetime):
        while self.lock: pass
        #try:
        #    remote = Runtime(ferry.unis_url, proxy={'subscribe': False, 'defer_update': True})
        #except Exception as exp:
        #        self.runtime.log.warn("Failed to contact transient UNIS - {}".format(exp))
        #        return
            
        type(self).lock = True
        store_defualt = self.runtime.settings['default_source']
        self.runtime.settings['default_source'] = ferry.unis_url
        try:
            for exnode in exnodes:
                print("Starting copy")
                exnode = next(self.runtime.exnodes.where({'selfRef': exnode}))
                upload = self.ForceUpload(ferry.accessPoint if isinstance(ferry.accessPoint, list) else [ferry.accessPoint])
                with Session(self.runtime, self._depots, viz_url=self._viz) as sess:
                    sess.copy(exnode.selfRef, download_schedule=self.ForceDownload(ferry.accessPoint), upload_schedule=upload, duration=lifetime)
                    print("Copy complete")
                    
                new_exnode = Exnode({k:v for k,v in exnode.to_JSON().items() if k not in ['id', 'selfRef', 'extents']})
                for extent in exnode.extents:
                    ext = Extent({k:v for k,v in exnode.to_JSON().items() if k not in ['id', 'selfRef', 'parent']})
                    ext.parent = new_exnode
                    self.runtime.insert(ext, commit=True)
                    
                self.runtime.insert(new_exnode, commit=True)
                ferry.new_exnodes.append(new_exnode)
            ferry.status = 'UPDATE'
            self.runtime.flush()
        except Exception:
            raise
        finally:
            type(self).lock = False
    
    def download(self, ferry, exnodes, lifetime):
        for exnode in exnodes:
            exnode = next(self.runtime.exnodes.where({'selfRef': exnode}))
            with Session(self.runtime, self._depots, viz_url=self._viz) as sess:
                sess.download(exnode.selfRef, exnode.name)
            self._pending[ferry.name].append((exnode.name, lifetime))
            
    def attach(self, runtime):
        super(IDMSService, self).attach(runtime)
    
    def update(self, resource):
        print("Resource touched {} {}".format(self._pending[resource.name], resource.status))
        self._log.info("Service touched - {}".format(resource.name))
        while self.lock: pass
        
        type(self).lock = True
        if resource.status == 'READY' and self._pending[resource.name]:
            store_defualt = self.runtime.settings['default_source']
            self.runtime.settings['default_source'] = ferry.unis_url
            #try:
            #    remote = Runtime(resource.unis_url, proxy={'subscribe': False, 'defer_update':True})
            #except Exception as exp:
            #    import traceback
            #    traceback.print_exc()
            #    self.runtime.log.warn("Failed to contact transient UNIS - {}".format(exp))
            #    type(self).lock = False
            #    return
            
            with Session(self.runtime, self._depots, viz_url=self._viz) as sess:
                for name, lifetime in self._pending[resource.name]:
                    upload = self.ForceUpload(resource.accessPoint if isinstance(resource.accessPoint, list) else [resource.accessPoint])
                    print("Starting upload")
                    try:
                        new_exnode = sess.upload(name, schedule=upload, duration=lifetime)
                        resource.new_exnodes.append(new_exnode[1])
                    except:
                        import traceback
                        traceback.print_exc()
                        type(self).lock = False
                        raise
                    print("Upload complete")
                resource.status = 'UPDATE'
                self.runtime.update(resource)
                self.runtime.flush()
                self._pending[resource.name] = []
        type(self).lock = False
