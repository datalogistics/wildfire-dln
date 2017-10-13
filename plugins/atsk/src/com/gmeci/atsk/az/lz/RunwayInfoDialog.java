
package com.gmeci.atsk.az.lz;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.TextView;

import com.atakmap.android.atsk.plugin.R;
import com.atakmap.coremap.log.Log;
import com.gmeci.atsk.az.AZBaseMeasurementFragment;
import com.gmeci.atsk.az.AZController;
import com.gmeci.atsk.resources.ATSKApplication;
import com.gmeci.core.ATSKConstants;
import com.gmeci.core.SurveyData;
import com.gmeci.core.SurveyPoint;
import com.gmeci.atsk.az.spinners.ATSKSpinner;
import com.gmeci.atsk.resources.ATSKDialogManager;
import com.gmeci.atsk.resources.ATSKDialogManager.DialogUpdateInterface;
import com.gmeci.atsk.resources.CoordinateHandJamDialog;
import com.gmeci.atsk.resources.CoordinateHandJamDialog.HandJamInterface;
import com.gmeci.atskservice.resolvers.AZProviderClient;
import com.gmeci.helpers.AZHelper;
import com.gmeci.conversions.Conversions;
import com.gmeci.conversions.Conversions.Unit;

import com.atakmap.coremap.locale.LocaleUtil;

