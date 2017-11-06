
package com.gmeci.atsk.map;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.atakmap.android.ipc.AtakBroadcast.DocumentedIntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.atakmap.android.icons.UserIcon;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapEvent;
import com.atakmap.android.maps.MapEventDispatcher;
import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.maps.PointMapItem;
import com.atakmap.android.maps.Polyline;
import com.atakmap.android.user.icon.SpotMapPalletFragment;
import com.atakmap.android.util.Circle;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.gmeci.atsk.ATSKATAKConstants;
import com.gmeci.atsk.MapHelper;
import com.gmeci.atsk.resources.ATSKApplication;
import com.gmeci.atsk.resources.ATSKMenuLoader;
import com.gmeci.atsk.vehicle.VehicleRadial;
import com.gmeci.atsk.vehicle.VehicleRotationTool;
import com.gmeci.atskservice.resolvers.ObstructionProviderClient;
import com.gmeci.conversions.Conversions;
import com.gmeci.core.ATSKConstants;
import com.gmeci.core.ATSKIntentConstants;
import com.gmeci.core.LineObstruction;
import com.gmeci.core.PointObstruction;
import com.gmeci.core.SurveyPoint;
import com.gmeci.vehicle.VehicleBlock;

import java.util.ArrayList;
import java.util.List;

/**
 * Cleaner, less buggy vehicle map item class that extends ATSKMarker
 */
public class ATSKVehicle extends ATSKMarker {

    private static final String TAG = "ATSKVehicle";

    public static final String SHOW_RINGS = "ShowAircraftRings";
    public static final String ROTATE = "RotateVehicle";
    public static final String AC_RINGS = "ac_rings";
    public static final String AC_RINGS_SELECTED = "ac_rings_selected";

    // State colors
    private static final int COLOR_TEMP = 0xFFA0FFA0;
    private static final int COLOR_PERM = 0xFF00FF00;

    // Max number of possible rings
    private static final int MAX_RINGS = 5;

    // Fields
    private boolean _setup = false;
    private MapView _mapView;
    private MapGroup _mapGroup;
    private ATSKDrawingTool _drawing;
    private LineObstruction _lineObs;
    private ATSKShape _shape;
    private String _type;
    private String _name;
    private double _width, _length, _height;
    private SurveyPoint _loc;
    private Circle[] _rings = new Circle[MAX_RINGS];

    public ATSKVehicle(String uid) {
        super(ATSKConstants.CURRENT_SCREEN_VEHICLE, uid);
    }

    @Override
    protected void init() {
        _mapView = MapView.getMapView();
        _mapGroup = _mapView.getRootGroup().findMapGroup(
                ATSKATAKConstants.ATSK_MAP_GROUP_OBS);
        _drawing = new ATSKDrawingTool(_mapView, _mapGroup);
        setupReceiver();
    }

    /**
     * Setup the vehicle obstruction marker, rings, and polyline
     *
     * @param type  vehicle type/name
     * @param loc   vehicle location/rotation
     * @param temp  true if temporary obstruction
     */
    public void setup(String type, SurveyPoint loc, boolean temp) {
        boolean typeChanged = !type.equals(_type);
        boolean rotChanged = _loc == null
                || _loc.course_true != loc.course_true;
        _type = type;
        _loc = new SurveyPoint(loc);

        setupMarker();

        // Setup polyline (should only change when type or rotation is modified)
        if (typeChanged || rotChanged)
            setupPolyline();

        // Setup radials (should only change when type changes)
        if (typeChanged)
            setupRadials();
        setRadialOn(hasMetaValue(AC_RINGS_SELECTED));
        setTemp(temp);
        moveTo(_loc);
    }

    public void setup(PointObstruction po, boolean temp) {
        setup(po.type, po, temp);
        setName(po.remark);
    }

    private void setupPolyline() {
        double[] dimen = VehicleBlock.getBlockDimensions(_type);

        _length = dimen[0];
        _width = dimen[1];
        _height = dimen[2];

        // Draw polyline
        _lineObs = VehicleBlock.buildLineObstruction(_type, _loc);
        _lineObs.uid = getUID();
        _shape = new ATSKShape(_mapView, getUID() + "_shape");
        _shape.setPoints(MapHelper.convertSurveyPoint2GeoPoint(_lineObs.points));
        _shape.setStrokeWeight(ATSKATAKConstants.LINE_WEIGHT_AC);
        _shape.setStrokeColor(COLOR_TEMP);
        _shape.setFillColor(COLOR_TEMP - ATSKDrawingTool.FILL_MASK);
        _shape.setStyle(Polyline.STYLE_STROKE_MASK
                | Polyline.STYLE_CLOSED_MASK | Polyline.STYLE_FILLED_MASK);
        _shape.setZOrder(ATSKATAKConstants.Z_ORDER_PARKING_AC);
        _shape.setMetaString("obsType", "vehicle_fill");
        addItem(_shape);
        _shape.setClickable(false);

        refreshItem(_shape);
    }

