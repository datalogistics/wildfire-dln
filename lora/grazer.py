#from unis import Runtime
# TODO etc

import threading

import whisper # not "from whisper import *" for clarity in function calls
import whisper_globals as wg

class grazer:
    def __init__(self):
        if __name__ != "__main__":        
            #TODO handle opts without opts, set default values here
            print('I am not main?')


        self.w_t = threading.Thread(target=whisper.begin,args=[])         
        self.w_t.daemon = True # so it does with the host process
        self.w_t.start()        

    def make_request(self): # TODO send the message, let the data trickle in
        pass
        
    def render_temperature(self):
        pass
        
    def add_imaginary_friends(self,N):
        pass # create N additional minions
        # get initial GPS from this node
    
    


if __name__ == "__main__":
    if not whisper.handle_opts():
        log.critical('cannot understand command-line arguments, bailing!')
        exit(1)  

    gr = grazer()
  
    while not wg.closing_time:
        time.sleep(SNOOZE_TIME)  
    
