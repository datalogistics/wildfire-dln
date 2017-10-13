
package com.gmeci.atskservice.databases;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.gmeci.core.SurveyPoint;
import com.gmeci.atskservice.resolvers.DBURIConstants;
import com.gmeci.atskservice.resolvers.GradientDBItem;

import java.util.ArrayList;
import java.util.List;

public class GradientDataSource {
    SQLiteDatabase database;
    private Context context;
    private GradientDBHelper dbLineHelper;
    private boolean Opened = false;

    public GradientDataSource(Context context) {
        this.context = context;
        dbLineHelper = new GradientDBHelper(context);
    }

    public boolean isOpened() {
        return Opened;
    }

    public void open() throws SQLException {
        Opened = true;
        dbLineHelper = new GradientDBHelper(context);
        database = dbLineHelper.getWritableDatabase();
    }

    public void close() {
        Opened = false;
        dbLineHelper.close();
    }

    //Gradient stuff
    public boolean updateGradientDetails(GradientDBItem gradient2Update) {
        ContentValues values = new ContentValues();
        values.put(DBURIConstants.COLUMN_GROUP_NAME_LINE,
                gradient2Update.getGroup());
        values.put(DBURIConstants.COLUMN_UID, gradient2Update.getUid());
        values.put(DBURIConstants.COLUMN_TYPE, gradient2Update.getType());
        values.put(DBURIConstants.COLUMN_DESCRIPTION,
                gradient2Update.getRemark());
        values.put(DBURIConstants.COLUMN_ANALYSIS_STATE,
                gradient2Update.getAnalysisState());
        values.put(DBURIConstants.COLUMN_HIDDEN,
                Boolean.toString(gradient2Update.getHidden()));

        values.put(DBURIConstants.COLUMN_GRADED_GRADIENT_L,
                gradient2Update.getAnalysisState());
        values.put(DBURIConstants.COLUMN_GRADED_GRADIENT_R,
                gradient2Update.getAnalysisState());
        values.put(DBURIConstants.COLUMN_SHOULDER_GRADIENT_L,
                gradient2Update.getAnalysisState());
        values.put(DBURIConstants.COLUMN_SHOULDER_GRADIENT_L,
                gradient2Update.getAnalysisState());
        values.put(DBURIConstants.COLUMN_LZ_GRADIENT_L,
                gradient2Update.getAnalysisState());
        values.put(DBURIConstants.COLUMN_LZ_GRADIENT_L,
                gradient2Update.getAnalysisState());
        values.put(DBURIConstants.COLUMN_MAINTAINED_GRADIENT_L,
                gradient2Update.getAnalysisState());
        values.put(DBURIConstants.COLUMN_MAINTAINED_GRADIENT_R,
                gradient2Update.getAnalysisState());

        int RowsEffected = database.update(DBURIConstants.TABLE_GRADIENT,
                values,
                DBURIConstants.COLUMN_ID + "=" + gradient2Update.getId(), null);

        return RowsEffected != 0;
    }

    public int countGradientPoints(String groupName, String uid) {
        String WhereClause = DBURIConstants.COLUMN_GROUP_NAME_LINE
                + " = ? and " + DBURIConstants.COLUMN_UID + " = ?";
        final Cursor cursor = database.query(
                DBURIConstants.TABLE_GRADIENT_POINTS,
                null, WhereClause, new String[] {
                        groupName, uid
                }, null, null, null);

        int Count = cursor.getCount();
        // Make sure to close the cursor
        cursor.close();
        return Count;
    }

    public boolean updateGradientPoints(String groupName, String uid,
            List<SurveyPoint> newGradientPoints) {
        deleteGradientPoints(groupName, uid);

        //add new points
        int order = 0;
        boolean Success = true;
        for (SurveyPoint currentPoint : newGradientPoints) {
            Success = Success
                    && appendGradientPoint(groupName, uid, currentPoint, order,
                            true);
            order++;
        }
        return Success;
    }

    public int countLinePoints(String groupName, String uid) {
        String WhereClause = DBURIConstants.COLUMN_GROUP_NAME_LINE
                + " = ? and " + DBURIConstants.COLUMN_UID + " = ?";
        final Cursor cursor = database.query(
                DBURIConstants.TABLE_GRADIENT_POINTS,
                null, WhereClause, new String[] {
                        groupName, uid
                }, null, null, null);

        int Count = cursor.getCount();
        // Make sure to close the cursor
        cursor.close();
        return Count;
    }

    public boolean appendGradientPoint(String groupName, String uid,
            SurveyPoint newGradientPoint, boolean Show) {

        int GradientPoints = countLinePoints(groupName, uid);
        appendGradientPoint(groupName, uid, newGradientPoint, GradientPoints,
                Show);
        return false;
    }