    private void setupMarker() {
        if (_setup || _loc == null)
            return;
        // Create center marker
        setTitle(getTitle());
        setMetaInteger("color", COLOR_TEMP);
        setMetaString(UserIcon.IconsetPath,
                SpotMapPalletFragment.LABEL_ONLY_ICONSETPATH);
        setTextColor(COLOR_TEMP);
        setMetaBoolean("temp_mode", true);
        setMetaBoolean("drag", false);
        setMetaBoolean("movable", false);
        setMetaBoolean("editable", true);
        setMetaBoolean("removable", true);
        setMetaString("obsType", "vehicle");
        setMetaDouble("minRenderScale", 0.0005);
        setMetaString("shapeName", getTitle());
        setMetaString("shapeUID", getUID() + "_shape");
        setMetaString(
                ATSKIntentConstants.OB_TYPE_SELECTED,
                ATSKIntentConstants.OB_STATE_POINT);
        setMetaString(
                ATSKIntentConstants.OB_MENU_GROUP,
                ATSKConstants.VEHICLE_GROUP);
        setPoint(toGeoPoint());
        addOnPointChangedListener(new OnPointChangedListener() {
            @Override
            public void onPointChanged(final PointMapItem item) {
                if (getTemp() || item.getPoint() == null)
                    return;
                GeoPoint gp = item.getPoint();
                GeoPoint center = _shape.getCenter();
                double dist = gp.distanceTo(center);
                if (dist > 0.01) {
                    Log.d(TAG, ((Marker) item).getTitle()
                            + " moved: " + gp + " (" + dist + ")");
                    _loc.setSurveyPoint(MapHelper.
                            convertGeoPoint2SurveyPoint(gp));
                    moveTo(_loc);
                    save();
                }
            }
        });
        // Remove all associated items when deleted
        addOnGroupChangedListener(new OnGroupChangedListener() {
            @Override
            public void onItemAdded(MapItem item, MapGroup group) {
                //Log.d(TAG, "Marker added to " + group.getFriendlyName());
            }

            @Override
            public void onItemRemoved(MapItem item, MapGroup group) {
                //Log.d(TAG, "Marker removed from " + group.getFriendlyName(), new Throwable());
                String uid = item.getUID();
                for (MapItem mi : group.getItems()) {
                    if (mi != item && mi.getUID().startsWith(uid))
                        group.removeItem(mi);
                }
                if (hasMetaValue(AC_RINGS))
                    removeMetaData(AC_RINGS);
                delete(false);
            }
        });
        // Hide/show rings with visibility toggle
        addOnVisibleChangedListener(new MapItem.OnVisibleChangedListener() {
            @Override
            public void onVisibleChanged(MapItem item) {
                if (getMetaBoolean(AC_RINGS_SELECTED, false)) {
                    for (Circle ring : _rings) {
                        if (ring != null)
                            ring.setVisible(getVisible());
                    }
                }
            }
        });
        addItem(this);
        _setup = true;
    }

