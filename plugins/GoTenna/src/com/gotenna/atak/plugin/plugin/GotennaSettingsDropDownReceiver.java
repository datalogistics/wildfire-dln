package com.gotenna.atak.plugin.plugin;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import com.atakmap.android.gui.PluginSpinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.atakmap.android.dropdown.DropDown;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.log.Log;
import com.gotenna.atak.plugin.R;
import com.gotenna.sdk.commands.GTCommandCenter;
import com.gotenna.sdk.commands.GTError;
import com.gotenna.sdk.interfaces.GTErrorListener;
import com.gotenna.sdk.responses.SystemInfoResponseData;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by kusbeck on 4/28/16.
 */
public class GotennaSettingsDropDownReceiver extends DropDownReceiver implements DropDown.OnStateListener {

    public static final String TAG = GotennaSettingsDropDownReceiver.class.getSimpleName();
    public static final String REMEMBER_PREVIOUS_ADDR = "rememberPreviousAddr";
    public static final String REMEMBER_PREVIOUS_USER = "rememberPreviousUser";
    public static final String BROADCAST_LOCATION = "broadcastLocation";
    public static final String SHOW_SETTINGS_UI_INTENT_ACTION = "SHOW_SETTINGS_UI_INTENT_ACTION";
    public static final String BROADCAST_LOCATION_FREQUENCY = "broadcastLocationFrequency";
    public static final int DEFAULT_BROADCAST_LOCATION_FREQUENCY = 180; // seconds
    public static final String BROADCAST_LOCATION_DISTANCE = "broadcastLocationDistance";
    public static final int DEFAULT_BROADCAST_LOCATION_DISTANCE = 50; // meters
    public static final String GOTENNA_TYPE = "gotennaDeviceType";
    public static final int DEFAULT_GOTENNA_TYPE = 1; // v1

    private View toolView;
    private Context pluginContext;
    private Context activityContext;
    private FirmwareUpdateHelper firmwareUpdateHelper;
    private EditText broadcastLocFrequency, broadcastLocDistance;
    private PluginSpinner gotennaTypeSelector;

    protected GotennaSettingsDropDownReceiver(MapView mapView, final Context context) {
        super(mapView);
        this.pluginContext = context;
    }

    @Override
    protected void disposeImpl() {

    }

    private SharedPreferences getPrefs() {
        return PreferenceManager.getDefaultSharedPreferences(getMapView().getContext());
    }

    private static final String DEFAULT_CLOSE_INTENT_ACTION = GotennaDropDownReceiver.SHOW_UI_INTENT_ACTION;
    private static final AtomicReference<String> intentActionForClose = new AtomicReference<String>(DEFAULT_CLOSE_INTENT_ACTION);

    @Override
    public void onReceive(Context context, Intent intent) {

        this.activityContext = context;

        Log.d(TAG, "Inflating GUI");
        final LayoutInflater inflater = (LayoutInflater) pluginContext.getSystemService(context.LAYOUT_INFLATER_SERVICE);
        toolView = inflater.inflate(R.layout.settings_layout, null);

        //showDropDown(toolView, HALF_WIDTH, FULL_HEIGHT, FULL_WIDTH, HALF_HEIGHT, false, this);
        showDropDown(toolView, HALF_WIDTH, FULL_HEIGHT, FULL_WIDTH, HALF_HEIGHT, this);




        ((Button)toolView.findViewById(R.id.refresh_sys_info_button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshSysInfo();
            }
        });

        this.firmwareUpdateHelper = new FirmwareUpdateHelper(context);
        try {
            this.firmwareUpdateHelper.checkForNewFirmwareFile();
        } catch (Exception e) {
            Log.w(TAG, "Error checking for firmware update", e);
        }

        ((Button)toolView.findViewById(R.id.update_firmware_button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "User clicked 'Update Firmware'");
                // For a firmware update, first we ask the goTenna what its current firmware version is so we can check if an update is needed
                try {
                    GTCommandCenter.getInstance().sendGetSystemInfo(new GTCommandCenter.GTSystemInfoResponseListener() {
                        @Override
                        public void onResponse(SystemInfoResponseData systemInfoResponseData) {
                            if (firmwareUpdateHelper.shouldDoFirmwareUpdate(systemInfoResponseData)) {
                                firmwareUpdateHelper.showFirmwareUpdateDialog(systemInfoResponseData);
                            } else {
                                Toast.makeText(activityContext, "Firmware is up-to-date.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }, new GTErrorListener() {
                        @Override
                        public void onError(GTError error) {
                            Log.w(TAG, error.toString());
                        }
                    });
                } catch (Exception e) {
                    Log.w(TAG, "Encountered exception while updating firmware...", e);
                }
            }
        });

        ((Button)toolView.findViewById(R.id.encryption_options_button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "User clicked 'Encryption Options' button");
                intentActionForClose.set(GotennaEncryptionSettingsDropDownReceiver.SHOW_ENCRYPTION_SETTINGS_UI_INTENT_ACTION);
                closeDropDown();
            }
        });

        // Get a handle on the shared preferences
        SharedPreferences prefs = getPrefs();

        final Switch rememberGotennaSwitch = ((Switch)toolView.findViewById(R.id.remember_gotenna_switch));
        rememberGotennaSwitch.setChecked(prefs.getBoolean(REMEMBER_PREVIOUS_ADDR, true));
        rememberGotennaSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                SharedPreferences prefs = getPrefs();
                prefs.edit()
                        .putBoolean(REMEMBER_PREVIOUS_ADDR, rememberGotennaSwitch.isChecked())
                        .apply();
            }
        });

