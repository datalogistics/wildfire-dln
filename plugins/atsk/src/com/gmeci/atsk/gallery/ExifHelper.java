
package com.gmeci.atsk.gallery;

/**
 * Temporary duplicate of ATAK ExifHelper
 */

import android.content.Context;
import android.hardware.GeomagneticField;
import android.media.ExifInterface;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import com.atakmap.android.elev.dt2.Dt2ElevationModel;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.PointMapItem;
import com.atakmap.android.util.ATAKUtilities;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.conversion.EGM96;
import com.atakmap.coremap.maps.coords.Altitude;
import com.atakmap.coremap.maps.coords.AltitudeReference;
import com.atakmap.coremap.maps.coords.AltitudeSource;
import com.atakmap.coremap.maps.coords.GeoPoint;

import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.ImageWriteException;
import org.apache.sanselan.Sanselan;
import org.apache.sanselan.common.IImageMetadata;
import org.apache.sanselan.formats.jpeg.JpegImageMetadata;
import org.apache.sanselan.formats.jpeg.exifRewrite.ExifRewriter;
import org.apache.sanselan.formats.tiff.TiffField;
import org.apache.sanselan.formats.tiff.TiffImageMetadata;
import org.apache.sanselan.formats.tiff.constants.ExifTagConstants;
import org.apache.sanselan.formats.tiff.constants.TagInfo;
import org.apache.sanselan.formats.tiff.constants.TiffConstants;
import org.apache.sanselan.formats.tiff.constants.TiffDirectoryConstants;
import org.apache.sanselan.formats.tiff.fieldtypes.FieldType;
import org.apache.sanselan.formats.tiff.write.TiffOutputDirectory;
import org.apache.sanselan.formats.tiff.write.TiffOutputField;
import org.apache.sanselan.formats.tiff.write.TiffOutputSet;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Reference:
 *    https://android.googlesource.com/platform/frameworks/base/+/cd92588/media/java/android/media/ExifInterface.java
 */

public class ExifHelper {

    public static final String TAG = "ExifHelper";

    private static String convertDoubleStringToRationalString(String string) {
        int decimalIndex = string.indexOf('.');
        double convertedNum = Double.parseDouble(string);
        if (convertedNum > -1 && decimalIndex > -1)
        {
            int decimalPlace = string.length() - decimalIndex - 1;
            int numerator = (int) (convertedNum * Math.pow(10, decimalPlace));
            int denominator = (int) Math.pow(10, decimalPlace);
            return numerator + "/" + denominator;
        }

        return string + "/1";
    }

    /**
     * format for encoding the latitude or longitude in EXIF data
     */
    private static String convert(double tude) {
        // absolute value
        tude = Math.abs(tude);

        // extract degree
        int degree = (int) tude;
        tude *= 60;
        tude -= (degree * 60.0d);

        // extract minute
        int minute = (int) tude;
        tude *= 60;
        tude -= (minute * 60.0d);

        // extract second
        int second = (int) (tude * 10000.0d);

        String sb = String.valueOf(degree) +
                "/1," +
                minute +
                "/1," +
                second +
                "/10000";
        return sb;
    }

    /**
     * Convert latlng value to DMS number array
     */
    private static Number[] getDMSArray(double tude) {
        tude = Math.abs(tude);
        int deg = (int) tude;
        tude *= 60;
        tude -= (deg * 60.0d);
        int min = (int) tude;
        tude *= 60;
        tude -= (min * 60.0d);
        return new Number[] {
                deg, min, Math.round(tude * 10000.0f) / 10000.0f
        };
    }

    /**
     * Obtains the current image offset.
     */
    static private double getImageOrientation(final int orientationFlag) {
        double orientationOffset = 0d;
        switch (orientationFlag) {
            case ExifInterface.ORIENTATION_NORMAL:
                orientationOffset = 0d;
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                orientationOffset = 180d;
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                orientationOffset = 270d;
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                orientationOffset = 90d;
                break;
        }
        return orientationOffset;
    }

