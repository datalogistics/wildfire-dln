
package com.gmeci.hardware.usb;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.util.Log;
import java.util.*;

import com.hoho.android.usbserial.driver.*;

public class UsbEventReceiverActivity extends Activity
{

    public final static String TAG = "UsbEventReceiver";

    private static final String ACTION_USB_PERMISSION = "com.gmeci.hardware.serialmanager.USB_PERMISSION";

    public Map<String, SerialDataListener> serialDevices = new HashMap<String, SerialDataListener>();

    private USBReceiver usbr;
    private UsbMonitor monitor;

    static private SendBuffer2ConsumersInterface parent;

    /**
     * Responsible for handling the case where the USB device is removed from the system or the case
     * where a permission request is needed prior to continuing.
     */
    class USBReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            UsbDevice d = (UsbDevice) intent
                    .getParcelableExtra(UsbManager.EXTRA_DEVICE);
            if (d == null)
                return;

            if (ACTION_USB_PERMISSION.equals(action)) {
            } else if (intent.getAction().equalsIgnoreCase(
                    UsbManager.ACTION_USB_DEVICE_DETACHED)) {

                String foundKey = null;
                for (String key : serialDevices.keySet()) {
                    if (serialDevices.get(key).getDevice()
                            .getDeviceId() == d.getDeviceId()) {
                        foundKey = key;
                    }
                }
                if (foundKey != null) {
                    SerialDataListener serialListener = serialDevices
                            .remove(foundKey);

                    if (serialListener != null) {
                        Log.d(TAG,
                                "cable disconnected: "
                                        + serialListener.toString()
                                        + " session: " + d.getDeviceId());
                        serialListener.cancel();
                    }
                }
            } else {
                Log.d(TAG, "unknown event: " + intent.getAction());
            }
        }
    }

    /**
     * Thread to continually monitor the USB device list and when a new UsbDevice is found that
     * is an FTDI based chip, then start listening.
     */
    public class UsbMonitor implements Runnable {
        private boolean cancelled = false;

        public void cancel() {
            cancelled = true;
        }

        public void run() {
            UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);

            while (!cancelled) {
                HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
                Iterator<UsbDevice> deviceIterator = deviceList.values()
                        .iterator();

                while (deviceIterator.hasNext()) {
                    final UsbDevice device = deviceIterator.next();
                    if (device.getVendorId() == 1027) {

                        UsbDeviceConnection con = null;

                        try {
                            con = manager.openDevice(device);
                        } catch (Exception e) {
                        }
                        if (con != null) {
                            String serialNumber = con.getSerial();
                            con.close();

                            if (serialNumber == null) {
                                serialNumber = device.getVendorId() + ":"
                                        + device.getProductId();
                                Log.d(TAG,
                                        "device has no serial number, log the device and vendor id["
                                                + serialNumber + "]");
                            }

                            final String fsn = serialNumber;

                            if ((serialDevices.get(fsn) == null)) {
                                runOnUiThread(new Runnable() {
                                    public void run() {
                                        startListener(fsn,
                                                device);
                                    }
                                });
                            }
                        }
                    }

                }
                try {
                    Thread.sleep(5000);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

        }
    }

    /**
     * Properly construct the listener for a specific device keyed with the serial number.
     * If startListener is called and there is already a thread managing that device, cancel the
     * thread and recreate it.
     */
    synchronized private void startListener(final String serialNumber,
            final UsbDevice d) {
        SerialDataListener sdl;

        sdl = serialDevices.remove(serialNumber);
        if (sdl != null)
            sdl.cancel();

        sdl = new SerialDataListener(d, serialNumber, 4800);
        Log.d(TAG, "inserted: " + sdl.toString());

        serialDevices.put(serialNumber, sdl);

        Thread t = new Thread(sdl);
        t.start();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        usbr = new USBReceiver();
        IntentFilter iFilter = new IntentFilter();
        iFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        this.registerReceiver(usbr, iFilter);
        monitor = new UsbMonitor();
        new Thread(monitor).start();
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart()");
        Intent intent = getIntent();

        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);

        UsbDevice d = (UsbDevice) intent
                .getParcelableExtra(UsbManager.EXTRA_DEVICE);

        if (d != null) {
            Log.d(TAG, "detected plug event for vendor: " + d.getVendorId() +
                    " product: " + d.getProductId());

            // request permission before continuing.   if permission is already granted, nothing should occur.
            PendingIntent mPermissionIntent = PendingIntent.getBroadcast(this,
                    0, new Intent(ACTION_USB_PERMISSION), 0);

            try {
                manager.requestPermission(d, mPermissionIntent);
            } catch (java.lang.SecurityException se) {
                Log.d(TAG,
                        "\n\n\n***NOTE***:   If you are seeing this message and unable to get the serial monitor to work, you may need to enable Device Administrator for Serial Monitor under Settings->General->Security\n\n\n");
            }
            onBackPressed();
            intent.removeExtra(UsbManager.EXTRA_DEVICE);
        }

        listAllSerialDevices();
    }

    public void listAllSerialDevices() {
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);

        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values()
                .iterator();

        Log.d(TAG, "------ Attached Devices --------------------------------");
        int num = 0;
        while (deviceIterator.hasNext()) {
            final UsbDevice device = deviceIterator.next();
            num++;
            Log.d(TAG, num + ") VendorID: " + device.getVendorId()
                    + " ProductID: " + device.getProductId());
        }
        Log.d(TAG, "--------------------------------------------------------");
    }

    public class SerialDataListener implements Runnable {
        final UsbDevice device;
        final String serialNumber;
        final int baud;
        boolean cancelled = false;

        /**
         * @param device the UsbDevice to be listened on.
         */
        public SerialDataListener(
                final UsbDevice device,
                final String serialNumber,
                final int baud) {
            this.device = device;
            this.serialNumber = serialNumber;
            this.baud = baud;
        }

        public UsbDevice getDevice() {
            return device;
        }

        public void cancel() {
            cancelled = true;
        }

        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public void run() {
            // Get UsbManager from Android.
            UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);

            if (device == null) {
                Log.d(TAG, "no device detected");
                return;
            }

            // Find the first available driver.
            UsbSerialDriver driver = UsbSerialProber.getDefaultProber()
                    .probeDevice(device);

            byte[] buffer = new byte[4096];
            if (driver != null) {

                UsbSerialPort port = null;

                try {
                    List<UsbSerialPort> lusb = driver.getPorts();
                    port = lusb.get(0);
                    port.open(manager.openDevice(device));

                    port.setParameters(baud, 8, UsbSerialPort.STOPBITS_1,
                            UsbSerialPort.PARITY_NONE);
                    StringBuilder val = new StringBuilder();
                    while (!isCancelled()) {
                        int numBytesRead = port.read(buffer, 1000);//Read 10 bytes from serial port
                        val.append(new String(buffer, 0, numBytesRead));
                        String line = val.toString();
                        int index = line.indexOf('\r');
                        if (index > 0) {
                            val.delete(0, index + 1);
                            String processed = line.substring(0, index)
                                    .replace(
                                            "\n", "");
                            Log.d(TAG, processed);
                            if (parent != null)
                                parent.SendBufferOut2Consumers(
                                        processed.getBytes("UTF-8"),
                                        serialNumber.hashCode());

                        }
                    }

                } catch (Exception ex) {
                    Log.d(TAG, "exception: ", ex);
                } finally {
                    try {
                        if (port != null)
                            port.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public static void setParent(final SendBuffer2ConsumersInterface p) {
        parent = p;
    }

    public interface SendBuffer2ConsumersInterface {
        void SendBufferOut2Consumers(byte[] buffer, int id);
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    public void onResume() {
        super.onResume();
        onBackPressed();
    }

    public void onDestroy() {
        super.onDestroy();
        monitor.cancel();
        unregisterReceiver(usbr);
        for (SerialDataListener value : serialDevices.values())
            value.cancel();

    }

}
