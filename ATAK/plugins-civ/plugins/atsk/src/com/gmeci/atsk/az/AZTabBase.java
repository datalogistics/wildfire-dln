
package com.gmeci.atsk.az;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.atakmap.android.atsk.plugin.R;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.coremap.log.Log;
import com.atakmap.filesystem.HashingUtils;
import com.gmeci.atsk.ATSKFragmentManager;
import com.gmeci.atsk.ATSKMapComponent;
import com.gmeci.atsk.az.currentsurvey.SurveyOptionsDialog;
import com.gmeci.atsk.map.ATSKVehicle;
import com.gmeci.atsk.obstructions.ObstructionController;
import com.gmeci.atsk.obstructions.ObstructionToolbar;
import com.gmeci.atsk.resources.ATSKApplication;
import com.gmeci.atsk.resources.ATSKBaseFragment;
import com.gmeci.atsk.toolbar.ATSKBaseToolbar;
import com.gmeci.atsk.toolbar.ATSKToolbar;
import com.gmeci.atsk.toolbar.ATSKToolbarComponent;
import com.gmeci.core.ATSKConstants;
import com.gmeci.core.ATSKIntentConstants;
import com.gmeci.core.LineObstruction;
import com.gmeci.core.PointObstruction;
import com.gmeci.core.SurveyData;
import com.gmeci.atskservice.resolvers.AZProviderClient;
import com.gmeci.atskservice.resolvers.ObstructionProviderClient;
import com.gmeci.conversions.Conversions;
import com.gmeci.core.SurveyPoint;
import com.gmeci.helpers.AZHelper;
import com.gmeci.helpers.ObstructionHelper;
import com.gmeci.vehicle.VehicleBlock;
import com.sromku.polygon.PolyPoint;
import com.sromku.polygon.Polygon;

import java.util.HashMap;
import java.util.List;

