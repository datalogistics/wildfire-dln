
package com.gmeci.atsk.az.farp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.gmeci.core.ATSKConstants;
import com.gmeci.core.ATSKIntentConstants;
import com.gmeci.core.SurveyData.AZ_TYPE;
import com.gmeci.atsk.az.AZTabBase;
import com.gmeci.conversions.Conversions;
import com.gmeci.core.SurveyPoint;

public abstract class FARPTabBase extends AZTabBase {
    public static final String DISTRESS_GROUP = "Distress";
    protected static final double SELECTED_SIZE_MULTIPLIER = 1.25f;
    protected static final int SELECTED_BG_COLOR = 0xff376d37;
    protected static final int NON_SELECTED_BG_COLOR = 0xff383838;
    private static final String TAG = "FARPTabBase";
    SharedPreferences gps_settings;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        gps_settings = PreferenceManager
                .getDefaultSharedPreferences(getActivity());
    }

    @Override
    public void onPause() {
        super.onPause();

        gps_settings
                .edit()
                .putString(ATSKConstants.COORD_FORMAT,
                        DisplayCoordinateFormat).apply();
        setOBState(ATSKIntentConstants.OB_STATE_HIDDEN);
    }

    @Override
    public void onResume() {
        super.onResume();
        DisplayCoordinateFormat = gps_settings.getString(
                ATSKConstants.COORD_FORMAT, Conversions.COORD_FORMAT_MGRS);
        Update();
    }

    @Override
    public void shotApproved(SurveyPoint sp, double range_m, double az_deg,
            double el_deg, boolean TopCollected) {
        newPosition(sp, TopCollected);
    }

    protected void Update() {
        String CurrentSurveyUID = azpc.getSetting(ATSKConstants.CURRENT_SURVEY,
                TAG);

        surveyData = azpc.getAZ(CurrentSurveyUID, true);
        if (surveyData != null && surveyData.getType() != AZ_TYPE.FARP) {
            surveyData.setType(AZ_TYPE.FARP);
            azpc.UpdateAZ(surveyData, "", true);
        }
    }

    protected void Notify(String NotificationTitle, String Notification1,
            String Notification2, String Notification3) {
        Intent NotificationBubbleIntent = new Intent(
                ATSKConstants.NOTIFICATION_BUBBLE);

        NotificationBubbleIntent.putExtra(ATSKConstants.NOTIFICATION_UPDATE,
                "true");
        NotificationBubbleIntent.putExtra(ATSKConstants.NOTIFICATION_TITLE,
                NotificationTitle);
        NotificationBubbleIntent.putExtra(ATSKConstants.NOTIFICATION_LINE1,
                Notification1);
        NotificationBubbleIntent.putExtra(ATSKConstants.NOTIFICATION_LINE1,
                Notification2);
        NotificationBubbleIntent.putExtra(ATSKConstants.NOTIFICATION_LINE3,
                Notification3);
        com.atakmap.android.ipc.AtakBroadcast.getInstance().sendBroadcast(
                NotificationBubbleIntent);
    }
}
