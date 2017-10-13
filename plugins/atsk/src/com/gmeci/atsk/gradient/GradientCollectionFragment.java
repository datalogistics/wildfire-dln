
package com.gmeci.atsk.gradient;

import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.coremap.maps.time.CoordinatedTime;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import com.atakmap.android.atsk.plugin.R;
import com.atakmap.coremap.log.Log;
import com.gmeci.atsk.resources.ATSKApplication;
import com.gmeci.core.ATSKConstants;
import com.gmeci.core.ATSKIntentConstants;
import com.gmeci.atsk.resources.ATSKDialogManager;
import com.gmeci.atsk.resources.ATSKDialogManager.ConfirmInterface;
import com.gmeci.atsk.resources.quickaction.ActionItem;
import com.gmeci.atsk.resources.quickaction.QuickAction;
import com.gmeci.atskservice.resolvers.DBURIConstants;
import com.gmeci.atskservice.resolvers.ObstructionProviderClient;

public class GradientCollectionFragment extends GradientTabBase {

    private static final String GRADIENT_COLLECTION_NAME = "Gradient_Collection_Name";

    private static final String GRADIENT_COLLECTED_COUNT = "Gradient_collection_count";
    private static final String TAG = "GradientCollecitonFragment";
    MyContentObserver gradientDBContentObserver;
    ObstructionProviderClient opc;
    SharedPreferences gps_settings;
    OnSharedPreferenceChangeListener listener;
    ImageView GradientOnImage;
    int SelectedItem = -1;
    int selectedPosition = -10;
    GradientCursorAdapter adapter;
    Cursor cursor;
    AlertDialog NameAD, SavingAD;
    QuickAction quickAction;
    String GroupName2Confirm, Name2Confirm;
    boolean GradientCollectionRunning = false;
    boolean ExpectingTransverseGradient = true;
    String CollectionName = ATSKConstants.DEFAULT_GROUP;
    final ConfirmInterface DeleteGradientConfirmInterface = new ConfirmInterface() {
        @Override
        public void ConfirmResponse(String Type, boolean Confirmed) {
            if (Confirmed) {
                gpc.DeleteGradient(GroupName2Confirm, UIDSelected2Change);
            }

        }
    };
    String GroupNameSelected2Change, NameSelected2Change, UIDSelected2Change;
    private Context pluginContext;

