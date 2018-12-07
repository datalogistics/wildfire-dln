/******************************************************************************

File: whisper.cpp
Author: Juliette Zerick (jzerick@iu.edu)
	for the WildfireDLN Project
	OPEN Networks Lab at Indiana University-Bloomington

whisper.cpp provides the interface between C drivers (whisper_lora.h) and
higher-level switch behavior coded in Python (whisper.py). Lower-level
functions for interacting with the LoRa transceiver in the LoRa Hat
manufactured by Dragino and passes the data on via sockets to Python
counterparts for processing. 

Last modified: December 6, 2018 

*******************************************************************************/

#include "whisper_lora.h"

#include <netdb.h>
#include <stdbool.h> // for bool type
#include <pthread.h> 
#include <queue> 
#include <net/if.h>

#define SNOOZE_TIME         5000 // in milliseconds

#define JUST_BROADCASTING   false // for debugging

#define INCOMING_ADDR       "127.0.0.1"
#define OUTGOING_ADDR       "127.0.0.1"

#define USING_MONITOR       false

//*******************************************************************************

using namespace std;

pthread_t py_listener_t, lora_listener_t, py_speaker_t, lora_speaker_t;

pthread_mutex_t lora_lock = PTHREAD_MUTEX_INITIALIZER;
pthread_mutex_t inbox_q_lock = PTHREAD_MUTEX_INITIALIZER;
pthread_mutex_t outbox_q_lock = PTHREAD_MUTEX_INITIALIZER;

volatile sig_atomic_t closing_time = false;

queue<char*> inbox_q;
queue<char*> outbox_q;

void mopup();

// for broadcasting, optional predefined packets to send out
char notif_pkt[] = "hello";

char* MY_MAC_ADDR = new char[13];

//*******************************************************************************

