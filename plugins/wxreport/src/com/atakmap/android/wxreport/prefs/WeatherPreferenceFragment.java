
package com.atakmap.android.wxreport.prefs;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.atakmap.android.preference.AtakPreferenceFragment;
import com.atakmap.android.wxreport.WxReportMapComponent;
import com.atakmap.android.wxreport.plugin.R;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;

import java.io.File;

public class WeatherPreferenceFragment extends AtakPreferenceFragment {

    private Context _pluginContext;

    public WeatherPreferenceFragment(Context pluginContext) {
        _pluginContext = pluginContext;
    }

    @Override
    public String getSubTitle() {
        return getSubTitle("Tools Preferences","Weather Report Settings");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final PreferenceScreen screen = getPreferenceManager().
                createPreferenceScreen(getActivity());
        setPreferenceScreen(screen);

        ListPreference tempUnitPreference = new ListPreference(getActivity());
        tempUnitPreference.setEntries(_pluginContext.getResources().getStringArray( R.array.tempUnitEntries));
        tempUnitPreference.setEntryValues(_pluginContext.getResources().getStringArray(R.array.tempUnitValues));
        tempUnitPreference.setDefaultValue("0");
        tempUnitPreference.setKey("weather_temp_key");
        tempUnitPreference.setTitle("Temperature Unit");
        tempUnitPreference.setSummary("Sets the temperature unit of measurement to use");
        tempUnitPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                return true;
            }
        });
        screen.addPreference(tempUnitPreference);

        Preference readmePref = new Preference(getActivity());
        readmePref.setTitle("Weather Report Plugin Documentation");
        readmePref.setSummary("Get Help With Using This Tool By Browsing The User Guide");
        readmePref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                File f = FileSystemUtils
                        .getItem(WxReportMapComponent.DIRECTORY_PATH
                                + File.separator
                                + WxReportMapComponent.USER_GUIDE);

                checkAndWarn(getActivity(), f.toString());
                return true;
            }
        });

        screen.addPreference(readmePref);
    }

    /**
     * Checks to see if adobe is installed and present.   Issues a warning otherwise.
     */
    public static void checkAndWarn(final Context context, final String file) {
        final SharedPreferences _prefs = PreferenceManager
                .getDefaultSharedPreferences(context);
        boolean displayHint = _prefs.getBoolean("atak.hint.missingadobe", true);

        if (!isInstalled(context, "com.adobe.reader") && displayHint) {

            View v = LayoutInflater.from(context)
                    .inflate(com.atakmap.app.R.layout.hint_screen, null);
            TextView tv = (TextView) v
                    .findViewById(com.atakmap.app.R.id.message);
            tv.setText("It is recommended that you use the official Acrobat Reader.\nViewing the documents in other Android PDF applications may not work properly.");

            new AlertDialog.Builder(context)
                    .setTitle("Acrobat Reader Missing")
                    .setView(v)
                    .setCancelable(false)
                    .setPositiveButton("OK",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                        int id) {
                                    _prefs.edit()
                                            .putBoolean(
                                                    "atak.hint.missingadobe",
                                                    false).apply();
                                    launchAdobe(context, file);
                                }
                            }).create().show();
        } else {
            launchAdobe(context, file);
        }
    }

    private static boolean isInstalled(Context context, String pkg) {
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(pkg, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            Log.d(TAG, "Package not installed: " + pkg, e);
        }
        return false;
    }

    static private void launchAdobe(final Context context, final String file) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(new File(file)),
                    "application/pdf");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "error launching a pdf viewer", e);
        }
    }
}
