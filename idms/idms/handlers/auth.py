import base64
import falcon
import hashlib
import hmac
import json
import time

from idms import settings
from idms.handlers.base import _BaseHandler
from idms.handlers.utils import get_body

class AuthHandler(_BaseHandler):
    @falcon.after(_BaseHandler.encode_response)
    def on_get(self, req, resp):
        if not req.auth:
            raise falcon.HTTPUnauthorized("No username or password presented")
        
        auth = base64.b64decode(req.auth.split()[1]).decode('utf-8').split(':')
        header = { "alg": "HS256", "typ": "JWT" }
        payload = { 
            "iss": auth[0],
            "exp": int(time.time()) + settings.TOKEN_TTL,
            "prv": ",".join(self._db.get_usr(auth[0], auth[1]))
        }
        tok = self._generate_token(header, payload)
        
        resp.body = { "Bearer": tok }
        resp.status = falcon.HTTP_200
        
    def _generate_token(self, header, payload):
        b_header = base64.urlsafe_b64encode(json.dumps(header).encode('utf-8'))
        b_payload = base64.urlsafe_b64encode(json.dumps(payload).encode('utf-8'))
        itok = ".".join([b_header.decode('utf-8'), b_payload.decode('utf-8')])
        sig = hmac.new(self._conf.get('secret', "there is no secret").encode('utf-8'), itok.encode('utf-8'), digestmod=hashlib.sha256).digest()
        return ".".join([itok, base64.urlsafe_b64encode(sig).decode('utf-8')])
