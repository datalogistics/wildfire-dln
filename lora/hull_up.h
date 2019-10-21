/******************************************************************************

File: hull_up.h
Author: Juliette Zerick (jzerick@iu.edu)
        for the WildfireDLN Project
        OPeN Networks Lab at Indiana University-Bloomington
        +
        Dragino, manufacturer of the LoRa Hat

This C header file contains low-level methods to interact with the LoRa Hat v1.4.
The bulk of the code is Copyright (c) 2018 Dragino, the hardware manufacturer's
transceiver example code available via Github:
https://github.com/dragino/rpi-lora-tranceiver/blob/master/dragino_lora_app/main.c

This is the Up Board-specific backend, relying on the MRAA libraries. 
Another version, designed for the Raspberry Pi, utilizes the equivalent WiringPi.

This work is part of the WildfireDLN project. It and other tools are available in
GitHub: 
https://github.com/datalogistics/wildfire-dln

Last modified: October 21, 2019

 *******************************************************************************/

#include </usr/include/mraa.h>
#include </usr/include/mraa/gpio.h>
#include </usr/include/mraa/spi.h>

volatile sig_atomic_t receiving = false;
volatile sig_atomic_t transmitting = false;
volatile sig_atomic_t resetting = true;

typedef bool boolean;
typedef unsigned char byte;

static const int SPI_BUS = 0;
static const int SPI_FREQ = 500000; 

bool sx1272 = false; // the LoRa GPS Hat has sx1276

// physical pins 
int dio0  = 7; // WiringPi pin 7, physical pin 7
int ssPin = 22; // WiringPi pin 6, physical pin 22
int RST   = 11; // WiringPi pin 0, physical pin 11

mraa_gpio_context gpio_ssPin, gpio_dio0, gpio_RST;
mraa_spi_context spi;

char message[MESSAGE_LEN];

enum sf_t { SF7=7, SF8, SF9, SF10, SF11, SF12 };

// Set spreading factor (SF7 - SF12)
sf_t sf = SF12;

// Set center frequency
uint32_t  freq = 915000000; // in Mhz! (EU=868.1, US=915.0)

byte hello[32] = "hello\0";

//*******************************************************************************

void delay_in_ms(int t){
    usleep(t*1000);
}

int setup_hw_interface(){
    mraa_result_t status = MRAA_SUCCESS;
    mraa_init();

    gpio_ssPin = mraa_gpio_init(ssPin);
    if (gpio_ssPin == NULL) {
        fprintf(stderr, "Failed to initialize GPIO %d\n", ssPin);
        mraa_deinit();
        return EXIT_FAILURE;
    }

    status = mraa_gpio_dir(gpio_ssPin, MRAA_GPIO_OUT);
    if (status != MRAA_SUCCESS) {
        goto err_exit;
    }

    gpio_dio0 = mraa_gpio_init(dio0);
    if (gpio_dio0 == NULL) {
        fprintf(stderr, "Failed to initialize GPIO %d\n", dio0);
        mraa_deinit();
        return EXIT_FAILURE;
    }

    status = mraa_gpio_dir(gpio_dio0, MRAA_GPIO_IN);
    if (status != MRAA_SUCCESS) {
        goto err_exit;
    }

    gpio_RST = mraa_gpio_init(RST);
    if (gpio_RST == NULL) {
        fprintf(stderr, "Failed to initialize GPIO %d\n", RST);
        mraa_deinit();
        return EXIT_FAILURE;
    }

    status = mraa_gpio_dir(gpio_RST, MRAA_GPIO_OUT);
    if (status != MRAA_SUCCESS) {
        goto err_exit;
    }

    spi = mraa_spi_init(SPI_BUS);
    if (spi == NULL) {
        fprintf(stderr, "Failed to initialize SPI\n");
        mraa_deinit();
        return EXIT_FAILURE;
    }

    /* set SPI frequency */
    status = mraa_spi_frequency(spi, SPI_FREQ);
    if (status != MRAA_SUCCESS)
        goto err_exit;

    /* set big endian mode */
    status = mraa_spi_lsbmode(spi, 0);
    if (status != MRAA_SUCCESS) {
        goto err_exit;
    }

    // how large is the word written to SPI? 2 hex = 8 bits
    status = mraa_spi_bit_per_word(spi, 8); // so spi_buf has len 1
    
    if (status != MRAA_SUCCESS) {
        fprintf(stdout, "Failed to set SPI Device to 8Bit mode\n");
        //fprintf(stdout, "Failed to set SPI Device to 16Bit mode\n");
        goto err_exit;
    }

    return EXIT_SUCCESS;

err_exit:
    mraa_result_print(status);

    /* stop spi */
    mraa_spi_stop(spi);

    /* deinitialize mraa for the platform (not needed most of the times) */
    mraa_deinit();

    return EXIT_FAILURE;    
    }
    
