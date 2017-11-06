package com.atakmap.android.elevation.dsm;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.map.layer.feature.geometry.Envelope;
import com.atakmap.map.layer.feature.geometry.Geometry;
import com.atakmap.map.layer.raster.DatasetDescriptor;
import com.atakmap.map.layer.raster.DatasetDescriptorSpiArgs;
import com.atakmap.map.layer.raster.ImageDatasetDescriptor;
import com.atakmap.map.layer.raster.ImageInfo;
import com.atakmap.map.layer.raster.gdal.GdalLayerInfo;
import com.atakmap.map.layer.raster.mosaic.FilterMosaicDatabaseCursor2;
import com.atakmap.map.layer.raster.mosaic.MosaicDatabase2;
import com.atakmap.map.layer.raster.mosaic.MultiplexingMosaicDatabaseCursor2;
import com.atakmap.math.MathUtils;
import com.atakmap.math.Rectangle;
import com.atakmap.util.Filter;

public class DSMMosaicDatabase implements MosaicDatabase2 {

    private File dsmDir;

    @Override
    public String getType() {
        return "DSM";
    }

    @Override
    public void open(File f) {
        this.dsmDir = f;
    }

    @Override
    public void close() {
        this.dsmDir = null;
    }

    @Override
    public Coverage getCoverage() {
        return null;
    }

    @Override
    public void getCoverages(Map<String, Coverage> coverages) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public Coverage getCoverage(String type) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Cursor query(QueryParameters params) {
        Cursor retval = new CursorImpl(this.dsmDir);
        Collection<Filter<MosaicDatabase2.Cursor>> filters = new ArrayList<Filter<MosaicDatabase2.Cursor>>();
        if(params != null) {
            if(params.precisionImagery != null && params.precisionImagery.booleanValue())
                return new MultiplexingMosaicDatabaseCursor2(Collections.<Cursor>emptySet());
            if(!Double.isNaN(params.minGsd) || !Double.isNaN(params.maxGsd))
                filters.add(new ResolutionFilter(params.minGsd, params.maxGsd));
            if(params.types != null)
                filters.add(new TypeFilter(params.types));
            if(params.path != null)
                filters.add(new PathFilter(params.path));
            if(params.spatialFilter != null)
                filters.add(new SpatialFilter(params.spatialFilter));
            if(params.srid != -1)
                filters.add(new SridFilter(params.srid));
        }
        if(!filters.isEmpty())
            retval = FilterMosaicDatabaseCursor2.filter(retval, filters);
        return retval;
    }

    /**************************************************************************/
    
    private static class CursorImpl implements Cursor {

        private File[] list;
        private int idx;
        private ImageInfo row;

        CursorImpl(File dir) {
            this.list = dir.listFiles();
            this.idx = -1;
            this.row = null;
        }
        
        private void validateRow() {
            if(this.list == null)
                throw new IllegalStateException();
            if(this.idx < 0 || this.idx >= this.list.length)
                throw new IndexOutOfBoundsException();
            
            // use the GDAL dataset handler to the parsing
            Set<DatasetDescriptor> descs = GdalLayerInfo.INSTANCE.create(new DatasetDescriptorSpiArgs(this.list[this.idx], null));
            if(descs == null || descs.isEmpty())
                return;
            
            // XXX - 
            for(DatasetDescriptor desc : descs) {
                if(!(desc instanceof ImageDatasetDescriptor))
                    continue;

                ImageDatasetDescriptor image = (ImageDatasetDescriptor)desc;
                this.row = new ImageInfo(GdalLayerInfo.getGdalFriendlyUri(image),
                                         "geotiff",
                                         image.isPrecisionImagery(),
                                         image.getUpperLeft(),
                                         image.getUpperRight(),
                                         image.getLowerRight(),
                                         image.getLowerLeft(),
                                         image.getMaxResolution(null),
                                         image.getWidth(),
                                         image.getHeight(),
                                         image.getSpatialReferenceID());
                break;
            }
        }

        @Override
        public boolean moveToNext() {
            if(this.list == null)
                return false;
            while(this.idx < this.list.length) {
                this.idx++;
                this.row = null;
                if(this.idx == this.list.length)
                    break;
                this.validateRow();
                if(this.row != null)
                    break;
            }
            return (this.row != null);
        }

        @Override
        public void close() { this.list = null; }

