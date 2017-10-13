FLIR-BT
_________________________________________________________________
PURPOSE AND CAPABILITIES

Receives GPS and Laser Range information over Bluetooth
_________________________________________________________________
STATUS

Initial Version.

_________________________________________________________________
ATAK VERSIONS

ATAK 3.5.2
_________________________________________________________________
POINT OF CONTACTS

dan.winsor@flir.com

_________________________________________________________________
USER GROUPS

None

_________________________________________________________________
EQUIPMENT REQUIRED

FLIR Recon V with Bluetooth module add-on.

_________________________________________________________________
EQUIPMENT SUPPORTED

FLIR Recon V with Bluetooth module add-on.

_________________________________________________________________
COMPILATION

_________________________________________________________________
DEVELOPER NOTES
Nmea messages supported:

GPS: $GGA and $RMC  (See Nmea documentation for format)

LRF: FLIR custom Nmea message $PFLTM in the form:

$PFLTM,nnn, ccccc, nnn, nnn, nnn, hhmmss.00,*CC\n
        ^     ^     ^    ^    ^     ^        ^
        |     |     |    |    |     |        |
        |     |     |    |    |     |        +----- Standard Nmea checksum. 
        |     |     |    |    |     |
        |     |     |    |    |     +-------------- time of laser range
        |     |     |    |    |
        |     |     |    |    +-------------------- Inclination in degrees
        |     |     |    |
        |     |     |    +------------------------- AZ Angle in degrees
        |     |     |
        |     |     +------------------------------ Distance in Meters
        |     |
        |      +----------------------------------- Target name
        |
        +------------------------------------------ target number
