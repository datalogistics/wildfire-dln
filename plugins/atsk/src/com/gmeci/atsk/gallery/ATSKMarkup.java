
package com.gmeci.atsk.gallery;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Region;
import android.media.ExifInterface;
import android.preference.PreferenceManager;

import com.atakmap.android.atsk.plugin.R;
import com.atakmap.android.imagecapture.TextRect;
import com.atakmap.android.imagecapture.TiledCanvas;
import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.conversion.EGM96;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.gmeci.atsk.resources.ATSKApplication;
import com.gmeci.conversions.Conversions;
import com.gmeci.conversions.Conversions.Unit;
import com.gmeci.core.ATSKConstants;

import org.apache.sanselan.formats.tiff.TiffImageMetadata;
import org.apache.sanselan.formats.tiff.constants.TiffConstants;
import org.apache.sanselan.formats.tiff.write.TiffOutputSet;

import java.io.File;
import java.util.Date;
import java.util.Formatter;
import java.util.HashMap;
import com.atakmap.coremap.locale.LocaleUtil;

public class ATSKMarkup {

    public static final String TAG = "ATSKMarkup";
    public static final String KEY_MARKUP = "Markup";
    public static final String KEY_MARKUP_DESC = "MarkupDesc";
    public static final String KEY_MARKUP_VERSION = "MarkupVersion";
    public static final int MARKUP_VERSION = 2;

    private static final int DEG_90 = 90;
    private static final int DEG_360 = 360;
    private static final int TILE_SIZE = 1024;

    private static float _dp = 1.0f;

    /**
     * Apply image markup to input file (without reading entire image into memory)
     * @param inFile Input image
     * @param outFile Output image containing markup
     * @param cb Callback to fire when markup is finished
     * @return True if successful
     */
    public static TiledCanvas applyMarkup(File inFile, File outFile,
            ProgressCallback cb) {
        if (inFile == null)
            return null;

        // Load tiled bitmap
        TiledCanvas in = new TiledCanvas(inFile, TILE_SIZE, TILE_SIZE);
        if (!in.valid())
            return null;

        // Create TIFF copy for writing to
        String[] options = {
                "TILED=YES"
        };
        Log.d(TAG, "Creating markup image: " + inFile.getAbsolutePath());
        File tmp = new File(outFile.getAbsolutePath() + "_tmp.tiff");
        in.copyToFile(tmp, "GTiff", options);

        TiffImageMetadata exif = ExifHelper.getExifMetadata(inFile);
        int rot = getRotation(exif);

        // Load output tiled bitmap
        TiledCanvas out = new TiledCanvas(tmp, TILE_SIZE, TILE_SIZE);
        try {
            int prog = 0, maxProg = in.getTileCount();
            if (!out.valid())
                return null;

            // Apply markup to each tile
            for (int y = 0; y < in.getTileCountY(); y++) {
                for (int x = 0; x < in.getTileCountX(); x++) {
                    Bitmap tile = in.readTile(x, y);
                    if (tile == null)
                        return null;
                    float tileX = x * TILE_SIZE, tileY = y * TILE_SIZE;
                    float tileRight = Math.min((x + 1) * TILE_SIZE,
                            out.getWidth());
                    float tileBottom = Math.min((y + 1) * TILE_SIZE,
                            out.getHeight());
                    if (rot == 0)
                        out.translate(-tileX, -tileY);
                    else if (rot == 90)
                        out.translate(tileBottom - out.getHeight(), -tileX);
                    else if (rot == 270)
                        out.translate(-tileY, tileRight - out.getWidth());
                    else
                        out.translate(tileRight - out.getWidth(), tileBottom
                                - out.getHeight());
                    tile = applyMarkup(exif, tile, out);
                    if (!out.writeTile(tile, x, y)) {
                        Log.e(TAG, "Failed to apply markup to tile "
                                + x + ", " + y);
                        return null;
                    }
                    if (cb != null && !cb.onProgress(prog, maxProg))
                        return null;
                }
            }
            // Copy to JPEG
            out.copyToFile(outFile, Bitmap.CompressFormat.JPEG, 100);
            setMarkup(inFile, outFile, true);
            // Remove leftover file from GDAL
            FileSystemUtils.deleteFile(new File(
                    outFile.getAbsolutePath() + ".aux.xml"));
        } finally {
            // Delete temporary TIFF output
            FileSystemUtils.deleteFile(tmp);
        }
        return new TiledCanvas(outFile, TILE_SIZE, TILE_SIZE);
    }

