import sys

#sys.path.append('/home/pi/.pyenv/versions/3.6.8/envs/ENV36/lib/python3.6/site-packages')

import wiringpi as wp
from functools import reduce

#include <string>
#include <stdio.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <arpa/inet.h>
#include <string.h>
#include <sys/time.h>
#include <signal.h>
#include <stdlib.h>

#include <sys/ioctl.h>

#include <wiringPi.h>
#include <wiringPiSPI.h>


 #############################################
 #############################################

REG_FIFO                  = 0x00
REG_OPMODE                = 0x01
REG_FIFO_ADDR_PTR         = 0x0D
REG_FIFO_TX_BASE_AD       = 0x0E
REG_FIFO_RX_BASE_AD       = 0x0F
REG_RX_NB_BYTES           = 0x13
REG_FIFO_RX_CURRENT_ADDR  = 0x10
REG_IRQ_FLAGS             = 0x12
REG_DIO_MAPPING_1         = 0x40
REG_DIO_MAPPING_2         = 0x41
REG_MODEM_CONFIG          = 0x1D
REG_MODEM_CONFIG2         = 0x1E
REG_MODEM_CONFIG3         = 0x26
REG_SYMB_TIMEOUT_LSB      = 0x1F
REG_PKT_SNR_VALUE         = 0x19
REG_PAYLOAD_LENGTH        = 0x22
REG_IRQ_FLAGS_MASK        = 0x11
REG_MAX_PAYLOAD_LENGTH    = 0x23
REG_HOP_PERIOD            = 0x24
REG_SYNC_WORD             = 0x39
REG_VERSION	  			  = 0x42

PAYLOAD_LENGTH            = 0x40

# LOW NOISE AMPLIFIER
REG_LNA                   = 0x0C
LNA_MAX_GAIN              = 0x23
LNA_OFF_GAIN              = 0x00
LNA_LOW_GAIN		      = 0x20

RegDioMapping1         = 0x40 # common
RegDioMapping2         = 0x41 # common

RegPaConfig            = 0x09 # common
RegPaRamp              = 0x0A # common
RegPaDac               = 0x5A # common

SX72_MC2_FSK              = 0x00
SX72_MC2_SF7              = 0x70
SX72_MC2_SF8              = 0x80
SX72_MC2_SF9              = 0x90
SX72_MC2_SF10             = 0xA0
SX72_MC2_SF11             = 0xB0
SX72_MC2_SF12             = 0xC0

SX72_MC1_LOW_DATA_RATE_OPTIMIZE = 0x01 # mandated for SF11 and SF12

# sx1276 RegModemConfig1
SX1276_MC1_BW_125              = 0x70
SX1276_MC1_BW_250              = 0x80
SX1276_MC1_BW_500              = 0x90
SX1276_MC1_CR_4_5          = 0x02
SX1276_MC1_CR_4_6          = 0x04
SX1276_MC1_CR_4_7          = 0x06
SX1276_MC1_CR_4_8          = 0x08

SX1276_MC1_IMPLICIT_HEADER_MODE_ON   = 0x01

# sx1276 RegModemConfig2
SX1276_MC2_RX_PAYLOAD_CRCON      = 0x04

# sx1276 RegModemConfig3
SX1276_MC3_LOW_DATA_RATE_OPTIMIZE =  0x08
SX1276_MC3_AGCAUTO               = 0x04

# preamble for lora networks (nibbles swapped)
LORA_MAC_PREAMBLE                = 0x34

RXLORA_RXMODE_RSSI_REG_MODEM_CONFIG1= 0x0A
#ifdef LMIC_SX1276
RXLORA_RXMODE_RSSI_REG_MODEM_CONFIG2 =0x70
#elif LMIC_SX1272
RXLORA_RXMODE_RSSI_REG_MODEM_CONFIG2 =0x74
#endif

# FRF
REG_FRF_MSB            = 0x06
REG_FRF_MID            = 0x07
REG_FRF_LSB            = 0x08

