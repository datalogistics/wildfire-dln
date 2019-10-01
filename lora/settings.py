'''**************************************************************************

File: settings.py
Language: Python 3.6.8
Author: Juliette Zerick (jzerick@iu.edu)
        for the WildfireDLN Project
        OPeN Networks Lab at Indiana University-Bloomington

Included are variables representing settings and options used throughout
the code, such as the logger and the *BUCKETs used to provide a smooth
shutdown. Additionally there are utilities for checking the validity of
input data, and tests to show the utilities are working properly. 

Last modified: September 10, 2019

****************************************************************************'''

from datetime import datetime
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
import pandas as pd
import random
import sys
import pathlib
import os
import argparse
from colorama import init, Fore, Back, Style
import requests

# bear in mind the importation of these modules will be executed above the
# modules' containing directory
import bridge

if bridge.HAVE_UNIS:
    try:
        #from unis import Runtime # is there a difference?
        from unis.runtime import Runtime   
        from unis.models import Node, schemaLoader
        from unis.models import Metadata
        rt = Runtime('http://localhost:9000')
        print('able to import everything!')
    except: # possible alternative, depending on the environment
        try: # in this case, Ubuntu 16.04 via Windows Subsystem for Linux
            sys.path.append('/home/minion/repobin/Lace')
            sys.path.append('/home/minion/repobin/UNISrt')
            
            # once more with less fail?
            from unis import Runtime  
            #from unis.runtime import Runtime   
            from unis.models import Node, schemaLoader
            from unis.models import Metadata
            rt = Runtime('http://localhost:9000')
            print('able to import everything!')
        except:
            pass

###############################################################################
#  FLAGS AND SETTINGS
#  Below are flags and settings with which to configure behavior.
###############################################################################

EPS = 10e-4 # a not-so-small epsilon used in timing

SNOOZE_TIME = 1. # in seconds
MICROSNOOZE_TIME = 0.01 # in seconds
SIM_MODE = False
BUFFER_SIZE = 1024  # for a faster response, use a smaller value (256)

# flags to indicate which notifications and updates are desired
WANT_THREAD_STATUS_UPDATES = True
WANT_PACKET_ERRORS = True
WANT_PLUMBING_ISSUES = True
WANT_RTG_TABLE_UPDATES = True
WANT_UNIS_UPDATES = True
WANT_DATA_FLOW = True
WANT_RECEPTION_UPDATES = True

INIT_LOG_NUM_COUNTER = 11

# Solution from Geeks for Geeks, author not identified.
# "Print Colors in Python terminal," post date unknown, available at:
# <https://www.geeksforgeeks.org/print-colors-python-terminal/>.
# last accessed: August 30, 2019.
init() # = colorama.init()

###############################################################################
#  LOGGING
#  Below are methods for handling logging. 
###############################################################################

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
counter = INIT_LOG_NUM_COUNTER

THREAD_STATUS_UPDATES_LEVEL_NUM = counter
logging.addLevelName(THREAD_STATUS_UPDATES_LEVEL_NUM, "THREAD_STATUS_UPDATES")
def logger_thread_status_upates(self, message, *args, **kws):
    if self.isEnabledFor(THREAD_STATUS_UPDATES_LEVEL_NUM) and WANT_THREAD_STATUS_UPDATES: 
        self._log(THREAD_STATUS_UPDATES_LEVEL_NUM, Fore.BLUE+translate_text(message)+Fore.WHITE, args, **kws) 
logging.Logger.thread_status_updates = logger_thread_status_upates
counter += 1

PACKET_ERRORS_LEVEL_NUM = counter
logging.addLevelName(PACKET_ERRORS_LEVEL_NUM, "PACKET_ERRORS")
def logger_packet_errors(self, message, *args, **kws):
    if self.isEnabledFor(PACKET_ERRORS_LEVEL_NUM) and WANT_PACKET_ERRORS:
        self._log(PACKET_ERRORS_LEVEL_NUM, Fore.YELLOW+translate_text(message)+Fore.WHITE, args, **kws) 
logging.Logger.packet_errors = logger_packet_errors
counter += 1

