
package com.gmeci.atsk.vehicle;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTabHost;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TabHost.OnTabChangeListener;

import com.atakmap.android.atsk.plugin.R;
import com.atakmap.coremap.log.Log;
import com.gmeci.atsk.resources.ATSKApplication;
import com.gmeci.core.ATSKIntentConstants;
import com.gmeci.core.SurveyPoint;
import com.gmeci.atsk.resources.ATSKBaseFragment;

public class VehicleTabHost extends ATSKBaseFragment
        implements OnTabChangeListener {

    private static final String TAG = "VehicleTabHost";

    private static final String AIRCRAFT_TAB = "Aircraft";
    private Context _plugin;
    private FragmentTabHost _tabHost;
    private LayoutInflater _inflater;
    private View _root;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        _plugin = ATSKApplication.getInstance().getPluginContext();
        _inflater = LayoutInflater.from(_plugin);
        _root = _inflater.inflate(R.layout.vehicle_main, container, false);

        _tabHost = (FragmentTabHost) _root.findViewById(android.R.id.tabhost);
        _tabHost.setup(_plugin, getChildFragmentManager(),
                R.id.realtabcontent);

        // Aircraft obstructions tab
        _tabHost.addTab(_tabHost.newTabSpec(AIRCRAFT_TAB).
                setIndicator("", _plugin.getResources().getDrawable(
                        R.drawable.aircraft_tab)),
                VehicleTabBase.class, null);

        for (int i = 0; i < _tabHost.getTabWidget().getChildCount(); i++) {
            _tabHost.getTabWidget().getChildAt(i).setPadding(0, 0, 0, 0);
        }
        return _root;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        _tabHost.setOnTabChangedListener(this);
        _tabHost.setCurrentTab(0);
    }

    @Override
    public void onTabCreated(Fragment tabFrag) {
        if (_tabHost != null && tabFrag != null
                && tabFrag instanceof VehicleTabBase)
            runOnCreate();
    }

    @Override
    public void onTabChanged(final String tabId) {
        /*VehicleTabBase tab = (VehicleTabBase) getChildFragmentManager()
                .findFragmentByTag(tabId);
        if (tab == null)
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    onTabChanged(tabId);
                }
            }, 300);*/
    }

    public void MapClickDetected(SurveyPoint point) {
        super.MapClickDetected(point);
        Log.d(TAG,
                "MapClickDetected - "
                        + ATSKApplication.getCollectionState());
        if (ATSKApplication.getCollectionState().equals(
                ATSKIntentConstants.OB_STATE_MAP_CLICK))
            newPosition(point, false);
    }

    @Override
    public void shotApproved(SurveyPoint sp, double range_m, double az_deg,
            double el_deg, boolean top) {
        super.shotApproved(sp, range_m, az_deg, el_deg, top);
        Log.d(TAG, "ShotApproved");
        newPosition(sp, top);
    }

    private void newPosition(SurveyPoint sp, boolean top) {
        VehicleTabBase tab = getTab();
        if (tab != null)
            tab.newPosition(sp, top);
    }

    @Override
    protected void SelectedPoint(boolean top) {
        Log.d(TAG, "SelectPoint");
        newPosition(pullGPS(), top);
    }

    @Override
    protected void SelectedPointPlusOffset(boolean TopCollected) {
        Log.d(TAG, "SelectedPointPlusOffset");
    }

    public void editVehicle(final String uid, final int copies) {
        if (!_created) {
            runOnCreate(new Runnable() {
                public void run() {
                    editVehicle(uid, copies);
                }
            });
            return;
        }
        VehicleTabBase tab = getTab();
        if (tab != null)
            tab.editVehicle(uid, copies);
    }

    private VehicleTabBase getTab() {
        return (VehicleTabBase) getChildFragmentManager().
                findFragmentByTag(_tabHost.getCurrentTabTag());
    }
}
