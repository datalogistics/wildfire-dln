
package com.atakmap.android.wxreport.plugin;

import com.atakmap.android.maps.MapView;

import android.content.Context;

public class ContextHelperSingleton
{
    private static ContextHelperSingleton mInstance = null;
    private MapView mapView;
    private Context pluginContext;

    private ContextHelperSingleton()
    {
        mapView = null;
        pluginContext = null;
    }

    public static ContextHelperSingleton getInstance()
    {
        if (mInstance == null)
        {
            mInstance = new ContextHelperSingleton();
        }
        return mInstance;
    }

    public MapView getMapView()
    {
        return this.mapView;
    }

    public void setMapView(MapView value)
    {
        mapView = value;
    }

    public Context getPluginContext()
    {
        return this.pluginContext;
    }

    public void setPluginContext(Context value)
    {
        pluginContext = value;
    }
}
