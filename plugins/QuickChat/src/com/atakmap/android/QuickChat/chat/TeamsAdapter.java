
package com.atakmap.android.QuickChat.chat;

import android.content.Context;
import android.content.SharedPreferences;
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
import com.atakmap.android.chat.ChatManagerMapComponent;
import com.atakmap.android.chat.TeamGroup;
import com.atakmap.android.contact.Contacts;
import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.log.Log;

import java.util.ArrayList;
import java.util.List;

import static com.atakmap.android.QuickChat.utils.PluginHelper.convertContactsToString;

/**
 * Created by Scott Auman on 11/7/2016.
 */

public class TeamsAdapter extends BaseAdapter implements
        AdapterView.OnItemLongClickListener,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private final Context context;
    private final List<String> clickedGroups = new ArrayList<String>();
    private List<TeamGroup> teamGroups = new ArrayList<TeamGroup>();
    private final boolean[] boxes;
    private Toast toast;
    private final String TAG = getClass().getSimpleName();

    public TeamsAdapter(Context context, List<TeamGroup> list) {
        this.context = context;
        this.teamGroups = list;
        boxes = new boolean[this.teamGroups.size()];
    }

    @Override
    public int getCount() {
        return teamGroups.size();
    }

    @Override
    public TeamGroup getItem(int position) {
        return teamGroups.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        View view = convertView;
        TeamsAdapter.ViewHolder viewHolder;
        if (view == null) {
            view = LayoutInflater.from(context).inflate(
                    R.layout.filter_team_popup_row, null);
            viewHolder = new TeamsAdapter.ViewHolder();
            viewHolder.state = (CheckBox) view
                    .findViewById(R.id.filter_user_popup_checkbox);
            viewHolder.image = (ImageView) view
                    .findViewById(R.id.enterGroupImageView);
            viewHolder.numUsers = (TextView) view
                    .findViewById(R.id.numberOfUsersTextView);
            view.setTag(viewHolder);
        }

        //attach all strings to views and setup checked states
        viewHolder = (TeamsAdapter.ViewHolder) view.getTag();
        final TeamGroup teamGroup = getItem(position);
        viewHolder.image.setColorFilter(teamGroup.getIconColor());
        viewHolder.state
                .setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView,
                            boolean isChecked) {
                        boxes[position] = isChecked;
                        if (isChecked) {
                            for (String string : Contacts.getInstance()
                                    .getAllContactsInTeam(teamGroup.getName())) {
                                clickedGroups.add(string);
                                Log.d(TAG, "Added UID " + string
                                        + " To Filter List");
                            }
                        } else {
                            for (String string : Contacts.getInstance()
                                    .getAllContactsInTeam(teamGroup.getName())) {
                                clickedGroups.remove(string);
                                Log.d(TAG, "REMOVED UID " + string
                                        + " To Filter List");
                            }
                        }
                    }
                });
        viewHolder.state.setChecked(boxes[position]);

        List<String> allContacts = Contacts.getInstance()
                .getAllContactsInTeam(teamGroup.getName());
        String numUserString = (getOnlineContacts(allContacts) + " User(s) Online");
        viewHolder.numUsers.setText(numUserString);
        //set a string on the current team color row
        if (teamGroup.getName().equals(getUserCurrentTeamColor())) {
            viewHolder.state.setText(teamGroup.getName() + " (Current Team)");
        } else {
            viewHolder.state.setText(teamGroup.getName());
        }
        return view; //return custom view back to adapter to display
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

    /**
     * @return the current team name
     */
    private String getUserCurrentTeamColor() {
        return ChatManagerMapComponent.getTeamName();
    }

    /**return back all strings(uids) that the user selected for this adapter
     * @return collection of uid string
     */
    public List<String> getClickedGroups() {
        return clickedGroups;
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view,
            int position, long id) {
        //enabling peeking functionality to show user, who is in each team group before adding
        List<String> allContactsInTeam = Contacts.getInstance()
                .getAllContactsInTeam(teamGroups.get(position).getName());

        if (allContactsInTeam.size() > 0) {
            showCustomToast(allContactsInTeam, teamGroups.get(position)
                    .getName());
        } else {
            //no users in that team skip making the adapter and binding it
            Toast.makeText(MapView.getMapView().getContext(),
                    "No User's Currently In That Team", Toast.LENGTH_SHORT)
                    .show();
        }
        return true;
    }

    /**uses the android toast view class to bind a custom view
     * that displays all available online users in the long pressed selected row
     * the view is created programmatically
     */
    private void showCustomToast(List<String> strings, String teamName) {
        if (toast != null) {
            toast.cancel();
        }

        LinearLayout layout = generateToastView(strings, teamName);
        toast = new Toast(MapView.getMapView().getContext());
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setView(layout);
        toast.show();
    }

    /**Programmatically creates a linear layout that contains a Listview
     * that binds to an adapter that creates a textview row
     * for each uid callsign that is sent in from the collection
     * @param strings collection of uids
     * @param teamName the team name(ie color) for header
     * @return linear layout
     */
    private LinearLayout generateToastView(List<String> strings, String teamName) {
        LinearLayout layout = new LinearLayout(context); //container layout

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);

        //listview
        ListView listView = new ListView(context);
        listView.setDivider(new ColorDrawable(context.getResources().getColor(
                android.R.color.transparent)));
        listView.setDividerHeight(0);
        listView.setAdapter(new ArrayAdapter<String>(
                context, R.layout.simple_list_item_row,
                R.id.list_item_tv_1, convertContactsToString(strings)));

        //header textview
        TextView textView = new TextView(context);
        textView.setBackgroundColor(Color.BLACK);
        textView.setTextColor(Color.WHITE);
        textView.setText(Html.fromHtml("     "
                + "<u>Current User(s) In Team Group " + teamName + "      "
                + "</u>"));
        textView.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
        layout.addView(textView);

        //container specs
        layout.setPadding(8, 8, 8, 8);
        layout.setLayoutParams(params);
        layout.setBackgroundDrawable(PluginHelper.
                pluginContext.getResources().getDrawable(
                        R.drawable.current_team_border));
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(listView);

        return layout;
    }

    /**override and catch changes in preference
     * if the team color changes and this view is open when returning
     * back from settings activity then update the adapter
     * to display the correct current team listing
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
            String key) {
        Log.d(TAG, "Change in key " + key);
        if (key.equals("locationTeam")) {
            notifyDataSetChanged();
        }
    }

    /*
        class that holds each row information
     */
    public static class ViewHolder {
        CheckBox state;
        ImageView image;
        TextView numUsers;
    }
}
