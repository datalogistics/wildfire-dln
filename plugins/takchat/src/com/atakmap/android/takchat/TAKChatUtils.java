package com.atakmap.android.takchat;

import android.content.Context;
import android.content.SharedPreferences;

import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.takchat.data.ChatDatabase;
import com.atakmap.android.takchat.data.ChatMessage;
import com.atakmap.android.takchat.data.XmppConference;
import com.atakmap.android.takchat.data.XmppContact;
import com.atakmap.android.takchat.net.ConnectionManager;
import com.atakmap.android.takchat.net.ContactManager;
import com.atakmap.android.takchat.plugin.R;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;
import com.atakmap.net.AtakAuthenticationCredentials;
import com.atakmap.net.AtakAuthenticationDatabase;

import org.jivesoftware.smack.packet.DefaultExtensionElement;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.StandardExtensionElement;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.id.StanzaIdUtil;
import org.jivesoftware.smackx.muc.HostedRoom;
import org.jivesoftware.smackx.muc.packet.MUCUser;
import org.jivesoftware.smackx.vcardtemp.packet.VCard;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Localpart;
import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.stringprep.XmppStringprepException;

import java.util.List;

/**
 * Common utils for the XMPP plugin
 *
 * Created by byoung on 6/29/2016.
 */
public class TAKChatUtils {

    private static final String TAG = "TAKChatUtils";

    public static final String TAK_XMPP_NAMESPACE = "urn:xmpp:tak";
    public static final String TAK_XMPP_ELEMENT = "tak";
    public static final String TAK_UID_XMPP_ELEMENT = "uid";

    public static Context pluginContext;
    public static MapView mapView;
    public static TAKChatMapComponent takChatComponent;

    public static String getPluginString(int r){
        return TAKChatUtils.pluginContext.getString(r);
    }


    /**
     * Create message from this device
     *
     * @param to
     * @param body
     * @return
     * @throws XmppStringprepException
     */
    public static Message createChat(String to, String body) throws XmppStringprepException {
        return createChat(TAKChatUtils.getUsernameFull(), to, body, StanzaIdUtil.newStanzaId());
    }

    /**
     * Create a chat message, and set the stanza ID
     *
     * @param from
     * @param to
     * @param body
     * @param stanzaId
     * @return
     */
    public static Message createChat(Jid from, String to, String body, String stanzaId) throws XmppStringprepException {

        //TODO error checking
        final Message out = new Message(JidCreate.from(to));
        out.setFrom(from);
        out.setBody(body);
        out.setStanzaId(stanzaId);
        out.setType(Message.Type.chat);
        return out;
    }

    /**
     * Get JID "local part" e.g. "user", for local device
     *
     * @return
     */
   // public static String getUsername(){
    public static String getUsernameLocalPart(){
        BareJid me = getUsernameBare();
        if(me == null || me.getLocalpartOrNull() == null){
            Log.w(TAG, "No username set");
            return null;
        }

        return me.getLocalpartOrNull().toString();
    }

    /**
     * Get bare JID e.g. "user@server", for local specified local part
     *
     * @param localPart of JID
     * @return
     */
    public static BareJid getJidFromLocal(String localPart) throws XmppStringprepException{
        if(FileSystemUtils.isEmpty(localPart))
            return null;

        String t = localPart.trim();
        if(FileSystemUtils.isEmpty(t))
            return null;

        BareJid me = getUsernameBare();
        if(me == null || me.getDomain() == null){
            Log.w(TAG, "No domain set");
            return null;
        }

        return JidCreate.bareFrom(Localpart.from(t), me.getDomain());
    }

    /**
     * Get Bare jid user@server, for local device
     *
     * @return
     */
    //public static BareJid getJid(){
    public static BareJid getUsernameBare(){
        //TODO cache this and listen for updates, do not query DB/prefs every time
        AtakAuthenticationCredentials credentials = AtakAuthenticationDatabase
                .getCredentials(TAKChatUtils.getPluginString(R.string.xmpp_credentials));
        if (credentials == null || FileSystemUtils.isEmpty(credentials.username)) {
            Log.w(TAG, "No user JID set");
            return null;
        }

        try {
            return JidCreate.bareFrom(credentials.username);
        } catch (XmppStringprepException e) {
            Log.w(TAG, "Failed to parse local bare jid", e);
        }

        return null;
    }

