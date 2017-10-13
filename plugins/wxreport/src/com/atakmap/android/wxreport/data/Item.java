
package com.atakmap.android.wxreport.data;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Item implements JSONPopulator
{
    // Item is an element of Yahoo Weather Services response
    // It can contain current weather conditions and other data
    private Condition condition;
    private List<Forecast> forecasst;
    public static String TAG = "Item";
    private String pubDate;

    public Item(){
        forecasst = new ArrayList<Forecast>();
    }

    public String getPubDate() {
        return pubDate;
    }

    public Condition getCondition()
    {
        return condition;
    }

    public List<Forecast> getForecasst() {
        return forecasst;
    }

    @Override
    public void populate(JSONObject data)
    {
        pubDate = data.optString("pubDate");
        // JSON data is key/value pair, extract current conditions
        condition = new Condition();
        condition.populate(data.optJSONObject("condition"));

        JSONArray forecastArray = data.optJSONArray("forecast");
        // JSON data is key/value pair, extract forecast
        for(int i = 0; i < forecastArray.length();i++){
            try {
                Forecast forecast = new Forecast();
                forecast.populate(forecastArray.getJSONObject(i));
                forecasst.add(forecast);
            } catch (JSONException e) {
                e.printStackTrace();
                //if we get an error something is wrong in the json stream just dont add it
            }
        }
    }
}
