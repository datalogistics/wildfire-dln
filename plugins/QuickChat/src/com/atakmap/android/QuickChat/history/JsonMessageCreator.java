
package com.atakmap.android.QuickChat.history;

import com.atakmap.coremap.log.Log;

import com.atakmap.android.maps.MapView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Scott Auman on 5/21/2016.
 */
class JsonMessageCreator {

    private final String TAG = JsonMessageCreator.this.toString();
    private IMessagePopper iMessagePopper;

    public JsonMessageCreator(IMessagePopper iMessagePopper) {
        this.iMessagePopper = iMessagePopper;
    }

    public JsonMessageCreator() {

    }

    /**
     * @param jsonString the json string containing all message objects
     * @return a List Object containing all message objects created from current json stream
     */
    public List<Message> parseJsonIntoMessageObjects(String jsonString) {

        List<Message> messages = new ArrayList<Message>();

        JSONArray messagesArray = getMessagesJsonArrayFromMainObject(jsonString);
        if (messagesArray == null) {
            Log.e(TAG, "messagesArray is null");
        } else {
            for (int i = 0; i < messagesArray.length(); i++) {
                try {
                    JSONObject jo = getMessageObjectFromMessageArray(
                            messagesArray, i);
                    if (jo != null)
                        messages.add(buildMessageFromMessageJsonObject(jo));
                    else
                        Log.e(TAG, "JSON Object is null");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        return messages;
    }

    /**
     * Returns the JsonObject for the given looping index
     *
     * @param array JsonArray object
     * @param i     the loping index
     * @return JsonObject
     */
    private JSONObject getMessageObjectFromMessageArray(JSONArray array, int i) {
        try {
            return array.getJSONObject(i);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Creates a message object from the supplied json
     * object parameters, each method contains all 4 strings
     * @throws JSONException
     */
    private Message buildMessageFromMessageJsonObject(JSONObject object)
            throws JSONException {

        Message message = new Message();
        message.setTime(object.getString("time"));
        message.setMessage(object.getString("message"));
        message.setFrom(object.getString("from"));
        message.setDate(object.getString("date"));
        message.setMessageDateObj(Long.parseLong(object.getString("dateLong")));
        message.setUid(object.getString("uid"));
        message.setTo(object.getString("to"));
        //o for received 1 for sent
        message.setType(object.getInt("type") == 0 ? Message.TYPE.RECEIVED : Message.TYPE.SENT);

        if (iMessagePopper != null) {
            iMessagePopper.onMessageCreated(message);
        }

        return message;
    }

    private JSONArray getMessagesJsonArrayFromMainObject(String string) {
        try {
            return new JSONObject(string).getJSONArray("messages");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Receives a message object, disassembles the object and
     * places into the end of the json array object list
     *
     * @param message the message object
     */
    public void addMessageToHistoryList(Message message) throws JSONException {

        JSONObject object = new JSONObject();
        object.put("date", message.getDate());
        object.put("time", message.getTime());
        object.put("from", message.getFrom());
        object.put("message", message.getMessage());
        object.put("dateLong", message.getMessageDateObj());
        object.put("type",message.getType() == Message.TYPE.RECEIVED ? 0 : 1);
        object.put("uid",message.getUid());
        object.put("to",message.getTo());

        buildJsonString(addMessageObjectToExistingList(object));
    }

    private JSONArray addMessageObjectToExistingList(JSONObject obj)
            throws JSONException {

        JSONArray messagesArray = getMessagesJsonArrayFromMainObject
                (SavedMessageHistory.getAllMessagesInHistory(MapView
                        .getMapView().getContext()));

        //coverity issue 18320 calling a method on a null object EMD
        if (messagesArray == null) {
            Log.e(TAG, "messagesArray is null");
        } else {
            //System.out.println(messagesArray);
            messagesArray.put(messagesArray.length(), obj);
        }
        return messagesArray;
    }

    private synchronized void buildJsonString(JSONArray array)
            throws JSONException {
        JSONObject object = new JSONObject();
        object.put("messages", array);
        SavedMessageHistory.saveMessagesInHistory(MapView.getMapView()
                .getContext(), object.toString());
    }

    public synchronized void removeMessage(Message message)
            throws JSONException {
        JSONArray newList = new JSONArray();
        JSONArray messagesArray = getMessagesJsonArrayFromMainObject(SavedMessageHistory
                .getAllMessagesInHistory(MapView.getMapView().getContext()));

        if (messagesArray == null) {
            Log.e(TAG, "messagesArray is null");
        } else {
            for (int i = 0; i < messagesArray.length(); i++) {
                if (!buildMessageFromMessageJsonObject(
                        messagesArray.getJSONObject(i)).getMessageDateObj()
                        .equals(message.getMessageDateObj())) {
                    newList.put(newList.length(),
                            messagesArray.getJSONObject(i));
                }
            }
        }
        buildJsonString(newList);
    }

}
