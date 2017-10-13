package com.atakmap.android.takchat.notification;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.atakmap.android.takchat.plugin.R;

/**
 * Simple service exists only to manage notifications for this plugin
 * This is a workaround for not being able to display plugin resources/icons in ATAK notifications
 *
 * Created by byoung on 10/23/16.
 */

public class TAKNotificationService extends Service {

    private final static String TAG = "TAKNotificationService";

    /**
     * Note, matches AndroidManifest.xml
     */
    private static final String NOTIF_SVC_INTENT = "com.atakmap.android.takchat.notification.TAKNotificationService";

    /**
     * Note, matches TAKChatDropDownReceiver.SHOW_CONTACT_LIST
     */
    private static final String ONGOING_PENDING_INTENT = "com.atakmap.android.takchat.SHOW_CONTACT_LIST";

    private final static int NOTIF_ID = TAG.hashCode();
    private NotificationManager _nm;
    private SharedPreferences _prefs;
    private ComponentName _componentName;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, "onCreate");
        _nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        _prefs = PreferenceManager.getDefaultSharedPreferences(this);

        //Note, this cannot reference ATAKActivity.atakComponentName b/c its a plugin svc
        _componentName = new ComponentName(getString(R.string.atak_package),
                getString(R.string.atak_activity));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.d(TAG, "onDestroy");
        if(_nm != null) {
            _nm.cancel(NOTIF_ID);
            _nm.cancelAll();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        if(_nm == null){
            Log.w(TAG, "Not initialized");
            return START_STICKY;
        }

        if(intent == null){
            Log.w(TAG, "No intent");
            return START_STICKY;
        }

        String message = intent.getStringExtra("message");
        String title = intent.getStringExtra("title");
        if(message == null || !intent.hasExtra("persistent") || !intent.hasExtra("success")){
            Log.w(TAG, "No notification message");
            return START_STICKY;
        }

        boolean bPersistent = intent.getBooleanExtra("persistent", false);
        boolean bSuccess = intent.getBooleanExtra("success", false);
        int iconId = intent.getIntExtra("iconId", -1);
        int unread = intent.getIntExtra("unread", 0);
        Intent pending = intent.getParcelableExtra("pending");
        showNotification(bPersistent, bSuccess, title, message, iconId, pending, unread);
        return START_STICKY;
    }

    /**
     * Create intent to start/stop service
     *
     * @param context
     * @param bSuccess
     *@param message @return
     */
    public static Intent createIntent(Context context, boolean bPersistent, boolean bSuccess, String title, String message, int iconId, Intent pending, int totalUnreadMessages) {
        //return new Intent(context, TAKNotificationService.class)
        return new Intent(NOTIF_SVC_INTENT)
                .putExtra("persistent", bPersistent)
                .putExtra("success", bSuccess)
                .putExtra("title", title)
                .putExtra("message", message)
                .putExtra("iconId", iconId)
                .putExtra("pending", pending)
                .putExtra("unread", totalUnreadMessages);

    }

    public static Intent createIntent(Context context) {
        //return new Intent(context, TAKNotificationService.class);
        return new Intent(NOTIF_SVC_INTENT);
    }


    /**
     * Show a notification for XMPP server connection
     */
    private void showNotification(boolean bPersistent, boolean bSuccess, String title, String message, int iconId, Intent pending, int unread) {

        Log.d(TAG, "Showing notification: " + bSuccess + ", " + message);
        final boolean blink = true;

        Intent atakFrontIntent = new Intent();
        atakFrontIntent.setComponent(_componentName);
        if(pending == null)
            atakFrontIntent.putExtra("internalIntent", new Intent(ONGOING_PENDING_INTENT));
        else
            atakFrontIntent.putExtra("internalIntent", pending);
        atakFrontIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        // requires the use of currentTimeMillis
        PendingIntent contentIntent = PendingIntent.getActivity(this,
                (int) System.currentTimeMillis(),
                atakFrontIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        String header = title != null ? title :
                bSuccess ? "Chat Server Connected" : "Chat Server Disconnected";

        if(message == null || "null".equals(message)){
            message = "";
        }

        //TODO cache builder?
        Notification.Builder nBuilder = new Notification.Builder(this);
        nBuilder.setContentTitle(header)
                .setContentText(message)
                .setTicker(message)
                .setContentIntent(contentIntent)
                .setAutoCancel(true);

        if(iconId > 0) {
            nBuilder.setSmallIcon(iconId);
        }else if(unread > 0) {
            nBuilder.setSmallIcon(R.drawable.takchat_unread2);
            Bitmap bm = BitmapFactory.decodeResource(getResources(), bSuccess ? R.drawable.takchat_connected : R.drawable.takchat_disconnected);
            if(bm != null)
                nBuilder.setLargeIcon(bm);
        }else{
            nBuilder.setSmallIcon(bSuccess ? R.drawable.takchat_connected : R.drawable.takchat_disconnected);
        }

        if (bPersistent) {
            nBuilder.setOngoing(true);
            nBuilder.setAutoCancel(false);
        } else {
            nBuilder.setOngoing(false);
            nBuilder.setAutoCancel(true);
        }

        final Notification notification = nBuilder.getNotification();

        if (_prefs.getBoolean("takchatVibratePhone", false))
            notification.defaults |= Notification.DEFAULT_VIBRATE;

        if (blink) {
            notification.ledARGB = Color.BLUE;
            notification.ledOnMS = 100;
            notification.ledOffMS = 300;
        }

//        if (_prefs.getBoolean("takchatAudibleNotifySent", false) ||
//                _prefs.getBoolean("takchatAudibleNotifyRecv", false) ||
//                _prefs.getBoolean("takchatAudibleNotifyBuddy", false))
//            notification.defaults |= Notification.DEFAULT_SOUND;


        //TODO fade notifications?
        int notificationId = bPersistent ? NOTIF_ID : message.hashCode();
        _nm.notify(notificationId, notification);
    }
}
