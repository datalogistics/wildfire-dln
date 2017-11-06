
package com.gmeci.atskservice.databases;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.gmeci.atskservice.resolvers.AZURIConstants;

public class AZDBHelper extends SQLiteOpenHelper {

    public static final String TAG = "AZDBHelper";
    private static final String DATABASE_NAME = com.gmeci.core.ATSKConstants.DB_LOCATION
            + "assaultZone_db.db";
    private static final int DATABASE_VERSION = 15;

    public AZDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(AZURIConstants.AZ_DATABASE_CREATE);
        database.execSQL(AZURIConstants.AZ_SETTINGS_DATABASE_CREATE);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(AZDBHelper.class.getName(), "Upgrading database from version "
                + oldVersion + " to " + newVersion
                + ", which will destroy all old data");
        if (oldVersion != newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + AZURIConstants.TABLE_AZ);
            db.execSQL("DROP TABLE IF EXISTS "
                    + AZURIConstants.TABLE_AZ_SETTINGS);

            onCreate(db);
        }

    }

}
