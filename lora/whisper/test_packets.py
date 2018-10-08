from whisper_protocol import *

'''
The test harness involves running code in an interpreter in each RPi, manually 
inserting into the message queues. Take the stimulus_response pairs, 
run stimulus in order, check against the expected response.
'''

PACKET_NO_BLOOM = '%s/0/%s/%f/%d/%s//%f|'
PACKET_WITH_BLOOM = '%s/%d,%s,%f/%s/%f/%d/%s//%f|'

my_addr = MY_MAC_ADDR
neighbor_addr = 'aa:bb:cc:dd:ee:ff'
far_neighbor_addr = 'ff:ee:dd:cc:bb:aa'
multicast = '*'
init_sender_addr = '00:11:22:33:44:55'

init_sender_time = now() - 20
neighbor_send_time = now() - 10
far_neighbor_send_time = now() - 5

payload = 'POS'
response_payload = '42,42'
RSSI_val = -42

bloom_count = 1

notification = MSG_TYPE_POS_UPDATE
request = MSG_TYPE_POS_REQUEST
response = MSG_TYPE_POS_RESPONSE

stimulus_response = []

no_response_pkt = 'no response'
dropped_pkt = 'dropped'
adjust_thresholds = 'adjust thresholds'

# ---------------------------------------------------
# MULTICAST, SATURATION REQUESTED, RESPONSE REQUESTED
# ---------------------------------------------------

# request from the initial sender, seen by the recipients
p01 = PACKET_WITH_BLOOM % (multicast,
    bloom_count, init_sender_addr, init_sender_time,
    initial_sender_addr,init_sender_time,
    request, payload, RSSI_val)
    
# request propagated by recipients
p02 = PACKET_WITH_BLOOM % (multicast,
    bloom_count+1, init_sender_addr, init_sender_time,
    my_addr, now(),
    request, payload, RSSI_val)

stimulus_response.append((p01,p02))

# further observations of propagated requests
# - ADJUST THRESHOLDS -

stimulus_response.append((p02,adjust_thresholds))

# response from the recipients to the initial sender
p03 = PACKET_WITH_BLOOM % (init_sender_addr,
    bloom_count, my_addr, now(),
    my_addr, now(),
    response, response_payload, RSSI_val)
    
stimulus_response.append((p01,p03))

# responses propagated by the recipients
p04 = PACKET_WITH_BLOOM % (init_sender_addr,
    bloom_count+1, neighbor_addr, neighbor_send_time,
    my_addr, now(),
    response, response_payload, RSSI_val)

stimulus_response.append((p03,p04))

# further observations of propagated responses
# - ADJUST THRESHOLDS -

stimulus_response.append((p04,adjust_thresholds))

# ------------------------------------------------------                                                   
# MULTICAST, SATURATION REQUESTED, NO RESPONSE REQUESTED 
# ------------------------------------------------------

# notification from the initial sender, seen by the recipients
p05 = PACKET_WITH_BLOOM % (multicast,
    bloom_count, init_sender_addr, init_sender_time,
    initial_sender_addr,init_sender_time,
    notification, payload, RSSI_val)
    
# recipients' reaction to the first observation of the notification
p06 = PACKET_WITH_BLOOM % (multicast,
    bloom_count+1, init_sender_addr, init_sender_time,
    my_addr, now(),
    notification, payload, RSSI_val)

stimulus_response.append((p05,p06))

# recipients' reaction to further observations of the notification
# - ADJUST THRESHOLDS - 

stimulus_response.append((p06,adjust_thresholds))

# -------------------------------------------------------
# MULTICAST, SATURATION NOT REQUESTED, RESPONSE REQUESTED 
# -------------------------------------------------------

# request from the initial sender, seen by the recipients
p07 = PACKET_NO_BLOOM % (multicast,
    init_sender_addr, init_sender_time,
    request, payload, RSSI_val)
    
# response from the recipients to the initial sender
p08 = PACKET_NO_BLOOM % (init_sender_addr,
    my_addr, now(),
    response, response_payload, RSSI_val)

stimulus_response.append((p07,p08))

# response seen by a neighbor
p09 = PACKET_NO_BLOOM % (init_sender_addr,
    neighbor_addr, neighbor_send_time,
    response, response_payload, RSSI_val)

# reaction to the response seen by the neighbor
# - DROP PACKET - 

stimulus_response.append((p09,dropped_pkt))

# ----------------------------------------------------------
# MULTICAST, SATURATION NOT REQUESTED, NO RESPONSE REQUESTED 
# ----------------------------------------------------------

# notification from the initial sender, seen by the recipient
p10 = PACKET_NO_BLOOM % (multicast,
    init_sender_addr, init_sender_time,
    notification, payload, RSSI_val)
    
# message from the recipients to the initial sender
# - NONE -

stimulus_response.append((p10,no_response_pkt))

# ------------------------------------------------------
# NO MULTICAST, SATURATION REQUESTED, RESPONSE REQUESTED 
# ------------------------------------------------------

