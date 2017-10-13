package com.atakmap.android.QuickChat.preferences;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.atakmap.android.QuickChat.chat.SavedFilteredPopupChatUsers;
import com.atakmap.android.QuickChat.components.QuickChatMapComponent;
import com.atakmap.android.QuickChat.history.SavedMessageHistory;
import com.atakmap.android.QuickChat.plugin.R;
import com.atakmap.android.QuickChat.utils.PluginHelper;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.preference.PluginPreferenceFragment;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Scott Auman on 5/11/2016.
 * the main preference class that handles the plugin preferences
 * the preferences is inflated from an XMl view, some elements are
 * device specific and added dynamically when main preference is disabled all prefs are disabled
 */
public class QuickChatPreferenceFragment extends
        PluginPreferenceFragment {

    public static final String QUICK_REPLY_LIST = "quick_reply_list";
    public static final String ACKNOWLEDGED = "Acknowledged";
    public static final String QUICK_REPLY_TEXT_KEY = "quick_reply_text_key";

    private CheckBoxPreference timePreference;
    private CheckBoxPreference largeTextPreference;
    private CheckBoxPreference markReadMessages;
    private CheckBoxPreference clearMessagesOnExit;
    private CheckBoxPreference vibratePref;
    private CheckBoxPreference enabledPopUp;
    private Preference bannerLocation;
    private Preference bannerTheme;
    private Preference quickReplyButton;

    private boolean vibration; //does the device support vibration?

    //handles the type of dialog to show theme-> the banner color theme
    //location-> where the banner will appear in Atak
    private DIALOG_TYPE dialog_type;

    private enum DIALOG_TYPE {
        LOCATION, THEME
    }

    public QuickChatPreferenceFragment() {
        super(PluginHelper.pluginContext, R.xml.chat_popup_pref_fragment);
    }

    public QuickChatPreferenceFragment(Context context) {
        super(context, R.xml.chat_popup_pref_fragment);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        vibration = PluginHelper.hasVibrator();

        quickReplyButton = findPreference("quick_reply_text_button");
        bannerLocation = findPreference("banner_location");
        bannerTheme = findPreference("banner_theme");
        timePreference = (CheckBoxPreference) findPreference("popup_24hr_time");
        largeTextPreference = (CheckBoxPreference) findPreference("popup_text_message_size");
        markReadMessages = (CheckBoxPreference) findPreference("popup_mark_message_read");
        enabledPopUp = (CheckBoxPreference) findPreference("chat_message_popup_dialog");
        Preference readmePreference = findPreference("chat_message_popups_documentation");
        clearMessagesOnExit = (CheckBoxPreference) findPreference("clear_history_on_exit");
        vibratePref = (CheckBoxPreference) findPreference("vibrate_on_popup");
        Preference clearSettings = (Preference) findPreference("clear_cmp_history");
        clearSettings.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                showConfirmDialog();
                return true;
            }
        });
        Preference export = findPreference("chat_message_popups_history_export");

        quickReplyButton
                .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        showTextDialog();
                        return true;
                    }
                });

        bannerLocation
                .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        dialog_type = DIALOG_TYPE.LOCATION;
                        showListPreferenceDialog("popup_banner_location", 0,
                                dialog_type);
                        return true;
                    }
                });

        bannerTheme
                .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        dialog_type = DIALOG_TYPE.THEME;
                        showListPreferenceDialog("chat_message_popups_style",
                                "0", dialog_type);
                        return true;
                    }
                });

        //exporting chat preference
        export.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                //ask what format for user?
                PluginHelper.showExportDialog(getActivity());
                return true;
            }
        });

        //main banner preference
        enabledPopUp
                .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference,
                            Object newValue) {
                        if ((Boolean) newValue) {
                            enablePrefs();
                        } else {
                            disablePrefs();
                        }
                        return true;
                    }
                });

        //documentation preference
        readmePreference
                .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        File f = FileSystemUtils
                                .getItem(QuickChatMapComponent.DIRECTORY_PATH
                                        + File.separator
                                        + QuickChatMapComponent.USER_GUIDE);

                        checkAndWarn(getActivity(), f.toString());
                        return true;
                    }
                });

        if (!vibration) {
            vibratePref.setEnabled(false);
        }
        //handle dependency state with main preference
        if (enabledPopUp.isChecked()) {
            enablePrefs();
        } else {
            disablePrefs();
        }
    }

    private void showConfirmDialog() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                getActivity());

        // set title
        alertDialogBuilder.setTitle("Confirm Plugin Settings Reset");

        // set dialog message
        alertDialogBuilder
                .setMessage("Clear and reset all plugin settings?, This will erase all stored data relevant to this plugin")
                .setCancelable(false)
                .setPositiveButton("Yes",new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        // if this button is clicked, close
                        // current activity
                        resetAll();
                    }
                })
                .setNegativeButton("No",new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        // if this button is clicked, just close
                        // the dialog box and do nothing
                        dialog.cancel();
                    }
                });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
    }

    private void resetAll() {

        //clear history out
        SavedMessageHistory.clearMessageHistory(MapView.getMapView().getContext());

        //clear saved users
        SavedFilteredPopupChatUsers.saveNewUserList(MapView.getMapView().getContext(),SavedFilteredPopupChatUsers.getDefault());

        //reset screen preferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MapView.getMapView().getContext());
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("chat_message_popups_style","0");
        editor.putInt("popup_banner_location",0);
        editor.putString(QUICK_REPLY_TEXT_KEY,ACKNOWLEDGED);
        editor.putStringSet(QUICK_REPLY_LIST,getDefaultQuickTextList());

        //commit all changes from this editor
        editor.apply();
        refresh();
    }

    private HashSet<String> getDefaultQuickTextList(){
        HashSet<String> replys = new HashSet<String>();
        replys.add(ACKNOWLEDGED);
        return replys;
    }

    private void refresh() {
        timePreference.setChecked(true);
        largeTextPreference.setChecked(true);
        markReadMessages.setChecked(true);
        vibratePref.setChecked(false);
        clearMessagesOnExit.setChecked(false);
        enabledPopUp.setChecked(true);
        enablePrefs();
    }

    /**
     * double dialog method displays the list of quick reply strings in the first
     * dialog, the second dialog displays an edit text allowing for input from user.
     */
    private void showTextDialog() {

        AlertDialog.Builder builderSingle = new AlertDialog.Builder(
                getActivity());
        final String[] items = convertSetToStringArray(getCurrentReplyList());
        builderSingle.setPositiveButton("ADD QUICK TEXT",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();//dismiss this dialog before showing next
                        showInputDialog();
                    }
                });
        builderSingle.setSingleChoiceItems(items,
                findCurrentClickedIndexOfQuickReply(),
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        PreferenceManager
                                .getDefaultSharedPreferences(
                                        MapView.getMapView().getContext())
                                .edit()
                                .putString(QUICK_REPLY_TEXT_KEY, items[arg1])
                                .apply();
                    }
                });
        Dialog dialog = builderSingle.create();
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.show();
    }

    /**converts a set<> to a native string array
     */
    private String[] convertSetToStringArray(Set<String> setString) {
        String[] array = new String[setString.size()];
        List<String> set = new ArrayList<String>();
        set.addAll(setString);
        for (int i = 0; i < set.size(); i++) {
            array[i] = set.get(i);
        }
        return array;
    }

    private void showInputDialog(){
        final AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).create();
        final EditText input = new EditText(getActivity());
        InputFilter[] filterArray = new InputFilter[1];
        filterArray[0] = new InputFilter.LengthFilter(12);
        input.setFilters(filterArray);
        input.setHint("");
        alertDialog.setTitle("Quick Reply Word");
        alertDialog.setMessage("Enter Word To Add To Quick Reply Selection List");
        alertDialog.setView(input);
        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(!input.getText().toString().equals("") || input.getText().toString().length() > 0){
                    addTextToReplyList(input.getText().toString().trim());
                }else{
                    Toast.makeText(getActivity(),"No Text Entered",Toast.LENGTH_SHORT).show();
                }
            }
        });
        alertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                showTextDialog();
            }
        });
        alertDialog.show();
    }

    /**adds the string to the current quick reply list stored in preferences
     */
    private void addTextToReplyList(String string) {
        Set<String> list = getCurrentReplyList();
        list.add(string);

        PreferenceManager
                .getDefaultSharedPreferences(MapView.getMapView().getContext())
                .edit().putStringSet(QUICK_REPLY_LIST, list).apply();
    }

    /**pings the preferences and returns the current string set that
     * contains the user defined quick reply text, will never return the actual empty set list
     * because we initiate the list with a default label on plugin launch
     * @return  Set<String> Collection</String>
     */
    private Set<String> getCurrentReplyList() {
        return getPreferenceManager().getSharedPreferences().getStringSet(
                QuickChatPreferenceFragment.QUICK_REPLY_LIST,
                getDefaultQuickTextList());
    }

    private int findCurrentClickedIndexOfQuickReply() {
        String replyString = getPreferenceManager()
                .getSharedPreferences()
                .getString(
                        QuickChatPreferenceFragment.QUICK_REPLY_TEXT_KEY,
                        QuickChatPreferenceFragment.ACKNOWLEDGED);

        List<String> list = new ArrayList<String>();
        list.addAll(getPreferenceManager().getSharedPreferences().getStringSet(
                QuickChatPreferenceFragment.QUICK_REPLY_LIST,
                new HashSet<String>()));

        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).equals(replyString)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public String getSubTitle() {
        return null;
    }

    /**
     * disables the preferences  that pertain to the main
     * preference , no need to allow changing of preference is
     * the main is not enabled
     */
    private void disablePrefs() {
        timePreference.setEnabled(false);
        markReadMessages.setEnabled(false);
        largeTextPreference.setEnabled(false);
        bannerLocation.setEnabled(false);
        clearMessagesOnExit.setEnabled(false);
        bannerTheme.setEnabled(false);
        if (vibration)
            vibratePref.setEnabled(false);
        quickReplyButton.setEnabled(false);
    }

    /**
     * enables child preferences  pertaining to the main preference
     * when main preference is enabled set child pref to enabled
     */
    private void enablePrefs() {
        timePreference.setEnabled(true);
        markReadMessages.setEnabled(true);
        largeTextPreference.setEnabled(true);
        bannerLocation.setEnabled(true);
        clearMessagesOnExit.setEnabled(true);
        bannerTheme.setEnabled(true);
        if (vibration)
            vibratePref.setEnabled(true);
        quickReplyButton.setEnabled(true);
    }

    private void showListPreferenceDialog(final String keyToUpdate,
            final Object pullType, DIALOG_TYPE type) {

        final Dialog dialog = new Dialog(getActivity());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View view;

        if (type == DIALOG_TYPE.LOCATION) {
            String theme = getPreferenceManager().getSharedPreferences()
                    .getString("chat_message_popups_style", "0");
            view = LayoutInflater.from(pluginContext).inflate(
                    theme.equals("0") ?
                            R.layout.image_dialog_wht
                            : R.layout.image_dialog_blk, null);
        } else {//showing theme dialog
            int loc = getPreferenceManager().getSharedPreferences().getInt(
                    "popup_banner_location", 0);
            view = LayoutInflater.from(pluginContext).inflate(loc == 0 ?
                    R.layout.theme_dialog_center : R.layout.theme_dialog_top,
                    null);
        }

        dialog.setContentView(view);
        dialog.findViewById(R.id.imageView1).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        setNewCustomListDialogValue(keyToUpdate, "0", pullType);
                        dialog.dismiss();
                    }
                });
        dialog.findViewById(R.id.imageView2).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        setNewCustomListDialogValue(keyToUpdate, "1", pullType);
                        dialog.dismiss();
                    }
                });
        setCurrentSelection(dialog, keyToUpdate, type, pullType);
        dialog.show();
    }

    /**
     * Sets the white border around one of the imageviews to designate the current selection
     * for when opening the dialog.
     */
    private void setCurrentSelection(Dialog dialog, String keyToUpdate,
            DIALOG_TYPE dType, Object type) {

        RelativeLayout layout = null;
        if (type instanceof Integer) {
            int i = PreferenceManager.getDefaultSharedPreferences(
                    MapView.getMapView().getContext()).getInt
                    (keyToUpdate, 0);
            layout = (RelativeLayout) dialog.findViewById(
                    i == 0 ? R.id.imageView1 : R.id.imageView2).getParent();
        } else if (type instanceof String) {
            String i = PreferenceManager.getDefaultSharedPreferences(
                    MapView.getMapView().getContext()).getString
                    (keyToUpdate, "0");
            layout = (RelativeLayout) dialog.findViewById(
                    i.equals("0") ? R.id.imageView1 : R.id.imageView2)
                    .getParent();
        }

        if (layout != null)
            layout.setBackgroundColor(Color.WHITE);
    }

    /**saved the new value in the specific preference key
     */
    private void setNewCustomListDialogValue(String key,String value,
            Object type) {

        if (type instanceof Integer) {
            PreferenceManager
                    .getDefaultSharedPreferences(
                            MapView.getMapView().getContext())
                    .edit().putInt(key, Integer.parseInt(value)).apply();
        } else if (type instanceof String) {
            PreferenceManager
                    .getDefaultSharedPreferences(
                            MapView.getMapView().getContext())
                    .edit().putString(key, value).apply();
        } else {
            Log.d(TAG, "how did we get here?");
        }
    }

    /**
     * Checks to see if adobe is installed and present.   Issues a warning otherwise.
     */
    private static void checkAndWarn(final Context context, final String file) {
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
