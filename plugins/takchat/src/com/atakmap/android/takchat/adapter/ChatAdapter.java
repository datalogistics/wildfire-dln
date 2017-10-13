package com.atakmap.android.takchat.adapter;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.atakmap.android.gui.ColorPalette;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapTouchController;
import com.atakmap.android.takchat.TAKChatUtils;
import com.atakmap.android.takchat.data.ChatDatabase;
import com.atakmap.android.takchat.data.ChatMessage;
import com.atakmap.android.takchat.data.ChatMessageComparator;
import com.atakmap.android.takchat.data.MessageLocationLink;
import com.atakmap.android.takchat.data.XmppContact;
import com.atakmap.android.takchat.data.XmppContactComparator;
import com.atakmap.android.takchat.net.ContactManager;
import com.atakmap.android.takchat.net.MessageManager;
import com.atakmap.android.takchat.net.MessageUnreadManager;
import com.atakmap.android.takchat.net.TAKChatXMPP;
import com.atakmap.android.takchat.notification.SoundManager;
import com.atakmap.android.takchat.plugin.R;
import com.atakmap.android.user.PlacePointTool;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.coords.Ellipsoid;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.coremap.maps.coords.MGRSPoint;
import com.atakmap.coremap.maps.time.CoordinatedTime;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.id.StanzaIdUtil;
import org.jxmpp.jid.Jid;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

/**
 * Manages a list of <code>{@link ChatMessage}</code>
 */
public class ChatAdapter extends BaseAdapter {

    private static final String TAG = "ChatAdapter";

    private static final int BACKGROUND_COLOR_UNREAD = 0x16FF0000;
    private static final int BACKGROUND_COLOR_READ = 0x00000000;
    private static final int ALL_UNREAD = -1;
    private static final int ALL_READ = -2;


