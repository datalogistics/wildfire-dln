
package com.gmeci.atsk.export.imagecapture;

import android.content.SharedPreferences;

import com.atakmap.android.hierarchy.filters.FOVFilter;
import com.atakmap.android.imagecapture.CapturePP;
import com.atakmap.android.imagecapture.CapturePrefs;
import com.atakmap.android.imagecapture.ImageCapture;
import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.toolbars.RangeAndBearingMapComponent;
import com.atakmap.coremap.maps.coords.GeoBounds;
import com.atakmap.opengl.GLES20FixedPipeline;
import com.gmeci.atsk.ATSKATAKConstants;
import com.gmeci.atsk.gallery.ATSKGalleryExport;
import com.gmeci.atsk.resources.ATSKApplication;
import com.gmeci.atskservice.resolvers.AZProviderClient;
import com.gmeci.core.ATSKConstants;
import com.gmeci.core.SurveyData;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Survey diagram capture tool
 */
public class SurveyImageCapture extends ImageCapture implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "SurveyImageCapture";
    private static final double LETTER_RATIO = 11d / 8.5d;

    private SurveyData _survey;
    private final String _name;
    private double _dimRatio = -1;

    // GL overlay parameters
    private FloatBuffer _boxVerts;

    public SurveyImageCapture(MapView mapView, File outFile, String outName) {
        super(mapView, outFile, SurveyCaptureToolbar.IDENTIFIER);
        _name = outName;
        updateDimensions();
        SharedPreferences prefs = SurveyCapturePrefs.getPrefs();
        if (prefs != null)
            prefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void dispose() {
        super.dispose();
        SharedPreferences prefs = SurveyCapturePrefs.getPrefs();
        if (prefs != null)
            prefs.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (key.equals(SurveyCapturePrefs.PREF_IMAGE_DIM)) {
            updateDimensions();
            _glSubject.onMapViewResized(_mapView);
        }
    }

    private void updateDimensions() {
        if (SurveyCapturePrefs.get(SurveyCapturePrefs.PREF_IMAGE_DIM)
                .equals(SurveyCapturePrefs.IMAGE_DIM_FULL))
            _dimRatio = -1;
        else
            _dimRatio = LETTER_RATIO;
    }

    private SurveyData getSurvey() {
        if (_survey == null) {
            AZProviderClient azpc = new AZProviderClient(
                    ATSKApplication.getInstance().getPluginContext());
            if (azpc.Start()) {
                _survey = azpc.getAZ(azpc.getSetting(
                        ATSKConstants.CURRENT_SURVEY, TAG), false);
                azpc.Stop();
            }
        }
        return _survey;
    }

    @Override
    public String getName() {
        return _name;
    }

    @Override
    public List<MapItem> getAllItems() {
        SurveyData survey = getSurvey();
        if (survey == null)
            return new ArrayList<MapItem>();

        if (!useCustomRenderer())
            return super.getAllItems();

        List<MapGroup> groups = new ArrayList<MapGroup>();
        MapGroup azGrp = _mapView.getRootGroup().findMapGroup(
                ATSKATAKConstants.ATSK_MAP_GROUP_AZ);
        if (azGrp != null) {
            MapGroup surveyGroup = azGrp.findMapGroup(survey.uid);
            if (surveyGroup != null)
                groups.add(surveyGroup);
        }

        MapGroup obsGrp = _mapView.getRootGroup().findMapGroup(
                ATSKATAKConstants.ATSK_MAP_GROUP_OBS);
        if (obsGrp != null) {
            MapGroup specificGrp = obsGrp.findMapGroup(survey.uid);
            if (specificGrp != null)
                groups.add(specificGrp);
            else
                groups.add(obsGrp);
        }

        // Include range and bearing items
        MapGroup rabGroup = RangeAndBearingMapComponent.getGroup();
        if (rabGroup != null)
            groups.add(rabGroup);

        // Extract items
        Set<MapItem> ret = new HashSet<MapItem>();
        for (MapGroup group : groups)
            ret.addAll(getAllItems(group, null));

        return new ArrayList<MapItem>(ret);
    }

    @Override
    public List<MapItem> getItems(GeoBounds bounds) {
        FOVFilter fovFilter = new FOVFilter(bounds);
        List<MapItem> items = getAllItems();
        List<MapItem> ret = new ArrayList<MapItem>();
        for (MapItem mi : items) {
            if (mi.getVisible() && fovFilter.accept(mi))
                ret.add(mi);
        }
        return ret;
    }

    @Override
    public boolean showMapImagery() {
        return CapturePrefs.get(CapturePrefs.PREF_SHOW_IMAGERY, true);
    }

    @Override
    public CapturePP getPostProcessor() {
        return new SurveyCapturePP(_glSubject, getSurvey());
    }

    @Override
    public boolean useCustomRenderer() {
        return SurveyCapturePrefs.get(SurveyCapturePrefs.PREF_IMAGE_ITEMS)
                .equals(SurveyCapturePrefs.IMAGE_ITEMS_ATSK);
    }

    @Override
    public boolean kmzOnly() {
        return false;
    }

    @Override
    public int getWidth() {
        if (_dimRatio == -1 || CapturePrefs.inPortraitMode())
            return super.getWidth();
        return Math.min(super.getWidth(),
                (int) (super.getHeight() * _dimRatio));
    }

    @Override
    public int getHeight() {
        if (_dimRatio == -1 || !CapturePrefs.inPortraitMode())
            return super.getHeight();
        return Math.min(super.getHeight(),
                (int) (super.getWidth() * _dimRatio));
    }

    @Override
    public GeoBounds getBounds() {
        return new GeoBounds(_mapView.inverse(0, 0), _mapView
                .inverse(getWidth(), getHeight()));
    }

    @Override
    public void drawOverlays() {
        float width = super.getWidth();
        float height = super.getHeight();
        if (_boxVerts == null) {
            // Darkened box covering cropped out area
            ByteBuffer bb = ByteBuffer.allocateDirect(4 * 2 * 4);
            bb.order(ByteOrder.nativeOrder());
            _boxVerts = bb.asFloatBuffer();
            _boxVerts.clear();
        }
        if (CapturePrefs.inPortraitMode()) {
            float boundary = getHeight();
            _boxVerts.put(0, 0); // Top left
            _boxVerts.put(1, height - boundary);
            _boxVerts.put(2, width); // Bottom left
            _boxVerts.put(3, height - boundary);
            _boxVerts.put(4, 0); // Top right
            _boxVerts.put(5, 0);
            _boxVerts.put(6, width); // Bottom right
            _boxVerts.put(7, 0);
        } else {
            float boundary = getWidth();
            _boxVerts.put(0, boundary); // Top left
            _boxVerts.put(1, 0);
            _boxVerts.put(2, boundary); // Bottom left
            _boxVerts.put(3, height);
            _boxVerts.put(4, width); // Top right
            _boxVerts.put(5, 0);
            _boxVerts.put(6, width); // Bottom right
            _boxVerts.put(7, height);
        }
        _boxVerts.clear();

        GLES20FixedPipeline.glPushMatrix();
        GLES20FixedPipeline.glEnable(GLES20FixedPipeline.GL_BLEND);
        GLES20FixedPipeline.glBlendFunc(GLES20FixedPipeline.GL_SRC_ALPHA,
                GLES20FixedPipeline.GL_ONE_MINUS_SRC_ALPHA);
        GLES20FixedPipeline.glEnableClientState(
                GLES20FixedPipeline.GL_VERTEX_ARRAY);

        GLES20FixedPipeline.glVertexPointer(2,
                GLES20FixedPipeline.GL_FLOAT, 0, _boxVerts);

        GLES20FixedPipeline.glColor4f(0, 0, 0, 0.6f);
        GLES20FixedPipeline.glDrawArrays(
                GLES20FixedPipeline.GL_TRIANGLE_STRIP, 0,
                4);

        GLES20FixedPipeline.glDisableClientState(
                GLES20FixedPipeline.GL_VERTEX_ARRAY);
        GLES20FixedPipeline.glDisable(GLES20FixedPipeline.GL_BLEND);
        GLES20FixedPipeline.glPopMatrix();
    }

    @Override
    protected void saveKMZ(File outImage, File tmpDir, CapturePP postDraw) {
        // Save PDF copy
        String path = outImage.getAbsolutePath();
        path = path.substring(0, path.lastIndexOf("."));
        ATSKGalleryExport.exportToPDF(new File(path + ".pdf"), outImage);

        // Save KMZ
        super.saveKMZ(outImage, tmpDir, postDraw);
    }
}
