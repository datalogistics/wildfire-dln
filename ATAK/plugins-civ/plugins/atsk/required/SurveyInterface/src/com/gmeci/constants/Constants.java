
package com.gmeci.constants;

public class Constants {

    public static final int FL_NONE = 0;
    public static final int FL_HIDE_LABEL = 1 << 0;
    public static final int FL_LABEL_TURNED = 1 << 1;
    public static final int FL_LABEL_MEASURE_LINES = 1 << 2;

    //Hardware
    public static final String SERIAL_BAUD_RATE = "SerialBaudRate";
    public static final String SURVEY_NAME = "NAME";

    public static final String FARP_AC_FAM_LINE_TYPE_SELECTED = "FARP_ac-fam_line_type-selected";
    public static final String FARP_AC_FAM_LINE_TYPE = "FARP_ac-fam_line_type";
    public static final String FARP_FAM_RX_LINE_TYPE = "FARP_fam-rx_line_type";
    public static final String FARP_FAM_RX_LINE_TYPE_SELECTED = "FARP_fam-rx_line_type-selected";
    public static final String FARP_FAM_TYPE = "FARP_fam";
    public static final String FARP_RX_TYPE = "FARP_rx";

    // LZ AMPs
    //public static final String AMP_1_DAY = "AMP-1 Day";
    //public static final String AMP_1_NIGHT = "AMP-1 Night";
    public static final String AMP_2_DAY = "AMP-2 Day";
    public static final String AMP_2_NIGHT = "AMP-2 Night";
    public static final String AMP_3_DAY = "AMP-3 Day Standard";
    public static final String AMP_3_NIGHT = "AMP-3 Night Standard";
    public static final String AMP_3_DAY_BOX = "AMP-3 Day 1000’ Box";
    public static final String AMP_3_NIGHT_BOX = "AMP-3 Night 1000’ Box";
    public static final String AMP_2_DAY_LTFW = "AMP-2 Day LTFW";
    public static final String AMP_2_NIGHT_LTFW = "AMP-2 Night LTFW";
    public static final String AMP_3_DAY_LTFW = "AMP-3 Day LTFW";
    public static final String AMP_3_NIGHT_LTFW = "AMP-3 Night LTFW";
    public static final String[] LZ_AMPS = new String[] {
            /*AMP_1_DAY, AMP_1_NIGHT, */AMP_2_DAY, AMP_2_NIGHT, AMP_3_DAY,
            AMP_3_NIGHT, AMP_3_DAY_BOX, AMP_3_NIGHT_BOX, AMP_2_DAY_LTFW,
            AMP_2_NIGHT_LTFW, AMP_3_DAY_LTFW, AMP_3_NIGHT_LTFW
    };

    public static final String BLUETOOTH_LIST = "BLUETOOTH_LIST";

