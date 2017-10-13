package com.atakmap.android.elevation.dsm;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.atakmap.coremap.conversions.Span;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.maps.coords.AltitudeReference;
import com.atakmap.map.elevation.ElevationData;
import com.atakmap.map.layer.raster.DatasetDescriptor;
import com.atakmap.map.layer.raster.DatasetDescriptorSpiArgs;
import com.atakmap.map.layer.raster.ImageDatasetDescriptor;
import com.atakmap.map.layer.raster.ImageInfo;
import com.atakmap.map.layer.raster.gdal.GdalLayerInfo;

public final class DSMManager {
    private DSMManager() {}
    
    private static File[] dsmDirs;
    private static DSMDatabase db;

    public static void initialize() {
        dsmDirs = FileSystemUtils.getItems("tools/dsm");
        db = new DSMDatabase(FileSystemUtils.getItem("Databases/dsm.db"));
    }
    
    public static void teardown() {
        dsmDirs = null;
        if(db != null) {
            db.close();
            db = null;
        }
    }

    public static DSMDatabase getDb() {
        return db;
    }
    
    public static void refresh() {
        db.validateCatalog();
        refreshImpl(dsmDirs);
    }
    
    private static void refreshImpl(File[] files) {
        for(int i = 0; i < files.length; i++) {
            if(files[i].isDirectory()) {
                File[] children = files[i].listFiles();
                if(children != null)
                    refreshImpl(children);
            } else if(!db.contains(files[i].getAbsolutePath())){
                // parse out elevation info
                final ElevationInfo parsed = parse(files[i]);
                if(parsed != null)
                    db.insert(parsed);
            }
        }
    }
    
    private static ElevationInfo parse(File file) {
        try {
            // use the GDAL dataset handler to the parsing
            Set<DatasetDescriptor> descs = GdalLayerInfo.INSTANCE.create(new DatasetDescriptorSpiArgs(file, null));
            if(descs == null || descs.isEmpty())
                return null;
            
            // XXX - 
            for(DatasetDescriptor desc : descs) {
                if(!(desc instanceof ImageDatasetDescriptor))
                    continue;
    
                ImageDatasetDescriptor image = (ImageDatasetDescriptor)desc;
                return new ElevationInfo(GdalLayerInfo.getGdalFriendlyUri(image),
                                         "geotiff",
                                         image.getUpperLeft(),
                                         image.getUpperRight(),
                                         image.getLowerRight(),
                                         image.getLowerLeft(),
                                         image.getMinResolution(null),
                                         image.getMaxResolution(null),
                                         image.getWidth(),
                                         image.getHeight(),
                                         image.getSpatialReferenceID(),
                                         ElevationData.MODEL_SURFACE,
                                         AltitudeReference.HAE,
                                         Span.METER,
                                         null);
            }

            return null;
        } catch(Throwable t) {
            // XXX - log error
            return null;
        }
    }
    
    static Map<String, ElevationCache> caches = null;

    public static ElevationCache getCache(ElevationInfo info) {
        if(caches == null) {
            caches = new HashMap<String, ElevationCache>();
            File d = FileSystemUtils.getItem("tmp");
            if(d.exists())
                FileSystemUtils.delete(d);
        }

        ElevationCache retval = caches.get(info.path);
        if(retval == null) {
            File d = FileSystemUtils.getItem("tmp");
            d.mkdir();
            
            try {
                retval = new ElevationCache(File.createTempFile("elevation", ".cache", d).getAbsolutePath());
                caches.put(info.path, retval);
            } catch(IOException e) {
                throw new RuntimeException(e);
            }
        }
        return retval;
    }
}
