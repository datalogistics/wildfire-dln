
package com.gmeci.atskservice.databases;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.gmeci.core.LineObstruction;
import com.gmeci.core.SurveyPoint;
import com.gmeci.atskservice.resolvers.DBURIConstants;
import com.gmeci.atskservice.resolvers.GradientDBItem;

import java.util.ArrayList;
import java.util.List;

public class LineObstructionDataSource {
    SQLiteDatabase database;
    Context context;
    private LineObstructionDBHelper dbLineHelper;
    private boolean Opened = false;

    public LineObstructionDataSource(Context context) {
        this.context = context;
        dbLineHelper = new LineObstructionDBHelper(context);
    }

    public static LineObstruction cursorToLineObstructionNoPoints(Cursor cursor) {
        LineObstruction line = new LineObstruction();
        line.group = (cursor.getString(1));
        line.uid = (cursor.getString(2));
        line.type = (cursor.getString(3));
        line.height = (cursor.getFloat(4));
        line.remarks = (cursor.getString(5));
        line.closed = (cursor.getInt(6) > 0);
        line.filled = (cursor.getInt(7) > 0);
        return line;
    }

    public boolean isOpened() {
        return Opened;
    }

    public void open() throws SQLException {
        Opened = true;
        dbLineHelper = new LineObstructionDBHelper(context);
        database = dbLineHelper.getWritableDatabase();
    }

    public void close() {
        Opened = false;
        dbLineHelper.close();
    }

    public boolean updateLineDetails(LineObstruction obstruction2Update) {
        ContentValues values = new ContentValues();
        values.put(DBURIConstants.COLUMN_GROUP_NAME_LINE,
                obstruction2Update.group);
        values.put(DBURIConstants.COLUMN_UID, obstruction2Update.uid);
        values.put(DBURIConstants.COLUMN_TYPE, obstruction2Update.type);
        values.put(DBURIConstants.COLUMN_HEIGHT, obstruction2Update.height);
        values.put(DBURIConstants.COLUMN_WIDTH, obstruction2Update.width);
        values.put(DBURIConstants.COLUMN_DESCRIPTION,
                obstruction2Update.remarks);
        values.put(DBURIConstants.COLUMN_CLOSED, obstruction2Update.closed);
        values.put(DBURIConstants.COLUMN_FILLED, obstruction2Update.filled);

        int RowsEffected = database.update(
                DBURIConstants.TABLE_LINE_OBSTRUCTIONS, values,
                DBURIConstants.COLUMN_UID + "=" + obstruction2Update.uid, null);

        return RowsEffected != 0;
    }

    public int countLinePoints(String groupName, String uid) {
        String WhereClause = DBURIConstants.COLUMN_GROUP_NAME_LINE
                + " = ? and " + DBURIConstants.COLUMN_UID + " = ?";
        Cursor cursor = database.query(
                DBURIConstants.TABLE_LINE_OBSTRUCTION_POINTS,
                null, WhereClause, new String[] {
                        groupName, uid
                }, null, null, null);

        int Count = cursor.getCount();
        // Make sure to close the cursor
        cursor.close();
        return Count;
    }

    public boolean updateLinePoints(String groupName, String uid,
            List<SurveyPoint> newLinePoints) {
        deleteLinePoints(groupName, uid);

        //add new points
        int order = 0;
        boolean Success = true;
        for (SurveyPoint currentPoint : newLinePoints) {
            Success = Success
                    && appendLinePoint(groupName, uid, currentPoint, order);
            order++;
        }
        return Success;
    }

    public boolean appendLinePoint(String groupName, String uid,
            SurveyPoint newLinePoint) {

        int LinePoints = countLinePoints(groupName, uid);
        appendLinePoint(groupName, uid, newLinePoint, LinePoints);
        return false;
    }

    public boolean appendLinePoint(String groupName, String uid,
            SurveyPoint newLinePoint, int order) {
        ContentValues values = new ContentValues();
        values.put(DBURIConstants.COLUMN_GROUP_NAME_LINE, groupName);
        values.put(DBURIConstants.COLUMN_UID, uid);
        values.put(DBURIConstants.COLUMN_POINT_ORDER, order);

        values.put(DBURIConstants.COLUMN_LAT, newLinePoint.lat);
        values.put(DBURIConstants.COLUMN_LON, newLinePoint.lon);
        values.put(DBURIConstants.COLUMN_HAE_M, newLinePoint.getHAE());
        values.put(DBURIConstants.COLUMN_LE, newLinePoint.circularError);
        values.put(DBURIConstants.COLUMN_CE, newLinePoint.linearError);

        long insertId = database.insert(
                DBURIConstants.TABLE_LINE_OBSTRUCTION_POINTS, "unk", values);
        return insertId > -1;
    }

