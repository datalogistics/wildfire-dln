
package com.atakmap.android.QuickChat.chat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.atakmap.android.QuickChat.plugin.R;
import com.atakmap.android.chat.TeamGroup;
import com.atakmap.android.contact.Contact;
import com.atakmap.android.contact.Contacts;
import com.atakmap.android.contact.GroupContact;
import com.atakmap.coremap.log.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Scott Auman on 11/3/2016.
 * A custom implementation of a BaseAdapter class
 * this adapter defines the top most structure of the inner user groups and team groups
 */

class AddUsersAdapter extends BaseAdapter {

    private final String TAG = getClass().getSimpleName();
    private final List<String> clickedUsers;
    private List<GroupContact> groupContacts;
    private final Context context;
    private final ListTypes listTypes;
    private boolean[] boxes; //handles selection states for custom rows

    //group uids we are looking for the add to custom view
    private final String[] validNames = {
            Contacts.TEAM_GROUPS, Contacts.USER_GROUPS, "HQ", "TadilJ"
    };

    interface ListTypes {
        void onGroupChange(TYPES type, List<?> list);
    }

    public enum TYPES {
        MAIN, CHILD, TEAMS, USER_GROUPS
    }

    AddUsersAdapter(Context context, ListTypes listTypes) {
        this.context = context;
        this.listTypes = listTypes;
        restoreMainAdapter();
        clickedUsers = new ArrayList<String>();
    }

    private void parseGroups(Contacts contacts) {
        List<GroupContact> gContacts = findGroups(contacts);
        if (gContacts == null || gContacts.size() == 0) {
            Log.d(TAG, "group contacts null @ parseGroups()");
            return;
        }
        groupContacts = gContacts;
    }

    public List<GroupContact> extractGroups(Contacts contacts) {
        return findGroups(contacts);
    }

    private void restoreMainAdapter() {
        parseGroups(Contacts.getInstance());
        boxes = new boolean[groupContacts.size()];
        notifyDataSetChanged();
    }

    private void restoreEnterableAdapter(CustomGroupContact customGroupContact) {
        List<GroupContact> groups = convertContactsToGroupContacts(
                customGroupContact.getAllContacts(false),
                customGroupContact.isUserCreated());
        if (customGroupContact.getName().equals("Groups")) {
            listTypes.onGroupChange(TYPES.USER_GROUPS, groups);
        } else if (customGroupContact.getName().equals("Teams"))
            listTypes.onGroupChange(TYPES.TEAMS, groups);
    }

    private List<GroupContact> convertContactsToGroupContacts(
            List<Contact> allContacts, boolean created) {
        List<GroupContact> teamGroups = new ArrayList<GroupContact>();
        for (Contact c : allContacts) {
            if (c instanceof TeamGroup) {
                TeamGroup teamGroup = (TeamGroup) c;
                teamGroups.add(teamGroup);
            } else if (c instanceof GroupContact) {
                GroupContact groupContact = (GroupContact) c;
                teamGroups.add(groupContact);
            }
        }
        return teamGroups;
    }

    private List<GroupContact> findGroups(Contacts contacts) {

        List<GroupContact> gContacts = new ArrayList<GroupContact>();
        for (Contact c : contacts.getAllContacts()) {
            if (c == null)
                continue;
            if (c instanceof GroupContact) {
                GroupContact groupContact = (GroupContact) c;
                if (findValidGroup(groupContact.getUID())) {
                    if (groupContact.getUID().equals(Contacts.TEAM_GROUPS)) {
                        List<Contact> contactList = groupContact.getAllContacts(false);
                        CustomGroupContact customGroupContact = new CustomGroupContact(
                                groupContact.getUID()
                                ,
                                getCallsignFromContactUid(groupContact.getUID()),
                                contactList,
                                false, true);
                        gContacts.add(customGroupContact);
                    } else if (groupContact.getUID().equals(
                            Contacts.USER_GROUPS)) {
                        Log.d(TAG,
                                getCallsignFromContactUid(groupContact.getUID()));
                        CustomGroupContact customGroupContact = new CustomGroupContact
                                (groupContact.getUID(),
                                        getCallsignFromContactUid(groupContact
                                                .getUID()),
                                        groupContact.getAllContacts(false), true,
                                        true);
                        gContacts.add(customGroupContact);
                    }
                }
            }
        }
        return gContacts;
    }

