
package com.gmeci.atskservice.databases;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;

import com.gmeci.atskservice.resolvers.AZURIConstants;

import java.util.ArrayList;

public class AZProvider extends ContentProvider {

    private static final int SINGLE_AZ = 1;
    private static final int GROUP_AZS = 2;
    private static final int AZ_SETTINGS = 3;
    private static final UriMatcher URI_MATCHER;
    @SuppressWarnings("unused")
    private static final String TAG = "AZProvider";

    static {
        URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
        URI_MATCHER.addURI(AZURIConstants.AUTHORITY_BASE, AZURIConstants.AZ,
                SINGLE_AZ);
        URI_MATCHER.addURI(AZURIConstants.AUTHORITY_BASE, AZURIConstants.AZS,
                GROUP_AZS);
        URI_MATCHER.addURI(AZURIConstants.AUTHORITY_BASE,
                AZURIConstants.AZ_SETTINGS, AZ_SETTINGS);
    }

    AZDataSource azDB;

    @Override
    public int delete(Uri uri, String where, String[] where_args) {
        int URIType = URI_MATCHER.match(uri);
        OpenDBIfNeeded(URIType);
        if (URIType == SINGLE_AZ || URIType == GROUP_AZS) {
            return azDB.database.delete(AZURIConstants.TABLE_AZ, where,
                    where_args);
        } else if (URIType == AZ_SETTINGS) {
            return azDB.database.delete(AZURIConstants.TABLE_AZ_SETTINGS,
                    where, where_args);
        }

        return 0;
    }

    @Override
    public String getType(Uri uri) {

        int URIType = URI_MATCHER.match(uri);
        if (URIType == SINGLE_AZ) {
            return "AZ";
        } else if (URIType == GROUP_AZS) {
            return "AZs";
        } else if (URIType == AZ_SETTINGS) {
            return "Settings";
        }
        return "NONE";
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        int URIType = URI_MATCHER.match(uri);
        OpenDBIfNeeded(URIType);
        if (URIType == SINGLE_AZ || URIType == GROUP_AZS) {
            //LOU look for duplicates first !!!!!
            long ID = azDB.database.insert(AZURIConstants.TABLE_AZ, "unk",
                    values);
            return Uri.parse(AZURIConstants.AUTHORITY_BASE + "/"
                    + AZURIConstants.AZ + "/" + ID);
        } else if (URIType == AZ_SETTINGS) {
            long ID = azDB.database.insert(AZURIConstants.TABLE_AZ_SETTINGS,
                    "unk", values);
            return Uri.parse(AZURIConstants.AUTHORITY_BASE + "/"
                    + AZURIConstants.AZ + "/" + ID);
        }

        return null;
    }

    @Override
    public boolean onCreate() {
        azDB = new AZDataSource(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {

        Cursor cursor = null;
        int URIType = URI_MATCHER.match(uri);

        OpenDBIfNeeded(URIType);
        if (URIType == SINGLE_AZ || URIType == GROUP_AZS) {
            cursor = azDB.database.query(AZURIConstants.TABLE_AZ, projection,
                    selection, selectionArgs, null, null, sortOrder);
        } else if (URIType == AZ_SETTINGS) {
            cursor = azDB.database
                    .query(AZURIConstants.TABLE_AZ_SETTINGS, projection,
                            selection, selectionArgs, null, null, sortOrder);
        }

        return cursor;
    }

    private void OpenDBIfNeeded(int URIType) {
        if (!azDB.isOpened()
                && (URIType == SINGLE_AZ || URIType == GROUP_AZS || URIType == AZ_SETTINGS)) {
            azDB.open();
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
        return super.bulkInsert(uri, values);
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        int Updated = 0;
        int URIType = URI_MATCHER.match(uri);
        OpenDBIfNeeded(URIType);
        if (URIType == SINGLE_AZ) {

            Updated = azDB.database.update(AZURIConstants.TABLE_AZ, values,
                    selection, selectionArgs);

        } else if (URIType == AZ_SETTINGS) {
            Updated = azDB.database.update(AZURIConstants.TABLE_AZ_SETTINGS,
                    values, selection, selectionArgs);
            if (Updated < 1) {
                Updated = (int) azDB.database.insert(
                        AZURIConstants.TABLE_AZ_SETTINGS, "unk", values);
            }
        }
        return Updated;
    }

}
