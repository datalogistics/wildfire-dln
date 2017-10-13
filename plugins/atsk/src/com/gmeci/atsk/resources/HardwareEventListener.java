
package com.gmeci.atsk.resources;

import com.gmeci.core.SurveyPoint;

import java.util.EventListener;

public interface HardwareEventListener extends EventListener {

    void LRFEvent(SurveyPoint sp, double range, double azimuth, double elev);
}
