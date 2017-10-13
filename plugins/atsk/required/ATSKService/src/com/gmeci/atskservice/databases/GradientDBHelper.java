
package com.gmeci.atskservice.databases;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.gmeci.atskservice.resolvers.DBURIConstants;

public class GradientDBHelper extends SQLiteOpenHelper {

    public static final String TAG = "GradientDBHelper";
    private static final String DATABASE_NAME = com.gmeci.core.ATSKConstants.DB_LOCATION
            + "Gradient_db.db";
    private static final int DATABASE_VERSION = 15;

    public GradientDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);

    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(DBURIConstants.GRADIENT_DATABASE_CREATE);
        database.execSQL(DBURIConstants.GRADIENT_POINTS_DATABASE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(GradientDBHelper.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        if (oldVersion != newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + DBURIConstants.TABLE_GRADIENT);
            db.execSQL("DROP TABLE IF EXISTS "
                    + DBURIConstants.TABLE_GRADIENT_POINTS);
            onCreate(db);
        }
    }

}
