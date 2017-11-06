
package com.gmeci.atsk.obstructions.obstruction;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnLongClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.atakmap.android.atsk.plugin.R;
import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.maps.Polyline;
import com.atakmap.android.toolbar.widgets.TextContainer;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.conversion.EGM96;
import com.atakmap.coremap.maps.coords.Altitude;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.gmeci.atsk.ATSKATAKConstants;
import com.gmeci.atsk.ATSKFragmentManager;
import com.gmeci.atsk.ATSKMapComponent;
import com.gmeci.atsk.MapHelper;
import com.gmeci.atsk.map.ATSKLabel;
import com.gmeci.atsk.obstructions.ObstructionToolbar;
import com.gmeci.atsk.resources.LCRButton;
import com.gmeci.atsk.toolbar.ATSKBaseToolbar;
import com.gmeci.atsk.toolbar.ATSKToolbar;
import com.gmeci.atsk.toolbar.ATSKToolbarComponent;
import com.gmeci.core.ATSKConstants;
import com.gmeci.core.ATSKIntentConstants;
import com.gmeci.core.LineObstruction;
import com.gmeci.core.PointObstruction;
import com.gmeci.core.SurveyPoint;
import com.gmeci.core.SurveyPoint.CollectionMethod;
import com.gmeci.atsk.obstructions.FilteredObstructionTypeSpinner;
import com.gmeci.atsk.resources.ATSKApplication;
import com.gmeci.atsk.resources.ATSKBaseFragment;
import com.gmeci.atsk.resources.ATSKDialogManager;
import com.gmeci.atsk.resources.ATSKDialogManager.DialogUpdateInterface;
import com.gmeci.atsk.resources.CoordinateHandJamDialog;
import com.gmeci.atsk.resources.CoordinateHandJamDialog.HandJamInterface;
import com.gmeci.constants.Constants;
import com.gmeci.conversions.Conversions;
import com.gmeci.conversions.Conversions.Unit;
import com.gmeci.helpers.ObstructionHelper;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public abstract class ObstructionTabBase extends Fragment implements
        HandJamInterface, DialogUpdateInterface,
        ATSKToolbar.OnToolbarVisibleListener {

    private static final String TAG = "ObstructionTabBase";
    private static final String WIDTH = "width";
    private static final String LENGTH = "length";
    private static final String HEIGHT = "height";
    private static final String ANGLE_T = "angle_t";
    private static final String CE_M = "ce";
    private static final String LE_M = "le";

    protected static final int SELECTED_BG_COLOR = 0xff376d37;
    protected static final int NON_SELECTED_BG_COLOR = 0xff383838;
    protected static final double SELECTED_SIZE_MULTIPLIER = 1.25f;
    protected static final int NAME_POSITION = 0;
    protected static final int HEIGHT_POSITION = 1;
    protected static final int DIAMETER_POSITION = 2;
    protected static final int LENGTH_POSITION = 3;
    protected static final int WIDTH_POSITION = 4;
    protected static final int ROTATION_POSITION = 5;
    protected static final int ALT_POSITION = 6;
    protected static final int LOCATION_POSITION = 7;
    protected static final int FIELD_COUNT = 8;

    public FilteredObstructionTypeSpinner _typeSpinner;
    protected ObstructionTabHost parentFragment;
    protected TextView PointCountLabelTV, PointCountTV;
    protected Button _nextBtn, _undoBtn, _leaderBtn, _leaderEndBtn;
    protected RadioGroup _rabRG;
    protected View _colorLayout;
    protected ImageButton _colorBtn;
    protected RadioButton _rabPoint1, _rabPoint2;
    protected LineObstruction _leaderObs;
    protected boolean _addingLeader = false;
    protected String DisplayCoordinateFormat = Conversions.COORD_FORMAT_MGRS;
    protected String CurrentRemark;
    protected final TextView[] StaticTVs = new TextView[25];
    protected final TextView[] LiveTVs = new TextView[25];
    protected final TextView[] UnitsTV = new TextView[25];
    protected TextView _methodTV;
    protected SharedPreferences units_settings;
    protected boolean DisplayUnitsFeet = true;
    protected boolean DisplayAnglesTrue = true;
    // SharedPreferences spinner_settings;
    protected OnSharedPreferenceChangeListener listener;
    protected boolean AllowDrawTempIcon = true;
    protected boolean AllowTypeUpdate = true;
    protected int StoredPositionIndex = 0;
    protected final SurveyPoint StoredPosition = new SurveyPoint();
    protected AlertDialog RemarkAD;
    protected PointObstruction CurrentObstruction = new PointObstruction();
    protected boolean _obsPlaced = false;
    protected Polyline CurrentObsRect;
    protected int CurrentlyEditedIndex = 0;

    /**
     * Text view selected
     */
    private final OnClickListener TVClickListener = new OnClickListener() {
        public void onClick(View v) {

            // Clear everything out
            int curIndex = CurrentlyEditedIndex;
            clearSelection();

            int index = getIndex(v);
            if (index == NAME_POSITION) {
                ShowRemarkHandJamDialog(_typeSpinner.getSelectedItem()
                        .toString() + " Remarks:", CurrentRemark);

                // selecting remarks should hide the left button bar and just
                // let me hand jam the dialog
                setOBState(ATSKIntentConstants.OB_STATE_HIDDEN);
            } else if (index == curIndex && ObstructionBarVisible()) {
                // Toggle selection
                setOBState(ATSKIntentConstants.OB_STATE_HIDDEN);
            } else {
                // Select index
                selectTV(index);
                ShowWaitingText(v);
            }
        }
    };
    private final OnLongClickListener TVLongClickListener = new OnLongClickListener() {

        @Override
        public boolean onLongClick(View v) {

            // Clear everything out
            clearSelection();
            setOBState(ATSKIntentConstants.OB_STATE_HIDDEN);

            int index = CurrentlyEditedIndex = getIndex(v);
            String name = getName(index);
            switch (index) {
                case LOCATION_POSITION:
                    ShowCoordinateHandJamDialog(CurrentObstruction.lat,
                            CurrentObstruction.lon);
                    break;
                case ROTATION_POSITION:
                    ShowAngleHandJamDialog(CurrentObstruction.course_true);
                    break;
                case HEIGHT_POSITION:
                    ShowMeasurementHandJamDialog(CurrentObstruction.height,
                            name, false);
                    break;
                case DIAMETER_POSITION:
                    ShowMeasurementHandJamDialog(CurrentObstruction.width,
                            name, false);
                    break;
                case LENGTH_POSITION:
                    ShowMeasurementHandJamDialog(CurrentObstruction.length,
                            name, false);
                    break;
                case WIDTH_POSITION:
                    ShowMeasurementHandJamDialog(CurrentObstruction.width,
                            name, false);
                    break;
                case ALT_POSITION:
                    ShowMeasurementHandJamDialog(CurrentObstruction.getMSL(),
                            name, true);
                    break;
                case NAME_POSITION:
                    ShowRemarkHandJamDialog(_typeSpinner.getSelectedItem()
                            .toString() + " Remarks:", CurrentRemark);
                    break;
                default:
                    Log.d(TAG, "Missed");
            }
            Log.e(TAG, "Currently edited index:" + CurrentlyEditedIndex);
            return true;
        }
    };

    protected void selectTV(int index) {
        CurrentlyEditedIndex = index;
        StoredPositionIndex = 0;
        Log.d(TAG, "CEI cleared to " + CurrentlyEditedIndex);

        StaticTVs[index]
                .setBackgroundResource(R.drawable.background_selected_left);
        LiveTVs[index]
                .setBackgroundResource(R.drawable.background_selected_left);

        if (UnitsTV[index] != null)
            UnitsTV[index]
                    .setBackgroundResource(R.drawable.background_selected_right);
    }

    /**
     * Given a view return its matching index
     * @param v View (usually TextView)
     * @return Matching index
     */
    protected int getIndex(View v) {
        int id = v.getId();
        for (int i = 0; i < FIELD_COUNT; i++) {
            if (StaticTVs[i] != null && id == StaticTVs[i].getId()
                    || LiveTVs[i] != null && id == LiveTVs[i].getId()
                    || UnitsTV[i] != null && id == UnitsTV[i].getId())
                return i;
        }
        return -1;
    }

    protected void setVisibility(int i, int visibility) {
        if (i < LiveTVs.length) {
            if (StaticTVs[i] != null)
                StaticTVs[i].setVisibility(visibility);
            if (LiveTVs[i] != null)
                LiveTVs[i].setVisibility(visibility);
            if (UnitsTV[i] != null)
                UnitsTV[i].setVisibility(visibility);
        }
    }

    protected void clearSelection() {
        if (parentFragment != null)
            parentFragment.StopRouteCollection();
        StoredPositionIndex = 0;
        CurrentlyEditedIndex = -1;
        UpdateDisplayMeasurements();
        UpdateDisplayUnits();
    }

    protected String getName(int index) {
        switch (index) {
            case LOCATION_POSITION:
                return "Location";
            case LENGTH_POSITION:
                return "Length";
            case WIDTH_POSITION:
                return "Width";
            case HEIGHT_POSITION:
                return "Height";
            case DIAMETER_POSITION:
                return "Diameter";
            case ALT_POSITION:
                return "Base Elevation (MSL)";
        }
        return "";
    }

    public void AllowTypeUpdates() {
        new Handler().postDelayed(new Runnable() {
            public void run() {
                AllowTypeUpdate = true;
            }
        }, 1000);
    }

    public double getLRCOffset() {
        return 0;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    protected abstract String getSettingModifier();

    @Override
    public void onPause() {

        Editor editor = units_settings.edit();

        editor.putString(ATSKConstants.LAT_EXTRA + "_" + getSettingModifier(),
                String.format("%.8f", CurrentObstruction.lat));
        editor.putString(ATSKConstants.LON_EXTRA + "_" + getSettingModifier(),
                String.format("%.8f", CurrentObstruction.lon));
        editor.putString(ATSKConstants.ALT_EXTRA + "_" + getSettingModifier(),
                String.format("%.3f", CurrentObstruction.getHAE()));

        editor.putString(WIDTH + "_" + getSettingModifier(),
                String.format("%.3f", CurrentObstruction.width));
        editor.putString(LENGTH + "_" + getSettingModifier(),
                String.format("%.3f", CurrentObstruction.length));
        editor.putString(HEIGHT + "_" + getSettingModifier(),
                String.format("%.3f", CurrentObstruction.height));
        editor.putString(ANGLE_T + "_" + getSettingModifier(),
                String.format("%.3f", CurrentObstruction.course_true));
        editor.putString(CE_M + "_" + getSettingModifier(),
                String.format("%.3f", CurrentObstruction.circularError));
        editor.putString(LE_M + "_" + getSettingModifier(),
                String.format("%.3f", CurrentObstruction.linearError));

        editor.putString(ATSKConstants.COORD_FORMAT, DisplayCoordinateFormat);
        editor.apply();
        units_settings.unregisterOnSharedPreferenceChangeListener(listener);
        ATSKToolbarComponent.getToolbar().removeVisibilityListener(this);
        ClearBoundsRectangle();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();

        AllowDrawTempIcon = false;
        String UnitsString = units_settings.getString(
                ATSKConstants.UNITS_DISPLAY, ATSKConstants.UNITS_FEET);
        DisplayUnitsFeet = !UnitsString.equals(ATSKConstants.UNITS_METERS);

        listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences prefs,
                    String key) {
                if (key.equals(Constants.ACTIVE_POINT_OBSTRUCTIONS_PREFERENCE)
                        || key.equals(Constants.ACTIVE_LINE_OBSTRUCTIONS_PREFERENCE)
                        || key.equals(Constants.ACTIVE_AREA_OBSTRUCTIONS_PREFERENCE))
                    UpdateSpinnerAdapter();
            }
        };
        units_settings.registerOnSharedPreferenceChangeListener(listener);
        ATSKToolbarComponent.getToolbar().addVisibilityListener(this);

        UpdateSpinnerAdapter();

        String AngleString = units_settings.getString(
                ATSKConstants.UNITS_ANGLE, ATSKConstants.UNITS_ANGLE_MAG);

        DisplayAnglesTrue = !AngleString.equals(ATSKConstants.UNITS_ANGLE_MAG);

        resetMeasurements();
        /*try {
            CurrentObstruction.lat = Double.parseDouble(units_settings
                    .getString(ATSKConstants.LAT_EXTRA + "_"
                            + getSettingModifier(), "0"));
            CurrentObstruction.lon = Double.parseDouble(units_settings
                    .getString(ATSKConstants.LON_EXTRA + "_"
                            + getSettingModifier(), "0"));

            CurrentObstruction.setHAE(Float.parseFloat(units_settings
                    .getString(ATSKConstants.ALT_EXTRA + "_"
                            + getSettingModifier(), "0")));
            CurrentObstruction.width = Float.parseFloat(units_settings
                    .getString(WIDTH + "_" + getSettingModifier(), "0"));
            CurrentObstruction.length = Float.parseFloat(units_settings
                    .getString(LENGTH + "_" + getSettingModifier(), "0"));
            CurrentObstruction.height = Float.parseFloat(units_settings
                    .getString(HEIGHT + "_" + getSettingModifier(), "0"));
            CurrentObstruction.course_true = Float.parseFloat(units_settings
                    .getString(ANGLE_T + "_" + getSettingModifier(), "0"));
            CurrentObstruction.circularError = Float.parseFloat(units_settings
                    .getString(CE_M + "_" + getSettingModifier(), "0"));
            CurrentObstruction.linearError = Float.parseFloat(units_settings
                    .getString(LE_M + "_" + getSettingModifier(), "0"));
        } catch (NumberFormatException ex) {

        }*/
        DisplayCoordinateFormat = units_settings.getString(
                ATSKConstants.COORD_FORMAT, Conversions.COORD_FORMAT_MGRS);
        UpdateDisplayMeasurements();

        AllowDrawTempIcon = true;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SetupButtons(view);
        SetupSpinners(view);

        _undoBtn = (Button) view.findViewById(R.id.undoLastPoint);
        _nextBtn = (Button) view.findViewById(R.id.nextLine);
        _leaderBtn = (Button) view.findViewById(R.id.addLeader);
        _leaderEndBtn = (Button) view.findViewById(R.id.endLeader);
        _rabRG = (RadioGroup) view.findViewById(R.id.rab_point_group);
        _rabPoint1 = (RadioButton) view.findViewById(R.id.rab_point_tail);
        _rabPoint2 = (RadioButton) view.findViewById(R.id.rab_point_head);
        _colorLayout = view.findViewById(R.id.color_layout);
        _colorBtn = (ImageButton) view.findViewById(R.id.color_btn);
        units_settings = PreferenceManager
                .getDefaultSharedPreferences(getActivity());

        PointCountLabelTV = (TextView) view
                .findViewById(R.id.point_count_static);
        PointCountTV = (TextView) view.findViewById(R.id.point_count);

        if (_nextBtn != null) {
            _nextBtn.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    // done with the line - move to the next one...
                    parentFragment.LineComplete(true, ObstructionTabBase.this);

                    _undoBtn.setVisibility(View.GONE);
                    _nextBtn.setVisibility(View.GONE);
                    if (PointCountTV != null)
                        PointCountTV.setText("0");
                    CurrentObstruction.setSurveyPoint(0, 0);
                    CurrentObstruction.collectionMethod = null;
                    _obsPlaced = false;
                    UpdateDisplayMeasurements();
                }
            });
        }
        if (_undoBtn != null) {
            _undoBtn.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {// LOU
                    // remove single point... if no points left - hide this
                    // button??
                    if (!parentFragment.undoLastPoint())
                        _undoBtn.setVisibility(View.GONE);
                }
            });
        }
        if (_leaderBtn != null) {
            _leaderBtn.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    beginLeader();
                }
            });
        }
        if (_leaderEndBtn != null) {
            _leaderEndBtn.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    endAddLeader();
                }
            });
        }
        endAddLeader();
    }

    @Override
    public void onDestroy() {
        endAddLeader(true);
        super.onDestroy();
    }

    @Override
    public void onToolbarVisible(ATSKBaseToolbar tb, boolean v) {
        if (tb instanceof ObstructionToolbar && !v)
            stopCollection();
    }

    protected ATSKBaseFragment notifyTabHost() {
        // Find tab host
        ATSKFragmentManager fm = ATSKMapComponent.getATSKFM();
        if (fm != null) {
            ATSKBaseFragment parent = fm.getCurrentFragment();
            if (parent instanceof ObstructionTabHost)
                parentFragment = (ObstructionTabHost) parent;
            if (parent != null)
                parent.onTabCreated(this);
            return parent;
        }
        return null;
    }

    private void UpdateDisplayUnits() {
        for (int i = 0; i < UnitsTV.length; i++) {
            if (UnitsTV[i] != null) {
                if (i == ROTATION_POSITION) {
                    if (DisplayAnglesTrue) {
                        UnitsTV[i].setText(String.format("%cT",
                                ATSKConstants.DEGREE_SYMBOL));
                    } else {
                        UnitsTV[i].setText(String.format("%cM",
                                ATSKConstants.DEGREE_SYMBOL));
                    }
                } else {
                    if (DisplayUnitsFeet)
                        UnitsTV[i].setText(" ft");
                    else
                        UnitsTV[i].setText(" m");
                }

            }
        }
    }

    protected void UpdateDisplayMeasurements() {
        LiveTVs[NAME_POSITION].setText(CurrentRemark);
        UpdateDisplayUnits();

        Unit displayUnit = (DisplayUnitsFeet ? Unit.FOOT : Unit.METER);

        LiveTVs[HEIGHT_POSITION].setText(Unit.METER.format(
                CurrentObstruction.height, displayUnit));
        LiveTVs[DIAMETER_POSITION].setText(Unit.METER.format(
                CurrentObstruction.width, displayUnit));
        LiveTVs[LENGTH_POSITION].setText(Unit.METER.format(
                CurrentObstruction.length, displayUnit));
        LiveTVs[WIDTH_POSITION].setText(Unit.METER.format(
                CurrentObstruction.width, displayUnit));
        //user wants MSL
        LiveTVs[ALT_POSITION].setText(CurrentObstruction
                .getMSLAltitude().toString(displayUnit));

        for (int i = 0; i < StaticTVs.length; i++) {
            if (CurrentlyEditedIndex != i) {
                int color;
                switch (i) {
                    case NAME_POSITION:
                        color = Color.BLACK;
                        break;
                    default:
                        color = 0;
                }
                if (StaticTVs[i] != null) {
                    StaticTVs[i].setBackgroundColor(color);
                    StaticTVs[i].setPadding(0, 0, 0, 0);
                }
                if (LiveTVs[i] != null) {
                    LiveTVs[i].setBackgroundColor(color);
                    LiveTVs[i].setPadding(0, 0, 0, 0);
                }
                if (UnitsTV[i] != null) {
                    UnitsTV[i].setBackgroundColor(color);
                    UnitsTV[i].setPadding(0, 0, 0, 0);
                }
            }
        }

        double angle = Conversions.deg360(CurrentObstruction.course_true);

        if (!DisplayAnglesTrue) {
            angle = Conversions.GetMagAngle(
                    CurrentObstruction.course_true, CurrentObstruction.lat,
                    CurrentObstruction.lon);
        }

        LiveTVs[ROTATION_POSITION].setText(String.format("%.1f", angle));

        String CoordinateString = Conversions.getCoordinateString(
                CurrentObstruction.lat, CurrentObstruction.lon,
                DisplayCoordinateFormat);

        if (_methodTV != null) {
            CollectionMethod method = CurrentObstruction.collectionMethod;
            if (method != null) {
                _methodTV.setVisibility(View.VISIBLE);
                _methodTV.setText(method.name);
                _methodTV.setBackgroundColor(method.color);
            } else
                _methodTV.setVisibility(View.GONE);
        }

        LiveTVs[LOCATION_POSITION].setText(CoordinateString);

        if (this instanceof ObstructionPointFragment)
            DrawBoundsRectangle();
    }

    protected void HideShowFields(String type) {
        if (Constants.isTaxiway(type) || Constants.isFlatTerrain(type))
            setVisibility(HEIGHT_POSITION, View.GONE);
        else
            setVisibility(HEIGHT_POSITION, View.VISIBLE);
    }

    public void UpdateType(String type) {
        _rabPoint1.setChecked(true);
        resetMeasurements();
    }

    private void SetupSpinners(View myView) {

        _typeSpinner = (FilteredObstructionTypeSpinner) myView
                .findViewById(R.id.type_spinner);

        _typeSpinner.setOnTouchListener(ATSKApplication.getInstance());

        _typeSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View view,
                    int pos, long id) {
                // run the filter for what is shown - what is not

                String TypeSelected = parentView.getItemAtPosition(pos)
                        .toString();
                StoredPositionIndex = 0;
                HideShowFields(TypeSelected);
                // change the item we're drawing on the map...
                if (AllowTypeUpdate)
                    UpdateType(TypeSelected);
            }// end onItemSelected

            public void onNothingSelected(AdapterView<?> arg0) {
                Log.d(TAG, "Nothing selected");
            }
        });
        _typeSpinner.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View arg0) {
                Log.d(TAG, "long press for selection type");
                ShowTypeSelectionDialog();
                // LOU refresh the type spinner here
                return true;
            }
        });
    }// end SetupSpinners

    protected void ShowTypeSelectionDialog() {
        com.atakmap.android.ipc.AtakBroadcast.getInstance().sendBroadcast(
                new Intent("com.atakmap.app.ADVANCED_SETTINGS"));

    }

    private void SetupButtons(View view) {
        Log.d(TAG, "setting up buttons");
        for (int i = 0; i < LiveTVs.length; i++) {
            LiveTVs[i] = null;
            StaticTVs[i] = null;
        }

        LiveTVs[NAME_POSITION] = (TextView) view.findViewById(R.id.Name);
        StaticTVs[NAME_POSITION] = (TextView) view
                .findViewById(R.id.Name_static);

        StaticTVs[HEIGHT_POSITION] = (TextView) view
                .findViewById(R.id.Height_static);
        LiveTVs[HEIGHT_POSITION] = (TextView) view.findViewById(R.id.Height);

        StaticTVs[DIAMETER_POSITION] = (TextView) view
                .findViewById(R.id.Diameter_static);
        LiveTVs[DIAMETER_POSITION] = (TextView) view
                .findViewById(R.id.Diameter);

        StaticTVs[LENGTH_POSITION] = (TextView) view
                .findViewById(R.id.Length_static);
        LiveTVs[LENGTH_POSITION] = (TextView) view.findViewById(R.id.Length);

        StaticTVs[WIDTH_POSITION] = (TextView) view
                .findViewById(R.id.Width_static);
        LiveTVs[WIDTH_POSITION] = (TextView) view.findViewById(R.id.Width);

        StaticTVs[ROTATION_POSITION] = (TextView) view
                .findViewById(R.id.Rotation_static);
        LiveTVs[ROTATION_POSITION] = (TextView) view
                .findViewById(R.id.Rotation);

        StaticTVs[ALT_POSITION] = (TextView) view.findViewById(R.id.Alt_static);
        LiveTVs[ALT_POSITION] = (TextView) view.findViewById(R.id.Alt);

        StaticTVs[LOCATION_POSITION] = (TextView) view
                .findViewById(R.id.Location_static);
        LiveTVs[LOCATION_POSITION] = (TextView) view
                .findViewById(R.id.Location);
        UnitsTV[ROTATION_POSITION] = (TextView) view
                .findViewById(R.id.Rotation_units);

        _methodTV = (TextView) view.findViewById(R.id.collection_method);

        for (int i = 0; i < UnitsTV.length; i++) {
            if (UnitsTV[i] != null) {
                UnitsTV[i].setClickable(true);
                UnitsTV[i].setLongClickable(true);

                UnitsTV[i].setOnLongClickListener(TVLongClickListener);
                UnitsTV[i].setOnClickListener(TVClickListener);
            }
        }
        for (int i = 0; i < LiveTVs.length; i++) {
            if (LiveTVs[i] != null) {
                StaticTVs[i].setClickable(true);
                LiveTVs[i].setClickable(true);
                LiveTVs[i].setLongClickable(true);
                StaticTVs[i].setLongClickable(true);

                StaticTVs[i].setOnLongClickListener(TVLongClickListener);
                LiveTVs[i].setOnLongClickListener(TVLongClickListener);
                StaticTVs[i].setOnClickListener(TVClickListener);
                LiveTVs[i].setOnClickListener(TVClickListener);
            }
        }

    }

    public void setParentInterface(ObstructionTabHost obInterface) {
        parentFragment = obInterface;
    }

    protected boolean setOBState(String RequestedState) {
        return ATSKApplication.setObstructionCollectionMethod(
                RequestedState, TAG, false);
    }

    protected boolean ObstructionBarVisible() {
        return ATSKToolbarComponent.getToolbar().getActive() instanceof ObstructionToolbar;
    }

    protected boolean setCollectingTop(boolean SetTop) {
        return setCollectingTop(SetTop, false);
    }

    protected boolean setCollectingTop(boolean SetTop, boolean IgnoreDefault) {
        return ATSKApplication.setObstructionCollectionTop(
                SetTop, IgnoreDefault, TAG);
    }

    void ShowAngleHandJamDialog(double currentAngle_True) {
        ATSKDialogManager adm = new ATSKDialogManager(getActivity(), this,
                false);
        SurveyPoint AnchorPoint = new SurveyPoint(CurrentObstruction.lat,
                CurrentObstruction.lon);
        adm.ShowAngleHandJamDialog(currentAngle_True,
                AnchorPoint, CurrentlyEditedIndex);
    }

    void ShowCoordinateHandJamDialog(double currentLat, double currentLon) {
        CoordinateHandJamDialog chjd = new CoordinateHandJamDialog();
        chjd.Initialize(CurrentObstruction.lat,
                CurrentObstruction.lon, DisplayCoordinateFormat,
                CurrentObstruction.getHAE(), this);
    }

    @Override
    public void UpdateCoordinate(double lat, double lon, double elevation) {
        SurveyPoint sp = new SurveyPoint(CurrentObstruction);
        sp.lat = lat;
        sp.lon = lon;
        sp.setHAE(elevation);
        sp.collectionMethod = CollectionMethod.MANUAL;
        CurrentlyEditedIndex = LOCATION_POSITION;
        newPosition(sp, false);
        CurrentlyEditedIndex = -1;
        UpdateDisplayMeasurements();

        // Zoom to point
        MapView mv = MapView.getMapView();
        if (mv != null)
            mv.getMapController().panTo(MapHelper.
                    convertSurveyPoint2GeoPoint(sp), false);
    }

    @Override
    public void UpdateCoordinateFormat(String DisplayFormat) {
        DisplayCoordinateFormat = DisplayFormat;
        UpdateDisplayMeasurements();
    }

    void ShowMeasurementHandJamDialog(double value_m, String Name,
            boolean isElevation) {
        ATSKDialogManager adm = new ATSKDialogManager(getActivity(), this, true);
        adm.ShowMeasurementHandJamDialog(value_m, Name, CurrentlyEditedIndex);
    }

    // double CurrentLat, CurrentLon;
    // double CurrentCE_m, CurrentLE_m, CurrentAngle_True, CurrentHeight_m,
    // CurrentDiameter_m, CurrentLength_m, CurrentWidth_m, CurrentAlt_m;

    private void ShowRemarkHandJamDialog(String Type, String currentRemark) {
        final AlertDialog.Builder ad = new AlertDialog.Builder(getActivity(),
                android.R.style.Theme_Holo_Dialog);
        ad.setTitle(Type);

        final EditText input = new EditText(getActivity());
        input.setTextColor(ATSKConstants.LIGHT_BLUE);
        input.setText(currentRemark);
        input.setSingleLine();
        input.setOnKeyListener(new OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER))) {
                    RemarkOKPressed(input, RemarkAD);
                    return true;
                }
                return false;
            }

        });

        ad.setView(input);
        ad.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int arg1) {
                dialog.dismiss();
            }
        });
        ad.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                RemarkOKPressed(input, dialog);
            }
        });
        RemarkAD = ad.create();
        RemarkAD.show();

    }

    boolean newPosition(SurveyPoint sp, boolean TopCollected) {

        if (CurrentlyEditedIndex == -1)
            return false;

        Log.d(TAG, "Currently Edited Index: " + CurrentlyEditedIndex);
        switch (CurrentlyEditedIndex) {
            case ROTATION_POSITION: {
                if (StoredPositionIndex == 1) {
                    LiveTVs[ROTATION_POSITION]
                            .setText(R.string.input_waiting2);
                    UnitsTV[ROTATION_POSITION].setText("");
                    ATSKBaseFragment.UpdateNotification(getActivity(),
                            "ROTATION COLLECTION", "2 of 2",
                            "WAITING FOR 2nd ROTATION POINT", "");
                } else {
                    ATSKBaseFragment.UpdateNotification(getActivity(),
                            "ROTATION COLLECTION", "",
                            "COLLECITON COMPLETECOMPLETE", "");
                }
                break;
            }
            case HEIGHT_POSITION: {
                if (StoredPositionIndex == 1) {
                    ATSKBaseFragment.UpdateNotification(getActivity(),
                            "HEIGHT COLLECTION", "2 of 2",
                            "WAITING FOR BOTTOM ELEVATION", "");
                    LiveTVs[HEIGHT_POSITION]
                            .setText(R.string.input_waiting2);
                }
                break;
            }
            default: {
                if (StoredPositionIndex == 1) {
                    LiveTVs[CurrentlyEditedIndex]
                            .setText(R.string.input_waiting2);
                }
                break;

            }
        }
        return false;
    }

    protected boolean StoreMeasurement(double newMeasurement_m) {

        Log.e(TAG, "Currently edited index 2: " + CurrentlyEditedIndex);
        switch (CurrentlyEditedIndex) {
            case HEIGHT_POSITION:
                if (CurrentObstruction.TopCollected) {
                    CurrentObstruction.setMSL(CurrentObstruction.getMSL()
                            + CurrentObstruction.height - newMeasurement_m);
                }
                CurrentObstruction.height = newMeasurement_m;
                return true;
            case DIAMETER_POSITION:
                CurrentObstruction.length = Math.abs(newMeasurement_m);
                CurrentObstruction.width = Math.abs(newMeasurement_m);
                return true;

            case LENGTH_POSITION:
                CurrentObstruction.length = Math.abs(newMeasurement_m);
                return true;

            case WIDTH_POSITION:
                CurrentObstruction.width = Math.abs(newMeasurement_m);
                return true;

            case ALT_POSITION://we want hae
                CurrentObstruction.setMSL(newMeasurement_m);//we show only msl
                return true;
            case ROTATION_POSITION:
                CurrentObstruction.course_true = newMeasurement_m;
                return true;
        }
        return false;
    }

    /**
     * Set route width using non-center offset
     * @param width New route width
     * @param side Anchor side
     * @return True if modified, false otherwise
     */
    protected boolean setRouteWidth(double width, LCRButton.CollectionSide side) {
        if (parentFragment == null)
            return false;
        CurrentObstruction.width = width;
        LineObstruction lo = parentFragment.getCurrentLine();
        if (lo == null)
            return false;
        if (ObstructionHelper.setRouteWidth(lo, width, side.ordinal())) {
            for (SurveyPoint sp : lo.points)
                sp.setHAE(ATSKApplication.getAltitudeHAE(sp));
            parentFragment.opc.UpdateLine(lo,
                    side != LCRButton.CollectionSide.CENTER);
        }
        return true;
    }

    /**
     * Is LRF the current collection method?
     * @return True if LRF is the current collection method
     */
    protected boolean usingLRF() {
        return ATSKApplication.getCollectionState()
                .equals(ATSKIntentConstants.OB_STATE_LRF);
    }

    /**
     * Set obstruction point/vertex location
     * @param sp Survey point
     * @param top True if collecting top
     */
    protected void setLocation(SurveyPoint sp, boolean top) {
        CurrentObstruction.TopCollected = top;
        CurrentObstruction.setSurveyPoint(sp);
        StoredPositionIndex = 0;
        //if in top mode - set height from this mode also?
        if (top && SurveyPoint.Altitude.isValid(sp.getHAE()))
            CurrentObstruction.setHAE(sp.getHAE()
                    - CurrentObstruction.height);
        else
            CurrentObstruction.setHAE(sp.getHAE());
        _obsPlaced = true;
    }

    /**
     * Set obstruction point/vertex elevation
     * @param sp Survey point to read elevation from
     * @param top True if collecting top
     */
    protected void setElevation(SurveyPoint sp, boolean top) {
        CurrentObstruction.TopCollected = top;
        CurrentObstruction.collectionMethod = sp.collectionMethod;
        if (top && SurveyPoint.Altitude.isValid(sp.getHAE()))
            CurrentObstruction.setHAE(sp.getHAE()
                    - CurrentObstruction.height);
        else
            CurrentObstruction.setHAE(sp.getHAE());
    }

    /**
     * Collect height using 1- or 2-point entry
     * @param sp Survey point used to collect height
     */
    protected void collectHeight(SurveyPoint sp) {
        double hae = sp.getHAE();
        if (StoredPositionIndex == 0) {
            // Use self marker elevation as 'bottom' if available
            Marker self = MapView.getMapView().getSelfMarker();
            if (self != null && self.getPoint() != null) {
                Altitude selfElev = EGM96.getInstance().getHAE(self.getPoint());
                if (selfElev.isValid())
                    CurrentObstruction.setHAE(MapHelper
                            .convertAltitude(selfElev).getValue());
            }

            double obsElev = CurrentObstruction.alt.isValid()
                    ? CurrentObstruction.getHAE()
                    : ATSKApplication.getAltitudeHAE(sp.lat, sp.lon);

            if (ATSKApplication.collectingTop() && usingLRF()
                    && SurveyPoint.Altitude.isValid(obsElev)
                    && SurveyPoint.Altitude.isValid(hae)) {

                // Factor in LRF height preference
                double lrfHeight;
                try {
                    lrfHeight = Double
                            .parseDouble(units_settings
                                    .getString(
                                            ATSKConstants.OBSTRUCTION_METHOD_LRF2GPS_OFFSET_HEIGHT_M,
                                            "2"));
                } catch (Exception e) {
                    lrfHeight = 2;
                }

                // 1-point entry (height - elevation)
                CurrentObstruction.setHAE(obsElev - lrfHeight);
                CurrentObstruction.height = (hae - obsElev) + lrfHeight;
                UpdateDisplayMeasurements();
            } else {
                // Fallback to 2-point entry
                CurrentObstruction.setHAE(hae);
                StoredPositionIndex++;
                setCollectingTop(true);
            }
        } else {
            if (CurrentObstruction.alt.isValid()
                    && SurveyPoint.Altitude.isValid(hae))
                CurrentObstruction.height = hae
                        - CurrentObstruction.getHAE();
            else
                CurrentObstruction.height = 0;
            StoredPositionIndex = 0;
            UpdateDisplayMeasurements();
            setCollectingTop(false);
        }
    }

    /**
     * Collect other parameter using range or bearing between 2 points
     * @param sp Survey point
     */
    protected void collectStoredPos(SurveyPoint sp) {
        //these are 2 point kinda values
        if (StoredPositionIndex == 0) {
            StoredPositionIndex++;
            StoredPosition.setSurveyPoint(sp);
        } else {
            StoredPositionIndex = 0;
            double[] ra = Conversions.CalculateRangeAngle(
                    StoredPosition.lat, StoredPosition.lon, sp.lat, sp.lon);
            StoreMeasurement(CurrentlyEditedIndex
                == ROTATION_POSITION ? ra[1] : ra[0]);
            UpdateDisplayMeasurements();
        }
    }

    public void HideNextLineButton() {
        if (_nextBtn != null)
            _nextBtn.setVisibility(View.GONE);
    }

    public void HideUndoButton() {
        if (_undoBtn != null)
            _undoBtn.setVisibility(View.GONE);
    }

    public void beginLeader() {
        if (_leaderBtn != null)
            _leaderBtn.setVisibility(View.GONE);
        if (_leaderEndBtn != null)
            _leaderEndBtn.setVisibility(View.VISIBLE);
        if (_addingLeader
                || !CurrentObstruction.type.equals(Constants.PO_LABEL))
            return;
        _leaderObs = new LineObstruction();
        _leaderObs.group = ATSKConstants.DEFAULT_GROUP;
        _leaderObs.uid = ATSKLabel.getNewLeaderUID(CurrentObstruction.uid,
                parentFragment.opc);
        _leaderObs.type = Constants.LO_LEADER;
        if (_obsPlaced)
            _leaderObs.points.add(new SurveyPoint(CurrentObstruction));
        selectTV(LOCATION_POSITION);
        setOBState(ATSKIntentConstants.OB_STATE_MAP_CLICK);
        TextContainer.getInstance()
                .displayPrompt("Add Leader Line: Tap to add point.");
        _addingLeader = true;
    }

    public boolean addLineLeaderPoint(SurveyPoint sp) {
        // Line leader
        if (_addingLeader && _leaderObs != null) {
            if (CurrentObstruction.type.equals(Constants.PO_LABEL)) {
                _leaderObs.points.add(sp);
                _leaderObs.height = CurrentObstruction.height;
                parentFragment.addLineObstruction(_leaderObs, true);
                // VW - Multi-leg leaders not necessary
                if (_leaderObs.points.size() > 1)
                    endAddLeader();
            } else {
                toast("Leader lines can only be added to labels.");
                endAddLeader();
            }
            return true;
        }
        return false;
    }

    public void endAddLeader(boolean removeTemp) {
        if (_leaderEndBtn != null)
            _leaderEndBtn.setVisibility(View.GONE);
        if (_leaderBtn != null) {
            _leaderBtn.setVisibility(CurrentObstruction.type.equals(
                    Constants.PO_LABEL) ? View.VISIBLE : View.GONE);
        }
        if (removeTemp && _leaderObs != null && _leaderObs.uid.startsWith(
                ATSKConstants.TEMP_POINT_UID)) {
            if (parentFragment != null) {
                List<LineObstruction> tempLeaders = ATSKLabel.getLeaders(
                        ATSKConstants.TEMP_POINT_UID, parentFragment.opc);
                for (LineObstruction lo : tempLeaders) {
                    Log.d(TAG, "Removing temp line obstruction: " + lo);
                    parentFragment.removeLineObstruction(lo);
                }
            }
            _leaderObs = null;
        }
        if (_addingLeader) {
            TextContainer.getInstance().closePrompt();
            setOBState(ATSKIntentConstants.OB_STATE_HIDDEN);
            stopCollection();
        }
        _addingLeader = false;
    }

    public void endAddLeader() {
        endAddLeader(false);
    }

    protected void toast(final String msg) {
        final MapView mp = MapView.getMapView();
        mp.post(new Runnable() {
            public void run() {
                Toast.makeText(mp.getContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void RemarkOKPressed(final EditText input, DialogInterface dialog) {
        CurrentRemark = input.getText().toString();
        UpdateDisplayMeasurements();
        if (dialog != null)
            dialog.dismiss();
    }

    public String getCurrentType() {
        return _typeSpinner.getSelectedItem().toString();
    }

    public String getDescription() {
        return LiveTVs[NAME_POSITION].getText().toString();
    }

    public double getHeight_m() {
        return Unit.METER.fromString(LiveTVs[HEIGHT_POSITION].getText()
                .toString());
    }

    public double getWidth_m() {
        return Unit.METER.fromString(LiveTVs[WIDTH_POSITION].getText()
                .toString());
    }

    protected abstract void UpdateSpinnerAdapter();

    // Used to stop editing whenever the toolbar is closed using the (X) button
    void stopCollection() {
        clearSelection();
        ATSKBaseFragment.UpdateNotification(getActivity(),
                "Selection ", " Cancelled", "", "");
        Log.d(TAG, "CEI cleared by clear()");
    }

    private void ShowWaitingText(View v) {
        int index = getIndex(v);
        if (index == -1)
            return;
        switch (index) {
            case HEIGHT_POSITION:
                ATSKBaseFragment.UpdateNotification(getActivity(),
                        "HEIGHT COLLECTION", "", "WAITING FOR TOP ELEVATION",
                        "");
                LiveTVs[HEIGHT_POSITION].setText(usingLRF() ?
                        R.string.input_waiting : R.string.input_waiting1);
                setCollectingTop(true, true);
                break;
            case LENGTH_POSITION:
            case WIDTH_POSITION:
            case ROTATION_POSITION:
                ATSKBaseFragment.UpdateNotification(getActivity(),
                        getName(index) + " COLLECTION", "",
                        "WAITING FOR 1st Point", "");
                LiveTVs[index].setText(R.string.input_waiting1);
                break;
        }
    }

    @Override
    public void UpdateMeasurement(int index, double measurement) {
        CurrentlyEditedIndex = index;
        StoreMeasurement(measurement);
        UpdateDisplayMeasurements();
    }

    @Override
    public void UpdateStringValue(int index, String value) {
        UpdateDisplayMeasurements();
    }

    @Override
    public void UpdateAngleUnits(boolean usingTrue) {
        DisplayAnglesTrue = usingTrue;
        UpdateDisplayMeasurements();
    }

    @Override
    public void UpdateDimensionUnits(boolean usingFeet) {
        DisplayUnitsFeet = usingFeet;
        UpdateDisplayMeasurements();
    }

    protected void DrawBoundsRectangle() {
        ClearBoundsRectangle();
        if (CurrentObstruction != null) {
            MapView mapView = MapView.getMapView();
            MapGroup group = mapView.getRootGroup().findMapGroup(
                    ATSKATAKConstants.ATSK_MAP_GROUP_OBS);
            if (group != null) {
                GeoPoint[] corners = MapHelper.convertSurveyPoint2GeoPoint(
                        Arrays.asList(CurrentObstruction.getCorners(true)));
                CurrentObsRect = new Polyline(UUID.randomUUID().toString());
                CurrentObsRect.setPoints(corners);
                CurrentObsRect.setStrokeColor(Color.GREEN);
                CurrentObsRect
                        .setStrokeWeight(ATSKATAKConstants.LINE_WEIGHT_AC);
                group.addItem(CurrentObsRect);
                CurrentObsRect.setClickable(false);
            }
        }
    }

    protected void resetMeasurements() {
        CurrentRemark = "";
        CurrentObstruction.width = 0;
        CurrentObstruction.length = 0;
        CurrentObstruction.height = 0;
        CurrentObstruction.course_true = 0;
        CurrentObstruction.remark = "";
        CurrentObstruction.alt.invalidate();
        StoredPosition.alt.invalidate();
        StoredPosition.course_true = 0;
        UpdateDisplayMeasurements();
    }

    public void ClearBoundsRectangle() {
        if (CurrentObsRect != null && CurrentObsRect.getGroup() != null)
            CurrentObsRect.getGroup().removeItem(CurrentObsRect);
        CurrentObsRect = null;
    }
}
