
package com.gmeci.atsk.obstructions.obstruction;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.atakmap.android.ipc.AtakBroadcast.DocumentedIntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTabHost;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabSpec;
import android.widget.Toast;

import com.atakmap.android.atsk.plugin.R;
import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.coremap.log.Log;
import com.gmeci.atsk.resources.ATSKApplication;
import com.gmeci.core.ATSKConstants;
import com.gmeci.core.ATSKIntentConstants;
import com.gmeci.core.LineObstruction;
import com.gmeci.core.PointObstruction;
import com.gmeci.core.SurveyPoint;
import com.gmeci.atsk.obstructions.obstruction.GPSAngle_OffsetDialog.GPSOffsetInterface;
import com.gmeci.atsk.obstructions.obstruction.TwoPPlusDSide_OffsetDialog.TwoPPlusDInterface;
import com.gmeci.atsk.resources.ATSKBaseFragment;
import com.gmeci.constants.Constants;
import com.gmeci.conversions.Conversions;

public class ObstructionTabHost extends ATSKBaseFragment implements
        GPSOffsetInterface, OnTabChangeListener, TwoPPlusDInterface {

    private static final String TWO_P_PLUS_D_P1_UID = "First Point 2PD";
    private static final String POINT_TAB = "POINT";
    private static final String EDIT_POINT_TAB = "EDIT POINT";
    private static final String EDIT_LINE_TAB = "EDIT LINE";
    private static final String EDIT_AREA_TAB = "EDIT AREA";
    private static final String ROUTE_TAB = "ROUTE";
    private static final String AREA_TAB = "AREA";
    private static final String SELECTED_TAB = "Selected Obstruction Tab";
    private static final String OBSTRUCTION_UID_COUNT = "OBSTRUCTION_UID_COUNT";
    private static final String TAG = "ObstructionTabHost";

    // Private access
    private LineObstruction _curLine;
    private Context _plugin;
    private int _copies = 0;
    private ObstructionTabBase _curTab;
    private String _selectedGroup, _selectedUID;

    //Tab host
    SurveyPoint TwoPPlusDPoint1, TwoPPlusDPoint2;
    TabSpec PointTabSpec, EditPointTabSpec, EditAreaTabSpec, EditLineTabSpec;
    boolean StoredTopCollected = false;
    final BroadcastReceiver RouteChangeRx = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ATSKConstants.STOP_COLLECTING_ROUTE_NOTIFICATION))
                StopRouteCollection();
        }

    };
    SurveyPoint OffsetPoint;

    private static double getFloat(SharedPreferences sp, String key,
            double dv) {
        try {
            return Float.parseFloat(sp.getString(key, dv + ""));
        } catch (NumberFormatException e) {
            return dv;
        }
    }

    private FragmentTabHost _tabHost;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        _plugin = ATSKApplication.getInstance().getPluginContext();

        View root = LayoutInflater.from(_plugin)
                .inflate(R.layout.obstruction_main, container, false);

        _tabHost = (FragmentTabHost) root.findViewById(android.R.id.tabhost);
        _tabHost.setup(_plugin, getChildFragmentManager(),
                R.id.realtabcontent);

        addNewObstructionFragmentsToTabHost();
        return root;
    }

    private void addNewObstructionFragmentsToTabHost() {
        _tabHost.clearAllTabs();
        PointTabSpec = _tabHost.newTabSpec(POINT_TAB).setIndicator(
                "",
                _plugin.getResources().getDrawable(
                        R.drawable.obstruction_point));
        _tabHost.addTab(PointTabSpec, ObstructionPointFragment.class, null);

        TabSpec RouteTabSpec = _tabHost.newTabSpec(ROUTE_TAB).setIndicator(
                "",
                _plugin.getResources().getDrawable(
                        R.drawable.obstruction_line));
        _tabHost.addTab(RouteTabSpec, ObstructionRouteFragment.class, null);

        _tabHost.addTab(
                _tabHost.newTabSpec(AREA_TAB)
                        .setIndicator("",
                                _plugin.getResources().getDrawable(
                                        R.drawable.obstruction_area)),
                ObstructionAreaFragment.class, null);
        for (int i = 0; i < _tabHost.getTabWidget().getChildCount(); i++) {
            _tabHost.getTabWidget().getChildAt(i).setPadding(0, 0, 0, 0);
        }
    }

    protected void SelectedPointPlusOffset(boolean TopCollected) {
        if (_curTab instanceof EditObstructionPointFragment
                || _curTab instanceof ObstructionPointFragment) {
            GPSSinglePoint(pullGPS(), true, TopCollected);
        } else
            GPSLinePoint(pullGPS(), true, TopCollected);
    }

    protected void SelectedPoint(boolean TopCollected) {
        if (_curTab instanceof EditObstructionPointFragment
                || _curTab instanceof ObstructionPointFragment) {
            GPSSinglePoint(pullGPS(), false, TopCollected);
        } else
            GPSLinePoint(pullGPS(), false, TopCollected);
    }

    @Override
    public void onPause() {
        putSetting(ATSKConstants.CURRENT_SCREEN, "", TAG);
        com.atakmap.android.ipc.AtakBroadcast.getInstance().unregisterReceiver(
                RouteChangeRx);
        ATSKApplication.setObstructionCollectionMethod(
                ATSKIntentConstants.OB_STATE_HIDDEN,
                "ObstructionMainFragment", false);

        RemovePointObstruction(ATSKConstants.DEFAULT_GROUP,
                ATSKConstants.TEMP_POINT_UID);
        //delete temp points...
        super.onPause();

        user_settings.edit().putInt(SELECTED_TAB,
                _tabHost.getCurrentTab()).apply();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onResume() {

        super.onResume();

        DocumentedIntentFilter filter = new DocumentedIntentFilter();
        filter.addAction(ATSKConstants.STOP_COLLECTING_ROUTE_NOTIFICATION);
        filter.addAction(ATSKConstants.STOP_COLLECTING_GRADIENT_NOTIFICATION);
        com.atakmap.android.ipc.AtakBroadcast.getInstance().registerReceiver(
                RouteChangeRx, filter);
        putSetting(ATSKConstants.CURRENT_SCREEN,
                ATSKConstants.CURRENT_SCREEN_OBSTRUCTION, TAG);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        int Tab2Open = user_settings.getInt(SELECTED_TAB, 0);
        _tabHost.setOnTabChangedListener(this);
        _tabHost.setCurrentTab(Tab2Open);
    }

    public String GetUID(String group, String type) {
        int pointCount = user_settings.getInt(OBSTRUCTION_UID_COUNT, 1);

        user_settings.edit().putInt(OBSTRUCTION_UID_COUNT,
                pointCount + 1).apply();

        return type + "." + group + "." + pointCount + 1;
    }

    @Override
    public void onTabCreated(Fragment tabFrag) {
        if (opc != null && _tabHost != null && tabFrag != null
                && tabFrag instanceof ObstructionTabBase) {
            initCurrentTab();
            opc.Start();
            runOnCreate();
        }
    }

    @Override
    public void onTabChanged(final String newTabTagName) {
        //initCurrentTab();
        Log.d(TAG, "Tab changed to " + newTabTagName);
    }

    private void initCurrentTab() {
        _curTab = getCurrentTab();
        if (_curTab == null)
            return;

        _curTab.setParentInterface(this);

        ATSKApplication.setObstructionCollectionMethod(
                ATSKIntentConstants.OB_STATE_HIDDEN,
                "ObstructionMainFragment", false);
        Log.e(TAG, "about to set parentInterface..");

        if (_selectedUID != null && _selectedGroup != null) {
            if (_curTab instanceof EditObstructionPointFragment) {
                ((EditObstructionPointFragment) _curTab).setBaseOPC(
                        opc, _selectedGroup, _selectedUID, _copies);
            } else if (_curTab instanceof EditObstructionRouteFragment) {
                ((EditObstructionRouteFragment) _curTab).setBaseOPC(
                        opc, _selectedGroup, _selectedUID);
            } else if (_curTab instanceof EditObstructionAreaFragment) {
                ((EditObstructionAreaFragment) _curTab).setBaseOPC(
                        opc, _selectedGroup, _selectedUID);
            }
            if (opc != null)
                _curLine = opc.GetLine(_selectedGroup, _selectedUID, true);
        }
        _selectedUID = _selectedGroup = null;
        _copies = 0;
    }

    public void GPSLinePoint(SurveyPoint sp, boolean withOffset,
            boolean TopCollected) {
        //LOU is this the same as GPSPoint?
        StoredTopCollected = TopCollected;
        if (withOffset) {
            OffsetPoint = sp;
            GPSAngle_OffsetDialog gaod = new GPSAngle_OffsetDialog();
            gaod.Initialize(this, sp.lat, sp.lon, getActivity());
            gaod.show();
        } else {
            Toast.makeText(getActivity(), "Vertex Collected",
                    Toast.LENGTH_SHORT).show();
            ObstructionTabBase obBaseFrag = getCurrentTab();
            obBaseFrag.newPosition(sp, StoredTopCollected);
        }
    }

    public LineObstruction getCurrentLine() {
        return _curLine;
    }

    public String getCurrentLineUID() {
        if (_curLine != null)
            return _curLine.uid;
        return null;
    }

    public void LineComplete(boolean resetScreen, ObstructionTabBase caller) {
        if (hardwareInterface != null) {
            try {
                hardwareInterface.EndCurrentRoute(false);
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to end current route", e);
            }
        }

        //last update on the line
        if (_curLine != null && caller != null) {
            //get the next line working...
            _curLine.type = caller.getCurrentType();
            _curLine.remarks = caller
                    .getDescription();
            _curLine.height = caller.getHeight_m();
            _curLine.width = caller.getWidth_m();
            _curLine.closed = _curLine.filled = Constants
                    .isArea(_curLine.type);
            if (opc != null)
                opc.UpdateLine(_curLine, false);
        }
        _curLine = null;
        if (resetScreen && caller != null) {
            caller.HideNextLineButton();
            caller.HideUndoButton();
        }
    }

    public boolean hasActiveLine() {
        return _curLine != null && _curLine.points.size() > 0;
    }

    protected void ToggleBreadcrumbCollection(boolean StartBCCollection) {
        ObstructionTabBase obBaseFrag = getCurrentTab();
        //get the next line working...
        String CurrentType = obBaseFrag.getCurrentType();
        String CurrentGroupName = obBaseFrag.CurrentObstruction.group;
        double WidthOffset = obBaseFrag.getLRCOffset();
        if (StartBCCollection) {
            try {
                String CurrentUIDName = getCurrentLineUID();
                if (CurrentUIDName == null) {
                    CurrentUIDName = GetUID(CurrentGroupName, CurrentType);
                    _curLine = new LineObstruction();
                    _curLine.closed = true;
                    _curLine.filled = true;
                    _curLine.group = ATSKConstants.DEFAULT_GROUP;
                    _curLine.uid = CurrentUIDName;
                    _curLine.height = obBaseFrag.CurrentObstruction.height;
                    _curLine.width = obBaseFrag.CurrentObstruction.width;
                    _curLine.remarks = obBaseFrag.CurrentRemark;
                    _curLine.remarks = obBaseFrag.getCurrentType();
                    if (opc != null)
                        opc.NewLine(_curLine);
                }
                double Height_m = getFloat(user_settings,
                        ATSKConstants.OBSTRUCTION_METHOD_GPS_HEIGHT_M, 2);
                if (hardwareInterface != null)
                    hardwareInterface.StartNewLineRoute(CurrentUIDName,
                            CurrentType, CurrentGroupName, Height_m,
                            WidthOffset);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else {
            try {
                hardwareInterface.EndCurrentRoute(false);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        Log.d(TAG, "Done toggling route collection");

    }

    @Override
    public boolean GPS2PPlusDStateChange(int Point2Collect, boolean TopCollected) {
        if (opc == null)
            return false;
        if (Point2Collect == 1) {
            try {
                TwoPPlusDPoint1 = hardwareInterface
                        .getMostRecentPoint();
            } catch (RemoteException e) {
                e.printStackTrace();
                return false;
            }
            if (TwoPPlusDPoint1 == null) {
                Toast.makeText(getActivity(), "No GPS Available",
                        Toast.LENGTH_SHORT).show();
                return false;
            }
            UpdateNotification(getActivity(), "2P + D", "First Point",
                    Conversions.GetLatLonDM(TwoPPlusDPoint1.lat,
                            TwoPPlusDPoint1.lon), "");

            PointObstruction newPointObstruction = new PointObstruction(
                    TwoPPlusDPoint1);
            newPointObstruction.uid = TWO_P_PLUS_D_P1_UID;
            newPointObstruction.type = Constants.PO_GENERIC_POINT;
            opc.NewPoint(newPointObstruction);

            return true;
        } else if (Point2Collect == 2) {
            try {
                TwoPPlusDPoint2 = hardwareInterface
                        .getMostRecentPoint();
            } catch (RemoteException e) {
                e.printStackTrace();
                return false;
            }
            if (TwoPPlusDPoint2 == null) {
                Toast.makeText(getActivity(), "No GPS Available",
                        Toast.LENGTH_SHORT).show();
                return false;
            }
            UpdateNotification(getActivity(), "2P + D", "Second Point",
                    Conversions.GetLatLonDM(TwoPPlusDPoint2.lat,
                            TwoPPlusDPoint2.lon), "");

            //clear the reminder point
            opc.DeletePoint(ATSKConstants.DEFAULT_GROUP, TWO_P_PLUS_D_P1_UID,
                    true);

            if (TwoPPlusDPoint1 != null) {
                //pop up a dialog to ask right/left and offset.
                TwoPPlusDSide_OffsetDialog tppdDlg = new TwoPPlusDSide_OffsetDialog();
                tppdDlg.Initialize(this, TopCollected);
                tppdDlg.show(getFragmentManager(), "Two Point + Distance");
            }
        }
        return true;
    }

    @Override
    public void RangeDirectionSelected(double Range_m, Boolean ToRight,
            boolean TopCollected) {
        if (opc == null)
            return;

        //clear the button bar
        //LOU is this for LRF or GPS?
        ATSKApplication.setObstructionCollectionMethod(
                ATSKIntentConstants.OB_STATE_HIDDEN,
                TAG, false);

        //get type from the area fragment
        ObstructionTabBase obBaseFrag = getCurrentTab();
        String Type = obBaseFrag.getCurrentType();
        String GroupName = obBaseFrag.CurrentObstruction.group;
        //draw our 4 points now...
        String NewUID = GetUID(GroupName, Type);

        _curLine = new LineObstruction();
        _curLine.closed = true;
        _curLine.filled = true;
        _curLine.group = GroupName;
        _curLine.uid = NewUID;
        _curLine.height = obBaseFrag.CurrentObstruction.height;
        _curLine.width = obBaseFrag.CurrentObstruction.width;
        _curLine.remarks = "";
        opc.NewLine(_curLine);

        _curLine = null;
        SurveyPoint newPoint = new SurveyPoint();
        newPoint.setSurveyPoint(TwoPPlusDPoint1.lat, TwoPPlusDPoint1.lon,
                TwoPPlusDPoint1.alt);
        opc.LineAppendPoint(GroupName, NewUID, newPoint);

        newPoint.setSurveyPoint(TwoPPlusDPoint2.lat, TwoPPlusDPoint2.lon,
                TwoPPlusDPoint2.alt);
        opc.LineAppendPoint(GroupName, NewUID, newPoint);

        //get Angle from first to second line
        double AngleOfPath = Conversions.CalculateAngledeg(TwoPPlusDPoint1.lat,
                TwoPPlusDPoint1.lon, TwoPPlusDPoint2.lat, TwoPPlusDPoint2.lon);
        if (ToRight)
            AngleOfPath += 90;
        else
            AngleOfPath -= 90;

        double ThirdCorner[] = Conversions.AROffset(TwoPPlusDPoint2.lat,
                TwoPPlusDPoint2.lon, AngleOfPath, Range_m);

        newPoint.setSurveyPoint(ThirdCorner[0], ThirdCorner[1],
                TwoPPlusDPoint2.alt);
        opc.LineAppendPoint(GroupName, NewUID, newPoint);

        double FourthCorner[] = Conversions.AROffset(TwoPPlusDPoint1.lat,
                TwoPPlusDPoint1.lon, AngleOfPath, Range_m);

        newPoint.setSurveyPoint(FourthCorner[0], FourthCorner[1],
                TwoPPlusDPoint1.alt);
        opc.LineAppendPoint(GroupName, NewUID, newPoint);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                LineComplete(true, getCurrentTab());
            }
        }, 300);

    }

    @Override
    public void shotApproved(SurveyPoint sp, double range, double azimuth,
            double elev, boolean TopCollected) {
        if (opc == null)
            return;
        super.shotApproved(sp, range, azimuth, elev, TopCollected);
        Log.d(TAG, "Shot Approved");

        if (StoredStateBeforeHiding
                .equals(ATSKIntentConstants.OB_STATE_2PPLUSD_LRF))
            ATSKApplication.setObstructionCollectionMethod(
                    ATSKIntentConstants.OB_STATE_2PPLUSD_LRF_2,
                    TAG, false);
        else
            ATSKApplication.setObstructionCollectionMethod(
                    StoredStateBeforeHiding, TAG, false);

        String tag = _tabHost.getCurrentTabTag();

        int LRFIndex = ATSKIntentConstants
                .GetLRFIndex(StoredStateBeforeHiding);
        if (tag != null && tag.equals(AREA_TAB) && LRFIndex > 0) {
            if (LRFIndex == 1) {
                ATSKApplication.setObstructionCollectionMethod(
                        ATSKIntentConstants.OB_STATE_2PPLUSD_LRF_2,
                        TAG, false);
                if (TwoPPlusDPoint1 == null)
                    TwoPPlusDPoint1 = new SurveyPoint();
                TwoPPlusDPoint1.setSurveyPoint(sp);
                //draw temporary dot on map
                PointObstruction newPointObstruction = new PointObstruction();
                newPointObstruction.setSurveyPoint(sp);
                newPointObstruction.uid = TWO_P_PLUS_D_P1_UID;
                newPointObstruction.type = Constants.PO_GENERIC_POINT;

                opc.NewPoint(newPointObstruction);

                UpdateNotification(getActivity(), "2P + D", "FIRST Point",
                        Conversions.GetLatLonDM(TwoPPlusDPoint1.lat,
                                TwoPPlusDPoint1.lon), "Collect Next Point"); //clear the reminder point
                return;
            } else if (LRFIndex == 2) {
                ATSKApplication.setObstructionCollectionMethod(
                        ATSKIntentConstants.OB_STATE_REQUESTED_AREA,
                        TAG, false);
                if (TwoPPlusDPoint2 == null)
                    TwoPPlusDPoint2 = new SurveyPoint();

                TwoPPlusDPoint2.setSurveyPoint(sp);
                opc.DeletePoint(ATSKConstants.DEFAULT_GROUP,
                        TWO_P_PLUS_D_P1_UID, true);
                UpdateNotification(getActivity(), "2P + D", "Second Point",
                        Conversions.GetLatLonDM(TwoPPlusDPoint2.lat,
                                TwoPPlusDPoint2.lon), "Set Depth/Side"); //clear the reminder point

                if (TwoPPlusDPoint1 != null) {
                    //pop up a dialog to ask right/left and offset.
                    TwoPPlusDSide_OffsetDialog tppdDlg = new TwoPPlusDSide_OffsetDialog();
                    tppdDlg.Initialize(this, TopCollected);
                    tppdDlg.show(getFragmentManager(),
                            "Two Point + Distance");
                }
            }
        }
        ObstructionTabBase obBaseFrag = getCurrentTab();
        obBaseFrag.newPosition(sp, TopCollected);
    }

    @Override
    public void MapClickDetected(SurveyPoint sp) {
        String RABString = "";
        if (hardwareInterface != null) {
            SurveyPoint myPos;
            try {
                myPos = hardwareInterface.getMostRecentPoint();
                if (myPos != null) {
                    double RAB[] = Conversions.calculateRangeAngle(myPos, sp);
                    if (RAB[0] > 1000) {
                        //use miles
                        if (RAB[0] > 50000) {
                            RABString = String.format(" %s",
                                    Conversions.GetCardinalDirection(RAB[1]));
                        } else
                            RABString = String.format(LocaleUtil.getCurrent(),
                                    "%.1fNM@ %s", RAB[0] * Conversions.M2NM,
                                    Conversions.GetCardinalDirection(RAB[1]));
                    } else
                        RABString = String.format(LocaleUtil.getCurrent(),
                                "%.1fft@ %s", RAB[0] * Conversions.M2F,
                                Conversions.GetCardinalDirection(RAB[1]));

                } else {
                    RABString = "NO OWN POSITION";
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        String AltString = String.format(LocaleUtil.getCurrent(),
                "ALT: %.1fft MSL %s", sp.getMSL() * Conversions.M2F, RABString);
        UpdateNotification(getActivity(), "Map Clicked",
                Conversions.getCoordinateString(sp.lat, sp.lon,
                        user_settings.getString(ATSKConstants.COORD_FORMAT,
                                Conversions.COORD_FORMAT_DM)), AltString, "");

        //check if we're waiting on a click
        String method = ATSKApplication.getCollectionState();
        if (!method.equals(ATSKIntentConstants.OB_STATE_MAP_CLICK)) {
            //not waiting for a map click
            return;
        }

        ObstructionTabBase obBaseFrag = getCurrentTab();
        obBaseFrag.newPosition(sp, false);
    }

    public void GPSSinglePoint(SurveyPoint sp, boolean WithOffset,
            boolean TopCollected) {
        if (sp == null) {
            Toast.makeText(getActivity(), "No GPS Point Collected",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (WithOffset) {
            OffsetPoint = sp;
            GPSAngle_OffsetDialog gaod = new GPSAngle_OffsetDialog();
            gaod.Initialize(this, sp.lat, sp.lon, getActivity());
            gaod.show();
        } else {
            ObstructionTabBase obBaseFrag = getCurrentTab();
            obBaseFrag.newPosition(sp, TopCollected);

            String AltString = String.format(LocaleUtil.getCurrent(),
                    "ALT: %.1fft MSL", sp.getMSL() * Conversions.M2F);
            UpdateNotification(getActivity(), "GPS Point",
                    Conversions.GetLatLonDM(sp.lat, sp.lon),
                    AltString, "");
        }
    }

    @Override
    public void RangeDirectionSelected(double Range_m, double Angle_true,
            boolean TopCollected) {

        //apply offset for new point
        double newPos[] = Conversions.AROffset(OffsetPoint.lat,
                OffsetPoint.lon, Angle_true, Range_m);

        ObstructionTabBase obBaseFrag = getCurrentTab();

        SurveyPoint sp = new SurveyPoint(OffsetPoint);
        sp.lat = newPos[0];
        sp.lon = newPos[1];
        obBaseFrag.newPosition(sp, TopCollected);
        UpdateNotification(getActivity(), "GPS Point + Offset",
                Conversions.GetMGRS(sp.lat, sp.lon),
                Conversions.GetLatLonDM(sp.lat, sp.lon),
                String.format(LocaleUtil.getCurrent(), "OFFSET %.1fm %.1fdeg",
                        Range_m, Angle_true));
    }

    protected ObstructionTabBase getCurrentTab() {
        if (_tabHost != null) {
            String tag = _tabHost.getCurrentTabTag();
            Fragment tab = getChildFragmentManager()
                    .findFragmentByTag(tag);
            if (tab instanceof ObstructionTabBase)
                return (ObstructionTabBase) tab;
        }
        return null;
    }

    public boolean undoLastPoint() {
        if (opc != null && _curLine != null) {
            boolean deleted = opc.LineDeleteLastPoint(_curLine.group,
                    _curLine.uid);
            _curLine = opc.GetLine(_curLine.group, _curLine.uid);
            if (_curLine != null && _curLine.points.size() == 0)
                _curLine = null;
            return deleted;
        }
        return false;
    }

    public void AddPointObstruction(PointObstruction po) {
        if (opc != null)
            opc.NewPoint(po);
    }

    public void addLineObstruction(LineObstruction newLine, boolean add) {
        if (opc != null)
            opc.NewLine(newLine, add);
    }

    public void removeLineObstruction(LineObstruction line) {
        if (opc != null)
            opc.DeleteLine(ATSKConstants.DEFAULT_GROUP, line.uid);
    }

    public void AddPoint2LineObstruction(PointObstruction newPoint,
            double offset,
            boolean filled, boolean closed) {

        if (opc == null)
            return;

        // Calculate real new position based on LCR offset
        if (_curLine != null) {
            SurveyPoint lastPoint = _curLine.points
                    .get(_curLine.points.size() - 1);
            if (lastPoint != null) {
                double ang = Conversions.CalculateAngledeg(lastPoint.lat,
                        lastPoint.lon, newPoint.lat, newPoint.lon);
                double[] offsetCoord = Conversions.AROffset(newPoint.lat,
                        newPoint.lon, ang - 90.0f, offset);
                newPoint.lat = offsetCoord[0];
                newPoint.lon = offsetCoord[1];
            }
        }

        if (_curLine == null) {
            _curLine = new LineObstruction();
            _curLine.group = newPoint.group;
            _curLine.uid = newPoint.uid;
            _curLine.type = newPoint.type;
            _curLine.remarks = newPoint.remark;
            _curLine.closed = closed;
            _curLine.filled = filled;
            _curLine.height = newPoint.height;
            _curLine.width = newPoint.width;
            _curLine.points.add(newPoint);
            opc.NewLine(_curLine);
        } else {
            _curLine.points.add(newPoint);
            opc.LineAppendPoint(_curLine.group,
                    _curLine.uid, newPoint);
        }
    }

    public boolean ObstructionSelectedOnMap(
            final String group, final String uid, final int copies) {
        if (!_created) {
            runOnCreate(new Runnable() {
                public void run() {
                    ObstructionSelectedOnMap(group, uid, copies);
                }
            });
            return false;
        }

        if (opc == null)
            return false;

        //maybe we want to get the click updates??
        PointObstruction clickedPO = opc.GetPointObstruction(group, uid);
        if (clickedPO == null) {
            Intent intent = new Intent();
            intent.setAction(ATSKConstants.ATSK_MAP_CHANGE);
            intent.putExtra(ATSKConstants.SELECTED, false);
            com.atakmap.android.ipc.AtakBroadcast.getInstance().sendBroadcast(
                    intent);
            return false;
        }
        _selectedGroup = group;
        _selectedUID = uid;
        _copies = copies;
        _tabHost.clearAllTabs();
        //        mTabHost.removeAllViews();
        EditPointTabSpec = _tabHost.newTabSpec(EDIT_POINT_TAB).setIndicator(
                "", _plugin.getResources().getDrawable(
                        R.drawable.obstruction_point_edit));
        _tabHost.addTab(EditPointTabSpec, EditObstructionPointFragment.class,
                null);

        _tabHost.invalidate();
        return false;
    }

    public boolean LineSelectedOnMap(final String group, final String uid) {
        if (!_created) {
            runOnCreate(new Runnable() {
                public void run() {
                    LineSelectedOnMap(group, uid);
                }
            });
            return false;
        }

        if (opc == null)
            return false;

        //maybe we want to get the click updates??
        LineObstruction clickedLO = opc.GetLine(group, uid);
        if (clickedLO == null) {
            Intent intent = new Intent();
            intent.setAction(ATSKConstants.ATSK_MAP_CHANGE);
            intent.putExtra(ATSKConstants.SELECTED, false);
            com.atakmap.android.ipc.AtakBroadcast.getInstance().sendBroadcast(
                    intent);
            return false;
        }
        _selectedGroup = group;
        _selectedUID = uid;
        _tabHost.clearAllTabs();
        //        mTabHost.removeAllViews();
        if (clickedLO.closed) {
            EditAreaTabSpec = _tabHost.newTabSpec(EDIT_AREA_TAB).setIndicator(
                    "", _plugin.getResources().getDrawable(
                            R.drawable.obstruction_area_edit));
            _tabHost.addTab(EditAreaTabSpec, EditObstructionAreaFragment.class,
                    null);
        } else {
            EditLineTabSpec = _tabHost.newTabSpec(EDIT_LINE_TAB).setIndicator(
                    "", _plugin.getResources().getDrawable(
                            R.drawable.obstruction_line_edit));
            _tabHost.addTab(EditLineTabSpec,
                    EditObstructionRouteFragment.class,
                    null);
        }
        _tabHost.invalidate();
        return false;
    }

    public void CloseEditWindow(boolean returnToNewWindow) {
        if (returnToNewWindow) {
            Intent intent = new Intent();
            intent.setAction(ATSKConstants.ATSK_MAP_CHANGE);
            intent.putExtra(ATSKConstants.SELECTED, false);
            com.atakmap.android.ipc.AtakBroadcast.getInstance().sendBroadcast(
                    intent);

            addNewObstructionFragmentsToTabHost();
        }
        _curLine = null;
        _selectedUID = _selectedGroup = null;
    }

    public void RemovePointObstruction(String Group, String PointUid) {
        if (opc != null)
            opc.DeletePoint(Group, PointUid, true);
    }

    void StopRouteCollection() {
        if (hardwareInterface == null)
            return;

        try {
            if (hardwareInterface.isCollectingRoute()) {
                hardwareInterface.EndCurrentRoute(false);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

}
