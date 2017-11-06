package com.atakmap.android.takchat.data;

import org.jivesoftware.smack.packet.Presence;

/**
 * Listens for contact list updates
 * See <code>{@link Presence}</code>
 *
 * Created by byoung on 7/29/2016.
 */
public interface ContactListener extends IListener {

    /**
     * 
     * @param presence
     * @return
     */
    boolean onPresenceChanged(Presence presence);

    /**
     *
     * @return
     */
    boolean onContactSizeChanged();
}
