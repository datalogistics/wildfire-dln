
package com.atakmap.android.QuickChat.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.atakmap.android.QuickChat.history.ExportHistory;
import com.atakmap.android.QuickChat.history.SavedMessageHistory;
import com.atakmap.android.QuickChat.plugin.R;
import com.atakmap.android.contact.Contact;
import com.atakmap.android.contact.Contacts;
import com.atakmap.android.maps.MapView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Scott Auman on 5/13/2016.
 * Localizes the tools for working with the plugin outside of the main package
 * or where you cannot get a reference you need where you are working @
 */
public class PluginHelper {

    public static Context pluginContext;

    public static void showDropDownExists() {
        Toast.makeText(MapView.getMapView().getContext(),
                "Drop Down Already Showing", Toast.LENGTH_SHORT).show();
    }

    public static SharedPreferences getMapViewPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(MapView
                .getMapView().getContext());
    }

    public static String getPluginStringFromResources(int id) {
        return pluginContext.getResources().getString(id);
    }

    public static void showExportDialog(Context context) {

        if (SavedMessageHistory.getAllMessagesInHistory(MapView.getMapView()
                .getContext()) == null
                ||
                SavedMessageHistory.getAllMessagesInHistory(
                        MapView.getMapView().getContext()).equals(
                        SavedMessageHistory.getDefault())) {
            Toast.makeText(MapView.getMapView().getContext(),
                    "No History To Export", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Choose File Format")
                .setCancelable(true)
                .setSingleChoiceItems
                (PluginHelper.pluginContext.getResources()
                        .getStringArray(R.array.files_array), -1,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                // The 'which' argument contains the index position
                                // of the selected item
                                new ExportHistory().buildFile(which);
                                dialog.dismiss();
                            }
                        });
        builder.create();
        builder.show();
    }

    /**
     * @return whether the device is a tablet or not!
     */
    public static boolean isTablet() {
        return MapView.getMapView().getContext().getResources()
                .getBoolean(com.atakmap.app.R.bool.isTablet);
    }

    /**
     * @return whether or not the users device has a vibrator equipped
     * used to alert users of notification events and system events
     */
    public static boolean hasVibrator() {
        Vibrator mVibrator = (Vibrator) MapView.getMapView().getContext()
                .getSystemService(Context.VIBRATOR_SERVICE);
        return mVibrator.hasVibrator();
    }

    public static boolean isDeviceTablet() {
        return MapView.getMapView().getContext().getResources()
                .getBoolean(com.atakmap.app.R.bool.isTablet);
    }

    /**
     * @param uuid the user uuid specific code for each user
     * @return a string version that represents their current callsign
     */
    public static String getCallsignFromContactUid(String uuid) {
        if (uuid != null) {
            Contact contact = Contacts.getInstance().getContactByUuid(uuid);
            if (contact != null) {
                return contact.getName();
            }
        }
        return null;
    }

    public static List<String> convertContactsToString(
            List<String> allContactsInTeam) {
        List<String> callsigns = new ArrayList<String>();
        for (String s : allContactsInTeam) {
            String string = getCallsignFromContactUid(s);
            if (string != null) {
                callsigns.add(string);
            }
        }
        return callsigns;
    }
}
