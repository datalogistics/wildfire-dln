
package com.gmeci.conversions;

import android.annotation.SuppressLint;
import android.hardware.GeomagneticField;
import android.util.Log;

import com.gmeci.core.SurveyPoint;
import com.gmeci.core.SurveyPoint.Altitude;
import com.google.gson.GsonBuilder;

import java.util.List;

@SuppressLint("DefaultLocale")
public class Conversions {

    public static final double KPH2MPS = 0.2777777777777778;
    public static final double DEG2RAD = Math.PI / 180.0;
    public static final double RAD2DEG = 180.0 / Math.PI;
    public static final double M2NM = 0.000539;
    public static final double THRESH = .001; // 1 mm or 1/1000 of a foot depending
    public static final double RATIO_99_1 = 0.578;

    /** 
     * The original GMECI implementation defined Meters to Feet as a constant of 3.28084f,
     * this further refines M2F to the best possible conversion.   This however contradicts
     * the official guidance on conversion factors - 
     * 
     * In 1959 the inch was set to be exactly 2.54 centimeters.  Prior to that, it was set 
     * at 39.37 inches per meter. The difference is only 0.0002%, or 2 parts per million, 
     * with the new definition being slightly smaller.  The "US Survey" numbers are still 
     * officially used where measurements are derived by "geodetic surveys" within the United 
     * States.  Presumably this was done to avoid having the definition of land owned and traded 
     * change with the unit change, even though the difference amounts to 111 square feet per square 
     * mile, or 25 square inches per acre.
     */

    public static final double M2F = (100d / (2.54d * 12));
    public static final String COORD_FORMAT_MGRS = "COORD_FORMAT_MGRS";
    public static final String COORD_FORMAT_DMS = "COORD_FORMAT_DMS";
    public static final String COORD_FORMAT_DM = "COORD_FORMAT_DM";
    public static final String COORD_FORMAT_UTM = "COORD_FORMAT_UTM";
    public static final String UNOBSTRUCTED = "Unobstructed";
    private static final String TAG = "Conversions";
    private static final double PI = 3.14159265358979323846;
    private static final double MIN_LAT = -80.5;
    private static final double MAX_LAT = 84.5;

    static {
        System.loadLibrary("conversions");
    }

    static {
        Log.d("Conversions", "Loading native library");
        System.loadLibrary("conversions");
        Log.d("Conversions", "Native library loaded");
    }

    public enum Unit {
        KILOMETER(0, 1000, "kilometer", "kilometers", "km"),
        METER(1, 1, "meter", "meters", "m"),
        MILE(2, 1609.344, "mile", "miles", "mi"),
        YARD(3, 3 / M2F, "yard", "yards", "yds"),
        FOOT(4, 1 / M2F, "foot", "feet", "ft"),
        NAUTICALMILE(5, 1 / M2NM, "nautical mile", "nautical miles", "NM");

        private final int _index;
        private final double _meters;
        private final String _single;
        private final String _plural;
        private final String _abbr;

        Unit(int index, double meters, String single, String plural, String abbr) {
            _index = index;
            _meters = meters;
            _single = single;
            _plural = plural;
            _abbr = abbr;
        }

        public String getAbbr() {
            return _abbr;
        }

        public String getSingle() {
            return _single;
        }

        public String getPlural() {
            return _plural;
        }

        public int getIndex() {
            return _index;
        }

        public double getMeters() {
            return _meters;
        }

        public double convertTo(double value, Unit dst) {
            if (this == dst)
                return value;
            return value * _meters / dst.getMeters();
        }

        public String format(double value) {
            return String.format("%.1f %s", value, getAbbr());
        }

        public String format(double value, Unit dst) {
            return dst.format(convertTo(value, dst));
        }

