package com.atakmap.android.takchat.data;

import com.atakmap.android.takchat.net.TAKChatXMPP;
import com.atakmap.coremap.maps.time.CoordinatedTime;

/**
 * Created by byoung on 11/22/16.
 */
public class TimedRequestHolder {
    private final long timeout;

    private final ResponseListener listener;

    public TimedRequestHolder(ResponseListener listener) {
        super();
        this.timeout = new CoordinatedTime().getMilliseconds()
                + TAKChatXMPP.PACKET_REPLY_TIMEOUT;
        this.listener = listener;
    }

    public boolean isExpired(long now) {
        return now > timeout;
    }

    public ResponseListener getListener() {
        return listener;
    }

    @Override
    public String toString() {
        return listener.toString() + ", " + timeout;
    }
}
