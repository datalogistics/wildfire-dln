
package com.gmeci.atsk.az;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import com.atakmap.android.atsk.plugin.R;
import com.gmeci.atsk.resources.ATSKApplication;
import com.gmeci.atskservice.resolvers.AZProviderClient;
import com.gmeci.conversions.Conversions;
import com.gmeci.core.ATSKConstants;
import com.gmeci.core.SurveyData;
import com.gmeci.core.SurveyData.AZ_TYPE;
import com.gmeci.core.SurveyPoint;
import com.gmeci.helpers.AZHelper;

import java.text.DecimalFormat;

/**
 * Dialog that displays the survey's main criteria fragment
 */
public class AZDetailsDialog extends DialogFragment {

    public static final String TAG = "AZDetailsDialog";

    private static final int NAME = 0;
    private static final int TYPE = 1;
    private static final int APPROACH = 2;
    private static final int DEPARTURE = 3;
    private static final int ELEVATION = 4;
    private static final int LENGTH = 5;
    private static final int WIDTH = 6;
    private static final int HEADING = 7;
    private static final int SLOPE_L = 8;
    private static final int SLOPE_W = 9;
    private static final int VALUE_COUNT = 10;

    private SharedPreferences _prefs;
    private AZProviderClient _azpc;
    private SurveyData _survey;
    private View _root;
    private boolean _created = false;

    private final TextView[] _values = new TextView[VALUE_COUNT];
    private ImageView _icon;

    public void init(AZProviderClient azpc, String surveyUID) {
        _azpc = azpc;
        if (!azpc.isStarted())
            azpc.Start();
        setSurvey(surveyUID);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        Context pluginContext = ATSKApplication.getInstance()
                .getPluginContext();

        _root = LayoutInflater.from(pluginContext).inflate(
                R.layout.survey_detail_dialog, container);
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        return _root;
    }

    @Override
    public void onViewCreated(View view, Bundle saved) {
        super.onViewCreated(view, saved);

        _values[NAME] = (TextView) _root.findViewById(R.id.name);
        _values[TYPE] = (TextView) _root.findViewById(R.id.type);
        _values[APPROACH] = (TextView) _root.findViewById(R.id.approach);
        _values[DEPARTURE] = (TextView) _root.findViewById(R.id.departure);
        _values[ELEVATION] = (TextView) _root.findViewById(R.id.alt);
        _values[LENGTH] = (TextView) _root.findViewById(R.id.length);
        _values[WIDTH] = (TextView) _root.findViewById(R.id.width);
        _values[HEADING] = (TextView) _root.findViewById(R.id.rot);
        _values[SLOPE_L] = (TextView) _root.findViewById(R.id.slope_l);
        _values[SLOPE_W] = (TextView) _root.findViewById(R.id.slope_w);

        _icon = (ImageView) _root.findViewById(R.id.type_image);

        _prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        _created = true;

        if (_survey != null)
            setSurvey(_survey.uid);
    }

    @Override
    public void onDestroy() {
        _created = false;
        super.onDestroy();
    }

    public void setSurvey(String surveyUID) {
        if (_azpc != null)
            _survey = _azpc.getAZ(surveyUID, false);
        if (_survey != null && _created) {
            // Top section
            AZ_TYPE type = _survey.getType();
            setValue(NAME, _survey.getSurveyName());
            setValue(TYPE, getAZType(type));
            _icon.setImageResource(getAZIcon(type));

            // Details
            DecimalFormat dec = new DecimalFormat("#.#");
            String coordFormat = _prefs.getString(
                    ATSKConstants.COORD_FORMAT, Conversions.COORD_FORMAT_MGRS);

            SurveyPoint anchor = AZHelper.CalculateAnchorFromAZCenter(
                    _survey, _survey.center, _survey.getApproachAnchor());
            setValue(APPROACH, Conversions.getCoordinateString(
                    anchor.lat, anchor.lon, coordFormat));

            anchor = AZHelper.CalculateAnchorFromAZCenter(
                    _survey, _survey.center, _survey.getDepartureAnchor());
            setValue(DEPARTURE, Conversions.getCoordinateString(
                    anchor.lat, anchor.lon, coordFormat));

            setValue(LENGTH, dec.format(_survey.getLength()));
            setValue(WIDTH, dec.format(_survey.width));
            setValue(HEADING, dec.format(_survey.angle));
            setValue(ELEVATION, dec.format(_survey.center.getMSL()));
            setValue(SLOPE_L, String.format("%.1f%%", _survey.slopeL));
            setValue(SLOPE_W, String.format("%.1f%%", _survey.slopeW));
        }
    }

    public void setValue(int index, Object value) {
        if (index >= 0 && index < VALUE_COUNT && _values[index] != null)
            _values[index].setText(String.valueOf(value));
    }

    private String getAZType(AZ_TYPE type) {
        if (type == AZ_TYPE.LZ)
            return "Landing Zone (LZ)";
        else if (type == AZ_TYPE.HLZ)
            return "Helicopter Landing Zone (HLZ)";
        else if (type == AZ_TYPE.DZ)
            return "Drop Zone (DZ)";
        else if (type == AZ_TYPE.FARP)
            return "Forward Arming and Refueling Point (FARP)";
        return "Unknown";
    }

    private int getAZIcon(AZ_TYPE type) {
        if (type == AZ_TYPE.LZ)
            return R.drawable.navigation_lz;
        else if (type == AZ_TYPE.HLZ)
            return R.drawable.navigation_hlz;
        else if (type == AZ_TYPE.DZ)
            return R.drawable.navigation_dz;
        else if (type == AZ_TYPE.FARP)
            return R.drawable.navigation_farp;
        return R.drawable.atsk_delete;
    }
}
