package com.atakmap.android.takchat.net;

import android.content.Intent;
import android.widget.Toast;

import com.atakmap.android.contact.Contacts;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.takchat.TAKChatDropDownReceiver;
import com.atakmap.android.takchat.TAKChatUtils;
import com.atakmap.android.takchat.data.ChatDatabase;
import com.atakmap.android.takchat.data.ContactListener;
import com.atakmap.android.takchat.data.XmppConference;
import com.atakmap.android.takchat.data.XmppContact;
import com.atakmap.android.takchat.plugin.R;
import com.atakmap.android.takchat.view.TAKChatView;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PresenceTypeFilter;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smack.roster.RosterListener;
import org.jivesoftware.smack.roster.RosterLoadedListener;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.Jid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Created by scallya on 8/3/2016.
 */
public class ContactManager extends IManager<ContactListener> implements RosterListener, RosterLoadedListener, ConnectivityListener {

    private final static String TAG = "XmppContactManager";
    public final String DEFAULT_AVAILABLE_STATUS;
    private final String DEFAULT_AWAY_STATUS;
    private final String DEFAULT_OFFLINE_STATUS;

    private List<XmppContact> contacts = new ArrayList<XmppContact>();

    private String myStatus;

    public ContactManager() {
        super(TAG);
        DEFAULT_AVAILABLE_STATUS = TAKChatUtils.getPluginString(R.string.app_name) + " available";
        DEFAULT_AWAY_STATUS = TAKChatUtils.getPluginString(R.string.app_name) + " in background";
        DEFAULT_OFFLINE_STATUS = TAKChatUtils.getPluginString(R.string.app_name) + " offline";

        myStatus = DEFAULT_OFFLINE_STATUS;
    }

    /**
     * Get an immutable list of contacts
     *
     * @param bIncludeConferences
     * @return
     */
    public List<XmppContact> getContacts(boolean bIncludeConferences) {
        synchronized (contacts) {
            if (bIncludeConferences)
                return Collections.unmodifiableList(contacts);

            List<XmppContact> ret = new ArrayList<XmppContact>();
            for (XmppContact c : contacts) {
                if (!TAKChatUtils.isConference(c))
                    ret.add(c);
            }

            return Collections.unmodifiableList(ret);
        }
    }

    public XmppContact getContactById(BareJid id) {
        if(id == null){
            Log.w(TAG, "Cannot get contact with empty id");
            return null;
        }

        synchronized (contacts) {
            for (XmppContact contact : contacts) {
                if (contact.getId() != null && contact.getId().equals(id))
                    return contact;
            }
        }

        if(TAKChatUtils.isMe(id)){
            VCardManager vm = TAKChatUtils.takChatComponent.getManager(VCardManager.class);
            if(vm == null)
                return null;
            return vm.getMyContact();
        }

        Log.w(TAG, "ID not found: " + id);
        return null;
    }

    public ArrayList<XmppConference> getConferences() {
        ArrayList<XmppConference> conferences = new ArrayList<XmppConference>();
        synchronized (contacts) {
            for (XmppContact contact : contacts) {  //TODO The better way would be manage this in a separate list!
                if (TAKChatUtils.isConference(contact))
                    conferences.add((XmppConference) contact);
            }
        }
        return conferences;
    }

    public XmppContact getContactByTAKUId(String uid) {
        synchronized (contacts) {
            for (XmppContact contact : contacts) {
                if (FileSystemUtils.isEquals(contact.getTakUserUID(), uid))
                    return contact;
            }
            return null;
        }
    }

    public List<BareJid> getUniqueIds() {
        List<BareJid> uniqueIds = new ArrayList<BareJid>();
        synchronized (contacts) {
            for (XmppContact contact : contacts) {
                uniqueIds.add(contact.getId());
            }
        }
        return uniqueIds;
    }

    private void disposeAllContacts() {
        synchronized (contacts) {
            contacts.clear();
        }
    }

