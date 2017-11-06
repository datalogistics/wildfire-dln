
package com.gmeci.atsk.az.lz;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.TextView;

import android.graphics.Color;

import com.gmeci.atsk.az.spinners.LZSpinner;
import com.gmeci.atsk.resources.ATSKApplication;
import com.gmeci.core.Criteria;
import com.atakmap.android.atsk.plugin.R;
import com.atakmap.coremap.log.Log;
import com.gmeci.atsk.az.AZBaseMeasurementFragment;
import com.gmeci.atsk.az.spinners.ATSKSpinner;

import com.gmeci.conversions.Conversions;

public class LZMeasurementFragment extends AZBaseMeasurementFragment {

    private static final String RESET_MEASUREMENTS_CONFIRM_TYPE = "ResetMeasurementsToMinimum";
    private static final String TAG = "LZMeasurementFragment";
    //Spinner stuff
    LZSpinner acSpinner;
    ATSKSpinner surfaceSpinner;
    private Button _reverseEnds;
    private int currentSpinnerSelectIndex = 0;
    private Criteria currentAircraft;
    private final static double THIRD_METER = 1 / 3d;

    private TextView runwayInfoTextView, surfaceLabelTextView;
    private final OnItemSelectedListener acSelectedListener = new OnItemSelectedListener() {

        @Override
        public void onItemSelected(AdapterView<?> parent, View view,
                int position, long id) {
            if (position == currentSpinnerSelectIndex)
                return;

            currentSpinnerSelectIndex = position;

            //reset measurement to ac minimumns uses the aircraft name so we need 
            //to set it here.
            Criteria crit = acSpinner.getACByIndex(position);
            if (crit != null && !surveyData.aircraft.equals(crit.Name)) {
                surveyData.aircraft = crit.Name;
                ResetMeasurementsToACMinimums();
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    };

    public LZMeasurementFragment() {
        AZTypeToDisplay = LZ_TWO_POINT;
        StandardUnitsFeet = true;
    }

    private void testCriteria(boolean notifyUpdate) {
        if (currentAircraft != null) {
            // recalculate the endClearZoneInnerWidth
            surveyData.endClearZoneInnerWidth = (currentAircraft
                    .getRunwayShoulder()
                    + currentAircraft.getMaintainedArea()
                    + currentAircraft.getGradedArea())
                    * 2.0f + surveyData.width;
        }

        TextView length = (TextView) _root
                .findViewById(R.id.dz_crit_meas_text_length_val);
        TextView width = (TextView) _root
                .findViewById(R.id.dz_crit_meas_text_width_val);
        TextView appthresh = (TextView) _root
                .findViewById(R.id.dz_crit_meas_text_appthres_val);
        TextView depthresh = (TextView) _root
                .findViewById(R.id.dz_crit_meas_text_depthres_val);
        length.setTextColor(Color.WHITE);
        width.setTextColor(Color.WHITE);
        appthresh.setTextColor(Color.WHITE);
        depthresh.setTextColor(Color.WHITE);

        if (surveyData != null) {
            surveyData.valid = true;
        }

        if (surveyData != null && currentAircraft != null) {

            if (surveyData.getLength() < currentAircraft.getRunwayLength()
                    - THIRD_METER) {
                length.setTextColor(Color.RED);
                surveyData.valid = false;
            }
            if (surveyData.width < currentAircraft.getRunwayWidth()
                    - THIRD_METER) {
                width.setTextColor(Color.RED);
                surveyData.valid = false;
            }

            if (!surveyData.surveyIsLTFW()) {

                if (surveyData.edges.ApproachOverrunLength_m < currentAircraft
                        .getApproachOverrunLength() - THIRD_METER) {

                    appthresh.setTextColor(Color.RED);
                    surveyData.valid = false;
                }
                if (surveyData.edges.DepartureOverrunLength_m < currentAircraft
                        .getDepartureOverrunLength() - THIRD_METER) {
                    depthresh.setTextColor(Color.RED);
                    surveyData.valid = false;
                }
            }
            if (notifyUpdate)
                azpc.UpdateAZ(surveyData, "validity", false);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        SetupSpinners();
        ResetMeasurementsToACMinimums();
    }

    @Override
    public void onPause() {
        // Force recalculate on tab close just to be safe
        UpdateCalculateGSR(true);
        super.onPause();
    }

    @Override
    public void postRecalc() {
        super.postRecalc();
        ResetMeasurementsToACMinimums();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        Context plugin = ATSKApplication.getInstance().getPluginContext();
        _root = LayoutInflater.from(plugin).inflate(
                R.layout.lz_crit_meas_fragment, container,
                false);
        return _root;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        surfaceLabelTextView = (TextView) _root
                .findViewById(R.id.surface_type_label);

        surfaceSpinner = (ATSKSpinner) _root
                .findViewById(R.id.runway_surface_spinner);
        surfaceSpinner.setPrompt("SURFACE TYPES");
        acSpinner = (LZSpinner) _root
                .findViewById(R.id.lz_crit_crit_spin_ac_type);
        acSpinner.setPrompt("SUPPORTED AIRCRAFT");

        acSpinner.setOnItemSelectedListener(acSelectedListener);

        // For debugging purposes
        _reverseEnds = (Button) _root.findViewById(R.id.reverse_ends);
        _reverseEnds.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (surveyData != null) {
                    surveyData.angle = Conversions
                            .deg360(surveyData.angle + 180);
                    UpdateDisplayMeasurements(true);
                }
            }
        });

        surfaceSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                    int position, long id) {
                String surface = surfaceSpinner.getItem(position);
                if (!surveyData.surface.equals(surface)) {
                    surveyData.surface = surface;
                    azpc.UpdateAZ(surveyData, "surface", false);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }

        });
    }

    private void SetupSpinners() {
        acSpinner.setup();

        currentSpinnerSelectIndex = acSpinner
                .GetPosition(surveyData.aircraft);
        acSpinner.setSelection(currentSpinnerSelectIndex);

        int SurfacePosition = surfaceSpinner.GetPosition(surveyData.surface);
        surfaceSpinner.setSelection(SurfacePosition);
    }

    @Override
    protected void UpdateSurvey(boolean AllowDBUpdate) {
        azpc.UpdateAZ(surveyData, "MeasurementsChanged", AllowDBUpdate);
    }

    // Sync with aircraft criteria
    private void ResetMeasurementsToACMinimums() {

        Log.d(TAG, "Resetting Measurements...");

        currentAircraft = acSpinner.getACByIndex(currentSpinnerSelectIndex);

        if (currentAircraft == null) {
            Log.d(TAG,
                    "no aircraft information found, no way to set or check the criteria.");
            return;
        }

        // move the original calculations that would determine the type of LZ 
        // so that they are set from the Criteria and not the actual values in 
        // the survey 
        surveyData.ltfw = (currentAircraft.getMaintainedArea() <= 0 &&
                currentAircraft.getDepartureOverrunLength() > 0);
        surveyData.stol = (currentAircraft.getMaintainedArea() <= 0 &&
                currentAircraft.getDepartureOverrunLength() <= 0);

        //surveyData.endClearZoneInnerWidth = currentAircraft.EndClearZoneInnerWidth_m;
        surveyData.endClearZoneInnerWidth = (currentAircraft
                .getRunwayShoulder()
                + currentAircraft.getMaintainedArea()
                + currentAircraft.getGradedArea())
                * 2.0f + surveyData.width;
        surveyData.endClearZoneOuterWidth = currentAircraft.EndClearZoneOuterWidth_m;
        surveyData.endClearZoneLength = currentAircraft.EndClearZoneLength_m;

        surveyData.edges.ShoulderWidth_m = currentAircraft.getRunwayShoulder();
        surveyData.edges.MaintainedAreaWidth_m = currentAircraft
                .getMaintainedArea();
        surveyData.edges.GradedAreaWidth_m = currentAircraft.getGradedArea();
        surveyData.approachInnerWidth = currentAircraft
                .getInnerApproachZoneWidth();
        surveyData.approachOuterWidth = currentAircraft
                .getOuterApproachZoneWidth();
        surveyData.approachInnerLength = currentAircraft
                .getInnerApproachZoneLength();
        surveyData.approachOuterLength = currentAircraft
                .getOuterApproachZoneLength();

        surveyData.edges.GradientThreshholdLZLonIntervalMax = currentAircraft.IncrementalLongitudinal;
        surveyData.edges.GradientThreshholdLZLonOverallMax = currentAircraft.OverallLongitudinal;

        surveyData.minApproachGlideSlopeDeg = currentAircraft
                .getApproachGlideSlope();
        surveyData.minDepartureGlideSlopeDeg = currentAircraft
                .getDepartureGlideSlope();

        // This updates the survey
        UpdateDisplayMeasurements(true);
    }

    @Override
    public void UpdateDisplayMeasurements(boolean b) {
        if (surveyData != null) {
            testCriteria(false);
            UpdateCalculateGSR(false);
            super.UpdateDisplayMeasurements(b);
        }
    }
}
