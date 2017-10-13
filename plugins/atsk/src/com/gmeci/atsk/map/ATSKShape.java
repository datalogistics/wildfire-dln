
package com.gmeci.atsk.map;

import android.content.Intent;
import android.graphics.PointF;
import android.os.Bundle;

import com.atakmap.android.drawing.tools.ShapeEditTool;
import com.atakmap.android.editableShapes.EditablePolyline;
import com.atakmap.android.imagecapture.CapturePP;
import com.atakmap.android.imagecapture.PointA;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapEventDispatcher;
import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Polyline;
import com.atakmap.android.toolbar.ToolManagerBroadcastReceiver;
import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.spatial.kml.KMLUtil;
import com.ekito.simpleKML.model.Coordinate;
import com.ekito.simpleKML.model.Data;
import com.ekito.simpleKML.model.ExtendedData;
import com.ekito.simpleKML.model.Feature;
import com.ekito.simpleKML.model.Folder;
import com.ekito.simpleKML.model.Geometry;
import com.ekito.simpleKML.model.IconStyle;
import com.ekito.simpleKML.model.LineStyle;
import com.ekito.simpleKML.model.Placemark;
import com.ekito.simpleKML.model.Point;
import com.ekito.simpleKML.model.PolyStyle;
import com.ekito.simpleKML.model.Style;
import com.ekito.simpleKML.model.StyleSelector;
import com.gmeci.atsk.ATSKATAKConstants;
import com.gmeci.atsk.ATSKFragment;
import com.gmeci.atsk.MapHelper;
import com.gmeci.atsk.obstructions.ObstructionController;
import com.gmeci.atsk.resources.ATSKApplication;
import com.gmeci.atsk.resources.ATSKMenuLoader;
import com.gmeci.atskservice.resolvers.ObstructionProviderClient;
import com.gmeci.constants.Constants;
import com.gmeci.conversions.Conversions;
import com.gmeci.conversions.Conversions.Unit;
import com.gmeci.core.ATSKConstants;
import com.gmeci.core.LineObstruction;
import com.gmeci.core.SurveyPoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ATSK-specific polylines
 */

public class ATSKShape extends EditablePolyline implements ATSKMapItem {

    private static final String TAG = "ATSKShape";
    private String _menu;
    private String _atskType;
    private boolean _testOrtho = false;
    private LineObstruction _lineObs = null;
    private OnGroupChangedListener _groupListener;

    // Hacky way of rendering label at specific location and orientation
    private EditablePolyline _labelLine;

    public ATSKShape(MapView map, MapGroup mapGroup, String uid, String atskType) {
        super(map, uid);
        _atskType = atskType;
        if (atskType == null)
            return;

        try {
            setClickable(true);
            setMetaBoolean("editable", true);
            setMetaString("menu", getShapeMenu());
            setMetaBoolean("ignoreMenu", false);
            setBasicLineStyle(Polyline.BASIC_LINE_STYLE_SOLID);
            setStrokeWeight(ATSKATAKConstants.LINE_WEIGHT);
            setLabelVisible(true);
            if (mapGroup != null)
                setMetaString(ATSKATAKConstants.SURVEY_UID, mapGroup
                        .getParentGroup().getFriendlyName());

            setMetaString(ATSKATAKConstants.ITEM_TYPE, atskType);
        } catch (Exception e) {
        }
    }

    public ATSKShape(MapView map, String atskType, LineObstruction lo) {
        this(map, null, lo.uid, atskType);
        _lineObs = lo;
        if (_lineObs.hasFlag(Constants.FL_HIDE_LABEL))
            setLabelVisible(false);
        if (_lineObs.hasFlag(Constants.FL_LABEL_TURNED))
            setRotateLabel(true);
    }

    public ATSKShape(MapView map, String uid) {
        this(map, null, uid, null);
    }

    public LineObstruction getObstruction() {
        return _lineObs;
    }

