/*******************************************************************************
File: whisper.cpp
Language: C++

This code provides an interface between the low-level methods in whisper_lora.h
and whisper.py. 

This layer serves as a gatekeeper, filtering expected abundant "noise" in the field
from messages of interest. Protocol and switch behaviors are handled in 
whisper.py, but at some point could be encoded in the C++ layer for the sake of
speed. This code could also be combined with whisper_lora.h if a suitable 
replacement for the Standard Template Library is found. For now, whisper.cpp
and whisper.py communicate via sockets.

A number of threads were used to provide resiliency. While the threads do
logically separate production and consumption of messages for ease of 
troubleshooting, assuming the device this code runs on has multiple cores 
(the RPi 3B+ has four), the device can nearly always remain in receive mode,
further avoiding loss of messages. The use of queues adds a cushion as well.

Author: Juliette Zerick (jzerick@iu.edu)
OPEN Lab, Indiana University
*******************************************************************************/

#include "whisper_lora.h" 

#include <netdb.h>
#include <stdbool.h> // for bool type
#include <pthread.h> 
#include <queue> 
#include <net/if.h>

#define PORT_OF_INTEREST    5683    
#define SNOOZE_TIME         5000 // in milliseconds

#define JUST_BROADCASTING   false

#define INCOMING_ADDR       "127.0.0.1"
#define OUTGOING_ADDR       "127.0.0.1"

#define USING_MINION_PROTOCOL true // need to find a better name 

//*******************************************************************************

using namespace std;

pthread_t py_listener_t, lora_listener_t, py_speaker_t, lora_speaker_t;

pthread_mutex_t lora_lock = PTHREAD_MUTEX_INITIALIZER;
pthread_mutex_t inbox_q_lock = PTHREAD_MUTEX_INITIALIZER;
pthread_mutex_t outbox_q_lock = PTHREAD_MUTEX_INITIALIZER;

//Flag for Ctrl-C - from RH examples
volatile sig_atomic_t closing_time = false;

queue<char*> inbox_q;
queue<char*> outbox_q;

void mopup();

char hello[] = "coap://localhost/time";

char* MY_MAC_ADDR = new char[13];

//*******************************************************************************

// TODO properly cite
// https://stackoverflow.com/questions/6767296/how-to-get-local-ip-and-mac-address-c
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

void sig_handler(int sig){
    closing_time = true;
    delay(SNOOZE_TIME);
    mopup();
} 

// used for testing
void broadcast(){
    while(!closing_time){
        transmit_via_lora(hello,strlen(hello));
        delay(SNOOZE_TIME); // in milliseconds
    }
}

// thread-safe removal of a message from the queue
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

// thread-safe push of a message to the queue
void lock_push(queue<char*> &q,char* msg,pthread_mutex_t &lock){
    pthread_mutex_lock(&lock);
    q.push(msg);
    pthread_mutex_unlock(&lock);
}

// slice a set of messages from the socket, then push
void split_lock_push(queue<char*> &q,char* msgs,pthread_mutex_t& lock){
    printf("%s\n",msgs);

    if (! USING_MINION_PROTOCOL){
        lock_push(q,msgs,lock);
        return;
    }

    // TODO retest 
    int total_msg_len = strlen(msgs);
    int idx = 0;
    
    while (idx < total_msg_len){
        if (msgs[idx] == '|'){ // at end of a message
            msgs[idx] = '\0'; // plant a termination character 

            lock_push(q,msgs,lock);
            printf("\tpushed %s\n",msgs);

            idx++;

            if(idx < total_msg_len)
                msgs = &msgs[idx];
	} else {
            idx++; // should be on the first character of the To address of a new message
	}
    }
}

// enacts the black hole protocol
bool not_noise(char* msg){
    if(!USING_MINION_PROTOCOL) 
        return true;    

    int total_msg_len = strlen(msg);

    // is it a multicast message?
    if (msg[0] == '*')
        return true;

    // is it a message meant for this device?
    int i;
    for (i=0;i<12;i++)
        if(msg[i] != MY_MAC_ADDR[i])
            return false;

    return true;
}

// listens on LoRa and adds received messages to the inbox_q. it is assumed that
// the bulk of messages received will not be relevant. not_noise() quickly sorts
// out the noise.
void* lora_listener(void* arg){
    char* msg = new char[MESSAGE_LEN];
    int* msg_len = new int;

    // radio init
    put_in_neutral();
    opmode(OPMODE_RX);

    while(!closing_time){ 
        pthread_mutex_lock(&lora_lock);

        // TODO check to make sure this doesn't introduce a bottleneck
        if(listen_for_lora(msg,msg_len)){
            pthread_mutex_unlock(&lora_lock);

            if(not_noise(msg)){
                lock_push(inbox_q,msg,inbox_q_lock);
                printf("inbox tender: msg pushed\n");
                msg = new char[MESSAGE_LEN];
            }else{
                memset(msg,0,MESSAGE_LEN);
            }

        }else{
            pthread_mutex_unlock(&lora_lock);
        }

        delay(1); //  in milliseconds
    }
    
    close(sock_raw);
    return (void*) NULL;
}

// takes messages from outbox_q and sends them via LoRa when possible
void* lora_speaker(void* arg){
    char* msg;

    while(!closing_time){
        msg = lock_pop(outbox_q,outbox_q_lock);
        
        if(msg != NULL){
            printf("outbox_tender: pulled this out of the queue%s\n",msg);

            pthread_mutex_lock(&lora_lock);
            transmit_via_lora(msg,strlen(msg));
            pthread_mutex_unlock(&lora_lock);
        }

        delay(SNOOZE_TIME); // in milliseconds
    }

    return (void*) NULL;    
}