    @Override
    public void entriesAdded(Collection<Jid> added) {
        Log.d(TAG, "entries added");
        for(Jid jid : added) {
            addContact(new XmppContact(jid.asBareJid()), false);
        }

        onContactSizeChanged();
    }

    /**
     * Add contact
     *
     * @param contact
     * @param bReplace  true: if already exists, skip
     */
    private void addContact(XmppContact contact, boolean bReplace) {
        synchronized (contacts) {
            int index = contacts.indexOf(contact);
            if (index != -1) {
                if(!bReplace){
                    Log.d(TAG, "Skipping existing contact: " + contact.toVerboseString());
                    return;
                }

                Log.d(TAG, "Updating contact: " + contact.toVerboseString());
                contacts.set(index, contact);
            } else {
                Log.d(TAG, "Adding contact: " + contact.toVerboseString());
                contacts.add(contact);
            }
        }

        if(!TAKChatUtils.isConference(contact)){
            //persist locally in DB
            ChatDatabase.getInstance(TAKChatUtils.pluginContext).addContact(contact);

            //add online contacts to base ATAK contact list
            contact.addContactListRef();
        }
    }

    private boolean removeContact(XmppContact contact) {
        Log.d(TAG, "Removing contact: " + contact.toVerboseString());

        if(!TAKChatUtils.isConference(contact)){
            //remove from local persistence
            ChatDatabase.getInstance(TAKChatUtils.pluginContext).removeContact(contact);
        }

        //remove whether individual contact or conference
        TAKChatView.remove(contact);
        contact.removeContactListRef();

        synchronized (contacts) {
            if (!contacts.contains(contact))
                return false;
            return contacts.remove(contact);
        }
    }

    public void reset() {
        Log.d(TAG, "reset");
        synchronized (contacts) {
            contacts.clear();
        }

        onContactSizeChanged();
    }

    @Override
    public void entriesUpdated(Collection<Jid> updated) {
        Log.d(TAG, "entries updated");
        for(Jid jid : updated){
            Log.d(TAG, "entry updated: " + jid);
            final Roster roster = Roster.getInstanceFor(TAKChatXMPP.getInstance().getConnection());
            RosterEntry entry = roster.getEntry(jid.asBareJid());
            if(entry == null){
                Log.w(TAG, "cannot update entry: " + jid);
                continue;
            }

            String name = entry.getName();
            Log.d(TAG, "Loading: " + entry.getJid().asBareJid());
            if(FileSystemUtils.isEmpty(name))
                name = entry.getJid().getLocalpartOrNull().toString();

            XmppContact contact = getContactById(jid.asBareJid());
            if(contact != null){
                Log.d(TAG, "Updating contact: " + contact.toVerboseString());
                contact.setName(name);
            }else {
                addContact(new XmppContact(name, entry.getJid().asBareJid()), true);
            }
        }

        onContactSizeChanged();

    }

    @Override
    public void entriesDeleted(Collection<Jid> deleted) {
        Log.d(TAG, "entries deleted");
        boolean bDeleted = false;
        for(Jid jid : deleted){
            Log.d(TAG, "entry deleted: " + jid);

            XmppContact contact = getContactById(jid.asBareJid());
            if(contact != null){
                bDeleted = true;
                Log.d(TAG, "Removing contact: " + contact.toVerboseString());
                removeContact(contact);
            }
        }

        if(bDeleted)
            onContactSizeChanged();
    }

    @Override
    public void presenceChanged(Presence presence) {
        if(isShutdown()) {
            Log.d(TAG, "Shutdown, ignoring presence: " + presence);
            return;
        }

        // TODO Smack workaround
        // See Bug 6969
        // Smack is locally sending us artificial "unavailable" presence for all contacts
        // when we call xmpp.disconnect, which we currently do during a disconnect or network hiccup
        if(!TAKChatXMPP.getInstance().isConnected() && presence.getTo() == null){
            //TODO also check for empty list of extensions?
            Log.w(TAG, "Not connected, ignoring presence: " + presence.toXML());
            return;
        }

        Log.d(TAG, "presenceChanged: " + presence.toXML());
        BareJid id = presence.getFrom().asBareJid();

        XmppContact contact = null;
        synchronized (contacts) {
            contact = getContactById(id);
        }

        //outside of sync block, notify listeners
        if(contact != null) {
            contact.updatePresence(presence);
            onPresenceChanged(presence);
        }else{
            Log.d(TAG, "Ignoring presence from: " + id.toString());
            return;
        }
    }

