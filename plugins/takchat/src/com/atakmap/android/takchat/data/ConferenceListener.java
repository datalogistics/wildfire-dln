package com.atakmap.android.takchat.data;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.packet.MUCUser;
import org.jxmpp.jid.EntityBareJid;

/**
 * Listens for conference messages and invitations
 * See <code>{@link Message}</code>
 *
 * Created by byoung on 8/11/2016.
 */
public interface ConferenceListener extends IListener {

    /**
     * Group chat message received
     *
     * @param message
     * @return
     */
    boolean onMessage(Message message);

    /**
     * Confernce invitation received
     *
     * @param room
     * @param reason
     * @param password
     * @param message
     * @param invite
     */
    void onInvitation(MultiUserChat room, String reason, String password, Message message, MUCUser.Invite invite);

    /**
     * An invitation we sent was declined
     *
     * @param invitee
     * @param reason
     */
    void onDeclined(EntityBareJid room, EntityBareJid invitee, String reason);
}
