import falcon
import json

from idms.handlers.base import _BaseHandler

class PolicyTracker(_BaseHandler):
    def on_get(self, req, resp, exnode=None):
        if exnode:
            policies = []
            exnode = next(self._db.exnodes.where({'id': exnode}))
            for policy in self._db.get_active_policies():
                if policy.match(exnode):
                    policies.append(policy.to_JSON())
        else:
            resp.body = json.dumps([p.to_JSON() for p in self._db.get_active_policies()])
            resp.status = falcon.HTTP_200
