
package com.gmeci.atsk.resources;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import com.atakmap.coremap.log.Log;

import com.gmeci.hardwareinterfaces.HardwareConsumerInterface;

public class ServiceConnectionManager {

    private static final String TAG = "ServiceConnectionManager";
    public HardwareServiceConnection hardwareServiceConnection = null;
    public HardwareConsumerInterface hardwareInterface;
    boolean hardwareServiceConnected = false;

    ServiceConnectionManagerInterface parentInterface;
    private Context parentActivity;

    public ServiceConnectionManager() {

    }

    public boolean startServiceManagement(Context activity,
            ServiceConnectionManagerInterface parentInterface, String Source) {
        return startServiceManagement(activity, parentInterface, Source, false);
    }

    public boolean startServiceManagement(Context activity,
            ServiceConnectionManagerInterface parentInterface, String Source,
            boolean ATSKServcieOnly) {
        this.parentInterface = parentInterface;
        parentActivity = activity;
        boolean obstructionBoolean = true;
        if (!ATSKServcieOnly)
            obstructionBoolean = initObstructionService(Source);

        return obstructionBoolean;
    }

    public void stopServiceManagement() {
        releaseObstructionService();
    }

    public HardwareConsumerInterface getHardwareInterface() {
        if (hardwareServiceConnected)
            return hardwareInterface;
        else
            return null;
    }

    private void releaseObstructionService() {
        if (hardwareServiceConnection == null)
            Log.e(TAG,
                    "Attempting to unbind a null service connection to CurrentRoute");

        if (hardwareServiceConnection != null)
            parentActivity.unbindService(hardwareServiceConnection);
        hardwareServiceConnection = null;
        Log.d(TAG, "ATSK ObstructionService Unbound");
    }

    private boolean initObstructionService(String Source) {
        hardwareServiceConnection = new HardwareServiceConnection();
        Intent i = new Intent();
        i.setClassName("com.gmeci.atskservice",
                "com.gmeci.hardware.HardwareService");
        i.setAction("com.gmeci.hardwareinterfaces.HardwareConsumerInterface");
        boolean success = parentActivity.bindService(i,
                hardwareServiceConnection, Context.BIND_AUTO_CREATE);
        if (success)
            Log.d(TAG, "Successful Binding to Hardware Service from " + Source);
        else
            Log.e(TAG, "Failed Bind to Hardware Service");
        return success;
    }

    private class HardwareServiceConnection implements ServiceConnection {

        @Override
        public void onServiceDisconnected(ComponentName name) {
            releaseObstructionService();
            hardwareInterface = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            hardwareInterface = HardwareConsumerInterface.Stub
                    .asInterface(service);
            hardwareServiceConnected = true;
            if (parentInterface != null)
                parentInterface.GotHardwareHandle();
        }
    }

}
