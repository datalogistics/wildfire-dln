/******************************************************************************

File: lora_c.cpp
Author: Juliette Zerick (jzerick@iu.edu)
	for the WildfireDLN Project
	OPeN Networks Lab at Indiana University-Bloomington

whisper_c.cpp provides the interface between C drivers (whisper_lora.h) and
higher-level switch behavior coded in Python (whisper.py). Lower-level
functions for interacting with the LoRa transceiver in the LoRa Hat
manufactured by Dragino and passes the data on via sockets to Python
counterparts for processing. 

Last modified: October 7, 2019

*******************************************************************************/

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

#define DEMOING                 true
#define SNOOZE_TIME             1000 // in milliseconds

#define INCOMING_ADDR           "127.0.0.1"
#define OUTGOING_ADDR           "127.0.0.1"

#define USING_MONITOR           false

#define EPS                     0.0001 // 10e-6
#define RESETTING               -1
#define RECEIVING               0
#define TRANSMITTING            1
#define RESET_DURATION          1
#define RECEIVING_DURATION      1
#define TRANSMITTING_DURATION   1

// Method of determining board type came from Yaakov H. via "Hello, I'm Yaakov!"
// in the post "Finding your Operating System version programmatically"
// available at <https://blog.yaakov.online/finding-operating-system-version/>
// last accessed: August 5, 2019

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
        //printf("sysname: %s\n", details.sysname);
        //printf("nodename: %s\n", details.nodename);
        //printf("release: %s\n", details.release);
        //printf("version: %s\n", details.version);
        //printf("machine: %s\n", details.machine);
        
        if (strncmp(details.machine,MACHINE_RPI,20) == 0)
            return CONFIRMED_RPI;
    
        if (strncmp(details.machine,MACHINE_UP_BOARD,20) == 0) 
            return CONFIRMED_UP_BOARD;
    }
    //return BOARD_UNKNOWN;
    return CONFIRMED_UP_BOARD;

}

int board_type = discern_board();

// by default, assume we're on an Up Board
#if board_type == CONFIRMED_RPI
    #include "lora_rpi.h"
#else
    #include "lora_up.h" 
#endif

// Command-line option parsing solution based off an example in the GNU Documentation,
// section "25.2.4 Example of Parsing Long Options with getopt_long" available at
// <https://www.gnu.org/software/libc/manual/html_node/Getopt-Long-Option-Example.html>
// last accessed: August 20, 2019

/* Flags set by '--receiver' and '--transmitter', respectively. */
static int receiver_flag;
static int transmitter_flag;

//*******************************************************************************

pthread_t py_listener_t, lora_listener_t, py_speaker_t, lora_speaker_t, radio_operator_t;

pthread_mutex_t lora_lock = PTHREAD_MUTEX_INITIALIZER;
pthread_mutex_t inbox_q_lock = PTHREAD_MUTEX_INITIALIZER;
pthread_mutex_t outbox_q_lock = PTHREAD_MUTEX_INITIALIZER;

volatile sig_atomic_t closing_time = false;

queue<char*> inbox_q;
queue<char*> outbox_q;

void mopup();

// for broadcasting, optional predefined packets to send out
char notif_pkt[] = "hello";

char* MY_MAC_ADDR = new char[13]; // 12-character string + termination character
int* MY_ORDERING = new int[48]; 

//*******************************************************************************

void spin_until(double ts,double eps){
    while(ts - now() > eps && !closing_time);
}

// Solution from Friek (2011) at StackOverflow
// in response to the following posted question:
// "How to get local IP and MAC address C [duplicate]" available at
// <https://stackoverflow.com/questions/6767296/how-to-get-local-ip-and-mac-address-c>
// last accessed: November 27, 2018
void get_mac_eth0(char* mac_str){
    #define HWADDR_len 6
    int s,i;
    struct ifreq ifr;
    
    s = socket(AF_INET, SOCK_DGRAM, 0);
    strcpy(ifr.ifr_name, "eth0");
    ioctl(s, SIOCGIFHWADDR, &ifr);

    for (i=0; i<HWADDR_len; i++)
        sprintf(&mac_str[i*2],"%02X",((unsigned char*)ifr.ifr_hwaddr.sa_data)[i]);

    mac_str[12]='\0';
}

