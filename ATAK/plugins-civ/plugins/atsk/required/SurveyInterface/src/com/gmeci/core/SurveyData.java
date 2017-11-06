/**
 * The original implementation used this structure and at this time, providing a more object oriented
 * variant might be more trouble than it is worth.   Do not add fields to this.
 */

package com.gmeci.core;

import android.util.Log;

import com.gmeci.atskservice.farp.FARPTankerItem;
import com.gmeci.conversions.Conversions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class SurveyData {

    /** 
     * Measurements stored in the survey class are always in meters, angles always
     * in true.
     */

    @SuppressWarnings("unused")
    private static final String TAG = "SurveyData";

    // 20 km max length/width/diameter
    public static final int MAX_LENGTH = 20000;

    final public Map<String, Object> data = new HashMap<String, Object>();

    private AZ_TYPE type;
    public String uid;

    public SurveyPoint center;
    public double width;
    private double length;

    public double angle;

    public String surface;
    public boolean visible;

    public boolean circularAZ;
    public int aircraftCount;
    public double approachAngle;
    public double departureAngle;
    public String aircraft;
    public int AnchorPosition;
    public final SurveyPoint pointOfOrigin;
    public String PointOfOriginDescription;

    public double approachGlideSlopeDeg;
    public double departureGlideSlopeDeg;
    public double minApproachGlideSlopeDeg;
    public double minDepartureGlideSlopeDeg;
    public double approachElevation;
    public double departureElevation;

    public double endClearZoneInnerWidth;
    public double endClearZoneOuterWidth;
    public double endClearZoneLength;
    public double approachInnerWidth;
    public double approachInnerLength;
    public double approachOuterWidth;
    public double approachOuterLength;
    //setAZPosition
    public double perPIElevation;
    public double hePIElevation;
    public double cdsPIElevation;
    public double perPIOffset;
    public double hePIOffset;
    public double cdsPIOffset;
    public boolean nightDrop;
    //RemarksList and corresponding maps

    public List<String> screenShotFileNameList;
    //setLZEdges
    public final LZEdges edges;
    //setElevation
    public double slopeL;
    public double slopeW;
    public double highestElevation;
    //info
    public String info;
    public double aDisplacedThreshold;
    public double dDisplacedThreshold;
    public PointObstruction worstApproachIncursionPoint;
    public PointObstruction worstDepartureIncursionPoint;

    public final double[] FAMRxAngle = new double[2];
    public String FAMRxShape = ATSKConstants.FARP_RX_LAYOUT_SINGLE;
    public final SurveyPoint[] FAMPoints = new SurveyPoint[2];

    public boolean valid;

    public final String getMetaString(String key) {
        return getMetaString(key, null);
    }

    public final String getMetaString(String key, String defaultVal) {
        String r = defaultVal;
        if (data.containsKey(key))
            r = typedGet(data, key, String.class);
        if (r == null)
            r = defaultVal;
        return r;
    }

    public final double getMetaDouble(String key, double defaultVal) {
        Double r = typedGet(data, key, Double.class);
        return r != null ? r : defaultVal;
    }

    public final boolean getMetaBoolean(String key, boolean defaultVal) {
        Boolean r = typedGet(data, key, Boolean.class);
        return r != null ? r : defaultVal;
    }

    public final SurveyPoint getMetaPoint(String key, SurveyPoint defaultVal) {
        String r = getMetaString(key);
        if (r != null && r.contains(",")) {
            String[] v = r.split(",");
            SurveyPoint sp = new SurveyPoint();
            for (int i = 0; i < v.length; i++) {
                double d;
                try {
                    d = Double.parseDouble(v[i]);
                } catch (Exception e) {
                    d = 0;
                }
                switch (i) {
                    case 0:
                        sp.lat = d;
                        break;
                    case 1:
                        sp.lon = d;
                        break;
                    case 2:
                        sp.setHAE(d);
                        break;
                    case 3:
                        sp.circularError = d;
                        break;
                    case 4:
                        sp.linearError = d;
                        break;
                }
            }
            return sp;
        }
        return defaultVal;
    }

    public final void setMetaDouble(String key, double value) {
        data.put(key, value);
    }

    public final void setMetaString(String key, String value) {
        data.put(key, value);
    }

    public final void setMetaBoolean(String key, boolean value) {
        data.put(key, value);
    }

    public final void setMetaPoint(String key, SurveyPoint point) {
        if (point != null) {
            setMetaString(key, point.toCSV());
        } else
            data.remove(key);
    }

    private static <T> T typedGet(Map<String, Object> map, String key,
            Class<T> type) {
        Object o = map.get(key);
        if (o == null)
            return null;
        try {
            return (T) o;
        } catch (ClassCastException e) {
            return null;
        }
    }

    public SurveyData() {
        circularAZ = false;

        valid = false;
        length = 0;
        width = 0;
        angle = 0;

        aircraft = "";

        endClearZoneInnerWidth = 0;
        endClearZoneOuterWidth = 0;
        endClearZoneLength = 0;

        type = AZ_TYPE.LZ;
        approachInnerLength = 0;
        approachInnerWidth = 0;
        approachOuterWidth = 0;
        approachOuterLength = 0;

        perPIElevation = SurveyPoint.Altitude.INVALID;
        hePIElevation = SurveyPoint.Altitude.INVALID;
        cdsPIElevation = SurveyPoint.Altitude.INVALID;
        approachGlideSlopeDeg = 99;
        departureGlideSlopeDeg = 99;
        surface = "ASPHALT";

        AnchorPosition = ATSKConstants.ANCHOR_APPROACH_CENTER;
        center = new SurveyPoint();
        pointOfOrigin = new SurveyPoint();
        highestElevation = 0;

        uid = "";
        screenShotFileNameList = new ArrayList<String>();

        edges = new LZEdges();

        info = "";

        // add back in because the HLZ processing is not smart enough to do the 
        // correct thing.
        hePIOffset = 457;
        perPIOffset = 274;
        cdsPIOffset = 183;

        aDisplacedThreshold = 0;
        dDisplacedThreshold = 0;
    }

    static public int getFARPSideIndex(boolean IsRight) {
        if (IsRight)
            return 0;
        return 1;
    }

    public String toString() {
        String PositionString = String.format("%s %s", this.type.name(),
                Conversions.GetLatLonDM(this.center.lat, this.center.lon));
        String SizeString = String.format("l=%.1f w=%.1f a=%.1f",
                length, width, angle);
        return SizeString + "\n" + PositionString;
    }

    public double getLength() {
        return getLength(true);
    }

    public double getLength(boolean Usable) {
        if (this.type == AZ_TYPE.LZ && !Usable) {
            return this.length - this.edges.ApproachOverrunLength_m
                    - this.edges.DepartureOverrunLength_m;
        }
        return this.length;
    }

    public void setLength(double length_m, boolean Usable) {
        if (this.type == AZ_TYPE.LZ && !Usable)
            length_m += this.edges.ApproachOverrunLength_m
                    + this.edges.DepartureOverrunLength_m;
        this.length = Math.min(Math.abs(length_m), MAX_LENGTH);
    }

    public void setLength(double length_m) {
        setLength(length_m, true);
    }

    public void setOverrunLength(double length_m, boolean approach) {
        if (this.type != AZ_TYPE.LZ)
            return;
        length_m = Math.min(Math.abs(length_m), MAX_LENGTH / 2);
        if (approach)
            this.edges.ApproachOverrunLength_m = length_m;
        else
            this.edges.DepartureOverrunLength_m = length_m;
    }

    public void setWidth(double width_m) {
        this.width = Math.min(Math.abs(width_m), MAX_LENGTH);
    }

    /**
     * Return full width of survey (same as survey.width except for LZ)
     * @return Full width of survey
     */
    public double getFullWidth() {
        if (this.type != AZ_TYPE.LZ)
            return width;
        return width + 2 * (edges.ShoulderWidth_m
                + edges.MaintainedAreaWidth_m + edges.GradedAreaWidth_m);
    }

    public void setRadius(double radius_m) {
        if (!circularAZ)
            return;
        radius_m = Math.min(Math.abs(radius_m * 2), MAX_LENGTH);
        if (width > length)
            width = radius_m;
        else
            length = radius_m;
    }

    public double getRadius() {
        return circularAZ ? Math.max(width, length) / 2 : -1;
    }

    public void setSurveyName(final String SurveyName) {
        data.put(ATSKConstants.FORM_AZ_NAME, SurveyName);
    }

    public String getSurveyName() {
        if (data.containsKey(ATSKConstants.FORM_AZ_NAME)) {
            return (String) data.get(ATSKConstants.FORM_AZ_NAME);
        } else {
            Log.w(TAG, "Setting survey name to UID " + uid);
            data.put(ATSKConstants.FORM_AZ_NAME, uid);
            return uid;
        }
    }

    public boolean ltfw;
    public boolean stol;

    public boolean surveyIsLTFW() {
        return ltfw && this.type == AZ_TYPE.LZ;
    }

    public boolean surveyIsSTOL() {
        return stol && this.type == AZ_TYPE.LZ;
    }

    public AZ_TYPE getType() {
        return type;
    }

    public void setType(AZ_TYPE AZType) {
        this.type = AZType;
        if (AZType == AZ_TYPE.LZ) {
            this.aircraft = "NONE";
            this.length = 0;
            this.width = 0;
        } else if (AZType == AZ_TYPE.HLZ) {
            this.length = 0;
            this.width = 0;
        } else if (AZType == AZ_TYPE.DZ) {
            this.length = 0;
            this.width = 0;
        }
    }

    public enum AZ_TYPE {
        LZ, DZ, HLZ, FARP, STOL, LTFW
    }

    public int getApproachAnchor() {
        if (AnchorPosition == ATSKConstants.ANCHOR_APPROACH_LEFT
                || AnchorPosition == ATSKConstants.ANCHOR_DEPARTURE_LEFT)
            return ATSKConstants.ANCHOR_APPROACH_LEFT;
        if (AnchorPosition == ATSKConstants.ANCHOR_APPROACH_RIGHT
                || AnchorPosition == ATSKConstants.ANCHOR_DEPARTURE_RIGHT)
            return ATSKConstants.ANCHOR_APPROACH_RIGHT;
        return ATSKConstants.ANCHOR_APPROACH_CENTER;
    }

    public int getDepartureAnchor() {
        if (AnchorPosition == ATSKConstants.ANCHOR_APPROACH_LEFT
                || AnchorPosition == ATSKConstants.ANCHOR_DEPARTURE_LEFT)
            return ATSKConstants.ANCHOR_DEPARTURE_LEFT;
        if (AnchorPosition == ATSKConstants.ANCHOR_APPROACH_RIGHT
                || AnchorPosition == ATSKConstants.ANCHOR_DEPARTURE_RIGHT)
            return ATSKConstants.ANCHOR_DEPARTURE_RIGHT;
        return ATSKConstants.ANCHOR_DEPARTURE_CENTER;
    }

    /**
     * FARP only - Get FAM cart distance based on cart type
     * @return Distance between FAM cart and refueling point in meters
     */
    public double getFAMDistance() {
        double ft = 100;
        if (FAMRxShape != null) {
            if (FAMRxShape.equals(ATSKConstants.FARP_RX_LAYOUT_RGR))
                ft = 200;
            else if (FAMRxShape.equals(ATSKConstants.FARP_RX_LAYOUT_SINGLE))
                ft = 150;
        }
        return ft / Conversions.M2F;
    }

    /**
     * Maximum distance from FAM cart position to refuel point
     */
    public double getHoseLength() {
        double ft = 200;
        if (FAMRxShape != null) {
            if (FAMRxShape.equals(ATSKConstants.FARP_RX_LAYOUT_RGR))
                ft = 300;
            else if (FAMRxShape.equals(ATSKConstants.FARP_RX_LAYOUT_SINGLE))
                ft = 150;
        }
        return ft / Conversions.M2F;
    }

    /**
     * Get the currently active FAM cart index
     * @return FAM cart index or -1 if neither
     */
    public int getActiveFAMIndex() {
        for (int i = 0; i < this.FAMPoints.length; i++) {
            if (this.FAMPoints[i] != null && this.FAMPoints[i].visible)
                return i;
        }
        return -1;
    }

    /**
     * FARP only - Update FAM receiver shape and make sure distances
     * are constrained according to spec
     */
    public void updateFAMShape(String newShape, FARPTankerItem tanker) {
        FAMRxShape = newShape;
        int activeIndex = getActiveFAMIndex();
        if (activeIndex == -1)
            return;
        SurveyPoint fuelPoint = tanker != null ?
                tanker.getFuelPoint(this, activeIndex == 0)
                : this.center;
        for (SurveyPoint famPoint : this.FAMPoints) {
            if (famPoint != null) {
                double rxAng = Conversions.calculateAngle(fuelPoint,
                        famPoint);
                SurveyPoint sp = Conversions.AROffset(fuelPoint,
                        rxAng, this.getFAMDistance());
                famPoint.lat = sp.lat;
                famPoint.lon = sp.lon;
            }
        }
    }
}
