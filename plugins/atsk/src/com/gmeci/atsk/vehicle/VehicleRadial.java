
package com.gmeci.atsk.vehicle;

import android.graphics.Color;

import com.gmeci.vehicle.VehicleBlock;

/**
 * Constants for vehicle radials
 */
public enum VehicleRadial {

    INVALID(-1, -1, "Invalid Radial", "INVALID", Color.BLACK),

    // Helicopters - type 0
    ROTOR_DIAMETER(VehicleBlock.TYPE_HELO, 0, "Rotor Diameter", "R", Color.RED),
    CONTINGENCY(VehicleBlock.TYPE_HELO, 1, "Contingency", "C", Color.GREEN),
    TRAINING(VehicleBlock.TYPE_HELO, 2, "Training", "T", Color.BLUE),
    BROWN_OUT(VehicleBlock.TYPE_HELO, 3, "Brown-out", "B", Color.YELLOW),

    // Planes - type 1
    AAC(VehicleBlock.TYPE_FWAC, 0, "Aircraft to Aircraft Clearance", "AAC",
            Color.RED),
    TOC(VehicleBlock.TYPE_FWAC, 1, "Taxiway Obstruction Clearance", "TOC",
            Color.BLUE),
    APS(VehicleBlock.TYPE_FWAC, 2,
            "Apron Parking Setback from Runway Centerline", "APS",
            Color.YELLOW),
    PTS(VehicleBlock.TYPE_FWAC, 3,
            "Parallel Taxiway Setback from Runway Centerline", "PTS",
            Color.BLACK);

    private final int _type;
    private final int _index;
    private final String _name;
    private final String _abbrev;
    private final int _color;

    VehicleRadial(int type, int index, String name, String abbrev, int color) {
        _type = type;
        _index = index;
        _name = name;
        _abbrev = abbrev;
        _color = color;
    }

    public int getType() {
        return _type;
    }

    public int getIndex() {
        return _index;
    }

    public String getName() {
        return _name;
    }

    public String getAbbrev() {
        return _abbrev;
    }

    public int getColor() {
        return _color;
    }

    public static VehicleRadial getByIndex(int type, int index) {
        for (VehicleRadial vr : values()) {
            if (vr.getType() == type && vr.getIndex() == index)
                return vr;
        }
        return INVALID;
    }
}