    public boolean deleteLastLinePoint(String groupName, String uid) {
        int LinePoints = countLinePoints(groupName, uid);
        int Result;
        if (groupName == null || groupName.length() < 1) {

            String WhereClause = DBURIConstants.COLUMN_UID + " = ? and "
                    + DBURIConstants.COLUMN_POINT_ORDER + " = ?";
            Result = database.delete(DBURIConstants.TABLE_LINE_OBSTRUCTIONS,
                    WhereClause, new String[] {
                            uid, String.format("%d", LinePoints - 1)
                    });
        } else {
            String WhereClause = DBURIConstants.COLUMN_UID + " = ? and "
                    + DBURIConstants.COLUMN_GROUP_NAME_LINE + " = ? and "
                    + DBURIConstants.COLUMN_POINT_ORDER + " = ?";
            Result = database.delete(DBURIConstants.TABLE_LINE_OBSTRUCTIONS,
                    WhereClause, new String[] {
                            uid, groupName, String.format("%d", LinePoints - 1)
                    });
        }
        return Result > 0;
    }

    public boolean deleteLine(String groupName, String uid) {
        if (groupName == null || groupName.length() < 1) {
            database.delete(DBURIConstants.TABLE_LINE_OBSTRUCTIONS,
                    DBURIConstants.COLUMN_UID + " = " + uid, null);
        } else {
            String WhereClause = DBURIConstants.COLUMN_UID + " = ? and "
                    + DBURIConstants.COLUMN_GROUP_NAME_LINE + " = ?";
            database.delete(DBURIConstants.TABLE_LINE_OBSTRUCTIONS,
                    WhereClause, new String[] {
                            uid, groupName
                    });
        }
        return deleteLinePoints(groupName, uid);
    }

    public boolean deleteLinePoints(String groupName, String uid) {
        int Result;
        if (groupName == null || groupName.length() < 1) {
            Result = database.delete(
                    DBURIConstants.TABLE_LINE_OBSTRUCTION_POINTS,
                    DBURIConstants.COLUMN_UID
                            + " = " + uid, null);
        } else {
            String WhereClause = DBURIConstants.COLUMN_UID + " = ? and "
                    + DBURIConstants.COLUMN_GROUP_NAME_LINE + " = ?";
            Result = database.delete(
                    DBURIConstants.TABLE_LINE_OBSTRUCTION_POINTS, WhereClause,
                    new String[] {
                            uid, groupName
                    });
        }
        return Result > 0;
    }

    public long createLineObstruction(String group, String uid, String type,
            String Remark, double height_m, double width_m, boolean closed,
            boolean filled) {
        ContentValues values = new ContentValues();
        values.put(DBURIConstants.COLUMN_GROUP_NAME_LINE, group);
        values.put(DBURIConstants.COLUMN_UID, uid);
        values.put(DBURIConstants.COLUMN_TYPE, type);
        values.put(DBURIConstants.COLUMN_HEIGHT, height_m);
        values.put(DBURIConstants.COLUMN_WIDTH, width_m);
        values.put(DBURIConstants.COLUMN_DESCRIPTION, Remark);
        values.put(DBURIConstants.COLUMN_CLOSED, closed);
        values.put(DBURIConstants.COLUMN_FILLED, filled);

        long insertId = database.insert(DBURIConstants.TABLE_LINE_OBSTRUCTIONS,
                "unk", values);

        return insertId;
    }

    public List<LineObstruction> getLineObstructions() {
        List<LineObstruction> lines = new ArrayList<LineObstruction>();

        Cursor lineCursor = database.query(
                DBURIConstants.TABLE_LINE_OBSTRUCTIONS,
                DBURIConstants.allColumnsLine, null, null, null, null, null);
        lineCursor.moveToFirst();
        while (!lineCursor.isAfterLast()) {
            LineObstruction newLO = cursorToLineObstructionNoPoints(lineCursor);

            //get points for this line
            ArrayList<SurveyPoint> LinePointList = getLinePointList(
                    newLO.group, newLO.uid);
            newLO.points = LinePointList;

            lines.add(newLO);
            lineCursor.moveToNext();
        }
        // Make sure to close the cursor
        lineCursor.close();
        return lines;
    }

