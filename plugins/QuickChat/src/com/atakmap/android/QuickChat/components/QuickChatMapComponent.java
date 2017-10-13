
package com.atakmap.android.QuickChat.components;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.atakmap.android.QuickChat.chat.FilterChatUserDropDown;
import com.atakmap.android.QuickChat.chat.NewMessageReceiver;
import com.atakmap.android.QuickChat.history.QuickChatHistoryDropDown;
import com.atakmap.android.QuickChat.history.SavedMessageHistory;
import com.atakmap.android.QuickChat.plugin.R;
import com.atakmap.android.QuickChat.preferences.QuickChatPreferenceFragment;
import com.atakmap.android.QuickChat.utils.PluginHelper;
import com.atakmap.android.chat.GeoChatService;
import com.atakmap.android.contact.Contacts;
import com.atakmap.android.contact.IndividualContact;
import com.atakmap.android.dropdown.DropDownMapComponent;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.ipc.AtakBroadcast.DocumentedIntentFilter;
import com.atakmap.android.maps.MapView;
import com.atakmap.app.preferences.ToolsPreferenceFragment;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**@author  Scott Auman
 * Plugin Componet class that registers  a dropdown class
 * superclass handles the contexts as well as the lifecycle of the receiver
 *
 */
public class QuickChatMapComponent extends DropDownMapComponent
        implements Contacts.OnContactsChangedListener {

    private static final String TAG = "QuickChatMapComponent";

    public static final String DIRECTORY_PATH = "tools" + File.separator
            + "quickchat";
    public static final String USER_GUIDE = "ATAKQuickChat 7-25-17.pdf";

    private  NewMessageReceiver newMessageReceiver;
    private QuickChatHistoryDropDown historyDropDown;
    private FilterChatUserDropDown filterReceiver;

    public static GeoChatService chatService;

    public void onCreate(Context context, Intent intent, final MapView view) {
        super.onCreate(context, intent, view);
        chatService = GeoChatService.getInstance();

        //make sure file structure is correct
        FileSystemUtils.ensureDataDirectory(DIRECTORY_PATH, false);

        //make sure user guide is in CMP directory
        String pdf = DIRECTORY_PATH + File.separator + USER_GUIDE;
        if (!FileSystemUtils.getItem(pdf).exists())
        {
            FileSystemUtils.copyFromAssetsToStorageFile(
                    context,
                    USER_GUIDE,
                    DIRECTORY_PATH + File.separator + USER_GUIDE,
                    false);
        }
        buildFilterDropDownReceiver(context);
        addInReplyString(QuickChatPreferenceFragment.ACKNOWLEDGED);
        //Contacts.getInstance().addListener(this);


        newMessageReceiver = new NewMessageReceiver();
        // DocumentedIntentFilter for incoming chat messages
        DocumentedIntentFilter newChatMessageFilter = new DocumentedIntentFilter();
        newChatMessageFilter
                .addAction("com.atakmap.android.ChatMessagePopups.NEW_CHAT_RECEIVED");
        AtakBroadcast.getInstance().registerReceiver(newMessageReceiver,
                newChatMessageFilter);

        ToolsPreferenceFragment.register(
                new ToolsPreferenceFragment.ToolPreference(
                        "Quick Chat Preferences",
                        "Adjust Quick Chat preferences",
                        "chat_message_popup_dialog_tools",
                        PluginHelper.pluginContext.getResources().getDrawable(
                                R.drawable.chatmessageplugin48),
                        new QuickChatPreferenceFragment(context)));
    }

    public static void addSentMessageToDB(Bundle msg){
        Intent intent = new Intent("com.atakmap.chatmessage.persistmessage");
        intent.putExtra("chat_bundle",msg);
        AtakBroadcast.getInstance().sendBroadcast(intent);
    }

    public static void sendMessageOut(Bundle msg, IndividualContact individualContact) {
        if (chatService != null) {
            chatService.sendMessage(msg, individualContact);
        }
    }

    private static void addInReplyString(String string) {
        Set<String> stringSet;
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(MapView.getMapView().getContext());
        stringSet = prefs.getStringSet(
                QuickChatPreferenceFragment.QUICK_REPLY_LIST,
                new HashSet<String>());

        if (!stringSet.contains(string)) {
            stringSet.add(string);
            prefs.edit()
                    .putStringSet(
                            QuickChatPreferenceFragment.QUICK_REPLY_LIST,
                            stringSet).apply();
            Log.d(TAG, "added to reply list " + string);
        } else {
            Log.d(TAG, string + " already exists");
        }
    }

    private void buildFilterDropDownReceiver(Context context) {
        //build and show fragment handling the custom filter list for showing pop ups for specific users
        filterReceiver = new FilterChatUserDropDown(
                com.atakmap.android.maps.MapView.getMapView(), context);
        // DocumentedIntentFilter for opening a chat window
        DocumentedIntentFilter filterUsersPopup = new DocumentedIntentFilter();
        filterUsersPopup.addAction("com.atakmap.android.FILTER_USERS_POPUPS");
        filterUsersPopup
                .addAction("com.atakmap.android.CONTACTS_CHANGED_EVENT");
        AtakBroadcast.getInstance().registerReceiver(filterReceiver,
                filterUsersPopup);

        historyDropDown = new QuickChatHistoryDropDown(
                MapView.getMapView());
        //intent receiver for history popup chat dropdown
        DocumentedIntentFilter historyDropDownReceiver = new DocumentedIntentFilter();
        historyDropDownReceiver
                .addAction("com.atakmap.android.QuickChat.SHOW_HISTORY_DROPDOWN");
        historyDropDownReceiver
                .addAction("com.atakmap.android.QuickChat.ADD_MESSAGE_TO_LIST");
        AtakBroadcast.getInstance().registerReceiver(historyDropDown,
                historyDropDownReceiver);

    }

    @Override
    protected void onDestroyImpl(Context context, MapView view) {
        super.onDestroyImpl(context, view);

        Log.d(TAG,"Removing message receiver, history dropdownReceiver");
        AtakBroadcast.getInstance().unregisterReceiver(historyDropDown);
        AtakBroadcast.getInstance().unregisterReceiver(newMessageReceiver);
        AtakBroadcast.getInstance().unregisterReceiver(filterReceiver);

        if (filterReceiver != null) {
            filterReceiver.onDestroy();
            filterReceiver = null;
        }

        if(historyDropDown != null){
            historyDropDown.onDestroy();
            historyDropDown = null;
        }

        //check clear history on plugin unload
        if (PreferenceManager.getDefaultSharedPreferences(MapView.getMapView()
                .getContext()).getBoolean("clear_history_on_exit", false)) {
            //clear all message history
            SavedMessageHistory.clearMessageHistory(MapView.getMapView()
                    .getContext());
            Log.d(TAG, "Message History Cleared!");
        }

        //unregister the preference with the tools
        ToolsPreferenceFragment.unregister("chat_message_popup_dialog_tools");
    }

    @Override
    public void onContactsSizeChange(Contacts contacts) {
        AtakBroadcast.getInstance().sendBroadcast(
                new Intent("com.atakmap.android.CONTACTS_CHANGED_EVENT"));
    }

    @Override
    public void onContactChanged(String uuid) {
        AtakBroadcast.getInstance().sendBroadcast(
                new Intent("com.atakmap.android.CONTACTS_CHANGED_EVENT"));
    }
}
