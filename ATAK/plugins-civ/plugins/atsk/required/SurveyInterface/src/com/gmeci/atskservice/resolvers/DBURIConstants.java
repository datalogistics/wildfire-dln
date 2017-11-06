
package com.gmeci.atskservice.resolvers;

public class DBURIConstants {

    public static final String OBSTRUCTION_AUTHORITY_BASE = "com.gmeci.atskservice.databases.ObstructionProvider";
    public static final String GRADIENT_AUTHORITY_BASE = "com.gmeci.atskservice.databases.GradientProvider";
    public static final String LINE = "line";
    public static final String GRADIENT = "gradient";
    public static final String GRADIENTS = "gradients";
    public static final String LINES = "lines";
    public static final String POINT = "point";
    public static final String POINTS = "points";
    public static final String LINE_POINT = "line_points";
    public static final String LINE_POINT_DISTINCT = "line_points_distinct";

    public static final String GRADIENT_POINT = "gradient_points";

    public final static String ObstructionBaseURIString = "content://"
            + DBURIConstants.OBSTRUCTION_AUTHORITY_BASE;
    public final static String GradientBaseURIString = "content://"
            + DBURIConstants.GRADIENT_AUTHORITY_BASE;
    public final static String POINT_URI = ObstructionBaseURIString + "/"
            + DBURIConstants.POINT;
    public final static String LINE_URI = ObstructionBaseURIString + "/"
            + DBURIConstants.LINE;
    public final static String LINE_POINT_URI = ObstructionBaseURIString + "/"
            + DBURIConstants.LINE_POINT;
    public final static String GRADIENT_URI = GradientBaseURIString + "/"
            + DBURIConstants.GRADIENT;
    public final static String GRADIENT_POINT_URI = GradientBaseURIString + "/"
            + DBURIConstants.GRADIENT_POINT;
    public final static String LINE_POINT_DISTINCT_URI = ObstructionBaseURIString
            + "/" + DBURIConstants.LINE_POINT_DISTINCT;

    public static final String TABLE_LINE_OBSTRUCTIONS = "line_obstructions";
    public static final String TABLE_LINE_OBSTRUCTION_POINTS = "line_obstruction_points";
    public static final String TABLE_GRADIENT = "gradients";
    public static final String TABLE_GRADIENT_POINTS = "gradient_points";

    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_UID = "uid";
    public static final String COLUMN_GROUP_NAME_LINE = "lo_group";
    public static final String COLUMN_LAT = "lat";
    public static final String COLUMN_LON = "lon";
    public static final String COLUMN_HAE_M = "hae";
    public static final String COLUMN_TYPE = "type";
    public static final String COLUMN_DESCRIPTION = "remark";
    public static final String COLUMN_LE = "le";
    public static final String COLUMN_CE = "ce";
    public static final String COLUMN_HOW = "how";
    public static final String COLUMN_HEIGHT = "height";
    public static final String COLUMN_POINT_ORDER = "point_order";
    public static final String COLUMN_CLOSED = "closed";
    public static final String COLUMN_FILLED = "filled";

    public static final String TABLE_POINT_OBSTRUCTIONS = "point_obstructions";
    public static final String COLUMN_GROUP_NAME_POINT = "po_group";
    public static final String COLUMN_WIDTH = "width";
    public static final String COLUMN_LENGTH = "length";
    public static final String COLUMN_ROTATION = "rotation";
    public static final String COLUMN_ANALYSIS_STATE = "analysis_state";
    public static final String COLUMN_SHOW = "show";
    public static final String COLUMN_RTK = "rtk";
    public static final String COLUMN_RANGE_FROM_START = "range_from_start";
    public static final String COLUMN_ANALYZED = "analized";
    public static final String COLUMN_INDEX = "transverse_index";
    public static final String COLUMN_TOP_COLLECTED = "top_collected";
    public static final String COLUMN_COLLECTION_METHOD = "collection_method";

    public static final String COLUMN_SHOULDER_GRADIENT_L = "shoulder_gradient_l";
    public static final String COLUMN_SHOULDER_GRADIENT_R = "shoulder_gradient_r";
    public static final String COLUMN_GRADED_GRADIENT_L = "graded_gradient_l";
    public static final String COLUMN_GRADED_GRADIENT_R = "graded_gradient_r";
    public static final String COLUMN_MAINTAINED_GRADIENT_L = "maintained_gradient_l";
    public static final String COLUMN_MAINTAINED_GRADIENT_R = "maintained_gradient_r";
    public static final String COLUMN_LZ_GRADIENT_L = "lz_gradient_l";
    public static final String COLUMN_LZ_GRADIENT_R = "lz_gradient_r";
    public static final String COLUMN_HIDDEN = "gradient_hidden";

