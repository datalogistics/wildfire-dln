
package com.gmeci.helpers;

import com.gmeci.core.ATSKConstants;
import com.gmeci.core.LineObstruction;
import com.gmeci.core.SurveyData;
import com.gmeci.core.SurveyData.AZ_TYPE;
import com.gmeci.core.SurveyPoint;
import com.gmeci.conversions.Conversions;

import java.util.ArrayList;

public class PolygonHelper {

    public static LineObstruction getAZOutline(SurveyData survey,
            boolean isCurrentSurvey) {
        ArrayList<SurveyPoint> corners;
        if (survey.getType() == AZ_TYPE.LZ) {
            corners = AZHelper.getAZCorners(survey, true);
        } else {
            corners = AZHelper.getAZCorners(survey, false);
        }
        LineObstruction azOutline = new LineObstruction();
        azOutline.filled = isCurrentSurvey;
        azOutline.points = corners;
        SurveyPoint closingPoint = new SurveyPoint(corners.get(0).lat,
                corners.get(0).lon);
        azOutline.points.add(closingPoint);
        if (survey.getType() == AZ_TYPE.DZ) {
            azOutline.type = ATSKConstants.DZ_MAIN;
        } else if (survey.getType() == AZ_TYPE.FARP) {
            azOutline.type = ATSKConstants.FARP_AC_TYPE;
        } else if (survey.getType() == AZ_TYPE.HLZ) {
            azOutline.type = ATSKConstants.HLZ_MAIN;
        } else {
            azOutline.type = ATSKConstants.LZ_MAIN;
        }
        azOutline.uid = survey.uid + azOutline.type;

        return azOutline;
    }

    public static LineObstruction getHLZApproachLine(SurveyData survey) {
        boolean inside = survey.getMetaBoolean(
                ATSKConstants.MAG_ARROWS_INSIDE, false);

        double[] offset;
        LineObstruction approachLine = new LineObstruction();
        approachLine.type = ATSKConstants.HLZ_APPROACH;
        approachLine.uid = survey.uid + ATSKConstants.HLZ_APPROACH;
        approachLine.filled = false;

        double maxDim = Math.max(survey.getLength(), survey.width);
        double minDim = survey.circularAZ ? survey.getRadius()
                : Math.min(survey.getLength(), survey.width) / 2;
        offset = Conversions.AROffset(survey.center.lat, survey.center.lon,
                survey.approachAngle - 180, inside ? 1 : maxDim * 1.1);
        SurveyPoint startPoint = new SurveyPoint(offset[0], offset[1]);

        offset = Conversions.AROffset(survey.center.lat, survey.center.lon,
                survey.approachAngle - 180,
                inside ? minDim - 1 : maxDim * 3);
        SurveyPoint endPoint = new SurveyPoint(offset[0], offset[1]);

        approachLine.points.add(endPoint);
        approachLine.points.add(startPoint);
        // Arrow
        double tailLen = inside ? minDim * 0.1 : maxDim * 0.1;
        addArrowHead(approachLine, survey.approachAngle, tailLen);
        return approachLine;
    }

    public static LineObstruction getHLZDepartureLine(SurveyData survey) {
        boolean inside = survey.getMetaBoolean(
                ATSKConstants.MAG_ARROWS_INSIDE, false);

        double[] offset;
        LineObstruction departureLine = new LineObstruction();
        departureLine.type = ATSKConstants.HLZ_DEPARTURE;
        departureLine.uid = survey.uid + ATSKConstants.HLZ_DEPARTURE;
        departureLine.filled = false;

        double maxDim = Math.max(survey.getLength(), survey.width);
        double minDim = survey.circularAZ ? survey.getRadius()
                : Math.min(survey.getLength(), survey.width) / 2;
        offset = Conversions.AROffset(survey.center.lat, survey.center.lon,
                survey.departureAngle, inside ? 1 : maxDim);
        SurveyPoint startPoint = new SurveyPoint(offset[0], offset[1]);
        offset = Conversions.AROffset(offset[0], offset[1],
                survey.departureAngle, inside ? minDim - 1 : maxDim * 2);
        SurveyPoint endPoint = new SurveyPoint(offset[0], offset[1]);
        departureLine.points.add(startPoint);
        departureLine.points.add(endPoint);
        // Arrow head
        double tailLen = inside ? minDim * 0.1 : maxDim * 0.1;
        addArrowHead(departureLine, survey.departureAngle, tailLen);
        return departureLine;
    }

