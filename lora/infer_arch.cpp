
#include <stdio.h>
#include <string.h>
#include <sys/utsname.h>
#include <netdb.h>
#include <stdbool.h> // for bool type
#include <pthread.h> 
#include <net/if.h>
#include <math.h> // for fmod; compilation with g++ does not require -lm 
#include <time.h>
#include <unistd.h>
#include <limits.h>
#include <sys/utsname.h>
#include <stdlib.h>
#include <getopt.h>

#include <string> 
#include <queue> 

using namespace std;

#define MACHINE_RPI             "armv7l"
#define MACHINE_UP_BOARD        "x86_64"

#define CONFIRMED_RPI           31 // because
#define CONFIRMED_UP_BOARD      41 // prime 
#define BOARD_UNKNOWN           53 // numbers

int discern_board(){
    struct utsname details;
    int ret = uname(&details);
    
    if (ret == 0){
        // left for reference
        printf("sysname: %s\n", details.sysname);
        printf("nodename: %s\n", details.nodename);
        printf("release: %s\n", details.release);
        printf("version: %s\n", details.version);
        printf("machine: %s\n", details.machine);
        
        if (strncmp(details.machine,MACHINE_RPI,6) == 0){
	    printf("RPI\n");
            return CONFIRMED_RPI;
	}
    
        if (strncmp(details.machine,MACHINE_UP_BOARD,6) == 0) {
	    printf("UP\n");
            return CONFIRMED_UP_BOARD;
	}
    }
    //return BOARD_UNKNOWN;
    return CONFIRMED_UP_BOARD;
}

int board_type = discern_board();

int main(){


return 0;
}