void get_ordering(int* ord_vec){
    int i;
    char c;

    printf("%d %d\n",transmitter_flag,receiver_flag);

    // for debugging: only transmit or only receive messages
    if (transmitter_flag || receiver_flag){
        for (i=0; i<48; i=i+1){
            MY_ORDERING[i] = transmitter_flag*TRANSMITTING + receiver_flag*RECEIVING;
        }
        return;
    }

    // alternative: assign roles to each minion by checking against the device hostname
    //      or the current user's name. code left if ever needed.

    // Solution from pezy (January 13, 2015), et al. at StackOverflow
    // in response to the following posted question:
    // "Get Computer Name and logged user name" available at
    // <https://stackoverflow.com/questions/27914311/get-computer-name-and-logged-user-name>
    // last accessed: June 29, 2019   
    
    /*char hostname[HOST_NAME_MAX];
    gethostname(hostname, HOST_NAME_MAX);
    //char username[LOGIN_NAME_MAX]; // if ever needed
    //getlogin_r(username, LOGIN_NAME_MAX);

    string s_dave = "dave"; 
    string s_bob = "bob";
    string s_stuart = "stuart";
    string s_jerry = "jerry";

    if (s_stuart.compare(hostname) == 0){ // leader/listener
        for (i=0; i<48; i=i+4){
            MY_ORDERING[i]=0;
            MY_ORDERING[i+1]=0;
            MY_ORDERING[i+2]=0;
            MY_ORDERING[i+3]=0;
        }
        return;
    } else { //if (s_stuart.compare(hostname) == 0){
        for (i=0; i<48; i=i+4){
            MY_ORDERING[i]=1;
            MY_ORDERING[i+1]=1;
            MY_ORDERING[i+2]=1;
            MY_ORDERING[i+3]=1;
        }
        return;
    } */

    for (i=0; i<48; i=i+4){
        c = tolower(MY_MAC_ADDR[i/4]);

        switch(c){
            case '0': MY_ORDERING[i]=0; MY_ORDERING[i+1]=0; MY_ORDERING[i+2]=0; MY_ORDERING[i+3]=0; break;
            case '1': MY_ORDERING[i]=0; MY_ORDERING[i+1]=0; MY_ORDERING[i+2]=0; MY_ORDERING[i+3]=1; break;
            case '2': MY_ORDERING[i]=0; MY_ORDERING[i+1]=0; MY_ORDERING[i+2]=1; MY_ORDERING[i+3]=0; break;
            case '3': MY_ORDERING[i]=0; MY_ORDERING[i+1]=0; MY_ORDERING[i+2]=1; MY_ORDERING[i+3]=1; break;
            case '4': MY_ORDERING[i]=0; MY_ORDERING[i+1]=1; MY_ORDERING[i+2]=0; MY_ORDERING[i+3]=0; break;
            case '5': MY_ORDERING[i]=0; MY_ORDERING[i+1]=1; MY_ORDERING[i+2]=0; MY_ORDERING[i+3]=1; break;
            case '6': MY_ORDERING[i]=0; MY_ORDERING[i+1]=1; MY_ORDERING[i+2]=1; MY_ORDERING[i+3]=0; break;
            case '7': MY_ORDERING[i]=0; MY_ORDERING[i+1]=1; MY_ORDERING[i+2]=1; MY_ORDERING[i+3]=1; break;
            case '8': MY_ORDERING[i]=1; MY_ORDERING[i+1]=0; MY_ORDERING[i+2]=0; MY_ORDERING[i+3]=0; break;
            case '9': MY_ORDERING[i]=1; MY_ORDERING[i+1]=0; MY_ORDERING[i+2]=0; MY_ORDERING[i+3]=1; break;
            case 'a': MY_ORDERING[i]=1; MY_ORDERING[i+1]=0; MY_ORDERING[i+2]=1; MY_ORDERING[i+3]=0; break;
            case 'b': MY_ORDERING[i]=1; MY_ORDERING[i+1]=0; MY_ORDERING[i+2]=1; MY_ORDERING[i+3]=1; break;
            case 'c': MY_ORDERING[i]=1; MY_ORDERING[i+1]=1; MY_ORDERING[i+2]=0; MY_ORDERING[i+3]=0; break;
            case 'd': MY_ORDERING[i]=1; MY_ORDERING[i+1]=1; MY_ORDERING[i+2]=0; MY_ORDERING[i+3]=1; break;
            case 'e': MY_ORDERING[i]=1; MY_ORDERING[i+1]=1; MY_ORDERING[i+2]=1; MY_ORDERING[i+3]=0; break; 
            case 'f': MY_ORDERING[i]=1; MY_ORDERING[i+1]=1; MY_ORDERING[i+2]=1; MY_ORDERING[i+3]=1; break;
        }
    }
}

