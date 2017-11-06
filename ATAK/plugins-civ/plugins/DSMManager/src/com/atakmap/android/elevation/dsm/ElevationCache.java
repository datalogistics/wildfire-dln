package com.atakmap.android.elevation.dsm;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdalconst.gdalconst;

import com.atakmap.database.DatabaseIface;
import com.atakmap.database.QueryIface;
import com.atakmap.database.StatementIface;
import com.atakmap.database.jsqlite.JsqliteDatabaseAdapter;
import com.atakmap.map.layer.raster.gdal.GdalTileReader;
import com.atakmap.map.layer.raster.tilereader.TileReader;
import com.atakmap.math.MathUtils;

public class ElevationCache {
    private DatabaseIface database;
    private int tileWidth;
    private int tileHeight;

    public ElevationCache(String file) {
        database = JsqliteDatabaseAdapter.openOrCreateDatabase(file);
        database.execute("CREATE TABLE IF NOT EXISTS dataset (path TEXT, tilewidth TEXT, tileheight TEXT)", null);
        database.execute("CREATE TABLE IF NOT EXISTS tiles (level INTEGER, tilecol INTEGER, tilerow INTEGER, minval REAL, maxval REAL, nodata INTEGER, width INTEGER, height INTEGER, data BLOB)", null);
        this.tileWidth = 512;
        this.tileHeight = 512;

    }
    
    private void validate(Dataset dataset) {
        // XXX - make sure correct dataset
        // XXX - if no entry, fill out image layout info/nodata value
    }

    /**
     * 
     * @param dataset
     * @param level
     * @param x         full image X
     * @param y         full image Y
     * @return
     */
    public double getValue(Dataset dataset, int level, double x, double y) {
        this.validate(dataset);

        final int tileColumn = ((int)MathUtils.clamp(Math.round(x), 0, dataset.GetRasterXSize()-1) / this.tileWidth) >> level; 
        final int tileRow = ((int)MathUtils.clamp(Math.round(y), 0, dataset.GetRasterYSize()-1) / this.tileHeight) >> level;

        final Tile tile = this.getTile(dataset, level, tileColumn, tileRow);
        if(tile == null)
            return Double.NaN;
        else if(tile.noData)
            return Double.NaN;

        if(Math.floor(x) == x && Math.floor(y) == y) {
            return getValue(tile, (int)x, (int)y);
        } else {
            // interpolate
            return getValueInterpolate(tile, x, y);
        }
    }
    
    /**
     * 
     * @param tile
     * @param x     full image X
     * @param y     full image Y
     * @return
     */
    private double getValue(Tile tile, int x, int y) {
        final int tx = ((x - (tile.tileColumn<<tile.level)*this.tileWidth)/ (1<<tile.level));
        final int ty = ((y - (tile.tileRow<<tile.level)*this.tileHeight)/ (1<<tile.level));
        
        int idx = (ty*tile.width)+tx;
        if(idx < 0)
            throw new IllegalArgumentException();
        final int val = (tile.data[(ty*tile.width)+tx]&0xFF);
        if(val == 0xFF)
            return Double.NaN;
        return (((double)val / 254d)*(tile.maxValue-tile.minValue))+tile.minValue;
    }
    
    /**
     * 
     * @param tile
     * @param x     full image X
     * @param y     full image Y
     * @return
     */
    private double getValueInterpolate(Tile tile, double x, double y) {
        final double samples[] = new double[]
                {
                    getValue(tile, (int)x, (int)y),
                    getValue(tile, (int)Math.ceil(x), (int)y),
                    getValue(tile, (int)x, (int)Math.ceil(y)),
                    getValue(tile, (int)Math.ceil(x), (int)Math.ceil(y)),
                };
            
        double v = 0.0d;
        int numSamples = 0;
        for(int i = 0; i < samples.length; i++) {
            if(!Double.isNaN(samples[i])) {
                v += samples[i];
                numSamples++;
            }
        }
        
        if(numSamples == 0)
            return Double.NaN;
        return v / numSamples;
    }
    
