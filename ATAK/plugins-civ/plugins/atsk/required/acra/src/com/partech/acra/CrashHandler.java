
package com.partech.acra;

import android.content.Context;
import android.util.Log;

import org.acra.collector.CrashReportData;
import org.acra.sender.ReportSender;
import org.acra.sender.ReportSenderException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class CrashHandler implements ReportSender {

    private static final String TAG = "CrashHandler";
    private static CrashHandler sInstance;
    private List<CrashHandler.CrashListener> listeners =
            new ArrayList<CrashHandler.CrashListener>();
    private String root = null;

    private CrashHandler() {
    }

    /**
     * Obtain an instance of the crash handler for usage by the system.
     * The crash handler is a singleton.
     */
    synchronized public static CrashHandler instance() {
        if (sInstance == null) {
            sInstance = new CrashHandler();
        }
        return sInstance;
    }

    /**
     * Provides the full path plus the root of the name for the crash log that
     * can be written when a crash is encountered.
     *
     * @param root is the full path plus the non-mutable part of the file name.
     */
    public void initialize(final String root) {
        this.root = root;
    }

    public void addListener(CrashHandler.CrashListener listener) {
        if (listeners.contains(listener))
            return;
        listeners.add(listener);
    }

    @Override
    public void send(Context context, CrashReportData crashData)
            throws ReportSenderException {
        if (root == null) {
            Log.d(TAG, "no crash log destination set.");
            return;
        }

        // write out crash details to disk
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss",
                Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String timeStamp = sdf.format(new Date());

        File f = new File(root + timeStamp + ".txt");

        if (!f.getParentFile().exists())
            if (!f.getParentFile().mkdir())
                Log.w(TAG, "Error creating directory" + f.getAbsolutePath());

        if (!f.delete())
            Log.w(TAG, "Error deleting file" + f.getAbsolutePath());

        try {
            if (!f.createNewFile())
                Log.w(TAG, "Error creating file" + f.getAbsolutePath());

            PrintWriter pw = new PrintWriter(new FileWriter(f));
            pw.println(crashData.toString());
            pw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // notify crash listeners
        if (listeners.size() > 0) {
            Log.d(TAG, "notifying " + listeners.size() + " crash listeners");
            for (CrashHandler.CrashListener listener : listeners) {
                try {
                    listener.onCrash();
                } catch (Throwable t) {
                    Log.e(TAG, "onCrash " + listener.getClass().getName(), t);
                }
            }
        }
    }

    static public interface CrashListener {
        public void onCrash();
    }
}
