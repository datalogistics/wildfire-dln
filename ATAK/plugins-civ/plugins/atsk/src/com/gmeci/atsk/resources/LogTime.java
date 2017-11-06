
package com.gmeci.atsk.resources;

import android.os.SystemClock;

import com.atakmap.coremap.log.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * Measure timing between events and log w/ thread info
 */
public class LogTime {

    private static final Map<String, Long> _startTimes = new HashMap<String, Long>();

    public static void beginMeasure(String classTag, String methodTag) {
        _startTimes.put(getTag(classTag, methodTag),
                SystemClock.elapsedRealtime());
    }

    public static void endMeasure(String classTag, String methodTag) {
        Long startTime = _startTimes.get(getTag(classTag, methodTag));
        if (startTime != null)
            Log.d(classTag, methodTag + " ("
                    + (SystemClock.elapsedRealtime() - startTime)
                    + "ms)" + " on " + Thread.currentThread().getName());
    }

    private static String getTag(String classTag, String methodTag) {
        return classTag + "/" + methodTag;
    }
}
