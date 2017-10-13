
package com.gmeci.atsk.map;

import android.graphics.Color;

import com.atakmap.android.editableShapes.EditablePolyline;
import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Polyline;
import com.atakmap.android.util.ATAKUtilities;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.map.AtakMapController;
import com.gmeci.atsk.MapHelper;
import com.gmeci.atsk.resources.ATSKMenuLoader;
import com.gmeci.constants.Constants;
import com.gmeci.core.ATSKConstants;
import com.gmeci.core.ATSKIntentConstants;
import com.gmeci.atsk.ATSKATAKConstants;
import com.gmeci.conversions.Conversions;
import com.gmeci.core.LineObstruction;

import java.util.Iterator;

public class ATSKDrawingTool {

    private final static String TAG = "ATSKDrawingTool";
    public static final int FILL_MASK = 0xCF000000;
    private final MapView _mapView;
    private final MapGroup _mapGroup;

    public ATSKDrawingTool(final MapView map, final MapGroup group) {
        _mapView = map;
        _mapGroup = group;
    }

    public void drawMapItem(final MapItem item, final String id) {
        drawMapItem(item, id, _mapGroup);
    }

    public void drawMapItem(final MapItem item) {
        drawMapItem(item, item.getMetaString("uid", "none"), _mapGroup);
    }

    public synchronized void drawMapItem(final MapItem item, final String uid,
            final MapGroup group) {
        if (item == null || item.getMetaString("uid", "none") == null
                || group == null)
            return;

        Iterator<MapItem> iter = group.getItems().iterator();
        MapItem oldItem = null;
        while (iter.hasNext()) {
            MapItem next = iter.next();
            if (next.getMetaString("uid", "none").equals(uid)) { // found the map item in our list -- mark for delete +
                // redraw
                // List return by getItems() is immutable - must remove from group after the fact
                oldItem = next;
                break;
            }
        }
        // map item existed already -- remove and read with updated properties.
        boolean editUpdate = false;
        if (oldItem != null) {
            // Check if the redraw is a result of editing
            editUpdate = item instanceof ATSKShape
                    && oldItem instanceof ATSKShape
                    && ((ATSKShape) oldItem).getSuperEditable();
            /*if(editUpdate)
                item.setEditable(true);*/
            if (editUpdate)
                ((ATSKShape) oldItem).copyFrom((ATSKShape) item);
            else
                group.removeItem(oldItem);
        }
        if (!editUpdate)
            group.addItem(item);
        /*if(editUpdate)
            ((ATSKShape)item).requestEdit();*/
    }

    public void centerMapOnLocation(final GeoPoint center) {
        Log.d(TAG, "center on location: " + center);
        AtakMapController controller = _mapView.getMapController();
        controller.panTo(center, false);
    }

    public void centerAndZoomMapOnLocation(final GeoPoint center,
            final double length_m) {

        Log.d(TAG, "center and zoom into location: " + center);

        //get top left from
        double[] topLeft = Conversions.AROffset(center.getLatitude(),
                center.getLongitude(), -45, length_m / 2);
        double[] botRight = Conversions.AROffset(center.getLatitude(),
                center.getLongitude(), 135, length_m / 2);

        ATAKUtilities.scaleToFit(_mapView,
                new GeoPoint[] {
                        new GeoPoint(topLeft[0], topLeft[1]),
                        new GeoPoint(botRight[0], botRight[1])
                },
                _mapView.getWidth(), _mapView.getHeight());

    }

    public void drawCircle(final MapGroup group, final GeoPoint center,
            final double radius_m, final String uID, final String name,
            final int lineColor) {

        ATSKCircle circle =
                new ATSKCircle(name, uID, group.addGroup(uID),
                        center, radius_m, lineColor, true);
        circle.setFillColor(lineColor - FILL_MASK);
        drawMapItem(circle, uID, group);
    }

    public void drawGradientLine(final GeoPoint[] linepoints, final String uid,
            final int lineColor) {

        Polyline gradientLine = new Polyline(uid);
        gradientLine.setPoints(linepoints);
        gradientLine.setStrokeWeight(ATSKATAKConstants.LINE_WEIGHT);
        gradientLine.setStrokeColor(lineColor);
        gradientLine.setClickable(false);

        if (lineColor == Color.MAGENTA)
            gradientLine.setZOrder(ATSKATAKConstants.Z_ORDER_GRADIENT_SELECTED);

        drawMapItem(gradientLine, uid);
    }

