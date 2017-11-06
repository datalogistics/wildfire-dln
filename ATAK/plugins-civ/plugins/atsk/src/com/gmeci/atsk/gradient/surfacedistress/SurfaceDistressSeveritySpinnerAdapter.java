
package com.gmeci.atsk.gradient.surfacedistress;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.atakmap.android.atsk.plugin.R;
import com.gmeci.atsk.obstructions.ObstructionSpinnerAdapter;
import com.gmeci.atsk.resources.ATSKApplication;

import java.util.ArrayList;

public class SurfaceDistressSeveritySpinnerAdapter extends ArrayAdapter<String> {

    private static final String TAG = "SurfaceDistressSeveritySpinnerAdapter";
    final ArrayList<String> _selections;
    final LayoutInflater _inflater;
    private static final int RED = 0xFF990000;
    private static final int GREEN = 0xFF009900;
    private static final int YELLOW = 0xFF999900;
    final int[] _colors = {
            GREEN, YELLOW, RED
    };
    private final Context _context;

    public SurfaceDistressSeveritySpinnerAdapter(Context context, int resource,
            ArrayList<String> selections) {

        super(context, resource, selections);
        _context = context;
        _selections = selections;

        _inflater = LayoutInflater.from(ATSKApplication
                .getInstance().getPluginContext());

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
    public View getDropDownView(int position, View convertView,
            ViewGroup parent) {
        View row = convertView;
        if (row == null)
            row = _inflater.inflate(R.layout.spinner_row, parent, false);

        String current = _selections.get(position);
        TextView label = (TextView) row.findViewById(R.id.type);
        label.setText(current);
        label.setTextColor(_colors[position] == RED
                ? Color.WHITE : Color.BLACK);
        row.setBackgroundColor(_colors[position]);
        return row;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View row = convertView;
        if (row == null)
            row = _inflater.inflate(
                    R.layout.spinner_button_bordered, parent, false);

        String current = _selections.get(position);
        TextView label = (TextView) row.findViewById(R.id.type);
        ImageView icon = (ImageView) row.findViewById(R.id.icon);
        label.setText(current);
        String type = SurfaceDistressSpinnerAdapter.
                getCurrentType() + "_" + position;
        icon.setImageResource(ObstructionSpinnerAdapter.getResource(type));

        return row;
    }

}
