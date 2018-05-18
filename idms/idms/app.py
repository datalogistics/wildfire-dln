import argparse
import falcon
import json

from idms.handlers import PolicyHandler, PolicyTracker, SSLCheck
from idms.lib.db import DBLayer
from idms.lib.middleware import FalconCORS
from idms.lib.service import IDMSService

from unis import Runtime

routes = {
    "p": {"handler": PolicyHandler},
    "a": {"handler": PolicyTracker},
    "a/{exnode}": {"handler": PolicyTracker}
}

def _get_app(unis, depots, viz):
    conf = {"auth": False, "secret": "a4534asdfsberwregoifgjh948u12"}
    rt = Runtime(unis, defer_update=True, preload=["nodes", "services"])
    db = DBLayer(rt, depots)
    service = IDMSService(db, viz)
    rt.addService(service)
    routes['p']['service'] = service
    
    ensure_ssl = SSLCheck(conf)
    app = falcon.API(middleware=[FalconCORS()])
    for k,v in routes.items():
        handler = v["handler"]
        del v["handler"]
        app.add_route("/{}".format(k), handler(conf, dblayer=db, **v))
    
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
    app = _get_app(unis, depots, viz)
    
    from wsgiref.simple_server import make_server
    server = make_server(args.host, port, app)
    port = "" if port == 80 else port
    print("Getting topology from {}".format(unis))
    print("Listening on {}{}{}".format(args.host,":" if port else "", port))
    server.serve_forever()
    
if __name__ == "__main__":
    main()
