package com.atakmap.android.takchat.view;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.takchat.TAKChatDropDownReceiver;
import com.atakmap.android.takchat.TAKChatMapComponent;
import com.atakmap.android.takchat.TAKChatUtils;
import com.atakmap.android.takchat.adapter.ContactListAdapter;
import com.atakmap.android.takchat.data.ChatMessage;
import com.atakmap.android.takchat.data.ContactListener;
import com.atakmap.android.takchat.data.DeliveryReceiptListener;
import com.atakmap.android.takchat.data.MessageListener;
import com.atakmap.android.takchat.data.XmppContact;
import com.atakmap.android.takchat.net.ConnectionManager;
import com.atakmap.android.takchat.net.ConnectivityListener;
import com.atakmap.android.takchat.net.ContactManager;
import com.atakmap.android.takchat.net.TAKChatXMPP;
import com.atakmap.android.takchat.net.VCardManager;
import com.atakmap.android.takchat.plugin.R;
import com.atakmap.android.util.MRUStringCache;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.coremap.log.Log;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Stanza;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.Jid;

import java.util.List;

/**
 * UI for a list of contacts
 * Also serves to route messages/data to the appropriate TAKChatView (as they do not all
 * individually register to listen for events, though they implement interfaces)
 *
 * Created by scallya on 7/12/2016.
 */
public class TAKContactsView extends RelativeLayout implements MessageListener, ConnectivityListener, ContactListener, DeliveryReceiptListener {

    private static final String TAG = "TAKContactsView";
    private static final String STATUS_CACHE_KEY = "takchat_status_cache";
    private static final int MAX_STATUS_LENGTH = 30;

    public TAKContactsView(Context context) {
        super(context);
    }