        // Convert a string like "50.0ft" to a double value
        public double fromString(String text) {
            double ret = 0.0d;
            if (text == null || text.isEmpty())
                return ret;
            try {
                // First try parsing text without units
                ret = Float.parseFloat(text);
            } catch (NumberFormatException e) {
                // Split value and unit
                String valueStr = "0";
                String unitStr = getAbbr();
                for (int i = 0; i < text.length(); i++) {
                    char c = text.charAt(i);
                    if (c >= 'a' && c <= 'z') {
                        valueStr = text.substring(0, i);
                        unitStr = text.substring(i);
                        break;
                    }
                }
                try {
                    ret = Float.parseFloat(valueStr);
                } catch (NumberFormatException e2) {
                    Log.w(TAG, "Failed to find float value in string " + text);
                }
                // Find matching Unit
                Unit unit = Unit.METER;
                for (Unit u : Unit.values()) {
                    if (unitStr.equals(u.getAbbr())
                            || unitStr.equals(u.getSingle())
                            || unitStr.equals(u.getPlural())) {
                        unit = u;
                        break;
                    }
                }
                return unit.convertTo(ret, this);
            }
            return ret;
        }
    }

    /**
     * Provided a measuremement in meters, returns either meters feet or yards.
     *
     * @param Measurement_m     the measuement in meters.
     * @param UnitFormat        the format (either METERS or something else.
     * @param StandardUnitsFeet false to produce yards.
     */
    public static double GetMeasurement(double Measurement_m,
            String UnitFormat,
            boolean StandardUnitsFeet) {
        if (UnitFormat.equalsIgnoreCase("METERS")
                || UnitFormat.equalsIgnoreCase("m")) {
            return Measurement_m;
        } else {
            if (StandardUnitsFeet)
                return Measurement_m * M2F;
            return Measurement_m * M2F / 3;
        }
    }

    /**
     * Provided a UnitFormat, return m for Meters otherwise ft or yards.
     *
     * @param UnitFormat        could be METERS or m or something else.
     * @param StandardUnitsFeet true will return feet, otherwise yards.
     */
    public static String GetMeasurementUnit(String UnitFormat,
            boolean StandardUnitsFeet) {
        if (UnitFormat.equalsIgnoreCase("METERS")
                || UnitFormat.equalsIgnoreCase("m")) {
            return "m";
        } else {
            if (StandardUnitsFeet)
                return "ft";
            return "yds";
        }
    }

    /**
     * Produce a coordinate String for a given latitude and longitude.
     * The coordinate string can either be one of COORD_FORMAT_MGRS,
     * COORD_FORMAT_DMS, COORD_FORMAT_DM, COORD_FORMAT_UTM.
     */
    public static String getCoordinateString(double lat, double lon,
            String CoordinateFormat) {

        if (Double.isNaN(lat) || Double.isNaN(lon)) {
            return "bad coords";
        }

        if (CoordinateFormat.equals(COORD_FORMAT_MGRS)) {
            return GetMGRS(lat, lon);
        } else if (CoordinateFormat.equals(COORD_FORMAT_DMS)) {
            return GetLatLonDMS(lat, lon);
        } else if (CoordinateFormat.equals(COORD_FORMAT_DM)) {
            return GetLatLonDM(lat, lon);
        } else if (CoordinateFormat.equals(COORD_FORMAT_UTM))
        {
            Log.d(TAG, "about to return UTM " + getUTMString(lat, lon)
                    + " for " + lat + " " + lon);
            return getUTMString(lat, lon);
        } else {
            return GetMGRS(lat, lon);
        }
    }

    public static String getCoordinateString(
            SurveyPoint sp, String coordFmt) {
        return getCoordinateString(sp.lat, sp.lon, coordFmt);
    }

    public static String GetCardinalDirection(double angle) {

        angle = deg360(angle);

        if (angle > 348.75 || angle < 11.25)
            return "N";
        else if (angle < 33.75)
            return "NNE";
        else if (angle < 56.25)
            return "NE";
        else if (angle < 78.75)
            return "ENE";
        else if (angle < 101.25)
            return "E";
        else if (angle < 123.75)
            return "ESE";
        else if (angle < 146.25)
            return "SE";
        else if (angle < 168.75)
            return "SSE";
        else if (angle < 191.25)
            return "S";
        else if (angle < 213.75)
            return "SSW";
        else if (angle < 236.25)
            return "SW";
        else if (angle < 258.75)
            return "WSW";
        else if (angle < 281.25)
            return "W";
        else if (angle < 303.75)
            return "WNW";
        else if (angle < 326.25)
            return "NW";
        else
            return "NNW";

    }

