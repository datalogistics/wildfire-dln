
package com.gmeci.hardware;

public class CurrentOwnshipGeoPointItem {
    private static int GPS_LIFE_TIME_S = 10;
    double lat;
    double lon;
    double ce;
    double le;
    double hae;
    double course;
    double speed;
    long timestamp;
    int LockType;
    String SourceName;
    int Timer;
    String rawInfo;

    public void Update(final double lat, final double lon, final double hae,
            final String id, final int lockQuality, final double ce_m,
            final double le_m, final double course, final double speed,
            final long timestamp, final String rawInfo) {
        this.lat = lat;
        this.lon = lon;
        this.hae = hae;
        this.LockType = lockQuality;
        this.SourceName = id;
        this.Timer = GPS_LIFE_TIME_S;
        this.course = course;
        this.speed = speed;
        this.ce = ce_m;
        this.le = le_m;
        this.timestamp = timestamp;
        this.rawInfo = rawInfo;

    }
}
