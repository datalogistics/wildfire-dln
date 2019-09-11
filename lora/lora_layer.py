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

import lora.deck as deck
from lora.vessel import *
from lora.sim import *

# Command-line option parsing solution based off an example in the docs, i.e.
# Python Documentation on argparse, available at
# <https://docs.python.org/3/library/argparse.html>
# last accessed: August 20, 2019
def handle_opts():
    parser = argparse.ArgumentParser(description='Life, the universe, and everything.')

    parser.add_argument('-m', '--manual', help='run lora_c manually',action="store_true")
    parser.add_argument('-r', '--receiver', help='receive only',action="store_true")
    parser.add_argument('-t', '--transmitter', help='transmit only',action="store_true")
    parser.add_argument('-f', '--defcoords', help='set default GPS coordinates as \'(lat,long)\'')
    parser.add_argument('-d', '--demo', help='demo mode',action="store_true")
    parser.add_argument('-a', '--anim', help='live visualization of temperature',action="store_true") 
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
    
    # can't be in receive-only and transmit-only modes simultaneously
    if args.receiver and args.transmitter: 
        log.error('receive-only and transmit-only modes selected. ' + \
            'please pick --receiver, --transmitter, or transceiver mode (default)')
        return False
    
    # animation requires demonstration mode
    if args.anim and not args.demo: 
        log.error('animation (--anim) requires demonstration-mode enabled, i.e. --demo.')
        return False
    
    # a simulation isn't a demonstration
    if args.demo and args.sim: 
        log.error('simulations (--sim) and demonstrations (--demo) cannot be run simultaneously.')
        return False

    # now sanity checks on input data

    # check whether coordinates make sense
    if args.defcoords != None:
        if not are_plausible_GPS_coordinates(args.defcoords): 
            log.error('receive-only and transmit-only modes selected. ' + \
                'please pick --receiver, --transmitter, or transceiver mode (default)')
            return False
            
        # otherwise, extract the data here
        arg_lat, arg_long = pluck_GPS_coordinates(args.defcoords)
        deck.DEFAULT_LATITUDE = arg_lat
        deck.BLOOMINGTON_LONGITUDE = arg_long

    # extract the remaining data
    
    deck.DEMOING = args.demo
    deck.ANIMATING = args.anim
    deck.SIM_MODE = args.sim
    deck.RECEIVE_ONLY = args.receiver
    deck.TRANSMIT_ONLY = args.transmitter
    deck.USE_BUOY_EFFECT = args.buoy
    deck.USING_LORA_C_HANDLER = not args.manual

    return True

def begin():
    signal.signal(signal.SIGINT, signal_handler)

    if not preflight_checks():
        log.critical('preflight checks failed, bailing!')
        exit(1)
    
    if deck.SIM_MODE:
        log.info('simulation starting')
        sim = fleet()
        sim.run() 
    else:
        my_name = get_hostname()
        M = vessel(my_name)
        M.begin() 
  
    while not deck.closing_time:
        time.sleep(SNOOZE_TIME)  

if __name__ == "__main__":
    if not handle_opts():
        log.critical('cannot understand command-line arguments, bailing!')
        exit(1)

    begin()


