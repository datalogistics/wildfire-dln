
package com.gmeci.atsk.gradient.surfacedistress;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.atakmap.android.atsk.plugin.R;
import com.gmeci.constants.Constants;

import java.util.ArrayList;
import java.util.HashMap;

public class SurfaceDistressSpinnerAdapter extends ArrayAdapter<String> {

    private static final String TAG = "SurfaceDistressSpinnerAdapter";
    final ArrayList<String> _selections;
    final LayoutInflater _inflater;
    private final Context _context;
    private static String _currentType = "";
    private static HashMap<String, Integer> _resourceMap;

    public SurfaceDistressSpinnerAdapter(Context context, int resource,
            ArrayList<String> selections) {

        super(context, resource, selections);
        _context = context;
        _selections = selections;
        _inflater = LayoutInflater.from(context);
        fillMap();
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

        if (row == null) {
            row = _inflater.inflate(R.layout.spinner_row_image, parent, false);
        }

        String type = _selections.get(position);
        ImageView icon = (ImageView) row.findViewById(R.id.icon);
        TextView label = (TextView) row.findViewById(R.id.type);

        label.setText(type);
        if (_resourceMap.containsKey(type))
            icon.setBackgroundResource(_resourceMap.get(_selections
                    .get(position)));
        else
            icon.setBackgroundResource(R.drawable.po_generic_point);

        return row;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        View row = convertView;

        if (row == null) {
            row = _inflater.inflate(R.layout.spinner_button_bordered_image,
                    parent, false);
        }

        String type = _currentType = _selections.get(position);
        ImageView icon = (ImageView) row.findViewById(R.id.icon);
        TextView label = (TextView) row.findViewById(R.id.type);

        label.setText(type);
        if (_resourceMap.containsKey(type))
            icon.setBackgroundResource(_resourceMap.get(_selections
                    .get(position)));
        else
            icon.setBackgroundResource(R.drawable.po_generic_point);

        return row;
    }

    public static String getCurrentType() {
        return _currentType;
    }

    private synchronized static void fillMap() {
        if (_resourceMap != null)
            return;
        _resourceMap = new HashMap<String, Integer>();
        _resourceMap.put(Constants.DISTRESS_DUST,
                R.drawable.sd_dust);
        _resourceMap.put(Constants.DISTRESS_JET_EROSION,
                R.drawable.sd_jet_blast_erosion);
        _resourceMap.put(Constants.DISTRESS_LOOSE_AGG,
                R.drawable.sd_aggregate);
        _resourceMap.put(Constants.DISTRESS_POTHOLE,
                R.drawable.sd_pothole);
        _resourceMap.put(
                Constants.DISTRESS_ROLLING_RESISTANT,
                R.drawable.sd_rolling_resist);
        _resourceMap.put(Constants.DISTRESS_RUTS,
                R.drawable.sd_ruts);
        _resourceMap.put(
                Constants.DISTRESS_STABLE_FAILURE,
                R.drawable.sd_stabilized_layer_failure);
    }
}
