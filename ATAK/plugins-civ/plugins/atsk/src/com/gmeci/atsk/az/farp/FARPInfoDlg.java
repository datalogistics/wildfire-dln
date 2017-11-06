
package com.gmeci.atsk.az.farp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.atakmap.android.atsk.plugin.R;
import com.atakmap.coremap.log.Log;
import com.gmeci.atsk.az.AZController;
import com.gmeci.atsk.resources.ATSKApplication;
import com.gmeci.atskservice.farp.FARPTankerItem;
import com.gmeci.core.ATSKConstants;
import com.gmeci.core.SurveyData;
import com.gmeci.atsk.az.lz.RunwayInfoDialog.RunwayInfoParentInterface;
import com.gmeci.atskservice.resolvers.AZProviderClient;
import com.gmeci.conversions.Conversions;
import com.gmeci.core.SurveyPoint;
import com.gmeci.helpers.AZHelper;

public class FARPInfoDlg extends DialogFragment {
    private static final String TAG = "HLZInfoDialog";
    private RunwayInfoParentInterface _parent;
    private TextView _leftReceiver, FARPACName, FARPNose;
    private ImageView _leftImg;
    private Button _saveBtn, _cancelBtn;
    private AZProviderClient _azpc;
    private SurveyData _survey;
    private SharedPreferences _prefs;
    private String _surveyUID, _coordFormat = Conversions.COORD_FORMAT_DM;
    private View _root;
    private FARPTankerItem _tanker;
    private final OnClickListener saveClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            Log.d(TAG, "On Save Click Listener");
            _azpc.UpdateAZ(_survey, "rwi", true);
            if (_parent != null)
                _parent.Update(true);

            FARPInfoDlg.this.dismiss();
        }
    };
    private final OnClickListener cancelClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            Log.d(TAG, "In on Cancel Click Listener");
            FARPInfoDlg.this.dismiss();
        }
    };

    public FARPInfoDlg() {
    }

    public void setupDialog(String surveyUID, AZProviderClient azpc,
            RunwayInfoParentInterface parent) {
        Log.d(TAG, "Current Survey uid: " + surveyUID);
        _surveyUID = surveyUID;
        _parent = parent;
        _azpc = azpc;
        _survey = azpc.getAZ(_surveyUID, true);
        _tanker = AZController.getInstance().getTanker(_survey.aircraft);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        Context pluginContext = ATSKApplication
                .getInstance().getPluginContext();
        _root = LayoutInflater.from(pluginContext).inflate(
                R.layout.farp_info_dlg, container, false);
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getDialog().getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        int width = pluginContext.getResources().getDisplayMetrics().widthPixels;
        int height = pluginContext.getResources().getDisplayMetrics().heightPixels;
        _root.setMinimumWidth((int) (width * 0.90f));
        _root.setMinimumHeight((int) (height * 0.80f));

        setupTextViews();
        _prefs = PreferenceManager
                .getDefaultSharedPreferences(getActivity());
        _coordFormat = _prefs.getString(
                ATSKConstants.COORD_FORMAT, Conversions.COORD_FORMAT_DM);

        UpdateMeasurements();
        setupButtons();
        return _root;
    }

    private void UpdateMeasurements() {
        FARPACName.setText(_survey.aircraft);
        SurveyPoint nose = _survey.center;
        if (_tanker != null)
            nose = Conversions.AROffset(_survey.center,
                    _survey.angle, _tanker.FuelPointOffset_m);
        FARPNose.setText(Conversions.getCoordinateString(
                nose.lat, nose.lon, _coordFormat));
        int recImg = FARPTabHost.getFARPImage(_survey.FAMRxShape);
        int side = _survey.getActiveFAMIndex();
        if (side == -1)
            return;
        SurveyPoint famPoint = _survey.FAMPoints[side];
        SurveyPoint[] rxPoints = AZHelper.getRefuelingPoints(
                side, _survey, _tanker);
        if (famPoint != null) {
            StringBuilder txt = new StringBuilder(
                    side == 0 ? "R:  " : "L:  ");
            txt.append(Conversions.getCoordinateString(
                    famPoint, _coordFormat));
            int pNum = 1;
            for (SurveyPoint rx : rxPoints) {
                if (rx == null || !rx.visible)
                    continue;
                txt.append("\nRX Point ");
                txt.append(String.valueOf(pNum++));
                txt.append(":  ");
                txt.append(Conversions.getCoordinateString(
                        rx, _coordFormat));
            }
            _leftReceiver.setText(txt.toString());
            _leftImg.setImageResource(recImg);
        }
    }

    private void setupTextViews() {
        //this should be the spinner
        FARPACName = (TextView) _root.findViewById(R.id.farp_ac_name);
        FARPNose = (TextView) _root.findViewById(R.id.farp_nose);

        LinearLayout famLayout = (LinearLayout) _root
                .findViewById(R.id.fam_layout);
        famLayout.setVisibility(_survey.getActiveFAMIndex() > -1 ?
                View.VISIBLE : View.GONE);
        TextView famTitle = (TextView) _root.findViewById(R.id.farp_fam_title);
        famTitle.setText(_root.getContext().getString(
                R.string.farp_rx_shape_title, _survey.FAMRxShape));

        _leftReceiver = (TextView) _root.findViewById(R.id.fam_positions);
        _leftImg = (ImageView) _root.findViewById(R.id.fam_image);
    }

    private void setupButtons() {
        _saveBtn = (Button) _root.findViewById(R.id.runway_info_save_button);
        _cancelBtn = (Button) _root
                .findViewById(R.id.runway_info_cancel_button);

        _saveBtn.setOnClickListener(saveClickListener);
        _cancelBtn.setOnClickListener(cancelClickListener);
    }

}
