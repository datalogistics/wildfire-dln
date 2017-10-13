
package com.gmeci.atsk;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.os.Environment;
import android.content.SharedPreferences;

import com.atakmap.android.atsk.plugin.R;
import com.atakmap.android.preference.PluginPreferenceFragment;
import com.gmeci.atsk.resources.ATSKApplication;

import android.text.Html;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import java.io.File;
import android.content.Intent;
import android.net.Uri;
import com.atakmap.coremap.log.Log;

/**
 */
public class ATSKPreferenceFragment extends PluginPreferenceFragment {

    private static Context staticPluginContext;
    private Preference readmePreference;
    public static final String TAG = "ATSKPreferenceFragment";

    /**
     * Only will be called after this has been instantiated with the 1-arg constructor.
     * Fragments must has a zero arg constructor.
     */
    public ATSKPreferenceFragment() {
        super(staticPluginContext, R.xml.preferences);
    }

    public ATSKPreferenceFragment(final Context pluginContext) {
        super(pluginContext, R.xml.preferences);
        staticPluginContext = pluginContext;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Preference aboutPref = findPreference("aboutATSK");
        aboutPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {

                popAbout();
                return false;
            }
        });

        readmePreference = (Preference) findPreference("atsk_documentation");
        readmePreference
                .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        File userManual = new File(Environment
                                .getExternalStorageDirectory()
                                .getAbsolutePath()
                                + "/atsk/ATAK_Survey_ToolKit.pdf");

                        checkAndWarn(getActivity(), userManual.toString());
                        return true;
                    }
                });
    }

    @Override
    public String getSubTitle() {
        return getSubTitle("Tool Preferences", staticPluginContext
                .getString(R.string.atsk_prefs_title));
    }

    private int getVersion(Context context, String pkg) {
        PackageManager manager = context.getPackageManager();
        try {
            PackageInfo pInfo = manager.getPackageInfo(pkg,
                    PackageManager.GET_ACTIVITIES);
            return pInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return -1;

    }

    private void popAbout() {
        final AlertDialog.Builder build = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = LayoutInflater.from(pluginContext);

        View v = inflater.inflate(R.layout.blankscreen, null);
        final TextView text = (TextView) v.findViewById(R.id.message);

        Context pluginContext = ATSKApplication
                .getInstance().getPluginContext();

        int version = getVersion(pluginContext, pluginContext.getPackageName());
        text.setText(Html.fromHtml("<H1>ATSK Version: " + version + "</H1>"));

        build.setView(v);
        build.setPositiveButton("OK", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();

            }
        });

        final AlertDialog dialog = build.create();
        dialog.show();

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
