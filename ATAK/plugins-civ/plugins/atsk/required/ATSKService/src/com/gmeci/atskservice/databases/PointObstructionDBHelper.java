
package com.gmeci.atskservice.databases;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.gmeci.atskservice.resolvers.DBURIConstants;
import com.gmeci.core.ATSKConstants;

public class PointObstructionDBHelper extends SQLiteOpenHelper {

    private static final String TAG = "PointObstructionDB";
    private static final String DATABASE_NAME = ATSKConstants.DB_LOCATION
            + "pointObstruction_db.db";

    // Versions 3 or newer should maintain data when upgraded
    private static final int UPGRADE_PERSIST_VERSION = 3;
    private static final int DATABASE_VERSION = 4;

    public PointObstructionDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(DBURIConstants.PO_DATABASE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (newVersion != oldVersion) {
            if (oldVersion >= UPGRADE_PERSIST_VERSION) {
                // Apply updates
                Log.d(TAG, "Upgrading database from version "
                        + oldVersion + " to " + newVersion
                        + " while persisting existing data.");
                if (oldVersion < 4)
                    // Added "collection_method" column for line points
                    db.execSQL(DBURIConstants.PO_DATABASE_UPDATE1);
            } else {
                // Start fresh
                Log.w(TAG, "Upgrading database from version "
                        + oldVersion + " to " + newVersion
                        + ", which will destroy all old data");
                db.execSQL("DROP TABLE IF EXISTS "
                        + DBURIConstants.TABLE_POINT_OBSTRUCTIONS);
                onCreate(db);
            }
        }
    }
}
