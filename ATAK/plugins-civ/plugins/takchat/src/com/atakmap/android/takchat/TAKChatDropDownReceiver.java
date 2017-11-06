
package com.atakmap.android.takchat;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import com.atakmap.android.data.DataMgmtReceiver;
import com.atakmap.android.dropdown.DropDown;
import com.atakmap.android.dropdown.DropDownManager;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.takchat.adapter.ContactListAdapter;
import com.atakmap.android.takchat.data.ChatDatabase;
import com.atakmap.android.takchat.data.HeadlineListener;
import com.atakmap.android.takchat.data.XmppConference;
import com.atakmap.android.takchat.data.XmppContact;
import com.atakmap.android.takchat.net.ConferenceManager;
import com.atakmap.android.takchat.net.ContactManager;
import com.atakmap.android.takchat.net.TAKChatXMPP;
import com.atakmap.android.takchat.net.VCardManager;
import com.atakmap.android.takchat.plugin.R;
import com.atakmap.android.takchat.view.TAKChatView;
import com.atakmap.android.takchat.view.TAKConferenceView;
import com.atakmap.android.takchat.view.TAKContactProfileView;
import com.atakmap.android.takchat.view.TAKContactsView;
import com.atakmap.android.tools.ActionBarReceiver;
import com.atakmap.android.tools.ActionBarView;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;

import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

/**
 * Manages the XMPP chat dropdown UI within ATAK
 * Processes <code>{@link Intent}</code>
 *
 * @author byoung
 */
public class TAKChatDropDownReceiver extends DropDownReceiver{

    public static final String TAG = "TAKChatDropDownReceiver";

    public static final String SHOW_CONTACT_LIST = "com.atakmap.android.takchat.SHOW_CONTACT_LIST";
    public static final String SHOW_CHAT = "com.atakmap.android.takchat.SHOW_CHAT";
    public static final String SHOW_HEADLINE = "com.atakmap.android.takchat.SHOW_HEADLINE";
    public static final String SHOW_PROFILE = "com.atakmap.android.takchat.SHOW_PROFILE";
    public static final String SHOW_CONF_PROFILE = "com.atakmap.android.takchat.SHOW_CONF_PROFILE";
    public static final String AVATAR_CAPTURED = "com.atakmap.android.takchat.AVATAR_CAPTURED";
    public static final String CLEAR_HISTORY = "com.atakmap.android.takchat.CLEAR_HISTORY";
    public static final String OPEN_SEARCH = "com.atakmap.android.takchat.OPEN_SEARCH";
    public static final String AVATAR_CAPTURED_FILEPATH = "filepath";

    private TAKChatView _chatView;
    private TAKContactsView _contactView;
    private ContactListAdapter _contactAdapter;
    private static SharedPreferences _prefs;
    private TAKChatView _activeChat;
    private boolean _searchActive = false;
    private EditText _searchText;