    private void setupRadials() {
        // Remove existing rings
        for (int i = 0; i < _rings.length; i++) {
            removeItem(_rings[i]);
            _rings[i] = null;
        }

        // Read values from file
        VehicleBlock vehicle = VehicleBlock.getBlock(_type);
        double[] radials = vehicle.getRadials();
        int rType = vehicle.getType();

        // Detect valid
        boolean gt0 = false;
        for (double r : radials)
            gt0 |= r > 0;
        if (gt0) {
            // Add rings
            boolean show = getMetaBoolean(AC_RINGS_SELECTED, false);
            setMetaBoolean(AC_RINGS, true);
            Conversions.Unit dispUnit = _unitsFeet ? Conversions.Unit.FOOT
                    : Conversions.Unit.METER;
            _rings = new Circle[radials.length];
            for (int i = 0; i < radials.length; i++) {
                if (radials[i] == 0.0d)
                    continue;
                VehicleRadial radial = VehicleRadial.getByIndex(rType, i);
                _rings[i] = new Circle(getRingCenter(), radials[i], getUID()
                        + "_ring_" + i);
                _rings[i].setStrokeWeight(ATSKATAKConstants.LINE_WEIGHT_AC);
                _rings[i].setStrokeColor(radial.getColor());
                // address (consecutive) radial overlap
                StringBuilder otherLbl = new StringBuilder();
                StringBuilder otherName = new StringBuilder();
                for (int j = 0; j < i; j++) {
                    if (radials[j] == radials[i]) {
                        _rings[j].setLabel("");
                        _rings[j].setClickable(false);
                        VehicleRadial other = VehicleRadial
                                .getByIndex(rType, j);
                        otherLbl.append(other.getAbbrev());
                        otherLbl.append(",");
                        otherName.append(other.getName());
                        otherName.append("\n");
                    }
                }
                _rings[i].setMetaString("radialName", otherName
                        + radial.getName());
                _rings[i].setLabel(otherLbl + radial.getAbbrev()
                        + ": "
                        + Conversions.Unit.METER.format(radials[i], dispUnit));
                _rings[i].setClickable(true);
                _rings[i].setMetaBoolean("ignoreMenu", true);
                _rings[i].setMetaBoolean("ignoreFocus", true);
                _rings[i].setVisible(show);
                addItem(_rings[i]);
            }
        } else {
            // Disable radial button
            if (hasMetaValue(AC_RINGS))
                removeMetaData(AC_RINGS);
            if (hasMetaValue(AC_RINGS_SELECTED))
                removeMetaData(AC_RINGS_SELECTED);
        }
    }

    public GeoPoint toGeoPoint() {
        return MapHelper.convertSurveyPoint2GeoPoint(_loc);
    }

    public PointObstruction toPointObstruction() {
        PointObstruction po = new PointObstruction(_loc);
        po.group = ATSKConstants.VEHICLE_GROUP;
        po.uid = getUID();
        po.alt = _loc.getHAEAltitude();
        po.type = _type;
        po.width = _width;
        po.length = _length;
        po.height = _height;
        po.remark = _name;
        po.TopCollected = false;
        return po;
    }

    public void setName(String name) {
        _name = name;
        setTitle(getTitle());
        setMetaString("remarks", _name);
    }

    public String getBlockName() {
        return _type;
    }

    public String getName() {
        return _name == null ? "" : _name;
    }

    @Override
    public String getTitle() {
        if (getName().equals(""))
            return _type;
        else
            return getName() + " (" + _type + ")";
    }

    public double getWidth() {
        return _width;
    }

    public double getLength() {
        return _length;
    }

    public double getHeight() {
        return _height;
    }

    public SurveyPoint getLocation() {
        return new SurveyPoint(_loc);
    }

    public void setTemp(boolean temp) {
        if (_shape != null) {
            _shape.setStrokeColor(temp ? COLOR_TEMP : COLOR_PERM);
            _shape.setFillColor((temp ? COLOR_TEMP : COLOR_PERM)
                    - ATSKDrawingTool.FILL_MASK);
        }
        _obs = temp ? null : toPointObstruction();
        setMetaBoolean("temp_mode", temp);
        setMetaBoolean("movable", !temp);
        setMetaInteger("color", temp ? COLOR_TEMP : COLOR_PERM);
        setTextColor(temp ? COLOR_TEMP : COLOR_PERM);
        setClickable(!temp);
        if (!temp)
            setMetaString("menu", getMenu());
        else
            removeMetaData("menu");
        setMetaBoolean("ignoreMenu", temp);
        refreshItem(this);
    }

    public boolean getTemp() {
        return getMetaBoolean("temp_mode", true);
    }

    public void moveTo(SurveyPoint loc) {
        double heading = getHeading();
        _loc = new SurveyPoint(loc);
        _loc.course_true = heading;
        GeoPoint gp = toGeoPoint();
        if (_shape != null) {
            _shape.moveClosedSet(_shape.getCenter(), gp);
            _lineObs.points.clear();
            for (GeoPoint p : _shape.getPoints()) {
                _lineObs.points.add(new SurveyPoint(p.getLatitude(),
                        p.getLongitude(), _loc.alt));
            }
        }
        setPoint(gp);
        GeoPoint boundsCenter = getRingCenter();
        for (Circle ring : _rings) {
            if (ring != null)
                ring.setCenterPoint(boundsCenter);
        }
    }

    public void setHeading(double trueHeading) {
        SurveyPoint loc = new SurveyPoint(_loc);
        loc.course_true = Conversions.deg360(trueHeading);
        setup(_type, loc, getTemp());
    }

    public double getHeading() {
        if (_loc != null)
            return _loc.course_true;
        return 0;
    }

