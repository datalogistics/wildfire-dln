
package com.gmeci.helpers;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class AssetHelper {
    public static final String ASSET_BASE = "atsk";
    private static final String TAG = "AssetHelper";
    private final static String BaseFolder = Environment
            .getExternalStorageDirectory()
            .getAbsolutePath() + "/";

    private Context context;

    public boolean CopyAssetsToSDCard(Context context, boolean ForceOverwrite) {
        return CopyAssetsToSDCard(context, "YourPassword", ForceOverwrite);
    }

    public boolean CopyAssetsToSDCard(Context context, String InPassword,
            boolean ForceOverwrite) {
        this.context = context;
        int AssetCount = copyFileOrDir(ASSET_BASE, ForceOverwrite);
        Log.d(TAG, "assets extrated " + AssetCount);

        return true;
    }

    private int copyFileOrDir(String path, boolean ForceOverwrite) {
        int count = 0;
        AssetManager assetManager = context.getAssets();
        String assets[] = null;
        try {
            assets = assetManager.list(path);

            if (assets.length == 0) {

                File f = new File(BaseFolder + path);
                if (!f.exists() || ForceOverwrite) {
                    count++;
                    copyFile(path);
                }
            } else {
                String fullPath = path;
                Log.i(TAG, "extracting directory: " + BaseFolder + fullPath);
                File dir = new File(BaseFolder + fullPath);
                if (!dir.exists() && !path.startsWith("images")
                        && !path.startsWith("sounds")
                        && !path.startsWith("webkit"))
                    if (!dir.mkdirs())
                        Log.e(TAG, "could not create dir, CHECK YOUR MANIFEST "
                                + fullPath);
                for (int i = 0; i < assets.length; ++i) {
                    String p;
                    if (path.equals(""))
                        p = "";
                    else
                        p = path + "/";

                    if (!path.startsWith("images")
                            && !path.startsWith("sounds")
                            && !path.startsWith("webkit"))
                        Log.i(TAG, "extracting file: " + p + assets[i]);
                    count += copyFileOrDir(p + assets[i],
                            ForceOverwrite);
                }
            }
        } catch (IOException ex) {
            Log.e(TAG, "I/O Exception", ex);
        }
        return count;
    }

    private void copyFile(String filename) {
        AssetManager assetManager = context.getAssets();

        InputStream in = null;
        OutputStream out = null;
        try {
            in = assetManager.open(filename);

            String newFileName = BaseFolder + filename;
            out = new BufferedOutputStream(new FileOutputStream(newFileName));

            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
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
