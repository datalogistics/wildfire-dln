
package com.gmeci.core;

import android.os.Parcel;
import android.os.Parcelable;

import com.gmeci.conversions.Conversions;

import java.io.Serializable;

public class PointObstruction extends SurveyPoint implements Parcelable,
        Serializable {

    public static final Parcelable.Creator<PointObstruction> CREATOR = new Parcelable.Creator<PointObstruction>() {

        @Override
        public PointObstruction createFromParcel(Parcel source) {
            return new PointObstruction(source);
        }

        @Override
        public PointObstruction[] newArray(int size) {
            return new PointObstruction[size];
        }
    };
    /**
     *
     */
    public String remark;
    public String type;
    public double height;
    public double width;
    public double length;
    public String uid;
    public String group;
    public boolean TopCollected;
    public int flags;

    public PointObstruction() {
        super();
        remark = "";
        type = "Generic Point";
        height = 10;
        width = 10;
        length = 10;
        group = ATSKConstants.DEFAULT_GROUP;
        uid = "non-unique";
        flags = 0;
    }

    public PointObstruction(PointObstruction source) {
        super(source);
        this.remark = source.remark;
        this.type = source.type;
        this.height = source.height;
        this.width = source.width;
        this.length = source.length;
        this.uid = source.uid;
        this.group = source.group;
        this.TopCollected = source.TopCollected;
        this.flags = source.flags;
        this.collectionMethod = source.collectionMethod;
    }

    public PointObstruction(SurveyPoint sp) {
        super(sp);
    }

    public PointObstruction(String type) {
        this.type = type;
        SetDefaultMeasurments();
        uid = "non-unique";
    }

    public PointObstruction(String type, SurveyPoint sp) {
        this(sp);
        this.type = type;
        SetDefaultMeasurments();
        uid = "non-unique";
    }

    public PointObstruction(Parcel in) {
        readFromParcel(in);
    }

    public String toString() {
        return String.format("%s %s-%s", type, super.toString(), uid);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(remark);
        dest.writeString(type);
        dest.writeDouble(height);
        dest.writeDouble(width);
        dest.writeDouble(length);
        dest.writeString(uid);
    }

    public void readFromParcel(Parcel in) {
        super.readFromParcel(in);
        remark = in.readString();
        type = in.readString();
        height = in.readDouble();
        width = in.readDouble();
        length = in.readDouble();
        uid = in.readString();
    }

    public void SetDefaultMeasurments() {

        height = 10;
        width = 10f;
        length = 10f;
    }

    public SurveyPoint[] getCorners(double width, double length, boolean closed) {
        double hWidth = width / 2;
        double hLength = length / 2;

        double right[] = Conversions.AROffset(
                lat, lon, course_true + 90, hWidth);
        double backRight[] = Conversions.AROffset(
                right[0], right[1], course_true + 180, hLength);
        double frontRight[] = Conversions.AROffset(
                right[0], right[1], course_true, hLength);

        double left[] = Conversions.AROffset(
                lat, lon, course_true - 90, hWidth);
        double backLeft[] = Conversions.AROffset(
                left[0], left[1], course_true + 180, hLength);
        double frontLeft[] = Conversions.AROffset(
                left[0], left[1], course_true, hLength);

        if (closed) {
            return new SurveyPoint[] {
                    new SurveyPoint(backRight[0], backRight[1]),
                    new SurveyPoint(frontRight[0], frontRight[1]),
                    new SurveyPoint(frontLeft[0], frontLeft[1]),
                    new SurveyPoint(backLeft[0], backLeft[1]),
                    new SurveyPoint(backRight[0], backRight[1])
            };
        }
        return new SurveyPoint[] {
                new SurveyPoint(backRight[0], backRight[1]),
                new SurveyPoint(frontRight[0], frontRight[1]),
                new SurveyPoint(frontLeft[0], frontLeft[1]),
                new SurveyPoint(backLeft[0], backLeft[1])
        };
    }

    public SurveyPoint[] getCorners(boolean closed) {
        return getCorners(width, length, closed);
    }

    public SurveyPoint[] getCorners() {
        return getCorners(width, length, false);
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
