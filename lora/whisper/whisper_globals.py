'''**************************************************************************

File: whisper_globals.py
Language: Python 3.6/7
Author: Juliette Zerick (jzerick@iu.edu)
        for the WildfireDLN Project
        OPEN Networks Lab at Indiana University-Bloomington

Included are variables needing to be accessed and modified globally in ways
for which the global reserved word proved insufficient. Global locks and flags,
including closing_time, hwich is used to synchronize across many files to
perform a smooth shutdown. 

Last modified: March 26, 2019

****************************************************************************'''

import threading

# signals to threads to shut down
closing_time = False

# signals that whisper-c must be restarted
whisper_c_is_dead = False

# subprocess handle for whisper-c
whisper_c_p = 0

# a lock to ensure atomic (uninterrupted) operations while threads are running around
CONCH = threading.Lock() 

# for uploading files
#import libdlt

USING_UNIS = True
UNIS_FAIL = -1
rt = UNIS_FAIL

if USING_UNIS:
    try:
        #from unis import Runtime # is there a difference?
        from unis.runtime import Runtime   
        from unis.models import Node, schemaLoader
        rt = Runtime('http://localhost:9000')
    except:
        pass
        
def have_UNIS():
    return rt != UNIS_FAIL