    private static final ThreadLocal<SimpleDateFormat> DateFormatter = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            SimpleDateFormat sdf = new SimpleDateFormat(
                    "yyyy-MM-dd", LocaleUtil.getCurrent());
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            return sdf;
        }
    };

    private static final ThreadLocal<SimpleDateFormat> TimeFormatter = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            SimpleDateFormat sdf = new SimpleDateFormat(
                    "HH:mm:ss'Z'", LocaleUtil.getCurrent());
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            return sdf;
        }
    };

    private LayoutInflater _inflater = null;
    private ArrayList<ChatMessage> _chatMessageList;
    private ArrayList<ChatMessage> _filteredMessageList;
    private boolean _bDisplaySender;
    private int _newMsgIndex;
    private int _countAtShowing;
    private String _searchTerms;

    /**
     * Initial list of chats, pre-sorted during DB query
     *
     * @param list
     */
    public ChatAdapter(ArrayList<ChatMessage> list) {
        _chatMessageList = list;
        _filteredMessageList = list;
        _bDisplaySender = false;
        _newMsgIndex = -1;
        _countAtShowing = 0;
        _inflater = (LayoutInflater) TAKChatUtils.pluginContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void dispose() {
        if(_chatMessageList != null)
            _chatMessageList.clear();
        if (_filteredMessageList != null)
            _filteredMessageList.clear();
    }

    public void search(String terms) {
        if (!FileSystemUtils.isEmpty(terms))
            terms = terms.toLowerCase();
        _searchTerms = terms;
        refresh();
    }

    @Override
    public int getCount() {
        return _filteredMessageList.size();
    }

    @Override
    public Object getItem(int position) {
        return position;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    class ViewHolder {
        TextView message;
        TextView time;
        ImageView status;

        LinearLayout layout;
        LinearLayout parent_layout;
        View newMessageIndicatorAbove;
        View newMessageIndicatorBelow;
    }

    //private long getViewCount = 0;
    //private long getViewTotal = 0;

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        //long start = System.currentTimeMillis();
        final ChatMessage message = _filteredMessageList.get(position);
        final boolean bIsMine = TAKChatUtils.isMine(message.getMessage());

        View row = convertView;
        ViewHolder holder = null;

        if (row == null) {
            row = _inflater.inflate(R.layout.takchat_bubble, null);

            holder = new ViewHolder();
            holder.message = (TextView) row.findViewById(R.id.message_text);
            holder.time = (TextView) row.findViewById(R.id.bubble_time_sent);
            holder.status = (ImageView) row.findViewById(R.id.bubble_image_status);

            holder.layout = (LinearLayout) row.findViewById(R.id.bubble_layout);
            holder.parent_layout = (LinearLayout) row.findViewById(R.id.bubble_layout_parent);
            holder.newMessageIndicatorAbove = row.findViewById(R.id.bubble_dashed_layout_above);
            holder.newMessageIndicatorBelow = row.findViewById(R.id.bubble_dashed_layout_below);

            row.setTag(holder);
        } else {
            holder = (ViewHolder) row.getTag();
        }

        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMessageOptions(message);
            }
        });
        row.setOnLongClickListener(new View.OnLongClickListener(){

            @Override
            public boolean onLongClick(View v) {
                showMessageOptions(message);
                return true;
            }
        });

        setMessage(holder.message, message);
        String senderTime = TimeFormatter.get().format(new Date(message.getTime()));
        String sendDate = DateFormatter.get().format(new Date(message.getTime()));

        //TODO, cache/resuse this?
        String today = DateFormatter.get().format(CoordinatedTime.currentDate());
        if(!FileSystemUtils.isEquals(today, sendDate)) {
            senderTime += "\n" + sendDate;
        }

        if(_bDisplaySender) {
            if(message.getMessage().getType() == Message.Type.chat &&
                    message.getMessage().getFrom() != null) {
                //chat message
                if(message.getMessage().getFrom().getLocalpartOrNull() != null &&
                        FileSystemUtils.isEquals(message.getMessage().getFrom().getLocalpartOrNull().toString(),
                                TAKChatUtils.getUsernameLocalPart())) {
                    senderTime = "you @ " + senderTime;
                }else{
                    senderTime = message.getMessage().getFrom().getLocalpartOrNull() + " @ " + senderTime;
                }
            }else if(message.getMessage().getType() == Message.Type.groupchat &&
                    message.getMessage().getFrom() != null){
                //group chat message
                if(bIsMine) {
                    senderTime = "you @ " + senderTime;
                }else{
                    senderTime = message.getMessage().getFrom().getResourceOrNull() + " @ " + senderTime;
                }
            }else{
                Log.w(TAG, "Unable to set from: " + message.toString());
            }
        }

        holder.time.setText(senderTime);

        // if message is mine then align to right
        if (bIsMine) {
            holder.message.setBackgroundResource(R.drawable.takchat_bubble2);
            holder.parent_layout.setGravity(Gravity.RIGHT);
            if(message.isDelivered()){
                holder.status.setVisibility(View.VISIBLE);
                holder.status.setImageResource(R.drawable.takchat_receipt_delivered);
            }else if(message.isError()){
                holder.status.setVisibility(View.VISIBLE);
                holder.status.setImageResource(R.drawable.takchat_receipt_error);
            }else{
                //not errored or delivered. just sent
                holder.status.setVisibility(View.GONE);
            }
        } else {
            // If not mine then align to left
            holder.message.setBackgroundResource(R.drawable.takchat_bubble1);
            holder.parent_layout.setGravity(Gravity.LEFT);
            holder.status.setVisibility(View.GONE);
            //msg.setBackgroundColor(Color.LTGRAY);
        }
        holder.message.setTextColor(Color.BLACK);

        //see if we should display the new message above this message
        if(_newMsgIndex == ALL_READ){
            //see if new messages were added after we opened
            if(position == _countAtShowing){
                row.setBackgroundColor(BACKGROUND_COLOR_UNREAD);
                holder.newMessageIndicatorAbove.setVisibility(View.VISIBLE);
                holder.newMessageIndicatorBelow.setVisibility(View.GONE);
            }else if(position > _countAtShowing){
                row.setBackgroundColor(BACKGROUND_COLOR_UNREAD);
                holder.newMessageIndicatorAbove.setVisibility(View.GONE);
                holder.newMessageIndicatorBelow.setVisibility(View.GONE);
            }else{
                row.setBackgroundColor(BACKGROUND_COLOR_READ);
                holder.newMessageIndicatorAbove.setVisibility(View.GONE);
                holder.newMessageIndicatorBelow.setVisibility(View.GONE);
            }
        }else if(_newMsgIndex == ALL_UNREAD){
            row.setBackgroundColor(BACKGROUND_COLOR_UNREAD);
            if(position == 0) {
                holder.newMessageIndicatorAbove.setVisibility(View.VISIBLE);
                holder.newMessageIndicatorBelow.setVisibility(View.GONE);
            }else{
                holder.newMessageIndicatorAbove.setVisibility(View.GONE);
                holder.newMessageIndicatorBelow.setVisibility(View.GONE);
            }
        }else{
            //some read, some unread
            row.setBackgroundColor(_newMsgIndex < position ? BACKGROUND_COLOR_UNREAD : BACKGROUND_COLOR_READ);
            if(_newMsgIndex >= 0 && _newMsgIndex == position) {
                holder.newMessageIndicatorAbove.setVisibility(View.GONE);
                holder.newMessageIndicatorBelow.setVisibility(View.VISIBLE);
            } else {
                holder.newMessageIndicatorAbove.setVisibility(View.GONE);
                holder.newMessageIndicatorBelow.setVisibility(View.GONE);
            }
        }

