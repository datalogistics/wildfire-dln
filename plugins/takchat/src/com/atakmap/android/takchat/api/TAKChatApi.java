package com.atakmap.android.takchat.api;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.takchat.TAKChatDropDownReceiver;
import com.atakmap.android.takchat.TAKChatUtils;
import com.atakmap.android.takchat.data.ChatDatabase;
import com.atakmap.android.takchat.data.XmppConference;
import com.atakmap.android.takchat.net.ConferenceManager;
import com.atakmap.android.takchat.net.ContactManager;
import com.atakmap.android.takchat.view.TAKConferenceView;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;

import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import java.util.ArrayList;

/**
 * Intent based API to allow other tools/plugins to interact with XMPP features
 *
 * Created by byoung on 6/9/17.
 */
public class TAKChatApi extends BroadcastReceiver{

    private static final String TAG = "TAKChatApi";

    /****************
     * Intents processed/received by TAKChatApi
     ****************/

    /**
     * Request unread count for a JID. Responds with SEND_UNREAD_COUNT_CHANGED
     * Required String extra:   jid
     */
    public static final String REQUEST_UNREAD_COUNT_CHANGED = "com.atakmap.android.takchat.api.REQUEST_UNREAD_COUNT_CHANGED";

    /**
     * Request list of conferences available on this device. Responds with SEND_CONFERENCE_LIST
     */
    public static final String REQUEST_CONFERENCE_LIST = "com.atakmap.android.takchat.api.REQUEST_CONFERENCE_LIST";

    /**
     * Send the message to the JID
     * Required String extra:   jid
     * Required String extra:   message
     */
    public static final String SEND_CHAT = "com.atakmap.android.takchat.api.SEND_CHAT";

    /**
     * Open the chat conversation for a JID
     * Required String extra:   bareJid
     */
    public static final String SHOW_CHAT = TAKChatDropDownReceiver.SHOW_CHAT;

    /**
     * Join the chatroom. If it does not exist, prompt user to create it
     * Required String extra:   jid
     */
    public static final String JOIN_CHATROOM = "com.atakmap.android.takchat.api.JOIN_CHATROOM";

    /**
     * Prompt the user to leave the chatroom
     * Required String extra:   jid
     */
    public static final String LEAVE_CHATROOM = "com.atakmap.android.takchat.api.LEAVE_CHATROOM";


    /****************
     * Intents sent by TAKChatApi
     ****************/

    /**
     * Send the unread count for a JID
     * Optional String extra:   jid if not set, then clear the unread message count
     * Optional String extra:   unread (int formatted as string)
     */
    public static final String SEND_UNREAD_COUNT_CHANGED = "com.atakmap.android.takchat.api.SEND_UNREAD_COUNT_CHANGED";

    /**
     * Send the list of conferences available on this device
     * Required ArrayList extra:   jids
     * Required ArrayList extra:   names (1 to 1 with jids)
     */
    public static final String SEND_CONFERENCE_LIST = "com.atakmap.android.takchat.api.SEND_CONFERENCE_LIST";

    //TODO always on for now, could only enable if another tools register via some intent registration mechanism
    private static boolean bEnableApi = true;

    public static class ConferenceListWrapper{

        final ArrayList<String> _jids;
        final ArrayList<String> _names;

        public ConferenceListWrapper(ArrayList<String> jids, ArrayList<String> names) {
            this._jids = jids;
            this._names = names;
        }

        public ArrayList<String> getJids() {
            return _jids;
        }

        public ArrayList<String> getNames() {
            return _names;
        }
    }

    private static TAKChatApi _instance;
    public static synchronized TAKChatApi getInstance() {
        if (_instance == null)
            _instance = new TAKChatApi();
        return _instance;
    }

    private TAKChatApi(){
        if(bEnableApi) {
            AtakBroadcast.DocumentedIntentFilter filter = new AtakBroadcast.DocumentedIntentFilter();
            filter.addAction(REQUEST_UNREAD_COUNT_CHANGED, "Allows tools to request unread count, responds with intent SEND_UNREAD_COUNT_CHANGED");
            filter.addAction(REQUEST_CONFERENCE_LIST, "Allows tools to request list of conferences available on this device, responds with intent SEND_CONFERENCE_LIST");

            filter.addAction(SEND_CHAT, "Allows tools to request to send a chat to a conversation");
            filter.addAction(SHOW_CHAT, "Allows tools to request to view a chat conversation");
            filter.addAction(JOIN_CHATROOM, "Allows tools to request to join a chat room (XMPP conference)");
            filter.addAction(LEAVE_CHATROOM, "Allows tools to request to leav a chat room (XMPP conference)");
            AtakBroadcast.getInstance().registerReceiver(this, filter);
        }
    }

