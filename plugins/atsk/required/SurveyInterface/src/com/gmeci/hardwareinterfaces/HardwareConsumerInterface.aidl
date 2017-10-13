package com.gmeci.hardwareinterfaces;

import com.gmeci.core.SurveyPoint;
import com.gmeci.hardwareinterfaces.GPSCallbackInterface;
interface HardwareConsumerInterface{
    //these should be separate - but OK for now.
    void StartNewGradientRoute(String uid, String type, String groupName, double GPSHeight_m);
    void StartNewLineRoute(String uid, String type, String groupName, double GPSHeight_m, double WidthOffset_m);
    void EndCurrentRoute(boolean Debug);
    
    boolean isCollectingGradient();
    boolean isCollectingRoute();
    
    SurveyPoint getMostRecentPoint();
    boolean register(String id, GPSCallbackInterface callback);    
    boolean unregister(String id);    
}
