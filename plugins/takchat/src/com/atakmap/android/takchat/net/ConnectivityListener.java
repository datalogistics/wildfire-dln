package com.atakmap.android.takchat.net;

import com.atakmap.android.takchat.data.IListener;

/**
 * Listens for XMPP server connectivity changes
 *
 * Created by byoung on 7/29/2016.
 */
public interface ConnectivityListener extends IListener {

    /**
     * Connected to server
     * @return
     */
    boolean onConnected();

    /**
     * Connection status update e.g. waiting to connect
     *
     * @param bSuccess
     * @param status
     * @return
     */
    boolean onStatus(boolean bSuccess, String status);

    /**
     * Disconnected from server
     * @return
     */
    boolean onDisconnected();
}
