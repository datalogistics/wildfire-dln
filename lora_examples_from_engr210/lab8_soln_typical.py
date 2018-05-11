# For ENGR210 Spring 2018, Lab 8

import time
import json 
import base64 # note that an old version is installed, uses legacy inteface
import paho.mqtt.client as mqtt
import binascii

# Bryce's xDot wrapper
from xdot import *

# the number of seconds between attempted actions by threads
SNOOZE_TIME = 1

# could automate retrieval of this too, but we have enough to deal with already
MY_ADDR = '00-80-00-00-04-00-4b-79'
BROKER_ADDRESS = "pivot.iuiot.org" 

# this gets triggered by the sending of a message, i.e. xDot.send_message;
# some code from publish.py
def on_message(client, userdata, message):
	datum = base64.b64decode(json.loads(message.payload)['data']).strip('\n')

	try:
		datum = int(datum)
	except:
		return

	print '[server] receives the potato with %d notch(es), then adds a notch' % (datum)

	# sanity checks? pfft
	datum = datum + 1 # notch
	enc_datum = base64.standard_b64encode(str(datum))
	topic = 'lora/%s/down' % (MY_ADDR)
	reply = '{\"data\":\"%s\",\"deveui\":\"%s\"}' % (enc_datum,MY_ADDR)

	print '[server] hurls the potato with %d notch(es)' % (datum)
	client.publish(topic,reply) 

if __name__ == "__main__":
	xD = xDot() 

	# some code from xdot.py's main
	if (xD.join_status() == True):
		print("Already connected to LoRa access point.")
	else:
		print("Connecting to the LoRa access point")
		if (not xD.join_network("MTCDT-19400691","MTCDT-19400691",1)):
			print("Error: Failed to connect to the LoRa Access point.")
			mopup(xD)
		else:
			print("Connected to LoRa access point: MTCDT-19400691.")

	# some code from client.py 
	client = mqtt.Client("mqtt_client") 
	client.on_message=on_message
	client.connect(BROKER_ADDRESS)
	client.loop_start()
	client.subscribe('lora/%s/up' % MY_ADDR) 

	# wait for everything to spin up
	time.sleep(5)

	# start the message passing
	my_counter = 1 # local counter for sanity checks

	while True:
		print '[xDot] throws the potato with %d notch(es)' % (my_counter)
		reply = xD.send_message(str(my_counter)) # on_message triggers right now

		try:
			datum = int(reply)
		except:
			continue

		# sanity check
		if datum != my_counter + 1: 
			continue # try again? 

		# check passed, carry on
		if datum < 10:
			print '[xDot] retrieves the potato with %d notch(es), then adds a notch' % (datum)
			my_counter = datum + 1 # notch
		else:
			print('[xDot] keeps the potato with ten notches')
			break

	client.loop_stop()
