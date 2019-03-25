'''**************************************************************************

File: whisper_settings.py
Language: Python 3.6/7
Author: Juliette Zerick (jzerick@iu.edu)
        for the WildfireDLN Project
        OPEN Networks Lab at Indiana University-Bloomington

In this file the class lora_message encapsulates the protocol used in the
whisper tool. For the moment it is defined and processed as ASCII strings.
The class has methods to verify the validity of strings as legitimate
packets handled by the protocol. The fields are defined in a long
comment a few lines farther down.

Last modified: March 18, 2019

****************************************************************************'''

import copy # for copy.deepcopy()

from whisper_settings import *
import whisper_globals as wg

'''
The class lora_message encapsulates the protocol and switch behavior of whisper-py.
Relevant messages (~packets) are received from whisper-c, fed to the inbox_q, then 
consumed.

The protocol uses ASCII messages (for now):

1) * for multicast or MAC address of recipient/

2) N>0 for saturation request (N=bloom count) or 0 for simple broadcast,
and if N>0 follows are: MAC address of the initial sender, epoch timestamp of the initial missive/

3) MAC address of last sender (which corresponds to RSSI value) /

4) epoch timestamp, combined with the sender address forms a message identifier/

5) request type/

6) payload/

7) reserved for future use:

8) When the packet is received by the handler, the : is removed and replaced with /
followed by: RSSI value|

thus completing the packet.

=> complete packet contains eight /-separated fields followed by a |

Note: the data a packet contains can be uniquely identified with a device address
and timestamp so long as devices have a single transceiver, or multiple transceivers
that transmit sequentially. 
'''
        
TABLE_NONCASE = ''

class table_entry:
    def __init__(self,original_sender,bloom_count,relaying_node,last_observed):
        self.original_sender = original_sender
        self.bloom_count = bloom_count
        self.relaying_node = relaying_node # TODO remove?
        self.last_observed = last_observed

class routing_table:
    def __init__(self):
        self.table = {}
        
    def update_table(self,lmsg):
        original_sender = lmsg.sender_addr
        last_observed = lmsg.send_time
        relaying_node = lmsg.sender_addr
        bloom_count = 1
        self.table[original_sender] = \
            table_entry(original_sender,bloom_count,relaying_node,last_observed)

        if lmsg.bloom_count > 1: 
            original_sender = lmsg.init_sender_addr
            relaying_node = lmsg.sender_addr
            last_observed = lmsg.init_send_time
            bloom_count = lmsg.bloom_count
            self.table[original_sender] = \
                table_entry(original_sender,bloom_count,relaying_node,last_observed)        

# the data store is transient memory; adjust further
class message_store:
    def __init__(self):
        self.inventory = {} # set doesn't work well for iteration
        self.thresholds = {}
        self.stock = {}
        
        log.info('msg_store is active')

    def update_inventory(self,lmsg):
        self.inventory[lmsg.key] = self.inventory[lmsg.key] + 1
        lmsg.time_to_die = lmsg.time_to_die + LIFETIME_EXTENSION
                
        # shuffling along the PDF of the distribution
        self.thresholds[lmsg.key] = np.exp(-self.inventory[lmsg.key]) * 5
        
        if self.inventory[lmsg.key] > 5:
            self.thresholds[lmsg.key] = 0
        else:
            self.thresholds[lmsg.key] = 0.5
        
        self.thresholds[lmsg.key] = 0
        
        log.msg_store_updates('thresholds adjusted for %s with %d observations' \
            % (self.stock[lmsg.key].skey,self.inventory[lmsg.key]))

    def get_snapshot(self):
        inv = copy.deepcopy(self.inventory)
        thr = copy.deepcopy(self.thresholds)
        
        return inv,thr

    def incorporate(self,lmsg):
        if lmsg.key not in self.inventory:
            self.inventory[lmsg.key] = 0
            log.msg_store_updates('incorporated %s' % (lmsg.skey))
            # always store the most recent copy of the message in the stock

        # update or add
        self.stock[lmsg.key] = lmsg 
        self.update_inventory(lmsg)

        lmsg.hit_msg_store = True
          
    def reset(self):
        del self.inventory, self.thresholds, self.stock
        
        self.inventory = {} 
        self.thresholds = {}
        self.stock = {}
        
        log.msg_store_updates('message store reset')

