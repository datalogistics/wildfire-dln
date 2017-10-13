
package com.gmeci.atsk.obstructions;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.AttributeSet;

import com.atakmap.android.gui.PluginSpinner;
import com.atakmap.android.maps.MapView;
import com.gmeci.atsk.resources.ATSKApplication;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class FilteredObstructionTypeSpinner extends PluginSpinner {

    final static public String TAG = "FilteredObstructionTypeSpinner";

    protected ObstructionSpinnerAdapter _adapter;
    protected SharedPreferences _prefs;
    protected List<String> _types;
    protected Context _context;

    public FilteredObstructionTypeSpinner(Context context, AttributeSet attrs,
            int defStyle, int mode) {
        super(context, attrs, defStyle, mode);
        setup(context);
    }

    public FilteredObstructionTypeSpinner(Context context, AttributeSet attrs,
            int defStyle) {
        super(context, attrs, defStyle);
        setup(context);
    }

    public FilteredObstructionTypeSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup(context);
    }

    public FilteredObstructionTypeSpinner(Context context) {
        super(context);
        setup(context);
    }

    private boolean setup(Context ctx) {
        _context = ctx;
        _prefs = PreferenceManager
                .getDefaultSharedPreferences(MapView
                        .getMapView().getContext());
        setup(ObstructionType.POINTS);
        return true;
    }

    public int GetPosition(String type) {
        int pos = 0;
        for (int i = 0; _types != null
                && i < _types.size(); i++) {
            if (_types.get(i).equalsIgnoreCase(type))
                return i;
        }
        //add this type to the list???
        if (_types != null) {
            pos = _types.size();
            _types.add(type);
        }
        _adapter = new ObstructionSpinnerAdapter(_context,
                android.R.layout.simple_spinner_item, _types);

        _adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        setAdapter(_adapter);
        return pos;
    }

    public boolean setup(ObstructionType type, List<String> excluded) {
        Context pluginContext =
                ATSKApplication.getInstance().getPluginContext();

        _types = new ArrayList<String>();
        Set<String> prefSet = type.getPref() != null ?
                _prefs.getStringSet(type.getPref(), null) : null;
        _types.addAll(prefSet != null ? prefSet :
                Arrays.asList(pluginContext.getResources()
                        .getStringArray(type.getArrayId())));

        if (excluded != null)
            _types.removeAll(excluded);

        _adapter = new ObstructionSpinnerAdapter(_context,
                android.R.layout.simple_spinner_item, _types);
        _adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        setAdapter(_adapter);
        return true;
    }

    public boolean setup(ObstructionType type) {
        return setup(type, null);
    }
}
