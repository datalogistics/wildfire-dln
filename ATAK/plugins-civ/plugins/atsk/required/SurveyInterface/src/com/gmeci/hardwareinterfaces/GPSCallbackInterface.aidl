package com.gmeci.hardwareinterfaces;

import com.gmeci.core.SurveyPoint;

interface GPSCallbackInterface
{
    void UpdateGPS(in SurveyPoint location, int quality);
}
