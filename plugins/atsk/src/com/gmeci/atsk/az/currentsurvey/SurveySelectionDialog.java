
package com.gmeci.atsk.az.currentsurvey;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.log.Log;

import java.util.Arrays;
import java.util.UUID;

import android.app.AlertDialog;

import com.atakmap.android.atsk.plugin.R;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.gmeci.atsk.resources.ATSKApplication;
import com.gmeci.core.ATSKConstants;
import com.gmeci.core.SurveyPoint;
import com.gmeci.atsk.az.spinners.ATSKSpinner;
import com.gmeci.core.SurveyData;
import com.gmeci.core.SurveyData.AZ_TYPE;
import com.gmeci.atsk.az.AZCursorAdapter;
import com.gmeci.atskservice.resolvers.AZProviderClient;
import com.gmeci.atskservice.resolvers.AZURIConstants;

public class SurveySelectionDialog extends DialogFragment implements
        OnClickListener {

    private static final String TAG = "SurveySelectionDialog";
    UpdateSurveyNameInterface parentInterface;
    private TextView title;

    //ListViews and related    
    private ListView currentSurveysListView;

    Button doneButton, addSurveyButton;

    AZProviderClient azpc;
    AZCursorAdapter cursorAdapter;
    private String _surveyUID = "",
            _surveyName = "",
            _surveyType = "";

    final OnItemClickListener surveyClickListener = new OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position,
                long arg3) {
            showSurveyOptionsDialog(position);
        }
    };
    final AdapterView.OnItemLongClickListener surveyLongClickListener = new AdapterView.OnItemLongClickListener() {

        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view,
                int position, long id) {
            showSurveyOptionsDialog(position);
            return true;
        }
    };
    //standard fragment start (view and inflator)
    private View _root;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        Context pluginContext = ATSKApplication
                .getInstance().getPluginContext();
        LayoutInflater pluginInflater = LayoutInflater.from(pluginContext);
        _root = pluginInflater.inflate(
                R.layout.current_survey_selectiondialog,
                container, false);
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getDialog().getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        int width = getResources().getDisplayMetrics().widthPixels;
        int height = getResources().getDisplayMetrics().heightPixels;
        _root.setMinimumWidth((int) (width * 0.70f));
        _root.setMinimumHeight((int) (height * 0.50f));

        return _root;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        azpc = new AZProviderClient(getActivity());
        azpc.Start();
        getCurrentSurveyUID();
        SetupListView();
        SetupButtons();

    }

    private void getCurrentSurveyUID() {

        _surveyUID = azpc.getSetting(ATSKConstants.CURRENT_SURVEY, TAG);

        SurveyData currentSurvey = azpc.getAZ(_surveyUID, false);
        if (currentSurvey == null)
            return;
        _surveyName = currentSurvey.getSurveyName();
        _surveyType = currentSurvey.getType().toString();

    }

    private void SetupButtons() {

        doneButton = (Button) _root
                .findViewById(R.id.surveyselection_button_done);
        doneButton.setOnClickListener(this);

        addSurveyButton = (Button) _root
                .findViewById(R.id.surveyselection_button_addsurvey);
        addSurveyButton.setOnClickListener(this);
        addSurveyButton.setTextColor(ATSKConstants.LIGHT_BLUE);

        final SharedPreferences _prefs =
                PreferenceManager.getDefaultSharedPreferences(getActivity());

        CheckBox cb = (CheckBox) _root
                .findViewById(R.id.show_on_start);
        cb.setChecked(_prefs.getBoolean(ATSKConstants.LAUNCH_PREF, false));
        cb.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {

            public void onCheckedChanged(CompoundButton cb, boolean checked) {
                _prefs.edit().putBoolean(ATSKConstants.LAUNCH_PREF, checked)
                        .apply();
            }
        });
    }

    private void SetupListView() {
        title = (TextView) _root.findViewById(R.id.surveyselection_text_title);

        title.setTextColor(ATSKConstants.LIGHT_BLUE);

        currentSurveysListView = (ListView) _root
                .findViewById(R.id.surveyselection_listview_surveys);

        rebuildSurveyList();

        registerForContextMenu(currentSurveysListView);
        currentSurveysListView.setOnCreateContextMenuListener(this);
        currentSurveysListView.setOnItemClickListener(surveyClickListener);
        currentSurveysListView
                .setOnItemLongClickListener(surveyLongClickListener);

    }

    @Override
    public void onPause() {
        super.onPause();
        //pass the current name and type
        String newUID = azpc.getSetting(ATSKConstants.CURRENT_SURVEY, TAG);
        SurveyData currentSurvey = azpc.getAZ(newUID, true);
        if (currentSurvey != null) {
            String newName = currentSurvey.getSurveyName();
            String newType = currentSurvey.getType().toString();

            if (parentInterface != null && !(_surveyUID.equals(newUID) &&
                    _surveyName.equals(newName) && _surveyType.equals(newType))) {
                parentInterface.updateCurrentSurveyHandle(newUID, newName,
                        newType);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "cleaning up the surveys_cursor");
        try {
            if (surveys_cursor != null)
                surveys_cursor.close();
        } catch (Exception e) {
        }

        azpc.Stop();

    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);

    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
    }

    synchronized protected void showSurveyOptionsDialog(int position) {

        Cursor cursor = (Cursor) cursorAdapter.getItem(position);
        if (cursor == null)
            return;

        String uid = cursor.getString(cursor
                .getColumnIndex(AZURIConstants.COLUMN_UID));
        SurveyOptionsDialog options = new SurveyOptionsDialog(
                getActivity(), getActivity()
                        .getSupportFragmentManager(), uid, azpc);

        String name = azpc.getAZName(uid);
        if (name == null || name.isEmpty())
            name = "(blank)";

        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(
                getActivity());
        alertBuilder
                .setView(options.getView())
                .setTitle(name)
                .setCancelable(false)
                .setNegativeButton("Back",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                rebuildSurveyList();
                            }
                        })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    public void onCancel(final DialogInterface dialog) {
                        rebuildSurveyList();
                    }
                });

        AlertDialog ad = alertBuilder.create();
        options.setAlertDialog(ad);
        ad.show();

    }

    Cursor surveys_cursor = null;

    synchronized public void rebuildSurveyList() {

        String selectedUID = azpc.getSetting(ATSKConstants.CURRENT_SURVEY, TAG);

        final Cursor old_cursor = surveys_cursor;

        cursorAdapter = new AZCursorAdapter(getActivity(),
                surveys_cursor = azpc.getAllSurveys(), azpc, selectedUID);
        currentSurveysListView.setAdapter(cursorAdapter);

        try {
            if (old_cursor != null)
                old_cursor.close();
        } catch (Exception e) {
        }

        closeKeyboard();

    }

    @Override
    public void onClick(View v) {
        if (v == doneButton) {
            dismiss();

        }
        else if (v == addSurveyButton)
            showAddSurveyDialog();
    }

    private void showAddSurveyDialog() {
        final AddSurveyDialog addDialog =
                new AddSurveyDialog(getActivity(), azpc);

        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(
                getActivity());
        alertBuilder
                .setTitle("Create New Survey")
                .setView(addDialog.getView())
                .setPositiveButton("Create",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                addDialog.createSurvey();
                                rebuildSurveyList();
                                SurveySelectionDialog.this.dismiss();
                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {
                            }
                        });
        alertBuilder.create().show();
    }

    public void closeKeyboard() {
        Activity act = getActivity();
        if (act != null) {
            InputMethodManager imm = (InputMethodManager) act
                    .getSystemService(Activity.INPUT_METHOD_SERVICE);
            if (imm != null && imm.isAcceptingText())
                imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
        }
    }

    public void SetUpdateInterface(UpdateSurveyNameInterface updateInterface) {
        parentInterface = updateInterface;
    }

    public static class AddSurveyDialog {

        private static String TAG = "AddSurveyDialog";
        final protected Context context;
        final protected EditText inputEdit;
        final protected AZProviderClient azpc;
        final protected TextView surveyNameStaticText;
        final protected TextView surveyTypeStaticText;
        final View root;
        final ATSKSpinner typeSpinner;

        static final int SPINNER_LZ = 0;
        static final int SPINNER_DZ = 1;
        static final int SPINNER_HLZ = 2;
        static final int SPINNER_FARP = 3;
        static final int SPINNER_COUNT = 4;
        final ATSKSpinner[] acSpinner = new ATSKSpinner[SPINNER_COUNT];
        final String[] azTypes = new String[SPINNER_COUNT];

        public AddSurveyDialog(Context context, final AZProviderClient azpc) {
            this.context = context;
            this.azpc = azpc;
            Context pluginContext = ATSKApplication
                    .getInstance().getPluginContext();
            LayoutInflater inflater = LayoutInflater.from(pluginContext);
            root = inflater.inflate(
                    R.layout.current_survey_add_dialog, null);

            surveyNameStaticText = (TextView) root
                    .findViewById(R.id.new_survey_dialog_text_label);
            surveyTypeStaticText = (TextView) root
                    .findViewById(R.id.new_survey_dialog_type_label);

            inputEdit = (EditText) root
                    .findViewById(R.id.new_survey_dialog_edit_surveyname);
            inputEdit.setTextColor(0xFFFFFFFF);
            inputEdit.setInputType(InputType.TYPE_CLASS_TEXT);
            inputEdit.setCursorVisible(true);
            inputEdit.setSelectAllOnFocus(true);
            inputEdit.setBackgroundResource(R.drawable.fullborder_background);

            azTypes[SPINNER_LZ] = "LZ";
            azTypes[SPINNER_DZ] = "DZ";
            azTypes[SPINNER_HLZ] = "HLZ";
            azTypes[SPINNER_FARP] = "FARP";

            // Criteria spinners
            acSpinner[SPINNER_LZ] = (ATSKSpinner)
                    root.findViewById(R.id.lz_ac_spinner);
            acSpinner[SPINNER_DZ] = (ATSKSpinner)
                    root.findViewById(R.id.dz_ac_spinner);
            acSpinner[SPINNER_HLZ] = (ATSKSpinner)
                    root.findViewById(R.id.hlz_ac_spinner);
            acSpinner[SPINNER_FARP] = (ATSKSpinner)
                    root.findViewById(R.id.farp_ac_spinner);

            typeSpinner = (ATSKSpinner) root
                    .findViewById(R.id.new_survey_dialog_spinner_aztype);
            typeSpinner.SetupSpinner(Arrays.asList(azTypes));
            typeSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent,
                        View view, int position, long id) {

                    String name = inputEdit.getText().toString();
                    String type = typeSpinner.getItem(position);

                    // Show/hide matching spinner
                    for (int i = SPINNER_LZ; i < SPINNER_COUNT; i++) {
                        acSpinner[i].setVisibility(
                                i == position ? View.VISIBLE : View.GONE);
                        // Update name if name is set to default
                        if (name.isEmpty() || name.equalsIgnoreCase(azTypes[i]))
                            inputEdit.setText(type);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
            typeSpinner.setSelection(0);
        }

        public View getView() {
            return root;
        }

        public void createSurvey() {
            int type = typeSpinner.getSelectedItemPosition();
            String ac = null;
            if (type >= SPINNER_LZ && type < SPINNER_COUNT)
                ac = (String) acSpinner[type].getSelectedItem();
            createSurvey(inputEdit.getText().toString(),
                    (String) typeSpinner.getSelectedItem(), ac);
        }

        protected void createSurvey(String surveyname, String type,
                String aircraft) {
            if (azpc == null)
                return;

            SurveyData survey = new SurveyData();
            survey.setSurveyName(surveyname);
            survey.visible = true;

            if (type.equals("LZ"))
                survey.setType(AZ_TYPE.LZ);
            else if (type.equals("DZ"))
                survey.setType(AZ_TYPE.DZ);
            else if (type.equals("HLZ"))
                survey.setType(AZ_TYPE.HLZ);
            else if (type.equals("FARP"))
                survey.setType(AZ_TYPE.FARP);

            survey.uid = UUID.randomUUID().toString();
            if (aircraft != null)
                survey.aircraft = aircraft;

            final MapView view = MapView.getMapView();
            GeoPoint location = view.getCenterPoint();
            survey.center = new SurveyPoint(location.getLatitude(),
                    location.getLongitude());

            azpc.NewAZ(survey);
            azpc.putSetting(ATSKConstants.CURRENT_SURVEY, survey.uid,
                    "AddSurvey");
        }

    }
}
