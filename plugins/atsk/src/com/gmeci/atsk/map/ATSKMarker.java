
package com.gmeci.atsk.map;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.atakmap.android.imagecapture.CapturePP;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapEventDispatcher;
import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.maps.PointMapItem;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.assets.Icon;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.spatial.kml.KMLUtil;
import com.ekito.simpleKML.model.Coordinate;
import com.ekito.simpleKML.model.Feature;
import com.ekito.simpleKML.model.Folder;
import com.ekito.simpleKML.model.Geometry;
import com.ekito.simpleKML.model.IconStyle;
import com.ekito.simpleKML.model.Placemark;
import com.ekito.simpleKML.model.Point;
import com.ekito.simpleKML.model.Style;
import com.ekito.simpleKML.model.StyleSelector;
import com.gmeci.atsk.ATSKATAKConstants;
import com.gmeci.atsk.ATSKFragment;
import com.gmeci.atsk.MapHelper;
import com.gmeci.atsk.resources.ATSKMenuLoader;
import com.gmeci.atskservice.resolvers.ObstructionProviderClient;
import com.gmeci.constants.Constants;
import com.gmeci.conversions.Conversions;
import com.gmeci.conversions.Conversions.Unit;
import com.gmeci.core.ATSKConstants;
import com.gmeci.core.PointObstruction;
import com.gmeci.core.SurveyPoint;

import java.util.ArrayList;
import java.util.List;

