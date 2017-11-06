
package com.gmeci.atsk.obstructions.obstruction;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.atakmap.android.ipc.AtakBroadcast.DocumentedIntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.atakmap.android.atsk.plugin.R;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapEvent;
import com.atakmap.android.maps.MapEventDispatcher;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.log.Log;
import com.gmeci.atsk.ATSKATAKConstants;
import com.gmeci.atsk.MapHelper;
import com.gmeci.atsk.map.ATSKShape;
import com.gmeci.atsk.obstructions.ObstructionController;
import com.gmeci.atsk.obstructions.ObstructionToolbar;
import com.gmeci.atsk.obstructions.ObstructionType;
import com.gmeci.atsk.resources.ATSKApplication;
import com.gmeci.atsk.toolbar.ATSKToolbar;
import com.gmeci.atsk.toolbar.ATSKToolbarComponent;
import com.gmeci.atskservice.resolvers.ObstructionProviderClient;
import com.gmeci.constants.Constants;
import com.gmeci.conversions.Conversions;
import com.gmeci.core.ATSKIntentConstants;
import com.gmeci.core.LineObstruction;
import com.gmeci.core.SurveyPoint;

import java.util.ArrayList;

/**
 * Created to remove duplicate code out of EditObstructionRouteFragment and EditObstructionAreaFragment
 */