PLUMBING_ISSUES_LEVEL_NUM = counter
logging.addLevelName(PLUMBING_ISSUES_LEVEL_NUM, "PLUMBING_ISSUES")
def logger_plumbing_issues(self, message, *args, **kws):
    if self.isEnabledFor(PLUMBING_ISSUES_LEVEL_NUM and WANT_PLUMBING_ISSUES):
        self._log(PLUMBING_ISSUES_LEVEL_NUM, Fore.RED+translate_text(message)+Fore.WHITE, args, **kws) 
logging.Logger.plumbing_issues = logger_plumbing_issues
counter += 1

RTG_TABLE_UPDATES_LEVEL_NUM = counter
logging.addLevelName(RTG_TABLE_UPDATES_LEVEL_NUM, "RTG_TABLE_UPDATES")
def logger_rtg_table_updates(self, message, *args, **kws):
    if self.isEnabledFor(RTG_TABLE_UPDATES_LEVEL_NUM) and WANT_RTG_TABLE_UPDATES:
        self._log(RTG_TABLE_UPDATES_LEVEL_NUM, Fore.MAGENTA+translate_text(message)+Fore.WHITE, args, **kws) 
logging.Logger.rtg_table_updates = logger_rtg_table_updates
counter += 1

UNIS_UPDATES_LEVEL_NUM = counter
logging.addLevelName(UNIS_UPDATES_LEVEL_NUM, "UNIS_UPDATES")
def logger_unis_updates(self, message, *args, **kws):
    if self.isEnabledFor(UNIS_UPDATES_LEVEL_NUM) and WANT_UNIS_UPDATES:
        self._log(UNIS_UPDATES_LEVEL_NUM, Fore.GREEN+translate_text(message)+Fore.WHITE, args, **kws) 
logging.Logger.unis_updates = logger_unis_updates
counter += 1

DATA_FLOW_LEVEL_NUM = counter
logging.addLevelName(DATA_FLOW_LEVEL_NUM, "DATA_FLOW")
def logger_data_flow(self, message, *args, **kws):
    if self.isEnabledFor(DATA_FLOW_LEVEL_NUM) and WANT_DATA_FLOW:
        self._log(DATA_FLOW_LEVEL_NUM, Fore.CYAN+translate_text(message)+Fore.WHITE, args, **kws) 
logging.Logger.data_flow = logger_data_flow
counter += 1

RECEPTION_UPDATES_LEVEL_NUM = counter
logging.addLevelName(RECEPTION_UPDATES_LEVEL_NUM, "RECEPTION_UPDATES")
def logger_reception_updates(self, message, *args, **kws):
    if self.isEnabledFor(RECEPTION_UPDATES_LEVEL_NUM) and WANT_RECEPTION_UPDATES:
        self._log(RECEPTION_UPDATES_LEVEL_NUM, Fore.WHITE+translate_text(message)+Fore.WHITE, args, **kws) 
logging.Logger.reception_updates = logger_reception_updates
counter += 1

log.setLevel(INIT_LOG_NUM_COUNTER)

###############################################################################
#  COMMAND-LINE PARAMETER PARSING
#  Below are methods for handling parsing of command-line parameters. 
###############################################################################

parser = argparse.ArgumentParser(description='Life, the universe, and everything.')

###############################################################################
#  READABILITY
#  Below are methods for making output data and logging more readable. 
###############################################################################

def couple_digits():
    return str(random.randint(0,1000))

def datetime_now():
    return datetime.today().strftime('%Y-%m-%d_%H-%M-')

def readable_ts(s):
    return datetime.fromtimestamp(s).strftime('%Y-%m-%d %H:%M:%S')

def readable_time(t0):
    try:
        t = float(t0)
    except:
        return t0
        
    if t - bridge.sim_start > 0:
        t = t - bridge.sim_start
        t = t*1 # some kind of fudge factor
   
    return round(t,2)

def translate_time(var_name): # a literal translation.
    # note that if var_name is actually a numerical entry, type(var_name)
    # will not be float, i.e. type(var_name)==float returns False.\
    # however, if it's a string, type(var_name)==str will return True.
    if type(var_name) != str:
        return readable_time(var_name)

    # Solution from miku (January 16, 2011) at StackOverflow
    # in response to the following posted question:
    # "How to extract a floating number from a string [duplicate]" available at
    # https://stackoverflow.com/questions/4703390/how-to-extract-a-floating-number-from-a-string<>
    # last accessed: June 28, 2019
    numbers = re.findall(r"\d+\.\d+",var_name)
    
    if len(numbers) > 0:
        for sn0 in numbers:
            sn = str(readable_time(sn0))
            var_name = var_name.replace(sn0,sn)
            
    return var_name

