package com.gotenna.atak.plugin.plugin;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;

import com.atakmap.coremap.log.Log;

import java.util.concurrent.atomic.AtomicReference;

import transapps.geom.GeoPoint;
import transapps.mapi.MapView;
import transapps.maps.plugin.event.MyLocationChangeListener;
import transapps.maps.plugin.lifecycle.Lifecycle;

//import com.gotenna.sdk.GoTenna;
//import com.gotenna.sdk.commands.GTCommand;
//import com.gotenna.sdk.commands.GTCommandCenter;
//import com.gotenna.sdk.commands.GTError;
//import com.gotenna.sdk.gids.GIDManager;
//import com.gotenna.sdk.interfaces.GTErrorListener;
//import com.gotenna.sdk.messages.GTBaseMessageData;
//import com.gotenna.sdk.messages.GTLocationMessageData;
//import com.gotenna.sdk.messages.GTMessageData;
//import com.gotenna.sdk.responses.GTResponse;

class GotennaLifecycle implements Lifecycle, MyLocationChangeListener {

    private static final String TAG = GotennaLifecycle.class.getSimpleName();

    // The default developer application token
    //public static final String GOTENNA_APP_TOKEN = "i6i61v4k4hehip3q6q06bu6gas91asa5";

    // The ATAK-specific developer token
    //public static final String GOTENNA_APP_TOKEN = "u5jolafk88kgu2jsbplejgaeqq49g3ag";

    // The SUPER token
    public static final String GOTENNA_APP_TOKEN = "RRwUQBYGE1kbUgBTQgAZChMWWldDDlgOAEZdVQIXAAoeBxNBUltVBgsdVgsOSEdd";

    private final AtomicReference<MapView> mapView = new AtomicReference<MapView>();
    private final AtomicReference<Activity> activity = new AtomicReference<Activity>();

    public GotennaLifecycle(Context ctx) {
        Log.d(TAG, "constructor");
    }

    @Override
    public void onCreate(final Activity activity, final MapView mapView) {
        Log.d(TAG, "onCreate()");
        this.mapView.set(mapView);
        this.activity.set(activity);

        try {
//            GoTenna.setApplicationToken(activity, GOTENNA_APP_TOKEN);
//            GTCommandCenter.getInstance().setMessageListener(new GTCommandCenter.GTMessageListener() {
//                @Override
//                public void onIncomingMessage(GTMessageData gtMessageData) {
//                    try {
//                        String msgData = new String(gtMessageData.getDataToProcess());
//                        CotEvent cot = CotEvent.parse(msgData);
//                        Log.d(TAG, "Got CoT event over GoTenna: \n" + cot);
//
//                        // show on my device
//                        // (do I really need this? i.e., does external dispatch imply internal as well?)
//                        CotMapComponent.getInternalDispatcher().dispatch(cot);
//
//                        // now proxy to other ATAKs...
//                        CotMapComponent.getExternalDispatcher().dispatch(cot);
//
//                    } catch (Exception e) {
//                        Log.w(TAG, "Error processing GoTenna message", e);
//                    }
//                }
//
//                @Override
//                public void onIncomingMessage(GTBaseMessageData gtBaseMessageData) {
//                    String text = gtBaseMessageData.getText();
//                    Log.d(TAG, "GoTenna msg: " + text);
//                    Toast.makeText(activity, text, Toast.LENGTH_LONG).show();
//
//                    GTLocationMessageData loc = gtBaseMessageData.getLocationMessageData();
//                    double lat = loc.latitude;
//                    double lon = loc.longitude;
//                    Log.d(TAG, " location of message: " + lat + ", " + lon);
//                }
//            });
        } catch (Exception e) {
            Log.w(TAG, "Error initializing GoTenna", e);
        }

    }

    @Override
    public void onStart() {

    }

    @Override
    public void onPause() {

    }

    @Override
    public void onDestroy() {

    }

    @Override
    public void onResume() {

    }

    @Override
    public void onFinish() {

    }

    @Override
    public void onStop() {

    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {

    }

    public static final boolean shouldEncrypt = false;

    @Override
    public void onLocationChanged(GeoPoint geoPoint) {
//        Activity atakActivity = activity.get();
//        if(atakActivity == null || !(atakActivity instanceof ATAKActivity)) {
//            Log.w(TAG, "couldn't get ATAK Activity");
//            return;
//        }
//
//        String uid = ((ATAKActivity) atakActivity).getMapView().getMapData().getString("deviceUID", null);
//        if(uid == null) {
//            Log.w(TAG, "couldn't get UID");
//            return;
//        }
//
//        String type = "a-f-G-U-C";
//        String version = "2.0";
//        double lat = geoPoint.getLatitudeE6() * 10E-6;
//        double lon = geoPoint.getLongitudeE6() * 10E-6;
//        double alt = geoPoint.getAltitude();
//        double ce = 999999;
//        double le = 999999;
//        CotPoint point = new CotPoint(lat, lon, alt, ce, le);
//        CoordinatedTime time = new CoordinatedTime();
//        CoordinatedTime start = new CoordinatedTime();
//        CoordinatedTime stale = new CoordinatedTime(20000);
//        String how = CotEvent.HOW_MACHINE_GENERATED;
//        CotDetail detail = new CotDetail("detail");
//        String opex = null;
//        String qos = null;
//        String access = null;
//        CotEvent cotEvent = new CotEvent(uid, type, version, point, time, start, stale, how, detail, opex, qos, access);
//        GTCommandCenter.getInstance().sendMessage(
//                cotEvent.toString().getBytes(),
//                GIDManager.SHOUT_GID,
//                new GTCommand.GTCommandResponseListener() {
//                    @Override
//                    public void onResponse(GTResponse gtResponse) {
//                        Log.d(TAG, gtResponse.toString());
//                    }
//                },
//                new GTErrorListener() {
//                    @Override
//                    public void onError(GTError gtError) {
//                        Log.w(TAG, gtError.toString());
//                    }
//                },
//                shouldEncrypt
//        );
//        Log.d(TAG, "Sending: " + cotEvent);
    }

}
