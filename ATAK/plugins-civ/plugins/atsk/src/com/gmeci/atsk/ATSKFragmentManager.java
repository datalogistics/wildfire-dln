
package com.gmeci.atsk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.atakmap.android.ipc.AtakBroadcast.DocumentedIntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.widget.Toast;

import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.toolbar.widgets.TextContainer;
import com.atakmap.coremap.log.Log;
import com.gmeci.atsk.az.AZTabHost;
import com.gmeci.atsk.az.currentsurvey.SurveyOptionsDialog;
import com.gmeci.atsk.gallery.ATSKGalleryFragment;
import com.gmeci.atsk.obstructions.ObstructionController;
import com.gmeci.atsk.resources.ATSKApplication;
import com.gmeci.atsk.toolbar.ATSKBaseToolbar;
import com.gmeci.atsk.toolbar.ATSKToolbarComponent;
import com.gmeci.atsk.vehicle.VehicleTabHost;
import com.gmeci.atsk.visibility.VizFragment;
import com.gmeci.atsk.visibility.VizPrefs;
import com.gmeci.atskservice.resolvers.AZURIConstants;
import com.gmeci.core.ATSKConstants;
import com.gmeci.core.ATSKIntentConstants;
import com.gmeci.core.LineObstruction;
import com.gmeci.core.PointObstruction;
import com.gmeci.core.SurveyData;
import com.gmeci.core.SurveyPoint;
import com.gmeci.core.SurveyData.AZ_TYPE;
import com.gmeci.atsk.az.currentsurvey.CurrentSurveyFragment;
import com.gmeci.atsk.az.currentsurvey.FragmentSelectionInterface;
import com.gmeci.atsk.az.currentsurvey.UpdateSurveyNameInterface;
import com.gmeci.atsk.az.dz.DZCriteriaTabHost;
import com.gmeci.atsk.az.farp.FARPTabHost;
import com.gmeci.atsk.az.hlz.HLZCriteriaTabHost;
import com.gmeci.atsk.az.lz.LZCriteriaTabHost;
import com.gmeci.atsk.export.ATSKExportFragment;
import com.gmeci.atsk.gradient.GradientTabHost;
import com.gmeci.atsk.obstructions.obstruction.ObstructionTabHost;
import com.gmeci.atsk.resources.ATSKBaseFragment;
import com.gmeci.atskservice.resolvers.AZProviderClient;
import com.gmeci.hardwareinterfaces.HardwareConsumerInterface;
import com.gmeci.helpers.AZHelper;
import com.gmeci.conversions.Conversions;

import com.gmeci.atsk.az.lz.LZParser;
import com.gmeci.core.Criteria;

import java.util.List;