    public boolean appendGradientPoint(String groupName, String uid,
            SurveyPoint newGradientPoint, int order, boolean Show) {
        ContentValues values = new ContentValues();
        values.put(DBURIConstants.COLUMN_GROUP_NAME_LINE, groupName);
        values.put(DBURIConstants.COLUMN_UID, uid);
        values.put(DBURIConstants.COLUMN_POINT_ORDER, order);

        values.put(DBURIConstants.COLUMN_LAT, newGradientPoint.lat);
        values.put(DBURIConstants.COLUMN_LON, newGradientPoint.lon);
        values.put(DBURIConstants.COLUMN_HAE_M, newGradientPoint.getHAE());
        values.put(DBURIConstants.COLUMN_LE, newGradientPoint.circularError);
        values.put(DBURIConstants.COLUMN_CE, newGradientPoint.linearError);
        if (Show)
            values.put(DBURIConstants.COLUMN_SHOW, "true");
        else
            values.put(DBURIConstants.COLUMN_SHOW, "false");

        long insertId = database.insert(DBURIConstants.TABLE_GRADIENT_POINTS,
                "unk", values);
        return insertId > -1;
    }

    public boolean deleteLastGradientPoint(String groupName, String uid) {
        int GradientPoints = countLinePoints(groupName, uid);
        int Result;
        if (groupName == null || groupName.length() < 1) {

            String WhereClause = DBURIConstants.COLUMN_UID + " = ? and "
                    + DBURIConstants.COLUMN_POINT_ORDER + " = ?";
            Result = database.delete(DBURIConstants.TABLE_GRADIENT,
                    WhereClause, new String[] {
                            uid, String.format("%d", GradientPoints - 1)
                    });
        } else {
            String WhereClause = DBURIConstants.COLUMN_UID + " = ? and "
                    + DBURIConstants.COLUMN_GROUP_NAME_LINE + " = ? and "
                    + DBURIConstants.COLUMN_POINT_ORDER + " = ?";
            Result = database.delete(
                    DBURIConstants.TABLE_GRADIENT,
                    WhereClause,
                    new String[] {
                            uid, groupName,
                            String.format("%d", GradientPoints - 1)
                    });
        }
        return Result > 0;
    }

    public boolean deleteGradient(String groupName, String uid) {
        if (groupName == null || groupName.length() < 1) {
            //String WhereClause = DBURIConstants.COLUMN_UID + " = ?";
            database.delete(DBURIConstants.TABLE_GRADIENT,
                    DBURIConstants.COLUMN_UID + " = " + uid, null);
        } else {
            String WhereClause = DBURIConstants.COLUMN_UID + " = ? and "
                    + DBURIConstants.COLUMN_GROUP_NAME_LINE + " = ?";
            database.delete(DBURIConstants.TABLE_GRADIENT, WhereClause,
                    new String[] {
                            uid, groupName
                    });
        }
        return deleteGradientPoints(groupName, uid);
    }

    public boolean deleteGradientPoints(String groupName, String uid) {
        int Result;
        if (groupName == null || groupName.length() < 1) {
            Result = database.delete(DBURIConstants.TABLE_GRADIENT_POINTS,
                    DBURIConstants.COLUMN_UID
                            + " = " + uid, null);
        } else {
            String WhereClause = DBURIConstants.COLUMN_UID + " = ? and "
                    + DBURIConstants.COLUMN_GROUP_NAME_LINE + " = ?";
            Result = database.delete(DBURIConstants.TABLE_GRADIENT_POINTS,
                    WhereClause, new String[] {
                            uid, groupName
                    });
        }
        return Result > 0;
    }

    public long createGradient(String group, String uid, String type,
            String Remark) {
        ContentValues values = new ContentValues();
        values.put(DBURIConstants.COLUMN_GROUP_NAME_LINE, group);
        values.put(DBURIConstants.COLUMN_UID, uid);
        values.put(DBURIConstants.COLUMN_TYPE, type);
        values.put(DBURIConstants.COLUMN_DESCRIPTION, Remark);
        values.put(DBURIConstants.COLUMN_HIDDEN, Boolean.toString(false));

        long insertId = database.insert(DBURIConstants.TABLE_GRADIENT, "unk",
                values);

        return insertId;
    }

    public List<String> getGradientGroupNames() {
        List<String> DBNameList = new ArrayList<String>();

        final Cursor cursor = database.query(true,
                DBURIConstants.TABLE_GRADIENT,
                new String[] {
                    DBURIConstants.COLUMN_GROUP_NAME_LINE
                }, null, null, DBURIConstants.COLUMN_GROUP_NAME_LINE, null,
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

    public int getGradientSize(String GroupName) {
        String WhereClause = DBURIConstants.COLUMN_GROUP_NAME_LINE + " = ?";
        final Cursor cursor = database.query(DBURIConstants.TABLE_GRADIENT,
                null, WhereClause, new String[] {
                    GroupName
                }, null, null, null);

        int Count = cursor.getCount();
        cursor.close();
        return Count;
    }

    //Done Gradient stuff
}
