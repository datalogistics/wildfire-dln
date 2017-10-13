
package com.gmeci.atsk.visibility;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;

import com.atakmap.android.atsk.plugin.R;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.tools.ActionBarView;
import com.gmeci.atsk.resources.ATSKApplication;
import com.gmeci.atsk.toolbar.ATSKBaseToolbar;

/**
 * Toolbar options for box select
 */
public class MapSelectToolbar implements ATSKBaseToolbar,
        View.OnClickListener {

    private static final String TAG = "MapSelectToolbar";

    private final MapView _mapView;
    private final MapSelectTool _tool;
    private ActionBarView _root;
    private ImageButton _boxSelect, _deleteBtn;

    public MapSelectToolbar(MapView mapView, MapSelectTool tool) {
        _mapView = mapView;
        _tool = tool;
    }

    @Override
    public synchronized void setupView() {
        if (_root == null) {
            Context plugin = ATSKApplication.getInstance().getPluginContext();
            _root = (ActionBarView) LayoutInflater.from(plugin).inflate(
                    R.layout.map_select_toolbar, _mapView,
                    false);
            _root.setEmbedded(false);
            _root.setClosable(false);

            // Main buttons
            _boxSelect = (ImageButton) _root.findViewById(R.id.selection_box);
            _boxSelect.setOnClickListener(this);
            _deleteBtn = (ImageButton) _root
                    .findViewById(R.id.selection_delete);
            _deleteBtn.setOnClickListener(this);
        }
    }

    @Override
    public synchronized ActionBarView getView() {
        return _root;
    }

    @Override
    public synchronized int[] getBounds() {
        int bounds[] = new int[4];
        if (_root != null) {
            bounds[0] = _root.getTop();
            bounds[1] = _root.getLeft();
            bounds[2] = _root.getBottom();
            bounds[3] = _root.getRight();
        }
        return bounds;
    }

    @Override
    public void onVisible(boolean v) {
        if (_tool != null) {
            if (v)
                _tool.requestBeginTool();
            else
                _tool.requestEndTool();
        }
    }

    @Override
    public boolean onBackButtonPressed() {
        return false;
    }

    @Override
    public void dispose() {
    }

    @Override
    public synchronized void onClick(View v) {
        if (v == _boxSelect) {
            _tool.boxSelect();
        } else if (v == _deleteBtn) {
            _tool.deleteSelection();
        }
    }
}
