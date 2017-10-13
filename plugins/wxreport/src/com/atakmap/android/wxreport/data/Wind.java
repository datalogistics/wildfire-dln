package com.atakmap.android.wxreport.data;

import org.json.JSONObject;

/**
 * Created by Scott Auman @Par Government on 9/21/2017.
 */

public class Wind implements JSONPopulator {

    private int chill;
    private int direction;
    private int speed;

    @Override
    public void populate(JSONObject data) {
        chill = data.optInt("chill");
        direction = data.optInt("direction");
        speed = data.optInt("speed");
    }

    public int getChill() {
        return chill;
    }

    public int getDirection() {
        return direction;
    }

    public int getSpeed() {
        return speed;
    }
}
