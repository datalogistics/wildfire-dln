
package com.gmeci.atskservice.resolvers;

import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentProviderOperation.Builder;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.util.Log;

import com.gmeci.constants.Constants;
import com.gmeci.core.ATSKConstants;
import com.gmeci.core.LineObstruction;
import com.gmeci.core.PointObstruction;
import com.gmeci.core.SurveyPoint;
import com.gmeci.core.SurveyPoint.CollectionMethod;

import java.util.ArrayList;
import java.util.List;

public class ObstructionProviderClient {

    private static final String TAG = "ObstructionProvider";
    //make sure you add the permission gmeci.READ to the manifest when using this class!!!!!
    private ContentProviderClient CPClient;
    private final Context context;

    public ObstructionProviderClient(Context context) {
        this.context = context;
    }

    synchronized public boolean Start() {
        if (CPClient == null) {
            CPClient = context.getContentResolver()
                    .acquireContentProviderClient(
                            Uri.parse(DBURIConstants.ObstructionBaseURIString));
            return CPClient != null;
        }
        return true;
    }

    synchronized public void Stop() {
        if (CPClient != null)
            CPClient.release();
        CPClient = null;
    }

    synchronized public boolean isStarted() {
        return CPClient != null;
    }

    synchronized public boolean clearAll() {
        if (CPClient == null)
            return false;
        try {
            int deleted = CPClient.delete(
                    Uri.parse(DBURIConstants.LINE_POINT_URI), null, null);
            deleted += CPClient.delete(Uri.parse(DBURIConstants.POINT_URI),
                    null, null);
            deleted += CPClient.delete(Uri.parse(DBURIConstants.LINE_URI),
                    null, null);
            return deleted > 0;
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return false;
    }

    synchronized public boolean UpdateLine(LineObstruction updatedLine,
            boolean UsePoints) {

        if (CPClient == null || updatedLine == null)
            return false;

        // Taxiways shouldn't have height
        if (Constants.isTaxiway(updatedLine.type))
            updatedLine.height = 0;

        ContentValues values = new ContentValues();
        values.put(DBURIConstants.COLUMN_GROUP_NAME_LINE, updatedLine.group);
        values.put(DBURIConstants.COLUMN_UID, updatedLine.uid);
        values.put(DBURIConstants.COLUMN_TYPE, updatedLine.type);
        values.put(DBURIConstants.COLUMN_HEIGHT, updatedLine.height);
        values.put(DBURIConstants.COLUMN_WIDTH, updatedLine.width);
        values.put(DBURIConstants.COLUMN_DESCRIPTION, updatedLine.remarks);
        values.put(DBURIConstants.COLUMN_CLOSED, updatedLine.closed);
        values.put(DBURIConstants.COLUMN_FILLED, updatedLine.filled);
        String WhereClause = DBURIConstants.COLUMN_UID + " = ? and "
                + DBURIConstants.COLUMN_GROUP_NAME_LINE + " = ?";
        int ItemsUpdated = 0;
        try {
            ItemsUpdated = CPClient.update(Uri.parse(DBURIConstants.LINE_URI),
                    values, WhereClause, new String[] {
                            updatedLine.uid, updatedLine.group
                    });
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }

        if (updatedLine.points != null && updatedLine.points.size() > 0
                && UsePoints) {
            try {//delete existing points and add new ones
                CPClient.delete(Uri.parse(DBURIConstants.LINE_POINT_URI),
                        WhereClause, new String[] {
                                updatedLine.uid, updatedLine.group
                        });

                //    ArrayList<ContentProviderOperation> bulkInsertPoints = new ArrayList<ContentProviderOperation>();
                int i = 0;
                for (SurveyPoint currentPoint : updatedLine.points) {
                    ContentValues initialValues = new ContentValues();
                    initialValues.put(DBURIConstants.COLUMN_GROUP_NAME_LINE,
                            updatedLine.group);
                    initialValues.put(DBURIConstants.COLUMN_UID,
                            updatedLine.uid);
                    initialValues.put(DBURIConstants.COLUMN_POINT_ORDER, i);
                    this.FillValuesWithPoint(initialValues, currentPoint);
                    try {
                        CPClient.insert(
                                Uri.parse(DBURIConstants.LINE_POINT_URI),
                                initialValues);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }

                    i++;
                }

                context.getContentResolver().notifyChange(
                        Uri.parse(DBURIConstants.LINE_POINT_URI + "/"
                                + updatedLine.group + "/" + updatedLine.uid
                                + "/alp"), null);
                return ItemsUpdated > 0;
            } catch (RemoteException e) {
                e.printStackTrace();
                return false;
            }
        }
        context.getContentResolver().notifyChange(
                Uri.parse(DBURIConstants.LINE_URI + "/" + updatedLine.group
                        + "/" + updatedLine.uid), null);

        return ItemsUpdated > 0;
    }

    synchronized public Cursor GetAllLinePointsBounded(double LatTop,
            double LatBottom, double LonLeft, double LonRight) {

        if (CPClient == null)
            return null;
        String LatBound = DBURIConstants.COLUMN_LAT + " >= ? and "
                + DBURIConstants.COLUMN_LAT + " <= ?";
        String LonBound = DBURIConstants.COLUMN_LON + " >= ? and "
                + DBURIConstants.COLUMN_LON + " <= ?";
        String WhereClause = LatBound + " and " + LonBound;
        String[] selectionArgs = {
                "" + LatBottom, "" + LatTop, "" + LonLeft, "" + LonRight
        };

        final Cursor linePointCursor;
        try {
            linePointCursor = CPClient.query(
                    Uri.parse(DBURIConstants.LINE_POINT_URI),
                    DBURIConstants.allColumnsLinePoints, WhereClause,
                    selectionArgs, null);
            return linePointCursor;

        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return null;
    }

    synchronized public boolean LineExists(String GroupName, String UID) {

        if (CPClient == null)
            return false;
        String WhereClause = DBURIConstants.COLUMN_UID + " = ? and "
                + DBURIConstants.COLUMN_GROUP_NAME_LINE + " = ?";

        Cursor pointCursor = null;
        try {
            pointCursor = CPClient.query(
                    Uri.parse(DBURIConstants.LINE_POINT_URI),
                    DBURIConstants.allColumnsLinePoints, WhereClause,
                    new String[] {
                            UID, GroupName
                    }, null);

            if (pointCursor != null && pointCursor.getCount() > 0) {
                Cursor lineCursor = CPClient.query(
                        Uri.parse(DBURIConstants.LINE_URI),
                        DBURIConstants.allColumnsLine, WhereClause,
                        new String[] {
                                UID, GroupName
                        }, null);
                if (lineCursor != null) {
                    int count = lineCursor.getCount();
                    lineCursor.close();
                    if (count > 0) {
                        return true;
                    }
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (pointCursor != null)
                pointCursor.close();
        }

        return false;
    }

    //require user to move to first before calling this...
    synchronized public LineObstruction GetLineObstruction(
            Cursor CurrentCursor,
            boolean withPoints) {
        if (CurrentCursor == null || CurrentCursor.getCount() < 1
                || CPClient == null)
            return null;
        LineObstruction newLO = new LineObstruction();

        newLO.group = CurrentCursor.getString(CurrentCursor
                .getColumnIndex(DBURIConstants.COLUMN_GROUP_NAME_LINE));
        newLO.uid = CurrentCursor.getString(CurrentCursor
                .getColumnIndex(DBURIConstants.COLUMN_UID));
        newLO.type = CurrentCursor.getString(CurrentCursor
                .getColumnIndex(DBURIConstants.COLUMN_TYPE));
        newLO.height = CurrentCursor.getFloat(CurrentCursor
                .getColumnIndex(DBURIConstants.COLUMN_HEIGHT));
        newLO.width = CurrentCursor.getFloat(CurrentCursor
                .getColumnIndex(DBURIConstants.COLUMN_WIDTH));
        newLO.remarks = CurrentCursor.getString(CurrentCursor
                .getColumnIndex(DBURIConstants.COLUMN_DESCRIPTION));
        newLO.closed = CurrentCursor.getInt(CurrentCursor
                .getColumnIndex(DBURIConstants.COLUMN_CLOSED)) > 0;
        newLO.filled = CurrentCursor.getInt(CurrentCursor
                .getColumnIndex(DBURIConstants.COLUMN_FILLED)) > 0;
        newLO.flags = CurrentCursor.getInt(CurrentCursor
                .getColumnIndex(DBURIConstants.COLUMN_TOP_COLLECTED));
        //CurrentCursor.close();
        if (withPoints) {
            newLO.points = new ArrayList<SurveyPoint>();
            String[] projection = DBURIConstants.allColumnsLinePoints;
            String WhereClause = DBURIConstants.COLUMN_UID + " = ? and "
                    + DBURIConstants.COLUMN_GROUP_NAME_LINE + " = ?";
            Cursor pointCursor = null;
            try {
                pointCursor = CPClient.query(
                        Uri.parse(DBURIConstants.LINE_POINT_URI), projection,
                        WhereClause, new String[] {
                                newLO.uid, newLO.group
                        }, null);
                if (pointCursor != null) {
                    pointCursor.moveToFirst();
                    for (boolean hasItem = pointCursor.moveToFirst(); hasItem; hasItem = pointCursor
                            .moveToNext()) {
                        newLO.points.add(GetLinePoint(pointCursor));
                    }
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            } finally {
                if (pointCursor != null) {
                    pointCursor.close();
                }
            }
        }
        return newLO;
    }

    public LineObstruction GetLineObstruction(Cursor CurrentCursor) {
        return GetLineObstruction(CurrentCursor, true);
    }

    synchronized public SurveyPoint GetLinePoint(Cursor cur) {
        if (cur == null || cur.getCount() == 0 || CPClient == null)
            return null;
        SurveyPoint newPoint = new SurveyPoint();
        newPoint.order = cur.getInt(3);
        newPoint.lat = cur.getDouble(4);
        newPoint.lon = cur.getDouble(5);
        newPoint.setHAE(cur.getFloat(6));
        newPoint.linearError = cur.getFloat(7);
        newPoint.circularError = cur.getFloat(8);

        int index = cur.getColumnIndex(DBURIConstants.COLUMN_COLLECTION_METHOD);
        if (index != -1)
            newPoint.collectionMethod = CollectionMethod.fromValue(
                    cur.getInt(index));

        return newPoint;
    }

    public PointObstruction GetPointObstruction(Cursor cur) {
        PointObstruction newPO = new PointObstruction();
        if (cur == null || cur.getCount() < 1)
            return null;
        newPO.group = cur.getString(1);
        newPO.uid = cur.getString(2);
        newPO.type = cur.getString(3);
        newPO.lat = cur.getDouble(4);
        newPO.lon = cur.getDouble(5);
        newPO.setHAE(cur.getFloat(6));
        newPO.linearError = cur.getFloat(7);
        newPO.circularError = cur.getFloat(8);
        newPO.height = cur.getFloat(9);
        newPO.width = cur.getFloat(10);
        newPO.length = cur.getFloat(11);
        newPO.remark = cur.getString(12);
        newPO.course_true = cur.getFloat(13);
        newPO.flags = cur.getInt(14);

        int index = cur.getColumnIndex(DBURIConstants.COLUMN_COLLECTION_METHOD);
        if (index != -1)
            newPO.collectionMethod = CollectionMethod.fromValue(
                    cur.getInt(index));

        return newPO;
    }

    synchronized public PointObstruction GetPointObstruction(String Group,
            String UID) {

        if (CPClient == null)
            return null;
        String[] projection = DBURIConstants.allColumnsPoint;
        String WhereClause = DBURIConstants.COLUMN_UID + " = ? and "
                + DBURIConstants.COLUMN_GROUP_NAME_POINT + " = ?";

        Cursor pointCursor = null;
        try {
            pointCursor = CPClient.query(Uri.parse(DBURIConstants.POINT_URI),
                    projection, WhereClause, new String[] {
                            UID, Group
                    }, null);
            if (pointCursor != null) {
                pointCursor.moveToFirst();
                return GetPointObstruction(pointCursor);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        } finally {
            if (pointCursor != null)
                pointCursor.close();
        }
        return null;

    }

    //returns true if add worked out
    synchronized public boolean NewPoint(PointObstruction po) {

        if (CPClient == null)
            return false;
        //delete existing point'
        this.DeletePoint(po.group, po.uid, false);

        if (po.group.equals(ATSKConstants.APRON_GROUP))
            return false;

        ContentValues values = new ContentValues();
        values.put(DBURIConstants.COLUMN_GROUP_NAME_POINT, po.group);
        values.put(DBURIConstants.COLUMN_UID, po.uid);
        values.put(DBURIConstants.COLUMN_TYPE, po.type);
        values.put(DBURIConstants.COLUMN_LAT, po.lat);
        values.put(DBURIConstants.COLUMN_LON, po.lon);
        values.put(DBURIConstants.COLUMN_HAE_M, po.getHAE());
        values.put(DBURIConstants.COLUMN_LE, po.linearError);
        values.put(DBURIConstants.COLUMN_CE, po.circularError);
        values.put(DBURIConstants.COLUMN_HEIGHT, po.height);
        values.put(DBURIConstants.COLUMN_WIDTH, po.width);
        values.put(DBURIConstants.COLUMN_LENGTH, po.length);
        values.put(DBURIConstants.COLUMN_DESCRIPTION, po.remark);
        values.put(DBURIConstants.COLUMN_ROTATION, po.course_true);

        // Top collected nis deprecated, store flags here instead
        values.put(DBURIConstants.COLUMN_TOP_COLLECTED, po.flags);
        values.put(DBURIConstants.COLUMN_COLLECTION_METHOD,
                po.collectionMethod == null ? -1 : po.collectionMethod.value);

        try {
            CPClient.insert(Uri.parse(DBURIConstants.POINT_URI), values);
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
        context.getContentResolver().notifyChange(
                Uri.parse(DBURIConstants.POINT_URI + "/" + po.group
                        + "/" + po.uid), null);
        return true;
    }

    synchronized public boolean DeletePoints(String GroupName) {
        if (CPClient == null)
            return false;
        String WhereClause = DBURIConstants.COLUMN_GROUP_NAME_POINT + " = ?";
        Cursor GroupCursor = null;
        try {
            GroupCursor = CPClient.query(Uri.parse(DBURIConstants.POINT_URI),
                    DBURIConstants.allColumnsPoint, WhereClause, new String[] {
                        GroupName
                    }, null);
            if (GroupCursor == null || GroupCursor.getCount() < 1)
                return false;

            int UIDPosition = GroupCursor
                    .getColumnIndex(DBURIConstants.COLUMN_UID);

            int DeleteCount = 0;
            for (GroupCursor.moveToFirst(); !GroupCursor.isAfterLast(); GroupCursor
                    .moveToNext()) {
                if (this.DeletePoint(GroupName,
                        GroupCursor.getString(UIDPosition),
                        true)) {
                    DeleteCount++;
                }
            }

            return DeleteCount > 0;
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (GroupCursor != null) {
                GroupCursor.close();
            }
        }
    }

    synchronized public boolean DeletePoint(String GroupName, String uid,
            boolean AllowNotifyChange) {
        if (CPClient == null)
            return false;

        String WhereClause = DBURIConstants.COLUMN_UID + " = ? and "
                + DBURIConstants.COLUMN_GROUP_NAME_POINT + " = ?";
        int DeleteCount = 0;
        try {
            DeleteCount = CPClient.delete(Uri.parse(DBURIConstants.POINT_URI),
                    WhereClause, new String[] {
                            uid, GroupName
                    });
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
        if (AllowNotifyChange && DeleteCount > 0) {
            context.getContentResolver().notifyChange(
                    Uri.parse(DBURIConstants.POINT_URI + "/" + GroupName + "/"
                            + uid), null);
        }
        return true;
    }

    public boolean EditPoint(PointObstruction updatePoint) {
        DeletePoint(updatePoint.group, updatePoint.uid, false);
        boolean Success = NewPoint(updatePoint);
        if (Success)
            context.getContentResolver().notifyChange(
                    Uri.parse(DBURIConstants.POINT_URI + "/"
                            + updatePoint.group + "/" + updatePoint.uid),
                    null);
        return Success;
    }

    public boolean NewLine(LineObstruction newLine) {
        return NewLine(newLine, true);
    }

    synchronized public boolean NewLine(LineObstruction newLine,
            boolean AddPoints) {

        if (CPClient == null)
            return false;

        if (newLine.group == null || newLine.group.length() == 0)
            newLine.group = ATSKConstants.DEFAULT_GROUP;
        else if (newLine.group.equals(ATSKConstants.VEHICLE_GROUP))
            return false;

        ContentValues values = new ContentValues();
        values.put(DBURIConstants.COLUMN_GROUP_NAME_LINE, newLine.group);
        values.put(DBURIConstants.COLUMN_UID, newLine.uid);
        values.put(DBURIConstants.COLUMN_TYPE, newLine.type);
        values.put(DBURIConstants.COLUMN_HEIGHT, newLine.height);
        values.put(DBURIConstants.COLUMN_WIDTH, newLine.width);
        values.put(DBURIConstants.COLUMN_DESCRIPTION, newLine.remarks);
        values.put(DBURIConstants.COLUMN_CLOSED, newLine.closed);
        values.put(DBURIConstants.COLUMN_FILLED, newLine.filled);
        values.put(DBURIConstants.COLUMN_TOP_COLLECTED, newLine.flags);

        try {

            String WhereClause = DBURIConstants.COLUMN_UID + " = ? and "
                    + DBURIConstants.COLUMN_GROUP_NAME_LINE + " = ?";

            int LinesDeleted = CPClient.delete(
                    Uri.parse(DBURIConstants.LINE_URI), WhereClause,
                    new String[] {
                            newLine.uid, newLine.group
                    });
            int PointsDeleted = CPClient.delete(
                    Uri.parse(DBURIConstants.LINE_POINT_URI), WhereClause,
                    new String[] {
                            newLine.uid, newLine.group
                    });

            Uri insertedURI = CPClient.insert(
                    Uri.parse(DBURIConstants.LINE_URI), values);
            //    Log.d(TAG, "Delete:"+ LinesDeleted+":"+PointsDeleted+" Added Here:"+insertedURI.toString());

        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
        //LOU we should erase existing points here someday

        //LOU make this a batch someday....
        if (newLine.points != null && newLine.points.size() > 0 && AddPoints) {
            //        ArrayList<ContentProviderOperation> bulkInsertPoints = new ArrayList<ContentProviderOperation>();
            int i = 0;
            for (SurveyPoint sp : newLine.points) {
                ContentValues initialValues = new ContentValues();
                initialValues.put(DBURIConstants.COLUMN_GROUP_NAME_LINE,
                        newLine.group);
                initialValues.put(DBURIConstants.COLUMN_UID, newLine.uid);
                initialValues.put(DBURIConstants.COLUMN_POINT_ORDER, i);
                this.FillValuesWithPoint(initialValues, sp);
                try {
                    CPClient.insert(Uri.parse(DBURIConstants.LINE_POINT_URI),
                            initialValues);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                /*
                Builder newCPOInsert = ContentProviderOperation.newInsert(Uri.parse(DBURIConstants.LINE_POINT_URI));

                values = new ContentValues();
                values.put(DBURIConstants.COLUMN_GROUP_NAME_LINE, newLine.group);
                values.put(DBURIConstants.COLUMN_UID, newLine.uid);
                values.put(DBURIConstants.COLUMN_POINT_ORDER, i);
                newCPOInsert.withValues(values);
                bulkInsertPoints.add(newCPOInsert.build());*/
                i++;
            }
            context.getContentResolver().notifyChange(
                    Uri.parse(DBURIConstants.LINE_POINT_URI + "/"
                            + newLine.group + "/" + newLine.uid + "/alp"),
                    null);
        } else
            context.getContentResolver().notifyChange(
                    Uri.parse(DBURIConstants.LINE_URI + "/" + newLine.group
                            + "/" + newLine.uid), null);
        return true;
    }

    private void FillValuesWithPoint(ContentValues values,
            SurveyPoint currentPoint) {
        FillValuesWithPoint(values, currentPoint, false);
    }

    private void FillValuesWithPoint(ContentValues values,
            SurveyPoint sp, boolean isGradient) {
        values.put(DBURIConstants.COLUMN_LAT, sp.lat);
        values.put(DBURIConstants.COLUMN_LON, sp.lon);
        values.put(DBURIConstants.COLUMN_HAE_M, sp.getHAE());
        values.put(DBURIConstants.COLUMN_LE, sp.circularError);
        values.put(DBURIConstants.COLUMN_CE, sp.linearError);
        if (isGradient) {
            if (sp.collectionMethod != null) {
                values.put(DBURIConstants.COLUMN_RTK,
                        Integer.toString(sp.collectionMethod.value));
            } else {
                values.put(DBURIConstants.COLUMN_RTK,
                        Integer.toString(CollectionMethod.MANUAL.value));
            }
            values.put(DBURIConstants.COLUMN_SHOW, Boolean.toString(true));
        } else
            values.put(DBURIConstants.COLUMN_COLLECTION_METHOD,
                    sp.collectionMethod != null
                            ? sp.collectionMethod.value : -1);
    }

    public boolean DeleteLine(String GroupName, String uid) {
        return DeleteLine(GroupName, uid, true, true);
    }

    synchronized public boolean DeleteLine(String group, String uid,
            boolean deletePoints, boolean notifyChange) {

        if (CPClient == null)
            return false;

        String WhereClause = DBURIConstants.COLUMN_UID + " = ? and "
                + DBURIConstants.COLUMN_GROUP_NAME_LINE + " = ?";
        int LinesDeleted = 0;
        try {
            LinesDeleted = CPClient.delete(Uri.parse(DBURIConstants.LINE_URI),
                    WhereClause, new String[] {
                            uid, group
                    });
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }

        //delete all of the points also
        if (deletePoints) {
            try {
                LinesDeleted += CPClient.delete(
                        Uri.parse(DBURIConstants.LINE_POINT_URI), WhereClause,
                        new String[] {
                                uid, group
                        });
            } catch (RemoteException e) {
                e.printStackTrace();
                return false;
            }
        }

        if (notifyChange) {
            context.getContentResolver()
                    .notifyChange(
                            Uri.parse(DBURIConstants.LINE_URI + "/" + group
                                    + "/" + uid), null);
        }
        return LinesDeleted > 0;
    }

    synchronized public boolean DeleteLine(String group, String uid,
            boolean deletePoints) {
        return DeleteLine(group, uid, deletePoints, true);
    }

    public boolean EditLine(LineObstruction editLine) {
        DeleteLine(editLine.group, editLine.uid, false, false);
        boolean Success = this.NewLine(editLine);
        context.getContentResolver().notifyChange(
                Uri.parse(DBURIConstants.LINE_URI + "/" + editLine.group
                        + "/" + editLine.uid), null);
        return Success;
    }

    synchronized public boolean LineDeleteLastPoint(String GroupName, String uid) {

        if (CPClient == null)
            return false;

        //String[] projection= new String[]{DBURIConstants.COLUMN_ID};
        //String WhereClause = DBURIConstants.COLUMN_UID+" = ? and "+ DBURIConstants.COLUMN_GROUP_NAME_LINE+" = ?";
        int ItemsDeleted = 0;
        try {
            int PointCount = GetPointsInLineCount(GroupName, uid);//countingCursor.getCount();
            if (PointCount > 0) {
                String WhereClause = DBURIConstants.COLUMN_UID + " = ? and "
                        + DBURIConstants.COLUMN_POINT_ORDER + " = ? and "
                        + DBURIConstants.COLUMN_GROUP_NAME_LINE + " = ?";
                ItemsDeleted = CPClient.delete(
                        Uri.parse(DBURIConstants.LINE_POINT_URI), WhereClause,
                        new String[] {
                                uid, String.format("%d", PointCount - 1),
                                GroupName
                        });
                if (PointCount == 1) {
                    context.getContentResolver().notifyChange(
                            Uri.parse(DBURIConstants.LINE_URI + "/" + GroupName
                                    + "/" + uid + "/dlp"), null);
                    return false;
                }
            } else
                return false;
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        //General case
        context.getContentResolver().notifyChange(
                Uri.parse(DBURIConstants.LINE_POINT_URI + "/" + GroupName + "/"
                        + uid + "/dlp"), null);
        context.getContentResolver().notifyChange(
                Uri.parse(DBURIConstants.LINE_URI + "/" + GroupName + "/" + uid
                        + "/dlp"), null);
        return ItemsDeleted > 0;
    }

    synchronized public int GetPointCount(String GroupName) {

        if (CPClient == null)
            return 0;

        String[] projection = new String[] {
                DBURIConstants.COLUMN_ID
        };
        String WhereClause = DBURIConstants.COLUMN_GROUP_NAME_POINT + " = ?";
        Cursor countingCursor;
        int PointCount = 0;
        try {
            countingCursor = CPClient.query(
                    Uri.parse(DBURIConstants.POINT_URI), projection,
                    WhereClause, new String[] {
                        GroupName
                    }, null);
            if (countingCursor != null) {
                PointCount = countingCursor.getCount();
                countingCursor.close();
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return PointCount;
    }

    synchronized public int GetLineCount(String GroupName) {

        if (CPClient == null)
            return 0;

        String[] projection = new String[] {
                DBURIConstants.COLUMN_ID
        };
        String WhereClause = DBURIConstants.COLUMN_GROUP_NAME_LINE + " = ?";
        Cursor countingCursor;
        int PointCount = 0;
        try {
            countingCursor = CPClient.query(Uri.parse(DBURIConstants.LINE_URI),
                    projection, WhereClause, new String[] {
                        GroupName
                    }, null);
            if (countingCursor != null) {
                PointCount = countingCursor.getCount();
                countingCursor.close();
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return PointCount;
    }

    synchronized public int GetPointsInLineCount(String GroupName, String uid) {

        if (CPClient == null)
            return 0;

        String[] projection = new String[] {
                DBURIConstants.COLUMN_ID
        };
        String WhereClause = DBURIConstants.COLUMN_UID + " = ? and "
                + DBURIConstants.COLUMN_GROUP_NAME_LINE + " = ?";
        Cursor countingCursor;
        int PointCount = 0;
        try {
            //Log.e(TAG, "getPointsInLineCount:"+group+" uid:"+ uid+" projection:"+projection+" Where:"+WhereClause );
            countingCursor = CPClient.query(
                    Uri.parse(DBURIConstants.LINE_POINT_URI), projection,
                    WhereClause, new String[] {
                            uid, GroupName
                    }, null);
            if (countingCursor != null) {
                PointCount = countingCursor.getCount();
                countingCursor.close();
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return PointCount;

    }

    synchronized public boolean LineAppendPoint(String GroupName, String uid,
            SurveyPoint newPoint) {

        if (CPClient == null)
            return false;

        if (GroupName == null)
            GroupName = ATSKConstants.DEFAULT_GROUP;
        if (uid == null) {
            Log.e(TAG, "No uid Provided for Appending Point " + newPoint);
            return false;
        }
        int PointCount = GetPointsInLineCount(GroupName, uid);
        try {
            ContentValues initialValues = new ContentValues();
            initialValues.put(DBURIConstants.COLUMN_GROUP_NAME_LINE, GroupName);
            initialValues.put(DBURIConstants.COLUMN_UID, uid);
            initialValues.put(DBURIConstants.COLUMN_POINT_ORDER, PointCount);
            this.FillValuesWithPoint(initialValues, newPoint);

            Uri insertedURI = CPClient.insert(
                    Uri.parse(DBURIConstants.LINE_POINT_URI), initialValues);
            //Log.d(TAG, "Appended here:"+ insertedURI.toString());
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }

        context.getContentResolver().notifyChange(
                Uri.parse(DBURIConstants.LINE_POINT_URI + "/" + GroupName + "/"
                        + uid + "/alp"), null);
        return true;
    }

    synchronized public boolean LineAppendPoints(String GroupName, String uid,
            List<SurveyPoint> newPoints) {

        if (CPClient == null)
            return false;
        ArrayList<ContentProviderOperation> bulkInsertPoints = new ArrayList<ContentProviderOperation>();

        Builder newCPOInsert = ContentProviderOperation.newInsert(Uri
                .parse(DBURIConstants.LINE_POINT_URI));
        ContentValues values;
        values = new ContentValues();
        values.put(DBURIConstants.COLUMN_GROUP_NAME_LINE, GroupName);
        values.put(DBURIConstants.COLUMN_UID, uid);

        int i = this.GetPointsInLineCount(GroupName, uid);
        for (SurveyPoint currentPoint : newPoints) {
            FillValuesWithPoint(values, currentPoint);
            values.put(DBURIConstants.COLUMN_POINT_ORDER, i);
            newCPOInsert.withValues(values);
            bulkInsertPoints.add(newCPOInsert.build());
            i++;
        }

        try {
            CPClient.applyBatch(bulkInsertPoints);
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        } catch (OperationApplicationException e) {
            return false;
        }

        context.getContentResolver().notifyChange(
                Uri.parse(DBURIConstants.LINE_POINT_URI + "/" + GroupName + "/"
                        + uid + "/alp"), null);
        return false;
    }

    synchronized public boolean LineModifyPoint(String GroupName, String uid,
            SurveyPoint newPoint, int Position) {

        if (CPClient == null)
            return false;

        String WhereClause = DBURIConstants.COLUMN_UID + " = ? and "
                + DBURIConstants.COLUMN_POINT_ORDER + " = ? and "
                + DBURIConstants.COLUMN_GROUP_NAME_LINE + " = ?";

        ContentValues values;
        values = new ContentValues();
        values.put(DBURIConstants.COLUMN_GROUP_NAME_LINE, GroupName);
        values.put(DBURIConstants.COLUMN_UID, uid);
        FillValuesWithPoint(values, newPoint);

        try {
            CPClient.update(Uri.parse(DBURIConstants.LINE_POINT_URI), values,
                    WhereClause, new String[] {
                            uid, "" + Position, GroupName
                    });
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
        context.getContentResolver().notifyChange(
                Uri.parse(DBURIConstants.LINE_POINT_URI + "/" + GroupName + "/"
                        + uid), null);
        return true;
    }

    synchronized public Cursor GetAllPoints() {

        if (CPClient == null)
            return null;

        Cursor cursor = null;
        try {
            Uri u = Uri.parse(DBURIConstants.POINT_URI);
            if (u != null)
                cursor = CPClient.query(u,
                        DBURIConstants.allColumnsPoint, null, null, null);

            //    Log.d(TAG, "Inside Got Cursor "+cursor.getCount());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return cursor;
    }

    synchronized public Cursor GetAllPointsBounded(double LatTop,
            double LatBottom,
            double LonLeft, double LonRight, String groupFilter) {

        if (CPClient == null)
            return null;

        String AdditionalFilter = "";
        if (groupFilter != null) {
            AdditionalFilter = " and " + DBURIConstants.COLUMN_GROUP_NAME_POINT
                    + " =?";
        }
        String LatBound = DBURIConstants.COLUMN_LAT + " >= ? and "
                + DBURIConstants.COLUMN_LAT + " <= ?";
        String LonBound = DBURIConstants.COLUMN_LON + " >= ? and "
                + DBURIConstants.COLUMN_LON + " <= ?";
        String WhereClause = LatBound + " and " + LonBound + AdditionalFilter;
        String[] selectionArgs = {
                "" + LatBottom, "" + LatTop, "" + LonLeft, "" + LonRight
        };
        if (groupFilter != null) {
            String[] NextselectionArgs = {
                    "" + LatBottom, "" + LatTop, "" + LonLeft, "" + LonRight,
                    groupFilter
            };
            selectionArgs = NextselectionArgs;
        }

        Cursor pointCursor = null;
        try {
            pointCursor = CPClient.query(Uri.parse(DBURIConstants.POINT_URI),
                    DBURIConstants.allColumnsPoint, WhereClause, selectionArgs,
                    null);
            //        Log.d(TAG, "Inside Got Cursor "+pointCursor.getCount());

        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return pointCursor;
    }

    synchronized public Cursor GetAllPointsCursor(String GroupName) {

        if (CPClient == null)
            return null;

        String WhereClause = DBURIConstants.COLUMN_GROUP_NAME_POINT + " = ?";

        Cursor cursor = null;
        try {
            cursor = CPClient.query(Uri.parse(DBURIConstants.POINT_URI),
                    DBURIConstants.allColumnsPoint, WhereClause, new String[] {
                        GroupName
                    }, null);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return cursor;
    }

    synchronized public Cursor GetAllLines(String GroupName) {

        if (CPClient == null)
            return null;

        Cursor cursor = null;

        String WhereClause = DBURIConstants.COLUMN_GROUP_NAME_LINE + " = ?";
        String[] ClauseFiller = {
                GroupName
        };
        if (GroupName == null || GroupName.length() == 0) {
            WhereClause = null;
            ClauseFiller = null;
        }

        try {
            if (CPClient != null)
                cursor = CPClient.query(Uri.parse(DBURIConstants.LINE_URI),
                        DBURIConstants.allColumnsLine, WhereClause,
                        ClauseFiller, null);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return cursor;
    }

    synchronized public Cursor GetAllLinesBounded(String GroupName,
            double LatTop,
            double LatBottom, double LonLeft, double LonRight) {

        if (CPClient == null)
            return null;

        Cursor cursor = null;

        String LatBound = DBURIConstants.COLUMN_LAT + " >= ? and "
                + DBURIConstants.COLUMN_LAT + " <= ?";
        String LonBound = DBURIConstants.COLUMN_LON + " >= ? and "
                + DBURIConstants.COLUMN_LON + " <= ?";
        String[] selectionArgs = {
                "" + LatBottom, "" + LatTop, "" + LonLeft, "" + LonRight
        };

        String WhereClause = LatBound + " and " + LonBound;
        try {
            Cursor PointUIDs = CPClient.query(
                    Uri.parse(DBURIConstants.LINE_POINT_DISTINCT_URI),
                    new String[] {
                        DBURIConstants.COLUMN_UID
                    }, WhereClause, selectionArgs, null);
            //should give us a list of all UIDs with points inside the bounding box....  
            //now we query for those?

            if (PointUIDs != null) {
                if (PointUIDs.getCount() < 1) {
                    PointUIDs.close();
                    return null;
                }
                String WhereClauseTotal = "", WhereClauseNext;
                ArrayList<String> UIDList = new ArrayList<String>();
                for (int i = 0; i < PointUIDs.getCount(); i++) {

                    PointUIDs.moveToPosition(i);
                    UIDList.add(PointUIDs.getString(0));
                    if (i == 0)
                        WhereClauseNext = DBURIConstants.COLUMN_UID + "= ? ";
                    else
                        WhereClauseNext = "or " + DBURIConstants.COLUMN_UID
                                + "= ? ";

                    WhereClauseTotal = WhereClauseTotal.concat(WhereClauseNext);
                }
                PointUIDs.close();

                //should have an Array of all UIDs and a LONG String of where clauses now
                cursor = CPClient.query(Uri.parse(DBURIConstants.LINE_URI),
                        DBURIConstants.allColumnsLine, WhereClauseTotal,
                        UIDList.toArray(new String[UIDList.size()]), null);
                return cursor;
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        if (cursor != null)
            cursor.close();

        return null;
    }

    public LineObstruction GetLine(String Group, String UID) {
        return GetLine(Group, UID, true);
    }

    synchronized public LineObstruction GetLine(String Group, String UID,
            boolean WithPoints) {
        if (Group == null || UID == null)
            return null;
        String WhereClause = DBURIConstants.COLUMN_UID + " = ? and "
                + DBURIConstants.COLUMN_GROUP_NAME_LINE + " = ?";

        Cursor cursor = null;
        if (CPClient == null)
            return null;
        try {
            cursor = CPClient.query(Uri.parse(DBURIConstants.LINE_URI),
                    DBURIConstants.allColumnsLine, WhereClause, new String[] {
                            UID, Group
                    }, null);
            if (cursor != null) {
                cursor.moveToFirst();
                return GetLineObstruction(cursor, WithPoints);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        } finally {
            if (cursor != null)
                cursor.close();
        }

        return null;
    }

    synchronized public List<PointObstruction> getAllPointObstructions() {
        List<PointObstruction> points = new ArrayList<PointObstruction>();
        if (isStarted()) {
            final Cursor cursor = GetAllPoints();
            if (cursor != null) {
                for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor
                        .moveToNext()) {
                    points.add(GetPointObstruction(cursor));
                }
                cursor.close();
            }
        }
        return points;
    }

    synchronized public List<LineObstruction> getAllLineObstructions(
            String group, boolean withPoints) {
        List<LineObstruction> lines = new ArrayList<LineObstruction>();
        if (isStarted()) {
            final Cursor cursor = GetAllLines(group);
            if (cursor != null) {
                for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor
                        .moveToNext()) {
                    lines.add(GetLineObstruction(cursor, withPoints));
                }
                cursor.close();
            }
        }
        return lines;
    }

    synchronized public List<LineObstruction> getAllLineObstructions(
            boolean withPoints) {
        return getAllLineObstructions(ATSKConstants.DEFAULT_GROUP, withPoints);
    }

    synchronized public List<LineObstruction> getLinesWithPrefix(
            String group, String prefix, boolean withPoints) {
        List<LineObstruction> lines = new ArrayList<LineObstruction>();

        String where = DBURIConstants.COLUMN_UID + " LIKE ? and "
                + DBURIConstants.COLUMN_GROUP_NAME_LINE + " = ?";

        Cursor cursor = null;
        if (CPClient == null)
            return null;
        try {
            cursor = CPClient.query(Uri.parse(DBURIConstants.LINE_URI),
                    DBURIConstants.allColumnsLine, where, new String[] {
                            prefix + "%", group
                    }, null);
            if (cursor != null) {
                while (cursor.moveToNext())
                    lines.add(GetLineObstruction(cursor, withPoints));
            }
        } catch (RemoteException e) {
            Log.e(TAG,
                    "Failed to get all lines with prefix \"" + prefix + "\"", e);
        } finally {
            if (cursor != null)
                cursor.close();
        }

        return lines;
    }
}
