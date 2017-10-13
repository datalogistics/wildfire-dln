
package com.atakmap.android.wxreport.service;

import com.atakmap.android.wxreport.data.Channel;

public interface WeatherServiceCallback
{
    void serviceSuccess(Channel channel);
    void startingService();
    void endingService();
    void serviceFailure(Exception exception);
}
