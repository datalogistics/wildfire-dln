
package com.gmeci.atsk.vehicle;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.atakmap.android.atsk.plugin.R;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.toolbar.widgets.TextContainer;
import com.atakmap.coremap.maps.coords.Altitude;
import com.atakmap.coremap.maps.coords.AltitudeReference;
import com.atakmap.coremap.maps.coords.AltitudeSource;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.gmeci.atsk.MapHelper;
import com.gmeci.atsk.az.AZTabBase;
import com.gmeci.atsk.map.ATSKVehicle;
import com.gmeci.atsk.obstructions.FilteredObstructionTypeSpinner;
import com.gmeci.atsk.obstructions.ObstructionSpinnerAdapter;
import com.gmeci.atsk.obstructions.ObstructionToolbar;
import com.gmeci.atsk.obstructions.ObstructionType;
import com.gmeci.atsk.resources.ATSKApplication;
import com.gmeci.atsk.resources.ATSKDialogManager;
import com.gmeci.atsk.resources.ATSKDialogManager.DialogUpdateInterface;
import com.gmeci.atsk.resources.CoordinateHandJamDialog;
import com.gmeci.atsk.resources.CoordinateHandJamDialog.HandJamInterface;
import com.gmeci.atsk.toolbar.ATSKToolbarComponent;
import com.gmeci.conversions.Conversions;
import com.gmeci.conversions.Conversions.Unit;
import com.gmeci.core.ATSKConstants;
import com.gmeci.core.ATSKIntentConstants;
import com.gmeci.core.PointObstruction;
import com.gmeci.core.SurveyPoint;
import com.gmeci.core.SurveyPoint.CollectionMethod;

import java.util.UUID;

/**
 * Aircraft obstruction fragment
 */
