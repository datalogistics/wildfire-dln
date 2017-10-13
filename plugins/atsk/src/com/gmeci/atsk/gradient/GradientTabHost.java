
package com.gmeci.atsk.gradient;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.ipc.AtakBroadcast.DocumentedIntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTabHost;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabSpec;

import com.atakmap.android.atsk.plugin.R;
import com.gmeci.atsk.resources.ATSKApplication;
import com.gmeci.core.ATSKConstants;
import com.gmeci.core.LineObstruction;
import com.gmeci.core.PointObstruction;
import com.gmeci.core.SurveyData;
import com.gmeci.core.SurveyData.AZ_TYPE;
import com.gmeci.core.SurveyPoint;
import com.gmeci.atsk.obstructions.obstruction.GPSAngle_OffsetDialog;
import com.gmeci.atsk.obstructions.obstruction.GPSAngle_OffsetDialog.GPSOffsetInterface;
import com.gmeci.atsk.resources.ATSKBaseFragment;
import com.gmeci.constants.Constants;
import com.gmeci.helpers.PolygonHelper;
import com.gmeci.conversions.Conversions;

public class GradientTabHost extends ATSKBaseFragment implements
        GPSOffsetInterface, OnTabChangeListener {
    private static final String COLLECTION_TAB = "Collection";
    private static final String ANALYSIS_TAB = "Analysis";
    private static final String SURFACE_DISTRESS_TAB = "Surface Distress";
    private static final String EDIT_TAB = "Surface Distress Edit";
    private static final String SELECTED_TAB = "Selected Gradient Tab";
    private static final String TAG = "GradientMainFragment";
    @SuppressWarnings("unused")
    public FragmentTabHost mTabHost;
    SurveyPoint OffsetPoint;
    SurveyData _survey;
    View _root;
    String ClickedGroup = "";
    String ClickedUID = "";
    TabSpec EditSurfaceDistressTabSpec;
    private ProgressBar gradientDrawPB;
    final BroadcastReceiver GradientProgressRX = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(
                    ATSKConstants.GRADIENT_DRAW_PROGRESS_ACTION)) {
                Bundle extras = intent.getExtras();
                int total = extras
                        .getInt(ATSKConstants.GRADIENT_DRAW_PROGRESS_TOTAL);
                int current = extras
                        .getInt(ATSKConstants.GRADIENT_DRAW_PROGRESS_CURRENT);

                if (current >= 3f * total / 4.0f || current == total - 1) {
                    gradientDrawPB.setVisibility(View.GONE);
                } else {
                    gradientDrawPB.setVisibility(View.VISIBLE);
                    gradientDrawPB.setMax(total);
                    gradientDrawPB.setProgress(current);
                }
            }

        }

    };
    private LayoutInflater mInflater;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        Context pluginContext = ATSKApplication
                .getInstance().getPluginContext();
        mInflater = LayoutInflater.from(pluginContext);
        _root = mInflater.inflate(R.layout.gradient_main, container, false);

        mTabHost = (FragmentTabHost) _root.findViewById(android.R.id.tabhost);
        mTabHost.setup(pluginContext, getChildFragmentManager(),
                R.id.realtabcontent);

        gradientDrawPB = (ProgressBar) _root.findViewById(R.id.gradientDrawPB);
        gradientDrawPB.setVisibility(View.GONE);
        AddNewGradientFragmentsToTabHost();

        return _root;
    }

    @Override
    public void onPause() {

        this.azpc.putSetting(ATSKConstants.CURRENT_SCREEN, "", TAG);
        com.atakmap.android.ipc.AtakBroadcast.getInstance().unregisterReceiver(
                GradientProgressRX);

        Intent DrawGradientRequest = new Intent(
                ATSKConstants.SHOW_HIDE_GRADIENT);
        DrawGradientRequest.putExtra(ATSKConstants.GRADIENT_CLASS,
                ATSKConstants.GRADIENT_CLASS_ALL);
        DrawGradientRequest.putExtra(ATSKConstants.SHOW, false);
        com.atakmap.android.ipc.AtakBroadcast.getInstance().sendBroadcast(
                DrawGradientRequest);

        opc.DeleteLine(ATSKConstants.DEFAULT_GROUP,
                ATSKConstants.GRADIENT_LEFT_LIMIT_UID);
        opc.DeleteLine(ATSKConstants.DEFAULT_GROUP,
                ATSKConstants.GRADIENT_RIGHT_LIMIT_UID);
        opc.DeleteLine(ATSKConstants.DEFAULT_GROUP,
                ATSKConstants.LONGITUDINAL_LIMIT_UID);

        opc.DeletePoints(ATSKConstants.GRADIENT_GROUP);
        super.onPause();
        user_settings.edit().putInt(SELECTED_TAB, mTabHost.getCurrentTab())
                .apply();

    }

    @Override
    public void onResume() {
        DocumentedIntentFilter filter = new DocumentedIntentFilter();
        filter.addAction(ATSKConstants.GRADIENT_DRAW_PROGRESS_ACTION);
        com.atakmap.android.ipc.AtakBroadcast.getInstance().registerReceiver(
                GradientProgressRX, filter);

        //request drawing all gradients
        Intent DrawGradientRequest = new Intent(
                ATSKConstants.SHOW_HIDE_GRADIENT);
        DrawGradientRequest.putExtra(ATSKConstants.GRADIENT_CLASS,
                ATSKConstants.GRADIENT_CLASS_ALL);
        DrawGradientRequest.putExtra(ATSKConstants.SHOW, true);
        com.atakmap.android.ipc.AtakBroadcast.getInstance().sendBroadcast(
                DrawGradientRequest);
        super.onResume();
        this.azpc.putSetting(ATSKConstants.CURRENT_SCREEN,
                ATSKConstants.CURRENT_SCREEN_GRADIENT, TAG);

        _survey = azpc.getAZ(azpc.getSetting(
                ATSKConstants.CURRENT_SURVEY, "GradientTabHost"), false);
        if (_survey != null
                && (_survey.getType() == AZ_TYPE.LZ
                        || _survey.getType() == AZ_TYPE.LTFW
                        || _survey.getType() == AZ_TYPE.STOL)) {

            // Gradient Left imaginary limit line drawing for user use.
            LineObstruction GradientLeft = PolygonHelper
                    .getLeftGradientLimitArea(_survey);
            GradientLeft.group = ATSKConstants.DEFAULT_GROUP;
            GradientLeft.closed = true;
            GradientLeft.filled = true;
            opc.NewLine(GradientLeft);

            // Gradient Right imaginary limit line drawing for user use.
            LineObstruction GradientRight = PolygonHelper
                    .getRightGradientLimitArea(_survey);
            GradientRight.group = ATSKConstants.DEFAULT_GROUP;
            GradientRight.closed = true;
            GradientRight.filled = true;
            opc.NewLine(GradientRight);

            // Longitudinal filter
            LineObstruction lonLimit = PolygonHelper
                    .getLongitudinalLimitArea(_survey);
            opc.NewLine(lonLimit);
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mTabHost.setOnTabChangedListener(this);
        int Tab2Open = user_settings.getInt(SELECTED_TAB, 0);
        mTabHost.setCurrentTab(Tab2Open);

    }

    @Override
    public void onTabCreated(Fragment tabFrag) {
        if (mTabHost != null && tabFrag != null
                && tabFrag instanceof GradientTabBase) {
            initCurrentTab();
            runOnCreate();
        }
    }

    @Override
    public void onTabChanged(final String tabId) {
        initCurrentTab();
    }

    private void initCurrentTab() {
        GradientTabBase currentTab = (GradientTabBase) getChildFragmentManager()
                .findFragmentByTag(mTabHost.getCurrentTabTag());
        if (currentTab != null) {
            currentTab.hardwareInterface = this.hardwareInterface;
            if (currentTab instanceof GradientCollectionFragment) {
                currentTab.hardwareInterface = this.hardwareInterface;
            }
            currentTab.SetSurveyInterface();
            if (currentTab instanceof EditSurfaceDistressFragment) {
                ((EditSurfaceDistressFragment) currentTab)
                        .setParentInterface(this);
            }
        }
    }

    public void CloseEditWindow(boolean returnToNewWindow) {
        if (returnToNewWindow) {
            AddNewGradientFragmentsToTabHost();
        }
    }

    private void AddNewGradientFragmentsToTabHost() {
        Context pluginContext = ATSKApplication
                .getInstance().getPluginContext();

        mTabHost.clearAllTabs();
        TabSpec CollectionTabSpec = mTabHost
                .newTabSpec(COLLECTION_TAB)
                .setIndicator(
                        "",
                        pluginContext.getResources().getDrawable(
                                R.drawable.gradient_collect));
        mTabHost.addTab(CollectionTabSpec, GradientCollectionFragment.class,
                null);

        TabSpec ProcessTabSpec = mTabHost
                .newTabSpec(ANALYSIS_TAB)
                .setIndicator(
                        "",
                        pluginContext.getResources().getDrawable(
                                R.drawable.gradient_analyze));
        mTabHost.addTab(ProcessTabSpec, GradientProcessFragment.class, null);

        TabSpec SurfaceDistressTabSpec = mTabHost.newTabSpec(
                SURFACE_DISTRESS_TAB).setIndicator(
                "",
                pluginContext.getResources().getDrawable(
                        R.drawable.gradient_sd_tab));
        mTabHost.addTab(SurfaceDistressTabSpec,
                GradientSurfaceDistressFragment.class, null);

        for (int i = 0; i < mTabHost.getTabWidget().getChildCount(); i++) {
            mTabHost.getTabWidget().getChildAt(i).setPadding(0, 0, 0, 0);
        }
    }

    @Override
    public void shotDetected(SurveyPoint sp, double range_m,
            double azimuth_deg,
            double elev_deg, boolean CollectingTop) {

        if (!user_settings.getBoolean(
                Constants.REQUIRE_LRF_APPROVAL, true)) {
            shotApproved(sp, range_m, azimuth_deg, elev_deg);
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
        AtakBroadcast.getInstance().sendBroadcast(i);
    }

    public void shotApproved(SurveyPoint sp, double range, double azimuth,
            double elev) {
        String tag = mTabHost.getCurrentTabTag();
        if (tag != null && tag.equals(SURFACE_DISTRESS_TAB)) {
            Fragment test = getChildFragmentManager().findFragmentByTag(
                    SURFACE_DISTRESS_TAB);
            if (test instanceof GradientSurfaceDistressFragment) {
                GradientSurfaceDistressFragment currentTab = (GradientSurfaceDistressFragment) test;
                currentTab.newPosition(sp);
            }
        }
    }

    public void GPSSinglePoint(SurveyPoint sp, boolean withOffset,
            boolean TopCollected) {

        String Tag = mTabHost.getCurrentTabTag();
        Fragment test = getChildFragmentManager().findFragmentByTag(Tag);

        UpdateMapClickNotification(sp);
        if (test instanceof GradientSurfaceDistressFragment) {
            GradientSurfaceDistressFragment currentTab = (GradientSurfaceDistressFragment) test;
            if (withOffset) {
                OffsetPoint = sp;
                GPSAngle_OffsetDialog gaod = new GPSAngle_OffsetDialog();
                gaod.Initialize(this, sp.lat, sp.lon,
                        this.getActivity());
                gaod.show();
            } else {
                currentTab.newPosition(sp);
            }
        }
    }

    private void UpdateMapClickNotification(SurveyPoint clickPoint) {
        String RABString = "";
        if (null != hardwareInterface) {
            SurveyPoint MyPosition;
            try {
                MyPosition = hardwareInterface.getMostRecentPoint();
                double RAB[] = Conversions.CalculateRangeAngle(
                        MyPosition.lat, MyPosition.lon, clickPoint.lat,
                        clickPoint.lon);
                if (RAB[0] > 1000) {
                    //use miles
                    if (RAB[0] > 50000) {
                        RABString = String.format(" %s",
                                Conversions.GetCardinalDirection(RAB[1]));
                    } else
                        RABString = String.format("%.1fNM@ %s", RAB[0]
                                * Conversions.M2NM,
                                Conversions.GetCardinalDirection(RAB[1]));
                } else
                    RABString = String.format("%.1fft@ %s", RAB[0]
                            * Conversions.M2F,
                            Conversions.GetCardinalDirection(RAB[1]));
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        String AltString = String.format("ALT:%.1fft   %s",
                clickPoint.getHAE() * Conversions.M2F, RABString);
        UpdateNotification(getActivity(), "Map Clicked",
                Conversions.getCoordinateString(clickPoint.lat, clickPoint.lon,
                        user_settings.getString(ATSKConstants.COORD_FORMAT,
                                Conversions.COORD_FORMAT_MGRS)), AltString, "");
    }

    protected void SelectedPointPlusOffset(boolean TopCollected) {

        this.GPSSinglePoint(this.pullGPS(), true, TopCollected);
    }

    protected void SelectedPoint(boolean TopCollected) {

        this.GPSSinglePoint(this.pullGPS(), false, TopCollected);

    }

    public void MapClickDetected(SurveyPoint ClickPoint) {
        GPSSinglePoint(ClickPoint, false, false);
    }

    private void SetCurrentTabAZSI() {
        if (mTabHost != null) {
            String Tag = mTabHost.getCurrentTabTag();
            if (Tag != null) {
                GradientTabBase currentTab = (GradientTabBase) getChildFragmentManager()
                        .findFragmentByTag(Tag);
                if (currentTab != null) {
                    currentTab.hardwareInterface = this.hardwareInterface;
                    currentTab.SetSurveyInterface();
                    return;
                }
            }
        }
        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                SetCurrentTabAZSI();
            }
        }, 300);

    }

    @Override
    public void SetSurveyInterface() {
        super.SetSurveyInterface();
        SetCurrentTabAZSI();
    }

    @Override
    public void shotApproved(SurveyPoint sp, double range_m, double az_deg,
            double el_deg, boolean CollectingTop) {
    }

    @Override
    public void RangeDirectionSelected(double Range_m, double Angle_true,
            boolean TopCollected) {
        double[] OffsetedPoint = Conversions.AROffset(OffsetPoint.lat,
                OffsetPoint.lon, Angle_true, Range_m);
        SurveyPoint NewOffsetPoint = new SurveyPoint(OffsetedPoint[0],
                OffsetedPoint[1]);
        NewOffsetPoint.alt = OffsetPoint.alt;
        NewOffsetPoint.circularError = OffsetPoint.circularError;
        NewOffsetPoint.linearError = OffsetPoint.linearError;

        GPSSinglePoint(NewOffsetPoint, false, false);

        UpdateNotification(getActivity(), "GPS Point Offset",
                Conversions.GetLatLonDM(OffsetedPoint[0], OffsetedPoint[1]),
                "", "");

    }

    public boolean ObstructionSelectedOnMap(final String group, final String uid) {

        if (!_created) {
            runOnCreate(new Runnable() {
                public void run() {
                    ObstructionSelectedOnMap(group, uid);
                }
            });
            return false;
        }

        Context pluginContext = ATSKApplication
                .getInstance().getPluginContext();
        ClickedGroup = group;
        ClickedUID = uid;
        PointObstruction clickedPO = opc.GetPointObstruction(ClickedGroup,
                ClickedUID);
        if (clickedPO == null) {
            Intent intent = new Intent();
            intent.setAction(ATSKConstants.ATSK_MAP_CHANGE);
            intent.putExtra(ATSKConstants.SELECTED, false);
            com.atakmap.android.ipc.AtakBroadcast.getInstance().sendBroadcast(
                    intent);
            return false;
        }

        mTabHost.clearAllTabs();
        //        mTabHost.removeAllViews();
        EditSurfaceDistressTabSpec = mTabHost
                .newTabSpec(EDIT_TAB)
                .setIndicator(
                        "",
                        pluginContext.getResources().getDrawable(
                                R.drawable.obstruction_point_edit));
        mTabHost.addTab(EditSurfaceDistressTabSpec,
                EditSurfaceDistressFragment.class, null);

        mTabHost.invalidate();
        return false;
    }
}
