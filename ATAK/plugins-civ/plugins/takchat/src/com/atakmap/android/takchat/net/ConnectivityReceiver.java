package com.atakmap.android.takchat.net;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.atakmap.coremap.log.Log;

/**
 * Process Android-level connectivity changed events
 *
 */
public class ConnectivityReceiver extends BroadcastReceiver {

    private static final String TAG = "ConnectivityReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction()))
            return;

        NetworkInfo networkInfo = intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
        if (networkInfo == null) {
            Log.e(TAG, "Unable to parse network info");
            return;
        }

        //NetworkManager.getInstance().onNetworkChange(networkInfo);
        //TODO
        Log.d(TAG, networkInfo.toString());
        if(isConnected(networkInfo)){
            //TODO force reconnect attempt now
        }else{
            //TODO what to do? update notification? Stop attempting connect?
        }
    }

    private boolean isConnected(NetworkInfo networkInfo) {
        return networkInfo != null && networkInfo.getState() == NetworkInfo.State.CONNECTED;
    }
}