// Initially the RadioHead libraries were used but were found to be unstable. This
// bit of code survived.
// Author: hallard at GitHub
// Library: RadioHead
// File: RadioHead/examples/raspi/rf95/rf95_client.cpp
// Repo online: <https://github.com/hallard/RadioHead/blob/master/examples/raspi/rf95/rf95_client.cpp>
// Last updated August 20, 2018
// Last accessed: November 27, 2018
void sig_handler(int sig){
    closing_time = true;
    printf("sig handler triggered\n");
    sleep(1);
    mopup();
}

char* lock_pop(queue<char*> &q,pthread_mutex_t &lock){
    char* msg = NULL;

    pthread_mutex_lock(&lock);
    if(q.size() > 0){
        msg = q.front();
        q.pop();
    }
    pthread_mutex_unlock(&lock);
    
    return msg;
}

// leave the parsing to the Python recipient
void lock_push(queue<char*> &q,char* msg,pthread_mutex_t &lock){
    pthread_mutex_lock(&lock);
    q.push(msg);
    pthread_mutex_unlock(&lock);
}

// controls phases of reception, transmission, and reset
void* radio_operator(void* arg){
    double start_time;
    double curr_time;
    int m;
    int last_phase, curr_phase;

    printf("radio_operator: active\n");
   
    last_phase = -1; // start as resetting

    while(!closing_time){
        while(!closing_time && fmod(now(),10) > EPS);
        
        start_time = floor(now());
        curr_time = start_time;
        m = 0;
        
        while(!closing_time && m < 48){
            curr_phase = MY_ORDERING[m];
            
            if (m > 0 && !resetting){ // latter test to handle the first loop
                last_phase = MY_ORDERING[m-1];
            }else{
                last_phase = MY_ORDERING[47];
            }
            
            m++;
            if (last_phase != curr_phase || resetting){
                resetting = true; receiving = false; transmitting = false;
                
                if(curr_phase == RECEIVING){
                    pthread_mutex_lock(&lora_lock);
                    switch_to_receive();
                    pthread_mutex_unlock(&lora_lock);
                }else{
                    pthread_mutex_lock(&lora_lock);
                    switch_to_transmit();
                    pthread_mutex_unlock(&lora_lock);
                }
                
                spin_until(curr_time + RESET_DURATION, EPS);
                curr_time += RESET_DURATION;
            }
            
            if (curr_phase == RECEIVING){
                resetting = false; receiving = true; transmitting = false;
                spin_until(curr_time + RECEIVING_DURATION, EPS);
                curr_time += RECEIVING_DURATION;
                continue;
            }
            
            if (curr_phase == TRANSMITTING){
                resetting = false; receiving = false; transmitting = true;
                spin_until(curr_time + TRANSMITTING_DURATION, EPS);
                curr_time += TRANSMITTING_DURATION;
                continue;    
            }
        }
    }
    return NULL;
}

// listens on LoRa and pushes received messages to the inbox queue
void* lora_listener(void* arg){
    char* msg = new char[MESSAGE_LEN];
    int* msg_len = new int;
    int* rssi_val = new int;
    bool got_something;

    printf("lora_listener: active\n");
    
    while(!closing_time){ 
        while(!receiving); // wait
        
        pthread_mutex_lock(&lora_lock);
        got_something = listen_for_lora(msg,msg_len,rssi_val);
        pthread_mutex_unlock(&lora_lock);

	    if(got_something){
            // overwrite the : and note that the buffer is already filled with 0
            sprintf(&(msg[*msg_len]),"%d|",*rssi_val);
            lock_push(inbox_q,msg,inbox_q_lock);
            printf("lora_listener: message placed in inbox, %s\n",msg);
            
            //delete msg; // left as a reminder: don't do this
            msg = new char[MESSAGE_LEN];
            memset(msg,0,MESSAGE_LEN);
        }

    // TODO check if this is necessary
	//delay_in_ms(1000); //  note: in milliseconds!
    }
    
    return (void*) NULL;
}

