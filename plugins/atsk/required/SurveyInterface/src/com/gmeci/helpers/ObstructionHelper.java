
package com.gmeci.helpers;

import android.database.Cursor;

import com.gmeci.constants.Constants;
import com.gmeci.core.ATSKConstants;
import com.gmeci.core.LineObstruction;
import com.gmeci.core.PointObstruction;
import com.gmeci.core.SurveyData;
import com.gmeci.core.SurveyData.AZ_TYPE;
import com.gmeci.core.SurveyPoint;
import com.gmeci.atskservice.resolvers.DBURIConstants;
import com.gmeci.atskservice.resolvers.ObstructionProviderClient;
import com.gmeci.conversions.Conversions;
import com.gmeci.vehicle.VehicleBlock;
import com.sromku.polygon.Line;
import com.sromku.polygon.PolyPoint;
import com.sromku.polygon.Polygon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ObstructionHelper {

    private static final double OBSTRUCTION_LIMIT_m = 1000;

    final public static char DEGREE = '\u00B0';

    public static String GetLineObstructionRemarkString(
            double height_m, String desc, String type,
            double[] rangeAngle, double clRange) {
        rangeAngle[1] = Conversions.deg360(rangeAngle[1]);

        // Ignore height on certain line types
        if (!(Constants.isTaxiway(type)
        || (Math.abs(height_m) <= Conversions.THRESH
        && Constants.isFlatTerrain(type))))
            desc = String.format("%.1fft %s",
                    height_m * Conversions.M2F, desc);

        // Build description
        String remark = String.format("%s (%.0fm)(%.0fft) @%.0f"
                + DEGREE + " from centerpoint",
                desc, rangeAngle[0], rangeAngle[0] * Conversions.M2F,
                rangeAngle[1]);
        if (!Double.isNaN(clRange))
            remark += String.format(" [(%.0fm)(%.0fft) from centerline]",
                    clRange, clRange * Conversions.M2F);
        return remark;
    }

    public static String GetObstructionRemarkString(PointObstruction po,
            double[] centerRAB, double clRange) {

        String desc = po.remark;
        if (desc == null)
            desc = "";
        centerRAB[1] = Conversions.deg360(centerRAB[1]);

        String remark, type = po.type;
        if (po.group != null && po.group.equals(ATSKConstants.DISTRESS_GROUP)) {
            type = type.replace("_2", " severe");
            type = type.replace("_1", " moderate");
            type = type.replace("_0", " mild");
            desc = desc.replace("_2", " severe");
            desc = desc.replace("_1", " moderate");
            desc = desc.replace("_0", " mild");
        }
        if (!desc.contains(type))
            desc = type + (desc.isEmpty() ? "" : ": " + desc);

        double heightFt = po.height * Conversions.M2F;
        remark = String.format("%.1fft %s %s (%.0fm)(%.0fft) @%.0f"
                + DEGREE + " from centerpoint",
                Math.abs(heightFt), (heightFt < 0 ? " deep" : ""),
                desc, centerRAB[0], centerRAB[0] * Conversions.M2F,
                centerRAB[1]);
        if (!Double.isNaN(clRange)) {
            remark += String.format(" [(%.0fm)(%.0fft) from centerline]",
                    clRange, clRange * Conversions.M2F);
        }
        return remark;
    }

    public static String GetObstructionRemarkString(
            PointObstruction po, double[] centerRAB) {
        return GetObstructionRemarkString(po, centerRAB, Double.NaN);
    }

    public static Polygon buildPolygon(List<SurveyPoint> corners) {
        if (corners == null || corners.isEmpty())
            return null;
        Polygon.Builder builder = Polygon.Builder();
        for (SurveyPoint sp : corners)
            builder.addVertex(polyPoint(sp));
        return builder.build();
    }

    public static PolyPoint polyPoint(SurveyPoint sp) {
        return new PolyPoint(sp.lat, sp.lon);
    }

    public static List<String> GetFilteredPointObstructionStrings(
            ObstructionProviderClient opc, List<PointObstruction> points,
            SurveyData survey, List<SurveyPoint> cornersIn,
            List<SurveyPoint> cornersOut) {

        List<String> remarks = new ArrayList<String>();

        if (points == null || points.isEmpty())
            return remarks;

        Polygon polyIn = buildPolygon(cornersIn);
        Polygon polyOut = buildPolygon(cornersOut);
        boolean inside = polyOut == null;

        for (PointObstruction po : points) {
            // Vehicles are counted as line obstructions
            if (po.group != null
                    && po.group.equals(ATSKConstants.VEHICLE_GROUP))
                continue;

            if (po.type.equals(Constants.PO_LABEL)) {
                List<LineObstruction> leaders = opc.getLinesWithPrefix(
                        po.group, po.uid + ATSKConstants.LEADER_SUFFIX, true);
                if (leaders != null) {
                    List<PointObstruction> leaderPoints = getLeaderPoints(po,
                            leaders);
                    remarks.addAll(GetFilteredPointObstructionStrings(
                            opc, leaderPoints, survey, cornersIn, cornersOut));
                }
                // Don't count label as obstruction
                continue;
            }

            PolyPoint pp = new PolyPoint(po.lat, po.lon);

            if (!inside && polyOut.contains(pp)
                            && !polyIn.contains(pp)
                            || inside && polyIn.contains(pp)) {
                double[] centerRAB = Conversions.CalculateRangeAngle(
                        survey.center.lat, survey.center.lon, po.lat, po.lon);
                double perpAngle = Conversions.deg360(centerRAB[1]
                        - survey.angle);
                double perpDist = Math.abs(centerRAB[0]
                        * Math.sin(Math.toRadians(perpAngle)));
                if (survey.circularAZ || survey.getType() == AZ_TYPE.HLZ
                        || survey.getType() == AZ_TYPE.LZ && survey.surveyIsSTOL())
                    perpDist = Double.NaN;
                String ObstructionRemark = GetObstructionRemarkString(
                        po, centerRAB, perpDist);
                remarks.add(ObstructionRemark);
            }
        }

        return remarks;
    }

    public static List<String> GetFilteredPointObstructionStrings(
            ObstructionProviderClient opc, Cursor filteredCursor,
            SurveyData survey, List<SurveyPoint> cornersIn,
            List<SurveyPoint> cornersOut) {
        return GetFilteredPointObstructionStrings(opc, cursorToPointList(
                filteredCursor), survey, cornersIn, cornersOut);
    }

    public static List<LineObstruction> getAllLines(
            ObstructionProviderClient opc, boolean withPoints) {
        List<LineObstruction> lineList = new ArrayList<LineObstruction>();
        final Cursor lines = opc.GetAllLines(ATSKConstants.DEFAULT_GROUP);
        if (lines != null) {
            for (lines.moveToFirst(); lines.getCount() > 0
                    && !lines.isAfterLast(); lines.moveToNext()) {
                LineObstruction lo = opc.GetLineObstruction(lines, withPoints);
                if (lo != null)
                    lineList.add(lo);
            }
            lines.close();
        }
        // Include vehicle obstructions here
        final Cursor vehCur = opc
                .GetAllPointsCursor(ATSKConstants.VEHICLE_GROUP);
        if (vehCur != null) {
            for (vehCur.moveToFirst(); vehCur.getCount() > 0
                    && !vehCur.isAfterLast(); vehCur.moveToNext()) {
                PointObstruction veh = opc.GetPointObstruction(vehCur);
                if (veh != null) {
                    LineObstruction lo = VehicleBlock.buildLineObstruction(
                            veh.type, veh);
                    lo.uid = veh.uid;
                    lo.type = veh.type;
                    lo.group = veh.group;
                    lo.remarks = veh.remark;
                    lo.height = veh.height;
                    lo.closed = lo.filled = true;
                    lineList.add(lo);
                }
            }
            vehCur.close();
        }
        return lineList;
    }

    public static List<PointObstruction> getLeaderPoints(
            PointObstruction po, List<LineObstruction> leaders) {
        // Use leader arrow points instead
        List<PointObstruction> leaderPoints = new ArrayList<PointObstruction>();
        // Filter first point in each leader
        for (LineObstruction lo : leaders) {
            if (lo == null || lo.points == null || lo.points.isEmpty())
                continue;
            PointObstruction lp = new PointObstruction(lo.points.get(0));
            lp.group = lo.group;
            lp.uid = lo.uid;
            lp.type = "";
            lp.height = lo.height;
            lp.width = po.width;
            lp.remark = (po.remark == null || po.remark.isEmpty()
                    ? po.type : po.remark);
            leaderPoints.add(lp);
        }
        return leaderPoints;
    }

    private static boolean remarksEmpty(LineObstruction lo) {
        return lo == null || lo.remarks == null
                || lo.remarks.isEmpty()
                || lo.remarks.equals("none");
    }

    /**
     * Filter lines for all survey types (except extra LZ polygons)
     * @param opc Activated obstruction provider client
     * @param survey Survey data
     * @param inside True to filter inside objects, false if outside only
     * @return List of filtered item remarks
     */
    public static List<String> GetFilteredLineObstructionStrings(
            ObstructionProviderClient opc, SurveyData survey, boolean inside) {

        List<String> ObstructionRemarks = new ArrayList<String>();

        // Instead of passing cursors around, retrieve data here
        List<LineObstruction> lines = getAllLines(opc, true);

        if (lines.isEmpty())
            return ObstructionRemarks;

        double radius = survey.getRadius();
        double radius_outer = radius + OBSTRUCTION_LIMIT_m;

        List<SurveyPoint> cornersIn = getAZCorners(survey, true);
        List<SurveyPoint> cornersOut = getAZCorners(survey, false);
        for (LineObstruction lo : lines) {
            String desc = lo.type;
            if (desc.equals(Constants.LO_LEADER))
                continue;
            if (!remarksEmpty(lo))
                desc += ": " + lo.remarks;

            List<SurveyPoint> hits = new ArrayList<SurveyPoint>();
            if (!inside) {
                // Filter out points that are only inside
                List<SurveyPoint> outHits = (survey.circularAZ
                        ? lineHitTest(lo, survey.center, radius_outer)
                        : lineHitTest(lo, cornersOut));

                if (survey.circularAZ) {
                    for (SurveyPoint sp : outHits) {
                        double range = Conversions.CalculateRangem(
                                survey.center.lat, survey.center.lon,
                                sp.lat, sp.lon);
                        if (range >= radius)
                            hits.add(sp);
                    }
                } else {
                    Polygon polyIn = buildPolygon(cornersIn);
                    for (SurveyPoint sp : outHits) {
                        if (!polyIn.contains(polyPoint(sp)))
                            hits.add(sp);
                    }
                }
            } else
                hits = (survey.circularAZ
                        ? lineHitTest(lo, survey.center, radius)
                        : lineHitTest(lo, cornersIn));

            if (!hits.isEmpty()) {
                // Sort line points by distance from center
                final SurveyPoint center = survey.center;
                Collections.sort(hits, new Comparator<SurveyPoint>() {
                    @Override
                    public int compare(SurveyPoint lhs, SurveyPoint rhs) {
                        return Double.compare(
                                Conversions.CalculateRangem(
                                        center.lat, center.lon, lhs.lat,
                                        lhs.lon),
                                Conversions.CalculateRangem(
                                        center.lat, center.lon, rhs.lat,
                                        rhs.lon));
                    }
                });
                // Show closest
                double[] centerRAB = Conversions.CalculateRangeAngle(
                        survey.center.lat, survey.center.lon,
                        hits.get(0).lat, hits.get(0).lon);
                double perpAngle = Conversions.deg360(centerRAB[1]
                        - survey.angle);
                double perpDist = Math.abs(centerRAB[0]
                        * Math.sin(Math.toRadians(perpAngle)));
                String ObstructionRemark = GetLineObstructionRemarkString(
                        lo.height, desc, lo.type, centerRAB, survey.circularAZ
                                || survey.getType() == AZ_TYPE.HLZ
                                || survey.getType() == AZ_TYPE.LZ && survey.surveyIsSTOL()
                                ? Double.NaN : perpDist);
                ObstructionRemarks.add(ObstructionRemark);
            }
        }

        return ObstructionRemarks;
    }

    /**
     * Filter line obstructions (LZ parts only)
     * @param opc Activated obstruction provider clieny
     * @param survey LZ survey
     * @param corners LZ area corners (graded, maintained, approach, etc.)
     * @return List of obstruction remarks
     */
    public static List<String> GetFilteredLineObstructionStrings(
            ObstructionProviderClient opc, SurveyData survey,
            List<SurveyPoint> corners) {

        List<String> ObstructionRemarks = new ArrayList<String>();

        // Instead of passing cursors around, retrieve data here
        List<LineObstruction> lines = getAllLines(opc, true);

        if (lines.isEmpty())
            return ObstructionRemarks;

        for (LineObstruction lo : lines) {
            String desc = lo.type;
            if (desc.equals(Constants.LO_LEADER))
                continue;
            if (!remarksEmpty(lo))
                desc += ":" + lo.remarks;

            List<SurveyPoint> hits = lineHitTest(lo, corners);
            if (!hits.isEmpty()) {
                // Add first point
                double[] centerRAB = Conversions.CalculateRangeAngle(
                        survey.center.lat, survey.center.lon,
                        hits.get(0).lat, hits.get(0).lon);
                double perpAngle = Conversions.deg360(centerRAB[1]
                        - survey.angle);
                double perpDist = Math.abs(centerRAB[0]
                        * Math.sin(Math.toRadians(perpAngle)));
                String ObstructionRemark = GetLineObstructionRemarkString(
                        lo.height, desc, lo.type, centerRAB, survey.circularAZ
                                || survey.getType() == AZ_TYPE.HLZ
                                || survey.getType() == AZ_TYPE.STOL
                                ? Double.NaN : perpDist);
                ObstructionRemarks.add(ObstructionRemark);
            }
        }

        return ObstructionRemarks;
    }

    public static List<String> GetApproachFilteredLineStrings(
            ObstructionProviderClient opc, SurveyData survey) {

        List<SurveyPoint> corners = AZHelper
                .getInnerApproachCorners(survey, false);
        return GetFilteredLineObstructionStrings(opc, survey, corners);
    }

    public static List<String> GetApproachFilteredObstructionStrings(
            ObstructionProviderClient opc, Cursor filteredCursor,
            SurveyData survey) {

        List<SurveyPoint> corners = AZHelper.getInnerApproachCorners(
                survey, false);
        return GetFilteredPointObstructionStrings(
                opc, filteredCursor, survey, corners, null);
    }

    public static List<String> GetAZFilteredObstructionStrings(
            ObstructionProviderClient opc, Cursor filteredCursor,
            SurveyData survey, boolean inside) {
        ArrayList<String> ObstructionRemarks = new ArrayList<String>();
        if (filteredCursor == null || filteredCursor.getCount() < 1)
            return ObstructionRemarks;

        if (survey.circularAZ) {
            for (filteredCursor.moveToFirst(); !filteredCursor.isAfterLast(); filteredCursor
                    .moveToNext()) {
                PointObstruction po = cursorToPointObstruction(filteredCursor);
                // Vehicles are counted as line obstructions during filtering
                if (po.group != null
                        && po.group.equals(ATSKConstants.VEHICLE_GROUP))
                    continue;
                double[] RangeAngle = Conversions.CalculateRangeAngle(
                        survey.center.lat,
                        survey.center.lon,
                        po.lat, po.lon);
                if (RangeAngle[0] < survey.getRadius() && inside) { //good - add to list
                    String ObstructionRemark = GetObstructionRemarkString(
                            po, RangeAngle);
                    ObstructionRemarks.add(ObstructionRemark);
                } else if (!inside
                        && RangeAngle[0] >= survey.getRadius()
                        && RangeAngle[0] < survey.getRadius()
                                + OBSTRUCTION_LIMIT_m) { //good - add to list
                    String ObstructionRemark = GetObstructionRemarkString(
                            po, RangeAngle);
                    ObstructionRemarks.add(ObstructionRemark);
                }
            }
            return ObstructionRemarks;
        } else {
            return GetFilteredPointObstructionStrings(opc,
                    filteredCursor, survey,
                    getAZCorners(survey, true),
                    inside ? null : getAZCorners(survey, false));
        }
    }

    public static Cursor GetFilteredPointCursor(ObstructionProviderClient opc,
            List<SurveyPoint> corners, String GroupFilter) {
        double TopLat = -90, BottomLat = 90, LeftLon = -180, RightLon = 180;

        for (int i = 0; i < 4; i++) {
            if (corners.get(i).lat > TopLat)
                TopLat = corners.get(i).lat;
            else if (corners.get(i).lat < BottomLat)
                BottomLat = corners.get(i).lat;
            else if (corners.get(i).lon > RightLon)
                RightLon = corners.get(i).lon;
            else if (corners.get(i).lon < LeftLon)
                LeftLon = corners.get(i).lon;
        }
        return opc.GetAllPointsBounded(TopLat, BottomLat, LeftLon, RightLon,
                GroupFilter);
    }

    public static Cursor GetDistressFilteredPointCursor(
            ObstructionProviderClient opc, SurveyData FilteringSurvey) {
        ArrayList<SurveyPoint> approachCorners = AZHelper
                .getAZCorners(FilteringSurvey);
        return GetDistressFilteredPointCursor(opc, approachCorners);
    }

    public static ArrayList<String> GetDistressStrings(
            SurveyData FilteringSurvey, Cursor filteredCursor) {
        ArrayList<SurveyPoint> corners = AZHelper.getAZCorners(FilteringSurvey);

        ArrayList<String> ObstructionRemarks = new ArrayList<String>();
        if (filteredCursor == null || filteredCursor.getCount() < 1)
            return ObstructionRemarks;
        int LatColumn = filteredCursor
                .getColumnIndex(DBURIConstants.COLUMN_LAT);
        int LonColumn = filteredCursor
                .getColumnIndex(DBURIConstants.COLUMN_LON);
        int DescriptionColumn = filteredCursor
                .getColumnIndex(DBURIConstants.COLUMN_DESCRIPTION);
        int TypeColumn = filteredCursor
                .getColumnIndex(DBURIConstants.COLUMN_TYPE);
        int GroupColumn = filteredCursor
                .getColumnIndex(DBURIConstants.COLUMN_GROUP_NAME_POINT);

        Polygon AZPolygon = buildPolygon(corners);

        SurveyPoint CenterOfApproach = AZHelper.CalculateCenterOfEdge(
                FilteringSurvey, true);

        for (filteredCursor.moveToFirst(); !filteredCursor.isAfterLast(); filteredCursor
                .moveToNext()) {
            double NewPointLat = filteredCursor.getDouble(LatColumn), NewPointLon = filteredCursor
                    .getDouble(LonColumn);
            if (AZPolygon.contains(new PolyPoint(NewPointLat, NewPointLon))) {
                String Type = filteredCursor.getString(TypeColumn);
                String Group = filteredCursor.getString(GroupColumn);
                String Description = filteredCursor
                        .getString(DescriptionColumn);
                if (Group.equals(ATSKConstants.DISTRESS_GROUP)) {
                    Type = Type.replace("_2", " severe");
                    Type = Type.replace("_1", " moderate");
                    Type = Type.replace("_0", " mild");

                    Description = Description.replace("_2", " severe");
                    Description = Description.replace("_1", " moderate");
                    Description = Description.replace("_0", " mild");
                }
                double[] RangeAngle = Conversions.CalculateRangeAngle(
                        CenterOfApproach.lat, CenterOfApproach.lon,
                        filteredCursor.getDouble(LatColumn),
                        filteredCursor.getDouble(LonColumn));
                String ObstructionRemark = String.format(
                        "%s -%s %.0fft From Approach", Type, Description,
                        RangeAngle[0] * Conversions.M2F);
                ObstructionRemarks.add(ObstructionRemark);
            }
        }

        return ObstructionRemarks;
    }

    private static Cursor GetDistressFilteredPointCursor(
            ObstructionProviderClient opc,
            ArrayList<SurveyPoint> approachCorners) {
        return GetFilteredPointCursor(opc, approachCorners,
                ATSKConstants.DISTRESS_GROUP);
    }

    public static Cursor GetApproachFilteredPointCursor(
            ObstructionProviderClient opc, SurveyData FilteringSurvey) {
        ArrayList<SurveyPoint> approachCorners = AZHelper
                .getInnerApproachCorners(FilteringSurvey, false);
        return GetFilteredPointCursor(opc, approachCorners, null);
    }

    public static Cursor GetAZFilteredPointCursor(SurveyData survey,
            ObstructionProviderClient opc, boolean FilterForInside) {
        if (FilterForInside) {
            double TopLat = -90, BottomLat = 90, LeftLon = -180, RightLon = 180;
            if (survey.circularAZ) {
                for (int i = 0; i < 4; i++) {
                    double[] Edge = Conversions.AROffset(
                            survey.center.lat,
                            survey.center.lon, 90 * i,
                            survey.getRadius());
                    if (i == 0)
                        TopLat = Edge[0];
                    else if (i == 1)
                        RightLon = Edge[1];
                    else if (i == 2)
                        BottomLat = Edge[0];
                    else if (i == 3)
                        LeftLon = Edge[1];
                }
                return opc.GetAllPointsBounded(TopLat, BottomLat, LeftLon,
                        RightLon, null);
            } else {
                ArrayList<SurveyPoint> corners;
                corners = AZHelper.getAZCorners(survey);
                return GetFilteredPointCursor(opc, corners, null);
            }
        } else {//outside
            ArrayList<SurveyPoint> corners;
            double TopLat = -90, BottomLat = 90, LeftLon = -180, RightLon = 180;
            if (survey.circularAZ) {
                for (int i = 0; i < 4; i++) {
                    double[] Edge = Conversions
                            .AROffset(
                                    survey.center.lat,
                                    survey.center.lon,
                                    90 * i, OBSTRUCTION_LIMIT_m
                                            + survey.getRadius());
                    if (i == 0)
                        TopLat = Edge[0];
                    else if (i == 1)
                        RightLon = Edge[1];
                    else if (i == 2)
                        BottomLat = Edge[0];
                    else if (i == 3)
                        LeftLon = Edge[1];
                }
                return opc.GetAllPointsBounded(TopLat, BottomLat, LeftLon,
                        RightLon, null);
            } else {
                /*double Width = FilteringSurvey.width;
                double Length = FilteringSurvey.getLength(true);
                FilteringSurvey.width += OBSTRUCTION_LIMIT_m * 2;
                FilteringSurvey.setLength(FilteringSurvey.getLength()
                        + OBSTRUCTION_LIMIT_m * 2);
                corners = AZHelper.getAZCorners(FilteringSurvey);

                FilteringSurvey.width = Width;
                FilteringSurvey.setLength(Length, true);*/

                return GetFilteredPointCursor(opc, getAZCorners(survey, false),
                        null);
            }
        }
    }

    public static List<String> GetAZLineObstructionStrings(
            ObstructionProviderClient opc, SurveyData survey, boolean inside) {
        return GetFilteredLineObstructionStrings(opc, survey, inside);
    }

    public static List<SurveyPoint> getAZCorners(SurveyData survey,
            boolean inside) {
        List<SurveyPoint> corners = AZHelper.getAZCorners(survey);
        if (!inside) {
            double padding = OBSTRUCTION_LIMIT_m * 2;
            corners = AZHelper.getCorners(survey.center.lat, survey.center.lon,
                    survey.getLength(true) + padding, survey.width + padding,
                    survey.angle, survey.edges.ApproachOverrunLength_m,
                    survey.edges.DepartureOverrunLength_m);
        }
        return corners;
    }

    public static PointObstruction cursorToPointObstruction(Cursor cursor) {
        PointObstruction target = new PointObstruction();
        target.group = (cursor.getString(1));
        target.uid = (cursor.getString(2));
        target.type = (cursor.getString(3));
        target.lat = (cursor.getDouble(4));
        target.lon = (cursor.getDouble(5));
        target.setHAE(cursor.getFloat(6));
        target.linearError = (cursor.getFloat(7));
        target.circularError = (cursor.getFloat(8));
        target.height = (cursor.getFloat(9));
        target.width = (cursor.getFloat(10));
        target.length = (cursor.getFloat(11));
        target.remark = (cursor.getString(12));
        target.course_true = (cursor.getFloat(13));
        target.flags = cursor.getInt(14);

        return target;
    }

    public static List<PointObstruction> cursorToPointList(Cursor cursor) {
        List<PointObstruction> points = new ArrayList<PointObstruction>();
        if (cursor == null || cursor.getCount() < 1)
            return points;
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            points.add(cursorToPointObstruction(cursor));
        }
        return points;
    }

    /**
     * Check if line crosses bounds and return points of collision
     * @param lo Line obstruction
     * @param corners Polygon bounds
     * @return Points of collision
     */
    public static List<SurveyPoint> lineHitTest(
            LineObstruction lo, List<SurveyPoint> corners) {

        List<SurveyPoint> hitPoints = new ArrayList<SurveyPoint>();

        if (lo == null || corners == null || corners.size() < 2)
            return hitPoints;

        LineObstruction cornersLO = new LineObstruction();
        cornersLO.points.addAll(corners);

        // Skip lines that are completely separated
        // Returns [minLat, minLon, maxLat, maxLon]
        double[] b1 = lo.getBounds();
        double[] b2 = cornersLO.getBounds();
        if (b1[2] < b2[0] || b1[0] > b2[2]
                || b1[3] < b2[1] || b1[1] > b2[3])
            return hitPoints;

        Polygon poly = buildPolygon(corners);
        SurveyPoint last = null;
        for (int i = 0; i < lo.points.size(); i++) {
            SurveyPoint sp = lo.points.get(i);
            if (i == 0 && lo.closed)
                last = lo.points.get(lo.points.size() - 1);

            // Check for containing vertex
            PolyPoint pp = polyPoint(sp);
            if (poly.contains(pp))
                hitPoints.add(sp);

            // Check for line intersection
            if (last != null) {
                Line line = new Line(pp, polyPoint(last));
                for (Line side : poly.getSides()) {
                    SurveyPoint inter = lineIntersect(line, side);
                    if (inter != null) {
                        inter.setHAE(Math.max(sp.getHAE(), last.getHAE()));
                        hitPoints.add(inter);
                    }
                }
            }
            last = sp;
        }
        return hitPoints;
    }

    /**
     * Line intersection test for circles
     * @param lo Line obstruction
     * @param center Center of circle
     * @param radius Radius of circle in meters
     * @return Collision points
     */
    public static List<SurveyPoint> lineHitTest(
            LineObstruction lo, SurveyPoint center, double radius) {

        List<SurveyPoint> hitPoints = new ArrayList<SurveyPoint>();

        if (lo == null || center == null || radius <= 0)
            return hitPoints;

        // Skip lines that are completely outside the circle
        // Returns [minLat, minLon, maxLat, maxLon]
        double[] b1 = lo.getBounds();
        double[] b2 = getCircleBoundingBox(center, radius);
        if (b1[2] < b2[0] || b1[0] > b2[2]
                || b1[3] < b2[1] || b1[1] > b2[3])
            return hitPoints;

        SurveyPoint last = null;
        double[] lastRAB = null;
        for (int i = 0; i < lo.points.size(); i++) {
            SurveyPoint sp = lo.points.get(i);
            if (i == 0 && lo.closed)
                last = lo.points.get(lo.points.size() - 1);

            // Check for containing vertex
            double[] spRAB = Conversions.CalculateRangeAngle(
                    center.lat, center.lon, sp.lat, sp.lon);
            if (spRAB[0] < radius)
                hitPoints.add(sp);

            // Check for line intersection
            if (last != null) {
                if (lastRAB == null)
                    lastRAB = Conversions.CalculateRangeAngle(
                            center.lat, center.lon, last.lat, last.lon);

                double x1 = spRAB[0] * Math.sin(Math.toRadians(spRAB[1])), y1 = spRAB[0]
                        * Math.cos(Math.toRadians(spRAB[1])), x2 = lastRAB[0]
                        * Math.sin(Math.toRadians(lastRAB[1])), y2 = lastRAB[0]
                        * Math.cos(Math.toRadians(lastRAB[1]));

                double dx = x2 - x1, dy = y2 - y1, dr = Math.hypot(dx, dy), D = x1
                        * y2 - x2 * y1, dr2 = Math.pow(dr, 2);

                double discrm = Math.pow(radius, 2) * dr2 - Math.pow(D, 2);
                if (discrm >= 0) {
                    // Intersection or tangent found
                    // Now get points of intersection
                    double dySign = (dy < 0 ? -1 : 1);
                    Line interLine = new Line(new PolyPoint(x1, y1),
                            new PolyPoint(x2, y2));
                    for (int p = 0; p < 2; p++) {
                        int sign = (p == 0 ? 1 : -1);
                        PolyPoint interPP = new PolyPoint(
                                (D * dy + sign
                                        * (dySign * dx * Math.sqrt(discrm)))
                                        / dr2,
                                (-D * dx + sign
                                        * (Math.abs(dy) * Math.sqrt(discrm)))
                                        / dr2);

                        // Need to check if point is on line
                        if (interLine.isInside(interPP)) {
                            // Convert meter offsets back to latlon
                            double inter_ang = Math.toDegrees(
                                    Math.atan2(interPP.x, interPP.y));
                            double inter_range = Math.hypot(interPP.x,
                                    interPP.y);

                            double[] inter_sp = Conversions.AROffset(
                                    center.lat,
                                    center.lon, inter_ang, inter_range);
                            SurveyPoint interSP = new SurveyPoint(inter_sp[0],
                                    inter_sp[1]);
                            interSP.setHAE(Math.max(sp.getHAE(), last.getHAE()));
                            hitPoints.add(interSP);
                        }
                    }
                }
            }
            lastRAB = spRAB;
            last = sp;
        }
        return hitPoints;
    }

    /**
     * Calculate a circle's bounding box cube in lat/lon
     * @param center Circle center
     * @param radius Radius in meters
     * @return [minLat, minLon, maxLat, maxLon]
     */
    public static double[] getCircleBoundingBox(SurveyPoint center,
            double radius) {
        double radius_far = radius / Math.cos(Math.toRadians(45));
        double[] min = Conversions.AROffset(center.lat, center.lon, 225,
                radius_far);
        double[] max = Conversions.AROffset(center.lat, center.lon, 45,
                radius_far);
        return new double[] {
                min[0], min[1], max[0], max[1]
        };
    }

    /**
     * Static port of Polygon.intersect that returns point
     * @param a Line 1
     * @param b Line 2
     * @return SurveyPoint of intersection
     */
    public static SurveyPoint lineIntersect(Line a, Line b) {
        double x, y;
        if (!a.isVertical() && !b.isVertical()) {
            if (a.getA() - b.getA() == 0)
                return null;
            x = (b.getB() - a.getB()) / (a.getA() - b.getA());
            y = b.getA() * x + b.getB();
        } else if (a.isVertical() && !b.isVertical()) {
            x = a.getStart().x;
            y = b.getA() * x + b.getB();
        } else {
            if (a.isVertical() || !b.isVertical())
                return null;
            x = b.getStart().x;
            y = a.getA() * x + a.getB();
        }
        PolyPoint inter = new PolyPoint(x, y);
        if (b.isInside(inter) && a.isInside(inter))
            return new SurveyPoint(x, y);
        return null;
    }

    /**
     * Set route width using non-center offset
     * @param lo Line obstruction
     * @param width New route width
     * @param side Anchor side (0 = left, 1 = center, 2 = right)
     * @return True if modified, false otherwise
     */
    public static boolean setRouteWidth(LineObstruction lo, double width,
            int side) {
        if (lo == null || lo.width == width)
            return false;
        if (side != 1) {
            // Offset all points by LRC offset
            List<SurveyPoint> newCenters = new ArrayList<SurveyPoint>();
            int size = lo.points.size();
            for (int i = 0; i < size; i++) {
                SurveyPoint c = lo.points.get(i);
                if (c == null)
                    continue;
                SurveyPoint p = i > 0 ? lo.points.get(i - 1) : null;
                SurveyPoint n = i < size - 1 ? lo.points.get(i + 1) : null;
                double ang = Conversions.computeAngle(p, c, n);
                double[] offset = Conversions.AROffset(c.lat, c.lon,
                        ang + (side == 2 ? 90 : -90),
                        (lo.width / 2) - (width / 2));
                newCenters.add(new SurveyPoint(offset[0], offset[1]));
            }
            lo.points.clear();
            lo.points.addAll(newCenters);
        }
        lo.width = width;
        return true;
    }
}
