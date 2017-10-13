
package com.gmeci.core;

public class Criteria implements Cloneable {

    public static final double MAX_INCREMENTAL_LONGITUDINAL = 10;
    //constants for the EC list display
    public static final String RunwayLength_str = "Runway Length";
    public static final String OverallLongitudinal_str = "Overall Longitudinal";
    public static final String IncrementalLongitudinal_str = "Incremental Longitidunal";
    public static final String RunwayWidth_str = "Runway Width";
    public static final String RunwayTransversePosMAX_str = "Runway Transverse +MAX";
    public static final String RunwayTransversePosMIN_str = "Runway Transverse +MIN";
    public static final String RunwayTransverseNegMIN_str = "Runway Transverse -MIN";
    public static final String RunwayTransverseNegMAX_str = "Runway Transverse -MAX";
    public static final String RunwayShoulder_str = "Runway Shoulder";
    public static final String RunwayShoulderObstacleHt_str = "Runway Shoulder Obstacle Height";
    public static final String ShoulderTransversePosMAX_str = "Shoulder Transverse +MAX";
    public static final String ShoulderTransversePosMIN_str = "Shoulder Transverse +MIN";
    public static final String ShoulderTransverseNegMIN_str = "Shoulder Transverse -MIN";
    public static final String ShoulderTransverseNegMAX_str = "Shoulder Transverse -MAX";
    public static final String GradedArea_str = "Graded Area";
    public static final String GradedAreaObstacleHt_str = "Graded Area Obstable Height";
    public static final String GradedTransversePosMAX_str = "Runway Transverse +MAX";
    public static final String GradedTransversePosMIN_str = "Runway Transverse +MIN";
    public static final String GradedTransverseNegMIN_str = "Runway Transverse -MIN";
    public static final String GradedTransverseNegMAX_str = "Runway Transverse -MAX";
    public static final String MaintainedArea_str = "Maintained Area";
    public static final String MaintainedObstacleHt_str = "Maintained Area Obstacle Height";
    public static final String MaintainedTransversePosMAX_str = "Maintained Transverse +MAX";
    public static final String MaintainedTransversePosMIN_str = "Maintained Transverse +MIN";
    public static final String MaintainedTransverseNegMIN_str = "Maintained Transverse -MIN";
    public static final String MaintainedTransverseNegMAX_str = "Maintained Transverse -MAX";
    public static final String ApproachOverrunLength_str = "Approach Overrun Length";
    public static final String DepartureOverrunLength_str = "Departure Overrun Length";
    public static final String OverrunWidth_str = "Overrun Width";
    public static final String OverrunShoulder_str = "Overrun Shoulder";
    public static final String EndClearZoneLength_str = "End Clear Zone Length";
    public static final String EndClearZoneInnerWidth_str = "End Clear Zone Inner Width";
    public static final String EndClearZoneOuterWidth_str = "End Clear Zone Outer Width";
    public static final String InnerApproachZoneLength_str = "Inner Approach Zone Length";
    public static final String InnerApproachZoneWidth_str = "Inner Approach Zone Width";
    public static final String OuterApproachZoneLength_str = "Outer Approach Zone Length";
    public static final String OuterApproachZoneWidth_str = "Outer Approach Zone Width";
    public static final String ApproachGlideSlope_str = "Approach Glide Slope";
    public static final String DepartureGlideSlope_str = "DepartureGlideSlope";
    public static final String DCPOffset_str = "DCP Offset";
    public static final String DCPSpacingPercentage_str = "DCP Spacing Percentage";
    public static final String DCP_1500_LESS_str = "1500 or less";
    public static final String DCP_1501_3000_str = "1501 to 3000";
    public static final String DCP_3000_MORE_str = "3000 or more";
    public String Name;
    public double RunwayLength_m;
    public double OverallLongitudinal;
    public double IncrementalLongitudinal = MAX_INCREMENTAL_LONGITUDINAL;
    public double RunwayWidth_m;
    public double RunwayTransversePosMAX;
    public double RunwayTransversePosMIN;
    public double RunwayTransverseNegMIN;
    public double RunwayTransverseNegMAX;
    public double RunwayShoulderWidth_m;
    public double RunwayShoulderObstacleMaxHeight_m;
    public double ShoulderTransversePosMAX;
    public double ShoulderTransversePosMIN;
    public double ShoulderTransverseNegMIN;
    public double ShoulderTransverseNegMAX;
    public double GradedAreaWidth_m;
    public double GradedAreaObstacleMaxHeight;
    public double GradedTransversePosMAX;
    public double GradedTransversePosMIN;
    public double GradedTransverseNegMIN;
    public double GradedTransverseNegMAX;
    public double MaintainedAreaWidth_m;
    public double MaintainedObstacleMaxHeight;
    public double MaintainedTransversePosMAX;
    public double MaintainedTransversePosMIN;
    public double MaintainedTransverseNegMIN;
    public double MaintainedTransverseNegMAX;
    public double ApproachOverrunLength_m;
    public double DepartureOverrunLength_m;
    public double OverrunWidth_m;
    public double OverrunShoulder_m;
    public double EndClearZoneLength_m;
    public double EndClearZoneInnerWidth_m;
    public double EndClearZoneOuterWidth_m;
    public double InnerApproachZoneLength_m;
    public double InnerApproachZoneWidth_m;
    public double OuterApproachZoneLength_m;
    public double OuterApproachZoneWidth_m;
    public double ApproachGlideSlope_deg;
    public double DepartureGlideSlope_deg;
    public double DCPOffset;
    public double DCPSpacingPercentage;
    public double DCP_1500_LESS;
    public double DCP_1501_3000;
    public double DCP_3000_MORE;
    private double ParkingBoundaryWidth_m;
    private double ParkingBoundaryLength_m;
    private double TaxiwayWidth_m;

