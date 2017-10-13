
package com.atakmap.android.QuickChat.chat;

import android.app.Activity;
import android.app.Application;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.method.ScrollingMovementMethod;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.atakmap.android.QuickChat.components.QuickChatMapComponent;
import com.atakmap.android.QuickChat.plugin.R;
import com.atakmap.android.QuickChat.preferences.QuickChatPreferenceFragment;
import com.atakmap.android.QuickChat.utils.PluginHelper;
import com.atakmap.android.chat.ChatManagerMapComponent;
import com.atakmap.android.contact.Contact;
import com.atakmap.android.contact.Contacts;
import com.atakmap.android.contact.IndividualContact;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.time.CoordinatedTime;

/**
 * Created by Scott Auman on 4/4/2016.
 * a android GUI dialog box that displays the current chat message incoming
 * from a specific user that this user deemed as true to receive the message as a popup
 * FEATURES:
 * callsign: clicked opens up conversation fragment with user
 * every instance of the dialogbox is created, incoming dialogs are put into a Q backstacks
 * and displayed at a later time or when this dialog is dismissed
 * this contains a custom shape layout file (drawable) catching the user's attention
 * and drops down from the top-center of the screen, without impeding the map or anything else the user is doing.
 */
public class QuickChatPopUpDialog extends Dialog {

    private TextView senderTV, messageTV, numberQ, recieve_time;
    private DIALOG_STATES currentState;
    private Bundle chatDataBundle;
    private String senderUid;
    private String message;
    private Button acknowledgedButton;
    private IChatPopupStateListener chatPopupStateListener;
    private TableRow reply, send;
    private EditText replyEditText;
    private final String TAG = getClass().getName();
    private SharedPreferences _prefs;

    /*
        handles the current state of the dialog
        used to check if active or deactivated
     */
    public enum DIALOG_STATES {
        SHOWING, DISMISSED
    }

    @Override
    public void setCanceledOnTouchOutside(boolean cancel) {
        super.setCanceledOnTouchOutside(false);
    }

    @Override
    public void setCancelable(boolean flag) {
        super.setCancelable(false);
    }

    public String getMessageDate() {
        return getChatDataBundle().getString("systemDate");
    }

    /**
     * @param a the number of messages in the list
     *          called to update the textview showing the numbers of messages
     */
    public void refreshQ(int a) {
        numberQ.setText("1/" + a);
    }

    /**
     * @param context The context to use.  Usually your {@link Application}
     *                or {@link Activity} object.
     * @param theme   - the custom theme to apply to dialog
     */
    QuickChatPopUpDialog(Context context, int theme) {
        super(context, theme);

        //custom themed set layout window params to top of screen
        Window window = this.getWindow();
        if (window != null) {
            WindowManager.LayoutParams wlp = window.getAttributes();
            wlp.gravity = Gravity.TOP;
            getWindow().setAttributes(wlp);
            init();
        }
    }

    private String getUidFromMessageBundle(Bundle message) {
        return message.getString("senderUid");
    }

    /**
     * @param context The context to use.  Usually your {@link Application}
     *                or {@link Activity} object.
     */
    QuickChatPopUpDialog(Context context) {
        super(context);
        init();
    }

