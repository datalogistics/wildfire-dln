
package com.gmeci.atsk.obstructions;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.PreferenceManager;

import com.atakmap.android.editableShapes.EditablePolyline;
import com.atakmap.android.icons.UserIcon;
import com.atakmap.android.toolbars.RangeAndBearingEndpoint;
import com.atakmap.android.user.icon.SpotMapPalletFragment;
import com.gmeci.atsk.MapHelper;
import com.gmeci.atsk.map.ATSKLabel;
import com.gmeci.atsk.map.ATSKLineLeader;
import com.gmeci.atsk.map.ATSKRangeAndBearingCircle;
import com.gmeci.atsk.map.ATSKRangeAndBearingLine;
import com.gmeci.atsk.map.ATSKShape;
import com.gmeci.atsk.map.ATSKVehicle;
import com.gmeci.atsk.resources.ATSKApplication;
import com.gmeci.atskservice.resolvers.ObstructionProviderClient;
import com.atakmap.android.atsk.plugin.R;
import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Marker;
import com.atakmap.coremap.maps.assets.Icon;
import com.atakmap.coremap.maps.conversion.EGM96;
import com.atakmap.coremap.maps.coords.Altitude;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.gmeci.conversions.Conversions;
import com.gmeci.core.ATSKConstants;
import com.gmeci.core.ATSKIntentConstants;
import com.gmeci.core.LineObstruction;
import com.gmeci.core.PointObstruction;
import com.gmeci.core.SurveyPoint;
import com.gmeci.atsk.ATSKATAKConstants;
import com.gmeci.atsk.map.ATSKDrawingTool;
import com.gmeci.atsk.map.ATSKMarker;
import com.gmeci.atsk.resources.ATSKBaseFragment;
import com.gmeci.constants.Constants;
import com.gmeci.helpers.LineHelper;
import com.atakmap.coremap.log.Log;

