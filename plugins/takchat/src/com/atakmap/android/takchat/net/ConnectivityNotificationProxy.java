package com.atakmap.android.takchat.net;

import android.content.ComponentName;
import android.content.Intent;

import com.atakmap.android.takchat.TAKChatUtils;
import com.atakmap.android.takchat.data.ChatDatabase;
import com.atakmap.android.takchat.data.ChatMessage;
import com.atakmap.android.takchat.data.MessageUnreadListener;
import com.atakmap.android.takchat.notification.TAKNotificationService;
import com.atakmap.coremap.log.Log;

/**
 * Simply relays to TAKNotificationService to manage the TAK Chat notifications
 *
 * Created by byoung on 7/31/2016.
 */
public class ConnectivityNotificationProxy implements MessageUnreadListener, ConnectivityListener {

    private final static String TAG = "ConnectivityNotificationProxy";

    private static Boolean bShutdown;

    public ConnectivityNotificationProxy() {
        bShutdown = false;
    }

    @Override
    public void dispose(){
        synchronized (bShutdown) {
            if (bShutdown)
                return;
        }

        Log.d(TAG, "dispose");
        bShutdown = true;
        try {
            TAKChatUtils.mapView.getContext().stopService(TAKNotificationService.createIntent(TAKChatUtils.mapView.getContext()));
        }catch(SecurityException e){
            Log.w(TAG, "Failed to stop Notification Service", e);
        }
    }


    /**
     * Show a one time notification, not the ongoing plugin notification
     *
     * @param message
     * @param iconId
     * @return
     */
    public static boolean showNotification(String title, String message, int iconId, Intent pending){
        return showNotification(false, false, title, message, iconId, pending, -1);
    }

    /**
     * Update the ongoing notification
     *
     * @return
     */
    public static boolean showNotification() {
        String message = null;
        boolean bSuccess = TAKChatXMPP.getInstance().isConnected();
        if(bSuccess)
            message =  "Logged in as: " + TAKChatXMPP.getInstance().getAccountDisplay();

        return showNotification(bSuccess, message);
    }

    /**
     * Update the ongoing notification
     *
     * @param bSuccess
     * @param message
     * @return
     */
    public static boolean showNotification(boolean bSuccess, String message){
        int unread = ChatDatabase.getInstance(TAKChatUtils.pluginContext).getUnreadCount(null);

        if(unread > 0){
            String str = String.valueOf(unread) + " unread " + (unread == 1 ? "message" : "messages") + ". "
                    + (message == null ? "" : message);
            return showNotification(true, bSuccess, null, str,
                    -1, null, unread);
        }else{
            return showNotification(true, bSuccess, null, message, -1, null, unread);
        }
    }

    /**
     * Show a notification for XMPP server connection
     * Update the ongoing notification
     *
     * @param bPersistent   true to update the ongoing notification, false to create a one time notification
     * @param bSuccess
     * @param message
     */
    private static boolean showNotification(boolean bPersistent, boolean bSuccess, String title, String message, int iconId, Intent pending, int unread) {
        synchronized (bShutdown) {
            if (bShutdown) {
                Log.d(TAG, "shutdown, ignoring: " + message);
                return false;
            }
        }

        ComponentName name = null;
        Log.d(TAG, "showNotification: " + unread + ", " + message);

        try{
            name = TAKChatUtils.mapView.getContext().startService(TAKNotificationService.createIntent(
                    TAKChatUtils.mapView.getContext(), bPersistent, bSuccess, title, message,
                    iconId, pending, unread));
        }catch(SecurityException e){
            Log.w(TAG, "Failed to start Notification Service", e);
            return false;
        }

        if(name == null){
            Log.w(TAG, "Failed to start notification service");
            return false;
        }

        return true;
    }

    @Override
    public void onMessageRead(ChatMessage message) {
        //no-op
        //TODO should this showNotification() ?
    }

    @Override
    public void onUnreadCountChanged() {
        showNotification();
    }

    @Override
    public boolean onConnected() {
        return true;
    }

    @Override
    public boolean onStatus(boolean bSuccess, String status) {
        ConnectivityNotificationProxy.showNotification(bSuccess, status);
        return true;
    }

    @Override
    public boolean onDisconnected() {
        return true;
    }
}
