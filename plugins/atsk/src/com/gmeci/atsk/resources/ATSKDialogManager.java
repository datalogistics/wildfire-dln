
package com.gmeci.atsk.resources;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.atakmap.android.atsk.plugin.R;
import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.coremap.log.Log;
import com.gmeci.core.ATSKConstants;
import com.gmeci.core.SurveyPoint;
import com.gmeci.conversions.Conversions;

public class ATSKDialogManager {

    @SuppressWarnings("unused")
    private static final String TAG = "ATSKDialogManager";
    private final Context _context, _plugin;
    private final DialogUpdateInterface _dui;
    private final SharedPreferences _prefs;
    private final boolean StandardUnitFeet;
    private boolean DisplayUnitsStandard = false;
    private boolean DisplayAnglesTrue = true;
    private int UnitsIndex = 1;
    private AlertDialog MeasurementAD;
    private AlertDialog StringAD;
    private int AngleSelection = 0;
    private AlertDialog AngleAD;

    /**
     * Create a new dialog manager
     * @param appContext The main application context (ATAK context)
     * @param dui The callback interface
     * @param standardUnits True to use feet, false to use yards
     */
    public ATSKDialogManager(final Context appContext,
            final DialogUpdateInterface dui,
            final boolean standardUnits) {
        _context = appContext;
        _plugin = ATSKApplication.getInstance().getPluginContext();
        _dui = dui;
        StandardUnitFeet = standardUnits;
        _prefs = PreferenceManager
                .getDefaultSharedPreferences(appContext);

        // Measurement units
        String unitPreference = _prefs.getString(
                ATSKConstants.UNITS_DISPLAY, ATSKConstants.UNITS_FEET);
        DisplayUnitsStandard = unitPreference
                .equalsIgnoreCase(ATSKConstants.UNITS_FEET);

        // Angle units
        unitPreference = _prefs.getString(ATSKConstants.UNITS_ANGLE,
                ATSKConstants.UNITS_ANGLE_MAG);
        DisplayAnglesTrue = unitPreference
                .equalsIgnoreCase(ATSKConstants.UNITS_ANGLE_TRUE);
    }

    public ATSKDialogManager(final Context appContext,
            final DialogUpdateInterface dui) {
        this(appContext, dui, true);
    }

