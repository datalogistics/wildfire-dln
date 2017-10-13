
package com.gmeci.core;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Environment;

import com.gmeci.constants.Constants;

public class ATSKConstants {

    public static final String FORM_AZ_NAME = "siteName";

    public static final String ATSK_SURVEY_FOLDER_BASE = Environment
            .getExternalStorageDirectory() + "/atsk/surveys";

    public static final String DB_LOCATION = Environment
            .getExternalStorageDirectory() + "/atsk/Databases/";

    public static final String LOGS_LOCATION = Environment
            .getExternalStorageDirectory() + "/atsk/logs";

    public static final String LAUNCH_PREF = "show_surveyselection_on_start";
    public static final String LAST_MENU_PREF = "atsk_last_menu";

    public static final String DZ_MAIN = "_DZMain";
    public static final String DZ_APPROACH = "_DZApproach";
    public static final String DZ_DEPARTURE = "_DZDeparture";

    public static final int DEGREE_SYMBOL = 0xb0;
    public static final double LONGITUDINAL_RIBBON_WIDTH_M = 6f;

    public static final String LZ_APPROACH_ARROW = "_LZApproachArrowLine";
    public static final String LZ_DEPARTURE_ARROW = "_LZDepartureArrowLine";
    public static final String LZ_CENTER_LINE = "_LZCenterLine";
    public static final String LZ_MAIN = "_LZMain";
    public static final String LZ_SHOULDER = "_LZShoulder";
    public static final String LZ_GRADED = "_LZGraded";
    public static final String LZ_MAINTAINED = "_LZMaintained";
    public static final String LZ_INNER_APPROACH = "_LZInnerApproach";
    public static final String LZ_INNER_DEPARTURE = "_LZInnerDeparture";
    public static final String LZ_OUTER_APPROACH = "_LZOuterApproach";
    public static final String LZ_OUTER_DEPARTURE = "_LZOuterDeparture";
    public static final String LZ_CLEAR = "_LZCLEAR";
    public static final String LZ_OVERRUN = "_LZOVERRUN";
    public static final String LZ_THRESHHOLD = "_LZTHRESHHOLD";
    public static final String HLZ_MAIN = "_HZMain";
    public static final String HLZ_APPROACH = "_HLZApproach";
    public static final String HLZ_DEPARTURE = "_HLZDeparture";
    public static final String HLZ_OBSTRUCTED = "_HLZObstructed";
    public static final String DZ_HEADING = "_DZHeading";
    public static final String FARP_AC_TYPE = "FARP Aircraft";
    public static final String FARP_FAM_TYPE = Constants.FARP_FAM_TYPE;
    public static final String FARP_FAM_TYPE_SELECTED = "FAM_SELECTED";

    public static final String FARP_ITEM = "FARP_POINT";
    public static final String FARP_PO = "FARP PO";
    public static final String FARP_HRS = "FARP HRS";
    public static final String FARP_HDP = "FARP HDP";
    public static final String FARP_FIRE = "FARP Fire Extinguisher";
    public static final String FARP_WATER = "FARP Water Container";

    public static final String DISPLACED_THRESHHOLD = "DISPLACED_THRESHHOLD";
    public static final String FARP_RX_TYPE = Constants.FARP_RX_TYPE;
    public static final String FARP_FAM_ANGLE_TYPE = Constants.FARP_RX_TYPE
            + "ANGLE";
    public static final String FARP_RX_TYPE_SELECTED = "FARP_RX_SELECTED";
    public static final String FARP_TANKER_TYPE = "FARP_TANKER";
    public static final String AC_FAM_LINE_TYPE = "_ac_fam";

    public static final String LAST_MAP_LAT = "LastMapLat";
    public static final String LAST_MAP_LON = "LastMapLon";
    public static final String LAST_MAP_ZOOM = "LastMapZoom";

