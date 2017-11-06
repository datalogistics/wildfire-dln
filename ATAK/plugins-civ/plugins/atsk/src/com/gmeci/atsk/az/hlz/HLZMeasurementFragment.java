
package com.gmeci.atsk.az.hlz;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import com.atakmap.android.atsk.plugin.R;
import com.atakmap.coremap.log.Log;
import com.gmeci.atsk.resources.ATSKApplication;
import com.gmeci.core.SurveyPoint;
import com.gmeci.atsk.az.AZBaseMeasurementFragment;
import com.gmeci.helpers.AZHelper;
import com.gmeci.conversions.Conversions;

public class HLZMeasurementFragment extends AZBaseMeasurementFragment {
    private static final String TAG = "HLZMeasurementFragment";

    final Context pluginContext;
    SlopeFindingAsyncTask findSlopeTask;

    public HLZMeasurementFragment() {
        AZTypeToDisplay = LZ_TWO_POINT;
        StandardUnitsFeet = true;
        pluginContext = ATSKApplication.getInstance().getPluginContext();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //show the slope stuff
        LinearLayout slopeLayout = (LinearLayout) _root
                .findViewById(R.id.hlz_slope);
        slopeLayout.setVisibility(View.VISIBLE);
        LabelTV[SLOPE_L_POSITION].setOnClickListener(null);
        ValueTV[SLOPE_L_POSITION].setOnClickListener(null);
        LabelTV[SLOPE_W_POSITION].setOnClickListener(null);
        ValueTV[SLOPE_W_POSITION].setOnClickListener(null);
    }

    @Override
    public void newPosition(SurveyPoint sp, boolean TopCollected) {
        super.newPosition(sp, TopCollected);
        Log.d(TAG, "New Position RXed");
        //calculate new slope?

        if (CurrentlyEditedIndex != -1)
            recalcSlopes();
    }

    @Override
    protected boolean StoreMeasurement(double newMeasurement_m) {
        boolean ret = super.StoreMeasurement(newMeasurement_m);
        if (CurrentlyEditedIndex != -1)
            recalcSlopes();
        return ret;
    }

    @Override
    protected boolean StoreMeasurement(double range, double angle) {
        boolean ret = super.StoreMeasurement(range, angle);
        if (CurrentlyEditedIndex != -1)
            recalcSlopes();
        return ret;
    }

    @Override
    public void UpdateCoordinate(double Lat, double Lon, double elevation) {
        super.UpdateCoordinate(Lat, Lon, elevation);
        if (CurrentlyEditedIndex != -1)
            recalcSlopes();
    }

    @Override
    protected void setToggleState(boolean circular, boolean AllowUpdateDB) {
        boolean surveyCircular = surveyData != null && surveyData.circularAZ;
        super.setToggleState(circular, AllowUpdateDB);
        if (surveyData != null && !_surveyNeedsUpdate
                && surveyCircular != circular)
            recalcSlopes();
    }

    protected void recalcSlopes() {
        findSlopeTask = new SlopeFindingAsyncTask();
        findSlopeTask.execute(1, 1, 1);
    }

    class SlopeFindingAsyncTask extends AsyncTask<Integer, Integer, Integer> {
        private static final double INVALID_ALT = SurveyPoint.Altitude.INVALID;
        double CenterElevation_m = INVALID_ALT,
                InlineElevation_m = INVALID_ALT,
                SideElevation_m = INVALID_ALT;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            ValueTV[SLOPE_L_POSITION].setText("Calculating");
            ValueTV[SLOPE_W_POSITION].setText("Calculating");
        }

        @Override
        protected void onPostExecute(Integer result) {

            super.onPostExecute(result);

            double surveyWidth = surveyData.circularAZ ?
                    surveyData.getRadius() * 2 : surveyData.width;
            double surveyLen = surveyData.circularAZ ?
                    surveyWidth : surveyData.getLength();

            if (CenterElevation_m != INVALID_ALT) {
                if (SideElevation_m != INVALID_ALT && surveyWidth > 0) {
                    double rise = SideElevation_m - CenterElevation_m;
                    surveyData.slopeW = (float) (rise / surveyWidth);
                }
                if (InlineElevation_m != INVALID_ALT && surveyLen > 0) {
                    double rise = InlineElevation_m - CenterElevation_m;
                    surveyData.slopeL = (float) (rise / surveyLen);
                }
            }
            UpdateDisplayMeasurements(true);
        }

        @Override
        protected Integer doInBackground(Integer... params) {
            //get elevation of center and top or bottom center and right or left center

            CenterElevation_m = ATSKApplication.getElevation_m_hae(
                    surveyData.center.lat, surveyData.center.lon);
            // First try approach edge
            SurveyPoint ApproachCenter = AZHelper.CalculateCenterOfEdge(
                    surveyData, true);
            InlineElevation_m = ATSKApplication.getElevation_m_hae(
                    ApproachCenter.lat, ApproachCenter.lon);
            if (InlineElevation_m == INVALID_ALT) {
                // Then try departure edge
                SurveyPoint DepartureCenter = AZHelper
                        .CalculateCenterOfEdge(surveyData, false);
                InlineElevation_m = ATSKApplication.getElevation_m_hae(
                        DepartureCenter.lat, DepartureCenter.lon);
            }
            double halfWidth = surveyData.circularAZ ?
                    surveyData.getRadius() : surveyData.width / 2;

            // First try right edge
            double RightEdge[] = Conversions.AROffset(
                    surveyData.center.lat, surveyData.center.lon,
                    surveyData.angle + 90, halfWidth);
            SideElevation_m = ATSKApplication.getElevation_m_hae(
                    RightEdge[0], RightEdge[1]);
            if (SideElevation_m == INVALID_ALT) {
                // Then try left edge
                double LeftEdge[] = Conversions.AROffset(
                        surveyData.center.lat, surveyData.center.lon,
                        surveyData.angle - 90, halfWidth);
                SideElevation_m = ATSKApplication.getElevation_m_hae(
                        LeftEdge[0], LeftEdge[1]);
            }

            return null;
        }

    }

}
