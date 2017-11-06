
package com.atakmap.android.wxreport.data;

import org.json.JSONObject;

public class Units implements JSONPopulator
{
    // Units is a Channel level element for Yahoo Weather Services response
    // It corresponds to either metric/english for units of weather conditions
    private String temperature;

    public String getTemperature()
    {
        return temperature;
    }

    @Override
    public void populate(JSONObject data)
    {
        // JSON data is key/value pair, extract units of temperature
        temperature = data.optString("temperature");
    }
}
