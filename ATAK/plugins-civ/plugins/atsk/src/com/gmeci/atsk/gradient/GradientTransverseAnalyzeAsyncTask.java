
package com.gmeci.atsk.gradient;

import android.database.Cursor;
import android.os.AsyncTask;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.atakmap.coremap.log.Log;
import com.gmeci.core.ATSKConstants;
import com.gmeci.core.LZEdges;
import com.gmeci.core.PointObstruction;
import com.gmeci.core.SurveyData;
import com.gmeci.core.SurveyPoint;
import com.gmeci.atskservice.resolvers.AZProviderClient;
import com.gmeci.atskservice.resolvers.DBURIConstants;
import com.gmeci.atskservice.resolvers.ObstructionProviderClient;
import com.gmeci.helpers.GradientAnalysisOPCHelper;
import com.gmeci.atskservice.resolvers.GradientProviderClient;
import com.gmeci.conversions.Conversions;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

public class GradientTransverseAnalyzeAsyncTask extends
        AsyncTask<String, Float, Float> {

    public static final String TAG = "GradientTransverseAnalyzeAsyncTask";
    final AZProviderClient azpc;
    final String CurrentSurveyUID;
    final GradientProviderClient gpc;
    final ProgressBar pb;
    final TextView tv;
    final ObstructionProviderClient opc;

    GradientTransverseAnalyzeAsyncTask(AZProviderClient azpc,
            String CurrentSurveyUID, GradientProviderClient gpc,
            ProgressBar pb, TextView tv, ObstructionProviderClient opc) {
        this.CurrentSurveyUID = CurrentSurveyUID;
        this.gpc = gpc;
        this.azpc = azpc;
        this.pb = pb;
        this.tv = tv;
        this.opc = opc;
    }

    @Override
    protected void onPostExecute(Float result) {
        super.onPostExecute(result);
        pb.setVisibility(View.GONE);
        tv.setVisibility(View.GONE);

    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        pb.setVisibility(View.VISIBLE);
        pb.setMax(100);
        tv.setVisibility(View.VISIBLE);
        tv.setText("Analyzing Gradients");
    }

    @Override
    protected void onProgressUpdate(Float... valuesF) {
        super.onProgressUpdate(valuesF);
        double valueF = valuesF[0];

        tv.setText(String.format("Analyzing Gradients: %.1f%%", valueF * 100));
        pb.setProgress((int) (valueF * 100));
    }

    @Override
    protected Float doInBackground(String... params) {

        SurveyData survey = azpc.getAZ(CurrentSurveyUID, false);

        if (survey == null)
            return -1f;

        survey.edges.LeftHalfRunwayGradient = 0;
        survey.edges.RightHalfRunwayGradient = 0;

        GradientAnalysisOPCHelper gaopch = new GradientAnalysisOPCHelper(
                survey.getLength(false), survey.width, gpc,
                survey.angle, survey.center.lat, survey.center.lon);
        final Cursor AllAnalyzedGradientsCursor = gpc.GetAnalyzedGradients(
                CurrentSurveyUID, true);
        if (AllAnalyzedGradientsCursor == null)
            return -2f;
        int UIDColumnIndex = AllAnalyzedGradientsCursor
                .getColumnIndex(DBURIConstants.COLUMN_UID);
        int TypeColumnIndex = AllAnalyzedGradientsCursor
                .getColumnIndex(DBURIConstants.COLUMN_TYPE);
        for (AllAnalyzedGradientsCursor.moveToFirst(); !AllAnalyzedGradientsCursor
                .isAfterLast()
                && !this.isCancelled(); AllAnalyzedGradientsCursor
                .moveToNext()) {
            String GradientUID = AllAnalyzedGradientsCursor
                    .getString(UIDColumnIndex);
            String Type = AllAnalyzedGradientsCursor.getString(TypeColumnIndex);

            if (Type.startsWith(ATSKConstants.GRADIENT_TYPE_TRANSVERSE)) {
                LZEdges grad = gaopch.AnalyzeTransverseGradient(
                        GradientUID, survey);

                //save worst to currentSurvey
                if (Math.abs(grad.LeftGradedAreaGradient) > Math
                        .abs(survey.edges.LeftGradedAreaGradient))
                    survey.edges.LeftGradedAreaGradient = grad.LeftGradedAreaGradient;
                if (Math.abs(grad.RightGradedAreaGradient) > Math
                        .abs(survey.edges.RightGradedAreaGradient))
                    survey.edges.RightGradedAreaGradient = grad.RightGradedAreaGradient;

                if (Math.abs(grad.LeftMaintainedAreaGradient) > Math
                        .abs(survey.edges.LeftMaintainedAreaGradient))
                    survey.edges.LeftMaintainedAreaGradient = grad.LeftMaintainedAreaGradient;
                if (Math.abs(grad.RightMaintainedAreaGradient) > Math
                        .abs(survey.edges.RightMaintainedAreaGradient))
                    survey.edges.RightMaintainedAreaGradient = grad.RightMaintainedAreaGradient;
                if (Math.abs(grad.LeftShoulderGradient) > Math
                        .abs(survey.edges.LeftShoulderGradient))
                    survey.edges.LeftShoulderGradient = grad.LeftShoulderGradient;
                if (Math.abs(grad.RightShoulderGradient) > Math
                        .abs(survey.edges.RightShoulderGradient))
                    survey.edges.RightShoulderGradient = grad.RightShoulderGradient;

                if (Math.abs(grad.LeftHalfRunwayGradient) > Math
                        .abs(survey.edges.LeftHalfRunwayGradient))
                    survey.edges.LeftHalfRunwayGradient = grad.LeftHalfRunwayGradient;
                if (Math.abs(grad.RightHalfRunwayGradient) > Math
                        .abs(survey.edges.RightHalfRunwayGradient))
                    survey.edges.RightHalfRunwayGradient = grad.RightHalfRunwayGradient;
            } else if (Type
                    .startsWith(ATSKConstants.GRADIENT_TYPE_LONGITUDINAL)) {
                HashMap<Double, String> problems = gaopch
                        .AnalyzeLongitudinalGradient(GradientUID,
                                (float) (ATSKConstants.
                                GRADIENT_SPACING_LONGITUDINAL_FT
                                / Conversions.M2F), survey);

                SurveyPoint startPoint = Conversions.AROffset(survey.center,
                        survey.angle + 180, (survey.getLength(false) / 2)
                                + survey.edges.ApproachOverrunLength_m);
                opc.DeletePoints(ATSKConstants.GRADIENT_GROUP);
                //draw problems on the map
                Iterator<Entry<Double, String>> it = problems.entrySet()
                        .iterator();
                while (it.hasNext()) {
                    Map.Entry<Double, String> pairs = it
                            .next();
                    double DistanceFromApproachCenter = pairs.getKey();
                    String ProblemDescription = pairs.getValue();
                    double LGradientProblemLocation[] = Conversions.AROffset(
                            startPoint.lat, startPoint.lon,
                            survey.angle,
                            DistanceFromApproachCenter);

                    PointObstruction LGradientProblem = new PointObstruction();
                    LGradientProblem.group = ATSKConstants.GRADIENT_GROUP;
                    LGradientProblem.lat = LGradientProblemLocation[0];
                    LGradientProblem.lon = LGradientProblemLocation[1];
                    LGradientProblem.alt = startPoint.alt;
                    LGradientProblem.type = ProblemDescription;
                    LGradientProblem.uid = UUID.randomUUID()
                            .toString();
                    // ATSKConstants.GRADIENT_TYPE_LONGITUDINAL_BAD + ProblemCount;
                    Log.d(TAG, "Adding " + LGradientProblem.uid + " to "
                            + LGradientProblem.lat + " " + LGradientProblem.lon);
                    opc.NewPoint(LGradientProblem);

                    Log.d(TAG, pairs.getKey() + " = "
                            + pairs.getValue());
                    it.remove(); // avoids a ConcurrentModificationException
                }
            }

            azpc.UpdateAZ(survey, "azgu", false);
            publishProgress((float) AllAnalyzedGradientsCursor
                    .getPosition() / AllAnalyzedGradientsCursor.getCount());
        }
        if (isCancelled()) {
            AllAnalyzedGradientsCursor.close();
            return (float) -1;
        }
        int Count = AllAnalyzedGradientsCursor.getCount();
        AllAnalyzedGradientsCursor.close();
        return (float) Count;

    }

}
