
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

public class GradientProvider extends ContentProvider {

    private static final int SINGLE_GRADIENT = 1;
    private static final int GRADIENT_POINT = 2;
    private static final UriMatcher GRADIENT_URI_MATCHER;
    @SuppressWarnings("unused")
    private static final String TAG = "GradientProvider";

    static {
        GRADIENT_URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
        GRADIENT_URI_MATCHER.addURI(DBURIConstants.GRADIENT_AUTHORITY_BASE,
                DBURIConstants.GRADIENT, SINGLE_GRADIENT);
        GRADIENT_URI_MATCHER.addURI(DBURIConstants.GRADIENT_AUTHORITY_BASE,
                DBURIConstants.GRADIENT_POINT, GRADIENT_POINT);
    }

    GradientDataSource gradientDB;

    @Override
    public int delete(Uri uri, String where, String[] where_args) {
        int URIType = GRADIENT_URI_MATCHER.match(uri);
        OpenDBIfNeeded(URIType);
        if (URIType == SINGLE_GRADIENT) {
            return gradientDB.database.delete(DBURIConstants.TABLE_GRADIENT,
                    where, where_args);
        } else if (URIType == GRADIENT_POINT) {
            return gradientDB.database.delete(
                    DBURIConstants.TABLE_GRADIENT_POINTS, where, where_args);
        }

        return 0;
    }

    @Override
    public String getType(Uri uri) {

        int URIType = GRADIENT_URI_MATCHER.match(uri);
        if (URIType == SINGLE_GRADIENT) {

            return "Gradinet";
        } else if (URIType == GRADIENT_POINT) {

            return "Gradinet Point";
        }
        return "NONE";
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        int URIType = GRADIENT_URI_MATCHER.match(uri);
        OpenDBIfNeeded(URIType);

        //LOU before inserting - we should see if there's anything else mathcing group/uid...

        if (URIType == SINGLE_GRADIENT) {
            long ID = gradientDB.database.insert(DBURIConstants.TABLE_GRADIENT,
                    "unk", values);

            return Uri.parse(DBURIConstants.GRADIENT_AUTHORITY_BASE + "/"
                    + DBURIConstants.GRADIENT + "/" + ID);
        } else if (URIType == GRADIENT_POINT) {
            long ID = gradientDB.database.insert(
                    DBURIConstants.TABLE_GRADIENT_POINTS, "unk", values);
            return Uri.parse(DBURIConstants.GRADIENT_AUTHORITY_BASE + "/"
                    + DBURIConstants.GRADIENT_POINT + "/" + ID);
        }

        return null;
    }

    @Override
    public boolean onCreate() {
        gradientDB = new GradientDataSource(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {

        Cursor cursor = null;
        int URIType = GRADIENT_URI_MATCHER.match(uri);

        OpenDBIfNeeded(URIType);
        //    Log.d(TAG, "URI "+uri+" maps to "+URIType);
        if (URIType == GRADIENT_POINT) {
            //Log.d(TAG, "Querying Gradient Points");
            cursor = gradientDB.database.query(
                    DBURIConstants.TABLE_GRADIENT_POINTS, projection,
                    selection, selectionArgs, null, null, sortOrder);
        } else if (URIType == SINGLE_GRADIENT) {
            //Log.d(TAG, "Querying Single Gradient");
            cursor = gradientDB.database
                    .query(DBURIConstants.TABLE_GRADIENT, projection,
                            selection, selectionArgs, null, null, sortOrder);
        }

        return cursor;
    }

    private void OpenDBIfNeeded(int URIType) {
        if ((URIType == SINGLE_GRADIENT || URIType == GRADIENT_POINT)
                && !gradientDB.isOpened()) {
            gradientDB.open();
        }
    }

    @Override
    public ContentProviderResult[] applyBatch(
            ArrayList<ContentProviderOperation> operations)
            throws OperationApplicationException {
        return super.applyBatch(operations);
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        int URIType = GRADIENT_URI_MATCHER.match(uri);

        if (URIType == GRADIENT_POINT) {

            gradientDB.database.beginTransaction();
            for (int i = 0; i < values.length; i++) {
                gradientDB.database.insert(
                        DBURIConstants.TABLE_GRADIENT_POINTS, "unk", values[i]);
            }
            gradientDB.database.setTransactionSuccessful();
            gradientDB.database.endTransaction();

            return values.length;
        } else
            return super.bulkInsert(uri, values);
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        int Updated = 0;
        int URIType = GRADIENT_URI_MATCHER.match(uri);

        if (URIType == GRADIENT_POINT) {

            Updated = gradientDB.database.update(
                    DBURIConstants.TABLE_GRADIENT_POINTS, values, selection,
                    selectionArgs);

        } else if (URIType == SINGLE_GRADIENT) {

            Updated = gradientDB.database.update(DBURIConstants.TABLE_GRADIENT,
                    values, selection, selectionArgs);

        }
        //LOU no point obstruction on the list?
        return Updated;
    }

}
