
package com.gmeci.atsk.az.spinners;

import android.content.Context;
import android.util.AttributeSet;

import com.atakmap.android.atsk.plugin.R;
import com.atakmap.coremap.log.Log;
import com.gmeci.atsk.az.AZController;
import com.gmeci.atskservice.farp.FARPTankerItem;
import com.gmeci.core.ATSKConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * Spinner with FARP tankers
 */
public class FARPSpinner extends ATSKSpinner {

    private static final String TAG = "FARPSpinner";

    public FARPSpinner(Context context, AttributeSet attrs, int defStyle,
            int mode) {
        super(context, attrs, defStyle, mode);
    }

    public FARPSpinner(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public FARPSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FARPSpinner(Context context) {
        super(context);
    }

    @Override
    public boolean setup() {
        List<String> acNames = new ArrayList<String>();
        try {
            for (FARPTankerItem fti : AZController.getInstance()
                    .getTankers().values())
                acNames.add(fti.Name);
        } catch (Exception e) {
            // Fallback to known defaults
            acNames.clear();
            acNames.add(ATSKConstants.AC_C17);
            acNames.add(ATSKConstants.AC_C130);
            Log.e(TAG, "Failed to load FARP tankers.", e);
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