    public static LineObstruction getDZHeadingLine(SurveyData survey) {

        double[] offset;
        LineObstruction headingLine = new LineObstruction();
        headingLine.type = ATSKConstants.DZ_HEADING;
        headingLine.uid = survey.uid + ATSKConstants.DZ_HEADING;
        headingLine.filled = false;

        double len = survey.circularAZ ? 0 : survey.getLength() / 2;
        offset = Conversions.AROffset(survey.center.lat, survey.center.lon,
                survey.angle, len * 0.5);
        SurveyPoint startPoint = new SurveyPoint(offset[0], offset[1]);
        offset = Conversions.AROffset(survey.center.lat, survey.center.lon,
                survey.angle, len * 0.75);
        SurveyPoint endPoint = new SurveyPoint(offset[0], offset[1]);
        headingLine.points.add(startPoint);
        headingLine.points.add(endPoint);

        // Arrow head
        double tailLen = len * 0.1;
        addArrowHead(headingLine, survey.angle, tailLen);
        return headingLine;
    }

    private static void addArrowHead(LineObstruction lo, double angle,
            double len) {
        SurveyPoint last = lo.points.get(lo.points.size() - 1);
        double[] tail1 = Conversions.AROffset(
                last.lat, last.lon, angle - 135, len);
        double[] tail2 = Conversions.AROffset(
                last.lat, last.lon, angle + 135, len);
        lo.points.add(new SurveyPoint(tail1[0], tail1[1]));
        lo.points.add(last);
        lo.points.add(new SurveyPoint(tail2[0], tail2[1]));
    }

    public static LineObstruction getLeftShoulder(SurveyData survey) {
        LineObstruction shoulderOutline = new LineObstruction();
        shoulderOutline.type = ATSKConstants.LZ_SHOULDER;
        shoulderOutline.uid = survey.uid + "L" + ATSKConstants.LZ_SHOULDER;
        ArrayList<SurveyPoint> corners = AZHelper.getShoulderlArea(survey,
                survey.edges.ShoulderWidth_m, false);
        shoulderOutline.points = corners;
        SurveyPoint closingPoint = new SurveyPoint(corners.get(0).lat,
                corners.get(0).lon);
        shoulderOutline.points.add(closingPoint);
        return shoulderOutline;
    }

    public static LineObstruction getRightShoulder(SurveyData survey) {
        LineObstruction shoulderOutline = new LineObstruction();

        shoulderOutline.type = ATSKConstants.LZ_SHOULDER;
        shoulderOutline.uid = survey.uid + "R" + ATSKConstants.LZ_SHOULDER;
        ArrayList<SurveyPoint> corners = AZHelper.getShoulderlArea(survey,
                survey.edges.ShoulderWidth_m, true);
        shoulderOutline.points = corners;
        SurveyPoint closingPoint = new SurveyPoint(corners.get(0).lat,
                corners.get(0).lon);
        shoulderOutline.points.add(closingPoint);
        return shoulderOutline;
    }

    public static LineObstruction getLeftGradedArea(SurveyData survey) {
        LineObstruction gradedOutline = new LineObstruction();

        gradedOutline.type = ATSKConstants.LZ_GRADED;
        gradedOutline.uid = survey.uid + "L" + ATSKConstants.LZ_GRADED;

        ArrayList<SurveyPoint> corners = AZHelper.getLateralArea(survey,
                survey.edges.GradedAreaWidth_m, false,
                survey.edges.ShoulderWidth_m, !survey.surveyIsLTFW());
        gradedOutline.points = corners;
        SurveyPoint closingPoint = new SurveyPoint(corners.get(0).lat,
                corners.get(0).lon);
        gradedOutline.points.add(closingPoint);
        return gradedOutline;
    }