FRF_MSB                = 0xD9 # 868.1 Mhz
FRF_MID                = 0x06
FRF_LSB                = 0x66

# ----------------------------------------
# Constants for radio registers
OPMODE_LORA    = 0x80
OPMODE_MASK    = 0x07
OPMODE_SLEEP   = 0x00
OPMODE_STANDBY = 0x01
OPMODE_FSTX    = 0x02
OPMODE_TX      = 0x03
OPMODE_FSRX    = 0x04
OPMODE_RX      = 0x05
OPMODE_RX_SINGLE = 0x06
OPMODE_CAD     = 0x07

# ----------------------------------------
# Bits masking the corresponding IRQs from the radio
IRQ_LORA_RXTOUT_MASK = 0x80
IRQ_LORA_RXDONE_MASK = 0x40
IRQ_LORA_CRCERR_MASK = 0x20
IRQ_LORA_HEADER_MASK = 0x10
IRQ_LORA_TXDONE_MASK = 0x08
IRQ_LORA_CDDONE_MASK = 0x04
IRQ_LORA_FHSSCH_MASK = 0x02
IRQ_LORA_CDDETD_MASK = 0x01

# DIO function mappings                D0D1D2D3
MAP_DIO0_LORA_RXDONE = 0x00  # 00------
MAP_DIO0_LORA_TXDONE = 0x40  # 01------
MAP_DIO1_LORA_RXTOUT = 0x00  # --00----
MAP_DIO1_LORA_NOP    = 0x30  # --11----
MAP_DIO2_LORA_NOP    = 0xC0  # ----11--

# #############################################
# #############################################
#

SPI_CHANNEL = 0
SPI_SPEED = 500000

message = ''

sx1272 = False

receivedbytes = 0
receivedCount = 0

SF7 = 7
SF8 = 8
SF9 = 9
SF10 = 10
SF11 = 11
SF12 = 12

HIGH = 1
LOW = 0

OUTPUT = 1;
INPUT = 0;

'''
*******************************************************************************
 *
 * Configure these values!
 *
 *******************************************************************************
'''
# Raspbery  - Raspberry connections
ssPin = 6
dio0  = 7
RST   = 0

# Set spreading factor (SF7 - SF12)
sf = SF7

# Set center frequency
freq = 915000000 # in Mhz! (915)

hello = b"HELLO"

# if the wrong type of argument gets passed in, let it explode to let the 
# user know to fix code
def byte2int(val):
    return int.from_bytes(val,byteorder='big')

def str2int(val):
    return int(val,2)

# checking types isn't for safety here, it's for flexibility
def make_byte(val):
    if type(val) == bytes: 
        val = byte2int(val)
    elif type(val) == str:
        val = str2int(val)

    retval = (val & 255).to_bytes(1,byteorder='big')
    #print(val,'->',retval)
    return retval

def make_int(val):
    if type(val) == bytes: 
        return byte2int(val)
    elif type(val) == str:
        return str2int(val)

    return val

def selectreceiver():
    wp.digitalWrite(ssPin, LOW)

def unselectreceiver():
    wp.digitalWrite(ssPin, HIGH)

def readReg(addr):
    selectreceiver()
    spibuf = bytes([addr&0x7F,0])
    #print(spibuf)
    recv_buf = wp.wiringPiSPIDataRW(SPI_CHANNEL, spibuf)
    unselectreceiver()
    #print('\t',recv_buf,type(recv_buf[1][1]))

    return recv_buf[1][1]

def writeReg(addr,value):
    spibuf = bytes([addr|0x80,value &0xff])
    selectreceiver()
    wp.wiringPiSPIDataRW(SPI_CHANNEL, spibuf)
    unselectreceiver()

def opmode (mode):
    OPMODE_MASK_INV = 0xf8
    writeReg(REG_OPMODE, (readReg(REG_OPMODE) & OPMODE_MASK_INV) | mode)

