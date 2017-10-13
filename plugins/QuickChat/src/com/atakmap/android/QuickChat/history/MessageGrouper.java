
package com.atakmap.android.QuickChat.history;

import com.atakmap.coremap.log.Log;

import com.atakmap.android.QuickChat.utils.DateConverter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by Scott Auman on 7/19/2016.
 * this class builds the header list and the child list used
 * for the expandable list view for the message history
 * we take all messages currently saved and sort the messages by date
 * each date will become a key value pair and all messages in that day will be put into a string
 * matching with that key pair, we also sort the header list and make sure that the earliest date is shown at the top
 * @author Scott Auman
 */
public class MessageGrouper {

    private List<Message> messages = new ArrayList<Message>();
    private final HashMap<String, List<Message>> messageMap = new HashMap<String, List<Message>>();
    private final String TAG = getClass().getSimpleName();

    public MessageGrouper(List<Message> m) {
        this.messages = m;

        HistoryAdapter.ADAPTER_TYPE type = QuickChatHistoryDropDown.getAdapterType();

        if(type == HistoryAdapter.ADAPTER_TYPE.DATE){
            sortThroughMessagesDate();
            sortHeadersByDateEarliest();
        }else{
            sortThroughMessagesCallsigns();
            sortHeadersByCallsigns();
        }
    }


    private void sortHeadersByCallsigns(){
        // Sort method needs a List, so let's first convert Set to List in Java
        Set<Map.Entry<String, List<Message>>> listOfEntries = getMessageMap()
                .entrySet();
        // Sort method needs a List, so let's first convert Set to List in Java
        List<Map.Entry<String, List<Message>>> list = new ArrayList<Map.Entry<String, List<Message>>>(
                listOfEntries);
        Collections.sort(list,
                new Comparator<Map.Entry<String, List<Message>>>() {
                    @Override
                    public int compare(Map.Entry<String, List<Message>> lhs,
                                       Map.Entry<String, List<Message>> rhs) {
                        return lhs.getKey().compareToIgnoreCase(rhs.getKey());
                    }
                });
        messageMap.clear(); //clear hash map for incoming sorted values
        // copying entries from List to Map-Hash
        for (Map.Entry<String, List<Message>> entry : list) {
            messageMap.put(entry.getKey(), entry.getValue());
        }
    }

    private void sortHeadersByDateEarliest() {

        // Sort method needs a List, so let's first convert Set to List in Java
        Set<Map.Entry<String, List<Message>>> listOfEntries = getMessageMap()
                .entrySet();
        // Sort method needs a List, so let's first convert Set to List in Java
        List<Map.Entry<String, List<Message>>> list = new ArrayList<Map.Entry<String, List<Message>>>(
                listOfEntries);
        Collections.sort(list,
                new Comparator<Map.Entry<String, List<Message>>>() {
                    @Override
                    public int compare(Map.Entry<String, List<Message>> lhs,
                            Map.Entry<String, List<Message>> rhs) {
                        DateConverter dateConverter = new DateConverter();
                        return dateConverter.compareDates(lhs.getKey(),
                                rhs.getKey());
                    }
                });
        messageMap.clear(); //clear hash map for incoming sorted values
        // copying entries from List to Map-Hash
        for (Map.Entry<String, List<Message>> entry : list) {
            messageMap.put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * sorts all the message objects and orders them
     * from earliest to latest
     */
    private synchronized void sortFromLatestToLast(List<Message> messages) {
        Collections.sort(messages, new Comparator<Message>() {
            @Override
            public int compare(Message lhs, Message rhs) {
                Date d1 = new Date(lhs.getMessageDateObj());
                Date d2 = new Date(rhs.getMessageDateObj());
                return d2.compareTo(d1);
            }
        });
    }


    private void sortThroughMessagesCallsigns() {

        for (Message message : messages) {
            String callSign = message.getFrom().equals("") ? message.getTo() : message.getFrom();
            if (messageMap.containsKey(callSign)) {
                messageMap.get(callSign).add(message);
                sortFromLatestToLast(messageMap.get(callSign));
            } else {
                List<Message> messages = new ArrayList<Message>();
                messages.add(message);
                messageMap.put(callSign, messages);
            }
        }

        if (messageMap == null) {
            Log.d(TAG, "message map is null @ #42");
        }
    }

    private void sortThroughMessagesDate() {

        for (Message message : messages) {
            if (messageMap.containsKey(message.getDate())) {
                messageMap.get(message.getDate()).add(message);
                sortFromLatestToLast(messageMap.get(message.getDate()));
            } else {
                List<Message> messages = new ArrayList<Message>();
                messages.add(message);
                messageMap.put(message.getDate(), messages);
            }
        }

        if (messageMap == null) {
            Log.d(TAG, "message map is null @ #42");
        }
    }

    public List<Message> getMessages() {
        return messages;
    }

    public HashMap<String, List<Message>> getMessageMap() {
        return messageMap;
    }

}
