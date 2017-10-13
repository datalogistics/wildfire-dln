
package com.gmeci.atsk.map;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.os.Bundle;

import com.atakmap.android.imagecapture.CapturePP;
import com.atakmap.android.maps.Arrow;
import com.atakmap.android.maps.DefaultMetaDataHolder;
import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.MetaDataHolder;
import com.atakmap.android.maps.Shape;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.coords.DistanceCalculations;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.spatial.kml.KMLUtil;
import com.ekito.simpleKML.model.Coordinates;
import com.ekito.simpleKML.model.Feature;
import com.ekito.simpleKML.model.Folder;
import com.ekito.simpleKML.model.Geometry;
import com.ekito.simpleKML.model.IconStyle;
import com.ekito.simpleKML.model.LineString;
import com.ekito.simpleKML.model.LineStyle;
import com.ekito.simpleKML.model.Placemark;
import com.ekito.simpleKML.model.Style;
import com.ekito.simpleKML.model.StyleSelector;
import com.gmeci.atsk.MapHelper;
import com.gmeci.conversions.Conversions;
import com.gmeci.core.ATSKConstants;
import com.gmeci.core.LineObstruction;
import com.gmeci.core.SurveyPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * Line leader with arrow tip
 */
public class ATSKLineLeader extends ATSKShape {

    public static final String TAG = "ATSKLineLeader";

    private LeaderArrow _arrow;

    public ATSKLineLeader(MapView map, String atskType, LineObstruction lo) {
        super(map, atskType, lo);
        init();
    }

    private void init() {
        addOnGroupChangedListener(new OnGroupChangedListener() {
            @Override
            public void onItemAdded(MapItem item, MapGroup group) {
                updateArrow();
            }

            @Override
            public void onItemRemoved(MapItem item, MapGroup group) {
                removeArrow();
            }
        });
        addOnPointsChangedListener(new OnPointsChangedListener() {
            @Override
            public void onPointsChanged(Shape s) {
                updateArrow();
            }
        });
        setMetaBoolean(ATSKMapItem.LABEL_ALWAYS_SHOW, true);
    }

    public ATSKLabel getParent() {
        ATSKMarker parent = ATSKMarker.find(getUID().substring(0,
                getUID().indexOf(ATSKConstants.LEADER_SUFFIX)));
        if (parent != null && parent instanceof ATSKLabel)
            return (ATSKLabel) parent;
        return null;
    }

    @Override
    public void rename(String name) {
        ATSKLabel parent = getParent();
        if (parent != null)
            parent.rename(name);
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (_arrow != null)
            _arrow.setVisible(visible);
    }

    private void updateArrow() {
        if (_points.size() < 2 || getGroup() == null) {
            removeArrow();
            return;
        }
        if (_arrow == null) {
            _arrow = new LeaderArrow(getUID() + "_arrow");
            getGroup().addItem(_arrow);
        }
        //_arrow.setStrokeWeight(getStrokeWeight());
        _arrow.setStrokeColor(getStrokeColor());
        _arrow.setText("");

        // Create arrow points
        SurveyPoint first = MapHelper.convertGeoPoint2SurveyPoint(
                _points.get(0));
        SurveyPoint second = MapHelper.convertGeoPoint2SurveyPoint(
                _points.get(1));
        double reverseAng = Conversions.calculateAngle(first, second);
        double[] rab = Conversions.AROffset(first.lat, first.lon,
                reverseAng, 0.1);
        _arrow.setPoint1(new GeoPoint(rab[0], rab[1]));
        _arrow.setPoint2(new GeoPoint(first.lat, first.lon));
    }

    private void removeArrow() {
        if (_arrow != null && _arrow.getGroup() != null)
            _arrow.getGroup().removeItem(_arrow);
        _arrow = null;
    }

    @Override
    protected Folder toKml() {
        ATSKLabel parent = getParent();
        if (_arrow == null || parent == null || _points.size() < 2)
            return super.toKml();
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
            String title = parent.getTitle();
            Folder folder = new Folder();
            folder.setName(title);
            List<StyleSelector> styles = new ArrayList<StyleSelector>();
            styles.add(style);
            folder.setStyleSelector(styles);
            List<Feature> folderFeatures = new ArrayList<Feature>();
            folder.setFeatureList(folderFeatures);

            // line between the two points
            Placemark linePlacemark = new Placemark();
            linePlacemark.setName(title);
            linePlacemark.setDescription(title);
            linePlacemark.setId(getUID() + title);
            linePlacemark.setStyleUrl("#" + styleId);
            linePlacemark.setVisibility(getVisible() ? 1 : 0);

            // Include arrow as part of the shape
            GeoPoint p1 = _points.get(1), p2 = _points.get(0);
            double range = 10, bearing = p1.bearingTo(p2);
            GeoPoint[] pts = {
                    p1, p2,
                    DistanceCalculations.computeDestinationPoint(
                            p2, bearing + 135, range),
                    p2,
                    DistanceCalculations.computeDestinationPoint(
                            p2, bearing - 135, range)
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

            return folder;
        } catch (Exception e) {
            Log.e(TAG, "Export of DrawingCircle to KML failed", e);
        }

        return null;
    }

    public static class LeaderArrow extends Arrow {

        public LeaderArrow(final String uid) {
            this(MapItem.createSerialId(), new DefaultMetaDataHolder(), uid);
        }

        public LeaderArrow(long serialId, MetaDataHolder metadata, String uid) {
            super(serialId, metadata, uid);
        }

        @Override
        public Bundle preDrawCanvas(CapturePP cap) {
            Bundle data = super.preDrawCanvas(cap);
            PointF[] p = (PointF[]) data.getSerializable("points");
            int pLen;
            if (p == null || (pLen = p.length) < 2)
                return data;
            PointF[] head = new PointF[] {
                    p[pLen - 3], p[pLen - 2], p[pLen - 1]
            };
            data.putSerializable("arrowHead", head);
            p[pLen - 3] = p[pLen - 2] = p[pLen - 1] = null;
            data.putSerializable("points", p);
            return data;
        }

        @Override
        public void drawCanvas(CapturePP cap, Bundle data) {
            // Draw arrow head
            PointF[] head = (PointF[]) data.getSerializable("arrowHead");
            if (head != null && head.length == 3) {
                Canvas can = cap.getCanvas();
                Path path = cap.getPath();
                Paint paint = cap.getPaint();
                float dr = cap.getResolution();
                float lineWeight = cap.getLineWeight();
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth((float) (getStrokeWeight()
                        + 2f) * lineWeight);
                paint.setColor(getStrokeColor());
                path.moveTo(dr * head[0].x, dr * head[0].y);
                for (PointF p : head)
                    path.lineTo(dr * p.x, dr * p.y);
                can.drawPath(path, paint);
                path.reset();
            }
            super.drawCanvas(cap, data);
        }
    }
}
