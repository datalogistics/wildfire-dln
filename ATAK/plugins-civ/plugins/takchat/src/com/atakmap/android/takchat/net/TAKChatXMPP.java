package com.atakmap.android.takchat.net;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.atakmap.android.math.MathUtils;
import com.atakmap.android.takchat.TAKChatUtils;
import com.atakmap.android.takchat.plugin.R;
import com.atakmap.android.takchat.uid.TAKUidFilter;
import com.atakmap.android.takchat.uid.TAKUidSendingListener;
import com.atakmap.android.takchat.uid.TAKUidStanzaListener;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.time.CoordinatedTime;
import com.atakmap.spatial.kml.KMLUtil;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.NotFilter;
import org.jivesoftware.smack.filter.StanzaExtensionFilter;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.filter.StanzaTypeFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.parsing.ExceptionLoggingCallback;
import org.jivesoftware.smack.sasl.SASLErrorException;
import org.jivesoftware.smack.sasl.SASLMechanism;
import org.jivesoftware.smack.sasl.core.SASLAnonymous;
import org.jivesoftware.smack.sasl.provided.SASLPlainMechanism;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smack.util.TLSUtils;
import org.jivesoftware.smackx.attention.packet.AttentionExtension;
import org.jivesoftware.smackx.chatstates.ChatStateManager;
import org.jivesoftware.smackx.chatstates.packet.ChatStateExtension;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo;
import org.jivesoftware.smackx.ping.android.ServerPingWithAlarmManager;
import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.util.Date;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManagerFactory;

/**
 * Configures and maintains the XMPP server connection
 *
 * Based on Smack XMPP client library
 */
public class TAKChatXMPP implements ConnectionListener{

    private final static String TAG = "TAKChatXMPP";
    public final static boolean SMACK_DEBUG = true;
    public final static int PACKET_REPLY_TIMEOUT = 30000;
    public static final String ERROR_TITLE = "Error";

    private static final String BAD_SMACK_ERROR_MSG = "The following addresses failed";
    private static final String DEFAULT_ERROR_MESSAGE = "Failed to connect to server";

    private static TAKChatXMPP _instance = null;
    private final SharedPreferences _prefs;
    private ConnectionSettings _config;
    private ConnectionStatus _status;
    private long _lastConnectTime;
    private String _lastError;

    private SASLErrorException _loginError;
    private SSLException _sslError;


    /**
     * Connection maintains its own read & writes threads
    */
    private XMPPTCPConnection _connection;

    public enum ConnectionStatus {
        waiting, connecting, authenticating, connected;
    }

    public static TAKChatXMPP getInstance(){
        if (_instance == null) {
            _instance = new TAKChatXMPP();
        }

        return _instance;
    }

    private TAKChatXMPP() {
        _lastConnectTime = -1;
        _lastError = "Waiting to connect...";
        _prefs = PreferenceManager.getDefaultSharedPreferences(TAKChatUtils.mapView.getContext());
        setStatus(ConnectionStatus.waiting);
        initSmackConfig();
    }

    public void clearConfig(){
        Log.d(TAG, "Clearing credentials");
        _config = null;
        //TODO ensure disconnecting?
    }

    /**
     * Reconnect based on new configuration/settings
     *
     * @param config
     */
    public void update(ConnectionSettings config) {
        boolean bReUseConnection = canReuseConnectionSettings(config);
        this._config = config;

        //init upon first connection, otherwise disconnect old connection
        if(bReUseConnection) {
            Log.w(TAG, "Re-using XMPP connection settings");
            disconnect(false);
        } else {
            Log.w(TAG, "Creating new XMPP connection settings");
            if(!init()){
                Log.w(TAG, "Failed to initialize XMPP connection");
                return;
            }
        }

        //now attempt connection
        connect();
    }

    /**
     * Check if a new connection object is necessary, or we can reconnect with same instance
     *
     * @param config
     * @return  true if connection may be reused
     */
    private boolean canReuseConnectionSettings(ConnectionSettings config) {
        if(config == null || _config == null)
            return false;

        //TODO if we re-use, what else needs to be cleaned up, removed?
        return _connection != null
                && FileSystemUtils.isEquals(config.getServerAddress(), _config.getServerAddress())
                && FileSystemUtils.isEquals(config.getCustomHost(), _config.getCustomHost())
                && (config.isRequireEncryption() == _config.isRequireEncryption())
                && FileSystemUtils.isEquals(config.getTrustStorePassword(), _config.getTrustStorePassword())
                && compareTruststore(config.getTrustStore(), _config.getTrustStore());
    }

