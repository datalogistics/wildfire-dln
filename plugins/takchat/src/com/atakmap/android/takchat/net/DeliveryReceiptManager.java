package com.atakmap.android.takchat.net;

import com.atakmap.android.takchat.data.ChatMessage;
import com.atakmap.android.takchat.data.DeliveryReceiptListener;
import com.atakmap.android.takchat.data.ResponseListener;
import com.atakmap.android.takchat.data.TimedRequestHolder;
import com.atakmap.android.takchat.data.TimerListener;
import com.atakmap.android.takchat.notification.SoundManager;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.time.CoordinatedTime;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.receipts.ReceiptReceivedListener;
import org.jxmpp.jid.Jid;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Processes delivery receipts for message sent by this/local user
 * Note, currently we're setting Smack lib to auto-reply to other users when we receive messages from them
 *
 * TODO currently using ReceiptReceivedListener and ReponseListener. Need both?
 *
 * Created by byoung on 7/31/2016.
 */
public class DeliveryReceiptManager extends IManager<DeliveryReceiptListener> implements
        ReceiptReceivedListener, TimerListener, ConnectivityListener {

    private static final String TAG = "DeliveryReceiptManager";

    public DeliveryReceiptManager() {
        super(TAG);
        _requests = new HashMap<String, TimedRequestHolder>();
    }

    /**
     * Map Stanza ID to listener
     */
    public Map<String, TimedRequestHolder> _requests;

    @Override
    public void init(AbstractXMPPConnection connection) {
        Log.d(TAG, "init");

        org.jivesoftware.smackx.receipts.DeliveryReceiptManager.getInstanceFor(connection).autoAddDeliveryReceiptRequests();
        org.jivesoftware.smackx.receipts.DeliveryReceiptManager.getInstanceFor(connection).addReceiptReceivedListener(this);
    }

    /**
     * Send stanza and get callback for sent, error, timeout
     *
     * @param iq
     * @param listener
     * @throws XMPPException
     */
    public void sendRequest(IQ iq, ResponseListener listener) throws SmackException.NotConnectedException, InterruptedException {
        String stanzaId = iq.getStanzaId();
        TimedRequestHolder holder = new TimedRequestHolder(listener);
        if(!TAKChatXMPP.getInstance().isConnected()){
            Log.w(TAG, "Not connected, cannot sendRequest: " + iq.getStanzaId());
            throw new SmackException.NotConnectedException("Not connected, cannot send: " + iq.getStanzaId());
        }

        Log.d(TAG, "sendRequest: " + stanzaId);
        TAKChatXMPP.getInstance().getConnection().sendStanza(iq);
        synchronized (_requests) {
            _requests.put(stanzaId, holder);
        }
    }

    @Override
    public void onReceiptReceived(Jid from, Jid to, String deliveryReceiptId, Stanza stanza) {
        //TODO any error checking on inputs?
        Log.d(TAG, "onReceiptReceived: from: " + from + " to: " + to + " deliveryReceiptId: " + deliveryReceiptId + " stanza: " + stanza);

        //TODO sound for receipt?

        synchronized (_listeners) {
            for (DeliveryReceiptListener listener : _listeners) {
                listener.onDeliveryReceipt(from, to, deliveryReceiptId, stanza);
            }
        }
    }

    public void messageSent(ChatMessage sent) {
        Log.d(TAG, "messageSent: " + sent.toString());

        SoundManager.getInstance().play(SoundManager.SOUND.SENT);

        synchronized (_listeners) {
            for (DeliveryReceiptListener listener : _listeners) {
                listener.onMessageSent(sent);
            }
        }
    }

    public void messageError(ChatMessage sent) {
        Log.w(TAG, "messageError: " + sent.toString());

        SoundManager.getInstance().play(SoundManager.SOUND.ERROR);

        synchronized (_listeners) {
            for (DeliveryReceiptListener listener : _listeners) {
                listener.onDeliveryError(sent);
            }
        }
    }

    @Override
    public void onTimer() {
        synchronized (_requests) {
            if (_requests == null || _requests.size() < 1) {
                //Log.d(TAG, "onTimer, no requests");
                return;
            }

            //TODO we have _requests blocked so be sure these are a quick operation...

            long now = new CoordinatedTime().getMilliseconds();
            //Log.d(TAG, "onTimer size: " + _requests.size() + ", now: " + now);
            Iterator<Map.Entry<String, TimedRequestHolder>> iterator = _requests.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, TimedRequestHolder> request = iterator.next();
                if (request != null && request.getValue().isExpired(now)) {
                    Log.d(TAG, "onTimer expired: " + request.getKey());
                    request.getValue().getListener().onTimeout(request.getKey());
                    iterator.remove();
                } else {
                    //Log.d(TAG, "onTimer not expired: " + request.getKey() + ": " + request.getValue().toString());
                }
            }
        }
    }

    public void processIq(IQ iq) {
        String stanzaId = iq.getStanzaId();
        if (stanzaId != null && (iq.getType() == IQ.Type.result || iq.getType() == IQ.Type.error)) {
            TimedRequestHolder requestHolder = null;
            synchronized (_requests) {
                requestHolder = _requests.remove(stanzaId);
            }

            if (requestHolder != null) {
                if (iq.getType() == IQ.Type.result) {
                    Log.d(TAG, "processIq onReceived: " + stanzaId);
                    requestHolder.getListener().onReceived(stanzaId, iq);
                } else {
                    Log.d(TAG, "processIq onError: " + stanzaId);
                    requestHolder.getListener().onError(stanzaId, iq);
                }
            }
        }
    }

    @Override
    public boolean onConnected() {
        return true;
    }

    @Override
    public boolean onStatus(boolean bSuccess, String status) {
        return true;
    }

    @Override
    public boolean onDisconnected() {
        synchronized (_requests) {
            //TODO we have _requests blocked so be sure these are a quick operation...

            for (Map.Entry<String, TimedRequestHolder> request : _requests.entrySet()) {
                Log.d(TAG, "onDisconnected: " + request.getKey());
                request.getValue().getListener().onDisconnect(request.getKey());
            }

            _requests.clear();
        }

        return true;
    }
}