    @Override
    public void onRosterLoaded(Roster roster) {
        Log.d(TAG, "onRosterLoaded");

        //TODO is this necessary at each startup? Currently doing so b/c only avatar updates
        //are published...
        BareJid me = TAKChatUtils.getUsernameBare();
        if(me != null) {
            TAKChatUtils.takChatComponent.getManager(VCardManager.class).load(me.asEntityBareJidIfPossible());
        }

        Collection<RosterEntry> entries = roster.getEntries();
        for (RosterEntry entry : entries) {
            if(entry == null || entry.getJid() == null){
                Log.w(TAG, "Skipping invalid Roster entry: " + entry.toString());
                continue;
            }

            //on first connect, presence is not available by this point, so we just pass
            //up base roster usernames. Presence will trickle in shortly
            Log.d(TAG, "Adding roster entry: " + entry.toString());
            String name = entry.getName();
            if(FileSystemUtils.isEmpty(name) && entry.getJid().getLocalpartOrNull() != null)
                name = entry.getJid().getLocalpartOrNull().toString();
            addContact(new XmppContact(name, entry.getJid().asBareJid()), false);
        }
    }

    @Override
    public void onRosterLoadingFailed(Exception e) {
        Log.e(TAG, "Roster failed to load!");
    }

    /**
     * Set the TAK UID for the specified XMPP contact JID
     * If mapped successfully, and mutation occurred, and the TAK UID is already in the main
     * ATAK contact list, then remove the XMPP user from the ATAK contact list
     *
     * @param from
     * @param takUserUid
     */
    public void setUID(Jid from, String takUserUid) {
        if(from == null || from.getLocalpartOrNull() == null || FileSystemUtils.isEmpty(takUserUid))
            return;

        boolean bMapped = false;
        XmppContact contact = null;
        synchronized (contacts) {
            contact = getContactById(from.asBareJid());
            if(contact != null && !TAKChatUtils.isConference(contact)) {
                Log.d(TAG, "Mapping " + contact.toVerboseString() + " to " + takUserUid + ", using: " + from.asBareJid().toString());
                bMapped = contact.setTakUserUID(takUserUid);
            }
        }

        //outside of sync block, notify listeners, but only if we updated the mapping
        if(bMapped){
            onContactSizeChanged();

            //if the contact is already in the contact list as a TAK user, then remove the XMPP
            // contact from the contact list
            if(contact != null && Contacts.getInstance().getContactByUuid(takUserUid) != null){
                //if we know this to be a TAK user, we do not need duplicate UID in contact list
                Log.d(TAG, "Removing mapped contact list ref " + contact.toVerboseString());
                contact.removeContactListRef();
            }else{
                Log.d(TAG, "Not removing mapped contact list ref " + contact.toVerboseString());
            }
        }else {
            Log.d(TAG, "Unable to map (or mapping not changed): " + from.toString() + " to " + takUserUid);
        }
    }

    /**
     * Remove XMPP user from ATAK Contact list if necessary
     *
     * @param item
     */
    public void itemAdded(MapItem item) {
        //see if we have a contact for this TAK user/marker
        XmppContact contact = getContactByTAKUId(item.getUID());
        if(contact != null){
            Log.d(TAG, "Found XMPP contact for new marker: " + item.getUID() + ", contact: " + contact.getId());

            //If XMPP contact is in contact list, remove it
            contact.removeContactListRef();
        }
    }

