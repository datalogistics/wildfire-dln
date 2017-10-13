
package com.gmeci.core;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

import com.gmeci.conversions.Conversions;
import com.gmeci.conversions.Conversions.Unit;

import java.io.Serializable;
import java.util.Comparator;

public class SurveyPoint implements Parcelable, Serializable {

    public enum CollectionMethod {
        MANUAL(0, "Manual", 0xFF226622),
        INTERNAL_GPS(1, "Internal GPS", 0xFF666622),
        EXTERNAL_GPS(2, "External GPS", 0xFF222266),
        RTK(3, "RTK", 0xFF222266);

        public final int value, color;
        public final String name;

        CollectionMethod(int value, String name, int color) {
            this.value = value;
            this.name = name;
            this.color = color;
        }

        public static CollectionMethod fromValue(int value) {
            for (CollectionMethod wc : CollectionMethod.values()) {
                if (wc.value == value)
                    return wc;
            }
            return MANUAL;
        }
    }

    // Speed comparator (range is stored here for gradient points)
    public static class SpeedComparator implements Comparator<SurveyPoint> {

        private final boolean ascending;

        public SpeedComparator(boolean ascending) {
            this.ascending = ascending;
        }

        @Override
        public int compare(SurveyPoint lhs, SurveyPoint rhs) {
            return this.ascending ? Double.compare(lhs.speed, rhs.speed)
                    : Double.compare(rhs.speed, lhs.speed);
        }
    }

    /**
     * Parceable implementation.
     */
    public static final Parcelable.Creator<SurveyPoint> CREATOR = new Parcelable.Creator<SurveyPoint>() {
        @Override
        public SurveyPoint createFromParcel(Parcel source) {
            return new SurveyPoint(source);
        }

        @Override
        public SurveyPoint[] newArray(int size) {
            return new SurveyPoint[size];
        }
    };
    private static final long serialVersionUID = 1328056176230803021L;
    public double lat;
    public double lon;
    public Altitude alt;
    public double course_true, speed;
    public double linearError;
    public double circularError;
    public long timestamp;
    public boolean visible;
    public int order;

    public CollectionMethod collectionMethod;
    public String rawInfo = "";

    /**
     * Construction of a SurveyPoint in the form of a latitude, longitude and altitude where the
     * altitude is in meters hae.
     */
    private SurveyPoint(final double latitude, final double longitude,
            final double altitude) {
        this.lat = latitude;
        this.lon = longitude;
        this.alt = new Altitude(altitude, AltitudeRef.HAE);

        linearError = 0;
        circularError = 0;
    }

    public SurveyPoint(final double latitude, final double longitude,
            final Altitude altitude) {
        this.lat = latitude;
        this.lon = longitude;
        this.alt = altitude;

        linearError = 0;
        circularError = 0;
    }

    /**
     * Construction of a nullary SurveyPoint (0,0,0)
     */
    public SurveyPoint() {
        this(0, 0, Altitude.INVALID);
    }

    /**
     * Construction of a survey point represented by latitude, longitude but no altitude.
     */
    public SurveyPoint(final double latitude, final double longitude) {
        this(latitude, longitude, Altitude.INVALID);
    }

    public SurveyPoint(SurveyPoint copy) {
        this(copy.lat, copy.lon, copy.alt);
        setSurveyPoint(copy);
    }

    public SurveyPoint(Parcel in) {
        readFromParcel(in);
    }

    /**
     * Print out a reasonable representation of the point for human readable purposes.
     */
    public String toString() {
        return String.format("%s ce:%.1f le:%.1f",
                Conversions.GetLatLonDM(lat, lon), circularError, linearError);
    }

    public String toCSV() {
        return lat + "," + lon + "," + getHAE()
                + "," + circularError + "," + linearError;
    }

    public void readFromParcel(Parcel in) {
        lat = in.readDouble();
        lon = in.readDouble();
        alt = new Altitude(in.readDouble(), AltitudeRef.HAE);

        linearError = in.readDouble();
        circularError = in.readDouble();

        this.course_true = in.readDouble();
        this.speed = in.readDouble();

        this.timestamp = in.readLong();

        this.collectionMethod =
                CollectionMethod.fromValue((int) in.readByte());

    }

    /**
     * Convienence method for setting the survey latitude, longitude, and altitude.
     */
    public void setSurveyPoint(final double lat, final double lon,
            final Altitude alt) {
        this.lat = lat;
        this.lon = lon;
        this.alt = alt;
    }

    private void setSurveyPoint(final double lat, final double lon,
            final double alt) {
        setSurveyPoint(lat, lon, new Altitude(alt, AltitudeRef.HAE));
    }