    public List<LineObstruction> getLineObstructionsInGroup(String GroupName) {
        List<LineObstruction> lines = new ArrayList<LineObstruction>();
        String WhereClause = DBURIConstants.COLUMN_GROUP_NAME_LINE + " = ?";

        Cursor lineCursor = database.query(
                DBURIConstants.TABLE_LINE_OBSTRUCTIONS,
                DBURIConstants.allColumnsLine, WhereClause, new String[] {
                    GroupName
                }, null, null, null);
        lineCursor.moveToFirst();
        while (!lineCursor.isAfterLast()) {
            LineObstruction newLO = cursorToLineObstructionNoPoints(lineCursor);

            //get points for this line
            ArrayList<SurveyPoint> LinePointList = getLinePointList(
                    newLO.group, newLO.uid);
            newLO.points = LinePointList;

            lines.add(newLO);
            lineCursor.moveToNext();
        }
        // Make sure to close the cursor
        lineCursor.close();
        return lines;
    }

    public List<LineObstruction> getLineObstructionItemsInGroup(String GroupName) {
        List<LineObstruction> lines = new ArrayList<LineObstruction>();
        String WhereClause = DBURIConstants.COLUMN_GROUP_NAME_LINE + " = ?";

        Cursor lineCursor = database.query(
                DBURIConstants.TABLE_LINE_OBSTRUCTIONS,
                DBURIConstants.allColumnsLine, WhereClause, new String[] {
                    GroupName
                }, null, null, null);
        lineCursor.moveToFirst();
        while (!lineCursor.isAfterLast()) {
            LineObstruction newLine = cursorToLineObstructionNoPoints(lineCursor);

            //get points for this line
            ArrayList<SurveyPoint> LinePointList = getLinePointList(
                    newLine.group, newLine.uid);
            newLine.points = LinePointList;

            lines.add(newLine);
            lineCursor.moveToNext();
        }
        // Make sure to close the cursor
        lineCursor.close();
        return lines;
    }

    private ArrayList<SurveyPoint> getLinePointList(String groupName, String uid) {
        ArrayList<SurveyPoint> linePoints = new ArrayList<SurveyPoint>();
        String WhereClause = DBURIConstants.COLUMN_GROUP_NAME_LINE
                + " = ? and " + DBURIConstants.COLUMN_UID + " = ?";

        Cursor linePointCursor = database.query(
                DBURIConstants.TABLE_LINE_OBSTRUCTION_POINTS,
                DBURIConstants.allColumnsLinePoints, WhereClause, new String[] {
                        groupName, uid
                }, null, null, DBURIConstants.COLUMN_POINT_ORDER + " ASC");
        linePointCursor.moveToFirst();
        while (!linePointCursor.isAfterLast()) {
            SurveyPoint NextPoint = cursorToLinePoint(linePointCursor);
            linePoints.add(NextPoint);
            linePointCursor.moveToNext();
        }
        // Make sure to close the cursor
        linePointCursor.close();
        return linePoints;
    }

