import itertools
<<<<<<< HEAD
import requests
=======
>>>>>>> 0552500d73c329fec28da4aaa9de1a87c005f82a

from unis import Runtime
from unis.models import Service, Exnode, Extent
from unis.services import RuntimeService
from collections import defaultdict
from libdlt import Session
from libdlt.schedule import BaseUploadSchedule, BaseDownloadSchedule
from lace import logging


<<<<<<< HEAD
=======

>>>>>>> 0552500d73c329fec28da4aaa9de1a87c005f82a
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
    
<<<<<<< HEAD
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
=======
    def __init__(self, depots):
        super(IDMSService, self).__init__()
        self._depots = depots
        self._log = logging.getLogger()
        
    def attach(self, runtime):
        super(IDMSService, self).attach(runtime)
        self._session = Session(runtime._unis._default_source, self._depots, viz_url="http://wdln-base-station:42424")
        runtime.pending_exnodes = defaultdict(list)
    
    def update(self, resource):
        print("Resource touched")
        self._log.info("Service touched - {}".format(resource.name))
        while self.lock:
            pass
        if resource.status == 'READY' and self._runtime.pending_exnodes[resource.name]:
            try:
                type(self).lock = True
                try:
                    tmpRT = Runtime(resource.unis_url, subscribe=False, defer_update=True)
                except Exception as exp:
                    self._runtime.log.warn("Failed to contact transient UNIS - {}".format(exp))
                    return
            
                todo = self._runtime.pending_exnodes[resource.name]
                self._runtime.pending_exnodes[resource.name] = []
                upload = self.ForceUpload(resource.accessPoint if isinstance(resource.accessPoint, list) else [resource.accessPoint])
                for exnode, lifetime in todo:
                    print("Starting copy")
                    try:
                        self._session.copy(exnode.selfRef, self.ForceDownload(resource.accessPoint), upload, duration=lifetime)
                    except Exception as exp:
                        import sys
                        print(sys.exc_info()[2])
                    print("Copy complete")
                    new_exnode = Exnode({k:v for k,v in exnode.to_JSON().items() if k not in ['id', 'selfRef', 'extents']})
                    for extent in exnode.extents:
                        if extent.location == resource.accessPoint:
                            new_extent = Extent({k:v for k,v in extent.to_JSON().items() if k not in ["id", "selfRef", "parent"]})
                            new_extent.parent = new_exnode
                            tmpRT.insert(new_extent, commit=True)
                            new_exnode.extents.append(new_extent)
                            pprint(new_extent.to_JSON())
                    tmpRT.insert(new_exnode, commit=True)
                    resource.new_exnodes.append(new_exnode)
                tmpRT.flush()
                resource.status = 'UPDATE'
                self._runtime.flush() 
            except:
                raise
            finally:
                type(self).lock = False
   
>>>>>>> 0552500d73c329fec28da4aaa9de1a87c005f82a
