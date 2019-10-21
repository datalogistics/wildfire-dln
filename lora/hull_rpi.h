/******************************************************************************

File: hull_rpip.h
Author: Juliette Zerick (jzerick@iu.edu)
        for the WildfireDLN Project
        OPeN Networks Lab at Indiana University-Bloomington
        +
        Dragino, manufacturer of the LoRa Hat

This C header file contains low-level methods to interact with the LoRa Hat v1.4.
The bulk of the code is Copyright (c) 2018 Dragino, the hardware manufacturer's
transceiver example code available via Github:
https://github.com/dragino/rpi-lora-tranceiver/blob/master/dragino_lora_app/main.c

This is the Raspberry Pi-specific backend, relying on the WiringPi libraries.
Another version, designed for the Up Board, utilizes the equivalent MRAA.

This work is part of the WildfireDLN project. It and other tools are available in
GitHub: 
https://github.com/datalogistics/wildfire-dln

Last modified: October 21, 2019

 *******************************************************************************/

#include "bilge.h"

#include <wiringPi.h>
#include <wiringPiSPI.h>

static const int CHANNEL = 0;

enum sf_t { SF7=7, SF8, SF9, SF10, SF11, SF12 };

// physical pin mapping
int ssPin = 6;
int dio0  = 7;
int RST   = 0;

//*******************************************************************************

void delay_in_ms(int t){
    delay(t);
}

int setup_hw_interface(){
    wiringPiSetup () ;
    pinMode(ssPin, OUTPUT);
    pinMode(dio0, INPUT);
    pinMode(RST, OUTPUT);

    wiringPiSPISetup(CHANNEL, 500000);
    return 0;
}

// nothing to do on the RPi, but something to do on the Up Board
int cleanup_hw_interface(){
    return 0; 
}

void selectreceiver(){
    digitalWrite(ssPin, LOW);
}

void unselectreceiver(){
    digitalWrite(ssPin, HIGH);
}

byte readReg(byte addr){
    unsigned char spibuf[2];

    selectreceiver();
    spibuf[0] = addr & 0x7F;
    spibuf[1] = 0x00;
    wiringPiSPIDataRW(CHANNEL, spibuf, 2);
    unselectreceiver();

    return spibuf[1];
}

void writeReg(byte addr, byte value){
    unsigned char spibuf[2];

    spibuf[0] = addr | 0x80;
    spibuf[1] = value;
    selectreceiver();
    wiringPiSPIDataRW(CHANNEL, spibuf, 2);

    unselectreceiver();
}

static void opmode (uint8_t mode) {
    writeReg(REG_OPMODE, (readReg(REG_OPMODE) & ~OPMODE_MASK) | mode);
}

static void opmodeLora() {
    uint8_t u = OPMODE_LORA;
    if (sx1272 == false)
        u |= 0x8;   // TBD: sx1276 high freq
    writeReg(REG_OPMODE, u);
}

void SetupLoRa(){
    // if so desired, timers to measure time to reset
    //double start,end;
    //start = now();

    digitalWrite(RST, HIGH);
    delay_in_ms(100);
    digitalWrite(RST, LOW);
    delay_in_ms(100);

    byte version = readReg(REG_VERSION);

    if (version == 0x22) {
        // sx1272
        printf("SX1272 detected, starting.\n");
        sx1272 = true;
    } else {
        // sx1276?
        digitalWrite(RST, LOW);
        delay_in_ms(100);
        digitalWrite(RST, HIGH);
        delay_in_ms(100);
        version = readReg(REG_VERSION);
        if (version == 0x12) {
            // sx1276
            printf("SX1276 detected, starting.\n");
            sx1272 = false;
        } else {
            printf("Unrecognized transceiver.\n");
            //exit(1);
        }
        
    sx1272 = false; // because the Dragino LoRa GPS Hat uses sx1276
    }

    opmode(OPMODE_SLEEP);

    // set frequency
    uint64_t frf = ((uint64_t)freq << 19) / 32000000;
    writeReg(REG_FRF_MSB, (uint8_t)(frf>>16) );
    writeReg(REG_FRF_MID, (uint8_t)(frf>> 8) );
    writeReg(REG_FRF_LSB, (uint8_t)(frf>> 0) );

    writeReg(REG_SYNC_WORD, 0x34); // LoRaWAN public sync word

    if (sx1272) {
        if (sf == SF11 || sf == SF12) {
            writeReg(REG_MODEM_CONFIG,0x0B);
        } else {
            writeReg(REG_MODEM_CONFIG,0x0A);
        }
        writeReg(REG_MODEM_CONFIG2,(sf<<4) | 0x04);
    } else {
        if (sf == SF11 || sf == SF12) {
            writeReg(REG_MODEM_CONFIG3,0x0C);
        } else {
            writeReg(REG_MODEM_CONFIG3,0x04);
        }
        writeReg(REG_MODEM_CONFIG,0x72);
        writeReg(REG_MODEM_CONFIG2,(sf<<4) | 0x04);
    }

    if (sf == SF10 || sf == SF11 || sf == SF12) {
        writeReg(REG_SYMB_TIMEOUT_LSB,0x05);
    } else {
        writeReg(REG_SYMB_TIMEOUT_LSB,0x08);
    }
    writeReg(REG_MAX_PAYLOAD_LENGTH,0x80);
    writeReg(REG_PAYLOAD_LENGTH,PAYLOAD_LENGTH);
    writeReg(REG_HOP_PERIOD,0xFF);
    writeReg(REG_FIFO_ADDR_PTR, readReg(REG_FIFO_RX_BASE_AD));
    writeReg(REG_LNA, LNA_MAX_GAIN);

    //end = now();
    //printf("Setup elapsed time=%f seconds\n",end-start);
}

