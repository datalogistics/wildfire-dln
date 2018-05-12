#!/usr/bin/env python

"""xDot AT Communication 
Class to enable sending and receiving from the xDot. 

This code was tested using an xDot Micro Developer Kit. The code assumes that a serial port connection to the xDot is available. Additionally, the xDot must be running the AT command interpreter. 

Notes about using the xDot Class

1.) Join the network: call join_network with the appropriate
network_id and network_key for your access point.  

2.) If the xDot is joined to an access point, send a message using
send_message.  

3.) To receive a packet from the access point, a packet must be sent -
although the packet does not need to include any useful data. An end
point device like the xDot, can receive one packet directly after
sending a packet. This minimizes the amount of time that the xDot mush
be in receive mode waiting for a packet.

"""

import serial
import base64
import sys
import re
import time

class xDot:
    def __init__(self,xDot_serial='/dev/ttyACM0',baud=115200):
        if (self.open_port(xDot_serial,baud)!=True):
            sys.exit(1)
        if (self.check_comm()!=True):
            print "xDot Failed to respond correctly to Attention (AT) Command"
            sys.exit(1)
        self.command_return_string = ""
        self.eui = ""
        self.whoami()      # Loads the eui variable with the device id of the adapter. 
        self.timeout = 2   # Timeout value for the serial port 

    def close_port(self):
        """ Close serial port used to communicate with the xDot """
        self.port.close()

    def open_port(self,serial_port,baud): 
        """ Open the serial port exported by the xDot developer's board """
        try:
            self.port = serial.Serial(serial_port, baud, timeout = 3) 
            return True
        except serial.serialutil.SerialException:
            print "Error: Could not open",serial_port
            return False
    
    def send_command(self,command_string):
        """ Send an AT command to the xDot """
        tic = time.time()
        self.port.write(command_string+"\n")
        response = ""
        self.command_return_string = ""
        while ((response != "OK") and (response != "ERROR")):
            response = self.port.readline().strip()
            if ((time.time() - tic) > 10):
                print "Command Timeout" 
                return False
            if ((response != "") and (response != "OK") and (response != "ERROR") and (response != command_string)):
                self.command_return_string = self.command_return_string + response + "\n"
        if (response == "OK"):
            return True
        else:
            print "response came back ERROR"
            return False
               
    def check_comm(self):
        """ Use the attention command to confirm that communicating with the xDot is possible """
        if (self.send_command("AT") == True):
            return True
        else:
            return False
    
    def whoami(self): 
        """ Read the xDot's EUI identifier (LoRa MAC address) """
        if (self.send_command("AT+DI") == True):
            self.eui = self.command_return_string.strip()
            return True
        else:
            return False

    def join_status(self):
        """ Check to see if the xDot is currently joined to the access point """
        match = re.compile('1')
        if (self.send_command("AT+NJS") == True):
            if (match.search(self.command_return_string)):
                return True
            else:
                return False
        else:
            print "xDot failed to respond to AT+NJS command"
            return False
    
    def join_network(self, network_id, network_key, network_freq_sub_band):
        """ Connect to the access point using network id and network key """
        ni_command = "AT+NI=1,"+network_id
        nk_command = "AT+NK=1,"+network_key
        fsb_command = "AT+FSB="+str(network_freq_sub_band)

        if (self.send_command(fsb_command) == False):
            print fsb_command
            return False
        if (self.send_command(ni_command) == False):
            print ni_command 
            return False
        if (self.send_command(nk_command) == False):
            print nk_command
            return False
        if (self.send_command("AT+JOIN") == False):
            print "AT+JOIN failed"
            return False
        return True

    def send_message(self, message):
        """ Cause the xDot to send a message to the LoRa access point. Any received data will be placed in command_return_string. """ 
        send_command = "AT+SEND="+message
        self.send_command(send_command)
        self.command_return_string = self.command_return_string.replace('0a','')
        self.command_return_string = self.command_return_string.replace('0d','')
        self.command_return_string = self.command_return_string.strip()
        try: 
            ret_val = bytearray.fromhex(self.command_return_string)
        except ValueError: 
            ret_val = ""
        return ret_val
            
if __name__ == "__main__":
    xD = xDot()
    if (xD.join_status() == True):
        print "Already connected to LoRa access point."
    else:
        print "Connecting to the LoRa access point"
        if (not xD.join_network("MTCDT-19400691","MTCDT-19400691",1)):
            print "Error: Failed to connect to the LoRa Access point."
            sys.exit(1)
        else:
            print "Connected to LoRa access point: MTCDT-19400691."
    received_data = xD.send_message("Hi Bryce!")
    print received_data
    xD.close_port()    
        
