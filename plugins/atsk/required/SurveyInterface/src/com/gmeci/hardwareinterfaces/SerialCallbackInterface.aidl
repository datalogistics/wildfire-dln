package com.gmeci.hardwareinterfaces;


interface SerialCallbackInterface{

    boolean SendData(in byte[] data, in int length, in int StreamID);
}