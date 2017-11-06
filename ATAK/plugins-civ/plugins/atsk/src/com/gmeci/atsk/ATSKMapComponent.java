
package com.gmeci.atsk;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import com.atakmap.android.ipc.AtakBroadcast.DocumentedIntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.net.Uri;
import android.provider.Settings;
import android.widget.Toast;

import com.gmeci.atsk.map.ATSKMapManager;
import com.gmeci.atskservice.resolvers.AZProviderClient;
import com.atakmap.android.atsk.plugin.R;
import com.atakmap.android.dropdown.DropDownMapComponent;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapView;
import com.atakmap.app.preferences.ToolsPreferenceFragment;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;
import com.gmeci.atsk.gallery.ATSKGalleryUtils;
import com.gmeci.atsk.resources.ATSKApplication;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import android.os.Environment;
import com.gmeci.atsk.export.ATSKMissionPackageManager;

public class ATSKMapComponent extends DropDownMapComponent {

    public final static String TAG = "ATSKMapComponent";
    public static ATSKFragment testFrag;
    final String[] REQAPPS = new String[] {
            "com.gmeci.atskservice"
    };
    private ATSKMissionPackageManager mpManager;
    private MapView _mapView;
    private MapGroup _mapGroup;
    private final BroadcastReceiver actionBarReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            showATSK();
        }
    };

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

    public static int getInternalVersion(final Context context) {
        BufferedReader bin = null;
        try {
            AssetManager assetManager = context.getAssets();
            InputStream in = assetManager.open("apks/version.txt");
            bin = new BufferedReader(new InputStreamReader(in,
                    FileSystemUtils.UTF8_CHARSET));
            String read = bin.readLine();
            return Integer.parseInt(read);
        } catch (Exception e) {
            return 17439;
        } finally {
            try {
                if (bin != null)
                    bin.close();
            } catch (IOException ioe) {
            }
        }
    }

    /**
     * Install an APK in the asset folder designated by the name.
     */
    public static File unrollAPK(final Context context,
            final Context atakContext, final String name) {
        Log.d(TAG, "unrolling: " + name);
        AssetManager assetManager = context.getAssets();

        InputStream in = null;
        OutputStream out = null;

        File apkroot = FileSystemUtils.getItem("apks");
        if (!apkroot.mkdir())
            Log.w(TAG, "Failed to create directory" + apkroot.getAbsolutePath());

        File apkfile = new File(apkroot, name);
        try {
            in = assetManager.open("apks/" + name);

            try {
                out = new FileOutputStream(apkfile);

                byte[] buffer = new byte[1024];

                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            } finally {
                in.close();
            }

            out.flush();

            return apkfile;
        } catch (Exception e) {
            Log.e(TAG, "failed to unroll: " + name, e);
        } finally {
            if (out != null)
                try {
                    out.close();
                } catch (IOException ioe) {
                    Log.e(TAG, "failed to close: " + name, ioe);
                }
        }
        return null;
    }

    public void install(final Context atakContext, final File apkfile) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(apkfile),
                "application/vnd.android.package-archive");
        atakContext.startActivity(intent);
    }

    public static ATSKFragment getFragment() {
        return testFrag;
    }

    public static ATSKFragmentManager getATSKFM() {
        if (testFrag != null)
            return testFrag.getATSKFM();
        return null;
    }

    public static boolean backButtonPressed() {
        return testFrag.backButtonPressed();
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

    private void checkUnknown(Context context) {
        try {
            boolean isNonPlayAppAllowed = Settings.Secure.getInt(
                    context.getContentResolver(),
                    Settings.Secure.INSTALL_NON_MARKET_APPS) == 1;

            if (!isNonPlayAppAllowed) {
                Toast.makeText(context,
                        "Enable allow UNKNOWN SOURCES to continue",
                        Toast.LENGTH_LONG).show();
                context.startActivity(new Intent(
                        android.provider.Settings.ACTION_SECURITY_SETTINGS));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Given a package name, request android to start uninstalling.
     */
    public void uninstall(Context context, final String packageName) {
        Intent intent = new Intent(Intent.ACTION_DELETE, Uri.fromParts(
                "package", packageName, null));
        context.startActivity(intent);
    }

    public boolean checkAndInstall(final Context context, final Intent intent,
            final MapView view) {

        final int REQVERSION = getInternalVersion(context);
        Log.d(TAG, "atsk internally supplied applications version: "
                + REQVERSION);

        boolean success = true;
        for (int i = 0; i < REQAPPS.length; ++i) {
            int appver = getVersion(context, REQAPPS[i]);
            Log.d(TAG, "atsk application installed: " + REQAPPS[i]
                    + " version: " + appver + " up-to-date: "
                    + (appver >= REQVERSION));
            success = success && isInstalled(context, REQAPPS[i]) &&
                    appver >= REQVERSION;
        }

        if (!success) {
            checkUnknown(view.getContext());

            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(
                    view.getContext());
            alertBuilder
                    .setTitle("Missing Applications")
                    .setMessage(
                            "This device is missing one or more applications with a version "
                                    + REQVERSION
                                    + " or greater.  Without these applications, ATSK will fail to run correctly.  Please click OK to start the installation process.  For each Android installation screen that appears, please accept the conditions to allow the application to install.  When installed, click Done."
                                    +
                                    "\n\tATSK Notebook")
                    .setPositiveButton("OK",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                        int which) {
                                    new Thread(new Runnable() {
                                        public void run() {
                                            // legacy
                                            if (isInstalled(context,
                                                    "com.atskpreferences"))
                                                uninstall(view.getContext(),
                                                        "com.atskpreferences");

                                            if (isInstalled(context,
                                                    "com.gmeci.hardwareservice"))
                                                uninstall(view.getContext(),
                                                        "com.gmeci.hardwareservice");

                                            //if (isInstalled(context,
                                            //        "com.gmeci.atskservice"))
                                            //    uninstall(view.getContext(),
                                            //            "com.gmeci.atskservice");

                                            final File localAPK = unrollAPK(
                                                    context,
                                                    view.getContext(),
                                                    "ATSKService-release.apk");
                                            postCheckAndInstall(context,
                                                    intent, view);
                                            if (localAPK != null)
                                                install(view.getContext(),
                                                        localAPK);
                                        }
                                    }, "ATSK-required").start();
                                }
                            });
            alertBuilder.create().show();
            return false;
        }
        return true;
    }

    private boolean launchAndBurn(Context context) {
        String pkg = "com.gmeci.atskservice";
        String act = pkg + ".MainActivity";
        try {
            PackageManager pm = context.getPackageManager();
            pm.getPackageInfo(pkg, PackageManager.GET_META_DATA);
            Log.d(TAG, "found " + pkg + " on the device");
        } catch (PackageManager.NameNotFoundException e) {
            Log.d(TAG, "could not find " + pkg + " on the device");
            return false;
        }
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(pkg, act));
        intent.putExtra("burnSupportFiles", "true");
        context.startActivity(intent);
        return true;

    }

    private void postCheckAndInstall(final Context context,
            final Intent intent,
            final MapView view) {
        final AlertDialog.Builder alertBuilder = new AlertDialog.Builder(
                view.getContext());
        alertBuilder
                .setTitle("Post Installation - Please Read")
                .setMessage(
                        "Wait for android to install the missing files.  If installation was successfull, the next step is to install the support files.\n\nIf you had trouble installing any of the applications or android reported an error during installation due to mismatched signatures, please click Uninstall and Reinstall to attempt to correct the error.")
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {

                                waitForItLaunchBurn(context, intent, view);
                            }
                        })
                .setNegativeButton("Uninstall and Reinstall",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                for (int i = 0; i < REQAPPS.length; ++i) {
                                    uninstall(view.getContext(),
                                            REQAPPS[i]);
                                }
                                waitForIt(context, intent, view);
                            }
                        });
        view.post(new Runnable() {
            public void run() {
                alertBuilder.create().show();
            }
        });
    }

    private void waitForIt(final Context context, final Intent intent,
            final MapView view) {
        final AlertDialog.Builder alertBuilder = new AlertDialog.Builder(
                view.getContext());
        alertBuilder
                .setTitle("Uninstall")
                .setMessage(
                        "Please press OK when the uninstall is complete.")
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                onCreate(context, intent, view);
                            }
                        });
        view.post(new Runnable() {
            public void run() {
                alertBuilder.create().show();
            }
        });
    }

    private void waitForItLaunchBurn(final Context context,
            final Intent intent,
            final MapView view) {
        final AlertDialog.Builder alertBuilder = new AlertDialog.Builder(
                view.getContext());
        alertBuilder
                .setTitle("Installing Support Files")
                .setMessage(
                        "Please wait until the ATSK Notebook launches and installs the support files.\nOnce the installation is complete, please press OK.")
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                onCreateSuccess(context, intent, view);
                            }
                        });
        view.post(new Runnable() {
            public void run() {
                alertBuilder.create().show();
            }
        });
        launchAndBurn(view.getContext());
    }

    @Override
    public void onCreate(Context context, Intent intent, MapView view) {

        // on the plugin side, use the appropriate broadcast implementation.
        AZProviderClient.setBroadcastImpl(new AZProviderClient.BroadcastImpl() {
            public void sendBroadcast(final Context context, final Intent intent) {
                com.atakmap.android.ipc.AtakBroadcast.getInstance()
                        .sendBroadcast(intent);
            }
        });

        String[] RequiredTemplateFiles = {
                "Aircraft.txt", "dz_requirements.csv", "farp_ac.csv",
                "hlz_data.csv", "parking_plan.csv"
        };

        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(view.getContext());
        File userManual = new File(Environment.getExternalStorageDirectory()
                .getAbsolutePath() + "/atsk/ATAK_Survey_ToolKit.pdf");

        if (!userManual.exists() || !sp.getString("atsk.doc", "").equals("2.0")) {
            sp.edit().putString("atsk.doc", "2.0").apply();
            copyFromAssetToFile(context,
                    "docs/ATAK_Survey_ToolKit.pdf", userManual);
        }

        if (checkAndInstall(context, intent, view)) {
            final String BaseFolder = Environment.getExternalStorageDirectory()
                    .getAbsolutePath() + "/atsk/az_templates/";
            boolean burn = false;
            for (int i = 0; i < 1; i++) {
                String FullFileName = BaseFolder + RequiredTemplateFiles[i];
                File testFile = new File(FullFileName);
                if (!testFile.exists()) {
                    Log.d(TAG, "missing file detected: " + testFile);
                    burn = true;
                }
            }

            if (burn)
                waitForItLaunchBurn(context, intent, view);
            else
                onCreateSuccess(context, intent, view);

        }

    }

    public void onCreateSuccess(Context context, Intent intent, MapView view) {

        context.setTheme(R.style.ATAKPluginTheme);

        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(view.getContext());
        sp.edit()
                .putString("atsk.version",
                        "" + getVersion(context, context.getPackageName()))
                .apply();
        Log.d(TAG,
                "loaded ATSK plugin version: "
                        + getVersion(context, context.getPackageName()));

        ToolsPreferenceFragment
                .register(
                new ToolsPreferenceFragment.ToolPreference(
                        context.getString(R.string.atsk_prefs_title),
                        "Adjust ATSK options",
                        "atskPreference",
                        context.getResources().getDrawable(
                                R.drawable.ic_menu_atsk),
                        new ATSKPreferenceFragment(context)));

        _mapView = view;

        // instantiate the singleton
        new ATSKApplication(view.getContext(), context);

        mpManager = new ATSKMissionPackageManager(view, context);

        DocumentedIntentFilter filter = new DocumentedIntentFilter();
        filter.addAction(ATSKATAKConstants.ATSK_ACTION_BAR);
        AtakBroadcast.getInstance().registerReceiver(actionBarReceiver, filter);

        // ATSK marker-specific radial receivers
        ATSKMapManager.registerReceivers();

        super.onCreate(context, intent, view);
    }

    @Override
    public void onResume(Context context, MapView view) {
        super.onResume(context, view);
        _mapView = view;
    }

    private void showATSK() {

        Log.d(TAG, "show ATSK");
        //MIKE - I should probably check to see if it exists first.......
        if (testFrag == null || !testFrag.isVisible()
                || !ATSKFragment.isOpen) {
            if (_mapGroup == null)
                _mapGroup = _mapView.getRootGroup().addGroup(
                        ATSKATAKConstants.ATSK_MAP_GROUP_TEST);
            testFrag = new ATSKFragment();
            testFrag.setMapView(_mapView);

            Intent atskFragmentIntent = new Intent();
            atskFragmentIntent.setAction("com.gmeci.atsk.OPEN_DROP_DOWN");
            atskFragmentIntent.putExtra(ATSKATAKConstants.TYPE_EXTRA,
                    ATSKATAKConstants.ATSK_EXTRA);
            AtakBroadcast.getInstance().sendBroadcast(atskFragmentIntent);

        }

    }

    @Override
    public void onPause(Context context, MapView view) {
        super.onPause(context, view);

    }

    @Override
    public void onStop(Context context, MapView view) {

    }

    @Override
    protected void onDestroyImpl(Context context, MapView view) {
        super.onDestroyImpl(context, view);
        final ATSKApplication app = ATSKApplication.getInstance();
        if (app != null)
            app.dispose();

        if (mpManager != null)
            mpManager.Stop();
        try {
            AtakBroadcast.getInstance().unregisterReceiver(actionBarReceiver);
            ATSKMapManager.unregisterReceivers();
        } catch (Exception e) {
            // if the tool has not been run, then the actionBarReceiver 
            // was never registered to begin with.
        }
        ATSKGalleryUtils.clearTempImages();
        ToolsPreferenceFragment.unregister("atskPreference");
        view.getContext().stopService(
                new Intent(PDFWriterService.SERVICE_INTENT));
    }

    public static boolean copyFromAssetToFile(Context context,
            String fileName, File outputFile) {

        InputStream in = null;
        OutputStream out = null;

        // attempt to make sure that the actual directory exists
        // before attempting to write into it.
        if (!outputFile.getParentFile().mkdir())
            Log.w(TAG, "Parent directory could not be created" +
                    outputFile.getParentFile().getAbsolutePath());

        try {
            in = context.getAssets().open(fileName);
            out = new FileOutputStream(outputFile);
            FileSystemUtils.copyStream(in, out);
        } catch (IOException ioe) {
            Log.e(TAG, "could not copy " + fileName + " to " + outputFile, ioe);
            try {
                if (in != null)
                    in.close();
            } catch (IOException ignore) {
            }
            try {
                if (out != null)
                    out.close();
            } catch (IOException ignore) {
            }

            return false;
        }
        return true;
    }

}
