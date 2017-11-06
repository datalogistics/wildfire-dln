
package com.gmeci.atsk.visibility;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.atakmap.android.imagecapture.GridTool;
import com.atakmap.android.imagecapture.GridToolbar;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.ipc.AtakBroadcast.DocumentedIntentFilter;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.toolbar.ToolManagerBroadcastReceiver;
import com.atakmap.android.toolbar.ToolbarBroadcastReceiver;
import com.gmeci.atsk.az.currentsurvey.CurrentSurveyFragment;
import com.gmeci.atsk.resources.ATSKBaseFragment;
import com.gmeci.atsk.toolbar.ATSKToolbar;
import com.gmeci.atsk.toolbar.ATSKToolbarComponent;
import com.gmeci.core.SurveyPoint;

public class VizFragment extends ATSKBaseFragment {

    private static final String TAG = "VizFragment";

    private final MapView _mapView;
    private final CurrentSurveyFragment _survey;
    private VizLayout _layout;
    private boolean _registered = false;

    public VizFragment(MapView mapView, CurrentSurveyFragment survey) {
        _mapView = mapView;
        _survey = survey;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        if (_mapView == null || _survey == null)
            return null;
        if (_layout != null)
            _layout.dispose();
        _layout = new VizLayout(container);
        _layout.setMapView(_mapView);
        _layout.setSurvey(_survey);
        return _layout.getRootView();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (_layout != null)
            _layout.init();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (_layout != null)
            _layout.resume();
        if (!_registered) {
            AtakBroadcast.getInstance().registerReceiver(_receiver,
                    new DocumentedIntentFilter(
                            ToolManagerBroadcastReceiver.END_TOOL));
            _registered = true;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (_layout != null)
            _layout.pause();
        if (_registered)
            AtakBroadcast.getInstance().unregisterReceiver(_receiver);
        _registered = false;
    }

    @Override
    public void onDestroy() {
        if (_layout != null)
            _layout.dispose();
        _layout = null;
        super.onDestroy();
    }

    @Override
    public boolean onBackButtonPressed() {
        String active = ToolbarBroadcastReceiver.getInstance().getActive();
        if (active != null && (active.equals(GridToolbar.IDENTIFIER)
                || active.equals(ATSKToolbar.IDENTIFIER))) {
            ATSKToolbarComponent.getToolbar().closeToolbar();
            return true;
        }
        return false;
    }

    @Override
    public void shotApproved(SurveyPoint sp, double range_m, double az_deg,
            double el_deg, boolean TopCollected) {
    }

    private final BroadcastReceiver _receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ToolManagerBroadcastReceiver.END_TOOL)) {
                String tool = intent.getStringExtra("tool");
                if (tool.equals(GridTool.TOOL_IDENTIFIER)
                        || tool.equals(MapSelectTool.TOOL_IDENTIFIER))
                    onBackButtonPressed();
            }
        }
    };
}