    /**
     * Convenience method for setting the survey latiude, longitude with a nullary altitude
     */
    public void setSurveyPoint(double lat, double lon) {
        setSurveyPoint(lat, lon, Altitude.INVALID);
    }

    public void setSurveyPoint(SurveyPoint sp) {
        setSurveyPoint(sp.lat, sp.lon, sp.alt);
        this.circularError = sp.circularError;
        this.linearError = sp.linearError;
        this.course_true = sp.course_true;
        this.speed = sp.speed;
        this.timestamp = sp.timestamp;
        this.collectionMethod = sp.collectionMethod;
    }

    /**
     * Provided a current point, offset the current point by finding the ray offset from a given
     * angle. Destroys the values within the current survey point latitude and longitude but
     * preserves the altitude, collection method, timestamp.
     */
    public void findCorner(double angle, double length, double width) {
        // sets this point's position at a corner
        double baseAngle = (180 / Math.PI) * Math
                .atan2(length, width);
        double finalAngle = baseAngle + angle - 90;

        double dist2corner = Math.sqrt((length * length)
                + (width * width));

        if (length < 0)
            finalAngle += 180;

        double corners[] = Conversions.AROffset(lat, lon, finalAngle,
                dist2corner);

        this.lat = corners[0];
        this.lon = corners[1];
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeDouble(lat);
        dest.writeDouble(lon);
        dest.writeDouble(getHAE());

        dest.writeDouble(linearError);
        dest.writeDouble(circularError);

        dest.writeDouble(course_true);
        dest.writeDouble(speed);
        dest.writeLong(timestamp);
        dest.writeByte((byte) collectionMethod.value);

    }

    public Altitude getAltitude(AltitudeRef ref) {
        return ref == AltitudeRef.HAE ? getHAEAltitude() : getMSLAltitude();
    }

    public Altitude getMSLAltitude() {
        if (alt.isMSL())
            return alt;
        if (!alt.isValid())
            return new Altitude(Altitude.INVALID, AltitudeRef.MSL);
        return new Altitude(Conversions.ConvertHAEtoMSL(lat,
                lon, alt.getValue()), AltitudeRef.MSL);
    }

    public Altitude getHAEAltitude() {
        if (alt.isHAE())
            return alt;
        if (!alt.isValid())
            return new Altitude(Altitude.INVALID, AltitudeRef.HAE);
        return new Altitude(Conversions.ConvertMSLtoHAE(lat,
                lon, alt.getValue()), AltitudeRef.HAE);
    }

    public double getMSL() {
        return getMSLAltitude().getValue();
    }

    public double getHAE() {
        return getHAEAltitude().getValue();
    }

    public void setHAE(double alt) {
        this.alt = new Altitude(alt, AltitudeRef.HAE);
    }

    public void setMSL(double alt) {
        this.alt = new Altitude(alt, AltitudeRef.MSL);
    }

    /**
     * Altitude as an object
     */
    public static class Altitude {
        public static final int INVALID = 9999999;

        private double _value;
        private final AltitudeRef _ref;

        public Altitude(double value, AltitudeRef ref) {
            _value = value;
            _ref = ref;
        }

        public static final boolean isValid(double val) {
            return val < INVALID;
        }

        public boolean isValid() {
            return _value < INVALID;
        }

        public void invalidate() {
            _value = INVALID;
        }

        public double getValue() {
            return _value;
        }

        public AltitudeRef getRef() {
            return _ref;
        }

        public boolean isMSL() {
            return _ref == AltitudeRef.MSL;
        }

        public boolean isHAE() {
            return _ref == AltitudeRef.HAE;
        }

        public String toString(Conversions.Unit unit, boolean includeUnits) {
            if (!isValid())
                return "UNKNOWN";
            return String.format("%.1f%s", Unit.METER.convertTo(_value, unit),
                    includeUnits ? " " + unit.getAbbr() : "");
        }

        public String toString(Unit unit) {
            return toString(unit, true);
        }

        @Override
        public String toString() {
            return toString(Unit.METER, true);
        }
    }

    public enum AltitudeRef {
        HAE(0, "HAE"),
        MSL(1, "MSL");

        private final int _value;
        private final String _abbr;

        AltitudeRef(final int value, final String abbr) {
            _value = value;
            _abbr = abbr;
        }

        public String getAbbr() {
            return _abbr;
        }

        public int getValue() {
            return _value;
        }
    }

    public double distanceTo(final SurveyPoint destination) {
        try {
            float[] result = new float[1];
            Location.distanceBetween(this.lat, this.lon,
                    destination.lat,
                    destination.lon, result);
            return result[0];
        } catch (IllegalArgumentException iae) {
            return Double.NaN;
        }
    }
}
