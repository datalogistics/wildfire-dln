
package com.gmeci.atsk.gradient;

/**
 * Created by smetana on 5/28/2014.
 */

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.atakmap.android.ipc.AtakBroadcast.DocumentedIntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;

import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.log.Log;
import com.gmeci.core.ATSKConstants;
import com.gmeci.atskservice.resolvers.DBURIConstants;
import com.gmeci.atskservice.resolvers.GradientDBItem;
import com.gmeci.atskservice.resolvers.GradientProviderClient;
import com.gmeci.constants.Constants;
import com.gmeci.helpers.LineHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GradientController {

    private static final String TAG = "GradientController";
    private final Context context;

    private MapGradientController mpc;
    private final HashMap<String, String> Type2MS2525Map = new HashMap<String, String>();
    private final GradientProviderClient gpc;
    private DrawGradientsAsyncTask gradAsyncTask;
    private String CurrentGradientUID = "";
    private final BroadcastReceiver mapChangeRx = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ATSKConstants.SHOW_HIDE_GRADIENT)) {

                String type = intent.getExtras().getString(
                        ATSKConstants.GRADIENT_CLASS, "");
                boolean show = intent.getExtras().getBoolean(
                        ATSKConstants.SHOW, true);
                Cursor cur = null;

                if (type.equals(ATSKConstants.GRADIENT_CLASS_FILTERED)) {
                    String CurrentSurveyUID = intent.getExtras().getString(
                            ATSKConstants.CURRENT_SURVEY,
                            ATSKConstants.DEFAULT_GROUP);
                    cur = gpc.GetAnalyzedGradients(CurrentSurveyUID,
                            true);
                } else if (type.equals(ATSKConstants.GRADIENT_CLASS_RAW))
                    cur = gpc.GetAllGradients(ATSKConstants.DEFAULT_GROUP,
                            false);
                else if (type.equals(ATSKConstants.GRADIENT_CLASS_ALL))
                    cur = gpc
                            .GetAllGradients(ATSKConstants.DEFAULT_GROUP, true);

                if (show)
                    drawGradients(cur);
                else
                    hideGradients(cur);
            } else if (intent.getAction().equals(
                    ATSKConstants.CURRENT_GRADIENT_UPDATE)) { //someone changed the "Current" gradient
                //make the CurrentGradientUID not current
                String NewCurrentGradient = intent
                        .getStringExtra(ATSKConstants.UID_EXTRA);

                new Thread(new CurrentGradientChangeRunnable(
                        CurrentGradientUID, false)).start();
                new Thread(new CurrentGradientChangeRunnable(
                        NewCurrentGradient, true)).start();

                CurrentGradientUID = NewCurrentGradient;

            }
        }
    };
    private GradientContentObserver gradientCO;
    private final MapView mapView;

    private static GradientController _instance;

    public static GradientController getInstance() {
        return _instance;
    }

    public GradientController(MapView mapview) {

        this.mapView = mapview;

        this.context = mapView.getContext();
        Type2MS2525Map.put(Constants.PO_GENERIC_POINT,
                "SUUAOP.........");
        gpc = new GradientProviderClient(context);
        _instance = this;
    }

    public void onResume() {
        gpc.Start();
        mpc = new MapGradientController(this.mapView);
        gradientCO = new GradientContentObserver(new Handler());

        context.getContentResolver().registerContentObserver(
                Uri.parse(DBURIConstants.GRADIENT_POINT_URI), true, gradientCO);
        context.getContentResolver().registerContentObserver(
                Uri.parse(DBURIConstants.GRADIENT_URI), true, gradientCO);

        DocumentedIntentFilter filter = new DocumentedIntentFilter();
        filter.addAction(ATSKConstants.ATSK_MAP_CHANGE);
        filter.addAction(ATSKConstants.CURRENT_GRADIENT_UPDATE);
        filter.addAction(ATSKConstants.SHOW_HIDE_GRADIENT);
        AtakBroadcast.getInstance().registerReceiver(mapChangeRx, filter);

    }

    public void onPause() {
        context.getContentResolver().unregisterContentObserver(this.gradientCO);
        this.mpc.close();

        AtakBroadcast.getInstance().unregisterReceiver(mapChangeRx);
        this.hideGradients(gpc.GetAllGradients(ATSKConstants.DEFAULT_GROUP,
                false));
        gpc.Stop();

        if (gradAsyncTask != null && !gradAsyncTask.isCancelled())
            gradAsyncTask.cancel(false);
    }

    private void drawGradients(Cursor cur) {
        if (cur == null || cur.getCount() <= 0)
            return;

        // Draw all gradients, pulling from DB if necessary
        List<GradientDBItem> newItems = new ArrayList<GradientDBItem>();
        try {
            cur.moveToFirst();
            while (!cur.isAfterLast() && cur.getCount() > 0) {
                if (!mpc.showGradient(cur.getString(2)))
                    // Gradient not drawn yet - add to list of new items
                    newItems.add(gpc.GetGradient(cur, false, false));
                cur.moveToNext();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to draw gradients", e);
        } finally {
            cur.close();
        }

        // Pull gradient points from DB asynchronously
        if (!newItems.isEmpty()) {
            Log.d(TAG, "Drawing " + newItems.size() + " new gradients");
            gradAsyncTask = new DrawGradientsAsyncTask(newItems);
            gradAsyncTask.execute(1);
        }
    }

    private void hideGradients(Cursor cur) {
        if (cur == null || cur.getCount() <= 0)
            return;
        try {
            cur.moveToFirst();
            while (!cur.isAfterLast() && cur.getCount() > 0) {
                mpc.hideGradient(cur.getString(2));
                cur.moveToNext();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to draw gradients", e);
        } finally {
            cur.close();
        }
    }

    public void removeAllGradients() {
        mpc.close();
    }

    private void UpdateGradientLine(String group, String uID) {
        GradientDBItem existingLineObstruction = gpc.GetGradient(group, uID,
                false);
        if (existingLineObstruction == null) {
            mpc.hideGradient(uID);
        } else {
            mpc.drawGradient(uID, existingLineObstruction.getType(),
                    existingLineObstruction.getPoints());
        }
    }

    private String GetUIDFromURI(Uri uri) {
        String URIString = uri.toString();

        URIString = URIString.replace("/alp", "");
        URIString = URIString.replace("/dlp", "");
        URIString = URIString.replace("/agp", "");
        URIString = URIString.replace("/dgp", "");
        String[] tokens = URIString.split("/");
        int Position = 1;
        if (tokens.length > Position + 1)
            return tokens[tokens.length - Position];
        return "";
    }

    private String GetGroupFromURI(Uri uri) {
        //content://com.gmeci.atskservice.obstructionDB.ObstructionProvider/point/default/Building.0
        String URIString = uri.toString();
        URIString = URIString.replace("/alp", "");
        URIString = URIString.replace("/dlp", "");
        URIString = URIString.replace("/agp", "");
        URIString = URIString.replace("/dgp", "");
        String[] tokens = URIString.split("/");
        int Position = 2;
        if (tokens.length > Position)
            return tokens[tokens.length - Position];
        return ATSKConstants.DEFAULT_GROUP;
    }

    private class DrawGradientsAsyncTask extends
            AsyncTask<Integer, Integer, Integer> {

        private final List<GradientDBItem> gradients;

        DrawGradientsAsyncTask(List<GradientDBItem> gradients) {
            this.gradients = gradients;
        }

        @Override
        protected Integer doInBackground(Integer... params) {

            // LOU someday we should filter based on range from the current survey

            Intent intent = new Intent(ATSKConstants.
                    GRADIENT_DRAW_PROGRESS_ACTION);
            intent.putExtra(ATSKConstants.GRADIENT_DRAW_PROGRESS_TOTAL,
                    gradients.size());
            int i = 0;
            for (GradientDBItem gradient : gradients) {
                if (this.isCancelled())
                    return -1;
                gpc.getPoints(gradient, false);
                if (gradient.getLinePoints().size() <= 0)
                    gpc.DeleteGradient(gradient.getGroup(), gradient.getUid(),
                            true);
                else
                    mpc.drawGradient(gradient.getUid(), gradient.getType(),
                            gradient.getLinePoints());

                intent.putExtra(ATSKConstants.GRADIENT_DRAW_PROGRESS_CURRENT,
                        i);
                AtakBroadcast.getInstance().sendBroadcast(intent);
                i++;
            }
            return gradients.size();
        }

    }

    private class CurrentGradientChangeRunnable implements Runnable {
        final String UID;
        final boolean CurrentSurvey;

        CurrentGradientChangeRunnable(String UID, boolean CurrentSurvey) {
            this.CurrentSurvey = CurrentSurvey;
            this.UID = UID;
        }

        @Override
        public void run() {

            if (UID == null || UID.length() < 1)
                return;
            GradientDBItem currentLine = gpc.GetGradient(
                    ATSKConstants.DEFAULT_GROUP, UID, false);
            //draw on the map...

            if (currentLine != null && currentLine.getUid().length() > 0) {
                String Type = currentLine.getType();
                if (CurrentSurvey)
                    Type = Type
                            + LineHelper.CURRENT_GRADIENT_MODIFIER;
                mpc.drawGradient(currentLine.getUid(), Type,
                        currentLine.getLinePoints());
            }
        }

    }

    private class GradientContentObserver extends ContentObserver {
        GradientContentObserver(Handler handler) {
            super(handler);
        }

        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            Log.d(TAG, "Changed");

        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);

            String Group = GetGroupFromURI(uri);
            String UID = GetUIDFromURI(uri);
            UpdateGradientLine(Group, UID);
        }

    }

}
