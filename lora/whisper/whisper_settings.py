'''**************************************************************************

File: whisper_settings.py
Language: Python 3.6/7
Author: Juliette Zerick (jzerick@iu.edu)
        for the WildfireDLN Project
        OPEN Networks Lab at Indiana University-Bloomington

Included are variables representing settings and options used throughout
the code, such as the logger and the *BUCKETs used to provide a smooth
shutdown. Additionally there are utilities for checking the validity of
input data, and tests to show the utilities are working properly. 

Last modified: March 25, 2019

****************************************************************************'''

import datetime as dt
import time
import numpy as np
import re
from functools import reduce
from uuid import getnode
import logging
import queue
import subprocess
import socket
from contextlib import closing

# for CoAP, if so desired
#import asyncio 
#from aiocoap import *
#import aiocoap.resource as resource

import whisper_globals as wg

# Solution from synthesizerpatel (2013), then edited by Piotr Dobrogost (2014) at StackOverflow
# in response to the following posted question:
# "Python Logging (function name, file name, line number) using a single file" available at
# <https://stackoverflow.com/questions/10973362/python-logging-function-name-file-name-line-number-using-a-single-file>
# last accessed: November 27, 2018
log = logging.getLogger('root')
FORMAT = '[%(filename)s:%(lineno)s - %(funcName)12s() ] %(message)s'
logging.basicConfig(format=FORMAT)

# Solutions from pfa (2012) and edited by Shiplu Mokaddim (2012), et al. at StackOverflow
# in response to the following posted question:
# "How to add a custom loglevel to Python's logging facility" available at
# <https://stackoverflow.com/questions/2183233/how-to-add-a-custom-loglevel-to-pythons-logging-facility>
# last accessed: February 3, 2019
THREAD_STATUS_UPDATES_LEVEL_NUM = 1
PACKET_ERRORS_LEVEL_NUM = 2
PLUMBING_ISSUES_LEVEL_NUM = 3 
RTG_TABLE_UPDATES_LEVEL_NUM = 4
MSG_STORE_UPDATES_LEVEL_NUM = 5
UNIS_UPDATES_LEVEL_NUM = 6
DATA_FLOW_LEVEL_NUM = 7
RECEPTION_UPDATES_LEVEL_NUM = 8
DATA_FOR_TESSA_LEVEL_NUM = 9

# place the level number constants of the loggers you are interested in, as listed above
SELECTED_LOGGERS = [DATA_FLOW_LEVEL_NUM,DATA_FOR_TESSA_LEVEL_NUM]

counter = 11
for level in SELECTED_LOGGERS:
    if level == THREAD_STATUS_UPDATES_LEVEL_NUM: THREAD_STATUS_UPDATES_LEVEL_NUM = counter
    elif level == PACKET_ERRORS_LEVEL_NUM: PACKET_ERRORS_LEVEL_NUM = counter
    elif level == PLUMBING_ISSUES_LEVEL_NUM: PLUMBING_ISSUES_LEVEL_NUM = counter
    elif level == RTG_TABLE_UPDATES_LEVEL_NUM: RTG_TABLE_UPDATES_LEVEL_NUM = counter
    elif level == MSG_STORE_UPDATES_LEVEL_NUM: MSG_STORE_UPDATES_LEVEL_NUM = counter
    elif level == UNIS_UPDATES_LEVEL_NUM: UNIS_UPDATES_LEVEL_NUM = counter
    elif level == DATA_FLOW_LEVEL_NUM: DATA_FLOW_LEVEL_NUM = counter
    elif level == RECEPTION_UPDATES_LEVEL_NUM: RECEPTION_UPDATES_LEVEL_NUM = counter
    elif level == DATA_FOR_TESSA_LEVEL_NUM: DATA_FOR_TESSA_LEVEL_NUM = counter
    counter += 1
    
LOGGER_LEVEL = min(SELECTED_LOGGERS)

