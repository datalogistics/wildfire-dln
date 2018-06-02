import itertools
import requests

from collections import defaultdict
from requests.exceptions import ConnectionError as ConnectionError
from libdlt import Session
from libdlt.schedule import BaseUploadSchedule, BaseDownloadSchedule
from lace import logging
from unis import Runtime
from unis.exceptions import ConnectionError as UnisConnectionError
from unis.models import Service, Exnode, Extent
from unis.rest import UnisClient
from unis.services import RuntimeService

class IDMSService(RuntimeService):
    class ForceUpload(BaseUploadSchedule):
        def __init__(self, sources):
            self._alt_ls = itertools.cycle(sources)
        def get(self, context):
            for depot in self._alt_ls:
                return depot

    targets = [Service]
    lock = False
    
    def __init__(self, dblayer, viz):
        super(IDMSService, self).__init__()
        self._log = logging.getLogger()
        self._db = dblayer
        self._pending = defaultdict(list)
        self._viz = viz
        
    def prepare(self, policy):
        for exnode in self.runtime.exnodes.where(lambda x: policy.match(x)):
            self.download(policy.sendto['$exact'], exnode, policy.meta['data_lifetime'])
        
    def download(self, ferry, exnode, lifetime):
        with Session(self.runtime, self._db.get_depots(), threads=1, viz_url=self._viz) as sess:
            sess.download(exnode.selfRef, exnode.name)
        self._pending[ferry].append((exnode.name, lifetime))
    
    def update(self, resource):
        if resource.serviceType != 'datalogistics:wdln:ferry':
            return
        print("Resource[{}] touched {} {}".format(resource.name, self._pending[resource.name], resource.status))
        self._log.info("Service touched - {}".format(resource.name))
        while self.lock: pass
        
        type(self).lock = True
        if resource.status == 'READY' and self._pending[resource.name]:
            self.runtime.addSources([{'url': resource.unis_url, 'default': False, 'ssl': None, 'verify': False, 'enabled': True}])
            
            try:
                UnisClient.resolve(resource.unis_url)
            except (ConnectionError, UnisConnectionError):
                self._log.warn("Could not connect to remote unis - {}".format(resource.unis_url))
                type(self).lock = False
                return
            
            self.runtime.settings['default_source'] = resource.unis_url
            with Session(self.runtime, self._db.get_depots(), bs="5m", threads=1, viz_url=self._viz) as sess:
                for name, lifetime in self._pending[resource.name]:
                    upload = self.ForceUpload([resource.accessPoint])
                    try:
                        result = sess.upload(name, schedule=upload, duration=lifetime)
                        resource.new_exnodes.append(result.exnode)
                    except Exception as exp:
                        import traceback
                        traceback.print_exc()
                        type(self).lock = False
                        self._log.warn("Failed to upload to target - {}".format(exp))
                        return
                resource.status = 'UPDATE'
                self.runtime.update(resource)
                self.runtime.flush()
                self._pending[resource.name] = []
        type(self).lock = False
