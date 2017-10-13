package com.atakmap.android.takchat.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.atakmap.android.takchat.TAKChatUtils;
import com.atakmap.android.takchat.plugin.R;
import com.atakmap.coremap.filesystem.FileSystemUtils;

import org.jivesoftware.smackx.muc.HostedRoom;

import java.util.ArrayList;
import java.util.List;

/**
 * Display a list of conferences
 *
 * @author byoung
 */
public class ConferenceSelectAdapter extends BaseAdapter {

    private final HostedRoom _baseData[];
    private List<HostedRoom> _items = new ArrayList<HostedRoom>();
    private String _searchTerms;

    public ConferenceSelectAdapter(HostedRoom[] data) {
        _baseData = data;
        refresh();
    }

    public void refresh() {
        if (_baseData == null)
            return;
        final List<HostedRoom> filtered = new ArrayList<HostedRoom>();
        for (HostedRoom room : _baseData) {
            if (accept(room))
                filtered.add(room);
        }

        TAKChatUtils.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                _items = filtered;
                notifyDataSetChanged();
            }
        });
    }

    @Override
    public Object getItem(int position) {
        return _items.get(position);
    }

    @Override
    public int getCount() {
        return _items.size();
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        ContactHolder holder = null;

        //TODO display online status
        if (row == null) {
            final LayoutInflater inflater = (LayoutInflater) TAKChatUtils.pluginContext.
                    getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            row = inflater.inflate(R.layout.takchat_conference_select_list_item, parent, false);

            holder = new ContactHolder();
            holder.txtName = (TextView) row
                    .findViewById(R.id.takchat_conferenceselect_name);
            holder.txtJID = (TextView) row
                    .findViewById(R.id.takchat_conferenceselect_jid);

            row.setTag(holder);
        } else {
            holder = (ContactHolder) row.getTag();
        }

        //TODO set lock icon if password is required. Possibly display number of current occupants
        HostedRoom room = (HostedRoom) getItem(position);
        holder.txtName.setText(room.getName());
        holder.txtJID.setText(room.getJid());
        return row;
    }

    private boolean accept(HostedRoom room) {
        return FileSystemUtils.isEmpty(_searchTerms)
                || TAKChatUtils.searchRoom(room, _searchTerms);
    }

    public void search(String terms) {
        if (!FileSystemUtils.isEmpty(terms))
            terms = terms.toLowerCase();
        _searchTerms = terms;
        refresh();
    }

    static class ContactHolder
    {
        TextView txtName;
        TextView txtJID;
    }
}