    public static LineObstruction getRightGradedArea(SurveyData survey) {
        LineObstruction gradedOutline = new LineObstruction();

        gradedOutline.type = ATSKConstants.LZ_GRADED;
        gradedOutline.uid = survey.uid + "R" + ATSKConstants.LZ_GRADED;

        ArrayList<SurveyPoint> corners = AZHelper.getLateralArea(survey,
                survey.edges.GradedAreaWidth_m, true,
                survey.edges.ShoulderWidth_m, !survey.surveyIsLTFW());
        gradedOutline.points = corners;
        SurveyPoint closingPoint = new SurveyPoint(corners.get(0).lat,
                corners.get(0).lon);
        gradedOutline.points.add(closingPoint);
        return gradedOutline;
    }

    public static LineObstruction getLeftMaintainedArea(SurveyData survey) {
        LineObstruction shoulderOutline = new LineObstruction();

        shoulderOutline.type = ATSKConstants.LZ_MAINTAINED;
        shoulderOutline.uid = survey.uid + "L" + ATSKConstants.LZ_MAINTAINED;

        ArrayList<SurveyPoint> corners = AZHelper.getLateralArea(survey,
                survey.edges.MaintainedAreaWidth_m, false,
                survey.edges.ShoulderWidth_m + survey.edges.GradedAreaWidth_m,
                true);
        shoulderOutline.points = corners;
        SurveyPoint closingPoint = new SurveyPoint(corners.get(0).lat,
                corners.get(0).lon);
        shoulderOutline.points.add(closingPoint);
        return shoulderOutline;
    }

    public static LineObstruction getLeftGradientLimitArea(SurveyData survey) {
        LineObstruction gradientLimitOutline = new LineObstruction();

        gradientLimitOutline.type = ATSKConstants.LZ_MAINTAINED;
        gradientLimitOutline.uid = ATSKConstants.GRADIENT_LEFT_LIMIT_UID;
        gradientLimitOutline.group = ATSKConstants.DEFAULT_GROUP;
        gradientLimitOutline.width = 0;

        ArrayList<SurveyPoint> corners = AZHelper.getLateralArea(survey,
                survey.getFullWidth() / 2,
                false, survey.edges.ShoulderWidth_m
                        + survey.edges.GradedAreaWidth_m
                        + survey.edges.MaintainedAreaWidth_m, false);
        gradientLimitOutline.points = corners;
        SurveyPoint closingPoint = new SurveyPoint(corners.get(0).lat,
                corners.get(0).lon);
        gradientLimitOutline.points.add(closingPoint);
        return gradientLimitOutline;
    }

    public static LineObstruction getRightGradientLimitArea(SurveyData survey) {
        LineObstruction gradientLimitOutline = new LineObstruction();

        gradientLimitOutline.type = ATSKConstants.LZ_MAINTAINED;
        gradientLimitOutline.uid = ATSKConstants.GRADIENT_RIGHT_LIMIT_UID;
        gradientLimitOutline.group = ATSKConstants.DEFAULT_GROUP;
        gradientLimitOutline.width = 0;

        ArrayList<SurveyPoint> corners = AZHelper.getLateralArea(survey,
                survey.getFullWidth() / 2,
                true, survey.edges.ShoulderWidth_m
                        + survey.edges.GradedAreaWidth_m
                        + survey.edges.MaintainedAreaWidth_m, false);
        gradientLimitOutline.points = corners;
        SurveyPoint closingPoint = new SurveyPoint(corners.get(0).lat,
                corners.get(0).lon);
        gradientLimitOutline.points.add(closingPoint);
        return gradientLimitOutline;
    }