def translate_text(V):
    for dev_id in bridge.dev_id2name_mapping:
        try:            # test the waters first, because
            dev_id in V # this 
        except:         # may
            continue    # explode!

        if dev_id in V:
            V = V.replace(dev_id,bridge.dev_id2name_mapping[dev_id])
            
    return translate_time(V)

def translate_df(df0):
    # don't modify the original, so make a copy to be returned
    df = df0.copy()
    
    # first change the column names; note that we can't individually assign
    # new names, and DataFrame.rename() only applies a mapper. so to translate
    # we must assign a complete, new list.
    C = list(df.columns)
    C = list(map(translate_text,C))
    df.columns = C

    # then the device index
    N = list(df.index)
    N = list(map(translate_text,N))
    df.index = N
    
    # now the data
    for i in range(len(df.dtypes)): 
        var_name = df.columns[i]
        for j in range(len(df.index)):
            dev_id = df.index[j]
            val = df.loc[dev_id,var_name]
            
            if df.dtypes[i] == int or df.dtypes[i] == float:
                df.loc[dev_id,var_name] = translate_time(val)
            else:
                df.loc[dev_id,var_name] = translate_text(val)

    # sorts columns 
    df = df.reindex(sorted(df.columns), axis=1) 
     
    # sorting indices 
    df.sort_index(inplace=True) # reduce waste 
     
    return df

###############################################################################
#  DATA STRUCTURE MANAGEMENT
#  Below are methods for managing data structures like the DataFrame. 
###############################################################################

# flag for indicating the device needs another node
DEV_NEEDS = 1
DEV_AMBIVALENT = 0.5
DEV_INDEPENDENT = 0

UNINIT_FLOAT = -999.0
UNINIT_INT = -999
UNINIT_STR = ''

def is_uninit(val):
    return val in [UNINIT_FLOAT,UNINIT_INT,UNINIT_STR]

def get_uninit_val(val):
    if type(val) == int: return UNINIT_INT
    if type(val) == float: return UNINIT_FLOAT
    return UNINIT_STR

def get_uninit_col(df,var_name,sample_val):
    # special case - causes problems in routing, left as reminder
    #if 'needs' in var_name:
    #    return [DEV_NEEDS]*len(df)

    # typical case
    uninit_val = get_uninit_val(sample_val)
    return [uninit_val]*len(df)

def get_init_col(df,var_name,init_val):
    # special case, in case the user forgets to supply the correct init_val
    if 'needs' in var_name:
        return [DEV_AMBIVALENT]*len(df)
    # special case: like before, but for counters
    elif PROMOTER_SUFFIX in var_name or ANSWERED_SUFFIX in var_name:
        return [0]*len(df)

    return [init_val]*len(df)

def get_uninit_row(df):
    L = []
    for i in range(len(df.dtypes)): 
        if df.dtypes[i] == int: L.append(UNINIT_INT)
        elif df.dtypes[i] == float: L.append(UNINIT_FLOAT)
        # note that df.dtypes[i] == str always returns False 
        else: L.append(UNINIT_STR) 

    return L

def get_init_row(df):
    L = []
    for i in range(len(df.dtypes)): 
        var_name = df.columns[i]
     
        # special cases first
        if 'needs' in var_name: L.append(DEV_AMBIVALENT)
        elif PROMOTER_SUFFIX in var_name or ANSWERED_SUFFIX in var_name: L.append(0)
        # typical case
        elif df.dtypes[i] == int: L.append(UNINIT_INT)
        elif df.dtypes[i] == float: L.append(UNINIT_FLOAT)
        # note that df.dtypes[i] == str always returns False 
        else: L.append(UNINIT_STR) 

    return L

# vertical zero vector of length POP_SIZE, transposed for readability
def get_zero_vec(): # note that numpy.matrix is deprecated
    v = np.zeros((1,POP_SIZE),dtype='int32') # arrays are recommended
    return v

# square zero matrix of dimension POP_SIZE x POP_SIZE
def get_zero_mat(): # note that numpy.matrix is deprecated
    m = np.zeros((POP_SIZE,POP_SIZE),dtype='int32') # arrays are recommended
    return m

