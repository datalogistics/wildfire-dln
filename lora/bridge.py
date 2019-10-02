'''**************************************************************************

File: bridge.py
Language: Python 3.6.8
Author: Juliette Zerick (jzerick@iu.edu)
        for the WildfireDLN Project
        OPeN Networks Lab at Indiana University-Bloomington

Included are variables needing to be accessed and modified globally in ways
for which the global reserved word proved insufficient. Global locks and flags,
including closing_time, hwich is used to synchronize across many files to
perform a smooth shutdown. 

Last modified: September 29, 2019

****************************************************************************'''

import sys
import threading
import time
import platform
import os
import socket

# signals to threads to shut down
closing_time = False

# signals that lora-c must be restarted
lora_c_is_dead = False

# subprocess handle for lora-c
lora_c_p = 0

# a lock to ensure atomic (uninterrupted) operations while threads are running around
CONCH = threading.Lock() 

# for more readable times in simulation
sim_start = time.time()

# for more readable columns in simulation
dev_id2name_mapping = {}

# for uploading files
#import libdlt

UNIS_URL = 'http://localhost:9000'
HAVE_UNIS = False
UNIS_FAIL = -1
rt = UNIS_FAIL

try:
    #from unis import Runtime # is there a difference?
    from unis.runtime import Runtime   
    from unis.models import Node, schemaLoader
    from unis.models import Metadata
    rt = Runtime(UNIS_URL)
    print('able to import everything!')
except: # possible alternative, depending on the environment
    try: # in this case, Ubuntu 16.04 via Windows Subsystem for Linux
        # once more with less fail?
        #sys.path.append('/home/minion/repobin/Lace') # if needed

        from unis import Runtime  
        from unis.models import Node, schemaLoader
        from unis.models import Metadata
        rt = Runtime(UNIS_URL)
        print('able to import everything!')
    except:
        pass

HAVE_UNIS = rt != UNIS_FAIL
        
# borrowed this from the ferry code, written by Jeremy Musser and Dr. Ezra Kissel,
# as part of the WildfireDLN project, of which Indiana University was a partner. 
# code available online publicly: 
# <https://github.com/datalogistics/wildfire-dln/blob/master/ferry/dln_ferry.py>
# last accessed August 26, master branch.
HAVE_FERRY_NODE = False
MY_FERRY_NODE_NAME = socket.gethostname()
MY_FERRY_NODE = UNIS_FAIL

if HAVE_UNIS: 
    nodes = rt.nodes.where({"name": MY_FERRY_NODE_NAME})

    try: # hope that the ferry's already loaded and registered its node
        MY_FERRY_NODE = next(nodes)
    except StopIteration: 
        pass

HAVE_FERRY_NODE = MY_FERRY_NODE != UNIS_FAIL

# equivalent to discern_board() in whisper_c.cpp
    
MACHINE_RPI = "armv7l"
MACHINE_UP_BOARD = "x86_64"

DEVICE_ARCH_IS_RPI = False
DEVICE_ARCH_IS_UPB = False

# by default, assume we're on an Up Board
if platform.machine() == MACHINE_RPI:
    DEVICE_ARCH_IS_RPI = True
    DEVICE_ARCH_IS_UPB = False
else:
    DEVICE_ARCH_IS_RPI = False
    DEVICE_ARCH_IS_UPB = True

# re/set via command-line arguments
SIM_MODE = False
RECEIVE_ONLY = False
TRANSMIT_ONLY = False
USE_EMCEE = False
USE_BUOY_EFFECT = False
USING_lora_c_HANDLER = True

BUOY_NOISE_STD = 0.00001

BLOOMINGTON_LATITUDE = 39.1653
BLOOMINGTON_LONGITUDE = -86.5264

DEFAULT_LATITUDE = BLOOMINGTON_LATITUDE
DEFAULT_LONGITUDE = BLOOMINGTON_LONGITUDE

INTENTIONAL_SMUDGE = 0.0000000001
DEFAULT_LATITUDE += INTENTIONAL_SMUDGE
DEFAULT_LONGITUDE += INTENTIONAL_SMUDGE

# Solution from Brian Oakley (2010)) at StackOverflow
# in response to the following posted question:
# "How do I get the full path of the current file's directory?" available at
# <https://stackoverflow.com/questions/3430372/how-do-i-get-the-full-path-of-the-current-files-directory>
# last accessed: August 20, 2019
CURRENT_PATH = os.getcwd()
if CURRENT_PATH[-1] != '/':
    CURRENT_PATH += '/'