void* lora_speaker(void* arg){
    char* msg;
    bool success = false;

    printf("\n\nlora_speaker: active\n");

    while(!closing_time){
        msg = lock_pop(outbox_q,outbox_q_lock);

        if(msg != NULL){
            printf("lora_speaker: received from outbox queue, %s\n",msg);

            while(!transmitting); // wait
            
            pthread_mutex_lock(&lora_lock);
            success = transmit_via_lora(msg,strlen(msg));
            pthread_mutex_unlock(&lora_lock);

	        if(!success){
                printf("lora_speaker: TRANSMIT FAILURE\n"); 
		        lock_push(outbox_q,msg,outbox_q_lock);
	        }
        }

        //delay_in_ms(SNOOZE_TIME); // in milliseconds // TODO check if necessary
    }

    return (void*) NULL;    
}

// py_speaker and py_listener were based on code from Silver Moon at BinaryTides
// "Receive full data with recv socket function in C"
// published online September 8, 2012
// and available online at 
// <https://www.binarytides.com/receive-full-data-with-recv-socket-function-in-c/>
// last accessed: November 27, 2018
void* py_listener(void* arg){
    int socket_desc;
    struct sockaddr_in server;

    int INCOMING_PORT = *(int*)arg;
    char* msg = new char[MESSAGE_LEN];

    //Create socket
    socket_desc = socket(AF_INET , SOCK_STREAM , 0);
    while (socket_desc == -1){
        printf("py_listener: attempting to establish socket\n");
        socket_desc = socket(AF_INET , SOCK_STREAM , 0);
    }

    //ip address of www.msn.com (get by doing a ping www.msn.com at terminal)
    server.sin_addr.s_addr = inet_addr(INCOMING_ADDR);
    server.sin_family = AF_INET;
    server.sin_port = htons( INCOMING_PORT );
 
    //Connect to remote server
    while (connect(socket_desc , (struct sockaddr *)&server , sizeof(server)) < 0){
        printf("py_listener: attempting to connect to Python counterpart\n");
    }
     
    printf("py_listener: active and connected\n");

    while(!closing_time){ 
        memset(msg,0,MESSAGE_LEN);

        if(recv(socket_desc, msg , MESSAGE_LEN , 0) < 0 || strlen(msg) == 0){
            printf("py_listener: recv failed");
            continue;
        }

        printf("py_listener: data received is %s \n",msg);

        // insert into outbox queue here
        lock_push(outbox_q,msg,outbox_q_lock);

        msg = new char[MESSAGE_LEN];
        //delay_in_ms(SNOOZE_TIME); // TODO ditto check
    }
    
    shutdown(socket_desc,2);
    return (void*) NULL;
}

// py_speaker and py_listener were based on code from Silver Moon at BinaryTides
// "Receive full data with recv socket function in C"
// published online September 8, 2012
// and available online at 
// <https://www.binarytides.com/receive-full-data-with-recv-socket-function-in-c/>
// last accessed: November 27, 2018

