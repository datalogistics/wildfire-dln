
package com.gmeci.atsk.gradient;

import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.gmeci.core.LineObstruction;
import com.gmeci.core.PointObstruction;
import com.gmeci.core.SurveyPoint;
import com.gmeci.atsk.ATSKATAKConstants;
import com.gmeci.atsk.MapHelper;
import com.gmeci.atsk.map.ATSKDrawingTool;
import com.gmeci.helpers.LineHelper;

import java.util.List;

/**
 * Created by smetana on 5/28/2014.
 */

public class MapGradientController {

    private final MapView mapView;
    private final MapGroup mapGroup;
    private final ATSKDrawingTool drawing;

    public MapGradientController(MapView mapView) {
        this.mapView = mapView;
        this.mapGroup = this.mapView.getRootGroup().addGroup(
                ATSKATAKConstants.ATSK_MAP_GROUP_GRD);
        drawing = new ATSKDrawingTool(this.mapView, mapGroup);
    }

    public boolean drawGradient(String uID, String type,
            List<SurveyPoint> linePoints) {
        GeoPoint[] geopoints = MapHelper
                .convertSurveyPoint2GeoPoint(linePoints);
        drawing.drawGradientLine(geopoints, uID, LineHelper.getLineColor(type));
        return false;
    }

    public void hideGradient(String uID) {
        MapItem hideItem = mapGroup.findItem("uid", uID);
        if (hideItem != null)
            hideItem.setVisible(false);
    }

    public boolean showGradient(String uID) {
        MapItem gradient = mapGroup.findItem("uid", uID);
        if (gradient != null) {
            gradient.setVisible(true);
            return true;
        }
        return false;
    }

    public void close() {
        this.mapGroup.clearGroups();
        this.mapGroup.clearItems();
    }
}
