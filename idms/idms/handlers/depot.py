import falcon
import json
import requests

from idms.handlers.base import _BaseHandler
from idms.handlers.utils import get_body

class DepotHandler(_BaseHandler):
    @falcon.after(_BaseHandler.encode_response)
    @get_body
    def on_post(self, req, resp, ref, body):
        self._db.manage_depots(ref, body['update'])
        resp.status = falcon.HTTP_200
        resp.body = json.dumps({'errorcode': None, "msg": ""})
