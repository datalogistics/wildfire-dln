## Instructions

Assuming stuart is broadcasting and bob is roaming:   

Login to `bob` as `pi`   
Then `cd /home/pi/repos/minionfest/whisper`   
Run `make`   
open `whisper.py` and  make sure these parameters are set this way:      
	`BROADCASTING = False`     
	`RUNNING_BODILESS = False`     
	`USING_C_HANDLER = True`    
	`USING_UNIS = False`          
Run `nohup ./run_whisper.sh`   
close terminal without interrupting the running process   

Login to `stuart` as `pi`   
Then `cd /home/pi/minionfest/whisper`   
Run `make`   
Open `whisper.py` and make sure the parameters are set this way:      
	`BROADCASTING = True`   
	`RUNNING_BODILESS = False`   
	`USING_C_HANDLER = True`   
	`USING_UNIS = False`      
Run `python3 whisper.py`   

Observe.
