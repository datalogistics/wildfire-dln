
package com.gmeci.atsk.obstructions;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.atakmap.android.ipc.AtakBroadcast.DocumentedIntentFilter;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.atakmap.android.atsk.plugin.R;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapEvent;
import com.atakmap.android.maps.MapEventDispatcher;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.menu.MenuLayoutWidget;
import com.atakmap.android.toolbar.ToolManagerBroadcastReceiver;
import com.atakmap.android.tools.ActionBarView;
import com.atakmap.android.widgets.LayoutWidget;
import com.atakmap.android.widgets.MapWidget;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.gmeci.atsk.MapHelper;
import com.gmeci.atsk.resources.ATSKApplication;
import com.gmeci.atsk.toolbar.ATSKBaseToolbar;
import com.gmeci.atsk.toolbar.ATSKToolbar;
import com.gmeci.atsk.toolbar.ATSKToolbarComponent;
import com.gmeci.atsk.toolbar.GPSPopupWindow;
import com.gmeci.atsk.toolbar.LRFPopupWindow;
import com.gmeci.core.ATSKConstants;
import com.gmeci.core.ATSKIntentConstants;
import com.gmeci.core.SurveyPoint;

/**
 * Toolbar for placing obstruction markers
 */
public class ObstructionToolbar implements ATSKBaseToolbar,
        View.OnClickListener, View.OnLongClickListener {

    private static final String TAG = "ObstructionToolbar";

    private final ATSKToolbar _manager;
    private ActionBarView _root;
    private final MapView _mapView;
    private String _toolId;
    private ImageButton _mapButton, _lrfButton, _heightButton, _gpsButton;

    private LRFPopupWindow lrfMenuPopup;
    private GPSPopupWindow gpsMenuPopup;
    private String CurrentObstructionState = ATSKIntentConstants.OB_STATE_HIDDEN;
    private boolean BreadcrumbsCollecting = false;
    private boolean Pull2PPlusDFirstPointCollected = false;
    private boolean TopCollection = true;
    private boolean CollectingLine = false;
    private boolean CollectingArea = false;
    private int CollectingHeight = -1;
    private boolean ButtonsOpened = false;

    public ObstructionToolbar(ATSKToolbar manager, MapView mapView) {
        _manager = manager;
        _mapView = mapView;
    }

    final BroadcastReceiver OBInputReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            Bundle values = intent.getExtras();
            boolean closing = false;

            if (action
                    .equals(ATSKIntentConstants.OB_STATE_ACTION)) {
                String src = values.getString(
                        ATSKIntentConstants.OB_STATE_SOURCE,
                        "None");
                if (src.equals(TAG)) {
                    //we requested this change - we probably shouldn't listen to it because we already updated
                    return;
                }
                if (values
                        .containsKey(ATSKIntentConstants.OB_STATE_ACTION)) {
                    //Update the display state
                    String newState = values
                            .getString(ATSKIntentConstants.OB_STATE_ACTION);

                    Log.d(TAG, "state of the toolbar: " + newState);

                    //open/ close the stuff we need
                    if (newState
                            .equals(ATSKIntentConstants.OB_STATE_REQUESTED_HIDDEN)
                            || newState
                                    .equals(ATSKIntentConstants.OB_STATE_HIDDEN)) {
                        //hide the toolbar
                        setButtonOpened(false);
                        closing = true;
                    } else if (newState
                            .equals(ATSKIntentConstants.OB_STATE_REQUESTED_POINT)) {
                        setButtonOpened(true, false, false);
                    } else if (newState
                            .equals(ATSKIntentConstants.OB_STATE_REQUESTED_ROUTE)) {
                        setButtonOpened(true, true, false);
                    } else if (newState
                            .equals(ATSKIntentConstants.OB_STATE_REQUESTED_AREA)) {
                        setButtonOpened(true, false, true);
                    } else if (newState
                            .equals(ATSKIntentConstants.OB_STATE_LRF)) {
                        setButtonOpened(true, CollectingLine, CollectingArea);
                        CurrentObstructionState = ATSKIntentConstants.OB_STATE_LRF;
                        refresh();
                    } else if (newState
                            .equals(ATSKIntentConstants.OB_STATE_2PPLUSD_LRF_2)) {
                        setButtonOpened(true, CollectingLine, CollectingArea);
                        CurrentObstructionState = ATSKIntentConstants.OB_STATE_2PPLUSD_LRF_2;
                        refresh();
                    } else if (newState
                            .equals(ATSKIntentConstants.OB_STATE_2PPLUSD_GPS)) {
                    } else {
                        CurrentObstructionState = newState;
                        refresh();
                    }
                }

                if (!closing) {
                    boolean OldTopCollection = TopCollection;
                    TopCollection = values.getBoolean(
                            ATSKIntentConstants.OB_COLLECT_TOP,
                            true);
                    if (TopCollection != OldTopCollection)
                        refresh();

                    String toolId = values.getString("tool");
                    Bundle toolExtras = values.getBundle("toolExtras");
                    if (toolId == null || _toolId == null
                            || !toolId.equals(_toolId))
                        endActiveTool();
                    _toolId = toolId;
                    if (_toolId != null) {
                        Intent begin = new Intent();
                        begin.setAction(ToolManagerBroadcastReceiver.BEGIN_TOOL);
                        begin.putExtra("tool", _toolId);
                        if (toolExtras != null)
                            begin.putExtras(toolExtras);
                        AtakBroadcast.getInstance().sendBroadcast(begin);
                    }
                }
            }
        }
    };

    private void endActiveTool() {
        if (_toolId != null) {
            Intent end = new Intent();
            end.setAction(ToolManagerBroadcastReceiver.END_TOOL);
            end.putExtra("tool", _toolId);
            AtakBroadcast.getInstance().sendBroadcast(end);
            _toolId = null;
        }
    }

    public synchronized void setupView() {
        if (_root == null) {
            Context plugin = ATSKApplication.getInstance().getPluginContext();
            LayoutInflater inflater = LayoutInflater.from(plugin);
            _root = (ActionBarView) inflater.inflate(
                    R.layout.obstruction_toolbar, _mapView,
                    false);
            _root.setEmbedded(false);
            _root.setClosable(false);

            _mapButton = (ImageButton) _root
                    .findViewById(R.id.atsk_toolbar_image_map);
            _lrfButton = (ImageButton) _root
                    .findViewById(R.id.atsk_toolbar_image_lrf);
            _gpsButton = (ImageButton) _root
                    .findViewById(R.id.atsk_toolbar_image_gps);
            _heightButton = (ImageButton) _root
                    .findViewById(R.id.atsk_toolbar_image_height);

            // Location buttons
            _mapButton.setOnClickListener(this);
            _lrfButton.setOnClickListener(this);
            _lrfButton.setOnLongClickListener(this);
            _gpsButton.setOnClickListener(this);
            _gpsButton.setOnLongClickListener(this);
            _heightButton.setOnClickListener(this);
            setupStateReceiver();
        }
    }

    @Override
    public synchronized ActionBarView getView() {
        return _root;
    }

    @Override
    public synchronized int[] getBounds() {
        int bounds[] = new int[4];
        if (_root != null) {
            bounds[0] = _mapButton.getTop();
            bounds[1] = _mapButton.getLeft();
            bounds[2] = _mapButton.getBottom();
            bounds[3] = _mapButton.getRight();
        }
        return bounds;
    }

    @Override
    public boolean onBackButtonPressed() {
        return false;
    }

    private void setupStateReceiver() {
        DocumentedIntentFilter filter = new DocumentedIntentFilter();
        filter.addAction(ATSKIntentConstants.OB_STATE_ACTION);
        AtakBroadcast.getInstance().registerReceiver(OBInputReceiver, filter);
    }

    private void mapClick(GeoPoint geoPoint) {
        SurveyPoint sp = MapHelper.convertGeoPoint2SurveyPoint(geoPoint);
        sp.setHAE(ATSKApplication.getElevation_m_hae(sp.lat, sp.lon));
        Intent i = new Intent(ATSKConstants.MAP_CLICK_ACTION);
        i.putExtra(ATSKConstants.SURVEY_POINT_EXTRA, (Parcelable) sp);
        i.putExtra(ATSKConstants.LAT_EXTRA, sp.lat);
        i.putExtra(ATSKConstants.LON_EXTRA, sp.lon);
        i.putExtra(ATSKConstants.ALT_EXTRA, sp.getHAE());
        i.putExtra(ATSKConstants.CE_EXTRA, sp.circularError);
        i.putExtra(ATSKConstants.LE_EXTRA, sp.linearError);
        AtakBroadcast.getInstance().sendBroadcast(i);
    }

    @Override
    public void onClick(View view) {
        if (view == _mapButton) {
            MapButtonClicked();
        } else if (view == _lrfButton) {
            LRFButtonClicked();
        } else if (view == _gpsButton) {
            GPSButtonClicked();
        } else if (view == _heightButton) {
            heightButtonClicked();
        }
    }

    @Override
    public boolean onLongClick(View view) {
        if (view == _gpsButton) {
            showGPSMenu(_gpsButton);
            return true;
        } else if (view == _lrfButton) {
            showLRFMenu(_lrfButton);
            return true;
        }
        return false;
    }

    private void refresh() {
        if (!_manager.isActive(this))
            _manager.setToolbar(this);
        setInputBlank();

        if (TopCollection)
            _heightButton
                    .setImageResource(R.drawable.atsk_toolbar_height_top);
        else
            _heightButton
                    .setImageResource(R.drawable.atsk_toolbar_height_bottom);

        if (isLRF()) {
            _lrfButton.setImageResource(R.drawable.atsk_toolbar_lrf_selected);
        } else if (isMapClick()) {
            _mapButton.setImageResource(R.drawable.atsk_toolbar_map_selected);
        } else if (isPullBC()) {
            if (BreadcrumbsCollecting)
                _gpsButton
                        .setImageResource(R.drawable.atsk_toolbar_breadcrumb_selected);
            else
                _gpsButton
                        .setImageResource(R.drawable.atsk_toolbar_breadcrumb);
        } else if (isPull2PPlusDGPS()) {
            if (Pull2PPlusDFirstPointCollected) {
                _gpsButton.setImageResource(R.drawable.atsk_toolbar_twop_two);
            } else {
                _gpsButton.setImageResource(R.drawable.atsk_toolbar_twop_one);
            }
        } else if (isPull2PPlusDGPS1()) {
            _gpsButton.setImageResource(R.drawable.atsk_toolbar_twop_one);
        } else if (isPull2PPlusDGPS2()) {
            _gpsButton.setImageResource(R.drawable.atsk_toolbar_twop_two);
        } else if (isPull2PPlusDLRF()) {
            if (Pull2PPlusDFirstPointCollected) {
                _lrfButton.setImageResource(R.drawable.atsk_toolbar_twop_two);
            } else {
                _lrfButton.setImageResource(R.drawable.atsk_toolbar_twop_one);
            }
        } else if (isPull2PPlusDLRF1()) {
            _lrfButton.setImageResource(R.drawable.atsk_toolbar_twop_one);
        } else if (isPull2PPlusDLRF2()) {
            _lrfButton.setImageResource(R.drawable.atsk_toolbar_twop_two);
        } else if (isGPSOffset()) {
            _gpsButton.setImageResource(R.drawable.atsk_toolbar_pd);
        } else if (isGPS()) {
            //_gpsButton.setImageResource(R.drawable.atsk_toolbar_gps_selected);
        }
    }

    public void setInputBlank() {
        _gpsButton.setImageResource(R.drawable.atsk_toolbar_gps);
        _lrfButton.setImageResource(R.drawable.atsk_toolbar_lrf);
        _mapButton.setImageResource(R.drawable.atsk_toolbar_map);
    }

    @Override
    public void onVisible(boolean v) {
        if (v && !_mapClickRegistered) {
            // Close radial menu (immediately)
            // Otherwise listeners get screwed up and the map stops being clickable
            closeRadial();
            // Register map click listener
            _mapView.getMapEventDispatcher().pushListeners();
            _mapView.getMapEventDispatcher().clearListeners(MapEvent.MAP_CLICK);
            _mapView.getMapEventDispatcher().addMapEventListener(
                    MapEvent.MAP_CLICK, mapClickListener);
            _mapClickRegistered = true;
        } else if (!v) {
            if (_mapClickRegistered) {
                // Unregister map click listener
                closeRadial();
                _mapView.getMapEventDispatcher().removeMapEventListener(
                        MapEvent.MAP_CLICK, mapClickListener);
                _mapView.getMapEventDispatcher().popListeners();
                _mapClickRegistered = false;
            }
            setCollectionState(ATSKIntentConstants.OB_STATE_REQUESTED_HIDDEN);
            setInputBlank();
            endActiveTool();
        }
    }

    private void closeRadial() {
        LayoutWidget rootWidget = (LayoutWidget) _mapView
                .getComponentExtra("rootLayoutWidget");
        for (int i = 0; i < rootWidget.getChildCount(); i++) {
            MapWidget w1 = rootWidget.getChildAt(i);
            if (w1 != null && w1 instanceof MenuLayoutWidget)
                ((MenuLayoutWidget) w1).clearMenu();
        }
    }

    public void dispose() {
        try {
            AtakBroadcast.getInstance().unregisterReceiver(OBInputReceiver);
        } catch (Exception e) {
            Log.d(TAG,
                    "OBInputReceiver not registered, probably the drop down was not loaded");
        }
    }

    private void setCollectionState(String state, boolean action) {
        CurrentObstructionState = state;
        ATSKApplication.setObstructionCollectionMethod(state, TAG, action);
    }

    private void setButtonOpened(boolean Opened, boolean isLine,
            boolean isArea,
            int CollectingHeight) {
        this.CollectingHeight = CollectingHeight;
        this.ButtonsOpened = Opened;
        if (ButtonsOpened) {
            //LOU should notify here...
            this.TopCollection = (CollectingHeight == 0);

            CollectingLine = isLine;
            CollectingArea = isArea;
            refresh();
        } else {
            //check menus.
            if (lrfMenuPopup != null && lrfMenuPopup.isShowing())
                lrfMenuPopup.dismiss();
            if (gpsMenuPopup != null && gpsMenuPopup.isShowing())
                gpsMenuPopup.dismiss();

            BreadcrumbsCollecting = false;

            ATSKToolbarComponent.getToolbar().closeToolbar(this);

            CurrentObstructionState = ATSKIntentConstants.OB_STATE_HIDDEN;
        }
    }

    private void setButtonOpened(boolean Opened) {
        setButtonOpened(Opened, false, false);
    }

    private void setButtonOpened(boolean Opened, boolean isLine, boolean isArea) {
        setButtonOpened(Opened, isLine, isArea, CollectingHeight);
    }

    private void setCollectionState(String state) {
        setCollectionState(state, false);
    }

    private void showLRFMenu(ImageView imageView) {
        lrfMenuPopup = new LRFPopupWindow();

        lrfMenuPopup.Initialize(_mapView.getContext(), CollectingArea);

        int xy[] = getMenuLocation(imageView);
        lrfMenuPopup.showAtLocation(_mapView, Gravity.TOP | Gravity.LEFT,
                xy[0], xy[1]);

    }

    private void showGPSMenu(ImageView imageView) {

        gpsMenuPopup = new GPSPopupWindow();

        gpsMenuPopup.Initialize(_mapView.getContext(), CollectingLine,
                CollectingArea, imageView.getHeight());

        int xy[] = getMenuLocation(imageView);

        gpsMenuPopup.showAtLocation(_mapView, Gravity.TOP | Gravity.LEFT,
                xy[0], xy[1]);

    }

    private void heightButtonClicked() {
        TopCollection = !TopCollection;
        refresh();
    }

    private void GPSButtonClicked() {
        //MIKE - bring in more state - line or area, etc
        setInputBlank();
        if (isGPS()) {
            setCollectionState(ATSKIntentConstants.OB_STATE_GPS,
                    true);
        } else if (isGPSOffset()) {
            setCollectionState(
                    ATSKIntentConstants.OB_STATE_OFFSET_GPS,
                    true);
        } else if (isPullBC()) {
            if (BreadcrumbsCollecting) {
                //send stop
                BreadcrumbsCollecting = false;
                //MIKE send stop, not this.
                //setCollectionState("none", true);
                setCollectionState(
                        ATSKIntentConstants.OB_STATE_BC_GPS_OFF,
                        true);
            } else {
                BreadcrumbsCollecting = true;
                setCollectionState(
                        ATSKIntentConstants.OB_STATE_BC_GPS_ON,
                        true);
            }
            refresh();
        } else if (isPull2PPlusDGPS()) {
            if (Pull2PPlusDFirstPointCollected) {
                //collect the second point!
                Pull2PPlusDFirstPointCollected = false;
                setCollectionState(
                        ATSKIntentConstants.OB_STATE_2PPLUSD_GPS_2,
                        true);
            } else {
                //collect the first point
                Pull2PPlusDFirstPointCollected = true;
                setCollectionState(
                        ATSKIntentConstants.OB_STATE_2PPLUSD_GPS_1,
                        true);

            }
            refresh();
        } else if (isPull2PPlusDGPS1()) {
            if (!Pull2PPlusDFirstPointCollected) {
                //collect the second point!
                Pull2PPlusDFirstPointCollected = true;
                setCollectionState(
                        ATSKIntentConstants.OB_STATE_2PPLUSD_GPS_1,
                        true);
                setCollectionState(
                        ATSKIntentConstants.OB_STATE_2PPLUSD_GPS_2,
                        false);
                refresh();
            }
        } else if (isPull2PPlusDGPS2()) {
            if (Pull2PPlusDFirstPointCollected) {
                //collect the second point!
                Pull2PPlusDFirstPointCollected = false;
                setCollectionState(
                        ATSKIntentConstants.OB_STATE_2PPLUSD_GPS_2,
                        true);
                setCollectionState(
                        ATSKIntentConstants.OB_STATE_2PPLUSD_GPS_1,
                        false);
                refresh();
            }
        } else
            setCollectionState(ATSKIntentConstants.OB_STATE_GPS,
                    true);
        //refresh();
    }

    private int[] getMenuLocation(ImageView imageView) {

        int x = imageView.getLeft() +
                imageView.getWidth() +
                imageView.getPaddingRight() +
                12;//this could be something different!

        int buttonLocation[] = new int[2];
        imageView.getLocationOnScreen(buttonLocation);

        //MIKE check for null values
        return new int[] {
                x, buttonLocation[1] - 1
        };
    }

    public String getState() {
        return CurrentObstructionState;
    }

    public boolean collectingTop() {
        return TopCollection;
    }

    private void LRFButtonClicked() {
        if (isLRF()) {
            setCollectionState("none");
        } else if (isPull2PPlusDLRF()) {

        } else if (isPull2PPlusDLRF1()) {
            /* if(!Pull2PPlusDFirstPointCollected)
             {
                 //collect the second point!
                 Pull2PPlusDFirstPointCollected = true;
                 setCollectionState(ATSKIntentConstants.OB_STATE_2PPLUSD_LRF_1, true);
                 setCollectionState(ATSKIntentConstants.OB_STATE_2PPLUSD_LRF_2, false);
                 refresh();
             }*/
        } else if (isPull2PPlusDLRF2()) {
            /*if(Pull2PPlusDFirstPointCollected)
            {
                //collect the second point!
                Pull2PPlusDFirstPointCollected = false;
                setCollectionState(ATSKIntentConstants.OB_STATE_2PPLUSD_LRF_2, true);
                setCollectionState(ATSKIntentConstants.OB_STATE_2PPLUSD_LRF_1, false);
                refresh();
            }*/
        } else
            setCollectionState(ATSKIntentConstants.OB_STATE_LRF);
        refresh();
    }

    private void MapButtonClicked() {
        if (isMapClick()) {
            setCollectionState("none");
        } else
            setCollectionState(ATSKIntentConstants.OB_STATE_MAP_CLICK);
        refresh();
    }

    private boolean isGPSOffset() {
        return CurrentObstructionState
                .equals(ATSKIntentConstants.OB_STATE_OFFSET_GPS);
    }

    public boolean isPullBC() {
        return CurrentObstructionState
                .equals(ATSKIntentConstants.OB_STATE_BC_GPS)
                | CurrentObstructionState
                        .equals(ATSKIntentConstants.OB_STATE_BC_GPS_OFF)
                | CurrentObstructionState
                        .equals(ATSKIntentConstants.OB_STATE_BC_GPS_ON);
    }

    public boolean isPull2PPlusDGPS() {
        return CurrentObstructionState
                .equals(ATSKIntentConstants.OB_STATE_2PPLUSD_GPS);
    }

    public boolean isPull2PPlusDGPS1() {
        return CurrentObstructionState
                .equals(ATSKIntentConstants.OB_STATE_2PPLUSD_GPS_1);
    }

    public boolean isPull2PPlusDGPS2() {
        return CurrentObstructionState
                .equals(ATSKIntentConstants.OB_STATE_2PPLUSD_GPS_2);
    }

    public boolean isPull2PPlusDLRF() {
        return CurrentObstructionState
                .equals(ATSKIntentConstants.OB_STATE_2PPLUSD_LRF);
    }

    public boolean isPull2PPlusDLRF1() {
        return CurrentObstructionState
                .equals(ATSKIntentConstants.OB_STATE_2PPLUSD_LRF_1);
    }

    public boolean isPull2PPlusDLRF2() {
        return CurrentObstructionState
                .equals(ATSKIntentConstants.OB_STATE_2PPLUSD_LRF_2);
    }

    public boolean isGPS() {
        return CurrentObstructionState
                .equals(ATSKIntentConstants.OB_STATE_GPS);
    }

    public boolean isLRF() {
        return CurrentObstructionState
                .equals(ATSKIntentConstants.OB_STATE_LRF);
    }

    public boolean isLRFExtended() {
        return CurrentObstructionState
                .equals(ATSKIntentConstants.OB_STATE_LRF_EXTENDED_MENU);
    }

    public boolean isMapClick() {
        return CurrentObstructionState
                .equals(ATSKIntentConstants.OB_STATE_MAP_CLICK);
    }

    public boolean isGPSExtended() {
        return CurrentObstructionState
                .equals(ATSKIntentConstants.OB_STATE_GPS_EXTENDED_MENU);
    }

    private boolean _mapClickRegistered = false;
    private final MapEventDispatcher.MapEventDispatchListener mapClickListener = new MapEventDispatcher.MapEventDispatchListener() {
        @Override
        public void onMapEvent(MapEvent event) {

            if (event.getType().equals(MapEvent.MAP_CLICK)) {
                // Tell touch controller we didn't handle item clicks so we get the map click instead.
                if (event.getPoint() != null) {
                    GeoPoint gp = _mapView.inverse(event.getPoint().x,
                            event.getPoint().y);
                    mapClick(gp);
                }
            }
        }
    };
}
