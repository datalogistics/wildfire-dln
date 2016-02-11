#!/usr/bin/env python

'''**************************************************************************

File: whipser_sim.py
Language: Python 3.6/7
Author: Juliette Zerick (jzerick@iu.edu)
        for the WildfireDLN Project
        OPeN Networks Lab at Indiana University-Bloomington

The whisper_sim class coordinates multiple vessels in simulations, with
methods available for simulated transfer of messages between devices.

Last modified: August 12, 2019

****************************************************************************'''

import pathlib
import os
import copy

import matplotlib.pyplot as plt
from mpl_toolkits.mplot3d import Axes3D
from matplotlib.patches import Circle, Rectangle
import mpl_toolkits.mplot3d.art3d as art3d
from scipy.interpolate import griddata
import numpy as np
from math import ceil

from vessel import *
import bridge

###############################################################################
#  FLAGS AND SETTINGS
#  Below are flags and settings with which to configure behavior.
###############################################################################

USING_NAMES = True
try:
    import names
    log.info('names package was imported successfully')
    USING_NAMES = True
except:
    log.info('names package could not be imported')
    USING_NAMES = False

POP_SIZE = 1
OUT_OF_RANGE = 15
NUDGE_FACTOR = .5
GRID_SIZE = 10 # square extending from (0,0) to (GRID_SIZE,GRID_SIZE)

USING_PLOT = False

SIM_TIMESTEP = 0.1 # in seconds
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

# pulled from the ferry code
UNIS_URL="http://localhost:8888"
LOCAL_UNIS_HOST="localhost"
LOCAL_UNIS_PORT=9000

PERISCOPE_PN = 'periscoped'
PERISCOPE_LAUNCH = '%s -p %d' % (PERISCOPE_PN,LOCAL_UNIS_PORT)

###############################################################################
#  UTILITIES
#  Below are small utility functions.
###############################################################################

# roughly interpolating an exponential distribution including these points:
# (0,-20), (OUT_OF_RANGE,-120), (OUT_OF_RANGE/2,-50)
def sim_RSSI(M,N):
    d = vessel_dist(M,N)
    a = -np.log(4.533333)/OUT_OF_RANGE
    b = -22.5
    c = 2.5
    return b*np.exp(a*d) + c

def get_vessel_name():
    # alternatively, if not testing with a shared instance of UNISrt
    #return names.get_first_name()
    return names.get_first_name()+str(random.randint(1,1000))

def create_spiral():
    global GRID_SIZE   

    # so nodes hear each other, nudge the nodes a little
    D = OUT_OF_RANGE * NUDGE_FACTOR # by reducing D

    # start at the origin and spiral out; for transference to a simulated
    # grid, translate points as necessary

    def R(theta):
        return theta + D/2
        
    def get_X(r):
        return 2*np.arcsin(D/(2*r))

    def get_x_coord(r,theta):
        return r*np.sin(theta)
        
    def get_y_coord(r,theta):
        return r*np.cos(theta)

    x = [0] # the first vessel
    y = [0]
    theta = 0

    # generate POPSIZE-1 more vessels
    for i in range(POP_SIZE-1):
        r = R(theta)
        theta = get_X(r) + theta
        x.append(get_x_coord(r,theta))
        y.append(get_y_coord(r,theta))

    width = max(x) - min(x)
    height = max(y) - min(y)
    GRID_SIZE = ceil(max([width,height]))

    x = np.array(x) - min(x)
    y = np.array(y) - min(y)    
    
    coords = list(zip(x,y))
    fleet = []
    for x0,y0 in coords:
        vessel_name = get_vessel_name()
        M = vessel(vessel_name)
        M.curr_lat = y0
        M.curr_long = x0
        fleet.append(M)

    # check number of nodes reachable by each node; for debugging
    '''
    for M in fleet:
        count = 0
        for N in fleet:
            if M.name != N.name:
                if vessel_dist(M,N) < OUT_OF_RANGE:
                    count += 1
        print(M.name,count)
    '''

    return fleet

###############################################################################
#  SIMULATION CLASS OBJECT
#  Below is the class which when configured and initialized generates a
#  simulation composed of multiple interacting vessels.
###############################################################################

