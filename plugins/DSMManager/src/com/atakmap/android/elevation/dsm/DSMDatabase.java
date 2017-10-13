package com.atakmap.android.elevation.dsm;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import com.atakmap.content.BindArgument;
import com.atakmap.content.CatalogCurrency;
import com.atakmap.content.CatalogCurrencyRegistry;
import com.atakmap.content.CatalogDatabase;
import com.atakmap.content.WhereClauseBuilder;
import com.atakmap.coremap.conversions.Span;
import com.atakmap.coremap.maps.coords.AltitudeReference;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.database.CursorIface;
import com.atakmap.database.CursorWrapper;
import com.atakmap.database.QueryIface;
import com.atakmap.database.StatementIface;
import com.atakmap.database.jsqlite.JsqliteDatabaseAdapter;
import com.atakmap.map.layer.feature.geometry.Envelope;
import com.atakmap.map.layer.raster.mosaic.MosaicDatabase2;
import com.atakmap.map.layer.raster.mosaic.MultiplexingMosaicDatabaseCursor2;
import com.atakmap.math.MathUtils;

public class DSMDatabase extends CatalogDatabase {

    private final static CatalogCurrencyRegistry currencyRegistry = new CatalogCurrencyRegistry();
    static {
        currencyRegistry.register(DefaultCurrency.INSTANCE);
    }

    public DSMMosaicDatabase dsmmdb = new DSMMosaicDatabase();
    
    private final static String TABLE_ELEVATION_INFO = "elevation_info";
    private final static String COLUMN_PATH = "path";
    private final static String COLUMN_TYPE = "type";
    private final static String COLUMN_UL_LAT = "ullat";
    private final static String COLUMN_UL_LNG = "ullng";
    private final static String COLUMN_UR_LAT = "urlat";
    private final static String COLUMN_UR_LNG = "urlng";
    private final static String COLUMN_LR_LAT = "lrlat";
    private final static String COLUMN_LR_LNG = "lrlng";
    private final static String COLUMN_LL_LAT = "lllat";
    private final static String COLUMN_LL_LNG = "lllng";
    private final static String COLUMN_MIN_GSD = "mingsd";
    private final static String COLUMN_MAX_GSD = "maxgsd";
    private final static String COLUMN_WIDTH = "width";
    private final static String COLUMN_HEIGHT = "height";
    private final static String COLUMN_SRID = "srid";
    private final static String COLUMN_MODEL = "model";
    private final static String COLUMN_REFERENCE = "reference";
    private final static String COLUMN_UNITS = "units";
    private final static String COLUMN_CACHE = "cache";

    // for spatial filtering
    private final static String COLUMN_MIN_LAT = "minlat";
    private final static String COLUMN_MIN_LNG = "minlng";
    private final static String COLUMN_MAX_LAT = "maxlat";
    private final static String COLUMN_MAX_LNG = "maxlng";

    private final static int DATABASE_VERSION = 1;

    public DSMDatabase(File db) {
        super(JsqliteDatabaseAdapter.openOrCreateDatabase(db.getAbsolutePath()),
              currencyRegistry);
    }
    
