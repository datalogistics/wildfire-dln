
package com.gmeci.hardware.bluetooth;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;

import com.gmeci.core.ATSKConstants;
import com.gmeci.atskservice.R;

public class BTStateNotification {

    Context context;
    NotificationManager notificationManager;
    PendingIntent pMainIntent, pBTEnableIntent, pGPSEnableIntent,
            pStopRouteIntent, pGradientIntent;
    int NotificationID = 4231;

    public BTStateNotification(Service context) {
        this.context = context;
        notificationManager = (NotificationManager) context
                .getSystemService(Service.NOTIFICATION_SERVICE);
        Intent intent = new Intent(ATSKConstants.BT_NOTIFICATION_ACTION);
        intent.putExtra(ATSKConstants.TYPE,
                ATSKConstants.BT_MAIN_PRESSED_ACTION);
        pMainIntent = PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        Intent intentEnable = new Intent(ATSKConstants.BT_NOTIFICATION_ACTION);
        intentEnable.putExtra(ATSKConstants.TYPE,
                ATSKConstants.BT_ENABLE_PRESSED_ACTION);
        pBTEnableIntent = PendingIntent.getBroadcast(context, 1, intentEnable,
                PendingIntent.FLAG_UPDATE_CURRENT);

        Intent StopRouteIntent = new Intent(
                ATSKConstants.BT_NOTIFICATION_ACTION);
        StopRouteIntent.putExtra(ATSKConstants.TYPE,
                ATSKConstants.STOP_COLLECTING_ROUTE_NOTIFICATION);
        pStopRouteIntent = PendingIntent.getBroadcast(context, 2,
                StopRouteIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent StopGradientIntent = new Intent(
                ATSKConstants.BT_NOTIFICATION_ACTION);
        StopGradientIntent.putExtra(ATSKConstants.TYPE,
                ATSKConstants.STOP_COLLECTING_GRADIENT_NOTIFICATION);
        pGradientIntent = PendingIntent.getBroadcast(context, 3,
                StopGradientIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent intentGPSEnable = new Intent(
                android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        intentGPSEnable.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        pGPSEnableIntent = PendingIntent.getActivity(context, 4,
                intentGPSEnable, 0);
    }

    public void UPDateBTState(boolean BTEnabled, boolean GPSEnabled,
            boolean CollectingRoute, boolean CollectingGradient) {

        if (notificationManager == null)
            return;
        if (BTEnabled && GPSEnabled && !CollectingRoute && !CollectingGradient) {
            notificationManager.cancel(NotificationID);
        } else {
            Notification.Builder nb = new Notification.Builder(context);
            String ContentText = "";
            nb.setContentTitle("ATSK Collection");
            nb.setSmallIcon(R.drawable.surveyor_man_72);
            nb.setContentIntent(pMainIntent);
            nb.setAutoCancel(true);
            if (!BTEnabled && GPSEnabled) {
                ContentText = "BT OFF";
                nb.addAction(R.drawable.atsk_bt_on, "Enable BT",
                        pBTEnableIntent);
            } else if (!GPSEnabled && BTEnabled) {

                ContentText = "GPS OFF";

                nb.addAction(R.drawable.gps_green_small, "Enable GPS",
                        pGPSEnableIntent);
            } else if (!BTEnabled && !GPSEnabled) {
                ContentText = "GPS/BT BOTH OFF";
                nb.setSmallIcon(R.drawable.surveyor_man_72);
                nb.setContentIntent(pMainIntent);
                nb.setAutoCancel(true);
                nb.addAction(R.drawable.atsk_bt_on, "Enable BT",
                        pBTEnableIntent);
                nb.addAction(R.drawable.gps_green_small, "Enable GPS",
                        pGPSEnableIntent);
            }

            if (CollectingRoute) {
                //nb.setContentText("COLLECTING ROUTE");
                //nb.setSmallIcon(R.drawable.surveyor_man_72);
                //nb.setContentIntent(pStopRouteIntent);
                //nb.setAutoCancel(true);
                ContentText = ContentText + ", COLLECTING ROUTE";
                nb.addAction(R.drawable.obstruction_line_red, "STOP ROUTE",
                        pStopRouteIntent);
            } else if (CollectingGradient) {
                ContentText = ContentText + ", COLLECTING GRADIENT";
                //nb.setSmallIcon(R.drawable.surveyor_man_72);
                //nb.setContentIntent(pStopRouteIntent);
                //nb.setAutoCancel(true);
                nb.addAction(R.drawable.gradient_red, "STOP GRADIENT",
                        pGradientIntent);
            }

            nb.setContentText(ContentText);
            Notification n = nb.build();
            notificationManager.notify(NotificationID, n);
        }

    }

    public void Hide() {
        notificationManager.cancel(NotificationID);
    }

}
