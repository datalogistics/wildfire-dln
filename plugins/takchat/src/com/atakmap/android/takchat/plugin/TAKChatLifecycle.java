package com.atakmap.android.takchat.plugin;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import com.atakmap.android.maps.MapComponent;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.takchat.TAKChatMapComponent;
import com.atakmap.android.takchat.TAKChatUtils;
import com.atakmap.android.util.NotificationUtil;
import com.atakmap.coremap.log.Log;

import transapps.maps.plugin.lifecycle.Lifecycle;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;

/**
 * ATAK Plugin interface for plugin lifecycle events
 */
public class TAKChatLifecycle implements Lifecycle {

    private final static String TAG = "TAKChatLifecycle";

    private final Collection<MapComponent> _overlays;

    public TAKChatLifecycle(Context ctx) {
        TAKChatUtils.pluginContext = ctx;
        this._overlays = new LinkedList<MapComponent>();
    }

    @Override
    public void onConfigurationChanged(Configuration arg0) {
        for(MapComponent c : this._overlays)
            c.onConfigurationChanged(arg0);
    }

    @Override
    public void onCreate(final Activity arg0, final transapps.mapi.MapView arg1) {
        if(arg1 == null || !(arg1.getView() instanceof MapView)) {
            Log.w(TAG, "This plugin is only compatible with ATAK MapView");
            return;
        }

        TAKChatUtils.mapView = (MapView)arg1.getView();
        TAKChatUtils.takChatComponent = new TAKChatMapComponent();

        TAKChatLifecycle.this._overlays.add(TAKChatUtils.takChatComponent);

        // create components
        Iterator<MapComponent> iter = TAKChatLifecycle.this._overlays.iterator();
        MapComponent c;
        while(iter.hasNext()) {
            c = iter.next();
            try {
                c.onCreate(TAKChatUtils.pluginContext,
                           arg0.getIntent(),
                        TAKChatUtils.mapView);
            } catch(Exception e) {
                Log.w(TAG, "Unhandled exception trying to create _overlays MapComponent", e);
                iter.remove();
                NotificationUtil.getInstance().postNotification(
                         NotificationUtil.GeneralIcon.NETWORK_ERROR.getID(),
                        "Chat Server Disconnected",
                        "Failed to initialize: " + TAKChatUtils.pluginContext.getString(R.string.app_name),
                        "Failed to initialize: " + TAKChatUtils.pluginContext.getString(R.string.app_name));
            }
        }
    }

    @Override
    public void onDestroy() {
        for(MapComponent c : this._overlays)
            c.onDestroy(TAKChatUtils.pluginContext, TAKChatUtils.mapView);
    }

    @Override
    public void onFinish() {
        // XXX - no corresponding MapComponent method
    }

    @Override
    public void onPause() {
        for(MapComponent c : this._overlays)
            c.onPause(TAKChatUtils.pluginContext, TAKChatUtils.mapView);
    }

    @Override
    public void onResume() {
        for(MapComponent c : this._overlays)
            c.onResume(TAKChatUtils.pluginContext, TAKChatUtils.mapView);
    }

    @Override
    public void onStart() {
        for(MapComponent c : this._overlays)
            c.onStart(TAKChatUtils.pluginContext, TAKChatUtils.mapView);
    }

    @Override
    public void onStop() {
        for(MapComponent c : this._overlays)
            c.onStop(TAKChatUtils.pluginContext, TAKChatUtils.mapView);
    }
}