public abstract class AZTabBase extends Fragment implements
        ATSKToolbar.OnToolbarVisibleListener {

    protected static final double SELECTED_SIZE_MULTIPLIER = 1.25f;
    private static final String TAG = "AZTabBase";
    protected ObstructionProviderClient opc;
    protected AZProviderClient azpc;
    protected SurveyData surveyData;
    protected SharedPreferences user_settings;

    protected boolean DisplayAnglesTrue = false;
    protected String DisplayCoordinateFormat = Conversions.COORD_FORMAT_MGRS;
    protected boolean DisplayUnitsStandard = true;

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

    protected abstract void UpdateScreen();

    public abstract void newPosition(SurveyPoint sp, boolean topCollected);

    public void SetSurveyInterface() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        user_settings = PreferenceManager
                .getDefaultSharedPreferences(getActivity());
        if (opc == null)
            opc = new ObstructionProviderClient(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        if (azpc == null)
            azpc = new AZProviderClient(getActivity());
        if (opc == null)
            opc = new ObstructionProviderClient(getActivity());
        return null;
    }

    public void onResume() {
        super.onResume();
        ReadPreferenceFile();
        if (opc == null)
            opc = new ObstructionProviderClient(getActivity());
        opc.Start();
        if (azpc == null)
            azpc = new AZProviderClient(getActivity());
        azpc.Start();
        surveyData = azpc.getAZ(
                azpc.getSetting(ATSKConstants.CURRENT_SURVEY, TAG), true);
        ATSKToolbarComponent.getToolbar().addVisibilityListener(this);
    }

    public void onPause() {
        super.onPause();

        if (opc != null)
            opc.Stop();
        if (azpc != null)
            azpc.Stop();

        ATSKToolbarComponent.getToolbar().removeVisibilityListener(this);
    }

    protected void stopCollection() {
        // Collection toolbar closed
    }

    @Override
    public void onToolbarVisible(ATSKBaseToolbar tb, boolean v) {
        if (tb instanceof ObstructionToolbar && !v)
            stopCollection();
    }

    protected void setupDetailsButton(View root) {
        if (root == null)
            return;
        Button detailsBtn = (Button) root.findViewById(R.id.details_btn);
        if (detailsBtn == null)
            return;
        detailsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (azpc != null && azpc.isStarted() && surveyData != null)
                    SurveyOptionsDialog.showAZDetails(azpc,
                            surveyData.uid, getActivity()
                                    .getSupportFragmentManager());
            }
        });
    }

    protected Conversions.Unit getDisplayUnit() {
        return DisplayUnitsStandard ? (surveyData != null
                && surveyData.getType() == SurveyData.AZ_TYPE.DZ ?
                Conversions.Unit.YARD : Conversions.Unit.FOOT)
                : Conversions.Unit.METER;
    }

    protected ATSKBaseFragment notifyTabHost() {
        // Find tab host
        ATSKFragmentManager fm = ATSKMapComponent.getATSKFM();
        if (fm != null) {
            ATSKBaseFragment parent = fm.getCurrentFragment();
            if (parent != null)
                parent.onTabCreated(this);
            return parent;
        }
        return null;
    }

    protected void ReadPreferenceFile() {

        //    units_settings.edit().putString(ATSKConstants.UNITS_ANGLE,DisplayAnglesTrue ? ATSKConstants.UNITS_ANGLE_TRUE : ATSKConstants.UNITS_ANGLE_MAG );
        String anglePreference = user_settings.getString(
                ATSKConstants.UNITS_ANGLE, ATSKConstants.UNITS_ANGLE_MAG);
        DisplayAnglesTrue = anglePreference.equalsIgnoreCase(
                ATSKConstants.UNITS_ANGLE_TRUE);
        DisplayCoordinateFormat = user_settings.getString(
                ATSKConstants.COORD_FORMAT, Conversions.COORD_FORMAT_MGRS);
        String DisplayUnitsString = user_settings.getString(
                ATSKConstants.UNITS_DISPLAY, ATSKConstants.UNITS_FEET);
        DisplayUnitsStandard = DisplayUnitsString
                .equals(ATSKConstants.UNITS_FEET);
    }

    public void shotApproved(SurveyPoint sp, double range_m,
            double azimuth_deg,
            double elev_deg, boolean topCollected) {
    }

    public boolean isOBVisible() {
        return ATSKIntentConstants.Visible(ATSKApplication
                .getCollectionState());
    }

    public boolean isOBWaitingForClick() {
        return ATSKIntentConstants.isWaitingForClick(ATSKApplication
                .getCollectionState());
    }

    public boolean isOBWaitingForGPS() {
        return ATSKIntentConstants.isWaitingForGPS(ATSKApplication
                .getCollectionState());
    }

    public boolean loadCurrentSurvey() {
        if (azpc == null)
            return false;
        String surveyUID = azpc.getSetting(ATSKConstants.CURRENT_SURVEY, TAG);

        if (surveyUID == null)
            return false;
        surveyData = azpc.getAZ(surveyUID, false);
        if (surveyData != null)
            return true;
        surveyData = new SurveyData();
        surveyData.uid = surveyUID;
        azpc.NewAZ(surveyData);
        azpc.putSetting(ATSKConstants.CURRENT_SURVEY, surveyData.uid, TAG);
        return false;
    }

    public boolean setOBState(String state) {
        return ATSKApplication.setObstructionCollectionMethod(
                state, TAG, false);
    }

    // Content resolver has updated survey
    public void updateSurvey(String surveyUID) {
        if (azpc != null && (surveyData == null
                || surveyData.uid.equals(surveyUID))) {
            Log.d(TAG, "Updating active survey: " + surveyUID);
            boolean started = azpc.isStarted();
            if (!started)
                azpc.Start();
            surveyData = azpc.getAZ(surveyUID, false);

            // If we start it here then we should stop it here
            if (!started)
                azpc.Stop();
        }
    }

    // Fragment manager is finished recalculating highest point, GSR, etc.
    public void postRecalc() {
        Log.d(TAG, "Initial recalculation finished.");
    }

    /** LZ ONLY **/

    private static HashMap<String, String> _gsrCache = new HashMap<String, String>();

    protected void UpdateCalculateGSR(boolean forceUpdate) {
        if (surveyData == null || surveyData.getType() != SurveyData.AZ_TYPE.LZ)
            return;

        // This function can be expensive time-wise
        // the more obstructions there are, so we should only
        // update when a relevant variable changes
        String relevantVars = surveyData.aircraft + ","
                + surveyData.center + ","
                + surveyData.angle + ","
                + surveyData.approachElevation + ","
                + surveyData.departureElevation + ","
                + surveyData.getLength() + ","
                + surveyData.edges.ApproachOverrunLength_m + ","
                + surveyData.edges.DepartureOverrunLength_m;
        String md5sum = HashingUtils.md5sum(relevantVars);

        if (!forceUpdate && _gsrCache.containsKey(surveyData.uid)
                && _gsrCache.get(surveyData.uid).equals(md5sum))
            return;
        _gsrCache.put(surveyData.uid, md5sum);

        // Get cached obstructions (much faster than querying)
        List<PointObstruction> points = ObstructionController
                .getInstance().getPointObstructions();
        List<LineObstruction> lines = ObstructionController
                .getInstance().getLineObstructions();

        List<PointObstruction> ApproachIncursions = AZHelper
                .CalculateLZGlideSlopeIncursions(surveyData,
                        points, lines, false);

        List<PointObstruction> DepartureIncursions = AZHelper
                .CalculateLZGlideSlopeIncursions(surveyData,
                        points, lines, true);

        filterVehicleObstructions(ApproachIncursions, false);
        filterVehicleObstructions(DepartureIncursions, true);

        if (ApproachIncursions.size() > 0) {
            surveyData.worstApproachIncursionPoint = ApproachIncursions.get(0);
            UpdateApproachIncursion(ApproachIncursions);
        } else {
            surveyData.worstApproachIncursionPoint = null;
            //set to the min values in config?
            surveyData.aDisplacedThreshold = 0;
            surveyData.approachGlideSlopeDeg = Conversions.RATIO_99_1;
        }
        if (DepartureIncursions.size() > 0) {
            surveyData.worstDepartureIncursionPoint = DepartureIncursions
                    .get(0);
            UpdateDepartureIncursion(DepartureIncursions);
        } else {
            surveyData.worstDepartureIncursionPoint = null;
            //set to the min values in config?
            surveyData.dDisplacedThreshold = 0;
            surveyData.departureGlideSlopeDeg = Conversions.RATIO_99_1;
        }
        azpc.UpdateAZ(surveyData, "gsr", false);
    }

    protected PointObstruction UpdateApproachIncursion(
            List<PointObstruction> ApproachPoints) {

        SurveyPoint appCenter = getCenterOfApproach(true, surveyData);

        PointObstruction worst = new PointObstruction(ApproachPoints.get(0));
        worst.uid = surveyData.uid + "_"
                + ATSKConstants.INCURSION_LINE_APPROACH_WORST;
        worst.type = ATSKConstants.INCURSION_LINE_APPROACH_WORST;
        worst.group = ATSKConstants.DEFAULT_GROUP;
        worst.remark = ATSKConstants.INCURSION_LINE_APPROACH_WORST;
        if (opc != null)
            opc.NewPoint(worst);

        //find new offset
        double NewDT = 0;

        //get the distance from end of clear to controlling.
        double endClearToControllingObDist = (float) Conversions
                .CalculateRangem(appCenter.lat, appCenter.lon,
                        worst.lat, worst.lon);

        //get the distance (using the desired GSR)//Min GSR angle and height of obs
        double totalheightDiff = worst.getHAE()
                + worst.height
                - surveyData.approachElevation;

        double controllingObDistSatisfyGSR = (float) (totalheightDiff / (Math
                .tan(Conversions.DEG2RAD * surveyData.minApproachGlideSlopeDeg)));

        if (endClearToControllingObDist < controllingObDistSatisfyGSR)
            NewDT = controllingObDistSatisfyGSR - endClearToControllingObDist;

        surveyData.aDisplacedThreshold = NewDT;

        double[] RAE = Conversions.CalculateRangeAngleElev(
                appCenter.lat, appCenter.lon,
                surveyData.approachElevation, worst.lat,
                worst.lon, worst.getHAE() + worst.height);

        surveyData.approachGlideSlopeDeg = RAE[2];
        return worst;
    }

    protected PointObstruction UpdateDepartureIncursion(
            List<PointObstruction> DeparturePoints) {

        SurveyPoint depCenter = getCenterOfApproach(false, surveyData);

        PointObstruction worst = new PointObstruction(DeparturePoints.get(0));
        worst.uid = surveyData.uid + "_"
                + ATSKConstants.INCURSION_LINE_DEPARTURE_WORST;
        worst.type = ATSKConstants.INCURSION_LINE_DEPARTURE_WORST;
        worst.group = ATSKConstants.DEFAULT_GROUP;
        worst.remark = ATSKConstants.INCURSION_LINE_DEPARTURE_WORST;
        if (opc != null)
            opc.NewPoint(worst);

        //find new offset
        double NewDT = 0;

        //get the distance from end of clear to controlling.
        double endClearToControllingObDist = Conversions
                .CalculateRangem(depCenter.lat, depCenter.lon,
                        worst.lat, worst.lon);

        //get the distance (using the desired GSR)//Min GSR angle and height of obs
        double totalheightDiff = worst.getHAE()
                + worst.height
                - surveyData.departureElevation;

        double controllingObDistSatisfyGSR = totalheightDiff / (Math
                .tan(Conversions.DEG2RAD
                        * surveyData.minDepartureGlideSlopeDeg));

        if (endClearToControllingObDist < controllingObDistSatisfyGSR)
            NewDT = controllingObDistSatisfyGSR - endClearToControllingObDist;

        surveyData.dDisplacedThreshold = NewDT;

        double[] RAE = Conversions.CalculateRangeAngleElev(
                depCenter.lat, depCenter.lon,
                surveyData.departureElevation, worst.lat,
                worst.lon, worst.getHAE() + worst.height);
        surveyData.departureGlideSlopeDeg = RAE[2];
        return worst;
    }

    protected static SurveyPoint getCenterOfApproach(boolean app,
            SurveyData survey) {
        double RealLength = survey.getLength(false) / 2.0f;
        double[] f = Conversions.AROffset(survey.center.lat,
                survey.center.lon, survey.angle + (app ? 180 : 0),
                RealLength + survey.endClearZoneLength
                        + GetOverrunOffset(app, survey));
        return new SurveyPoint(f[0], f[1]);
    }

    protected static double GetOverrunOffset(boolean app, SurveyData survey) {
        if (!app)
            return (survey.edges.ApproachOverrunLength_m - survey.edges.DepartureOverrunLength_m) / 2;
        return (survey.edges.DepartureOverrunLength_m - survey.edges.ApproachOverrunLength_m) / 2;
    }

    // Convert vehicle point obstructions to line obstructions and ensure
    // they're actually colliding with the approach/departure polygon
    protected void filterVehicleObstructions(
            List<PointObstruction> points, boolean depart) {

        if (points == null || points.size() == 0)
            return;

        // Get approach polygon
        Polygon depBounds = ObstructionHelper.buildPolygon(
                AZHelper.getInnerApproachCorners(surveyData, depart));

        // Vehicle line points
        for (int i = 0; i < points.size(); i++) {
            PointObstruction incursion = points.get(i);
            if (incursion.group.equals(ATSKConstants.VEHICLE_GROUP)) {
                // Retrieve vehicle line obstruction
                ATSKVehicle existing = ATSKVehicle.find(incursion.uid);
                LineObstruction lo;
                if (existing != null && existing.getLineObstruction() != null)
                    lo = existing.getLineObstruction();
                else
                    lo = VehicleBlock.buildLineObstruction(
                            incursion.type, incursion);
                // Check if each point in line obstruction is colliding
                boolean inside = false;
                for (SurveyPoint sp : lo.points) {
                    if (depBounds.contains(new PolyPoint(sp.lat, sp.lon)))
                        inside = true;
                }
                if (!inside)
                    // Remove vehicle from list if no points are inside
                    points.remove(i--);
                else if (i == 0)
                    // Display minimum bounding box
                    points.set(i, lo.getAABB());
            }
        }
    }
}
