There are two options here. First, you can use the Multitech MultiConnect Conduit with the xDot for precise communication. If at all possible *avoid resetting the gateway*. Unfortunately occasionally the device does become unresponsive without a reset.  

The basic wrapper class for the xDot is xdot.py. The version that doesn't rely on gateway responses as confirmation of receipt, and may allow explosions to occur, is xdot_no_guardrails.py. 

xdot.py 
xdot_no_guardrails.py (handle with care)

send_coords.py retrieves GPS coordinates off the Dragino LoRa/GPS Hat and submits them to the gateway via the xDot. If an MQTT server is available, it is possible to observe the transfer by monitoring an MQTT client.

send_coords.py 	
send_coords_with_observation.py (requires an MQTT server)

The second option is simply broadcasting coordinates using the Hat, or turning the RPi+Hat into a single-channel gateway, using the following code, compiled with Makefile and requiring the BCM2835 and RadioHead libraries. 

rf95_client.cpp (broadcasts GPS coordinates input as command-line arguments, attempts a number of times to confirm receipt)
rf95_server.cpp (receives messages and replies)

