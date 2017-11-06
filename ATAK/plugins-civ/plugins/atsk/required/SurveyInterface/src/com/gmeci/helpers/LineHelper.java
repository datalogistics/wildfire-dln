
package com.gmeci.helpers;

import android.graphics.Color;

import com.gmeci.core.ATSKConstants;
import com.gmeci.constants.Constants;

import java.util.Map;
import java.util.HashMap;

public abstract class LineHelper {

    public static final String CURRENT_GRADIENT_MODIFIER = "_CURRENT";

    private static class Style {
        public final int color;
        public final double thickness;

        public Style(final int color, final double thickness) {
            this.color = color;
            this.thickness = thickness;
        }

    }

    private static final Map<String, Style> styleMap = new HashMap<String, Style>();
    static {
        styleMap.put(Constants.LO_ARRESTING_GEAR, new Style(0xFFd9c5c5, 3));
        styleMap.put(Constants.LO_BARRIER, new Style(0xFF8f8585, 2));
        styleMap.put(Constants.LO_BERMS, new Style(0xFF645504, 3));
        styleMap.put(Constants.LO_BRIDGE, new Style(0xFF8f8585, 3));
        styleMap.put(Constants.LO_CANAL, new Style(0xFF8f8585, 3));
        styleMap.put(Constants.LO_CULVERT, new Style(0xFF8f8585, 3));
        styleMap.put(Constants.LO_CURB, new Style(0xFFcfc8c8, 10));
        styleMap.put(Constants.LO_DITCH, new Style(0xFF8f8585, 3));
        styleMap.put(Constants.LO_FENCELINE, new Style(0xFF8f8585, 3));
        styleMap.put(Constants.LO_GATE, new Style(0xFF8f8585, 3));
        styleMap.put(Constants.LO_GENERIC_ROUTE, new Style(0xFF000000, 3));
        styleMap.put(Constants.LO_GUARD_RAIL, new Style(0xFF8f8585, 2));
        styleMap.put(Constants.LO_HEDGES, new Style(0xFF3fa934, 6));
        styleMap.put(Constants.LO_HIGHWAY, new Style(0xFF8f8585, 6));
        styleMap.put(Constants.LO_HILL, new Style(0xFF16850a, 4));
        styleMap.put(Constants.LO_LIGHT_POLE_WIRES, new Style(0xFF050505, 3));
        styleMap.put(Constants.LO_MOUND, new Style(0xFF8c451f, 3));
        styleMap.put(Constants.LO_MOUNTAIN, new Style(0xFF8c451f, 3));
        styleMap.put(Constants.LO_PATH, new Style(0xFF8c451f, 3));
        styleMap.put(Constants.LO_PIPELINE, new Style(0xFF8c451f, 3));
        styleMap.put(Constants.LO_POLES_WIRES, new Style(0xFF8c451f, 3));
        styleMap.put(Constants.LO_POWERLINES, new Style(0xFFe80000, 3));
        styleMap.put(Constants.LO_RAILROAD, new Style(0xFF000000, 3));
        styleMap.put(Constants.LO_RIDGELINE, new Style(0xFF000000, 3));
        styleMap.put(Constants.LO_RIVER, new Style(0xFF0021e8, 3));
        styleMap.put(Constants.LO_ROAD, new Style(0xFF8e91a0, 3));
        styleMap.put(Constants.LO_RUTS, new Style(0xFF8e91a0, 3));
        styleMap.put(Constants.LO_TREELINE, new Style(0xFF4c8e2f, 3));
        styleMap.put(Constants.LO_WIRES, new Style(0xFF800000, 3));
        styleMap.put(Constants.LO_LEADER, new Style(0xFF000000, 3));
        styleMap.put(Constants.LO_TAXIWAY, new Style(0xFF000000, 3));
        styleMap.put(Constants.AO_LAKE, new Style(0xFF021e90, 3));
        styleMap.put(Constants.AO_OCEAN, new Style(0xFF021e80, 3));
        styleMap.put(Constants.AO_POND, new Style(0xFF0020e3, 3));
        styleMap.put(Constants.AO_POOL, new Style(0xFF0018e0, 3));
        styleMap.put(Constants.AO_SAND, new Style(0xFFF6FA85, 3));
        styleMap.put(Constants.AO_SWAMP, new Style(0xFF008209, 3));
        styleMap.put(Constants.AO_TREES, new Style(0xFF4c8e2f, 3));
        styleMap.put(Constants.AO_GRAVEL, new Style(0xFF8f8585, 3));
        styleMap.put(Constants.AO_BUSHES, new Style(0xFF4c8e2f, 3));
        styleMap.put(Constants.AO_BUILDINGS, new Style(0xFFaba0a0, 3));
        styleMap.put(Constants.AO_APRON, new Style(0xFF000000, 3));
    }

    public static int getLineColor(String type) {
        return getLineColor(type, true);
    }

