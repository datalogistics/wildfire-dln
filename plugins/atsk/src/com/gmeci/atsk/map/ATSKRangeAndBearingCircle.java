
package com.gmeci.atsk.map;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.PointMapItem;
import com.atakmap.coremap.conversions.Span;
import com.atakmap.coremap.conversions.SpanUtilities;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.maps.assets.Icon;
import com.gmeci.atskservice.resolvers.ObstructionProviderClient;
import com.gmeci.core.ATSKConstants;
import com.gmeci.core.PointObstruction;

public class ATSKRangeAndBearingCircle extends ATSKMarker implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "ATSKRangeAndBearingCircle";

    private SharedPreferences _prefs;
    private ATSKCircle _circle;

    public ATSKRangeAndBearingCircle(PointObstruction po) {
        super("", po);
    }

    @Override
    protected void init() {
        super.init();
        setMetaDouble("minRenderScale", Double.MAX_VALUE);
        setMetaString("obsType", "R&B");
        _prefs = PreferenceManager.getDefaultSharedPreferences(
                MapView.getMapView().getContext());
        _prefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void setObstruction(PointObstruction obs) {
        super.setObstruction(obs);
        if (_circle != null) {
            _circle.setRadius(_obs.width / 2);
            _circle.setStrokeColor(_obs.flags);
            updateLabel();
        }
    }

    @Override
    public void setIcon(Icon ico) {
        if (ico != null && _obs != null && _obs.flags
                    != ico.getColor(Icon.STATE_DEFAULT)) {
            Icon.Builder builder = ico.buildUpon();
            builder.setColor(Icon.STATE_DEFAULT, _obs.flags);
            super.setIcon(builder.build());
            return;
        }
        super.setIcon(ico);
    }

    @Override
    public void onPointChanged(final PointMapItem item) {
        super.onPointChanged(item);
        if (_circle != null)
            _circle.setCenterPoint(getPoint());
    }

    // TODO: Database overhaul
    // 'flags' can be used for color or label visibility, not both
    @Override
    public void setLabelVisible(boolean visible) {
    }

    @Override
    public boolean getLabelVisible() {
        return true;
    }

    @Override
    public void onItemAdded(MapItem item, MapGroup group) {
        if (_circle == null && _obs != null) {
            _circle = new ATSKCircle(_obs.remark, getUID() + "_circle",
                    group, getPoint(), _obs.width / 2, _obs.flags, false);
            _circle.setMetaString("menu", getMetaString("menu", ""));
            _circle.setMetaString("obsUID", getUID());
            updateLabel();
            setMetaString("shapeUID", _circle.getUID());
            group.addItem(_circle);
        }
    }

    @Override
    public void onItemRemoved(MapItem item, MapGroup group) {
        super.onItemRemoved(item, group);
        _prefs.unregisterOnSharedPreferenceChangeListener(this);
        if (_circle != null) {
            if (_circle.getGroup() != null)
                _circle.getGroup().removeItem(_circle);
        }
        _circle = null;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (key.equals("rab_rng_units_pref"))
            updateLabel();
    }

    @Override
    public void save() {
        if (_obs == null)
            return;
        Context ctx = MapView.getMapView().getContext();
        ObstructionProviderClient opc =
                new ObstructionProviderClient(ctx);
        if (opc.Start()) {
            PointObstruction po = opc.GetPointObstruction(
                    ATSKConstants.DEFAULT_GROUP, _obs.uid);
            if (_circle != null)
                _obs.flags = _circle.getStrokeColor() | 0xFF000000;
            if (po != null)
                opc.EditPoint(_obs);
            else
                _obs = null;
            opc.Stop();
        }
    }

    private void updateLabel() {
        if (_circle == null)
            return;
        String label;
        if (_obs != null && !FileSystemUtils.isEmpty(_obs.remark))
            label = _obs.remark;
        else {
            int type = Span.METRIC;
            try {
                type = Integer.parseInt(_prefs.getString("rab_rng_units_pref",
                        String.valueOf(Span.METRIC)));
            } catch (Exception e) {
            }
            label = SpanUtilities.formatType(type, _circle.getRadius(),
                    Span.METER);
        }
        _circle.setLabel(label);
        _circle.setMetaString("callsign", getTitle());
        _circle.setMetaString("title", getTitle());
    }
}
