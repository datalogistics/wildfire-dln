package com.atakmap.android.takchat.net;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.atakmap.android.takchat.TAKChatUtils;
import com.atakmap.android.takchat.data.ChatDatabase;
import com.atakmap.android.takchat.data.ConferenceListener;
import com.atakmap.android.takchat.data.MessageListener;
import com.atakmap.android.takchat.data.ResponseListener;
import com.atakmap.android.takchat.data.XmppConference;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;

import org.jivesoftware.smack.PresenceListener;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.forward.packet.Forwarded;
import org.jivesoftware.smackx.mam.MamManager;
import org.jivesoftware.smackx.mam.element.MamElements;
import org.jivesoftware.smackx.mam.element.MamQueryIQ;
import org.jivesoftware.smackx.muc.DefaultParticipantStatusListener;
import org.jivesoftware.smackx.muc.DefaultUserStatusListener;
import org.jivesoftware.smackx.muc.HostedRoom;
import org.jivesoftware.smackx.muc.InvitationListener;
import org.jivesoftware.smackx.muc.InvitationRejectionListener;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatException;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.jivesoftware.smackx.muc.RoomInfo;
import org.jivesoftware.smackx.muc.SubjectUpdatedListener;
import org.jivesoftware.smackx.muc.packet.MUCUser;
import org.jivesoftware.smackx.xdata.Form;
import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.packet.DataForm;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.EntityJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.stringprep.XmppStringprepException;
import org.jxmpp.util.XmppDateTime;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Manages XMPP/MUC conferences (aka group) chat
 *
 * Created by byoung on 8/11/2016.
 */
public class ConferenceManager extends IManager<ConferenceListener> implements MessageListener, ConnectivityListener {

    final static public String TAG = "ConferenceManager";

    private boolean _bFirstConnect;
    private MultiUserChatManager _mchatManager;

    public ConferenceManager() {
        super(TAG);
        _bFirstConnect = true;
    }

    @Override
    public boolean onMessage(Stanza stanza) {
        if(Message.class.isInstance(stanza)) {
            Message message = (Message)stanza;
            if(message.getType().equals(Message.Type.groupchat)) {
                ArrayList<XmppConference> conferences = TAKChatUtils.takChatComponent.getManager(ContactManager.class).getConferences();
                for (XmppConference conf : conferences) {
                    if (conf.getId().equals(message.getFrom().asBareJid())) {
                             Log.d(TAG, "MUC onMessage: " + message.toString());
                        synchronized (_listeners) {
                            for (ConferenceListener l : _listeners) {
                                l.onMessage(message);
                            }
                        }

                        return true;
                    }
                }
            }
         }

        return false;
    }

    @Override
    public synchronized void dispose() {
        if(_isShutdown)
            return;

        super.dispose();
    }

