
package com.gmeci.atsk.obstructions.obstruction;

import android.os.Bundle;
import android.view.View;

import com.atakmap.android.atsk.plugin.R;
import com.gmeci.atsk.obstructions.ObstructionType;
import com.gmeci.atsk.resources.ATSKApplication;
import com.gmeci.atsk.resources.LCRButton;
import com.gmeci.conversions.Conversions;
import com.gmeci.core.LineObstruction;
import com.gmeci.core.SurveyPoint;
import com.gmeci.helpers.ObstructionHelper;

public class EditObstructionRouteFragment extends EditObstructionFragment {

    private static final String TAG = "EditObstructionRouteFragment";

    private LCRButton lcrButton;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        lcrButton = (LCRButton) view.findViewById(R.id.lcr_selector);
        if (lcrButton != null) {
            lcrButton.setVisibility(View.VISIBLE);
            lcrButton.setSelectionSide(LCRButton.CollectionSide.CENTER);
        }
    }

    @Override
    protected void UpdateSpinnerAdapter() {
        _typeSpinner.setup(ObstructionType.ROUTES);
    }

    @Override
    protected String getSettingModifier() {
        return "Route";
    }

    @Override
    protected void HideShowFields(String type) {
        super.HideShowFields(type);
        setVisibility(WIDTH_POSITION, View.VISIBLE);
    }

    @Override
    protected boolean StoreMeasurement(double newMeasurement_m) {
        switch (CurrentlyEditedIndex) {
            case WIDTH_POSITION:
                if (lcrButton != null)
                    return setRouteWidth(newMeasurement_m,
                            lcrButton.getCollectionSide());
        }
        return super.StoreMeasurement(newMeasurement_m);
    }

    @Override
    protected boolean setRouteWidth(double width, LCRButton.CollectionSide side) {
        if (parentFragment == null)
            return false;
        CurrentObstruction.width = width;
        LineObstruction lo = getCurrentLine();
        if (lo == null)
            return false;
        boolean changePoints = side != LCRButton.CollectionSide.CENTER;
        if (ObstructionHelper.setRouteWidth(lo, width, side.ordinal())) {
            if (changePoints) {
                for (SurveyPoint sp : lo.points)
                    sp.setHAE(ATSKApplication.getAltitudeHAE(sp));
            }
            parentFragment.opc.UpdateLine(lo, changePoints);
        }
        return true;
    }

    @Override
    public double getLRCOffset() {
        LCRButton.CollectionSide cs = lcrButton.getCollectionSide();
        if (cs == LCRButton.CollectionSide.LEFT)
            return (CurrentObstruction.width / 2f) * -1;
        if (cs == LCRButton.CollectionSide.RIGHT)
            return (CurrentObstruction.width / 2f);
        return 0;
    }

    @Override
    protected void setLocation(SurveyPoint sp, boolean top) {
        LineObstruction lo = getCurrentLine();
        if (lo != null && pointIndex >= 0 && pointIndex < lo.points.size()
                && lcrButton.getCollectionSide()
                    != LCRButton.CollectionSide.CENTER) {
            // Set location for edge of line
            SurveyPoint p = pointIndex > 0 ? lo.points.get(pointIndex - 1)
                    : null;
            SurveyPoint n = pointIndex < lo.points.size() - 1 ?
                    lo.points.get(pointIndex + 1) : null;
            double ang = Conversions.computeAngle(p, sp, n);
            double[] offsetCoord = Conversions.AROffset(sp.lat, sp.lon,
                    ang - 90.0f, getLRCOffset());
            sp.lat = offsetCoord[0];
            sp.lon = offsetCoord[1];
        }
        super.setLocation(sp, top);
    }
}
