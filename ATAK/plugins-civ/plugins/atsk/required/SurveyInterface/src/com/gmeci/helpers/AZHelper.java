
package com.gmeci.helpers;

import android.database.Cursor;
import android.util.Log;

import com.gmeci.atskservice.farp.FARPTankerItem;
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
import com.sromku.polygon.PolyPoint;
import com.sromku.polygon.Polygon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class AZHelper {
    public static final int TOP_RIGHT = 0;
    public static final int TOP_LEFT = 1;
    public static final int BOTTOM_LEFT = 2;
    public static final int BOTTOM_RIGHT = 3;
    private static final String TAG = "AZHelper";
    public static final double QUAD_OBSTRUCTION_LIMIT = 10000;
    public static final double TEN_TO_ONE_ELEV_DEG = 5.7105931374996425126958813482344;//10 to 1 radio
    private static final double MAX_FAM_RX_LENGTH = (float) (300f / Conversions.M2F);
    public static final int TOP = 0;
    public static final int RIGHT = 1;
    public static final int BOTTOM = 2;
    public static final int LEFT = 3;

    private static void CalculateMiddleFARPRx(SurveyPoint rx,
            SurveyPoint fam, double angle, double length) {
        rx.setSurveyPoint(Conversions.AROffset(fam, angle, length));
        rx.course_true = angle;
        rx.visible = true;
    }

    private static double[] CalculatePerpFARPRx(boolean Right,
            SurveyPoint FAMPoint, double FAMRxAngle, double Width_m) {
        double RxPoint[];
        if (Right)
            RxPoint = Conversions.AROffset(FAMPoint.lat, FAMPoint.lon,
                    FAMRxAngle - 90, Width_m);
        else
            RxPoint = Conversions.AROffset(FAMPoint.lat, FAMPoint.lon,
                    FAMRxAngle + 90, Width_m);

        return RxPoint;
    }

    private static void CalculateFARPRx(boolean right, SurveyPoint rx,
            SurveyPoint fam, double angle, double length) {
        angle += right ? -45 : 45;
        rx.setSurveyPoint(Conversions.AROffset(fam, angle, length));
        rx.course_true = angle;
        rx.visible = true;
    }

    public static LineObstruction getFAMLine(
            SurveyData survey, FARPTankerItem tanker, int sideIndex) {
        SurveyPoint famPoint = survey.FAMPoints[sideIndex];
        if (famPoint == null || !famPoint.visible || tanker == null)
            return new LineObstruction();
        LineObstruction FAMLine = new LineObstruction();
        if (famPoint.circularError > 0) {
            FAMLine.type = ATSKConstants.FARP_FAM_TYPE_SELECTED;
        } else
            FAMLine.type = ATSKConstants.FARP_FAM_TYPE;
        FAMLine.uid = survey.uid
                + ATSKConstants.FARP_FAM_TYPE
                + sideIndex;
        FAMLine.points = new ArrayList<SurveyPoint>();
        FAMLine.points.add(tanker.getFuelPoint(survey, sideIndex == 0));
        FAMLine.points.add(new SurveyPoint(famPoint));
        return FAMLine;
    }

    public static SurveyPoint[] getRefuelingPoints(int sideIndex,
            SurveyData survey, FARPTankerItem tanker, LineObstruction ret) {
        SurveyPoint[] rx = new SurveyPoint[3];
        for (int j = 0; j < 3; j++) {
            rx[j] = new SurveyPoint();
            rx[j].visible = false;
        }
        double length = MAX_FAM_RX_LENGTH - survey.getFAMDistance();
        if (survey.FAMRxShape == null)
            return rx;
        SurveyPoint famPoint = survey.FAMPoints[sideIndex];
        if (tanker == null || famPoint == null)
            return rx;
        double famAngle = survey.FAMRxAngle[sideIndex];
        if (survey.FAMRxShape.equals(ATSKConstants.FARP_RX_LAYOUT_SINGLE)) {
            // Single - middle line only
            CalculateMiddleFARPRx(rx[0], famPoint, famAngle, length);
            ret.points.add(famPoint);
            ret.points.add(rx[0]);
        } else if (survey.FAMRxShape
                .equals(ATSKConstants.FARP_RX_LAYOUT_HRIGHT)) {
            //center
            CalculateMiddleFARPRx(rx[0], famPoint, famAngle, length);
            //right
            CalculateFARPRx(true, rx[1], famPoint, famAngle, length);
            ret.points.add(rx[0]);
            ret.points.add(famPoint);
            ret.points.add(rx[1]);
        } else if (survey.FAMRxShape.equals(ATSKConstants.FARP_RX_LAYOUT_HLEFT)) {
            //center
            CalculateMiddleFARPRx(rx[0], famPoint, famAngle, length);
            //left
            CalculateFARPRx(false, rx[1], famPoint, famAngle, length);
            ret.points.add(rx[0]);
            ret.points.add(famPoint);
            ret.points.add(rx[1]);
        } else if (survey.FAMRxShape.equals(ATSKConstants.FARP_RX_LAYOUT_SPLIT)) {
            //left
            CalculateFARPRx(false, rx[0], famPoint, famAngle, length);
            //right
            CalculateFARPRx(true, rx[1], famPoint, famAngle, length);
            ret.points.add(rx[0]);
            ret.points.add(famPoint);
            ret.points.add(rx[1]);
        } else if (survey.FAMRxShape
                .equals(ATSKConstants.FARP_RX_LAYOUT_TRIPLE)) {
            //all three
            //center
            CalculateMiddleFARPRx(rx[0], famPoint, famAngle, length);
            //left
            CalculateFARPRx(false, rx[1], famPoint, famAngle, length);
            //right
            CalculateFARPRx(true, rx[2], famPoint, famAngle, length);
            ret.points.add(rx[1]);
            ret.points.add(famPoint);
            ret.points.add(rx[0]);
            ret.points.add(famPoint);
            ret.points.add(rx[2]);
        } else {
            SurveyPoint fuelPoint = tanker.getFuelPoint(survey, sideIndex == 0);
            double angle = Conversions.calculateAngle(fuelPoint, famPoint);
            // RGR (wing attached)
            // hose 1
            CalculateMiddleFARPRx(rx[0], famPoint, famAngle, length);
            // connecting hose
            SurveyPoint connecting = new SurveyPoint();
            CalculateMiddleFARPRx(connecting, famPoint, angle,
                    survey.getFAMDistance());
            //right
            CalculateMiddleFARPRx(rx[1], connecting, famAngle, length);
            ret.points.add(rx[0]);
            ret.points.add(famPoint);
            ret.points.add(connecting);
            ret.points.add(rx[1]);
        }
        return rx;
    }

    public static SurveyPoint[] getRefuelingPoints(int sideIndex,
            SurveyData survey, FARPTankerItem tanker) {
        return getRefuelingPoints(sideIndex, survey, tanker,
                new LineObstruction());
    }

    //finds all points breaking a glide slope plane inside an approach or departure polygon
    //puts the worst obstruciton point FIRST on the list
    public static List<PointObstruction> CalculateLZGlideSlopeIncursions(
            SurveyData survey, List<PointObstruction> points,
            List<LineObstruction> lines, boolean useDeparture) {
        List<PointObstruction> badPoints = new ArrayList<PointObstruction>();
        if (survey == null)
            return badPoints;

        ArrayList<SurveyPoint> corners = AZHelper.getInnerApproachCorners(
                survey, useDeparture);

        //Loop to determine the lat and lon boundaries of the AZ
        Iterator<SurveyPoint> it = corners.iterator();
        double latBottom = 90.0;
        double lonLeft = 180.0;
        double latTop = -90;
        double lonRight = -180;
        while (it.hasNext()) {
            SurveyPoint next = it.next();
            if (next.lat < latBottom) {
                latBottom = next.lat;
            }
            if (next.lon < lonLeft) {
                lonLeft = next.lon;
            }
            if (next.lat > latTop) {
                latTop = next.lat;
            }
            if (next.lon > lonRight) {
                lonRight = next.lon;
            }
        }
        Polygon ApproachPGon = ObstructionHelper.buildPolygon(corners);
        double worstGSR = 0;
        double minGSR = 0;

        SurveyPoint centerOfEdge = AZHelper
                .CalculateCenterOfEdge(
                        survey.angle,
                        survey.getLength(false) + 2
                                * survey.endClearZoneLength,
                        survey.center, !useDeparture);
        if (survey.getType() == AZ_TYPE.LZ
                || survey.getType() == AZ_TYPE.STOL
                || survey.getType() == AZ_TYPE.LTFW) {
            if (useDeparture)
                centerOfEdge.setHAE(survey.departureElevation);
            else
                centerOfEdge.setHAE(survey.approachElevation);
        }

        // Can't filter if we have an invalid elevation
        if (centerOfEdge.getHAE() == SurveyPoint.Altitude.INVALID)
            return badPoints;

        // Points
        if (points != null) {
            for (PointObstruction po : points) {
                if (po != null && !isIncursionUID(po.uid) &&
                        !po.type.equals(Constants.PO_RAB_CIRCLE)) {
                    if (po.type.equals(Constants.PO_LABEL)) {
                        List<LineObstruction> leaders = new ArrayList<LineObstruction>();
                        for (LineObstruction lo : lines) {
                            if (lo.type.equals(Constants.LO_LEADER)
                                    && lo.uid.startsWith(po.uid))
                                leaders.add(lo);
                        }
                        // Keep bad points ordered
                        List<PointObstruction> leaderPoints =
                                CalculateLZGlideSlopeIncursions(survey,
                                        ObstructionHelper.getLeaderPoints(
                                                po, leaders),
                                        null, useDeparture);
                        for (PointObstruction lp : leaderPoints) {
                            double[] RAE = Conversions
                                    .CalculateRangeAngleElev(
                                            centerOfEdge.lat,
                                            centerOfEdge.lon,
                                            centerOfEdge.getHAE(), lp.lat,
                                            lp.lon, lp.getHAE() + lp.height);
                            if (RAE[2] < minGSR)
                                continue;
                            if (RAE[2] > worstGSR) {
                                worstGSR = RAE[2];
                                badPoints.add(0, lp);
                            } else
                                badPoints.add(lp);
                        }
                    }

                    boolean inside = ApproachPGon.contains(new PolyPoint(
                            po.lat, po.lon));
                    // Add points where only the extents of their
                    // width/length penetrate the approach/departure
                    if (!inside && po.width > 0
                            && po.length > 0) {
                        SurveyPoint[] poCorners = po.getCorners();
                        for (SurveyPoint sp : poCorners) {
                            if (ApproachPGon.contains(new PolyPoint(sp.lat,
                                    sp.lon))) {
                                inside = true;
                                break;
                            }
                        }
                    }
                    if (inside) {
                        double[] RAE = Conversions
                                .CalculateRangeAngleElev(
                                        centerOfEdge.lat, centerOfEdge.lon,
                                        centerOfEdge.getHAE(),
                                        po.lat,
                                        po.lon,
                                        po.getHAE() + po.height);
                        //keep the worst point first
                        if (RAE[2] < minGSR)
                            continue;
                        if (RAE[2] > worstGSR) {
                            worstGSR = RAE[2];
                            badPoints.add(0, po);
                        } else
                            badPoints.add(po);
                    }
                }
            }
        }

        // Routes/Areas
        if (lines != null) {
            for (LineObstruction lo : lines) {
                if (lo != null && !isIncursionUID(lo.uid)
                        && !lo.type.equals(Constants.LO_LEADER)
                        && !lo.type.equals(Constants.PO_RAB_LINE)) {
                    List<SurveyPoint> hitPoints =
                            ObstructionHelper.lineHitTest(lo, corners);

                    for (SurveyPoint lp : hitPoints) {
                        double[] RAE = Conversions.CalculateRangeAngleElev(
                                centerOfEdge.lat, centerOfEdge.lon,
                                centerOfEdge.getHAE(), lp.lat,
                                lp.lon, lp.getHAE() + lo.height);
                        if (RAE[2] < minGSR)
                            continue;

                        PointObstruction badPoint = new PointObstruction();
                        badPoint.lat = lp.lat;
                        badPoint.lon = lp.lon;
                        badPoint.alt = lp.alt;
                        badPoint.width = lo.width;
                        badPoint.height = lo.height;
                        badPoint.type = lo.type;
                        //keep the worst point first
                        if (RAE[2] > worstGSR) {
                            worstGSR = RAE[2];
                            badPoints.add(0, badPoint);
                        } else
                            badPoints.add(badPoint);
                    }
                }
            }
        }
        return badPoints;
    }

    private static boolean isIncursionUID(final String UID) {
        return UID.endsWith(ATSKConstants.INCURSION_LINE_APPROACH)
                || UID.endsWith(ATSKConstants.INCURSION_LINE_APPROACH_WORST) ||
                UID.endsWith(ATSKConstants.INCURSION_LINE_DEPARTURE)
                || UID.endsWith(ATSKConstants.INCURSION_LINE_DEPARTURE_WORST);
    }

    //starts at top right and proceeds counter clockwise

    public static ArrayList<SurveyPoint> getCorners(double CenterLat,
            double CenterLon, double Length_m, double Width_m,
            double Angle_deg_t) {
        return getCorners(CenterLat, CenterLon, Length_m, Width_m, Angle_deg_t,
                0, 0);
    }

    public static ArrayList<SurveyPoint> getCorners(double CenterLat,
            double CenterLon, double Length_m, double Width_m,
            double Angle_deg_t,
            double ApproachOffset_m, double DepartureOffset_m) {

        Log.d(TAG, "getting the AZ corners: lat=" + CenterLat +
                " lon=" + CenterLon + " len=" + Length_m + " width=" + Width_m
                + "angle=" + Angle_deg_t +
                " approachOffset=" + ApproachOffset_m + " departOffset="
                + DepartureOffset_m);

        ArrayList<SurveyPoint> corners = new ArrayList<SurveyPoint>();

        double halfLength = Length_m / 2f;
        double halfWidth = Width_m / 2f;

        double[] RightMiddle = Conversions.AROffset(CenterLat, CenterLon,
                Angle_deg_t + 90, halfWidth);
        double[] LeftMiddle = Conversions.AROffset(CenterLat, CenterLon,
                Angle_deg_t - 90, halfWidth);

        double[] TRCorner = Conversions.AROffset(RightMiddle[0],
                RightMiddle[1], Angle_deg_t, halfLength - DepartureOffset_m);//departure
        double[] BRCorner = Conversions.AROffset(RightMiddle[0],
                RightMiddle[1], Angle_deg_t + 180, halfLength
                        - ApproachOffset_m);//approach

        double[] TLCorner = Conversions.AROffset(LeftMiddle[0], LeftMiddle[1],
                Angle_deg_t, halfLength - DepartureOffset_m);//departure
        double[] BLCorner = Conversions.AROffset(LeftMiddle[0], LeftMiddle[1],
                Angle_deg_t + 180, halfLength - ApproachOffset_m);//approach

        corners.add(new SurveyPoint(TRCorner[0], TRCorner[1]));
        corners.add(new SurveyPoint(TLCorner[0], TLCorner[1]));
        corners.add(new SurveyPoint(BLCorner[0], BLCorner[1]));
        corners.add(new SurveyPoint(BRCorner[0], BRCorner[1]));

        /*for (int i = 0; i < corners.size(); ++i) {
            Log.d(TAG, "survey point[" + i + "]: " + corners.get(i));
        }*/

        return corners;
    }

    /*Top right first-  moving counter clockwise*/
    public static ArrayList<SurveyPoint> getAZCorners(SurveyData survey) {
        return getAZCorners(survey, true);
    }

    public static ArrayList<SurveyPoint> getAZCorners(SurveyData survey,
            boolean Allowtruncation) {

        double ApproachOffset_m = 0, DepartureOffset_m = 0;
        if (Allowtruncation)
            ApproachOffset_m = survey.edges.ApproachOverrunLength_m;
        if (Allowtruncation)
            DepartureOffset_m = survey.edges.DepartureOverrunLength_m;

        return getCorners(survey.center.lat, survey.center.lon,
                survey.getLength(true), survey.width,
                survey.angle, ApproachOffset_m, DepartureOffset_m);

    }

    public static ArrayList<SurveyPoint> getOuterApproachCorners(
            SurveyData survey, boolean isDeparture) {
        // create ArrayList with initial capacity of 5
        ArrayList<SurveyPoint> corners = new ArrayList<SurveyPoint>();
        double angle = survey.angle;

        if (isDeparture)
            angle += 180;

        if (angle > 360)
            angle -= 360;

        // corners closest to the runway -- at same length, but going out
        // further
        SurveyPoint inner_right = new SurveyPoint();
        inner_right.lat = survey.center.lat;
        inner_right.lon = survey.center.lon;
        SurveyPoint inner_left = new SurveyPoint();
        inner_left.lat = survey.center.lat;
        inner_left.lon = survey.center.lon;
        inner_right.findCorner(angle, survey.getLength(false) / 2f,
                survey.approachInnerWidth / 2f);
        inner_left.findCorner(angle, survey.getLength(false) / 2f, (-1
                * survey.approachInnerWidth / 2f));

        // corners.add(APPROACH_INNER_LEFT, inner_left);

        double trapezoidAngle = CalculateTrapezoidAngle(
                survey.approachInnerLength, survey.approachInnerWidth,
                survey.approachOuterWidth);

        double pos[] = Conversions.AROffset(inner_right.lat, inner_right.lon,
                angle - trapezoidAngle,
                survey.approachInnerLength);
        SurveyPoint mid_right = new SurveyPoint(pos[0], pos[1]);

        pos = Conversions
                .AROffset(inner_left.lat, inner_left.lon, angle
                        + trapezoidAngle, survey.approachInnerLength);
        SurveyPoint mid_left = new SurveyPoint(pos[0], pos[1]);

        pos = Conversions.AROffset(mid_right.lat, mid_right.lon, angle,
                survey.approachOuterLength);
        SurveyPoint outer_right = new SurveyPoint(pos[0], pos[1]);

        pos = Conversions.AROffset(mid_left.lat, mid_left.lon, angle,
                survey.approachOuterLength);
        SurveyPoint outer_left = new SurveyPoint(pos[0], pos[1]);

        // These MUST be in this order, android will bitch about all the
        // writeToParcel if not
        //corners.add(APPROACH_INNER_RIGHT, inner_right);
        corners.add(mid_right);
        corners.add(outer_right);
        corners.add(outer_left);

        //switched these two - switch them back to mid left then inner left
        corners.add(mid_left);
        //corners.add(APPROACH_INNER_LEFT, inner_left);

        return corners;
    }

    public static ArrayList<SurveyPoint> getInnerApproachCorners(
            SurveyData survey, boolean isDeparture) {

        ArrayList<SurveyPoint> points = new ArrayList<SurveyPoint>();

        double RealLength = survey.getLength(false) / 2;
        if (isDeparture) {
            double nearCenter[] = Conversions.AROffset(survey.center.lat,
                    survey.center.lon, survey.angle, RealLength
                            + survey.endClearZoneLength);
            double farCenter[] = Conversions.AROffset(survey.center.lat,
                    survey.center.lon, survey.angle, RealLength
                            + survey.endClearZoneLength
                            + survey.approachInnerLength);

            double nearRight[] = Conversions.AROffset(nearCenter[0],
                    nearCenter[1], survey.angle + 90,
                    survey.endClearZoneOuterWidth / 2);
            double nearLeft[] = Conversions.AROffset(nearCenter[0],
                    nearCenter[1], survey.angle - 90,
                    survey.endClearZoneOuterWidth / 2);

            double farRight[] = Conversions.AROffset(farCenter[0],
                    farCenter[1], survey.angle + 90,
                    survey.approachInnerWidth / 2);
            double farLeft[] = Conversions
                    .AROffset(farCenter[0], farCenter[1],
                            survey.angle - 90,
                            survey.approachInnerWidth / 2);
            points.add(new SurveyPoint(nearRight[0], nearRight[1]));
            points.add(new SurveyPoint(nearLeft[0], nearLeft[1]));
            points.add(new SurveyPoint(farLeft[0], farLeft[1]));
            points.add(new SurveyPoint(farRight[0], farRight[1]));
            points.add(new SurveyPoint(nearRight[0], nearRight[1]));
        } else {
            double nearCenter[] = Conversions.AROffset(survey.center.lat,
                    survey.center.lon, survey.angle + 180, RealLength
                            + survey.endClearZoneLength);
            double farCenter[] = Conversions.AROffset(survey.center.lat,
                    survey.center.lon, survey.angle + 180, RealLength
                            + survey.endClearZoneLength
                            + survey.approachInnerLength);

            double nearRight[] = Conversions.AROffset(nearCenter[0],
                    nearCenter[1], survey.angle + 90,
                    survey.endClearZoneOuterWidth / 2);
            double nearLeft[] = Conversions.AROffset(nearCenter[0],
                    nearCenter[1], survey.angle - 90,
                    survey.endClearZoneOuterWidth / 2);

            double farRight[] = Conversions.AROffset(farCenter[0],
                    farCenter[1], survey.angle + 90,
                    survey.approachInnerWidth / 2);
            double farLeft[] = Conversions
                    .AROffset(farCenter[0], farCenter[1],
                            survey.angle - 90,
                            survey.approachInnerWidth / 2);
            points.add(new SurveyPoint(nearRight[0], nearRight[1]));
            points.add(new SurveyPoint(nearLeft[0], nearLeft[1]));
            points.add(new SurveyPoint(farLeft[0], farLeft[1]));
            points.add(new SurveyPoint(farRight[0], farRight[1]));
            points.add(new SurveyPoint(nearRight[0], nearRight[1]));
        }
        return points;
    }

    private static double CalculateTrapezoidAngle(double length,
            double shortWidth, double longWidth) {
        double widthDiff = longWidth / 2f - (shortWidth / 2f);
        double Angle = Math.atan(widthDiff / length);
        return 180 / Math.PI * Angle;
    }

    public static SurveyPoint CalculateLZCenter(int Anchor,
            SurveyPoint clicked, double Length, double Width, double Angle_deg_t) {

        if (Anchor == ATSKConstants.ANCHOR_CENTER)
            return clicked;

        SurveyPoint center = new SurveyPoint();
        double HalfLength = Length / 2f;
        double HalfWidth = Width / 2f;

        double FinalAngle = 0;
        double FinalRange = 0;

        if (Anchor == ATSKConstants.ANCHOR_APPROACH_LEFT) {
            FinalRange = Math.sqrt((HalfLength * HalfLength)
                    + (HalfWidth * HalfWidth));
            double angle2center = Math.toDegrees(Math.atan(HalfWidth
                    / HalfLength));
            FinalAngle = Angle_deg_t + angle2center;
        } else if (Anchor == ATSKConstants.ANCHOR_APPROACH_CENTER) {
            FinalAngle = Angle_deg_t;
            FinalRange = HalfLength;
        } else if (Anchor == ATSKConstants.ANCHOR_APPROACH_RIGHT) {
            FinalRange = Math.sqrt((HalfLength * HalfLength)
                    + (HalfWidth * HalfWidth));
            double angle2center = Math.toDegrees(Math.atan(HalfWidth
                    / HalfLength));
            FinalAngle = Angle_deg_t - angle2center;
        } else if (Anchor == ATSKConstants.ANCHOR_DEPARTURE_LEFT) {
            FinalRange = Math.sqrt((HalfLength * HalfLength)
                    + (HalfWidth * HalfWidth));
            double Angle2Center = Math.toDegrees((Math.atan(HalfWidth
                    / HalfLength)));
            FinalAngle = Angle_deg_t - Angle2Center - 180;
        } else if (Anchor == ATSKConstants.ANCHOR_DEPARTURE_CENTER) {
            FinalAngle = 180 + Angle_deg_t;
            FinalRange = HalfLength;
        } else if (Anchor == ATSKConstants.ANCHOR_DEPARTURE_RIGHT) {
            FinalRange = Math.sqrt((HalfLength * HalfLength)
                    + (HalfWidth * HalfWidth));
            double Angle2Center = Math.toDegrees(Math.atan(HalfWidth
                    / HalfLength));
            FinalAngle = Angle_deg_t + Angle2Center - 180;
        }

        FinalAngle = Conversions.deNaN(FinalAngle);

        double coordinates[] = Conversions.AROffset(clicked.lat,
                clicked.lon, clicked.getHAE(), FinalAngle, FinalRange, 0);
        center.setSurveyPoint(coordinates[0], coordinates[1]);
        center.setHAE(clicked.getHAE());
        center.linearError = clicked.linearError;
        center.circularError = clicked.circularError;

        return center;
    }

    public static SurveyPoint CalculateLZCenter(SurveyData survey,
            SurveyPoint clicked) {
        if (survey.circularAZ)
            // Circular center isn't affected by anchor position
            return survey.center;
        return CalculateLZCenter(survey.AnchorPosition, clicked,
                survey.getLength(true), survey.width, survey.angle);
    }

    public static ArrayList<SurveyPoint> getOverrunCoordinates(
            SurveyData survey, boolean isDeparture, boolean inside) {
        ArrayList<SurveyPoint> overrunCoords = new ArrayList<SurveyPoint>();
        double angle_offset = 0;
        ArrayList<SurveyPoint> Corners = getAZCorners(survey, false);
        if (isDeparture) { // calculate departure overrun
            SurveyPoint tr = Corners.get(0);
            SurveyPoint tl = Corners.get(1);

            if (inside)
                angle_offset = 180;

            overrunCoords.add(tr);
            overrunCoords.add(tl);

            double outerL[] = Conversions.AROffset(tl.lat, tl.lon,
                    survey.angle + angle_offset,
                    survey.edges.DepartureOverrunLength_m);
            overrunCoords.add(new SurveyPoint(outerL[0], outerL[1]));

            double outerR[] = Conversions.AROffset(tr.lat, tr.lon,
                    survey.angle + angle_offset,
                    survey.edges.DepartureOverrunLength_m);
            overrunCoords.add(new SurveyPoint(outerR[0], outerR[1]));

        } else { // calculate approach overrun

            if (!inside)
                angle_offset = 180;

            SurveyPoint br = Corners.get(2);
            SurveyPoint bl = Corners.get(3);

            overrunCoords.add(br);
            overrunCoords.add(bl);

            double outerL[] = Conversions.AROffset(bl.lat, bl.lon,
                    survey.angle + angle_offset,
                    survey.edges.ApproachOverrunLength_m);
            overrunCoords.add(new SurveyPoint(outerL[0], outerL[1]));

            double outerR[] = Conversions.AROffset(br.lat, br.lon,
                    survey.angle + angle_offset,
                    survey.edges.ApproachOverrunLength_m);
            overrunCoords.add(new SurveyPoint(outerR[0], outerR[1]));
        }
        return overrunCoords;
    }

    public static SurveyPoint CalculatePointOfImpact(SurveyData survey,
            String piName) {
        // Custom PI locations
        boolean customPI = survey.getMetaBoolean("customPI", false);
        double verti = 0, horiz = 0, elev = SurveyPoint.Altitude.INVALID;
        if (piName.equals("cds")) {
            verti = survey.cdsPIOffset;
            elev = survey.cdsPIElevation;
            horiz = survey.getMetaDouble("cds_horiz_offset", 0);
        } else if (piName.equals("per")) {
            verti = survey.perPIOffset;
            elev = survey.perPIElevation;
            horiz = survey.getMetaDouble("per_horiz_offset", 0);
        } else if (piName.equals("he")) {
            verti = survey.hePIOffset;
            elev = survey.hePIElevation;
            horiz = survey.getMetaDouble("he_horiz_offset", 0);
        }
        if (!customPI) {
            if (survey.circularAZ)
                return survey.center;
            horiz = 0;
        }

        // Calculate position
        SurveyPoint leadingEdge = CalculateCenterOfEdge(survey, true);
        double angle = survey.angle;
        double pi[] = Conversions.AROffset(leadingEdge.lat,
                leadingEdge.lon, angle, verti);
        pi = Conversions.AROffset(pi[0], pi[1], angle + 90, horiz);

        SurveyPoint sp = new SurveyPoint(pi[0], pi[1]);
        sp.setHAE(elev);
        return sp;
    }

    public static SurveyPoint CalculateCenterOfEdge(double Angle_deg_t,
            double Length_m, SurveyPoint Center, boolean leadingEdge) {
        double halfLength = Length_m / 2f;
        double angle;
        if (leadingEdge)
            angle = Angle_deg_t + 180; //rotate by 180, want the leading edge, opposite direction that the DZ is going in
        else
            angle = Angle_deg_t;

        double[] edgePoints = Conversions.AROffset(Center.lat, Center.lon,
                angle, halfLength);
        //MIKE this should only return a lat and lon
        SurveyPoint CenterPoint = new SurveyPoint(edgePoints[0], edgePoints[1]);
        //LOU this is probably wrong...
        CenterPoint.alt = Center.alt;
        return CenterPoint;
    }

    public static SurveyPoint CalculateCenterOfEdge(SurveyData survey,
            boolean leadingEdge) {
        double surveyLen = survey.circularAZ ?
                survey.getRadius() * 2 : survey.getLength(true);
        surveyLen = ((surveyLen / 2) - (leadingEdge ? survey.edges
                .ApproachOverrunLength_m : survey.edges
                .DepartureOverrunLength_m)) * 2;
        return CalculateCenterOfEdge(survey.angle,
                surveyLen, survey.center, leadingEdge);
    }

    public static SurveyPoint CalculateAnchorFromAZCenter(SurveyData survey,
            SurveyPoint clicked, int Anchor) {
        return CalculateAnchorFromAZCenter(Anchor, survey.getLength(true),
                survey.width, survey.angle, clicked);
    }

    //public static SurveyPoint CalculateAnchorFromAZCenter(SurveyData survey, SurveyPoint clicked, int Anchor) {
    public static SurveyPoint CalculateAnchorFromAZCenter(int Anchor,
            double Length_m, double Width_m, double Angle_deg_true,
            SurveyPoint AnchorPoint) {
        if (Anchor == ATSKConstants.ANCHOR_CENTER)
            return AnchorPoint;

        SurveyPoint center = new SurveyPoint();
        double HalfLength = Length_m / 2f;
        double HalfWidth = Width_m / 2f;
        double Angle = Angle_deg_true;

        double FinalAngle = 0;
        double FinalRange = 0;

        if (Anchor == ATSKConstants.ANCHOR_APPROACH_LEFT) {
            FinalRange = Math.sqrt((HalfLength * HalfLength)
                    + (HalfWidth * HalfWidth));
            double angle2center = Math.toDegrees(Math.atan(HalfWidth
                    / HalfLength));
            FinalAngle = Angle + angle2center + 180;
        } else if (Anchor == ATSKConstants.ANCHOR_APPROACH_CENTER) {
            FinalAngle = Angle + 180;
            FinalRange = HalfLength;
        } else if (Anchor == ATSKConstants.ANCHOR_APPROACH_RIGHT) {
            FinalRange = Math.sqrt((HalfLength * HalfLength)
                    + (HalfWidth * HalfWidth));
            double angle2center = Math.toDegrees(Math.atan(HalfWidth
                    / HalfLength));
            FinalAngle = Angle - angle2center + 180;
        } else if (Anchor == ATSKConstants.ANCHOR_DEPARTURE_LEFT) {
            FinalRange = Math.sqrt((HalfLength * HalfLength)
                    + (HalfWidth * HalfWidth));
            double Angle2Center = Math.toDegrees((Math.atan(HalfWidth
                    / HalfLength)));
            FinalAngle = Angle - Angle2Center;
        } else if (Anchor == ATSKConstants.ANCHOR_DEPARTURE_CENTER) {
            FinalAngle = Angle;
            FinalRange = HalfLength;
        } else if (Anchor == ATSKConstants.ANCHOR_DEPARTURE_RIGHT) {
            FinalRange = Math.sqrt((HalfLength * HalfLength)
                    + (HalfWidth * HalfWidth));
            double Angle2Center = Math.toDegrees(Math.atan(HalfWidth
                    / HalfLength));
            FinalAngle = Angle + Angle2Center;
        }

        FinalAngle = Conversions.deNaN(FinalAngle);

        double coordinates[] = Conversions.AROffset(AnchorPoint.lat,
                AnchorPoint.lon, AnchorPoint.getHAE(), FinalAngle, FinalRange,
                0);
        center.setSurveyPoint(coordinates[0], coordinates[1]);
        center.setHAE(AnchorPoint.getHAE());
        center.linearError = AnchorPoint.linearError;
        center.circularError = AnchorPoint.circularError;

        return center;
    }

    public static ArrayList<SurveyPoint> getShoulderlArea(SurveyData survey,
            double width_m, boolean right) {
        ArrayList<SurveyPoint> areaCorners = new ArrayList<SurveyPoint>();

        int topIndex, bottomIndex;
        double angle = survey.angle;
        if (right) {
            topIndex = 0;
            bottomIndex = 3;
            angle += 90f;
        } else {
            topIndex = 1;
            bottomIndex = 2;
            angle -= 90;
        }

        ArrayList<SurveyPoint> Corners = getAZCorners(survey, true);
        double topInside[], bottomInside[];
        topInside = Conversions.AROffset(Corners.get(topIndex).lat,
                Corners.get(topIndex).lon, survey.angle,
                survey.edges.DepartureOverrunLength_m);
        bottomInside = Conversions.AROffset(Corners.get(bottomIndex).lat,
                Corners.get(bottomIndex).lon, survey.angle + 180,
                survey.edges.ApproachOverrunLength_m);

        areaCorners.add(new SurveyPoint(topInside[0], topInside[1]));
        areaCorners.add(new SurveyPoint(bottomInside[0], bottomInside[1]));

        double bottomOutside[] = Conversions.AROffset(bottomInside[0],
                bottomInside[1], angle, width_m);
        areaCorners.add(new SurveyPoint(bottomOutside[0], bottomOutside[1]));

        double topOutside[] = Conversions.AROffset(topInside[0], topInside[1],
                angle, width_m);
        areaCorners.add(new SurveyPoint(topOutside[0], topOutside[1]));

        return areaCorners;
    }

    public static ArrayList<SurveyPoint> getLateralArea(SurveyData survey,
            double width_m, boolean right, double inset_m, boolean usableOnly) {
        ArrayList<SurveyPoint> areaCorners = new ArrayList<SurveyPoint>();

        int topIndex, bottomIndex;
        double angle = survey.angle;
        if (right) {
            topIndex = 0;
            bottomIndex = 3;
            angle += 90f;
        } else {
            topIndex = 1;
            bottomIndex = 2;
            angle -= 90;
        }

        ArrayList<SurveyPoint> Corners = getAZCorners(survey, usableOnly);
        double topInside[] = Conversions.AROffset(Corners.get(topIndex).lat,
                Corners.get(topIndex).lon, angle, inset_m);
        double bottomInside[] = Conversions.AROffset(
                Corners.get(bottomIndex).lat, Corners.get(bottomIndex).lon,
                angle, inset_m);
        areaCorners.add(new SurveyPoint(topInside[0], topInside[1]));
        areaCorners.add(new SurveyPoint(bottomInside[0], bottomInside[1]));

        double bottomOutside[] = Conversions.AROffset(bottomInside[0],
                bottomInside[1], angle, width_m);
        areaCorners.add(new SurveyPoint(bottomOutside[0], bottomOutside[1]));

        double topOutside[] = Conversions.AROffset(topInside[0], topInside[1],
                angle, width_m);
        areaCorners.add(new SurveyPoint(topOutside[0], topOutside[1]));

        return areaCorners;
    }

    public static ArrayList<SurveyPoint> getOverrunArrow(SurveyData survey,
            boolean innerArrow, boolean departure) {
        ArrayList<SurveyPoint> arrowPoints = new ArrayList<SurveyPoint>();
        double offset;
        double angle = survey.angle;
        double thirdWidth = survey.width / 3f;

        if (innerArrow)
            offset = survey.edges.ApproachOverrunLength_m / 8;
        else
            offset = survey.edges.ApproachOverrunLength_m / 4;

        SurveyPoint edge;

        if (departure) {
            edge = AZHelper.CalculateCenterOfEdge(survey, false);
        } else {
            edge = AZHelper.CalculateCenterOfEdge(survey, true);
            angle += 180;
        }

        double arrowTip[] = Conversions.AROffset(edge.lat, edge.lon, angle,
                offset);
        SurveyPoint tipPoint = new SurveyPoint(arrowTip[0], arrowTip[1]);
        tipPoint.setHAE(arrowTip[2]);

        double arrowHeadRight[] = Conversions.AROffset(tipPoint.lat,
                tipPoint.lon, angle + 60, thirdWidth);
        double arrowHeadLeft[] = Conversions.AROffset(tipPoint.lat,
                tipPoint.lon, angle - 60, thirdWidth);

        SurveyPoint pArrowHeadRight = new SurveyPoint(arrowHeadRight[0],
                arrowHeadRight[1]);
        SurveyPoint pArrowHeadLeft = new SurveyPoint(arrowHeadLeft[0],
                arrowHeadLeft[1]);

        arrowPoints.add(pArrowHeadLeft);
        arrowPoints.add(tipPoint);
        arrowPoints.add(pArrowHeadRight);

        return arrowPoints;
    }

    public static PointObstruction[][] CalculateQuadrantObstructions(
            SurveyData survey, ObstructionProviderClient opc) {
        if (survey == null)
            return null;

        double maxLat = -90, minLat = 90, minLon = 180, maxLon = -180;
        for (int i = TOP; i <= LEFT; i++) {
            double[] edge = Conversions.AROffset(survey.center.lat,
                    survey.center.lon, 90 * i, survey.getLength()
                            + QUAD_OBSTRUCTION_LIMIT);
            maxLat = Math.max(maxLat, edge[0]);
            maxLon = Math.max(maxLon, edge[1]);
            minLat = Math.min(minLat, edge[0]);
            minLon = Math.min(minLon, edge[1]);
        }

        // GSR stored within point 'speed' field
        List<List<PointObstruction>> ret = new ArrayList<List<PointObstruction>>(
                LEFT + 1);
        List<List<SurveyPoint>> wedges = new ArrayList<List<SurveyPoint>>(
                LEFT + 1);
        for (int i = TOP; i <= LEFT; i++) {
            //build an empty list every time
            ret.add(new ArrayList<PointObstruction>());
            // Create wedge
            List<SurveyPoint> wedge = new ArrayList<SurveyPoint>();
            wedge.add(new SurveyPoint(survey.center));
            switch (i) {
                case TOP:
                    wedge.add(new SurveyPoint(maxLat, minLon));
                    wedge.add(new SurveyPoint(maxLat, maxLon));
                    break;
                case RIGHT:
                    wedge.add(new SurveyPoint(maxLat, maxLon));
                    wedge.add(new SurveyPoint(minLat, maxLon));
                    break;
                case BOTTOM:
                    wedge.add(new SurveyPoint(minLat, minLon));
                    wedge.add(new SurveyPoint(minLat, maxLon));
                    break;
                case LEFT:
                    wedge.add(new SurveyPoint(minLat, minLon));
                    wedge.add(new SurveyPoint(maxLat, minLon));
                    break;
            }
            wedges.add(wedge);
        }

        ArrayList<SurveyPoint> HLZCorners = AZHelper
                .getAZCorners(survey, false);
        Polygon HLZPGon = ObstructionHelper.buildPolygon(HLZCorners);

        Cursor ptCursor = null;
        try {
            ptCursor = opc.GetAllPointsBounded(maxLat, minLat,
                    minLon, maxLon, null);
            if (ptCursor != null) {
                for (ptCursor.moveToFirst(); !ptCursor.isAfterLast(); ptCursor
                        .moveToNext()) {
                    //First get range, angle, and elevation values from the chosen points lat lon and height.                
                    PointObstruction endPoint = opc
                            .GetPointObstruction(ptCursor);
                    if (endPoint == null)
                        continue;

                    double alt = survey.center.getHAE();
                    if (alt == SurveyPoint.Altitude.INVALID)
                        alt = 0;

                    double endAlt = endPoint.getHAE();
                    if (!endPoint.alt.isValid())
                        endAlt = 0;

                    double[] RangeAngleElev = Conversions
                            .CalculateRangeAngleElev(survey.center.lat,
                                    survey.center.lon,
                                    alt, endPoint.lat,
                                    endPoint.lon, endAlt + endPoint.height);

                    //Log.d(TAG, "found a point obstruction: " + endPoint.height);

                    //Adjust the azimuth to meet mil standard unit circle
                    double azimuth = Conversions.deg360(RangeAngleElev[1]);

                    //Check to see if the Elevation, in degrees, exceeds the max elevation angle for appr/dep.
                    double elev_deg = RangeAngleElev[2];
                    if (IsOutsideHLZ(survey, RangeAngleElev, HLZPGon,
                            endPoint) && elev_deg > 0) {

                        //Log.d(TAG, "found a point in a line obstruction that obstructs: " + endPoint.type + 
                        //           " centerpoint alt: " + alt + " endAlt: " + endAlt + " height: " + endPoint.height + 
                        //           " computed elev in deg: " + elev_deg + " distance: " + RangeAngleElev[0]);

                        endPoint.speed = elev_deg;

                        for (int i = TOP; i <= LEFT; i++) {
                            if (Conversions.angleWithin(azimuth,
                                    (i * 90) - 45, (i * 90) + 45))
                                ret.get(i).add(endPoint);
                        }
                    }
                }
            }
        } finally {
            if (ptCursor != null)
                ptCursor.close();
        }
        double surveyAlt = survey.center.getHAE();
        if (surveyAlt == SurveyPoint.Altitude.INVALID)
            surveyAlt = 0;
        final Cursor lineCursor = opc.GetAllLinesBounded(
                ATSKConstants.DEFAULT_GROUP, maxLat, minLat, minLon, maxLon);
        if (lineCursor != null) {
            for (lineCursor.moveToFirst(); lineCursor.getCount() > 0
                    && !lineCursor.isAfterLast(); lineCursor.moveToNext()) {
                LineObstruction lo = opc.GetLine(ATSKConstants.DEFAULT_GROUP,
                        lineCursor.getString(lineCursor
                                .getColumnIndex(DBURIConstants.COLUMN_UID)));
                if (lo == null || lo.points == null || lo.points.isEmpty())
                    continue;

                List<SurveyPoint> closest = closestPointsFromCenter(
                        survey.center, lo);
                for (SurveyPoint sp : closest) {
                    double endAlt = sp.getHAE();
                    if (!sp.alt.isValid())
                        endAlt = 0;

                    double[] RangeAngleElev = Conversions
                            .CalculateRangeAngleElev(survey.center.lat,
                                    survey.center.lon,
                                    surveyAlt, sp.lat, sp.lon,
                                    endAlt + lo.height);

                    //Check to see if the Elevation, in degrees, exceeds the max elevation angle for appr/dep.
                    double elev_deg = RangeAngleElev[2];
                    if (IsOutsideHLZ(survey, RangeAngleElev, HLZPGon, sp)
                            && elev_deg > 0) {
                        PointObstruction endPoint = new PointObstruction(sp);
                        endPoint.uid = lo.uid;
                        endPoint.remark = lo.remarks;
                        endPoint.type = lo.type;
                        endPoint.group = lo.group;
                        endPoint.height = lo.height;
                        endPoint.speed = elev_deg;
                        for (int i = TOP; i <= LEFT; i++) {
                            Polygon wedge = ObstructionHelper
                                    .buildPolygon(wedges.get(i));
                            PolyPoint pp = ObstructionHelper.polyPoint(sp);
                            if (wedge.contains(pp)) {
                                ret.get(i).add(endPoint);
                                break;
                            }
                        }
                    }
                }

                /*for (int i = TOP; i <= LEFT; i++) {
                    List<SurveyPoint> hitPoints = ObstructionHelper
                            .lineHitTest(lo, wedges.get(i));
                    for (SurveyPoint sp : hitPoints) {
                        double endAlt = sp.getHAE();
                        if (!sp.alt.isValid())
                            endAlt = 0;

                        double[] RangeAngleElev = Conversions
                                .CalculateRangeAngleElev(survey.center.lat,
                                        survey.center.lon,
                                        surveyAlt, sp.lat, sp.lon,
                                        endAlt + lo.height);

                        //Check to see if the Elevation, in degrees, exceeds the max elevation angle for appr/dep.
                        double elev_deg = RangeAngleElev[2];
                        if (IsOutsideHLZ(survey, RangeAngleElev, HLZPGon, sp)
                                && elev_deg > 0) {
                            PointObstruction endPoint = new PointObstruction(sp);
                            endPoint.uid = lo.uid;
                            endPoint.remark = lo.remarks;
                            endPoint.type = lo.type;
                            endPoint.group = lo.group;
                            endPoint.height = lo.height;
                            endPoint.speed = elev_deg;
                            ret.get(i).add(endPoint);
                        }
                    }
                }*/
            }
            lineCursor.close();
        }
        // Sort descending by GSR (elevation angle in degrees)
        PointObstruction[][] retArr = new PointObstruction[LEFT + 1][];
        for (int i = TOP; i <= LEFT; i++) {
            List<PointObstruction> quad = ret.get(i);
            if (!quad.isEmpty()) {
                Collections.sort(quad, gsrComp);
                retArr[i] = quad.toArray(new PointObstruction[quad.size()]);
            } else
                retArr[i] = null;
        }
        return retArr;
    }

    /**
     * Given a point and a line obstruction:
     * Find the points on each line segment closest to the point
     * @param center Center point
     * @param line Line obstruction
     * @return List of closest points
     */
    public static List<SurveyPoint> closestPointsFromCenter(
            SurveyPoint center, LineObstruction line) {
        List<SurveyPoint> ret = new ArrayList<SurveyPoint>();
        for (int i = 1; i < line.points.size(); i++) {
            SurveyPoint start = line.points.get(i - 1), end = line.points
                    .get(i);
            double[] lineRA = Conversions.calculateRangeAngle(start, end);
            double[] centerRA = Conversions.calculateRangeAngle(start, center);
            double angle = Conversions.deg360(centerRA[1] - lineRA[1]);
            double vert = centerRA[0] * Math.cos(Math.toRadians(angle));
            if (vert > 0 && vert < lineRA[0]) {
                double[] lp = Conversions.AROffset(start.lat, start.lon,
                        lineRA[1], vert);
                SurveyPoint hit = new SurveyPoint(lp[0], lp[1]);
                hit.setHAE(Math.max(start.getHAE(), end.getHAE()));
                ret.add(hit);
            }
            ret.add(start);
        }
        ret.add(line.points.get(line.points.size() - 1));
        return ret;
    }

    private static final Comparator<PointObstruction> gsrComp = new Comparator<PointObstruction>() {
        @Override
        public int compare(PointObstruction lhs, PointObstruction rhs) {
            return Double.compare(rhs.speed, lhs.speed);
        }
    };

    private static boolean IsOutsideHLZ(SurveyData surveyData,
            double[] RangeAngleElev, Polygon hLZPGon, SurveyPoint endPoint) {

        if (surveyData.circularAZ) {
            return RangeAngleElev[0] > surveyData.getRadius();
        } else {
            //build a polygon and work it out?
            //LOU fix me
            return !(hLZPGon
                    .contains(new PolyPoint(endPoint.lat, endPoint.lon)));
        }
    }
}
