package com.atakmap.android.commout.plugin;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import com.atakmap.android.maps.MapComponent;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.commout.CommoutMapComponent;

import transapps.maps.plugin.lifecycle.Lifecycle;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.util.Log;

public class CommoutLifecycle implements Lifecycle {

    private final Context pluginContext;
    private final Collection<MapComponent> overlays;
    private MapView mapView;

    private final static String TAG = "CommoutLifecycle";

    public CommoutLifecycle(Context ctx) {
        this.pluginContext = ctx;
        this.overlays = new LinkedList<MapComponent>();
        this.mapView = null;
    }

    @Override
    public void onConfigurationChanged(Configuration arg0) {
        for(MapComponent c : this.overlays)
            c.onConfigurationChanged(arg0);
    }

    @Override
    public void onCreate(final Activity arg0, final transapps.mapi.MapView arg1) {
        if(arg1 == null || !(arg1.getView() instanceof MapView)) {
            Log.w(TAG, "This plugin is only compatible with ATAK MapView");
            return;
        }
        this.mapView = (MapView)arg1.getView();
        CommoutLifecycle.this.overlays.add(new CommoutMapComponent());

        // create components
        Iterator<MapComponent> iter = CommoutLifecycle.this.overlays.iterator();
        MapComponent c;
        while(iter.hasNext()) {
            c = iter.next();
            try {
                c.onCreate(CommoutLifecycle.this.pluginContext, 
                           arg0.getIntent(), 
                           CommoutLifecycle.this.mapView);
            } catch(Exception e) {
               Log.w(TAG, "Unhandled exception trying to create overlays MapComponent", e);
               iter.remove();
            }
        }
    }

    @Override
    public void onDestroy() {
        for(MapComponent c : this.overlays)
            c.onDestroy(this.pluginContext, this.mapView);
    }

    @Override
    public void onFinish() {
        // XXX - no corresponding MapComponent method
    }

    @Override
    public void onPause() {
        for(MapComponent c : this.overlays)
            c.onPause(this.pluginContext, this.mapView);
    }

    @Override
    public void onResume() {
        for(MapComponent c : this.overlays)
            c.onResume(this.pluginContext, this.mapView);
    }

    @Override
    public void onStart() {
        for(MapComponent c : this.overlays)
            c.onStart(this.pluginContext, this.mapView);
    }

    @Override
    public void onStop() {
        for(MapComponent c : this.overlays)
            c.onStop(this.pluginContext, this.mapView);
    }
}
