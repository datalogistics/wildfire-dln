
package com.atakmap.android.takchat;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.atakmap.android.image.ImageGalleryReceiver;
import com.atakmap.android.ipc.AtakBroadcast.DocumentedIntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;

import com.atakmap.android.contact.ContactUtil;
import com.atakmap.android.cot.CotMapComponent;
import com.atakmap.android.data.DataMgmtReceiver;
import com.atakmap.android.dropdown.DropDownMapComponent;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapEvent;
import com.atakmap.android.maps.MapEventDispatcher;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.network.ui.CredentialsPreference;
import com.atakmap.android.takchat.api.TAKChatApi;
import com.atakmap.android.takchat.data.ChatDatabase;
import com.atakmap.android.takchat.data.HeadlineListener;
import com.atakmap.android.takchat.data.TAKChatContactHandler;
import com.atakmap.android.takchat.data.TimerListener;
import com.atakmap.android.takchat.data.XmppConference;
import com.atakmap.android.takchat.net.ConferenceManager;
import com.atakmap.android.takchat.net.ConnectionManager;
import com.atakmap.android.takchat.net.ConnectivityNotificationProxy;
import com.atakmap.android.takchat.net.ContactManager;
import com.atakmap.android.takchat.net.DeliveryReceiptManager;
import com.atakmap.android.takchat.net.IManager;
import com.atakmap.android.takchat.net.MessageManager;
import com.atakmap.android.takchat.net.MessageUnreadManager;
import com.atakmap.android.takchat.net.TAKChatXMPP;
import com.atakmap.android.takchat.net.VCardManager;
import com.atakmap.android.takchat.notification.SoundManager;
import com.atakmap.android.takchat.plugin.R;
import com.atakmap.android.takchat.plugin.TAKChatTool;
import com.atakmap.android.takchat.view.ConferenceInvitationListener;
import com.atakmap.android.takchat.view.TAKChatPreferenceFragment;
import com.atakmap.android.takchat.view.TAKContactProfileView;
import com.atakmap.app.preferences.NetworkConnectionPreferenceFragment;
import com.atakmap.app.preferences.ToolsPreferenceFragment;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;
import com.atakmap.net.AtakAuthenticationCredentials;
import com.atakmap.net.AtakAuthenticationDatabase;

import org.jivesoftware.smack.AbstractXMPPConnection;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * The XMPP chat <code>{@link com.atakmap.android.maps.MapComponent}</code>
 * Coordinates the various <code>{@link IManager}</code> instances
 * Processes lifecycle and preference changed events
 *
 * @author byoung
 */
public class TAKChatMapComponent extends DropDownMapComponent{

    public static final String TAG = "TAKChatMapComponent";
    public static final String TAKCHAT_PREFERENCE = "takchatPreference";
    private static final long TIMER_DELAY_MILLIS = 1000;

    private TAKChatDropDownReceiver _ddr;
    private TAKChatApi _api;
    private SharedPreferences _prefs;
    private List<IManager> _managers;
    private boolean _isShutdown;
    private ExecutorService _backgroundSvc;

    /**
     * ctor
     */
    public TAKChatMapComponent(){}

