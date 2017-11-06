
package com.gmeci.atsk.az.dz;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.atakmap.android.atsk.plugin.R;
import com.gmeci.atsk.resources.ATSKApplication;
import com.gmeci.atsk.resources.ATSKDialogManager;
import com.gmeci.conversions.Conversions;
import com.gmeci.conversions.Conversions.Unit;
import com.gmeci.core.ATSKIntentConstants;
import com.gmeci.core.SurveyData;
import com.gmeci.atsk.az.AZTabBase;
import com.gmeci.atsk.az.spinners.ATSKSpinner;
import com.gmeci.atskservice.resolvers.AZURIConstants;
import com.gmeci.atskservice.dz.DZCapabilities;
import com.gmeci.atskservice.dz.DZRequirementsParser;
import com.gmeci.core.SurveyPoint;
import com.gmeci.helpers.AZHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DZImpactFragment extends AZTabBase
        implements OnClickListener, OnLongClickListener,
        ATSKDialogManager.DialogUpdateInterface,
        OnCheckedChangeListener {

    protected static final int NON_SELECTED_BG_COLOR = 0xff383838;
    protected static final double SELECTED_SIZE_MULTIPLIER = 1.25f;

    private static final int PI_CDS = 0;
    private static final int PI_PER = 1;
    private static final int PI_HE = 2;
    private static final int NUM_PI = 3;

    private String TAG = "DZImpactFragment";
    private CheckBox _nightDropBox, _customPI;
    private ATSKSpinner _acSpinner;
    private List<String> _aclist;
    private int _acPos = 0;
    private Button _capBtn;
    private DZRequirementsParser _dzParser;
    private View _root;
    private final TextView[] _piOffsets = new TextView[NUM_PI];
    private TextView _cdsLabel;
    private final LinearLayout[] _piLayouts = new LinearLayout[NUM_PI];
    private int _piSelected = -1;
    private int _piLongClicked = -1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        Context pluginContext = ATSKApplication
                .getInstance().getPluginContext();
        _root = LayoutInflater.from(pluginContext).inflate(
                R.layout.dz_crit_impact_fragment, container,
                false);
        return _root;
    }

    @Override
    public void onResume() {
        super.onResume();

        loadCurrentSurvey();
        SetupParser();
        SetupButtons();
        SetupFragment();
        updatePIViews();
    }

    private void SetupFragment() {
        if (this.surveyData != null) {
            _customPI.setChecked(this.surveyData.getMetaBoolean("customPI",
                    false));
            _nightDropBox.setChecked(surveyData.nightDrop);
        }

        SetupSpinners();
    }

    public void SetSurveyInterface() {
        super.SetSurveyInterface();
    }

    private void SetupParser() {

        _dzParser = new DZRequirementsParser();
        try {
            _dzParser.parseRequirementsFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        _customPI = (CheckBox) _root.findViewById(R.id.dz_custom_pi);
        _customPI.setOnCheckedChangeListener(this);
        _nightDropBox = (CheckBox) _root
                .findViewById(R.id.dz_crit_capability_check_nightdrop);
        _nightDropBox.setOnCheckedChangeListener(this);
        _cdsLabel = (TextView) _root.findViewById(R.id.cds_label);
        _piLayouts[PI_CDS] = (LinearLayout) _root.findViewById(R.id.cds_layout);
        _piOffsets[PI_CDS] = (TextView) _root.findViewById(R.id.cds_offset);
        _piLayouts[PI_PER] = (LinearLayout) _root.findViewById(R.id.per_layout);
        _piOffsets[PI_PER] = (TextView) _root.findViewById(R.id.per_offset);
        _piLayouts[PI_HE] = (LinearLayout) _root.findViewById(R.id.he_layout);
        _piOffsets[PI_HE] = (TextView) _root.findViewById(R.id.he_offset);
        for (int i = 0; i < NUM_PI; i++) {
            _piLayouts[i].setOnClickListener(this);
            _piLayouts[i].setOnLongClickListener(this);
        }
    }

    private void updatePIViews() {
        if (this.surveyData != null) {
            boolean customPI = surveyData.getMetaBoolean("customPI", false);
            Unit dispUnit = getDisplayUnit();
            _piOffsets[PI_CDS].setText(Unit.METER.format(
                    this.surveyData.cdsPIOffset, dispUnit));
            _piOffsets[PI_PER].setText(Unit.METER.format(
                    this.surveyData.perPIOffset, dispUnit));
            _piOffsets[PI_HE].setText(Unit.METER.format(
                    this.surveyData.hePIOffset, dispUnit));
            for (int i = 0; i < NUM_PI; i++) {
                _piLayouts[i].setVisibility(customPI && (!surveyData.circularAZ
                        || i <= PI_CDS) ? View.VISIBLE : View.GONE);
            }
            if (this.surveyData.circularAZ)
                _cdsLabel.setText("PI:");
            else
                _cdsLabel.setText("CDS:");
            _piLongClicked = -1;
        }
    }

    @Override
    public void onClick(View v) {
        if (this.surveyData == null)
            return;

        // Find clicked PI view index
        int pi = findPI(v);
        if (pi != -1)
            _piSelected = (_piSelected == pi ? -1 : pi);

        // Deselect all except currently selected
        for (int i = 0; i < NUM_PI; i++) {
            if (_piSelected == i)
                _piLayouts[i]
                        .setBackgroundResource(R.drawable.background_selected_center);
            else {
                _piLayouts[i].setBackgroundResource(0);
                _piLayouts[i].setPadding(0, 0, 0, 0);
            }
        }

        // Set collection mode
        ATSKApplication.setObstructionCollectionMethod(
                _piSelected == -1 ? ATSKIntentConstants.OB_STATE_HIDDEN
                        : ATSKIntentConstants.OB_STATE_REQUESTED_POINT, TAG,
                false);
    }

    @Override
    public boolean onLongClick(View v) {
        if (this.surveyData == null)
            return false;
        int pi = findPI(v);
        if (pi != -1) {
            double offset;
            switch (pi) {
                case PI_CDS:
                    offset = this.surveyData.cdsPIOffset;
                    break;
                case PI_PER:
                    offset = this.surveyData.perPIOffset;
                    break;
                default:
                case PI_HE:
                    offset = this.surveyData.hePIOffset;
                    break;
            }
            _piLongClicked = pi;
            ATSKDialogManager adm = new ATSKDialogManager(getActivity(), this,
                    false);
            adm.ShowMeasurementHandJamDialog(offset, getPIName(pi), _piSelected);
            return true;
        }
        return false;
    }

    @Override
    public void onCheckedChanged(CompoundButton v, boolean checked) {
        if (surveyData == null)
            return;
        if (v == _customPI) {
            _nightDropBox.setVisibility(checked ? View.GONE : View.VISIBLE);
            if (surveyData.getMetaBoolean("customPI", false) != checked) {
                surveyData.setMetaBoolean("customPI", checked);
                azpc.UpdateAZ(surveyData, "PI", false);
            }
            if (!checked) {
                if (_piSelected != -1)
                    onClick(_piLayouts[_piSelected]);
                getDZRequirements();
            }
            updatePIViews();
        } else if (v == _nightDropBox) {
            //adjust capability based on night numbers.....
            surveyData.nightDrop = checked;
            azpc.UpdateAZ(surveyData, "PI", false);
            getDZRequirements();
        }
    }

    @Override
    public void UpdateGSRAngleUnits(boolean GSR) {
    }

    @Override
    public void UpdateMeasurement(int index, double measurement) {
        if (_piLongClicked == -1 && _piSelected != -1)
            _piLongClicked = _piSelected;
        if (_piLongClicked != -1 && this.surveyData != null) {
            setPI(_piLongClicked, measurement);
            azpc.UpdateAZ(surveyData, AZURIConstants.DZ_PIS_UPDATED, true);
            updatePIViews();
        }
    }

    @Override
    public void UpdateStringValue(int index, String value) {
    }

    @Override
    public void UpdateAngleUnits(boolean usingTrue) {
    }

    @Override
    public void UpdateDimensionUnits(boolean usingFeet) {
        DisplayUnitsStandard = usingFeet;
    }

    private void SetupSpinners() {

        if (_dzParser == null)
            return;
        _aclist = _dzParser.getAircraft();

        _acSpinner = (ATSKSpinner) _root
                .findViewById(R.id.dz_crit_capability_spin_ac);
        _acSpinner.SetupSpinner(new ArrayList<String>(_aclist));

        //set previously selected AC Name...
        int pos = -1;
        for (int i = 0; i < _aclist.size(); ++i) {
            if (surveyData.aircraft != null &&
                    _aclist.get(i).equalsIgnoreCase(surveyData.aircraft))
                pos = i;
        }
        if (pos < 0) {
            _acSpinner.setSelection(0);
            setSurveyAircraft((String) _acSpinner.getAdapter().getItem(0));
        } else {
            _acSpinner.setSelection(pos);
        }

        _acSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adaptparent, View view,
                    int pos, long id) {
                _acPos = pos;
                surveyData.aircraft = _aclist.get(pos);
                azpc.UpdateAZ(surveyData, "ACName", false);

                getDZRequirements();

            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }

        });
    }

    private int findPI(View v) {
        int pi = -1;
        for (int i = 0; i < NUM_PI; i++) {
            if (_piLayouts[i] == v || _piOffsets[i] == v) {
                pi = i;
                break;
            }
        }
        return pi;
    }

    private String getPIName(int index) {
        switch (index) {
            case PI_CDS:
                return "CDS";
            case PI_PER:
                return "PER";
            case PI_HE:
                return "HE";
        }
        return "";
    }

    private void setSurveyAircraft(String ac) {
        if (ac != null)
            surveyData.aircraft = ac;
        else
            return;

        azpc.UpdateAZ(surveyData, "ACName", false);
    }

    private void getDZRequirements() {

        if (surveyData.circularAZ) {
            //circular DZ's PI is generally center. 
            DZCapabilities dzcaps = _dzParser.getDZCapabilities(
                    (int) surveyData.getRadius() * 2,
                    (int) surveyData.getRadius() * 2, 600,
                    surveyData.aircraft);
            SetDZPIs(surveyData, dzcaps);
        } else {
            DZCapabilities dzcaps = _dzParser.getDZCapabilities(
                    surveyData.getLength(), surveyData.width, 600f,
                    surveyData.aircraft);
            SetDZPIs(surveyData, dzcaps);
        }

    }

    private void SetupButtons() {

        _capBtn = (Button) _root
                .findViewById(R.id.dz_crit_capability_button_showcapability);
        _capBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showCapabilityDialog();
            }
        });
    }

    private void showCapabilityDialog() {

        DZCapabilityDialogFragment capability = new DZCapabilityDialogFragment();

        capability.Initialize(_dzParser);

        String acname = "";
        if (_aclist.get(_acPos) != null)
            acname = _aclist.get(_acPos);

        capability.setupDialog(acname, azpc);

        capability.show(getFragmentManager(), "DZCAPABILITY");
    }

    protected void SetSurveyedPI() {
        // Frenchy slides for DZ say Surveyed will place all PI in the center of the DZ. 
        if (surveyData != null) {
            setPI(PI_CDS, surveyData.getLength() / 2);
            setPI(PI_HE, surveyData.getLength() / 2);
            setPI(PI_PER, surveyData.getLength() / 2);
            updatePIViews();
            azpc.UpdateAZ(surveyData, AZURIConstants.DZ_PIS_UPDATED, true);
        }
    }

    protected void SetMissionSpecificPI() {
        // Frenchy slides for DZ say Mission Specific will use the 13-217 offsets. 
        getDZRequirements();
    }

    protected void SetDZPIs(SurveyData surveyData, DZCapabilities dzreq) {
        if (surveyData.getMetaBoolean("customPI", false))
            return;
        if (!surveyData.nightDrop) {
            setPI(PI_CDS, dzreq.cds_pi);
            setPI(PI_HE, dzreq.he_pi);
            setPI(PI_PER, dzreq.per_pi);
        } else {
            setPI(PI_CDS, dzreq.cds_pi_night);
            setPI(PI_HE, dzreq.he_pi_night);
            setPI(PI_PER, dzreq.per_pi_night);
        }
        surveyData.setMetaDouble("cds_horiz_offset", 0);
        surveyData.setMetaDouble("per_horiz_offset", 0);
        surveyData.setMetaDouble("he_horiz_offset", 0);
        updatePIViews();
        azpc.UpdateAZ(surveyData, AZURIConstants.DZ_PIS_UPDATED, true);
    }

    private void setPI(int index, double offset, double elevation_m_hae) {
        boolean validAlt = elevation_m_hae != SurveyPoint.Altitude.INVALID;
        switch (index) {
            case PI_CDS:
                this.surveyData.cdsPIOffset = offset;
                this.surveyData.cdsPIElevation = validAlt ? elevation_m_hae
                        : ATSKApplication
                                .getAltitudeHAE(AZHelper
                                        .CalculatePointOfImpact(
                                                this.surveyData, "cds"));
                break;
            case PI_PER:
                this.surveyData.perPIOffset = offset;
                this.surveyData.perPIElevation = validAlt ? elevation_m_hae
                        : ATSKApplication
                                .getAltitudeHAE(AZHelper
                                        .CalculatePointOfImpact(
                                                this.surveyData, "per"));
                break;
            case PI_HE:
                this.surveyData.hePIOffset = offset;
                this.surveyData.hePIElevation = validAlt ? elevation_m_hae
                        : ATSKApplication.getAltitudeHAE(AZHelper
                                .CalculatePointOfImpact(this.surveyData, "he"));
                break;
        }
    }

    private void setPI(int index, double offset) {
        setPI(index, offset, SurveyPoint.Altitude.INVALID);
    }

    @Override
    public void newPosition(SurveyPoint sp, boolean TopCollected) {
        if (this.surveyData == null)
            return;

        // Calculate perpendicular distance between leading edge and point
        SurveyPoint leading = AZHelper.CalculateCenterOfEdge(this.surveyData,
                true);
        double[] ra = Conversions.calculateRangeAngle(leading, sp);
        ra[1] = Conversions.deg360(ra[1] - this.surveyData.angle);
        double vertiOffset = ra[0] * Math.cos(Math.toRadians(ra[1]));
        double horizOffset = ra[0] * Math.sin(Math.toRadians(ra[1]));

        // Set appropriate offset
        String piName = getPIName(_piSelected);
        if (!piName.isEmpty())
            this.surveyData.setMetaDouble(piName.toLowerCase()
                    + "_horiz_offset", horizOffset);

        setPI(_piSelected, vertiOffset, sp.getHAE());
        azpc.UpdateAZ(surveyData, AZURIConstants.DZ_PIS_UPDATED, true);
        updatePIViews();
    }

    @Override
    protected void UpdateScreen() {
        // TODO Auto-generated method stub

    }

}
