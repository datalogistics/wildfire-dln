package com.atakmap.android.takchat.net;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.atakmap.android.filesystem.ResourceFile;
import com.atakmap.android.gui.HintDialogHelper;
import com.atakmap.android.image.ExifHelper;
import com.atakmap.android.image.ImageGalleryBaseAdapter;
import com.atakmap.android.takchat.TAKChatUtils;
import com.atakmap.android.takchat.data.ChatDatabase;
import com.atakmap.android.takchat.data.VCardListener;
import com.atakmap.android.takchat.data.VCardUpdateExtension;
import com.atakmap.android.takchat.data.XmppContact;
import com.atakmap.android.takchat.plugin.R;
import com.atakmap.android.takchat.view.TAKContactProfileView;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.time.CoordinatedTime;

import org.apache.sanselan.formats.tiff.TiffImageMetadata;
import org.apache.sanselan.formats.tiff.constants.TiffConstants;
import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.StanzaExtensionFilter;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.filter.StanzaTypeFilter;
import org.jivesoftware.smack.packet.DefaultExtensionElement;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.StandardExtensionElement;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smackx.muc.packet.MUCUser;
import org.jivesoftware.smackx.vcardtemp.packet.VCard;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

/**
 * Manages <code>{@link VCard}</code> for self and contacts
 *
 * Created by byoung on 9/23/2016.
 */
public class VCardManager extends IManager<VCardListener> {

    private static final String TAG = "VCardManager";
    private final static int VCARD_PACKET_REPLY_TIMEOUT = 120000;

    public enum UpdateMode{ None, IfNecessary, Force };

    //TODO put this in prefs in case someone's XMPP server needs smaller? Or can handle larger..
    private static final int MAX_AVATAR_WIDTH = 256;
    private final SharedPreferences _prefs;

    private VCard myVCard;

    public VCardManager(SharedPreferences prefs) {
        super(TAG);
        _prefs = prefs;
    }

    @Override
    public synchronized void dispose() {
        super.dispose();
    }

    public synchronized VCard getMyVCard() {
        return myVCard;
    }