# Solution from tomatom (October 13, 2017) at StackOverflow
# in response to the following posted question:
# "Drop all data in a pandas dataframe" available at
# <https://stackoverflow.com/questions/39173992/drop-all-data-in-a-pandas-dataframe>
# last accessed: June 28, 2019
def empty_df(df):
    return df.iloc[0:0]
    
# parameter listing matches function name
def funnel_q2list(Q,L):
    while not bridge.closing_time and Q.qsize() > 0:
        try:
            item = Q.get_nowait()
            L.append(item)
        except: # no message? stop
            pass
            
    return L

# note change in parameter listing to go with the function name
def funnel_list2q(L,Q):
    for item in L:
        Q.put(item)

# how many times is a message re-transmitted? this is the initial (maximum)
# of a decrementing counter.
ALLOWABLE_MULLIGANS = 5

def cull_mulligans(L):
    for lmsg in L:
        try:
            lmsg.mulligan_counter -= 1
        except:
            lmsg.mulligan_counter = ALLOWABLE_MULLIGANS

    for i in reversed(range(len(L))):
        if L[i].mulligan_counter == 0: 
            del L[i].mulligan_counter
            del L[i]

def get_node_data(node_name,node_id):
    data = {'id':node_id,'name':node_name}
    return data

# borrowed this from the ferry code, written by Jeremy Musser and Dr. Ezra Kissel,
# as part of the WildfireDLN project, of which Indiana University was a partner. 
# code available online publicly: 
# <https://github.com/datalogistics/wildfire-dln/blob/master/ferry/dln_ferry.py>
# last accessed March 26, 2019, master branch.
def register_or_retrieve_node(node_name,node_id):
    if not bridge.HAVE_UNIS: return bridge.UNIS_FAIL

    node_name += '_TEST'

    n = bridge.rt.nodes.where({'name': node_name})
    try: # allow for reuse. alternatively, throw an error.
        n = next(n)
        log.unis_updates('node with name=%s found' % (node_name))
    except StopIteration:
        log.unis_updates('node with name=%s not found, creating now' % (node_name))
        n = Node(get_node_data())
        #n.dev_id = dev_id # note that here, this creation and assignment fails
        bridge.rt.insert(n, commit=True) 
        bridge.rt.flush() 
    
    update_var(n,'name',node_name)   
    update_var(n,'id',node_id)

    return n

def node_has_var(node,var_name):
    return var_name in node._obj.__dict__ 

def get_metadata_data(node_id,metadata_id):
    data = {'id': metadata_id, \
        'subject': {'rel': 'full','href': 'http://localhost:9000/nodes/{}'.format(node_id)}, \
        'eventType': 'test'}
    
    #requests.post(url, data=json.dumps(data))
    return data

def register_or_retrieve_metadata(node_id,metadata_id):
    if not bridge.HAVE_UNIS: return bridge.UNIS_FAIL

    m = bridge.rt.metadata.where({'id': metadata_id})
    try: # allow for reuse. alternatively, throw an error.
        m = next(m)
        log.unis_updates('metadata with mid=%s found' % (metadata_id))
    except:
        log.unis_updates('metadata with mid=%s not found, creating now' % (metadata_id))
        m = Metadata(get_metadata_data(node_id,metadata_id))
        bridge.rt.insert(m, commit=True) 
        bridge.rt.flush() 

    return m

# TODO cite the noise.py code
def create_data_stream_poster(metadata_id):
    url = "{}/data/{}".format(url, metadata_id)

    def data_poster_function(ts,val):
        data = { 'mid': metadata_id, 'data': [{'ts': ts, 'value': val}] }
        headers= { "Content-Type": "application/perfsonar+json profile=http://unis.crest.iu.edu/schema/20160630/datum#",
               "Accept": "*/*" }
        requests.post(url, data=json.dumps(data), headers=headers)

    return data_poster_function

# this takes in a string parameter, a value set to some variable, and determines if
# quotation marks need to be inserted. if so, marks are added. otherwise, no change is made.
def quote(val):
    try: # this brings back memories
        float(val) # covers int as well
        return val
    except:
        pass
    
    # have a tuple/list/dictionary? note that typecasting fails for these
    if len(val) > 2:
        if val[0] == '(' and val[-1] == ')' or val[0] == '{' and val[-1] == '}' \
        or val[0] == '[' and val[-1] == ']':
            return val
    
    # ruled everything else out but string
    return '\'' + val + '\''

