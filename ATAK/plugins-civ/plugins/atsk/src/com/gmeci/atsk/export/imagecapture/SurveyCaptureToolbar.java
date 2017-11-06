
package com.gmeci.atsk.export.imagecapture;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.preference.PreferenceManager;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;

import com.atakmap.android.atsk.plugin.R;
import com.atakmap.android.dropdown.DropDownManager;
import com.atakmap.android.imagecapture.CapturePrefs;
import com.atakmap.android.imagecapture.GLCaptureTool;
import com.atakmap.android.mapcompass.CompassArrowMapComponent;
import com.atakmap.android.maps.MapMode;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.toolbar.IToolbarExtension;
import com.atakmap.android.toolbar.Tool;
import com.atakmap.android.toolbar.ToolbarBroadcastReceiver;
import com.atakmap.android.tools.ActionBarView;
import com.atakmap.coremap.log.Log;
import com.atakmap.map.layer.opengl.GLLayerFactory;
import com.gmeci.atsk.export.ATSKMissionPackageManager;
import com.gmeci.atsk.resources.ATSKApplication;
import com.atakmap.android.ipc.AtakBroadcast;
import com.gmeci.atsk.toolbar.ATSKToolbar;
import com.gmeci.atsk.toolbar.ATSKToolbarComponent;
import com.gmeci.atskservice.resolvers.AZProviderClient;
import com.gmeci.core.ATSKConstants;
import com.gmeci.core.SurveyData;

import android.content.Intent;
import android.widget.RadioGroup;

import java.io.File;
import java.util.List;

/**
 * Created by sommerj on 3/18/2016.
 * Toolbar used to begin capture and set capture parameters
 */
