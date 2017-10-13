
package com.atakmap.android.wxreport.prefs;

import com.atakmap.android.maps.MapView;
import com.atakmap.app.SettingsActivity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class WeatherPreferencesReceiver extends BroadcastReceiver {

    protected final MapView mapView;
    protected final Context pluginContext;

    public WeatherPreferencesReceiver(Context pluginContext, MapView mapView) {
        this.mapView = mapView;
        this.pluginContext = pluginContext;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(
                WeatherPreferencesComponent.SHOW_WEATHER_PREFS)) {
            showPrefs(context);
        }
    }

    protected void showPrefs(Context context) {

        Intent intent = new Intent(mapView.getContext(), SettingsActivity.class);
        intent.putExtra("frag_class_name",
                WeatherPreferenceFragment.class.getName());

        Bundle extraParams = new Bundle();
        intent.putExtra("extra_params", extraParams);

        ((Activity) mapView.getContext()).startActivityForResult(intent, 0);
    }
}