    public List<String> getLineGroupNames() {
        List<String> DBNameList = new ArrayList<String>();

        Cursor cursor = database.query(true,
                DBURIConstants.TABLE_LINE_OBSTRUCTIONS, new String[] {
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

    private SurveyPoint cursorToLinePoint(Cursor linePointCursor) {

        SurveyPoint linePoint = new SurveyPoint();
        linePoint.lat = linePointCursor.getDouble(4);
        linePoint.lon = linePointCursor.getDouble(5);
        linePoint.setHAE(linePointCursor.getFloat(6));
        linePoint.linearError = linePointCursor.getFloat(7);
        linePoint.circularError = linePointCursor.getFloat(8);
        return linePoint;
    }

    public int getLineGroupSize(String GroupName) {
        String WhereClause = DBURIConstants.COLUMN_GROUP_NAME_LINE + " = ?";
        Cursor cursor = database.query(DBURIConstants.TABLE_LINE_OBSTRUCTIONS,
                null, WhereClause, new String[] {
                    GroupName
                }, null, null, null);

        int Count = cursor.getCount();
        cursor.close();
        return Count;
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
        Cursor cursor = database.query(DBURIConstants.TABLE_GRADIENT_POINTS,
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
            database.delete(DBURIConstants.TABLE_GRADIENT,
                    DBURIConstants.COLUMN_UID + " = " + uid, null);
        } else {
            String WhereClause = DBURIConstants.COLUMN_UID + " = ? and "
                    + DBURIConstants.COLUMN_GROUP_NAME_LINE + " = ?";
            database.delete(DBURIConstants.TABLE_GRADIENT,
                    WhereClause, new String[] {
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

        long insertId = database.insert(DBURIConstants.TABLE_GRADIENT, "unk",
                values);

        return insertId;
    }

    public List<GradientDBItem> getGradients() {
        List<GradientDBItem> gradients = new ArrayList<GradientDBItem>();

        Cursor gradientCursor = database.query(DBURIConstants.TABLE_GRADIENT,
                DBURIConstants.allColumnsLine, null, null, null, null, null);
        gradientCursor.moveToFirst();
        while (!gradientCursor.isAfterLast()) {
            GradientDBItem newGradientDBItem = cursorToGradient(gradientCursor);

            //get points for this line
            ArrayList<SurveyPoint> GradientPointList = (ArrayList<SurveyPoint>) getGradientPointList(
                    newGradientDBItem.getGroup(), newGradientDBItem.getUid());
            newGradientDBItem.setLinePoints(GradientPointList);

            gradients.add(newGradientDBItem);
            gradientCursor.moveToNext();
        }
        // Make sure to close the cursor
        gradientCursor.close();
        return gradients;
    }

    public List<GradientDBItem> getGradientsInGroup(String GroupName) {
        List<GradientDBItem> gradients = new ArrayList<GradientDBItem>();
        String WhereClause = DBURIConstants.COLUMN_GROUP_NAME_LINE + " = ?";

        Cursor gradientCursor = database.query(DBURIConstants.TABLE_GRADIENT,
                DBURIConstants.allColumnsLine, WhereClause, new String[] {
                    GroupName
                }, null, null, null);
        gradientCursor.moveToFirst();
        while (!gradientCursor.isAfterLast()) {
            GradientDBItem newLineDBItem = cursorToGradient(gradientCursor);

            //get points for this line
            ArrayList<SurveyPoint> LinePointList = getLinePointList(
                    newLineDBItem.getGroup(), newLineDBItem.getUid());
            newLineDBItem.setLinePoints(LinePointList);

            gradients.add(newLineDBItem);
            gradientCursor.moveToNext();
        }
        // Make sure to close the cursor
        gradientCursor.close();
        return gradients;
    }

    private List<SurveyPoint> getGradientPointList(String groupName, String uid) {
        List<SurveyPoint> gradientPoints = new ArrayList<SurveyPoint>();
        String WhereClause = DBURIConstants.COLUMN_GROUP_NAME_LINE
                + " = ? and " + DBURIConstants.COLUMN_UID + " = ?";

        Cursor linePointCursor = database.query(
                DBURIConstants.TABLE_GRADIENT_POINTS,
                DBURIConstants.allColumnsLinePoints, WhereClause, new String[] {
                        groupName, uid
                }, null, null, DBURIConstants.COLUMN_POINT_ORDER + " ASC");
        linePointCursor.moveToFirst();
        while (!linePointCursor.isAfterLast()) {
            SurveyPoint NextPoint = cursorToLinePoint(linePointCursor);
            gradientPoints.add(NextPoint);
            linePointCursor.moveToNext();
        }
        // Make sure to close the cursor
        linePointCursor.close();
        return gradientPoints;
    }

    public List<String> getGradientGroupNames() {
        List<String> DBNameList = new ArrayList<String>();

        Cursor cursor = database.query(true, DBURIConstants.TABLE_GRADIENT,
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

    private SurveyPoint cursorToGradientPoint(Cursor linePointCursor) {

        SurveyPoint linePoint = new SurveyPoint();
        linePoint.lat = linePointCursor.getDouble(4);
        linePoint.lon = linePointCursor.getDouble(5);
        linePoint.setHAE(linePointCursor.getFloat(6));
        linePoint.linearError = linePointCursor.getFloat(7);
        linePoint.circularError = linePointCursor.getFloat(8);
        return linePoint;
    }

    private GradientDBItem cursorToGradient(Cursor cursor) {
        GradientDBItem gradient = new GradientDBItem();
        gradient.setId(cursor.getLong(0));
        gradient.setGroup(cursor.getString(1));
        gradient.setUid(cursor.getString(2));
        gradient.setType(cursor.getString(3));
        gradient.setRemark(cursor.getString(4));

        gradient.setShoulderGradientL(cursor.getFloat(6));
        gradient.setShoulderGradientR(cursor.getFloat(7));
        gradient.setGradedGradientL(cursor.getFloat(8));
        gradient.setGradedGradientR(cursor.getFloat(9));
        gradient.setMaintainedGradientL(cursor.getFloat(10));
        gradient.setMaintainedGradientR(cursor.getFloat(11));
        gradient.setLZGradientL(cursor.getFloat(12));
        gradient.setLZGradientR(cursor.getFloat(13));

        return gradient;
    }

    public int getGradientSize(String GroupName) {
        String WhereClause = DBURIConstants.COLUMN_GROUP_NAME_LINE + " = ?";
        Cursor cursor = database.query(DBURIConstants.TABLE_GRADIENT,
                null, WhereClause, new String[] {
                    GroupName
                }, null, null, null);

        int Count = cursor.getCount();
        cursor.close();
        return Count;
    }

    //Done Gradient stuff
}
