
package com.gmeci.atskservice;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.ListView;

import com.gmeci.core.ATSKConstants;
import com.gmeci.helpers.AssetHelper;
import com.gmeci.core.SurveyData;
import com.gmeci.atskservice.resolvers.AZProviderClient;
import com.gmeci.atskservice.resolvers.AZURIConstants;
import com.gmeci.atskservice.resolvers.DBURIConstants;
import com.gmeci.atskservice.databases.AZCursorAdapter;
import com.gmeci.atskservice.resolvers.GradientProviderClient;

import java.io.File;

public class MainActivity extends Activity {

    public static final int OverwriteFiles = Menu.FIRST + 5;
    private static final String CANCEL_MENU_TITLE = "CANCEL";
    private static final String DELETE_MENU_TITLE = "DELETE SURVEY";

    private final static String TAG = "MainActivity";
    private AZProviderClient azpc;
    private int ListContextMenu = 42;
    private GradientProviderClient gpc;
    private AZCursorAdapter cursorAdapter;
    private Button BurnFilesButton;
    private Button editbutton;
    private MyContentObserver co;
    private Handler coHandler = new Handler();
    // listens to long presses on each survey in the list
    OnItemLongClickListener surveyLongClickListener = new OnItemLongClickListener() {

        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view,
                int position, long id) {

            return false;
        }

    };
    private ListView listView;
    // listens to clicks on each survey in the list
    OnItemClickListener surveyClickListener = new OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position,
                long id) {

            Cursor c = (Cursor) cursorAdapter.getItem(position);
            String uid = c.getString(AZURIConstants.UID_INDEX);
            azpc.putSetting(ATSKConstants.CURRENT_SURVEY, uid,
                    "ATSKService Main Activity");

            listView.post(new Runnable() {
                public void run() {
                    rebuildSurveyList();
                }
            });

        }

    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem item5 = menu.add(0, OverwriteFiles, 0,
                "Overwrite Support Files");
        {
            item5.setAlphabeticShortcut('o');
        }
        return true;
    }

    public void showRemarksFragment(final SurveyData currentSurvey) {

        String surveyType = currentSurvey.getType().toString();
        String activity = null;

        if (surveyType.equals(SurveyData.AZ_TYPE.DZ.toString())) {
            activity = "com.gmeci.atskservice.form.DZForm";
        } else if (surveyType.equals(SurveyData.AZ_TYPE.LZ.toString())) {
            if (currentSurvey.surveyIsLTFW()) {
                activity = "com.gmeci.atskservice.form.LTFWLZForm";
            } else if (currentSurvey.surveyIsSTOL()) {
                activity = "com.gmeci.atskservice.form.STOLForm";
            } else {
                activity = "com.gmeci.atskservice.form.LZForm";
            }
        } else if (surveyType.equals(SurveyData.AZ_TYPE.HLZ.toString())) {
            activity = "com.gmeci.atskservice.form.HLZForm";
        } else if (surveyType.equals(SurveyData.AZ_TYPE.FARP.toString())) {
            activity = "com.gmeci.atskservice.form.FARPForm";
        }

        if (activity != null) {
            Intent intent = new Intent();
            intent.setClassName("com.gmeci.atskservice", activity);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent i;
        switch (item.getItemId()) {

            case OverwriteFiles:
                new OverwriteThread(false).start();

                return true;

        }
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {

        if (ListContextMenu == item.getItemId()) {
            if (item.getTitle().equals(CANCEL_MENU_TITLE)) {

            } else if (item.getTitle().equals(DELETE_MENU_TITLE)) {
                AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item
                        .getMenuInfo();
                Cursor c = (Cursor) cursorAdapter.getItem(info.position);
                String Name = c.getString(AZURIConstants.UID_INDEX);
                int DeletedItems = azpc.deleteAZ(Name);
                if (DeletedItems < 1)
                    Log.d(TAG, "Failed to delete Survey");

                cursorAdapter.changeCursor(azpc.getAllSurveys());
            }
            return true;
        }

        Intent i;
        switch (item.getItemId()) {

            case OverwriteFiles:
                new OverwriteThread(false).start();
                return true;

        }
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        azpc.Stop();
        gpc.Stop();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            //set up acra
            final File path = new File(
                    Environment.getExternalStorageDirectory()
                            + File.separator + "atsk" + File.separator);

            if (!path.mkdirs())
                Log.e(TAG, "Failed to create ATSK root directory: " + path);

            File logs = new File(path, "crashlogs");
            if (!logs.mkdir())
                Log.e(TAG, "Failed to create ATSK crash logs directory: "
                        + logs);
            com.partech.acra.CrashHandler.instance().initialize(
                    logs.toString() + File.separator + "atsk_notebook_");

            Log.d(TAG, "initialized logging directory: " + logs);
        } catch (Exception e) {
            Log.e(TAG, "error occurred setting up acra");
        }

        setContentView(R.layout.activity_main);
        azpc = new AZProviderClient(this);
        azpc.Start();

        gpc = new GradientProviderClient(this);
        gpc.Start();

        try {
            getWindow().addFlags(
                    WindowManager.LayoutParams.class.getField(
                            "FLAG_NEEDS_MENU_KEY").getInt(null));
        } catch (NoSuchFieldException e) {
            // Ignore since this fields won't exist in most versions of Android
        } catch (IllegalAccessException e) {
            Log.w("Optionmenus",
                    "Could not access FLAG_NEEDS_MENU_KEY in addLegacyOverflowButton()",
                    e);
        }
        co = new MyContentObserver(coHandler);
        this.getContentResolver().registerContentObserver(
                Uri.parse(DBURIConstants.LINE_URI), true, co);
        this.getContentResolver().registerContentObserver(
                Uri.parse(DBURIConstants.POINT_URI), true, co);

        listView = (ListView) findViewById(R.id.current_surveys_listview);
        listView.setOnItemClickListener(surveyClickListener);
        listView.setOnItemLongClickListener(surveyLongClickListener);

        editbutton = (Button) findViewById(R.id.editbutton);

        editbutton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                SurveyData CurrentSurvey =
                        azpc.getAZ(azpc.getSetting(
                                ATSKConstants.CURRENT_SURVEY, TAG), true);

                if (CurrentSurvey != null)
                    showRemarksFragment(CurrentSurvey);
            }
        });

        BurnFilesButton = (Button) findViewById(R.id.burn_files);
        BurnFilesButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                new OverwriteThread(false).start();
            }
        });

        Button hardware = (Button) findViewById(R.id.hardware);
        hardware.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent i = new Intent(getApplicationContext(),
                        com.gmeci.hardware.HardwareActivity.class);
                startActivity(i);
            }
        });
        cursorAdapter = new AZCursorAdapter(this, azpc.getAllSurveys(),
                azpc, azpc.getSetting(ATSKConstants.CURRENT_SURVEY, TAG));

        listView.setAdapter(cursorAdapter);
        registerForContextMenu(listView);
        listView.setOnCreateContextMenuListener(this);

        Intent intent = getIntent();
        if (intent != null) {
            String burnRequest = intent.getStringExtra("burnSupportFiles");
            if (burnRequest != null && burnRequest.equals("true")) {
                Log.d(TAG, "requesting an update to the existing support files");
                new OverwriteThread(true).start();
            } else {
                Log.d(TAG,
                        "not requesting an update to the existing support files");
            }
        } else {
            Log.d(TAG,
                    "not launched with any additional intents requesting action");
        }

    }

    @Override
    protected void onPause() {
        releaseService();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        initService();
        this.rebuildSurveyList();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        Cursor c = (Cursor) cursorAdapter.getItem(info.position);
        String Name = c.getString(AZURIConstants.NAME_INDEX);
        ListContextMenu = v.getId();
        menu.setHeaderTitle("Survey: " + Name);
        menu.add(0, ListContextMenu, 0, DELETE_MENU_TITLE);
        menu.add(0, ListContextMenu, 0, CANCEL_MENU_TITLE);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item
                .getMenuInfo();

        Cursor c = (Cursor) cursorAdapter.getItem(info.position);
        String Name = c.getString(AZURIConstants.UID_INDEX);
        azpc.deleteAZ(Name);

        return true;

    }

    private void initService() {
    }

    private void releaseService() {
    }

    private void rebuildSurveyList() {

        cursorAdapter = new AZCursorAdapter(this, azpc.getAllSurveys(),
                azpc, azpc.getSetting(ATSKConstants.CURRENT_SURVEY, TAG));

        listView.setAdapter(cursorAdapter);
    }

    public class OverwriteThread extends Thread {
        private final boolean finishApp;
        private ProgressDialog pd;

        public OverwriteThread(final boolean finishApp) {
            this.finishApp = finishApp;
        }

        @Override
        public void run() {
            try {
                super.run();
                final AssetHelper ah = new AssetHelper();
                ah.CopyAssetsToSDCard(MainActivity.this, "NA", true);
                synchronized (this) {
                    if (pd != null)
                        pd.dismiss();
                }
            } finally {
                if (finishApp) {
                    System.exit(0); // required to completely finish the service
                }
            }
        }

        @Override
        public synchronized void start() {
            pd = new ProgressDialog(MainActivity.this);
            pd.setTitle("Overwriting Files...");
            pd.setMessage("Please wait.");
            pd.setCancelable(false);
            pd.setIndeterminate(true);
            pd.show();
            super.start();

        }

    }

    static private class MyContentObserver extends ContentObserver {

        public MyContentObserver(Handler handler) {
            super(handler);
        }

        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            Log.d(TAG, "Changed");
        }

    }

}
