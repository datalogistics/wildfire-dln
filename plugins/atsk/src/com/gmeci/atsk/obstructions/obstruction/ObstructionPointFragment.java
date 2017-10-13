
package com.gmeci.atsk.obstructions.obstruction;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.RadioGroup;

import com.atakmap.android.atsk.plugin.R;
import com.atakmap.android.gui.ColorPalette;
import com.atakmap.android.maps.MapView;
import com.gmeci.atsk.map.ATSKLabel;
import com.gmeci.atsk.obstructions.ObstructionType;
import com.gmeci.atsk.resources.ATSKApplication;
import com.gmeci.conversions.Conversions;
import com.gmeci.core.ATSKConstants;
import com.gmeci.core.ATSKIntentConstants;
import com.gmeci.constants.Constants;
import com.gmeci.core.LineObstruction;
import com.gmeci.core.PointObstruction;
import com.gmeci.core.SurveyPoint;

import java.util.List;
import java.util.UUID;

public class ObstructionPointFragment extends ObstructionTabBase {

    private static final String TAG = "ObstructionPointFragment";
    private LineObstruction _rabLine;
    private int _rabColor = Color.RED;

    public ObstructionPointFragment() {
    }

    public void UpdateType(String type) {
        super.UpdateType(type);
        if (type.equals(Constants.PO_RAB_LINE))
            removeTempPoint();
        AddTemporaryPointAllSources();
        endAddLeader();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        Context plugin = ATSKApplication.getInstance().getPluginContext();
        return LayoutInflater.from(plugin).inflate(
                R.layout.obstruction_point, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        _nextBtn.setVisibility(View.GONE);
        _nextBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                _nextBtn.setVisibility(View.GONE);
                AddPointAllSources();
            }
        });
        _colorBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showColorDialog();
            }
        });
        _rabRG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (getCurrentType().equals(Constants.PO_RAB_LINE)
                        && obstructionValid()) {
                    // Update location/elev based on selected point in R&B line
                    SurveyPoint sp = _rabLine.points.get(checkedId
                            == R.id.rab_point_tail ? 0 : 1);
                    CurrentObstruction.setSurveyPoint(sp.lat, sp.lon, sp.alt);
                    UpdateDisplayMeasurements();
                }
            }
        });

        AllowTypeUpdate = false;
        UpdateSpinnerAdapter();
        AllowTypeUpdates();
        notifyTabHost();
        if (_leaderBtn != null)
            _leaderBtn.setVisibility(View.GONE);
    }

    boolean AddTemporaryPointAllSources() {
        //Store this obstruction
        if (!AllowDrawTempIcon)
            return false;
        String type = getCurrentType();
        boolean changed = !type.equals(CurrentObstruction.type);
        CurrentObstruction.type = type;
        CurrentObstruction.uid = ATSKConstants.TEMP_POINT_UID;
        CurrentObstruction.remark = CurrentRemark;
        if (type.equals(Constants.PO_RAB_CIRCLE))
            CurrentObstruction.flags = _rabColor;
        if (parentFragment != null && _obsPlaced) {
            if (type.equals(Constants.PO_RAB_LINE)) {
                addTempRabLine();
                _nextBtn.setVisibility(obstructionValid() ? View.VISIBLE
                        : View.GONE);
            } else {
                removeTempRabLine();
                parentFragment.AddPointObstruction(CurrentObstruction);
                _nextBtn.setVisibility(View.VISIBLE);
            }
        }
        if (changed)
            endAddLeader();
        UpdateDisplayMeasurements();
        return true;
    }

    private void addTempRabLine() {
        if (parentFragment != null) {
            if (_rabLine == null) {
                _rabLine = new LineObstruction();
                _rabLine.points.add(null);
                _rabLine.points.add(null);
                _rabLine.type = Constants.PO_RAB_LINE;
                _rabLine.uid = ATSKConstants.TEMP_LINE_UID;
                setRabLinePoint();
            }
            _rabLine.remarks = CurrentRemark;
            _rabLine.flags = _rabColor;
            if (obstructionValid())
                parentFragment.addLineObstruction(_rabLine, true);
        }
    }

    private void removeTempRabLine() {
        if (parentFragment != null && _rabLine != null)
            parentFragment.removeLineObstruction(_rabLine);
        _rabLine = null;
    }

    private void setRabLinePoint() {
        if (getCurrentType().equals(Constants.PO_RAB_LINE) && _rabLine != null) {
            int index = (_rabRG.getCheckedRadioButtonId()
                    == R.id.rab_point_tail ? 0 : 1);
            _rabLine.points.set(index, new SurveyPoint(CurrentObstruction));
            if (!obstructionValid()) {
                if (index == 0)
                    _rabPoint2.setChecked(true);
                else
                    _rabPoint1.setChecked(true);
            }
        }
    }

    private boolean obstructionValid() {
        String type = getCurrentType();
        if (!_obsPlaced)
            return false;
        if (type.equals(Constants.PO_RAB_LINE))
            return _rabLine != null
                    && _rabLine.points.get(0) != null
                    && _rabLine.points.get(1) != null;
        if (type.equals(Constants.PO_RAB_CIRCLE))
            return CurrentObstruction.width > 0;
        return true;
    }

    protected void UpdateSpinnerAdapter() {
        _typeSpinner.setup(ObstructionType.POINTS);
    }

    boolean AddPointAllSources() {
        if (parentFragment == null)
            return false;

        boolean obsValid = obstructionValid();
        String type = getCurrentType();
        if (type.equals(Constants.PO_RAB_LINE) && _rabLine != null) {
            if (obsValid) {
                LineObstruction rabLine = new LineObstruction();
                rabLine.points.addAll(_rabLine.points);
                rabLine.type = type;
                rabLine.group = ATSKConstants.DEFAULT_GROUP;
                rabLine.remarks = CurrentRemark;
                rabLine.flags = _rabColor;
                rabLine.uid = UUID.randomUUID().toString();
                setRabLinePoint();
                parentFragment.addLineObstruction(rabLine, true);
            }
            _rabPoint1.setChecked(true);
            _obsPlaced = false;
            UpdateDisplayMeasurements();
            removeTempRabLine();
            return false;
        } else if (type.equals(Constants.PO_RAB_CIRCLE)) {
            CurrentObstruction.flags = _rabColor;
        }

        CurrentObstruction.type = type;
        CurrentObstruction.uid = UUID.randomUUID().toString();
        //parentFragment.GetUID(CurrentObstruction.group, CurrentType);

        CurrentObstruction.remark = CurrentRemark;

        if (_leaderObs != null
                && _leaderObs.uid.startsWith(ATSKConstants.TEMP_POINT_UID)) {
            List<LineObstruction> tempLeaders = ATSKLabel.getLeaders(
                    ATSKConstants.TEMP_POINT_UID, parentFragment.opc);
            for (LineObstruction lo : tempLeaders) {
                parentFragment.removeLineObstruction(lo);
                lo.uid = ATSKLabel.getNewLeaderUID(
                        CurrentObstruction.uid, parentFragment.opc);
                parentFragment.addLineObstruction(lo, true);
            }
        }
        endAddLeader();
        removeTempRabLine();

        removeTempPoint();
        if (obsValid)
            parentFragment.AddPointObstruction(CurrentObstruction);
        CurrentObstruction = new PointObstruction(CurrentObstruction);
        CurrentObstruction.uid = ATSKConstants.TEMP_POINT_UID;
        CurrentObstruction.setSurveyPoint(0, 0);
        CurrentObstruction.collectionMethod = null;
        _obsPlaced = false;
        UpdateDisplayMeasurements();
        return false;
    }

    protected void removeTempPoint() {
        if (parentFragment != null)
            parentFragment.RemovePointObstruction(
                    ATSKConstants.DEFAULT_GROUP,
                    ATSKConstants.TEMP_POINT_UID);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        endAddLeader(true);
        super.onPause();

        if (parentFragment != null) {
            removeTempPoint();
            if (_rabLine != null)
                parentFragment.removeLineObstruction(_rabLine);
            _rabLine = null;
        }
        //when tabs switch it goes to onPause.....
        //remove the temporary point
    }

    @Override
    protected void setLocation(SurveyPoint sp, boolean top) {
        if (getCurrentType().equals(Constants.PO_RAB_CIRCLE)) {
            boolean noRadius = CurrentObstruction.width <= 0;
            if (_rabPoint1.isChecked()) {
                super.setLocation(sp, top);
                _rabPoint2.setChecked(noRadius);
            } else {
                CurrentObstruction.width = Conversions.calculateRange(
                        CurrentObstruction, sp) * 2;
                _rabPoint1.setChecked(noRadius);
            }
            return;
        }
        super.setLocation(sp, top);
        setRabLinePoint();
    }

    @Override
    boolean newPosition(SurveyPoint sp, boolean top) {

        switch (CurrentlyEditedIndex) {
            case ALT_POSITION:
            case LOCATION_POSITION: {
                addLineLeaderPoint(sp);
                setLocation(sp, top);
                AddTemporaryPointAllSources();
                UpdateDisplayMeasurements();
                break;
            }
            case NAME_POSITION: {
                //throw the position away or show RAB?
                StoredPositionIndex = 0;
                break;
            }
            case HEIGHT_POSITION: {
                UpdateDisplayMeasurements();
                collectHeight(sp);
                break;
            }
            default: {
                collectStoredPos(sp);
            }

        }
        super.newPosition(sp, top);
        return false;
    }

    @Override
    protected void DrawBoundsRectangle() {
        if (getCurrentType().equals(Constants.PO_RAB_CIRCLE))
            ClearBoundsRectangle();
        else
            super.DrawBoundsRectangle();
    }

    @Override
    protected void selectTV(int index) {
        super.selectTV(index);
        if (parentFragment == null)
            return;
        setOBState(ATSKIntentConstants.OB_STATE_REQUESTED_POINT);
    }

    @Override
    protected void HideShowFields(String type) {
        super.HideShowFields(type);
        boolean rabLine = type.equals(Constants.PO_RAB_LINE);
        boolean rabCircle = type.equals(Constants.PO_RAB_CIRCLE);
        _rabRG.setVisibility(rabLine || rabCircle ? View.VISIBLE : View.GONE);
        Context plugin = ATSKApplication.getInstance().getPluginContext();
        if (rabLine) {
            _rabPoint1.setText(plugin.getText(R.string.tail));
            _rabPoint2.setText(plugin.getText(R.string.head));
        } else if (rabCircle) {
            _rabPoint1.setText(plugin.getText(R.string.center));
            _rabPoint2.setText(plugin.getText(R.string.radius2));
        }
        _colorLayout.setVisibility(rabLine || rabCircle ?
                View.VISIBLE : View.GONE);
        setVisibility(HEIGHT_POSITION, rabLine || rabCircle ?
                View.GONE : View.VISIBLE);

        if (Constants.isPointWithLW(type)) {
            //hide diameter
            setVisibility(DIAMETER_POSITION, View.GONE);
            setVisibility(LENGTH_POSITION, View.VISIBLE);
            setVisibility(WIDTH_POSITION, View.VISIBLE);
            setVisibility(ROTATION_POSITION, View.VISIBLE);
        } else {
            setVisibility(DIAMETER_POSITION,
                    rabLine ? View.GONE : View.VISIBLE);
            setVisibility(LENGTH_POSITION, View.GONE);
            setVisibility(WIDTH_POSITION, View.GONE);
            setVisibility(ROTATION_POSITION, View.GONE);
        }
    }

    @Override
    public void UpdateDisplayMeasurements() {
        super.UpdateDisplayMeasurements();
        if (_rabLine != null && !_rabLine.remarks.equals(CurrentRemark))
            addTempRabLine();
        if (Color.alpha(_rabColor) != 255)
            _rabColor = Color.RED;
        _colorBtn.setImageDrawable(ATSKApplication
                .getColorRect(_rabColor));
    }

    @Override
    protected String getSettingModifier() {
        return "Point";
    }

    @Override
    public void UpdateGSRAngleUnits(boolean GSR) {
        // TODO Auto-generated method stub
    }

    protected void showColorDialog() {
        Context ctx = MapView.getMapView().getContext();
        Context plugin = ATSKApplication.getInstance().getPluginContext();
        AlertDialog.Builder adb = new AlertDialog.Builder(ctx);
        adb.setTitle(plugin.getString(R.string.set_color));
        if (Color.alpha(_rabColor) != 255)
            _rabColor = Color.RED;
        ColorPalette palette = new ColorPalette(ctx, _rabColor);
        adb.setView(palette);
        final AlertDialog alert = adb.create();
        alert.show();
        palette.setOnColorSelectedListener(new ColorPalette.OnColorSelectedListener() {
            @Override
            public void onColorSelected(int color, String label) {
                _rabColor = color;
                if (getCurrentType().equals(Constants.PO_RAB_LINE))
                    addTempRabLine();
                UpdateDisplayMeasurements();
                alert.dismiss();
            }
        });
    }
}
