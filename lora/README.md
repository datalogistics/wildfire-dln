The basic xDot wrapper class is xdot.py. The version that doesn't rely on gateway responses, and may allow explosions to occur, is xdot_no_guardrails.py. 

xdot.py 
xdot_no_guardrails.py - handle with care

These are two solutions to ENGR 210 Lab 8 (Spring 2018) that use the xDot LoRa device (via xdot wrapper class) and an MQTT server. The lab description is here: https://github.iu.edu/SOIC-Digital-Systems/Spring-2018/wiki/Lab8

lab8_soln_mqtt_monitor.py - uses a threaded monitor to confirm transfer and send responses
lab8_soln_typical.py - only uses the xDot wrapper and MQTT client to respond 

The solutions above were used to create methods for the ferry. send_coords_with_observation.py will retrieve GPS coordinates from the LoRa/GPS Hat and send them over LoRa via the xDot wrapper class, with a subprocess to monitor messages and confirm that messages have been sent. send_coords.py has the barebones functions to do the same without any confirmation.

send_coords_with_observation.py 
send_coords.py 	
