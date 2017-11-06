
package com.gmeci.atskservice.dz;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by smetana on 6/26/2014.
 */
public class JPADSCriteria {

    private final List<Double> lengths;
    private final List<Double> altitudes;

    public JPADSCriteria() {
        lengths = new ArrayList<Double>();
        altitudes = new ArrayList<Double>();
    }

    public double getCapabilities(double dzlength, double altitude) {
        if (lengths == null || altitudes == null || lengths.isEmpty()
                || altitudes.isEmpty())
            return 0;

        //find the spot in the alt array. 
        int alt_spot = 0;

        for (int i = 0; i < altitudes.size(); i++) {
            if (altitude > altitudes.get(i))
                alt_spot = i;
            else
                break;
        }

        if (lengths.size() < alt_spot) {
            return 0;
        }

        double length = lengths.get(alt_spot);
        if (length > dzlength)
            return 0;
        else
            return 1;
    }

    public void addAlt(double alt) {
        if (this.altitudes != null)
            this.altitudes.add(alt);
    }

    public void addLength(Double length) {
        if (this.lengths != null)
            this.lengths.add(length);
    }

}