    /**
     * Return true if trust stores match
     *
     * @param lhs
     * @param rhs
     * @return
     */
    private boolean compareTruststore(byte[] lhs, byte[] rhs) {
        if(lhs == null && rhs == null)
            return true;

        if(lhs == null || rhs == null)
            return false;

        if(lhs == rhs)
            return true;

        if(lhs.length != rhs.length)
            return false;

        //Best way to compare if they match, TODO need to compare all bytes?
        if(lhs.length > 0 && rhs.length > 0){
            if(lhs[0] != rhs[0])
                return false;
            if(lhs[lhs.length-1] != rhs[rhs.length-1])
                return false;
        }

        return true;
    }

    private boolean init() {
        if(_config == null || !_config.isValid()){
            Log.w(TAG, "Configuration message invalid");
            return false;
        }

        Log.d(TAG, "init: " + _config.toString());
        setStatus(ConnectionStatus.waiting);
        XMPPTCPConnectionConfiguration.Builder builder = XMPPTCPConnectionConfiguration.builder();
        if(!_config.isRequireEncryption()){
            //Note this would disable encryption, and not proceed if server requires TLS
//            Log.w(TAG, "Encryption disabled");
//            builder.setSecurityMode(ConnectionConfiguration.SecurityMode.disabled);


           //Still attempt TLS, but with no TLS certificate validation
           builder.setSecurityMode(ConnectionConfiguration.SecurityMode.ifpossible);
           final boolean bInsecure = true;
            if (bInsecure) {
                Log.w(TAG, "Accepting all certificates");
                try {
                    TLSUtils.disableHostnameVerificationForTlsCertificates(builder);
                    TLSUtils.acceptAllCertificates(builder);
                } catch (NoSuchAlgorithmException e) {
                    Log.w(TAG, "Failed to acceptAllCertificates", e);
                } catch (KeyManagementException e) {
                    Log.w(TAG, "Failed to acceptAllCertificates", e);
                }
            }
        }else{
            builder.setSecurityMode(ConnectionConfiguration.SecurityMode.required);
            Log.d(TAG, "Encryption required");
            builder.setKeystoreType("BKS");
            System.setProperty("ssl.TrustManagerFactory.algorithm",
                    javax.net.ssl.KeyManagerFactory.getDefaultAlgorithm());

            if(_config.hasTrustStore()) {
                Log.d(TAG, "Using truststore of size: " + _config.getTrustStore().length);
                SSLContext sslContext = null;
                try {
                    sslContext = createSSLContext();
                } catch (Exception e) {
                    Log.e(TAG, "Failed to create SSL Factory", e);
                    _lastError = "Failed to connect securely: " + e.getMessage();
                    String title = TAKChatUtils.getPluginString(R.string.app_name) + " " + ERROR_TITLE;
                    Toast.makeText(TAKChatUtils.mapView.getContext(), title +  ", " + _lastError, Toast.LENGTH_SHORT).show();
                    return false;
                }

                if(sslContext != null) {
                    //Note we require hostname verification based on TLS certificates
                    builder.setCustomSSLContext(sslContext);
                }
            } else{
                Log.e(TAG, "SSL Truststore not provided: " + _config.getServerAddress());
                _lastError = "SSL Truststore required for: " + _config.getServerAddress() + ". Check settings";
                String title = TAKChatUtils.getPluginString(R.string.app_name) + " " + ERROR_TITLE;
                Toast.makeText(TAKChatUtils.mapView.getContext(), title +  ", " + _lastError, Toast.LENGTH_SHORT).show();
                return false;
            }
        }

        DomainBareJid serviceName = null;
        try {
            serviceName = JidCreate.domainBareFrom(_config.getServerAddress());
            builder.setXmppDomain(serviceName);

            if(_config.hasCustomHost()){
                Log.d(TAG, "Configuring custom host: " + _config.getCustomHost() + ", for service: " + serviceName);
                builder.setHost(_config.getCustomHost());
            }else{
                Log.d(TAG, "Configuring for service: " + serviceName);
                builder.setHost(_config.getServerAddress());
            }

            builder.setPort(_config.getServerPort())
                    .setDebuggerEnabled(SMACK_DEBUG)
                    .setResource(_config.getResource())
                    .setCompressionEnabled(true)
                    .setSendPresence(true);
        } catch (XmppStringprepException e) {
            Log.e(TAG, "Failed to parse server address JID: " + _config.getServerAddress(), e);
            _lastError = "Invalid Chat Server: " + _config.getServerAddress();
            String title = TAKChatUtils.getPluginString(R.string.app_name) + " " + ERROR_TITLE;
            Toast.makeText(TAKChatUtils.mapView.getContext(), title +  ", " + _lastError, Toast.LENGTH_SHORT).show();
            return false;
        }

        //TODO what does this do?
        XMPPTCPConnection.setUseStreamManagementResumptiodDefault(true);
        XMPPTCPConnection.setUseStreamManagementDefault(true);

        setUpSASL();

        //setup connection
        //TODO if we create a new connection on updated params, are listeners lost anywhere?
        _connection = new XMPPTCPConnection(builder.build());
        _connection.addConnectionListener(this);
        _connection.setFromMode(XMPPConnection.FromMode.USER);

        //setup xmpp packet listeners
        _connection.addAsyncStanzaListener(new TAKUidStanzaListener(), new TAKUidFilter());
        _connection.addAsyncStanzaListener(
                TAKChatUtils.takChatComponent.getManager(MessageManager.class), new StanzaFilter() {
            @Override
            public boolean accept(Stanza stanza) {
                //accept all packets and let other components filter as needed
                return true;
            }
        });

        //_connection.addPacketSendingListener(new TAKUidSendingListener(), new StanzaFilter() {
        _connection.addPacketInterceptor(new TAKUidSendingListener(), new StanzaFilter() {
            @Override
            public boolean accept(Stanza stanza) {
                //add TAK UID to outgoing messages. Note adding to IQ seems to cause issues with ejabberd
                return (stanza instanceof Presence) || (stanza instanceof Message);
            }
        });

        //Add support for attention/buzz
        //TODO where does this code belong?
        AndFilter attExt = new AndFilter(new StanzaFilter[]{StanzaTypeFilter.MESSAGE, new StanzaExtensionFilter(new AttentionExtension()), new NotFilter(MessageTypeFilter.ERROR)});
        _connection.addAsyncStanzaListener(new StanzaListener() {
            @Override
            public void processStanza(Stanza stanza) throws SmackException.NotConnectedException, InterruptedException {
                //TODO these can be sent by users, for from the server/web UI
                //TODO how to display in UI
                Log.d(TAG, "Attention:  " + stanza.toXML());
            }
        }, attExt);
        ServiceDiscoveryManager.getInstanceFor(_connection).addFeature(AttentionExtension.NAMESPACE);

        ServiceDiscoveryManager.getInstanceFor(_connection).addFeature(ChatStateExtension.NAMESPACE);

        //init so we can get callbacks for incoming chat state changes
        ChatStateManager.getInstance(_connection);

        //manage errors and reconnects
        _connection.setParsingExceptionCallback(new ExceptionLoggingCallback());

        //this will ping server every 30 minutes using AlarmManager (scheduled during doze)
        ServerPingWithAlarmManager pingMgr = ServerPingWithAlarmManager.getInstanceFor(_connection);
        if(pingMgr != null) {
            pingMgr.setEnabled(true);
            pingMgr.onCreate(TAKChatUtils.mapView.getContext());
        }

        //init connection (this does not start connection attempt)
        TAKChatUtils.takChatComponent.init(_connection);
        return true;
    }