    public static LineObstruction getLongitudinalLimitArea(SurveyData survey) {
        LineObstruction lonLimit = new LineObstruction();

        lonLimit.type = ATSKConstants.LZ_MAINTAINED;
        lonLimit.uid = ATSKConstants.LONGITUDINAL_LIMIT_UID;
        lonLimit.group = ATSKConstants.DEFAULT_GROUP;
        lonLimit.width = 0;
        lonLimit.closed = true;
        lonLimit.filled = true;

        ArrayList<SurveyPoint> corners = AZHelper.getLateralArea(survey,
                ATSKConstants.LONGITUDINAL_RIBBON_WIDTH_M,
                true, -(survey.width + ATSKConstants
                .LONGITUDINAL_RIBBON_WIDTH_M) / 2, false);
        lonLimit.points = corners;
        SurveyPoint closingPoint = new SurveyPoint(corners.get(0).lat,
                corners.get(0).lon);
        lonLimit.points.add(closingPoint);
        return lonLimit;
    }

    public static LineObstruction getRightMaintainedArea(SurveyData survey) {
        LineObstruction maintainedOutline = new LineObstruction();

        maintainedOutline.type = ATSKConstants.LZ_MAINTAINED;
        maintainedOutline.uid = survey.uid + "R" + ATSKConstants.LZ_MAINTAINED;

        ArrayList<SurveyPoint> corners = AZHelper.getLateralArea(survey,
                survey.edges.MaintainedAreaWidth_m, true,
                survey.edges.ShoulderWidth_m + survey.edges.GradedAreaWidth_m,
                true);
        maintainedOutline.points = corners;
        SurveyPoint closingPoint = new SurveyPoint(corners.get(0).lat,
                corners.get(0).lon);
        maintainedOutline.points.add(closingPoint);
        return maintainedOutline;
    }

    public static LineObstruction getApproachOverrunArea(SurveyData survey) {
        LineObstruction overrunOutline = new LineObstruction();

        overrunOutline.type = ATSKConstants.LZ_OVERRUN;
        overrunOutline.uid = survey.uid + "A" + ATSKConstants.LZ_OVERRUN;

        ArrayList<SurveyPoint> corners = AZHelper.getOverrunCoordinates(survey,
                false, true/*survey.approachInside*/);
        overrunOutline.points = corners;
        SurveyPoint closingPoint = new SurveyPoint(corners.get(0).lat,
                corners.get(0).lon);
        overrunOutline.points.add(closingPoint);
        return overrunOutline;
    }

    public static ArrayList<LineObstruction> getApproachOverrunArrows(
            SurveyData survey) {
        ArrayList<LineObstruction> Arrows = new ArrayList<LineObstruction>();

        if (survey.width < 1.5)
            return Arrows;
        int ArrowCount = (int) (survey.edges.ApproachOverrunLength_m / (survey.width / 4));
        double ArrowHeight_m = survey.edges.ApproachOverrunLength_m
                / ArrowCount;

        //    double AngleOffset=180;
        //            AngleOffset=0;

        double Length = (survey.getLength(true) / 2);//+ survey.edges.ApproachOverrunLength_m;
        //    if(survey.approachInside)
        //        Length = (survey.Length_m/2);

        for (int i = 0; i < ArrowCount; i++) {
            double[] CenterOfArrowStartPosition = Conversions.AROffset(
                    survey.center.lat, survey.center.lon,
                    survey.angle + 180, Length - (ArrowHeight_m * i));
            ArrayList<SurveyPoint> ArrowPoints = DrawArrow(
                    CenterOfArrowStartPosition, survey.angle,
                    survey.width);
            LineObstruction lo = new LineObstruction();
            lo.uid = ATSKConstants.LZ_DEPARTURE_ARROW + i;
            lo.type = ATSKConstants.LZ_DEPARTURE_ARROW;
            lo.points = ArrowPoints;
            Arrows.add(lo);
        }

        return Arrows;
    }

