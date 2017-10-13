
package com.gmeci.helpers;

import android.database.Cursor;
import android.util.Log;

import com.gmeci.core.ATSKConstants;
import com.gmeci.core.LZEdges;
import com.gmeci.core.SurveyData;
import com.gmeci.core.SurveyPoint;
import com.gmeci.atskservice.resolvers.DBURIConstants;
import com.gmeci.atskservice.resolvers.GradientDBItem;
import com.gmeci.atskservice.resolvers.GradientProviderClient;
import com.gmeci.constants.Constants;
import com.gmeci.conversions.Conversions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class GradientAnalysisOPCHelper {
    private static final String TAG = "GradientOPCHelper";
    private final HashMap<Integer, IntervalGradientItem> gradientIntervals = new LogHashMap<Integer, IntervalGradientItem>();
    private SurveyPoint CenterOfGradientPoint;
    private int IDColumn, LatColumn, LonColumn, AltColumn;
    private final double CenterLat;
    private final double CenterLon;
    private final double AZHeading_deg;
    private final double AZWidth_m;
    private final double AZLength_m;
    private final GradientProviderClient gpc;
    private TransverseCalcHelper LMaintained = new TransverseCalcHelper(
            "L Maintained"),
            LGraded = new TransverseCalcHelper("L Graded"),
            LShoulder = new TransverseCalcHelper("L Shoulder"),
            LAZ = new TransverseCalcHelper("L Half-AZ", true),
            RAZ = new TransverseCalcHelper("R Half-AZ", true),
            RShoulder = new TransverseCalcHelper("R Shoulder"),
            RGraded = new TransverseCalcHelper("R Graded"),
            RMaintained = new TransverseCalcHelper("R Maintained");

    // Ordered from left to right
    private TransverseCalcHelper[] _trHelpers = {
            LMaintained, LGraded, LShoulder, LAZ, RAZ, RShoulder, RGraded,
            RMaintained
    };

    private boolean TransverseCalculated = false;

    public GradientAnalysisOPCHelper(double AZLength_m, double AZWidth_m,
            GradientProviderClient gpc, double AZHeading_deg_t,
            double CenterLat, double CenterLon) {
        this.gpc = gpc;
        this.AZWidth_m = AZWidth_m;
        this.AZLength_m = AZLength_m;
        AZHeading_deg = AZHeading_deg_t;
        this.CenterLat = CenterLat;
        this.CenterLon = CenterLon;
    }

    public static String GetLongidudinalGradientUID(String SurveyUID) {
        return SurveyUID + "_" + ATSKConstants.LONGITUDINAL;
    }

    public static String GetTransverseGradientUID(String SurveyUID,
            int TransverseCount) {
        return SurveyUID + "_" + ATSKConstants.TRANSVERSE + "_"
                + TransverseCount;
    }

    private static SurveyPoint lineIntersect(double x1, double y1, double x2,
            double y2, double x3, double y3, double x4, double y4) {
        double denom = (y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1);
        if (denom == 0.0) { // Lines are parallel.
            return null;
        }
        double ua = ((x4 - x3) * (y1 - y3) - (y4 - y3) * (x1 - x3)) / denom;
        double ub = ((x2 - x1) * (y1 - y3) - (y2 - y1) * (x1 - x3)) / denom;
        // if (ua >= 0.0f && ua <= 1.0f && ub >= 0.0f && ub <= 1.0f)
        {
            // Get the intersection point.
            return new SurveyPoint((x1 + ua * (x2 - x1)), (y1 + ua * (y2 - y1)));
        }

    }

    public HashMap<Integer, IntervalGradientItem> getGradientIntervalMap() {
        return gradientIntervals;
    }

    // Log each put call with associated var name
    private static class LogHashMap<K, V> extends HashMap<K, V> {
        private String _name = "LogHashMap";

        public void setName(String name) {
            _name = name;
        }

        @Override
        public V put(K key, V value) {
            StackTraceElement[] trace = (new Throwable()).getStackTrace();
            Log.d(TAG,
                    _name
                            + ": put("
                            + String.valueOf(key)
                            + ", "
                            + String.valueOf(value)
                            + ")"
                            + (trace.length > 1 ? "\n" + trace[1].toString()
                                    : ""));
            return super.put(key, value);
        }
    }

    public HashMap<Double, String> AnalyzeLongitudinalGradient(
            String gradientUid, double minRange, SurveyData survey) {

        //range filter complete - every point is bare minimum distances apart
        HashMap<Double, String> ProblemAreaMap = new LogHashMap<Double, String>();
        ((LogHashMap) ProblemAreaMap).setName("ProblemAreaMap");
        ((LogHashMap) gradientIntervals).setName("GradientIntervals");

        //we should at least find the point closest to the center...
        Cursor cur = gpc.GetGradientPoints(ATSKConstants.DEFAULT_GROUP,
                gradientUid, false);

        if (cur == null || cur.getCount() < 3) {
            for (double dist = 0; dist < AZLength_m; dist += minRange)
                ProblemAreaMap.put(dist, Constants.GRADIENT_MISSING_DATA);
            ProblemAreaMap.put(AZLength_m, Constants.GRADIENT_MISSING_DATA);
            if (cur != null)
                cur.close();
            Log.w(TAG, "CurrentGradientCursor " + (cur == null ? "null"
                    : "length < 3"));
            return ProblemAreaMap;
        }
        IDColumn = cur.getColumnIndex(DBURIConstants.COLUMN_ID);
        LatColumn = cur.getColumnIndex(DBURIConstants.COLUMN_LAT);
        LonColumn = cur.getColumnIndex(DBURIConstants.COLUMN_LON);
        AltColumn = cur.getColumnIndex(DBURIConstants.COLUMN_HAE_M);
        int RangeColumn = cur.getColumnIndex(DBURIConstants
                .COLUMN_RANGE_FROM_START);

        //start at the beginning...  every x meters ..
        RangeGradientsFromApproach(cur);

        // reusing, need to close the first one.
        cur.close();

        //2 jobs - is the angle ok and are the points close enough...
        cur = gpc.GetGradientPoints(ATSKConstants.DEFAULT_GROUP,
                gradientUid, true);
        if (cur == null)
            return ProblemAreaMap;

        // Average into 200 ft intervals
        double startInterval = -1, avgAlt = 0, pointCount = 0;
        List<RangeAltItem> intervals = new ArrayList<RangeAltItem>();
        while (cur.moveToNext()) {
            float range = cur.getFloat(RangeColumn);
            float alt = cur.getFloat(AltColumn);
            if (startInterval == -1)
                startInterval = minRange * Math.floor(range / minRange);
            if (range < startInterval + minRange) {
                avgAlt += alt;
                pointCount++;
            } else {
                intervals.add(new RangeAltItem(startInterval,
                        avgAlt / pointCount));
                avgAlt = pointCount = 0;
                startInterval = minRange * Math.floor(range / minRange);
            }
        }
        cur.close();
        if (intervals.isEmpty())
            return ProblemAreaMap;

        // Check for missing data before start of gradient
        int intervalIndex = 0;
        double startRange = 0;
        double firstRange = intervals.get(0).range_m;
        while (startRange < firstRange) {
            gradientIntervals.put(intervalIndex, new IntervalGradientItem(
                    intervalIndex, startRange, Float.MAX_VALUE));
            ProblemAreaMap.put(startRange, Constants.GRADIENT_MISSING_DATA);
            startRange += minRange;
            intervalIndex++;
        }

        // Check if any bad slopes (>1.5%) between 200 ft intervals
        RangeAltItem start = intervals.get(0);
        survey.edges.LongitudinalGradientInterval = 0;
        for (int i = 1; i < intervals.size(); i++) {
            RangeAltItem next = intervals.get(i);
            double slope = ((next.alt_m - start.alt_m) /
                    (next.range_m - start.range_m)) * 100;
            double elevAngle = 180f / Math.PI * Math.atan(slope / 100);

            // Check if slope is too steep and apply to all intervals between
            // previous and current
            double range = start.range_m;
            while (range < next.range_m) {
                if (Math.abs(slope) >= survey.edges.GradientThreshholdLZLonIntervalMax)
                    ProblemAreaMap.put(range, Constants.GRADIENT_STEEP_SEGMENT);
                intervalIndex = (int) (range / minRange);
                if (!gradientIntervals.containsKey(intervalIndex) || Math.abs(
                        gradientIntervals.get(intervalIndex).gradient_deg)
                        <= Math.abs(elevAngle))
                    gradientIntervals.put(intervalIndex,
                            new IntervalGradientItem(intervalIndex, range,
                                    elevAngle));
                range += minRange;
            }

            // Store worst slope
            if (Math.abs(slope) > Math.abs(survey.edges.
                    LongitudinalGradientInterval))
                survey.edges.LongitudinalGradientInterval = slope;

            start = next;
        }

        // Check for missing data after end of gradient
        double lastRange = intervals.get(intervals.size() - 1).range_m;
        while (lastRange + minRange < AZLength_m) {
            lastRange += minRange;
            intervalIndex = (int) (lastRange / minRange);
            gradientIntervals.put(intervalIndex, new IntervalGradientItem(
                    intervalIndex, lastRange, Float.MAX_VALUE));
            ProblemAreaMap.put(lastRange, Constants.GRADIENT_MISSING_DATA);
        }

        // Check if entire LZ slope is usable
        // Based on survey app/dep elevation, not gradient start/end
        survey.edges.LongitudinalGradientOverall =
                ((survey.departureElevation - survey.approachElevation) /
                AZLength_m) * 100;
        if (survey.edges.LongitudinalGradientOverall > survey.edges.GradientThreshholdLZLonOverallMax)
            ProblemAreaMap.put(AZLength_m, Constants.GRADIENT_STEEP_OVERALL);

        // Update longitudinal state
        GradientDBItem gradient = gpc.GetGradient(ATSKConstants.DEFAULT_GROUP,
                gradientUid, true);
        if (gradient != null) {
            gradient.setUid(gradientUid);
            gradient.setType(ProblemAreaMap.size() > 0 ?
                    ATSKConstants.GRADIENT_TYPE_LONGITUDINAL_BAD :
                    ATSKConstants.GRADIENT_TYPE_LONGITUDINAL_GOOD);
            gradient.setGroup(ATSKConstants.DEFAULT_GROUP);
            gradient.setLongitudinalIntervalGraident(
                    survey.edges.LongitudinalGradientInterval);
            gradient.setLongitudinalOverallGradient(
                    survey.edges.LongitudinalGradientOverall);
            gpc.GradientModify_NoPoints(gradient);
        }

        return ProblemAreaMap;
    }

    private void RangeGradientsFromApproach(Cursor CurrentGradientCursor) {
        double[] LZCenterApproachPoint = Conversions.AROffset(CenterLat,
                CenterLon, AZHeading_deg, -1 * (AZLength_m / 2.0f));

        double RangeFromCenter = AZLength_m;
        CenterOfGradientPoint = new SurveyPoint(LZCenterApproachPoint[0],
                LZCenterApproachPoint[1]);

        //calculate ranges from approach - maybe these numbers were already there?
        //LOU maybe these are always ranged and ordered already?
        for (CurrentGradientCursor.moveToFirst(); !CurrentGradientCursor
                .isAfterLast(); CurrentGradientCursor.moveToNext()) {
            double Lat = CurrentGradientCursor.getDouble(LatColumn);
            double Lon = CurrentGradientCursor.getDouble(LonColumn);
            double alt_m = CurrentGradientCursor.getDouble(AltColumn);
            int ID = CurrentGradientCursor.getInt(IDColumn);

            double[] RangeAngle = Conversions.CalculateRangeAngle(
                    LZCenterApproachPoint[0], LZCenterApproachPoint[1], Lat,
                    Lon);
            RangeAngle[1] = RangeAngle[1] - AZHeading_deg;
            if (RangeAngle[1] > 45 && RangeAngle[1] < 270)
                RangeAngle[0] *= -1;

            if (Math.abs(RangeAngle[0] - (AZLength_m / 2.0f)) < RangeFromCenter) {
                RangeFromCenter = (float) Math.abs(RangeAngle[0]
                        - (AZLength_m / 2.0f));
                CenterOfGradientPoint.lat = Lat;
                CenterOfGradientPoint.lon = Lon;
                CenterOfGradientPoint.setHAE(alt_m);
            }

            gpc.SetGradientAnalysisResults(ID, (float) (RangeAngle[0]),
                    "From Approach", "" + ID);
        }
        CurrentGradientCursor.close();
    }

    public LZEdges AnalyzeTransverseGradient(String GradientUID,
            SurveyData survey) {
        Cursor CurrentGradientCursor = gpc.GetGradientPoints(
                ATSKConstants.DEFAULT_GROUP, GradientUID, false);
        gpc.ClearGradientAnalysis("");//no survey name needed
        if (CurrentGradientCursor == null) {
            return survey.edges;
        }

        if (CurrentGradientCursor.getCount() < 3) {
            CurrentGradientCursor.close();
            return survey.edges;
        }
        IDColumn = CurrentGradientCursor
                .getColumnIndex(DBURIConstants.COLUMN_ID);
        LatColumn = CurrentGradientCursor
                .getColumnIndex(DBURIConstants.COLUMN_LAT);
        LonColumn = CurrentGradientCursor
                .getColumnIndex(DBURIConstants.COLUMN_LON);
        AltColumn = CurrentGradientCursor
                .getColumnIndex(DBURIConstants.COLUMN_HAE_M);
        //find the middle gradient - get height of middle gradient
        GetMiddleGradientPosition(CurrentGradientCursor);

        //calculate ranges from middle gradient
        CalcualteRangeFromCenter(CurrentGradientCursor, survey);

        //calculate slopes of each section
        CurrentGradientCursor.close();

        CurrentGradientCursor = gpc.GetGradientPoints(
                ATSKConstants.DEFAULT_GROUP, GradientUID, true);
        if (CurrentGradientCursor != null) {
            LZEdges CalculatedGradients = AnalyzeTransversePoints(
                    CurrentGradientCursor, survey.edges);
            CurrentGradientCursor.close();
            gpc.SetGradientEdges(ATSKConstants.DEFAULT_GROUP, GradientUID,
                    CalculatedGradients);
            TransverseCalculated = true;
            return CalculatedGradients;
        } else {
            return survey.edges;
        }
    }

    public double GetCenterHeightHAE() {
        if (CenterOfGradientPoint == null)
            return SurveyPoint.Altitude.INVALID;
        return CenterOfGradientPoint.getHAE();
    }

    public TransverseCalcHelper[] getMaintaineds() {
        if (TransverseCalculated)
            return new TransverseCalcHelper[] {
                    LMaintained, RMaintained
            };
        else
            return null;
    }

    public TransverseCalcHelper[] getGradeds() {
        if (TransverseCalculated)
            return new TransverseCalcHelper[] {
                    LGraded, RGraded
            };
        else
            return null;
    }

    public TransverseCalcHelper[] getShoulders() {
        if (TransverseCalculated)
            return new TransverseCalcHelper[] {
                    LShoulder, RShoulder
            };
        else
            return null;
    }

    public TransverseCalcHelper[] getAZs() {
        if (TransverseCalculated)
            return new TransverseCalcHelper[] {
                    LAZ, RAZ
            };
        else
            return null;
    }

    private LZEdges AnalyzeTransversePoints(final Cursor GradientCursor,
            final LZEdges edgeSizes) {

        int RangeColumn = GradientCursor
                .getColumnIndex(DBURIConstants.COLUMN_RANGE_FROM_START);

        double[] rangeLines = {
                -1
                        * (AZWidth_m / 2 + edgeSizes.ShoulderWidth_m
                                + edgeSizes.GradedAreaWidth_m + edgeSizes.MaintainedAreaWidth_m),
                -1
                        * (AZWidth_m / 2 + edgeSizes.ShoulderWidth_m + edgeSizes.GradedAreaWidth_m),
                -1 * (AZWidth_m / 2 + edgeSizes.ShoulderWidth_m),
                -1 * AZWidth_m / 2,
                0,
                AZWidth_m / 2,
                AZWidth_m / 2 + edgeSizes.ShoulderWidth_m,
                AZWidth_m / 2 + edgeSizes.ShoulderWidth_m
                        + edgeSizes.GradedAreaWidth_m,
                AZWidth_m / 2 + edgeSizes.ShoulderWidth_m
                        + edgeSizes.GradedAreaWidth_m
                        + edgeSizes.MaintainedAreaWidth_m
        };

        TransverseCalcHelper LOuter = new TransverseCalcHelper("L Outer"), ROuter = new TransverseCalcHelper(
                "R Outer");
        LOuter.reset(rangeLines[0] * 2, rangeLines[0]);
        for (int i = 0; i < _trHelpers.length; i++)
            _trHelpers[i].reset(rangeLines[i], rangeLines[i + 1]);
        ROuter.reset(rangeLines[8], rangeLines[8] * 2);

        TransverseCalcHelper[] calcs = new TransverseCalcHelper[] {
                LOuter, LMaintained, LGraded, LShoulder, LAZ,
                RAZ, RShoulder, RGraded, RMaintained, ROuter
        };

        //the gradient cursor is ordered by range -x -> 0->x
        for (GradientCursor.moveToFirst(); !GradientCursor.isAfterLast(); GradientCursor
                .moveToNext()) {
            // left is negative
            double range = GradientCursor.getFloat(RangeColumn);
            SurveyPoint sp = GradientDBItem
                    .cursorToSurveyPoint(GradientCursor);
            sp.speed = range;

            // Filter inside points
            for (TransverseCalcHelper c : calcs) {
                if (c.contains(sp))
                    c.addInside(sp);
            }
        }
        //GradientCursor is closed outside the method call

        // Filter any missing points
        if (RAZ.getInsidePoint() == null)
            RAZ.addInside(LAZ.getInsidePoint(), true);
        else
            LAZ.addInside(RAZ.getInsidePoint(), true);
        for (TransverseCalcHelper c : _trHelpers)
            c.filter(calcs);

        // Check if we have enough data
        LZEdges GradientHolder = new LZEdges(edgeSizes);
        /*for (TransverseCalcHelper c : _trHelpers) {
            if (!c.isValid()) {
                if (c == LAZ || c == RAZ || c.width > 0.5) {
                    Log.w(TAG, "Invalid " + c.name
                            + " " + c.getInvalid()
                            + ". Stopping transverse analysis.");
                    return GradientHolder;
                }
            }
        }*/

        // Mark as invalid only if both middle points are missing
        if (!LAZ.isValid() && !RAZ.isValid()) {
            Log.w(TAG, "Missing both middle transverse gradients ("
                    + LAZ.getInvalid() + ", " + RAZ.getInvalid()
                    + "). Stopping transverse analysis.");
            return GradientHolder;
        }

        if (RMaintained.isValid()) {
            double[] RMaintainedOffset = Conversions
                    .CalculateRangeAngleElev(
                            RMaintained.getOutsidePoint(),
                            RMaintained.getInsidePoint());
            GradientHolder.RightMaintainedAreaGradient = GetGradient(RMaintainedOffset);
        }
        if (RGraded.isValid()) {
            double[] RMGradedOffset = Conversions.CalculateRangeAngleElev(
                    RGraded.getOutsidePoint(), RGraded.getInsidePoint());
            GradientHolder.RightGradedAreaGradient = GetGradient(RMGradedOffset);
        }
        if (RShoulder.isValid()) {
            double[] RShoulderOffset = Conversions.CalculateRangeAngleElev(
                    RShoulder.getOutsidePoint(), RShoulder.getInsidePoint());
            GradientHolder.RightShoulderGradient = GetGradient(RShoulderOffset);
        }
        if (RAZ.isValid()) {
            double[] RAZOffset = Conversions.CalculateRangeAngleElev(
                    RAZ.getOutsidePoint(), RAZ.getInsidePoint());
            GradientHolder.RightHalfRunwayGradient = GetGradient(RAZOffset);
        }
        if (LMaintained.isValid()) {
            double[] LMaintainedOffset = Conversions
                    .CalculateRangeAngleElev(
                            LMaintained.getOutsidePoint(),
                            LMaintained.getInsidePoint());
            GradientHolder.LeftMaintainedAreaGradient = GetGradient(LMaintainedOffset);
        }

        if (LGraded.isValid()) {
            double[] LMGradedOffset = Conversions.CalculateRangeAngleElev(
                    LGraded.getOutsidePoint(), LGraded.getInsidePoint());
            GradientHolder.LeftGradedAreaGradient = GetGradient(LMGradedOffset);
        }

        if (LShoulder.isValid()) {
            double[] LShoulderOffset = Conversions.CalculateRangeAngleElev(
                    LShoulder.getOutsidePoint(), LShoulder.getInsidePoint());
            GradientHolder.LeftShoulderGradient = GetGradient(LShoulderOffset);
        }
        if (LAZ.isValid()) {
            double[] LAZOffset = Conversions.CalculateRangeAngleElev(
                    LAZ.getOutsidePoint(), LAZ.getInsidePoint());
            GradientHolder.LeftHalfRunwayGradient = GetGradient(LAZOffset);
        }
        //save to gradient cursor
        //build edges?
        return GradientHolder;
    }

    private static double GetGradient(double[] RAEOffset) {
        if (RAEOffset.length != 3)
            return -100;
        return -RAEOffset[2];
    }

    private void CalcualteRangeFromCenter(Cursor GradientCursor,
            SurveyData survey) {
        double minRange = Double.MAX_VALUE;
        int IndexColumn = GradientCursor
                .getColumnIndex(DBURIConstants.COLUMN_INDEX);

        for (GradientCursor.moveToFirst(); !GradientCursor.isAfterLast(); GradientCursor
                .moveToNext()) {
            double min_alt_m_hae = 0;
            int index = GradientCursor.getInt(IndexColumn);
            double lat = GradientCursor.getDouble(LatColumn);
            double lon = GradientCursor.getDouble(LonColumn);
            double alt_m = GradientCursor.getDouble(AltColumn);
            int ID = GradientCursor.getInt(IDColumn);

            // Get distance from survey center-line
            SurveyPoint app = AZHelper.CalculateAnchorFromAZCenter(survey,
                    survey.center, survey.getApproachAnchor());
            double[] ra = Conversions.CalculateRangeAngle(
                    app.lat, app.lon, lat, lon);

            // Negative range is fine since we need to know left/right
            ra[0] *= Math.sin(Math.toRadians(
                    Conversions.deg360(ra[1] - survey.angle)));

            /*Log.d(TAG, "Gradient[" + index + "]: " + ra[0]
                    + " from center (angle diff = " + (ra[1] - survey.angle) + ")");*/

            // Store closest center elevation
            if (Math.abs(ra[0]) < minRange) {
                min_alt_m_hae = alt_m;
                minRange = Math.abs(ra[0]);
            }
            //    Log.d(TAG, "RAnge "+ RangeAngle[0]+" Angle:"+RangeAngle[1]);
            gpc.SetGradientAnalysisResults(ID, (float) ra[0],
                    "From Center", "" + index);
            if (!CenterOfGradientPoint.alt.isValid())
                CenterOfGradientPoint.setHAE(min_alt_m_hae);
        }

    }

    private int GetMiddleGradientPosition(Cursor GradientCursor) {//returns cursor position of middle point
        int MinRangeIndex = -1;
        double MinRange = Float.MAX_VALUE;
        double MinHeightAlt_m = 0;

        for (GradientCursor.moveToFirst(); !GradientCursor.isAfterLast(); GradientCursor
                .moveToNext()) {
            double Lat = GradientCursor.getDouble(LatColumn);
            double Lon = GradientCursor.getDouble(LonColumn);
            double alt_m = GradientCursor.getDouble(AltColumn);
            double Range = Conversions.CalculateRangem(CenterLat, CenterLon,
                    Lat, Lon);
            /*Log.d(TAG,
                    String.format("Position:%d,  at %f %f",
                            GradientCursor.getPosition(), Lat, Lon));*/
            if (Range < MinRange) {
                MinHeightAlt_m = (float) alt_m;
                MinRangeIndex = GradientCursor.getPosition();
                MinRange = (float) Range;
            }
        }

        //we have a point closest to center now...
        //find intersection
        GradientCursor.moveToPosition(MinRangeIndex);
        double GradientLat = GradientCursor.getDouble(LatColumn);
        double GradientLon = GradientCursor.getDouble(LonColumn);

        double[] AboveCenter = Conversions.AROffset(CenterLat, CenterLon,
                AZHeading_deg, 10000);
        double[] BelowCenter = Conversions.AROffset(CenterLat, CenterLon,
                AZHeading_deg + 180, 10000);

        double[] GradientPointRight = Conversions.AROffset(GradientLat,
                GradientLon, AZHeading_deg + 90, 1000);
        double[] GradientPointLeft = Conversions.AROffset(GradientLat,
                GradientLon, AZHeading_deg + 270, 1000);
        CenterOfGradientPoint = lineIntersect(AboveCenter[0], AboveCenter[1],
                BelowCenter[0], BelowCenter[1], GradientPointRight[0],
                GradientPointRight[1], GradientPointLeft[0],
                GradientPointLeft[1]);
        if (CenterOfGradientPoint != null
                && !CenterOfGradientPoint.alt.isValid())
            CenterOfGradientPoint.setHAE(MinHeightAlt_m);

        return MinRangeIndex;
    }

    public SurveyPoint getCenterPoint() {
        return CenterOfGradientPoint;
    }

    private static class RangeAltItem {
        public double range_m, alt_m;

        RangeAltItem(double range_m, double alt_m) {
            this.range_m = range_m;
            this.alt_m = alt_m;
        }

        public void set(RangeAltItem next) {
            this.range_m = next.range_m;
            this.alt_m = next.alt_m;
        }
    }

    static public class IntervalGradientItem {
        public final double DistanceFromApproach_m;
        public final double gradient_deg;
        public final int IntervalIndex;

        public IntervalGradientItem(int Index, double Distance_m,
                double Gradient_deg) {
            this.DistanceFromApproach_m = Distance_m;
            this.gradient_deg = Gradient_deg;
            this.IntervalIndex = Index;
        }

        public String toString() {
            String result = String.format("%d %.1f", IntervalIndex,
                    gradient_deg);
            return result;
        }
    }

    public static class TransverseCalcHelper {
        private List<SurveyPoint> outPoints = new ArrayList<SurveyPoint>();
        private List<SurveyPoint> inPoints = new ArrayList<SurveyPoint>();
        public String name;
        public double width, minRange, maxRange;
        private SurveyPoint outAvg, inAvg;
        public boolean middle = false;

        // Sort survey points by altitude
        public static final Comparator<SurveyPoint> ALT_COMP = new Comparator<SurveyPoint>() {
            @Override
            public int compare(SurveyPoint lhs, SurveyPoint rhs) {
                return Double.compare(lhs.getHAE(), rhs.getHAE());
            }
        };

        public TransverseCalcHelper(String name, boolean middle) {
            this.name = name;
            this.middle = middle;
        }

        public TransverseCalcHelper(String name) {
            this(name, false);
        }

        /**
         * Reset calc helper with the supplied parameters
         * @param minRange Minimum range
         * @param maxRange Maximum range
         */
        public void reset(double minRange, double maxRange) {
            this.outPoints.clear();
            this.outAvg = null;
            this.inPoints.clear();
            this.inAvg = null;
            this.minRange = minRange;
            this.maxRange = maxRange;
            this.width = maxRange - minRange;
        }

        /**
         * Find most accurate inside/outside points from other helpers
         * This function should be called directly after populating inner points
         * @param calc Other calc helpers to read inside points from
         */
        public void filter(TransverseCalcHelper[] calc) {
            if (isValid())
                return;
            List<SurveyPoint> outs = new ArrayList<SurveyPoint>();
            List<SurveyPoint> ins = new ArrayList<SurveyPoint>();
            for (TransverseCalcHelper c : calc) {
                if (this == c)
                    continue;
                SurveyPoint cin = c.getInsidePoint();
                if (cin == null || (!c.contains(cin) && !c.middle))
                    continue;
                if (c.minRange < this.minRange) {
                    if (this.minRange < 0)
                        outs.add(cin);
                    else if (c.minRange >= 0)
                        ins.add(cin);
                }
                if (c.maxRange > this.maxRange) {
                    if (this.minRange < 0)
                        ins.add(cin);
                    else if (c.minRange >= 0)
                        outs.add(cin);
                }
            }
            final double midRange = (this.minRange + this.maxRange) / 2;
            final Comparator<SurveyPoint> distComp = new Comparator<SurveyPoint>() {
                @Override
                public int compare(SurveyPoint lhs, SurveyPoint rhs) {
                    return Double.compare(Math.abs(lhs.speed - midRange),
                            Math.abs(rhs.speed - midRange));
                }
            };
            if (inPoints.isEmpty() && !ins.isEmpty()) {
                Collections.sort(ins, distComp);
                addInside(ins.get(0));
            }
            if (outPoints.isEmpty() && !outs.isEmpty()) {
                Collections.sort(outs, distComp);
                addOutside(outs.get(0));
            }
        }

        /**
         * Compare point range with boundaries
         * @param sp Survey point w/ range field as "speed"
         * @return -1 if less than min, 1 if greater than max, 0 if inside
         */
        public boolean contains(SurveyPoint sp) {
            return sp.speed >= minRange && sp.speed <= maxRange;
        }

        public void addInside(SurveyPoint point, boolean emptyOnly) {
            if (!emptyOnly || this.inPoints.isEmpty()) {
                this.inPoints.add(new SurveyPoint(point));
                this.inAvg = null;
            }
        }

        public void addInside(SurveyPoint point) {
            addInside(point, false);
        }

        public void addOutside(SurveyPoint point, boolean emptyOnly) {
            if (!emptyOnly || this.outPoints.isEmpty()) {
                this.outPoints.add(new SurveyPoint(point));
                this.outAvg = null;
            }
        }

        public void addOutside(SurveyPoint point) {
            addOutside(point, false);
        }

        public SurveyPoint getInsidePoint() {
            if (this.inAvg == null)
                this.inAvg = calcBestPoint(inPoints);
            return this.inAvg;
        }

        public SurveyPoint getOutsidePoint() {
            if (this.outAvg == null)
                this.outAvg = calcBestPoint(outPoints);
            return this.outAvg;
        }

        private static SurveyPoint calcBestPoint(List<SurveyPoint> points) {
            int pointCount = points.size();
            if (pointCount == 0)
                return null;
            else if (pointCount == 1)
                return points.get(0);
            Collections.sort(points, ALT_COMP);

            // First get average and median altitude
            double altMean = 0.0, altMedian = 0;
            for (int i = 0; i < pointCount; i++) {
                double alt = points.get(i).getHAE();
                altMean += alt;
                if (i == pointCount / 2) {
                    if (pointCount % 2 == 0)
                        altMedian = (alt + points.get(i - 1).getHAE()) / 2.0;
                    else
                        altMedian = alt;
                }
            }
            altMean /= pointCount;

            // Get standard deviation
            double altSum = 0;
            for (SurveyPoint sp : points)
                altSum += Math.pow(sp.getHAE() - altMean, 2);
            double stdev = Math.sqrt(altSum / pointCount);

            // Now average all elevation points within 2 standard deviations (95.45%)
            double minAlt = altMedian - stdev * 2, maxAlt = altMedian + stdev
                    * 2;
            double latMean = 0.0, lonMean = 0.0, rMean = 0.0;
            altMean = 0.0;
            pointCount = 0;
            for (SurveyPoint sp : points) {
                if (sp.getHAE() >= minAlt && sp.getHAE() <= maxAlt) {
                    latMean += sp.lat;
                    lonMean += sp.lon;
                    altMean += sp.getHAE();
                    rMean += sp.speed;
                    pointCount++;
                }
            }
            latMean /= pointCount;
            lonMean /= pointCount;
            altMean /= pointCount;
            rMean /= pointCount;

            SurveyPoint spMean = new SurveyPoint(latMean, lonMean);
            spMean.setHAE(altMean);
            spMean.speed = rMean;
            return spMean;
        }

        public boolean isValid() {
            return !(outPoints.isEmpty() || inPoints.isEmpty());
        }

        public String getInvalid() {
            if (outPoints.isEmpty())
                return "Outside Point";
            if (inPoints.isEmpty())
                return "Inside Point";
            return "None";
        }
    }

}
