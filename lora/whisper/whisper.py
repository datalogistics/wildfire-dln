#!/usr/bin/env python

'''**************************************************************************

File: whisper.py
Language: Python 3.7
Author: Juliette Zerick (jzerick@iu.edu)
        for the WildfireDLN Project
        OPEN Networks Lab at Indiana University-Bloomington

This contains the higher-level protocol and switch behaviors. It interfaces
with whisper.cpp, which quickly filters relevant messages that are passed
to whisper.py.

A number of threads were used to logically separate production and consumption
of messages for ease of troubleshooting. The use of queues adds a cushion to
avoid lost messages, providing some resiliency.

Last modified: December 6, 2018

****************************************************************************'''

import signal
import sys
import socket
import argparse
import subprocess
import queue
import random
import threading

from whisper_protocol import *
import whisper_globals as wg

# for CoAP
#import asyncio 
#from aiocoap import *
#import aiocoap.resource as resource

# for UNISrt
from unis.runtime import Runtime

# for uploading files
#import libdlt

BROADCASTING = True
RUNNING_BODILESS = False
USING_C_HANDLER = True
USING_UNIS = False

def snooze_and_wait(Q):
    while not wg.closing_time:
        if Q.qsize() > 0:
            try:
                item = Q.get_nowait()
                return item
            except: # no message? snooze.
                pass
                
        time.sleep(SNOOZE_TIME)

# Solution from Matt J (2009), then edited by Grimthorr (2018) at StackOverflow 
# in response to the following posted question:
# "How do I capture SIGINT in Python?" available at
# <https://stackoverflow.com/questions/1112343/how-do-i-capture-sigint-in-python>
# last accessed: November 27, 2018
def signal_handler(sig, frame):
    wg.closing_time = True
    time.sleep(SNOOZE_TIME)
    mopup()

