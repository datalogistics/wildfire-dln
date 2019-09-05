rpi : whisper_lora_rpi.h whisper_c_rpi.cpp
	g++ -Wall -o whisper_c -lwiringPi -pthread whisper_c_rpi.cpp 

up : whisper_lora_up.h whisper_c_up.cpp
	g++ -Wall -o whisper_c  -pthread whisper_c_up.cpp -lmraa

clean :
	rm -rf __pycache__ edit whisper_c *.o a.out *.pyc *.csv

run:
	xvfb-run -a python3 whisper.py
