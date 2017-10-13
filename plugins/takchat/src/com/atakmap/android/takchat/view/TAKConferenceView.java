package com.atakmap.android.takchat.view;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.atakmap.android.coordoverlay.CoordOverlayMapReceiver;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.takchat.TAKChatUtils;
import com.atakmap.android.takchat.adapter.ConferenceSelectAdapter;
import com.atakmap.android.takchat.adapter.ContactSelectAdapter;
import com.atakmap.android.takchat.data.ChatDatabase;
import com.atakmap.android.takchat.data.ChatMessage;
import com.atakmap.android.takchat.data.HostedRoomComparator;
import com.atakmap.android.takchat.data.MessageLocationLink;
import com.atakmap.android.takchat.data.XmppConference;
import com.atakmap.android.takchat.data.XmppContact;
import com.atakmap.android.takchat.net.ConferenceManager;
import com.atakmap.android.takchat.net.ContactManager;
import com.atakmap.android.takchat.net.DeliveryReceiptManager;
import com.atakmap.android.takchat.net.TAKChatXMPP;
import com.atakmap.android.takchat.plugin.R;
import com.atakmap.android.util.ATAKUtilities;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.muc.HostedRoom;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.Occupant;
import org.jivesoftware.smackx.muc.RoomInfo;
import org.jivesoftware.smackx.muc.packet.MUCUser;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Localpart;
import org.jxmpp.stringprep.XmppStringprepException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * UI for a conference, aka group chat, conversation
 *
 * Created by scallya on 9/29/2016.
 */
public class TAKConferenceView extends TAKChatView {

    private static final String TAG = "TAKConferenceView";

    private View _confView;
    private XmppConference _conference;
    private static AlertDialog _participantDialog;

    public TAKConferenceView() {
        Log.d(TAG, "ctor");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        _confView = super.onCreateView(inflater, container, savedInstanceState);
        _conference = (XmppConference)_contact;
        _chatAdapter.setDisplaySender(true);
        return _confView;
    }

