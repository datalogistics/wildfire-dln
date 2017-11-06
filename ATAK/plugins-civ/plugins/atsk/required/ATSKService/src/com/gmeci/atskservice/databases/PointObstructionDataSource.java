
package com.gmeci.atskservice.databases;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.gmeci.core.PointObstruction;
import com.gmeci.atskservice.resolvers.DBURIConstants;
import com.gmeci.helpers.ObstructionHelper;

import java.util.ArrayList;
import java.util.List;

public class PointObstructionDataSource {
    public SQLiteDatabase database;
    Context context;
    private PointObstructionDBHelper dbHelper;
    private boolean Opened = false;

    public PointObstructionDataSource(Context context) {
        this.context = context;
        dbHelper = new PointObstructionDBHelper(context);
    }

    public boolean isOpened() {
        return Opened;
    }

    public void open() throws SQLException {
        Opened = true;
        dbHelper = new PointObstructionDBHelper(context);
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        Opened = false;
        dbHelper.close();
    }

    public boolean updateTarget(PointObstruction obstruction2Update) {
        ContentValues values = new ContentValues();
        values.put(DBURIConstants.COLUMN_GROUP_NAME_POINT,
                obstruction2Update.group);
        values.put(DBURIConstants.COLUMN_UID, obstruction2Update.uid);
        values.put(DBURIConstants.COLUMN_TYPE, obstruction2Update.type);
        values.put(DBURIConstants.COLUMN_LAT, obstruction2Update.lat);
        values.put(DBURIConstants.COLUMN_LON, obstruction2Update.lon);
        values.put(DBURIConstants.COLUMN_HAE_M, obstruction2Update.getHAE());
        values.put(DBURIConstants.COLUMN_LE, obstruction2Update.linearError);
        values.put(DBURIConstants.COLUMN_CE, obstruction2Update.circularError);
        values.put(DBURIConstants.COLUMN_HEIGHT, obstruction2Update.height);
        values.put(DBURIConstants.COLUMN_WIDTH, obstruction2Update.width);
        values.put(DBURIConstants.COLUMN_LENGTH, obstruction2Update.length);
        values.put(DBURIConstants.COLUMN_DESCRIPTION,
                obstruction2Update.remark);
        values.put(DBURIConstants.COLUMN_ROTATION,
                obstruction2Update.course_true);

        int RowsEffected = database.update(
                DBURIConstants.TABLE_POINT_OBSTRUCTIONS, values,
                DBURIConstants.COLUMN_UID + "=" + obstruction2Update.uid, null);

        if (RowsEffected == 0)
            return false;
        return true;
    }

    public PointObstruction createPointObstruction(String group, String uid,
            double Lat, double Lon, double hae, String type, String Remark,
            double ce_m, double le_m, double length_m, float width_m,
            double height_m, double rotation_deg_true) {
        ContentValues values = new ContentValues();
        values.put(DBURIConstants.COLUMN_GROUP_NAME_POINT, group);
        values.put(DBURIConstants.COLUMN_UID, uid);
        values.put(DBURIConstants.COLUMN_TYPE, type);
        values.put(DBURIConstants.COLUMN_LAT, Lat);
        values.put(DBURIConstants.COLUMN_LON, Lon);
        values.put(DBURIConstants.COLUMN_HAE_M, hae);
        values.put(DBURIConstants.COLUMN_LE, le_m);
        values.put(DBURIConstants.COLUMN_CE, ce_m);
        values.put(DBURIConstants.COLUMN_HEIGHT, height_m);
        values.put(DBURIConstants.COLUMN_WIDTH, width_m);
        values.put(DBURIConstants.COLUMN_LENGTH, length_m);
        values.put(DBURIConstants.COLUMN_DESCRIPTION, Remark);
        values.put(DBURIConstants.COLUMN_ROTATION, rotation_deg_true);

        database.insert(
                DBURIConstants.TABLE_POINT_OBSTRUCTIONS, "unk", values);
        final Cursor cursor = database.query(
                DBURIConstants.TABLE_POINT_OBSTRUCTIONS,
                DBURIConstants.allColumnsPoint, DBURIConstants.COLUMN_UID
                        + " = " + uid, null,
                null, null, null);
        cursor.moveToFirst();
        PointObstruction newComment = ObstructionHelper
                .cursorToPointObstruction(cursor);
        cursor.close();
        return newComment;
    }