    public static double deg360(double deg) {
        return deg % 360 + (deg < 0 ? 360 : 0);
    }

    /**
     * Check if an angle is between a range
     * @param deg Angle to check
     * @param minDeg Minimum angle
     * @param maxDeg Maximum angle
     * @return True if angle is between range
     */
    public static boolean angleWithin(double deg, double minDeg, double maxDeg) {
        minDeg = deg360(minDeg);
        maxDeg = deg360(maxDeg);
        deg = deg360(deg);
        if (maxDeg < minDeg) {
            if (deg < maxDeg)
                deg += 360;
            maxDeg += 360;
        }
        return deg >= minDeg && deg <= maxDeg;
    }

    public static double deNaN(double d) {
        if (Double.isNaN(d) || Double.isInfinite(d))
            return 0;
        return d;
    }

    /**
     * Get the bounds for a set of bounds
     * @param points List of points
     * @return [minLat, minLon, maxLat, maxLon]
     */
    public static double[] getBounds(List<SurveyPoint> points) {
        double[] bounds = new double[] {
                90, 180, -90, -180
        };
        for (SurveyPoint sp : points) {
            bounds[0] = Math.min(bounds[0], sp.lat);
            bounds[1] = Math.min(bounds[1], sp.lon);
            bounds[2] = Math.max(bounds[2], sp.lat);
            bounds[3] = Math.max(bounds[3], sp.lon);
        }
        return bounds;
    }

    /**
     * Given a previous point, current point, and next point, compute the angular
     * change when traveling in the forward direction from p->c->n and the angle
     * desired at the bend.
     */
    public static double computeAngle(SurveyPoint p, SurveyPoint c,
            SurveyPoint n) {

        if (c == null)
            return 0;

        final double[] first, second;
        double angleIn = -1, angleOut = 0;
        if (p != null) {
            first = CalculateRangeAngle(p.lat, p.lon, c.lat, c.lon);
            angleIn = angleOut = deg360(first[1]);
        }
        if (n != null) {
            second = CalculateRangeAngle(c.lat, c.lon, n.lat, n.lon);
            angleOut = deg360(second[1]);
            if (angleIn == -1)
                angleIn = angleOut;
        }

        double angle = (angleIn + angleOut) / 2;

        // the spread of the angle is greater than 180 degrees, swap the angle
        if (Math.abs(angleIn - angleOut) > 180)
            angle = angle - 180;

        return angle;
    }

    public static double calculateRange(SurveyPoint start, SurveyPoint end) {
        return CalculateRangem(start.lat, start.lon, end.lat, end.lon);
    }

    public static double calculateAngle(SurveyPoint start, SurveyPoint end) {
        return deg360(CalculateAngledeg(start.lat, start.lon, end.lat, end.lon));
    }

    public static double[] calculateRangeAngle(SurveyPoint start,
            SurveyPoint end) {
        return CalculateRangeAngle(start.lat, start.lon, end.lat, end.lon);
    }

    public static double[] CalculateRangeAngle(
            double startLat, double startLon, double endLat, double endLon) {
        double[] ra = CalculateRangeandAngle(startLat, startLon, endLat, endLon);

        // Sanitize output
        for (int i = 0; i < ra.length; i++)
            ra[i] = deNaN(ra[i]);
        return ra;
    }

    public static double[] CalculateRangeAngleElev(
            SurveyPoint start, SurveyPoint end) {
        return CalculateRangeAngleElev(
                start.lat, start.lon, start.getHAE(),
                end.lat, end.lon, end.getHAE());
    }

