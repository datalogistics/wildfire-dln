
package com.gmeci.atsk.export.imagecapture;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.atakmap.android.atsk.plugin.R;
import com.atakmap.android.imagecapture.CaptureDialog;
import com.atakmap.android.imagecapture.CapturePP;
import com.atakmap.android.imagecapture.CapturePrefs;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.util.LimitingThread;
import com.atakmap.coremap.conversions.Span;
import com.atakmap.coremap.log.Log;
import com.gmeci.atsk.az.currentsurvey.UpdateSurveyNameInterface;
import com.gmeci.atsk.resources.ATSKApplication;
import com.gmeci.atsk.visibility.VizController;
import com.gmeci.atsk.visibility.VizLayout;
import com.gmeci.atsk.visibility.VizPrefs;
import com.gmeci.conversions.Conversions.Unit;
import com.gmeci.core.SurveyData;
import com.gmeci.core.SurveyData.AZ_TYPE;

import com.atakmap.coremap.locale.LocaleUtil;

/**
 * Survey image capture dialog w/ post-processing options
 */
public class SurveyCaptureDialog extends CaptureDialog
        implements View.OnClickListener, TextWatcher, UpdateSurveyNameInterface {

    private static final String TAG = "SurveyCaptureDialog";

    // Radio buttons
    private static final int INFO_TR = 0;
    private static final int INFO_TL = 1;
    private static final int INFO_BR = 2;
    private static final int INFO_BL = 3;
    private static final int COMPASS_LEFT = 4;
    private static final int COMPASS_RIGHT = 5;
    private static final int RB_COUNT = 6;

    // Check boxes
    private static final int SHOW_IMAGERY = 0;
    private static final int SHOW_INFOBOX = 1;
    private static final int SHOW_ARROWS = 2;
    private static final int SHOW_SCALE = 3;
    private static final int SHOW_HEADINGS = 4;
    private static final int SHOW_DIMENSIONS = 5;
    private static final int CB_COUNT = 6;

    // Text input
    private static final int FONT_SIZE = 0;
    private static final int LABEL_SIZE = 1;
    private static final int ICON_SIZE = 2;
    private static final int LINE_WEIGHT = 3;
    private static final int ET_COUNT = 4;

    private final MapView _mapView;
    private SurveyData _survey;
    private CapturePP _postDraw;
    private final RadioButton[] _rb = new RadioButton[RB_COUNT];
    private final CheckBox[] _cb = new CheckBox[CB_COUNT];
    private final Button[] _cbBtn = new Button[CB_COUNT];
    private final EditText[] _et = new EditText[ET_COUNT];
    private View _scrollView;
    private FrameLayout _extraView;
    private VizLayout _vizLayout;

    public SurveyCaptureDialog(MapView mapView, SurveyData survey,
            CapturePP postDraw) {
        super(mapView.getContext(), postDraw);
        _mapView = mapView;
        _survey = survey;
        _postDraw = postDraw;
    }

    @Override
    protected void setupView() {
        _title = (TextView) _root.findViewById(R.id.image_dlg_title);
        _imgView = (ImageView) _root.findViewById(R.id.image_dlg_bitmap);
        _loader = (ProgressBar) _root.findViewById(R.id.image_dlg_loader);

        _viewHandler = new Handler();
        onViewCreated();

        _created = true;
    }

    @Override
    public void onViewCreated() {
        _container = (LinearLayout) _root
                .findViewById(R.id.image_dlg_container);

        // Overlay manager
        _vizLayout = new VizLayout();
        _vizLayout.setMapView(_mapView);
        _vizLayout.init();
        _vizLayout.showTools(false);
        _vizLayout.setSurvey(_survey);
        _vizLayout.resume();

        final View vlRoot = _vizLayout.getRootView();
        vlRoot.findViewById(R.id.viz_title).setVisibility(View.GONE);
        Spinner ampSpinner = (Spinner) vlRoot.findViewById(
                R.id.viz_lz_amps_spinner);
        if (ampSpinner != null && ampSpinner.getParent() instanceof ViewGroup)
            ((ViewGroup) ampSpinner.getParent()).removeView(ampSpinner);

        Button vlBack = (Button) vlRoot.findViewById(R.id.viz_back);
        vlBack.setVisibility(View.VISIBLE);
        vlBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                View toggleView = vlRoot.findViewById(R.id.viz_toggles);
                View colorsView = vlRoot.findViewById(R.id.viz_colors);
                if (colorsView.getVisibility() == View.VISIBLE) {
                    toggleView.setVisibility(View.VISIBLE);
                    colorsView.setVisibility(View.GONE);
                } else {
                    showVizManager(false);
                }
            }
        });

        _scrollView = _root.findViewById(R.id.viz_cap_scroll_view);
        _extraView = (FrameLayout) _root.findViewById(R.id.viz_cap_extra_view);
        _extraView.addView(vlRoot);

        _et[FONT_SIZE] = (EditText) _root.findViewById(R.id.viz_cap_font_size);
        _et[LABEL_SIZE] = (EditText) _root
                .findViewById(R.id.viz_cap_label_size);
        _et[ICON_SIZE] = (EditText) _root.findViewById(R.id.viz_cap_icon_size);
        _et[LINE_WEIGHT] = (EditText) _root
                .findViewById(R.id.viz_cap_line_weight);

        _rb[INFO_TR] = (RadioButton) _root
                .findViewById(R.id.surveyimg_infobox_tr);
        _rb[INFO_TL] = (RadioButton) _root
                .findViewById(R.id.surveyimg_infobox_tl);
        _rb[INFO_BR] = (RadioButton) _root
                .findViewById(R.id.surveyimg_infobox_br);
        _rb[INFO_BL] = (RadioButton) _root
                .findViewById(R.id.surveyimg_infobox_bl);
        _rb[COMPASS_LEFT] = (RadioButton) _root.findViewById(
                R.id.compass_left);
        _rb[COMPASS_RIGHT] = (RadioButton) _root.findViewById(
                R.id.compass_right);

        // Check boxes
        _cb[SHOW_IMAGERY] = (CheckBox) _root
                .findViewById(R.id.viz_cap_show_imagery);
        _cb[SHOW_INFOBOX] = (CheckBox) _root
                .findViewById(R.id.viz_cap_info_box);
        _cb[SHOW_ARROWS] = (CheckBox) _root
                .findViewById(R.id.viz_cap_north_arrows);
        _cb[SHOW_SCALE] = (CheckBox) _root
                .findViewById(R.id.viz_cap_scale_bar);
        _cb[SHOW_HEADINGS] = (CheckBox) _root
                .findViewById(R.id.viz_cap_headings);
        _cb[SHOW_DIMENSIONS] = (CheckBox) _root
                .findViewById(R.id.viz_cap_dimensions);

        // Check box edit buttons
        _cbBtn[SHOW_INFOBOX] = (Button) _root
                .findViewById(R.id.viz_cap_edit_info);
        _cbBtn[SHOW_SCALE] = (Button) _root
                .findViewById(R.id.viz_cap_edit_scale);

        // Switch to overlay manager
        Button launchViz = (Button) _root
                .findViewById(R.id.launch_overlay_manager);
        launchViz.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showVizManager(true);
            }
        });

        // Set text inputs
        for (int i = 0; i < ET_COUNT; i++) {
            if (_et[i] == null)
                Log.w(TAG, "Text input[" + i + "] is null!");
            else {
                int defValue;
                String key = getETPrefIndex(i);
                switch (i) {
                    case FONT_SIZE:
                        defValue = 12;
                        break;
                    case LABEL_SIZE:
                        defValue = 12;
                        break;
                    case ICON_SIZE:
                        defValue = 28;
                        break;
                    default:
                    case LINE_WEIGHT:
                        defValue = 5;
                        break;
                }
                _et[i].setText(String.valueOf(CapturePrefs.get(key, defValue)));
                _et[i].addTextChangedListener(this);
            }
        }

        updateRadioButtons();

        // Set check boxes
        for (int i = 0; i < CB_COUNT; i++) {
            if (_cb[i] == null)
                Log.w(TAG, "Check box[" + i + "] is null!");
            else {
                if (i == SHOW_IMAGERY)
                    _cb[i].setChecked(CapturePrefs.get(
                            CapturePrefs.PREF_SHOW_IMAGERY,
                            true));
                else
                    _cb[i].setChecked(VizPrefs.get(getCBPrefIndex(i)));
                if (_cbBtn[i] != null)
                    _cbBtn[i].setOnClickListener(this);
                _cb[i].setOnCheckedChangeListener(_checkChanged);
            }
        }
    }

    private void updateRadioButtons() {
        // Set radio buttons;
        int infoPos = (Integer) SurveyCapturePrefs.get(
                SurveyCapturePrefs.PREF_INFOBOX_POS);
        int compassPos = (Integer) SurveyCapturePrefs.get(
                SurveyCapturePrefs.PREF_COMPASS_POS);
        if (infoPos == -1)
            infoPos = SurveyCapturePrefs.INFO_POS_TR;
        SurveyCapturePrefs.set(SurveyCapturePrefs.PREF_INFOBOX_POS, infoPos);
        fixOverlap(true);

        for (int i = 0; i < RB_COUNT; i++) {
            if (_rb[i] == null)
                Log.w(TAG, "Radio button[" + i + "] is null!");
            else {
                _rb[i].setOnCheckedChangeListener(null);
                if (getInfoPref(i) == infoPos
                        || getCompassPref(i) == compassPos)
                    _rb[i].setChecked(true);
                _rb[i].setOnCheckedChangeListener(_checkChanged);
            }
        }
    }

    /**
     * Fix overlap between info box and compass
     * @param infoFirst True for info box to take priority over compass
     */
    private void fixOverlap(boolean infoFirst) {
        int infoPos = (Integer) SurveyCapturePrefs.get(SurveyCapturePrefs
                .PREF_INFOBOX_POS);
        int compassPos = (Integer) SurveyCapturePrefs.get(SurveyCapturePrefs
                .PREF_COMPASS_POS);
        int compassRB = -1, infoRB = -1;
        if (infoPos == SurveyCapturePrefs.INFO_POS_TR
                && compassPos == SurveyCapturePrefs.COMPASS_POS_RIGHT) {
            infoRB = INFO_TL;
            compassRB = COMPASS_LEFT;
        } else if (infoPos == SurveyCapturePrefs.INFO_POS_TL
                && compassPos == SurveyCapturePrefs.COMPASS_POS_LEFT) {
            infoRB = INFO_TR;
            compassRB = COMPASS_RIGHT;
        }
        if (infoRB == -1)
            return;
        if (infoFirst) {
            SurveyCapturePrefs.set(SurveyCapturePrefs.PREF_COMPASS_POS,
                    getCompassPref(compassRB));
            _rb[compassRB].setChecked(true);
        } else {
            SurveyCapturePrefs.set(SurveyCapturePrefs.PREF_INFOBOX_POS,
                    getInfoPref(infoRB));
            _rb[infoRB].setChecked(true);
        }
    }

    private int getInfoPref(int index) {
        switch (index) {
            case INFO_TR:
                return SurveyCapturePrefs.INFO_POS_TR;
            case INFO_TL:
                return SurveyCapturePrefs.INFO_POS_TL;
            case INFO_BR:
                return SurveyCapturePrefs.INFO_POS_BR;
            case INFO_BL:
                return SurveyCapturePrefs.INFO_POS_BL;
        }
        return -1;
    }

    private int getCompassPref(int index) {
        switch (index) {
            case COMPASS_LEFT:
                return SurveyCapturePrefs.COMPASS_POS_LEFT;
            case COMPASS_RIGHT:
                return SurveyCapturePrefs.COMPASS_POS_RIGHT;
        }
        return -1;
    }

    @Override
    public void notifyChange() {
        _notifyLimiter.exec();
    }

    private final OnCheckedChangeListener _checkChanged = new OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton cb, boolean checked) {
            int index = getIndex(cb);
            if (index == -1)
                return;
            if (cb instanceof RadioButton && checked) {
                // Have to re-implement radio button behavior
                // since it's now split between layouts
                if (getInfoPref(index) != -1) {
                    for (int i = 0; i < RB_COUNT; i++) {
                        if (_rb[i] != cb && getInfoPref(i) != -1)
                            _rb[i].setChecked(false);
                    }
                }
                rbChecked(index);
            } else if (cb instanceof CheckBox)
                cbChecked(index, checked);
        }
    };

    private int getIndex(CompoundButton cb) {
        for (int i = 0; i < RB_COUNT; i++) {
            if (cb == _rb[i])
                return i;
        }
        for (int i = 0; i < CB_COUNT; i++) {
            if (cb == _cb[i])
                return i;
        }
        return -1;
    }

    private int getIndex(Button btn) {
        for (int i = 0; i < CB_COUNT; i++) {
            if (_cbBtn[i] == btn)
                return i;
        }
        return -1;
    }

    private void rbChecked(int index) {
        if (getInfoPref(index) != -1) {
            SurveyCapturePrefs.set(SurveyCapturePrefs.PREF_INFOBOX_POS,
                    getInfoPref(index));
            fixOverlap(true);
        } else if (getCompassPref(index) != -1) {
            SurveyCapturePrefs.set(SurveyCapturePrefs.PREF_COMPASS_POS,
                    getCompassPref(index));
            fixOverlap(false);
        }
        notifyChange();
    }

    private void cbChecked(int index, boolean checked) {
        if (index == SHOW_IMAGERY)
            CapturePrefs.set(CapturePrefs.PREF_SHOW_IMAGERY, checked);
        else {
            if (_cbBtn[index] != null)
                _cbBtn[index].setEnabled(checked);
            VizPrefs.set(getCBPrefIndex(index), checked);
        }
        notifyChange();
    }

    private int getCBPrefIndex(int index) {
        switch (index) {
            default:
            case SHOW_INFOBOX:
                return VizPrefs.IMGCAP_INFO;
            case SHOW_ARROWS:
                return VizPrefs.IMGCAP_ARROWS;
            case SHOW_SCALE:
                return VizPrefs.IMGCAP_SCALE;
            case SHOW_HEADINGS:
                return VizPrefs.IMGCAP_HEADINGS;
            case SHOW_DIMENSIONS:
                return VizPrefs.IMGCAP_DIMENSIONS;
        }
    }

    @Override
    protected String getETPrefIndex(int index) {
        switch (index) {
            default:
            case FONT_SIZE:
                return CapturePrefs.PREF_FONT_SIZE;
            case LINE_WEIGHT:
                return CapturePrefs.PREF_LINE_WEIGHT;
            case ICON_SIZE:
                return CapturePrefs.PREF_ICON_SIZE;
            case LABEL_SIZE:
                return CapturePrefs.PREF_LABEL_SIZE;
        }
    }

    @Override
    public void updateCurrentSurveyHandle(String newUID, String newName,
            String newType) {
    }

    @Override
    protected int getLayoutId() {
        return R.layout.survey_capture_pp_dialog;
    }

    @Override
    protected Context getLayoutContext() {
        return ATSKApplication.getInstance().getPluginContext();
    }

    public void setOnSaveListener(Runnable r) {
        _onSave = r;
    }

    public void setOnRedoListener(Runnable r) {
        _onRedo = r;
    }

    public void setOnChangedListener(Runnable r) {
        _onChanged = r;
    }

    public void setSurvey(SurveyData survey) {
        _survey = survey;

        // Apply changes
        if (_created) {
            // Only HLZ uses heading text
            _cb[SHOW_HEADINGS].setVisibility(_survey.getType() !=
                    AZ_TYPE.HLZ && _survey.getType() != AZ_TYPE.DZ
                    ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        _vizLayout.dispose();
    }

    @Override
    protected void beginSetup() {
        if (_created) {
            setSurvey(_survey);
            _loader.setVisibility(View.GONE);
            _container.setVisibility(View.VISIBLE);
            _imgView.setVisibility(View.VISIBLE);
            _imgView.setImageBitmap(_bmp);
            loadFinished();
        }
    }

    @Override
    protected Bitmap loadBitmap() {
        return _bmp;
    }

    @Override
    public void loadFinished() {
        _title.setText(R.string.imgcap_output_title);
        _container.setVisibility(View.VISIBLE);
    }

    @Override
    public void onClick(View v) {
        if (v instanceof Button) {
            switch (getIndex((Button) v)) {
                case SHOW_INFOBOX:
                    showInfoEditor();
                    break;
                case SHOW_SCALE:
                    showScaleEditor();
                    break;
            }
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s,
            int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s,
            int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        try {
            for (int i = 0; i < ET_COUNT; i++)
                CapturePrefs.set(getETPrefIndex(i), Integer.parseInt(
                        _et[i].getText().toString()));
            notifyChange();
        } catch (NumberFormatException e) {
        }
    }

    /**
     * Edit lines shown in info box
     */
    public void showInfoEditor() {
        if (_postDraw == null) {
            toast("Failed to open info editor.");
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(getLayoutContext());
        View v = inflater.inflate(R.layout.survey_capture_edit_info, null);
        final EditText name = (EditText) v.findViewById(
                R.id.edit_info_survey_name);
        final EditText loc = (EditText) v.findViewById(
                R.id.edit_info_survey_location);
        final EditText date = (EditText) v.findViewById(
                R.id.edit_info_survey_date);

        name.setText(_postDraw.getTitle());
        loc.setText(_postDraw.getLocation());
        date.setText(_postDraw.getDateStamp());

        AlertDialog.Builder adb = new AlertDialog.Builder(_context);
        adb.setTitle("Edit Info");
        adb.setView(v);
        adb.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                _postDraw.setInfo(name.getText().toString(),
                        loc.getText().toString(), date.getText().toString());
                notifyChange();
            }
        });
        adb.setNegativeButton("Cancel", null);
        adb.show();
    }

    /**
     * Edit lines shown in info box
     */
    public void showScaleEditor() {
        if (_postDraw == null) {
            toast("Failed to open scale editor.");
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(getLayoutContext());
        View v = inflater.inflate(
                R.layout.survey_capture_edit_mapscale, null);
        final EditText scale = (EditText) v.findViewById(R.id.edit_map_scale);
        final EditText segs = (EditText) v
                .findViewById(R.id.edit_map_scale_segs);
        final TextView scaleUnit = (TextView) v
                .findViewById(R.id.edit_map_scale_unit);
        final TextView segsUnit = (TextView) v
                .findViewById(R.id.edit_map_scale_segs_unit);
        scale.setText(String.format(LocaleUtil.getCurrent(), "%.0f",
                _postDraw.getMapScaleDisplay()));
        segs.setText(String.format(LocaleUtil.getCurrent(), "%.0f",
                _postDraw.getMapScaleSegment()));

        Unit dispUnit = _postDraw.getDisplayUnit() == Span.YARD ? Unit.YARD
                : Unit.FOOT;
        scaleUnit.setText(String.format(LocaleUtil.getCurrent(), "/ %.0f %s",
                _postDraw.getMapRange(), dispUnit.getAbbr()));
        segsUnit.setText(dispUnit.getAbbr());

        AlertDialog.Builder adb = new AlertDialog.Builder(_context);
        adb.setTitle("Edit Scale Bar");
        adb.setView(v);
        adb.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    _postDraw.setMapScale(
                            Double.parseDouble(scale.getText().toString()),
                            Double.parseDouble(segs.getText().toString()));
                    notifyChange();
                } catch (Exception e) {
                    toast("Failed to read map scale input.");
                }
            }
        });
        adb.setNegativeButton("Cancel", null);
        adb.show();
    }

    private void showVizManager(boolean show) {
        if (_vizLayout == null)
            return;
        _scrollView.setVisibility(show ? View.GONE : View.VISIBLE);
        _extraView.setVisibility(show ? View.VISIBLE : View.GONE);
        VizController cont = _vizLayout.getVisibilityController();
        if (cont != null)
            cont.setOnChangedListener(show ? _vizChanged : null);
    }

    private void toast(String txt) {
        Toast.makeText(_context, txt, Toast.LENGTH_LONG).show();
    }

    private final Runnable _vizChanged = new Runnable() {
        @Override
        public void run() {
            notifyChange();
        }
    };

    private final LimitingThread _notifyLimiter =
            new LimitingThread("ATSK_CaptureNotifyLimiter", new Runnable() {
                @Override
                public void run() {
                    if (_created) {
                        SurveyCaptureDialog.super.notifyChange();
                        try {
                            Thread.sleep(500);
                        } catch (Exception e) {
                        }
                    }
                }
            });
}
