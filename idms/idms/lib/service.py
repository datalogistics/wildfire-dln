from unis.models import Service, Exnode, Extent
from unis.services import RuntimeService
from collections import defaultdict
from libdlt import Session
from libdlt.schedule import AbstractSchedule, BaseDownloadSchedule

class IDMSService(RuntimeService):
    class ForceUpload(AbstractSchedule):
        def __init__(self, sources):
            self._ls = cycle(sources)
        def setSource(self, source):
            pass
    
    targets = [Service]
    
    def attach(self, runtime):
        super(IDMSService, self).attach(runtime)
        self._session = Session(runtime._unis._default_source, None)
        runtime.pending_exnodes = defaultdict(list)
    
    def update(self, resource):
        if resource.STATUS == 'READY' and self._runtime.pending_exnodes[resource.name]:
            try:
                tmpRT = Runtime(resource.unis_url, defer_update=True)
            except Exception as exp:
                self._runtime.log.warn("Failed to contact transient UNIS - {}".format(exp))
                return
                
            upload = ForceUpload(resource.accessPoint)
            for exnode, lifetime in self._runtime.pending_exnodes[resource.name]:
                self._session.copy(exnode.selfRef, BaseDownloadSchedule(), upload, duration=lifetime)
                new_exnode = Exnode({k:v for k,v in exnode.to_JSON() if k not in ["id", "selfRef", "extents"]})
                new_exnode.extents = [x for x in exnode.extents if x.location == resource.accessPoint]
                tmpRT.insert(new_exnode, commit=True)
                resource.new_exnodes.append(new_exnode)
                
            tmpRT.flush()
            self._runtime.pending_exnodes[resource.name].clear()
            resource.STATUS = 'UPDATE'
            self._runtime.flush()
