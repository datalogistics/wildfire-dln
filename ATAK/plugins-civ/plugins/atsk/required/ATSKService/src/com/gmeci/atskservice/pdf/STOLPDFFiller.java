
package com.gmeci.atskservice.pdf;

import android.content.Context;
import android.support.v4.app.NotificationCompat.Builder;
import android.util.Log;

import com.gmeci.core.ATSKConstants;
import com.gmeci.core.SurveyData;
import com.gmeci.core.SurveyPoint;
import com.gmeci.helpers.AZHelper;
import com.gmeci.helpers.PolygonHelper;
import com.gmeci.conversions.Conversions;
import com.lowagie.text.DocumentException;

import java.io.IOException;
import java.lang.String;

public class STOLPDFFiller extends AbstractPDFFiller {

    STOLPDFFiller(Context context) {
        super(context);
    }

    @Override
    public void fillNonPreferenceParts() throws IOException, DocumentException {

        UpdateNotification(survey, mBuilder);
        mBuilder.setProgress(100, 60, false);
        mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());

        //set isSTOL flag to look for the write PDF file name
        fillSTOLForm();

        set("surfaceName", survey.surface);

        mBuilder.setProgress(100, 70, false);
        mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());

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
        String ApproachMGRS = Conversions.GetMGRS(ApproachCenter[0],
                ApproachCenter[1]);
        if (!fields.setField("runway1Mgrs",
                ApproachMGRS)) {
            Log.e(TAG, "failed to write ApproachMGRS in form");
        }

        String ApproachLat = Conversions.GetLatDM(ApproachCenter[0]);
        if (!fields
                .setField("runway1Latitude", ApproachLat)) {
            Log.e(TAG, "failed to write ApproachLat in form");
        }
        String ApproachLon = Conversions.GetLonDM(ApproachCenter[1]);
        if (!fields
                .setField("runway1Longitude", ApproachLon)) {
            Log.e(TAG, "failed to write ApproachLon in form");
        }

        set("runway1Elevation",
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

        String DepartureMGRS = Conversions.GetMGRS(DepartureCenter[0],
                DepartureCenter[1]);
        if (!fields.setField("runway2Mgrs",
                DepartureMGRS)) {
            Log.e(TAG, "failed to write DepartureCenter MGRS in form");
        }
        String DepartureLat = Conversions.GetLatDM(DepartureCenter[0]);
        if (!fields.setField("runway2Latitude",
                DepartureLat)) {
            Log.e(TAG, "failed to write DepartureCenter Lat in form");
        }
        String DepartureLon = Conversions.GetLonDM(DepartureCenter[1]);
        if (!fields.setField("runway2Longitude",
                DepartureLon)) {
            Log.e(TAG, "failed to write DepartureCenter Lon in form");
        }
        set("runway2Elevation",
                getFeetMSLString(DepartureCenter[0], DepartureCenter[1],
                        survey.departureElevation));

        //section 6
        FillMagnecitSizeSection(survey);

        /**Section 9**/
        FillPDFCoordinateSection(survey, mBuilder);

        mBuilder.setContentText("STOL PDF Complete").setProgress(0, 0, false);
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
        String GridZone = Conversions.GetMGRS(RealCenter[0],
                RealCenter[1]);
        if (GridZone.length() > 3) {
            if (!Character.isDigit(GridZone.charAt(2)))
                GridZone = GridZone.substring(0, 3);
            else
                GridZone = GridZone.substring(0, 2);
        }

        set("gridZone", GridZone);

        String GridEasting = Conversions.GetUTMEastingShort(RealCenter[0],
                RealCenter[1]);
        set("easting", GridEasting);

        String GridNorthing = Conversions.GetUTMNorthingShort(RealCenter[0],
                RealCenter[1]);
        set("northing", GridNorthing);

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
        String Length = String.format("%.0f ft",
                survey.getLength(false) * METERS_2_FEET);

        set((String) "length", Length);

        String Width = String.format("%.0f ft", survey.width
                * METERS_2_FEET);
        set("width", Width);

        String TrueString = String.format("%.1f",
                Conversions.deg360(survey.angle));
        set("runway1True", TrueString);
        TrueString = String.format("%.1f",
                Conversions.deg360(survey.angle + 180));
        set("runway2True", TrueString);

        String MagString = String.format("%.1f",
                Conversions.GetMagAngle(survey.angle,
                        survey.center.lat, survey.center.lon));
        set("runway1Magnetic", MagString);
        MagString = String.format("%.1f", Conversions.GetMagAngle(
                survey.angle + 180, survey.center.lat,
                survey.center.lon));
        set("runway2Magnetic", MagString);

        String GridString = String.format("%.1f", Conversions.deg360(
                Conversions.GetMGRSHeadingOffset((float) survey.center.lat,
                        survey.center.lon,
                        survey.angle)));
        set("runway1Grid", GridString);
        GridString = String.format("%.1f", Conversions.deg360(
                Conversions.GetMGRSHeadingOffset((float) survey.center.lat,
                        survey.center.lon,
                        survey.angle + 180)));
        set("runway2Grid", GridString);

        set("runway1VariationSource", "2015");
        set("datum", "EGM-96");

        set("runway1LdtFrGsr",
                Conversions.ConvertGlideSlopeAngleToRatio(Math
                        .abs(survey.approachGlideSlopeDeg), true));
        set("runway2LdtFrGsr",
                Conversions.ConvertGlideSlopeAngleToRatio(Math
                        .abs(survey.departureGlideSlopeDeg), true));

        set(
                "runway1DisplacedThreshold",
                String.format("%.0fft", Conversions.M2F
                        * survey.aDisplacedThreshold));
        set(
                "runway2DisplacedThreshold",
                String.format("%.0fft", Conversions.M2F
                        * survey.aDisplacedThreshold));
        //LOU look for the asymectrical Overrun
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
        set("runway2Mgrs2",
                Conversions.GetMGRS(CenterOfDep.lat, CenterOfDep.lon));

        set("runway1Label2",
                getRunwayName(survey.angle, survey.center.lat,
                        survey.center.lon));
        set("runway2Label2",
                getRunwayName(survey.angle + 180, survey.center.lat,
                        survey.center.lon));

    }

    public void fillSTOLForm() throws IOException, DocumentException {

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
        return "STOL Form(V6).pdf";
    }

}