    public static final String ACTIVE_POINT_OBSTRUCTIONS_PREFERENCE = "Active Point Obstructions";
    public static final String ACTIVE_LINE_OBSTRUCTIONS_PREFERENCE = "Active Line Obstructions";
    public static final String ACTIVE_AREA_OBSTRUCTIONS_PREFERENCE = "Active Area Obstructions";
    public static final String REQUIRE_LRF_APPROVAL = "Require LRF APproval";
    public static final String GRADIENT_MISSING_DATA = "Missing Gradient Data";
    public static final String GRADIENT_STEEP_SEGMENT = "Steep Gradient";
    public static final String GRADIENT_STEEP_OVERALL = "Steep OVERALL Gradient";
    public static final String AO_GENERIC_AREA = "Generic Area";
    public static final String AO_BUILDINGS = "Buildings";
    public static final String AO_BUSHES = "Bushes";
    public static final String AO_CRATERS = "Craters";
    public static final String AO_TREES = "Trees";
    public static final String AO_SWAMP = "Swamp";
    public static final String AO_POOL = "Pool";
    public static final String AO_SAND = "Sand";
    public static final String AO_POND = "Pond";
    public static final String AO_OCEAN = "Ocean";
    public static final String AO_LAKE = "Lake";
    public static final String AO_GRAVEL = "Gravel";
    public static final String AO_APRON = "Apron";
    public static final String[] AREA_TYPES = new String[] {
            AO_POOL, AO_GENERIC_AREA, AO_APRON, AO_BUILDINGS, AO_BUSHES,
            AO_CRATERS,
            AO_GRAVEL, AO_LAKE, AO_OCEAN, AO_POND, AO_SAND, AO_SWAMP, AO_TREES
    };
    public static final String[] DEFAULT_AREAS = new String[] {
            AO_GENERIC_AREA, AO_APRON, AO_BUILDINGS, AO_BUSHES, AO_CRATERS,
            AO_TREES,
            AO_SAND, AO_LAKE, AO_GRAVEL
    };
    public static final String LO_ARRESTING_GEAR = "Arresting Gear";
    public static final String LO_BARRIER = "Barrier";
    public static final String LO_BERMS = "Berms";
    public static final String LO_BRIDGE = "Bridge";
    public static final String LO_CANAL = "Canal";
    public static final String LO_CONTOUR_LINE = "Contour Line";
    public static final String LO_CURB = "Curb";
    public static final String LO_CULVERT = "Culvert";
    public static final String LO_DITCH = "Ditch";
    public static final String LO_FENCELINE = "Fenceline";
    public static final String LO_GATE = "Gate";
    public static final String LO_GENERIC_ROUTE = "Generic Route";
    public static final String LO_GUARD_RAIL = "Guard Rail";
    public static final String LO_HEDGES = "Hedges";
    public static final String LO_HIGHWAY = "Highway";
    public static final String LO_HILL = "Hill";
    public static final String LO_LIGHT_POLE_WIRES = "Light Poles with Wires";
    public static final String LO_MOUNTAIN = "Mountain";
    public static final String LO_PATH = "Path";
    public static final String LO_MOUND = "Mound";
    public static final String LO_PIPELINE = "Pipeline";
    public static final String LO_POLES_WIRES = "Poles with Wires";
    public static final String LO_POWERLINES = "Powerlines";
    public static final String LO_RAILROAD = "Railroad";
    public static final String LO_RIDGELINE = "Ridge Line";
    public static final String LO_RIVER = "River";
    public static final String LO_ROAD = "Road";
    public static final String LO_RUTS = "Ruts";
    public static final String LO_SLOPE = "Slope in Percent";
    public static final String LO_TRAIL = "Trail";
    public static final String LO_TREELINE = "Treeline";
    public static final String LO_WALL = "Wall";
    public static final String LO_WIRES = "Wires";
    public static final String LO_LEADER = "Leader Line";
    public static final String LO_TAXIWAY = "Taxiway";
    public static final String[] LINE_TYPES = new String[] {
            LO_ARRESTING_GEAR, LO_BERMS, LO_BARRIER, LO_BRIDGE,
            LO_CANAL, LO_CONTOUR_LINE, LO_CULVERT, LO_CURB, LO_DITCH,
            LO_FENCELINE, LO_GATE, LO_GENERIC_ROUTE,
            LO_GUARD_RAIL, LO_HEDGES, LO_HIGHWAY, LO_HILL, LO_LIGHT_POLE_WIRES,
            LO_MOUNTAIN, LO_PATH, LO_MOUND, LO_PIPELINE, LO_POLES_WIRES,
            LO_POWERLINES, LO_RAILROAD, LO_RIDGELINE, LO_RIVER, LO_ROAD,
            LO_RUTS, LO_SLOPE,
            LO_TRAIL, LO_TREELINE, LO_WALL, LO_WIRES, LO_TAXIWAY
    };
    public static final String[] DEFAULT_LINES = new String[] {
            LO_BERMS, LO_CURB, LO_DITCH, LO_FENCELINE, LO_TAXIWAY,
            LO_GENERIC_ROUTE,
            LO_POLES_WIRES, LO_POWERLINES, LO_ROAD, LO_RUTS, LO_TREELINE
    };
    public static final String[] FLAT_TERRAIN = new String[] {
            LO_ROAD, LO_RUTS, LO_CURB, LO_TAXIWAY,
            AO_LAKE, AO_GRAVEL, AO_SAND, AO_APRON
    };
    public static final String[] TAXI_TYPES = new String[] {
            LO_TAXIWAY, AO_APRON
    };
    public static String PO_APRON_ANCHOR = "apron_anchor";
    public static final String PO_BLANK = "Blank";
    public static final String PO_RED = "Red";
    public static final String PO_BLACK = "Black";
    public static final String PO_AIRFIELD_INSTRUMENT = "Airfield Instrument";
    public static final String PO_ANTENNA = "Antenna";
    public static final String PO_BERMS = "Berms";
    public static final String PO_BUILDING = "Building";
    public static final String PO_BUSH = "Bush";
    public static final String PO_CRATER = "Crater";
    public static final String PO_DUMPSTER = "Dumpster";
    public static final String PO_FIRE_HYDRANT = "Fire Hydrant";
    public static final String PO_FLAGPOLE = "Flagpole";
    public static final String PO_FUEL_TANK = "Fuel Tank";
    public static final String PO_GENERIC_POINT = "Generic Point";
    public static final String PO_RAB_LINE = "R&B Line";
    public static final String PO_RAB_CIRCLE = "R&B Circle";
    public static final String PO_LABEL = "Label";
    public static final String PO_HVAC_UNIT = "HVAC Unit";
    public static final String PO_LASER = "Laser Target";
    public static final String PO_LEDGE = "Ledge";
    public static final String PO_LIGHT = "Light";
    public static final String PO_LIGHT_POLE = "Light Pole";
    public static final String PO_MOUND = "Mound";
    public static final String PO_PEAK = "Peak";
    public static final String PO_POLE = "Pole";
    public static final String PO_PYLON = "Pylon";
    public static final String PO_ROTATING_BEACON = "Rotating Beacon";
    public static final String PO_SAT_DISH = "Satellite Dish";
    public static final String PO_SIGN = "Sign";
    public static final String PO_TRANSFORMER = "Transformer";
    public static final String PO_TREE = "Tree";
    public static final String PO_WINDSOCK = "Windsock";
    public static final String PO_WXVANE = "WX Vane";
    public static final String PO_CBR_HIDDEN = "cbr_hidden";
    public static final String PO_CBR = "cbr";
    public static final String PO_LZ_DCP = "lz_dcp";
    public static final String PO_GTM = "gtm";
    public static final String PO_AMP = "amp";
    public static final String PO_AMP_PANEL = PO_AMP + "_panel";
    public static final String PO_AMP_PANEL_ORANGE = PO_AMP_PANEL + "_orange";
    public static final String PO_AMP_LIGHT = PO_AMP + "_light";
    public static final String PO_AMP_RCL = PO_AMP + "_rcl";
    public static final String PO_AMP_STROBE = PO_AMP + "_strobe";
    public static final String PO_AMP_BOX = PO_AMP + "_box";
    public static String[] TERRAIN_POINT_TYPES = new String[] {
            PO_CRATER, PO_PEAK, PO_BERMS, PO_MOUND
    };
    public static String[] TERRAIN_LINE_TYPES = new String[] {
            LO_CANAL, LO_DITCH, PO_BERMS, LO_HILL, LO_MOUNTAIN, LO_MOUND,
            LO_RIDGELINE, LO_SLOPE
    };
    public static String[] TERRAIN_AREA_TYPES = new String[] {
            AO_CRATERS, AO_LAKE, AO_POND
    };

