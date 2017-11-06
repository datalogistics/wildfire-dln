
package com.gmeci.atsk.obstructions.obstruction;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;

import com.atakmap.android.atsk.plugin.R;
import com.gmeci.atsk.resources.ATSKApplication;
import com.gmeci.core.ATSKConstants;
import com.gmeci.conversions.Conversions;

public class GPSAngle_OffsetDialog {

    private static final int ERROR_BG = 0xFF490000;
    private static final int NO_ERROR_BG = 0xFF383838;
    private static final String TAG = "GPSAngle_OffsetDialog";

    private static final String GPSOD_ANGLE = "GPSOD_ANGLE";
    private static final String GPSOD_RANGE = "GPSOD_RANGE";
    private static final String GPSOD_UNITS_M = "GPSOD_UNITS_M";
    private static final String GPSOD_HEADING_TRUE = "GPSOD_HEADING_TRUE";

    private SharedPreferences _prefs;
    private Context _context;
    private String _displayFmt;
    private GPSOffsetInterface _gpsoi;
    private double _lat, _lon;
    private CheckBox _feetCb, _metersCb, _angTrueCB, _angMagCb;
    private boolean _angleMag = false;
    private EditText _offsetDist, _offsetAng;

    public GPSAngle_OffsetDialog() {

    }

    public void Initialize(GPSOffsetInterface gpsoi, double lat, double lon,
            Context activity) {
        _gpsoi = gpsoi;
        _lat = lat;
        _lon = lon;
        _context = activity;

    }

    public void show() {
        Context pluginContext = ATSKApplication
                .getInstance().getPluginContext();

        View view = LayoutInflater.from(pluginContext).inflate(
                R.layout.gpsanglerangedialog, null);

        _prefs = PreferenceManager.getDefaultSharedPreferences(_context);
        double SavedAngle = _prefs.getFloat(GPSOD_ANGLE, 0);
        double SavedRange = _prefs.getFloat(GPSOD_RANGE, 10);
        boolean SavedUnitsMeters = _prefs.getBoolean(GPSOD_UNITS_M, false);

        _angleMag = _prefs.getBoolean(GPSOD_HEADING_TRUE, false);

        if (!SavedUnitsMeters)
            _displayFmt = ATSKConstants.UNITS_FEET;
        else
            _displayFmt = ATSKConstants.UNITS_METERS;

        _angTrueCB = (CheckBox) view.findViewById(R.id.angle_true);
        //    AngleTrueCheckbox.setButtonDrawable(R.drawable.checkbox_selector_blue);
        _angTrueCB
                .setOnCheckedChangeListener(new OnCheckedChangeListener() {
                    public void onCheckedChanged(CompoundButton button,
                            boolean checked) {
                        if (checked) {
                            _angleMag = false;
                            _angMagCb.setChecked(false);
                        }
                    }
                });
        _angMagCb = (CheckBox) view.findViewById(R.id.angle_mag);
        //    AngleMagCheckbox.setButtonDrawable(R.drawable.checkbox_selector_blue);
        _angMagCb
                .setOnCheckedChangeListener(new OnCheckedChangeListener() {
                    public void onCheckedChanged(CompoundButton button,
                            boolean checked) {
                        if (checked) {
                            _angleMag = true;
                            _angTrueCB.setChecked(false);
                        }
                    }
                });

        if (!_angleMag)
            _angTrueCB.setChecked(true);
        else
            _angMagCb.setChecked(true);

        _feetCb = (CheckBox) view.findViewById(R.id.units_feet);
        //    FeetCheck.setButtonDrawable(R.drawable.checkbox_selector_blue);
        _feetCb.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton button, boolean checked) {
                if (checked) {
                    String OldDisplayFormat = _displayFmt;
                    _displayFmt = ATSKConstants.UNITS_FEET;

                    _metersCb.setChecked(false);
                    if (!OldDisplayFormat.equals(_displayFmt))
                        UpdateUnits();
                }
            }
        });
        _metersCb = (CheckBox) view.findViewById(R.id.units_m);
        //        MetersCheck.setButtonDrawable(R.drawable.checkbox_selector_blue);
        _metersCb.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton button, boolean checked) {
                if (checked) {

                    _feetCb.setChecked(false);
                    String OldDisplayFormat = _displayFmt;
                    _displayFmt = ATSKConstants.UNITS_METERS;
                    if (!OldDisplayFormat.equals(_displayFmt))
                        UpdateUnits();

                }
            }
        });
        if (SavedUnitsMeters)
            _metersCb.setChecked(true);
        else
            _feetCb.setChecked(true);

        _offsetDist = (EditText) view.findViewById(R.id.range_offset);
        _offsetDist.setText(String.format("%.1f", SavedRange));
        _offsetDist.setBackgroundColor(Color.rgb(38, 38, 38));

        _offsetDist.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);

        _offsetAng = (EditText) view.findViewById(R.id.angle_offset);
        _offsetAng.setText(String.format("%.1f", SavedAngle));
        _offsetAng.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);

        AlertDialog ConfirmDialog;
        final AlertDialog.Builder ad = new AlertDialog.Builder(_context);
        ad.setTitle("GPS OFFSET");
        ad.setView(view);

        UpdateUnits();

        ad.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int arg1) {
                dialog.dismiss();
            }
        });

        ad.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String OffsetString = _offsetDist.getText().toString();
                String OffsetAngleString = _offsetAng.getText().toString();

                if (OffsetString.length() < 1)
                    OffsetString = "0";
                double Offset_m = 0, AngleOffset_t = 0, RawOffset = 0, RawAngle = 0;

                try {
                    RawOffset = Offset_m = Float.parseFloat(OffsetString);
                    RawAngle = AngleOffset_t = Float
                            .parseFloat(OffsetAngleString);
                } catch (NumberFormatException ex) {
                }

                if (_displayFmt.equals(ATSKConstants.UNITS_FEET))
                    Offset_m /= Conversions.M2F;

                if (_angleMag)
                    AngleOffset_t = Conversions.GetTrueAngle(
                            AngleOffset_t, _lat, _lon);

                SharedPreferences.Editor editor = _prefs.edit();
                editor.putFloat(GPSOD_ANGLE, (float) RawAngle);
                editor.putFloat(GPSOD_RANGE, (float) RawOffset);

                boolean UnitsM = !_displayFmt.equals(ATSKConstants.UNITS_FEET);

                editor.putBoolean(GPSOD_UNITS_M, UnitsM);
                editor.putBoolean(GPSOD_HEADING_TRUE, _angleMag);
                editor.apply();

                _gpsoi.RangeDirectionSelected(Offset_m, AngleOffset_t, false);//LOU come back here
                dialog.dismiss();

            }
        });

        ad.create().show();

    }

    private void UpdateUnits() {
        boolean ft = _displayFmt.equals(ATSKConstants.UNITS_FEET);
        _feetCb.setChecked(ft);
        _metersCb.setChecked(!ft);
    }

    public interface GPSOffsetInterface {
        void RangeDirectionSelected(double Range_m, double Angle_true,
                boolean TopCollected);

    }
}