    public static ArrayList<LineObstruction> getDepartureOverrunArrows(
            SurveyData survey) {
        ArrayList<LineObstruction> Arrows = new ArrayList<LineObstruction>();

        if (survey.width < 1.5)
            return Arrows;
        int ArrowCount = (int) (survey.edges.DepartureOverrunLength_m / (survey.width / 4));
        double ArrowHeight_m = survey.edges.DepartureOverrunLength_m
                / ArrowCount;

        //        double AngleOffset=0;
        double Length = (survey.getLength(true) / 2);//+ survey.edges.DepartureOverrunLength_m;

        //    if(survey.departureInside)    
        //        Length = (survey.Length_m/2);

        //            AngleOffset=180;
        for (int i = 0; i < ArrowCount; i++) {
            double[] CenterOfArrowStartPosition = Conversions.AROffset(
                    survey.center.lat, survey.center.lon,
                    survey.angle, Length - (ArrowHeight_m * i));
            ArrayList<SurveyPoint> ArrowPoints = DrawArrow(
                    CenterOfArrowStartPosition, survey.angle + 180,
                    survey.width);
            LineObstruction lo = new LineObstruction();
            lo.uid = ATSKConstants.LZ_APPROACH_ARROW + i;
            lo.type = ATSKConstants.LZ_APPROACH_ARROW;
            lo.points = ArrowPoints;
            Arrows.add(lo);
        }

        return Arrows;
    }

    private static ArrayList<SurveyPoint> DrawArrow(
            double[] centerOfArrowStartPosition, double angle, double width_m) {
        ArrayList<SurveyPoint> ArrowPoints = new ArrayList<SurveyPoint>();
        if (width_m < 1.5)
            return ArrowPoints;
        double[] StartPoint = Conversions.AROffset(
                centerOfArrowStartPosition[0], centerOfArrowStartPosition[1],
                angle + 90, width_m / 2);
        double[] MidPoint = Conversions.AROffset(centerOfArrowStartPosition[0],
                centerOfArrowStartPosition[1], angle, width_m / 4);
        double[] EndPoint = Conversions.AROffset(centerOfArrowStartPosition[0],
                centerOfArrowStartPosition[1], angle - 90, width_m / 2);
        ArrowPoints.add(new SurveyPoint(StartPoint[0], StartPoint[1]));
        ArrowPoints.add(new SurveyPoint(MidPoint[0], MidPoint[1]));
        ArrowPoints.add(new SurveyPoint(EndPoint[0], EndPoint[1]));
        return ArrowPoints;
    }

    public static LineObstruction getDepartureOverrunArea(SurveyData survey) {
        LineObstruction overrunOutline = new LineObstruction();

        overrunOutline.type = ATSKConstants.LZ_OVERRUN;
        overrunOutline.uid = survey.uid + "D" + ATSKConstants.LZ_OVERRUN;

        ArrayList<SurveyPoint> corners = AZHelper.getOverrunCoordinates(survey,
                true, true/*survey.departureInside*/);
        overrunOutline.points = corners;
        SurveyPoint closingPoint = new SurveyPoint(corners.get(0).lat,
                corners.get(0).lon);
        overrunOutline.points.add(closingPoint);
        return overrunOutline;
    }

    public static double GetOverrunOffset(boolean Approach, SurveyData survey) {
        if (!Approach)
            return (survey.edges.ApproachOverrunLength_m - survey.edges.DepartureOverrunLength_m) / 2;
        return (survey.edges.DepartureOverrunLength_m - survey.edges.ApproachOverrunLength_m) / 2;
    }

