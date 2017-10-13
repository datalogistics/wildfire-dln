
package com.atakmap.android.helloworld.notification;

import com.atakmap.android.helloworld.plugin.R;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import android.util.Log;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;

/** 
 * Please note, this Service cannot reference anything from ATAK CORE because it is started up by a 
 * classloader that knows nothing about the plugin interface.    So for example you cannot reference
 * Marker, GeoPoint, Android Support Library, etc.
 * This will start up, but when looking for the class from ATAK CORE will bomb out even if it compiled
 * succesfully. 
 * This is because compilation for plugins weakly links against the ATAK classes and depends on 
 * them being found at runtime.
 * Compiling any other way will cause duplicative classes and the main plugin will not load properly.
 */

public class NotificationService extends Service {

    private final static String TAG = "NotificationService";

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG,
                "getting ready to show the notification, can never use notification compat.");

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        Intent contentIntent = new Intent();
        PendingIntent appIntent = PendingIntent.getActivity(this, 0,
                contentIntent, 0);

        Notification notification =
                new Notification(R.drawable.abc, "Hello World!",
                        System.currentTimeMillis());
        notification.setLatestEventInfo(this, "1", "2", appIntent);

        notificationManager.notify(9999, notification);

    }

}
