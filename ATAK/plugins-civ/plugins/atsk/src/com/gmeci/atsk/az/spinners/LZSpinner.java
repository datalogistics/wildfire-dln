
package com.gmeci.atsk.az.spinners;

import android.content.Context;
import android.util.AttributeSet;

import com.atakmap.android.atsk.plugin.R;
import com.gmeci.atsk.az.lz.LZParser;
import com.gmeci.core.Criteria;

import java.util.List;

public class LZSpinner extends ATSKSpinner {

    private static final String TAG = "LZSpinner";

    private ATSKSpinnerAdapter _adapter;
    private LZParser _lzParser;
    private List<String> _acList;

    public LZSpinner(Context context, AttributeSet attrs, int defStyle, int mode) {
        super(context, attrs, defStyle, mode);
    }

    public LZSpinner(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public LZSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LZSpinner(Context context) {
        super(context);
    }

    @Override
    public boolean setup() {
        setOnTouchListener(com.gmeci.atsk.resources.ATSKApplication
                .getInstance());
        setBackgroundResource(R.drawable.bordered_spinner_selector);

        _lzParser = LZParser.getInstance();
        _acList = _lzParser.GetAircraftNames();

        _adapter = new ATSKSpinnerAdapter(context,
                android.R.layout.simple_spinner_item, _acList);
        _adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        setAdapter(_adapter);
        setSelection(0);
        return true;
    }

    @Override
    public int GetPosition(String type) {
        if (_acList == null || type == null)
            return 0;
        for (int i = 0; _acList != null && i < _acList.size(); i++) {
            if (_acList.get(i).equalsIgnoreCase(type))
                return i;
        }
        return 0;
    }

    public Criteria getACByIndex(int position) {
        if (_lzParser != null)
            return _lzParser.GetAircraftByName(_adapter.getItem(position));
        return null;
    }
}
