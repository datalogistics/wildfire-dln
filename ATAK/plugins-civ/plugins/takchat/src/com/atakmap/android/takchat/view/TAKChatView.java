package com.atakmap.android.takchat.view;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.takchat.TAKChatDropDownReceiver;
import com.atakmap.android.takchat.TAKChatUtils;
import com.atakmap.android.takchat.adapter.ChatAdapter;
import com.atakmap.android.takchat.adapter.ContactSelectAdapter;
import com.atakmap.android.takchat.data.ChatDatabase;
import com.atakmap.android.takchat.data.ChatMessage;
import com.atakmap.android.takchat.data.ContactListener;
import com.atakmap.android.takchat.data.DeliveryReceiptListener;
import com.atakmap.android.takchat.data.MessageListener;
import com.atakmap.android.takchat.data.MessageLocationLink;
import com.atakmap.android.takchat.data.XmppConference;
import com.atakmap.android.takchat.data.XmppContact;
import com.atakmap.android.takchat.data.XmppContactComparator;
import com.atakmap.android.takchat.net.ConnectivityListener;
import com.atakmap.android.takchat.net.ContactManager;
import com.atakmap.android.takchat.net.MessageManager;
import com.atakmap.android.takchat.net.MessageUnreadManager;
import com.atakmap.android.takchat.net.TAKChatXMPP;
import com.atakmap.android.takchat.plugin.R;
import com.atakmap.android.tools.ActionBarReceiver;
import com.atakmap.android.tools.ActionBarView;
import com.atakmap.android.util.ATAKUtilities;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.chatstates.packet.ChatStateExtension;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Domainpart;
import org.jxmpp.jid.parts.Localpart;
import org.jxmpp.stringprep.XmppStringprepException;
import org.jxmpp.util.XmppStringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * UI for a chat conversation
 *
 * Created by byoung on 7/12/2016.
 */
public class TAKChatView extends Fragment implements ConnectivityListener, MessageListener, ContactListener, DeliveryReceiptListener {

    private static final String TAG = "TAKChatView";

    /**
     * Map Bare JID to chat view
     */
    static Map<String, TAKChatView> mapping;

    protected KeyboardListenerEditText _msgEdittext;
    protected EditText _msgSearchTxt;
    protected ChatAdapter _chatAdapter;
    private View _rootView, _conversationHeaderOuter, _availableView, _msgForm, _searchForm;
    private ListView _msgListView;
    protected TextView _conversationTitle, _conversationStatus;
    protected XmppContact _contact = null;

    protected TAKChatDropDownReceiver.ChatDropDownReveiver _parent;

    static{
        mapping = new HashMap<String, TAKChatView>();
    }

    private boolean _bShowing = false;
    private boolean _searchActive = false;

    public static TAKChatView getChatView(String contactStr) {
        XmppContact contact = null;
        try{
            contact = TAKChatUtils.takChatComponent.getManager(ContactManager.class).getContactById(JidCreate.bareFrom(contactStr));
        } catch (XmppStringprepException e) {
            Log.e(TAG, "Error getting chat view!", e);
            return null;
        }

        if(contact == null){
            Log.w(TAG, "Unable to find contact for chat: " + contactStr);
            return null;
        }

        return getChatView(contact);
    }

    public static TAKChatView getChatView(XmppContact contact) {
        if(contact == null || contact.getId() == null){
            Log.w(TAG, "Unable to find chat for invalid contact");
            return null;
        }

        String contactStr = contact.getId().toString();
        boolean isConference = TAKChatUtils.isConference(contact);
        boolean bRefresh = false;
        TAKChatView chatView = null;
        synchronized (mapping) {
            chatView = mapping.get(contactStr);
            if(chatView == null) {
                Log.d(TAG, "Creating chat view for " + contact + ", conf=" + isConference);
                if(isConference) {
                    chatView = new TAKConferenceView();
                } else {
                    chatView = new TAKChatView();
                }

                bRefresh = true;
                mapping.put(contactStr, chatView);
            }else{
                Log.d(TAG, "getChatView found: " + chatView.getClass().toString() + ", for: " + contactStr + ", conf=" + isConference);
            }
        }

        if(bRefresh){
            //refresh outside of sync block
            chatView.refresh(contact);
        }

        return chatView;
    }

