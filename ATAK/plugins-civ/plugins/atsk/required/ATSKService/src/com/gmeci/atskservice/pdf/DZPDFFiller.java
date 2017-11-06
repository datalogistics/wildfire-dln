
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

public class DZPDFFiller extends AbstractPDFFiller {

    private final static double THIRD_METER = 1 / 3d;

    DZPDFFiller(Context context) {
        super(context);
    }

    @Override
    protected String getBlankFileName(SurveyData survey) {
        return "DZ Form(V6).pdf";
    }

    private boolean UpdateNotification(SurveyData survey,
            Builder mBuilder) {

        mBuilder.setContentTitle("ATSK Building DZ PDF")
                .setContentText(
                        String.format("DZ %s PDF In Progress",
                                survey.getSurveyName()));

        mBuilder.setSmallIcon(com.gmeci.atskservice.R.drawable.lz_small);

        mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());

        return true;
    }

    private boolean isValid(final double actLength, final double actWidth,
            final double reqLength,
            final double reqWidth) {
        return actLength + THIRD_METER >= reqLength
                && actWidth + THIRD_METER >= reqWidth;
    }

    @Override
    public void fillNonPreferenceParts() throws IOException, DocumentException {

        UpdateNotification(survey, mBuilder);

        mBuilder.setProgress(100, 60, false);

        additionalStuff();

        mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());

        SurveyPoint CenterLeadingEdge = AZHelper.CalculateCenterOfEdge(
                survey, true);
        SurveyPoint CenterTrailingEdge = AZHelper.CalculateCenterOfEdge(
                survey, false);

        double LeftLeading[] = Conversions.AROffset(CenterLeadingEdge.lat,
                CenterLeadingEdge.lon, survey.angle - 90,
                survey.width / 2);
        double RightLeading[] = Conversions.AROffset(CenterLeadingEdge.lat,
                CenterLeadingEdge.lon, survey.angle + 90,
                survey.width / 2);

        double LeftTrailing[] = Conversions.AROffset(
                CenterTrailingEdge.lat, CenterTrailingEdge.lon,
                survey.angle - 90,
                survey.width / 2);
        double RightTrailing[] = Conversions.AROffset(
                CenterTrailingEdge.lat, CenterTrailingEdge.lon,
                survey.angle + 90,
                survey.width / 2);

        if (!survey.circularAZ) {
            String LLEString = Conversions.GetMGRS(LeftLeading[0],
                    LeftLeading[1]) + " / "
                    + Conversions.GetLatLonDM(LeftLeading[0], LeftLeading[1]);
            set("leftLeadingEdge", LLEString);
            String RLEString = Conversions.GetMGRS(RightLeading[0],
                    RightLeading[1]) + " / "
                    + Conversions.GetLatLonDM(RightLeading[0], RightLeading[1]);
            set("rightLeadingEdge", RLEString);
            String LTEString = Conversions.GetMGRS(LeftTrailing[0],
                    LeftTrailing[1]) + " / "
                    + Conversions.GetLatLonDM(LeftTrailing[0], LeftTrailing[1]);
            set("leftTrailingEdge", LTEString);
            String RTEString = Conversions.GetMGRS(RightTrailing[0],
                    RightTrailing[1])
                    + " / "
                    + Conversions.GetLatLonDM(RightTrailing[0],
                            RightTrailing[1]);
            set("rightTrailingEdge", RTEString);
        } else {
            set("leftLeadingEdge", "N/A");
            set("rightLeadingEdge", "N/A");
            set("leftTrailingEdge", "N/A");
            set("rightTrailingEdge", "N/A");
        }

        mBuilder.setProgress(100, 65, false);
        mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());

        //dz only

        set("approvedDayCds",
                GetApproval("approvedDayCds"));
        set("approvedDayPer",
                GetApproval("approvedDayPer"));
        set("approvedDayHe",
                GetApproval("approvedDayHe"));
        set("approvedDayMff",
                GetApproval("approvedDayMff"));
        set("approvedDaySatb",
                GetApproval("approvedDaySatb"));
        set("approvedDayCrrc",
                GetApproval("approvedDayCrrc"));
        set("approvedDayHsllads",
                GetApproval("approvedDayHsllads"));
        set("approvedDayHvcds",
                GetApproval("approvedDayHvcds"));

        set("approvedNightCds",
                GetApproval("approvedNightCds"));
        set("approvedNightPer",
                GetApproval("approvedNightPer"));
        set("approvedNightHe",
                GetApproval("approvedNightHe"));
        set("approvedNightMff",
                GetApproval("approvedNightMff"));
        set("approvedNightSatb",
                GetApproval("approvedNightSatb"));
        set("approvedNightCrrc",
                GetApproval("approvedNightCrrc"));
        set("approvedNightHsllads",
                GetApproval("approvedNightHsllads"));
        set("approvedNightHvcds",
                GetApproval("approvedNightHvcds"));

        boolean customPI = survey.getMetaBoolean("customPI", false);
        if (!survey.circularAZ
                && (customPI || isValid(survey.getLength(),
                        survey.width, 365.5, 365.5))) {
            SurveyPoint CDSPos = AZHelper.CalculatePointOfImpact(survey, "cds");
            set("cdsPiMgrs", Conversions.GetMGRS(CDSPos.lat, CDSPos.lon));
            set("cdsPiLatitude", Conversions.GetLatDM(CDSPos.lat));
            set("cdsPiLongitude", Conversions.GetLonDM(CDSPos.lon));
            set("dimCdsPi", getYdsMetersString(survey.cdsPIOffset));

            if (survey.cdsPIElevation == SurveyPoint.Altitude.INVALID) {
                set("elevCdsPi", "UNK");
            } else {
                set("elevCdsPi",
                        getFeetMSLString(CDSPos.lat, CDSPos.lon,
                                survey.cdsPIElevation));
            }
        } else {
            set("cdsPiMgrs", "N/A");
            set("cdsPiLatitude", "N/A");
            set("cdsPiLongitude", "N/A");
            set("dimCdsPi", "N/A");
            set("elevCdsPi", "N/A");
        }

        if (!survey.circularAZ
                && (customPI || isValid(survey.getLength(),
                        survey.width, 914.4, 548.5))) {
            SurveyPoint HEPos = AZHelper.CalculatePointOfImpact(survey, "he");
            set("hePiMgrs", Conversions.GetMGRS(HEPos.lat, HEPos.lon));
            set("hePiLatitude", Conversions.GetLatDM(HEPos.lat));
            set("hePiLongitude", Conversions.GetLonDM(HEPos.lon));
            set("dimHePi", getYdsMetersString(survey.hePIOffset));
            if (survey.hePIElevation == SurveyPoint.Altitude.INVALID) {
                set("elevHePi", "UNK");
            } else {
                set("elevHePi",
                        getFeetMSLString(HEPos.lat, HEPos.lon,
                                survey.hePIElevation));
            }
        } else {
            set("hePiMgrs", "N/A");
            set("hePiLatitude", "N/A");
            set("hePiLongitude", "N/A");
            set("dimHePi", "N/A");
            set("elevHePi", "N/A");
        }

        if (!survey.circularAZ
                && (customPI || isValid(survey.getLength(),
                        survey.width, 548.5, 548.5))) {
            SurveyPoint PEPos = AZHelper.CalculatePointOfImpact(survey, "per");
            set("pePiMgrs", Conversions.GetMGRS(PEPos.lat, PEPos.lon));
            set("pePiLatitude", Conversions.GetLatDM(PEPos.lat));
            set("pePiLongitude", Conversions.GetLonDM(PEPos.lon));
            set("dimPePi", getYdsMetersString(survey.perPIOffset));
            if (survey.perPIElevation == SurveyPoint.Altitude.INVALID) {
                set("elevPePi", "UNK");
            } else {
                set("elevPePi",
                        getFeetMSLString(PEPos.lat, PEPos.lon,
                                survey.perPIElevation));
            }
        } else {
            set("pePiMgrs", "N/A");
            set("pePiLatitude", "N/A");
            set("pePiLongitude", "N/A");
            set("dimPePi", "N/A");
            set("elevPePi", "N/A");
        }

        //Set CDS, HE, and PER values for MGRS and Lat Lon.

        // FIXME - the original implementation did not contain the highest elevation lat, lon
        // so unlike all of the other fixes I made with the PE/HE/CDS stuff, I could not accurately
        // do the MSL conversion.   In a small DZ, this should be very close and likely why the 
        // other errors were not caught.

        set("highestElevation",
                getFeetMSLString(survey.center.lat, survey.center.lon,
                        survey.highestElevation));

        mBuilder.setProgress(100, 70, false);
        mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());
        //section 6
        if (survey.circularAZ) {
            set((String) "length", "N/A");
            set("width", "N/A");
            set("radius", getYdsMetersString(survey.getRadius()));
        } else {
            if (!fields.setField("radius", "N/A")) {
                Log.e(TAG, "failed to write Radius in form");
            }
            set("length", getYdsMetersString(survey.getLength()));
            set("width", getYdsMetersString(survey.width));
        }

        double RealCenter[] = Conversions.AROffset(survey.center.lat,
                survey.center.lon, survey.angle,
                PolygonHelper.GetOverrunOffset(false, survey));

        String CenterMGRS = Conversions.GetMGRS(RealCenter[0], RealCenter[1]);
        set("centerPointMgrs", CenterMGRS);

        String CenterLat = Conversions.GetLatDM(RealCenter[0]);
        set("centerPointLatitude", CenterLat);

        String CenterLon = Conversions.GetLonDM(RealCenter[1]);
        set("centerPointLongitude", CenterLon);

        String pooMGRS = Conversions.GetMGRS(survey.pointOfOrigin.lat,
                survey.pointOfOrigin.lon);
        set("pointOfOrigin", pooMGRS);

        if (survey.center.circularError < (3f / 4f)
                * ATSKConstants.DEFAULT_MAP_CLICK_ERROR_M) {
            String[] states = fields.getAppearanceStates("gpsDerivedYes");
            if (states != null)
                fields.setField("gpsDerivedYes", states[0]);
        } else {
            String[] states = fields.getAppearanceStates("gpsDerivedNo");
            if (states != null)
                fields.setField("gpsDerivedNo", states[0]);
        }

        set("trueValue", getAngleString(survey.angle));

        set("gridMgrs",
                getAngleString(Conversions
                        .GetMGRSHeadingOffset((float) survey.center.lat,
                                survey.center.lon, survey.angle)));

        set("magnetic", getAngleString(
                Conversions.GetMagAngle(survey.angle,
                        survey.center.lat, survey.center.lon)));

        set("variationSourceDate", "WMM 2015");
        set("spheroid", "WGS-84");
        set("datum", "WGS-84");

        final String GridZone = getGridZone();
        set("dzCoordGridZone", GridZone);

        String GridEasting = Conversions.GetUTMEastingShort(RealCenter[0],
                RealCenter[1]);
        set("dzCoordEasting", GridEasting);

        String GridNorthing = Conversions.GetUTMNorthingShort(RealCenter[0],
                RealCenter[1]);
        set("dzCoordNorthing", GridNorthing);

        mBuilder.setProgress(100, 90, false);
        mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());
    }

    private void additionalStuff() throws IOException, DocumentException {

        set("approvedDayCds",
                GetApproval("approvedDayCds"));
        set("approvedDayPer",
                GetApproval("approvedDayPer"));
        set("approvedDayHe",
                GetApproval("approvedDayHe"));
        set("approvedDayMff",
                GetApproval("approvedDayMff"));
        set("approvedDaySatb",
                GetApproval("approvedDaySatb"));
        set("approvedDayCrrc",
                GetApproval("approvedDayCrrc"));
        set("approvedDayHsllads",
                GetApproval("approvedDayHsllads"));
        set("approvedDayHvcds",
                GetApproval("approvedDayHvcds"));

        set("approvedNightCds",
                GetApproval("approvedNightCds"));
        set("approvedNightPer",
                GetApproval("approvedNightPer"));
        set("approvedNightHe",
                GetApproval("approvedNightHe"));
        set("approvedNightMff",
                GetApproval("approvedNightMff"));
        set("approvedNightSatb",
                GetApproval("approvedNightSatb"));
        set("approvedNightCrrc",
                GetApproval("approvedNightCrrc"));
        set("approvedNightHsllads",
                GetApproval("approvedNightHsllads"));
        set("approvedNightHvcds",
                GetApproval("approvedNightHvcds"));
    }
}
