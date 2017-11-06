
package com.gmeci.atsk.resources;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Environment;

import com.atakmap.coremap.log.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class AssetFileManager {

    public static final String TAG = "AssetFileManager";
    @SuppressWarnings("unused")
    private static final String ATSK_FOLDER_BASE = "/atsk";
    private static final String ATSK_REMARK_TEMPLATE_FOLDER_BASE = "/atsk/remark_templates";
    @SuppressWarnings("unused")
    private static final String ATSK_AZ_DATA_FOLDER_BASE = "/atsk/az_data";
    final Context mContext;

    public AssetFileManager(Context context) {
        mContext = context;
    }

    public boolean UpdateAssets() {
        File f = new File(Environment.getExternalStorageDirectory()
                + ATSK_REMARK_TEMPLATE_FOLDER_BASE);
        if (!f.isDirectory()) {
            if (!f.mkdirs())
                Log.w(TAG, "Failed to create directories" + f.getAbsolutePath());
        }

        AssetManager assetManager = mContext.getAssets();
        String[] files = null;

        //Try to open up the projects asset folder
        try {
            files = assetManager.list("");
        } catch (IOException e) {
            Log.e(TAG, "error has occurred opening the asset", e);
        }

        for (int i = 0; files != null && i < files.length; i++) {
            copyFile(files[i], mContext, ATSK_REMARK_TEMPLATE_FOLDER_BASE);
        }

        return true;
    }

    private void copyFile(String filename, Context context, String baseFolder) {
        AssetManager assetManager = context.getAssets();

        String path = Environment.getExternalStorageDirectory()
                .getAbsolutePath() + "/" + baseFolder + "/" + filename;
        File file = new File(path);
        if (file.exists())
            return;

        InputStream in = null;
        OutputStream out = null;

        //open the file passed in from assets, and write it with
        //the same name on the SD card.
        try {
            in = assetManager.open(filename);
            out = new FileOutputStream(path);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            //clean up files
            in.close();
            in = null;
            out.flush();
            out.close();
            out = null;
        } catch (Exception e) {
            Log.e(TAG, "error has occurred copying an asset: " + filename
                    + " to: " + baseFolder, e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ioe) {
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ioe) {
                }
            }
        }

    }
}