    public static final String[] allColumnsPoint = {
            COLUMN_ID,
            COLUMN_GROUP_NAME_POINT,
            COLUMN_UID,
            COLUMN_TYPE,
            COLUMN_LAT,
            COLUMN_LON,
            COLUMN_HAE_M,
            COLUMN_LE,
            COLUMN_CE,
            COLUMN_HEIGHT,
            COLUMN_WIDTH,
            COLUMN_LENGTH,
            COLUMN_DESCRIPTION,
            COLUMN_ROTATION,
            COLUMN_TOP_COLLECTED,
            COLUMN_COLLECTION_METHOD
    };

    public static final String[] allColumnsLine = {
            COLUMN_ID,
            COLUMN_GROUP_NAME_LINE,
            COLUMN_UID,
            COLUMN_TYPE,
            COLUMN_HEIGHT,
            COLUMN_WIDTH,
            COLUMN_DESCRIPTION,
            COLUMN_CLOSED,
            COLUMN_FILLED,
            COLUMN_TOP_COLLECTED
    };
    public static final String[] allColumnsGradient = {
            COLUMN_ID,
            COLUMN_GROUP_NAME_LINE,
            COLUMN_UID,
            COLUMN_TYPE,
            COLUMN_DESCRIPTION,
            COLUMN_ANALYSIS_STATE,
            COLUMN_SHOULDER_GRADIENT_L,
            COLUMN_SHOULDER_GRADIENT_R,
            COLUMN_GRADED_GRADIENT_L,
            COLUMN_GRADED_GRADIENT_R,
            COLUMN_MAINTAINED_GRADIENT_L,
            COLUMN_MAINTAINED_GRADIENT_R,
            COLUMN_LZ_GRADIENT_L,
            COLUMN_LZ_GRADIENT_R,
            COLUMN_HIDDEN

    };
    public static final String[] allColumnsLinePoints = {
            COLUMN_ID,
            COLUMN_GROUP_NAME_LINE,
            COLUMN_UID,
            COLUMN_POINT_ORDER,
            COLUMN_LAT,
            COLUMN_LON,
            COLUMN_HAE_M,
            COLUMN_LE,
            COLUMN_CE,
            COLUMN_COLLECTION_METHOD
    };
    public static final String[] allColumnsGradientPoints = {
            COLUMN_ID,
            COLUMN_GROUP_NAME_LINE,
            COLUMN_UID,
            COLUMN_POINT_ORDER,
            COLUMN_LAT,
            COLUMN_LON,
            COLUMN_HAE_M,
            COLUMN_LE,
            COLUMN_CE,
            COLUMN_HOW,
            COLUMN_SHOW,
            COLUMN_RTK,
            COLUMN_RANGE_FROM_START,
            COLUMN_ANALYZED,
            COLUMN_INDEX
    };

    public static final String LINE_DATABASE_CREATE = "CREATE TABLE IF NOT EXISTS  "
            + TABLE_LINE_OBSTRUCTIONS + "("
            + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COLUMN_GROUP_NAME_LINE + " TEXT NOT NULL, "
            + COLUMN_UID + " TEXT NOT NULL, "
            + COLUMN_TYPE + " TEXT NOT NULL, "
            + COLUMN_HEIGHT + " REAL, "
            + COLUMN_WIDTH + " REAL, "
            + COLUMN_DESCRIPTION + " TEXT, "
            + COLUMN_CLOSED + " INTEGER, "
            + COLUMN_FILLED + " INTEGER, "
            + COLUMN_TOP_COLLECTED + " INTEGER "
            + ");";

    public static final int SHOULDER_L_GRADIENT_COLUMN_POSITION = 6;
    public static final int SHOULDER_R_GRADIENT_COLUMN_POSITION = 7;

    public static final int GRADED_L_GRADIENT_COLUMN_POSITION = 8;
    public static final int GRADED_R_GRADIENT_COLUMN_POSITION = 9;

    public static final int MAINTAINED_L_GRADIENT_COLUMN_POSITION = 10;
    public static final int MAINTAINED_R_GRADIENT_COLUMN_POSITION = 11;