# updates a variable by the name of var_name in the given node; if the variable doesn't
# exist, it will be created
def update_var(node,var_name,val):
    if node_has_var(node,var_name):
        S = 'node.%s = %s' % (var_name,quote(val))
        log.unis_updates('variable %s was found, setting now via %s' % (var_name,S))
        exec(S)
        bridge.rt.flush() 
        return
        
    S = 'node.extendSchema(\'%s\',%s)' % (var_name,quote(val))
    log.unis_updates('variable %s not found, creating now via %s' % (var_name,S))
    exec(S)    
    
###############################################################################
#  PROCESS MANAGEMENT AND SYNCHRONIZATION
#  Below are various methods for managing multiple runnning processes and
#  cleanly disposing of them when the time comes.
###############################################################################    

LORA_PATH = bridge.CURRENT_PATH # full path needed
LORA_C_FN = LORA_PATH + 'lora_c' # if running this process at boot
LORA_C_PROC_CALL = LORA_C_FN + ' -i %d -o %d'
LORA_C_RECEIVER_OPT = '--receiver'
LORA_C_TRANSMITTER_OPT = '--transmitter'

# buckets that will contain all threads, processes, and sockets used for easier mopup() 
THREAD_BUCKET = [] # one day I'll come up with better names
PROCESS_BUCKET = [] # but buckets are so versatile!
SOCKET_BUCKET = [] # they can be watering cans, trash cans, filing cabinets...
QUEUE_BUCKET = [] # vases, cat carriers, laundry hampers...

TIME_OF_CREATION = datetime(2019,1,30).timestamp()

def now():
    return time.time()

INITIATION_TIME = now()

def start_lora_c(incoming_port,outgoing_port):
    proc_call = LORA_C_PROC_CALL % (incoming_port,outgoing_port)

    # at most one of these options will be added to the call
    if bridge.RECEIVE_ONLY: proc_call += ' %s' % (LORA_C_RECEIVER_OPT)
    if bridge.TRANSMIT_ONLY: proc_call += ' %s' % (LORA_C_TRANSMITTER_OPT)
    
    log.info('summoning the c-handler via %s' % (proc_call))

    bridge.lora_c_p = subprocess.Popen(proc_call,shell=True, #TODO remove shell
        stdin=subprocess.PIPE,stdout=subprocess.PIPE,
        stderr=subprocess.PIPE) # for tidiness

def dump_process_bucket():
    for i in reversed(range(len(PROCESS_BUCKET))):
        PROCESS_BUCKET[i].terminate()
        del PROCESS_BUCKET[i]
        log.info('subprocess terminated')

def dispose_of_lora_c():
    try: # note that lora_c_p is the subprocess handle 
        bridge.lora_c_p.terminate()
        del bridge.lora_c_p
        log.info('lora_c_p disposed of')
    except:
        pass

# a clean disposal so we don't leave zombie processes hanging around. use in place of exit().
def mopup():
    log.info('cleaning up...')

    #os.system('./apocalypse.sh') 

    dump_process_bucket()
    dispose_of_lora_c()

    for t in THREAD_BUCKET:
        del t
        #log.info('thread terminated') # put back in when spam is tolerable

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

# Solution from Matt J (2009), then edited by Grimthorr (2018) at StackOverflow 
# in response to the following posted question:
# "How do I capture SIGINT in Python?" available at
# <https://stackoverflow.com/questions/1112343/how-do-i-capture-sigint-in-python>
# last accessed: November 27, 2018
def signal_handler(sig, frame):
    bridge.closing_time = True
    time.sleep(SNOOZE_TIME)
    mopup()

# Solution from Alex (2010) and edited by the Tin Man (2015), et al. at StackOverflow
# in response to the following posted question:
# "How can I use Python to get the system hostname?" available at
# <https://stackoverflow.com/questions/4271740/how-can-i-use-python-to-get-the-system-hostname>
# last accessed: June 28, 2019
def get_hostname():
    return socket.gethostname()

RESETTING = -1 
RECEIVING = 0
TRANSMITTING = 1

RESET_DURATION = 1. # in seconds, approximately
RECEIVING_DURATION = 1. # in seconds
TRANSMITTING_DURATION = 1. # in seconds

