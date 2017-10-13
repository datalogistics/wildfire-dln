
package com.gmeci.hardware.bluetooth;

import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.gmeci.core.ATSKConstants;
import com.gmeci.hardware.HardwareService;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class BluetoothSocketListener implements Runnable {
    int StreamID;
    Context context;
    BTSocketInterface btsi;
    private BluetoothSocket socket;
    private InputStream inStream;

    public BluetoothSocketListener(Context context, BluetoothSocket socket,
            int StreamID, BTSocketInterface btsi) {
        this.socket = socket;
        this.StreamID = StreamID;
        this.context = context;
        this.btsi = btsi;
    }

    @Override
    public void run() {
        byte[] buffer = new byte[HardwareService.BUFFER_SIZE];
        try {
            inStream = socket.getInputStream();
            String deviceconnected = socket.toString();
            String remoteDevice = socket.getRemoteDevice().getName();
            Log.d(deviceconnected, remoteDevice);
            int bytesRead = -1;
            // Keep listening to the InputStream while connected
            while (true) {
                // Read from the InputStream
                bytesRead = inStream.read(buffer);
                btsi.SendBuffer2Consumers(
                        Arrays.copyOfRange(buffer, 0, bytesRead), StreamID);
            }
        } catch (IOException e) {
            Log.e("BLUETOOTH", "disconnected", e);

            //LOU clean this up  should all be on 1 list??...
            btsi.RemoveSocket(socket.getRemoteDevice().getAddress());

            Intent btStatusDisconnectIntent = new Intent();
            btStatusDisconnectIntent.setAction(ATSKConstants.BT_DEVICE_CHANGE);
            btStatusDisconnectIntent.putExtra(
                    ATSKConstants.BT_CONNECTION_UPDATE_EXTRA, false);
            btStatusDisconnectIntent.putExtra(ATSKConstants.BT_NAME, socket
                    .getRemoteDevice().getName());
            context.sendBroadcast(btStatusDisconnectIntent);

        }
    }

    public interface BTSocketInterface {
        boolean RemoveSocket(String mac);

        void SendBuffer2Consumers(byte[] rollingbuffer, int StreamID);
    }

}
