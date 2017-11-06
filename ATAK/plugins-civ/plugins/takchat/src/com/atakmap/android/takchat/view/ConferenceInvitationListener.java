package com.atakmap.android.takchat.view;

import android.app.AlertDialog;
import android.widget.Toast;

import com.atakmap.android.takchat.TAKChatUtils;
import com.atakmap.android.takchat.data.ConferenceListener;
import com.atakmap.android.takchat.data.XmppConference;
import com.atakmap.android.takchat.net.ConferenceManager;
import com.atakmap.android.takchat.net.TAKChatXMPP;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.RoomInfo;
import org.jivesoftware.smackx.muc.packet.MUCUser;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.EntityBareJid;

/**
 * Handles conference invitations
 *
 * Created by byoung on 8/11/2016.
 */
public class ConferenceInvitationListener implements ConferenceListener {

    private static final String TAG = "ConferenceInvitationListener";

    @Override
    public boolean onMessage(Message message) {
        Log.d(TAG, "onMessage: " + message);
        return true;
    }

    @Override
    public void onInvitation(final MultiUserChat room, final String reason, final String password, final Message message, final MUCUser.Invite invite) {
        if(room == null || room.getRoom() == null || room.getRoom().getLocalpart() == null || invite == null){
            Log.d(TAG, "onInvitation invalid input");
            return;
        }

        Log.d(TAG, "onInvitation: " + invite.toXML());
        final XmppConference conference = new XmppConference(room, room.getRoom().getLocalpart().toString());
        if(!FileSystemUtils.isEmpty(password)) {
            Log.d(TAG, "Setting invitation password");
            conference.setPassword(password);
        }
        TAKChatUtils.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TAKChatUtils.takChatComponent.getManager(ConferenceManager.class).getRoomInfo(
                        conference, new ConferenceManager.RoomInfoListener() {

                            @Override
                            public void onRoomInfoReceived(RoomInfo roomInfo) {
                                if(roomInfo == null){
                                    Log.w(TAG, "No room info found for invitation: " + invite.toXML());
                                    Toast.makeText(TAKChatUtils.mapView.getContext(), "Could not get Conference Info: " +
                                            room.getRoom().toString() + " invited by: " + invite.getFrom().toString(), Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                TAKConferenceView.displayConferenceInfo(conference, roomInfo, invite);
                            }
                        });
            }
        });
    }

    @Override
    public void onDeclined(final EntityBareJid room, final EntityBareJid invitee, final String reason) {
        if(invitee == null || invitee.getLocalpart() == null){
            Log.d(TAG, "onDeclined invalid input");
            return;
        }

        String message = invitee.toString() + " declined invitation";
        if(room != null) {
            message += " to join: " + room.toString();
        }

        if(!FileSystemUtils.isEmpty(reason))
            message += "\nReason: " + reason;

        final String fMessage = message;
        TAKChatUtils.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(TAKChatUtils.mapView.getContext())
                        .setIcon(com.atakmap.app.R.drawable.xmpp_icon)
                        .setTitle(invitee.getLocalpart() + " Declined Invitation")
                        .setMessage(fMessage)
                        .setPositiveButton("OK", null)
                        .show();
            }
        });
    }

    public static void decline(BareJid room, EntityBareJid from, EntityBareJid to, String reason){
        if(!TAKChatXMPP.getInstance().isConnected()){
            Log.w(TAG, "Not connected, skipping decline: " + room.toString());
            return;
        }

        Message message = new Message(room);
        MUCUser mucUser = new MUCUser();
        MUCUser.Decline decline = new MUCUser.Decline(reason, from, to);
        mucUser.setDecline(decline);
        message.addExtension(mucUser);

        try {
            TAKChatXMPP.getInstance().getConnection().sendStanza(message);
        } catch (SmackException.NotConnectedException e) {
            Log.w(TAG, "Failed to decline: " + room.toString(), e);
        } catch (InterruptedException e) {
            Log.w(TAG, "Failed to decline: " + room.toString(), e);
        }
    }

    @Override
    public void dispose() {

    }
}
