import falcon
import json

from idms.handlers.base import _BaseHandler

class PolicyTracker(_BaseHandler):
    def on_get(self, req, resp, exnode=None):
        resp.body = json.dumps([p.to_JSON() for p in self._db.get_active_policies(exnode)])
        resp.status = falcon.HTTP_200
