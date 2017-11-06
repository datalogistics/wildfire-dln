
package com.gmeci.hardware.bluetooth;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.gmeci.core.ATSKConstants;

public class NotificationTouchReceiver extends BroadcastReceiver {
    private static final String TAG = "NotificationTouchReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Received Notification Touch Event:" + intent.toString());
        Bundle extras = intent.getExtras();
        if (extras == null) {
            Log.d(TAG, "no extras giving up");
            return;
        }
        String NewAction = extras.getString(ATSKConstants.TYPE, "");
        Log.d(TAG, "Received Notification Touch Event:" + NewAction);
        if (NewAction.length() > 0) {
            Intent outIntent = new Intent(NewAction);
            context.sendBroadcast(outIntent);
        }

    }

}
