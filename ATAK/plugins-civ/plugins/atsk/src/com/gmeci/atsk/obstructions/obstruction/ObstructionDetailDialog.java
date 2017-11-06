
package com.gmeci.atsk.obstructions.obstruction;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.atakmap.android.atsk.plugin.R;
import com.atakmap.android.maps.MapView;
import com.gmeci.atsk.obstructions.ObstructionSpinnerAdapter;
import com.gmeci.core.ATSKConstants;
import com.gmeci.core.LineObstruction;
import com.gmeci.core.PointObstruction;
import com.gmeci.constants.Constants;
import com.gmeci.conversions.Conversions;
import com.gmeci.core.SurveyPoint;

import java.text.DecimalFormat;

public class ObstructionDetailDialog {

    private static final String TAG = "ObstructionDetailDialog";
    private static final int NAME_INDEX = 0;
    private static final int TYPE_INDEX = 1;
    private static final int HEIGHT_INDEX = 2;
    private static final int LENGTH_INDEX = 3;
    private static final int WIDTH_INDEX = 4;
    private static final int ANGLE_INDEX = 5;
    private static final int ELEVATION_INDEX = 6;
    private static final int POSITION_INDEX = 7;
    private static final int METHOD_INDEX = 8;
    private static final int EXTRA_INDEX = 9;
    private static final int MAX_TVS = 10;
    private static final DecimalFormat DEC_FMT = new DecimalFormat("#.#");

    private final MapView _mapView;
    private final Context _context, _plugin;
    private final View _root;
    private PointObstruction _pointObs;
    private LineObstruction _lineObs;
    private final TextView[] StaticTV = new TextView[MAX_TVS];
    private final TextView[] ValueTV = new TextView[MAX_TVS];
    private final TextView[] UnitsTV = new TextView[MAX_TVS];
    private String _coordFormat, _unitFormat, _angleFormat;
    private ImageView _icon;

    public ObstructionDetailDialog(MapView mapView, Context plugin) {
        _mapView = mapView;
        _context = mapView.getContext();
        _plugin = plugin;
        _root = LayoutInflater.from(plugin).inflate(
                R.layout.obstruction_detail_dialog, _mapView, false);
    }

    public void show(PointObstruction po) {
        _pointObs = po;
        show();
    }

    public void show(LineObstruction lo) {
        _lineObs = lo;
        show();
    }

    private void show() {
        if (_pointObs == null && _lineObs == null) {
            Toast.makeText(_context, _plugin.getString(R.string.
                    obs_details_failed), Toast.LENGTH_LONG).show();
            return;
        }
        SetupTextViews();
        _icon = (ImageView) _root.findViewById(R.id.type_image);
        SharedPreferences units_settings = PreferenceManager
                .getDefaultSharedPreferences(_mapView.getContext());
        _coordFormat = units_settings.getString(
                ATSKConstants.COORD_FORMAT, Conversions.COORD_FORMAT_MGRS);
        _unitFormat = units_settings.getString(ATSKConstants.UNITS_DISPLAY,
                ATSKConstants.UNITS_FEET);
        _angleFormat = units_settings.getString(ATSKConstants.UNITS_ANGLE,
                ATSKConstants.UNITS_ANGLE_MAG);
        UpdateScreen();

        AlertDialog.Builder b = new AlertDialog.Builder(_mapView.getContext());
        b.setTitle(_plugin.getString(R.string.obs_details_title));
        b.setView(_root);
        b.setPositiveButton(_plugin.getString(R.string.ok), null);
        b.show();
    }