    private Tile getTile(Dataset dataset, int level, int tileColumn, int tileRow) {
        // query for the tile
        QueryIface result = null;
        try {
            result = this.database.compileQuery("SELECT minval, maxval, nodata, width, height, tilerow, tilecol, level, data FROM tiles WHERE level = ? AND tilecol = ? AND tilerow = ? LIMIT 1");
            result.bind(1, level);
            result.bind(2, tileColumn);
            result.bind(3, tileRow);
            
            if(result.moveToNext()) {
                final Tile retval = new Tile();
                retval.minValue = result.getDouble(0);
                retval.maxValue = result.getDouble(1);
                retval.noData = (result.getInt(2)!=0);
                retval.width = result.getInt(3);
                retval.height = result.getInt(4);
                retval.tileRow = result.getInt(5);
                retval.tileColumn = result.getInt(6);
                retval.level = result.getInt(7);
                retval.data = result.getBlob(8);

                return retval;
            }
        } finally {
            if(result != null)
                result.close();
        }

        Tile retval;

        // XXX - if the tile is not available, look for a lower resolution copy
        if(false && level > 2) {
            Tile ul = this.getTile(dataset, level-1, tileColumn*2, tileRow*2);
            Tile ur = this.getTile(dataset, level-1, tileColumn*2+1, tileRow*2);
            Tile ll = this.getTile(dataset, level-1, tileColumn*2, tileRow*2+1);
            Tile lr = this.getTile(dataset, level-1, tileColumn*2+1, tileRow*2+1);
            
            if(ul.noData && ur.noData && ll.noData && lr.noData) {
                retval = new Tile();
                retval.minValue = Double.NaN;
                retval.maxValue = Double.NaN;
                retval.noData = true;
                retval.level = level;
                retval.tileColumn = tileColumn;
                retval.tileRow = tileRow;
                retval.width = this.tileWidth;
                retval.height = this.tileHeight;
                retval.data = null;
            } else {
                retval = new Tile();
                retval.minValue = MathUtils.min(ul.minValue, ur.minValue, ll.minValue, lr.minValue);
                retval.maxValue = MathUtils.max(ul.maxValue, ur.maxValue, ll.maxValue, lr.maxValue);
                retval.noData = false;
                retval.level = level;
                retval.tileColumn = tileColumn;
                retval.tileRow = tileRow;
                retval.width = this.tileWidth;
                retval.height = this.tileHeight;
                retval.data = new byte[retval.width*retval.height];

                Tile[] tiles = new Tile[] {ul, ur, ll, lr};
                for(int i = 0; i < 4; i++) {
                    if(tiles[i].noData) {
                        // fill quad nodata
                        for(int y = 0; y < this.tileHeight/2; y++) {
                            final int fully = (y+((this.tileHeight/2)*(i/2)));
                            final int fullx = ((this.tileWidth/2)*(i%2));
                            final int idx = (fully*retval.width)+fullx;
                            
                            Arrays.fill(retval.data, idx, this.tileWidth/2, (byte)0xFF);
                        }
                    } else {
                        for(int y = 0; y < this.tileHeight/2; y++) {
                            for(int x = 0; x < this.tileWidth/2; x++) {
                                final int fully = (y+((this.tileHeight/2)*(i/2)));
                                final int fullx = (x+((this.tileWidth/2)*(i%2)));
                                final int idx = (fully*retval.width)+fullx;
                                // XXX - expects x/y as full image coords
                                final double value = getValueInterpolate(tiles[i], (x*2)+0.5, (y*2)+0.5);
                                if(Double.isNaN(value)) {
                                    retval.data[idx] = (byte)0xFF;
                                } else {
                                    retval.data[idx] = (byte)MathUtils.clamp((((value-retval.minValue)/(retval.maxValue-retval.minValue))*254d), 0d, 254d);    
                                }                                
                            }
                        }                        
                    }
                }
            }
        } else {
            // generate the tile from the source dataset
            
            Sampler sampler = Sampler.create(dataset);
            Double[] ndv = new Double[1];
            dataset.GetRasterBand(1).GetNoDataValue(ndv);
            final double noDataValue = (ndv[0] != null) ? ndv[0].doubleValue() : Double.NaN;
            
            GdalTileReader reader = new GdalTileReader(dataset, dataset.GetDescription(), this.tileWidth, this.tileHeight, null, TileReader.getMasterIOThread());
            
            final int dstW = reader.getTileWidth(level, tileColumn);
            final int dstH = reader.getTileHeight(level, tileRow);
            
            double[] samples = new double[dstW*dstH];
            SampleStatistics stats = new SampleStatistics();
            sampler.readSamples(samples,
                                dataset,
                                noDataValue,
                                (int)reader.getTileSourceX(level, tileColumn),
                                (int)reader.getTileSourceY(level, tileRow),
                                (int)reader.getTileSourceWidth(level, tileColumn),
                                (int)reader.getTileSourceHeight(level, tileRow),
                                dstW,
                                dstH,
                                stats);
            
            
            retval = new Tile();
            retval.minValue = stats.min;
            retval.maxValue = stats.max;
            retval.noData = stats.noData;
            retval.level = level;
            retval.tileColumn = tileColumn;
            retval.tileRow = tileRow;
            retval.width = this.tileWidth;
            retval.height = this.tileHeight;
            
            if(!retval.noData) {
                retval.data = new byte[retval.width*retval.height];
                
                int dstIdx = 0;
                double sample;
                for(int y = 0; y < dstH; y++) {
                    dstIdx = (y*retval.width);
                    for(int x = 0; x < dstW; x++) {
                        sample = samples[(y*dstW)+x];
                        if(Double.isNaN(sample))
                            retval.data[dstIdx++] = (byte)0xFF;
                        else
                            retval.data[dstIdx++] = (byte)(((sample-stats.min)/(stats.max-stats.min))*254d);
                    }
                }
            }
        }
        
        StatementIface stmt = null;
        try {
            stmt = this.database.compileStatement("INSERT INTO tiles (level, tilecol, tilerow, minval, maxval, nodata, width, height, data) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)");
            stmt.bind(1, retval.level);
            stmt.bind(2, retval.tileColumn);
            stmt.bind(3, retval.tileRow);
            stmt.bind(4, retval.minValue);
            stmt.bind(5, retval.maxValue);
            stmt.bind(6, retval.noData ? 1 : 0);
            stmt.bind(7, retval.width);
            stmt.bind(8, retval.height);
            stmt.bind(9, retval.data);
            
            stmt.execute();
        } finally {
            if(stmt != null)
                stmt.close();
        }
        
        return retval;
    }
    
