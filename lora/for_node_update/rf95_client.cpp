// rf95_client.cpp
//
// Example program showing how to use RH_RF95 on Raspberry Pi
// Uses the bcm2835 library to access the GPIO pins to drive the RFM95 module
// Requires bcm2835 library to be already installed
// http://www.airspayce.com/mikem/bcm2835/
// Use the Makefile in this directory:
// cd example/raspi/rf95
// make
// sudo ./rf95_client
//
// Contributed by Charles-Henri Hallard based on sample RH_NRF24 by Mike Poublon
// modified by Juliette Zerick (jzerick@iu.edu)

#include <bcm2835.h>
#include <stdio.h>
#include <signal.h>
#include <unistd.h>
#include <time.h>
#include <cstdlib>

#include <RH_RF69.h>
#include <RH_RF95.h>

// Dragino Raspberry PI hat
// see https://github.com/dragino/Lora
#define BOARD_DRAGINO_PIHAT

// Now we include RasPi_Boards.h so this will expose defined 
// constants with CS/IRQ/RESET/on board LED pins definition
#include "../RasPiBoards.h"

// Our RFM95 Configuration 
#define RF_FREQUENCY  915.00
#define RF_GATEWAY_ID 0 
#define RF_NODE_ID    1

#define MAX_ATTEMPTS  5

// Create an instance of a driver
RH_RF95 rf95(RF_CS_PIN, RF_IRQ_PIN);
//RH_RF95 rf95(RF_CS_PIN);

//Flag for Ctrl-C
volatile sig_atomic_t force_exit = false;

int num_sent;
int num_replies;

void sig_handler(int sig)
{
  printf("\n%s Break received, exiting! %d sent, %d replies.\n", 
		  __BASEFILE__,num_sent,num_replies);
  force_exit=true;
}

// counter available for debugging, if required
void send_data(int counter,const char* str_latitude,const char* str_longitude){
  char data[RH_RF95_MAX_MESSAGE_LEN];
  uint8_t len  = RH_RF95_MAX_MESSAGE_LEN;

  memset(data,'\0',RH_RF95_MAX_MESSAGE_LEN); // no effect

  // need some filler to protect the payload, so use a counter
  sprintf(data,"%06d|%s,%s",counter,str_latitude,str_longitude);
  len = strlen(data);

  rf95.send((uint8_t*)data, len);
  rf95.waitPacketSent();
  
  printf("Sent message of length %d to [%d] => ",len,RF_NODE_ID );
  printf("%s",data);
  printf("\n" );
}
  
bool get_message(char* buf, uint8_t* len){
  rf95.setModeRx();
#ifdef RF_IRQ_PIN
  // We have a IRQ pin ,pool it instead reading
  // Modules IRQ registers from SPI in each loop
  rf95.setModeRx();
  
  // Rising edge fired ?
  if (bcm2835_gpio_eds(RF_IRQ_PIN)) {
    // Now clear the eds flag by setting it to 1
    bcm2835_gpio_set_eds(RF_IRQ_PIN);
    //printf("Packet Received, Rising event detect for pin GPIO%d\n", RF_IRQ_PIN);
#endif

    clock_t t_start = clock(), t_now;
    while (!rf95.available()){
      t_now = clock();

      if ((t_now - t_start)/1000. > 1){
        printf("-- no response received in %dms --\n",t_now-t_start);
        return false;
      }
    }

    if (rf95.available()) { 
      // Should be a message for us now
      uint8_t from = rf95.headerFrom();
      uint8_t to   = rf95.headerTo();
      uint8_t id   = rf95.headerId();
      uint8_t flags= rf95.headerFlags();;
      int8_t rssi  = rf95.lastRssi();

      memset(buf,'\0',sizeof(buf));

      if (rf95.recv((uint8_t*)buf, len)) {
        printf("Message acknowledged!\n");

	// buffer issue here
        //printf("Message of length %u from [%u] <= ", *len,RF_GATEWAY_ID);
        //printf("%s",buf); 
      } else {  
        printf("-- receipt failed --\n");
        return false;
      }

      printf("\n");
      return true;
    }
  }
  return false;
}

//Main Function
int main (int argc, const char* argv[] ) {
  static unsigned long last_millis;
  static unsigned long led_blink = 0;

  if (argc != 3){
    printf("usage: sudo ./rf95_client <latitude> <longitude>\n");
    exit(1);
  }

  const char* str_latitude = argv[1];
  const char* str_longitude = argv[2];
  
  signal(SIGINT, sig_handler);
  printf( "%s\n", __BASEFILE__);

  if (!bcm2835_init()) {
    fprintf( stderr, "%s bcm2835_init() Failed\n\n", __BASEFILE__ );
    return 1;
  }
  
  printf( "RF95 CS=GPIO%d", RF_CS_PIN);

#ifdef RF_IRQ_PIN
  printf( ", IRQ=GPIO%d", RF_IRQ_PIN );
  // IRQ Pin input/pull down 
  pinMode(RF_IRQ_PIN, INPUT);
  bcm2835_gpio_set_pud(RF_IRQ_PIN, BCM2835_GPIO_PUD_DOWN);
  // for rising edge detection
#endif
  
#ifdef RF_RST_PIN
  printf( ", RST=GPIO%d", RF_RST_PIN );
  // Pulse a reset on module
  pinMode(RF_RST_PIN, OUTPUT);
  digitalWrite(RF_RST_PIN, LOW );
  bcm2835_delay(150);
  digitalWrite(RF_RST_PIN, HIGH );
  bcm2835_delay(100);
#endif

  if (!rf95.init()) {
    fprintf( stderr, "\nRF95 module init failed, Please verify wiring/module\n" );
    return 1;
  }
  
  printf( "\nRF95 module initialized successfully.\r\n");
#ifdef RF_IRQ_PIN
  rf95.available();
  bcm2835_gpio_ren(RF_IRQ_PIN);
#endif
  rf95.setTxPower(14, false); 
  rf95.setFrequency( RF_FREQUENCY );
  rf95.setThisAddress(RF_NODE_ID);
  rf95.setHeaderFrom(RF_NODE_ID);
  rf95.setPromiscuous(true);
  rf95.setHeaderTo(RF_GATEWAY_ID);  

  printf("RF95 node [%d] initialized successfuly OK @ %3.2fMHz\n", RF_NODE_ID, RF_FREQUENCY );

  last_millis = millis();

  num_sent = 0;
  num_replies = 0;

  // resiliency requires battery preservation, too
  while (!force_exit && (num_sent < MAX_ATTEMPTS)) {
    // Send every 0.5 seconds
    if ( millis() - last_millis > 500 ) {
      last_millis = millis();

      // Send a message to rf95_server
      send_data(num_sent,str_latitude,str_longitude);
      num_sent++;

      char buf[RH_RF95_MAX_MESSAGE_LEN];
      uint8_t len  = sizeof(buf);

      memset(buf,'\0',sizeof(buf));

      char ok[] = "OK";

      if(get_message(buf,&len)){
        num_replies++;

	// optional: stop transmitting post-acknowledgement
        break;

        // optional: kill the loop (stop transmitting) if the reply is, say, "OK"
        //if (strcmp(buf,ok) == 0) {
        //  printf("received OK reply\n");
        //  break; //optional
        }
      }
    }
  
  printf( "\n%s Ending\n", __BASEFILE__ );
  bcm2835_close();
  return 0;
}