    public static double[] CalculateRangeAngleElev(
            double StartLat, double StartLon, double Hae,
            double EndLat, double EndLon, double EndHae) {
        if (Hae >= Altitude.INVALID)
            Hae = 0.0d;
        if (EndHae >= Altitude.INVALID)
            EndHae = 0.0d;

        double[] rae = CalculateRangeandAngleElev(StartLat, StartLon, Hae,
                EndLat, EndLon, EndHae);

        // Sanitize output
        for (int i = 0; i < rae.length; i++)
            rae[i] = deNaN(rae[i]);
        return rae;
    }

    public static double[] AROffset(
            double startlat, double startlon, double hae_m,
            double Azimuth, double Range_m, double ElevationAngle) {
        boolean invalid = hae_m >= Altitude.INVALID;
        if (invalid)
            hae_m = 0.0d;

        Azimuth = deNaN(Azimuth);
        Range_m = deNaN(Range_m);
        ElevationAngle = deNaN(ElevationAngle);

        double[] ret = AROffsetElev(startlat, startlon, hae_m,
                Azimuth, Range_m, ElevationAngle);
        if (invalid)
            ret[2] = Altitude.INVALID;
        return ret;
    }

    public static double ConvertMSLtoHAE(
            double Latitude, double Longitude, double Geoid_Height) {
        if (Geoid_Height >= Altitude.INVALID)
            return Altitude.INVALID;
        return ConvertGeoidToEllipsoidHeight(Latitude, Longitude, Geoid_Height);
    }

    public static double ConvertHAEtoMSL(
            double Latitude, double Longitude, double Ellipsoid_Height) {
        if (Ellipsoid_Height >= Altitude.INVALID)
            return Altitude.INVALID;
        return ConvertEllipsoidToGeoidHeight(Latitude, Longitude,
                Ellipsoid_Height);
    }

    public static SurveyPoint AROffset(SurveyPoint start, double angle,
            double range_m) {
        double[] latlon = AROffset(start.lat, start.lon, angle, range_m);
        return new SurveyPoint(latlon[0], latlon[1]);
    }

    /**
     * Given an initial point, angle and range compute the resulting survey point.
     * The return is an array with [0] lat and [1] lon.
     */
    public static native double[] AROffset(double startlat, double startlon,
            double angle, double range_m);

    /*input: position in degrees, elevation in meters,  Az and El Angles in Degrees
     * output:appears to return Azimuth, Range, Elevation Angle
     * */
    private static native double[] AROffsetElev(double startlat,
            double startlon,
            double hae_m, double Azimuth, double Range_m, double ElevationAngle);

    private static native double[] CalculateRangeandAngle(double StartLat,
            double StartLon, double EndLat, double EndLon);

    private static native double[] CalculateRangeandAngleElev(double StartLat,
            double StartLon, double Hae, double EndLat, double EndLon,
            double EndHae);

    public static native double CalculateRangem(double startlat,
            double startlon, double endlat, double endlon);

    public static native double CalculateAngledeg(double startlat,
            double startlon, double endlat, double endlon);

    public static native String GetLatLonDMS(double Lat, double Lon);

    public static String GetLatLonDM(double Lat, double Lon) {
        return GetLatDM(Lat) + " " + GetLonDM(Lon);
    }

    public static native String GetLatDM(double Lat);

    public static native String GetLonDM(double Lon);

    public static native long GetGeoidHeight(double Lat, double Lon);

    private static native double ConvertGeoidToEllipsoidHeight(double Latitude,
            double Longitude, double Geoid_Height);

    private static native double ConvertEllipsoidToGeoidHeight(double Latitude,
            double Longitude, double Ellipsoid_Height);

    public static native String GetUTMHemisphereZone(double Lat, double Lon);

    public static native String GetUTMEasting(double Lat, double Lon);

    public static native String GetUTMNorthing(double Lat, double Lon);

    private static native String GetMGRS(double Lat, double Lon, int Digits);

    public static native double GetMGRSHeadingOffset(double lat, double lon,
            double heading);

