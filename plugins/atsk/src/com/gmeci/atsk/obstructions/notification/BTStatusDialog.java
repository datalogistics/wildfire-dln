
package com.gmeci.atsk.obstructions.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.atakmap.android.ipc.AtakBroadcast.DocumentedIntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.atakmap.android.atsk.plugin.R;
import com.atakmap.android.ipc.AtakBroadcast;
import android.bluetooth.BluetoothDevice;
import com.gmeci.atsk.resources.ATSKApplication;
import com.gmeci.core.ATSKConstants;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import com.atakmap.android.bluetooth.BluetoothManager;

public class BTStatusDialog extends DialogFragment {

    private static String TAG = "BTStatusDialog";

    private static final String DELETE_MENU_TITLE = "DISCONNECT";
    Context context;
    Button RescanButton;
    ArrayList<String> ExistingList;
    ArrayList<String> ATAKExistingList;
    ArrayList<String> ATAKSerialExistingList;
    ArrayAdapter<String> btConnectedNameAdapter;
    ArrayAdapter<String> atakbtConnectedNameAdapter;
    ArrayAdapter<String> atakserialConnectedNameAdapter;
    int ListContextMenu = 42;
    String SelectedConnection = "";
    private ListView BTList;
    private ListView ATAKBTList;
    private ListView ATAKSerialList;

    private TextView noBT;
    private TextView noSerial;

    final OnMenuItemClickListener listener = new OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            //delete this item...
            ExistingList.remove(SelectedConnection);
            btConnectedNameAdapter.notifyDataSetChanged();

