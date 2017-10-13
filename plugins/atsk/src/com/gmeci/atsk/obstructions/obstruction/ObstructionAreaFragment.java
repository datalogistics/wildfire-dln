
package com.gmeci.atsk.obstructions.obstruction;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import com.atakmap.android.atsk.plugin.R;
import com.gmeci.atsk.obstructions.ObstructionType;
import com.gmeci.atsk.resources.ATSKApplication;
import com.gmeci.core.ATSKConstants;
import com.gmeci.core.ATSKIntentConstants;
import com.gmeci.core.LineObstruction;
import com.gmeci.core.PointObstruction;
import com.gmeci.atskservice.resolvers.DBURIConstants;
import com.gmeci.atskservice.resolvers.ObstructionProviderClient;
import com.gmeci.core.SurveyPoint;

import java.util.UUID;

public class ObstructionAreaFragment extends ObstructionTabBase {
    LineContentObserver lineCO;
    final Handler coHandler = new Handler();
    ObstructionProviderClient opc;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        opc = new ObstructionProviderClient(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        Context pluginContext = ATSKApplication
                .getInstance().getPluginContext();
        return LayoutInflater.from(pluginContext).inflate(
                R.layout.obstruction_point, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    protected void UpdateSpinnerAdapter() {
        _typeSpinner.setup(ObstructionType.AREAS);
    }

    private String getUID() {
        String uid = parentFragment.getCurrentLineUID();
        if (uid == null)
            uid = UUID.randomUUID().toString();
        return uid;
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
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        PointCountLabelTV.setVisibility(View.VISIBLE);
        PointCountTV.setVisibility(View.VISIBLE);

        AllowTypeUpdate = false;
        UpdateSpinnerAdapter();
        AllowTypeUpdates();
        if (_undoBtn != null) {
            _undoBtn.setOnClickListener(new OnClickListener() {
                public void onClick(View arg0) {
                    parentFragment.undoLastPoint();
                }
            });
        }
        notifyTabHost();
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
        getActivity().getContentResolver().unregisterContentObserver(lineCO);

        if (parentFragment != null) {
            parentFragment.LineComplete(false, this);
            try {
                if (parentFragment.hardwareInterface != null)
                    parentFragment.hardwareInterface
                            .EndCurrentRoute(false);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        //when tabs switch it goes to onPause.....
    }

    @Override
    boolean newPosition(SurveyPoint sp, boolean top) {
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

    private void AddPoint2LineAllSources() {
        PointObstruction newPointObstruction = new PointObstruction(
                CurrentObstruction);

        if (parentFragment.hasActiveLine())
            _nextBtn.setVisibility(View.VISIBLE);

        _undoBtn.setVisibility(View.VISIBLE);
        newPointObstruction.type = _typeSpinner.getSelectedItem().toString();
        newPointObstruction.uid = java.util.UUID.randomUUID().toString();

        parentFragment
                .AddPoint2LineObstruction(newPointObstruction, 0.0d, true, true);
    }

    @Override
    protected void HideShowFields(String type) {
        setVisibility(DIAMETER_POSITION, View.GONE);
        setVisibility(LENGTH_POSITION, View.GONE);
        setVisibility(WIDTH_POSITION, View.GONE);
        setVisibility(ROTATION_POSITION, View.GONE);
        super.HideShowFields(type);
    }

    @Override
    protected void selectTV(int index) {
        super.selectTV(index);
        if (parentFragment == null)
            return;
        setOBState(ATSKIntentConstants.OB_STATE_REQUESTED_AREA);
    }

    @Override
    protected String getSettingModifier() {
        return "Area";
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
