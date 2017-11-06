
package com.gmeci.atsk.az.lz;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.atakmap.android.atsk.plugin.R;
import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.gmeci.atsk.MapHelper;
import com.gmeci.atsk.az.AZTabBase;
import com.gmeci.atsk.obstructions.FilteredObstructionTypeSpinner;
import com.gmeci.atsk.obstructions.ObstructionToolbar;
import com.gmeci.atsk.obstructions.ObstructionType;
import com.gmeci.atsk.resources.ATSKApplication;
import com.gmeci.atsk.resources.ATSKDialogManager;
import com.gmeci.atsk.resources.ATSKDialogManager.DialogUpdateInterface;
import com.gmeci.atsk.resources.CoordinateHandJamDialog;
import com.gmeci.atsk.resources.CoordinateHandJamDialog.HandJamInterface;
import com.gmeci.atsk.resources.LCRButton;
import com.gmeci.atsk.toolbar.ATSKToolbarComponent;
import com.gmeci.constants.Constants;
import com.gmeci.conversions.Conversions;
import com.gmeci.conversions.Conversions.Unit;
import com.gmeci.core.ATSKConstants;
import com.gmeci.core.ATSKIntentConstants;
import com.gmeci.core.LineObstruction;
import com.gmeci.core.PointObstruction;
import com.gmeci.core.SurveyPoint;
import com.gmeci.helpers.ObstructionHelper;

import java.util.UUID;

/**
 * Add aprons and taxiways
 */
