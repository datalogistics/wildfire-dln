
package com.gmeci.atskservice.pdf;

import android.content.Context;
import android.support.v4.app.NotificationCompat.Builder;

import com.gmeci.core.ATSKConstants;
import com.gmeci.core.SurveyData;
import com.gmeci.core.SurveyPoint;
import com.gmeci.helpers.AZHelper;
import com.gmeci.helpers.PolygonHelper;
import com.gmeci.conversions.Conversions;
import com.lowagie.text.DocumentException;

import java.io.IOException;
import java.lang.String;

public class LZPDFFiller extends AbstractPDFFiller {

    LZPDFFiller(Context context) {
        super(context);
    }

    @Override
    public void fillNonPreferenceParts() throws IOException, DocumentException {

        UpdateNotification(survey, mBuilder);
        mBuilder.setProgress(100, 60, false);
        mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());

        if (survey.surveyIsLTFW()) {
        } else {
            fillLZForm();
        }

        set("approachEndOverrunLengthAndUnits",
                String.format("%.0f ft", survey.edges.ApproachOverrunLength_m
                        * METERS_2_FEET));

        set("departEndOverrunLengthAndUnits",
                String.format("%.0f ft", survey.edges.DepartureOverrunLength_m
                        * METERS_2_FEET));

        String gsr = String.format("App:%s Dep:%s",
                Conversions.ConvertGlideSlopeAngleToRatio(Math
                        .abs(survey.approachGlideSlopeDeg), true),
                Conversions.ConvertGlideSlopeAngleToRatio(Math
                        .abs(survey.departureGlideSlopeDeg), true));

        if (survey.approachGlideSlopeDeg <
                survey.getMetaDouble("stdApproachOverrun",
                        survey.edges.ApproachOverrunLength_m)
                ||
                survey.departureGlideSlopeDeg <
                survey.getMetaDouble("stdDepartureOverrun",
                        survey.edges.DepartureOverrunLength_m)) {
            gsr += " (See Penetrations)";
        }

        set("glideSlopeRatio", gsr);

        if (survey.edges.LongitudinalGradientInterval <= (double) -1000)
            set("longitudinalRunwayGradient",
                    String.format("NONE"));
        else
            set("longitudinalRunwayGradient",
                    GetGradientString(survey.edges.LongitudinalGradientOverall));

        set("surfaceName", survey.surface);
        set("leftShoulderAndUnits",
                String.format("%.0f ft", Conversions.M2F
                        * survey.edges.ShoulderWidth_m));
        set("rightClearZoneAndUnits",
                String.format("%.0f ft", Conversions.M2F
                        * survey.edges.ShoulderWidth_m));
        set("leftClearZoneAndUnits",
                String.format("%.0f ft", Conversions.M2F
                        * survey.edges.GradedAreaWidth_m));
        set("rightShoulderAndUnits",
                String.format("%.0f ft", Conversions.M2F
                        * survey.edges.GradedAreaWidth_m));

