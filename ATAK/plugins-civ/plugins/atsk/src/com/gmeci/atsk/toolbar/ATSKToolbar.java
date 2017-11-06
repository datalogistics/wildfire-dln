
package com.gmeci.atsk.toolbar;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import com.atakmap.android.ipc.AtakBroadcast.DocumentedIntentFilter;
import android.graphics.Point;
import android.os.Parcelable;
import android.os.RemoteException;
import android.support.v4.app.FragmentActivity;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;

import com.atakmap.android.maps.Marker;
import com.atakmap.app.ATAKActivity;
import com.gmeci.atsk.ATSKFragmentManager;
import com.gmeci.atsk.MapHelper;
import com.gmeci.conversions.Conversions;
import com.gmeci.core.ATSKConstants;
import com.atakmap.coremap.maps.coords.GeoPoint;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.atakmap.android.gridlines.GridLinesMapComponent;
import com.atakmap.android.imagecapture.CustomGrid;
import com.atakmap.android.ipc.AtakBroadcast;
import com.gmeci.atsk.az.currentsurvey.SurveySelectionDialog;

import com.gmeci.atsk.gradient.GradientController;
import com.gmeci.atsk.obstructions.ObstructionController;

import com.atakmap.android.atsk.plugin.R;
import com.atakmap.android.dropdown.DropDown.OnStateListener;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.toolbar.IToolbarExtension;
import com.atakmap.android.toolbar.Tool;
import com.atakmap.android.toolbar.ToolbarBroadcastReceiver;
import com.atakmap.coremap.log.Log;
import com.gmeci.atsk.ATSKATAKConstants;
import com.gmeci.atsk.ATSKFragment;
import com.gmeci.atsk.ATSKMapComponent;
import com.gmeci.atsk.obstructions.notification.NotificationController;
import com.gmeci.atsk.resources.ServiceConnectionManagerInterface;
import com.atakmap.android.tools.ActionBarView;