    public static final String[] POINT_TYPES = new String[] {
            PO_AIRFIELD_INSTRUMENT, PO_ANTENNA, PO_BERMS, PO_BUILDING, PO_BUSH,
            PO_CRATER, PO_DUMPSTER, PO_FIRE_HYDRANT, PO_FLAGPOLE, PO_FUEL_TANK,
            PO_GENERIC_POINT, PO_RAB_LINE, PO_RAB_CIRCLE, PO_HVAC_UNIT,
            PO_LEDGE, PO_LIGHT, PO_LIGHT_POLE, PO_MOUND, PO_PEAK, PO_POLE,
            PO_PYLON, PO_ROTATING_BEACON,
            PO_SAT_DISH, PO_SIGN, PO_TRANSFORMER, PO_TREE, PO_WINDSOCK,
            PO_WXVANE, PO_LABEL, PO_LZ_DCP, PO_GTM
    };
    public static final String[] POINT_TYPES_WITH_LW = new String[] {
            PO_AIRFIELD_INSTRUMENT, PO_BERMS, PO_BUILDING,
            PO_DUMPSTER, PO_GENERIC_POINT, PO_HVAC_UNIT,
            PO_LEDGE, PO_LIGHT, PO_LIGHT_POLE, PO_MOUND, PO_PYLON,
            PO_SIGN, PO_TRANSFORMER
    };//removed BUSH 

