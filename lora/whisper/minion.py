#!/usr/bin/env python

'''**************************************************************************

File: minion.py
Language: Python 3.6/7
Author: Juliette Zerick (jzerick@iu.edu)
        for the WildfireDLN Project
        OPEN Networks Lab at Indiana University-Bloomington

This contains the higher-level protocol and switch behaviors for a single
device in the class minion. Each instance interfaces with a whisper-c 
instance (compiled from whisper.cpp), which quickly filters relevant messages
that are passed on.

A number of threads were used to logically separate production and consumption
of messages for ease of troubleshooting. The use of queues adds a cushion to
avoid lost messages, providing some resiliency.

Last modified: March 18, 2019

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

# for uploading files
#import libdlt

if wg.USING_UNIS:
    try:
        #from unis import Runtime # is there a difference?
        from unis.runtime import Runtime   
        from unis.models import Node, schemaLoader
        #rt = Runtime('http://localhost:9000')
    except:
        pass

BROADCASTING = False # when testing hardware receipt, one device broadcasts on loop
RESTART_TIME = 1.0 # seconds; the time it takes for the LoRa driver to restart

ANY_DEVICE = '^' # or think of something clever
VAR_DOES_NOT_EXIST = 'VARDNE'

def snooze_and_wait(Q):
    while not wg.closing_time:
        if Q.qsize() > 0:
            try:
                item = Q.get_nowait()
                return item
            except: # no message? snooze.
                pass

        time.sleep(SNOOZE_TIME)

def get_fake_mac_addr():
    S = ''
    for i in range(12): S += random.choice('0123456789abcdef')
    return S

# borrowed this from the DLN ferry code # TODO cite
def register_or_retrieve_node(dev_id):
    n = wg.rt.nodes.where({'dev_id': dev_id})
    try: # allow for reuse. alternatively, throw an error.
        n = next(n)
        log.unis_updates('node with dev_id=%s found' % (dev_id))
    except StopIteration:
        log.unis_updates('node with dev_id=%s not found, creating now' % (dev_id))
        n = Node()
        #n.dev_id = dev_id # here, this creation and assignment fails
        wg.rt.insert(n, commit=True) 
        wg.rt.flush() 

    update_var(n,'dev_id',dev_id)   
    return n

def node_has_var(node,var_name):
    return var_name in node._obj.__dict__ 

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

#TODO cite meeting notes
# updates a variable by the name of var_name in the given node; if the variable doesn't
# exist, it will be created
def update_var(node,var_name,val):
    if node_has_var(node,var_name):
        S = 'node.%s = %s' % (var_name,quote(val))
        log.unis_updates('variable %s was found, setting now via %s' % (var_name,S))
        exec(S)
        wg.rt.flush() 
        return
    '''
    S = 'node.%s = %s' % (var_name,quote(val))
    log.unis_updates('variable %s not found, creating now via %s' % (var_name,S))
    exec(S)
    wg.rt.flush() 
    node.extendSchema(var_name)
    wg.rt.flush()
    '''
    S = 'node.extendSchema(\'%s\',%s)' % (var_name,quote(val))
    log.unis_updates('variable %s not found, creating now via %s' % (var_name,S))
    exec(S)
    
class minion:
    def __init__(self,name,rt):
        self.name = name

        if SIM_MODE:
            self.mac_addr = get_fake_mac_addr()
        else:
            self.mac_addr = get_my_mac_addr() 
    
        self.node = register_or_retrieve_node(self.mac_addr)
        update_var(self.node,'name',self.name)
    
        # queues for managing incoming and outgoing messages
        self.inbox_q = queue.Queue()
        self.is_listening = True
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

        # filters for streaming replies
        self.filtered_streams = []

    def transmit(self,lmsg):
        self.is_listening = False
        self.outbox_q.put(lmsg)
        time.sleep(RESTART_TIME)
        self.is_listening = True

    # used in simulation
    def __eq__(self,other):
        return self.name == other.name

    def dump_outbox(self):
        if not SIM_MODE:
            return []
        
        bundle = []
    
        while not wg.closing_time and self.outbox_q.qsize() > 0:
            try:
                item = self.outbox_q.get_nowait()
            except: # no message
                break

            bundle.append(item)                
                
            if item.sim_key not in self.outbox_record:
                self.outbox_record[item.sim_key] = 0
                
            self.outbox_record[item.sim_key] += 1  
                
        return bundle

    # will be a freestanding thread; handles broadcasting
    def promoter(self):
        log.thread_status_updates('thread active for %s' % (self.name))

        while not wg.closing_time:
            current_time = now()

            # TODO put this back in when ready
            #for i in range(len(self.msg_store.thresholds)-1,-1,-1): # avoid indexing issues
            #    if self.msg_store.thresholds.keys()[i].time_to_die > now:
            #        del self.msg_store.thresholds[self.msg_store.thresholds.keys()[i]]

            # note that the list() cast must be performed lest a pickling error occur
            K = copy.deepcopy(list(self.msg_store.thresholds.keys()))

            for k in K:
                p = self.msg_store.thresholds[k]

                if random.random() < p:
                    self.transmit(self.msg_store.inventory[k])

            time.sleep(SNOOZE_TIME)

    def bodiless_setup(self,curr_lat,curr_long):
        self.curr_lat = curr_lat
        self.curr_long = curr_long

    # handles requests for GPS location, tracks the location of other devices
    def cartographer(self):
        log.thread_status_updates('thread active for %s' % (self.name))

        while not wg.closing_time:
            lmsg = snooze_and_wait(self.carto_q)
            
            if wg.closing_time:
                break
            
            lmsg.hit_carto = True

            # we've got mail!
            log.debug('%s received mail %s' % (self.name,lmsg.skey))
            
            # TODO need to put these back in?
            # is this a request? if not, send it to UNIS to harvest information
            #if lmsg.msg_type in [MSG_TYPE_POS_UPDATE,MSG_TYPE_POS_RESPONSE]:
            #    self.altar_q.put(lmsg)
            #    continue
            
            # we have a request. is it one this device needs to respond to?
            #if lmsg.recipient_addr != self.mac_addr and not lmsg.multicast:
            #    del lmsg
            #    continue
            
            if lmsg.msg_type in [MSG_TYPE_POS_UPDATE,MSG_TYPE_POS_RESPONSE]:
                recipient_addr = lmsg.recipient_addr
                if recipient_addr not in self.eavesdrop_record:
                    self.eavesdrop_record[recipient_addr] = []
                self.eavesdrop_record[recipient_addr].append(lmsg)
                continue

            if not SIM_MODE:
                curr_lat, curr_long = retrieve_gps()
            else:
                curr_lat, curr_long = self.curr_lat, self.curr_long

            lmsg.response_payload = '%f,%f' % (curr_lat,curr_long)
            lmsg.send_response(self.mac_addr,self.outbox_q,self.msg_store)
            
            if SIM_MODE: 
                self.response_record[lmsg.skey] = lmsg.response_lmsg.skey
        
    # only receives messages with saturation requests
    def continue_bloom(self,lmsg): # note that lmsg is a duplicate and safe to modify
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
        
        log.debug('%s amplified %s' % (self.name,lmsg.skey))
        self.msg_store.incorporate(lmsg.response_lmsg) 

    # dedicated to the many-armed Postal Sorting Alien from "Men in Black II"
    def postal_sorter(self):
        log.thread_status_updates('many-armed postal sorter active for %s' % (self.name))

        if SIM_MODE:
            self.inbox_record = {}
            self.outbox_record = {}
            self.response_record = {}
            self.eavesdrop_record = {}

        while not wg.closing_time:
            lmsg = snooze_and_wait(self.inbox_q)

            if wg.closing_time:
                break        
            
            if not lmsg.pkt_valid:
                log.debug('%s received mail that was invalid, packet=%s' % (self.name,lmsg.initial_pkt))
                continue
            
            lmsg.hit_sorter = True
            
            if SIM_MODE:
                if lmsg.key not in self.inbox_record:
                    self.inbox_record[lmsg.sim_key] = 0
                    
                self.inbox_record[lmsg.sim_key] += 1

            # we've got mail!
            log.debug('%s got mail %s' % (self.name,lmsg.skey))

            # check filters to see if we need to stream the message elsewhere
            for (F,G) in self.filtered_streams:
                if F(lmsg):
                    G(lmsg)

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

            # TODO check logic, place notes here
            # accept any responses to harvest for data
            
            # don't accept requests unless this device needs to respond to them 
            if msg_type_is_request(lmsg.msg_type) \
                and not (lmsg.recipient_addr == self.mac_addr or lmsg.multicast):
                # don't delete the message in the event it requested saturation
                continue

            if lmsg.msg_type in ALTAR_KEEPER_DOMAIN:
                log.unis_updates('altar keeper has incoming mail')
                self.altar_q.put(lmsg)        

            elif lmsg.msg_type in CARTOGRAPHER_DOMAIN:
                self.carto_q.put(lmsg)
    '''
    Notes from February 13, 2019 OPEN Lab Meeting:
	- You can create objects like Nodes, but for it to be tracked by UNISrt, you must insert it,
	i.e. if n is a Node, rt.insert(n,commit=True) will push it to the backend. Accessing its ID
	is n.id; furthermore:
	    - n._obj.__dict__ will output a list of all the objects associated with that Node instance
		    To add a property:
		        n.bar = 'baz'
		        rt.flush() 
		        n.extendSchema('bar')
		        rt.flush()
	    - Object Link builds on JSON's bifurcation of a class, with the 'directed' property
	    indicating whether the object should be a dict or something else (note to self: don't
	    mess with it)
	- You can use Ports to define endpoints between Nodes, or other objects (make sure you
	add the objects to UNIS first)
    - DataCollection tracks data streams, and register functions associated with them (e.g. "sum")
    '''
    # takes in requests from special altar_q
    def altar_keeper(self):
        log.thread_status_updates('thread active for %s' % (self.name))

        while not wg.closing_time:
            lmsg = snooze_and_wait(self.altar_q)

            if wg.closing_time:
                break

            lmsg.hit_altar = True
            log.data_flow('%s received offering %s' % (self.name,lmsg.skey))

            # TODO add timestamp? 
            
            # format: relevant device address, variable to update, updated value
            # where the addressee is the MAC address of a specific device, or 
            # '*' to match any device, so long as the message is multicast
            M = lmsg.payload.split(',')
            if len(M) < 2 or len(M) > 4:
                del lmsg
                continue    

            # ruling out misconfigured messages
            dev_id = M[0]
            if not (is_plausible_MAC_addr(dev_id) or dev_id == ANY_DEVICE and lmsg.multicast):
                del lmsg
                continue

            var_name = M[1]

            # all checks passed, onward to parsing
            log.data_flow('%s%s' % (self.name,lmsg.show_request()))

            # POST
            if lmsg.msg_type in [MSG_TYPE_UNIS_POST_NOTIF,MSG_TYPE_UNIS_POST_ACK_REQ, \
            MSG_TYPE_UNIS_POST_ACK,MSG_TYPE_UNIS_GET_RESPONSE]: 
                if dev_id == ANY_DEVICE: # invalid parameter here
                    continue

                val = M[2]
                
                # don't insert into UNIS
                if lmsg.msg_type == MSG_TYPE_UNIS_GET_RESPONSE and val == VAR_DOES_NOT_EXIST:
                    log.data_flow('%s::%s does not exist' % (dev_id,var_name))
                    continue # TODO must map reply to receptionist
                
                # TODO should give some response via receptionist
                if lmsg.msg_type == MSG_TYPE_UNIS_POST_ACK
                    continue
                
                # that leaves us with MSG_TYPE_UNIS_POST_NOTIF, MSG_TYPE_UNIS_POST_ACK_REQ, and
                # MSG_TYPE_UNIS_GET_RESPONSE with existing variables
                
                node = register_or_retrieve_node(dev_id)
                update_var(node,var_name,val)
                
                # mirror the broadcast to match messages later
                if lmsg.msg_type == MSG_TYPE_UNIS_POST_ACK_REQ:
                    lmsg.response_payload = lmsg.payload 
                    lmsg.send_response(self.mac_addr,self.outbox_q,self.msg_store)
                    log.data_flow('%s%s' % (self.name,lmsg.show_response()))
                    
            # GET
            elif lmsg.msg_type in [MSG_TYPE_UNIS_GET_REQUEST]: # return requested information
            
                if dev_id == ANY_DEVICE or dev_id == self.mac_addr: # valid parameter here
                    node = self.node
                else:
                    node = register_or_retrieve_node(dev_id)

                if not node_has_var(node,var_name) and lmsg.msg_type == MSG_TYPE_UNIS_GET_REQUEST:
                    lmsg.response_payload = '%s,%s,%s' % (self.mac_addr,var_name,VAR_DOES_NOT_EXIST)
                    lmsg.send_response(self.mac_addr,self.outbox_q,self.msg_store)
                    log.data_flow('%s%s' % (self.name,lmsg.show_response()))
                    continue
                    
                S = 'node.%s' % (var_name)
                val = eval(S)

                lmsg.response_payload = '%s,%s,%s' % (self.mac_addr,var_name,str(val))
                lmsg.send_response(self.mac_addr,self.outbox_q,self.msg_store)
                log.data_flow('%s%s' % (self.name,lmsg.show_response()))

    # listens for messages from whisper-c via socket, pushes received messages to the
    # inbox_q
    def c_listener(self):
        log.thread_status_updates('thread active')

        self.incoming_port = find_free_port()

        self.inc_s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.inc_s.bind(('localhost', self.incoming_port))
        SOCKET_BUCKET.append(self.inc_s)

        log.plumbing_issues('listening on port %d...' % (self.incoming_port))
        self.inc_s.listen(1)
        inc_conn, inc_addr = self.inc_s.accept()

        log.plumbing_issues('connector address is %s' % (str(inc_addr)))

        while not wg.closing_time:
            if wg.whisper_c_is_dead:
                log.plumbing_issues('listening on port %d...' % (self.incoming_port))
                self.inc_s.listen(1)
                inc_conn, inc_addr = self.inc_s.accept()

                log.plumbing_issues('connector address is %s' % (str(inc_addr)))

            try:
                stream = inc_conn.recv(BUFFER_SIZE)
            except:
                log.plumbing_issues('whisper-c is dead?')
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

            log.plumbing_issues("received packet: %s" % (pkt))

            msg = lora_message(pkt)

            if msg.pkt_valid:
                self.inbox_q.put(msg)
            else:
                log.plumbing_issues('PACKET INVALID')

            time.sleep(SNOOZE_TIME)

        log.plumbing_issues('closing time! shutting down')        

    # sends messages to whisper-c via socket, pulling from the outbox_q
    def c_speaker(self):
        log.thread_status_updates('thread active')

        self.outgoing_port = find_free_port()

        self.out_s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.out_s.bind(('localhost', self.outgoing_port))
        SOCKET_BUCKET.append(self.out_s)

        log.plumbing_issues('listening on port %d...' % (self.outgoing_port))
        self.out_s.listen(1)
        out_conn, out_addr = self.out_s.accept()

        log.plumbing_issues('connector address is %s' % (str(out_addr)))

        # if response not ready, put it back in the queue
        while not wg.closing_time:
            if wg.whisper_c_is_dead:
                log.plumbing_issues('listening on port %d...' % (self.outgoing_port))
                self.out_s.listen(1)
                out_conn, out_addr = self.out_s.accept()

                log.plumbing_issues('connector address is %s' % (str(out_addr)))

            # pull something from the queue here
            lmsg = snooze_and_wait(self.outbox_q)
            
            if wg.closing_time:
                break

            # keep this code in the event it's needed in the future:
            # check for the response payload as an indicator of readiness
            #try:
            #    lmsg.response # not lmsg.response_payload
            #except: # if not, put it back in queue
            #    self.transmit(lmsg)
            #    continue

            # got something! send it.
            pkt = lmsg.form_pkt()

            try:
                ret_val = out_conn.send(pkt.encode())
            except: # if we can't send, we have problems
                retval = False
          
            if not ret_val: 
                self.transmit(pkt) #TODO check: is this supposed to be lmsg?
                log.plumbing_issues('whisper-c is dead?')
                wg.whisper_c_is_dead = True
                continue

            log.plumbing_issues("sent packet: %s" % (pkt))
            #time.sleep(SNOOZE_TIME)

    def broadcaster(self):
        while not wg.closing_time:
            pkt = '%s/0/%s/%f/%d/wdln//-42|' % (MULTICAST,self.mac_addr,now(),
                MSG_TYPE_POS_REQUEST)

            lmsg = lora_message(pkt)
            log.plumbing_issues('%s put dummy request in queue' % (self.name))
            self.transmit(lmsg)
            time.sleep(5)
            
    def prod_flood(self,recipient,msg_type,payload):
        send_time = now()
        pkt = '%s/1,%s,%f/%s/%f/%d/%s//-42|' % (recipient,self.mac_addr,send_time,
            self.mac_addr,send_time,msg_type,payload)
        lmsg = lora_message(pkt)
        
        if not lmsg.pkt_valid:
            log.packet_errors('%s warns the saturation request is invalid!' % (self.name))
            return 
        
        log.plumbing_issues('%s put the saturation request in queue' % (self.name))
        self.outbox_q.put(lmsg)
        
        return lmsg

    def prod_spurt(self,recipient,msg_type,payload):
        send_time = now()
        pkt = '%s/0/%s/%f/%d/%s//-42|' % (recipient,self.mac_addr,send_time,
            msg_type,payload)
        lmsg = lora_message(pkt)
        
        if not lmsg.pkt_valid:
            log.packet_errors('%s says the non-saturation packet is invalid!' % (self.name))
            return 
        
        log.plumbing_issues('%s put the non-saturation packet in queue' % (self.name))
        self.transmit(lmsg)

        return lmsg

    # handles requests as delegated by the receptionist. 
    # if this becomes too computationally intensive, launch a separate process that
    # gets a streaming feed of incoming data
    def intern(self,recipient_addr,saturation_req,msg_type,payload,timeout):
        start_time = now()

        packet_no_bloom =           '%s/0/%s/%f/%d/%s//:'
        packet_with_bloom = '%s/%d,%s,%f/%s/%f/%d/%s//:'

        # keeping variable names for the sake of clarity
        init_send_time = now()
        send_time = init_send_time
        
        init_sender_addr = self.my_mac_addr
        sender_addr = init_sender_addr

        # TODO replace with prod functions
        if saturation_req:
            S = packet_with_bloom % (recipient_addr,
                1,init_sender_addr,init_send_time,
                sender_addr,
                send_time,
                msg_type,
                payload)
        else:
            S = packet_no_bloom % (recipient_addr,
                sender_addr,
                send_time,
                msg_type,
                payload)

        lmsg = lora_message(S)
        
        # TODO want an error response here?
        if not lmsg.pkt_valid:
            pass

        exp_recipient_addr = lmsg.sender_addr
        exp_msg_type = get_response_msg_type(lmsg.msg_type)
        exp_saturation_req = lmsg.saturation_req

        # create filter and response
        def F(rmsg):
            return rmsg.recipient_addr == exp_recipient_addr \
            and rmsg.msg_type == exp_msg_type \
            and rmsg.saturation_req == exp_saturation_req

        def G(rmsg):
            T = '(%s,%f,%s)' % (rmsg.sender_addr,rmsg.send_time,rmsg.payload)
            # TODO add socket transmission

        
        # add (filter,response) tuple to self.filtered_streams

        while True:
            if now() - start_time > timeout:
                break
            time.sleep(SNOOZE_TIME)                
                
        # TODO cleanup: delete queue objects, sockets, etc.

    # TODO accepts requests via TCP connection then summons an intern to handle it
    def receptionist(self,rt):
        pass
    
    def summon_receptionist(self):
        self.receptionist_t = threading.Thread(target=self.receptionist, args = [rt])
        self.receptionist_t.daemon = True # so it does with the host process
        THREAD_BUCKET.append(self.receptionist_t) # for easier cleanup
        self.receptionist_t.start()

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
            log.plumbing_issues('waiting for ports to be acquired')
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

        #self.promoter_t = threading.Thread(target=self.promoter)
        #self.promoter_t.daemon = True # so it does with the host process
        #THREAD_BUCKET.append(self.promoter_t) # for easier cleanup
        #self.promoter_t.start()

    def begin(self):
        # if not running bodiless, create limbs
        if not SIM_MODE:
            self.summon_comms_threads()
            time.sleep(3) # wait for the threads to start up

        # before summoning the rest
        self.summon_keepers()#rt)

        if not SIM_MODE:
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
