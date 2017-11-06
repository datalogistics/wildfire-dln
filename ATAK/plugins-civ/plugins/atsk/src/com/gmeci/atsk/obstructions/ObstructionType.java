
package com.gmeci.atsk.obstructions;

import com.atakmap.android.atsk.plugin.R;
import com.gmeci.constants.Constants;

/**
 * List of obstruction types and their matching lists
 */

public enum ObstructionType {
    POINTS("point", Constants.ACTIVE_POINT_OBSTRUCTIONS_PREFERENCE,
            R.array.DEFAULT_POINT_TYPES),
    ROUTES("route", Constants.ACTIVE_LINE_OBSTRUCTIONS_PREFERENCE,
            R.array.DEFAULT_LINE_TYPES),
    AREAS("area", Constants.ACTIVE_AREA_OBSTRUCTIONS_PREFERENCE,
            R.array.DEFAULT_AREA_TYPES),
    VEHICLES("vehicle", null, R.array.DEFAULT_VEHICLE_TYPES),
    TAXIWAYS("taxiway", null, R.array.TAXIWAY_TYPES);

    private final String _name;

    // Preference key for user-specified list of available obstructions
    private final String _pref;

    // String array ID for default obstruction types
    private final int _arrayId;

    ObstructionType(String name, String pref, int arrayId) {
        _name = name;
        _pref = pref;
        _arrayId = arrayId;
    }

    public String getName() {
        return _name;
    }

    public String getPref() {
        return _pref;
    }

    public int getArrayId() {
        return _arrayId;
    }
}
