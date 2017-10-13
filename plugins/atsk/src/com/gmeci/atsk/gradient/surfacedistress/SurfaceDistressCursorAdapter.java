
package com.gmeci.atsk.gradient.surfacedistress;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.atakmap.android.atsk.plugin.R;
import com.gmeci.atsk.resources.ATSKApplication;
import com.gmeci.atskservice.resolvers.DBURIConstants;
import com.gmeci.constants.Constants;

import java.util.HashMap;

public class SurfaceDistressCursorAdapter extends CursorAdapter {
    final LayoutInflater inflater;

    final HashMap<String, Integer> Type2ImageResourceMap = new HashMap<String, Integer>();
    int SelectedItem = -10;
    private TextView title = null;
    private TextView detail = null;
    private ImageView i11 = null;

    @SuppressWarnings("deprecation")
    public SurfaceDistressCursorAdapter(Context context, Cursor c) {
        super(context, c);

        Context pluginContext = ATSKApplication
                .getInstance().getPluginContext();

        inflater = LayoutInflater.from(pluginContext);

        Type2ImageResourceMap.put(Constants.DISTRESS_DUST,
                R.drawable.sd_dust);
        Type2ImageResourceMap.put(Constants.DISTRESS_JET_EROSION,
                R.drawable.sd_jet_blast_erosion);
        Type2ImageResourceMap.put(Constants.DISTRESS_LOOSE_AGG,
                R.drawable.sd_aggregate);
        Type2ImageResourceMap.put(Constants.DISTRESS_POTHOLE,
                R.drawable.sd_pothole);
        Type2ImageResourceMap.put(
                Constants.DISTRESS_ROLLING_RESISTANT,
                R.drawable.sd_rolling_resist);
        Type2ImageResourceMap.put(Constants.DISTRESS_RUTS,
                R.drawable.sd_ruts);
        Type2ImageResourceMap.put(
                Constants.DISTRESS_STABLE_FAILURE,
                R.drawable.sd_stabilized_layer_failure);
    }

    public void SetSelectedItem(int SelectedItem) {
        this.SelectedItem = SelectedItem;
    }

    @Override
    public void bindView(View mRow, Context arg1, Cursor cursor) {

        int currentPostion = cursor.getPosition();
        boolean Selected = (currentPostion == SelectedItem);

        title = (TextView) mRow.findViewById(R.id.title);
        detail = (TextView) mRow.findViewById(R.id.detail);
        i11 = (ImageView) mRow.findViewById(R.id.img);
        //we should see how many points there are in this line.

        String Type = cursor.getString((cursor
                .getColumnIndex(DBURIConstants.COLUMN_TYPE)));
        String[] TypeLevel = Type.split("_");
        if (TypeLevel.length < 2) {
            title.setText("Distress");
        } else {
            title.setText(TypeLevel[0]);
            detail.setText(TypeLevel[1]);
            i11.setImageResource(getImageResource(TypeLevel[0].replace("_", "")));
            //set background based on distress level
            if (!TypeLevel[1].contains("2")) {
                title.setTextColor(Color.BLACK);
                detail.setTextColor(Color.BLACK);
            } else {
                title.setTextColor(Color.WHITE);
                detail.setTextColor(Color.WHITE);
            }

            mRow.setBackgroundColor(GetBGColor(Selected, TypeLevel[1]));

        }
    }

    private int GetBGColor(boolean Selected, String Type) {
        if (Type.contains("0")) {
            if (Selected)
                return 0xff009933;
            return (0xFF00FF00);
        } else if (Type.contains("1")) {
            if (Selected)
                return 0xffccff66;
            return (0xFFFFFF00);
        } else {
            if (Selected)
                return 0xff990000;
            return (0xFFF00F00);
        }
    }

    private int getImageResource(String TypeString) {
        if (Type2ImageResourceMap.containsKey(TypeString)) {
            return Type2ImageResourceMap.get(TypeString);
        }

        return R.drawable.gradient_long_good;
    }

    @Override
    public View newView(Context arg0, Cursor cursor, ViewGroup parent) {
        return inflater.inflate(R.layout.gradient_list, parent, false);
    }

}
