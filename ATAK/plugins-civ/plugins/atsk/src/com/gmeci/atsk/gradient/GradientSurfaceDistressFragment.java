
package com.gmeci.atsk.gradient;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.atakmap.android.atsk.plugin.R;
import com.atakmap.coremap.log.Log;
import com.gmeci.atsk.resources.ATSKApplication;
import com.gmeci.core.ATSKConstants;
import com.gmeci.core.ATSKIntentConstants;
import com.gmeci.core.PointObstruction;
import com.gmeci.core.SurveyData;
import com.gmeci.atsk.gradient.surfacedistress.SurfaceDistressCursorAdapter;
import com.gmeci.atsk.gradient.surfacedistress.SurfaceDistressSeveritySpinner;
import com.gmeci.atsk.gradient.surfacedistress.SurfaceDistressTypeSpinner;
import com.gmeci.atsk.resources.ATSKDialogManager.ConfirmInterface;
import com.gmeci.atsk.resources.CoordinateHandJamDialog;
import com.gmeci.atsk.resources.CoordinateHandJamDialog.HandJamInterface;
import com.gmeci.atskservice.resolvers.ObstructionProviderClient;
import com.gmeci.core.SurveyPoint;
import com.gmeci.helpers.ObstructionHelper;
import com.gmeci.constants.Constants;
import com.gmeci.conversions.Conversions;

