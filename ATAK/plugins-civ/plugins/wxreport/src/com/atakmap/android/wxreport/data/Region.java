
package com.atakmap.android.wxreport.data;

import com.atakmap.coremap.log.Log;

import org.json.JSONObject;

public class Region implements JSONPopulator
{
    // Region is an Item level element in Yahoo Weather Services response
    // It corresponds to a state
    private String region;
    public static String TAG = "Region";

    public String getRegion()
    {
        return region;
    }

    @Override
    public void populate(JSONObject data)
    {
        // JSON data is key/value pair, extract state
        Log.d(TAG, "Getting region string from, " + data.toString());
        region = data.optString("region");
        Log.d(TAG, "Got region string, " + region);
    }
}