    public static LineObstruction getClearApproachTrapezoid(SurveyData survey) {

        LineObstruction appClear = new LineObstruction();
        appClear.type = ATSKConstants.LZ_CLEAR;
        appClear.uid = survey.uid + "AT" + ATSKConstants.LZ_CLEAR;

        double RealLength = survey.getLength(false) / 2.0f;
        //    if(survey.approachInside)
        {
            //        RealLength -= survey.edges.ApproachOverrunLength_m;
        }

        double nearCenter[] = Conversions.AROffset(survey.center.lat,
                survey.center.lon, survey.angle + 180, RealLength
                        + GetOverrunOffset(true, survey));
        double farCenter[] = Conversions.AROffset(survey.center.lat,
                survey.center.lon, survey.angle + 180, RealLength
                        + GetOverrunOffset(true, survey)
                        + survey.endClearZoneLength);
        double nearRight[] = Conversions
                .AROffset(nearCenter[0], nearCenter[1],
                        survey.angle + 90,
                        survey.endClearZoneInnerWidth / 2);
        double nearLeft[] = Conversions
                .AROffset(nearCenter[0], nearCenter[1],
                        survey.angle - 90,
                        survey.endClearZoneInnerWidth / 2);

        double farRight[] = Conversions
                .AROffset(farCenter[0], farCenter[1],
                        survey.angle + 90,
                        survey.endClearZoneOuterWidth / 2);
        double farLeft[] = Conversions
                .AROffset(farCenter[0], farCenter[1],
                        survey.angle - 90,
                        survey.endClearZoneOuterWidth / 2);

        appClear.points.add(new SurveyPoint(nearRight[0], nearRight[1]));
        appClear.points.add(new SurveyPoint(nearLeft[0], nearLeft[1]));
        appClear.points.add(new SurveyPoint(farLeft[0], farLeft[1]));
        appClear.points.add(new SurveyPoint(farRight[0], farRight[1]));
        appClear.points.add(new SurveyPoint(nearRight[0], nearRight[1]));

        return appClear;
    }

    public static LineObstruction getClearDepartureTrapezoid(SurveyData survey) {

        LineObstruction depClear = new LineObstruction();

        depClear.type = ATSKConstants.LZ_CLEAR;
        depClear.uid = survey.uid + "DT" + ATSKConstants.LZ_CLEAR;
        double RealLength = survey.getLength(false) / 2.0f;
        //if(survey.departureInside)
        {
            //        RealLength -= survey.edges.DepartureOverrunLength_m;
        }

        double nearCenter[] = Conversions.AROffset(survey.center.lat,
                survey.center.lon, survey.angle, RealLength
                        + GetOverrunOffset(false, survey));
        double farCenter[] = Conversions.AROffset(survey.center.lat,
                survey.center.lon, survey.angle, RealLength
                        + GetOverrunOffset(false, survey)
                        + survey.endClearZoneLength);

        double nearRight[] = Conversions
                .AROffset(nearCenter[0], nearCenter[1],
                        survey.angle + 90,
                        survey.endClearZoneInnerWidth / 2);
        double nearLeft[] = Conversions
                .AROffset(nearCenter[0], nearCenter[1],
                        survey.angle - 90,
                        survey.endClearZoneInnerWidth / 2);

        double farRight[] = Conversions
                .AROffset(farCenter[0], farCenter[1],
                        survey.angle + 90,
                        survey.endClearZoneOuterWidth / 2);
        double farLeft[] = Conversions
                .AROffset(farCenter[0], farCenter[1],
                        survey.angle - 90,
                        survey.endClearZoneOuterWidth / 2);
        depClear.points.add(new SurveyPoint(nearRight[0], nearRight[1]));
        depClear.points.add(new SurveyPoint(nearLeft[0], nearLeft[1]));
        depClear.points.add(new SurveyPoint(farLeft[0], farLeft[1]));
        depClear.points.add(new SurveyPoint(farRight[0], farRight[1]));
        depClear.points.add(new SurveyPoint(nearRight[0], nearRight[1]));
        return depClear;
    }

