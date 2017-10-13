package com.atakmap.android.takchat.notification;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Vibrator;
import android.preference.PreferenceManager;

import com.atakmap.android.takchat.TAKChatUtils;
import com.atakmap.android.takchat.plugin.R;
import com.atakmap.coremap.log.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * Loads and plays audio files, optionally
 * Also vibrates device, optionally
 *
 * Contains logic to avoid playing or vibrating too often
 *
 * @author byoung
 */
public class SoundManager implements SoundPool.OnLoadCompleteListener, SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "SoundManager";
    private static final long REPLAY_THRESHOLD_MILLS = 1000;
    private static final long VIBRATE_THRESHOLD_MILLS = 2000;
    private static final long[] VIBRATE_PATTERN = {0, 200, 400, 350};

    /**
     * Supported sounds list
     */
    public enum SOUND{ COMING, ERROR, LEAVE, RECEIVE, SENT };

    private SoundPool _sound;
    private Vibrator _vibrator;

    private SharedPreferences _prefs;
    private boolean _bPlaySoundSent;
    private boolean _bPlaySoundRecv;
    private boolean _bPlaySoundBuddy;
    private boolean _bVibrate;

    /**
     * Do not replay the same sound many times in a row
     */
    private Map<Integer, Long> _lastPlayed;
    private long _lastVibrate;

    private int _comingId = -1;
    private int _errorId = -1;
    private int _leaveId = -1;
    private int _recvId = -1;
    private int _sentId = -1;

    private boolean _comingReady;
    private boolean _errorReady;
    private boolean _leaveReady;
    private boolean _recvReady;
    private boolean _sentReady;

    private static SoundManager _instance;

    public static SoundManager getInstance(){
        if(_instance == null){
            _instance = new SoundManager();
        }

        return _instance;
    }

    private SoundManager(){
        _prefs = PreferenceManager.getDefaultSharedPreferences(TAKChatUtils.mapView.getContext());
        _prefs.registerOnSharedPreferenceChangeListener(this);
        _bPlaySoundSent = _prefs.getBoolean("takchatAudibleNotifySent", false);
        _bPlaySoundRecv = _prefs.getBoolean("takchatAudibleNotifyRecv", false);
        _bPlaySoundBuddy = _prefs.getBoolean("takchatAudibleNotifyBuddy", false);
        _bVibrate = _prefs.getBoolean("takchatVibratePhone", false);
        _lastPlayed = new HashMap<Integer, Long>();
        _lastVibrate = 0;

        _sound = new SoundPool(5, AudioManager.STREAM_NOTIFICATION, 0);
        _sound.setOnLoadCompleteListener(this);

        _vibrator = (Vibrator) TAKChatUtils.mapView.getContext().getSystemService(Context.VIBRATOR_SERVICE);
        load();
    }

    private void load() {
        Log.d(TAG, "load");
        _comingId = _sound.load(TAKChatUtils.pluginContext, R.raw.coming, 1);
        _errorId = _sound.load(TAKChatUtils.pluginContext, R.raw.error, 1);
        _leaveId = _sound.load(TAKChatUtils.pluginContext, R.raw.leave, 1);
        _recvId = _sound.load(TAKChatUtils.pluginContext, R.raw.recv, 1);
        _sentId = _sound.load(TAKChatUtils.pluginContext, R.raw.sent, 1);
    }

    @Override
    public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
        //Log.d(TAG, "onLoadComplete: " + sampleId);
        if(status == 0) {
            if (sampleId == _comingId)
                _comingReady = true;
            if (sampleId == _errorId)
                _errorReady = true;
            if (sampleId == _leaveId)
                _leaveReady = true;
            if (sampleId == _recvId)
                _recvReady = true;
            if (sampleId == _sentId)
                _sentReady = true;
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if("takchatAudibleNotifySent".equals(key)) {
            _bPlaySoundSent = _prefs.getBoolean("takchatAudibleNotifySent", false);
        }else if("takchatAudibleNotifyRecv".equals(key)) {
            _bPlaySoundRecv = _prefs.getBoolean("takchatAudibleNotifyRecv", false);
        }else if("takchatAudibleNotifyBuddy".equals(key)){
            _bPlaySoundBuddy = _prefs.getBoolean("takchatAudibleNotifyBuddy", false);
        } else if("takchatVibratePhone".equals(key)){
            _bVibrate = _prefs.getBoolean("takchatVibratePhone", false);
        }
    }

    /**
     * Play the specified sound, based on settings
     * Also don't overplay a given sound
     *
     * @param s
     */
    public void play(SOUND s){
        if(!_bPlaySoundSent && !_bPlaySoundRecv && !_bPlaySoundBuddy && !_bVibrate){
            //Log.d(TAG, "play disabled: " + s.toString());
            return;
        }

        //get sound resource ID based on SOUND and settings
        int res = -1;
        switch(s){
            case COMING:
                if(_bPlaySoundBuddy && _comingReady)
                    res = _comingId;
                break;
            case LEAVE:
                if(_bPlaySoundBuddy &&_leaveReady)
                    res = _leaveId;
                break;
            case RECEIVE:
                if(_bPlaySoundRecv &&_recvReady)
                    res = _recvId;
                break;
            case SENT:
                if(_bPlaySoundSent && _sentReady)
                    res = _sentId;
                break;
            case ERROR:
                if(_bPlaySoundSent &&_errorReady)
                    res = _errorId;
                break;
        }

        long now = System.currentTimeMillis();
        if(res < 0){
            Log.d(TAG, "Not playing: " + s.toString());
        }else {
            //ready to play, check when we last played this sound
            if (_sound != null) {
                Integer key = Integer.valueOf(res);
                Long lastPlayed = _lastPlayed.get(key);
                if (_sound == null ||
                        (lastPlayed != null && (now - lastPlayed.longValue() < REPLAY_THRESHOLD_MILLS))) {
                    Log.d(TAG, "Skipping: " + s.toString() + ", " + (now - lastPlayed.longValue()));
                } else {
                    int streamId = _sound.play(res, 1.0f, 1.0f, 1, 0, 1.0f);
                    Log.d(TAG, "Playing: " + s.toString() + ", " + streamId);

                    //update time this sound was last played
                    _lastPlayed.put(key, Long.valueOf(now));
                }
            }
        }

        //see if we need to vibrate
        if(_bVibrate && s != SOUND.SENT){
            if(now - _lastVibrate < VIBRATE_THRESHOLD_MILLS){
                Log.d(TAG, "Skipping vibrate: " + s.toString() + ", " + (now - _lastVibrate));
            }else{
                if(_vibrator != null && _vibrator.hasVibrator()) {
                    Log.d(TAG, "Vibrating");
                    _vibrator.vibrate(VIBRATE_PATTERN, -1);
                    _lastVibrate = now;
                }else{
                    Log.w(TAG, "Unable to vibrate");
                }
            }
        }
    }

    public void dispose(){
        if(_sound != null){
            _sound.release();
            _sound = null;
        }

        if(_vibrator != null){
            _vibrator.cancel();
            _vibrator = null;
        }
    }
}
