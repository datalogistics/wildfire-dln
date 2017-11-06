
package com.atakmap.android.QuickChat.chat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;

import com.atakmap.android.QuickChat.history.Message;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.coremap.log.Log;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by Scott Auman on 5/13/2016.
 * Receiver catches the intent sent from ChatManagerMapComponet in ATAK CORE
 * when a new chat message comes in the intent fires and sends the bundle containg all chat metadata
 * to this receiver, the message is parsed and if the user is on the list to receive popup messages
 * a dialog box will fire for every message incoming.
 */
@SuppressWarnings("ALL")
public class NewMessageReceiver extends BroadcastReceiver {

    private final String TAG = getClass().getSimpleName();

    private final QuickChatPopups quickChatPopups;
    private SharedPreferences chatPrefs;
    private QuickChatPopUpDialog quickChatPopUpDialog;
    public static final String CHAT_POPUP_MESSAGING_PLUGIN_BUNDLE_NAME = "chat_bundle";

    public NewMessageReceiver() {

        chatPrefs = PreferenceManager.getDefaultSharedPreferences(MapView
                .getMapView().getContext());
        quickChatPopups = new QuickChatPopups();
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        Bundle bundle = null;

        if (intent != null) {
            bundle = intent
                    .getBundleExtra(CHAT_POPUP_MESSAGING_PLUGIN_BUNDLE_NAME);

            //Coverity issue 18319 bundle could be null EMD
            if (bundle == null) {
                Log.e(TAG, "Bundle is null: "
                        + CHAT_POPUP_MESSAGING_PLUGIN_BUNDLE_NAME);
                return;
            }
            if (bundle.getString("message").equals("")
                    || bundle.getString("message").length() == 0) {
            } else {
                /*
                    check status of pref set for displaying chat messages as popups
                 */
                if (chatPrefs.getBoolean("chat_message_popup_dialog", true)
                        &&
                        SavedFilteredPopupChatUsers.isUserInList(MapView
                                .getMapView().getContext(),
                                getUidFromMessageBundle(bundle))) {

                    //show message or put to backstack
                    showMessagePopUp(bundle);
                }
            }
        }
    }

    private String getUidFromMessageBundle(Bundle message) {
        return message.getString("senderUid");
    }