    private void initSmackConfig() {
        if(SMACK_DEBUG) {
            System.setProperty("smack.debuggerClass",
                    "org.jivesoftware.smack.debugger.ConsoleDebugger");
            // "com.xabber.android.data.FileLogDebugger");
            System.setProperty("smack.debugEnabled", "true");
            SmackConfiguration.DEBUG = true;
        }

        SmackConfiguration.setDefaultPacketReplyTimeout(PACKET_REPLY_TIMEOUT);
        ServiceDiscoveryManager.setDefaultIdentity(new DiscoverInfo.Identity(
                "client",
                TAKChatUtils.getPluginString(R.string.app_name),
                "Android"));
    }

    private void setUpSASL() {
        //Note, in practice only PLAIN works with LDAP enabled XMPP servers (e.g. not SCRAM)
        //Note Smack also uses priority to determine e.g. SCRAM used rather than PLAIN

        //TODO move these into prefs so we can control w/out a recompile?
        final boolean bForcePlainTextAuth = false;
        final boolean bAllowPlainTextAuth = true;
        final boolean bAllowAnonymousAuth = false;

        if (bForcePlainTextAuth || bAllowPlainTextAuth) {
            Log.d(TAG, "Register SASL: " + SASLPlainMechanism.NAME);
            if(!SASLAuthentication.isSaslMechanismRegistered(SASLPlainMechanism.NAME)){
                SASLAuthentication.registerSASLMechanism(new SASLPlainMechanism());
            }
            SASLAuthentication.unBlacklistSASLMechanism(SASLMechanism.PLAIN);
        }

       if (bForcePlainTextAuth) {
            //blacklist all but PLAIN
           Log.d(TAG, "Force PLAIN SASL: " + SASLPlainMechanism.NAME);
           final Map<String, String> registeredSASLMechanisms = SASLAuthentication.getRegisterdSASLMechanisms();
            for (String mechanism : registeredSASLMechanisms.values()) {
                SASLAuthentication.blacklistSASLMechanism(mechanism);
            }

            SASLAuthentication.unBlacklistSASLMechanism(SASLMechanism.PLAIN);
        } else {
            //Allow others defaults e.g. SCRAM-SHA-1
            final Map<String, String> registeredSASLMechanisms = SASLAuthentication.getRegisterdSASLMechanisms();
            for (String mechanism : registeredSASLMechanisms.values()) {
                SASLAuthentication.unBlacklistSASLMechanism(mechanism);
            }

            if(bAllowPlainTextAuth) {
                Log.d(TAG, "Allow PLAIN SASL: " + SASLPlainMechanism.NAME);
                SASLAuthentication.unBlacklistSASLMechanism(SASLMechanism.PLAIN);
            }else{
                //Disallow PLAIN
                Log.d(TAG, "Disallow PLAIN SASL: " + SASLPlainMechanism.NAME);
                SASLAuthentication.blacklistSASLMechanism(SASLMechanism.PLAIN);
            }
        }

        if(!bAllowAnonymousAuth && SASLAuthentication.isSaslMechanismRegistered(SASLAnonymous.NAME)){
            Log.d(TAG, "Blacklist SASL: " + SASLAnonymous.NAME);
            SASLAuthentication.blacklistSASLMechanism(SASLAnonymous.NAME);
        }

        if(SMACK_DEBUG) {
            for (String s : SASLAuthentication.getRegisterdSASLMechanisms().values()) {
                Log.d(TAG, "SASL Registered: " + s);
            }

            for (String s : SASLAuthentication.getBlacklistedSASLMechanisms()) {
                Log.d(TAG, "SASL Blacklisted: " + s);
            }
        }
    }

