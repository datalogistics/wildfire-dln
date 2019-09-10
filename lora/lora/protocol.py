'''**************************************************************************

File: whisper_protocol.py
Language: Python 3.6.8
Author: Juliette Zerick (jzerick@iu.edu)
        for the WildfireDLN Project
        OPeN Networks Lab at Indiana University-Bloomington

In this file the class lora_message encapsulates the protocol used in the
whisper tool. For the moment it is defined and processed as ASCII strings.
The class has methods to verify the validity of7 strings as legitimate
packets handled by the protocol. The fields are defined in a long
comment a few lines farther down.

Last modified: August 12, 2019

****************************************************************************'''

import copy # for copy.deepcopy()
import pandas as pd

from whisper_settings import * # contains protocol utilities for tidiness
import whisper_globals as wg

'''
The class lora_message encapsulates the protocol and switch behavior of whisper-py.
Relevant messages (~packets) are received from whisper-c, fed to the inbox_q, then 
consumed.

The protocol uses ASCII-encoded messages (for now):

1) * for multicast or MAC address of recipient/

2) N>0 for saturation request (N=bloom count) or 0 for simple broadcast,
and if N>0 follows are

    a) N, MAC address of the initial sender, epoch timestamp of the initial missive/

3) MAC address of last sender (which corresponds to RSSI value) /

4) epoch timestamp, combined with the sender address forms a message identifier/

5) message type/
if the message type is a response to a previously received request, the original
request key will be referenced, i.e.
   
    a) message type,(original_sender,timestamp)/ 
    OR
    b) message type,original_sender,timestamp)/ 

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

# blank packets to be filled in with appropriate arguments
PACKET_NO_BLOOM =           '%s/0/%s/%f/%d%s/%s//:'
PACKET_WITH_BLOOM = '%s/%d,%s,%f/%s/%f/%d%s/%s//:'

'''
column names and dtypes of the initial DataFrame contained in each Vessel
there is redundant data, duplicated under different variable names, but this
is for the sake of readability, translating from hardware (e.g. addresses) and
data (e.g. device identifiers). the DataFrame contains the minimum information,
redundant or otherwise, for inference and model building.
'''
PROTOCOL_COLUMNS = {'receipt_ts':pd.datetime, 'receipt_time':float, 
'listener_dev_id':str, 'skey':str,
'recipient_addr':str, 
'bloom_count':int, 'init_sender_addr':str, 'init_send_time':float,
'sender_addr':str, 'send_time':float, 
'sender_dev_id':str, 'sender_last_obs':float, 'est_hops_to_sender':int,
'relayer_dev_id':str, 'relayer_last_obs':float,
'msg_type':int, 'msg_type_str':str,
'ref_dev_id':str, 'ref_time':float, 'ref_skey':str, 
'payload':str, 
'obs_time':float, 'obs_dev_id':str, 'obs_gps_lat':float, 'obs_gps_long':float,
'obs_var_name':str, 'obs_val':str,
'dependency_count':int,
'RSSI_val':float, 
'key_dev_id':str, 'key_time':float, 
'listener_prom_skey':str, 'listener_resp_skey':str}

'''
an observation or data point (payload) plus ample metadata, sometimes derived
or inferred. not all metadata is included in the generated DataFrame row
with columns listed above. some derivations are included only to show
methods of inference used.
'''
class lora_message:
    def __init__(self,pkt):
        # set some flags to initial values; if True, they will be reset
        self.pkt_valid = False
        self.has_ref = False
        self.is_response = False
        self.response_requested = False
        self.is_rtg = False
        self.is_panic = False
        self.is_harvestable = False
        
        # TODO encode in base 64, will need conversion functions bitstring->str
        
        self.initial_pkt = pkt
        M = pkt.strip(MESSAGE_TERMINATOR).split(MESSAGE_DELIMITER)

        # for testing in c_listener, need one initial setting
        self.RSSI_val = 'undetermined'

        # problematic--code is left as a reminder
        #if len(M) != NUM_PROTOCOL_FIELDS:
        #    log.packet_errors('packet has incorrect number of fields')
        #    return

        # got tired of manually changing the indices when changing the protocol
        i = 0 # so just increment this index

        if not is_plausible_MAC_addr(M[i]) and M[i] != MULTICAST: 
            log.packet_errors('packet has nonsensical recipient address, %s' % (M[i]))
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
        
        # now infer
        self.listener_dev_id = '' # provided later by the listening Vessel
        
        # don't infer from echoes. can't check for circular messages, i.e.:
        if self.saturation_req: # and self.bloom_count > 1: <-- reminder: don't
        # and self.init_sender_addr != self.my_dev_id  <-- checks must be performed
        # and self.sender_addr != self.my_dev_id:      <-- at layer above  
            self.sender_dev_id = self.init_sender_addr
            self.sender_last_obs = self.init_send_time
            self.est_hops_to_sender = self.bloom_count
            self.relayer_dev_id = self.sender_addr
            self.relayer_last_obs = self.send_time
        else:
            self.sender_dev_id = self.sender_addr
            self.sender_last_obs = self.send_time
            self.est_hops_to_sender = 1
            self.relayer_dev_id = self.sender_dev_id
            self.relayer_last_obs = self.sender_last_obs
                    
        # might there be a reference to an earlier message?
        CHECK_REF = False
        if ',' in M[i]:
            M0 = M[i].split(',')[0]
            CHECK_REF = True
        else: 
            M0 = M[i]

        if not is_plausible_msg_type(M0):
            log.packet_errors('packet\'s request type is not translatable')
            return
        self.msg_type = int(M0) # conversion and validity already tested  
        self.msg_type_str = msg_code2msg_text_d[self.msg_type]
        self.response_requested = msg_type_is_request(self.msg_type)
        self.is_response = msg_type_is_response(self.msg_type)

        if CHECK_REF: 
            if len(M[i].split(',')) > 2:
                M1 = (M[i].split(',')[1]).strip('(').strip(')')
                M2 = (M[i].split(',')[2]).strip('(').strip(')')
                
                if is_plausible_MAC_addr(M1) and is_plausible_timestamp(M2):
                    self.ref_dev_id = normalize_addr(M1)
                    self.ref_time = float(M2)
                    self.ref_key = (self.ref_dev_id,self.ref_time)
                    self.ref_skey = '(%s,%f)' % (self.ref_dev_id,self.ref_time)
                    self.has_ref = True

        i = i + 1

        self.payload = M[i]

        if self.msg_type == MSG_TYPE_RTG: # do extra checks to allow for formatting mistakes
            if not is_plausible_number(self.payload):
                log.packet_errors('packet has implausible routing message payload (dep. count)')
                return
            
            self.dependent = self.sender_addr
            self.dependency_count = int(float(self.payload))
            self.sender_dependent = int(self.dependency_count > 0)
            self.sender_independent = int(self.dependency_count == 0)
            self.is_rtg = True
            
        if self.msg_type == MSG_TYPE_PANIC:
            self.is_panic = True

        if self.msg_type in HARVESTABLE:
            if self.msg_type in [MSG_TYPE_POS_RESPONSE, MSG_TYPE_POS_UPDATE]:
                # format: latitude,longitude
                if not are_plausible_GPS_coordinates(self.payload):
                    log.packet_errors('packet has implausible GPS coordinates in payload')
                    return

                if self.saturation_req:
                    self.obs_time = self.init_send_time
                    self.obs_dev_id = self.init_sender_addr
                else:
                    self.obs_time = self.send_time
                    self.obs_dev_id = self.sender_addr
                    
                self.obs_gps_lat, self.obs_gps_long = pluck_GPS_coordinates(self.payload)
            else: 
                # format: time of observation, latitude, longitude,
                #         observing device's dev_id, name of variable observed, measurement
                # where the addressee is the MAC address of a specific device, or 
                # '*' to match any device, so long as the message is multicast
                obs = self.payload.split(',')
                if len(obs) != 6:
                    log.packet_errors('packet has misformatted harvestable payload data')
                    return    

                # ruling out misconfigured messages
                obs_time, obs_gps_lat, obs_gps_long, obs_dev_id, obs_var_name, obs_val = obs
                if not is_plausible_timestamp(obs_time) \
                or not is_plausible_lat(obs_gps_lat) \
                or not is_plausible_long(obs_gps_long) \
                or not is_plausible_MAC_addr(obs_dev_id):
                    log.packet_errors('packet has implausible harvestable payload metadata')
                    return

                self.obs_time = float(obs_time)
                self.obs_gps_lat = float(obs_gps_lat)
                self.obs_gps_long = float(obs_gps_long)
                self.obs_dev_id = obs_dev_id
                self.obs_var_name = obs_var_name
                self.obs_val = convert_val(obs_val)

            # if demoing in a place where GPS is unavailable, add "seasoning"
            # to simulate movement of the device, like a buoy in the ocean
            # subjected to Brownian motion
            if self.obs_gps_lat == DEFAULT_LATITUDE:
                if wg.USE_BUOY_EFFECT: self.obs_gps_lat += season()

            if self.obs_gps_long == DEFAULT_LONGITUDE:
                if wg.USE_BUOY_EFFECT: self.obs_gps_long += season()

            self.is_harvestable = True

        i = i + 1

        # reserved for future use
        i = i + 1

        if not is_plausible_RSSI_value(M[i]): 
            log.packet_errors('packet RSSI value is implausible')
            self.RSSI_val = M[i] # need this for splitting
            print(pkt)
            return
        
        self.RSSI_val = float(M[i])
        i = i + 1

        if self.saturation_req: 
            # to avoid noisy chaos, only track the original message
            self.key = (self.init_sender_addr,self.init_send_time)
            self.key_dev_id = self.init_sender_addr
            self.key_time = self.init_send_time
        else:
            self.key = (self.sender_addr,self.send_time)
            self.key_dev_id = self.sender_addr
            self.key_time = self.send_time
            
        # for ease of logging
        #self.skey = str(self.key) # typecasting produces an inelegant key
        self.skey = '(%s,%f)' % (self.key_dev_id,self.key_time)

        if self.msg_type == MSG_TYPE_RTG:
            if not is_plausible_int(self.payload):
                log.packet_errors('packet routing hop count is implausible')
                return

        # if the packet survived the gauntlet, indicate as much
        self.pkt_valid = True

    # is this (self) message an amplification of the other (other_lmsg) message?
    def is_amplification_of(self,other_lmsg):
        if not self.saturation_req or not other_lmsg.saturation_req: return False
        if self.key != other_lmsg.key: return False
        if self.bloom_count <= other_lmsg.bloom_count: return False
        return True
    
    # is this (self) message definitely a response to the other (other_lmsg) message?
    # note: this does not distinguish between original requests and 
    # any amplifications.
    def is_response_to(self,other_lmsg):
        if not self.is_response or not other_lmsg.is_request: return False

        # is this response the right kind for the possible request?
        if self.msg_type != get_response_msg_type(other_lmsg.msg_type): return False
        
        # we're not checking whether this message is a response to a 
        # particular amplification of an original (saturation) request
        if other_lmsg.saturation_req:
            if self.send_time < other_lmsg.init_send_time: return False
        else: 
            if self.send_time < other_lmsg.send_time: return False
        
        # the following tests are not in the next function. this is what
        # distinguishes the two methods. only a reference to the original
        # request can definitely indicate that this message is a response
        if not self.has_ref: return False 
        if self.ref_key != other_lmsg.key: return False

        return True

    # without a reference to the original request, we can only confirm
    # plausibility. i.e. it is plausible that this (self) message is a 
    # response to the other message (other_lmsg).
    def could_be_response_to(self,other_lmsg):
        if not self.is_response or not other_lmsg.is_request: return False

        # is this response the right kind for the possible request?
        if self.msg_type != get_response_msg_type(other_lmsg.msg_type): return False
        
        # we're not checking whether this message is a response to a 
        # particular amplification of an original (saturation) request
        if other_lmsg.saturation_req:
            if self.send_time < other_lmsg.init_send_time: return False
        else: 
            if self.send_time < other_lmsg.send_time: return False
            
        return True
    
    def show_incoming(self):
        S = ''
        if self.saturation_req:
            S += self.init_sender_addr + ('[f]' % (self.init_send_time)) + '->'
        S += self.sender_addr + ('[f]' % (self.send_time)) + '->' + recipient_addr + ': ' + self.payload
        return S

    def add_ref(self): # only to be used by to_packet
        if not self.has_ref: return ''
        return ',%s,%f' % (self.ref_dev_id,self.ref_time)
 
    def to_packet(self): # only to be used by c_speaker
        # reminder:
        # PACKET_WITH_BLOOM = '%s/%d,%s,%f/%s/%f/%d%s/%s//:'
        if self.saturation_req:
            S = PACKET_WITH_BLOOM % (self.recipient_addr,
                self.bloom_count,self.init_sender_addr,self.init_send_time,
                self.sender_addr,
                self.send_time,
                self.msg_type,self.add_ref(),
                self.payload)
        # reminder:                 
        # PACKET_NO_BLOOM = '%s/0/%s/%f/%d%s/%s//:'                
        else:
            S = PACKET_NO_BLOOM % (self.recipient_addr,
                self.sender_addr,
                self.send_time,
                self.msg_type,self.add_ref(),
                self.payload)
        return S

    def __str__(self):
        return self.form_pkt()

    def generate_amplification(self,my_mac_addr):
        if not self.saturation_req:
            log.packet_errors('attempted amplification of non-saturation packet')
            return False
            
        flyer = copy.deepcopy(self)
        flyer.bloom_count += 1
        flyer.sender_addr = my_mac_addr
        flyer.send_time = now()

        flyer_pkt = add_fake_RSSI_to_packet(flyer.to_packet())
        
        reviewed_flyer = lora_message(flyer_pkt)      
        if not reviewed_flyer.pkt_valid:
            log.packet_errors('generated amplification packet is invalid')
            return False
            
        self.amplif_lmsg = reviewed_flyer
        return True

    # only handles replies to requests
    def generate_response(self,my_mac_addr):
        if not msg_type_is_request(self.msg_type):
            return False

        # filling in of the appropriate payload is done elsewhere
        for i in range(3):
            try:
                self.response_payload
            except: 
                log.debug('message waiting to send')
                time.sleep(SNOOZE_TIME)
                continue

        # could throw an error, but if this device can't send, odds are the entire 
        # system is about to implode shortly
        if i == 3: 
            return False

        if self.multicast:
            RECIP_FIELD = self.sender_addr

        TIME_FIELD = now()

        if self.saturation_req:
            RECIP_FIELD = self.init_sender_addr
        else:
            RECIP_FIELD = self.sender_addr
        
        # defaults, changed as necessary
        SENDER_FIELD = my_mac_addr

        # this function only handles requests
        MSG_TYPE_FIELD = get_response_msg_type(self.msg_type)
        
        PAYLOAD = self.response_payload 
        RESERVED_FIELD = RESERVED_DEFAULT

        RESPONSE_REF = ''
        if self.response_requested:
            if self.saturation_req:
                resp_ref_addr = self.init_sender_addr
                resp_ref_time = str(self.init_send_time)
            else:
                resp_ref_addr = self.sender_addr
                resp_ref_time = str(self.send_time)
            RESPONSE_REF += ',%s,%s' % (resp_ref_addr,resp_ref_time)

        # reminder:
        # PACKET_WITH_BLOOM = '%s/%d,%s,%f/%s/%f/%d%s/%s//:'
        if self.saturation_req:
            S = PACKET_WITH_BLOOM % (RECIP_FIELD,
                1,my_mac_addr,TIME_FIELD,
                SENDER_FIELD,
                TIME_FIELD,
                MSG_TYPE_FIELD, RESPONSE_REF,
                PAYLOAD)
        # reminder:                 
        # PACKET_NO_BLOOM = '%s/0/%s/%f/%d%s/%s//:'   
        else:
            S = PACKET_NO_BLOOM % (RECIP_FIELD,
                SENDER_FIELD,
                TIME_FIELD,
                MSG_TYPE_FIELD, RESPONSE_REF,
                PAYLOAD)

        # for last checks. use best available estimate of an RSSI value.
        self.response_pkt = add_fake_RSSI_to_packet(S)
        
        # said last checks
        self.response_lmsg = lora_message(self.response_pkt)
        
        # optional. could force checks by the caller, or just let it blow up
        # when a boolean is referenced like a lora_message
        if not self.response_lmsg.pkt_valid:
            log.packet_errors('generated response packet is invalid')
            return False
        
        return True

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

    def generate_mini_df(self,listener_dev_id):
        mini_df = {}
        
        mini_df['receipt_time'] = [self.receipt_time]
        mini_df['receipt_ts'] = [pd.to_datetime(self.receipt_time,unit='s')]
        mini_df['listener_dev_id'] = [listener_dev_id]      
        mini_df['skey'] = [self.skey]
        
        mini_df['recipient_addr'] = [self.recipient_addr]
        mini_df['bloom_count'] = [self.bloom_count]
        mini_df['init_sender_addr'] = ['']
        mini_df['init_send_time'] = [0]  
        
        if self.saturation_req:
            mini_df['init_sender_addr'] = [self.init_sender_addr]
            mini_df['init_send_time'] = [self.init_send_time]
        
        mini_df['sender_addr'] = [self.sender_addr]
        mini_df['send_time'] = [self.send_time]

        mini_df['sender_dev_id'] = [self.sender_dev_id]
        mini_df['sender_last_obs'] = [self.sender_last_obs]
        mini_df['est_hops_to_sender'] = [self.est_hops_to_sender]
        mini_df['relayer_dev_id'] = [self.relayer_dev_id]
        mini_df['relayer_last_obs'] = [self.relayer_last_obs]
        
        mini_df['msg_type'] = [self.msg_type]
        mini_df['msg_type_str'] = [msg_code2msg_text_d[self.msg_type]]
        
        # default values
        mini_df['ref_dev_id'] = ['']
        mini_df['ref_time'] = [0]
        mini_df['ref_skey'] = ['']
        
        # change if applicable
        if self.is_response and self.has_ref:
            mini_df['ref_dev_id'] = [self.ref_dev_id]
            mini_df['ref_time'] = [self.ref_time]
            mini_df['ref_skey'] = [self.ref_skey]    
        
        mini_df['payload'] = [self.payload]
        
        # default values
        mini_df['obs_time'] = [0]
        mini_df['obs_dev_id'] = ['']
        mini_df['obs_gps_lat'] = ['']
        mini_df['obs_gps_long'] = ['']
        mini_df['obs_var_name'] = ['']
        mini_df['obs_val'] = ['']
        
        # change if applicable
        if self.is_harvestable:
            mini_df['obs_time'] = [self.obs_time]
            mini_df['obs_dev_id'] = [self.obs_dev_id]
            mini_df['obs_gps_lat'] = [self.obs_gps_lat]
            mini_df['obs_gps_long'] = [self.obs_gps_long]            
            if self.msg_type not in [MSG_TYPE_POS_RESPONSE,MSG_TYPE_POS_UPDATE]:
                mini_df['obs_var_name'] = [self.obs_var_name]
                mini_df['obs_val'] = [self.obs_val]                
        
        # a reminder: 
        # DEV_NEEDS = 1
        # DEV_AMBIVALENT = 0.5
        # DEV_INDEPENDENT = 0
        mini_df['dependency_count'] = [DEV_AMBIVALENT] # default
        
        if self.is_rtg:
            mini_df['dependency_count'] = [self.dependency_count]
        
        mini_df['RSSI_val'] = [self.RSSI_val]
        mini_df['key_dev_id'] = [self.key_dev_id]
        mini_df['key_time'] = [self.key_time]

        # default values are in the except clause
        try:
            if self.amplif_lmsg.pkt_valid: # if available
                mini_df['listener_prom_skey'] = [self.amplif_lmsg.skey]
        except:
            mini_df['listener_prom_skey'] = ['']

        try:
            if self.response_lmsg.pkt_valid: # if available
                mini_df['listener_resp_skey'] = [self.response_lmsg.skey]
        except:
            mini_df['listener_resp_skey'] = ['']

        df = pd.DataFrame(mini_df)
        df.set_index('receipt_ts',inplace=True)
        return df

    # faster
    def __ne__(self,other):
        if self.send_time != other.send_time:
            return True
        
        if self.key != other.key:
            return True

        return False

    def __eq__(self,other):
        return not self.__neq__(other)