// pulls messages from the inbox queue, then sends them out via socket
void* py_speaker(void* arg){
    int socket_desc;
    struct sockaddr_in server;
    int OUTGOING_PORT = *(int*)arg;

    char* msg;

    //Create socket
    socket_desc = socket(AF_INET , SOCK_STREAM , 0);

    while (socket_desc == -1){
        socket_desc = socket(AF_INET , SOCK_STREAM , 0);
        printf("py_speaker: attempting to establish socket\n");
    }

    //ip address of www.msn.com (get by doing a ping www.msn.com at terminal)
    server.sin_addr.s_addr = inet_addr(OUTGOING_ADDR);
    server.sin_family = AF_INET;
    server.sin_port = htons(OUTGOING_PORT);
 
    //Connect to remote server
    while (connect(socket_desc , (struct sockaddr *)&server , sizeof(server)) < 0){
        printf("py_speaker: attempting to connect to Python counterpart\n");
    }

    printf("py_speaker: active and connected\n");

    while(!closing_time){
        // try to pull from inbox qeuue
        if(inbox_q.size() > 0){
            msg = lock_pop(inbox_q,inbox_q_lock);

            printf("py_speaker: retrieved from the inbox queue, %s\n",msg);

            if(send(socket_desc , msg , strlen(msg) , 0) < 0){
                printf("py_speaker: send failed");
		        lock_push(inbox_q,msg,inbox_q_lock);
                continue;
            }

        printf("py_speaker: transmitted message %s\n",msg);
        }
    }
    shutdown(socket_desc,2);
    return (void*) NULL;
}

void mopup(){ 
    //pthread_join(port_listener_t, NULL); // insufficient

    pthread_kill(py_listener_t, SIGTERM);
    pthread_kill(lora_listener_t, SIGTERM);
    pthread_kill(py_speaker_t, SIGTERM);
    pthread_kill(lora_speaker_t, SIGTERM);
    pthread_kill(radio_operator_t, SIGTERM);
    printf("mopping up\n");

    writeReg(RegPaRamp, 0x00);
    opmode(OPMODE_SLEEP);
    exit(0);
}

int main (int argc, char *argv[]) {
    // for graceful shutdowns
    signal(SIGINT, sig_handler);

    // for handling command-line parameters; see citation where flags are defined

    int py_inc_port = -1;
    int py_out_port = -1;
    receiver_flag = 0;
    transmitter_flag = 0;
    
    int c; // need an int, not char, to make the library happy
    int option_index = 0; /* getopt_long stores the option index here. */
            
    while (1){
        static struct option long_options[] = {
          /* These options set a flag. */
          {"receiver",      no_argument,        &receiver_flag,     1},
          {"transmitter",   no_argument,        &transmitter_flag,  1},
          {"fin",           no_argument,        0,                  'f'},
          /* These options don't set a flag.
             We distinguish them by their indices. */
          {"in-port",       required_argument,  0,                  'i'},
          {"out-port",      required_argument,  0,                  'o'},
          {0, 0, 0, 0}
        };

        c = getopt_long (argc, argv, "i:o:f",long_options, &option_index);

        /* Detect the end of the options. */
        if (c == -1){
            break;
        }

        // note that there are no warnings for bad inputs, as it is assumed this
        // program will be run via Python subprocess

        switch (c){
            case 0:
                /* If this option set a flag, do nothing else now. */
                break;

            case 'f':
                printf ("at index %d, option -f with value %s\n", option_index,optarg);
                break;

            case 'i': // left for reference
                printf ("at index %d, option -i with value %s\n", option_index,optarg);
                py_inc_port = atoi(optarg);
                break;

            case 'o': // left for reference
                printf ("at index %d,option -o with value %s\n", option_index, optarg);
                py_out_port = atoi(optarg);
                break;

            case '?':
                /* getopt_long already printed an error message. */
                break;

            default:
                abort();
        }
    }

    printf("%d %d\n", transmitter_flag,receiver_flag);

    get_mac_eth0(MY_MAC_ADDR);
    get_ordering(MY_ORDERING);

    setup_hw_interface();
    SetupLoRa();
    put_in_neutral();
    //opmode(OPMODE_RX); // optional: start in receive mode
    //receiving = true; 

    pthread_create(&lora_listener_t, NULL, &lora_listener, (void*) NULL);
    pthread_create(&lora_speaker_t, NULL, &lora_speaker, (void*) NULL);
    pthread_create(&radio_operator_t, NULL, &radio_operator, (void*) NULL);

    pthread_create(&py_listener_t, NULL, &py_listener, (void*) &py_inc_port);
    pthread_create(&py_speaker_t, NULL, &py_speaker, (void*) &py_out_port);

    while(!closing_time){
        sleep(1);
    }

    printf("shutting down\n");
    cleanup_hw_interface();
    
    mopup();

    return 0;
}

