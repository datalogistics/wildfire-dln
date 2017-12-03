import argparse
import falcon
import json

from idms.handlers import PolicyHandler, AuthHandler, SSLCheck
from unis import Runtime

# TMP Mock database object
class _Database(object):
    def __init__(self):
        self._store = {}
        self._policies = {
            "Ferry-Backpack-00": {
                "ferry_name": "wdln-ferry-00",
                "data_lifetime": 21600
            },
            "Ferry-Mule-17": {
                "ferry-name": "wdln-ferry-17",
                "data_lifetime": 108000
            },
            "Ferry-Drone-03": {
                "ferry-name": "wdln-ferry-03",
                "data-lifetime": 3600
            }
        }
        self._usrs = {
            "admin": { "pwd": "admin", "prv": ["ls", "x", "v"] },
            "reader": { "pwd": "reader", "prv": ["ls"] },
            "programmer": { "pwd": "programmer", "prv": ["x"] },
            "qa": { "pwd": "qa", "prv": ["v"] }
        }
        
    ### DO NOT ACTUALLY USE THIS, IT IS horribly INSECURE ###
    def get_usr(self, usr, pwd):
        for k, v in self._usrs.items():
            if k == usr:
                if v.get("pwd", "") != pwd:
                    raise falcon.HTTPForbidden("Incorrect password")
            
                return v["prv"]
        raise falcon.HTTPForbidden("Unknown username")
        
    def find(self, usr=None):
        for k, ls in self._store.items():
            if not usr or usr == k:
                for f in ls:
                    yield f

    def insert(self, usr, flangelet):
        if usr not in self._store:
            self._store[usr] = []
        self._store[usr].append(flangelet)

def _get_app(unis):
    conf = { "auth": False, "secret": "a4534asdfsberwregoifgjh948u12" }
    db = _Database()
    rt = Runtime(unis, defer_update=True)
    
    auth      = AuthHandler(conf, db)
    policy    = PolicyHandler(conf, db, rt)
    
    ensure_ssl = SSLCheck(conf)
    #app = falcon.API(middleware=[ensure_ssl])
    app = falcon.API()
    app.add_route('/', policy)
    
    return app
    
def main():
    from lace import logging
    parser = argparse.ArgumentParser(description='')
    parser.add_argument('-u', '--unis', default='http://localhost:8888', type=str,
                        help='Set the comma diliminated urls to the unis instances of interest')
    parser.add_argument('-p', '--port', default=8000, type=int, help='Set the port for the server')
    parser.add_argument('-d', '--debug', default=0, type=int, help='Set the log level')
    args = parser.parse_args()
    
    logging.trace.setLevel([logging.NOTSET, logging.INFO, logging.DEBUG][args.debug])
    port = args.port
    unis = [str(u) for u in args.unis.split(',')]
    print(unis)
    app = _get_app(unis)
    
    from wsgiref.simple_server import make_server
    server = make_server('localhost', port, app)
    port = "" if port == 80 else port
    print("Getting topology from {}".format(unis))
    print("Listening on {}{}{}".format('http://localhost',":" if port else "", port))
    server.serve_forever()
    
if __name__ == "__main__":
    main()