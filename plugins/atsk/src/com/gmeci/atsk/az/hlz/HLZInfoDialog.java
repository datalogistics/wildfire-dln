
package com.gmeci.atsk.az.hlz;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.atakmap.android.atsk.plugin.R;
import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.log.Log;
import com.gmeci.atsk.ATSKATAKConstants;
import com.gmeci.atsk.resources.ATSKApplication;
import com.gmeci.constants.Constants;
import com.gmeci.core.ATSKConstants;
import com.gmeci.core.SurveyData;
import com.gmeci.core.SurveyData.AZ_TYPE;
import com.gmeci.core.SurveyPoint;
import com.gmeci.atsk.az.lz.RunwayInfoDialog.RunwayInfoParentInterface;
import com.gmeci.atsk.resources.CoordinateHandJamDialog.HandJamInterface;
import com.gmeci.atskservice.resolvers.AZProviderClient;
import com.gmeci.helpers.AZHelper;
import com.gmeci.conversions.Conversions;
import com.gmeci.conversions.Conversions.Unit;

import java.util.ArrayList;

public class HLZInfoDialog extends DialogFragment implements HandJamInterface {

    private static final String TAG = "HLZInfoDialog";
    private RunwayInfoParentInterface _parent;
    final OnLongClickListener ApproachLongClickListener = new OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            return false;
        }
    };
    final OnLongClickListener DepartureLongClickListener = new OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            return false;
        }
    };

    protected static final int APPROACH = 0;
    protected static final int APPROACH_ELEV = 1;
    protected static final int DEPARTURE = 2;
    protected static final int DEPARTURE_ELEV = 3;
    protected static final int CENTER = 4;
    protected static final int RADIUS = 5;
    protected static final int LENGTH = 6;
    protected static final int WIDTH = 7;
    protected static final int HEADING = 8;
    protected static final int PI_PER = 9;
    protected static final int PI_PER_ELEV = 10;
    protected static final int PI_HE = 11;
    protected static final int PI_HE_ELEV = 12;
    protected static final int PI_CDS = 13;
    protected static final int PI_CDS_ELEV = 14;
    protected static final int TV_COUNT = 15;

    protected final TextView[] _titles = new TextView[TV_COUNT];
    protected final TextView[] _values = new TextView[TV_COUNT];

    TextView _title;
    TextView ApproachCornersLeft, ApproachCornersRight,
            DepartureCornersLeft, DepartureCornersRight;
    private Button _saveBtn;
    private Button _cancelBtn;
    private AZProviderClient _azpc;
    private SurveyData _survey;
    private SharedPreferences _prefs;
    private boolean _unitsFeet = false;
    private boolean _angleTrue = true;
    private boolean _dz = false;
    private String _surveyUID;
    private String _coordFormat = Conversions.COORD_FORMAT_DM;
    private View _root;
    private final OnClickListener saveClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            Log.d(TAG, "On Save Click Listener");
            _azpc.UpdateAZ(_survey, "rwi", true);
            if (_parent != null)
                _parent.Update(true);

            HLZInfoDialog.this.dismiss();
        }
    };
    private final OnClickListener cancelClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            Log.d(TAG, "In on Cancel Click Listener");
            HLZInfoDialog.this.dismiss();
        }
    };

    public HLZInfoDialog() {
    }

    public void setupDialog(String surveyUID, AZProviderClient azpc,
            RunwayInfoParentInterface parent) {
        Log.d(TAG, "Current Survey uid: " + surveyUID);
        _surveyUID = surveyUID;
        _parent = parent;
        _azpc = azpc;
        _survey = azpc.getAZ(_surveyUID, true);

    }

    private void setupButtons() {
        _saveBtn = (Button) _root.findViewById(R.id.runway_info_save_button);
        _cancelBtn = (Button) _root
                .findViewById(R.id.runway_info_cancel_button);

        _saveBtn.setOnClickListener(saveClickListener);
        _cancelBtn.setOnClickListener(cancelClickListener);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        Context pluginContext = ATSKApplication
                .getInstance().getPluginContext();
        _root = LayoutInflater.from(pluginContext).inflate(
                R.layout.hlz_info_dlg, container, false);
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getDialog().getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setupTextViews();
        setupButtons();
        _prefs = PreferenceManager
                .getDefaultSharedPreferences(getActivity());
        _coordFormat = _prefs.getString(
                ATSKConstants.COORD_FORMAT, Conversions.COORD_FORMAT_DM);
        String unitPreference = _prefs.getString(
                ATSKConstants.UNITS_DISPLAY, ATSKConstants.UNITS_FEET);
        String anglePreference = _prefs.getString(
                ATSKConstants.UNITS_ANGLE, ATSKConstants.UNITS_ANGLE_MAG);

        _unitsFeet = unitPreference.equals(ATSKConstants.UNITS_FEET);
        _angleTrue = !anglePreference.equals(ATSKConstants.UNITS_ANGLE_MAG);

        UpdateMeasurements();
        setupButtons();
        return _root;
    }

    private void UpdateMeasurements() {

        UpdateCoordinateDisplay();

        double angDiff = 0;
        String angFmt = "T";
        if (!_angleTrue) {
            angDiff = Conversions.GetDeclination(_survey.center.lat,
                    _survey.center.lon, 0);
            angFmt = "MAG";
        }
        double head = Conversions.deg360(_survey.angle - angDiff);
        double app = Conversions.deg360(_survey.approachAngle - angDiff);
        double dep = Conversions.deg360(_survey.departureAngle - angDiff);

        setText(DEPARTURE, String.format("%.1f %s", dep, angFmt));
        setText(APPROACH, String.format("%.1f %s", app, angFmt));
        setText(HEADING, String.format("%.1f %s", head, angFmt));
    }

    private void setText(int index, String str) {
        if (index < _values.length)
            _values[index].setText(str);
    }

    private void UpdateCoordinateDisplay() {
        ArrayList<SurveyPoint> corners = AZHelper.getAZCorners(_survey);

        setText(CENTER, "Center:" + getCoordString(_survey.center));
        ApproachCornersLeft.setText("L:" + getCoordString(corners.get(2)));
        ApproachCornersRight.setText("R:" + getCoordString(corners.get(3)));
        DepartureCornersLeft.setText("L:" + getCoordString(corners.get(1)));
        DepartureCornersRight.setText("R:" + getCoordString(corners.get(0)));

        Unit uDst = _unitsFeet ? (_dz ? Unit.YARD : Unit.FOOT)
                : Unit.METER;
        Unit elevDst = _unitsFeet ? Unit.FOOT : Unit.METER;

        // PI locations
        updatePIViz();
        SurveyPoint perLoc = AZHelper.CalculatePointOfImpact(_survey, "per");
        SurveyPoint heLoc = AZHelper.CalculatePointOfImpact(_survey, "he");
        SurveyPoint cdsLoc = AZHelper.CalculatePointOfImpact(_survey, "cds");
        perLoc.setHAE(_survey.perPIElevation);
        heLoc.setHAE(_survey.hePIElevation);
        cdsLoc.setHAE(_survey.cdsPIElevation);
        setText(PI_PER, getCoordString(perLoc));
        setText(PI_HE, getCoordString(heLoc));
        setText(PI_CDS, getCoordString(cdsLoc));

        // Elevations
        SurveyPoint appPoint = AZHelper.CalculateAnchorFromAZCenter(
                _survey, _survey.center, ATSKConstants.ANCHOR_APPROACH_CENTER);
        SurveyPoint depPoint = AZHelper.CalculateAnchorFromAZCenter(
                _survey, _survey.center, ATSKConstants.ANCHOR_DEPARTURE_CENTER);
        appPoint.setHAE(_survey.approachElevation);
        depPoint.setHAE(_survey.departureElevation);
        setText(APPROACH_ELEV, appPoint.getMSLAltitude()
                .toString(elevDst, true));
        setText(DEPARTURE_ELEV,
                depPoint.getMSLAltitude().toString(elevDst, true));
        setText(PI_PER_ELEV, perLoc.getMSLAltitude().toString(elevDst, true));
        setText(PI_HE_ELEV, heLoc.getMSLAltitude().toString(elevDst, true));
        setText(PI_CDS_ELEV, cdsLoc.getMSLAltitude().toString(elevDst, true));

        // Dimensions
        setText(RADIUS, Unit.METER.format(_survey.getRadius(), uDst));
        setText(LENGTH, Unit.METER.format(_survey.getLength(), uDst));
        setText(WIDTH, Unit.METER.format(_survey.width, uDst));

    }

    private void setupTextViews() {
        _title = (TextView) _root.findViewById(R.id.hlz_info_dlg);

        LinearLayout adAngleLayout =
                (LinearLayout) _root.findViewById(R.id.ad_angles);
        LinearLayout dzPiLayout =
                (LinearLayout) _root.findViewById(R.id.dz_pi);

        _dz = _survey.getType() == AZ_TYPE.DZ;
        adAngleLayout.setVisibility(_dz ? View.GONE : View.VISIBLE);
        dzPiLayout.setVisibility(_dz && !_survey.circularAZ ? View.VISIBLE
                : View.GONE);
        _title.setText(_dz ? "DZ Info" : "HLZ Info");

        _titles[APPROACH] = (TextView) _root
                .findViewById(R.id.approach_textview);
        _values[APPROACH] = (TextView) _root.findViewById(R.id.approach_value);
        _titles[APPROACH].setOnLongClickListener(ApproachLongClickListener);
        _values[APPROACH].setOnLongClickListener(ApproachLongClickListener);

        _titles[DEPARTURE] = (TextView) _root
                .findViewById(R.id.departure_textview);
        _values[DEPARTURE] = (TextView) _root
                .findViewById(R.id.departure_value);
        _titles[DEPARTURE].setOnLongClickListener(DepartureLongClickListener);
        _values[DEPARTURE].setOnLongClickListener(DepartureLongClickListener);

        _titles[HEADING] = (TextView) _root.findViewById(R.id.heading_title);
        _values[HEADING] = (TextView) _root.findViewById(R.id.heading_value);
        _titles[HEADING].setOnLongClickListener(DepartureLongClickListener);
        _values[HEADING].setOnLongClickListener(DepartureLongClickListener);

        if (_survey.circularAZ) {
            LinearLayout rectLayout = (LinearLayout) _root
                    .findViewById(R.id.hlz_rect);
            rectLayout.setVisibility(View.GONE);
            LinearLayout circularLayout = (LinearLayout) _root
                    .findViewById(R.id.hlz_circular);
            circularLayout.setVisibility(View.VISIBLE);
        }

        _titles[CENTER] = (TextView) _root.findViewById(R.id.center_title);
        _values[CENTER] = (TextView) _root
                .findViewById(R.id.center_coordinate);
        _titles[RADIUS] = (TextView) _root.findViewById(R.id.radius_title);
        _values[RADIUS] = (TextView) _root.findViewById(R.id.radius_value);

        ApproachCornersLeft = (TextView) _root
                .findViewById(R.id.approach_corners_Left);
        ApproachCornersRight = (TextView) _root
                .findViewById(R.id.approach_corners_Right);

        DepartureCornersLeft = (TextView) _root
                .findViewById(R.id.departure_corners_Left);
        DepartureCornersRight = (TextView) _root
                .findViewById(R.id.departure_corners_Right);

        _titles[APPROACH_ELEV] = (TextView) _root
                .findViewById(R.id.app_elev_title);
        _values[APPROACH_ELEV] = (TextView) _root
                .findViewById(R.id.app_elev_value);
        _titles[DEPARTURE_ELEV] = (TextView) _root
                .findViewById(R.id.dep_elev_title);
        _values[DEPARTURE_ELEV] = (TextView) _root
                .findViewById(R.id.dep_elev_value);

        _titles[LENGTH] = (TextView) _root.findViewById(R.id.length_title);
        _values[LENGTH] = (TextView) _root.findViewById(R.id.length_value);
        _titles[WIDTH] = (TextView) _root.findViewById(R.id.width_title);
        _values[WIDTH] = (TextView) _root.findViewById(R.id.width_value);

        _titles[PI_PER] = (TextView) _root.findViewById(R.id.per_title);
        _titles[PI_PER_ELEV] = (TextView) _root
                .findViewById(R.id.per_elev_title);
        _titles[PI_HE] = (TextView) _root.findViewById(R.id.he_title);
        _titles[PI_HE_ELEV] = (TextView) _root.findViewById(R.id.he_elev_title);
        _titles[PI_CDS] = (TextView) _root.findViewById(R.id.cds_title);
        _titles[PI_CDS_ELEV] = (TextView) _root
                .findViewById(R.id.cds_elev_title);

        _values[PI_PER] = (TextView) _root.findViewById(R.id.per_loc);
        _values[PI_PER_ELEV] = (TextView) _root
                .findViewById(R.id.per_elev_value);
        _values[PI_HE] = (TextView) _root.findViewById(R.id.he_loc);
        _values[PI_HE_ELEV] = (TextView) _root.findViewById(R.id.he_elev_value);
        _values[PI_CDS] = (TextView) _root.findViewById(R.id.cds_loc);
        _values[PI_CDS_ELEV] = (TextView) _root
                .findViewById(R.id.cds_elev_value);
    }

    @Override
    public void UpdateCoordinate(double Lat, double Lon, double elevation) {
        // TODO Auto-generated method stub

    }

    @Override
    public void UpdateCoordinateFormat(String DisplayFormat) {
        // TODO Auto-generated method stub

    }

    protected String getCoordString(SurveyPoint loc) {
        return Conversions.getCoordinateString(loc.lat, loc.lon,
                _coordFormat);
    }

    private void updatePIViz() {
        if (_dz) {
            MapGroup azGroup = MapView.getMapView().getRootGroup()
                    .findMapGroup(ATSKATAKConstants.ATSK_MAP_GROUP_AZ);
            if (azGroup == null)
                return;

            MapGroup surveyGroup = azGroup.findMapGroup(_survey.uid);
            if (surveyGroup == null)
                return;

            MapItem per = surveyGroup.deepFindUID(
                    _survey.uid + "_" + Constants.POINT_PI_PER);
            MapItem he = surveyGroup.deepFindUID(
                    _survey.uid + "_" + Constants.POINT_PI_HE);
            MapItem cds = surveyGroup.deepFindUID(
                    _survey.uid + "_" + Constants.POINT_PI_CDS);

            LinearLayout perGroup = (LinearLayout) _root
                    .findViewById(R.id.dz_pi_per);
            LinearLayout heGroup = (LinearLayout) _root
                    .findViewById(R.id.dz_pi_he);
            LinearLayout cdsGroup = (LinearLayout) _root
                    .findViewById(R.id.dz_pi_cds);
            perGroup.setVisibility(per == null ? View.GONE : View.VISIBLE);
            heGroup.setVisibility(he == null ? View.GONE : View.VISIBLE);
            cdsGroup.setVisibility(cds == null ? View.GONE : View.VISIBLE);
        }
    }
}
