#!/usr/bin/env python

'''**************************************************************************

File: lora_layer.py
Language: Python 3.6.8
Author: Juliette Zerick (jzerick@iu.edu)
        for the WildfireDLN Project
        OPeN Networks Lab at Indiana University-Bloomington

This contains the higher-level protocol and switch behaviors. It interfaces
with lora_c.cpp, which quickly filters relevant messages that are passed
to lora_layer.py.

A number of threads were used to logically separate production and consumption
of messages for ease of troubleshooting. The use of queues adds a cushion to
avoid lost messages, providing some resiliency.

Last modified: September 10, 2019

****************************************************************************'''

import argparse

# bear in mind the importation of these modules will be executed above the
# modules' containing directory
import lora.bridge as deck
from lora.vessel import *
#from lora.sim import *

# Command-line option parsing solution based off an example in the docs, i.e.
# Python Documentation on argparse, available at
# <https://docs.python.org/3/library/argparse.html>
# last accessed: August 20, 2019
def handle_opts():
    parser = argparse.ArgumentParser(description='Life, the universe, and everything.')

    parser.add_argument('-m', '--manual', help='run lora_c manually',action="store_true")
    parser.add_argument('-r', '--receiver', help='receive only',action="store_true")
    parser.add_argument('-t', '--transmitter', help='transmit only',action="store_true")
    parser.add_argument('-e', '--emcee', help='use the emcee',action="store_true")
    parser.add_argument('-f', '--defcoords', help='set default GPS coordinates as \'(lat,long)\'')
    parser.add_argument('-b', '--buoy', \
        help='use fixed coordinates with artificial noise',action="store_true")
    parser.add_argument('-s', '--sim', help='simulation mode',action="store_true")

    args = parser.parse_args()

    '''
    e.g. output:
    
    minion@runabout:~/repobin/minionfest/whisper$ python3 parse_test.py --transmitter -f '(10,10)'
    Namespace(anim=False, buoy=False, defcoords='(10,10)', demo=False, receiver=False, sim=False, transmitter=True)
    '''

    # check for invalid combinations first
    
    # can't be in receive-only/transmit-only/emcee-only modes simultaneously
    # note that boolean addition (+) reverts to integer addition
    if sum([args.receiver,args.transmitter,args.emcee]) > 1:
        log.error('more than one mode was selected. ' + \
                'if you are not using the default transceiver behavior, ' + \
                'please select only ONE of the following modes:\n' + \
                '\treceiver mode\t\t--receiver\n'
                '\ttransmitter mode\t\t--transmitter\n' 
                '\temcee mode\t\t--emcee')
        return False
    
    # now sanity checks on input data

    # check whether coordinates make sense
    if args.defcoords != None:
        if not are_plausible_GPS_coordinates(args.defcoords): 
            log.error('specified GPS coordinates are implausible.')
            return False
            
        # otherwise, extract the data here
        arg_lat, arg_long = pluck_GPS_coordinates(args.defcoords)
        bridge.DEFAULT_LATITUDE = arg_lat
        bridge.BLOOMINGTON_LONGITUDE = arg_long

    # extract the remaining data
    
    bridge.SIM_MODE = args.sim
    bridge.RECEIVE_ONLY = args.receiver
    bridge.TRANSMIT_ONLY = args.transmitter
    bridge.USE_EMCEE = args.emcee
    bridge.USE_BUOY_EFFECT = args.buoy
    bridge.USING_LORA_C_HANDLER = not args.manual

    return True

def begin():
    signal.signal(signal.SIGINT, signal_handler)

    if not preflight_checks():
        log.critical('preflight checks failed, bailing!')
        exit(1)
    
    if bridge.SIM_MODE:
        log.info('simulation starting')
        sim = fleet()
        sim.run() 
    else:
        my_name = get_hostname()
        M = vessel(my_name)
        M.begin() 
  
    while not bridge.closing_time:
        time.sleep(SNOOZE_TIME)  

if __name__ == "__main__":
    if not handle_opts():
        log.critical('cannot understand command-line arguments, bailing!')
        exit(1)

    begin()


