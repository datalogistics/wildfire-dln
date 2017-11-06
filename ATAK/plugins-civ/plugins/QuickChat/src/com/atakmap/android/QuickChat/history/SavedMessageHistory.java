
package com.atakmap.android.QuickChat.history;

import android.content.Context;
import android.preference.PreferenceManager;

import com.atakmap.coremap.log.Log;

import org.json.JSONException;

import java.util.List;

/**
 * Created by Scott Auman on 5/21/2016.
 */
public class SavedMessageHistory {

    private static final String HISTORY_KEY = "saved_message_history";
    public static final String TAG = "SavedMessageHistory";

    /**Saves a new or existing json string containing all message objects into stored memory preferences
     */
    public static synchronized void saveMessagesInHistory(Context context,
            String string) {
        Log.d(TAG,"Saved History " + string);
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putString(HISTORY_KEY, string).apply();
    }

    /**
     * @return string containing json data of all messages saved in memory
     * each json object is a Message Object (see MessageObject class)
     * the messages: array holds all messages up to date until user deletes or clears out memory
     */
    public static synchronized String getAllMessagesInHistory(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(HISTORY_KEY, getDefault());
    }

    public static synchronized List<Message> getAllMessagesInHistory(
            Context context, boolean a) {
        return new JsonMessageCreator()
                .parseJsonIntoMessageObjects(getAllMessagesInHistory(context));
    }

    /**Returns a string that is used as the default json
     * @return @String
     */
    public static String getDefault() {
        return "{ " +
                "\"messages\"" +
                ":[" +
                "]" +
                " }";
    }

    /**Clears the message history list
     * sets back to default!
     */
    public static void clearMessageHistory(Context context) {
        saveMessagesInHistory(context, getDefault());
    }

    public static void addMessageToList(Context context, Message message) {
        //received new message to add to the list!
        JsonMessageCreator jsonMessageCreator = new JsonMessageCreator();
        try {
            jsonMessageCreator.addMessageToHistoryList(message);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**removes a single message object from
     */
    public static void removeMessage(Message message) {
        try {
            new JsonMessageCreator().removeMessage(message);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static void removeMessages(List<Message> messages) {
        for (Message message : messages) {
            try {
                new JsonMessageCreator().removeMessage(message);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}
