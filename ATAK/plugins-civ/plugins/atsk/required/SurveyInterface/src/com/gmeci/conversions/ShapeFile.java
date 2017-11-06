
package com.gmeci.conversions;

import android.util.Log;

import com.gmeci.core.SurveyPoint;

import java.io.File;

/**
 * Save survey as shape file
 */
public class ShapeFile {

    private static final String TAG = "ShapeFile";

    static {
        System.loadLibrary("shapelib-1.3.0");
    }

    public static boolean savePolygon(File outFile, String label,
            SurveyPoint[] points) {
        double[] lat = new double[points.length];
        double[] lon = new double[points.length];
        int i = 0;
        for (SurveyPoint sp : points) {
            lat[i] = sp.lat;
            lon[i++] = sp.lon;
        }
        String result = savePolygon(outFile.getAbsolutePath(), label, lat, lon);
        if (!result.isEmpty()) {
            Log.e(TAG, "savePolygon returned: " + result);
            return false;
        }
        return true;
    }

    public static native String savePolygon(String filePath, String label,
            double[] lat, double[] lon);

    public static boolean saveArc(File outFile, String label,
            SurveyPoint[] points) {
        double[] lat = new double[points.length];
        double[] lon = new double[points.length];
        int i = 0;
        for (SurveyPoint sp : points) {
            lat[i] = sp.lat;
            lon[i++] = sp.lon;
        }
        String result = saveArc(outFile.getAbsolutePath(), label, lat, lon);
        if (!result.isEmpty()) {
            Log.e(TAG, "saveArc returned: " + result);
            return false;
        }
        return true;
    }

    public static native String saveArc(String filePath, String label,
            double[] lat, double[] lon);

    public static boolean savePoint(File outFile, String label,
            SurveyPoint point) {
        String result = savePoint(outFile.getAbsolutePath(), label, point.lat,
                point.lon);
        if (!result.isEmpty()) {
            Log.e(TAG, "savePoint returned: " + result);
            return false;
        }
        return true;
    }

    public static native String savePoint(String filePath, String label,
            double lat, double lon);

}
