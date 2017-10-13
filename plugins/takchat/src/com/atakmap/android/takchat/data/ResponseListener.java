package com.atakmap.android.takchat.data;

import org.jivesoftware.smack.packet.IQ;

/**
 * Created by byoung on 11/21/16.
 *
 * TODO combine with <code>{@link DeliveryReceiptListener}</code>?
 */

public interface ResponseListener {

    void onReceived(String packetId, IQ iq);

    void onError(String packetId, IQ iq);

    void onTimeout(String packetId);

    void onDisconnect(String packetId);
}
