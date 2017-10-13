package com.gotenna.atak.plugin.plugin;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.atakmap.coremap.maps.coords.GeoPoint;
import com.gotenna.sdk.commands.GTCommand;
import com.gotenna.sdk.commands.GTCommandCenter;
import com.gotenna.sdk.commands.GTError;
import com.gotenna.sdk.interfaces.GTErrorListener;
import com.gotenna.sdk.responses.GTResponse;
import com.gotenna.sdk.types.GTDataTypes;

import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by kusbeck on 12/22/16.
 */

public class DisseminationService {

    public static final String TAG = "GoTennaDissemination";

    private static DisseminationService _instance = null;
    private DisseminationService() { }

    public static DisseminationService getInstance() {
        if(_instance == null)
            _instance = new DisseminationService();
        return _instance;
    }

    public void sendBFT(BlueForceTrack toSend) {
        blueForceTrack.set(toSend);
    }

    public void sendCoT(Message toSend) {
        addToQueue(toSend);
    }

    public void sendChat(Message toSend) {
        addToQueue(toSend);
    }

    private AtomicReference<Message> lastMessageSent = new AtomicReference<Message>(null);

    private void addToQueue(Message message) {
        if(message.getMessage().length > 256) {
            Log.w(TAG, "Message too large. Not sending over goTenna");
            return;
        } else {

            // Don't bother sending the same message twice in a row
            // (band-aid for SPoI / PLRF / etc. messages that get sent a bunch of times)
            try {
                Message lastMsgSent = lastMessageSent.getAndSet(message);
                if (lastMsgSent != null && message != null) {
                    // Compare the actual message array contents
                    if (Arrays.equals(lastMsgSent.getMessage(), message.getMessage())) {
                        // The messages are exactly the same, don't bother sending again
                        if (message.getMessage() != null) {
                            Log.d(TAG, "Not sending duplicate message of length " + message.getMessage().length);
                        }
                        return;
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Unexpected exception checking for duplicate message", e);
            }

            try {
                sendGotennaMessageNow(message, new OnMessageSendError() {
                    @Override
                    public void onError(Message message, GTError error) {
                        Log.w(TAG, "Message could not be sent over goTenna: " + error);
                        if (message.getCallback() != null) {
                            message.getCallback().onSendError(error == null ? null : error.toString());
                        }
                    }
                });
            } catch (Exception e) {
                Log.w(TAG, "Unexpected exception sending goTenna message", e);
            }
        }
        // Ignore the message queue for now
        //messageQueue.add(message);
    }

    private AtomicBoolean shouldRun = new AtomicBoolean(true);
    private AtomicReference<Context> context = new AtomicReference<Context>();

    public void start(Context context) {
        this.context.set(context);
        shouldRun.set(true);
        disseminationThread.start();
    }

    public void stop() {
        shouldRun.set(false);
    }

    private AtomicBoolean lastSendWasError = new AtomicBoolean(false);

    private SharedPreferences getPrefs() {
        if(context.get() == null) return null;
        return PreferenceManager.getDefaultSharedPreferences(context.get());
    }

    // in meters
    private int getMinDistanceToSend() {
        SharedPreferences prefs = getPrefs();
        if(prefs == null)
            return GotennaSettingsDropDownReceiver.DEFAULT_BROADCAST_LOCATION_DISTANCE;
        return getPrefs().getInt(GotennaSettingsDropDownReceiver.BROADCAST_LOCATION_DISTANCE,
                GotennaSettingsDropDownReceiver.DEFAULT_BROADCAST_LOCATION_DISTANCE);
    }

    private int getMaxTimeToWaitMillis() {
        SharedPreferences prefs = getPrefs();
        if(prefs == null)
            return 1000 * GotennaSettingsDropDownReceiver.DEFAULT_BROADCAST_LOCATION_FREQUENCY;
        return 1000 * getPrefs().getInt(GotennaSettingsDropDownReceiver.BROADCAST_LOCATION_FREQUENCY,
                GotennaSettingsDropDownReceiver.DEFAULT_BROADCAST_LOCATION_FREQUENCY);
    }

    private int getMinTimeToWaitMillis() {
        return 12 * 1000; // 12 seconds (5 messages per min)
    }

    private Thread disseminationThread = new Thread() {
        @Override
        public void run() {
            while(shouldRun.get()) {

                try {
//                    Log.d(TAG, "Ready to send...");
//                    Message toSend = messageQueue.poll(getTimeToWaitMillis(hardWait), TimeUnit.MILLISECONDS);
//                    if(toSend != null) {
//                        Log.d(TAG, "...a high-priority message");
//                        sendGotennaMessageNow(toSend, new OnMessageSendError() {
//                            @Override
//                            public void onError(Message message, GTError error) {
//                                // indicate that this message needs a resend and
//                                //  put it back on the outgoing queue
//                                message.needsResend.set(true);
//                                messageQueue.add(message);
//                            }
//                        });
//                    } else {
                        final BlueForceTrack bftToSend = blueForceTrack.get();
                        if(shouldSendBFTnow(bftToSend)) {
                            Log.d(TAG, "Sending a Blue Force Track message");
                            sendGotennaMessageNow(bftToSend, new OnMessageSendError() {
                                @Override
                                public void onError(final Message message, GTError error) {
                                    Log.w(TAG, "Error sending BFT: " + error);
                                    // put back in BFT spot if we haven't had another BFT report since
                                    blueForceTrack.compareAndSet(null, bftToSend);
                                }
                            });
                            lastBFTsent.set(bftToSend);
                            blueForceTrack.set(null);
                        }

                    int hardWait = getMinTimeToWaitMillis();
                    Thread.sleep(hardWait);

//                    }
                } catch (Exception e) {
                    // Prevent exceptions from stopping our thread
                    Log.w(TAG, "Unexpected exception", e);
                }
            }
        }
    };

    private boolean shouldSendBFTnow(BlueForceTrack bftToSend) {
        if(bftToSend == null) {
            Log.w(TAG, "Unexpectedly got a NULL BFT");
            return false;
        }
        BlueForceTrack lastBft = lastBFTsent.get();
        if(lastBft == null || lastBft.getPoint() == null) return true;
        Double distanceMoved = bftToSend.distanceTo(lastBft.getPoint());
        if(distanceMoved != null && distanceMoved > getMinDistanceToSend()) return true;
        Log.d(TAG, "Only moved " + distanceMoved + "m since last BFT - not far enough");
        long timeSinceLastSend = System.currentTimeMillis() - lastBft.getTime();
        if(timeSinceLastSend > getMaxTimeToWaitMillis()) return true;
        Log.d(TAG, "Only been " + timeSinceLastSend/1000 + "s since last BFT - not long enough");
        return false;
    }

    interface OnMessageSendError {
        void onError(Message message, GTError error);
    }

    AtomicLong lastSentMessageTime = new AtomicLong(-1);
    public void sendGotennaMessageNow(final Message toSend, final OnMessageSendError errBack) {

        GTCommandCenter.getInstance().sendBroadcastMessage(toSend.getMessage(), new GTCommand.GTCommandResponseListener() {
            @Override
            public void onResponse(GTResponse gtResponse) {
                if(gtResponse.getResponseCode().equals(GTDataTypes.GTCommandResponseCode.POSITIVE)) {
                    // log the metrics
                    if(lastSentMessageTime.get() > 0) {
                        long millisSinceLastSend = System.currentTimeMillis() - lastSentMessageTime.get();
                        Log.d(TAG, "Milliseconds since last goTenna message sent: " + millisSinceLastSend);
                    }
                    lastSentMessageTime.set(System.currentTimeMillis());

                    // trigger the callback
                    toSend.getCallback().onMessageSent();
                    Log.d(TAG, "Successfully sent goTenna Message!");

                    // since we got a message through, let's try to send a little faster
                    // TODO: figure out how to adapt the timing
                    //timeToWait.addAndGet(-(DELTA_MILLIS/10));
                }
            }
        }, new GTErrorListener() {
            @Override
            public void onError(GTError gtError) {
                if(gtError.getCode() == GTError.DATA_RATE_LIMIT_EXCEEDED) {
                    lastSendWasError.set(true);
                    // TODO: figure out apaptive backoff
//                    int ttw = timeToWait.addAndGet(DELTA_MILLIS);
//                    if(ttw > MAX_MILLIS_TO_WAIT) {
//                        timeToWait.set(MAX_MILLIS_TO_WAIT);
//                        ttw = MAX_MILLIS_TO_WAIT;
//                    }
//                    Log.d(TAG, "Data rate limit exceeded, backing off to " + ttw);
                }
                errBack.onError(toSend, gtError);
            }
        }, 3);
    }

    interface OnMessageSentCallback {
        public void onMessageSent();
        public void onSendError(String error);
    }

    private static OnMessageSentCallback EMPTY_CALLBACK = new OnMessageSentCallback() {
        @Override
        public void onMessageSent() {
            // do nothing
        }

        @Override
        public void onSendError(String error) {
            // do nothing
        }
    };

    public static class Message implements Comparable<Message> {
        protected byte[] message;
        protected OnMessageSentCallback callback;
        protected AtomicBoolean needsResend = new AtomicBoolean(false);
        public Message(byte[] message, OnMessageSentCallback callback) {
            this.message = message;
            this.callback = callback;
        }
        public byte[] getMessage() { return message; }
        public OnMessageSentCallback getCallback() {
            if(callback == null) {
                return EMPTY_CALLBACK;
            }
            return callback;
        }

        @Override
        public int compareTo(Message message) {
            if(needsResend.get()) return -1; // re-sends take higher priority
            return 0;
        }
    }

    public static class BlueForceTrack extends Message {
        private GeoPoint point;
        private long atTime = System.currentTimeMillis();
        public BlueForceTrack(byte[] message, OnMessageSentCallback callback, GeoPoint point) {
            super(message, callback);
            this.point = point;
        }
        public GeoPoint getPoint() { return point; }
        public Double distanceTo(GeoPoint otherPoint) {
            if(point == null || otherPoint == null) return null;
            return point.distanceTo(otherPoint);
        }
        public long getTime() { return atTime; }
    }

    private AtomicReference<BlueForceTrack> lastBFTsent = new AtomicReference<BlueForceTrack>();
    private AtomicReference<BlueForceTrack> blueForceTrack = new AtomicReference<BlueForceTrack>();
    private BlockingQueue<Message> messageQueue = new LinkedBlockingQueue<Message>();

}
