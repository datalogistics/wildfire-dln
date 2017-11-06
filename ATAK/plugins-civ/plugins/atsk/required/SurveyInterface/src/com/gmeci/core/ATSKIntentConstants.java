
package com.gmeci.core;

public class ATSKIntentConstants {

    public static final String OB_MENU_POINT_CLICK_ACTION = "OBPointMenuAction";

    public static final String AZ_MENU_CLICK_ACTION = "AZMenuAction";
    public static final String AZ_MENU_UID = "AZMenuUIDSelected";

    public static final String OB_MENU_UID = "OBMenuUIDSelected";
    public static final String OB_MENU_GROUP = "OBMenuGroupSelected";
    public static final String MENU_REQUEST = "OBMenuRequest";
    public static final String MENU_DELETE = "OBMenuRequestDelete";
    public static final String MENU_ZOOM = "OBMenuRequestZoom";
    public static final String MENU_EDIT = "OBMenuRequestEdit";
    public static final String OB_MENU_EXTRA = "OBMenuRequestExtra";
    public static final String OB_TYPE_SELECTED = "OBTypeSelectedExtra";

    public static final String OB_OUT_POINT_INDEX = "OBOutAction_POINT_INDEX";

    public static final String OB_STATE_ACTION = "OBStateAction";
    public static final String OB_STATE_SOURCE = "OBStateChangeSource";

    public static final String OB_COLLECT_TOP = "OB Collect Top";
    public static final String OB_COLLECT_TOP_IGNORE_DEFAULT = OB_COLLECT_TOP
            + "_IgnoreDefault";
    public static final String OB_STATE_REQUESTED_POINT = "OBStateRequested_POINT";
    public static final String OB_STATE_REQUESTED_ROUTE = "OBStateRequested_ROUTE";
    public static final String OB_STATE_REQUESTED_AREA = "OBStateRequested_AREA";
    public static final String OB_STATE_REQUESTED_HIDDEN = "OBStateRequested_HIDDEN";

    public static final String OB_STATE_LRF_HIDDEN = "LRF Hidden";
    public static final String OB_STATE_HIDDEN = "Hidden";
    public static final String OB_STATE_POINT = "Point_Show";
    public static final String OB_STATE_LINE = "Line_Show";
    public static final String OB_STATE_AREA = "Area_Show";

    public static final String OB_STATE_MAP_CLICK = "Map Click";
    public static final String OB_STATE_LRF = "LRF Ready";
    public static final String OB_STATE_2PPLUSD_LRF = "Pull2PPlus D LRF Ready";
    public static final String OB_STATE_2PPLUSD_LRF_1 = "Pull2PPlus D LRF 1 Ready";
    public static final String OB_STATE_2PPLUSD_LRF_2 = "Pull2PPlus D LRF 2 Ready";
    public static final String OB_STATE_GPS = "GPS Ready";
    public static final String OB_STATE_BC_GPS = "GPS BC Ready";

    public static final String OB_STATE_BC_GPS_OFF = "OB ACTION REQUESTED STOP";
    public static final String OB_STATE_BC_GPS_ON = "OB ACTION REQUESTED START";

    public static final String OB_STATE_2PPLUSD_GPS = "Pull2PPlus D GPS Ready";
    public static final String OB_STATE_2PPLUSD_GPS_1 = "Pull2PPlus D GPS 1 Ready";
    public static final String OB_STATE_2PPLUSD_GPS_2 = "Pull2PPlus D GPS 2 Ready";
    public static final String OB_STATE_LRF_EXTENDED_MENU = "LRF Extended Menu Items";
    public static final String OB_STATE_GPS_EXTENDED_MENU = "GPS Extended Menu Items";
    public static final String OB_STATE_OFFSET_GPS = "GPS OFFSET Ready";
    public static final String OB_ACTION = "OB ACTION REQUESTED";

    public static boolean isWaitingForGPS(String CurrentState) {
        if (CurrentState == null || CurrentState.length() == 0)
            return false;
        return CurrentState.equals(OB_STATE_2PPLUSD_GPS)
                || CurrentState.equals(OB_STATE_BC_GPS)
                || CurrentState.equals(OB_STATE_2PPLUSD_GPS_1)
                || CurrentState.equals(OB_STATE_2PPLUSD_GPS_2)
                || CurrentState.equals(OB_STATE_OFFSET_GPS);
    }

    public static boolean isWaitingForLRF(String CurrentState) {
        if (CurrentState == null || CurrentState.length() == 0)
            return false;
        return CurrentState.equals(OB_STATE_LRF)
                || CurrentState.equals(OB_STATE_2PPLUSD_LRF)
                || CurrentState.equals(OB_STATE_2PPLUSD_LRF_1)
                || CurrentState.equals(OB_STATE_2PPLUSD_LRF_2)
                || CurrentState.equals(ATSKIntentConstants.OB_STATE_LRF_HIDDEN);
    }

    public static boolean isWaitingForClick(String CurrentState) {
        if (CurrentState == null || CurrentState.length() == 0)
            return false;
        return CurrentState.equals(OB_STATE_MAP_CLICK);
    }

    public static String GetState(boolean isPoint, boolean isLine,
            boolean isArea) {
        if (isPoint)
            return OB_STATE_POINT;
        else if (isLine)
            return OB_STATE_LINE;
        else if (isArea)
            return OB_STATE_AREA;
        return OB_STATE_HIDDEN;
    }

    public static int GetLRFIndex(String CurrentState) {
        if (CurrentState.equals(OB_STATE_2PPLUSD_LRF_1)
                || CurrentState.equals(OB_STATE_2PPLUSD_LRF))
            return 1;
        else if (CurrentState.equals(OB_STATE_2PPLUSD_LRF_2))
            return 2;

        return 0;
    }

    public static boolean Visible(String CurrentState) {
        return !(CurrentState.equals(OB_STATE_HIDDEN)
                || CurrentState.equals(OB_STATE_REQUESTED_HIDDEN)
                || CurrentState.equals(ATSKIntentConstants.OB_STATE_LRF_HIDDEN));
    }
}
