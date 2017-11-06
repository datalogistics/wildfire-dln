
package com.atakmap.android.QuickChat.chat;

/**
 * Created by AumanS on 4/7/2016.
 * used to navigate when current dialogs are dismissed
 * as well as if user touched the cancelAll button
 */
public interface IChatPopupStateListener {
    void onDialogCanceled(QuickChatPopUpDialog quickChatPopUpDialog);
}