    private static final double getFloat(SharedPreferences sp, String key,
            double dv) {
        try {
            return Float.parseFloat(sp.getString(key, dv + ""));
        } catch (NumberFormatException e) {
            return dv;
        }
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        opc = new ObstructionProviderClient(getActivity());
        opc.Start();
        gps_settings = PreferenceManager
                .getDefaultSharedPreferences(getActivity());
        listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(
                    SharedPreferences sharedPreferences, String key) {
                if (key.equals(ATSKConstants.OBSTRUCTION_METHOD_GPS_HEIGHT_M)) {
                    double Height_m = getFloat(gps_settings,
                            ATSKConstants.OBSTRUCTION_METHOD_GPS_HEIGHT_M, 2);
                }
            }
        };
    }

    @Override
    public void onDestroy() {
        opc.Stop();
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        pluginContext = ATSKApplication.getInstance().getPluginContext();
        return LayoutInflater.from(pluginContext).inflate(
                R.layout.gradient_collect, container, false);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "Pausing Gradient Collection 0 ");
        while (UpdadingGradient) {
            Log.d(TAG, "waiting on the gradient update");
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        gps_settings.edit().putString(GRADIENT_COLLECTION_NAME, CollectionName)
                .apply();

        Log.d(TAG, "Pausing Gradient Collection");
        SetCurrentGradientUID("NONE");
        getActivity().getContentResolver().unregisterContentObserver(
                gradientDBContentObserver);
        gps_settings.unregisterOnSharedPreferenceChangeListener(listener);

    }

    public String getTodaysDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yy-MMM-dd'T'HH:mm");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return (sdf.format(CoordinatedTime.currentDate()));
    }

    @Override
    public void onResume() {
        super.onResume();
        UpdadingGradient = false;

        CollectionName = getTodaysDate();

        gps_settings.registerOnSharedPreferenceChangeListener(listener);

        gradientDBContentObserver = new MyContentObserver(coHandler);

        UpdateGradientList();
        getActivity().getContentResolver().registerContentObserver(
                Uri.parse(DBURIConstants.GRADIENT_URI), true,
                gradientDBContentObserver);
        getActivity().getContentResolver().registerContentObserver(
                Uri.parse(DBURIConstants.GRADIENT_POINT_URI), true,
                gradientDBContentObserver);

        CheckGradientCollectionStatus();
    }

    private void CheckGradientCollectionStatus() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    if (hardwareInterface == null) {
                        CheckGradientCollectionStatus();

                    } else {
                        if (hardwareInterface.isCollectingGradient()) {

                            GradientOnImage
                                    .setBackgroundResource(R.drawable.gradient_off);
                            GradientCollectionRunning = true;
                        } else {
                            GradientOnImage
                                    .setBackgroundResource(R.drawable.gradient_on);
                            GradientCollectionRunning = false;

                        }
                    }
                } catch (RemoteException e) {
                    GradientOnImage
                            .setBackgroundResource(R.drawable.gradient_off);
                    GradientCollectionRunning = true;
                }
            }

        }, 1000);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupGradientCollectionButton(view);

        getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        getListView().setOnItemLongClickListener(
                new OnItemLongClickListener() {

                    @Override
                    public boolean onItemLongClick(AdapterView<?> l, View v,
                            int position, long rowId) {
                        Cursor c = cursor;
                        c.moveToPosition(position);
                        String Group = c.getString(1);
                        String UID = c.getString(2);
                        /*String Description = c.getString(c
                                .getColumnIndex(DBURIConstants.COLUMN_DESCRIPTION));

                        if (Description == null || Description.length() == 0)
                            Description = UID;*/
                        GradientItemQuickAction(v, Group, ""/*Description*/,
                                UID);

                        return false;
                    }

                });

        notifyTabHost();
    }

    private void setupGradientCollectionButton(View view) {
        GradientOnImage = (ImageView) view.findViewById(R.id.toggle_collection);
        GradientOnImage.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                if (GradientCollectionRunning) {
                    try {
                        hardwareInterface.EndCurrentRoute(false);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    GradientOnImage
                            .setBackgroundResource(R.drawable.gradient_on);
                    //pd.dismiss();
                    GradientCollectionRunning = false;
                } else {
                    try {
                        GradientOnImage
                                .setBackgroundResource(R.drawable.gradient_off);
                        int GradientCount = gps_settings.getInt(
                                GRADIENT_COLLECTED_COUNT, 0);
                        GradientCount++;
                        CollectionName = getTodaysDate();
                        //get the gps offset height
                        double Height_m = getFloat(gps_settings,
                                ATSKConstants.OBSTRUCTION_METHOD_GPS_HEIGHT_M,
                                2);
                        if (hardwareInterface == null) {
                            Toast.makeText(
                                    getActivity(),
                                    "Failed to start Gradient Collection - RESTART",
                                    Toast.LENGTH_LONG).show();
                            return;
                        }
                        hardwareInterface.StartNewGradientRoute("Gradient_"
                                + CollectionName + "_" + GradientCount,
                                CollectionName, ATSKConstants.DEFAULT_GROUP,
                                Height_m);
                        gps_settings
                                .edit()
                                .putInt(GRADIENT_COLLECTED_COUNT, GradientCount)
                                .apply();
                        GradientCollectionRunning = true;
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        //if the colelction is still running from a previous start - we should display it

    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {

        if (selectedPosition == position) {
            selectedPosition = -10;
            SetCurrentGradientUID("");
            v.setSelected(false);
        } else {
            Cursor c = cursor;
            c.moveToPosition(position);
            String UID = c.getString(2);
            SetCurrentGradientUID(UID);
            selectedPosition = position;
            v.setSelected(true);
        }

        adapter.SetSelectedItem(position);
        adapter.notifyDataSetChanged();
    }

    public void UpdateGradientList() {
        Cursor oldcursor = cursor;
        cursor = gpc.GetAllGradients(ATSKConstants.DEFAULT_GROUP, false);
        if (cursor != null) {
            Log.d(TAG, "created new cursor " + cursor.getCount());

            adapter = new GradientCursorAdapter(pluginContext, getActivity(),
                    cursor, gpc, false, true);

            setListAdapter(adapter);
        }
        if (oldcursor != null) {
            Log.d(TAG, "closing an old cursor.");
            oldcursor.close();
        }
    }

    private void ShowNameHandJamDialog(String Title, String Name,
            final NameResponse responseHere) {

        final AlertDialog.Builder ad = new AlertDialog.Builder(getActivity(),
                android.R.style.Theme_Holo_Dialog);

        ad.setTitle(Title);
        final EditText input = new EditText(getActivity());
        input.setTextColor(0xFFFFFFFF);
        input.setBackgroundColor(Color.BLACK);
        input.setText(Name);
        input.setSingleLine();
        input.setOnKeyListener(new OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER))) {
                    responseHere.NamePressed(input, NameAD);
                    return true;
                }
                return false;
            }

        });

        ad.setView(input);
        ad.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int arg1) {
                dialog.dismiss();
            }
        });
        ad.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                responseHere.NamePressed(input, NameAD);
            }
        });
        NameAD = ad.create();
        NameAD.show();

    }

    private void GradientItemQuickAction(View v, final String GroupName,
            final String Name, final String UID) {

        Context pluginContext = ATSKApplication
                .getInstance().getPluginContext();

        quickAction = new QuickAction(getActivity());

        //make new quick action
        ActionItem quickType = new ActionItem();

        quickType.setTitle("Delete " + Name);
        quickType.setIcon(pluginContext.getResources().getDrawable(
                R.drawable.atsk_delete));
        quickAction.addActionItem(quickType);

        quickType.setTitle("Rename " + Name);
        quickType.setIcon(pluginContext.getResources().getDrawable(
                R.drawable.atsk_gradient_name_change));
        quickAction.addActionItem(quickType);

        quickType.setTitle("View " + Name);
        quickType.setIcon(pluginContext.getResources().getDrawable(
                R.drawable.atsk_gradient_view));
        quickAction.addActionItem(quickType);

        quickAction
                .setOnActionItemClickListener(new QuickAction.OnActionItemClickListener() {
                    @Override
                    public void onItemClick(int pos) {
                        if (pos == 0) {
                            //delete the selected item
                            Name2Confirm = Name;
                            UIDSelected2Change = UID;
                            GroupName2Confirm = GroupName;
                            //should center/highlite the gradient also...
                            SetCurrentGradientUID(UID);

                            ATSKDialogManager.ShowConfirmDialog(getActivity(),
                                    "Confirm Delete Gradient?",
                                    "Delete " + (Name.isEmpty() ? UID : Name)
                                            + "?",
                                    "Delete", DeleteGradientConfirmInterface);

                        } else if (pos == 1) {
                            GroupNameSelected2Change = GroupName;
                            NameSelected2Change = Name;
                            UIDSelected2Change = UID;
                            //save old name here??
                            ShowNameHandJamDialog("Re-Name Gradient",
                                    NameSelected2Change,
                                    new reNameGradientResponse());
                        } else if (pos == 2) { //make this the current Gradient...
                            //set a current Gradient somehow????
                            SetCurrentGradientUID(UID);

                            //center on this also
                            Cursor GradientPoints = gpc.GetGradientPoints(
                                    GroupName, UID, true);
                            if (GradientPoints != null
                                    && GradientPoints.getCount() > 1) {
                                GradientPoints.moveToFirst();
                                double lat = GradientPoints.getDouble(GradientPoints
                                        .getColumnIndex(DBURIConstants.COLUMN_LAT));
                                double lon = GradientPoints.getDouble(GradientPoints
                                        .getColumnIndex(DBURIConstants.COLUMN_LON));

                                GradientPoints.moveToLast();
                                lat += GradientPoints.getDouble(GradientPoints
                                        .getColumnIndex(DBURIConstants.COLUMN_LAT));
                                lon += GradientPoints.getDouble(GradientPoints
                                        .getColumnIndex(DBURIConstants.COLUMN_LON));

                                Intent goTo = new Intent(
                                        "com.atakmap.android.maps.ZOOM_TO_LAYER");
                                goTo.putExtra("point", new GeoPoint(lat / 2d,
                                        lon / 2d).toString());
                                AtakBroadcast.getInstance().sendBroadcast(goTo);
                            } else {
                                Toast.makeText(getActivity(),
                                        "Failed to zoom to "
                                                + UID, Toast.LENGTH_LONG)
                                        .show();
                            }
                            if (GradientPoints != null)
                                GradientPoints.close();

                        }

                    }

                });
        quickAction.show(v);
    }

    public void SetSurveyInterface() {
        super.SetSurveyInterface();
        //we should close the toolbar
        ATSKApplication.setObstructionCollectionMethod(
                ATSKIntentConstants.OB_STATE_HIDDEN,
                "GradientCollection", false);

    }

    interface NameResponse {
        void NamePressed(final EditText input, DialogInterface dialog);
    }

    private class MyContentObserver extends ContentObserver {

        public MyContentObserver(Handler handler) {
            super(handler);
        }

        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            Log.d(TAG, "Changed");

            //    UpdateGradientList();
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            UpdadingGradient = true;
            UpdateGradientList();
            UpdadingGradient = false;

        }

    }

    class reNameGradientResponse implements NameResponse {

        @Override
        public void NamePressed(EditText input, DialogInterface dialog) {
            String NewName = input.getText().toString();
            //Group, uid;
            gpc.RenameGradient(UIDSelected2Change, GroupNameSelected2Change,
                    NewName);
            if (dialog != null)
                dialog.dismiss();
        }
    }

}
