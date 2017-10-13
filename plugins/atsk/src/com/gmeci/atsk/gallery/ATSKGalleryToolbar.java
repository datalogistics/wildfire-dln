
package com.gmeci.atsk.gallery;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import com.atakmap.android.atsk.plugin.R;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.tools.ActionBarView;
import com.gmeci.atsk.resources.ATSKApplication;
import com.gmeci.atsk.gallery.ATSKGalleryFragment.GalleryAction;
import com.gmeci.atsk.toolbar.ATSKBaseToolbar;

/**
 * Toolbar options (capture + delete)
 */
public class ATSKGalleryToolbar implements ATSKBaseToolbar,
        View.OnClickListener {

    private static final String TAG = "ATSKGalleryToolbar";

    private ActionBarView _root;
    private ATSKGalleryFragment _gallery;
    private ImageButton _capture, _import, _export, _delete;
    private Button _cancel, _action;
    private final MapView _mapView;

    public ATSKGalleryToolbar(MapView mapView) {
        _mapView = mapView;
    }

    @Override
    public synchronized void setupView() {
        if (_root == null) {
            Context plugin = ATSKApplication.getInstance().getPluginContext();
            _root = (ActionBarView) LayoutInflater.from(plugin).inflate(
                    R.layout.image_gallery_toolbar, _mapView,
                    false);
            _root.setEmbedded(false);
            _root.setClosable(false);

            // Main buttons
            _capture = (ImageButton) _root.findViewById(R.id.gallery_capture);
            _capture.setOnClickListener(this);
            _import = (ImageButton) _root.findViewById(R.id.gallery_import);
            _import.setOnClickListener(this);
            _export = (ImageButton) _root.findViewById(R.id.gallery_export);
            _export.setOnClickListener(this);
            _delete = (ImageButton) _root.findViewById(R.id.gallery_delete);
            _delete.setOnClickListener(this);

            // Cancel/confirm buttons
            _action = (Button) _root.findViewById(R.id.gallery_action);
            _action.setOnClickListener(this);
            _cancel = (Button) _root.findViewById(R.id.gallery_cancel);
            _cancel.setOnClickListener(this);
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
    }

    @Override
    public boolean onBackButtonPressed() {
        if (_cancel != null && _cancel.getVisibility() == View.VISIBLE) {
            _cancel.performClick();
            return true;
        }
        return false;
    }

    @Override
    public void dispose() {
    }

    public void setGallery(ATSKGalleryFragment gallery) {
        _gallery = gallery;
    }

    @Override
    public void onClick(View v) {
        if (_gallery != null) {
            if (v == _capture)
                _gallery.performAction(GalleryAction.CAPTURE);
            else if (v == _import)
                _gallery.performAction(GalleryAction.IMPORT);
            else if (v == _export) {
                if (_gallery.performAction(GalleryAction.EXPORT)) {
                    _action.setText("Export");
                    hidePrimaryActions(true);
                }
            } else if (v == _delete) {
                if (_gallery.performAction(GalleryAction.DELETE)) {
                    _action.setText("Delete");
                    hidePrimaryActions(true);
                }
            } else if (v == _action) {
                if (_gallery.performAction(GalleryAction.DONE))
                    hidePrimaryActions(false);
            } else if (v == _cancel) {
                _gallery.performAction(GalleryAction.CANCEL);
                hidePrimaryActions(false);
            }
        }
    }

    private void hidePrimaryActions(boolean hide) {
        _capture.setVisibility(hide ? View.GONE : View.VISIBLE);
        _import.setVisibility(hide ? View.GONE : View.VISIBLE);
        _export.setVisibility(hide ? View.GONE : View.VISIBLE);
        _delete.setVisibility(hide ? View.GONE : View.VISIBLE);
        _action.setVisibility(hide ? View.VISIBLE : View.GONE);
        _cancel.setVisibility(hide ? View.VISIBLE : View.GONE);
        ((Activity) _mapView.getContext()).invalidateOptionsMenu();
    }
}
