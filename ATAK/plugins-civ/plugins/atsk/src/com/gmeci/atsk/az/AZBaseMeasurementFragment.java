
package com.gmeci.atsk.az;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.RadioGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup.OnCheckedChangeListener;

import com.atakmap.android.atsk.plugin.R;
import com.atakmap.android.elev.dt2.Dt2ElevationModel;
import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.PointMapItem;
import com.atakmap.android.maps.PointMapItem.OnPointChangedListener;
import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.coords.Altitude;
import com.gmeci.atsk.ATSKATAKConstants;
import com.gmeci.atsk.MapHelper;
import com.gmeci.atsk.map.ATSKMarker;
import com.gmeci.atsk.resources.ATSKApplication;
import com.gmeci.constants.Constants;
import com.gmeci.core.ATSKConstants;
import com.gmeci.core.ATSKIntentConstants;
import com.gmeci.core.SurveyData;
import com.gmeci.core.SurveyData.AZ_TYPE;
import com.gmeci.core.SurveyPoint;
import com.gmeci.atsk.resources.ATSKDialogManager;
import com.gmeci.atsk.resources.ATSKDialogManager.DialogUpdateInterface;
import com.gmeci.atsk.resources.CoordinateHandJamDialog;
import com.gmeci.atsk.resources.CoordinateHandJamDialog.HandJamInterface;
import com.gmeci.helpers.AZHelper;
import com.gmeci.conversions.Conversions;
import com.gmeci.conversions.Conversions.Unit;
import com.gmeci.atsk.resources.LCRButton;
import com.gmeci.atsk.resources.LCRButton.CollectionSide;

