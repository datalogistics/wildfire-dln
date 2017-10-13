
package com.atakmap.android.wxreport.data;

import com.atakmap.coremap.log.Log;

import org.json.JSONObject;

public class Location implements JSONPopulator
{
    // Location is an Item level element in Yahoo Weather Services response
    // It contains a city/state for current weather conditions
    private City city;
    private Region region;
    public static String TAG = "WxReportLocation";

    public City getCity()
    {
        return city;
    }

    public Region getRegion()
    {
        return region;
    }

    @Override
    public void populate(JSONObject data)
    {
        // JSON data is key/value pair, extract city/state
        Log.d(TAG, "New city");
        city = new City();
        Log.d(TAG, "Getting city data from, " + data.toString());
        city.populate(data);
        Log.d(TAG, "Populated city");

        Log.d(TAG, "New region");
        region = new Region();
        Log.d(TAG, "Getting region data from, " + data.toString());
        region.populate(data);
        Log.d(TAG, "Populated region");
    }
}