logging.addLevelName(THREAD_STATUS_UPDATES_LEVEL_NUM, "THREAD_STATUS_UPDATES")
def logger_thread_status_upates(self, message, *args, **kws):
    if self.isEnabledFor(THREAD_STATUS_UPDATES_LEVEL_NUM):
        # Yes, logger takes its '*args' as 'args'.
        self._log(THREAD_STATUS_UPDATES_LEVEL_NUM, message, args, **kws) 
logging.Logger.thread_status_updates = logger_thread_status_upates

logging.addLevelName(PACKET_ERRORS_LEVEL_NUM, "PACKET_ERRORS")
def logger_packet_errors(self, message, *args, **kws):
    if self.isEnabledFor(PACKET_ERRORS_LEVEL_NUM):
        # Yes, logger takes its '*args' as 'args'.
        self._log(PACKET_ERRORS_LEVEL_NUM, message, args, **kws) 
logging.Logger.packet_errors = logger_packet_errors

logging.addLevelName(PLUMBING_ISSUES_LEVEL_NUM, "PLUMBING_ISSUES")
def logger_plumbing_issues(self, message, *args, **kws):
    if self.isEnabledFor(UNIS_UPDATES_LEVEL_NUM):
        # Yes, logger takes its '*args' as 'args'.
        self._log(PLUMBING_ISSUES_LEVEL_NUM, message, args, **kws) 
logging.Logger.plumbing_issues = logger_plumbing_issues

logging.addLevelName(RTG_TABLE_UPDATES_LEVEL_NUM, "RTG_TABLE_UPDATES")
def logger_rtg_table_updates(self, message, *args, **kws):
    if self.isEnabledFor(RTG_TABLE_UPDATES_LEVEL_NUM):
        # Yes, logger takes its '*args' as 'args'.
        self._log(MSG_STORE_UPDATES_LEVEL_NUM, message, args, **kws) 
logging.Logger.rtg_table_updates = logger_rtg_table_updates

logging.addLevelName(MSG_STORE_UPDATES_LEVEL_NUM, "MSG_STORE_UPDATES")
def logger_msg_store_updates(self, message, *args, **kws):
    if self.isEnabledFor(MSG_STORE_UPDATES_LEVEL_NUM):
        # Yes, logger takes its '*args' as 'args'.
        self._log(MSG_STORE_UPDATES_LEVEL_NUM, message, args, **kws) 
logging.Logger.msg_store_updates = logger_msg_store_updates

logging.addLevelName(UNIS_UPDATES_LEVEL_NUM, "UNIS_UPDATES")
def logger_unis_updates(self, message, *args, **kws):
    if self.isEnabledFor(UNIS_UPDATES_LEVEL_NUM):
        # Yes, logger takes its '*args' as 'args'.
        self._log(UNIS_UPDATES_LEVEL_NUM, message, args, **kws) 
logging.Logger.unis_updates = logger_unis_updates

logging.addLevelName(DATA_FLOW_LEVEL_NUM, "DATA_FLOW")
def logger_data_flow(self, message, *args, **kws):
    if self.isEnabledFor(DATA_FLOW_LEVEL_NUM):
        # Yes, logger takes its '*args' as 'args'.
        self._log(DATA_FLOW_LEVEL_NUM, message, args, **kws) 
logging.Logger.data_flow = logger_data_flow

logging.addLevelName(RECEPTION_UPDATES_LEVEL_NUM, "RECEPTION_UPDATES")
def logger_reception_updates(self, message, *args, **kws):
    if self.isEnabledFor(RECEPTION_UPDATES_LEVEL_NUM):
        # Yes, logger takes its '*args' as 'args'.
        self._log(RECEPTION_UPDATES_LEVEL_NUM, message, args, **kws) 
logging.Logger.reception_updates = logger_reception_updates