    // Apply image markup to input file
    private static Bitmap applyMarkup(TiffImageMetadata exif, Bitmap tile,
            Canvas can) {

        Resources res = ATSKApplication.getInstance()
                .getPluginContext().getResources();
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(
                MapView.getMapView().getContext());

        // Load image metadata
        String desc = ExifHelper.getString(exif,
                TiffConstants.TIFF_TAG_IMAGE_DESCRIPTION, "");

        boolean marked = ExifHelper.getExtraBoolean(exif, KEY_MARKUP, false);
        String markupDesc = ExifHelper
                .getExtraString(exif, KEY_MARKUP_DESC, "");
        int markupVer = ExifHelper.getExtraInt(exif, KEY_MARKUP_VERSION, 1);
        boolean updateDesc = marked && !markupDesc.equals(desc)
                && !desc.isEmpty() && markupVer <= MARKUP_VERSION;
        if (marked && !updateDesc)
            return tile;

        char north = ExifHelper.getString(exif,
                TiffConstants.GPS_TAG_GPS_IMG_DIRECTION_REF, "M").charAt(0);
        double azimuth = ExifHelper.getDouble(exif,
                TiffConstants.GPS_TAG_GPS_IMG_DIRECTION, Double.NaN);
        double pitch = ExifHelper.getExtraFloat(exif, "ImgPitch", 0.0f);
        double roll = ExifHelper.getExtraFloat(exif, "ImgRoll", 0.0f);
        double hfov = ExifHelper.getExtraFloat(exif, "HorizontalFOV", 90.0f);
        double vfov = ExifHelper.getExtraFloat(exif, "VerticalFOV", 45.0f);
        int rot = getRotation(exif);
        int sori = ((rot / 90) + 3) % 4;

        // Rotate bitmap according to EXIF orientation
        int width = can.getWidth(), height = can.getHeight();
        if (rot != 0) {
            // Get rotation matrix
            Matrix matrix = new Matrix();
            matrix.postRotate(rot);

            // Create new rotated bitmap
            tile = Bitmap.createBitmap(tile, 0, 0, tile.getWidth(),
                    tile.getHeight(), matrix, false);
            if (rot == 90 || rot == 270) {
                width = can.getHeight();
                height = can.getWidth();
            }
        }

        _dp = (float) Math.min(width, height) / 360.0f;
        can.setBitmap(tile);

        // GPS location
        GeoPoint loc = ExifHelper.getLocation(exif);
        String mgrs = "Unknown Location";
        String alt = "Unknown Elevation";
        if (loc.isValid())
            mgrs = Conversions.GetMGRS(loc.getLatitude(), loc.getLongitude());
        if (loc.getAltitude().isValid()) {
            String pref = prefs.getString("alt_display_pref", "MSL");
            Unit altUnit = prefs.getString(ATSKConstants.UNITS_DISPLAY,
                    ATSKConstants.UNITS_FEET)
                    .equals(ATSKConstants.UNITS_METERS) ?
                    Unit.METER : Unit.FOOT;
            double altValue;
            if (pref.equals("HAE"))
                altValue = EGM96.getInstance().getHAE(loc).getValue();
            else {
                altValue = EGM96.getInstance().getMSL(loc).getValue();
                pref = "MSL";
            }
            alt = String.format(res.getString(R.string.atsk_alt),
                    Unit.METER.convertTo(altValue, altUnit), altUnit.getAbbr(),
                    pref);
        }

        // Correct azimuth based on preferences
        /*String northPref = prefs.getString(ATSKConstants.UNITS_ANGLE,
                ATSKConstants.UNITS_ANGLE_MAG);*/
        // Always use magnetic north (requested by VW)
        String northPref = ATSKConstants.UNITS_ANGLE_MAG;
        if (north == 'M' && !northPref.equals(ATSKConstants.UNITS_ANGLE_MAG)) {
            azimuth = Conversions.GetTrueAngle(azimuth, loc.getLatitude(),
                    loc.getLongitude());
            north = 'T';
        } else if (north == 'T'
                && !northPref.equals(ATSKConstants.UNITS_ANGLE_TRUE)) {
            azimuth = Conversions.GetMagAngle(azimuth, loc.getLatitude(),
                    loc.getLongitude());
            north = 'M';
        }

        // Date/time stamp
        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb, LocaleUtil.getCurrent());
        Date now = new Date(System.currentTimeMillis());
        formatter.format("%1$tY:%1$tm:%1$td", now);
        String dateStamp = ExifHelper.getString(exif,
                TiffConstants.GPS_TAG_GPS_DATE_STAMP, sb.toString());

