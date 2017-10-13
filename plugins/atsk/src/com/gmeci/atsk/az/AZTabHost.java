
package com.gmeci.atsk.az;

import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTabHost;
import android.view.View;
import android.widget.TabHost.OnTabChangeListener;

import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.coremap.log.Log;
import com.gmeci.atsk.resources.ATSKApplication;
import com.gmeci.core.ATSKConstants;
import com.gmeci.core.ATSKIntentConstants;
import com.gmeci.core.SurveyPoint;
import com.gmeci.atsk.obstructions.obstruction.GPSAngle_OffsetDialog;
import com.gmeci.atsk.obstructions.obstruction.GPSAngle_OffsetDialog.GPSOffsetInterface;
import com.gmeci.atsk.resources.ATSKBaseFragment;
import com.gmeci.conversions.Conversions;

abstract public class AZTabHost extends ATSKBaseFragment implements
        GPSOffsetInterface,
        OnTabChangeListener {

    protected View _root;
    public FragmentTabHost mTabHost;
    public String SELECTED_TAB = "";
    SurveyPoint OffsetPoint;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        user_settings = PreferenceManager
                .getDefaultSharedPreferences(getActivity());
    }

    public void onResume() {
        super.onResume();
        if (mTabHost != null) {
            mTabHost.setOnTabChangedListener(this);
            int storedTab = user_settings.getInt(SELECTED_TAB, 0);
            mTabHost.setCurrentTab(storedTab);
        }
    }

    @Override
    public void onPause() {
        azpc.putSetting(ATSKConstants.CURRENT_SCREEN, "", TAG);
        if (mTabHost != null) {
            int tabIndex = mTabHost.getCurrentTab();
            Log.d(TAG, "Saving tab Index: " + tabIndex);
            user_settings.edit().putInt(SELECTED_TAB, tabIndex).apply();
        }
        ATSKApplication.setObstructionCollectionMethod(
                ATSKIntentConstants.OB_STATE_HIDDEN,
                "ParkingPlanMainFragment", false);

        super.onPause();
    }

    @Override
    public void onTabChanged(final String tabId) {
        AZTabBase currentTab = (AZTabBase) getChildFragmentManager()
                .findFragmentByTag(tabId);
        if (currentTab == null) {
            new Handler().postDelayed(new Runnable() {

                @Override
                public void run() {
                    onTabChanged(tabId);
                }
            }, 300);
        } else {
            ATSKApplication.setObstructionCollectionMethod(
                    ATSKIntentConstants.OB_STATE_HIDDEN,
                    "ObstructionMainFragment", false);
            currentTab.SetSurveyInterface();
        }
    }

    @Override
    public void MapClickDetected(SurveyPoint sp) {

        String RABString = "";
        SurveyPoint myPos;
        try {
            if (hardwareInterface != null)
                myPos = hardwareInterface.getMostRecentPoint();
            else
                myPos = new SurveyPoint(sp);
            double RAB[] = Conversions.calculateRangeAngle(myPos, sp);
            if (RAB[0] > 1000) {
                //use miles
                if (RAB[0] > 50000) {
                    RABString = String.format(" %s",
                            Conversions.GetCardinalDirection(RAB[1]));
                } else if (RAB[0] < 1)
                    RABString = "SAME POSITION";
                else
                    RABString = String.format(LocaleUtil.getCurrent(),
                            "%.1fNM@ %s", RAB[0] * Conversions.M2NM,
                            Conversions.GetCardinalDirection(RAB[1]));
            } else
                RABString = String.format(LocaleUtil.getCurrent(),
                        "%.1fft@ %s", RAB[0] * Conversions.M2F,
                        Conversions.GetCardinalDirection(RAB[1]));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "map click detected: " + sp.lat + ", "
                + sp.lon + ", " + sp.alt);

        String AltString = String.format(LocaleUtil.getCurrent(),
                "ALT:%.1fft   %s", sp.getMSL() * Conversions.M2F, RABString);
        UpdateNotification(getActivity(), "Map Clicked",
                Conversions.GetMGRS(sp),
                Conversions.GetLatLonDM(sp.lat, sp.lon),
                AltString);

        if (isOBWaitingForClick())
            newPosition(sp, false);
    }

    public boolean newPosition(SurveyPoint sp, boolean TopCollected) {

        AZTabBase currentTab = getCurrentTab();
        if (currentTab != null) {
            currentTab.newPosition(sp, TopCollected);
            return true;
        }
        return false;
    }

    protected void SelectedPointPlusOffset(boolean TopCollected) {
        GPSSinglePoint(pullGPS(), true, TopCollected);
    }

    protected void SelectedPoint(boolean TopCollected) {
        if (hardwareInterface != null) {
            try {
                newPosition(hardwareInterface.getMostRecentPoint(),
                        TopCollected);
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to get most recent point", e);
            }
        }
    }

    @Override
    public void RangeDirectionSelected(double Range_m, double Angle_true,
            boolean TopCollected) {
        double[] OffsetedPoint = Conversions.AROffset(OffsetPoint.lat,
                OffsetPoint.lon, Angle_true, Range_m);

        AZTabBase curTab = getCurrentTab();
        if (curTab != null) {
            SurveyPoint sp = new SurveyPoint(OffsetPoint);
            sp.lat = OffsetedPoint[0];
            sp.lon = OffsetedPoint[1];
            curTab.newPosition(sp, TopCollected);
        }

        UpdateNotification(getActivity(), "GPS Point",
                Conversions.GetLatLonDM(OffsetedPoint[0], OffsetedPoint[1]),
                Conversions.GetMGRS(OffsetedPoint[0], OffsetedPoint[1]), "");
    }

    public void GPSSinglePoint(SurveyPoint sp, boolean WithOffset,
            boolean TopCollected) {
        if (sp != null) {
            if (WithOffset) {
                OffsetPoint = sp;
                GPSAngle_OffsetDialog gaod = new GPSAngle_OffsetDialog();
                gaod.Initialize(this, sp.lat, sp.lon, getActivity());
                gaod.show();
            } else {
                AZTabBase currentTab = getCurrentTab();
                if (currentTab != null)
                    currentTab.newPosition(sp, TopCollected);

                UpdateNotification(
                        getActivity(),
                        "GPS Point",
                        Conversions.GetMGRS(sp),
                        Conversions.GetLatLonDM(sp.lat, sp.lon),
                        "");
            }
        }
    }

    @Override
    public void SetSurveyInterface() {
        super.SetSurveyInterface();
        SetCurrentTabAZSI();
    }

    private void SetCurrentTabAZSI() {
        AZTabBase currentTab = getCurrentTab();
        if (currentTab != null) {
            currentTab.SetSurveyInterface();
            return;
        }
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                SetCurrentTabAZSI();
            }
        }, 300);
    }

    @Override
    public void shotApproved(SurveyPoint sp, double range_m, double az_deg,
            double el_deg, boolean TopCollected) {
        super.shotApproved(sp, range_m, az_deg, el_deg, TopCollected);
        AZTabBase currentTab = getCurrentTab();
        if (currentTab != null)
            currentTab.shotApproved(sp, range_m, az_deg, el_deg, TopCollected);
    }

    protected AZTabBase getCurrentTab() {
        if (mTabHost != null) {
            String tag = mTabHost.getCurrentTabTag();
            if (tag != null) {
                Fragment frag = getChildFragmentManager()
                        .findFragmentByTag(tag);
                if (frag instanceof AZTabBase)
                    return (AZTabBase) frag;
            }
        }
        return null;
    }

    public void updateSurvey(String surveyUID) {
        AZTabBase curTab = getCurrentTab();
        if (curTab != null)
            curTab.updateSurvey(surveyUID);
    }

    public void postRecalc() {
        AZTabBase curTab = getCurrentTab();
        if (curTab != null)
            curTab.postRecalc();
    }
}
