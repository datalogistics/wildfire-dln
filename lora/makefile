rpi : lora_rpi.h lora_c.cpp
	g++ -Wall -o lora_c -lwiringPi -pthread lora_c.cpp 
 
up : lora_up.h lora_c.cpp
	g++ -Wall -o lora_c  -pthread lora_c.cpp -lmraa

clean :
	rm -rf __pycache__ edit lora_c *.o a.out *.pyc *.csv

