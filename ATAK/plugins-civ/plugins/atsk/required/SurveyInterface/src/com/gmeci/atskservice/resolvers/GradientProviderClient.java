
package com.gmeci.atskservice.resolvers;

import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentProviderOperation.Builder;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.gmeci.conversions.Conversions;
import com.gmeci.core.ATSKConstants;
import com.gmeci.core.LZEdges;
import com.gmeci.core.LineObstruction;
import com.gmeci.core.SurveyPoint;
import com.gmeci.helpers.GradientAnalysisOPCHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GradientProviderClient {

    private static final String TAG = "GradientProviderClient";
    private ContentProviderClient CPClient;
    private final Context context;

    public GradientProviderClient(Context context) {
        this.context = context;
    }

    public boolean EditGradient(LineObstruction editLine) {
        DeleteGradient(editLine.group, editLine.uid, false);
        boolean Success = this.NewGradient(editLine, false);
        context.getContentResolver().notifyChange(
                Uri.parse(DBURIConstants.GRADIENT_URI + "/"
                        + editLine.group + "/" + editLine.uid), null);
        return Success;
    }

    public boolean Start() {
        CPClient = context.getContentResolver().acquireContentProviderClient(
                Uri.parse(DBURIConstants.GradientBaseURIString));
        return CPClient != null;
    }

    public void Stop() {
        if (CPClient != null)
            CPClient.release();
        CPClient = null;
    }

    public Cursor GetGradientPoints(String Group, String UID,
            boolean OrderAscentingRange) {
        String WhereClause = DBURIConstants.COLUMN_UID + " = ?";

        if (CPClient == null) {
            Log.e(TAG,
                    "Accessing CPCLIent without open or after close: GetGradientPoints");
            return null;
        }

        String orderBy = DBURIConstants.COLUMN_RANGE_FROM_START + " ASC";
        if (!OrderAscentingRange) {
            orderBy = null;
        } else {
            Log.d(TAG, "ordering");
        }

        Cursor pointCursor;
        try {
            pointCursor = CPClient.query(
                    Uri.parse(DBURIConstants.GRADIENT_POINT_URI),
                    DBURIConstants.allColumnsGradientPoints, WhereClause,
                    new String[] {
                        UID
                    }, orderBy);
            return pointCursor;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Cursor GetAnalyzedGradients(String SurveyUID,
            boolean BothLongAndTrans) {
        if (CPClient == null) {
            Log.e(TAG,
                    "Accessing CPCLIent without open or after close: GetAnalyzedGradients");
            return null;
        }

        String WhereClause = DBURIConstants.COLUMN_UID + " like ? or "
                + DBURIConstants.COLUMN_UID + " like ?";
        if (!BothLongAndTrans)
            WhereClause = DBURIConstants.COLUMN_UID + " like ?";

        try {

            String[] WhereFillers = {
                    "%"
                            + GradientAnalysisOPCHelper
                                    .GetLongidudinalGradientUID(SurveyUID)
                            + "%",
                    "%" + SurveyUID + "_" + ATSKConstants.TRANSVERSE + "%"
            };
            if (!BothLongAndTrans) {
                String[] WhereFillersTrans = {
                        "%" + SurveyUID + "_" + ATSKConstants.TRANSVERSE + "%"
                };
                WhereFillers = WhereFillersTrans;
            }
            Cursor gradients = CPClient.query(
                    Uri.parse(DBURIConstants.GRADIENT_URI),
                    DBURIConstants.allColumnsGradient, WhereClause,
                    WhereFillers, null);
            return gradients;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

    public int DeleteAnalyzedGradients(String SurveyUID) {
        if (CPClient == null) {
            Log.e(TAG,
                    "Accessing CPCLIent without open or after close: DeleteAnalyzedGradients");
            return -1;
        }
        String WhereClause = DBURIConstants.COLUMN_UID + " like ?";
        try {
            int DeletedPoints = CPClient.delete(
                    Uri.parse(DBURIConstants.GRADIENT_POINT_URI),
                    WhereClause,
                    new String[] {
                        "%"
                                + GradientAnalysisOPCHelper
                                        .GetLongidudinalGradientUID(SurveyUID)
                                + "%"
                    });
            DeletedPoints += CPClient.delete(
                    Uri.parse(DBURIConstants.GRADIENT_URI), WhereClause,
                    new String[] {
                        "%" + SurveyUID + "_" + ATSKConstants.LONGITUDINAL
                                + "%"
                    });
            DeletedPoints += CPClient.delete(
                    Uri.parse(DBURIConstants.GRADIENT_POINT_URI), WhereClause,
                    new String[] {
                        "%" + SurveyUID + "_" + ATSKConstants.TRANSVERSE + "%"
                    });
            DeletedPoints += CPClient.delete(
                    Uri.parse(DBURIConstants.GRADIENT_URI), WhereClause,
                    new String[] {
                        "%" + SurveyUID + "_" + ATSKConstants.TRANSVERSE + "%"
                    });
            return DeletedPoints;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    public int GetGradientHiddenPointCount(String Group, String UID) {
        if (CPClient == null) {
            Log.e(TAG,
                    "Accessing CPCLIent without open or after close: GetGradientHiddenPointCount");
            return -1;
        }

        String[] projection = new String[] {
                DBURIConstants.COLUMN_POINT_ORDER
        };
        String WhereClause = DBURIConstants.COLUMN_UID + " = ? and "
                + DBURIConstants.COLUMN_SHOW + " = ? and "
                + DBURIConstants.COLUMN_GROUP_NAME_LINE + " = ?";
        Cursor pointCursor = null;
        int count = 0;
        try {
            pointCursor = CPClient.query(
                    Uri.parse(DBURIConstants.GRADIENT_POINT_URI), projection,
                    WhereClause, new String[] {
                            UID, Boolean.toString(false), Group
                    }, null);
            count = pointCursor.getCount();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (pointCursor != null)
                pointCursor.close();
        }
        return count;

    }

    public Cursor GetGradientPointsBounded(double LatTop, double LatBottom,
            double LonLeft, double LonRight, String SkipBeginningUID) {
        if (CPClient == null) {
            Log.e(TAG,
                    "Accessing CPCLIent without open or after close: GetGradientPointsBounded");
            return null;
        }

        String LatBound = DBURIConstants.COLUMN_LAT + " >= ? and "
                + DBURIConstants.COLUMN_LAT + " <= ?";
        String LonBound = DBURIConstants.COLUMN_LON + " >= ? and "
                + DBURIConstants.COLUMN_LON + " <= ?";
        String UIDFilter = "";
        String[] selectionArgsLong = {
                "" + LatBottom, "" + LatTop, "" + LonLeft, "" + LonRight,
                "%" + SkipBeginningUID + "%"
        };
        String[] selectionArgsShort = {
                "" + LatBottom, "" + LatTop, "" + LonLeft, "" + LonRight
        };
        String[] selectionArgs;
        if (SkipBeginningUID != null && SkipBeginningUID.length() > 0) {
            selectionArgs = selectionArgsLong;
            UIDFilter = " and " + DBURIConstants.COLUMN_UID + " not like ?";
        } else
            selectionArgs = selectionArgsShort;

        String WhereClause = LatBound + " and " + LonBound + UIDFilter;

        Cursor gradientCursor;
        try {
            gradientCursor = CPClient.query(
                    Uri.parse(DBURIConstants.GRADIENT_POINT_URI),
                    DBURIConstants.allColumnsGradientPoints, WhereClause,
                    selectionArgs, null);

            return gradientCursor;
        } catch (Exception e) {
            Log.e(TAG, "Error during GetGradientPointsBounded:", e);
        }
        return null;
    }

    public boolean GradientModify_NoPoints(GradientDBItem gradient2Update) {

        if (CPClient == null) {
            Log.e(TAG,
                    "Accessing CPCLIent without open or after close: GradientModify_NoPoints");
            return false;
        }

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
                gradient2Update.getGradedGradientL());
        values.put(DBURIConstants.COLUMN_GRADED_GRADIENT_R,
                gradient2Update.getGradedGradientR());
        values.put(DBURIConstants.COLUMN_SHOULDER_GRADIENT_L,
                gradient2Update.getShoulderGradientL());
        values.put(DBURIConstants.COLUMN_SHOULDER_GRADIENT_R,
                gradient2Update.getShoulderGradientR());
        if (gradient2Update.getType().startsWith(
                ATSKConstants.GRADIENT_TYPE_LONGITUDINAL)) {
            values.put(DBURIConstants.COLUMN_LZ_GRADIENT_L,
                    gradient2Update.getLongitudinalIntervalGraident());
            values.put(DBURIConstants.COLUMN_LZ_GRADIENT_R,
                    gradient2Update.getLongitudinalOverallGradient());
        } else {
            values.put(DBURIConstants.COLUMN_LZ_GRADIENT_L,
                    gradient2Update.getLZGradientL());
            values.put(DBURIConstants.COLUMN_LZ_GRADIENT_R,
                    gradient2Update.getLZGradientR());
        }
        values.put(DBURIConstants.COLUMN_MAINTAINED_GRADIENT_L,
                gradient2Update.getMaintainedGradientL());
        values.put(DBURIConstants.COLUMN_MAINTAINED_GRADIENT_R,
                gradient2Update.getMaintainedGradientR());

        int UpdateCount = 0;
        String WhereClause = DBURIConstants.COLUMN_UID + " = ? and "
                + DBURIConstants.COLUMN_GROUP_NAME_LINE + " = ?";
        try {
            UpdateCount = CPClient.update(
                    Uri.parse(DBURIConstants.GRADIENT_URI),
                    values,
                    WhereClause,
                    new String[] {
                            gradient2Update.getUid(),
                            gradient2Update.getGroup()
                    });
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        context.getContentResolver().notifyChange(
                Uri.parse(DBURIConstants.GRADIENT_URI + "/"
                        + gradient2Update.getGroup() + "/"
                        + gradient2Update.getUid()), null);
        return UpdateCount != 0;
    }

    public int GetGradientCount(String GroupName) {
        if (CPClient == null) {
            Log.e(TAG,
                    "Accessing CPCLIent without open or after close: GetGradientCount");
            return 0;
        }

        String[] projection = new String[] {
                DBURIConstants.COLUMN_ID
        };
        String WhereClause = DBURIConstants.COLUMN_GROUP_NAME_LINE + " = ?";
        Cursor countingCursor;
        int PointCount = 0;
        try {
            countingCursor = CPClient.query(
                    Uri.parse(DBURIConstants.GRADIENT_URI), projection,
                    WhereClause, new String[] {
                        GroupName
                    }, null);
            PointCount = countingCursor.getCount();
            countingCursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return PointCount;
    }

    public int GetPointsInGradientCount(String GroupName, String uid) {
        if (CPClient == null) {
            Log.e(TAG,
                    "Accessing CPCLIent without open or after close: GetPointsInGradientCount");
            return 0;
        }
        String[] projection = new String[] {
                DBURIConstants.COLUMN_ID
        };
        String WhereClause = DBURIConstants.COLUMN_UID + " = ? and "
                + DBURIConstants.COLUMN_GROUP_NAME_LINE + " = ?";
        Cursor countingCursor;
        int PointCount = 0;
        try {
            countingCursor = CPClient.query(
                    Uri.parse(DBURIConstants.GRADIENT_POINT_URI), projection,
                    WhereClause, new String[] {
                            uid, GroupName
                    }, null);
            PointCount = countingCursor.getCount();
            countingCursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return PointCount;

    }

    public boolean GradientModifyPoint(String GroupName, String uid,
            SurveyPoint newPoint, int Position) {

        if (CPClient == null) {
            Log.e(TAG,
                    "Accessing CPCLIent without open or after close: GradientModifyPoint");
            return false;
        }

        String WhereClause = DBURIConstants.COLUMN_UID + " = ? and "
                + DBURIConstants.COLUMN_POINT_ORDER + " = ? and "
                + DBURIConstants.COLUMN_GROUP_NAME_LINE + " = ?";

        ContentValues values;
        values = new ContentValues();
        values.put(DBURIConstants.COLUMN_GROUP_NAME_LINE, GroupName);
        values.put(DBURIConstants.COLUMN_UID, uid);
        FillValuesWithPoint(values, newPoint, true);

        try {
            CPClient.update(Uri.parse(DBURIConstants.GRADIENT_POINT_URI),
                    values, WhereClause, new String[] {
                            uid, "" + Position, GroupName
                    });
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        context.getContentResolver().notifyChange(
                Uri.parse(DBURIConstants.GRADIENT_POINT_URI + "/" + GroupName
                        + "/" + uid), null);
        return true;
    }

    public boolean RenameGradient(String OldName, String OldGroup,
            String NewName) {
        if (CPClient == null) {
            Log.e(TAG,
                    "Accessing CPCLIent without open or after close: RenameGradient");
            return false;
        }

        ContentValues values;
        values = new ContentValues();
        values.put(DBURIConstants.COLUMN_DESCRIPTION, NewName);

        String WhereClause = DBURIConstants.COLUMN_UID + " = ? and "
                + DBURIConstants.COLUMN_GROUP_NAME_LINE + " = ?";
        int Updated = 0;
        try {
            Updated = CPClient.update(Uri.parse(DBURIConstants.GRADIENT_URI),
                    values, WhereClause, new String[] {
                            OldName, OldGroup
                    });

            //Updated+= CPClient.update(Uri.parse(DBURIConstants.GRADIENT_POINT_URI), values, WhereClause, new String[]{OldName,OldGroup});
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        context.getContentResolver().notifyChange(
                Uri.parse(DBURIConstants.GRADIENT_URI + "/" + OldGroup + "/"
                        + OldName), null);
        return Updated > 0;
    }

    public boolean DeleteGradient(String GroupName, String uid) {
        return DeleteGradient(GroupName, uid, true);
    }

    public boolean DeleteLongitudinalGradient(String SurveyName) {
        DeleteGradient(ATSKConstants.DEFAULT_GROUP, SurveyName + "_"
                + ATSKConstants.GRADIENT_CURRENT_LON);

        return true;
    }

    public boolean ClearGradientAnalysis(String CurrentSurveyUID) {
        if (CPClient == null) {
            Log.e(TAG,
                    "Accessing CPCLIent without open or after close: ClearGradientAnalysis");
            return false;
        }

        ContentValues values = new ContentValues();
        values.put(DBURIConstants.COLUMN_ANALYZED, "");
        values.put(DBURIConstants.COLUMN_RANGE_FROM_START, "-1");
        values.put(DBURIConstants.COLUMN_INDEX, "0");

        int UpdateCount = 0;
        try {
            UpdateCount = CPClient.update(
                    Uri.parse(DBURIConstants.GRADIENT_POINT_URI), values, null,
                    null);
            Log.d(TAG, "Updated:" + UpdateCount);
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.DeleteLongitudinalGradient(CurrentSurveyUID);

        return UpdateCount > 1;
    }

    public boolean DeleteGradient(String GroupName, String uid,
            boolean DeletePointsAlso) {
        if (CPClient == null) {
            Log.e(TAG,
                    "Accessing CPCLIent without open or after close: DeleteGradient");
            return false;
        }

        int Effected = 0;
        String WhereClause = DBURIConstants.COLUMN_UID + " = ? and "
                + DBURIConstants.COLUMN_GROUP_NAME_LINE + " = ?";
        try {
            Effected = CPClient.delete(Uri.parse(DBURIConstants.GRADIENT_URI),
                    WhereClause, new String[] {
                            uid, GroupName
                    });
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        //delete all of the points also
        if (DeletePointsAlso) {
            try {
                CPClient.delete(Uri.parse(DBURIConstants.GRADIENT_POINT_URI),
                        WhereClause, new String[] {
                                uid, GroupName
                        });
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        context.getContentResolver().notifyChange(
                Uri.parse(DBURIConstants.GRADIENT_URI + "/" + GroupName + "/"
                        + uid), null);
        return Effected > 0;
    }

    public GradientDBItem GetGradient(Cursor CurrentCursor,
            boolean VisiblePointsOnly) {
        return GetGradient(CurrentCursor, true, VisiblePointsOnly);
    }

    public boolean SetGradientPointVisiblitiy(String GroupName, String uid,
            int Position, boolean Visible) {
        String WhereClause = DBURIConstants.COLUMN_UID + " = ? and "
                + DBURIConstants.COLUMN_POINT_ORDER + " = ? and "
                + DBURIConstants.COLUMN_GROUP_NAME_LINE + " = ?";
        if (CPClient == null) {
            Log.e(TAG,
                    "Accessing CPCLIent without open or after close: SetGradientPointVisiblitiy");
            return false;
        }
        ContentValues values;
        values = new ContentValues();
        values.put(DBURIConstants.COLUMN_SHOW, Boolean.toString(Visible));

        try {
            CPClient.update(Uri.parse(DBURIConstants.GRADIENT_POINT_URI),
                    values, WhereClause, new String[] {
                            uid, "" + Position, GroupName
                    });
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean SetGradientAllPointsVisible(String GroupName, String uid) {
        if (CPClient == null) {
            Log.e(TAG,
                    "Accessing CPCLIent without open or after close: SetGradientAllPointsVisible");
            return false;
        }

        String WhereClause = DBURIConstants.COLUMN_UID + " = ?  and "
                + DBURIConstants.COLUMN_GROUP_NAME_LINE + " = ?";

        ContentValues values;
        values = new ContentValues();
        values.put(DBURIConstants.COLUMN_SHOW, Boolean.toString(true));

        try {
            CPClient.update(Uri.parse(DBURIConstants.GRADIENT_POINT_URI),
                    values, WhereClause, new String[] {
                            uid, GroupName
                    });
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean NewGradient(LineObstruction newLine) {
        return NewGradient(newLine, true);
    }

    //deletes any gradient with same name/group before adding
    //returns true if the gradient already existed and was deleted...
    public boolean NewGradient(LineObstruction newGradientLine,
            boolean AddPoints) {
        if (CPClient == null) {
            Log.e(TAG,
                    "Accessing CPCLIent without open or after close: NewGradient");
            return false;
        }

        boolean GradientExisted = this.DeleteGradient(
                newGradientLine.group, newGradientLine.uid, true);

        ContentValues values = new ContentValues();
        values.put(DBURIConstants.COLUMN_GROUP_NAME_LINE,
                newGradientLine.group);
        values.put(DBURIConstants.COLUMN_UID, newGradientLine.uid);
        values.put(DBURIConstants.COLUMN_TYPE, newGradientLine.type);
        values.put(DBURIConstants.COLUMN_DESCRIPTION,
                newGradientLine.remarks);

        try {
            CPClient.insert(Uri.parse(DBURIConstants.GRADIENT_URI), values);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        //LOU we should erase existing points here someday

        if (AddPoints) {
            GradientAppendPoints(newGradientLine.group,
                    newGradientLine.uid, newGradientLine.points);
        }

        context.getContentResolver()
                .notifyChange(
                        Uri.parse(DBURIConstants.GRADIENT_URI + "/"
                                + newGradientLine.group + "/"
                                + newGradientLine.uid), null);
        return GradientExisted;
    }

    public Cursor GetAllGradients(String GroupName,
            boolean ShowAnalyzedGradients) {
        if (CPClient == null)
            return null;
        if (ShowAnalyzedGradients) {
            Log.d(TAG, "Getting all gradients" + GroupName);
            Cursor cursor = null;
            try {
                cursor = CPClient.query(Uri.parse(DBURIConstants.GRADIENT_URI),
                        DBURIConstants.allColumnsGradient, null, null, null);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return cursor;
        } else {
            String WhereClause = DBURIConstants.COLUMN_UID + " not like ? and "
                    + DBURIConstants.COLUMN_UID + " not like ?";
            String[] WhereFillers = {
                    "%_" + ATSKConstants.LONGITUDINAL + "%",
                    "%_" + ATSKConstants.TRANSVERSE + "%"
            };

            Log.d(TAG, "Getting all gradients" + GroupName);
            Cursor cursor = null;
            try {
                cursor = CPClient.query(Uri.parse(DBURIConstants.GRADIENT_URI),
                        DBURIConstants.allColumnsGradient, WhereClause,
                        WhereFillers, null);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return cursor;
        }
    }

    public boolean GradientAppendPoints(String GroupName, String uid,
            List<SurveyPoint> newPoints) {
        if (CPClient == null) {
            Log.e(TAG,
                    "Accessing CPCLIent without open or after close: GradientAppendPoints");
            return false;
        }

        ArrayList<ContentProviderOperation> bulkInsertPoints = new ArrayList<ContentProviderOperation>();
        int i = this.GetPointsInGradientCount(GroupName, uid);
        ContentValues[] valueArray = new ContentValues[newPoints.size()];
        int currentValueIndex = 0;
        for (SurveyPoint currentPoint : newPoints) {
            if (currentPoint.lat > 90 || currentPoint.lat < -90
                    || currentPoint.lon > 180 || currentPoint.lon < -180) {
                Log.d(TAG, "Invalid lat/lon in gradient");

            } else {//good lat lon
                Builder newCPOInsert = ContentProviderOperation.newInsert(Uri
                        .parse(DBURIConstants.GRADIENT_POINT_URI));
                ContentValues values = new ContentValues();
                values.put(DBURIConstants.COLUMN_GROUP_NAME_LINE, GroupName);
                values.put(DBURIConstants.COLUMN_UID, uid);
                FillValuesWithPoint(values, currentPoint, true);
                values.put(DBURIConstants.COLUMN_POINT_ORDER, i);
                values.put(DBURIConstants.COLUMN_SHOW, Boolean.toString(true));
                valueArray[currentValueIndex] = values;
                newCPOInsert.withValues(values);
                bulkInsertPoints.add(newCPOInsert.build());
                i++;
                currentValueIndex++;
            }
        }

        try {
            CPClient.bulkInsert(Uri.parse(DBURIConstants.GRADIENT_POINT_URI),
                    valueArray);
            //    CPClient.applyBatch(bulkInsertPoints);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        Log.e(TAG, "Finished adding to DB");

        context.getContentResolver().notifyChange(
                Uri.parse(DBURIConstants.GRADIENT_POINT_URI + "/" + GroupName
                        + "/" + uid + "/agp"), null);
        return true;
    }

    public void SetGradientEdges(String Group, String UID, LZEdges EdgeGradients) {
        if (CPClient == null) {
            Log.e(TAG,
                    "Accessing CPCLIent without open or after close: SetGradientEdges");
            return;
        }

        String WhereClause = DBURIConstants.COLUMN_UID + " = ? and "
                + DBURIConstants.COLUMN_GROUP_NAME_LINE + " = ?";

        ContentValues values = new ContentValues();
        //LOU hidden gradients?
        if (EdgeGradients.ValidTransverseGradients()) {
            values.put(DBURIConstants.COLUMN_TYPE,
                    ATSKConstants.GRADIENT_TYPE_TRANSVERSE_GOOD);
        } else
            values.put(DBURIConstants.COLUMN_TYPE,
                    ATSKConstants.GRADIENT_TYPE_TRANSVERSE_BAD);

        values.put(DBURIConstants.COLUMN_LZ_GRADIENT_L, ""
                + EdgeGradients.LeftHalfRunwayGradient);
        values.put(DBURIConstants.COLUMN_LZ_GRADIENT_R, ""
                + EdgeGradients.RightHalfRunwayGradient);
        values.put(DBURIConstants.COLUMN_SHOULDER_GRADIENT_L, ""
                + EdgeGradients.LeftShoulderGradient);
        values.put(DBURIConstants.COLUMN_SHOULDER_GRADIENT_R, ""
                + EdgeGradients.RightShoulderGradient);
        values.put(DBURIConstants.COLUMN_GRADED_GRADIENT_L, ""
                + EdgeGradients.LeftGradedAreaGradient);
        values.put(DBURIConstants.COLUMN_GRADED_GRADIENT_R, ""
                + EdgeGradients.RightGradedAreaGradient);
        values.put(DBURIConstants.COLUMN_MAINTAINED_GRADIENT_L, ""
                + EdgeGradients.LeftMaintainedAreaGradient);
        values.put(DBURIConstants.COLUMN_MAINTAINED_GRADIENT_R, ""
                + EdgeGradients.RightMaintainedAreaGradient);

        try {
            CPClient.update(Uri.parse(DBURIConstants.GRADIENT_URI), values,
                    WhereClause, new String[] {
                            UID, Group
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }

        context.getContentResolver()
                .notifyChange(
                        Uri.parse(DBURIConstants.GRADIENT_URI + "/" + Group
                                + "/" + UID), null);

    }

    public GradientDBItem GetGradient(String Group, String UID,
            boolean VisiblePointsOnly) {
        String WhereClause = DBURIConstants.COLUMN_UID + " = ? and "
                + DBURIConstants.COLUMN_GROUP_NAME_LINE + " = ?";

        if (CPClient == null) {
            Log.e(TAG,
                    "Accessing CPCLIent without open or after close: GetGradient");
            return null;
        }

        Cursor cursor = null;
        try {
            cursor = CPClient.query(Uri.parse(DBURIConstants.GRADIENT_URI),
                    DBURIConstants.allColumnsGradient, WhereClause,
                    new String[] {
                            UID, Group
                    }, null);
            //Log.d(TAG, "Items"+ cursor.getCount());
            if (cursor != null) {
                cursor.moveToFirst();
                return GetGradient(cursor, true, VisiblePointsOnly);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null)
                cursor.close();
        }

        return null;
    }

    public GradientDBItem GetGradient(Cursor CurrentCursor, boolean withPoints,
            boolean visibleOnly) {
        if (CurrentCursor == null || CurrentCursor.getCount() == 0)
            return null;

        GradientDBItem gradientItem = new GradientDBItem();

        gradientItem.setGroup(CurrentCursor.getString(1));
        gradientItem.setUid(CurrentCursor.getString(2));
        gradientItem.setType(CurrentCursor.getString(3));
        gradientItem.setRemark(CurrentCursor.getString(4));

        gradientItem.setShoulderGradientL(CurrentCursor.getFloat(6));
        gradientItem.setShoulderGradientR(CurrentCursor.getFloat(7));
        gradientItem.setGradedGradientL(CurrentCursor.getFloat(8));
        gradientItem.setGradedGradientR(CurrentCursor.getFloat(9));
        gradientItem.setMaintainedGradientL(CurrentCursor.getFloat(10));
        gradientItem.setMaintainedGradientR(CurrentCursor.getFloat(11));

        gradientItem.setLZGradientL(CurrentCursor.getFloat(12));
        gradientItem.setLZGradientR(CurrentCursor.getFloat(13));

        gradientItem
                .setLongitudinalIntervalGraident(CurrentCursor.getFloat(12));
        gradientItem
                .setLongitudinalIntervalGraident(CurrentCursor.getFloat(13));
        //newLO = CurrentCursor.getString(5);
        //    CurrentCursor.close();
        if (withPoints)
            getPoints(gradientItem, visibleOnly);
        return gradientItem;
    }

    /**
     * Get all gradients (with points) in a specified bounds
     * This searches for all gradient points within a certain threshold and then
     * gets the associated gradient
     * @param group Gradient group
     */
    public List<GradientDBItem> getFilteredGradients(String group,
            List<SurveyPoint> bounds) {
        double[] b = Conversions.getBounds(bounds);
        String[] projection = DBURIConstants.allColumnsGradientPoints;
        String WhereClause = DBURIConstants.COLUMN_GROUP_NAME_LINE + " = ? and "
                + DBURIConstants.COLUMN_LAT + " >= ? and "
                + DBURIConstants.COLUMN_LON + " >= ? and "
                + DBURIConstants.COLUMN_LAT + " <= ? and "
                + DBURIConstants.COLUMN_LON + " <= ?";
        Map<String, List<SurveyPoint>> gradients =
                new HashMap<String, List<SurveyPoint>>();

        long start = System.currentTimeMillis();
        Cursor cur = null;
        try {
            cur = CPClient.query(Uri.parse(DBURIConstants.GRADIENT_POINT_URI),
                    projection, WhereClause, new String[] { group,
                            String.valueOf(b[0]), String.valueOf(b[1]),
                            String.valueOf(b[2]), String.valueOf(b[3])
                    }, null);
            if (cur != null) {
                int uidCol = cur.getColumnIndex(DBURIConstants.COLUMN_UID);
                for (cur.moveToFirst(); !cur.isAfterLast(); cur.moveToNext()) {
                    String uid = cur.getString(uidCol);
                    List<SurveyPoint> g = gradients.get(uid);
                    if (g == null)
                        g = new ArrayList<SurveyPoint>();
                    g.add(GetGradientPoint(cur));
                    gradients.put(uid, g);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get gradient points", e);
        } finally {
            if (cur != null)
                cur.close();
        }
        Log.d(TAG, "Gradient point filtering took "
                + (System.currentTimeMillis() - start) + "ms");

        start = System.currentTimeMillis();
        List<GradientDBItem> ret = new ArrayList<GradientDBItem>();
        cur = GetAllGradients(group, true);
        if (cur != null) {
            try {
                int uidCol = cur.getColumnIndex(DBURIConstants.COLUMN_UID);
                for (cur.moveToFirst(); !cur.isAfterLast(); cur.moveToNext()) {
                    String uid = cur.getString(uidCol);
                    List<SurveyPoint> points = gradients.get(uid);
                    if (points != null) {
                        GradientDBItem g = GetGradient(cur, false, false);
                        g.setLinePoints(points);
                        ret.add(g);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception retrieving gradients", e);
            } finally {
                cur.close();
            }
        }
        Log.d(TAG, "Get all gradients took "
                + (System.currentTimeMillis() - start) + "ms");

        return ret;
    }

    public void getPoints(GradientDBItem gradient, boolean visibleOnly) {
        gradient.setLinePoints(new ArrayList<SurveyPoint>());
        String[] projection = DBURIConstants.allColumnsGradientPoints;
        String WhereClause = DBURIConstants.COLUMN_UID + " = ? and "
                + DBURIConstants.COLUMN_GROUP_NAME_LINE + " = ?";
        Cursor pCur = null;
        try {
            pCur = CPClient.query(
                    Uri.parse(DBURIConstants.GRADIENT_POINT_URI),
                    projection, WhereClause, new String[] {
                            gradient.getUid(), gradient.getGroup()
                    }, null);
            //Log.d(TAG, "Point Items"+pointCursor.getCount());

            if (pCur != null) {
                for (boolean hasItem = pCur.moveToFirst(); hasItem; hasItem = pCur
                        .moveToNext()) {
                    boolean show = !visibleOnly || Boolean.parseBoolean(
                            pCur.getString(pCur.getColumnIndex(
                                    DBURIConstants.COLUMN_SHOW)));
                    if (show)
                        gradient.getLinePoints().add(
                                GetGradientPoint(pCur));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get gradient points", e);
        } finally {
            if (pCur != null)
                pCur.close();
        }
    }

    private SurveyPoint GetGradientPoint(Cursor cur) {
        if (cur == null || cur.getCount() == 0)
            return null;
        SurveyPoint newPoint = new SurveyPoint();
        newPoint.order = cur.getInt(cur.getColumnIndex(
                DBURIConstants.COLUMN_POINT_ORDER));
        newPoint.lat = cur.getDouble(cur.getColumnIndex(
                DBURIConstants.COLUMN_LAT));
        newPoint.lon = cur.getDouble(cur.getColumnIndex(
                DBURIConstants.COLUMN_LON));
        newPoint.setHAE(cur.getFloat(cur.getColumnIndex(
                DBURIConstants.COLUMN_HAE_M)));
        newPoint.linearError = cur.getFloat(cur.getColumnIndex(
                DBURIConstants.COLUMN_LE));
        newPoint.circularError = cur.getFloat(cur.getColumnIndex(
                DBURIConstants.COLUMN_CE));
        newPoint.visible = Boolean.parseBoolean(cur.getString(
                cur.getColumnIndex(DBURIConstants.COLUMN_SHOW)));

        // Collection method
        int rtkCol = cur.getColumnIndex(DBURIConstants.COLUMN_RTK);
        int rtk = 0;
        if (rtkCol != -1) {
            try {
                rtk = Integer.parseInt(cur.getString(rtkCol));
            } catch (Exception e) {
                try {
                    rtk = Boolean.parseBoolean(cur.getString(rtkCol))
                            ? SurveyPoint.CollectionMethod.RTK.value : 0;
                } catch (Exception e2) {
                    Log.e(TAG, "Failed to parse \"" + DBURIConstants.COLUMN_RTK
                            + "\" for gradient point ("
                            + cur.getString(rtkCol) + "): " + e2.getMessage());
                    rtk = 0;
                }
            }
        } else
            Log.e(TAG, "Missing column for " + DBURIConstants.COLUMN_RTK);
        newPoint.collectionMethod = SurveyPoint.CollectionMethod
                .fromValue(rtk);
        return newPoint;
    }

    public boolean SetGradientAnalysisResults(int ID, double Range,
            String Result, String Index) {

        if (CPClient == null) {
            Log.e(TAG,
                    "Accessing CPCLIent without open or after close: SetGradientAnalysisResults");
            return false;
        }

        ContentValues values = new ContentValues();
        values.put(DBURIConstants.COLUMN_RANGE_FROM_START, Range);
        values.put(DBURIConstants.COLUMN_ANALYZED, Result);
        values.put(DBURIConstants.COLUMN_INDEX, Index);

        String WhereClause = DBURIConstants.COLUMN_ID + " = ?";

        int ItemsUpdated = 0;
        try {
            ItemsUpdated = CPClient.update(
                    Uri.parse(DBURIConstants.GRADIENT_POINT_URI), values,
                    WhereClause, new String[] {
                        "" + ID
                    });
        } catch (Exception e) {
            Log.e(TAG, "Failed to query gradients", e);
            return false;
        }
        return ItemsUpdated > 0;
    }

    public boolean GradientExists(String GroupName, String UID) {
        if (CPClient == null) {
            Log.e(TAG,
                    "Accessing CPCLIent without open or after close: GradientExists");
            return false;
        }

        String[] projection = DBURIConstants.allColumnsGradient;
        String WhereClause = DBURIConstants.COLUMN_UID + " = ? and "
                + DBURIConstants.COLUMN_GROUP_NAME_LINE + " = ?";

        Cursor gradientCursor = null;
        try {
            gradientCursor = CPClient.query(
                    Uri.parse(DBURIConstants.GRADIENT_URI), projection,
                    WhereClause, new String[] {
                            UID, GroupName
                    }, null);

            return (gradientCursor.getCount() > 0);
        } catch (Exception e) {
            Log.e(TAG, "Failed to query gradients", e);
            return false;
        } finally {
            if (gradientCursor != null)
                gradientCursor.close();
        }

    }

    /**
     * Get a simplified line obstruction for the filtered longitudinal
     * @param minPointRange Min range between 2 points to consider them equal
     * @return
     */
    public LineObstruction getLongitudinalLine(double minPointRange) {
        if (CPClient == null) {
            Log.e(TAG,
                    "Accessing CPCLIent without open or after close: getLongitudinalLine");
            return null;
        }

        LineObstruction GradientLine = new LineObstruction();

        String WhereClause = DBURIConstants.COLUMN_RANGE_FROM_START + " > ?";
        String orderBy = DBURIConstants.COLUMN_RANGE_FROM_START + " ASC";
        Cursor cursor = null;
        try {
            cursor = CPClient.query(
                    Uri.parse(DBURIConstants.GRADIENT_POINT_URI),
                    DBURIConstants.allColumnsGradientPoints, WhereClause,
                    new String[] {
                        "0"
                    }, orderBy);

            if (cursor != null) {
                int LastRange = (int) (-1 * minPointRange);
                int RangeColumn = cursor.getColumnIndex(
                        DBURIConstants.COLUMN_RANGE_FROM_START);
                for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor
                        .moveToNext()) {
                    double Range = cursor.getFloat(RangeColumn);
                    if (Range - minPointRange - LastRange >= 0) {
                        LastRange = (int) Range;
                        SurveyPoint newGradientPoint = GradientDBItem
                                .cursorToSurveyPoint(cursor);
                        GradientLine.points.add(newGradientPoint);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to query gradients", e);
        } finally {
            if (cursor != null)
                cursor.close();
        }

        return GradientLine;
    }

    public boolean SetGradientType(String GroupName, String UID, String Type) {
        if (CPClient == null) {
            Log.e(TAG,
                    "Accessing CPCLIent without open or after close: SetGradientType");
            return false;
        }

        String WhereClause = DBURIConstants.COLUMN_UID + " = ? and "
                + DBURIConstants.COLUMN_GROUP_NAME_LINE + " = ?";

        ContentValues values = new ContentValues();
        //LOU hidden gradients?

        values.put(DBURIConstants.COLUMN_TYPE, Type);

        int ItemsUpdated = 0;
        try {
            ItemsUpdated = CPClient.update(
                    Uri.parse(DBURIConstants.GRADIENT_URI), values,
                    WhereClause, new String[] {
                            UID, GroupName
                    });
        } catch (Exception e) {
            Log.e(TAG, "Failed to query gradients", e);
            return false;
        }

        context.getContentResolver().notifyChange(
                Uri.parse(DBURIConstants.GRADIENT_URI + "/" + GroupName + "/"
                        + UID), null);
        return ItemsUpdated > 0;

    }

    private void FillValuesWithPoint(ContentValues values,
            SurveyPoint currentPoint, boolean isGradient) {
        values.put(DBURIConstants.COLUMN_LAT, currentPoint.lat);
        values.put(DBURIConstants.COLUMN_LON, currentPoint.lon);
        values.put(DBURIConstants.COLUMN_HAE_M, currentPoint.getHAE());
        values.put(DBURIConstants.COLUMN_LE, currentPoint.circularError);
        values.put(DBURIConstants.COLUMN_CE, currentPoint.linearError);
        if (isGradient) {
            if (currentPoint.collectionMethod != null) {
                values.put(DBURIConstants.COLUMN_RTK,
                        Integer.toString(currentPoint.collectionMethod
                                .value));
            } else {
                values.put(DBURIConstants.COLUMN_RTK,
                        Integer.toString(SurveyPoint.CollectionMethod.MANUAL
                                .value));
            }
            values.put(DBURIConstants.COLUMN_SHOW, Boolean.toString(true));
        }
    }

    public boolean GradientAppendPoint(String GroupName, String uid,
            SurveyPoint newPoint) {
        if (CPClient == null) {
            Log.e(TAG,
                    "Accessing CPCLIent without open or after close: GradientAppendPoint");
            return false;
        }

        int PointCount = GetPointsInGradientCount(GroupName, uid);
        try {
            ContentValues initialValues = new ContentValues();

            initialValues.put(DBURIConstants.COLUMN_GROUP_NAME_LINE, GroupName);
            initialValues.put(DBURIConstants.COLUMN_UID, uid);
            initialValues.put(DBURIConstants.COLUMN_POINT_ORDER, PointCount);
            this.FillValuesWithPoint(initialValues, newPoint, true);

            CPClient.insert(Uri.parse(DBURIConstants.GRADIENT_POINT_URI),
                    initialValues);
        } catch (Exception e) {
            Log.e(TAG, "Failed to insert gradients", e);
            return false;
        }

        context.getContentResolver().notifyChange(
                Uri.parse(DBURIConstants.GRADIENT_POINT_URI + "/" + GroupName
                        + "/" + uid + "/agp"), null);
        return true;
    }

    public String GetGradientType(String UID) {

        if (CPClient == null) {
            Log.e(TAG,
                    "Accessing CPCLIent without open or after close: GetGradientType");
            return "";
        }

        String WhereClause = DBURIConstants.COLUMN_UID + " = ?";
        Cursor cursor = null;
        try {
            cursor = CPClient.query(Uri.parse(DBURIConstants.GRADIENT_URI),
                    new String[] {
                        DBURIConstants.COLUMN_TYPE
                    }, WhereClause, new String[] {
                        UID
                    }, null);
            Log.d(TAG, "Items" + cursor.getCount());
            if (cursor.getCount() <= 0)
                return "";
            cursor.moveToFirst();
            return cursor.getString(0);
        } catch (Exception e) {
            Log.e(TAG, "Failed to query gradients", e);
        } finally {
            if (cursor != null)
                cursor.close();
        }

        return "";
    }

}