    @Override
    public boolean setPoint(int index, GeoPoint point) {
        boolean ret = false;
        if (!isBulkOperation()) {
            ObstructionProviderClient opc =
                    new ObstructionProviderClient(mapView.getContext());
            if (opc.Start()) {
                boolean remove = (point == null);
                if (remove)
                    point = getCenter();
                LineObstruction lo = ObstructionController.getInstance()
                        .getLineObstruction(getUID());
                if (lo != null && getNumPoints() > 0) {
                    // Just in case
                    index = Math.min(Math.max(index, 0), getNumPoints() - 1);

                    boolean hitPoint = getMetaString("hit_type", "point")
                            .equals("point");
                    GeoPoint lastTouch = GeoPoint.parseGeoPoint(
                            getMetaString("last_touch", null));
                    if (hitPoint || lastTouch == null)
                        lastTouch = _points.get(index);

                    int lineSize = lo.points.size();
                    SurveyPoint newPoint = MapHelper
                            .convertGeoPoint2SurveyPoint(point);
                    SurveyPoint touchPoint = MapHelper
                            .convertGeoPoint2SurveyPoint(lastTouch);

                    // Most likely a route
                    if (getNumPoints() >= lineSize * 2 && lo.width > 0.0) {

                        // Get left-side index (center line index in LO)
                        boolean rightLine = false;
                        if (index >= lineSize) {
                            index = (lineSize - index)
                                    + (lineSize - (hitPoint ? 1 : 0));
                            rightLine = true;
                        }
                        index = Math.min(Math.max(0, index), lineSize - 1);

                        if (!remove) {
                            // Line hit - get new center point
                            SurveyPoint loPoint = new SurveyPoint(
                                    lo.points.get(index));
                            if (!hitPoint && index > 0) {
                                SurveyPoint prevPoint = lo.points
                                        .get(index - 1);
                                double ang = Conversions
                                        .CalculateAngledeg(prevPoint.lat,
                                                prevPoint.lon, loPoint.lat,
                                                loPoint.lon)
                                        + (rightLine ? -90 : 90);
                                double[] center = Conversions.AROffset(
                                        touchPoint.lat,
                                        touchPoint.lon, ang, lo.width / 2.0);
                                loPoint.lat = center[0];
                                loPoint.lon = center[1];
                            }

                            // Correct route left/right edge movement
                            double[] rangeAng = Conversions
                                    .CalculateRangeAngle(
                                            touchPoint.lat, touchPoint.lon,
                                            newPoint.lat, newPoint.lon);
                            double[] newLatLon = Conversions.AROffset(
                                    loPoint.lat,
                                    loPoint.lon, rangeAng[1], rangeAng[0]);
                            newPoint.lat = newLatLon[0];
                            newPoint.lon = newLatLon[1];
                        }
                    }
                    newPoint.setHAE(ATSKApplication.getAltitudeHAE(newPoint));

                    if (hitPoint) {
                        // Edit single point
                        index = index % lineSize;
                        if (index < 0)
                            index += lineSize;
                        if (remove)
                            lo.points.remove(index);
                        else
                            lo.points.set(index, newPoint);
                        Log.d(TAG, "Updated line obstruction [" + index + "] "
                                + lo.remarks);
                    } else if (!remove) {
                        // Add new point
                        if (index >= lineSize)
                            index = lineSize;
                        lo.points.add(index, newPoint);
                        Log.d(TAG, "Added point to line obstruction [" + index
                                + "] " + lo.remarks);
                    }
                    opc.EditLine(lo);
                    ret = true;
                }
                opc.Stop();
            }
        }
        return ret;
    }

    @Override
    public synchronized GeoPoint getPoint(int index) {
        // Avoid ATAK crash when splitting a line
        if (index >= 0 && index < _points.size())
            return super.getPoint(index);
        else
            Log.w(TAG, "Attempted to getPoint index "
                    + index + " when size is " + _points.size());
        return null;
    }

    @Override
    public void removePoint(int index) {
        setPoint(index, null);
    }