    public ElevationInfo getElevationInfo(String path) {
        QueryIface result = null;
        try {
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT ")
                   .append(COLUMN_PATH).append(", ")
                   .append(COLUMN_TYPE).append(", ")
                   .append(COLUMN_UL_LAT).append(", ")
                   .append(COLUMN_UL_LNG).append(", ")
                   .append(COLUMN_UR_LAT).append(", ")
                   .append(COLUMN_UR_LNG).append(", ")
                   .append(COLUMN_LR_LAT).append(", ")
                   .append(COLUMN_LR_LNG).append(", ")
                   .append(COLUMN_LL_LAT).append(", ")
                   .append(COLUMN_LL_LNG).append(", ")
                   .append(COLUMN_MIN_GSD).append(", ")
                   .append(COLUMN_MAX_GSD).append(", ")
                   .append(COLUMN_WIDTH).append(", ")
                   .append(COLUMN_HEIGHT).append(", ")
                   .append(COLUMN_SRID).append(", ")
                   .append(COLUMN_MODEL).append(", ")
                   .append(COLUMN_REFERENCE).append(", ")
                   .append(COLUMN_UNITS).append(", ")
                   .append(COLUMN_CACHE)
               .append(" FROM ")
               .append(TABLE_ELEVATION_INFO)
               .append(" WHERE ")
                   .append(COLUMN_PATH).append(" = ?")
               .append(" LIMIT 1");
            
            result = this.database.compileQuery(sql.toString());
            result.bind(1, path);
            
            if(!result.moveToNext())
                return null;
            
            return new ElevationInfo(result.getString(0),
                                     result.getString(1),
                                     new GeoPoint(result.getDouble(2), result.getDouble(3)),
                                     new GeoPoint(result.getDouble(4), result.getDouble(5)),
                                     new GeoPoint(result.getDouble(6), result.getDouble(7)),
                                     new GeoPoint(result.getDouble(8), result.getDouble(9)),
                                     result.getDouble(10),
                                     result.getDouble(11),
                                     result.getInt(12),
                                     result.getInt(13),
                                     result.getInt(14),
                                     result.getInt(15),
                                     AltitudeReference.findFromValue(result.getInt(16)),
                                     Span.findFromValue(result.getInt(17)),
                                     result.getString(18));
        } finally {
            if(result != null)
                result.close();
        }
    }
    
    public void update(String path, int model, AltitudeReference reference, Span units) {
        StatementIface stmt = null;
        try {
            StringBuilder sql = new StringBuilder();
            sql.append("UPDATE ").append(TABLE_ELEVATION_INFO)
               .append(" SET ")
                   .append(COLUMN_MODEL).append(" = ?, ")
                   .append(COLUMN_REFERENCE).append(" = ?, ")
                   .append(COLUMN_UNITS).append(" = ? ")
               .append(" WHERE ")
                   .append(COLUMN_PATH).append(" = ?");
            
            stmt = this.database.compileStatement(sql.toString());
            stmt.bind(1, model);
            stmt.bind(2, reference.getValue());
            stmt.bind(3, units.getValue());
            stmt.bind(4, path);

            stmt.execute();
        } finally {
            if(stmt != null)
                stmt.close();
        }
    }
    