//        final Switch rememberUserSwitch = ((Switch)toolView.findViewById(R.id.remember_user_switch));
//        rememberUserSwitch.setChecked(prefs.getBoolean(REMEMBER_PREVIOUS_USER, false));
//        rememberUserSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
//                SharedPreferences prefs = getPrefs();
//                prefs.edit()
//                        .putBoolean(REMEMBER_PREVIOUS_USER, rememberUserSwitch.isChecked())
//                        .apply();
//            }
//        });

        final Switch broadcastLocSwitch  = ((Switch)toolView.findViewById(R.id.broadcast_location_switch));
        broadcastLocSwitch.setChecked(prefs.getBoolean(BROADCAST_LOCATION, true));
        broadcastLocSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                SharedPreferences prefs = getPrefs();
                prefs.edit()
                        .putBoolean(BROADCAST_LOCATION, broadcastLocSwitch.isChecked())
                        .apply();
            }
        });

        gotennaTypeSelector = ((PluginSpinner)toolView.findViewById(R.id.gotenna_type_selector));
        int gtTypePref = prefs.getInt(GOTENNA_TYPE, DEFAULT_GOTENNA_TYPE);
        gotennaTypeSelector.setSelection(gtTypePref-1);

        broadcastLocFrequency = ((EditText)toolView.findViewById(R.id.broadcast_location_frequency));
        broadcastLocFrequency.setText(Integer.toString(prefs.getInt(BROADCAST_LOCATION_FREQUENCY, DEFAULT_BROADCAST_LOCATION_FREQUENCY)));

        broadcastLocDistance = ((EditText)toolView.findViewById(R.id.broadcast_location_distance));
        broadcastLocDistance.setText(Integer.toString(prefs.getInt(BROADCAST_LOCATION_DISTANCE, DEFAULT_BROADCAST_LOCATION_DISTANCE)));

    }

    void showToast(final String msg) {
        ((Activity)getMapView().getContext()).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getMapView().getContext(), msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void refreshSysInfo() {
        GTCommandCenter.getInstance().sendGetSystemInfo(new GTCommandCenter.GTSystemInfoResponseListener() {
            @Override
            public void onResponse(final SystemInfoResponseData systemInfoResponseData) {
                ((Activity) getMapView().getContext()).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ((TextView) toolView.findViewById(R.id.system_info_text)).setText(systemInfoResponseData.toString());
                    }
                });
            }
        }, new GTErrorListener() {
            @Override
            public void onError(final GTError gtError) {
                ((Activity) getMapView().getContext()).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ((TextView) toolView.findViewById(R.id.system_info_text)).setText(gtError.toString());
                    }
                });
            }
        });
    }




    @Override
    public void onDropDownSelectionRemoved() {

    }

    @Override
    public void onDropDownClose() {
        // Save any preferences that need saving...
        SharedPreferences prefs = getPrefs();
        try {
            prefs.edit()
                    .putInt(BROADCAST_LOCATION_FREQUENCY, Integer.parseInt(broadcastLocFrequency.getText().toString()))
                    .apply();
        } catch (Exception e) {
            Toast.makeText(getMapView().getContext(), "Could not save Location Frequency", Toast.LENGTH_SHORT);
        }

        try {
            prefs.edit()
                    .putInt(BROADCAST_LOCATION_DISTANCE, Integer.parseInt(broadcastLocDistance.getText().toString()))
                    .apply();
        } catch (Exception e) {
            Toast.makeText(getMapView().getContext(), "Could not save Location Distance", Toast.LENGTH_SHORT);
        }

        try {
            String selected = gotennaTypeSelector.getSelectedItem().toString().toLowerCase();
            if(selected.contains("mesh")) {
                prefs.edit()
                        .putInt(GOTENNA_TYPE, 2)
                        .apply();
            } else if(selected.contains("pro")) {
                prefs.edit()
                        .putInt(GOTENNA_TYPE, 3)
                        .apply();
            } else {
                prefs.edit()
                        .putInt(GOTENNA_TYPE, 1)
                        .apply();
            }
        } catch (Exception e) {
            //ignore
            Log.w(TAG, "caught unexpected exception handling gotenna type selection", e);
        }

        // Send an Intent to open the DropDown
        Intent openToolIntent = new Intent(intentActionForClose.getAndSet(DEFAULT_CLOSE_INTENT_ACTION));
        this.activityContext.sendBroadcast(openToolIntent);
    }

    @Override
    public void onDropDownSizeChanged(double v, double v1) {

    }

    @Override
    public void onDropDownVisible(boolean b) {

    }
}
