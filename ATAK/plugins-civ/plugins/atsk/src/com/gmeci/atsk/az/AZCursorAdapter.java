
package com.gmeci.atsk.az;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.atakmap.android.atsk.plugin.R;
import com.gmeci.atsk.resources.ATSKApplication;
import com.gmeci.core.SurveyData;
import com.gmeci.atskservice.resolvers.AZProviderClient;
import com.gmeci.atskservice.resolvers.AZURIConstants;
import com.gmeci.conversions.Conversions;

import java.util.HashMap;

public class AZCursorAdapter extends CursorAdapter {
    @SuppressWarnings("unused")
    private static final String TAG = "AZCursorAdapter";
    private final LayoutInflater inflater;
    private HashMap<String, Integer> Type2ImageResourceMap;

    private String _surveyUID = "";
    private final AZProviderClient _azpc;

    static private class Holder {
        CheckBox visibleCheckbox = null;
        TextView title = null;
        TextView detail = null;
        ImageView i11 = null;
        ImageView currentImage = null;
        boolean checkStatus = false;
        String Type;
        String UID;
        String NAME;
    }

    @SuppressWarnings("deprecation")
    public AZCursorAdapter(Context context, Cursor c, AZProviderClient azpc) {
        super(context, c);

        Context pluginContext = ATSKApplication
                .getInstance().getPluginContext();
        inflater = LayoutInflater.from(pluginContext);
        _azpc = azpc;
        setupType2ImageMap();
    }

    public AZCursorAdapter(Context context, Cursor c, AZProviderClient azpc,
            String currentSurveyUID) {
        super(context, c, 0);

        Context pluginContext = ATSKApplication
                .getInstance().getPluginContext();

        inflater = LayoutInflater.from(pluginContext);
        _azpc = azpc;

        setupType2ImageMap();

        _surveyUID = currentSurveyUID;
    }

    public void SetSelectedUID(String uid) {
        _surveyUID = uid;
    }

    private void setupType2ImageMap() {

        Type2ImageResourceMap = new HashMap<String, Integer>();
        Type2ImageResourceMap.put(SurveyData.AZ_TYPE.DZ.name(),
                R.drawable.navigation_dz);
        Type2ImageResourceMap.put(SurveyData.AZ_TYPE.LZ.name(),
                R.drawable.navigation_lz);
        Type2ImageResourceMap
                .put(SurveyData.AZ_TYPE.HLZ.name(), R.drawable.navigation_hlz);
        Type2ImageResourceMap
                .put(SurveyData.AZ_TYPE.STOL.name(), R.drawable.navigation_lz);
        Type2ImageResourceMap.put(SurveyData.AZ_TYPE.FARP.name(),
                R.drawable.navigation_farp);
    }

    @Override
    public void bindView(View mRow, Context arg1, final Cursor cursor) {

        Holder tag = (Holder) mRow.getTag();
        final Holder holder;
        if (tag == null) {
            final Holder h = new Holder();

            h.visibleCheckbox = (CheckBox) mRow.findViewById(R.id.az_visible);
            h.title = (TextView) mRow.findViewById(R.id.title);
            h.detail = (TextView) mRow.findViewById(R.id.detail);
            h.i11 = (ImageView) mRow.findViewById(R.id.img);
            h.currentImage = (ImageView) mRow.findViewById(R.id.current);

            mRow.setTag(h);
            holder = h;
        } else {
            holder = tag;
        }

        //we should see how many points there are in this line.

        holder.UID = cursor.getString(AZURIConstants.UID_INDEX);
        holder.Type = cursor.getString(AZURIConstants.TYPE_INDEX);
        holder.NAME = cursor.getString(AZURIConstants.NAME_INDEX);
        holder.title.setText(holder.NAME);

        holder.detail.setText(Conversions.GetMGRS(
                cursor.getDouble(AZURIConstants.COLUMN_LAT_INDEX),
                cursor.getDouble(AZURIConstants.COLUMN_LON_INDEX)));
        holder.i11.setImageResource(getImageResource(holder.Type));

        String checkedString = cursor
                .getString(AZURIConstants.COLUMN_VISIBLE_INDEX);
        holder.checkStatus = Boolean.parseBoolean(checkedString);

        holder.visibleCheckbox.setChecked(holder.checkStatus);
        holder.visibleCheckbox
                .setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(CompoundButton buttonView,
                            boolean isChecked) {
                        holder.checkStatus = isChecked;
                        //Log.d(TAG, holder.NAME + " is checked: " + isChecked);
                        _azpc.setVisibility(holder.UID, holder.checkStatus);
                        cursor.requery();
                    }

                });
        //Log.d(TAG, "name: " + holder.NAME + " checkbox: " + holder.visibleCheckbox.isChecked() + " uid: " + holder.UID);

        if (holder.UID.equals(_surveyUID)) {
            holder.currentImage.setVisibility(View.VISIBLE);
            holder.currentImage.setImageResource(R.drawable.atsk_star);
            holder.visibleCheckbox.setVisibility(View.GONE);
            mRow.setBackgroundColor(0xFF383838);
        } else {
            holder.currentImage.setVisibility(View.INVISIBLE);
            holder.visibleCheckbox.setVisibility(View.VISIBLE);
            mRow.setBackgroundColor(0xFF000000);
        }

    }

    private int getImageResource(String TypeString) {
        if (Type2ImageResourceMap.containsKey(TypeString)) {
            return Type2ImageResourceMap.get(TypeString);
        }

        return R.drawable.navigation_lz;
    }

    @Override
    public View newView(Context arg0, Cursor cursor, ViewGroup parent) {
        return inflater
                .inflate(R.layout.current_survey_list_row, parent, false);
    }

}
