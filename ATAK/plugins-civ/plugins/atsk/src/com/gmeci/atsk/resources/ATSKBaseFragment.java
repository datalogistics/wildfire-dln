
package com.gmeci.atsk.resources;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.atakmap.android.ipc.AtakBroadcast.DocumentedIntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.View;

import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.coremap.log.Log;
import com.gmeci.core.ATSKConstants;
import com.gmeci.core.ATSKIntentConstants;
import com.gmeci.core.PointObstruction;
import com.gmeci.core.SurveyPoint;
import com.gmeci.atskservice.resolvers.AZProviderClient;
import com.gmeci.atskservice.resolvers.ObstructionProviderClient;
import com.gmeci.constants.Constants;
import com.gmeci.hardwareinterfaces.HardwareConsumerInterface;

public abstract class ATSKBaseFragment extends Fragment {
    public static final String TEMP_LASER_UID = "Laser Point";
    protected static final String TAG = "ATSKBaseFragment";
    //map 
    public ObstructionProviderClient opc;
    //obstruction service
    public HardwareConsumerInterface hardwareInterface;
    /**
     * ATSKBaseFragment Info
     **/
    /* ATSK Base Fragment is designed to make the addition of ATSK fragments easier.
    * This will hold the misc. common code that most fragments will require.
    * You will no longer have to deal with map clicks and external device intents.  */

    protected AZProviderClient azpc;
    protected SharedPreferences user_settings;

    //flag to toggle all intents. defaulted to true
    protected String StoredStateBeforeHiding = ATSKIntentConstants.OB_STATE_HIDDEN;
    //Hardware Interaction
    ATSKHardwareAdapter hardwareAdapter;
    boolean TopCollection = false;
    protected Runnable _onCreateListener;
    protected boolean _created = false;
    final BroadcastReceiver OBInputReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            Bundle extras = intent.getExtras();

