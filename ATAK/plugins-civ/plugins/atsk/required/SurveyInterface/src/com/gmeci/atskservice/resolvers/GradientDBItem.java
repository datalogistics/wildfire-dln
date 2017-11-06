
package com.gmeci.atskservice.resolvers;

import android.database.Cursor;

import com.gmeci.core.LineObstruction;
import com.gmeci.core.SurveyPoint;

import java.util.ArrayList;
import java.util.List;

public class GradientDBItem {
    private long id;
    private String type, uid, group;
    private String Remark, AnalysisState;
    private boolean Hidden;

    private double ShoulderGradientL, ShoulderGradientR, LZGradientL,
            LZGradientR, GradedGradientL, GradedGradientR, MaintainedGradientL,
            MaintainedGradientR;
    private double LongitudinalIntervalGraident, LongitudinalOverallGradient;
    private List<SurveyPoint> LinePoints;

    public GradientDBItem() {
        Hidden = false;
    }

    public GradientDBItem(GradientDBItem item2Clone) {
        id = item2Clone.id;
        type = item2Clone.type;
        uid = item2Clone.uid;
        group = item2Clone.group;
        Remark = item2Clone.Remark;
        AnalysisState = item2Clone.AnalysisState;
        Hidden = item2Clone.Hidden;
        ShoulderGradientL = item2Clone.ShoulderGradientL;
        ShoulderGradientR = item2Clone.ShoulderGradientR;
        LZGradientL = item2Clone.LZGradientL;
        LZGradientR = item2Clone.LZGradientR;
        GradedGradientL = item2Clone.GradedGradientL;
        GradedGradientR = item2Clone.GradedGradientR;
        MaintainedGradientL = item2Clone.MaintainedGradientL;
        MaintainedGradientR = item2Clone.MaintainedGradientR;

        this.setLinePoints(item2Clone.getLinePoints());

    }

    public static SurveyPoint cursorToSurveyPoint(Cursor cursor) {
        return cursorToSurveyPoint(cursor, 0);
    }

    public static SurveyPoint cursorToSurveyPoint(Cursor cursor, double Speed) {
        SurveyPoint target = new SurveyPoint();
        target.speed = Speed;
        target.lat = (cursor.getDouble(4));
        target.lon = (cursor.getDouble(5));
        target.setHAE(cursor.getFloat(6));
        target.linearError = (cursor.getFloat(7));
        target.circularError = (cursor.getFloat(8));

        return target;
    }

    public double getLongitudinalIntervalGraident() {
        return LongitudinalIntervalGraident;
    }

    public void setLongitudinalIntervalGraident(
            double longitudinalIntervalGraident) {
        LongitudinalIntervalGraident = longitudinalIntervalGraident;
    }

    public double getLongitudinalOverallGradient() {
        return LongitudinalOverallGradient;
    }

    public void setLongitudinalOverallGradient(
            double longitudinalOverallGradient) {
        LongitudinalOverallGradient = longitudinalOverallGradient;
    }

    public boolean getHidden() {
        return Hidden;
    }

    public void setHidden(boolean Hidden) {
        this.Hidden = Hidden;
    }

    public double getShoulderGradientL() {
        return ShoulderGradientL;
    }

    public void setShoulderGradientL(double shoulderGradientL) {
        ShoulderGradientL = shoulderGradientL;
    }

    public double getShoulderGradientR() {
        return ShoulderGradientR;
    }

    public void setShoulderGradientR(double shoulderGradientR) {
        ShoulderGradientR = shoulderGradientR;
    }

    public double getLZGradientL() {
        return LZGradientL;
    }

    public void setLZGradientL(double lZGradientL) {
        LZGradientL = lZGradientL;
    }

    public double getLZGradientR() {
        return LZGradientR;
    }

    public void setLZGradientR(double lZGradientR) {
        LZGradientR = lZGradientR;
    }

    public double getGradedGradientL() {
        return GradedGradientL;
    }

    public void setGradedGradientL(double gradedGradientL) {
        GradedGradientL = gradedGradientL;
    }

    public double getGradedGradientR() {
        return GradedGradientR;
    }

    public void setGradedGradientR(double gradedGradientR) {
        GradedGradientR = gradedGradientR;
    }

    public double getMaintainedGradientL() {
        return MaintainedGradientL;
    }

    public void setMaintainedGradientL(double maintainedGradientL) {
        MaintainedGradientL = maintainedGradientL;
    }

    public double getMaintainedGradientR() {
        return MaintainedGradientR;
    }

    public void setMaintainedGradientR(double maintainedGradientR) {
        MaintainedGradientR = maintainedGradientR;
    }

    public List<SurveyPoint> getPoints() {
        if (getLinePoints() == null)
            return new ArrayList<SurveyPoint>();
        return getLinePoints();
    }

    public void setPoints(List<SurveyPoint> Points) {
        setLinePoints(Points);
    }

    public String getRemark() {
        return Remark;
    }

    public void setRemark(String remark) {
        Remark = remark;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public LineObstruction toLO() {
        return toLO(false);
    }

    public LineObstruction toLO(boolean WithPoints) {
        LineObstruction newLO = new LineObstruction();
        newLO.group = this.group;
        newLO.type = this.type;
        newLO.uid = this.uid;
        newLO.remarks = this.AnalysisState;
        if (newLO.remarks == null)
            newLO.remarks = "";
        if (WithPoints)
            newLO.points = (ArrayList<SurveyPoint>) this.getPoints();
        return newLO;
    }

    public String getAnalysisState() {
        return AnalysisState;
    }

    public void setAnalysisState(String newState) {
        AnalysisState = newState;
    }

    public List<SurveyPoint> getLinePoints() {
        return LinePoints;
    }

    public void setLinePoints(List<SurveyPoint> linePoints) {
        LinePoints = linePoints;
    }

}
