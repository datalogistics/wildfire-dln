
package com.gmeci.atsk.az.spinners;

import android.content.Context;
import android.util.AttributeSet;

import com.atakmap.android.atsk.plugin.R;
import com.atakmap.coremap.log.Log;
import com.gmeci.atskservice.dz.DZRequirementsParser;
import com.gmeci.core.ATSKConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * Spinner with DZ criteria
 */
public class DZSpinner extends ATSKSpinner {

    private static final String TAG = "DZSpinner";

    public DZSpinner(Context context, AttributeSet attrs, int defStyle, int mode) {
        super(context, attrs, defStyle, mode);
    }

    public DZSpinner(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public DZSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DZSpinner(Context context) {
        super(context);
    }

    @Override
    public boolean setup() {
        List<String> acNames = new ArrayList<String>();
        DZRequirementsParser parser = new DZRequirementsParser();
        try {
            parser.parseRequirementsFile();
            acNames.addAll(parser.getAircraft());
        } catch (Exception e) {
            // Fallback to known defaults
            acNames.clear();
            acNames.add(ATSKConstants.AC_C130);
            acNames.add(ATSKConstants.AC_C17);
            Log.e(TAG, "Failed to parse DZ requirements", e);
        }

        setOnTouchListener(com.gmeci.atsk.resources.ATSKApplication
                .getInstance());
        setBackgroundResource(R.drawable.bordered_spinner_selector);

        ATSKSpinnerAdapter adapter = new ATSKSpinnerAdapter(context,
                android.R.layout.simple_spinner_item, acNames);
        adapter.setDropDownViewResource(android.R.layout
                .simple_spinner_dropdown_item);
        setAdapter(adapter);
        setSelection(0);
        return true;
    }
}
