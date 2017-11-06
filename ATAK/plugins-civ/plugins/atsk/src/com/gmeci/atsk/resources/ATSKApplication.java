
package com.gmeci.atsk.resources;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentManager;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

import com.atakmap.android.elev.dt2.Dt2ElevationModel;
import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.conversion.EGM96;
import com.atakmap.coremap.maps.coords.Altitude;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.gmeci.atsk.ATSKFragmentManager;
import com.gmeci.atsk.ATSKMapComponent;
import com.gmeci.atsk.obstructions.ObstructionToolbar;
import com.gmeci.atsk.toolbar.ATSKBaseToolbar;
import com.gmeci.atsk.toolbar.ATSKToolbarComponent;
import com.gmeci.atskservice.resolvers.AZProviderClient;

import android.database.Cursor;
import com.gmeci.atskservice.resolvers.GradientProviderClient;
import com.gmeci.atskservice.resolvers.DBURIConstants;
import com.gmeci.core.ATSKConstants;
import com.gmeci.core.ATSKIntentConstants;
import com.gmeci.core.SurveyData;
import com.gmeci.core.SurveyPoint;

import android.content.Intent;

public class ATSKApplication implements OnTouchListener {

    private static final String TAG = "ATSKApplication";
    private static ATSKApplication singleton;

    final Handler handler = new Handler();
    FragmentManager fm;
    Runnable mLongPressed = null;

    private final Context _context;
    private final Context _plugin;
    private static SharedPreferences _prefs;
    private boolean shouldClick = false;

    static final double ROUGH_EXTRA = 1 / 7000d;
    static GradientProviderClient gpc;

    public ATSKApplication(Context context, Context pluginContext) {
        //request a service connection
        _plugin = pluginContext;
        _context = context;

        gpc = new GradientProviderClient(context);
        gpc.Start();

        singleton = this;
    }

    public static ATSKApplication getInstance() {
        return singleton;
    }

    public Context getPluginContext() {
        return _plugin;
    }

    public Context getATAKContext() {
        return _context;
    }

    public SurveyData getCurrentSurvey() {
        AZProviderClient azpc = new AZProviderClient(_context);
        if (azpc.Start()) {
            try {
                String uid = azpc.getSetting(ATSKConstants.CURRENT_SURVEY, TAG);
                return azpc.getAZ(uid, false);
            } finally {
                azpc.Stop();
            }
        }
        return null;
    }

    public void dispose() {
    }

    public void putSupportFragmentManager(FragmentManager supportFragmentManager) {
        fm = supportFragmentManager;
    }

    public FragmentManager getSupportFragmentManager() {
        ATSKFragmentManager afm = ATSKMapComponent.getATSKFM();
        if (fm == null && afm != null)
            fm = afm.getSupportManager();
        return fm;
    }