abstract public class AZBaseMeasurementFragment extends AZTabBase implements
        HandJamInterface, DialogUpdateInterface {

    public static final String LZ_ONE_POINT = "LZ_ONE_POINT";
    public static final String LZ_TWO_POINT = "LZ_TWO_POINT";
    public static final String DZ = "DZ";
    public static final String HLZ = "HLZ";
    protected static final double SELECTED_SIZE_MULTIPLIER = 1.25f;
    protected static final int CENTER_POSITION = 0;
    protected static final int RADIUS_POSITION = 1;
    protected static final int ANCHOR_POSITION = 2;
    protected static final int LENGTH_POSITION = 3;
    protected static final int ELEVATION_POSITION = 4;
    protected static final int WIDTH_POSITION = 5;
    protected static final int DRAG_POSITION = 6;
    protected static final int SLOPE_L_POSITION = 7;
    protected static final int SLOPE_W_POSITION = 8;
    protected static final int DEPARTURE_POSITION = 9;
    protected static final int APP_ELEVATION_POSITION = 10;
    protected static final int DEP_ELEVATION_POSITION = 11;
    protected static final int DEP_THRESHOLD = 12;
    protected static final int APP_THRESHOLD = 13;
    private static final String TAG = "AZBaseMeasurementFragment";
    //standard fragment start (view and inflator)
    protected View _root;
    protected RadioGroup typeToggle;
    protected String AZTypeToDisplay;
    protected boolean StandardUnitsFeet = false;
    protected final TextView[] LabelTV = new TextView[14];
    protected final TextView[] ValueTV = new TextView[14];
    protected final TextView[] UnitsTV = new TextView[14];

    protected LinearLayout _rectButtons;
    protected LCRButton _alignBtn;
    protected Button _setupBtn, _cancelSetup;
    protected int _setupStep = -1;
    protected boolean _surveyNeedsUpdate = true;

    protected int CurrentlyEditedIndex = -1;
    protected int StoredPositionIndex = 0;
    protected final SurveyPoint StoredPosition = new SurveyPoint();

    private final OnClickListener TVClickListener = new OnClickListener() {
        public void onClick(View v) {

            //boolean Visible = isOBVisible();
            UpdateDisplayMeasurements(false);

            for (int i = 0; i < LabelTV.length; i++) {
                if (LabelTV[i] == null)
                    continue;

                int UnitsTVId = -1;
                if (UnitsTV[i] != null)
                    UnitsTVId = UnitsTV[i].getId();

                boolean same = v.getId() == LabelTV[i].getId()
                        || v.getId() == ValueTV[i].getId()
                        || v.getId() == UnitsTVId;

                if (same) {
                    toggleTV(i);
                    break;
                }
            }
        }
    };
    private final OnLongClickListener TVLongClickListener = new OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            CurrentlyEditedIndex = -1;
            UpdateDisplayMeasurements(false);

            if (LabelTV[CENTER_POSITION] != null
                    && (v.getId() == LabelTV[CENTER_POSITION].getId() || v
                            .getId() == ValueTV[CENTER_POSITION].getId())) {
                CurrentlyEditedIndex = CENTER_POSITION;
                ShowCoordinateHandJamDialog(surveyData.center.lat,
                        surveyData.center.lon, surveyData.center.getHAE());
            } else if (LabelTV[RADIUS_POSITION] != null
                    && (v.getId() == LabelTV[RADIUS_POSITION].getId()
                            || v.getId() == ValueTV[RADIUS_POSITION].getId() || v
                            .getId() == UnitsTV[RADIUS_POSITION].getId())) {
                CurrentlyEditedIndex = RADIUS_POSITION;
                ShowMeasurementHandJamDialog(surveyData.getRadius(),
                        "Radius");
            } else if (LabelTV[ANCHOR_POSITION] != null
                    && (v.getId() == LabelTV[ANCHOR_POSITION].getId() || v
                            .getId() == ValueTV[ANCHOR_POSITION].getId())) {
                CurrentlyEditedIndex = ANCHOR_POSITION;

                if (AZTypeToDisplay
                        .equals(AZBaseMeasurementFragment.LZ_TWO_POINT)) {
                    SurveyPoint AnchorPoint = AZHelper
                            .CalculateAnchorFromAZCenter(surveyData,
                                    surveyData.center,
                                    ATSKConstants.ANCHOR_APPROACH_CENTER);
                    ShowCoordinateHandJamDialog(AnchorPoint.lat,
                            AnchorPoint.lon, AnchorPoint.getHAE());
                } else {
                    SurveyPoint AnchorPoint = AZHelper
                            .CalculateAnchorFromAZCenter(surveyData,
                                    surveyData.center,
                                    surveyData.getApproachAnchor());
                    ShowCoordinateHandJamDialog(AnchorPoint.lat,
                            AnchorPoint.lon, AnchorPoint.getHAE());
                }

            } else if (LabelTV[DEPARTURE_POSITION] != null
                    && (v.getId() == LabelTV[DEPARTURE_POSITION].getId() || v
                            .getId() == ValueTV[DEPARTURE_POSITION].getId())) {
                CurrentlyEditedIndex = DEPARTURE_POSITION;
                SurveyPoint AnchorPoint = AZHelper.CalculateAnchorFromAZCenter(
                        surveyData, surveyData.center,
                        surveyData.getDepartureAnchor());
                ShowCoordinateHandJamDialog(AnchorPoint.lat, AnchorPoint.lon,
                        AnchorPoint.getHAE());
            } else if (LabelTV[LENGTH_POSITION] != null
                    && (v.getId() == LabelTV[LENGTH_POSITION].getId()
                            || v.getId() == ValueTV[LENGTH_POSITION].getId() || v
                            .getId() == UnitsTV[LENGTH_POSITION].getId())) {
                CurrentlyEditedIndex = LENGTH_POSITION;
                ShowMeasurementHandJamDialog(surveyData.getLength(false),
                        "Length");
            } else if (LabelTV[WIDTH_POSITION] != null
                    && (v.getId() == LabelTV[WIDTH_POSITION].getId()
                            || v.getId() == ValueTV[WIDTH_POSITION].getId() || v
                            .getId() == UnitsTV[WIDTH_POSITION].getId())) {
                CurrentlyEditedIndex = WIDTH_POSITION;
                ShowMeasurementHandJamDialog(surveyData.width, "Width");
            } else if (LabelTV[ELEVATION_POSITION] != null
                    && (v.getId() == LabelTV[ELEVATION_POSITION].getId()
                            || v.getId() == ValueTV[ELEVATION_POSITION].getId() || v
                            .getId() == UnitsTV[ELEVATION_POSITION].getId())) {
                CurrentlyEditedIndex = ELEVATION_POSITION;
                ShowMeasurementHandJamDialog(surveyData.center.getMSL(),
                        "Elevation (msl)", true);
            } else if (LabelTV[DRAG_POSITION] != null
                    && (v.getId() == LabelTV[DRAG_POSITION].getId()
                            || v.getId() == ValueTV[DRAG_POSITION].getId() || v
                            .getId() == UnitsTV[DRAG_POSITION].getId())) {
                CurrentlyEditedIndex = DRAG_POSITION;
                ShowAngleHandJamDialog(surveyData.angle);
            } else if (LabelTV[SLOPE_W_POSITION] != null
                    && (v.getId() == LabelTV[SLOPE_W_POSITION].getId() || v
                            .getId() == ValueTV[SLOPE_W_POSITION].getId())) {
                CurrentlyEditedIndex = SLOPE_W_POSITION;

                ATSKDialogManager adm = new ATSKDialogManager(getActivity(),
                        AZBaseMeasurementFragment.this);
                adm.ShowTextHandJamDialog(
                        String.format("%.1f", surveyData.slopeW),
                        "Slope Across Width %", CurrentlyEditedIndex);
            } else if (LabelTV[SLOPE_L_POSITION] != null
                    && (v.getId() == LabelTV[SLOPE_L_POSITION].getId() || v
                            .getId() == ValueTV[SLOPE_L_POSITION].getId())) {
                CurrentlyEditedIndex = SLOPE_L_POSITION;

                ATSKDialogManager adm = new ATSKDialogManager(getActivity(),
                        AZBaseMeasurementFragment.this);
                adm.ShowTextHandJamDialog(
                        String.format("%.1f", surveyData.slopeL),
                        "Slope Across Length %", CurrentlyEditedIndex);
            } else if (LabelTV[APP_ELEVATION_POSITION] != null
                    && (v.getId() == LabelTV[APP_ELEVATION_POSITION].getId()
                            || v.getId() == ValueTV[APP_ELEVATION_POSITION]
                                    .getId() || v.getId() == UnitsTV[APP_ELEVATION_POSITION]
                            .getId())) {
                CurrentlyEditedIndex = APP_ELEVATION_POSITION;
                ShowMeasurementHandJamDialog(
                        (float) Conversions.ConvertHAEtoMSL(
                                surveyData.center.lat, surveyData.center.lon,
                                surveyData.approachElevation),
                        "Elevation (msl)", false);
            } else if (LabelTV[DEP_ELEVATION_POSITION] != null
                    && (v.getId() == LabelTV[DEP_ELEVATION_POSITION].getId()
                            || v.getId() == ValueTV[DEP_ELEVATION_POSITION]
                                    .getId() || v.getId() == UnitsTV[DEP_ELEVATION_POSITION]
                            .getId())) {
                CurrentlyEditedIndex = DEP_ELEVATION_POSITION;
                ShowMeasurementHandJamDialog(
                        (float) Conversions.ConvertHAEtoMSL(
                                surveyData.center.lat, surveyData.center.lon,
                                surveyData.departureElevation),
                        "Elevation (msl)", false);
            } else if (LabelTV[DEP_THRESHOLD] != null
                    && (v.getId() == LabelTV[DEP_THRESHOLD].getId()
                            || v.getId() == ValueTV[DEP_THRESHOLD].getId() || v
                            .getId() == UnitsTV[DEP_THRESHOLD].getId())) {
                CurrentlyEditedIndex = DEP_THRESHOLD;
                ShowMeasurementHandJamDialog(
                        surveyData.edges.DepartureOverrunLength_m,
                        "Departure Threshold");
            } else if (LabelTV[APP_THRESHOLD] != null
                    && (v.getId() == LabelTV[APP_THRESHOLD].getId()
                            || v.getId() == ValueTV[APP_THRESHOLD].getId() || v
                            .getId() == UnitsTV[APP_THRESHOLD].getId())) {
                CurrentlyEditedIndex = APP_THRESHOLD;
                ShowMeasurementHandJamDialog(
                        surveyData.edges.ApproachOverrunLength_m,
                        "Approach Threshold");

            } else {
                Log.d(TAG, "Long Click  Missed!!!!!!!!!!!!!!!!!!!!!!!!!!");
                return false;
            }
            setOBState(ATSKIntentConstants.OB_STATE_REQUESTED_HIDDEN);
            return true;
        }
    };

    public AZBaseMeasurementFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        Context pluginContext = ATSKApplication
                .getInstance().getPluginContext();
        _root = LayoutInflater.from(pluginContext).inflate(
                R.layout.dz_crit_meas_fragment, container,
                false);
        return _root;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    public void SetSurveyInterface() {
        super.SetSurveyInterface();

    }

    @Override
    public void onResume() {
        super.onResume();
        loadSurvey();
        //surveyData.AnchorPosition = ATSKConstants.ANCHOR_APPROACH_CENTER;
        //read preferences for the coordinate format
        DisplayCoordinateFormat = user_settings.getString(
                ATSKConstants.COORD_FORMAT, Conversions.COORD_FORMAT_MGRS);
    }

    @Override
    public void onPause() {
        super.onPause();
        CurrentlyEditedIndex = -1;
        user_settings.edit()
                .putString(ATSKConstants.COORD_FORMAT, DisplayCoordinateFormat)
                .apply();
        endAutoSetup();

        // Survey may be modified in other tab
        _surveyNeedsUpdate = true;

        removeAnchorListeners();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupToggleButtons();
        setupCircularTextViews();
        setupRectangularTextViews();

        ValueTV[ELEVATION_POSITION] = (TextView) _root
                .findViewById(R.id.dz_crit_meas_text_height_val);
        LabelTV[ELEVATION_POSITION] = (TextView) _root
                .findViewById(R.id.dz_crit_meas_text_height_label);
        UnitsTV[ELEVATION_POSITION] = (TextView) _root
                .findViewById(R.id.dz_crit_meas_text_height_units);

        LabelTV[SLOPE_L_POSITION] = (TextView) _root
                .findViewById(R.id.hlz_slope_label_l);
        ValueTV[SLOPE_L_POSITION] = (TextView) _root
                .findViewById(R.id.hlz_slope_value_l);
        LabelTV[SLOPE_W_POSITION] = (TextView) _root
                .findViewById(R.id.hlz_slope_label_w);
        ValueTV[SLOPE_W_POSITION] = (TextView) _root
                .findViewById(R.id.hlz_slope_value_w);

        setupDetailsButton(_root);
        setupTVClickListeners();
    }

    private void loadSurvey() {
        loadCurrentSurvey();
        _surveyNeedsUpdate = false;

        if (surveyData != null) {
            if (surveyData.circularAZ
                    && (surveyData.getType() == AZ_TYPE.DZ || surveyData
                            .getType() == AZ_TYPE.HLZ)) {

                if (typeToggle != null)
                    ((RadioButton) _root.findViewById(R.id.radioCircular))
                            .setChecked(true);
                setToggleState(true, false);
            } else {
                if (typeToggle != null)
                    ((RadioButton) _root.findViewById(R.id.radioRectangular))
                            .setChecked(true);

                setToggleState(false, false);
            }

            if (_alignBtn != null)
                _alignBtn.setSelectionSide(surveyData.AnchorPosition);

        } else {//no DZ for some reason - fake it?
            if (typeToggle != null)
                ((RadioButton) _root.findViewById(R.id.radioRectangular))
                        .setChecked(true);
            //    dzCurrentAnchor = 0;
            setToggleState(false, true);
        }
        UpdateDisplayMeasurements(false);
        addAnchorListeners();
    }

    // Fine-adjust movement listeners
    protected void addAnchorListeners() {
        if (surveyData == null)
            return;

        MapGroup azGroup = MapView.getMapView().getRootGroup()
                .findMapGroup(ATSKATAKConstants.ATSK_MAP_GROUP_AZ);
        if (azGroup == null) {
            Log.w(TAG, "AZ group not found: "
                    + ATSKATAKConstants.ATSK_MAP_GROUP_AZ);
            return;
        }

        MapGroup surveyGroup = azGroup.findMapGroup(surveyData.uid);
        if (surveyGroup == null) {
            Log.w(TAG, "survey group not found: " + surveyData.uid);
            return;
        }

        surveyGroup.addOnItemListChangedListener(_anchorItemChanged);
        surveyGroup.forEachItem(
                new MapGroup.MapItemsCallback() {
                    public boolean onItemFunction(MapItem item) {
                        addAnchorListener(item);
                        return false;
                    }
                });
    }

    protected void removeAnchorListeners() {
        if (surveyData == null)
            return;

        MapGroup azGroup = MapView.getMapView().getRootGroup()
                .findMapGroup(ATSKATAKConstants.ATSK_MAP_GROUP_AZ);
        if (azGroup == null)
            return;

        MapGroup surveyGroup = azGroup.findMapGroup(surveyData.uid);
        if (surveyGroup == null)
            return;
        surveyGroup.removeOnItemListChangedListener(_anchorItemChanged);
        surveyGroup.forEachItem(
                new MapGroup.MapItemsCallback() {
                    public boolean onItemFunction(MapItem item) {
                        removeAnchorListener(item);
                        return false;
                    }
                });
    }

    private final OnPointChangedListener _opcl = new OnPointChangedListener() {
        public void onPointChanged(PointMapItem pmi) {
            if (pmi == null || pmi.getPoint() == null)
                return;
            setAnchorPos(anchorIndex(pmi), MapHelper
                    .convertGeoPoint2SurveyPoint(pmi.getPoint()), 1.0, true);
        }
    };

    private final MapGroup.OnItemListChangedListener _anchorItemChanged = new MapGroup.OnItemListChangedListener() {
        @Override
        public void onItemAdded(MapItem item, MapGroup group) {
            addAnchorListener(item);
        }

        @Override
        public void onItemRemoved(MapItem item, MapGroup group) {
        }
    };

    private void addAnchorListener(MapItem item) {
        if (anchorIndex(item) > -1) {
            ((ATSKMarker) item).addOnPointChangedListener(_opcl);
            item.setClickable(true);
        }
    }

    private void removeAnchorListener(MapItem item) {
        if (anchorIndex(item) > -1) {
            ((ATSKMarker) item).removeOnPointChangedListener(_opcl);
            item.setClickable(false);
        }
    }

    protected int anchorIndex(MapItem item) {
        if (item != null && item instanceof ATSKMarker) {
            String uid = item.getUID();
            if (uid.contains(Constants.POINT_ANCHOR)) {
                try {
                    return Integer.parseInt(
                            uid.substring(uid.lastIndexOf("_") + 1));
                } catch (Exception e) {
                    return -1;
                }
            }
        }
        return -1;
    }

    /**
     * Recalculate survey bounds based on anchor position
     * @param anchor Anchor index
     * @param pos Position of anchor
     * @param tol Range from old to new must be >tol to update (0.0 to ignore)
     * @param lockAngle True to adjust length/width rather than length/angle
     */
    protected void setAnchorPos(int anchor, SurveyPoint pos, double tol,
            boolean lockAngle) {
        if (anchor == -1)
            return;

        SurveyPoint old = AZHelper.CalculateAnchorFromAZCenter(
                surveyData, surveyData.center, anchor);
        double range = Conversions.CalculateRangem(
                old.lat, old.lon, pos.lat, pos.lon);

        if (range < tol)
            return;

        if (anchor == ATSKConstants.ANCHOR_CENTER) {
            surveyData.center.setSurveyPoint(pos);
            UpdateDisplayMeasurements(true);
            return;
        }

        // Same-side main anchor, opposite-side main anchor, opposite anchor
        int curAnchor, oppCurAnchor, oppAnchor;
        int angToCur;
        int widthScale = 1;
        int oldEdit = CurrentlyEditedIndex;
        boolean app;

        if (anchor <= ATSKConstants.ANCHOR_APPROACH_RIGHT) {
            curAnchor = surveyData.getApproachAnchor();
            oppCurAnchor = surveyData.getDepartureAnchor();
            CurrentlyEditedIndex = ANCHOR_POSITION;
            oppAnchor = anchor + 4;
            angToCur = (anchor < curAnchor ? -90 : 90);
            if (anchor == ATSKConstants.ANCHOR_APPROACH_CENTER
                    || curAnchor == ATSKConstants.ANCHOR_APPROACH_CENTER)
                widthScale = 2;
            app = true;
        } else {
            curAnchor = surveyData.getDepartureAnchor();
            oppCurAnchor = surveyData.getApproachAnchor();
            CurrentlyEditedIndex = DEPARTURE_POSITION;
            oppAnchor = anchor - 4;
            angToCur = (anchor < curAnchor ? 90 : -90);
            if (anchor == ATSKConstants.ANCHOR_DEPARTURE_CENTER
                    || curAnchor == ATSKConstants.ANCHOR_DEPARTURE_CENTER)
                widthScale = 2;
            app = false;
        }

        if (anchor != curAnchor) {
            // New angle to survey anchor = angle to opposing anchor +/-90
            SurveyPoint anc = AZHelper.CalculateAnchorFromAZCenter(
                    surveyData, surveyData.center, curAnchor);
            SurveyPoint opp = AZHelper.CalculateAnchorFromAZCenter(
                    surveyData, surveyData.center, oppAnchor);
            double opp_ra[] = Conversions.CalculateRangeAngle(
                    opp.lat, opp.lon, pos.lat, pos.lon);
            double ancAngle;
            double offset[];

            if (lockAngle) {
                // Width and length needed
                double anc_ra[] = Conversions.CalculateRangeAngle(pos.lat,
                        pos.lon, anc.lat, anc.lon);
                anc_ra[1] = Conversions.deg360(anc_ra[1] - surveyData.angle);
                opp_ra[1] = Conversions.deg360(opp_ra[1] - surveyData.angle);
                double width = Math.abs(anc_ra[0] *
                        Math.sin(Math.toRadians(anc_ra[1])) * widthScale);
                double length = Math.abs(opp_ra[0] *
                        Math.cos(Math.toRadians(opp_ra[1])));

                // Calculate new position for same-side main anchor
                SurveyPoint oppAnc = AZHelper.CalculateAnchorFromAZCenter(
                        surveyData, surveyData.center, oppCurAnchor);
                offset = Conversions.AROffset(oppAnc.lat, oppAnc.lon,
                        surveyData.angle + (app ? 180 : 0), length);

                // Set width then recalculate survey center
                surveyData.setWidth(width);
                surveyData.center = AZHelper.CalculateLZCenter(
                        surveyData, app ? anc : oppAnc);
            } else {
                // Angle and length needed
                double ancRange = Conversions.CalculateRangem(
                        old.lat, old.lon, anc.lat, anc.lon);
                ancAngle = Conversions.deg360(angToCur + opp_ra[1]);
                offset = Conversions.AROffset(pos.lat, pos.lon,
                        ancAngle, ancRange);
            }

            // Update survey anchor point
            pos.setSurveyPoint(offset[0], offset[1]);
            Altitude dted = Dt2ElevationModel.getInstance()
                    .queryPoint(offset[0], offset[1]);
            pos.alt = MapHelper.convertAltitude(dted);
        }

        newPosition(pos, false);
        CurrentlyEditedIndex = oldEdit;
    }

    protected void UpdateSurvey(boolean AllowDBUpdate) {

        if (!azpc.isStarted())
            azpc.Start();
        azpc.UpdateAZ(surveyData, "cud", AllowDBUpdate);
    }

    private void setupToggleButtons() {

        typeToggle = (RadioGroup) _root
                .findViewById(R.id.dz_crit_meas_type);

        if (typeToggle != null) {
            typeToggle
                    .setOnCheckedChangeListener(new OnCheckedChangeListener() {

                        @Override
                        public void onCheckedChanged(RadioGroup group,
                                int checkedId) {
                            if (checkedId == R.id.radioRectangular) {
                                setToggleState(false, true);
                            } else if (checkedId == R.id.radioCircular) {
                                setToggleState(true, true);
                            }
                        }
                    });
        }
    }

    private void setupCircularTextViews() {
        ValueTV[CENTER_POSITION] = (TextView) _root
                .findViewById(R.id.dz_crit_meas_text_centerpos_val);
        LabelTV[CENTER_POSITION] = (TextView) _root
                .findViewById(R.id.dz_crit_meas_text_centerpos_label);

        ValueTV[RADIUS_POSITION] = (TextView) _root
                .findViewById(R.id.dz_crit_meas_text_radius_val);
        LabelTV[RADIUS_POSITION] = (TextView) _root
                .findViewById(R.id.dz_crit_meas_text_radius_label);
        UnitsTV[RADIUS_POSITION] = (TextView) _root
                .findViewById(R.id.dz_crit_meas_text_radius_units);
    }

    private void setupRectangularTextViews() {

        // can be null for LZ
        _rectButtons = (LinearLayout) _root.findViewById(R.id.rectButtons);
        _alignBtn = (LCRButton) _root.findViewById(R.id.lcr_selector);
        if (_alignBtn != null) {
            _alignBtn.addOnChangedListener(new LCRButton.OnChangedListener() {
                public void onSideChanged(LCRButton lcrButton,
                        CollectionSide side) {
                    int pos = ATSKConstants.ANCHOR_APPROACH_CENTER;
                    if (side == CollectionSide.LEFT)
                        pos = ATSKConstants.ANCHOR_APPROACH_LEFT;
                    else if (side == CollectionSide.RIGHT)
                        pos = ATSKConstants.ANCHOR_APPROACH_RIGHT;
                    if (surveyData.AnchorPosition != pos) {
                        SurveyPoint app = AZHelper.CalculateAnchorFromAZCenter(
                                surveyData, surveyData.center,
                                surveyData.getApproachAnchor());
                        SurveyPoint dep = AZHelper.CalculateAnchorFromAZCenter(
                                surveyData, surveyData.center,
                                surveyData.getDepartureAnchor());
                        surveyData.AnchorPosition = pos;
                        recalculateWithApproach(surveyData, app);
                        recalculateWithDeparture(surveyData, dep);
                        UpdateDisplayMeasurements(true);
                    }
                }
            });
        }
        _setupBtn = (Button) _root.findViewById(R.id.autoSetup);
        _cancelSetup = (Button) _root.findViewById(R.id.cancelSetup);
        if (_setupBtn != null) {
            _setupBtn.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    beginAutoSetup();
                }
            });
            _cancelSetup.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    endAutoSetup();
                }
            });
        }

        ValueTV[ANCHOR_POSITION] = (TextView) _root
                .findViewById(R.id.dz_crit_meas_text_pos_val);
        LabelTV[ANCHOR_POSITION] = (TextView) _root
                .findViewById(R.id.dz_crit_meas_text_pos_label);

        ValueTV[DEPARTURE_POSITION] = (TextView) _root
                .findViewById(R.id.dz_crit_meas_text_dep_val);
        LabelTV[DEPARTURE_POSITION] = (TextView) _root
                .findViewById(R.id.dz_crit_meas_text_dep_label);

        ValueTV[APP_ELEVATION_POSITION] = (TextView) _root
                .findViewById(R.id.dz_crit_meas_text_app_elev_val);
        LabelTV[APP_ELEVATION_POSITION] = (TextView) _root
                .findViewById(R.id.dz_crit_meas_text_app_elev_label);
        UnitsTV[APP_ELEVATION_POSITION] = (TextView) _root
                .findViewById(R.id.dz_crit_meas_text_app_elev_units);

        ValueTV[DEP_ELEVATION_POSITION] = (TextView) _root
                .findViewById(R.id.dz_crit_meas_text_dep_elev_val);
        LabelTV[DEP_ELEVATION_POSITION] = (TextView) _root
                .findViewById(R.id.dz_crit_meas_text_dep_elev_label);
        UnitsTV[DEP_ELEVATION_POSITION] = (TextView) _root
                .findViewById(R.id.dz_crit_meas_text_dep_elev_units);

        ValueTV[LENGTH_POSITION] = (TextView) _root
                .findViewById(R.id.dz_crit_meas_text_length_val);
        LabelTV[LENGTH_POSITION] = (TextView) _root
                .findViewById(R.id.dz_crit_meas_text_length_label);
        UnitsTV[LENGTH_POSITION] = (TextView) _root
                .findViewById(R.id.dz_crit_meas_text_length_units);

        ValueTV[WIDTH_POSITION] = (TextView) _root
                .findViewById(R.id.dz_crit_meas_text_width_val);
        LabelTV[WIDTH_POSITION] = (TextView) _root
                .findViewById(R.id.dz_crit_meas_text_width_label);
        UnitsTV[WIDTH_POSITION] = (TextView) _root
                .findViewById(R.id.dz_crit_meas_text_width_units);

        ValueTV[DRAG_POSITION] = (TextView) _root
                .findViewById(R.id.dz_crit_meas_text_dragheading_val);
        LabelTV[DRAG_POSITION] = (TextView) _root
                .findViewById(R.id.dz_crit_meas_text_dragheading_label);
        UnitsTV[DRAG_POSITION] = (TextView) _root
                .findViewById(R.id.dz_crit_meas_text_dragheading_units);

        ValueTV[APP_THRESHOLD] = (TextView) _root
                .findViewById(R.id.dz_crit_meas_text_appthres_val);
        LabelTV[APP_THRESHOLD] = (TextView) _root
                .findViewById(R.id.dz_crit_meas_text_appthres_label);
        UnitsTV[APP_THRESHOLD] = (TextView) _root
                .findViewById(R.id.dz_crit_meas_text_appthresh_units);

        ValueTV[DEP_THRESHOLD] = (TextView) _root
                .findViewById(R.id.dz_crit_meas_text_depthres_val);
        LabelTV[DEP_THRESHOLD] = (TextView) _root
                .findViewById(R.id.dz_crit_meas_text_depthres_label);
        UnitsTV[DEP_THRESHOLD] = (TextView) _root
                .findViewById(R.id.dz_crit_meas_text_depthresh_units);
    }

    // Start automatic setup mode
    protected void beginAutoSetup() {
        if (_cancelSetup != null)
            _cancelSetup.setVisibility(View.VISIBLE);
        if (_setupBtn != null)
            _setupBtn.setVisibility(View.GONE);
        _setupStep = 0;
        continueAutoSetup();
    }

    protected void continueAutoSetup() {
        if (_setupStep < 0 || _setupStep > 2) {
            endAutoSetup();
            return;
        }
        int index = -1;
        switch (_setupStep) {
            case 0:
                index = ANCHOR_POSITION;
                break;
            case 1:
                index = DEPARTURE_POSITION;
                break;
            case 2:
                index = WIDTH_POSITION;
                break;
        }
        toggleTV(index);
        if (_setupStep == 2) {
            // Start width measurement at departure point
            SurveyPoint dep = AZHelper.CalculateAnchorFromAZCenter(surveyData,
                    surveyData.center, surveyData.getDepartureAnchor());
            dep.setHAE(surveyData.departureElevation);
            newPosition(dep, false);
        }
        _setupStep++;
        UpdateDisplayMeasurements(false);
    }

    protected void endAutoSetup() {
        if (_setupStep >= 0)
            toggleTV(-1);
        _setupStep = -1;
        if (_cancelSetup != null)
            _cancelSetup.setVisibility(View.GONE);
        if (_setupBtn != null)
            _setupBtn.setVisibility(View.VISIBLE);
    }

    private void setupTVClickListeners() {

        for (TextView unit : UnitsTV) {
            if (unit != null) {
                unit.setClickable(true);
                unit.setLongClickable(true);
                unit.setOnLongClickListener(TVLongClickListener);
                unit.setOnClickListener(TVClickListener);
            }
        }
        for (int i = 0; i < ValueTV.length; i++) {
            if (ValueTV[i] != null) {
                LabelTV[i].setClickable(true);
                ValueTV[i].setClickable(true);
                ValueTV[i].setLongClickable(true);
                LabelTV[i].setLongClickable(true);

                LabelTV[i].setOnLongClickListener(TVLongClickListener);
                ValueTV[i].setOnLongClickListener(TVLongClickListener);
                LabelTV[i].setOnClickListener(TVClickListener);
                ValueTV[i].setOnClickListener(TVClickListener);
            }
        }
    }

    protected boolean StoreMeasurement(double newMeasurement_m) {
        //we need to calculate the anchor based on the OLD measurement - then a new center based on the anchor and the NEW measurement
        SurveyPoint AnchorPoint = AZHelper.CalculateAnchorFromAZCenter(
                surveyData, surveyData.center, surveyData.AnchorPosition);
        //Log.d(TAG, "SurveyData Center HAE: " + surveyData.center.getHAE());
        switch (CurrentlyEditedIndex) {
            case RADIUS_POSITION:
                setRadius(newMeasurement_m);
                return true;

            case LENGTH_POSITION:
                setLength(newMeasurement_m, false);
                surveyData.center = AZHelper.CalculateLZCenter(surveyData,
                        AnchorPoint);
                return true;

            case WIDTH_POSITION:
                return StoreMeasurement(Math.abs(newMeasurement_m),
                        surveyData.angle);
            case ELEVATION_POSITION:
                surveyData.center.setMSL(newMeasurement_m);
                return true;
            case DRAG_POSITION:
                surveyData.angle = newMeasurement_m;
                surveyData.center = AZHelper.CalculateLZCenter(surveyData,
                        AnchorPoint);
                return true;
            case APP_ELEVATION_POSITION:
                surveyData.approachElevation = (float) Conversions
                        .ConvertMSLtoHAE(surveyData.center.lat,
                                surveyData.center.lon, newMeasurement_m);
                return true;
            case DEP_ELEVATION_POSITION:
                surveyData.departureElevation = (float) Conversions
                        .ConvertMSLtoHAE(surveyData.center.lat,
                                surveyData.center.lon, newMeasurement_m);
                return true;
            case DEP_THRESHOLD:
                setOverrunLength(newMeasurement_m, false);
                return true;
            case APP_THRESHOLD:
                setOverrunLength(newMeasurement_m, true);
                return true;

        }
        return false;
    }

    protected void toggleTV(int index) {
        // Clear other text views
        for (int i = 0; i < ValueTV.length; i++) {
            if (i != index)
                ClearTV(i);
        }

        // Reset 2-point entry
        StoredPositionIndex = 0;

        // Invalid index provided
        if (index < 0 || index >= ValueTV.length) {
            CurrentlyEditedIndex = -1;
            setOBState(ATSKIntentConstants.OB_STATE_REQUESTED_HIDDEN);
            return;
        }

        // Select matching text view
        if (CurrentlyEditedIndex == index) {
            CurrentlyEditedIndex = -1;
            ClearTV(index);
            setOBState(ATSKIntentConstants.OB_STATE_REQUESTED_HIDDEN);
        } else {
            CurrentlyEditedIndex = index;
            ValueTV[index].setText(R.string.input_waiting);
            LabelTV[index].setBackgroundResource(
                    R.drawable.background_selected_left);
            ValueTV[index].setBackgroundResource(
                    R.drawable.background_selected_center);

            if (UnitsTV[index] != null)
                UnitsTV[index].setBackgroundResource(
                        R.drawable.background_selected_right);
            else
                ValueTV[index].setBackgroundResource(
                        R.drawable.background_selected_right);
            setOBState(ATSKIntentConstants.OB_STATE_REQUESTED_POINT);
        }
    }

    protected void ClearTV(int i) {
        if (i < 0 || i > ValueTV.length)
            return;
        if (LabelTV[i] != null) {
            LabelTV[i].setBackgroundResource(0);
            LabelTV[i].setPadding(0, 0, 0, 0);
        }
        if (ValueTV[i] != null) {
            ValueTV[i].setBackgroundResource(0);
            ValueTV[i].setPadding(0, 0, 0, 0);
        }
        if (UnitsTV[i] != null) {
            UnitsTV[i].setBackgroundResource(0);
            UnitsTV[i].setPadding(0, 0, 0, 0);
        }
    }

    protected void UpdateDisplayMeasurements(boolean AllowDBUpdate) {
        if (surveyData == null)
            return;
        UpdateUnitsDisplay();

        Unit dispUnit = getDisplayUnit();

        if (ValueTV[RADIUS_POSITION] != null)
            ValueTV[RADIUS_POSITION].setText(Unit.METER.format(
                    surveyData.getRadius(), dispUnit));
        if (ValueTV[LENGTH_POSITION] != null)
            ValueTV[LENGTH_POSITION].setText(Unit.METER.format(
                    surveyData.getLength(false), dispUnit));
        if (ValueTV[WIDTH_POSITION] != null)
            ValueTV[WIDTH_POSITION].setText(Unit.METER.format(
                    surveyData.width, dispUnit));
        if (ValueTV[ELEVATION_POSITION] != null)
            ValueTV[ELEVATION_POSITION].setText(
                    surveyData.center.getMSLAltitude().toString(
                            DisplayUnitsStandard ? Unit.FOOT : Unit.METER));

        if (ValueTV[DRAG_POSITION] != null) {
            if (DisplayAnglesTrue) {
                surveyData.angle = Conversions.deg360(surveyData.angle);
                ValueTV[DRAG_POSITION].setText(String.format("%.1f",
                        surveyData.angle));
            } else {
                double MagAngle = Conversions.GetMagAngle(
                        surveyData.angle, surveyData.center.lat,
                        surveyData.center.lon);
                ValueTV[DRAG_POSITION].setText(String.format("%.1f", MagAngle));
            }
        }

        SurveyPoint approach = AZHelper.CalculateAnchorFromAZCenter(
                surveyData, surveyData.center, surveyData.getApproachAnchor());
        String CenterPositionString = Conversions.getCoordinateString(
                surveyData.center.lat, surveyData.center.lon,
                DisplayCoordinateFormat);
        String AnchorPositionString = Conversions
                .getCoordinateString(approach.lat, approach.lon,
                        DisplayCoordinateFormat);

        SurveyPoint departure = AZHelper.CalculateAnchorFromAZCenter(
                surveyData, surveyData.center, surveyData.getDepartureAnchor());
        String DeparturePointString = Conversions.getCoordinateString(
                departure.lat, departure.lon,
                DisplayCoordinateFormat);

        //update the spinner
        if (ValueTV[CENTER_POSITION] != null)
            ValueTV[CENTER_POSITION].setText(CenterPositionString);
        if (ValueTV[ANCHOR_POSITION] != null)
            ValueTV[ANCHOR_POSITION].setText(AnchorPositionString);
        if (ValueTV[DEPARTURE_POSITION] != null)
            ValueTV[DEPARTURE_POSITION].setText(DeparturePointString);

        if (ValueTV[SLOPE_L_POSITION] != null) {
            ValueTV[SLOPE_L_POSITION].setText(String.format("%.1f%%",
                    surveyData.slopeL));
        }
        if (ValueTV[SLOPE_W_POSITION] != null) {
            ValueTV[SLOPE_W_POSITION].setText(String.format("%.1f%%",
                    surveyData.slopeW));
        }

        if (ValueTV[APP_THRESHOLD] != null) {
            ValueTV[APP_THRESHOLD].setText(Unit.METER.format(
                    surveyData.edges.ApproachOverrunLength_m, dispUnit));
        }
        if (ValueTV[DEP_THRESHOLD] != null) {
            ValueTV[DEP_THRESHOLD].setText(Unit.METER.format(
                    surveyData.edges.DepartureOverrunLength_m, dispUnit));
        }

        // Elevation is never in yards
        if (dispUnit == Unit.YARD)
            dispUnit = Unit.FOOT;
        if (ValueTV[APP_ELEVATION_POSITION] != null) {

            if (SurveyPoint.Altitude.isValid(surveyData.approachElevation))
                ValueTV[APP_ELEVATION_POSITION].setText(Unit.METER.format(
                        Conversions.ConvertHAEtoMSL(
                                surveyData.center.lat, surveyData.center.lon,
                                surveyData.approachElevation), dispUnit));
            else
                ValueTV[APP_ELEVATION_POSITION].setText(R.string.unknown);

        }
        if (ValueTV[DEP_ELEVATION_POSITION] != null) {
            if (SurveyPoint.Altitude.isValid(surveyData.departureElevation))
                ValueTV[DEP_ELEVATION_POSITION].setText(Unit.METER.format(
                        Conversions.ConvertHAEtoMSL(
                                surveyData.center.lat, surveyData.center.lon,
                                surveyData.departureElevation), dispUnit));
            else
                ValueTV[DEP_ELEVATION_POSITION].setText(R.string.unknown);
        }

        UpdateSurvey(AllowDBUpdate);
    }

    private void UpdateUnitsDisplay() {
        for (int i = 0; i < UnitsTV.length; i++) {
            if (UnitsTV[i] != null) {
                if (i == DRAG_POSITION) {
                    if (DisplayAnglesTrue) {
                        UnitsTV[i].setText(String.format("%cT",
                                ATSKConstants.DEGREE_SYMBOL));
                    } else {
                        UnitsTV[i].setText(String.format("%cM",
                                ATSKConstants.DEGREE_SYMBOL));
                    }
                } else if (DisplayUnitsStandard) {
                    if (StandardUnitsFeet || i == ELEVATION_POSITION) {
                        UnitsTV[i].setText(" ft");
                    } else {
                        UnitsTV[i].setText(" yds");

                    }
                } else
                    UnitsTV[i].setText(" m");
            }
        }
    }

    void ShowCoordinateHandJamDialog(double currentLat, double currentLon,
            double elevation) {
        CoordinateHandJamDialog chjd = new CoordinateHandJamDialog();
        chjd.Initialize(currentLat, currentLon,
                DisplayCoordinateFormat, elevation, this);
    }

    protected void setToggleState(boolean circular, boolean AllowUpdateDB) {
        if (surveyData == null || _surveyNeedsUpdate
                || surveyData.getType() == AZ_TYPE.LZ
                || surveyData.getType() == AZ_TYPE.FARP)
            return;
        endAutoSetup();
        showCircularTextViews(circular);
        setOBState(ATSKIntentConstants.OB_STATE_REQUESTED_HIDDEN);
        circular = surveyData.circularAZ;
        surveyData.circularAZ = typeToggle != null
                && ((RadioButton) _root.findViewById(R.id.radioCircular))
                        .isChecked();
        if (circular != surveyData.circularAZ)
            UpdateDisplayMeasurements(AllowUpdateDB);
    }

    protected void showCircularTextViews(boolean visible) {

        int circularVisibility = View.GONE;
        int rectVisibility = View.VISIBLE;

        if (visible) {
            circularVisibility = View.VISIBLE;
            rectVisibility = View.GONE;
        }

        if (_rectButtons != null)
            _rectButtons.setVisibility(rectVisibility);

        if (ValueTV[CENTER_POSITION] != null) {
            ValueTV[CENTER_POSITION].setVisibility(circularVisibility);
            LabelTV[CENTER_POSITION].setVisibility(circularVisibility);
        }
        if (ValueTV[RADIUS_POSITION] != null) {
            ValueTV[RADIUS_POSITION].setVisibility(circularVisibility);
            LabelTV[RADIUS_POSITION].setVisibility(circularVisibility);
            //UnitsTV[RADIUS_POSITION].setVisibility(circularVisibility);
        }

        if (ValueTV[ANCHOR_POSITION] != null) {
            ValueTV[ANCHOR_POSITION].setVisibility(rectVisibility);
            LabelTV[ANCHOR_POSITION].setVisibility(rectVisibility);
        }
        if (ValueTV[DEPARTURE_POSITION] != null) {
            ValueTV[DEPARTURE_POSITION].setVisibility(rectVisibility);
            LabelTV[DEPARTURE_POSITION].setVisibility(rectVisibility);
        }
        if (ValueTV[APP_ELEVATION_POSITION] != null) {
            ValueTV[APP_ELEVATION_POSITION].setVisibility(rectVisibility);
            LabelTV[APP_ELEVATION_POSITION].setVisibility(rectVisibility);
            //UnitsTV[APP_ELEVATION_POSITION].setVisibility(rectVisibility);
        }
        if (ValueTV[DEP_ELEVATION_POSITION] != null) {
            ValueTV[DEP_ELEVATION_POSITION].setVisibility(rectVisibility);
            LabelTV[DEP_ELEVATION_POSITION].setVisibility(rectVisibility);
            //UnitsTV[DEP_ELEVATION_POSITION].setVisibility(rectVisibility);
        }

        if (ValueTV[LENGTH_POSITION] != null) {
            ValueTV[LENGTH_POSITION].setVisibility(rectVisibility);
            LabelTV[LENGTH_POSITION].setVisibility(rectVisibility);
            //UnitsTV[LENGTH_POSITION].setVisibility(rectVisibility);
        }
        if (ValueTV[WIDTH_POSITION] != null) {
            ValueTV[WIDTH_POSITION].setVisibility(rectVisibility);
            LabelTV[WIDTH_POSITION].setVisibility(rectVisibility);
            //UnitsTV[WIDTH_POSITION].setVisibility(rectVisibility);
        }
        if (ValueTV[ELEVATION_POSITION] != null) {
            ValueTV[ELEVATION_POSITION].setVisibility(View.VISIBLE);
            LabelTV[ELEVATION_POSITION].setVisibility(View.VISIBLE);
            //UnitsTV[ELEVATION_POSITION].setVisibility(View.VISIBLE);
        }
        if (ValueTV[DRAG_POSITION] != null) {
            ValueTV[DRAG_POSITION].setVisibility(rectVisibility);
            LabelTV[DRAG_POSITION].setVisibility(rectVisibility);
            UnitsTV[DRAG_POSITION].setVisibility(rectVisibility);
        }

    }

    @Override
    public void shotApproved(SurveyPoint sp, double range_m, double az_deg,
            double el_deg, boolean CollectingTop) {

        if (DisplayAnglesTrue)
            az_deg = Conversions.GetTrueAngle(az_deg, sp.lat, sp.lon);
        SurveyPoint AnchorPoint = AZHelper.CalculateAnchorFromAZCenter(
                surveyData, surveyData.center, surveyData.AnchorPosition);
        //shot has been approved, here is the information. 
        switch (CurrentlyEditedIndex) {
            case CENTER_POSITION: {
                newPosition(sp, CollectingTop);
                break;
            }
            case RADIUS_POSITION: {
                setRadius(range_m);
                break;
            }
            case ANCHOR_POSITION: {
                //set anchor lat/lon
                newPosition(sp, CollectingTop);
                break;
            }
            case DEPARTURE_POSITION: {
                //set anchor lat/lon
                newPosition(sp, CollectingTop);
                break;
            }
            case LENGTH_POSITION: {
                setLength(range_m, true);
                surveyData.center = AZHelper.CalculateLZCenter(surveyData,
                        AnchorPoint);
                break;
            }
            case WIDTH_POSITION: {
                StoreMeasurement(range_m, surveyData.angle);
                break;
            }
            case ELEVATION_POSITION: {
                surveyData.center.setSurveyPoint(sp);
                break;
            }
            case DEP_ELEVATION_POSITION: {
                surveyData.departureElevation = sp.getHAE();
                surveyData.center = AZHelper.CalculateLZCenter(surveyData,
                        AnchorPoint);
                break;
            }
            case APP_ELEVATION_POSITION: {
                surveyData.approachElevation = sp.getHAE();
                surveyData.center = AZHelper.CalculateLZCenter(surveyData,
                        AnchorPoint);
                break;
            }
            case DEP_THRESHOLD: {
                setOverrunLength(range_m, false);
                break;
            }
            case APP_THRESHOLD: {
                setOverrunLength(range_m, true);
                break;
            }
            case DRAG_POSITION: {
                //set the angle
                surveyData.angle = az_deg;
                surveyData.center = AZHelper.CalculateLZCenter(surveyData,
                        AnchorPoint);
                break;
            }
        }

        //Log.d(TAG, "ShotApproved End: Center HAE: " + surveyData.center.getHAE());
        UpdateDisplayMeasurements(true);

    }

    private String GetSelectedType() {
        if (CurrentlyEditedIndex == RADIUS_POSITION) {
            return "RADIUS";
        } else if (CurrentlyEditedIndex == ANCHOR_POSITION) {
            return "ANCHOR POINT";
        } else if (CurrentlyEditedIndex == DEPARTURE_POSITION) {
            return "ANCHOR POINT";//SMET - this might need to be more specific.   
        } else if (CurrentlyEditedIndex == LENGTH_POSITION) {
            return "LENGTH";
        } else if (CurrentlyEditedIndex == WIDTH_POSITION) {
            return "WIDTH";
        } else if (CurrentlyEditedIndex == ELEVATION_POSITION) {
            return "ELEVATION";
        } else if (CurrentlyEditedIndex == DEP_THRESHOLD) {
            return "THRESHOLD";
        } else if (CurrentlyEditedIndex == APP_THRESHOLD) {
            return "THRESHOLD";
        } else if (CurrentlyEditedIndex == DRAG_POSITION) {
            return "ANGLE";
        }
        return "";
    }

    protected boolean StoreMeasurement(double range, double angle) {

        SurveyPoint AnchorPoint = AZHelper.CalculateAnchorFromAZCenter(
                surveyData, surveyData.center, surveyData.AnchorPosition);
        switch (CurrentlyEditedIndex) {
            case RADIUS_POSITION: {
                setRadius(range);
                break;
            }
            case LENGTH_POSITION: {
                setLength(range, false);
                break;
            }
            case WIDTH_POSITION: {
                if (_setupStep == 3 && surveyData.getApproachAnchor()
                            == ATSKConstants.ANCHOR_APPROACH_CENTER)
                    range *= 2;
                setWidth(range);
                continueAutoSetup();
                break;
            }
            case DRAG_POSITION: {
                surveyData.angle = angle;
                break;
            }
            case DEP_THRESHOLD: {
                setOverrunLength(range, false);
                break;
            }
            case APP_THRESHOLD: {
                setOverrunLength(range, true);
                break;
            }
        }
        surveyData.center = AZHelper.CalculateLZCenter(surveyData, AnchorPoint);
        //drawSurvey now that all measuremnets are happy
        return false;
    }

    protected void setRadius(double radius_m) {
        AZController.setRadius(surveyData, radius_m);
    }

    protected void setLength(double length_m, boolean usable) {
        AZController.setLength(surveyData, length_m, usable);
    }

    protected void setWidth(double width_m) {
        AZController.setWidth(surveyData, width_m);
    }

    protected void setOverrunLength(double length_m, boolean approach) {
        AZController.setOverrunLength(surveyData, length_m, approach);
    }

    void ShowMeasurementHandJamDialog(double meters, String Name) {
        ShowMeasurementHandJamDialog(meters, Name, false);
    }

    void ShowMeasurementHandJamDialog(double meters, String Name,
            boolean IsHeight) {
        ATSKDialogManager adm = new ATSKDialogManager(getActivity(),
                this, StandardUnitsFeet || IsHeight);
        adm.ShowMeasurementHandJamDialog(meters, Name, CurrentlyEditedIndex);
    }

    void ShowAngleHandJamDialog(double currentAngle_True) {
        ATSKDialogManager adm = new ATSKDialogManager(getActivity(), this,
                false);
        SurveyPoint AnchorPoint = AZHelper.CalculateAnchorFromAZCenter(
                surveyData, surveyData.center, surveyData.AnchorPosition);
        adm.ShowAngleHandJamDialog(currentAngle_True, AnchorPoint,
                CurrentlyEditedIndex);
    }

    //Hand Jam Updates
    @Override
    public void UpdateMeasurement(int index, double measurement) {
        //call our update
        CurrentlyEditedIndex = index;
        StoreMeasurement(measurement);
        UpdateDisplayMeasurements(true);
    }

    @Override
    public void UpdateStringValue(int index, String value) {
        //getting back slopes here??
        Float slope;
        try {
            slope = Float.parseFloat(value);
        } catch (NumberFormatException e) {
            return;
        }

        switch (CurrentlyEditedIndex) {
            case SLOPE_L_POSITION:
                surveyData.slopeL = slope;
                break;
            case SLOPE_W_POSITION:
                surveyData.slopeW = slope;
                break;
        }
        UpdateDisplayMeasurements(false);

    }

    @Override
    public void UpdateAngleUnits(boolean usingTrue) {
        DisplayAnglesTrue = usingTrue;
        user_settings.edit().putString(
                ATSKConstants.UNITS_ANGLE,
                DisplayAnglesTrue ? ATSKConstants.UNITS_ANGLE_TRUE
                        : ATSKConstants.UNITS_ANGLE_MAG).apply();
    }

    @Override
    public void UpdateDimensionUnits(boolean usingStandard) {
        DisplayUnitsStandard = usingStandard;
        user_settings.edit().putString(
                ATSKConstants.UNITS_DISPLAY,
                DisplayUnitsStandard ? ATSKConstants.UNITS_FEET
                        : ATSKConstants.UNITS_METERS).apply();
    }

    /**
     * HandJam coordinates.
     */
    @Override
    public void UpdateCoordinate(double Lat, double Lon, double elevation) {
        Log.d(TAG, "hand jam update coordinate: lat=" + Lat + " lon=" + Lon
                + " elevation=" + elevation);
        switch (CurrentlyEditedIndex) {
            case CENTER_POSITION:
                surveyData.center.lat = Lat;
                surveyData.center.lon = Lon;
                surveyData.center.setHAE(elevation);
                break;
            case ANCHOR_POSITION:
            case DEPARTURE_POSITION:
                //SMET - might need to check 1pt vs 2pt modes here. 
                SurveyPoint sp = new SurveyPoint(Lat, Lon);
                sp.circularError = 99;
                sp.linearError = 99;
                sp.setHAE(elevation);
                if (CurrentlyEditedIndex == ANCHOR_POSITION)
                    recalculateWithApproach(surveyData, sp);
                else
                    recalculateWithDeparture(surveyData, sp);
                StoredPositionIndex = 0;
                continueAutoSetup();
                break;
        }
        UpdateDisplayMeasurements(true);
    }

    @Override
    public void UpdateCoordinateFormat(String DisplayFormat) {
        DisplayCoordinateFormat = DisplayFormat;
        UpdateDisplayMeasurements(false);
    }

    /**
     * GPS coordinates.
     */
    @Override
    public void newPosition(SurveyPoint sp, boolean TopCollected) {

        /*Log.d(TAG, "gps or map update coordinate: lat=" + lat + " lon=" + lon
                + " elevation=" + elevation_m + " ce=" + ce);*/

        switch (CurrentlyEditedIndex) {
            case ELEVATION_POSITION:
            case CENTER_POSITION: {
                surveyData.center.setSurveyPoint(sp);
                // Re-query approach and departure elevation
                SurveyPoint app = AZHelper.CalculateAnchorFromAZCenter(
                        surveyData, surveyData.center,
                        surveyData.getApproachAnchor());
                surveyData.approachElevation = ATSKApplication
                        .getAltitudeHAE(app);
                SurveyPoint dep = AZHelper.CalculateAnchorFromAZCenter(
                        surveyData, surveyData.center,
                        surveyData.getDepartureAnchor());
                surveyData.departureElevation = ATSKApplication
                        .getAltitudeHAE(dep);
                StoredPositionIndex = 0;
                break;
            }
            case APP_ELEVATION_POSITION:
            case ANCHOR_POSITION: {
                //SMET - might need to check 1pt vs 2pt modes here.
                surveyData.approachElevation = sp.getHAE();
                recalculateWithApproach(surveyData, sp);
                StoredPositionIndex = 0;
                continueAutoSetup();
                break;
            }
            case DEP_ELEVATION_POSITION:
            case DEPARTURE_POSITION: {
                surveyData.departureElevation = sp.getHAE();
                recalculateWithDeparture(surveyData, sp);
                StoredPositionIndex = 0;
                continueAutoSetup();
                break;
            }
            case RADIUS_POSITION: {
                StoredPositionIndex = 0;
                double[] RangeAngle = Conversions.calculateRangeAngle(
                        surveyData.center, sp);
                if (RangeAngle[0] < 1500000)
                    StoreMeasurement((float) RangeAngle[0],
                            (float) RangeAngle[1]);
                break;
            }
            default: {
                if (CurrentlyEditedIndex >= 0) {
                    //these are 2 point kinda values
                    if (StoredPositionIndex == 0) {
                        StoredPositionIndex++;
                        StoredPosition.setSurveyPoint(sp);

                        UpdateNotification(getActivity(), "1 of 2 COLLECTED",
                                Conversions.getCoordinateString(sp,
                                        DisplayCoordinateFormat),
                                GetSelectedType(), "");

                        ValueTV[CurrentlyEditedIndex].setText("1 of 2 POINTS");
                        return;
                    } else {
                        StoredPositionIndex = 0;
                        double[] ra = Conversions.calculateRangeAngle(
                                StoredPosition, sp);

                        if (_setupStep == 3
                                && CurrentlyEditedIndex == WIDTH_POSITION) {
                            // Use perpendicular distance -  Assumes StoredPosition
                            // is still the departure point we automatically set earlier
                            ra[1] = Conversions
                                    .deg360(ra[1] - surveyData.angle);
                            ra[0] = Math.abs(ra[0]
                                    * Math.sin(Math.toRadians(ra[1])));
                        }

                        StoreMeasurement((float) ra[0], (float) ra[1]);

                        UpdateNotification(getActivity(), "2 of 2 COLLECTED",
                                Conversions.GetLatLonDM(sp.lat, sp.lon),
                                Conversions.GetMGRS(sp),
                                String.format(LocaleUtil.getCurrent(),
                                        "%.0fm@%.1fdegT", ra[0], ra[1]));
                    }
                } else {
                    setOBState(ATSKIntentConstants.OB_STATE_REQUESTED_HIDDEN);
                }
            }
        }
        UpdateDisplayMeasurements(true);
    }

    public static void recalculateWithApproach(
            SurveyData survey, SurveyPoint newApproach) {

        survey.approachElevation = newApproach.getHAE();

        SurveyPoint depPoint = AZHelper.CalculateAnchorFromAZCenter(
                survey, survey.center,
                survey.getDepartureAnchor());

        double newRAB[] = Conversions.CalculateRangeAngle(newApproach.lat,
                newApproach.lon, depPoint.lat, depPoint.lon);

        if (newRAB[0] < SurveyData.MAX_LENGTH)
            survey.setLength((float) newRAB[0], true);

        survey.angle = (float) newRAB[1];
        survey.center = AZHelper.CalculateLZCenter(
                survey.getApproachAnchor(), newApproach,
                survey.getLength(true), survey.width,
                survey.angle);
        survey.center.setHAE(ATSKApplication
                .getAltitudeHAE(survey.center));
    }

    public static void recalculateWithDeparture(
            SurveyData survey, SurveyPoint newDeparture) {

        survey.departureElevation = newDeparture.getHAE();

        SurveyPoint appPoint = AZHelper.CalculateAnchorFromAZCenter(
                survey, survey.center,
                survey.getApproachAnchor());

        double newRAB[] = Conversions.CalculateRangeAngle(appPoint.lat,
                appPoint.lon, newDeparture.lat, newDeparture.lon);

        if (newRAB[0] < SurveyData.MAX_LENGTH) {
            survey.setLength((float) newRAB[0], true);
        } else {
            double[] clamped = Conversions
                    .AROffset(newDeparture.lat,
                            newDeparture.lon, survey.angle + 180,
                            survey.getLength(true));
            appPoint.lat = clamped[0];
            appPoint.lon = clamped[1];
        }

        survey.angle = (float) newRAB[1];

        appPoint.setHAE(survey.approachElevation);
        survey.center = AZHelper.CalculateLZCenter(
                survey.getApproachAnchor(), appPoint,
                survey.getLength(true), survey.width,
                survey.angle);
        survey.center.setHAE(ATSKApplication
                .getAltitudeHAE(survey.center));
    }

    public void stopCollection() {
        UpdateDisplayMeasurements(false);
        toggleTV(-1);
    }

    @Override
    protected void UpdateScreen() {
    }

    @Override
    public void UpdateGSRAngleUnits(boolean GSR) {

    }

    @Override
    public void updateSurvey(String surveyUID) {
        super.updateSurvey(surveyUID);
        UpdateDisplayMeasurements(false);
    }
}
