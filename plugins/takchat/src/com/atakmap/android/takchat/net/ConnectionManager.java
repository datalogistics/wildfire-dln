package com.atakmap.android.takchat.net;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.network.ui.CredentialsPreference;
import com.atakmap.android.takchat.TAKChatMapComponent;
import com.atakmap.android.takchat.TAKChatUtils;
import com.atakmap.android.takchat.data.TimerListener;
import com.atakmap.android.takchat.notification.SoundManager;
import com.atakmap.android.takchat.plugin.R;
import com.atakmap.comms.CotService;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;
import com.atakmap.net.AtakAuthenticationCredentials;
import com.atakmap.net.AtakAuthenticationDatabase;
import com.atakmap.net.AtakCertificateDatabaseIFace;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo;
import org.jivesoftware.smackx.forward.packet.Forwarded;
import org.jivesoftware.smackx.mam.MamManager;
import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;

import java.util.Date;

/**
 * Manages the connection to the XMPP server
 *
 * Created by byoung on 7/29/2016.
 */
public class ConnectionManager extends IManager<ConnectivityListener> implements ConnectivityListener, TimerListener {
    private static final String TAG = "ConnectionManager";
    public static final String DEFAULT_SYNC_PREF_NAME = "default";
    public static final String SYNC_PREF_NAME = "takchatLastSyncTime";

    private TAKChatXMPP _xmpp;
    private final SharedPreferences _prefs;
    //private ConnectivityReceiver _connectivityReceiver;
    private ReconnectInfo _reconnectInfo;

    /**
     * State of connect/reconnect attempt
     */
    private static class ReconnectInfo {

        /**
         * Number of timer/seconds to wait b/f reconnect attempt
         */
        private final static int RECONNECT_POLICY[] = new int[]{2, 7, 20, 60};

        /**
         * Number of attempts to reconnect without success
         */
        int _attempts;

        /**
         * Number of seconds passed from last successful connection
         */
        int _timerCounter;

        public ReconnectInfo(){
            _attempts = 1;
            _timerCounter = 0;
        }

        public void reset() {
            _attempts = 0;
            _timerCounter = 0;
            //TODO handle credentials changed...?
        }

        @Override
        public String toString() {
            return "attempts=" + _attempts + ", counter=" + _timerCounter;
        }
    }

    private enum ConfigFeedbackMode { Dialog, Toast, None };

    public ConnectionManager(SharedPreferences prefs) {
        super(TAG);
        this._prefs = prefs;
        _reconnectInfo = new ReconnectInfo();

        //TODO use ConnectivityManager to determine if connection dropped
        //DocumentedIntentFilter filter = new DocumentedIntentFilter();
        //filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        //_connectivityReceiver = new ConnectivityReceiver();
        //AtakBroadcast.getInstance().registerSystemReceiver(_connectivityReceiver, filter);
    }

    private static String getUserPassword(){
        AtakAuthenticationCredentials credentials = AtakAuthenticationDatabase
                .getCredentials(TAKChatUtils.getPluginString(R.string.xmpp_credentials));
        if (credentials == null || FileSystemUtils.isEmpty(credentials.password)) {
            Log.w(TAG, "No user password set");
            return null;
        }

        return credentials.password;
    }

