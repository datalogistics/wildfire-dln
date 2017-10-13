package com.atakmap.android.takchat.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.LayerDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.atakmap.android.maps.Marker;
import com.atakmap.android.takchat.TAKChatUtils;
import com.atakmap.android.takchat.data.ChatDatabase;
import com.atakmap.android.takchat.data.XmppContact;
import com.atakmap.android.takchat.plugin.R;
import com.atakmap.android.takchat.view.badge.AtakLayerDrawableUtil;
import com.atakmap.android.takchat.view.badge.BadgeDrawable;
import com.atakmap.android.util.ATAKUtilities;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;

import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Display a list of contacts
 *
 * @author byoung
 */
public class ContactSelectAdapter extends BaseAdapter {

    private static final String TAG = "ContactSelectAdapter";

    private final XmppContact _baseData[];
    private List<XmppContact> _items = new ArrayList<XmppContact>();
    private Set<XmppContact> _selected = new HashSet<XmppContact>();

    private final boolean _multiSelect;
    private OnItemClickListener _listener;
    private String _searchTerms;

    public ContactSelectAdapter(XmppContact[] data, boolean multiSelect) {
        //TODO listen for contact presence/status changes
        _baseData = data;
        _multiSelect = multiSelect;
        refresh();
    }

    public ContactSelectAdapter(XmppContact[] data) {
        this(data, false);
    }

    public ContactSelectAdapter(List<String> ids) {
        _baseData = new XmppContact[ids.size()];
        for (int i = 0; i < _baseData.length; i++) {
            try {
                _baseData[i] = new XmppContact(JidCreate.bareFrom(ids.get(i)));
            } catch (XmppStringprepException e) {
                Log.e(TAG, "Failed to parse id " + ids.get(i));
            }
        }
        _multiSelect = false;
        refresh();
    }

    public void refresh() {
        if (_baseData == null)
            return;
        final List<XmppContact> filtered = new ArrayList<XmppContact>();
        for (XmppContact c : _baseData) {
            if (accept(c))
                filtered.add(c);
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
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public int getCount() {
        return _items.size();
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final View row;
        final ContactHolder holder;

        //TODO display online status
        if (convertView == null) {
            final LayoutInflater inflater = (LayoutInflater) TAKChatUtils.pluginContext.
                    getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            row = inflater.inflate(R.layout.takchat_contact_select_list_item, parent, false);
            holder = new ContactHolder();
            holder.imgIcon = (ImageView) row
                    .findViewById(R.id.takchat_contactselect_image);
            holder.txtAlias = (TextView) row
                    .findViewById(R.id.takchat_contactselect_alias);
            holder.txtStatus = (TextView) row
                    .findViewById(R.id.takchat_contactselect_status);
            holder.checkBox = (CheckBox) row
                    .findViewById(R.id.takchat_contactselect_checkbox);
            row.setTag(holder);
        } else {
            row = convertView;
            holder = (ContactHolder) row.getTag();
        }
        holder.checkBox.setVisibility(_multiSelect ? View.VISIBLE : View.GONE);

        final XmppContact contact = (XmppContact) getItem(position);
        if (contact == null)
            return row;
        holder.txtAlias.setText(contact.getName());
        holder.txtStatus.setText(contact.getStatus());
        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (_multiSelect)
                    holder.checkBox.toggle();
                else if (_listener != null)
                    _listener.onItemClick(ContactSelectAdapter.this, contact, position);
            }
        });

        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(_selected.contains(contact));
        holder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton cb, boolean checked) {
                if (checked)
                    _selected.add(contact);
                else
                    _selected.remove(contact);
            }
        });

        float textSize = BadgeDrawable.DEFAULT_TEXT_SIZE * TAKChatUtils.pluginContext.getResources().getDisplayMetrics().density;
        Marker marker = contact.getMarker();
        if(TAKChatUtils.isConference(contact)){
            holder.imgIcon.setImageResource(R.drawable.takchat_people);
            holder.imgIcon.setColorFilter(Color.WHITE);
        } else if(marker != null) {
            ATAKUtilities.SetIcon(TAKChatUtils.mapView.getContext(), holder.imgIcon, marker);
            textSize = BadgeDrawable.DEFAULT_TEXT_SIZE - 4; //TODO hack - see Bug 6965;
        } else{
            holder.imgIcon.setImageResource(R.drawable.takchat_xmpp_contact);
            holder.imgIcon.setColorFilter(Color.WHITE);
        }

        if(contact.isAvailable()) {
            if(!contact.isAway())
                if(TAKChatUtils.isConference(contact)) {
                    row.setBackgroundColor(ContactListAdapter.BACKGROUND_COLOR_CONFERENCE);
                }else{
                    row.setBackgroundColor(ContactListAdapter.BACKGROUND_COLOR_ALIVE);
                }
            else
                row.setBackgroundColor(ContactListAdapter.BACKGROUND_COLOR_STALE);
        } else {
            row.setBackgroundColor(ContactListAdapter.BACKGROUND_COLOR_DEAD);
        }

        LayerDrawable ld = (LayerDrawable) TAKChatUtils.pluginContext.getResources().getDrawable(R.drawable.xmpp_badge);
        if(ld!=null){
            int count = ChatDatabase.getInstance(TAKChatUtils.pluginContext).getUnreadCount(contact.getId());
            AtakLayerDrawableUtil.getInstance(TAKChatUtils.pluginContext).setBadgeCount(ld,
                    holder.imgIcon.getDrawable(), count, textSize);
            holder.imgIcon.setImageDrawable(ld);
        }

        return row;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        _listener = listener;
        notifyDataSetChanged();
    }

    private boolean accept(XmppContact c) {
        return FileSystemUtils.isEmpty(_searchTerms)
                || TAKChatUtils.searchContact(c, _searchTerms);
    }

    public void search(String terms) {
        if (!FileSystemUtils.isEmpty(terms))
            terms = terms.toLowerCase();
        _searchTerms = terms;
        refresh();
    }

    public Set<XmppContact> getSelected() {
        return new HashSet<XmppContact>(_selected);
    }

    static class ContactHolder {
        ImageView imgIcon;
        TextView txtAlias;
        TextView txtStatus;
        CheckBox checkBox;
    }

    public interface OnItemClickListener {
        void onItemClick(ContactSelectAdapter adapter, XmppContact contact, int position);
    }
}