public class ATSKFragmentManager implements UpdateSurveyNameInterface,
        FragmentSelectionInterface {

    final public static String TAG = "ATSKFragmentManager";

    private static final int POINT_CHECK_COUNT = 40;

    private MapView _mapView;
    private SharedPreferences _prefs;
    private Context _context = null;
    private Context _plugin = null;

    // Main window and navigaton bars
    private View _mainSpot = null;
    private View _surveySpot = null;
    private View _navSpot = null;

    // Navigation fragment information
    private FragmentManager _manager = null;
    private ATSKNavigationFragment _navFrag;
    private ATSKBaseFragment _currentFragment;
    private String _currentTag;

    // Survey information
    private String _surveyType = "";
    private String _surveyUID = "";
    private CurrentSurveyFragment _surveyFragment;

    // Hardware interface
    private HardwareConsumerInterface _hardwareInterface;

    private AZProviderClient _azpc;

    public final BroadcastReceiver AZClickRx = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle extras = intent.getExtras();
            String ClickedUID = extras.getString(ATSKConstants.UID_EXTRA);

            handleAZClicked(ClickedUID);
        }
    };

    public void Start(final Context pluginContext, final MapView mapView,
            FragmentManager manager,
            View atskLayout, View currentNameSpot, View navSpot, boolean isOpen) {

        _mapView = mapView;
        _context = mapView.getContext();
        _plugin = pluginContext;
        _manager = manager;
        _mainSpot = atskLayout;
        _surveySpot = currentNameSpot;
        _navSpot = navSpot;

        _prefs = PreferenceManager.getDefaultSharedPreferences(_context);

        if (atskLayout == null)
            Log.d(TAG, "error will occur", new Exception());

        _azpc = new AZProviderClient(_context);
        _azpc.Start();

        // Right-side navigation bar
        _navFrag = new ATSKNavigationFragment();
        _navFrag.setFragmentManager(this);
        _manager.beginTransaction().add(_navSpot.getId(),
                _navFrag, "survey").commitAllowingStateLoss();

        //move to the ATSKMainfragment
        loadLastFragment();
        setCurrentSurveyFragment();
        setupAZClickListener();

        DocumentedIntentFilter radialFilter = new DocumentedIntentFilter();
        radialFilter.addAction(ATSKIntentConstants.AZ_MENU_CLICK_ACTION);
        AtakBroadcast.getInstance().registerReceiver(_radialReceiver,
                radialFilter);
    }

    public void Stop() {
        Thread t = new Thread(new Runnable() {
            public void run() {
                Log.d(TAG, "ending the session, calculating the highest point");

                CalculateNonStandard(_azpc.getSetting(
                        ATSKConstants.CURRENT_SURVEY, TAG));

                CalculateHighestPoint(_azpc.getSetting(
                        ATSKConstants.CURRENT_SURVEY, TAG));
                Log.d(TAG, "ending the session, calculating the incursions");
                CalculateIncursions(_azpc.getSetting(
                        ATSKConstants.CURRENT_SURVEY, TAG));
                Log.d(TAG, "shutting down ATSKFragmentManager");
                _azpc.Stop();
            }
        });
        t.start();
        ATSKToolbarComponent.getToolbar().closeToolbar();
        _currentTag = null;

        try {
            AtakBroadcast.getInstance().unregisterReceiver(_radialReceiver);
            AtakBroadcast.getInstance().unregisterReceiver(AZClickRx);
        } catch (Exception e) {
            // protect against double unregister
        }
    }

    public void SetSurveyInterface() {
        setCurrentFragmentInterface();
    }

    void setHardwareConsumerInterface(HardwareConsumerInterface obs) {
        _hardwareInterface = obs;
        if (_currentFragment != null)
            _currentFragment.setHardwareInterface(_hardwareInterface);
    }

    public HardwareConsumerInterface getHardwareConsumerInterface() {
        return _hardwareInterface;
    }

    private void setCurrentFragmentInterface() {
        if (_currentTag != null) {
            Fragment currentFragment = _manager
                    .findFragmentByTag(_currentTag);

            if (currentFragment instanceof ATSKBaseFragment) {
                ((ATSKBaseFragment) currentFragment)
                        .SetSurveyInterface();
            }
        }
    }

    private void setCurrentSurveyFragment() {
        //setup currentSurveyFragment
        _surveyFragment = new CurrentSurveyFragment();
        _surveyFragment.setup(_context, this);
        _manager.beginTransaction()
                .add(_surveySpot.getId(), _surveyFragment,
                        "survey").commitAllowingStateLoss();
    }

    public boolean handleBackButton() {
        //MIKE - consider making this a boolean, as we want to tell the people if ATSK can be removed or not!!
        Fragment f = _manager.findFragmentById(_mainSpot.getId());
        if (f == null)
            return false;
        Log.d(TAG, "Fragment is " + f.isVisible());
        if (!f.isVisible())
            return false;

        // Back button when toolbar is open should just close toolbar
        ATSKBaseToolbar tb = ATSKToolbarComponent.getToolbar().getActive();
        if (tb != null) {
            if (!tb.onBackButtonPressed())
                ATSKToolbarComponent.getToolbar().closeToolbar();
        } else if (!_currentFragment.onBackButtonPressed())
            setHomeFragment();
        return true;
    }

    public void setHomeFragment() {
        fragmentSelected(ATSKFragment.CRITERIA, _surveyType);
        // Close obstruction toolbar
        ATSKApplication.setObstructionCollectionMethod(
                ATSKIntentConstants.OB_STATE_REQUESTED_HIDDEN, TAG, true);
    }

    private void loadLastFragment() {
        final SurveyData survey = _azpc.getAZ(
                _azpc.getSetting(ATSKConstants.CURRENT_SURVEY, TAG), true);
        if (survey == null)
            return;

        _surveyType = survey.getType().toString();
        _surveyUID = survey.uid;
        Log.d(TAG, "loading last survey: " + survey.getSurveyName());
        VizPrefs.applyToSurvey(survey);

        // Returning from ATAK image container drop-down
        String lastMenu = _prefs.getString(ATSKConstants.LAST_MENU_PREF, "");
        if (!lastMenu.isEmpty())
            fragmentSelected(lastMenu, _surveyType);
        else
            setHomeFragment();
    }

    private void showFragment(Fragment desired, String tag) {
        if (_currentTag == null)
            _currentTag = tag;
        if (tag.equals(_currentTag)) {
            _manager.beginTransaction()
                    .replace(_mainSpot.getId(), desired, tag)
                    .commitAllowingStateLoss();
            _prefs.edit().putString(ATSKConstants.LAST_MENU_PREF, tag).apply();
        }
    }

    @Override
    public void updateCurrentSurveyHandle(String newUID, String newName,
            String newType) {

        Thread t = new Thread(new Runnable() {
            public void run() {
                CalculateNonStandard(_surveyUID);
                Log.d(TAG, "changing survey, calculating the highest point: "
                        + _surveyUID);
                CalculateHighestPoint(_surveyUID);
                Log.d(TAG, "changing survey, calculating the incursions: "
                        + _surveyUID);
                CalculateIncursions(_surveyUID);
                if (_currentFragment instanceof AZTabHost) {
                    _mapView.post(new Runnable() {
                        @Override
                        public void run() {
                            ((AZTabHost) _currentFragment).postRecalc();
                        }
                    });
                }
            }
        });
        t.start();
        if (!_surveyType.equals(newType) || !_surveyUID.equals(newUID)) {
            Log.d(TAG, "loading new survey: " + newName);
            _surveyType = newType;
            _surveyUID = newUID;
            setHomeFragment();
        } else
            fragmentSelected(_currentTag, _surveyType);
    }

    public boolean isCurrentFragment(String fragment) {
        return _currentTag != null && _currentTag.equals(fragment);
    }

    public String getCurrentFragmentTag() {
        return _currentTag;
    }

    public ATSKBaseFragment getCurrentFragment() {
        return _currentFragment;
    }

    public void reloadActiveFragment() {
        fragmentSelected(_currentTag, _surveyType);
    }

    public void notifySurveyUpdate(String surveyUID) {
        if (_currentFragment instanceof AZTabHost)
            ((AZTabHost) _currentFragment).updateSurvey(surveyUID);
    }

    public FragmentManager getSupportManager() {
        return _manager;
    }

    public void fragmentSelected(String selectedFragment) {
        fragmentSelected(selectedFragment, _surveyType);
    }

    @Override
    public void fragmentSelected(String selectedFragment, String type) {

        if (selectedFragment == null)
            selectedFragment = ATSKFragment.CRITERIA;

        if (_navFrag != null)
            _navFrag.setAZType(type);

        // Gradient/parking menus only available for LZ
        if (type != null && !type.equals(AZ_TYPE.LZ.toString())
                && selectedFragment.equals(ATSKFragment.GRAD)) {
            setHomeFragment();
            return;
        }

        //        Log.d(TAG, "SET FRAGMENT: " + selectedFragment
        //                + " (" + type + ")", new Throwable());

        if (_currentTag != null && !isCurrentFragment(selectedFragment))
            ATSKToolbarComponent.getToolbar().closeToolbar();
        TextContainer.getInstance().closePrompt();

        if (_navFrag != null)
            _navFrag.highlightButton(selectedFragment);

        //dont want to save remarks as the current fragment, as its an activity
        if (!selectedFragment.equals(ATSKFragment.REMARKS))
            _currentTag = selectedFragment;

        if (selectedFragment.equals(ATSKFragment.GRAD)) {
            //show the gradient fragment.
            GradientTabHost gradientFragment = new GradientTabHost();
            setCurrentFragment(gradientFragment);
            gradientFragment.SetSurveyInterface();
            showFragment(gradientFragment, ATSKFragment.GRAD);
        } else if (selectedFragment.equals(ATSKFragment.OBS)) {
            ObstructionTabHost obsFragment = new ObstructionTabHost();
            setCurrentFragment(obsFragment);

            ATSKApplication.setObstructionCollectionMethod(
                    ATSKIntentConstants.OB_STATE_REQUESTED_HIDDEN,
                    TAG, true);
            obsFragment.SetSurveyInterface();
            showFragment(obsFragment, ATSKFragment.OBS);
        }

        if (type == null)
            return;

        _surveyType = type;

        if (selectedFragment.equals(ATSKFragment.CRITERIA))
            showCriteriaFragment(type);
        else if (selectedFragment.equals(ATSKFragment.REMARKS))
            showRemarksFragment();
        else if (selectedFragment.equals(ATSKFragment.EXPORT))
            showExportFragment();
        else if (selectedFragment.equals(ATSKFragment.VEHICLE))
            showVehicleFragment();
        else if (selectedFragment.equals(ATSKFragment.VIZ))
            showVisibilityFragment();
        else if (selectedFragment.equals(ATSKFragment.IMG))
            showImageFragment();
    }

    private void showExportFragment() {
        ATSKExportFragment exportFragment = new ATSKExportFragment();
        exportFragment.SetSurveyInterface();
        setCurrentFragment(exportFragment);
        showFragment(exportFragment, ATSKFragment.EXPORT);
    }

    private void showVehicleFragment() {
        VehicleTabHost vehicle = new VehicleTabHost();
        vehicle.SetSurveyInterface();
        setCurrentFragment(vehicle);
        showFragment(vehicle, ATSKFragment.VEHICLE);
    }

    private void showImageFragment() {
        ATSKGalleryFragment gallery = new ATSKGalleryFragment();
        gallery.setSurvey(_surveyFragment);
        gallery.setMapView(_mapView);
        gallery.setFragmentManager(this);
        setCurrentFragment(gallery);
        showFragment(gallery, ATSKFragment.IMG);
    }

    private void showVisibilityFragment() {
        VizFragment viz = new VizFragment(_mapView, _surveyFragment);
        setCurrentFragment(viz);
        showFragment(viz, ATSKFragment.VIZ);
    }

    private void setCurrentFragment(ATSKBaseFragment frag) {
        if (frag != null && _currentFragment != frag) {
            _currentFragment = frag;
            _currentFragment.setHardwareInterface(_hardwareInterface);
        }
    }

    private synchronized void CalculateIncursions(final String uid) {
        SurveyData surveyData = _azpc.getAZ(uid, false);
        if (surveyData == null)
            return;

        List<PointObstruction> points = ObstructionController
                .getInstance().getPointObstructions();
        List<LineObstruction> lines = ObstructionController
                .getInstance().getLineObstructions();

        List<PointObstruction> ApproachIncursionpoints = AZHelper
                .CalculateLZGlideSlopeIncursions(surveyData, points, lines,
                        false);
        List<PointObstruction> DepartureIncursionpoints = AZHelper
                .CalculateLZGlideSlopeIncursions(surveyData, points, lines,
                        true);

        if (ApproachIncursionpoints.size() > 0) {
            surveyData.worstApproachIncursionPoint = ApproachIncursionpoints
                    .get(0);
        } else {
            surveyData.worstApproachIncursionPoint = null;
            //set to the min values in config?
            surveyData.aDisplacedThreshold = 0;
            surveyData.approachGlideSlopeDeg = Conversions.RATIO_99_1;
        }
        if (DepartureIncursionpoints.size() > 0) {
            surveyData.worstDepartureIncursionPoint =
                    DepartureIncursionpoints.get(0);
        } else {
            surveyData.worstDepartureIncursionPoint = null;
            //set to the min values in config?
            surveyData.dDisplacedThreshold = 0;
            surveyData.departureGlideSlopeDeg = Conversions.RATIO_99_1;
        }

        _azpc.UpdateAZ(surveyData, "incursion-remark", false);
    }

    private synchronized void CalculateNonStandard(final String uid) {
        if (uid == null)
            return;

        SurveyData survey = _azpc.getAZ(uid, true);

        if (survey == null)
            return;

        String surveyType = survey.getType().toString();
        if (!surveyType.equals(SurveyData.AZ_TYPE.LZ.toString()))
            return;

        if (survey.aircraft != null) {
            Criteria c = LZParser.getInstance().GetAircraftByName(
                    survey.aircraft);
            if (c != null) {
                Log.d(TAG, "recording information for: " + survey.aircraft
                        + " " +
                        c.ApproachOverrunLength_m + " "
                        + c.DepartureOverrunLength_m);
                survey.setMetaDouble("stdApproachOverrun",
                        c.ApproachOverrunLength_m);
                survey.setMetaDouble("stdDepartureOverrun",
                        c.DepartureOverrunLength_m);
                survey.setMetaDouble("stdApproachGSR",
                        c.ApproachGlideSlope_deg);
                survey.setMetaDouble("stdDepartureGSR",
                        c.DepartureGlideSlope_deg);
                _azpc.UpdateAZ(survey, AZURIConstants.AZ_REMARKS_UPDATED, true);
            } else {
                Log.d(TAG, "could not find plane information for: "
                        + survey.aircraft);
            }
        }
    }

    private synchronized void CalculateHighestPoint(final String uid) {
        if (uid == null)
            return;

        SurveyData survey = _azpc.getAZ(uid, true);

        if (survey == null)
            return;

        survey.highestElevation = GetHighestElevation_m_hae(survey);

        _azpc.UpdateAZ(survey, "showRemarks", false);
    }

    static double GetHighestElevation_m_hae(SurveyData survey) {

        double length = survey.circularAZ ? survey.getRadius() * 2
                : survey.getLength(true);
        double width = survey.circularAZ ? length : survey.width;

        double CenterStart[] = Conversions.AROffset(survey.center.lat,
                survey.center.lon,
                survey.angle, length / 2);
        double CenterEnd[] = Conversions.AROffset(survey.center.lat,
                survey.center.lon,
                survey.angle + 180, length / 2);

        double HighestPoint_m_hae = -1000;

        double centerLine = GetHighestInAline_m(CenterStart[0],
                CenterStart[1], CenterEnd[0], CenterEnd[1], POINT_CHECK_COUNT);

        double LeftStart[] = Conversions.AROffset(CenterStart[0],
                CenterStart[1], survey.angle + 90, width / 2);
        double LeftEnd[] = Conversions.AROffset(LeftStart[0], LeftStart[1],
                survey.angle + 180, length);

        double RightStart[] = Conversions.AROffset(CenterStart[0],
                CenterStart[1], survey.angle - 90, width / 2);
        double RightEnd[] = Conversions.AROffset(RightStart[0], RightStart[1],
                survey.angle + 180, length);

        double HighestLeft = GetHighestInAline_m(LeftStart[0], LeftStart[1],
                LeftEnd[0], LeftEnd[1], POINT_CHECK_COUNT);
        double HighestRight = GetHighestInAline_m(RightStart[0], RightStart[1],
                RightEnd[0], RightEnd[1], POINT_CHECK_COUNT);

        if (centerLine != SurveyPoint.Altitude.INVALID
                && centerLine > HighestPoint_m_hae)
            HighestPoint_m_hae = centerLine;

        if (survey.approachElevation != SurveyPoint.Altitude.INVALID
                && survey.approachElevation > HighestPoint_m_hae)
            HighestPoint_m_hae = survey.approachElevation;

        if (survey.departureElevation != SurveyPoint.Altitude.INVALID
                && survey.departureElevation > HighestPoint_m_hae)
            HighestPoint_m_hae = survey.departureElevation;

        // probably need to check to see if the survey is actuall a DZ before checking these.
        if (survey.getType() == SurveyData.AZ_TYPE.DZ) {
            if (survey.perPIElevation != SurveyPoint.Altitude.INVALID
                    && survey.perPIElevation > HighestPoint_m_hae)
                HighestPoint_m_hae = survey.perPIElevation;
            if (survey.cdsPIElevation != SurveyPoint.Altitude.INVALID
                    && survey.cdsPIElevation > HighestPoint_m_hae)
                HighestPoint_m_hae = survey.cdsPIElevation;
            if (survey.hePIElevation != SurveyPoint.Altitude.INVALID
                    && survey.hePIElevation > HighestPoint_m_hae)
                HighestPoint_m_hae = survey.hePIElevation;
        }

        if (HighestLeft != SurveyPoint.Altitude.INVALID
                && HighestLeft > HighestPoint_m_hae)
            HighestPoint_m_hae = HighestLeft;
        if (HighestRight != SurveyPoint.Altitude.INVALID
                && HighestRight > HighestPoint_m_hae)
            HighestPoint_m_hae = HighestRight;

        return HighestPoint_m_hae;
    }

    static private double GetHighestInAline_m(double StartLat, double StartLon,
            double EndLat, double EndLon, int Points2Check) {
        double DeltaLat = (StartLat - EndLat) / Points2Check;
        double DeltaLon = (StartLon - EndLon) / Points2Check;
        double HighestPoint_m = -1000;
        for (int i = 0; i < Points2Check; i++) {
            double Height_m = ATSKApplication.getElevation_m_hae(StartLat
                    + (DeltaLat * i), StartLon + (DeltaLon * i));

            if (Height_m != SurveyPoint.Altitude.INVALID
                    && Height_m > HighestPoint_m)
                HighestPoint_m = Height_m;

        }
        return HighestPoint_m;
    }

    private void showRemarksFragment() {

        final String uid = _azpc.getSetting(ATSKConstants.CURRENT_SURVEY, TAG);
        Log.d(TAG, "call to show survey uid: " + uid);

        CalculateNonStandard(uid);
        CalculateHighestPoint(uid);

        SurveyData currentSurvey = _azpc.getAZ(uid, true);
        if (currentSurvey == null)
            return;

        if (currentSurvey.aircraft != null
                && currentSurvey.aircraft.equals("NONE") &&
                currentSurvey.getType() == AZ_TYPE.LZ) {
            _surveyFragment.getActivity().runOnUiThread(
                    new Runnable() {
                        public void run() {
                            Toast.makeText(
                                    _context,
                                    "Select a valid LZ Aircraft Criteria",
                                    Toast.LENGTH_LONG).show();

                        }
                    });
            return;
        }

        _surveyType = currentSurvey.getType().toString();
        Log.d(TAG, "call to show survey type of: " + _surveyType);
        if (_surveyType != null) {
            if (_surveyType.equals(SurveyData.AZ_TYPE.DZ.toString())) {
                Intent intent = new Intent();
                intent.setClassName("com.gmeci.atskservice",
                        "com.gmeci.atskservice.form.DZForm");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                _context.startActivity(intent);
            } else if (_surveyType.equals(SurveyData.AZ_TYPE.LZ.toString())) {
                CalculateIncursions(_azpc.getSetting(
                        ATSKConstants.CURRENT_SURVEY, TAG));
                if (currentSurvey.surveyIsLTFW()) {
                    Intent intent = new Intent();
                    intent.setClassName("com.gmeci.atskservice",
                            "com.gmeci.atskservice.form.LTFWLZForm");
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    _context.startActivity(intent);
                } else if (currentSurvey.surveyIsSTOL()) {
                    Intent intent = new Intent();
                    intent.setClassName("com.gmeci.atskservice",
                            "com.gmeci.atskservice.form.STOLForm");
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    _context.startActivity(intent);
                } else {
                    Intent intent = new Intent();
                    intent.setClassName("com.gmeci.atskservice",
                            "com.gmeci.atskservice.form.LZForm");
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    try {
                        _context.startActivity(intent);
                    } catch (Exception e) {
                        if (_surveyFragment != null &&
                                _surveyFragment.getActivity() != null) {

                            _surveyFragment.getActivity().runOnUiThread(
                                    new Runnable() {
                                        public void run() {
                                            Toast.makeText(
                                                    _context,
                                                    "Error occurred initializing ATSK Remarks Screen, check to see if the ATSKPreference is installed.",
                                                    Toast.LENGTH_LONG).show();

                                        }
                                    });
                        }
                        Log.d(TAG,
                                "Error occurred initializing ATSK Remarks Screen, check to see if the ATSKPreference is installed.",
                                e);
                    }
                }
            } else if (_surveyType.equals(SurveyData.AZ_TYPE.HLZ.toString())) {
                Intent intent = new Intent();
                intent.setClassName("com.gmeci.atskservice",
                        "com.gmeci.atskservice.form.HLZForm");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                _context.startActivity(intent);
            } else if (_surveyType.equals(SurveyData.AZ_TYPE.LTFW.toString())) {
                Intent intent = new Intent();
                intent.setClassName("com.gmeci.atskservice",
                        "com.gmeci.atskservice.form.LTFWForm");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                _context.startActivity(intent);
            } else if (_surveyType.equals(SurveyData.AZ_TYPE.STOL.toString())) {
                Intent intent = new Intent();
                intent.setClassName("com.gmeci.atskservice",
                        "com.gmeci.atskservice.form.STOLForm");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                _context.startActivity(intent);
            } else if (_surveyType.equals(SurveyData.AZ_TYPE.FARP.toString())) {
                Intent intent = new Intent();
                intent.setClassName("com.gmeci.atskservice",
                        "com.gmeci.atskservice.form.FARPForm");
                _context.startActivity(intent);
            }
        }

    }

    private void showCriteriaFragment(String type) {
        if (type.equals(AZ_TYPE.DZ.toString())) {
            DZCriteriaTabHost dzCriteriaFragment = new DZCriteriaTabHost();
            dzCriteriaFragment.SetSurveyInterface();
            dzCriteriaFragment.setHardwareInterface(_hardwareInterface);
            setCurrentFragment(dzCriteriaFragment);
            showFragment(dzCriteriaFragment, ATSKFragment.CRITERIA);
        } else if (type.equals(AZ_TYPE.HLZ.toString())) {
            HLZCriteriaTabHost hlzCriteriaFragment = new HLZCriteriaTabHost();
            hlzCriteriaFragment.SetSurveyInterface();
            hlzCriteriaFragment.setHardwareInterface(_hardwareInterface);
            setCurrentFragment(hlzCriteriaFragment);
            showFragment(hlzCriteriaFragment, ATSKFragment.CRITERIA);
        } else if (type.equals(AZ_TYPE.LZ.toString())) {
            LZCriteriaTabHost lzCriteriaFragment = new LZCriteriaTabHost();
            lzCriteriaFragment.SetSurveyInterface();
            lzCriteriaFragment.setHardwareInterface(_hardwareInterface);
            setCurrentFragment(lzCriteriaFragment);
            showFragment(lzCriteriaFragment, ATSKFragment.CRITERIA);
        } else if (type.equals(AZ_TYPE.FARP.toString())) {
            FARPTabHost farpCriteriaFragment = new FARPTabHost();
            farpCriteriaFragment.SetSurveyInterface();
            farpCriteriaFragment.setHardwareInterface(_hardwareInterface);
            setCurrentFragment(farpCriteriaFragment);
            showFragment(farpCriteriaFragment, ATSKFragment.CRITERIA);
        }
    }

    private void setupAZClickListener() {

        DocumentedIntentFilter AZClickFilter = new DocumentedIntentFilter();
        AZClickFilter.addAction(ATSKConstants.AZ_CLICK_ACTION);
        AZClickFilter.addAction(ATSKConstants.CURRENT_SURVEY_CHANGE_ACTION);
        AtakBroadcast.getInstance().registerReceiver(AZClickRx, AZClickFilter);
    }

    private void handleAZClicked(String uid) {
        SurveyData survey = _azpc.getAZ(uid, false);
        if (survey != null) {
            _surveyType = survey.getType().toString();
            _surveyFragment.updateCurrentSurveyHandle(uid,
                    _azpc.getAZName(uid), survey.getType().toString());
            setHomeFragment();
        }
    }

    // AZ radial menu item clicked
    private final BroadcastReceiver _radialReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            Bundle extras = intent.getExtras();

            if (extras == null || _surveyFragment == null)
                return;

            String req = extras.getString(
                    ATSKIntentConstants.MENU_REQUEST, "none");
            String uid = extras.getString(ATSKIntentConstants.AZ_MENU_UID);
            Log.d(TAG, "Radial item request: " + req);

            FragmentActivity act = _surveyFragment.getActivity();
            FragmentManager fragManager = act.getSupportFragmentManager();
            if (req.equals(ATSKIntentConstants.OB_MENU_EXTRA)) {
                // Editable survey details
                if (!SurveyOptionsDialog.showAZDetails(_azpc, uid, fragManager))
                    Toast.makeText(context, "Invalid Survey Selected",
                            Toast.LENGTH_SHORT).show();

                // Generic survey details (unfinished)
                /*AZDetailsDialog dialog = new AZDetailsDialog();
                dialog.init(_azpc, uid);
                dialog.show(fragManager, "surveyDetailsDialog");*/
            } else if (req.equals(ATSKIntentConstants.MENU_EDIT)) {
                // Edit survey
                _azpc.putSetting(ATSKConstants.CURRENT_SURVEY, uid,
                        "SurveyOptionsDlg");

                /*SurveyOptionsDialog options = new SurveyOptionsDialog(
                        act, fragManager, uid, _azpc);

                String surveyName = _azpc.getAZName(uid);
                if (surveyName == null || surveyName.length() < 1)
                    surveyName = "(blank)";

                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(
                        act);
                alertBuilder
                        .setView(options.getView())
                        .setTitle(surveyName)
                        .setCancelable(false)
                        .setNegativeButton("Back", null)
                        .setOnCancelListener(null);

                AlertDialog ad = alertBuilder.create();
                options.setAlertDialog(ad);
                ad.show();*/
            }
        }
    };
}
