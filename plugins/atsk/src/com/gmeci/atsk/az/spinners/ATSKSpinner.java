
package com.gmeci.atsk.az.spinners;

import android.content.Context;
import android.util.AttributeSet;

import com.atakmap.android.atsk.plugin.R;
import com.atakmap.android.gui.PluginSpinner;

import java.util.List;

public class ATSKSpinner extends PluginSpinner {

    public Context context;
    ATSKSpinnerAdapter typeAdapter;
    List<String> spinnerContentStringArray;

    public ATSKSpinner(Context context, AttributeSet attrs, int defStyle,
            int mode) {
        super(context, attrs, defStyle, mode);
        this.context = context;
        setup();
    }

    public ATSKSpinner(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;
        setup();
    }

    public ATSKSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        setup();
    }

    public ATSKSpinner(Context context) {
        super(context);
        this.context = context;
        setup();
    }

    public boolean setup() {
        setBackgroundResource(R.drawable.bordered_spinner_selector);
        if (typeAdapter != null)
            typeAdapter.clear();
        typeAdapter = new ATSKSpinnerAdapter(context,
                android.R.layout.simple_spinner_item);

        typeAdapter
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        setAdapter(typeAdapter);
        return true;
    }

    public String getItem(int position) {
        if (typeAdapter != null && typeAdapter.selections != null) {
            return typeAdapter.selections.get(position);
        }
        if (spinnerContentStringArray == null)
            return "";
        return spinnerContentStringArray.get(position);
    }

    public int GetPosition(String type) {
        if (type == null)
            return 0;

        if (typeAdapter != null && typeAdapter.selections != null) {
            for (int i = 0; i < typeAdapter.selections.size(); i++) {
                if (typeAdapter.selections.get(i).equalsIgnoreCase(type))
                    return i;
            }
            return 0;
        }

        for (int i = 0; spinnerContentStringArray != null
                && i < spinnerContentStringArray.size(); i++) {
            if (spinnerContentStringArray.get(i).equalsIgnoreCase(type))
                return i;
        }
        return 0;
    }

    public boolean SetupSpinner(List<String> spinnerContent) {
        spinnerContentStringArray = spinnerContent;
        typeAdapter = new ATSKSpinnerAdapter(context,
                android.R.layout.simple_spinner_item, spinnerContent);
        typeAdapter
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        setAdapter(typeAdapter);
        return true;
    }

}
