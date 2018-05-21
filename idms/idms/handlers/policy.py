import falcon
import json
import requests

from idms.handlers.base import _BaseHandler
from idms.handlers.utils import get_body
from idms.lib.policy import Policy

class PolicyHandler(_BaseHandler):
    def __init__(self, conf, dblayer, service):
        self._service = service
        super().__init__(conf, dblayer)
    
    @falcon.after(_BaseHandler.encode_response)
    def on_get(self, req, resp):
        policies = {}
        for dest in self._db.get_policies():
            name = "to_{}".format(dest['ferry_name'])
            dest['description'] = "Send to {}".format(dest['ferry_name'])
            policies[name] = dest
        resp.body = policies
        resp.status = falcon.HTTP_200
    
    #@falcon.before(_BaseHandler.do_auth)
    @falcon.after(_BaseHandler.encode_response)
    @get_body
    def on_post(self, req, resp, body):
        assert "policies" in body, "No policies specified in request"
        assert "nodes" in body, "No nodes specified in request"
        for policy in body['policies']:
            try:
                desc = {"selfRef": {"$in": [x['selfRef'] for x in body['nodes']]}}
                sendto = { "$exact": policy['ferry_name']}
                policy = Policy(desc, sendto, {"data_lifetime": policy.get('data_lifetime', 10800)})
                self._db.register_policy(policy)
                self._service.prepare(policy)
            except KeyError as exp:
                resp.body = json.dumps({"errorcode": 2, "msg": "Malformed policy request"})
                resp.status = falcon.HTTP_401
                raise
        
        resp.body = json.dumps({"errorcode": None, "msg": ""})
        resp.status = falcon.HTTP_200
