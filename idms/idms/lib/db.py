import copy

class DBLayer(object):
    def __init__(self, runtime, depots):
        self._rt = runtime
        self._custom_depots = depots or {}
        self._active = []
        
    def _get_ferry_list(self):
        return self._rt.services.where({"serviceType": "datalogistics:wdln:ferry"})
    def _get_base_list(self):
        return self._rt.services.where({"serviceType": "datalogistics:wdln:base"})
    
    def register_policy(self, policy):
        self._active.append(policy)
    def get_active_policies(self, exnode=None):
        if exnode:
            exnode = next(self._rt.exnodes.where({'id': exnode}))
            for policy in self._active:
                if policy.match(exnode):
                    yield policy
        else:
            for policy in self._active:
                yield policy
    
    def get_policies(self):
        dests = []
        for ferry in self._get_ferry_list():
            dests.append({"ferry_name": ferry.name, "data_lifetime": 108000})
        return dests
    
    def get_depots(self):
        depots = copy.deepcopy(self._custom_depots)
        for ferry in self._get_ferry_list():
            depots[ferry.accessPoint] = {"enabled": True}
        for depot in self._rt.services.where({"serviceType": "ibp_server"}):
            depots[depot.accessPoint] = {"enabled": True}
        for base in self._get_base_list():
            depots[base.accessPoint] = {"enabled": True}
        return depots
