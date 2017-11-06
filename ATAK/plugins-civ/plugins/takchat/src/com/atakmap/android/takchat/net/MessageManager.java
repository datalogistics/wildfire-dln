package com.atakmap.android.takchat.net;

import com.atakmap.android.takchat.TAKChatUtils;
import com.atakmap.android.takchat.data.ChatMessage;
import com.atakmap.android.takchat.data.MessageListener;
import com.atakmap.android.takchat.plugin.R;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;
import com.atakmap.filesystem.HashingUtils;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.delay.packet.DelayInformation;
import org.jivesoftware.smackx.forward.packet.Forwarded;
import org.jivesoftware.smackx.mam.element.MamElements;
import org.jxmpp.jid.EntityJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import java.util.HashMap;

/**
 * Manages chat messages
 *
 * Created by byoung on 7/29/2016.
 */
public class MessageManager extends IManager<MessageListener> implements StanzaListener {

    final static public String TAG = "MessageManager";

    private HashMap<EntityJid, Chat> _myChats;

    public MessageManager() {
        super(TAG);
        _myChats = new HashMap<EntityJid, Chat>();
    }

    @Override
    public synchronized void dispose() {
        super.dispose();
    }

    @Override
    public void processStanza(Stanza stanza) {
        if(isShutdown()) {
            Log.d(TAG, "Shutdown, ignoring: " + stanza);
            return;
        }

        //first handle errors and similar cases
        if(Message.class.isInstance(stanza)) {
            Message message = (Message)stanza;

            if (message.getType() == Message.Type.error) {
                Log.w(TAG, "Received error message: " + message.toXML());

                //TODO need to handle various types of error e.g. send chat, send groupchat, send invite, join conference, etc

                ChatMessage wrapper = new ChatMessage(message);
                wrapper.setSent(false);
                wrapper.setError(true);
                wrapper.setDelivered(false);
                TAKChatUtils.takChatComponent.getManager(DeliveryReceiptManager.class).messageError(wrapper);

                //do not pass error message onto rest of message listeners
                return;
            }

            //we are sometimes getting groupchat w/out stanza ID (e.g. from Xabber or archived by ejabberd)
            //may need hash parts of the message to get a repeatable ID so re-delivery can be detected
            if(FileSystemUtils.isEmpty(stanza.getStanzaId())){
                String hash = hashMessage(message);
                Log.w(TAG, "No stanza ID set for: " + stanza.toXML() + ", using hash: " + hash);
                stanza.setStanzaId(hash);
            }

            if(stanza.hasExtension(MamElements.NAMESPACE)){
                MamElements.MamResultExtension result = MamElements.MamResultExtension.from(message);
                if(result != null){
                    Forwarded fwd = result.getForwarded();
                    if(fwd != null) {
                        DelayInformation delay = fwd.getDelayInformation();

                        //TODO what other restrictions?
                        Stanza forwardedStanza = fwd.getForwardedStanza();
                        if (forwardedStanza != null && forwardedStanza instanceof Message) {
                            //we are sometimes getting groupchat w/out stanza ID (e.g. from Xabber or archived by ejabberd)
                            //may need hash parts of the message to get a repeatable ID so re-delivery can be detected
                            if(FileSystemUtils.isEmpty(forwardedStanza.getStanzaId())){
                                String hash = hashMessage(forwardedStanza);
                                Log.w(TAG, "No stanza ID set for forwarded: " + forwardedStanza.toXML() + ", using hash: " + hash);
                                forwardedStanza.setStanzaId(hash);
                            }

                            if (delay != null) {
                                //TODO delay has the actual message time...
                                Log.d(TAG, "Adding delay information for archived message: " + delay.toXML());
                                forwardedStanza.addExtension(delay);
                            }

                            if(forwardedStanza.getTo() == null) {
                                //Note seeing this for mam:1 group chats from ejabberd server
                                Message fwdMessage = (Message) forwardedStanza;
                                if (fwdMessage.getType() != null && fwdMessage.getType() == Message.Type.groupchat){
                                    Log.w(TAG, "Archived groupchat has no 'To' set, using self");
                                    fwdMessage.setTo(TAKChatUtils.getUsernameFull());
                                }
                            }

                            Log.d(TAG, "Stanza has archive extension: " + MamElements.NAMESPACE + ", " + forwardedStanza.toXML());
                            //TODO should we pass directly to listeners, or call this.onMessage recursively?
                            synchronized (_listeners){
                                for(MessageListener l : _listeners){
                                    l.onMessage(forwardedStanza);
                                }
                            }

                            return;
                        }
                    }
                }
            }

        } else if(IQ.class.isInstance(stanza)) {
            //look for IQ delivered or error
            TAKChatUtils.takChatComponent.getManager(DeliveryReceiptManager.class).processIq((IQ)stanza);
        } else if(Presence.class.isInstance(stanza)) {
            //TODO anything we need to do with error presence? e.g. contact in roster not found, or contact's server not found
            //e.g. look for <error> extensions with code, type, and other details
            Presence presence = (Presence)stanza;
            if (presence.getType() == Presence.Type.error) {
                Log.d(TAG, "presence error from: " + (presence.getFrom() == null ? "na" : presence.getFrom().toString()));
            }
            //TODO handle any other Presence.Type?
        }


        //TODO tone down logging after testing
        Log.d(TAG, "processMessage: " + stanza.toXML());

        synchronized (_listeners){
            for(MessageListener l : _listeners){
                l.onMessage(stanza);
            }
        }

        return;
    }

