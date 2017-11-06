
package com.gmeci.atskservice.pdf;

import android.content.Context;
import android.util.Log;

import com.gmeci.core.ATSKConstants;
import com.gmeci.core.PointObstruction;
import com.gmeci.core.SurveyData;
import com.gmeci.atskservice.resolvers.ObstructionProviderClient;
import com.gmeci.helpers.AZHelper;
import com.gmeci.helpers.PolygonHelper;
import com.gmeci.conversions.Conversions;
import com.lowagie.text.DocumentException;

import java.io.IOException;
import java.lang.String;

public class HLZPDFFiller extends AbstractPDFFiller {

    HLZPDFFiller(Context context) {
        super(context);
    }

    @Override
    protected void fillNonPreferenceParts() throws IOException,
            DocumentException {

        ObstructionProviderClient opc;
        opc = new ObstructionProviderClient(context);
        if (!opc.Start())
            Log.d(TAG, "Failed to retreive ObstructionProviderClient");
        else
            Log.d(TAG, "Got ObstructionProviderClient");

        mBuilder.setContentTitle("ATSK Building HLZ PDF")
                .setContentText(
                        String.format("HLZ %s PDF In Progress",
                                survey.getSurveyName()))
                .setSmallIcon(com.gmeci.atskservice.R.drawable.lz_small);
        mBuilder.setProgress(100, 5, false);
        mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());

        set("slope",
                String.format("Longitude: %.0f%% Transverse:%.0f%%",
                        survey.slopeL, survey.slopeW));
        mBuilder.setContentText("HLZ PDF SLOPE");

        mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());
        mBuilder.setProgress(100, 70, false);

        String ac = survey.aircraft;
        if (ac == null || ac.length() == 0 || survey.aircraftCount == 0) {
            set("qtyType", "NONE SELECTED");
        } else {
            set("qtyType", String.format(
                    "%d / %s", survey.aircraftCount, ac));
        }
        mBuilder.setContentText("HLZ PDF QTY");

        mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());
        String ControllingObstacles = GetControllingObstructionString(
                survey, opc);
        set("quadrant",
                ControllingObstacles);

        double RealCenter[] = Conversions.AROffset(survey.center.lat,
                survey.center.lon, survey.angle,
                PolygonHelper.GetOverrunOffset(false, survey));

        String CenterMGRS = Conversions.GetMGRS(RealCenter[0], RealCenter[1]);
        set((String) "centerpointMgrs", CenterMGRS);

        String CenterLat = Conversions.GetLatDM(RealCenter[0]);
        set("centerpointLatitude", CenterLat);

        String CenterLon = Conversions.GetLonDM(RealCenter[1]);
        set("centerpointLongitude", CenterLon);

        try {
            if (survey.center.circularError < (3f / 4f)
                    * ATSKConstants.DEFAULT_MAP_CLICK_ERROR_M) {
                String[] states = fields.getAppearanceStates("gpsDerived");
                if (states != null && states.length > 0)
                    fields.setField("gpsDerived", states[0]);
            } else {
                String[] states = fields
                        .getAppearanceStates("notGpsDerived");
                if (states != null && states.length > 0)
                    fields.setField("notGpsDerived", states[0]);
            }
        } catch (Exception e) {
            Log.e(TAG, "failed to set the gps derived information", e);
        }

        set("trueAxis", getAngleString(survey.approachAngle) + "/" +
                getAngleString(survey.departureAngle));

        set("gridAxis", getAngleString(Conversions
                .GetMGRSHeadingOffset((float) survey.center.lat,
                        survey.center.lon,
                        survey.approachAngle)) + "/" +
                getAngleString(Conversions
                        .GetMGRSHeadingOffset((float) survey.center.lat,
                                survey.center.lon,
                                survey.departureAngle)));

        set("magneticAxis", getAngleString(Conversions
                .GetMagAngle(survey.approachAngle,
                        survey.center.lat, survey.center.lon)) + "/" +
                getAngleString(Conversions
                        .GetMagAngle(survey.departureAngle,
                                survey.center.lat, survey.center.lon)));

        set("variationDataSource", "WMM 2015");

        final String GridZone = getGridZone();
        set("gridZone", GridZone);

        String GridEasting = Conversions.GetUTMEastingShort(RealCenter[0],
                RealCenter[1]);
        set("easting", GridEasting);

        String GridNorthing = Conversions.GetUTMNorthingShort(RealCenter[0],
                RealCenter[1]);
        set("northing", GridNorthing);

        if (survey.circularAZ) {
            set("length", getFeetString(2 * survey.getRadius()));
            set("width", getFeetString(2 * survey.getRadius()));
        } else {
            set("length", getFeetString(survey.getLength()));
            set("width", getFeetString(survey.width));
        }
        set("diagram", "See Attached Diagram");

        set("spheroidAndDatum", "WGS-84/EGM-96");

        set("elevation",
                getFeetMSLString(survey.center.lat, survey.center.lon,
                        survey.highestElevation));

        mBuilder.setContentText("HLZ PDF QUADRANTS");
        mBuilder.setProgress(100, 80, false);
        mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());

        mBuilder.setProgress(100, 90, false);
        mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());
    }

    @Override
    protected String getBlankFileName(SurveyData survey) {
        return "HLZ Form(V6).pdf";
    }

    public static String GetControllingObstructionString(
            SurveyData currentSurvey, ObstructionProviderClient opc) {
        PointObstruction[][] controllingPos = AZHelper
                .CalculateQuadrantObstructions(currentSurvey, opc);

        boolean N = false;
        boolean E = false;
        boolean S = false;
        boolean W = false;
        for (int i = 0; i < controllingPos.length; i++) {
            if (controllingPos[i] != null) {
                double[] RangeAngleElev = Conversions
                        .CalculateRangeAngleElev(currentSurvey.center.lat,
                                currentSurvey.center.lon,
                                currentSurvey.center.getHAE(),
                                controllingPos[i][0].lat,
                                controllingPos[i][0].lon,
                                controllingPos[i][0].height);

                String s = Conversions.GetCardinalDirection(RangeAngleElev[1]);

                if (s.startsWith("N"))
                    N = true;
                else if (s.startsWith("E"))
                    E = true;
                else if (s.startsWith("S"))
                    S = true;
                else if (s.startsWith("W"))
                    W = true;
            }
        }
        String obstructed = "";
        String unobstructed = "";
        if (N)
            obstructed += "/N";
        else
            unobstructed += "/N";
        if (E)
            obstructed += "/E";
        else
            unobstructed += "/E";
        if (S)
            obstructed += "/S";
        else
            unobstructed += "/S";
        if (W)
            obstructed += "/W";
        else
            unobstructed += "/W";
        if (obstructed.startsWith("/"))
            obstructed = obstructed.substring(1, obstructed.length());
        if (obstructed.length() > 0)
            obstructed = obstructed + " Obstructed     ";
        if (unobstructed.startsWith("/"))
            unobstructed = unobstructed.substring(1, unobstructed.length());
        if (unobstructed.length() > 0)
            unobstructed = unobstructed + " Unobstructed";

        //Put all the info in the string at the end
        return obstructed + unobstructed;
    }

}
