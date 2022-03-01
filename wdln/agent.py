#!/usr/bin/env python3
import bottle, threading, argparse, os, time

from wdln import settings
from wdln.config import MultiConfig
from asyncio import TimeoutError
from unis.exceptions import ConnectionError, UnisReferenceError
from wdln.ferry.ibp_iface import IBPWatcher
from wdln.ferry.agent import Agent
from lace import logging

log = logging.getLogger("wdln.app")
def agentloop(agent):
    def touch():
        err = 0
        agent.register()
        while err < agent.cfg['engine']['maxfail']:
            time.sleep(agent.cfg['engine']['interval'])
            try:
                if not agent.set_pos(): raise TimeoutError()
                agent.service.touch()
                log.info(f"[{agent.cfg['servicetype']}] Checking in...")
            except (ConnectionError, TimeoutError, UnisReferenceError) as e:
                log.warning("Could not update agent records")
                log.debug(f"-- {e}")
                err += 1

    while True:
        try:
            touch()
            time.sleep(agent.cfg['engine']['interval'])
        except (ConnectionError, TimeoutError, UnisReferenceError) as e:
            time.sleep(agent.cfg['engine']['interval'])
            log.warning("Re-registering agent...")
            log.debug(f"-- {e}")

def main():
    conf = MultiConfig(settings.DEFAULT_FERRY_CONFIG, "DLN ferry agent manages files hosted on the WDLN ferry",
                       filevar=settings.AGENT_CONFPATH)
    parser = argparse.ArgumentParser(description='')
    parser.add_argument('-T', '--servicetype', type=str, choices=["base", "ferry"], metavar="SERVICE",
                        help="Set service type for feature selection")
    parser.add_argument('-H', '--remote.host', type=str, metavar="HOST",
                        help='Remote UNIS instance host for registration and metadata')
    parser.add_argument('-P', '--remote.port', type=str, metavar="PORT",
                        help='Remote UNIS instance port for registration and metadata')
    parser.add_argument('-p', '--local.port', type=str, metavar="PORT",
                        help='Local UNIS port')
    parser.add_argument('-n', '--name', type=str,
                        help='Set ferry node name (ignore system hostname)')
    parser.add_argument('-l', '--localonly', action='store_true',
                        help='Run using only local UNIS instance (on-ferry)')
    parser.add_argument('-i', '--ibp', action='store_true',
                        help='Update IBP config to reflect interface changes on system')
    parser.add_argument('-V', '--version', action='store_true',
                        help='Display the current program version')
    conf = conf.from_parser(parser, include_logging=True)
    if conf['version']:
        from wdln.version import __version__
        print(f"v{__version__}")
        exit(0)
    if conf['ibp']: IBPWatcher()

    agent = Agent(conf)
    agentloop(agent)

if __name__ == "__main__":
    main()