    public static LineObstruction getInnerApproachTrapezoid(SurveyData survey) {
        LineObstruction approachTrapezoid = new LineObstruction();
        approachTrapezoid.type = ATSKConstants.LZ_INNER_DEPARTURE;
        approachTrapezoid.uid = survey.uid + "IAT"
                + ATSKConstants.LZ_INNER_APPROACH;

        double RealLength = survey.getLength(false) / 2;
        //    if(survey.approachInside)
        //        RealLength-= survey.edges.ApproachOverrunLength_m;

        double nearCenter[] = Conversions.AROffset(
                survey.center.lat,
                survey.center.lon,
                survey.angle + 180,
                RealLength + survey.endClearZoneLength
                        + GetOverrunOffset(true, survey));
        double farCenter[] = Conversions.AROffset(
                survey.center.lat,
                survey.center.lon,
                survey.angle + 180,
                RealLength + survey.endClearZoneLength
                        + survey.approachInnerLength
                        + GetOverrunOffset(true, survey));

        double nearRight[] = Conversions
                .AROffset(nearCenter[0], nearCenter[1],
                        survey.angle + 90,
                        survey.endClearZoneOuterWidth / 2);
        double nearLeft[] = Conversions
                .AROffset(nearCenter[0], nearCenter[1],
                        survey.angle - 90,
                        survey.endClearZoneOuterWidth / 2);

        double farRight[] = Conversions.AROffset(farCenter[0], farCenter[1],
                survey.angle + 90, survey.approachInnerWidth / 2);
        double farLeft[] = Conversions.AROffset(farCenter[0], farCenter[1],
                survey.angle - 90, survey.approachInnerWidth / 2);
        approachTrapezoid.points
                .add(new SurveyPoint(nearRight[0], nearRight[1]));
        approachTrapezoid.points.add(new SurveyPoint(nearLeft[0], nearLeft[1]));
        approachTrapezoid.points.add(new SurveyPoint(farLeft[0], farLeft[1]));
        approachTrapezoid.points.add(new SurveyPoint(farRight[0], farRight[1]));
        approachTrapezoid.points
                .add(new SurveyPoint(nearRight[0], nearRight[1]));

        return approachTrapezoid;
    }

    public static LineObstruction getOuterApproachTrapezoid(SurveyData survey) {
        LineObstruction approachTrapezoid = new LineObstruction();
        approachTrapezoid.type = ATSKConstants.LZ_OUTER_APPROACH;
        approachTrapezoid.uid = survey.uid + "OAT"
                + ATSKConstants.LZ_OUTER_APPROACH;

        double RealLength = survey.getLength(false) / 2;
        //    if(survey.approachInside)
        //        RealLength-= survey.edges.ApproachOverrunLength_m;

        double nearCenter[] = Conversions.AROffset(
                survey.center.lat,
                survey.center.lon,
                survey.angle + 180,
                RealLength + survey.endClearZoneLength
                        + survey.approachInnerLength
                        + GetOverrunOffset(true, survey));
        double farCenter[] = Conversions.AROffset(
                survey.center.lat,
                survey.center.lon,
                survey.angle + 180,
                RealLength + survey.endClearZoneLength
                        + survey.approachInnerLength
                        + survey.approachOuterLength
                        + GetOverrunOffset(true, survey));

        double nearRight[] = Conversions.AROffset(nearCenter[0], nearCenter[1],
                survey.angle + 90, survey.approachInnerWidth / 2);
        double nearLeft[] = Conversions.AROffset(nearCenter[0], nearCenter[1],
                survey.angle - 90, survey.approachInnerWidth / 2);

        double farRight[] = Conversions.AROffset(farCenter[0], farCenter[1],
                survey.angle + 90, survey.approachOuterWidth / 2);
        double farLeft[] = Conversions.AROffset(farCenter[0], farCenter[1],
                survey.angle - 90, survey.approachOuterWidth / 2);
        approachTrapezoid.points
                .add(new SurveyPoint(nearRight[0], nearRight[1]));
        approachTrapezoid.points.add(new SurveyPoint(nearLeft[0], nearLeft[1]));
        approachTrapezoid.points.add(new SurveyPoint(farLeft[0], farLeft[1]));
        approachTrapezoid.points.add(new SurveyPoint(farRight[0], farRight[1]));
        approachTrapezoid.points
                .add(new SurveyPoint(nearRight[0], nearRight[1]));
        return approachTrapezoid;
    }

