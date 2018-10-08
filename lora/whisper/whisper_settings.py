# settings and utilities to enforce them

import datetime as dt
import time
import numpy as np
import re
from functools import reduce
from uuid import getnode

'''
The class lora_message, defined in the file whisper_protocol.py (which imports this file), 
encapsulates the protocol and switch behavior of whisper-py. Relevant messages
(~packets) are received from whisper-c, fed to the inbox_q, then consumed.

The protocol uses ASCII messages (for now):

1) * for multicast or MAC address of recipient/

2) N>0 for saturation request (N=bloom count) or 0 for simple broadcast,
and if N>0 follows are: MAC address of the initial sender, epoch timestamp of the initial missive/

3) MAC address of last sender (which corresponds to RSSI value) /

4) epoch timestamp, combined with the sender address forms a message identifier/

5) message type/

6) payload/

7) reserved for future use:

8) When the packet is received by the handler, the : is removed and replaced with /
followed by: RSSI value|

completing the packet.

=> complete packet contains eight /-separated fields followed by a |

Note: the data a packet contains can be uniquely identified with a device address
and timestamp so long as devices have a single transceiver, or multiple transceivers
that transmit sequentially. 
'''

DEFAULT_LIFESPAN = 60*60*1 # in seconds
LIFETIME_EXTENSION = 5*60 # in seconds

TIME_OF_CREATION = dt.datetime(2018,10,6).timestamp()

def now():
    return time.time()

INITIATION_TIME = now()

# https://stackoverflow.com/questions/28927958/python-get-mac-address
# Shifullah Ahmed Khan@StackOverflow
def get_my_MAC_addr():
    original_mac_address = getnode()
    hex_mac_address = str(":".join(re.findall('..', '%012x' % original_mac_address)))

    return hex_mac_address

MY_MAC_ADDR = get_my_MAC_addr()

# is it six octets, representing a 48-bit integer?
def is_plausible_MAC_addr(addr):
    octet = '[a-fA-F0-9]{2,2}'

    def get_pattern(punc):
        return reduce(lambda x,y: x+punc+y,6*[octet])

    regex_with_colon = get_pattern(':')
    regex_with_dash = get_pattern('-')
    regex_without_punc = '[a-fA-F0-9]{12,12}'

    pattern_with_colon = re.compile(regex_with_colon)
    pattern_with_dash = re.compile(regex_with_dash)
    pattern_without_punc = re.compile(regex_without_punc)

    patterns = [pattern_with_colon,pattern_with_dash,pattern_without_punc]

    for pattern in patterns:
        if pattern.match(addr) != None:
            return True

    return False

def normalize_addr(addr):
    return addr.replace(':','').replace('-','').upper()

MESSAGE_DELIMITER = '/'
MESSAGE_TERMINATOR = '|'

# to be removed uppon receipt
TRANSIENT_TERMINATOR = ':'

MULTICAST = '*'

NUM_PROTOCOL_FIELDS = 8

def is_plausible_timestamp(T):
    try:
        t = float(T)
    except:
        return False

    current_time = now()

    if TIME_OF_CREATION <= t and t <= current_time:
        return True

    return False

BAD_MESSAGE = 0

MSG_TYPE_UNIS_POST_NOTIF = 1
MSG_TYPE_UNIS_POST_REQUEST = 2
MSG_TYPE_UNIS_POST_RESPONSE = 3

MSG_TYPE_UNIS_GET_REQUEST = 4
MSG_TYPE_UNIS_GET_RESPONSE = 5

MSG_TYPE_QUERY = 6
MSG_TYPE_QUERY_RESPONSE = 7

MSG_TYPE_POS_UPDATE = 8
MSG_TYPE_POS_REQUEST = 9
MSG_TYPE_POS_RESPONSE = 10

MSG_TYPE_NOTIF = 11
MSG_TYPE_NOTIF_RESPONSE = 12

MESSAGE_TYPES = [MSG_TYPE_UNIS_POST_NOTIF, MSG_TYPE_UNIS_POST_REQUEST, MSG_TYPE_UNIS_POST_RESPONSE,
MSG_TYPE_UNIS_GET_REQUEST, MSG_TYPE_UNIS_GET_RESPONSE,
MSG_TYPE_QUERY, MSG_TYPE_QUERY_RESPONSE,
MSG_TYPE_POS_UPDATE, MSG_TYPE_POS_REQUEST, MSG_TYPE_POS_RESPONSE,
MSG_TYPE_NOTIF, MSG_TYPE_NOTIF_RESPONSE]

NOTIFICATIONS = [MSG_TYPE_UNIS_POST_NOTIF, 
MSG_TYPE_POS_UPDATE,
MSG_TYPE_NOTIF]

REQUESTS = [MSG_TYPE_UNIS_POST_REQUEST,
MSG_TYPE_UNIS_GET_REQUEST,
MSG_TYPE_QUERY, 
MSG_TYPE_POS_REQUEST]

RESPONSES = [MSG_TYPE_UNIS_POST_RESPONSE,
MSG_TYPE_UNIS_GET_RESPONSE,
MSG_TYPE_QUERY_RESPONSE,
MSG_TYPE_POS_RESPONSE,
MSG_TYPE_NOTIF_RESPONSE]

def msg_type_is_defined(R):
    return R in MESSAGE_TYPES

def msg_type_is_notif(R):
    return R in NOTIFICATIONS

def msg_type_is_request(R):
    return R in REQUESTS

def msg_type_is_response(R):
    return R in RESPONSES

