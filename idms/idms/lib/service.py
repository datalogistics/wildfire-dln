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

from idms import engine

class IDMSService(RuntimeService):
    targets = [Service, Exnode]
    
    def __init__(self, dblayer):
        super(IDMSService, self).__init__()
        self._db = dblayer
    def new(self, resource):
        self._db.update_policies(resource)
    def update(self, resource):
        self._db.update_policies(resource)