            if (action.equals(ATSKIntentConstants.OB_STATE_ACTION)) {
                String src = extras.getString(
                        ATSKIntentConstants.OB_STATE_SOURCE, "None");
                if (src.equals(TAG)) {
                    // don't listen to a change requested by the same class
                    return;
                }

                if (extras.containsKey(ATSKIntentConstants.OB_COLLECT_TOP))
                    TopCollection = extras.getBoolean(
                            ATSKIntentConstants.OB_COLLECT_TOP, true);

                String state = extras.getString(
                        ATSKIntentConstants.OB_STATE_ACTION);
                if (state != null
                        && (state.equals(ATSKIntentConstants.OB_STATE_HIDDEN)
                                || state.equals(ATSKIntentConstants.OB_STATE_REQUESTED_HIDDEN)
                                || state
                                    .equals(ATSKIntentConstants.OB_STATE_LRF_HIDDEN))) {
                    // Hide LRF laser marker
                    if (opc != null)
                        opc.DeletePoint(ATSKConstants.DEFAULT_GROUP,
                                TEMP_LASER_UID, true);
                }

                boolean doAction = extras.getBoolean(
                        ATSKIntentConstants.OB_ACTION, false);
                if (!doAction)
                    return;

                if (extras.containsKey(ATSKIntentConstants.OB_STATE_ACTION)) {
                    //Update the display state

                    //open/ close the stuff we need
                    //int StateIndex = Values.getInt(ATSKIntentConstants.OB_OUT_POINT_INDEX, 0);
                    if (state
                            .equals(ATSKIntentConstants.OB_STATE_2PPLUSD_GPS_1)) {
                        GPS2PPlusDStateChange(1, TopCollection);
                        //point Plus offset
                        //SelectedPointPlusOffset();
                    } else if (state
                            .equals(ATSKIntentConstants.OB_STATE_2PPLUSD_GPS_2)) {
                        GPS2PPlusDStateChange(2, TopCollection);
                        //point Plus offset
                        //SelectedPointPlusOffset();
                    } else if (state
                            .equals(ATSKIntentConstants.OB_STATE_2PPLUSD_GPS)) {
                        GPS2PPlusDStateChange(1, TopCollection);
                        //point Plus offset
                        SelectedPointPlusOffset(TopCollection);
                    } else if (state
                            .equals(ATSKIntentConstants.OB_STATE_OFFSET_GPS)) {
                        //point Plus offset
                        SelectedPointPlusOffset(TopCollection);
                    } else if (state.equals(ATSKIntentConstants.OB_STATE_GPS))
                        SelectedPoint(TopCollection);
                    else if (state
                            .equals(ATSKIntentConstants.OB_STATE_BC_GPS_OFF))
                        ToggleBreadcrumbCollection(false);
                    else if (state
                            .equals(ATSKIntentConstants.OB_STATE_BC_GPS_ON))
                        ToggleBreadcrumbCollection(true);
                }
            }
        }
    };
    private final BroadcastReceiver AZClickReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent i) {
            SurveyPoint sp = i.getParcelableExtra(ATSKConstants.
                    SURVEY_POINT_EXTRA);
            if (sp == null) {
                sp = new SurveyPoint(
                        i.getDoubleExtra(ATSKConstants.LAT_EXTRA, 0),
                        i.getDoubleExtra(ATSKConstants.LON_EXTRA, 0));
                sp.setHAE(i.getDoubleExtra(ATSKConstants.ALT_EXTRA, 0));
                sp.circularError = i.getDoubleExtra(ATSKConstants.CE_EXTRA, 20);
                sp.linearError = i.getDoubleExtra(ATSKConstants.LE_EXTRA, 20);
            }
            MapClickDetected(sp);
        }
    };
    private final BroadcastReceiver lrfApprovedRX = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent i) {
            double range_m = 0, az_deg = 0, elev_deg = 0;
            SurveyPoint sp = i.getParcelableExtra(ATSKConstants.
                    SURVEY_POINT_EXTRA);
            if (sp == null) {
                sp = new SurveyPoint(
                        i.getDoubleExtra(ATSKConstants.LAT_EXTRA, 0),
                        i.getDoubleExtra(ATSKConstants.LON_EXTRA, 0));
                sp.setHAE(i.getDoubleExtra(ATSKConstants.ALT_EXTRA, 0));
                sp.circularError = i.getDoubleExtra(ATSKConstants.CE_EXTRA, 20);
                sp.linearError = i.getDoubleExtra(ATSKConstants.LE_EXTRA, 20);
            }
            range_m = i.getDoubleExtra(ATSKConstants.RANGE_M, 0);
            az_deg = i.getDoubleExtra(ATSKConstants.AZIMUTH_T, 0);
            elev_deg = i.getDoubleExtra(ATSKConstants.ELEVATION, 0);
            boolean TopCollected = i.getBooleanExtra(
                    ATSKIntentConstants.OB_COLLECT_TOP, false);

            shotApproved(sp, range_m, az_deg, elev_deg, TopCollected);
        }
    };

    public ATSKBaseFragment() {
    }

    public static void UpdateNotification(Context context, String Title,
            String Line1, String Line2, String Line3) {
        Intent BubbleIntent = new Intent();
        BubbleIntent.setAction(ATSKConstants.NOTIFICATION_BUBBLE);
        BubbleIntent.putExtra(ATSKConstants.NOTIFICATION_UPDATE, true);
        BubbleIntent.putExtra(ATSKConstants.NOTIFICATION_TITLE, Title);
        BubbleIntent.putExtra(ATSKConstants.NOTIFICATION_LINE1, Line1);
        BubbleIntent.putExtra(ATSKConstants.NOTIFICATION_LINE2, Line2);
        BubbleIntent.putExtra(ATSKConstants.NOTIFICATION_LINE3, Line3);
        AtakBroadcast.getInstance().sendBroadcast(BubbleIntent);
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        opc = new ObstructionProviderClient(getActivity());
        azpc = new AZProviderClient(getActivity());
        user_settings = PreferenceManager
                .getDefaultSharedPreferences(getActivity());
    }

    public boolean GPS2PPlusDStateChange(int newState, boolean TopCollected) {
        return false;
    }

    public void SetSurveyInterface() {
    }

    public void setHardwareInterface(HardwareConsumerInterface hwInterface) {
        hardwareInterface = hwInterface;
    }

    @Override
    public void onResume() {
        super.onResume();
        setupHardwareEvents();
        //handle map stuff
        if (opc != null)
            opc.Start();
        if (azpc != null)
            azpc.Start();
        AtakBroadcast.getInstance().registerReceiver(AZClickReceiver,
                new DocumentedIntentFilter(ATSKConstants.MAP_CLICK_ACTION));

        AtakBroadcast.getInstance().registerReceiver(lrfApprovedRX,
                new DocumentedIntentFilter(
                        ATSKConstants.NOTIFICATION_BUBBLE_LRF_APPROVED));

        DocumentedIntentFilter filter = new DocumentedIntentFilter();
        filter.addAction(ATSKIntentConstants.OB_STATE_ACTION);
        AtakBroadcast.getInstance().registerReceiver(
                OBInputReceiver,
                filter);

    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        _created = true;
    }

    public void runOnCreate(Runnable r) {
        _onCreateListener = r;
    }

    protected void runOnCreate() {
        if (_onCreateListener != null)
            _onCreateListener.run();
        _onCreateListener = null;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (opc != null)
            opc.Stop();
        if (azpc != null)
            azpc.Stop();
        hardwareAdapter.unregister(getActivity().getApplicationContext());
        AtakBroadcast.getInstance().unregisterReceiver(
                OBInputReceiver);
        AtakBroadcast.getInstance().unregisterReceiver(
                AZClickReceiver);
        AtakBroadcast.getInstance().unregisterReceiver(
                lrfApprovedRX);
        if (opc != null)
            opc.DeletePoint(ATSKConstants.DEFAULT_GROUP,
                    TEMP_LASER_UID, true);
    }

    public boolean onBackButtonPressed() {
        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        _created = false;
    }

    // Child tab has finished onViewCreated
    public void onTabCreated(Fragment tabFrag) {
    }

    protected void SelectedPointPlusOffset(boolean TopCollected) {

    }

    protected void SelectedPoint(boolean TopCollected) {
        Log.d(TAG, "SelectedPoint");
    }

    protected void ToggleBreadcrumbCollection(boolean b) {

    }

    public void DrawAZ(String surveyUID, SurveyPoint center, double length_m,
            double width_m, double angle_degtrue) {
    }

    public void shotApproved(SurveyPoint sp, double range_m, double az_deg,
            double el_deg, boolean TopCollected) {
        if (opc != null)
            opc.DeletePoint(ATSKConstants.DEFAULT_GROUP,
                    TEMP_LASER_UID, true);
    }

    public void MapClickDetected(SurveyPoint ClickPoint) {
        //save it here...
        putSetting(ATSKConstants.LAST_MAP_LAT, "" + ClickPoint.lat, TAG);
        putSetting(ATSKConstants.LAST_MAP_LON, "" + ClickPoint.lon, TAG);
        putSetting(ATSKConstants.LAST_MAP_ZOOM, "-1", TAG);
    }

    public void GPSLinePoint(SurveyPoint ClickPoint, boolean withOffset) {

    }

    protected void putSetting(String SettingName, String SettingValue,
            String ChangeRequestor) {
        if (azpc != null)
            azpc.putSetting(SettingName, SettingValue, ChangeRequestor);
    }

    public void setupHardwareEvents() {
        hardwareAdapter = new ATSKHardwareAdapter();
        hardwareAdapter.SetupHardwareAdapter(getActivity()
                .getApplicationContext());
        hardwareAdapter.setListener(new HardwareEventListener() {
            public void LRFEvent(SurveyPoint sp, double range, double azimuth,
                    double elev) {
                shotDetected(sp, range, azimuth, elev, TopCollection);
            }
        });
    }

    public void shotDetected(SurveyPoint sp, double range_m,
            double azimuth_deg,
            double elev_deg, boolean TopCollected) {

        Log.d(TAG, "LRF Shot");
        StoredStateBeforeHiding = ATSKApplication.getCollectionState();

        if (!ATSKIntentConstants
                .isWaitingForLRF(StoredStateBeforeHiding)) {
            return;
        }

        //Show LRF icon here
        PointObstruction newPoint = new PointObstruction(sp);
        newPoint.type = Constants.PO_LASER;
        newPoint.uid = TEMP_LASER_UID;
        newPoint.group = ATSKConstants.DEFAULT_GROUP;
        newPoint.remark = String.format(LocaleUtil.getCurrent(),
                "%.1fm@%.1fdeg", range_m, azimuth_deg);
        if (opc != null)
            opc.NewPoint(newPoint);

        if (!user_settings.getBoolean(
                Constants.REQUIRE_LRF_APPROVAL, true)) {
            shotApproved(sp, range_m, azimuth_deg, elev_deg, TopCollected);
            return;
        }
        Intent i = new Intent(ATSKConstants.NOTIFICATION_BUBBLE);
        i.putExtra(ATSKConstants.SURVEY_POINT_EXTRA, (Parcelable) sp);
        i.putExtra(ATSKConstants.LAT_EXTRA, sp.lat);
        i.putExtra(ATSKConstants.LON_EXTRA, sp.lon);
        i.putExtra(ATSKConstants.ALT_EXTRA, sp.getHAE());
        i.putExtra(ATSKConstants.RANGE_M, range_m);
        i.putExtra(ATSKConstants.AZIMUTH_T, azimuth_deg);
        i.putExtra(ATSKConstants.ELEVATION, elev_deg);
        i.putExtra(ATSKConstants.LRF_INPUT, true);
        i.putExtra(ATSKIntentConstants.OB_COLLECT_TOP, TopCollected);
        AtakBroadcast.getInstance().sendBroadcast(i);
    }

    /**
     * Pull the current GPS for as described by the GPS puck or currently where the
     * location marker is.
     */

    public SurveyPoint pullGPS() {
        SurveyPoint gps = null;
        if (hardwareInterface != null) {
            try {
                gps = hardwareInterface.getMostRecentPoint();
            } catch (RemoteException e) {
                Log.d(TAG,
                        "error occurred pulling the GPS from the hardware interface.",
                        e);
            }
        }
        Log.d(TAG, "pulled a GPS point: " + gps);
        return gps;
    }

    public boolean isOBVisible() {
        String CurrentState = "None";
        CurrentState = ATSKApplication.getCollectionState();
        return ATSKIntentConstants.Visible(CurrentState);
    }

    public boolean isOBWaitingForClick() {
        String CurrentState = "None";
        CurrentState = ATSKApplication.getCollectionState();
        return ATSKIntentConstants
                .isWaitingForClick(CurrentState);
    }

    public boolean isOBWaitingForGPS() {
        String CurrentState = "None";
        CurrentState = ATSKApplication.getCollectionState();
        return ATSKIntentConstants.isWaitingForGPS(CurrentState);
    }

}
