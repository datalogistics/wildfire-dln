package com.atakmap.android.wxreport.data;

import org.json.JSONObject;

/**
 * Created by Scott Auman @Par Government on 9/21/2017.
 */

public class Atmosphere implements JSONPopulator {

    private int humidity;
    private double pressure;
    private int rising;
    private double visibility;


    public int getHumidity() {
        return humidity;
    }

    public double getPressure() {
        return pressure;
    }

    public int getRising() {
        return rising;
    }

    public double getVisibility() {
        return visibility;
    }

    @Override
    public void populate(JSONObject data) {
        humidity = data.optInt("humidity");
        pressure = data.optDouble("pressure");
        rising = data.optInt("rising");
        visibility = data.optDouble("visibility",0.0);
    }




}