    private boolean findValidGroup(String name) {
        for (String validName : validNames) {
            if (name.equals(validName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * How many items are in the data set represented by this Adapter.
     *
     * @return Count of items.
     */
    @Override
    public int getCount() {
        return groupContacts.size();
    }

    /**
     * @param uuid the string uid
     * @return the callsign for the given uid
     */
    private String getCallsignFromContactUid(String uuid) {
        if (uuid != null) {
            Contact contact = Contacts.getInstance().getContactByUuid(uuid);
            if (contact != null) {
                return contact.getName();
            }
        }
        return null;
    }

    /**
     * Get the data item associated with the specified position in the data set.
     *
     * @param position Position of the item whose data we want within the adapter's
     *                 data set.
     * @return The data at the specified position.
     */
    @Override
    public GroupContact getItem(int position) {
        return groupContacts.get(position);
    }

    /**
     * Get the row id associated with the specified position in the list.
     *
     * @param position The position of the item within the adapter's data set whose row id we want.
     * @return The id of the item at the specified position.
     */
    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(final int position, final View convertView,
            ViewGroup parent) {
        View view = convertView;
        ViewHolder viewHolder;
        if (view == null) {
            view = LayoutInflater.from(context).inflate(
                    R.layout.filter_add_popup_row, null);
            viewHolder = new ViewHolder();
            viewHolder.name = (TextView) view
                    .findViewById(R.id.callSignTextView);
            viewHolder.state = (CheckBox) view
                    .findViewById(R.id.filter_user_popup_checkbox);
            viewHolder.image = (ImageView) view
                    .findViewById(R.id.enterGroupImageView);
            viewHolder.numberOfGroups = (TextView) view
                    .findViewById(R.id.groupNumberTextView);
            view.setTag(viewHolder);
        }

        viewHolder = (ViewHolder) view.getTag();
        if (getItem(position) instanceof CustomGroupContact) {
            final CustomGroupContact customGroupContact = (CustomGroupContact) getItem(position);
            if (customGroupContact.isEnterable()) {
                viewHolder.state.setVisibility(View.GONE);
                viewHolder.name.setText(getItem(position).getName());
                viewHolder.image.setVisibility(View.VISIBLE);
                viewHolder.numberOfGroups.setVisibility(View.VISIBLE);
                int size = customGroupContact.getAllContacts(false).size();
                viewHolder.numberOfGroups.setText( //coverity 19181 -dead code
                        size > 0 ? size + " Inner Group(s)" : "No Inner Groups");
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //showNextAdapter();
                        restoreEnterableAdapter((CustomGroupContact) getItem(position));
                    }
                });
                return view;
            }
        }

        viewHolder.state.setVisibility(View.VISIBLE);
        viewHolder.numberOfGroups.setVisibility(View.GONE);
        viewHolder.image.setVisibility(View.GONE);
        viewHolder.name.setText(getItem(position).getName());
        viewHolder.state.setChecked(boxes[position]);
        viewHolder.state
                .setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView,
                            boolean isChecked) {
                        if (isChecked) {
                            boxes[position] = true;
                            clickedUsers.add(getItem(position).getName());
                        } else {
                            boxes[position] = false;
                            clickedUsers.remove(getItem(position).getName());
                        }
                    }
                });
        return view;
    }

    static class ViewHolder {
        TextView name;
        TextView numberOfGroups;
        CheckBox state;
        ImageView image;
    }
}
