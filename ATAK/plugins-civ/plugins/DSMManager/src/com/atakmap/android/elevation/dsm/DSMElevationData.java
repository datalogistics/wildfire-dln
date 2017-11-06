package com.atakmap.android.elevation.dsm;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Iterator;

import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;

import com.atakmap.coremap.conversions.Span;
import com.atakmap.coremap.conversions.SpanUtilities;
import com.atakmap.coremap.maps.conversion.EGM96;
import com.atakmap.coremap.maps.coords.Altitude;
import com.atakmap.coremap.maps.coords.AltitudeReference;
import com.atakmap.coremap.maps.coords.AltitudeSource;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.map.elevation.AbstractElevationData;
import com.atakmap.map.elevation.ElevationData;
import com.atakmap.map.elevation.ElevationDataSpi;
import com.atakmap.map.layer.raster.DatasetProjection2;
import com.atakmap.map.layer.raster.DefaultDatasetProjection2;
import com.atakmap.map.layer.raster.ImageInfo;
import com.atakmap.math.MathUtils;
import com.atakmap.math.PointD;

public final class DSMElevationData extends AbstractElevationData {

    public final static ElevationDataSpi SPI = new ElevationDataSpi() {

        @Override
        public ElevationData create(ImageInfo object) {
            ElevationInfo info = DSMManager.getDb().getElevationInfo(object.path);
            if(info == null)
                return null;
            return new DSMElevationData(info);
        }

        @Override
        public int getPriority() {
            return 2;
        }
    };

    private final ElevationInfo info;
    private final DatasetProjection2 proj;
    
    DSMElevationData(ElevationInfo info) {
        super(info.model, info.type, info.maxGsd);
        
        this.info = info;
        this.proj = new DefaultDatasetProjection2(info.srid,
                                                  info.width,
                                                  info.height,
                                                  info.upperLeft,
                                                  info.upperRight,
                                                  info.lowerRight,
                                                  info.lowerLeft);
    }

    @Override
    public double getResolution() {
        return this.info.maxGsd;
    }

    @Override
    public double getElevation(double latitude, double longitude) {
        PointD img = new PointD(0d, 0d);
        if(!this.proj.groundToImage(new GeoPoint(latitude, longitude), img))
            return Double.NaN;
        
        Dataset dataset = null;
        try {
            dataset = gdal.Open(this.info.path);
            if(dataset == null)
                return Double.NaN;
            
            Double[] ndv = new Double[1];
            dataset.GetRasterBand(1).GetNoDataValue(ndv);
            final double noDataValue = (ndv[0] != null) ? ndv[0].doubleValue() : Double.NaN;

            return this.getElevationImpl(dataset,
                                         latitude,
                                         longitude,
                                         img.x, img.y,
                                         noDataValue);
        } catch(Throwable t) {
            return Double.NaN;
        } finally {
            if(dataset != null)
                dataset.delete();
        }
    }

    @Override
    public void getElevation(Iterator<GeoPoint> points, double[] elevations, Hints hints) {
        Dataset dataset = null;
        try {
            dataset = gdal.Open(this.info.path);
            if(dataset == null) {
                Arrays.fill(elevations, Double.NaN);
                return;
            }

            int level = (hints != null) ? MathUtils.clamp((int)(Math.log(hints.resolution / info.maxGsd)/Math.log(2d)), 0, 10) : 0;
                
            Double[] ndv = new Double[1];
            dataset.GetRasterBand(1).GetNoDataValue(ndv);
            final double noDataValue = (ndv[0] != null) ? ndv[0].doubleValue() : Double.NaN;

            ElevationCache cache = DSMManager.getCache(this.info);
            
            PointD img = new PointD(0d, 0d);
            GeoPoint geo;
            int idx = 0;
            while(points.hasNext()) {
                geo = points.next();
                if(!this.proj.groundToImage(geo, img)) {
                    elevations[idx++] = Double.NaN;
                } else if(true || hints == null || !hints.preferSpeed){
                    elevations[idx++] = this.getElevationImpl(dataset,
                                                              geo.getLatitude(),
                                                              geo.getLongitude(),
                                                              img.x, img.y,
                                                              noDataValue);
                } else {
                    cache.getValue(dataset, level, img.x, img.y);
                }
            }
        } catch(Throwable t) {
            Arrays.fill(elevations, Double.NaN);
            t.printStackTrace();
        } finally {
            if(dataset != null)
                dataset.delete();
        }
    }

    private double getElevationImpl(Dataset dataset, double lat, double lon, double x, double y, double noDataValue) {
        // XXX - doing nearest neighbor for now -- consider interpolation.
        x = (int)(x+0.5d);
        y = (int)(y+0.5d);

        // handle out of bounds
        if(x < 0 || x >= dataset.GetRasterXSize())
            return Double.NaN;
        if(y < 0 || y >= dataset.GetRasterYSize())
            return Double.NaN;
        

        final int dataType = dataset.GetRasterBand(1).getDataType();
        ByteBuffer arr = ByteBuffer.allocateDirect(8);
        arr.order(ByteOrder.nativeOrder());

        // read the pixel
        final int success = dataset.ReadRaster_Direct(
                (int)x, (int)y, 1, 1, // src x,y,w,h
                1, 1, // dst w,h
                dataType,
                arr,
                new int[] {1} // bands
                );
        if (success != gdalconst.CE_None)
            return Double.NaN;

        // XXX - no data value handling

        double retval;
        if(dataType == gdalconst.GDT_Byte)
            retval = (arr.get(0)&0xFF); // XXX - assuming unsigned ???
        else if(dataType == gdalconst.GDT_UInt16)
            retval = (arr.getShort(0)&0xFFFF);
        else if(dataType == gdalconst.GDT_Int16)
            retval = arr.getShort(0);
        else if(dataType == gdalconst.GDT_UInt32)
            retval = ((long)arr.getInt(0)&0xFFFFFFFFL);
        else if(dataType == gdalconst.GDT_Int32)
            retval = arr.getInt(0);
        else if(dataType == gdalconst.GDT_Float32)
            retval = arr.getFloat(0);
        else if(dataType == gdalconst.GDT_Float64)
            retval = arr.getDouble(0);
        else
            return Double.NaN;
        if(retval == noDataValue)
            return Double.NaN;
        
        retval = SpanUtilities.convert(retval, this.info.units, Span.METER);
        if(this.info.reference != AltitudeReference.HAE) {
            retval = EGM96.getInstance().getHAE(lat,
                                                lon,
                                                new Altitude(retval,
                                                             this.info.reference,
                                                             AltitudeSource.UNKNOWN)).getValue();
        }
        return retval;
    }
}
