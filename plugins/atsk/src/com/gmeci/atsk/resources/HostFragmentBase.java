
package com.gmeci.atsk.resources;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.ipc.AtakBroadcast.DocumentedIntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.gmeci.core.ATSKConstants;
import com.gmeci.core.SurveyPoint;
import com.gmeci.conversions.Conversions;

public class HostFragmentBase extends Fragment {
    private final BroadcastReceiver AZClickReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Bundle extras = intent.getExtras();
            if (extras.containsKey(ATSKConstants.LAT_EXTRA)
                    && extras.containsKey(ATSKConstants.LON_EXTRA)) {
                String LatString = ATSKConstants.LAT_EXTRA;
                String LonString = ATSKConstants.LON_EXTRA;
                try {
                    double lat = extras.getDouble(LatString);
                    double lon = extras.getDouble(LonString);
                    double alt = (float) extras
                            .getDouble(ATSKConstants.ALT_EXTRA);
                    SurveyPoint clickedPosition = new SurveyPoint(lat, lon);
                    clickedPosition.setHAE(alt);
                    String AltString = String.format("ALT: %.1fft", alt
                            * Conversions.M2F);

                    UpdateNotification("Map Clicked",
                            Conversions.GetMGRS(lat, lon),
                            Conversions.GetLatLonDM(lat, lon), AltString);

                } catch (NumberFormatException nfe) {
                    nfe.printStackTrace();
                }
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        AtakBroadcast.getInstance().registerReceiver(AZClickReceiver,
                new DocumentedIntentFilter(ATSKConstants.MAP_CLICK_ACTION));
    }

    @Override
    public void onPause() {
        super.onPause();
        AtakBroadcast.getInstance().unregisterReceiver(AZClickReceiver);
    }

    public void UpdateNotification(String Title, String Line1, String Line2,
            String Line3) {
        Intent BubbleIntent = new Intent();
        BubbleIntent.setAction(ATSKConstants.NOTIFICATION_BUBBLE);
        BubbleIntent.putExtra(ATSKConstants.NOTIFICATION_UPDATE, true);
        BubbleIntent.putExtra(ATSKConstants.NOTIFICATION_TITLE, Title);
        BubbleIntent.putExtra(ATSKConstants.NOTIFICATION_LINE1, Line1);
        BubbleIntent.putExtra(ATSKConstants.NOTIFICATION_LINE2, Line2);
        BubbleIntent.putExtra(ATSKConstants.NOTIFICATION_LINE3, Line3);
        com.atakmap.android.ipc.AtakBroadcast.getInstance().sendBroadcast(
                BubbleIntent);
    }
}
