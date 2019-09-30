import pandas as pd

# bear in mind the importation of these modules will be executed above the
# modules' containing directory
from lora.protocol import *
import lora.bridge as bridge

LAST_OBS_VAR_NAME = 'last_obs_time'

class cargo_hold:
    def __init__(self,my_name,my_dev_id,my_lora_id,vessel_transmit_f):
        self.name = my_name
        self.my_dev_id = my_dev_id
        self.my_lora_id = my_lora_id        
        self.vessel_transmit_f = vessel_transmit_f
    
        # warning: pandas is not thread safe (v0.11 onward)
        self.data_lock = threading.Lock() 
        self.rtg_lock = threading.Lock()
        self.df = self.gen_init_df()
    
        # searching for devices observed thus far and other operations can 
        # of course be performed using operations on the DataFrame, but these
        # redundant data structures reduce computational load. what is elegant
        # code on a cluster is torture for a small device.
        self.devices_seen = set() 
        self.who_I_need = set() # probably grammatically incorrect
        self.who_needs_me = set() # it's quiet...
        self.who_does_not_need_me = set() # beleaguered English teachers
        self.who_needs_whom = set() # must be sleeping
        self.who_does_not_need_whom = set()

    def gen_init_df(self): 
        df = pd.DataFrame(columns=PROTOCOL_COLUMNS.keys())
        df.set_index('receipt_ts',inplace=True)
        return df

    def append_batch(self,last_batch):
        if len(last_batch) == 0: return # nothing to do
    
        df0 = pd.concat([self.df] + last_batch,sort=True) # note: sorting requires pandas 0.23.0+
        
        self.data_lock.acquire()
        self.df = df0
        self.data_lock.release()
        #self.in_the_weeds('%d new records added to df, now %d' % (len(last_batch),len(self.df)))
        
    def in_the_weeds(self,S):
        if IN_THE_WEEDS:
            print(self.add_name(' :: '+translate_text(S)))

    def save_run_results(self): 
        df = translate_df(self.df)
        #fn = datetime_now()+self.name+'.csv'
        fn = self.name+'.csv'
        df.to_csv(fn,sep=DF_DELIM)        

    def dump_sets(self):
        print(self.name,'dev seen',self.devices_seen)
        print(self.name,'who I need',self.who_I_need) 
        print(self.name,'who needs me',self.who_needs_me)
        print(self.name,'whod does not need me',self.who_does_not_need_me)
        print(self.name,'who needs whom',self.who_needs_whom)
        print(self.name,'who does not need whom',self.who_does_not_need_whom)

        print(bridge.dev_id2name_mapping)
        
    def seen_msg(self,lmsg):
        skey = lmsg.skey
        all_skeys = set(self.df['skey'])
        return skey in all_skeys

    def update_table_entry(self,idx,col_name,val): # keep for reference!
        if bridge.closing_time: return # avoid threading errors during shutdown
        if idx not in self.df.index or col_name not in self.df.columns: return

        # no checks for pre-existing data
        if not bridge.closing_time:
            self.data_lock.acquire()
            '''
            A reminder:
    
            self.df[var_name][dev_id] = val
    
            produces the pandas warning SettingWithCopyWarning, when
            'A value is trying to be set on a copy of a slice from a DataFrame'
    
            The way to set the value without unintentionally slicing and creating
            a duplicate DataFrame that is then modified (leaving the original
            untouched) is with .loc, as follows:
            '''   
            try:
                self.df.loc[idx,col_name] = val 
            except IndexError:
                log.error('failure! saving progress to file for analysis')
                self.df.to_csv('update-failure-%s-%s-%s.csv' % (idx,col_name,val))
                bridge.closing_time = True
                mopup()  
    
            self.data_lock.release()

    def gleaner_update(self):
        #self.in_the_weeds('gleaner updating')
        self.update_devices_seen()
        self.update_who_I_need()
        self.update_who_needs_me()
        self.update_who_needs_whom()
        #self.in_the_weeds('gleaner update complete')

    def update_ferry_loc(self,dev_id,gps_lat,gps_long,obs_time):
        n = register_or_retrieve_node(dev_id)
        
        # does the node have a timestamp of the last observation of its location?
        if LAST_OBS_VAR_NAME in n._obj.__dict__['location'].__dict__:
            # if we already have a more recent (presumably better) estimate, do nothing
            if obs_time < n.location.last_obs_time:
                return 

        # otherwise update. note that attempting update_var(n,'location.latitude',gps_lat) 
        # produces errors, so do this one manually
        n.location.longitude = gps_long
        n.location.latitude = gps_lat
        n.location.last_obs_time = obs_time
        bridge.rt.flush()

    def update_devices_seen(self):
        dev_seen = set(self.df['sender_dev_id']) \
            | set(self.df['relayer_dev_id']) - set(['',MULTICAST])

        self.data_lock.acquire()
        self.devices_seen = dev_seen
        self.data_lock.release()

        if bridge.HAVE_UNIS:
            for d in self.devices_seen:
                # do we have data? 
                (gps_lat,gps_long,obs_time) = self.estimate_loc(d,now())
                
                if DATA_NOT_FOUND not in [gps_lat,gps_long,obs_time]:
                    update_ferry_loc(d,gps_lat,gps_long,obs_time)
                # if not, do nothing. wait until the next check to try updating.

    def who_has_promoted(self,skey):
        # no errors thrown if no record exists with the skey given    
        return set(self.df[self.df['skey'] == skey]['sender_addr']) - set(['',MULTICAST])

    def has_promoted(self,skey,dev_id):
        return dev_id in self.who_has_promoted(skey)

    def who_has_responded(self,skey): 
        # no errors thrown if no record exists with the skey given 
        rdf = self.df[self.df['ref_skey'] == skey]
        return set(rdf[rdf['bloom_count'] > 0]['init_sender_addr']) \
            | set(rdf[rdf['bloom_count'] == 0]['sender_addr'])- set(['',MULTICAST])

    def has_responded(self,skey,dev_id):
        return dev_id in self.who_has_responded(skey)

    def get_response_dataset_from_query(self,query_skey):  #TODO
        # no errors thrown if no record exists with the skey given 
        rdf = self.df[self.df['ref_skey'] == query_skey]
        if len(rdf) == 0: return set()
        
        sample = rdf.head(1)
        
    def do_I_need(self,dev_id):
        return dev_id in self.who_I_need
        
    def does_dev_need_me(self,dev_id):
        return dev_id in self.who_needs_me

    def update_who_I_need(self): 
        # do not convert to sets. we need the full, ordered DataFrame columns.
        Senders = list(self.df['sender_dev_id']) 
        Relayers = list(self.df['relayer_dev_id'])
        win = {}

        # the number of Relayers is not necessary less than the number of Senders
        if len(Senders) == 0 or len(Relayers) == 0: return

        # as saturation progresses, we hear more amplifications.
        # so use the most recent. hopefully it's not far out, close to the edge. 
        for i in range(len(self.df)):
            sender = Senders[i]
            relayer = Relayers[i]

            if sender == self.my_dev_id or relayer == self.my_dev_id: continue

            if relayer not in win:
                win[relayer] = set()
            win[relayer].add(sender)

        # repackaging for sorting
        win_t = []
        for relayer in win:
            win_t.append([len(win[relayer]),relayer,win[relayer]])

        # the big fish eat the smaller fishes    
        if len(win_t) > 1:
            win_t.sort()
            for i in reversed(range(1,len(win_t),1)): # big fish here
                ti = win_t[i][2]

                # have we devoured all that came before?
                if max(map(lambda x: len(x[2]),win_t[:i])) == 0:
                    break

                for j in range(i): # small fish here
                    tj = win_t[j][2]
                    if len(tj) > 0:
                        if ti | tj == ti: # feeding time
                            win_t[j][2] = set() # will be filtered out shortly
                            self.in_the_weeds('tj (%s) ate ti (%s)' % (win_t[i][1],win_t[j][1]))

        if len(win_t) > 0:
            win_d = {}
            for w in win_t:
                win_d[w[1]] = set(w[2])

            self.data_lock.acquire()
            self.who_I_need = win_d
            self.data_lock.release()

            # update the approprite columns 
            for dev_id in win_d: 
                self.announce_dep(dev_id)

    def announce_dep(self,recipient): 
        if recipient == self.my_dev_id: return
    
        sender = self.my_dev_id
        send_time = now()
        msg_type = MSG_TYPE_RTG

        if recipient in self.who_I_need:
            payload = len(self.who_I_need[recipient])
        else:
            payload = 0

        pkt = '%s/0/%s/%f/%d/%d//%f|' % (recipient,sender,send_time,
            msg_type,payload,FAKE_RSSI_VALUE)
        lmsg = lora_message(pkt)

        if not lmsg.pkt_valid:
            #log.packet_errors(self.add_name('says dependency announcement packet is invalid!'))
            return 

        #log.data_flow(self.add_name('put the announcement packet to %s in queue' % (recipient)))
        self.vessel_transmit_f(lmsg)

        return lmsg

    def get_rtg_results(self,needs_df):
        # get records from the last hour only i.e. the last 3600 seconds
        cutoff = now() - 3600
        df_last_hour = needs_df[needs_df['send_time'] > cutoff]

        # restrict further to routing messages only
        df_rtg = df_last_hour[df_last_hour['msg_type'] == MSG_TYPE_RTG]

        # and only take the most recent message
        all_recip = set(df_rtg['recipient_addr'])- set(['',MULTICAST])
        selected_records = []

        # a walk down memory lane
        for recip_dev in all_recip:
            recip_rec = df_rtg[df_rtg['recipient_addr'] == recip_dev]
            Senders = set(recip_rec['sender_addr'])- set(['',MULTICAST])
            
            for sender in Senders:
                # beware the SettingWithCopyWarning
                recip_sender_rec = recip_rec[recip_rec['sender_addr'] == sender].copy(deep=True)              
                recip_sender_rec.sort_values(by='send_time',ascending=False,inplace=True)
                most_recent_rec = recip_sender_rec.head(1)
                selected_records.append(most_recent_rec)

        if len(selected_records) == 0:
            edf = empty_df(needs_df)
            return set(),set(),set(),edf,edf

        sel_df = pd.concat(selected_records,sort=True) # note: sorting requires pandas 0.23.0+

        # split by classification
        dep_df = sel_df[sel_df['dependency_count'] > 0]
        indep_df = sel_df[sel_df['dependency_count'] == 0]

        # extract
        dependents = set(dep_df['sender_addr'])- set(['',MULTICAST])
        independents = set(indep_df['sender_addr'])- set(['',MULTICAST])
        ambivalents = self.devices_seen - dependents - independents 

        return dependents, independents, ambivalents, dep_df, indep_df

    def update_who_needs_me(self):
        # only determining my needs 
        needs_df = self.df[self.df['recipient_addr'] == self.my_dev_id]
    
        # restrict to recent, unique entries only
        deps, indeps, ambs, dep_df, indep_df = self.get_rtg_results(needs_df)
        deps = set(dep_df['sender_addr'])- set(['',MULTICAST])
        indeps = set(indep_df['sender_addr'])- set(['',MULTICAST])
        ambs = self.devices_seen - deps - indeps 

        self.data_lock.acquire()
        self.who_needs_me = deps
        self.who_does_not_need_me = indeps
        self.data_lock.release()

    def update_who_needs_whom(self):    
        # only determining others' needs 
        needs_df = self.df[self.df['recipient_addr'] != self.my_dev_id]
    
        # restrict to recent, unique entries only
        deps, indeps, ambs, dep_df, indep_df = self.get_rtg_results(needs_df)
        
        dep_d = {}
        for this_dev_id in deps:
            this_dev_dep_df = dep_df[dep_df['recipient_addr'] == this_dev_id]
            this_dev_dep = set(this_dev_dep_df['sender_addr'])- set(['',MULTICAST])
            dep_d[this_dev_id] = this_dev_dep

        indep_d = {}
        for this_dev_id in indeps:
            this_dev_indep_df = indep_df[indep_df['recipient_addr'] == this_dev_id]
            this_dev_indep = set(this_dev_indep_df['sender_addr'])- set(['',MULTICAST])
            indep_d[this_dev_id] = this_dev_indep

        self.data_lock.acquire()
        self.who_needs_whom = dep_d
        self.who_does_not_need_whom = indep_d
        self.data_lock.release()        
        
    def x_needs_y(self,dev_x,dev_y):
        # do we have the data?
        if dev_y in self.who_needs_whom: 
            # is dev_x a dependent of dev_y?
            if dev_x in self.who_needs_whom[dev_y]: 
                return True
                
        return False
    
    def x_indep_y(self,dev_x,dev_y):
        # do we have the data?
        if dev_y in self.who_does_not_need_whom: 
            # is dev_x independent of dev_y?
            if dev_x in self.who_does_not_need_whom[dev_y]: 
                return True
                
        return False
    
    def are_dependent(self,dev_x,dev_y):
        return self.x_needs_y(dev_x,dev_y) or self.x_needs_y(dev_y,dev_x)

    def are_independent(self,dev_x,dev_y):
        return self.x_indep_y(dev_x,dev_y) and self.x_indep_y(dev_y,dev_x)

    def last_var_obs(self,var_name,msg_stack):
        if var_name == 'obs_gps_lat':
            data0 = msg_stack[msg_stack['obs_gps_lat'] != '']
        elif var_name == 'obs_gps_long':
            data0 = msg_stack[msg_stack['obs_gps_long'] != '']
        else:
            data0 = msg_stack[msg_stack['obs_var_name'] == var_name]

        # return the empty DataFrame
        if len(data0) == 0: data0 
        
        # beware the SettingWithCopyWarning
        data = data0.copy(deep=True)
        data.sort_values(by='send_time',ascending=False,inplace=True) 
        last_obs = data.head(1)
        return last_obs

    def retrieve_last_var_obs(self,dev_id,var_name):
        msg_stack = self.df[self.df['obs_dev_id'] == dev_id]
        if len(msg_stack) == 0: return msg_stack
        last_obs = self.last_var_obs(var_name,msg_stack)
        return last_obs
        
    def retrieve_var_from_df(self,dev_id,var_name): 
        last_obs = self.retrieve_last_var_obs(dev_id,var_name)
        if len(last_obs) == 0: return DATA_NOT_FOUND,DATA_NOT_FOUND
        
        if var_name == 'obs_gps_lat':
            retval = last_obs['obs_gps_lat'].item()
        elif var_name == 'obs_gps_long':
            retval = last_obs['obs_gps_long'].item()
        else:
            retval = last_obs['obs_val'].item()
        
        return last_obs['obs_time'].item(), retval

    def estimate_loc(self,dev_id,obs_time): 
        # isolate data pertaining to this device
        this_dev_df = self.df[self.df['obs_dev_id'] == dev_id]
        
        # make a copy to ensure nothing is changed in the main DataFrame
        this_dev_loc = this_dev_df[this_dev_df['obs_gps_lat'] != ''].copy(deep=True)
        if len(this_dev_loc) == 0: return (DATA_NOT_FOUND,DATA_NOT_FOUND, DATA_NOT_FOUND)
        
        # compute differences. find an observation of location as close
        # to obs_time as possible.
        this_dev_loc['diff'] = abs(this_dev_loc['obs_time'] - obs_time)
        min_time_diff = min(list(this_dev_loc['diff']))
        row = this_dev_loc[this_dev_loc['diff'] == min_time_diff].head(1) 
        gps_lat = row['obs_gps_lat'].item()
        gps_long = row['obs_gps_long'].item()
        obs_time = row['obs_time'].item()

        # send back default values in the DataFrame
        if DATA_NOT_FOUND in [gps_lat, gps_long, obs_time]:
            return (DATA_NOT_FOUND, DATA_NOT_FOUND, DATA_NOT_FOUND)

        return (gps_lat,gps_long,obs_time)

    def best_estimate_dataset(self):
        est_d = {}
        all_vars = set(self.df['obs_var_name']) - set(['',DATA_NOT_FOUND])
        
        for var_name in all_vars:
            per_var = []
        
            for dev_id in self.devices_seen | set([self.my_dev_id]):
                obs_time, obs_val = self.retrieve_var_from_df(dev_id,var_name)
                if DATA_NOT_FOUND in [obs_time,obs_val]: continue
                est_lat,est_long = self.estimate_loc(dev_id,obs_time)
                if DATA_NOT_FOUND in [est_lat,est_long]: continue
                        
                D = {'obs_time':[obs_time], 'obs_dev_id':[dev_id],
                    'est_lat':[est_lat], 'est_long':[est_long],
                    'obs_var_name':[var_name], 'obs_val':[obs_val]}
                
                per_var.append(pd.DataFrame(D))

            if len(per_var) > 0: 
                cdf = pd.concat(per_var,sort=True) # note: sorting requires pandas 0.23.0
                cdf.set_index('obs_time',inplace=True)
                #del cdf['obs_var_name'] # don't, in case these DataFrames are merged later
                est_d[var_name] = cdf

        return est_d
        
    def temp_dataset(self):
        rows_with_var = self.df[self.df['obs_var_name'] == 'temp']
        temp_plus_metadata = copy.deepcopy(rows_with_var[['obs_time','obs_gps_lat','obs_gps_long','obs_val']])
        temp_plus_metadata.drop_duplicates(inplace=True)
        return temp_plus_metadata
        