    private void UpdateScreen() {
        String type = "", name = "";
        double width = 0, height = 0, length = 0;
        int iconId = 0;
        SurveyPoint.CollectionMethod method = null;
        Resources res = _plugin.getResources();
        String unit = " " + Conversions.GetMeasurementUnit(_unitFormat, true);

        if (_lineObs != null) {
            if (_lineObs.remarks != null)
                name = _lineObs.remarks;
            type = _lineObs.type;
            width = _lineObs.width;
            height = _lineObs.height;
            length = _lineObs.getLength();
            iconId = ObstructionSpinnerAdapter.getResource(_lineObs.type);
            method = _lineObs.getCenter().collectionMethod;

            StaticTV[POSITION_INDEX].setText(_plugin.getString(
                    R.string.points2));
            ValueTV[POSITION_INDEX].setText(String.valueOf(
                    _lineObs.points.size()));

            if (Constants.isArea(type))
                StaticTV[LENGTH_INDEX].setText(_plugin.getString(
                        R.string.perimeter));
        } else if (_pointObs != null) {
            if (_pointObs.remark != null)
                name = _pointObs.remark;
            type = _pointObs.type;
            width = _pointObs.width;
            height = _pointObs.height;
            length = _pointObs.length;
            method = _pointObs.collectionMethod;

            if (_pointObs.group.equals(ATSKConstants.VEHICLE_GROUP))
                iconId = R.drawable.aircraft_tab;
            else
                iconId = ObstructionSpinnerAdapter.getResource(type);

            ValueTV[POSITION_INDEX].setText(Conversions
                    .getCoordinateString(_pointObs.lat, _pointObs.lon,
                            _coordFormat));

            if (!_pointObs.alt.isValid()) {
                ValueTV[ELEVATION_INDEX].setText(_plugin.getString(
                        R.string.unknown));
                UnitsTV[ELEVATION_INDEX].setText("");
            } else {
                ValueTV[ELEVATION_INDEX].setText(DEC_FMT.format(
                        Conversions.GetMeasurement(_pointObs.getMSL(),
                                _unitFormat,
                                true)));
                UnitsTV[ELEVATION_INDEX].setText(unit);
            }

            _pointObs.course_true = Conversions.deg360(_pointObs.course_true);
            if (_angleFormat.equals(ATSKConstants.UNITS_ANGLE_MAG)) {
                double Angle2Display = Conversions.GetMagAngle(
                        _pointObs.course_true, _pointObs.lat, _pointObs.lon);

                ValueTV[ANGLE_INDEX].setText(DEC_FMT.format(Angle2Display));
                UnitsTV[ANGLE_INDEX].setText(String.format("%cM",
                        ATSKConstants.DEGREE_SYMBOL));
            } else {
                ValueTV[ANGLE_INDEX].setText(DEC_FMT.format(
                        _pointObs.course_true));
                UnitsTV[ANGLE_INDEX].setText(String.format("%cT",
                        ATSKConstants.DEGREE_SYMBOL));
            }
        }

        int maxNameLen = 40;
        if (name.length() > maxNameLen) {
            int firstLine = name.indexOf("\n");
            if (firstLine < maxNameLen && firstLine > 2)
                maxNameLen = firstLine - 1;
            StaticTV[EXTRA_INDEX].setVisibility(View.VISIBLE);
            StaticTV[EXTRA_INDEX].setText(name);
            name = name.substring(0, maxNameLen);
        }
        ValueTV[NAME_INDEX].setText(name);
        ValueTV[TYPE_INDEX].setText(type);
        _icon.setImageDrawable(iconId > 0 ? res.getDrawable(iconId) : null);

        ValueTV[WIDTH_INDEX].setText(DEC_FMT.format(Conversions
                .GetMeasurement(width, _unitFormat, true)));
        UnitsTV[WIDTH_INDEX].setText(unit);

        ValueTV[HEIGHT_INDEX].setText(DEC_FMT.format(Conversions
                .GetMeasurement(height, _unitFormat, true)));
        UnitsTV[HEIGHT_INDEX].setText(unit);

        ValueTV[LENGTH_INDEX].setText(DEC_FMT.format(Conversions
                .GetMeasurement(length, _unitFormat, true)));
        UnitsTV[LENGTH_INDEX].setText(unit);

        ValueTV[METHOD_INDEX].setText(method != null ? method.name
                : _plugin.getString(R.string.unknown));
        ValueTV[METHOD_INDEX].setBackgroundColor(method != null
                ? method.color : 0);

        HideShowFields(type);
    }

    private void ChangeVisibility(int Index, int ShowHide) {

        if (StaticTV[Index] != null)
            StaticTV[Index].setVisibility(ShowHide);
        if (ValueTV[Index] != null)
            ValueTV[Index].setVisibility(ShowHide);
        if (UnitsTV[Index] != null)
            UnitsTV[Index].setVisibility(ShowHide);
    }