        double[] hms = ExifHelper.getDoubleArray(exif,
                TiffConstants.GPS_TAG_GPS_TIME_STAMP,
                new double[] {
                        now.getHours(), now.getMinutes(), now.getSeconds()
                });
        String timeStamp = String.format(LocaleUtil.getCurrent(),
                "%1$02d:%2$02d:%3$02d",
                (int) hms[0], (int) hms[1], (int) hms[2]);

        Path path = new Path();

        // BG paint
        float bgThick = dp(48);
        Paint bp = new Paint();
        bp.setStyle(Paint.Style.FILL);

        // Text paint
        float crossWidth = dp(0.5f);
        float tickWidth = dp(2);
        float padding = dp(2);
        float borderWidth = dp(1);
        float titleSize = dp(12);
        float tickSize = dp(10);
        Paint tp = new Paint();
        tp.setAntiAlias(true);
        tp.setTextSize(titleSize);

        if (!updateDesc) {
            // Straight cross
            tp.setStrokeWidth(crossWidth);
            tp.setStyle(Paint.Style.STROKE);
            tp.setColor(Color.argb(128, 160, 160, 160));
            path.moveTo(0, height / 2);
            path.rLineTo(width, 0);
            path.moveTo(width / 2, 0);
            path.rLineTo(0, height);
            drawPathStroke(can, path, tp);
            path.reset();

            // Rolled cross
            tp.setColor(Color.rgb(160, 160, 160));
            double rad = Math
                    .toRadians(Math.round(roll) * (sori % 2 == 0 ? -1 : 1));
            float radius = Math.max(width, height);
            float c = (float) Math.cos(rad) * radius, s = (float) Math.sin(rad)
                    * radius;
            float hWidth = width / 2.0f, hHeight = height / 2.0f;
            path.moveTo(hWidth - c, hHeight - s);
            path.lineTo(hWidth + c, hHeight + s);
            path.moveTo(hWidth + s, hHeight - c);
            path.lineTo(hWidth - s, hHeight + c);
            drawPathStroke(can, path, tp);
            path.reset();

            // Text
            tp.setTextAlign(Paint.Align.LEFT);
            tp.setStrokeWidth(0);
            tp.setStyle(Paint.Style.FILL);
            drawTextStroke(can, "Roll: " + String.format(
                    res.getString(R.string.atsk_deg), roll),
                    padding, height / 2.0f - padding, tp);
        }