    /**
     * Add XMPP user to ATAK Contact list if necessary
     *
     * @param item
     */
    public void itemRemoved(MapItem item) {
        //see if we have a contact for this TAK user/marker
        XmppContact contact = getContactByTAKUId(item.getUID());
        if(contact != null){
            Log.d(TAG, "Found XMPP contact for removed marker: " + item.getUID() + ", contact: " + contact.getId());
            //If XMPP contact is not in contact list, add it
            contact.addContactListRef();
        }
    }

    private boolean onPresenceChanged(Presence presence) {
        //Notify listeners to refresh all
        synchronized (_listeners) {
            for (ContactListener l : _listeners) {
                l.onPresenceChanged(presence);
            }
        }

        return true;
    }

    private boolean onContactSizeChanged() {
        //Notify listeners to refresh all
        synchronized (_listeners) {
            for (ContactListener l : _listeners) {
                l.onContactSizeChanged();
            }
        }

        return true;
    }

    @Override
    public boolean onConnected() {
        Log.d(TAG, "onConnected");
        return onContactSizeChanged();
    }

    @Override
    public boolean onStatus(boolean bSuccess, String status) {
        return true;
    }

    @Override
    public boolean onDisconnected() {
        Log.d(TAG, "onDisconnected");
        return onContactSizeChanged();
    }

    public void dispose(){
        if(_isShutdown)
            return;

        super.dispose();
        disposeAllContacts();
    }

    public boolean sendAway() {
        return sendPresence(TAKChatUtils.createSelfPresence(true, true, DEFAULT_AWAY_STATUS));
    }

    public boolean sendAvailable() {
        //TODO synchronized on myStatus?
        return sendPresence(TAKChatUtils.createSelfPresence(true, false, myStatus));
    }

    public boolean sendOffline() {
        return sendPresence(TAKChatUtils.createSelfPresence(false, true, DEFAULT_OFFLINE_STATUS));
    }

    public synchronized boolean setMyStatus(String status) {
        if(FileSystemUtils.isEquals(myStatus, status))
            return sendAvailable();

        myStatus = status;
        return sendAvailable();
    }

    public synchronized String getMyStatus() {
        if(TAKChatXMPP.getInstance().isConnected())
            return myStatus;
        else
            return TAKChatUtils.getPluginString(R.string.disconnected);
    }

