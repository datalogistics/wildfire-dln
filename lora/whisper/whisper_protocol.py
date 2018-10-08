from whisper_settings import *

'''
The class lora_message, defined in the file whisper_protocol.py (which imports this file), 
encapsulates the protocol and switch behavior of whisper-py. Relevant messages
(~packets) are received from whisper-c, fed to the inbox_q, then consumed.

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

completing the packet.

=> complete packet contains eight /-separated fields followed by a |

Note: the data a packet contains can be uniquely identified with a device address
and timestamp so long as devices have a single transceiver, or multiple transceivers
that transmit sequentially. 
'''

# the data store is transient memory; adjust further
class message_store:
    def __init__(self):
        self.inventory = {} # set doesn't work well for iteration
        self.thresholds = {}
        self.stock = {}

    def add(self,lmsg):
        if lmsg.key not in self.thresholds.keys():
            self.stock[lmsg.key] = 0
        
        # update or add
        self.stock[lmsg.key] = self.stock[lmsg.key] + 1
        self.inventory[lmsg.key] = lmsg
        lmsg.time_to_die = lmsg.time_to_die + LIFETIME_EXTENSION
                
        # shuffling along the PDF of the distribution
        self.thresholds[lmsg.key] = -np.exp(self.stock[lmsg.key])        

# queues for storing messages for consumption by various processes
inbox_q = queue.Queue()
outbox_q = queue.Queue()

# a message_store instance for managing transient memory 
msg_store = message_store()
 
