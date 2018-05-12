import sys
import os
import subprocess
import threading
import time
import re
import json 
import base64 

# Bryce's xDot wrapper
from xdot_no_guardrails import * # no return message from gateway => unnecessary explosions

GPS_DEV_READ_LEN=50 # number of lines of output to read from gps3

# default values for the location of the ferry. for now, Bloomington, Indiana.
BLOOMINGTON_LATITUDE=39.16533 # "vertical axis" ~ y
BLOOMINGTON_LONGITUDE=-86.52639 # "horizontal axis" ~ x

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
                return (latitude,longitude)

        # no line of output contained the data we needed. cleanup
        # and try again, if so desired.
        p.kill()

    return (latitude,longitude)

######## CONSTANTS, GLOBAL VARIABLES, AND UTILITIES ##############################################

# the number of seconds between attempted actions by threads
SNOOZE_TIME = 1

# could automate retrieval of this too, but we have enough to deal with already
MY_ADDR = '00-80-00-00-04-00-32-88'

# a lock to ensure atomic (uninterrupted) operations while threads are running around
CONCH = threading.Lock() 

# buckets that will contain all threads and processes used for easy mopup() 
THREAD_BUCKET = []
PROCESS_BUCKET = []

# a clean disposal so we don't leave zombie processes hanging around. use in place of exit().
def mopup(xD):
    xD.close_port()

    for p in PROCESS_BUCKET:
        p.terminate()
        del p

    for t in THREAD_BUCKET:
        del t

    exit(0)

# atomic printing function for readable output
def atomic_print(S):
    CONCH.acquire()
    print S
    CONCH.release()

# FUNCTIONALITY: MONITOR THE FLOW OF MESSAGES TO INFER EVENTS ON THE SERVER ######################

# call to launch the monitored process
PROC_SUB_ALL_CALL = 'mosquitto_sub -h pivot.iuiot.org -v -t lora/%s/+' % (MY_ADDR)

# convert JSON string to JSON object
def s2j(S):
    return json.loads(S.split(' ')[1])

# default output, should parsing fail
DECODE_FAIL = ''

# utility to extract data from string S, whatever form it might take
def retrieve_float(S):
    try: # try decoding, assuming S contains a JSON object
        J = s2j(S)
        D = base64.decodestring(J['data'])
        D = float(D)
        return D
    except: # that didn't work
        pass

    try: # try Bryce's decoding, then try treating D as containing a JSON object
        D = S.replace('0a','').replace('0d','').strip()
        J = s2j(D)
        D = base64.decodestring(J['data'])
        D = float(D)
        return D
    except: # this didn't work either
        pass

    try: # ok, try regular expressions
        D = re.findall('data:(.+),deveui',S)[0]
        D = base64.decodestring(D)
        D = float(D)
        return D
    except: # out of ideas
        pass

    # all parsing attempts failed, return a default value
    return DECODE_FAIL

OF_INTEREST = 'lora/00-80-00-00-04-00-32-88/up'

# this will be launched as a thread. it will observe messages output by the monitored process
# (fed in by the process's pipe) and store UP messages in up_q and DOWN messages in down_q
def observe_message_flow(proc_name,pipe,up_q,down_q):
    atomic_print('<%s> started reading' % (proc_name))

    while True:
        try:
            msg = pipe.readline().strip('\n')
        except: # no message? snooze.
            time.sleep(SNOOZE_TIME)
            continue

        if '/up' in msg:
            datum = retrieve_float(msg)
            atomic_print('<%s> observed UP message %s' % (proc_name,str(datum)))

# launch the mosquitto_sub process 
proc_sub_all = subprocess.Popen(PROC_SUB_ALL_CALL,shell=True,stdin=subprocess.PIPE,stdout=subprocess.PIPE)
PROCESS_BUCKET.append(proc_sub_all) # add to the bucket for easier cleanup

# thread for observing messages coming out of the previously launched process
psub_all_t = threading.Thread(target=observe_message_flow, args=('pivot observer',proc_sub_all.stdout, up_q,down_q))
psub_all_t.daemon = True # so it dies with the host process
psub_all_t.start() # add to the bucket for easier cleanup
THREAD_BUCKET.append(psub_all_t)

# use the lock to ensure non-garbled printing of messages from Bryce's xdot class
CONCH.acquire() 
xD = xDot() 
CONCH.release()

# from xdot.py's main
if (xD.join_status() == True):
    atomic_print("Already connected to LoRa access point.")
else:
    atomic_print("Connecting to the LoRa access point")
    if (not xD.join_network("MTCDT-19400691","MTCDT-19400691",1)):
        atomic_print("Error: Failed to connect to the LoRa Access point.")
        mopup(xD)
    else:
        atomic_print("Connected to LoRa access point: MTCDT-19400691.")

latitude,longitude = retrieve_gps()

msg = '%.6f' % (latitude)
xD.send_message(msg) # this publishes an UP message 

msg = '%.6f' % (longitude)
xD.send_message(msg) # this publishes an UP message 

time.sleep(5) # wait for messages to be detected by the observer

atomic_print('done!')
mopup(xD)
