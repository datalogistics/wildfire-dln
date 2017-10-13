
package com.gmeci.atsk.az;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.atakmap.android.atsk.plugin.R;
import com.gmeci.atsk.resources.ATSKApplication;
import com.gmeci.core.ATSKConstants;
import com.gmeci.conversions.Conversions;

public class AngleHandJamDialog {

    private static final String TAG = "AngleHandJamDialog";
    private final Context _context;
    private final SharedPreferences _prefs;
    int AngleSelection = 0;
    AlertDialog AngleAD;
    private boolean _angTrue;

    private double _currentAngle;
    private double _lat, _lon;

    private AngleHandJamInterface _parent;

    public AngleHandJamDialog(Context context) {
        _context = context;
        _prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    private void closeKeyboard(EditText input) {
        InputMethodManager imm = (InputMethodManager) _context
                .getSystemService(Activity.INPUT_METHOD_SERVICE);

        imm.toggleSoftInput(InputMethodManager.HIDE_NOT_ALWAYS, 0);
        input.setInputType(0);
        imm.hideSoftInputFromWindow(input.getWindowToken(), 0);

    }

    public void setFormatByString(String currentFormat,
            AngleHandJamInterface parent) {
        _parent = parent;
        _angTrue = currentFormat
                .equals(ATSKConstants.UNITS_ANGLE_TRUE);
    }

    public void setFormatByFlag(boolean angTrue, AngleHandJamInterface parent) {
        _parent = parent;
        _angTrue = angTrue;
    }

    public void Show(final double inputAngle, final double lat, final double lon) {

        Context pluginContext = ATSKApplication
                .getInstance().getPluginContext();

        final AlertDialog.Builder ad = new AlertDialog.Builder(_context,
                android.R.style.Theme_Holo_Dialog);
        ad.setTitle("Angle");

        String MeasurementString = String.format("%.1f", inputAngle);
        if (_prefs.getString(ATSKConstants.UNITS_ANGLE, "").equals(
                ATSKConstants.UNITS_ANGLE_MAG)) {//We want to display MAG
            AngleSelection = 1;
            MeasurementString = String.format("%.1f",
                    Conversions.GetMagAngle(inputAngle, lat, lon));
        } else {//We want to display TRUE
            AngleSelection = 0;
        }
        CharSequence[] UnitsArray = {
                "TRUE", "MAG"
        };

        final EditText input = new EditText(pluginContext);
        input.setTextColor(ATSKConstants.LIGHT_BLUE);
        input.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        input.setText(MeasurementString);
        input.setBackgroundResource(R.drawable.fullborder_background);

        input.setInputType(InputType.TYPE_CLASS_NUMBER
                | InputType.TYPE_NUMBER_FLAG_DECIMAL
                | InputType.TYPE_NUMBER_FLAG_SIGNED);

        input.setOnKeyListener(new OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER))) {
                    AngleOKPressed(input, AngleAD);
                    return true;
                }
                return false;
            }
        });

        ad.setView(input);
        ad.setSingleChoiceItems(UnitsArray, AngleSelection,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AngleSelection = which;

                    }
                });

        ad.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int arg1) {
                dialog.dismiss();
            }
        });
        ad.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                AngleOKPressed(input, dialog);
            }

        });
        AngleAD = ad.create();
        AngleAD.show();
        AngleAD.setOnDismissListener(new OnDismissListener() {

            @Override
            public void onDismiss(DialogInterface dialog) {
                //closeKeyboard(input);
            }

        });
    }

    private void AngleOKPressed(EditText input, DialogInterface dialog) {
        if (dialog != null)
            dialog.dismiss();
        _angTrue = AngleSelection == 0;

        String RecordedValueString = input.getText().toString();
        _currentAngle = Conversions.deg360(Float
                .parseFloat(RecordedValueString));
        if (_angTrue) {
            _prefs.edit().putString(ATSKConstants.UNITS_ANGLE,
                    ATSKConstants.UNITS_ANGLE_TRUE).apply();
        } else {
            _prefs.edit().putString(ATSKConstants.UNITS_ANGLE,
                    ATSKConstants.UNITS_ANGLE_MAG).apply();
        }
        _parent.UpdateDisplayMeasurements(_currentAngle, _angTrue);
    }

    public double getCurrentAngle() {
        return _currentAngle;
    }

    public interface AngleHandJamInterface {
        void UpdateDisplayMeasurements(double currentAngle,
                boolean DisplayAngleTrue);
    }
}
