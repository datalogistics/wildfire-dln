
package com.gmeci.atsk.gradient;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.atakmap.android.atsk.plugin.R;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.log.Log;
import com.gmeci.atsk.gradient.graphs.AllTransverseGradientsGraphActivity;
import com.gmeci.atsk.resources.ATSKApplication;
import com.gmeci.core.ATSKConstants;
import com.gmeci.atsk.resources.ATSKDialogManager;
import com.gmeci.atsk.resources.ATSKDialogManager.ConfirmInterface;
import com.gmeci.atsk.resources.quickaction.ActionItem;
import com.gmeci.atsk.resources.quickaction.QuickAction;
import com.gmeci.atskservice.resolvers.DBURIConstants;
import com.gmeci.atskservice.resolvers.ObstructionProviderClient;
import com.gmeci.atskservice.resolvers.GradientDBItem;
import com.gmeci.atsk.gradient.graphs.LongitudinalGradientGraphActivity;
import com.gmeci.atsk.gradient.graphs.TransverseGradientGraphActivity;
import com.gmeci.core.SurveyData;

public class GradientProcessFragment extends GradientTabBase {

    private static final String TAG = "GradientProcessFragment";

    private QuickAction quickAction;
    private Context context, plugin;
    private ObstructionProviderClient opc;
    private GPCLongitudinalFilterTask gpcalat;
    private boolean hideRaw = false, hideAnalyzed = false;
    private GPCTransverseFilterTask gpcatat;
    private Button analyzeBtn, hideAnalyzedBtn;
    private ProgressBar TopPB, BottomPB;
    private TextView TopTV, BottomTV;
    private GradientCursorAdapter adapter;
    private Cursor cursor;
    private int selectedPosition = -10;
    private MyContentObserver gradientContentObserver;
    private String GroupName2Confirm, Name2Confirm;
    private SharedPreferences _prefs;

    //LEGACY MODE
    private boolean _legacy = true;

