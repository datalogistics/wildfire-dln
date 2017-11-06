
package com.gmeci.atsk.obstructions.obstruction;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.atakmap.android.atsk.plugin.R;
import com.gmeci.atsk.obstructions.ObstructionType;
import com.gmeci.atsk.resources.ATSKApplication;
import com.gmeci.core.ATSKConstants;
import com.gmeci.core.ATSKIntentConstants;
import com.gmeci.core.LineObstruction;
import com.gmeci.core.PointObstruction;
import com.gmeci.atskservice.resolvers.DBURIConstants;
import com.gmeci.atskservice.resolvers.ObstructionProviderClient;

import com.gmeci.atsk.resources.LCRButton;
import com.gmeci.atsk.resources.LCRButton.CollectionSide;
import com.gmeci.core.SurveyPoint;

import java.util.UUID;

public class ObstructionRouteFragment extends ObstructionTabBase {

    LineContentObserver lineCO;
    final Handler coHandler = new Handler();
    ObstructionProviderClient opc;
    LCRButton lcrButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        opc = new ObstructionProviderClient(getActivity());
    }

    public double getLRCOffset() {
        CollectionSide cs = lcrButton.getCollectionSide();
        if (cs == CollectionSide.LEFT)
            return (CurrentObstruction.width / 2f) * -1;
        if (cs == CollectionSide.RIGHT)
            return (CurrentObstruction.width / 2f);
        return 0;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        Context pluginContext = ATSKApplication
                .getInstance().getPluginContext();
        return LayoutInflater.from(pluginContext).inflate(
                R.layout.obstruction_route, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        lcrButton = (LCRButton) view.findViewById(R.id.lcr_selector);
        if (lcrButton != null)
            lcrButton.setSelectionSide(CollectionSide.CENTER);
        PointCountLabelTV.setVisibility(View.VISIBLE);
        PointCountTV.setVisibility(View.VISIBLE);
        AllowTypeUpdate = false;
        UpdateSpinnerAdapter();
        AllowTypeUpdates();
        notifyTabHost();
    }

    protected void UpdateSpinnerAdapter() {
        _typeSpinner.setup(ObstructionType.ROUTES);
    }

    @Override
    public void onResume() {
        super.onResume();
        opc.Start();
        lineCO = new LineContentObserver(coHandler);
        getActivity().getContentResolver().registerContentObserver(
                Uri.parse(DBURIConstants.LINE_POINT_URI), true, lineCO);
    }

    @Override
    public void onPause() {
        super.onPause();
        opc.Stop();
        //when tabs switch it goes to onPause.....
        getActivity().getContentResolver().unregisterContentObserver(lineCO);
        if (parentFragment != null) {
            parentFragment.LineComplete(false, this);
            //stop collecting a route BC list if we're collecting
            try {
                if (parentFragment != null
                        && parentFragment.hardwareInterface != null)
                    parentFragment.hardwareInterface
                            .EndCurrentRoute(false);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected boolean newPosition(SurveyPoint sp, boolean top) {
        switch (CurrentlyEditedIndex) {
            case LOCATION_POSITION: {
                setLocation(sp, top);
                AddPoint2LineAllSources();
                UpdateDisplayMeasurements();
                break;
            }
            case ALT_POSITION: {
                setElevation(sp, top);
                UpdateDisplayMeasurements();
                break;
            }
            case NAME_POSITION: {
                //throw the position away or show RAB?
                StoredPositionIndex = 0;
                UpdateDisplayMeasurements();
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
    protected void UpdateDisplayMeasurements() {
        if (opc != null && parentFragment != null) {
            LineObstruction lo = parentFragment.getCurrentLine();
            if (lo != null && !lo.remarks.equals(CurrentRemark)) {
                lo.remarks = CurrentRemark;
                opc.UpdateLine(lo, false);
            }
        }
        super.UpdateDisplayMeasurements();
    }

    @Override
    protected boolean StoreMeasurement(double newMeasurement_m) {
        switch (CurrentlyEditedIndex) {
            case WIDTH_POSITION:
                CollectionSide cs = lcrButton.getCollectionSide();
                return setRouteWidth(newMeasurement_m, cs);
            case HEIGHT_POSITION:
                if (parentFragment == null || opc == null)
                    break;
                LineObstruction curLine = parentFragment.getCurrentLine();
                if (curLine != null && curLine.height != newMeasurement_m) {
                    curLine.height = newMeasurement_m;
                    opc.UpdateLine(curLine, false);
                }
                break;
        }
        return super.StoreMeasurement(newMeasurement_m);
    }

    private String getUID() {
        String uid = parentFragment.getCurrentLineUID();
        if (uid == null)
            uid = UUID.randomUUID().toString();
        return uid;
    }

    private void AddPoint2LineAllSources() {
        PointObstruction newPO = new PointObstruction(CurrentObstruction);

        if (parentFragment.hasActiveLine())
            _nextBtn.setVisibility(View.VISIBLE);

        String CurrentType = _typeSpinner.getSelectedItem().toString();

        _undoBtn.setVisibility(View.VISIBLE);

        newPO.uid = getUID();
        newPO.type = CurrentType;

        Toast.makeText(getActivity(), "Vertex Collected", Toast.LENGTH_SHORT)
                .show();
        parentFragment.AddPoint2LineObstruction(newPO, getLRCOffset(),
                false, false);

        UpdateDisplayMeasurements();
    }

    @Override
    public void UpdateType(String NewType) {
        //Redraw temporary line...
        resetMeasurements();
        LineObstruction existingLine = null;
        if (parentFragment != null)
            existingLine = parentFragment.opc.GetLine(
                    CurrentObstruction.group, getUID());
        if (existingLine != null) {
            existingLine.type = NewType;
            parentFragment.opc.UpdateLine(existingLine, false);
        }
    }

    @Override
    protected void selectTV(int index) {
        super.selectTV(index);
        if (parentFragment == null)
            return;
        setOBState(ATSKIntentConstants.OB_STATE_REQUESTED_ROUTE);
    }

    @Override
    protected void HideShowFields(String type) {
        //hide diameter
        setVisibility(DIAMETER_POSITION, View.GONE);
        setVisibility(LENGTH_POSITION, View.GONE);
        setVisibility(ROTATION_POSITION, View.GONE);
        setVisibility(WIDTH_POSITION, View.VISIBLE);
        super.HideShowFields(type);
    }

    @Override
    protected String getSettingModifier() {
        return "Route";
    }

    @Override
    public void UpdateGSRAngleUnits(boolean GSR) {
        // TODO Auto-generated method stub

    }

    private class LineContentObserver extends ContentObserver {

        public LineContentObserver(Handler handler) {
            super(handler);
        }

        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            String URIString = uri.toString();
            String Group = ATSKConstants.GetGroupFromURI(uri);
            String UID = ATSKConstants.GetUIDFromURI(uri);
            if (URIString.contains("alp")) { //update the line point count
                int PointCount = opc.GetPointsInLineCount(Group, UID);
                PointCountTV.setText(String.format("%d", PointCount));
                _undoBtn.setVisibility(View.VISIBLE);
                if (PointCount > 1)
                    _nextBtn.setVisibility(View.VISIBLE);
            }

        }

    }

}
