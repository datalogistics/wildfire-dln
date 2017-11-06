package com.atakmap.android.takchat.net;

import com.atakmap.coremap.filesystem.FileSystemUtils;

import org.jxmpp.jid.EntityFullJid;

/**
 * Stores details of the XMPP server connection
 *
 * Created by byoung on 7/8/2016.
 */
public class ConnectionSettings{

    private EntityFullJid _username;
    private String _customHost;
    private int _serverPort;
    private String _password;
    private String _takUid;

    /**
     * true to use encryption
     */
    private boolean _bRequireEncryption;

    /**
     * If using encryption, can use specific truststore, or delegate to OS
     */
    private byte[] _trustStore;
    private String _trustStorePassword;

    public ConnectionSettings(EntityFullJid username, String customHost, int serverPort,
                              String password, String takUid,
                              boolean bRequireEncryption,
                              byte[] trustStore, String trustStorePassword) {
        this._username = username;
        this._customHost = customHost;
        this._serverPort = serverPort;
        this._username = username;
        this._password = password;
        this._takUid = takUid;
        this._bRequireEncryption = bRequireEncryption;
        this._trustStore = trustStore;
        this._trustStorePassword = trustStorePassword;
    }

    public String getServerAddress() {
        return _username.getDomain().toString();
    }

    public boolean hasCustomHost(){
        return !FileSystemUtils.isEmpty(_customHost);
    }

    public String getCustomHost(){ return _customHost; }

    public int getServerPort() {
        return _serverPort;
    }

    public String getUsername() {
        return _username.getLocalpartOrNull().toString();
    }

    public String getPassword() {
        return _password;
    }

    public String getTakUid(){ return _takUid; }

    public String getResource() {
        return _username.getResourceOrNull().toString();
    }

    public boolean isRequireEncryption() {
        return _bRequireEncryption;
    }

    public byte[] getTrustStore() {
        return _trustStore;
    }

    public String getTrustStorePassword() {
        return _trustStorePassword;
    }

    public boolean hasTrustStore(){
        return _trustStore != null && _trustStore.length > 0 &&
                _trustStorePassword != null && _trustStorePassword.length() > 0;
    }

    @Override
    public String toString() {
        return _username.toString();
    }

    public boolean isValid(){
        return _username != null &&
                _username.getLocalpartOrNull() != null &&
                !FileSystemUtils.isEmpty(_username.getLocalpartOrNull().toString()) &&
                _username.getDomain() != null &&
                !FileSystemUtils.isEmpty(_username.getDomain().toString()) &&
                _username.getResourceOrNull() != null &&
                !FileSystemUtils.isEmpty(_username.getResourceOrNull().toString()) &&
                !FileSystemUtils.isEmpty(_password) &&
                _serverPort > 0 &&
                (_bRequireEncryption ? hasTrustStore() : true);
    }

    /**
     * Validate required settings and return appropriate error message;
     * @return
     */
    public String validate(){
        if(isValid())
            return null;

        if(_username == null)
            return "User Credentials required";

        if(_username.getDomain() == null)
            return "Server URL required";
        if(!_username.hasLocalpart())
            return "Username required";
        if(FileSystemUtils.isEmpty(_password))
            return "Password required";
        if(!_username.hasResource())
            return "Chat resource required";
        if(_serverPort <= 0)
            return "Server port invalid";
        if(_bRequireEncryption && !hasTrustStore())
            return "TLS Truststore & password required";

        return null;
    }

    public String getAccountDisplay() {
        return _username.asBareJid().toString();
    }
}
