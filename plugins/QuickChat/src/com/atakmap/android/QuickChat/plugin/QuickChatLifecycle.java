package com.atakmap.android.QuickChat.plugin;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;

import com.atakmap.android.QuickChat.components.QuickChatMapComponent;
import com.atakmap.android.QuickChat.utils.PluginHelper;
import com.atakmap.android.maps.MapComponent;
import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.log.Log;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import transapps.maps.plugin.lifecycle.Lifecycle;

public class QuickChatLifecycle implements Lifecycle {

    private final Context pluginContext;
    private final Collection<MapComponent> overlays;
    private MapView mapView;
    public static final String TAG = "QuickChat";

    public QuickChatLifecycle(Context ctx) {
        this.pluginContext = ctx;
        PluginHelper.pluginContext = ctx;
        this.overlays = new LinkedList<MapComponent>();
        this.mapView = null;
    }

    @Override
    public void onConfigurationChanged(Configuration arg0) {
        for (MapComponent c : this.overlays)
            c.onConfigurationChanged(arg0);
    }

    @Override
    public void onCreate(final Activity arg0, final transapps.mapi.MapView arg1) {
        if (arg1 == null || !(arg1.getView() instanceof MapView)) {
            Log.w(TAG, "This plugin is only compatible with ATAK MapView");
            return;
        }

        this.mapView = (MapView) arg1.getView();

        //add classes that contain UI elements
        //the component class extending from these classes tell ATAK where they will be shown

        //Map components - dropdown
        //preference components - preference fragments/files
        QuickChatLifecycle.this.overlays
                .add(new QuickChatMapComponent());

        // create components
        Iterator<MapComponent> iter = QuickChatLifecycle.this.overlays
                .iterator();
        MapComponent c;
        while (iter.hasNext()) {
            c = iter.next();
            try {
                c.onCreate(QuickChatLifecycle.this.pluginContext,
                        arg0.getIntent(),
                        QuickChatLifecycle.this.mapView);
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
        Log.d(TAG,"Destroying Quick Chat Plugin");
        for (MapComponent c : this.overlays)
            c.onDestroy(this.pluginContext, this.mapView);
    }

    @Override
    public void onFinish() {
        // XXX - no corresponding MapComponent method
    }

    @Override
    public void onPause() {
        for (MapComponent c : this.overlays)
            c.onPause(this.pluginContext, this.mapView);
    }

    @Override
    public void onResume() {
        for (MapComponent c : this.overlays)
            c.onResume(this.pluginContext, this.mapView);
    }

    @Override
    public void onStart() {
        for (MapComponent c : this.overlays)
            c.onStart(this.pluginContext, this.mapView);

    }

    @Override
    public void onStop() {
        for (MapComponent c : this.overlays)
            c.onStop(this.pluginContext, this.mapView);
    }
}
