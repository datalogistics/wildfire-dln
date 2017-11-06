package com.atakmap.android.takchat.data;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;

/**
 * Listens for XMPP <code>{@link Message}</code>
 *
 * Created by byoung on 7/29/2016.
 */
public interface MessageListener extends IListener {

    /**
     * XMPP stanza was received
     *
     * @param stanza
     * @return
     */
    boolean onMessage(Stanza stanza);
}
