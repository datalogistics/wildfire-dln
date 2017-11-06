
package com.gmeci.atskservice.dz;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by smetana on 6/26/2014.
 */
public class CDSCriteria {

    public final List<Double> qty;
    public final List<Double> lengths;
    double width;
    double alt_min_f;
    double alt_change_f;
    double alt_mod;
    double night_mod;
    double pi_day;
    double pi_night;
    double capability;

    public CDSCriteria() {
        qty = new ArrayList<Double>();
        lengths = new ArrayList<Double>();
        this.width = 0;
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
        if (lengths == null || qty == null || lengths.isEmpty()
                || qty.isEmpty())
            return 0;

        double length_mod = 0;
        double width_mod = width;

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
        }

        //handle the initial less than case
        if (dzlength < length_mod || dzwidth < width_mod)
            return 0;

        //lets figure out a spot
        int length_spot = 0;
        for (int i = 0; i < lengths.size(); i++) {
            if (dzlength > (lengths.get(i) + length_mod))
                length_spot = i;
            else
                break;
        }
        if (length_spot > qty.size())
            return 0;
        else
            capability = qty.get(length_spot);

        return capability;
    }

    public double getWidth() {
        return width;
    }

    public void setWidth(double width) {
        this.width = width;
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

    public void setNight_mod(double night_mod) {
        this.night_mod = night_mod;
    }

    public void addQty(Double qty) {
        if (this.qty != null)
            this.qty.add(qty);
    }

    public void addLength(Double len) {
        if (this.lengths != null)
            this.lengths.add(len);
    }

}
