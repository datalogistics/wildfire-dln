
package com.gmeci.atskservice.resolvers;

public class AZURIConstants {

    public static final String AUTHORITY_BASE = "com.gmeci.atskservice.databases.AZProvider";
    public final static String BaseURIString = "content://" + AUTHORITY_BASE;
    public static final String AZ = "AZ";
    public static final String AZS = "AZS";
    public static final String AZ_SETTINGS = "SETTINGS";

    public static final String AZ_INFORMATION_UPDATED = "azi";
    public static final String AZ_PARAMETERS_UPDATED = "azpa";
    public static final String DZ_PARAMETERS_UPDATED = "dzpa";
    public static final String DZ_PIS_UPDATED = "dzpi";
    public static final String AZ_REMARKS_UPDATED = "azrk";
    public static final String AZ_SCREENSHOTS_UPDATED = "azrk";

    public final static String SINGLE_AZ_URI = BaseURIString + "/" + AZ;

    public final static String AZ_SETTING_URI = BaseURIString + "/"
            + AZ_SETTINGS;
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_UID = "uid";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_VALUE = "value";
    public static final String COLUMN_TYPE = "type";

    public static final String COLUMN_LAT = "lat";
    public static final String COLUMN_LON = "lon";
    public static final String COLUMN_HAE_M = "hae";
    public static final String COLUMN_LENGTH_M = "length";
    public static final String COLUMN_WIDTH_M = "width";
    public static final String COLUMN_ROTATION = "rotation";
    public static final String COLUMN_AZ_JSON = "json";
    public static final String COLUMN_AZ_VISIBLE = "visible";

    public static final String TABLE_AZ = "assault_zones";
    public static final String TABLE_AZ_SETTINGS = "az_settings";

    public static final String[] allColumnsAZSettings = {
            COLUMN_ID,
            COLUMN_NAME,
            COLUMN_VALUE
    };

    public static final String AZ_SETTINGS_DATABASE_CREATE = "CREATE TABLE IF NOT EXISTS  "
            + TABLE_AZ_SETTINGS + "("
            + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COLUMN_NAME + " TEXT NOT NULL, "
            + COLUMN_VALUE + " TEXT NOT NULL "
            + ");";

    public static final String AZ_DATABASE_CREATE = "CREATE TABLE IF NOT EXISTS  "
            + TABLE_AZ + "("
            + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COLUMN_UID + " TEXT NOT NULL, "
            + COLUMN_NAME + " TEXT NOT NULL, "
            + COLUMN_TYPE + " TEXT NOT NULL, "
            + COLUMN_LAT + " REAL NOT NULL, "
            + COLUMN_LON + " REAL NOT NULL, "
            + COLUMN_HAE_M + " REAL NOT NULL, "
            + COLUMN_ROTATION + " REAL NOT NULL, "
            + COLUMN_LENGTH_M + " REAL NOT NULL, "
            + COLUMN_WIDTH_M + " REAL NOT NULL, "
            + COLUMN_AZ_JSON + " TEXT NOT NULL, "
            + COLUMN_AZ_VISIBLE + " TEXT NOT NULL "
            + ");";
    public static final int JSON_INDEX = 10;
    public static final int UID_INDEX = 1;
    public static final int NAME_INDEX = 2;
    public static final int TYPE_INDEX = 3;
    public static final int COLUMN_LAT_INDEX = 4;
    public static final int COLUMN_LON_INDEX = 5;
    public static final int COLUMN_VISIBLE_INDEX = 11;

    public static final String[] allColumnsAZ = {
            COLUMN_ID,
            COLUMN_UID,
            COLUMN_NAME,
            COLUMN_TYPE,
            COLUMN_LAT,
            COLUMN_LON,
            COLUMN_HAE_M,
            COLUMN_ROTATION,
            COLUMN_LENGTH_M,
            COLUMN_WIDTH_M,
            COLUMN_AZ_JSON,
            COLUMN_AZ_VISIBLE
    };
    public static final String[] uidOnlyColumnsAZ = {
            COLUMN_UID

    };

}
