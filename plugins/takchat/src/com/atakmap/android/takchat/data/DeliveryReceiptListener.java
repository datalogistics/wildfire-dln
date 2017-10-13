package com.atakmap.android.takchat.data;

import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Stanza;
import org.jxmpp.jid.Jid;

/**
 * Listens for message delivery receipts
 * See <code>{@link Presence}</code>
 *
 * Created by byoung on 7/29/2016.
 */
public interface DeliveryReceiptListener extends IListener {

    /**
     * Message has been sent to the server
     * @param message
     */
    void onMessageSent(ChatMessage message);

    /**
     * Message has been delivered to recipient
     * Note this is an extension, not supported by all XMPP clients
     *
     * @param from
     * @param to
     * @param deliveryReceiptId
     * @param stanza
     */
    void onDeliveryReceipt(Jid from, Jid to, String deliveryReceiptId, Stanza stanza);

    /**
     * Message send failure
     *
     * @param message
     */
    void onDeliveryError(ChatMessage message);
}