    private void init() {
        requestWindowFeature(Window.FEATURE_NO_TITLE); //remove title bar
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        calculateNumberOfLines();

        //instance of Layout inflater to inflate dialog xml view
        LayoutInflater inflater =
                (LayoutInflater) PluginHelper.pluginContext.
                        getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        //do not allow dismiss or cancel ability for dialog
        setCanceledOnTouchOutside(false);
        setCancelable(false);

        final View inflate = PreferenceManager
                .getDefaultSharedPreferences(MapView.getMapView()
                        .getContext())
                .getString("chat_message_popups_style", "0").equals("0") ?
                inflater.inflate(R.layout.chat_messsage_popup_dialog_white,
                        null, false)
                : inflater.inflate(R.layout.chat_messsage_popup_dialog_black,
                        null, false);

        setContentView(inflate);
        assignElements(inflate);

        //attach onclicklisteners to views
        senderTV.setOnClickListener(goToChatListener); //go straight to conversation fragment for user

        //when dialog is dismissed/cancelled change the current state and tell the calling class about
        //this change in state
        setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                currentState = DIALOG_STATES.DISMISSED;
                chatPopupStateListener.onDialogCanceled(getObjectInstance());
            }
        });

        //disables the dim background when showing this dialog on screen
        //android basic guidelines darkens the background behind the dialog
        //with a map being viewed or any other module lets make sure not to cover up anything!
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

        _prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
    }

    private void assignElements(View inflate) {

        //set XML widgets to java variables
        recieve_time = (TextView) inflate
                .findViewById(R.id.receiveTimeTextView);
        senderTV = (TextView) inflate
                .findViewById(R.id.message_sender_textview);
        messageTV = (TextView) inflate
                .findViewById(R.id.message_popup_dialog_contents);
        numberQ = (TextView) inflate
                .findViewById(R.id.number_message_in_q_textview);
        Button dismissButton = (Button) inflate
                .findViewById(R.id.dismissButton);
        Button replyButton = (Button) inflate.findViewById(R.id.replyButton);
        Button sendButton = (Button) inflate.findViewById(R.id.sendButton);
        acknowledgedButton = (Button) inflate
                .findViewById(R.id.acknowledgedButton);
        ImageButton revertButton = (ImageButton) inflate
                .findViewById(R.id.revertButton);

        reply = (TableRow) inflate.findViewById(R.id.replyTableRow);
        send = (TableRow) inflate.findViewById(R.id.customButtonTableRow);

        replyButton.setOnClickListener(dialogWidgetsClickListener);
        sendButton.setOnClickListener(dialogWidgetsClickListener);
        acknowledgedButton.setOnClickListener(dialogWidgetsClickListener);
        revertButton.setOnClickListener(dialogWidgetsClickListener);

        replyEditText = (EditText) inflate.findViewById(R.id.replyEditText);

        dismissButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        reply.setVisibility(View.GONE);
        acknowledgedButton.setText(getSavedQuickReplyString());

        setTextButtonSize(acknowledgedButton);
        setTextButtonSize(replyButton);
        setTextButtonSize(dismissButton);
    }

    private void setTextButtonSize(Button button) {
        button.setTextAppearance(
                getContext(),
                PluginHelper.isTablet() ? android.R.style.TextAppearance_Holo_Medium
                        :
                        android.R.style.TextAppearance_Holo_Small);
        button.setTextColor(Color.WHITE);
    }

    private final View.OnClickListener dialogWidgetsClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int id = v.getId();
            switch (id) {
                case R.id.replyButton:
                    send.setVisibility(View.GONE);
                    reply.setVisibility(View.VISIBLE);
                    replyEditText.requestFocus();
                    break;
                case R.id.acknowledgedButton:
                    //send chat message to callsign "Acknowledged"
                    boolean a = sendMessage(createChatBundle(acknowledgedButton
                            .getText().toString()), new String[] {
                        getSenderUid()
                    });
                    Log.d(TAG, "Sending message was successful " + a);
                    if (a) {
                        cancel();
                    }
                    break;
                case R.id.sendButton:
                    //send the supplied text in edittext to callisgn user
                    if (testForInput(replyEditText)) {
                        boolean b = sendMessage(createChatBundle(replyEditText
                                .getText().toString()),
                                new String[] {
                                    getSenderUid()
                                });
                        Log.d(TAG, "Sending message was successful " + b);
                        if (b) {
                            cancel();
                        }
                    } else {
                        Toast.makeText(MapView.getMapView().getContext(),
                                "Cannot Send An Empty Message",
                                Toast.LENGTH_SHORT).show();
                    }
                    break;
                case R.id.revertButton:
                    send.setVisibility(View.VISIBLE);
                    reply.setVisibility(View.GONE);
                    break;
            }
        }
    };

    private String getSavedQuickReplyString() {
        return PreferenceManager.getDefaultSharedPreferences(
                MapView.getMapView().getContext()).getString
                (QuickChatPreferenceFragment.QUICK_REPLY_TEXT_KEY,
                        QuickChatPreferenceFragment.ACKNOWLEDGED);
    }

    private boolean testForInput(Object object) {
        boolean empty = false;

        if (object instanceof EditText) {
            EditText editText = (EditText) object;
            empty = !editText.getText().toString().equals("");
        }

        return empty;
    }

    /**Create a chat line bundle that describes a message object
     * to send to the chatservice
     * @return Bundle chat descriptions
     */
    private Bundle createChatBundle(String message) {

        Bundle msg = new Bundle();

        String selfUID = MapView.getDeviceUid();
        String selfCallsign = MapView.getMapView().getDeviceCallsign();
        msg.putString("senderCallsign", selfCallsign);
        msg.putString("senderUid", selfUID);
        msg.putString("uid", selfUID);
        msg.putString("uidMessage",chatDataBundle.getString("senderCallsign"));
        msg.putString("deviceType", MapView.getMapView().getMapData()
                .getString("deviceType"));
        msg.putString("protocol", "CoT");
        msg.putLong("sentTime", (new CoordinatedTime())
                .getMilliseconds());
        msg.putString("type", "GeoChat");
        msg.putString("message", message);
        msg.putString("conversationId", chatDataBundle.getString("conversationId"));
        msg.putString("conversationName", chatDataBundle.getString("conversationName"));
        msg.putString("parent", chatDataBundle.getString("parent"));
        msg.putBoolean("groupOwner", chatDataBundle.getBoolean("groupOwner"));
        msg.putStringArray("destinations", new String[0]);

        return msg;
    }

    private boolean sendMessage(Bundle msg, String[] recipients) {

        Contacts _contacts = Contacts.getInstance();
        for (String dest : recipients) {
            Contact c = _contacts.getContactByUuid(dest);
            if (c != null && c instanceof IndividualContact) {
                IndividualContact contact = (IndividualContact) c;
                try {
                    if (QuickChatMapComponent.chatService == null) {
                        Toast.makeText(
                                MapView.getMapView().getContext(),
                                "Could Not Connect To Server, Unable To Send Message",
                                Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    QuickChatMapComponent.addSentMessageToDB(msg);
                    QuickChatMapComponent.sendMessageOut(msg,contact);

                    //add cot sent bundle to history add intent we must create
                    // a new message object from cot bundle we are sending out
                    NewMessageReceiver.sendAddMessageToHistoryIntent
                            (NewMessageReceiver.messageCreation(msg));
                } catch (Exception e) {
                    Log.w(TAG,
                            "Error sending message via Chat Service: "
                                    + e.getMessage());
                    Toast.makeText(MapView.getMapView().getContext(),
                            "Couldn't figure out where to send chat to!",
                            Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * @return the chatmessagepopupdialog object contained as this class instance
     */
    private QuickChatPopUpDialog getObjectInstance() {
        return this;
    }

    /**
     * @return the number of lines to display on the device
     * tablets have larger width and height so give them more lines to display
     * phones have smaller height and width give them < 3   tablets > 3
     */
    private int calculateNumberOfLines() {

        if (PluginHelper.isTablet()) {
            return 7;
        }
        return 5;
    }

    /**
     * sets the data from the bundle provided by the chat incoming receiver
     */
    public void addDataToDialog() {

        //extract message and sender information from bundle
        senderTV.setText("Callsign: "
                + chatDataBundle.getString("senderCallsign"));
        messageTV.setText(chatDataBundle.getString("message"));
        recieve_time.setText(chatDataBundle.getString("systemTime"));

        reSizeText(messageTV); //resize text based on length
        messageTV.setMovementMethod(new ScrollingMovementMethod()); //enables scrolling inside of textview contents
        messageTV.setMaxLines(calculateNumberOfLines()); //based on device set the max lines option for scrolling

        setSenderUid(getUidFromMessageBundle(chatDataBundle));
        setMessage(chatDataBundle.getString("message"));
    }

    public void setMessageQAmount(int a, int b) {
        numberQ.setText(a + "/" + b);
    }

    /**
     * Re sizes text view's text based on size of chat messsage
     * SIZES: <75 Large text < 150 Medium   > 150 small
     *
     * @param tv the textview with the message contents
     */
    private void reSizeText(TextView tv) {

        int messageCount = tv.getText().toString().length();
        if (PreferenceManager.getDefaultSharedPreferences(getContext())
                .getBoolean("popup_text_message_size", true)) {
            //user wants larger text style
            tv.setTextAppearance(MapView.getMapView().getContext(),
                    android.R.style.TextAppearance_DeviceDefault_Large);
        } else {
            if (messageCount < 50) {
                tv.setTextAppearance(MapView.getMapView().getContext(),
                        android.R.style.TextAppearance_DeviceDefault_Large);
            } else if (messageCount < 100) {
                tv.setTextAppearance(MapView.getMapView().getContext(),
                        android.R.style.TextAppearance_DeviceDefault_Medium);
            } else {
                tv.setTextAppearance(MapView.getMapView().getContext(),
                        android.R.style.TextAppearance_DeviceDefault_Small);
            }
        }

        //set text based upon theme
        tv.setTextColor((PreferenceManager.getDefaultSharedPreferences(MapView
                .getMapView().getContext()).getString(
                "chat_message_popups_style", "0")).equals("1") ?
                Color.parseColor("#FFFFFF") : Color.parseColor("#000000"));

    }

    /**
     * displays the dialog on screen
     * layout params are center vertical | center horizontal
     */
    @Override
    public void show() {
        super.show();
        setCurrentState(DIALOG_STATES.SHOWING);
        markAsRead();
    }

    /**
     * send chat bundle out as intent to catch to receiver in
     * ChatManagerMapComponet.java (core)
     */
    private void markAsRead() {
        if (_prefs.getBoolean("popup_mark_message_read", true)) {
            //mark message as read for specific receiver callsign
            final Intent intent = new Intent("com.atakmap.chat.markmessageread");
            intent.putExtra("chat_bundle", getChatDataBundle());
            AtakBroadcast.getInstance().sendBroadcast(intent);
        }
    }

    /**
     * event listener - user clicked the callsign of the message sender
     */
    private View.OnClickListener goToChatListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            //launch chat and go to personal chat with sender_callsign
            Contact contact = Contacts.getInstance().getContactByUuid
                    (getUidFromMessageBundle(chatDataBundle));
            openChatWindowIntent(contact);
            //dismiss banner for this contact only!
            cancel();
        }
    };

    /**Creates the intent and attaches intent to broadcast receiver
     * to open up conversation fragment with supplied UID
     * @param contact The Contact object containing the sender info(uuid,callsigns etc.)
     */
    private void openChatWindowIntent(Contact contact) {

        if (contact instanceof IndividualContact) {
            IndividualContact individualContact = (IndividualContact) contact;
            ChatManagerMapComponent.getInstance().openConversation(
                    individualContact, true);
        }
    }

    public TextView getSenderTV() {
        return senderTV;
    }

    public TextView getMessageTV() {
        return messageTV;
    }

    public TextView getRecieve_time() {
        return recieve_time;
    }

    public DIALOG_STATES getCurrentState() {
        return currentState;
    }

    private void setCurrentState(DIALOG_STATES currentState) {
        this.currentState = currentState;
    }

    public Bundle getChatDataBundle() {
        return chatDataBundle;
    }

    public void setChatDataBundle(Bundle chatDataBundle) {
        this.chatDataBundle = chatDataBundle;
    }

    private String getSenderUid() {
        return senderUid;
    }

    private void setSenderUid(String senderUid) {
        this.senderUid = senderUid;
    }

    public String getMessage() {
        return message;
    }

    private void setMessage(String message) {
        this.message = message;
    }

    public void setChatPopupStateListener(
            IChatPopupStateListener chatPopupStateListener) {
        this.chatPopupStateListener = chatPopupStateListener;
    }
}
