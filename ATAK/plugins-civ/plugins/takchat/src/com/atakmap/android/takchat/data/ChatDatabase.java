package com.atakmap.android.takchat.data;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.atakmap.android.takchat.TAKChatUtils;
import com.atakmap.android.takchat.api.TAKChatApi;
import com.atakmap.android.takchat.net.MessageUnreadManager;
import com.atakmap.android.takchat.net.TAKChatXMPP;
import com.atakmap.android.takchat.net.VCardManager;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.time.CoordinatedTime;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.jivesoftware.smackx.vcardtemp.packet.VCard;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Support persistence of data via SQLite
 *  Chat Messages, Contact VCards, Conferences, Message links (e.g. index of locations)
 *
 * Created by byoung on 7/31/2016.
 */
public class ChatDatabase extends SQLiteOpenHelper implements MessageListener, VCardListener, DeliveryReceiptListener, MessageUnreadListener {

    private static final String TAG = "XmppChatDatabase";
    private static final int DATABASE_VERSION = 2;
    private static final String DATABASE_NAME = "xmpp.sqlite";

    /**
     * Store details of incoming/outgoing chat messages
     */
    public static final String TABLE_CHAT = "ChatMessages";

    /**
     * Store VCard (aka contact card, profile) for buddies
     */
    public static final String TABLE_VCARD = "ChatVCard";

    /**
     * Store list of conferences the user has joined
     */
    public static final String TABLE_CONFERENCES = "ChatConferences";

    /**
     * Stores links within a chat message e.g. a clickable location/grid
     */
    public static final String TABLE_LINKS = "ChatLinks";

    private static final int BOOLEAN_TRUE = 1;
    private static final int BOOLEAN_FALSE = 0;

    private static ChatDatabase _instance;
    private SharedPreferences _prefs;

    static class DBColumn {
        public String key = null;
        public String type = null;
        public boolean unique = false;

        public DBColumn(String key, String type) {
            this.key = key;
            this.type = type;
        }
        public DBColumn(String key, String type, boolean unique) {
            this(key, type);
            this.unique = unique;
        }

        public boolean isUnique() {
            return unique;
        }
    }

    // By convention, make these match the names of the fields in the Bundle.
    //Local account JID
    private static final String ID_COL_NAME = "id";
    //Bare JID
    private static final String MESSAGE_TO_COL_NAME = "messageTo";
    private static final String MESSAGE_ACCT_COL_NAME = "acct";
    //Bare JID
    private static final String MESSAGE_FROM_COL_NAME = "messageFrom";
    private static final String MESSAGE_ID_COL_NAME = "stanzaId";
    private static final String MESSAGE_THREAD_ID_COL_NAME = "threadId";
    //See <code>org.jivesoftware.smack.packet</code>
    private static final String MESSAGE_TYPE_COL_NAME = "messageType";
    //The message XML (entire stanza)
    private static final String MESSAGE_CONTENT_COL_NAME = "messageContent";
    //This field is processed, but not stored in DB
    private static final String TEMP_MESSAGE_BODY_COL_NAME = "tempMessageBody";
    //time msg was received
    private static final String MESSAGE_RECV_COL_NAME = "messageRecvTime";
    //timestamp from message, if available e.g. via 'delay' extension
    private static final String MESSAGE_SENT_COL_NAME = "sent";
    private static final String MESSAGE_TIMESTAMP_COL_NAME = "messageTimestamp";
    private static final String MESSAGE_ERROR_COL_NAME = "error";
    private static final String MESSAGE_DELIVERED_COL_NAME = "delivered";
    private static final String MESSAGE_READ_COL_NAME = "read";

    private static final String VCARD_ACCT_COL_NAME = MESSAGE_ACCT_COL_NAME;
    private static final String VCARD_JID_COL_NAME = "jid";
    private static final String VCARD_XML_COL_NAME = "xml";
    private static final String VCARD_TIME_COL_NAME = "vcardTime";
    //TODO store avatar separate from rest of Vcard?
    //private static final String VCARD_AVATAR_COL_NAME = "avatar";
    private static final String VCARD_HASH_COL_NAME = "avatarhash";

    private static final String CONF_ACCT_COL_NAME = MESSAGE_ACCT_COL_NAME;
    private static final String CONF_ALIAS_COL_NAME = "confAlias";
    private static final String CONF_ID_COL_NAME = "confId";
    private static final String CONF_PASSWD_COL_NAME = "confPasswd";

    private static final String LINK_ACCT_COL_NAME = MESSAGE_ACCT_COL_NAME;
    private static final String LINK_STANZAID_COL_NAME = "linkStanzaId";
    private static final String LINK_INDEX_COL_NAME = "linkIndex";
    private static final String LINK_TYPE_COL_NAME = "linkType";
    private static final String LINK_TEXT_COL_NAME = "linkText";


    // DB types
    private static final String PK_COL_TYPE = "INTEGER PRIMARY KEY";
    private static final String TEXT_COL_TYPE = "TEXT";
    private static final String INTEGER_COL_TYPE = "INTEGER";
    private static final String BLOB_COL_TYPE = "BLOB";

    private static final DBColumn[] CHAT_COLS = {
            new DBColumn(ID_COL_NAME, PK_COL_TYPE),
            new DBColumn(MESSAGE_ACCT_COL_NAME, TEXT_COL_TYPE),
            new DBColumn(MESSAGE_TO_COL_NAME, TEXT_COL_TYPE),
            new DBColumn(MESSAGE_FROM_COL_NAME, TEXT_COL_TYPE),
            new DBColumn(MESSAGE_ID_COL_NAME, TEXT_COL_TYPE, true),
            new DBColumn(MESSAGE_THREAD_ID_COL_NAME, TEXT_COL_TYPE),
            new DBColumn(MESSAGE_TYPE_COL_NAME, TEXT_COL_TYPE),
            new DBColumn(MESSAGE_RECV_COL_NAME, INTEGER_COL_TYPE),
            new DBColumn(MESSAGE_TIMESTAMP_COL_NAME, INTEGER_COL_TYPE),
            new DBColumn(MESSAGE_CONTENT_COL_NAME, TEXT_COL_TYPE),
            new DBColumn(MESSAGE_SENT_COL_NAME, INTEGER_COL_TYPE),
            new DBColumn(MESSAGE_ERROR_COL_NAME, INTEGER_COL_TYPE),
            new DBColumn(MESSAGE_DELIVERED_COL_NAME, INTEGER_COL_TYPE),
            new DBColumn(MESSAGE_READ_COL_NAME, INTEGER_COL_TYPE)
    };

    private static final DBColumn[] VCARD_COLS = {
            new DBColumn(ID_COL_NAME, PK_COL_TYPE),
            new DBColumn(VCARD_ACCT_COL_NAME, TEXT_COL_TYPE),
            new DBColumn(VCARD_JID_COL_NAME, TEXT_COL_TYPE),
            new DBColumn(VCARD_XML_COL_NAME, TEXT_COL_TYPE),
            new DBColumn(VCARD_TIME_COL_NAME, INTEGER_COL_TYPE),
            //new DBColumn(VCARD_AVATAR_COL_NAME, BLOB_COL_TYPE),
            new DBColumn(VCARD_HASH_COL_NAME, TEXT_COL_TYPE)
    };
    private static final String[] VCARD_COLS_NAMES = {
            ID_COL_NAME,
            VCARD_ACCT_COL_NAME,
            VCARD_JID_COL_NAME,
            VCARD_XML_COL_NAME,
            VCARD_TIME_COL_NAME,
            //VCARD_AVATAR_COL_NAME,
            VCARD_HASH_COL_NAME
    };
    private static final String[] VCARD_HASH_COLS_NAMES = {
            VCARD_HASH_COL_NAME
    };

