'''**************************************************************************

File: whisper_globals.py
Language: Python 3.7
Author: Juliette Zerick (jzerick@iu.edu)
        for the WildfireDLN Project
        OPEN Networks Lab at Indiana University-Bloomington

Included are variables needing to be accessed and modified globally in ways
for which the global reserved word proved insufficient. Global locks and flags,
including closing_time, hwich is used to synchronize across many files to
perform a smooth shutdown. 

Last modified: December 6, 2018

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
