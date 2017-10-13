package com.atakmap.android.takchat.data;

/**
 * Listens for changes to state of unread messages
 *
 * Created by byoung on 10/25/2016.
 */
public interface MessageUnreadListener extends IListener {

    /**
     * Message has been read by local user
     * @param message
     */
    void onMessageRead(ChatMessage message);


    /**
     * Number of unread messages has changed
     */
    void onUnreadCountChanged();
}
