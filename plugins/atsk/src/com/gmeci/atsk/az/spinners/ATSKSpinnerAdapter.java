
package com.gmeci.atsk.az.spinners;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.atakmap.android.atsk.plugin.R;
import com.gmeci.atsk.resources.ATSKApplication;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

public class ATSKSpinnerAdapter extends ArrayAdapter<String> {

    private static final String TAG = "ATSKSpinnerAdapter";
    final List<String> selections;
    LayoutInflater inflater;

    public ATSKSpinnerAdapter(Context context, int resource,
            List<String> selections) {

        super(context, resource, selections);
        SetupSpinner();
        this.selections = selections;
    }

    public ATSKSpinnerAdapter(Context context, int resource) {
        super(context, resource);

        SetupSpinner();

        Context pluginContext = ATSKApplication
                .getInstance().getPluginContext();
        String[] strings = pluginContext.getResources().getStringArray(
                R.array.atsk_runway_surface_types);
        selections = new ArrayList<String>(Arrays.asList(strings));
    }

    private void SetupSpinner() {
        Context pluginContext = ATSKApplication
                .getInstance().getPluginContext();

        inflater = LayoutInflater.from(pluginContext);
    }

    @Override
    public int getCount() {
        if (selections == null)
            return 0;
        return selections.size();
    }

    @Override
    public String getItem(int position) {
        if (selections == null)
            return "";
        if (position >= selections.size())
            position = 0;
        if (selections.size() < 1)
            return "none";
        return selections.get(position);
    }

    @Override
    public long getItemId(int position) {
        return super.getItemId(position);
    }

    @Override
    public View getDropDownView(int position, View convertView,
            ViewGroup parent) {

        View row = convertView;

        if (row == null) {
            row = inflater.inflate(R.layout.spinner_row, parent, false);
        }

        String current = selections.get(position);
        TextView label = (TextView) row.findViewById(R.id.type);
        label.setPadding(0, 0, 0, 0);
        label.setText(current);
        return row;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        View row = convertView;

        if (row == null)
            row = inflater.inflate(R.layout.spinner_button_bordered, parent,
                    false);

        String current = selections.get(position);
        TextView label = (TextView) row.findViewById(R.id.type);
        label.setText(current);
        return row;
    }

}