    public boolean onTouch(final View view, MotionEvent motionEvent) {

        switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                shouldClick = true;
                handler.postDelayed(mLongPressed = new Runnable() {
                    public void run() {
                        shouldClick = false;
                        view.performLongClick();
                    }
                }, android.view.ViewConfiguration.getLongPressTimeout());

                break;
            case MotionEvent.ACTION_UP:
                if (mLongPressed != null)
                    handler.removeCallbacks(mLongPressed);

                if (shouldClick) {
                    view.performClick();
                }
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                break;
            case MotionEvent.ACTION_POINTER_UP:
                break;
            case MotionEvent.ACTION_MOVE:
                break;
        }
        return true;
    }

    static public double getElevation_m_hae(double Lat, double Lon) {
        double dted = 0;

        Cursor cursor = gpc.GetGradientPointsBounded(Lat + ROUGH_EXTRA, Lat
                - ROUGH_EXTRA, Lon - ROUGH_EXTRA, Lon + ROUGH_EXTRA, null);
        if (cursor == null || cursor.getCount() < 1) {
            if (cursor != null)
                cursor.close();

            return getAltitudeHAE(Lat, Lon);

        }

        //let's use collected gradients instead.
        int Index = cursor.getColumnIndex(DBURIConstants.COLUMN_HAE_M);

        cursor.moveToNext();
        double Elevation_m_hae = cursor.getDouble(Index);

        cursor.close();

        return Elevation_m_hae;

    }

    public static double getAltitudeHAE(final GeoPoint gp) {
        return getAltitudeHAE(gp.getLatitude(), gp.getLongitude());
    }

    public static double getAltitudeHAE(SurveyPoint sp) {
        return getAltitudeHAE(sp.lat, sp.lon);
    }

    public static double getAltitudeHAE(double lat, double lon) {
        Dt2ElevationModel dem = Dt2ElevationModel.getInstance();
        try {
            Altitude alt = dem.queryPoint(lat, lon);
            Altitude altHAE = EGM96.getInstance().getHAE(lat, lon, alt);
            if (altHAE.isValid())
                return altHAE.getValue();
        } catch (Exception e) {
        }
        return SurveyPoint.Altitude.INVALID;
    }

    private static ObstructionToolbar getObsToolbar() {
        ATSKBaseToolbar toolbar = ATSKToolbarComponent
                .getToolbar().getActive();
        if (toolbar instanceof ObstructionToolbar)
            return (ObstructionToolbar) toolbar;
        return null;
    }

    public static String getCollectionState() {
        ObstructionToolbar toolbar = getObsToolbar();
        return toolbar != null ? toolbar.getState()
                : ATSKIntentConstants.OB_STATE_HIDDEN;
    }

    public static boolean collectingTop() {
        ObstructionToolbar toolbar = getObsToolbar();
        return toolbar != null && toolbar.collectingTop();
    }

    static public boolean setObstructionCollectionMethod(
            String newState, String ChangeSource,
            boolean ActionRequested, String toolId, Bundle toolExtras) {
        String currentState = getCollectionState();
        boolean same = newState.equals(currentState);

        if (_prefs == null)
            _prefs = PreferenceManager.getDefaultSharedPreferences(
                    MapView.getMapView().getContext());

        Log.d(TAG, "setObstructionCollectionMethod(" + newState + ")");

        // Save last collection method (map click or LRF)
        if (newState.equals(ATSKIntentConstants.OB_STATE_MAP_CLICK)
                || newState.equals(ATSKIntentConstants.OB_STATE_LRF)) {
            _prefs.edit().putString(ATSKConstants.LAST_COLLECTION_METHOD_PREF,
                    newState).apply();
            Log.d(TAG, "Saving collection state preference: " + newState);
        } else if (newState.contains("OBStateRequested")
                && !newState
                        .equals(ATSKIntentConstants.OB_STATE_REQUESTED_HIDDEN)) {
            String lastMethod = _prefs.getString(
                    ATSKConstants.LAST_COLLECTION_METHOD_PREF, null);
            if (lastMethod != null) {
                Log.d(TAG, "Reloading collection state preference: "
                        + lastMethod);
                newState = lastMethod;
            }
        }

        // send an intent to tell everyone it changed?
        Intent obChangeIntent = new Intent(
                ATSKIntentConstants.OB_STATE_ACTION);
        obChangeIntent.putExtra(
                ATSKIntentConstants.OB_STATE_ACTION,
                newState);
        obChangeIntent.putExtra(
                ATSKIntentConstants.OB_STATE_SOURCE,
                ChangeSource);
        obChangeIntent.putExtra(
                ATSKIntentConstants.OB_COLLECT_TOP,
                collectingTop());
        obChangeIntent.putExtra(
                ATSKIntentConstants.OB_ACTION,
                ActionRequested);
        if (toolId != null) {
            obChangeIntent.putExtra("tool", toolId);
            if (toolExtras != null)
                obChangeIntent.putExtra("toolExtras", toolExtras);
        }

        com.atakmap.android.ipc.AtakBroadcast.getInstance().sendBroadcast(
                obChangeIntent);
        return same;
    }

    static public boolean setObstructionCollectionMethod(
            String CollectionState, String ChangeSource,
            boolean ActionRequested, String toolId) {
        return setObstructionCollectionMethod(CollectionState,
                ChangeSource, ActionRequested, toolId, null);
    }

    static public boolean setObstructionCollectionMethod(
            String CollectionState, String ChangeSource,
            boolean ActionRequested) {
        return setObstructionCollectionMethod(CollectionState,
                ChangeSource, ActionRequested, null);
    }

    static public boolean setObstructionCollectionTop(boolean newTop,
            boolean ignoreDefault, String ChangeSource) {
        boolean curTop = collectingTop();
        boolean same = newTop == curTop;
        Intent obChangeIntent = new Intent(
                ATSKIntentConstants.OB_STATE_ACTION);
        obChangeIntent.putExtra(
                ATSKIntentConstants.OB_STATE_SOURCE,
                ChangeSource);
        obChangeIntent.putExtra(
                ATSKIntentConstants.OB_COLLECT_TOP,
                newTop);
        obChangeIntent
                .putExtra(
                        ATSKIntentConstants.OB_COLLECT_TOP_IGNORE_DEFAULT,
                        ignoreDefault);
        com.atakmap.android.ipc.AtakBroadcast.getInstance().sendBroadcast(
                obChangeIntent);
        return same;
    }

    public static ShapeDrawable getColorRect(int color) {
        RectShape rect = new RectShape();
        rect.resize(50, 50);
        ShapeDrawable sd = new ShapeDrawable();
        sd.setBounds(0, 0, 50, 50);
        sd.setIntrinsicHeight(50);
        sd.setIntrinsicWidth(50);
        sd.setShape(rect);
        sd.getPaint().setColor(color);
        return sd;
    }

}
