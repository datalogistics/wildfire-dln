#!/usr/bin/env python

'''**************************************************************************

File: whisper.py
Language: Python 3.6/7
Author: Juliette Zerick (jzerick@iu.edu)
        for the WildfireDLN Project
        OPEN Networks Lab at Indiana University-Bloomington

This contains the higher-level protocol and switch behaviors. It interfaces
with whisper.cpp, which quickly filters relevant messages that are passed
to whisper.py.

A number of threads were used to logically separate production and consumption
of messages for ease of troubleshooting. The use of queues adds a cushion to
avoid lost messages, providing some resiliency.

Last modified: March 17, 2019

****************************************************************************'''

import pathlib
import os
import copy

import matplotlib.pyplot as plt
from mpl_toolkits.mplot3d import Axes3D
from matplotlib.patches import Circle, Rectangle
import mpl_toolkits.mplot3d.art3d as art3d

from minion import *
import whisper_globals as wg

#TODO put this in settings file when the code stabilizes

# for CoAP
#import asyncio 
#from aiocoap import *
#import aiocoap.resource as resource

# for uploading files
#import libdlt

# this is getting damned annoying
import sys # if the following path does not exist, no error will be thrown
sys.path.append('/usr/local/lib/python3.6/dist-packages') 

USING_NAMES = False
try:
    import names
    log.info('names package was imported successfully')
    USING_NAMES = True
except:
    log.info('names package could not be imported')

POP_SIZE = 2
OUT_OF_RANGE = 10
GRID_SIZE = 5 # square extending from (0,0) to (GRID_SIZE,GRID_SIZE)

USING_PLOT = False

SIM_TIMESTEP = 0.5 # in seconds
SIM_RUNTIME = 6 # in seconds

if SIM_TIMESTEP == 0:
    NUM_ITERATIONS = 10 # default
else:
    NUM_ITERATIONS = int(SIM_RUNTIME / SIM_TIMESTEP)

INBOX_IMP_COLOR = 'g'
INBOX_IMP_MARKER = 'o'
OUTBOX_IMP_COLOR = 'b'
OUTBOX_IMP_MARKER = 'o'
RESP_IMP_COLOR = 'r'
RESP_IMP_MARKER = 'o'

# pulled from the makefile and invoked in preflight_checks()
# note that compilation will only succeed on the RPi; if wiringPi does not
# recognize the hardware it's being run on, the library will avert its
# inclusion.
WHISPER_C_FN = 'whisper'
MAKE_WHISPER = 'g++ -Wall -o %s -lwiringPi -pthread whisper.cpp' % (WHISPER_C_FN)
MAKE_CLEAN = 'rm -rf __pycache__ edit %s *.o a.out *.pyc' % (WHISPER_C_FN)

# pulled from the ferry code
UNIS_URL="http://localhost:8888"
LOCAL_UNIS_HOST="localhost"
LOCAL_UNIS_PORT=9000

PERISCOPE_PN = 'periscoped'
PERISCOPE_LAUNCH = '%s -p %d' % (PERISCOPE_PN,LOCAL_UNIS_PORT)

POSSIBLE_MINION_NAMES = ['Bob','Kevin','Stuart','Dave','Carl']

def get_minion_name():
    if USING_NAMES:
        return names.get_first_name(gender='male')+str(random.randint(1,1000))
        #return names.get_first_name(gender='male') # TODO put back

    return random.choice(POSSIBLE_MINION_NAMES) + str(random.randint(1,POP_SIZE*100))

def minion_dist(M,N):
    d = np.sqrt((M.curr_lat - N.curr_lat)**2 + (M.curr_long - N.curr_long)**2)
    return d

# roughly interpolating an exponential distribution including these points:
# (0,-20), (OUT_OF_RANGE,-120), (OUT_OF_RANGE/2,-50)
def sim_RSSI(M,N):
    d = minion_dist(M,N)
    a = -np.log(4.533333)/OUT_OF_RANGE
    b = -22.5
    c = 2.5
    return b*np.exp(a*d) + c
    
