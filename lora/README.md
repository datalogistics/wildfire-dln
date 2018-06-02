LoRa has a number of uses by the ferries. There are currently two options to do so. 

Multitech MultiConnect Conduit + xDot
----------------------------------------------------

The MultiConnect Conduit is a powerful gateway enabling precise communication. It plays well with paired hardware like the xDot, a USB-attachable transceiver. 

The basic wrapper class for the xDot is `xdot.py`. The version that doesn't rely on gateway responses as confirmation of receipt, and may allow explosions to occur, is `xdot_no_guardrails.py`.

`send_coords.py` retrieves GPS coordinates off the Dragino LoRa/GPS Hat and submits them to the gateway via the xDot. If an MQTT server is available, it is possible to observe the transfer by monitoring an MQTT client via `send_coords_with_observation.py`.

### For the demo:

**Receiver**: Connect the Conduit via mini-USB cable to your laptop, inserting the mini-USB end into the port on the back of the Conduit. Open up a terminal and run the following:

```sudo screen /dev/ttyACM0 115200```

You may need to press the enter key or control+C to see the login screen. The login and password information will be sent via Slack private message.

When the console is available, run the following to monitor messages incoming to the gateway:

```mosquitto_sub lora/+/+```

**Transmitter**: Attach an xDot to one of the RPis and retrieve from the repo  `xdot_no_guardrails.py` and `send_coords.py`. Run `sudo python send_coords.py` whenever you wish to transmit GPS coordinates.

When you run the Python script on the transmitter, the messages will show as JSON-encoded strings in the receiver console. 

**WARNING**: *If at all possible, avoid resetting the gateway*. Unfortunately occasionally the device does become unresponsive without a reset. If the gateway becomes unresponsive, naturally first try power cycling or rebooting with a quick push of the reset button. Failing that, you can reset to defaults (3s<push<30s). In both cases you can configure the gateway by attaching it to your laptop via USB-ethernet adapter. Open an internet browser and navigate to `192.168.2.1`. Enter the login and password (given by Slack message). 

If you have reset to defaults, use the default login and passphrase `admin` and `admin`. Immediately run the First Time Setup Wizard; it should pop up on its own but can be selected as a tab on the left side of the screen. Keep the wifi and cellular options disabled. Once done, navigate to the Setup / LoRa tab. Set the LoRa mode to be Network Server. Define the network name to be MTCDT-19400691 and the passphrase to be the same; tick the box to enable a Public server. Also, change the default login and password. The remaining default options should suffice. Official documentation is available [here](http://www.multitech.net/developer/software/aep/getting-started-aep/).

If things really hit the fan, you can revert to basic firmware by holding down the reset button for more than 30s. You will then need to reinstall the Conduit interface software. Connect the Conduit via mini-USB cable to your laptop, and start up `screen` like previously described. The default login and password will now be `root` and `root`. You will need to mount the micro SD card already inserted behind the front panel. Find the appropriate device via `ls /dev/m*` . The card name will follow this pattern: `m.*p1` (starts with m and ends with p1). Mount the device via `mount /dev/<card name> /mnt`

Enter the `/mnt` directory. The interface installation file will follow this pattern: `.*upgrade.bin` (file that ends with upgrade.bin). Once you've identified the file, create a new directory via `mkdir /var/volatile/flash-upgrade`. Copy over the installation file with 
`cp /mnt/<installation file> /var/volatile/flash-upgrade/upgrade.bin`, then run `touch /var/volatile/do_flash_upgrade` and `reboot`. Official documentation is [here](http://www.multitech.net/developer/software/mlinux/using-mlinux/flashing-mlinux-firmware-for-conduit/
) under the second section.

Log back in to the device and mount the SD card again. Copy over the `*.ipk` files to `/home/root`. First install both packet-forwarding packages with opkg: `opkg install <filename>`. Then install the lora-network-server package. Again `reboot`. At this point you can hook up the Conduit to your laptop with the USB-ethernet adapter and configure as described previously. 

Two RPi+Hat Transceivers
----------------------------------
The second option is simply broadcasting coordinates using the Hat and optionally turning the RPi+Hat into a single-channel gateway, using the following: `rf95_client.cpp` broadcasts GPS coordinates input as command-line arguments, attempts a number of times to confirm receipt and `rf95_server.cpp` receives messages and replies.

Use requires installation of the BCM2835 and RadioHead libraries and compilation with the Makefile. Upon installing RadioHead, you can simply descend the directory to reach the rf95_server/client code in `/RadioHead/examples/raspi/rf95`. It is easiest to replace the existing code in the directory (with the above) and compiling with `make` in situ. On one RPi, run `sudo ./rf95_server` program while on the other run `sudo ./rf95_client`. The client will broadcast its GPS coordinates, and the server will reply to the client. Note that the client can simply broadcast without reply but will be lonely without acknowledgment from the server. 

Note: this is *not* the code currently loaded on the RPis en route to the conference.

### For the demo

The above code was slightly modified to simulate a distant ferry periodically transmitting its location with a listener receiving the messages and updating the appropriate Node; the client process transmits until it receives acknowledgment by the server process. Python wrapper scripts `broadcast_coordinates.py` and `lora_listener.py`, respectively, repeat the process for the sake of demonstration. Most of the code has been uploaded to this repo under `for_node_update` but is already available on the RPis provided (bob and stuart). Relevant code to attach to a local UNISrt instance (and update something) has been included in the `lora_listener.py` script. 

**Transmitter**: Power up bob and attach via the USB-ethernet adapter to your laptop. ssh into `192.168.1.90` and descend the RadioHead directories as described previously. Start up the broadcasting script with `nohup ./broadcast_script.sh`
Close your terminal to end the ssh session but *don't kill the process*. Detach bob from your laptop and let him run. He will broadcast every few seconds for 10-12 hours, or until you power him down. 

**Receiver**: Power up stuart and attach via the USB-ethernet adapter to your laptop. ssh into `192.168.1.90` and descend the RadioHead directories like before. Run `sudo python lora_listener.py`. The script will periodically listen for bob's broadcast. This code is not yet uploaded to this repo but is on the RPi. 