logging.addLevelName(DATA_FOR_TESSA_LEVEL_NUM, "DATA_FOR_TESSA")
def data_for_tessa(self, message, *args, **kws):
    if self.isEnabledFor(DATA_FOR_TESSA_LEVEL_NUM):
        # Yes, logger takes its '*args' as 'args'.
        self._log(DATA_FOR_TESSA_LEVEL_NUM, message, args, **kws) 
logging.Logger.data_for_tessa = data_for_tessa

log.setLevel(LOGGER_LEVEL)

SNOOZE_TIME = 1. # in seconds

WHISPER_C_PROC_CALL = 'sudo ./whisper %d %d'

BUFFER_SIZE = 1024  # for a faster response, use a smaller value (256)

# buckets that will contain all threads, processes, and sockets used for easier mopup() 
THREAD_BUCKET = [] # one day I'll come up with better names
PROCESS_BUCKET = [] # but buckets are so versatile!
SOCKET_BUCKET = [] # they can be watering cans, trash cans, filing cabinets...
QUEUE_BUCKET = [] # vases, cat carriers, laundry hampers...

SIM_MODE = True

DEFAULT_LIFESPAN = 60*60*1 # in seconds
LIFETIME_EXTENSION = 5*60 # in seconds

TIME_OF_CREATION = dt.datetime(2018,10,6).timestamp()

MSG_COUNTER = 0

def now():
    return time.time()

INITIATION_TIME = now()

def start_whisper_c(outgoing_port,incoming_port):
    proc_call = WHISPER_C_PROC_CALL % (outgoing_port,incoming_port)
    log.info('summoning the c-handler via %s' % (proc_call))

    wg.whisper_c_p = subprocess.Popen(proc_call,shell=True,
        stdin=subprocess.PIPE,stdout=subprocess.PIPE,
        stderr=subprocess.PIPE) # for tidiness

def dump_process_bucket():
    for i in reversed(range(len(PROCESS_BUCKET))):
        PROCESS_BUCKET[i].terminate()
        del PROCESS_BUCKET[i]
        log.info('subprocess terminated')

def dispose_of_whisper_c():
    try:
        wg.whisper_c_p.terminate()
        del wg.whisper_c_p
        log.info('whisper_c_p disposed of')
    except:
        pass

# a clean disposal so we don't leave zombie processes hanging around. use in place of exit().
def mopup():
    log.info('cleaning up...')

    dump_process_bucket()
    dispose_of_whisper_c()

    for t in THREAD_BUCKET:
        del t
        log.info('thread terminated')

    # terminates if socket handle has at least one connection
    # zero connections => OSError: [Errno 9] Bad file descriptor
    for sock in SOCKET_BUCKET:
        try:
            sock.shutdown(socket.SHUT_RDWR)
            log.info('shutdown complete')
        except OSError:
            log.info('shutdown failed')
            pass
        
        try:
            sock.close()
            log.info('socket closure succeeded')
        except:
            log.info('socket closure failed')
            pass 
        
        log.info('socket closed')

def mopup_and_exit():
    mopup()
    exit()

# Solution from saaj (2017) at StackOverflow 
# in response to the following posted question:
# "On localhost, how do I pick a free port number?" available at
# <https://stackoverflow.com/questions/1365265/on-localhost-how-do-i-pick-a-free-port-number>
# last accessed: November 27, 2018
def find_free_port():
    with closing(socket.socket(socket.AF_INET, socket.SOCK_STREAM)) as s:
        s.bind(('', 0))
        return s.getsockname()[1]

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
    return (addr.replace(':','').replace('-','')).upper()

# Solution from Shifullah Ahmed Khan (2018) at StackOverflow
# in response to the following posted question:
# "Python - Get mac address" available at
# <https://stackoverflow.com/questions/28927958/python-get-mac-address>
# last accessed: November 27, 2018
def get_my_mac_addr():
    original_mac_address = getnode()
    hex_mac_address = str(":".join(re.findall('..', '%012x' % original_mac_address)))

    return normalize_addr(hex_mac_address)

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

    return True

    '''
    current_time = now()

    if TIME_OF_CREATION <= t and t <= current_time:
        return True

    return False
    '''

