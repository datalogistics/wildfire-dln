
package com.gmeci.atsk.az.hlz;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.atakmap.android.ipc.AtakBroadcast.DocumentedIntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.atakmap.android.atsk.plugin.R;
import com.atakmap.coremap.log.Log;
import com.gmeci.atsk.map.ATSKShape;
import com.gmeci.atsk.obstructions.MapObstructionController;
import com.gmeci.atsk.obstructions.ObstructionController;
import com.gmeci.atsk.resources.ATSKApplication;
import com.gmeci.core.ATSKConstants;
import com.gmeci.core.ATSKIntentConstants;
import com.gmeci.core.LineObstruction;
import com.gmeci.core.PointObstruction;
import com.gmeci.atsk.az.AZTabBase;
import com.gmeci.atsk.az.AngleHandJamDialog;
import com.gmeci.atsk.az.hlz.QuadrantView.AngleCalculatedInterface;
import com.gmeci.core.SurveyPoint;
import com.gmeci.helpers.AZHelper;
import com.gmeci.conversions.Conversions;
import com.atakmap.android.ipc.AtakBroadcast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.atakmap.coremap.locale.LocaleUtil;

public class HLZQuadrantObstructionsFragment extends AZTabBase implements
        AngleHandJamDialog.AngleHandJamInterface {

    private static final String TAG = "HLZQuadrantObstructionsFragment";

    private static final int BLUE = Color.parseColor("#ff00ffff");
    private static final int RED = Color.parseColor("#ffff0000");
    private static final String APPROACH = "APPROACH";
    private static final String DEPARTURE = "DEPARTURE";
    private PointObstruction[][] badQuads;
    private final List<String> incurLines = new ArrayList<String>();
    //Approach and Departure buttons, flags, and state variables
    Button approachButton, departureButton;
    boolean collectingAppr = false;
    boolean collectingDep = false;
    //State variables dealing with ApproachDepartureFormatDialog
    String field2edit;
    String currentAngleFormat;

    //Read in from the content provider
    QuadObstructionAsyncTask qoat;
    final BroadcastReceiver DeleteReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle extras = intent.getExtras();
            String req = extras.getString(
                    ATSKIntentConstants.MENU_REQUEST, "none");
            if (req.equals(ATSKIntentConstants.MENU_DELETE))
                UpdateQuadCircle();
        }

    };
    private View _root;
    //Draw View for my quadrant picture
    private QuadrantView circleView;
    final AngleCalculatedInterface angleConsumer = new AngleCalculatedInterface() {

        @Override
        public boolean NewAngle(double ang, int action) {
            if (action == MotionEvent.ACTION_CANCEL)
                return false;
            boolean released = action == MotionEvent.ACTION_UP;
            if (collectingAppr) {
                double app = Conversions.deg360(ang);
                updateButtons(app, surveyData.departureAngle);
                if (released) {
                    surveyData.approachAngle = app;
                    azpc.UpdateAZ(surveyData, "apa", true);
                    endCollect(true);
                }
                return true;
            } else if (collectingDep) {
                double dep = Conversions.deg360(ang + 180);
                updateButtons(surveyData.approachAngle, dep);
                if (released) {
                    surveyData.departureAngle = dep;
                    azpc.UpdateAZ(surveyData, "dpu", true);
                    endCollect(false);
                }
                return true;
            }
            return false;
        }

    };

    @Override
    public void onPause() {
        if (surveyData != null)
            azpc.UpdateAZ(surveyData, "hlzquad", true);

        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getActivity()
                        .getApplicationContext());
        Editor editor = prefs.edit();

        editor.putString(ATSKConstants.UNITS_ANGLE, currentAngleFormat);
        editor.apply();

        try {
            AtakBroadcast.getInstance().unregisterReceiver(DeleteReceiver);
        } catch (Exception e) {
            Log.d(TAG, "receiver not previously registered", e);
        }
        resetGSR();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();

        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getActivity()
                        .getApplicationContext());
        currentAngleFormat = prefs.getString(ATSKConstants.UNITS_ANGLE,
                ATSKConstants.UNITS_ANGLE_MAG);

        loadCurrentSurvey();
        SetupApproachAndDepartureButtons();
        SetupDocumentedIntentFilters();
        UpdateQuadCircle();
        updateGSR();
        UpdateQuadCircle();
    }

    @Override
    protected void stopCollection() {
        endCollect(collectingAppr);
    }

    private void SetupDocumentedIntentFilters() {
        DocumentedIntentFilter deleteFilter = new DocumentedIntentFilter();
        deleteFilter
                .addAction(ATSKIntentConstants.OB_MENU_POINT_CLICK_ACTION);
        AtakBroadcast.getInstance().registerReceiver(DeleteReceiver,
                deleteFilter);
    }

    private String getAppDepString(boolean app, double rawAngle) {
        //Actual number for departure
        String name = app ? "App" : "Dep";
        double showAngle;
        char north;
        if (currentAngleFormat.equals(ATSKConstants.UNITS_ANGLE_MAG)) {//convert to mag if current format is MAG
            showAngle = Conversions.GetMagAngle(rawAngle,
                    surveyData.center.lat, surveyData.center.lon);
            north = 'M';
        } else {
            showAngle = Conversions.deg360(rawAngle);
            north = 'T';
        }
        return String.format(LocaleUtil.getCurrent(), "%s: %.1f%c%c(%s)", name,
                showAngle,
                ATSKConstants.DEGREE_SYMBOL, north,
                Conversions.GetCardinalDirection(rawAngle));
    }

    private String getAppDepString(boolean app) {
        return getAppDepString(app, app ? surveyData.approachAngle
                : surveyData.departureAngle);
    }

    private void UpdateQuadCircle() {
        if (qoat != null && !(qoat.getStatus() != AsyncTask.Status.RUNNING)) {
            qoat = new QuadObstructionAsyncTask();
            qoat.execute();
        }
        if (qoat == null) {
            qoat = new QuadObstructionAsyncTask();
            qoat.execute();
        }

        //    badQuads = new boolean[]{false, false, false, false};
        //    PointObstruction[] tmpPOs = AZHelper.CalculateQuadrantObstructions(surveyData, opc);

        //circleView.UpdateDrawing(surveyData.approachAngle, surveyData.departureAngle, badQuads);
    }

    private void SetupApproachAndDepartureButtons() {

        approachButton = (Button) _root.findViewById(R.id.approach_button);

        approachButton.setTextColor(BLUE);
        approachButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!collectingAppr) {
                    if (collectingDep)
                        endCollect(false);
                    approachButton.setBackgroundColor(ATSKConstants.LIGHT_BLUE);
                    approachButton.setTextColor(Color.WHITE);

                    approachButton.setText("Approach Waiting...");
                    collectingAppr = true;
                    setOBState(ATSKIntentConstants.OB_STATE_REQUESTED_POINT);
                } else
                    endCollect(true);
            }
        });
        approachButton.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                collectingAppr = false;
                view.setBackgroundResource(R.drawable.fullborder_background);
                field2edit = APPROACH;
                ShowAngleHandJamDialog(surveyData.approachAngle);
                return true;
            }
        });

        departureButton = (Button) _root.findViewById(R.id.departure_button);

        departureButton.setTextColor(RED);
        departureButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!collectingDep) {
                    if (collectingAppr)
                        endCollect(true);
                    departureButton
                            .setBackgroundColor(ATSKConstants.LIGHT_BLUE);
                    departureButton.setTextColor(Color.WHITE);
                    departureButton.setText("Departure Waiting...");
                    collectingDep = true;
                    setOBState(ATSKIntentConstants.OB_STATE_REQUESTED_POINT);
                } else
                    endCollect(false);
            }

        });
        departureButton.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                collectingDep = false;
                view.setBackgroundResource(R.drawable.fullborder_background);
                field2edit = DEPARTURE;
                ShowAngleHandJamDialog(surveyData.departureAngle);
                return true;
            }
        });
        approachButton.setText(getAppDepString(true));
        departureButton.setText(getAppDepString(false));

        CheckBox magArrows = (CheckBox) _root
                .findViewById(R.id.mag_arrows_inside);
        magArrows
                .setOnCheckedChangeListener(new OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(
                            CompoundButton cb, boolean check) {
                        if (surveyData != null) {
                            surveyData.setMetaBoolean(
                                    ATSKConstants.MAG_ARROWS_INSIDE, check);
                            azpc.UpdateAZ(surveyData, "", true);
                        }
                    }
                });
        magArrows.setChecked(surveyData.getMetaBoolean(
                ATSKConstants.MAG_ARROWS_INSIDE, false));

        CheckBox showInc = (CheckBox) _root
                .findViewById(R.id.show_incursions);
        showInc.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton cb, boolean check) {
                if (surveyData != null) {
                    surveyData.setMetaBoolean(ATSKConstants.SHOW_INCURSIONS,
                            check);
                    updateGSR();
                }
            }
        });
        showInc.setChecked(surveyData.getMetaBoolean(
                ATSKConstants.SHOW_INCURSIONS, true));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        Context pluginContext = ATSKApplication
                .getInstance().getPluginContext();

        LayoutInflater inf = LayoutInflater.from(pluginContext);
        _root = inf.inflate(R.layout.hlz_quad_obstructions_fragment,
                container, false);

        return _root;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        circleView = (QuadrantView) view.findViewById(R.id.quadrant);

        circleView.InitView(angleConsumer);
    }

    private void endCollect(boolean app) {
        if (app) {
            approachButton.setBackgroundResource(
                    R.drawable.fullborder_background);
            approachButton.setTextColor(BLUE);
            collectingAppr = false;
        } else {
            departureButton.setBackgroundResource(
                    R.drawable.fullborder_background);
            departureButton.setTextColor(RED);
            collectingDep = false;
        }
        updateButtons();
        setOBState(ATSKIntentConstants.OB_STATE_REQUESTED_HIDDEN);
    }

    void ShowAngleHandJamDialog(double inputAngle) {
        // TODO / FIXME   need further investigation
        // unsure how the ahjd treats this inputAngle -- when looking at the Show() method it seems to apply both types of
        // conversions to the angle.
        AngleHandJamDialog ahjd = new AngleHandJamDialog(getActivity());

        ahjd.setFormatByString(currentAngleFormat, this);
        ahjd.Show(inputAngle, surveyData.center.lat, surveyData.center.lon);
    }

    @Override
    public void newPosition(SurveyPoint sp, boolean TopCollected) {
        if (collectingAppr) {
            //Always closed after point collected
            Log.d(TAG, "calculate angle, degrees for: " + sp.lat
                    + ", " + sp.lon + " center: " + surveyData.center.lat
                    + ", " + surveyData.center.lon);
            double apprVal = Conversions.calculateAngle(sp, surveyData.center);
            Log.d(TAG, "calculated angle: " + apprVal);

            surveyData.approachAngle = Conversions.deg360(apprVal);
            azpc.UpdateAZ(surveyData, "apa", true);
            endCollect(true);
        } else if (collectingDep) {
            //Always closed after point collected
            double depVal = Conversions.calculateAngle(surveyData.center, sp);

            surveyData.departureAngle = Conversions.deg360(depVal);
            azpc.UpdateAZ(surveyData, "dpu", true);
            endCollect(false);
        }
        updateGSR();
    }

    @Override
    public void shotApproved(SurveyPoint sp, double range_m, double az_deg,
            double el_deg, boolean TopCollected) {
    }

    public void updateButtons(double app, double dep) {
        approachButton.setText(getAppDepString(true, app));
        departureButton.setText(getAppDepString(false, dep));
        updateGSR();
    }

    public void updateButtons() {
        updateButtons(surveyData.approachAngle,
                surveyData.departureAngle);
    }

    @Override
    public void UpdateDisplayMeasurements(double currentAngle,
            boolean DisplayAngleTrue) {
        if (DisplayAngleTrue)
            currentAngleFormat = ATSKConstants.UNITS_ANGLE_TRUE;
        else {
            currentAngleFormat = ATSKConstants.UNITS_ANGLE_MAG;
            currentAngle = Conversions.GetTrueAngle(currentAngle,
                    surveyData.center.lat, surveyData.center.lon);
        }

        if (field2edit.equals(APPROACH))
            surveyData.approachAngle = currentAngle;
        else
            surveyData.departureAngle = currentAngle;
        azpc.UpdateAZ(surveyData, "ada", true);

        updateButtons();
    }

    @Override
    protected void UpdateScreen() {

    }

    /**
     * Update HLZ quadrant glide-slope ratio displays
     */
    private void updateGSR() {
        // Update circular view in HLZ tab
        if (circleView != null)
            circleView.UpdateDrawing(surveyData.approachAngle,
                    surveyData.departureAngle, badQuads);

        // Highlight obstructions on map
        resetGSR();
        boolean show = surveyData.getMetaBoolean(
                ATSKConstants.SHOW_INCURSIONS, true);
        if (badQuads == null || !show)
            return;
        MapObstructionController moc = ObstructionController
                .getInstance().getMapController();
        for (int i = 0; i < badQuads.length; i++) {
            if (badQuads[i] == null)
                continue;
            for (int j = 0; j < badQuads[i].length; j++) {
                PointObstruction po = badQuads[i][j];
                // Draw line from survey center to each incursion
                LineObstruction line = new LineObstruction();
                line.uid = ATSKConstants.GSR_MARKER + i + "_LINE_" + j;
                line.type = ATSKConstants.GSR_MARKER;
                line.remarks = "";
                line.points.add(surveyData.center);
                line.points.add(po);
                moc.UpdateLine(line);
                synchronized (incurLines) {
                    incurLines.add(line.uid);
                }
                if (j == 0) {
                    // Highlight worst incursion
                    LineObstruction marker = new LineObstruction();
                    marker.remarks = "";
                    marker.type = ATSKConstants.GSR_MARKER;
                    marker.uid = ATSKConstants.GSR_MARKER + i;
                    marker.filled = true;
                    po.width = Math.max(po.width, 10);
                    po.length = Math.max(po.length, 10);
                    Collections.addAll(marker.points,
                            po.getCorners(true));
                    // Add box to map directly (no database)
                    moc.UpdateLine(marker);
                    synchronized (incurLines) {
                        incurLines.add(marker.uid);
                    }
                }
            }
        }
    }

    /**
     * Remove GSR map indicators (red boxes + lines)
     */
    private void resetGSR() {
        synchronized (incurLines) {
            for (String uid : incurLines) {
                ATSKShape item = ATSKShape.find(uid);
                if (item != null && item.getGroup() != null)
                    item.getGroup().removeItem(item);
            }
            incurLines.clear();
        }
    }

    class QuadObstructionAsyncTask extends AsyncTask<Integer, Integer, Integer> {
        PointObstruction[][] tmpPOs;

        @Override
        protected void onPreExecute() {
            badQuads = new PointObstruction[4][];
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Integer result) {
            badQuads = tmpPOs;
            updateGSR();
            super.onPostExecute(result);
        }

        @Override
        protected Integer doInBackground(Integer... params) {
            tmpPOs = AZHelper.CalculateQuadrantObstructions(surveyData, opc);
            return null;
        }
    }

}
