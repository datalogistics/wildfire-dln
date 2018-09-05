import subprocess

# Bryce's xDot wrapper
from xdot_no_guardrails import * # no return message from gateway => unnecessary explosions

GPS_DEV_READ_LEN=50 # number of lines of output to read from gps3

# default values for the location of the ferry. for now, Bloomington, Indiana.
BLOOMINGTON_LATITUDE=39.16533 # "vertical axis" ~ y
BLOOMINGTON_LONGITUDE=-86.52639 # "horizontal axis" ~ x

GPS_DEV_LOC='/dev/ttyS0' # path/location to the Hat's GPS device
GPS_DEV_READ_LEN=50 # number of lines of output to read from said device
MAX_GPS_READ_ATTEMPTS=3 # number of times to attempt extraction of GPS coordinates

# the call to read the data
GPS_DEV_PROC_CALL='sudo cat %s | head -n %d' % (GPS_DEV_LOC,GPS_DEV_READ_LEN)
# small function to parse out latitude/longitude values from device output.

# this function naively assumes that the input string S contains the data
# needed and formatted as expected--explosions incurred from parsing failures
# are to be caught in the calling function.
def extract_coords(S):
    S0 = S.split(',')
    latitude = float(S0[2]) / 100.
    lat_dir = S0[3]
    longitude = float(S0[4]) / 100.
    long_dir = S0[5]

    if lat_dir == 'S':
        latitude = -latitude

    if long_dir == 'W':
        longitude = -longitude

    return (latitude,longitude)

# attempts to retrieve the device's current GPS coordinates, reading
# GPS_DEV_READ_LEN lines of output from GPS_DEV_LOC per attempt, with
# at most MAX_GPS_READ_ATTEMPTS attempts. 
def retrieve_gps():
    latitude = BLOOMINGTON_LATITUDE 
    longitude = BLOOMINGTON_LONGITUDE

    for i in range(MAX_GPS_READ_ATTEMPTS):
        p = subprocess.Popen(GPS_DEV_PROC_CALL,shell=True,
                stdin=subprocess.PIPE,stdout=subprocess.PIPE,
                stderr=subprocess.PIPE) # for tidiness

        for j in range(GPS_DEV_READ_LEN):
            S = p.stdout.readline()
            
            # convert bytes->str (ASCII) if necessary
            if type(S) == bytes:
                try: # sometimes fails
                    S = S.decode('ascii')
                except: # conversion failed! try the next line
                    continue

            # now that we have a string, search it for an indicator
            # of the presence of GPS coordinate data
            if 'GPGGA' in S: # specifically this
                try: # attempt parsing
                    (latitude,longitude) = extract_coords(S)
                except: # parsing failed! try the next line
                    continue

                # parsing successful!
                p.kill() # cleanup
                return (latitude,longitude)

        # no line of output contained the data we needed. cleanup
        # and try again, if so desired.
        p.kill()

    return (latitude,longitude)

def send_coords():
    xD = xDot()

    if (xD.join_status() != True):
        if (not xD.join_network("MTCDT-19400691","MTCDT-19400691",1)):
            return

    latitude,longitude = retrieve_gps()

    # sent messages can be at most ~10 characters. for better precision, send
    # two separate messages. when possible, retrieve an ACK from the gateway to
    # acknowledge receipt of the data.
    msg = '%.6f' % (latitude)
    xD.send_message(msg) # this publishes an UP message 

    msg = '%.6f' % (longitude)
    xD.send_message(msg) # this publishes an UP message 

    xD.close_port() 

if __name__ == "__main__":
    send_coords()
