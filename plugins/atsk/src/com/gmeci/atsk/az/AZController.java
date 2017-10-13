
package com.gmeci.atsk.az;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.atakmap.android.ipc.AtakBroadcast.DocumentedIntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.support.v4.app.FragmentManager;
import android.util.SparseArray;
import android.widget.Toast;

import com.atakmap.android.ipc.AtakBroadcast;
import com.gmeci.atsk.export.ATSKTransferPackage;
import com.gmeci.atsk.resources.ATSKApplication;
import com.gmeci.atsk.resources.LogTime;
import com.gmeci.atskservice.dz.DZCapabilities;
import com.gmeci.atskservice.dz.DZRequirementsParser;

import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.log.Log;
import com.atakmap.filesystem.HashingUtils;
import com.atakmap.math.MathUtils;
import com.gmeci.atsk.ATSKFragmentManager;
import com.gmeci.atsk.ATSKMapComponent;
import com.gmeci.atsk.toolbar.ATSKToolbar;
import com.gmeci.atsk.visibility.VizPrefs;
import com.gmeci.core.ATSKConstants;
import com.gmeci.core.LineObstruction;
import com.gmeci.core.PointObstruction;
import com.gmeci.core.SurveyData;
import com.gmeci.core.SurveyData.AZ_TYPE;
import com.gmeci.core.SurveyPoint;
import com.gmeci.atsk.ATSKATAKConstants;
import com.gmeci.atskservice.resolvers.AZProviderClient;
import com.gmeci.atskservice.resolvers.AZURIConstants;
import com.gmeci.constants.Constants;
import com.gmeci.helpers.FARPHelper;
import com.gmeci.atskservice.farp.FARPACParser;
import com.gmeci.atskservice.farp.FARPTankerItem;
import com.gmeci.helpers.AZHelper;
import com.gmeci.helpers.PolygonHelper;
import com.gmeci.conversions.Conversions;
import com.gmeci.conversions.Conversions.Unit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AZController {

    public static final int SCREEN_SHOT_COUNT = 3;
    private static final String TAG = "AZController";
    private static final String CONTROLLER_TAG = "controller_tag";
    private static final double MAX_FARP_RANGE = 1500;

    private final static double THIRD_METER = 1 / 3d;

    // Offset of FARP items (PO, HDP, HRS, etc.)
    private static final double FARP_ITEM_OFFSET = 2d;

    private AZContentObserver _azco;
    private final Context _context;
    private final AZProviderClient _azpc;
    private MapAZController _azmc;

    private DZRequirementsParser _dzParser;

    private String _storedSurvey = "";
    private String _announcedSurvey = "";
    private Map<String, FARPTankerItem> _tankers = new HashMap<String, FARPTankerItem>();
    private FragmentManager _fm;
    private FARPHelper _fh;
    private final Context _plugin;
    private final MapView _mapView;
    private MapGroup _mapGroup;

    private static AZController _instance;
    //private Updater calc;
    private static final HashMap<String, String> _drawCache = new HashMap<String, String>();
    //private static String _lastDrawContent = "";

    final BroadcastReceiver ScreenShotRX = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle Extras = intent.getExtras();
            String SurveyUID = Extras.getString(ATSKConstants.UID_EXTRA,
                    "default");
            double CenterLat = Extras.getDouble(ATSKConstants.LAT_EXTRA, 32);
            double CenterLon = Extras.getDouble(ATSKConstants.LON_EXTRA, -86);
            double Length_m = (float) Extras.getDouble(
                    ATSKConstants.LENGTH_EXTRA, 1000);
            //do we need to change what the map shows as the current survey?

            SurveyData currentSurvey = _azpc.getAZ(SurveyUID, false);
            if (currentSurvey == null)
                return;
            currentSurvey.screenShotFileNameList = new ArrayList<String>();

            int LastZELevel = -1;
            _azmc.PrepareScreenShots(SCREEN_SHOT_COUNT);
            for (int i = 0; i < SCREEN_SHOT_COUNT; i++) {
                //find out what zoom level we ended up on

                _azmc.ZoomExtents(CenterLat, CenterLon, Length_m, LastZELevel,
                        true);
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                String FileName = _azmc.CaptureScreen();
                if (FileName != null && FileName.length() > 0)
                    currentSurvey.screenShotFileNameList.add(FileName);

                LastZELevel = MapAZController.LAST_ZE_LEVEL - 1;
            }
            _azpc.UpdateAZ(currentSurvey, "ClearSS", false);
            Intent screenShotRequestIntent = new Intent(
                    ATSKConstants.SCREEN_SHOT_COMPLETE_ACTION);
            screenShotRequestIntent.putExtra(ATSKConstants.UID_EXTRA,
                    currentSurvey.uid);
            AtakBroadcast.getInstance().sendBroadcast(screenShotRequestIntent);
        }
    };
    final BroadcastReceiver CurrentSurveyRX = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String curSurvey = intent.getExtras().getString(
                    ATSKConstants.UID_EXTRA);
            //do we need to change what the map shows as the current survey?
            if (!_storedSurvey.equals(curSurvey)) {
                //redraw the existing stored survey as not current - then redraw the newCurrentSurvey as the currentSurvey
                UpdateAZ(_storedSurvey);
                _storedSurvey = curSurvey;
                UpdateAZ(_storedSurvey);
            }
            _announcedSurvey = curSurvey;
        }

    };

    public AZController(Context pluginContext, MapView mapView) {

        _mapView = mapView;
        _mapGroup = mapView.getRootGroup().findMapGroup(
                ATSKATAKConstants.ATSK_MAP_GROUP_AZ);
        if (_mapGroup == null)
            _mapGroup = mapView.getRootGroup().addGroup(
                    ATSKATAKConstants.ATSK_MAP_GROUP_AZ);

        //calc = new Updater();

        _context = mapView.getContext();
        _plugin = pluginContext;

        _azmc = new MapAZController(mapView);

        _azpc = new AZProviderClient(_context);
        _azpc.Start();

        FARPACParser facp = new FARPACParser();
        try {
            _tankers = facp.parseFile();
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        _dzParser = new DZRequirementsParser();
        try {
            _dzParser.parseRequirementsFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        _instance = this;

    }

    synchronized static public AZController getInstance() {
        return _instance;
    }

    public void setFragmentManager(final FragmentManager fm) {
        _fm = fm;
    }

    public void onPause() {

        if (_azco != null)
            _context.getContentResolver().unregisterContentObserver(_azco);

        try {
            AtakBroadcast.getInstance().unregisterReceiver(CurrentSurveyRX);
        } catch (Exception ioe) {
            Log.d(TAG, "receiver not registered: " + CurrentSurveyRX);
        }
        try {
            AtakBroadcast.getInstance().unregisterReceiver(ScreenShotRX);
        } catch (Exception ioe) {
            Log.d(TAG, "receiver not registered: " + ScreenShotRX);
        }

        _azmc.close();
    }

    public void onResume() {
        if (_azmc == null)
            _azmc = new MapAZController(_mapView);

        DocumentedIntentFilter CurrentSurveyChangeFilter = new DocumentedIntentFilter();
        CurrentSurveyChangeFilter
                .addAction(ATSKConstants.CURRENT_SURVEY_CHANGE_ACTION);
        AtakBroadcast.getInstance().registerReceiver(CurrentSurveyRX,
                CurrentSurveyChangeFilter);

        DocumentedIntentFilter ScreenShotRequestFilter = new DocumentedIntentFilter();
        ScreenShotRequestFilter
                .addAction(ATSKConstants.SCREEN_SHOT_REQUEST_ACTION);
        AtakBroadcast.getInstance().registerReceiver(ScreenShotRX,
                ScreenShotRequestFilter);

        Handler coHandler = new Handler();
        _azco = new AZContentObserver(coHandler);
        _context.getContentResolver().registerContentObserver(
                Uri.parse(AZURIConstants.SINGLE_AZ_URI), true, _azco);

    }

    public Map<String, FARPTankerItem> getTankers() {
        return new HashMap<String, FARPTankerItem>(_tankers);
    }

    public FARPTankerItem getTanker(String name) {
        return _tankers.get(name);
    }

    synchronized public int DrawAllAZs() {
        int AZsDrawn = 0;

        //LOU get fewer fields
        final Cursor azCursor = _azpc.getAllSurveys(true);

        String CurrentSurveyUID = _azpc.getSetting(
                ATSKConstants.CURRENT_SURVEY, TAG);
        double Lat = Double.NaN;
        double Lon = Double.NaN;
        double Length = Double.NaN;

        if (azCursor != null) {
            for (azCursor.moveToFirst(); !azCursor.isAfterLast(); azCursor
                    .moveToNext()) {
                boolean Visible = Boolean.parseBoolean(azCursor
                        .getString(AZURIConstants.COLUMN_VISIBLE_INDEX));
                String UID = _azpc.getSurveyUID(azCursor);
                if (Visible || CurrentSurveyUID.equals(UID)) {
                    //the current survey is ALWAYS visible
                    Log.d(TAG, "drawing az: " + UID);
                    UpdateAZ(UID);
                    if (CurrentSurveyUID.equals(UID)) {
                        Log.d(TAG, "setting zoom parameters to az: " + UID);
                        Lat = azCursor
                                .getDouble(AZURIConstants.COLUMN_LAT_INDEX);
                        Lon = azCursor
                                .getDouble(AZURIConstants.COLUMN_LON_INDEX);
                        Length = azCursor
                                .getFloat(azCursor
                                        .getColumnIndex(AZURIConstants.COLUMN_LENGTH_M));
                    }
                }
            }
            azCursor.close();
        }

        return AZsDrawn;
    }

    synchronized public void RemoveAllAZs() {
        final Cursor azCursor = _azpc.getAllSurveys(true);
        String currentSurveyUID = _azpc.getSetting(
                ATSKConstants.CURRENT_SURVEY,
                TAG);

        if (azCursor != null) {
            for (azCursor.moveToFirst(); !azCursor.isAfterLast(); azCursor
                    .moveToNext()) {
                boolean visible = Boolean.parseBoolean(azCursor
                        .getString(AZURIConstants.COLUMN_VISIBLE_INDEX));
                String uid = _azpc.getSurveyUID(azCursor);
                if (visible || currentSurveyUID.equals(uid)) {
                    Log.d(TAG, "Removing AZ " + uid);
                    _azmc.DeleteAZ(uid, true);
                }
            }
            azCursor.close();
        }
        _drawCache.clear();
    }

    /*private class Updater implements Runnable, Disposable {
        boolean disposed;
        int state;
        Thread thread;

        public Updater() {
            this.disposed = false;
            this.state = 0;

            this.thread = new Thread(this);
            this.thread.setPriority(Thread.NORM_PRIORITY);
            this.thread.setName("az-renderer");
            this.thread.start();
        }

        public synchronized void change(String uid) {
            if (lastUID != null && !lastUID.equals(uid))
                _azmc.DeleteAZ(lastUID, true);
            lastUID = uid;
            if(!redraws.contains(uid)) {
                Log.d(TAG, "Adding " + uid + " to redraws");
                redraws.add(uid);
                this.state++;
                this.notify();
            }
        }

        @Override
        public void dispose() {
            synchronized (this) {
                this.disposed = true;
                this.notify();
            }
        }
        @Override
        public void run() {
            try {
                int compute = 0;
                while (true) {
                    synchronized (this) {
                        if (this.disposed)
                            break;
                        if (compute == this.state) {
                            try {
                                this.wait();
                            } catch (InterruptedException ignored) {
                            }
                            continue;
                        }
                        compute = this.state;
                    }


                    Log.d(TAG, "Begin drawing " + redraws.size() + " surveys");
                    for(String uid : redraws) {
                        Log.d(TAG, "Drawing " + uid);
                        prUpdateAZ(uid);
                    }
                }
            } finally {
            }
        }

        private String lastUID;
        private final Set<String> redraws =
                Collections.synchronizedSet(new HashSet<String>());
    }*/

    public void UpdateAZ(String uID) {
        //calc.change(uID);
        prUpdateAZ(uID);
    }

    synchronized private void prUpdateAZ(String uID) {
        if (ATSKToolbar.isClosing) {
            Log.w(TAG, "Failed to UpdateAZ (ATSK is closing)");
            return;
        }
        SurveyData survey = _azpc.getAZ(uID, false);
        boolean Visible = _azpc.isVisible(uID);
        String CurrentSurveyUID = _azpc.getSetting(
                ATSKConstants.CURRENT_SURVEY,
                TAG);
        boolean isCurrentSurvey = uID.equals(CurrentSurveyUID);
        if (survey == null) {
            //delete the entire AZ (not sure we can recycle the FSIDs
            _drawCache.remove(uID);
            _azmc.DeleteAZ(uID, isCurrentSurvey);
        } else {
            if (!isCurrentSurvey && !Visible) {
                _drawCache.remove(uID);
                _azmc.DeleteAZ(uID, false);
                return;
            }

            // Calculate MD5 sum of survey data + current survey UID
            // (since changing current survey requires redraw)
            String surveyContent = Conversions.toJson(survey);
            String currentSurveyUID = _azpc.getSetting(
                    ATSKConstants.CURRENT_SURVEY, TAG);
            String md5sum = HashingUtils.md5sum(currentSurveyUID
                    + surveyContent);

            // No changes - don't redraw
            if (_drawCache.containsKey(uID)
                    && _drawCache.get(uID).equals(md5sum)) {
                Log.d(TAG, "Skipping UpdateAZ call, survey "
                        + uID + " hasn't changed since last update.");
                return;
            }

            // Log difference in content
            /*for(int i = 0; i < _lastDrawContent.length(); i++) {
                if(surveyContent.length() <= i) {
                    Log.w(TAG, "Diff: (length shorter)");
                    break;
                }
                if(_lastDrawContent.charAt(i) != surveyContent.charAt(i)) {
                    Log.w(TAG, "old = " + _lastDrawContent.substring(Math.max(i-64, 0), i+32)
                            + "\nnew = " + surveyContent.substring(Math.max(i-64, 0), i+32));
                    break;
                }
            }*/

            // Save state
            _drawCache.put(uID, md5sum);
            //_lastDrawContent = surveyContent;

            if (isCurrentSurvey && _storedSurvey.isEmpty())
                _storedSurvey = uID;

            // Only used by HLZ and DZ
            double radius = survey.getRadius();

            if (survey.getType() == AZ_TYPE.DZ) {//DZ
                // Anchors and PIs
                List<PointObstruction> DZPositions = BuildDZPoints(survey,
                        isCurrentSurvey);
                ArrayList<LineObstruction> AZOutline = BuildAZPolygons(
                        survey, isCurrentSurvey);

                _azmc.UpdateDZ(survey.uid,
                        _azpc.getAZName(survey.uid), AZOutline,
                        survey.center, radius, DZPositions,
                        isCurrentSurvey, survey.getSurveyName());
            } else if (survey.getType() == AZ_TYPE.HLZ) {//HLZ
                // Anchor points
                List<PointObstruction> hlzPoints = BuildAnchors(survey,
                        false);
                ArrayList<LineObstruction> AZOutline = BuildAZPolygons(
                        survey, isCurrentSurvey);

                _azmc.UpdateHLZ(survey.uid,
                        _azpc.getAZName(survey.uid), AZOutline,
                        survey.center, radius, hlzPoints,
                        isCurrentSurvey, survey.getSurveyName());
            } else if (survey.getType() == AZ_TYPE.LZ
                    || survey.getType() == AZ_TYPE.STOL) {
                LogTime.beginMeasure(TAG, "Draw LZ");
                LineObstruction LZOutline = PolygonHelper
                        .getAZOutline(survey, isCurrentSurvey);
                ArrayList<PointObstruction> LZPoints = BuildLZPoints(survey);
                ArrayList<LineObstruction> LZPolys = BuildLZPolygons(
                        survey, LZPoints);
                //ArrayList<LineObstruction> approachCorners
                _azmc.UpdateLZ(survey.uid, LZOutline, LZPolys, LZPoints,
                        isCurrentSurvey, survey.getSurveyName(),
                        survey.width, survey.valid);
                LogTime.endMeasure(TAG, "Draw LZ");
            } else if (survey.getType() == AZ_TYPE.FARP) {
                List<LineObstruction> FARPPolygons = BuildFARPPolygons(
                        survey, isCurrentSurvey);
                List<PointObstruction> FARPPoints = BuildFARPPoints(
                        survey, isCurrentSurvey);
                _azmc.UpdateFARP(survey, FARPPolygons, FARPPoints,
                        survey.getSurveyName());
            }
            if (isCurrentSurvey)
                _azmc.drawGalleryIcons(survey.uid);
            VizPrefs.applyToSurvey(survey);
        }
    }

    public ArrayList<PointObstruction> BuildLZPoints(SurveyData survey) {

        ArrayList<PointObstruction> poList = new ArrayList<PointObstruction>();

        // Add anchor points
        if (VizPrefs.get(VizPrefs.LZ_ANCHORS))
            poList.addAll(BuildAnchors(survey, false));

        // stop here if not drawing a landing strip
        if (survey.getType() != AZ_TYPE.LZ)
            return poList;

        LZPointBuilder lpb = new LZPointBuilder(survey);

        // Gradient transverse markers
        if (VizPrefs.get(VizPrefs.LZ_MAX_GTMS))
            poList.addAll(lpb.getGradientMarkers(200, "max"));
        if (VizPrefs.get(VizPrefs.LZ_MIN_GTMS))
            poList.addAll(lpb.getGradientMarkers(1000, "min"));

        // Aircraft marking patterns
        if (VizPrefs.get(VizPrefs.LZ_AMPS))
            poList.addAll(lpb.getAMPs());

        // DCPs
        if (VizPrefs.get(VizPrefs.LZ_DCPS))
            poList.addAll(lpb.getDCPs());

        return poList;
    }

    private List<LineObstruction> BuildFARPPolygons(
            SurveyData survey, boolean isCurrentSurvey) {
        if (_fh == null)
            _fh = new FARPHelper();
        List<LineObstruction> lines = new ArrayList<LineObstruction>();

        // 1 line for aircraft
        ArrayList<PointF> ACOutlineXYList = _fh.getACOutlineXY(
                survey.aircraft, true);
        LineObstruction ACOutline = FARPHelper.BuildLineFromOutline(
                survey.center, ACOutlineXYList);
        ACOutline.filled = isCurrentSurvey;
        ACOutline.uid = survey.uid + "AC";
        ACOutline.type = ATSKConstants.FARP_AC_TYPE;
        lines.add(ACOutline);

        survey.center.course_true = survey.angle;

        if (survey.FAMPoints != null) {
            //should add lines for max/min angles on aircraft for FAM placement
            if (survey.FAMPoints[0] != null
                    && survey.FAMPoints[1] != null) {
                if (isCurrentSurvey)
                    lines.addAll(getAngleLines(survey));
            }

            //1 line from AC to each FAM Cart
            for (int sideIndex = 0; sideIndex < survey.FAMPoints.length; sideIndex++) {
                if (survey.FAMPoints[sideIndex] != null) {
                    double Range = (float) Conversions.CalculateRangem(
                            survey.center.lat, survey.center.lon,
                            survey.FAMPoints[sideIndex].lat,
                            survey.FAMPoints[sideIndex].lon);
                    if (Range < MAX_FARP_RANGE
                            && survey.FAMPoints[sideIndex].visible) {
                        //go to each FAM cart
                        FARPTankerItem tanker = _fh.getFARPTankerItem(
                                survey.aircraft);
                        LineObstruction famLine = AZHelper
                                .getFAMLine(survey, tanker, sideIndex);
                        if (isCurrentSurvey)
                            famLine.addFlag(Constants.FL_LABEL_MEASURE_LINES);
                        lines.add(famLine);

                        LineObstruction FARPRxLine = new LineObstruction();
                        FARPRxLine.type = ATSKConstants.FARP_FAM_TYPE;
                        FARPRxLine.uid = survey.uid + "FAM_RX_LINE"
                                + sideIndex;
                        AZHelper.getRefuelingPoints(sideIndex, survey, tanker,
                                FARPRxLine);
                        if (isCurrentSurvey)
                            FARPRxLine
                                    .addFlag(Constants.FL_LABEL_MEASURE_LINES);
                        lines.add(FARPRxLine);

                    }//beat fam range filter
                }
            }//both sides
        }

        // Obstruction export boundaries
        //lines.add(getExportOutline(survey));

        //1 line from fam cart to each AC
        return lines;
    }

    private List<PointObstruction> BuildFARPPoints(
            SurveyData survey, boolean isCurrentSurvey) {
        List<PointObstruction> ret = new ArrayList<PointObstruction>();
        FARPTankerItem tanker = getTanker(survey.aircraft);
        int side = 0;
        SurveyPoint famPoint = survey.FAMPoints[side];
        if (famPoint == null || !famPoint.visible)
            famPoint = survey.FAMPoints[++side];
        if (!isCurrentSurvey || tanker == null || survey.FAMRxShape == null
                || famPoint == null || !famPoint.visible)
            return ret;
        // Need 3 sets of points: fuel point, fam cart point, refuel points
        SurveyPoint fuelPoint = tanker.getFuelPoint(survey, side == 0);
        SurveyPoint[] rxPoints = AZHelper.getRefuelingPoints(side, survey,
                tanker);
        double fuelAng = Conversions.calculateAngle(fuelPoint, famPoint);
        double angOff = side == 0 ? -90 : 90;

        // Fuel point: PO, water container, fire extinguisher
        PointObstruction po = new PointObstruction(ATSKConstants.FARP_PO,
                Conversions.AROffset(fuelPoint, fuelAng, FARP_ITEM_OFFSET));
        po.setSurveyPoint(Conversions.AROffset(po, fuelAng + angOff,
                FARP_ITEM_OFFSET));
        po.course_true = fuelAng + 180;
        PointObstruction water = new PointObstruction(
                ATSKConstants.FARP_WATER,
                Conversions.AROffset(po, fuelAng, FARP_ITEM_OFFSET));
        water.course_true = fuelAng + 180;
        PointObstruction fire = new PointObstruction(
                ATSKConstants.FARP_FIRE,
                Conversions.AROffset(water, fuelAng, FARP_ITEM_OFFSET));
        fire.course_true = fuelAng + 180;
        ret.add(po);
        ret.add(water);
        ret.add(fire);

        // FAM cart / hose connection: HRS, water container
        PointObstruction hrs = new PointObstruction(ATSKConstants.FARP_HRS,
                Conversions.AROffset(famPoint, fuelAng + angOff,
                        FARP_ITEM_OFFSET));
        hrs.course_true = fuelAng + 180;
        ret.add(hrs);
        if (!survey.FAMRxShape.equals(ATSKConstants.FARP_RX_LAYOUT_RGR)) {
            water = new PointObstruction(ATSKConstants.FARP_WATER,
                    Conversions.AROffset(hrs, fuelAng, FARP_ITEM_OFFSET));
            water.course_true = fuelAng + 180;
            ret.add(water);
        }

        // Refuel points: HDP, fire extinguisher, water container, PO
        PointObstruction hdp;
        for (SurveyPoint rx : rxPoints) {
            if (rx == null || !rx.visible)
                continue;
            SurveyPoint rxOff = Conversions.AROffset(rx,
                    rx.course_true + 180, FARP_ITEM_OFFSET);
            po = new PointObstruction(ATSKConstants.FARP_PO,
                    Conversions.AROffset(rxOff,
                            rx.course_true + angOff, FARP_ITEM_OFFSET));
            po.course_true = rx.course_true + 180;
            water = new PointObstruction(ATSKConstants.FARP_WATER,
                    Conversions.AROffset(rxOff, rx.course_true - angOff,
                            FARP_ITEM_OFFSET));
            water.course_true = rx.course_true + 180;
            fire = new PointObstruction(ATSKConstants.FARP_FIRE,
                    Conversions.AROffset(water, rx.course_true + 180,
                            FARP_ITEM_OFFSET));
            fire.course_true = rx.course_true + 180;
            hdp = new PointObstruction(ATSKConstants.FARP_HDP,
                    Conversions.AROffset(fire, rx.course_true + 180,
                            FARP_ITEM_OFFSET));
            hdp.course_true = rx.course_true + 180;
            ret.add(po);
            ret.add(water);
            ret.add(fire);
            ret.add(hdp);
        }
        int i = 1;
        for (PointObstruction p : ret)
            p.uid = survey.uid + "_" + ATSKConstants.FARP_ITEM + "_" + (i++);
        return ret;
    }

    private List<LineObstruction> getAngleLines(SurveyData survey) {

        List<LineObstruction> lines = new ArrayList<LineObstruction>();
        //get start point of lines...
        FARPTankerItem ti = _fh.getFARPTankerItem(survey.aircraft);
        SurveyPoint fPos;
        if (survey.FAMRxShape != null && survey.FAMRxShape
                .equals(ATSKConstants.FARP_RX_LAYOUT_RGR))
            fPos = Conversions.AROffset(survey.center,
                    survey.angle + 180, ti.rgrWingOffset);
        else
            fPos = Conversions.AROffset(survey.center,
                    survey.angle + 180, ti.FuelPointOffset_m);

        //making 4 lines - 2 lines per side

        double maxLen;
        int famIndex = survey.getActiveFAMIndex();
        if (famIndex > -1 && survey.FAMRxShape != null) {
            maxLen = survey.getFAMDistance()
                    + survey.getHoseLength()
                    + (survey.FAMRxShape
                            .equals(ATSKConstants.FARP_RX_LAYOUT_RGR)
                            ? ti.rgrCenterlineOffset
                            : ti.FuelCenterlineOffset_m);
        } else
            return lines;

        SurveyPoint tr = Conversions.AROffset(fPos,
                survey.angle + ti.StartAngle, maxLen);
        SurveyPoint br = Conversions.AROffset(fPos,
                survey.angle + ti.EndAngle, maxLen);
        SurveyPoint tl = Conversions.AROffset(fPos,
                survey.angle - ti.StartAngle, maxLen);
        SurveyPoint bl = Conversions.AROffset(fPos,
                survey.angle - ti.EndAngle, maxLen);
        LineObstruction TRLine = new LineObstruction();
        TRLine.uid = survey.uid + "TR";
        TRLine.type = ATSKConstants.FARP_FAM_ANGLE_TYPE;
        TRLine.points.add(fPos);
        TRLine.points.add(tr);
        lines.add(TRLine);

        LineObstruction BRLine = new LineObstruction();
        BRLine.uid = survey.uid + "BR";
        BRLine.type = ATSKConstants.FARP_FAM_ANGLE_TYPE;
        BRLine.points.add(fPos);
        BRLine.points.add(br);
        lines.add(BRLine);

        LineObstruction TLLine = new LineObstruction();
        TLLine.uid = survey.uid + "TL";
        TLLine.type = ATSKConstants.FARP_FAM_ANGLE_TYPE;
        TLLine.points.add(fPos);
        TLLine.points.add(tl);
        lines.add(TLLine);

        LineObstruction BLLine = new LineObstruction();
        BLLine.uid = survey.uid + "BL";
        BLLine.type = ATSKConstants.FARP_FAM_ANGLE_TYPE;
        BLLine.points.add(fPos);
        BLLine.points.add(bl);
        lines.add(BLLine);

        return lines;
    }

    private boolean isValid(final double actLength, final double actWidth,
            final double reqLength,
            final double reqWidth) {
        return actLength + THIRD_METER >= reqLength
                && actWidth + THIRD_METER >= reqWidth;
    }

    private List<PointObstruction> BuildAnchors(
            SurveyData survey, boolean centerPoint) {
        List<PointObstruction> pointList = new ArrayList<PointObstruction>();

        // Anchors for fine adjust and long-press move
        int appAnchor = survey.getApproachAnchor();
        int depAnchor = survey.getDepartureAnchor();
        for (int i = ATSKConstants.ANCHOR_APPROACH_LEFT; i <= ATSKConstants.ANCHOR_DEPARTURE_RIGHT; i++) {

            // Circular surveys only have a center anchor
            if (survey.circularAZ && i != ATSKConstants.ANCHOR_CENTER)
                continue;

            // LZ shouldn't have corner anchors
            if (survey.getType() == AZ_TYPE.LZ
                    && i != ATSKConstants.ANCHOR_APPROACH_CENTER
                    && i != ATSKConstants.ANCHOR_DEPARTURE_CENTER)
                continue;

            PointObstruction anchor = new PointObstruction(
                    AZHelper.CalculateAnchorFromAZCenter(
                            survey, survey.center, i));
            anchor.uid = survey.uid + "_"
                    + Constants.POINT_ANCHOR + "_" + i;
            anchor.type = Constants.POINT_ANCHOR;
            anchor.visible = (i == appAnchor || i == depAnchor);

            if (i == ATSKConstants.ANCHOR_CENTER) {
                if (!centerPoint && !survey.circularAZ)
                    continue;
                anchor.visible = true;
            }
            pointList.add(anchor);
        }

        return pointList;
    }

    private List<PointObstruction> BuildAnchors(SurveyData survey) {
        return BuildAnchors(survey, true);
    }

    private List<PointObstruction> BuildDZPoints(SurveyData currentSurvey,
            boolean isCurrentSurvey) {

        List<PointObstruction> DZPointList = new ArrayList<PointObstruction>();

        if (!isCurrentSurvey)
            return DZPointList;

        // Anchor points
        DZPointList.addAll(BuildAnchors(currentSurvey));

        // for non-mission specific points, put the point right in the center.
        boolean customPI = currentSurvey.getMetaBoolean("customPI", false);

        if (!customPI && currentSurvey.circularAZ)
            currentSurvey.cdsPIOffset = currentSurvey.getRadius();

        if (!currentSurvey.circularAZ
                && (customPI || isValid(currentSurvey.getLength(),
                        currentSurvey.width, 914.4, 548.5))) {
            PointObstruction hePoint = new PointObstruction(
                    AZHelper.CalculatePointOfImpact(currentSurvey, "he"));
            hePoint.uid = currentSurvey.uid + "_"
                    + Constants.POINT_PI_HE;
            hePoint.type = Constants.POINT_PI_HE;
            hePoint.remark = Constants.POINT_PI_HE;
            if (currentSurvey.hePIElevation == SurveyPoint.Altitude.INVALID)
                currentSurvey.hePIElevation = (float) (ATSKApplication
                        .getElevation_m_hae(hePoint.lat, hePoint.lon));
            DZPointList.add(hePoint);
        }

        if (!currentSurvey.circularAZ
                && (customPI || isValid(currentSurvey.getLength(),
                        currentSurvey.width, 548.5, 548.5))) {
            PointObstruction perPoint = new PointObstruction(
                    AZHelper.CalculatePointOfImpact(currentSurvey, "per"));
            perPoint.uid = currentSurvey.uid + "_"
                    + Constants.POINT_PI_PER;
            perPoint.type = Constants.POINT_PI_PER;
            perPoint.remark = Constants.POINT_PI_PER;

            // calculate the elevation
            if (currentSurvey.perPIElevation == SurveyPoint.Altitude.INVALID)
                currentSurvey.perPIElevation = (float) (ATSKApplication
                        .getElevation_m_hae(perPoint.lat, perPoint.lon));
            DZPointList.add(perPoint);
        }

        if (customPI || currentSurvey.circularAZ
                || isValid(currentSurvey.getLength(), currentSurvey.width,
                        365.5, 365.5)) {
            if (!customPI && !currentSurvey.circularAZ) {
                DZCapabilities dzreq = _dzParser.getDZCapabilities(
                        currentSurvey.getLength(), currentSurvey.width, 600f,
                        currentSurvey.aircraft);

                if (!currentSurvey.nightDrop) {
                    currentSurvey.cdsPIOffset = dzreq.cds_pi;
                } else {
                    currentSurvey.cdsPIOffset = dzreq.cds_pi_night;
                }
            }

            PointObstruction cdsPoint =
                    new PointObstruction(AZHelper.CalculatePointOfImpact(
                            currentSurvey, "cds"));
            cdsPoint.uid = currentSurvey.uid + "_"
                    + Constants.POINT_PI_CDS;
            cdsPoint.type = Constants.POINT_PI_CDS;

            if (currentSurvey.circularAZ) {
                cdsPoint.remark = "PI";
            } else
                cdsPoint.remark = Constants.POINT_PI_CDS;

            // calculate the elevation
            if (currentSurvey.cdsPIElevation == SurveyPoint.Altitude.INVALID)
                currentSurvey.cdsPIElevation = (float) (ATSKApplication
                        .getElevation_m_hae(cdsPoint.lat, cdsPoint.lon));
            DZPointList.add(cdsPoint);
        }

        _azpc.UpdateAZ(currentSurvey, "PISElevation", false);

        // Point of Origin (optional)
        PointObstruction POPoint = new PointObstruction(
                currentSurvey.pointOfOrigin);
        POPoint.uid = currentSurvey.uid + "_"
                + Constants.POINT_PO;

        POPoint.remark = "POO";

        POPoint.type = Constants.POINT_PO;
        DZPointList.add(POPoint);

        return DZPointList;
    }

    private ArrayList<LineObstruction> BuildLZPolygons(
            SurveyData currentSurvey, List<PointObstruction> points) {

        ArrayList<LineObstruction> Polygons = new ArrayList<LineObstruction>();

        //we want to add the extra polygons, even if we aren't current, just so the id's get in the map
        Polygons.add(PolygonHelper.getLeftShoulder(currentSurvey));
        Polygons.add(PolygonHelper.getRightShoulder(currentSurvey));

        Polygons.add(PolygonHelper.getLeftGradedArea(currentSurvey));
        Polygons.add(PolygonHelper.getRightGradedArea(currentSurvey));

        Polygons.add(PolygonHelper.getLeftMaintainedArea(currentSurvey));
        Polygons.add(PolygonHelper.getRightMaintainedArea(currentSurvey));

        Polygons.add(PolygonHelper.getApproachOverrunArea(currentSurvey));
        Polygons.add(PolygonHelper.getDepartureOverrunArea(currentSurvey));
        Polygons.addAll(PolygonHelper.getApproachOverrunArrows(currentSurvey));
        Polygons.addAll(PolygonHelper.getDepartureOverrunArrows(currentSurvey));

        Polygons.add(PolygonHelper.getClearApproachTrapezoid(currentSurvey));
        Polygons.add(PolygonHelper.getClearDepartureTrapezoid(currentSurvey));

        Polygons.add(PolygonHelper.getInnerApproachTrapezoid(currentSurvey));
        Polygons.add(PolygonHelper.getOuterApproachTrapezoid(currentSurvey));

        Polygons.add(PolygonHelper.getInnerDepartureTrapezoid(currentSurvey));
        Polygons.add(PolygonHelper.getOuterDepartureTrapezoid(currentSurvey));

        //Polygons.add(getExportOutline(currentSurvey));

        LineObstruction CenterLine = new LineObstruction();
        CenterLine.type = ATSKConstants.LZ_CENTER_LINE;
        CenterLine.uid = currentSurvey.uid + "CL"
                + ATSKConstants.LZ_CENTER_LINE;
        SurveyPoint StartCenter = AZHelper.CalculateCenterOfEdge(currentSurvey,
                true);
        SurveyPoint EndCenter = AZHelper.CalculateCenterOfEdge(currentSurvey,
                false);
        CenterLine.points.add(EndCenter);
        CenterLine.points.add(StartCenter);
        Polygons.add(CenterLine);

        //draw overruns

        if (currentSurvey.aDisplacedThreshold > 0) {
            LineObstruction ApproachDTLine = new LineObstruction();
            //ApproachDTLine.filled = true;
            ApproachDTLine.uid = currentSurvey.uid + "APP"
                    + ATSKConstants.DISPLACED_THRESHHOLD;
            ApproachDTLine.type = ATSKConstants.DISPLACED_THRESHHOLD;

            // threshold line for the approach
            SurveyPoint CenterOfDispThresh = AZHelper
                    .CalculateCenterOfEdge(
                            currentSurvey.angle,
                            2 * ((currentSurvey.getLength(false) / 2.0)
                            - currentSurvey.aDisplacedThreshold),
                            currentSurvey.center, true);
            //offset this the full width of LZ???
            double Left[] = Conversions.AROffset(CenterOfDispThresh.lat,
                    CenterOfDispThresh.lon, currentSurvey.angle + 90,
                    currentSurvey.width / 2);
            ApproachDTLine.points.add(new SurveyPoint(Left[0], Left[1]));
            double Right[] = Conversions.AROffset(CenterOfDispThresh.lat,
                    CenterOfDispThresh.lon, currentSurvey.angle - 90,
                    currentSurvey.width / 2);
            ApproachDTLine.points.add(new SurveyPoint(Right[0], Right[1]));
            Polygons.add(ApproachDTLine);
        }
        if (currentSurvey.dDisplacedThreshold > 0) {
            LineObstruction DepartureDTLine = new LineObstruction();
            //DepartureDTLine.filled = true;
            DepartureDTLine.uid = currentSurvey.uid + "DEP"
                    + ATSKConstants.DISPLACED_THRESHHOLD;
            DepartureDTLine.type = ATSKConstants.DISPLACED_THRESHHOLD;

            // threshold line for the departure
            SurveyPoint CenterOfDispThresh = AZHelper
                    .CalculateCenterOfEdge(
                            currentSurvey.angle,
                            2 * ((currentSurvey.getLength(false) / 2.0)
                            - currentSurvey.dDisplacedThreshold),
                            currentSurvey.center, false);
            //offset this the full width of LZ???
            double Left[] = Conversions.AROffset(CenterOfDispThresh.lat,
                    CenterOfDispThresh.lon, currentSurvey.angle + 90,
                    currentSurvey.width / 2);
            DepartureDTLine.points.add(new SurveyPoint(Left[0], Left[1]));
            double Right[] = Conversions.AROffset(CenterOfDispThresh.lat,
                    CenterOfDispThresh.lon, currentSurvey.angle - 90,
                    currentSurvey.width / 2);
            DepartureDTLine.points.add(new SurveyPoint(Right[0], Right[1]));
            Polygons.add(DepartureDTLine);
        }

        // Convert any meta-points to shapes
        for (int i = 0; i < points.size(); i++) {
            PointObstruction po = points.get(i);
            if (po.type.equals(Constants.PO_AMP_BOX)) {
                LineObstruction lo = new LineObstruction();
                lo.uid = po.uid;
                lo.type = po.type;
                lo.points.addAll(Arrays.asList(po.getCorners(true)));
                lo.closed = true;
                lo.filled = true;
                Polygons.add(lo);
                points.remove(i--);
            }
        }

        return Polygons;
    }

    /**
     * Get a red outline of the export boundaries for obstructions and gradients
     * @param survey Survey data
     * @return Polygon
     */
    private LineObstruction getExportOutline(SurveyData survey) {
        List<SurveyPoint> corners = ATSKTransferPackage.getExportBounds(survey);
        LineObstruction lo = new LineObstruction();
        lo.points.addAll(corners);
        lo.points.add(corners.get(0));
        lo.type = ATSKConstants.APRON_ROUTE_BOUNDARY_TYPE;
        lo.closed = true;
        lo.filled = false;
        return lo;
    }

    private ArrayList<LineObstruction> BuildAZPolygons(
            SurveyData survey, boolean isCurrentSurvey) {
        ArrayList<LineObstruction> polygons = new ArrayList<LineObstruction>();

        if (!survey.circularAZ)
            polygons.add(PolygonHelper.getAZOutline(survey, isCurrentSurvey));

        //if this is the current survey show the dots if not, return an empty.
        if (!checkSurveyCurrent(survey))
            return polygons;

        if (survey.getType() == AZ_TYPE.HLZ) {
            //need approach and depature lines if HLZ is current Survey
            polygons.add(PolygonHelper.getHLZApproachLine(survey));
            polygons.add(PolygonHelper.getHLZDepartureLine(survey));
        } else if (survey.getType() == AZ_TYPE.DZ) {
            polygons.add(PolygonHelper.getDZHeadingLine(survey));
        }
        //polygons.add(getExportOutline(survey));

        return polygons;
    }

    private boolean checkSurveyCurrent(SurveyData survey) {

        String currentSurveyUID = _azpc.getSetting(
                ATSKConstants.CURRENT_SURVEY,
                TAG);

        return currentSurveyUID.equals(survey.uid);
    }

    private class AZContentObserver extends ContentObserver {
        public AZContentObserver(Handler handler) {
            super(handler);
        }

        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);

            String surveyUID = GetUIDFromURI(uri);
            UpdateAZ(surveyUID);

            ATSKFragmentManager afm = ATSKMapComponent.getATSKFM();
            if (afm != null)
                afm.notifySurveyUpdate(surveyUID);
        }

        private String GetUIDFromURI(Uri uri) {
            String URIString = uri.toString();

            String[] Tokens = URIString.split("/");

            return Tokens[Tokens.length - 1];
        }
    }

    // PO SparseArray with extra debugging parameters
    static private class DCPArray extends SparseArray<PointObstruction> {
        public void put(String semantic, int key, PointObstruction value) {
            if (get(key) != null)
                Log.w(TAG, "Overwriting DCP @ " + key + " [" + semantic + "]");
            super.put(key, value);
        }
    }

    private static void showToast(final String msg) {
        MapView.getMapView().post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MapView.getMapView().getContext(),
                        msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Set survey length while addressing possible clamping
     * @param survey Survey data
     * @param length Length in meters
     * @param usable True for usable length only
     */
    public static void setLength(SurveyData survey, double length,
            boolean usable) {
        if (length > SurveyData.MAX_LENGTH)
            showToast("Clamping length to " + Unit.METER
                    .format(SurveyData.MAX_LENGTH, Unit.KILOMETER));
        survey.setLength(length, usable);
    }

    /**
     * Set survey radius while addressing possible clamping
     * @param survey Survey data
     * @param radius Radius in meters
     */
    public static void setRadius(SurveyData survey, double radius) {
        if (survey.circularAZ) {
            if (radius > SurveyData.MAX_LENGTH / 2)
                showToast("Clamping radius to " + Unit.METER
                        .format(SurveyData.MAX_LENGTH / 2, Unit.KILOMETER));
            survey.setRadius(radius);
        }
    }

    /**
     * Set survey width while addressing possible clamping
     * @param survey Survey data
     * @param width Width in meters
     */
    public static void setWidth(SurveyData survey, double width) {
        if (width > SurveyData.MAX_LENGTH)
            showToast("Clamping width to " + Unit.METER
                    .format(SurveyData.MAX_LENGTH, Unit.KILOMETER));
        survey.setWidth(width);
    }

    public static void setOverrunLength(SurveyData survey, double length,
            boolean approach) {
        if (length > SurveyData.MAX_LENGTH / 2)
            showToast("Clamping overrun length to " + Unit.METER
                    .format(SurveyData.MAX_LENGTH / 2, Unit.KILOMETER));
        survey.setOverrunLength(length, approach);
    }

    private static class LZPointBuilder {

        private final SurveyData _survey;
        private final double _totalM, _totalFt, _halfLen;

        public LZPointBuilder(SurveyData survey) {
            _survey = survey;
            double len = _survey.getLength(false);
            _totalM = MathUtils.clamp(len, 0, 30000);
            _totalFt = _totalM * Conversions.M2F;
            _halfLen = _survey.getLength(true) / 2;
        }

        /**
         * Calculate point location from start of runway
         * @param distFt Distance from start of runway in feet
         * @return Destination survey point
         */
        private SurveyPoint calcOffset(double distFt) {
            double[] ret = Conversions.AROffset(_survey.center.lat,
                    _survey.center.lon,
                    _survey.angle, (distFt / Conversions.M2F) - _halfLen
                            + _survey.edges.ApproachOverrunLength_m);
            return new SurveyPoint(ret[0], ret[1]);
        }

        /**
         * Calculate point location from starting point
         * @param start Starting point
         * @param ang Angle relative to survey angle
         * @param distFt Distance in feet
         * @return Destination survey point
         */
        private SurveyPoint calcOffset(
                SurveyPoint start, double ang, double distFt) {
            double[] ret = Conversions.AROffset(start.lat, start.lon,
                    _survey.angle + ang, distFt / Conversions.M2F);
            return new SurveyPoint(ret[0], ret[1]);
        }

        private PointObstruction createPoint(
                SurveyPoint loc, String type, int num, String prefix) {
            PointObstruction po = new PointObstruction(loc);
            po.uid = _survey.uid + "_"
                    + (prefix != null ? prefix + "_" : "")
                    + type + "_" + (num - 1);
            po.type = type;
            po.remark = String.valueOf(num);
            return po;
        }

        private PointObstruction createPoint(
                SurveyPoint loc, String type, int num) {
            return createPoint(loc, type, num, null);
        }

        /**
         * Build gradient markers for a given survey
         * @param distFt The distance between each marker in feet
         * @param prefix The prefix to assign to each point
         * @return List of gradient markers
         */
        public List<PointObstruction> getGradientMarkers(int distFt,
                String prefix) {
            List<PointObstruction> ret = new ArrayList<PointObstruction>();
            int pointNum = 1;
            for (int i = 0; i <= _totalFt; i += distFt) {
                // Create GTM
                ret.add(createPoint(calcOffset(i), Constants.PO_GTM,
                        pointNum, prefix));

                // add last marker to end of runway
                int lengthDiff = (int) (_totalFt - i);
                if (lengthDiff < distFt && lengthDiff > 0)
                    i = (int) _totalFt - distFt;

                pointNum++;
            }

            return ret;
        }

        /**
         * Build aircraft marking pattern based on survey data
         * @return List of markers within pattern
         */
        public List<PointObstruction> getAMPs() {
            List<PointObstruction> ret = new ArrayList<PointObstruction>();
            String ampType = _survey.getMetaString("ampType", null);
            String ac = _survey.aircraft;
            if (ampType == null || ac == null)
                return ret;

            // Aircraft marking index
            int ampNum = (ampType.startsWith("AMP-3") ? 3 :
                    (ampType.startsWith("AMP-2") ? 2 : 1));
            // Is fixed wing type
            boolean standard = ac.equals(ATSKConstants.AC_C130)
                    || ac.equals(ATSKConstants.AC_C17);
            boolean ltfw = _survey.surveyIsLTFW() || ampType.contains("LTFW");
            boolean night = ampType.contains("Night");

            // Distance of overrun boxes
            int boxSize = ltfw && ampNum > 1 ? 200 : (standard ? 500 : 1000);
            if (ampType.contains("1000’"))
                boxSize = 1000;
            int distFt = 500;
            // Offset of edge panel
            int edgeOffFt = night ? 0 : 5;
            // Distance from center line to edge
            double widthOff = (_survey.width / 2) * Conversions.M2F;
            // Total number of sets across length
            int setNum = 0;
            int totalSets = (int) Math.ceil(_totalFt / distFt) + 1;

            List<SurveyPoint> points = new ArrayList<SurveyPoint>();
            int pNum = 1;
            // Only AMP-1 uses the intermediate lights
            for (int i = 0; i <= _totalFt; i += distFt) {
                SurveyPoint cen = calcOffset(i);

                // Edge panels
                if (setNum <= 0 || setNum >= totalSets - 1) {
                    points.add(calcOffset(cen, 90, widthOff + edgeOffFt));
                    points.add(calcOffset(cen, -90, widthOff + edgeOffFt));
                    if (ampNum == 1) {
                        points.add(calcOffset(cen, 90, widthOff + edgeOffFt + 6));
                        points.add(calcOffset(cen, -90, widthOff + edgeOffFt
                                + 6));
                    }
                }

                // add last marker to end of runway
                int lengthDiff = (int) (_totalFt - i);
                if (lengthDiff < distFt && lengthDiff > 0)
                    i = (int) _totalFt - distFt;
                setNum++;
            }
            // AMP-2/3 uses the approach/departure overrun boxes
            // These are meta-points (drawn as shapes later)
            for (int i = 1; i <= 2; i++) {
                double off = (double) boxSize / 2.0d;
                if (i != 1)
                    off += _totalFt - boxSize;
                PointObstruction box = createPoint(calcOffset(off),
                        Constants.PO_AMP, pNum++);
                box.width = _survey.width;
                box.length = boxSize / Conversions.M2F;
                box.course_true = _survey.angle;
                box.type = Constants.PO_AMP_BOX;
                ret.add(box);
            }

            // Create point obstructions
            for (SurveyPoint sp : points) {
                // Leave out last 2 lights on AMP-3 Night
                if (ampNum == 3 && night && pNum >= points.size() - 1)
                    break;
                PointObstruction po = createPoint(sp, Constants.PO_AMP, pNum);
                po.type = night && ampNum != 2 ? Constants.PO_AMP_LIGHT
                        : (!night && (ampNum == 1 && pNum <= 4 || pNum <= 2)
                                ? Constants.PO_AMP_PANEL_ORANGE
                                : Constants.PO_AMP_PANEL);
                if (night && ampNum == 2 && pNum == 2)
                    po.type = Constants.PO_AMP_RCL;
                po.remark = "";
                ret.add(po);
                pNum++;
            }

            // Strobe light
            if (night) {
                PointObstruction po = createPoint(calcOffset(_totalFt),
                        Constants.PO_AMP, pNum);
                po.type = Constants.PO_AMP_STROBE;
                po.remark = "";
                ret.add(po);
            }

            return ret;
        }

        /**
         * Build DCPs given survey data
         * @return List of DCP markers
         */
        public List<PointObstruction> getDCPs() {
            List<PointObstruction> ret = new ArrayList<PointObstruction>();
            DCPArray dcps = new DCPArray();

            // edge points along sides and center line
            int pointNum = 1;

            // approach overrun center
            double[] latlon = Conversions.AROffset(_survey.center.lat,
                    _survey.center.lon,
                    _survey.angle + 180, _halfLen -
                            _survey.edges.ApproachOverrunLength_m / 2);
            dcps.put("overrun", pointNum++, new PointObstruction(
                    new SurveyPoint(
                            latlon[0], latlon[1])));

            double tenFt = (10.0f / Conversions.M2F);
            double edgeOffset = Math.max((_survey.width / 2.0f) - tenFt, tenFt);

            for (int i = 0; i <= _totalFt; i += 500) {
                // Last set
                int lengthDiff = (int) (_totalFt - i);
                if (lengthDiff < 100 && _totalFt >= 100) {
                    pointNum++;
                    if (lengthDiff > 0)
                        i = (int) _totalFt;
                    lengthDiff = 0;
                }

                // center edge point
                PointObstruction center = new PointObstruction(calcOffset(i));
                dcps.put("3P edge", pointNum + 1, center);

                // left edge point
                latlon = Conversions.AROffset(center.lat, center.lon,
                        _survey.angle - 90, edgeOffset);
                dcps.put("3P edge", pointNum,
                        new PointObstruction(new SurveyPoint(latlon[0],
                                latlon[1])));

                // right edge point
                latlon = Conversions.AROffset(center.lat, center.lon,
                        _survey.angle + 90, edgeOffset);
                dcps.put("3P edge", pointNum + 2,
                        new PointObstruction(new SurveyPoint(latlon[0],
                                latlon[1])));

                // add one more set
                if (lengthDiff < 500 && lengthDiff > 0)
                    i = (int) _totalFt - 500;

                pointNum += 3 + (MathUtils.clamp(lengthDiff, 0, 500) / 250);

                // Fix for 250-300 ft, 1250-1300ft, etc.
                if (lengthDiff >= 250 && lengthDiff < 300
                        && (_totalFt - lengthDiff) % 1000 == 0) {
                    pointNum--;
                }
            }

            // get highest point num
            int highest = 0;
            for (int i = 0; i < dcps.size(); i++)
                highest = Math.max(highest, dcps.keyAt(i));

            // stopping point
            if (_totalFt > 100) {
                dcps.put("stopping", highest - 3, new PointObstruction(
                        calcOffset(_totalFt - 50)));
            }

            // departure overrun center
            latlon = Conversions.AROffset(_survey.center.lat,
                    _survey.center.lon, _survey.angle, _halfLen
                            - _survey.edges.DepartureOverrunLength_m / 2);
            dcps.put("overrun", highest + 1, new PointObstruction(
                    new SurveyPoint(
                            latlon[0], latlon[1])));

            // center line offset points
            // start at 5 to account for approach overrun center + first set of 3
            pointNum = 5;
            int offset = 0;
            for (int i = 200; i <= (int) (_totalFt - 100); i += 200) {
                if (offset > 0 && offset % 2 == 0)
                    pointNum += 3;
                if (offset == 4) {
                    offset = 0;
                    continue;
                }
                // offset 15ft from center line (commented out as per item 101 06APR2016 feedback)
                /*latlon = Conversions.AROffset(latlon[0], latlon[1], ang
                    + (offset % 2 == 0 ? 90 : -90), (15.0f / Conversions.M2F));*/
                dcps.put("CL offset", pointNum++, new PointObstruction(
                        calcOffset(i)));
                offset++;
            }

            // style settings
            for (int i = 0; i < dcps.size(); i++) {
                int ind = dcps.keyAt(i);
                PointObstruction po = dcps.valueAt(i);
                po.uid = _survey.uid + "_" + Constants.PO_LZ_DCP + "_" + i;
                po.type = Constants.PO_LZ_DCP;
                po.remark = "" + ind;
                ret.add(po);
            }
            return ret;
        }
    }
}
