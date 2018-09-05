#!/usr/bin/env python

'''
File: whisper.py
Language: Python

This contains the higher-level protocol and switch behaviors. It interfaces
with whisper.cpp, which quickly filters relevant messages that are passed
to whisper.py. 

A number of threads were used to logically separate production and consumption
of messages for ease of troubleshooting. The use of queues adds a cushion to
avoid lost messages, providing some resiliency.

Author: Juliette Zerick (jzerick@iu.edu)
OPEN Lab, Indiana University
'''
import signal
import sys
import socket
import time
import threading 
import subprocess
import queue
from contextlib import closing

import asyncio 
from aiocoap import *

# will be updated by main once threads establish sockets
INCOMING_PORT = 0
OUTGOING_PORT = 0

# socket IDs for incoming and outgoing ports
inc_s = 0
out_s = 0

SNOOZE_TIME = 3 # seconds

WHISPER_C_PROC_CALL = 'sudo ./whisper %d %d'

BUFFER_SIZE = 1024  # for a faster response, use a smaller value (256)

MSG_COUNTER = 0

# queues for storing observed messages, declared here for tidiness
inbox_q = queue.Queue()
outbox_q = queue.Queue()

# a lock to ensure atomic (uninterrupted) operations while threads are running around
CONCH = threading.Lock() 

# buckets that will contain all threads and processes used for easy mopup() 
THREAD_BUCKET = []
PROCESS_BUCKET = []

# signals to threads to shut down
closing_time = False

# a clean disposal so we don't leave zombie processes hanging around. use in place of exit().
def mopup():
    for p in PROCESS_BUCKET:
        p.terminate()
        del p

    for t in THREAD_BUCKET:
        del t

    exit()

# https://stackoverflow.com/questions/1112343/how-do-i-capture-sigint-in-python
def signal_handler(sig, frame):
    global closing_time
    closing_time = True
    time.sleep(SNOOZE_TIME)
    mopup()

#https://stackoverflow.com/questions/1365265/on-localhost-how-do-i-pick-a-free-port-number
def find_free_port():
    with closing(socket.socket(socket.AF_INET, socket.SOCK_STREAM)) as s:
        s.bind(('', 0))
        return s.getsockname()[1]

'''
This encapsulates the protocol and switch behavior of whisper-py. Relevant messages
(~packets) are received from whisper-c, fed to the inbox_q, then
consumed here.

The protocol uses ASCII messages:
* for multicast or MAC address of recipient/
MAC address of sender/
four digits giving the length of the payload, padded with 0s/
payload/
|

The following actions can be taken:
- if the message is a request for position, the current GPS coordinates are
  packed up as a multicast message and pushed to the outbox_q
- the message is submitted to the coap-server-periscope-interface
  (yet to be named), and the response returned by pushing to the outbox_q
- if a message is to saturate the field, the message will be hop-broadcasted
  depending on a threshold, like the reverse of Strogatz's famous
  firefly synchronization paper

Currently the code only performs the second action. While the latter two
behaviors could be encapsulated in the unnamed coap-server-periscope-interface
it would be faster to handle the actions here.

to be more cleverly named after the character in Men in Black 2:
http://www.pilkipedia.co.uk/wiki/index.php/Postal_Sorting_Alien
'''
async def postal_sorter(inbox_q,outbox_q):
    protocol = await Context.create_client_context()

    while not closing_time:
        #print ('PYinbox: box length is',inbox_q.qsize())
        try:
            msg = inbox_q.get_nowait()
        except: # no message? snooze.
            #print ('PYinbox: no messages :(')
            time.sleep(SNOOZE_TIME)
            continue

        # we've got mail!
        msg = msg.decode()
        print ('PYinbox: got mail', msg)

        request = Message(code=GET, uri=msg)

        try:
            response = await protocol.request(request).response
        except Exception as e:
            print('Failed to fetch resource:')
            print(e)
        else:
            print('Result: %s\n%r'%(response.code, response.payload))
            outbox_q.put((response.payload).decode()) 

