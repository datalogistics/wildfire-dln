
package com.gmeci.atsk.az.farp.FARPSupport;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import com.atakmap.android.atsk.plugin.R;
import com.gmeci.atsk.resources.ATSKApplication;
import com.gmeci.core.ATSKConstants;
import com.gmeci.core.SurveyData;
import com.gmeci.core.SurveyPoint;
import com.gmeci.atsk.az.farp.FARPReceiverFragment;
import com.gmeci.atskservice.resolvers.AZProviderClient;

public class FARPFAMLeftRightDeleteDialog extends AlertDialog {
    private static final String TAG = "FARPFAMLeftRightDeleteDialog";
    private final Context _context;
    private String _surveyUID;
    private AZProviderClient _azpc;
    private FARPReceiverFragment _parent;

    protected FARPFAMLeftRightDeleteDialog(Context context, int theme) {
        super(context, theme);
        _context = context;
    }

    public FARPFAMLeftRightDeleteDialog(Context context) {
        super(context, android.R.style.Theme_Holo_Dialog);
        _context = context;
    }

    private void setupView() {
        Context pluginContext = ATSKApplication
                .getInstance().getPluginContext();
        LayoutInflater inflater = LayoutInflater.from(pluginContext);
        View dialogLayout = inflater.inflate(
                R.layout.farp_fam_side_delete_dialog, null);

        final Button deleteLeft = (Button) dialogLayout
                .findViewById(R.id.delete_left);
        deleteLeft.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                SurveyData CurrentSurvey = _azpc.getAZ(_surveyUID, false);
                if (CurrentSurvey == null)
                    return;

                if (CurrentSurvey.FAMPoints[SurveyData.getFARPSideIndex(false)] == null)
                    CurrentSurvey.FAMPoints[SurveyData.getFARPSideIndex(false)] = new SurveyPoint();
                CurrentSurvey.FAMPoints[SurveyData.getFARPSideIndex(false)].visible = false;
                CurrentSurvey.FAMRxAngle[0] = -400;
                CurrentSurvey.FAMRxAngle[1] = -400;
                _azpc.UpdateAZ(CurrentSurvey, "FAM", true);
                _parent.UpdateDisplayMeasurements(CurrentSurvey);
                deleteLeft.setVisibility(View.GONE);
            }
        });
        deleteLeft.setTextColor(ATSKConstants.LIGHT_BLUE);

        final Button delRight = (Button) dialogLayout
                .findViewById(R.id.delete_right);
        delRight.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                SurveyData CurrentSurvey = _azpc.getAZ(_surveyUID, false);
                if (CurrentSurvey == null)
                    return;

                CurrentSurvey.FAMPoints[SurveyData.getFARPSideIndex(true)].visible = false;

                CurrentSurvey.FAMRxAngle[0] = -400;
                CurrentSurvey.FAMRxAngle[1] = -400;

                _azpc.UpdateAZ(CurrentSurvey, "FAM", true);
                delRight.setVisibility(View.GONE);
                _parent.UpdateDisplayMeasurements(CurrentSurvey);
            }

        });
        delRight.setTextColor(ATSKConstants.LIGHT_BLUE);

        setTitle("Delete FAM Points");
        setView(dialogLayout);
    }

    public void Initialize(AZProviderClient azpc, FARPReceiverFragment parent) {
        _surveyUID = azpc.getSetting(ATSKConstants.CURRENT_SURVEY, TAG);
        _parent = parent;
        _azpc = azpc;
        setupView();
        show();
    }

}