// the minimum steps required to reset the transceiver, to switch between
// transmit and receive modes successfully
void ReSetupLoRa() {
    // if so desired, timers to measure time to reset
    //double start,end;
    //start = now();
    
    digitalWrite(RST, HIGH);
    delay_in_ms(100);
    digitalWrite(RST, LOW);
    delay_in_ms(100);

    digitalWrite(RST, LOW);
    delay_in_ms(100);
    digitalWrite(RST, HIGH);
    delay_in_ms(100);

    sx1272 = false;
    opmode(OPMODE_SLEEP);

    // set frequency

    uint64_t frf = ((uint64_t)freq << 19) / 32000000;
    writeReg(REG_FRF_MSB, (uint8_t)(frf>>16) );
    writeReg(REG_FRF_MID, (uint8_t)(frf>> 8) );
    writeReg(REG_FRF_LSB, (uint8_t)(frf>> 0) );

    writeReg(REG_SYNC_WORD, 0x34); // LoRaWAN public sync word

    if (!sx1272) {
        if (sf == SF11 || sf == SF12) {
            writeReg(REG_MODEM_CONFIG3,0x0C);
        } else {
            writeReg(REG_MODEM_CONFIG3,0x04);
        }
        writeReg(REG_MODEM_CONFIG,0x72);
        writeReg(REG_MODEM_CONFIG2,(sf<<4) | 0x04);
    }

    if (sf == SF10 || sf == SF11 || sf == SF12) {
        writeReg(REG_SYMB_TIMEOUT_LSB,0x05);
    } else {
        writeReg(REG_SYMB_TIMEOUT_LSB,0x08);
    }

    writeReg(REG_MAX_PAYLOAD_LENGTH,0x80);
    writeReg(REG_PAYLOAD_LENGTH,PAYLOAD_LENGTH);
    writeReg(REG_HOP_PERIOD,0xFF);
    writeReg(REG_FIFO_ADDR_PTR, readReg(REG_FIFO_RX_BASE_AD));
    writeReg(REG_LNA, LNA_MAX_GAIN);

    //end = now();
    //printf("ReSetup elapsed time=%f seconds\n",end-start);
}

void put_in_neutral(){
    opmodeLora();
    opmode(OPMODE_STANDBY);
}

long int switch_to_receive(){
    long int switch_time = -1;

    if (!receiving){
        ReSetupLoRa();

        // note: this block of code must remain in the conditional
        // otherwise we won't receive
        put_in_neutral();
        opmode(OPMODE_RX);
        
        printf(">> now in RECEIVE mode <<\n");
    }

    return switch_time;
}

long int switch_to_transmit(){
    long int switch_time = -1;

    if (!transmitting){
        ReSetupLoRa();

        // keep this in the conditional, like above
        put_in_neutral();
	
	    /* left as a reminder to not activate now. the activation function 
	       will be called just prior to transmission in the sending function.
	       it must be invoked in that order or no transmission will occur.*/
        //opmode(OPMODE_TX); 
        
        printf("<< now in TRANSMIT mode >>\n");
    }
    return switch_time;
}

bool lora_recv(char* msg_buf, int* msg_len) {
    // clear rxDone
    writeReg(REG_IRQ_FLAGS, 0x40);
    int irqflags = readReg(REG_IRQ_FLAGS);

    //  payload crc: 0x20
    if((irqflags & 0x20) == 0x20){
        printf("CRC error\n");
        writeReg(REG_IRQ_FLAGS, 0x20);
        return false;
    } else {
        memset(msg_buf,0,MESSAGE_LEN);
        *msg_len = 0;

        byte currentAddr = readReg(REG_FIFO_RX_CURRENT_ADDR);
        byte receivedCount = readReg(REG_RX_NB_BYTES);
        *msg_len = (int)receivedCount;

        writeReg(REG_FIFO_ADDR_PTR, currentAddr);
        for(int i = 0; i < receivedCount; i++){
            msg_buf[i] = (char)readReg(REG_FIFO);

           /* occasionally the message received overflows the buffer, typically
              when two packets are received back-to-back and get "glued" 
              together. attempting to read it all in will kill the transceiver. 
              so we salvage what we can--what should be a full packet--but
              toss the remainder. unusually, increasing the MESSAGE_LEN
              does not avert this explosion. when msg_len=255, there is a 
              bomb to defuse, regardless of how large a container it is 
              supposed to reside in.
           */
	       if (((*msg_len) == 255) && (i == 200)){
		            printf("ditching!\n\n\n\n");
                    break;
		   }
	    }
    }

    return true;
}

