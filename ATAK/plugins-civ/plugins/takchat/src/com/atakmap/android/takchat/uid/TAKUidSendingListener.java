package com.atakmap.android.takchat.uid;

import com.atakmap.android.takchat.TAKChatUtils;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.packet.Stanza;

/**
 * Adds local TAK UID to select outoing XMPP messages
 *
 * Created by byoung on 8/2/2016.
 */
public class TAKUidSendingListener implements StanzaListener {

    private static final String TAG = "TAKUidSendingListener";

    @Override
    public void processStanza(Stanza stanza) throws SmackException.NotConnectedException {
        //TAKChatUtils.addUidExtension(stanza, _config.getTakUid());
        TAKChatUtils.addUidExtension(stanza, TAKChatUtils.mapView.getDeviceUid());
        //Log.d(TAG, "Adding UID: " + TAKChatUtils.mapView.getDeviceUid() + " to stanza: " + stanza.toXML());
    }
}
