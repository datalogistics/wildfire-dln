
package com.partech.acra;

import android.app.Application;

import org.acra.annotation.ReportsCrashes;

@ReportsCrashes
public class BasicApplication extends Application {

    static final String TAG = "BasicApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        // start listening for any crash to report it
        /**
         ACRA.init(this);
         ACRA.getErrorReporter().setReportSender(CrashHandler.instance());
         // add custom content not supported by ACRA
         ACRA.getErrorReporter().putCustomData("os.version",
         System.getProperty("os.version"));
         ACRA.getErrorReporter().putCustomData("ACRA.version", "4.6");
         Log.d(TAG, "basic acra capability initialized");
         **/
    }

}