// Solution from Friek (2011) at StackOverflow
// in response to the following posted question:
// "How to get local IP and MAC address C [duplicate]" available at
// <https://stackoverflow.com/questions/6767296/how-to-get-local-ip-and-mac-address-c>
// last accessed: November 27, 2018
void get_mac_eth0(char* MAC_str){
    #define HWADDR_len 6
    int s,i;
    struct ifreq ifr;
    
    s = socket(AF_INET, SOCK_DGRAM, 0);
    strcpy(ifr.ifr_name, "eth0");
    ioctl(s, SIOCGIFHWADDR, &ifr);

    for (i=0; i<HWADDR_len; i++)
        sprintf(&MAC_str[i*2],"%02X",((unsigned char*)ifr.ifr_hwaddr.sa_data)[i]);

    MAC_str[12]='\0';
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
    delay(SNOOZE_TIME);
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

void lock_push(queue<char*> &q,char* msg,pthread_mutex_t &lock){
    pthread_mutex_lock(&lock);
    q.push(msg);
    pthread_mutex_unlock(&lock);
}

void split_lock_push(queue<char*> &q,char* msgs,pthread_mutex_t& lock){
    printf("split_lock_push: received %s\n",msgs);

    int total_msg_len = strlen(msgs);
    int idx = 0;
    int msg_start = 0;
    
    while (idx < total_msg_len){
        if (msgs[idx] == '|' or msgs[idx] == ':'){ // at end of a message
            msgs[idx] = '\0'; // plant a termination character now
            lock_push(q,&(msgs[msg_start]),lock);
	    msg_start = idx + 1;
	}  
 	idx++; // should be on the first character of the To address of a new message
    }
}

bool has_saturation_req(char*msg){
    unsigned int i;
    
    for(i=0; i<strlen(msg); i++){
        if(msg[i] == '/'){
            if(i + 1 < strlen(msg) && msg[i+1] == '0'){
                return false;}
            else{
                return true;}
	}
    }
                
    return false;
}

bool not_noise(char* msg){
/**
    int total_msg_len = strlen(msg);

    // is it a multicast message?
    if (msg[0] == '*')
        return true;

    // is it a message meant for this device?
    int i;
    for (i=0;i<12;i++){
        if(msg[i] != MY_MAC_ADDR[i]){
            if(has_saturation_req(msg)){
                return true;}
            else{
                return false;}
	}
    }
**/
    return true;
}

// listens on LoRa and pushes received messages to the inbox queue
void* lora_listener(void* arg){
    char* msg = new char[MESSAGE_LEN];
    int* msg_len = new int;
    int* rssi_val = new int;
    bool got_something;
    long int switch_time = 0; // if ever needed

    printf("lora_listener: active\n");
    
    while(!closing_time){ 
        pthread_mutex_lock(&lora_lock);
	switch_time = switch_to_receive();
        got_something = listen_for_lora(msg,msg_len,rssi_val);
	pthread_mutex_unlock(&lora_lock);

	if(got_something){
            // overwrite the : and note that the buffer is already filled with 0
            sprintf(&(msg[*msg_len-1]),"%d|",*rssi_val);
 
  	    if(not_noise(msg)){
                split_lock_push(inbox_q,msg,inbox_q_lock);
                printf("lora_listener: message placed in inbox, %s\n",msg);
		//delete msg; // left as a reminder: don't do this
                msg = new char[MESSAGE_LEN];
            }else{
                memset(msg,0,MESSAGE_LEN);
            }
	}

	delay(1000); //  note: in milliseconds!
    }
    
    return (void*) NULL;
}

// TODO add a socket to listen on for datagrams, will not perform error checking
// takes messages from the outbox queue and transmits them via LoRa when possible
void* lora_speaker(void* arg){
    char* msg;
    long int switch_time = 0; // if ever needed
    bool success = false;

    printf("lora_speaker: active\n");

    while(!closing_time){
        msg = lock_pop(outbox_q,outbox_q_lock);
        
        if(msg != NULL){
            printf("lora_speaker: received from outbox queue %s\n",msg);

            pthread_mutex_lock(&lora_lock);
	    switch_time = switch_to_transmit();
            success = transmit_via_lora(msg,strlen(msg));
            pthread_mutex_unlock(&lora_lock);

	    if(!success){
                printf("lora_speaker: TRANSMIT FAILURE\n"); 
		lock_push(outbox_q,msg,outbox_q_lock);
	    }
        }

        delay(SNOOZE_TIME); // in milliseconds
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
        delay(SNOOZE_TIME);
    }

    //ip address of www.msn.com (get by doing a ping www.msn.com at terminal)
    server.sin_addr.s_addr = inet_addr(INCOMING_ADDR);
    server.sin_family = AF_INET;
    server.sin_port = htons( INCOMING_PORT );
 
    //Connect to remote server
    while (connect(socket_desc , (struct sockaddr *)&server , sizeof(server)) < 0){
        printf("py_listener: attempting to connect to Python counterpart\n");
        delay(SNOOZE_TIME);
    }
     
    printf("py_listener: active and connected\n");

    while(!closing_time){ 
        memset(msg,0,MESSAGE_LEN);

        if(recv(socket_desc, msg , MESSAGE_LEN , 0) < 0 || strlen(msg) == 0){
            printf("py_listener: recv failed");
            delay(SNOOZE_TIME);
            continue;
        }

        printf("py_listener: data received %s \n",msg);

        // insert into outbox queue here
        lock_push(outbox_q,msg,outbox_q_lock);

        msg = new char[MESSAGE_LEN];
        delay(SNOOZE_TIME);
    }
    
    shutdown(socket_desc,2);
    return (void*) NULL;
}

// pulls messages from the inbox queue, then sends them out via socket

// py_speaker and py_listener were based on code from Silver Moon at BinaryTides
// "Receive full data with recv socket function in C"
// published online September 8, 2012
// and available online at 
// <https://www.binarytides.com/receive-full-data-with-recv-socket-function-in-c/>
// last accessed: November 27, 2018
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
        delay(SNOOZE_TIME);
    }

    //ip address of www.msn.com (get by doing a ping www.msn.com at terminal)
    server.sin_addr.s_addr = inet_addr(OUTGOING_ADDR);
    server.sin_family = AF_INET;
    server.sin_port = htons(OUTGOING_PORT);
 
    //Connect to remote server
    while (connect(socket_desc , (struct sockaddr *)&server , sizeof(server)) < 0){
        printf("py_speaker: attempting to connect to Python counterpart\n");
        delay(SNOOZE_TIME);
    }

    printf("py_speaker: active and connected\n");

    while(!closing_time){
        // try to pull from inbox qeuue
        if(inbox_q.size() > 0){
            msg = lock_pop(inbox_q,inbox_q_lock);
        
            printf("py_speaker: retrieved from the inbox queue %s\n",msg);

            if(send(socket_desc , msg , strlen(msg) , 0) < 0){
                printf("py_speaker: send failed");
                delay(SNOOZE_TIME);
		
		lock_push(inbox_q,msg,inbox_q_lock);
                continue;
            }

        printf("py_speaker: sent message %s of length %d\n",msg,strlen(msg));
        delay(SNOOZE_TIME);
        }        
    }
    shutdown(socket_desc,2);
    return (void*) NULL;
}