    protected boolean HideShowFields(String type) {
        if (_lineObs == null && _pointObs == null)
            return false;

        if (Constants.isTaxiway(type))
            ChangeVisibility(HEIGHT_INDEX, View.GONE);

        String group = _lineObs != null ? _lineObs.group : _pointObs.group;
        if (_lineObs != null) {
            if (Constants.isArea(_lineObs.type)) {
                ChangeVisibility(ANGLE_INDEX, View.GONE);
                ChangeVisibility(ELEVATION_INDEX, View.GONE);
                ChangeVisibility(WIDTH_INDEX, View.GONE);
            } else if (Constants.isLine(_lineObs.type)) {
                ChangeVisibility(ANGLE_INDEX, View.GONE);
                ChangeVisibility(ELEVATION_INDEX, View.GONE);
            } else if (group.equals(ATSKConstants.APRON_GROUP)) {
                ChangeVisibility(ANGLE_INDEX, View.VISIBLE);
                ChangeVisibility(WIDTH_INDEX, View.VISIBLE);
                ChangeVisibility(HEIGHT_INDEX, View.GONE);
                ChangeVisibility(ELEVATION_INDEX, View.GONE);
                StaticTV[LENGTH_INDEX].setText(R.string.length);
            }
        }
        //must be a point
        else if (group.equals(ATSKConstants.CBR_GROUP)) {
            ChangeVisibility(ANGLE_INDEX, View.GONE);
            ChangeVisibility(WIDTH_INDEX, View.GONE);
            ChangeVisibility(HEIGHT_INDEX, View.GONE);
            ChangeVisibility(LENGTH_INDEX, View.GONE);
            ChangeVisibility(ELEVATION_INDEX, View.GONE);

            StaticTV[LENGTH_INDEX].setText(R.string.length);
        } else if (Constants.isPointWithLW(type)) {
            //hide diameter
            ChangeVisibility(ANGLE_INDEX, View.VISIBLE);
            ChangeVisibility(WIDTH_INDEX, View.VISIBLE);

            StaticTV[LENGTH_INDEX].setText(R.string.length);
        } else if (group.equals(ATSKConstants.VEHICLE_GROUP)) {
            ChangeVisibility(ANGLE_INDEX, View.VISIBLE);
            ChangeVisibility(WIDTH_INDEX, View.VISIBLE);
            ChangeVisibility(HEIGHT_INDEX, View.VISIBLE);
            ChangeVisibility(LENGTH_INDEX, View.VISIBLE);
            ChangeVisibility(ELEVATION_INDEX, View.VISIBLE);

            StaticTV[LENGTH_INDEX].setText(R.string.length);
        } else {
            //hide l/w/rotation
            ChangeVisibility(ANGLE_INDEX, View.GONE);
            ChangeVisibility(WIDTH_INDEX, View.GONE);

            StaticTV[LENGTH_INDEX].setText(R.string.diameter2);

            //    UnitsTV[ROTATION_POSITION].setVisibility(View.GONE);
        }

        return false;
    }

    private void SetupTextViews() {
        StaticTV[NAME_INDEX] = (TextView) _root.findViewById(R.id.Name_static);
        ValueTV[NAME_INDEX] = (TextView) _root.findViewById(R.id.Name);

        StaticTV[TYPE_INDEX] = (TextView) _root.findViewById(R.id.Type_static);
        ValueTV[TYPE_INDEX] = (TextView) _root.findViewById(R.id.Type);

        StaticTV[HEIGHT_INDEX] = (TextView) _root
                .findViewById(R.id.Height_static);
        ValueTV[HEIGHT_INDEX] = (TextView) _root.findViewById(R.id.Height);
        UnitsTV[HEIGHT_INDEX] = (TextView) _root
                .findViewById(R.id.Height_units);

        StaticTV[LENGTH_INDEX] = (TextView) _root
                .findViewById(R.id.Length_static);
        ValueTV[LENGTH_INDEX] = (TextView) _root.findViewById(R.id.Length);
        UnitsTV[LENGTH_INDEX] = (TextView) _root
                .findViewById(R.id.Length_units);

        StaticTV[WIDTH_INDEX] = (TextView) _root
                .findViewById(R.id.Width_static);
        ValueTV[WIDTH_INDEX] = (TextView) _root.findViewById(R.id.Width);
        UnitsTV[WIDTH_INDEX] = (TextView) _root.findViewById(R.id.Width_units);

        StaticTV[ANGLE_INDEX] = (TextView) _root
                .findViewById(R.id.Rotation_static);
        ValueTV[ANGLE_INDEX] = (TextView) _root.findViewById(R.id.Rotation);
        UnitsTV[ANGLE_INDEX] = (TextView) _root
                .findViewById(R.id.Rotation_units);

        StaticTV[ELEVATION_INDEX] = (TextView) _root
                .findViewById(R.id.BaseAlt_static);
        ValueTV[ELEVATION_INDEX] = (TextView) _root.findViewById(R.id.BaseAlt);
        UnitsTV[ELEVATION_INDEX] = (TextView) _root
                .findViewById(R.id.BaseAlt_units);

        StaticTV[POSITION_INDEX] = (TextView) _root
                .findViewById(R.id.Location_static);
        ValueTV[POSITION_INDEX] = (TextView) _root.findViewById(R.id.Location);

        StaticTV[METHOD_INDEX] = (TextView) _root
                .findViewById(R.id.method_static);
        ValueTV[METHOD_INDEX] = (TextView) _root.findViewById(R.id.method);

        StaticTV[EXTRA_INDEX] = (TextView) _root.findViewById(R.id.extra);

        for (int i = 0; i < MAX_TVS; i++) {
            if (StaticTV[i] != null)
                StaticTV[i].setTextColor(Color.WHITE);
            if (ValueTV[i] != null)
                ValueTV[i].setTextColor(Color.WHITE);
            if (UnitsTV[i] != null)
                UnitsTV[i].setTextColor(Color.WHITE);
        }
    }

}
