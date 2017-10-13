
package com.gmeci.atsk.az.spinners;

import android.content.Context;
import android.util.AttributeSet;

import com.atakmap.android.atsk.plugin.R;
import com.atakmap.coremap.log.Log;
import com.gmeci.atsk.az.hlz.HLZRequirementsParser;
import com.gmeci.atsk.az.hlz.HelicopterRequirements;

import java.util.ArrayList;
import java.util.List;

/**
 * Spinner with DZ criteria
 */
public class HLZSpinner extends ATSKSpinner {

    private static final String TAG = "HLZSpinner";
    public static final String HLZ_DATA_CSV = "hlz_data.csv";

    public HLZSpinner(Context context, AttributeSet attrs, int defStyle,
            int mode) {
        super(context, attrs, defStyle, mode);
    }

    public HLZSpinner(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public HLZSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public HLZSpinner(Context context) {
        super(context);
    }

    @Override
    public boolean setup() {
        List<String> acNames = new ArrayList<String>();
        HLZRequirementsParser parser = new HLZRequirementsParser();
        try {
            List<HelicopterRequirements> reqs = parser.parseFile(HLZ_DATA_CSV);
            for (HelicopterRequirements req : reqs)
                acNames.add(req.HeliName);
        } catch (Exception e) {
            // Fallback to known defaults
            acNames.clear();
            acNames.add("MH-53");
            acNames.add("UH-1");
            acNames.add("HH-60");
            acNames.add("CV-22");
            acNames.add("MH-47E");
            acNames.add("MH-6/AH-6");
            Log.e(TAG, "Failed to load HLZ requirements.", e);
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
