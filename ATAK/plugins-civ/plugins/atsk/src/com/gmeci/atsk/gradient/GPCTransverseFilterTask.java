
package com.gmeci.atsk.gradient;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.util.SparseArray;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.atakmap.coremap.log.Log;
import com.gmeci.atsk.resources.LogTime;
import com.gmeci.atskservice.resolvers.GradientDBItem;
import com.gmeci.core.ATSKConstants;
import com.gmeci.core.LineObstruction;
import com.gmeci.core.SurveyData;
import com.gmeci.core.SurveyPoint;
import com.gmeci.atskservice.resolvers.AZProviderClient;
import com.gmeci.atskservice.resolvers.GradientProviderClient;
import com.gmeci.helpers.AZHelper;
import com.gmeci.conversions.Conversions;
import com.gmeci.helpers.GradientAnalysisOPCHelper;
import com.sromku.polygon.PolyPoint;
import com.sromku.polygon.Polygon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GPCTransverseFilterTask extends AsyncTask<String, Float, Integer> {

    private static final double TRANSVERSE_RIBBON_WIDTH_M = 4f;
    private static final String TAG = "GPCTransverseFilterTask";
    private final GradientProviderClient gpc;
    private final AZProviderClient azpc;
    private final ProgressBar pb;
    private final TextView tv;
    private final List<Transverse> _transverses = new ArrayList<Transverse>();
    private double ReportedCount = 100;

    public GPCTransverseFilterTask(Context context,
            AZProviderClient azpc, GradientProviderClient gpc, ProgressBar pb,
            TextView tv) {
        this.azpc = azpc;
        this.gpc = gpc;
        this.pb = pb;
        this.tv = tv;
    }

    public static Polygon buildPolygon(List<SurveyPoint> transCorners) {
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
    protected Integer doInBackground(String... surveyuid) {
        //clear DB to mark all points not-used

        //lets look for transverse lines
        //clear out all points in gradient
        gpc.ClearGradientAnalysis("");
        //find the list of possible T-Gradient start points...
        FindTransverseGradients(surveyuid[0], this);
        return null;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        tv.setText("Transverse Gradient");
        this.pb.setVisibility(View.VISIBLE);
        this.tv.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onProgressUpdate(Float... values) {
        double fvalue = values[0];
        pb.setProgress((int) fvalue);
        if (ReportedCount != values[1]) {
            ReportedCount = values[1];
            pb.setMax((int) ReportedCount);
        }
        tv.setText(String.format("Finding Transverse Gradient Points: %.0f%%",
                100 * values[0] / values[1]));
        super.onProgressUpdate(values);
    }

    @Override
    protected void onPostExecute(Integer result) {
        super.onPostExecute(result);

        pb.setVisibility(View.GONE);
        tv.setVisibility(View.GONE);
    }

    private void FindTransverseGradients(String surveyName,
            AsyncTask parentTask) {
        _transverses.clear();
        //filter for start points along the edge of the survey
        gpc.ClearGradientAnalysis("");
        SurveyData survey = azpc.getAZ(surveyName, true);
        if (survey == null)
            return;

        // Create a box for detecting transverse candidates
        double halfWidth = survey.getFullWidth() / 2;
        List<SurveyPoint> fullCorners = AZHelper.getCorners(
                survey.center.lat, survey.center.lon, survey.getLength(false),
                halfWidth * 4, survey.angle);
        Polygon fullPoly = buildPolygon(fullCorners);

        final Cursor fullCur = gpc.GetAllGradients(
                ATSKConstants.DEFAULT_GROUP, false);
        if (fullCur == null)
            return;

        SparseArray<SurveyPoint> points = new SparseArray<SurveyPoint>();
        //GetColumnIDs(fullCur);
        float prog;
        float progMax = (float) fullCur.getCount() * 10;
        int pointId = 0;
        for (fullCur.moveToFirst(); !fullCur.isAfterLast()
                && !parentTask.isCancelled(); fullCur.moveToNext()) {
            GradientDBItem grad = gpc.GetGradient(fullCur, false);
            if (grad != null) {
                for (SurveyPoint sp : grad.getPoints()) {
                    if (fullPoly.contains(new PolyPoint(sp.lat, sp.lon))) {
                        sp.order = pointId++;
                        points.put(sp.order, sp);
                    }
                }
            }
            prog = (float) fullCur.getPosition();
            publishProgress(prog, progMax);
        }
        fullCur.close();

        SparseArray<PolyPoint> polyPoints = new SparseArray<PolyPoint>(
                points.size());
        for (int i = 0; i < points.size(); i++) {
            SurveyPoint sp = points.valueAt(i);
            polyPoints.put(sp.order, new PolyPoint(sp.lat, sp.lon));
        }
        int found = 0;
        int skipped = 0;
        // Set up progress so we go from 10% to 50%
        progMax = points.size() * (1f / 0.9f) * 2;
        prog = progMax - points.size();
        for (int i = 0; i < points.size(); i++) {
            SurveyPoint sp = points.valueAt(i);

            // Check to make sure point is past right maintained
            double range = calcRange(survey, sp.lat, sp.lon, false);
            if (range >= halfWidth) {
                // Calculate how far vertically the point is (relative to center)
                double lengthDiff = calcRange(survey, sp.lat, sp.lon, true);

                // Get far right bounds edge point from that position
                double[] pos = Conversions.AROffset(survey.center.lat,
                        survey.center.lon, survey.angle, lengthDiff);
                pos = Conversions.AROffset(pos[0], pos[1],
                        survey.angle + 90, halfWidth * 2);

                // Now build a horizontal polygon across the LZ
                // Length is equal to right edge point range
                List<SurveyPoint> trCorners = GetTransverseRibbon(
                        pos[0], pos[1], survey.angle - 90, halfWidth * 4);
                Polygon trPoly = buildPolygon(trCorners);

                // Filter points
                List<SurveyPoint> trPoints = new ArrayList<SurveyPoint>();
                SparseArray<PolyPoint> filtered = filterPoints(trPoly,
                        polyPoints);
                for (int j = 0; j < filtered.size(); j++)
                    trPoints.add(points.get(filtered.keyAt(j)));

                scanTransverse(survey, trPoints);
                found++;
            } else
                skipped++;
            publishProgress(++prog, progMax);
        }
        Log.d(TAG, "Found: " + found + " (" + _transverses.size()
                + " transverses), skipped " + skipped);
        LogTime.beginMeasure(TAG, "Publish");
        // Start progress at 50%
        progMax = _transverses.size() * 2;
        prog = _transverses.size();
        int trCount = 0;
        for (int i = 0; i < _transverses.size(); i++) {
            Transverse tr = _transverses.get(i);
            if (tr.valid()) {
                gpc.ClearGradientAnalysis("");
                List<SurveyPoint> trPoints = tr.getPoints();
                Collections.sort(trPoints, new SurveyPoint.
                        SpeedComparator(false));
                for (SurveyPoint sp : trPoints)
                    gpc.SetGradientAnalysisResults(sp.order, sp.speed,
                            ATSKConstants.TRANSVERSE, "");
                //find good points for all possible transverse
                LineObstruction trLine = new LineObstruction();
                trLine.points.addAll(trPoints);
                trLine.uid = GradientAnalysisOPCHelper
                        .GetTransverseGradientUID(survey.uid, trCount);
                trLine.group = ATSKConstants.DEFAULT_GROUP;
                trLine.type = ATSKConstants.GRADIENT_TYPE_TRANSVERSE;
                trLine.remarks = ATSKConstants.GRADIENT_TYPE_TRANSVERSE
                        + "_" + trCount;
                //draw this?
                gpc.NewGradient(trLine);
                trCount++;
            } else {
                Log.d(TAG, "Throwing out invalid transverse #" + i);
            }
            publishProgress(++prog, progMax);
        }
        LogTime.endMeasure(TAG, "Publish");
    }

    /**
     * Starting from a right edge point, find all points down the transverse
     * Must contain at least 3 points (past R maintained, past L maintained, and center)
     * @param survey The survey data
     * @param points List of gradient points within transverse polygon
     */
    private boolean scanTransverse(SurveyData survey, List<SurveyPoint> points) {
        PointChecker pChecker = new PointChecker(survey);
        for (SurveyPoint sp : points) {
            // Get range from center line
            sp.speed = calcRange(survey, sp.lat, sp.lon, false);
            // Check if this point satisfies our minimum requirements
            pChecker.add(sp);
        }
        // Enough data for a valid transverse
        if (pChecker.valid()) {
            // Second pass - take from existing transverses if we found
            // a better one
            int spread = pChecker.getSpread();
            for (Transverse tr : _transverses) {
                List<SurveyPoint> toRemove = new ArrayList<SurveyPoint>();
                for (SurveyPoint sp : points) {
                    if (tr.hasPoint(sp.order)) {
                        if (spread > tr.getSpread())
                            tr.removePoint(sp.order);
                        else
                            toRemove.add(sp);
                    }
                }
                for (SurveyPoint sp : toRemove)
                    points.remove(sp);
            }

            // Re-check
            pChecker.clear();
            for (SurveyPoint sp : points)
                pChecker.add(sp);
            if (!pChecker.valid())
                return false;
            _transverses.add(new Transverse(survey, pChecker.getSpread(),
                    points));
            return true;
        }
        return false;
    }

    private ArrayList<SurveyPoint> GetTransverseRibbon(double Lat, double Lon,
            double Angle, double Length) {
        SurveyPoint Center = AZHelper.CalculateLZCenter(
                ATSKConstants.ANCHOR_APPROACH_CENTER,
                new SurveyPoint(Lat, Lon), Length,
                TRANSVERSE_RIBBON_WIDTH_M, Angle);
        return AZHelper.getCorners(Center.lat, Center.lon, Length,
                TRANSVERSE_RIBBON_WIDTH_M, Angle);
    }

    /**
     * Calculate point vertical/horizontal range from survey center
     * @param survey Survey data
     * @param lat Latitude of point
     * @param lon Longitude of point
     * @param vertical True to calculate vertical range
     *                 false to calculate horizontal range
     * @return Range from center point in meters (negative if left/backward)
     */
    private double calcRange(SurveyData survey, double lat,
            double lon, boolean vertical) {
        double[] ra = Conversions.CalculateRangeAngle(
                survey.center.lat, survey.center.lon, lat, lon);
        ra[1] = Math.toRadians(Conversions.deg360(ra[1] - survey.angle));
        return ra[0] * (vertical ? Math.cos(ra[1]) : Math.sin(ra[1]));
    }

    private SparseArray<PolyPoint> filterPoints(Polygon poly,
            SparseArray<PolyPoint> points) {
        SparseArray<PolyPoint> ret = new SparseArray<PolyPoint>();
        for (int i = 0; i < points.size(); i++) {
            PolyPoint pp = points.valueAt(i);
            if (poly.contains(pp))
                ret.put(points.keyAt(i), pp);
        }
        return ret;
    }

    private static class PointChecker {
        public static final int NUM_RANGES = 10, FAR_LEFT = 0,
                FAR_RIGHT = NUM_RANGES - 1, MIDDLE_RIGHT = NUM_RANGES / 2,
                MIDDLE_LEFT = MIDDLE_RIGHT - 1;
        private final RangeThreshold[] _ranges = new RangeThreshold[NUM_RANGES];

        public PointChecker(SurveyData survey) {
            double halfWidth = survey.getFullWidth() / 2.0;
            double[] thresh = new double[] {
                    halfWidth,
                    survey.edges.MaintainedAreaWidth_m,
                    survey.edges.GradedAreaWidth_m,
                    survey.edges.ShoulderWidth_m,
                    survey.width / 2
            };
            double min = -halfWidth * 2, max;
            for (int i = 0; i < NUM_RANGES / 2; i++) {
                min += i > 0 ? thresh[i - 1] : 0;
                max = min + thresh[i];
                _ranges[i] = new RangeThreshold(min, max);
                _ranges[NUM_RANGES - i - 1] = new RangeThreshold(-max, -min);
            }
        }

        public boolean add(SurveyPoint sp) {
            for (RangeThreshold rt : _ranges) {
                if (rt.add(sp))
                    return true;
            }
            return false;
        }

        public boolean valid() {
            return !(/*_ranges[FAR_LEFT].empty()
                     || _ranges[FAR_RIGHT].empty()
                     || */_ranges[MIDDLE_LEFT].empty()
            && _ranges[MIDDLE_RIGHT].empty());
        }

        public int getSpread() {
            if (!valid())
                return 0;
            int spread = 0;
            for (RangeThreshold rt : _ranges)
                spread += rt.count;
            return spread;
        }

        public void clear() {
            for (RangeThreshold rt : _ranges)
                rt.count = 0;
        }
    }

    private static class RangeThreshold {
        private final double _minRange, _maxRange;
        public int count = 0;

        public RangeThreshold(double minRange, double maxRange) {
            if (minRange > maxRange) {
                _minRange = maxRange;
                _maxRange = minRange;
            } else {
                _minRange = minRange;
                _maxRange = maxRange;
            }
        }

        public boolean add(SurveyPoint sp) {
            if (sp.speed >= _minRange && sp.speed <= _maxRange) {
                this.count++;
                return true;
            }
            return false;
        }

        public boolean empty() {
            return this.count <= 0;
        }
    }

    private static class Transverse {
        private final SparseArray<SurveyPoint> _points = new SparseArray<SurveyPoint>();
        private int _spread = 0;
        private SurveyData _survey;

        public Transverse(SurveyData survey, int spread,
                List<SurveyPoint> points) {
            _survey = survey;
            _spread = spread;
            for (SurveyPoint sp : points)
                _points.put(sp.order, sp);
        }

        public synchronized List<SurveyPoint> getPoints() {
            List<SurveyPoint> ret = new ArrayList<SurveyPoint>();
            for (int i = 0; i < _points.size(); i++)
                ret.add(_points.valueAt(i));
            return ret;
        }

        public synchronized boolean hasPoint(int id) {
            SurveyPoint match = _points.get(id);
            return match != null && match.order == id;
        }

        public synchronized void removePoint(int id) {
            _points.remove(id);
            recalcSpread();
        }

        public synchronized int getSpread() {
            return _spread;
        }

        public boolean valid() {
            return check().valid();
        }

        private synchronized void recalcSpread() {
            PointChecker pc = check();
            _spread = pc.getSpread();
        }

        private synchronized PointChecker check() {
            PointChecker pc = new PointChecker(_survey);
            for (int i = 0; i < _points.size(); i++)
                pc.add(_points.valueAt(i));
            return pc;
        }
    }
}