    public LineObstruction getLineObstruction() {
        return _lineObs;
    }

    public PointObstruction getAABB() {
        return _lineObs == null ? toPointObstruction()
                : _lineObs.getAABB();
    }

    public PointObstruction getOBB() {
        if (_lineObs == null)
            return toPointObstruction();

        // Get non-rotated line obstruction
        LineObstruction line = _lineObs;
        if (_loc.course_true != 0) {
            SurveyPoint locNoRot = new SurveyPoint(_loc);
            locNoRot.course_true = 0;
            List<SurveyPoint> vehPoints = VehicleBlock
                    .buildPolyline(_type, locNoRot);
            line = new LineObstruction();
            line.points = new ArrayList<SurveyPoint>(vehPoints);
        }

        double minLat = 90.0, maxLat = -90, minLon = 180, maxLon = -180;
        for (SurveyPoint sp : line.points) {
            minLat = Math.min(minLat, sp.lat);
            minLon = Math.min(minLon, sp.lon);
            maxLat = Math.max(maxLat, sp.lat);
            maxLon = Math.max(maxLon, sp.lon);
        }
        PointObstruction ret = new PointObstruction();
        ret.lat = (minLat + maxLat) / 2;
        ret.lon = (minLon + maxLon) / 2;
        ret.course_true = _loc.course_true;
        ret.alt = _loc.alt;
        ret.height = _height;
        ret.width = Conversions.CalculateRangem(minLat, minLon, minLat, maxLon);
        ret.length = Conversions
                .CalculateRangem(minLat, minLon, maxLat, minLon);
        return ret;
    }

    public GeoPoint getRingCenter() {
        SurveyPoint obb = getOBB();
        double[] rangeAng = Conversions.CalculateRangeAngle(
                _loc.lat, _loc.lon, obb.lat, obb.lon);
        double[] newPos = Conversions.AROffset(_loc.lat, _loc.lon, rangeAng[1]
                + _loc.course_true, rangeAng[0]);
        return MapHelper.convertSurveyPoint2GeoPoint(new SurveyPoint(newPos[0],
                newPos[1]));
    }

    public void setRadialOn(boolean show) {
        GeoPoint boundsCenter = getRingCenter();
        for (Circle ring : _rings) {
            if (ring != null) {
                ring.setCenterPoint(boundsCenter);
                ring.setVisible(show);
            }
        }
        if (hasMetaValue(AC_RINGS)) {
            if (show)
                setMetaBoolean(AC_RINGS_SELECTED, true);
            else
                removeMetaData(AC_RINGS_SELECTED);
        }
    }

    public boolean isRadialOn() {
        boolean on = false;
        for (Circle ring : _rings) {
            if (ring != null)
                on |= ring.getVisible();
        }
        return on;
    }

    @Override
    public void save() {
        Log.d(TAG, "Calling save on " + this);
        ObstructionProviderClient opc =
                new ObstructionProviderClient(_mapView.getContext());
        if (opc.Start()) {
            opc.EditPoint(toPointObstruction());
            opc.Stop();
        }
    }

    @Override
    public void rename(String name) {
        setName(name);
        save();
    }

    @Override
    public void setLabelVisible(boolean visible) {
    }

    @Override
    public boolean getLabelVisible() {
        return true;
    }

    @Override
    public void delete() {
        delete(true);
    }

    public void delete(boolean removeFromDb) {
        if (removeFromDb)
            super.delete();
        else
            removeItem(this);
        removeItem(_shape);
        for (Circle ring : _rings)
            removeItem(ring);
        if (getVehicles().isEmpty())
            stopReceiver();
    }

    private void addItem(MapItem mi) {
        if (mi != null)
            _drawing.drawMapItem(mi, mi.getUID(), _mapGroup);
    }

    private void refreshItem(MapItem mi) {
        if (mi != null)
            mi.refresh(_mapView.getMapEventDispatcher(), null, getClass());
    }

    private void removeItem(MapItem mi) {
        if (mi != null && mi.getGroup() != null)
            mi.getGroup().removeItem(mi);
    }

    public String getMenu() {
        return ATSKMenuLoader.loadMenu("menus/obs_vehicle_menu.xml");
    }

    @Override
    public String toString() {
        return TAG + ": " + getTitle() + " [" + getUID() + "]";
    }

    /**********************************************
     * STATIC
     **********************************************/

    private static boolean _receiverSetup = false;
    private static VehicleRotationTool _rotationTool;
    private static SharedPreferences _prefs;
    private static boolean _unitsFeet = true;