void broadcast(){
    long int switch_time = 0; // if ever needed
    bool success = false;

    while(!closing_time){
        pthread_mutex_lock(&lora_lock);
	switch_time = switch_to_transmit();
        success = transmit_via_lora(notif_pkt,strlen(notif_pkt));
        pthread_mutex_unlock(&lora_lock);

        if(!success){
            printf("could not broadcast!\n");
	}

	delay(SNOOZE_TIME); // in milliseconds
    }
}

void mopup(){ 
    //pthread_join(port_listener_t, NULL); // insufficient

    if(!JUST_BROADCASTING){
        pthread_kill(py_listener_t, SIGTERM);
        pthread_kill(lora_listener_t, SIGTERM);
        pthread_kill(py_speaker_t, SIGTERM);
        pthread_kill(lora_speaker_t, SIGTERM);
    }
        
    writeReg(RegPaRamp, 0x00);
    opmode(OPMODE_SLEEP);
    exit(0);
}

int main (int argc, char *argv[]) {
    int py_inc_port = -1;
    int py_out_port = -1;

    if(argc != 3 && !JUST_BROADCASTING){
        printf("usage: sudo ./whisper <incoming port> <outgoing port>\n");
        exit(1);
    }
    
    if(!JUST_BROADCASTING){
        py_inc_port = atoi(argv[1]);
        py_out_port = atoi(argv[2]);
    }

    get_mac_eth0(MY_MAC_ADDR);

    // for graceful shutdowns
    signal(SIGINT, sig_handler);

    wiringPiSetup () ;
    pinMode(ssPin, OUTPUT);
    pinMode(dio0, INPUT);
    pinMode(RST, OUTPUT);

    wiringPiSPISetup(CHANNEL, 500000);

    SetupLoRa();
    put_in_neutral();
    opmode(OPMODE_RX);
    listening = true;

    // defined in whisper_lora.h
    if(USING_MONITOR){
        pthread_create(&monitor_t, NULL, &monitor, (void*) NULL);
    }

    pthread_create(&lora_listener_t, NULL, &lora_listener, (void*) NULL);
    pthread_create(&lora_speaker_t, NULL, &lora_speaker, (void*) NULL);

    if (JUST_BROADCASTING){
        broadcast(); // endless loop until manually killed
    } // hence if broadcasting, none of the code below will execute


    pthread_create(&py_listener_t, NULL, &py_listener, (void*) &py_inc_port);
    pthread_create(&py_speaker_t, NULL, &py_speaker, (void*) &py_out_port);

    while(!closing_time){
        delay(SNOOZE_TIME);
    }

    printf("shutting down\n");
    mopup();
    return 0;
}
