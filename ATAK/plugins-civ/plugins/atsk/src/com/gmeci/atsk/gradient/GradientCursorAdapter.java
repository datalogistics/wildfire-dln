
package com.gmeci.atsk.gradient;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.atakmap.android.atsk.plugin.R;
import com.gmeci.core.ATSKConstants;
import com.gmeci.atskservice.resolvers.DBURIConstants;
import com.gmeci.atskservice.resolvers.GradientProviderClient;

import java.util.HashMap;
import com.atakmap.coremap.locale.LocaleUtil;

class GradientCursorAdapter extends CursorAdapter {
    final LayoutInflater inflater;

    final GradientProviderClient gpc;
    final HashMap<String, Integer> Type2ImageResourceMap;// = new HashMap<String, Integer>();
    int SelectedItem = -10;
    boolean Collection = false;
    private TextView _title, _detail,
            _detailLZ, _detailSH, _detailGR, _detailMN;
    private ImageView _icon;
    private final boolean ltfw;

    @SuppressWarnings("deprecation")
    public GradientCursorAdapter(Context pluginContext, Context context,
            Cursor c,
            GradientProviderClient gpc, boolean ltfw, boolean Collection) {
        super(context, c);
        this.gpc = gpc;
        this.Collection = Collection;
        this.ltfw = ltfw;
        inflater = LayoutInflater.from(pluginContext);

        Type2ImageResourceMap = new HashMap<String, Integer>();
        Type2ImageResourceMap.put(ATSKConstants.GRADIENT_TYPE_LONGITUDINAL,
                R.drawable.gradient_longitudinal);
        Type2ImageResourceMap.put(ATSKConstants.GRADIENT_TYPE_TRANSVERSE,
                R.drawable.gradient_transverse);
        Type2ImageResourceMap.put(
                ATSKConstants.GRADIENT_TYPE_LONGITUDINAL_GOOD,
                R.drawable.gradient_long_good);
        Type2ImageResourceMap.put(ATSKConstants.GRADIENT_TYPE_TRANSVERSE_GOOD,
                R.drawable.gradient_transverse_good);
        Type2ImageResourceMap.put(ATSKConstants.GRADIENT_TYPE_LONGITUDINAL_BAD,
                R.drawable.gradient_longitudinal_bad);
        Type2ImageResourceMap.put(ATSKConstants.GRADIENT_TYPE_TRANSVERSE_BAD,
                R.drawable.gradient_transverse_bad);
        Type2ImageResourceMap.put(
                ATSKConstants.GRADIENT_TYPE_TRANSVERSE_BAD_HIDDEN,
                R.drawable.gradient_transverse_hidden);
        Type2ImageResourceMap.put(
                ATSKConstants.GRADIENT_TYPE_TRANSVERSE_GOOD_HIDDEN,
                R.drawable.gradient_transverse_hidden);
        Type2ImageResourceMap.put(
                ATSKConstants.GRADIENT_TYPE_LONGITUDINAL_BAD_HIDDEN,
                R.drawable.gradient_longitudinal_hidden);
        Type2ImageResourceMap.put(
                ATSKConstants.GRADIENT_TYPE_LONGITUDINAL_GOOD_HIDDEN,
                R.drawable.gradient_longitudinal_hidden);

    }

    public void SetSelectedItem(int SelectedItem) {
        this.SelectedItem = SelectedItem;
    }

