package com.atakmap.android.takchat.data;

import java.util.Comparator;

/**
 * Compares messages for sorting
 * See also <code>{@link ChatMessage}</code>
 *
 * Created by byoung on 11/28/16.
 */
public class ChatMessageComparator implements Comparator<ChatMessage> {

    @Override
    public int compare(ChatMessage lhs, ChatMessage rhs) {
        if(lhs == null)
            return 1;
        else if(rhs == null)
            return -1;

        //sort by time
        return lhs.getTime().compareTo(rhs.getTime());
    }
}
