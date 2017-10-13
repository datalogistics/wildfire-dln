
package com.gmeci.atsk.az.farp.FARPSupport;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.atakmap.android.atsk.plugin.R;
import com.gmeci.atsk.az.farp.FARPTabHost;
import com.gmeci.atsk.resources.ATSKApplication;

import java.util.List;

public class FARPReceiverLayoutSpinnerAdapter extends ArrayAdapter<String> {

    private static final String TAG = "FARPReceiverLayoutSpinnerAdapter";
    private final List<String> _selections;
    private final LayoutInflater _inflater;

    public FARPReceiverLayoutSpinnerAdapter(Context context, int resource,
            List<String> selections) {
        super(context, resource, selections);

        Context pluginContext = ATSKApplication
                .getInstance().getPluginContext();
        _selections = selections;
        _inflater = LayoutInflater.from(pluginContext);
    }

    @Override
    public int getCount() {
        return _selections.size();
    }

    @Override
    public String getItem(int position) {
        return _selections.get(position);
    }

    @Override
    public long getItemId(int position) {
        return super.getItemId(position);
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        View row = convertView;

        if (row == null) {
            row = _inflater.inflate(R.layout.spinner_row_image, parent, false);
        }
        ImageView icon = (ImageView) row.findViewById(R.id.icon);
        TextView label = (TextView) row.findViewById(R.id.type);
        if (position >= _selections.size()) {
            position = 0;
        }
        String type = _selections.get(position);
        label.setText(type);

        icon.setBackgroundResource(FARPTabHost.getFARPImage(type));

        return row;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View row = convertView;

        if (row == null)
            row = _inflater.inflate(R.layout.spinner_button_bordered_image,
                    parent, false);

        ImageView icon = (ImageView) row.findViewById(R.id.icon);
        TextView label = (TextView) row.findViewById(R.id.type);
        if (position >= _selections.size()) {
            position = 0;
        }
        String type = _selections.get(position);
        label.setText(type);

        icon.setBackgroundResource(FARPTabHost.getFARPImage(type));

        return row;
    }

}