# an observation or data point (payload) plus metadata
class lora_message:
    def __init__(self,pkt):
        self.pkt_valid = False
        self.initial_pkt = pkt
        M = pkt.strip(MESSAGE_TERMINATOR).split(MESSAGE_DELIMITER)

        if len(M) != NUM_PROTOCOL_FIELDS:
            log.packet_errors('packet has incorrect number of fields')
            return
        
        # for debugging
        self.hit_sorter = False
        self.hit_carto = False
        self.hit_altar = False
        self.hit_outbox = False
        self.hit_msg_store = False

        self.table_case = TABLE_NONCASE

        # got tired of manually changing the indices when changing the protocol
        i = 0 

        if not is_plausible_MAC_addr(M[i]) and M[i] != MULTICAST: 
            log.packet_errors('packet has nonsensical recipient address')
            return
        self.recipient_addr = normalize_addr(M[i])
        self.multicast = (self.recipient_addr == MULTICAST)
        i = i + 1

        if not is_plausible_bloom(M[i]): 
            log.packet_errors('packet has implausible bloom values')
            return
        self.bloom_count = int(M[i].split(',')[0]) # conversion and split tested
        self.saturation_req = (self.bloom_count != NO_SATURATION)

        if self.saturation_req:
            self.init_sender_addr = normalize_addr(M[i].split(',')[1])
            self.init_send_time = float(M[i].split(',')[2])
        i = i + 1

        if not is_plausible_MAC_addr(M[i]): 
            log.packet_errors('packet has implausible sender address')
            return
        self.sender_addr = normalize_addr(M[i])
        i = i + 1

        if not is_plausible_timestamp(M[i]): 
            log.packet_errors('packet has implausible send timestamp')
            return
        self.send_time = float(M[i])
        self.receipt_time = now()
        i = i + 1
        
        if not is_plausible_msg_type(M[i]): 
            log.packet_errors('packet\'s request type is not translatable')
            return
        self.msg_type = int(M[i]) # conversion and validity tested  
        self.response_requested = msg_type_is_request(self.msg_type)
        i = i + 1

        self.payload = M[i]
        i = i + 1

        # reserved for future use
        i = i + 1

        if not is_plausible_RSSI_value(M[i]): 
            log.packet_errors('packet RSSI value is implausible')
            return
        self.RSSI_val = float(M[i])
        i = i + 1

        if self.saturation_req:
            # to avoid noisy chaos, only track the original message
            self.key = (self.init_sender_addr,self.init_send_time)
            
            if SIM_MODE: 
                self.sim_key = (self.sender_addr,self.init_sender_addr,self.init_send_time)
        else:
            self.key = (self.sender_addr,self.send_time)
            
            if SIM_MODE:
                self.sim_key = (self.sender_addr,self.send_time)

        # for ease of logging
        #self.skey = str(self.key) # inelegant
        self.skey = '(%s,%f)' % (self.key[0],self.key[1])

        self.time_to_die = self.receipt_time + DEFAULT_LIFESPAN
        self.retransmit_prob = 0. 
        self.observation_count = 0

        self.pkt_valid = True       

    def show_incoming(self):
        S = ''
        if self.saturation_req:
            S += self.init_sender_addr + ('[f]' % (self.init_send_time)) + '->'
            #S += self.init_sender_addr + ' -> ' 
        S += self.sender_addr + ('[f]' % (self.send_time)) + '->' + recipient_addr + ': ' + self.payload
        #S += self.sender_addr + ' -> ' + recipient_addr + ': ' + self.payload        
        return S

    def form_pkt(self):
        packet_no_bloom =           '%s/0/%s/%f/%d/%s//:'
        packet_with_bloom = '%s/%d,%s,%f/%s/%f/%d/%s//:'

        if self.saturation_req:
            S = packet_with_bloom % (self.recipient_addr,
                self.bloom_count,self.init_sender_addr,self.init_send_time,
                self.sender_addr,
                self.send_time,
                self.msg_type,
                self.payload)
        else:
            S = packet_no_bloom % (self.recipient_addr,
                self.sender_addr,
                self.send_time,
                self.msg_type,
                self.payload)

        return S

    def __str__(self):
        packet_no_bloom =           '%s/0/%s/%f/%d/%s//%f|'
        packet_with_bloom = '%s/%d,%s,%f/%s/%f/%d/%s//%f|'

        if self.saturation_req:
            S = packet_with_bloom % (self.recipient_addr,
                self.bloom_count,self.init_sender_addr,self.init_send_time,
                self.sender_addr,
                self.send_time,
                self.msg_type,
                self.payload,
                self.RSSI_val)
        else:
            S = packet_no_bloom % (self.recipient_addr,
                self.sender_addr,
                self.send_time,
                self.msg_type,
                self.payload,
                self.RSSI_val)

        return S

    def gen_response_pkt(self,F):
        # also functions as a flag as to whether the message is ready to send
        self.response = reduce(lambda x,y: str(x)+MESSAGE_DELIMITER+str(y),F) \
            + MESSAGE_DELIMITER + TRANSIENT_TERMINATOR

        # best available estimate of an RSSI value
        self.response_pkt = self.response[:-1] + str(self.RSSI_val) + MESSAGE_TERMINATOR
        self.response_lmsg = lora_message(self.response_pkt)
        
        if not self.response_lmsg.pkt_valid:
            log.packet_errors('generated response packet is invalid')
            exit()

    # only handles replies to requests
    def send_response(self,my_mac_addr,outbox_q,my_msg_store):
        if not msg_type_is_request(self.msg_type):
            return
    
        # filling in of the appropriate payload is done elsewhere
        for i in range(3):
            try:
                self.response_payload
            except: #TODO
                log.debug('message waiting to send')
                time.sleep(SNOOZE_TIME)
                continue

        if i == 3: # TODO add error?
            return 

        if self.multicast:
            RECIP_FIELD = self.sender_addr

        TIME_FIELD = now()

        if self.saturation_req:
            RECIP_FIELD = self.init_sender_addr
            SAT_FIELD = '1,%s,%f' % (my_mac_addr,TIME_FIELD)
        else:
            RECIP_FIELD = self.sender_addr
            SAT_FIELD = NO_SATURATION
        
        # defaults, changed as necessary
        SENDER_FIELD = my_mac_addr

        # this function only handles requests
        MSG_TYPE_FIELD = get_response_msg_type(self.msg_type)
        
        PAYLOAD = self.response_payload 
        RESERVED_FIELD = RESERVED_DEFAULT

        # note: RSSI value and terminator will be added upon receipt

        F = [RECIP_FIELD,SAT_FIELD,SENDER_FIELD,TIME_FIELD,MSG_TYPE_FIELD,PAYLOAD,RESERVED_FIELD]
        self.gen_response_pkt(F)

        # set these flags before copying
        self.hit_outbox = True
        
        if self.saturation_req:
            self.hit_msg_store = True
            my_msg_store.incorporate(self.response_lmsg)

        outbox_q.put(self.response_lmsg)

    def show_request(self):
        S = ':%s,\'%s\'' % (msg_code2msg_text_d[self.msg_type],self.payload)
        if self.saturation_req:
            S = (' <- %s[%f]' % (self.init_sender_addr,self.init_send_time)) + S

        S = (' <- %s[%f]' % (self.sender_addr,self.send_time)) + S
        S = (' = {%s}' % (self.recipient_addr)) + S
        return S

    def show_response(self):
        try:
            lmsg = self.response_lmsg
        except:
            return ''
            
        S = (' = {%s}' % (lmsg.sender_addr))
        S = S + (':%s,\'%s\'' % (msg_code2msg_text_d[lmsg.msg_type],lmsg.payload))
        S = S + (' -> %s[%f]' % (self.recipient_addr,self.send_time))
        return S       

    # faster
    def __ne__(self,other):
        if self.send_time != other.send_time:
            return True
        
        if self.key != other.key:
            return True

        return False

    def __eq__(self,other):
        return not self.__neq__(other)
