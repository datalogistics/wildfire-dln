import itertools

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
   
