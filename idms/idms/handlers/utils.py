import base64
import json

def get_body(fn):
    def _f(self, *args, **kwargs):
        if req.content_length:
            kwargs['body'] = json.loads(req.stream.read().decode('utf-8'))
        fn(self, *args, **kwargs)
    
    return _f
