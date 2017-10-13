package com.atakmap.android.takchat.data;

import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.takchat.TAKChatUtils;
import com.atakmap.android.takchat.contact.XmppIndividualContact;
import com.atakmap.android.takchat.net.TAKChatXMPP;
import com.atakmap.android.takchat.notification.SoundManager;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;

import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smackx.vcardtemp.packet.VCard;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Localpart;
import org.jxmpp.stringprep.XmppStringprepException;

/**
 * Represents a "buddy" from the XMPP roster managed by the XMPP server
 *
 * Created by scallya on 8/3/2016.
 */
public class XmppContact {
    private static final String TAG = "XmppContact";

    protected String name;
    protected BareJid id;

    protected boolean available;
    protected boolean away;
    protected String status;

    protected ClientSoftware clientSoftware;

    /**
     * If this is a TAK user e.g. another ATAK, cache that user's CoT UID
     */
    protected String takUserUID;

    protected VCard vCard;

    /**
     * Keep a reference to the ATAK contact list contact we've created
     * Only added to contact list for non TAK users
     */
    protected XmppIndividualContact _contactListRef;

    public Presence getPresence() {
        return TAKChatUtils.createPresence(id, available, away, status);
    }

    public enum ClientSoftware {
        ATAK, WINTAK, DEFAULT
    }

    public XmppContact(String name, BareJid id) {
        this.name = name;
        this.id = id;
        available = false;
        away = false;
        status = null;
        clientSoftware = ClientSoftware.DEFAULT;
        _contactListRef = new XmppIndividualContact(this);
    }

    public XmppContact(String name, String id) throws XmppStringprepException {
        this(name, JidCreate.bareFrom(id));
    }

    public XmppContact(BareJid id) {
        Localpart local = id.getLocalpartOrNull();
        if(local != null)
            this.name = local.toString();
        this.id = id;
        available = false;
        away = false;
        status = null;
        clientSoftware = ClientSoftware.DEFAULT;
        _contactListRef = new XmppIndividualContact(this);
    }

    public void updatePresence(Presence presence) {
        //Log.d(TAG, "Updating: " + toString() + " type=" + presence.getType() + ", mode=" + presence.getMode() + ", status=" + presence.getStatus());

        boolean previouslyAvailable = this.available;

        this.available = presence.isAvailable();
        this.away = presence.isAway();
        this.status = presence.getStatus();

        _contactListRef.setUpdateStatus();

        if (previouslyAvailable && !this.available) {
            SoundManager.getInstance().play(SoundManager.SOUND.LEAVE);
        } else if (!previouslyAvailable && this.available) {
            SoundManager.getInstance().play(SoundManager.SOUND.COMING);
        }
    }

    public void setAvailable(boolean available) {
        //Log.d(TAG, "setAvailable: " + available);
        this.available = available;
    }

    public boolean isAvailable() {
        return TAKChatXMPP.getInstance().isConnected() && available;
    }

    public boolean isAway() {
        return away;
    }

    public void setAway(boolean away) {
        this.away = away;
    }

    public ClientSoftware getClientSoftware() {
        return clientSoftware;
    }
    public void setClientSoftware(ClientSoftware clientSoftware) {
        this.clientSoftware = clientSoftware;
    }

    public String getStatus(){
        return status;
    }

    public boolean hasStatus() {
        return !FileSystemUtils.isEmpty(status);
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean hasTakUserUID(){
        return !FileSystemUtils.isEmpty(this.takUserUID);
    }


    public String getTakUserUID() {
        return takUserUID;
    }

    /**
     * Set TAK UID
     *
     * @param takUserUID
     * @return  True if UID was updated
     */
    public boolean setTakUserUID(String takUserUID) {
        if(FileSystemUtils.isEquals(this.takUserUID, takUserUID))
            return false;

        this.takUserUID = takUserUID;
        return true;
    }

    public Marker getMarker(){
        if(hasTakUserUID()){
            MapItem mapItem = TAKChatUtils.mapView.getRootGroup().deepFindUID(this.takUserUID);
            if(mapItem != null && mapItem instanceof Marker){
                return (Marker)mapItem;
            }
        }

        return null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        //TODO Not this simple!  I think we need to tell the server or something!
    }

    public BareJid getId() {
        return id;
    }

    public VCard getVCard() {
        return vCard;
    }

    public void setVCard(VCard vCard) {
        this.vCard = vCard;
    }


    public void addContactListRef() {
        Log.d(TAG, "Adding contact list ref: " + this.toString());
        //add to contact list and set status
        _contactListRef.setUpdateStatus();
    }

    public void removeContactListRef() {
        Log.d(TAG, "Removing contact list ref: " + this.toString());
        _contactListRef.removeListRef();
    }

    @Override
    public boolean equals(Object contact) {
        if(XmppContact.class.isInstance(contact)) {
            return ((XmppContact)contact).getId().asBareJid().toString().equals(this.getId().asBareJid().toString());
        }
        return false;
    }

    public String toVerboseString() {
        return String.format("%s, %b %b, %s", id.toString(), available, away, status);
    }

    @Override
    public String toString() {
        return id.toString();
    }
}