public class LZTaxiwayFragment extends AZTabBase implements
        HandJamInterface, DialogUpdateInterface {

    private static final String TAG = "LZTaxiwayFragment";

    private static final int LOCATION_POSITION = 0;
    private static final int NAME_POSITION = 1;
    private static final int ALT_POSITION = 2;
    private static final int WIDTH_POSITION = 3;
    private static final int INPUT_COUNT = 4;

    private View _root;
    private final TextView[] _inputs = new TextView[INPUT_COUNT];
    private TextView _pointCount;
    private Button _undoBtn, _nextBtn;
    private PointObstruction _point = new PointObstruction();
    private PointObstruction _storedPoint = new PointObstruction();
    private LineObstruction _curLine;
    private FilteredObstructionTypeSpinner _spinner;
    private LCRButton _lcrButton;
    private int _editedIndex = -1, _storedPos = 0;

    private final View.OnClickListener _inputClick = new View.OnClickListener() {
        public void onClick(View v) {
            select(getIndex(v), false);
        }
    };

    private final View.OnLongClickListener _inputLongClick = new View.OnLongClickListener() {
        public boolean onLongClick(View v) {
            int idx = getIndex(v);
            select(idx, true);
            return true;
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        Context plugin = ATSKApplication
                .getInstance().getPluginContext();
        _root = LayoutInflater.from(plugin).inflate(
                R.layout.lz_taxiways, container, false);
        return _root;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        _spinner = (FilteredObstructionTypeSpinner) _root
                .findViewById(R.id.type_spinner);
        _spinner.setup(ObstructionType.TAXIWAYS);
        _nextBtn = (Button) _root.findViewById(R.id.nextLine);
        _undoBtn = (Button) _root.findViewById(R.id.undoLastPoint);
        _lcrButton = (LCRButton) _root.findViewById(R.id.lcr_selector);
        _pointCount = (TextView) _root.findViewById(R.id.point_count);

        _undoBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!undoLastPoint())
                    _undoBtn.setVisibility(View.GONE);
                if (!hasActiveLine())
                    _nextBtn.setVisibility(View.GONE);
                updateDisplay();
            }
        });

        _nextBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                _undoBtn.setVisibility(View.GONE);
                _nextBtn.setVisibility(View.GONE);
                endLine();
            }
        });

        _inputs[LOCATION_POSITION] = (TextView) _root
                .findViewById(R.id.Location);
        _inputs[NAME_POSITION] = (TextView) _root.findViewById(R.id.Name);
        _inputs[ALT_POSITION] = (TextView) _root.findViewById(R.id.Alt);
        _inputs[WIDTH_POSITION] = (TextView) _root.findViewById(R.id.Width);

        for (int i = 0; i < INPUT_COUNT; i++) {
            View parent = getParent(i);
            parent.setClickable(true);
            parent.setOnClickListener(_inputClick);
            parent.setOnLongClickListener(_inputLongClick);
            _inputs[i].setClickable(true);
            _inputs[i].setOnClickListener(_inputClick);
            _inputs[i].setOnLongClickListener(_inputLongClick);
        }

        _spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view,
                    int pos, long id) {
                String type = adapterView.getItemAtPosition(pos).toString();
                updateType(type);
            }

            public void onNothingSelected(AdapterView<?> adapterView) {
                Log.d(TAG, "Nothing selected");
            }
        });

        _point.type = getSelectedType();
        resetMeasurements();
    }

    @Override
    protected void stopCollection() {
        select(-1, false);
    }

    @Override
    public void newPosition(SurveyPoint sp, boolean top) {
        switch (_editedIndex) {
            case LOCATION_POSITION: {
                _point.setSurveyPoint(sp);
                addVertex();
                updateDisplay();
                break;
            }
            case ALT_POSITION: {
                _point.setHAE(sp.getHAE());
                _point.collectionMethod = sp.collectionMethod;
                updateDisplay();
                break;
            }
            case NAME_POSITION: {
                //throw the position away or show RAB?
                _storedPos = 0;
                updateDisplay();
                break;
            }
            case WIDTH_POSITION: {
                if (_storedPos == 0) {
                    _storedPos++;
                    _storedPoint.setSurveyPoint(sp);
                    setText(WIDTH_POSITION, R.string.input_waiting2);
                } else {
                    _storedPos = 0;
                    double[] ra = Conversions.calculateRangeAngle(
                            _storedPoint, sp);
                    setMeasurement(ra[0]);
                    updateDisplay();
                }
            }
        }
    }

    @Override
    public void shotApproved(SurveyPoint sp, double range_m,
            double azimuth_deg,
            double elev_deg, boolean topCollected) {
        newPosition(sp, topCollected);
    }

    @Override
    public void UpdateCoordinate(double lat, double lon, double elevation) {
        SurveyPoint sp = new SurveyPoint(lat, lon);
        sp.setHAE(elevation);
        sp.linearError = GeoPoint.LE90_UNKNOWN;
        sp.circularError = GeoPoint.CE90_UNKNOWN;
        sp.collectionMethod = SurveyPoint.CollectionMethod.MANUAL;
        _editedIndex = LOCATION_POSITION;
        newPosition(sp, false);
        select(_editedIndex = -1, false);
        updateDisplay();

        // Zoom to point
        MapView mv = MapView.getMapView();
        if (mv != null)
            mv.getMapController().panTo(MapHelper.
                    convertSurveyPoint2GeoPoint(sp), false);
    }

    @Override
    public void UpdateCoordinateFormat(String DisplayFormat) {
        DisplayCoordinateFormat = DisplayFormat;
        updateDisplay();
    }

    @Override
    public void UpdateMeasurement(int index, double measurement) {
        _editedIndex = index;
        setMeasurement(measurement);
        select(-1, false);
        updateDisplay();
    }

    @Override
    public void UpdateStringValue(int index, String value) {
        updateDisplay();
    }

    @Override
    public void UpdateAngleUnits(boolean usingTrue) {
        DisplayAnglesTrue = usingTrue;
        updateDisplay();
    }

    @Override
    public void UpdateGSRAngleUnits(boolean GSR) {
    }

    @Override
    public void UpdateDimensionUnits(boolean usingFeet) {
        DisplayUnitsStandard = usingFeet;
        updateDisplay();
    }

    private void select(int idx, boolean longPress) {
        boolean same = idx == _editedIndex && ATSKToolbarComponent
                .getToolbar().getActive() instanceof ObstructionToolbar;
        _editedIndex = idx;

        // Deselect others
        for (int i = 0; i < INPUT_COUNT; i++) {
            if (i == NAME_POSITION)
                continue;
            View parent = getParent(i);
            parent.setBackgroundResource(0);
            parent.setPadding(0, 0, 0, 0);
        }
        updateDisplay();

        if (longPress) {
            if (idx == NAME_POSITION)
                editName();
            else if (idx == LOCATION_POSITION) {
                CoordinateHandJamDialog chjd = new CoordinateHandJamDialog();
                chjd.Initialize(_point.lat,
                        _point.lon, DisplayCoordinateFormat,
                        _point.getHAE(), this);
            } else {
                ATSKDialogManager adm = new ATSKDialogManager(getActivity(),
                        this);
                adm.ShowMeasurementHandJamDialog(
                        Unit.METER.fromString(getValue(idx)),
                        getIndexName(idx), idx);
            }
            setOBState(ATSKIntentConstants.OB_STATE_HIDDEN);
            return;
        }

        if (idx == -1 || same) {
            _editedIndex = -1;
            setOBState(ATSKIntentConstants.OB_STATE_HIDDEN);
            return;
        }

        // Select current
        getParent(idx).setBackgroundResource(
                R.drawable.background_selected_center);
        if (idx == WIDTH_POSITION)
            setText(idx, R.string.input_waiting1);

        setOBState(isArea() ? ATSKIntentConstants.OB_STATE_REQUESTED_AREA
                : ATSKIntentConstants.OB_STATE_REQUESTED_ROUTE);
    }

    private int getIndex(View v) {
        for (int i = 0; i < INPUT_COUNT; i++) {
            if (_inputs[i] == v)
                return i;
            else if (v instanceof ViewGroup) {
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
        View parent = (View) _inputs[idx].getParent();
        return (parent == null ? _inputs[idx] : parent);
    }

    // Argument value is in meters
    private void setMeasurement(double measurement) {
        switch (_editedIndex) {
            case WIDTH_POSITION:
                if (_curLine != null && _lcrButton != null)
                    setRouteWidth(measurement, _lcrButton.getCollectionSide());
                _point.width = measurement;
                break;
            case ALT_POSITION:
                _point.height = measurement;
                break;
        }
    }

    private void editName() {
        final AlertDialog.Builder ad = new AlertDialog.Builder(getActivity(),
                android.R.style.Theme_Holo_Dialog);
        ad.setTitle(getSelectedType() + " Remarks:");

        final EditText input = new EditText(getActivity());
        input.setTextColor(ATSKConstants.LIGHT_BLUE);
        input.setText(getValue(NAME_POSITION));
        input.setSingleLine();
        input.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (((event.getAction() == KeyEvent.ACTION_DOWN)
                && (keyCode == KeyEvent.KEYCODE_ENTER))) {
                    select(-1, false);
                    setName(input.getText().toString());
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
                select(-1, false);
                setName(input.getText().toString());
            }
        });
        ad.show();
    }

    private void updateType(String type) {
        _storedPos = 0;

        if (!Constants.isTaxiway(type))
            type = Constants.LO_TAXIWAY;

        getParent(WIDTH_POSITION).setVisibility(
                isArea() ? View.GONE : View.VISIBLE);
        _lcrButton.setVisibility(
                isArea() ? View.GONE : View.VISIBLE);
        _lcrButton.setSelectionSide(LCRButton.CollectionSide.CENTER);

        if (!type.equals(_point.type)) {
            if (_curLine != null) {
                opc.DeleteLine(ATSKConstants.DEFAULT_GROUP,
                        _curLine.uid, true);
                _curLine = null;
            }
            _point.type = type;
            resetMeasurements();
        }
    }

    private void resetMeasurements() {
        _point.remark = "";
        _point.width = 0;
        _point.alt.invalidate();
        _storedPoint.alt.invalidate();
        updateDisplay();
    }

    private void setText(int idx, String text) {
        if (idx >= 0 && _inputs[idx] != null)
            _inputs[idx].setText(text);
    }

    private void setText(int idx, int resId) {
        if (idx >= 0 && _inputs[idx] != null)
            _inputs[idx].setText(resId);
    }

    private String getValue(int idx) {
        if (idx >= 0 && _inputs[idx] != null)
            return _inputs[idx].getText().toString();
        return "";
    }

    private String getIndexName(int idx) {
        switch (idx) {
            case WIDTH_POSITION:
                return "Width";
            case ALT_POSITION:
                return "Elevation";
        }
        return "UNKNOWN";
    }

    private void updateDisplay() {
        Unit displayUnit = (DisplayUnitsStandard ? Unit.FOOT : Unit.METER);
        setText(LOCATION_POSITION, Conversions.getCoordinateString(
                _point.lat, _point.lon, DisplayCoordinateFormat));
        setText(NAME_POSITION, _point.remark);
        setText(WIDTH_POSITION, Unit.METER.format(_point.width, displayUnit));
        setText(ALT_POSITION, _point.getMSLAltitude().toString(displayUnit));

        if (_curLine != null) {
            _curLine.width = _point.width;
            _curLine.remarks = _point.remark;
            opc.UpdateLine(_curLine, false);
            _pointCount.setText(String.valueOf(_curLine.points.size()));
        } else
            _pointCount.setText("0");
    }

    private boolean undoLastPoint() {
        if (opc != null && _curLine != null) {
            boolean deleted = opc.LineDeleteLastPoint(_curLine.group,
                    _curLine.uid);
            _curLine = opc.GetLine(_curLine.group, _curLine.uid);
            if (_curLine != null && _curLine.points.size() == 0)
                _curLine = null;
            return deleted;
        }
        return false;
    }

    public void endLine() {
        if (hasActiveLine()) {
            _curLine.type = getSelectedType();
            _curLine.remarks = _point.remark;
            _curLine.height = 0;
            _curLine.width = _point.width;
            _curLine.closed = _curLine.filled = Constants.isArea(_curLine.type);
            opc.UpdateLine(_curLine, false);
        }
        _curLine = null;
        updateDisplay();
    }

    private boolean hasActiveLine() {
        return _curLine != null && _curLine.points.size() > 1;
    }

    private String getSelectedType() {
        return _spinner.getSelectedItem().toString();
    }

    private void setName(String name) {
        _point.remark = name;
        updateDisplay();
    }

    private boolean isArea() {
        return Constants.isArea(getSelectedType());
    }

    public double getLRCOffset() {

        LCRButton.CollectionSide cs = _lcrButton.getCollectionSide();
        if (cs == LCRButton.CollectionSide.LEFT)
            return (_point.width / 2f) * -1;
        if (cs == LCRButton.CollectionSide.RIGHT)
            return (_point.width / 2f);
        return 0;
    }

    private void addVertex() {

        String type = getSelectedType();
        _undoBtn.setVisibility(View.VISIBLE);

        Toast.makeText(getActivity(), "Vertex Collected",
                Toast.LENGTH_SHORT).show();

        PointObstruction newPO = new PointObstruction(_point);

        // Calculate real new position based on LCR offset
        if (_curLine != null && _curLine.points.size() > 0) {
            SurveyPoint lastPoint = _curLine.points
                    .get(_curLine.points.size() - 1);
            if (lastPoint != null) {
                double ang = Conversions.CalculateAngledeg(lastPoint.lat,
                        lastPoint.lon, newPO.lat, newPO.lon);
                double[] offsetCoord = Conversions.AROffset(newPO.lat,
                        newPO.lon, ang - 90.0f, getLRCOffset());
                newPO.lat = offsetCoord[0];
                newPO.lon = offsetCoord[1];
            }
        }

        if (_curLine == null) {
            _curLine = new LineObstruction();
            _curLine.group = newPO.group;
            _curLine.uid = UUID.randomUUID().toString();
            _curLine.type = type;
            _curLine.remarks = newPO.remark;
            _curLine.closed = _curLine.filled = isArea();
            _curLine.height = 0;
            _curLine.width = newPO.width;
            _curLine.points.add(newPO);
            opc.NewLine(_curLine);
        } else {
            _curLine.points.add(newPO);
            opc.LineAppendPoint(_curLine.group,
                    _curLine.uid, newPO);
        }

        if (hasActiveLine())
            _nextBtn.setVisibility(View.VISIBLE);
    }

    @Override
    public void UpdateScreen() {
    }

    protected boolean setRouteWidth(double width, LCRButton.CollectionSide side) {
        if (_curLine == null)
            return false;
        if (ObstructionHelper.setRouteWidth(_curLine, width, side.ordinal())) {
            for (SurveyPoint sp : _curLine.points)
                sp.setHAE(ATSKApplication.getAltitudeHAE(sp));
            opc.UpdateLine(_curLine, side != LCRButton.CollectionSide.CENTER);
        }
        return true;
    }
}