# Solution from Matt J (2009), then edited by Grimthorr (2018) at StackOverflow 
# in response to the following posted question:
# "How do I capture SIGINT in Python?" available at
# <https://stackoverflow.com/questions/1112343/how-do-i-capture-sigint-in-python>
# last accessed: November 27, 2018
def signal_handler(sig, frame):
    wg.closing_time = True
    time.sleep(SNOOZE_TIME)
    mopup()
    
# Solution from Jeremy Grifski of The Renegade Coder.
# "How to Check if a File Exists in Python," posted February 17, 2018, available at:
# <https://therenegadecoder.com/code/how-to-check-if-a-file-exists-in-python/>.
# last accessed: December 21, 2018.
def file_exists(fn):
    p = pathlib.Path('./whisper') 
    return p.is_file()

# Solution from 
# Solution from mluebke (2011), then edited by monk-time (2017) at StackOverflow 
# in response to the following posted question:
# "Python check if a process is running or not?" available at
# <https://stackoverflow.com/questions/7787120/python-check-if-a-process-is-running-or-not>.
# last accessed: December 21, 2018.
def process_running(pn):
    return pn in (p.name() for p in psutil.process_iter())

# check if whisper exists; if not, compile; check for daemons, etc. 
def preflight_checks():
    # was this script run with the Python 3 interpreter? 
    major_version_num = sys.version_info[0]
    minor_version_num = sys.version_info[1]

    if major_version_num < 3:
        log.error('must run with Python 3.x')
        return False

    # do we have UNIS?
    if wg.USING_UNIS and not wg.have_UNIS():
        log.critical('unable to connect to UNIS instance')
        return False

    # we're running on hardware
    if not SIM_MODE:
        # does whisper-c exist in compiled form?
        if not file_exists(WHISPER_C_FN): # try compiling
            log.error('whisper-c executable not found, attempting compilation')
            os.system(MAKE_CLEAN)
            os.system(MAKE_WHISPER)
        
        # if compilation failed, bail
        if not file_exists(WHISPER_C_FN):
            log.critical('whisper-c compilation failed')
            return False
    
    # is periscoped running?
    '''
    if not process_running(PERISCOPE_PN):
        # try launching it
        os.system(PERISCOPE_LAUNCH)
    
    # if launching failed, bail
    if not process_running(PERISCOPE_PN): 
        log.error('periscoped not found')
        return False
    '''
    
    return True

def gen_horde_coord():
    return random.random()*GRID_SIZE

# vertical zero vector of length POP_SIZE, transposed for readability
def get_zero_vec(): # note that numpy.matrix is deprecated
    v = np.zeros((1,POP_SIZE),dtype='int32') # arrays are recommended
    return v

# square zero matrix of dimension POP_SIZE x POP_SIZE
def get_zero_mat(): # note that numpy.matrix is deprecated
    m = np.zeros((POP_SIZE,POP_SIZE),dtype='int32') # arrays are recommended
    return m

