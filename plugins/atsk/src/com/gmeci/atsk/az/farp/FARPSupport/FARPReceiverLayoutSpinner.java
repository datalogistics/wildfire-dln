
package com.gmeci.atsk.az.farp.FARPSupport;

import android.content.Context;
import android.util.AttributeSet;

import com.atakmap.android.gui.PluginSpinner;
import com.gmeci.atsk.resources.ATSKApplication;
import com.gmeci.atskservice.farp.FARPTankerItem;
import com.gmeci.core.ATSKConstants;

import java.util.ArrayList;
import java.util.List;

public class FARPReceiverLayoutSpinner extends PluginSpinner {

    FARPReceiverLayoutSpinnerAdapter typeAdapter;

    final Context context;
    final Context pluginContext;

    public FARPReceiverLayoutSpinner(Context context, AttributeSet attrs,
            int defStyle, int mode) {
        super(context, attrs, defStyle, mode);
        this.context = context;
        this.pluginContext = ATSKApplication
                .getInstance().getPluginContext();

        SetupSpinner();

    }

    public FARPReceiverLayoutSpinner(Context context, AttributeSet attrs,
            int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;
        this.pluginContext = ATSKApplication
                .getInstance().getPluginContext();

        SetupSpinner();
    }

    public FARPReceiverLayoutSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        this.pluginContext = ATSKApplication
                .getInstance().getPluginContext();
        SetupSpinner();

    }

    public FARPReceiverLayoutSpinner(Context context) {
        super(context);
        this.context = context;
        this.pluginContext = ATSKApplication
                .getInstance().getPluginContext();
        SetupSpinner();
    }

    private boolean SetupSpinner() {
        SetSpinnerAdapter(new FARPTankerItem(ATSKConstants.AC_C17));
        return true;
    }

    public boolean SetupSpinner(FARPTankerItem tanker) {
        SetSpinnerAdapter(tanker);
        return true;
    }

    public String GetSetting(int pos) {
        if (typeAdapter != null)
            return typeAdapter.getItem(pos);
        return ATSKConstants.FARP_RX_LAYOUT_SINGLE;
    }

    public int GetPosition(String type) {
        if (type == null || typeAdapter == null)
            return 0;
        for (int i = 0; i < typeAdapter.getCount(); i++) {
            if (type.equals(typeAdapter.getItem(i)))
                return i;
        }
        return 0;
    }

    public boolean SetSpinnerAdapter(FARPTankerItem tanker) {
        List<String> types = new ArrayList<String>();
        if (tanker != null)
            types = tanker.getReceivers();
        typeAdapter = new FARPReceiverLayoutSpinnerAdapter(pluginContext,
                android.R.layout.simple_spinner_item, types);

        typeAdapter
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        setAdapter(typeAdapter);
        return true;
    }

}