    public static int getImageOrientation(File img) {
        TiffImageMetadata exif = getExifMetadata(img);
        if (exif != null) {
            return (int) getImageOrientation(getInt(exif,
                    TiffConstants.EXIF_TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL));
        }
        return 0;
    }

    /**
     * Obtains the current window offset.
     */
    static private double getWindowOffset(final Context context) {
        double orientationOffset = 0d;
        try {
            WindowManager _winManager =
                    (WindowManager) context
                            .getSystemService(Context.WINDOW_SERVICE);
            Display display = _winManager.getDefaultDisplay();
            int rotation = display.getRotation();
            switch (rotation) {
                case Surface.ROTATION_0:
                    orientationOffset = 0d;
                    break;
                case Surface.ROTATION_90:
                    orientationOffset = 90d;
                    break;
                case Surface.ROTATION_180:
                    orientationOffset = 180d;
                    break;
                case Surface.ROTATION_270:
                    orientationOffset = 270d;
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG,
                    "Error has occurred getting the window and rotation, setting 0",
                    e);
            orientationOffset = 0d;
        }
        return orientationOffset;
    }

    /**
     * Computes the required correction to the image based on the orientation of the
     * of ATAK vs the orienatation of the image.
     */
    static private double computeCorrection(double iO, double aO) {
        Log.d(TAG, "captured image orienation: " + iO
                + " map view orientation: " + aO);
        if (iO == 90d && aO == 90d)
            return -90d;
        else if (iO == 180d && aO == 90d)
            return +180d;
        else if (iO == 270d && aO == 90d)
            return +90d;

        else if (iO == 0d && aO == 0d)
            return +90d;
        else if (iO == 180d && aO == 0d)
            return -90d;
        else if (iO == 270d && aO == 0d)
            return +180d;

        else if (iO == 0d && aO == 270d)
            return +180d;
        else if (iO == 90d && aO == 270d)
            return +90d;
        else if (iO == 270d && aO == 270d)
            return -90d;

        else
            return 0d;
    }

    public static TiffImageMetadata getExifMetadata(File jpegFile) {
        try {
            IImageMetadata metadata = Sanselan.getMetadata(jpegFile);
            JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;
            if (jpegMetadata != null)
                return jpegMetadata.getExif();
        } catch (Exception e) {
            Log.w(TAG, "Error getting exif metadata from "
                    + jpegFile.getAbsolutePath());
        }
        return null;
    }

    public static TiffOutputSet getExifOutput(TiffImageMetadata exif) {
        TiffOutputSet tos = null;
        // Get existing output set
        if (exif != null) {
            try {
                tos = exif.getOutputSet();
            } catch (Exception e) {
                Log.w(TAG, "Error getting exif output set.");
            }
        }
        // Create new output set if we couldn't get the existing one
        if (tos == null)
            tos = new TiffOutputSet();
        return tos;
    }

    public static double getDouble(TiffImageMetadata exif, TagInfo tag,
            double defaultValue) {
        if (exif != null && tag != null) {
            try {
                TiffField tf = exif.findField(tag);
                if (tf != null)
                    return tf.getDoubleValue();
            } catch (ImageReadException e) {
                Log.w(TAG, "Failed to read EXIF tag " + tag.name
                        + " as double.");
            }
        }
        return defaultValue;
    }

    public static double[] getDoubleArray(TiffImageMetadata exif, TagInfo tag,
            double[] defaultValue) {
        if (exif != null && tag != null) {
            try {
                TiffField tf = exif.findField(tag);
                if (tf != null)
                    return tf.getDoubleArrayValue();
            } catch (ImageReadException e) {
                Log.w(TAG, "Failed to read EXIF tag " + tag.name
                        + " as double array.");
            }
        }
        return defaultValue;
    }

    public static int getInt(TiffImageMetadata exif, TagInfo tag,
            int defaultValue) {
        if (exif != null && tag != null) {
            try {
                TiffField tf = exif.findField(tag);
                if (tf != null)
                    return tf.getIntValue();
            } catch (ImageReadException e) {
                Log.w(TAG, "Failed to read EXIF tag " + tag.name
                        + " as integer.");
            }
        }
        return defaultValue;
    }