BAD_MESSAGE = 0

# send out data with which recipient nodes with which to update UNIS instances
MSG_TYPE_UNIS_POST_NOTIF = 1
MSG_TYPE_UNIS_POST_ACK_REQ = 2
MSG_TYPE_UNIS_POST_ACK = 3

# send out request for data from recipients' UNIS instances
MSG_TYPE_UNIS_GET_REQUEST = 4
MSG_TYPE_UNIS_GET_RESPONSE = 5

# updates, requests, responses for communicating current position (in GPS coordinates)
MSG_TYPE_POS_UPDATE = 6
MSG_TYPE_POS_REQUEST = 7
MSG_TYPE_POS_RESPONSE = 8

# generic query and response -- type specified in payload
MSG_TYPE_QUERY = 9
MSG_TYPE_QUERY_RESPONSE = 10

# generic notification and request for acknowledgment -- type specified in payload
MSG_TYPE_NOTIF = 11
MSG_TYPE_NOTIF_ACK_REQ = 12
MSG_TYPE_NOTIF_ACK = 13

MESSAGE_TYPES = [MSG_TYPE_UNIS_POST_NOTIF, MSG_TYPE_UNIS_POST_ACK_REQ, MSG_TYPE_UNIS_POST_ACK,
MSG_TYPE_UNIS_GET_REQUEST, MSG_TYPE_UNIS_GET_RESPONSE,
MSG_TYPE_POS_UPDATE, MSG_TYPE_POS_REQUEST, MSG_TYPE_POS_RESPONSE,
MSG_TYPE_QUERY, MSG_TYPE_QUERY_RESPONSE,
MSG_TYPE_NOTIF, MSG_TYPE_NOTIF_ACK_REQ, MSG_TYPE_NOTIF_ACK]

NOTIFICATIONS = [MSG_TYPE_UNIS_POST_NOTIF, 
MSG_TYPE_POS_UPDATE,
MSG_TYPE_NOTIF]

REQUESTS = [MSG_TYPE_UNIS_POST_ACK_REQ,
MSG_TYPE_UNIS_GET_REQUEST,
MSG_TYPE_POS_REQUEST,
MSG_TYPE_QUERY, 
MSG_TYPE_NOTIF_ACK_REQ]

RESPONSES = [MSG_TYPE_UNIS_POST_ACK,
MSG_TYPE_UNIS_GET_RESPONSE,
MSG_TYPE_POS_RESPONSE,
MSG_TYPE_QUERY_RESPONSE,
MSG_TYPE_NOTIF_ACK]

# messages of these types should be directed to the altar_keeper
ALTAR_KEEPER_DOMAIN = [MSG_TYPE_UNIS_POST_NOTIF, MSG_TYPE_UNIS_POST_ACK_REQ, MSG_TYPE_UNIS_POST_ACK,
MSG_TYPE_UNIS_GET_REQUEST, MSG_TYPE_UNIS_GET_RESPONSE]

# messages of these types should be directed to the cartographer
CARTOGRAPHER_DOMAIN = [MSG_TYPE_POS_UPDATE, MSG_TYPE_POS_REQUEST, MSG_TYPE_POS_RESPONSE]

def msg_type_is_defined(R):
    return R in MESSAGE_TYPES

def msg_type_is_notif(R):
    return R in NOTIFICATIONS

def msg_type_is_request(R):
    return R in REQUESTS

def msg_type_is_response(R):
    return R in RESPONSES

