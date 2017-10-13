
package com.gmeci.atsk.vehicle;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.atakmap.android.ipc.AtakBroadcast.DocumentedIntentFilter;
import android.os.Bundle;

import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.toolbar.Tool;
import com.atakmap.android.toolbar.ToolManagerBroadcastReceiver;
import com.atakmap.android.toolbar.widgets.TextContainer;
import com.atakmap.coremap.log.Log;
import com.gmeci.atsk.map.ATSKVehicle;
import com.gmeci.atsk.resources.ATSKApplication;
import com.gmeci.conversions.Conversions;
import com.gmeci.core.ATSKConstants;
import com.gmeci.core.ATSKIntentConstants;
import com.gmeci.core.SurveyPoint;

/**
 * Rotation tool for vehicles
 */
public class VehicleRotationTool extends Tool {

    public static final String TAG = "VehicleRotationTool";
    public static final String TOOL_IDENTIFIER = "atsk_vehicle_rotation_tool";

    private ATSKVehicle _vehicle;
    private final TextContainer _cont;
    private final MapView _mapView;

    public VehicleRotationTool(MapView mapView) {
        super(mapView, TOOL_IDENTIFIER);
        _cont = TextContainer.getInstance();
        _mapView = mapView;
        ToolManagerBroadcastReceiver.getInstance()
                .registerTool(TOOL_IDENTIFIER, this);
    }

    @Override
    public boolean onToolBegin(Bundle extras) {
        super.onToolBegin(extras);

        String uid = extras.getString("uid", "");

        // Register map click, LRF, and GPS receivers
        DocumentedIntentFilter filter = new DocumentedIntentFilter();
        filter.addAction(ATSKConstants.MAP_CLICK_ACTION);
        filter.addAction(ATSKConstants.NOTIFICATION_BUBBLE_LRF_APPROVED);
        AtakBroadcast.getInstance().registerReceiver(_mapReceiver, filter);
        _mapView.getMapTouchController().setToolActive(true);

        // Find matching vehicle obstruction
        _vehicle = ATSKVehicle.find(uid);
        if (_vehicle != null) {
            _cont.displayPrompt("Tap to change vehicle heading.");
            return true;
        }
        return false;
    }

    @Override
    public void onToolEnd() {
        if (!ATSKApplication.getCollectionState()
                .equals(ATSKIntentConstants.OB_STATE_HIDDEN))
            ATSKApplication.setObstructionCollectionMethod(
                    ATSKIntentConstants.OB_STATE_HIDDEN, TAG, false);
        AtakBroadcast.getInstance().unregisterReceiver(_mapReceiver);
        _mapView.getMapTouchController().setToolActive(false);
        _cont.closePrompt();
    }

    @Override
    public void dispose() {
    }

    private final BroadcastReceiver _mapReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (_vehicle == null) {
                Log.w(TAG, "Vehicle is null");
                return;
            }
            if (intent.getExtras() == null) {
                Log.w(TAG, "Intent contains no extra data");
                return;
            }

            String action = intent.getAction();

            // Retrieve latlon coordinates from intent based on action
            double lat, lon;
            if (action.equals(ATSKConstants.MAP_CLICK_ACTION) || action.equals(
                    ATSKConstants.NOTIFICATION_BUBBLE_LRF_APPROVED)) {
                // Map click or LRF
                lat = intent
                        .getDoubleExtra(ATSKConstants.LAT_EXTRA, Double.NaN);
                lon = intent
                        .getDoubleExtra(ATSKConstants.LON_EXTRA, Double.NaN);
            } else
                return;

            if (Double.isNaN(lat) || Double.isNaN(lon)) {
                Log.w(TAG, "Invalid map coordinates received from intent");
                return;
            }
            SurveyPoint loc = _vehicle.getLocation();
            _vehicle.setHeading(Conversions.CalculateAngledeg(loc.lat, loc.lon,
                    lat, lon));
            _vehicle.save();
            requestEndTool();
        }
    };
}