    public ConnectionSettings getConfig(){
        int port = 5222;
        try {
            port = Integer.parseInt(_prefs.getString("takchatServerPort", "5222"));
        }catch(NumberFormatException e){
            Log.w(TAG, "Failed to parse takchatServerPort", e);
            port = 5222;
        }

        EntityFullJid me = TAKChatUtils.getUsernameFull();
        String password = ConnectionManager.getUserPassword();

        //check if we have a custom host
        String customHost = null;
        if(_prefs.getBoolean("takchatUseCustomHost", false)){
            customHost = _prefs.getString("takchatCustomHost", null);
        }

        //check if we have a truststore
        boolean bEncryption = _prefs.getBoolean("takchatRequireSSL", true);
        boolean bTakServerCerts = _prefs.getBoolean("takchatUseTakServerTrustStore", true);

        AtakAuthenticationCredentials caCertCredentials = null;
        byte[] chatTrustStore = null;
        String trustStorePassword = null;
        if(bEncryption) {
            Log.d(TAG, "Encryption required");
            if(bTakServerCerts){
                Log.d(TAG, "Use TAK Server certs");
                chatTrustStore = CotService.getCertificateDatabase().getCertificateForType(
                        AtakCertificateDatabaseIFace.TYPE_TRUST_STORE_CA);

                caCertCredentials =
                        CotService
                                .getAuthenticationDatabase()
                                .getCredentialsForType(
                                        AtakAuthenticationCredentials.TYPE_caPassword);
            }

            //if TAK server certs not loaded, check for chat CA certs
            if(chatTrustStore == null || caCertCredentials == null || caCertCredentials.password == null){
                Log.d(TAG, "Use Chat Server certs");
                chatTrustStore = CotService.getCertificateDatabase().getCertificateForType(
                        TAKChatUtils.getPluginString(R.string.caChatTrustStore));

                caCertCredentials =
                        CotService.getAuthenticationDatabase().getCredentialsForType(
                                TAKChatUtils.getPluginString(R.string.caChatPassword));
            }

            if (chatTrustStore == null) {
                Log.w(TAG, "Chat CA Trust store not found");
            } else {
                Log.d(TAG, "Found chat CA Trust store of size: " + chatTrustStore.length);
            }

            if(caCertCredentials != null && caCertCredentials.password != null)
                trustStorePassword = caCertCredentials.password;
        }

        return new ConnectionSettings(
                me,
                customHost,
                port,
                password,
                MapView.getDeviceUid(),
                bEncryption, chatTrustStore, trustStorePassword);
    }

    public static String getResourceId() {
        return TAKChatUtils.mapView.getContext().getString(com.atakmap.app.R.string.app_name) + "-" + MapView.getDeviceUid();
    }

    public void startup(){
        Log.d(TAG, "startup");
        configure(ConfigFeedbackMode.Dialog);
    }

    private boolean configure(final ConfigFeedbackMode mode){
        ConnectionSettings config = getConfig();

        //if disabled, or not properly configured, then do not attempt connect
        boolean bEnabled = _prefs.getBoolean("takchatEnabled", true);
        if(!bEnabled || config == null || !config.isValid()){
            //log error
            String message = null;
            if(!bEnabled){
                message = "Chat is disabled";
                Log.w(TAG, message);
            }else{
                String errorMessage = (config == null ? "" : config.validate());
                Log.w(TAG, "Configuration message invalid: " + errorMessage);
                message = "Chat settings invalid.";
                if(!FileSystemUtils.isEmpty(errorMessage))
                    message += " " + errorMessage;
            }

            //disconnect if necessary
            if(_xmpp != null) {
                _xmpp.disconnect(false);
            }

            //notify user
            switch(mode){
                case Toast:
                    Toast.makeText(TAKChatUtils.mapView.getContext(), message, Toast.LENGTH_LONG).show();
                    break;
                case Dialog:
                    if(!bEnabled && _prefs.getBoolean("takChatSuppressDisabledDialog", false)){
                        Log.d(TAG, "Chat disabled dialog is suppressed");
                        return false;
                    }else{
                        //first time we've shown it...
                        _prefs.edit().putBoolean("takChatSuppressDisabledDialog", true).apply();
                    }

                    AlertDialog.Builder builder = new AlertDialog.Builder(TAKChatUtils.mapView.getContext());
                    builder.setIcon(com.atakmap.app.R.drawable.xmpp_icon)
                            .setTitle("Configure Chat Settings")
                            .setMessage(message + ". Configure now?")
                            .setPositiveButton("Configure", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    displayConfigurationPrompt();
                                }
                            })
                            .setNegativeButton("Cancel", null);
                    builder.create().show();

                    break;
                default:
                case None:
                    break;
            }

