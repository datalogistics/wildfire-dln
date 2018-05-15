import argparse
import falcon
import json

from idms.handlers import PolicyHandler, AuthHandler, SSLCheck
from idms.lib.service import IDMSService
from idms.lib.middleware import FalconCORS
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
                "ferry_name": "wdln-ferry-17",
                "data_lifetime": 108000
            },
            "Ferry-Drone-03": {
                "ferry_name": "wdln-ferry-03",
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
    
    def insert(self, filename, policy):
        if usr not in self._store:
            self._store[filename] = []
        self._store[filename].append(policy)

def _get_app(unis, depots, policies, viz):
    conf = { "auth": False, "secret": "a4534asdfsberwregoifgjh948u12" }
    db = _Database()
    rt = Runtime(unis, defer_update=True, preload=["nodes", "services"])
    service = IDMSService(depots, policies, viz)
    rt.addService(service)
    auth      = AuthHandler(conf, db)
    policy    = PolicyHandler(conf, db, service)
    
    ensure_ssl = SSLCheck(conf)
    app = falcon.API(middleware=[FalconCORS()])
    app.add_route('/', policy)
    
    return app
    
def main():
    from lace import logging
    from lace.logging import trace
    parser = argparse.ArgumentParser(description='')
    parser.add_argument('-u', '--unis', default='http://wdln-base-station:8888', type=str,
                        help='Set the comma diliminated urls to the unis instances of interest')
    parser.add_argument('-H', '--host', default='wdln-base-station', type=str, help='Set the host for the server')
    parser.add_argument('-p', '--port', default=8000, type=int, help='Set the port for the server')
    parser.add_argument('-d', '--debug', default="NONE", type=str, help='Set the log level')
    parser.add_argument('-D', '--depots', default='', type=str, help='Provide a file for the depot decriptions')
    parser.add_argument('-v', '--visualize', default='', type=str, help='Set the server for the visualization effects')
    parser.add_argument('-q', '--viz_port', default='42424', type=str, help='Set the port fo the visualization effects')
    args = parser.parse_args()
    
    level = {"NONE": logging.NOTSET, "INFO": logging.INFO, "DEBUG": logging.DEBUG}[args.debug]
    log = logging.getLogger()
    log.setLevel(level)
    trace.setLevel(level)
    trace.setLevel(level)
    port = args.port
    unis = [str(u) for u in args.unis.split(',')]
    depots = None
    if args.depots:
        with open(args.depots) as f:
            depots = json.load(f)
    viz = "{}:{}".format(args.visualize, args.viz_port) if args.visualize else None
    app = _get_app(unis, depots, args.policies, viz)
    
    from wsgiref.simple_server import make_server
    server = make_server(args.host, port, app)
    port = "" if port == 80 else port
    print("Getting topology from {}".format(unis))
    print("Listening on {}{}{}".format(args.host,":" if port else "", port))
    server.serve_forever()
    
if __name__ == "__main__":
    main()
