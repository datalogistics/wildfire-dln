
package com.gmeci.atsk.az;

import android.content.Context;
import android.graphics.Color;

import com.atakmap.android.atsk.plugin.R;
import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.maps.assets.Icon;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.gmeci.atsk.gallery.ATSKGalleryUtils;
import com.gmeci.atsk.gallery.ExifHelper;
import com.gmeci.atsk.resources.ATSKApplication;
import com.gmeci.atsk.resources.ATSKMenuLoader;
import com.gmeci.atsk.visibility.VizPrefs;
import com.gmeci.atskservice.resolvers.AZProviderClient;
import com.gmeci.core.ATSKConstants;
import com.gmeci.core.LineObstruction;
import com.gmeci.core.PointObstruction;
import com.gmeci.core.SurveyData;
import com.gmeci.core.SurveyPoint;
import com.gmeci.atsk.ATSKATAKConstants;
import com.gmeci.atsk.MapHelper;
import com.gmeci.atsk.map.ATSKDrawingTool;
import com.gmeci.atsk.map.ATSKMarker;
import com.gmeci.constants.Constants;
import com.gmeci.helpers.LineHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MapAZController {

    private static final String TAG = "MapAZController";
    public static final int LAST_ZE_LEVEL = -1;
    private final HashMap<String, Integer> _icons = new HashMap<String, Integer>();
    private final MapView _mapView;
    private MapGroup _mapGroup; // I think we should make another map group- use a constant......
    private final ATSKDrawingTool _drawing;

    public MapAZController(MapView map) {
        _mapView = map;
        _mapGroup = map.getRootGroup().findMapGroup(
                ATSKATAKConstants.ATSK_MAP_GROUP_AZ);
        if (_mapGroup == null) {
            _mapGroup = _mapView.getRootGroup().addGroup(
                    ATSKATAKConstants.ATSK_MAP_GROUP_AZ);
        }

        _drawing = new ATSKDrawingTool(_mapView, _mapGroup);
        setupImageMap();
    }

    private void setupImageMap() {
        _icons.put(Constants.PO_BLANK, R.drawable.blank);
        _icons.put(Constants.PO_RED, R.drawable.po_generic_point_tv);
        _icons.put(Constants.PO_BLACK, R.drawable.po_generic_point);
        _icons.put(Constants.PO_GENERIC_POINT, R.drawable.po_generic_point);
        _icons.put(Constants.POINT_PI_CDS, R.drawable.blue_marker);
        _icons.put(Constants.POINT_PI_HE, R.drawable.red_marker);
        _icons.put(Constants.POINT_PI_PER, R.drawable.green_marker);
        _icons.put(Constants.POINT_DEP_ANCHOR, R.drawable.dep_anchor);
        _icons.put(Constants.POINT_APP_ANCHOR, R.drawable.app_anchor);
        _icons.put(Constants.POINT_CENTER_ANCHOR, R.drawable.center_anchor);
        _icons.put(Constants.PO_LZ_DCP, R.drawable.dcp);
        _icons.put(Constants.PO_GTM, R.drawable.gtm);
        _icons.put(Constants.PO_AMP_PANEL, R.drawable.amp_panel);
        _icons.put(Constants.PO_AMP_PANEL_ORANGE, R.drawable.amp_panel);
        _icons.put(Constants.PO_AMP_LIGHT, R.drawable.amp_light);
        _icons.put(Constants.PO_AMP_STROBE, R.drawable.amp_strobe);
        _icons.put(Constants.PO_AMP_RCL, R.drawable.amp_rcl);
        _icons.put(ATSKConstants.FARP_FAM_TYPE, R.drawable.farp_fam_tv);
        _icons.put(ATSKConstants.FARP_RX_TYPE, R.drawable.farp_rx_tv);
        _icons.put(ATSKConstants.FARP_PO, R.drawable.farp_po);
        _icons.put(ATSKConstants.FARP_HRS, R.drawable.farp_hrs);
        _icons.put(ATSKConstants.FARP_HDP, R.drawable.farp_hdp);
        _icons.put(ATSKConstants.FARP_FIRE, R.drawable.farp_fire_extinguisher);
        _icons.put(ATSKConstants.FARP_WATER, R.drawable.farp_water_container);
        _icons.put(ATSKGalleryUtils.IMG_MARKER, R.drawable.camera);
    }

    public String CaptureScreen() {
        return null;
    }

    public void ZoomExtents(double lat, double lon, double length_m,
            int MinimumMapLevel, boolean RequireSynchronous) {
        _drawing.centerAndZoomMapOnLocation(new GeoPoint(lat, lon), length_m);
    }

    public boolean UpdateFARP(SurveyData survey,
            List<LineObstruction> polygons,
            List<PointObstruction> points, String label) {
        MapGroup azGroup = clearOrCreate(survey.uid);
        drawFarp(azGroup, survey, polygons, points, label);
        return false;
    }

    synchronized private MapGroup clearOrCreate(String UID) {
        MapGroup azGroup = _mapGroup.findMapGroup(UID);
        if (azGroup != null) {
            azGroup.clearGroups();
            azGroup.clearItems();
        } else
            azGroup = _mapGroup.addGroup(UID);
        return azGroup;
    }

    private void drawFarp(MapGroup farpGroup, SurveyData survey,
            List<LineObstruction> polygons, List<PointObstruction> points,
            String label) {

        for (LineObstruction lo : polygons)
            _drawing.drawAZLine(farpGroup, lo, label,
                    VizPrefs.getColor(lo.type, LineHelper
                            .getLineColor(lo.type, false)),
                    getFarpIsFillable(lo.type));

        for (PointObstruction po : points)
            _drawing.drawMapItem(getAZMarker(survey.getType()
                    .name(), po), po.uid, farpGroup);

        //drawAZPoints(farpGroup, points2Draw);

    }

    private boolean getFarpIsFillable(String type) {
        return type.equals(ATSKConstants.FARP_AC_TYPE);

    }

    public boolean UpdateHLZ(String UID, String name,
            ArrayList<LineObstruction> Polygons2Draw, SurveyPoint center,
            double radius_m, List<PointObstruction> HLZPositions,
            boolean isCurrentSurvey, String label) {

        MapGroup azGroup = clearOrCreate(UID);
        int color = VizPrefs.getColor(VizPrefs.HLZ_MAIN_OUTLINE,
                LineHelper.getLineColor(ATSKConstants.HLZ_MAIN,
                        isCurrentSurvey));

        if (radius_m < 0)
            drawRectangularAZ(azGroup, UID, Polygons2Draw,
                    color, isCurrentSurvey, label);
        else
            drawCircularAZ(azGroup, name, UID + ATSKConstants.HLZ_MAIN,
                    Polygons2Draw, center, radius_m, color,
                    isCurrentSurvey, label);

        if (isCurrentSurvey)
            drawAZPoints(ATSKConstants.HLZ_MAIN, azGroup, HLZPositions);

        return false;
    }

    public void UpdateLZ(String UID, LineObstruction LZOutline,
            ArrayList<LineObstruction> Polygons2Draw,
            ArrayList<PointObstruction> LZPositions, boolean isCurrentSurvey,
            String name, double width, boolean valid) {

        MapGroup azGroup = clearOrCreate(UID);
        int color = valid ? VizPrefs.getColor(VizPrefs.LZ_MAIN_OUTLINE,
                LineHelper.getLineColor(ATSKConstants.LZ_MAIN,
                        isCurrentSurvey))
                : VizPrefs.getColor(VizPrefs.LZ_INVALID_OUTLINE,
                        0xFFFF0000);

        drawLZ(azGroup, LZOutline, Polygons2Draw,
                color, isCurrentSurvey, name);

        if (isCurrentSurvey)
            drawAZPoints(ATSKConstants.LZ_MAIN, azGroup, LZPositions);
    }

    /**
     * Redraw LZ points such as anchors, DCPs, and gradient markers
     * This should only be called when toggling visibility
     * @param survey Survey data object
     */
    public void redrawLZPoints(SurveyData survey) {
        if (survey.getType() == SurveyData.AZ_TYPE.LZ) {
            // Check if this is the current survey
            AZProviderClient azpc = new AZProviderClient(_mapView.getContext());
            if (azpc.Start()) {
                try {
                    String currentSurvey = azpc.getSetting(
                            ATSKConstants.CURRENT_SURVEY, TAG);
                    if (currentSurvey == null
                            || !currentSurvey.equals(survey.uid))
                        return;
                } finally {
                    azpc.Stop();
                }
            }
            MapGroup lzGroup = _mapGroup.findMapGroup(survey.uid);
            if (lzGroup != null) {
                List<PointObstruction> points = AZController
                        .getInstance().BuildLZPoints(survey);
                // Avoid re-drawing points already available
                for (int i = 0; i < points.size(); i++) {
                    PointObstruction po = points.get(i);
                    if (lzGroup.findItem("uid", po.uid) != null)
                        points.remove(i--);
                }
                drawAZPoints(ATSKConstants.LZ_MAIN, lzGroup, points);
            }
        }
    }

    public boolean DeleteAZ(String UID, boolean isCurrentSurvey) {
        clearOrCreate(UID);
        return false;
    }

    public boolean UpdateDZ(String UID, String name,
            ArrayList<LineObstruction> polygons, SurveyPoint center,
            double radius_m, List<PointObstruction> DZPositions,
            boolean isCurrentSurvey, String label) {

        MapGroup azGroup = clearOrCreate(UID);
        int surveyColor = VizPrefs
                .getColor(VizPrefs.DZ_MAIN_OUTLINE,
                        LineHelper.getLineColor(ATSKConstants.DZ_MAIN,
                                isCurrentSurvey));

        if (radius_m < 0)
            drawRectangularAZ(azGroup, UID, polygons, surveyColor,
                    isCurrentSurvey, label);
        else
            drawCircularAZ(azGroup, name, UID + ATSKConstants.DZ_MAIN,
                    polygons, center, radius_m, surveyColor,
                    isCurrentSurvey, label);

        //then handle the points
        //give the AZ points a different menu....

        if (isCurrentSurvey)
            drawAZPoints(ATSKConstants.DZ_MAIN, azGroup, DZPositions);

        return false;
    }

    private void drawAZPoints(String surveyType, MapGroup azGroup,
            List<PointObstruction> azPositions) {

        if (azPositions != null) {
            for (PointObstruction po : azPositions)
                _drawing.drawMapItem(
                        getAZMarker(surveyType, po), po.uid, azGroup);
        }
    }

    private void drawCircularAZ(MapGroup azGroup, String name, String uid,
            ArrayList<LineObstruction> polygons2Draw, SurveyPoint centerPoint,
            double radius_m, int lineColor, boolean filled, String label) {
        //helper function for DZ and HLZ
        _drawing.drawCircle(azGroup,
                MapHelper.convertSurveyPoint2GeoPoint(centerPoint),
                radius_m, uid, name, lineColor);

        for (LineObstruction azLine : polygons2Draw) {
            _drawing.drawAZLine(azGroup, azLine, label,
                    VizPrefs.getColor(azLine.type, LineHelper
                            .getLineColor(azLine.type, true)),
                    isDashed(azLine.type),
                    azLine.filled);
        }
    }

    private void drawRectangularAZ(MapGroup azGroup, String uid,
            ArrayList<LineObstruction> polygons2Draw, int lineColor,
            boolean filled, String label) {
        //helper function for DZ and HLZ

        for (LineObstruction azLine : polygons2Draw) {
            //group line uid, color, filled.
            _drawing.drawAZLine(azGroup, azLine, label,
                    VizPrefs.getColor(azLine.type, LineHelper
                            .getLineColor(azLine.type, true)),
                    isDashed(azLine.type),
                    azLine.filled);
        }
    }

    /**
     * HLZ is the only dashed line at the moment.
     */
    private boolean isDashed(String type) {
        return type.equals(ATSKConstants.HLZ_APPROACH) ||
                type.equals(ATSKConstants.HLZ_DEPARTURE) ||
                type.equals(ATSKConstants.DZ_HEADING);
    }

    private void drawLZ(MapGroup azGroup, LineObstruction LZOutline,
            ArrayList<LineObstruction> polygons2Draw, int lineColor,
            boolean isCurrentSurvey, String label) {

        _drawing.drawLZLine(azGroup,
                MapHelper.convertSurveyPoint2GeoPoint(LZOutline.points),
                LZOutline.uid, label,
                lineColor,
                true);

        if (!isCurrentSurvey)
            return;

        for (LineObstruction lzLine : polygons2Draw) {
            _drawing.drawLZLine(azGroup,
                    MapHelper.convertSurveyPoint2GeoPoint(lzLine.points),
                    lzLine.uid, label,
                    VizPrefs.getColor(lzLine.type, LineHelper
                            .getLineColor(lzLine.type, true)),
                    lzLine.filled);
        }
    }

    private ATSKMarker getAZMarker(String surveyType,
            PointObstruction existingPoint) {

        if (existingPoint == null)
            return null;

        GeoPoint point = MapHelper.convertSurveyPoint2GeoPoint(existingPoint);

        ATSKMarker marker = new ATSKMarker(ATSKConstants.CURRENT_SCREEN_AZ,
                existingPoint.uid);
        marker.setPoint(point);
        if (Double.compare(existingPoint.course_true, 0) != 0)
            marker.setTrack(existingPoint.course_true, Double.NaN);
        if (existingPoint.remark != null && existingPoint.remark.length() > 0) {
            marker.setMetaString("callsign", existingPoint.remark);
            marker.setTitle(existingPoint.remark);
        } else if (existingPoint.type != null) {
            String label = getLabel(existingPoint.type);
            marker.setMetaString("callsign", label);
            marker.setTitle(label);
            marker.setLabelVisible(false);
            marker.setAlwaysShowText(false);
        }
        // otherwise fall back to the UID - which is nasty

        marker.setMetaBoolean("clickable", false);
        marker.setMetaBoolean("removable", true);
        marker.setMetaBoolean("editable", true);

        int anchor = anchorIndex(existingPoint);
        if (anchor > -1) {
            // Get correct icon
            String type = Constants.POINT_CENTER_ANCHOR;
            if (anchor <= ATSKConstants.ANCHOR_APPROACH_RIGHT)
                type = Constants.POINT_APP_ANCHOR;
            else if (anchor >= ATSKConstants.ANCHOR_DEPARTURE_LEFT)
                type = Constants.POINT_DEP_ANCHOR;
            marker.setIcon(getIconFromType(type));

            // LZ anchor fine adjust menu
            marker.setMetaString("menu", ATSKMenuLoader
                    .loadMenu("menus/az_anchor_menu.xml"));
            marker.setMetaBoolean("ignoreMenu", false);
            // Update center point outline color so it's not confused w/ DCP
            if (anchor == ATSKConstants.ANCHOR_CENTER)
                marker.setIconColor(LineHelper.getLineColor(surveyType, true));
            else if (!existingPoint.visible)
                marker.setIconColor(0);
            else
                marker.setZOrder(Double.NEGATIVE_INFINITY);
            marker.setMetaBoolean("movable", true);
        } else {
            marker.setIcon(getIconFromType(existingPoint.type));
            if (existingPoint.type != null &&
                    existingPoint.type.equals(Constants.PO_AMP_PANEL_ORANGE))
                marker.setIconColor(Color.rgb(255, 128, 0));
            marker.setMetaBoolean("ignoreMenu", true);
            marker.setMetaBoolean("movable", false);
        }

        // DCPs and GTMs hidden by default
        if (existingPoint.type.equals(Constants.PO_LZ_DCP)
                || existingPoint.type.equals(Constants.PO_GTM))
            marker.setVisible(false);

        return marker;
    }

    private static int anchorIndex(PointObstruction po) {
        if (po != null) {
            if (po.type != null && po.type.equals(Constants.POINT_ANCHOR)) {
                try {
                    return Integer.parseInt(po.uid.substring(
                            po.uid.lastIndexOf("_") + 1));
                } catch (Exception e) {
                    return -1;
                }
            }
        }
        return -1;
    }

    private static String getLabel(final String type) {
        if (type.equals("az_anchor"))
            return "Anchor Point";
        else if (type.endsWith("Departure"))
            return "Departure Line";
        else if (type.endsWith("Approach"))
            return "Approach Line";
        else if (type.endsWith("_WORST"))
            return "";
        return type;
    }

    private Icon getIconFromType(String type) {

        int iconNumber;

        if (_icons.containsKey(type))
            iconNumber = _icons.get(type);
        else
            iconNumber = R.drawable.po_generic_point;

        Context pluginContext = ATSKApplication
                .getInstance().getPluginContext();

        Icon azIcon;
        azIcon = new Icon("android.resource://"
                + pluginContext.getPackageName() + "/"
                + iconNumber);

        return azIcon;
    }

    public void drawGalleryIcons(String surveyUID) {
        // Remove old gallery markers
        for (MapItem item : _mapGroup.getItems()) {
            if (item != null && item.getMetaBoolean(
                    "galleryMarker", false))
                _mapGroup.removeItem(item);
        }
        boolean show = VizPrefs.get(VizPrefs.GALLERY_ICONS);
        File[] imgs = ATSKGalleryUtils.getImages(surveyUID);
        MapGroup surveyGroup = _mapGroup.findMapGroup(surveyUID);
        if (surveyGroup != null) {
            // Draw new gallery markers
            for (File img : imgs)
                drawGalleryIcon(surveyUID, img, surveyGroup, show);
        }
    }

    public void drawGalleryIcon(String surveyUID, File img,
            MapGroup surveyGroup, boolean show) {
        GeoPoint gp = ExifHelper.fixImage(_mapView, img.getAbsolutePath());
        if (gp == null || !gp.isValid())
            return;
        String uid = surveyUID + "_pic_" + img.getName();
        String name = ExifHelper.getDescription(img);
        if (name == null || name.isEmpty())
            name = img.getName();
        else if (name.length() > 32)
            name = name.substring(0, 32) + "...";
        String summary = "";
        if (name.contains("\n")) {
            // Titles with newlines aren't handled correctly by GLMarker
            // Need to split into summary text
            String[] lines = name.split("\n");
            name = lines[0];
            for (int i = 1; i < lines.length; i++)
                summary += lines[i] + (i < lines.length - 1 ? "\n" : "");
        }
        ATSKMarker marker = new ATSKMarker(ATSKConstants
                .CURRENT_SCREEN_GALLERY, uid);
        marker.setPoint(gp);
        marker.setIcon(getIconFromType(ATSKGalleryUtils.IMG_MARKER));
        marker.setClickable(true);
        marker.setTitle(name);
        marker.setSummary(summary);
        marker.setMetaString("callsign", name);
        marker.setMetaString("imagePath", img.getAbsolutePath());
        marker.setMetaDouble("minRenderScale", 0.0005);
        marker.setMetaString("menu", ATSKMenuLoader
                .loadMenu("menus/az_gallery_menu.xml"));
        marker.setMetaBoolean("ignoreMenu", false);
        marker.setMetaBoolean("galleryMarker", true);
        if (!show)
            marker.setVisible(false);
        _drawing.drawMapItem(marker, uid, surveyGroup);
    }

    public void close() {
    }

    public void PrepareScreenShots(int screenShotCount) {
    }
}
