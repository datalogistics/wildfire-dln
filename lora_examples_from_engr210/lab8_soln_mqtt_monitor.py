# For ENGR210 Spring 2018, Lab 8

# Author: Juliette Zerick
# Note: This is a solution for Lab 8 using a simulated recipient, inferring the receipt of messages
# by the server by observing the flow of messages rather than using the mqtt client to confirm
# directly. To perform the latter, fill in the appropriate code and set USING_MQTT_CLIENT=True.

# standard tools for parsing output of automated processes
import sys
import os
import subprocess
import threading
import time
import re

# tools specific to this task
import json 
import Queue
import base64 # note that an old version is installed, uses legacy inteface

# Bryce's xDot wrapper
from xdot import *

# use either the simulated recipient or add code to interact with the server via the mqtt client
USING_SIM = True
USING_MQTT_CLIENT = False 

# for enforcing proper parameter selection
def pick_one(choices): 
	total = sum(map(lambda x: int(x),choices)) # mapping guaranteed in Python
	return total == 1

if not pick_one([USING_MQTT_CLIENT,USING_SIM]):
	print 'Please select for use either the simulated recipient or MQTT client.'
	exit(1)

######## CONSTANTS, GLOBAL VARIABLES, AND UTILITIES ##############################################

# the number of seconds between attempted actions by threads
SNOOZE_TIME = 1

# could automate retrieval of this too, but we have enough to deal with already
MY_ADDR = '00-80-00-00-04-00-4b-79'

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

	exit()

# atomic printing function, for readable output
def atomic_print(S):
	CONCH.acquire()
	print S
	CONCH.release()

# FUNCTIONALITY: PUBLISH A MESSAGE VIA COMMAND-LINE ##############################################

PUB_DOWN_UNFORMATTED = 'mosquitto_pub -h pivot.iuiot.org -t lora/%s/down -m \"{\"data\":\"%s\",\"deveui\":\"%s\"}\"'
PUB_UP_UNFORMATTED = 'mosquitto_pub -h pivot.iuiot.org -t lora/%s/up -m \"{\"data\":\"%s\",\"deveui\":\"%s\"}\"'

# not sure why but base64.encodestring() adds a newline at the end of the returned string
def publish(S0,direction):
	S = S0.strip()
	enc64_S = base64.encodestring(S).replace('\n','')

	# can uncomment but it gets verbose
	if direction == 'down':
		#atomic_print('\t<-- publish sending DOWN message ' + S)
		os.system(PUB_DOWN_UNFORMATTED % (MY_ADDR,enc64_S,MY_ADDR))
	elif direction == 'up':
		#atomic_print('\t--> publish> sending UP message ' + S)
		os.system(PUB_UP_UNFORMATTED % (MY_ADDR,enc64_S,MY_ADDR))
		# else: do nothing

# FUNCTIONALITY: MONITOR THE FLOW OF MESSAGES TO INFER EVENTS ON THE SERVER ######################

# call to launch the monitored process
PROC_SUB_ALL_CALL = 'mosquitto_sub -h pivot.iuiot.org -v -t lora/%s/+' % (MY_ADDR)

# convert JSON string to JSON object
def s2j(S):
	return json.loads(S.split(' ')[1])

# default output, should parsing fail
DECODE_FAIL = ''

# utility to extract data from string S, whatever form it might take
def retrieve_int(S):
	# Note: try-except blocks are notorious for hiding bugs and causing debugging headaches.
	# only apply them when you are very sure the contained code is correct, i.e. it explodes
	# only when it's supposed to.

	try: # try decoding, assuming S contains a JSON object
		J = s2j(S)
		D = base64.decodestring(J['data'])
		D = int(D)
		return D
	except: # that didn't work
		pass

	try: # try Bryce's decoding, then try treating D as containing a JSON object
		D = S.replace('0a','').replace('0d','').strip()
		J = s2j(D)
		D = base64.decodestring(J['data'])
		D = int(D)
		return D
	except: # this didn't work either
		pass

	try: # ok, try regular expressions
		D = re.findall('data:(.+),deveui',S)[0]
		D = base64.decodestring(D)
		D = int(D)
		return D
	except: # out of ideas
		pass

	# all parsing attempts failed, return a default value
	return DECODE_FAIL

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

		# got something
		datum = retrieve_int(msg)

		# store and report
		if '/down' in msg:
			down_q.put(msg) # stored full message
			atomic_print('<%s> observed DOWN message %s' % (proc_name,str(datum)))
		elif '/up' in msg:
			up_q.put(msg) # stored full message
			atomic_print('<%s> observed UP message %s' % (proc_name,str(datum)))
		'''
		else: # optional. may output blank messages.
			atomic_print('<%s> observed message %s' % (proc_name,str(datum)))
		'''

