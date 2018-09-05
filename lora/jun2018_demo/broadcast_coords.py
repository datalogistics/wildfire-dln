# author: Juliette Zerick (jzerick@iu.edu)

import subprocess

LORA_PROC_CALL='sudo ./rf95_client %f %f'

GPS_DEV_READ_LEN=50 # number of lines of output to read from gps3

# default values for the location of the ferry. for now, Bloomington, Indiana.
BLOOMINGTON_LATITUDE=39.16533 # "vertical axis" ~ y
BLOOMINGTON_LONGITUDE=-86.52639 # "horizontal axis" ~ x

# default values for the San Diego Convetion Center
SANDIEGO_LATITUDE=32.707175 # ~ y-axis
SANDIEGO_LONGITUDE=-117.162417 # ~ x-axis

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
    latitude = SANDIEGO_LATITUDE 
    longitude = SANDIEGO_LONGITUDE

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
    latitude,longitude = retrieve_gps()
    proc_call = LORA_PROC_CALL % (latitude,longitude)
    print 'running:',proc_call

    p = subprocess.Popen(proc_call,shell=True,
        stdin=subprocess.PIPE,stdout=subprocess.PIPE,
        stderr=subprocess.PIPE) # for tidiness

    # bad idea. don't do it.
    #while p.stdout:
    #    print p.stdout.readline()

    p.communicate() # cleanup, if not using kill()
    # p.kill() # cleanup, if not using communicate()

if __name__ == "__main__":
    send_coords()