    private static final DBColumn[] CONFERENCE_COLS = {
            new DBColumn(ID_COL_NAME, PK_COL_TYPE),
            new DBColumn(CONF_ACCT_COL_NAME, TEXT_COL_TYPE),
            new DBColumn(CONF_ID_COL_NAME, TEXT_COL_TYPE, true),
            new DBColumn(CONF_ALIAS_COL_NAME, TEXT_COL_TYPE),
            new DBColumn(CONF_PASSWD_COL_NAME, TEXT_COL_TYPE),
    };

    private static final DBColumn[] LINK_COLS = {
            new DBColumn(ID_COL_NAME, PK_COL_TYPE),
            new DBColumn(LINK_ACCT_COL_NAME, TEXT_COL_TYPE),
            new DBColumn(LINK_STANZAID_COL_NAME, TEXT_COL_TYPE),
            new DBColumn(LINK_INDEX_COL_NAME, INTEGER_COL_TYPE),
            new DBColumn(LINK_TYPE_COL_NAME, TEXT_COL_TYPE),
            new DBColumn(LINK_TEXT_COL_NAME, TEXT_COL_TYPE)
    };

    /**
     * Retrieve the current ChatDatabase instance, or create a new one if none exists
     * @param context  Plugin context to access DB
     * @return  Current instance of the ChatDatabase
     */
    public static ChatDatabase getInstance(Context context) {
        if (_instance == null) {
            _instance = new ChatDatabase(context);
        }
        return _instance;
    }

