
package com.gmeci.hardware;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.Log;

import java.util.List;

//looks for action of "com.gmeci.parseStreamingDataForGPSLRF" in a service and starts it if it looks like a match...
//the started service binds back to us
//this allows others to start the service or bind to us without being a service...
//we'll pass them streaming data (probably BT or serial) and they'll pass us back results of GPS or LRF events

public class ParserManager {
    private static final String TAG = "ParserManager";

    public static int startParserServices(Context context) {
        PackageManager packageManager = context.getPackageManager();
        Intent intent = new Intent("com.gmeci.parseStreamingDataForGPSLRF");
        List<ResolveInfo> list = packageManager.queryIntentServices(intent, 0);
        int StartCount = 0;
        for (ResolveInfo CurrentResolve : list) {
            StartCount++;
            Log.d(TAG, "resolved:" + CurrentResolve.toString());
            Intent i = new Intent();
            i.setClassName(CurrentResolve.serviceInfo.packageName,
                    CurrentResolve.serviceInfo.name);
            context.startService(i);
        }
        return StartCount;
    }
}
