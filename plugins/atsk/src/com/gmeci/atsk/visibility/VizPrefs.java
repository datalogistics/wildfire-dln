
package com.gmeci.atsk.visibility;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.SparseArray;

import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.log.Log;
import com.gmeci.constants.Constants;
import com.gmeci.core.ATSKConstants;
import com.gmeci.core.SurveyData;

import java.util.ArrayList;
import java.util.List;

/**
 * Set and get overlay manager preferences
 */
public class VizPrefs {
    public static final String TAG = "VizPrefs";
    public static final String PREF_PREFIX = "atsk_viz_show_";
    public static final String PREF_COL = "atsk_color_";
    public static final String PREF_SCROLL = "atsk_viz_scroll";
    public static final String SHADING = "_shading";

    // Check boxes
    public static final int ALL_OBS = 0;
    public static final int POINTS = 1;
    public static final int ROUTES = 2;
    public static final int AREAS = 3;
    public static final int VEHICLES = 4;
    public static final int OBS_SHADING = 5;
    public static final int GALLERY_ICONS = 6;
    public static final int SURVEY = 7;
    public static final int LZ_MAIN_OUTLINE = 8;
    public static final int LZ_MAIN_SHADING = 9;
    public static final int LZ_CENTER_LINE = 10;
    public static final int LZ_THRESHOLD_LINES = 11;
    public static final int LZ_DCPS = 12;
    public static final int LZ_AMPS = 13;
    public static final int LZ_MIN_GTMS = 14;
    public static final int LZ_MAX_GTMS = 15;
    public static final int LZ_ANCHORS = 16;
    public static final int LZ_SHOULDERS = 17;
    public static final int LZ_OVERRUNS = 18;
    public static final int LZ_OVERRUNS_HATCHING = 19;
    public static final int LZ_CLEAR_ZONES = 20;
    public static final int LZ_APPROACHES = 21;
    public static final int HLZ_MAIN_OUTLINE = 22;
    public static final int HLZ_MAIN_SHADING = 23;
    public static final int HLZ_ANCHORS = 24;
    public static final int HLZ_APPROACH_LINE = 25;
    public static final int HLZ_DEPARTURE_LINE = 26;
    public static final int FARP_AIRCRAFT = 27;
    public static final int FARP_AIRCRAFT_SHADING = 28;
    public static final int FARP_CART = 29;
    public static final int FARP_LEFT_LINES = 30;
    public static final int FARP_RIGHT_LINES = 31;
    public static final int FARP_ITEMS = 32;
    public static final int DZ_MAIN_OUTLINE = 33;
    public static final int DZ_MAIN_SHADING = 34;
    public static final int DZ_CENTER = 35;
    public static final int DZ_ANCHORS = 36;
    public static final int DZ_HEADING_LINE = 37;
    public static final int DZ_PO = 38;
    public static final int DZ_PER = 39;
    public static final int DZ_HE = 40;
    public static final int DZ_CDS = 41;
    public static final int IMGCAP = 42;
    public static final int IMGCAP_ARROWS = 43;
    public static final int IMGCAP_INFO = 44;
    public static final int IMGCAP_SCALE = 45;
    public static final int IMGCAP_HEADINGS = 46;
    public static final int IMGCAP_DIMENSIONS = 47;
    public static final int LZ_INVALID_OUTLINE = 48;
    public static final int RANGE_AND_BEARING = 49;
    public static final int CB_COUNT = 50;

    // Radio groups
    public static final int LZ_ANCHOR_SIZES = 0;
    public static final int HLZ_ANCHOR_SIZES = 1;
    public static final int DZ_ANCHOR_SIZES = 2;
    public static final int RG_COUNT = 3;
    public static final int ANCHOR_LARGE = 0;
    public static final int ANCHOR_MEDIUM = 1;
    public static final int ANCHOR_SMALL = 2;

    private static final SparseArray<String[]> _typeMap = new SparseArray<String[]>();

    private static SharedPreferences _prefs;