    public static final int LZ_L_GRADIENT_COLUMN_POSITION = 12;
    public static final int LZ_R_GRADIENT_COLUMN_POSITION = 13;
    public static final int LZ_INTERVAL_GRADIENT_COLUMN_POSITION = 12;
    public static final int LZ_OVERALL_GRADIENT_COLUMN_POSITION = 13;
    public static final String GRADIENT_DATABASE_CREATE = "CREATE TABLE IF NOT EXISTS  "
            + TABLE_GRADIENT + "("
            + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COLUMN_GROUP_NAME_LINE + " TEXT NOT NULL, "
            + COLUMN_UID + " TEXT NOT NULL, "
            + COLUMN_TYPE + " TEXT NOT NULL, "
            + COLUMN_DESCRIPTION + " TEXT, "
            + COLUMN_ANALYSIS_STATE + " TEXT, "
            + COLUMN_SHOULDER_GRADIENT_L + " REAL, "
            + COLUMN_SHOULDER_GRADIENT_R + " REAL, "
            + COLUMN_GRADED_GRADIENT_L + " REAL, "
            + COLUMN_GRADED_GRADIENT_R + " REAL, "
            + COLUMN_MAINTAINED_GRADIENT_L + " REAL, "
            + COLUMN_MAINTAINED_GRADIENT_R + " REAL, "
            + COLUMN_LZ_GRADIENT_L + " REAL, "
            + COLUMN_LZ_GRADIENT_R + " REAL, "
            + COLUMN_HIDDEN + " TEXT"
            + ");";

    public static final String GRADIENT_POINTS_DATABASE_CREATE = "CREATE TABLE IF NOT EXISTS  "
            + TABLE_GRADIENT_POINTS + "("
            + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COLUMN_GROUP_NAME_LINE + " TEXT NOT NULL, "
            + COLUMN_UID + " TEXT NOT NULL, "
            + COLUMN_POINT_ORDER + " INTEGER, "
            + COLUMN_LAT + " REAL, "
            + COLUMN_LON + " REAL, "
            + COLUMN_HAE_M + " REAL, "
            + COLUMN_LE + " REAL, "
            + COLUMN_CE + " REAL, "
            + COLUMN_HOW + " TEXT, "
            + COLUMN_SHOW + " TEXT, "
            + COLUMN_RTK + " TEXT, "
            + COLUMN_RANGE_FROM_START + " REAL, "
            + COLUMN_ANALYZED + " TEXT, "
            + COLUMN_INDEX + " TEXT "
            + ");";
    public static final String LINE_POINTS_DATABASE_CREATE = "CREATE TABLE IF NOT EXISTS  "
            + TABLE_LINE_OBSTRUCTION_POINTS + "("
            + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COLUMN_GROUP_NAME_LINE + " TEXT NOT NULL, "
            + COLUMN_UID + " TEXT NOT NULL, "
            + COLUMN_POINT_ORDER + " INTEGER, "
            + COLUMN_LAT + " REAL, "
            + COLUMN_LON + " REAL, "
            + COLUMN_HAE_M + " REAL, "
            + COLUMN_LE + " REAL, "
            + COLUMN_CE + " REAL, "
            + COLUMN_COLLECTION_METHOD + " INTEGER "
            + ");";

    public static final String LINE_POINTS_DATABASE_UPDATE1 = "ALTER TABLE "
            + TABLE_LINE_OBSTRUCTION_POINTS + " ADD "
            + COLUMN_COLLECTION_METHOD + " INTEGER;";

    public static final String PO_DATABASE_CREATE = "CREATE TABLE IF NOT EXISTS  "
            + TABLE_POINT_OBSTRUCTIONS + "("
            + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COLUMN_GROUP_NAME_POINT + " TEXT NOT NULL, "
            + COLUMN_UID + " TEXT NOT NULL, "
            + COLUMN_TYPE + " TEXT NOT NULL, "
            + COLUMN_LAT + " REAL, "
            + COLUMN_LON + " REAL, "
            + COLUMN_HAE_M + " REAL, "
            + COLUMN_LE + " REAL, "
            + COLUMN_CE + " REAL, "
            + COLUMN_HEIGHT + " REAL, "
            + COLUMN_WIDTH + " REAL, "
            + COLUMN_LENGTH + " REAL, "
            + COLUMN_DESCRIPTION + " TEXT, "
            + COLUMN_ROTATION + " REAL, "
            + COLUMN_TOP_COLLECTED + " INTEGER, "
            + COLUMN_COLLECTION_METHOD + " INTEGER "
            + ");";

    public static final String PO_DATABASE_UPDATE1 = "ALTER TABLE "
            + TABLE_POINT_OBSTRUCTIONS + " ADD "
            + COLUMN_COLLECTION_METHOD + " INTEGER;";

}
