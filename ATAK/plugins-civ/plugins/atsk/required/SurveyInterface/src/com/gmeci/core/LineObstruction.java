
package com.gmeci.core;

import android.os.Parcel;
import android.os.Parcelable;

import com.gmeci.conversions.Conversions;

import java.io.Serializable;
import java.util.ArrayList;

public class LineObstruction implements Parcelable, Serializable {

    public static final Parcelable.Creator<LineObstruction> CREATOR = new Parcelable.Creator<LineObstruction>() {

        @Override
        public LineObstruction createFromParcel(Parcel source) {
            return new LineObstruction(source);
        }

        @Override
        public LineObstruction[] newArray(int size) {
            return new LineObstruction[size];
        }
    };
    public ArrayList<SurveyPoint> points;
    public String remarks;
    public String type;
    public String uid, group;
    public boolean closed;
    public boolean filled;
    public double width;
    public double height;
    public int flags;

    public LineObstruction() {
        this.points = new ArrayList<SurveyPoint>();
        remarks = "none";
        type = "empty";
        closed = false;
        filled = false;
        uid = "none";
        width = 0;
        height = 1;
        flags = 0;
    }

    public LineObstruction(Parcel in) {
        points = new ArrayList<SurveyPoint>();
        readFromParcel(in);
    }

    public String toString() {
        return String.format("%s-%s", uid, type);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeList(points);
        dest.writeString(remarks);
        dest.writeString(type);
        dest.writeString(uid);
        dest.writeByte((byte) (closed ? 1 : 0)); //http://stackoverflow.com/questions/6201311/how-to-read-write-a-boolean-when-implementing-the-parcelable-interface
        dest.writeByte((byte) (filled ? 1 : 0));
        dest.writeDouble(width);
        dest.writeDouble(height);
        dest.writeInt(flags);
    }

    public void readFromParcel(Parcel in) {
        in.readList(points, SurveyPoint.class.getClassLoader());
        remarks = in.readString();
        type = in.readString();
        uid = in.readString();
        closed = in.readByte() == 1;
        filled = in.readByte() == 1;
        width = in.readDouble();
        height = in.readDouble();
        flags = in.readInt();
    }

    // Get segment length (from seg to next)
    public double getSegmentLength(int seg) {
        if (seg < 0)
            seg = 0;
        else if (seg >= points.size())
            seg = points.size() - 1;
        int next = (seg == points.size() - 1 ? 0 : seg + 1);
        return Conversions.calculateRange(points.get(seg), points.get(next));
    }

    // Calculate area perimeter or route length in meters
    public double getLength() {
        double len = 0.0;
        for (int i = 0; i < points.size() - 1; i++)
            len += getSegmentLength(i);
        if (filled)
            len += getSegmentLength(points.size() - 1);
        return len;
    }

    // Return center based on type (area or route)
    public SurveyPoint getCenter() {
        SurveyPoint center = new SurveyPoint();
        if (filled) {
            // Area - average point
            for (SurveyPoint sp : points) {
                center.lat += sp.lat;
                center.lon += sp.lon;
            }
            center.lat /= points.size();
            center.lon /= points.size();
        } else {
            // Route - half-way point along route
            double midLen = getLength() / 2;
            double len = 0.0;
            for (int i = 0; i < points.size() - 1; i++) {
                len += getSegmentLength(i);
                if (len >= midLen) {
                    SurveyPoint p1 = points.get(i);
                    SurveyPoint p2 = points.get(i + 1);
                    double ang = Conversions.CalculateAngledeg(
                            p2.lat, p2.lon, p1.lat, p1.lon);
                    double[] cen = Conversions.AROffset(
                            p2.lat, p2.lon, ang, len - midLen);
                    center.setSurveyPoint(cen[0], cen[1]);
                    center.course_true = ang;
                    break;
                }
            }
        }

        // Collection method is based on worst method throughout entire line
        center.collectionMethod = null;
        for (SurveyPoint sp : points) {
            if (center.collectionMethod == null
                    || sp.collectionMethod == null
                    || sp.collectionMethod.ordinal()
                    < center.collectionMethod.ordinal()) {
                center.collectionMethod = sp.collectionMethod;
                if (sp.collectionMethod == null) // Null is worst
                    break;
            }
        }
        return center;
    }

    // Calculate and return bounds [minLat, minLon, maxLat, maxLon]
    public double[] getBounds() {
        return Conversions.getBounds(points);
    }

    // Return point obstruction representing axis-aligned bounding box
    public PointObstruction getAABB() {
        if (points == null || points.isEmpty())
            return new PointObstruction();
        double minLat = 90.0, maxLat = -90, minLon = 180, maxLon = -180;
        for (SurveyPoint sp : points) {
            minLat = Math.min(minLat, sp.lat);
            minLon = Math.min(minLon, sp.lon);
            maxLat = Math.max(maxLat, sp.lat);
            maxLon = Math.max(maxLon, sp.lon);
        }
        PointObstruction ret = new PointObstruction();
        ret.lat = (minLat + maxLat) / 2;
        ret.lon = (minLon + maxLon) / 2;
        ret.alt = points.get(0).alt;
        ret.height = height;
        ret.width = Conversions.CalculateRangem(minLat, minLon, minLat, maxLon);
        ret.length = Conversions
                .CalculateRangem(minLat, minLon, maxLat, minLon);
        return ret;
    }

    public void addFlag(int flag) {
        flags |= flag;
    }

    public boolean hasFlag(int flag) {
        return flags == flag || (flags & flag) != 0;
    }

    public void removeFlag(int flag) {
        flags &= ~flag;
    }
}
