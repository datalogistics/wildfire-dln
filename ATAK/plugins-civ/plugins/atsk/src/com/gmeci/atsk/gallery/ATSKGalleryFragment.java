
package com.gmeci.atsk.gallery;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import com.atakmap.android.ipc.AtakBroadcast.DocumentedIntentFilter;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.atakmap.android.atsk.plugin.R;
import com.atakmap.android.gui.ImportFileBrowserDialog;
import com.atakmap.android.image.ImageActivity;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.tools.menu.ActionBroadcastData;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;
import com.gmeci.atsk.ATSKFragment;
import com.gmeci.atsk.ATSKFragmentManager;
import com.gmeci.atsk.az.MapAZController;
import com.gmeci.atsk.az.currentsurvey.CurrentSurveyFragment;
import com.gmeci.atsk.resources.ATSKApplication;
import com.gmeci.atsk.resources.ATSKBaseFragment;
import com.gmeci.atsk.resources.ThumbView;
import com.gmeci.atsk.toolbar.ATSKBaseToolbar;
import com.gmeci.atsk.toolbar.ATSKToolbar;
import com.gmeci.atsk.toolbar.ATSKToolbarComponent;
import com.gmeci.core.ATSKConstants;
import com.gmeci.core.SurveyPoint;

import java.io.File;
import java.util.List;

/**
 * Gallery of images based on survey
 */
