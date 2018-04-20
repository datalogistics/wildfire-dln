import falcon
import json
import requests

from idms.handlers.base import _BaseHandler
from idms.handlers.utils import get_body

class PolicyHandler(_BaseHandler):
    def __init__(self, conf, db, service):
        self.service = service
        super().__init__(conf, db)
        
    #@falcon.before(_BaseHandler.do_auth)
    @falcon.after(_BaseHandler.encode_response)
    @get_body
    def on_post(self, req, resp, body):
        assert "policies" in body, "No policies specified in request"
        assert "nodes" in body, "No files specified in request"
        for policy in body['policies']:
            try:
                self.service.prepare(policy, [x['selfRef'] for x in body['nodes']])
            except KeyError as exp:
                resp.body = json.dumps({"errorcode": 2, "msg": "Malformed policy request"})
                resp.status = falcon.HTTP_401
                raise
        
        resp.body = json.dumps({"errorcode": None, "msg": ""})
        resp.status = falcon.HTTP_200
        
    #@falcon.before(_BaseHandler.do_auth)
    @get_body
    def on_get(self, req, resp, body):
        resp.body = json.dumps(self._db._policies)
