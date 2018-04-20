import falcon
import json
<<<<<<< HEAD
import requests
=======
>>>>>>> 0552500d73c329fec28da4aaa9de1a87c005f82a

from idms.handlers.base import _BaseHandler
from idms.handlers.utils import get_body

class PolicyHandler(_BaseHandler):
<<<<<<< HEAD
    def __init__(self, conf, db, service):
        self.service = service
        super().__init__(conf, db)
        
=======
    def __init__(self, conf, db, rt):
        self.rt = rt
        super().__init__(conf, db)

>>>>>>> 0552500d73c329fec28da4aaa9de1a87c005f82a
    #@falcon.before(_BaseHandler.do_auth)
    @falcon.after(_BaseHandler.encode_response)
    @get_body
    def on_post(self, req, resp, body):
<<<<<<< HEAD
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
        
=======
        assert "nodes" in body and "policies" in body, "Bad request form client"
        if any(['ferry_name' not in x for x in body['policies']]):
            resp.body = json.dumps({"errorcode": 1, "msg": "Policy request contained malformed policy requests"})
            resp.status = falcon.HTTP_401
            print(body['policies'])
            raise ValueError("Invalid format in policy - {}".format(body))
        for policy in body['policies']:
            try:
                policy = self._db._policies[policy['ferry_name']]
                print(policy)
                self.rt.pending_exnodes[policy['ferry_name']].extend(list(map(lambda x: (self.rt.find(x['selfRef']), policy['data_lifetime']), body['nodes'])))
            except KeyError as exp:
                resp.body = json.dumps({"errorcode": 2, "msg": "Policy request contained unknown policy name"})
                resp.status = falcon.HTTP_401
                raise
        
        resp.body   = json.dumps({"errorcode": None, "msg": ""})
        resp.status = falcon.HTTP_200
    
>>>>>>> 0552500d73c329fec28da4aaa9de1a87c005f82a
    #@falcon.before(_BaseHandler.do_auth)
    @get_body
    def on_get(self, req, resp, body):
        resp.body = json.dumps(self._db._policies)
