import falcon
import json

from idms.handlers.base import _BaseHandler
from idms.handlers.utils import get_body

class PolicyHandler(_BaseHandler):
    def __init__(self, conf, db, rt):
        self.rt = rt
        super().__init__(conf, db)

    #@falcon.before(_BaseHandler.do_auth)
    @falcon.after(_BaseHandler.encode_response)
    @get_body
    def on_post(self, req, resp, body):
        print(body)
        
    #@falcon.before(_BaseHandler.do_auth)
    @get_body
    def on_get(self, req, resp, body):
        resp.body = json.dumps(self._db._policies)
