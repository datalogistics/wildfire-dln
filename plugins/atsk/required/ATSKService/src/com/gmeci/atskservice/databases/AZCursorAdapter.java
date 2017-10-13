
package com.gmeci.atskservice.databases;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.gmeci.core.SurveyData;
import com.gmeci.atskservice.resolvers.AZProviderClient;
import com.gmeci.atskservice.resolvers.AZURIConstants;
import com.gmeci.atskservice.R;
import com.gmeci.conversions.Conversions;

import java.util.HashMap;

public class AZCursorAdapter extends CursorAdapter {
    LayoutInflater inflater;

    private HashMap<String, Integer> Type2ImageResourceMap;
    private String CurrentSurveyUID;
    private TextView title = null;
    private TextView detail = null;
    private ImageView i11 = null;

    @SuppressWarnings("deprecation")
    public AZCursorAdapter(Context context, Cursor c, AZProviderClient azpc,
            String CurrentSurveyUID) {
        super(context, c);
        inflater = LayoutInflater.from(context);
        this.CurrentSurveyUID = CurrentSurveyUID;
        Type2ImageResourceMap = new HashMap<String, Integer>();
        Type2ImageResourceMap.put(SurveyData.AZ_TYPE.DZ.name(), R.drawable.dz);
        Type2ImageResourceMap.put(SurveyData.AZ_TYPE.LZ.name(), R.drawable.lz);
        Type2ImageResourceMap
                .put(SurveyData.AZ_TYPE.STOL.name(), R.drawable.lz);
        Type2ImageResourceMap
                .put(SurveyData.AZ_TYPE.HLZ.name(), R.drawable.hlz);
        Type2ImageResourceMap.put(SurveyData.AZ_TYPE.FARP.name(),
                R.drawable.farp);
    }

    @Override
    public void bindView(View mRow, Context arg1, Cursor cursor) {

        String UID = cursor.getString(AZURIConstants.UID_INDEX);

        if (CurrentSurveyUID.compareTo(UID) == 0) {
            mRow.setBackgroundColor(0xFF383838);
        } else {
            mRow.setBackgroundColor(0xFF000000);
        }

        title = (TextView) mRow.findViewById(R.id.title);
        detail = (TextView) mRow.findViewById(R.id.detail);
        i11 = (ImageView) mRow.findViewById(R.id.img);

        //we should see how many points there are in this line.

        String Type = cursor.getString(AZURIConstants.TYPE_INDEX);
        String name = cursor.getString(AZURIConstants.NAME_INDEX);
        title.setText(name);

        detail.setText(Conversions.GetMGRS(
                cursor.getDouble(AZURIConstants.COLUMN_LAT_INDEX),
                cursor.getDouble(AZURIConstants.COLUMN_LON_INDEX)));
        i11.setImageResource(getImageResource(Type));
    }

    private int getImageResource(String TypeString) {
        if (Type2ImageResourceMap.containsKey(TypeString)) {
            return Type2ImageResourceMap.get(TypeString);
        }

        return R.drawable.gradient_long_good;
    }

    @Override
    public View newView(Context arg0, Cursor cursor, ViewGroup parent) {
        return inflater.inflate(R.layout.az_list_row, parent, false);
    }

}
