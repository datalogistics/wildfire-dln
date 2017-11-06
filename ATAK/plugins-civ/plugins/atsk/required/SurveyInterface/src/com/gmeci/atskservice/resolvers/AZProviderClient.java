
package com.gmeci.atskservice.resolvers;

import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.util.Log;

import com.gmeci.conversions.Conversions;
import com.gmeci.core.ATSKConstants;
import com.gmeci.core.SurveyData;
import com.gmeci.core.SurveyPoint;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;

public class AZProviderClient {

    private static final String TAG = "AZProviderClient";
    ContentProviderClient CPClient;
    final Context context;

    static BroadcastImpl bimpl = new BroadcastImpl() {
        public void sendBroadcast(final Context context, final Intent intent) {
            if (context != null)
                context.sendBroadcast(intent);
        }
    };

    public interface BroadcastImpl {
        public void sendBroadcast(final Context context, final Intent intent);
    }

    public AZProviderClient(final Context context) {
        this.context = context;
    }

    static public void setBroadcastImpl(BroadcastImpl newBimpl) {
        bimpl = newBimpl;
    }

    synchronized public boolean Start() {
        if (CPClient == null) {
            CPClient = context.getContentResolver()
                    .acquireContentProviderClient(
                            Uri.parse(AZURIConstants.BaseURIString));
            if (CPClient == null)
                return false;
            else {
                return true;
            }
        }
        return false;
    }

    synchronized public void Stop() {
        if (CPClient != null)
            CPClient.release();
        CPClient = null;
    }

    synchronized public boolean isStarted() {
        return CPClient != null;
    }

    public String getSetting(String SettingName, String Source) {
        Cursor Results = null;
        if (CPClient == null) {
            Log.e(TAG, "AZProviderClient not started.", new Exception());
            if (!Start())
                return "";
        }
        String WhereClause = AZURIConstants.COLUMN_NAME + " = ? ";

        try {
            Results = CPClient.query(Uri.parse(AZURIConstants.AZ_SETTING_URI),
                    AZURIConstants.allColumnsAZSettings, WhereClause,
                    new String[] {
                        SettingName
                    }, null);

            if (Results != null && Results.getCount() > 0) {
                Results.moveToFirst();
                if (!Results.getString(2).equals(""))
                    return Results.getString(2);
            }
            if (SettingName.equals(ATSKConstants.CURRENT_SURVEY)) {
                String DEFAULT_NAME = "default";
                putSetting(SettingName, DEFAULT_NAME, "fromGet" + SettingName
                        + " requestor:" + Source, true);
                return DEFAULT_NAME;
            }

        } catch (RemoteException e) {
            e.printStackTrace();
        } finally {
            if (Results != null)
                Results.close();
        }
        return "";
    }

