package com.gmeci.hardwareinterfaces;


import com.gmeci.hardwareinterfaces.SerialCallbackInterface;


interface ParserInterface{

    boolean RegisterConsumer(in String id, in SerialCallbackInterface callback);

    void NewOwnshipPoint(double lat, double lon, double hae_m, 
                         String id, int LockQuality, double ce_m, double le_m, 
                         double course_deg_t, double speed_mps, String DeviceName, long timetamp, 
                         String rawInfo);

    //void NewOffsetPosition(double lat, double lon,double hae_m, double ce_m, double le_m, String DeviceName);
}

