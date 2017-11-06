
package com.gmeci.atsk.export.imagecapture;

import com.atakmap.android.imagecapture.CapturePrefs;

import java.util.HashMap;
import java.util.Map;

/**
 * Survey capture specific capture preferences
 */

public class SurveyCapturePrefs extends CapturePrefs {

    public static final String PREF_IMAGE_DIM = "atsk_cap_image_dim";
    public static final String IMAGE_DIM_LETTER = "letter";
    public static final String IMAGE_DIM_FULL = "full";

    public static final String PREF_IMAGE_ITEMS = "atsk_cap_items";
    public static final String IMAGE_ITEMS_ATSK = "atsk";
    public static final String IMAGE_ITEMS_ALL = "all";

    public static final String PREF_INFOBOX_POS = "atsk_cap_info_pos";
    public static final int INFO_POS_TR = 0;
    public static final int INFO_POS_TL = 1;
    public static final int INFO_POS_BR = 2;
    public static final int INFO_POS_BL = 3;

    public static final String PREF_COMPASS_POS = "atsk_cap_compass_pos";
    public static final int COMPASS_POS_LEFT = 0;
    public static final int COMPASS_POS_RIGHT = 1;

    private static final Map<String, Object> _defaultMap = new HashMap<String, Object>();
    static {
        _defaultMap.put(PREF_IMAGE_DIM, IMAGE_DIM_LETTER);
        _defaultMap.put(PREF_IMAGE_ITEMS, IMAGE_ITEMS_ATSK);
        _defaultMap.put(PREF_INFOBOX_POS, INFO_POS_TR);
        _defaultMap.put(PREF_COMPASS_POS, COMPASS_POS_LEFT);
    }

    public static Object get(String key) {
        Object def = _defaultMap.get(key);
        if (def == null)
            return CapturePrefs.get(key, null);
        if (def instanceof Integer) {
            int value = 0;
            try {
                value = Integer.parseInt(String.valueOf(def));
            } catch (Exception ignore) {
            }
            return CapturePrefs.get(key, value);
        } else if (def instanceof Boolean) {
            boolean value = false;
            try {
                value = Boolean.parseBoolean(String.valueOf(def));
            } catch (Exception ignore) {
            }
            return CapturePrefs.get(key, value);
        }
        return CapturePrefs.get(key, String.valueOf(def));
    }
}