    /**
     * Join conference
     *
     * @param conference
     * @param bSyncWithServer
     * @return  true if conference was joined
     */
    public void join(XmppConference conference, boolean bSyncWithServer) {
        Log.d(TAG, "join: " + conference.toString());

        //TODO delay setting available until successfully joined?
        conference.setAvailable(true);

        //first add to local list, so it gets added as a conference, not indirectly as a contact via a Presence message
        TAKChatUtils.takChatComponent.getManager(ContactManager.class).addGroup(conference);

        ConnectionSettings config = TAKChatXMPP.getInstance().getConfig();
        String conferenceId = conference.getId().getLocalpartOrNull().toString();

        try {
            //TODO see if MUC supported by server. Or is that part of the base spec?

            MultiUserChat mucchat = getMUC(conferenceId);
            conference.setMUC(mucchat);
            if (!mucchat.isJoined()) {
                try {
                    //TODO use requestHistorySince()?
                    if(FileSystemUtils.isEmpty(conference.getPassword())) {
                        Log.d(TAG, "Joining room " + conferenceId + " for username " + config.getUsername());
                        mucchat.join(Resourcepart.from(config.getUsername()));
                    }else{
                        Log.d(TAG, "Joining room " + conferenceId + " for username " + config.getUsername() + ", with password");
                        mucchat.join(Resourcepart.from(config.getUsername()), conference.getPassword());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error while joining the room " + conferenceId, e);
                    //TODO bail out?
                }

                Log.d(TAG, "Room joined: " + mucchat.getRoom().toString());
                //Note conference/groupchat messages are handled ConferenceManager.onMessage()

                addListeners(conference, mucchat);
            } else {
                Log.d(TAG, "Room already joined: " + conferenceId + " for username " + config.getUsername());
            }
        } catch (XmppStringprepException e) {
            //TODO handle these errors
            Log.w(TAG, "Unable to join room: " + conferenceId, e);
        }

        if (!bSyncWithServer) {
            Log.d(TAG, "Not syncing: " + conferenceId);
            return;
        }

        syncWithServer(config, conferenceId);
        return;
    }

    public void create(XmppConference conference, boolean bSyncWithServer,
                       String conferenceDescription, boolean bPersistent) {
        Log.d(TAG, "create: " + conference.toString());
        //TODO background thread for create, join, createOrJoin

        String local = TAKChatUtils.getUsernameLocalPart();
        if(FileSystemUtils.isEmpty(local)){
            Log.d(TAG, "create no local username");
            return;
        }

        BareJid bareJid = TAKChatUtils.getUsernameBare();
        if(bareJid == null){
            Log.d(TAG, "create no bare username");
            return;
        }

        //TODO delay setting available until successfully joined? and not if an error is received
        conference.setAvailable(true);

        //first add to local list, so it gets added as a conference, not indirectly as a contact via a Presence message
        TAKChatUtils.takChatComponent.getManager(ContactManager.class).addGroup(conference);

        ConnectionSettings config = TAKChatXMPP.getInstance().getConfig();
        String conferenceId = conference.getId().getLocalpartOrNull().toString();

        try {
            //TODO see if MUC supported by server. Or is that part of the base spec?

            MultiUserChat mucchat = getMUC(conferenceId);
            conference.setMUC(mucchat);
            if (!mucchat.isJoined()) {
                try {
                    Log.d(TAG, "Creating room " + conferenceId + " for username " + config.getUsername());
                    MultiUserChat.MucCreateConfigFormHandle handle = mucchat.create(Resourcepart.from(config.getUsername()));
                    //TODO use MucEnterConfiguration.Builder.requestHistorySince()?

                    if(!bPersistent){
                        Log.d(TAG, "Accepting room defaults " + conferenceId);
                        handle.makeInstant();
                    }else {
                        Log.d(TAG, "Creating persistent room " + conferenceId);

                        Form form = mucchat.getConfigurationForm();
                        Form answerForm = form.createAnswerForm();
                        for (FormField field : form.getFields()) {
                            if (!FormField.Type.hidden.name().equals(field.getType()) && field.getVariable() != null) {
                                Log.d(TAG, "form: " + field.getType() + " = " + field.getVariable()); //TODO remove some logging after testing
                                answerForm.setDefaultAnswer(field.getVariable());
                            }
                        }

                        answerForm.setAnswer(FormField.FORM_TYPE, "http://jabber.org/protocol/muc#roomconfig");
                        answerForm.setAnswer("muc#roomconfig_roomname", conference.getName());

                        if (!FileSystemUtils.isEmpty(conferenceDescription)) {
                            answerForm.setAnswer("muc#roomconfig_roomdesc", conferenceDescription);
                        }

                        //TODO make these other options configurable?
                        answerForm.setAnswer("muc#roomconfig_publicroom", true);
                        answerForm.setAnswer("muc#roomconfig_persistentroom", bPersistent);
                        answerForm.setAnswer("muc#roomconfig_allowinvites", true);

                        if (!FileSystemUtils.isEmpty(conference.getPassword())) {
                            answerForm.setAnswer("muc#roomconfig_passwordprotectedroom", true);
                            answerForm.setAnswer("muc#roomconfig_roomsecret", conference.getPassword());

                        } else {
                            answerForm.setAnswer("muc#roomconfig_passwordprotectedroom", false);
                        }

                        final List<String> owners = new ArrayList<String>();
                        owners.add(bareJid.toString());
                        answerForm.setAnswer("muc#roomconfig_roomowners", owners);

                        mucchat.sendConfigurationForm(answerForm);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error while creating the room " + conferenceId, e);
                    //TODO bail out?
                }

                Log.d(TAG, "Room created: " + mucchat.getRoom().toString());
                //Note conference/groupchat messages are handled ConferenceManager.onMessage()

                addListeners(conference, mucchat);

            } else {
                Log.d(TAG, "Room already joined: " + conferenceId + " for username " + config.getUsername());
            }
        } catch (XmppStringprepException e) {
            //TODO handle these errors
            Log.w(TAG, "Unable to create room: " + conferenceId, e);
        }

        if (!bSyncWithServer) {
            Log.d(TAG, "Not syncing: " + conferenceId);
            return;
        }

        syncWithServer(config, conferenceId);
        return;
    }

    /**
     * Join conference if exists, otherwise create
     *
     * @param conference
     * @param bSyncWithServer
     * @return  true if conference was joined
     */
    public void createOrJoin(XmppConference conference, boolean bSyncWithServer) {
        Log.d(TAG, "createOrJoin: " + conference.toString());

        //TODO delay setting available until successfully joined?
        conference.setAvailable(true);

        //first add to local list, so it gets added as a conference, not indirectly as a contact via a Presence message
        TAKChatUtils.takChatComponent.getManager(ContactManager.class).addGroup(conference);

        ConnectionSettings config = TAKChatXMPP.getInstance().getConfig();
        String conferenceId = conference.getId().getLocalpartOrNull().toString();

        try {
            //TODO see if MUC supported by server. Or is that part of the base spec?

            MultiUserChat mucchat = getMUC(conferenceId);
            conference.setMUC(mucchat);
            if (!mucchat.isJoined()) {
                try {
                    //TODO only create if its a temp room?
                    //TODO after a restart, password may be required..

                    //TODO use requestHistorySince()?
                    if(FileSystemUtils.isEmpty(conference.getPassword())) {
                        Log.d(TAG, "createOrJoin room " + conferenceId + " for username " + config.getUsername());
                        mucchat.createOrJoin(Resourcepart.from(config.getUsername()));
                    }else{
                        Log.d(TAG, "createOrJoin room " + conferenceId + " for username " + config.getUsername() + ", with password");
                        mucchat.createOrJoinIfNecessary(Resourcepart.from(config.getUsername()), conference.getPassword());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error while createOrJoin the room " + conferenceId, e);
                    //TODO bail out?
                }

                Log.d(TAG, "Room createOrJoin: " + mucchat.getRoom().toString());
                //Note conference/groupchat messages are handled ConferenceManager.onMessage()

                addListeners(conference, mucchat);

            } else {
                Log.d(TAG, "Room already joined: " + conferenceId + " for username " + config.getUsername());
            }
        } catch (XmppStringprepException e) {
            //TODO handle these errors
            Log.w(TAG, "Unable to createOrJoin room: " + conferenceId, e);
        }

        if (!bSyncWithServer) {
            Log.d(TAG, "Not syncing: " + conferenceId);
            return;
        }

        syncWithServer(config, conferenceId);
        return;
    }

    private void addListeners(XmppConference conference, MultiUserChat mucchat) {
        //listen for room invitations we sent being declined
        mucchat.addInvitationRejectionListener(new InvitationRejectionListener() {
            @Override
            public void invitationDeclined(EntityBareJid invitee, String reason, Message message, MUCUser.Decline decline) {
                Log.d(TAG, "invitationDeclined: " + message.toXML());
                //TODO sound?
                synchronized (_listeners) {
                    for (ConferenceListener l : _listeners) {
                        l.onDeclined(message.getFrom() == null ? null : message.getFrom().asEntityBareJidIfPossible(),
                                invitee, reason);
                    }
                }
            }
        });

        //listen for room occupant presence
        mucchat.addParticipantListener(new PresenceListener() {
            @Override
            public void processPresence(Presence presence) {
                Log.d(TAG, "processPresence: " + presence.toXML());
            }
        });

        //listen for my privileges in the room
        mucchat.addUserStatusListener(new DefaultUserStatusListener() {
            @Override
            public void kicked(Jid actor, String s) {
                if(actor == null) return;
                Log.d(TAG, "kicked: " + actor.toString() + ", " + s);
            }

            @Override
            public void voiceGranted() {
                Log.d(TAG, "voiceGranted");
            }

            @Override
            public void voiceRevoked() {
                Log.d(TAG, "voiceGranted");
            }

            @Override
            public void banned(Jid actor, String s) {
                if(actor == null) return;
                Log.d(TAG, "banned: " + actor.toString() + ", " + s);
            }

            @Override
            public void roomDestroyed(MultiUserChat multiUserChat, String s) {
                if(multiUserChat == null) return;
                Log.d(TAG, "roomDestroyed: " + multiUserChat.toString() + ", " + s);
            }
        });

        //listen for privileges of other users in the room
        mucchat.addParticipantStatusListener(new DefaultParticipantStatusListener() {
            @Override
            public void joined(EntityFullJid participant) {
                if(participant == null) return;
                Log.d(TAG, "participant joined: " + participant.toString());
                //SoundManager.getInstance().play(SoundManager.SOUND.COMING);
            }

            @Override
            public void left(EntityFullJid participant) {
                if(participant == null) return;
                Log.d(TAG, "participant left: " + participant.toString());
                //SoundManager.getInstance().play(SoundManager.SOUND.LEAVE);
            }

            @Override
            public void banned(EntityFullJid participant, Jid actor, String reason) {
                if(participant == null) return;
                Log.d(TAG, "participant banned: " + participant.toString());
            }

            @Override
            public void voiceGranted(EntityFullJid participant) {
                if(participant == null) return;
                Log.d(TAG, "participant voiceGranted: " + participant.toString());
            }

            @Override
            public void voiceRevoked(EntityFullJid participant) {
                if(participant == null) return;
                Log.d(TAG, "participant voiceRevoked: " + participant.toString());
            }
        });

        mucchat.addSubjectUpdatedListener(new SubjectUpdatedListener() {
            @Override
            public void subjectUpdated(String s, EntityFullJid entityFullJid) {
                Log.d(TAG, "subjectUpdated: " + entityFullJid.toString() + ", " + s);
            }
        });
    }

    /**
     * Note portions of this method are blocking remote/server calls e.g. archive.queryArchive
     *
     * @param config
     * @param conferenceId
     * @return
     */
    private boolean syncWithServer(ConnectionSettings config, String conferenceId) {
        //get time we last synced
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(TAKChatUtils.mapView.getContext());
        long now = System.currentTimeMillis();
        long syncPoint = TAKChatUtils.getSyncPoint(prefs, conferenceId);

        if(!TAKChatXMPP.getInstance().isConnected()){
            Log.w(TAG, "cannot syncWithServer without connection");
            return false;
        }

        try {
            MamManager archive = MamManager.getInstanceFor(TAKChatXMPP.getInstance().getConnection());
            if(!archive.isSupportedByServer()){
                Log.d(TAG, "Message archives not supported by server: " + config.getServerAddress());
                return false;
            }

            Log.d(TAG, "Syncing '" + conferenceId + "' messages since " + syncPoint  + " (" + (now - syncPoint)/1000  + " seconds) server: " + config.getServerAddress());

            //TODO support server paging rather than query entire history upon first join?
            //TODO better org, query all saved rooms on this device
            //Now query archive for group chats
            Jid roomJid = getConferenceJid(conferenceId);
            MamManager.MamQueryResult queryResult = archive.queryArchive(null, new Date(syncPoint), null, roomJid, null);
            if(queryResult == null || queryResult.forwardedMessages == null || queryResult.forwardedMessages.size() < 1){
                Log.d(TAG, "No messages archived at server: " + config.getServerAddress() + " for room: " + roomJid.toString());
            }else {

                Log.d(TAG, "Received " + queryResult.forwardedMessages.size() + " archived messages from server: " + config.getServerAddress() + " for room: " + roomJid.toString());
                if (TAKChatXMPP.SMACK_DEBUG) {
                    for (Forwarded message : queryResult.forwardedMessages) {
                        Log.d(TAG, "Received archived MUC message: " + message.toXML());
                    }
                }
            }

            //attempt use of "to" rather than "with" for search query
            TAKMamQueryIQ manualQuery = new TAKMamQueryIQ(roomJid, new Date(syncPoint));
            Log.d(TAG, "Sending manual IQ query: " + manualQuery.toString());
            //_connection.setFromMode(XMPPConnection.FromMode.UNCHANGED);

            TAKChatUtils.takChatComponent.getManager(DeliveryReceiptManager.class).sendRequest(manualQuery, new ResponseListener() {
                @Override
                public void onReceived(String stanzaId, IQ iq) {
                    Log.d(TAG, "onReceived: " + stanzaId);
                    //TODO
                }

                @Override
                public void onError(String stanzaId, IQ iq) {
                    Log.d(TAG, "onError: " + stanzaId);
                    //TODO
                }

                @Override
                public void onTimeout(String stanzaId) {
                    Log.d(TAG, "onTimeout: " + stanzaId);
                    //TODO
                }

                @Override
                public void onDisconnect(String stanzaId) {
                    Log.d(TAG, "onDisconnect: " + stanzaId);
                    //TODO
                }
            });
            //_connection.setFromMode(XMPPConnection.FromMode.USER);

            //update last sync time
            TAKChatUtils.setSyncPoint(prefs, conferenceId, now);
        } catch (SmackException.NoResponseException e) {
            //TODO process these errors further?
            Log.w(TAG, "Unable to sync archive", e);
        } catch (XMPPException.XMPPErrorException e) {
            Log.w(TAG, "Unable to sync archive", e);
        } catch (SmackException.NotConnectedException e) {
            Log.w(TAG, "Unable to sync archive", e);
        } catch (InterruptedException e) {
            Log.w(TAG, "Unable to sync archive", e);
        } catch (SmackException.NotLoggedInException e) {
            Log.w(TAG, "Unable to sync archive", e);
        } catch (XmppStringprepException e) {
            Log.w(TAG, "Unable to sync archive", e);
        }

        return true;
    }

    public static BareJid getContactFromResource(Jid id) {
        if(id == null || id.getResourceOrNull() == null)
            return null;

        try {
            return TAKChatUtils.getJidFromLocal(id.getResourceOrNull().toString());
        } catch (XmppStringprepException e) {
            Log.w(TAG, "Unable to get conference from resource", e);
        }
        return null;
    }

    public static EntityJid getConferenceJid(String conferenceName) throws XmppStringprepException {
        ConnectionSettings config = TAKChatXMPP.getInstance().getConfig();
        if(config == null || !config.isValid()){
            return null;
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(TAKChatUtils.mapView.getContext());
        String groupChatServer = prefs.getString("takchatServerConfPrefix", "conference");
        if(!FileSystemUtils.isEmpty(groupChatServer)){
            groupChatServer += ".";
        }

        groupChatServer += config.getServerAddress();
        return JidCreate.entityBareFrom(conferenceName + "@" + groupChatServer);
    }

    private static DomainBareJid getConferenceSvcName() throws XmppStringprepException {
        ConnectionSettings config = TAKChatXMPP.getInstance().getConfig();
        if(config == null || !config.isValid()){
            return null;
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(TAKChatUtils.mapView.getContext());
        String groupChatServer = prefs.getString("takchatServerConfPrefix", "conference");
        if(!FileSystemUtils.isEmpty(groupChatServer)){
            groupChatServer += ".";
        }

        groupChatServer += config.getServerAddress();
        return JidCreate.domainBareFrom(groupChatServer);
    }

    private MultiUserChat getMUC(String conferenceName) throws XmppStringprepException {
        return _mchatManager.getMultiUserChat(getConferenceJid(conferenceName).asEntityBareJid());
    }


    public void leaveConference(XmppConference conference) {
        leaveConference(conference.getId().getLocalpartOrNull().toString());
    }

    public boolean leaveConference(String conferenceName){
        try {
            Log.d(TAG, "leaving conference: " + conferenceName);
            MultiUserChat muc = getMUC(conferenceName);
            if(muc == null){
                Log.w(TAG, "Failed to leave conference: " + conferenceName);
                return false;
            }

            muc.leave();
            return true;
        } catch (XmppStringprepException e) {
            Log.w(TAG, "Failed to leave conference", e);
        } catch (InterruptedException e) {
            Log.w(TAG, "Failed to leave conference", e);
        } catch (SmackException.NotConnectedException e) {
            Log.w(TAG, "Failed to leave conference", e);
        }
        return false;
    }

    @Override
    public boolean onConnected() {
        Log.d(TAG, "onConnected");

        if(_bFirstConnect) {
            _bFirstConnect = false;

            _mchatManager = MultiUserChatManager.getInstanceFor(TAKChatXMPP.getInstance().getConnection());
            _mchatManager.addInvitationListener(new InvitationListener() {
                @Override
                public void invitationReceived(XMPPConnection xmppConnection, MultiUserChat room, EntityJid inviter, String reason, String password, Message message, MUCUser.Invite invite) {
                    Log.d(TAG, "invitationReceived: " + room.toString());

                    synchronized (_listeners) {
                        for (ConferenceListener l : _listeners) {
                            l.onInvitation(room, reason, password, message, invite);
                        }
                    }
                }
            });

            //now group chat is available
            List<XmppConference> conferences = TAKChatUtils.takChatComponent.getManager(ContactManager.class).getConferences();
            for (XmppConference conference : conferences) {
                conference.setAvailable(true);
                conference.addContactListRef();
            }

            //do initial join
            Log.d(TAG, "onConnected, join all");
            joinAll(false);
        }else{
            //need to re-join conference when connected/reconnected
            Log.d(TAG, "onConnected, rejoin all");
            joinAll(true);
        }

        return true;
    }

    @Override
    public boolean onStatus(boolean bSuccess, String status) {
        return true;
    }

    @Override
    public boolean onDisconnected() {
        Log.d(TAG, "onDisconnected");

        //now group chat is available
        List<XmppConference> conferences = TAKChatUtils.takChatComponent.getManager(ContactManager.class).getConferences();
        for (XmppConference conference : conferences) {
            conference.setAvailable(false);
            conference.removeContactListRef();
        }

        return true;
    }

    /**
     * Set the iq/query[to] rather than the "with"
     * This seems to work for getting a MUC archive
     */
    private static class TAKMamQueryIQ extends MamQueryIQ {

        public TAKMamQueryIQ(Jid to, Date start){
            super(UUID.randomUUID().toString(), null, getNewMamForm(start));
            setType(Type.set);
            setTo(to);
        }

        private static DataForm getNewMamForm(Date start) {
            FormField field = new FormField("FORM_TYPE");
            field.setType(org.jivesoftware.smackx.xdata.FormField.Type.hidden);
            field.addValue(MamElements.NAMESPACE);
            DataForm form = new DataForm(org.jivesoftware.smackx.xdata.packet.DataForm.Type.submit);
            form.addField(field);

            FormField formField = new FormField("start");
            formField.addValue(XmppDateTime.formatXEP0082Date(start));
            form.addField(formField);

            return form;
        }
    }

    @Override
    public void onLoaded() {
        //add conferences to contact list
        loadConferences();
    }

    public void loadConferences() {
        Log.d(TAG, "loadConferences");

        //Load saved conferences as contacts, not available until connected
        List<XmppConference> conferences = ChatDatabase.getInstance(TAKChatUtils.pluginContext).getConferences();
        for (XmppConference conference : conferences) {
            TAKChatUtils.takChatComponent.getManager(ContactManager.class).addGroup(conference);
        }
    }

    public void getHostedConferences(HostedRoomsListener listener) {
        Log.d(TAG, "getHostedConferences");
        if(_mchatManager == null){
            Log.w(TAG, "getHostedConferences failed, chat manager not set yet");
            Toast.makeText(TAKChatUtils.mapView.getContext(), "Failed to get conference list...", Toast.LENGTH_SHORT).show();
            return;
        }


        String hostname = null;
        try {
            DomainBareJid svc = ConferenceManager.getConferenceSvcName();
            if(svc == null){
                Log.w(TAG, "Failed to get conference service name");
                //TODO notify user... return;
            }

            hostname = svc.toString();
        } catch (XmppStringprepException e) {
            Log.w(TAG, "Failed to get conference service name", e);
            //TODO notify user...
            return;
        }
        new GetHostedRoomTask(_mchatManager, hostname, listener).execute();
    }


    public interface HostedRoomsListener {
        void onHostedRoomsReceived(List<HostedRoom> hostedRooms);
    }

    public void requestHostedRooms(final HostedRoomsListener listener) {

        TAKChatUtils.runInBackground(new Runnable() {
            @Override
            public void run() {
                List<HostedRoom> hostedRooms = null;

                try {
                    hostedRooms = _mchatManager.getHostedRooms(getConferenceSvcName());
                } catch (SmackException.NoResponseException e){
                    Log.w(TAG, "Failed to get hosted rooms", e);
                } catch (InterruptedException e) {
                    Log.w(TAG, "Failed to get hosted rooms", e);
                } catch (XMPPException.XMPPErrorException e) {
                    Log.w(TAG, "Failed to get hosted rooms", e);
                } catch (XmppStringprepException e) {
                    Log.w(TAG, "Failed to get hosted rooms", e);
                } catch (SmackException.NotConnectedException e) {
                    Log.w(TAG, "Failed to get hosted rooms", e);
                } catch (MultiUserChatException.NotAMucServiceException e) {
                    Log.w(TAG, "Failed to get hosted rooms", e);
                }

                listener.onHostedRoomsReceived(hostedRooms);
            }
        });

    }

    public void getRoomInfo(XmppConference conference, RoomInfoListener listener) {
        Log.d(TAG, "getRoomInfo");
        if(_mchatManager == null){
            Log.w(TAG, "getRoomInfo failed, chat manager not set yet");
            Toast.makeText(TAKChatUtils.mapView.getContext(), "Failed to get conference info...", Toast.LENGTH_SHORT).show();
            return;
        }

        new GetRoomInfoTask(_mchatManager, conference, listener).execute();
    }

    public interface RoomInfoListener {
        void onRoomInfoReceived(RoomInfo roomInfo);
    }

    public void requestRoomInfo(final EntityBareJid roomJid, final RoomInfoListener listener) {
        TAKChatUtils.runInBackground(new Runnable() {
            @Override
            public void run() {
                RoomInfo roomInfo = null;

                try {
                    Log.d(TAG, "Requesting room info " + roomJid);
                    roomInfo = _mchatManager.getRoomInfo(roomJid);
                } catch (SmackException.NoResponseException e){
                    Log.w(TAG, "Failed to get hosted rooms", e);
                } catch (InterruptedException e) {
                    Log.w(TAG, "Failed to get hosted rooms", e);
                } catch (SmackException.NotConnectedException e) {
                    Log.w(TAG, "Failed to get hosted rooms", e);
                } catch (XMPPException.XMPPErrorException e) {
                    Log.w(TAG, "Failed to get hosted rooms", e);
                }

                //TODO jump back to UI thread?
                listener.onRoomInfoReceived(roomInfo);
            }
        });
    }

    /**
     * Join all conferences
     */
    public void joinAll(final boolean bLeave){

        Log.d(TAG, "joinAll");

        TAKChatUtils.runInBackground(new Runnable() {
            @Override
            public void run() {
                ArrayList<XmppConference> confs = TAKChatUtils.takChatComponent.getManager(ContactManager.class).getConferences();
                if(FileSystemUtils.isEmpty(confs)){
                    Log.d(TAG, "No conferences to join");
                    return;
                }

                for(XmppConference conference : confs){
                    if(bLeave){
                        ConferenceManager.this.leaveConference(conference);
                    }

                    Log.d(TAG, "join: " + conference.toVerboseString());

                    ConferenceManager.this.createOrJoin(conference, true);
                }
            }
        });
    }

    /**
     * Simple background task to query list of conferences available on XMPP server
     * Display waiting dialog in the meantime
     *
     * @author byoung
     */
    static class GetHostedRoomTask extends AsyncTask<Void, Integer, Void>{

        private static final String TAG = "GetHostedRoomTask";

        private final HostedRoomsListener _callback;
        private final String _hostname;
        private ProgressDialog _progressDialog;
        private final MultiUserChatManager _mgr;

        private List<HostedRoom> _rooms;

        public GetHostedRoomTask(MultiUserChatManager mgr, String hostname, HostedRoomsListener cb) {
            this._callback = cb;
            this._mgr = mgr;
            this._hostname = hostname;
        }

        @Override
        protected void onPreExecute() {
            // Before running code in background/worker thread
            _progressDialog = new ProgressDialog(TAKChatUtils.mapView.getContext());
            _progressDialog.setTitle("Searching");
            _progressDialog.setIcon(com.atakmap.app.R.drawable.xmpp_icon);
            _progressDialog
                    .setMessage("Searching for conferences on " + _hostname +  "...");
            _progressDialog.setCancelable(true);
            _progressDialog.show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            Thread.currentThread().setName("GetHostedRoomTask");

            try {
                _rooms = _mgr.getHostedRooms(getConferenceSvcName());
                Log.d(TAG, (FileSystemUtils.isEmpty(_rooms) ? "No" : "" + _rooms.size()) + " rooms found");
            } catch (Exception e){
                Log.w(TAG, "Failed to get hosted rooms", e);
            }

            return null;
        }

        @Override
        protected void onCancelled(Void aVoid) {
            super.onCancelled(aVoid);
            Log.d(TAG, "Cancelled");

            if (_progressDialog != null) {
                _progressDialog.dismiss();
                _progressDialog = null;
            }

            if (_callback != null) {
                _callback.onHostedRoomsReceived(null);
            }
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if(FileSystemUtils.isEmpty(_rooms)){
                Log.w(TAG, "No hosted rooms found");
            }

            if (_progressDialog != null) {
                _progressDialog.dismiss();
                _progressDialog = null;
            }

            if (_callback != null) {
                _callback.onHostedRoomsReceived(_rooms);
            }
        }
    }

    /**
     * Simple background task to query info for a Conference Room
     * Display waiting dialog in the meantime
     *
     * @author byoung
     */
    static class GetRoomInfoTask extends AsyncTask<Void, Integer, Void>{

        private static final String TAG = "GetRoomInfoTask";

        private final RoomInfoListener _callback;
        private final XmppConference _conference;
        private ProgressDialog _progressDialog;
        private final MultiUserChatManager _mgr;

        private RoomInfo _roomInfo;

        public GetRoomInfoTask(MultiUserChatManager mgr, XmppConference room, RoomInfoListener cb) {
            this._callback = cb;
            this._mgr = mgr;
            this._conference = room;
        }

        @Override
        protected void onPreExecute() {
            // Before running code in background/worker thread
            _progressDialog = new ProgressDialog(TAKChatUtils.mapView.getContext());
            _progressDialog.setTitle("Searching");
            _progressDialog.setIcon(com.atakmap.app.R.drawable.xmpp_icon);
            _progressDialog
                    .setMessage("Searching for " + _conference.getName() + " info...");
            _progressDialog.setCancelable(true);
            _progressDialog.show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            Thread.currentThread().setName(TAG);

            try {
                _roomInfo = _mgr.getRoomInfo(_conference.getId().asEntityBareJidIfPossible());
                Log.d(TAG, "Room Info found: " + _roomInfo.toString());
            } catch (SmackException.NoResponseException e){
                Log.w(TAG, "Failed to get room info", e);
            } catch (InterruptedException e) {
                Log.w(TAG, "Failed to get room info", e);
            } catch (SmackException.NotConnectedException e) {
                Log.w(TAG, "Failed to get room info", e);
            } catch (XMPPException.XMPPErrorException e) {
                Log.w(TAG, "Failed to get room info", e);
            }

            return null;
        }

        @Override
        protected void onCancelled(Void aVoid) {
            super.onCancelled(aVoid);
            Log.d(TAG, "Cancelled");

            if (_progressDialog != null) {
                _progressDialog.dismiss();
                _progressDialog = null;
            }

            if (_callback != null) {
                _callback.onRoomInfoReceived(null);
            }
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if(_roomInfo == null){
                Log.w(TAG, "No room info found");
            }

            if (_progressDialog != null) {
                _progressDialog.dismiss();
                _progressDialog = null;
            }

            if (_callback != null) {
                _callback.onRoomInfoReceived(_roomInfo);
            }
        }
    }
}
