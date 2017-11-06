
package com.atakmap.android.wxreport.data;

import android.content.Context;
import android.graphics.drawable.Drawable;

import com.atakmap.android.wxreport.util.WeatherUtils;

import org.json.JSONObject;

public class Forecast implements JSONPopulator
{
    // Forecast is an array set of Item elements of Yahoo Weather Services response
    // It represents current weather conditions
    private int code;
    private String selected_date;
    private int hi_temperature;
    private int lo_temperature;
    private String description;
    public static String TAG = "Forecast";

    public Drawable get_conditionImage(Context pContext) {
        return WeatherUtils.findConditionImageByCode(pContext,code);
    }

    public String getSelected_date() {
        return selected_date;
    }

    public int getHiTemperature()
    {
        return hi_temperature;
    }

    public int getLoTemperature()
    {
        return lo_temperature;
    }

    public String getDescription()
    {
        return description;
    }

    @Override
    public void populate(JSONObject data)
    {
        // JSON data is key/value pair, extract date, hi/lo temperature and description of conditions
        code = data.optInt("code");
        selected_date = data.optString("date");
        hi_temperature = data.optInt("high");
        lo_temperature = data.optInt("low");
        description = data.optString("text");
    }
}