class minion:
    def __init__(self,name,rt):
        self.name = name
        self.mac_addr = get_my_mac_addr()
        self.rt = rt
    
        # queues for managing incoming and outgoing messages
        self.inbox_q = queue.Queue()
        QUEUE_BUCKET.append(self.inbox_q)
        self.outbox_q = queue.Queue()
        QUEUE_BUCKET.append(self.outbox_q)

        # additional queues for storing observed messages
        self.carto_q = queue.Queue()
        QUEUE_BUCKET.append(self.carto_q)
        self.altar_q = queue.Queue()
        QUEUE_BUCKET.append(self.altar_q)

        # a message_store instance for managing transient memory 
        self.msg_store = message_store()
        
        # will be updated by main once threads establish sockets
        self.incoming_port = 0
        self.outgoing_port = 0

        # socket IDs for incoming and outgoing ports
        self.inc_s = 0
        self.out_s = 0

    # will be a freestanding thread; handles broadcasting
    def promoter(self):
        log.info('thread active')

        while not wg.closing_time:
            current_time = now()

            for i in xrange(len(self.msg_store)-1,-1,-1): # avoid indexing issues
                if self.msg_store.keys()[i].time_to_die > now:
                    del self.msg_store[self.msg_store.keys()[i]]

            for k in self.msg_store.keys():
                p = self.msg_store.thresholds[k]

                if random.random() < p:
                    self.outbox_q.put(self.msg_store.inventory[k])

            time.sleep(SNOOZE_TIME)

    # handles requests for GPS location, tracks the location of other devices
    def cartographer(self):
        log.info('thread active')

        while not wg.closing_time:
            lmsg = snooze_and_wait(self.carto_q)
            
            if wg.closing_time:
                break
            
            lmsg.hit_carto = True

            # we've got mail!
            log.debug('received mail %s' % lmsg.skey)
            
            # is this a request? if not, send it to UNIS to harvest information
            if lmsg.msg_type in [MSG_TYPE_POS_UPDATE,MSG_TYPE_POS_RESPONSE]:
                self.altar_q.put(lmsg)
            
            # we have a request. is it one this device needs to respond to?
            if lmsg.recipient_addr != self.mac_addr and not lmsg.multicast:
                del lmsg
                continue
            
            curr_lat, curr_long = retrieve_gps()
            lmsg.response_payload = '%f,%f' % (curr_lat,curr_long)
            lmsg.send_response(self.mac_addr,self.outbox_q,self.msg_store)
            
        log.info('closing time, shutting down')
        
    # only receives messages with saturation requests
    def continue_bloom(self,lmsg):
        if not lmsg.saturation_req:
            return
        
        def bloom():
            return '%d,%s,%f' % (lmsg.bloom_count + 1, lmsg.init_sender_addr,lmsg.init_send_time)

        # defaults, changed as necessary
        RECIP_FIELD = lmsg.recipient_addr
        SAT_FIELD = bloom()

        SENDER_FIELD = self.mac_addr
        TIME_FIELD = now()
        MSG_TYPE_FIELD = lmsg.msg_type
        PAYLOAD = lmsg.payload 
        RESERVED_FIELD = RESERVED_DEFAULT

        F = [RECIP_FIELD,SAT_FIELD,SENDER_FIELD,TIME_FIELD,MSG_TYPE_FIELD,PAYLOAD,RESERVED_FIELD]
        lmsg.gen_response_pkt(F)
        
        log.debug('amplified %s' % (lmsg.skey))
        self.msg_store.incorporate(lmsg.response_lmsg)  

    # dedicated to the unnamed Postal Sorting Alien from "Men in Black II"
    def postal_sorter(self):
        log.info('many-armed thread active')

        while not wg.closing_time:
            lmsg = snooze_and_wait(self.inbox_q)

            if wg.closing_time:
                break        
            
            if not lmsg.pkt_valid:
                log.debug('received mail was invalid %s' % (lmsg.initial_pkt))
                continue
            
            lmsg.hit_sorter = True

            # we've got mail!
            log.debug('got mail %s' % lmsg.skey)

            # have we seen this before? update the inventory. in the case of non-saturation
            # requests, the conditional prevents this device from replying repeatedly.
            if lmsg.key in self.msg_store.inventory:
                self.msg_store.update_inventory(lmsg)
                continue

            # push along packets with saturation requests, unless this device is
            # the intended recipient, or if it's a multicast
            if lmsg.saturation_req and (lmsg.recipient_addr != self.mac_addr or lmsg.multicast):
                flyer = copy.deepcopy(lmsg)
                self.continue_bloom(flyer)

            # accept any responses to harvest for data
            
            # don't accept requests unless this device needs to respond to them
            if msg_type_is_request(lmsg.msg_type) \
                and not (lmsg.recipient_addr == self.mac_addr or lmsg.multicast):
                # don't delete the message in the event it requested saturation
                continue

            if lmsg.msg_type in ALTAR_KEEPER_DOMAIN:
                self.altar_q.put(lmsg)        

            elif lmsg.msg_type in CARTOGRAPHER_DOMAIN:
                self.carto_q.put(lmsg)

    # takes in requests from special altar_q
    def altar_keeper(self):
        log.info('thread active')

        if USING_UNIS:
            my_hostname = socket.gethostname()
            my_ferry_exnode = rt.nodes.where({"name": my_hostname})

        # get the names of exnodes already present in UNIS
        MAC2index = {}
        MAC2GPS = {}

        while not wg.closing_time:
            # note: the following is draft code

            # don't use snooze_and_wait for this. see the sleep call under except:
            try:
                lmsg = self.altar_q.get_nowait()
            except: 
                time.sleep(SNOOZE_TIME)

                # searching and creating: near-polynomial time
                # recreating the mapping: linear time
                '''
                del MAC2index
                MAC2index = {}
                
                for i in range(len(self.rt.exnodes)):
                    MAC2index[exn.MAC] = i
                '''
                continue

            if wg.closing_time:
                break

            if lmsg.msg_type in [MSG_TYPE_POS_UPDATE,MSG_TYPE_POS_RESPONSE]:
                s = lmsg.payload.split(',')
                c = (float(s[0]),float(s[1]))

                if lmsg.saturation_req:
                    MAC2GPS[lmsg.init_sender_addr] = c
                    obs_obj = lmsg.init_sender_addr
                    obs_time = lmsg.init_send_time 
                else:
                    MAC2GPS[lmsg.sender_addr] = c
                    obs_obj = lmsg.sender_addr
                    obs_time = lmsg.send_time

                log.info('observed %s@(%f,%f) at t=%f' % (obs_obj, c[0],c[1],obs_time))

            continue

            # TODO TODO TODO TODO 

            lmsg.hit_altar = True

            # is this a message from which to harvest information?


            # format: command,MAC address of relevant device,variable to update,updated value

            M = lmsg.split(',')
            if len(M) < 3 or len(M) > 4:
                del lmsg
                continue    

            action_type = M[0].upper()
            if action_type not in ['POST','GET']:
                del lmsg
                continue

            dev_id = M[1]
            if not is_plausible_MAC_addr(dev_id):
                del lmsg
                continue

            var_name = M[2]

            # default response
            S = 'ERROR'

            if action_type == 'POST': # absorb the new information
                updated_val = M[3]
                S = 'self.rt.exnodes[%d].%s = %s' % (MAC2index(dev_id),var_name,str(updated_val))
                
                try:
                    exec(S)
                except NameError:
                    pass
            elif action_type == 'GET': # return requested information
                S = 'self.rt.exnodes[%d].%s' % (MAC2index(dev_id),var_name)

                try:
                    val = eval(S)
                except NameError:
                    pass

            lmsg.response_payload = S
            lmsg.send_response(self.mac_addr,self.outbox_q,self.msg_store)

    # listens for messages from whisper-c via socket, pushes received messages to the
    # inbox_Q
    def c_listener(self):
        log.info('thread active')

        self.incoming_port = find_free_port()

        self.inc_s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.inc_s.bind(('localhost', self.incoming_port))
        SOCKET_BUCKET.append(self.inc_s)

        log.info('listening on port %d...' % (self.incoming_port))
        self.inc_s.listen(1)
        inc_conn, inc_addr = self.inc_s.accept()

        log.info('connector address is %s' % (str(inc_addr)))

        while not wg.closing_time:
            if wg.whisper_c_is_dead:
                log.info('listening on port %d...' % (self.incoming_port))
                self.inc_s.listen(1)
                inc_conn, inc_addr = self.inc_s.accept()

                log.info('connector address is %s' % (str(inc_addr)))

            try:
                stream = inc_conn.recv(BUFFER_SIZE)
            except:
                log.debug('whisper-c is dead?')
                wg.whisper_c_is_dead = True
                continue

            if not stream:
                #print ('waiting for a message')
                time.sleep(SNOOZE_TIME)
                continue

            # hack off a piece of the stream, then put it in pkt
            try: # this can blow up
                pkt = stream.decode()
            except:
                continue

            log.debug("received packet: %s" % (pkt))

            msg = lora_message(pkt)

            if msg.pkt_valid:
                self.inbox_q.put(msg)
            else:
                log.warning('PACKET INVALID')

            time.sleep(SNOOZE_TIME)

        log.info('closing time! shutting down')        

    # sends messages to whisper-c via socket, pulling from the outbox_q
    def c_speaker(self):
        log.info('thread active')

        self.outgoing_port = find_free_port()

        self.out_s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.out_s.bind(('localhost', self.outgoing_port))
        SOCKET_BUCKET.append(self.out_s)

        log.info('listening on port %d...' % (self.outgoing_port))
        self.out_s.listen(1)
        out_conn, out_addr = self.out_s.accept()

        log.info('connector address is %s' % (str(out_addr)))

        # if response not ready, put it back in the queue
        while not wg.closing_time:
            if wg.whisper_c_is_dead:
                log.info('listening on port %d...' % (self.outgoing_port))
                self.out_s.listen(1)
                out_conn, out_addr = self.out_s.accept()

                log.info('connector address is %s' % (str(out_addr)))

            # pull something from the queue here
            lmsg = snooze_and_wait(self.outbox_q)
            
            if wg.closing_time:
                break        

            # keep this code in the event it's needed in the future:
            # check for the response payload as an indicator of readiness
            #try:
            #    lmsg.response # not lmsg.response_payload
            #except: # if not, put it back in queue
            #    outbox_q.put(lmsg)
            #    continue

            # got something! send it.
            pkt = lmsg.form_pkt()

            try:
                ret_val = out_conn.send(pkt.encode())
            except: # if we can't send, we have problems
                retval = False
          
            if not ret_val: 
                self.outbox_q.put(pkt)
                log.debug('whisper-c is dead?')
                wg.whisper_c_is_dead = True
                continue

            time.sleep(1)
            log.info("sent packet: %s" % (pkt))
            #time.sleep(SNOOZE_TIME)

    def broadcaster(self):
        while not wg.closing_time:
            pkt = '%s/0/%s/%f/%d/wdln//-42|' % (MULTICAST,self.mac_addr,now(),
                MSG_TYPE_POS_REQUEST)

            lmsg = lora_message(pkt)
            log.info('put dummy request in queue')
            self.outbox_q.put(lmsg)
            time.sleep(5)

    def summon_broadcaster(self):
        self.broadcaster_t = threading.Thread(target=self.broadcaster, args = [])
        self.broadcaster_t.daemon = True # so it does with the host process
        THREAD_BUCKET.append(self.broadcaster_t) # for easier cleanup
        self.broadcaster_t.start()

    # summon threads to communicate with wihsper-c
    def summon_comms_threads(self):
        self.c_listener_t = threading.Thread(target=self.c_listener, args = [])
        self.c_listener_t.daemon = True # so it does with the host process
        THREAD_BUCKET.append(self.c_listener_t) # for easier cleanup
        self.c_listener_t.start()

        self.c_speaker_t = threading.Thread(target=self.c_speaker, args = [])
        self.c_speaker_t.daemon = True # so it does with the host process
        THREAD_BUCKET.append(self.c_speaker_t) # for easier cleanup
        self.c_speaker_t.start()

    # a handler for whisper-c when stable enough to run unsupervised
    def c_handler(self):
        start_whisper_c(self.outgoing_port,self.incoming_port)
    
        while not wg.closing_time: 
            if wg.whisper_c_is_dead:
                dispose_of_whisper_c() # whisper-c is dead
                start_whisper_c(self.outgoing_port,self.incoming_port) # long live whisper-c
                wg.whisper_c_is_dead = False

            # check if the proess is still running  
            poll = wg.whisper_c_p.poll()
            if poll != None:
                wg.whisper_c_is_dead = True
                continue

            try:
                msg = p.stdout.readline().strip('\n')
            except: # no message? snooze.
                time.sleep(SNOOZE_TIME)
                continue

            if len(msg) > 0:
                log.info('\t',msg)

    # summon a thread to handle the whisper-c instance, when sufficiently
    # stable to run unsupervised
    def summon_handler(self):
        while self.incoming_port == 0 or self.outgoing_port == 0:
            log.info('waiting for ports to be acquired')
            time.sleep(SNOOZE_TIME)   # reversed for whisper mirror in C

        self.c_handler_t = threading.Thread(target=self.c_handler, args=[])
        self.c_handler_t.daemon = True # so it does with the host process
        THREAD_BUCKET.append(self.c_handler_t) # for easier cleanup
        self.c_handler_t.start()

    def summon_keepers(self):#rt):
        self.altar_t = threading.Thread(target=self.altar_keeper,args=[])#rt])
        self.altar_t.daemon = True # so it does with the host process
        THREAD_BUCKET.append(self.altar_t) # for easier cleanup
        self.altar_t.start()

        self.post_t = threading.Thread(target=self.postal_sorter)
        self.post_t.daemon = True # so it does with the host process
        THREAD_BUCKET.append(self.post_t) # for easier cleanup
        self.post_t.start()

        self.carto_t = threading.Thread(target=self.cartographer)
        self.carto_t.daemon = True # so it does with the host process
        THREAD_BUCKET.append(self.carto_t) # for easier cleanup
        self.carto_t.start()

    def begin(self):
        self.summon_comms_threads()
  
        # wait for the threads to start up
        time.sleep(3)

        self.summon_keepers()#rt)

        # summon the c-handler or print out the ports to start the process manually
        if USING_C_HANDLER:
            self.summon_handler()
        else: # notify the user they need to run whisper-c manually
            log.warning(\
            '''

            The c-handler has not been selected for use. Please manually execute
            the whisper module with ports being used, i.e. run the following:

            ./whisper %d %d

            ''' % (self.outgoing_port,self.incoming_port))

        if BROADCASTING:
            self.summon_broadcaster()    

#TODO check if whisper exists; if not, compile; check for daemons, etc.
def pre_flight_checks():
    return
            
def main():
    signal.signal(signal.SIGINT, signal_handler)

    rt = -1

    if USING_UNIS:
        # use fqdn to determine local endpoints
        name = socket.gethostname()
        fqdn = socket.getfqdn()

        UNIS_URL="http://localhost:8888"

        LOCAL_UNIS_HOST="localhost"
        LOCAL_UNIS_PORT=9000

        LOCAL_DEPOT={"ibp://{}:6714".format(fqdn): { "enabled": True}}
        LOCAL_UNIS = "http://{}:{}".format(fqdn, LOCAL_UNIS_PORT)

        urls = [{"default": True, "url": LOCAL_UNIS}]
        opts = {"cache": { "preload": ["nodes", "services", "exnodes"]}}

        rt = Runtime(urls, **opts)
        #sess = libdlt.Session(rt, bs="5m", depots=LOCAL_DEPOT, threads=1)

    bob = minion('bob',rt)
    bob.begin()
    
    while not wg.closing_time:
        time.sleep(SNOOZE_TIME)  

if __name__ == "__main__":
    main()