    public void insert(ElevationInfo info) {
        StatementIface stmt = null;
        try {
            StringBuilder sql = new StringBuilder();
            sql.append("INSERT INTO ").append(TABLE_ELEVATION_INFO)
               .append(" (")
                   .append(COLUMN_PATH).append(", ")
                   .append(COLUMN_TYPE).append(", ")
                   .append(COLUMN_UL_LAT).append(", ")
                   .append(COLUMN_UL_LNG).append(", ")
                   .append(COLUMN_UR_LAT).append(", ")
                   .append(COLUMN_UR_LNG).append(", ")
                   .append(COLUMN_LR_LAT).append(", ")
                   .append(COLUMN_LR_LNG).append(", ")
                   .append(COLUMN_LL_LAT).append(", ")
                   .append(COLUMN_LL_LNG).append(", ")
                   .append(COLUMN_MIN_GSD).append(", ")
                   .append(COLUMN_MAX_GSD).append(", ")
                   .append(COLUMN_WIDTH).append(", ")
                   .append(COLUMN_HEIGHT).append(", ")
                   .append(COLUMN_SRID).append(", ")
                   .append(COLUMN_MODEL).append(", ")
                   .append(COLUMN_REFERENCE).append(", ")
                   .append(COLUMN_UNITS).append(", ")
                   .append(COLUMN_CACHE).append(", ")
                   .append(COLUMN_MIN_LAT).append(", ")
                   .append(COLUMN_MIN_LNG).append(", ")
                   .append(COLUMN_MAX_LAT).append(", ")
                   .append(COLUMN_MAX_LNG)
               .append(")")
               .append(" VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            
            stmt = this.database.compileStatement(sql.toString());
            int idx = 1;
            stmt.bind(idx++, info.path);
            stmt.bind(idx++, info.type);
            stmt.bind(idx++, info.upperLeft.getLatitude());
            stmt.bind(idx++, info.upperLeft.getLongitude());
            stmt.bind(idx++, info.upperRight.getLatitude());
            stmt.bind(idx++, info.upperRight.getLongitude());
            stmt.bind(idx++, info.lowerRight.getLatitude());
            stmt.bind(idx++, info.lowerRight.getLongitude());
            stmt.bind(idx++, info.lowerLeft.getLatitude());
            stmt.bind(idx++, info.lowerLeft.getLongitude());
            stmt.bind(idx++, info.minGsd);
            stmt.bind(idx++, info.maxGsd);
            stmt.bind(idx++, info.width);
            stmt.bind(idx++, info.height);
            stmt.bind(idx++, info.srid);
            stmt.bind(idx++, info.model);
            stmt.bind(idx++, info.reference.getValue());
            stmt.bind(idx++, info.units.getValue());
            stmt.bind(idx++, info.cache);
            stmt.bind(idx++, MathUtils.min(info.upperLeft.getLatitude(),
                                           info.upperRight.getLatitude(),
                                           info.lowerRight.getLatitude(),
                                           info.lowerLeft.getLatitude()));
            stmt.bind(idx++, MathUtils.min(info.upperLeft.getLongitude(),
                                           info.upperRight.getLongitude(),
                                           info.lowerRight.getLongitude(),
                                           info.lowerLeft.getLongitude()));
            stmt.bind(idx++, MathUtils.max(info.upperLeft.getLatitude(),
                                           info.upperRight.getLatitude(),
                                           info.lowerRight.getLatitude(),
                                           info.lowerLeft.getLatitude()));
            stmt.bind(idx++, MathUtils.max(info.upperLeft.getLongitude(),
                                           info.upperRight.getLongitude(),
                                           info.lowerRight.getLongitude(),
                                           info.lowerLeft.getLongitude()));

            stmt.execute();
        } finally {
            if(stmt != null)
                stmt.close();
        }
    }

    public boolean contains(String path) {
        QueryIface result = null;
        try {
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT 1 FROM ")
               .append(TABLE_ELEVATION_INFO)
               .append(" WHERE ")
                   .append(COLUMN_PATH).append(" = ?")
               .append(" LIMIT 1");
            result = this.database.compileQuery(sql.toString());
            result.bind(1, path);

            return result.moveToNext();
        } finally {
            if(result != null)
                result.close();
        }
    }

    /**************************************************************************/
    // CatalogDatabase
    
    @Override
    protected void dropTables() {
        super.dropTables();
        
        // XXX - delete cache files

        this.database.execute("DROP TABLE IF EXISTS " + TABLE_ELEVATION_INFO, null);
    }

    @Override
    protected void buildTables() {
        super.buildTables();
        
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE ")
           .append(TABLE_ELEVATION_INFO)
           .append(" (")
               .append(COLUMN_PATH).append(" TEXT, ")
               .append(COLUMN_TYPE).append(" TEXT, ")
               .append(COLUMN_UL_LAT).append(" REAL, ")
               .append(COLUMN_UL_LNG).append(" REAL, ")
               .append(COLUMN_UR_LAT).append(" REAL, ")
               .append(COLUMN_UR_LNG).append(" REAL, ")
               .append(COLUMN_LR_LAT).append(" REAL, ")
               .append(COLUMN_LR_LNG).append(" REAL, ")
               .append(COLUMN_LL_LAT).append(" REAL, ")
               .append(COLUMN_LL_LNG).append(" REAL, ")
               .append(COLUMN_MIN_GSD).append(" REAL, ")
               .append(COLUMN_MAX_GSD).append(" REAL, ")
               .append(COLUMN_WIDTH).append(" INTEGER, ")
               .append(COLUMN_HEIGHT).append(" INTEGER, ")
               .append(COLUMN_SRID).append(" INTEGER, ")
               .append(COLUMN_MODEL).append(" INTEGER, ")
               .append(COLUMN_REFERENCE).append(" INTEGER, ")
               .append(COLUMN_UNITS).append(" INTEGER, ")
               .append(COLUMN_CACHE).append(" INTEGER, ")
               .append(COLUMN_MIN_LAT).append(" REAL, ")
               .append(COLUMN_MIN_LNG).append(" REAL, ")
               .append(COLUMN_MAX_LAT).append(" REAL, ")
               .append(COLUMN_MAX_LNG).append(" REAL")
           .append(")");
        
        this.database.execute(sql.toString(), null);
    }
    
    @Override
    protected boolean checkDatabaseVersion() {
        return (this.database.getVersion() == databaseVersion());
    }

    @Override
    protected void setDatabaseVersion() {
        this.database.setVersion(databaseVersion());
    }

    @Override
    public void validateCatalog() {
        super.validateCatalog();
        
        // XXX - won't be necessary once base class machinery is being used
        
        Set<String> toDelete = new HashSet<String>();
        
        CursorIface result = null;
        try {
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT ")
                   .append(COLUMN_PATH)
               .append(" FROM ")
               .append(TABLE_ELEVATION_INFO);
            
            result = this.database.query(sql.toString(), null);
            while(result.moveToNext()) {
                final String path = result.getString(0);
                if(!(new File(path)).exists())
                    toDelete.add(path);
            }
        } finally {
            if(result != null)
                result.close();
        }
        
        for(String path : toDelete) {
            StatementIface stmt = null;
            try {
                StringBuilder sql = new StringBuilder();
                sql.append("DELETE FROM ")
                   .append(TABLE_ELEVATION_INFO)
                   .append(" WHERE ")
                       .append(COLUMN_PATH).append(" = ?");
                stmt = this.database.compileStatement(sql.toString());
                stmt.bind(1, path);
                stmt.execute();
            } finally {
                if(stmt != null)
                    stmt.close();
            }
        }
    }

    /**************************************************************************/
    // MosaicDatabase2
    

    public class DSMMosaicDatabase implements MosaicDatabase2 { 
    @Override
    public String getType() {
        return "dsm";
    }

    @Override
    public void open(File f) {
        // XXX - 
    }

    @Override
    public void close() {
       DSMDatabase.this.close();
    }

    @Override
    public Coverage getCoverage() {
        // XXX - 
        return null;
    }

    @Override
    public void getCoverages(Map<String, Coverage> coverages) {
        // XXX - 
    }

    @Override
    public Coverage getCoverage(String type) {
        // XXX - 
        return null;
    }

    @Override
    public Cursor query(QueryParameters params) {
        CursorIface result = null;
        try {
            StringBuilder sql = new StringBuilder();
            LinkedList<BindArgument> args = new LinkedList<BindArgument>();
            
            sql.append("SELECT ")
                   .append("ROWID, ") // 0
                   .append(COLUMN_PATH).append(", ") // 1
                   .append(COLUMN_TYPE).append(", ") // 2
                   .append(COLUMN_UL_LAT).append(", ") // 3
                   .append(COLUMN_UL_LNG).append(", ") // 4
                   .append(COLUMN_UR_LAT).append(", ") // 5
                   .append(COLUMN_UR_LNG).append(", ") // 6
                   .append(COLUMN_LR_LAT).append(", ") // 7
                   .append(COLUMN_LR_LNG).append(", ") // 8
                   .append(COLUMN_LL_LAT).append(", ") // 9
                   .append(COLUMN_LL_LNG).append(", ") // 10
                   .append(COLUMN_MIN_GSD).append(", ") // 11
                   .append(COLUMN_MAX_GSD).append(", ") // 12
                   .append(COLUMN_WIDTH).append(", ") // 13
                   .append(COLUMN_HEIGHT).append(", ") // 14
                   .append(COLUMN_SRID).append(", ") // 15
                   .append(COLUMN_MIN_LAT).append(", ") // 16
                   .append(COLUMN_MIN_LNG).append(", ") // 17
                   .append(COLUMN_MAX_LAT).append(", ") // 18
                   .append(COLUMN_MAX_LNG) // 19
               .append(" FROM ")
               .append(TABLE_ELEVATION_INFO);

            if(params != null) {
                if(params.precisionImagery != null && params.precisionImagery.booleanValue())
                    return new MultiplexingMosaicDatabaseCursor2(Collections.<Cursor>emptySet());

                WhereClauseBuilder where = new WhereClauseBuilder();                
                if(!Double.isNaN(params.minGsd)) {
                    where.beginCondition();
                    where.append(COLUMN_MAX_GSD);
                    where.append(" <= ?");
                    where.addArg(params.minGsd);
                }
                if(!Double.isNaN(params.maxGsd)) {
                    where.beginCondition();
                    where.append(COLUMN_MIN_GSD);
                    where.append(" >= ?");
                    where.addArg(params.maxGsd);
                }
                if(params.types != null) {
                    where.beginCondition();
                    where.appendIn(COLUMN_TYPE, params.types);
                }
                if(params.path != null) {
                    where.beginCondition();
                    where.append(COLUMN_PATH);
                    where.append(" = ?");
                    where.addArg(params.path);
                }
                if(params.spatialFilter != null) {
                    final Envelope spatialFilter = params.spatialFilter.getEnvelope();
                    where.beginCondition();
                    where.append("(");
                        where.append(COLUMN_MIN_LAT);
                        where.append(" <= ?");
                        where.addArg(spatialFilter.maxY);
                    where.append(" AND ");
                        where.append(COLUMN_MAX_LAT);
                        where.append(" >= ?");
                        where.addArg(spatialFilter.minY);
                    where.append(" AND ");
                        where.append(COLUMN_MIN_LNG);
                        where.append(" <= ?");
                        where.addArg(spatialFilter.maxX);
                    where.append(" AND ");
                        where.append(COLUMN_MAX_LNG);
                        where.append(" >= ?");
                        where.addArg(spatialFilter.minX);
                    where.append(")");
                }
                if(params.srid != -1) {
                    where.beginCondition();
                    where.append(COLUMN_SRID);
                    where.append(" = ?");
                    where.addArg(params.srid);
                }
                
                final String selection = where.getSelection();
                if(selection != null) {
                    sql.append(" WHERE ").append(selection);
                    args.addAll(where.getBindArgs());
                }
            }
            
            result = BindArgument.query(DSMDatabase.this.database, sql.toString(), args);

            final CursorImpl retval = new CursorImpl(result,
                                                     0, // rowid
                                                     1, // path
                                                     2, // type
                                                     3, 4, // ul
                                                     5, 6, // ur
                                                     7, 8, // lr
                                                     9, 10, // ll
                                                     16, // min lat
                                                     17, // min lng
                                                     18, // max lat
                                                     19, // max lng
                                                     13, // width
                                                     14, // height
                                                     15, // srid
                                                     11, // min gsd
                                                     12); // max gsd
            result = null;
            return retval;
        } finally {
            if(result != null)
                result.close();
        }
    }
    }
    
    /**************************************************************************/
    
    private static int databaseVersion() {
        return (CATALOG_VERSION | (DATABASE_VERSION << 16));
    }
    
    /**************************************************************************/

    private static class CursorImpl extends CursorWrapper implements MosaicDatabase2.Cursor {

        private final int pathIdx;
        private final int typeIdx;
        private final int ulLatIdx;
        private final int ulLngIdx;
        private final int urLatIdx;
        private final int urLngIdx;
        private final int lrLatIdx;
        private final int lrLngIdx;
        private final int llLatIdx;
        private final int llLngIdx;
        private final int minLatIdx;
        private final int minLngIdx;
        private final int maxLatIdx;
        private final int maxLngIdx;
        private final int widthIdx;
        private final int heightIdx;
        private final int sridIdx;
        private final int rowIdIdx;
        private final int minGsdIdx;
        private final int maxGsdIdx;
        
        CursorImpl(CursorIface impl,
                   int rowIdIdx,
                   int pathIdx,
                   int typeIdx,
                   int ulLatIdx,
                   int ulLngIdx,
                   int urLatIdx,
                   int urLngIdx,
                   int lrLatIdx,
                   int lrLngIdx,
                   int llLatIdx,
                   int llLngIdx,
                   int minLatIdx,
                   int minLngIdx,
                   int maxLatIdx,
                   int maxLngIdx,
                   int widthIdx,
                   int heightIdx,
                   int sridIdx,
                   int minGsdIdx,
                   int maxGsdIdx) {
            super(impl);

            this.rowIdIdx = rowIdIdx;
            this.pathIdx = pathIdx;
            this.typeIdx = typeIdx;
            this.ulLatIdx = ulLatIdx;
            this.ulLngIdx = ulLngIdx;
            this.urLatIdx = urLatIdx;
            this.urLngIdx = urLngIdx;
            this.lrLatIdx = lrLatIdx;
            this.lrLngIdx = lrLngIdx;
            this.llLatIdx = llLatIdx;
            this.llLngIdx = llLngIdx;
            this.minLatIdx = minLatIdx;
            this.minLngIdx = minLngIdx;
            this.maxLatIdx = maxLatIdx;
            this.maxLngIdx = maxLngIdx;
            this.widthIdx = widthIdx;
            this.heightIdx = heightIdx;
            this.sridIdx = sridIdx;
            this.minGsdIdx = minGsdIdx;
            this.maxGsdIdx = maxGsdIdx;
        }
                

        @Override
        public GeoPoint getUpperLeft() {
            return new GeoPoint(this.getDouble(this.ulLatIdx),
                                this.getDouble(this.ulLngIdx));
        }

        @Override
        public GeoPoint getUpperRight() {
            return new GeoPoint(this.getDouble(this.urLatIdx),
                                this.getDouble(this.urLngIdx));
        }

        @Override
        public GeoPoint getLowerRight() {
            return new GeoPoint(this.getDouble(this.lrLatIdx),
                                this.getDouble(this.lrLngIdx));
        }

        @Override
        public GeoPoint getLowerLeft() {
            return new GeoPoint(this.getDouble(this.llLatIdx),
                                this.getDouble(this.llLngIdx));
        }

        @Override
        public double getMinLat() {
            return this.getDouble(this.minLatIdx);
        }

        @Override
        public double getMinLon() {
            return this.getDouble(this.minLngIdx);
        }

        @Override
        public double getMaxLat() {
            return this.getDouble(this.maxLatIdx);
        }

        @Override
        public double getMaxLon() {
            return this.getDouble(this.maxLngIdx);
        }

        @Override
        public String getPath() {
            return this.getString(this.pathIdx);
        }

        @Override
        public String getType() {
            return this.getString(this.typeIdx);
        }

        @Override
        public double getMinGSD() {
            return this.getDouble(this.minGsdIdx);
        }

        @Override
        public double getMaxGSD() {
            return this.getDouble(this.maxGsdIdx);
        }

        @Override
        public int getWidth() {
            return this.getInt(this.widthIdx);
        }

        @Override
        public int getHeight() {
            return this.getInt(this.heightIdx);
        }

        @Override
        public int getId() {
            return this.getInt(this.rowIdIdx);
        }

        @Override
        public int getSrid() {
            return this.getInt(this.sridIdx);
        }

        @Override
        public boolean isPrecisionImagery() {
            return false;
        }

        @Override
        public MosaicDatabase2.Frame asFrame() {
            return new MosaicDatabase2.Frame(this);
        }
    }

    /**************************************************************************/
    
    private static class DefaultCurrency implements CatalogCurrency {

        final static CatalogCurrency INSTANCE = new DefaultCurrency();
        
        private DefaultCurrency() {}
        
        @Override
        public String getName() {
            return "default";
        }

        @Override
        public int getAppVersion() {
            return 1;
        }

        @Override
        public byte[] getAppData(File file) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public boolean isValidApp(File f, int appVersion, byte[] appData) {
            return false;
        }
    }
}