    IChatPopupStateListener iChatPopupStateListener = new IChatPopupStateListener() {
        @Override
        public void onDialogCanceled(QuickChatPopUpDialog dialog) {
            try {
                //attempt to remove, if dismiss() dialog was last then this will hit the EX
                //causing it to bypass looking for another dialog in backstack and showing that one
                //which does not exist
                quickChatPopups.getDialogs().remove(dialog);
                if (quickChatPopups.getDialogs().size() > 0) {
                    quickChatPopups.reOrderChatsByTime();
                    quickChatPopUpDialog = quickChatPopups
                            .getNextDialogToDisplay(); //find next dialog
                    quickChatPopUpDialog.refreshQ(quickChatPopups
                            .getDialogs().size());
                    quickChatPopUpDialog.show();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    };

    /**
     * Check if a instance of QuickChatPopUpDialog is showing
     * if there is no instance running then create a new dialog and show it
     * if there is create it and store in QuickChatPopups(stores the backstacked messages)
     *
     * @param fullMsg the chat  message metadata
     */
    private void showMessagePopUp(Bundle fullMsg) {

        fullMsg.putString("systemTime", formatSystemTime(new Date()));
        fullMsg.putString("systemDate", formatSystemDate(new Date()));
        fullMsg.putLong("date", new Date().getTime());
        //check if popup is already showing

        if (quickChatPopUpDialog != null) {
            if (quickChatPopUpDialog.getCurrentState() == QuickChatPopUpDialog.DIALOG_STATES.SHOWING) {
                //create new message and add to backstack list
                quickChatPopups.getDialogs().add(
                        createNewChatMessagePopUp(fullMsg));
                quickChatPopUpDialog.refreshQ(quickChatPopups.getDialogs()
                        .size());
            } else {
                quickChatPopUpDialog = createNewChatMessagePopUp(fullMsg);
                quickChatPopUpDialog.setMessageQAmount(1, 1);
                quickChatPopups.getDialogs().add(quickChatPopUpDialog);
                quickChatPopUpDialog.show();
                vibration(); //vibrate device if pref is set up when receiving a new popup message
            }
        } else {
            //create and show new message only instance
            quickChatPopUpDialog = createNewChatMessagePopUp(fullMsg);
            quickChatPopUpDialog.setMessageQAmount(1, 1);
            quickChatPopups.getDialogs().add(quickChatPopUpDialog);
            QuickChatPopups.allPopups.add(quickChatPopUpDialog);
            quickChatPopUpDialog.show();
            vibration(); //vibrate device if pref is set up when receiving a new popup message
        }
    }

    private void vibration() {
        Vibrator v = (Vibrator) MapView.getMapView().getContext()
                .getSystemService(Context.VIBRATOR_SERVICE);
        // Vibrate for 500 milliseconds when receiving a new popup message
        if (chatPrefs.getBoolean("vibrate_on_popup", false))
            v.vibrate(300);
    }

    /**
     * Returns a date string formatted as MM/DD/YYYY
     * the date is the current date when the message popup was recieved/sent
     *
     * @param date
     * @return
     */
    private static String formatSystemDate(Date date) {
        return new SimpleDateFormat("MM/dd/yyyy", LocaleUtil.getCurrent())
                .format(date);
    }

    /**
     * Converts the standard java Date(Calendar) into a more
     * readable instance we have to include the preference for 24 hour clock
     * if 24 hour clock get Calendar HPUR OF DAY Constant , if standard 12 hour clock
     * get instance of Constant HOUR, make sure we add 1 to the 0 index returned
     * see docs: http://developer.android.com/reference/java/util/Calendar.html
     *
     * @param date
     * @return
     */
    public static String formatSystemTime(Date date) {
        Calendar cal = Calendar.getInstance(TimeZone.getDefault());
        cal.setTime(date);

        boolean using24Time = PreferenceManager.getDefaultSharedPreferences(MapView.getMapView().getContext()
        ).getBoolean("popup_24hr_time", true);

        String hour = !using24Time ?
                String.valueOf((cal.get(Calendar.HOUR) == 0) ? 12 : cal
                        .get(Calendar.HOUR)) :
                String.valueOf(cal.get(Calendar.HOUR_OF_DAY));

        String min = String.valueOf(cal.get(Calendar.MINUTE));
        String sec = String.valueOf(cal.get(Calendar.SECOND));
        String am_pm = String.valueOf(cal.get(Calendar.AM_PM));

        if (hour.length() == 1) {
            hour = 0 + hour;
        }
        if (min.length() == 1) {
            min = 0 + min;
        }
        //format the time string to be placed in the bundle
        //if the 24hour time is used then do not attach the time postix
        return hour + ":" + min + ":" + (sec.length() == 1 ? "0" + sec : sec) + " "
                + (!using24Time ? (am_pm.equals("1") ? "PM" : "AM") : "");

    }

    /**
     * Creates a new dialog with supplied information
     *
     * @param fullMsg the COt message bundle
     * @return QuickChatPopUpDialog object
     */
    private QuickChatPopUpDialog createNewChatMessagePopUp(Bundle fullMsg) {

        QuickChatPopUpDialog quickChatPopUpDialog;

        if (chatPrefs.getInt("popup_banner_location", 0) == 1) {
            quickChatPopUpDialog = new
                    QuickChatPopUpDialog(MapView.getMapView().getContext(),
                            com.atakmap.app.R.style.MessagePopUpDialog);
        } else {
            quickChatPopUpDialog = new
                    QuickChatPopUpDialog(MapView.getMapView().getContext());
        }

        quickChatPopUpDialog.setChatDataBundle(fullMsg);
        quickChatPopUpDialog.addDataToDialog();
        quickChatPopUpDialog
                .setChatPopupStateListener(iChatPopupStateListener);
        QuickChatPopups.allPopups.add(quickChatPopUpDialog);

        sendAddMessageToHistoryIntent(messageCreation(quickChatPopUpDialog));
        return quickChatPopUpDialog;
    }

    /**
     * Creates and sends out the required intent to link to
     * the history dropdown receiver class
     * the intent is separated from the initial intent that
     * shows the dropdown
     * this intent just sends the message object out
     * to add it to the list in memory, if the dropdown is showing it is added and shown in list
     *
     * @param message the message object containing all
     *                information needed to display in history
     */
    public static void sendAddMessageToHistoryIntent(Message message) {

        Intent intent = new Intent(
                "com.atakmap.android.QuickChat.ADD_MESSAGE_TO_LIST");
        intent.putExtra("MESSAGE", message);
        AtakBroadcast.getInstance().sendBroadcast(intent);
    }

    /**
     * creates a message object from the information contained in the
     * chatmessagepopupdialog that displays on the screen
     *
     * @param dialog
     * @return Message Object
     */
    public static Message messageCreation(QuickChatPopUpDialog dialog) {
        Message message = new Message();
        message.setDate(dialog.getMessageDate());
        message.setFrom(dialog.getChatDataBundle().getString("senderCallsign"));
        message.setTo("");
        message.setMessage(dialog.getMessageTV().getText().toString());
        message.setTime(dialog.getRecieve_time().getText().toString());
        message.setMessageDateObj(dialog.getChatDataBundle().getLong("date"));
        message.setUid(dialog.getChatDataBundle().getString("senderUid"));
        message.setType(Message.TYPE.RECEIVED);
        return message;
    }

    /**
     * creates a message object from the information contained in the
     * chatmessagepopupdialog that displays on the screen
     *
     * @param dialog
     * @return Message Object
     */
    public static Message messageCreation(Bundle cotBundle) {
        Message message = new Message();
        cotBundle.putString("systemTime", formatSystemTime(new Date()));
        cotBundle.putString("systemDate", formatSystemDate(new Date()));
        cotBundle.putLong("date", new Date().getTime());
        message.setUid(cotBundle.getString("uidMessage"));
        message.setDate(cotBundle.getString("systemDate"));
        message.setTo(cotBundle.getString("uidMessage"));
        message.setFrom("");
        message.setMessage(cotBundle.getString("message"));
        message.setTime(cotBundle.getString("systemTime"));
        message.setMessageDateObj(cotBundle.getLong("sentTime"));
        message.setType(Message.TYPE.SENT);
        return message;
    }
}