    public TAKChatDropDownReceiver(SharedPreferences prefs) {
        super(TAKChatUtils.mapView);
        _prefs = prefs;
        _activeChat = null;

        //initialize primary/contact view
        getView();

        _contactAdapter = new ContactListAdapter(_prefs);

        ListView list = (android.widget.ListView) _contactView.findViewById(R.id.chat_presence_list);
        list.setAdapter(_contactAdapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                XmppContact contact = (XmppContact) _contactAdapter.getItem(position);
                TAKChatView.showConversation(contact);

            }
        });
        list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                final XmppContact contact = (XmppContact) _contactAdapter.getItem(position);
                TAKChatView.showConversationOptions(contact, true, null);
                return true;
            }
        });
    }

    public void disposeImpl() {
        if(_contactAdapter != null){
            _contactAdapter.dispose();
        }
    }

    public class ChatDropDownReveiver extends DropDownReceiver {
        public ChatDropDownReveiver() {
            super(TAKChatUtils.mapView);
        }

        public void show(final TAKChatView cf) {
            ActionBarReceiver.getInstance().setToolView(getToolbarView(R.layout.takchat_conversation_toolbar));

            showDropDown(cf, HALF_WIDTH, FULL_HEIGHT, FULL_WIDTH, HALF_HEIGHT, false, false, new DropDown.OnStateListener() {
                @Override
                public void onDropDownSelectionRemoved() {
                }

                @Override
                public void onDropDownClose() {
                    Log.d(TAG, "onDropDownClose");
                    //ActionBarReceiver.getInstance().setToolView(null);
                    cf.closing();
                }

                @Override
                public void onDropDownSizeChanged(double width, double height) {
                }

                @Override
                public void onDropDownVisible(boolean v) {
                    Log.d(TAG, "onDropDownVisible: " + v);
                    if (v) {
                        ActionBarReceiver.getInstance().setToolView(getToolbarView(R.layout.takchat_conversation_toolbar));
                    }else{
                        cf.closing();
                    }
                }
            });
            cf.setParent(this);
        }

        @Override
        protected void disposeImpl() {

        }

        @Override
        public void onReceive(Context context, Intent intent) {

        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (SHOW_CONTACT_LIST.equals(intent.getAction())) {
             Log.d(TAG, "showing contact list");
            setAssociationKey(TAKChatMapComponent.TAKCHAT_PREFERENCE);
            ActionBarReceiver.getInstance().setToolView(getToolbarView(R.layout.takchat_contactlist_toolbar));
            setRetain(true);
            showDropDown(getView(), THIRD_WIDTH, FULL_HEIGHT, FULL_WIDTH, HALF_HEIGHT, false, false, new DropDown.OnStateListener() {
                        @Override
                        public void onDropDownSelectionRemoved() {
                        }

                        @Override
                        public void onDropDownClose() {
                            //ActionBarReceiver.getInstance().setToolView(null);
                        }

                        @Override
                        public void onDropDownSizeChanged(double width, double height) {
                        }

                        @Override
                        public void onDropDownVisible(boolean v) {
                            if (v) {
                                ActionBarReceiver.getInstance().setToolView(getToolbarView(R.layout.takchat_contactlist_toolbar));
                            } else {
                                toggleSearch(false);
                            }
                        }
                    });
        } else if (SHOW_CHAT.equals(intent.getAction())) {
            final String contactStr = intent.getStringExtra("bareJid");
            if(FileSystemUtils.isEmpty(contactStr)){
                Log.w(TAG, "Cannot show chat without jid");
                return;
            }

            Log.d(TAG, "showing chat for " + contactStr);
            XmppContact contact = null;
            try{
                contact = TAKChatUtils.takChatComponent.getManager(ContactManager.class).getContactById(JidCreate.entityBareFrom(contactStr));
            } catch (XmppStringprepException e) {
                Log.e(TAG, "Error getting chat view: " + contactStr, e);
                Toast.makeText(TAKChatUtils.mapView.getContext(), "Invalid chat username: " + contactStr, Toast.LENGTH_LONG).show();
                return;
            }

            if(contact == null){
                //not a buddy, prompt user to add buddy
                Log.w(TAG, "Unable to find contact for chat view: " + contactStr);
                AlertDialog.Builder builder = new AlertDialog.Builder(TAKChatUtils.mapView.getContext());
                builder.setTitle("Add User")
                        .setIcon(com.atakmap.app.R.drawable.xmpp_icon)
                        .setMessage(contactStr + " is not currently in your contact list. Add user now?")
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                try {
                                    ContactManager.addBuddy(JidCreate.entityBareFrom(contactStr), contactStr);
                                } catch (XmppStringprepException e) {
                                    Log.w(TAG, "Failed to add buddy", e);
                                }
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                return;
            }else {
                //found contact, open chat view
                TAKChatView tcv = TAKChatView.getChatView(contact);
                if (tcv != null) {
                    ChatDropDownReveiver cddr = new ChatDropDownReveiver();
                    cddr.setRetain(true);
                    //TODO when to clear/null out _activeChat?
                    _activeChat = tcv;
                    setAssociationKey(TAKChatMapComponent.TAKCHAT_PREFERENCE);
                    cddr.show(tcv);
                    tcv.showing();
                } else {
                    Toast.makeText(TAKChatUtils.mapView.getContext(), "Failed to initiate chat with: " + contactStr, Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Error retrieving chat view!");
                }
            }
        } else if(SHOW_HEADLINE.equals(intent.getAction())){
            HeadlineListener.dialog(intent);
        } else if(SHOW_PROFILE.equals(intent.getAction())){
            String bareJid = intent.getStringExtra("bareJid");
            if(FileSystemUtils.isEmpty(bareJid)){
                Log.w(TAG, "Cannot show profile without jid");
                return;
            }

            TAKContactProfileView.getInstance().showContactInfo(bareJid);
        } else if(SHOW_CONF_PROFILE.equals(intent.getAction())){
            String bareJid = intent.getStringExtra("bareJid");
            if(FileSystemUtils.isEmpty(bareJid)){
                Log.w(TAG, "Cannot show conf profile without jid");
                return;
            }

            TAKConferenceView.displayConferenceInfo(bareJid);
        } else if(AVATAR_CAPTURED.equals(intent.getAction())){
            TAKChatUtils.takChatComponent.getManager(VCardManager.class)
                    .setMyAvatar(intent.getStringExtra(TAKChatDropDownReceiver.AVATAR_CAPTURED_FILEPATH));
        } else if(DataMgmtReceiver.ZEROIZE_CONFIRMED_ACTION.equals(intent.getAction())){
            Log.d(TAG, "Clearing chat database");
            ChatDatabase.getInstance(TAKChatUtils.pluginContext).deleteAll();
        } else if(CLEAR_HISTORY.equals(intent.getAction())){
            Log.d(TAG, "Clearing chat history");
            //Note if server resync's data it may be reloaded up next app restart
            Toast.makeText(TAKChatUtils.mapView.getContext(), "Clearing chat history...", Toast.LENGTH_SHORT).show();
            ChatDatabase.getInstance(TAKChatUtils.pluginContext).deleteMessages();
            refreshContactList(true, false, false);
        } else if (OPEN_SEARCH.equals(intent.getAction()))
            toggleSearch(true);
    }

    public ActionBarView getToolbarView(int resourceId) {
        LayoutInflater inflater = LayoutInflater.from(TAKChatUtils.pluginContext);
        ActionBarView toolbar = (ActionBarView) inflater.inflate(resourceId, null);

        ImageButton contactListAdd = (ImageButton) toolbar.findViewById(R.id.takchat_contactlist_toolbar_add);
        if (contactListAdd != null) {
            contactListAdd.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    if(!TAKChatXMPP.getInstance().isConnected()){
                        Log.d(TAG, "Not connected, cannot add contact");
                        Toast.makeText(TAKChatUtils.mapView.getContext(), "Not connected, cannot add contact", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Log.d(TAG, "Action bar contactListAdd");
                    new AlertDialog.Builder(getMapView().getContext())
                            .setIcon(com.atakmap.app.R.drawable.xmpp_icon)
                            .setTitle(TAKChatUtils.getPluginString(R.string.app_name) + " Add...")
                            .setMessage("Add Contact or Conference?")
                            .setNeutralButton("Contact", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int i) {
                                    dialog.dismiss();
                                    TAKChatView.showAddContactPrompt();
                                }
                            })
                            .setPositiveButton("Conference", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int i) {
                                    dialog.dismiss();
                                    TAKConferenceView.showAddConferencePrompt(_prefs);
                                }
                            })
                            .setNegativeButton("Recent", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int i) {
                                    dialog.dismiss();
                                    TAKChatView.showSwitchChatPrompt();
                                }
                            })
                            .show();
                }
            });
        }

        ImageButton contactSearch = (ImageButton) toolbar.findViewById(R.id.takchat_contactlist_search);
        if (contactSearch != null) {
            contactSearch.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleSearch();
                }
            });
        }

        ImageButton contactListVcard = (ImageButton) toolbar.findViewById(R.id.takchat_contactlist_toolbar_vcard);
        if (contactListVcard != null) {
            contactListVcard.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    Log.d(TAG, "Action bar contactListVcard");
                    //view my profile
                    TAKContactProfileView.getInstance().showContactInfo(
                            TAKChatUtils.takChatComponent.getManager(VCardManager.class).getMyContact());
                }
            });
        }

        ImageButton contactListOptions = (ImageButton) toolbar.findViewById(R.id.takchat_contactlist_toolbar_options);
        if (contactListOptions != null) {
            contactListOptions.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d(TAG, "Action bar contactListOptions");
                    TAKContactsView.showContactListOptions(_prefs, _contactAdapter);
                }
            });
        }

        ImageButton conversationSwitch = (ImageButton) toolbar.findViewById(R.id.takchat_conversation_toolbar_switch);
        if (conversationSwitch != null) {
            conversationSwitch.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    if(_activeChat == null){
                        Log.w(TAG, "No active chat");
                        return;
                    }
                    Log.d(TAG, "Action bar conversationSwitch");
                    TAKChatView.showSwitchChatPrompt();
                }
            });
        }

        ImageButton conversationVcard = (ImageButton) toolbar.findViewById(R.id.takchat_conversation_toolbar_vcard);
        if (conversationVcard != null) {
            conversationVcard.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    if(_activeChat == null || _activeChat.getContact() == null){
                        Log.w(TAG, "No active chat");
                        return;
                    }

                    XmppContact contact = _activeChat.getContact();
                    if(TAKChatUtils.isConference(contact)){
                        Log.d(TAG, "Action bar conference info: " + contact.toString());
                        TAKConferenceView.displayConferenceInfo((XmppConference)contact);
                    }else {
                        Log.d(TAG, "Action bar conversationVcard: " + contact.toString());
                        TAKContactProfileView.getInstance().showContactInfo(_activeChat.getContact());
                    }

                }
            });
        }

        ImageButton convSearch = (ImageButton) toolbar.findViewById(
                R.id.takchat_conversation_toolbar_search);
        if (convSearch != null) {
            convSearch.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(_activeChat == null){
                        Log.w(TAG, "No active chat");
                        return;
                    }

                    Log.d(TAG, "Action bar conversation search");
                    _activeChat.toggleSearch();
                }
            });
        }

        ImageButton conversationOptions = (ImageButton) toolbar.findViewById(R.id.takchat_conversation_toolbar_options);
        if (conversationOptions != null) {
            conversationOptions.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    if(_activeChat == null){
                        Log.w(TAG, "No active chat");
                        return;
                    }

                    Log.d(TAG, "Action bar conversationOptions");
                    _activeChat.showConversationOptions(false);
                }
            });
        }

        return toolbar;
    }

    TAKContactsView getView() {
        if(_contactView != null) {
            //_contactAdapter.redrawList();
            _contactView.refresh();
            return _contactView;
        }

        final LayoutInflater inflater = (LayoutInflater)TAKChatUtils.pluginContext.
                getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        _contactView = (TAKContactsView) inflater.inflate(R.layout.takchat_contactlist, null);
        _contactView.refresh();
        return _contactView;
    }

    public ContactListAdapter getAdapter() {
        return _contactAdapter;
    }

    public void refreshContactList(final boolean bClearMessages, final boolean bClearContacts, final boolean bReJoinConferences){
        if(bClearMessages){
            Log.d(TAG, "reset clear messages");
            DropDownManager.getInstance().closeAllDropDowns();

            TAKChatView.reset();
        }

        if(bClearContacts) {
            Log.d(TAG, "reset clear contacts");

            //reset contact list mgr and UI, remove all ongoing chats
            //Note contact list will be reloaded via server roster
            TAKChatUtils.takChatComponent.getManager(ContactManager.class).reset();

            //now reload this user's conferences
            TAKChatUtils.takChatComponent.getManager(ConferenceManager.class).loadConferences();
            if(TAKChatXMPP.getInstance().isConnected()) {
                TAKChatUtils.takChatComponent.getManager(ConferenceManager.class).joinAll(bReJoinConferences);
            }
        }

        Log.d(TAG, "refreshContactList");
        if(_contactView != null)
            _contactView.refresh();

        if(_contactAdapter != null)
            _contactAdapter.redrawList();

        updateView();
    }

    private void updateView() {
        if (_contactView != null) {
            // Search function
            View searchLayout = _contactView.findViewById(R.id.contact_search_layout);
            if (searchLayout == null)
                return;
            final ImageButton cancelSearchBtn = (ImageButton) _contactView.findViewById(
                    R.id.contact_search_cancel_btn);
            searchLayout.setVisibility(_searchActive ? View.VISIBLE : View.GONE);
            cancelSearchBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleSearch(false);
                }
            });
            if (_searchText == null) {
                _searchText = (EditText) _contactView.findViewById(R.id.contact_search_text);
                _searchText.setFocusable(true);
                _searchText.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }
                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                    }
                    @Override
                    public void afterTextChanged(Editable s) {
                        if (_searchActive)
                            searchContacts(s.toString());
                    }
                });
                _searchText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View view, boolean b) {
                        InputMethodManager imm = (InputMethodManager) getMapView().getContext()
                                .getSystemService(Context.INPUT_METHOD_SERVICE);

                        if (b)
                            imm.showSoftInput(view,
                                    InputMethodManager.SHOW_IMPLICIT);
                        else
                            imm.hideSoftInputFromWindow(
                                    view.getWindowToken(), 0);

                    }
                });
            }
        }
    }

    public void toggleSearch(boolean active) {
        if (_searchActive != active) {
            _searchActive = active;
            refreshContactList(false, false, false);
            if (_searchText != null && active) {
                _searchText.setText("");
                _searchText.requestFocus();
            }
            searchContacts("");
        }
    }

    public void toggleSearch() {
        toggleSearch(!_searchActive);
    }

    private void searchContacts(String terms) {
        if (_contactAdapter != null) {
            _contactAdapter.search(terms);
        }
    }
}