        mBuilder.setProgress(100, 70, false);
        mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());

        set("leftShoulderAndUnits",
                String.format("%.0f ft", Conversions.M2F
                        * survey.edges.ShoulderWidth_m));
        set("rightShoulderAndUnits",
                String.format("%.0f ft", Conversions.M2F
                        * survey.edges.ShoulderWidth_m));
        set("leftClearZoneAndUnits",
                String.format("%.0f ft", Conversions.M2F
                        * survey.edges.GradedAreaWidth_m));
        set("rightClearZoneAndUnits",
                String.format("%.0f ft", Conversions.M2F
                        * survey.edges.GradedAreaWidth_m));

        /**Gradient**/
        set("gradientLeftShoulderPdf",
                GetGradientString(survey.edges.LeftShoulderGradient));
        set("gradientRightShoulderPdf",
                GetGradientString(survey.edges.RightShoulderGradient));
        set("gradientLeftGradedAreaPdf",
                GetGradientString(survey.edges.LeftGradedAreaGradient));
        set("gradientRightGradedAreaPdf",
                GetGradientString(survey.edges.RightGradedAreaGradient));
        set("gradientLeftShoulderPdf",
                GetGradientString(survey.edges.LeftShoulderGradient));
        set("gradientRightShoulderPdf",
                GetGradientString(survey.edges.RightShoulderGradient));
        set("gradientLeftGradedAreaPdf",
                GetGradientString(survey.edges.LeftGradedAreaGradient));
        set("gradientRightGradedAreaPdf",
                GetGradientString(survey.edges.RightGradedAreaGradient));
        set("gradientLeftTransitionAreaPdf",
                GetGradientString(survey.edges.LeftMaintainedAreaGradient));
        set("gradientRightTransitionAreaPdf",
                GetGradientString(survey.edges.RightMaintainedAreaGradient));

        /**Runway**/

        double leftHalf = survey.edges.LeftHalfRunwayGradient;
        double rightHalf = survey.edges.RightHalfRunwayGradient;

        if (Math.abs(leftHalf) <= 0.5 || Math.abs(leftHalf) >= 3.0) {
            set("gradientLeftHalfRunwayPdf", GetGradientString(leftHalf)
                    + " (SEE REMARK)");
        } else {
            set("gradientLeftHalfRunwayPdf", GetGradientString(leftHalf));
        }

        if (Math.abs(rightHalf) <= 0.5 || Math.abs(rightHalf) >= 3.0) {
            set("gradientRightHalfRunwayPdf", GetGradientString(rightHalf)
                    + " (SEE REMARK)");
        } else {
            set("gradientRightHalfRunwayPdf", GetGradientString(rightHalf));
        }

        mBuilder.setProgress(100, 75, false);
        mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());

        /**Approach**/
        double ApproachCenter[] = Conversions.AROffset(
                survey.center.lat,
                survey.center.lon,
                180 + survey.angle,
                survey.getLength(false)
                        / 2
                        + PolygonHelper.GetOverrunOffset(true,
                                survey));
        set("approachEndMgrs",
                Conversions.GetMGRS(ApproachCenter[0], ApproachCenter[1]));

        set("approachEndLatitude", Conversions.GetLatDM(ApproachCenter[0]));

        set("approachEndLongitude", Conversions.GetLonDM(ApproachCenter[1]));

        set("approachEndElevationAndUnits",
                getFeetMSLString(ApproachCenter[0], ApproachCenter[1],
                        survey.approachElevation));

        /**Departure**/
        double DepartureCenter[] = Conversions.AROffset(
                survey.center.lat,
                survey.center.lon,
                survey.angle,
                survey.getLength(false)
                        / 2
                        + PolygonHelper.GetOverrunOffset(false,
                                survey));

        set("departureEndMgrs",
                Conversions.GetMGRS(DepartureCenter[0], DepartureCenter[1]));

        set("departureEndLatitude",
                Conversions.GetLatDM(DepartureCenter[0]));

        set("departureEndLongitude",
                Conversions.GetLonDM(DepartureCenter[1]));

        set("departureEndElevationAndUnits",
                getFeetMSLString(DepartureCenter[0], DepartureCenter[1],
                        survey.departureElevation));

        set("highestElevationAndUnits",
                getFeetMSLString(survey.center.lat, survey.center.lon,
                        survey.highestElevation));

        //section 6
        FillMagnecitSizeSection(survey);

        /**Section 9**/
        FillPDFCoordinateSection(survey, mBuilder);

        mBuilder.setContentText("LZ PDF Complete").setProgress(0, 0, false);
        mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());

        mBuilder.setProgress(100, 80, false);
        mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());
        mBuilder.setProgress(100, 90, false);
        mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());
    }

    private void FillPDFCoordinateSection(SurveyData survey,
            Builder mBuilder) throws IOException, DocumentException {

        double RealCenter[] = Conversions.AROffset(survey.center.lat,
                survey.center.lon, survey.angle,
                PolygonHelper.GetOverrunOffset(false, survey));

        final String GridZone = getGridZone();
        set("lzCoordGridZone", GridZone);

        String GridEasting = Conversions.GetUTMEastingShort(RealCenter[0],
                RealCenter[1]);
        set("lzCoordEasting", GridEasting);

        String GridNorthing = Conversions.GetUTMNorthingShort(RealCenter[0],
                RealCenter[1]);
        set("lzCoordNorthing", GridNorthing);

        String CenterMGRS = Conversions.GetMGRS(RealCenter[0], RealCenter[1]);
        set("centerPointMgrs", CenterMGRS);

        String CenterLat = Conversions.GetLatDM(RealCenter[0]);
        set("centerPointLatitude", CenterLat);

        String CenterLon = Conversions.GetLonDM(RealCenter[1]);
        set("centerPointLongitude", CenterLon);

        set("longitudinalRunwayGradient",
                GetGradientString(survey.edges.LongitudinalGradientOverall));
        set("runway2LongitudinalGradient",
                GetGradientString(-1
                        * survey.edges.LongitudinalGradientOverall));

        String BothGradients = String.format("%sL %sR",
                GetGradientString(survey.edges.LeftHalfRunwayGradient),
                GetGradientString(survey.edges.RightHalfRunwayGradient));
        String BothGradientsInverse = String.format("%sL %sR",
                GetGradientString(survey.edges.RightHalfRunwayGradient),
                GetGradientString(survey.edges.LeftHalfRunwayGradient));
        set("runway1TransverseGradient",
                BothGradients);
        set("runway2TransverseGradient",
                BothGradientsInverse);

        if (survey.center.circularError < (3f / 4f)
                * ATSKConstants.DEFAULT_MAP_CLICK_ERROR_M) {
            String[] states = fields.getAppearanceStates("gpsDerivedInd");
            if (states != null)
                fields.setField("gpsDerivedInd", states[0]);
        } else {
            String[] states = fields.getAppearanceStates("notGpsDerived");
            if (states != null)
                fields.setField("notGpsDerived", states[0]);
        }
        mBuilder.setProgress(100, 55, false);
        mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());
    }

    private void FillMagnecitSizeSection(SurveyData survey)
            throws IOException, DocumentException {
        set((String) "lengthAndUnits", getFeetString(survey.getLength(false)));
        set("widthAndUnits", getFeetString(survey.width));

        set("elevation",
                getFeetMSLString(survey.center.lat, survey.center.lon,
                        survey.center.getHAE()));

        set("true1AndUnits", getAngleAndInverseString(survey.angle));

        set("magnetic1AndUnits",
                getAngleAndInverseString(Conversions.GetMagAngle(survey.angle,
                        survey.center.lat, survey.center.lon)));

        set("grid1AndUnits",
                getAngleAndInverseString(Conversions.GetMGRSHeadingOffset(
                        (float) survey.center.lat, survey.center.lon,
                        survey.angle)));

        set("sourceName", "WMM 2015");
        set("spheroid", "WGS-84");
        set("datum", "EGM-96");
        set("spheroidAndDatum", "WGS-84/EGM-96");

        set("runway1LdtFrGsr",
                Conversions.ConvertGlideSlopeAngleToRatio(Math
                        .abs(survey.approachGlideSlopeDeg), true));
        set("runway2LdtFrGsr",
                Conversions.ConvertGlideSlopeAngleToRatio(Math
                        .abs(survey.departureGlideSlopeDeg), true));

        set("runway1DisplacedThreshold",
                getFeetString(survey.aDisplacedThreshold));
        set("runway2DisplacedThreshold",
                getFeetString(survey.dDisplacedThreshold));

        SurveyPoint CenterOfApp = AZHelper.CalculateCenterOfEdge(
                survey.angle, survey.getLength(false)
                        + (survey.aDisplacedThreshold * 2),
                survey.center, true);
        SurveyPoint CenterOfDep = AZHelper.CalculateCenterOfEdge(
                survey.angle, survey.getLength(false)
                        + (survey.dDisplacedThreshold * 2),
                survey.center, false);

        set("runway1Latitude2",
                Conversions.GetLatDM(CenterOfApp.lat));
        set("runway1Longitude2",
                Conversions.GetLonDM(CenterOfApp.lon));
        set("runway1Mgrs2",
                Conversions.GetMGRS(CenterOfApp.lat, CenterOfApp.lon));

        set("runway2Latitude2",
                Conversions.GetLatDM(CenterOfDep.lat));
        set("runway2Longitude2",
                Conversions.GetLonDM(CenterOfDep.lon));

        set("runway1Label2",
                getRunwayName(survey.angle, survey.center.lat,
                        survey.center.lon));
        set("runway2Label2",
                getRunwayName(survey.angle + 180, survey.center.lat,
                        survey.center.lon));
    }

    public void fillLZForm() throws IOException, DocumentException {

        String TrueString = String.format("%.1f/%.1f",
                Conversions.deg360(survey.angle),
                Conversions.deg360(survey.angle + 180));
        set("true1AndUnits", TrueString);

        String GridString = String.format("%.1f/%.1f", Conversions.deg360(
                Conversions.GetMGRSHeadingOffset((float) survey.center.lat,
                        survey.center.lon,
                        survey.angle)),
                Conversions.deg360(Conversions.GetMGRSHeadingOffset(
                        survey.center.lat, survey.center.lon,
                        survey.angle + 180)));
        set("grid1AndUnits", GridString);

        String MagString = String.format("%.1f/%.1f", Conversions
                .GetMagAngle(survey.angle,
                        survey.center.lat, survey.center.lon),
                Conversions.GetMagAngle(
                        survey.angle + 180,
                        survey.center.lat, survey.center.lon));
        set("magnetic1AndUnits", MagString);
    }

    private boolean UpdateNotification(SurveyData survey,
            Builder mBuilder) {
        mBuilder.setContentTitle("ATSK Building PDF")
                .setContentText(
                        String.format("%s PDF In Progress",
                                survey.getSurveyName()));

        mBuilder.setSmallIcon(com.gmeci.atskservice.R.drawable.lz_small);
        mBuilder.setProgress(100, 5, false);
        mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());

        return true;
    }

    @Override
    protected String getBlankFileName(SurveyData survey) {
        if (survey.surveyIsLTFW())
            return "LTFW Form(V6).pdf";
        else
            return "LZ Form(V6).pdf";
    }

}
