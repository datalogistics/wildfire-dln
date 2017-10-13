
package com.gmeci.atsk.az.farp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.atakmap.android.atsk.plugin.R;
import com.atakmap.math.MathUtils;
import com.gmeci.atsk.az.AZController;
import com.gmeci.atsk.resources.ATSKApplication;
import com.gmeci.core.ATSKConstants;
import com.gmeci.core.ATSKIntentConstants;
import com.gmeci.core.SurveyData;
import com.gmeci.core.SurveyPoint;
import com.gmeci.atsk.az.farp.FARPSupport.FARPFAMLeftRightDeleteDialog;
import com.gmeci.atsk.az.farp.FARPSupport.FARPReceiverLayoutSpinner;
import com.gmeci.atsk.resources.ATSKDialogManager;
import com.gmeci.atsk.resources.ATSKDialogManager.DialogUpdateInterface;
import com.gmeci.atsk.resources.CoordinateHandJamDialog;
import com.gmeci.atskservice.farp.FARPTankerItem;
import com.gmeci.conversions.Conversions;

import java.util.Map;

public class FARPReceiverFragment extends FARPTabBase implements
        CoordinateHandJamDialog.HandJamInterface, DialogUpdateInterface {

    private static final String TAG = "FARPReceiverFragment";
    private static final int ANGLE_STATIC = 0;
    private static final int ANGLE_STATIC_LINE = 1;
    private static final int ANGLE_VALUE = 2;
    private static final int ANGLE_UNITS = 3;
    SharedPreferences gps_settings;
    SharedPreferences units_settings;
    FARPTankerItem currentTanker;
    ImageView rxSideStatic;
    TextView FAMLocationStaticTV, FAMLocationTV;
    //    TextView[] RXPoints = new TextView[3];
    //    TextView[] RXPointsStatic = new TextView[3];
    final TextView[] FAMRXAngleTVs = new TextView[4];
    FARPReceiverLayoutSpinner _rxSpinner;
    protected Map<String, FARPTankerItem> _tankers;
    boolean ShowingRight = false;
    int SelectedID = -1;
    private final OnClickListener FAMPointClickListener = new OnClickListener() {
        public void onClick(View v) {
            if (SelectedID > 0) {
                stopCollection();
            } else {
                deSelectCollectionItems(false);
                SelectedID = v.getId();
                setOBState(ATSKIntentConstants.OB_STATE_REQUESTED_POINT);

                if (SelectedID == FAMLocationStaticTV.getId()
                        || SelectedID == FAMLocationTV.getId()) {
                    FAMLocationStaticTV
                            .setBackgroundResource(R.drawable.background_selected_left);
                    FAMLocationTV
                            .setBackgroundResource(R.drawable.background_selected_left);

                    //update DB to show we're waiting for it?
                    int FAMIndex = SurveyData.getFARPSideIndex(ShowingRight);

                    if (surveyData.FAMPoints != null) {
                        if (surveyData.FAMPoints[FAMIndex] == null)
                            surveyData.FAMPoints[FAMIndex] = new SurveyPoint();

                        surveyData.FAMPoints[1 - FAMIndex].visible = false;
                        surveyData.FAMPoints[FAMIndex].visible = true;
                        surveyData.FAMPoints[FAMIndex].circularError = surveyData
                                .getFAMDistance();
                    }

                    azpc.UpdateAZ(surveyData, "FAM Pull", true);

                    String Side = "Left";
                    if (ShowingRight)
                        Side = "Right";
                    Notify("FAM CART Location", "Select " + Side + " FAM",
                            "Position", "");
                }

            }
        }
    };
    private final OnClickListener RxLayoutAngleClickListener = new OnClickListener() {
        public void onClick(View v) {
            if (SelectedID > 0) {
                deSelectCollectionItems(true);
            } else {
                deSelectCollectionItems(false);
                SelectedID = v.getId();
                setOBState(ATSKIntentConstants.OB_STATE_REQUESTED_POINT);
                for (int i = 0; i < FAMRXAngleTVs.length; i++) {
                    if (FAMRXAngleTVs[i] != null) {
                        FAMRXAngleTVs[i]
                                .setBackgroundResource(R.drawable.background_selected_left);
                    }
                }
                String Side = "Left";
                if (ShowingRight)
                    Side = "Right";
                Notify("Receiver Location", "Select RX",
                        String.format("%s", Side), "FARP FAM Angle");

            }
        }
    };
    private final OnLongClickListener FAMPointLongClickListener = new OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            SelectedID = v.getId();
            int SideIndex = getSideIndex();

            CoordinateHandJamDialog chjd = new CoordinateHandJamDialog();
            if (surveyData.FAMPoints[SideIndex] == null)
                surveyData.FAMPoints[SideIndex] = new SurveyPoint();
            chjd.Initialize(surveyData.FAMPoints[SideIndex].lat,
                    surveyData.FAMPoints[SideIndex].lon,
                    DisplayCoordinateFormat,
                    surveyData.FAMPoints[SideIndex].getHAE(),
                    FARPReceiverFragment.this);

            return true;
        }

    };
    private final OnLongClickListener RXPointLongClickListener = new OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            SelectedID = v.getId();
            int SideIndex = getSideIndex();

            ATSKDialogManager adm = new ATSKDialogManager(getActivity(),
                    FARPReceiverFragment.this, false);

            SurveyPoint AnchorPoint = new SurveyPoint(surveyData.center.lat,
                    surveyData.center.lon);//
            adm.ShowAngleHandJamDialog(surveyData.FAMRxAngle[SideIndex],
                    AnchorPoint, SelectedID);

            return true;
        }

    };

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        gps_settings = PreferenceManager
                .getDefaultSharedPreferences(getActivity());

    }

    @Override
    public void onDestroy() {

        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        _tankers = AZController.getInstance().getTankers();
        Context pluginContext = ATSKApplication
                .getInstance().getPluginContext();
        return LayoutInflater.from(pluginContext).inflate(
                R.layout.farp_receiver, container, false);
    }

    @Override
    public void onPause() {
        super.onPause();
        deSelectCollectionItems(true);

    }

    public void UpdateDisplayMeasurements(SurveyData updatedSurvey) {
        surveyData = updatedSurvey;
        GetVisibleSide();
        UpdateDisplayMeasurements();
    }

    private void GetVisibleSide() {
        if (surveyData != null && surveyData.FAMPoints != null
                && surveyData.FAMPoints.length == 2) {
            int index = getSideIndex();
            if (surveyData.FAMPoints[index] == null
                    || !surveyData.FAMPoints[index].visible)
                ShowingRight = !ShowingRight;
        }
    }

    private void UpdateDisplayMeasurements() {

        int SideIndex = getSideIndex();
        if (DisplayAnglesTrue) {
            surveyData.FAMRxAngle[SideIndex] =
                    Conversions.deg360(surveyData.FAMRxAngle[SideIndex]);
            FAMRXAngleTVs[ANGLE_VALUE].setText(String.format("%.1f",
                    surveyData.FAMRxAngle[SideIndex]));
            FAMRXAngleTVs[ANGLE_UNITS].setText(String.format("%cT",
                    ATSKConstants.DEGREE_SYMBOL));
        } else {
            double MagAngle = Conversions.GetMagAngle(
                    surveyData.FAMRxAngle[SideIndex], surveyData.center.lat,
                    surveyData.center.lon);
            FAMRXAngleTVs[ANGLE_VALUE].setText(String.format("%.1f", MagAngle));
            FAMRXAngleTVs[ANGLE_UNITS].setText(String.format("%cM",
                    ATSKConstants.DEGREE_SYMBOL));
        }
        //set locations for  for each rx point...
        String FAMCoordinateString = "NONE";
        if (surveyData.FAMPoints != null
                && surveyData.FAMPoints[SideIndex] != null) {
            FAMCoordinateString = Conversions.getCoordinateString(
                    surveyData.FAMPoints[SideIndex].lat,
                    surveyData.FAMPoints[SideIndex].lon,
                    DisplayCoordinateFormat);
            surveyData.FAMPoints[1 - SideIndex].visible = false;
            surveyData.FAMPoints[SideIndex].visible = true;
            azpc.UpdateAZ(surveyData, "FARP", true);
        }
        FAMLocationTV.setText(FAMCoordinateString);

        if (!ShowingRight)
            rxSideStatic
                    .setBackgroundResource(R.drawable.farp_fam_left_on);
        else
            rxSideStatic
                    .setBackgroundResource(R.drawable.farp_fam_right_on);
    }

    private int getSideIndex() {
        return SurveyData.getFARPSideIndex(ShowingRight);
    }

    @Override
    public void onResume() {
        super.onResume();

        units_settings = PreferenceManager
                .getDefaultSharedPreferences(getActivity());
        String AngleString = units_settings.getString(
                ATSKConstants.UNITS_ANGLE, ATSKConstants.UNITS_ANGLE_MAG);
        DisplayAnglesTrue = !AngleString.equals(ATSKConstants.UNITS_ANGLE_MAG);
        GetVisibleSide();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupFARPReceiverButtons(view);

    }

    private void setupFARPReceiverButtons(View view) {

        rxSideStatic = (ImageView) view.findViewById(R.id.receiver_side_static);
        rxSideStatic.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ShowingRight = !ShowingRight;
                UpdateDisplayMeasurements();
                //clear out anything selected...
                deSelectCollectionItems(true);
                setOBState(ATSKIntentConstants.OB_STATE_HIDDEN);
            }
        });

        rxSideStatic.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                FARPFAMLeftRightDeleteDialog FFLRDD = new FARPFAMLeftRightDeleteDialog(
                        getActivity());
                FFLRDD.Initialize(azpc, FARPReceiverFragment.this);
                deSelectCollectionItems(true);
                setOBState(ATSKIntentConstants.OB_STATE_HIDDEN);
                return false;
            }
        });

        FAMLocationStaticTV = (TextView) view
                .findViewById(R.id.FAMLocation_static);
        FAMLocationTV = (TextView) view.findViewById(R.id.FAMLocation);
        FAMLocationTV.setOnLongClickListener(FAMPointLongClickListener);
        FAMLocationStaticTV.setOnLongClickListener(FAMPointLongClickListener);
        FAMLocationTV.setOnClickListener(FAMPointClickListener);
        FAMLocationStaticTV.setOnClickListener(FAMPointClickListener);
        FAMRXAngleTVs[ANGLE_STATIC] = (TextView) view
                .findViewById(R.id.fam2rxangle_static);
        FAMRXAngleTVs[ANGLE_STATIC_LINE] = (TextView) view
                .findViewById(R.id.angle_static);
        FAMRXAngleTVs[ANGLE_VALUE] = (TextView) view.findViewById(R.id.angle);
        FAMRXAngleTVs[ANGLE_UNITS] = (TextView) view
                .findViewById(R.id.angle_units);
        for (int i = 0; i < FAMRXAngleTVs.length; i++) {
            if (FAMRXAngleTVs[i] != null) {
                FAMRXAngleTVs[i].setOnClickListener(RxLayoutAngleClickListener);
                FAMRXAngleTVs[i]
                        .setOnLongClickListener(RXPointLongClickListener);
            }
        }
        _rxSpinner = (FARPReceiverLayoutSpinner) view
                .findViewById(R.id.points);
        _rxSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int pos,
                    long index) {
                if (surveyData != null) {
                    FARPTankerItem tanker = _tankers.get(surveyData.aircraft);
                    surveyData.updateFAMShape(_rxSpinner.GetSetting(pos),
                            tanker);

                    deSelectCollectionItems(true);
                    setOBState(ATSKIntentConstants.OB_STATE_HIDDEN);

                    azpc.UpdateAZ(surveyData, "FARP", true);
                    UpdateDisplayMeasurements();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                //

            }

        });

        //if the collection is still running from a previous start - we should display it

    }

    @Override
    protected void Update() {
        if (_tankers == null || _tankers.isEmpty())
            return;
        for (FARPTankerItem fti : _tankers.values()) {
            if (surveyData.aircraft == null
                    || surveyData.aircraft.equals("")
                    || fti.Name.equals(surveyData.aircraft)) {
                currentTanker = fti;
                break;
            }
        }
        if (currentTanker == null)
            currentTanker = _tankers.get(ATSKConstants.AC_C130);
        _rxSpinner.SetupSpinner(currentTanker);
        int maxPos = _rxSpinner.getAdapter().getCount() - 1;
        int pos = _rxSpinner.GetPosition(surveyData.FAMRxShape);
        pos = Math.min(pos, maxPos);
        _rxSpinner.setSelection(pos);
        GetVisibleSide();
        UpdateDisplayMeasurements();
    }

    private double clampAngle(double deg, double min, double max) {
        min = Conversions.deg360(min);
        max = Conversions.deg360(max);
        deg = Conversions.deg360(deg);
        /*Log.d(TAG, "Deg: " + deg);
        Log.d(TAG, "Min: " + min);
        Log.d(TAG, "Max: " + max);*/
        if (max < min) {
            if (deg < max)
                deg += 360;
            max += 360;
        }
        /*Log.d(TAG, "Linear Deg: " + deg);
        Log.d(TAG, "Linear Min: " + min);
        Log.d(TAG, "Linear Max: " + max);*/
        return Conversions.deg360(MathUtils.clamp(deg, min, max));
    }

    @Override
    public void UpdateCoordinate(double Lat, double Lon, double elevation) {
        for (int i = 0; i < FAMRXAngleTVs.length; i++) {
            if (SelectedID == FAMRXAngleTVs[i].getId()) {
                int SideIndex = getSideIndex();
                if (surveyData.FAMPoints[SideIndex] == null)
                    surveyData.FAMPoints[SideIndex] = new SurveyPoint();
                double AngleT = Conversions.CalculateAngledeg(
                        surveyData.FAMPoints[SideIndex].lat,
                        surveyData.FAMPoints[SideIndex].lon, Lat, Lon);
                surveyData.FAMRxAngle[SideIndex] = AngleT;
                UpdateDisplayMeasurements();
                azpc.UpdateAZ(surveyData, "FAMCoord", true);
            }
        }
    }

    @Override
    public void UpdateCoordinateFormat(String DisplayFormat) {
        DisplayCoordinateFormat = DisplayFormat;
        UpdateDisplayMeasurements();
    }

    private void deSelectCollectionItems(boolean AllowUpdate) {
        setOBState(ATSKIntentConstants.OB_STATE_HIDDEN);
        SelectedID = -1;
        for (int i = 0; i < FAMRXAngleTVs.length; i++) {
            if (FAMRXAngleTVs[i] != null) {
                FAMRXAngleTVs[i].setBackgroundColor(NON_SELECTED_BG_COLOR);
            }
        }
        FAMLocationStaticTV.setBackgroundColor(NON_SELECTED_BG_COLOR);
        FAMLocationTV.setBackgroundColor(NON_SELECTED_BG_COLOR);

        if (surveyData != null && azpc.isStarted()) {
            azpc.UpdateAZ(surveyData, "FAM Pull OFF", AllowUpdate);
        }
    }

    @Override
    public void UpdateMeasurement(int index, double measurement) {
        for (int i = 0; i < FAMRXAngleTVs.length; i++) {
            if (SelectedID == FAMRXAngleTVs[i].getId()) {
                int SideIndex = getSideIndex();
                surveyData.FAMRxAngle[SideIndex] = measurement;
                UpdateDisplayMeasurements();
                azpc.UpdateAZ(surveyData, "hjRxAngle", true);
                return;
            }
        }
    }

    @Override
    public void UpdateStringValue(int index, String value) {
    }

    @Override
    public void UpdateAngleUnits(boolean usingTrue) {
        DisplayAnglesTrue = usingTrue;
        units_settings
                .edit()
                .putString(
                        ATSKConstants.UNITS_ANGLE,
                        usingTrue ? ATSKConstants.UNITS_ANGLE_TRUE
                                : ATSKConstants.UNITS_ANGLE_MAG).apply();
    }

    @Override
    public void UpdateDimensionUnits(boolean usingFeet) {
    }

    @Override
    protected void UpdateScreen() {
    }

    @Override
    public void newPosition(SurveyPoint sp, boolean TopCollected) {
        int side = getSideIndex();
        for (int i = 0; i < FAMRXAngleTVs.length; i++) {
            if (SelectedID == FAMRXAngleTVs[i].getId()
                    || SelectedID == FAMRXAngleTVs[i].getId()) {
                //calculate the angle from FAM cart
                if (surveyData.FAMPoints[side] == null)
                    surveyData.FAMPoints[side] = new SurveyPoint();
                double FAM2RxAngle = Conversions.calculateAngle(
                        surveyData.FAMPoints[side], sp);

                surveyData.FAMRxAngle[side] = FAM2RxAngle;
                break;
            }
        }
        if (SelectedID == FAMLocationStaticTV.getId()
                || SelectedID == FAMLocationTV.getId()) {

            /* Keep FAM cart within the radius specified by MinFAMDistance_m */
            //SurveyPoint fp = surveyData.center;

            // Start at actual fuel position (not the survey center point)
            double surveyAng = surveyData.center.course_true;
            SurveyPoint fp = currentTanker.getFuelPoint(surveyData,
                    ShowingRight);

            double startAngle = surveyAng + currentTanker.StartAngle
                    * (ShowingRight ? 1 : -1);
            double endAngle = surveyAng + currentTanker.EndAngle
                    * (ShowingRight ? 1 : -1);

            // Range and bearing from center to new position
            double[] rab = Conversions.calculateRangeAngle(fp, sp);

            // Clamp angle within tanker start and end angle
            if (ShowingRight)
                rab[1] = clampAngle(rab[1], startAngle, endAngle);
            else
                rab[1] = clampAngle(rab[1], endAngle, startAngle);

            // New position along the same or clamped angle
            double[] pos = Conversions.AROffset(fp.lat, fp.lon,
                    (float) rab[1], surveyData.getFAMDistance());

            if (surveyData.FAMPoints[side] == null)
                surveyData.FAMPoints[side] = new SurveyPoint();

            surveyData.FAMPoints[side].lat = pos[0];
            surveyData.FAMPoints[side].lon = pos[1];
            surveyData.FAMPoints[side].setHAE(sp.getHAE());
            surveyData.FAMPoints[side].circularError = surveyData
                    .getFAMDistance();
            surveyData.FAMPoints[1 - side].visible = false;
            surveyData.FAMPoints[side].visible = true;
        }

        azpc.UpdateAZ(surveyData, "FARP", true);
        UpdateDisplayMeasurements();
    }

    @Override
    public void UpdateGSRAngleUnits(boolean GSR) {
        // TODO Auto-generated method stub

    }

    @Override
    protected void stopCollection() {
        deSelectCollectionItems(true);
        setOBState(ATSKIntentConstants.OB_STATE_HIDDEN);
    }

}