    public static String getString(TiffImageMetadata exif, TagInfo tag,
            String defaultValue) {
        if (exif != null && tag != null) {
            try {
                TiffField tf = exif.findField(tag);
                if (tf != null) {
                    String ret = tf.getStringValue();
                    return (ret == null ? defaultValue : ret);
                }
            } catch (ImageReadException e) {
                Log.w(TAG, "Failed to read EXIF tag " + tag.name
                        + " as string.");
            }
        }
        return defaultValue;
    }

    public static double getAltitude(TiffImageMetadata exif, double defaultValue) {
        double alt = getDouble(exif,
                TiffConstants.GPS_TAG_GPS_ALTITUDE, Double.NaN);
        if (!Double.isNaN(alt)) {
            if (getInt(exif,
                    TiffConstants.GPS_TAG_GPS_ALTITUDE_REF, 0) == 1)
                alt *= -1;
            return alt;
        }
        return defaultValue;
    }

    public static double[] getLatLon(TiffImageMetadata exif) {
        double latitude = Double.NaN, longitude = Double.NaN;
        if (exif != null) {
            try {
                TiffImageMetadata.GPSInfo latlng = exif.getGPS();
                if (latlng != null) {
                    latitude = latlng.getLatitudeAsDegreesNorth();
                    longitude = latlng.getLongitudeAsDegreesEast();
                }
            } catch (ImageReadException e) {
                Log.w(TAG, "Failed to read EXIF GPS location.");
            }
        }
        return new double[] {
                latitude, longitude
        };
    }

    public static GeoPoint getLocation(TiffImageMetadata exif) {
        double[] latlon = getLatLon(exif);
        Altitude alt = new Altitude(getAltitude(exif,
                Altitude.UNKNOWN.getValue()),
                AltitudeReference.MSL, AltitudeSource.GPS);
        return new GeoPoint(latlon[0], latlon[1], alt,
                GeoPoint.CE90_UNKNOWN, GeoPoint.LE90_UNKNOWN);
    }

    public static String getDescription(File img) {
        TiffImageMetadata exif = getExifMetadata(img);
        return getString(exif, TiffConstants.TIFF_TAG_IMAGE_DESCRIPTION, "");
    }

    /**
     * Convert JSON string in UserComment to a Map (GeoTakCam images only)
     * Fields include:
     * - ImgPitch (float)
     * - ImgRoll (float)
     * - Inclination (float)
     * - HorizontalFOV (float)
     * - VerticalFOV (float)
     * - Address (string)
     */
    public static void getExtras(TiffImageMetadata exif,
            Map<String, Object> bundle) {
        if (exif == null || bundle == null)
            return;
        String jsonString = getString(exif,
                TiffConstants.EXIF_TAG_USER_COMMENT, null);
        if (jsonString != null && jsonString.length() > 0) {
            try {
                JSONObject jo = new JSONObject(jsonString);
                Iterator iter = jo.keys();
                while (iter.hasNext()) {
                    String key = (String) iter.next();
                    bundle.put(key, jo.get(key));
                    //Log.d(TAG, "Bundle[" + key + "] = " + jo.get(key));
                }
            } catch (Exception e) {
                Log.w(TAG,
                        "Failed to parse EXIF UserComment as JSON: "
                                + e.getMessage());
            }
        }
    }

    public static void putExtras(TiffImageMetadata exif,
            Map<String, Object> bundle, TiffOutputSet tos) {
        if (exif == null || bundle == null)
            return;
        try {
            // Need to remove UserComment from IFD 0 first
            tos.removeField(TiffConstants.EXIF_TAG_USER_COMMENT);
            // Then write it back to Sub IFD
            JSONObject jo = new JSONObject(bundle);
            updateField(tos, TiffConstants.EXIF_TAG_USER_COMMENT,
                    jo.toString());
        } catch (Exception e) {
            Log.w(TAG, "Failed to putExtras", e);
        }
    }

    public static Object getExtra(TiffImageMetadata exif, String key) {
        Map<String, Object> map = new HashMap<String, Object>();
        getExtras(exif, map);
        return map.get(key);
    }

