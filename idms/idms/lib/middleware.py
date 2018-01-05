from falcon import HTTP_METHODS

class FalconCORS(object):
    PERMIT = ['*']
    
    def process_request(self, req, resp):
        pass
    
    def process_resource(self, req, resp, resource, kwargs):
        origin, method, headers = [req.get_header(x) for x in ['Origin', 'Access-Control-Request-Method', 'Access-Control-Request-Headers']]
        if req.method == 'OPTIONS' and origin and method and headers:
            methods = ','.join(sorted([m for m in HTTP_METHODS if hasattr(resource, 'on_'+m.lower())]))
            resp.set_header('Access-Control-Allow-Headers', headers)
            resp.set_header('Access-Control-Allow-Methods', methods)

    def process_response(self, req, resp, resource, req_succeeded):
        permit = set(self.PERMIT) & set(['*', req.get_header('Origin')])
        if permit:
            resp.set_header('Access-Control-Allow-Origin', list(permit)[0])
