#!/usr/bin/env python3

import os
import time
import argparse
import socket
import threading
import lace, logging
import subprocess

import libdlt
from unis.models import Exnode, Service, Node, schemaLoader
from unis.runtime import Runtime

GEOLOC="http://unis.crest.iu.edu/schema/ext/dln/1/geoloc#"
FERRY_SERVICE="http://unis.crest.iu.edu/schema/ext/dln/1/ferry#"

UNIS_URL="http://localhost:8888"
LOCAL_UNIS_HOST="localhost"
LOCAL_UNIS_PORT=9000

GPS_DEV_LOC='/dev/ttyS0' # path/location to the Hat's GPS device
GPS_DEV_READ_LEN=50 # number of lines of output to read from said device
MAX_GPS_READ_ATTEMPTS=3 # number of times to attempt extraction of GPS coordinates

# the call to read the data
GPS_DEV_PROC_CALL='sudo cat %s | head -n %d' % (GPS_DEV_LOC,GPS_DEV_READ_LEN)

# default values for the location of the ferry. for now, Bloomington, Indiana.
BLOOMINGTON_LATITUDE=39.16533 # "vertical axis" ~ y
BLOOMINGTON_LONGITUDE=-86.52639 # "horizontal axis" ~ x

# globals
DOWNLOAD_DIR="/depot/web"
log = None
sess = None

DLNFerry = schemaLoader.get_class(FERRY_SERVICE)
GeoLoc = schemaLoader.get_class(GEOLOC)

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

                log.info('Ferry location identified as %f,%f' % (latitude,longitude))
                return (latitude,longitude)

        # no line of output contained the data we needed. cleanup
        # and try again, if so desired.
        p.kill() 

    log.info('Ferry location estimated to be %f,%f' % (latitude,longitude))
    return (latitude,longitude)

def register(rt, name, fqdn):
    n = rt.nodes.where({"name": name})
    try:
        n = next(n)
    except:
        n = Node();
        n.name = name
        rt.insert(n, commit=True)

    # add GPS coordinates extracted from the Hat
    n.location.latitude,n.location.longitude = retrieve_gps()

    s = rt.services.where({"runningOn": n})
    try:
        s = next(s)
    except:
        s = DLNFerry()
        s.runningOn = n
        s.serviceType="datalogistics:wdln:ferry"
        s.name = name
        s.accessPoint = "ibp://{}:6714".format(fqdn)
        s.unis_url = "http://{}:{}".format(fqdn, LOCAL_UNIS_PORT)
        s.status = "READY"
        rt.insert(s, commit=True)

    # simply update the timestamps on our node and service resources
    def touch(n,s):
        while True:
            time.sleep(5)
            try:
                n.poke()
                s.poke()
            except Exception as e:
                log.error("Could not update node/service resources: {}".format(e))
        
    th = threading.Thread(
        name='toucher',
        target=touch,
        daemon=True,
        args=(n,s,),
    )
    th.start()

    return (n,s)
    
def init_runtime(remote, local, local_only):
    while True:
        try:
            if local_only:
                urls = [{"default": True, "url": local}]
                opts = {"preload": ["nodes", "services", "exnodes"], "subscribe": {"exnodes": file_cb}}
                log.debug("Connecting to UNIS instance(s): {}".format(local))
            else:
                urls = [{"default": True, "url": remote}, {"url": local}]
                opts = {"preload": ["nodes", "services"]}
                log.debug("Connecting to UNIS instance(s): {}".format(remote+','+local))
            rt = Runtime(urls, **opts)
            return rt
        except Exception as e:
            #import traceback
            #traceback.print_exc()
            log.warn("Could not contact UNIS servers {}, retrying...".format(urls))
        time.sleep(5)

def file_cb(ex):
    time.sleep(2)
    local_download(sess, [ex])

def local_download(sess, exnodes):
    for f in exnodes:
        if not len(f.extents):
            continue
        log.info("Downloading: {} ({} bytes)".format(f.name, f.size))
        try:
            diff, dsize, res = sess.download(f.selfRef, "{}/{}".format(DOWNLOAD_DIR,f.name))
        except Exception as e:
            log.error("Could not download file: {}".format(e))
            continue
        if dsize != res.size:
            log.warn("WARNING: {}: transferred {} of {} bytes \
            (check depot file)".format(res.name,dsize,res.size))
        else:
            log.info("{0} ({1} {2:.2f} MB/s) {3}".format(res.name, res.size,
                res.size/1e6/diff,res.selfRef))
            
def run_local(sess, n, s, rt):
    i=0
    while True:
        (i%5) or log.info("Waiting for some local action...")
        i+=1
        time.sleep(1)
        
def run_remote(sess, n, s, rt):
    i=0
    while True:
        (i%5) or log.info("Waiting for some remote action...")
        if s.status == "UPDATE":
            dl_list = s.new_exnodes
            log.info("Caught UPDATE status with {} new exnodes".format(len(dl_list)))
            local_download(sess, dl_list)
            time.sleep(1)
            s.status = "READY"
            s.commit()
        i+=1
        time.sleep(1)
    
def init_logging(args):
    level = logging.DEBUG if args.verbose else logging.INFO
    level = logging.CRITICAL if args.quiet else level
    log = lace.logging.getLogger("ferry")
    log.setLevel(level)
    return log
        
def main():
    global log
    global DOWNLOAD_DIR
    global sess
    
    parser = argparse.ArgumentParser(description="DLN Mobile Ferry Agent")
    parser.add_argument('-H', '--host', type=str, default=UNIS_URL,
        help='UNIS instance for registration and metadata')
    parser.add_argument('-n', '--name', type=str, default=None,
        help='Set ferry node name (ignore system hostname)')
    parser.add_argument('-d', '--download', type=str, default=DOWNLOAD_DIR,
        help='Set local download directory')
    parser.add_argument('-l', '--local', action='store_true',
        help='Run using only local UNIS instance (on-ferry)')
    parser.add_argument('-v', '--verbose', action='store_true',
        help='Produce verbose output from the script')
    parser.add_argument('-q', '--quiet', action='store_true',
        help='Quiet mode, no logging output')

    args = parser.parse_args()
    log = init_logging(args)
    
    name = socket.gethostname()
    fqdn = socket.getfqdn()
    log.info("Ferry \"{}\" reporting for duty".format(name))
    if args.name:
        name = args.name
        log.info("Setting ferry name to \"{}\"".format(name))

    DOWNLOAD_DIR = args.download
    try:
        os.makedirs(DOWNLOAD_DIR)
    except FileExistsError:
        pass
    except OSError as exp:
        raise exp

    # use fqdn to determine local endpoints
    LOCAL_DEPOT={"ibp://{}:6714".format(fqdn): { "enabled": True}}

    # allow an alternative UNIS instance (non-ferry) in local mode
    if (args.local and args.host != UNIS_URL):
        LOCAL_UNIS=args.host
    else:
        LOCAL_UNIS = "http://{}:{}".format(fqdn, LOCAL_UNIS_PORT)
    
    # get our initial UNIS-RT and libdlt sessions
    rt = init_runtime(args.host, LOCAL_UNIS, args.local)
    sess = libdlt.Session([{"default": True, "url": LOCAL_UNIS}],
        bs="5m", depots=LOCAL_DEPOT, threads=1)

    # Start the registration loop
    # returns handles to the node and service objects
    (n,s) = register(rt, name, fqdn)

    # run our main loop
    if args.local:
        run_local(sess, n, s, rt)
    else:
        run_remote(sess, n, s, rt)
    
if __name__ == "__main__":
    main()
