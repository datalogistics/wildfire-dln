
package com.atakmap.android.wxreport.prefs;

import android.content.Context;
import android.content.Intent;

import com.atakmap.android.ipc.AtakBroadcast.DocumentedIntentFilter;
import com.atakmap.android.maps.AbstractMapComponent;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.wxreport.plugin.R;
import com.atakmap.app.preferences.ToolsPreferenceFragment;

public class WeatherPreferencesComponent extends AbstractMapComponent {
    public final static String SHOW_WEATHER_PREFS =
            "com.atakmap.android.wxreport.WeatherPreferencesComponent.SHOW_PREFS";

    private WeatherPreferencesReceiver preferencesReceiver;

    @Override
    public void onCreate(final Context context, Intent intent,
            final MapView view) {
        preferencesReceiver = new WeatherPreferencesReceiver(context, view);

        DocumentedIntentFilter filter = new DocumentedIntentFilter();
        filter.addAction(SHOW_WEATHER_PREFS);
        registerReceiver(context, preferencesReceiver, filter);

        view.post(new Runnable() {
            public void run() {
                WeatherPreferenceFragment cspf = new WeatherPreferenceFragment(context);

                ToolsPreferenceFragment.register(
                        new ToolsPreferenceFragment.ToolPreference(
                                "Weather Report Preferences",
                                "Adjust the Weather Report preferences",
                                "weather_report_preference",
                                context.getResources().getDrawable(
                                        R.drawable.weatherreportplugin48),
                                cspf));
            }
        });

    }

    @Override
    protected void onDestroyImpl(Context context, MapView view) {
        ToolsPreferenceFragment.unregister("weather_report_preference");
    }

    @Override
    public void onStart(Context context, MapView view) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onStop(Context context, MapView view) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onPause(Context context, MapView view) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onResume(Context context, MapView view) {
        // TODO Auto-generated method stub

    }
}
