
package com.gmeci.hardware.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.gmeci.core.ATSKConstants;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class BluetoothScanner implements Runnable {

    public static final String TAG = "BluetoothScanner";

    private static final UUID MY_UUID_INSECURE = UUID
            .fromString("00001101-0000-1000-8000-00805F9B34FB");
    BluetoothAdapter bluetooth;
    Context context;
    NewBTConnectionInterface nbti;
    boolean[] BT_Connected;
    String[] name;

    public BluetoothScanner(String Requester, BluetoothAdapter bluetooth,
            NewBTConnectionInterface nbti, Context context) {
        this.bluetooth = bluetooth;
        this.context = context;
        this.nbti = nbti;
    }

    @Override
    public void run() {
        ScanBluetooth();
    }

    public String[] GetNameList() {
        return name;
    }

    public boolean[] GetConnectedList() {
        return BT_Connected;
    }

    /**
     * Attempt to establish a connection using the least secure protocol but 
     * generally is considered the most sucessful.
     */
    private BluetoothSocket tryConnect3(BluetoothDevice device)
            throws IOException {
        BluetoothSocket bs = device
                .createInsecureRfcommSocketToServiceRecord(MY_UUID_INSECURE);
        try {
            Thread.sleep(500);
        } catch (Exception e) {
        }
        return bs;
    }

    /**
     * Attempt to establish a secure connection using the insecure uuid.
     */
    private BluetoothSocket tryConnect1(BluetoothDevice device)
            throws IOException {
        BluetoothSocket bs = device
                .createRfcommSocketToServiceRecord(MY_UUID_INSECURE);
        try {
            Thread.sleep(500);
        } catch (Exception e) {
        }
        return bs;
    }

    /**
     * Attempt to establish a connection by use of reflection.   Helps on some 
     * devices.
     */
    private BluetoothSocket tryConnect2(BluetoothDevice device) {
        Method m;
        try {
            m = device.getClass().getMethod("createRfcommSocket", new Class[] {
                    int.class
            });
            BluetoothSocket s = (BluetoothSocket) m.invoke(device, 1);
            return s;
        } catch (SecurityException e) {
            Log.e(TAG, "create() failed", e);
        } catch (NoSuchMethodException e) {
            Log.e(TAG, "create() failed", e);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "create() failed", e);
        } catch (IllegalAccessException e) {
            Log.e(TAG, "create() failed", e);
        } catch (InvocationTargetException e) {
            Log.e(TAG, "create() failed", e);
        }
        return null;

    }

    private boolean good(final BluetoothSocket s) {
        if (s != null) {
            if (s.isConnected())
                return true;
            try {
                s.close();
            } catch (Exception e) {
            }
        }
        return false;
    }

    public void ScanBluetooth() {
        String mac = "";
        Set<BluetoothDevice> pairedDevices = bluetooth.getBondedDevices();

        bluetooth.cancelDiscovery();

        if (pairedDevices == null) {
            Log.d(TAG, "error finding bonded devices, exiting scanning process");
            return;
        }

        name = new String[pairedDevices.size()];
        BT_Connected = new boolean[pairedDevices.size()];

        int i = 0;
        for (BluetoothDevice deviceOnPairedList : pairedDevices) {
            if (!bluetooth.isEnabled()
                    || Thread.currentThread().isInterrupted()) {
                return;
            }
            BluetoothSocket clientSocket = null;
            mac = deviceOnPairedList.getAddress();
            BluetoothDevice device = bluetooth.getRemoteDevice(mac);

            Intent BT_intent = new Intent();
            BT_intent.setAction(ATSKConstants.BT_LIST_REPOPULATED);

            final String dname = device.getName();

            boolean connect = true;
            // black list all of the ATAK internally handled bluetooth devices 
            if (dname == null ||
                    dname.startsWith("PLRF Wireless") ||
                    dname.startsWith("MOSKITIO") ||
                    dname.startsWith("PLRF25C") ||
                    dname.startsWith("Vector21") ||
                    dname.startsWith("Bad Elf") ||
                    dname.startsWith("TP360")) {
                Log.d(TAG, "bluetooth device: " + dname
                        + " is internally handled by ATAK");
                connect = false;
            }

            if (!nbti.isMACConnected(mac) && connect) {

                try {
                    if (!good(clientSocket)) {
                        try {
                            clientSocket = tryConnect1(device);
                            name[i] = clientSocket.getRemoteDevice().getName();
                            clientSocket.connect();
                            Log.d(TAG, "succesful connection using method 1");
                        } catch (Exception e) {
                            Log.d(TAG, "failure to connect trying method 1 " +
                                    deviceOnPairedList.getName());
                        }
                    }
                    if (!good(clientSocket)) {
                        try {
                            clientSocket = tryConnect2(device);
                            name[i] = clientSocket.getRemoteDevice().getName();
                            clientSocket.connect();
                            Log.d(TAG, "succesful connection using method 2");
                        } catch (Exception e) {
                            Log.d(TAG, "failure to connect trying method 2 " +
                                    deviceOnPairedList.getName());
                        }
                    }
                    if (!good(clientSocket)) {
                        clientSocket = tryConnect3(device);
                        name[i] = clientSocket.getRemoteDevice().getName();
                        clientSocket.connect();
                        Log.d(TAG, "succesful connection using method 3");
                    }
                    nbti.Connected(clientSocket, i);
                    BT_Connected[i] = true;

                } catch (IOException e) {
                    Log.d(TAG, "failure to connect trying method 3 " +
                            deviceOnPairedList.getName());
                    Log.e(TAG,
                            "Did not connect " + deviceOnPairedList.getName()
                                    + " " + deviceOnPairedList.getAddress(), e);
                    BT_Connected[i] = false;
                }

                BT_intent.putExtra(ATSKConstants.BT_CONNECTION, BT_Connected);
                BT_intent.putExtra(ATSKConstants.BT_NAME, name);
                BT_intent.putExtra(ATSKConstants.BT_TOTAL_DEVICES,
                        pairedDevices.size());
                BT_intent.putExtra(ATSKConstants.BT_CONNECTION_UPDATE_EXTRA,
                        pairedDevices.size() > 0);

                BT_intent.putExtra(ATSKConstants.BT_LIST_NUMBER, i);
                context.sendBroadcast(BT_intent);
                i++;
            }

        }

    }

    public interface NewBTConnectionInterface {
        boolean Connected(BluetoothSocket clientSocket, int index);

        boolean isMACConnected(String mac);
    }
}
