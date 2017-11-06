package com.atakmap.android.takchat.data;

import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.time.CoordinatedTime;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.delay.packet.DelayInformation;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Represents an XMPP <code>{@link Message}</code>
 *
 * Created by Andrew on 9/2/2016.
 */
public class ChatMessage {

    private static final String TAG = "ChatMessage";

    private Message _message;
    private Long _time;
    private boolean _sent;
    private boolean _error;
    private boolean _delivered;
    private boolean _read;
    private List<MessageLocationLink> _locations;

    public ChatMessage(Message message, Long time) {
        _sent = false;
        _error = false;
        _delivered = false;
        _read = false;
        _locations = new ArrayList<MessageLocationLink>();
        _message = message;

        //see if a time was provided (e.g. time message was received and stored in DB)
        if(time != null && time.longValue() > -1){
            _time = time.longValue();
            Log.d(TAG, "time set: " + _time);
        }else{
            //set time to now (e.g. just received from network/server)
            _time = Long.valueOf(new CoordinatedTime().getMilliseconds());
            Log.d(TAG, "time now: " + _time);
        }

        long delay = getDelay(message);
        if(delay >= 0){
            _time = delay;
        }
    }

    /**
     * Get server timestamp from message
     * Currently using "DelayInformation" XEP-0203
     *
     * @param message
     * @return  timestamp, or -1 if none available
     */
    public static long getDelay(Message message) {
        DelayInformation delay = DelayInformation.from(message);
        if(delay != null){
            Date stamp = delay.getStamp();
            if(stamp != null){
                long delayL = new CoordinatedTime(stamp.getTime()).getMilliseconds();
                if(delayL > 0){
                    Log.d(TAG, "Set delayed message time: " + delayL + ", from " + stamp.toString());
                    return delayL;
                }
            }
        }

        return -1;
    }

    public ChatMessage(Message message) {
        this(message, null);
    }

    public Message getMessage() {
        return _message;
    }

    public Long getTime() {
        return _time;
    }

    public boolean isSent() {
        return _sent;
    }

    public void setSent(boolean sent) {
        this._sent = sent;
    }

    public boolean isError() {
        return _error;
    }

    public void setError(boolean error) {
        this._error = error;
    }

    public boolean isDelivered() {
        return _delivered;
    }

    public void setDelivered(boolean delivered) {
        this._delivered = delivered;
    }

    public boolean isRead() {
        return _read;
    }

    public void setRead(boolean read) {
        this._read = read;
    }

    public boolean hasLocations(){
        return !FileSystemUtils.isEmpty(_locations);
    }

    public List<MessageLocationLink> getLocations() {
        return _locations;
    }

    public void setLocations(List<MessageLocationLink> locations) {
        if(locations == null){
            _locations.clear();
            Log.d(TAG, "Cleared locations");
            return;
        }

        if(locations.size() > 0)
            Log.d(TAG, "Set location count: " + locations.size());

        this._locations = locations;
    }

    @Override
    public String toString() {
        return _message + ", " + _message.getBody() + ", " + _time + " " + _sent + " " + _error + " " + _delivered + " " + _read;
    }

    @Override
    public boolean equals(Object o) {
        if(!(o instanceof ChatMessage))
            return super.equals(o);

        ChatMessage rhs = (ChatMessage)o;
        if(_message == null || rhs._message == null)
            return false;

        return FileSystemUtils.isEquals(_message.getStanzaId(), rhs._message.getStanzaId());
    }

    @Override
    public int hashCode() {
        return _message.hashCode();
    }
}