    static {
        // LZ
        _typeMap.put(LZ_MAIN_OUTLINE, new String[] {
                ATSKConstants.LZ_MAIN
        });
        _typeMap.put(LZ_CENTER_LINE, new String[] {
                ATSKConstants.LZ_CENTER_LINE
        });
        _typeMap.put(LZ_SHOULDERS, new String[] {
                ATSKConstants.LZ_SHOULDER,
                ATSKConstants.LZ_MAINTAINED, ATSKConstants.LZ_GRADED
        });
        _typeMap.put(LZ_OVERRUNS, new String[] {
                ATSKConstants.LZ_OVERRUN
        });
        _typeMap.put(LZ_OVERRUNS_HATCHING, new String[] {
                ATSKConstants.LZ_APPROACH_ARROW,
                ATSKConstants.LZ_DEPARTURE_ARROW
        });
        _typeMap.put(LZ_CLEAR_ZONES, new String[] {
                ATSKConstants.LZ_CLEAR
        });
        _typeMap.put(LZ_APPROACHES, new String[] {
                ATSKConstants.LZ_INNER_APPROACH,
                ATSKConstants.LZ_OUTER_APPROACH,
                ATSKConstants.LZ_INNER_DEPARTURE,
                ATSKConstants.LZ_OUTER_DEPARTURE
        });

        // DZ
        _typeMap.put(DZ_MAIN_OUTLINE, new String[] {
                ATSKConstants.DZ_MAIN,
                Constants.POINT_CENTER_ANCHOR
        });
        _typeMap.put(DZ_HEADING_LINE, new String[] {
                ATSKConstants.DZ_HEADING
        });

        // HLZ
        _typeMap.put(HLZ_MAIN_OUTLINE, new String[] {
                ATSKConstants.HLZ_MAIN
        });
        _typeMap.put(HLZ_APPROACH_LINE, new String[] {
                ATSKConstants.HLZ_APPROACH
        });
        _typeMap.put(HLZ_DEPARTURE_LINE, new String[] {
                ATSKConstants.HLZ_DEPARTURE
        });

        // FARP
        _typeMap.put(FARP_AIRCRAFT, new String[] {
                ATSKConstants.FARP_AC_TYPE
        });
        _typeMap.put(FARP_CART, new String[] {
                ATSKConstants.FARP_FAM_TYPE
        });
        _typeMap.put(FARP_LEFT_LINES, new String[] {
                ATSKConstants.FARP_FAM_ANGLE_TYPE
        });
    }

    // Apply visibility preferences from outside menu
    public static void applyToSurvey(SurveyData survey) {
        VizController vizController = new VizController(
                MapView.getMapView(), survey);
        vizController.syncPrefs();
    }

    public static SharedPreferences getPrefs() {
        if (_prefs == null) {
            try {
                _prefs = PreferenceManager.getDefaultSharedPreferences(
                        MapView.getMapView().getContext());
            } catch (Exception e) {
                _prefs = null;
            }
        }
        return _prefs;
    }

    // Get visibility preference (defaults to true except if DCPs)
    public static boolean get(int index) {
        return getPrefs() != null && _prefs.getBoolean(
                PREF_PREFIX + getKey(index, false),
                (index != LZ_DCPS && index != LZ_AMPS
                        && index != LZ_MIN_GTMS && index != LZ_MAX_GTMS));
    }

    public static int getRG(int group) {
        if (getPrefs() != null)
            return _prefs.getInt(PREF_PREFIX + getKey(group, true), 0);
        return 0;
    }

    public static int get(String key, int defaultVal) {
        if (getPrefs() != null) {
            try {
                return _prefs.getInt(key, defaultVal);
            } catch (Exception e) {
                try {
                    return Integer.parseInt(_prefs.getString(key,
                            String.valueOf(defaultVal)));
                } catch (Exception e2) {
                    Log.e(TAG, "Failed to get preference int: " + key, e);
                }
            }
        }
        return defaultVal;
    }

    public static boolean get(String key, boolean defaultVal) {
        if (getPrefs() != null) {
            try {
                return _prefs.getBoolean(key, defaultVal);
            } catch (Exception e) {
                try {
                    return Boolean.parseBoolean(_prefs.getString(key,
                            String.valueOf(defaultVal)));
                } catch (Exception e2) {
                    Log.e(TAG, "Failed to get preference boolean: " + key, e);
                }
            }
        }
        return defaultVal;
    }

