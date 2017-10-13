
package com.gmeci.atsk.az.dz;

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.atakmap.android.atsk.plugin.R;
import com.atakmap.coremap.log.Log;
import com.gmeci.atsk.resources.ATSKApplication;
import com.gmeci.core.ATSKConstants;
import com.gmeci.core.ATSKIntentConstants;
import com.gmeci.atsk.az.AZTabBase;
import com.gmeci.atsk.resources.ATSKDialogManager;
import com.gmeci.atsk.resources.ATSKDialogManager.DialogUpdateInterface;
import com.gmeci.atsk.resources.CoordinateHandJamDialog;
import com.gmeci.atsk.resources.CoordinateHandJamDialog.HandJamInterface;
import com.gmeci.atskservice.resolvers.AZURIConstants;
import com.gmeci.conversions.Conversions;
import com.gmeci.conversions.Conversions.Unit;
import com.gmeci.core.SurveyPoint;

public class DZOriginFragment extends AZTabBase implements HandJamInterface,
        DialogUpdateInterface {

    protected static final int ORIGIN_POSITION = 0;
    protected static final int ORIGINDESC_POSITION = 1;
    protected static final int HEADING_POSITION = 2;
    protected static final int DIST_POSITION = 3;
    protected static final int SELECTED_BG_COLOR = 0xff376d37;
    protected static final int NON_SELECTED_BG_COLOR = 0xff383838;
    protected static final double SELECTED_SIZE_MULTIPLIER = 1.25f;
    private final TextView[] LabelTV = new TextView[5];
    private final TextView[] ValueTV = new TextView[5];
    private final TextView[] UnitsTV = new TextView[5];
    private double Distance_m = 0, Heading_true = 0;
    private String Description = "";
    private int CurrentlyEditedIndex = 0;
    private String DisplayCoordinateFormat = Conversions.COORD_FORMAT_MGRS;
    final String TAG = "DZ Capabilities";
    private int StoredPositionIndex = 0;
    private final DialogUpdateInterface TextUpdateInterface = new DialogUpdateInterface() {

        @Override
        public void UpdateMeasurement(int index, double measurement) {
        }

        @Override
        public void UpdateStringValue(int index, String newText) {
            Description = newText;
            surveyData.PointOfOriginDescription = Description;
            //azpc.UpdateAZ(surveyData, "PoDDesc", false);
            UpdateDisplayMeasurements();
            azpc.UpdateAZ(surveyData, AZURIConstants.DZ_PARAMETERS_UPDATED,
                    true);
        }

        @Override
        public void UpdateAngleUnits(boolean usingTrue) {
            //

        }

        @Override
        public void UpdateDimensionUnits(boolean usingFeet) {
            //
        }

        @Override
        public void UpdateGSRAngleUnits(boolean GSR) {
            // TODO Auto-generated method stub

        }
    };
    //standard fragment start (view and inflator)
    private View _root;
    private final OnClickListener TVClickListener = new OnClickListener() {
        public void onClick(View v) {

            boolean Visible = isOBVisible();

            for (int i = 0; i < LabelTV.length; i++) {
                if (LabelTV[i] == null)
                    continue;

                int UnitsTVId = -1;
                if (UnitsTV[i] != null)
                    UnitsTVId = UnitsTV[i].getId();

                if (!Visible
                        && (v.getId() == LabelTV[i].getId() || v.getId() == ValueTV[i]
                                .getId())) {
                    CurrentlyEditedIndex = i;

                    //draw borders

                    //LOU we should use a 9 patch with a border...
                    LabelTV[i]
                            .setBackgroundResource(R.drawable.background_selected_left);
                    ValueTV[i]
                            .setBackgroundResource(R.drawable.background_selected_center);

                    if (UnitsTV[i] != null) {
                        UnitsTV[i]
                                .setBackgroundResource(R.drawable.background_selected_right);
                    } else
                        ValueTV[i]
                                .setBackgroundResource(R.drawable.background_selected_right);

                    if (CurrentlyEditedIndex == ORIGIN_POSITION) {
                        Log.d(TAG, "Position Collecting");
                        if (v.getId() == LabelTV[i].getId()
                                || v.getId() == ValueTV[i].getId())
                            Log.d(TAG, "OP Desitnation picked");
                        setOBState(ATSKIntentConstants.OB_STATE_REQUESTED_POINT);
                    } else if (v.getId() == LabelTV[ORIGINDESC_POSITION]
                            .getId()
                            || v.getId() == ValueTV[ORIGINDESC_POSITION]
                                    .getId()) {
                        CurrentlyEditedIndex = ORIGINDESC_POSITION;
                        ShowTextHandJamDialog(Description, "Origin Description");
                    }

                } else {//not a match

                    LabelTV[i].setBackgroundColor(NON_SELECTED_BG_COLOR);
                    ValueTV[i].setBackgroundColor(NON_SELECTED_BG_COLOR);
                    if (UnitsTV[i] != null) {
                        UnitsTV[i].setBackgroundColor(NON_SELECTED_BG_COLOR);
                    }
                    if (v.getId() == LabelTV[i].getId()
                            || v.getId() == ValueTV[i].getId()
                            || v.getId() == UnitsTVId)
                        setOBState(ATSKIntentConstants.OB_STATE_HIDDEN);
                }
            }
        }
    };
    private final OnLongClickListener TVLongClickListener = new OnLongClickListener() {

        @Override
        public boolean onLongClick(View v) {

            if (v.getId() == LabelTV[ORIGIN_POSITION].getId()
                    || v.getId() == ValueTV[ORIGIN_POSITION].getId()) {
                CurrentlyEditedIndex = ORIGIN_POSITION;
                ShowCoordinateHandJamDialog(surveyData.pointOfOrigin.lat,
                        surveyData.pointOfOrigin.lon,
                        surveyData.pointOfOrigin.getHAE());
                return true;
            } else if (v.getId() == LabelTV[ORIGINDESC_POSITION].getId()
                    || v.getId() == ValueTV[ORIGINDESC_POSITION].getId()) {
                CurrentlyEditedIndex = ORIGINDESC_POSITION;
                ShowTextHandJamDialog(Description, "Origin Description");
                return true;
            } else {
                Log.d(TAG, "Long Click  Missed!");
            }
            return false;
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        Context pluginContext = ATSKApplication
                .getInstance().getPluginContext();
        _root = LayoutInflater.from(pluginContext).inflate(
                R.layout.dz_crit_origin_fragment, container,
                false);
        return _root;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadCurrentSurvey();
        UpdateDistHeading();
        UpdateDisplayMeasurements();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        user_settings = PreferenceManager
                .getDefaultSharedPreferences(getActivity());

        SetupTextViews();
    }

    private void SetupTextViews() {

        ValueTV[ORIGIN_POSITION] = (TextView) _root
                .findViewById(R.id.dz_crit_meas_text_origin_val);
        LabelTV[ORIGIN_POSITION] = (TextView) _root
                .findViewById(R.id.dz_crit_meas_text_origin_label);

        ValueTV[ORIGINDESC_POSITION] = (TextView) _root
                .findViewById(R.id.dz_crit_meas_text_origindesc_val);
        LabelTV[ORIGINDESC_POSITION] = (TextView) _root
                .findViewById(R.id.dz_crit_meas_text_origindesc_label);

        ValueTV[HEADING_POSITION] = (TextView) _root
                .findViewById(R.id.dz_crit_meas_text_heading_val);
        LabelTV[HEADING_POSITION] = (TextView) _root
                .findViewById(R.id.dz_crit_meas_text_heading_label);
        UnitsTV[HEADING_POSITION] = (TextView) _root
                .findViewById(R.id.dz_crit_meas_text_heading_units);

        ValueTV[DIST_POSITION] = (TextView) _root
                .findViewById(R.id.dz_crit_meas_text_distance_val);
        LabelTV[DIST_POSITION] = (TextView) _root
                .findViewById(R.id.dz_crit_meas_text_distance_label);
        UnitsTV[DIST_POSITION] = (TextView) _root
                .findViewById(R.id.dz_crit_meas_text_distance_units);

        SetupTVClickListeners();
    }

    private void SetupTVClickListeners() {
        for (int i = 0; i < UnitsTV.length; i++) {
            if (UnitsTV[i] != null) {
                if (i != DIST_POSITION && i != HEADING_POSITION) {
                    UnitsTV[i].setClickable(true);
                    UnitsTV[i].setLongClickable(true);
                    UnitsTV[i].setOnLongClickListener(TVLongClickListener);
                    UnitsTV[i].setOnClickListener(TVClickListener);
                }
            }
        }
        for (int i = 0; i < ValueTV.length; i++) {
            if (ValueTV[i] != null) {

                if (i != DIST_POSITION && i != HEADING_POSITION) {
                    LabelTV[i].setClickable(true);
                    ValueTV[i].setClickable(true);
                    ValueTV[i].setLongClickable(true);
                    LabelTV[i].setLongClickable(true);

                    LabelTV[i].setOnLongClickListener(TVLongClickListener);
                    ValueTV[i].setOnLongClickListener(TVLongClickListener);
                    LabelTV[i].setOnClickListener(TVClickListener);
                    ValueTV[i].setOnClickListener(TVClickListener);
                }
            }
        }
    }

    void ShowTextHandJamDialog(String text, String DialogTitle) {
        ATSKDialogManager adm = new ATSKDialogManager(getActivity(),
                TextUpdateInterface, false);
        adm.ShowTextHandJamDialog(text, DialogTitle, CurrentlyEditedIndex);
    }

    protected boolean StoreMeasurement(double newMeasurement_m) {
        switch (CurrentlyEditedIndex) {
            case DIST_POSITION:
                Distance_m = newMeasurement_m;
                return true;

            case HEADING_POSITION:
                Heading_true = newMeasurement_m;
                return true;
        }
        return false;
    }

    protected void UpdateDisplayMeasurements() {
        UpdateUnitsDisplay();

        Unit dispUnit = (DisplayUnitsStandard ? Unit.YARD : Unit.METER);

        ValueTV[DIST_POSITION].setText(Unit.METER.format(Distance_m, dispUnit));

        if (DisplayAnglesTrue) {
            Heading_true = Conversions.deg360(Heading_true);
            ValueTV[HEADING_POSITION].setText(String.format("%.1f",
                    Heading_true));
        } else {
            double HeadingMag = Conversions.GetMagAngle(
                    Heading_true, surveyData.pointOfOrigin.lat,
                    surveyData.pointOfOrigin.lon);
            ValueTV[HEADING_POSITION]
                    .setText(String.format("%.1f", HeadingMag));
        }

        String CoordinateString = Conversions.getCoordinateString(
                surveyData.pointOfOrigin.lat, surveyData.pointOfOrigin.lon,
                DisplayCoordinateFormat);
        ValueTV[ORIGIN_POSITION].setText(CoordinateString);

        if (Description != null)
            ValueTV[ORIGINDESC_POSITION].setText(Description);
    }

    private void UpdateUnitsDisplay() {
        for (int i = 0; i < UnitsTV.length; i++) {
            if (UnitsTV[i] != null) {
                if (i == HEADING_POSITION) {
                    if (DisplayAnglesTrue) {
                        UnitsTV[i].setText(String.format("%cT",
                                ATSKConstants.DEGREE_SYMBOL));
                    } else {
                        UnitsTV[i].setText(String.format("%cM",
                                ATSKConstants.DEGREE_SYMBOL));
                    }
                } else if (DisplayUnitsStandard)
                    UnitsTV[i].setText(" yds");
                else
                    UnitsTV[i].setText(" m");
            }
        }
    }

    @Override
    public void newPosition(SurveyPoint sp, boolean TopCollected) {

        switch (CurrentlyEditedIndex) {
            case ORIGIN_POSITION: {
                surveyData.pointOfOrigin.setSurveyPoint(sp);
                StoredPositionIndex = 0;
                UpdateDistHeading();
                azpc.UpdateAZ(surveyData, AZURIConstants.DZ_PARAMETERS_UPDATED,
                        true);
                break;
            }

        }
        UpdateDisplayMeasurements();
    }

    private void UpdateDistHeading() {

        double[] RangeAngle = Conversions.CalculateRangeAngle(
                surveyData.pointOfOrigin.lat, surveyData.pointOfOrigin.lon,
                surveyData.center.lat, surveyData.center.lon);

        Distance_m = (float) RangeAngle[0];
        Heading_true = (float) Conversions.deg360(RangeAngle[1]);

        Description = surveyData.PointOfOriginDescription;

    }

    protected boolean StoreMeasurement(double range, double angle) {
        switch (CurrentlyEditedIndex) {
            case DIST_POSITION: {
                Distance_m = range;
                break;
            }
            case HEADING_POSITION: {
                Heading_true = range;
                break;
            }
        }
        //drawSurvey now that all measurements are happy
        return false;
    }

    @Override
    public void shotApproved(SurveyPoint sp, double range_m, double az_deg,
            double el_deg, boolean TopCollected) {
        if (DisplayAnglesTrue)
            az_deg = Conversions.GetTrueAngle(az_deg, sp.lat, sp.lon);
        //shot has been approved, here is the information. 
        switch (CurrentlyEditedIndex) {
            case HEADING_POSITION: {
                Heading_true = az_deg;
                break;
            }
            case DIST_POSITION: {
                Distance_m = range_m;
                break;
            }
            case ORIGIN_POSITION: {
                azpc.UpdateAZ(surveyData, AZURIConstants.DZ_PARAMETERS_UPDATED,
                        true);

                UpdateDistHeading();
                break;
            }
        }
        UpdateDisplayMeasurements();
    }

    void ShowCoordinateHandJamDialog(double currentLat, double currentLon,
            double elevation) {

        CoordinateHandJamDialog chjd = new CoordinateHandJamDialog();
        chjd.Initialize(currentLat, currentLon,
                DisplayCoordinateFormat, elevation, this);
    }

    @Override
    public void UpdateCoordinate(double Lat, double Lon, double elevation) {
        surveyData.pointOfOrigin.lat = Lat;
        surveyData.pointOfOrigin.lon = Lon;
        surveyData.pointOfOrigin.setHAE(elevation);

        azpc.UpdateAZ(surveyData, AZURIConstants.DZ_PARAMETERS_UPDATED, true);
    }

    @Override
    public void UpdateCoordinateFormat(String DisplayFormat) {
        DisplayCoordinateFormat = DisplayFormat;
        UpdateDisplayMeasurements();
    }

    @Override
    public void UpdateMeasurement(int index, double measurement) {
    }

    @Override
    public void UpdateStringValue(int index, String value) {
    }

    @Override
    public void UpdateAngleUnits(boolean usingTrue) {
    }

    @Override
    public void UpdateDimensionUnits(boolean usingFeet) {
    }

    @Override
    protected void UpdateScreen() {
    }

    @Override
    public void UpdateGSRAngleUnits(boolean GSR) {
    }

    protected void stopCollection() {
        CurrentlyEditedIndex = -1;
        for (int i = 0; i < LabelTV.length; i++) {
            if (LabelTV[i] != null)
                LabelTV[i].setBackgroundColor(NON_SELECTED_BG_COLOR);
            if (ValueTV[i] != null)
                ValueTV[i].setBackgroundColor(NON_SELECTED_BG_COLOR);
            if (UnitsTV[i] != null)
                UnitsTV[i].setBackgroundColor(NON_SELECTED_BG_COLOR);
        }
    }
}
