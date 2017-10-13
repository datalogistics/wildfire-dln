
package com.gmeci.atsk.gradient.surfacedistress;

import android.content.Context;
import android.util.AttributeSet;

import com.atakmap.android.gui.PluginSpinner;
import com.gmeci.constants.Constants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class SurfaceDistressSeveritySpinner extends PluginSpinner {

    SurfaceDistressSeveritySpinnerAdapter typeAdapter;

    HashMap<String, SeverityDescription> _severity;
    final Context context;
    ArrayList<String> distressSeverityStringArray;

    public SurfaceDistressSeveritySpinner(Context context, AttributeSet attrs,
            int defStyle, int mode) {
        super(context, attrs, defStyle, mode);

        SetupSpinner(Constants.SURFACE_DISTRESSES[0]);
        this.context = context;
    }

    public SurfaceDistressSeveritySpinner(Context context, AttributeSet attrs,
            int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;

        SetupSpinner(Constants.SURFACE_DISTRESSES[0]);
    }

    public SurfaceDistressSeveritySpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        SetupSpinner(Constants.SURFACE_DISTRESSES[0]);

    }

    public SurfaceDistressSeveritySpinner(Context context) {
        super(context);
        this.context = context;
        SetupSpinner(Constants.SURFACE_DISTRESSES[0]);
    }

    // Set severity level values to be displayed in spinner
    public boolean SetupSpinner(String Type) {
        fillMap();
        String[] OptionsArray = _severity
                .get(Constants.DISTRESS_POTHOLE).values;
        if (_severity.containsKey(Type))
            OptionsArray = _severity.get(Type).values;
        SetSpinnerAdapter(OptionsArray);
        return true;
    }

    public int GetPosition(String type) {
        for (int i = 0; distressSeverityStringArray != null
                && i < distressSeverityStringArray.size(); i++) {
            if (distressSeverityStringArray.get(i).equalsIgnoreCase(type))
                return i;
        }
        return 0;
    }

    public boolean SetSpinnerAdapter(String[] Options) {

        distressSeverityStringArray = new ArrayList<String>(
                Arrays.asList(Options));

        typeAdapter = new SurfaceDistressSeveritySpinnerAdapter(context,
                android.R.layout.simple_spinner_item,
                distressSeverityStringArray);
        typeAdapter
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        setAdapter(typeAdapter);
        return true;
    }

    static class SeverityDescription {
        final String[] values;

        public SeverityDescription(String low, String med, String high) {
            values = new String[] {
                    low, med, high
            };
        }
    }

    private void fillMap() {
        if (_severity != null)
            return;
        _severity = new HashMap<String, SeverityDescription>();
        // Pothole
        _severity.put(Constants.DISTRESS_POTHOLE,
                new SeverityDescription(
                        "<4\" Deep or <15\" Diameter",
                        "<4\" Deep and >15\" Diameter",
                        ">9\" Deep and >15\" Diameter"));

        // Ruts
        _severity.put(Constants.DISTRESS_RUTS,
                new SeverityDescription(
                        "<4\" Deep", "4\"-9\" Deep", ">9\" Deep"));

        // Loose Aggregate
        _severity.put(Constants.DISTRESS_LOOSE_AGG,
                new SeverityDescription(
                        "COVERS <1/10", "COVERS 1/10 - 1/2", "COVERS >1/2"));

        // Dust
        _severity.put(Constants.DISTRESS_DUST,
                new SeverityDescription(
                        "NO OBSTRUCTION", "PARTIAL", "THICK"));

        // Rolling Resistant
        _severity.put(Constants.DISTRESS_ROLLING_RESISTANT,
                new SeverityDescription(
                        "<3.5\" DEEP", "3.5\"- 7.75\" DEEP", ">7.75\" DEEP"));

        // Jet Blast Erosion
        _severity.put(Constants.DISTRESS_JET_EROSION,
                new SeverityDescription(
                        "<1\" DEEP", "1\"- 3\" DEEP", ">3\" DEEP"));

        // Stabilized Layer Failure
        _severity.put(Constants.DISTRESS_STABLE_FAILURE,
                new SeverityDescription(
                        "<1\" DEEP", "1\"- 2\" DEEP", ">2\" DEEP"));
    }
}
