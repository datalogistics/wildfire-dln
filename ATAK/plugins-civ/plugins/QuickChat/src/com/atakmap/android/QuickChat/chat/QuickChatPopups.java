
package com.atakmap.android.QuickChat.chat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by AumanS on 4/7/2016.
 * handles the ordering and state of each popup message received
 * saves all messages incoming (filtered users of course)
 *
 */
class QuickChatPopups {

    private final List<QuickChatPopUpDialog> dialogs = new ArrayList<QuickChatPopUpDialog>();
    public static final List<QuickChatPopUpDialog> allPopups = new ArrayList<QuickChatPopUpDialog>();

    public List<QuickChatPopUpDialog> getDialogs() {
        return dialogs;
    }

    /**checks if the backstack contains any dialogs left
     * returns the next dialog is != 0
     */
    public QuickChatPopUpDialog getNextDialogToDisplay() {
        if (dialogs.size() > 0) {
            return (getDialogs()).get(0);
        }
        return null;
    }

    /**
     * re sorts all dialogs based on recieve/send time
     * making sure the oldest timestamps are shown first
     */
    public void reOrderChatsByTime() {

        //sort all dialog objects in set by oldest --> to newest insuring user receives all messages
        if (getDialogs().size() > 0) {
            Collections.sort(getDialogs(),
                    new Comparator<QuickChatPopUpDialog>() {

                        @Override
                        public int compare(QuickChatPopUpDialog lhs,
                                QuickChatPopUpDialog rhs) {
                            //compare by chat message time
                            long timeA = lhs.getChatDataBundle()
                                    .getLong("date");
                            long timeB = lhs.getChatDataBundle()
                                    .getLong("date");

                            return Long.valueOf(timeA).compareTo(
                                    Long.valueOf(timeB));
                        }
                    });
        }
    }
}