        tp.setColor(Color.rgb(160, 160, 160));
        TextRect descRect = new TextRect(tp, padding, desc);
        TextRect locRect = new TextRect(tp, padding, mgrs + " " + alt);
        TextRect timeRect = new TextRect(tp, padding, dateStamp + " "
                + timeStamp);

        float locTimeHeight = Math.max(locRect.height(), timeRect.height());
        float bottomBarHeight = locTimeHeight + (FileSystemUtils.isEmpty(desc)
                ? 0 : descRect.height());
        if (updateDesc) {
            bp.setColor(markupDesc.isEmpty() ? 0x80000000 : 0xFF000000);
            can.drawRect(0, height - bottomBarHeight, width - bgThick,
                    height - locTimeHeight, bp);
        } else {
            bp.setColor(0x80000000);
            can.drawRect(0, height - bottomBarHeight, width - bgThick,
                    height, bp);
        }

        // Description
        tp.setTextAlign(Paint.Align.CENTER);
        descRect.setPos(new PointF(width / 2, height - locTimeHeight),
                TextRect.ALIGN_BOTTOM | TextRect.ALIGN_X_CENTER);
        descRect.draw(can, borderWidth);

        if (updateDesc) {
            // Setting this to null frees the bitmap resource
            // and resets all canvas transformations
            can.setBitmap(null);

            // Rotate bitmap back to original orientation
            if (rot != 0) {
                Matrix matrix = new Matrix();
                matrix.postRotate(-rot);
                tile = Bitmap.createBitmap(tile, 0, 0, tile.getWidth(),
                        tile.getHeight(), matrix, false);
            }
            return tile;
        }

        // Location/altitude
        tp.setTextAlign(Paint.Align.LEFT);
        locRect.setPos(new PointF(0, height), TextRect.ALIGN_BOTTOM
                | TextRect.ALIGN_LEFT);
        locRect.draw(can, borderWidth);

        // Time stamp
        tp.setTextAlign(Paint.Align.RIGHT);
        timeRect.setPos(new PointF(width - bgThick, height),
                TextRect.ALIGN_BOTTOM | TextRect.ALIGN_RIGHT);
        timeRect.draw(can, borderWidth);

        /* Bar background */
        can.drawRect(0, 0, width, bgThick, bp);
        can.drawRect(width - bgThick, bgThick, width, height, bp);

        /* Azimuth bar */
        tp.setTextAlign(Paint.Align.CENTER);
        can.clipRect(0, 0, width, bgThick, Region.Op.REPLACE);
        drawTextStroke(can, String.format(res.getString(R.string.atsk_deg_azi),
                azimuth, getAzimuthReference(azimuth), north),
                width / 2, bgThick - (padding * 2), tp);

        // # of 5 degree spacings within horizontal FOV
        int numTicks = Math.max(1, (int) Math.round(hfov / 5));
        // Pixel spacing between each tick
        float spacing = (float) width / (float) numTicks;
        // Left-most starting degree (rounded to nearest factor of 5)
        double minDeg = azimuth - (hfov / 2);
        int deg = (int) Math.round(minDeg / 5.0f) * 5;
        // Pixel offset used to smooth tick movement
        int offset = -(int) Math.round(((minDeg - deg) / 5f) * spacing);
        if (deg < 0)
            deg += DEG_360;
        // Start with short or long tick (denotes 5 or 10)
        boolean shortTick = deg % 10 == 5;
        // Set text paint params
        tp.setStrokeWidth(0);
        tp.setStyle(Paint.Style.FILL);
        tp.setTextSize(tickSize);
        for (int i = 0; i <= numTicks; i++) {
            int tickX = offset + Math.round(i * spacing);
            path.moveTo(tickX, 0);
            path.rLineTo(0, bgThick / (shortTick ? 5f : 3f));
            // Include values on long ticks
            if (!shortTick) {
                String tickValue = String.format(
                        res.getString(R.string.atsk_deg),
                        (float) (deg % DEG_360));
                drawTextStroke(can, tickValue, tickX, (bgThick / 3) + tickSize,
                        tp);
            }
            deg += 5;
            shortTick = !shortTick;
        }
        tp.setStrokeWidth(tickWidth);
        tp.setStyle(Paint.Style.STROKE);
        can.drawPath(path, tp);
        path.reset();