public class GradientSurfaceDistressFragment extends GradientTabBase implements
        HandJamInterface {

    @SuppressWarnings("unused")
    private final static String TAG = "GradientSurfaceDistressFragment";

    ObstructionProviderClient opc;
    SurfaceDistressTypeSpinner surfaceDistressTypeSpinner;
    SurfaceDistressSeveritySpinner surfaceDistressSeveritySpinner;
    String DisplayCoordinateFormat = Conversions.COORD_FORMAT_MGRS;

    Button AddDistressButton;
    final TextView[] DistressTVs = new TextView[2];
    String CurrentDistress = Constants.DISTRESS_POTHOLE;
    int CurrentDistressLevel = 0;
    int selectedPosition = -10;
    String GroupName2Confirm, Name2Confirm;
    boolean LocationSelected = false;
    double CurrentLat = -91, CurrentLon = -181;
    double CurrentAlt_m = 0;

    SurfaceDistressCursorAdapter adapter = null;
    Cursor distressCursor = null;

    Context pluginContext;
    SurveyData CurrentSurvey;
    ConfirmInterface DeleteDistressConfirmInterface = new ConfirmInterface() {
        @Override
        public void ConfirmResponse(String Type, boolean Confirmed) {
            if (Confirmed) {

                opc.DeletePoint(GroupName2Confirm, Name2Confirm, true);
            }

        }
    };
    private final OnLongClickListener LocationLongClickListener = new OnLongClickListener() {
        public boolean onLongClick(View v) {
            CoordinateHandJamDialog chjd = new CoordinateHandJamDialog();
            chjd.Initialize(CurrentLat, CurrentLon,
                    DisplayCoordinateFormat, CurrentAlt_m,
                    GradientSurfaceDistressFragment.this);
            return true;
        }
    };
    private final OnClickListener LocationClickListener = new OnClickListener() {
        public void onClick(View v) {

            boolean Visible = ObstructionBarVisible();
            if (!Visible) {
                boolean Success = setOBState(ATSKIntentConstants.OB_STATE_REQUESTED_POINT);
                if (Success) {
                    for (int i = 0; i < 2; i++) {
                        DistressTVs[i]
                                .setBackgroundResource(R.drawable.background_selected_center);
                    }
                    LocationSelected = true;
                }
            } else {

                setOBState(ATSKIntentConstants.OB_STATE_HIDDEN);
                for (int i = 0; i < 2; i++) {
                    DistressTVs[i].setBackgroundColor(NON_SELECTED_BG_COLOR);

                }
                LocationSelected = false;
            }
        }
    };

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        opc = new ObstructionProviderClient(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        pluginContext = ATSKApplication.getInstance().getPluginContext();
        return LayoutInflater.from(pluginContext).inflate(
                R.layout.gradient_surface_distressis,
                container, false);
    }

    @Override
    public void onPause() {
        super.onPause();

        opc.DeletePoint(ATSKConstants.DISTRESS_GROUP,
                ATSKConstants.TEMP_POINT_UID, true);
        opc.Stop();

        ATSKApplication.setObstructionCollectionMethod(
                ATSKIntentConstants.OB_STATE_HIDDEN,
                "GradientCollection", false);
    }

    @Override
    public void onResume() {
        super.onResume();
        UpdadingGradient = false;

        CurrentSurvey = azpc.getAZ(
                azpc.getSetting(ATSKConstants.CURRENT_SURVEY, "Distress Frag"),
                false);
        opc.Start();
        UpdateGradientList();

    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        AddDistressButton = (Button) view.findViewById(R.id.add_distress);
        AddDistressButton.setVisibility(View.GONE);

        AddDistressButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                //get current GPS position and drop an obstruction there...
                if (CurrentLat < -90 || CurrentLon < -180) {
                    Toast.makeText(
                            getActivity(),
                            "Invalid coordinates for Distress\nTap Location and collect position",
                            Toast.LENGTH_LONG).show();
                    return;
                }
                //int Count = opc.GetPointCount(ATSKConstants.DISTRESS_GROUP);

                PointObstruction DistressPoint = new PointObstruction();
                DistressPoint.setSurveyPoint(CurrentLat, CurrentLon);
                DistressPoint.setHAE(CurrentAlt_m);
                DistressPoint.remark = CurrentDistress + "_"
                        + CurrentDistressLevel;
                DistressPoint.group = ATSKConstants.DISTRESS_GROUP;
                DistressPoint.height = 0;
                DistressPoint.uid = java.util.UUID.randomUUID().toString(); //ATSKConstants.DISTRESS_GROUP + Count;
                DistressPoint.type = CurrentDistress + "_"
                        + CurrentDistressLevel;
                DistressPoint.visible = true;

                opc.NewPoint(DistressPoint);
                opc.DeletePoint(ATSKConstants.DISTRESS_GROUP,
                        ATSKConstants.TEMP_POINT_UID, true);
                UpdateGradientList();

                AddDistressButton.setVisibility(View.GONE);

            }

        });

        SetupSpinners(view);

        DistressTVs[0] = (TextView) view.findViewById(R.id.Location_static);
        DistressTVs[1] = (TextView) view.findViewById(R.id.Location);

        for (int i = 0; i < 2; i++) {
            DistressTVs[i].setClickable(true);
            DistressTVs[i].setLongClickable(true);

            DistressTVs[i].setOnLongClickListener(LocationLongClickListener);
            DistressTVs[i].setOnClickListener(LocationClickListener);
        }

        notifyTabHost();
    }

    public void UpdateGradientList() {

        Cursor oldcursor = distressCursor;

        distressCursor = ObstructionHelper
                .GetDistressFilteredPointCursor(opc, CurrentSurvey);

        if (distressCursor != null) {
            Log.d(TAG, "Got distress Cursor " + distressCursor.getCount());

            if (adapter != null)
                Log.d(TAG, "adapter already created, may be a cursor leak here");

            adapter = new SurfaceDistressCursorAdapter(getActivity(),
                    distressCursor);

            setListAdapter(adapter);

        }
        if (oldcursor != null)
            oldcursor.close();
    }

    private void SetupSpinners(View view) {

        surfaceDistressSeveritySpinner = (SurfaceDistressSeveritySpinner) view
                .findViewById(R.id.distress_severity_spinner);
        surfaceDistressSeveritySpinner
                .setOnItemSelectedListener(new OnItemSelectedListener() {

                    public void onItemSelected(AdapterView<?> parentView,
                            View view, int pos, long arg3) {
                        CurrentDistressLevel = pos;
                        updateDistress();
                    }

                    public void onNothingSelected(AdapterView<?> arg0) {
                    }

                });

        surfaceDistressTypeSpinner = (SurfaceDistressTypeSpinner) view
                .findViewById(R.id.distress_type_spinner);
        surfaceDistressTypeSpinner
                .setOnItemSelectedListener(new OnItemSelectedListener() {

                    public void onItemSelected(AdapterView<?> parentView,
                            View view, int pos, long arg3) {
                        CurrentDistress = parentView.getItemAtPosition(pos)
                                .toString();
                        //Update Distress SEverity display
                        surfaceDistressSeveritySpinner
                                .SetupSpinner(CurrentDistress);
                        updateDistress();
                    }

                    public void onNothingSelected(AdapterView<?> arg0) {
                    }

                });

    }

    protected void updateDistress() {
        //draw temp point?
        PointObstruction DistressPoint = new PointObstruction();
        DistressPoint.setSurveyPoint(CurrentLat, CurrentLon);
        DistressPoint.setHAE(CurrentAlt_m);
        DistressPoint.remark = CurrentDistress + "_TEMP";
        DistressPoint.group = ATSKConstants.DISTRESS_GROUP;
        DistressPoint.height = 0;
        DistressPoint.uid = ATSKConstants.TEMP_POINT_UID;
        DistressPoint.type = CurrentDistress + "_" + CurrentDistressLevel;
        DistressPoint.visible = true;
        AddDistressButton.setVisibility(View.VISIBLE);
        if (opc != null)
            opc.NewPoint(DistressPoint);
        //Update display
        UpdateDisplayMeasurements();
    }

    public void newPosition(SurveyPoint sp) {
        if (!LocationSelected)
            return;
        CurrentLat = sp.lat;
        CurrentLon = sp.lon;
        CurrentAlt_m = sp.getHAE();
        updateDistress();
    }

    @Override
    public void UpdateCoordinate(double Lat, double Lon, double elevation) {
        CurrentLat = Lat;
        CurrentLon = Lon;
        CurrentAlt_m = (float) elevation;
        UpdateDisplayMeasurements();

    }

    protected void UpdateDisplayMeasurements() {

        try {
            String CoordinateString = Conversions.getCoordinateString(
                    CurrentLat,
                    CurrentLon, DisplayCoordinateFormat);
            if (DistressTVs != null && DistressTVs[1] != null)
                DistressTVs[1].setText(CoordinateString);
        } catch (NullPointerException npe) {
            Log.d(TAG, "invalid coordinates used", new Exception());

        }
    }

    @Override
    public void UpdateCoordinateFormat(String DisplayFormat) {
        DisplayCoordinateFormat = DisplayFormat;
        UpdateDisplayMeasurements();
    }

}
