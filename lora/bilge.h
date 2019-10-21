/******************************************************************************

File: bilge.h
Author: Juliette Zerick (jzerick@iu.edu)
        for the WildfireDLN Project
        OPeN Networks Lab at Indiana University-Bloomington
        +
        Dragino, manufacturer of the LoRa Hat

This C header file contains various methods and definitions common to the 
hull_up/rpi header files. The definitions are Copyright (c) 2018 Dragino, from
the hardware manufacturer's transceiver example code available via Github:
https://github.com/dragino/rpi-lora-tranceiver/blob/master/dragino_lora_app/main.c

This work is part of the WildfireDLN project. It and other tools are available in
GitHub: 
https://github.com/datalogistics/wildfire-dln

Last modified: October 21, 2019

 *******************************************************************************/
 
#include <stdio.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <arpa/inet.h>
#include <string.h>
#include <sys/time.h>
#include <signal.h>
#include <stdlib.h>
#include <time.h>
#include <pthread.h>

#include <sys/ioctl.h>
#include <net/if.h> 
#include <unistd.h>
#include <netinet/in.h>

#define REG_FIFO                    0x00
#define REG_OPMODE                  0x01
#define REG_FIFO_ADDR_PTR           0x0D
#define REG_FIFO_TX_BASE_AD         0x0E
#define REG_FIFO_RX_BASE_AD         0x0F
#define REG_RX_NB_BYTES             0x13
#define REG_FIFO_RX_CURRENT_ADDR    0x10
#define REG_IRQ_FLAGS               0x12
#define REG_DIO_MAPPING_1           0x40
#define REG_DIO_MAPPING_2           0x41
#define REG_MODEM_CONFIG            0x1D
#define REG_MODEM_CONFIG2           0x1E
#define REG_MODEM_CONFIG3           0x26
#define REG_SYMB_TIMEOUT_LSB        0x1F
#define REG_PKT_SNR_VALUE           0x19
#define REG_PAYLOAD_LENGTH          0x22
#define REG_IRQ_FLAGS_MASK          0x11
#define REG_MAX_PAYLOAD_LENGTH      0x23
#define REG_HOP_PERIOD              0x24
#define REG_SYNC_WORD               0x39
#define REG_VERSION                 0x42

#define PAYLOAD_LENGTH              0x40

// LOW NOISE AMPLIFIER
#define REG_LNA                     0x0C
#define LNA_MAX_GAIN                0x23
#define LNA_OFF_GAIN                0x00
#define LNA_LOW_GAIN                0x20

#define RegDioMapping1              0x40 // common
#define RegDioMapping2              0x41 // common

#define RegPaConfig                 0x09 // common
#define RegPaRamp                   0x0A // common
#define RegPaDac                    0x5A // common

#define SX72_MC2_FSK                0x00
#define SX72_MC2_SF7                0x70
#define SX72_MC2_SF8                0x80
#define SX72_MC2_SF9                0x90
#define SX72_MC2_SF10               0xA0
#define SX72_MC2_SF11               0xB0
#define SX72_MC2_SF12               0xC0

#define SX72_MC1_LOW_DATA_RATE_OPTIMIZE  0x01 // mandated for SF11 and SF12

// sx1276 RegModemConfig1
#define SX1276_MC1_BW_125            0x70
#define SX1276_MC1_BW_250            0x80
#define SX1276_MC1_BW_500            0x90
#define SX1276_MC1_CR_4_5            0x02
#define SX1276_MC1_CR_4_6            0x04
#define SX1276_MC1_CR_4_7            0x06
#define SX1276_MC1_CR_4_8            0x08

#define SX1276_MC1_IMPLICIT_HEADER_MODE_ON    0x01

// sx1276 RegModemConfig2
#define SX1276_MC2_RX_PAYLOAD_CRCON           0x04

// sx1276 RegModemConfig3
#define SX1276_MC3_LOW_DATA_RATE_OPTIMIZE     0x08
#define SX1276_MC3_AGCAUTO                    0x04

// preamble for lora networks (nibbles swapped)
#define LORA_MAC_PREAMBLE                     0x34

#define RXLORA_RXMODE_RSSI_REG_MODEM_CONFIG1  0x0A
#ifdef LMIC_SX1276
#define RXLORA_RXMODE_RSSI_REG_MODEM_CONFIG2  0x70
#elif LMIC_SX1272
#define RXLORA_RXMODE_RSSI_REG_MODEM_CONFIG2  0x74
#endif