    public static LineObstruction getInnerDepartureTrapezoid(SurveyData survey) {
        LineObstruction departureTrapezoid = new LineObstruction();
        departureTrapezoid.type = ATSKConstants.LZ_INNER_DEPARTURE;
        departureTrapezoid.uid = survey.uid + "IDT"
                + ATSKConstants.LZ_INNER_DEPARTURE;

        double RealLength = survey.getLength(false) / 2;
        //    if(survey.departureInside)
        //        RealLength-= survey.edges.DepartureOverrunLength_m;

        double nearCenter[] = Conversions.AROffset(
                survey.center.lat,
                survey.center.lon,
                survey.angle,
                RealLength + survey.endClearZoneLength
                        + GetOverrunOffset(false, survey));
        double farCenter[] = Conversions.AROffset(
                survey.center.lat,
                survey.center.lon,
                survey.angle,
                RealLength + survey.endClearZoneLength
                        + survey.approachInnerLength
                        + GetOverrunOffset(false, survey));

        double nearRight[] = Conversions
                .AROffset(nearCenter[0], nearCenter[1],
                        survey.angle + 90,
                        survey.endClearZoneOuterWidth / 2);
        double nearLeft[] = Conversions
                .AROffset(nearCenter[0], nearCenter[1],
                        survey.angle - 90,
                        survey.endClearZoneOuterWidth / 2);

        double farRight[] = Conversions.AROffset(farCenter[0], farCenter[1],
                survey.angle + 90, survey.approachInnerWidth / 2);
        double farLeft[] = Conversions.AROffset(farCenter[0], farCenter[1],
                survey.angle - 90, survey.approachInnerWidth / 2);
        departureTrapezoid.points.add(new SurveyPoint(nearRight[0],
                nearRight[1]));
        departureTrapezoid.points
                .add(new SurveyPoint(nearLeft[0], nearLeft[1]));
        departureTrapezoid.points.add(new SurveyPoint(farLeft[0], farLeft[1]));
        departureTrapezoid.points
                .add(new SurveyPoint(farRight[0], farRight[1]));
        departureTrapezoid.points.add(new SurveyPoint(nearRight[0],
                nearRight[1]));
        return departureTrapezoid;
    }

    public static LineObstruction getOuterDepartureTrapezoid(SurveyData survey) {
        LineObstruction departureTrapezoid = new LineObstruction();
        departureTrapezoid.type = ATSKConstants.LZ_OUTER_DEPARTURE;
        departureTrapezoid.uid = survey.uid + "ODT"
                + ATSKConstants.LZ_OUTER_DEPARTURE;

        double RealLength = survey.getLength(false) / 2;
        //    if(survey.departureInside)
        //        RealLength-= survey.edges.DepartureOverrunLength_m;

        double nearCenter[] = Conversions.AROffset(
                survey.center.lat,
                survey.center.lon,
                survey.angle,
                RealLength + survey.endClearZoneLength
                        + survey.approachInnerLength
                        + GetOverrunOffset(false, survey));
        double farCenter[] = Conversions.AROffset(
                survey.center.lat,
                survey.center.lon,
                survey.angle,
                RealLength + survey.endClearZoneLength
                        + survey.approachInnerLength
                        + survey.approachOuterLength
                        + GetOverrunOffset(false, survey));

        double nearRight[] = Conversions.AROffset(nearCenter[0], nearCenter[1],
                survey.angle + 90, survey.approachInnerWidth / 2);
        double nearLeft[] = Conversions.AROffset(nearCenter[0], nearCenter[1],
                survey.angle - 90, survey.approachInnerWidth / 2);

        double farRight[] = Conversions.AROffset(farCenter[0], farCenter[1],
                survey.angle + 90, survey.approachOuterWidth / 2);
        double farLeft[] = Conversions.AROffset(farCenter[0], farCenter[1],
                survey.angle - 90, survey.approachOuterWidth / 2);
        departureTrapezoid.points.add(new SurveyPoint(nearRight[0],
                nearRight[1]));
        departureTrapezoid.points
                .add(new SurveyPoint(nearLeft[0], nearLeft[1]));
        departureTrapezoid.points.add(new SurveyPoint(farLeft[0], farLeft[1]));
        departureTrapezoid.points
                .add(new SurveyPoint(farRight[0], farRight[1]));
        departureTrapezoid.points.add(new SurveyPoint(nearRight[0],
                nearRight[1]));
        return departureTrapezoid;
    }

}
