package com.atakmap.android.elevation.dsm;

import com.atakmap.coremap.conversions.Span;
import com.atakmap.coremap.maps.coords.AltitudeReference;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.map.layer.raster.ImageInfo;

public final class ElevationInfo extends ImageInfo {

    public final double minGsd;
    public final int model;
    public final AltitudeReference reference;
    public final Span units;
    public final String cache;

    public ElevationInfo(String path,
                         String type,
                         GeoPoint upperLeft,
                         GeoPoint upperRight,
                         GeoPoint lowerRight,
                         GeoPoint lowerLeft,
                         double minGsd,
                         double maxGsd,
                         int width,
                         int height,
                         int srid,
                         int model,
                         AltitudeReference reference,
                         Span units,
                         String cache) {
        super(path,
              type,
              false,
              upperLeft,
              upperRight,
              lowerRight,
              lowerLeft,
              maxGsd,
              width,
              height,
              srid);
        
        this.minGsd = minGsd;
        this.model = model;
        this.reference = reference;
        this.units = units;
        this.cache = cache;
    }
}