// FRF
#define        REG_FRF_MSB              0x06
#define        REG_FRF_MID              0x07
#define        REG_FRF_LSB              0x08

#define        FRF_MSB                  0xD9 // 868.1 Mhz
#define        FRF_MID                  0x06
#define        FRF_LSB                  0x66

// ----------------------------------------
// Constants for radio registers
#define OPMODE_LORA      0x80
#define OPMODE_MASK      0x07
#define OPMODE_SLEEP     0x00
#define OPMODE_STANDBY   0x01
#define OPMODE_FSTX      0x02
#define OPMODE_TX        0x03
#define OPMODE_FSRX      0x04
#define OPMODE_RX        0x05
#define OPMODE_RX_SINGLE 0x06
#define OPMODE_CAD       0x07

// ----------------------------------------
// Bits masking the corresponding IRQs from the radio
#define IRQ_LORA_RXTOUT_MASK 0x80
#define IRQ_LORA_RXDONE_MASK 0x40
#define IRQ_LORA_CRCERR_MASK 0x20
#define IRQ_LORA_HEADER_MASK 0x10
#define IRQ_LORA_TXDONE_MASK 0x08
#define IRQ_LORA_CDDONE_MASK 0x04
#define IRQ_LORA_FHSSCH_MASK 0x02
#define IRQ_LORA_CDDETD_MASK 0x01

// DIO function mappings                D0D1D2D3
#define MAP_DIO0_LORA_RXDONE   0x00  // 00------
#define MAP_DIO0_LORA_TXDONE   0x40  // 01------
#define MAP_DIO1_LORA_RXTOUT   0x00  // --00----
#define MAP_DIO1_LORA_NOP      0x30  // --11----
#define MAP_DIO2_LORA_NOP      0xC0  // ----11--

#define LOW              0 
#define HIGH             1

#define MESSAGE_LEN      512

/*
typedef bool boolean;
typedef unsigned char byte;

volatile sig_atomic_t receiving = false;
volatile sig_atomic_t transmitting = false;
volatile sig_atomic_t resetting = true;

bool sx1272 = false; // the LoRa GPS Hat has sx1276
*/
enum sf_t { SF7=7, SF8, SF9, SF10, SF11, SF12 };

// Set spreading factor (SF7 - SF12)
//sf_t sf = SF12;

// Set center frequency
uint32_t freq = 915000000; // in Mhz! (EU=868.1, US=915.0)

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

// Solution from Friek (2011) at StackOverflow
// in response to the following posted question:
// "How to get local IP and MAC address C [duplicate]" available at
// <https://stackoverflow.com/questions/6767296/how-to-get-local-ip-and-mac-address-c>
// last accessed: November 27, 2018
void get_mac_addr_from_eth0(char* mac_str){
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

// Solution from Charles Salvia (2009) and edited by Jamesprite (2012) at StackOverflow
// in response to the following posted question:
// "How to get MAC address of your machine using a C program?" available at
// <https://stackoverflow.com/questions/1779715/how-to-get-mac-address-of-your-machine-using-a-c-program>
// last accessed: October 21, 2019
int get_mac_addr(unsigned char* mac_str){
    struct ifreq ifr;
    struct ifconf ifc;
    unsigned char buf[1024];
    int success = 0;

    int sock = socket(AF_INET, SOCK_DGRAM, IPPROTO_IP);
    if (sock == -1) { /* handle error*/ };

    ifc.ifc_len = sizeof(buf);
    ifc.ifc_buf = buf;
    if (ioctl(sock, SIOCGIFCONF, &ifc) == -1) { /* handle error */ }

    struct ifreq* it = ifc.ifc_req;
    const struct ifreq* const end = it + (ifc.ifc_len / sizeof(struct ifreq));

    for (; it != end; ++it) {
        strcpy(ifr.ifr_name, it->ifr_name);
        if (ioctl(sock, SIOCGIFFLAGS, &ifr) == 0) {
            if (! (ifr.ifr_flags & IFF_LOOPBACK)) { // don't count loopback
                if (ioctl(sock, SIOCGIFHWADDR, &ifr) == 0) {
                    success = 1;
                    break;
                }
            }
        }
        else { /* handle error */ }
    }

    if (success) memcpy(mac_str, ifr.ifr_hwaddr.sa_data, 6);
    mac_str[6] = 0;
    return success;
}


