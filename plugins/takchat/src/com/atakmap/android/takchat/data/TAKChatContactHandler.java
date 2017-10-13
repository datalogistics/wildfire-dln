package com.atakmap.android.takchat.data;

import android.content.Intent;

import com.atakmap.android.contact.Contact;
import com.atakmap.android.contact.ContactConnectorManager;
import com.atakmap.android.contact.XmppConnector;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.takchat.TAKChatDropDownReceiver;
import com.atakmap.android.takchat.TAKChatUtils;
import com.atakmap.android.takchat.net.ContactManager;
import com.atakmap.android.takchat.net.VCardManager;
import com.atakmap.android.takchat.plugin.R;
import com.atakmap.android.tools.menu.ActionBroadcastData;
import com.atakmap.android.tools.menu.ActionBroadcastExtraStringData;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;

import org.jivesoftware.smackx.vcardtemp.packet.VCard;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import java.util.ArrayList;

/**
 * Supports initiating chat via ATAK radial menu
 *
 * Created by byoung on 9/13/2016.
 */
public class TAKChatContactHandler extends ContactConnectorManager.ContactConnectorHandler {
    private static final String TAG = "TAKChatContactHandler";

    @Override
    public boolean isSupported(String type) {
        return FileSystemUtils.isEquals(type, XmppConnector.CONNECTOR_TYPE);
    }

    @Override
    public boolean hasFeature(ContactConnectorManager.ConnectorFeature feature) {
        return feature == ContactConnectorManager.ConnectorFeature.Avatar ||
                feature == ContactConnectorManager.ConnectorFeature.Profile ||
                feature == ContactConnectorManager.ConnectorFeature.Presence ||
                feature == ContactConnectorManager.ConnectorFeature.NotificationCount;
    }

    @Override
    public String getName() {
        return TAKChatUtils.mapView.getContext().getString(com.atakmap.app.R.string.connector_xmpp);
    }

    @Override
    public boolean handleContact(String connectorType, String contactUID, String connectorAddress) {
        Log.d(TAG, "handleContact: " + connectorAddress + ", for: " + contactUID);

        Intent openChatIntent = new Intent(TAKChatDropDownReceiver.SHOW_CHAT);
        openChatIntent.putExtra("bareJid", connectorAddress);
        AtakBroadcast.getInstance().sendBroadcast(openChatIntent);
        return true;
    }

    @Override
    public Object getFeature(String connectorType, ContactConnectorManager.ConnectorFeature feature, String contactUID, String connectorAddress) {
        if(feature == ContactConnectorManager.ConnectorFeature.Avatar){
            //Log.d(TAG, "getFeature avatar searching for: " + connectorAddress);
            EntityBareJid jid = null;

            try {
                jid = JidCreate.entityBareFrom(connectorAddress);
            } catch (XmppStringprepException e) {
                Log.w(TAG, "getFeature avatar, failed to parse: " + connectorAddress + ", " + e.getMessage());
                return null;
            }

            ContactManager mgr = TAKChatUtils.takChatComponent.getManager(ContactManager.class);
            if(mgr == null){
                return false;
            }

            XmppContact contact = mgr.getContactById(jid);
            if(TAKChatUtils.isConference(contact)){
                return false;
            }

            final VCardManager vm = TAKChatUtils.takChatComponent.getManager(VCardManager.class);
            if(vm == null){
                return null;
            }

            final VCard card = vm.getVCard(jid, VCardManager.UpdateMode.IfNecessary);
            if(card == null){
                return null;
            }

            byte[] bytes = card.getAvatar();
            if(FileSystemUtils.isEmpty(bytes)) {
                return null;
            }

            //Log.d(TAG, "getFeature avatar found for: " + jid.toString());
            return new VCardAvatarFeature(card);
        }else if(feature == ContactConnectorManager.ConnectorFeature.Profile){
            //Log.d(TAG, "getFeature profile searching for: " + connectorAddress);
            EntityBareJid jid = null;

            try {
                jid = JidCreate.entityBareFrom(connectorAddress);
            } catch (XmppStringprepException e) {
                Log.w(TAG, "getFeature profile, failed to parse: " + connectorAddress + ", " + e.getMessage());
                return null;
            }

            ContactManager mgr = TAKChatUtils.takChatComponent.getManager(ContactManager.class);
            if(mgr == null){
                return false;
            }

            XmppContact contact = mgr.getContactById(jid);
            if(contact == null){
                return null;
            }

            if(TAKChatUtils.isConference(contact)){
                //Log.d(TAG, "getFeature profile found for conference: " + jid.toString());
                ArrayList<ActionBroadcastExtraStringData> extras =
                        new ArrayList<ActionBroadcastExtraStringData>();
                extras.add(new ActionBroadcastExtraStringData("bareJid", connectorAddress));
                extras.add(new ActionBroadcastExtraStringData("iconUri", XmppConnector.GetIconUri()));
                return new ActionBroadcastData(TAKChatDropDownReceiver.SHOW_CONF_PROFILE, extras);
            }

            //TODO dont need to pull avatar out of DB at the moment...
            final VCardManager vm = TAKChatUtils.takChatComponent.getManager(VCardManager.class);
            if(vm == null){
                return null;
            }

            final VCard card = vm.getVCard(jid, VCardManager.UpdateMode.IfNecessary);
            if(card == null){
                return null;
            }

            //Log.d(TAG, "getFeature profile found for: " + jid.toString());
            ArrayList<ActionBroadcastExtraStringData> extras =
                    new ArrayList<ActionBroadcastExtraStringData>();
            extras.add(new ActionBroadcastExtraStringData("bareJid", connectorAddress));
            extras.add(new ActionBroadcastExtraStringData("iconUri", XmppConnector.GetIconUri()));
            return new ActionBroadcastData(TAKChatDropDownReceiver.SHOW_PROFILE, extras);
        }else if(feature == ContactConnectorManager.ConnectorFeature.NotificationCount) {
            //Log.d(TAG, "getFeature notifications searching for: " + connectorAddress);
            EntityBareJid jid = null;

            try {
                jid = JidCreate.entityBareFrom(connectorAddress);
            } catch (XmppStringprepException e) {
                Log.w(TAG, "getFeature notifications, failed to parse: " + connectorAddress + ", " + e.getMessage());
                return null;
            }

            return Integer.valueOf(ChatDatabase.getInstance(TAKChatUtils.pluginContext).getUnreadCount(jid));
        } else if(feature == ContactConnectorManager.ConnectorFeature.Presence) {
            //Log.d(TAG, "getFeature presence searching for: " + connectorAddress);
            EntityBareJid jid = null;

            try {
                jid = JidCreate.entityBareFrom(connectorAddress);
            } catch (XmppStringprepException e) {
                Log.w(TAG, "getFeature presence, failed to parse: " + connectorAddress + ", " + e.getMessage());
                return null;
            }

            ContactManager mgr = TAKChatUtils.takChatComponent.getManager(ContactManager.class);
            if (mgr == null) {
                return null;
            }

            XmppContact contact = mgr.getContactById(jid);
            if (contact == null) {
                Log.w(TAG, "getFeature presence, failed to parse 2: " + connectorAddress);
                return null;
            }

            if (!contact.isAvailable()) {
                return Contact.UpdateStatus.DEAD;
            } else if (contact.isAway()) {
                return Contact.UpdateStatus.STALE;
            } else {
                return Contact.UpdateStatus.CURRENT;
            }
        }

        return null;
    }

    @Override
    public String getDescription() {
        return TAKChatUtils.getPluginString(R.string.app_name) + " provides (server based) XMPP chat";
    }
}
