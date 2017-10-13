
package com.atakmap.android.QuickChat.history;

import android.content.Context;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.atakmap.android.QuickChat.chat.NewMessageReceiver;
import com.atakmap.android.QuickChat.plugin.R;
import com.atakmap.android.QuickChat.utils.PluginHelper;
import com.atakmap.android.maps.MapView;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Scott Auman on 5/21/2016.
 * This adapter contains the methods used to populate the expandable list view
 * from all the saved chat popup messages
 * @author Scott Auman
 */
public class HistoryAdapter extends BaseExpandableListAdapter {

    private Context context;
    private List<Message> messages = new ArrayList<Message>();
    private List<String> _listDataHeader; // header titles
    private LinearLayout.LayoutParams params;

    // child data in format of header title, child title
    private HashMap<String, List<Message>> _listDataChild;
    private boolean selectedStatus[][]; // array to hold selected state
    private List<MessageViewHolder> holders = new ArrayList<MessageViewHolder>();
    private QuickChatHistoryDropDown.NoMessagesStateListener noMessagesStateListener;

    enum ADAPTER_TYPE {
        DATE,CALLISGN
    }

    public HistoryAdapter(
            Context context, QuickChatHistoryDropDown.NoMessagesStateListener noMessagesStateListenere) {

        this.context = context;
        this.noMessagesStateListener = noMessagesStateListenere;

        _listDataHeader = new ArrayList<String>();
        _listDataChild = new HashMap<String, List<Message>>();

        //build the initial selected state 2 D array
        selectedStatus = new boolean[_listDataHeader.size()][];
        for (int i = 0; i < _listDataHeader.size(); i++) {
            int childSize = _listDataChild.get(_listDataHeader.get(i)).size();
            selectedStatus[i] = new boolean[childSize];
            for (int j = 0; j < childSize; j++) {
                selectedStatus[i][j] = false;
            }
        }
        params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.RIGHT;

        refresh();
    }

    /**
     * when data changes from an add/delete
     * or when loading the adapter data
     * get the data from stored json string
     * sort the messages from early-latest
     * then tell the adapter the data set has changed
     */
    public void refresh() {

        JsonMessageCreator jsonMessageCreator = new JsonMessageCreator();
        messages.clear();
        messages = jsonMessageCreator.parseJsonIntoMessageObjects(
                SavedMessageHistory.getAllMessagesInHistory(MapView
                        .getMapView().getContext()));
        updateTimes(); //update any times that occur this is invoked when creating a new listing
        recreateMessages();

        selectedStatus = new boolean[_listDataHeader.size()][];
        for (int i = 0; i < _listDataHeader.size(); i++) {
            int childSize = _listDataChild.get(_listDataHeader.get(i)).size();
            selectedStatus[i] = new boolean[childSize];
            for (int j = 0; j < childSize; j++) {
                selectedStatus[i][j] = selectedStatus[i][j];
            }
        }

        notifyDataSetChanged();
        this.noMessagesStateListener
                .onStateChanged(getAllMessagesCount() == 0 ? View.VISIBLE
                        : View.GONE);
        this.noMessagesStateListener
                .updateNumberOfMessages(getAllMessagesCount());
    }

    /**when changes to the underlying dataset happens
     * this method clears out all data from child and parent views
     * we recreate the views from scratch instead of trying to insert objects in
     *the dataset does not involve heavy data structures so we can get away with this
     */
    private void recreateMessages() {
        _listDataChild.clear();
        _listDataHeader.clear();

        MessageGrouper messageGrouper = new MessageGrouper(messages);
        _listDataHeader.addAll(messageGrouper.getMessageMap().keySet());
        _listDataChild = messageGrouper.getMessageMap();
    }

    /**Sets the correct value in the selected states array index
     * 1 = selected 0 = not selected
     * this is the only way to keep track of custom selected rows using
     * a custom adapter than standard string arrays or primitive objects
     * @param group
     * @param child
     */
    public void addSelectedItem(int group, int child) {
        selectedStatus[group][child] = !selectedStatus[group][child];
        notifyDataSetChanged();
    }

    /**
     * runs through all messages and returns the number of messages
     * currently stored in memory
     * @return
     */
    public int getAllMessagesCount() {
        return messages.size();
    }

    /**
     * re evaluates all time stamps on messages dispalyed in history drop down
     * when the pref changes
     */
    public void updateTimes() {
        //change all times to 24 hour
        for (Message message : messages) {
            message.setTime(NewMessageReceiver.formatSystemTime(new Date(message
                    .getMessageDateObj())));
        }
        notifyDataSetChanged();
    }

