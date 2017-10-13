package com.atakmap.android.takchat.data;

import org.jivesoftware.smackx.vcardtemp.packet.VCard;

/**
 * Listens for <code>{@link VCard}</code> updates
 *
 * Created by byoung on 9/23/2016.
 */
public interface VCardListener extends IListener {

    /**
     * Received a VCard update from the server
     * @param card
     * @return
     */
    boolean onVCardUpdate(VCard card);

    /**
     * Local VCard updates were saved to the server
     * @param card
     * @return
     */
    boolean onVCardSaved(VCard card);

    /**
     * Local VCard udpates failed to save to the server
     * @param card
     * @return
     */
    boolean onVCardSaveFailed(VCard card);
}
