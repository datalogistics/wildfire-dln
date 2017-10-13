
package com.gmeci.atskservice.farp;

import com.gmeci.conversions.Conversions;
import com.gmeci.core.ATSKConstants;
import com.gmeci.core.SurveyData;
import com.gmeci.core.SurveyPoint;

import java.util.ArrayList;
import java.util.List;

public class FARPTankerItem {
    public String Name = "";
    public String BlockFileName = "";
    public double StartAngle = 0, EndAngle = 0;
    public double FuelPointOffset_m = 10, FuelCenterlineOffset_m;
    public double rgrWingOffset, rgrCenterlineOffset;
    public int MaxFuelPoints = 3;

    public FARPTankerItem(String Name) {
        this.Name = Name;
    }

    /**
     * Calculate fuel point based on tanker specs, center, and angle
     * @param rxType Receiver type
     * @param center Tanker center
     * @param angle Tanker angle
     * @param right True for right-side point, false for left
     * @return Fuel point
     */
    public SurveyPoint getFuelPoint(String rxType, SurveyPoint center,
            double angle, boolean right) {
        if (rxType != null && rxType.equals(ATSKConstants.FARP_RX_LAYOUT_RGR)) {
            SurveyPoint fp = Conversions.AROffset(center,
                    angle + 180, rgrWingOffset);
            return Conversions.AROffset(fp, angle + (right ? 90 : -90),
                    rgrCenterlineOffset);
        } else {
            SurveyPoint fp = Conversions.AROffset(center,
                    angle + 180, FuelPointOffset_m);
            return Conversions.AROffset(fp, angle + (right ? 90 : -90),
                    FuelCenterlineOffset_m);
        }
    }

    public SurveyPoint getFuelPoint(SurveyData survey, boolean right) {
        return getFuelPoint(survey.FAMRxShape, survey.center,
                survey.angle, right);
    }

    /**
     * Get receivers supported by this tanker
     * @return List of supported receiver types
     */
    public List<String> getReceivers() {
        List<String> types = new ArrayList<String>();
        if (MaxFuelPoints >= 1)
            types.add(ATSKConstants.FARP_RX_LAYOUT_SINGLE);
        if (MaxFuelPoints >= 2) {
            types.add(ATSKConstants.FARP_RX_LAYOUT_HLEFT);
            types.add(ATSKConstants.FARP_RX_LAYOUT_HRIGHT);
            types.add(ATSKConstants.FARP_RX_LAYOUT_SPLIT);
        }
        if (MaxFuelPoints >= 3)
            types.add(ATSKConstants.FARP_RX_LAYOUT_TRIPLE);
        if (Name.equals(ATSKConstants.AC_C130))
            types.add(ATSKConstants.FARP_RX_LAYOUT_RGR);
        return types;
    }
}