    @Override
    public void bindView(View mRow, Context arg1, Cursor cursor) {

        int curPos = cursor.getPosition();
        mRow.setBackgroundColor(
                curPos == SelectedItem ? 0xFF383838 : 0xFF000000);

        _title = (TextView) mRow.findViewById(R.id.title);
        _icon = (ImageView) mRow.findViewById(R.id.img);
        _detail = (TextView) mRow.findViewById(R.id.detail);
        _detailLZ = (TextView) mRow.findViewById(R.id.detail_lz);
        _detailSH = (TextView) mRow.findViewById(R.id.detail_sh);
        _detailGR = (TextView) mRow.findViewById(R.id.detail_gr);
        _detailMN = (TextView) mRow.findViewById(R.id.detail_mn);

        String UID = cursor.getString(cursor
                .getColumnIndex(DBURIConstants.COLUMN_UID));
        String Description = cursor.getString((cursor
                .getColumnIndex(DBURIConstants.COLUMN_DESCRIPTION)));
        String Group = cursor.getString(cursor
                .getColumnIndex(DBURIConstants.COLUMN_GROUP_NAME_LINE));
        int pointCount = gpc.GetPointsInGradientCount(Group, UID);

        String type = cursor.getString(3);

        //System.out.println("UID: " + UID + " DS: " + Description + " Type: " + Type);
        if (Description == null || Description.length() == 0) {
            _title.setText(UID);
        } else
            _title.setText(Description);

        if (type.startsWith(ATSKConstants.GRADIENT_TYPE_LONGITUDINAL)) {
            if (type.equals(ATSKConstants.GRADIENT_TYPE_LONGITUDINAL)) {
                _detail.setText(String.format(LocaleUtil.getCurrent(),
                        "Points: %d",
                        pointCount));
            } else {
                double overall = cursor.getDouble(DBURIConstants
                        .LZ_OVERALL_GRADIENT_COLUMN_POSITION);
                double interval = cursor.getDouble(DBURIConstants
                        .LZ_INTERVAL_GRADIENT_COLUMN_POSITION);
                _detail.setText(String.format(LocaleUtil.getCurrent(),
                        "Interval: %.1f  Overall: %.1f%nPoints: %d",
                        interval, overall, pointCount));
            }
            showTransverseDetails(false);
        } else {
            double lz_l = cursor
                    .getDouble(DBURIConstants.LZ_L_GRADIENT_COLUMN_POSITION);
            double lz_r = cursor
                    .getDouble(DBURIConstants.LZ_R_GRADIENT_COLUMN_POSITION);
            double sh_l = cursor
                    .getDouble(DBURIConstants.SHOULDER_L_GRADIENT_COLUMN_POSITION);
            double sh_r = cursor
                    .getDouble(DBURIConstants.SHOULDER_R_GRADIENT_COLUMN_POSITION);
            double gr_l = cursor
                    .getDouble(DBURIConstants.GRADED_L_GRADIENT_COLUMN_POSITION);
            double gr_r = cursor
                    .getDouble(DBURIConstants.GRADED_R_GRADIENT_COLUMN_POSITION);
            double mn_l = cursor
                    .getDouble(DBURIConstants.MAINTAINED_L_GRADIENT_COLUMN_POSITION);
            double mn_r = cursor
                    .getDouble(DBURIConstants.MAINTAINED_R_GRADIENT_COLUMN_POSITION);
            boolean noData = lz_l == 0 && lz_r == 0 && sh_l == 0 && sh_r == 0
                    && gr_l == 0 && gr_r == 0 && mn_l == 0 && mn_r == 0;

            if (!noData) {
                _detailLZ.setText(String.format(LocaleUtil.getCurrent(),
                        "LZ: %.1f %.1f",
                        lz_l, lz_r));
                _detailSH.setText(String.format(LocaleUtil.getCurrent(),
                        "%s: %.1f %.1f",
                        this.ltfw ? "A" : "SH", sh_l, sh_r));
                _detailGR.setText(String.format(LocaleUtil.getCurrent(),
                        "%s: %.1f %.1f",
                        this.ltfw ? "B" : "GR", gr_l, gr_r));
                _detailMN.setText(String.format(LocaleUtil.getCurrent(),
                        "MN: %.1f %.1f",
                        mn_l, mn_r));
                showTransverseDetails(true);
            } else {
                // No data - show point count instead
                _detail.setText(String.format(LocaleUtil.getCurrent(),
                        "Points: %d",
                        pointCount));
                showTransverseDetails(false);
            }
        }
        _icon.setImageResource(getImageResource(type));
    }

    private void showTransverseDetails(boolean show) {
        _detail.setVisibility(show ? View.GONE : View.VISIBLE);
        _detailLZ.setVisibility(show ? View.VISIBLE : View.GONE);
        _detailSH.setVisibility(show ? View.VISIBLE : View.GONE);
        _detailGR.setVisibility(show ? View.VISIBLE : View.GONE);
        _detailMN.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private int getImageResource(String TypeString) {
        if (Collection) {
            return R.drawable.gradient_good;
        }

        if (Type2ImageResourceMap.containsKey(TypeString)) {
            return Type2ImageResourceMap.get(TypeString);
        }

        return R.drawable.gradient_transverse;
    }

    @Override
    public View newView(Context arg0, Cursor cursor, ViewGroup parent) {
        return inflater.inflate(R.layout.gradient_list, parent, false);
    }

}