bool listen_for_lora(char* msg_buf, int* msg_len, int* rssi_val) {
    long int SNR;
    int rssicorr;

    // cannot listen while in transmit mode
    if(!receiving){
	return false;
    }


    if(digitalRead(dio0) == 1){
	printf("1\n");

        if(lora_recv(msg_buf,msg_len)) {
		printf("2\n");
	    if ((*msg_len) == 0){
	        return false;
	    }
		
            byte value = readReg(REG_PKT_SNR_VALUE);
            if( value & 0x80 ){ // The SNR sign bit is 1 
                // Invert and divide by 4
                value = ( ( ~value + 1 ) & 0xFF ) >> 2;
                SNR = -value;
            } else {
                // Divide by 4
                SNR = ( value & 0xFF ) >> 2;
            }
            
            if (sx1272) {
                rssicorr = 139;
            } else {
                rssicorr = 157;
            }

            printf("\tPacket RSSI: %d, ", readReg(0x1A)-rssicorr);
            printf("\tRSSI: %d, ", readReg(0x1B)-rssicorr);
            printf("\tSNR: %li, ", SNR);
            printf("\tLength: %i", *msg_len);
            printf("\tPayload: %s\n", msg_buf);

            *rssi_val = readReg(0x1A)-rssicorr;
	   
            return true;
        } // received a message
    } // dio0=1
    
    return false;
}

static void configPower (int8_t pw) {
    if (sx1272 == false) {
        // no boost used for now
        if(pw >= 17) {
            pw = 15;
        } else if(pw < 2) {
            pw = 2;
        }
        // check board type for BOOST pin
        writeReg(RegPaConfig, (uint8_t)(0x80|(pw&0xf)));
        writeReg(RegPaDac, readReg(RegPaDac)|0x4);

    } else {
        // set PA config (2-17 dBm using PA_BOOST)
        if(pw > 17) {
            pw = 17;
        } else if(pw < 2) {
            pw = 2;
        }
        writeReg(RegPaConfig, (uint8_t)(0x80|(pw-2)));
    }
}

void clear_buf(byte* buf,int L){
    memset(buf,0,L);
}

static void write_buf_to_lora_reg(byte addr, byte *value, byte len) { 
    unsigned char spibuf[MESSAGE_LEN];  
    memset(buf,0,MESSAGE_LEN);

    spibuf[0] = addr | 0x80;   
    for (int i = 0; i < len; i++) {  
        spibuf[i + 1] = value[i];   
    }                                                                                                   
    selectreceiver();  
    wiringPiSPIDataRW(CHANNEL, spibuf, len + 1); 
    unselectreceiver();                                                        
}

bool transmit_via_lora(char* msg, int msg_len) {
    // cannot transmit while in receive mode
    if(!transmitting){
	 return false;
    }

    put_in_neutral(); 
	
    byte* frame = (byte*) msg;
    byte datalen = (byte)msg_len;

    writeReg(RegPaRamp, (readReg(RegPaRamp) & 0xF0) | 0x08); // set PA ramp-up time 50 uSec

    configPower(23);

    // set the IRQ mapping DIO0=TxDone DIO1=NOP DIO2=NOP
    writeReg(RegDioMapping1, MAP_DIO0_LORA_TXDONE|MAP_DIO1_LORA_NOP|MAP_DIO2_LORA_NOP);
    // clear all radio IRQ flags
    writeReg(REG_IRQ_FLAGS, 0xFF);
    // mask all IRQs but TxDone
    writeReg(REG_IRQ_FLAGS_MASK, ~IRQ_LORA_TXDONE_MASK);

    // initialize the payload size and address pointers
    writeReg(REG_FIFO_TX_BASE_AD, 0x00);
    writeReg(REG_FIFO_ADDR_PTR, 0x00);
    writeReg(REG_PAYLOAD_LENGTH, datalen);

    // download buffer to the radio FIFO
    write_buf_to_lora_reg(REG_FIFO, frame, datalen);
    
    // now we actually start the transmission
    opmode(OPMODE_TX); // activate

    printf("sending: %s\n", frame);

    writeReg(RegPaRamp, 0x00);
    delay_in_ms(300); // need this to ensure payload delivery and decent RSSI  
    
    return true; 
}

