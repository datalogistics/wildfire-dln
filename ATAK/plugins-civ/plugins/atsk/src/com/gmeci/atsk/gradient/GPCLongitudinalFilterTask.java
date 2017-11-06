
package com.gmeci.atsk.gradient;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import com.atakmap.coremap.log.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.gmeci.core.ATSKConstants;
import com.gmeci.core.LineObstruction;
import com.gmeci.core.SurveyData;
import com.gmeci.core.SurveyPoint;
import com.gmeci.atskservice.resolvers.AZProviderClient;
import com.gmeci.atskservice.resolvers.DBURIConstants;
import com.gmeci.atskservice.resolvers.GradientProviderClient;
import com.gmeci.helpers.AZHelper;
import com.gmeci.conversions.Conversions;
import com.gmeci.helpers.GradientAnalysisOPCHelper;
import com.sromku.polygon.PolyPoint;
import com.sromku.polygon.Polygon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GPCLongitudinalFilterTask extends
        AsyncTask<String, Float, Integer> {
    private static final String TAG = "GPCLongitudinalFilterTask";
    private int LatColumn, LonColumn, IDColumn, altColumn;
    private final GradientProviderClient gpc;
    private final AZProviderClient azpc;
    private final Context context;
    private String UID;
    private final ProgressBar pb;
    private final TextView tv;
    private double MinDistanceBetweenPoints_m = 1;
    private double ReportedCount = 100;

    public GPCLongitudinalFilterTask(Context context,
            AZProviderClient azpc, GradientProviderClient gpc, ProgressBar pb,
            TextView tv, double MinDistanceBetweenPoints_m) {
        this.context = context;
        this.azpc = azpc;
        this.gpc = gpc;
        this.pb = pb;
        this.tv = tv;
        this.MinDistanceBetweenPoints_m = MinDistanceBetweenPoints_m;
    }

    @Override
    protected Integer doInBackground(String... SurveyUID) {
        //clear DB to mark all points not-used

        UID = SurveyUID[0];
        gpc.ClearGradientAnalysis(SurveyUID[0]);
        SurveyData sd = azpc.getAZ(UID, false);
        if (sd != null)
            FindLongitudinalGradient(SurveyUID[0], sd.getSurveyName(), this);

        return 1;
    }

    private Polygon buildPolygon(ArrayList<SurveyPoint> transCorners) {
        return Polygon
                .Builder()
                .addVertex(
                        new PolyPoint(transCorners.get(0).lat, transCorners
                                .get(0).lon))
                .addVertex(
                        new PolyPoint(transCorners.get(1).lat, transCorners
                                .get(1).lon))
                .addVertex(
                        new PolyPoint(transCorners.get(2).lat, transCorners
                                .get(2).lon))
                .addVertex(
                        new PolyPoint(transCorners.get(3).lat, transCorners
                                .get(3).lon)).build();
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        this.pb.setVisibility(View.VISIBLE);
        this.tv.setVisibility(View.VISIBLE);

        tv.setText("Longitudinal Gradient");

    }

    @Override
    protected void onPostExecute(Integer result) {
        super.onPostExecute(result);
        pb.setVisibility(View.GONE);
        tv.setVisibility(View.GONE);
    }

    @Override
    protected void onProgressUpdate(Float... values) {
        double fvalue = values[0];
        pb.setProgress((int) fvalue);
        if (ReportedCount != values[1]) {
            ReportedCount = values[1];
            pb.setMax((int) ReportedCount);
        }
        tv.setText(String.format(
                "Finding Longitudinal Gradient Points: %.0f%%",
                100 * values[0] / values[1]));
        super.onProgressUpdate(values);
    }

    Cursor getBoundedCursor(ArrayList<SurveyPoint> corners) {
        double TopLat = -90, BottomLat = 90, LeftLon = 180, RightLon = -180;
        for (SurveyPoint currentPoint : corners) {
            if (currentPoint.lat > TopLat)
                TopLat = currentPoint.lat;
            if (currentPoint.lat < BottomLat)
                BottomLat = currentPoint.lat;
            if (currentPoint.lon > RightLon)
                RightLon = currentPoint.lon;
            if (currentPoint.lon < LeftLon)
                LeftLon = currentPoint.lon;

        }
        final Cursor transCursor = gpc.GetGradientPointsBounded(TopLat,
                BottomLat,
                LeftLon, RightLon, "");
        return transCursor;
    }

    private void GetColumnIDs(final Cursor cursor) {
        //find column positions for accessing cursor
        IDColumn = cursor.getColumnIndex(DBURIConstants.COLUMN_ID);
        LatColumn = cursor.getColumnIndex(DBURIConstants.COLUMN_LAT);
        LonColumn = cursor.getColumnIndex(DBURIConstants.COLUMN_LON);
        altColumn = cursor.getColumnIndex(DBURIConstants.COLUMN_HAE_M);
    }

    private void FindLongitudinalGradient(String surveyUID, String surveyName,
            AsyncTask parentTask) {
        gpc.ClearGradientAnalysis(surveyUID);
        SurveyData currentSurvey = azpc.getAZ(surveyUID, true);
        if (currentSurvey == null)
            return;

        ArrayList<SurveyPoint> corners = AZHelper.getCorners(
                currentSurvey.center.lat, currentSurvey.center.lon,
                currentSurvey.getLength(false),
                ATSKConstants.LONGITUDINAL_RIBBON_WIDTH_M,
                currentSurvey.angle,
                -currentSurvey.edges.ApproachOverrunLength_m,
                -currentSurvey.edges.DepartureOverrunLength_m);

        //get cursor for possibly useful points
        Polygon longitudinalPolygon = buildPolygon(corners);
        final Cursor boundedCursor = getBoundedCursor(corners);

        if (boundedCursor == null)
            return;

        if (boundedCursor.getCount() < 1) {
            boundedCursor.close();
            return;
        }

        //find column positions for accessing cursor
        GetColumnIDs(boundedCursor);

        double approachDist = (currentSurvey.getLength(false) / 2)
                + currentSurvey.edges.ApproachOverrunLength_m;
        double[] f = Conversions.AROffset(currentSurvey.center.lat,
                currentSurvey.center.lon, currentSurvey.angle + 180,
                approachDist);
        SurveyPoint startPoint = new SurveyPoint(f[0], f[1]);

        Log.d(TAG, "Start point = " + Conversions.GetMGRS(startPoint));
        for (SurveyPoint sp : corners)
            Log.d(TAG, "Corner = " + Conversions.GetMGRS(sp));

        boolean FoundStart = false, FoundMiddle = false, FoundEnd = false;
        double usableRunway = currentSurvey.getLength(false);
        List<SurveyPoint> lonPoints = new ArrayList<SurveyPoint>();
        for (boundedCursor.moveToFirst(); !boundedCursor.isAfterLast()
                && !parentTask.isCancelled(); boundedCursor.moveToNext()) {
            final double lat = boundedCursor.getDouble(LatColumn);
            final double lon = boundedCursor.getDouble(LonColumn);
            final double alt = boundedCursor.getFloat(altColumn);
            if (lat < 90 && lat > -90 && lon < 180 && lon > -180 &&
                    longitudinalPolygon.contains(new PolyPoint(lat, lon))) {
                double ra[] = Conversions.CalculateRangeAngle(
                        startPoint.lat, startPoint.lon, lat, lon);
                double angDiff = Conversions
                        .deg360(ra[1] - currentSurvey.angle);
                double range = (float) ra[0] * Math.cos(
                        Math.toRadians(angDiff));
                if (range < usableRunway / 3)
                    FoundStart = true;
                else if (range < 2 * usableRunway / 3)
                    FoundMiddle = true;
                else
                    FoundEnd = true;
                gpc.SetGradientAnalysisResults(
                        boundedCursor.getInt(IDColumn), (float) range,
                        ATSKConstants.LONGITUDINAL, "");
                SurveyPoint sp = new SurveyPoint(lat, lon);
                sp.setHAE(alt);
                sp.speed = range;
                lonPoints.add(sp);
            }
            this.publishProgress((float) boundedCursor.getPosition(),
                    (float) boundedCursor.getCount());
        }
        boundedCursor.close();
        Log.d(TAG, "Found " + lonPoints + " bounded points");

        //in the gradient cursor - there are still TONS of possible points - most will be invalid
        //Valid points have a RANGE value set

        // Find good points for longitudinal; remove any duplicates/redundant points
        Collections.sort(lonPoints, new SurveyPoint.SpeedComparator(true));
        LineObstruction longitudinal = new LineObstruction();
        int lastRangeFt = -1, avgCount = 0;
        SurveyPoint avgPoint = new SurveyPoint();
        for (SurveyPoint sp : lonPoints) {
            int rangeFt = (int) Math.round(sp.speed * Conversions.M2F);
            if (rangeFt > lastRangeFt) {
                if (avgCount > 0) {
                    avgPoint.lat /= avgCount;
                    avgPoint.lon /= avgCount;
                    avgPoint.speed /= avgCount;
                    avgPoint.setHAE(avgPoint.getHAE() / avgCount);
                    longitudinal.points.add(avgPoint);
                }
                avgPoint = new SurveyPoint(sp);
                avgCount = 1;
                lastRangeFt = rangeFt;
            } else if (rangeFt == lastRangeFt) {
                avgPoint.lat += sp.lat;
                avgPoint.lon += sp.lon;
                avgPoint.speed += sp.speed;
                avgPoint.setHAE(avgPoint.getHAE() + sp.getHAE());
                avgCount++;
            }
        }

        Log.d(TAG, "Filtered longitudinal has " + longitudinal.points.size()
                + " points");

        // Check if canceled
        if (parentTask.isCancelled())
            return;

        gpc.DeleteGradient(ATSKConstants.DEFAULT_GROUP,
                GradientAnalysisOPCHelper
                        .GetLongidudinalGradientUID(surveyUID));
        longitudinal.uid = GradientAnalysisOPCHelper
                .GetLongidudinalGradientUID(surveyUID);
        longitudinal.group = ATSKConstants.DEFAULT_GROUP;

        longitudinal.remarks = surveyName;

        //check if the minimal requirements are met
        if (FoundStart && FoundMiddle && FoundEnd)
            longitudinal.type = ATSKConstants.GRADIENT_TYPE_LONGITUDINAL;
        else {
            if (!FoundStart)
                Log.w(TAG, "Failed to find longitudinal start");
            if (!FoundMiddle)
                Log.w(TAG, "Failed to find longitudinal middle");
            if (!FoundEnd)
                Log.w(TAG, "Failed to find longitudinal end");
            longitudinal.type = ATSKConstants.GRADIENT_TYPE_LONGITUDINAL_BAD;
        }
        if (parentTask.isCancelled())
            return;
        //draw this - should look same as filtered line but have WAY fewer points in it 
        gpc.NewGradient(longitudinal);
    }

}