class minionfest:
    def __init__(self):
        self.rt = wg.rt
        self.horde = []
    
        for i in range(POP_SIZE):
            # are there female minions? thus far only males have been observed. 
            minion_name = get_minion_name()
            minion_lat = gen_horde_coord()
            minion_long = gen_horde_coord()
            self.horde.append(minion(minion_name,self.rt))
            
            self.horde[i].begin()
            self.horde[i].curr_lat = minion_lat
            self.horde[i].curr_long = minion_long
            
            log.info('minion %s is located at (%f,%f)' % (minion_name,minion_long,minion_lat))

        self.fig = plt.figure()
        self.ax = self.fig.add_subplot(111, projection='3d')
        self.ax.set_xlabel('longitude')
        self.ax.set_ylabel('latitude')
        self.ax.set_zlabel('time')
        
        self.ax.set_xlim3d(0,GRID_SIZE)
        self.ax.set_ylim3d(0,GRID_SIZE)
        self.ax.set_zlim3d(now(),now()+3)
    
        self.summon_field_master()
            
    def within_range(self,M,N):
        return minion_dist(M,N) < OUT_OF_RANGE
            
    def field_master(self):
        log.info('thread active')
    
        if USING_PLOT:
            for M in self.horde:
                r = Rectangle((M.curr_long,M.curr_lat),0.5,0.5,alpha=1,\
                    ec = "green", fc = "lime")
                self.ax.add_patch(r)
                art3d.pathpatch_2d_to_3d(r, z=now(), zdir="z")               

            #plt.legend([r], ['WDLN device'])
                
        added_blast_to_legend = False   
    
        while not wg.closing_time:
            for M in self.horde:
                for msg in M.dump_outbox():
                    if USING_PLOT:
                        # alpha = 1 => opaque
                        # alpha = 0 => transparent
                        c = Circle((M.curr_long,M.curr_lat),OUT_OF_RANGE,alpha=0.2,\
                            ec = "black", fc = "CornflowerBlue")
                        self.ax.add_patch(c)
                        art3d.pathpatch_2d_to_3d(c, z=now(), zdir="z")
                        
                        if not added_blast_to_legend:
                            plt.legend([r,c], ['WDLN device', 'message broadcast radius'])                         
                            added_blast_to_legend = True
                                    
                    for N in self.horde:
                        # some redundant computation here
                        if N != M and self.within_range(M,N) and N.is_listening:
                            reissued = copy.deepcopy(msg)
                            reissued.RSSI_val = sim_RSSI(M,N)
                            N.inbox_q.put(reissued)
                            log.debug('transferred %s -> %s' % (M.name,N.name))
        
    def summon_field_master(self):
        fm_t = threading.Thread(target=self.field_master, args = [])
        fm_t.daemon = True # so it does with the host process
        THREAD_BUCKET.append(fm_t) # for easier cleanup
        fm_t.start()
        self.fm_t = fm_t

    def get_inbox_imprint(self,lmsg,t):
        if not SIM_MODE:
            return get_zero_vec()
    
        v = get_zero_vec()
        K = lmsg.sim_key
        for i in range(len(self.horde)):
            M = self.horde[i]
            if K in M.inbox_record.keys():
                M.inbox_record[K]
                v[0,i] = M.inbox_record[K]
                
                if USING_PLOT:
                    x = M.curr_long
                    y = M.curr_lat
                    
                    if (x,y) not in self.inbox_imprint_bin:
                        #self.ax.scatter(x,y,t,c=INBOX_IMP_COLOR,marker=INBOX_IMP_MARKER)
                        self.inbox_imprint_bin.add((x,y))

        return v

    def get_outbox_imprint(self,lmsg,t):
        if not SIM_MODE:
            return get_zero_vec()
    
        v = get_zero_vec()
        K = lmsg.sim_key
        for i in range(len(self.horde)):
            M = self.horde[i]
            if K in M.outbox_record.keys():
                v[0,i] = M.outbox_record[K]
                
                if USING_PLOT:
                    x = M.curr_long
                    y = M.curr_lat
                    
                    if (x,y) not in self.outbox_imprint_bin:
                        #self.ax.scatter(x,y,t,c=OUTBOX_IMP_COLOR,marker=OUTBOX_IMP_MARKER)
                        self.outbox_imprint_bin.add((x,y))
                    
        return v 

    def get_response_imprint(self,lmsg,t):
        if not SIM_MODE:
            return get_zero_vec()

        v = get_zero_vec()
        K = lmsg.sim_key
        for i in range(len(self.horde)):
            M = self.horde[i]
            if K in M.response_record.keys():
                R = M.response_record[K] # get the response
                v[0,i] = M.outbox_record[R] # match the observation in the outbox

                if USING_PLOT:
                    x = M.curr_long
                    y = M.curr_lat

                    if (x,y) not in self.response_imprint_bin:
                        #self.ax.scatter(x,y,t,c=RESP_IMP_COLOR,marker=RESP_IMP_MARKER)
                        self.response_imprint_bin.add((x,y))

        return v         

    def run(self):
        # insert item
        if len(self.horde) > 0:
            first_minion = self.horde[0]
        
            # test: requests position with saturation - passed
            #plmsg = self.horde[0].prod_flood(MULTICAST,MSG_TYPE_POS_REQUEST,'tagh')
            
            # TODO move this to a Jupyter notebook
            
            # for your UNIS integration testing pleasure, the options are:
            '''
            MSG_TYPE_UNIS_POST_NOTIF
            MSG_TYPE_UNIS_POST_ACK_REQ
            MSG_TYPE_UNIS_POST_ACK
            MSG_TYPE_UNIS_GET_REQUEST 
            MSG_TYPE_UNIS_GET_RESPONSE 
            '''
            
            # test: posts data - passed
            '''
            msg_type = MSG_TYPE_UNIS_POST_NOTIF            
            dev_id = first_minion.node.dev_id
            var_name = 'name'
            val = first_minion.name
            message = '%s,%s,%s' % (dev_id,var_name,str(val))
            plmsg = first_minion.prod_flood(MULTICAST,msg_type,message)
            '''

            # test: MSG_TYPE_UNIS_GET_REQUEST, var exists - passed
            '''         
            msg_type = MSG_TYPE_UNIS_GET_REQUEST            
            dev_id = first_minion.node.dev_id
            var_name = 'name'
            val = first_minion.name
            message = '%s,%s' % (ANY_DEVICE,var_name)
            plmsg = first_minion.prod_flood(MULTICAST,msg_type,message)             
            '''

            # test: MSG_TYPE_UNIS_GET_REQUEST, when var does not exist - passed
            '''
            msg_type = MSG_TYPE_UNIS_GET_REQUEST            
            dev_id = first_minion.node.dev_id
            var_name = 'nonexistent'
            val = first_minion.name
            message = '%s,%s' % (ANY_DEVICE,var_name)
            plmsg = first_minion.prod_flood(MULTICAST,msg_type,message)   
            '''
                      
            # test: MSG_TYPE_UNIS_POST_ACK_REQ
            '''
            msg_type = MSG_TYPE_UNIS_POST_ACK_REQ            
            dev_id = first_minion.node.dev_id
            var_name = 'name'
            val = first_minion.name
            message = '%s,%s,%s' % (dev_id,var_name,str(val))
            plmsg = first_minion.prod_flood(MULTICAST,msg_type,message)  
            '''
            
        self.outbox_imprint_bin = set()
        self.inbox_imprint_bin = set()
        self.response_imprint_bin = set()        
        
        start_time = now()
        L = []
        for t in range(NUM_ITERATIONS):
            v = self.get_inbox_imprint(plmsg,now())
            
            if np.sum(v) == POP_SIZE - 1:
                 end_time = now()
                 break
            
            time.sleep(SIM_TIMESTEP)
            L.append(np.sum(v) / (POP_SIZE - 1))
        
        if USING_PLOT:
            plt.show()
        
        time.sleep(10)
        wg.closing_time = True
       
def main():
    signal.signal(signal.SIGINT, signal_handler)

    if not preflight_checks():
        log.critical('preflight checks failed, bailing!')
        exit(1)
    
    if SIM_MODE:
        log.info('simulation starting')
        sim = minionfest()
        sim.run()
    else:
        bob = minion('bob',wg.rt)
        bob.begin()
   
    
    while not wg.closing_time:
        time.sleep(SNOOZE_TIME)  

if __name__ == "__main__":
    main()
