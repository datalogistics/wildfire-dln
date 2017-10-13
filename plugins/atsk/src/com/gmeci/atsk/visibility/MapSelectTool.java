
package com.gmeci.atsk.visibility;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import com.atakmap.android.drawing.mapItems.DrawingRectangle;
import com.atakmap.android.editableShapes.Rectangle;
import com.atakmap.android.hierarchy.filters.FOVFilter;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.DefaultMapGroup;
import com.atakmap.android.maps.MapEvent;
import com.atakmap.android.maps.MapEventDispatcher.MapEventDispatchListener;
import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.PointMapItem;
import com.atakmap.android.menu.MapMenuReceiver;
import com.atakmap.android.toolbar.Tool;
import com.atakmap.android.toolbar.ToolManagerBroadcastReceiver;
import com.atakmap.android.toolbar.widgets.TextContainer;
import com.atakmap.coremap.maps.coords.GeoBounds;
import com.gmeci.atsk.map.ATSKMarker;
import com.gmeci.atsk.map.ATSKShape;
import com.gmeci.atskservice.resolvers.ObstructionProviderClient;
import com.gmeci.core.LineObstruction;
import com.gmeci.core.PointObstruction;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Box selection tool for obstructions
 */
public class MapSelectTool extends Tool implements MapEventDispatchListener {

    private static final String TAG = "MapSelectTool";
    private static final String MAIN_PROMPT = "Press and drag the corners of the green box to select items. Press the trash icon in the toolbar to remove all items within the box.";
    public static final String TOOL_IDENTIFIER = "com.gmeci.atsk.visibility.MAP_SELECT";

    private MapGroup _group, _rectGroup;
    private final TextContainer _container;
    private Rectangle _selectBox;
    private ObstructionProviderClient _opc;
    private final Button _toolButton;

    public MapSelectTool(MapView mapView, Button button, MapGroup obsGroup) {
        super(mapView, TOOL_IDENTIFIER);
        _group = obsGroup;
        _toolButton = button;
        _container = TextContainer.getInstance();
        ToolManagerBroadcastReceiver.getInstance()
                .registerTool(TOOL_IDENTIFIER, this);
        _opc = new ObstructionProviderClient(_mapView.getContext());
    }

    @Override
    public boolean onToolBegin(Bundle extras) {
        super.onToolBegin(extras);

        // Display help text
        _container.displayPrompt(MAIN_PROMPT);
        _toolButton.setSelected(true);

        // Map event listeners
        _mapView.getMapEventDispatcher().pushListeners();
        _mapView.getMapEventDispatcher().addMapEventListener(
                MapEvent.ITEM_DRAG_STARTED, this);
        _mapView.getMapEventDispatcher().addMapEventListener(
                MapEvent.ITEM_DRAG_CONTINUED, this);
        _mapView.getMapEventDispatcher().addMapEventListener(
                MapEvent.ITEM_DRAG_DROPPED, this);
        _mapView.getMapTouchController().setToolActive(true);

        // Create selection box
        boxSelect();
        return true;
    }

    @Override
    public void onToolEnd() {
        // Delete selection box
        if (_selectBox != null)
            boxSelect();

        // Close help text
        _container.closePrompt();
        _toolButton.setSelected(false);

        // Clear listeners
        _mapView.getMapEventDispatcher().clearListeners();
        _mapView.getMapEventDispatcher().popListeners();
        _mapView.getMapTouchController().setToolActive(false);

        // Close radial in case it's opened so listeners aren't screwed up
        AtakBroadcast.getInstance().sendBroadcast(
                new Intent(MapMenuReceiver.HIDE_MENU));
    }

    @Override
    public void dispose() {
    }

    @Override
    public void onMapEvent(MapEvent event) {
        if (_selectBox == null || _selectBox.getGroup() == null)
            return;
        MapItem item = event.getItem();
        if (!partOfSelect(item))
            return;
        String type = event.getType();
        PointMapItem pmi = (PointMapItem) item;
        if (_selectBox.getAssociationMarkerIndex(pmi) != -1)
            return;
        if (type.equals(MapEvent.ITEM_DRAG_CONTINUED)
                || type.equals(MapEvent.ITEM_DRAG_DROPPED)) {
            Point p = event.getPoint();
            if (p != null)
                pmi.setPoint(_mapView.inverse(p.x, p.y));
        }
    }