public class VehicleTabBase extends AZTabBase
        implements OnClickListener, OnLongClickListener,
        OnItemSelectedListener, HandJamInterface, DialogUpdateInterface {

    private static final String TAG = "VehicleTabBase";
    private static final String PROMPT_ROT_FIRST = "Rotation Angle: Set first point.";
    private static final String PROMPT_ROT_SECOND = "Rotation Angle: Set second point.";

    // Input indices
    private static final int LOCATION = 0;
    private static final int NAME = 1;
    private static final int HEIGHT = 2;
    private static final int LENGTH = 3;
    private static final int WIDTH = 4;
    private static final int ROTATION = 5;
    private static final int ELEVATION = 6;
    private static final int INPUT_COUNT = 7;

    // Views and resources
    private View _root;
    private Context _context;
    private Resources _res;
    private MapView _mapView;
    private SharedPreferences _prefs;
    private final TextView[] _inputs = new TextView[INPUT_COUNT];
    private TextView _methodTV;
    private ImageButton _nextButton, _saveButton, _cancelButton;
    private FilteredObstructionTypeSpinner _spinner;
    private AlertDialog _alert;
    private boolean _created = false;
    private boolean _tempMode = false;
    private boolean _editMode = false;
    private boolean _obsPlaced = false;
    private int _copies = 0;

    // Temp object fields
    private ATSKVehicle _activeObs = null;
    private String _blockName = null;
    private String _remarks = "";
    private SurveyPoint _loc;
    private boolean _trueNorth;
    private boolean _usingFeet;
    private int _collectionIdx = -1;
    private SurveyPoint _firstLoc;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        Context plugin = ATSKApplication.getInstance().getPluginContext();
        _mapView = MapView.getMapView();
        _context = _mapView.getContext();
        _res = plugin.getResources();
        _root = LayoutInflater.from(plugin).inflate(
                R.layout.vehicle_fragment, container, false);
        _created = false;

        // Load from preferences
        _prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        reloadPrefs();
        setLocation(new SurveyPoint());

        return _root;
    }

    @Override
    public void onPause() {
        cleanup();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        reloadPrefs();
        setupAircraft();
        updateDisplay();
        azpc.putSetting(ATSKConstants.CURRENT_SCREEN,
                ATSKConstants.CURRENT_SCREEN_VEHICLE, TAG);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Instantiation
        _inputs[LOCATION] = (TextView) _root.findViewById(R.id.loc);
        _inputs[NAME] = (TextView) _root.findViewById(R.id.name);
        _inputs[HEIGHT] = (TextView) _root.findViewById(R.id.height);
        _inputs[LENGTH] = (TextView) _root.findViewById(R.id.length);
        _inputs[WIDTH] = (TextView) _root.findViewById(R.id.width);
        _inputs[ROTATION] = (TextView) _root.findViewById(R.id.rotation);
        _inputs[ELEVATION] = (TextView) _root.findViewById(R.id.alt);
        _methodTV = (TextView) _root.findViewById(R.id.collection_method);

        // Click events
        for (int i = 0; i < INPUT_COUNT; i++) {
            View parent = getParent(i);
            parent.setClickable(true);
            parent.setOnClickListener(this);
            parent.setOnLongClickListener(this);
        }

        _spinner = (FilteredObstructionTypeSpinner)
                _root.findViewById(R.id.aircraft_spinner);
        _spinner.setup(ObstructionType.VEHICLES);
        _spinner.setOnItemSelectedListener(this);

        // Add new vehicle
        _nextButton = (ImageButton) _root.findViewById(R.id.nextObs);
        _nextButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (_activeObs == null)
                    return;
                PointObstruction po = _activeObs.toPointObstruction();
                opc.NewPoint(po);
                _activeObs.setTemp(false);
                cleanup(false);
                clearParameters();
            }
        });

        // Save edited vehicle
        _saveButton = (ImageButton) _root.findViewById(R.id.save);
        _saveButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (_activeObs == null || _tempMode)
                    return;
                _activeObs.save();
                stopEditMode();
            }
        });
        _cancelButton = (ImageButton) _root.findViewById(R.id.cancel);
        _cancelButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                stopEditMode();
            }
        });

        _created = true;
        clearParameters();
        notifyTabHost();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cleanup();
    }

    @Override
    protected void UpdateScreen() {
    }

    @Override
    protected void stopCollection() {
        setOBState(ATSKIntentConstants.OB_STATE_HIDDEN);
        select(_collectionIdx = -1);
        _firstLoc = null;
        TextContainer.getInstance().closePrompt();
    }

    @Override
    public void onClick(View v) {
        int idx = getIndex(v);
        switch (idx) {
            case HEIGHT:
            case WIDTH:
            case LENGTH:
                toast("This field is read-only for vehicle obstructions.");
                break;
            case NAME:
                inputAlert(_blockName + " Remarks:", NAME);
                break;
            case LOCATION:
            case ROTATION:
            case ELEVATION:
                toggleObsMode(idx);
                break;
        }
    }

    @Override
    public boolean onLongClick(View v) {
        int idx = getIndex(v);
        ATSKDialogManager adm;
        stopCollection();
        switch (idx) {
            case NAME:
            case HEIGHT:
            case WIDTH:
            case LENGTH:
                onClick(v);
                break;
            case LOCATION:
                CoordinateHandJamDialog dialog = new CoordinateHandJamDialog();
                dialog.Initialize(_loc.lat, _loc.lon,
                        DisplayCoordinateFormat, _loc.getHAE(), this);
                break;
            case ROTATION:
                adm = new ATSKDialogManager(getActivity(), this, false);
                adm.ShowAngleHandJamDialog(_loc.course_true, _loc, ROTATION);
                break;
            case ELEVATION:
                adm = new ATSKDialogManager(getActivity(), this, true);
                adm.ShowMeasurementHandJamDialog(_loc.getMSL(),
                        "Base Elevation (MSL)", ELEVATION);
                break;
        }
        return true;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view,
            int position, long id) {
        _blockName = _spinner.getItemAtPosition(position).toString();
        if (_obsPlaced) {
            startTempMode();
            setupAircraft();
        }
        updateDisplay();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    @Override
    public void UpdateCoordinate(double lat, double lon, double elevation) {
        _collectionIdx = LOCATION;
        SurveyPoint sp = new SurveyPoint(lat, lon);
        sp.setHAE(elevation);
        sp.collectionMethod = CollectionMethod.MANUAL;
        newPosition(sp, false);
        _collectionIdx = -1;

        // Zoom to point
        MapView mv = MapView.getMapView();
        if (mv != null)
            mv.getMapController().panTo(MapHelper.
                    convertSurveyPoint2GeoPoint(sp), false);
    }

    @Override
    public void newPosition(SurveyPoint sp, boolean TopCollected) {
        if (_collectionIdx == LOCATION) {
            _obsPlaced = true;
            if (!_editMode)
                startTempMode();
            if (_copies > 0) {
                setupAircraft();
                _copies--;
                if (_activeObs != null) {
                    _activeObs.setTemp(false);
                    ATSKVehicle vo = new ATSKVehicle(
                            UUID.randomUUID().toString());
                    vo.setup(_activeObs.toPointObstruction(), false);
                    vo.setRadialOn(_activeObs.isRadialOn());
                    vo.moveTo(sp);
                    opc.NewPoint(vo.toPointObstruction());
                    if (_copies > 0)
                        startCopy();
                }
                if (_activeObs == null || _copies == 0)
                    stopEditMode();
            } else {
                setLocation(sp);
            }
        } else if (_collectionIdx == ROTATION) {
            if (_firstLoc == null) {
                TextContainer.getInstance().displayPrompt(PROMPT_ROT_SECOND);
                _firstLoc = sp;
            } else {
                setRotation(Conversions.calculateAngle(_firstLoc, sp));
                toggleObsMode(ROTATION);
            }
        } else if (_collectionIdx == ELEVATION) {
            _loc.setHAE(sp.getHAE());
            _loc.collectionMethod = sp.collectionMethod;
            updateDisplay();
        }
    }

    @Override
    public void UpdateCoordinateFormat(String format) {
        DisplayCoordinateFormat = format;
        updateDisplay();
    }

    public void editVehicle(String uid, int numCopies) {
        if (!_created)
            return;
        cleanup();
        ATSKVehicle obs = ATSKVehicle.find(uid);
        if (obs != null) {
            _obsPlaced = true;
            _editMode = true;
            _tempMode = false;
            _copies = numCopies;
            _activeObs = obs;

            // Copy obstruction params to menu
            _loc = obs.getLocation();
            _blockName = obs.getBlockName();
            _remarks = obs.getName();
            ObstructionSpinnerAdapter adapter = (ObstructionSpinnerAdapter) _spinner
                    .getAdapter();
            _spinner.setSelection(adapter.getPosition(_blockName));
            if (_copies > 0)
                startCopy();
            else
                _activeObs.setTemp(true);
            updateDisplay();
        }
    }

    private void startCopy() {
        if (_copies > 0) {
            if (_collectionIdx != LOCATION)
                toggleObsMode(LOCATION);
            setOBState(ATSKIntentConstants.OB_STATE_MAP_CLICK);
        }
    }

    public void stopEditMode() {
        if (_activeObs != null) {
            // Reload obstruction from database
            PointObstruction po = opc.GetPointObstruction(
                    ATSKConstants.VEHICLE_GROUP, _activeObs.getUID());
            if (po != null)
                _activeObs.setup(po, false);
            else
                _activeObs.delete(false);
        }
        _copies = 0;
        _editMode = false;
        _obsPlaced = false;
        _activeObs = null;
        stopCollection();
        clearParameters();
        updateDisplay();
    }

    private void startTempMode() {
        if (!_editMode && !_tempMode && _obsPlaced) {
            _tempMode = true;
            _nextButton.setVisibility(View.VISIBLE);
            updateDisplay();
        }
    }

    private void toast(String msg) {
        Toast.makeText(_context, msg, Toast.LENGTH_LONG).show();
    }

    private int getIndex(View v) {
        for (int i = 0; i < INPUT_COUNT; i++) {
            if (_inputs[i] == v)
                return i;
            else {
                ViewGroup parent = (ViewGroup) v;
                for (int j = 0; j < parent.getChildCount(); j++) {
                    if (_inputs[i] == parent.getChildAt(j))
                        return i;
                }
            }
        }
        return 0;
    }

    private View getParent(int idx) {
        return (View) _inputs[idx].getParent();
    }

    private void setLocation(SurveyPoint sp) {
        double trueDeg = (_loc == null ? sp.course_true : _loc.course_true);
        _loc = new SurveyPoint(sp);
        _loc.course_true = trueDeg;
        if (_tempMode && _activeObs == null) {
            _activeObs = new ATSKVehicle(UUID.randomUUID().toString());
            _obsPlaced = true;
            setupAircraft();
        }
        updateDisplay();
    }

    private void setRotation(double deg) {
        _loc.course_true = Conversions.deg360(deg);
        if (_activeObs != null)
            _activeObs.setHeading(deg);
        updateDisplay();
    }

    private void clearParameters() {
        _remarks = "";
        _loc.setSurveyPoint(0, 0);
        _loc.collectionMethod = null;
        _loc.course_true = 0;
        _loc.alt.invalidate();
        updateDisplay();
    }

    private void setInputText(int idx, Object value) {
        _inputs[idx].setText(String.valueOf(value));
    }

    private void updateDisplay() {
        if (!_created)
            return;

        Unit displayUnit = (_usingFeet ? Unit.FOOT : Unit.METER);

        // Update text views
        setInputText(LOCATION, Conversions.getCoordinateString(
                _loc.lat, _loc.lon, DisplayCoordinateFormat));
        setInputText(NAME, _remarks);
        String deg = _res.getString(R.string.atsk_deg_north);
        if (_trueNorth)
            setInputText(ROTATION, String.format(deg, _loc.course_true, 'T'));
        else
            setInputText(ROTATION, String.format(deg,
                    Conversions.GetMagAngle(_loc.course_true,
                            _loc.lat, _loc.lon), 'M'));

        setInputText(ELEVATION, _loc.getMSLAltitude().toString(displayUnit));

        // Update temporary obstruction
        if (_activeObs != null) {
            _activeObs.moveTo(_loc);
            _activeObs.setName(_remarks);
            setInputText(WIDTH, Unit.METER.format(
                    _activeObs.getWidth(), displayUnit));
            setInputText(LENGTH, Unit.METER.format(
                    _activeObs.getLength(), displayUnit));
            setInputText(HEIGHT, Unit.METER.format(
                    _activeObs.getHeight(), displayUnit));
        } else {
            String none = Unit.METER.format(0, displayUnit);
            setInputText(WIDTH, none);
            setInputText(LENGTH, none);
            setInputText(HEIGHT, none);
        }
        _saveButton.setVisibility(_editMode && _copies <= 0 ? View.VISIBLE
                : View.GONE);
        _cancelButton.setVisibility(_editMode ? View.VISIBLE : View.GONE);

        if (_methodTV != null) {
            if (_loc.collectionMethod != null) {
                _methodTV.setVisibility(View.VISIBLE);
                _methodTV.setText(_loc.collectionMethod.name);
                _methodTV.setBackgroundColor(_loc.collectionMethod.color);
            } else
                _methodTV.setVisibility(View.GONE);
        }
    }

    private void setupAircraft() {
        if (!_created || !_obsPlaced || _blockName == null ||
                _activeObs == null || (!_editMode && !_tempMode))
            return;
        _activeObs.setup(_blockName, _loc, true);
    }

    private void inputAlert(String title, final int idx) {
        final AlertDialog.Builder adb = new AlertDialog.Builder(getActivity());
        adb.setTitle(title);

        final EditText input = new EditText(getActivity());
        input.setText(_inputs[idx].getText());
        input.setSingleLine();
        input.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN
                        && keyCode == KeyEvent.KEYCODE_ENTER) {
                    finishAlert(input, idx);
                    return true;
                }
                return false;
            }
        });

        adb.setView(input);
        adb.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int arg1) {
                dialog.dismiss();
            }
        });
        adb.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                finishAlert(input, idx);
            }
        });
        _alert = adb.create();
        _alert.show();
    }

    private void finishAlert(EditText input, int idx) {
        if (idx == NAME && _activeObs != null) {
            _remarks = input.getText().toString();
            updateDisplay();
        }
        _inputs[idx].setText(input.getText());
        if (_alert != null)
            _alert.dismiss();
    }

    private void toggleObsMode(int idx) {
        boolean enable = (idx != _collectionIdx || !(ATSKToolbarComponent
                .getToolbar().getActive() instanceof ObstructionToolbar));
        _collectionIdx = (enable ? idx : -1);

        _firstLoc = null;
        TextContainer.getInstance().closePrompt();

        // Enable obstruction toolbar
        if (enable) {
            if (idx == ROTATION)
                TextContainer.getInstance().displayPrompt(PROMPT_ROT_FIRST);
            String method = ATSKApplication.getCollectionState();
            if (!method.equals(ATSKIntentConstants.OB_STATE_MAP_CLICK))
                setOBState(ATSKIntentConstants.OB_STATE_REQUESTED_POINT);
        } else
            setOBState(ATSKIntentConstants.OB_STATE_HIDDEN);

        select(_collectionIdx);
    }

    private void select(int idx) {
        // Set green background on active input
        for (int i = 0; i < INPUT_COUNT; i++) {
            if (i == NAME)
                continue;
            View parent = getParent(i);
            parent.setPadding(0, 0, 0, 0);
            parent.setBackgroundResource(i == idx ?
                    R.drawable.fullborder_background_selected : 0);
        }
    }

    private void cleanup(boolean hideToolbar) {
        if (_activeObs != null && _tempMode)
            _activeObs.delete(false);
        if (_editMode)
            stopEditMode();
        _activeObs = null;
        _tempMode = false;
        _editMode = false;
        _obsPlaced = false;
        _nextButton.setVisibility(View.GONE);
        if (hideToolbar)
            setOBState(ATSKIntentConstants.OB_STATE_HIDDEN);
    }

    private void cleanup() {
        cleanup(true);
    }

    private void reloadPrefs() {
        _trueNorth = _prefs.getString(ATSKConstants.UNITS_ANGLE,
                ATSKConstants.UNITS_ANGLE_MAG).equals(
                ATSKConstants.UNITS_ANGLE_TRUE);
        _usingFeet = _prefs.getString(ATSKConstants.UNITS_DISPLAY,
                ATSKConstants.UNITS_FEET).equals(ATSKConstants.UNITS_FEET);
    }

    @Override
    public void UpdateMeasurement(int index, double measurement) {
        switch (index) {
            case ELEVATION:
                _loc.setMSL(_usingFeet ? Conversions.GetMeasurement(
                        measurement, "m", true) : measurement);
                break;
            case ROTATION:
                setRotation(measurement);
                break;
        }
        updateDisplay();
    }

    @Override
    public void UpdateAngleUnits(boolean usingTrue) {
        _trueNorth = usingTrue;
        _prefs.edit().putString(ATSKConstants.UNITS_ANGLE,
                _trueNorth ? ATSKConstants.UNITS_ANGLE_TRUE :
                        ATSKConstants.UNITS_ANGLE_MAG).apply();
        updateDisplay();
    }

    @Override
    public void UpdateGSRAngleUnits(boolean GSR) {
    }

    @Override
    public void UpdateStringValue(int index, String value) {
    }

    public void UpdateDimensionUnits(boolean usingFeet) {
        _usingFeet = usingFeet;
        _prefs.edit().putString(ATSKConstants.UNITS_DISPLAY, _usingFeet ?
                ATSKConstants.UNITS_FEET : ATSKConstants.UNITS_METERS)
                .apply();
    }
}