    public void onCreate(final Context context, Intent intent, final MapView view) {
        Log.d(TAG, "Creating TAK Chat plugin...");
        super.onCreate(context, intent, view);

        _isShutdown = false;
        _prefs = PreferenceManager.getDefaultSharedPreferences(view.getContext());
        SoundManager.getInstance();

        //add _prefs
        ToolsPreferenceFragment
                .register(
                        new ToolsPreferenceFragment.ToolPreference(
                                TAKChatUtils.getPluginString(R.string.takchat_prefs),
                                TAKChatUtils.getPluginString(R.string.takchat_prefs_adjust),
                                TAKCHAT_PREFERENCE,
                                TAKChatUtils.pluginContext.getResources().getDrawable(
                                        R.drawable.ic_launcher),
                                new TAKChatPreferenceFragment(TAKChatUtils.pluginContext)));

        _managers = new ArrayList<IManager>();

        ConnectionManager connectionManager = new ConnectionManager(_prefs);
        MessageManager messageManager = new MessageManager();
        VCardManager vCardManager = new VCardManager(_prefs);

        final ContactManager contactManager = new ContactManager();
        final DeliveryReceiptManager receiptManager = new DeliveryReceiptManager();
        final MessageUnreadManager unreadManager = new MessageUnreadManager();
        _managers.add(contactManager);
        _managers.add(vCardManager);
        _managers.add(messageManager);
        _managers.add(connectionManager);
        _managers.add(receiptManager);
        _managers.add(unreadManager);

        connectionManager.add(contactManager);
        connectionManager.add(receiptManager);

        ConnectivityNotificationProxy notificationProxy = new ConnectivityNotificationProxy();
        connectionManager.add(notificationProxy);

        messageManager.add(ChatDatabase.getInstance(context));
        messageManager.add(new HeadlineListener());
        receiptManager.add(ChatDatabase.getInstance(context));
        unreadManager.add(ChatDatabase.getInstance(context));
        unreadManager.add(notificationProxy);
        TAKChatTool pluginTool = TAKChatTool.getInstance();
        if(pluginTool != null){
            pluginTool.refreshIcon();
            unreadManager.add(pluginTool);
            connectionManager.add(pluginTool);
        }else{
            Log.w(TAG, "Failed to register plugin tool listener");
        }

        _ddr = new TAKChatDropDownReceiver(_prefs);
        //The TAKContactsView is a message router for all the chat views, currently
        messageManager.add(_ddr.getView());
        connectionManager.add(_ddr.getView());
        receiptManager.add(_ddr.getView());
        //TODO do we need both to listen? Maybe use a tighter copuling of these two...
        contactManager.add(_ddr.getView());
        contactManager.add(_ddr.getAdapter());
        unreadManager.add(_ddr.getAdapter());

        DocumentedIntentFilter filter = new DocumentedIntentFilter();
        filter.addAction(TAKChatDropDownReceiver.SHOW_CONTACT_LIST);
        filter.addAction(TAKChatDropDownReceiver.SHOW_CHAT);
        filter.addAction(TAKChatDropDownReceiver.SHOW_HEADLINE);
        filter.addAction(TAKChatDropDownReceiver.SHOW_PROFILE);
        filter.addAction(TAKChatDropDownReceiver.SHOW_CONF_PROFILE);
        filter.addAction(TAKChatDropDownReceiver.AVATAR_CAPTURED);
        filter.addAction(TAKChatDropDownReceiver.CLEAR_HISTORY);
        filter.addAction(TAKChatDropDownReceiver.OPEN_SEARCH);
        filter.addAction(DataMgmtReceiver.ZEROIZE_CONFIRMED_ACTION);
        this.registerReceiver(context, _ddr, filter);

        //do this after _ddr creation to ensure page adapter construction
        //TODO what other listeners? UI views to display avatar? in listview? or just contactinfo?
        ConferenceManager confManager = new ConferenceManager();
        vCardManager.add(ChatDatabase.getInstance(context));
        vCardManager.add(TAKContactProfileView.getInstance());

        _managers.add(confManager);
        messageManager.add(confManager);
        confManager.add(new ConferenceInvitationListener());
        connectionManager.add(confManager);

        filter = new DocumentedIntentFilter();
        filter.addAction(CredentialsPreference.CREDENTIALS_UPDATED);
        filter.addAction(NetworkConnectionPreferenceFragment.CERTIFICATE_UPDATED);
        this.registerReceiver(context, _credListener, filter);

        _backgroundSvc = Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable, "TAK Chat Worker");
                thread.setPriority(Thread.MIN_PRIORITY);
                thread.setDaemon(true);
                return thread;
            }
        });

        CotMapComponent.getInstance().getContactConnectorMgr()
                .addContactHandler(new TAKChatContactHandler());

        //components initialize before connection attempt
        onLoaded();

        //begin connection attempt
        connectionManager.startup();
        _prefs.registerOnSharedPreferenceChangeListener(prefListener);

        //listen for new team members
        view.getMapEventDispatcher().addMapEventListener(
                MapEvent.ITEM_ADDED, _mapEventListener);
        view.getMapEventDispatcher().addMapEventListener(
                MapEvent.ITEM_REMOVED, _mapEventListener);

        filter = new DocumentedIntentFilter();
        filter.addAction("com.atakmap.android.ACTIVITY_FINISHED");
        AtakBroadcast.getInstance().registerReceiver(_activityResultReceiver, filter);

        startTimer();

        _api = TAKChatApi.getInstance();
        _api.sendConferences();
    }

    private SharedPreferences.OnSharedPreferenceChangeListener prefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            //TODO implement prefs for audio/vibrate/export/clear, based on GeoChat code

            if (key.equals("takchatEnabled")){
                //clear the suppression of the user dialog during startup
                sharedPreferences.edit().remove("takChatSuppressDisabledDialog").apply();

                //reset the contact list "extra row"
                _ddr.refreshContactList(false, false, false);

                boolean bEnabled = sharedPreferences.getBoolean("takchatEnabled", true);
                if (bEnabled){
                    if(TAKChatXMPP.getInstance().isConnected()){
                        Log.d(TAG, "Chat enabled, Already connected");
                    }else{
                        Log.d(TAG, "Chat enabled, Connecting...");
                        getManager(ConnectionManager.class).forceReconnect();
                    }
                } else {
                    if(TAKChatXMPP.getInstance().isConnected()){
                        Log.d(TAG, "Chat disabled, Disconnecting...");
                    }else{
                        Log.d(TAG, "Chat disabled, Already disconnected");
                    }

                    //force disconnect either way since a connect attempt may be pending...
                    getManager(ConnectionManager.class).forceReconnect();
                }
            }

            //TODO listen on takchatCredentials, truststore, etc
            else if (key.equals("takchatServerPort") ||
                    key.equals("takchatRequireSSL")) {
                //TODO if takchatRequireSSL is set to true, then reconnects but still does not use SSL
                getManager(ConnectionManager.class).forceReconnect();
            } else if (key.equals("takChatSyncArchiveTime")) {
                //TODO should we delete locally stored messages which are older than selected time?
                clearSyncTimes();

                //req-query with updated time
                getManager(ConnectionManager.class).queryArchive();
                getManager(ConferenceManager.class).joinAll(false);
            } else if(key.equals("takchatShowOffline")){
                if(_ddr != null){
                    _ddr.refreshContactList(false, false, false);
                }
            } else if(key.equals("takchatUseCustomHost")){
                boolean takchatUseCustomHost = sharedPreferences.getBoolean("takchatUseCustomHost", false);
                Log.d(TAG, "takchatUseCustomHost updated: " + takchatUseCustomHost);
                getManager(ConnectionManager.class).forceReconnect();
            } else if(key.equals("takchatCustomHost")){
                String takchatCustomHost = sharedPreferences.getString("takchatCustomHost", null);
                Log.d(TAG, "takchatCustomHost updated: " + takchatCustomHost);
                getManager(ConnectionManager.class).forceReconnect();
            } else if(key.equals("takchatUseTakServerTrustStore")){
                boolean takchatUseTakServerTrustStore = sharedPreferences.getBoolean("takchatUseTakServerTrustStore", true);
                Log.d(TAG, "takchatUseTakServerTrustStore updated: " + takchatUseTakServerTrustStore);
                getManager(ConnectionManager.class).forceReconnect();
            }
        }
    };


    /**
     * Clear local sync time state
     */
    private void clearSyncTimes() {
        //reset last sync time so we can re-query with no range regardless of last query
        TAKChatUtils.clearSyncPoint(_prefs, ConnectionManager.DEFAULT_SYNC_PREF_NAME);

        //then reset sync all conferences
        ArrayList<XmppConference> confs = TAKChatUtils.takChatComponent.getManager(ContactManager.class).getConferences();
        if(FileSystemUtils.isEmpty(confs)){
            Log.d(TAG, "No conferences to sync");
        }else {
            for (XmppConference conference : confs) {
                //TODO need null/error check?
                String conferenceName = conference.getId().getLocalpartOrNull().toString();
                TAKChatUtils.clearSyncPoint(_prefs, conferenceName);
            }
        }
    }

    /**
     * Clear local sync time state
     * Clear unread message count
     * Stop reconnect attempt
     */
    private void credentialsChanged() {
        getManager(MessageUnreadManager.class).onUnreadCountChanged(null);
        clearSyncTimes();
        //TODO stop reconnect attempt...
        //ReconnectionManager.getInstanceFor(_connection).disableAutomaticReconnection();
    }

    private BroadcastReceiver _credListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (CredentialsPreference.CREDENTIALS_UPDATED.equals(intent.getAction())) {
                String credType = intent.getStringExtra("type");
                Log.d(TAG, "Credentials changed: " + credType);

                //see if user XMPP creds or chat truststore passwd were updated
                if(TAKChatUtils.getPluginString(R.string.xmpp_credentials).equals(credType)
                    || TAKChatUtils.getPluginString(R.string.caChatPassword).equals(credType)){
                    getManager(ConnectionManager.class).forceReconnect();
                }

                //update outgoing self SA
                if(TAKChatUtils.getPluginString(R.string.xmpp_credentials).equals(credType)){
                    credentialsChanged();
                    updateXmppUsernamePref();
                }

                if(_ddr != null){
                    //update username displayed in drop down
                    _ddr.refreshContactList(true, true, true);
                }
            } else if (NetworkConnectionPreferenceFragment.CERTIFICATE_UPDATED.equals(intent.getAction())) {
                String certType = intent.getStringExtra("type");
                //see if chat truststore was loaded
                if(TAKChatUtils.getPluginString(R.string.caChatTrustStore).equals(certType)){
                    getManager(ConnectionManager.class).forceReconnect();
                }
            }
        }
    };

    private void updateXmppUsernamePref() {
        AtakAuthenticationCredentials credentials = AtakAuthenticationDatabase
                .getCredentials(TAKChatUtils.getPluginString(R.string.xmpp_credentials));
        if (credentials != null && !FileSystemUtils.isEmpty(credentials.username)) {
            Log.d(TAG, "Updating XMPP username setting: " + credentials.username);
            _prefs.edit().putString("saXmppUsername", credentials.username).apply();
        }else{
            _prefs.edit().remove("saXmppUsername").apply();
        }
    }

    /**
     * Listen for new TAK users coming online
     */
    private MapEventDispatcher.MapEventDispatchListener _mapEventListener = new MapEventDispatcher.MapEventDispatchListener() {
        @Override
        public void onMapEvent(MapEvent event) {
            final MapItem item = event.getItem();
            if(ContactUtil.isTakContact(item)) {
                ContactManager cm = getManager(ContactManager.class);
                if(cm != null) {
                    if(FileSystemUtils.isEquals(event.getType(), MapEvent.ITEM_ADDED)) {
                        cm.itemAdded(item);
                    }else if(FileSystemUtils.isEquals(event.getType(), MapEvent.ITEM_REMOVED)) {
                        cm.itemRemoved(item);
                    }
                }
            }
        }
    };

    BroadcastReceiver _activityResultReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle extras = intent.getExtras();
            if (extras == null) {
                Log.d(TAG, "No extras");
                return;
            }

            final int requestCode = extras.getInt("requestCode");
            final int resultCode = extras.getInt("resultCode");
            final Intent data = (Intent) extras.getParcelable("data");

            Log.d(TAG, "Got Activity Result: "
                    + (resultCode == Activity.RESULT_OK ? "OK" : "ERROR")
                    + " for request " + requestCode);
            if (resultCode != Activity.RESULT_OK) {
                Log.d(TAG, "Skipping result: " + resultCode);
                return;
            }

            if (data == null || data.getData() == null) {
                Log.w(TAG, "Skipping result, no data: " + resultCode);
                return;
            }

            String filePath = null;
            boolean isTemp = false;
            Uri dataUri = data.getData();
            if (dataUri != null) {
                filePath = ImageGalleryReceiver.getContentField(context, dataUri,
                        MediaStore.Images.ImageColumns.DATA);
                if (filePath == null) {
                    // Newer versions of Android may not give a path URI (just byte data and file name)
                    // So we'll need to copy the data to a temp file
                    String name = ImageGalleryReceiver.getContentField(context,
                            dataUri, MediaStore.Images.ImageColumns.DISPLAY_NAME);
                    if (name != null) {
                        try {
                            File tmpFile = new File(FileSystemUtils.validityScan(
                                    FileSystemUtils.getItem(FileSystemUtils.TMP_DIRECTORY)
                                            + File.separator + name));
                            ImageGalleryReceiver.extractContent(context, dataUri, tmpFile);
                            filePath = tmpFile.getAbsolutePath();
                            isTemp = true;
                        } catch (Exception e) {
                            filePath = null;
                        }
                    }
                }
            } else {
                Log.d(TAG, "Skipping result: " + resultCode);
                return;
            }

            if (!FileSystemUtils.isFile(filePath)) {
                Log.w(TAG, "Skipping missing result: " + resultCode + ", "
                        + filePath);
                return;
            }

            TAKChatUtils.takChatComponent.getManager(VCardManager.class).setMyAvatar(filePath);
            if (isTemp)
                FileSystemUtils.delete(filePath);
        }
    };

    /**
     * Start periodically callbacks.
     */
    private void startTimer() {
        TAKChatUtils.runOnUiThreadDelayed(timerRunnable, TIMER_DELAY_MILLIS);
    }

    private final Runnable timerRunnable = new Runnable() {

        @Override
        public void run() {
            for (TimerListener listener : getManagers(TimerListener.class))
                listener.onTimer();

            if (!_isShutdown)
                startTimer();
        }

    };

    @Override
    public void onPause(Context context, MapView view) {
        //TODO also away if screen turns off?
        Log.d(TAG, "onPause...");
        super.onPause(context, view);
        getManager(ContactManager.class).sendAway();
    }

    @Override
    public void onResume(Context context, MapView view) {
        Log.d(TAG, "onResume...");
        super.onResume(context, view);
        getManager(ContactManager.class).sendAvailable();
    }

    @Override
    public void onStop(Context context, MapView view) {
        Log.d(TAG, "onStop...");
        super.onStop(context, view);
    }

    /**
     * Get specified manager class
     *
     * @param clazz
     * @param <T>
     * @return
     */
    public <T extends IManager> T getManager(Class<T> clazz) {
        if (_isShutdown) {
            Log.w(TAG, "getManager already shutdown: " + clazz.getName());
            return null;
        }

        synchronized (_managers) {
            for (IManager manager : _managers) {
                if (clazz.isInstance(manager)) {
                    //Log.d(TAG, "Found manager: " + clazz.getName());
                    return ((T) manager);
                }
            }
        }

        Log.w(TAG, "Failed to find: " + clazz.getName());
        return null;
    }

    /**
     * Get specified manager class
     *
     * @param clazz
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> getManagers(Class<T> clazz) {
        if (_isShutdown) {
            Log.w(TAG, "getManagers already shutdown: " + clazz.getName());
            return Collections.emptyList();
        }
        List<T> collection = new ArrayList<T>();
        for (IManager manager : _managers) {
            if (clazz.isInstance(manager)) {
                //Log.d(TAG, "Found manager: " + clazz.getName());
                collection.add((T) manager);
            }
        }
        return Collections.unmodifiableList(collection);
    }

    public void init(AbstractXMPPConnection connection){
        Log.d(TAG, "init" + connection.toString());
        synchronized (_managers) {
            for (IManager manager : _managers) {
                manager.init(connection);
            }
        }
    }

    public void onLoaded(){
        Log.d(TAG, "onLoaded");
        synchronized (_managers) {
            for (IManager manager : _managers) {
                manager.onLoaded();
            }
        }
    }

    public TAKChatDropDownReceiver getDropDown() {
        return _ddr;
    }

    @Override
    protected void onDestroyImpl(Context context, MapView view) {
        super.onDestroyImpl(context, view);
        Log.d(TAG, "onDestroyImpl...");

        _api.dispose();

        //TODO does Smack do this? apparently not consistently..
        ContactManager cm = getManager(ContactManager.class);
        if(cm != null) {
            cm.sendOffline();
        }

        SoundManager.getInstance().dispose();

        synchronized (_managers) {
            for (IManager manager : _managers) {
                manager.dispose();
            }

            //Now that all have shutdown, disconnect from server
            getManager(ConnectionManager.class).disconnect();
            _managers.clear();
        }

        _isShutdown = true;

        ToolsPreferenceFragment.unregister(TAKCHAT_PREFERENCE);

        if(prefListener != null && _prefs != null){
            _prefs.unregisterOnSharedPreferenceChangeListener(prefListener);
            prefListener = null;
        }

        if(_credListener != null){
            this.unregisterReceiver(TAKChatUtils.pluginContext, _credListener);
            _credListener = null;
        }

        if (_activityResultReceiver != null) {
            AtakBroadcast.getInstance().unregisterReceiver(_activityResultReceiver);
            _activityResultReceiver = null;
        }

        if(_ddr != null) {
            _ddr.dispose();
            _ddr = null;
        }

        if(_mapEventListener != null) {
            view.getMapEventDispatcher().removeMapEventListener(MapEvent.ITEM_ADDED, _mapEventListener);
            view.getMapEventDispatcher().removeMapEventListener(MapEvent.ITEM_REMOVED, _mapEventListener);
            _mapEventListener = null;
        }

        if(_backgroundSvc != null) {
            _backgroundSvc.shutdownNow();
        }
    }

    /**
     * Submits request to be executed in background.
     */
    public void runInBackground(final Runnable runnable) {
        _backgroundSvc.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    runnable.run();
                } catch (Throwable t) {
                    Log.e(TAG, "Background service", t);
                }
            }
        });
    }
}