def opmodeLora():
    u = OPMODE_LORA
    if not sx1272:
        u |= 0x8   # TBD: sx1276 high freq
    writeReg(REG_OPMODE, u)

def SetupLoRa():
    global sx1272
    wp.digitalWrite(RST, HIGH)
    wp.delay(100)
    wp.digitalWrite(RST, LOW)
    wp.delay(100)

    version = readReg(REG_VERSION)

    if (version == 0x22):
        # sx1272
        print("SX1272 detected, starting.\n")
        sx1272 = True
    else:
        # sx1276?
        wp.digitalWrite(RST, LOW)
        wp.delay(100)
        wp.digitalWrite(RST, HIGH)
        wp.delay(100)
        version = readReg(REG_VERSION)
        if (version == 0x12):
            # sx1276
            print("SX1276 detected, starting.\n")
            sx1272 = False
        else:
            print("Unrecognized transceiver.\n")
            print("Version: 0x%x\n",version)

    opmode(OPMODE_SLEEP)

    # set frequency
    frf = int((freq << 19) / 32000000)
    writeReg(REG_FRF_MSB, (frf>>16) )
    writeReg(REG_FRF_MID, (frf>> 8) )
    writeReg(REG_FRF_LSB, (frf>> 0) )

    writeReg(REG_SYNC_WORD, 0x34) # LoRaWAN public sync word

    if sx1272:
        if sf == SF11 or sf == SF12:
            writeReg(REG_MODEM_CONFIG,0x0B)
        else:
            writeReg(REG_MODEM_CONFIG,0x0A)
        
        writeReg(REG_MODEM_CONFIG2,(sf<<4) | 0x04)
    else:
        if (sf == SF11 or sf == SF12):
            writeReg(REG_MODEM_CONFIG3,0x0C)
        else:
            writeReg(REG_MODEM_CONFIG3,0x04)
        
        writeReg(REG_MODEM_CONFIG,0x72)
        writeReg(REG_MODEM_CONFIG2,(sf<<4) | 0x04)

    if (sf == SF10 or sf == SF11 or sf == SF12):
        writeReg(REG_SYMB_TIMEOUT_LSB,0x05)
    else:
        writeReg(REG_SYMB_TIMEOUT_LSB,0x08)

    writeReg(REG_MAX_PAYLOAD_LENGTH,0x80)
    writeReg(REG_PAYLOAD_LENGTH,PAYLOAD_LENGTH)
    writeReg(REG_HOP_PERIOD,0xFF)
    writeReg(REG_FIFO_ADDR_PTR, readReg(REG_FIFO_RX_BASE_AD))

    writeReg(REG_LNA, LNA_MAX_GAIN)

def receive(payload):
    # clear rxDone
    writeReg(REG_IRQ_FLAGS, 0x40)

    irqflags = readReg(REG_IRQ_FLAGS)

    #  payload crc: 0x20
    if((irqflags & 0x20) == 0x20):
        print("CRC error\n")
        writeReg(REG_IRQ_FLAGS, 0x20)
        return False
    else:
        currentAddr = readReg(REG_FIFO_RX_CURRENT_ADDR)
        receivedCount = readReg(REG_RX_NB_BYTES)
        receivedbytes = receivedCount

        print(receivedCount,receivedbytes)

        writeReg(REG_FIFO_ADDR_PTR, currentAddr)
        payload = b''

        for i in range(receivedCount):
            payload += readReg(REG_FIFO)
        print(payload)

    return True

