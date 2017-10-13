
package com.gmeci.hardware;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import java.nio.charset.Charset;

import com.gmeci.core.ATSKConstants;
import com.gmeci.core.SurveyPoint;
import com.gmeci.atskservice.R;
import com.gmeci.conversions.Conversions;
import com.gmeci.hardwareinterfaces.GPSCallbackInterface;
import com.gmeci.hardwareinterfaces.HardwareConsumerInterface;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.io.File;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;

public class HardwareActivity extends Activity {

    private final String TAG = "Hardware Service";

    private OutputStreamWriter writer;

    public HardwareServiceConnection hardwareServiceConnection = null;
    public HardwareConsumerInterface hardwareServiceInterface;
    protected SharedPreferences user_settings;
    Button sendRTK, ReadFile, RescanButton, Devices, Gradient,
            Gradient_DataBase, LineDataBase, RawRecord;
    ImageView gpsImage;
    TextView DeviceList;
    List<SurveyPoint> GradientList = null;
    ArrayAdapter<String> pointListAdapter;
    ArrayAdapter<String> GradientListAdapter;
    Context Activitycontext;
    String DeviceListArray[];
    TextView deviceNames, GPSLocation, GPSError, GPSTime, GPSElevation,
            RawData;
    String[] baudRates;

    private GPSCallbackInterface gpsci = new GPSCallbackInterface.Stub() {
        private SimpleDateFormat timeFormat;

        @Override
        public void UpdateGPS(final SurveyPoint gps_position, int arg1)
                throws RemoteException {
            if (timeFormat == null) {
                timeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'",
                        Locale.US);
                timeFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            }

            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    double lat, lon, alt;
                    lat = gps_position.lat;
                    lon = gps_position.lon;
                    alt = gps_position.getHAE();
                    if (GPSLocation != null)
                        GPSLocation.setText(Conversions.GetLatLonDM(lat, lon));

                    if (GPSError != null)
                        if (gps_position.circularError == 9999999) {
                            GPSError.setText("NMEA GST Message Missing");

                        } else {

                            GPSError.setText("CE: "
                                    + gps_position.circularError + "M "
                                    + "LE: " + gps_position.linearError + "M");
                        }

                    if (GPSElevation != null)
                        GPSElevation
                                .setText(Math.round(alt * Conversions.M2F)
                                        + "ft HAE or "
                                        +
                                        Math.round(Conversions.ConvertHAEtoMSL(
                                                lat, lon, alt)
                                                * Conversions.M2F) + "ft MSL");

                    if (GPSTime != null)
                        GPSTime.setText(timeFormat.format(new Date(
                                gps_position.timestamp)));

                    logToFile(GPSTime.getText()
                            + " lat: "
                            + gps_position.lat
                            + " lon:"
                            + gps_position.lon
                            + " hae: "
                            + (gps_position.getHAE() * Conversions.M2F)
                            + " msl: "
                            + (Conversions.ConvertHAEtoMSL(gps_position.lat,
                                    gps_position.lon, gps_position.getHAE()) * Conversions.M2F)
                            + " from: " + gps_position.rawInfo);
                    if (RawData != null)
                        RawData.setText(gps_position.rawInfo);

                }

            });
        }

    };
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String ActionString = intent.getAction();
            if (ActionString != null) {
                if (intent.getAction().equals(
                        ATSKConstants.GMECI_HARDWARE_GPS_ACTION)) {//LOU look here
                    ActionString = intent
                            .getStringExtra(ATSKConstants.GMECI_GPS_QUALITY);
                    if (ActionString != null) {
                        if (intent.getStringExtra(
                                ATSKConstants.GMECI_GPS_QUALITY).equals(
                                ATSKConstants.GPS_NO_CONNECTION)) {
                            gpsImage.setImageResource(R.drawable.no_gps);
                        }//end no connection
                        else if (intent.getStringExtra(
                                ATSKConstants.GMECI_GPS_QUALITY).equals(
                                ATSKConstants.GPS_INTERNAL)) {
                            gpsImage.setImageResource(R.drawable.internal_gps);
                        }//end internal
                        else if (intent.getStringExtra(
                                ATSKConstants.GMECI_GPS_QUALITY).equals(
                                ATSKConstants.GPS_EXTERNAL)) {
                            gpsImage.setImageResource(R.drawable.external_no_rtk_gps);
                        }//end external no rtk
                        else if (intent.getStringExtra(
                                ATSKConstants.GMECI_GPS_QUALITY).equals(
                                ATSKConstants.GPS_EXTERNAL_RTK)) {
                            gpsImage.setImageResource(R.drawable.external_rtk_gps);
                        }
                    }
                }
                if (ActionString == null)
                    return;
                if (ActionString
                        .equals(ATSKConstants.GMECI_HARDWARE_LRF_ACTION)) {
                    double range = 0, azimuth = 0, elevAngle = 0, alt = 0;
                    double lat = 0.0, lon = 0.0;
                    String Mode = "";

                    lat = intent.getDoubleExtra(ATSKConstants.LAT_EXTRA, lat);
                    lon = intent.getDoubleExtra(ATSKConstants.LON_EXTRA, lon);
                    alt = intent.getDoubleExtra(ATSKConstants.ALT_EXTRA, alt);

                    range = intent.getDoubleExtra(ATSKConstants.RANGE_M, range);
                    azimuth = intent.getDoubleExtra(ATSKConstants.AZIMUTH_T,
                            azimuth);
                    elevAngle = intent.getDoubleExtra(ATSKConstants.ELEVATION,
                            elevAngle);

                    String LRFString = String
                            .format("Range: %.1f Azimuth: %.1f Elevation:%.1f %n Lat: %.8f Lon:%.8f Alt:%.1f ",
                                    range, azimuth, elevAngle, lat, lon, alt);
                }
                if (ActionString.equals(ATSKConstants.BT_LIST_REPOPULATED)) {
                    String[] BT_Devices = intent
                            .getStringArrayExtra(ATSKConstants.BT_NAME);
                    boolean[] BT_Connected = intent
                            .getBooleanArrayExtra(ATSKConstants.BT_CONNECTION);
                    //int TotalPairedDevices = Integer.parseInt(intent.getExtras().get(ATSKConstants.BT_TOTAL_DEVICES).toString());
                    //int ListNumber = Integer.parseInt(intent.getExtras().get(ATSKConstants.BT_LIST_NUMBER).toString());

                    if (BT_Devices == null) {
                        BT_Devices = new String[] {
                                "No Bluetooth Device Connected"
                        };
                    }
                    DeviceListArray = BT_Devices;
                    String DeviceNames = Arrays.toString(BT_Devices);
                    DeviceNames = DeviceNames.replace(",", "\n");
                    Log.d(TAG, DeviceNames);
                    DeviceList.setText(Arrays.toString(BT_Devices) + "\n"
                            + Arrays.toString(BT_Connected));

                }
                if (ActionString.equals("OBSTRUCTION_GPS")) {
                    double lat = 0.0, lon = 0.0;
                    double alt = 0;

                    lat = intent.getDoubleExtra(ATSKConstants.LAT_EXTRA, lat);
                    lon = intent.getDoubleExtra(ATSKConstants.LON_EXTRA, lon);
                    alt = intent.getDoubleExtra(ATSKConstants.ALT_EXTRA, alt);

                    GPSLocation.setText(lat + "," + lon + "," + alt);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_hardware);

        Resources res = getResources();
        baudRates = res.getStringArray(R.array.baud);

        //COYNE this is needed for TrimbleParser to start with Hardware Service
        // will probably be the same for PLRF parser
        Intent i = new Intent();
        i.setClassName("com.gmeci.atskservice",
                "com.gmeci.hardware.HardwareService");
        i.setAction("com.gmeci.hardwareinterfaces.HardwareConsumerInterface");
        startService(i);

        Activitycontext = this;

        user_settings = PreferenceManager.getDefaultSharedPreferences(this);

        initService();

        gpsImage = (ImageView) findViewById(R.id.gps_image);

        DeviceList = (TextView) findViewById(R.id.DeviceListView);
        GPSLocation = (TextView) findViewById(R.id.GpsLocation);
        GPSElevation = (TextView) findViewById(R.id.GpsElevation);
        GPSTime = (TextView) findViewById(R.id.GpsTime);
        GPSError = (TextView) findViewById(R.id.GpsError);
        RawData = (TextView) findViewById(R.id.RawData);
        GPSLocation.setText("");
        GPSTime.setText("");
        GPSElevation.setText("");

        RescanButton = (Button) findViewById(R.id.RescanBluetooth);
        RescanButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent i = new Intent();
                i.setAction(ATSKConstants.BT_SCAN);
                sendBroadcast(i);
            }
        });

        RawRecord = (Button) findViewById(R.id.RawRecord);
        RawRecord.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                RawRecord.setSelected(!RawRecord.isSelected());
                synchronized (HardwareActivity.this) {
                    if (writer != null) {
                        try {
                            writer.close();
                        } catch (Exception e) {
                        }
                    }

                    writer = null;

                    if (RawRecord.isSelected()) {
                        RawRecord.setText("Stop Raw Record");
                        File dir = new File(ATSKConstants.LOGS_LOCATION);
                        if (!dir.exists())
                            dir.mkdir();
                        File f = new File(dir, "data-"
                                + System.currentTimeMillis() + "-log.csv");
                        try {
                            writer = new OutputStreamWriter(
                                    new FileOutputStream(f),
                                    Charset.forName("UTF-8").newEncoder());
                        } catch (Exception e) {
                        }

                    } else {
                        RawRecord.setText("Start Raw Record");
                    }

                }
            }
        });

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ATSKConstants.GMECI_HARDWARE_GPS_ACTION);
        intentFilter.addAction(ATSKConstants.GMECI_HARDWARE_LRF_ACTION);
        intentFilter.addAction(ATSKConstants.BT_LIST_REPOPULATED);
        intentFilter.addAction(ATSKConstants.BT_SCAN);
        intentFilter.addAction("OBSTRUCTION_GPS");
        registerReceiver(receiver, intentFilter);
    }

    private synchronized void logToFile(String msgToSend) {
        try {
            if (writer != null) {
                try {
                    writer.write(msgToSend + "\n");
                    writer.flush();
                } catch (Exception e) {
                    Log.w("problem writing log file", e);
                }
            }
        } catch (Exception e) {
            Log.w("problem writing log file", e);
        }
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "Reconnect");
        super.onResume();
        if (hardwareServiceInterface != null) {
            try {
                hardwareServiceInterface.register("HardwareServiceActivity",
                        gpsci);
            } catch (RemoteException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        SendSerialIntent();
    }

    @Override
    protected void onPause() {
        if (hardwareServiceInterface != null) {
            try {
                hardwareServiceInterface.unregister("HardwareServiceActivity");
            } catch (RemoteException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        super.onPause();
    }

    /**
        @Override
        public boolean onCreateOptionsMenu(Menu menu) {
            // Inflate the menu; this adds items to the action bar if it is present.
            getMenuInflater().inflate(R.menu.hardware, menu);
            return true;
        }
    **/

    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
        Log.d(TAG, "stopping the hardware service");
    }

    private void releaseService() {
        if (hardwareServiceConnection == null)
            Log.e(TAG,
                    "Attempting to unbind a null service connection to CurrentRoute");
        this.getApplicationContext().unbindService(hardwareServiceConnection);
        hardwareServiceConnection = null;
        Log.d(TAG, "Hardware Service Unbound");
    }

    private void initService() {
        hardwareServiceConnection = new HardwareServiceConnection();
        boolean success = getApplicationContext().bindService(
                getCurrentRouteInterfaceIntent(), hardwareServiceConnection,
                Context.BIND_AUTO_CREATE);
        if (success)
            Log.d("Service", "Successful Binding to Hardware Service");
        else
            Log.e("Service", "Failed Bind to Hardware Service");

    }

    private Intent getCurrentRouteInterfaceIntent() {
        Intent i = new Intent();
        i.setClassName("com.gmeci.atskservice",
                "com.gmeci.hardware.HardwareService");
        i.setAction("com.gmeci.hardwareinterfaces.HardwareConsumerInterface");
        return i;
    }//end getCurrentRouteInterfaceIntent

    //LOU THIS GOES AWAY WHEN THE REAL APPLICATION IS READY!!!
    public void SendSerialIntent() {
        Intent i = new Intent();
        i.setAction(ATSKConstants.SERIAL_CONNECTION_ACTION);
        i.putExtra(ATSKConstants.SERIAL_ATTACHED, ATSKConstants.SERIAL_ATTACHED);
        sendBroadcast(i);
    }

    private class HardwareServiceConnection implements ServiceConnection {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            try {
                hardwareServiceInterface.unregister("HardwareServiceActivity");
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            releaseService();
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            hardwareServiceInterface = HardwareConsumerInterface.Stub
                    .asInterface(service);
            //ask for GPS responses
            try {
                hardwareServiceInterface.register(
                        "HardwareServiceActivity", gpsci);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            Log.d(TAG, "hardwareServiceInterface is connected");
        }
    }
}