int cleanup_hw_interface(){
    /* stop spi */
    mraa_spi_stop(spi);

    /* deinitialize mraa for the platform (not needed most of the times) */
    mraa_deinit();

    return EXIT_SUCCESS;
}

// Solution from Ciro Santilli (2016) at StackOverflow
// in response to the following posted question:
// "How to measure time in milliseconds using ANSI C?" available at
// <https://stackoverflow.com/questions/361363/how-to-measure-time-in-milliseconds-using-ansi-c>
// last accessed: July 17, 2019
double now() {
    struct timespec ts;
    timespec_get(&ts, TIME_UTC);
    double as_frac = ts.tv_sec + ts.tv_nsec/1000000000.;
    return as_frac;
}

void selectreceiver(){
    mraa_result_t status = MRAA_SUCCESS;
    status = mraa_gpio_write(gpio_ssPin, LOW);
    if (status != MRAA_SUCCESS){
         printf("receiver selection failure\n");
    }
    if(mraa_gpio_read(gpio_ssPin) == HIGH){
        printf("select fail!\n");
    }
}

void unselectreceiver(){
    mraa_result_t status = MRAA_SUCCESS;
    status = mraa_gpio_write(gpio_ssPin, HIGH);
    if (status != MRAA_SUCCESS){
         printf("receiver unselection failure\n");
    }

    if(mraa_gpio_read(gpio_ssPin) == LOW){
        printf("unselect fail!\n");
    }
}

byte readReg(byte addr){
    uint8_t spibuf[2];
    uint8_t rxbuf[2];

    spibuf[0] = addr & 0x7F;
    spibuf[1] = 0x00;

    selectreceiver();
    mraa_spi_transfer_buf(spi, spibuf,rxbuf, 2);
    unselectreceiver();

    return rxbuf[1];
}

void writeReg(byte addr, byte value){
    uint8_t spibuf[2];
    uint8_t rxbuf[2];

    spibuf[0] = addr | 0x80;
    spibuf[1] = value;
    
    selectreceiver();
    mraa_spi_transfer_buf(spi, spibuf,(uint8_t*)rxbuf, 2);

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

    mraa_result_t status = MRAA_SUCCESS;
    status = mraa_gpio_write(gpio_RST, HIGH);
    if (status != MRAA_SUCCESS){
         printf("reset failure (1)\n");
    }

    delay_in_ms(100);

    status = mraa_gpio_write(gpio_RST, LOW);
    if (status != MRAA_SUCCESS){
         printf("reset failure (2)\n");
    }

    delay_in_ms(100);

    byte version = readReg(REG_VERSION);

    if (version == 0x22) {
        // sx1272
        printf("SX1272 detected, starting.\n");
        sx1272 = true;
    } else {
        // sx1276?
        status = mraa_gpio_write(gpio_RST, LOW);
        if (status != MRAA_SUCCESS){
            printf("reset failure (3)\n");
        }

        delay_in_ms(100);

        status = mraa_gpio_write(gpio_RST, HIGH);
        if (status != MRAA_SUCCESS){
            printf("reset failure (4)\n");
        }

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
    
    mraa_result_t status = MRAA_SUCCESS;
    status = mraa_gpio_write(gpio_RST, HIGH);
    if (status != MRAA_SUCCESS){
         printf("reset failure (1)\n");
    }

    delay_in_ms(100);

    status = mraa_gpio_write(gpio_RST, LOW);
    if (status != MRAA_SUCCESS){
         printf("reset failure (2)\n");
    }

    delay_in_ms(100);

    status = mraa_gpio_write(gpio_RST, LOW);
    if (status != MRAA_SUCCESS){
        printf("reset failure (3)\n");
    }

    delay_in_ms(100);

    status = mraa_gpio_write(gpio_RST, HIGH);
    if (status != MRAA_SUCCESS){
        printf("reset failure (4)\n");
    }

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
        printf("heading into TRANSMIT mode\n");
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

    if(mraa_gpio_read(gpio_dio0) == 1){
        if(lora_recv(msg_buf,msg_len)) {
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
    int N = (int) len;
    uint8_t spibuf[N+1];
    uint8_t rxbuf[N+1];
    
    clear_buf(spibuf,N+1);
    clear_buf(rxbuf,N+1);

    spibuf[0] = 0x80;
    for (int i = 0; i < len; i++) {
        spibuf[i + 1] = value[i];
    }                                                                                                   
    selectreceiver();  
    mraa_spi_transfer_buf(spi, spibuf,rxbuf, N+1);
    unselectreceiver();                                                      
}

bool transmit_via_lora(char* msg, int msg_len) {
    // cannot transmit while in receive mode
    if(!transmitting){
	 return false;
    }

    put_in_neutral(); // TODO check if needed
	
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
    delay_in_ms(300); 
    
    return true; 
}


