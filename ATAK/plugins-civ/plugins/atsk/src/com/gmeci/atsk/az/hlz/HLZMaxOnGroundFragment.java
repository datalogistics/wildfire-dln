
package com.gmeci.atsk.az.hlz;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.atakmap.android.atsk.plugin.R;
import com.gmeci.atsk.az.AZTabBase;
import com.gmeci.atsk.az.spinners.ATSKSpinner;
import com.gmeci.atsk.az.spinners.HLZSpinner;
import com.gmeci.atsk.resources.ATSKApplication;
import com.gmeci.core.SurveyPoint;
import com.gmeci.vehicle.VehicleBlock;
import com.atakmap.coremap.log.Log;
import com.gmeci.conversions.Conversions.Unit;
import com.gmeci.core.ATSKConstants;

import java.util.ArrayList;
import java.util.Iterator;

public class HLZMaxOnGroundFragment extends AZTabBase {

    private static final String TAG = "HLZMaxOnGroundFragment";

    private static final String DIMENSIONS = "DIMENSIONS";
    private static final String TRAINING = "TRAINING";
    private static final String CONTINGENCY = "CONTINGENCY";
    private static final String BROWNOUT = "BROWNOUT";

    HLZRequirementsParser hlzParser;
    ArrayList<HelicopterRequirements> hlzReqList;
    ArrayList<String> heliNames;

    RadioGroup conditionGroupView;
    RadioButton dimenBtn, trainingBtn, contingencyBtn, brownoutBtn;
    ATSKSpinner ac;
    TextView critSize, maxOnGround;
    private View mView;
    private LayoutInflater mInflater;
    //State Variables
    private String curHelicopterName;
    private HelicopterRequirements curReqs;
    private int lastCheckedId;
    //Drawable IDs
    private int id_radio_button_unchecked;
    private int id_radio_button_checked;

    private static final double THIRD_METER = 1 / 3d;

    public void onResume() {
        super.onResume();
        SetupSpinner();
    }

    public void onPause() {
        if (surveyData != null)
            azpc.UpdateAZ(surveyData, "hlzmaxonground", true);
        super.onPause();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        Context pluginContext = ATSKApplication
                .getInstance().getPluginContext();
        mInflater = LayoutInflater.from(pluginContext);
        mView = mInflater.inflate(R.layout.hlz_max_on_ground_fragment,
                container, false);

        DetermineDrawableDimensionFromScreenSize();
        SetupTextViews();
        SetupRadioViews();

        return mView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

    }

    private void DetermineDrawableDimensionFromScreenSize() {
        int ScreenSize = getActivity().getResources().getConfiguration().screenLayout
                &
                Configuration.SCREENLAYOUT_SIZE_MASK;
        switch (ScreenSize) {
            case Configuration.SCREENLAYOUT_SIZE_LARGE:
                //TABLET
                id_radio_button_unchecked = R.drawable.radiobutton_unchecked;
                id_radio_button_checked = R.drawable.radiobutton_checked;
                break;
            case Configuration.SCREENLAYOUT_SIZE_NORMAL:
                //PHONE
                id_radio_button_unchecked = R.drawable.radiobutton_unchecked_small;
                id_radio_button_checked = R.drawable.radiobutton_checked_small;
                break;
            default:
                id_radio_button_unchecked = R.drawable.radiobutton_unchecked;
                id_radio_button_checked = R.drawable.radiobutton_checked;
        }
    }