# these functions save a few conditional statementss
def get_notif_msg_type(R):
    if R in [MSG_TYPE_UNIS_POST_NOTIF, MSG_TYPE_UNIS_POST_ACK_REQ, MSG_TYPE_UNIS_POST_ACK]:
        return MSG_TYPE_UNIS_POST_NOTIF

    if R in [MSG_TYPE_UNIS_GET_REQUEST, MSG_TYPE_UNIS_GET_RESPONSE]:
        return BAD_MESSAGE
        
    if R in [MSG_TYPE_QUERY, MSG_TYPE_QUERY_RESPONSE]:
        return BAD_MESSAGE
        
    if R in [MSG_TYPE_POS_UPDATE, MSG_TYPE_POS_REQUEST, MSG_TYPE_POS_RESPONSE]:
        return MSG_TYPE_POS_UPDATE
        
    if R in [MSG_TYPE_NOTIF, MSG_TYPE_NOTIF_ACK_REQ, MSG_TYPE_NOTIF_ACK]:
        return MSG_TYPE_NOTIF
        
    return BAD_MESSAGE

def get_request_msg_type(R):
    if R in [MSG_TYPE_UNIS_POST_NOTIF, MSG_TYPE_UNIS_POST_ACK_REQ, MSG_TYPE_UNIS_POST_ACK]:
        return MSG_TYPE_UNIS_POST_ACK_REQ

    if R in [MSG_TYPE_UNIS_GET_REQUEST, MSG_TYPE_UNIS_GET_RESPONSE]:
        return MSG_TYPE_UNIS_GET_REQUEST
        
    if R in [MSG_TYPE_QUERY, MSG_TYPE_QUERY_RESPONSE]:
        return MSG_TYPE_QUERY
        
    if R in [MSG_TYPE_POS_UPDATE, MSG_TYPE_POS_REQUEST, MSG_TYPE_POS_RESPONSE]:
        return MSG_TYPE_POS_REQUEST
        
    if R in [MSG_TYPE_NOTIF, MSG_TYPE_NOTIF_ACK_REQ, MSG_TYPE_NOTIF_ACK]:
        return MSG_TYPE_NOTIF_ACK_REQ
        
    return BAD_MESSAGE

def get_response_msg_type(R):
    if R in [MSG_TYPE_UNIS_POST_NOTIF, MSG_TYPE_UNIS_POST_ACK_REQ, MSG_TYPE_UNIS_POST_ACK]:
        return MSG_TYPE_UNIS_POST_ACK

    if R in [MSG_TYPE_UNIS_GET_REQUEST, MSG_TYPE_UNIS_GET_RESPONSE]:
        return MSG_TYPE_UNIS_GET_RESPONSE
        
    if R in [MSG_TYPE_QUERY, MSG_TYPE_QUERY_RESPONSE]:
        return MSG_TYPE_QUERY_RESPONSE
        
    if R in [MSG_TYPE_POS_UPDATE, MSG_TYPE_POS_REQUEST, MSG_TYPE_POS_RESPONSE]:
        return MSG_TYPE_POS_RESPONSE
        
    if R in [MSG_TYPE_NOTIF, MSG_TYPE_NOTIF_ACK_REQ, MSG_TYPE_NOTIF_ACK]:
        return MSG_TYPE_NOTIF_ACK
        
    return BAD_MESSAGE

BAD_MESSAGE = 0

# send out data with which recipient nodes with which to update UNIS instances
MSG_TYPE_UNIS_POST_NOTIF = 1
MSG_TYPE_UNIS_POST_ACK_REQ = 2
MSG_TYPE_UNIS_POST_ACK = 3

# send out request for data from recipients' UNIS instances
MSG_TYPE_UNIS_GET_REQUEST = 4
MSG_TYPE_UNIS_GET_RESPONSE = 5

# updates, requests, responses for communicating current position (in GPS coordinates)
MSG_TYPE_POS_UPDATE = 6
MSG_TYPE_POS_REQUEST = 7
MSG_TYPE_POS_RESPONSE = 8

# generic query and response -- type specified in payload
MSG_TYPE_QUERY = 9
MSG_TYPE_QUERY_RESPONSE = 10

