package com.atakmap.android.takchat.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.LayerDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.takchat.TAKChatUtils;
import com.atakmap.android.takchat.data.ChatDatabase;
import com.atakmap.android.takchat.data.ChatMessage;
import com.atakmap.android.takchat.data.ContactListener;
import com.atakmap.android.takchat.data.MessageUnreadListener;
import com.atakmap.android.takchat.data.XmppConference;
import com.atakmap.android.takchat.data.XmppContact;
import com.atakmap.android.takchat.net.ConnectionManager;
import com.atakmap.android.takchat.net.ConnectionSettings;
import com.atakmap.android.takchat.net.ContactManager;
import com.atakmap.android.takchat.net.TAKChatXMPP;
import com.atakmap.android.takchat.plugin.R;
import com.atakmap.android.takchat.view.TAKChatView;
import com.atakmap.android.takchat.view.TAKConferenceView;
import com.atakmap.android.takchat.view.TAKContactProfileView;
import com.atakmap.android.takchat.view.badge.AtakLayerDrawableUtil;
import com.atakmap.android.takchat.view.badge.BadgeDrawable;
import com.atakmap.android.util.ATAKUtilities;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;

import org.jivesoftware.smack.packet.Presence;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages a list of contacts, tracked by XMPP JID
 *
 * Created by scallya on 7/29/2016.
 */
public class ContactListAdapter extends BaseAdapter implements ContactListener, MessageUnreadListener {

    private static final String TAG = "ContactListAdapter";

    public static final int BACKGROUND_COLOR_ALIVE = 0x4F009900;
    public static final int BACKGROUND_COLOR_STALE = 0x4FFFFF66;
    public static final int BACKGROUND_COLOR_DEAD = 0x4FFF3333;
    public static final int BACKGROUND_COLOR_CONFERENCE = 0xF42E538B;

    private ContactManager _contacts;
    private List<XmppContact> _listToDisplay;
    private final SharedPreferences _prefs;
    private int _totalUsers;
    private boolean _bAllUsersOnline;
    private String _searchTerms;

    public ContactListAdapter(SharedPreferences prefs) {
        _prefs = prefs;
        _contacts = TAKChatUtils.takChatComponent.getManager(ContactManager.class);
        _listToDisplay = new ArrayList<XmppContact>();
        _totalUsers = 0;
        _bAllUsersOnline = false;
    }

    @Override
    public void dispose() {
        if(_listToDisplay != null){
            _listToDisplay.clear();
        }
    }

    /**
     * TODO:
     *
     * Mechanic to specify Portrait
     * base default portrait on client software
     *   ATAK = ATAK logo
     *   WinTAK = WinTAK logo
     *   all others = default XMPP icon?
     *
     * Allow the user to view a "contact card"
     *
     * we need to add a mechanic for sorting
     *
     * Add a mechanic to allow for Nameing of contacts if they dont specify one (long press?)
     * Include check box to make change ignore external updates
     * Update name if updated externally
     * Default name?
     */
    public void redrawList() {

        //TODO should we force this onto background thread?

        //get contacts to display and sort here
        boolean showOffline = _prefs.getBoolean("takchatShowOffline", false);
        Log.d(TAG, "redrawList: " + showOffline);

        List<XmppContact> temp = _contacts.getContacts(true);
        final List<XmppContact> toDisplay = new ArrayList<XmppContact>();

        //cache data to be used for "extra" view"
        _bAllUsersOnline = true;
        _totalUsers = temp.size();
        for(XmppContact contact : temp){
            _bAllUsersOnline &= (contact != null && contact.isAvailable());
        }

        // Filter
        for (XmppContact c : temp) {
            if (c != null && (showOffline || c.isAvailable())
                    && TAKChatUtils.searchContact(c, _searchTerms))
                toDisplay.add(c);
        }


        //look up current unread counts to be used for sorting/comparisons
        Map<String, Integer> unread = new HashMap<String, Integer>();
        ChatDatabase db = ChatDatabase.getInstance(TAKChatUtils.pluginContext);
        for(XmppContact c : toDisplay){
            unread.put(c.getId().toString(), db.getUnreadCount(c.getId()));
        }

        Collections.sort(toDisplay, new ContactComparator(unread));

        //now redraw on UI thread
        TAKChatUtils.runOnUiThread(new Runnable() {
            @Override
            public void run(){
                _listToDisplay.clear();
                _listToDisplay.addAll(toDisplay);
                notifyDataSetChanged();
            }
        });
    }