    private ChatDatabase(Context context) {
        super(context, FileSystemUtils.getItem("Databases/" + DATABASE_NAME)
                .toString(), null, DATABASE_VERSION);
        _prefs = PreferenceManager.getDefaultSharedPreferences(
                TAKChatUtils.mapView.getContext());
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "Creating DB version: " + DATABASE_VERSION);
        createTable(db, TABLE_CHAT, CHAT_COLS);
        createTable(db, TABLE_VCARD, VCARD_COLS);
        createTable(db, TABLE_CONFERENCES, CONFERENCE_COLS);
        createTable(db, TABLE_LINKS, LINK_COLS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "onUpgrade version: " + oldVersion + ", to: " + newVersion);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CHAT);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_VCARD);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CONFERENCES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LINKS);
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    private void createTable(SQLiteDatabase db, String tableName, DBColumn[] columns) {
        StringBuilder createGroupTable = new StringBuilder("CREATE TABLE "
                + tableName + " (");
        String delim = "";
        for (DBColumn col : columns) {
            createGroupTable.append(delim).append(col.key).append(" ")
                    .append(col.type).append((col.isUnique())?" UNIQUE":"");
            delim = ", ";
        }
        createGroupTable.append(")");
        db.execSQL(createGroupTable.toString());
    }

    @Override
    public void dispose() {
    }

    public synchronized boolean deleteAll(){
        Log.d(TAG, "deleteAll");

        try {
            //TODO delete only for this account?
            getWritableDatabase().delete(TABLE_CHAT, null, null);
            getWritableDatabase().delete(TABLE_VCARD, null, null);
            getWritableDatabase().delete(TABLE_CONFERENCES, null, null);
            getWritableDatabase().delete(TABLE_LINKS, null, null);
            TAKChatUtils.takChatComponent.getManager(MessageUnreadManager.class).onUnreadCountChanged(null);
            return true;
        } catch (SQLiteException e) {
            Log.w(TAG, "Failed to deleteAll", e);
        }
        return false;
    }

    public synchronized boolean deleteMessages(){
        Log.d(TAG, "deleteMessages");

        try {
            //TODO delete only for this account?
            getWritableDatabase().delete(TABLE_CHAT, null, null);
            getWritableDatabase().delete(TABLE_LINKS, null, null);
            TAKChatUtils.takChatComponent.getManager(MessageUnreadManager.class).onUnreadCountChanged(null);
            return true;
        } catch (SQLiteException e) {
            Log.w(TAG, "Failed to deleteMessages", e);
        }
        return false;
    }

    public synchronized boolean deleteMessages(BareJid jid){
        BareJid localAcctJid = getLocalAccount();
        if(localAcctJid == null){
            Log.w(TAG, "cannot delete messages without account jid");
            return false;
        }

        if(jid == null){
            Log.w(TAG, "cannot delete messages without jid");
            return false;
        }

        //TODO be sure this does not delete groupchat messages from this user. I dont think it does b/c
        //server sends conference occupant JID, rather than user's normal entity JID
        Log.d(TAG, "Deleting messages: " + jid.toString());
        try {
            String where = MESSAGE_ACCT_COL_NAME + "='" + localAcctJid.toString() + "' AND "
                    + MESSAGE_TO_COL_NAME + " LIKE '" + jid.toString() + "%' AND "
                    + MESSAGE_FROM_COL_NAME + " LIKE '" + localAcctJid.getLocalpartOrNull().toString() + "%' OR "
                    + MESSAGE_TO_COL_NAME + " LIKE '" + localAcctJid.getLocalpartOrNull().toString() + "%' AND "
                    + MESSAGE_FROM_COL_NAME + " LIKE '" + jid.toString() + "%'";

            if (this.getWritableDatabase().delete(TABLE_CHAT, where, null) < 0)
                return false;
            else
                return true;
        }catch(SQLiteException e){
            Log.w(TAG, "Failed to delete messages: " + jid.toString(), e);
            return false;
        }
    }

    /**
     * Process incoming stanza
     *
     * @param stanza
     * @return
     */
    @Override
    public synchronized boolean onMessage(Stanza stanza) {

        //We only care about messages that are directed at us, and are chat messages
        //TODO what about headlines/announcements? What else might we care about
        if(Message.class.isInstance(stanza)) {
            Message message = (Message)stanza;
            if((message.getType().equals(Message.Type.chat) ||
                    message.getType().equals(Message.Type.groupchat))) {

                String stanzaId = message.getStanzaId();
                if(FileSystemUtils.isEmpty(stanzaId)){
                    Log.w(TAG, "cannot onMessage without stanza id: " + stanza.toXML());
                    return false;
                }

                //Note we currently store  "subject: body" as the "body" in the DB, we do not
                //currently have two separate DB columns
                String body = TAKChatUtils.getBody(message);
                if(!FileSystemUtils.isEmpty(body)){

                    //see if this is a re-delivery of a message we already have
                    //TODO inefficient, we query the DB for this message twice. here and in addMessage()
                    ChatMessage existing = getMessage(stanzaId, false);
                    //Log.d(TAG, "Log incoming: " + stanza.toXML() + (existing == null ? "" : ", re-delivery"));

                    Bundle bundledMessage = new Bundle();
                    bundledMessage.putString(MESSAGE_TO_COL_NAME, message.getTo().toString());
                    bundledMessage.putString(MESSAGE_FROM_COL_NAME, message.getFrom().toString());
                    bundledMessage.putString(MESSAGE_ID_COL_NAME, stanzaId);
                    bundledMessage.putString(MESSAGE_THREAD_ID_COL_NAME, message.getThread());
                    bundledMessage.putString(MESSAGE_TYPE_COL_NAME, message.getType().toString());

                    //store now as time received
                    long now = new CoordinatedTime().getMilliseconds();
                    bundledMessage.putLong(MESSAGE_RECV_COL_NAME, now);

                    //see if we can determine time that message was actually sent
                    long delay = ChatMessage.getDelay(message);
                    if(delay < 0){
                        //set now as the message time
                        //Log.d(TAG, "Using now timestamp: " + now);
                        bundledMessage.putLong(MESSAGE_TIMESTAMP_COL_NAME, now);
                    }else{
                        //use server provided time stamp
                        //Log.d(TAG, "Using server provided timestamp: " + delay);
                        bundledMessage.putLong(MESSAGE_TIMESTAMP_COL_NAME, delay);
                    }

                    //store message XML
                    bundledMessage.putString(MESSAGE_CONTENT_COL_NAME, message.toXML().toString());
                    //note, message body is processed, but not stored in DB
                    bundledMessage.putString(TEMP_MESSAGE_BODY_COL_NAME, body);

                    //yes I got the message
                    bundledMessage.putLong(MESSAGE_SENT_COL_NAME, BOOLEAN_TRUE);
                    bundledMessage.putLong(MESSAGE_ERROR_COL_NAME, BOOLEAN_FALSE);
                    bundledMessage.putLong(MESSAGE_DELIVERED_COL_NAME, BOOLEAN_TRUE);
                    //see if its already been read
                    bundledMessage.putLong(MESSAGE_READ_COL_NAME, (existing == null ? BOOLEAN_FALSE : existing.isRead() ? BOOLEAN_TRUE : BOOLEAN_FALSE));
                    addMessage(bundledMessage);

                    Bundle details = new Bundle();
                    EntityBareJid jid = message.getFrom().asEntityBareJidIfPossible();
                    if(jid != null) {
                        details.putString("jid", jid.toString());
                        details.putString("unread", String.valueOf(getUnreadCount(jid)));
                    }
                    TAKChatUtils.takChatComponent.getManager(MessageUnreadManager.class).onUnreadCountChanged(details);
                    return true;
                }else{
                    Log.d(TAG, "Ignoring message with no body, of type: " + message.getType());
                }
            }else{
                Log.d(TAG, "Ignoring message of type: " + message.getType());
            }
        }
        return false;
    }


    /**
     * Add the message to the DB
     * MESSAGE_ID_COL_NAME bundle string is required
     *
     * @param bundle  Message to add
     * @return  Key into DB to find message
     */
    private long addMessage(Bundle bundle) {
        long id = -1;

        BareJid localAcctJid = getLocalAccount();
        if(localAcctJid == null){
            Log.w(TAG, "cannot addMessage without account jid");
            return id;
        }

        bundle.putString(MESSAGE_ACCT_COL_NAME, localAcctJid.toString());

        String stanzaID = bundle.getString(MESSAGE_ID_COL_NAME);
        ChatMessage existing = getMessage(stanzaID, false);

        // Populate ContentValues
        ContentValues chatValues = new ContentValues();
        for (DBColumn dbCol : CHAT_COLS) {
            String dbColName = dbCol.key;
            String bundleKey = dbColName;
            String dataType = dbCol.type;

            if (TEXT_COL_TYPE.equals(dataType)) {
                String dataFromBundle = bundle.getString(bundleKey);

                if (dbColName != null && dataFromBundle != null) {
                    chatValues.put(dbColName, dataFromBundle);
                }
            } else if (INTEGER_COL_TYPE.equals(dataType)) {
                Long dataFromBundle = bundle.getLong(bundleKey, -1);
                if (dataFromBundle < 0)
                    dataFromBundle = null;

                //do not update timestamp if message is redelivered, errored, or receipt, etc
                if(FileSystemUtils.isEquals(dbColName, MESSAGE_RECV_COL_NAME) && existing != null){
                    Log.d(TAG, "Skipping recv timestamp: " + dataFromBundle + ", existing: " + existing.getTime());
                    continue;
                }
                if(FileSystemUtils.isEquals(dbColName, MESSAGE_TIMESTAMP_COL_NAME) && existing != null){
                    Log.d(TAG, "Skipping server timestamp: " + dataFromBundle + ", existing: " + existing.getTime());
                    continue;
                }

                if (dbColName != null && dataFromBundle != null) {
                    chatValues.put(dbColName, dataFromBundle);
                }
            } // ignore other types, including PK
        }

        //now insert or update
        if(existing == null){
            //Log.d(TAG, "Adding message: " + message.getString("message"));
            id = this.getWritableDatabase().insert(TABLE_CHAT, null, chatValues);

            //now search for locations, etc that we want to cache
            parseText(stanzaID, bundle.getString(TEMP_MESSAGE_BODY_COL_NAME));
        }else{
            //Log.d(TAG, "Updating message for: " + stanzaID + ", " + message.getString("message"));
            String[] args = new String[] {
                    localAcctJid.toString(),
                    stanzaID
            };

            String where = MESSAGE_ACCT_COL_NAME + "=? AND " + MESSAGE_ID_COL_NAME + "=?";
            try {
                id = this.getWritableDatabase().update(TABLE_CHAT,
                        chatValues, where, args);
            }catch(SQLiteException e){
                Log.w(TAG, "Failed to update message", e);
                return -1;
            }
        }

        return id;
    }

    private void parseText(String stanzaID, String body) {
        //TODO offload to worker thread/pool?
        //TODO what else to parse? TAK UIDs, emojis?

        List<MessageLocationLink> locations = MessageLocationLink.getLocations(body);
        if(FileSystemUtils.isEmpty(locations)){
            return;
        }

        for(MessageLocationLink location : locations){
            addLocationLink(stanzaID, location);
        }
    }

    /**
     * Get the local account JID to use for storing/retrieving data from SQLite
     * @return
     */
    private BareJid getLocalAccount() {
        return TAKChatUtils.getUsernameBare();
    }

    /**
     *  Get all chats to or from the specified JID
     *
     * @param jid
     * @return
     */
    public synchronized ArrayList<ChatMessage> retrieveHistory(BareJid jid) {
        //we currently always include links when loading a convo/history
        final boolean bLoadLinks = true;

        ArrayList<ChatMessage> out = new ArrayList<ChatMessage>();
        BareJid localAcctJid = getLocalAccount();
        if(localAcctJid == null){
            Log.w(TAG, "cannot retrieveHistory without account jid");
            return out;
        }

        BareJid localJid = getLocalAccount();
        if(localJid == null){
            Log.w(TAG, "cannot retrieveHistory without local jid");
            return out;
        }

        Cursor cursor = null;
        try {
            //TODO need to filter by Message.Type chat vs groupchat?
            cursor = this.getReadableDatabase().rawQuery("SELECT * FROM " + TABLE_CHAT + " WHERE "
                    + MESSAGE_ACCT_COL_NAME + " = '" + localAcctJid.toString() + "' AND "
                    + MESSAGE_TO_COL_NAME + " LIKE '" + jid.toString() + "%' AND "
                    + MESSAGE_FROM_COL_NAME + " LIKE '" + localJid.getLocalpartOrNull().toString() + "%' OR "
                    + MESSAGE_TO_COL_NAME + " LIKE '" + localJid.getLocalpartOrNull().toString() + "%' AND "
                    + MESSAGE_FROM_COL_NAME + " LIKE '" + jid.toString() + "%'"
                    + " ORDER BY " + MESSAGE_TIMESTAMP_COL_NAME, null);
            if (cursor.moveToFirst()) {
                do {
                    ChatMessage msg = cursorToChatMessage(cursor);
                    if(msg == null){
                        Log.w(TAG, "Failed to retrieve message");
                        continue;
                    }

                    if(bLoadLinks)
                        msg.setLocations(getLocationLinks(msg.getMessage().getStanzaId()));
                    out.add(msg);
                } while (cursor.moveToNext());
            }
        } catch (SQLiteException e) {
            Log.w(TAG, "Failed to retrieveHistory", e);
        } finally {
            if (cursor != null)
                cursor.close();
        }

        //Log.d(TAG, "Found " + out.size() + " for: " + jid.toString());
        return out;
    }

    /**
     * Get count of message from specified JID. If not, get count of all unread messages
     * @param jid
     * @return
     */
    public synchronized int getUnreadCount(BareJid jid) {
        Cursor cursor = null;
        int count = 0;

        BareJid localAcctJid = getLocalAccount();
        if(localAcctJid == null || localAcctJid.getLocalpartOrNull() == null){
            Log.w(TAG, "cannot getUnreadCount without account jid");
            return count;
        }

        try {
            if(jid == null){
                //Log.d(TAG, "Getting unread messages");
                cursor = this.getReadableDatabase().rawQuery("SELECT COUNT (*) FROM " + TABLE_CHAT
                                + " WHERE " + MESSAGE_ACCT_COL_NAME + " = '" + localAcctJid.toString()
                                + "' AND " + MESSAGE_TO_COL_NAME + " LIKE '" + localAcctJid.getLocalpartOrNull().toString()
                                + "%' AND " + MESSAGE_READ_COL_NAME + " = '" + BOOLEAN_FALSE + "'",
                        null);
            }else {
                //Log.d(TAG, "Getting unread messages for: " + jid.toString());
                cursor = this.getReadableDatabase().rawQuery("SELECT COUNT (*) FROM " + TABLE_CHAT
                                + " WHERE " + MESSAGE_ACCT_COL_NAME + " = '" + localAcctJid.toString()
                                + "' AND " + MESSAGE_TO_COL_NAME + " LIKE '" + localAcctJid.getLocalpartOrNull().toString()
                                + "%' AND " + MESSAGE_FROM_COL_NAME + " LIKE '" + jid.toString()
                                + "%' AND " + MESSAGE_READ_COL_NAME + " = '" + BOOLEAN_FALSE + "'",
                        null);
            }
            if(cursor != null) {
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    count = cursor.getInt(0);
                }
            }
        } catch (SQLiteException e) {
            Log.w(TAG, "Failed to getUnreadCount", e);
        } finally {
            if (cursor != null)
                cursor.close();
        }

        //Log.d(TAG, "Found " + count + " unread");
        return count;
    }

    /**
     * Get specified message from the DB
     *
     * @param stanzaID
     * @return
     */
    public synchronized ChatMessage getMessage(String stanzaID, boolean bLoadLinks) {
        BareJid localAcctJid = getLocalAccount();
        if(localAcctJid == null){
            Log.w(TAG, "cannot getMessage without account jid");
            return null;
        }

        if(FileSystemUtils.isEmpty(stanzaID)){
            Log.w(TAG, "Cannot getMessage without id");
            return null;
        }

        Cursor cursor = null;
        try {
            cursor = this.getReadableDatabase().rawQuery("SELECT * FROM " + TABLE_CHAT + " WHERE "
                    + MESSAGE_ACCT_COL_NAME + " = '" + localAcctJid.toString() + "' AND "
                    + MESSAGE_ID_COL_NAME + " = '" + stanzaID
                    + "' LIMIT 1", null);
            if (cursor.moveToFirst()) {
                ChatMessage ret = cursorToChatMessage(cursor);
                if(!bLoadLinks || ret == null)
                    return ret;

                ret.setLocations(getLocationLinks(stanzaID));
            }
        } catch (SQLiteException e) {
            Log.w(TAG, "Failed to getMessage", e);
        } finally {
            if (cursor != null)
                cursor.close();
        }

        //Log.d(TAG, "No messages for: " + stanzaID);
        return null;
    }

    private List<MessageLocationLink> getLocationLinks(String stanzaID) {
        List<MessageLocationLink> out = new ArrayList<MessageLocationLink>();
        if(FileSystemUtils.isEmpty(stanzaID)){
            Log.w(TAG, "Cannot getLocationLinks without message ID");
            return out;
        }

        BareJid localAcctJid = getLocalAccount();
        if(localAcctJid == null){
            Log.w(TAG, "cannot getLocationLinks without account jid");
            return out;
        }

        Cursor cursor = null;
        try {
            //TODO need to filter by Message.Type chat vs groupchat?
            cursor = this.getReadableDatabase().rawQuery("SELECT * FROM " + TABLE_LINKS + " WHERE "
                    + LINK_ACCT_COL_NAME + " = '" + localAcctJid.toString() + "' AND "
                    + LINK_STANZAID_COL_NAME + " = '" + stanzaID + "' "
                    + " ORDER BY " + LINK_INDEX_COL_NAME, null);
            if (cursor.moveToFirst()) {
                do {
                    MessageLocationLink link = cursorToLocationLink(cursor);
                    if(link == null){
                        Log.w(TAG, "Failed to retrieve message location link");
                        continue;
                    }

                    out.add(link);
                } while (cursor.moveToNext());
            }
        } catch (SQLiteException e) {
            Log.w(TAG, "Failed to getLocationLinks", e);
        } finally {
            if (cursor != null)
                cursor.close();
        }

        //Log.d(TAG, "Found " + out.size() + " links for: " + stanzaID);
        return out;
    }

    private MessageLocationLink cursorToLocationLink(Cursor cursor) {
        MessageLocationLink.LocationLinkType linkType = null;
        try {
            linkType = MessageLocationLink.LocationLinkType.valueOf(cursor.getString(4));
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Failed to cursorToLocationLink", e);
            return null;
        }

        return new MessageLocationLink(cursor.getInt(3), linkType, cursor.getString(5));
    }

    private long addLocationLink(String stanzaID, MessageLocationLink location){
        if(FileSystemUtils.isEmpty(stanzaID)){
            Log.w(TAG, "Cannot addLocationLink without message ID");
            return -1;
        }

        if(location == null || !location.isValid()){
            Log.w(TAG, "cannot addLocationLink without link");
            return -1;
        }

        BareJid localAcctJid = getLocalAccount();
        if(localAcctJid == null){
            Log.w(TAG, "cannot addLocationLink without account jid");
            return -1;
        }

        //TODO check if this link already in DB? It should not get stored twice...
        ContentValues linkValues = new ContentValues();
        linkValues.put(LINK_ACCT_COL_NAME, localAcctJid.toString());
        linkValues.put(LINK_STANZAID_COL_NAME, stanzaID);
        linkValues.put(LINK_INDEX_COL_NAME, location.getStartIndex());
        linkValues.put(LINK_TYPE_COL_NAME, location.getType().toString());
        linkValues.put(LINK_TEXT_COL_NAME, location.getLinkText());

        long id = -1;
        try {
            Log.d(TAG, "Adding location link: " + location.toString());
            id = this.getWritableDatabase().insert(TABLE_LINKS, null, linkValues);
        } catch (SQLiteException e) {
            Log.e(TAG, "Failed to addLocationLink", e);
        }

        return id;
    }

    private ChatMessage cursorToChatMessage(Cursor cursor) {
        try {
            Stanza stanza = PacketParserUtils.parseStanza(cursor.getString(9));
            if(stanza == null || !(stanza instanceof Message)){
                Log.w(TAG, "cursorToChatMessage failed to parse message");
                return null;
            }

            ChatMessage wrapper = new ChatMessage((Message)stanza, cursor.getLong(8));
            wrapper.setSent(cursor.getInt(10) == 0 ? false : true);
            wrapper.setError(cursor.getInt(11) == 0 ? false : true);
            wrapper.setDelivered(cursor.getInt(12) == 0 ? false : true);
            wrapper.setRead(cursor.getInt(13) == 0 ? false : true);

            //Log.d(TAG, "cursorToChatMessage: " + wrapper.toString());

            return wrapper;
        } catch (Exception e) {
            Log.w(TAG, "cursorToChatMessage", e);
            return null;
        }
    }

    /**
     * Locally store message that I sent
     *
     * @param message
     */
    @Override
    public void onMessageSent(ChatMessage message) {
        //Log.d(TAG, "onMessageSent: " + message.toString());
        message.setSent(true);
        message.setRead(true);
        addMessage(message);
    }


    /**
     * Update message that I read
     *
     * @param message
     */
    @Override
    public void onMessageRead(ChatMessage message) {
        //Log.d(TAG, "onMessageRead: " + message.toString());
        message.setRead(true);
        addMessage(message);
    }

    @Override
    public void onUnreadCountChanged() {
        //no-op
    }

    /**
     * Locally add or update message that I sent
     *
     * @param message
     */
    private synchronized void addMessage(ChatMessage message) {
        //add to db
        Bundle bundledMessage = new Bundle();
        if(message.getMessage().getTo() != null)
            bundledMessage.putString(MESSAGE_TO_COL_NAME, message.getMessage().getTo().toString());
        if(message.getMessage().getFrom() != null)
            bundledMessage.putString(MESSAGE_FROM_COL_NAME, message.getMessage().getFrom().toString());
        bundledMessage.putString(MESSAGE_ID_COL_NAME, message.getMessage().getStanzaId());
        bundledMessage.putString(MESSAGE_THREAD_ID_COL_NAME, message.getMessage().getThread());
        if(message.getMessage().getType() != null)
            bundledMessage.putString(MESSAGE_TYPE_COL_NAME, message.getMessage().getType().toString());
        bundledMessage.putLong(MESSAGE_RECV_COL_NAME, message.getTime());
        bundledMessage.putLong(MESSAGE_TIMESTAMP_COL_NAME, message.getTime());
        bundledMessage.putString(MESSAGE_CONTENT_COL_NAME, message.getMessage().toXML().toString());
        bundledMessage.putString(TEMP_MESSAGE_BODY_COL_NAME, TAKChatUtils.getBody(message));
        bundledMessage.putLong(MESSAGE_SENT_COL_NAME, message.isSent() ? BOOLEAN_TRUE : BOOLEAN_FALSE);
        bundledMessage.putLong(MESSAGE_ERROR_COL_NAME, message.isError() ? BOOLEAN_TRUE : BOOLEAN_FALSE);
        bundledMessage.putLong(MESSAGE_DELIVERED_COL_NAME, message.isDelivered() ? BOOLEAN_TRUE : BOOLEAN_FALSE);
        bundledMessage.putLong(MESSAGE_READ_COL_NAME, message.isRead() ? BOOLEAN_TRUE : BOOLEAN_FALSE);

        addMessage(bundledMessage);
    }

    @Override
    public void onDeliveryReceipt(Jid from, Jid to, String deliveryReceiptId, Stanza stanza) {
        //Log.d(TAG, "onDeliveryReceipt: " + deliveryReceiptId);
        //mark as delivered in DB
        Bundle bundledMessage = new Bundle();
        bundledMessage.putString(MESSAGE_ID_COL_NAME, deliveryReceiptId);
        bundledMessage.putLong(MESSAGE_SENT_COL_NAME, BOOLEAN_TRUE);
        bundledMessage.putLong(MESSAGE_ERROR_COL_NAME, BOOLEAN_FALSE);
        bundledMessage.putLong(MESSAGE_DELIVERED_COL_NAME, BOOLEAN_TRUE);
        bundledMessage.putLong(MESSAGE_READ_COL_NAME, BOOLEAN_TRUE);

        addMessage(bundledMessage);
    }

    @Override
    public void onDeliveryError(ChatMessage message) {
        Log.d(TAG, "onDeliveryError: " + message.toString());

        //mark as error in DB, add to db as necessary
        if(message.getMessage().getType() == Message.Type.chat || message.getMessage().getType() == Message.Type.groupchat) {
            message.setError(true);
            message.setDelivered(false);
            message.setRead(true);
            addMessage(message);
        }else if(message.getMessage().getType() == Message.Type.error){
            //see if we have this message in DB
            ChatMessage existing = getMessage(message.getMessage().getStanzaId(), false);
            if(existing == null || existing.getMessage() == null){
                Log.w(TAG, "Cannot process error message:" + message.getMessage().toString());
                return;
            }

            //mark as error in DB
            Bundle bundledMessage = new Bundle();
            bundledMessage.putString(MESSAGE_ID_COL_NAME, existing.getMessage().getStanzaId());
            bundledMessage.putLong(MESSAGE_SENT_COL_NAME, BOOLEAN_FALSE);
            bundledMessage.putLong(MESSAGE_ERROR_COL_NAME, BOOLEAN_TRUE);
            bundledMessage.putLong(MESSAGE_DELIVERED_COL_NAME, BOOLEAN_FALSE);
            bundledMessage.putLong(MESSAGE_READ_COL_NAME, BOOLEAN_TRUE);
            addMessage(bundledMessage);
        }else{
            Log.w(TAG, "Ignoring error message: " + message.toString());
        }
    }


    public synchronized boolean deleteMessage(ChatMessage message) {
        BareJid localAcctJid = getLocalAccount();
        if(localAcctJid == null){
            Log.w(TAG, "cannot delete without account jid");
            return false;
        }

        //TODO will it come back on a future server/MAM sync? if so, should we ignore it?
        ChatMessage existing = getMessage(message.getMessage().getStanzaId(), false);
        if(existing != null){
            Log.d(TAG, "Deleting message: " + message.toString());
            try {
                String where = MESSAGE_ACCT_COL_NAME + "='" + localAcctJid.toString() + "' AND "
                        + MESSAGE_ID_COL_NAME + "='" + message.getMessage().getStanzaId() + "'";
                if (this.getWritableDatabase().delete(TABLE_CHAT, where, null) < 0)
                    return false;
                else
                    return true;
            }catch(SQLiteException e){
                Log.w(TAG, "Failed to delete message", e);
                return false;
            }
        }else{
            Log.w(TAG, "Cannot delete message: " + message.toString());
            return false;
        }
    }


    /**
     * Store contact list locally so we can view chats prior to server connection
     * Just store contact JID if we dont already have VCard stored
     *
     * @param contact
     * @return
     */
    public synchronized boolean addContact(XmppContact contact){
        if(contact == null){
            Log.w(TAG, "Cannot add empty contact");
            return false;
        }

        if(TAKChatUtils.isConference(contact)){
            Log.d(TAG, "Skipping vcard for conference: " + contact.toString());
            return true;
        }

        if(hasVCard(contact)){
            Log.d(TAG, "already stored contact: " + contact.toString());
            return true;
        }

        VCard card = new VCard();
        card.setType(VCard.Type.result);
        //indicate VCard is temp via this app, not yet received from XMPP server
        card.setStanzaId("localvcard");
        card.setFrom(contact.getId());
        return addVCard(card);
    }

    /**
     * Remove local messages & VCard for our ex-buddy
     *
     * @param contact
     * @return
     */
    public synchronized boolean removeContact(XmppContact contact){
        deleteMessages(contact.getId());
        return deleteVCard(contact.getId());
    }

    private boolean hasVCard(XmppContact contact) {
        return hasVCard(contact.getId());
    }

    private boolean hasVCard(BareJid jid) {
        if (jid== null){
            Log.w(TAG, "Cannot get vcard with empty id");
            return false;
        }

        BareJid localAcctJid = getLocalAccount();
        if(localAcctJid == null){
            Log.w(TAG, "cannot get vcard without account jid");
            return false;
        }

        Cursor cursor = null;
        try {
            cursor = this.getReadableDatabase().rawQuery("SELECT COUNT (*) FROM " + TABLE_VCARD + " WHERE "
                    + VCARD_ACCT_COL_NAME + " = '" + localAcctJid.toString() + "' AND "
                    + VCARD_JID_COL_NAME + " = '" + jid.toString() + "'", null);

            int count = 0;
            if(cursor != null && cursor.getCount() > 0) {
                cursor.moveToFirst();
                count = cursor.getInt(0);
            }

            if (count > 0) {
                //Log.d(TAG, "VCard found for: " + jid.toString());
                return true;
            }else{
                //Log.d(TAG, "No VCard for: " + jid.toString());
                return false;
            }
        } catch (SQLiteException e) {
            Log.w(TAG, "Failed to hasVCard", e);
            return false;
        } finally {
            if (cursor != null)
                cursor.close();
        }
    }

    public synchronized boolean deleteVCard(BareJid jid) {
        BareJid localAcctJid = getLocalAccount();
        if(localAcctJid == null){
            Log.w(TAG, "cannot delete vcard without account jid");
            return false;
        }

        if(hasVCard(jid)){
            Log.d(TAG, "Deleting VCard: " + jid.toString());
            try {
                String where =  VCARD_ACCT_COL_NAME + "='" + localAcctJid.toString() + "' AND "
                        + VCARD_JID_COL_NAME + "='" + jid.toString()+ "'";
                if (this.getWritableDatabase().delete(TABLE_VCARD, where, null) < 0)
                    return false;
                else
                    return true;
            }catch(SQLiteException e){
                Log.w(TAG, "Failed to delete VCard", e);
                return false;
            }
        }else{
            Log.w(TAG, "Cannot delete missing VCard: " + jid.toString());
            return false;
        }
    }

    @Override
    public boolean onVCardUpdate(VCard card) {
        return addVCard(card);
    }

    @Override
    public boolean onVCardSaved(VCard card) {
        return true;
    }

    @Override
    public boolean onVCardSaveFailed(VCard card) {
        return true;
    }

    private synchronized boolean addVCard(VCard card) {
        //Log.d(TAG, "onVCardUpdate: " + card.toString());

        //currently store time it was received/stored, not time it was set by remote user
        return addVCard(card, new CoordinatedTime().getMilliseconds());
    }


    private synchronized boolean addVCard(VCard card, long time){
        BareJid localAcctJid = getLocalAccount();
        if(localAcctJid == null){
            Log.w(TAG, "cannot addVCard without account jid");
            return false;
        }

        if(card == null || card.getFrom() == null) {
            Log.w(TAG, "cannot addVCard invalid");
            return false;
        }

        if(card.getType() != VCard.Type.result) {
            Log.w(TAG, "cannot addVCard of invalid type: " + card.toXML());
            return false;
        }

        //TODO workaround
        //See Bug 6939, do not store conference VCard
        if(VCardManager.isConference(_prefs, card.getFrom().asBareJid())){
            Log.w(TAG, "Skipping addVCard for conference: " + card.toXML());
            return false;
        }

        //TODO scale down received vcards? Note, still need to track the server's vcard hash for comparisons...

        //see if already exists in DB
        VCard existing = getVCard(card.getFrom().asBareJid());
        if(existing == null){
            Log.d(TAG, "Adding vcard for: " + card.getFrom());
            ContentValues insertValues = new ContentValues();
            insertValues.put(VCARD_ACCT_COL_NAME, localAcctJid.toString());
            insertValues.put(VCARD_JID_COL_NAME, card.getFrom().toString());
            insertValues.put(VCARD_XML_COL_NAME, card.toXML().toString());
            insertValues.put(VCARD_TIME_COL_NAME, time);
            insertValues.put(VCARD_HASH_COL_NAME, card.getAvatarHash());
            if (this.getWritableDatabase().insert(TABLE_VCARD, null, insertValues) < 0)
                return false;
            else
                return true;
        }else{
            Log.d(TAG, "Updating vcard for: " + card.getFrom());
            ContentValues insertValues = new ContentValues();
            insertValues.put(VCARD_XML_COL_NAME, card.toXML().toString());
            insertValues.put(VCARD_TIME_COL_NAME, time);
            insertValues.put(VCARD_HASH_COL_NAME, card.getAvatarHash());
            String[] args = new String[] {
                    localAcctJid.toString(),
                    card.getFrom().toString()
            };

            String where = VCARD_ACCT_COL_NAME + "=? AND " + VCARD_JID_COL_NAME + "=?";
            try {
                if (this.getWritableDatabase().update(TABLE_VCARD,
                        insertValues, where, args) < 0)
                    return false;
                else
                    return true;
            }catch(SQLiteException e){
                Log.w(TAG, "Failed to update VCard", e);
                return false;
            }
        }
    }

    public synchronized VCard getVCard(BareJid jid){
        BareJid localAcctJid = getLocalAccount();
        if(localAcctJid == null){
            Log.w(TAG, "cannot getVCard without account jid");
            return null;
        }

        if (jid == null) {
            Log.w(TAG, "Unable to get VCard without JID");
            return null;
        }

        //TODO make it optional to extract the avatar bytes?
        //Log.d(TAG, "Loading vcard for: " + jid.toString());
        Cursor cursor = null;
        VCard ret = null;
        try {
            String[] selectionArgs = new String[2];
            selectionArgs[0] = localAcctJid.toString();
            selectionArgs[1] = jid.toString();

            String where = VCARD_ACCT_COL_NAME + "=? AND " + VCARD_JID_COL_NAME + "=?";
            cursor = this.getReadableDatabase().query(
                    TABLE_VCARD,
                    VCARD_COLS_NAMES,
                    where, selectionArgs,
                    null, null, null);

            if (!cursor.moveToFirst())
                return null;

            //take first match
            ret = vCardFromCursor(cursor);
        } catch (Exception e) {
            Log.w(TAG, "Failed to get VCard (" + jid + "): ", e);
        } finally {
            if (cursor != null)
                cursor.close();
        }

        if (ret != null) {
            //Log.d(TAG, "Loaded VCard: " + ret.toString());
            return ret;
        }

        //Log.d(TAG, "VCard does not exist: " + jid);
        return null;
    }

    /**
     * Retrieve list of contacts currently stored in the DB.
     */
    public synchronized List<XmppContact> getContacts() {
        List<XmppContact> ret = new ArrayList<XmppContact>();
        BareJid localAcctJid = getLocalAccount();
        if(localAcctJid == null){
            Log.w(TAG, "cannot getContacts without account jid");
            return ret;
        }

        Cursor cursor = null;
        try {
            cursor = getReadableDatabase().rawQuery("SELECT * FROM " + TABLE_VCARD + " WHERE "
                            + VCARD_ACCT_COL_NAME + " = '" + localAcctJid.toString() + "'"
                    , null);
            if (cursor.moveToFirst()) {
                do {
                    XmppContact contact = new XmppContact(
                            JidCreate.entityBareFrom(cursor.getString(2)));
                    //Log.d(TAG, "Loaded contact: " + contact.toVerboseString());
                    ret.add(contact);
                } while (cursor.moveToNext());
            }
        } catch (SQLiteException e) {
            Log.w(TAG, "A SQL error occurred!", e);
        } catch (XmppStringprepException e) {
            Log.w(TAG, "Failed to get contact!", e);
        } finally {
            if (cursor != null)
                cursor.close();
        }

        //Log.d(TAG, "Loaded contact count: " + ret.size());
        return ret;
    }

    private VCard vCardFromCursor(Cursor cursor) {
        try {
            String xml = cursor.getString(3);
            Stanza stanza = PacketParserUtils.parseStanza(xml);
            VCard card = null;
            if(stanza instanceof VCard){
                card = (VCard)stanza;
            }

            //TODO anything to do now with Avatar hash? verify it?
            //String hash = cursor.getString(4);
            //String hash2 = card.getAvatarHash();

            return card;
        } catch (Exception e) {
            Log.w(TAG, "Failed to create VCard from cursor", e);
            return null;
        }
    }

    public synchronized String getVCardHash(BareJid jid) {
        BareJid localAcctJid = getLocalAccount();
        if(localAcctJid == null){
            Log.w(TAG, "cannot getVCardHash without account jid");
            return null;
        }

        if (jid == null) {
            Log.w(TAG, "Unable to get VCard hash without JID");
            return null;
        }

        //TODO make it optional to extract the avatar bytes?
        //Log.d(TAG, "Loading vcard hash for: " + jid.toString());
        Cursor cursor = null;
        String hash = null;
        try {
            String[] selectionArgs = new String[2];
            selectionArgs[0] = localAcctJid.toString();
            selectionArgs[1] = jid.toString();

            String where = VCARD_ACCT_COL_NAME + "=? AND " + VCARD_JID_COL_NAME + "=?";
            cursor = this.getReadableDatabase().query(
                    TABLE_VCARD,
                    VCARD_HASH_COLS_NAMES,
                    where, selectionArgs,
                    null, null, null);

            if (!cursor.moveToFirst())
                return null;

            //take first match
            hash = cursor.getString(0);
        } catch (Exception e) {
            Log.w(TAG, "Failed to get VCard hash (" + jid + "): ", e);
        } finally {
            if (cursor != null)
                cursor.close();
        }

        if (!FileSystemUtils.isEmpty(hash)) {
            //Log.d(TAG, "Loaded VCard hash: " + hash);
            return hash;
        }

        //Log.d(TAG, "VCard hash does not exist: " + jid);
        return null;

    }

    /**
     * Get specified conference from the DB
     *
     * @param jid
     * @return
     */
    public synchronized XmppConference getConference(BareJid jid) {
        BareJid localAcctJid = getLocalAccount();
        if(localAcctJid == null){
            Log.w(TAG, "cannot getConference without account jid");
            return null;
        }

        if(jid == null){
            Log.w(TAG, "cannot getConference without jid");
            return null;
        }

        if(TAKChatXMPP.getInstance().getConnection() == null){
                Log.w(TAG, "cannot getConference without connection");
            return null;
        }

        Cursor cursor = null;
        try {
            cursor = this.getReadableDatabase().rawQuery("SELECT * FROM " + TABLE_CONFERENCES +
                    " WHERE "
                    + CONF_ACCT_COL_NAME + " = '" + localAcctJid.toString() + "' AND "
                    + CONF_ID_COL_NAME + " = '" + jid.toString()
                    + "' LIMIT 1", null);
            if (cursor.moveToFirst()) {
                //TODO we need to set the MUC in here, or can it be delayed until later by UI/Manager?
                MultiUserChatManager mchatManager = MultiUserChatManager.getInstanceFor(TAKChatXMPP.getInstance().getConnection());
                MultiUserChat muc = mchatManager.getMultiUserChat(JidCreate.entityBareFrom(cursor.getString(2)));
                XmppConference conf = new XmppConference(muc, cursor.getString(3));
                conf.setPassword(cursor.getString(4));
                //Log.d(TAG, "Loaded conf: " + conf.toVerboseString());
                return conf;
            }
        } catch (SQLiteException e) {
            Log.w(TAG, "A SQL error occurred!", e);
        } catch (XmppStringprepException e) {
            Log.w(TAG, "A SQL error occurred!", e);
        } finally {
            if (cursor != null)
                cursor.close();
        }

        //Log.d(TAG, "No conference for: " + jid);
        return null;
    }

    public synchronized boolean hasConference(BareJid jid) {
        if (jid== null){
            Log.w(TAG, "Cannot get conference with empty id");
            return false;
        }

        BareJid localAcctJid = getLocalAccount();
        if(localAcctJid == null){
            Log.w(TAG, "cannot get conference without jid");
            return false;
        }

        Cursor cursor = null;
        try {
            cursor = this.getReadableDatabase().rawQuery("SELECT COUNT (*) FROM " + TABLE_CONFERENCES + " WHERE "
                    + CONF_ACCT_COL_NAME + " = '" + localAcctJid.toString() + "' AND "
                    + CONF_ID_COL_NAME + " = '" + jid.toString() + "'", null);

            int count = 0;
            if(cursor != null && cursor.getCount() > 0) {
                cursor.moveToFirst();
                count = cursor.getInt(0);
            }

            if (count > 0) {
                //Log.d(TAG, "Conference found for: " + jid.toString());
                return true;
            }else{
                //Log.d(TAG, "No Conference for: " + jid.toString());
                return false;
            }
        } catch (SQLiteException e) {
            Log.w(TAG, "Failed to hasConference", e);
            return false;
        } finally {
            if (cursor != null)
                cursor.close();
        }
    }

    /**
     * Retrieve list of conferences currently stored in the DB.
     * @return List of conferences currently stored in the DB.
     */
    public synchronized List<XmppConference> getConferences() {
        List<XmppConference> ret = new ArrayList<XmppConference>();
        ArrayList<String> jids = new ArrayList<String>();
        ArrayList<String> names = new ArrayList<String>();

        BareJid localAcctJid = getLocalAccount();
        if(localAcctJid == null){
            Log.w(TAG, "cannot getConferences without account jid");
            return ret;
        }

        XMPPConnection connection = TAKChatXMPP.getInstance().getConnection();
        MultiUserChatManager chatManager = null;

        if(connection != null){
            chatManager = MultiUserChatManager.getInstanceFor(TAKChatXMPP.getInstance().getConnection());
        }

        Cursor cursor = null;
        try {
            cursor = getReadableDatabase().rawQuery("SELECT * FROM " + TABLE_CONFERENCES + " WHERE "
                    + CONF_ACCT_COL_NAME + " = '" + localAcctJid.toString() + "'"
                    , null);
            if (cursor.moveToFirst()) {
                do {
                    XmppConference conf = null;
                    if(chatManager == null){
                        //just load details
                        conf = new XmppConference(cursor.getString(3),
                                JidCreate.entityBareFrom(cursor.getString(2)), cursor.getString(4));
                    }else{
                        //load with MUC if possible
                        MultiUserChat muc = chatManager.getMultiUserChat(JidCreate.entityBareFrom(cursor.getString(2)));
                        conf = new XmppConference(muc, cursor.getString(3));
                        conf.setPassword(cursor.getString(4));
                    }

                    //Log.d(TAG, "Loaded conf: " + conf.toVerboseString());
                    ret.add(conf);
                    if(conf.getId() != null) {
                        jids.add(conf.getId().toString());
                        names.add(conf.getName());
                    }
                } while (cursor.moveToNext());
            }
        } catch (SQLiteException e) {
            Log.w(TAG, "A SQL error occurred!", e);
        } catch (XmppStringprepException e) {
            Log.w(TAG, "Failed to get Conference!", e);
        } finally {
            if (cursor != null)
                cursor.close();
        }

        TAKChatApi.getInstance().sendConferences(new TAKChatApi.ConferenceListWrapper(jids, names));

        //Log.d(TAG, "Loaded conference count: " + ret.size());
        return ret;
    }

    public synchronized TAKChatApi.ConferenceListWrapper getConferenceMetadata() {
        ArrayList<String> jids = new ArrayList<String>();
        ArrayList<String> names = new ArrayList<String>();

        BareJid localAcctJid = getLocalAccount();
        if(localAcctJid == null){
            Log.w(TAG, "cannot getConference IDs without account jid");
            return new TAKChatApi.ConferenceListWrapper(jids, names);
        }

        Cursor cursor = null;
        try {
            cursor = getReadableDatabase().rawQuery("SELECT * FROM " + TABLE_CONFERENCES + " WHERE "
                            + CONF_ACCT_COL_NAME + " = '" + localAcctJid.toString() + "'"
                    , null);
            if (cursor.moveToFirst()) {
                do {
                    EntityBareJid jid = JidCreate.entityBareFrom(cursor.getString(2));

                    //Log.d(TAG, "Loaded conf: " + jid.toString());
                    if(jid!= null) {
                        jids.add(jid.toString());
                        names.add(cursor.getString(3));
                    }
                } while (cursor.moveToNext());
            }
        } catch (SQLiteException e) {
            Log.w(TAG, "A SQL error occurred!", e);
        } catch (XmppStringprepException e) {
            Log.w(TAG, "Failed to get Conference!", e);
        } finally {
            if (cursor != null)
                cursor.close();
        }

        //Log.d(TAG, "Loaded conference ID count: " + ret.size());
        return new TAKChatApi.ConferenceListWrapper(jids, names);
    }

    /**
     * Save specified conference parameters to DB.
     * @return Index of saved DB.  -1 if failure.
     */
    public synchronized long addConference(XmppConference conference) {
        long id = -1;
        BareJid localAcctJid = getLocalAccount();
        if(localAcctJid == null){
            Log.w(TAG, "cannot addConference without account jid");
            return id;
        }

        ContentValues confValues = new ContentValues();
        confValues.put(CONF_ID_COL_NAME, conference.getId().toString());
        confValues.put(CONF_ALIAS_COL_NAME, conference.getName());
        confValues.put(CONF_PASSWD_COL_NAME, conference.getPassword());

        try {
            if(!hasConference(conference.getId())){
                Log.d(TAG, "Saving conference: " + conference.toVerboseString());
                confValues.put(CONF_ACCT_COL_NAME, localAcctJid.toString());
                id = getWritableDatabase().insert(TABLE_CONFERENCES, null, confValues);
            }else{
                Log.d(TAG, "Updating conference: " + conference.toVerboseString());
                String[] args = new String[] {
                        localAcctJid.toString(),
                        conference.getId().toString()
                };
                String where = CONF_ACCT_COL_NAME + "=? AND "
                    + CONF_ID_COL_NAME + "=?";

                id = this.getWritableDatabase().update(TABLE_CONFERENCES,
                        confValues, where, args);
            }
        } catch (SQLiteException e) {
            Log.e(TAG, "Failed to save conference", e);
        }

        TAKChatApi.getInstance().sendConferences(getConferenceMetadata());
        return id;
    }

    /**
     * Remove specified conference from the DB if it exists.
     * Also remove the corresponding groupchats
     * @return True if successfully removed, false if failure.
     */
    public synchronized boolean removeConference(XmppConference conference) {
        BareJid localAcctJid = getLocalAccount();
        if(localAcctJid == null){
            Log.w(TAG, "cannot removeConference without account jid");
            return false;
        }

        Log.d(TAG, "removeConference: " + conference.toVerboseString());

        String id = conference.getId().toString();
        String where = CONF_ACCT_COL_NAME + "='" + localAcctJid.toString() + "' AND "
                + CONF_ID_COL_NAME + "='" + id + "'";

        try {
            if(getWritableDatabase().delete(TABLE_CONFERENCES, where, null) <= 0) {
                Log.w(TAG, "Failed to remove: " + id);
            }
        } catch (SQLiteException e) {
            Log.w(TAG, "Failed to removeConference", e);
        }

        //remove chat messages stored locally as well
        Log.d(TAG, "Deleting messages for conference: " + conference.toString());
        try {
            where = MESSAGE_ACCT_COL_NAME + "='" + localAcctJid.toString() + "' AND "
                    + MESSAGE_TYPE_COL_NAME + "='" + Message.Type.groupchat + "' AND "
                    + MESSAGE_FROM_COL_NAME + " LIKE '" + id + "%'";

            if (this.getWritableDatabase().delete(TABLE_CHAT, where, null) < 0) {
                Log.w(TAG, "No messages removed messages for: " + id);
            }

            Bundle details = new Bundle();
            details.putString("jid", id);
            details.putString("unread", "0");
            TAKChatUtils.takChatComponent.getManager(MessageUnreadManager.class).onUnreadCountChanged(details);
            TAKChatApi.getInstance().sendConferences(getConferenceMetadata());
            return true;
        }catch(SQLiteException e){
            Log.w(TAG, "Failed to delete messages for: " + id, e);
        }

        return false;
    }

    public void exportHistory(String filename) {
        Log.d(TAG, "exportHistory: " + filename);
        //Note, could move this into settings if helpful
        boolean bIncludeHeaders = true;

        SQLiteDatabase db = null;
        Cursor cursor = null;
        List<List<String>> resultTable;
        try {
            db = this.getReadableDatabase();
            cursor = db.rawQuery(
                    "SELECT * FROM " + TABLE_CHAT, null);
            resultTable = new LinkedList<List<String>>();
            //first add header row
            if(bIncludeHeaders) {
                List<String> currentRow = new LinkedList<String>();
                for (int i = 0; i < CHAT_COLS.length; i++) {
                    currentRow.add(CHAT_COLS[i].key);
                }
                resultTable.add(currentRow);
            }

            //now add data rows
            if (cursor.moveToFirst()) {
                do {
                    List<String> currentRow = new LinkedList<String>();
                    for (int i = 0; i < cursor.getColumnCount(); i++) {
                        currentRow.add(cursor.getString(i));
                    }
                    resultTable.add(currentRow);
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        try {
            com.atakmap.android.chat.ChatDatabase.writeToFile(filename, resultTable);
        } catch (IOException ioe) {
            Log.e(TAG, "Error writing chat history to file " + filename, ioe);
        }
    }
}
