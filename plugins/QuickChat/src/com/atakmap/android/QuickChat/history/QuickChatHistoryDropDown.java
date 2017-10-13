
package com.atakmap.android.QuickChat.history;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.atakmap.android.QuickChat.plugin.QuickChatLifecycle;
import com.atakmap.android.QuickChat.plugin.R;
import com.atakmap.android.QuickChat.utils.PluginHelper;
import com.atakmap.android.dropdown.DropDown;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.log.Log;

/**
 * Created by Scott Auman on 5/18/2016.
 * provides a drop down window that shows every
 * message shown as a popup message to the user as a "history"
 */
public class QuickChatHistoryDropDown extends DropDownReceiver implements
        DropDown.OnStateListener, SharedPreferences.OnSharedPreferenceChangeListener {

    private final View view;
    private final ExpandableListView listView;
    private final TextView noMessages;
    private final TextView numMessages;
    private SharedPreferences _prefs;
    private final String TAG = getClass().getSimpleName();
    private HistoryAdapter adapter;
    private boolean groupByDate;
    private  ImageButton filter;

    public QuickChatHistoryDropDown(final MapView mapView) {
        super(mapView);

        PreferenceManager.getDefaultSharedPreferences(mapView.getContext()).registerOnSharedPreferenceChangeListener(this);

        //inflate the view used for the dropdown
        view = LayoutInflater.from(PluginHelper.pluginContext).inflate
                (R.layout.history_message_dropdown, null);

        //create widget variable casts
        noMessages = (TextView) view.findViewById(R.id.noMessagesTextView);
        numMessages = (TextView) view.findViewById(R.id.textView4);
        listView = (ExpandableListView) view
                .findViewById(R.id.messagesListView);
        ImageButton clearButton = (ImageButton) view
                .findViewById(R.id.deleteImageButton);
        ImageButton export = (ImageButton) view
                .findViewById(R.id.exportChatImageButton);
        filter = (ImageButton) view
                .findViewById(R.id.groupByImageButton);

        _prefs = PreferenceManager.getDefaultSharedPreferences(
                MapView.getMapView().getContext());

        //attach click listeners
        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.deleteImageButton:
                        deleteHistory();
                        break;
                    case R.id.exportChatImageButton:
                        PluginHelper.showExportDialog(MapView.getMapView()
                                .getContext());
                        break;
                    case R.id.groupByImageButton:
                        setGroupByDate(!groupByDate);
                        Toast.makeText(getMapView().getContext(),
                                "History Sorted By " + (groupByDate ? "Dates" : "Callsigns"),Toast.LENGTH_SHORT).show();
                        break;
                    default:
                        break;
                }
            }
        };
        clearButton.setOnClickListener(onClickListener);
        export.setOnClickListener(onClickListener);
        filter.setOnClickListener(onClickListener);

        groupByDate = (_prefs.getBoolean("groupAdapterByDate",true));
        changeGroupByIcon();

        //set custom padding on group icon
        DisplayMetrics metrics = new DisplayMetrics();
        ((Activity) MapView.getMapView().getContext()).getWindowManager()
                .getDefaultDisplay().getMetrics(metrics);
        int width = metrics.widthPixels;
        listView.setIndicatorBounds(width - GetPixelFromDips(50), width
                - GetPixelFromDips(10));
        listView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v,
                    int groupPosition, int childPosition, long id) {
                adapter.addSelectedItem(groupPosition, childPosition);
                return true;
            }
        });

        //listen for preference changes!
        SharedPreferences.OnSharedPreferenceChangeListener prefChangeListener
                = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(
                    SharedPreferences sharedPreferences, String key) {
                if (key.equals("popup_24hr_time")) {
                    if (getAdapter() != null)
                        getAdapter().updateTimes();
                }else if(key.equals("groupAdapterByDate")){
                    setGroupByDate(sharedPreferences.getBoolean(key,true));
                }
            }
        };
        _prefs.registerOnSharedPreferenceChangeListener(prefChangeListener);

    }

    private void createAndBindAdapter() {
        //attach adapter and set to current expandable list view
        adapter = new HistoryAdapter(PluginHelper.pluginContext,
                noMessagesStateListener);
        listView.setAdapter(adapter); //add adapter with info to listview
    }

    private void deleteHistory() {
        if (getAdapter() != null && getAdapter().getAllMessagesCount() > 0) {
            if (adapter.isItemsSelected()) {
                showConfirmDialog(false);
            } else {
                showConfirmDialog(true);
            }
        } else {
            Toast.makeText(MapView.getMapView().getContext(),
                    "No Messages To Delete", Toast.LENGTH_SHORT).show();
        }
    }

    private int GetPixelFromDips(float pixels) {
        // Get the screen's density scale
        final float scale = MapView.getMapView().getContext()
                .getResources().getDisplayMetrics().density;
        // Convert the dps to pixels, based on density scale
        return (int) (pixels * scale + 0.5f);
    }

    /**
     * displays a android dialog asking user
     * if they want to delete all messages in saved history
     * this is caused when no specific messages are selected
     */
    private void showConfirmDialog(final boolean all) {

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                MapView.getMapView().getContext());

        // set title
        alertDialogBuilder.setTitle("Confirm Delete?");

        // set dialog message
        alertDialogBuilder
                .setMessage(
                        all ? "Clear All Popup Messages Saved?"
                                : "Clear Selected Messages")
                .setCancelable(true)
                .setPositiveButton("Yes",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // if this button is clicked, close
                                // current activity
                                //clear all saved static popups
                                //call notify data set changed!
                                if (all) {
                                    SavedMessageHistory.saveMessagesInHistory(
                                            MapView.getMapView().getContext(),
                                            SavedMessageHistory.getDefault());
                                    Toast.makeText(
                                            MapView.getMapView().getContext(),
                                            "All Saved Popup Messages Cleared",
                                            Toast.LENGTH_SHORT).show();
                                } else {
                                    SavedMessageHistory.removeMessages(adapter
                                            .getAllMessagesSelected());
                                    Toast.makeText(
                                            MapView.getMapView().getContext(),
                                            "Selected Messages Deleted",
                                            Toast.LENGTH_SHORT).show();
                                }
                                //refresh listview adapter
                                if (getAdapter() != null)
                                    getAdapter().refresh();
                            }
                        })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // if this button is clicked, just close
                        // the dialog box and do nothing
                        dialog.cancel();
                    }
                });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(key.equals("popup_24hr_time")){
            if(isVisible())
                createAndBindAdapter();
        }
    }

    public interface NoMessagesStateListener {
        void onStateChanged(int visibility);

        void updateNumberOfMessages(int i);
    }

    private final NoMessagesStateListener noMessagesStateListener = new NoMessagesStateListener() {
        @Override
        public void onStateChanged(int visibility) {
            noMessages.setVisibility(visibility);
        }

        @Override
        public void updateNumberOfMessages(int i) {
            numMessages.setText("" + i);
        }
    };

    public void onDestroy(){
        Log.d(QuickChatLifecycle.TAG,"Destroying Filter Drop Down");
        if(isVisible()){
            closeDropDown();
        }
    }

    @Override
    protected void disposeImpl() {
    }

    /**
     * When the intent matching this receiver is called and sent out as
     * a broadcast, this method is called
     */
    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent.getAction().equals(
                "com.atakmap.android.QuickChat.SHOW_HISTORY_DROPDOWN")) {
            // 50% Width 50% height
            if (!isVisible()) {
                showDropDown(view, HALF_WIDTH, FULL_HEIGHT, FULL_WIDTH,
                        HALF_HEIGHT, false, this);
            } else {
                PluginHelper.showDropDownExists();
            }
        } else if (intent.getAction().equals(
                "com.atakmap.android.QuickChat.ADD_MESSAGE_TO_LIST")) {
            SavedMessageHistory.addMessageToList(PluginHelper.pluginContext,
                    (Message) intent.getParcelableExtra("MESSAGE"));
            if (isVisible()) {
                adapter.refresh();
                return;
            }
        }

        /*
            something is funky with calling refresh() on adapter
            when we show the dropdown
            the underlying data updates but not the views
            so we are going to re-create the adapter
         */
        createAndBindAdapter();
    }

    public void setGroupByDate(boolean groupByDate) {
        _prefs.edit().putBoolean("groupAdapterByDate",groupByDate).apply();
        this.groupByDate = groupByDate;
        changeGroupByIcon();
        createAndBindAdapter();
    }

    private void changeGroupByIcon() {
        filter.setImageDrawable(PluginHelper.pluginContext.getResources().
                getDrawable(!groupByDate ? R.drawable.ic_user_icon
            : R.drawable.ic_date_icon));
    }

    public static HistoryAdapter.ADAPTER_TYPE getAdapterType() {
        return PreferenceManager.getDefaultSharedPreferences(MapView.getMapView().getContext()).
                getBoolean("groupAdapterByDate",true) ? HistoryAdapter.ADAPTER_TYPE.DATE :
                HistoryAdapter.ADAPTER_TYPE.CALLISGN;
    }

    @Override
    public void onDropDownSelectionRemoved() {
    }

    /**
     * handle class cleanup when
     * the dropdown closes by user or unexpectedly
     */
    @Override
    public void onDropDownClose() {
    }

    @Override
    public void onDropDownSizeChanged(double width, double height) {
    }

    @Override
    public void onDropDownVisible(boolean v) {

    }

    private HistoryAdapter getAdapter() {
        if (adapter != null) {
            return adapter;
        }
        Log.d(TAG, "Could not refresh adapter is null");
        return null;
    }
}