    @Override
    protected boolean addPointNoSync(int index, GeoPoint point) {
        setMetaString("hit_type", "line");
        return setPoint(index, point);
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (_labelLine != null)
            _labelLine.setVisible(visible);
    }

    @Override
    public void copy(int copies) {
        // Unsupported
    }

    @Override
    public void rename(String name) {
        if (_lineObs != null) {
            _lineObs.remarks = name;
            save();
        }
    }

    @Override
    public void save() {
        if (_lineObs != null) {
            ObstructionProviderClient opc =
                    new ObstructionProviderClient(mapView.getContext());
            if (opc.Start()) {
                LineObstruction lo = opc.GetLine(
                        ATSKConstants.DEFAULT_GROUP, getUID());
                if (lo != null)
                    opc.EditLine(_lineObs);
                else
                    _lineObs = null;
                opc.Stop();
            }
        }
    }

    @Override
    public void delete() {
        if (_lineObs != null) {
            ObstructionProviderClient opc = new ObstructionProviderClient(
                    MapView.getMapView().getContext());
            if (opc.Start()) {
                opc.DeleteLine(_lineObs.group, _lineObs.uid, true);
                opc.Stop();
            }
        }
        if (getGroup() != null)
            getGroup().removeItem(this);
    }

    public int getObstructionHitIndex() {
        return getObstructionHitIndex(-1);
    }

    public int getObstructionHitIndex(int index) {
        LineObstruction lo = ObstructionController.getInstance()
                .getLineObstruction(getUID());
        if (lo == null || getNumPoints() <= 0)
            return -1;
        // Just in case
        if (index == -1) {
            index = getMetaInteger("hit_index", -1);
            if (index == -1)
                return -1;
        }
        index = Math.min(Math.max(index, 0), getNumPoints() - 1);

        boolean hitPoint = getMetaString("hit_type", "point")
                .equals("point");

        int lineSize = lo.points.size();

        // Most likely a route
        if (getNumPoints() >= lineSize * 2 && lo.width > 0.0) {
            // Get left-side index (center line index in LO)
            if (index >= lineSize)
                index = (lineSize - index)
                        + (lineSize - (hitPoint ? 1 : 0));
            index = Math.min(Math.max(0, index), lineSize - 1);
        }

        index = index % lineSize;
        if (index < 0)
            index += lineSize;
        return index;
    }

    /**
     * Set visibility of label
     * @param visible True to show, false to hide
     */
    public void setLabelVisible(boolean visible) {
        boolean changed = visible != getLabelVisible();
        if (visible) {
            if (isRoute() && _labelLine != null)
                setLabel(_labelLine.getMetaString("title", null));
            removeMetaData("minRenderScale");
            setMetaBoolean(LABEL_VISIBLE, true);
            if (_lineObs != null)
                _lineObs.removeFlag(Constants.FL_HIDE_LABEL);
        } else {
            if (isRoute())
                setLabel(null);
            setMetaDouble("minRenderScale", Double.MAX_VALUE);
            removeMetaData(LABEL_VISIBLE);
            if (_lineObs != null)
                _lineObs.addFlag(Constants.FL_HIDE_LABEL);
        }
        if (changed && getGroup() != null)
            refresh(MapView.getMapView().getMapEventDispatcher(),
                    null, getClass());
    }

    /**
     * Return if the label is visible
     * @return True if label visible, false otherwise
     */
    public boolean getLabelVisible() {
        return _atskType != null && (hasMetaValue(LABEL_ALWAYS_SHOW)
                || hasMetaValue(LABEL_VISIBLE));
    }

    /**
     * Return label end points (used for survey image capture)
     * @return Label end points
     */
    public GeoPoint[] getLabelPoints() {
        if (_labelLine != null)
            return _labelLine.getPoints();
        return new GeoPoint[] {
                getLabelPoint()
        };
    }

    /**
     * Get label center point
     * @return Label center point
     */
    public GeoPoint getLabelPoint() {
        if (_labelLine != null)
            return _labelLine.getCenter();
        return GeoPoint.centerOfExtremes(getPoints(), 0, getNumPoints());
    }

