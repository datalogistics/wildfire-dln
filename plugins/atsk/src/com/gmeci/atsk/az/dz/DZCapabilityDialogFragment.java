
package com.gmeci.atsk.az.dz;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.TextView;

import com.atakmap.android.atsk.plugin.R;
import com.gmeci.atsk.resources.ATSKApplication;
import com.gmeci.core.ATSKConstants;
import com.gmeci.core.SurveyData;
import com.gmeci.atsk.az.spinners.ATSKSpinner;
import com.gmeci.atskservice.resolvers.AZProviderClient;
import com.gmeci.atskservice.dz.DZCapabilities;
import com.gmeci.atskservice.dz.DZRequirementsParser;
import com.gmeci.conversions.Conversions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DZCapabilityDialogFragment extends DialogFragment {

    private static final int FUDGE = 2; // value in meters to fudge the value
    protected static final int RADIUS_POSITION = 0;
    protected static final int LENGTH_POSITION = 1;
    protected static final int WIDTH_POSITION = 2;
    private static final String TAG = "DZ Capability Dialog";
    private AZProviderClient _azpc;
    private double _altitude = 1000;
    private ATSKSpinner _altSpinner;
    private String _acName = "";
    private List<String> _acList;
    private int _acPos = 0;
    private TextView _forText, _dayText, _nightText;
    private TextView _perText, _perDayText, _perNightText;
    private TextView _cdsText, _cdsDayText, _cdsNightText;
    private TextView _heText, _heDayText, _heNightText;
    private TextView _mffText, _mffDayText, _mffNightText;
    private TextView _satbText, _satbDayText, _satbNightText;
    private TextView _crrcText, _crrcDayText, _crrcNightText;
    private TextView _hslladsText, _hslladsDayText, _hslladsNightText;
    private TextView _hvcdsText, _hvcdsDayText, _hvcdsNightText;
    private TextView _jpadsText, _jpadsDayText, _jpadsNightText;
    private final TextView[] DialogLabelTV = new TextView[3];
    private final TextView[] DialogValueTV = new TextView[3];
    private final TextView[] DialogUnitsTV = new TextView[3];
    private TextView _altText;
    private String _surveyName;
    private SurveyData _surveyData;
    private DZCapabilities _dzCaps;
    private View _root;
    private DZRequirementsParser _dzParser;

    public DZCapabilityDialogFragment() {
    }

    public void Initialize(DZRequirementsParser dzparser) {
        _dzParser = dzparser;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        Context pluginContext = ATSKApplication
                .getInstance().getPluginContext();
        _root = LayoutInflater.from(pluginContext).inflate(
                R.layout.dz_crit_impact_capability_dialog,
                container, false);//make sure attachToRoot is false
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getDialog().getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        int width = pluginContext.getResources().getDisplayMetrics().widthPixels;
        int height = pluginContext.getResources().getDisplayMetrics().heightPixels;
        _root.setMinimumWidth((int) (width * 0.80f));
        _root.setMinimumHeight((int) (height * 0.60f));

        return _root;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SetupACList();
        DetermineCapability();

        SetupDialogTextViews();
        SetupSpinners();
        SetValues();

    }

    public void setupDialog(String acname, AZProviderClient azpc2) {
        _azpc = azpc2;

        loadCurrentSurvey();

        _acName = acname;
        _surveyName = _surveyData.getSurveyName();

    }

    public boolean loadCurrentSurvey() {
        _surveyData = new SurveyData();
        String currentUID = _azpc.getSetting(ATSKConstants.CURRENT_SURVEY, TAG);

        _surveyData = _azpc.getAZ(currentUID, true);

        return _surveyData != null;
    }

    private void SetupACList() {
        _acList = _dzParser.getAircraft();
        if (_acList.contains(_acName)) {
            _acPos = _acList.indexOf(_acName);
        }
    }

    private void DetermineCapability() {
        //this function will read the parser and determine the capabilities. 

        String uid = _azpc.getSetting(ATSKConstants.CURRENT_SURVEY, TAG);
        _surveyName = _azpc.getAZName(uid);
        _surveyData = _azpc.getAZ(uid, true);

        if (_surveyData == null)
            return;

        if (_surveyData.circularAZ)
            _dzCaps = _dzParser.getDZCapabilities((_surveyData.getRadius() * 2)
                    + FUDGE,
                    (_surveyData.getRadius() * 2) + FUDGE, _altitude, _acName);
        else
            _dzCaps = _dzParser.getDZCapabilities(_surveyData.getLength()
                    + FUDGE,
                    _surveyData.width + FUDGE, _altitude, _acName);
    }

    private void SetupSpinners() {

        Context pluginContext = ATSKApplication
                .getInstance().getPluginContext();
        _altSpinner = (ATSKSpinner) _root.findViewById(
                R.id.dz_crit_capability_spin_altitiude);
        ArrayList<String> altlist = new ArrayList<String>(
                Arrays.asList(pluginContext.getResources().getStringArray(
                        R.array.atsk_altitude_array)));
        _altSpinner.SetupSpinner(altlist);

        int pos = 0;

        if (altlist.contains(String.valueOf((int) _altitude)))
            pos = altlist.indexOf(String.valueOf((int) _altitude));

        _altSpinner.setSelection(pos);

        _altSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adaptparent, View view,
                    int pos, long id) {
                _altitude = Float.valueOf((String) adaptparent
                        .getItemAtPosition(pos));
                DetermineCapability();
                SetValues();
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }

        });
    }

    private void SetupDialogTextViews() {
        DialogValueTV[RADIUS_POSITION] = (TextView) _root
                .findViewById(R.id.dz_capability_dialog_text_radius_value);
        DialogLabelTV[RADIUS_POSITION] = (TextView) _root
                .findViewById(R.id.dz_capability_dialog_text_radius_label);
        DialogUnitsTV[RADIUS_POSITION] = (TextView) _root
                .findViewById(R.id.dz_capability_dialog_text_radius_units);

        DialogValueTV[LENGTH_POSITION] = (TextView) _root
                .findViewById(R.id.dz_capability_dialog_text_length_value);
        DialogLabelTV[LENGTH_POSITION] = (TextView) _root
                .findViewById(R.id.dz_capability_dialog_text_length_label);
        DialogUnitsTV[LENGTH_POSITION] = (TextView) _root
                .findViewById(R.id.dz_capability_dialog_text_length_units);

        DialogValueTV[WIDTH_POSITION] = (TextView) _root
                .findViewById(R.id.dz_capability_dialog_text_width_value);
        DialogLabelTV[WIDTH_POSITION] = (TextView) _root
                .findViewById(R.id.dz_capability_dialog_text_width_label);
        DialogUnitsTV[WIDTH_POSITION] = (TextView) _root
                .findViewById(R.id.dz_capability_dialog_text_width_units);

        SharedPreferences units_settings = PreferenceManager
                .getDefaultSharedPreferences(getActivity());

        boolean usingMeters = units_settings.getString(
                ATSKConstants.UNITS_DISPLAY,
                ATSKConstants.UNITS_FEET).equals(ATSKConstants.UNITS_METERS);

        for (int i = 0; i < DialogUnitsTV.length; i++) {
            if (DialogUnitsTV[i] != null) {
                if (!usingMeters)
                    DialogUnitsTV[i].setText(" yds");
                else
                    DialogUnitsTV[i].setText(" m");
            }
        }

        if (loadCurrentSurvey()) {
            if (_surveyData.circularAZ) {
                double radius = _surveyData.getRadius();

                if (!usingMeters)
                    radius = (float) (radius * Conversions.M2F / 3);

                DialogValueTV[RADIUS_POSITION].setText(String.format("%.0f",
                        radius));

                DialogValueTV[LENGTH_POSITION].setVisibility(TextView.GONE);
                DialogLabelTV[LENGTH_POSITION].setVisibility(TextView.GONE);
                DialogUnitsTV[LENGTH_POSITION].setVisibility(TextView.GONE);

                DialogValueTV[WIDTH_POSITION].setVisibility(TextView.GONE);
                DialogLabelTV[WIDTH_POSITION].setVisibility(TextView.GONE);
                DialogUnitsTV[WIDTH_POSITION].setVisibility(TextView.GONE);
            } else {
                double length = _surveyData.getLength();
                double width = _surveyData.width;
                if (!usingMeters) {
                    length = (float) (length * Conversions.M2F / 3);
                    width = (float) (width * Conversions.M2F / 3);

                }

                DialogValueTV[LENGTH_POSITION].setText(String.format("%.0f",
                        length));
                DialogValueTV[WIDTH_POSITION].setText(String.format("%.0f",
                        width));

                DialogValueTV[RADIUS_POSITION].setVisibility(TextView.GONE);
                DialogLabelTV[RADIUS_POSITION].setVisibility(TextView.GONE);
                DialogUnitsTV[RADIUS_POSITION].setVisibility(TextView.GONE);
            }
        }

        _altText = (TextView) _root
                .findViewById(R.id.dz_capability_dialog_text_altitude_label);

        _forText = (TextView) _root
                .findViewById(R.id.dz_capability_dialog_text_FOR);

        _dayText = (TextView) _root
                .findViewById(R.id.dz_capability_dialog_text_DAY);

        _nightText = (TextView) _root
                .findViewById(R.id.dz_capability_dialog_text_NIGHT);

        _cdsText = (TextView) _root
                .findViewById(R.id.dz_capability_dialog_text_CDS);

        _cdsDayText = (TextView) _root
                .findViewById(R.id.dz_capability_dialog_text_DayCDS);

        _cdsNightText = (TextView) _root
                .findViewById(R.id.dz_capability_dialog_text_NightCDS);

        _perText = (TextView) _root
                .findViewById(R.id.dz_capability_dialog_text_PER);

        _perDayText = (TextView) _root
                .findViewById(R.id.dz_capability_dialog_text_DayPER);

        _perNightText = (TextView) _root
                .findViewById(R.id.dz_capability_dialog_text_NightPER);

        _heText = (TextView) _root
                .findViewById(R.id.dz_capability_dialog_text_HE);

        _heDayText = (TextView) _root
                .findViewById(R.id.dz_capability_dialog_text_DayHE);

        _heNightText = (TextView) _root
                .findViewById(R.id.dz_capability_dialog_text_NightHE);

        _mffText = (TextView) _root
                .findViewById(R.id.dz_capability_dialog_text_MFF);

        _mffDayText = (TextView) _root
                .findViewById(R.id.dz_capability_dialog_text_DayMFF);

        _mffNightText = (TextView) _root
                .findViewById(R.id.dz_capability_dialog_text_NightMFF);

        _satbText = (TextView) _root
                .findViewById(R.id.dz_capability_dialog_text_SATB);

        _satbDayText = (TextView) _root
                .findViewById(R.id.dz_capability_dialog_text_DaySATB);

        _satbNightText = (TextView) _root
                .findViewById(R.id.dz_capability_dialog_text_NightSATB);

        _crrcText = (TextView) _root
                .findViewById(R.id.dz_capability_dialog_text_CRRC);

        _crrcDayText = (TextView) _root
                .findViewById(R.id.dz_capability_dialog_text_DayCRRC);

        _crrcNightText = (TextView) _root
                .findViewById(R.id.dz_capability_dialog_text_NightCRRC);

        _hslladsText = (TextView) _root
                .findViewById(R.id.dz_capability_dialog_text_HSLLADS);

        _hslladsDayText = (TextView) _root
                .findViewById(R.id.dz_capability_dialog_text_DayHSLLADS);

        _hslladsNightText = (TextView) _root
                .findViewById(R.id.dz_capability_dialog_text_NightHSLLADS);

        _hvcdsText = (TextView) _root
                .findViewById(R.id.dz_capability_dialog_text_HVCDS);

        _hvcdsDayText = (TextView) _root
                .findViewById(R.id.dz_capability_dialog_text_DayHVCDS);

        _hvcdsNightText = (TextView) _root
                .findViewById(R.id.dz_capability_dialog_text_NightHVCDS);

        _jpadsText = (TextView) _root
                .findViewById(R.id.dz_capability_dialog_text_JPADS);

        _jpadsDayText = (TextView) _root
                .findViewById(R.id.dz_capability_dialog_text_DayJPADS);

        _jpadsNightText = (TextView) _root
                .findViewById(R.id.dz_capability_dialog_text_NightJPADS);

    }

    private void SetValues() {

        if (_dzCaps == null)
            return;

        _cdsDayText.setText(String.valueOf(_dzCaps.cds));
        _cdsNightText.setText(String.valueOf(_dzCaps.cds_night));

        _perDayText.setText(String.valueOf(_dzCaps.per));
        _perNightText.setText(String.valueOf(_dzCaps.per_night));

        _heDayText.setText(String.valueOf(_dzCaps.he));
        _heNightText.setText(String.valueOf(_dzCaps.he_night));

        _mffDayText.setText(String.valueOf(_dzCaps.mff));
        _mffNightText.setText(String.valueOf(_dzCaps.mff_night));

        _satbDayText.setText(String.valueOf(_dzCaps.satb));
        _satbNightText.setText(String.valueOf(_dzCaps.satb_night));

        _crrcDayText.setText(String.valueOf(_dzCaps.crrc));
        _crrcNightText.setText(String.valueOf(_dzCaps.crrc_night));

        _hslladsDayText.setText(String.valueOf(_dzCaps.hsllads));
        _hslladsNightText.setText(String.valueOf(_dzCaps.hsllads_night));

        _hvcdsDayText.setText(String.valueOf(_dzCaps.hvcds));
        _hvcdsNightText.setText(String.valueOf(_dzCaps.hvcds_night));

        _jpadsDayText.setText(String.valueOf(_dzCaps.jpads));
        _jpadsNightText.setText(String.valueOf(_dzCaps.jpads_night));

    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);

    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
    }

}
