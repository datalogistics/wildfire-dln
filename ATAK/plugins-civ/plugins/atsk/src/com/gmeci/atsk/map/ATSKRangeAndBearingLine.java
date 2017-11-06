
package com.gmeci.atsk.map;

import android.graphics.Color;
import android.os.Bundle;

import com.atakmap.android.importexport.ExportFilters;
import com.atakmap.android.maps.MapEventDispatcher;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.PointMapItem;
import com.atakmap.android.toolbars.RangeAndBearingEndpoint;
import com.atakmap.android.toolbars.RangeAndBearingMapItem;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.coords.DistanceCalculations;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.spatial.file.export.KMZFolder;
import com.atakmap.spatial.kml.KMLUtil;
import com.ekito.simpleKML.model.Coordinates;
import com.ekito.simpleKML.model.Feature;
import com.ekito.simpleKML.model.Folder;
import com.ekito.simpleKML.model.Geometry;
import com.ekito.simpleKML.model.IconStyle;
import com.ekito.simpleKML.model.LineString;
import com.ekito.simpleKML.model.LineStyle;
import com.ekito.simpleKML.model.Placemark;
import com.ekito.simpleKML.model.Point;
import com.ekito.simpleKML.model.Style;
import com.ekito.simpleKML.model.StyleSelector;
import com.gmeci.atsk.MapHelper;
import com.gmeci.atsk.resources.ATSKMenuLoader;
import com.gmeci.atskservice.resolvers.ObstructionProviderClient;
import com.gmeci.conversions.Conversions;
import com.gmeci.core.ATSKConstants;
import com.gmeci.core.LineObstruction;
import com.gmeci.core.SurveyPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * Range and bearing line with customizable label
 */