# generic notification and request for acknowledgment -- type specified in payload
MSG_TYPE_NOTIF = 11
MSG_TYPE_NOTIF_ACK_REQ = 12
MSG_TYPE_NOTIF_ACK = 13

msg_code2msg_text_d = {}

msg_code2msg_text_d[BAD_MESSAGE] = 'BAD_MESSAGE'
msg_code2msg_text_d[MSG_TYPE_UNIS_POST_NOTIF] = 'MSG_TYPE_UNIS_POST_NOTIF'
msg_code2msg_text_d[MSG_TYPE_UNIS_POST_ACK_REQ] = 'MSG_TYPE_UNIS_POST_ACK_REQ'
msg_code2msg_text_d[MSG_TYPE_UNIS_POST_ACK] = 'MSG_TYPE_UNIS_POST_ACK'
msg_code2msg_text_d[MSG_TYPE_UNIS_GET_REQUEST] = 'MSG_TYPE_UNIS_GET_REQUEST'
msg_code2msg_text_d[MSG_TYPE_UNIS_GET_RESPONSE] = 'MSG_TYPE_UNIS_GET_RESPONSE'
msg_code2msg_text_d[MSG_TYPE_POS_UPDATE] = 'MSG_TYPE_POS_UPDATE'
msg_code2msg_text_d[MSG_TYPE_POS_REQUEST] = 'MSG_TYPE_POS_REQUEST'
msg_code2msg_text_d[MSG_TYPE_POS_RESPONSE] = 'MSG_TYPE_POS_RESPONSE'
msg_code2msg_text_d[MSG_TYPE_QUERY] = 'MSG_TYPE_QUERY'
msg_code2msg_text_d[MSG_TYPE_QUERY_RESPONSE] = 'MSG_TYPE_QUERY_RESPONSE'
msg_code2msg_text_d[MSG_TYPE_NOTIF] = 'MSG_TYPE_NOTIF'
msg_code2msg_text_d[MSG_TYPE_NOTIF_ACK_REQ] = 'MSG_TYPE_NOTIF_ACK_REQ'
msg_code2msg_text_d[MSG_TYPE_NOTIF_ACK] = 'MSG_TYPE_NOTIF_ACK'

def msg_code2msg_desc(code):
    if code not in msg_code2msg_text_d:
        return 'BAD_CODE'

    return msg_code2msg_text_d[code]

# placeholder
RESERVED_DEFAULT = ''

RSSI_VAL_SUPREMUM = 0
BAD_RECEPTION = -90 # the threshold at which a message will be barely detected
MIN_SANE_RSSI_VAL = -200 # minimum observed thus far: -122

def is_plausible_RSSI_value(S):
    try:
        S0 = float(S)
    except:
        return False

    if S0 > RSSI_VAL_SUPREMUM:
        return False

    if S0 < MIN_SANE_RSSI_VAL:
        return False

    return True

MIN_LAT = -90.
MAX_LAT = 90.

# latitude has range in [-90,90]
def is_plausible_lat(c):
    try:
        c0 = float(c)
    except:
        return False

    if MIN_LAT < c0 and c0 < MAX_LAT:
        return True
    
    return False

MIN_LONG = -180.
MAX_LONG = 180.

# longitude has range in [-180,180]
def is_plausible_long(c):
    try:
        c0 = float(c)
    except:
        return False

    if MIN_LONG < c0 and c0 < MAX_LONG:
        return True
    
    return False

def are_plausible_GPS_coordinates(S):
    s0 = S.strip('(').strip(')')
    
    try:
        c0 = float(s0.split(',')[0])
        c1 = float(s0.split(',')[1])
    except:
        return False
        
    return is_plausible_lat(c0) and is_plausible_long(c1)
        
    # ordering assumed to be (latitude,longitude)

def get_GPS_coordinates(S):
    s0 = S.strip('(').strip(')')
    c0 = float(s0.split(',')[0])
    c1 = float(s0.split(',')[1])
    
    return c0, c1    
    
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
            init_send_time = S.split(',')[2]
        except:
            return False

        if not is_plausible_MAC_addr(init_sender_addr):
            return False

        if not is_plausible_timestamp(init_send_time):
            return False
    else:
        if len(S.split(',')) > 1:
            return False

    return True