    public TAKContactsView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TAKContactsView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void refresh(){
        VCardManager mgr = TAKChatUtils.takChatComponent.getManager(VCardManager.class);
        if(mgr == null)
            return;

        Log.d(TAG, "refresh");
        final XmppContact self = mgr.getMyContact();
        TextView aliasView = (TextView) findViewById(R.id.contact_list_alias);
        aliasView.setText(TAKChatUtils.getUsernameLocalPart());
        aliasView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                TAKContactProfileView.getInstance().showContactInfo(self);
            }
        });
        TextView statusView = (TextView) findViewById(R.id.contact_list_status);
        //TODO sometimes if this is red while dropdown is open, and device connects. The icon does
        //not turn green until dropdown is reopened
        final View availableView = findViewById(R.id.contact_list_available);
        TAKContactProfileView.setStatus(self, availableView);
        availableView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                TAKContactProfileView.getInstance().showContactInfo(self);
            }
        });
        //availableView.invalidate();

        boolean connected = TAKChatXMPP.getInstance().isConnected();
        if (connected) {
            statusView.setText(TAKChatUtils.takChatComponent.getManager(ContactManager.class).getMyStatus());
        } else {
            //if not connected, display current connection status
            statusView.setText(TAKChatXMPP.getInstance().getStatusMessage());
        }

        statusView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                TAKContactProfileView.getInstance().showContactInfo(self);
            }
        });

        final ImageButton statusDetails = (ImageButton) findViewById(R.id.contact_list_status_details);
        statusDetails.setBackgroundResource(TAKContactsView.getStatusDetailsIcon());
        statusDetails.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Viewing status details");

                //TODO improve this view..
                new AlertDialog.Builder(TAKChatUtils.mapView.getContext())
                        .setIcon(com.atakmap.app.R.drawable.xmpp_icon)
                        .setTitle("Connection Details")
                        .setMessage(getStatusDetailsMessage())
                        .setPositiveButton("OK", null)
                        .show();
            }
        });
    }

    public static int getStatusDetailsIcon() {
        return TAKChatXMPP.getInstance().isConnected() ? R.drawable.takchat_info : R.drawable.takchat_error;
    }

    public static String getStatusDetailsMessage() {
        if(TAKChatXMPP.getInstance().isConnected() ){
            BareJid jid = TAKChatUtils.getUsernameBare();
            return String.format(LocaleUtil.getCurrent(), "Logged in as %s\n%s",
                    (jid == null ? "unknown" : jid.toString()),
                    TAKChatXMPP.getInstance().getLastConnectTime());
        }else{
            return TAKChatXMPP.getInstance().getLastError();
        }
    }

    @Override
    public boolean onMessage(Stanza stanza) {

        if(stanza == null || stanza.getFrom() == null){
            Log.w(TAG, "onMessage message invalid: " + (stanza == null ? "" : stanza.toString()));
            //TODO we get iq/bind in here... not really an error, but can safely ignore
            //IQ Stanza (bind urn:ietf:params:xml:ns:xmpp-bind) [id=xYLqy-7,type=result,]
            return false;
        }

        Log.d(TAG, "onMessage incoming stanza: " + stanza);

        if(stanza instanceof Message){
            Log.d(TAG, "onMessage incoming Message: " + stanza);
            Message message = (Message)stanza;

            //new message delivered, refresh unread count
            String contactString = message.getFrom().asBareJid().toString();
            if(message.getType() == Message.Type.chat && TAKChatUtils.isMine(stanza)) {
                //dont swap for Type.groupchat
                Log.d(TAG, "onMessage incoming self Message: " + stanza);
                contactString = message.getTo().asBareJid().toString();
            }

            //note ptp chat and group chat messages both delivered to chatview here
            TAKChatView chatView = TAKChatView.getChatView(contactString);
            if(chatView != null) {
                chatView.onMessage(message);
                return true;
            }else {
                Log.w(TAG, "Discard message, no chat for: " + message.getFrom().toString());
                return false;
            }
        } else if(stanza instanceof Presence){
            Log.d(TAG, "onMessage incoming Presence: " + stanza);
        } else if(stanza instanceof IQ){
            Log.d(TAG, "onMessage incoming IQ: " + stanza);
        }else{
            Log.w(TAG, "onMessage ignoring stanza of type: " + stanza.toXML());
        }
        return true;
    }

    @Override
    public void dispose() {}

    public static void getStatus(final SharedPreferences prefs) {
        final ContactManager contacts = TAKChatUtils.takChatComponent.getManager(ContactManager.class);
        AlertDialog.Builder builder = new AlertDialog.Builder(TAKChatUtils.mapView.getContext());
        LayoutInflater inflater = (LayoutInflater)TAKChatUtils.pluginContext.
                getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View statusView = inflater.inflate(R.layout.takchat_status, null, false);

        final EditText editStatus = (EditText)statusView.findViewById(R.id.takchat_status_text);
        editStatus.setText(contacts.getMyStatus());

        final ImageButton historyButton = (ImageButton) statusView.findViewById(R.id.takchat_status_history);
        historyButton.setOnClickListener(new android.view.View.OnClickListener() {

                    @Override
                    public void onClick(View v) {

                        List<String> history = MRUStringCache.GetHistory(prefs, STATUS_CACHE_KEY);
                        if (history == null || history.size() < 1)
                        {
                            Toast.makeText(TAKChatUtils.mapView.getContext(),
                                    "No History available...",
                                    Toast.LENGTH_LONG)
                                    .show();
                            return;
                        }

                        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
                                TAKChatUtils.mapView.getContext(),
                                android.R.layout.select_dialog_singlechoice);
                        for (String url : history)
                            arrayAdapter.add(url);

                        new AlertDialog.Builder(TAKChatUtils.mapView.getContext())
                                .setTitle("Status History")
                                .setAdapter(arrayAdapter,
                                        new DialogInterface.OnClickListener() {

                                            @Override
                                            public void onClick(
                                                    DialogInterface dialog,
                                                    int which) {
                                                String status = arrayAdapter
                                                        .getItem(which);
                                                if (!FileSystemUtils
                                                        .isEmpty(status))
                                                    editStatus.setText(status);
                                                dialog.dismiss();
                                            }
                                        })
                                .setNegativeButton("Cancel", null)
                                .show();
                    }
                });

        builder.setIcon(com.atakmap.app.R.drawable.xmpp_icon)
                .setCancelable(false)
                .setTitle("Set Status")
                .setView(statusView)
                .setPositiveButton("Update", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int i) {
                        dialog.cancel();

                        String status = editStatus.getText().toString();
                        if (!FileSystemUtils.isEmpty(status)) {
                            if(status.length() > MAX_STATUS_LENGTH)
                                status = status.substring(0, MAX_STATUS_LENGTH);

                            MRUStringCache.UpdateHistory(prefs, STATUS_CACHE_KEY, status);
                        }

                        //TODO sanitize text?
                        contacts.setMyStatus(status);
                        //TODO refresh this view
                        //TAKContactsView.this.refresh(prefs);
                    }
                })
                .setNegativeButton("Cancel", null);

        builder.create().show();
    }

    @Override
    public boolean onConnected() {
        Log.d(TAG, "onConnected");
        TAKChatUtils.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                refresh();
            }
        });
        return true;
    }

    @Override
    public boolean onStatus(boolean bSuccess, String status) {
        TAKChatUtils.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                refresh();
            }
        });
        return true;
    }

    @Override
    public boolean onDisconnected() {
        Log.d(TAG, "onDisconnected");
        TAKChatUtils.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                refresh();
            }
        });
        return true;
    }

    @Override
    public boolean onPresenceChanged(Presence presence) {
        Log.d(TAG, "onPresenceChanged: " + presence.toXML());
        TAKChatView chatView = TAKChatView.getChatView(presence.getFrom().asBareJid().toString());
        if(chatView != null) {
            chatView.onPresenceChanged(presence);
            return true;
        }else {
            //TODO remove this logging after testing
            Log.w(TAG, "No chat for presence: " + presence.toString());
            return false;
        }
    }

    @Override
    public boolean onContactSizeChanged() {
        //no-op here. Note ContactListAdapter handle this..
        return false;
    }

    public static void showContactListOptions(final SharedPreferences prefs, final ContactListAdapter contactAdapter) {
        final String[] options2 = TAKChatUtils.pluginContext.getResources().getStringArray(R.array.main_menu_array);
        AlertDialog.Builder builder = new AlertDialog.Builder(TAKChatUtils.mapView.getContext());
        builder.setIcon(com.atakmap.app.R.drawable.xmpp_icon)
                .setTitle(TAKChatUtils.getPluginString(R.string.app_name) + " options")
                .setItems(options2, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch(which) {
                            case 0:  //Add Contact
                                TAKChatView.showAddContactPrompt();
                                break;
                            case 1:  //Add Conference
                                TAKConferenceView.showAddConferencePrompt(prefs);
                                break;
                            case 2: // Search contacts
                                AtakBroadcast.getInstance().sendBroadcast(
                                        new Intent(TAKChatDropDownReceiver.OPEN_SEARCH));
                                break;
                            case 3:  //Toggle Offline Contacts
                                boolean cur = prefs.getBoolean("takchatShowOffline", false);
                                Log.d(TAG, "Toggling offline contacts: " + cur);
                                prefs.edit().putBoolean("takchatShowOffline", !cur).apply();
                                Toast.makeText(TAKChatUtils.mapView.getContext(),
                                        (cur ? "Hiding" : "Displaying") + " offline contacts...",
                                        Toast.LENGTH_LONG).show();
                                //TODO use an intent or something rather than pass this in...
                                if(contactAdapter != null)
                                    contactAdapter.redrawList();
                                break;
                            case 4:  //Set Status
                                TAKContactProfileView.getInstance().showContactInfo(
                                        TAKChatUtils.takChatComponent.getManager(VCardManager.class).getMyContact());
                                break;
                            case 5:  //Close All Chats
                                TAKChatView.reset();
                                break;
                            case 6:  //Reconnect Now
                                if(TAKChatXMPP.getInstance().isConnected()) {
                                    new AlertDialog.Builder(TAKChatUtils.mapView.getContext())
                                            .setIcon(com.atakmap.app.R.drawable.xmpp_icon)
                                            .setTitle("Confirm Reconnect")
                                            .setMessage("Disconnect, and reconnect now?")
                                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    Log.d(TAG, "Reconnecting now...");
                                                    TAKChatUtils.takChatComponent.getManager(ConnectionManager.class).forceReconnect(true);
                                                }
                                            })
                                            .setNegativeButton("Cancel", null)
                                            .show();
                                }else{
                                    Log.d(TAG, "Reconnecting now...");
                                    TAKChatUtils.takChatComponent.getManager(ConnectionManager.class).forceReconnect(true);
                                }
                                break;
                            case 7:  //Settings
                                AtakBroadcast.getInstance().sendBroadcast(new Intent("com.atakmap.app.ADVANCED_SETTINGS")
                                        .putExtra("toolkey", TAKChatMapComponent.TAKCHAT_PREFERENCE));
                            default:
                        }
                    }
                });
        builder.create().show();
    }

    @Override
    public void onMessageSent(ChatMessage message) {
        Log.d(TAG, "onMessageSent: " + message.toString());
        TAKChatView chatView = TAKChatView.getChatView(message.getMessage().getTo().asBareJid().toString());
        if(chatView != null) {
            chatView.onMessageSent(message);
        }else {
            //TODO remove this logging after testing
            Log.w(TAG, "No chat for sent message: " + message.toString());
        }
    }

    @Override
    public void onDeliveryReceipt(Jid from, Jid to, String deliveryReceiptId, Stanza stanza) {
        Log.d(TAG, "onDeliveryReceipt: " + deliveryReceiptId.toString());
        TAKChatView chatView = TAKChatView.getChatView(from.asBareJid().toString());
        if(chatView != null) {
            chatView.onDeliveryReceipt(from, to, deliveryReceiptId, stanza);
        }else {
            //TODO remove this logging after testing
            Log.w(TAG, "No chat for receipt: " + deliveryReceiptId.toString());
        }
    }

    @Override
    public void onDeliveryError(ChatMessage message) {
        Log.d(TAG, "onDeliveryError: " + message.toString());


        if(message.getMessage().getType() == Message.Type.chat || message.getMessage().getType() == Message.Type.groupchat) {
            //if its a chat, find view mapping via "to"
            TAKChatView chatView = TAKChatView.getChatView(message.getMessage().getTo().asBareJid().toString());
            if(chatView != null) {
                chatView.onDeliveryError(message);
            }else {
                //TODO remove this logging after testing
                Log.w(TAG, "No chat for message error: " + message.toString());
            }
        }else if(message.getMessage().getType() == Message.Type.error) {
            //if its an error, find view mapping via "from"
            TAKChatView chatView = TAKChatView.getChatView(message.getMessage().getFrom().asBareJid().toString());
            if(chatView != null) {
                chatView.onDeliveryError(message);
            }else {
                //TODO remove this logging after testing
                Log.w(TAG, "No chat for error: " + message.toString());
            }
        }else{
            Log.w(TAG, "Ignoring error message: " + message.toString());
        }
    }
}