    /**
     *  Get full JID e.g. "user@server/resource", for local device
     * @return
     */
    //public static EntityFullJid getFullJid() {
    public static EntityFullJid getUsernameFull() {
        //TODO cache this and listen for updates, do not query DB/prefs every time
        BareJid me = getUsernameBare();
        if(me == null){
            Log.w(TAG, "No full jid set");
            return null;
        }

        //TODO is this the most efficient approach?
        try {
            return JidCreate.entityFullFrom(me.getLocalpartOrNull(),
                    me.getDomain(), Resourcepart.from(mapView.getDeviceUid()));
        } catch (XmppStringprepException e) {
            Log.w(TAG, "Failed to get full self JID", e);
        }
        return null;
    }

    public static boolean isMe(String jidString){
        EntityBareJid jid = null;

        try {
            jid = JidCreate.entityBareFrom(jidString);
        } catch (XmppStringprepException e) {
            Log.w(TAG, "Failed to parse: " + jidString, e);
            return false;
        }

        return isMe(jid);
    }

    public static boolean isMe(BareJid jid){
        if(jid == null)
            return false;

        BareJid me = getUsernameBare();
        if(me == null)
            return false;

        return FileSystemUtils.isEquals(jid.toString(), me.toString());
    }

    /**
     * available maps to XMPP Presence.Type
     * away maps to XMPP Presence.Mode
     *
     * @param available
     * @param away
     * @param status
     * @return
     */
    public static Presence createSelfPresence(boolean available, boolean away, String status){
        Presence message = new Presence(available ? Presence.Type.available : Presence.Type.unavailable);
        message.setMode(away ? Presence.Mode.away : Presence.Mode.available);
        if(!FileSystemUtils.isEmpty(status))
            message.setStatus(status);
        return message;
    }

    /**
     * available maps to XMPP Presence.Type
     * away maps to XMPP Presence.Mode
     *
     * @param available
     * @param away
     * @param status
     * @return
     */
    public static Presence createPresence(Jid from, boolean available, boolean away, String status){
        Presence message = new Presence(available ? Presence.Type.available : Presence.Type.unavailable);
        message.setFrom(from);
        message.setMode(away ? Presence.Mode.away : Presence.Mode.available);
        if(!FileSystemUtils.isEmpty(status))
            message.setStatus(status);
        return message;
    }


    /**
     * Check if the given stanza is sent by this device
     *
     * @param stanza
     * @return
     */
    public static boolean isMine(Stanza stanza){
        if(stanza == null) {
            return false;
        }

        //TODO is this DB access quick enough to do it often based on UI listview?
        //check "from" to see if I sent it
        BareJid me = getUsernameBare();
        //TODO need to ignore case?
        if(me != null && stanza.getFrom() != null && me.toString().equals(stanza.getFrom().asBareJid().toString())) {
            Log.d(TAG, "isMine (from): " + stanza.getFrom() + ", " + stanza.toString());
            return true;
        }

        //check if stanza has the TAK uid extension with my uid
        String takUid = TAKChatUtils.getUidExtension(stanza);
        if(takUid != null && takUid.equals(TAKChatUtils.mapView.getDeviceUid())){
            Log.d(TAG, "isMine (uid): " + TAKChatUtils.mapView.getDeviceUid() + ", " + stanza.toString());
            return true;
        }

        if(Message.class.isInstance(stanza)) {
            Message msg = (Message)stanza;
            if (msg.getType().equals(Message.Type.groupchat)) {
                Resourcepart resourcepart = msg.getFrom().getResourceOrNull();
                if(resourcepart != null && me.getLocalpartOrNull() != null) {
                    if(FileSystemUtils.isEquals(resourcepart.toString(), me.getLocalpartOrNull().toString()) ||
                        FileSystemUtils.isEquals(resourcepart.toString(), ConnectionManager.getResourceId())){
                        Log.d(TAG, "isMine (groupChat): " + me.toString() + ", " + stanza.toString());
                        return true;
                    }
                }
            }
        }

        //TODO remove some of this logging after testing
        Log.d(TAG, "!isMine: " + stanza.getFrom());
        return false;
    }


