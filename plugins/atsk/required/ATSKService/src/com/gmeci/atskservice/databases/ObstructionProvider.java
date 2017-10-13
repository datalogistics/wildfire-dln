
package com.gmeci.atskservice.databases;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;

import com.gmeci.atskservice.resolvers.DBURIConstants;

import java.util.ArrayList;

public class ObstructionProvider extends ContentProvider {

    private static final int SINGLE_POINT = 1;
    private static final int GROUP_POINTS = 2;
    private static final int SINGLE_LINE = 3;
    private static final int GROUP_LINES = 4;
    private static final int LINE_POINT = 5;
    private static final int LINE_POINT_DISTINCT = 6;
    private static final UriMatcher URI_MATCHER;
    private static final String TAG = "ObstructionProvider";

    static {
        URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
        URI_MATCHER.addURI(DBURIConstants.OBSTRUCTION_AUTHORITY_BASE,
                DBURIConstants.POINT, SINGLE_POINT);
        URI_MATCHER.addURI(DBURIConstants.OBSTRUCTION_AUTHORITY_BASE,
                DBURIConstants.POINTS, GROUP_POINTS);

        URI_MATCHER.addURI(DBURIConstants.OBSTRUCTION_AUTHORITY_BASE,
                DBURIConstants.LINE, SINGLE_LINE);
        URI_MATCHER.addURI(DBURIConstants.OBSTRUCTION_AUTHORITY_BASE,
                DBURIConstants.LINE_POINT, LINE_POINT);
        URI_MATCHER.addURI(DBURIConstants.OBSTRUCTION_AUTHORITY_BASE,
                DBURIConstants.LINE_POINT_DISTINCT, LINE_POINT_DISTINCT);
        URI_MATCHER.addURI(DBURIConstants.OBSTRUCTION_AUTHORITY_BASE,
                DBURIConstants.LINES, GROUP_LINES);

    }

    LineObstructionDataSource lineDB;
    PointObstructionDataSource pointDB;

    @Override
    public int delete(Uri uri, String where, String[] where_args) {
        int URIType = URI_MATCHER.match(uri);
        OpenDBIfNeeded(URIType);
        if (URIType == SINGLE_POINT) {
            return pointDB.database.delete(
                    DBURIConstants.TABLE_POINT_OBSTRUCTIONS, where, where_args);
        } else if (URIType == SINGLE_LINE) {
            return lineDB.database.delete(
                    DBURIConstants.TABLE_LINE_OBSTRUCTIONS, where, where_args);

        } else if (URIType == LINE_POINT) {
            return lineDB.database.delete(
                    DBURIConstants.TABLE_LINE_OBSTRUCTION_POINTS, where,
                    where_args);
        }

        return 0;
    }

    @Override
    public String getType(Uri uri) {

        int URIType = URI_MATCHER.match(uri);
        if (URIType == SINGLE_POINT) {
            return "Point";
        } else if (URIType == SINGLE_LINE) {
            return "Line";
        } else if (URIType == LINE_POINT) {

            return "Line Point";
        }

        return "NONE";
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        int URIType = URI_MATCHER.match(uri);
        OpenDBIfNeeded(URIType);

        //LOU before inserting - we should see if there's anything else mathcing group/uid...

        if (URIType == SINGLE_POINT) {

            long ID = pointDB.database.insert(
                    DBURIConstants.TABLE_POINT_OBSTRUCTIONS, "unk", values);
            return Uri.parse(DBURIConstants.OBSTRUCTION_AUTHORITY_BASE + "/"
                    + DBURIConstants.POINT + "/" + ID);
        } else if (URIType == SINGLE_LINE) {
            long ID = lineDB.database.insert(
                    DBURIConstants.TABLE_LINE_OBSTRUCTIONS, "unk", values);

            return Uri.parse(DBURIConstants.OBSTRUCTION_AUTHORITY_BASE + "/"
                    + DBURIConstants.LINE + "/" + ID);
        } else if (URIType == LINE_POINT) {
            long ID = lineDB.database
                    .insert(DBURIConstants.TABLE_LINE_OBSTRUCTION_POINTS,
                            "unk", values);
            return Uri.parse(DBURIConstants.OBSTRUCTION_AUTHORITY_BASE + "/"
                    + DBURIConstants.LINE_POINT + "/" + ID);
        }

        return null;
    }

    @Override
    public boolean onCreate() {
        lineDB = new LineObstructionDataSource(getContext());
        pointDB = new PointObstructionDataSource(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {

        Cursor cursor = null;
        int URIType = URI_MATCHER.match(uri);

        OpenDBIfNeeded(URIType);

        if (URIType == SINGLE_LINE) {
            cursor = lineDB.database.query(
                    DBURIConstants.TABLE_LINE_OBSTRUCTIONS, projection,
                    selection, selectionArgs, null, null, sortOrder);
        } else if (URIType == LINE_POINT) {
            cursor = lineDB.database.query(
                    DBURIConstants.TABLE_LINE_OBSTRUCTION_POINTS, projection,
                    selection, selectionArgs, null, null, sortOrder);
        } else if (URIType == SINGLE_POINT) {
            cursor = pointDB.database.query(
                    DBURIConstants.TABLE_POINT_OBSTRUCTIONS, projection,
                    selection, selectionArgs, null, null, sortOrder);

        } else if (URIType == LINE_POINT_DISTINCT) {
            cursor = lineDB.database.query(true,
                    DBURIConstants.TABLE_LINE_OBSTRUCTION_POINTS, projection,
                    selection, selectionArgs, DBURIConstants.COLUMN_UID, null,
                    sortOrder, null);
        }

        return cursor;
    }

    private void OpenDBIfNeeded(int URIType) {
        if (!lineDB.isOpened()
                && (URIType == SINGLE_LINE || URIType == LINE_POINT
                        || URIType == GROUP_LINES || URIType == LINE_POINT_DISTINCT)) {
            lineDB.open();
        }
        if (!pointDB.isOpened()
                && (URIType == SINGLE_POINT || URIType == GROUP_POINTS)) {
            pointDB.open();
        }
    }

    @Override
    public ContentProviderResult[] applyBatch(
            ArrayList<ContentProviderOperation> operations)
            throws OperationApplicationException {
        // 
        return super.applyBatch(operations);
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        return super.bulkInsert(uri, values);
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        int Updated = 0;
        int URIType = URI_MATCHER.match(uri);
        if (URIType == LINE_POINT) {

            Updated = lineDB.database.update(
                    DBURIConstants.TABLE_LINE_OBSTRUCTION_POINTS, values,
                    selection, selectionArgs);

        } else if (URIType == SINGLE_LINE) {
            Updated = lineDB.database.update(
                    DBURIConstants.TABLE_LINE_OBSTRUCTIONS, values, selection,
                    selectionArgs);

        }
        //LOU no point obstruction on the list?
        return Updated;
    }

}
