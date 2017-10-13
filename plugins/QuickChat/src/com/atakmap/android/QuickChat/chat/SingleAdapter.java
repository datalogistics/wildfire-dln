
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
import com.atakmap.android.QuickChat.utils.PluginHelper;
import com.atakmap.android.contact.Contacts;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Scott Auman on 11/14/2016.
 */
public class SingleAdapter extends BaseAdapter {

    private List<String> singleContacts = new ArrayList<String>();
    private final List<String> selected = new ArrayList<String>();
    private final Context context;

    private boolean boxes[];

    public SingleAdapter(Context context) {
        this.context = context;
        singleContacts = Contacts.getInstance().getAllIndividualContactUuids();
        boxes = new boolean[singleContacts.size()];
    }

    @Override
    public void notifyDataSetChanged() {
        boxes = new boolean[singleContacts.size()];
        super.notifyDataSetChanged();
    }

    /**
     * How many items are in the data set represented by this Adapter.
     *
     * @return Count of items.
     */
    @Override
    public int getCount() {
        return singleContacts.size();
    }

    /**
     * Get the data item associated with the specified position in the data set.
     *
     * @param position Position of the item whose data we want within the adapter's
     *                 data set.
     * @return The data at the specified position.
     */
    @Override
    public String getItem(int position) {
        return singleContacts.get(position);
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

    /**
     * Get a View that displays the data at the specified position in the data set. You can either
     * create a View manually or inflate it from an XML layout file. When the View is inflated, the
     * parent View (GridView, ListView...) will apply default layout parameters unless you use
     * {@link LayoutInflater#inflate(int, ViewGroup, boolean)}
     * to specify a root view and to prevent attachment to the root.
     *
     * @param position    The position of the item within the adapter's data set of the item whose view
     *                    we want.
     * @param convertView The old view to reuse, if possible. Note: You should check that this view
     *                    is non-null and of an appropriate type before using. If it is not possible to convert
     *                    this view to display the correct data, this method can create a new view.
     *                    Heterogeneous lists can specify their number of view types, so that this View is
     *                    always of the right type (see {@link #getViewTypeCount()} and
     *                    {@link #getItemViewType(int)}).
     * @param parent      The parent that this view will eventually be attached to
     * @return A View corresponding to the data at the specified position.
     */
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        TeamsAdapter.ViewHolder viewHolder;
        View view = convertView;

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

        viewHolder = (TeamsAdapter.ViewHolder) view.getTag();
        viewHolder.image.setVisibility(View.GONE);
        viewHolder.numUsers.setText(PluginHelper
                .getCallsignFromContactUid(getItem(position)));
        viewHolder.state
                .setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView,
                            boolean isChecked) {
                        boxes[position] = isChecked;
                        if (isChecked) {
                            boxes[position] = true;
                            selected.add(getItem(position));
                        } else {
                            boxes[position] = false;
                            selected.remove(getItem(position));
                        }
                    }
                });
        viewHolder.state.setChecked(boxes[position]);

        return view;
    }

    public List<String> getSelected() {
        return selected;
    }
}