    public static void ShowConfirmDialog(final Context context,
            final String Title,
            final String Detail, final String Type,
            final ConfirmInterface parentInterface) {

        AlertDialog ConfirmDialog;
        final AlertDialog.Builder ad = new AlertDialog.Builder(context);
        ad.setTitle(Title);
        ad.setMessage(Detail);

        ad.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int arg1) {
                parentInterface.ConfirmResponse(Type, false);
                dialog.dismiss();
            }
        });
        ad.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

                parentInterface.ConfirmResponse(Type, true);
                dialog.dismiss();
            }
        });

        ConfirmDialog = ad.create();
        ConfirmDialog.show();
        ConfirmDialog.setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                parentInterface.ConfirmResponse(Type, false);
            }
        });
    }

    private void closeKeyboard(EditText input) {
        InputMethodManager imm = (InputMethodManager) _context
                .getSystemService(Activity.INPUT_METHOD_SERVICE);

        if (imm.isAcceptingText()) {
            imm.toggleSoftInput(InputMethodManager.HIDE_NOT_ALWAYS, 0);
            input.setInputType(0);
            imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
        }

    }

    public void ShowMeasurementHandJamDialog(double currentHeight_m,
            String DialogTitle, final int index) {
        final AlertDialog.Builder ad = new AlertDialog.Builder(_context);

        ad.setTitle(DialogTitle);

        // User input shouldn't contain 9999999
        if (currentHeight_m >= SurveyPoint.Altitude.INVALID)
            currentHeight_m = 0.0;

        String MeasurementString = format(currentHeight_m);

        CharSequence[] UnitsArray = {
                "Yards", "Meters"
        };

        if (StandardUnitFeet)
            UnitsArray[0] = "Feet";

        if (DisplayUnitsStandard) {
            UnitsIndex = 0;
            if (StandardUnitFeet) {
                MeasurementString = format(currentHeight_m * Conversions.M2F);
            } else {
                //must be yards
                MeasurementString = format(currentHeight_m * Conversions.M2F
                        / 3);
            }
        }

        final EditText input = new EditText(_context);
        input.setText(MeasurementString);
        input.setInputType(InputType.TYPE_CLASS_NUMBER
                | InputType.TYPE_NUMBER_FLAG_DECIMAL
                | InputType.TYPE_NUMBER_FLAG_SIGNED);
        input.setSelectAllOnFocus(true);
        input.setOnKeyListener(new OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER))) {
                    MeasurementOKPressed(input, index, MeasurementAD);
                    return true;
                }
                return false;
            }
        });

        input.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);

        ad.setView(input);
        ad.setSingleChoiceItems(UnitsArray, UnitsIndex,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (UnitsIndex != which) {
                            String MeasurementString;
                            String RecordedValueString = input.getText()
                                    .toString();
                            double CurrentMeasurementValue;
                            try {
                                CurrentMeasurementValue = Float
                                        .parseFloat(RecordedValueString);
                            } catch (NumberFormatException ex) {
                                CurrentMeasurementValue = 0;
                            }

                            if (which == 0) {//standard units from meters
                                if (StandardUnitFeet) {
                                    MeasurementString = format(
                                            CurrentMeasurementValue
                                                    * Conversions.M2F);
                                } else {
                                    MeasurementString = format(
                                            CurrentMeasurementValue
                                                    * Conversions.M2F / 3.0);
                                }
                            } else {//metric from standard units
                                if (StandardUnitFeet) {//was feet making it meters?
                                    MeasurementString = format(
                                            CurrentMeasurementValue
                                                    / Conversions.M2F);
                                } else {
                                    MeasurementString = format(
                                            CurrentMeasurementValue * 3
                                                    / Conversions.M2F);
                                }
                            }
                            input.setText(MeasurementString);
                        }//units changed
                        UnitsIndex = which;
                    }
                });

        ad.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int arg1) {
                dialog.dismiss();
            }
        });
        ad.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

                MeasurementOKPressed(input, index, dialog);
            }
        });

        MeasurementAD = ad.create();
        MeasurementAD.show();
        MeasurementAD.setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                closeKeyboard(input);
            }
        });
    }

    private void MeasurementOKPressed(final EditText input,
            final int index, DialogInterface dialog) {
        DisplayUnitsStandard = UnitsIndex == 0;

        String RecordedValueString = input.getText().toString();
        double RecordedValue;
        try {
            RecordedValue = Float.parseFloat(RecordedValueString);
        } catch (NumberFormatException ex) {
            RecordedValue = 0;
        }

        if (DisplayUnitsStandard) {
            _prefs.edit().putString(ATSKConstants.UNITS_DISPLAY,
                    ATSKConstants.UNITS_FEET).apply();
            RecordedValue /= Conversions.M2F;
            if (!StandardUnitFeet)//must be yards
                RecordedValue *= 3;//multiply by feet/yard

        } else
            _prefs.edit().putString(ATSKConstants.UNITS_DISPLAY,
                    ATSKConstants.UNITS_METERS).apply();

        if (_dui != null) {
            _dui.UpdateDimensionUnits(DisplayUnitsStandard);
            _dui.UpdateMeasurement(index, RecordedValue);
        }
        if (dialog != null)
            dialog.dismiss();
    }

    public void ShowTextHandJamDialog(String text, String DialogTitle,
            final int index) {
        final AlertDialog.Builder ad = new AlertDialog.Builder(_context);
        ad.setTitle(DialogTitle);

        final EditText input = new EditText(_context);
        input.setTextColor(Color.WHITE);
        input.setBackgroundColor(Color.BLACK);
        input.setText(text);
        input.setSelectAllOnFocus(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);

        input.setOnKeyListener(new OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER))) {
                    TextStringConfirmed(input, index, StringAD);
                    return true;
                }
                return false;
            }

        });

        input.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId,
                    KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    TextStringConfirmed(input, index, StringAD);
                    return true;
                }
                return false;
            }
        });

        ad.setView(input);

        ad.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int arg1) {
                dialog.dismiss();
            }
        });
        ad.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

                String RecordedValueString = input.getText().toString();
                if (_dui != null)
                    _dui.UpdateStringValue(index, RecordedValueString);
                //UpdateTextValues(RecordedValueString);
                dialog.dismiss();
                //MIKE - this is where we store the string
            }
        });

        StringAD = ad.create();
        StringAD.show();

        StringAD.setOnDismissListener(new OnDismissListener() {

            @Override
            public void onDismiss(DialogInterface dialog) {
                closeKeyboard(input);
            }

        });
    }

    protected void TextStringConfirmed(EditText input, int index,
            AlertDialog stringAD2) {
        String RecordedValueString = input.getText().toString();
        if (_dui != null)
            _dui.UpdateStringValue(index, RecordedValueString);

        stringAD2.dismiss();

    }

    public void ShowAngleHandJamDialog2(double currentAngle_True,
            final int index,
            final SurveyPoint anchorPoint, final double LZAngle_t) {

        LinearLayout layout = new LinearLayout(_context);
        LinearLayout.LayoutParams parms = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setLayoutParams(parms);

        layout.setGravity(Gravity.CLIP_VERTICAL);
        layout.setPadding(2, 2, 2, 2);

        final AlertDialog.Builder ad = new AlertDialog.Builder(_context);

        String TitleString = _plugin.getString(R.string.angle_magvar,
                Conversions.GetMagAngle(0, anchorPoint.lat, anchorPoint.lon));
        ad.setTitle(TitleString);

        if (!DisplayAnglesTrue)
            AngleSelection = 1;
        else
            AngleSelection = 0;
        CharSequence[] UnitsArray = {
                "TRUE", "MAG"
        };
        final EditText input = new EditText(_context);
        input.setText(GetMeasurementString(currentAngle_True,
                DisplayAnglesTrue, anchorPoint));
        input.setSelectAllOnFocus(true);
        input.setInputType(InputType.TYPE_CLASS_NUMBER
                | InputType.TYPE_NUMBER_FLAG_DECIMAL
                | InputType.TYPE_NUMBER_FLAG_SIGNED);
        input.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        input.setSelectAllOnFocus(true);

        final Button MatchLZButton = new Button(_context);
        MatchLZButton.setText("MATCH LZ");
        MatchLZButton.setTextColor(Color.WHITE);
        MatchLZButton.setInputType(InputType.TYPE_CLASS_NUMBER
                | InputType.TYPE_NUMBER_FLAG_DECIMAL
                | InputType.TYPE_NUMBER_FLAG_SIGNED);
        MatchLZButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                String MeasurementString = format(LZAngle_t);

                if (!DisplayAnglesTrue) {
                    AngleSelection = 1;
                    MeasurementString = format(Conversions
                            .GetMagAngle(LZAngle_t, anchorPoint.lat,
                                    anchorPoint.lon));
                }
                input.setText(MeasurementString);
            }

        });

        input.setOnKeyListener(new OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER))) {
                    AngleOKPressed(input, index, AngleAD, anchorPoint);
                    return true;
                }
                return false;
            }
        });

        input.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId,
                    KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    AngleOKPressed(input, index, AngleAD, anchorPoint);
                    return true;
                }
                return false;
            }
        });

        LinearLayout.LayoutParams tv1Params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        layout.addView(input, tv1Params);
        layout.addView(MatchLZButton, tv1Params);

        ad.setView(layout);
        ad.setSingleChoiceItems(UnitsArray, AngleSelection,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (AngleSelection == which)
                            return;
                        AngleSelection = which;
                        try {
                            AngleSelection = which;
                            String CurrentAngleString = input.getText()
                                    .toString();
                            double CurrentAngle = Float
                                    .parseFloat(CurrentAngleString);
                            double NewAngle;
                            if (which == 0) {//turn to true
                                DisplayAnglesTrue = true;
                                NewAngle = Conversions.GetTrueAngle(
                                        CurrentAngle, anchorPoint.lat,
                                        anchorPoint.lon);
                            } else {//turn to mag
                                DisplayAnglesTrue = false;
                                NewAngle = Conversions.GetMagAngle(
                                        CurrentAngle, anchorPoint.lat,
                                        anchorPoint.lon);
                            }
                            String MeasurementString = format(NewAngle);

                            input.setText(MeasurementString);
                        } catch (Exception e) {
                            Log.w(TAG, "Failed to convert angle.");
                        }
                    }
                });

        ad.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int arg1) {
                dialog.dismiss();
            }
        });
        ad.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                AngleOKPressed(input, index, dialog, anchorPoint);
            }
        });

        AngleAD = ad.create();
        AngleAD.show();

        AngleAD.setOnDismissListener(new OnDismissListener() {

            @Override
            public void onDismiss(DialogInterface dialog) {
                closeKeyboard(input);
            }

        });

    }

    String GetMeasurementString(double currentAngle_True,
            boolean DisplayAnglesTrue, SurveyPoint anchorPoint) {
        currentAngle_True = Conversions.deg360(currentAngle_True);
        String MeasurementString = format(currentAngle_True);
        if (!DisplayAnglesTrue) {
            double MagAngle = Conversions.GetMagAngle(currentAngle_True,
                    anchorPoint.lat, anchorPoint.lon);
            MeasurementString = format(MagAngle);
        }
        AngleSelection = 1;
        return MeasurementString;
    }

    public void ShowAngleHandJamDialog(double currentAngle_True,
            final SurveyPoint anchorPoint, final int index) {
        final AlertDialog.Builder ad = new AlertDialog.Builder(_context);
        String TitleString = _plugin.getString(R.string.angle_magvar,
                Conversions.GetMagAngle(0, anchorPoint.lat, anchorPoint.lon));
        ad.setTitle(TitleString);

        CharSequence[] UnitsArray = {
                "TRUE", "MAG"
        };
        String MeasurementString = GetMeasurementString(currentAngle_True,
                DisplayAnglesTrue, anchorPoint);

        if (!DisplayAnglesTrue)
            AngleSelection = 1;
        else
            AngleSelection = 0;

        final EditText input = new EditText(_context);
        input.setText(MeasurementString);
        input.setSelectAllOnFocus(true);
        input.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        input.setInputType(InputType.TYPE_CLASS_NUMBER
                | InputType.TYPE_NUMBER_FLAG_DECIMAL
                | InputType.TYPE_NUMBER_FLAG_SIGNED);

        input.setOnKeyListener(new OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER))) {
                    AngleOKPressed(input, index, AngleAD, anchorPoint);
                    return true;
                }
                return false;
            }
        });

        input.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId,
                    KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    AngleOKPressed(input, index, AngleAD, anchorPoint);
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
                        if (AngleSelection == which)
                            return;
                        try {
                            AngleSelection = which;
                            String CurrentAngleString = input.getText()
                                    .toString();
                            double CurrentAngle = Float
                                    .parseFloat(CurrentAngleString);
                            double NewAngle;
                            if (which == 0) {//turn to true
                                DisplayAnglesTrue = true;
                                NewAngle = Conversions.GetTrueAngle(
                                        CurrentAngle, anchorPoint.lat,
                                        anchorPoint.lon);
                            } else {//turn to mag
                                DisplayAnglesTrue = false;
                                NewAngle = Conversions.GetMagAngle(
                                        CurrentAngle, anchorPoint.lat,
                                        anchorPoint.lon);
                            }
                            String MeasurementString = format(NewAngle);

                            input.setText(MeasurementString);
                        } catch (Exception e) {
                            Log.w(TAG, "Failed to convert angle.");
                        }
                    }
                });

        ad.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int arg1) {
                dialog.dismiss();
            }
        });
        ad.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                AngleOKPressed(input, index, dialog, anchorPoint);
            }
        });

        AngleAD = ad.create();
        AngleAD.show();

        AngleAD.setOnDismissListener(new OnDismissListener() {

            @Override
            public void onDismiss(DialogInterface dialog) {
                closeKeyboard(input);
            }

        });

    }

    public void ShowGSRAngleHandJamDialog(final double currentAngle,
            final int index, boolean ShowGSR) {
        final AlertDialog.Builder ad = new AlertDialog.Builder(_context);
        ad.setTitle("Glide Slope Ratio - Angle");

        AngleSelection = 0;
        String MeasurementString = String.format(LocaleUtil.getCurrent(),
                "%.2f", currentAngle);
        if (ShowGSR) {
            MeasurementString = Conversions
                    .ConvertGlideSlopeAngleToRatio(currentAngle);
            AngleSelection = 1;
        }

        CharSequence[] UnitsArray = {
                "ANGLE", "GSR"
        };

        final EditText input = new EditText(_context);
        input.setText(MeasurementString);
        input.setSelectAllOnFocus(true);
        input.setInputType(InputType.TYPE_CLASS_NUMBER
                | InputType.TYPE_NUMBER_FLAG_DECIMAL
                | InputType.TYPE_NUMBER_FLAG_SIGNED);

        input.setOnKeyListener(new OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER))) {
                    GSRAngleOKPressed(input, index, AngleAD,
                            AngleSelection == 1);
                    return true;
                }
                return false;
            }
        });

        input.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId,
                    KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    GSRAngleOKPressed(input, index, AngleAD,
                            AngleSelection == 1);
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

                        if (AngleSelection == which)
                            return;
                        AngleSelection = which;
                        if (AngleSelection == 0) {
                            //read the GSR and convert to angle

                            //angle
                            String MeasurementString = format(currentAngle);
                            input.setText(MeasurementString);
                        } else {
                            //read the angle and convert to gsr
                            String MeasurementString = Conversions
                                    .ConvertGlideSlopeAngleToRatio(currentAngle);
                            input.setText(MeasurementString);
                        }
                    }
                });

        ad.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int arg1) {
                dialog.dismiss();
            }
        });
        ad.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                GSRAngleOKPressed(input, index, dialog, AngleSelection == 1);
            }
        });

        AngleAD = ad.create();
        AngleAD.show();

        AngleAD.setOnDismissListener(new OnDismissListener() {

            @Override
            public void onDismiss(DialogInterface dialog) {
                closeKeyboard(input);
            }

        });

    }

    private void AngleOKPressed(final EditText input, int index,
            DialogInterface dialog, SurveyPoint anchorPoint) {

        if (dialog != null)
            dialog.dismiss();

        DisplayAnglesTrue = AngleSelection == 0;

        String RecordedValueString = input.getText().toString();
        RecordedValueString = RecordedValueString.replace("1:", "");
        double RecordedValue;
        try {
            RecordedValue = Float.parseFloat(RecordedValueString);
        } catch (NumberFormatException ex) {
            RecordedValue = 0.0f;
        }

        if (DisplayAnglesTrue) {
            _prefs.edit().putString(ATSKConstants.UNITS_ANGLE,
                    ATSKConstants.UNITS_ANGLE_TRUE).apply();
        } else {
            _prefs.edit().putString(ATSKConstants.UNITS_ANGLE,
                    ATSKConstants.UNITS_ANGLE_MAG).apply();
            RecordedValue = Conversions.GetTrueAngle(RecordedValue,
                    anchorPoint.lat, anchorPoint.lon);
        }

        if (_dui != null) {
            _dui.UpdateAngleUnits(DisplayAnglesTrue);
            _dui.UpdateMeasurement(index, Conversions.deg360(RecordedValue));
        }
    }

    private void GSRAngleOKPressed(final EditText input, final int index,
            DialogInterface dialog, boolean GSR) {

        if (dialog != null)
            dialog.dismiss();

        String RecordedValueString = input.getText().toString();
        RecordedValueString = RecordedValueString.replace("1:", "");

        double RecordedValue;
        try {

            RecordedValue = Float.parseFloat(RecordedValueString);
            if (GSR) {
                RecordedValue = Math.atan(1f / RecordedValue)
                        * Conversions.RAD2DEG;
            }

        } catch (NumberFormatException ex) {
            RecordedValue = 0.0f;
        }

        _prefs.edit().putString(ATSKConstants.UNITS_GSR_ANGLE,
                GSR ? ATSKConstants.UNITS_GSR_ANGLE_GSR
                        : ATSKConstants.UNITS_GSR_ANGLE).apply();

        if (_dui != null) {
            _dui.UpdateGSRAngleUnits(GSR);
            _dui.UpdateMeasurement(index, RecordedValue);
        }
    }

    private String format(double value) {
        return String.format(LocaleUtil.getCurrent(), "%.1f", value);
    }

    public interface DialogUpdateInterface {
        void UpdateGSRAngleUnits(boolean GSR);

        void UpdateMeasurement(int index, double measurement);

        void UpdateStringValue(int index, String value);

        void UpdateAngleUnits(boolean usingTrue);

        void UpdateDimensionUnits(boolean usingFeet);
    }

    public interface ConfirmInterface {
        void ConfirmResponse(String Type, boolean Confirmed);
    }

}
