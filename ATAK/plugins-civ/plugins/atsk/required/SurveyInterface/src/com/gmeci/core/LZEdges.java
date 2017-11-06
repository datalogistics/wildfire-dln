
package com.gmeci.core;

public class LZEdges {

    public double ApproachOverrunLength_m;
    public double DepartureOverrunLength_m;
    public double ShoulderWidth_m;
    public double GradedAreaWidth_m;
    public double MaintainedAreaWidth_m;
    public double LeftMaintainedAreaGradient;
    public double RightMaintainedAreaGradient;
    public double LeftGradedAreaGradient;
    public double RightGradedAreaGradient;
    public double LeftShoulderGradient;
    public double RightShoulderGradient;
    public double LeftHalfRunwayGradient;
    public double RightHalfRunwayGradient;
    public double LongitudinalGradientOverall;
    public double LongitudinalGradientInterval;

    public double GradientThreshholdLZLonOverallMax;
    public double GradientThreshholdLZLonIntervalMax;
    public final double GradientThreshholdLZTransMaxPlus;
    public final double GradientThreshholdLZTransMaxMinus;
    public final double GradientThreshholdLZTransMinPlus;
    public final double GradientThreshholdLZTransMinMinus;

    public final double GradientThreshholdShoulderTransMaxPlus;
    public final double GradientThreshholdShoulderTransMaxMinus;
    public final double GradientThreshholdShoulderTransMinPlus;
    public final double GradientThreshholdShoulderTransMinMinus;
    public final double GradientThreshholdGradedTransMaxPlus;
    public final double GradientThreshholdGradedTransMaxMinus;
    public final double GradientThreshholdGradedTransMinPlus;
    public final double GradientThreshholdGradedTransMinMinus;
    public final double GradientThreshholdMaintainedTransMaxPlus;
    public final double GradientThreshholdMaintainedTransMaxMinus;
    public final double GradientThreshholdMaintainedTransMinPlus;
    public final double GradientThreshholdMaintainedTransMinMinus;

    public LZEdges(LZEdges sourceEdge) {
        super();
        GradientThreshholdLZLonOverallMax = sourceEdge.GradientThreshholdLZLonOverallMax;
        GradientThreshholdLZLonIntervalMax = sourceEdge.GradientThreshholdLZLonIntervalMax;

        GradientThreshholdLZTransMaxPlus = sourceEdge.GradientThreshholdLZTransMaxPlus;
        GradientThreshholdLZTransMaxMinus = sourceEdge.GradientThreshholdLZTransMaxMinus;
        GradientThreshholdLZTransMinPlus = sourceEdge.GradientThreshholdLZTransMinPlus;
        GradientThreshholdLZTransMinMinus = sourceEdge.GradientThreshholdLZTransMinMinus;

        GradientThreshholdShoulderTransMaxPlus = sourceEdge.GradientThreshholdShoulderTransMaxPlus;
        GradientThreshholdShoulderTransMaxMinus = sourceEdge.GradientThreshholdShoulderTransMaxMinus;
        GradientThreshholdShoulderTransMinPlus = sourceEdge.GradientThreshholdShoulderTransMinPlus;
        GradientThreshholdShoulderTransMinMinus = sourceEdge.GradientThreshholdShoulderTransMinMinus;

        GradientThreshholdGradedTransMaxPlus = sourceEdge.GradientThreshholdGradedTransMaxPlus;
        GradientThreshholdGradedTransMaxMinus = sourceEdge.GradientThreshholdGradedTransMaxMinus;
        GradientThreshholdGradedTransMinPlus = sourceEdge.GradientThreshholdGradedTransMinPlus;
        GradientThreshholdGradedTransMinMinus = sourceEdge.GradientThreshholdGradedTransMinMinus;

        GradientThreshholdMaintainedTransMaxPlus = sourceEdge.GradientThreshholdMaintainedTransMaxPlus;
        GradientThreshholdMaintainedTransMaxMinus = sourceEdge.GradientThreshholdMaintainedTransMaxMinus;
        GradientThreshholdMaintainedTransMinPlus = sourceEdge.GradientThreshholdMaintainedTransMinPlus;
        GradientThreshholdMaintainedTransMinMinus = sourceEdge.GradientThreshholdMaintainedTransMinMinus;
    }