    /**
     * Return upright label angle (used for survey image capture)
     * @return Label angle between -90 and 90 degrees
     */
    public double getLabelAngle() {
        if (_labelLine != null && _lineObs.hasFlag(Constants.FL_LABEL_TURNED)) {
            String pointStr = getMetaString("label_point", "");
            try {
                SurveyPoint sp = Conversions.fromJson(pointStr,
                        SurveyPoint.class);
                if (sp != null) {
                    double ang = Conversions.deg360(sp.course_true + 90);
                    if (ang > 90 && ang < 270)
                        ang += 180;
                    return Conversions.deg360(ang);
                }
            } catch (Exception e) {
                Log.w(TAG, "Failed to parse label point for "
                        + getTitle() + ": " + String.valueOf(pointStr));
            }
        }
        return 0;
    }

    public String getATSKType() {
        return _atskType;
    }

    public String getLabel() {
        if (_atskType == null)
            return "";
        if (_labelLine != null && _labelLine.hasMetaValue("title"))
            return _labelLine.getMetaString("title", "");
        else if (hasMetaValue("centerPointLabel"))
            return getMetaString("centerPointLabel", "");
        String title = getTitle();
        if (title == null)
            return "";
        return title;
    }

    public void requestEdit() {
        if (_atskType == null)
            return;
        Intent intent = new Intent();
        intent.setAction(ToolManagerBroadcastReceiver.BEGIN_TOOL);
        intent.putExtra("tool", ShapeEditTool.TOOL_IDENTIFIER);
        intent.putExtra("uid", getUID());
        intent.putExtra("ignoreToolbar", true);
        intent.putExtra("scaleToFit", false);
        AtakBroadcast.getInstance().sendBroadcast(intent);
    }

    @Override
    public void setEditable(boolean editable) {
        if (_atskType == null)
            return;
        int strokeColor = getStrokeColor();
        super.setEditable(editable);
        setStrokeColor(strokeColor);
    }

    @Override
    public boolean getEditable() {
        return getSuperEditable() || _testOrtho;
    }

    public boolean getSuperEditable() {
        return super.getEditable();
    }

    // Workaround to get the correct hit from EditablePolyline.testOrthoHit
    // when not in edit mode (getEditable() must return true for this to work)
    @Override
    public boolean testOrthoHit(int xpos, int ypos, GeoPoint point, MapView view) {
        _testOrtho = true;
        boolean ret = super.testOrthoHit(xpos, ypos, point, view);
        _testOrtho = false;
        return ret;
    }

    @Override
    protected String getShapeMenu() {
        if (_menu != null)
            return _menu;
        String menu = "menus/obs_shape_menu.xml";
        if (isRoute()) {
            if (this instanceof ATSKLineLeader)
                menu = "menus/obs_line_leader_menu.xml";
            else
                menu = "menus/obs_route_menu.xml";
        }
        return ATSKMenuLoader.loadMenu(menu);
    }

    // Suppress edit-mode menus
    @Override
    protected String getLineMenu() {
        return super.getEditable() ? super.getLineMenu() : getShapeMenu();
    }

    @Override
    protected String getCornerMenu() {
        return super.getEditable() ? super.getCornerMenu() : getShapeMenu();
    }

    public void setShapeMenu(String menu) {
        _menu = menu;
        setMetaString("menu", menu);
        refresh(mapView.getMapEventDispatcher(), null,
                ATSKShape.class);
    }

    public boolean isRoute() {
        return getMetaString("obsType", "area").equals("route");
    }

    public void copyFrom(ATSKShape other) {
        setMetaString("centerPointLabel",
                other.getMetaString("centerPointLabel", ""));
        setMetaString("callsign", other.getMetaString("callsign", ""));
        setMetaString("title", other.getMetaString("title", ""));
        setMetaString("shapeName", other.getMetaString("shapeName", ""));
        setMetaString("label_point", other.getMetaString("label_point", ""));
        setPoints(other.getPoints());
        setStrokeColor(other.getStrokeColor());
        if (other.getFilled())
            setFillColor(other.getFillColor());
        setLabel(getMetaString("title", null));
        updateLabelLine();
    }