    private class ContactComparator implements Comparator<XmppContact> {

        private final Map<String, Integer> _unread;

        public ContactComparator(Map<String, Integer> unread){
            _unread = unread;
        }

        @Override
        public int compare(XmppContact contact1, XmppContact contact2) {

            if(contact1 == null || FileSystemUtils.isEmpty(contact1.getName()))
                return 1;
            else if(contact2 == null || FileSystemUtils.isEmpty(contact2.getName()))
                return -1;


            Integer lcount = _unread.get(contact1.getId().toString());
            Integer rcount = _unread.get(contact2.getId().toString());
            if(lcount == null)
                return 1;
            else if(rcount == null)
                return -1;

            if(lcount.intValue() != rcount.intValue()){
                return lcount.intValue() > rcount.intValue() ? -1 : 1;
            }

            //first sort by status
            if(contact1.isAvailable() != contact2.isAvailable()){
                return contact1.isAvailable() ? -1 : 1;
            }
            if(contact1.isAway() != contact2.isAway()){
                return contact1.isAway() ? 1 : -1;
            }

            //then by name (if both are contacts or conferences)
            if(TAKChatUtils.isConference(contact1) == TAKChatUtils.isConference(contact2)){
                return contact1.getName().compareTo(contact2.getName());
            }

            //finally put confs above contacts
            return TAKChatUtils.isConference(contact1) ? -1 : 1;
        }
    };

    @Override
    public boolean onPresenceChanged(Presence presence) {
        redrawList();
        return true;
    }

    @Override
    public boolean onContactSizeChanged() {
        redrawList();
        return true;
    }

    @Override
    public int getCount() {
        return _listToDisplay.size() + 1;
    }