    private synchronized void setMyVCard(final VCard vCard) {
        this.myVCard = vCard;
        Log.d(TAG, "Self vcard set");

        //My VCard was loaded, give user opportunity to update profile
        TAKChatUtils.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                HintDialogHelper
                        .showHint(
                                TAKChatUtils.mapView.getContext(),
                                "Update Chat Profile",
                                "Your user profile may be updated at any time via the "
                                        + TAKChatUtils.getPluginString(R.string.app_name)
                                        + " action bar.",
                                "takchat.profileOffer",
                                new HintDialogHelper.HintActions() {
                                    public void preHint() { }
                                    public void postHint() {
                                        TAKContactProfileView.getInstance().showContactInfo(
                                                getMyContact(), vCard, UpdateMode.None);
                                    }
                                });
            }
        });
    }

    /**
     * Get an XmppContact for the local user
     * @return
     */
    public XmppContact getMyContact(){
        if(myVCard == null){
            Log.w(TAG, "Local contact card not set");
            return null;
        }

        BareJid me = TAKChatUtils.getUsernameBare();
        if(me == null){
            Log.w(TAG, "Local contact jid not set");
            return null;
        }

        boolean bIsConnected = TAKChatXMPP.getInstance().isConnected();

        XmppContact contact = new XmppContact(me);
        contact.setTakUserUID(TAKChatUtils.mapView.getDeviceUid());
        contact.setClientSoftware(XmppContact.ClientSoftware.ATAK);
        contact.setVCard(myVCard);

        if(bIsConnected) {
            contact.setStatus(TAKChatUtils.takChatComponent.getManager(ContactManager.class).getMyStatus());
            contact.setAway(false);
            contact.setAvailable(true);
        }else{
            contact.setStatus(TAKChatUtils.getPluginString(R.string.disconnected));
            contact.setAway(true);
            contact.setAvailable(false);
        }

        return contact;
    }

    /**
     * If VCard is stored locally, get VCard from DB.
     * Optionally request from server asynchronously. See VCardListener
     *
     * @param jid
     * @return
     */
    public VCard getVCard(final EntityBareJid jid, UpdateMode mode){

        //pull from DB
        VCard cache = ChatDatabase.getInstance(TAKChatUtils.pluginContext).getVCard(jid);

        //determine if we should pull update from server
        switch(mode){
            case None:
                return cache;
            case IfNecessary:
                if(cache == null){
                    load(jid);
                }
                return cache;
            default:
            case Force:
                load(jid);
                return cache;
        }
    }

    /**
     * Load specified VCard
     * Listeners will be notified when complete
     */
    public void load(final EntityBareJid jid) {
        if(jid == null){
            Log.d(TAG, "Cannot load without jid");
            return;
        }

        if(VCardManager.isConference(_prefs, jid)){
            Log.d(TAG, "Skipping vcard for conference: " + jid.toString());
            return;
        }

        Log.d(TAG, "Loading vcard for: " + jid.toString());

        TAKChatUtils.runInBackground(new Runnable() {
            @Override
            public void run() {

                if(TAKChatXMPP.getInstance().isConnected()) {
                    VCard vCard = null;

                    try {
                        XMPPConnection connection = TAKChatXMPP.getInstance().getConnection();
                        final org.jivesoftware.smackx.vcardtemp.VCardManager vCardManager =
                                org.jivesoftware.smackx.vcardtemp.VCardManager.getInstanceFor(connection);

                        //bump up reply timeout since avatar may be large compared to other stanzas
                        connection.setPacketReplyTimeout(VCARD_PACKET_REPLY_TIMEOUT);

                        long start = new CoordinatedTime().getMilliseconds();
                        vCard = vCardManager.getInstanceFor(connection).loadVCard(jid);
                        long stop = new CoordinatedTime().getMilliseconds();
                        Log.d(TAG, "Received VCard in seconds: " + (stop-start)/1000D);
                        connection.setPacketReplyTimeout(TAKChatXMPP.PACKET_REPLY_TIMEOUT);

                    } catch (SmackException.NoResponseException e) {
                        Log.w(TAG, "Failed to load VCard", e);
                    } catch (SmackException.NotConnectedException e ) {
                        Log.w(TAG, "Failed to load VCard", e);
                    } catch (XMPPException.XMPPErrorException e ) {
                        Log.w(TAG, "Failed to load VCard", e);

                        if (e.getXMPPError().getCondition() == XMPPError.Condition.item_not_found) {
                            vCard = new VCard();
                        }

                    } catch (ClassCastException e) {
                        // http://stackoverflow.com/questions/31498721/error-loading-vcard-information-using-smack-emptyresultiq-cannot-be-cast-to-or
                        Log.w(TAG, "ClassCastException", e);
                        vCard = new VCard();
                    } catch (InterruptedException e) {
                        Log.w(TAG, "Failed to load VCard", e);

                    }

                    if (vCard == null) {
                        Log.w(TAG, "Failed to load VCard for: " + jid.toString());
                    }else if(vCard.getType() != IQ.Type.result) {
                        Log.w(TAG, "Skipping VCard response for: " + jid.toString() + ", " + vCard.toXML());
                    }else{
                        onVCardUpdate(vCard, jid);
                    }
                }else{
                    Log.w(TAG, "Not connected, skipping VCard request: " + jid.toString());
                }
            }
        });
    }

    private void onVCardUpdate(VCard vCard, EntityBareJid jid) {
        Log.d(TAG, "Successfully loaded VCard for: " + jid.toString());
        //now notify listeners
        synchronized (VCardManager.this._listeners){
            for(VCardListener l : _listeners){
                l.onVCardUpdate(vCard);
            }
        }

        if(TAKChatUtils.isMe(jid)) {
            setMyVCard(vCard);
        }
    }

    public void save(final VCard vCard) {
        Log.d(TAG, "Saving vcard");

        TAKChatUtils.runInBackground(new Runnable() {
            @Override
            public void run() {

                if(TAKChatXMPP.getInstance().isConnected()) {
                    try {
                        XMPPConnection connection = TAKChatXMPP.getInstance().getConnection();
                        final org.jivesoftware.smackx.vcardtemp.VCardManager vCardManager =
                                org.jivesoftware.smackx.vcardtemp.VCardManager.getInstanceFor(connection);

                        //bump up reply timeout since avatar may be large compared to other stanzas
                        connection.setPacketReplyTimeout(VCARD_PACKET_REPLY_TIMEOUT);

                        long start = new CoordinatedTime().getMilliseconds();
                        vCardManager.getInstanceFor(connection).saveVCard(vCard);
                        long stop = new CoordinatedTime().getMilliseconds();
                        Log.d(TAG, "Saved VCard in seconds: " + (stop-start)/1000D);
                        connection.setPacketReplyTimeout(TAKChatXMPP.PACKET_REPLY_TIMEOUT);

                        onVCardSaved(vCard, vCard.getAvatarHash());

                    } catch (SmackException.NoResponseException e) {
                        Log.w(TAG, "Failed to save VCard", e);
                        onVCardSaveFailed(vCard);
                    } catch (XMPPException.XMPPErrorException e) {
                        Log.w(TAG, "Failed to save VCard", e);
                        onVCardSaveFailed(vCard);
                    } catch (InterruptedException e) {
                        Log.w(TAG, "Failed to save VCard", e);
                        onVCardSaveFailed(vCard);
                    } catch (SmackException.NotConnectedException e) {
                        Log.w(TAG, "Failed to save VCard", e);
                        onVCardSaveFailed(vCard);
                    }

                    //TODO notify any listeners?
                }else{
                    Log.w(TAG, "Not connected, skipping VCard save");
                }
            }
        });
    }

    private void onVCardSaveFailed(VCard vCard) {
        synchronized (VCardManager.this._listeners){
            for(VCardListener l : _listeners){
                l.onVCardSaveFailed(vCard);
            }
        }
    }

    private void onVCardSaved(VCard vCard, String avatarHash) {
        if (avatarHash == null) {
            avatarHash = "";
        }

        //now send updated presence
        sendVCardUpdate(avatarHash);

        //if we did not change avatar, then request vcard to sync up with server
        //we only get notifications of avatar changes
        BareJid me = TAKChatUtils.getUsernameBare();
        String existingHash = ChatDatabase.getInstance(TAKChatUtils.pluginContext).getVCardHash(me);
        if(me != null && FileSystemUtils.isEquals(existingHash, avatarHash)){
            Log.d(TAG, "Resync-ing self vcard");
            load(me.asEntityBareJidIfPossible());
        }else{
            //Note, do not store in DB here, wait for server to send us the vcard-temp update
        }

        synchronized (VCardManager.this._listeners){
            for(VCardListener l : _listeners){
                l.onVCardSaved(vCard);
            }
        }
    }

    public void sendVCardUpdate(String avatarHash) {
        Log.w(TAG, "sendVCardUpdate: " + avatarHash);

        Presence presence = TAKChatUtils.createSelfPresence(true, false,
                TAKChatUtils.takChatComponent.getManager(ContactManager.class).getMyStatus()
        );

        final VCardUpdateExtension vCardUpdate = new VCardUpdateExtension();
        vCardUpdate.setPhotoHash(avatarHash);
        presence.addExtension(vCardUpdate);

        XMPPConnection connection = TAKChatXMPP.getInstance().getConnection();
        if(!TAKChatXMPP.getInstance().isConnected()) {
            Log.w(TAG, "sendVCardUpdate: Chat service not onConnected: " + (presence == null ? "" : presence.toString()));
            return;
        }

        try {
            Log.d(TAG, "Sending: " + presence.toString());
            connection.sendStanza(presence);
        } catch (SmackException.NotConnectedException e) {
            Log.e(TAG, "sendVCardUpdate NotConnectedException", e);
        } catch (Exception e) {
            Log.e(TAG, "sendVCardUpdate Exception", e);
        }
    }

    /**
     * UI Thread only
     * @return
     */
    public void clearMyAvatar(){
        if(myVCard == null){
            Log.w(TAG, "Local contact card not set, cannot clear");
            return;
        }

        Log.d(TAG, "Clearing avatar");

        AlertDialog.Builder builder = new AlertDialog.Builder(TAKChatUtils.mapView.getContext());
        builder.setTitle("Delete Profile Picture?")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        myVCard.removeAvatar();

                        //now send to server
                        save(myVCard);
                    }
                })
                .setNegativeButton("Cancel", null);
        builder.show();
    }

    /**
     * UI Thread only
     *
     * @param fullPath
     * @return
     */
    public boolean setMyAvatar(final String fullPath) {
        if(myVCard == null){
            Log.w(TAG, "Local contact card not set, cannot set photo");
            return false;
        }

        File src = null;
        try{
            src = new File(
                    FileSystemUtils.validityScan(fullPath));
        } catch (IOException ioe) {
            Log.w(TAG, "Cannot import avatar file: " + fullPath, ioe);
            return false;
        }

        if(!FileSystemUtils.isFile(src)){
            Log.w(TAG, "setMyAvatar no filepath: " + src.getAbsolutePath());
            return false;
        }

        Log.d(TAG, "setMyAvatar: " + src.getAbsolutePath());
        //TODO error checking. TODO maintain aspect ratio..
        //resize if necessary
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inPreferQualityOverSpeed = false;
        opts.inPreferredConfig = Bitmap.Config.RGB_565;
        Bitmap thumb = BitmapFactory.decodeFile(src.getAbsolutePath(), opts);

        //TODO more efficient to take thumb then rotate? Is EXIF data lost when take thumb?
        TiffImageMetadata exif = ExifHelper.getExifMetadata(src);
        thumb = ImageGalleryBaseAdapter.rotateBitmap(thumb,
                ExifHelper.getInt(exif,
                        TiffConstants.EXIF_TAG_ORIENTATION, 0));

        byte[] bytes = null;
        if (opts.outWidth > MAX_AVATAR_WIDTH || opts.outHeight > MAX_AVATAR_WIDTH) {
            Log.d(TAG, "resizing avatar: " + src.getAbsolutePath() + " from (" + opts.outHeight + "," + opts.outWidth + ") to: " + MAX_AVATAR_WIDTH);
            thumb = ThumbnailUtils.extractThumbnail(thumb, MAX_AVATAR_WIDTH, MAX_AVATAR_WIDTH);
            ByteArrayOutputStream bos = null;
            try {
                bos = new ByteArrayOutputStream();
                thumb.compress(Bitmap.CompressFormat.PNG, 100, bos);
                bytes = bos.toByteArray();
            } catch (IllegalStateException e) {
                Log.w(TAG, "unable to save:" + src.getAbsolutePath(), e);
                return false;
            } finally {
                try {
                    if (bos != null)
                        bos.close();
                } catch (IOException e) {
                }
            }
        }

        if(FileSystemUtils.isEmpty(bytes)){
            Log.w(TAG, "unable to serialize:" + src.getAbsolutePath());
            return false;
        }

        //confirm avatar with user
        final byte[] fbytes = bytes;
        ImageView imageView = new ImageView(TAKChatUtils.mapView.getContext());
        imageView.setImageBitmap(thumb);
        AlertDialog.Builder builder = new AlertDialog.Builder(TAKChatUtils.mapView.getContext());
        builder.setTitle("Set Profile Picture?")
                .setView(imageView)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //set from file URL as image/jpeg
                        myVCard.setAvatar(fbytes, ResourceFile.MIMEType.JPG.MIME);

                        //Note we do not delete local file. But server will respond with new avatar
                        //so it will be validated as current upon that server stanza coming in
                        Toast.makeText(TAKChatUtils.mapView.getContext(),
                                "Sending to server... Avatar will be available once confirmed by server.",
                                Toast.LENGTH_LONG).show();

                        //now send to server
                        save(myVCard);

                        Log.d(TAG, "avatar set of size: " + fbytes.length);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Log.d(TAG, "Cancelled avatar: " + fullPath);
                    }
                });

        AlertDialog dlg = builder.create();
        WindowManager.LayoutParams screenLP = new WindowManager.LayoutParams();
        screenLP.copyFrom(dlg.getWindow().getAttributes());
        screenLP.width = WindowManager.LayoutParams.MATCH_PARENT;
        screenLP.height = WindowManager.LayoutParams.MATCH_PARENT;
        dlg.getWindow().setAttributes(screenLP);
        dlg.show();

        return true;
    }

    @Override
    public void init(AbstractXMPPConnection connection) {
        Log.d(TAG, "init");
        AndFilter updateExt = new AndFilter(new StanzaFilter[]{StanzaTypeFilter.PRESENCE,
                new StanzaExtensionFilter(VCardUpdateExtension.ELEMENT_NAME, VCardUpdateExtension.NAMESPACE)});

        connection.addAsyncStanzaListener(new StanzaListener(){
                @Override
                public void processStanza(Stanza stanza) throws SmackException.NotConnectedException, InterruptedException {
                    if(stanza == null || stanza.getFrom() == null){
                        Log.d(TAG, "Skipping invalid presence");
                        return;
                    }

                    //Note we only want to process VCard for contacts in our roster, not conferences
                    //or users in conference
                    if(stanza.hasExtension(MUCUser.NAMESPACE)){
                        Log.d(TAG, "Skipping MUC user presence: " + stanza.getFrom().toString());
                        return;
                    }

                    if(VCardManager.isConference(_prefs, stanza.getFrom().asBareJid())){
                        Log.d(TAG, "Skipping presence for conference: " + stanza.getFrom().toString());
                        return;
                    }

                    checkAvatarUpdate(stanza);
                }
            }, updateExt);
    }

    @Override
    public void onLoaded() {
        //make locally stored self VCard available immediately
        VCard vcard = ChatDatabase.getInstance(TAKChatUtils.pluginContext).getVCard(TAKChatUtils.getUsernameBare());
        if(vcard == null){
            Log.d(TAG, "Self vcard not stored yet");
            return;
        }

        setMyVCard(vcard);
    }

    /**
     * Deep check if JID is for a conference
     *
     * @param jid
     * @return
     */
    public static boolean isConference(SharedPreferences prefs, BareJid jid) {
        if(jid == null) {
            Log.w(TAG, "Skipping presence for invalid jid");
            return false;
        }

        //See Bug 6939
        //but sometimes servers send us conference occupant presence without MUC extension
        //so lets add a few more checks here
        ContactManager cm = TAKChatUtils.takChatComponent.getManager(ContactManager.class);
        if(cm == null){
            Log.d(TAG, "Cannot load without contact manager");
            return false;
        }

        XmppContact contact = cm.getContactById(jid);
        if(TAKChatUtils.isConference(contact)){
            Log.d(TAG, "isConference: " + jid.toString());
            return true;
        }

        if(ChatDatabase.getInstance(TAKChatUtils.pluginContext).hasConference(jid)){
            Log.d(TAG, "Skipping vcard for DB conference: " + jid.toString());
            return true;
        }

        String lastDitchMatch = "@" + prefs.getString("takchatServerConfPrefix", "conference") + ".";
        if(jid.toString().contains(lastDitchMatch)){
            //log stack so we can track it down
            Log.w(TAG, "isConference (2): " + jid.toString(), new Exception("isConference JID match"));
            return true;
        }

        return false;
    }


    public void checkAvatarUpdate(Stanza stanza){
        Log.d(TAG, "checkAvatarUpdate: " + stanza.toXML());

        EntityBareJid jid = null;
        try {
            jid = JidCreate.entityBareFrom(stanza.getFrom());
        } catch (XmppStringprepException e) {
            Log.w(TAG, "checkAvatarUpdate", e);
            return;
        }

        if(jid == null){
            Log.w(TAG, "checkAvatarUpdate invalid JID");
            return;
        }

        //TODO check if user is our roster b/f downloading?
        String currentHash = ChatDatabase.getInstance(TAKChatUtils.pluginContext).getVCardHash(jid);

        //TODO migrate this to VCardUpdateExtension.Provider ?
        // Now get the update hash
        String newHash = null;
        ExtensionElement extElement = stanza.getExtension(
                VCardUpdateExtension.ELEMENT_NAME,
                VCardUpdateExtension.NAMESPACE);
        if(extElement instanceof DefaultExtensionElement){
            Log.d(TAG, "Found DefaultExtensionElement");
            DefaultExtensionElement dee = (DefaultExtensionElement) extElement;
            newHash = dee.getValue(VCardUpdateExtension.PHOTO_NAME);
        } else if(extElement instanceof StandardExtensionElement){
            Log.d(TAG, "Found StandardExtensionElement");
            StandardExtensionElement see = (StandardExtensionElement) extElement;
            StandardExtensionElement photosee = see.getFirstElement(VCardUpdateExtension.PHOTO_NAME);
            if(photosee != null)
                newHash = photosee.getText();
        }

        if(!FileSystemUtils.isEquals(newHash, currentHash)) {
            Log.d(TAG, "Avatar updated for: " + jid.toString() + " from: " + currentHash + ", to: " + newHash);
            //kick off VCard request to get update
            load(jid);
        }else{
            //TODO remove logging after testing
            Log.d(TAG, "Avatar not updated for: " + jid.toString() + ", hash: " + currentHash);
        }
    }
}
