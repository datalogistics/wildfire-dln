import base64
import json

def get_body(fn):
    def _f(self, req, resp):
        body = {}
        if req.content_length:
            body = json.loads(req.stream.read().decode('utf-8'))
        fn(self, req, resp, body)
    
    return _f