    public static void addUidExtension(Stanza stanza, String takUid) {
        if(stanza == null || takUid == null || takUid.length() < 1)
            return;

        //TODO convert to StandardExtensionElement or make a new extension class?
        DefaultExtensionElement extTak = new DefaultExtensionElement(TAK_XMPP_ELEMENT, TAK_XMPP_NAMESPACE);
        extTak.setValue("uid", takUid);
        stanza.addExtension(extTak);
    }

    public static String getUidExtension(Stanza stanza){
        if(stanza == null)
            return null;

        ExtensionElement extTak = stanza.getExtension(TAK_XMPP_NAMESPACE);
        if(extTak != null) {
            if(extTak instanceof DefaultExtensionElement){
                DefaultExtensionElement dee = (DefaultExtensionElement) extTak;
                return dee.getValue(TAK_UID_XMPP_ELEMENT);
            } else if(extTak instanceof StandardExtensionElement){
                StandardExtensionElement see = (StandardExtensionElement) extTak;
                StandardExtensionElement uidsee = see.getFirstElement(TAK_UID_XMPP_ELEMENT);
                if(uidsee != null)
                    return uidsee.getText();
            }
        }

        return null;
    }

    public static long getSyncPoint(SharedPreferences prefs, String label) {
        long now = System.currentTimeMillis();
        String prefLabel = ConnectionManager.SYNC_PREF_NAME + label;
        long timeLastSync = prefs.getLong(prefLabel, -1);

        //get amount of time user wants to sync
        long takChatSyncArchiveTime = Integer.parseInt(prefs.getString(
                "takChatSyncArchiveTime", "86400000"));
        if(takChatSyncArchiveTime <= 0){
            Log.d(TAG, "Not sync'ing chat archive");
            return -1;
        }
        long takSyncPrefTime = now - takChatSyncArchiveTime;

        //sync time is max (most recent) of last sync time, and how much user wants to sync
        long ret = timeLastSync <= 0 ? takSyncPrefTime : Math.max(timeLastSync, takSyncPrefTime);
//        Log.d(TAG, String.format("getSyncPoint %s, now=%d, lastSync=%d (%d), takSyncArchiveTime=%d, syncPrefTime=%d (%d), ret=%d (%d)",
//                prefLabel, now,
//                timeLastSync, (now-timeLastSync),
//                takChatSyncArchiveTime,
//                takSyncPrefTime, (now-takSyncPrefTime),
//                ret, (now-ret)));
        return ret;
    }

    public static void setSyncPoint(SharedPreferences prefs, String label, long syncPoint){
        String prefLabel = ConnectionManager.SYNC_PREF_NAME + label;
        //long now = System.currentTimeMillis();
        //Log.d(TAG, String.format("setSyncPoint %s  abs=%d, diff=%d", prefLabel, syncPoint, (now-syncPoint)));

        prefs.edit().putLong(prefLabel, syncPoint).apply();
    }

    public static void clearSyncPoint(SharedPreferences prefs, String label){
        prefs.edit().remove(ConnectionManager.SYNC_PREF_NAME + label).apply();
    }

    public static void runOnUiThread(Runnable runnable) {
        TAKChatUtils.mapView.post(runnable);
    }

    public static void runOnUiThreadDelayed(Runnable runnable, long delayMillis) {
        TAKChatUtils.mapView.postDelayed(runnable, delayMillis);
    }

    public static void runInBackground(Runnable runnable) {
        TAKChatUtils.takChatComponent.runInBackground(runnable);
    }

    public static boolean isGroupPresence(Stanza stanza) {
        if(stanza == null || !(stanza instanceof Presence))
            return false;

        return stanza.getExtension(MUCUser.NAMESPACE) != null;
    }

    public static boolean isConference(XmppContact contact){
        return XmppConference.class.isInstance(contact);
    }

