
package com.gmeci.atsk.gallery;

import android.os.Environment;

import com.atakmap.android.image.ImageDropDownReceiver;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.time.CoordinatedTime;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import com.atakmap.coremap.locale.LocaleUtil;

/**
 * Image gallery static methods
 */
public class ATSKGalleryUtils {

    private static final String TAG = "ATSKGalleryUtils";

    public static final String IMG_DIR = Environment
            .getExternalStorageDirectory()
            + File.separator + "atsk" + File.separator + "images";
    public static final String IMG_DIR_TMP = IMG_DIR + File.separator + "temp";

    public static final String IMG_CAPTURE = "com.gmeci.atsk.gallery.IMAGE_CAPTURE";
    public static final String ACTIVITY_FINISHED = "com.atakmap.android.ACTIVITY_FINISHED";
    public static final int IMG_IMPORT_CODE = 6789;
    public static final String IMG_MARKER = "Image Marker";

    public static File getImageDir(String uid) {
        return new File(IMG_DIR, uid);
    }

    public static File getImageCache() {
        return getImageDir(".cache");
    }

    // Return new file based on survey UID and  date stamp
    public static File newImage(String uid, String ext) {
        final String possible[] = {
                "", "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k",
                "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w",
                "x", "y", "z"
        };
        final String uidEncoded = URLEncoder.encode(uid);
        final String path = IMG_DIR + File.separator + uidEncoded;
        final File dir = new File(path);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                Log.d(TAG, "Failed to make dir at" + dir.getAbsolutePath());
            }
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss",
                LocaleUtil.getCurrent());
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        final String name = sdf.format(CoordinatedTime.currentDate());
        for (String c : possible) {
            File file = new File(dir, name + c + "." + ext);
            if (!file.exists())
                return file;
        }
        return new File(dir, name + "." + ext);
    }

    public static File newImage(String uid) {
        return newImage(uid, "jpg");
    }

    // Get a single image for survey
    public static File getImage(String uid, String name) {
        return new File(getImageDir(uid), name);
    }

    // Get temporary image
    public static File getTempImage(File img) {
        File tmp = new File(IMG_DIR_TMP, img.getName());
        if (!tmp.getParentFile().exists() && !tmp.getParentFile().mkdirs())
            Log.e(TAG, "Failed to create temporary image directory: " + tmp);
        return tmp;
    }

    // Clean out temporary image directory
    public static void clearTempImages() {
        FileSystemUtils.deleteDirectory(new File(IMG_DIR_TMP), true);
    }

    // Get list of images for survey
    public static File[] getImages(String uid) {
        if (uid == null)
            return new File[0];
        File dir = getImageDir(uid);
        File[] imgs = new File[0];
        if (dir.exists() && dir.isDirectory())
            imgs = dir.listFiles();

        // dir.listFiles could return null
        if (imgs == null)
            imgs = new File[0];

        return imgs;
    }

    public static String importImage(String uid, String path) {
        if (path == null || !(new File(path)).exists())
            return "Import failed: File does not exist.";

        File image = new File(path);
        if (!image.isFile())
            return "Import failed: File is a directory.";

        if (!image.canRead())
            return "Import failed: File cannot be read.";

        if (!ImageDropDownReceiver.ImageFileFilter
                .accept(null, image.getName()))
            return "Import failed: File is not an image.";

        File copy = addCopyPrefix(new File(getImageDir(uid), image.getName()));
        try {
            FileSystemUtils.copyFile(image, copy);
        } catch (IOException e) {
            Log.e(TAG, "Failed to copy image to ATSK directory", e);
            return "Import failed: Failed to copy image to ATSK directory.";
        }
        return null;
    }

    // Add prefix to file that already exists in the same directory
    public static File addCopyPrefix(File file) {
        File dir = file.getParentFile();
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                Log.d(TAG, "Failed to make dir at" + dir.getAbsolutePath());
            }
            return file;
        }
        String name = file.getName();
        int i = 1;
        while (file.exists())
            file = new File(dir, "(" + (i++) + ") " + name);
        return file;
    }
}