def hex2bin(S):
    return bin(int(S,16)).replace('0b','')

def get_ordering(S):
    B = hex2bin(S)    
    return list(map(int,B))

def spin_until(ts,eps):
    while ts - now() > eps:
        pass

def snooze_and_wait(Q):
    while not bridge.closing_time:
        if Q.qsize() > 0:
            try:
                item = Q.get_nowait()
                return item
            except: # no message? snooze.
                pass

        time.sleep(SNOOZE_TIME)

###############################################################################
#  PREFLIGHT CHECKS
#  Below are methods for performing preflight checks 
###############################################################################

def gps_available():
    rlat, rlong = retrieve_gps()
    
    # check both to reduce the odds of mistaken lack of availability
    return rlat == bridge.DEFAULT_LATITUDE and rlong == bridge.DEFAULT_LONGITUDE

# Solution from 
# Solution from mluebke (2011), then edited by monk-time (2017) at StackOverflow 
# in response to the following posted question:
# "Python check if a process is running or not?" available at
# <https://stackoverflow.com/questions/7787120/python-check-if-a-process-is-running-or-not>.
# last accessed: December 21, 2018.
def process_running(pn):
    return pn in (p.name() for p in psutil.process_iter())

# pulled from the makefile and invoked in preflight_checks()
# note that compilation will only succeed on the RPi; if wiringPi does not
# recognize the hardware it's being run on, the library will avert its
# inclusion. note that the first two parameters are defined previously line ~454.
# they are shown here for reference.
LORA_C_FN = bridge.CURRENT_PATH + 'lora_c' # need full path if running this process at boot
LORA_C_SRC_FN = bridge.CURRENT_PATH + 'lora_c.cpp'
MAKE_CLEAN = 'rm -rf __pycache__ edit %s *.o a.out *.pyc' % (LORA_C_FN)

if bridge.DEVICE_ARCH_IS_RPI: # assume by default we are on an Up Board
    MAKE_LORA = 'g++ -Wall -o %s -lwiringPi -pthread %s' % (LORA_C_FN, LORA_C_SRC_FN)
else:
    MAKE_LORA = 'g++ -Wall -o %s -pthread %s' % (LORA_C_FN, LORA_C_SRC_FN)

# Solution from Jeremy Grifski of The Renegade Coder.
# "How to Check if a File Exists in Python," posted February 17, 2018, available at:
# <https://therenegadecoder.com/code/how-to-check-if-a-file-exists-in-python/>.
# last accessed: December 21, 2018.
def file_exists(fn):
    p = pathlib.Path(fn) 
    return p.is_file()

# check if lora_c exists; if not, compile; check for daemons, etc. 
def preflight_checks():
    # was this script run with the Python 3 interpreter? 
    major_version_num = sys.version_info[0]
    minor_version_num = sys.version_info[1]

    if major_version_num < 3:
        log.error('must run with Python 3.x')
        return False

    #TODO
    # is periscoped running? redundant but leave in case needed later
    '''
    if not process_running(PERISCOPE_PN):
        # try launching it
        os.system(PERISCOPE_LAUNCH)
    
    # if launching failed, bail
    if not process_running(PERISCOPE_PN): 
        log.error('periscoped not found')
        return False
    '''

    # do we have UNIS?
    if not bridge.HAVE_UNIS:
        log.critical('unable to connect to UNIS instance')

    # if we're running on hardware,
    if not SIM_MODE:
        # does lora-c exist in compiled form?
        if not file_exists(LORA_C_FN): # try compiling
            log.error('lora-c executable not found, attempting compilation')
            os.system(MAKE_CLEAN)
            os.system(MAKE_LORA)
        
        # if compilation failed, bail
        if not file_exists(LORA_C_FN):
            log.critical('lora-c compilation failed')
            return False
        else: log.critical('lora-c compilation successful')

        # if we expect it, is GPS available?
        if not gps_available():
            log.critical('GPS appears to be unavailable, will use default coordinates')
        else:
            log.critical('GPS appears to be available')

    return True

###############################################################################
#  MESSAGE TYPES AND CONSTANTS
#  Below are constants for distinguishing different types of messages
###############################################################################

MESSAGE_DELIMITER = '/'
MESSAGE_TERMINATOR = '|'

MULTICAST = '*'
ANY_DEVICE = '^' 
DATA_NOT_FOUND = 'DATADNE'