        /* Pitch bar */
        boolean pitchPercent = prefs.getBoolean(res.getString(
                R.string.atsk_pitch_percent), false);
        can.rotate(DEG_90);
        can.translate(0, -width);
        can.clipRect(bgThick, 0, height, bgThick, Region.Op.REPLACE);
        tp.setStyle(Paint.Style.FILL);
        tp.setTextSize(titleSize);
        tp.setTextAlign(Paint.Align.CENTER);
        drawTextStroke(can, "Pitch: " + (pitchPercent ?
                Math.round(degPercent(pitch)) + "%"
                : String.format(res.getString(R.string.atsk_deg), pitch))
                + " GSR: " + getGSR(pitch),
                height / 2, bgThick - (padding * 2), tp);

        // Draw ticks
        numTicks = (int) Math.round((pitchPercent ?
                degPercent(vfov) : vfov) / 5);
        spacing = (float) height / (float) numTicks;
        minDeg = pitch - (vfov / 2);
        if (pitchPercent)
            minDeg = degPercent(minDeg);
        deg = (int) Math.round(minDeg / 5.0f) * 5;
        offset = (int) -Math.round(((minDeg - deg) / 5f) * spacing);
        shortTick = Math.abs(deg % 10) == 5;
        tp.setTextSize(tickSize);
        for (int i = 0; i <= numTicks; i++) {
            int tickX = offset + Math.round(i * spacing);
            path.moveTo(tickX, 0);
            path.rLineTo(0, bgThick / (shortTick ? 5f : 3f));
            // Include values on long ticks
            if (!shortTick) {
                String tickValue = pitchPercent ? deg + "%" :
                        String.format(res.getString(R.string.atsk_deg),
                                (float) (deg % DEG_360));
                drawTextStroke(can, tickValue, tickX, (bgThick / 3) + tickSize,
                        tp);
            }
            deg += 5;
            shortTick = !shortTick;
        }
        tp.setStrokeWidth(tickWidth);
        tp.setStyle(Paint.Style.STROKE);
        can.drawPath(path, tp);

        // Setting this to null frees the bitmap resource
        // and resets all canvas transformations
        can.setBitmap(null);

