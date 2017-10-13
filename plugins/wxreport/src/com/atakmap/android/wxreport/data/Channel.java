
package com.atakmap.android.wxreport.data;

import com.atakmap.coremap.log.Log;

import org.json.JSONException;
import org.json.JSONObject;

public class Channel implements JSONPopulator
{
    // Channel is a top level element of Yahoo Weather Services response
    // It represents a feed and its contents and contains all the other elements
    private Units units;
    private Location location;
    private Item item;
    private Atmosphere atmosphere;
    private Wind wind;
    public static String TAG = "Channel";

    public Units getUnits()
    {
        return units;
    }

    public Location getLocation()
    {
        return location;
    }

    public Item getItem()
    {
        return item;
    }

    public Wind getWind() {
        return wind;
    }

    public Atmosphere getAtmosphere() {
        return atmosphere;
    }

    @Override
    public void populate(JSONObject data) throws JSONException {

        wind = new Wind();
        wind.populate(data.getJSONObject("wind"));

        atmosphere =  new Atmosphere();
        atmosphere.populate(data.getJSONObject("atmosphere"));

        // Create internal structure and fill in
        units = new Units();
        units.populate(data.optJSONObject("units"));

        location = new Location();
        Log.d(TAG, "Getting location data from, " + data.toString());
        location.populate(data.optJSONObject("location"));
        Log.d(TAG, "Populated location");

        item = new Item();
        item.populate(data.optJSONObject("item"));

    }
}
