
package com.gmeci.atsk.obstructions;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.atakmap.android.atsk.plugin.R;
import com.gmeci.atsk.resources.ATSKApplication;
import com.gmeci.constants.Constants;

import java.util.HashMap;
import java.util.List;

public class ObstructionSpinnerAdapter extends ArrayAdapter<String> {

    private static final String TAG = "ObstructionSpinnerAdapter";
    private final List<String> _selections;
    private static HashMap<String, Integer> _resourceMap = null;
    private final LayoutInflater _inflater;

    public ObstructionSpinnerAdapter(Context context, int resource,
            List<String> selections) {

        super(context, resource, selections);

        _selections = selections;
        _inflater = LayoutInflater.from(ATSKApplication
                .getInstance().getPluginContext());
        fillResourceMap();
    }

    private synchronized static void fillResourceMap() {
        if (_resourceMap != null)
            return;
        _resourceMap = new HashMap<String, Integer>();
        _resourceMap.put(Constants.LO_BERMS,
                R.drawable.lo_berms);
        _resourceMap.put(Constants.LO_CANAL,
                R.drawable.lo_canal);
        _resourceMap.put(Constants.LO_CONTOUR_LINE,
                R.drawable.lo_contour_line);
        _resourceMap.put(Constants.LO_CURB,
                R.drawable.lo_curb);
        _resourceMap.put(Constants.LO_DITCH,
                R.drawable.lo_ditch);
        _resourceMap.put(Constants.LO_FENCELINE,
                R.drawable.lo_fenceline);
        _resourceMap.put(Constants.LO_GENERIC_ROUTE,
                R.drawable.lo_generic_route);
        _resourceMap.put(Constants.LO_GUARD_RAIL,
                R.drawable.lo_guard_rail);
        _resourceMap.put(Constants.LO_HEDGES,
                R.drawable.lo_hedges);
        _resourceMap.put(Constants.LO_HIGHWAY,
                R.drawable.lo_highway);
        _resourceMap.put(Constants.LO_HILL,
                R.drawable.lo_hill);
        _resourceMap.put(Constants.LO_LIGHT_POLE_WIRES,
                R.drawable.lo_light_pole_w_wires);
        _resourceMap.put(Constants.LO_MOUNTAIN,
                R.drawable.lo_mountain);
        _resourceMap.put(Constants.LO_PATH,
                R.drawable.lo_path);
        _resourceMap.put(Constants.LO_MOUND,
                R.drawable.lo_mound);
        _resourceMap.put(Constants.LO_PIPELINE,
                R.drawable.lo_pipeline);
        _resourceMap.put(Constants.LO_POLES_WIRES,
                R.drawable.lo_poles_w_wires);
        _resourceMap.put(Constants.LO_POWERLINES,
                R.drawable.lo_powerlines);
        _resourceMap.put(Constants.LO_RAILROAD,
                R.drawable.lo_railroad);
        _resourceMap.put(Constants.LO_RIDGELINE,
                R.drawable.lo_ridgeline);
        _resourceMap.put(Constants.LO_RIVER,
                R.drawable.lo_river);
        _resourceMap.put(Constants.LO_ROAD,
                R.drawable.lo_road);
        _resourceMap.put(Constants.LO_RUTS,
                R.drawable.lo_ruts);
        _resourceMap.put(Constants.LO_SLOPE,
                R.drawable.lo_slope);
        _resourceMap.put(Constants.LO_WALL,
                R.drawable.lo_wall);
        _resourceMap.put(Constants.LO_TRAIL,
                R.drawable.lo_trail);
        _resourceMap.put(Constants.LO_TREELINE,
                R.drawable.lo_treeline);
        _resourceMap.put(Constants.LO_WIRES,
                R.drawable.lo_wires);

        _resourceMap.put(Constants.AO_POOL,
                R.drawable.po_pool);
        _resourceMap.put(Constants.AO_GRAVEL,
                R.drawable.lo_gravel);
        _resourceMap.put(Constants.AO_LAKE,
                R.drawable.lo_lake);
        _resourceMap.put(Constants.AO_POND,
                R.drawable.lo_pond);
        _resourceMap.put(Constants.AO_OCEAN,
                R.drawable.lo_ocean);
        _resourceMap.put(Constants.AO_SAND,
                R.drawable.lo_sand);
        _resourceMap.put(Constants.AO_TREES,
                R.drawable.lo_trees);
        _resourceMap.put(Constants.AO_SWAMP,
                R.drawable.lo_swamp);
        _resourceMap.put(Constants.AO_GENERIC_AREA,
                R.drawable.lo_generic_area);
        _resourceMap.put(Constants.AO_APRON,
                R.drawable.ao_apron);
        _resourceMap.put(Constants.AO_BUSHES,
                R.drawable.po_bush);
        _resourceMap.put(Constants.AO_CRATERS,
                R.drawable.po_crater);
        _resourceMap.put(Constants.AO_BUILDINGS,
                R.drawable.po_building);
        _resourceMap.put(Constants.PO_LABEL,
                R.drawable.po_label);
        _resourceMap.put(Constants.PO_RAB_LINE,
                R.drawable.po_rab_line);
        _resourceMap.put(Constants.PO_RAB_CIRCLE,
                R.drawable.po_rab_circle);
        _resourceMap.put(Constants.PO_TREE,
                R.drawable.po_tree);
        _resourceMap.put(
                Constants.PO_AIRFIELD_INSTRUMENT,
                R.drawable.po_airfield_instrument);
        _resourceMap.put(Constants.PO_ANTENNA,
                R.drawable.po_antenna);
        _resourceMap.put(Constants.PO_BERMS,
                R.drawable.po_berms);
        _resourceMap.put(Constants.PO_BUILDING,
                R.drawable.po_building);
        _resourceMap.put(Constants.PO_BUSH,
                R.drawable.po_bush);
        _resourceMap.put(Constants.LO_CANAL,
                R.drawable.po_canal);
        _resourceMap.put(Constants.PO_CRATER,
                R.drawable.po_crater);
        _resourceMap.put(Constants.PO_DUMPSTER,
                R.drawable.po_dumpster);
        _resourceMap.put(Constants.PO_FIRE_HYDRANT,
                R.drawable.po_fire_hydrant);
        _resourceMap.put(Constants.PO_FLAGPOLE,
                R.drawable.po_flagpole);
        _resourceMap.put(Constants.PO_FUEL_TANK,
                R.drawable.po_fuel_tank);
        _resourceMap.put(Constants.PO_GENERIC_POINT,
                R.drawable.po_generic_point);
        _resourceMap.put(Constants.PO_HVAC_UNIT,
                R.drawable.po_hvac_unit);
        _resourceMap.put(Constants.PO_LEDGE,
                R.drawable.po_ledge);
        _resourceMap.put(Constants.PO_LIGHT,
                R.drawable.po_light);
        _resourceMap.put(Constants.PO_LIGHT_POLE,
                R.drawable.po_light_pole);
        _resourceMap.put(Constants.PO_MOUND,
                R.drawable.po_mound);
        _resourceMap.put(Constants.PO_PEAK,
                R.drawable.po_peak);
        _resourceMap.put(Constants.PO_POLE,
                R.drawable.po_pole);
        _resourceMap.put(Constants.PO_PYLON,
                R.drawable.po_pylon);
        _resourceMap.put(Constants.PO_ROTATING_BEACON,
                R.drawable.po_rotating_beacon);
        _resourceMap.put(Constants.PO_SAT_DISH,
                R.drawable.po_sat_dish);
        _resourceMap.put(Constants.PO_TRANSFORMER,
                R.drawable.po_transformer);
        _resourceMap.put(Constants.PO_WINDSOCK,
                R.drawable.po_windsock);
        _resourceMap.put(Constants.PO_WXVANE,
                R.drawable.po_wxvane);
        _resourceMap.put(Constants.PO_SIGN,
                R.drawable.po_sign_tv);
        _resourceMap.put(Constants.PO_CBR_HIDDEN,
                R.drawable.cbr_hidden);
        _resourceMap.put(Constants.LO_TAXIWAY,
                R.drawable.lo_taxiway);

        _resourceMap.put(Constants.DISTRESS_DUST + "_0",
                R.drawable.sd_dust_green);
        _resourceMap.put(Constants.DISTRESS_DUST + "_1",
                R.drawable.sd_dust_yellow);
        _resourceMap.put(Constants.DISTRESS_DUST + "_2",
                R.drawable.sd_dust_red);

        _resourceMap.put(Constants.DISTRESS_JET_EROSION
                + "_0", R.drawable.sd_jet_blast_erosion_green);
        _resourceMap.put(Constants.DISTRESS_JET_EROSION
                + "_1", R.drawable.sd_jet_blast_erosion_yellow);
        _resourceMap.put(Constants.DISTRESS_JET_EROSION
                + "_2", R.drawable.sd_jet_blast_erosion_red);

        _resourceMap.put(Constants.DISTRESS_LOOSE_AGG
                + "_0", R.drawable.sd_aggregate_green);
        _resourceMap.put(Constants.DISTRESS_LOOSE_AGG
                + "_1", R.drawable.sd_aggregate_yellow);
        _resourceMap.put(Constants.DISTRESS_LOOSE_AGG
                + "_2", R.drawable.sd_aggregate_red);

        _resourceMap.put(Constants.DISTRESS_POTHOLE
                + "_0", R.drawable.sd_pothole_green);
        _resourceMap.put(Constants.DISTRESS_POTHOLE
                + "_1", R.drawable.sd_pothole_yellow);
        _resourceMap.put(Constants.DISTRESS_POTHOLE
                + "_2", R.drawable.sd_pothole_red);

        _resourceMap.put(
                Constants.DISTRESS_ROLLING_RESISTANT + "_0",
                R.drawable.sd_rolling_resist_green);
        _resourceMap.put(
                Constants.DISTRESS_ROLLING_RESISTANT + "_1",
                R.drawable.sd_rolling_resist_yellow);
        _resourceMap.put(
                Constants.DISTRESS_ROLLING_RESISTANT + "_2",
                R.drawable.sd_rolling_resist_red);

        _resourceMap.put(Constants.DISTRESS_RUTS + "_0",
                R.drawable.sd_ruts_green);
        _resourceMap.put(Constants.DISTRESS_RUTS + "_1",
                R.drawable.sd_ruts_yellow);
        _resourceMap.put(Constants.DISTRESS_RUTS + "_2",
                R.drawable.sd_ruts_red);

        _resourceMap.put(
                Constants.DISTRESS_STABLE_FAILURE + "_0",
                R.drawable.sd_stabilized_layer_failure_green);
        _resourceMap.put(
                Constants.DISTRESS_STABLE_FAILURE + "_1",
                R.drawable.sd_stabilized_layer_failure_yellow);
        _resourceMap.put(
                Constants.DISTRESS_STABLE_FAILURE + "_2",
                R.drawable.sd_stabilized_layer_failure_red);
    }

    public synchronized static int getResource(String type) {
        fillResourceMap();
        Integer res = _resourceMap.get(type);
        return (res == null ? 0 : res);
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

        int resId = getResource(_selections.get(position));
        View row = convertView;
        if (row == null)
            row = _inflater.inflate(R.layout.spinner_row_image, parent, false);

        ImageView icon = (ImageView) row.findViewById(R.id.icon);
        if (icon != null) {
            icon.setVisibility(resId == 0 ? View.GONE : View.VISIBLE);
            icon.setBackgroundResource(resId);
        }

        TextView label = (TextView) row.findViewById(R.id.type);
        String type = _selections.get(position);
        label.setText(type);
        return row;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        int resId = getResource(_selections.get(position));
        View row = convertView;
        if (row == null)
            row = _inflater.inflate(resId == 0 ?
                    R.layout.spinner_button_noleft_border
                    : R.layout.spinner_button_bordered_image,
                    parent, false);

        if (resId != 0) {
            ImageView icon = (ImageView) row.findViewById(R.id.icon);
            if (icon != null)
                icon.setBackgroundResource(resId);
        }
        TextView label = (TextView) row.findViewById(R.id.type);
        String type = _selections.get(position);
        label.setText(type);
        return row;
    }

}
