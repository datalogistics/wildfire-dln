
package com.gmeci.hardware;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;

import com.gmeci.core.ATSKConstants;
import com.gmeci.core.LineObstruction;
import com.gmeci.core.SurveyPoint;
import com.gmeci.atskservice.resolvers.ObstructionProviderClient;
import com.gmeci.atskservice.resolvers.GradientProviderClient;
import com.gmeci.constants.Constants;
import com.gmeci.conversions.Conversions;
import com.gmeci.hardwareinterfaces.GPSCallbackInterface;
import com.gmeci.hardwareinterfaces.HardwareConsumerInterface;
import com.gmeci.hardwareinterfaces.ParserInterface;
import com.gmeci.hardwareinterfaces.SerialCallbackInterface;
import com.gmeci.hardware.bluetooth.BTStateNotification;
import com.gmeci.hardware.bluetooth.BluetoothScanner;
import com.gmeci.hardware.bluetooth.BluetoothScanner.NewBTConnectionInterface;
import com.gmeci.hardware.bluetooth.BluetoothSocketListener;
import com.gmeci.hardware.bluetooth.BluetoothSocketListener.BTSocketInterface;
import com.gmeci.hardware.usb.UsbEventReceiverActivity.SendBuffer2ConsumersInterface;
import com.gmeci.hardware.usb.UsbEventReceiverActivity;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import android.location.GpsStatus.NmeaListener;