# placeholder
RESERVED_DEFAULT = ''

i=0
BAD_MESSAGE = i; i+=1

# send out data with which recipient nodes with which to update UNIS instances
MSG_TYPE_UNIS_POST_NOTIF = i; i+=1
MSG_TYPE_UNIS_POST_ACK_REQ = i; i+=1
MSG_TYPE_UNIS_POST_ACK = i; i+=1

# send out request for data from recipients' UNIS instances
MSG_TYPE_UNIS_GET_REQUEST = i; i+=1
MSG_TYPE_UNIS_GET_RESPONSE = i; i+=1

# updates, requests, responses for communicating current position (in GPS coordinates)
MSG_TYPE_POS_UPDATE = i; i+=1
MSG_TYPE_POS_REQUEST = i; i+=1
MSG_TYPE_POS_RESPONSE = i; i+=1

# generic query and response -- type specified in payload
MSG_TYPE_QUERY = i; i+=1
MSG_TYPE_QUERY_RESPONSE = i; i+=1

# generic notification and request for acknowledgment -- type specified in payload
MSG_TYPE_NOTIF = i; i+=1
MSG_TYPE_NOTIF_ACK_REQ = i; i+=1
MSG_TYPE_NOTIF_ACK = i; i+=1

# for disseminating routing data
MSG_TYPE_RTG = i; i+=1

# panic button messages: highest priority, don't follow usual routing rules
MSG_TYPE_PANIC = i; i+=1

MESSAGE_TYPES = [MSG_TYPE_UNIS_POST_NOTIF, MSG_TYPE_UNIS_POST_ACK_REQ, MSG_TYPE_UNIS_POST_ACK,
MSG_TYPE_UNIS_GET_REQUEST, MSG_TYPE_UNIS_GET_RESPONSE,
MSG_TYPE_POS_UPDATE, MSG_TYPE_POS_REQUEST, MSG_TYPE_POS_RESPONSE,
MSG_TYPE_QUERY, MSG_TYPE_QUERY_RESPONSE,
MSG_TYPE_NOTIF, MSG_TYPE_NOTIF_ACK_REQ, MSG_TYPE_NOTIF_ACK,
MSG_TYPE_RTG, MSG_TYPE_PANIC]

NOTIFICATIONS = [MSG_TYPE_UNIS_POST_NOTIF, 
MSG_TYPE_POS_UPDATE,
MSG_TYPE_NOTIF,
MSG_TYPE_RTG]

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

# will expand over time
HARVESTABLE = \
    [MSG_TYPE_UNIS_GET_RESPONSE, MSG_TYPE_UNIS_POST_NOTIF, MSG_TYPE_UNIS_POST_ACK,
    MSG_TYPE_POS_RESPONSE, MSG_TYPE_POS_UPDATE,
    MSG_TYPE_QUERY_RESPONSE, 
    MSG_TYPE_NOTIF, MSG_TYPE_NOTIF_ACK]

def msg_type_is_defined(R):
    return R in MESSAGE_TYPES

def msg_type_is_notif(R):
    return R in NOTIFICATIONS

def msg_type_is_request(R):
    return R in REQUESTS

def msg_type_is_response(R):
    return R in RESPONSES

def msg_type_is_harvestable(R):
    return R in HARVESTABLE

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
    
    if R in [MSG_TYPE_RTG]:
        return MSG_TYPE_RTG
    
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
msg_code2msg_text_d[MSG_TYPE_RTG] = 'MSG_TYPE_RTG'
msg_code2msg_text_d[MSG_TYPE_PANIC] = 'MSG_TYPE_PANIC'

def msg_code2msg_desc(code):
    if code not in msg_code2msg_text_d:
        return 'BAD_CODE'

    return msg_code2msg_text_d[code]

###############################################################################
#  DATA CLEANSING AND PARSING
#  Below are methods for data cleansing and parsing data out. 
###############################################################################

def is_plausible_timestamp(T):
    try:
        t = float(T)
    except:
        return False

    return True

    ''' # debatably useful, but leave in case
    current_time = now()

    if TIME_OF_CREATION <= t and t <= current_time:
        return True

    return False
    '''

def is_plausible_int(S):
    try:
        int(S)
    except:
        return False
        
    if type(S) == str and '.' in S:
        return False
        
    return True
    