            return false;
        }

        if(_xmpp == null) {
            Log.d(TAG, "Creating XMPP instance");
            _xmpp = TAKChatXMPP.getInstance();
        }else{
            Log.d(TAG, "Updating XMPP instance");
        }

        _xmpp.update(config);
        return true;
    }

    private void displayConfigurationPrompt() {
        //first see if TAK Server certs are available for re-use
        byte[] chatTrustStore = CotService.getCertificateDatabase().getCertificateForType(
                AtakCertificateDatabaseIFace.TYPE_TRUST_STORE_CA);

        AtakAuthenticationCredentials caCertCredentials = CotService
                .getAuthenticationDatabase()
                .getCredentialsForType(
                        AtakAuthenticationCredentials.TYPE_caPassword);

        boolean bTakCertsAvailable = !FileSystemUtils.isEmpty(chatTrustStore)
                && caCertCredentials != null
                && caCertCredentials.password != null;

        if(!bTakCertsAvailable){
            Log.d(TAG, "Displaying advanced settings");
            AtakBroadcast.getInstance().sendBroadcast(
                    new Intent("com.atakmap.app.ADVANCED_SETTINGS")
                            .putExtra("toolkey", TAKChatMapComponent.TAKCHAT_PREFERENCE));
            return;
        }

        Log.d(TAG, "Prompting user credentials: " + bTakCertsAvailable);

        //layout borrowed from AtakAuthenticationHandler, but not using
        //that DB to cache credentials
        LayoutInflater inflater = LayoutInflater.from(TAKChatUtils.mapView.getContext());
        View dialogView = inflater.inflate(com.atakmap.app.R.layout.login_dialog, null);

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
                TAKChatUtils.mapView.getContext());
        dialogBuilder.setTitle("Login to Chat Server");
        dialogBuilder.setView(dialogView);

        final Dialog loginDialog = dialogBuilder.create();

        Button loginButton = (Button) dialogView.findViewById(com.atakmap.app.R.id.btn_login);
        Button cancelButton = (Button) dialogView.findViewById(com.atakmap.app.R.id.btn_cancel);
        final EditText uidText = (EditText) dialogView
                .findViewById(com.atakmap.app.R.id.txt_name);
        uidText.setHint("Required (user@domain.net)");
        final EditText pwdText = (EditText) dialogView
                .findViewById(com.atakmap.app.R.id.password);
        pwdText.setHint("Required");

        final String type = TAKChatUtils.getPluginString(R.string.xmpp_credentials);
        AtakAuthenticationCredentials credentials = AtakAuthenticationDatabase
                .getCredentials(type);
        if (credentials != null
                && !FileSystemUtils.isEmpty(credentials.username)) {
            uidText.setText(credentials.username);
        }
        if (credentials != null
                && !FileSystemUtils.isEmpty(credentials.password)) {
            pwdText.setText(credentials.password);
        }

        loginButton.setText("Login");
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String username = uidText.getText().toString().trim();
                final String password = pwdText.getText().toString();

                Log.d(TAG, "Saving user credentials: " + username);
                loginDialog.dismiss();
                AtakAuthenticationDatabase.saveCredentials(
                        type, username, password, true);

                AtakBroadcast.getInstance().sendBroadcast(
                        new Intent(CredentialsPreference.CREDENTIALS_UPDATED).putExtra("type",
                                type));
            }
        });

        cancelButton.setText("Advanced Settings");
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginDialog.dismiss();

                AtakBroadcast.getInstance().sendBroadcast(
                        new Intent("com.atakmap.app.ADVANCED_SETTINGS")
                                .putExtra("toolkey", TAKChatMapComponent.TAKCHAT_PREFERENCE));
            }
        });

        loginDialog.show();
    }

    public void dispose() {
        super.dispose();
        Log.d(TAG, "dispose");

        //TAKChatUtils.mapView.getContext().unregisterReceiver(_connectivityReceiver);
    }

    /**
     * Disconnect and shutdown
     */
    public void disconnect(){
        //Note we don't do this in dispose, b/c other managers need chance
        //to shutdown prior to the disconnect
        if(_xmpp != null) {
            _xmpp.disconnect(true);
        }
    }

    public void connectionClosed(Exception e) {
        if(e == null)
            Log.d(TAG, "connectionClosed");
        else
            Log.w(TAG, "connectionClosed", e);
        onDisconnected();
    }

    @Override
    public boolean onConnected() {
        Log.d(TAG, "onConnected");
        _reconnectInfo.reset();

        SoundManager.getInstance().play(SoundManager.SOUND.COMING);

        //Note... do not do network calls inside onConnected impls
        synchronized (_listeners){
            for(ConnectivityListener l : _listeners){
                l.onConnected();
            }
        }

        TAKChatUtils.takChatComponent.getManager(ConnectionManager.class).queryArchive();
        return true;
    }

    @Override
    public boolean onStatus(boolean bSuccess, String status) {
        Log.d(TAG, "onStatus: " + bSuccess + ", " + status);
        synchronized (_listeners){
            for(ConnectivityListener l : _listeners){
                l.onStatus(bSuccess, status);
            }
        }
        return true;
    }

    @Override
    public boolean onDisconnected() {
        Log.d(TAG, "onDisconnected");

        SoundManager.getInstance().play(SoundManager.SOUND.LEAVE);

        synchronized (_listeners){
            for(ConnectivityListener l : _listeners){
                l.onDisconnected();
            }
        }
        return true;
    }

    /**
     * Query archive using MAM XEP-313, based on user prefs
     * Note, messages are piped into processPacket(), so they are not directly processed here
     */
    public void queryArchive() {
        if(_xmpp == null || !_xmpp.isConnected()) {
            Log.w(TAG, "Chat service not connected queryArchive");
            return;
        }

        TAKChatUtils.runInBackground(new Runnable() {
            @Override
            public void run() {
                queryArchiveInternal();
            }
        });
    }

    @Override
    public void onTimer() {
        if(_xmpp == null){
            //Log.w(TAG, "onTimer, connection not created...");
            return;
        }

        if(_xmpp.hasLoginError()){
            Log.w(TAG, "onTimer, login error, not connecting...");
            return;
        }

        if (_xmpp.getStatus() == TAKChatXMPP.ConnectionStatus.waiting) {
            //Log.d(TAG, "onTimer waiting");
            int reconnectAfter;
            if (_reconnectInfo._attempts < ReconnectInfo.RECONNECT_POLICY.length)
                reconnectAfter = ReconnectInfo.RECONNECT_POLICY[_reconnectInfo._attempts];
            else
                reconnectAfter = ReconnectInfo.RECONNECT_POLICY[ReconnectInfo.RECONNECT_POLICY.length - 1];
            if (_reconnectInfo._timerCounter >= reconnectAfter) {
                _reconnectInfo._timerCounter = 0;
                _reconnectInfo._attempts += 1;
                Log.d(TAG, "onTimer updateConnection: " + _reconnectInfo.toString());
                checkConnection(false);
            } else {
                //Log.d(TAG, "onTimer increment timer counter: " + _reconnectInfo.toString());
                _reconnectInfo._timerCounter += 1;
            }
        } else {
            //Log.d(TAG, "onTimer not waiting: " + _xmpp.getStatus());
            _reconnectInfo._timerCounter = 0;
        }
    }

    public void forceReconnect(boolean bPromptConfig) {
        //TODO during recent refactoring, lost ability to prompt user during force reconnect, if config/server/credentials are invalid
        //b/c checkConnection currently disconnects, and then lets onTimer reconnection on next iteration
        forceReconnect();
    }

    public void forceReconnect() {
        if(_xmpp == null){
            Log.d(TAG, "forceReconnect, creating connection...");
            configure(ConfigFeedbackMode.Toast);
        }else {
            _xmpp.clearLoginError();
            checkConnection(true);
        }
    }

    private boolean checkConnection(boolean bForceReconnect) {
        if(_xmpp == null){
            Log.w(TAG, "updateConnection, connection not created...");
            return false;
        }

        if(bForceReconnect){
            _xmpp.clearConfig();
        }

        //TODO move status from _xmpp into this class?
        Log.d(TAG, "checkConnection: " + bForceReconnect + ", " + _xmpp.getStatus());

        boolean networkAvailable = true;  //TODO NetworkManager.getInstance().getState() == NetworkState.available
        if (!networkAvailable) {
            if (_xmpp.getStatus() == TAKChatXMPP.ConnectionStatus.connected
                    || _xmpp.getStatus() == TAKChatXMPP.ConnectionStatus.authenticating
                    || _xmpp.getStatus() == TAKChatXMPP.ConnectionStatus.connecting) {
                Log.d(TAG, "checkConnection no network disconnect");
                _xmpp.disconnect(false);
                return true;
            } else if (_xmpp.getStatus() == TAKChatXMPP.ConnectionStatus.waiting) {
                Log.d(TAG, "checkConnection no network keep waiting");
                return false;
            } else{
                Log.w(TAG, "checkConnection Invalid state: " + _xmpp.getStatus());
                return false;
            }
        } else if(bForceReconnect){
            if (_xmpp.getStatus() == TAKChatXMPP.ConnectionStatus.connected
                    || _xmpp.getStatus() == TAKChatXMPP.ConnectionStatus.authenticating
                    || _xmpp.getStatus() == TAKChatXMPP.ConnectionStatus.connecting) {
                Log.d(TAG, "checkConnection force reconnect disconnect");
                _xmpp.disconnect(false);
                return true;
            } else if (_xmpp.getStatus() == TAKChatXMPP.ConnectionStatus.waiting) {
                Log.d(TAG, "checkConnection  force reconnect now");
                configure(ConfigFeedbackMode.Toast);
                return true;
            }else{
                Log.w(TAG, "checkConnection Invalid state: " + _xmpp.getStatus());
                return false;
            }
        } else {
            //network is available, and not a user forced reconnect, just a timer update
            if (_xmpp.getStatus() == TAKChatXMPP.ConnectionStatus.waiting) {
                Log.d(TAG, "checkConnection waiting, now attempt reconnect");
                configure(ConfigFeedbackMode.None);
                return true;
            } else {
                Log.d(TAG, "checkConnection no change");
                return false;
            }
        }
    }

    /**
     * Queries server for messages since last sync
     * Blocks and waits for response
     */
    private void queryArchiveInternal(){

        XMPPConnection connection = TAKChatXMPP.getInstance().getConnection();
        ConnectionSettings config = TAKChatXMPP.getInstance().getConfig();

        if(TAKChatXMPP.SMACK_DEBUG) {
            try {
                //List<String> features = ServiceDiscoveryManager.getInstanceFor(_connection).getFeatures();
                //for (String feature : features) {
                //    Log.d(TAG, "Feature: " + feature);
                //}

                Jid serverJid = JidCreate.domainBareFrom(config.getServerAddress());
                DiscoverInfo discoInfo = ServiceDiscoveryManager.getInstanceFor(connection).discoverInfo(serverJid);
                for (DiscoverInfo.Identity identity : discoInfo.getIdentities()) {
                    Log.d(TAG, "Identity: " + identity.getName() + ", " + identity.getCategory() + ", " + identity.getType() + ", " + identity.toXML());
                }
                for (DiscoverInfo.Feature feature : discoInfo.getFeatures()) {
                    Log.d(TAG, "Feature: " + feature.getVar());
                }
                for (ExtensionElement extension : discoInfo.getExtensions()) {
                    Log.d(TAG, "Extension: " + extension.toXML());
                }
            } catch (Exception e) {
                //TODO catch appropriate exceptions types/scope
                Log.w(TAG, "Failed to discover server features", e);
            }
        }

        //TODO be sure these prefs and the local SQLCipher chat DB are both cleared together e.g. "Clear Data"
        //so just store chat db in default app storage, not ATAK/Databases

        //get time we last synced

        try {
            MamManager archive = MamManager.getInstanceFor(connection);
            if(!archive.isSupportedByServer()){
                Log.d(TAG, "Message archives not supported by server: " + config.getServerAddress());
                return;
            }

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(TAKChatUtils.mapView.getContext());
            long syncPoint = TAKChatUtils.getSyncPoint(prefs, DEFAULT_SYNC_PREF_NAME);
            long now = System.currentTimeMillis();
            Log.d(TAG, "Syncing messages since " + syncPoint  + " (" + (now - syncPoint)/1000  + " seconds) from server: " + config.getServerAddress());

            MamManager.MamQueryResult queryResult = archive.queryArchiveWithStartDate(new Date(syncPoint));
            if(queryResult == null || queryResult.forwardedMessages == null || queryResult.forwardedMessages.size() < 1){
                Log.d(TAG, "No messages archived at server: " + config.getServerAddress());
            }else {
                Log.d(TAG, "Received " + queryResult.forwardedMessages.size() + " archived messages from server: " + config.getServerAddress());
                if (TAKChatXMPP.SMACK_DEBUG) {
                    for (Forwarded message : queryResult.forwardedMessages) {
                        Log.d(TAG, "Received archived message: " + message.toXML());
                        //TODO should we add to UI/DB from here?
                    }
                }
            }

            //update last sync time
            TAKChatUtils.setSyncPoint(prefs, DEFAULT_SYNC_PREF_NAME, now);
        } catch (SmackException.NoResponseException e) {
            //TODO process these errors further?
            Log.w(TAG, "Unable to sync archive", e);
        } catch (XMPPException.XMPPErrorException e) {
            Log.w(TAG, "Unable to sync archive", e);
        } catch (SmackException.NotConnectedException e) {
            Log.w(TAG, "Unable to sync archive", e);
        } catch (InterruptedException e) {
            Log.w(TAG, "Unable to sync archive", e);
        } catch (SmackException.NotLoggedInException e) {
            Log.w(TAG, "Unable to sync archive", e);
        }
    }
}