    public static String get(String key, String defaultVal) {
        if (getPrefs() != null) {
            try {
                return _prefs.getString(key, defaultVal);
            } catch (Exception e) {
                Log.e(TAG, "Failed to get preference string: " + key, e);
            }
        }
        return defaultVal;
    }

    public static int getColor(int index, int defColor) {
        if (getPrefs() != null) {
            try {
                return _prefs.getInt(PREF_COL + getKey(index), defColor);
            } catch (Exception e) {
                Log.e(TAG, "Failed to get color for " + getKey(index), e);
            }
        }
        return defColor;
    }

    public static int getColor(String type, int defColor) {
        return getColor(getIndexFromType(type), defColor);
    }

    // Set visibility preference
    public static void set(int index, boolean show) {
        if (getPrefs() != null)
            _prefs.edit().putBoolean(PREF_PREFIX
                    + getKey(index), show).apply();
    }

    public static void setColor(int index, int color) {
        if (getPrefs() != null)
            _prefs.edit().putInt(PREF_COL
                    + getKey(index), color).apply();
    }

    public static void set(int group, int index) {
        if (getPrefs() != null)
            _prefs.edit().putInt(PREF_PREFIX
                    + getKey(group, true), index).apply();
    }

    public static void set(String key, String value) {
        if (getPrefs() != null)
            _prefs.edit().putString(key, value).apply();
    }

    public static void set(String key, int value) {
        if (getPrefs() != null)
            _prefs.edit().putInt(key, value).apply();
    }

    public static void set(String key, boolean value) {
        if (getPrefs() != null)
            _prefs.edit().putBoolean(key, value).apply();
    }

    public static void resetColors() {
        if (getPrefs() != null) {
            SharedPreferences.Editor editor = _prefs.edit();
            for (int i = 0; i < CB_COUNT; i++)
                editor.remove(PREF_COL + getKey(i));
            editor.apply();
        }
    }

    public static String[] getTypes(int index) {
        return _typeMap.get(index);
    }

    public static int getIndexFromType(String type) {
        for (int i = 0; i < _typeMap.size(); i++) {
            for (String t : _typeMap.valueAt(i)) {
                if (type.equals(t))
                    return _typeMap.keyAt(i);
            }
        }
        return 0;
    }

    public static String getKey(int index, boolean rg) {
        if (rg) {
            switch (index) {
                default:
                case LZ_ANCHOR_SIZES:
                case HLZ_ANCHOR_SIZES:
                case DZ_ANCHOR_SIZES:
                    return "az_anchor_sizes";
            }
        }
        switch (index) {
            default:
            case ALL_OBS:
                return "all_obs";
            case POINTS:
                return "points";
            case ROUTES:
                return "routes";
            case AREAS:
                return "areas";
            case VEHICLES:
                return "vehicles";
            case OBS_SHADING:
                return "obs_shading";
            case RANGE_AND_BEARING:
                return "range_and_bearing";
            case GALLERY_ICONS:
                return "gallery_icons";
            case SURVEY:
                return "survey";
            case LZ_MAIN_OUTLINE:
                return "lz_main_outline";
            case LZ_MAIN_SHADING:
                return "lz_main_shading";
            case LZ_CENTER_LINE:
                return "lz_center_line";
            case LZ_THRESHOLD_LINES:
                return "lz_threshold_lines";
            case LZ_DCPS:
                return "lz_dcps";
            case LZ_AMPS:
                return "lz_amps";
            case LZ_MIN_GTMS:
                return "lz_min_gtms";
            case LZ_MAX_GTMS:
                return "lz_max_gtms";
            case LZ_ANCHORS:
                return "lz_anchor";
            case LZ_SHOULDERS:
                return "lz_shoulders";
            case LZ_OVERRUNS:
                return "lz_overruns";
            case LZ_OVERRUNS_HATCHING:
                return "lz_overruns_hatching";
            case LZ_CLEAR_ZONES:
                return "lz_clear_zones";
            case LZ_APPROACHES:
                return "lz_approaches";
            case HLZ_MAIN_OUTLINE:
                return "hlz_main_outline";
            case HLZ_MAIN_SHADING:
                return "hlz_main_shading";
            case HLZ_ANCHORS:
                return "hlz_anchor";
            case HLZ_APPROACH_LINE:
                return "hlz_approach_line";
            case HLZ_DEPARTURE_LINE:
                return "hlz_departure_line";
            case FARP_AIRCRAFT:
                return "farp_aircraft";
            case FARP_AIRCRAFT_SHADING:
                return "farp_aircraft_shading";
            case FARP_CART:
                return "farp_cart";
            case FARP_LEFT_LINES:
                return "farp_left_lines";
            case FARP_RIGHT_LINES:
                return "farp_left_lines";
            case FARP_ITEMS:
                return "farp_items";
            case DZ_MAIN_OUTLINE:
                return "dz_main_outline";
            case DZ_MAIN_SHADING:
                return "dz_main_shading";
            case DZ_CENTER:
                return "dz_center";
            case DZ_ANCHORS:
                return "dz_anchor";
            case DZ_HEADING_LINE:
                return "dz_heading_line";
            case DZ_PO:
                return "dz_po";
            case DZ_PER:
                return "dz_per";
            case DZ_HE:
                return "dz_he";
            case DZ_CDS:
                return "dz_cds";
            case IMGCAP:
                return "imgcap";
            case IMGCAP_INFO:
                return "imgcap_info_box";
            case IMGCAP_ARROWS:
                return "imgcap_north_arrows";
            case IMGCAP_SCALE:
                return "imgcap_scale_bar";
            case IMGCAP_HEADINGS:
                return "imgcap_headings";
            case IMGCAP_DIMENSIONS:
                return "imgcap_dimensions";
            case LZ_INVALID_OUTLINE:
                return "lz_invalid_outline";
        }
    }