            Intent removeIntent = new Intent(
                    ATSKConstants.BT_DROP_SINGLE_CONNECTION);
            removeIntent.putExtra(ATSKConstants.BT_NAME, SelectedConnection);
            AtakBroadcast.getInstance().sendSystemBroadcast(removeIntent);
            return true;
        }
    };

    private final BroadcastReceiver ATAKbtRX = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            ATAKExistingList.clear();
            List<BluetoothDevice> list = BluetoothManager.getInstance()
                    .getConnections();
            for (BluetoothDevice d : list) {
                try {
                    ATAKExistingList.add(d.getName());
                } catch (Exception e) {
                }
            }
            if (ATAKExistingList.size() > 0 || ExistingList.size() > 0) {
                noBT.setVisibility(View.GONE);
            } else {
                noBT.setVisibility(View.VISIBLE);
            }
            atakbtConnectedNameAdapter.notifyDataSetChanged();
        }
    };

    private final BroadcastReceiver serialManagerRx = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String[] arr = intent.getStringArrayExtra("active");
            ATAKSerialExistingList.clear();

            if (arr != null) {
                for (String s : arr) {
                    String[] device = s.split("\\|");
                    if (device != null && device.length > 1)
                        ATAKSerialExistingList.add(device[1]);
                }
            }
            if (ATAKSerialExistingList.size() > 0) {
                noSerial.setVisibility(View.GONE);
            } else {
                noSerial.setVisibility(View.VISIBLE);
            }
            atakserialConnectedNameAdapter.notifyDataSetChanged();
        }
    };

    private final BroadcastReceiver btRX = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            //shouldpopulare a list for the listview

            Bundle extras = intent.getExtras();
            if (intent.getAction().equals(ATSKConstants.BT_LIST_REPOPULATED)) {
                String names[] = extras.getStringArray(ATSKConstants.BT_NAME);
                if (names != null) {
                    for (int i = 0; i < names.length; i++) {
                        if (names[i] == null)
                            names[i] = "";
                    }
                    ExistingList.clear();
                    ExistingList
                            .addAll(new HashSet<String>(Arrays.asList(names)));
                    btConnectedNameAdapter.notifyDataSetChanged();
                }

            } else if (extras.containsKey(ATSKConstants.BT_NAMES)) {
                String[] ExistingDevices = extras
                        .getStringArray(ATSKConstants.BT_NAMES);
                if (ExistingDevices != null && ExistingDevices.length > 0) {
                    ExistingList.clear();
                    Set<String> mySet = new HashSet<String>(
                            Arrays.asList(ExistingDevices));
                    ExistingList.addAll(mySet);
                    btConnectedNameAdapter.notifyDataSetChanged();
                }
            } else if (extras.containsKey(ATSKConstants.BT_NAME)) {
                String NewConnection = extras.getString(ATSKConstants.BT_NAME);
                if (NewConnection == null
                        || ExistingList.contains(NewConnection)) {

                } else
                    btConnectedNameAdapter.add(NewConnection);
            }

            if (ATAKExistingList.size() > 0 || ExistingList.size() > 0) {
                noBT.setVisibility(View.GONE);
            } else {
                noBT.setVisibility(View.VISIBLE);
            }
        }
    };

    public void setupDialog(Context context) {
        this.context = context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        Context pluginContext = ATSKApplication
                .getInstance().getPluginContext();

        View root = LayoutInflater.from(pluginContext).inflate(
                R.layout.bt_status_dialog, container);

        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        BTList = (ListView) root.findViewById(R.id.connection_listview);
        ATAKBTList = (ListView) root
                .findViewById(R.id.atak_connection_listview);
        ATAKSerialList = (ListView) root
                .findViewById(R.id.atak_serial_connection_listview);
        RescanButton = (Button) root.findViewById(R.id.rescan);

        noBT = (TextView) root.findViewById(R.id.noBT);
        noSerial = (TextView) root.findViewById(R.id.noSerial);

        RescanButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent RescanIntent = new Intent(ATSKConstants.BT_SCAN);
                AtakBroadcast.getInstance().sendSystemBroadcast(RescanIntent);

                RescanIntent = new Intent(
                        "com.atakmap.android.bluetooth.RESCAN");
                RescanIntent.putExtra("enable", true);
                AtakBroadcast.getInstance().sendBroadcast(RescanIntent);

                ExistingList.clear();
                btConnectedNameAdapter.notifyDataSetChanged();

            }

        });
        ExistingList = new ArrayList<String>();

        ATAKExistingList = new ArrayList<String>();
        List<BluetoothDevice> list = BluetoothManager.getInstance()
                .getConnections();
        for (BluetoothDevice d : list) {
            try {
                ATAKExistingList.add(d.getName());
            } catch (Exception e) {
            }
        }

        ATAKSerialExistingList = new ArrayList<String>();

        AtakBroadcast.getInstance().registerSystemReceiver(
                serialManagerRx,
                new DocumentedIntentFilter(
                        "com.partech.serialmanager.ActiveSerial"));

        Intent rasi = new Intent(
                "com.partech.serialmanager.RequestActiveSerial");
        AtakBroadcast.getInstance().sendSystemBroadcast(rasi);

        btConnectedNameAdapter = new ArrayAdapter<String>(context,
                android.R.layout.simple_list_item_1, android.R.id.text1,
                ExistingList);
        atakbtConnectedNameAdapter = new ArrayAdapter<String>(context,
                android.R.layout.simple_list_item_1, android.R.id.text1,
                ATAKExistingList);
        atakserialConnectedNameAdapter = new ArrayAdapter<String>(context,
                android.R.layout.simple_list_item_1, android.R.id.text1,
                ATAKSerialExistingList);

        BTList.setAdapter(btConnectedNameAdapter);
        ATAKBTList.setAdapter(atakbtConnectedNameAdapter);
        ATAKSerialList.setAdapter(atakserialConnectedNameAdapter);

        DocumentedIntentFilter responsefilter = new DocumentedIntentFilter();
        responsefilter.addAction(ATSKConstants.BT_DEVICE_CHANGE);
        responsefilter.addAction(ATSKConstants.BT_LIST_REPOPULATED);

        AtakBroadcast.getInstance()
                .registerSystemReceiver(btRX, responsefilter);
        registerForContextMenu(BTList);

        DocumentedIntentFilter atakresponsefilter = new DocumentedIntentFilter(
                "com.atakmap.android.bluetooth.RESCAN_COMPLETE");
        AtakBroadcast.getInstance()
                .registerReceiver(ATAKbtRX, atakresponsefilter);

        //request BT status

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent btStatusIntent = new Intent();
                btStatusIntent.setAction(ATSKConstants.BT_STATUS_REQUEST);
                AtakBroadcast.getInstance().sendSystemBroadcast(btStatusIntent);
            }
        }, 1000);

        return root;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        AtakBroadcast.getInstance().unregisterSystemReceiver(btRX);
        AtakBroadcast.getInstance().unregisterSystemReceiver(serialManagerRx);
        AtakBroadcast.getInstance().unregisterReceiver(ATAKbtRX);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        SelectedConnection = btConnectedNameAdapter.getItem(info.position);
        ListContextMenu = v.getId();
        menu.setHeaderTitle("BT Connection(" + SelectedConnection + ")");
        menu.add(0, ListContextMenu, 0, DELETE_MENU_TITLE);
        menu.getItem(0).setOnMenuItemClickListener(listener);

    }

}