    public static int getExtraInt(TiffImageMetadata exif,
            String key, int defaultValue) {
        Object v = getExtra(exif, key);
        try {
            if (v != null)
                return Integer.parseInt(String.valueOf(v));
        } catch (Exception e) {
            Log.w(TAG, "getExtraInt: " + key
                    + " is not an integer value (" + v + ")", e);
        }
        return defaultValue;
    }

    public static float getExtraFloat(TiffImageMetadata exif,
            String key, float defaultValue) {
        Object v = getExtra(exif, key);
        try {
            if (v != null)
                return Float.parseFloat(String.valueOf(v));
        } catch (Exception e) {
            Log.w(TAG, "getExtraFloat: " + key
                    + " is not a floating-point value (" + v + ")", e);
        }
        return defaultValue;
    }

    public static boolean getExtraBoolean(TiffImageMetadata exif,
            String key, boolean defaultValue) {
        Object v = getExtra(exif, key);
        try {
            if (v != null)
                return Boolean.parseBoolean(String.valueOf(v));
        } catch (Exception e) {
            Log.w(TAG, "getExtraBoolean: " + key
                    + " is not a boolean value (" + v + ")", e);
        }
        return defaultValue;
    }

    public static String getExtraString(TiffImageMetadata exif,
            String key, String defaultValue) {
        Object v = getExtra(exif, key);
        try {
            if (v != null)
                return String.valueOf(v);
        } catch (Exception e) {
            Log.w(TAG, "getExtraString: " + key
                    + " is not a string value (" + v + ")", e);
        }
        return defaultValue;
    }

    public static boolean updateField(TiffOutputSet set, TagInfo tag,
            Object data) {
        if (set != null && tag != null) {
            try {
                TiffOutputDirectory tod;
                if (tag.directoryType.directoryType == TiffDirectoryConstants.DIRECTORY_TYPE_GPS)
                    tod = set.getOrCreateGPSDirectory();
                else
                    tod = set.getOrCreateExifDirectory();
                tod.removeField(tag);
                TiffOutputField outputField = null;
                if (data instanceof String) {
                    if (tag.dataTypes[0] == ExifTagConstants.FIELD_TYPE_UNDEFINED) {
                        // Undefined field types require an 8-byte encoding indentifier
                        byte[] marker = new byte[] {
                                0x41, 0x53, 0x43,
                                0x49, 0x49, 0x00, 0x00, 0x00
                        };
                        byte[] comment = ((String) data).getBytes("US-ASCII");
                        byte[] bytesComment =
                                new byte[marker.length + comment.length];
                        System.arraycopy(marker, 0, bytesComment, 0,
                                marker.length);
                        System.arraycopy(comment, 0, bytesComment,
                                marker.length, comment.length);
                        outputField = new TiffOutputField(
                                tag, tag.dataTypes[0],
                                bytesComment.length, bytesComment);
                    } else
                        outputField = TiffOutputField.create(tag,
                                set.byteOrder,
                                (String) data);
                } else if (data instanceof Number)
                    outputField = TiffOutputField.create(tag, set.byteOrder,
                            (Number) data);
                else if (data instanceof Number[])
                    outputField = TiffOutputField.create(tag, set.byteOrder,
                            (Number[]) data);
                else
                    throw new ImageWriteException("Invalid data type.");
                tod.add(outputField);
                return true;
            } catch (Exception e) {
                StringBuilder exc = new StringBuilder(
                        "Failed to write EXIF tag ");
                exc.append(tag.name);
                exc.append(" as ");
                exc.append(data.getClass());
                // List valid data types
                exc.append("\nExpected data types [");
                exc.append(tag.length);
                exc.append("]:");
                for (FieldType type : tag.dataTypes) {
                    exc.append("\n\t");
                    exc.append(type.name);
                }
                Log.e(TAG, exc.toString(), e);
            }
        }
        return false;
    }

