rpi : lora_rpi.h lora_c.cpp
	g++ -Wall -o lora_c -D MACHINA=828073 -lwiringPi -pthread lora_c.cpp 
 
up : lora_up.h lora_c.cpp
	g++ -Wall -o lora_c -D MACHINA=858066 -pthread lora_c.cpp -lmraa

clean :
	rm -rf __pycache__ edit lora_c *.o a.out *.pyc *.csv