public class RunwayInfoDialog extends DialogFragment implements
        HandJamInterface, DialogUpdateInterface {

    private static final String TAG = "RunwayInfoDialog";
    // primitives
    private static final int MAX_EDITABLE_TVS = 9;
    private static final int MAX_TVS = 9;
    private static final float SELECTED_SIZE_MULTIPLIER = 1.25f;
    // view constants
    private static final int USABLE_WIDTH = 0;
    private static final int RUNWAY_WIDTH = 1;
    private static final int SHOULDER_WIDTH = 2;
    private static final int USABLE_LENGTH = 3;
    private static final int RUNWAY_LENGTH = 4;
    private static final int APPROACH_OVERRUN_LENGTH = 5;
    private static final int DEPARTURE_OVERRUN_LENGTH = 6;
    private static final int RUNWAY_HEADING = 7;
    private static final int RUNWAY_NAME = 8;

    // listeners
    private static final int APPROACH_COORDINATES = 0;
    private static final int APPROACH_THRESHOLD = 1;
    private static final int APPROACH_GSR = 2;
    private static final int DEPARTURE_COORDINATES = 3;
    private static final int DEPARTURE_THRESHOLD = 4;
    private static final int DEPARTURE_GSR = 5;
    private static final int MAX_VIEW_BUTTONS = 6;
    // view components
    ATSKSpinner surfaceSpinner;
    TextView ApproachLengthSignTextView, DepartureLengthSignTextView;
    private int _clickedID = -1;
    private View _root;
    private Button _saveBtn, _cancelBtn;
    private final TextView[] textViewButtons = new TextView[MAX_VIEW_BUTTONS];
    private final TextView[] valueTV = new TextView[MAX_EDITABLE_TVS];
    private final TextView[] unitTV = new TextView[MAX_EDITABLE_TVS];
    private final TextView[] textViews = new TextView[MAX_TVS];
    // private fields;
    private AZProviderClient _azpc;
    private SurveyData _survey;
    private RunwayInfoParentInterface _parent;
    private SharedPreferences _prefs;
    private boolean _unitsFeet = false;
    private boolean _angleTrue = true;
    private int currentlySelectedTVIndex = -1;
    private String currentUID;
    private String currentCoordinateFormat = Conversions.COORD_FORMAT_DM;
    private final OnClickListener saveClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            Log.d(TAG, "On Save Click Listener");
            _azpc.UpdateAZ(_survey, "rwi", true);
            if (_parent != null)
                _parent.Update(true);

            RunwayInfoDialog.this.dismiss();
        }
    };
    private final OnClickListener cancelClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            Log.d(TAG, "In on Cancel Click Listener");
            RunwayInfoDialog.this.dismiss();
        }
    };
    private final OnClickListener measurementClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            Log.d(TAG, "In onMeasurementClickListener");
            _clickedID = v.getId();

            if (checkForIDMatch(_clickedID, USABLE_WIDTH)) {
                changeColorClickedTV(USABLE_WIDTH, true);
            } else if (checkForIDMatch(_clickedID, RUNWAY_WIDTH)) {
                changeColorClickedTV(RUNWAY_WIDTH, true);
            } else if (checkForIDMatch(_clickedID, RUNWAY_NAME)) {
                ATSKDialogManager adm = new ATSKDialogManager(getActivity(),
                        RunwayInfoDialog.this);
                adm.ShowTextHandJamDialog(_azpc.getAZName(currentUID),
                        "Runway Name", _clickedID);
            }
        }
    };
    private final OnLongClickListener measurementLongClickListener = new OnLongClickListener() {

        @Override
        public boolean onLongClick(View v) {
            Log.d(TAG, "onMeasurementLongCLickListener");
            currentlySelectedTVIndex = -1;
            _clickedID = v.getId();

            /*if (checkForIDMatch(clickedID, USABLE_WIDTH)) {
                currentlySelectedTVIndex = USABLE_WIDTH;
                ShowMeasurementHandJamDialog(survey.edges.LeftShoulderWidth_m
                        + survey.width, "Usable Width");
            }*/
            if (checkForIDMatch(_clickedID, RUNWAY_WIDTH)) {
                currentlySelectedTVIndex = RUNWAY_WIDTH;
                ShowMeasurementHandJamDialog(_survey.width, "Runway Width",
                        false);
            } else if (checkForIDMatch(_clickedID, SHOULDER_WIDTH)) {
                currentlySelectedTVIndex = SHOULDER_WIDTH;
                ShowMeasurementHandJamDialog(_survey.edges.ShoulderWidth_m,
                        "Runway Width", false);
            } else if (checkForIDMatch(_clickedID, RUNWAY_HEADING)) {
                currentlySelectedTVIndex = RUNWAY_HEADING;
                ShowAngleHandJamDialog(_survey.angle, "Runway Heading");
            }
            /*else if (checkForIDMatch(clickedID, USABLE_LENGTH)) {
                ShowMeasurementHandJamDialog(
                        survey.edges.ApproachOverrunLength_m + survey.Length_m,
                        "Usable Length");
                currentlySelectedTVIndex = USABLE_LENGTH;
            }*/
            else if (checkForIDMatch(_clickedID, RUNWAY_LENGTH)) {
                currentlySelectedTVIndex = RUNWAY_LENGTH;
                ShowMeasurementHandJamDialog(_survey.getLength(false),
                        "Runway Length", false);
            } else if (checkForIDMatch(_clickedID, APPROACH_OVERRUN_LENGTH)) {
                currentlySelectedTVIndex = APPROACH_OVERRUN_LENGTH;
                ShowMeasurementHandJamDialog(
                        _survey.edges.ApproachOverrunLength_m, "Overrun", false);
            } else if (checkForIDMatch(_clickedID, DEPARTURE_OVERRUN_LENGTH)) {
                currentlySelectedTVIndex = DEPARTURE_OVERRUN_LENGTH;
                ShowMeasurementHandJamDialog(
                        _survey.edges.ApproachOverrunLength_m, "Overrun", false);
            }

            return currentlySelectedTVIndex == -1;

        }

    };
    private final OnLongClickListener TVButtonLongClickListener = new OnLongClickListener() {

        @Override
        public boolean onLongClick(View v) {
            _clickedID = v.getId();
            if (checkForIDMatch(_clickedID, APPROACH_COORDINATES)) {
                SurveyPoint approach = AZHelper.CalculateAnchorFromAZCenter(
                        _survey, _survey.center, _survey.getApproachAnchor());
                CoordinateHandJamDialog chjd = new CoordinateHandJamDialog();
                chjd.Initialize(approach.lat, approach.lon,
                        currentCoordinateFormat, _survey.approachElevation,
                        RunwayInfoDialog.this);
                return true;
            } else if (checkForIDMatch(_clickedID, DEPARTURE_COORDINATES)) {
                SurveyPoint departure = AZHelper.CalculateAnchorFromAZCenter(
                        _survey, _survey.center, _survey.getDepartureAnchor());
                CoordinateHandJamDialog chjd = new CoordinateHandJamDialog();
                chjd.Initialize(departure.lat, departure.lon,
                        currentCoordinateFormat, _survey.departureElevation,
                        RunwayInfoDialog.this);
                return true;
            }
            return false;
        }
    };

    // constructor
    public RunwayInfoDialog() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        Context pluginContext = ATSKApplication
                .getInstance().getPluginContext();
        _root = LayoutInflater.from(pluginContext)
                .inflate(R.layout.lz_runway_info_dlg, container, false);
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getDialog().getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        int width = getResources().getDisplayMetrics().widthPixels;
        int height = getResources().getDisplayMetrics().heightPixels;
        _root.setMinimumWidth((int) (width * 0.90f));
        _root.setMinimumHeight((int) (height * 0.80f));

        _prefs = PreferenceManager
                .getDefaultSharedPreferences(getActivity());
        String anglePreference = _prefs.getString(
                ATSKConstants.UNITS_ANGLE, ATSKConstants.UNITS_ANGLE_MAG);
        _angleTrue = anglePreference
                .equalsIgnoreCase(ATSKConstants.UNITS_ANGLE_TRUE);

        setupButtons();
        setupTextViews();

        surfaceSpinner = (ATSKSpinner) _root
                .findViewById(R.id.runway_surface_spinner);
        surfaceSpinner.setPrompt("SURFACE TYPES");
        surfaceSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                    int position, long id) {
                _survey.surface = surfaceSpinner.getItem(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        int SurfacePosition = surfaceSpinner.GetPosition(_survey.surface);
        surfaceSpinner.setSelection(SurfacePosition);

        currentCoordinateFormat = _prefs.getString(
                ATSKConstants.COORD_FORMAT, Conversions.COORD_FORMAT_DM);
        String unitPreference = _prefs.getString(
                ATSKConstants.UNITS_DISPLAY, ATSKConstants.UNITS_FEET);

        _unitsFeet = unitPreference.equals(ATSKConstants.UNITS_FEET);

        UpdateMeasurements();

        return _root;
    }

    private void changeColorClickedTV(int textViewID, boolean setSelected) {
        if (setSelected) {
            valueTV[textViewID]
                    .setBackgroundResource(R.drawable.background_selected_center);
            unitTV[textViewID]
                    .setBackgroundResource(R.drawable.background_selected_right);
        }
    }

    public void setupDialog(String currentSurveyName, AZProviderClient azpc,
            RunwayInfoParentInterface parent) {
        Log.d(TAG, "Current Survey uid: " + currentSurveyName);
        currentUID = currentSurveyName;
        _parent = parent;
        _azpc = azpc;
        _survey = azpc.getAZ(currentUID, true);

    }

    private void setupTextViews() {

        //TextView titleTV = (TextView) _root
        //        .findViewById(R.id.lz_launch_runway_info_dlg);
        //TextView approachTV = (TextView) _root
        //        .findViewById(R.id.approach_textview);
        //TextView departureTV = (TextView) _root
        //        .findViewById(R.id.departure_textview);
        //TextView surfaceLabelTextView = (TextView) _root
        //        .findViewById(R.id.surface_type_label);
        ApproachLengthSignTextView = (TextView) _root
                .findViewById(R.id.atsk_ApproachLengthSignTextView);
        DepartureLengthSignTextView = (TextView) _root
                .findViewById(R.id.atsk_DepartureLengthSignTextView);

        textViews[USABLE_WIDTH] = (TextView) _root
                .findViewById(R.id.runway_info_usable_width_text);
        textViews[RUNWAY_WIDTH] = (TextView) _root
                .findViewById(R.id.runway_info_runway_width_text);
        textViews[SHOULDER_WIDTH] = (TextView) _root
                .findViewById(R.id.runway_info_shoulder_width_text);
        textViews[USABLE_LENGTH] = (TextView) _root
                .findViewById(R.id.runway_info_usable_length_text);
        textViews[RUNWAY_LENGTH] = (TextView) _root
                .findViewById(R.id.runway_info_runway_length_text);
        textViews[APPROACH_OVERRUN_LENGTH] = (TextView) _root
                .findViewById(R.id.approach_runway_info_overrun_length_text);
        textViews[DEPARTURE_OVERRUN_LENGTH] = (TextView) _root
                .findViewById(R.id.departure_runway_info_overrun_length_text);

        textViews[RUNWAY_HEADING] = (TextView) _root
                .findViewById(R.id.runway_info_runway_heading_label);
        textViews[RUNWAY_NAME] = (TextView) _root
                .findViewById(R.id.runway_info_runway_name_label);

        valueTV[USABLE_WIDTH] = (TextView) _root
                .findViewById(R.id.runway_info_usable_width_value);
        valueTV[RUNWAY_WIDTH] = (TextView) _root
                .findViewById(R.id.runway_info_runway_width_value);
        valueTV[SHOULDER_WIDTH] = (TextView) _root
                .findViewById(R.id.runway_info_shoulder_width_value);
        valueTV[USABLE_LENGTH] = (TextView) _root
                .findViewById(R.id.runway_info_usable_length_value);
        valueTV[RUNWAY_LENGTH] = (TextView) _root
                .findViewById(R.id.runway_info_runway_length_value);

        valueTV[APPROACH_OVERRUN_LENGTH] = (TextView) _root
                .findViewById(R.id.approach_runway_info_overrun_length_value);
        valueTV[DEPARTURE_OVERRUN_LENGTH] = (TextView) _root
                .findViewById(R.id.departure_runway_info_overrun_length_value);
        valueTV[RUNWAY_HEADING] = (TextView) _root
                .findViewById(R.id.runway_info_runway_heading_value);
        valueTV[RUNWAY_NAME] = (TextView) _root
                .findViewById(R.id.runway_info_runway_name_value);

        unitTV[USABLE_WIDTH] = (TextView) _root
                .findViewById(R.id.runway_info_usable_width_units);
        unitTV[RUNWAY_WIDTH] = (TextView) _root
                .findViewById(R.id.runway_info_runway_width_units);
        unitTV[SHOULDER_WIDTH] = (TextView) _root
                .findViewById(R.id.runway_info_shoulder_width_units);
        unitTV[USABLE_LENGTH] = (TextView) _root
                .findViewById(R.id.runway_info_usable_length_units);
        unitTV[RUNWAY_LENGTH] = (TextView) _root
                .findViewById(R.id.runway_info_runway_length_units);
        unitTV[DEPARTURE_OVERRUN_LENGTH] = (TextView) _root
                .findViewById(R.id.departure_runway_info_overrun_length_units);
        unitTV[APPROACH_OVERRUN_LENGTH] = (TextView) _root
                .findViewById(R.id.approach_runway_info_overrun_length_units);
        unitTV[RUNWAY_HEADING] = (TextView) _root
                .findViewById(R.id.runway_info_runway_heading_units);

        for (TextView t : valueTV) {
            t.setClickable(true);
            t.setLongClickable(true);
            t.setOnClickListener(measurementClickListener);
            t.setOnLongClickListener(measurementLongClickListener);
        }

        for (TextView t : unitTV) {
            if (t == null)
                break;

            t.setClickable(true);
            t.setLongClickable(true);
            t.setOnClickListener(measurementClickListener);
            t.setOnLongClickListener(measurementLongClickListener);
        }

        textViewButtons[APPROACH_COORDINATES] = (TextView) _root
                .findViewById(R.id.runway_info_approach_coordinate);
        textViewButtons[APPROACH_THRESHOLD] = (TextView) _root
                .findViewById(R.id.runway_info_approach_threshold);
        textViewButtons[APPROACH_GSR] = (TextView) _root
                .findViewById(R.id.runway_info_approach_gsr);
        textViewButtons[DEPARTURE_COORDINATES] = (TextView) _root
                .findViewById(R.id.runway_info_departure_coordinate);
        textViewButtons[DEPARTURE_THRESHOLD] = (TextView) _root
                .findViewById(R.id.runway_info_departure_threshold);
        textViewButtons[DEPARTURE_GSR] = (TextView) _root
                .findViewById(R.id.runway_info_departure_gsr);

        UpdateCoordinateDisplay();

        for (TextView t : textViewButtons)
            t.setOnLongClickListener(TVButtonLongClickListener);
    }

    private void setupButtons() {
        _saveBtn = (Button) _root.findViewById(R.id.runway_info_save_button);
        _cancelBtn = (Button) _root
                .findViewById(R.id.runway_info_cancel_button);

        _saveBtn.setOnClickListener(saveClickListener);
        _cancelBtn.setOnClickListener(cancelClickListener);
    }

    private void UpdateMeasurements() {

        UpdateUnitsDisplay();
        UpdateCoordinateDisplay();

        Unit displayUnit = (_unitsFeet ? Unit.FOOT : Unit.METER);
        double angle = Conversions.deg360(_survey.angle);
        if (!_angleTrue)
            angle = Conversions.GetMagAngle(_survey.angle,
                    _survey.center.lat, _survey.center.lon);

        valueTV[RUNWAY_WIDTH].setText(Unit.METER.format(
                _survey.width, displayUnit));
        valueTV[SHOULDER_WIDTH].setText(Unit.METER.format(
                _survey.edges.ShoulderWidth_m, displayUnit));
        valueTV[RUNWAY_LENGTH].setText(Unit.METER.format(
                _survey.getLength(false), displayUnit));
        valueTV[RUNWAY_HEADING].setText(String.format("%.1f", angle));
        valueTV[RUNWAY_NAME].setText(_survey.getSurveyName());
        UpdateUsableMeasurements();
    }

    private void UpdateUnitsDisplay() {
        String unit = _unitsFeet ? " ft" : " m";
        String angle_unit = _angleTrue ? " true" : " mag";

        for (int i = 0; i <= DEPARTURE_OVERRUN_LENGTH; i++) {
            if (unitTV[i] != null)
                unitTV[i].setText(unit);
        }

        unitTV[RUNWAY_HEADING].setText(angle_unit);
    }

    private void UpdateUsableMeasurements() {
        Unit displayUnit = (_unitsFeet ? Unit.FOOT : Unit.METER);
        double lenOffset = _survey.edges.ApproachOverrunLength_m;
        double appLenOffset = _survey.edges.ApproachOverrunLength_m;
        ApproachLengthSignTextView.setText("+");

        lenOffset += _survey.edges.DepartureOverrunLength_m;
        double depLenOffset = _survey.edges.DepartureOverrunLength_m;
        DepartureLengthSignTextView.setText("+");

        valueTV[USABLE_LENGTH].setText(Unit.METER.format(
                _survey.getLength(false) + lenOffset, displayUnit));
        valueTV[USABLE_WIDTH]
                .setText(Unit.METER.format(
                        _survey.edges.ShoulderWidth_m * 2 + _survey.width,
                        displayUnit));
        valueTV[APPROACH_OVERRUN_LENGTH].setText(
                Unit.METER.format(appLenOffset, displayUnit));
        valueTV[DEPARTURE_OVERRUN_LENGTH].setText(Unit.METER.format(
                depLenOffset, displayUnit));
    }

    private void ShowMeasurementHandJamDialog(double measurement, String name,
            boolean isElevation) {
        ATSKDialogManager adm = new ATSKDialogManager(getActivity(), this);
        adm.ShowMeasurementHandJamDialog(measurement, name, _clickedID);
    }

    private void ShowAngleHandJamDialog(double CurrentAngle, String name) {
        ATSKDialogManager adm = new ATSKDialogManager(getActivity(), this);
        adm.ShowAngleHandJamDialog(CurrentAngle, _survey.center, _clickedID);
    }

    private boolean checkForIDMatch(int clickedID, int tvIndex) {
        boolean match = false;
        if (tvIndex < textViewButtons.length) {
            if (clickedID == textViewButtons[tvIndex].getId())
                match = true;
        }
        if (tvIndex < valueTV.length && tvIndex < unitTV.length) {
            if (unitTV[tvIndex] != null && clickedID == unitTV[tvIndex].getId())
                match = true;
            else if (valueTV[tvIndex] != null
                    && clickedID == valueTV[tvIndex].getId())
                match = true;
        }

        return match;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.gmeci.atskresources.CoordinateHandJamDialog.HandJamInterface#
     * UpdateCoordinate(double, double)
     */
    @Override
    public void UpdateCoordinate(double Lat, double Lon, double elevation) {
        SurveyPoint sp = new SurveyPoint(Lat, Lon);
        sp.setHAE(elevation);
        if (checkForIDMatch(_clickedID, APPROACH_COORDINATES)) {
            /*//recalculate everythign from this approach point
            SurveyPoint departure = AZHelper
                    .CalculateCenterOfEdge(_survey, true);
            //need a new center, length, and angle
            UpdateLZCLA(Lat, Lon, elevation, departure.lat, departure.lon,
                    departure.getHAE());*/
            AZBaseMeasurementFragment.recalculateWithApproach(
                    _survey, sp);

        } else if (checkForIDMatch(_clickedID, DEPARTURE_COORDINATES)) {
            /*SurveyPoint approach = AZHelper
                    .CalculateCenterOfEdge(_survey, false);
            UpdateLZCLA(approach.lat, approach.lon, approach.getHAE(), Lat,
                    Lon, elevation);*/
            AZBaseMeasurementFragment.recalculateWithDeparture(
                    _survey, sp);
        }
        UpdateMeasurements();
    }

    private void UpdateLZCLA(double start_lat, double start_lon,
            double start_alt,
            double end_lat, double end_lon, double end_alt) {
        double RA[] = Conversions.CalculateRangeAngle(start_lat, start_lon,
                end_lat, end_lon);
        if (RA[1] < 0)
            RA[1] += 180;
        _survey.angle = (float) RA[1];
        AZController.setLength(_survey, (float) RA[0], true);
        _survey.center.lat = (start_lat + end_lat) / 2.0;
        _survey.center.lon = (end_lat + end_lon) / 2.0;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.gmeci.atskresources.CoordinateHandJamDialog.HandJamInterface#
     * UpdateCoordinateFormat(java.lang.String)
     */
    @Override
    public void UpdateCoordinateFormat(String DisplayFormat) {
        currentCoordinateFormat = DisplayFormat;
        UpdateCoordinateDisplay();
    }

    private void UpdateCoordinateDisplay() {

        Unit displayUnit = (_unitsFeet ? Unit.FOOT : Unit.METER);

        SurveyPoint approach = AZHelper.CalculateAnchorFromAZCenter(
                _survey, _survey.center, _survey.getApproachAnchor());
        SurveyPoint departure = AZHelper.CalculateAnchorFromAZCenter(
                _survey, _survey.center, _survey.getDepartureAnchor());

        textViewButtons[APPROACH_COORDINATES].setText(Conversions
                .getCoordinateString(approach.lat, approach.lon,
                        currentCoordinateFormat));
        textViewButtons[DEPARTURE_COORDINATES].setText(Conversions
                .getCoordinateString(departure.lat, departure.lon,
                        currentCoordinateFormat));
        textViewButtons[APPROACH_THRESHOLD].setText(String.format(
                LocaleUtil.getCurrent(),
                "Threshold: %s", Unit.METER.format(_survey.aDisplacedThreshold,
                        displayUnit)));
        textViewButtons[DEPARTURE_THRESHOLD].setText(String.format(
                LocaleUtil.getCurrent(),
                "Threshold: %s", Unit.METER.format(_survey.dDisplacedThreshold,
                        displayUnit)));
        textViewButtons[APPROACH_GSR].setText(String.format(
                LocaleUtil.getCurrent(),
                "GSR: %s (%s)", Conversions.ConvertGlideSlopeAngleToRatio(
                        _survey.approachGlideSlopeDeg),
                Conversions.ConvertGlideSlopeAngleToRatio(
                        _survey.minApproachGlideSlopeDeg)));
        textViewButtons[DEPARTURE_GSR].setText(String.format(
                LocaleUtil.getCurrent(),
                "GSR: %s (%s)", Conversions.ConvertGlideSlopeAngleToRatio(
                        _survey.departureGlideSlopeDeg),
                Conversions.ConvertGlideSlopeAngleToRatio(
                        _survey.minDepartureGlideSlopeDeg)));
    }

    /*
     * Dialog Update Interface Callbacks (non-Javadoc)
     *
     * @see com.gmeci.atskresources.ATSKDialogManager.DialogUpdateInterface#
     * UpdateMeasurement(float)
     */
    @Override
    public void UpdateMeasurement(int index, double measurement) {
        Log.d(TAG, "Updating Measurement");

        switch (currentlySelectedTVIndex) {
            case USABLE_WIDTH:
                break; // how do we handle this? only other two are editable?
            case RUNWAY_WIDTH:
                AZController.setWidth(_survey, Math.abs(measurement));
                break;
            case SHOULDER_WIDTH:
                _survey.edges.ShoulderWidth_m = Math.abs(measurement);
                break;
            case USABLE_LENGTH:
                break;
            case RUNWAY_LENGTH:
                AZController.setLength(_survey, Math.abs(measurement), false);
                break;
            case APPROACH_OVERRUN_LENGTH:
                AZController.setOverrunLength(_survey, measurement, true);
                break;
            case DEPARTURE_OVERRUN_LENGTH:
                AZController.setOverrunLength(_survey, measurement, false);
                break;
            case RUNWAY_HEADING:
                _survey.angle = measurement;
                break;
        }
        UpdateMeasurements();
    }

    @Override
    public void UpdateStringValue(int index, String value) {
        //name must be updated...
        _survey.setSurveyName(value);
        valueTV[RUNWAY_NAME].setText(_survey.getSurveyName());
    }

    @Override
    public void UpdateAngleUnits(boolean usingTrue) {
        _angleTrue = usingTrue;
        UpdateUnitsDisplay();
    }

    @Override
    public void UpdateDimensionUnits(boolean usingFeet) {
        _unitsFeet = usingFeet;
        UpdateUnitsDisplay();
    }

    @Override
    public void UpdateGSRAngleUnits(boolean GSR) {
        // TODO Auto-generated method stub

    }

    public interface RunwayInfoParentInterface {
        void Update(boolean redraw);
    }
}
