#!/usr/bin/env python

'''**************************************************************************

File: whisper_vessel.py
Language: Python 3.6/7
Author: Juliette Zerick (jzerick@iu.edu)
        for the WildfireDLN Project
        OPeN Networks Lab at Indiana University-Bloomington

This contains the higher-level protocol and switch behaviors for a single
device in the class minion. Each instance interfaces with a whisper-c 
instance (compiled from whisper.cpp), which quickly filters relevant messages
that are passed on.

A number of threads were used to logically separate production and consumption
of messages for ease of troubleshooting. The use of queues adds a cushion to
avoid lost messages, providing some resiliency.

Last modified: August 27, 2019

****************************************************************************'''

import signal
import sys
import socket
import argparse
import subprocess
import queue
import random
import threading
import pathlib
import os
import copy
from math import floor, ceil

from scipy.interpolate import griddata
#import matplotlib.pyplot as plt # not yet

from whisper_cargo import *
import whisper_globals as wg

# check if UNIS is available. this test should have already been performed in whisper_globals.py
if wg.HAVE_UNIS:
    try:
        from unis.runtime import Runtime   
        from unis.models import Node, schemaLoader
        #rt = Runtime('http://localhost:9000') # performed in whisper_globals
    except:
        pass

# TODO fix or remove
#if wg.ANIMATING:
#    import matplotlib
#    matplotlib.use('GTK3Agg')
import matplotlib.pyplot as plt

TABLE_NONCASE = ''
UPDATE_TIME = 10
DF_DELIM = '|'
IN_THE_WEEDS = False # enable yet more verbose output for debugging
PRESUMED_DEAD = 15 # seconds of inactivity by whisper-c that indicates non-functionality

