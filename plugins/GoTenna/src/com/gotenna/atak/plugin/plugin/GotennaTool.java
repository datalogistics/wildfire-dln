package com.gotenna.atak.plugin.plugin;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import com.atakmap.android.ipc.AtakBroadcast.DocumentedIntentFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.ViewGroup;

import com.atakmap.coremap.log.Log;
import com.gotenna.atak.plugin.R;

import java.util.concurrent.atomic.AtomicBoolean;

import transapps.mapi.MapView;
import transapps.maps.plugin.tool.Group;
import transapps.maps.plugin.tool.Tool;
import transapps.maps.plugin.tool.ToolDescriptor;

public class GotennaTool extends Tool implements ToolDescriptor {

    private static final String TAG = "GotennaTool";

    private Context context;

    public GotennaTool(Context context) {
        this.context = context;

        context.setTheme(R.style.ATAKPluginTheme);
    }

    @Override
    public String getDescription() {
        return "GoTenna";
    }

    @Override
    public Drawable getIcon() {
        return (context == null) ? null : context.getResources().getDrawable(R.mipmap.ic_launcher);
    }

    @Override
    public Group[] getGroups() {
        return new Group[] {Group.GENERAL};
    }

    @Override
    public String getShortDescription() {
        return "GoTenna";
    }

    @Override
    public Tool getTool() {
        return this;
    }

    AtomicBoolean firstTime = new AtomicBoolean(true);

    @Override
    public void onActivate(final Activity activity, final MapView mapView, final ViewGroup viewGroup, final Bundle bundle, ToolCallback toolCallback) {
        Log.d(TAG, "GotennaTool Activated");
        viewGroup.setVisibility(ViewGroup.INVISIBLE);

        // Only register the receivers once
        if(firstTime.getAndSet(false)) {

            // Register the DropDown for SHOW_UI_INTENT_ACTION Intents
            GotennaDropDownReceiver dropDownReceiver = new GotennaDropDownReceiver((com.atakmap.android.maps.MapView) mapView.getView(), context);
            DocumentedIntentFilter dropDownIntentFilter = new DocumentedIntentFilter();
            dropDownIntentFilter.addAction(GotennaDropDownReceiver.SHOW_UI_INTENT_ACTION);
            activity.registerReceiver(dropDownReceiver, dropDownIntentFilter);

            GotennaSettingsDropDownReceiver settingsReceiver = new GotennaSettingsDropDownReceiver((com.atakmap.android.maps.MapView) mapView.getView(), context);
            DocumentedIntentFilter settingsIntentFilter = new DocumentedIntentFilter();
            settingsIntentFilter.addAction(GotennaSettingsDropDownReceiver.SHOW_SETTINGS_UI_INTENT_ACTION);
            activity.registerReceiver(settingsReceiver, settingsIntentFilter);

            GotennaEncryptionSettingsDropDownReceiver encryptionSettingsReceiver = GotennaEncryptionSettingsDropDownReceiver.initialize((com.atakmap.android.maps.MapView) mapView.getView(), context);
            DocumentedIntentFilter encryptionSettingsIntentFilter = new DocumentedIntentFilter();
            encryptionSettingsIntentFilter.addAction(GotennaEncryptionSettingsDropDownReceiver.SHOW_ENCRYPTION_SETTINGS_UI_INTENT_ACTION);
            activity.registerReceiver(encryptionSettingsReceiver, encryptionSettingsIntentFilter);

        }

        // Send an Intent to open the DropDown
        Intent openToolIntent = new Intent(GotennaDropDownReceiver.SHOW_UI_INTENT_ACTION);
        this.context.sendBroadcast(openToolIntent);
    }

    @Override
    public void onDeactivate(ToolCallback arg0) {}

}