        @Override
        public boolean isClosed() { return (this.list == null); }

        @Override
        public GeoPoint getUpperLeft() { return this.row.upperLeft; }

        @Override
        public GeoPoint getUpperRight() { return this.row.upperRight; }

        @Override
        public GeoPoint getLowerRight() { return this.row.lowerRight; }

        @Override
        public GeoPoint getLowerLeft() { return this.row.lowerLeft; }

        @Override
        public double getMinLat() {
            return MathUtils.min(this.row.upperLeft.getLatitude(),
                                 this.row.upperRight.getLatitude(),
                                 this.row.lowerRight.getLatitude(),
                                 this.row.lowerLeft.getLatitude());
        }

        @Override
        public double getMinLon() {
            return MathUtils.min(this.row.upperLeft.getLongitude(),
                                 this.row.upperRight.getLongitude(),
                                 this.row.lowerRight.getLongitude(),
                                 this.row.lowerLeft.getLongitude());
        }

        @Override
        public double getMaxLat() {
            return MathUtils.max(this.row.upperLeft.getLatitude(),
                                 this.row.upperRight.getLatitude(),
                                 this.row.lowerRight.getLatitude(),
                                 this.row.lowerLeft.getLatitude());
        }

        @Override
        public double getMaxLon() {
            return MathUtils.max(this.row.upperLeft.getLongitude(),
                                 this.row.upperRight.getLongitude(),
                                 this.row.lowerRight.getLongitude(),
                                 this.row.lowerLeft.getLongitude());
        }

        @Override
        public String getPath() { return this.row.path; }

        @Override
        public String getType() { return this.row.type; }

        @Override
        public double getMinGSD() { return this.row.maxGsd; }

        @Override
        public double getMaxGSD() { return this.row.maxGsd; }

        @Override
        public int getWidth() { return this.row.width; }

        @Override
        public int getHeight() { return this.row.height; }

        @Override
        public int getId() {
            // XXX - 
            return this.idx;
        }

        @Override
        public int getSrid() { return this.row.srid; }

        @Override
        public boolean isPrecisionImagery() { return false; }

        @Override
        public Frame asFrame() { return new Frame(this); }
    }
    
    /**************************************************************************/
    
    private final static class ResolutionFilter implements Filter<MosaicDatabase2.Cursor> {
        private final double minGsd;
        private final double maxGsd;
        
        ResolutionFilter(double minGsd, double maxGsd) {
            this.minGsd = minGsd;
            this.maxGsd = maxGsd;
        }

        @Override
        public boolean accept(Cursor arg) {
            if(!Double.isNaN(this.minGsd) && (arg.getMaxGSD() > this.minGsd))
                return false;
            if(!Double.isNaN(this.maxGsd) && (arg.getMinGSD() < this.maxGsd))
                return false;
            return true;
        }
    }
    
    private final static class SpatialFilter implements Filter<MosaicDatabase2.Cursor> {
        private Envelope filter;
        
        SpatialFilter(Geometry geometry) {
            this.filter = geometry.getEnvelope();
        }
        
        @Override
        public boolean accept(Cursor arg) {
            return Rectangle.intersects(this.filter.minX,
                                        this.filter.minY,
                                        this.filter.maxX,
                                        this.filter.minY,
                                        arg.getMinLon(),
                                        arg.getMinLat(),
                                        arg.getMaxLon(),
                                        arg.getMaxLat());
        }
    }
    
    private final static class TypeFilter implements Filter<MosaicDatabase2.Cursor> {
        private final Set<String> types;
        
        TypeFilter(Set<String> types) {
            this.types = types;
        }

        @Override
        public boolean accept(Cursor arg) {
            return this.types.contains(arg.getType());
        }
    }
    
    private final static class PathFilter implements Filter<MosaicDatabase2.Cursor> {
        private final String path;
        
        PathFilter(String path) {
            this.path = path;
        }

        @Override
        public boolean accept(Cursor arg) {
            return arg.getPath().equals(this.path);
        }
    }
    
    private final static class SridFilter implements Filter<MosaicDatabase2.Cursor> {
        private final int srid;
        
        SridFilter(int srid) {
            this.srid = srid;
        }

        @Override
        public boolean accept(Cursor arg) {
            return (arg.getSrid() == this.srid);
        }
    }
}