class vessel:
    # note that the small army of threads is not summoned until minion.begin() is called
    def __init__(self,name):
        self.my_name = name
        self.silent_running = False

        if SIM_MODE:
            self.my_dev_id = get_fake_mac_addr()
        else:
            self.my_dev_id = get_my_mac_addr() 

        wg.dev_id2name_mapping[self.my_dev_id] = self.my_name

        if wg.DEMOING and wg.ANIMATING:
            self.fig, self.ax = plt.subplots()

        # for storing messages, and looking them up later
        self.msg_lookup = {}

        if wg.HAVE_UNIS:
            # find or create an appropriate node
            if wg.HAVE_FERRY_NODE: # are we on a ferry with a node already?
                self.my_node = wg.MY_FERRY_NODE 
                self.my_node_name = wg.MY_FERRY_NODE_NAME
            else: 
                self.my_node = register_or_retrieve_node(self.my_dev_id) 
                self.my_node_name = self.my_dev_id
        
            # now find or add Metadata objects to stream data into DataCollections
            
            # messages are posted to the msg_stream in transmit() and by the postal_sorter
            self.my_msg_metadata = register_or_retrieve_metadata(self.my_node_name+'_msg_stream')
            self.my_msg_stream = self.my_msg_metadata.data 
            
            # data are posted to the data_stream by the gleaner
            self.my_data_metadata = register_or_retrieve_metadata(self.my_node_name+'_data_stream')
            self.my_data_stream = self.my_data_metadata.data
        
        # dependency list is a routing table for colocalization
        self.last_updated_dep_list = now()  

        # queues for managing incoming and outgoing messages, used by the threads
        # c_speaker and c_listener
        self.inbox_q = queue.Queue()
        QUEUE_BUCKET.append(self.inbox_q)
        self.outbox_q = queue.Queue()
        QUEUE_BUCKET.append(self.outbox_q)

        # additional queues for responding to requests, amplifying flood requests, etc.
        self.glean_q = queue.Queue()
        QUEUE_BUCKET.append(self.glean_q)        
        self.promotion_q = queue.Queue()
        QUEUE_BUCKET.append(self.promotion_q)
        self.request_q = queue.Queue()
        QUEUE_BUCKET.append(self.request_q)
        self.operator_q = queue.Queue()
        QUEUE_BUCKET.append(self.operator_q)

        # will be updated by main once threads establish sockets
        self.incoming_port = 0
        self.outgoing_port = 0

        # socket IDs for incoming and outgoing ports
        self.inc_s = 0
        self.out_s = 0

        # an ephemeral data store
        self.cargo = cargo_hold(self.my_name,self.my_dev_id,self.transmit)
        
        #self.my_ordering = get_ordering(self.my_dev_id)
        self.my_ordering = [1]*48
        self.my_status = RESETTING

    def transmit(self,lmsg): 
        self.glean_q.put(lmsg) # always
        
        self.my_msg_stream.append(lmsg.initial_pkt)
        
        # left as a reminder that this action is now handled by the radio_operator
        #self.outbox_q.put(lmsg) 
        
        self.operator_q.put(lmsg)

    def dump_outbox(self): 
        if not SIM_MODE:
            return []

        bundle = []
        while not wg.closing_time and self.outbox_q.qsize() > 0:
            try:
                item = self.outbox_q.get_nowait()
            except: # no messages
                break
            bundle.append(item)

        return bundle
    
    def gleaner(self):
        log.thread_status_updates(self.add_name('thread active'))
        last_updated = now()
        last_batch = []

        while not wg.closing_time:
            lmsg = snooze_and_wait(self.glean_q)
            
            # not enough time to do a last gleaner_update here
            if wg.closing_time: break
            
            # note that while the postal_sorter checks for invalid packets, transmit does not
            #if not lmsg.pkt_valid:
            #    log.packet_errors('received packet %s is invalid, tossing' % (lmsg.skey))
            #else:
            #    log.data_flow(self.add_name('received %s' % (lmsg.skey)))

            # add it to the pile
            mini_df = lmsg.generate_mini_df(self.my_dev_id)
            last_batch.append(mini_df)
            
            # get data, insert into UNIS node
            if lmsg.harvestable and wg.HAVE_UNIS \
                t = (lmsg.obs_time,lmsg.obs_dev_id,lmsg.obs_gps_lat,lmsg.obs_gps_long)
                self.my_data_stream.append(t)

            self.append_batch(last_batch) 
            last_batch = []

            # the update methods generate announcement packets which go through
            # transmit, then wind up at the gleaner. to avoid an exponentially
            # growing flood of announcement packets, don't let them trigger updates.
            # note that even with long UPDATE_TIME values and more frequent
            # updates to last_updated, the flood inevitably occurs.
            #
            # note also that the following condition is insufficient to avert 
            # such a flood. it is left as a reminder.
            #if lmsg.msg_type == MSG_TYPE_RTG: and lmsg.sender_addr == self.my_dev_id:
            if lmsg.msg_type == MSG_TYPE_RTG: # sufficient
                continue
            
            if now() - last_updated > UPDATE_TIME \
            and ((now() - last_updated) / UPDATE_TIME) < 2.:
                if not SIM_MODE:
                    self.append_batch(last_batch) 
                self.gleaner_update()
                last_batch = []
                last_updated = now() 

    # will be a freestanding thread; handles broadcasting
    def promoter(self):
        log.thread_status_updates(self.add_name('thread active'))

        while not wg.closing_time:
            lmsg = snooze_and_wait(self.promotion_q)
            if wg.closing_time: break

            init_sender = lmsg.init_sender_addr
            relaying_node = lmsg.sender_addr
            recipient = lmsg.recipient_addr

            # the question: do I need to amplify this? 

            # is this an echo of something I have already promoted?
            if self.cargo.has_promoted(lmsg.skey,self.my_dev_id):
                continue

            # if not multicast, are sender and recipient already dependent?  
            # if so, the recipient probably got the message. discard.
            if not lmsg.multicast and self.are_dependent(init_sender,recipient): 
                self.in_the_weeds('%s tossing %s because sender/recipient dependent' \
                    % (self.my_dev_id,lmsg.skey))
                continue

            # is the relaying node and recipient (if not multicast) independent
            # of me? if so, discard. note that in saturation requests it is likely
            # the sender will be independent of this device (density argument),
            # so don't use it for deduction.
            if self.are_independent(self.my_dev_id,relaying_node) \
            and not lmsg.multicast \
            and self.are_independent(self.my_dev_id,recipient):
                self.in_the_weeds(' %s tossing %s because no dependence on me' \
                    % (self.my_dev_id,lmsg.skey))
                continue

            # has the (non-multicast) recipient already responded? discard.
            if not lmsg.multicast and lmsg.response_requested \
                and self.cargo.has_responded(lmsg.skey,recipient):
                self.in_the_weeds(' %s tossing %s because recipient already responded' \
                    % (self.my_dev_id,lmsg.skey))
                continue

            # have all the nodes that depend on me already broadcasted? discard. 
            # note that if this node has no dependents, err on the side of caution
            # (noise) and assume it's a node new to the swarm and hasn't made
            # friends--err, found dependents--just yet.
            dependents = self.cargo.who_needs_me
            promoters = self.cargo.who_has_promoted(lmsg.skey)
            if len(dependents) > 0 and dependents - promoters == set():
                self.in_the_weeds(' %s tossing %s because dependents broadcasted' \
                    % (self.my_dev_id,lmsg.key))
                continue

            # no reason not to broadcast!
            if(lmsg.generate_amplification(self.my_dev_id)):
                log.data_flow(self.add_name('amplifying %s' % (lmsg.skey)))
                self.transmit(lmsg.amplif_lmsg)
            else: 
                log.error('amplification of %s failed! putting back in queue' % (lmsg.skey))    
                self.promotion_q.put(lmsg)
        
    # handles requests for GPS location, tracks the location of other devices
    def request_handler(self): #TODO fix payload
        log.thread_status_updates(self.add_name('thread active'))

        while not wg.closing_time:
            lmsg = snooze_and_wait(self.request_q)
            if wg.closing_time: break
            lmsg.hit_request = True

            # we've got mail!
            log.data_flow(self.add_name('%s received request %s' % (self.my_name,lmsg.skey)))

            # get position
            if lmsg.msg_type == MSG_TYPE_POS_REQUEST:
                if not SIM_MODE:
                    curr_lat, curr_long = retrieve_gps()
                else:
                    curr_lat, curr_long = self.curr_lat, self.curr_long

                lmsg.response_payload = '%f,%f' % (curr_lat,curr_long)
                if lmsg.generate_response(self.my_dev_id): 
                    log.data_flow(self.add_name('responds with %s' % (lmsg.show_response())))
                    self.transmit(lmsg.response_lmsg)
                else:
                    log.packet_errors(self.add_name('failed to respond to request %s' % (lmsg.skey)))  

            # UNIS GET request or generic QUERY--return requested information
            elif lmsg.msg_type in [MSG_TYPE_UNIS_GET_REQUEST,MSG_TYPE_QUERY]: 
                # format: relevant device address, variable to update, updated value
                # where the addressee is the MAC address of a specific device, or 
                # '*' to match any device, so long as the message is multicast
                M = lmsg.payload.split(',')
                if len(M) != 2:
                    continue    

                # ruling out misconfigured messages
                dev_id = M[0]
                if not (is_plausible_dev_id(dev_id) or dev_id == ANY_DEVICE and lmsg.multicast):
                    continue

                var_name = M[1] 
                reply_data_not_found = True

                # data requested from the UNIS instance, specifically
                if wg.HAVE_UNIS and lmsg.msg_type == MSG_TYPE_UNIS_GET_REQUEST:
                    if dev_id == ANY_DEVICE or dev_id == self.my_dev_id: # valid parameter here
                        node = self.my_node
                    else:
                        node = register_or_retrieve_node(dev_id)

                    if node_has_var(node,var_name):
                        S = 'node.%s' % (var_name)
                        val = eval(S)
                        reply_var_did_not_exist = False
                        
                # generic request, or UNIS GET request when not using UNIS. 
                # be kind and check the DataFrame, and hope for the best.
                elif lmsg.msg_type == MSG_TYPE_QUERY or \
                (lmsg.msg_type == MSG_TYPE_UNIS_GET_REQUEST and not wg.HAVE_UNIS):
                    if dev_id == ANY_DEVICE or dev_id == self.my_dev_id: 
                        dev_id == self.my_dev_id

                    obs_time, val = self.retrieve_var_from_df(dev_id,var_name)
                    if val == DATA_NOT_FOUND:
                        reply_data_not_found = False
 
                if reply_data_not_found:
                    lmsg.response_payload = '%s,%s,%s,%s' % (obs_time,dev_id,var_name,DATA_NOT_FOUND)
                else:
                    lmsg.response_payload = '%s,%s,%s,%s' % (obs_time,dev_id,var_name,str(val))
    
                if lmsg.generate_response(self.my_dev_id):
                    log.data_flow(self.add_name('responds with %s' % (lmsg.show_response())))
                    self.transmit(lmsg.response_lmsg)  
                else:
                    log.packet_errors(self.add_name('failed to respond to request %s' % (lmsg.skey)))    

            elif lmsg.msg_type == MSG_TYPE_UNIS_POST_ACK_REQ: 
                # the data should have already been harvested during ingestion
                # so nothing left to do but respond with an ACK
                lmsg.response_payload = lmsg.payload # echo the previous payload
                if lmsg.generate_response(self.my_dev_id):
                    #log.data_flow(self.add_name('responds with %s' % (lmsg.show_response())))
                    self.transmit(lmsg.response_lmsg)  
                else:
                    log.packet_errors(self.add_name('failed to respond to request %s' % (lmsg.skey)))        

    # dedicated to the Postal Sorting Alien from "Men in Black II"
    def postal_sorter(self):
        log.thread_status_updates(self.add_name('postal sorter active'))
        msgs_seen = set()

        while not wg.closing_time:
            lmsg = snooze_and_wait(self.inbox_q)
            if wg.closing_time: break

            print('got something!!!!!!!!!!!!!!!');

            if not lmsg.pkt_valid:
                log.data_flow('received mail that was invalid, %s' % (lmsg.initial_pkt))
                continue

            # we've got mail!
            #log.data_flow(self.add_name('received message %s' % (lmsg.skey)))

            # discard echoes
            msg_sig = (lmsg.sender_addr,lmsg.send_time)
            if msg_sig in msgs_seen:
                print('tossing echo',msg_sig)
                continue
            else: msgs_seen.add(msg_sig)
            
            # push data to UNIS
            self.my_msg_stream.append(lmsg.initial_pkt)
            
            if lmsg.saturation_req and lmsg.init_sender_addr == self.my_dev_id \
            or lmsg.sender_addr == self.my_dev_id or self.cargo.seen_msg(lmsg): 
                continue

            # always
            #self.in_the_weeds('shared message with gleaner')
            self.glean_q.put(lmsg)

            print('put msg in glean q')

            my_obs_gps_lat, my_obs_gps_long = retrieve_gps()

            print('DATA,%f,%f,%f,%f\n' \
                % (lmsg.obs_time,my_obs_gps_lat,my_obs_gps_long,lmsg.RSSI_val))

            # a saturation request to the promoter, unless this is a message
            # sent directly to this device, or if this message was the original
            # sender. then handle requests or harvest in the next two code blocks,
            # respectively.
            if lmsg.saturation_req \
            and (lmsg.recipient_addr != self.my_dev_id or lmsg.multicast):
                #self.in_the_weeds('shared message with promoter')
                self.promotion_q.put(lmsg)
    
            # for responding to requests
            if msg_type_is_request(lmsg.msg_type) \
            and (lmsg.recipient_addr == self.my_dev_id or lmsg.multicast):
                #self.in_the_weeds('shared message with request handler')
                self.request_q.put(lmsg)

            # if we overhear a response to a request, should we proactively respond?
            # from a single message we have no way of inferring whether the request
            # was multicast or targeted, saturation or not--unless more than one
            # device responds.
            #TODO

    def radio_operator(self):
        log.thread_status_updates(self.add_name('thread active'))

        eps = 10e-3 # increase in size to avoid losing packets
        mulligan_bucket = []

        while not wg.closing_time:
            # synchronize with the lora_listener, and other devices
            
            while now() % 10 > eps and not wg.closing_time: 
                pass # status will be RESET initially

            start_time = floor(now())
            curr_time = start_time
            m = 0
            
            while not wg.closing_time and m < len(self.my_ordering):
                curr_phase = self.my_ordering[m]
                
                if m > 0: last_phase = self.my_ordering[m-1]
                else: last_phase = self.my_ordering[-1]
                
                if m < len(self.my_ordering) - 1: next_phase = self.my_ordering[m+1]
                else: next_phase = self.my_ordering[0]
            
                m += 1

                if last_phase != curr_phase: # spin until the reset is complete
                    self.my_status = RESETTING
                    spin_until(curr_time + RESET_DURATION, eps)
                    curr_time += RESET_DURATION
                    print(self.my_name,'HAS RESET')
                
                if curr_phase == RECEIVING:
                    self.my_status = RECEIVING
                    spin_until(curr_time + RECEIVING_DURATION, eps)
                    curr_time += RECEIVING_DURATION
                    print(self.my_name,'IS DONE RECEIVING')
                    continue
                
                if curr_phase == TRANSMITTING:
                    self.my_status = TRANSMITTING
                    print(self.my_name,'IS TRANSMITTING')
                    
                    ts = curr_time + TRANSMITTING_DURATION
                    
                    # first toss out the mulligans
                    if ts - now() > eps: 
                        funnel_list2q(mulligan_bucket,self.outbox_q)
                        print('funneled %d' % (len(mulligan_bucket)))
                        
                    # transmit new stuff
                    while ts - now() > eps:
                        just_a_bucket = []
                        funnel_q2list(self.operator_q,just_a_bucket)
                        funnel_list2q(just_a_bucket,self.outbox_q)
                        mulligan_bucket += just_a_bucket
                        
                    cull_mulligans(mulligan_bucket)
                    
                    curr_time += TRANSMITTING
                    print('post transmitting',curr_time,now())
                    continue

    # sends messages to whisper-c via socket, pulling from the outbox_q
    def c_speaker(self):
        log.thread_status_updates(self.add_name('thread active'))

        self.outgoing_port = find_free_port()

        self.out_s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.out_s.bind(('localhost', self.outgoing_port))
        SOCKET_BUCKET.append(self.out_s)

        log.plumbing_issues(self.add_name('listening on port %d...' % (self.outgoing_port)))
        self.out_s.listen(1)
        out_conn, out_addr = self.out_s.accept()

        log.plumbing_issues(self.add_name('connector address is %s' % (str(out_addr))))

        # if response not ready, put it back in the queue
        while not wg.closing_time:
            if wg.whisper_c_is_dead:
                log.plumbing_issues(self.add_name('listening on port %d...' % (self.outgoing_port)))
                self.out_s.listen(1)
                out_conn, out_addr = self.out_s.accept()

                log.plumbing_issues(self.add_name('connector address is %s' % (str(out_addr))))

            # yes, it would make sense to put the timed transmission logic here, but 
            # I hesitate to mess with reliable code that interfaces with whisper-c

            # pull something from the queue here
            lmsg = snooze_and_wait(self.outbox_q)
            if wg.closing_time: break
 
            #TODO repeatedly send add counter within this func only, decrement 
 
            # got something! check it.
            if not lmsg.pkt_valid:
                log.plumbing_issues(self.add_name('received invalid packet: %s' % (pkt)))
                continue

            # turn it back into a string for sending via socket
            pkt = lmsg.to_packet()

            try:
                ret_val = out_conn.send(pkt.encode())
            except: # if we can't send, we have problems
                ret_val = False
  
            # put the message back in queue via transmit
            if not ret_val: 
                #self.transmit(lmsg)  # unnecessary thanks to mulligans. left as a reminder.
                log.plumbing_issues(self.add_name('whisper-c is dead?'))
                wg.whisper_c_is_dead = True
                continue

            #log.plumbing_issues(self.add_name("sent packet: %s" % (pkt)))
            print('sent',pkt)

    def animator(self): # doesn't actually animate
        log.thread_status_updates(self.add_name('thread active'))
        self.fig,self.ax = plt.subplots()

        num_points = 0
        
        while not wg.closing_time:
            time.sleep(1)

            if len(self.temp_df) > 3 and len(self.temp_df) > num_points:
                num_points = len(self.temp_df)

                x = np.array(self.temp_df['obs_gps_long'])
                y = np.array(self.temp_df['obs_gps_lat'])
                z = np.array(self.temp_df['obs_val'])                

                print(self.temp_df)
        
                xmin = np.min(x)
                xmax = np.max(x)
                ymin = np.min(y)
                ymax = np.max(y)

                stepsize = min([abs(xmax-xmin),abs(ymax-ymin)])/100

                try:
                    xi = np.arange(xmin,xmax+stepsize,stepsize)
                    yi = np.arange(ymin,ymax+stepsize,stepsize)
                    xi, yi = np.meshgrid(xi,yi)
                    zi = griddata((x,y),z,(xi,yi),method='cubic')
                except:
                    time.sleep(3)
                    continue
                                
                #self.ax.clear()
                #self.fig.clf()
                self.ax.contourf(xi,yi,zi)
                plt.xlabel('longitude',fontsize=14)
                plt.ylabel('latitude',fontsize=14)
                plt.title('Composite contour map of locally interpolated, simulated temperature values')

                plt.pause(0.06)

    def emcee(self): # vessel name is the hostname
        while not wg.closing_time:
            start_time = now()
            self.temp_df = self.temp_dataset()
            end_time = now()  
            log.data_flow(self.add_name('created estimated dataset in %fs with %s records' \
                % (end_time - start_time, len(self.temp_df))))          
            #print(self.my_name,'@',retrieve_gps())
            
            # randomly choose the next broadcast action
            #fl = [self.flood_var_broadcast,self.flood_pos_update,self.var_broadcast,self.flood_pos_update]
            #f = random.choice(fl)   
            #lmsg = f('*')
             
            # or just blast away
            #self.flood_temp_var_broadcast('*')
            self.flood_pos_update('*')
                
            #log.data_flow(self.add_name('put dummy request in queue'))  
            #print(self.my_name,'@',retrieve_gps())

            #st = random.randint(1,3) 
            st=1             
            time.sleep(st)

