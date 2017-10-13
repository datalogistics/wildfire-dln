
package com.gmeci.atsk.az.dz;

import com.gmeci.atsk.az.AZBaseMeasurementFragment;
import com.gmeci.atsk.resources.ATSKApplication;
import com.gmeci.helpers.AZHelper;

public class DZMeasurementFragment extends AZBaseMeasurementFragment {

    public DZMeasurementFragment() {
        //AZTypeToDisplay = "DZ";
        AZTypeToDisplay = LZ_TWO_POINT;
    }

    @Override
    protected void UpdateSurvey(boolean AllowDBUpdate) {
        recalcPIs();
        super.UpdateSurvey(AllowDBUpdate);
    }

    private static final String[] PI_NAMES = {
            "cds", "per", "he"
    };

    private void recalcPIs() {
        // Recalculate PI elevations since positions may have changed
        if (this.surveyData != null) {
            for (String name : PI_NAMES) {
                double elev = ATSKApplication.getAltitudeHAE(AZHelper
                        .CalculatePointOfImpact(this.surveyData, name));
                if (name.equals("cds"))
                    this.surveyData.cdsPIElevation = elev;
                else if (name.equals("per"))
                    this.surveyData.perPIElevation = elev;
                else if (name.equals("he"))
                    this.surveyData.hePIElevation = elev;
            }
        }
    }
}
