
package com.gmeci.atsk.az.farp.FARPSupport;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnLongClickListener;

import com.atakmap.android.atsk.plugin.R;
import com.gmeci.atsk.az.spinners.ATSKSpinner;
import com.gmeci.atsk.az.spinners.ATSKSpinnerAdapter;

import java.util.ArrayList;

public class FARPACSpinner extends ATSKSpinner implements OnLongClickListener {

    static String TAG = "ACSpinner";

    ATSKSpinnerAdapter typeAdapter;
    ArrayList<String> aclist;

    public FARPACSpinner(Context context, AttributeSet attrs, int defStyle,
            int mode) {
        super(context, attrs, defStyle, mode);
        this.context = context;
    }

    public FARPACSpinner(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;
    }

    public FARPACSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    public FARPACSpinner(Context context) {
        super(context);
        this.context = context;
    }

    public void SetupACSpinner() {

        setOnLongClickListener(this);
        setBackgroundResource(R.drawable.bordered_spinner_selector);

        aclist = new ArrayList<String>();
        aclist.add("C-17");
        aclist.add("C-130");
        aclist.add("KC-135");

        typeAdapter = new ATSKSpinnerAdapter(context,
                android.R.layout.simple_spinner_item, aclist);
        typeAdapter
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        setAdapter(typeAdapter);
        setSelection(0);
    }

    public int GetPosition(String type) {
        for (int i = 0; aclist != null && i < aclist.size(); i++) {
            if (aclist.get(i).equalsIgnoreCase(type))
                return i;
        }
        return 0;
    }

    @Override
    public boolean onLongClick(View v) {
        //show the dialog for editing the minimums.
        //showCriteriaEditDialog();
        return true;
    }

}