############################################################################################
#### IN PROGRESS ###########################################################################  
############################################################################################

    # handles requests as delegated by the receptionist. 
    # if this becomes too computationally intensive, launch a separate process that
    # gets a streaming feed of incoming data
    def intern(self,recipient_addr,saturation_req,msg_type,payload,timeout):
        start_time = now()

        packet_no_bloom =          '%s/0/%s/%f/%d/%s//:'
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
    
        # TODO cleanup

    # TODO accepts requests via TCP connection then summons an intern to handle it
    def receptionist(self,rt):
        pass

############################################################################################
#### STABLE CODE ###########################################################################  
############################################################################################

    def add_name(self,msg):
        return ('*%s* ' % (self.my_name)) + msg

    # used in simulation
    def __eq__(self,other):
        return self.my_name == other.name

    def bodiless_setup(self,curr_lat,curr_long):
        self.curr_lat = curr_lat
        self.curr_long = curr_long

    def bite_off(self,s0):
        if '/' not in s0: return s0
        S = s0.split('/')
        S = S[1:] # toss the first bit
        return reduce(lambda x,y: x+'/'+y,S) # returns a string

    # used by c_listener to chop up packets that got stuck together when transmitted
    # over socket
    def chop_packet_stream(self,pkt):
        P = pkt # copy
        chopped = []

        while len(P) > 10: # absurdly low lower bound on valid packet string length
        
            # base case: is this a single, valid packet?
            lmsg = lora_message(P)
            if lmsg.pkt_valid: #done
                chopped.append(lmsg) 
                break # so stop here
      
            # was something else wrong in the packet? 
            if is_plausible_RSSI_value(lmsg.RSSI_val):
                break # at the end of the stream, stop here

            # presumably we have some slicing to do

            # is the next message multicast?
            if type(lmsg.RSSI_val) == str:
                if lmsg.RSSI_val[-1] == MULTICAST:
                    p0 = P.split(lmsg.RSSI_val)[0] + lmsg.RSSI_val[:-1]
                else:
                    recip_addr = lmsg.RSSI_val[-12:]
                    p0 = P.split(lmsg.RSSI_val)[0] + lmsg.RSSI_val.replace(recip_addr,'')

                lmsg = lora_message(p0)
                if lmsg.pkt_valid:
                    chopped.append(lmsg)
                    # no break!
                else:
                    print('\n***************** crap!!! ********************\n')
                    
                    print('CRAP?',pkt)

                # finally, chop off the first packet
                P = P.replace(p0,'')

        return chopped

    # listens for messages from whisper-c via socket, pushes received messages
    # to the inbox_q
    def c_listener(self):
        log.thread_status_updates(self.add_name('thread active'))

        self.incoming_port = find_free_port()

        self.inc_s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.inc_s.bind(('localhost', self.incoming_port))
        SOCKET_BUCKET.append(self.inc_s)

        log.plumbing_issues(self.add_name('listening on port %d...' % (self.incoming_port)))
        self.inc_s.listen(1)
        inc_conn, inc_addr = self.inc_s.accept()

        log.plumbing_issues(self.add_name('connector address is %s' % (str(inc_addr))))

        while not wg.closing_time:
            if wg.whisper_c_is_dead:
                log.plumbing_issues(self.add_name('listening on port %d...' % (self.incoming_port)))
                self.inc_s.listen(1)
                inc_conn, inc_addr = self.inc_s.accept()

                log.plumbing_issues(self.add_name('connector address is %s' % (str(inc_addr))))

            try:
                stream = inc_conn.recv(BUFFER_SIZE)
            except:
                log.plumbing_issues(self.add_name('whisper-c is dead?'))
                wg.whisper_c_is_dead = True
                continue

            if not stream:
                time.sleep(MICROSNOOZE_TIME)
                continue

            # hack off a piece of the stream, then put it in pkt
            try: # this can blow up
                pkt = stream.decode()
            except:
                continue

            print('$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$')

            log.plumbing_issues(self.add_name('received packet: %s' % (pkt)))

            print(pkt)

            msgs = self.chop_packet_stream(pkt)


            print('got',len(msgs),'messages in this batch\n\n')

            for msg in msgs:
                if msg.pkt_valid:
                    self.inbox_q.put(msg)
                    print(msg.to_packet())
                else:
                    log.plumbing_issues(self.add_name('PACKET INVALID'))

            time.sleep(SNOOZE_TIME)

        log.plumbing_issues(self.add_name('closing time! shutting down'))    

    def prod_flood(self,recipient,msg_type,payload):
        send_time = now()
        pkt = '%s/1,%s,%f/%s/%f/%d/%s//-42|' % (recipient,self.my_dev_id,send_time,
            self.my_dev_id,send_time,msg_type,payload)
        lmsg = lora_message(pkt)

        if not lmsg.pkt_valid:
            log.packet_errors(self.add_name('warns the saturation request is invalid!'))
            return 

        log.data_flow(self.add_name('put the saturation request in queue'))
        self.transmit(lmsg)
        return lmsg

    def prod_spurt(self,recipient,msg_type,payload):
        send_time = now()
        pkt = '%s/0/%s/%f/%d/%s//-42|' % (recipient,self.my_dev_id,send_time,
            msg_type,payload)
        lmsg = lora_message(pkt)

        if not lmsg.pkt_valid:
            log.packet_errors(self.add_name('says the non-saturation packet is invalid!'))
            return 

        log.data_flow(self.add_name('put the non-saturation packet in queue'))
        self.transmit(lmsg)
        return lmsg

    def pos_request(self,recipient):
        self.prod_spurt(recipient,MSG_TYPE_POS_REQUEST,'')
    
    def flood_pos_request(self,recipient):
        self.prod_flood(recipient,MSG_TYPE_POS_REQUEST,'')

    def flood_pos_update(self,recipient):
        if not SIM_MODE:
            curr_lat, curr_long = retrieve_gps()
        else:
            curr_lat, curr_long = self.curr_lat, self.curr_long

        payload = '%s,%s' % (curr_lat,curr_long)
        self.prod_flood(recipient,MSG_TYPE_POS_UPDATE,payload)

    def pos_update(self,recipient):
        if not SIM_MODE:
            curr_lat, curr_long = retrieve_gps()
        else:
            curr_lat, curr_long = self.curr_lat, self.curr_long

        payload = '%s,%s' % (curr_lat,curr_long)
        self.prod_spurt(recipient,MSG_TYPE_POS_UPDATE,payload)

    def var_broadcast(self,recipient):#TODO 
        pass

    def flood_var_broadcast(self,recipient): #TODO generalize
        pass

    def prepare_temp_var_payload(self):
        obs_time = now()
        obs_gps = retrieve_gps()
        obs_gps_lat = obs_gps[0]
        obs_gps_long = obs_gps[1]
        obs_dev_id = self.my_dev_id
        obs_var_name = 'temp'
        obs_val = get_onboard_temp()
        payload = '%f,%f,%f,%s,%s,%f' % (obs_time, obs_gps_lat, obs_gps_long, obs_dev_id, obs_var_name, obs_val)
        return payload

    def temp_var_broadcast(self,recipient): #TODO fix payload
        payload = self.prepare_temp_var_payload()
        self.prod_spurt(recipient,MSG_TYPE_NOTIF,payload)
    
    def flood_temp_var_broadcast(self,recipient):#TODO fix payload
        payload = self.prepare_temp_var_payload()
        self.prod_flood(recipient,MSG_TYPE_NOTIF,payload)

    # Process-reading code is from Eli Bendersky's website post,
    # "Interacting with a long-running child process in Python"
    # available at 
    # <https://eli.thegreenplace.net/2017/interacting-with-a-long-running-child-process-in-python/>
    # last accessed: August 22, 2019
    def attempt_read(): 
        return wg.whisper_c_p.stdout.readline().decode('utf-8')

    # a handler for whisper-c when stable enough to run unsupervised
    def c_handler(self):
        start_whisper_c(self.incoming_port,self.outgoing_port)
        last_observed = now()
    
        while not wg.closing_time: 
            if wg.whisper_c_is_dead:
                dispose_of_whisper_c() # whisper-c is dead
                start_whisper_c(self.incoming_port,self.outgoing_port) # long live whisper-c
                wg.whisper_c_is_dead = False
                print('long live whisper-c')

            # I could have implemented a keep-alive message via socket but 
            # figured I might at some point need to observe the process more closely
            while not wg.whisper_c_is_dead:
                line = self.attempt_read()
                print(line) #TODO remove once stable
                if line != '':
                    last_observed = now()
                if now() - last_observed > PRESUMED_DEAD:
                    wg.whisper_c_is_dead = True
                    break
                time.sleep(1)
                print('checking for pulse again') #TODO remove as well

    def summon_receptionist(self):
        self.receptionist_t = threading.Thread(target=self.receptionist, args = [rt])
        self.receptionist_t.daemon = True # so it does with the host process
        THREAD_BUCKET.append(self.receptionist_t) # for easier cleanup
        self.receptionist_t.start()

    def summon_emcee(self):
        self.emcee_t = threading.Thread(target=self.emcee, args = [])
        self.emcee_t.daemon = True # so it does with the host process
        THREAD_BUCKET.append(self.emcee_t) # for easier cleanup
        self.emcee_t.start()

    def summon_animator(self):
        self.animator_t = threading.Thread(target=self.animator, args = [])
        self.animator_t.daemon = True # so it does with the host process
        THREAD_BUCKET.append(self.animator_t) # for easier cleanup
        self.animator_t.start()

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

    # summon a thread to handle the whisper-c instance so long as it's
    # sufficiently stable to run unsupervised
    def summon_handler(self):
        while self.incoming_port == 0 or self.outgoing_port == 0:
            log.plumbing_issues(self.add_name('waiting for ports to be acquired'))
            time.sleep(SNOOZE_TIME)   # reversed for whisper mirror in C

        self.c_handler_t = threading.Thread(target=self.c_handler, args=[])
        self.c_handler_t.daemon = True # so it does with the host process
        THREAD_BUCKET.append(self.c_handler_t) # for easier cleanup
        self.c_handler_t.start()

    def summon_keepers(self):
        self.glean_t = threading.Thread(target=self.gleaner,args=[])#rt])
        self.glean_t.daemon = True # so it does with the host process
        THREAD_BUCKET.append(self.glean_t) # for easier cleanup
        self.glean_t.start()
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        AAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
        self.request_t = threading.Thread(target=self.request_handler)
        self.request_t.daemon = True # so it does with the host process
        THREAD_BUCKET.append(self.request_t) # for easier cleanup
        self.request_t.start()

        self.promoter_t = threading.Thread(target=self.promoter)
        self.promoter_t.daemon = True # so it does with the host process
        THREAD_BUCKET.append(self.promoter_t) # for easier cleanup
        self.promoter_t.start()

        self.post_t = threading.Thread(target=self.postal_sorter)
        self.post_t.daemon = True # so it does with the host process
        THREAD_BUCKET.append(self.post_t) # for easier cleanup
        self.post_t.start()

        self.operator_t = threading.Thread(target=self.radio_operator)
        self.operator_t.daemon = True # so it does with the host process
        THREAD_BUCKET.append(self.operator_t) # for easier cleanup
        self.operator_t.start()

    def begin(self):
        # if not running bodiless, create limbs
        if not SIM_MODE:
            self.summon_comms_threads()
            time.sleep(3) # wait for the threads to start up

        # before summoning the rest
        self.summon_keepers()

        if not SIM_MODE:
            # summon the c-handler or print out the ports to start the process manually
            if wg.USING_WHISPER_C_HANDLER:
                self.summon_handler()
            else: # notify the user they need to run whisper-c manually
                log.warning( \
                '''

                The c-handler has not been selected for use. Please manually execute
                the whisper module with ports being used, i.e. run the following:

                ./whisper_c -i %d -o %d

                ''' % (self.incoming_port,self.outgoing_port))

        if wg.DEMOING:
            self.temp_df = []
            self.summon_emcee()   

        if wg.DEMOING and wg.ANIMATING:
            self.summon_animator()

