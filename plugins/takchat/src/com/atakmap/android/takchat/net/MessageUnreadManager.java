package com.atakmap.android.takchat.net;

import android.os.Bundle;

import com.atakmap.android.takchat.TAKChatUtils;
import com.atakmap.android.takchat.data.ChatDatabase;
import com.atakmap.android.takchat.data.ChatMessage;
import com.atakmap.android.takchat.data.MessageUnreadListener;
import com.atakmap.android.takchat.api.TAKChatApi;
import com.atakmap.coremap.log.Log;

import org.jxmpp.jid.EntityBareJid;

/**
 * Processes message unread status
 *
 * Created by byoung on 10/25/2016.
 */
public class MessageUnreadManager extends IManager<MessageUnreadListener>{

    private static final String TAG = "MessageUnreadManager";

    public MessageUnreadManager() {
        super(TAG);
    }

    public void messageRead(ChatMessage read) {
        Log.d(TAG, "messageRead: " + read.toString());
        read.setRead(true);

        synchronized (_listeners) {
            for (MessageUnreadListener listener : _listeners) {
                listener.onMessageRead(read);
            }
        }

        //TODO should it always be getFrom() or should be getTo() in some cases?
        Bundle details = new Bundle();
        EntityBareJid jid = read.getMessage().getFrom().asEntityBareJidIfPossible();
        if(jid != null) {
            details.putString("jid", jid.toString());
            details.putString("unread", String.valueOf(ChatDatabase.getInstance(TAKChatUtils.pluginContext).getUnreadCount(jid)));
        }
        onUnreadCountChanged(details);
    }

    public void onUnreadCountChanged(Bundle bundle) {
        Log.d(TAG, "onUnreadCountChanged");

        synchronized (_listeners) {
            for (MessageUnreadListener listener : _listeners) {
                listener.onUnreadCountChanged();
            }
        }

        //send out intent for other tools
        TAKChatApi.getInstance().onUnreadCountChanged(bundle);
    }
}