    @Override
    public Object getItem(int position) {
        if(position >= _listToDisplay.size())
            return null;

        return _listToDisplay.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public void onMessageRead(ChatMessage message) {
        //no-op
    }

    @Override
    public void onUnreadCountChanged() {
        redrawList();
    }

    public void search(String terms) {
        if (!FileSystemUtils.isEmpty(terms))
            terms = terms.toLowerCase();
        _searchTerms = terms;
        redrawList();
    }

    private static class ViewHolder {
        public ImageView icon = null;
        public TextView alias = null;
        public TextView detail = null;
        public ImageButton pan = null;
        public ImageButton vcard = null;
        public TextView markerCallsign = null;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if(position >= _listToDisplay.size()){
            //Log.d(TAG, "Creating search button view for position: "
            //        + position);
            convertView = getExtraRow(parent);
            return convertView;
        }

        ViewHolder holder = null;
        if (convertView == null || convertView.getTag() == null) {
            final LayoutInflater inflater = (LayoutInflater) TAKChatUtils.pluginContext.
                getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.takchat_contact_list_item, parent, false);

            holder = new ViewHolder();

            holder.alias = (TextView) convertView
                    .findViewById(R.id.contact_list_item_alias);

            holder.detail = (TextView) convertView
                    .findViewById(R.id.contact_list_item_detail);

            holder.markerCallsign = (TextView) convertView
                    .findViewById(R.id.contact_list_item_callsign);

            holder.icon = (ImageView) convertView.findViewById(R.id.contact_image);
            holder.pan = (ImageButton) convertView.findViewById(R.id.contact_list_item_pan_button);
            holder.pan.setFocusable(false);
            holder.vcard = (ImageButton) convertView.findViewById(R.id.contact_list_item_vcard_button);
            holder.vcard.setFocusable(false);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        updateViewHolder(holder, convertView, position);

        return convertView;
    }

    private enum EXTRA_ROW_MODE{DISABLED, CONFIG, RECONNECTNOW, ADDCONTACT, TOGGLEOFFLINE};

    private View getExtraRow(ViewGroup parent) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        final View convertView = inf.inflate(
                R.layout.takchat_contactlist_extrarow, null);

        final boolean curToggle = _prefs.getBoolean("takchatShowOffline", false);
        EXTRA_ROW_MODE mode = EXTRA_ROW_MODE.TOGGLEOFFLINE;
        String message = null;
        ConnectionSettings config = TAKChatUtils.takChatComponent.getManager(ConnectionManager.class).getConfig();
        if(!_prefs.getBoolean("takchatEnabled", true)) {
            //chat is disabled
            message = "Tap to enable chat";
            mode = EXTRA_ROW_MODE.DISABLED;
        }else if(config == null || !config.isValid()){
            //not configured properly
            message = "Tap to configure connection";
            mode = EXTRA_ROW_MODE.CONFIG;
        } else if (!TAKChatXMPP.getInstance().isConnected()) {
            //not connected
            message = "Reconnect now";
            mode = EXTRA_ROW_MODE.RECONNECTNOW;
        } else if (_totalUsers < 1 || _bAllUsersOnline) {
            //no contacts, or none hidden
            message = "Tap to add contact";
            mode = EXTRA_ROW_MODE.ADDCONTACT;
        } else {
            //just toggle visibility of offline contacts
            message = (curToggle ? "Hide" : "Display") + " offline contacts...";
            mode = EXTRA_ROW_MODE.TOGGLEOFFLINE;
        }

        //TODO need this refreshed upon config, contact, toggle...
        //TODO need more padding on text button L/R, and make it shorter less padding top/bottom

        final EXTRA_ROW_MODE fMode = mode;
        //Log.d(TAG, "Creating extra view: " + message + ", " +  fMode.toString());
        Button btnDetails = (Button) convertView
                .findViewById(R.id.takchat_contactlist_item_extraBtn);
        btnDetails.setText(message);
        btnDetails.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                //Log.d(TAG, "Extra button pressed: " + fMode.toString());
                switch(fMode){
                    case DISABLED:
                        Toast.makeText(TAKChatUtils.mapView.getContext(), "Enabling chat...", Toast.LENGTH_SHORT).show();
                        _prefs.edit().putBoolean("takchatEnabled", true).apply();
                        redrawList();
                        break;
                    case CONFIG:
                        AtakBroadcast.getInstance().sendBroadcast(new Intent("com.atakmap.app.ADVANCED_SETTINGS"));
                        break;
                    case ADDCONTACT:
                        TAKChatView.showAddContactPrompt();
                        break;
                    case RECONNECTNOW:
                        if(TAKChatXMPP.getInstance().isConnected()) {
                            new AlertDialog.Builder(TAKChatUtils.mapView.getContext())
                                    .setIcon(com.atakmap.app.R.drawable.xmpp_icon)
                                    .setTitle("Confirm Reconnect")
                                    .setMessage("Disconnect, and reconnect now?")
                                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Log.d(TAG, "Reconnecting now...");
                                            TAKChatUtils.takChatComponent.getManager(ConnectionManager.class).forceReconnect(true);
                                        }
                                    })
                                    .setNegativeButton("Cancel", null)
                                    .show();
                        }else{
                            Log.d(TAG, "Reconnecting now...");
                            TAKChatUtils.takChatComponent.getManager(ConnectionManager.class).forceReconnect(true);
                        }
                        break;
                    case TOGGLEOFFLINE:
                        Log.d(TAG, "Toggling offline contacts: " + curToggle);
                        _prefs.edit().putBoolean("takchatShowOffline", !curToggle).apply();
                        redrawList();
                        break;
                }
            }
        });

        return convertView;
    }

    private void updateViewHolder(ViewHolder holder, View convertView, int position)
    {
        final XmppContact contactToDisplay =  _listToDisplay.get(position);
        if (contactToDisplay == null) {
            Log.w(TAG, "Couldn't find contact with position: " + position);
            //TODO reset any of the views?
            convertView.setBackgroundColor(BACKGROUND_COLOR_DEAD);
            return;
        }

        holder.pan.setEnabled(false);
        if (contactToDisplay != null) {
            //Log.d(TAG, "Getting view for: " + contactToDisplay.toVerboseString());
            holder.alias.setText(contactToDisplay.getName());
            if(contactToDisplay.hasStatus())
                holder.detail.setText(contactToDisplay.getStatus());
            else
                holder.detail.setText(contactToDisplay.getId().toString());
            if(contactToDisplay.isAvailable()) {
                if(!contactToDisplay.isAway())
                    if(TAKChatUtils.isConference(contactToDisplay)) {
                       convertView.setBackgroundColor(BACKGROUND_COLOR_CONFERENCE);
                    }else{
                        convertView.setBackgroundColor(BACKGROUND_COLOR_ALIVE);
                    }
                else
                    convertView.setBackgroundColor(BACKGROUND_COLOR_STALE);
            } else {
                convertView.setBackgroundColor(BACKGROUND_COLOR_DEAD);
            }
        }

        float textSize = BadgeDrawable.DEFAULT_TEXT_SIZE * TAKChatUtils.pluginContext.getResources().getDisplayMetrics().density;
        final Marker pmi = contactToDisplay.getMarker();
        if(pmi != null){
            //Log.d(TAG, "Setting marker for view: " + pmi.getUID());
            ATAKUtilities.SetIcon(TAKChatUtils.mapView.getContext(), holder.icon, pmi);
            textSize = BadgeDrawable.DEFAULT_TEXT_SIZE - 4; //TODO hack - see Bug 6965;
            holder.pan.setVisibility(ImageButton.VISIBLE);
            holder.pan.setEnabled(true);
            holder.pan.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    TAKChatUtils.mapView.getMapController().panTo(pmi.getPoint(), true);
                }
            });

            if(FileSystemUtils.isEquals(pmi.getTitle(), contactToDisplay.getName())) {
                holder.markerCallsign.setVisibility(TextView.GONE);
            }else{
                holder.markerCallsign.setVisibility(TextView.VISIBLE);
                holder.markerCallsign.setText(pmi.getTitle());
            }

        }else{
            //Log.d(TAG, "Not setting marker for view: " + contactToDisplay.getId().toString());
            holder.icon.setImageResource(R.drawable.takchat_xmpp_contact);
            holder.icon.setColorFilter(Color.WHITE);
            holder.pan.setVisibility(ImageButton.GONE);
            holder.pan.setEnabled(false);
            holder.pan.setOnClickListener(null);
            holder.markerCallsign.setVisibility(TextView.GONE);
        }

        holder.vcard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(TAKChatUtils.isConference(contactToDisplay)){
                    TAKConferenceView.displayConferenceInfo((XmppConference)contactToDisplay);
                }else {
                    TAKContactProfileView.getInstance().showContactInfo(contactToDisplay);
                }
            }
        });

        if(TAKChatUtils.isConference(contactToDisplay)){
            holder.icon.setImageResource(R.drawable.takchat_people);
            holder.icon.setColorFilter(Color.WHITE);
        }

        LayerDrawable ld = (LayerDrawable) TAKChatUtils.pluginContext.getResources().getDrawable(R.drawable.xmpp_badge);
        if(ld!=null){
            int count = ChatDatabase.getInstance(TAKChatUtils.pluginContext).getUnreadCount(contactToDisplay.getId());
            AtakLayerDrawableUtil.getInstance(TAKChatUtils.pluginContext).setBadgeCount(ld,
                    holder.icon.getDrawable(), count, textSize);
            holder.icon.setImageDrawable(ld);
        }
    }
}