    public static String GetUTMNorthingShort(double Lat, double Lon) {
        String s = GetUTMNorthing(Lat, Lon);

        for (int i = s.length(); i < 7; ++i)
            s = "0" + s;
        return s.substring(0, 2);

    }

    public static String GetUTMEastingShort(double Lat, double Lon) {
        String s = GetUTMEasting(Lat, Lon);
        for (int i = s.length(); i < 7; ++i)
            s = "0" + s;
        return s.substring(0, 2);
    }

    public static String GetMGRS(double Lat, double Lon) {
        if (Lat < -90 || Lat > 90)
            return null;
        if (Lon < -180 || Lon > 180)
            return null;
        return GetMGRS(Lat, Lon, 5);
    }

    public static String GetMGRS(SurveyPoint sp) {
        return GetMGRS(sp.lat, sp.lon);
    }

    /**
     * Based on an existing table (WMM2010), provide the proper declination correction
     * for a specific angle at a latitude and longitude.
     */
    public static double GetMagAngle(double TrueAngle, double Lat, double Lon) {
        final double dec = Conversions.GetDeclination(Lat, Lon, 0);
        return deg360(TrueAngle - dec);
    }

    public static double GetTrueAngle(double MagAngle, double Lat, double Lon) {
        final double dec = Conversions.GetDeclination(Lat, Lon, 0);
        return deg360(MagAngle + dec);
    }

    public static double GetDeclination(final double Lat, final double Lon,
            final double alt) {
        GeomagneticField geo = new GeomagneticField((float) Lat, (float) Lon,
                (float) alt, System.currentTimeMillis());
        return geo.getDeclination();
    }

    public static String getUTMString(double Lat, double Lon) {

        if ((Lat < MIN_LAT) || (Lat > MAX_LAT)) {
            Lat = Double.NaN;
            Lon = Double.NaN;
        }

        String utmString = "";
        String zone = GetUTMHemisphereZone(Lat, Lon);
        zone = zone.replace(" ", ""); //MIKE a quick fix
        String northing = GetUTMNorthing(Lat, Lon);
        String easting = GetUTMEasting(Lat, Lon);

        utmString = zone + " " + easting + " " + northing;
        return utmString;
    }

    public static String ConvertGlideSlopeAngleToRatio(double angle_deg,
            boolean form) {
        //this will convert x_degrees to a xx:1 ratio.

        //1:ratio# = 1/tan(angle_deg)

        String ratio = "";

        double rationum = 1 / Math.tan(angle_deg * Conversions.DEG2RAD);

        if (rationum >= 99)
            ratio = (form ? UNOBSTRUCTED : "99:1");
        else
            ratio = String.valueOf(((int) Math.round(rationum))) + ":1";
        return ratio;
    }

    public static String ConvertGlideSlopeAngleToRatio(double angle_deg) {
        return ConvertGlideSlopeAngleToRatio(angle_deg, false);
    }

    public static double ConvertGlideSlopeRatioToAngle_deg(String ratio) {
        if (ratio.equals(UNOBSTRUCTED))
            ratio = "99:1";

        double angleResult = 0;
        //trim the "1
        ratio = ratio.trim();
        ratio = ratio.replace(":1", "");

        double rationum = Double.valueOf(ratio);

        if (!Double.isNaN(rationum)) {
            double angle = Math.atan(1 / rationum);
            angleResult = angle * Conversions.RAD2DEG;
        }

        return angleResult;

    }

    // Gson helpers - use these to avoid crashes with NaN double values
    public static String toJson(Object o) {
        if (o != null) {
            GsonBuilder gb = new GsonBuilder();
            gb.serializeSpecialFloatingPointValues();
            return gb.create().toJson(o);
        }
        return "";
    }

    public static <T> T fromJson(String data, Class<T> cls) {
        GsonBuilder gb = new GsonBuilder();
        gb.serializeSpecialFloatingPointValues();
        return gb.create().fromJson(data, cls);
    }
}