    public static final double DEFAULT_MAP_CLICK_ERROR_M = 40;
    public static final String GRADIENT_CURRENT_LON = "Current Longitude";
    public static final double GRADIENT_SPACING_LONGITUDINAL_FT = 200;
    //num LRF shots to keep on the map at one time
    public static final int MAX_LRF_SHOTS = 1;
    public static final String SHOW_HIDE_GRADIENT = "SHOW_HIDE GRADIENT CLASS";
    public static final String GRADIENT_CLASS = "GRADIENT_CLASS";
    public static final String GRADIENT_CLASS_FILTERED = "GRADIENT_CLASS FILTERED";
    public static final String GRADIENT_CLASS_ALL = "GRADIENT_CLASS ALL";
    public static final String GRADIENT_CLASS_RAW = "GRADIENT_CLASS RAW";
    public static final String SHOW = "Show";
    public static final String GRADIENT_DRAW_PROGRESS_ACTION = "Draw Gradient Progress Action";
    public static final String GRADIENT_DRAW_PROGRESS_CURRENT = "Draw Gradient Current";
    public static final String GRADIENT_DRAW_PROGRESS_TOTAL = "Draw Gradient TOTAL";
    public static final String SCREEN_SHOT_REQUEST_ACTION = "ScreenShot Request Action";
    public static final String SCREEN_SHOT_COMPLETE_ACTION = "ScreenShot Complete Action";
    public static final String GRADIENT_LEFT_LIMIT_UID = "GRADIENT_LEFT_LIMIT";
    public static final String GRADIENT_RIGHT_LIMIT_UID = "GRADIENT_RIGHT_LIMIT";
    public static final String LONGITUDINAL_LIMIT_UID = "LONGITUDINAL_LIMIT";
    public static final String CURRENT_GRADIENT_UPDATE = "CURRENT_GRADIENT_UPDATE";
    public static final String DEFAULT_GROUP = "default";
    public static final String DISTRESS_GROUP = "Distress";
    public static final String VEHICLE_GROUP = "Vehicles";
    //PAWLOS intents
    public static final String CBR_GROUP = "cbr";
    public static final String LONGITUDINAL = "LONGITUDINAL";
    public static final String TRANSVERSE = "TRANSVERSE";

    public static final String GRADIENT_GROUP = "Gradient Problems";
    public static final String TEMP_APRON_UID = "Temp Apron";
    public static final String TEMP_POINT_UID = "Temp Point";
    public static final String TEMP_LINE_UID = "Temp Line";
    public static final String GRADIENT_HIDDEN_MODIFIER = "-h";
    public static final String GRADIENT_GOOD_MODIFIER = "-g";
    public static final String GRADIENT_BAD_MODIFIER = "-b";
    public static final String GRADIENT_TYPE_LONGITUDINAL = "grad_long";
    public static final String GRADIENT_TYPE_TRANSVERSE = "grad_trans";
    public static final String GRADIENT_TYPE_LONGITUDINAL_BAD = GRADIENT_TYPE_LONGITUDINAL
            + GRADIENT_BAD_MODIFIER;
    public static final String GRADIENT_TYPE_LONGITUDINAL_GOOD = GRADIENT_TYPE_LONGITUDINAL
            + GRADIENT_GOOD_MODIFIER;
    public static final String GRADIENT_TYPE_LONGITUDINAL_BAD_HIDDEN = GRADIENT_TYPE_LONGITUDINAL
            + GRADIENT_BAD_MODIFIER + GRADIENT_HIDDEN_MODIFIER;
    public static final String GRADIENT_TYPE_LONGITUDINAL_GOOD_HIDDEN = GRADIENT_TYPE_LONGITUDINAL
            + GRADIENT_GOOD_MODIFIER + GRADIENT_HIDDEN_MODIFIER;
    public static final String GRADIENT_TYPE_TRANSVERSE_BAD = GRADIENT_TYPE_TRANSVERSE
            + GRADIENT_BAD_MODIFIER;
    public static final String GRADIENT_TYPE_TRANSVERSE_GOOD = GRADIENT_TYPE_TRANSVERSE
            + GRADIENT_GOOD_MODIFIER;
    public static final String GRADIENT_TYPE_TRANSVERSE_BAD_HIDDEN = GRADIENT_TYPE_TRANSVERSE
            + GRADIENT_BAD_MODIFIER + GRADIENT_HIDDEN_MODIFIER;
    public static final String GRADIENT_TYPE_TRANSVERSE_GOOD_HIDDEN = GRADIENT_TYPE_TRANSVERSE
            + GRADIENT_GOOD_MODIFIER + GRADIENT_HIDDEN_MODIFIER;
    public static final String COORD_FORMAT = "COORD_FORMAT";
    public static final String UNITS_GSR_ANGLE = "Display GSR Angle";
    public static final String UNITS_GSR_ANGLE_GSR = "GSR";
    public static final String UNITS_GSR_ANGLE_ANGLE = "Angle";
    public static final String UNITS_ANGLE = "UNITS_ANGLE";
    public static final String UNITS_ANGLE_TRUE = "UNITS_ANGLE_TRUE";
    public static final String UNITS_ANGLE_MAG = "UNITS_ANGLE_MAG";
    public static final String UNITS_METERS = "METERS";
    public static final String UNITS_FEET = "FEET";
    public static final String UNITS_DISPLAY = "UNITS";
    public static final String MAP_CLICK_ACTION = "MapClick";
    public static final String PT_OBSTRUCTION_CLICK_ACTION = "PointObstructionClicked";
    public static final String L_OBSTRUCTION_CLICK_ACTION = "LineObstructionClicked";
    public static final String AZ_CLICK_ACTION = "AZClicked";
    public static final String UID_EXTRA = "uid";
    public static final String GROUP_EXTRA = "group";
    public static final String COPY_EXTRA = "copy";
    public static final String SURVEY_POINT_EXTRA = "SurveyPoint";
    public static final String LAT_EXTRA = "Latitude";
    public static final String LON_EXTRA = "Longitude";
    public static final String ALT_EXTRA = "Altitude_m";
    public static final String CE_EXTRA = "ce_m";
    public static final String LE_EXTRA = "ce_m";
    public static final String LENGTH_EXTRA = "Length";
    public static final String WIDTH_EXTRA = "Width";
    public static final String ANGLE_EXTRA = "Angle_t";
    public static final String MAG_ARROWS_INSIDE = "magArrowsInside";
    public static final String SHOW_INCURSIONS = "showIncursions";
    public static final String LAST_COLLECTION_METHOD_PREF = "atsk_collection_method";