public class SurveyCaptureToolbar implements IToolbarExtension,
        View.OnClickListener,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "SurveyCaptureToolbar";
    public static final String IDENTIFIER = "com.gmeci.atsk.export.imagecapture.TOOLBAR";

    private SurveyImageCapture _imgCap;

    private ActionBarView _root;
    private final MapView _mapView;
    private final Context _plugin;
    private final SharedPreferences _prefs;

    private ImageButton _imageryBtn;
    private boolean _showImagery = true;
    private boolean _visible = false;
    private boolean _restarting = false;

    public SurveyCaptureToolbar(MapView mapView) {
        _mapView = mapView;
        _plugin = ATSKApplication.getInstance().getPluginContext();
        _prefs = PreferenceManager.getDefaultSharedPreferences(_mapView
                .getContext());
        ToolbarBroadcastReceiver.getInstance().registerToolbarComponent(
                IDENTIFIER, this);
    }

    private void setupView() {
        if (_root == null) {
            _root = (ActionBarView) LayoutInflater.from(_plugin).inflate(
                    R.layout.image_capture_toolbar, _mapView, false);

            _root.setEmbedded(false);
            _root.setClosable(false);

            _root.findViewById(R.id.survey_img_capture)
                    .setOnClickListener(this);

            _imageryBtn = (ImageButton) _root.findViewById(
                    R.id.survey_imagery_toggle);
            _imageryBtn.setOnClickListener(this);

            _root.findViewById(R.id.survey_capture_settings)
                    .setOnClickListener(this);
            showImagery(CapturePrefs.get(CapturePrefs.PREF_SHOW_IMAGERY, true));
        }
    }

    @Override
    public List<Tool> getTools() {
        return null;
    }

    @Override
    public boolean hasToolbar() {
        return true;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (key.equals(SurveyCapturePrefs.PREF_IMAGE_ITEMS) && _visible) {
            _restarting = true;
            AtakBroadcast.getInstance().sendBroadcast(new Intent(
                    ToolbarBroadcastReceiver.UNSET_TOOLBAR));
        }
    }

    @Override
    public ActionBarView getToolbarView() {
        setupView();
        return _root;
    }

    @Override
    public void onToolbarVisible(boolean v) {
        if (v == _visible)
            return;
        Display disp = ((Activity) _mapView.getContext())
                .getWindowManager().getDefaultDisplay();
        Point size = new Point();
        disp.getSize(size);
        Point focus = _mapView.getMapController().getFocusPoint();
        ATSKToolbar tb = ATSKToolbarComponent.getToolbar();
        boolean portraitMode = size.y > size.x;
        boolean cropped = SurveyCapturePrefs.get(SurveyCapturePrefs
                .PREF_IMAGE_DIM).equals(SurveyCapturePrefs.IMAGE_DIM_LETTER);
        if (v) {
            SurveyData survey = null;
            AZProviderClient azpc = new AZProviderClient(_plugin);
            if (azpc.Start()) {
                survey = azpc.getAZ(azpc.getSetting(
                        ATSKConstants.CURRENT_SURVEY, TAG), false);
                azpc.Stop();
            }

            if (survey == null)
                return;

            AtakBroadcast.getInstance().sendBroadcast(new Intent(
                    MapMode.NORTH_UP.getIntent()));
            AtakBroadcast.getInstance().sendBroadcast(new Intent(
                    CompassArrowMapComponent.HIDE_ACTION));
            DropDownManager.getInstance().hidePane();
            _prefs.registerOnSharedPreferenceChangeListener(this);
            File outFile = new File(ATSKMissionPackageManager
                    .getFileName(survey, ""));
            File outDir = outFile.getParentFile();
            if (!outDir.exists() && !outDir.mkdirs())
                Log.w(TAG, "Failed to create output directory: " + outDir);
            GLLayerFactory.register(GLCaptureTool.SPI);
            _imgCap = new SurveyImageCapture(_mapView, outDir,
                    outFile.getName());
            if (cropped) {
                if (portraitMode)
                    _mapView.getMapController().panBy(0, tb.getHeight()
                            * focus.y / _mapView.getHeight(), false);
                else
                    _mapView.getMapController().panBy(tb.getWidth()
                            * focus.x / _mapView.getWidth(), 0, false);
            }
            _visible = true;
        } else {
            if (_imgCap != null) {
                _imgCap.dispose();
                _imgCap = null;
            }
            _prefs.unregisterOnSharedPreferenceChangeListener(this);
            if (cropped) {
                if (portraitMode)
                    _mapView.getMapController().panBy(0, -tb.getHeight()
                            * focus.y / _mapView.getHeight(), false);
                else
                    _mapView.getMapController().panBy(-tb.getWidth()
                            * focus.x / _mapView.getWidth(), 0, false);
            }
            if (!_restarting) {
                AtakBroadcast.getInstance().sendBroadcast(new Intent(
                        CompassArrowMapComponent.SHOW_ACTION));
                DropDownManager.getInstance().unHidePane();
            } else
                // XXX - hack; restart tool after 100ms
                _mapView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        _restarting = false;
                        Intent toolbar = new Intent(ToolbarBroadcastReceiver
                                .OPEN_TOOLBAR);
                        toolbar.putExtra("toolbar", SurveyCaptureToolbar
                                .IDENTIFIER);
                        AtakBroadcast.getInstance().sendBroadcast(toolbar);
                    }
                }, 100);
            _visible = false;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.survey_img_capture:
                if (_imgCap != null)
                    _imgCap.startCapture();
                break;
            case R.id.survey_imagery_toggle:
                showImagery(!_showImagery);
                break;
            case R.id.survey_capture_settings:
                showSettingsDialog();
                break;
        }
    }

    private void showImagery(boolean show) {
        _showImagery = show;
        _imageryBtn.setImageResource(_showImagery ?
                R.drawable.white_box : R.drawable.imagery);
        CapturePrefs.set(CapturePrefs.PREF_SHOW_IMAGERY, _showImagery);
    }

    // Display selection dialog for all available capture resolutions
    private void showSettingsDialog() {
        View v = LayoutInflater.from(_plugin).inflate(
                R.layout.survey_capture_res_dialog, _mapView,
                false);

        // Image dimensions
        String dim = (String) SurveyCapturePrefs.get(
                SurveyCapturePrefs.PREF_IMAGE_DIM);
        final RadioGroup dimGroup = (RadioGroup) v.findViewById(
                R.id.image_dim_group);
        if (dim.equals(SurveyCapturePrefs.IMAGE_DIM_FULL))
            dimGroup.check(R.id.image_dim_full);
        else
            dimGroup.check(R.id.image_dim_letter);

        // Resolution
        int captureRes = CapturePrefs.get(CapturePrefs.PREF_RES, 2);
        final RadioGroup resGroup = (RadioGroup) v.findViewById(R.id.res_group);
        if (captureRes < 4)
            resGroup.check(R.id.res_low);
        else
            resGroup.check(R.id.res_high);

        // Image format
        String format = CapturePrefs.get(CapturePrefs.PREF_FORMAT, ".jpg");
        final RadioGroup fmtGroup = (RadioGroup) v.findViewById(
                R.id.format_group);
        if (format.equals(".png"))
            fmtGroup.check(R.id.format_png);
        else
            fmtGroup.check(R.id.format_jpeg);

        // Items to capture
        final RadioGroup itmGroup = (RadioGroup) v.findViewById(
                R.id.items_group);
        if (SurveyCapturePrefs.get(SurveyCapturePrefs.PREF_IMAGE_ITEMS)
                .equals(SurveyCapturePrefs.IMAGE_ITEMS_ATSK))
            itmGroup.check(R.id.items_atsk);
        else
            itmGroup.check(R.id.items_all);

        AlertDialog.Builder adb = new AlertDialog.Builder(_mapView.getContext());
        adb.setTitle(_plugin.getString(R.string.imgcap_settings));
        adb.setView(v);
        adb.setPositiveButton(_plugin.getString(R.string.done),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        // Image dimensions
                        CapturePrefs
                                .set(SurveyCapturePrefs.PREF_IMAGE_DIM,
                                        dimGroup.
                                                getCheckedRadioButtonId() == R.id.image_dim_letter
                                                ? SurveyCapturePrefs.IMAGE_DIM_LETTER
                                                : SurveyCapturePrefs.IMAGE_DIM_FULL);

                        // Capture resolution
                        CapturePrefs.set(CapturePrefs.PREF_RES, resGroup.
                                getCheckedRadioButtonId() == R.id.res_low ? 2
                                : 6);

                        // Capture format
                        CapturePrefs.set(CapturePrefs.PREF_FORMAT, fmtGroup.
                                getCheckedRadioButtonId() == R.id.format_jpeg
                                ? ".jpg" : ".png");

                        // Capture items
                        CapturePrefs
                                .set(SurveyCapturePrefs.PREF_IMAGE_ITEMS,
                                        itmGroup.
                                                getCheckedRadioButtonId() == R.id.items_atsk
                                                ? SurveyCapturePrefs.IMAGE_ITEMS_ATSK
                                                : SurveyCapturePrefs.IMAGE_ITEMS_ALL);
                    }
                });
        adb.setNegativeButton(_plugin.getString(R.string.cancel), null);
        adb.show();
    }
}
