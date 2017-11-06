package com.atakmap.android.takchat.uid;

import com.atakmap.android.takchat.TAKChatUtils;
import com.atakmap.android.takchat.net.ContactManager;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.packet.Stanza;

/**
 * Listens for TAK UID extension in incoming messages. Maps those to
 * <code>{@link com.atakmap.android.maps.MapItem}</code> when posible
 *
 * Created by byoung on 7/31/2016.
 */
public class TAKUidStanzaListener implements StanzaListener {

    private static final String TAG = "TakUidStanzaListener";

    @Override
    public void processStanza(Stanza stanza) throws SmackException.NotConnectedException {
        String takUid = TAKChatUtils.getUidExtension(stanza);
        if(takUid == null || takUid.length() < 1) {
            Log.w(TAG, "StanzaExtensionFilter matched but missing TAK UID: " + stanza.toXML());
        }else {
            if(TAKChatUtils.isGroupPresence(stanza)){
                //Do not set TAK UID for presence on the group
                Log.d(TAG, "StanzaExtensionFilter ignoring group presence: " + stanza.toXML() + ", with TAK UID: " + takUid);
                return;
            }

            Log.d(TAG, "StanzaExtensionFilter matched: " + stanza.toXML() + ", with TAK UID: " + takUid);
            if(FileSystemUtils.isEquals(TAKChatUtils.mapView.getDeviceUid(), takUid)){
                Log.d(TAG, "Ignoring my own UID");
                return;
            }

            //TODO groupchat from TAK user still causing issues maybe. should we only process Presence in here?
            // if not better process to vs from JID processing for groupchat
            TAKChatUtils.takChatComponent.getManager(ContactManager.class).setUID(stanza.getFrom(), takUid);
        }
    }
}
