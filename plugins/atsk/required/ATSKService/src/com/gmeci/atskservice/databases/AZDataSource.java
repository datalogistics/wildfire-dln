
package com.gmeci.atskservice.databases;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class AZDataSource {
    SQLiteDatabase database;
    private Context context;
    private AZDBHelper dbAZHelper;
    private boolean Opened = false;

    public AZDataSource(Context context) {
        this.context = context;
        dbAZHelper = new AZDBHelper(context);
    }

    public boolean isOpened() {
        return Opened;
    }

    public void open() throws SQLException {
        Opened = true;
        dbAZHelper = new AZDBHelper(context);
        database = dbAZHelper.getWritableDatabase();
    }

    public void close() {
        Opened = false;
        dbAZHelper.close();
    }

}