    private void SetupRadioViews() {
        conditionGroupView = (RadioGroup) mView
                .findViewById(R.id.condition_selection);

        Context plugin = ATSKApplication
                .getInstance().getPluginContext();
        dimenBtn = new RadioButton(plugin);
        trainingBtn = new RadioButton(plugin);
        contingencyBtn = new RadioButton(plugin);
        brownoutBtn = new RadioButton(plugin);

        dimenBtn.setText("Dimensions");
        trainingBtn.setText("Training");
        brownoutBtn.setText("Brown Out");
        contingencyBtn.setText("Contingency");

        conditionGroupView.addView(dimenBtn);
        conditionGroupView.addView(trainingBtn);
        conditionGroupView.addView(contingencyBtn);
        conditionGroupView.addView(brownoutBtn);
        for (int i = 0; i < conditionGroupView.getChildCount(); i++) {
            ((RadioButton) conditionGroupView.getChildAt(i))
                    .setTextColor(Color.WHITE);
            ((RadioButton) conditionGroupView.getChildAt(i))
                    .setButtonDrawable(id_radio_button_unchecked);
        }

        lastCheckedId = dimenBtn.getId();

        conditionGroupView
                .setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(RadioGroup group, int checkedId) {

                        RadioButton btn = (RadioButton) group
                                .findViewById(checkedId);
                        btn.setButtonDrawable(id_radio_button_checked);

                        RadioButton lastbtn = (RadioButton) group
                                .findViewById(lastCheckedId);
                        lastbtn.setButtonDrawable(id_radio_button_unchecked);
                        lastCheckedId = checkedId;

                        UpdateMaxOnGround();
                    }

                });
    }

    private void SetupTextViews() {
        maxOnGround = (TextView) mView.findViewById(R.id.max_on_ground_display);
        critSize = (TextView) mView.findViewById(R.id.criteria_size);
    }

    private void SetupSpinner() {
        ac = (ATSKSpinner) mView
                .findViewById(R.id.helicopter_type_spinner);
        heliNames = new ArrayList<String>();
        hlzParser = new HLZRequirementsParser();
        hlzReqList = hlzParser.parseFile(HLZSpinner.HLZ_DATA_CSV);
        if (hlzReqList == null) {
            hlzReqList = new ArrayList<HelicopterRequirements>();
            Toast.makeText(getActivity(),
                    "Failed to load Helicopter Reqs...", Toast.LENGTH_SHORT)
                    .show();
            return;
        } else {
            Iterator<HelicopterRequirements> it = hlzReqList.iterator();
            while (it.hasNext()) {
                HelicopterRequirements next = it.next();
                heliNames.add(next.HeliName);
            }
        }
        ac.SetupSpinner(heliNames);

        ac.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> adapterView,
                    View clickedView,
                    int position, long arg3) {
                if (ac.getAdapter().getItem(position) != null) {
                    curHelicopterName = ac.getAdapter()
                            .getItem(position).toString();
                    UpdateMaxOnGround();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                if (ac.getAdapter().getItem(0) != null) {
                    curHelicopterName = ac.getAdapter().getItem(0)
                            .toString();
                    UpdateMaxOnGround();
                }
            }

        });

        String ant = surveyData.getMetaString("aircraftNumType", DIMENSIONS);
        if (ant.equals(DIMENSIONS)) {
            dimenBtn.setChecked(true);
            dimenBtn.setButtonDrawable(id_radio_button_checked);
        } else if (ant.equals(TRAINING)) {
            trainingBtn.setChecked(true);
            trainingBtn.setButtonDrawable(id_radio_button_checked);
        } else if (ant.equals(CONTINGENCY)) {
            contingencyBtn.setChecked(true);
            contingencyBtn.setButtonDrawable(id_radio_button_checked);
        } else if (ant.equals(BROWNOUT)) {
            brownoutBtn.setChecked(true);
            brownoutBtn.setButtonDrawable(id_radio_button_checked);
        }

        //set previously selected AC Name...
        int pos = -1;
        for (int i = 0; i < heliNames.size(); ++i) {
            if (surveyData.aircraft != null &&
                    heliNames.get(i).equalsIgnoreCase(surveyData.aircraft))
                pos = i;
        }
        Log.d(TAG, "found previously selected heli" + surveyData.aircraft);
        if (pos < 0) {
            ac.setSelection(0);
            UpdateMaxOnGround();
        } else {
            ac.setSelection(pos);
            UpdateMaxOnGround();
        }

        final ATSKSpinner surfaceSpinner = (ATSKSpinner) mView
                .findViewById(R.id.runway_surface_spinner);
        surfaceSpinner.setPrompt("SURFACE TYPES");
        surfaceSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                    int position, long id) {
                surveyData.surface = surfaceSpinner.getItem(position);
                Log.d(TAG, "hlz surface: " + surveyData.surface);
                azpc.UpdateAZ(surveyData, "surface", false);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }

        });

        int SurfacePosition = surfaceSpinner.GetPosition(surveyData.surface);
        surfaceSpinner.setSelection(SurfacePosition);

    }

    private void UpdateMaxOnGround() {
        loadCurrentSurvey();
        for (HelicopterRequirements req : hlzReqList) {
            if (req.HeliName.equals(curHelicopterName)) {
                curReqs = req;
                break;
            }
        }

        if (curReqs == null)
            return;

        double length_m = 0, width_m = 0;

        if (dimenBtn.isChecked()) {
            String blockName = curHelicopterName;
            if (blockName.equals("MH-47E"))
                blockName = "CH-47";
            else if (blockName.equals("MH-6/AH-6"))
                blockName = "MH-6";
            else if (blockName.equals("HH-60"))
                blockName = "UH-60";
            else if (blockName.equals("MH-53"))
                blockName = "HH-53";
            VehicleBlock block = new VehicleBlock(blockName);
            if (block.isValid()) {
                length_m = block.getDimensions()[0];
                width_m = block.getDimensions()[1];
                surveyData.setMetaString("aircraftNumType", DIMENSIONS);
            }
        } else if (trainingBtn.isChecked()) {
            length_m = curReqs.trainingLength_m;
            width_m = curReqs.trainingWidth_m;
            surveyData.setMetaString("aircraftNumType", TRAINING);
        } else if (contingencyBtn.isChecked()) {
            length_m = curReqs.contingencyLength_m;
            width_m = curReqs.contingencyWidth_m;
            surveyData.setMetaString("aircraftNumType", CONTINGENCY);
        } else if (brownoutBtn.isChecked()) {
            length_m = curReqs.brownoutLength_m;
            width_m = curReqs.brownoutWidth_m;
            surveyData.setMetaString("aircraftNumType", BROWNOUT);
        }

        Log.d(TAG, "values: " + surveyData.width + " " + surveyData.getLength()
                + " geometry: " + width_m + " " + length_m);
        int maxNumber = 0;
        if (length_m > 0 && width_m > 0) {
            if (surveyData.circularAZ) {
                double radius = surveyData.getRadius();
                double surveyArea = Math.pow(radius, 2) * Math.PI;
                double hlzArea = width_m * length_m;
                maxNumber = (int) Math.floor(surveyArea / hlzArea);
            } else {
                double surveyLen = surveyData.getLength() + THIRD_METER;
                double surveyWidth = surveyData.width + THIRD_METER;
                int width2Width = (int) (surveyWidth / width_m);
                int length2Length = (int) (surveyLen / length_m);
                int width2Length = (int) (surveyWidth / length_m);
                int length2Width = (int) (surveyLen / width_m);

                maxNumber = Math.max(width2Width * length2Length,
                        width2Length * length2Width);
            }

            Unit dispUnit = user_settings.getString(
                    ATSKConstants.UNITS_DISPLAY,
                    ATSKConstants.UNITS_FEET).equals(ATSKConstants.UNITS_FEET)
                    ? Unit.FOOT : Unit.METER;
            critSize.setText(String.format("(%s x %s)",
                    Unit.METER.format(length_m, dispUnit),
                    Unit.METER.format(width_m, dispUnit)));
        } else {
            critSize.setText(R.string.unknown);
            Log.w(TAG, "Failed to load criteria for " + curHelicopterName);
        }
        maxOnGround.setText(String.valueOf(maxNumber));
        surveyData.aircraftCount = maxNumber;
        surveyData.aircraft = curHelicopterName;

    }

    @Override
    public void newPosition(SurveyPoint sp, boolean TopCollected) {
    }

    @Override
    public void shotApproved(SurveyPoint sp, double range_m, double az_deg,
            double el_deg, boolean TopCollected) {
    }

    @Override
    protected void UpdateScreen() {
    }
}
