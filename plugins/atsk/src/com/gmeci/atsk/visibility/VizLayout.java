
package com.gmeci.atsk.visibility;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ShapeDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Spinner;

import com.atakmap.android.atsk.plugin.R;
import com.atakmap.android.gridlines.GridLinesMapComponent;
import com.atakmap.android.gui.ColorPalette;
import com.atakmap.android.imagecapture.GridToolbar;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.toolbar.ToolbarBroadcastReceiver;
import com.atakmap.coremap.log.Log;
import com.atakmap.math.MathUtils;
import com.gmeci.atsk.ATSKATAKConstants;
import com.gmeci.atsk.az.currentsurvey.CurrentSurveyFragment;
import com.gmeci.atsk.resources.ATSKApplication;
import com.gmeci.atsk.toolbar.ATSKToolbar;
import com.gmeci.atsk.toolbar.ATSKToolbarComponent;
import com.gmeci.atskservice.resolvers.AZProviderClient;
import com.gmeci.constants.Constants;
import com.gmeci.core.SurveyData;
import com.gmeci.core.SurveyData.AZ_TYPE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VizLayout {

    private static final String TAG = "VizLayout";

    public static final int LZ_SECTION = 0;
    public static final int HLZ_SECTION = 1;
    public static final int FARP_SECTION = 2;
    public static final int DZ_SECTION = 3;
    public static final int AZ_SECTIONS = 4;

    private SurveyData _survey;
    private CurrentSurveyFragment _surveyFrag;
    private VizController _vizController;
    private MapSelectToolbar _selectTool;
    private MapView _mapView;
    private MapGroup _obsGroup;
    private MapGroup _azGroup;
    private View _root;
    private ScrollView _toggleView, _colorsView;
    private View _toolView;
    private final int[] _cbIds = new int[VizPrefs.CB_COUNT];
    private final int[] _colIds = new int[VizPrefs.CB_COUNT];
    private final CheckBox[] _cb = new CheckBox[VizPrefs.CB_COUNT];
    private Button _backBtn, _dropGrid;
    private GridToolbar _gridTool;
    private Spinner _ampSpinner;
    private ArrayAdapter<String> _ampAdapter;
    private Button[] _colBtn = new Button[VizPrefs.CB_COUNT];
    private final RadioGroup[] _rg = new RadioGroup[VizPrefs.RG_COUNT];
    private final LinearLayout[] _sections = new LinearLayout[AZ_SECTIONS];
    private final LinearLayout[] _colSections = new LinearLayout[AZ_SECTIONS];
    private boolean _created = false;
    private boolean _scrollCreated = false;
    private boolean _syncing = false;
    private boolean _paused = true;

    public VizLayout() {
        this(null);
    }

    public VizLayout(ViewGroup container) {
        _created = false;
        _scrollCreated = false;
        Context pluginContext = ATSKApplication.getInstance()
                .getPluginContext();
        _root = LayoutInflater.from(pluginContext).inflate(
                R.layout.visibility_main, container,
                false);
    }

    public View getRootView() {
        return _root;
    }

    /**
     * Call this after the view is instantiated
     * Usually this is right away, for fragments it's in onViewCreated
     */
    public void init() {
        _toggleView = (ScrollView) _root.findViewById(R.id.viz_toggles);
        _toggleView.getViewTreeObserver().addOnGlobalLayoutListener(
                new OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        SharedPreferences prefs = VizPrefs.getPrefs();
                        if (_scrollCreated || prefs == null)
                            return;
                        // Set scrollbar to saved position
                        int bottom = _toggleView.getBottom();
                        int scroll = MathUtils.clamp(
                                prefs.getInt(VizPrefs.PREF_SCROLL, 0), 0,
                                bottom);
                        _toggleView.setScrollY(scroll);
                        _scrollCreated = true;
                    }
                });

        _colorsView = (ScrollView) _root.findViewById(R.id.viz_colors);
        _colorsView.setVisibility(View.GONE);

        _toolView = _root.findViewById(R.id.viz_tools);

        final ATSKToolbar toolbar = ATSKToolbarComponent.getToolbar();

        // Start map select tool
        final Button mapSelect = (Button) _root
                .findViewById(R.id.viz_map_select);
        mapSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (_selectTool == null)
                    _selectTool = new MapSelectToolbar(_mapView,
                            new MapSelectTool(_mapView, mapSelect, _obsGroup));
                if (toolbar.isActive(_selectTool))
                    toolbar.closeToolbar(_selectTool);
                else
                    toolbar.setToolbar(_selectTool);
            }
        });

        _dropGrid = (Button) _root.findViewById(R.id.viz_drop_grid);
        _dropGrid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (_gridTool == null)
                    _gridTool = new GridToolbar(_mapView, _dropGrid);
                Intent toolbar = new Intent();
                toolbar.setAction(ToolbarBroadcastReceiver.OPEN_TOOLBAR);
                toolbar.putExtra("toolbar", GridToolbar.IDENTIFIER);
                AtakBroadcast.getInstance().sendBroadcast(toolbar);
                ATSKToolbar.droppedGrid = true;
            }
        });
        _dropGrid.setText(GridLinesMapComponent.getCustomGrid()
                .isValid() ? R.string.edit_grid : R.string.drop_grid);

        Button colorSelect = (Button) _root
                .findViewById(R.id.line_colors_btn);
        colorSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                _toggleView.setVisibility(View.GONE);
                _colorsView.setVisibility(View.VISIBLE);
                _backBtn.setVisibility(View.VISIBLE);
            }
        });

        Button colorReset = (Button) _root.findViewById(R.id.col_reset);
        colorReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                VizPrefs.resetColors();
                for (int i = 0; i < VizPrefs.CB_COUNT; i++) {
                    if (_colBtn[i] != null && _vizController != null) {
                        updateColorButton(i);
                        _vizController.setItemColor(i,
                                _vizController.getItemColor(i));
                    }
                }
            }
        });

        _backBtn = (Button) _root.findViewById(R.id.viz_back);
        _backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                _toggleView.setVisibility(View.VISIBLE);
                _colorsView.setVisibility(View.GONE);
                _backBtn.setVisibility(View.GONE);
            }
        });

        // Aircraft marking patterns spinner
        List<String> ampList = new ArrayList<String>();
        ampList.addAll(Arrays.asList(Constants.LZ_AMPS));
        _ampAdapter = new ArrayAdapter<String>(ATSKApplication
                .getInstance().getPluginContext(),
                R.layout.spinner_row_small, R.id.type, ampList);
        _ampSpinner = (Spinner) _root.findViewById(R.id.viz_lz_amps_spinner);
        _ampSpinner.setAdapter(_ampAdapter);
        _ampSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent, View view,
                            int position, long id) {
                        setSurveyAMP(_ampAdapter.getItem(position));
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                });

        for (int i = 0; i < VizPrefs.CB_COUNT; i++)
            _cbIds[i] = -1;

        // Obstructions
        _cbIds[VizPrefs.ALL_OBS] = R.id.viz_all_obs;
        _cbIds[VizPrefs.POINTS] = R.id.viz_points_obs;
        _cbIds[VizPrefs.ROUTES] = R.id.viz_routes_obs;
        _cbIds[VizPrefs.AREAS] = R.id.viz_areas_obs;
        _cbIds[VizPrefs.VEHICLES] = R.id.viz_vehicles_obs;
        _cbIds[VizPrefs.OBS_SHADING] = R.id.viz_shading_obs;
        _cbIds[VizPrefs.RANGE_AND_BEARING] = R.id.viz_rab;
        _cbIds[VizPrefs.GALLERY_ICONS] = R.id.viz_gallery_icons;
        _cbIds[VizPrefs.SURVEY] = R.id.viz_survey;

        // LZ pieces
        _sections[LZ_SECTION] = (LinearLayout) _toggleView
                .findViewById(R.id.viz_lz_categories);
        _colSections[LZ_SECTION] = (LinearLayout) _colorsView
                .findViewById(R.id.col_lz_categories);
        _cbIds[VizPrefs.LZ_MAIN_OUTLINE] = R.id.viz_lz_main_outline;
        _cbIds[VizPrefs.LZ_MAIN_SHADING] = R.id.viz_lz_main_shading;
        _cbIds[VizPrefs.LZ_CENTER_LINE] = R.id.viz_lz_center_line;
        _cbIds[VizPrefs.LZ_THRESHOLD_LINES] = R.id.viz_lz_threshold_lines;
        _cbIds[VizPrefs.LZ_DCPS] = R.id.viz_lz_dcps;
        _cbIds[VizPrefs.LZ_AMPS] = R.id.viz_lz_amps;
        _cbIds[VizPrefs.LZ_MIN_GTMS] = R.id.viz_lz_min_gtms;
        _cbIds[VizPrefs.LZ_MAX_GTMS] = R.id.viz_lz_max_gtms;
        _cbIds[VizPrefs.LZ_ANCHORS] = R.id.viz_lz_anchor;
        _cbIds[VizPrefs.LZ_SHOULDERS] = R.id.viz_lz_shoulders;
        _cbIds[VizPrefs.LZ_OVERRUNS] = R.id.viz_lz_overruns;
        _cbIds[VizPrefs.LZ_OVERRUNS_HATCHING] = R.id.viz_lz_overruns_hatching;
        _cbIds[VizPrefs.LZ_CLEAR_ZONES] = R.id.viz_lz_clear_zones;
        _cbIds[VizPrefs.LZ_APPROACHES] = R.id.viz_lz_approaches;

        _colIds[VizPrefs.LZ_MAIN_OUTLINE] = R.id.col_lz_main_outline;
        _colIds[VizPrefs.LZ_INVALID_OUTLINE] = R.id.col_lz_invalid_outline;
        _colIds[VizPrefs.LZ_CENTER_LINE] = R.id.col_lz_center_line;
        _colIds[VizPrefs.LZ_SHOULDERS] = R.id.col_lz_shoulders;
        _colIds[VizPrefs.LZ_OVERRUNS] = R.id.col_lz_overruns;
        _colIds[VizPrefs.LZ_OVERRUNS_HATCHING] = R.id.col_lz_overruns_hatching;
        _colIds[VizPrefs.LZ_CLEAR_ZONES] = R.id.col_lz_clear_zones;
        _colIds[VizPrefs.LZ_APPROACHES] = R.id.col_lz_approaches;

        // HLZ pieces
        _sections[HLZ_SECTION] = (LinearLayout) _root
                .findViewById(R.id.viz_hlz_categories);
        _colSections[HLZ_SECTION] = (LinearLayout) _colorsView
                .findViewById(R.id.col_hlz_categories);
        _cbIds[VizPrefs.HLZ_MAIN_OUTLINE] = R.id.viz_hlz_main;
        _cbIds[VizPrefs.HLZ_MAIN_SHADING] = R.id.viz_hlz_main_shading;
        _cbIds[VizPrefs.HLZ_ANCHORS] = R.id.viz_hlz_anchors;
        _cbIds[VizPrefs.HLZ_APPROACH_LINE] = R.id.viz_hlz_approach_line;
        _cbIds[VizPrefs.HLZ_DEPARTURE_LINE] = R.id.viz_hlz_departure_line;

        _colIds[VizPrefs.HLZ_MAIN_OUTLINE] = R.id.col_hlz_main;
        _colIds[VizPrefs.HLZ_APPROACH_LINE] = R.id.col_hlz_approach_line;
        _colIds[VizPrefs.HLZ_DEPARTURE_LINE] = R.id.col_hlz_departure_line;

        // FARP pieces
        _sections[FARP_SECTION] = (LinearLayout) _root
                .findViewById(R.id.viz_farp_categories);
        _colSections[FARP_SECTION] = (LinearLayout) _colorsView
                .findViewById(R.id.col_farp_categories);
        _cbIds[VizPrefs.FARP_AIRCRAFT] = R.id.viz_farp_aircraft;
        _cbIds[VizPrefs.FARP_AIRCRAFT_SHADING] = R.id.viz_farp_aircraft_shading;
        _cbIds[VizPrefs.FARP_CART] = R.id.viz_farp_cart;
        _cbIds[VizPrefs.FARP_LEFT_LINES] = R.id.viz_farp_left_lines;
        _cbIds[VizPrefs.FARP_RIGHT_LINES] = R.id.viz_farp_right_lines;
        _cbIds[VizPrefs.FARP_ITEMS] = R.id.viz_farp_items;

        _colIds[VizPrefs.FARP_AIRCRAFT] = R.id.col_farp_aircraft;
        _colIds[VizPrefs.FARP_CART] = R.id.col_farp_cart;
        _colIds[VizPrefs.FARP_LEFT_LINES] = R.id.col_farp_left_lines;

        // DZ pieces
        _sections[DZ_SECTION] = (LinearLayout) _root
                .findViewById(R.id.viz_dz_categories);
        _colSections[DZ_SECTION] = (LinearLayout) _colorsView
                .findViewById(R.id.col_dz_categories);
        _cbIds[VizPrefs.DZ_MAIN_OUTLINE] = R.id.viz_dz_main;
        _cbIds[VizPrefs.DZ_MAIN_SHADING] = R.id.viz_dz_main_shading;
        _cbIds[VizPrefs.DZ_CENTER] = R.id.viz_dz_center;
        _cbIds[VizPrefs.DZ_ANCHORS] = R.id.viz_dz_anchors;
        _cbIds[VizPrefs.DZ_HEADING_LINE] = R.id.viz_dz_heading_line;
        _cbIds[VizPrefs.DZ_PO] = R.id.viz_dz_po;
        _cbIds[VizPrefs.DZ_PER] = R.id.viz_dz_per;
        _cbIds[VizPrefs.DZ_HE] = R.id.viz_dz_he;
        _cbIds[VizPrefs.DZ_CDS] = R.id.viz_dz_cds;

        _colIds[VizPrefs.DZ_MAIN_OUTLINE] = R.id.col_dz_main;
        _colIds[VizPrefs.DZ_HEADING_LINE] = R.id.col_dz_heading_line;

        // Anchor point sizes
        _rg[VizPrefs.LZ_ANCHOR_SIZES] = (RadioGroup) _root
                .findViewById(R.id.viz_lz_anchor_sizes);
        _rg[VizPrefs.HLZ_ANCHOR_SIZES] = (RadioGroup) _root
                .findViewById(R.id.viz_hlz_anchor_sizes);
        _rg[VizPrefs.DZ_ANCHOR_SIZES] = (RadioGroup) _root
                .findViewById(R.id.viz_dz_anchor_sizes);

        _created = true;

        // Check box listeners
        for (int i = 0; i < VizPrefs.CB_COUNT; i++) {
            if (_cbIds[i] != -1)
                _cb[i] = (CheckBox) _toggleView.findViewById(_cbIds[i]);
            if (_cb[i] == null) {
                Log.e(TAG, "Checkbox undefined: " + i
                        + " (" + VizPrefs.getKey(i) + ")");
                continue;
            }
            _cb[i].setOnCheckedChangeListener(_cbListener);
        }

        // Color button listeners
        for (int i = 0; i < VizPrefs.CB_COUNT; i++) {
            if (_colIds[i] != -1)
                _colBtn[i] = (Button) _colorsView.findViewById(_colIds[i]);
            if (_colBtn[i] != null)
                _colBtn[i].setOnClickListener(_colListener);
        }

        // Radio group listeners
        for (int i = 0; i < VizPrefs.RG_COUNT; i++)
            _rg[i].setOnCheckedChangeListener(_rgListener);
    }

    /**
     * Toggle the tool buttons (drop grid and remove items)
     * @param show True to show the tool buttons
     */
    public void showTools(boolean show) {
        if (_toolView != null)
            _toolView.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    /**
     * Call when activity is resumed
     */
    public void resume() {
        _paused = false;
        if (_surveyFrag != null)
            setSurvey(_surveyFrag);
        syncPrefs();
    }

    /**
     * Call when activity is paused
     */
    public void pause() {
        _paused = true;
    }

    /**
     * Call when window/fragment is closed
     */
    public void dispose() {
        SharedPreferences prefs = VizPrefs.getPrefs();
        if (prefs != null && _toggleView != null) {
            int scroll = _toggleView.getScrollY();
            prefs.edit().putInt(VizPrefs.PREF_SCROLL, scroll).apply();
        }
        _created = false;
    }

    // Set survey fragment used to initialize view
    public void setSurvey(CurrentSurveyFragment surveyFrag) {
        if (_mapView == null)
            return;
        _surveyFrag = surveyFrag;
        AZProviderClient azpc = new AZProviderClient(_mapView.getContext());
        if (azpc.Start()) {
            SurveyData survey = azpc.getAZ(surveyFrag.getCurrentSurveyUID(),
                    false);
            azpc.Stop();
            setSurvey(survey);
        } else {
            Log.w(TAG, "AZPC could not be started.");
        }
    }

    // Set survey data
    public void setSurvey(SurveyData survey) {
        if (survey == null) {
            Log.w(TAG, "Attempted to set current survey to null.");
            return;
        }
        if (_survey != survey) {
            _survey = survey;
            _vizController = new VizController(_mapView, _survey);
            if (_created) {
                AZ_TYPE type = _survey.getType();
                for (int i = 0; i < AZ_SECTIONS; i++) {
                    _sections[i].setVisibility(View.GONE);
                    _colSections[i].setVisibility(View.GONE);
                }
                int sect;
                switch (type) {
                    default:
                    case LZ:
                        sect = LZ_SECTION;
                        break;
                    case HLZ:
                        sect = HLZ_SECTION;
                        break;
                    case DZ:
                        sect = DZ_SECTION;
                        break;
                    case FARP:
                        sect = FARP_SECTION;
                        break;
                }
                _sections[sect].setVisibility(View.VISIBLE);
                _colSections[sect].setVisibility(View.VISIBLE);

                String ampType = _survey.getMetaString(
                        "ampType", Constants.AMP_2_DAY);
                _ampSpinner.setSelection(_ampAdapter.getPosition(ampType));
            }
        }
    }

    /**
     * Set the current survey's aircraft marking pattern
     * @param ampType Aicraft marking pattern type
     */
    private void setSurveyAMP(String ampType) {
        if (_survey != null && _mapView != null) {
            String old = _survey.getMetaString("ampType", "");
            if (!old.equals(ampType)) {
                _survey.setMetaString("ampType", ampType);
                AZProviderClient azpc = new AZProviderClient(
                        _mapView.getContext());
                if (azpc.Start()) {
                    azpc.UpdateAZ(_survey, "AMP", true);
                    azpc.Stop();
                }
            }
        }
    }

    // Set map view and groups used to control visibility of map items
    public boolean setMapView(MapView mapView) {
        if (mapView == null)
            mapView = MapView.getMapView();
        if (_mapView == mapView)
            return true;
        _mapView = mapView;
        _vizController = new VizController(_mapView, _survey);
        _obsGroup = _mapView.getRootGroup()
                .findMapGroup(ATSKATAKConstants.ATSK_MAP_GROUP_OBS);
        _azGroup = _mapView.getRootGroup()
                .findMapGroup(ATSKATAKConstants.ATSK_MAP_GROUP_AZ);
        if (_obsGroup == null || _azGroup == null) {
            /*Log.w(TAG, "Failed to find ATSK map groups!"
                    +"\nWill retry on next preference update...");*/
            _mapView = null;
            return false;
        }
        return true;
    }

    public VizController getVisibilityController() {
        return _vizController;
    }

    private final OnCheckedChangeListener _cbListener = new OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView,
                boolean isChecked) {
            if (!_paused && !_syncing)
                setChecked(getIndex(buttonView), isChecked);
        }
    };

    private final View.OnClickListener _colListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            openColorPrompt(v);
        }
    };

    private final RadioGroup.OnCheckedChangeListener _rgListener = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            if (!_paused && !_syncing) {
                rbChecked(getGroupIndex(group), getIndex(group, checkedId));
            }
        }
    };

    // Get check box index
    private int getIndex(CompoundButton v) {
        for (int i = 0; i < VizPrefs.CB_COUNT; i++) {
            if (_cb[i] == v)
                return i;
        }
        return 0;
    }

    // Get group index based on radio group view
    private int getGroupIndex(RadioGroup group) {
        for (int i = 0; i < VizPrefs.RG_COUNT; i++) {
            if (_rg[i] == group)
                return i;
        }
        return 0;
    }

    // Get color button index
    private int getColIndex(View v) {
        for (int i = 0; i < VizPrefs.CB_COUNT; i++) {
            if (_colBtn[i] == v)
                return i;
        }
        return 0;
    }

    // Get currently selected radio button index
    private static int getIndex(RadioGroup group) {
        return getIndex(group, group.getCheckedRadioButtonId());
    }

    // Get radio button index based on id
    private static int getIndex(RadioGroup group, int id) {
        int index = 0;
        for (int i = 0; i < group.getChildCount(); i++) {
            View v = group.getChildAt(i);
            if (v instanceof RadioButton) {
                if (id == v.getId())
                    break;
                index++;
            }
        }
        return index;
    }

    // Reverse of above
    private static int getRadioButtonId(RadioGroup group, int index) {
        int id = -1, curIndex = 0;
        for (int i = 0; i < group.getChildCount(); i++) {
            View v = group.getChildAt(i);
            if (v instanceof RadioButton) {
                if (curIndex == index) {
                    id = v.getId();
                    break;
                }
                curIndex++;
            }
        }
        return id;
    }

    // Set checkbox checked and apply visibility settings to map items
    private void setChecked(int index, boolean checked) {
        if (_survey == null || index < 0 || index >= _cb.length)
            return;

        if (_created && index == VizPrefs.LZ_AMPS)
            _ampSpinner.setVisibility(checked ? View.VISIBLE : View.GONE);

        if (!setMapView(_mapView))
            return;

        if (_created && _cb[index] != null && checked != _cb[index].isChecked()) {
            // Ignore pieces that aren't part of this survey type
            AZ_TYPE type = VizPrefs.getAZType(index);
            if (!_syncing && type != null && type != _survey.getType())
                return;
            // Let onCheckedChanged handle flow of operation
            _cb[index].setChecked(checked);
            if (!_syncing)
                return;
        }

        if (!_syncing) {
            // Update preference value
            VizPrefs.set(index, checked);
            // Update map item visibility
            _vizController.setVisible(index, checked);

            _syncing = true;
            for (int i = 0; i < VizPrefs.CB_COUNT; i++)
                setChecked(i, VizPrefs.get(i));
            _syncing = false;
        }
    }

    // Radio button has been checked or should be checked
    private void rbChecked(int group, int index) {
        if (_survey == null || group < 0 || group >= _rg.length)
            return;

        if (!setMapView(_mapView))
            return;

        index = MathUtils.clamp(index,
                VizPrefs.ANCHOR_LARGE, VizPrefs.ANCHOR_SMALL);

        // Check button if not already
        if (_created && getIndex(_rg[group]) != index) {
            _rg[group].check(getRadioButtonId(_rg[group], index));
            if (!_syncing)
                return;
        }

        VizPrefs.set(group, index);
        _vizController.setSize(group, index);
    }

    /**
     * Read in visibility preferences and apply them to the layout
     */
    public void syncPrefs() {
        _vizController.syncPrefs();
        _syncing = true;
        // Save preferences
        boolean[] viz = new boolean[VizPrefs.CB_COUNT];
        for (int i = 0; i < VizPrefs.CB_COUNT; i++)
            viz[i] = VizPrefs.get(i);

        // Set checkboxes
        for (int i = 0; i < VizPrefs.CB_COUNT; i++)
            setChecked(i, viz[i]);

        // Set radio buttons
        for (int i = 0; i < VizPrefs.RG_COUNT; i++)
            rbChecked(i, VizPrefs.getRG(i));

        // Set color buttons
        for (int i = 0; i < VizPrefs.CB_COUNT; i++) {
            if (_colBtn[i] != null)
                updateColorButton(i);
        }

        _syncing = false;
    }

    private void openColorPrompt(View v) {
        if (!(v instanceof Button) || _mapView == null)
            return;
        final int index = getColIndex(v);
        Context ctx = _mapView.getContext();
        AlertDialog.Builder adb = new AlertDialog.Builder(ctx);
        adb.setTitle(_colBtn[index].getText() + " Color");
        ColorPalette palette = new ColorPalette(ctx,
                _vizController.getItemColor(index));
        adb.setView(palette);
        final AlertDialog alert = adb.create();
        alert.show();
        palette.setOnColorSelectedListener(new ColorPalette.OnColorSelectedListener() {
            @Override
            public void onColorSelected(int color, String label) {
                _vizController.setItemColor(index, color);
                VizPrefs.setColor(index, color);
                updateColorButton(index);
                alert.dismiss();
            }
        });
    }

    private void updateColorButton(int index) {
        ShapeDrawable sd = ATSKApplication.getColorRect(
                _vizController.getItemColor(index));
        _colBtn[index].setCompoundDrawables(sd, null, null, null);
    }
}