import com.gmeci.conversions.Conversions.Unit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import com.atakmap.coremap.locale.LocaleUtil;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class MapObstructionController {

    private final static String TAG = "ATAKObstructionController";

    private final Map<String, Integer> _icons = new HashMap<String, Integer>();
    private final Set<String> _obsTypes = new HashSet<String>();
    private final MapView _mapView;
    private MapGroup _mapGroup;
    private final ATSKDrawingTool _drawing;
    private final SharedPreferences _prefs;

    public final ObstructionProviderClient _opc;

    public MapObstructionController(final MapView mapView) {
        _mapView = mapView;
        _mapGroup = mapView.getRootGroup().findMapGroup(
                ATSKATAKConstants.ATSK_MAP_GROUP_OBS);
        if (_mapGroup == null)
            _mapGroup = mapView.getRootGroup().addGroup(
                    ATSKATAKConstants.ATSK_MAP_GROUP_OBS);
        _drawing = new ATSKDrawingTool(_mapView, _mapGroup);
        _prefs = PreferenceManager.getDefaultSharedPreferences(_mapView
                .getContext());
        _prefs.registerOnSharedPreferenceChangeListener(_prefsListener);
        _opc = new ObstructionProviderClient(mapView.getContext());
        setupImageMap();
        _opc.Start();

    }

    public void dispose() {
        _prefs.unregisterOnSharedPreferenceChangeListener(_prefsListener);
        _opc.Stop();
    }

    private void setupImageMap() {

        Collections.addAll(_obsTypes, Constants.LINE_TYPES);

        Collections.addAll(_obsTypes, Constants.POINT_TYPES);

        Collections.addAll(_obsTypes, Constants.AREA_TYPES);
        _icons.put(Constants.PO_BLANK, R.drawable.po_generic_point);
        _icons.put(Constants.PO_RED, R.drawable.po_generic_point);
        _icons.put(Constants.PO_BLACK, R.drawable.po_generic_point);
        _icons.put(Constants.PO_LASER, R.drawable.laser_map);
        _icons.put(Constants.PO_AIRFIELD_INSTRUMENT,
                R.drawable.po_airfield_instrument_tv);
        _icons.put(Constants.LO_ARRESTING_GEAR,
                R.drawable.po_arresting_gear_tv);
        _icons.put(Constants.PO_ANTENNA, R.drawable.po_antenna_tv);
        _icons.put(Constants.LO_BARRIER, R.drawable.po_barrier_tv);
        _icons.put(Constants.PO_BERMS, R.drawable.po_berms_tv);
        _icons.put(Constants.PO_BUILDING, R.drawable.po_building_tv);
        _icons.put(Constants.PO_BUSH, R.drawable.po_bush_tv);
        _icons.put(Constants.PO_CRATER, R.drawable.po_crater_tv);
        _icons.put(Constants.PO_DUMPSTER, R.drawable.po_dumpster_tv);
        _icons.put(Constants.PO_FIRE_HYDRANT, R.drawable.po_fire_hydrant_tv);
        _icons.put(Constants.PO_FLAGPOLE, R.drawable.po_flagpole_tv);
        _icons.put(Constants.PO_FUEL_TANK, R.drawable.po_fuel_tank_tv);
        _icons.put(Constants.PO_HVAC_UNIT, R.drawable.po_hvac_unit_tv);
        _icons.put(Constants.PO_POLE, R.drawable.po_pole_tv);
        _icons.put(Constants.PO_LIGHT_POLE, R.drawable.po_light_pole_tv);
        _icons.put(Constants.PO_LIGHT, R.drawable.po_light_tv);
        _icons.put(Constants.PO_MOUND, R.drawable.po_mound_tv);
        _icons.put(Constants.AO_POOL, R.drawable.po_pool_tv);
        _icons.put(Constants.PO_ROTATING_BEACON,
                R.drawable.po_rotating_beacon_tv);
        _icons.put(Constants.PO_TREE, R.drawable.po_tree_tv);
        _icons.put(Constants.PO_SAT_DISH, R.drawable.po_sat_dish_tv);
        _icons.put(Constants.PO_TRANSFORMER, R.drawable.po_transformer_tv);
        _icons.put(Constants.PO_WINDSOCK, R.drawable.po_windsock_tv);
        _icons.put(Constants.PO_WXVANE, R.drawable.po_wxvane_tv);
        _icons.put(Constants.PO_GENERIC_POINT, R.drawable.po_generic_point);
        _icons.put(Constants.PO_LABEL, R.drawable.po_label);
        _icons.put(Constants.PO_RAB_LINE, R.drawable.po_rab_line);
        _icons.put(Constants.PO_RAB_CIRCLE, R.drawable.po_generic_point);
        _icons.put(Constants.PO_CBR, R.drawable.cbr);
        _icons.put(Constants.PO_CBR_HIDDEN, R.drawable.cbr_hidden);
        _icons.put(Constants.PO_SIGN, R.drawable.po_sign_tv);
        _icons.put(Constants.PO_PEAK, R.drawable.po_peak_tv);
        _icons.put(Constants.PO_LEDGE, R.drawable.po_ledge_tv);
        _icons.put(Constants.PO_PYLON, R.drawable.po_pylon_tv);

        _icons.put(Constants.DISTRESS_DUST + "_" + 0,
                R.drawable.sd_dust_green);
        _icons.put(Constants.DISTRESS_JET_EROSION + "_"
                + 0, R.drawable.sd_jet_blast_erosion_green);
        _icons.put(Constants.DISTRESS_LOOSE_AGG + "_"
                + 0, R.drawable.sd_aggregate_green);
        _icons.put(
                Constants.DISTRESS_POTHOLE + "_" + 0,
                R.drawable.sd_pothole_green);
        _icons.put(Constants.DISTRESS_ROLLING_RESISTANT
                + "_" + 0, R.drawable.sd_rolling_resist_green);
        _icons.put(Constants.DISTRESS_RUTS + "_" + 0,
                R.drawable.sd_ruts_green);
        _icons.put(Constants.DISTRESS_STABLE_FAILURE
                + "_" + 0, R.drawable.sd_stabilized_layer_failure_green);

        _icons.put(Constants.DISTRESS_DUST + "_" + 1,
                R.drawable.sd_dust_yellow);
        _icons.put(Constants.DISTRESS_JET_EROSION + "_"
                + 1, R.drawable.sd_jet_blast_erosion_yellow);
        _icons.put(Constants.DISTRESS_LOOSE_AGG + "_"
                + 1, R.drawable.sd_aggregate_yellow);
        _icons.put(
                Constants.DISTRESS_POTHOLE + "_" + 1,
                R.drawable.sd_pothole_yellow);
        _icons.put(Constants.DISTRESS_ROLLING_RESISTANT
                + "_" + 1, R.drawable.sd_rolling_resist_yellow);
        _icons.put(Constants.DISTRESS_RUTS + "_" + 1,
                R.drawable.sd_ruts_yellow);
        _icons.put(Constants.DISTRESS_STABLE_FAILURE
                + "_" + 1, R.drawable.sd_stabilized_layer_failure_yellow);

        _icons.put(Constants.DISTRESS_DUST + "_" + 2,
                R.drawable.sd_dust_red);
        _icons.put(Constants.DISTRESS_JET_EROSION + "_"
                + 2, R.drawable.sd_jet_blast_erosion_red);
        _icons.put(Constants.DISTRESS_LOOSE_AGG + "_"
                + 2, R.drawable.sd_aggregate_red);
        _icons.put(
                Constants.DISTRESS_POTHOLE + "_" + 2,
                R.drawable.sd_pothole_red);
        _icons.put(Constants.DISTRESS_ROLLING_RESISTANT
                + "_" + 2, R.drawable.sd_rolling_resist_red);
        _icons.put(Constants.DISTRESS_RUTS + "_" + 2,
                R.drawable.sd_ruts_red);
        _icons.put(Constants.DISTRESS_STABLE_FAILURE
                + "_" + 2, R.drawable.sd_stabilized_layer_failure_red);

        _icons.put(Constants.GRADIENT_MISSING_DATA,
                R.drawable.po_l_gradient_hole);
        _icons.put(Constants.GRADIENT_STEEP_SEGMENT,
                R.drawable.po_l_gradient_bad);
        _icons.put(Constants.GRADIENT_STEEP_OVERALL,
                R.drawable.po_l_gradient_bad);

    }

    public boolean AddNewObstruction(PointObstruction po) {
        drawPointObstruction(po);
        return true;
    }

    public boolean UpdatePoint(String uID, PointObstruction existingPoint) {
        drawPointObstruction(existingPoint);
        return true;
    }

    public boolean RemovePoint(String group, String uID) {
        removeMapItem(group, uID);
        return true;
    }

    public boolean RemoveApron(String Group, String uID) {

        MapGroup apronGroup = _mapGroup.findMapGroup(uID);

        if (apronGroup != null) {
            apronGroup.clearGroups();
            apronGroup.clearItems();
        }

        return false;
    }

    public boolean RemoveGradient(String group, String uID) {
        return false;
    }

    private void removeMapItem(String group, String uID) {
        MapItem remove = _mapGroup.findItem("uid", uID);
        if (remove != null)
            _mapGroup.removeItem(remove);
    }

    public boolean UpdateApron(String group, String uID,
            List<LineObstruction> ACOUtlines, LineObstruction ApronOutline) {
        MapGroup apronGroup = _mapGroup.findMapGroup(uID);

        if (apronGroup != null) {
            apronGroup.clearGroups();
            apronGroup.clearItems();
        } else
            apronGroup = _mapGroup.addGroup(uID);

        drawApron(apronGroup, uID, ACOUtlines, ApronOutline);

        return false;
    }

    private synchronized void drawApron(final MapGroup apronGroup,
            final String uID,
            List<LineObstruction> acoUtlines, LineObstruction apronOutline) {

        if (apronOutline != null)
            _drawing.drawParkingLine(apronGroup,
                    MapHelper
                            .convertSurveyPoint2GeoPoint(apronOutline.points),
                    apronOutline.uid,
                    LineHelper.getLineColor(apronOutline.type),
                    true);

        for (LineObstruction lo : acoUtlines)
            _drawing.drawParkingAircraft(apronGroup,
                    MapHelper.convertSurveyPoint2GeoPoint(lo.points),
                    lo.uid,
                    LineHelper.getLineColor(lo.type));

    }

    private boolean shouldLabel(final String type) {
        return _obsTypes.contains(type);
    }

    private void drawPointObstruction(final PointObstruction po) {

        // Vehicles
        if (po.group != null && po.group.equals(ATSKConstants.VEHICLE_GROUP)) {
            ATSKVehicle vo = ATSKVehicle.find(po.uid);
            if (vo == null)
                vo = new ATSKVehicle(po.uid);
            vo.setup(po, false);
            return;
        }

        GeoPoint point = MapHelper.convertSurveyPoint2GeoPoint(po);
        ATSKMarker marker = ATSKMarker.find(po.uid);
        boolean existing = marker != null;
        boolean distress = po.group != null
                && po.group.equals(ATSKConstants.DISTRESS_GROUP);
        if (po.type.equals(Constants.PO_LABEL)) {
            // Label only
            if (!existing)
                marker = new ATSKLabel(ATSKConstants
                        .CURRENT_SCREEN_OBSTRUCTION, po);
            else
                marker.setObstruction(po);
            marker.setMetaString(UserIcon.IconsetPath,
                    SpotMapPalletFragment.LABEL_ONLY_ICONSETPATH);
        } else if (po.type.equals(Constants.PO_RAB_CIRCLE)) {
            // R&B Circle
            if (!existing)
                marker = new ATSKRangeAndBearingCircle(po);
            else
                marker.setObstruction(po);
            marker.setIcon(getIconFromType(po.type, po.uid));
        } else {
            // Regular point obstruction
            if (!existing)
                marker = new ATSKMarker(distress ?
                        ATSKConstants.CURRENT_SCREEN_GRADIENT :
                        ATSKConstants.CURRENT_SCREEN_OBSTRUCTION, po);
            else
                marker.setObstruction(po);
            marker.setIcon(getIconFromType(po.type, po.uid));
        }

        if (po.group == null)
            po.group = ATSKConstants.DEFAULT_GROUP;

        boolean temp = po.uid.equals(ATSKConstants.TEMP_POINT_UID);
        marker.setPoint(point);
        marker.setMarkerHitBounds(-30, -30, 30, 30);
        marker.setMetaString("group", po.group);
        marker.setMetaBoolean("movable", !temp);
        marker.setMetaBoolean("removable", true);
        marker.setMetaBoolean("editable", true);
        marker.setMetaString("obsName", po.type);
        marker.setMetaString("remarks", po.remark);

        setLabel(po, marker);
        marker.setMetaBoolean("ignoreMenu", temp);

        marker.setMetaString(
                ATSKIntentConstants.OB_TYPE_SELECTED,
                ATSKIntentConstants.OB_STATE_POINT);
        marker.setMetaString(ATSKIntentConstants.OB_MENU_GROUP,
                po.group);

        if (po.uid.equals(ATSKBaseFragment.TEMP_LASER_UID)) {
            Marker lrfMarker = new Marker(point, po.uid);
            lrfMarker.setIcon(getIconFromType(po.type,
                    po.uid));
            lrfMarker.setMetaString("group", po.group);
            lrfMarker.setMetaBoolean("movable", false);
            lrfMarker.setMetaBoolean("removable", true);
            lrfMarker.setMetaBoolean("editable", true);
            lrfMarker.setMetaBoolean("ignoreMenu", true);

            _drawing.drawMapItem(lrfMarker);
            return;
        }

        if (!existing)
            _drawing.drawMapItem(marker);
    }

    public void drawLineObstruction(LineObstruction lo, GeoPoint[] points) {

        if (points == null || points.length < 1)
            return;

        // Range and bearing lines
        if (lo.type.equals(Constants.PO_RAB_LINE)) {
            RangeAndBearingEndpoint pt1 = new RangeAndBearingEndpoint(
                    points[0], lo.uid + "_pt1");
            RangeAndBearingEndpoint pt2 = new RangeAndBearingEndpoint(
                    points[1], lo.uid + "_pt2");
            ATSKRangeAndBearingLine rabLine = new ATSKRangeAndBearingLine(
                    pt1, pt2, _mapView, lo);
            _drawing.drawMapItem(pt1);
            _drawing.drawMapItem(pt2);
            _drawing.drawMapItem(rabLine);
            return;
        }

        ATSKShape obsLine;
        boolean lineLeader = lo.uid.contains(ATSKConstants.LEADER_SUFFIX);
        boolean taxiway = Constants.isTaxiway(lo.type);
        if (lineLeader) {
            obsLine = new ATSKLineLeader(_mapView,
                    ATSKConstants.CURRENT_SCREEN_OBSTRUCTION, lo);
        } else {
            obsLine = new ATSKShape(_mapView,
                    Constants.isTaxiway(lo.type) ? ""
                            : ATSKConstants.CURRENT_SCREEN_OBSTRUCTION, lo);
        }

        int lineColor = LineHelper.getLineColor(lo.type);
        obsLine.setMetaString("remarks", lo.remarks);
        obsLine.setMetaString("obsName", lo.type);
        setLabel(lo, obsLine);
        if (lo.filled) {
            obsLine.setFilled(true);
            if (taxiway)
                obsLine.setFillColor(0);
            else
                obsLine.setFillColor(lineColor - ATSKDrawingTool.FILL_MASK);
            obsLine.setMetaString("obsType", "area");

            // close the obstruction
            GeoPoint[] p = new GeoPoint[points.length + 1];
            System.arraycopy(points, 0, p, 0, points.length);
            p[points.length] = p[0];
            points = p;
        } else {
            obsLine.setFilled(false);
            obsLine.setMetaString("obsType", "route");

            if (lo.uid.equals(ATSKConstants.TEMP_LINE_UID)) {
                obsLine.setLineStyle(EditablePolyline.BASIC_LINE_STYLE_DASHED);
                obsLine.setLabel(null);
                obsLine.setClickable(false);
            } else if (!lineLeader) {
                // Calculate label position/rotation
                // Stored for later so we we're not starting and
                // stopping the OPC hundreds of times when dragging points
                obsLine.setMetaString("label_point",
                        Conversions.toJson(lo.getCenter()));
            }
        }

        for (int i = 0; i < points.length; i++) {
            if (points[i] == null) {
                Log.e(TAG, "serious error has occurred: line point["
                        + i + "] is null", new Exception());
                return;
            }
        }
        obsLine.setMetaString(
                ATSKIntentConstants.OB_TYPE_SELECTED,
                ATSKIntentConstants.OB_STATE_LINE);
        obsLine.setMetaString(ATSKIntentConstants.OB_MENU_GROUP,
                "default");
        obsLine.setPoints(points);
        obsLine.setStrokeColor(lineColor);
        _drawing.drawMapItem(obsLine);
    }

    private String getLabel(PointObstruction po) {
        String mdesc = po.remark;
        if (mdesc == null || mdesc.length() == 0)
            mdesc = po.type;
        if (po.type.equals(Constants.PO_LABEL) || po.type.equals(
                Constants.PO_RAB_CIRCLE)) {
            // Don't show height for label-only POs
            return mdesc;
        }
        Unit heightUnit = getHeightUnit();
        return String.format(LocaleUtil.getCurrent(), "%s %1.0f %s", mdesc,
                Unit.METER.convertTo(po.height, heightUnit),
                heightUnit.getAbbr());
    }

    private String getLabel(LineObstruction lo) {
        String desc = lo.type;
        if (desc.contains("_LZMaintained")
                || desc.contains(ATSKConstants.INCURSION_LINE_APPROACH)
                || desc.contains(ATSKConstants.INCURSION_LINE_DEPARTURE)
                || desc.startsWith(ATSKConstants.GSR_MARKER))
            return "";

        if (lo.remarks != null && !lo.remarks.isEmpty()
                && !lo.remarks.equalsIgnoreCase("none"))
            desc = lo.remarks;

        Unit heightUnit = getHeightUnit();
        final String mHeight = String.format(LocaleUtil.getCurrent(),
                "%1.0f %s", Unit.METER.convertTo(lo.height, heightUnit),
                heightUnit.getAbbr());

        // Ignore zero height for taxiways and flat terrains
        if (Constants.isTaxiway(lo.type)
                || (Math.abs(lo.height) <= Conversions.THRESH
                && Constants.isFlatTerrain(lo.type)))
            return desc;
        return desc + " " + mHeight;
    }

    private void setLabel(PointObstruction po, Marker marker) {
        String label = getLabel(po);
        marker.setMetaString("callsign", label);
        if (shouldLabel(po.type))
            marker.setTitle(label);
    }

    private void setLabel(LineObstruction lo, ATSKShape shape) {
        String name = getLabel(lo);
        if (lo.filled)
            shape.setMetaString("centerPointLabel", name);
        else if (!lo.uid.contains(ATSKConstants.LEADER_SUFFIX))
            shape.setLabel(name);
        shape.setMetaString("callsign", name);
        shape.setMetaString("shapeName", name);
    }

    private Unit getHeightUnit() {
        return _prefs.getString(ATSKConstants.UNITS_DISPLAY,
                ATSKConstants.UNITS_FEET).equals(ATSKConstants.UNITS_METERS)
                ? Unit.METER : Unit.FOOT;
    }

    private Icon getIconFromType(final String type, final String uid) {

        int iconNumber = 0;

        if (_icons.containsKey(type))
            iconNumber = _icons.get(type);
        else
            iconNumber = R.drawable.po_generic_point;

        Context pluginContext = ATSKApplication.getInstance()
                .getPluginContext();

        Icon obsIcon;
        obsIcon = new Icon("android.resource://"
                + pluginContext.getPackageName() + "/"
                + iconNumber);

        if (uid.equals(ATSKConstants.TEMP_POINT_UID)) {
            Icon.Builder builder = new Icon.Builder();
            builder.setSize(64, 64);
            builder.setImageUri(Icon.STATE_DEFAULT, "android.resource://"
                    + pluginContext.getPackageName() + "/"
                    + iconNumber);
            obsIcon = builder.build();
        }
        return obsIcon;
    }

    public boolean RemoveLine(String group, String uID) {

        if (group.equals(ATSKConstants.APRON_GROUP)) {
            MapGroup apronDelete = _mapGroup.findMapGroup(uID);

            if (apronDelete != null)
                _mapGroup.removeGroup(apronDelete);
        } else
            removeMapItem(group, uID);
        return false;
    }

    public boolean CenterMap(double Lat, double Lon, boolean SuperZoom) {
        //MIKE - this was just added. implement accordingly.
        _drawing.centerMapOnLocation(new GeoPoint(Lat, Lon));
        return false;
    }

    /**
     * ATSKMapManager::getAltitudeHAE
     */
    public double GetElevation_m_msl(double Lat, double Lon) {
        final com.atakmap.android.elev.dt2.Dt2ElevationModel dem =
                com.atakmap.android.elev.dt2.Dt2ElevationModel.getInstance();

        double elevation_f = SurveyPoint.Altitude.INVALID;
        try {
            Altitude alt = dem.queryPoint(Lat, Lon);
            Altitude altMSL = EGM96.getInstance().getMSL(Lat, Lon, alt);
            if (altMSL.isValid())
                elevation_f = altMSL.getValue();
            else
                elevation_f = SurveyPoint.Altitude.INVALID;

        } catch (Exception e) {
        }

        return (float) elevation_f;

    }

    public boolean UpdateLine(LineObstruction lo) {
        if (lo.points.size() < 2)
            return false;

        List<SurveyPoint> linePoints = new ArrayList<SurveyPoint>();
        linePoints.addAll(lo.points);
        if (lo.filled) {
            //added to close the area drawn
            SurveyPoint closingPoint = new SurveyPoint(linePoints.get(0).lat,
                    linePoints.get(0).lon);
            linePoints.add(closingPoint);
        } else
            linePoints = ObstructionController
                    .createOffsetPoints(linePoints, lo.width);
        drawLineObstruction(lo, MapHelper
                .convertSurveyPoint2GeoPoint(linePoints));
        return false;
    }

    public boolean UpdateGradient(String group, String uID, String type,
            List<SurveyPoint> linePoints, boolean Clickable) {
        return false;
    }

    private OnSharedPreferenceChangeListener _prefsListener = new OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(
                SharedPreferences sharedPreferences, String key) {
            if (key.equals(ATSKConstants.UNITS_DISPLAY)) {
                for (MapItem item : _mapGroup.getItems()) {
                    if (item instanceof ATSKMarker) {
                        PointObstruction po = ((ATSKMarker) item)
                                .getObstruction();
                        if (po != null)
                            setLabel(po, ((ATSKMarker) item));
                    } else if (item instanceof ATSKShape) {
                        LineObstruction lo = ((ATSKShape) item)
                                .getObstruction();
                        if (lo != null)
                            setLabel(lo, ((ATSKShape) item));
                    }
                }
            }
        }
    };
}