# queues for storing observed messages; declared here for tidiness
up_q = Queue.Queue()
down_q = Queue.Queue()

# FUNCTIONALITY: SIMULATE THE RECIPIENT (SERVER) ######################################################

# launch the mosquitto_sub process (also, generally should avoid shell=True)
proc_sub_all = subprocess.Popen(PROC_SUB_ALL_CALL,shell=True,stdin=subprocess.PIPE,stdout=subprocess.PIPE)
PROCESS_BUCKET.append(proc_sub_all) # add to the bucket for easier cleanup

# thread for observing messages coming out of the previously launched process
psub_all_t = threading.Thread(target=observe_message_flow, args=('pivot observer',proc_sub_all.stdout, up_q,down_q))
psub_all_t.daemon = True # so it dies with the host process
psub_all_t.start() # add to the bucket for easier cleanup
THREAD_BUCKET.append(psub_all_t)

# this will be launched as a thread--this is the simulated recipient. it waits for a message (fed by
# the observer thread), retrieves it (from up_q), and publishes a response. 
def simulated_recipient(proc_name,pipe,up_q,down_q):
	atomic_print('<%s> started dequeueing' % (proc_name))
	my_counter = -1 

	while True:
		try: # retrieve a message from the up_q
			msg = up_q.get_nowait()
		except:
			time.sleep(SNOOZE_TIME)
			continue

		datum = retrieve_int(msg)

		# technically an unnecessary check
		if datum == my_counter + 1: 
			my_counter = datum + 1
			publish(str(my_counter),'down')
			atomic_print('<%s> returned potato with %d notch(es)' % (proc_name,my_counter))

		if my_counter >= 10:
			return

if USING_SIM:
	selected_proc = simulated_recipient

# alternative: fill this in to use the MQTT client
def mqtt_client(proc_name,pipe,up_q,down_q):
	atomic_print('<%s> started listening' % (proc_name))

	# do stuff

if USING_MQTT_CLIENT:
	selected_proc = mqtt_client

prec_t = threading.Thread(target=selected_proc, args=('simulated recipient',sys.stdout, up_q,down_q))
prec_t.daemon = True # dies with the host process
prec_t.start()
THREAD_BUCKET.append(prec_t)

# FUNCTIONALITY: INTERACT WITH XDOT VIA BRYCE'S WRAPPER CLASS #########################################

# use the lock to ensure non-garbled printing of messages from Bryce's class
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

if USING_SIM:
	atomic_print('** using simulated recipient **')

	# start the message passing
	my_counter = 0
	atomic_print('[xDot] throws potato with zero notches')
	xD.send_message(str(my_counter)) # this publishes an UP message 

	# rather than come up with conditionals, run an infinite while loop and break it conditionally
	# it's faster to code but prone to explosions
	while True:
		try: # retrieve a message from the down_q
			msg = down_q.get_nowait()
		except:
			time.sleep(SNOOZE_TIME)
			continue

		datum = retrieve_int(msg)

		# technically an unnecessary check
		if datum == my_counter + 1:
			my_counter = datum + 1

			# have to break the message-passing loop somewhere. do it here.
			if my_counter > 9:
				atomic_print('[xDot] keeps the potato with ten notches')
				break

			atomic_print('[xDot] throws potato with %d notch(es)' % my_counter)
			xD.send_message(str(my_counter)) # this publishes an UP message 

elif USING_MQTT_CLIENT:
	atomic_print('** using MQTT client **')

	'''
	mqtt client code goes here
	'''

atomic_print('done!')
mopup(xD)