    public LZEdges() {

        GradientThreshholdLZLonOverallMax = 3;
        GradientThreshholdLZLonIntervalMax = 1.5f;

        GradientThreshholdLZTransMaxPlus = 3;
        GradientThreshholdLZTransMaxMinus = -3;
        GradientThreshholdLZTransMinPlus = .5f;
        GradientThreshholdLZTransMinMinus = -.5f;

        GradientThreshholdShoulderTransMaxPlus = 5;
        GradientThreshholdShoulderTransMaxMinus = -5f;
        GradientThreshholdShoulderTransMinPlus = 1.5f;
        GradientThreshholdShoulderTransMinMinus = -1.5f;

        GradientThreshholdGradedTransMaxPlus = 5;
        GradientThreshholdGradedTransMaxMinus = -5;
        GradientThreshholdGradedTransMinPlus = 2;
        GradientThreshholdGradedTransMinMinus = -2;

        GradientThreshholdMaintainedTransMaxPlus = 10;
        GradientThreshholdMaintainedTransMaxMinus = -20;
        GradientThreshholdMaintainedTransMinPlus = 0;
        GradientThreshholdMaintainedTransMinMinus = 0;

        LongitudinalGradientOverall = LongitudinalGradientInterval = 0;

        ApproachOverrunLength_m = 0;
        DepartureOverrunLength_m = 0;
        ShoulderWidth_m = 3;
        GradedAreaWidth_m = 5;
        MaintainedAreaWidth_m = 10;

        LeftMaintainedAreaGradient = RightMaintainedAreaGradient = 0;
        LeftGradedAreaGradient = RightGradedAreaGradient = 0;
        LeftShoulderGradient = RightShoulderGradient = 0;
        LeftHalfRunwayGradient = RightHalfRunwayGradient = 0;

    }

    public boolean ValidTransverseGradients() {
        //too steep check
        if (GradientThreshholdLZTransMaxPlus < LeftHalfRunwayGradient
                || GradientThreshholdLZTransMaxMinus > LeftHalfRunwayGradient ||
                GradientThreshholdLZTransMaxPlus < RightHalfRunwayGradient
                || GradientThreshholdLZTransMaxMinus > RightHalfRunwayGradient) {//too steep up or down on LZ
            return false;
        }

        if (GradientThreshholdShoulderTransMaxPlus < LeftShoulderGradient
                || GradientThreshholdShoulderTransMaxMinus > LeftShoulderGradient
                ||
                GradientThreshholdShoulderTransMaxPlus < RightShoulderGradient
                || GradientThreshholdShoulderTransMaxMinus > RightShoulderGradient) {//too steep up or down on Shoulder
            return false;
        }

        if (GradientThreshholdGradedTransMaxPlus < LeftGradedAreaGradient
                || GradientThreshholdGradedTransMaxMinus > LeftGradedAreaGradient
                ||
                GradientThreshholdGradedTransMaxPlus < RightGradedAreaGradient
                || GradientThreshholdGradedTransMaxMinus > RightGradedAreaGradient) {//too steep up or down on Graded
            return false;
        }

        if (GradientThreshholdMaintainedTransMaxPlus < LeftMaintainedAreaGradient
                || GradientThreshholdShoulderTransMaxMinus > LeftMaintainedAreaGradient
                ||
                GradientThreshholdMaintainedTransMaxPlus < RightMaintainedAreaGradient
                || GradientThreshholdShoulderTransMaxMinus > RightMaintainedAreaGradient) {//too steep up or down on Maintained
            return false;
        }
        //too shallow check////////////////////
        if (GradientThreshholdLZTransMinPlus < LeftHalfRunwayGradient
                && GradientThreshholdLZTransMinMinus > LeftHalfRunwayGradient ||
                GradientThreshholdLZTransMinPlus < RightHalfRunwayGradient
                && GradientThreshholdLZTransMinMinus > RightHalfRunwayGradient) {//too shallow up or down on LZ
            return false;
        }
        if (GradientThreshholdShoulderTransMinPlus < LeftShoulderGradient
                && GradientThreshholdShoulderTransMinMinus > LeftShoulderGradient
                ||
                GradientThreshholdShoulderTransMinPlus < RightShoulderGradient
                && GradientThreshholdShoulderTransMinMinus > RightShoulderGradient) {//too shallow up or down on Shoulder
            return false;
        }
        if (GradientThreshholdGradedTransMinMinus < LeftGradedAreaGradient
                && GradientThreshholdGradedTransMinMinus > LeftGradedAreaGradient
                ||
                GradientThreshholdGradedTransMinMinus < RightGradedAreaGradient
                && GradientThreshholdGradedTransMinMinus > RightGradedAreaGradient) {//too shallow up or down on Graded
            return false;
        }
        return !(GradientThreshholdMaintainedTransMinPlus < LeftMaintainedAreaGradient
                && GradientThreshholdMaintainedTransMinMinus > LeftMaintainedAreaGradient
                || GradientThreshholdMaintainedTransMinPlus < RightMaintainedAreaGradient
                && GradientThreshholdMaintainedTransMinMinus > RightMaintainedAreaGradient);

    }

}