    @Override
    public boolean getClickable() {
        if (_atskType == null)
            return false;
        removeMetaData("submenu_map");
        return !ATSKFragment.isMapState()
                && ATSKFragment.isMapType(_atskType)
                && super.getClickable();
    }

    @Override
    public void persist(MapEventDispatcher dispatcher,
            Bundle persistExtras, Class clazz) {
    }

    public void setRotateLabel(boolean enable) {
        if (enable) {
            setMetaBoolean("rotate_label", true);
            if (_lineObs != null)
                _lineObs.addFlag(Constants.FL_LABEL_TURNED);
        } else {
            removeMetaData("rotate_label");
            if (_lineObs != null)
                _lineObs.removeFlag(Constants.FL_LABEL_TURNED);
        }
    }

    public void updateLabelLine() {
        if (this instanceof ATSKLineLeader || !isRoute()
                || _points.size() < 2 || getGroup() == null
                || !getLabelVisible()) {
            removeLabelLine();
            return;
        }
        if (_labelLine == null)
            _labelLine = new EditablePolyline(mapView, getUID() + "_label");
        if (_labelLine.getGroup() == null)
            getGroup().addItem(_labelLine);
        _labelLine.setStrokeWeight(0);
        _labelLine.setStrokeColor(0);
        _labelLine.clearWithoutNotify();

        // Route center point is stored in JSON string
        // Otherwise we would need access to the line obstruction
        // to do this calculation, which requires (often repetitive) OPC start/stop
        String pointStr = getMetaString("label_point", "");
        if (pointStr != null && !pointStr.isEmpty()) {
            try {
                SurveyPoint center = Conversions.fromJson(pointStr,
                        SurveyPoint.class);
                if (center != null) {
                    if (!hasMetaValue("rotate_label"))
                        center.course_true = 270;
                    // bounding circle
                    double len = Conversions.CalculateRangem(
                            minimumBoundingBox.getSouth(),
                            minimumBoundingBox.getWest(),
                            minimumBoundingBox.getNorth(),
                            minimumBoundingBox.getEast()) / 2;
                    double[] pos = Conversions.AROffset(center.lat,
                            center.lon, center.course_true, len);
                    _labelLine.addPoint(new GeoPoint(pos[0], pos[1]));
                    pos = Conversions.AROffset(center.lat,
                            center.lon, center.course_true + 180, len);
                    _labelLine.addPoint(new GeoPoint(pos[0], pos[1]));
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to parse invalid label point.", e);
            }
        }
    }

    public void setLabel(String txt) {
        if (_labelLine == null)
            _labelLine = new EditablePolyline(mapView, getUID() + "_label");
        if (_groupListener == null) {
            _groupListener = new OnGroupChangedListener() {
                @Override
                public void onItemAdded(MapItem item, MapGroup group) {
                    updateLabelLine();
                }

                @Override
                public void onItemRemoved(MapItem item, MapGroup group) {
                    removeLabelLine();
                    if (_groupListener != null)
                        removeOnGroupChangedListener(_groupListener);
                    _groupListener = null;
                }
            };
            addOnGroupChangedListener(_groupListener);
        }
        if (txt == null || txt.isEmpty()) {
            _labelLine.removeMetaData("labels");
            _labelLine.removeMetaData("title");
            _labelLine.setLabels(null);
            return;
        }
        Map<String, Object> labels = new HashMap<String, Object>();
        Map<String, Object> segment = new HashMap<String, Object>();
        segment.put("segment", 0);
        segment.put("text", txt);
        labels.put("segment", segment);
        _labelLine.setMetaMap("labels", labels);
        _labelLine.setLabels(labels);
        _labelLine.setMetaString("title", txt);
        _labelLine.setMetaBoolean("staticLabel", true);
        setMetaString("title", txt);
    }

    public void removeLabelLine() {
        if (_labelLine != null && _labelLine.getGroup() != null)
            _labelLine.getGroup().removeItem(_labelLine);
        _labelLine = null;
    }

    public void showLineMeasurements() {
        Map<String, Object> labels = new HashMap<String, Object>();
        for (int i = 0; i < _points.size() - 1; i++) {
            double range = _points.get(i).distanceTo(_points.get(i + 1));
            Map<String, Object> segment = new HashMap<String, Object>();
            segment.put("segment", i);
            segment.put(
                    "text",
                    String.format(LocaleUtil.getCurrent(), "%.0f %s",
                            Unit.METER.convertTo(range, Unit.FOOT),
                            Unit.FOOT.getAbbr()));
            labels.put("seg" + i, segment);
        }
        setMetaMap("labels", labels);
        setLabels(labels);
        setMetaBoolean("staticLabel", true);
    }

    public static ATSKShape find(String uid) {
        MapGroup atskGroup = MapView.getMapView().getRootGroup()
                .findMapGroup(ATSKATAKConstants.ATSK_MAP_GROUP_OBS);
        if (atskGroup != null) {
            MapItem item = atskGroup.deepFindUID(uid);
            if (item instanceof ATSKShape)
                return (ATSKShape) item;
        }
        return null;
    }

    @Override
    public String getKMLDescription() {
        StringBuilder desc = new StringBuilder();
        List<Data> data = kmlDataList();
        for (int i = 0; i < data.size(); i++) {
            desc.append(data.get(i).getDisplayName());
            desc.append(": ");
            desc.append(data.get(i).getValue());
            if (i < data.size() - 1)
                desc.append("\n");
        }
        return desc.toString();
    }

    @Override
    protected List<Data> kmlDataList() {
        List<Data> dataList = new ArrayList<Data>();
        if (_lineObs != null) {
            SurveyPoint center = _lineObs.getCenter();
            center.setHAE(ATSKApplication.getAltitudeHAE(center));
            String name = _lineObs.remarks;
            if (name != null && !name.isEmpty() && !name.equals("none"))
                dataList.add(data("Name", "name", name));
            dataList.add(data("Type", "type", _lineObs.type));
            dataList.add(data("Center Location", "center",
                    Conversions.GetMGRS(center.lat, center.lon)));
            dataList.add(data("Center Elevation", "elevation", center
                    .getMSLAltitude().toString(Conversions.Unit.FOOT)));
            dataList.add(data("Height", "height", getFt(_lineObs.height)));
            dataList.add(data("Length", "length", getFt(_lineObs.getLength())));
            dataList.add(data("Width", "width", getFt(_lineObs.width)));
        }
        return dataList;
    }

    private static Data data(String display, String key, String value) {
        Data d = new Data();
        d.setDisplayName(display);
        d.setName(key);
        d.setValue(value);
        return d;
    }

    @Override
    protected Folder toKml() {
        // Skip empty shapes (this shouldn't happen anyway)
        GeoPoint[] points = getPoints();
        if (points == null || points.length < 1)
            return null;
        try {
            // style element
            Style style = new Style();

            // Hack to make pushpin invisible
            IconStyle istyle = new IconStyle();
            istyle.setColor(KMLUtil.convertKmlColor(0x00000000));
            com.ekito.simpleKML.model.Icon icon = new com.ekito.simpleKML.model.Icon();
            String whtpushpin = context.getString(
                    com.atakmap.app.R.string.whtpushpin);
            icon.setHref(whtpushpin);
            istyle.setIcon(icon);
            style.setIconStyle(istyle);

            LineStyle lstyle = new LineStyle();
            lstyle.setColor(KMLUtil.convertKmlColor(getStrokeColor()));
            lstyle.setWidth((float) getStrokeWeight());
            style.setLineStyle(lstyle);

            PolyStyle pstyle = new PolyStyle();
            pstyle.setColor(KMLUtil.convertKmlColor(getFillColor()));
            pstyle.setFill(determineIfFilled());
            pstyle.setOutline(1);
            style.setPolyStyle(pstyle);

            String styleId = KMLUtil.hash(style);
            style.setId(styleId);

            // Folder element containing styles, shape and label
            Folder folder = new Folder();
            folder.setName(kmlFolderName());

            List<StyleSelector> styles = new ArrayList<StyleSelector>();
            styles.add(style);
            folder.setStyleSelector(styles);
            List<Feature> folderFeatures = new ArrayList<Feature>();
            folder.setFeatureList(folderFeatures);

            List<Data> dataList = kmlDataList();
            ExtendedData edata = new ExtendedData();
            edata.setDataList(dataList);

            // So the function doesn't crash...
            Placemark outmark = createOuterPlacemark(styleId);

            // Shape label
            String label = getLabel();
            if (!getLabelVisible() || label.isEmpty())
                label = null;
            Coordinate labelCoord = KMLUtil.convertKmlCoord(getLabelPoint(),
                    false);
            if (label != null && labelCoord != null) {
                Point labelPoint = new Point();
                labelPoint.setCoordinates(labelCoord);
                labelPoint.setAltitudeMode("absolute");

                Placemark pmark = new Placemark();
                pmark.setId(getUID() + "_label");
                pmark.setName(label);
                pmark.setStyleUrl("#" + styleId);
                pmark.setVisibility(getVisible() ? 1 : 0);

                List<Geometry> labelGeo = new ArrayList<Geometry>();
                labelGeo.add(labelPoint);
                pmark.setGeometryList(labelGeo);
                folderFeatures.add(pmark);
            }
            folder.setDescription(getKMLDescription());
            return createKmlGeometry(folder, outmark, edata, styleId,
                    folderFeatures);
        } catch (Exception e) {
            Log.e(TAG, "Export of " + getClass().getSimpleName() +
                    " to KML failed with Exception", e);
        }
        return null;
    }

    @Override
    public Bundle preDrawCanvas(CapturePP cap) {
        Bundle data = super.preDrawCanvas(cap);
        GeoPoint[] points = getLabelPoints();
        if (points != null) {
            PointF[] labelPoints = new PointF[points.length];
            for (int i = 0; i < points.length; i++)
                labelPoints[i] = cap.forward(points[i]);
            data.putSerializable("labelPoints", labelPoints);
        }
        return data;
    }

    @Override
    public void drawCanvas(CapturePP cap, Bundle data) {
        super.drawCanvas(cap, data);

        PointF[] lp = (PointF[]) data
                .getSerializable("labelPoints");
        // Center label is already taken care of
        if (lp != null && lp.length == 2) {
            String label = getLabel();
            if (getLabelVisible() && cap.shouldDrawLabel(label, lp)) {
                PointA labelPos = new PointA((lp[0].x + lp[1].x) / 2,
                        (lp[0].y + lp[1].y) / 2, (float) getLabelAngle());
                cap.drawLabel(label, labelPos);
            }
        }

        // Draw measurement labels
        PointF[] p = (PointF[]) data.getSerializable("points");
        Map<String, Object> labels = getLabels();
        if (p != null && labels != null && !labels.isEmpty()) {
            for (int i = 0; i < labels.size(); i++) {
                if (i + 1 >= p.length)
                    break;
                Map<String, Object> seg = (Map<String, Object>) labels
                        .get("seg" + i);
                if (seg == null)
                    continue;
                String txt = String.valueOf(seg.get("text"));
                PointA pos = new PointA((p[i].x + p[i + 1].x) / 2,
                        (p[i].y + p[i + 1].y) / 2, 0);
                pos.angle = (float) Math.toDegrees(Math.atan2(p[i].y
                        - p[i + 1].y,
                        p[i].x - p[i + 1].x));
                if (cap.shouldDrawLabel(txt, new PointF[] {
                        p[i], p[i + 1]
                }))
                    cap.drawLabel(txt, pos);
            }
        }
    }

    private static String getFt(double meters) {
        return Conversions.Unit.METER.format(meters, Conversions.Unit.FOOT);
    }
}
