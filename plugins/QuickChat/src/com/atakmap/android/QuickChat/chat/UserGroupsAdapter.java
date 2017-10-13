
package com.atakmap.android.QuickChat.chat;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.Html;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.atakmap.android.QuickChat.plugin.R;
import com.atakmap.android.QuickChat.utils.PluginHelper;
import com.atakmap.android.contact.Contacts;
import com.atakmap.android.contact.GroupContact;
import com.atakmap.android.maps.MapView;

import java.util.ArrayList;
import java.util.List;

import static com.atakmap.android.QuickChat.utils.PluginHelper.convertContactsToString;

/**
 * Created by Scott Auman on 11/7/2016.
 */

public class UserGroupsAdapter extends BaseAdapter implements
        AdapterView.OnItemLongClickListener {

    private final Context context;
    private final List<GroupContact> list;
    private final boolean[] boxes;
    private final List<String> checkedBoxes = new ArrayList<String>();
    private Toast toast;

    public UserGroupsAdapter(Context context,
            List<GroupContact> list) {
        this.context = context;
        this.list = list;
        boxes = new boolean[list.size()];
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public GroupContact getItem(int position) {
        return list.get(position);
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
                    R.layout.filter_team_popup_row, null);
            usersViewHolder = new UsersViewHolder();
            usersViewHolder.checkBox = (CheckBox) convertView
                    .findViewById(R.id.filter_user_popup_checkbox);
            usersViewHolder.numOfUsers = (TextView) convertView
                    .findViewById(R.id.numberOfUsersTextView);
            usersViewHolder.image = (ImageView) convertView
                    .findViewById(R.id.enterGroupImageView);
            convertView.setTag(usersViewHolder);
        }

        usersViewHolder = (UsersViewHolder) convertView.getTag();
        usersViewHolder.numOfUsers.setText(getOnlineContacts(getItem(position).getAllContactUIDs(false)) + " User(s) Online");
        usersViewHolder.image.setImageDrawable(PluginHelper.pluginContext
                .getResources().getDrawable(R.drawable.group_icon));
        usersViewHolder.checkBox
                .setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView,
                            boolean isChecked) {
                        if (isChecked) {
                            boxes[position] = true;
                            addAllUsersFromGroup(getItem(position)
                                    .getFilteredUIDs(true));
                        } else {
                            boxes[position] = false;
                            removeAllUsersFromGroup(getItem(position).getAllContactUIDs(false));
                        }
                    }
                });
        usersViewHolder.checkBox.setChecked(boxes[position]);
        usersViewHolder.checkBox.setText(PluginHelper
                .getCallsignFromContactUid(getItem(position).getUID()));
        return convertView;
    }

    private String getOnlineContacts(List<String> users) {
        int online = 0;
        Contacts contacts = Contacts.getInstance();
        synchronized (contacts) {
            for (String s : users) {
                if (contacts.validContact(contacts.getContactByUuid(s))) {
                    online++;
                }
            }
        }
        return online + "";
    }

    private void addAllUsersFromGroup(List<String> strings) {
        for (String s : strings) {
            checkedBoxes.add(s);
        }
    }

    private void removeAllUsersFromGroup(List<String> strings) {
        for (String s : strings) {
            checkedBoxes.remove(s);
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view,
            int position, long id) {
        //enabling peeking functionality to show user, who is in each team group before adding
        //Toast.makeText(MapView.getMapView().getContext(),"Long Click!",Toast.LENGTH_SHORT).show();
        List<String> allContactsInTeam = list.get(position).getAllContactUIDs(false);
        if (allContactsInTeam.size() > 0) {
            showCustomToast(allContactsInTeam, list.get(position).getName());
        } else {
            //no users in that team skip making the adapter and binding it
            Toast.makeText(MapView.getMapView().getContext(),
                    "No User's Currently In That Team", Toast.LENGTH_SHORT)
                    .show();
        }
        return true;
    }

    private void showCustomToast(List<String> strings, String teamName) {

        ListView listView = new ListView(context);
        listView.setDivider(new ColorDrawable(context.getResources().getColor(
                android.R.color.transparent)));
        listView.setDividerHeight(0);
        listView.setAdapter(new ArrayAdapter<String>(
                context, R.layout.simple_list_item_row,
                R.id.list_item_tv_1, convertContactsToString(strings)));
        LinearLayout layout = new LinearLayout(context);
        layout.setBackgroundDrawable(PluginHelper.pluginContext.getResources()
                .getDrawable(R.drawable.current_team_border));
        layout.setOrientation(LinearLayout.VERTICAL);
        TextView textView = new TextView(context);
        textView.setBackgroundColor(Color.BLACK);
        textView.setText(Html.fromHtml("<u>     Current User(s) In Group "
                + teamName + "     " + "</u>"));
        textView.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
        layout.addView(textView);
        layout.addView(listView);

        if (toast != null) {
            toast.cancel();
        }
        toast = new Toast(MapView.getMapView().getContext());
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setView(layout);
        toast.show();
    }

    public List<String> getCheckedBoxes() {
        return checkedBoxes;
    }

    public static class UsersViewHolder {
        ImageView image;
        CheckBox checkBox;
        TextView numOfUsers;
    }
}