    private boolean sendPresence(Presence presence) {
        if(presence == null){
            Log.w(TAG, "Chat presence invalid");
            return false;
        }

        //TODO return error back to UI/_listeners?
        XMPPConnection connection = TAKChatXMPP.getInstance().getConnection();
        if(!TAKChatXMPP.getInstance().isConnected()) {
            Log.w(TAG, "sendPresence: Chat service not onConnected: " + (presence == null ? "" : presence.toString()));
            return false;
        }

        try {
            Log.d(TAG, "Sending: " + presence.toString());
            connection.sendStanza(presence);
            return true;
        } catch (SmackException.NotConnectedException e) {
            Log.e(TAG, "sendPresence NotConnectedException", e);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "sendPresence Exception", e);
            return false;
        }
    }

    @Override
    public void onLoaded() {
        loadContacts();
    }

    private void loadContacts() {
        Log.d(TAG, "loadContacts");

        //Load saved contacts, not available until connected
        List<XmppContact> contacts = ChatDatabase.getInstance(TAKChatUtils.pluginContext).getContacts();
        BareJid me = TAKChatUtils.getUsernameBare();
        for (XmppContact contact : contacts) {
            if(me != null && FileSystemUtils.isEquals(contact.getId().toString(), me.toString())){
                Log.d(TAG, "Skipping self contact");
                continue;
            }

            contact.setAvailable(false);
            addContact(contact, false);
        }

        onContactSizeChanged();
    }

    @Override
    public void init(AbstractXMPPConnection connection) {
        final Roster roster = Roster.getInstanceFor(connection);
        roster.setRosterLoadedAtLogin(true);
        //for now we allow everyone to subscribe. May need a mode where
        //local user has to approve subscription requests
        roster.setSubscriptionMode(Roster.SubscriptionMode.accept_all);

        roster.addRosterListener(this);
        roster.addRosterLoadedListener(this);

        //when someone subscribes to us, we do the same
        //TODO may want to make a setting or UI to control subscriptions
        //See above Roster.SubscriptionMode.accept_all
        connection.addAsyncStanzaListener(new StanzaListener() {
            public void processStanza(Stanza stanza) throws SmackException.NotConnectedException, InterruptedException {
                Presence presence = (Presence)stanza;
                //TODO verify this was "to" me? Jid to = presence.getTo();
                Jid from = presence.getFrom();
                if(from == null) {
                    Log.d(TAG, "Skipping subscribe without from: " + stanza.toXML());
                    return;
                }

                try {
                    Log.d(TAG, "Mutual subscribing to: " + from.toString());
                    roster.sendSubscriptionRequest(from.asBareJid());

                    Intent intent = new Intent(TAKChatDropDownReceiver.SHOW_CHAT);
                    intent.putExtra("bareJid", from.asBareJid().toString());


                    String message = from.asBareJid().toString() + " and you are now connected.";
                    ConnectivityNotificationProxy.showNotification(
                            TAKChatUtils.getPluginString(R.string.app_name) + " Buddy Added",
                            message, R.drawable.xmpp_icon, intent);
                } catch (SmackException.NotLoggedInException e) {
                    Log.w(TAG, "Error during mutual subscription", e);
                }
            }
        }, PresenceTypeFilter.SUBSCRIBE);
    }

    public void reload(XMPPTCPConnection connection) {
        Roster roster = Roster.getInstanceFor(connection);
//        if (roster.isLoaded()) {
//            //TODO I know its loaded... I want to _re_load...
//            Log.d(TAG, "Roster already loaded");
//            return;
//        }

        Log.d(TAG, "Reloading roster");
        try {
            roster.reload();
        } catch (SmackException.NotLoggedInException e) {
            Log.e(TAG, "reload NotLoggedInException", e);
        } catch (SmackException.NotConnectedException e) {
            Log.e(TAG, "reload NotConnectedException", e);
        } catch (InterruptedException e) {
            Log.e(TAG, "reload InterruptedException", e);
        }
    }

    /**
     * Send server request to add buddy
     *
     * @param jid
     * @param name
     * @return true if server request was sent
     */
    public static boolean addBuddy(final EntityBareJid jid, final String name) {
        //TODO notify user upon error?
        if(!TAKChatXMPP.getInstance().isConnected()){
            Log.w(TAG, "cannot addBuddy without connection");
            return false;
        }

        TAKChatUtils.runInBackground(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d(TAG, "Adding buddy: " + jid);
                    Roster roster = Roster.getInstanceFor(TAKChatXMPP.getInstance().getConnection());
                    roster.createEntry(jid, name, null);
                } catch (SmackException.NotLoggedInException e) {
                    Log.e(TAG, "Failed to add buddy: " + jid.toString(), e);
                } catch (SmackException.NoResponseException e) {
                    Log.e(TAG, "Failed to add buddy: " + jid.toString(), e);
                } catch (XMPPException.XMPPErrorException e) {
                    Log.e(TAG, "Failed to add buddy: " + jid.toString(), e);
                } catch (SmackException.NotConnectedException e) {
                    Log.e(TAG, "Failed to add buddy: " + jid.toString(), e);
                } catch (InterruptedException e) {
                    Log.e(TAG, "Failed to add buddy: " + jid.toString(), e);
                }
            }
        });

        Toast.makeText(TAKChatUtils.mapView.getContext(), "Adding buddy: " + name, Toast.LENGTH_SHORT).show();
        return true;
    }


    public static boolean requestAuthorization(XmppContact contact) {
        boolean result = false;
        if(!TAKChatXMPP.getInstance().isConnected()){
            Log.w(TAG, "cannot requestAuthorization without connection");
            return result;
        }

        try {
            Roster.getInstanceFor(TAKChatXMPP.getInstance().getConnection()).sendSubscriptionRequest(contact.getId());
        } catch (SmackException.NotLoggedInException e) {
            Log.e(TAG, "Failed to send authorization request for: " + contact.getName(), e);
        } catch (SmackException.NotConnectedException e) {
            Log.e(TAG, "Failed to send authorization request for: " + contact.getName(), e);
        } catch (InterruptedException e) {
            Log.e(TAG, "Failed to send authorization request for: " + contact.getName(), e);
        }
        return result;
    }

    /**
     * Remove from server roster.
     * See entriesDeleted for server response, when we actually remove local data
     *
     * @param contact
     * @return  true if server request was sent
     */
    public boolean removeBuddy(final XmppContact contact) {
        if(!TAKChatXMPP.getInstance().isConnected()){
            Log.w(TAG, "cannot removeBuddy without connection");
            return false;
        }

        final Roster roster = Roster.getInstanceFor(TAKChatXMPP.getInstance().getConnection());
        final RosterEntry entry = roster.getEntry(contact.getId());
        if(entry == null){
            Log.w(TAG, "Unable to remove buddy: " + contact.getId());
            return false;
        }else {
            TAKChatUtils.runInBackground(new Runnable() {
                @Override
                public void run() {
                    try {
                        Log.d(TAG, "Removing buddy: " + contact.getId());
                        roster.removeEntry(entry);
                    } catch (SmackException.NotLoggedInException e) {
                        Log.e(TAG, "Failed to remove buddy: " + contact.getId().toString(), e);
                    } catch (SmackException.NoResponseException e) {
                        Log.e(TAG, "Failed to remove buddy: " + contact.getId().toString(), e);
                    } catch (XMPPException.XMPPErrorException e) {
                        Log.e(TAG, "Failed to remove buddy: " + contact.getId().toString(), e);
                    } catch (SmackException.NotConnectedException e) {
                        Log.e(TAG, "Failed to remove buddy: " + contact.getId().toString(), e);
                    } catch (InterruptedException e) {
                        Log.e(TAG, "Failed to remove buddy: " + contact.getId().toString(), e);
                    }
                }
            });

            Toast.makeText(TAKChatUtils.mapView.getContext(), "Removing buddy: " + contact.getName(), Toast.LENGTH_SHORT).show();
            return true;
        }
    }

    public void addGroup(XmppConference conf) {
        this.addContact(conf, true);
        conf.addContactListRef();
        onContactSizeChanged();
    }

    public void removeGroup(XmppConference conf) {
        this.removeContact(conf);
        onContactSizeChanged();
    }


    /**
     * Set the local Name(aka Alias, or Handle) for this buddy. Store in my roster
     *
     * @param jid
     * @param name
     * @return
     */
    public static boolean setName(BareJid jid, String name) {
        if(!TAKChatXMPP.getInstance().isConnected()){
            Log.w(TAG, "cannot setName without connection");
            return false;
        }

        //TODO empty name OK per spec?
        //TODO background thread?
        try {
            Roster roster = Roster.getInstanceFor(TAKChatXMPP.getInstance().getConnection());
            RosterEntry entry = roster.getEntry(jid);
            if(entry == null){
                Log.w(TAG, "Unable to set handle for buddy: " + jid);
                return false;
            }else {
                Log.d(TAG, "Setting handle for buddy: " + jid);
                entry.setName(name);
                return true;
            }
        } catch (SmackException.NoResponseException e) {
            Log.e(TAG, "Failed to set buddy name: " + jid.toString(), e);
        } catch (XMPPException.XMPPErrorException e) {
            Log.e(TAG, "Failed to set buddy name: " + jid.toString(), e);
        } catch (SmackException.NotConnectedException e) {
            Log.e(TAG, "Failed to set buddy name: " + jid.toString(), e);
        } catch (InterruptedException e) {
            Log.e(TAG, "Failed to set buddy name: " + jid.toString(), e);
        }

        return false;
    }
}
