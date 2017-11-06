
package com.gmeci.atsk.map;

import android.graphics.Color;
import android.os.Bundle;

import com.atakmap.android.importexport.ExportFilters;
import com.atakmap.android.importexport.Exportable;
import com.atakmap.android.maps.MapEventDispatcher;
import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.util.Circle;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.spatial.file.export.KMZFolder;
import com.atakmap.spatial.kml.KMLUtil;
import com.ekito.simpleKML.model.Feature;
import com.ekito.simpleKML.model.Folder;
import com.ekito.simpleKML.model.Geometry;
import com.ekito.simpleKML.model.LineStyle;
import com.ekito.simpleKML.model.Placemark;
import com.ekito.simpleKML.model.PolyStyle;
import com.ekito.simpleKML.model.Polygon;
import com.ekito.simpleKML.model.Style;
import com.ekito.simpleKML.model.StyleSelector;
import com.gmeci.atsk.resources.ATSKMenuLoader;
import com.gmeci.core.ATSKConstants;
import com.gmeci.atsk.ATSKATAKConstants;
import com.gmeci.atsk.ATSKFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * Circle used by DZ and HLZ survey
 */
public class ATSKCircle extends Circle implements Exportable {

    private static final String TAG = "ATSKCircle";

    public ATSKCircle(String title, String uid, MapGroup mapGroup,
            GeoPoint center, double radius, int lineColor, boolean surveyCircle) {
        super(center, radius, uid);

        setCenterPoint(center);
        setStrokeColor(lineColor);
        setStrokeWeight(2d);

        if (surveyCircle) {
            setStrokeWeight(ATSKATAKConstants.LINE_WEIGHT);
            setFillColor(Color.argb(30,
                    lineColor,
                    lineColor,
                    lineColor));
            setMetaString(ATSKATAKConstants.SURVEY_UID, mapGroup
                    .getParentGroup().getFriendlyName());
            setMetaString("menu", ATSKMenuLoader.loadMenu("menus/az_menu.xml"));
            setMetaString(ATSKATAKConstants.ITEM_TYPE,
                    ATSKConstants.CURRENT_SCREEN_AZ);
        }
        setMetaString("title", title);
    }

    @Override
    public boolean getClickable() {
        return !ATSKFragment.isMapState() && super.getClickable();
    }

    @Override
    public void persist(MapEventDispatcher dispatcher,
            Bundle persistExtras, Class clazz) {
    }

    @Override
    public boolean isSupported(Class target) {
        return Folder.class.equals(target) ||
                KMZFolder.class.equals(target);
    }

    @Override
    public Object toObjectOf(Class target, ExportFilters filters) {
        if (filters != null && filters.filter(this))
            return null;
        if (Folder.class.equals(target))
            return toKml();
        else if (KMZFolder.class.equals(target))
            return toKmz();
        return null;
    }

    public KMZFolder toKmz() {
        Folder f = toKml();
        if (f == null)
            return null;
        return new KMZFolder(f);
    }

    /**
     * Convert single circle to KML
     * @return Folder for circle KML
     */
    public Folder toKml() {
        try {
            Style rstyle = new Style();
            LineStyle lstyle = new LineStyle();
            lstyle.setColor(KMLUtil.convertKmlColor(getStrokeColor()));
            lstyle.setWidth((float) getStrokeWeight());
            rstyle.setLineStyle(lstyle);
            PolyStyle pstyle = new PolyStyle();
            pstyle.setColor(KMLUtil.convertKmlColor(getFillColor()));
            // if fully transparent, then no fill, otherwise check fill mask
            //Note Circle currently does have STYLE_FILLED_MASK set by default
            int a = (getFillColor() >> 24) & 0xFF;
            pstyle.setFill(a == 0 ? 0 : 1);
            pstyle.setOutline(1);
            rstyle.setPolyStyle(pstyle);

            String outerRingId = KMLUtil.hash(rstyle);
            rstyle.setId(outerRingId);

            // Folder element containing styles, shape and label
            String title = getMetaString("title", "");
            Folder folder = new Folder();
            folder.setName(title);
            List<StyleSelector> styles = new ArrayList<StyleSelector>();
            styles.add(rstyle);
            folder.setStyleSelector(styles);
            List<Feature> folderFeatures = new ArrayList<Feature>();
            folder.setFeatureList(folderFeatures);

            // Need to create a place mark for geometry to show up
            Placemark pm = new Placemark();
            pm.setId(getUID() + title + " outer");
            pm.setName(title);
            pm.setStyleUrl("#" + outerRingId);
            pm.setVisibility(getVisible() ? 1 : 0);

            Polygon polygon = KMLUtil.createPolygonWithLinearRing(
                    getPoints(), title + " text");
            if (polygon == null) {
                Log.w(TAG, "Unable to create outer ring KML Polygon");
                return null;
            }
            polygon.setAltitudeMode("clampToGround");

            List<Geometry> geom = new ArrayList<Geometry>();
            pm.setGeometryList(geom);
            geom.add(polygon);
            folderFeatures.add(pm);

            return folder;
        } catch (Exception e) {
            Log.e(TAG, "Export to KML failed", e);
        }
        return null;
    }
}
