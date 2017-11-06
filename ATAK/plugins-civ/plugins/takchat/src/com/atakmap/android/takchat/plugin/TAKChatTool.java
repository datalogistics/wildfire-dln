package com.atakmap.android.takchat.plugin;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.view.ViewGroup;

import com.atakmap.android.contact.Contacts;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.takchat.TAKChatDropDownReceiver;
import com.atakmap.android.takchat.TAKChatMapComponent;
import com.atakmap.android.takchat.TAKChatUtils;
import com.atakmap.android.takchat.data.ChatDatabase;
import com.atakmap.android.takchat.data.ChatMessage;
import com.atakmap.android.takchat.data.MessageUnreadListener;
import com.atakmap.android.takchat.net.ConnectivityListener;
import com.atakmap.android.takchat.net.TAKChatXMPP;
import com.atakmap.android.takchat.view.badge.AtakLayerDrawableUtil;
import com.atakmap.android.tools.ActionBarReceiver;
import com.atakmap.coremap.log.Log;

import transapps.mapi.MapView;
import transapps.maps.plugin.tool.Group;
import transapps.maps.plugin.tool.Tool;
import transapps.maps.plugin.tool.ToolDescriptor;

/**
 * ATAK Plugin interface, inject point for user interaction with this plugin's UI
 */
public class TAKChatTool extends Tool implements ToolDescriptor, MessageUnreadListener, ConnectivityListener {

    private static final String TAG = "TAKChatTool";

    private final Context _pluginContext;
    private LayerDrawable _icon;

    private static TAKChatTool _instance;

    public static TAKChatTool getInstance(){
        //created via ATAK Plugin loader, via reflection
        return _instance;
    }

    public TAKChatTool(Context context) {
        _instance = this;
        this._pluginContext = context;
        _icon = (LayerDrawable) _pluginContext.getResources().getDrawable(R.drawable.xmpp_badge);
    }

    @Override
    public String getDescription() {
        return TAKChatUtils.getPluginString(R.string.app_name);
    }

    @Override
    public Drawable getIcon() {
        if(_icon != null)
            return _icon;

        Log.w(TAG, "Failed to load LayerDrawable");
        return (_pluginContext == null) ? null : _pluginContext.getResources().getDrawable(R.drawable.ic_launcher);
    }

    @Override
    public Group[] getGroups() {
        return new Group[] {Group.GENERAL};
    }

    @Override
    public String getShortDescription() {
        return TAKChatUtils.getPluginString(R.string.app_name);
    }

    @Override
    public Tool getTool() {
        return this;
    }

    @Override
    public void onActivate(Activity arg0, MapView arg1, ViewGroup arg2, Bundle arg3,
            ToolCallback arg4) {

        // Hack to close the dropdown that automatically opens when a tool
        // plugin is activated.
        if (arg4 != null) {
            arg4.onToolDeactivated(this);
        }
        // Intent to launch the dropdown or tool

        //arg2.setVisibility(ViewGroup.INVISIBLE);
        if(TAKChatUtils.takChatComponent != null &&
                TAKChatUtils.takChatComponent.getDropDown() != null) {
            TAKChatUtils.takChatComponent.getDropDown().
                    setAssociationKey(TAKChatMapComponent.TAKCHAT_PREFERENCE);
        }

        Intent i = new Intent(TAKChatDropDownReceiver.SHOW_CONTACT_LIST);
        AtakBroadcast.getInstance().sendBroadcast(i);
    }

    @Override
    public void onDeactivate(ToolCallback arg0) {}

    @Override
    public void onMessageRead(ChatMessage message) {}

    @Override
    public void onUnreadCountChanged() {
        final int count = ChatDatabase.getInstance(TAKChatUtils.pluginContext).getUnreadCount(null);
        final boolean bConnected = TAKChatXMPP.getInstance().isConnected();
        Log.d(TAG, "onUnreadCountChanged: " + bConnected + ", " + count);

        if(_icon != null){
            TAKChatUtils.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Contacts.getInstance().updateTotalUnreadCount();

                    AtakLayerDrawableUtil.getInstance(TAKChatUtils.pluginContext).setBadgeCount(
                            _icon, null, count, bConnected);
                    AtakBroadcast.getInstance().sendBroadcast(new Intent(ActionBarReceiver.REFRESH_ACTION_BAR));
                }
            });
        }
    }

    @Override
    public void dispose() {}

    /**
     * Immediately refresh icon
     */
    public void refreshIcon(){
        onUnreadCountChanged();
    }

    @Override
    public boolean onConnected() {
        onUnreadCountChanged();
        return true;
    }

    @Override
    public boolean onStatus(boolean bSuccess, String status) {
        return true;
    }

    @Override
    public boolean onDisconnected() {
        onUnreadCountChanged();
        return true;
    }
}
