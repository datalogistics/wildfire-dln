
package com.gmeci.atsk.gradient.surfacedistress;

import android.content.Context;
import android.util.AttributeSet;

import com.atakmap.android.gui.PluginSpinner;
import com.gmeci.constants.Constants;

import java.util.ArrayList;
import java.util.Arrays;

public class SurfaceDistressTypeSpinner extends PluginSpinner {

    SurfaceDistressSpinnerAdapter typeAdapter;

    final Context context;
    ArrayList<String> distressTypeStringArray;

    public SurfaceDistressTypeSpinner(Context context, AttributeSet attrs,
            int defStyle, int mode) {
        super(context, attrs, defStyle, mode);

        SetupSpinner();
        this.context = context;
    }

    public SurfaceDistressTypeSpinner(Context context, AttributeSet attrs,
            int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;

        SetupSpinner();
    }

    public SurfaceDistressTypeSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        SetupSpinner();

    }

    public SurfaceDistressTypeSpinner(Context context) {
        super(context);
        this.context = context;
        SetupSpinner();
    }

    private boolean SetupSpinner() {

        SetSpinnerAdapter();
        return true;
    }

    public int GetPosition(String type) {
        for (int i = 0; distressTypeStringArray != null
                && i < distressTypeStringArray.size(); i++) {
            if (distressTypeStringArray.get(i).equalsIgnoreCase(type))
                return i;
        }
        return 0;
    }

    public boolean SetSpinnerAdapter() {
        distressTypeStringArray = new ArrayList<String>(
                Arrays.asList(Constants.SURFACE_DISTRESSES));

        typeAdapter = new SurfaceDistressSpinnerAdapter(context,
                android.R.layout.simple_spinner_item, distressTypeStringArray);

        typeAdapter
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        setAdapter(typeAdapter);
        return true;
    }

}