    public static final String OBSTRUCTION_METHOD_GPS_HEIGHT_M = "GPS Height ";
    public static final String OBSTRUCTION_METHOD_LRF2GPS_OFFSET_HEIGHT_M = "LRF-GPS Offset Height";
    public static final String MEASUREMENTS_ATAK_GROUP_ID = "Measurements";

    /**
     * Intent Fragments below
     **/
    //action name
    public static final String CURRENT_SURVEY_CHANGE_ACTION = "CurrentSurveyUIDChanged";
    public static final String ATSK_FRAGMENT_CHANGE = "ATSK_FRAGMENT_CHANGE";
    public static final String ATSK_MAP_CHANGE = "ATSK_MAP_CHANGE";
    public static final String SELECTED = "Selected";
    public static final String GMECI_HARDWARE_GPS_ACTION = "GMECI_HARDWARE_GPS_ACTION";
    public static final String GMECI_HARDWARE_LRF_ACTION = "GMECI_HARDWARE_LRF_ACTION";
    public static final String GMECI_HARDWARE_MAP_ACTION = "GMECI_HARDWARE_MAP_ACTION";
    //Serial Port
    public static final String SERIAL_CONNECTION_ACTION = "com.gmeci.atsk.SERIAL_CONNECTION_ACTION";
    public static final String SERIAL_BAUD_CHANGED = "com.gmeci.atsk.SERIAL_BAUD_CHANGED";

    public static final String SERIAL_ATTACHED = "com.gmeci.atsk.SERIAL_ATTACHED";

    //OBSTRUCTION Constants
    //LAT, LON and ALT already declared

    public static final String AZIMUTH_T = "AZIMUTH_T";
    public static final String ELEVATION = "ELEVATION";
    public static final String RANGE_M = "RANGE_M";
    public static final String DEVICE = "DEVICE";
    public static final String BT_LIST_REPOPULATED = "com.gmeci.BT_LIST_REPOPULATED";
    public static final String BT_SCAN = "com.gmeci.BT_SCAN";
    public static final String BT_CONNECTION = "com.gmeci.BT_CONNECTION";
    public static final String BT_CONNECTION_UPDATE_EXTRA = "com.gmeci.BT_CONNECTION_UPDATE";
    public static final String BT_NAME = "com.gmeci.BT_NAME";
    public static final String BT_NAMES = "com.gmeci.BT_NAMES";
    public static final String BT_DROP_SINGLE_CONNECTION = "com.gmeci.BT_DROP_SINGLE_CONNECTION";
    public static final String BT_TOTAL_DEVICES = "com.gmeci.BT_TOTAL_DEVICES";
    public static final String BT_LIST_NUMBER = "com.gmeci.BT_LIST_NUMBER";
    public static final String BT_DEVICE_CHANGE = "com.gmeci.BT_DEVICE_CHANGE";
    public static final String BT_STATUS_REQUEST = "com.gmeci.BT_STATUS_REQUEST";
    //Point