    public PointObstruction getPointObstructionFromID(int DBIDNumber) {
        String WhereClause = DBURIConstants.COLUMN_ID + " = ?";

        Cursor cursor = database.query(DBURIConstants.TABLE_POINT_OBSTRUCTIONS,
                null, WhereClause, new String[] {
                    String.format("%d", DBIDNumber)
                }, null, null, null);
        cursor.moveToFirst();
        PointObstruction po = ObstructionHelper
                .cursorToPointObstruction(cursor);
        cursor.close();
        return po;
    }

    public List<PointObstruction> getPointObstructionsInGroup(String GroupName) {
        List<PointObstruction> targets = new ArrayList<PointObstruction>();
        String WhereClause = DBURIConstants.COLUMN_GROUP_NAME_POINT + " = ?";

        Cursor cursor = database.query(DBURIConstants.TABLE_POINT_OBSTRUCTIONS,
                null, WhereClause, new String[] {
                    GroupName
                }, null, null, null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            PointObstruction target = ObstructionHelper
                    .cursorToPointObstruction(cursor);
            targets.add(target);
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();
        return targets;
    }

    public List<String> getGroupNames() {
        List<String> DBNameList = new ArrayList<String>();

        Cursor cursor = database.query(true,
                DBURIConstants.TABLE_POINT_OBSTRUCTIONS, new String[] {
                    DBURIConstants.COLUMN_GROUP_NAME_POINT
                }, null, null, DBURIConstants.COLUMN_GROUP_NAME_POINT, null,
                null, null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            DBNameList.add(cursor.getString(0));
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();
        return DBNameList;
    }

    public PointObstruction getPointObstruction(String Groupname, String uid) {
        String WhereClause = DBURIConstants.COLUMN_GROUP_NAME_POINT
                + " = ? and " + DBURIConstants.COLUMN_UID + " = ?";
        final Cursor cursor = database.query(
                DBURIConstants.TABLE_POINT_OBSTRUCTIONS,
                null, WhereClause, new String[] {
                        Groupname, uid
                }, null, null, null);

        if (cursor.getCount() == 0) {
            cursor.close();
            return null;
        }
        cursor.moveToFirst();
        PointObstruction returnItem = ObstructionHelper
                .cursorToPointObstruction(cursor);
        cursor.close();
        return returnItem;
    }

    public List<PointObstruction> getAllPointObstructions() {
        List<PointObstruction> targets = new ArrayList<PointObstruction>();

        Cursor cursor = database.query(DBURIConstants.TABLE_POINT_OBSTRUCTIONS,
                DBURIConstants.allColumnsPoint, null, null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            PointObstruction target = ObstructionHelper
                    .cursorToPointObstruction(cursor);
            targets.add(target);
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();
        return targets;
    }

    public int getSize(String GroupName) {
        //    List<PointObstructionDBItem> targets = new ArrayList<PointObstructionDBItem>();
        String WhereClause = DBURIConstants.COLUMN_GROUP_NAME_POINT + " = ?";

        Cursor cursor = database.query(DBURIConstants.TABLE_POINT_OBSTRUCTIONS,
                null, WhereClause, new String[] {
                    GroupName
                }, null, null, null);

        int Count = cursor.getCount();
        // Make sure to cl    ose the cursor
        cursor.close();
        return Count;
    }

    public void deletePointObstruction(String groupName, String uid) {

        if (groupName == null || groupName.length() < 1) {
            database.delete(DBURIConstants.TABLE_POINT_OBSTRUCTIONS,
                    DBURIConstants.COLUMN_UID
                            + " = " + uid, null);
        } else {
            //String WhereClause = DBURIConstants.COLUMN_UID + " = ? and "+DBURIConstants.COLUMN_GROUP_NAME_LINE + " = ? and "+DBURIConstants.COLUMN_POINT_ORDER+" = ?";
            //Result = database.delete(DBURIConstants.TABLE_LINE_OBSTRUCTIONS, WhereClause, new String[]{uid,groupName, String.format("%d", LinePoints-1)});

            database.delete(DBURIConstants.TABLE_POINT_OBSTRUCTIONS,
                    DBURIConstants.COLUMN_UID
                            + " = " + uid + " and "
                            + DBURIConstants.COLUMN_GROUP_NAME_POINT + " = "
                            + groupName, null);
        }
    }
}