class whisper_sim:
    def __init__(self):
        self.rt = bridge.rt
        self.fleet = []

        self.fleet = create_random_fleet()

        for i in range(POP_SIZE):
             M = self.fleet[i]
            log.data_flow('vessel %s is located at (%f,%f)' % (M.name,M.curr_long,M.curr_lat))

        self.create_name_mapping() 

        for M in self.fleet:
            M.begin()

        self.fig = plt.figure()
        self.ax = self.fig.add_subplot(111, projection='3d')
        self.ax.set_xlabel('longitude')
        self.ax.set_ylabel('latitude')
        self.ax.set_zlabel('time')
        
        self.ax.set_xlim3d(0,GRID_SIZE)
        self.ax.set_ylim3d(0,GRID_SIZE)
        self.ax.set_zlim3d(now(),now()+20)
    
        self.summon_field_master()
            
    def within_range(self,M,N):
        return vessel_dist(M,N) < OUT_OF_RANGE
            
    def field_master(self):
        log.data_flow('thread active')
    
        if USING_PLOT:
            for M in self.fleet:
                r = Rectangle((M.curr_long,M.curr_lat),0.5,0.5,alpha=1,\
                    ec = "green", fc = "lime")
                self.ax.add_patch(r)
                art3d.pathpatch_2d_to_3d(r, z=now(), zdir="z")     
                
                x = M.curr_long
                y = M.curr_lat
                z = 0
                zdir = 'x'
                label = '(%d, %d, %d), dir=%s' % (x, y, z, zdir)
                self.ax.text(x, y, z, label, zdir)

            #plt.legend([r], ['WDLN device'])
                
        added_blast_to_legend = False   
    
        while not bridge.closing_time:
            for M in self.fleet:
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
                                    
                    for N in self.fleet:
                        # some redundant computation here
                        if N != M and self.within_range(M,N):# and N.my_status: #TODO
                            reissued = copy.deepcopy(msg)
                            reissued.RSSI_val = sim_RSSI(M,N)
                            N.inbox_q.put(reissued)
                            #log.data_flow('transferred %s -> %s' % (M.name,N.name))
                            
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
        for i in range(len(self.fleet)):
            M = self.fleet[i]
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
        for i in range(len(self.fleet)):
            M = self.fleet[i]
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
        for i in range(len(self.fleet)):
            M = self.fleet[i]
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

    def create_name_mapping(self):
        if not SIM_MODE:
            return 
            
        table = {}
        for m in self.fleet:
            table[m.my_dev_id] = m.name
        
        self.dev2name = table

    def run(self):
        if len(self.fleet) > 0 and not BROADCASTING:
            # one option
            '''
            self.fleet[0].flood_var_broadcast('*')
            self.fleet[1].flood_var_broadcast('*')
            time.sleep(1)
            '''
            
            # another, less chatty option
            '''
            for Q in range(9):
                v0 = random.choice(range(len(self.fleet)))
                opt = random.choice(['flood_pos_request','flood_var_broadcast','best_estimate_dataset'])
                
                if opt == 'flood_pos_request':
                    self.fleet[v0].flood_pos_request('*')
                elif opt == 'flood_var_broadcast':
                    self.fleet[v0].flood_var_broadcast('*')
                elif opt == 'best_estimate_dataset':
                    print(self.fleet[v0].best_estimate_dataset())
                
                time.sleep(3)
            '''

            for Q in range(len(self.fleet)):
                self.fleet[Q].flood_var_broadcast('*')
                self.fleet[Q].flood_pos_update('*')
                time.sleep(random.random())
                
        time.sleep(30)
        
        # if monitoring message spread
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
        
        time.sleep(2)
        '''
        bridge.closing_time = True
        mopup()

        fig = plt.figure()
        ax = fig.add_subplot(111)

        node_coords_x = []
        node_coords_y = []

        for Q in range(len(self.fleet)):
            M = self.fleet[Q]
            M.save_run_results()
            M.gleaner_update()
            df_d = self.fleet[Q].best_estimate_dataset()
            
            if 'temp' not in df_d or len(df_d['temp']) < 4:
                continue
            
            print(self.fleet[Q].name,len(df_d['temp']))
            
            x = np.array(list(df_d['temp']['est_long']))
            y = np.array(list(df_d['temp']['est_lat']))
            z = np.array(list(df_d['temp']['obs_val']))
            
            node_coords_x += list(df_d['temp']['est_long'])
            node_coords_y += list(df_d['temp']['est_lat'])
            
            xi = yi = np.arange(0,GRID_SIZE+0.01,0.01)
            xi,yi = np.meshgrid(xi,yi)
        
            zi = griddata((x,y),z,(xi,yi),method='cubic')

            ax.contourf(xi,yi,zi,np.arange(0,GRID_SIZE+0.01,0.01))
        
        ax.scatter(node_coords_x,node_coords_y,c='black',label='nodes')
        
        plt.xlabel('longitude',fontsize=16)
        plt.ylabel('latitude',fontsize=16)
        plt.title('Composite contour map of locally interpolated, simulated temperature values')

        # turns out there's no really good way to add a colorbar.
        
        # so go ahead and plot.
        plt.show()