    public boolean putSetting(String SettingName, String SettingValue,
            String ChangeRequestor, boolean IgnoreChange) {
        String OldValue = "";
        if (CPClient == null) {
            Log.e(TAG, "AZProviderClient not started.", new Exception());
            if (!Start())
                return false;
        }

        if (!IgnoreChange && SettingName.equals(ATSKConstants.CURRENT_SURVEY)) {
            Log.d("putSetting", ChangeRequestor);
            OldValue = this.getSetting(ATSKConstants.CURRENT_SURVEY,
                    ChangeRequestor);
        }

        ContentValues values = new ContentValues();
        values.put(AZURIConstants.COLUMN_VALUE, SettingValue);
        values.put(AZURIConstants.COLUMN_NAME, SettingName);
        int UpdatedItems = 0;
        String WhereClause = AZURIConstants.COLUMN_NAME + " = ? ";
        try {
            UpdatedItems = CPClient.update(
                    Uri.parse(AZURIConstants.AZ_SETTING_URI), values,
                    WhereClause, new String[] {
                        SettingName
                    });
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        context.getContentResolver().notifyChange(
                Uri.parse(AZURIConstants.AZ_SETTING_URI + "/" + SettingName),
                null);

        if (!IgnoreChange && SettingName.equals(ATSKConstants.CURRENT_SURVEY)
                && !OldValue.equals(ATSKConstants.CURRENT_SURVEY)) {
            Intent CSChangeIntent = new Intent(
                    ATSKConstants.CURRENT_SURVEY_CHANGE_ACTION);
            CSChangeIntent.putExtra(ATSKConstants.UID_EXTRA, SettingValue);
            bimpl.sendBroadcast(context, CSChangeIntent);
        }
        return UpdatedItems > 0;
    }

    public boolean putSetting(String SettingName, String SettingValue,
            String ChangeRequestor) {
        return putSetting(SettingName, SettingValue, ChangeRequestor, false);
    }

    //info
    public boolean setSurveyInfo(String surveyUID, String info,
            boolean allowMapUpdate) {
        SurveyData CurrentSurvey = getAZ(surveyUID, true);
        if (CurrentSurvey != null) {
            CurrentSurvey.info = info;
            UpdateAZ(CurrentSurvey, AZURIConstants.AZ_INFORMATION_UPDATED,
                    allowMapUpdate);
        }
        return true;
    }

    public boolean setSurveyScreenShotList(String surveyUID,
            ArrayList<String> NewSurveyList) {
        SurveyData CurrentSurvey = getAZ(surveyUID, true);
        if (CurrentSurvey != null) {
            CurrentSurvey.screenShotFileNameList = NewSurveyList;
            return UpdateAZ(CurrentSurvey,
                    AZURIConstants.AZ_SCREENSHOTS_UPDATED,
                    false);
        }
        return false;
    }

    public boolean setAZParameters(String surveyUID, double length_m,
            double width_m, double angle_degtrue, SurveyData.AZ_TYPE aztype,
            boolean allowMapUpdate) {
        if (CPClient == null) {
            Log.e(TAG, "AZProviderClient not started.", new Exception());
            if (!Start())
                return false;
        }
        //String WhereClause = DBURIConstants.COLUMN_UID+" = ? ";
        int deleteCount = -1;
        try {
            deleteCount = CPClient.delete(
                    Uri.parse(AZURIConstants.SINGLE_AZ_URI), null, null);
        } catch (RemoteException e) {
            e.printStackTrace();
            return deleteCount > 0;
        }
        if (deleteCount > 0)
            context.getContentResolver()
                    .notifyChange(
                            Uri.parse(AZURIConstants.SINGLE_AZ_URI + "/"
                                    + "all"), null);
        return deleteCount > 0;
    }

    public String getSurveyUID(Cursor cursor) {
        if (cursor == null || cursor.getCount() < 1)
            // absolutely the very first survey in the system.
            // FIXME:  do not require a first survey
            return java.util.UUID.randomUUID().toString();
        return cursor.getString(cursor
                .getColumnIndex(AZURIConstants.COLUMN_UID));

    }

    public SurveyData getAZ(String UID, boolean AddIfMissing) {
        if (UID == null)
            return new SurveyData();
        Cursor cursor = null;

        if (CPClient == null) {
            Log.e(TAG, "AZProviderClient not started.", new Exception());
            if (!Start()) {
                if (AddIfMissing) {
                    SurveyData sd = new SurveyData();
                    sd.uid = UID;
                    return sd;
                }
                return null;
            }
        }

        String WhereClause = DBURIConstants.COLUMN_UID + " = ? ";
        try {
            cursor = CPClient.query(Uri.parse(AZURIConstants.SINGLE_AZ_URI),
                    AZURIConstants.allColumnsAZ, WhereClause, new String[] {
                        UID
                    }, null);
            if (cursor != null)
                cursor.moveToFirst();

        } catch (RemoteException e) {
            e.printStackTrace();
        }

        SurveyData retval = null;
        if (cursor == null || cursor.getCount() < 1 && AddIfMissing) {
            retval = new SurveyData();
            retval.center = new SurveyPoint(35.145759, -78.999133);
            retval.uid = UID;
            retval.setSurveyName("default");
            this.NewAZ(retval);
        } else {
            retval = getAZ(cursor);
        }

        if (cursor != null)
            cursor.close();

        return retval;
    }

    public boolean isVisible(String UID) {
        Cursor cursor = null;

        if (CPClient == null) {
            Log.e(TAG, "AZProviderClient not started.", new Exception());
            if (!Start())
                return false;
        }

        String WhereClause = DBURIConstants.COLUMN_UID + " = ? ";
        try {

            String[] typeOnly = {
                    AZURIConstants.COLUMN_AZ_VISIBLE
            };

            if (CPClient == null)
                return false;
            cursor = CPClient.query(Uri.parse(AZURIConstants.SINGLE_AZ_URI),
                    typeOnly, WhereClause, new String[] {
                        UID
                    }, null);
            if (cursor != null && cursor.getCount() > 0) {
                cursor.moveToFirst();
                return Boolean.parseBoolean(cursor.getString(0));
            } else {
                return false;
            }
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (cursor != null)
                cursor.close();
        }
    }

    public String getAZName(final String UID) {
        String DefaultName = "default";
        Cursor cursor = null;
        if (CPClient == null) {
            Log.e(TAG, "AZProviderClient not started.", new Exception());
            if (!Start())
                return DefaultName;
        }

        String WhereClause = DBURIConstants.COLUMN_UID + " = ? ";
        try {

            String[] typeOnly = {
                    AZURIConstants.COLUMN_NAME
            };

            cursor = CPClient.query(Uri.parse(AZURIConstants.SINGLE_AZ_URI),
                    typeOnly, WhereClause, new String[] {
                        UID
                    }, null);
            if (cursor != null) {
                cursor.moveToFirst();
                return cursor.getString(0);
            } else {
                return DefaultName;
            }
        } catch (RemoteException e) {
            e.printStackTrace();
            return DefaultName;
        } finally {
            if (cursor != null)
                cursor.close();
        }

    }

    public void renameAZ(String UID, String newName) {
        SurveyData currentSurvey = this.getAZ(UID, false);
        if (newName == null)
            newName = "default";
        if (currentSurvey == null)
            return;
        currentSurvey.setSurveyName(newName);
        UpdateAZ(currentSurvey, AZURIConstants.AZ_PARAMETERS_UPDATED, true);
    }

    //require user to move to first before calling this...
    public SurveyData getAZ(Cursor CurrentCursor) {
        if (CurrentCursor == null || CurrentCursor.getCount() < 1) {

            return null;
        }
        String WholeClassString = CurrentCursor
                .getString(AZURIConstants.JSON_INDEX);

        // Legacy support
        WholeClassString = legacyConversion(WholeClassString);

        SurveyData newSurveyItem = null;
        try {
            newSurveyItem = Conversions.fromJson(WholeClassString,
                    SurveyData.class);
        } catch (Exception e) {
            Log.e(TAG,
                    "Failed to convert JSON string back to SurveyData object",
                    e);
            return null;
        }
        if (newSurveyItem != null) {
            if (newSurveyItem.FAMPoints[0] == null)
                newSurveyItem.FAMPoints[0] = new SurveyPoint();
            if (newSurveyItem.FAMPoints[1] == null)
                newSurveyItem.FAMPoints[1] = new SurveyPoint();
        }
        return newSurveyItem;
    }

    @SuppressWarnings("unused")
    public boolean NewAZ(SurveyData newSurvey) {
        //delete existing point'
        this.deleteAZ(newSurvey.uid);
        if (CPClient == null) {
            Log.e(TAG, "AZProviderClient not started.", new Exception());
            if (!Start())
                return false;
        }

        ContentValues values = new ContentValues();
        FillValues(values, newSurvey);
        try {
            Uri UpdatedURI = CPClient.insert(
                    Uri.parse(AZURIConstants.SINGLE_AZ_URI), values);
            Log.d(TAG, UpdatedURI.toString());
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
        context.getContentResolver().notifyChange(
                Uri.parse(AZURIConstants.SINGLE_AZ_URI + "/" + newSurvey.uid),
                null);

        return true;

    }

    /**
     * Sanitize survey parameters to prevent database exceptions
     * @param survey Survey data object
     */
    private void sanitizeSurvey(SurveyData survey) {
        if (survey.center == null) {
            survey.center = new SurveyPoint();
            Log.w(TAG, "Survey has null center point", new Throwable());
        }
        if (survey.center.lat < -90 || survey.center.lat > 90) {
            Log.w(TAG, "Survey has invalid latitude: "
                    + survey.center.lat, new Throwable());
            survey.center.lat = 0;
        }
        if (survey.center.lon < -180 || survey.center.lon > 180) {
            Log.w(TAG, "Survey has invalid longitude: "
                    + survey.center.lon, new Throwable());
            survey.center.lon = 0;
        }
        if (survey.width < 0) {
            Log.w(TAG, "Survey has negative width: "
                    + survey.width, new Throwable());
            survey.setWidth(Math.abs(survey.width));
        }
        if (survey.getLength() < 0) {
            Log.w(TAG, "Survey has negative length: "
                    + survey.getLength(), new Throwable());
            survey.setLength(Math.abs(survey.getLength()));
        }
        survey.angle = Conversions.deg360(survey.angle);
    }

    boolean FillValues(ContentValues values, SurveyData newSurvey) {
        //This is where we actually set the surveys values
        if (newSurvey == null)
            return false;
        sanitizeSurvey(newSurvey);
        values.put(AZURIConstants.COLUMN_UID, newSurvey.uid);
        values.put(AZURIConstants.COLUMN_NAME, newSurvey.getSurveyName());
        values.put(AZURIConstants.COLUMN_TYPE, newSurvey.getType().name());
        values.put(AZURIConstants.COLUMN_LAT, newSurvey.center.lat);
        values.put(AZURIConstants.COLUMN_LON, newSurvey.center.lon);
        values.put(AZURIConstants.COLUMN_HAE_M, newSurvey.center.getHAE());
        values.put(AZURIConstants.COLUMN_ROTATION, newSurvey.angle);
        values.put(AZURIConstants.COLUMN_WIDTH_M, newSurvey.width);
        values.put(AZURIConstants.COLUMN_LENGTH_M, newSurvey.getLength());

        values.put(AZURIConstants.COLUMN_AZ_VISIBLE,
                Boolean.toString(newSurvey.visible));
        String SurveyPackageString = Conversions.toJson(newSurvey);
        values.put(AZURIConstants.COLUMN_AZ_JSON, SurveyPackageString);
        return true;
    }

    public boolean UpdateAZ(SurveyData newSurvey, String URI_Decoration,
            boolean allowMapUpdate) {
        if (newSurvey == null) {
            Log.w(TAG, "Attempted to update null survey", new Exception());
            return false;
        }

        int UpdatedItems = 0;
        ContentValues values = new ContentValues();

        if (CPClient == null) {
            Log.e(TAG, "AZProviderClient not started.", new Exception());
            if (!Start())
                return false;
        }

        FillValues(values, newSurvey);
        try {
            String WhereClause = DBURIConstants.COLUMN_UID + " = ? ";
            UpdatedItems = CPClient.update(
                    Uri.parse(AZURIConstants.SINGLE_AZ_URI), values,
                    WhereClause, new String[] {
                        newSurvey.uid
                    });

            if (UpdatedItems < 1) {
                if (this.NewAZ(newSurvey))
                    return true;
            }
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
        if (allowMapUpdate)
            context.getContentResolver().notifyChange(
                    Uri.parse(AZURIConstants.SINGLE_AZ_URI + "/"
                            + newSurvey.uid), null);
        return UpdatedItems > 0;
    }

    public int deleteAZ(String uID) {
        String WhereClause = DBURIConstants.COLUMN_UID + " = ? ";

        if (CPClient == null) {
            Log.e(TAG, "AZProviderClient not started.", new Exception());
            if (!Start())
                return -1;
        }

        int deleteCount = -1;
        try {
            deleteCount = CPClient.delete(
                    Uri.parse(AZURIConstants.SINGLE_AZ_URI), WhereClause,
                    new String[] {
                        uID
                    });
        } catch (RemoteException e) {
            e.printStackTrace();
            return deleteCount;
        }
        if (deleteCount > 0)
            context.getContentResolver().notifyChange(
                    Uri.parse(AZURIConstants.SINGLE_AZ_URI + "/" + uID), null);
        return deleteCount;
    }

    public Cursor getAllSurveys() {
        return getAllSurveys(true);
    }

    public Cursor getAllSurveysBounded(double LatTop, double LatBottom,
            double LonLeft, double LonRight) {
        Cursor Results = null;
        if (CPClient == null) {
            Log.e(TAG, "AZProviderClient not started.", new Exception());
            if (!Start())
                return null;
        }

        String LatBound = DBURIConstants.COLUMN_LAT + " >= ? and "
                + DBURIConstants.COLUMN_LAT + " <= ?";
        String LonBound = DBURIConstants.COLUMN_LON + " >= ? and "
                + DBURIConstants.COLUMN_LON + " <= ?";
        String[] selectionArgs = {
                "" + LatBottom, "" + LatTop, "" + LonLeft, "" + LonRight
        };

        String WhereClause = LatBound + " and " + LonBound;
        try {
            Results = CPClient.query(Uri.parse(AZURIConstants.SINGLE_AZ_URI),
                    AZURIConstants.allColumnsAZ, WhereClause, selectionArgs,
                    null);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return Results;
    }

    public Cursor getAllSurveys(boolean AllFields) {
        Cursor Results = null;
        if (CPClient == null) {
            Log.e(TAG, "AZProviderClient not started.", new Exception());
            if (!Start())
                return null;
        }

        try {
            Uri u = Uri.parse(AZURIConstants.SINGLE_AZ_URI);
            if (u != null) {
                if (AllFields) {
                    Results = CPClient.query(u,
                            AZURIConstants.allColumnsAZ, null, null, null);
                } else {
                    Results = CPClient.query(u,
                            AZURIConstants.uidOnlyColumnsAZ, null, null, null);
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return Results;
    }

    public boolean setVisibility(String aZUID, boolean checked) {

        ContentValues values = new ContentValues();
        values.put(AZURIConstants.COLUMN_AZ_VISIBLE, Boolean.toString(checked));

        if (CPClient == null) {
            Log.e(TAG, "AZProviderClient not started.", new Exception());
            if (!Start())
                return false;
        }

        String WhereClause = DBURIConstants.COLUMN_UID + " = ?";

        int ItemsUpdated = 0;
        try {
            ItemsUpdated = CPClient.update(
                    Uri.parse(AZURIConstants.SINGLE_AZ_URI), values,
                    WhereClause, new String[] {
                        aZUID
                    });
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
        context.getContentResolver().notifyChange(
                Uri.parse(AZURIConstants.SINGLE_AZ_URI + "/" + aZUID), null);
        return ItemsUpdated > 0;

    }

    public boolean clearAll() {
        //String WhereClause = DBURIConstants.COLUMN_UID+" = ? ";
        int deleteCount = -1;
        try {
            deleteCount = CPClient.delete(
                    Uri.parse(AZURIConstants.SINGLE_AZ_URI), null, null);
        } catch (RemoteException e) {
            e.printStackTrace();
            return deleteCount > 0;
        }
        if (deleteCount > 0)
            context.getContentResolver()
                    .notifyChange(
                            Uri.parse(AZURIConstants.SINGLE_AZ_URI + "/"
                                    + "all"), null);
        return deleteCount > 0;
    }

    // Recursively convert all 'double' altitude fields to 'Altitude' objects
    // within a JSON class string
    public static JSONObject legacyConversion(JSONObject input) {
        Iterator keys = input.keys();
        GsonBuilder gb = new GsonBuilder();
        gb.serializeSpecialFloatingPointValues();
        Gson gs = gb.create();
        while (keys.hasNext()) {
            try {
                String key = String.valueOf(keys.next());
                Object field = input.get(key);
                if (field == null)
                    continue;
                if (field instanceof JSONObject) {
                    // Already converted
                    if (key.equals("alt"))
                        continue;

                    JSONObject objField = (JSONObject) field;
                    legacyConversion(objField);
                    input.put(key, objField);
                } else if (field instanceof JSONArray) {
                    JSONArray fieldArr = (JSONArray) field;
                    for (int i = 0; i < fieldArr.length(); i++) {
                        if (fieldArr.get(i) instanceof JSONObject) {
                            JSONObject objField = (JSONObject) fieldArr.get(i);
                            legacyConversion(objField);
                            fieldArr.put(i, objField);
                        }
                    }
                    input.put(key, fieldArr);
                } else if (key.equals("alt")) {
                    double alt = input.getDouble("alt");
                    if (alt == 0.0f)
                        alt = SurveyPoint.Altitude.INVALID;
                    SurveyPoint.Altitude altitude = new SurveyPoint.Altitude(
                            alt,
                            SurveyPoint.AltitudeRef.HAE);
                    input.put("alt", new JSONObject(gs.toJson(altitude)));
                }
            } catch (JSONException e) {
            }
        }
        return input;
    }

    public static String legacyConversion(String classString) {
        /*
        // Write out JSON to file (only useful when loading from database)
        File out = new File(Environment.getExternalStorageDirectory(), "debug_survey_import.json");
        FileWriter fw = null;
        try {
            fw = new FileWriter(out);
            fw.write(classString);
        } catch(Exception e) {
        } finally {
            if(fw != null)
                try {
                    fw.close();
                } catch(IOException e) {}
        }*/

        try {
            JSONObject classObj = new JSONObject(classString);
            return legacyConversion(classObj).toString();
        } catch (Exception e) {
            Log.w(TAG, "Failed to parse class string as JSON object.", e);
        }
        return classString;
    }
}