    private static void displayOccupants(final XmppConference conference) {

        if(!TAKChatXMPP.getInstance().isConnected()){
            Log.d(TAG, "Not connected, cannot leave conference");
            Toast.makeText(TAKChatUtils.mapView.getContext(), "Not connected, cannot display occupants", Toast.LENGTH_SHORT).show();
            return;
        }

        if(conference == null || conference.getMUC() == null){
            Log.w(TAG, "Failed to display occupants");
            Toast.makeText(TAKChatUtils.mapView.getContext(), "Not connected to server...", Toast.LENGTH_SHORT).show();
            return;
        }

        //TODO could display icon (marker icon vs XMPP icon) and status (R/Y/G circle) in this dialog
        List<String> occupants = new ArrayList<String>();
        List<EntityFullJid> occs = conference.getMUC().getOccupants();
        for (EntityFullJid occupant : occs) {
            try {
                String id = TAKChatUtils.getJidFromLocal(occupant.getResourceOrNull().toString()).toString();
                if (id != null)
                    occupants.add(id);
            } catch (XmppStringprepException e) {
                Log.w(TAG, "Could not retrieve JID for: " + occupant.getResourceOrNull().toString(), e);
            }
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(TAKChatUtils.mapView.getContext());
        final ContactSelectAdapter adapter = new ContactSelectAdapter(occupants);
        LayoutInflater inflater = (LayoutInflater)TAKChatUtils.pluginContext.
                getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.takchat_conference_select_list, null);
        ListView list = (ListView) v.findViewById(R.id.takchat_conference_list);
        EditText search = (EditText) v.findViewById(R.id.takchat_conference_search_list);
        list.setAdapter(adapter);
        _participantDialog = builder.setTitle(conference.getName() + " occupants")
                .setIcon(com.atakmap.app.R.drawable.xmpp_icon)
                .setView(v)
                .setPositiveButton("Ok", null)
                .setNegativeButton("Invite others", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        displayContactSelectionPrompt(conference);
                    }
                })
                .create();
        adapter.setOnItemClickListener(_itemClickListener);
        search.setVisibility(View.VISIBLE);
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
            @Override
            public void afterTextChanged(Editable s) {
                adapter.search(s.toString());
            }
        });
        _participantDialog.show();
    }

    private static ContactSelectAdapter.OnItemClickListener _itemClickListener
            = new ContactSelectAdapter.OnItemClickListener() {
        @Override
        public void onItemClick(ContactSelectAdapter adapter, XmppContact contact, int position) {
            final String text = contact.toString();
            BareJid me = TAKChatUtils.getUsernameBare();
            if (me != null && text.equals(me.toString())) {
                Toast.makeText(TAKChatUtils.pluginContext, "Cannot add yourself as a contact", Toast.LENGTH_LONG).show();
                return;  //Do nothing if it's self!
            }

            XmppContact c = TAKChatUtils.getContactById(text);
            if (c == null) {
                AlertDialog.Builder builder = new AlertDialog.Builder(TAKChatUtils.mapView.getContext());
                builder.setTitle("Add User")
                        .setIcon(com.atakmap.app.R.drawable.xmpp_icon)
                        .setMessage(text + " is not currently in your contact list. Add user now?")
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                try {
                                    dialog.dismiss();
                                    ContactManager.addBuddy(JidCreate.entityBareFrom(text), text);
                                } catch (XmppStringprepException e) {
                                    Log.w(TAG, "Failed to add buddy", e);
                                    Toast.makeText(TAKChatUtils.mapView.getContext(), "Invalid chat username: " + text, Toast.LENGTH_LONG).show();
                                }
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                return;
            }else{
                Log.d(TAG, text + " is already in your contact list");
            }
            TAKChatView.showConversation(c);
            _participantDialog.dismiss();
        }
    };

    private static ArrayList<String> formatArray(List<Occupant> input, String role) {
        ArrayList<String> output = new ArrayList<String>();
        for (Occupant occupant : input) {
            output.add(occupant.getJid().toString() + " (" + role + ")");
        }
        return output;
    }

    private static void displayContactSelectionPrompt(final XmppConference conference) {
        if(!TAKChatXMPP.getInstance().isConnected()){
            Log.d(TAG, "Not connected, cannot invite contact");
            Toast.makeText(TAKChatUtils.mapView.getContext(), "Not connected, cannot invite contact", Toast.LENGTH_SHORT).show();
            return;
        }

        if(conference == null || conference.getMUC() == null){
            Log.w(TAG, "Failed to display contacts");
            Toast.makeText(TAKChatUtils.mapView.getContext(), "Not connected to server...", Toast.LENGTH_SHORT).show();
            return;
        }

        //TODO consider extending and leveraging ContactSelectAdapter here. So can display status, icon, etc
        //TODO sort, see XmppContactComparator

        final List<XmppContact> contacts = new ArrayList<XmppContact>(TAKChatUtils.takChatComponent.getManager(ContactManager.class).getContacts(false));
        ArrayList<XmppContact> currentOccupants = new ArrayList<XmppContact>();
        for (EntityFullJid fullJid : conference.getMUC().getOccupants()) {
            BareJid id = ConferenceManager.getContactFromResource(fullJid);
            if (id != null)
                currentOccupants.add(new XmppContact(id));
        }
        contacts.removeAll(currentOccupants);

        AlertDialog.Builder builder = new AlertDialog.Builder(TAKChatUtils.mapView.getContext());
        LayoutInflater inflater = (LayoutInflater)TAKChatUtils.pluginContext.
                getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.takchat_conference_select_list, null);
        final ContactSelectAdapter adapter = new ContactSelectAdapter(contacts.toArray(
                new XmppContact[contacts.size()]), true);
        final ListView view = (ListView) v.findViewById(R.id.takchat_conference_list);
        final EditText search = (EditText) v.findViewById(
                R.id.takchat_conference_search_list);
        search.setVisibility(View.VISIBLE);
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
            @Override
            public void afterTextChanged(Editable s) {
                adapter.search(s.toString());
            }
        });
        view.setAdapter(adapter);
        final AlertDialog alertDialog = builder.setIcon(com.atakmap.app.R.drawable.xmpp_icon)
                .setTitle("Select contacts to invite")
                .setView(v)
                .setPositiveButton("Invite", null)
                .setNegativeButton("Cancel", null)
                .create();
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Set<XmppContact> ret = adapter.getSelected();
                        if(ret.size() < 1) {
                            Toast.makeText(TAKChatUtils.mapView.getContext(), "Select at least one contact", Toast.LENGTH_LONG).show();
                            return;
                        }
                        alertDialog.cancel();
                        for(XmppContact contact : ret) {
                            EntityBareJid toInvite = contact.getId().asEntityBareJidIfPossible();
                            String message = "Come join conference: " + conference.getId();
                            sendInvitation(toInvite, message, conference);
                        }
                    }
                });
            }
        });
        alertDialog.show();
    }

    /**
     * Return true if invitation was sent. *Not* if it was accepted
     * @param to
     * @param message
     * @return
     */
    public static boolean sendInvitation(EntityBareJid to, String message, XmppConference conference){
        if(conference == null || conference.getMUC() == null){
            Log.w(TAG, "Failed to send invitation");
            Toast.makeText(TAKChatUtils.mapView.getContext(), "Not connected to server...", Toast.LENGTH_SHORT).show();
            return false;
        }

        if(!TAKChatXMPP.getInstance().isConnected()){
            Log.d(TAG, "Not connected, cannot send invitation");
            Toast.makeText(TAKChatUtils.mapView.getContext(), "Not connected, cannot send invitation", Toast.LENGTH_SHORT).show();
            return false;
        }

        Log.d(TAG, "Sending invitation to: " + to.toString());
        try {
            conference.getMUC().invite(to, message);
            return true;
        } catch (SmackException.NotConnectedException e) {
            //TODO handle errors
            Log.w(TAG, "Failed to sendInvitation", e);
        } catch (InterruptedException e) {
            Log.w(TAG, "Failed to sendInvitation", e);
        }

        //TODO listen for receiving error or confirmation stanzas
        //e.g.
        //RECV (0): <message from='bleeytroom@conference.jabber.at' to='bleeyt@jabber.at/ATAK-ANDROID-990004820495921' type='error' xml:lang='en' id='E21KX-14'><x xmlns='http://jabber.org/protocol/muc#user'><invite to='bleeyt2@jabber.at'><reason>Come join conference: bleey
        //08-12 11:46:10.027  30266-30520/com.atakmap.app I/System.out﹕ 11:46:10 AM RECV (0): troom</reason></invite></x><error code='405' type='cancel'><not-allowed xmlns='urn:ietf:params:xml:ns:xmpp-stanzas'/><text xmlns='urn:ietf:params:xml:ns:xmpp-stanzas'>Invitations are not allowed in this conference</text></error></message>

        return false;
    }

    @Override
    protected void sendTextMessage() {
        if(_conference == null || _conference.getMUC() == null){
            Log.w(TAG, "Failed to send message");
            displayErrorToUser("Not connected to server - Failed to send message!");
            return;
        }
        String message = _msgEdittext.getEditableText().toString();

        //TODO for group chat sent while not connected, we are not getting an exception or error
        //for PTP chat, we do get back an error
        if(!TAKChatXMPP.getInstance().isConnected()){
            //TODO should we add to DB/UI with little red X?
            Log.d(TAG, "Not connected, cannot send: " + message);
            displayErrorToUser("Not connected, cannot send message");
            return;
        }

        if (!FileSystemUtils.isEmpty(message)) {
            Log.d(TAG, "Sending group chat: " + message);
            MultiUserChat muc = _conference.getMUC();
            Message msg = new Message(muc.getRoom(), Message.Type.groupchat);
            msg.setBody(message);
            ChatMessage wrapper = new ChatMessage(msg);

            //TODO these are parsed for UI, and again in ChatDatabase.onMessageSent for DB
            //storage, could optimized to only do once, with some refactoring of ChatDatabase
            wrapper.setLocations(MessageLocationLink.getLocations(message));

            try {
                _msgEdittext.setText("");

                //add to UI
                add(wrapper);

                //send to server
                muc.sendMessage(msg);

                //notify local listeners
                wrapper.setSent(true);
                wrapper.setError(false);
                wrapper.setDelivered(false);
                wrapper.setRead(true);
                TAKChatUtils.takChatComponent.getManager(DeliveryReceiptManager.class).messageSent(wrapper);
            } catch (SmackException.NotConnectedException e) {
                //TODO use consistent error reporting throughout plugin. Toast? Notification?
                //TODO also display little red X for failed messages
                displayErrorToUser("Not connected - Failed to send message!");
                Log.w(TAG, "Failed to send group chat", e);

                //notify local listeners
                wrapper.setSent(false);
                wrapper.setError(true);
                wrapper.setDelivered(false);
                wrapper.setRead(true);

                //set from to local user, as it may not be set by Smack during error
                wrapper.getMessage().setFrom(TAKChatUtils.getUsernameFull());
                //send app internal event. Note this class is one of the listeners..
                TAKChatUtils.takChatComponent.getManager(DeliveryReceiptManager.class).messageError(wrapper);
            } catch (InterruptedException e) {
                displayErrorToUser("Interrupted Exception - try again.");
                Log.w(TAG, "Failed to send group chat", e);

                //notify local listeners
                wrapper.setSent(false);
                wrapper.setError(true);
                wrapper.setDelivered(false);
                wrapper.setRead(true);

                //set from to local user, as it may not be set by Smack during error
                wrapper.getMessage().setFrom(TAKChatUtils.getUsernameFull());
                //send app internal event. Note this class is one of the listeners..
                TAKChatUtils.takChatComponent.getManager(DeliveryReceiptManager.class).messageError(wrapper);
            }

            //TODO need to send to internal listeners?
            //Note if sent successfully, we'll get it back to be added to UI & DB, but probably want
            //to go ahead and locally store...
        }

    }

    @Override
    public void showConversationOptions(boolean bIncludeChat) {
        TAKConferenceView.showConferenceOptions(_parent, _conference, bIncludeChat);
    }

    public static void showConferenceOptions(final DropDownReceiver parent, final XmppConference conference, boolean bIncludeChat) {
        //TODO if options being shown from contact list, then display "chat" option here
        final String[] options2 = TAKChatUtils.pluginContext.getResources().getStringArray(
                bIncludeChat ? R.array.conference_menu_group_chat_array : R.array.conference_menu_group_array);
        AlertDialog.Builder builder = new AlertDialog.Builder(TAKChatUtils.mapView.getContext());
        builder.setIcon(com.atakmap.app.R.drawable.xmpp_icon)
                .setTitle("Conference Options")
                .setItems(options2, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:  //Invite to chat
                                displayContactSelectionPrompt(conference);
                                break;
                            case 1:  //Leave conference

                                if(!TAKChatXMPP.getInstance().isConnected()){
                                    Log.d(TAG, "Not connected, cannot leave conference");
                                    Toast.makeText(TAKChatUtils.mapView.getContext(), "Not connected, cannot leave conference", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                AlertDialog.Builder builder2 = new AlertDialog.Builder(TAKChatUtils.mapView.getContext());
                                builder2.setIcon(com.atakmap.app.R.drawable.xmpp_icon)
                                        .setTitle("Confirm Leave")
                                        .setMessage("Leave Conference: " + conference.getName())
                                        .setPositiveButton("Leave", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                leave(conference, parent);

                                                Toast.makeText(TAKChatUtils.mapView.getContext(), "Leaving conference: " + conference.getName(), Toast.LENGTH_SHORT).show();
                                            }
                                        })
                                        .setNegativeButton("Cancel", null)
                                        .show();
                                break;
                            case 2:  //Conference Info
                                displayConferenceInfo(conference);
                                break;
                            case 3:  //List of occupants
                                displayOccupants(conference);
                                break;
                            case 4:  //chat
                                TAKChatView.showConversation(conference);
                                break;
                            default:
                                break;
                        }
                    }
                });
        builder.create().show();
    }

    public static void leave(final XmppConference conference, final DropDownReceiver parent) {
        TAKChatUtils.runInBackground(new Runnable() {
            @Override
            public void run() {
                TAKChatUtils.takChatComponent.getManager(ConferenceManager.class).leaveConference(conference);
                if(parent != null)
                    parent.closeDropDown();
                TAKChatUtils.takChatComponent.getManager(ContactManager.class).removeGroup(conference);
                ChatDatabase.getInstance(TAKChatUtils.pluginContext).removeConference(conference);
                String conferenceName = conference.getId().getLocalpartOrNull().toString();
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                        TAKChatUtils.mapView.getContext());
                TAKChatUtils.clearSyncPoint(prefs, conferenceName);
            }
        });
    }

    public static void displayConferenceInfo(final String bareJid) {
        XmppContact contact = null;
        try{
            contact = TAKChatUtils.takChatComponent.getManager(ContactManager.class).getContactById(JidCreate.bareFrom(bareJid));
        } catch (XmppStringprepException e) {
            Log.e(TAG, "Error getting conference info", e);
            return;
        }

        if(contact == null || !(contact instanceof XmppConference)){
            Log.w(TAG, "Unable to find contact for conference: " + bareJid);
            return;
        }

        Log.d(TAG, "showing profile for conference " + contact);
        TAKConferenceView.displayConferenceInfo((XmppConference)contact);
    }

    public static void displayConferenceInfo(final XmppConference conference) {
        if(!TAKChatXMPP.getInstance().isConnected()){
            Log.d(TAG, "Not connected, cannot display conference info");
            Toast.makeText(TAKChatUtils.mapView.getContext(), "Not connected, cannot view info", Toast.LENGTH_SHORT).show();
            return;
        }

        if (conference == null) {
            Log.w(TAG, "Cannot show info for invalid conference");
            return;
        }

        TAKChatUtils.takChatComponent.getManager(ConferenceManager.class).getRoomInfo(
                conference, new ConferenceManager.RoomInfoListener() {

                    @Override
                    public void onRoomInfoReceived(RoomInfo roomInfo) {
                        if(roomInfo == null){
                            Log.w(TAG, "No room info found");
                            Toast.makeText(TAKChatUtils.mapView.getContext(), "Could not get Conference Info...", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        //display dialog with list of conferences
                        displayConferenceInfo(conference, roomInfo, null);
                    }
                });
    }

    public static void displayConferenceInfo(final XmppConference conference, final RoomInfo info, final MUCUser.Invite invitation) {
        final boolean bConnected = TAKChatXMPP.getInstance().isConnected();

        //see if we are already joined
        boolean bJoined = false;
        for(XmppConference conf : TAKChatUtils.takChatComponent.getManager(ContactManager.class).getConferences()) {
            if (conf.getId().toString().equals(conference.getId().toString())) {
                bJoined = true;
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(TAKChatUtils.mapView.getContext());
        LayoutInflater inflater = (LayoutInflater)TAKChatUtils.pluginContext.
                getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View contactView = inflater.inflate(R.layout.takchat_conference_info, null);
        builder.setView(contactView)
                .setIcon(com.atakmap.app.R.drawable.xmpp_icon)
                .setCancelable(invitation == null);

        if(bConnected && !bJoined && invitation != null){
            builder.setTitle("Conference Invitation: " + conference.getId());
            builder.setPositiveButton("Join", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //TODO alias still editable from this view?
                    try {
                        TAKChatUtils.runInBackground(new Runnable() {
                            @Override
                            public void run() {
                                TAKChatUtils.takChatComponent.getManager(ConferenceManager.class).join(conference, true);
                                ChatDatabase.getInstance(TAKChatUtils.pluginContext).addConference(conference);
                            }
                        });
                        dialog.dismiss();
                    } catch (Exception e) {
                        Log.e(TAG, "Error creating Conference", e);
                    }
                }
            });
            builder.setNegativeButton("Decline", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ConferenceInvitationListener.decline(info.getRoom(), invitation.getTo(),
                            invitation.getFrom().asEntityBareJid(), "Not interested at this time");
                    dialog.dismiss();
                }
            });
        }else{
            builder.setTitle("Conference: " + conference.getId());
            builder.setPositiveButton("Close", null);
        }

        final AlertDialog dlg = builder.create();

        final View invititationLayout = contactView.findViewById(R.id.conference_info_invite_layout);
        if(invitation == null) {
            invititationLayout.setVisibility(View.GONE);
        }else{
            invititationLayout.setVisibility(View.VISIBLE);
            if(invitation.getFrom() != null && invitation.getFrom().getLocalpart() != null)
                ((TextView) contactView.findViewById(R.id.conference_info_invite_label)).setText(invitation.getFrom().getLocalpart().toString() + " sent invite");
            if(!FileSystemUtils.isEmpty(invitation.getReason()))
                ((TextView) contactView.findViewById(R.id.conference_info_invite_description)).setText(invitation.getReason());

            ImageView icon = (ImageView) contactView.findViewById(R.id.conference_info_invite_icon);

            XmppContact contact = TAKChatUtils.takChatComponent.getManager(ContactManager.class).getContactById(invitation.getFrom().asBareJid());
            if(contact != null) {
                Marker marker = contact.getMarker();
                if(marker != null) {
                    ATAKUtilities.SetIcon(TAKChatUtils.mapView.getContext(), icon, marker);
                    ((TextView) contactView.findViewById(R.id.conference_info_invite_label)).setText(
                           CoordOverlayMapReceiver.getDisplayName(marker) + " (" +
                                   invitation.getFrom().getLocalpart().toString() + ") sent invite");
                }
            }
        }

        final TextView contact_info_alias = (TextView) contactView.findViewById(R.id.conference_info_alias);
        contact_info_alias.setText(conference.getName());

        final TextView contact_info_jid = (TextView) contactView.findViewById(R.id.conference_info_jid);
        contact_info_jid.setText(conference.getId());

        final TextView conference_info_occupants = (TextView) contactView.findViewById(R.id.conference_info_occupants);
        final ImageButton conference_info_occupantList = (ImageButton) contactView.findViewById(R.id.conference_info_occupantList);

        if(info.getOccupantsCount() > 1){
            conference_info_occupants.setText(String.valueOf(info.getOccupantsCount()) + " Occupants");
        }else{
            conference_info_occupants.setText("You are only occupant");
        }

        final TextView conference_info_desc = (TextView) contactView.findViewById(R.id.conference_info_desc);
        if(FileSystemUtils.isEmpty(info.getDescription())){
            conference_info_desc.setVisibility(View.GONE);
            contactView.findViewById(R.id.conference_info_desc_label).setVisibility(View.GONE);
        }else {
            conference_info_desc.setText(info.getDescription());
        }

        final TextView conference_info_name = (TextView) contactView.findViewById(R.id.conference_info_name);
        if(FileSystemUtils.isEmpty(info.getName())){
            conference_info_name.setVisibility(View.GONE);
            contactView.findViewById(R.id.conference_info_name_label).setVisibility(View.GONE);
        }else {
            conference_info_name.setText(info.getName());
        }

        final TextView conference_info_subject = (TextView) contactView.findViewById(R.id.conference_info_subject);
        if(FileSystemUtils.isEmpty(info.getSubject())){
            conference_info_subject.setVisibility(View.GONE);
            contactView.findViewById(R.id.conference_info_subject_label).setVisibility(View.GONE);
        }else {
            conference_info_subject.setText(info.getSubject());
        }

        List<String> contactJids = info.getContactJids();
        if(!FileSystemUtils.isEmpty(contactJids)){
            for(String cj : contactJids){
                Log.d(TAG,"Contact JID: " + cj);
            }
        }

        final CheckBox conference_info_passwdProtected = (CheckBox) contactView.findViewById(R.id.conference_info_passwdProtected);
        conference_info_passwdProtected.setChecked(info.isPasswordProtected());

        final CheckBox conference_info_membersOnly = (CheckBox) contactView.findViewById(R.id.conference_info_membersOnly);
        conference_info_membersOnly.setChecked(info.isMembersOnly());

        final CheckBox conference_info_persistent = (CheckBox) contactView.findViewById(R.id.conference_info_persistent);
        conference_info_persistent.setChecked(info.isPersistent());

        conference_info_occupantList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dlg.dismiss();
                displayOccupants(conference);
            }
        });

        contactView.findViewById(R.id.conference_info_aliasEdit).setVisibility(bConnected && (invitation == null) ? View.VISIBLE : View.GONE);
        contactView.findViewById(R.id.conference_info_aliasEdit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Edit Conference alias: " + conference.toString());
                final EditText input = new EditText(TAKChatUtils.mapView.getContext());
                input.setText(conference.getName());

                AlertDialog.Builder build = new AlertDialog.Builder(TAKChatUtils.mapView.getContext())
                        .setTitle("Enter Name")
                        .setView(input)
                        .setPositiveButton("OK",
                                new DialogInterface.OnClickListener() {

                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dlg.dismiss();

                                        String inputStr = input.getText().toString();
                                        if(FileSystemUtils.isEmpty(inputStr)){
                                            Log.d(TAG, "Conference alias empty");
                                            Toast.makeText(TAKChatUtils.mapView.getContext(), "Must not be empty...", Toast.LENGTH_SHORT).show();
                                            return;
                                        }

                                        conference.setName(inputStr);

                                        //update local DB..
                                        ChatDatabase.getInstance(TAKChatUtils.pluginContext).addConference(conference);

                                        //update contact list
                                        TAKChatUtils.takChatComponent.getManager(ContactManager.class).addGroup(conference);
                                    }
                                })
                        .setNegativeButton("Cancel", null);
                build.show();
            }
        });

        // set dialog dims appropriately based on device size
        WindowManager.LayoutParams screenLP = new WindowManager.LayoutParams();
        screenLP.copyFrom(dlg.getWindow().getAttributes());
        screenLP.width = WindowManager.LayoutParams.MATCH_PARENT;
        screenLP.height = WindowManager.LayoutParams.MATCH_PARENT;
        dlg.getWindow().setAttributes(screenLP);

        dlg.show();
    }

    /**
     * Supports 2 use cases:
     *  Create new conference, all fields editable, password optional
     *  Capture password for existing conference to join, password required
     *
     * @param prefs
     * @param room
     * @param bPasswdRequired
     */
    public static void showCreateConferencePrompt(final SharedPreferences prefs,
                                                  final HostedRoom room, final boolean bPasswdRequired) {

        Log.d(TAG, "showCreateConferencePrompt: " + bPasswdRequired);

        if(!TAKChatXMPP.getInstance().isConnected()){
            Log.d(TAG, "Not connected, cannot add contact");
            Toast.makeText(TAKChatUtils.mapView.getContext(), "Not connected, cannot add contact", Toast.LENGTH_SHORT).show();
            return;
        }

        String groupChatPrefix = prefs.getString("takchatServerConfPrefix", "conference");
        BareJid me = TAKChatUtils.getUsernameBare();
        if(me == null){
            Log.d(TAG, "Not configured, cannot add contact");
            Toast.makeText(TAKChatUtils.mapView.getContext(), "Not configured, cannot add contact", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(TAKChatUtils.mapView.getContext());
        LayoutInflater inflater = (LayoutInflater)TAKChatUtils.pluginContext.
                getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View createConferenceView = inflater.inflate(R.layout.takchat_create_conference, null);
        final AlertDialog dialog = builder.setView(createConferenceView)
                .setTitle("Add Conference")
                .setIcon(com.atakmap.app.R.drawable.xmpp_icon)
                .setCancelable(false)
                .setPositiveButton("Create", null)
                .setNeutralButton("Cancel", null)
                .create();
        final EditText conferenceServerET = (EditText) createConferenceView.findViewById(R.id.create_conference_server);
        final EditText conferenceNameET = (EditText) createConferenceView.findViewById(R.id.create_conference_name);
        final EditText conferenceAliasET = (EditText) createConferenceView.findViewById(R.id.create_conference_alias);
        final EditText conferencePasswordET = (EditText) createConferenceView.findViewById(R.id.create_conference_password);
        final EditText conferenceDescET = (EditText) createConferenceView.findViewById(R.id.create_conference_desc);
        final CheckBox conferencePersistentCB = (CheckBox) createConferenceView.findViewById(R.id.create_conference_persistent);
        final View create_conference_optionalLayout = createConferenceView.findViewById(R.id.create_conference_optionalLayout);
        conferenceServerET.setText(groupChatPrefix + "." + me.getDomain().toString());

        conferencePersistentCB.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                create_conference_optionalLayout.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            }
        });

        if(bPasswdRequired){
            create_conference_optionalLayout.setVisibility(View.VISIBLE);
        }

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(final DialogInterface diag) {
                ((AlertDialog) diag).getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String serverName = conferenceServerET.getText().toString();
                        String conferenceName = conferenceNameET.getText().toString().trim();
                        String conferenceAlias = conferenceAliasET.getText().toString().trim();
                        String conferencePassword = conferencePasswordET.getText().toString().trim();
                        final String conferenceDescription = conferenceDescET.getText().toString().trim();
                        final boolean bPersistent = conferencePersistentCB.isChecked();

                        if (serverName == null || serverName.isEmpty()) {
                            Toast.makeText(TAKChatUtils.mapView.getContext(), "Server name is not specified", Toast.LENGTH_LONG).show();
                            return;
                        }
                        if (conferenceName == null || conferenceName.isEmpty()) {
                            Toast.makeText(TAKChatUtils.mapView.getContext(), "Conference ID is invalid", Toast.LENGTH_LONG).show();
                            return;
                        }

                        conferenceName = conferenceName.trim();
                        if(FileSystemUtils.isEmpty(conferenceAlias)){
                            //default alias to conference name
                            conferenceAlias = conferenceName;
                        }

                        conferenceName = FileSystemUtils.sanitizeFilename(conferenceName);
                        if (conferenceName == null || conferenceName.isEmpty()) {
                            Toast.makeText(TAKChatUtils.mapView.getContext(), "Conference ID is invalid", Toast.LENGTH_LONG).show();
                            return;
                        }

                        if(bPasswdRequired && FileSystemUtils.isEmpty(conferencePassword)){
                            Toast.makeText(TAKChatUtils.mapView.getContext(), "Password is required", Toast.LENGTH_LONG).show();
                            return;
                        }

                        try {
                            for(XmppConference conf : TAKChatUtils.takChatComponent.getManager(ContactManager.class).getConferences()) {
                                if (conf.getId().getLocalpartOrNull().toString().equals(conferenceName)) {
                                    Toast.makeText(TAKChatUtils.mapView.getContext(), "You're already connected to a group with ID: " + conferenceName + "!", Toast.LENGTH_LONG).show();
                                    return;
                                }
                            }
                            final XmppConference conf = new XmppConference(conferenceAlias,
                                    ConferenceManager.getConferenceJid(conferenceName).asEntityBareJid(),
                                    conferencePassword);
                            TAKChatUtils.runInBackground(new Runnable() {
                                @Override
                                public void run() {
                                    if(bPasswdRequired) {
                                        Log.d(TAG, "join: " + conf.toString());
                                        TAKChatUtils.takChatComponent.getManager(ConferenceManager.class).join(conf, true);
                                    }else{
                                        //TODO get list of conferences, if already exists prompt user to join or change inputs for new conf

                                        Log.d(TAG, "create: " + conf.toString());
                                        TAKChatUtils.takChatComponent.getManager(ConferenceManager.class).create(
                                                conf, true, conferenceDescription, bPersistent);
                                    }
                                    ChatDatabase.getInstance(TAKChatUtils.pluginContext).addConference(conf);
                                }
                            });

                            diag.cancel();
                        } catch (Exception e) {
                            Log.e(TAG, "Error creating Conference", e);
                        }
                    }
                });
            }
        });

        if(room != null){
            //populate UI
            Localpart local = room.getJid().getLocalpartOrNull();
            if(local != null)
                conferenceNameET.setText(local.toString());
            conferenceAliasET.setText(room.getName());
        }

        conferenceNameET.requestFocus();
        if(bPasswdRequired){
            conferencePasswordET.setHint("Password Required");
            conferencePasswordET.requestFocus();

            conferenceDescET.setVisibility(View.GONE);
            conferencePersistentCB.setVisibility(View.GONE);
        }

        dialog.show();
    }

    /**
     * Allow user to select a room from a list
     *
     * @param hostedRooms
     * @param prefs
     */
    private static void displayConferenceSelectionPrompt(final List<HostedRoom> hostedRooms, final SharedPreferences prefs) {
        //TODO check if password is required by server. Display secure icon in list adapter
        //also require user to enter password on next dialog screen

        final List<HostedRoom> toDisplay = new ArrayList<HostedRoom>();
        List<XmppConference> existing = ChatDatabase.getInstance(TAKChatUtils.pluginContext).getConferences();
        if(!FileSystemUtils.isEmpty(existing)){
            for(HostedRoom cur : hostedRooms){
                boolean bAlreadyJoined = false;
                for(XmppConference conf : existing){
                    if(FileSystemUtils.isEquals(cur.getJid().toString(), conf.getId().toString())){
                        Log.d(TAG, "Already joined: " + cur.getJid().toString());
                        bAlreadyJoined = true;
                        break;
                    }else{
                        //TODO tone down logging after testing
                        Log.d(TAG, "Not joined: " + cur.getJid().toString() + ", " + conf.getId().toString());
                    }
                }

                if(!bAlreadyJoined) {
                    Log.d(TAG, "Adding room: " + cur.getJid().toString());
                    toDisplay.add(cur);
                }
            }
        }else{
            toDisplay.addAll(hostedRooms);
        }

        //be sure we have some contacts to display
        if(FileSystemUtils.isEmpty(toDisplay)){
            Log.d(TAG, "No available rooms found...");
            Toast.makeText(TAKChatUtils.mapView.getContext(), "No available rooms found...", Toast.LENGTH_SHORT).show();
            return;
        }

        //now sort and display for user selection
        Log.d(TAG, "Selecting conference from list of size: " + toDisplay.size());
        Collections.sort(toDisplay, new HostedRoomComparator());

        HostedRoom[] cArray = new HostedRoom[toDisplay.size()];
        toDisplay.toArray(cArray);
        final ConferenceSelectAdapter cAdapter = new ConferenceSelectAdapter(cArray);

        LayoutInflater inflater = LayoutInflater.from(TAKChatUtils.pluginContext);
        LinearLayout layout = (LinearLayout) inflater.inflate(
                R.layout.takchat_conference_select_list, null);
        EditText search = (EditText) layout.findViewById(R.id.takchat_conference_search_list);
        ListView listView = (ListView) layout.findViewById(R.id.takchat_conference_list);
        listView.setAdapter(cAdapter);

        search.setVisibility(View.VISIBLE);
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
            @Override
            public void afterTextChanged(Editable s) {
                cAdapter.search(s.toString());
            }
        });

        AlertDialog.Builder b = new AlertDialog.Builder(TAKChatUtils.mapView.getContext());
        b.setIcon(com.atakmap.app.R.drawable.xmpp_icon);
        b.setTitle("Select conference");
        b.setView(layout);
        b.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        final AlertDialog bd = b.create();
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                bd.dismiss();

                final HostedRoom room = (HostedRoom) cAdapter.getItem(position);
                if (room == null) {
                    Toast.makeText(TAKChatUtils.mapView.getContext(), "Failed to select conference...",
                            Toast.LENGTH_SHORT).show();
                    Log.w(TAG, "Failed to select conference");
                    return;
                }

                //display conf dialog again, pre-populated with this info
                Log.d(TAG, "Getting info for user selected conference: " + room.getName());
                final XmppConference conf = new XmppConference(room.getName(), room.getJid(), null);

                //user selected a room, get detailed info
                TAKChatUtils.takChatComponent.getManager(ConferenceManager.class).getRoomInfo(
                        conf, new ConferenceManager.RoomInfoListener() {

                            @Override
                            public void onRoomInfoReceived(RoomInfo roomInfo) {
                                if(roomInfo == null){
                                    Log.w(TAG, "No room info found for selected: " + room.getName());
                                    Toast.makeText(TAKChatUtils.mapView.getContext(), "Could not get Conference Info: " +
                                            room.getName() + ", please try again", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                //see if we need to collect password from user
                                if(roomInfo.isPasswordProtected()){
                                    //show dialog to collect password
                                    Log.w(TAG, "Password required for selected: " + room.getName());
                                    Toast.makeText(TAKChatUtils.mapView.getContext(), "Password requied for: " +
                                            room.getName(), Toast.LENGTH_SHORT).show();
                                    showCreateConferencePrompt(prefs, room, true);
                                }else{
                                    //no password required, join conference
                                    TAKChatUtils.runInBackground(new Runnable() {
                                        @Override
                                        public void run() {
                                            TAKChatUtils.takChatComponent.getManager(ConferenceManager.class).join(conf, true);
                                            ChatDatabase.getInstance(TAKChatUtils.pluginContext).addConference(conf);
                                        }
                                    });
                                }
                            }
                        });
            }
        });

        bd.show();
    }

    /**
     * Query list of conferences available
     *
     * @param prefs
     */
    public static void joinConference(final SharedPreferences prefs) {

        //get list of available conferences
        TAKChatUtils.takChatComponent.getManager(ConferenceManager.class).getHostedConferences(
                new ConferenceManager.HostedRoomsListener() {
                    @Override
                    public void onHostedRoomsReceived(List<HostedRoom> hostedRooms) {
                        if(FileSystemUtils.isEmpty(hostedRooms)){
                            Log.w(TAG, "No hosted rooms found");
                            Toast.makeText(TAKChatUtils.mapView.getContext(), "No hosted rooms found...", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        //display dialog with list of conferences
                        displayConferenceSelectionPrompt(hostedRooms, prefs);
                    }
                });
    }

    public static void showAddConferencePrompt(final SharedPreferences prefs) {
        new AlertDialog.Builder(TAKChatUtils.mapView.getContext())
                .setIcon(com.atakmap.app.R.drawable.xmpp_icon)
                .setTitle("Add Conference")
                .setMessage("Join existing or create new?")
                .setNeutralButton("Join", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int i) {
                        dialog.dismiss();
                        TAKConferenceView.joinConference(prefs);
                    }
                })
                .setPositiveButton("Create", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int i) {
                        dialog.dismiss();
                        TAKConferenceView.showCreateConferencePrompt(prefs, null, false);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int i) {
                        dialog.dismiss();
                    }
                })
                .show();
    }
}