    public static final String NOTIFICATION_AUTOOPEN = "notification open";
    public static final String NOTIFICATION_TITLE = "notification title";
    public static final String NOTIFICATION_LINE1 = "notification line1";
    public static final String NOTIFICATION_LINE2 = "notification line2";
    public static final String NOTIFICATION_LINE3 = "notification line3";
    public static final String NOTIFICATION_UPDATE = "notification update";
    public static final String NOTIFICATION_BUBBLE_LRF_APPROVED = "Notification Bubble LRF Approval";
    public static final String NOTIFICATION_BUBBLE = "Notification Bubble Action";
    //ATAK Map Group Constants

    //Anchor Position Constants
    public static final int ANCHOR_APPROACH_LEFT = 0;
    public static final int ANCHOR_APPROACH_CENTER = 1;
    public static final int ANCHOR_APPROACH_RIGHT = 2;
    public static final int ANCHOR_CENTER = 3;
    public static final int ANCHOR_DEPARTURE_LEFT = 4;
    public static final int ANCHOR_DEPARTURE_CENTER = 5;
    public static final int ANCHOR_DEPARTURE_RIGHT = 6;

    //GPS Quality indicators tells what GPS we are using
    public static final String GMECI_GPS_QUALITY = "GMECI_GPS_QUALITY";
    public static final String GPS_EXTERNAL_RTK = "GPS_EXTERNAL_RTK";
    public static final String GPS_EXTERNAL = "GPS_EXTERNAL";
    public static final String GPS_INTERNAL = "GPS_INTERNAL";
    public static final String GPS_ALERT = "GPS_ALERT";
    public static final String GPS_ALERT_LOST_RTK = "GPS_ALERT_LOST_RTK";
    public static final String GPS_NO_CONNECTION = "GPS_NO_CONNECTION";

    public static final String LRF_INPUT = "LRF";

    public static final int LIGHT_BLUE = Color.parseColor("#ff33b5e5");
    public static final String EXPORT_ATSK_MISSION_PACKAGE = "EXPORT_ATSK_MISSION_PACKAGE";
    public static final String CURRENT_SCREEN = "CURRENT_SCREEN";
    public static final String CURRENT_SCREEN_AZ = "CURRENT_SCREEN_AZ";
    public static final String CURRENT_SCREEN_OBSTRUCTION = "CURRENT_SCREEN_OBSTRUCTION";
    public static final String CURRENT_SCREEN_VEHICLE = "CURRENT_SCREEN_VEHICLE";
    public static final String CURRENT_SCREEN_CBR = "CURRENT_SCREEN_CBR";
    public static final String CURRENT_SCREEN_GRADIENT = "CURRENT_SCREEN_GRADIENT";
    public static final String CURRENT_SCREEN_PARKING = "CURRENT_SCREEN_PARKING";
    public static final String CURRENT_SCREEN_GALLERY = "CURRENT_SCREEN_GALLERY";
    public static final String CURRENT_SURVEY = "CURRENT_SURVEY";
    public static final String FARP_RX_LAYOUT_SINGLE = "SINGLE";
    public static final String FARP_RX_LAYOUT_HLEFT = "H-LEFT";
    public static final String FARP_RX_LAYOUT_HRIGHT = "H-RIGHT";
    public static final String FARP_RX_LAYOUT_SPLIT = "SPLIT";
    public static final String FARP_RX_LAYOUT_TRIPLE = "THREE";
    public static final String FARP_RX_LAYOUT_RGR = "RGR";
    public static final String AC_C130 = "C-130";
    public static final String AC_C17 = "C-17";
    public static final String INCURSION_LINE_APPROACH = "IncursionLineApproach";
    public static final String INCURSION_LINE_DEPARTURE = "IncursionLineDeparture";
    public static final String INCURSION_LINE_APPROACH_WORST = "IncursionLineApproach_WORST";
    public static final String INCURSION_LINE_DEPARTURE_WORST = "IncursionLineDeparture_WORST";
    public static final String GSR_MARKER = "HLZ_GSR_MARKER_";
    public static final String[] FARP_RX_TYPES = new String[] {
            FARP_RX_LAYOUT_SINGLE, FARP_RX_LAYOUT_HLEFT, FARP_RX_LAYOUT_HRIGHT,
            FARP_RX_LAYOUT_SPLIT, FARP_RX_LAYOUT_TRIPLE, FARP_RX_LAYOUT_RGR
    };
    public static final String SURVEY_WIDTH = "survey_width";
    public static final String SURVEY_LENGTH = "survey_length";
    public static final String LEADER_SUFFIX = "_leader";
    public static String ATSK_APRON_RESPONSE_INTENT = "ATSK_APRON_RESPONSE_INTENT";
    public static String ATSK_APRON_CLOSE_EDIT_INTENT = "ATSK_APRON_CLOSE_RESPONSE_INTENT";
    public static final String ATSK_APRON_EDIT_INTENT = "ATSK_APRON_RESPONSE_INTENT";
    public static final String ATSK_CBR_EDIT_INTENT = "ATSK_CBR_RESPONSE_INTENT";
    public static String BT_ENABLE_REQUEST_ACTION = "gmeci.com.bt_enable";
    public static String GPS_ENABLE_REQUEST_ACTION = "gmeci.com.bt_enable";
    public static String BT_MAIN_PRESSED_ACTION = "gmeci.com.bt_main_pressed";
    public static String BT_ENABLE_PRESSED_ACTION = "gmeci.com.bt_enable_pressed";
    public static String GPS_ENABLE_PRESSED_ACTION = "gmeci.com.gps_enable_pressed";
    public static String TYPE = "gmeci.com.bt_notification_type";