# listens for messages from whisper-c via socket, pushes received messages to the
# inbox_Q
def c_listener(inbox_q):
    global INCOMING_PORT
    INCOMING_PORT = find_free_port()

    inc_s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    inc_s.bind(('localhost', INCOMING_PORT))

    #print 'PYinc listening... on port',INCOMING_PORT
    inc_s.listen(1)
    inc_conn, inc_addr = inc_s.accept()

    print ('PYinc: connector address is', inc_addr)

    while not closing_time:
        msg = inc_conn.recv(BUFFER_SIZE)
        
        if not msg:
            #print ('PYinc: waiting for message')
            time.sleep(SNOOZE_TIME)
            continue

        inbox_q.put(msg)

        print ("PYinc: received", msg)
        time.sleep(SNOOZE_TIME)

# sends messages to whisper-c via socket, pulling from the outbox_q
def c_speaker(outbox_q):
    global OUTGOING_PORT
    OUTGOING_PORT = find_free_port()

    out_s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    out_s.bind(('localhost', OUTGOING_PORT))

    print ('PYout: listening')
    out_s.listen(1)
    out_conn, out_addr = out_s.accept()

    print ('PYout: connector address is',out_addr)

    while not closing_time:
        # pull something from the queue here
        try: # retrieve a message from the up_q
            msg = outbox_q.get_nowait()
        except:
            #print ('PYout: no messages :(')
            time.sleep(SNOOZE_TIME)
            continue

        # got something! send it.
        ret_val = out_conn.send(msg.encode())
        
        if not ret_val: 
            print ('PYout unable to send, sleeping')
            time.sleep(SNOOZE_TIME)
            continue

        print ("PYout sent msg:", msg)
        time.sleep(SNOOZE_TIME*5)

# TODO a handler for whisper-c when stable enough to run unsupervised
def c_handler():
    while INCOMING_PORT == 0 or OUTGOING_PORT == 0:
        print ("PYCH: waiting for ports to be acquired",INCOMING_PORT,OUTGOING_PORT)
        time.sleep(SNOOZE_TIME)   # reversed for whisper mirror in C
    proc_call = WHISPER_C_PROC_CALL % (OUTGOING_PORT,INCOMING_PORT)
    print ('PYCH: running:',proc_call)

    p = subprocess.Popen(proc_call,shell=True,
        stdin=subprocess.PIPE,stdout=subprocess.PIPE,
        stderr=subprocess.PIPE) # for tidiness

    while not closing_time:
        try:
            msg = p.stdout.readline().strip('\n')
            if len(msg) > 0:
                print ('PYCH',msg)
        except: # no message? snooze.
            time.sleep(SNOOZE_TIME)
            continue

    # reminder from old code: this is a bad idea. don't do it.
    #while p.stdout:
    #    print p.stdout.readline()

    # p.communicate() # for cleanup, if not using kill()
    # p.kill() # for cleanup, if not using communicate()

# summon threads to communicate with wihsper-c
def summon_comms_threads(inbox_q,outbox_q):
    c_listener_t = threading.Thread(target=c_listener, args = [inbox_q])
    c_listener_t.daemon = True # so it does with the host process
    THREAD_BUCKET.append(c_listener_t) # for easier cleanup
    c_listener_t.start()

    c_speaker_t = threading.Thread(target=c_speaker, args = [outbox_q])
    c_speaker_t.daemon = True # so it does with the host process
    THREAD_BUCKET.append(c_speaker_t) # for easier cleanup
    c_speaker_t.start()

# summon a thread to handle the whisper-c instance, when sufficiently
# stable to run unsupervised
def summon_handler(inbox_q,outbox_q):
    c_handler_t = threading.Thread(target=c_handler)
    c_handler_t.daemon = True # so it does with the host process
    THREAD_BUCKET.append(c_handler_t) # for easier cleanup
    c_handler_t.start()

if __name__ == "__main__":
    signal.signal(signal.SIGINT, signal_handler)
    summon_comms_threads(inbox_q,outbox_q)

    # wait for the threads to start up
    time.sleep(SNOOZE_TIME)

    print (OUTGOING_PORT,INCOMING_PORT) # until we can use c_handler
    # summon_handler(inbox_q,outbox_q)

    asyncio.get_event_loop().run_until_complete(postal_sorter(inbox_q,outbox_q))

    # stops when killed

