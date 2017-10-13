goTenna ATAK Plugin

_________________________________________________________________
PURPOSE AND CAPABILITIES

The purpose of the goTenna ATAK plugin is to use the goTenna radios as a
network transport for simple situation awareness data (e.g., PLI, Chat).

One goTenna device with a secondary network (e.g., TAK Server) will proxy blue
force tracks it receives over goTenna to the secondary network.

_________________________________________________________________
STATUS

Subset of Releases:
  ATAK-Plugin-GoTenna-civ-3.3.23803.apk
  ATAK-Plugin-GoTenna-mil-3.3.23748.apk
  ATAK-Plugin-GoTenna-mil-3.3.23858.apk
  ATAK-Plugin-GoTenna-mil-3.3.24465.apk

_________________________________________________________________
ATAK VERSIONS

ATAK 3.3, 3.4

_________________________________________________________________
POINT OF CONTACTS

Primary Developer Contact:     

Kyle Usbeck
 kusbeck@bbn.com
 617.873.3681
 BBN Technologies

_________________________________________________________________
USER GROUPS

_________________________________________________________________
EQUIPMENT REQUIRED

goTenna 
www.gotenna.com

_________________________________________________________________
EQUIPMENT SUPPORTED

_________________________________________________________________
COMPILATION

       for local testing 

                gradle clean assembleDebug

       for finished apk
       
                gradle clean assembleRelease


_________________________________________________________________
DEVELOPER NOTES

This currently uses a goTenna SDK application token that is registered with
goTenna to Kyle Usbeck kusbeck@bbn.com
