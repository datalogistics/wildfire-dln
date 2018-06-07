import falcon
import json
import requests

from idms.handlers.base import _BaseHandler
from idms.handlers.utils import get_body
from idms.lib.policy import Policy

class PolicyHandler(_BaseHandler):
    @falcon.after(_BaseHandler.encode_response)
    def on_get(self, req, resp):
        policies = {}
        policies["geo_north"] = {"description": "Send to Northwest Ferries",
                                 "ferry_name": "geo_north", "data_lifetime": 108000}
        policies["geo_south"] = {"description": "Send to Southeast Ferries",
                                 "ferry_name": "geo_south", "data_lifetime": 108000}
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
                subject = {"selfRef": {"$in": [x['selfRef'] for x in body['nodes']]}}
                ttl = policy.get('data_lifetime', 10800)
                if policy['ferry_name'] in ["geo_north", "geo_south"]:
                    if policy['ferry_name'] == 'geo_north':
                        ul = (32.709167, -117.160778)
                        ur = (32.704405, -117.165924)
                        lr = (32.708477, -117.172384)
                        ll = (32.712449, -117.167223)
                    else:
                        ul = (32.709167, -117.160778)
                        ur = (32.704405, -117.165924)
                        lr = (32.701435, -117.160541)
                        ll = (32.706334, -117.154630)
                    args = {"poly": [ul, ur, lr, ll], 'ttl': ttl}
                    verb = ("$or", {'policies': [("$geo", args), ("$replicate", {'copies': 2, 'ttl': ttl})]})
                else:
                    exact = ("$exact", {'dest': policy['ferry_name'], 'ttl': ttl})
                    replicate = ("$replicate", {'copies': 2, 'ttl': ttl})
                    verb = ("$or", {'policies': [exact, replicate]})
                policy = Policy(subject, verb)
                policy_id = self._db.register_policy(policy)
            except KeyError as exp:
                resp.body = json.dumps({"errorcode": 2, "msg": "Malformed policy request"})
                resp.status = falcon.HTTP_401
                raise
        
        resp.body = json.dumps({"errorcode": None, "msg": "", "policyid": policy_id})
        resp.status = falcon.HTTP_200