    private SSLContext createSSLContext() throws KeyStoreException,
            NoSuchAlgorithmException, KeyManagementException, IOException, CertificateException {

        SSLContext sslContext = null;
        InputStream trustedIn = null;
        try {
            trustedIn = new ByteArrayInputStream(_config.getTrustStore());
            KeyStore trustStore = KeyStore.getInstance("PKCS12");
            trustStore.load(trustedIn, _config.getTrustStorePassword().toCharArray());

            TrustManagerFactory trustManagerFactory = TrustManagerFactory
                    .getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustStore);

            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagerFactory.getTrustManagers(), new SecureRandom());
        }finally {
            if(trustedIn != null)
                trustedIn.close();
        }

        return sslContext;
    }


    public boolean isConnected() {
        if(_connection == null) {
            //Log.w(TAG, "isConnected connection not set") ;
            return false;
        }

        //Log.d(TAG, "isConnected: " + (_status == ConnectionStatus.connected));
        return _status == ConnectionStatus.connected;
    }

    public ConnectionStatus getStatus(){
        return _status;
    }

    void setStatus(ConnectionStatus status) {
        Log.d(TAG, "Setting status: " + status);
        ConnectionManager cm = TAKChatUtils.takChatComponent.getManager(ConnectionManager.class);
        if(cm == null){
            return;
        }

        _status = status;
        if(_loginError != null){
            String account = getAccountDisplay();
            cm.onStatus(false, "Login failed" + (FileSystemUtils.isEmpty(account) ? "" : ": " + account));
        } else if(_sslError != null){
            String account = getAccountDisplay();
            cm.onStatus(false, "SSL error" + (FileSystemUtils.isEmpty(account) ? "" : ": " + account));
        }else {
            cm.onStatus((status == ConnectionStatus.connected), getStatusMessage());
        }
    }

    public String getStatusMessage() {
        //TODO need to be synchronized?
        if(!_prefs.getBoolean("takchatEnabled", true)){
            _lastError = "Chat is currently disabled. Check your settings.";
            return _lastError;
        }

        if(_config == null || !_config.isValid()){
            return _status.toString() + ", Configure credentials via settings";
        }

        switch(_status){
            case waiting:
                return "Waiting to connect: " + _config.getServerAddress();
            case connecting:
                return "Connecting to: " + _config.getServerAddress() + "...";
            case authenticating:
                return "Logging in as: " + getAccountDisplay() + "...";
            case connected:
                return "Logged in as: " + getAccountDisplay();
        }

        return _status.toString();
    }

    public String getLastConnectTime() {
        if(_lastConnectTime < 0){
            Log.w(TAG, "Last connection time not set");
            return "Not connected";
        }

        long diff = new CoordinatedTime().getMilliseconds() - _lastConnectTime;
        return "Connected " + MathUtils.GetTimeRemainingString(diff) + ", since "
                + KMLUtil.KMLDateTimeFormatter.get().format(new Date(_lastConnectTime));
    }

    public String getLastError() {
        return _lastError;
    }

    private void setLoginError(SASLErrorException e) {
        Log.w(TAG, "setLoginError: " + e.getMessage());
        String account = getAccountDisplay();

        _lastError = "Login failed" + (FileSystemUtils.isEmpty(account) ? "" : ": " + account);
        TAKChatUtils.takChatComponent.getManager(ConnectionManager.class).onStatus(
                false, _lastError);
        this._loginError = e;
    }

    private void setSSLError(SSLException e) {
        Log.w(TAG, "setSSLError: " + e.getMessage());
        String account = getAccountDisplay();

        _lastError = "SSL error" + (FileSystemUtils.isEmpty(account) ? "" : ": " + account);
        TAKChatUtils.takChatComponent.getManager(ConnectionManager.class).onStatus(
                false, _lastError);
        this._sslError = e;
    }

    void clearLoginError(){
        Log.w(TAG, "clearLoginError");
        TAKChatUtils.takChatComponent.getManager(ConnectionManager.class).onStatus(
                (_status == ConnectionStatus.connected), getStatusMessage());
        this._loginError = null;
        this._sslError = null;
    }

    public boolean hasLoginError() {
        return _loginError != null;
    }

    public void disconnect(boolean bShuttingDown) {
        Log.d(TAG, "disconnect: " + bShuttingDown);

        if(bShuttingDown && _connection != null){
            //Smack does this automatically during shutdown
            //sendPresence(TAKChatUtils.createSelfPresence(false, true));

            ServerPingWithAlarmManager pingMgr = ServerPingWithAlarmManager.getInstanceFor(_connection);
            if(pingMgr != null) {
                pingMgr.setEnabled(false);
                pingMgr.onDestroy();
            }
        }

        setStatus(ConnectionStatus.waiting);
        if(_connection != null) {
            Thread thread = new Thread(TAG + " disconnect thread") {
                @Override
                public void run() {
                    if (_connection != null)
                        try {
                            _connection.disconnect();
                        } catch (Exception e) {
                            _lastError = "Disconnect error: " + e.getMessage();
                            Log.w(TAG, "Disconnect error", e);
                        }
                }

            };
            thread.setPriority(Thread.MIN_PRIORITY);
            thread.setDaemon(true);
            thread.start();
        }
    }

    private void connect() {
        if (isConnected()) {
            Log.w(TAG, "Already connected...");
            return;
        }
        clearLoginError();
        setStatus(TAKChatXMPP.ConnectionStatus.connecting);

        TAKChatUtils.runInBackground(new Runnable() {
            @Override
            public void run() {

                _lastError = "Connecting...";
                Log.d(TAG, "connecting...");

                try {
                    _connection.connect();
                } catch (IOException e) {
                    Log.e(TAG, "Failed to connect", e);
                    connectionClosedOnError(e);
                    return;
                } catch (SmackException e) {
                    Log.e(TAG, "Failed to connect", e);
                    connectionClosedOnError(e);
                    return;
                } catch (XMPPException e) {
                    Log.e(TAG, "Failed to connect", e);
                    connectionClosedOnError(e);
                    return;
                } catch (InterruptedException e) {
                    Log.e(TAG, "Failed to connect", e);
                    connectionClosedOnError(e);
                    return;
                }

                //no exception, go ahead and authenticate
                login();
            }
        });
    }

    private void login() {
        if(_config == null || !_config.isValid()){
            Log.w(TAG, "Cannot login without credentials");
            connectionClosed();
            _connection.disconnect();
            return;
        }

        Log.d(TAG, "logging in as: " + _config.getUsername());
        setStatus(ConnectionStatus.authenticating);

        try {
            _connection.login(_config.getUsername(), _config.getPassword());
            _lastConnectTime = new CoordinatedTime().getMilliseconds();
            setStatus(ConnectionStatus.connected);

            //TODO dont blind set to available as due to auto reconnect logic, app may still be in background
            //TODO only set this once per ATAK startup? dont do so on reconnect
            ContactManager mgr = TAKChatUtils.takChatComponent.getManager(ContactManager.class);

            // TODO Smack workaround
            // See Bug 6969
            // Upon reconnect, our status is getting set to unavailable automatically by Smack,
            // apparently due to Smack sending "unavailable" when we call disconnect, and it getting
            // queued and then sent after the stream reconnect "resume"
            mgr.setMyStatus(mgr.DEFAULT_AVAILABLE_STATUS);

            TAKChatUtils.takChatComponent.getManager(ConnectionManager.class).onConnected();
        } catch (Exception e) {
            Log.e(TAG, "Failed to login as: " + _config.getUsername(), e);
            connectionClosedOnError(e);
            _connection.disconnect();
        }
    }

    public XMPPConnection getConnection() {
        return _connection;
    }

    public ConnectionSettings getConfig() {
        return _config;
    }

    public String getAccountDisplay() {
        if(_config == null || !_config.isValid())
            return null;

        return _config.getAccountDisplay();
    }

    ///////// ConnectionListener implementation //////////////////
    @Override
    public void connected(final XMPPConnection connection) {
        Log.d(TAG, "onConnected");
    }

    @Override
    public void connectionClosed() {
        Log.d(TAG, "connectionClosed");
        setStatus(ConnectionStatus.waiting);
        //_lastError = "Connection closed";
        ConnectionManager cm = TAKChatUtils.takChatComponent.getManager(ConnectionManager.class);
        if(cm != null)
            cm.connectionClosed(null);
    }

    @Override
    public void connectionClosedOnError(Exception e) {
        Log.w(TAG, "connectionClosedOnError", e);
        setStatus(ConnectionStatus.waiting);

        //check for special cases, better user feedback
        if(!setSpecialCaseError(e) && !setSpecialCaseError(e.getCause())){
            setDefaultError(e);
        }

        ConnectionManager cm = TAKChatUtils.takChatComponent.getManager(ConnectionManager.class);
        if(cm != null)
            cm.connectionClosed(e);
    }

    private void setDefaultError(Exception e) {
        String error = e.getMessage();
        if(FileSystemUtils.isEmpty(error) || FileSystemUtils.isEquals(error, BAD_SMACK_ERROR_MSG)) {
            error = DEFAULT_ERROR_MESSAGE +
                    (_config == null ? "" : ": " + _config.getServerAddress());
        }

        _lastError= "Connection error: " + error;
    }

    private boolean setSpecialCaseError(Throwable e) {
        if(e != null && e instanceof SASLErrorException){
            setLoginError((SASLErrorException)e);
            return true;
        } else if(e != null && e instanceof SSLException){
            setSSLError((SSLException)e);
            return true;
        }else{
            return false;
        }
    }

    @Override
    public void reconnectingIn(int seconds) {
    }

    @Override
    public void reconnectionFailed(Exception e) {
    }

    @Override
    public void reconnectionSuccessful() {
    }

    @Override
    public void authenticated(XMPPConnection connection, boolean bResumed) {
        if(bResumed){
            Log.d(TAG, "Authenticated, Resumed");
            //Not necessary here as login seems to execute so we just do this there...
            //TAKChatUtils.takChatComponent.getManager(ConnectionManager.class).onConnected();
        }else{
            Log.d(TAG, "Authenticated");
        }
    }
}