# an observation or data point (payload) plus metadata
class lora_message:
    def __init__(self,pkt):
        self.pkt_valid = False
        M = pkt.strip(MESSAGE_TERMINATOR).split(MESSAGE_DELIMITER)

        if len(M) != NUM_PROTOCOL_FIELDS:
            return

        # got tired of manually changing the indices when changing the protocol
        i = 0 

        if not is_plausible_MAC_addr(M[i]) and M[i] != MULTICAST: return
        self.recipient_addr = normalize_addr(M[i])
        self.multicast = (recipient_addr == MULTICAST)
        i = i + 1

        if not is_plausible_bloom(M[i]): return
        self.bloom_count = int(M[i].split(',')[0])
        self.saturation_req = (self.bloom_count != NO_SATURATION)

        if self.saturation_req:
            self.init_sender_addr = M[i].split(',')[1]
            self.init_msg_time = M[i].split(',')[2]
        i = i + 1

        if not is_plausible_MAC_addr(M[i]): return
        self.sender_addr = normalize_addr(M[i])
        i = i + 1

        if not is_plausible_timestamp(M[i]): return
        self.msg_time = float(M[i])
        self.receipt_time = now()
        i = i + 1

        if not msg_type_is_defined(M[i]): return 
        c = int(M[i])
        self.response_requested = msg_type_is_request(self.msg_type)
        i = i + 1

        self.payload = M[i]
        i = i + 1

        # reserved for future use
        i = i + 1

        if not is_plausible_RSSI_value(M[i]): return
        self.RSSI_val = float(M[i])
        i = i + 1

        if self.saturation_req:
            # to avoid noisy chaos, only track the original message
            self.key = (self.init_sender_addr,self.init_msg_time)
        else:
            self.key = (self.sender_addr,self.msg_time)

        self.time_to_die = self.receipt_time + DEFAULT_LIFESPAN
        self.retransmit_prob = 0. 
        self.observation_count = 0

        self.pkt_valid = True       

    def __str__(self):
        packet_no_bloom =          '%s/0/%s/%f/%d/%s//%f|'
        packet_with_bloom = '%s/%d,%s,%f/%s/%f/%d/%s//%f|'

        if self.saturation_req:
            S = packet_with_bloom % (self.recipient_addr,
                self.bloom_count,self.init_sender_addr,self.init_msg_time,
                self.sender_addr,
                self.msg_time,
                self.msg_time,
                self.payload,
                self.RSSI_val)
                
        else:
            S = packet_no_bloom % (self.recipient_addr,
                self.bloom_count,
                self.sender_addr,
                self.msg_time,
                self.msg_time,
                self.payload,
                self.RSSI_val)
                
    def create_response(self,F):
        # also functions as a flag as to whether the message is ready to send
        self.response = functools.reduce(lambda x,y: str(x)+MESSAGE_DELIMITER+str(y),F) + TRANSIENT_TERMINATOR

    # could do this from inidividual threads to avoid redundant checks but for the sake
    # of debugging will include the behaviors here, in one place
    def send_response(self):
        # filling in of the appropriate payload is done by the postal_sorter
        try:
            self.response_payload
        except:
            return False

        # implementation of a truth table

        def bloom():
            return '%d,%s,%f' % (self.bloom_count + 1, self.init_sender_addr,self.init_msg_time)

        # defaults, changed as necessary
        TIME_FIELD = now()
        PAYLOAD = self.response_payload 
        SENDER_FIELD = MY_MAC_ADDR
        RECIP_FIELD = self.sender_addr

        if self.saturation_req:
            SAT_FIELD = bloom()
        else:
            SAT_FIELD = NO_SATURATION

        REQ_TYPE_FIELD = self.msg_type
        RESERVED_FIELD = RESERVED_DEFAULT

        # note: RSSI value and terminator will be added upon receipt

        # reduce the redundancy when this is settled code
        if self.multicast: # (T,*,*)
            if self.saturation_req: # (T,T,*)
                # first, prepare the broadcasted duplicate
                PAYLOAD = self.payload

                # avoid redundant saturation
                SENDER_FIELD = self.init_sender_addr
                TIME_FIELD = self.init_msg_time

                F = [RECIP_FIELD,SAT_FIELD,SENDER_FIELD,TIME_FIELD,REQ_TYPE_FIELD,PAYLOAD,RESERVED_FIELD]
                self.create_response(F)

                flyer = copy.deepcopy(self) 
                msg_store.add(flyer)

                # remove the indicator of completion, just in case
                del self.response

                if self.response_requested: # (T,T,T)
                    REQ_TYPE_FIELD = noack(self.msg_type)

                TIME_FIELD = now()
                SENDER_FIELD = MY_MAC_ADDR
                PAYLOAD = self.response_payload

            else: # (T,F,*)
                if self.response_requested: # (T,F,T)
                    REQ_TYPE_FIELD = noack(self.msg_type)
                    outbox_q.put(self)

                else: # (T,F,F)
                    return True # just a notification; no response required
        
        else: # (F,*,*)
            if self.saturation_req: # (F,T,*)
                if self.response_requested: # (F,T,T)
                    # first case: non-recipient is to relay the message onward
                    if self.recipient_addr != MY_MAC_ADDR:
                        RECIP_FIELD = self.recipient_addr
                        PAYLOAD = self.payload

                        # avoid redundant saturation
                        SENDER_FIELD = self.init_sender_addr
                        TIME_FIELD = self.init_msg_time

                        msg_store.add(self)

                    # second case: recipient responds
                    else:
                        RECIP_FIELD = self.init_sender_addr
                        REQ_TYPE_FIELD = noack(self.msg_type)
                        outbox_q.put(self)

                else: # (F,T,F)
                    # first case: non-recipient is to relay the message onward
                    if self.recipient_addr != MY_MAC_ADDR:
                        RECIP_FIELD = self.recipient_addr
                        PAYLOAD = self.payload

                        # avoid redundant saturation
                        SENDER_FIELD = self.init_sender_addr
                        TIME_FIELD = self.init_msg_time

                        msg_store.add(self)

                    # second case: recipient responds
                    else:
                        RECIP_FIELD = self.init_sender_addr
                        outbox_q.put(self)

            # only if this device was the intended recipient will these cases occur
            else: # (F,F,*)
                if self.response_requested: # (F,F,T)
                    REQ_TYPE_FIELD = noack(self.msg_type)
                    outbox_q.put(self)

                else: # (F,F,F)
                    return True # just a notification; no response required

        F = [RECIP_FIELD,SAT_FIELD,SENDER_FIELD,TIME_FIELD,REQ_TYPE_FIELD,PAYLOAD,RESERVED_FIELD]
        self.create_response(F)

        return True        

    # faster
    def __ne__(self,other):
        if self.msg_time != other.msg_time:
            return True
        
        if self.key != other.key:
            return True

        return False

    def __eq__(self,other):
        return not self.__neq__(other)