    public Criteria() {
    }

    public Criteria(Criteria aircraft) {
        this.Name = aircraft.Name;
        this.RunwayLength_m = aircraft.RunwayLength_m;
        this.OverallLongitudinal = aircraft.OverallLongitudinal;
        this.IncrementalLongitudinal = aircraft.IncrementalLongitudinal;
        this.RunwayWidth_m = aircraft.RunwayWidth_m;
        this.RunwayTransversePosMAX = aircraft.RunwayTransversePosMAX;
        this.RunwayTransversePosMIN = aircraft.RunwayTransversePosMIN;
        this.RunwayTransverseNegMIN = aircraft.RunwayTransverseNegMIN;
        this.RunwayTransverseNegMAX = aircraft.RunwayTransverseNegMAX;
        this.RunwayShoulderWidth_m = aircraft.RunwayShoulderWidth_m;
        this.RunwayShoulderObstacleMaxHeight_m = aircraft.RunwayShoulderObstacleMaxHeight_m;
        this.ShoulderTransversePosMAX = aircraft.ShoulderTransversePosMAX;
        this.ShoulderTransversePosMIN = aircraft.ShoulderTransversePosMIN;
        this.ShoulderTransverseNegMIN = aircraft.ShoulderTransverseNegMIN;
        this.ShoulderTransverseNegMAX = aircraft.ShoulderTransverseNegMAX;
        this.GradedAreaWidth_m = aircraft.GradedAreaWidth_m;
        this.GradedAreaObstacleMaxHeight = aircraft.GradedAreaObstacleMaxHeight;
        this.GradedTransversePosMAX = aircraft.GradedTransversePosMAX;
        this.GradedTransversePosMIN = aircraft.GradedTransversePosMIN;
        this.GradedTransverseNegMIN = aircraft.GradedTransverseNegMIN;
        this.GradedTransverseNegMAX = aircraft.GradedTransverseNegMAX;
        this.MaintainedAreaWidth_m = aircraft.MaintainedAreaWidth_m;
        this.MaintainedObstacleMaxHeight = aircraft.MaintainedObstacleMaxHeight;
        this.MaintainedTransversePosMAX = aircraft.MaintainedTransversePosMAX;
        this.MaintainedTransversePosMIN = aircraft.MaintainedTransversePosMIN;
        this.MaintainedTransverseNegMIN = aircraft.MaintainedTransverseNegMIN;
        this.MaintainedTransverseNegMAX = aircraft.MaintainedTransverseNegMAX;
        this.ApproachOverrunLength_m = aircraft.ApproachOverrunLength_m;
        this.DepartureOverrunLength_m = aircraft.DepartureOverrunLength_m;
        this.OverrunWidth_m = aircraft.OverrunWidth_m;
        this.OverrunShoulder_m = aircraft.OverrunShoulder_m;
        this.EndClearZoneLength_m = aircraft.EndClearZoneLength_m;
        this.EndClearZoneInnerWidth_m = aircraft.EndClearZoneInnerWidth_m;
        this.EndClearZoneOuterWidth_m = aircraft.EndClearZoneOuterWidth_m;
        this.InnerApproachZoneLength_m = aircraft.InnerApproachZoneLength_m;
        this.InnerApproachZoneWidth_m = aircraft.InnerApproachZoneWidth_m;
        this.OuterApproachZoneLength_m = aircraft.OuterApproachZoneLength_m;
        this.OuterApproachZoneWidth_m = aircraft.OuterApproachZoneWidth_m;
        this.ApproachGlideSlope_deg = aircraft.ApproachGlideSlope_deg;
        this.DepartureGlideSlope_deg = aircraft.DepartureGlideSlope_deg;
        this.DCPOffset = aircraft.DCPOffset;
        this.DCPSpacingPercentage = aircraft.DCPSpacingPercentage;
        this.DCP_1500_LESS = aircraft.DCP_1500_LESS;
        this.DCP_1501_3000 = aircraft.DCP_1501_3000;
        this.DCP_3000_MORE = aircraft.DCP_3000_MORE;

    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }

    public double getApproachOverrunLength() {
        return ApproachOverrunLength_m;
    }

    public void setApproachOverrunLength(double approachOverrunLength) {
        ApproachOverrunLength_m = approachOverrunLength;
    }

    public double getDepartureOverrunLength() {
        return DepartureOverrunLength_m;
    }

    public void setDepartureOverrunLength(double departureOverrunLength) {
        DepartureOverrunLength_m = departureOverrunLength;
    }

    public double getEndClearZoneInnerWidth() {
        return EndClearZoneInnerWidth_m;
    }

    public void setEndClearZoneInnerWidth(double endClearZoneInnerWidth) {
        EndClearZoneInnerWidth_m = endClearZoneInnerWidth;
    }

    public double getEndClearZoneOuterWidth() {
        return EndClearZoneOuterWidth_m;
    }

    public void setEndClearZoneOuterWidth(double endClearZoneOuterWidth) {
        EndClearZoneOuterWidth_m = endClearZoneOuterWidth;
    }

    public double getInnerApproachZoneLength() {
        return InnerApproachZoneLength_m;
    }

    public void setInnerApproachZoneLength(double innerApproachZoneLength) {
        InnerApproachZoneLength_m = innerApproachZoneLength;
    }

    public double getInnerApproachZoneWidth() {
        return InnerApproachZoneWidth_m;
    }

    public void setInnerApproachZoneWidth(double innerApproachZoneWidth) {
        InnerApproachZoneWidth_m = innerApproachZoneWidth;
    }

    public double getOuterApproachZoneLength() {
        return OuterApproachZoneLength_m;
    }

    public void setOuterApproachZoneLength(double outerApproachZoneLength) {
        OuterApproachZoneLength_m = outerApproachZoneLength;
    }

    public double getOuterApproachZoneWidth() {
        return OuterApproachZoneWidth_m;
    }

    public void setOuterApproachZoneWidth(double outerApproachZoneWidth) {
        OuterApproachZoneWidth_m = outerApproachZoneWidth;
    }

    public double getRunwayLength() {
        return RunwayLength_m;
    }

    public void setRunwayLength(double runwayLength) {
        RunwayLength_m = runwayLength;
    }

    public double getRunwayWidth() {
        return RunwayWidth_m;
    }

    public void setRunwayWidth(double runwayWidth) {
        RunwayWidth_m = runwayWidth;
    }

    public double getRunwayShoulder() {
        return RunwayShoulderWidth_m;
    }

    public void setRunwayShoulder(double runwayShoulder) {
        RunwayShoulderWidth_m = runwayShoulder;
    }

    public double getGradedArea() {
        return GradedAreaWidth_m;
    }

    public void setGradedArea(double gradedArea) {
        GradedAreaWidth_m = gradedArea;
    }

    public double getMaintainedArea() {
        return MaintainedAreaWidth_m;
    }

    public void setMaintainedArea(double maintainedArea) {
        MaintainedAreaWidth_m = maintainedArea;
    }

    public double getOverrunWidth() {
        return OverrunWidth_m;
    }

    public void setOverrunWidth(double overrunWidth) {
        OverrunWidth_m = overrunWidth;
    }

    public double getOverrunShoulder() {
        return OverrunShoulder_m;
    }

    public void setOverrunShoulder(double overrunShoulder) {
        OverrunShoulder_m = overrunShoulder;
    }

    public double getEndClearZoneLength() {
        return EndClearZoneLength_m;
    }

    public void setEndClearZoneLength(double endClearZoneLength) {
        EndClearZoneLength_m = endClearZoneLength;
    }

    public double getParkingBoundaryWidth() {
        return ParkingBoundaryWidth_m;
    }

    public void setParkingBoundaryWidth(double parkingBoundary_m) {
        ParkingBoundaryWidth_m = parkingBoundary_m;
    }

    public double getParkingBoundaryLength() {
        return ParkingBoundaryLength_m;
    }

    public void setParkingBoundaryLength(double parkingBoundary_m) {
        ParkingBoundaryLength_m = parkingBoundary_m;
    }

    public double getTaxiwayWidth() {
        return TaxiwayWidth_m;
    }

    public void setTaxiwayWidth(double taxiwayWidth_m) {
        TaxiwayWidth_m = taxiwayWidth_m;
    }

    public double getApproachGlideSlope() {
        return ApproachGlideSlope_deg;
    }

    public void setApproachGlideSlope(double approachGlideSlope) {
        ApproachGlideSlope_deg = approachGlideSlope;
    }

    public double getDepartureGlideSlope() {
        return DepartureGlideSlope_deg;
    }

    public void setDepartureGlideSlope(double departureGlideSlope) {
        DepartureGlideSlope_deg = departureGlideSlope;
    }

}