public class ATSKGalleryFragment extends ATSKBaseFragment
        implements ATSKToolbar.OnToolbarVisibleListener {

    private static final String TAG = "ATSKGalleryFragment";
    private static final ActionBroadcastData _captureIntent =
            new ActionBroadcastData(ATSKGalleryUtils.IMG_CAPTURE, null);

    enum GalleryAction {
        CAPTURE, IMPORT, EXPORT, DELETE, DONE, CANCEL
    }

    private enum SelectMode {
        NONE, EXPORT, DELETE
    }

    private MapView _mapView;
    private ATSKFragmentManager _manager;
    private String _surveyUID;
    private Resources _res;
    private View _root;
    private GridView _grid;
    private TextView _title;
    private CheckBox _markupCB;
    private ATSKGalleryToolbar _toolbar;
    private ATSKGalleryAdapter _adapter;
    private final ATSKGalleryReceiver _galleryReceiver = new ATSKGalleryReceiver();
    private boolean _created = false;
    private SelectMode _selectMode = SelectMode.NONE;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        _created = false;
        Context pluginContext = ATSKApplication.getInstance()
                .getPluginContext();
        _root = LayoutInflater.from(pluginContext).inflate(
                R.layout.image_gallery, container,
                false);

        // Setup image toolbar
        _toolbar = new ATSKGalleryToolbar(_mapView);
        _toolbar.setGallery(this);

        if (_manager != null && _manager.isCurrentFragment(
                ATSKFragment.IMG))
            ATSKToolbarComponent.getToolbar().setToolbar(_toolbar);

        // Image import
        DocumentedIntentFilter importFilter = new DocumentedIntentFilter();
        importFilter.addAction(ATSKGalleryUtils.ACTIVITY_FINISHED);
        AtakBroadcast.getInstance().registerReceiver(_galleryReceiver,
                importFilter);

        return _root;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        _markupCB = (CheckBox) _root.findViewById(R.id.apply_markup);
        _markupCB.setVisibility(View.GONE);
        _grid = (GridView) _root.findViewById(R.id.gallery_grid);
        _title = (TextView) _root.findViewById(R.id.gallery_title);
        _created = true;

        // Remove temporary gallery markers
        List<MapItem> galleryMarkers = _mapView.getRootGroup()
                .deepFindItems("tempGalleryMarker", "true");
        for (MapItem mi : galleryMarkers)
            mi.getGroup().removeItem(mi);

        _res = ATSKApplication.getInstance()
                .getPluginContext().getResources();
        _adapter = new ATSKGalleryAdapter(_mapView, getActivity()
                .getSupportFragmentManager());
        _grid.setAdapter(_adapter);
    }

    // Set survey fragment used to initialize view
    public void setSurvey(CurrentSurveyFragment survey) {
        String surveyUID = survey != null ? survey.getCurrentSurveyUID() : null;
        if (_surveyUID == null && surveyUID == null)
            return;
        if (_surveyUID == null || !_surveyUID.equals(surveyUID)) {
            _surveyUID = surveyUID;
            refresh();
        }
    }

    // Set map view used to initialize image capture return listener
    public void setMapView(MapView mapView) {
        if (_mapView != mapView) {
            // Image capture receiver
            if (_mapView != null)
                AtakBroadcast.getInstance()
                        .unregisterReceiver(_galleryReceiver);
            _mapView = mapView;
            DocumentedIntentFilter galleryFilter = new DocumentedIntentFilter();
            galleryFilter.addAction(ATSKGalleryUtils.IMG_CAPTURE);
            AtakBroadcast.getInstance().registerReceiver(_galleryReceiver,
                    galleryFilter);
            _galleryReceiver.setGallery(this);
        }
    }

    // Set fragment manager used to close view
    public void setFragmentManager(ATSKFragmentManager manager) {
        _manager = manager;
    }

    // Update image gallery
    public void refresh() {
        if (_created && _surveyUID != null && _adapter != null) {
            // Refresh file adapter
            _adapter.setSurveyUID(_surveyUID);
            _adapter.refresh();

            // Update title
            _title.setText(String.format(_res.getString(
                    R.string.atsk_gallery_title), _adapter.getCount()));

            // Update map items
            MapAZController azmc = new MapAZController(_mapView);
            azmc.drawGalleryIcons(_surveyUID);
        }
    }

    // Only close fragment when toolbar matches what we're using
    // and the current fragment is still set to image gallery
    @Override
    public void onToolbarVisible(ATSKBaseToolbar tb, boolean v) {
        if (!v && tb == _toolbar && _manager != null
                && _manager.isCurrentFragment(ATSKFragment.IMG))
            _manager.setHomeFragment();
    }

    // Action received from toolbar
    public boolean performAction(GalleryAction action) {
        boolean success = true;
        if (action == GalleryAction.CAPTURE) {
            // Request image capture
            File imageFile = ATSKGalleryUtils.newImage(_surveyUID);
            ImageActivity ia = new ImageActivity(_mapView.getContext(),
                    _surveyUID,
                    _captureIntent, imageFile.getAbsolutePath(), _mapView,
                    false);
            ia.start();
        } else if (action == GalleryAction.EXPORT) {
            if (_adapter.getCount() == 0) {
                toast("There are no images to export.");
                success = false;
            } else {
                // Start multi-select export
                _selectMode = SelectMode.EXPORT;
                enableMultiSelect(true);
            }
        } else if (action == GalleryAction.DELETE) {
            if (_adapter.getCount() == 0) {
                toast("There are no images to delete.");
                success = false;
            } else {
                // Start multi-select removal
                _selectMode = SelectMode.DELETE;
                enableMultiSelect(true);
            }
        } else if (action == GalleryAction.IMPORT) {
            showImportDialog();
        } else if (action == GalleryAction.CANCEL) {
            // Cancel multi-select
            _selectMode = SelectMode.NONE;
            enableMultiSelect(false);
        } else if (action == GalleryAction.DONE) {
            success = finishMultiSelect();
        }
        return success;
    }

    // Import image from file
    public void importImage(String path) {
        String err = ATSKGalleryUtils.importImage(
                _surveyUID, path);
        if (err != null)
            toast(err);
        else
            refresh();
    }

    public void importImage(Uri imageUri) {
        importImage(getPathFromURI(imageUri));
    }

    @Override
    public void shotApproved(SurveyPoint sp, double range_m, double az_deg,
            double el_deg, boolean TopCollected) {
    }

    @Override
    public void onResume() {
        super.onResume();
        azpc.putSetting(ATSKConstants.CURRENT_SCREEN,
                ATSKConstants.CURRENT_SCREEN_GALLERY, TAG);
        refresh();
        ATSKToolbarComponent.getToolbar().addVisibilityListener(this);
    }

    @Override
    public void onPause() {
        ATSKToolbarComponent.getToolbar().removeVisibilityListener(this);
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        if (_mapView != null)
            AtakBroadcast.getInstance().unregisterReceiver(_galleryReceiver);
        unbindDrawables(_root);
        if (_adapter != null)
            _adapter.dispose();
        _adapter = null;
        if (_grid != null)
            _grid.setAdapter(null);
        super.onDestroyView();
    }

    private void unbindDrawables(View view) {
        if (view.getBackground() != null)
            view.getBackground().setCallback(null);

        if (view instanceof ThumbView) {
            ((ThumbView) view).dispose();
        } else if (view instanceof ImageView) {
            ((ImageView) view).setImageBitmap(null);
        } else if (view instanceof ViewGroup) {
            // Clear out children
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++)
                unbindDrawables(viewGroup.getChildAt(i));

            // Clear out views
            if (!(view instanceof AdapterView))
                viewGroup.removeAllViews();
        }
    }

    private boolean finishMultiSelect() {
        if (_selectMode == SelectMode.NONE)
            return true;

        final String[] items = _adapter.getSelected();
        if (items == null || items.length == 0) {
            toast("No files are selected.");
            return false;
        }

        // Convert strings to files
        final File[] files = new File[items.length];
        for (int i = 0; i < items.length; i++)
            files[i] = new File(items[i]);

        final boolean export = _selectMode == SelectMode.EXPORT;
        Context pluginContext = ATSKApplication.getInstance()
                .getPluginContext();

        if (export) {
            ATSKGalleryExport eit = new ATSKGalleryExport(
                    _mapView, files, true, _markupCB.isChecked(), null);
            eit.start();
            enableMultiSelect(false);
            refresh();
            return true;
        }

        Drawable icon = pluginContext.getResources().getDrawable(
                R.drawable.ic_menu_delete);
        new AlertDialog.Builder(_mapView.getContext())
                .setTitle("Confirm Delete")
                .setIcon(icon)
                .setMessage("Delete "
                        + files.length + " files?")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        //Now delete
                        Log.d(TAG, "Deleting " + files.length + " images.");
                        for (File f : files)
                            FileSystemUtils.deleteFile(f);
                        enableMultiSelect(false);
                        refresh();
                    }
                })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                dialog.dismiss();
                                enableMultiSelect(false);
                            }
                        })
                .show();
        return true;
    }

    private void enableMultiSelect(boolean enabled) {
        if (_adapter != null)
            _adapter.enableMultiSelect(enabled);
        if (_markupCB != null)
            _markupCB.setVisibility(enabled && _selectMode == SelectMode.EXPORT
                    ? View.VISIBLE : View.GONE);
    }

    private void toast(String msg) {
        Toast.makeText(_mapView.getContext(), msg, Toast.LENGTH_LONG).show();
    }

    private String getPathFromURI(Uri contentURI) {
        if (contentURI == null) {
            Log.w(TAG, "Failed to get path without URI");
            return null;
        }

        Cursor cursor = null;
        try {
            cursor = _mapView.getContext().getContentResolver()
                    .query(contentURI, null, null, null, null);
            if (cursor == null) {
                return contentURI.getPath();
            } else {
                cursor.moveToFirst();
                int idx = cursor
                        .getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                if (idx < 0)
                    return null;

                return cursor.getString(idx);
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to get path from URI: " + contentURI.toString(),
                    e);
        } finally {
            if (cursor != null)
                cursor.close();
        }

        return null;
    }

    private void showImportDialog() {
        ListAdapter adapter = new ArrayAdapter<String>(_mapView.getContext(),
                android.R.layout.simple_list_item_1, new String[] {
                        "Gallery", "File Manager"
                });

        AlertDialog.Builder adb = new AlertDialog.Builder(_mapView.getContext());
        adb.setTitle("Select Import Method");
        adb.setAdapter(adapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        importFromGallery();
                        break;
                    case 1:
                        importFromManager();
                        break;
                }
            }
        });
        adb.show();
    }

    private void importFromGallery() {
        // Import image from storage
        try {
            Intent agc = new Intent();
            agc.setType("image/*");
            agc.setAction(Intent.ACTION_GET_CONTENT);
            ((Activity) _mapView.getContext()).startActivityForResult(
                    agc, ATSKGalleryUtils.IMG_IMPORT_CODE);
        } catch (Exception e) {
            Log.w(TAG, "Failed to ACTION_GET_CONTENT image", e);
            toast("Import failed: No image gallery app installed.");
        }
    }

    private void importFromManager() {
        ImportFileBrowserDialog.show("Select Image To Import",
                new String[] {
                        "jpg", "jpeg", "png",
                        "bmp", "lnk", "ntf"
                },
                new ImportFileBrowserDialog.DialogDismissed() {
                    public void onFileSelected(final File file) {
                        if (file == null)
                            return;
                        importImage(file.getAbsolutePath());
                    }

                    public void onDialogClosed() {
                        //Do nothing
                    }
                }, _mapView.getContext());
    }
}
