package com.atakmap.android.takchat.net;

import com.atakmap.android.takchat.data.IListener;
import com.atakmap.coremap.log.Log;

import org.jivesoftware.smack.AbstractXMPPConnection;

import java.util.ArrayList;
import java.util.List;

/**
 * Templated base class for managers which manage a list of <code>{@link IListener}</code>
 * The managers are in turn managed by <code>{@link com.atakmap.android.takchat.TAKChatMapComponent}</code>
 *
 * Created by byoung on 7/31/2016.
 */
public abstract class IManager<T extends IListener> {

    private final static String TAG = "IManager";

    protected List<T> _listeners;
    protected final String _tag;
    protected boolean _isShutdown;

    protected IManager(String tag){
        this._tag = tag;
        this._listeners = new ArrayList<T>();
        _isShutdown = false;
    }

    public synchronized void add(T l){
        if(_listeners.contains(l))
            return;

        Log.d(TAG, _tag + ": add: " + l.toString());
        _listeners.add(l);
    }

    public synchronized boolean remove(T l){
        if(!_listeners.contains(l))
            return false;

        Log.d(TAG, _tag + ": remove: " + l.toString());
        return _listeners.remove(l);
    }

    public synchronized void dispose(){
        if(_isShutdown)
            return;

        Log.d(TAG, _tag + ": dispose");
        for(T l : _listeners){
            l.dispose();
        }

        _isShutdown = true;
    }

    protected boolean isShutdown() {
        return _isShutdown;
    }

    /**
     * All components created
     * Invoked during startup
     */
    public void onLoaded(){}

    /**
     * Callback to initialize XMPP connection
     * Invoked before connection attempt
     *
     * @param connection
     */
    public void init(AbstractXMPPConnection connection){}
}