    public static XmppContact getContactById(String id) {
        try {
            return takChatComponent.getManager(ContactManager.class).getContactById(JidCreate.bareFrom(id));
        } catch (XmppStringprepException e) {
            Log.e(TAG, "Error retrieving Contact: " + id, e);
        }
        return null;
    }

    /**
     * Get Subject: Body, using default language
     * Note, both subject and body are optional, per XMPP spec
     * Note, message may have multiple subject and/or body, for different languages
     *
     * @param message
     * @return
     */
    public static String getBody(Message message) {
        String ret = "";
        String subj = message.getSubject();
        String body = message.getBody();

        if(!FileSystemUtils.isEmpty(subj)){
            ret = subj;
            if(!FileSystemUtils.isEmpty(body)){
                ret += ": ";
            }
        }
        if(!FileSystemUtils.isEmpty(body)) {
            ret += body;
        }

        return ret;
    }

    public static String getBody(ChatMessage message) {
        return getBody(message.getMessage());
    }

    /**
     * Search contact for the specified terms
     * @param c XMPP contact
     * @param terms Search terms
     * @return True if terms are empty or match
     */
    public static boolean searchContact(XmppContact c, String terms) {
        // Search filter
        if (FileSystemUtils.isEmpty(terms))
            return true;
        // Search order:
        // 1 - Name
        // 2 - JID
        // 3 - Marker title
        // 4 - Details
        // 5 - Conversation
        return match(c.getName(), terms) || match(c.getId().toString(), terms)
                || searchMarker(c.getMarker(), terms) || searchCard(c.getVCard(), terms)
                || searchConversation(c, terms);
    }

    public static boolean searchRoom(HostedRoom room, String terms) {
        return FileSystemUtils.isEmpty(terms) || match(room.getName(), terms)
                || match(room.getJid().toString(), terms);
    }

    /**
     * Search a marker's title, callsign, and summary for certain terms
     * @param marker Map marker
     * @param terms Search terms
     * @return True if the marker's title, callsign, or summary contains the terms
     */
    public static boolean searchMarker(Marker marker, String terms) {
        return marker != null && (FileSystemUtils.isEmpty(terms) ||
                match(marker.getTitle(), terms) || match(marker.getMetaString(
                "callsign", ""), terms) || match(marker.getSummary(), terms));
    }

    /**
     * Search the contact card for certain terms
     * @param card Contact VCard
     * @param terms Search terms
     * @return True if card contains the search terms
     */
    public static boolean searchCard(VCard card, String terms) {
        return card != null && (FileSystemUtils.isEmpty(terms) || match(card.getNickName(), terms)
                || match(card.getJabberId(), terms) || match(card.getFirstName(), terms)
                || match(card.getMiddleName(), terms) || match(card.getLastName(), terms)
                || match(card.getSuffix(), terms) || match(card.getEmailWork(), terms)
                || match(card.getEmailHome(), terms) || match(card.getOrganization(), terms));
    }

    /**
     * Search a contact's entire conversation
     * @param c XMPP contact
     * @param terms Search terms
     * @return True if conversation contains the search terms
     */
    public static boolean searchConversation(XmppContact c, String terms) {
        if (FileSystemUtils.isEmpty(terms))
            return true;
        if (c != null && c.getId() != null) {
            List<ChatMessage> chatList = ChatDatabase.getInstance(TAKChatUtils
                    .pluginContext).retrieveHistory(c.getId().asBareJid());
            for (ChatMessage cMsg : chatList) {
                if (searchMessage(cMsg, terms))
                    return true;
            }
        }
        return false;
    }

    public static boolean searchMessage(ChatMessage cMsg, String terms) {
        if (FileSystemUtils.isEmpty(terms))
            return true;
        if (cMsg != null && cMsg.getMessage() != null) {
            Message msg = cMsg.getMessage();
            if (match(msg.getSubject(), terms) || match(msg.getBody(), terms))
                return true;
        }
        return false;
    }

    private static boolean match(String str, String terms) {
        return !FileSystemUtils.isEmpty(str) && str.toLowerCase().contains(terms);
    }

}
