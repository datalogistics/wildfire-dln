package com.atakmap.android.takchat.data;

import android.app.AlertDialog;
import android.content.Intent;

import com.atakmap.android.takchat.TAKChatDropDownReceiver;
import com.atakmap.android.takchat.TAKChatUtils;
import com.atakmap.android.takchat.net.ConnectivityNotificationProxy;
import com.atakmap.android.takchat.plugin.R;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.time.CoordinatedTime;
import com.atakmap.spatial.kml.KMLUtil;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.XMPPError;
import org.jxmpp.jid.Jid;

/**
 * Listens for XMPP "headline" <code>{@link Message}</code>
 *
 * Created by byoung on 8/11/2016.
 */
public class HeadlineListener implements MessageListener {

    private static final String TAG = "HeadlineListener";
    public static final String TITLE = "Important Message";

    @Override
    public boolean onMessage(Stanza stanza) {
        if(stanza == null){
            Log.w(TAG, "onMessage message invalid");
            //TODO notify user?
            return false;
        }

        if(!(stanza instanceof Message)){
            //Log.d(TAG, "onMessage incoming Message: " + stanza);
            return false;
        }

        Message message = (Message)stanza;
        //Log.d(TAG, "processMessage incoming message: " + message);

        if (message.getType() == Message.Type.headline
                && message.getBody() != null) {

            //TODO tone down some logging
            Log.d(TAG, "onMessageReceived headline: " + message.toXML());
            headline(message.getFrom(), message.getBody());
        } else if (message.getType() == Message.Type.normal
                && message.getBody() != null) {

            //TODO OpenFire admin broswer can send message to user, it comes through this
            //way, what else comes through as "normal" ?
            //TODO tone down some logging
            Log.d(TAG, "onMessageReceived normal: " + message.toXML());
           headline(message.getFrom(), message.getBody());
        } else if (message.getType() == Message.Type.error) {
            XMPPError error = message.getError();
            Log.e(TAG, "Encountered an error message! " + message.toXML());
        } else {
            Log.d(TAG, "Ignoring message of type: " + message.getType() + " Message - " + message);
        }

        return true;
    }

    private void headline(Jid from, String body) {

        //TODO chat window instead of notification? persist in DB?

        String str = body;
        Intent intent = new Intent(TAKChatDropDownReceiver.SHOW_HEADLINE);
        if(from != null && from.getLocalpartOrNull() != null && !FileSystemUtils.isEmpty(from.getLocalpartOrNull().toString())){
            str = from.getLocalpartOrNull().toString()  + " says: " + str;
            intent.putExtra("from", from.getLocalpartOrNull().toString());
        }

        //TODO could stanza have a timestamp?
        intent.putExtra("time", CoordinatedTime.currentDate().getTime());
        intent.putExtra("body", body);

        String message = TAKChatUtils.getPluginString(R.string.app_name) + " " + TITLE;
        ConnectivityNotificationProxy.showNotification(message, str, R.drawable.xmpp_icon, intent);
        Log.i(TAG, "Headliner says: " + str);
    }

    @Override
    public void dispose() {

    }

    public static void dialog(Intent intent) {
        String from = intent.getStringExtra("from");
        String body = intent.getStringExtra("body");
        long time = intent.getLongExtra("time", -1);

        String str = body;
        if(!FileSystemUtils.isEmpty(from)){
            str = from + " says: " + str;
        }

        if(time > 0)
            str = "At " + KMLUtil.KMLDateTimeFormatter.get().format(time) + "\n" + str;

        AlertDialog.Builder builder = new AlertDialog.Builder(TAKChatUtils.mapView.getContext());
        builder.setIcon(com.atakmap.app.R.drawable.xmpp_icon)
                .setTitle(TAKChatUtils.getPluginString(R.string.app_name) + " " + HeadlineListener.TITLE)
                .setMessage(str)
                .setPositiveButton("OK", null);
        builder.create().show();
    }
}