    public void showing() {
        Log.d(TAG, "showing");
        _bShowing = true;
        if(_chatAdapter == null){
            Log.w(TAG, "showing but adapter is empty");
            //TODO update 'read' column in DB for all messages in this convo?
            return;
        }else{
            _chatAdapter.showing();
        }
    }

    public void closing(){
        Log.d(TAG, "closing");
        toggleSearch(false);
        _bShowing = false;
    }

    public void toggleSearch(boolean active) {
        if (_searchActive != active) {
            _searchActive = active;
            if (_msgSearchTxt != null) {
                _msgSearchTxt.setText("");
                if (active)
                    _msgSearchTxt.requestFocus();
            }
            _msgForm.setVisibility(_searchActive ? View.INVISIBLE : View.VISIBLE);
            _searchForm.setVisibility(_searchActive ? View.VISIBLE : View.INVISIBLE);
            searchConversation("");
        }
    }

    public void toggleSearch() {
        toggleSearch(!_searchActive);
    }

    private void searchConversation(String terms) {
        if (_chatAdapter != null)
            _chatAdapter.search(terms);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        final LayoutInflater pluginInflater = (LayoutInflater)TAKChatUtils.pluginContext.
                getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        _rootView = pluginInflater.inflate(R.layout.takchat_conversation, container, false);
        _conversationHeaderOuter = _rootView.findViewById(R.id.conversationHeaderOuter);
        _msgEdittext = (KeyboardListenerEditText) _rootView.findViewById(R.id.messageEditText);
        //TODO when entering text, update chat state 'composing' when done set paused. then back to active
        //_component.getManager(MessageManager.class).setChatState(ChatState.composing);

        //listen for keyboard open/close
        _msgEdittext.setKeyboardListener(new KeyboardListenerEditText.KeyBoardListener() {
            @Override
            public void keyboardShowing(boolean bShowing) {
                Log.d(TAG, "keyboardShowing: " + bShowing);
                AtakBroadcast.getInstance().sendBroadcast(new Intent(
                        ActionBarReceiver.TOGGLE_ACTIONBAR).putExtra("show", !bShowing));

                if(!bShowing){
                    ActionBarView view = TAKChatUtils.takChatComponent.getDropDown().getToolbarView(R.layout.takchat_conversation_toolbar);
                    if(view != null){
                        Log.d(TAG, "Refreshing toolbar");
                        ActionBarReceiver.getInstance().setToolView(view);
                    }
                }
                _conversationHeaderOuter.setVisibility(bShowing ? View.GONE : View.VISIBLE);
            }
        });

        _msgForm = _rootView.findViewById(R.id.messageForm);
        _searchForm = _rootView.findViewById(R.id.searchForm);
        _msgForm.setVisibility(_searchActive ? View.INVISIBLE : View.VISIBLE);
        _searchForm.setVisibility(_searchActive ? View.VISIBLE : View.INVISIBLE);
        _msgListView = (ListView) _rootView.findViewById(R.id.msgListView);
        _conversationTitle = (TextView) _rootView.findViewById(R.id.conversationTitle);
        _conversationTitle.setText(_contact.getName());
        _conversationStatus = (TextView) _rootView.findViewById(R.id.conversationStatus);
        _conversationStatus.setText("inactive");
        _availableView = _rootView.findViewById(R.id.conversationAvailable);
        TAKContactProfileView.setStatus(null, _availableView);

        ImageButton cancelButton = (ImageButton) _rootView.findViewById(R.id.exitSearchButton);
        cancelButton.setOnClickListener(exitSearchListener);

        ImageButton sendButton = (ImageButton) _rootView.findViewById(R.id.sendMessageButton);
        sendButton.setOnClickListener(sendTextMessageListener);

        _msgSearchTxt = (EditText) _rootView.findViewById(R.id.messageSearchText);
        _msgSearchTxt.setFocusable(true);
        _msgSearchTxt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
            @Override
            public void afterTextChanged(Editable s) {
                if (_searchActive)
                    searchConversation(s.toString());
            }
        });
        _msgSearchTxt.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                InputMethodManager imm = (InputMethodManager) getActivity()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);

                if (b)
                    imm.showSoftInput(view,
                            InputMethodManager.SHOW_IMPLICIT);
                else
                    imm.hideSoftInputFromWindow(
                            view.getWindowToken(), 0);

            }
        });

        Marker marker = (_contact == null ? null : _contact.getMarker());

        ImageButton conversationContactPan = (ImageButton) _rootView.findViewById(R.id.conversationContactPan);
        if(marker != null) {
            _conversationTitle.setOnClickListener(_zoomListener);
            conversationContactPan.setOnClickListener(_zoomListener);
            conversationContactPan.setVisibility(ImageButton.VISIBLE);
        }else{
            conversationContactPan.setVisibility(ImageButton.GONE);
        }

        ImageView conversationImage = (ImageView) _rootView.findViewById(R.id.conversationImage);
        if(TAKChatUtils.isConference(_contact)){
            conversationImage.setImageResource(R.drawable.takchat_people);
            conversationImage.setColorFilter(Color.WHITE);
        } else if(marker != null) {
            ATAKUtilities.SetIcon(TAKChatUtils.mapView.getContext(), conversationImage, marker);
            conversationImage.setOnClickListener(_zoomListener);
        }else{
            conversationImage.setImageResource(R.drawable.takchat_xmpp_contact);
            conversationImage.setColorFilter(Color.WHITE);
        }

        refresh(_contact);

        getView();
        // ----Set autoscroll of listview when a new message arrives----//
        _msgListView.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
        _msgListView.setStackFromBottom(true);
        _msgListView.setAdapter(_chatAdapter);
        return _rootView;
    }

    private View.OnClickListener _zoomListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if(_contact != null && _contact.getMarker() != null){
                TAKChatUtils.mapView.getMapController().panTo(_contact.getMarker().getPoint(), true);
            }
        }
    };

    public void setParent(TAKChatDropDownReceiver.ChatDropDownReveiver ddr) {
        _parent = ddr;
    }


    private View.OnClickListener sendTextMessageListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            sendTextMessage();
        }
    };

    private View.OnClickListener exitSearchListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            toggleSearch(false);
        }
    };

    public void refresh(XmppContact contact){
        if(contact == null){
            Log.w(TAG, "Cannot refresh invalid contact");
            return;
        }

        Log.d(TAG, "refresh");
        this._contact = contact;

        if(_chatAdapter == null) {
            ArrayList<ChatMessage> chatlist = ChatDatabase.getInstance(TAKChatUtils.pluginContext)
                    .retrieveHistory(_contact.getId().asBareJid());
            Log.d(TAG, "creating chat adapter: " + _contact.toString() + ", msg count: " + chatlist.size());
            _chatAdapter = new ChatAdapter(chatlist);
            _chatAdapter.setDisplaySender(true);
        }

        if(_conversationTitle != null) {
            _conversationTitle.setText(contact.getName());
            _conversationTitle.invalidate();
        }
        if(_conversationStatus != null) {
            _conversationStatus.setText(contact.getStatus());
            _conversationStatus.invalidate();
        }
        //display R/Y/G icon for status also
        if(_availableView != null) {
            TAKContactProfileView.setStatus(contact, _availableView);
        }
        //TODO be sure each chat view gets callback when contact/connectivity status changes...
    }

    private void disconnected(){
        if(_conversationStatus != null) {
            _conversationStatus.setText(TAKChatUtils.getPluginString(R.string.disconnected));
            _conversationStatus.invalidate();
        }
        //display R/Y/G icon for status also
        if(_availableView != null) {
            TAKContactProfileView.setStatus(null, _availableView);
        }
    }

    public XmppContact getContact() {
        return _contact;
    }

    protected void sendTextMessage() {
        String message = _msgEdittext.getEditableText().toString();
        if(!TAKChatXMPP.getInstance().isConnected()){
            //TODO should we add to DB/UI with little red X?
            Log.d(TAG, "Not connected, cannot send: " + message);
            displayErrorToUser("Not connected, cannot send message");
            return;
        }

        if (!FileSystemUtils.isEmpty(message)) {
            Log.d(TAG, "Sending: " + message);

            final Message chatMessage;
            try {
                chatMessage = TAKChatUtils.createChat(_contact.getId().toString(), message);
                if(chatMessage == null){
                    Log.e(TAG, "Failed to create chat message for: " + _contact.getId());

                    _msgEdittext.setText("");
                    //TODO notify user...
                    return;
                }
            } catch (XmppStringprepException e) {
                Log.e(TAG, "Failed to create chat message for: " + _contact.getId(), e);
                _msgEdittext.setText("");
                //TODO notify user...
                return;
            }

            //TODO need to store this stanza ID with the message in local UI and DB, so they can
            //later be mapped during msg delivery receipt
            String stanzaId = chatMessage.getStanzaId();
            Log.d(TAG, "Sending chat with stanza id: " + stanzaId);

            _msgEdittext.setText("");

            //add to UI
            ChatMessage wrapper = new ChatMessage(chatMessage);
            //TODO these are parsed for UI, and again in ChatDatabase.onMessageSent for DB
            //storage, could optimized to only do once, with some refactoring of ChatDatabase
            wrapper.setLocations(MessageLocationLink.getLocations(message));
            add(wrapper);

            //send to server
            TAKChatUtils.takChatComponent.getManager(MessageManager.class).sendChat(wrapper);
        }
    }

    protected void displayErrorToUser(String msg) {
        Toast.makeText(
                TAKChatUtils.mapView.getContext(),
                msg,
                Toast.LENGTH_LONG).show();
    }


    protected void add(final ChatMessage message) {
        if(_chatAdapter == null){
            Log.w(TAG, "UI Adapter not ready, discarding message: " + message);
            return;
        }

        Log.d(TAG, "Adding message to UI: " + message.toString());
        TAKChatUtils.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                _chatAdapter.add(message);
            }
        });
    }


    @Override
    public boolean onMessage(Stanza stanza) {
        if(stanza == null || stanza.getFrom() == null){
            Log.w(TAG, "onMessage message invalid: " + (stanza == null ? "" : stanza.toString()));
            //TODO notify user?
            return false;
        }

        if(!(stanza instanceof Message)){
            //Note, currently only invoked via TAKContactsView.onMessage
            Log.w(TAG, "onMessage ignoring incoming stanza: " + stanza);
            return false;
        }

        Message message = (Message)stanza;
        if(message == null){
            Log.w(TAG, "onMessageReceived message invalid: " + (message == null ? "" : message.toString()));
            //TODO notify user?
            return false;
        }

        Log.d(TAG, "processMessage incoming message for " + this._contact.toVerboseString() + ": " + message);

        String body = TAKChatUtils.getBody(message);
        if ((message.getType() == Message.Type.chat || message.getType() == Message.Type.groupchat)
                && !FileSystemUtils.isEmpty(body)) {

            ChatMessage wrapper = new ChatMessage(message);
            if(!wrapper.hasLocations()){
                //TODO these are parsed for UI, and again in ChatDatabase.onMessageSent for DB
                //storage, could optimized to only do once, with some refactoring of ChatDatabase
                wrapper.setLocations(MessageLocationLink.getLocations(body));
            }
            add(wrapper);

            if(_bShowing){
                Log.d(TAG, "onMessageReceived showing: " + message.toString());
                wrapper.setRead(true);
                TAKChatUtils.takChatComponent.getManager(MessageUnreadManager.class).messageRead(wrapper);
            }else {
                Log.d(TAG, "onMessageReceived not showing: " + message.toString());
            }
        } else if (message.getType() == Message.Type.headline
                && !FileSystemUtils.isEmpty(body)) {

            //TODO headline/attention can come from another user. Assume it should display
            //in the chat view for that user. Also see HeadlineListener
            Log.d(TAG, "onMessageReceived headline: " + message.toString());
            add(new ChatMessage(message));
        } else if (message.getType() == Message.Type.normal) {
            Log.d(TAG, "Ignoring normal: " + message.toXML());
        } else if (message.getType() == Message.Type.error) {
            Log.w(TAG, "Ignoring error: " + message.toXML());
        } else{
            if(message.hasExtension(ChatStateExtension.NAMESPACE)) {
                final ChatStateExtension chatStateExtension = (ChatStateExtension) message.getExtension(ChatStateExtension.NAMESPACE);
                if (_conversationStatus != null) {
                    TAKChatUtils.runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            _conversationStatus.setText(chatStateExtension.getChatState().toString());
                            _conversationStatus.invalidate();
                        }
                    });
                }
            } else {
                Log.d(TAG, "Ignoring message of type: " + message.getType());
            }
        }

        return true;
    }

    @Override
    public void dispose() {
        if(_parent != null)
            _parent.closeDropDown();
        if(_chatAdapter != null)
            _chatAdapter.dispose();
    }

    public void showConversationOptions(boolean bIncludeChat) {
        showConversationOptions(_contact, bIncludeChat, _parent);
    }

    public static void showConversationOptions(final XmppContact contact, final boolean bIncludeChat, final DropDownReceiver parent) {
        if(contact == null){
            Log.w(TAG, "Cannot show conversation options without contact");
            return;
        }

        if(TAKChatUtils.isConference(contact)){
            TAKConferenceView.showConferenceOptions(null, (XmppConference) contact, bIncludeChat);
        }else {
            final String[] options = TAKChatUtils.pluginContext.getResources().getStringArray(
                    bIncludeChat ? R.array.contact_menu_chat_array : R.array.contact_menu_array);
            AlertDialog.Builder builder = new AlertDialog.Builder(TAKChatUtils.mapView.getContext());
            builder.setIcon(com.atakmap.app.R.drawable.xmpp_icon);
            builder.setTitle("Contact Options");
            builder.setItems(options, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case 0:  //Contact Info
                            TAKContactProfileView.getInstance().showContactInfo(contact);
                            break;

                        //TODO uncomment and re-align switch labels once we implement groups
                        // case 1:  //Edit Groups
                        //    TAKChatView.showEditGroupsDialog();
                        //    break;
                        case 1:  //Delete
                            dialog.dismiss();
                            if(!TAKChatXMPP.getInstance().isConnected()){
                                Log.d(TAG, "Not connected, cannot delete");
                                Toast.makeText(TAKChatUtils.mapView.getContext(), "Not connected, cannot delete contact", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            new AlertDialog.Builder(TAKChatUtils.mapView.getContext())
                                    .setTitle("Remove contact: " + contact.getName())
                                    .setIcon(com.atakmap.app.R.drawable.xmpp_icon)
                                    .setMessage("Remove contact from buddy list?")
                                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            if (TAKChatUtils.takChatComponent.getManager(ContactManager.class).removeBuddy(contact)) {
                                                Log.w(TAG, "Failed to remove buddy: " + contact.toVerboseString());
                                                if(parent != null)
                                                    parent.closeDropDown();
                                            }
                                        }
                                    })
                                    .setNegativeButton("Cancel", null)
                                    .show();
                            break;
                        case 2:  //Re-Authorize
                            dialog.dismiss();
                            if(!TAKChatXMPP.getInstance().isConnected()){
                                Log.d(TAG, "Not connected, cannot re-authorize");
                                Toast.makeText(TAKChatUtils.mapView.getContext(), "Not connected, cannot re-authorize", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            new AlertDialog.Builder(TAKChatUtils.mapView.getContext())
                                    .setTitle("Re-request authorization?")
                                    .setIcon(com.atakmap.app.R.drawable.xmpp_icon)
                                    .setMessage("Re-request authorization from " + contact.getName() + "?")
                                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            TAKChatUtils.takChatComponent.getManager(ContactManager.class).requestAuthorization(contact);
                                        }
                                    })
                                    .setNegativeButton("Cancel", null)
                                    .show();
                            break;
                        case 3:  //Chat & Close All Chats
                            if(bIncludeChat) {
                                TAKChatView.showConversation(contact);
                            }else {
                                TAKChatView.reset();
                            }
                            break;
                        default:
                    }
                }
            });
            builder.create().show();
        }
    }

    public static void showAddContactPrompt() {
        if(!TAKChatXMPP.getInstance().isConnected()){
            Log.d(TAG, "Not connected, cannot add contact");
            Toast.makeText(TAKChatUtils.mapView.getContext(), "Not connected, cannot add contact", Toast.LENGTH_SHORT).show();
            return;
        }

        final BareJid me = TAKChatUtils.getUsernameBare();
        if(me == null){
            Log.d(TAG, "JID not set, cannot add contact");
            Toast.makeText(TAKChatUtils.mapView.getContext(), "Not connected, cannot add contact", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(TAKChatUtils.mapView.getContext());
        LayoutInflater inflater = (LayoutInflater)TAKChatUtils.pluginContext.
                getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View createContactView = inflater.inflate(R.layout.takchat_create_contact, null);
        AlertDialog dialog = builder.setView(createContactView)
                .setTitle("Add contact")
                .setIcon(com.atakmap.app.R.drawable.xmpp_icon)
                .setPositiveButton("Create", null)
                .setNeutralButton("Cancel", null)
                .create();
        final EditText localNameET = (EditText) createContactView.findViewById(R.id.create_contact_local);
        final EditText domainNameET = (EditText) createContactView.findViewById(R.id.create_contact_domain);
        domainNameET.setText(me.getDomain().toString());

        //TODO: THIS IS CODED WRONG - USE AN ADAPTER OR SOMETHING!
        final LinearLayout checkBoxList = (LinearLayout) createContactView.findViewById(R.id.check_box_list);
        for(XmppConference conference : TAKChatUtils.takChatComponent.getManager(ContactManager.class).getConferences()) {
            CheckBox checkBox = new CheckBox(TAKChatUtils.pluginContext);
            checkBox.setText(conference.getId());
            checkBoxList.addView(checkBox);
        }

        final EditText aliasET = (EditText) createContactView.findViewById(R.id.create_contact_name);
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(final DialogInterface diag) {
                ((AlertDialog)diag).getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String localName = localNameET.getText().toString();
                        String domainName = domainNameET.getText().toString();
                        String alias = aliasET.getText().toString();

                        if (FileSystemUtils.isEmpty(localName)) {
                            Toast.makeText(TAKChatUtils.mapView.getContext(), "Please enter a valid User Name", Toast.LENGTH_LONG).show();
                            return;
                        }

                        if (FileSystemUtils.isEmpty(domainName)) {
                            Toast.makeText(TAKChatUtils.mapView.getContext(), "Please enter a valid Server Domain", Toast.LENGTH_LONG).show();
                            return;
                        }

                        if (FileSystemUtils.isEquals(localName, me.getLocalpartOrNull().toString()) && FileSystemUtils.isEquals(domainName, me.getDomain().toString())) {
                            Toast.makeText(TAKChatUtils.mapView.getContext(), "Please enter a buddy's username, not your own...", Toast.LENGTH_LONG).show();
                            return;
                        }

                        try {
                            EntityBareJid jid = JidCreate.entityBareFrom(Localpart.from(localName), Domainpart.from(domainName));
                            if (jid == null || !XmppStringUtils.isBareJid(jid.toString())) {
                                Toast.makeText(TAKChatUtils.mapView.getContext(), "Please enter a valid User ID", Toast.LENGTH_LONG).show();
                                return;
                            }

                            XmppContact existing = TAKChatUtils.takChatComponent.getManager(ContactManager.class).getContactById(jid);
                            if(existing != null){
                                Toast.makeText(TAKChatUtils.mapView.getContext(), "Contact is already on your roster", Toast.LENGTH_LONG).show();
                                return;
                            }

                            if(FileSystemUtils.isEmpty(alias)){
                                //default alias to username
                                alias = localName;
                            }

                            diag.cancel();

                            ContactManager.addBuddy(jid, alias);
                        } catch (XmppStringprepException e) {
                            Log.e(TAG, "Error adding contact", e);
                            Toast.makeText(TAKChatUtils.mapView.getContext(), "Invalid chat username", Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });
        dialog.show();
        //display another dialog to allow entry of the contacts id and alias (if desired)
        //Allow for creation of a new group
    }

    public static void showEditGroupsDialog() {
        //TODO
        Log.d(TAG, "showEditGroupsDialog: Not implemented yet");
    }

    public static void showConversation(XmppContact contact) {
        Log.d(TAG, "showConversation: " + contact.toVerboseString() + ", conf=" + TAKChatUtils.isConference(contact));
        Intent openChatIntent = new Intent(TAKChatDropDownReceiver.SHOW_CHAT);
        openChatIntent.putExtra("bareJid", contact.getId().toString());
        AtakBroadcast.getInstance().sendBroadcast(openChatIntent);
    }

    @Override
    public boolean onConnected() {
        Log.d(TAG, "onConnected");
        return true;
    }

    @Override
    public boolean onStatus(boolean bSuccess, String status) {
        return true;
    }

    @Override
    public boolean onDisconnected() {
        Log.d(TAG, "onDisconnected");

        TAKChatUtils.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                disconnected();
            }
        });
        return true;
    }


    @Override
    public boolean onPresenceChanged(Presence presence) {
        //TODO be sure this refresh has the updated state available
        TAKChatUtils.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                refresh(_contact);
            }
        });
        return true;
    }

    @Override
    public boolean onContactSizeChanged() {
        //no-op
        return false;
    }

    @Override
    public void onMessageSent(ChatMessage message) {
        //no-op for now. TAKChatView does an add() at send time..
        Log.d(TAG, "onMessageSent ignored: " + message.toString());
    }

    @Override
    public void onDeliveryReceipt(Jid from, Jid to, String deliveryReceiptId, Stanza stanza) {
        if(_chatAdapter == null){
            //TODO still process message if UI has not been opened yet
            Log.w(TAG, "UI Adapter not ready, discarding receipt: " + deliveryReceiptId);
            return;
        }

        Log.d(TAG, "Adding receipt to UI: " + stanza.toString());
        _chatAdapter.onDeliveryReceipt(from, to, deliveryReceiptId, stanza);
    }

    @Override
    public void onDeliveryError(ChatMessage message) {
        if(_chatAdapter == null){
            //TODO still process message if UI has not been opened yet
            Log.w(TAG, "UI Adapter not ready, discarding error: " + message);
            return;
        }

        Log.d(TAG, "Adding error to UI: " + message.toString());
        _chatAdapter.onDeliveryError(message);
    }

    public static void showSwitchChatPrompt() {
        //TODO need proper way to get list of recent chats
        final List<XmppContact> toDisplay = new ArrayList<XmppContact>();

        synchronized (mapping) {
            for (TAKChatView chat : mapping.values()) {
                toDisplay.add(chat.getContact());
            }
        }

        //be sure we have some contacts to display
        if(FileSystemUtils.isEmpty(toDisplay)){
            Log.d(TAG, "No recent chats");
            Toast.makeText(TAKChatUtils.mapView.getContext(), "No recent chats...", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            //create a dummy row to allow user to jump to contact list
            BareJid displayRosterJid = JidCreate.bareFrom("DisplayRoster@temp");
            XmppConference diplayRoster = new XmppConference("Display Roster", displayRosterJid, null);
            diplayRoster.setAvailable(true);
            toDisplay.add(0, diplayRoster);
        } catch (XmppStringprepException e) {
            Log.w(TAG, "Failed to add display roster", e);
            return;
        }

        //now sort and display for user selection
        //TODO sort by most recently chatted, instead?
        Collections.sort(toDisplay, new XmppContactComparator());

        XmppContact[] cArray = new XmppContact[toDisplay.size()];
        toDisplay.toArray(cArray);
        final ContactSelectAdapter cAdapter = new ContactSelectAdapter(cArray);

        LayoutInflater inflater = LayoutInflater.from(TAKChatUtils.pluginContext);
        LinearLayout layout = (LinearLayout) inflater.inflate(
                R.layout.takchat_contact_select_list, null);
        ListView listView = (ListView) layout.findViewById(R.id.takchat_contact_list);
        listView.setAdapter(cAdapter);

        AlertDialog.Builder b = new AlertDialog.Builder(TAKChatUtils.mapView.getContext());
        b.setIcon(com.atakmap.app.R.drawable.xmpp_icon);
        b.setTitle("Select recent chat");
        b.setView(layout);
        b.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        final AlertDialog bd = b.create();
        cAdapter.setOnItemClickListener(new ContactSelectAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(ContactSelectAdapter adapter, XmppContact contact, int position) {
                bd.dismiss();
                if(position == 0){
                    Log.d(TAG, "Display roster");
                    Intent openChatIntent = new Intent(TAKChatDropDownReceiver.SHOW_CONTACT_LIST);
                    AtakBroadcast.getInstance().sendBroadcast(openChatIntent);
                    return;
                }

                if (contact == null) {
                    Toast.makeText(TAKChatUtils.mapView.getContext(), "Failed to open chat...",
                            Toast.LENGTH_SHORT).show();
                    Log.w(TAG, "Failed to open chat");
                    return;
                }

                //now open chat
                Log.d(TAG, "switch chat: " + contact.toVerboseString() + ", conf=" + TAKChatUtils.isConference(contact));
                Intent openChatIntent = new Intent(TAKChatDropDownReceiver.SHOW_CHAT);
                openChatIntent.putExtra("bareJid", contact.getId().toString());
                AtakBroadcast.getInstance().sendBroadcast(openChatIntent);
            }
        });

        bd.show();
    }

    /**
     * Dispose of all the ongoing chat views
     */
    public static void reset() {
        Log.d(TAG, "reset");

        synchronized (mapping) {
            for (TAKChatView view : mapping.values()) {
                view.dispose();
            }

            mapping.clear();
        }
    }

    /**
     * Dispose of the specified chat view
     */
    public static void remove(XmppContact contact) {
        if(contact == null || contact.getId() == null){
            Log.d(TAG, "cannot remove invalid contact");
            return;
        }

        TAKChatView chatView = null;
        synchronized (mapping) {
            chatView = mapping.get(contact.getId().toString());
            if (chatView == null) {
                Log.d(TAG, "Cannot remove: " + contact.toString());
            }else{
                Log.d(TAG, "remove: " + contact.toString());
                mapping.remove(chatView);
            }
        }

        if(chatView != null){
            chatView.dispose();
        }
    }
}