    private static class Tile {
        public double minValue;
        public double maxValue;
        public boolean noData;
        public int width;
        public int height;
        public int tileRow;
        public int tileColumn;
        public int level;
        public byte[] data;
        
        
    }
    
    private static abstract class Sampler {
        private final int sampleSize;
        private final int sampleType;

        protected Sampler(int sampleType, int sampleSize) {
            this.sampleType = sampleType;
            this.sampleSize = sampleSize;
        }

        public void readSamples(double[] buffer, Dataset dataset, double noDataValue, int srcX, int srcY, int srcW, int srcH, int dstW, int dstH, SampleStatistics stats) {
            final int numSamples = (dstW*dstH);
            ByteBuffer raw = ByteBuffer.allocateDirect(numSamples*getSampleSize());
            raw.order(ByteOrder.nativeOrder());

            dataset.ReadRaster_Direct(srcX, srcY, srcW, srcH, dstW, dstH, this.getSampleType(), raw, new int[] {1}, 0);
            raw.clear();

            int idx = 0;
            stats.noData = true;
            
            // start looping to set initial min/max
            for(; idx < numSamples; idx++) {
                final double sample = nextSample(raw);
                if(sample == noDataValue) {
                    buffer[idx++] = Double.NaN;
                } else {
                    buffer[idx++] = sample;
                    stats.noData = false;
                    stats.min = sample;
                    stats.max = sample;
                    break;
                }
            }
            
            // complete loop
            for(; idx < numSamples; idx++) {
                final double sample = nextSample(raw);
                if(sample == noDataValue) {
                    buffer[idx++] = Double.NaN;
                } else {
                    buffer[idx++] = sample;
                    if(stats.min > sample)
                        stats.min = sample;
                    if(stats.max < sample)
                        stats.max = sample;
                }
            }
        }

        public abstract double nextSample(ByteBuffer buffer);

        public final int getSampleSize() {
            return this.sampleSize;
        }
        public final int getSampleType() {
            return this.sampleType;
        }
        
        public static Sampler create(Dataset dataset) {
            final Band band = dataset.GetRasterBand(1);
            final int dataType = band.getDataType();
            if(dataType == gdalconst.GDT_Byte)
                return Uint8Sampler;
            else if(dataType == gdalconst.GDT_UInt16)
                return Uint16Sampler;
            else if(dataType == gdalconst.GDT_Int16)
                return Int16Sampler;
            else if(dataType == gdalconst.GDT_UInt32)
                return Uint32Sampler;
            else if(dataType == gdalconst.GDT_Int32)
                return Int32Sampler;
            else if(dataType == gdalconst.GDT_Float32)
                return Float32Sampler;
            else if(dataType == gdalconst.GDT_Float64)
                return Float64Sampler;
            else
                throw new IllegalArgumentException();
        }
    }
    
    private final static Sampler Uint8Sampler = new Sampler(gdalconst.GDT_Byte, 1) {
        @Override
        public double nextSample(ByteBuffer buffer) {
            return (buffer.get()&0xFF);
        }
    };
    private final static Sampler Uint16Sampler = new Sampler(gdalconst.GDT_UInt16, 2) {
        @Override
        public double nextSample(ByteBuffer buffer) {
            return (buffer.getShort()&0xFFFF);
        }
    };
    private final static Sampler Int16Sampler = new Sampler(gdalconst.GDT_Int16, 2) {
        @Override
        public double nextSample(ByteBuffer buffer) {
            return buffer.getShort();
        }
    };
    private final static Sampler Uint32Sampler = new Sampler(gdalconst.GDT_UInt32, 4) {
        @Override
        public double nextSample(ByteBuffer buffer) {
            return ((long)buffer.getInt()&0xFFFFFFFFL);
        }
    };
    private final static Sampler Int32Sampler = new Sampler(gdalconst.GDT_Int32, 4) {
        @Override
        public double nextSample(ByteBuffer buffer) {
            return buffer.getInt();
        }
    };
    private final static Sampler Float32Sampler = new Sampler(gdalconst.GDT_Float32, 4) {
        @Override
        public double nextSample(ByteBuffer buffer) {
            return buffer.getFloat();
        }
    };
    private final static Sampler Float64Sampler = new Sampler(gdalconst.GDT_Float64, 8) {
        @Override
        public double nextSample(ByteBuffer buffer) {
            return buffer.getDouble();
        }
    };
    
    private static class SampleStatistics {
        boolean noData;
        double min;
        double max;
    }
}
