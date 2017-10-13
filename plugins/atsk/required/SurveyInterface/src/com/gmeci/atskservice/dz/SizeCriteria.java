
package com.gmeci.atskservice.dz;

import android.util.Log;

/**
 * Created by smetana on 6/26/2014.
 */
public class SizeCriteria {

    private static final String TAG = "SizeCriteria";

    String type;
    double length_m;
    double width_m;
    double qty_mod;
    double night_mod;
    double alt_min_f;
    double alt_change_f;
    double alt_mod;
    double pi_day;
    double pi_night;
    double capability;

    public SizeCriteria() {
        this.type = "";
        this.length_m = 0;
        this.width_m = 0;
        this.qty_mod = 0;
        this.night_mod = 0;
        this.alt_change_f = 0;
        this.alt_min_f = 0;
        this.alt_mod = 0;
        this.pi_day = 0;
        this.pi_night = 0;
        this.capability = 0;
    }

    public double getCapabilities(double dzlength, double dzwidth,
            boolean night,
            double altitude) {
        //calculation methodology:
        //determine the size for 1 (min length + night_mod + altitude mod
        //use the difference of (dzlength - min_for_1_size) for extra

        double length_mod = this.length_m;
        double width_mod = this.width_m;

        if (this.type.equals("HVCDS"))
            Log.d(TAG, "HVCDS, bitches");

        //handle night
        if (night) {
            length_mod += this.night_mod;
            width_mod += this.night_mod;
        }

        //handle altitude
        if (this.alt_min_f < altitude) {
            double size_mod = (float) Math.floor((altitude - alt_min_f)
                    / alt_change_f)
                    * this.alt_mod;
            length_mod += size_mod;
            width_mod += size_mod;
        } else if (this.alt_min_f > altitude)
            return 0;

        //handle less than cases. 
        if (dzlength < length_mod || dzwidth < width_mod)
            return 0;
        else
            capability = 1;//at least 1

        //handle quantity
        double size_mod = (float) Math.floor((dzlength - length_mod) / qty_mod);

        capability += size_mod;

        return capability;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getLength_m() {
        return length_m;
    }

    public void setLength_m(double length_m) {
        this.length_m = length_m;
    }

    public double getWidth_m() {
        return width_m;
    }

    public void setWidth_m(double width_m) {
        this.width_m = width_m;
    }

    public void setQty_mod(double qty_mod) {
        this.qty_mod = qty_mod;
    }

    public void setNight_mod(double night_mod) {
        this.night_mod = night_mod;
    }

    public void setAlt_min_f(double alt_min_f) {
        this.alt_min_f = alt_min_f;
    }

    public void setAlt_change_f(double alt_change_f) {
        this.alt_change_f = alt_change_f;
    }

    public void setAlt_mod(double alt_mod) {
        this.alt_mod = alt_mod;
    }

    public double getPi_day() {
        return pi_day;
    }

    public void setPi_day(double pi_day) {
        this.pi_day = pi_day;
    }

    public double getPi_night() {
        return pi_night;
    }

    public void setPi_night(double pi_night) {
        this.pi_night = pi_night;
    }

}