import com.gmeci.atsk.az.AZController;
import com.gmeci.atsk.obstructions.ObstructionToolbar;
import com.gmeci.core.SurveyPoint;
import com.gmeci.hardwareinterfaces.HardwareConsumerInterface;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ATSKToolbar extends DropDownReceiver implements
        ServiceConnectionManagerInterface, IToolbarExtension, OnStateListener,
        com.atakmap.android.lrf.RangeFinderAction {

    private static final String TAG = "ATSKToolbar";
    public static final String IDENTIFIER = "com.gmeci.atsk.ATSK_TOOLBAR";

    private final SharedPreferences _prefs;

    public static boolean isClosing = false;
    public static boolean droppedGrid = false;
    private int _width, _height;

    private ATSKBaseToolbar _toolbar;
    private ObstructionToolbar _obToolbar;

    private NotificationController notificationController;
    private final Context _pluginContext;
    private final MapView _mapView;
    private final Marker _selectionMarker;
    private boolean _itemSelected = false;

    AZController azController;
    ObstructionController obstructionController;
    GradientController gradientController;

    public ATSKToolbar(MapView mapView, Context pluginContext) {
        super(mapView);
        _mapView = mapView;
        _pluginContext = pluginContext;

        _prefs = PreferenceManager.getDefaultSharedPreferences(
                mapView.getContext());

        ToolbarBroadcastReceiver.getInstance().registerToolbarComponent(
                ATSKATAKConstants.TOOLBAR_ATSK_OPEN, this);
        ToolbarBroadcastReceiver.getInstance().registerToolbarComponent(
                IDENTIFIER, this);

        DocumentedIntentFilter filter = new DocumentedIntentFilter();
        filter.addAction("com.gmeci.atsk.OPEN_DROP_DOWN");//this comes from sidepanel manager
        filter.addAction("com.gmeci.atsk.CLOSE_DROP_DOWN");//this comes from sidepanel manager
        filter.addAction("com.gmeci.atsk.HIDE_DROP_DOWN");//this comes from sidepanel manager
        AtakBroadcast.getInstance().registerReceiver(this, filter);

        // Placeholder for DropDownReceiver.setSelected to focus on
        _selectionMarker = new Marker(UUID.randomUUID().toString());
        _selectionMarker.setPoint(GeoPoint.ZERO);
        _selectionMarker.setZOrder(Double.NEGATIVE_INFINITY);
        _selectionMarker.setClickable(false);
        _selectionMarker.setVisible(false);
    }

    public Context getPluginContext() {
        return _pluginContext;
    }

    // Initialize controllers and create default toolbar (obstruction)
    synchronized private void setup() {
        if (_toolbar == null) {
            // Default to obstruction toolbar
            _obToolbar = new ObstructionToolbar(this, _mapView);
            _obToolbar.setupView();
            azController = new AZController(_pluginContext, getMapView());
            obstructionController = new ObstructionController(getMapView());
            gradientController = new GradientController(getMapView());
        }
    }

    // Called when the entire plugin drop-down is shown
    protected void showDropDown() {
        com.atakmap.android.lrf.LocalRangeFinderInput.getInstance()
                .registerAction(this);
        setup();

        if (!isVisible()) {
            setAssociationKey("atskPreference");
            showDropDown(ATSKMapComponent.getFragment(), THREE_EIGHTHS_WIDTH,
                    FULL_HEIGHT, FULL_WIDTH, HALF_HEIGHT, true, false, this);

            azController.onResume();
            azController.DrawAllAZs();
            gradientController.onResume();
            obstructionController.onResume();
            obstructionController.DrawAllObstructions();

            Thread t = new Thread() {
                public void run() {
                    if (_prefs.getBoolean(ATSKConstants.LAUNCH_PREF, false)) {
                        while (ATSKMapComponent.getFragment()
                                .getFragmentManager() == null) {
                            try {
                                Thread.sleep(250);
                            } catch (Exception e) {
                            }
                        }
                        SurveySelectionDialog surveyDialog = new SurveySelectionDialog();
                        surveyDialog.SetUpdateInterface(ATSKMapComponent
                                .getATSKFM());
                        surveyDialog.show(ATSKMapComponent.getFragment()
                                .getFragmentManager(), TAG);
                    }
                }
            };
            t.start();

        }
    }

    synchronized private void setupNotificationWindow() {

        Log.d(TAG, "setupNotificationController");
        if (notificationController == null) {
            Log.d(TAG, "creating a new notificationController");
            notificationController = new NotificationController(_pluginContext);

        }
        notificationController.clearText();

        notificationController.setParent((FragmentActivity) _mapView
                .getContext());
        notificationController
                .setImageResource(R.drawable.smart_bubble_minimized);
        notificationController.setAdjustViewBounds(true);
        notificationController.setButtonOpened(false);

        View mapView = ((ATAKActivity) _mapView.getContext()).findViewById(
                com.atakmap.app.R.id.map_view);
        if (mapView == null)
            return;
        ViewParent container = mapView.getParent();
        if (container == null || !(container instanceof RelativeLayout))
            return;
        ((RelativeLayout) container).addView(notificationController);
        setNotificationControllerParams();
    }

    private void setNotificationControllerParams() {
        if (notificationController == null)
            return;
        ViewGroup.LayoutParams lp = notificationController.getLayoutParams();
        if (lp instanceof RelativeLayout.LayoutParams) {
            RelativeLayout.LayoutParams rlp = (RelativeLayout.LayoutParams) lp;
            rlp.width = LayoutParams.WRAP_CONTENT;
            rlp.height = LayoutParams.WRAP_CONTENT;
            rlp.leftMargin = 0;
            rlp.topMargin = _mapView.getDefaultActionBarHeight() * 2;
            notificationController.setLayoutParams(rlp);
            notificationController.setVisibility(View.VISIBLE);
        } else {
            ViewParent vp = notificationController.getParent();
            Log.e(TAG, "Invalid layout params on notification controller: "
                    + lp + " ( " + vp + ")");
        }
    }

    private void hideNotificationController() {
        if (notificationController != null) {
            ViewParent parent = notificationController.getParent();
            if (parent != null && parent instanceof ViewGroup)
                ((ViewGroup) parent).removeView(notificationController);
        }
    }

    /**
     * Change toolbar or refresh if no change
     *
     * @param toolbar Toolbar object (null allowed)
     * @param refresh True to (re)open the toolbar
     */
    public void setToolbar(ATSKBaseToolbar toolbar, boolean refresh) {
        String oldType = _toolbar != null ? _toolbar.getClass().getName()
                : "null";
        String newType = toolbar != null ? toolbar.getClass().getName()
                : "null";
        if (_toolbar != toolbar) {
            onToolbarVisible(false);
            _toolbar = toolbar;
            if (toolbar != null)
                _toolbar.setupView();
        }
        if (refresh && _toolbar != null && !oldType.equals(newType))
            refresh();
        ((Activity) _mapView.getContext()).invalidateOptionsMenu();
    }

    public void setToolbar(ATSKBaseToolbar toolbar) {
        setToolbar(toolbar, true);
    }

    /**
     * Check if given toolbar is active
     *
     * @param toolbar Toolbar object
     */
    public boolean isActive(ATSKBaseToolbar toolbar) {
        return _toolbar == toolbar;
    }

    public ATSKBaseToolbar getActive() {
        return _toolbar;
    }

    // Send open toolbar request to ATAK
    public void refresh() {

        Intent atskToolbarIntent = new Intent(
                "com.atakmap.android.maps.toolbar.SET_TOOLBAR");
        atskToolbarIntent.putExtra("toolbar", IDENTIFIER);
        AtakBroadcast.getInstance().sendBroadcast(atskToolbarIntent);
    }

    // Close main toolbar
    public void closeToolbar() {
        Intent closeToolbarIntent = new Intent(ATSKATAKConstants.UNSET_TOOLBAR);
        AtakBroadcast.getInstance().sendBroadcast(closeToolbarIntent);
        setToolbar(null);
    }

    // Close main toolbar only if the current toolbar is equal to argument
    public void closeToolbar(ATSKBaseToolbar toolbar) {
        if (toolbar == _toolbar)
            closeToolbar();
    }

    @Override
    public List<Tool> getTools() {
        return new ArrayList<Tool>();
    }

    @Override
    public ActionBarView getToolbarView() {
        return (_toolbar == null ? null : _toolbar.getView());
    }

    @Override
    public boolean hasToolbar() {
        return true;
    }

    @Override
    public void onToolbarVisible(final boolean v) {
        if (_toolbar != null) {
            ATSKBaseToolbar tb = _toolbar;
            if (!v)
                _toolbar = null;
            tb.onVisible(v);
            for (OnToolbarVisibleListener l : _vizListeners)
                l.onToolbarVisible(tb, v);
            if (!v && tb != _obToolbar)
                tb.dispose();
        }
    }

    @Override
    public void disposeImpl() {
        try {
            AtakBroadcast.getInstance().unregisterReceiver(this);
        } catch (Exception e) {
            Log.d(TAG,
                    "ATSKToolbar not registered, probably the drop down was not loaded");
        }

        hideNotificationController();
        if (notificationController != null)
            notificationController.dispose();
    }

    @Override
    public void GotHardwareHandle() {

    }

    @Override
    public void GotATSKServiceHandle() {
    }

    @Override
    public void onDropDownSelectionRemoved() {
        Log.d(TAG, "selection removed:");
    }

    @Override
    public void onDropDownVisible(boolean v) {
        Log.d(TAG, "is visible: " + v);
        if (v)
            showRemainingViews();
        else
            ATSKFragment.isOpen = true;
    }

    @Override
    public void onDropDownSizeChanged(double width, double height) {
        Log.d(TAG, "resizing width=" + width + " height=" + height);
        Display disp = ((Activity) _mapView.getContext())
                .getWindowManager().getDefaultDisplay();
        Point size = new Point();
        disp.getSize(size);
        _width = (int) Math.round(width * size.x);
        _height = (int) Math.round(height * size.y);
    }

    @Override
    public void onDropDownClose() {
        Log.d(TAG, "drop down closed.");

        com.atakmap.android.lrf.LocalRangeFinderInput.getInstance()
                .registerAction(null);

        azController.onPause();
        gradientController.onPause();
        obstructionController.onPause();

        AZController.getInstance().RemoveAllAZs();
        GradientController.getInstance().removeAllGradients();
        ObstructionController.getInstance().RemoveAllLineObstructions();
        ObstructionController.getInstance().RemoveAllPointObstructions();
        AZController.getInstance().RemoveAllAZs();
        isClosing = true;
        closeToolbar();

        if (_toolbar != null)
            _toolbar.dispose();
        _toolbar = null;
        if (_obToolbar != null)
            _obToolbar.dispose();
        _obToolbar = null;

        // Remove the grid if we dropped it from ATSK
        CustomGrid grid = GridLinesMapComponent.getCustomGrid();
        if (droppedGrid && grid.isValid())
            grid.clear();
        droppedGrid = false;

        hideNotificationController();
        ATSKFragment.isOpen = false;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        Log.d(TAG, "ATSK Toolbar just received: " + intent.getAction());
        isClosing = false;

        String action = intent.getAction();

        if (action.equals("com.gmeci.atsk.OPEN_DROP_DOWN"))
            showDropDown();
        else if (action.equals("com.gmeci.atsk.CLOSE_DROP_DOWN"))
            closeDropDown();
        else if (action.equals("com.gmeci.atsk.HIDE_DROP_DOWN"))
            hideDropDown();

    }

    /**
     * Show selection marker on an area
     * @param point Point to focus on
     */
    public void setSelected(GeoPoint point) {
        _selectionMarker.setPoint(point);
        if (!_itemSelected) {
            super.setSelected(_selectionMarker,
                    "asset:/icons/outline.png", false);
            _itemSelected = true;
        }
    }

    public void clearSelected() {
        if (_itemSelected) {
            super.setSelected(null, "");
            _itemSelected = false;
        }
    }

    private void showRemainingViews() {
        if (notificationController == null
                || notificationController.getParent() == null)
            setupNotificationWindow();
        notificationController.setVisibility(View.VISIBLE);
    }

    @Override
    protected boolean onBackButtonPressed() {
        Log.d(TAG,
                "back button pressed in the ATSK drop down, closing drop down");
        ATSKMapComponent.backButtonPressed();
        return true;

    }

    @Override
    public void onRangeFinderInfo(String SourceDevice,
            double Range, double Azimuth, double ElevationAngle) {

        SurveyPoint sp = null;
        HardwareConsumerInterface hci = null;
        ATSKFragmentManager fm = ATSKMapComponent.getATSKFM();
        if (fm != null)
            hci = fm.getHardwareConsumerInterface();
        if (hci != null) {
            try {
                sp = hci.getMostRecentPoint();
            } catch (RemoteException e) {
                Log.d(TAG,
                        "error occurred pulling the GPS from the hardware interface.",
                        e);
            }
        }

        if (sp == null) {
            GeoPoint gp = _mapView.getSelfMarker().getPoint();
            sp = MapHelper.convertGeoPoint2SurveyPoint(gp);
        }

        double[] pos = Conversions.AROffset(sp.lat, sp.lon, sp.getHAE(),
                Conversions.GetTrueAngle(Azimuth, sp.lat, sp.lon),
                Range, ElevationAngle);
        sp.lat = pos[0];
        sp.lon = pos[1];
        sp.setHAE(pos[2]);

        Intent i = new Intent(ATSKConstants.GMECI_HARDWARE_LRF_ACTION);
        i.putExtra(ATSKConstants.SURVEY_POINT_EXTRA, (Parcelable) sp);
        i.putExtra(ATSKConstants.LAT_EXTRA, sp.lat);
        i.putExtra(ATSKConstants.LON_EXTRA, sp.lon);
        i.putExtra(ATSKConstants.ALT_EXTRA, sp.getHAE());
        i.putExtra(ATSKConstants.DEVICE, SourceDevice);
        i.putExtra(ATSKConstants.RANGE_M, Range);
        i.putExtra(ATSKConstants.ELEVATION, ElevationAngle);
        i.putExtra(ATSKConstants.AZIMUTH_T, Azimuth);
        _mapView.getContext().sendBroadcast(i);
    }

    public int getWidth() {
        return _width;
    }

    public int getHeight() {
        return _height;
    }

    private final List<OnToolbarVisibleListener> _vizListeners = new ArrayList<OnToolbarVisibleListener>();

    public void addVisibilityListener(OnToolbarVisibleListener l) {
        if (!_vizListeners.contains(l))
            _vizListeners.add(l);
    }

    public void removeVisibilityListener(OnToolbarVisibleListener l) {
        _vizListeners.remove(l);
    }

    public interface OnToolbarVisibleListener {
        void onToolbarVisible(ATSKBaseToolbar tb, boolean v);
    }
}
