
package com.atakmap.android.wxreport.data;

import com.atakmap.coremap.log.Log;

import org.json.JSONObject;

public class City implements JSONPopulator
{
    // City is a location within a channel of Yahoo Weather Services response
    private String city;
    public static String TAG = "City";

    public String getCity()
    {
        return city;
    }

    @Override
    public void populate(JSONObject data)
    {
        // JSON data is key/value pair, extract city
        Log.d(TAG, "Getting city string from, " + data.toString());
        city = data.optString("city");
        Log.d(TAG, "Got city string, " + city);
    }
}
