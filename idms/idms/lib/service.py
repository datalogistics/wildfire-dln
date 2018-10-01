from unis.services import RuntimeService
from unis.services.event import new_update_event

class IDMSService(RuntimeService):
    def __init__(self, dblayer):
        self._db = dblayer

    @new_update_event(["services", "exnodes"])
    def new(self, resource):
        self._db.update_policies(resource)
