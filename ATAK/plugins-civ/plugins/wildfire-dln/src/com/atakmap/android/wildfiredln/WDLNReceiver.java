package com.atakmap.android.wildfiredln;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class WDLNReceiver extends BroadcastReceiver
{
    public static final String TAG = "WDLN_Reciever";

    public static final String WDLN_TEST = "com.atakmap.android.wildfiredln.WDLN_TEST";
    public static final String WDLN_VIEW = "com.atakmap.android.wildfiredln.WDLN_VIEW";

    private WildfireDLN parent;

    public WDLNReceiver(WildfireDLN p)
    {
        parent = p;
    }


    @Override
    public void onReceive(Context context, Intent intent)
    {
        Log.d(TAG,"Got Intent "+intent);
        if (WDLN_VIEW.equals(intent.getAction()))
        {
            Log.d(TAG,"Got WDLN_VIEW Intent "+intent);
        }
        else if("com.atakmap.maps.images.DISPLAY".equals(intent.getAction()))
        {
            String uid = intent.getStringExtra("uid");
            parent.DisplayByUID(uid);
        }
    }
}