    public static String getKey(int index) {
        return getKey(index, false);
    }

    public static SurveyData.AZ_TYPE getAZType(int index) {
        if (getKey(index).startsWith("lz_"))
            return SurveyData.AZ_TYPE.LZ;
        if (getKey(index).startsWith("hlz_"))
            return SurveyData.AZ_TYPE.HLZ;
        if (getKey(index).startsWith("farp_"))
            return SurveyData.AZ_TYPE.FARP;
        if (getKey(index).startsWith("dz_"))
            return SurveyData.AZ_TYPE.DZ;
        return null;
    }

    public static List<String> getSurveyUIDs(SurveyData survey, int index) {

        ArrayList<String> uids = new ArrayList<String>();

        if (getAZType(index) != survey.getType())
            return uids;

        String uid = survey.uid;
        switch (index) {
            case LZ_MAIN_OUTLINE:
            case LZ_INVALID_OUTLINE:
                uids.add(uid + ATSKConstants.LZ_MAIN);
                break;
            case LZ_MAIN_SHADING:
                uids.add(uid + ATSKConstants.LZ_MAIN + SHADING);
                break;
            case LZ_CENTER_LINE:
                uids.add(uid + "CL" + ATSKConstants.LZ_CENTER_LINE);
                break;
            case LZ_THRESHOLD_LINES:
                uids.add(uid + "APP" + ATSKConstants.DISPLACED_THRESHHOLD);
                uids.add(uid + "DEP" + ATSKConstants.DISPLACED_THRESHHOLD);
                break;
            case LZ_DCPS:
            case LZ_AMPS:
            case LZ_MIN_GTMS:
            case LZ_MAX_GTMS:
                if (index == LZ_DCPS)
                    uids.add(uid + "_" + Constants.PO_LZ_DCP + "_*");
                else if (index == LZ_AMPS)
                    uids.add(uid + "_" + Constants.PO_AMP + "_*");
                else
                    uids.add(uid + "_" + (index == LZ_MIN_GTMS ? "min" : "max")
                            + "_" + Constants.PO_GTM + "_*");
                break;
            case LZ_ANCHORS:
                uids.add(uid + "_" + Constants.POINT_ANCHOR + "_*");
                break;
            case LZ_SHOULDERS:
                uids.add(uid + "L" + ATSKConstants.LZ_SHOULDER);
                uids.add(uid + "L" + ATSKConstants.LZ_MAINTAINED);
                uids.add(uid + "L" + ATSKConstants.LZ_GRADED);
                uids.add(uid + "R" + ATSKConstants.LZ_SHOULDER);
                uids.add(uid + "R" + ATSKConstants.LZ_MAINTAINED);
                uids.add(uid + "R" + ATSKConstants.LZ_GRADED);
                break;
            case LZ_OVERRUNS:
                uids.add(uid + "A" + ATSKConstants.LZ_OVERRUN);
                uids.add(uid + "D" + ATSKConstants.LZ_OVERRUN);
                break;
            case LZ_OVERRUNS_HATCHING:
                uids.add(ATSKConstants.LZ_DEPARTURE_ARROW + "*");
                uids.add(ATSKConstants.LZ_APPROACH_ARROW + "*");
                break;
            case LZ_CLEAR_ZONES:
                uids.add(uid + "AT" + ATSKConstants.LZ_CLEAR);
                uids.add(uid + "DT" + ATSKConstants.LZ_CLEAR);
                break;
            case LZ_APPROACHES:
                uids.add(uid + "IAT" + ATSKConstants.LZ_INNER_APPROACH);
                uids.add(uid + "IDT" + ATSKConstants.LZ_INNER_DEPARTURE);
                uids.add(uid + "OAT" + ATSKConstants.LZ_OUTER_APPROACH);
                uids.add(uid + "ODT" + ATSKConstants.LZ_OUTER_DEPARTURE);
                break;
            case HLZ_MAIN_OUTLINE:
                uids.add(uid + ATSKConstants.HLZ_MAIN);
                break;
            case HLZ_MAIN_SHADING:
                uids.add(uid + ATSKConstants.HLZ_MAIN + SHADING);
                break;
            case HLZ_ANCHORS:
                uids.add(uid + "_" + Constants.POINT_ANCHOR + "_*");
                break;
            case HLZ_APPROACH_LINE:
                uids.add(uid + ATSKConstants.HLZ_APPROACH);
                break;
            case HLZ_DEPARTURE_LINE:
                uids.add(uid + ATSKConstants.HLZ_DEPARTURE);
                break;
            case FARP_AIRCRAFT:
                uids.add(uid + "AC");
                break;
            case FARP_AIRCRAFT_SHADING:
                uids.add(uid + "AC" + SHADING);
                break;
            case FARP_CART:
                uids.add(uid + "_RX_*");
                uids.add(uid + "FAM_RX_LINE*");
                uids.add(uid + ATSKConstants.FARP_FAM_TYPE + "*");
                break;
            case FARP_ITEMS:
                uids.add(survey.uid + "_" + ATSKConstants.FARP_ITEM + "_*");
                break;
            case FARP_LEFT_LINES:
                uids.add(uid + "TL");
                uids.add(uid + "BL");
                break;
            case FARP_RIGHT_LINES:
                uids.add(uid + "TR");
                uids.add(uid + "BR");
                break;
            case DZ_MAIN_OUTLINE:
                uids.add(uid + ATSKConstants.DZ_MAIN);
                break;
            case DZ_MAIN_SHADING:
                uids.add(uid + ATSKConstants.DZ_MAIN + SHADING);
                break;
            case DZ_ANCHORS:
                for (int i = ATSKConstants.ANCHOR_APPROACH_LEFT; i <= ATSKConstants.ANCHOR_DEPARTURE_RIGHT; i++) {
                    if (i == ATSKConstants.ANCHOR_CENTER)
                        continue;
                    uids.add(uid + "_" + Constants.POINT_ANCHOR + "_" + i);
                }
                break;
            case DZ_CENTER:
                uids.add(uid + "_" + Constants.POINT_ANCHOR
                        + "_" + ATSKConstants.ANCHOR_CENTER);
                break;
            case DZ_HEADING_LINE:
                uids.add(uid + ATSKConstants.DZ_HEADING);
                break;
            case DZ_PO:
                uids.add(uid + "_" + Constants.POINT_PO);
                break;
            case DZ_PER:
                uids.add(uid + "_" + Constants.POINT_PI_PER);
                break;
            case DZ_HE:
                uids.add(uid + "_" + Constants.POINT_PI_HE);
                break;
            case DZ_CDS:
                uids.add(uid + "_" + Constants.POINT_PI_CDS);
                break;
        }
        return uids;
    }
}
