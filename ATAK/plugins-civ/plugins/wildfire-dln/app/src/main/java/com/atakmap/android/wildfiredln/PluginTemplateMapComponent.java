
package com.atakmap.android.wildfiredln;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import com.atakmap.android.ipc.AtakBroadcast.DocumentedIntentFilter;

import com.atakmap.android.maps.MapView;
import com.atakmap.android.dropdown.DropDownMapComponent;

import com.atakmap.coremap.log.Log;
import com.atakmap.android.wildfiredln.plugin.R;

public class PluginTemplateMapComponent extends DropDownMapComponent {

    public static final String TAG = PluginTemplateMapComponent.class.getSimpleName();

    public Context pluginContext;
    private Activity pluginActivity;

    public void setActivity(Activity aa)
    {
        Log.d(TAG, "Setting Plugin Activity");
        pluginActivity = aa;
    }

    public void onCreate(final Context context, Intent intent, final MapView view) {

        context.setTheme(R.style.ATAKPluginTheme);
        super.onCreate(context, intent, view);
        pluginContext = context;

        com.atakmap.android.wildfiredln.PluginTemplateDropDownReceiver ddr = new com.atakmap.android.wildfiredln.PluginTemplateDropDownReceiver(view,context);
        ddr.setActivity(pluginActivity);

        Log.d(TAG, "registering the show plugin template filter");
        DocumentedIntentFilter ddFilter = new DocumentedIntentFilter();
        ddFilter.addAction(com.atakmap.android.wildfiredln.PluginTemplateDropDownReceiver.SHOW_PLUGIN_TEMPLATE);
        this.registerReceiver(context, ddr, ddFilter);
        Log.d(TAG, "registered the show plugin template filter");
    }

    @Override
    protected void onDestroyImpl(Context context, MapView view) {
        super.onDestroyImpl(context, view);
    }

}