//        long end = System.currentTimeMillis();
//        getViewCount++;
//        getViewTotal += (end - start);
//        Log.d(TAG, "getView time=" + (end-start) + " ms. avg=" + (getViewTotal/getViewCount) + " ms");

        return row;
    }

    /**
     * Set message to view
     * Any locations in supported formats are made clickable
     *
     * @param view
     * @param message
     */
    private void setMessage(TextView view, ChatMessage message) {
        String body = TAKChatUtils.getBody(message);
        if(!message.hasLocations()){
            view.setText(body);
            return;
        }

        //be sure spannable does not contain '\n' except on paragraph boundary
        Log.d(TAG, "Message location count: " + message.getLocations().size());
        SpannableString spannableString = new SpannableString(body);

        //make all locations underlined and clickable
        for(final MessageLocationLink location : message.getLocations()){
            if(location == null || !location.isValid()){
                Log.w(TAG, "Skipping invalid location");
                continue;
            }

            Log.d(TAG, "Adding link: " + location.toString());
            spannableString.setSpan(
                    new ClickableSpan() {
                        @Override
                        public void onClick(View widget) {
                            onLinkClick(location);
                        }
                    },
                    location.getStartIndex(),
                    (location.getStartIndex() + location.getLinkText().length()),
                    Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        }

        view.setText(spannableString);
        view.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void onLinkClick(MessageLocationLink location) {
        GeoPoint gp = null;
        if(location.getType() == MessageLocationLink.LocationLinkType.MGRS) {
            Log.d(TAG, "Clicked MGRS: " + location.toString());
            gp = MGRSPoint.decodeString(location.getLinkText(),
                    Ellipsoid.WGS_84, null).toUTMPoint(null).toGeoPoint();
        } else if(location.getType() == MessageLocationLink.LocationLinkType.LATLON) {
            Log.d(TAG, "Clicked Lat Long: " + location.toString());
            gp = GeoPoint.parseGeoPoint(location.getLinkText());
        } else {
            Log.w(TAG, "Ignoring location click: " + location.toString());
        }

        if(gp == null || !gp.isValid()){
            Log.w(TAG, "Ignoring invalid location click");
            return;
        }

        //TODO zoom in on point or leave at current zoom level?
        AtakBroadcast.getInstance().sendBroadcast(
                new Intent("com.atakmap.android.maps.ZOOM_TO_LAYER")
                        .putExtra("point", gp.toString()));

        // Find closest item within 5 meters of the location
        MapItem mi = TAKChatUtils.mapView.getRootGroup()
                .deepFindClosestItem(gp, 5, new HashMap<String, String>());
        if (mi == null) {
            Context ctx = TAKChatUtils.mapView.getContext();
            final GeoPoint finalgp = gp;
            /*final SharedPreferences prefs = PreferenceManager
                    .getDefaultSharedPreferences(ctx);
            if (prefs.contains("takchatDropMarkerLink")) {
                if (prefs.getBoolean("takchatDropMarkerLink", false))
                    showDropMarkerChoices(gp);
                return;
            }
            final CheckBox rememberCb = new CheckBox(ctx);
            rememberCb.setText("Remember choice");*/
            new AlertDialog.Builder(ctx).setTitle("Drop Marker")
                    .setMessage("Drop marker at location?")
                    //.setView(rememberCb)
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            showDropMarkerChoices(finalgp);
                            /*if (rememberCb.isChecked())
                                prefs.edit().putBoolean("takchatDropMarkerLink",
                                        true).apply();*/
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            /*if (rememberCb.isChecked())
                                prefs.edit().putBoolean("takchatDropMarkerLink",
                                        false).apply();*/
                        }
                    })
                    .show();
        } else
            MapTouchController.goTo(mi, false);
    }

    private void showDropMarkerChoices(final GeoPoint gp) {
        View v = LayoutInflater.from(TAKChatUtils.pluginContext)
                .inflate(R.layout.takchat_drop_marker_dialog, null);
        ImageButton dropFriendly = (ImageButton) v.findViewById(R.id.dropFriendly),
                dropHostile = (ImageButton) v.findViewById(R.id.dropHostile),
                dropNeutral = (ImageButton) v.findViewById(R.id.dropNeutral),
                dropUnknown = (ImageButton) v.findViewById(R.id.dropUnknown),
                dropSpot = (ImageButton) v.findViewById(R.id.dropSpot);
        final AlertDialog dialog = new AlertDialog.Builder(
                TAKChatUtils.mapView.getContext())
                .setTitle("Drop Marker")
                .setMessage("Select marker type to drop:")
                .setView(v)
                .setNeutralButton("Cancel", null)
                .create();
        View.OnClickListener onClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String type;
                switch (v.getId()) {
                    default:
                    case R.id.dropFriendly:
                        type = "a-f-G";
                        break;
                    case R.id.dropHostile:
                        type = "a-h-G";
                        break;
                    case R.id.dropNeutral:
                        type = "a-n-G";
                        break;
                    case R.id.dropUnknown:
                        type = "a-u-G";
                        break;
                }
                String uid = UUID.randomUUID().toString();
                PlacePointTool.MarkerCreator creator = new PlacePointTool
                        .MarkerCreator(gp)
                        .showCotDetails(false)
                        .setUid(uid)
                        .setType(type);
                MapTouchController.goTo(creator.placePoint(), false);
                dialog.dismiss();
            }
        };

        View.OnClickListener onSpotClick = new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                //ask user for color
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
                        TAKChatUtils.mapView.getContext())
                        .setTitle("Choose Spot Color");
                final ColorPalette palette = new ColorPalette(TAKChatUtils.mapView.getContext(), Color.WHITE);
                dialogBuilder.setView(palette);
                final AlertDialog alert = dialogBuilder.create();

                final ColorPalette.OnColorSelectedListener l = new ColorPalette.OnColorSelectedListener() {
                    @Override
                    public void onColorSelected(int color, String label) {

                        PlacePointTool.MarkerCreator creator = new PlacePointTool
                                .MarkerCreator(gp)
                                .showCotDetails(false)
                                .setUid(UUID.randomUUID().toString())
                                .setType("u-d-p")
                                .setColor(color);
                        MapTouchController.goTo(creator.placePoint(), false);

                        alert.cancel();
                    }
                };

                palette.setOnColorSelectedListener(l);
                alert.show();
                dialog.dismiss();
            }
        };

        dropFriendly.setOnClickListener(onClick);
        dropHostile.setOnClickListener(onClick);
        dropNeutral.setOnClickListener(onClick);
        dropUnknown.setOnClickListener(onClick);
        dropSpot.setOnClickListener(onSpotClick);
        dialog.show();
    }

    private void showMessageOptions(final ChatMessage message) {
        //TODO check if connected prior to sending/fwd/etc

        int resourceId = (TAKChatUtils.isMine(message.getMessage()) && message.isError()) ?
                R.array.conversation_message_resend_array :
                R.array.conversation_message_array;

        final String[] options2 = TAKChatUtils.pluginContext.getResources().getStringArray(resourceId);
        AlertDialog.Builder builder = new AlertDialog.Builder(TAKChatUtils.mapView.getContext());
        builder.setIcon(com.atakmap.app.R.drawable.xmpp_icon)
                .setTitle("Message options: " + TAKChatUtils.getBody(message))
                .setItems(options2, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch(which) {
                            case 0:  //Copy Text
                                copy(message);
                                break;
                            case 1:  //Forward
                                forward(message);
                                break;
                            case 2:  //Remove from history
                                delete(message);
                                break;
                            case 3:  //Resend
                                resend(message);
                                break;
                        }
                    }
                });
        builder.create().show();
    }

    private void forward(final ChatMessage message) {
        if(!TAKChatXMPP.getInstance().isConnected()){
            Log.d(TAG, "Not connected, cannot forward");
            Toast.makeText(TAKChatUtils.mapView.getContext(), "Not connected, cannot forward message", Toast.LENGTH_SHORT).show();
            return;
        }

        //get all contacts except the dest of the original message
        final List<XmppContact> toDisplay = new ArrayList<XmppContact>();
        List<XmppContact> imuutableList = TAKChatUtils.takChatComponent.getManager(ContactManager.class).getContacts(true);
        for(XmppContact c : imuutableList){
            //include all contacts, except the original destination
            //TODO what is proper way to compare the JIDs here?
            if(!FileSystemUtils.isEquals(c.getId().asBareJid().toString(), message.getMessage().getTo().asBareJid().toString())){
                toDisplay.add(c);
            }
        }

        //be sure we have some contacts to display
        if(FileSystemUtils.isEmpty(toDisplay)){
            Log.d(TAG, "No contacts to forward to");
            Toast.makeText(TAKChatUtils.mapView.getContext(), "No other contacts...", Toast.LENGTH_SHORT).show();
            return;
        }

        //now sort and display for user selection
        Collections.sort(toDisplay, new XmppContactComparator());

        XmppContact[] cArray = new XmppContact[toDisplay.size()];
        toDisplay.toArray(cArray);
        final ContactSelectAdapter cAdapter = new ContactSelectAdapter(cArray);

        LayoutInflater inflater = LayoutInflater.from(TAKChatUtils.pluginContext);
        LinearLayout layout = (LinearLayout) inflater.inflate(
                R.layout.takchat_contact_select_list, null);
        ListView listView = (ListView) layout.findViewById(R.id.takchat_contact_list);
        listView.setAdapter(cAdapter);

        AlertDialog.Builder b = new AlertDialog.Builder(TAKChatUtils.mapView.getContext());
        b.setIcon(com.atakmap.app.R.drawable.xmpp_icon);
        b.setTitle("Select contact");
        b.setView(layout);
        b.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        final AlertDialog bd = b.create();
        cAdapter.setOnItemClickListener(new ContactSelectAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(ContactSelectAdapter adapter, XmppContact contact, int position) {
                bd.dismiss();
                if (contact == null) {
                    Toast.makeText(TAKChatUtils.mapView.getContext(), "Failed to forward...",
                            Toast.LENGTH_SHORT).show();
                    Log.w(TAG, "Failed to forward to contact");
                    return;
                }

                //now forward
                forward(contact, message);
            }
        });

        bd.show();
    }

    private void forward(XmppContact contact, ChatMessage message) {
        //TODO confirm with user prior to FWD?

        Message fwd = new Message(message.getMessage());
        fwd.setType(TAKChatUtils.isConference(contact) ? Message.Type.groupchat : Message.Type.chat);
        fwd.setTo(contact.getId());
        fwd.setStanzaId(StanzaIdUtil.newStanzaId());
        ChatMessage wrapper = new ChatMessage(fwd);

        Log.d(TAG, "Forwarding: " + wrapper.toString() + ", to: " + contact.toVerboseString());
        //TODO with this approach, Smack is resetting the type to "chat" not "groupchat" so fwd'ing to a group is failing
        TAKChatUtils.takChatComponent.getManager(MessageManager.class).sendChat(wrapper);

        // TODO display that contact's chat view?
    }

    private static void copy(ChatMessage message) {
        ClipboardManager clipboard = (ClipboardManager) TAKChatUtils.mapView
                .getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(TAKChatUtils.getPluginString(R.string.app_name), TAKChatUtils.getBody(message));
        clipboard.setPrimaryClip(clip);

        Toast.makeText(TAKChatUtils.mapView.getContext(), "Copied to clipboard...", Toast.LENGTH_SHORT).show();
    }

    private static void resend(final ChatMessage message) {
        if(!TAKChatXMPP.getInstance().isConnected()){
            Log.d(TAG, "Not connected, cannot resend");
            Toast.makeText(TAKChatUtils.mapView.getContext(), "Not connected, cannot resend message", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(TAKChatUtils.mapView.getContext())
                .setTitle("Confirm Resend")
                .setMessage("Resend message now?")
                .setIcon(com.atakmap.app.R.drawable.ic_menu_send)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //TODO update the timestamp on the message in the DB/UI, move it to bottom of UI list?
                        Log.d(TAG, "Resending: " + message.toString());
                        TAKChatUtils.takChatComponent.getManager(MessageManager.class).sendChat(message);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void delete(final ChatMessage message) {
        new AlertDialog.Builder(TAKChatUtils.mapView.getContext())
                .setTitle("Confirm Delete")
                .setMessage("Remove from local history?")
                .setIcon(com.atakmap.app.R.drawable.ic_menu_delete_32)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(TAG, "Deleting: " + message.toString());
                        if(ChatDatabase.getInstance(TAKChatUtils.pluginContext).deleteMessage(message)){
                            remove(message);
                        }else{
                            Log.w(TAG, "Failed to delete: " + message.toString());
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }


    /**
     * Call from UI thread only. chat list is not synchronized
     * @param message
     */
    public void add(ChatMessage message) {
        if(_chatMessageList.contains(message)){
            Log.d(TAG, "Ignore duplicate message: " + message.toString());
            return;
        }

        if(!message.isRead() && !TAKChatUtils.isMine(message.getMessage())){
            SoundManager.getInstance().play(SoundManager.SOUND.RECEIVE);
        }

        _chatMessageList.add(message);
        refresh();
    }

    /**
     * Call from UI thread only. chat list is not synchronized
     * @param message
     */
    private void remove(ChatMessage message) {
        if(_chatMessageList.remove(message)) {
            Log.d(TAG, "Removing: " + message);
            refresh();
        }else{
            Log.w(TAG, "Failed to remove UI message: " + message.toString());
        }
    }

    public void refresh() {
        // Filter
        final ArrayList<ChatMessage> filtered = new ArrayList<ChatMessage>();
        if (!FileSystemUtils.isEmpty(_searchTerms)) {
            for (ChatMessage msg : _chatMessageList) {
                if (TAKChatUtils.searchMessage(msg, _searchTerms))
                    filtered.add(msg);
            }
        } else {
            filtered.addAll(_chatMessageList);
        }

        Collections.sort(filtered, new ChatMessageComparator());
        TAKChatUtils.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //sort by time
                _filteredMessageList = filtered;
                notifyDataSetChanged();
            }
        });
    }

    public void onDeliveryReceipt(final Jid from, final Jid to, final String deliveryReceiptId, final Stanza stanza) {
        TAKChatUtils.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for(ChatMessage cur : _chatMessageList){
                    if(FileSystemUtils.isEquals(cur.getMessage().getStanzaId(), deliveryReceiptId)){
                        cur.setError(false);
                        cur.setDelivered(true);
                        refresh();
                        return;
                    }
                }

                Log.w(TAG, "Unable to set delivery flag for: " + stanza.toString());
            }
        });
    }

    public void onDeliveryError(final ChatMessage message) {
        TAKChatUtils.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for(ChatMessage cur : _chatMessageList){
                    if(cur.equals(message)){
                        cur.setError(true);
                        cur.setDelivered(false);
                        refresh();
                        return;
                    }
                }

                Log.w(TAG, "Unable to set error flag for: " + message.toString());
            }
        });
    }

    public void setDisplaySender(boolean _bDisplaySender) {
        //TODO synchronized?
        this._bDisplaySender = _bDisplaySender;
        refresh();
    }

    /**
     * This chat/convo is being displayed
     * Set which message is last read, and then mark all messages as read
     *
     * @return true if at least one message was marked as read
     */
    public boolean showing() {
        _newMsgIndex = 0;
        _countAtShowing = _chatMessageList.size();
        boolean updated = false;

        if(FileSystemUtils.isEmpty(_chatMessageList)){
            Log.d(TAG, "No messages");
            return updated;
        }

        boolean bFirstMessageRead = _chatMessageList.get(0).isRead();
        for(int i = 0; i < _chatMessageList.size(); i++) {
            ChatMessage message = _chatMessageList.get(i);
            if(!message.isRead()) {
                //mark it as read
                message.setRead(true);
                //TODO support batch processing for DB, listeners, intents
                TAKChatUtils.takChatComponent.getManager(MessageUnreadManager.class).messageRead(message);
                updated = true;
            }else{
                //this message has been read previously
                _newMsgIndex = i;
            }
        }

        //check if all unread
        if(_newMsgIndex == 0 && !FileSystemUtils.isEmpty(_chatMessageList) && !bFirstMessageRead){
            Log.d(TAG, "All messages unread, count: " + _countAtShowing);
            _newMsgIndex = ALL_UNREAD;
        } else if(_newMsgIndex == _chatMessageList.size()-1){
            Log.d(TAG, "All messages read, count: " + _countAtShowing);
            _newMsgIndex = ALL_READ;
        }else {
            Log.d(TAG, "new msg index: " + _newMsgIndex + ", count: " + _countAtShowing);
        }
        search("");
        return updated;
    }
}