public class EditObstructionFragment extends ObstructionTabBase
        implements MapEventDispatcher.MapEventDispatchListener {

    private static final String TAG = "EditObstructionFragment";
    protected MapView mapView;
    protected Context pluginContext;
    protected Button saveBtn, cancelBtn;
    protected View pointNav;
    protected TextView pointNavText;

    protected LineObstruction originalLine;
    protected int pointIndex = -1;
    protected ArrayList<SurveyPoint> oldPoints = new ArrayList<SurveyPoint>();
    protected String clickedGroup, clickedUID;
    protected ObstructionProviderClient opc;
    private boolean _saved = false;
    private boolean _active = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        super.onCreateView(inflater, container, savedInstanceState);

        mapView = MapView.getMapView();
        pluginContext = ATSKApplication
                .getInstance().getPluginContext();

        return LayoutInflater.from(pluginContext).inflate(
                R.layout.obstruction_point, container, false);
    }

    protected void UpdateSpinnerAdapter() {
        _typeSpinner.setup(ObstructionType.AREAS);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        AllowTypeUpdate = false;
        UpdateSpinnerAdapter();

        // Save/cancel buttons
        saveBtn = (Button) view.findViewById(R.id.save);
        saveBtn.setVisibility(View.VISIBLE);
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SaveEditedPoint();
            }
        });
        cancelBtn = (Button) view.findViewById(R.id.cancel);
        cancelBtn.setVisibility(View.VISIBLE);
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CancelEditing();
            }
        });

        // Point navigator
        pointNav = view.findViewById(R.id.point_nav_layout);
        pointNavText = (TextView) view.findViewById(R.id.point_nav_index);
        pointNavText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (pointIndex >= 0) {
                    mapView.getMapController().panTo(
                            MapHelper.convertSurveyPoint2GeoPoint(
                                    CurrentObstruction), true);
                }
            }
        });
        Button navLeft = (Button) view.findViewById(R.id.point_nav_left);
        Button navRight = (Button) view.findViewById(R.id.point_nav_right);
        View.OnClickListener navClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LineObstruction line = getCurrentLine();
                if (line != null) {
                    int index = pointIndex + (v.getId()
                                == R.id.point_nav_left ? -1 : 1);
                    pointIndex = (index % line.points.size())
                            + (index < 0 ? line.points.size() : 0);
                }
                UpdateDisplayMeasurements();
                pointNavText.callOnClick();
            }
        };
        navLeft.setOnClickListener(navClick);
        navRight.setOnClickListener(navClick);

        AllowTypeUpdates();
        notifyTabHost();
    }

    public void UpdateType(String NewType) {
        //Redraw temporary line...
        LineObstruction existingLine = null;
        if (parentFragment != null)
            existingLine = parentFragment.getCurrentLine();
        if (existingLine != null) {
            existingLine.type = NewType;
            opc.UpdateLine(existingLine, false);
        }
    }

    public String getType() {
        return _typeSpinner.getSelectedItem().toString();
    }

    protected void SaveEditedPoint() {
        if (opc == null) {
            Log.e(TAG, "No OPC");
            return;
        }
        _saved = true;
        oldPoints.clear();
        setEditable(false);
        updateEditedLine();
        parentFragment.CloseEditWindow(true);
        dispose();
    }

    protected void CancelEditing() {
        if (_saved)
            return;

        //turn type and all numbers back where they started???
        setEditable(false);

        if (originalLine != null)
            originalLine.points = new ArrayList<SurveyPoint>(oldPoints);
        oldPoints.clear();

        if (originalLine != null)
            opc.UpdateLine(originalLine, true);
        parentFragment.CloseEditWindow(true);
        dispose();
    }

    @Override
    protected void clearSelection() {
        super.clearSelection();
        ATSKToolbarComponent.getToolbar().clearSelected();
    }

    protected void updateEditedLine() {
        if (opc == null)
            return;
        LineObstruction editedLine = getCurrentLine();
        if (editedLine != null) {
            editedLine.type = getType();
            editedLine.group = CurrentObstruction.group;
            editedLine.uid = clickedUID;
            editedLine.height = CurrentObstruction.height;
            editedLine.width = CurrentObstruction.width;
            editedLine.remarks = CurrentRemark;

            for (SurveyPoint sp : editedLine.points) {
                if (!sp.getHAEAltitude().isValid())
                    sp.setHAE(ATSKApplication.getAltitudeHAE(sp));
            }
            opc.UpdateLine(editedLine, true);
        }
    }

    public void dispose() {
        if (_active) {
            AtakBroadcast.getInstance().unregisterReceiver(_toolbarReceiver);
            mapView.getMapEventDispatcher().clearListeners();
            mapView.getMapEventDispatcher().popListeners();
            ATSKToolbarComponent.getToolbar().clearSelected();
            _active = false;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (opc != null)
            InitializeScreen();
    }

    protected void InitializeScreen() {
        originalLine = getCurrentLine();
        if (originalLine != null) {
            _obsPlaced = true;
            _saved = false;
            // Disable modification of leader line visual attributes
            boolean isLeader = originalLine.type.equals(Constants.LO_LEADER);
            StaticTVs[WIDTH_POSITION].setEnabled(!isLeader);
            LiveTVs[WIDTH_POSITION].setEnabled(!isLeader);
            _typeSpinner.setEnabled(!isLeader);

            oldPoints = new ArrayList<SurveyPoint>(originalLine.points);
            _typeSpinner
                    .setSelection(_typeSpinner.GetPosition(originalLine.type));
            CurrentObstruction.setSurveyPoint(0, 0);
            CurrentObstruction.collectionMethod = null;
            CurrentObstruction.group = originalLine.group;
            originalLine.uid = clickedUID;
            originalLine.group = clickedGroup;
            CurrentObstruction.width = originalLine.width;
            CurrentObstruction.height = originalLine.height;
            CurrentRemark = originalLine.remarks;
            updatePointIndex();

            setEditable(true);
            UpdateDisplayMeasurements();
        }

        if (!_active) {
            AtakBroadcast.getInstance()
                    .registerReceiver(
                            _toolbarReceiver,
                            new DocumentedIntentFilter(
                                    ATSKATAKConstants.UNSET_TOOLBAR));
            mapView.getMapEventDispatcher().pushListeners();
            mapView.getMapEventDispatcher().addMapEventListener(
                    MapEvent.ITEM_CLICK, this);
            mapView.getMapEventDispatcher().addMapEventListener(
                    MapEvent.ITEM_DRAG_STARTED, this);
            mapView.getMapEventDispatcher().addMapEventListener(
                    MapEvent.ITEM_DRAG_DROPPED, this);
            _active = true;
        }
    }

    @Override
    public void onPause() {
        CancelEditing();
        super.onPause();
    }

    @Override
    public void onMapEvent(MapEvent event) {
        MapItem clicked = event.getItem();
        if (clicked != null && clicked instanceof ATSKShape
                && clicked.getUID().equals(clickedUID)) {
            updatePointIndex();
        }
    }

    @Override
    boolean newPosition(SurveyPoint sp, boolean top) {

        switch (CurrentlyEditedIndex) {
            case LOCATION_POSITION: {
                setLocation(sp, top);
                break;
            }
            case ALT_POSITION: {
                setElevation(sp, top);
                break;
            }
            case NAME_POSITION: {
                StoredPositionIndex = 0;
                break;
            }
            case HEIGHT_POSITION: {
                collectHeight(sp);
                break;
            }
            default: {
                collectStoredPos(sp);
            }
        }

        UpdateDisplayMeasurements();
        super.newPosition(sp, top);
        return false;
    }

    @Override
    protected void setLocation(SurveyPoint sp, boolean top) {
        super.setLocation(sp, top);
        LineObstruction lo = getCurrentLine();
        if (pointIndex >= 0 && pointIndex < lo.points.size()) {
            lo.points.set(pointIndex, sp);
            opc.UpdateLine(lo, true);
        }
    }

    @Override
    protected void setElevation(SurveyPoint sp, boolean top) {
        super.setElevation(sp, top);
        LineObstruction lo = getCurrentLine();
        if (pointIndex >= 0 && pointIndex < lo.points.size()) {
            lo.points.set(pointIndex, sp);
            opc.UpdateLine(lo, true);
        }
    }

    @Override
    protected boolean StoreMeasurement(double newMeasurement_m) {
        boolean ret = super.StoreMeasurement(newMeasurement_m);
        if (CurrentlyEditedIndex == ALT_POSITION) {
            SurveyPoint sp = new SurveyPoint(CurrentObstruction);
            sp.setHAE(Conversions.ConvertMSLtoHAE(CurrentObstruction.lat,
                    CurrentObstruction.lon, newMeasurement_m));
            setElevation(sp, false);
        }
        UpdateDisplayMeasurements();
        return ret;
    }

    @Override
    public void UpdateDisplayMeasurements() {
        if (pointNavText != null)
            pointNavText.setText(pointIndex < 0 ? pluginContext
                    .getString(R.string.not_available) :
                    String.valueOf(pointIndex));
        LineObstruction lo = getCurrentLine();
        if (lo != null && pointIndex >= 0 && pointIndex < lo.points.size()) {
            SurveyPoint sp = lo.points.get(pointIndex);
            if (sp != null) {
                ATSKToolbar tb = ATSKToolbarComponent.getToolbar();
                if (tb.getActive() instanceof ObstructionToolbar)
                    tb.setSelected(MapHelper.convertSurveyPoint2GeoPoint(sp));
                else
                    tb.clearSelected();
                CurrentObstruction.setSurveyPoint(sp);
            }
        }
        super.UpdateDisplayMeasurements();
    }

    @Override
    protected void selectTV(int index) {
        super.selectTV(index);
        if (parentFragment == null)
            return;
        if (this instanceof EditObstructionRouteFragment)
            setOBState(ATSKIntentConstants.OB_STATE_REQUESTED_ROUTE);
        else if (this instanceof EditObstructionAreaFragment)
            setOBState(ATSKIntentConstants.OB_STATE_REQUESTED_AREA);
        if (index > -1)
            ATSKToolbarComponent.getToolbar().setSelected(
                    MapHelper.convertSurveyPoint2GeoPoint(
                            CurrentObstruction));
    }

    @Override
    protected void HideShowFields(String type) {
        setVisibility(DIAMETER_POSITION, View.GONE);
        setVisibility(LENGTH_POSITION, View.GONE);
        setVisibility(ROTATION_POSITION, View.GONE);
        if (pointNav != null)
            pointNav.setVisibility(View.VISIBLE);
        super.HideShowFields(type);
    }

    @Override
    protected String getSettingModifier() {
        return "Area";
    }

    protected LineObstruction getCurrentLine() {
        return ObstructionController.getInstance()
                .getLineObstruction(clickedUID);
    }

    public void setBaseOPC(ObstructionProviderClient opc, String clickedGroup,
            String clickedUID) {
        this.opc = opc;
        this.clickedGroup = clickedGroup;
        this.clickedUID = clickedUID;
        if (StaticTVs[DIAMETER_POSITION] != null) {
            //use the opc to set up the screen
            InitializeScreen();
        }
    }

    @Override
    public void UpdateGSRAngleUnits(boolean GSR) {
    }

    protected void setEditable(boolean editable) {
        ATSKShape shape = ATSKShape.find(clickedUID);
        if (shape != null && editable != shape.getEditable()) {
            shape.setEditable(editable);
            if (editable)
                shape.requestEdit();
        }
    }

    /**
     * Update point index based on last hit point on shape
     */
    protected void updatePointIndex() {
        ATSKShape shape = ATSKShape.find(clickedUID);
        pointIndex = shape != null ? shape.getObstructionHitIndex() : -1;
        UpdateDisplayMeasurements();
    }

    private final BroadcastReceiver _toolbarReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ATSKATAKConstants.UNSET_TOOLBAR))
                setEditable(true);
        }
    };
}