    public static String[] CIRCULAR_POINT_TYPES = new String[]
    {
            PO_ANTENNA, PO_BUSH, PO_FIRE_HYDRANT, PO_FLAGPOLE, PO_TREE,
            PO_PYLON, PO_POLE, PO_LIGHT_POLE, PO_LIGHT
    };

    public static final String[] DEFAULT_POINTS = new String[] {
            PO_BUILDING, PO_BUSH, PO_GENERIC_POINT, PO_POLE, PO_TREE, PO_SIGN,
            PO_RAB_LINE, PO_RAB_CIRCLE
    };

    //Types for POINTS on a DZ
    public static final String POINT_PI_HE = "HE";
    public static final String POINT_PI_PER = "PER";
    public static final String POINT_PI_CDS = "CDS";
    public static final String POINT_PO = "PO";
    public static final String POINT_ANCHOR = "anchor";
    public static final String POINT_DEP_ANCHOR = "dep_" + POINT_ANCHOR;
    public static final String POINT_CENTER_ANCHOR = "center_" + POINT_ANCHOR;
    public static final String POINT_APP_ANCHOR = "app_" + POINT_ANCHOR;

    public static final String DISTRESS_POTHOLE = "Pothole";
    public static final String DISTRESS_RUTS = "Ruts";
    public static final String DISTRESS_LOOSE_AGG = "Loose Aggregate";
    public static final String DISTRESS_DUST = "Dust";
    public static final String DISTRESS_ROLLING_RESISTANT = "Rolling Resistant";
    public static final String DISTRESS_JET_EROSION = "Jet Blast Erosion";
    public static final String DISTRESS_STABLE_FAILURE = "Stabilized Layer Failure";
    public static final String[] SURFACE_DISTRESSES = new String[] {
            DISTRESS_POTHOLE, DISTRESS_RUTS, DISTRESS_LOOSE_AGG, DISTRESS_DUST,
            DISTRESS_ROLLING_RESISTANT, DISTRESS_JET_EROSION,
            DISTRESS_STABLE_FAILURE
    };

    public static final String DISTRESS_GREEN = "GREEN";
    public static final String DISTRESS_YELLOW = "YELLOW";
    public static final String DISTRESS_RED = "RED";

    public static String[] SURFACE_DISTRESSES_SEVERITY = new String[] {
            DISTRESS_GREEN, DISTRESS_YELLOW, DISTRESS_RED
    };

    public static String APPROACH_FORMAT = "APPROACH_FORMAT";
    public static String DEPARTURE_FORMAT = "DEPARTURE_FORMAT";

    // Helper methods for arrays of types
    private static boolean isType(String type, String[] types) {
        for (String t : types) {
            if (t.equals(type))
                return true;
        }
        return false;
    }

    public static boolean isPoint(String type) {
        return isType(type, POINT_TYPES);
    }

    public static boolean isPointWithLW(String type) {
        return isType(type, POINT_TYPES_WITH_LW);
    }

    public static boolean isLine(String type) {
        return isType(type, LINE_TYPES);
    }

    public static boolean isArea(String type) {
        return isType(type, AREA_TYPES);
    }

    public static boolean isFlatTerrain(String type) {
        return isType(type, FLAT_TERRAIN);
    }

    public static boolean isTaxiway(String type) {
        return isType(type, TAXI_TYPES);
    }
}
