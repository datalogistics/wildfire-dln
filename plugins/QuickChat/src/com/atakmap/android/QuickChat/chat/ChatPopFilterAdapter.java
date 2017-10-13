
package com.atakmap.android.QuickChat.chat;

import android.content.Context;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.atakmap.android.QuickChat.plugin.R;
import com.atakmap.android.contact.Contact;
import com.atakmap.android.contact.Contacts;
import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.log.Log;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by AumanS on 4/21/2016.
 * list adapter that display's the list of popUpUsers
 * see javaDoc for Override methods
 */
class ChatPopFilterAdapter extends BaseAdapter {

    private static final String TAG = ChatPopFilterAdapter.class
            .getSimpleName();

    private final Context context;
    private final FilterChatUserDropDown.ItemsChecked itemsChecked;
    private final List<UsersViewHolder> checkedBoxes = new ArrayList<UsersViewHolder>();
    private List<PopUpUser> uuids = new ArrayList<PopUpUser>();
    private boolean[] boxes;

    public void clear() {
        uuids.clear();
        notifyDataSetChanged();
    }

    /**
     * determine if the list of users is sorted already
     * no need to show sort button is the list is already sorted!
     *
     * @return true| false
     */
    public boolean isListSorted() {
        List<PopUpUser> list = new ArrayList<PopUpUser>(uuids);
        Collections.sort(list, new Comparator<PopUpUser>() {
            @Override
            public int compare(PopUpUser lhs, PopUpUser rhs) {
                String callA = lhs.getName();
                String callB = rhs.getName();
                return callA.compareToIgnoreCase(callB);
            }
        });

        //loop through each index in both lists comparing each uuid to the same index in the other list
        //since we are using custom objects we need to break them in
        if (list.size() == uuids.size()) {
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).getUid().equals(uuids.get(i).getUid())) {
                    continue;
                }
                return false;
            }
        }
        return true;
    }

    /**
     * sorts list in alphabetic order using custom comparator interface
     * sorts by callsign names because that is the text the user sees
     */
    public void sortAZ() {
        if (uuids != null && uuids.size() > 0) {
            Collections.sort(uuids, new Comparator<PopUpUser>() {
                @Override
                public int compare(PopUpUser lhs, PopUpUser rhs) {
                    String callA = lhs.getName();
                    String callB = rhs.getName();
                    return callA.compareToIgnoreCase(callB);
                }
            });
            notifyDataSetChanged();
            ReSortSavedList reSortSavedList = new ReSortSavedList(MapView
                    .getMapView().getContext());
            if (reSortSavedList.getStatus() == AsyncTask.Status.RUNNING)
                reSortSavedList.cancel(true);
            //start BG thread that resorts list in prefs might as well do this off the UI thread to not hinder
            reSortSavedList.execute(uuids);
        }
    }

    public static class UsersViewHolder {
        TextView name;
        ImageView image;
        CheckBox checkBox;
        String uuid;
    }

    public List<UsersViewHolder> getCheckedBoxes() {
        return checkedBoxes;
    }

    public ChatPopFilterAdapter(Context context,
            FilterChatUserDropDown.ItemsChecked itemsChecked) {
        this.context = context;
        uuids = SavedFilteredPopupChatUsers
                .getEntireUserListPopUpUsers(context);
        this.itemsChecked = itemsChecked;
        boxes = new boolean[uuids.size()];
    }

    @Override
    public int getCount() {
        return uuids.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        UsersViewHolder usersViewHolder;

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(
                    R.layout.filter_user_popup_row, null);
            usersViewHolder = new UsersViewHolder();
            usersViewHolder.name = (TextView) convertView
                    .findViewById(R.id.callSignTextView);
            usersViewHolder.checkBox = (CheckBox) convertView
                    .findViewById(R.id.filter_user_popup_checkbox);
            usersViewHolder.image = (ImageView) convertView
                    .findViewById(R.id.enterGroupImageView);
            usersViewHolder.image.setVisibility(View.GONE);
            convertView.setTag(usersViewHolder);
        }

        usersViewHolder = (UsersViewHolder) convertView.getTag();
        usersViewHolder.uuid = uuids.get(position).getUid();
        final UsersViewHolder finalUsersViewHolder = usersViewHolder;
        usersViewHolder.checkBox
                .setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView,
                            boolean isChecked) {
                        if (isChecked) {
                            boxes[position] = true;
                            checkedBoxes.add(finalUsersViewHolder);
                        } else {
                            boxes[position] = false;
                            checkedBoxes.remove(finalUsersViewHolder);
                        }
                        //return state of checkboxes to enable.disable delete button on bar
                        itemsChecked
                                .itemsChecked(checkCheckedStateForCallBack());
                    }
                });
        usersViewHolder.checkBox.setChecked(boxes[position]);
        usersViewHolder.name.setText(checkName(uuids.get(position)));

        return convertView;
    }

    private boolean checkCheckedStateForCallBack() {
        for (UsersViewHolder holders : checkedBoxes) {
            if (holders.checkBox.isChecked()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Catches changes in callsigns based off of the UID for the user
     * if the user changes their callsign and was contained in the supporting list
     * catch the change and re store the object into prefs
     *
     * @param poppers the object containing user info
     * @return string used to set the text for the given textview showing the user's callsign
     */
    private String checkName(PopUpUser poppers) {

        try {
            if (poppers == null) {
                Log.e(TAG, "poppers is null");
                return PopUpUser.DEFAULT_NAME;
            }

            String contactUid = getCallsignFromContactUid(poppers.getUid());
            if (contactUid != null) {
                if (getCallsignFromContactUid(poppers.getUid()).equals(
                        poppers.getName())) {
                    return poppers.getName();
                } else {
                    //save the change into the object
                    poppers.setName(getCallsignFromContactUid(poppers.getUid()));
                    SavedFilteredPopupChatUsers.changeCallsignForUser(context,
                            poppers);
                    return poppers.getName();
                }
            } else {
                Log.d(TAG, "null callsign @ checkName");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return poppers.getName();//just in case if the conversion fails default to original name
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
     * tell the list adapter , we changed some data so update yourself!!!!
     */
    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
        boxes = new boolean[uuids.size()];
    }

    /**
     * @param data List<PopUpUser></> containing data from the search results
     *             when user searches for specific string the list containing all matches
     *             enteries is returned and set as the new data source
     */
    public void setNewData(List<PopUpUser> data) {
        if (data.size() == 0) { //coverity 18324 data cannot be null
            itemsChecked.noItemsInSearch();
        } else {
            uuids = data;
            notifyDataSetChanged();
        }
    }

    /**
     * similar  to notifyDataSetChanged
     * but clears the list first then resets itself
     */
    public void refresh() {
        clear();
        uuids = SavedFilteredPopupChatUsers
                .getEntireUserListPopUpUsers(context);
        notifyDataSetChanged();
    }

    /**
     * Runs along the UI thread ASYNC
     * re orders the list of objects by callsign names
     * we do this off the UI thread  because larger list and dealing with custom objects can
     * block the UI thread. We do not want that :)
     */
    static class ReSortSavedList extends
            AsyncTask<List<PopUpUser>, Void, Boolean> {

        private final Context context;

        public ReSortSavedList(Context context) {
            this.context = context;
        }

        @Override
        protected Boolean doInBackground(List<PopUpUser>... params) {
            ContactJsonFormat jsonFormat = new ContactJsonFormat();
            try {
                SavedFilteredPopupChatUsers.saveNewUserList(context,
                        jsonFormat.convertObjectsToJson(params[0]));
                return true;
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return false;
        }
    }
}