public class ATSKRangeAndBearingLine extends RangeAndBearingMapItem
        implements ATSKMapItem {

    private boolean _initFinished = false;
    private final PointMapItem _pt1, _pt2;

    public ATSKRangeAndBearingLine(PointMapItem pt1, PointMapItem pt2,
            MapView mapView, final LineObstruction lo) {
        super(pt1, pt2, mapView, lo.uid, false);
        _pt1 = pt1;
        _pt2 = pt2;
        initEndPoint(pt1);
        initEndPoint(pt2);
        if (!getUID().equals(ATSKConstants.TEMP_LINE_UID)) {
            setMetaString("menu", ATSKMenuLoader
                    .loadMenu("menus/atsk_rab_menu.xml"));
            setClickable(true);
        }
        setLabel(lo.remarks);
        if (Color.alpha(lo.flags) == 255)
            setStrokeColor(lo.flags);
        setMetaString("obsType", "R&B");
        _initFinished = true;
    }

    private void initEndPoint(PointMapItem pt) {
        pt.setMetaBoolean("nevercot", true);
        pt.setMetaBoolean("archive", false);
        if (!getUID().equals(ATSKConstants.TEMP_LINE_UID)) {
            pt.setMetaString("menu", "menus/rab_endpoint_menu.xml");
            pt.setClickable(true);
        }
        if (pt instanceof RangeAndBearingEndpoint)
            ((RangeAndBearingEndpoint) pt).setParent(this);
    }

    @Override
    public void setText(String text) {
        String label = getMetaString("remarks", "");
        if (!label.isEmpty() && !label.equals("none"))
            super.setText(label);
        else
            super.setText(text);
    }

    @Override
    public void rename(String name) {
        setLabel(name);
        save();
    }

    private void setLabel(String name) {
        setMetaString("remarks", name);
        if (name.isEmpty() || name.equals("none")) {
            // Restore default label
            // updateLabel is private so we have to force a refresh
            setBearingUnits(getBearingUnits());
        } else
            setText(name);
    }

    @Override
    public void onPointChanged(PointMapItem item) {
        super.onPointChanged(item);
        save();
    }

    @Override
    public void copy(int copies) {
    }

    @Override
    public void setLabelVisible(boolean visible) {
    }

    @Override
    public boolean getLabelVisible() {
        return true;
    }

    @Override
    public void save() {
        if (!_initFinished)
            return;
        ObstructionProviderClient opc =
                new ObstructionProviderClient(_mapView.getContext());
        if (opc.Start()) {
            LineObstruction lo = opc.GetLine(
                    ATSKConstants.DEFAULT_GROUP, getUID());
            if (lo != null) {
                SurveyPoint pt1 = MapHelper
                        .convertGeoPoint2SurveyPoint(getPoint1());
                SurveyPoint pt2 = MapHelper
                        .convertGeoPoint2SurveyPoint(getPoint2());
                String remarks = getMetaString("remarks", "");
                int color = getStrokeColor() | 0xFF000000;
                // Check for some difference before re-saving it
                if (Conversions.calculateRange(pt1,
                        lo.points.get(0)) > Conversions.THRESH
                        || Conversions.calculateRange(pt2,
                                lo.points.get(1)) > Conversions.THRESH
                        || !lo.remarks.equals(remarks)
                        || lo.flags != color) {
                    lo.points.set(0, pt1);
                    lo.points.set(1, pt2);
                    lo.remarks = remarks;
                    lo.flags = color;
                    opc.EditLine(lo);
                }
            }
            opc.Stop();
        }
    }

    @Override
    public void delete() {
        ObstructionProviderClient opc =
                new ObstructionProviderClient(_mapView.getContext());
        if (opc.Start()) {
            opc.DeleteLine(ATSKConstants.DEFAULT_GROUP, getUID());
            opc.Stop();
        }
        deleteItem(this);
        deleteItem(_pt1);
        deleteItem(_pt2);
        dispose();
    }

    private static void deleteItem(MapItem item) {
        if (item != null && item.getGroup() != null)
            item.getGroup().removeItem(item);
    }

    @Override
    public String getKMLDescription() {
        return "";
    }

    @Override
    public void persist(MapEventDispatcher dispatcher,
            Bundle persistExtras, Class clazz) {
    }

    private Folder toKml() {

        if (_point1 == null || _point2 == null) {
            Log.w(TAG, "Unable to create KML Folder without 2 points");
            return null;
        }

        try {
            // style inner ring
            Style style = new Style();
            LineStyle lstyle = new LineStyle();
            lstyle.setColor(KMLUtil.convertKmlColor(getStrokeColor()));
            lstyle.setWidth(2F);
            style.setLineStyle(lstyle);

            IconStyle istyle = new IconStyle();
            istyle.setColor(KMLUtil.convertKmlColor(getStrokeColor()));
            com.ekito.simpleKML.model.Icon icon = new com.ekito.simpleKML.model.Icon();
            icon.setHref("http://maps.google.com/mapfiles/kml/shapes/cross-hairs.png");
            istyle.setScale(0.5f);
            istyle.setIcon(icon);
            style.setIconStyle(istyle);

            String styleId = KMLUtil.hash(style);
            style.setId(styleId);

            // Folder element containing styles, shape and label
            Folder folder = new Folder();
            folder.setName(getText());
            List<StyleSelector> styles = new ArrayList<StyleSelector>();
            styles.add(style);
            folder.setStyleSelector(styles);
            List<Feature> folderFeatures = new ArrayList<Feature>();
            folder.setFeatureList(folderFeatures);

            // line between the two points
            Placemark linePlacemark = new Placemark();
            linePlacemark.setName(getText());
            linePlacemark.setDescription(getText());
            linePlacemark.setId(getUID() + getText());
            linePlacemark.setStyleUrl("#" + styleId);
            linePlacemark.setVisibility(getVisible() ? 1 : 0);

            // Include arrow as part of the shape
            double range = 10, bearing = _point1.bearingTo(_point2);
            GeoPoint[] pts = {
                    _point1, _point2,
                    DistanceCalculations.computeDestinationPoint(
                            _point2, bearing + 135, range),
                    _point2,
                    DistanceCalculations.computeDestinationPoint(
                            _point2, bearing - 135, range)
            };
            Coordinates coordinates = new Coordinates(KMLUtil.convertKmlCoords(
                    pts, true));
            LineString lineString = new LineString();
            lineString.setCoordinates(coordinates);
            lineString.setAltitudeMode("clampToGround");

            List<Geometry> geomtries = new ArrayList<Geometry>();
            geomtries.add(lineString);
            linePlacemark.setGeometryList(geomtries);
            folderFeatures.add(linePlacemark);

            // Add mid-line label
            Placemark labelPlacemark = new Placemark();
            labelPlacemark.setName(getText());
            labelPlacemark.setId(getUID() + getText() + "_label");
            labelPlacemark.setStyleUrl("#" + styleId);
            labelPlacemark.setVisibility(getVisible() ? 1 : 0);

            Point point = new Point();
            point.setAltitudeMode("clampToGround");
            point.setCoordinates(KMLUtil.convertKmlCoord(GeoPoint
                    .midPoint(_point1, _point2), true));
            geomtries = new ArrayList<Geometry>();
            geomtries.add(point);
            labelPlacemark.setGeometryList(geomtries);
            folderFeatures.add(labelPlacemark);

            return folder;
        } catch (Exception e) {
            Log.e(TAG, "Export of DrawingCircle to KML failed", e);
        }

        return null;
    }

    private KMZFolder toKmz() {
        Folder f = toKml();
        if (f == null)
            return null;
        return new KMZFolder(f);
    }

    @Override
    public Object toObjectOf(Class target, ExportFilters filters) {
        if (filters != null && filters.filter(this))
            return null;

        if (Folder.class.equals(target))
            return toKml();
        else if (KMZFolder.class.equals(target))
            return toKmz();

        return super.toObjectOf(target, filters);
    }
}
