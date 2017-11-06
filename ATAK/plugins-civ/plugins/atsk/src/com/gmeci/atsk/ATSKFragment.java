
package com.gmeci.atsk;

import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.atakmap.android.atsk.plugin.R;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.log.Log;
import com.gmeci.atsk.resources.ATSKApplication;
import com.gmeci.core.ATSKConstants;
import com.gmeci.core.ATSKIntentConstants;
import com.gmeci.atsk.az.AZController;
import com.gmeci.atsk.map.ATSKMapManager;
import com.gmeci.atsk.obstructions.ObstructionController;
import com.gmeci.atsk.resources.ServiceConnectionManager;
import com.gmeci.atsk.resources.ServiceConnectionManagerInterface;
import com.gmeci.atskservice.resolvers.AZProviderClient;
import com.gmeci.atskservice.resolvers.AZURIConstants;
import com.gmeci.hardwareinterfaces.HardwareConsumerInterface;

import java.lang.reflect.Field;

public class ATSKFragment extends Fragment implements
        ServiceConnectionManagerInterface {

    private static final String TAG = "ATSKFragment";

    // Fragment tags
    public static final String OBS = "FRAGMENT_OBS";
    public static final String GRAD = "FRAGMENT_GRAD";
    public static final String CRITERIA = "FRAGMENT_CRITERIA";
    public static final String REMARKS = "FRAGMENT_REMARKS";
    public static final String EXPORT = "FRAGMENT_EXPORT";
    public static final String CBR = "FRAGMENT_CBR";
    public static final String VEHICLE = "FRAGMENT_VEHICLE";
    public static final String IMG = "FRAGMENT_IMG";
    public static final String VIZ = "FRAGMENT_VIZ";

    public static boolean isOpen = false;
    //grease
    static AZProviderClient azpc;
    static AZContentObserver azco;
    static String currentScreen = "";
    public HardwareConsumerInterface hardwareConsumerInterface;
    View azspot = null;
    View currentNameSpot = null;
    View navSpot = null;
    AZController azController;
    ObstructionController obstructionController;
    //Services and related
    ServiceConnectionManager scm;
    ATSKFragmentManager atskFragmentManager = null;
    private static FragmentManager _fragManager;

    ATSKMapManager atskMapManager;
    //Standard fragment start (view and inflater)
    private View _root;
    private MapView _mapView;
    private Context _plugin;

    public static boolean isMapState() {
        return ATSKApplication
                .getCollectionState()
                .equals(ATSKIntentConstants.OB_STATE_MAP_CLICK);
    }

    public static boolean isMapType(String type) {
        return type.isEmpty()
                || currentScreen.equals(type)
                || (currentScreen
                        .equals(ATSKConstants.CURRENT_SCREEN_OBSTRUCTION)
                        || currentScreen
                                .equals(ATSKConstants.CURRENT_SCREEN_VEHICLE)
                        || currentScreen
                            .equals(ATSKConstants.CURRENT_SCREEN_GRADIENT))
                && (type.equals(ATSKConstants.CURRENT_SCREEN_OBSTRUCTION)
                        || type.equals(ATSKConstants.CURRENT_SCREEN_VEHICLE)
                        || type.equals(ATSKConstants.CURRENT_SCREEN_GRADIENT));
    }

    @Override
    public void onResume() {
        super.onResume();

        atskMapManager.Start(_mapView);
        azpc = new AZProviderClient(_mapView.getContext());
        azpc.Start();
        currentScreen = azpc.getSetting(ATSKConstants.CURRENT_SCREEN, TAG);

        Handler coHandler = new Handler();
        azco = new AZContentObserver(coHandler);
        _mapView.getContext().getContentResolver()
                .registerContentObserver(
                        Uri.parse(AZURIConstants.AZ_SETTING_URI), true, azco);

        String prefix = "fine";
        _mapView.getMapData().putString("locationSourcePrefix", prefix);

        SendSerialIntent();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        _plugin = ATSKApplication.getInstance().getPluginContext();

        LayoutInflater pluginInflater = LayoutInflater.from(_plugin);

        _root = pluginInflater.inflate(R.layout.atsk_test_start_fragholder,
                container, false);

        // Main screen elements
        azspot = _root.findViewById(R.id.atsk_main_frame);
        currentNameSpot = _root.findViewById(R.id.atsk_currentsurvey_frame);
        navSpot = _root.findViewById(R.id.atsk_navigation_frame);

        return _root;
    }

    @Override
    public void onDestroyView() {
        atskFragmentManager.Stop();
        super.onDestroyView();
        scm.stopServiceManagement();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Log.d(TAG, "onViewCreated called for atsk");

        _fragManager = getChildFragmentManager();

        atskMapManager = new ATSKMapManager();

        SetupServices();

        obstructionController = ObstructionController.getInstance();
        obstructionController.setFragmentManager(getChildFragmentManager());

        azController = AZController.getInstance();
        azController.setFragmentManager(getChildFragmentManager());

        atskFragmentManager = new ATSKFragmentManager();

        try {
            atskFragmentManager.Start(_plugin, _mapView,
                    getChildFragmentManager(), azspot, currentNameSpot,
                    navSpot, isOpen);
        } catch (NullPointerException npe) {
            Log.d(TAG, "exception occurred: ", npe);
            _mapView.post(new Runnable() {
                public void run() {
                    Toast.makeText(
                            _mapView.getContext(),
                            "Error occurred initializing ATSK, check to see if the ATSKService is installed.",
                            Toast.LENGTH_LONG).show();
                }

            });
            backButtonPressed();

        }

    }

    private void SetupServices() {
        scm = new ServiceConnectionManager();
        scm.startServiceManagement(getActivity(), this, "atak");
    }

    public boolean backButtonPressed() {
        return atskFragmentManager != null
                && atskFragmentManager.handleBackButton();
    }

    @Override
    public void onPause() {
        super.onPause();

        //obstructionController.onPause();
        //gradientController.onPause();

        atskMapManager.Stop();

        if (azpc != null)
            azpc.Stop();
        if (azco != null)
            _mapView.getContext().getContentResolver()
                    .unregisterContentObserver(azco);

    }

    @Override
    public void onDetach() {
        super.onDetach();
        try {
            Field childFragmentManager = Fragment.class
                    .getDeclaredField("mChildFragmentManager");
            childFragmentManager.setAccessible(true);
            childFragmentManager.set(this, null);

        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public void setMapView(MapView mapView) {
        _mapView = mapView;
    }

    public ATSKFragmentManager getATSKFM() {
        return atskFragmentManager;
    }

    public static FragmentManager getManager() {
        return _fragManager;
    }

    @Override
    public void GotHardwareHandle() {
        Log.d(TAG, "Obstruction Service Connected");
        hardwareConsumerInterface = scm.getHardwareInterface();
        if (hardwareConsumerInterface != null) {
            atskFragmentManager.setHardwareConsumerInterface(
                    hardwareConsumerInterface);
            atskMapManager.SetHardwareInterface(hardwareConsumerInterface);
        }
    }

    @Override
    public void GotATSKServiceHandle() {
        Log.d(TAG, "ATSK Service Connected");
        atskFragmentManager.SetSurveyInterface();
        atskMapManager.SetSurveyInterface();

    }

    public void SendSerialIntent() {
        final Intent i = new Intent();
        i.setAction(ATSKConstants.SERIAL_CONNECTION_ACTION);
        i.putExtra(ATSKConstants.SERIAL_ATTACHED, ATSKConstants.SERIAL_ATTACHED);
        AtakBroadcast.getInstance().sendBroadcast(i);
    }

    private static class AZContentObserver extends ContentObserver {
        public AZContentObserver(Handler handler) {
            super(handler);
        }

        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            final String UID = GetUIDFromURI(uri);
            if (UID.equals(ATSKConstants.CURRENT_SCREEN)) {
                if (azpc.isStarted())
                    currentScreen = azpc.getSetting(UID, TAG);
                else
                    currentScreen = "";
            }
        }

        private String GetUIDFromURI(Uri uri) {
            final String uriString = uri.toString();
            final String[] split = uriString.split("/");
            return split[split.length - 1];
        }
    }

}
