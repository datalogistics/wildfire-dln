
package com.gmeci.atskservice.pdf;

import android.content.Context;
import android.util.Log;

import com.gmeci.core.SurveyData;
import com.gmeci.conversions.Conversions;
import com.lowagie.text.DocumentException;

import java.io.IOException;
import java.lang.String;

public class FARPPDFFiller extends AbstractPDFFiller {

    FARPPDFFiller(Context context) {
        super(context);
    }

    @Override
    protected void fillNonPreferenceParts() throws IOException,
            DocumentException {

        mBuilder.setContentTitle("ATSK Building PDF")
                .setContentText(
                        String.format("%s PDF In Progress",
                                survey.getSurveyName()));
        mBuilder.setProgress(100, 5, false);
        mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());

        mBuilder.setProgress(100, 60, false);
        mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());

        double ApproachCenter[] = Conversions.AROffset(
                survey.center.lat, survey.center.lon,
                survey.angle, survey.getLength(false));

        String ApproachMGRS = Conversions.GetMGRS(ApproachCenter[0],
                ApproachCenter[1]);
        String AOverrunLength = String
                .format("%.1f", survey.edges.ApproachOverrunLength_m
                        * METERS_2_FEET);
        if (!fields.setField((String) "approachEndOverrunLengthAndUnits",
                AOverrunLength)) {
            Log.e(TAG, "failed to write A overrun in form");
        }
        String DOverrunLength = String.format("%.1f",
                survey.edges.DepartureOverrunLength_m
                        * METERS_2_FEET);
        if (!fields.setField("departEndOverrunLengthAndUnits",
                DOverrunLength)) {
            Log.e(TAG, "failed to write A overrun in form");
        }

        if (!fields.setField("approachEndMgrs",
                ApproachMGRS)) {
            Log.e(TAG, "failed to write ApproachMGRS in form");
        }

        mBuilder.setProgress(100, 65, false);
        mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());
        String ApproachLat = Conversions.GetLatDM(ApproachCenter[0]);
        if (!fields.setField("approachEndLatitude", ApproachLat)) {
            Log.e(TAG, "failed to write ApproachLat in form");
        }
        String ApproachLon = Conversions.GetLonDM(ApproachCenter[1]);
        if (!fields.setField("approachEndLongitude", ApproachLon)) {
            Log.e(TAG, "failed to write ApproachLon in form");
        }
        String ElevationString = " ";

        ElevationString = String.format(
                "%.1f ft",
                Conversions.ConvertHAEtoMSL(
                        survey.center.lat,
                        survey.center.lon,
                        survey.approachElevation)
                        * Conversions.M2F);

        set("approachEndElevationAndUnits", ElevationString);

        mBuilder.setProgress(100, 70, false);
        mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());

        //departure////////////////////////////////////////////////////////////////////////////////////////////////////////
        double DepartureCenter[] = Conversions.AROffset(
                survey.center.lat, survey.center.lon,
                180 + survey.angle,
                survey.getLength(false));

        String DepartureMGRS = Conversions.GetMGRS(DepartureCenter[0],
                DepartureCenter[1]);
        if (!fields.setField("departureEndMgrs",
                DepartureMGRS)) {
            Log.e(TAG, "failed to write DepartureCenter MGRS in form");
        }
        String DepartureLat = Conversions.GetLatDM(DepartureCenter[0]);
        set("departureEndLongitude",
                DepartureLat);

        mBuilder.setProgress(100, 75, false);
        mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());
        String DepartureLon = Conversions.GetLonDM(DepartureCenter[1]);
        set("departureEndLatitude",
                DepartureLon);

        String ElevationStringDep = " ";
        ElevationStringDep = String.format(
                "%.1f ft",
                Conversions.ConvertHAEtoMSL(
                        survey.center.lat,
                        survey.center.lon,
                        survey.departureElevation)
                        * Conversions.M2F);
        set("approachEndElevationAndUnits",
                ElevationStringDep);

        set("highestElevationAndUnits",
                String.format(
                        "%.1fft",
                        Conversions.ConvertHAEtoMSL(
                                survey.center.lat,
                                survey.center.lon,
                                survey.highestElevation)
                                * Conversions.M2F));

        mBuilder.setContentText("FARP PDF Complete").setProgress(0, 0,
                false);
        mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());

        mBuilder.setProgress(100, 80, false);
        mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());
    }

    @Override
    protected String getBlankFileName(SurveyData survey) {
        return "FARP Form(V6).pdf";
    }
}