    /**
     * Toggle box selection
     */
    void boxSelect() {
        if (_selectBox == null) {
            if (_rectGroup == null) {
                _rectGroup = new DefaultMapGroup("ATSK-SelectBox");
                _rectGroup.setMetaBoolean("addToObjList", false);
                _group.addGroup(_rectGroup);
            }
            int width = _mapView.getWidth(), w4 = width / 4;
            int height = _mapView.getHeight(), h4 = height / 4;
            _selectBox = new DrawingRectangle(_rectGroup,
                    _mapView.inverse(width - w4, height - h4),
                    _mapView.inverse(width - w4, h4),
                    _mapView.inverse(w4, h4),
                    _mapView.inverse(w4, height - h4),
                    UUID.randomUUID().toString());
            _selectBox.setStrokeColor(Color.GREEN);
            _selectBox.setFillColor(0);
            _selectBox.setEditable(true);
            _selectBox.setMetaBoolean("nevercot", true);
            _selectBox.removeMetaData("menu");
            _selectBox.setMetaBoolean("ignoreMenu", true);
            _selectBox.setMetaBoolean("ignoreFocus", true);
            _group.addItem(_selectBox);
            _selectBox.getAnchorItem().setVisible(false);
            for (MapItem mp : _rectGroup.getItems()) {
                if (mp instanceof PointMapItem && _selectBox
                        .getAssociationMarkerIndex((PointMapItem) mp) != -1)
                    mp.setVisible(false);
            }
        } else {
            if (_selectBox.getGroup() != null)
                _selectBox.getGroup().removeItem(_selectBox);
            if (_rectGroup.getParentGroup() != null)
                _rectGroup.getParentGroup().removeGroup(_rectGroup);
            _selectBox = null;
            _rectGroup = null;
        }
    }

    /**
     * Delete any items within selection
     */
    void deleteSelection() {
        final List<MapItem> selected = getSelected();
        if (selected.isEmpty()) {
            showToast("No items selected.");
            return;
        }

        AlertDialog.Builder adb = new AlertDialog.Builder(_mapView.getContext());
        adb.setTitle("Confirm Delete?");
        adb.setMessage("Are you sure you want to permanently remove "
                + selected.size() + " items?");
        adb.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (!_opc.Start()) {
                    showToast("Failed to remove items.");
                    return;
                }
                for (MapItem mp : selected) {
                    if (validObs(mp)) {
                        if (mp instanceof ATSKMarker) {
                            PointObstruction po = ((ATSKMarker) mp)
                                    .getObstruction();
                            _opc.DeletePoint(po.group, po.uid, true);
                        } else if (mp instanceof ATSKShape) {
                            LineObstruction lo = ((ATSKShape) mp)
                                    .getObstruction();
                            _opc.DeleteLine(lo.group, lo.uid, true);
                        }
                    }
                }
                showToast("Successfully removed " + selected.size() + " items.");
                _opc.Stop();
                dialog.dismiss();
            }
        });
        adb.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        adb.show();
    }

    private List<MapItem> getSelected() {
        List<MapItem> selected = new ArrayList<MapItem>();
        if (_selectBox != null && _selectBox.getGroup() != null) {
            GeoBounds bounds = _selectBox.getBounds(null);
            FOVFilter filter = new FOVFilter(new FOVFilter.MapState(_mapView,
                    bounds));
            for (MapItem mp : _group.getItems()) {
                if (validObs(mp) && filter.accept(mp))
                    selected.add(mp);
            }
        }
        return selected;
    }

    private void showToast(String str) {
        Toast.makeText(_mapView.getContext(), str, Toast.LENGTH_LONG).show();
    }

    private boolean validObs(MapItem mp) {
        return !partOfSelect(mp)
                && mp.getVisible()
                && (mp instanceof ATSKMarker
                        && ((ATSKMarker) mp).getObstruction() != null
                        || mp instanceof ATSKShape
                        && ((ATSKShape) mp).getObstruction() != null);
    }

    private boolean partOfSelect(MapItem item) {
        if (item == null)
            return false;
        if (_selectBox != null && _rectGroup != null)
            return item == _selectBox || item.getGroup() == _rectGroup;
        return false;
    }
}