    //Pawlos input type, obst defaults, spinner and obstruction GUI constants
    public static String BT_NOTIFICATION_ACTION = "gmeci.com.bt_enotification_pressed";
    public static String STOP_COLLECTING_NOTIFICATION_ACTION = "gmeci.com.collection_stop";
    public static final String STOP_COLLECTING_ROUTE_NOTIFICATION = "gmeci.com.route";
    public static final String STOP_COLLECTING_GRADIENT_NOTIFICATION = "gmeci.com.gradient";
    public static String GPS_NOTIFICATION_ACTION = "gmeci.com.gps_notification_pressed";
    /*apron group and type constants */
    public static String APRON_PREFIX = "apron_";
    public static final String APRON_TYPE = "APRON_TYPE";
    public static final String APRON_GROUP = "APRON_GROUP";
    public static String APRON_COUNT = "APRON_COUNT";
    /*pp route group and type constants*/
    public static String APRON_ROUTE_PREFIX = "apron_route_";
    public static final String APRON_ROUTE_TAXIWAY_TYPE = "Taxiway";
    public static final String APRON_ROUTE_BOUNDARY_TYPE = "Boundary";

    public static String GetUIDFromURI(Uri uri) {
        String URIString = uri.toString();

        URIString = URIString.replace("/alp", "");
        URIString = URIString.replace("/dlp", "");
        String[] Tokens = URIString.split("/");
        int Position = 1;
        if (Tokens != null && Tokens.length > Position + 1) {
            return Tokens[Tokens.length - Position];
        }
        return "";
    }

    public static String GetGroupFromURI(Uri uri) {
        //content://com.gmeci.atskservice.databases.ObstructionProvider/point/default/Building.0
        String URIString = uri.toString();
        URIString = URIString.replace("/alp", "");
        URIString = URIString.replace("/dlp", "");
        URIString = URIString.replace("/agp", "");
        URIString = URIString.replace("/dgp", "");
        URIString = URIString.replace("/pointGroup", "");

        String[] Tokens = URIString.split("/");
        int Position = 2;
        if (Tokens != null && Tokens.length > Position) {
            return Tokens[Tokens.length - Position];
        }
        return ATSKConstants.DEFAULT_GROUP;
    }

    static public int getVersion(Context context, String pkg) {
        PackageManager manager = context.getPackageManager();
        if (manager != null) {
            try {
                PackageInfo pInfo = manager.getPackageInfo(pkg,
                        PackageManager.GET_ACTIVITIES);
                return pInfo.versionCode;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }
        return -1;

    }

}