public class HardwareService extends Service implements LocationListener,
        SendBuffer2ConsumersInterface {

    public static final int SERVICE_SHUTDOWN = 10;

    private boolean gpsStarted = false;

    public static final int BUFFER_SIZE = 1024;
    private static final String TAG = "HardwareService";
    //public static final String SERVICE_READY = "com.gmeci.HardwareService";
    private static final int GPS_STATUS_UPDATE_INTERVAL = 15;
    private static final long GPS_TIMEOUT_THRESHOLD = 3000;
    public HashMap<String, CurrentOwnshipGeoPointItem> CurrentOwnshipResultMap = new HashMap<String, CurrentOwnshipGeoPointItem>();
    public boolean onCurrentRoute = false, onCurrentGradient = false;
    public Location gpsLocation;
    BluetoothAdapter bluetooth = BluetoothAdapter.getDefaultAdapter();
    double GPSHeight_m = 0;
    int LastGPSQuality = -1;

    Object lock = new Object();
    SurveyPoint CurrentOwnship = new SurveyPoint();
    Thread bluetoothScanThread;
    Thread OwnshipSelectorThread;
    DataListener dataListener;
    OwnshipSelector ownshipSelector;
    LocationManager locationManager;
    String GradientUID, GradientGroupName;
    String LineUID, LineGroupName;
    double LineLROffset_m = 0;
    Handler handler;
    ObstructionProviderClient opc;
    GradientProviderClient gpc;
    BTStateNotification btStateNotifier;
    String OldProvider = "blank";
    private ArrayList<BluetoothSocket> BTSocketList = new ArrayList<BluetoothSocket>();
    private ArrayList<Thread> ThreadListenerList = new ArrayList<Thread>();

    private String internalGpsRawInfo = "";
    private final MyNmeaListener _nmeaListener = new MyNmeaListener();

    BluetoothScanner.NewBTConnectionInterface nbti = new NewBTConnectionInterface() {

        @Override
        public boolean Connected(BluetoothSocket clientSocket, int index) {

            BTSocketList.add(clientSocket);
            BluetoothSocketListener bsl = new BluetoothSocketListener(
                    HardwareService.this, clientSocket, index, btsi);
            Thread messageListener = new Thread(bsl);
            messageListener.start();

            ThreadListenerList.add(messageListener);
            return false;
        }

        @Override
        public boolean isMACConnected(String mac) {
            for (int i = 0; i < BTSocketList.size(); i++) {
                if (BTSocketList.get(i).getRemoteDevice().getAddress()
                        .equals(mac)) {
                    return true;
                }
            }
            return false;
        }

    };
    public final BroadcastReceiver BlueToothRx = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.e(TAG, "Rescan requested: " + intent.getAction());
            if (intent.getAction().equals(ATSKConstants.BT_SCAN)
                    && !bluetoothScanThread.isAlive()) {
                //LOU why close good connections already?
                for (int i = 0; i < BTSocketList.size(); i++) {
                    try {
                        BTSocketList.get(i).close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                BTSocketList.clear();

                //if we're scaning - go ahead and let it run instead - just skip this...
                bluetoothScanThread = new Thread(new BluetoothScanner(
                        "BluetoothRescanIntent", bluetooth, nbti,
                        HardwareService.this));
                bluetoothScanThread.start();

            }
        }
    };
    // LIST to hold all of the parsers that have registered with the obstruction service
    private ArrayList<ConsumerCallbackItem> SerialDataConsumerCallbackList = new ArrayList<ConsumerCallbackItem>();
    BluetoothSocketListener.BTSocketInterface btsi = new BTSocketInterface() {

        @Override
        public boolean RemoveSocket(String mac) {
            for (int i = 0; i < BTSocketList.size(); i++) {
                if (BTSocketList.get(i).getRemoteDevice().getAddress()
                        .equals(mac)) {
                    BTSocketList.remove(i);
                    return true;
                }
            }
            return false;
        }

        @Override
        public void SendBuffer2Consumers(byte[] rollingbuffer, int StreamID) {
            SendBufferOut2Consumers(rollingbuffer, StreamID);
        }

    };
    private ArrayList<GPSCallbackItem> GPSCallbackList = new ArrayList<GPSCallbackItem>();
    private long lastSeenInternalGPS;
    private long lastInternalGPSTime;
    private BroadcastReceiver btStatusRequestRx = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(
                    ATSKConstants.BT_DROP_SINGLE_CONNECTION)) {
                Bundle bundle = intent.getExtras();
                if (bundle.containsKey(ATSKConstants.BT_NAME)) {
                    String Name2Delete = bundle
                            .getString(ATSKConstants.BT_NAME);
                    DropSingleConnection(Name2Delete);
                }
                return;
            } else if (intent.getAction().equals(
                    ATSKConstants.BT_ENABLE_REQUEST_ACTION)
                    || intent.getAction().equals(
                            ATSKConstants.BT_ENABLE_PRESSED_ACTION)) {
                if (!bluetooth.isEnabled()) {
                    bluetooth.enable();
                }
            } else if (intent.getAction().equals(
                    ATSKConstants.STOP_COLLECTING_GRADIENT_NOTIFICATION)
                    || (intent.getAction()
                            .equals(ATSKConstants.STOP_COLLECTING_ROUTE_NOTIFICATION))) {
                onCurrentGradient = false;
                onCurrentRoute = false;
                //fix the updatesdf
                btStateNotifier
                        .UPDateBTState(
                                bluetooth.isEnabled(),
                                locationManager
                                        .isProviderEnabled(LocationManager.GPS_PROVIDER),
                                onCurrentRoute, onCurrentGradient);

            } else if (intent.getAction().equals(
                    ATSKConstants.BT_MAIN_PRESSED_ACTION)) {
                //someone pushed our notification - I guess we hide it?
                btStateNotifier.Hide();
            } else if (intent.getAction().equals(
                    BluetoothAdapter.ACTION_STATE_CHANGED)) {
                if (btStateNotifier == null)
                    btStateNotifier = new BTStateNotification(
                            HardwareService.this);

                final int state = intent.getIntExtra(
                        BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_ON:
                        if (bluetoothScanThread == null)
                            bluetoothScanThread = new Thread(
                                    new BluetoothScanner("Back On RX",
                                            bluetooth, nbti,
                                            HardwareService.this));
                        handler.post(new Runnable() {

                            @Override
                            public void run() {
                                if (!bluetoothScanThread.isAlive()) {
                                    bluetoothScanThread.interrupt();
                                    try {
                                        Thread.sleep(3000);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    bluetoothScanThread = new Thread(
                                            new BluetoothScanner("Back On RX",
                                                    bluetooth, nbti,
                                                    HardwareService.this));

                                    bluetoothScanThread.start();
                                }
                            }

                        });
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        //show the notification bar
                        if (btStateNotifier != null) {
                            btStateNotifier
                                    .UPDateBTState(
                                            false,
                                            locationManager
                                                    .isProviderEnabled(LocationManager.GPS_PROVIDER),
                                            onCurrentRoute, onCurrentGradient);

                        }
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        //drop the notification bar
                        btStateNotifier
                                .UPDateBTState(
                                        true,
                                        locationManager
                                                .isProviderEnabled(LocationManager.GPS_PROVIDER),
                                        onCurrentRoute, onCurrentGradient);
                        break;

                }
            }
            //LOU put more in here
            List<String> deviceConnected = GetDevicesConnected();
            Intent btStatusIntent = new Intent();
            btStatusIntent.setAction(ATSKConstants.BT_DEVICE_CHANGE);
            btStatusIntent.putExtra(ATSKConstants.BT_CONNECTION_UPDATE_EXTRA,
                    deviceConnected.size() > 0);
            String[] devices = deviceConnected
                    .toArray(new String[deviceConnected.size()]);

            btStatusIntent.putExtra(ATSKConstants.BT_NAMES, devices);
            btStatusIntent.putExtra(ATSKConstants.BT_TOTAL_DEVICES,
                    deviceConnected.size());
            btStatusIntent.putExtra(ATSKConstants.BT_LIST_NUMBER, 0);
            HardwareService.this.sendBroadcast(btStatusIntent);
        }

        private void DropSingleConnection(String Name2Delete) {
            for (int i = 0; i < BTSocketList.size(); i++) {
                if (BTSocketList.get(i).getRemoteDevice().getName()
                        .equals(Name2Delete)) {
                    try {
                        BTSocketList.get(i).close();
                        BTSocketList.remove(i);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }
        }
    };

    public HardwareService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "Hardware Service -- Binding");
        if (ParserInterface.class.getName().equals(intent.getAction())) {
            return new ParserInterface.Stub() {
                @Override
                public void NewOwnshipPoint(double lat, double lon, double hae,
                        String id, int LockQuality, double ce_m, double le_m,
                        double course, double speed, String Source,
                        long timestamp, String rawInfo)
                        throws RemoteException {
                    //how do we know when to draw a current path and when not to?
                    CurrentOwnshipGeoPointItem newownshipItem;
                    if (CurrentOwnshipResultMap.containsKey(id)) {
                        newownshipItem = CurrentOwnshipResultMap.get(id);
                    } else {
                        newownshipItem = new CurrentOwnshipGeoPointItem();
                    }
                    newownshipItem.Update(lat, lon, hae, id, LockQuality, ce_m,
                            le_m, course, speed, timestamp, rawInfo);
                    CurrentOwnshipResultMap.put(id, newownshipItem);//does this have to happen if it existed already?
                }

                @Override
                public boolean RegisterConsumer(String id,
                        SerialCallbackInterface callback)
                        throws RemoteException {

                    Log.d(TAG, "Registering SerialCallbackInterface Consumer "
                            + id);

                    //even if it's on the list already ...  it might be updating or something - always take an update
                    ConsumerCallbackItem newItem = new ConsumerCallbackItem();
                    newItem.ConsumerName = id;
                    newItem.eventCallback = callback;
                    synchronized (SerialDataConsumerCallbackList) {
                        for (ConsumerCallbackItem currentItem : SerialDataConsumerCallbackList) {
                            if (currentItem.ConsumerName.equals(id)) {
                                //LOU should we update the callback?
                                return true;
                            }
                        }
                        SerialDataConsumerCallbackList.add(newItem);
                    }
                    if (SerialDataConsumerCallbackList.size() > 0) {
                        startGps();
                    }
                    return false;
                }

            };
        } else if (HardwareConsumerInterface.class.getName().equals(
                intent.getAction())) {
            return new HardwareConsumerInterface.Stub() {
                @Override
                public SurveyPoint getMostRecentPoint()
                        throws RemoteException {

                    synchronized (lock) {
                        if (CurrentOwnship.collectionMethod == null)
                            CurrentOwnship.collectionMethod = SurveyPoint.CollectionMethod.MANUAL;
                        return CurrentOwnship;
                    }
                }

                public boolean unregister(final String id)
                        throws RemoteException {
                    synchronized (GPSCallbackList) {
                        Log.d(TAG, "unregister:" + id);
                        GPSCallbackItem removed = null;
                        for (GPSCallbackItem currentItem : GPSCallbackList) {
                            if (currentItem.ConsumerName.equals(id)) {
                                removed = currentItem;
                            }
                        }
                        if (removed != null)
                            GPSCallbackList.remove(removed);
                    }
                    return true;

                }

                public boolean register(String id,
                        GPSCallbackInterface callback) throws RemoteException {

                    Log.d(TAG, "RegisterGPSPositionConsumer:" + id
                            + " callback is " + callback);

                    for (GPSCallbackItem currentItem : GPSCallbackList) {
                        if (currentItem.ConsumerName.equals(id)) {
                            currentItem.ConsumerName = id;
                            currentItem.NewDataCallback = callback;
                            return true;
                        }
                    }
                    //add new item to list...
                    GPSCallbackItem newItem = new GPSCallbackItem();
                    newItem.ConsumerName = id;
                    newItem.NewDataCallback = callback;

                    synchronized (GPSCallbackList) {
                        GPSCallbackList.add(newItem);
                    }

                    handler.post(new Runnable() {
                        public void run() {
                            startGps();
                        }
                    });

                    return false;
                }

                @SuppressWarnings({
                        "unchecked", "rawtypes"
                })
                @Override
                public void EndCurrentRoute(boolean Debug)
                        throws RemoteException {

                    onCurrentRoute = false;
                    onCurrentGradient = false;
                    if (bluetooth != null)
                        btStateNotifier
                                .UPDateBTState(
                                        bluetooth.isEnabled(),
                                        locationManager
                                                .isProviderEnabled(LocationManager.GPS_PROVIDER),
                                        onCurrentRoute, onCurrentGradient);

                }

                @Override
                public void StartNewGradientRoute(String uid,
                        String description, String groupName, double GPSHeight)
                        throws RemoteException {
                    GradientUID = uid;
                    GradientGroupName = groupName;
                    GPSHeight_m = GPSHeight;
                    onCurrentGradient = true;
                    onCurrentRoute = false;
                    if (bluetooth != null)
                        btStateNotifier
                                .UPDateBTState(
                                        bluetooth.isEnabled(),
                                        locationManager
                                                .isProviderEnabled(LocationManager.GPS_PROVIDER),
                                        onCurrentRoute, onCurrentGradient);

                    if (!gpc.GradientExists(groupName, uid)) {
                        LineObstruction newLine = new LineObstruction();
                        if (description.length() < 2) {
                            description = String.format("GPS@%.1fft",
                                    GPSHeight_m * Conversions.M2F);
                        }
                        newLine.remarks = description;
                        newLine.uid = uid;
                        newLine.group = groupName;
                        gpc.NewGradient(newLine);
                    }

                }

                boolean isLine(String Type) {
                    for (String LineType : Constants.LINE_TYPES) {
                        if (LineType.equals(Type))
                            return true;
                    }
                    return false;
                }

                @Override
                public void StartNewLineRoute(String uid, String type,
                        String groupName, double GPSHeight, double WidthOffset_m)
                        throws RemoteException {
                    LineUID = uid;
                    LineLROffset_m = WidthOffset_m;
                    LineGroupName = groupName;
                    GPSHeight_m = GPSHeight;
                    //check if line already exists and add it if not...
                    if (!opc.LineExists(groupName, uid)) {
                        LineObstruction newLine = new LineObstruction();
                        newLine.remarks = uid;
                        newLine.uid = uid;
                        newLine.type = type;
                        newLine.filled = newLine.closed = !isLine(type);

                        newLine.group = groupName;
                        opc.NewLine(newLine, false);
                    }
                    onCurrentRoute = true;
                    onCurrentGradient = false;
                    if (bluetooth != null)
                        btStateNotifier
                                .UPDateBTState(
                                        bluetooth.isEnabled(),
                                        locationManager
                                                .isProviderEnabled(LocationManager.GPS_PROVIDER),
                                        onCurrentRoute, onCurrentGradient);
                }

                @Override
                public boolean isCollectingGradient() throws RemoteException {
                    return onCurrentGradient;
                }

                @Override
                public boolean isCollectingRoute() throws RemoteException {
                    return onCurrentRoute;
                }

            };
        }
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler();

        btStateNotifier = new BTStateNotification(this);
        if (bluetooth != null)
            bluetoothScanThread = new Thread(new BluetoothScanner(
                    "Constructor", bluetooth, nbti, HardwareService.this));

        new Thread(dataListener = new DataListener()).start();

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        startGps();

        if (bluetooth != null && bluetooth.isEnabled()) {
            btStateNotifier.Hide();
            if (!bluetoothScanThread.isAlive()) {
                bluetoothScanThread.start();
            }
        }
        if (bluetooth != null)
            btStateNotifier.UPDateBTState(bluetooth.isEnabled(),
                    locationManager
                            .isProviderEnabled(LocationManager.GPS_PROVIDER),
                    onCurrentRoute, onCurrentGradient);
        gpc = new GradientProviderClient(this);
        gpc.Start();
        opc = new ObstructionProviderClient(this);
        if (!opc.Start()) {
            Log.e(TAG, "Failed to connect to opc");
        }
        SetupBTReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ATSKConstants.BT_SCAN);
        this.registerReceiver(BlueToothRx, filter);

        UsbEventReceiverActivity.setParent(this);

        ParserManager.startParserServices(this);
        //Start GPS selector thread....
        ownshipSelector = new OwnshipSelector();

        OwnshipSelectorThread = new Thread(ownshipSelector);
        OwnshipSelectorThread.start();
    }

    private void SetupBTReceiver() {
        IntentFilter filterBT = new IntentFilter();
        filterBT.addAction(ATSKConstants.BT_STATUS_REQUEST);
        filterBT.addAction(ATSKConstants.BT_DROP_SINGLE_CONNECTION);
        filterBT.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filterBT.addAction(ATSKConstants.BT_ENABLE_PRESSED_ACTION);
        filterBT.addAction(ATSKConstants.STOP_COLLECTING_GRADIENT_NOTIFICATION);
        filterBT.addAction(ATSKConstants.STOP_COLLECTING_ROUTE_NOTIFICATION);
        filterBT.addAction(ATSKConstants.BT_MAIN_PRESSED_ACTION);
        this.registerReceiver(btStatusRequestRx, filterBT);
    }

    private synchronized void stopGps() {
        try {
            locationManager.removeUpdates(this);
            locationManager.removeNmeaListener(_nmeaListener);
        } catch (Exception e) {
            Log.d(TAG, "unable to remove listener");
        }
        gpsStarted = false;
    }

    private synchronized void startGps() {

        if (!gpsStarted) {
            Intent i = new Intent();
            i.setAction(ATSKConstants.GMECI_HARDWARE_GPS_ACTION);
            i.putExtra(ATSKConstants.GMECI_GPS_QUALITY,
                    ATSKConstants.GPS_INTERNAL);
            sendBroadcast(i);

            try {
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER, 1000, 0f,
                        this);
                locationManager.addNmeaListener(_nmeaListener);
            } catch (IllegalArgumentException iae) {
                Log.d(TAG, "device does not have GPS");

            }

            gpsStarted = true;
        }

    }

    @Override
    public void onDestroy() {

        dataListener.cancel();
        ownshipSelector.cancel();
        stopGps();
        unregisterReceiver(BlueToothRx);
        opc.Stop();
        gpc.Stop();
        btStateNotifier.Hide();
        super.onDestroy();

    }

    private void SendGPSQuality(int GPSQualityInt, boolean UseInternal,
            boolean RTKLost) {

        Log.d(TAG, "GPSQualityInt: " + GPSQualityInt + " UseInternal: "
                + UseInternal + " RTKLost: " + RTKLost);
        Intent i = new Intent();
        i.setAction(ATSKConstants.GMECI_HARDWARE_GPS_ACTION);
        if (GPSQualityInt >= 2) {
            Log.d(TAG, "broadcasting quality level RTK");
            i.putExtra(ATSKConstants.GMECI_GPS_QUALITY,
                    ATSKConstants.GPS_EXTERNAL_RTK);
        } else if (UseInternal && GPSQualityInt >= 0) {
            Log.d(TAG, "broadcasting quality level GPS");
            i.putExtra(ATSKConstants.GMECI_GPS_QUALITY,
                    ATSKConstants.GPS_INTERNAL);
        } else if (GPSQualityInt >= 0) {
            Log.d(TAG, "broadcasting quality level GPS");
            i.putExtra(ATSKConstants.GMECI_GPS_QUALITY,
                    ATSKConstants.GPS_EXTERNAL);
        } else {
            Log.d(TAG, "broadcasting no connection");
            i.putExtra(ATSKConstants.GMECI_GPS_QUALITY,
                    ATSKConstants.GPS_NO_CONNECTION);
        }
        if (RTKLost)
            i.putExtra(ATSKConstants.GPS_ALERT,
                    ATSKConstants.GPS_ALERT_LOST_RTK);
        sendBroadcast(i);
    }

    public void SendBufferOut2Consumers(byte[] rollingbuffer, int StreamID) {
        synchronized (SerialDataConsumerCallbackList) {
            Iterator<ConsumerCallbackItem> iter = SerialDataConsumerCallbackList
                    .iterator();
            while (iter.hasNext()) {
                ConsumerCallbackItem currentItem = iter.next();
                try {
                    currentItem.eventCallback.SendData(rollingbuffer,
                            rollingbuffer.length, StreamID);
                } catch (RemoteException e) {
                    //failed - remove from the list...
                    iter.remove();
                }
            }//done looping through all data consumers
        }
    }

    int count = 0;

    private void updateGPS(final SurveyPoint currentPoint) {

        synchronized (GPSCallbackList) {
            if (GPSCallbackList.size() == 0) {
                count++;
                if (count < SERVICE_SHUTDOWN) {
                    Log.d(TAG, "no listeners exist, counting " + count);
                    return;
                } else {
                    stopGps();
                    return;
                }
            } else {
                count = 0;
            }

            Iterator<GPSCallbackItem> iter = GPSCallbackList.iterator();
            while (iter.hasNext()) {
                GPSCallbackItem currentItem = iter.next();
                try {
                    if (currentItem != null && currentPoint != null
                            && currentItem.NewDataCallback != null) {
                        currentItem.NewDataCallback.UpdateGPS(currentPoint,
                                (int) currentPoint.circularError);
                    } else {
                        iter.remove();
                        Log.d(TAG, " Removing item at " + currentItem
                                + " for Current Point: " + currentPoint);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "removing orphan item from list", e);
                    iter.remove();
                }

            }//done looping through all data consumers
        }
    }

    public List<String> GetDevicesConnected() {
        List<String> deviceConnected = new ArrayList<String>();
        for (BluetoothSocket currentSocket : BTSocketList) {
            deviceConnected.add(currentSocket.getRemoteDevice().getName());
        }
        return deviceConnected;
    }

    private void Sleep(long msec) {

        try {
            Thread.sleep(msec);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

        if (OldProvider.compareTo(provider) == 0) {
        } else {
            Log.d("Status", "changed: " + provider);
            OldProvider = provider;
            if (gpsLocation == null) {
                Intent i = new Intent();
                i.setAction(ATSKConstants.GMECI_GPS_QUALITY);
                i.putExtra(ATSKConstants.GMECI_GPS_QUALITY,
                        ATSKConstants.GPS_INTERNAL);
                sendBroadcast(i);
            }
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if (gpsLocation == null) {
            Intent i = new Intent();
            i.setAction(ATSKConstants.GMECI_GPS_QUALITY);
            i.putExtra(ATSKConstants.GMECI_GPS_QUALITY,
                    ATSKConstants.GPS_INTERNAL);
            sendBroadcast(i);
        }
        gpsLocation = location;

        if (lastInternalGPSTime < location.getTime()) {
            lastInternalGPSTime = location.getTime();
            lastSeenInternalGPS = SystemClock.elapsedRealtime();
        }

    }

    @Override
    public void onProviderDisabled(String provider) {
        internalGpsRawInfo = "";
        Intent i = new Intent();
        i.setAction(ATSKConstants.GMECI_HARDWARE_GPS_ACTION);
        i.putExtra(ATSKConstants.GMECI_GPS_QUALITY,
                ATSKConstants.GPS_NO_CONNECTION);
        sendBroadcast(i);
        Log.d("Provider", "disabled: " + provider);

        btStateNotifier.UPDateBTState(bluetooth.isEnabled(), false,
                onCurrentRoute, onCurrentGradient);
    }

    @Override
    public void onProviderEnabled(String provider) {
        internalGpsRawInfo = "";
        Intent i = new Intent();
        i.setAction(ATSKConstants.GMECI_HARDWARE_GPS_ACTION);
        i.putExtra(ATSKConstants.GMECI_GPS_QUALITY, ATSKConstants.GPS_INTERNAL);
        sendBroadcast(i);

        //update notification
        btStateNotifier.UPDateBTState(bluetooth.isEnabled(), true,
                onCurrentRoute, onCurrentGradient);
        Log.d("Provider", "enabled: " + provider);
    }

    private class DataListener implements Runnable {
        DatagramSocket s;
        byte[] receiveData = new byte[64 * 1024];
        DatagramPacket receivePacket = new DatagramPacket(receiveData,
                receiveData.length);

        boolean cancelled = false;

        public void cancel() {
            cancelled = true;
            try {
                s.close();

            } catch (Exception e) {
            }

        }

        public void run() {
            try {
                Log.d(TAG, "starting test data listener");

                InetAddress address = InetAddress.getByName("239.200.200.150");
                Log.d(TAG, "ip address: " + address);
                int port = 31976;
                s = new DatagramSocket(port);
                while (!cancelled && !s.isClosed()) {
                    receivePacket.setLength(receiveData.length);
                    s.receive(receivePacket);
                    String data = new String(receiveData, 0, receivePacket
                            .getLength());

                    Log.d(TAG, "receive test data: " + data);
                    try {
                        String d[] = data.split(",");
                        CurrentOwnshipGeoPointItem noi = new CurrentOwnshipGeoPointItem();
                        int lt = 0;
                        if (d.length > 3)
                            lt = Integer.parseInt(d[3]);

                        noi.Update(Double.parseDouble(d[0]),
                                Double.parseDouble(d[1]),
                                Double.parseDouble(d[2]),
                                "externaltest", lt, 16, 16, 0, 0,
                                new java.util.Date().getTime(), data);
                        CurrentOwnshipResultMap.put("externaltest", noi);
                    } catch (Exception e) {
                        Log.d(TAG, "error reading test data: " + data, e);
                    }

                }
            } catch (Exception er) {
                Log.d(TAG, "error occurred: ", er);
            }
        }

    }

    private class OwnshipSelector implements Runnable {

        final SurveyPoint BestOwnship = new SurveyPoint();
        boolean BestOwnshipFilled = false;
        boolean cancelled = false;

        public void cancel() {
            cancelled = true;
        }

        @Override
        public void run() {

            //Sleep(5000);
            int SlowTimer = 0;
            while (!cancelled) {
                SlowTimer++;
                Sleep(1000);
                //iterate over all GPS points - decrement validity and send the best position out...
                int BestSourceLock = -1;
                for (CurrentOwnshipGeoPointItem ownship : CurrentOwnshipResultMap
                        .values()) {
                    if (ownship.Timer > 0) {
                        if (ownship.LockType > BestSourceLock) {
                            BestOwnship.setHAE(ownship.hae);
                            BestOwnship.lat = ownship.lat;
                            BestOwnship.lon = ownship.lon;
                            BestSourceLock = ownship.LockType;
                            BestOwnship.circularError = ownship.ce;
                            BestOwnship.linearError = ownship.le;
                            BestOwnshipFilled = true;
                            BestOwnship.speed = ownship.speed;
                            BestOwnship.course_true = ownship.course;
                            BestOwnship.timestamp = ownship.timestamp;
                            BestOwnship.rawInfo = ownship.rawInfo;
                            if (ownship.LockType >= 2) {
                                BestOwnship.collectionMethod = SurveyPoint.CollectionMethod.RTK;
                            } else {
                                BestOwnship.collectionMethod = SurveyPoint.CollectionMethod.EXTERNAL_GPS;
                            }
                        }
                        ownship.Timer--;
                    }

                }//loop through all possible GPS sources...

                boolean UseInternal = false;

                //LOU add something to tell us when we have no internal GPS also...
                if (BestSourceLock < 0 && gpsLocation != null) {
                    BestSourceLock = 0;
                    //we need to use internal GPS
                    BestOwnship.lat = gpsLocation.getLatitude();
                    BestOwnship.lon = gpsLocation.getLongitude();
                    BestOwnship.setHAE(gpsLocation.getAltitude());
                    BestOwnship.circularError = gpsLocation.getAccuracy();
                    BestOwnship.linearError = gpsLocation.getAccuracy();
                    BestOwnship.course_true = gpsLocation.getBearing();
                    BestOwnship.timestamp = gpsLocation.getTime();
                    BestOwnship.collectionMethod = SurveyPoint.CollectionMethod.INTERNAL_GPS;
                    BestOwnship.rawInfo = internalGpsRawInfo;
                    UseInternal = true;
                    BestOwnshipFilled = true;

                    // shb - fix

                    if (SystemClock.elapsedRealtime() - lastSeenInternalGPS > GPS_TIMEOUT_THRESHOLD) {
                        //Log.d(TAG, "internal gps lost and that is what we were gonna use");
                        BestOwnshipFilled = false;
                        BestSourceLock = -1;
                        UseInternal = false;
                    }
                }

                if (BestSourceLock < 0)
                    BestOwnshipFilled = false;

                if (BestSourceLock != LastGPSQuality
                        || SlowTimer % GPS_STATUS_UPDATE_INTERVAL == 0) {//tell everyone the GPS quality has changed
                    boolean RTKLost = false;
                    if (LastGPSQuality >= 2 && BestSourceLock < 2) {
                        RTKLost = true;
                    }
                    LastGPSQuality = BestSourceLock;

                    SendGPSQuality(LastGPSQuality, UseInternal, RTKLost);
                }

                if (BestOwnshipFilled) {
                    BestOwnship.setHAE(BestOwnship.getHAE() - GPSHeight_m);
                    synchronized (lock) {
                        CurrentOwnship = BestOwnship;
                        updateGPS(CurrentOwnship);
                        if (onCurrentGradient) {

                            gpc.GradientAppendPoint(GradientGroupName,
                                    GradientUID, BestOwnship);
                        } else if (onCurrentRoute) {
                            //LineLROffset_m;
                            Conversions.AROffset(CurrentOwnship.lat,
                                    CurrentOwnship.lon,
                                    CurrentOwnship.course_true + 90,
                                    LineLROffset_m);
                            opc.LineAppendPoint(LineGroupName, LineUID,
                                    CurrentOwnship);
                        }
                    }
                }
            }//loop until the service dies

        }

    }

    private class MyNmeaListener implements NmeaListener {
        public MyNmeaListener() {
        }

        public void onNmeaReceived(final long timestamp, final String nmea) {
            if (nmea.startsWith("$GPGGA")) {
                internalGpsRawInfo = nmea;
            }
        }

    }

}
