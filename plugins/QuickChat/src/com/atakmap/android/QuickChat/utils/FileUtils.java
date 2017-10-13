package com.atakmap.android.QuickChat.utils;

import android.content.Context;
import android.os.Environment;
import android.widget.Toast;

import com.atakmap.coremap.log.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

/**
 * Created by Scott Auman on 6/29/2016.
 * copied from AirOverlays
 */
public class FileUtils {

    private final Context context;
    private final String TAG = getClass().getSimpleName();

    public FileUtils(Context context) {
        this.context = context;
    }

    public boolean write(String dirName, String fileName, String data) {

        OutputStreamWriter outputStreamWriter = null;

        try {
            File outputFile = new File(checkOrCreateDirectory(dirName),
                    fileName);
            outputStreamWriter = new OutputStreamWriter
                    (new FileOutputStream(outputFile), Charset.forName("UTF-8")); //coverity 18589 using default charset encoding
            outputStreamWriter.write(data);
            outputStreamWriter.flush();
            Toast.makeText(
                    context.getApplicationContext(),
                    "Quick Chat Popup History Saved To : "
                            + outputFile.getAbsolutePath(),
                    Toast.LENGTH_LONG).show();
            outputStreamWriter.close();
            return true;
        } catch (IOException e) {
            Log.w(TAG, e.getMessage(), e);

            Toast.makeText(context,
                    e.getMessage() + " Unable to write to external storage.",
                    Toast.LENGTH_LONG).show();
            //coverity 19167 - resource leak
            if (outputStreamWriter != null) {
                try {
                    outputStreamWriter.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
        return false;
    }

    private File checkOrCreateDirectory(String dirName) {
        File root = Environment.getExternalStorageDirectory();
        File outDir = new File(root.getAbsolutePath() + File.separator
                + dirName);
        if (!outDir.exists()) {
            //create directory
            boolean made = outDir.mkdirs(); //coverity 18590 base use of return value -SA
            Log.d(TAG, made ? "created Directory "
                    : " did not create directory" + outDir.getAbsolutePath());
        }
        if (!outDir.isDirectory()) {
            if (!outDir.mkdir()) {
                Log.d(TAG, "problem creating outDir @ FileUtils");
            }
        }
        if (!outDir.isDirectory()) {
            Log.d(TAG, "Unable to create directory " + dirName
                    + " Maybe the SD card is mounted?");
        }

        return outDir;
    }
}