public class ATSKMarker extends Marker implements ATSKMapItem,
        PointMapItem.OnPointChangedListener,
        MapItem.OnGroupChangedListener {

    public static final String TAG = "ATSKMarker";
    public final static double DEFAULT_MIN_RENDER_SCALE = (1.0d / 100000.0d);

    protected final String _type;
    protected PointObstruction _obs;
    private boolean _drawRotated = false;

    public ATSKMarker(String type, PointObstruction po) {
        super(po.uid);
        _obs = po;
        _type = type;
        init();
    }

    public ATSKMarker(String type, String uid) {
        super(uid);
        _obs = null;
        _type = type;
        init();
    }

    protected void init() {
        setLabelVisible(_obs == null || !_obs
                .hasFlag(Constants.FL_HIDE_LABEL));
        setMetaBoolean("disable_offscreen_indicator", true);
        setMetaString("obsType", "point");
        updateMenu();
        addOnPointChangedListener(this);
        addOnGroupChangedListener(this);
    }

    @Override
    public void onPointChanged(final PointMapItem item) {
        if (_obs == null || getGroup() == null)
            return;
        SurveyPoint sp = MapHelper.convertGeoPoint2SurveyPoint(item.getPoint());
        double range = Conversions.calculateRange(sp, _obs);
        double alt = Math.abs(sp.getHAE() - _obs.getHAE());
        if (range < 0.01 && alt < 0.01)
            return;
        Log.d(TAG, ((Marker) item).getTitle() + " moved: " + item.getPoint()
                + " (" + range + "m, " + alt + "m HAE)");
        _obs.setSurveyPoint(sp);
        save();
    }

    public void setObstruction(PointObstruction obs) {
        _obs = obs;
        updateMenu();
    }

    protected void updateMenu() {
        String menu = "";
        if (_obs != null && !_obs.uid.equals(ATSKConstants.TEMP_POINT_UID)) {
            if (_obs.type.equals(Constants.PO_RAB_CIRCLE))
                menu = ATSKMenuLoader
                        .loadMenu("menus/atsk_rab_circle_menu.xml");
            else if (_obs.group == null || _obs.group.isEmpty()
                    || _obs.group.equals(ATSKConstants.DEFAULT_GROUP))
                menu = ATSKMenuLoader.loadMenu("menus/obs_point_menu.xml");
            else if (_obs.group.equals(ATSKConstants.DISTRESS_GROUP))
                menu = ATSKMenuLoader.loadMenu("menus/obs_distress_menu.xml");
            else if (_obs.group.equals(ATSKConstants.VEHICLE_GROUP))
                menu = ATSKMenuLoader.loadMenu("menus/obs_vehicle_menu.xml");
        }
        setMetaString("menu", menu);
    }

    @Override
    public void setAlwaysShowText(boolean show) {
        super.setAlwaysShowText(show);
        if (show)
            setMetaBoolean(LABEL_ALWAYS_SHOW, true);
        else
            removeMetaData(LABEL_ALWAYS_SHOW);
    }

    public boolean isVehicle() {
        return _type.equals(ATSKConstants.CURRENT_SCREEN_VEHICLE);
    }

    // Change icon size
    public boolean setIconSize(int size) {
        Icon ic = getIcon();
        if (ic != null && ic.getWidth() != size && ic.getHeight() != size) {
            Icon.Builder icb = ic.buildUpon();
            icb.setSize(size, size);
            setIcon(icb.build());
            return true;
        }
        return false;
    }

    // Change icon color modulation
    public boolean setIconColor(int color) {
        Icon ico = getIcon();
        if (ico != null) {
            Icon.Builder builder = ico.buildUpon();
            builder.setColor(Icon.STATE_DEFAULT, color);
            setIcon(builder.build());
            return true;
        }
        return false;
    }

    @Override
    public boolean getClickable() {
        // Hack - prevent sub-menu icon swapping
        // TODO: Add 'swapIcon' check for sub-menu in ATAK
        // Despite the name, 'disableSwap' only prevents
        // the 'onClick' actions from being swapped
        removeMetaData("submenu_map");

        return !ATSKFragment.isMapState()
                && ATSKFragment.isMapType(_type)
                && super.getClickable();
    }

    public PointObstruction getObstruction() {
        return _obs;
    }

    // Never persist ATSK map items
    @Override
    public void persist(MapEventDispatcher dispatcher,
            Bundle persistExtras, Class clazz) {
    }

    @Override
    public void copy(int copies) {
        Context ctx = MapView.getMapView().getContext();
        if (copies <= 0) {
            Toast.makeText(ctx, "Please enter a valid number of copies.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        if (_obs != null) {
            Toast.makeText(ctx, "Duplicating \"" + getTitle()
                    + "\" " + copies + " times. Tap to set position.",
                    Toast.LENGTH_LONG).show();
            // Request edit menu on new point
            Intent edit = new Intent(
                    ATSKConstants.PT_OBSTRUCTION_CLICK_ACTION);
            edit.putExtra(ATSKConstants.UID_EXTRA, _obs.uid);
            edit.putExtra(ATSKConstants.GROUP_EXTRA, _obs.group);
            edit.putExtra(ATSKConstants.COPY_EXTRA, copies);
            AtakBroadcast.getInstance().sendBroadcast(edit);
        }
    }

    @Override
    public void rename(String name) {
        if (_obs != null) {
            _obs.remark = name;
            save();
        }
    }

    @Override
    public void save() {
        if (_obs == null)
            return;

        Context ctx = MapView.getMapView().getContext();
        ObstructionProviderClient opc =
                new ObstructionProviderClient(ctx);
        if (opc.Start()) {
            PointObstruction po = opc.GetPointObstruction(
                    isVehicle() ? ATSKConstants.VEHICLE_GROUP
                            : ATSKConstants.DEFAULT_GROUP, _obs.uid);
            if (po != null)
                opc.EditPoint(_obs);
            else
                _obs = null;
            opc.Stop();
        }
    }

    @Override
    public void setLabelVisible(boolean visible) {
        boolean changed = visible != getLabelVisible();
        if (visible) {
            setMetaDouble("minRenderScale", DEFAULT_MIN_RENDER_SCALE);
            removeMetaData("centerPointLabel");
            setMetaBoolean(LABEL_VISIBLE, true);
            if (_obs != null)
                _obs.removeFlag(Constants.FL_HIDE_LABEL);
        } else {
            setMetaDouble("minRenderScale", Double.MAX_VALUE);
            removeMetaData(LABEL_VISIBLE);
            if (_obs != null)
                _obs.addFlag(Constants.FL_HIDE_LABEL);
        }
        if (changed && getGroup() != null)
            refresh(MapView.getMapView().getMapEventDispatcher(),
                    null, getClass());
    }

    @Override
    public boolean getLabelVisible() {
        return hasMetaValue(LABEL_ALWAYS_SHOW) || hasMetaValue(LABEL_VISIBLE);
    }

    @Override
    public void onItemAdded(MapItem item, MapGroup group) {
    }

    @Override
    public void onItemRemoved(MapItem item, MapGroup group) {
        removeOnPointChangedListener(this);
        removeOnGroupChangedListener(this);
    }

    @Override
    public void delete() {
        if (_obs != null) {
            ObstructionProviderClient opc = new ObstructionProviderClient(
                    MapView.getMapView().getContext());
            if (opc.Start()) {
                opc.DeletePoint(_obs.group, _obs.uid, true);
                opc.Stop();
            }
        }
        if (getGroup() != null)
            getGroup().removeItem(this);
    }

    @Override
    public int getStyle() {
        return super.getStyle() | (_drawRotated
                ? Marker.STYLE_ROTATE_HEADING_MASK : 0);
    }

    @Override
    public void drawCanvas(CapturePP cap, Bundle data) {
        // Hack to get Marker.drawCanvas to draw rotated markers
        // without rendering the extra heading arrow
        _drawRotated = Double.compare(getTrackHeading(), Double.NaN) != 0;
        super.drawCanvas(cap, data);
        _drawRotated = false;
    }

    public static ATSKMarker find(String uid) {
        MapGroup atskGroup = MapView.getMapView().getRootGroup()
                .findMapGroup(ATSKATAKConstants.ATSK_MAP_GROUP_OBS);
        if (atskGroup != null) {
            MapItem item = atskGroup.deepFindUID(uid);
            if (item instanceof ATSKMarker)
                return (ATSKMarker) item;
        }
        return null;
    }

    @Override
    public String getKMLDescription() {
        if (_obs != null) {
            String name = (_obs.remark == null || _obs.remark.isEmpty())
                    ? _obs.type : _obs.remark;
            return name + "\nType: " + _obs.type
                    + "\nLocation: "
                    + Conversions.GetMGRS(_obs.lat, _obs.lon)
                    + "\nElevation: "
                    + _obs.getMSLAltitude().toString(Unit.FOOT)
                    + "\nHeight: " + getFt(_obs.height)
                    + "\nLength: " + getFt(_obs.length)
                    + "\nWidth: " + getFt(_obs.width);
        }
        return "";
    }

    protected Folder toKml() {

        if (!getVisible())
            return null;

        try {
            // style element
            Style style = new Style();
            IconStyle istyle = new IconStyle();
            int color = getAffiliationColor(this);
            istyle.setColor(KMLUtil.convertKmlColor(color));

            // use crosshairs
            com.ekito.simpleKML.model.Icon icon = new com.ekito.simpleKML.model.Icon();
            icon.setHref("http://maps.google.com/mapfiles/kml/shapes/cross-hairs.png");
            istyle.setScale(0.5f);

            istyle.setIcon(icon);
            style.setIconStyle(istyle);

            String styleId = KMLUtil.hash(style);
            style.setId(styleId);

            // Folder element containing styles, shape and label
            Folder folder = new Folder();
            if (getGroup() != null
                    && !FileSystemUtils.isEmpty(getGroup().getFriendlyName()))
                folder.setName(getGroup().getFriendlyName());
            else
                folder.setName(getTitle());
            List<StyleSelector> styles = new ArrayList<StyleSelector>();
            styles.add(style);
            folder.setStyleSelector(styles);
            List<Feature> folderFeatures = new ArrayList<Feature>();
            folder.setFeatureList(folderFeatures);

            // Placemark element
            Placemark label = null;
            String title = getTitle();
            if (title == null)
                title = "";
            if (getLabelVisible() && !title.isEmpty()) {
                label = new Placemark();
                label.setId(getUID());
                label.setName(title);
                label.setStyleUrl("#" + styleId);
                label.setVisibility(1);
            }

            Coordinate coord = KMLUtil.convertKmlCoord(getPoint(), false);
            if (coord == null) {
                Log.w(TAG, "No marker location set");
                return null;
            }

            Point centerPoint = new Point();
            centerPoint.setCoordinates(coord);
            centerPoint.setAltitudeMode("absolute");

            List<Geometry> pointGeomtries = new ArrayList<Geometry>();
            pointGeomtries.add(centerPoint);
            if (label != null) {
                label.setGeometryList(pointGeomtries);
                folderFeatures.add(label);

                //set an HTML description (e.g. for the Google Earth balloon)
                //String desc = getKMLDescription(null);
                label.setDescription(getKMLDescription());
            }

            return folder;
        } catch (Exception e) {
            Log.e(TAG, "Export of Marker to KML failed with Exception", e);
        }

        return null;
    }

    private static String getFt(double meters) {
        return Unit.METER.format(meters, Unit.FOOT);
    }
}