    private String hashMessage(Stanza stanza) {
        //TODO is this repeatable?
        //TODO is this unique enough? If A sends B the same message twice will it hash collision? not if it has delay/timestamp...
        // If not, hash to, from, body, delay/timestamp, etc
        return HashingUtils.sha256sum(stanza.toXML().toString());
    }

    public boolean sendChat(ChatMessage wrapper) {
        if(sendChatInternal(wrapper.getMessage())) {
            wrapper.setSent(true);
            wrapper.setError(false);
            wrapper.setDelivered(false);
            wrapper.setRead(true);
            //send app internal event. Note this class is one of the listeners..
            TAKChatUtils.takChatComponent.getManager(DeliveryReceiptManager.class).messageSent(wrapper);
            return true;
        }else{
            Log.w(TAG, "Failed to send chat: " + wrapper.toString());
            wrapper.setSent(false);
            wrapper.setError(true);
            wrapper.setDelivered(false);
            wrapper.setRead(true);

            //set from to local user, as it may not be set by Smack during error
            wrapper.getMessage().setFrom(TAKChatUtils.getUsernameFull());
            //send app internal event. Note this class is one of the listeners..
            TAKChatUtils.takChatComponent.getManager(DeliveryReceiptManager.class).messageError(wrapper);
            return false;
        }
    }

    private boolean sendChatInternal(Message chatMessage) {
        if(chatMessage == null){
            Log.w(TAG, "Chat message invalid");
            return false;
        }

        XMPPConnection connection = TAKChatXMPP.getInstance().getConnection();
        ConnectionSettings config = TAKChatXMPP.getInstance().getConfig();

        //TODO return error back to UI/_listeners?
        if(!TAKChatXMPP.getInstance().isConnected()) {
            Log.w(TAG, "sendMessage: Chat service not onConnected: " + (chatMessage == null ? "" : chatMessage.toString()));
            return false;
        }

        Chat myChat = null;
        try {
            EntityJid toJid = JidCreate.entityBareFrom(chatMessage.getTo());
            myChat = _myChats.get(toJid);
            if(myChat == null) {
                myChat = ChatManager.getInstanceFor(connection).createChat(toJid, null);
                 _myChats.put(toJid, myChat);
            }
        } catch (XmppStringprepException e) {
            String message = "Failed to parse server address JID: " + config.getServerAddress();
            Log.e(TAG, message, e);

            String title = TAKChatUtils.getPluginString(R.string.app_name) + " " + TAKChatXMPP.ERROR_TITLE;
            ConnectivityNotificationProxy.showNotification(title, message, R.drawable.xmpp_icon, null);
        }

        try {
            Log.d(TAG, "Sending: " + chatMessage.toString());
            if (myChat != null) 
                myChat.sendMessage(chatMessage);

            return true;
        } catch (SmackException.NotConnectedException e) {
            Log.e(TAG, "sendMessage NotConnectedException", e);
            //TODO return error message
            return false;
        } catch (Exception e) {
            Log.e(TAG, "sendMessage Exception", e);
            //TODO return error message
            return false;
        }
    }

    //TODO add this back in!
    /*public boolean setChatState(ChatState state){
        try {
            Log.d(TAG, "Setting chat state: " + state);
            ChatStateManager.getInstance(TAKChatXMPP.getInstance().getConnection()).setCurrentState(state, _myChat);
            return true;
        } catch (SmackException.NotConnectedException e) {
            Log.e(TAG, "setChatState NotConnectedException", e);
            //TODO return error message
            return false;
        } catch (InterruptedException e) {
            Log.e(TAG, "setChatState NotConnectedException", e);
            //TODO return error message
            return false;
        }
    }*/
}
