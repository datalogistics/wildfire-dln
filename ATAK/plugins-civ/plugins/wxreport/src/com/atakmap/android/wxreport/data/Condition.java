
package com.atakmap.android.wxreport.data;

import org.json.JSONObject;

public class Condition implements JSONPopulator
{
    // Condition is an Item level element of Yahoo Weather Services response
    // It represents current weather conditions
    private int code;
    private int temperature;
    private String description;

    public int getCode()
    {
        return code;
    }

    public int getTemperature()
    {
        return temperature;
    }

    public String getDescription()
    {
        return description;
    }

    @Override
    public void populate(JSONObject data)
    {
        // JSON data is key/value pair, extract temperature and description of conditions
        code = data.optInt("code");
        temperature = data.optInt("temp");
        description = data.optString("text");
    }
}