        // Rotate bitmap back to original orientation
        if (rot != 0) {
            Matrix matrix = new Matrix();
            matrix.postRotate(-rot);
            tile = Bitmap.createBitmap(tile, 0, 0, tile.getWidth(),
                    tile.getHeight(), matrix, false);
        }
        return tile;
    }

    // Set markup metadata to true or false
    public static void setMarkup(File inFile, File outFile, boolean markup) {
        TiffImageMetadata exif = ExifHelper.getExifMetadata(inFile);
        if (exif != null) {
            HashMap<String, Object> bundle = new HashMap<String, Object>();
            ExifHelper.getExtras(exif, bundle);
            // Update metadata with "Markup" parameter set to true
            TiffOutputSet tos = ExifHelper.getExifOutput(exif);
            try {
                if (markup) {
                    bundle.put(KEY_MARKUP, true);
                    bundle.put(KEY_MARKUP_DESC, ExifHelper.getString(exif,
                            TiffConstants.TIFF_TAG_IMAGE_DESCRIPTION, ""));
                    bundle.put(KEY_MARKUP_VERSION, MARKUP_VERSION);
                } else {
                    bundle.remove(KEY_MARKUP);
                    bundle.remove(KEY_MARKUP_DESC);
                    bundle.remove(KEY_MARKUP_VERSION);
                }
                ExifHelper.putExtras(exif, bundle, tos);
            } catch (Exception e) {
                Log.w(TAG, "Failed to set extra parameter \"Markup\" to true.",
                        e);
            }
            ExifHelper.saveExifOutput(tos, outFile);
        }
    }

    // Returns true if the image has been marked up before
    public static boolean upToDate(File image) {
        TiffImageMetadata exif = ExifHelper.getExifMetadata(image);
        boolean marked = ExifHelper.getExtraBoolean(exif, KEY_MARKUP, false);
        int markupVer = ExifHelper.getExtraInt(exif, KEY_MARKUP_VERSION, 1);
        String markupDesc = ExifHelper
                .getExtraString(exif, KEY_MARKUP_DESC, "");
        String desc = ExifHelper.getString(exif,
                TiffConstants.TIFF_TAG_IMAGE_DESCRIPTION, "");
        return marked && markupVer >= MARKUP_VERSION
                && (desc.isEmpty() || desc.equals(markupDesc));
    }

    // Return typical output file
    public static File getOutputFile(File inFile) {
        String name = inFile.getName();
        return new File(inFile.getParent(),
                name.substring(0, name.lastIndexOf(".")) + "_markup.jpg");
    }

    private static void drawTextStroke(Canvas can, String text, float x,
            float y, Paint textPaint) {
        Paint strokePaint = new Paint(textPaint);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setColor(Color.BLACK);
        strokePaint.setStrokeWidth(dp(1));
        can.drawText(text, x, y, strokePaint);
        can.drawText(text, x, y, textPaint);
    }

    private static void drawPathStroke(Canvas can, Path path, Paint strokePaint) {
        Paint boldPath = new Paint(strokePaint);
        boldPath.setStyle(Paint.Style.STROKE);
        boldPath.setColor(Color.BLACK);
        float pathStroke = strokePaint.getStrokeWidth();
        float outerStroke = Math.min(pathStroke / 2, dp(0.5f));
        boldPath.setStrokeWidth(pathStroke + outerStroke);
        can.drawPath(path, boldPath);
        can.drawPath(path, strokePaint);
    }

    private static float dp(float v) {
        return v * _dp;
    }

    private static String getAzimuthReference(double azimuth) {
        String dir;
        switch ((int) Math.round(azimuth / 45.0f)) {
            case 1:
                dir = "NE";
                break;
            case 2:
                dir = "E";
                break;
            case 3:
                dir = "SE";
                break;
            case 4:
                dir = "S";
                break;
            case 5:
                dir = "SW";
                break;
            case 6:
                dir = "W";
                break;
            case 7:
                dir = "NW";
                break;
            default:
                dir = "N";
        }
        return dir;
    }

    private static String getGSR(double deg) {
        if (Double.compare(deg, 0.0f) == 0)
            return "Infinity:1";
        return String.format(LocaleUtil.getCurrent(), "%d:1",
                Math.round(1.0d / Math.tan(Math.toRadians(deg))));
    }

    private static int getRotation(TiffImageMetadata exif) {
        int sori = ExifHelper.getInt(exif, TiffConstants.TIFF_TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL);
        int rot;
        switch (sori) {
            case ExifInterface.ORIENTATION_ROTATE_90:
            case ExifInterface.ORIENTATION_TRANSPOSE:
                rot = 90;
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                rot = 180;
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
            case ExifInterface.ORIENTATION_TRANSVERSE:
                rot = 270;
                break;
            case ExifInterface.ORIENTATION_NORMAL:
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
            default:
                rot = 0;
                break;
        }
        return rot;
    }

    private static double degPercent(double deg) {
        if (Double.compare(deg, 0.0f) == 0)
            return 0;
        return 100 / (1.0d / Math.tan(Math.toRadians(deg)));
    }

    public interface ProgressCallback {
        // Each time a tile of the image is finished processing
        // Return true to continue, false to quit
        boolean onProgress(int prog, int max);
    }
}
