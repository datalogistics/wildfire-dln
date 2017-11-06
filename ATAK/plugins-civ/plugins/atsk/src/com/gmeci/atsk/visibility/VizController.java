
package com.gmeci.atsk.visibility;

import android.graphics.Color;

import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Polyline;
import com.atakmap.android.maps.Shape;
import com.gmeci.atsk.ATSKATAKConstants;
import com.gmeci.atsk.az.MapAZController;
import com.gmeci.atsk.map.ATSKDrawingTool;
import com.gmeci.atsk.map.ATSKMarker;
import com.gmeci.constants.Constants;
import com.gmeci.core.SurveyData;
import com.gmeci.helpers.LineHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Control map item visibility and color
 */

public class VizController {

    private final MapView _mapView;
    private final SurveyData _survey;
    private final MapGroup _obsGroup, _azGroup, _surveyGroup;
    private boolean _looping = false;
    private Runnable _onChanged;

    public VizController(MapView mapView, SurveyData survey) {
        _mapView = mapView;
        _survey = survey;
        _obsGroup = _mapView.getRootGroup()
                .findMapGroup(ATSKATAKConstants.ATSK_MAP_GROUP_OBS);
        _azGroup = _mapView.getRootGroup()
                .findMapGroup(ATSKATAKConstants.ATSK_MAP_GROUP_AZ);
        if (_survey != null && _azGroup != null)
            _surveyGroup = _azGroup.findMapGroup(_survey.uid);
        else
            _surveyGroup = null;
    }

    /**
     * Update visibility of all map items based on preferences
     */
    public void syncPrefs() {
        _looping = true;
        // Save preferences
        boolean[] viz = new boolean[VizPrefs.CB_COUNT];
        for (int i = 0; i < VizPrefs.CB_COUNT; i++)
            viz[i] = VizPrefs.get(i);

        // Set checkboxes
        for (int i = 0; i < VizPrefs.CB_COUNT; i++)
            setVisible(i, viz[i]);

        // Set radio buttons
        for (int i = 0; i < VizPrefs.RG_COUNT; i++)
            setSize(i, VizPrefs.getRG(i));
        _looping = false;
    }

    public void setVisible(int index, boolean visible) {
        // Parent check-boxes
        if (!_looping) {
            // Only affect children when not syncing checkboxes to prefs
            int start = -1, end = 0;
            switch (index) {
                case VizPrefs.ALL_OBS:
                    start = VizPrefs.ALL_OBS;
                    end = VizPrefs.SURVEY;
                    break;
                case VizPrefs.SURVEY:
                    start = VizPrefs.SURVEY;
                    end = VizPrefs.IMGCAP;
                    setVisible(_surveyGroup, "*", visible);
                    break;
                case VizPrefs.IMGCAP:
                    start = VizPrefs.IMGCAP;
                    end = VizPrefs.CB_COUNT;
                    break;
            }
            for (int i = start + 1; i < end; i++)
                setVisible(i, visible);
            if (start > -1)
                return;
        }

        // Survey pieces
        if (index > VizPrefs.SURVEY && index < VizPrefs.IMGCAP) {
            // Only min or max can be checked at once
            if (visible) {
                if (index == VizPrefs.LZ_MIN_GTMS)
                    setVisible(VizPrefs.LZ_MAX_GTMS, false);
                else if (index == VizPrefs.LZ_MAX_GTMS)
                    setVisible(VizPrefs.LZ_MIN_GTMS, false);
            }
            // We need to re-draw LZ points since they're no longer
            // drawn by default. This is to speed up LZ drawing performance.
            if (visible && (index == VizPrefs.LZ_ANCHORS
                    || index == VizPrefs.LZ_DCPS
                    || index == VizPrefs.LZ_AMPS
                    || index == VizPrefs.LZ_MAX_GTMS
                    || index == VizPrefs.LZ_MIN_GTMS)) {
                MapAZController azmc = new MapAZController(_mapView);
                azmc.redrawLZPoints(_survey);
            }

            // Set items visibility
            for (String uid : VizPrefs.getSurveyUIDs(_survey, index))
                setVisible(_surveyGroup, uid, visible);

            // Sync overrun hatching visibility
            if (index == VizPrefs.LZ_OVERRUNS)
                setVisible(VizPrefs.LZ_OVERRUNS_HATCHING, visible);
        } else {
            // Obstructions
            Collection<MapItem> obsItems = findMapItems(_obsGroup);
            if (index == VizPrefs.OBS_SHADING) {
                for (MapItem item : obsItems) {
                    if (!item.getMetaString("obsType", "route").equals("route"))
                        setVisible(item, visible, true);
                }
            } else if (index == VizPrefs.POINTS || index == VizPrefs.VEHICLES) {
                for (MapItem item : obsItems) {
                    String obsType = item.getMetaString("obsType", "");
                    if (index == VizPrefs.POINTS && obsType.equals("point")
                            || index == VizPrefs.VEHICLES
                            && obsType.equals("vehicle"))
                        setVisible(item, visible, false);
                }
            } else if (index == VizPrefs.ROUTES || index == VizPrefs.AREAS) {
                for (MapItem item : obsItems) {
                    String obsType = item.getMetaString("obsType", "");
                    if (index == VizPrefs.ROUTES && obsType.equals("route")
                            || index == VizPrefs.AREAS
                            && obsType.equals("area"))
                        setVisible(item, visible, false);
                }
            } else if (index == VizPrefs.GALLERY_ICONS) {
                for (MapItem item : findMapItems(_surveyGroup)) {
                    if (item.getMetaBoolean("galleryMarker", false))
                        setVisible(item, visible, false);
                }
            } else if (index == VizPrefs.RANGE_AND_BEARING) {
                for (MapItem item : obsItems) {
                    if (item.getMetaString("obsType", "").equals("R&B"))
                        setVisible(item, visible, false);
                }
            }
        }
        if (!_looping)
            VizPrefs.set(index, visible);
        onChanged();
    }