    public static boolean saveExifOutput(TiffOutputSet tos, File imageFile) {
        BufferedOutputStream bos = null;
        // Obtain unmodified image byte array first
        try {
            byte[] imageData = FileSystemUtils.read(imageFile);
            bos = new BufferedOutputStream(new FileOutputStream(imageFile));
            // Then update and save
            new ExifRewriter().updateExifMetadataLossless(imageData, bos, tos);
            bos.close();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to save EXIF output to "
                    + imageFile.getAbsolutePath(), e);
        } finally {
            try {
                if (bos != null)
                    bos.close();
            } catch (IOException e) {
            }
        }
        return false;
    }

    /**
     * Use EXIF location, otherwise use self location, default to last known location...
     *
     * @return GeoPoint location based on EXIF location
     */
    public static GeoPoint fixImage(final MapView _mapView, final String path) {
        try {
            // Use sanselan to get jpeg exif metadata instead of built-in library
            // which is unable to read most of the GPS tags
            boolean changed = false;
            File imageFile = new File(path);
            TiffImageMetadata exif = getExifMetadata(imageFile);
            if (exif == null)
                return null;
            TiffOutputSet tos = getExifOutput(exif);

            // Check for existing exif metadata
            double latitude = Double.NaN, longitude = Double.NaN;
            TiffImageMetadata.GPSInfo latlng = exif.getGPS();
            if (latlng != null) {
                latitude = latlng.getLatitudeAsDegreesNorth();
                longitude = latlng.getLongitudeAsDegreesEast();
            }
            // Azimuth
            boolean trueNorth = getString(exif,
                    TiffConstants.GPS_TAG_GPS_IMG_DIRECTION_REF, "M").equals(
                    "T");
            double direction = getDouble(exif,
                    TiffConstants.GPS_TAG_GPS_IMG_DIRECTION, Double.NaN);

            // Altitude (MSL)
            double alt = getAltitude(exif, Double.NaN);

            // Circular error (CE)
            double ce = getDouble(exif,
                    TiffConstants.GPS_TAG_GPS_DOP, Double.NaN);

            // Self-marker point (use as default for missing data)
            // If self-marker isn't set then use the middle of the screen
            MapItem item = ATAKUtilities.findSelf(_mapView);
            GeoPoint sp = (item != null ? ((PointMapItem) item).getPoint()
                    : _mapView.getPoint());

            // Fill in missing exif data

            // Missing latitude
            double newAlt = alt, newDir = direction;
            if (Double.isNaN(latitude)) {
                latitude = sp.getLatitude();
                updateField(tos, TiffConstants.GPS_TAG_GPS_LATITUDE,
                        getDMSArray(latitude));
                updateField(tos, TiffConstants.GPS_TAG_GPS_LATITUDE_REF,
                        latitude < 0.0d ? "S" : "N");
                Log.d(TAG, "Image latitude is invalid, using self-marker: "
                        + latitude);
                changed = true;
            }
            // Missing longitude
            if (Double.isNaN(longitude)) {
                longitude = sp.getLongitude();
                updateField(tos, TiffConstants.GPS_TAG_GPS_LONGITUDE,
                        getDMSArray(longitude));
                updateField(tos, TiffConstants.GPS_TAG_GPS_LONGITUDE_REF,
                        longitude < 0.0d ? "W" : "E");
                Log.d(TAG, "Image longitude is invalid, using self-marker: "
                        + longitude);
                changed = true;
            }
            Altitude altitude = Altitude.UNKNOWN;

            // Missing altitude
            if (Double.isNaN(newAlt)) {
                // Self-marker altitude
                Altitude spAltitude = EGM96.getInstance().getMSL(sp);
                // If we're using the self-marker location, use the altitude
                // since it may be more accurate than DTED
                if (latitude == sp.getLatitude()
                        && longitude == sp.getLongitude())
                    altitude = spAltitude;
                // Then try DTED
                if (!altitude.isValid()) {
                    Dt2ElevationModel ele = Dt2ElevationModel.getInstance();
                    altitude = ele.queryPoint(latitude, longitude);
                    // If that doesn't work, revert back to self-marker altitude
                    if (!altitude.isValid())
                        altitude = spAltitude;
                }
                // Set new altitude
                if (altitude.isValid()) {
                    newAlt = altitude.getValue();
                    Log.d(TAG,
                            "Image altitude is invalid, using DTED/self-marker: "
                                    + newAlt);
                    updateField(tos, new TagInfo("GPSAltitude", 0x06,
                            TiffConstants.FIELD_TYPE_DESCRIPTION_RATIONAL, 1,
                            TiffConstants.EXIF_DIRECTORY_GPS),
                            Math.abs(altitude.getValue()));
                    changed = true;
                }
            } else
                // EXIF altitude
                altitude = new Altitude(newAlt, AltitudeReference.MSL,
                        AltitudeSource.GPS);

            // Missing circular error
            if (Double.isNaN(ce)) {
                ce = sp.getCE();
                updateField(tos, new TagInfo("GPSDOP", 0x0B,
                        TiffConstants.FIELD_TYPE_DESCRIPTION_RATIONAL, 1,
                        TiffConstants.EXIF_DIRECTORY_GPS), ce);
                Log.d(TAG, "Image CE is invalid, using self-marker: " + ce);
                changed = true;
            }

            // Missing azimuth
            if (Double.isNaN(newDir)) {
                // use the ATAK computed device azimuth
                newDir = _mapView.getMapData().getDouble(
                        "deviceAzimuth",
                        Double.NaN);
                if (!Double.isNaN(newDir)) {
                    // Calculate azimuth based on device orientation
                    final int oFlag = getInt(exif,
                            TiffConstants.EXIF_TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_NORMAL);
                    final double iO = getImageOrientation(oFlag);
                    final double aO = getWindowOffset(_mapView.getContext());
                    newDir = newDir + computeCorrection(iO, aO) + 360d;
                    while (newDir > 360)
                        newDir = newDir - 360d;

                    Log.d(TAG,
                            "computed GPSImgDirection based on orientation: "
                                    + newDir);
                }
                trueNorth = true;
            } else if (trueNorth) {
                // Need to compute magnetic north from true north
                GeomagneticField gf = new GeomagneticField((float) latitude,
                        (float) longitude, (float) EGM96.getInstance().
                                getHAE(latitude, longitude, altitude)
                                .getValue(),
                        System.currentTimeMillis());
                double declination = gf.getDeclination();
                newDir -= declination;
                Log.d(TAG, "Corrected GPSImgDirection azimuth to " + newDir
                        + " (declination = " + declination + ")");
            }

            // Update EXIF data
            if (Double.compare(direction, newDir) != 0) {
                updateField(tos, new TagInfo("GPSImgDirection", 0x11,
                        TiffConstants.FIELD_TYPE_DESCRIPTION_RATIONAL, 1,
                        TiffConstants.EXIF_DIRECTORY_GPS), newDir);
                changed = true;
            }
            if (trueNorth) {
                updateField(tos, new TagInfo("GPSImgDirectionRef", 0x10,
                        TiffConstants.FIELD_TYPE_DESCRIPTION_ASCII, 1,
                        TiffConstants.EXIF_DIRECTORY_GPS), "M");
                changed = true;
            }
            if (altitude.isValid()
                    && Double.compare(alt, altitude.getValue()) != 0) {
                updateField(tos, new TagInfo("GPSAltitudeRef", 0x05,
                        TiffConstants.FIELD_TYPE_DESCRIPTION_BYTE, 1,
                        TiffConstants.EXIF_DIRECTORY_GPS),
                        (byte) (altitude.getValue() > 0 ? 0 : 1));
                changed = true;
            }

            // do not use the device heading
            //double heading = ((Marker) item).getTrackHeading();

            // To prevent changing the lastModified time for no reason
            if (changed) {
                saveExifOutput(tos, imageFile);
                Log.d(TAG, "captured image and corrected/added metadata");
            }
            // Return geopoint of image
            return new GeoPoint(latitude, longitude, altitude, ce,
                    GeoPoint.LE90_UNKNOWN);
        } catch (Exception e) {
            Log.e(TAG, "error correcting the exif data", e);
        }
        // else return the center point of the current map view.
        Log.d(TAG, "captured image and placed without geospatial information");
        return _mapView.getPoint();
    }

}
