import base64
#import bson
import falcon
import hashlib
import hmac
import json
import time

from idms.settings import MIME

class _BaseHandler(object):
    def __init__(self, conf, dblayer):
        self._conf = conf
        self._db = dblayer
    
    @classmethod
    def do_auth(self, req, resp, resource, params):
        if resource._conf.get('auth', False):
            if not req.auth:
                raise falcon.HTTPMissingHeader("Missing OAuth token", "Authorization")
            
            try:
                bearer, token = req.auth.split()
                assert(bearer == "OAuth")
            except AssertionError as exp:
                raise falcon.HTTPInvalidHeader("Malformed Authorization header", "Authorization")
            
            parts = token.split('.')
            if len(parts) != 3:
                raise falcon.HTTPUnauthorized("Token is not a valid JWT token")
            itok = ".".join(parts[:2])
            sig = hmac.new(resource._conf.get('secret', "there is no secret").encode('utf-8'), itok.encode('utf-8'), digestmod=hashlib.sha256).digest()
            if not hmac.compare_digest(base64.urlsafe_b64encode(sig), parts[2].encode('utf-8')):
                raise falcon.HTTPForbidden()
                
            payload = json.loads(base64.urlsafe_b64decode(parts[1]).decode('utf-8'))
            if payload["exp"] < int(time.time()):
                raise falcon.HTTPForbidden(description="Token has expired")
                
            if not resource.authorize(payload['prv']):
                raise falcon.HTTPForbidden(description="User does not have permission to use this function")
                
            self._usr = payload["iss"]
            
    @classmethod
    def encode_response(self, req, resp, resource):
        if not req.get_header("Accept"):
            raise falcon.HTTPMissingHeader("Accept")
        
        if req.client_accepts(MIME['PSJSON']) or req.client_accepts(MIME['JSON']):
            resp.body = json.dumps(resp.body)
        #elif req.client_accepts(MIME['PSBSON']) or req.client_accepts(MIME['BSON']):
        #    resp.body = bson.dumps(resp.body)
        
    def authorize(self, grants):
        return True
    

class SSLCheck(object):
    def __init__(self, conf):
        self._conf = conf
    
    def process_request(self, req, resp):
        if req.protocol != 'https':
            raise falcon.HTTPBadRequest(title='400 HTTPS required', 
                                        description='Flanged requires an SSL connection to authenticate requests')
        
    def process_resource(self, req, resp, resource, params):
        pass
    def process_response(self, req, resp, resource, req_suceeded):
        pass