# request from the initial sender, seen by the recipient
p11 = PACKET_WITH_BLOOM % (my_addr,
    bloom_count, init_sender_addr, init_sender_time,
    initial_sender_addr,init_sender_time,
    request, payload, RSSI_val)
    
# request as seen by a neighbor
p12 = PACKET_WITH_BLOOM % (neighbor_addr,
    bloom_count, init_sender_addr, init_sender_time,
    initial_sender_addr, init_sender_time,
    request, payload, RSSI_val)
    
# neighbor's reaction to the request
p13 = PACKET_WITH_BLOOM % (neighbor_addr,
    bloom_count+1, init_sender_addr, init_sender_time,
    my_addr, now(),
    request, payload, RSSI_val)

stimulus_response((p12,p13))

# response from the recipient to the request
p14 = PACKET_WITH_BLOOM % (init_sender_addr,
    bloom_count, my_addr, now(),
    my_addr, now(),
    response, response_payload, RSSI_val)

stimulus_response((p11,p14))
stimulus_response((p13,p14))

# recipient's reaction to further neighbor-propagated requests
# - ADJUST THRESHOLDS - 

stimulus_response((p13,adjust_thresholds))

# response as seen by a neighbor
p15 = PACKET_WITH_BLOOM % (init_sender_addr,
    bloom_count, neighbor_addr, neighbor_send_time,
    neighbor_addr, neighbor_send_time,
    response, response_payload, RSSI_val)

# neighbor's reaction to the response
p16 = PACKET_WITH_BLOOM % (init_sender_addr,
    bloom_count+1, neighbor_addr, neighbor_send_time,
    my_addr, now(),
    response, response_payload, RSSI_val)

stimulus_response((p15,p16))

# recipient's reaction to further neighbor-propagated responses
# - ADJUST THRESHOLDS - 

stimulus_response((p16,adjust_thresholds))

# ---------------------------------------------------------
# NO MULTICAST, SATURATION REQUESTED, NO RESPONSE REQUESTED 
# ---------------------------------------------------------

# notification from the initial sender, seen by the recipient
p17 = PACKET_WITH_BLOOM % (my_addr,
    bloom_count, init_sender_addr, init_sender_time,
    initial_sender_addr,init_sender_time,
    notification, payload, RSSI_val)

# response from the recipient
# - NONE -

stimulus_response.append((p17,no_response_pkt))

# notification as seen by a neighbor
p18 = PACKET_WITH_BLOOM % (neighbor_addr,
    bloom_count, init_sender_addr, init_sender_time,
    initial_sender_addr,init_sender_time,
    notification, payload, RSSI_val)
    
# neighbor's reaction to the first observation of the notification 
p19 = PACKET_WITH_BLOOM % (neighbor_addr,
    bloom_count+1, init_sender_addr, init_sender_time,
    my_addr, now(),
    notification, payload, RSSI_val)

stimulus_response.append((p18,p19))

# recipients' reaction to further observations of the notification
# - ADJUST THRESHOLDS - 

stimulus_response.append((p19,adjust_thresholds))

# ----------------------------------------------------------
# NO MULTICAST, SATURATION NOT REQUESTED, RESPONSE REQUESTED 
# ----------------------------------------------------------

# request from the initial sender, seen by the recipient
p20 = PACKET_NO_BLOOM % (my_addr,
    init_sender_addr, init_sender_time,
    request, payload, RSSI_val)

# message from the recipient to the initial sender
p21 = PACKET_NO_BLOOM % (init_sender_addr,
    my_addr, now(),
    response, response_payload, RSSI_val)
    
stimulus_response.append((p20,p21))

# request as seen by a neighbor
p22 = PACKET_NO_BLOOM % (neighbor_addr,
    init_sender_addr, init_sender_time,
    request, payload, RSSI_val)
    
# neighbor's reaction to the request
# - DROP PACKET -

stimulus_response.append((p22,dropped_pkt))

# response as seen by a neighbor
p23 = PACKET_NO_BLOOM % (init_sender_addr,
    neighbor_addr, neighbor_send_time,
    response, response_payload, RSSI_val)

# neighbor's reaction to the response
# - DROP PACKET -

stimulus_response.append((p23,dropped_pkt))

# -------------------------------------------------------------
# NO MULTICAST, SATURATION NOT REQUESTED, NO RESPONSE REQUESTED 
# -------------------------------------------------------------

# notification from the initial sender, seen by the recipient
p24 = PACKET_NO_BLOOM % (my_addr,
    init_sender_addr, init_sender_time,
    notification, payload, RSSI_val)

# message from the recipient to the initial sender
# - NONE -

stimulus_response.append((p24,no_response_pkt))

# notification as seen by a neighbor
p25 = PACKET_NO_BLOOM % (neighbor_addr,
    init_sender_addr, init_sender_time,
    notification, payload, RSSI_val)

# neighbor's reaction to the notification
# - DROP PACKET -

stimulus_response.append((p25,dropped_pkt))