def is_plausible_float(S):
    try:
        float(S)
    except:
        return False
    
    if type(S) == str and '.' not in S:
        return False
    
    return True

def is_plausible_number(S):
    return is_plausible_int(S) or is_plausible_float(S)

def convert_val(S):
    if is_plausible_int(S):
        return int(S)
    if is_plausible_float(S):
        return float(S)
    return S

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
    
    # ordering assumed to be (latitude,longitude)
    try:
        c0 = float(s0.split(',')[0])
        c1 = float(s0.split(',')[1])
    except:
        return False
        
    return is_plausible_lat(c0) and is_plausible_long(c1)

def pluck_GPS_coordinates(S):
    s0 = S.strip('(').strip(')')
    
    # ordering assumed to be (latitude,longitude)
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

def is_plausible_dev_id(dev_id):
    return is_plausible_MAC_addr(dev_id)

def normalize_addr(addr):
    return (addr.replace(':','').replace('-','')).lower()

# Solution from Shifullah Ahmed Khan (2018) at StackOverflow
# in response to the following posted question:
# "Python - Get mac address" available at
# <https://stackoverflow.com/questions/28927958/python-get-mac-address>
# last accessed: November 27, 2018
def get_my_mac_addr():
    original_mac_address = getnode()
    hex_mac_address = str(":".join(re.findall('..', '%012x' % original_mac_address)))

    return normalize_addr(hex_mac_address)
    
###############################################################################
#  SENSOR DATA RETRIEVAL
#  Below are methods for rerieving data from attached sensors. 
###############################################################################

# if demoing indoors with little room for movement, add some simulated noise to
# the retrieved coordinate. seasoning is applied in protocol.__init__s
def season(): 
    return np.random.normal(0,bridge.BUOY_NOISE_STD)

GPS_DEV_READ_LEN=50 # number of lines of output to read from said device
MAX_GPS_READ_ATTEMPTS=3 # number of times to attempt extraction of GPS coordinates

# path/location to the Hat's GPS device
if bridge.DEVICE_ARCH_IS_RPI: # by default, asssume we're on an Up Board
    GPS_DEV_LOC='/dev/ttyS0' 
else:
    GPS_DEV_LOC='/dev/ttyS4' 

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

# attempts to retrieve the device's current GPS coordinates, reading
# GPS_DEV_READ_LEN lines of output from GPS_DEV_LOC per attempt, with
# at most MAX_GPS_READ_ATTEMPTS attempts. 
def retrieve_gps():
    latitude = bridge.DEFAULT_LATITUDE 
    longitude = bridge.DEFAULT_LONGITUDE

    for i in range(MAX_GPS_READ_ATTEMPTS):
        p = subprocess.Popen(GPS_DEV_PROC_CALL,shell=True, #TODO remove shell
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

FAKE_ONBOARD_TEMPERATURE = -42.

# Solution from Keval Patel of Medium.
# "Monitor the core temperature of your Raspberry Pi," posted July 3, 2017, available at:
# <https://medium.com/@kevalpatel2106/monitor-the-core-temperature-of-your-raspberry-pi-3ddfdf82989f>
# last accessed: June 28, 2019
def get_onboard_temp():
    if bridge.DEVICE_ARCH_IS_RPI:
        temp = os.popen('vcgencmd measure_temp').readline()
        return float(temp.replace('temp=','').replace('\'C',''))    

    return FAKE_ONBOARD_TEMPERATURE
    
###############################################################################
#  SIMULATION
#  Below are methods for simulating swarms. 
###############################################################################

FAKE_RSSI_VALUE = -42

def add_fake_RSSI_to_packet(S):
    return S[:-1] + str(FAKE_RSSI_VALUE) + MESSAGE_TERMINATOR
    
def get_fake_mac_addr():
    S = '' # could use upper-case but lower-case makes disambiguating addresses easier
    for i in range(12): S += random.choice('0123456789abcdef')
    return normalize_addr(S)

def get_fake_mac_addr_starting_with_one():
    S = '1' # could use upper-case but lower-case makes disambiguating addresses easier
    for i in range(11): S += random.choice('0123456789abcdef')
    return normalize_addr(S)

def vessel_dist(M,N):
    d = np.sqrt((M.curr_lat - N.curr_lat)**2 + (M.curr_long - N.curr_long)**2)
    return d
        
