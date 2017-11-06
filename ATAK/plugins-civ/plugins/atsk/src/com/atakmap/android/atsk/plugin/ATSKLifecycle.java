
package com.atakmap.android.atsk.plugin;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;

import com.atakmap.android.maps.MapComponent;
import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.log.Log;
import com.gmeci.atsk.ATSKMapComponent;
import com.gmeci.atsk.toolbar.ATSKToolbarComponent;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import transapps.maps.plugin.lifecycle.Lifecycle;

public class ATSKLifecycle implements Lifecycle {

    private final static String TAG = "ATSKLifecycle";
    private final Context _plugin;
    private final Collection<MapComponent> _overlays;
    private MapView _mapView;

    public ATSKLifecycle(Context ctx) {
        _plugin = ctx;
        _overlays = new LinkedList<MapComponent>();
        _mapView = null;
    }

    @Override
    public void onConfigurationChanged(Configuration arg0) {
        for (MapComponent c : _overlays)
            c.onConfigurationChanged(arg0);
    }

    @Override
    public void onCreate(final Activity arg0, final transapps.mapi.MapView arg1) {
        if (arg1 == null || !(arg1.getView() instanceof MapView)) {
            Log.w(TAG, "This plugin is only compatible with ATAK MapView");
            return;
        }
        _mapView = (MapView) arg1.getView();
        // <component class="com.gmeci.atsk.ATSKMapComponent"/>
        // <component class="com.gmeci.atsk.toolbar.ATSKToolbarComponent"/>

        _overlays.add(new ATSKMapComponent());
        _overlays.add(new ATSKToolbarComponent());

        // create components
        Iterator<MapComponent> iter = _overlays.iterator();
        MapComponent c;
        while (iter.hasNext()) {
            c = iter.next();
            try {
                c.onCreate(_plugin, arg0.getIntent(), _mapView);
            } catch (Exception e) {
                Log.w(TAG,
                        "Unhandled exception trying to create overlays MapComponent",
                        e);
                iter.remove();
            }
        }
    }

    @Override
    public void onDestroy() {
        for (MapComponent c : _overlays)
            c.onDestroy(_plugin, _mapView);
    }

    @Override
    public void onFinish() {
        // XXX - no corresponding MapComponent method
    }

    @Override
    public void onPause() {
        for (MapComponent c : _overlays)
            c.onPause(_plugin, _mapView);
    }

    @Override
    public void onResume() {
        for (MapComponent c : _overlays)
            c.onResume(_plugin, _mapView);
    }

    @Override
    public void onStart() {
        for (MapComponent c : _overlays)
            c.onStart(_plugin, _mapView);
    }

    @Override
    public void onStop() {
        for (MapComponent c : _overlays)
            c.onStop(_plugin, _mapView);
    }

}