    @Override
    public int getGroupCount() {
        return _listDataHeader.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return _listDataChild.get(_listDataHeader.get(groupPosition)).size();
    }

    @Override
    public String getGroup(int groupPosition) {
        return _listDataHeader.get(groupPosition);
    }

    @Override
    public Message getChild(int groupPosition, int childPosition) {
        return _listDataChild.get(_listDataHeader.get(groupPosition))
                .get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    /**Specifies the view to use when displaying a header
     * @param groupPosition
     * @param isExpanded
     * @param convertView
     * @param parent
     * @return
     */
    @Override
    public View getGroupView(int groupPosition, boolean isExpanded,
            View convertView, ViewGroup parent) {

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(
                    R.layout.history_list_group_row, null);
        }
        TextView groupName = (TextView) convertView
                .findViewById(R.id.lblListHeader);
        groupName.setText(getGroup(groupPosition));
        TextView count = (TextView) convertView.findViewById(R.id.messageCount);
        count.setText("  #"
                + _listDataChild.get(_listDataHeader.get(groupPosition)).size());
        return convertView;
    }

    /**
     * constructs the child view to use when expanding a group
     * the view holds a custom viewholder class that stores the iformation that we
     * use to populate eah rows custom layout
     * the view is checked for null before inflating it again
     * @param groupPosition
     * @param childPosition
     * @param isLastChild
     * @param convertView
     * @param parent
     * @return
     */
    @Override
    public View getChildView(final int groupPosition, final int childPosition,
            boolean isLastChild, View convertView, ViewGroup parent) {

        MessageViewHolder messageViewHolder;

        if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(
                        R.layout.message_history_row, null);

            messageViewHolder = new MessageViewHolder();
            messageViewHolder.message = (TextView) convertView
                    .findViewById(R.id.messageTextView);
            convertView.setTag(messageViewHolder);
            holders.add(messageViewHolder);
        }

        messageViewHolder = (MessageViewHolder) convertView.getTag();
        Message message = getChild(groupPosition, childPosition);

        SpannableString ss1=  new SpannableString(message.getMessage());
        ss1.setSpan(new RelativeSizeSpan(2f), 0,ss1.length(), 0); // set size 125% of the normal size
        //batch the 9 patch image based on message type
        //sent messages need to be aligned on the right

        if(message.getType() == Message.TYPE.RECEIVED){
            messageViewHolder.message.setBackgroundDrawable(PluginHelper.pluginContext.getResources().getDrawable(R.drawable.img_received));
            messageViewHolder.message.setText("From: " + message.getFrom()  + "\n" + "@ " + message.getTime() + "\n\n" + ss1);
            params.gravity = Gravity.LEFT;
        }else{
            messageViewHolder.message.setBackgroundDrawable(PluginHelper.pluginContext.getResources().getDrawable(R.drawable.img_sent));
            messageViewHolder.message.setText("To: " + message.getTo()  + "\n" + "@ " + message.getTime() + "\n\n" + ss1);
            params.gravity = Gravity.RIGHT;
        }
        messageViewHolder.message.setLayoutParams(params);

        return convertView;
    }


    /**runs through all the messages selected states
     * pulls out the states that are marked true and grabs its message object
     * @return
     */
    public List<Message> getAllMessagesSelected() {
        List<Message> messages = new ArrayList<Message>();
        for (int i = 0; i < selectedStatus.length; i++) {
            for (int col = 0; col < selectedStatus[i].length; col++) {
                if (selectedStatus[i][col]) {
                    messages.add(_listDataChild.get(_listDataHeader.get(i))
                            .get(col));
                }
            }
        }

        return messages;
    }

    /**loops through the cureent selected states list

     * @return boolean true | false
     */
    public boolean isItemsSelected() {
        int num = 0;
        for (int i = 0; i < selectedStatus.length; i++) {
            for (int col = 0; col < selectedStatus[i].length; col++) {
                if (selectedStatus[i][col]) {
                    num++;
                }
            }
        }

        return num > 0;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    /**
     * Custom Viewholder to store the listview rows metadata
     * this is the more pratical approach then having the view recycle everytime
     * the getView() creates the viewholder if the view does not have a saved tag associated with it
     * if the tag contains a viewholder instance then dispaly the metadata from the tagged viewholder
     * instead of creating a new one
     */
    public static class MessageViewHolder {
        TextView message;
    }
}
