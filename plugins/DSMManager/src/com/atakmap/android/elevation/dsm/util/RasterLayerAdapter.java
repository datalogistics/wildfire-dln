package com.atakmap.android.elevation.dsm.util;

import java.util.Collection;
import java.util.LinkedList;

import com.atakmap.android.elevation.dsm.DSMManager;
import com.atakmap.android.elevation.dsm.ElevationInfo;
import com.atakmap.map.layer.feature.geometry.Geometry;
import com.atakmap.map.layer.raster.DatasetDescriptor;
import com.atakmap.map.layer.raster.RasterLayer2;
import com.atakmap.map.layer.raster.mosaic.MosaicDatabase2;
import com.atakmap.map.projection.EquirectangularMapProjection;
import com.atakmap.map.projection.Projection;

public final class RasterLayerAdapter implements RasterLayer2 {

    public final static RasterLayer2 INSTANCE = new RasterLayerAdapter();
    
    private RasterLayerAdapter() {}
    
    @Override
    public <T extends Extension> T getExtension(Class<T> clazz) {
        return null;
    }

    @Override
    public void setVisible(boolean visible) {}

    @Override
    public boolean isVisible() {
        return true;
    }

    @Override
    public void addOnLayerVisibleChangedListener(OnLayerVisibleChangedListener l) {}

    @Override
    public void removeOnLayerVisibleChangedListener(OnLayerVisibleChangedListener l) {}

    @Override
    public String getName() {
        return "DSM";
    }

    @Override
    public void setSelection(String type) {}

    @Override
    public String getSelection() {
        return null;
    }

    @Override
    public boolean isAutoSelect() {
        return false;
    }

    @Override
    public Collection<String> getSelectionOptions() {
        MosaicDatabase2.Cursor result = null;
        try {
            LinkedList<String> retval = new LinkedList<String>();
            result = DSMManager.getDb().dsmmdb.query(null);
            while(result.moveToNext())
                retval.add(result.getPath());
            return retval;
        } finally {
            if(result != null)
                result.close();
        }
    }

    @Override
    public void addOnSelectionChangedListener(OnSelectionChangedListener l) {}

    @Override
    public void removeOnSelectionChangedListener(OnSelectionChangedListener l) {}

    @Override
    public Geometry getGeometry(String selection) {
        ElevationInfo info = DSMManager.getDb().getElevationInfo(selection);
        if(info == null)
            return null;
        return DatasetDescriptor.createSimpleCoverage(info.upperLeft,
                                                      info.upperRight,
                                                      info.lowerRight,
                                                      info.lowerLeft);
    }

    @Override
    public double getMinimumResolution(String selection) {
        return Double.MAX_VALUE;
    }

    @Override
    public double getMaximumResolution(String selection) {
        return 0d;
    }

    @Override
    public Projection getPreferredProjection() {
        return EquirectangularMapProjection.INSTANCE;
    }

    @Override
    public void addOnPreferredProjectionChangedListener(OnPreferredProjectionChangedListener l) {}

    @Override
    public void removeOnPreferredProjectionChangedListener(OnPreferredProjectionChangedListener l) {}

    @Override
    public void setVisible(String selection, boolean visible) {}

    @Override
    public boolean isVisible(String selection) {
        return true;
    }

    @Override
    public void addOnSelectionVisibleChangedListener(OnSelectionVisibleChangedListener l) {}

    @Override
    public void removeOnSelectionVisibleChangedListener(OnSelectionVisibleChangedListener l) {}

    @Override
    public float getTransparency(String selection) {
        return 0f;
    }

    @Override
    public void setTransparency(String selection, float value) {}

    @Override
    public void addOnSelectionTransparencyChangedListener(OnSelectionTransparencyChangedListener l) {}

    @Override
    public void removeOnSelectionTransparencyChangedListener(OnSelectionTransparencyChangedListener l) {}
}
