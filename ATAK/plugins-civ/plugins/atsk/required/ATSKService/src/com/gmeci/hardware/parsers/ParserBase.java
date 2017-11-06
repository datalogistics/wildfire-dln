
package com.gmeci.hardware.parsers;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.gmeci.hardwareinterfaces.ParserInterface;
import com.gmeci.hardwareinterfaces.SerialCallbackInterface;

public abstract class ParserBase extends Service {

    protected static final String TAG = "ParserBase";
    protected ParserInterface parserInterface;
    protected HardwareServiceConnection serviceConnection = null;
    private SerialCallbackInterface serialCallback = new SerialCallbackInterface.Stub() {

        @Override
        public boolean SendData(byte[] data, int length, int StreamID)
                throws RemoteException {
            return ReadSerialData(data, length, StreamID);
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return null;
    }

    protected void releaseService() {
        if (serviceConnection == null)
            Log.e(TAG,
                    "Attempting to unbind a null service connection to PLGRService");

        unbindService(serviceConnection);
        serviceConnection = null;
        Log.d(TAG, "PLGRService Unbound");
    }

    protected void initService() {
        serviceConnection = new HardwareServiceConnection();
        boolean success = bindService(getParserInterfaceIntent(),
                serviceConnection, Context.BIND_AUTO_CREATE);
        if (success)
            Log.d(TAG, "Successful Binding to OBSTRUCTION Service");
        else
            Log.e(TAG, "Failed Bind to OBSTRUCTION Service");
    }

    protected Intent getParserInterfaceIntent() {
        Intent i = new Intent();
        i.setClassName("com.gmeci.atskservice",
                "com.gmeci.hardware.HardwareService");
        i.setAction("com.gmeci.hardwareinterfaces.ParserInterface");
        return i;
    }

    abstract boolean ReadSerialData(byte[] data, int length, int StreamID);

    abstract String GetParserName();

    private class HardwareServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            parserInterface = ParserInterface.Stub.asInterface(service);
            try {
                parserInterface.RegisterConsumer(GetParserName(),
                        serialCallback);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            releaseService();
        }
    }

}