def receivepacket():
    if(wp.digitalRead(dio0) == 1):
        if(receive(message)):
            value = readReg(REG_PKT_SNR_VALUE)
            if( value & 0x80 ): # The SNR sign bit is 1
                # Invert and divide by 4
                value = ( ( ~value + 1 ) & 0xFF ) >> 2
                SNR = -value
            else:
                # Divide by 4
                SNR = ( value & 0xFF ) >> 2
                
            if (sx1272):
                rssicorr = 139
            else:
                rssicorr = 157

            print("Packet RSSI: %d, " % (readReg(0x1A)-rssicorr))
            print("RSSI: %d, " % (readReg(0x1B)-rssicorr))
            print("SNR: %li, " % (SNR))
            print(type(receivedbytes),receivedbytes)
            print("Length: %d" % (receivedbytes))
            print("Payload: %s\n" % message)
            wp.digitalWrite(dio0,LOW)


def configPower (pw):
    if (sx1272 == False):
        # no boost used for now
        if(pw >= 17):
            pw = 15
        elif(pw < 2):
            pw = 2
        
        # check board type for BOOST pin
        writeReg(RegPaConfig, (0x80 | (pw & 0xf)) )
        writeReg(RegPaDac, readReg(RegPaDac) | 0x4)

    else:
        # set PA config (2-17 dBm using PA_BOOST)
        if(pw > 17):
            pw = 17
        elif(pw < 2):
            pw = 2
        
        writeReg(RegPaConfig, (0x80 | (pw - 2)) )

import codecs

def writeBuf(addr,msg,length):
    msg0 = list(map(ord,msg))
    buflist = [addr|0x80] + msg0
    spibuf = bytes(buflist)

    print(spibuf)

    selectreceiver()           
    wp.wiringPiSPIDataRW(SPI_CHANNEL, spibuf)
    unselectreceiver()         

def txlora(frame,datalen):
    # set the IRQ mapping DIO0=TxDone DIO1=NOP DIO2=NOP
    writeReg(RegDioMapping1, MAP_DIO0_LORA_TXDONE|MAP_DIO1_LORA_NOP|MAP_DIO2_LORA_NOP)
    # clear all radio IRQ flags
    writeReg(REG_IRQ_FLAGS, 0xFF)
    # mask all IRQs but TxDone
    writeReg(REG_IRQ_FLAGS_MASK, ~IRQ_LORA_TXDONE_MASK)

    # initialize the payload size and address pointers
    writeReg(REG_FIFO_TX_BASE_AD, 0x00)
    writeReg(REG_FIFO_ADDR_PTR, 0x00)
    writeReg(REG_PAYLOAD_LENGTH, datalen)

    # download buffer to the radio FIFO
    writeBuf(REG_FIFO, frame, datalen)
    # now we actually start the transmission
    opmode(OPMODE_TX)

    print("send: %s\n", frame)

def main():
    if (len(sys.argv) < 2):
        print ("Usage: argv[0] sender|rec [message]\n")
        exit(1)

    wp.wiringPiSetupGpio()
    wp.pinMode(ssPin, OUTPUT)
    wp.pinMode(dio0, INPUT)
    wp.pinMode(RST, OUTPUT)

    wp.wiringPiSPISetup(SPI_CHANNEL, SPI_SPEED)

    SetupLoRa()

    if (sys.argv[1] == 'sender'):
        opmodeLora()
        # enter standby mode (required for FIFO loading))
        opmode(OPMODE_STANDBY)

        writeReg(RegPaRamp, (readReg(RegPaRamp) & 0xF0) | 0x08) # set PA ramp-up time 50 uSec

        configPower(23)

        print("Send packets at SF%i on %.6lf Mhz.\n" % (sf,freq/1000000.))
        print("------------------\n")

        if len(sys.argv) > 2:
            hello = argv[2].encode()
        else:
            hello = 'HELLO'

        while(1) :
            txlora(hello, len(hello))
            wp.delay(5000)
    else:
        # radio init
        opmodeLora()
        opmode(OPMODE_STANDBY)
        opmode(OPMODE_RX)
        print("Listening at SF%i on %.6lf Mhz.\n" % (sf,freq/1000000.))
        print("------------------\n")
        wp.digitalWrite(dio0,LOW)
        while(1):
            receivepacket() 
            wp.delay(1000)

    return 0


main()
