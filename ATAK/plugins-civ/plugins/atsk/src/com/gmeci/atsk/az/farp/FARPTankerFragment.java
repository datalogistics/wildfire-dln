
package com.gmeci.atsk.az.farp;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.TextView;

import com.atakmap.android.atsk.plugin.R;
import com.atakmap.coremap.log.Log;
import com.gmeci.atsk.az.AZController;
import com.gmeci.atsk.resources.ATSKApplication;
import com.gmeci.core.ATSKConstants;
import com.gmeci.core.ATSKIntentConstants;
import com.gmeci.atsk.az.spinners.ATSKSpinner;
import com.gmeci.atsk.resources.ATSKDialogManager;
import com.gmeci.atsk.resources.ATSKDialogManager.DialogUpdateInterface;
import com.gmeci.atsk.resources.CoordinateHandJamDialog;
import com.gmeci.atskservice.farp.FARPTankerItem;
import com.gmeci.conversions.Conversions;
import com.gmeci.core.SurveyPoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FARPTankerFragment extends FARPTabBase implements
        DialogUpdateInterface, CoordinateHandJamDialog.HandJamInterface {

    private static final String TAG = "FARPRefuellerFragment";
    private static final int LOCATION = 0;
    private static final int ROTATION = 1;

    TextView surfaceTV, ACLocationStaticTV, ACLocationTV, RotationStaticTV,
            RotationTV, RotationUnitsTV;
    ATSKSpinner SurfaceSpinner, ACSpinner;

    protected int _selected = -1;
    private final OnLongClickListener TVLongClickListener = new OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            if (v.getId() == ACLocationStaticTV.getId()
                    || v.getId() == ACLocationTV.getId()) {
                _selected = LOCATION;
                ShowCoordinateHandJamDialog(surveyData.center.lat,
                        surveyData.center.lon, surveyData.center.getHAE());
            } else if (v.getId() == RotationStaticTV.getId()
                    || v.getId() == RotationTV.getId()
                    || v.getId() == RotationUnitsTV.getId()) {
                _selected = ROTATION;
                ATSKDialogManager adm = new ATSKDialogManager(getActivity(),
                        FARPTankerFragment.this, false);
                adm.ShowAngleHandJamDialog(surveyData.center.course_true,
                        surveyData.center, _selected);
            }
            return true;
        }
    };
    private final OnClickListener TVClickListener = new OnClickListener() {
        public void onClick(View v) {

            if (_selected > -1) {
                stopCollection();
            } else {
                setOBState(ATSKIntentConstants.OB_STATE_REQUESTED_POINT);
                //unselect everything...
                if (v.getId() == ACLocationStaticTV.getId()
                        || v.getId() == ACLocationTV.getId()) {
                    _selected = LOCATION;
                    ACLocationTV
                            .setBackgroundResource(R.drawable.background_selected_left);
                    ACLocationStaticTV
                            .setBackgroundResource(R.drawable.background_selected_left);
                    Notify("TANKER POSITION", "", "Select Tanker Position", "");
                } else if (v.getId() == RotationStaticTV.getId()
                        || v.getId() == RotationUnitsTV.getId()
                        || v.getId() == RotationTV.getId()) {
                    _selected = ROTATION;
                    RotationStaticTV
                            .setBackgroundResource(R.drawable.background_selected_left);
                    RotationUnitsTV
                            .setBackgroundResource(R.drawable.background_selected_left);
                    RotationTV
                            .setBackgroundResource(R.drawable.background_selected_left);

                    Notify("TANKER ROTATION", "", "Select Tanker ",
                            "Nose Angle");
                }
            }
            //update screen in case we have any "Waiting" notifications hanging around
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        Context pluginContext = ATSKApplication
                .getInstance().getPluginContext();
        return LayoutInflater.from(pluginContext).inflate(
                R.layout.farp_tanker, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        Update();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupFARPRefuellerButton(view);
        setupDetailsButton(view);
    }

    private void setupFARPRefuellerButton(View view) {
        //if the collection is still running from a previous start - we should display it

        Context pluginContext = ATSKApplication
                .getInstance().getPluginContext();

        surfaceTV = (TextView) view.findViewById(R.id.surface_static);

        ACLocationStaticTV = (TextView) view.findViewById(R.id.Location_static);
        ACLocationTV = (TextView) view.findViewById(R.id.Location);

        ACLocationStaticTV.setOnClickListener(TVClickListener);
        ACLocationTV.setOnClickListener(TVClickListener);
        ACLocationStaticTV.setOnLongClickListener(TVLongClickListener);
        ACLocationTV.setOnLongClickListener(TVLongClickListener);

        RotationStaticTV = (TextView) view.findViewById(R.id.Rotation_static);
        RotationTV = (TextView) view.findViewById(R.id.Rotation);
        RotationUnitsTV = (TextView) view.findViewById(R.id.Rotation_units);

        RotationStaticTV.setOnClickListener(TVClickListener);
        RotationTV.setOnClickListener(TVClickListener);
        RotationUnitsTV.setOnClickListener(TVClickListener);

        RotationStaticTV.setOnLongClickListener(TVLongClickListener);
        RotationTV.setOnLongClickListener(TVLongClickListener);
        RotationUnitsTV.setOnLongClickListener(TVLongClickListener);

        SurfaceSpinner = (ATSKSpinner) view
                .findViewById(R.id.runway_surface_spinner);
        String[] surfaceTypes = pluginContext.getResources().getStringArray(
                R.array.atsk_runway_surface_types);
        ArrayList<String> typesList = new ArrayList<String>(
                Arrays.asList(surfaceTypes));
        SurfaceSpinner.SetupSpinner(typesList);
        SurfaceSpinner.setSelection(0);
        SurfaceSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adaptparent, View view,
                    int pos, long id) {
                surveyData.surface = (String) adaptparent
                        .getItemAtPosition(pos);
                azpc.UpdateAZ(surveyData, "FARPAC", true);
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }

        });
        Log.d(TAG, "We have these items surface:" + SurfaceSpinner.getCount());

        ACSpinner = (ATSKSpinner) view.findViewById(R.id.ac_spinner);
        ACSpinner.SetupSpinner(getTankerNameList());
        ACSpinner.setSelection(0);
        ACSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adaptparent, View view,
                    int pos, long id) {
                //ac changed - we should make sure we delete the old airplane
                String ac = (String) adaptparent.getItemAtPosition(pos);
                fixRxOffset(surveyData.center.lat, surveyData.center.lon,
                        surveyData.angle, ac);
                surveyData.aircraft = ac;

                // Make sure the selected receiver is supported by this aircraft
                FARPTankerItem tanker = AZController.getInstance()
                        .getTanker(surveyData.aircraft);
                if (tanker != null) {
                    List<String> supportedRx = tanker.getReceivers();
                    if (!supportedRx.isEmpty() && !supportedRx
                            .contains(surveyData.FAMRxShape))
                        surveyData.updateFAMShape(supportedRx.get(0), tanker);
                }

                azpc.UpdateAZ(surveyData, "FARPAC", true);
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }

        });
        Log.d(TAG, "We have these items ac:" + SurfaceSpinner.getCount());
    }

    private ArrayList<String> getTankerNameList() {
        return new ArrayList<String>(AZController.getInstance()
                .getTankers().keySet());
    }

    void ShowCoordinateHandJamDialog(double currentLat, double currentLon,
            double elevation) {

        CoordinateHandJamDialog chjd = new CoordinateHandJamDialog();
        chjd.Initialize(currentLat, currentLon,
                DisplayCoordinateFormat, elevation, this);
    }

    private void UpdateDisplayMeasurements() {
        surveyData.center.course_true =
                Conversions.deg360(surveyData.center.course_true);
        surveyData.angle = surveyData.center.course_true;

        if (DisplayAnglesTrue) {
            RotationTV.setText(String.format("%.1f",
                    surveyData.center.course_true));
            RotationUnitsTV.setText(String.format("%cT",
                    ATSKConstants.DEGREE_SYMBOL));
        } else {
            RotationUnitsTV.setText(String.format("%cM",
                    ATSKConstants.DEGREE_SYMBOL));

            double MagAngle = Conversions.GetMagAngle(
                    surveyData.center.course_true, surveyData.center.lat,
                    surveyData.center.lon);
            RotationTV.setText(String.format("%.1f", MagAngle));
        }

        String CoordinateString = Conversions.getCoordinateString(
                surveyData.center.lat, surveyData.center.lon,
                DisplayCoordinateFormat);
        ACLocationTV.setText(CoordinateString);
    }

    @Override
    protected void Update() {
        super.Update();

        UpdateDisplayMeasurements();
        String AngleString = gps_settings.getString(ATSKConstants.UNITS_ANGLE,
                ATSKConstants.UNITS_ANGLE_MAG);
        DisplayAnglesTrue = !AngleString.equals(ATSKConstants.UNITS_ANGLE_MAG);

        ACSpinner.setSelection(ACSpinner
                .GetPosition(surveyData.aircraft));

        SurfaceSpinner.setSelection(SurfaceSpinner
                .GetPosition(surveyData.surface));
    }

    @Override
    public void UpdateCoordinate(double lat, double lon, double elevation) {
        fixRxOffset(lat, lon, surveyData.angle, surveyData.aircraft);
        surveyData.center.lat = lat;
        surveyData.center.lon = lon;
        surveyData.center.setHAE(elevation);

        UpdateDisplayMeasurements();
        azpc.UpdateAZ(surveyData, "FARP", true);
    }

    @Override
    public void UpdateCoordinateFormat(String DisplayFormat) {
        DisplayCoordinateFormat = DisplayFormat;
        UpdateDisplayMeasurements();
    }

    @Override
    public void UpdateMeasurement(int index, double measurement) {
        if (index == ROTATION) {
            fixRxOffset(surveyData.center.lat, surveyData.center.lon,
                    measurement, surveyData.aircraft);
            surveyData.center.course_true = measurement;
            surveyData.angle = measurement;
        }
        UpdateDisplayMeasurements();
        azpc.UpdateAZ(surveyData, "FARP", true);

    }

    @Override
    public void UpdateStringValue(int index, String value) {
    }

    @Override
    public void UpdateAngleUnits(boolean usingTrue) {
        DisplayAnglesTrue = usingTrue;
        gps_settings.edit().putString(ATSKConstants.UNITS_ANGLE,
                usingTrue ? ATSKConstants.UNITS_ANGLE_TRUE
                        : ATSKConstants.UNITS_ANGLE_MAG).apply();
        UpdateDisplayMeasurements();
        azpc.UpdateAZ(surveyData, "FARP", true);
    }

    @Override
    public void UpdateDimensionUnits(boolean usingFeet) {
    }

    @Override
    protected void UpdateScreen() {

    }

    @Override
    public void newPosition(SurveyPoint sp, boolean TopCollected) {

        if (_selected == LOCATION) {
            fixRxOffset(sp.lat, sp.lon, surveyData.angle, surveyData.aircraft);
            surveyData.center.setSurveyPoint(sp);
            //in FARP - we'll use CE as the min distance circle
            surveyData.center.circularError = 0;
        } else if (_selected == ROTATION) {
            double degTrue = Conversions.calculateAngle(sp, surveyData.center);
            fixRxOffset(surveyData.center.lat, surveyData.center.lon,
                    degTrue, surveyData.aircraft);
            surveyData.center.course_true = degTrue;
            surveyData.angle = degTrue;
        }

        azpc.UpdateAZ(surveyData, "FARP", true);
        UpdateDisplayMeasurements();
    }

    private void fixRxOffset(double newLat, double newLon,
            double newAng, String newAircraft) {
        SurveyPoint newPoint = new SurveyPoint(newLat, newLon);
        FARPTankerItem tanker = AZController.getInstance()
                .getTanker(surveyData.aircraft);
        double angOffset = surveyData.angle - newAng;
        for (int i = 0; i < surveyData.FAMPoints.length; i++) {
            if (surveyData.FAMPoints[i] != null) {
                SurveyPoint fuelPoint = tanker != null ? tanker.getFuelPoint(
                        surveyData, i == 0) : surveyData.center;
                double rxAng = Conversions.calculateAngle(fuelPoint,
                        surveyData.FAMPoints[i]) - angOffset;
                tanker = AZController.getInstance().getTanker(newAircraft);
                fuelPoint = tanker != null ? tanker.getFuelPoint(
                        surveyData.FAMRxShape,
                        newPoint, newAng, i == 0) : newPoint;
                SurveyPoint sp = Conversions.AROffset(fuelPoint,
                        rxAng, surveyData.getFAMDistance());
                surveyData.FAMPoints[i].setSurveyPoint(sp);
                surveyData.FAMRxAngle[i] -= angOffset;
            }
        }
    }

    @Override
    public void UpdateGSRAngleUnits(boolean GSR) {
    }

    @Override
    protected void stopCollection() {
        _selected = -1;
        ACLocationTV.setBackgroundColor(NON_SELECTED_BG_COLOR);
        ACLocationStaticTV.setBackgroundColor(NON_SELECTED_BG_COLOR);
        RotationStaticTV.setBackgroundColor(NON_SELECTED_BG_COLOR);
        RotationUnitsTV.setBackgroundColor(NON_SELECTED_BG_COLOR);
        RotationTV.setBackgroundColor(NON_SELECTED_BG_COLOR);
        setOBState(ATSKIntentConstants.OB_STATE_HIDDEN);
    }

}
