
package com.gmeci.atsk.obstructions;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.atakmap.android.ipc.AtakBroadcast.DocumentedIntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentManager;

import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.log.Log;
import com.gmeci.atsk.resources.LogTime;
import com.gmeci.atsk.toolbar.ATSKToolbarComponent;
import com.gmeci.core.ATSKConstants;
import com.gmeci.core.ATSKIntentConstants;
import com.gmeci.core.LineObstruction;
import com.gmeci.core.PointObstruction;
import com.gmeci.core.SurveyPoint;
import com.gmeci.atsk.obstructions.obstruction.ObstructionDetailDialog;
import com.gmeci.atskservice.resolvers.AZProviderClient;
import com.gmeci.atskservice.resolvers.DBURIConstants;
import com.gmeci.atskservice.resolvers.ObstructionProviderClient;
import com.gmeci.atskservice.resolvers.GradientProviderClient;
import com.gmeci.constants.Constants;
import com.gmeci.conversions.Conversions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ObstructionController {
    private static final String TAG = "ObstructionController";

    private PointContentObserver _pointCO;
    private LineContentObserver _lineCO;
    private final Handler _coHandler = new Handler();
    private FragmentManager _fm;
    private final Map<String, String> _2525Map = new HashMap<String, String>();

    private MapObstructionController _moc;
    private ObstructionProviderClient _opc;
    private AZProviderClient _azpc;
    private GradientProviderClient _gpc;

    final private MapView _mapView;
    final Context _context;

    /**
     * Obstruction cache
     */
    private final Map<String, PointObstruction> _pointObs = new HashMap<String, PointObstruction>();
    private final Map<String, LineObstruction> _lineObs = new HashMap<String, LineObstruction>();

    public final BroadcastReceiver OBClickRx = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent
                    .getAction()
                    .equals(ATSKIntentConstants.OB_MENU_POINT_CLICK_ACTION)) {//point menu clicked
                Bundle extras = intent.getExtras();

                String uid = extras.getString(
                        ATSKIntentConstants.OB_MENU_UID, "None");
                String group = extras.getString(
                        ATSKIntentConstants.OB_MENU_GROUP,
                        ATSKConstants.DEFAULT_GROUP);
                String req = extras.getString(
                        ATSKIntentConstants.MENU_REQUEST, "Req");
                String type = extras.getString("obsType", "");

                // Line obstructions
                boolean isLine = type.equals("route") || type.equals("area");

                if (req.equals(ATSKIntentConstants.MENU_EDIT)) {
                    Intent editIntent = new Intent();
                    editIntent.setAction(isLine ?
                            ATSKConstants.L_OBSTRUCTION_CLICK_ACTION :
                            ATSKConstants.PT_OBSTRUCTION_CLICK_ACTION);
                    editIntent.putExtra(ATSKConstants.UID_EXTRA, uid);
                    editIntent.putExtra(ATSKConstants.GROUP_EXTRA, group);
                    AtakBroadcast.getInstance().sendBroadcast(editIntent);
                } else if (req.equals(ATSKIntentConstants.MENU_DELETE)) {
                    if (isLine)
                        _opc.DeleteLine(group, uid);
                    else
                        _opc.DeletePoint(group, uid, true);
                } else if (req.equals(ATSKIntentConstants.OB_MENU_EXTRA)) {
                    ObstructionDetailDialog odd = new ObstructionDetailDialog(
                            _mapView, ATSKToolbarComponent.getToolbar().
                                    getPluginContext());
                    if (isLine) {
                        LineObstruction lo = _opc.GetLine(group, uid);
                        odd.show(lo);
                    } else {
                        PointObstruction po = _opc.GetPointObstruction(
                                group, uid);
                        odd.show(po);
                    }
                }
            }
        }
    };

    private static ObstructionController _instance;

    public static ObstructionController getInstance() {
        return _instance;
    }

    public void setFragmentManager(final FragmentManager fm) {
        _fm = fm;
    }

    public ObstructionController(MapView mapView) {

        _mapView = mapView;
        _context = _mapView.getContext();

        _2525Map.put(Constants.PO_BLANK, "SUUAOPBLANK....");
        _2525Map.put(Constants.PO_RED, "SUUAOPRED......");
        _2525Map.put(Constants.PO_BLACK, "SUUAOPBLA......");
        _2525Map.put(Constants.PO_LASER, "SUUAOPBLA......");

        _2525Map.put(Constants.PO_AIRFIELD_INSTRUMENT,
                "SUUAOPRED......");
        _2525Map.put(Constants.LO_ARRESTING_GEAR,
                "SUUAOPAG.......");

        _2525Map.put(Constants.PO_ANTENNA,
                "SUUAOPAN.......");
        _2525Map.put(Constants.LO_BARRIER,
                "SUUAOPAB.......");
        _2525Map.put(Constants.PO_BERMS, "SUUAOPABU......");
        _2525Map.put(Constants.PO_BUSH, "SUUAOPTRB......");
        _2525Map
                .put(Constants.PO_CRATER, "SUUAOPCR.......");
        _2525Map.put(Constants.PO_DUMPSTER,
                "SUUAOPD........");
        _2525Map.put(Constants.PO_FIRE_HYDRANT,
                "SUUAOPHF.......");
        _2525Map.put(Constants.PO_FLAGPOLE,
                "SUUAOPPF.......");
        _2525Map.put(Constants.PO_FUEL_TANK,
                "SUUAOPTF.......");
        _2525Map.put(Constants.PO_HVAC_UNIT,
                "SUUAOPHVAC.....");
        _2525Map.put(Constants.PO_POLE, "SUUAOPP........");
        _2525Map.put(Constants.PO_LIGHT_POLE,
                "SUUAOPPL.......");
        _2525Map.put(Constants.PO_LIGHT, "SUUAOPL........");
        _2525Map.put(Constants.PO_MOUND, "SUUAOPM........");
        _2525Map.put(Constants.AO_POOL, "SUUAOPPOOL.....");
        _2525Map.put(Constants.PO_ROTATING_BEACON,
                "SUUAOPB........");
        _2525Map.put(Constants.PO_TREE, "SUUAOPTR.......");
        _2525Map.put(Constants.PO_SAT_DISH,
                "SUUAOPSD.......");
        _2525Map.put(Constants.PO_TREE, "SUUAOPS........");
        _2525Map.put(Constants.PO_TRANSFORMER,
                "SUUAOPT........");
        _2525Map.put(Constants.PO_WINDSOCK,
                "SUUAOPWXW......");
        _2525Map.put(Constants.PO_WXVANE, "SUUAOPWX......");
        _2525Map.put(Constants.PO_GENERIC_POINT,
                "SUUAOP.........");

        _instance = this;
    }

    /**
     * Given a width produce a set of offset points, left and right from centerline.
     */
    public static List<SurveyPoint> createOffsetPoints(
            final List<SurveyPoint> centerLine,
            final double width) {

        if (width == 0 || centerLine == null || centerLine.size() == 0)
            return centerLine;

        List<SurveyPoint> leftSide = new ArrayList<SurveyPoint>();
        List<SurveyPoint> rightSide = new ArrayList<SurveyPoint>();

        // TODO: break this out into its own method and account for centerline left right
        // average the angles both ways so we do do not end up with a pinched runway
        if (width > 0) {
            final int size = centerLine.size();
            final double halfWidth = width / 2;
            for (int i = 0; i < size; i++) {
                SurveyPoint c = centerLine.get(i);
                SurveyPoint p = i > 0 ? centerLine.get(i - 1) : null;
                SurveyPoint n = i < size - 1 ? centerLine.get(i + 1) : null;
                double ang = Conversions.computeAngle(p, c, n);

                double[] left = Conversions.AROffset(c.lat, c.lon, ang + 90,
                        halfWidth);
                double[] right = Conversions.AROffset(c.lat, c.lon, ang - 90,
                        halfWidth);

                SurveyPoint p1 = new SurveyPoint(left[0], left[1]);
                SurveyPoint p2 = new SurveyPoint(right[0], right[1]);
                leftSide.add(0, p1);
                rightSide.add(p2);

            }
            // close the loop
            if (!rightSide.isEmpty())
                leftSide.add(rightSide.get(0));
            rightSide.addAll(leftSide);
        }
        return rightSide;

    }

    public void onResume() {
        _opc = new ObstructionProviderClient(_context);
        _opc.Start();
        _azpc = new AZProviderClient(_context);
        _azpc.Start();
        _gpc = new GradientProviderClient(_context);
        _gpc.Start();

        if (_moc == null)
            _moc = new MapObstructionController(_mapView);

        _lineCO = new LineContentObserver(_coHandler);
        _pointCO = new PointContentObserver(_coHandler);

        _context.getContentResolver().registerContentObserver(
                Uri.parse(DBURIConstants.POINT_URI), true, _pointCO);
        _context.getContentResolver().registerContentObserver(
                Uri.parse(DBURIConstants.LINE_URI), true, _lineCO);
        _context.getContentResolver().registerContentObserver(
                Uri.parse(DBURIConstants.LINE_POINT_URI), true, _lineCO);

        DocumentedIntentFilter OBMenuFilter = new DocumentedIntentFilter();
        OBMenuFilter
                .addAction(ATSKIntentConstants.OB_MENU_POINT_CLICK_ACTION);
        AtakBroadcast.getInstance().registerReceiver(OBClickRx, OBMenuFilter);

    }

    public void dispose() {
        if (_moc != null)
            _moc.dispose();
    }

    public void onPause() {
        try {
            _context.getContentResolver().unregisterContentObserver(_pointCO);
        } catch (Exception e) {
            Log.d(TAG, "content observer not previously registered for: "
                    + _pointCO);
        }
        try {
            _context.getContentResolver().unregisterContentObserver(_lineCO);
        } catch (Exception e) {
            Log.d(TAG, "content observer not previously registered for: "
                    + _lineCO);
        }

        try {
            AtakBroadcast.getInstance().unregisterReceiver(OBClickRx);
        } catch (Exception e) {
            Log.d(TAG, "receiver not previously registered for: " + OBClickRx);
        }

        RemoveAllLineObstructions();
        RemoveAllPointObstructions();
        _opc.Stop();
        _azpc.Stop();
        _gpc.Stop();
    }

    public void DrawAllObstructions() {
        DrawAllPointObstructions();
        DrawAllLineObstructions();
    }

    public void DrawAllPointObstructions() {
        LogTime.beginMeasure(TAG, "DrawAllPointObstructions");
        final List<PointObstruction> points = _opc.getAllPointObstructions();

        // Update cache
        initPointCache(points);

        // Draw
        for (PointObstruction po : points) {
            //draw on the map...
            if (po.group.contains(ATSKConstants.APRON_GROUP)) {
                // Parking plan aprons removed
            } else if (!po.uid.contains(ATSKConstants.TEMP_POINT_UID))
                //if a temp point exists in DB - don't draw it...
                AddNewObstruction(po);
        }
        LogTime.endMeasure(TAG, "DrawAllPointObstructions");
    }

    public void RemoveAllPointObstructions() {
        // clear all the point obstructions off the map
        final List<PointObstruction> points = _opc.getAllPointObstructions();
        for (PointObstruction po : points) {
            _moc.RemovePoint("", po.uid);
            if (po.group.equals(ATSKConstants.APRON_GROUP))
                _moc.RemoveLine(ATSKConstants.APRON_GROUP, po.uid);
        }
        clearPointCache();
    }

    public void DrawAllLineObstructions() {
        LogTime.beginMeasure(TAG, "DrawAllLineObstructions");
        //Draw all Default group lines
        final List<LineObstruction> lines = _opc.getAllLineObstructions(true);
        initLineCache(lines);
        for (LineObstruction lo : lines) {
            //draw on the map...
            if (lo != null
                    && !lo.type
                            .endsWith(ATSKConstants.INCURSION_LINE_DEPARTURE_WORST)
                    && !lo.type
                            .endsWith(ATSKConstants.INCURSION_LINE_APPROACH_WORST)
                    && !lo.type
                            .endsWith(ATSKConstants.INCURSION_LINE_DEPARTURE)
                    && !lo.type.endsWith(ATSKConstants.INCURSION_LINE_APPROACH)) {
                _moc.UpdateLine(lo);
            }
        }
        LogTime.endMeasure(TAG, "DrawAllLineObstructions");
    }

    public void RemoveAllLineObstructions() {
        final List<LineObstruction> lines = _opc.getAllLineObstructions(false);
        for (LineObstruction lo : lines) {
            //remove from map
            if (lo != null)
                _moc.RemoveLine(ATSKConstants.DEFAULT_GROUP, lo.uid);
        }
        clearLineCache();
    }

    public boolean AddNewObstruction(PointObstruction po) {
        if (po.type.endsWith(ATSKConstants.INCURSION_LINE_DEPARTURE_WORST)
                || po.type
                        .endsWith(ATSKConstants.INCURSION_LINE_APPROACH_WORST))
            return false;

        _moc.AddNewObstruction(po);
        return true;
    }

    public void UpdateLine(String group, String uID) {
        LineObstruction line = _opc.GetLine(group, uID);
        if (line == null || line.points.size() < 1) {
            _moc.RemoveLine(group, uID);
            synchronized (_lineObs) {
                _lineObs.remove(uID);
            }
        } else {
            synchronized (_lineObs) {
                _lineObs.put(uID, line);
            }
            _moc.UpdateLine(line);
        }
    }

    public void UpdatePoint(String group, String uID) {
        PointObstruction existingPoint = _opc.GetPointObstruction(group, uID);
        if (existingPoint == null) {
            if (group.equals(ATSKConstants.APRON_GROUP)) {
                _moc.RemoveApron(group, uID);
            } else if (uID
                    .endsWith(ATSKConstants.INCURSION_LINE_APPROACH_WORST)
                    || uID.endsWith(ATSKConstants.INCURSION_LINE_DEPARTURE_WORST)) {
                _moc.RemoveLine(group, uID);
            } else
                _moc.RemovePoint(group, uID);
            synchronized (_pointObs) {
                _pointObs.remove(uID);
            }
        } else {
            synchronized (_pointObs) {
                _pointObs.put(uID, existingPoint);
            }
            if (existingPoint.group.equals(ATSKConstants.APRON_GROUP)) {
                // Do nothing - Parking plan aprons are removed
            } else if (existingPoint.uid
                    .endsWith(ATSKConstants.INCURSION_LINE_APPROACH_WORST)
                    || existingPoint.uid
                            .endsWith(ATSKConstants.INCURSION_LINE_DEPARTURE_WORST)
                    ||
                    existingPoint.uid
                            .endsWith(ATSKConstants.INCURSION_LINE_APPROACH)
                    || existingPoint.uid
                            .endsWith(ATSKConstants.INCURSION_LINE_DEPARTURE)) {

                // Construct worst obstruction bounding area
                SurveyPoint[] corners = existingPoint.getCorners(
                        Math.max(existingPoint.width, 20),
                        Math.max(existingPoint.length, 20), true);
                ArrayList<SurveyPoint> points = new ArrayList<SurveyPoint>(
                        Arrays.asList(corners));

                LineObstruction lo = new LineObstruction();
                lo.uid = uID;
                lo.group = group;
                lo.points = points;
                lo.remarks = getLabel(existingPoint);
                lo.type = existingPoint.type;
                lo.width = 0;
                _moc.UpdateLine(lo);
            } else {
                if (existingPoint.remark == null)
                    existingPoint.remark = "";
                if (existingPoint.remark.contains("\n")) {
                    existingPoint.remark = "";
                }
                _moc.UpdatePoint(uID, existingPoint);
            }
        }
    }

    public static String getLabel(PointObstruction po) {

        if (po.type.contains("IncursionLineDeparture"))
            return "";
        if (po.type.contains("IncursionLineApproach"))
            return "";

        if (po.remark != null && po.remark.length() > 0)
            return po.remark;
        return po.type;
    }

    public MapObstructionController getMapController() {
        return _moc;
    }

    public PointObstruction getPointObstruction(String uid) {
        synchronized (_pointObs) {
            return _pointObs.get(uid);
        }
    }

    public List<PointObstruction> getPointObstructions() {
        List<PointObstruction> copy = new ArrayList<PointObstruction>();
        synchronized (_pointObs) {
            copy.addAll(_pointObs.values());
        }
        return copy;
    }

    private void initPointCache(List<PointObstruction> points) {
        synchronized (_pointObs) {
            _pointObs.clear();
            for (PointObstruction po : points)
                _pointObs.put(po.uid, po);
        }
    }

    private void clearPointCache() {
        synchronized (_pointObs) {
            _pointObs.clear();
        }
    }

    public LineObstruction getLineObstruction(String uid) {
        synchronized (_lineObs) {
            return _lineObs.get(uid);
        }
    }

    public List<LineObstruction> getLineObstructions() {
        List<LineObstruction> copy = new ArrayList<LineObstruction>();
        synchronized (_lineObs) {
            copy.addAll(_lineObs.values());
        }
        return copy;
    }

    private void initLineCache(List<LineObstruction> lines) {
        synchronized (_lineObs) {
            _lineObs.clear();
            for (LineObstruction lo : lines)
                _lineObs.put(lo.uid, lo);
        }
    }

    private void clearLineCache() {
        synchronized (_lineObs) {
            _lineObs.clear();
        }
    }

    private class PointContentObserver extends ContentObserver {

        public PointContentObserver(Handler handler) {
            super(handler);
        }

        public void onChange(boolean selfChange) {
            super.onChange(selfChange);

        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            String Group = ATSKConstants.GetGroupFromURI(uri);

            if (uri.toString().contains("pointGroup")) {
                //we're deleting a group - no single point to update
            } else {
                String UID = ATSKConstants.GetUIDFromURI(uri);
                UpdatePoint(Group, UID);
            }
        }
    }

    private class LineContentObserver extends ContentObserver {
        public LineContentObserver(Handler handler) {
            super(handler);
        }

        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);

            String Group = ATSKConstants.GetGroupFromURI(uri);
            String UID = ATSKConstants.GetUIDFromURI(uri);
            UpdateLine(Group, UID);
        }
    }
}
