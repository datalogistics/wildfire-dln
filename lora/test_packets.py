'''**************************************************************************

File: test_packets.py
Language: Python 3.7
Author: Juliette Zerick (jzerick@iu.edu)
        for the WildfireDLN Project
        OPEN Networks Lab at Indiana University-Bloomington

Contained are methods for a test harness for the whisper tool. It is meant
to be imported and then invoked in a Python interpreter (e.g. Jupyter
Notebook) on a Raspberry Pi. 

Test messages (stimulus) are manually inserted into message queues and the 
response observed by capturing output from outgoing message queues and
the message_store. 

Last modified: November 27, 2018

****************************************************************************'''

from inspect import getframeinfo, stack

from whisper import *
import whisper_globals as wg

'''
The test harness involves running code in an interpreter in each RPi, manually 
inserting into the message queues. Take the stimulus_response pairs, 
run stimulus in order, check against the expected response.
'''

def cleanup_and_report(test_result):
    caller = getframeinfo(stack()[1][0])
    caller_filename = caller.filename
    caller_line_num = caller.lineno
    
    if test_result:
        log.debug('\n\ntest ok!\n')
    else:
        log.debug('mopup requested from %s at line %d' % (caller_filename,caller_line_num))
        mopup()
        exit()    
    
def print_msg(M):
    print('\t to:                   %s' % (M.recipient_addr))
    print('\t saturation requested: %d' % (M.bloom_count))
    
    if M.saturation_req:
        print('\t\t initial sender:    %s' % (M.init_sender_addr)) 
        print('\t\t initial send time: %f' % (M.init_send_time))
    
    print('\t from:                 %s' % (M.sender_addr))
    print('\t time sent:            %f' % (M.send_time))
    print('\t request type:         %d' % (M.msg_type))
    print('\t payload:              %s' % (M.payload))

def single_test(stimulus,transit_qs):
    global inbox_q,outbox_q,carto_q

    stim_lmsg = lora_message(stimulus)
    
    if not stim_lmsg.pkt_valid:
        log.debug('stimulus packet was invalid')
        exit()
 
    log.debug('injecting packet into the inbox')
    inbox_q.put(stim_lmsg)

    time.sleep(3)

    # did we hit the transition queues? 
    for t in transit_qs:
        if t == carto_q and not stim_lmsg.hit_carto: return cleanup_and_report(False)
        if t == altar_q and not stim_lmsg.hit_altar: return cleanup_and_report(False)
        if t == outbox_q and not stim_lmsg.hit_outbox: return cleanup_and_report(False)
        if t == wg.msg_store and not stim_lmsg.hit_msg_store: return cleanup_and_report(False)

    if stim_lmsg.hit_msg_store:
        print('\n   retrieved from message store:\n')
        
        for K in wg.msg_store.inventory.keys():
            print('     <',K,wg.msg_store.inventory[K],'>')
            retrieved_from_msg_store = wg.msg_store.stock[K]
            print_msg(retrieved_from_msg_store)
            print()
            
    if stim_lmsg.hit_outbox:
        print('\n   response retrieved from outbox:\n')
        
        retrieved_from_outbox = outbox_q.get_nowait()
        print_msg(retrieved_from_outbox)
        return retrieved_from_outbox        
        
        