    private static void setupReceiver() {
        if (!_receiverSetup) {
            DocumentedIntentFilter filter = new DocumentedIntentFilter();
            filter.addAction(SHOW_RINGS);
            filter.addAction(ROTATE);
            AtakBroadcast.getInstance().registerReceiver(_radialReceiver,
                    filter);

            MapView.getMapView().getMapEventDispatcher()
                    .addMapEventListener(MapEvent.ITEM_CLICK, _ringTapListener);

            if (_prefs == null) {
                _prefs = PreferenceManager
                        .getDefaultSharedPreferences(MapView.getMapView()
                                .getContext());
                _prefs.registerOnSharedPreferenceChangeListener(_prefsListener);
                _unitsFeet = _prefs.getString(ATSKConstants.UNITS_DISPLAY,
                        ATSKConstants.UNITS_FEET).equals(
                        ATSKConstants.UNITS_FEET);
            }
            _receiverSetup = true;
        }
    }

    private static void stopReceiver() {
        if (_receiverSetup) {
            AtakBroadcast.getInstance().unregisterReceiver(_radialReceiver);
            MapView.getMapView()
                    .getMapEventDispatcher()
                    .removeMapEventListener(MapEvent.ITEM_CLICK,
                            _ringTapListener);
            if (_prefs != null)
                _prefs.unregisterOnSharedPreferenceChangeListener(
                        _prefsListener);
            _receiverSetup = false;
        }
    }

    // Aircraft-specific radial options
    private static final BroadcastReceiver _radialReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            Bundle extras = intent.getExtras();
            if (extras == null)
                return;

            String uid = extras.getString("uid", null);
            if (uid == null)
                return;

            ATSKVehicle ac = find(uid);
            if (ac == null)
                return;

            String action = intent.getAction();
            if (action.equals(SHOW_RINGS))
                ac.setRadialOn(!ac.isRadialOn());
            else if (action.equals(ROTATE)) {
                // Start vehicle rotation tool
                if (_rotationTool == null)
                    _rotationTool = new VehicleRotationTool(
                            MapView.getMapView());
                Bundle toolExtras = new Bundle(1);
                toolExtras.putString("uid", uid);
                ATSKApplication.setObstructionCollectionMethod(
                        ATSKIntentConstants.OB_STATE_MAP_CLICK, TAG, false,
                        VehicleRotationTool.TOOL_IDENTIFIER, toolExtras);
            }
        }
    };

    private static final MapEventDispatcher.MapEventDispatchListener _ringTapListener = new MapEventDispatcher.MapEventDispatchListener() {
        @Override
        public void onMapEvent(MapEvent event) {
            MapItem mi = event.getItem();
            if (mi != null && mi.hasMetaValue("radialName")) {
                // Show radial name
                MapView mv = MapView.getMapView();
                Toast.makeText(mv.getContext(),
                        mi.getMetaString("radialName", "Unknown"),
                        Toast.LENGTH_LONG).show();
                // Hack to prevent panning when clicked
                mi.setMetaString("menu_point", mv.getCenterPoint()
                        .toStringRepresentation());
                // Remove coord overlay
                AtakBroadcast.getInstance().sendBroadcast(
                        new Intent("com.atakmap.android.maps.HIDE_DETAILS"));
                event.getExtras().putBoolean("eventNotHandled", false);
            }
        }
    };

    private static final SharedPreferences.OnSharedPreferenceChangeListener _prefsListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(
                SharedPreferences prefs, String key) {
            if (key.equals(ATSKConstants.UNITS_DISPLAY)) {
                _unitsFeet = prefs.getString(key, ATSKConstants.UNITS_FEET)
                        .equals(ATSKConstants.UNITS_FEET);
                // Respawn all radials
                for (ATSKVehicle veh : getVehicles())
                    veh.setupRadials();
            }
        }
    };

    public static ATSKVehicle find(String uid) {
        ATSKMarker ret = ATSKMarker.find(uid);
        if (ret instanceof ATSKVehicle)
            return (ATSKVehicle) ret;
        return null;
    }

    public static List<ATSKVehicle> getVehicles() {
        List<ATSKVehicle> veh = new ArrayList<ATSKVehicle>();
        MapGroup atskGroup = MapView.getMapView().getRootGroup()
                .findMapGroup(ATSKATAKConstants.ATSK_MAP_GROUP_OBS);
        if (atskGroup != null) {
            for (MapItem mi : atskGroup.getItems())
                if (mi instanceof ATSKVehicle)
                    veh.add((ATSKVehicle) mi);
        }
        return veh;
    }
}