// listens for messages from whisper.py via socket, then adds them to the outbox_q 
void* py_listener(void* arg){
    // TODO properly cite
    // frrom https://www.binarytides.com/receive-full-data-with-recv-socket-function-in-c/
    int socket_desc;
    struct sockaddr_in server;

    int INCOMING_PORT = *(int*)arg;
    char* msg = new char[MESSAGE_LEN];

    //Create socket
    socket_desc = socket(AF_INET , SOCK_STREAM , 0);
    while (socket_desc == -1){
        //printf("Cinc: trying to establish socket\n");
        socket_desc = socket(AF_INET , SOCK_STREAM , 0);
        delay(SNOOZE_TIME);
    }

    //printf("Cinc: created socket\n");

    server.sin_addr.s_addr = inet_addr(INCOMING_ADDR);
    server.sin_family = AF_INET;
    server.sin_port = htons( INCOMING_PORT );
 
    //Connect to remote server
    while (connect(socket_desc , (struct sockaddr *)&server , sizeof(server)) < 0){
        //printf("Cinc: connect error\n");
        delay(SNOOZE_TIME);
    }
     
    //printf("Cinc: Connected!\n");

    while(!closing_time){ 
        memset(msg,0,MESSAGE_LEN);

        if(recv(socket_desc, msg , MESSAGE_LEN , 0) < 0 || strlen(msg) == 0){
            //printf("Cinc: recv failed");
            delay(SNOOZE_TIME);
            continue;
        }

        printf("Cinc: data received %s \n",msg);

        // insert into outbox queue here
        split_lock_push(outbox_q,msg,outbox_q_lock);

        msg = new char[MESSAGE_LEN];
        delay(SNOOZE_TIME);
    }
    
    //close(sock_raw);
    return (void*) NULL;
}

// pulls messages from the inbox_q and passes them to whisper.py 
void* py_speaker(void* arg){
    // frrom https://www.binarytides.com/receive-full-data-with-recv-socket-function-in-c/
    int socket_desc;
    struct sockaddr_in server;
    int OUTGOING_PORT = *(int*)arg;

    char* msg;

    //Create socket
    socket_desc = socket(AF_INET , SOCK_STREAM , 0);

    while (socket_desc == -1){
        socket_desc = socket(AF_INET , SOCK_STREAM , 0);
        //printf("Cout: trying to establish socket\n");
        delay(SNOOZE_TIME);
    }

    //printf("Cout: created socket\n");

    server.sin_addr.s_addr = inet_addr(OUTGOING_ADDR);
    server.sin_family = AF_INET;
    server.sin_port = htons(OUTGOING_PORT);
 
    //Connect to remote server
    while (connect(socket_desc , (struct sockaddr *)&server , sizeof(server)) < 0){
        //printf("Cout: connect error\n");
        delay(SNOOZE_TIME);
    }

    //printf("Cout: Connected\n");

    while(!closing_time){
        // try to pull from inbox qeuue
        if(inbox_q.size() > 0){
            msg = lock_pop(inbox_q,inbox_q_lock);
        
            printf("Cout: pulled this off the queue %s\n",msg);

            if(send(socket_desc , msg , strlen(msg) , 0) < 0){
                //printf("Cout: send failed");
                delay(SNOOZE_TIME);
                continue;
            }

        //printf("Cout: data sent %s of length %d\n",outgoing_message,strlen(outgoing_message));
        //printf("Cout: data sent %s of length %d\n",msg,strlen(msg));
        delay(SNOOZE_TIME);
        }        
    }

    return (void*) NULL;
}

// cleaning up prior to shutdown
void mopup(){ 
    //pthread_join(port_listener_t, NULL); // insufficient

    if(!JUST_BROADCASTING){
        pthread_kill(py_listener_t, SIGTERM);
        pthread_kill(lora_listener_t, SIGTERM);
        pthread_kill(py_speaker_t, SIGTERM);
        pthread_kill(lora_speaker_t, SIGTERM);
    }
        
    writeReg(RegPaRamp, 0x00);
    put_in_neutral();
    exit(0);
}

int main (int argc, char *argv[]) {
    if(argc != 3){
        printf("usage: sudo ./whisper <incoming port> <outgoing port>\n");
        exit(1);
    }

    get_mac_eth0(MY_MAC_ADDR);

    int py_inc_port = atoi(argv[1]);
    int py_out_port = atoi(argv[2]);

    // it's 0
    //printf("default value? %x\n",readReg(RegPaRamp));

    // for a coordinated, clean shutdown
    signal(SIGINT, sig_handler);

    // set up the RPi/Hat
    wiringPiSetup () ;
    pinMode(ssPin, OUTPUT);
    pinMode(dio0, INPUT);
    pinMode(RST, OUTPUT);

    wiringPiSPISetup(CHANNEL, 500000);

    SetupLoRa();

    if (JUST_BROADCASTING){
        broadcast(); // endless loop until killed
    } // if broadcasting, none of the code below will execute

    // summon the army of threads
    pthread_create(&py_listener_t, NULL, &py_listener, (void*) &py_inc_port);
    pthread_create(&lora_listener_t, NULL, &lora_listener, (void*) NULL);
    pthread_create(&py_speaker_t, NULL, &py_speaker, (void*) &py_out_port);
    pthread_create(&lora_speaker_t, NULL, &lora_speaker, (void*) NULL);
    
    // wait until it's time to shut down
    while(!closing_time){
        delay(SNOOZE_TIME);
    }

    mopup();

    return (0);
}

