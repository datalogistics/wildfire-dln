
package com.atakmap.android.helloworld;
import android.content.Context;
import android.preference.Preference;

import com.atakmap.android.gui.PanEditTextPreference;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.preference.PluginPreferenceFragment;
import com.atakmap.android.helloworld.plugin.R;
import android.os.Bundle;
import android.view.View;

public class HelloWorldPreferenceFragment extends PluginPreferenceFragment {

    private static Context staticPluginContext;
    public static final String TAG = "HellWorldPreferenceFragment";

    /**
     * Only will be called after this has been instantiated with the 1-arg constructor.
     * Fragments must has a zero arg constructor.
     */
    public HelloWorldPreferenceFragment() {
        super(staticPluginContext, R.xml.preferences);
    }

    public HelloWorldPreferenceFragment(final Context pluginContext) {
        super(pluginContext, R.xml.preferences);
        staticPluginContext = pluginContext;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((PanEditTextPreference) findPreference("key_for_helloworld")).checkValidInteger();
    }

    @Override
    public String getSubTitle() {
        return getSubTitle("Tool Preferences", "Hello World Preferences");
    }

}