    final ConfirmInterface DeleteGradientConfirmInterface = new ConfirmInterface() {
        @Override
        public void ConfirmResponse(String Type, boolean Confirmed) {
            if (Confirmed) {
                gpc.DeleteGradient(GroupName2Confirm, Name2Confirm);
            }

        }
    };

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = MapView.getMapView().getContext();
        _prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        plugin = ATSKApplication.getInstance().getPluginContext();
        return LayoutInflater.from(plugin).inflate(
                R.layout.gradient_analysis, container, false);
    }

    @Override
    public void onPause() {

        if (gpcalat != null) {
            while (gpcalat.getStatus() != Status.FINISHED) {
                gpcalat.cancel(true);
                Log.d(TAG, "waiting on gpcalat to finish...");
                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        if (gpcatat != null) {
            while (gpcatat.getStatus() != Status.FINISHED) {
                gpcatat.cancel(true);
                Log.d(TAG, "waiting on gpcatat to finish...");
                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        while (UpdadingGradient) {
            Log.d(TAG, "waiting on gradient update to finish...");
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        SetCurrentGradientUID("NONE");
        opc.Stop();
        getActivity().getContentResolver().unregisterContentObserver(
                gradientContentObserver);
        super.onPause();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {

        v.setSelected(true);
        if (selectedPosition == position) {
            selectedPosition = -10;
            SetCurrentGradientUID("None");
        } else {
            Cursor c = cursor;
            c.moveToPosition(position);
            String UID = c.getString(2);
            SetCurrentGradientUID(UID);
            selectedPosition = position;
        }
        adapter.SetSelectedItem(selectedPosition);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onResume() {

        super.onResume();
        opc = new ObstructionProviderClient(getActivity());
        opc.Start();
        UpdateAnalyzedGradientList();
        gradientContentObserver = new MyContentObserver(coHandler);
        getActivity().getContentResolver().registerContentObserver(
                Uri.parse(DBURIConstants.GRADIENT_URI), true,
                gradientContentObserver);
        _legacy = _prefs.getBoolean("pref_legacy_graph", true);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        this.getListView().setOnItemLongClickListener(
                new OnItemLongClickListener() {

                    @Override
                    public boolean onItemLongClick(AdapterView<?> l, View v,
                            int position, long rowId) {
                        Cursor LogClickedItem = cursor;
                        LogClickedItem.moveToPosition(position);
                        String Group = LogClickedItem.getString(LogClickedItem
                                .getColumnIndex(DBURIConstants.COLUMN_GROUP_NAME_LINE));
                        String UID = LogClickedItem.getString(LogClickedItem
                                .getColumnIndex(DBURIConstants.COLUMN_UID));
                        String Type = LogClickedItem.getString(LogClickedItem
                                .getColumnIndex(DBURIConstants.COLUMN_TYPE));

                        GradientItemQuickAction(v, Group, UID, Type, position);

                        return true;
                    }

                });
        TopPB = (ProgressBar) view.findViewById(R.id.topPB);
        TopPB.setVisibility(View.GONE);
        BottomPB = (ProgressBar) view.findViewById(R.id.bottomPB);
        BottomPB.setVisibility(View.GONE);
        TopTV = (TextView) view.findViewById(R.id.topPBName);
        TopTV.setVisibility(View.GONE);
        BottomTV = (TextView) view.findViewById(R.id.bottomPBName);
        BottomTV.setVisibility(View.GONE);

        analyzeBtn = (Button) view.findViewById(R.id.analyze);
        analyzeBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String CurrentSurveyUID = azpc.getSetting(
                        ATSKConstants.CURRENT_SURVEY, TAG);

                opc.DeletePoints(ATSKConstants.GRADIENT_GROUP);
                //async task
                GradientTransverseAnalyzeAsyncTask gtaat = new GradientTransverseAnalyzeAsyncTask(
                        azpc, CurrentSurveyUID, gpc, TopPB, TopTV, opc);
                gtaat.execute(CurrentSurveyUID);
                hideAnalyzed = false;
                hideAnalyzedBtn.setText(plugin
                        .getString(R.string.hide_analyzed));
            }
        });
        final Button filterBtn = (Button) view.findViewById(R.id.filter);
        filterBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                //clear DB to mark all points not-used
                String CurrentSurveyUID = azpc.getSetting(
                        ATSKConstants.CURRENT_SURVEY, TAG);

                opc.DeletePoints(ATSKConstants.GRADIENT_GROUP);
                int ItemsDeleted = gpc
                        .DeleteAnalyzedGradients(CurrentSurveyUID);
                Log.d(TAG, "analyzed gradients deleted: " + ItemsDeleted);
                gpcalat = new GPCLongitudinalFilterTask(
                        getActivity(), azpc, gpc, TopPB, TopTV, 50);
                //gpcalat.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, CurrentSurveyUID);
                gpcalat.execute(CurrentSurveyUID);

                gpcatat = new GPCTransverseFilterTask(
                        getActivity(), azpc, gpc, BottomPB, BottomTV);
                //gpcatat.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, CurrentSurveyUID);
                gpcatat.execute(CurrentSurveyUID);
            }
        });
        final Button hideRawBtn = (Button) view.findViewById(R.id.hide_raw);
        hideRawBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent i = new Intent(ATSKConstants.SHOW_HIDE_GRADIENT);
                i.putExtra(ATSKConstants.GRADIENT_CLASS,
                        ATSKConstants.GRADIENT_CLASS_RAW);
                i.putExtra(ATSKConstants.SHOW, hideRaw);
                AtakBroadcast.getInstance().sendBroadcast(i);
                hideRawBtn.setText(plugin.getString(hideRaw ?
                        R.string.hide_raw : R.string.show_raw));
                hideRaw = !hideRaw;
            }

        });

        hideAnalyzedBtn = (Button) view.findViewById(R.id.hide_filtered);
        hideAnalyzedBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent i = new Intent(ATSKConstants.SHOW_HIDE_GRADIENT);
                i.putExtra(ATSKConstants.GRADIENT_CLASS,
                        ATSKConstants.GRADIENT_CLASS_FILTERED);
                i.putExtra(ATSKConstants.SHOW, hideAnalyzed);
                i.putExtra(ATSKConstants.CURRENT_SURVEY,
                        azpc.getSetting(ATSKConstants.CURRENT_SURVEY, TAG));
                AtakBroadcast.getInstance().sendBroadcast(i);
                hideAnalyzedBtn.setText(plugin.getString(hideAnalyzed ?
                        R.string.hide_analyzed : R.string.show_analyzed));
                hideAnalyzed = !hideAnalyzed;
            }

        });

        notifyTabHost();
    }

    public void UpdateAnalyzedGradientList() {

        String CurrentSurveyUID = azpc.getSetting(ATSKConstants.CURRENT_SURVEY,
                TAG);
        if (CurrentSurveyUID.equals(""))
            return;

        Cursor oldcursor = cursor;
        cursor = gpc.GetAnalyzedGradients(CurrentSurveyUID, true);
        if (cursor == null)
            return;

        //no analysis button if we don't have a cursor
        analyzeBtn.setVisibility(cursor.getCount() > 0 ? View.VISIBLE
                : View.GONE);

        //Log.d(TAG, "Got Cursor " + cursor.getCount());
        SurveyData survey = azpc.getAZ(CurrentSurveyUID, false);
        adapter = new GradientCursorAdapter(plugin, getActivity(),
                cursor, gpc, survey != null && survey.surveyIsLTFW(), false);

        setListAdapter(adapter);
        getListView().setTextFilterEnabled(true);

        if (oldcursor != null) {
            //Log.d(TAG, "closing old gradient list cursor");
            oldcursor.close();
        }

    }

    private void GradientItemQuickAction(View v, final String GroupName,
            final String Name, final String Type, final int Index) {
        quickAction = new QuickAction(getActivity());

        Resources res = ATSKApplication
                .getInstance().getPluginContext().getResources();

        //make new quick action
        ActionItem quickType = new ActionItem();

        //get the TYpe and ...
        final GradientDBItem gradient = gpc.GetGradient(
                GroupName, Name, true);

        if (gradient == null)
            return;

        final boolean isTransverse = gradient.getType()
                .startsWith(ATSKConstants.GRADIENT_TYPE_TRANSVERSE);

        // Delete gradient
        quickType.setTitle("Delete");
        quickType.setIcon(res.getDrawable(R.drawable.atsk_delete));
        quickAction.addActionItem(quickType);

        // View gradient chart
        quickType.setTitle("View");
        quickType.setIcon(res.getDrawable(R.drawable.atsk_gradient_view));
        quickAction.addActionItem(quickType);

        if (isTransverse) {
            // Add gradient to PDF form
            quickType.setTitle("Add to Form");
            quickType.setIcon(res.getDrawable(R.drawable.navigation_id_badge));
            quickAction.addActionItem(quickType);
        }

        quickAction.setOnActionItemClickListener(
                new QuickAction.OnActionItemClickListener() {
                    @Override
                    public void onItemClick(int pos) {
                        if (pos == 0) {
                            //delete the selected item
                            Name2Confirm = Name;
                            GroupName2Confirm = GroupName;
                            ATSKDialogManager.ShowConfirmDialog(getActivity(),
                                    "Confirm Delete Gradient?", Name, "Delete",
                                    DeleteGradientConfirmInterface);

                        } else if (pos == 1) { //make this the current Gradient...
                            SetCurrentGradientUID(gradient.getUid());
                            //show detailed activity

                            if (Type.contains(ATSKConstants.LONGITUDINAL)
                                    || Type.contains(ATSKConstants.GRADIENT_TYPE_LONGITUDINAL)) {

                                new LongitudinalGradientGraphActivity(_legacy)
                                        .show(context);
                            } else {
                                if (_legacy) {
                                    new TransverseGradientGraphActivity(_legacy)
                                            .show(
                                                    context, Name, Index - 1);
                                } else {
                                    new AllTransverseGradientsGraphActivity(
                                            _legacy).show(
                                            context, Name, Index - 1);
                                }
                            }
                        } else if (pos == 2) {
                            if (!isTransverse)
                                return;
                            String surveyUID = azpc.getSetting(
                                    ATSKConstants.CURRENT_SURVEY, TAG);
                            SurveyData survey = azpc.getAZ(surveyUID, false);
                            if (survey != null) {
                                survey.edges.LeftMaintainedAreaGradient =
                                        gradient.getMaintainedGradientL();
                                survey.edges.RightMaintainedAreaGradient =
                                        gradient.getMaintainedGradientR();
                                survey.edges.LeftGradedAreaGradient =
                                        gradient.getGradedGradientL();
                                survey.edges.RightGradedAreaGradient =
                                        gradient.getGradedGradientR();
                                survey.edges.LeftShoulderGradient =
                                        gradient.getShoulderGradientL();
                                survey.edges.RightShoulderGradient =
                                        gradient.getShoulderGradientR();
                                survey.edges.LeftHalfRunwayGradient =
                                        gradient.getLZGradientL();
                                survey.edges.RightHalfRunwayGradient =
                                        gradient.getLZGradientR();
                                azpc.UpdateAZ(survey, "azgu", false);
                                Toast.makeText(
                                        getActivity(),
                                        "Added gradient "
                                                + Name.substring(
                                                        Name.indexOf(ATSKConstants.TRANSVERSE))
                                                + " to output form.",
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    }

                });
        quickAction.show(v);
    }

    private class MyContentObserver extends ContentObserver {

        public MyContentObserver(Handler handler) {
            super(handler);
        }

        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            Log.d(TAG, "Changed");
            UpdadingGradient = true;
            UpdateAnalyzedGradientList();
            UpdadingGradient = false;

        }

    }

}
