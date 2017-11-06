
package com.gmeci.atsk.az.lz;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import com.atakmap.android.atsk.plugin.R;
import com.gmeci.atsk.resources.ATSKApplication;

import java.util.List;
import java.util.Map;

public class ExpandableListAdapter extends BaseExpandableListAdapter {

    private static final String TAG = "ExpandableListAdapter";
    private final Activity _acitivty;
    private final Map<String, List<String>> _measurements;
    private final List<String> _headers;

    public ExpandableListAdapter(Activity act, List<String> headers,
            Map<String, List<String>> listCollection) {
        _acitivty = act;
        _measurements = listCollection;
        _headers = headers;
    }

    public Object getChild(int groupPosition, int childPosition) {
        return _measurements.get(_headers.get(groupPosition)).get(
                childPosition);
    }

    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    public View getChildView(final int groupPosition, final int childPosition,
            boolean isLastChild, View convertView, ViewGroup parent) {
        final String ACdimension = (String) getChild(groupPosition,
                childPosition);

        Context pluginContext = ATSKApplication
                .getInstance().getPluginContext();
        LayoutInflater inflater = LayoutInflater.from(pluginContext);

        if (convertView == null)
            convertView = inflater.inflate(R.layout.ac_exp_list_child_item,
                    null);

        TextView item = (TextView) convertView
                .findViewById(R.id.ac_exp_list_child_item_tv);
        item.setTextColor(Color.WHITE);
        item.setText(ACdimension);

        return convertView;
    }

    public int getChildrenCount(int groupPosition) {
        return _measurements.get(_headers.get(groupPosition))
                .size();
    }

    public Object getGroup(int groupPosition) {
        return _headers.get(groupPosition);
    }

    public int getGroupCount() {
        return _headers.size();
    }

    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    public View getGroupView(int groupPosition, boolean isExpanded,
            View convertView, ViewGroup parent) {
        String tvName = (String) getGroup(groupPosition);
        if (convertView == null) {
            Context pluginContext = ATSKApplication
                    .getInstance().getPluginContext();
            LayoutInflater infalInflater = LayoutInflater.from(pluginContext);
            convertView = infalInflater.inflate(
                    R.layout.ac_exp_list_group_item,
                    null);
        }
        TextView item = (TextView) convertView
                .findViewById(R.id.ac_exp_list_child_item_tv);
        item.setText(tvName);
        item.setTextColor(Color.WHITE);
        return convertView;
    }

    public boolean hasStableIds() {
        return true;
    }

    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    public void modify(int groupPosition, int childPosition, String newValue) {
        List<String> child = _measurements.get(_headers
                .get(groupPosition));
        child.set(childPosition, newValue);
        notifyDataSetChanged();
    }

}
