
package com.gmeci.atsk.obstructions.obstruction;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

import com.atakmap.android.atsk.plugin.R;
import com.atakmap.coremap.log.Log;
import com.gmeci.atsk.map.ATSKMarker;
import com.gmeci.atsk.obstructions.ObstructionType;
import com.gmeci.atsk.resources.ATSKApplication;
import com.gmeci.core.ATSKConstants;
import com.gmeci.core.ATSKIntentConstants;
import com.gmeci.core.LineObstruction;
import com.gmeci.core.PointObstruction;
import com.gmeci.core.SurveyPoint;
import com.gmeci.atskservice.resolvers.ObstructionProviderClient;
import com.gmeci.constants.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EditObstructionPointFragment extends ObstructionTabBase {

    private static final String TAG = "ObstructionPointFragment";
    double StartingLat, StartingLon;
    private PointObstruction originalPoint;
    private Button SaveButton, CancelButton;
    private ObstructionProviderClient opc;
    private String clickedGroup, clickedUID;
    private ATSKMarker _marker;
    private int _copies = 0;

    protected void UpdateSpinnerAdapter() {
        List<String> excluded = new ArrayList<String>();
        excluded.add(Constants.PO_RAB_LINE);
        excluded.add(Constants.PO_RAB_CIRCLE);
        _typeSpinner.setup(ObstructionType.POINTS, excluded);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        Context pluginContext = ATSKApplication.getInstance()
                .getPluginContext();
        return LayoutInflater.from(pluginContext).inflate(
                R.layout.obstruction_point, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        AllowTypeUpdate = false;
        UpdateSpinnerAdapter();
        SaveButton = (Button) view.findViewById(R.id.save);
        SaveButton.setVisibility(View.VISIBLE);
        SaveButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                SaveEditedPoint(false);
                DeleteTemporaryPoints();
            }
        });
        CancelButton = (Button) view.findViewById(R.id.cancel);
        CancelButton.setVisibility(View.VISIBLE);
        CancelButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                _copies = 0;
                parentFragment.CloseEditWindow(true);
                DeleteTemporaryPoints();
            }
        });
        AllowTypeUpdates();
        notifyTabHost();
    }

    protected boolean DeleteTemporaryPoints() {
        endAddLeader(true);
        if (_marker != null)
            _marker.setMovable(true);
        _marker = null;
        if (opc != null) {
            boolean PointDeleted = opc.DeletePoint(ATSKConstants.DEFAULT_GROUP,
                    ATSKConstants.TEMP_POINT_UID, true);
            boolean LineDeleted = opc.DeleteLine(ATSKConstants.DEFAULT_GROUP,
                    ATSKConstants.TEMP_LINE_UID, true);
            return PointDeleted && LineDeleted;
        }
        return false;
    }

    boolean SaveEditedPoint(boolean copy) {
        //Store this obstruction
        CurrentObstruction.type = _typeSpinner.getSelectedItem().toString();
        CurrentObstruction.uid = copy ? UUID.randomUUID().toString()
                : clickedUID;
        CurrentObstruction.remark = CurrentRemark;
        parentFragment.AddPointObstruction(CurrentObstruction);
        if (!copy)
            parentFragment.CloseEditWindow(true);
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        if (opc != null)
            InitializeScreen();
    }

    private void InitializeScreen() {
        if (opc != null)
            originalPoint = opc.GetPointObstruction(clickedGroup, clickedUID);
        else
            originalPoint = null;

        // thread safety issue with GetPointObstruction
        if (originalPoint == null) {
            originalPoint = new PointObstruction();
            originalPoint.uid = clickedUID;
            originalPoint.group = clickedGroup;
        } else
            _obsPlaced = true;

        _marker = ATSKMarker.find(clickedUID);
        if (_marker != null)
            _marker.setMovable(false);

        CurrentObstruction = new PointObstruction(originalPoint);
        _typeSpinner.setSelection(_typeSpinner.GetPosition(originalPoint.type));
        CurrentRemark = originalPoint.remark;
        UpdateDisplayMeasurements();
        StartingLat = CurrentObstruction.lat;
        StartingLon = CurrentObstruction.lon;
        endAddLeader();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        DeleteTemporaryPoints();
    }

    public void UpdateType(String NewType) {
        AddTemporaryPointAllSources();
        endAddLeader();
    }

    boolean AddTemporaryPointAllSources() { //Store this obstruction
        if (opc == null)
            return false;
        PointObstruction newPointObstruction = new PointObstruction(
                CurrentObstruction);
        newPointObstruction.type = _typeSpinner.getSelectedItem().toString();
        newPointObstruction.uid = ATSKConstants.TEMP_POINT_UID;
        newPointObstruction.remark = CurrentRemark;

        parentFragment.AddPointObstruction(newPointObstruction);

        LineObstruction TempLine = new LineObstruction();
        TempLine.uid = ATSKConstants.TEMP_LINE_UID;
        TempLine.group = ATSKConstants.DEFAULT_GROUP;
        TempLine.type = Constants.LO_GENERIC_ROUTE;
        TempLine.points.add(new SurveyPoint(CurrentObstruction.lat,
                CurrentObstruction.lon));
        TempLine.points.add(new SurveyPoint(StartingLat, StartingLon));
        opc.NewLine(TempLine);

        return false;
    }

    @Override
    boolean newPosition(SurveyPoint sp, boolean top) {

        switch (CurrentlyEditedIndex) {
            case ALT_POSITION:
            case LOCATION_POSITION: {
                addLineLeaderPoint(sp);
                setLocation(sp, top);
                if (_copies > 0) {
                    _copies--;
                    SaveEditedPoint(true);
                    DeleteTemporaryPoints();
                    if (_copies == 0)
                        parentFragment.CloseEditWindow(true);
                    else
                        startCopy();
                } else
                    AddTemporaryPointAllSources();
                break;
            }
            case NAME_POSITION: {
                //throw the position away or show RAB?
                StoredPositionIndex = 0;
                break;
            }
            case HEIGHT_POSITION: {
                collectHeight(sp);
                if (ObstructionBarVisible())
                    setOBState(ATSKIntentConstants.OB_STATE_HIDDEN);
                else
                    setOBState(ATSKIntentConstants.OB_STATE_POINT);
                //height positon can be 1 position required or 2...
                break;
            }
            default: {
                collectStoredPos(sp);
            }
        }
        UpdateDisplayMeasurements();
        return false;
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
        if (Constants.isPointWithLW(type)) {
            //hide diameter
            setVisibility(DIAMETER_POSITION, View.GONE);
            setVisibility(LENGTH_POSITION, View.VISIBLE);
            setVisibility(WIDTH_POSITION, View.VISIBLE);
            setVisibility(ROTATION_POSITION, View.VISIBLE);
        } else {
            setVisibility(DIAMETER_POSITION, View.VISIBLE);
            setVisibility(LENGTH_POSITION, View.GONE);
            setVisibility(WIDTH_POSITION, View.GONE);
            setVisibility(ROTATION_POSITION, View.GONE);
        }
        super.HideShowFields(type);
    }

    @Override
    protected String getSettingModifier() {
        return "Point";
    }

    public void setBaseOPC(ObstructionProviderClient opc, String clickedGroup,
            String clickedUID, int numCopies) {
        this.opc = opc;
        this.clickedGroup = clickedGroup;
        this.clickedUID = clickedUID;
        if (StaticTVs[DIAMETER_POSITION] != null)
            InitializeScreen();
        _copies = numCopies;
        startCopy();
    }

    protected void startCopy() {
        if (_copies > 0) {
            selectTV(LOCATION_POSITION);
            setOBState(ATSKIntentConstants.OB_STATE_MAP_CLICK);
            SaveButton.setVisibility(View.GONE);
        }
    }

    @Override
    public void UpdateGSRAngleUnits(boolean GSR) {
        // TODO Auto-generated method stub
    }
}