    /**
     * Set visibility of all items (which have their visibilities controlled)
     * @param visible True for visible, false for invisible
     */
    public void setAllVisible(boolean visible) {
        _looping = true;
        for (int i = 0; i < VizPrefs.CB_COUNT; i++) {
            VizPrefs.set(i, visible);
            setVisible(i, visible);
        }
        _looping = false;
    }

    public List<MapItem> findMapItems(MapGroup group, String uid) {
        List<MapItem> items = new ArrayList<MapItem>();
        if (group == null)
            return items;
        if (uid.equals("*"))
            items.addAll(group.getItems());
        else
            items.addAll(group.deepFindItems("uid", uid));
        return items;
    }

    public List<MapItem> findMapItems(MapGroup group, List<String> uids) {
        List<MapItem> items = new ArrayList<MapItem>();
        if (group == null)
            return items;
        for (String uid : uids) {
            for (MapItem item : findMapItems(group, uid)) {
                if (!items.contains(item))
                    items.add(item);
            }
        }
        return items;
    }

    public List<MapItem> findMapItems(MapGroup group) {
        return findMapItems(group, "*");
    }

    public void setVisible(MapGroup group, String uid, boolean visible) {
        if (group == null)
            return;

        // Shading-only toggle
        boolean fillOnly = false;
        if (uid.endsWith(VizPrefs.SHADING)) {
            fillOnly = true;
            uid = uid.substring(0, uid.indexOf(VizPrefs.SHADING));
        }

        for (MapItem item : findMapItems(group, uid))
            setVisible(item, visible, fillOnly);
        onChanged();
    }

    public void setVisible(MapItem item, boolean visible, boolean fillOnly) {
        if (item != null) {
            if (fillOnly) {
                if (item instanceof Shape) {
                    Shape shape = (Shape) item;
                    int fillColor = 0;
                    String type = shape.getMetaString("obsName", "");
                    if (visible && !Constants.isTaxiway(type))
                        fillColor = shape.getStrokeColor()
                                - ATSKDrawingTool.FILL_MASK;
                    shape.setFillColor(fillColor);
                }
            } else {
                item.setVisible(visible);
                if (item.hasMetaValue("shapeUID")) {
                    MapItem shape = item.getGroup().deepFindUID(
                            item.getMetaString("shapeUID", item.getUID()));
                    if (shape != null)
                        shape.setVisible(visible);
                }
            }
        }
    }

    public int getItemColor(int index) {
        int defColor = Color.BLACK;
        if (index == VizPrefs.LZ_INVALID_OUTLINE)
            defColor = Color.RED;
        else {
            String[] types = VizPrefs.getTypes(index);
            if (types != null && types.length > 0)
                defColor = LineHelper.getLineColor(types[0]);
        }
        return VizPrefs.getColor(index, defColor);
    }

    public void setItemColor(int index, int color) {
        if (!_survey.valid && index == VizPrefs.LZ_MAIN_OUTLINE
                || _survey.valid && index == VizPrefs.LZ_INVALID_OUTLINE)
            return;
        List<String> uids = VizPrefs.getSurveyUIDs(_survey, index);
        if (index == VizPrefs.FARP_LEFT_LINES)
            // Color pref includes all angle lines
            uids.addAll(VizPrefs.getSurveyUIDs(_survey,
                    VizPrefs.FARP_RIGHT_LINES));
        List<MapItem> items = findMapItems(_azGroup, uids);
        for (MapItem mi : items) {
            if (mi != null && mi instanceof Shape) {
                Shape shp = (Shape) mi;
                shp.setStrokeColor(color);
                if ((shp.getStyle() & Polyline.STYLE_FILLED_MASK) > 0
                        && Color.alpha(shp.getFillColor()) > 0)
                    shp.setFillColor(color - ATSKDrawingTool.FILL_MASK);
            }
        }
        onChanged();
    }

    /**
     * Set icon size based on anchor size preference
     * @param group Survey group index
     * @param index Size index
     */
    public void setSize(int group, int index) {
        List<String> uidList = new ArrayList<String>();
        switch (group) {
            case VizPrefs.LZ_ANCHOR_SIZES:
                uidList = VizPrefs.getSurveyUIDs(_survey, VizPrefs.LZ_ANCHORS);
                break;
            case VizPrefs.HLZ_ANCHOR_SIZES:
                uidList = VizPrefs.getSurveyUIDs(_survey, VizPrefs.HLZ_ANCHORS);
                break;
            case VizPrefs.DZ_ANCHOR_SIZES:
                uidList = VizPrefs.getSurveyUIDs(_survey, VizPrefs.DZ_ANCHORS);
                uidList.addAll(VizPrefs.getSurveyUIDs(_survey,
                        VizPrefs.DZ_CENTER));
                break;
        }
        int iconSize = 32;
        switch (index) {
            case VizPrefs.ANCHOR_LARGE:
                iconSize = 32;
                break;
            case VizPrefs.ANCHOR_MEDIUM:
                iconSize = 24;
                break;
            case VizPrefs.ANCHOR_SMALL:
                iconSize = 16;
                break;
        }
        for (MapItem item : findMapItems(_surveyGroup, uidList)) {
            if (item instanceof ATSKMarker)
                ((ATSKMarker) item).setIconSize(iconSize);
        }
        onChanged();
    }

    private void onChanged() {
        if (_onChanged != null)
            _onChanged.run();
    }

    public void setOnChangedListener(Runnable r) {
        _onChanged = r;
    }
}