    public void dispose(){
        AtakBroadcast.getInstance().unregisterReceiver(this);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if(!bEnableApi)
            return;

        Log.d(TAG, "onReceive: " + intent.getAction());

        //most received intents require a JID
        EntityBareJid jid = null;
        boolean bJidRequired = !REQUEST_CONFERENCE_LIST.equals(intent.getAction())
                && !SHOW_CHAT.equals(intent.getAction());
        if(bJidRequired) {
            String jidString = intent.getStringExtra("jid");
            if (FileSystemUtils.isEmpty(jidString)) {
                Log.w(TAG, "onReceive missing jid");
                return;
            }

            try {
                jid = JidCreate.entityBareFrom(jidString);
            } catch (XmppStringprepException e) {
                Log.w(TAG, "entityBareFrom, failed to parse: " + jidString + ", " + e.getMessage());
                Toast.makeText(context, "Invalid chat room " + jidString, Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if(REQUEST_UNREAD_COUNT_CHANGED.equals(intent.getAction())) {
            onUnreadCountChanged(jid);
        }else if(REQUEST_CONFERENCE_LIST.equals(intent.getAction())){
            sendConferences();
        }else if(SEND_CHAT.equals(intent.getAction())){
            String message = intent.getStringExtra("message");
            if(FileSystemUtils.isEmpty(message)){
                Log.w(TAG, "onReceive missing message");
                return;
            }

            //TODO send message
            Log.w(TAG, "SEND_CHAT not implemented yet");

        }else if(SHOW_CHAT.equals(intent.getAction())){
            //no-op, handled by TAKChatDropDownReceiver
        }else if(JOIN_CHATROOM.equals(intent.getAction())){
            //see if already joined, Otherwise attempt to join if it exists.
            //TODO prompt user to create the chatroom if it doesn't exist on server?

            String name = jid.hasLocalpart() ? jid.getLocalpart().toString() : null;
            if(FileSystemUtils.isEmpty(name))
                name = jid.toString();
            final XmppConference conf = new XmppConference(name, jid, null);
            final ConferenceManager confMgr = TAKChatUtils.takChatComponent.getManager(ConferenceManager.class);
            final ContactManager contactMgr = TAKChatUtils.takChatComponent.getManager(ContactManager.class);
            if(confMgr == null || contactMgr == null){
                Log.d(TAG, "Shutdown, skipping API intent");
                return;
            }

            for(XmppConference cur : contactMgr.getConferences()) {
                if (cur.equals(conf)) {
                    Log.d(TAG, "You're already connected to a group with ID: " + jid + "!");
                    return;
                }
            }

            final EntityBareJid fJid = jid;
            new AlertDialog.Builder(TAKChatUtils.mapView.getContext())
                    .setIcon(com.atakmap.app.R.drawable.xmpp_icon)
                    .setTitle("Join Chat Conference")
                    .setMessage("Join " + jid.toString() + "?")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            TAKChatUtils.runInBackground(new Runnable() {
                                @Override
                                public void run() {
                                    confMgr.join(conf, true);
                                    ChatDatabase.getInstance(TAKChatUtils.pluginContext).addConference(conf);
                                }
                            });

                            onUnreadCountChanged(fJid);
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();

        }else if(LEAVE_CHATROOM.equals(intent.getAction())){
            //see if joined. If so, leave chatroom
            String name = jid.hasLocalpart() ? jid.getLocalpart().toString() : null;
            if(FileSystemUtils.isEmpty(name))
                name = jid.toString();
            final XmppConference conf = new XmppConference(name, jid, null);
            final ConferenceManager confMgr = TAKChatUtils.takChatComponent.getManager(ConferenceManager.class);
            final ContactManager contactMgr = TAKChatUtils.takChatComponent.getManager(ContactManager.class);
            if(confMgr == null || contactMgr == null){
                Log.d(TAG, "Shutdown, skipping API intent");
                return;
            }

            for(XmppConference cur : contactMgr.getConferences()) {
                if (cur.equals(conf)) {
                    Log.d(TAG, "Prompting user to leave group with ID: " + jid + "!");
                    new AlertDialog.Builder(TAKChatUtils.mapView.getContext())
                            .setIcon(com.atakmap.app.R.drawable.xmpp_icon)
                            .setTitle("Leave Chat Conference")
                            .setMessage("Leave " + jid.toString() + "?")
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    TAKConferenceView.leave(conf, null);
                                }
                            })
                            .setNegativeButton("Cancel", null)
                            .show();

                    return;
                }
            }

            Log.d(TAG, "You're already not connected to a group with ID: " + jid + "!");
        }
    }

    private void onUnreadCountChanged(EntityBareJid jid) {
        Bundle details = new Bundle();
        details.putString("jid", jid.toString());
        details.putString("unread", String.valueOf(ChatDatabase.getInstance(TAKChatUtils.pluginContext).getUnreadCount(jid)));
        onUnreadCountChanged(details);
    }

    public void sendConferences() {
        sendConferences(ChatDatabase.getInstance(TAKChatUtils.pluginContext).getConferenceMetadata());
    }

    public void onUnreadCountChanged(Bundle bundle) {
        if(!bEnableApi)
            return;

        AtakBroadcast.getInstance().sendBroadcast(bundle(SEND_UNREAD_COUNT_CHANGED, bundle));
    }

    public void sendConferences(ConferenceListWrapper list) {
        if(!bEnableApi)
            return;

        AtakBroadcast.getInstance().sendBroadcast(new Intent(SEND_CONFERENCE_LIST)
                .putExtra("jids", list.getJids())
                .putExtra("names", list.getNames()));
    }

    /**
     * Assumes all bundled values are formatted as strings
     * @param action
     * @param bundle
     * @return
     */
    private static Intent bundle(String action, Bundle bundle) {
        Intent intent = new Intent(action);
        if(bundle != null && bundle.size() > 0) {
            for(String key : bundle.keySet()) {
                intent.putExtra(key, bundle.getString(key));
            }
        }

        return intent;
    }
}