    public static int getLineColor(String type, boolean isCurrentSurvey) {
        if (type == null)
            return 0xFFFFFFFF;

        Style s = styleMap.get(type);
        if (s != null)
            return s.color;

        if (type.equals(ATSKConstants.GRADIENT_LEFT_LIMIT_UID)
                || type.equals(ATSKConstants.GRADIENT_RIGHT_LIMIT_UID)
                || type.equals(ATSKConstants.LONGITUDINAL_LIMIT_UID))
            return 0xF0fdff38; //yellow
        if (type.equals(ATSKConstants.APRON_TYPE))
            return 0xFF00FFFF;
        if (type.equals(ATSKConstants.APRON_ROUTE_BOUNDARY_TYPE))
            return 0xFFFF0000;
        if (type.equals(ATSKConstants.APRON_ROUTE_TAXIWAY_TYPE))
            return 0xFFFF00FF;
        if (type.contains(ATSKConstants.GRADIENT_TYPE_LONGITUDINAL)) {
            if (type.contains(CURRENT_GRADIENT_MODIFIER))
                return Color.MAGENTA;
            return Color.BLUE;
        }
        if (type.contains(ATSKConstants.GRADIENT_TYPE_TRANSVERSE)) {
            if (type.contains(CURRENT_GRADIENT_MODIFIER))
                return Color.MAGENTA;
            return Color.BLUE;
        } else if (type.contains(CURRENT_GRADIENT_MODIFIER))
            return Color.MAGENTA;

        if (type.equals(ATSKConstants.DZ_MAIN)) {
            if (isCurrentSurvey)
                return 0xFF5c8c00;
            else
                return 0xFF1e2e00;
        }
        if (type.equals(ATSKConstants.LZ_MAIN)) {
            //return 0xFFFF0404;
            return 0xFF588882;
        }

        if (type.endsWith(ATSKConstants.INCURSION_LINE_APPROACH)
                || type.endsWith(ATSKConstants.INCURSION_LINE_DEPARTURE)
                || type.endsWith(ATSKConstants.INCURSION_LINE_APPROACH_WORST)
                || type.endsWith(ATSKConstants.INCURSION_LINE_DEPARTURE_WORST)
                || type.startsWith(ATSKConstants.GSR_MARKER)) {
            return 0xFFFF0404;
        } else if (type.equals(ATSKConstants.LZ_INNER_APPROACH)
                || type.equals(ATSKConstants.LZ_OUTER_APPROACH))
            return 0xFFFFFF00;
        else if (type.equals(ATSKConstants.LZ_INNER_DEPARTURE)
                || type.equals(ATSKConstants.LZ_OUTER_DEPARTURE))
            return 0xFFFFFF00;
        else if (type.equals(ATSKConstants.LZ_SHOULDER))
            return 0xFF000000;
        else if (type.equals(ATSKConstants.LZ_CLEAR))
            return 0xFF000000;
        else if (type.equals(ATSKConstants.LZ_GRADED))
            return 0xFF000000;
        else if (type.equals(ATSKConstants.LZ_MAINTAINED))
            return 0xFF000000;
        else if (type.equals(ATSKConstants.LZ_OVERRUN))
            return 0xFF000000;
        else if (type.equals(ATSKConstants.DISPLACED_THRESHHOLD))
            return 0xFFFF0066;
        else if (type.equals(ATSKConstants.LZ_THRESHHOLD))
            return 0xFF000000;
        if (type.equals(ATSKConstants.HLZ_MAIN)) {
            if (isCurrentSurvey)
                return 0xFF000080;
            else
                return 0xFF00002a;
        }
        if (type.equals(ATSKConstants.HLZ_APPROACH)) {
            return 0xFF00FFFF;
        }
        if (type.equals(ATSKConstants.HLZ_DEPARTURE)
                || type.equals(ATSKConstants.DZ_HEADING)) {
            return 0xFFFF0000;
        }
        if (type.equals(ATSKConstants.HLZ_OBSTRUCTED)) {
            return Color.RED;
        } else if (type.equals(ATSKConstants.FARP_AC_TYPE))
            return Color.GREEN;
        else if (type.equals(ATSKConstants.FARP_FAM_TYPE))
            return Color.GREEN;
        else if (type.equals(ATSKConstants.FARP_FAM_TYPE_SELECTED))
            return 0xFF02bc1c;

        else if (type.equals(ATSKConstants.FARP_FAM_ANGLE_TYPE))
            return Color.BLACK;
        else if (type.equals(ATSKConstants.FARP_RX_TYPE))
            return Color.YELLOW;
        else if (type.equals(ATSKConstants.FARP_RX_TYPE_SELECTED))
            return 0xffefef00;//lt yellow
        else if (type.equals(ATSKConstants.FARP_TANKER_TYPE))
            return Color.WHITE;
        else if (type.equals(Constants.FARP_AC_FAM_LINE_TYPE))
            return Color.CYAN;
        else if (type.equals(Constants.FARP_FAM_RX_LINE_TYPE))
            return Color.CYAN;

        return 0xFF000000;
    }

}
