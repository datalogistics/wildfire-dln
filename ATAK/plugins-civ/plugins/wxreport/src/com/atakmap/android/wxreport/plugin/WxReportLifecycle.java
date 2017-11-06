
package com.atakmap.android.wxreport.plugin;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;

import com.atakmap.android.maps.MapComponent;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.wxreport.WxReportMapComponent;
import com.atakmap.android.wxreport.prefs.WeatherPreferencesComponent;
import com.atakmap.coremap.log.Log;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import transapps.maps.plugin.lifecycle.Lifecycle;

public class WxReportLifecycle implements Lifecycle
{
    // Lifecycle represents the state of existence of plugin
    // It also contains stages of life that plugin experiences
    private final Context pluginContext;
    private final Collection<MapComponent> overlays;
    private MapView mapView;

    private final static String TAG = WxReportLifecycle.class.getSimpleName();

    public WxReportLifecycle(Context ctx)
    {
        this.pluginContext = ctx;
        this.overlays = new LinkedList<MapComponent>();
        this.mapView = null;
    }

    @Override
    public void onConfigurationChanged(Configuration arg0)
    {
        // If any configuration changes, inform the map overlay array
        for (MapComponent c : this.overlays)
        {
            c.onConfigurationChanged(arg0);
        }
    }

    @Override
    public void onCreate(final Activity arg0, final transapps.mapi.MapView arg1)
    {
        // Only work if running within ATAK
        if (arg1 == null || !(arg1.getView() instanceof MapView))
        {
            Log.w(TAG, "This plugin is only compatible with ATAK MapView");
            return;
        }

        // Add components to view
        this.mapView = (MapView) arg1.getView();
        WxReportLifecycle.this.overlays.add(new WxReportMapComponent());

        this.overlays.add(new WeatherPreferencesComponent());

        // Create components
        Iterator<MapComponent> iter = WxReportLifecycle.this.overlays
                .iterator();
        MapComponent c;
        while (iter.hasNext())
        {
            c = iter.next();
            try
            {
                c.onCreate(WxReportLifecycle.this.pluginContext,
                        arg0.getIntent(), WxReportLifecycle.this.mapView);
            } catch (Exception e)
            {
                Log.w(TAG,
                        "Unhandled exception trying to create overlays MapComponent",
                        e);
                iter.remove();
            }
        }
    }

    @Override
    public void onDestroy()
    {
        // Remove map overlay components
        for (MapComponent c : this.overlays)
        {
            c.onDestroy(this.pluginContext, this.mapView);
        }
    }

    @Override
    public void onFinish()
    {
        // XXX - no corresponding MapComponent method
    }

    @Override
    public void onPause()
    {
        // If pause is called on LifeCycle?
        for (MapComponent c : this.overlays)
        {
            c.onPause(this.pluginContext, this.mapView);
        }
    }

    @Override
    public void onResume()
    {
        // For after pause
        for (MapComponent c : this.overlays)
        {
            c.onResume(this.pluginContext, this.mapView);
        }
    }

    @Override
    public void onStart()
    {
        // When this plugin starts
        for (MapComponent c : this.overlays)
        {
            c.onStart(this.pluginContext, this.mapView);
        }
    }

    @Override
    public void onStop()
    {
        // When plugin stops
        for (MapComponent c : this.overlays)
        {
            c.onStop(this.pluginContext, this.mapView);
        }
    }
}