def get_notif(R)
    if R in [MSG_TYPE_UNIS_POST_NOTIF, MSG_TYPE_UNIS_POST_REQUEST, MSG_TYPE_UNIS_POST_RESPONSE]:
        return MSG_TYPE_UNIS_POST_NOTIF

    if R in [MSG_TYPE_UNIS_GET_REQUEST, MSG_TYPE_UNIS_GET_RESPONSE]:
        return BAD_MESSAGE
        
    if R in [MSG_TYPE_QUERY, MSG_TYPE_QUERY_RESPONSE]:
        return BAD_MESSAGE
        
    if R in [MSG_TYPE_POS_UPDATE, MSG_TYPE_POS_REQUEST, MSG_TYPE_POS_RESPONSE]:
        return MSG_TYPE_POS_UPDATE
        
    if R in [MSG_TYPE_NOTIF, MSG_TYPE_NOTIF_RESPONSE]:
        return MSG_TYPE_NOTIF
        
    return BAD_MESSAGE

def get_request(R)
    if R in [MSG_TYPE_UNIS_POST_NOTIF, MSG_TYPE_UNIS_POST_REQUEST, MSG_TYPE_UNIS_POST_RESPONSE]:
        return MSG_TYPE_UNIS_POST_REQUEST

    if R in [MSG_TYPE_UNIS_GET_REQUEST, MSG_TYPE_UNIS_GET_RESPONSE]:
        return MSG_TYPE_UNIS_GET_REQUEST
        
    if R in [MSG_TYPE_QUERY, MSG_TYPE_QUERY_RESPONSE]:
        return MSG_TYPE_QUERY
        
    if R in [MSG_TYPE_POS_UPDATE, MSG_TYPE_POS_REQUEST, MSG_TYPE_POS_RESPONSE]:
        return MSG_TYPE_POS_REQUEST
        
    if R in [MSG_TYPE_NOTIF, MSG_TYPE_NOTIF_RESPONSE]:
        return BAD_MESSAGE
        
    return BAD_MESSAGE

def get_response(R)
    if R in [MSG_TYPE_UNIS_POST_NOTIF, MSG_TYPE_UNIS_POST_REQUEST, MSG_TYPE_UNIS_POST_RESPONSE]:
        return MSG_TYPE_UNIS_POST_RESPONSE

    if R in [MSG_TYPE_UNIS_GET_REQUEST, MSG_TYPE_UNIS_GET_RESPONSE]:
        return MSG_TYPE_UNIS_GET_RESPONSE
        
    if R in [MSG_TYPE_QUERY, MSG_TYPE_QUERY_RESPONSE]:
        return MSG_TYPE_QUERY_RESPONSE
        
    if R in [MSG_TYPE_POS_UPDATE, MSG_TYPE_POS_REQUEST, MSG_TYPE_POS_RESPONSE]:
        return MSG_TYPE_POS_RESPONSE
        
    if R in [MSG_TYPE_NOTIF, MSG_TYPE_NOTIF_RESPONSE]:
        return MSG_TYPE_NOTIF_RESPONSE
        
    return BAD_MESSAGE

# placeholder
RESERVED_DEFAULT = ''

MAX_POSSIBLE_RSSI_VAL = 0
BAD_RECEPTION = -90 # the threshold at which a message will be barely detected
MIN_SANE_RSSI_VAL = -100

def is_plausible_RSSI_value(S):
    try:
        S0 = float(S)
    except:
        return False

    if S0 > MAX_POSSIBLE_RSSI_VAL:
        return False

    if S0 < MIN_SANE_RSSI_VAL:
        return False

    return True

NO_SATURATION = 0

def is_plausible_bloom(S):
    try:
        bloom_count = int(S.split(',')[0])
    except:
        return False

    if bloom_count < 0:
        return False

    if bloom_count != NO_SATURATION:
        try:
            init_sender_addr = S.split(',')[1]
            init_msg_time = S.split(',')[2]
        except:
            return False

        if not is_plausible_MAC_addr(init_sender_addr):
            return False

        if not is_plausible_timestamp(init_msg_time):
            return False
    else:
        if len(S.split(',')) > 1:
            return False

    return True
    
def test_tests():
    if not is_plausible_MAC_addr('01:a2:3d:1d:9e:55'): return False
    if not is_plausible_MAC_addr('01-a2-3d-1d-9e-55'): return False
    if not is_plausible_MAC_addr('01a23d1d9e55'): return False
    if not is_plausible_MAC_addr('01A23d1d9e55'): return False
    if is_plausible_MAC_addr('01a23d19e55'): return False
    if is_plausible_MAC_addr('01a23d1d9g55'): return False

    if is_plausible_RSSI_value('5'): return False
    if not is_plausible_RSSI_value('-100'): return False
    if not is_plausible_RSSI_value('0'): return False
    if is_plausible_RSSI_value('A'): return False
    if not is_plausible_RSSI_value('-21.5'): return False

    if normalize_addr('01:a2:3d:1d:9e:55') != '01A23D1D9E55':
        return False

    for R in RESPONSES:
        if get_request(R) != R - 1: return False
        if not msg_type_is_response(R): return False      
        if msg_type_is_request(R): return False
    
    for R in REQUESTS:
        if get_response(R) != R + 1: return False
        if not msg_type_is_request(R): return False      
        if msg_type_is_response(R): return False

    current_time = now() - 10

    if not is_plausible_bloom('0'): return False
    if not is_plausible_bloom('1,01:a2:3d:1d:9e:55,%s' % current_time): return False
    if is_plausible_bloom('1,01:a2:3d:1d:9e:55,'): return False
    if is_plausible_bloom('0,01:a2:3d:1d:9e:55,%s' % current_time): return False
    if is_plausible_bloom('1,:a2:3d:1d:9e:55,%s' % current_time): return False
    if is_plausible_bloom('-1,01:a2:3d:1d:9e:55,%s' % current_time): return False
    if is_plausible_bloom('1,,%s' % current_time): return False
    if is_plausible_bloom('1'): return False

    return True
