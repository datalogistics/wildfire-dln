
package com.gmeci.atsk.az.lz;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.atakmap.android.atsk.plugin.R;
import com.gmeci.atsk.resources.ATSKApplication;
import com.gmeci.core.ATSKConstants;
import com.gmeci.core.LineObstruction;
import com.gmeci.core.PointObstruction;
import com.gmeci.core.SurveyData;
import com.gmeci.core.SurveyPoint;
import com.gmeci.atsk.az.AZTabBase;
import com.gmeci.conversions.Conversions;
import com.gmeci.conversions.Conversions.Unit;

import java.util.List;

public class LZThresholdFragment extends AZTabBase {

    private static final String TAG = "LZThresholdFragment";
    boolean ShowGSR = true;
    boolean ShowIncursions = true;
    private View _root;
    private Button GSRButton, ToggleGSRButton;
    private TextView ApproachTV, DepartureTV, AppGSRTV, DepGSRTV,
            AppGSRValueTV, DepGSRValueTV;
    private TextView AppDTLabelTV, AppDTValueTV, AppDTUnitsTV, DepDTLabelTV,
            DepDTValueTV, DepDTUnitsTV;
    private ProgressBar progressBar;
    private final OnClickListener gsrButtonClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            UpdateCalculateGSR();
        }
    };

    public static double GetOverrunOffset(boolean Approach, SurveyData survey) {
        if (!Approach)
            return (survey.edges.ApproachOverrunLength_m - survey.edges.DepartureOverrunLength_m) / 2;
        return (survey.edges.DepartureOverrunLength_m - survey.edges.ApproachOverrunLength_m) / 2;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        Context pluginContext = ATSKApplication
                .getInstance().getPluginContext();
        _root = LayoutInflater.from(pluginContext).inflate(
                R.layout.lz_threshold_fragment, container,
                false);

        return _root;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        GSRButton = (Button) _root.findViewById(R.id.glide_slope_button);
        GSRButton.setOnClickListener(gsrButtonClickListener);

        ToggleGSRButton = (Button) _root
                .findViewById(R.id.toggle_glide_slope_button);
        ToggleGSRButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                ShowIncursions = !ShowIncursions;
                if (ShowIncursions) {
                    ToggleGSRButton.setText("HIDE INCURSIONS");
                } else {
                    ToggleGSRButton.setText("SHOW INCURSIONS");
                }

                UpdateCalculateGSR();
            }

        });

        progressBar = (ProgressBar) _root.findViewById(R.id.topPB);
        progressBar.setVisibility(View.GONE);
        setupTextviews();
    }

    boolean isDepartureGSR(int ID2Check) {
        return ID2Check == DepGSRTV.getId()
                || ID2Check == DepGSRValueTV.getId();
    }

    boolean isApproachGSR(int ID2Check) {
        return ID2Check == AppGSRTV.getId()
                || ID2Check == AppGSRValueTV.getId();
    }

    private void updateDisplayValues() {

        Unit dispUnit = (DisplayUnitsStandard ? Unit.FOOT : Unit.METER);
        AppDTValueTV.setText(Unit.METER.format(
                surveyData.aDisplacedThreshold, dispUnit));
        DepDTValueTV.setText(Unit.METER.format(
                surveyData.dDisplacedThreshold, dispUnit));

        if (ShowGSR) {
            String AppGSR = String
                    .format("%s (%s) ",
                            Conversions
                                    .ConvertGlideSlopeAngleToRatio(surveyData.approachGlideSlopeDeg),
                            Conversions
                                    .ConvertGlideSlopeAngleToRatio(surveyData.minApproachGlideSlopeDeg));
            AppGSRValueTV.setText(AppGSR);
            String DepGSR = String
                    .format("%s (%s) ",
                            Conversions
                                    .ConvertGlideSlopeAngleToRatio(surveyData.departureGlideSlopeDeg),
                            Conversions
                                    .ConvertGlideSlopeAngleToRatio(surveyData.minDepartureGlideSlopeDeg));
            DepGSRValueTV.setText(DepGSR);
        } else {
            String AppGSR = String.format("%.2f%c (%.2f%c) ",
                    surveyData.approachGlideSlopeDeg,
                    ATSKConstants.DEGREE_SYMBOL,
                    surveyData.minApproachGlideSlopeDeg,
                    ATSKConstants.DEGREE_SYMBOL);
            AppGSRValueTV.setText(AppGSR);
            String DepGSR = String.format("%.2f%c (%.2f%c) ",
                    surveyData.departureGlideSlopeDeg,
                    ATSKConstants.DEGREE_SYMBOL,
                    surveyData.minDepartureGlideSlopeDeg,
                    ATSKConstants.DEGREE_SYMBOL);
            DepGSRValueTV.setText(DepGSR);
        }
    }

    private void setupTextviews() {
        ApproachTV = (TextView) _root.findViewById(R.id.lz_approach_label);
        DepartureTV = (TextView) _root.findViewById(R.id.lz_departure_label);

        AppGSRTV = (TextView) _root.findViewById(R.id.lz_approach_gsr);
        DepGSRTV = (TextView) _root.findViewById(R.id.lz_departure_gsr);

        AppGSRValueTV = (TextView) _root
                .findViewById(R.id.lz_approach_gsr_value);
        DepGSRValueTV = (TextView) _root
                .findViewById(R.id.lz_departure_gsr_value);

        AppDTLabelTV = (TextView) _root.findViewById(R.id.lz_approach_dt);
        AppDTValueTV = (TextView) _root
                .findViewById(R.id.lz_approach_dt_value);
        AppDTUnitsTV = (TextView) _root
                .findViewById(R.id.lz_approach_dt_units);
        DepDTLabelTV = (TextView) _root.findViewById(R.id.lz_departure_dt);
        DepDTValueTV = (TextView) _root
                .findViewById(R.id.lz_departure_dt_value);
        DepDTUnitsTV = (TextView) _root
                .findViewById(R.id.lz_departure_dt_units);

        DepDTUnitsTV.setOnLongClickListener(null);
        AppDTUnitsTV.setOnLongClickListener(null);
        DepDTLabelTV.setOnLongClickListener(null);
        AppDTLabelTV.setOnLongClickListener(null);
        AppDTValueTV.setOnLongClickListener(null);
        DepDTValueTV.setOnLongClickListener(null);

        AppGSRTV.setOnLongClickListener(null);
        DepGSRTV.setOnLongClickListener(null);
        AppGSRValueTV.setOnLongClickListener(null);
        DepGSRValueTV.setOnLongClickListener(null);
    }

    public void onPause() {
        if (surveyData != null) {
            HideIncursions();
            azpc.UpdateAZ(surveyData, "saveoff", true);
        }
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();

        String unitPreference = user_settings.getString(
                ATSKConstants.UNITS_DISPLAY, ATSKConstants.UNITS_FEET);
        DisplayUnitsStandard = unitPreference
                .equalsIgnoreCase(ATSKConstants.UNITS_FEET);

        ShowGSR = true;
        String gsrPreference = user_settings.getString(
                ATSKConstants.UNITS_GSR_ANGLE,
                ATSKConstants.UNITS_GSR_ANGLE_GSR);
        if (gsrPreference.equalsIgnoreCase(ATSKConstants.UNITS_GSR_ANGLE_ANGLE))
            ShowGSR = false;

        loadCurrentSurvey();
        updateDisplayValues();
        UpdateCalculateGSR();
    }

    @Override
    public void shotApproved(SurveyPoint sp, double range_m, double az_deg,
            double el_deg, boolean TopCollected) {
    }

    @Override
    public void newPosition(SurveyPoint sp, boolean TopCollected) {
    }

    @Override
    protected void UpdateCalculateGSR(boolean forceUpdate) {
        HideIncursions();
        super.UpdateCalculateGSR(forceUpdate);
        updateDisplayValues();
    }

    protected void UpdateCalculateGSR() {
        UpdateCalculateGSR(true);
    }

    @Override
    protected PointObstruction UpdateApproachIncursion(
            List<PointObstruction> ApproachPoints) {

        PointObstruction worst = super.UpdateApproachIncursion(ApproachPoints);
        if (!ShowIncursions)
            return worst;

        SurveyPoint appCenter = getCenterOfApproach(true, surveyData);
        LineObstruction ApproachIncursion = new LineObstruction();
        ApproachIncursion.uid = surveyData.uid + "_"
                + ATSKConstants.INCURSION_LINE_APPROACH;
        ApproachIncursion.type = ATSKConstants.INCURSION_LINE_APPROACH;
        ApproachIncursion.height = worst.height;
        ApproachIncursion.width = 0;
        ApproachIncursion.closed = true;
        ApproachIncursion.filled = true;
        ApproachIncursion.remarks = ATSKConstants.INCURSION_LINE_APPROACH;
        ApproachIncursion.group = ATSKConstants.DEFAULT_GROUP;
        ApproachIncursion.points.add(appCenter);

        for (PointObstruction incursion : ApproachPoints) {
            ApproachIncursion.points.add(incursion);
            ApproachIncursion.points.add(appCenter);
        }
        opc.NewLine(ApproachIncursion, true);
        return worst;
    }

    @Override
    protected PointObstruction UpdateDepartureIncursion(
            List<PointObstruction> DeparturePoints) {

        PointObstruction worst = super
                .UpdateDepartureIncursion(DeparturePoints);
        if (!ShowIncursions)
            return worst;

        SurveyPoint depCenter = getCenterOfApproach(false, surveyData);
        LineObstruction DepartureIncursion = new LineObstruction();
        DepartureIncursion.uid = surveyData.uid + "_"
                + ATSKConstants.INCURSION_LINE_DEPARTURE;
        DepartureIncursion.type = ATSKConstants.INCURSION_LINE_DEPARTURE;
        DepartureIncursion.height = worst.height;
        DepartureIncursion.width = 0;
        DepartureIncursion.closed = true;
        DepartureIncursion.filled = true;
        DepartureIncursion.remarks = ATSKConstants.INCURSION_LINE_DEPARTURE;
        DepartureIncursion.group = ATSKConstants.DEFAULT_GROUP;
        DepartureIncursion.points.add(depCenter);

        for (PointObstruction incursion : DeparturePoints) {
            DepartureIncursion.points.add(incursion);
            DepartureIncursion.points.add(depCenter);
        }
        opc.NewLine(DepartureIncursion, true);
        return worst;
    }

    public void HideIncursions() {
        if (surveyData != null) {
            opc.DeleteLine(ATSKConstants.DEFAULT_GROUP, surveyData.uid + "_" +
                    ATSKConstants.INCURSION_LINE_APPROACH);
            opc.DeleteLine(ATSKConstants.DEFAULT_GROUP, surveyData.uid + "_" +
                    ATSKConstants.INCURSION_LINE_DEPARTURE);
            opc.DeletePoint(ATSKConstants.DEFAULT_GROUP, surveyData.uid + "_" +
                    ATSKConstants.INCURSION_LINE_APPROACH_WORST, true);
            opc.DeletePoint(ATSKConstants.DEFAULT_GROUP, surveyData.uid + "_" +
                    ATSKConstants.INCURSION_LINE_DEPARTURE_WORST, true);
        }
    }

    @Override
    protected void UpdateScreen() {
    }
}