    public void drawLZLine(final MapGroup azGroup, final GeoPoint[] points,
            final String uID, String label,
            final int lineColor, final boolean isFilled) {

        if (points == null || points.length < 1)
            return;

        ATSKShape epoly = new ATSKShape(_mapView, azGroup.addGroup(uID),
                uID, ATSKConstants.CURRENT_SCREEN_AZ);
        epoly.setPoints(points);
        epoly.setStrokeColor(lineColor);
        epoly.setShapeMenu(ATSKMenuLoader.loadMenu("menus/az_menu.xml"));
        if (isFilled) {
            epoly.setFilled(true);
            epoly.setFillColor(lineColor - FILL_MASK);
        } else
            epoly.setFilled(false);
        if (uID.contains(ATSKConstants.LZ_CENTER_LINE)) {
            epoly.setLineStyle(EditablePolyline.BASIC_LINE_STYLE_DASHED);
            epoly.setZOrder(ATSKATAKConstants.Z_ORDER_LZ_THRESH);
        }

        if (uID.contains(ATSKConstants.LZ_MAIN)
                || uID.contains(ATSKConstants.DISPLACED_THRESHHOLD))//Z_ORDER_LZ_THRESH
            epoly.setZOrder(ATSKATAKConstants.Z_ORDER_LZ_RUNWAY);
        else if (uID.contains(ATSKConstants.DISPLACED_THRESHHOLD))
            epoly.setZOrder(ATSKATAKConstants.Z_ORDER_LZ_THRESH);
        else
            epoly.setZOrder(ATSKATAKConstants.Z_ORDER_PARKING_LINE);

        epoly.setMetaString("shapeName", label);

        drawMapItem(epoly, uID, azGroup);
    }

    public ATSKShape drawAZLine(final MapGroup group, LineObstruction lo,
            String label, final int lineColor, final boolean filled) {
        return drawAZLine(group, lo, label, lineColor, false, filled);
    }

    public ATSKShape drawAZLine(final MapGroup group, LineObstruction lo,
            String label, final int lineColor, final boolean dashed,
            final boolean filled) {

        if (lo.points == null || lo.points.isEmpty())
            return null;

        ATSKShape epoly = new ATSKShape(_mapView, group.addGroup(lo.uid),
                lo.uid, ATSKConstants.CURRENT_SCREEN_AZ);
        epoly.setPoints(MapHelper.convertSurveyPoint2GeoPoint(lo.points));
        epoly.setStrokeColor(lineColor);
        epoly.setShapeMenu(ATSKMenuLoader.loadMenu("menus/az_menu.xml"));
        epoly.setFilled(filled);
        if (filled)
            epoly.setFillColor(lineColor - FILL_MASK);
        epoly.setMetaString("shapeName", label);
        if (dashed)
            epoly.setBasicLineStyle(Polyline.BASIC_LINE_STYLE_DASHED);
        if (lo.hasFlag(Constants.FL_LABEL_MEASURE_LINES))
            epoly.showLineMeasurements();
        drawMapItem(epoly, lo.uid, group);
        return epoly;
    }

    public void drawParkingLine(final MapGroup apronGroup,
            final GeoPoint[] points,
            final String uID, final int lineColor, final boolean filled) {

        if (points == null || points.length < 1)
            return;

        ATSKShape epoly = new ATSKShape(_mapView, apronGroup.addGroup(uID),
                uID, ATSKConstants.CURRENT_SCREEN_PARKING);
        epoly.setPoints(points);
        epoly.setStrokeColor(lineColor);
        epoly.setMetaString(
                ATSKIntentConstants.OB_TYPE_SELECTED,
                ATSKIntentConstants.OB_STATE_POINT);
        epoly.setMetaString(ATSKIntentConstants.OB_MENU_GROUP,
                ATSKConstants.APRON_GROUP);
        epoly.setZOrder(ATSKATAKConstants.Z_ORDER_PARKING_LINE);
        if (filled) {
            epoly.setFilled(true);
            epoly.setFillColor(lineColor - FILL_MASK);
        } else
            epoly.setFilled(false);

        drawMapItem(epoly, uID, apronGroup);
    }

    public void drawParkingAircraft(final MapGroup apronGroup,
            final GeoPoint[] linepoints,
            final String uid, final int lineColor) {
        Polyline aircraftLine = new Polyline(uid);
        aircraftLine.setPoints(linepoints);
        Log.d(TAG, "drawParkingAircraft was:" + linepoints.length);
        aircraftLine.setStrokeWeight(ATSKATAKConstants.LINE_WEIGHT_AC);
        aircraftLine.setStrokeColor(Color.WHITE);
        aircraftLine.setClickable(false);
        aircraftLine.setZOrder(ATSKATAKConstants.Z_ORDER_PARKING_AC);
        drawMapItem(aircraftLine, uid, apronGroup);
    }

}