def is_plausible_msg_type(S):
    try:
        msg_type = int(S)
    except:
        return False
        
    return msg_type_is_defined(msg_type)
    
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

    if normalize_addr('01:a2:3d:1d:9e:55') != '01a23d1d9e55':
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

    # [-180,180]
    if not is_plausible_long('-180'): return False
    if not is_plausible_long('180'): return False
    if not is_plausible_long('0.'): return False
    if not is_plausible_long('-0'): return False
    if is_plausible_long('-181'): return False
    if is_plausible_long('181'): return False  
    if is_plausible_long('q'): return False
    if is_plausible_long('180e'): return False

    # [-90,90]
    if not is_plausible_lat('-90'): return False
    if not is_plausible_lat('90'): return False
    if not is_plausible_lat('-0'): return False
    if not is_plausible_lat('0.'): return False
    if is_plausible_lat('-91'): return False
    if is_plausible_lat('91'): return False
    if is_plausible_lat('z'): return False
    if is_plausible_lat('-90e'): return False

    return True

GPS_DEV_LOC='/dev/ttyS0' # path/location to the Hat's GPS device
GPS_DEV_READ_LEN=50 # number of lines of output to read from said device
MAX_GPS_READ_ATTEMPTS=3 # number of times to attempt extraction of GPS coordinates

# the call to read the data
GPS_DEV_PROC_CALL='sudo cat %s | head -n %d' % (GPS_DEV_LOC,GPS_DEV_READ_LEN)

# small function to parse out latitude/longitude values from device output.
# this function naively assumes that the input string S contains the data
# needed and formatted as expected--explosions incurred from parsing failures
# are to be caught in the calling function.
def extract_coords(S):
    S0 = S.split(',')

    latitude = float(S0[2]) / 100.
    lat_dir = S0[3]
    longitude = float(S0[4]) / 100.
    long_dir = S0[5]

    if lat_dir == 'S':
        latitude = -latitude

    if long_dir == 'W':
        longitude = -longitude

    return (latitude,longitude)

BLOOMINGTON_LATITUDE = -87
BLOOMINGTON_LONGITUDE = 72

# attempts to retrieve the device's current GPS coordinates, reading
# GPS_DEV_READ_LEN lines of output from GPS_DEV_LOC per attempt, with
# at most MAX_GPS_READ_ATTEMPTS attempts. 
def retrieve_gps():
    latitude = BLOOMINGTON_LATITUDE 
    longitude = BLOOMINGTON_LONGITUDE

    for i in range(MAX_GPS_READ_ATTEMPTS):
        p = subprocess.Popen(GPS_DEV_PROC_CALL,shell=True,
                stdin=subprocess.PIPE,stdout=subprocess.PIPE,
                stderr=subprocess.PIPE) # for tidiness

        for j in range(GPS_DEV_READ_LEN):
            S = p.stdout.readline()

            # convert bytes->str (ASCII) if necessary
            if type(S) == bytes:
                try: # sometimes fails
                    S = S.decode('ascii')
                except: # conversion failed! try the next line
                    continue

            # now that we have a string, search it for an indicator
            # of the presence of GPS coordinate data
            if 'GPGGA' in S: # specifically this
                try: # attempt parsing
                    (latitude,longitude) = extract_coords(S)
                except: # parsing failed! try the next line
                    continue

                # parsing successful!
                p.kill() # cleanup

                log.info('Ferry location identified as %f,%f' % (latitude,longitude))
                return (latitude,longitude)

        # no line of output contained the data we needed. cleanup
        # and try again, if so desired.
        p.kill() 

        log.info('Ferry location estimated to be %f,%f' % (latitude,longitude))
        return (latitude,longitude)
