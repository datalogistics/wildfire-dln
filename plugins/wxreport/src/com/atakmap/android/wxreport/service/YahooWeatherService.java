
package com.atakmap.android.wxreport.service;

import android.net.Uri;
import android.os.AsyncTask;

import com.atakmap.android.wxreport.data.Channel;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;

public class YahooWeatherService
{
    private WeatherServiceCallback callback;
    private String location;
    private Exception error;
    private Channel channel;
    public static String TAG = "YahooWeatherService";

    public YahooWeatherService(WeatherServiceCallback callback)
    {
        this.callback = callback;
    }

    public String getLocation()
    {
        return location;
    }

    private void refreshWeather(String loc)
    {
        location = loc;
        new AsyncTask<String, Void, String>()
        {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                callback.startingService();
            }

            @Override
            protected String doInBackground(String... strings)
            {
                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                String YQL = String
                        .format("select * from weather.forecast where woeid in (select woeid from geo.places(1) where text=\"%s\")",
                                strings[0]);
                //String YQL = String.format("select * from weather.forecast where woeid in (select woeid from geo.places(1) where text=\"(42.2,-78.6)\")");
                String endpoint = String
                        .format("https://query.yahooapis.com/v1/public/yql?q=%s&format=json",
                                Uri.encode(YQL));
                try
                {
                    // Create a URL connection to send the weather request
                    // Set parameters for the connection
                    URL url = new URL(endpoint);
                    URLConnection connection = url.openConnection();
                    connection.setConnectTimeout(1000);
                    connection.setReadTimeout(500);
                    connection.connect();
                    InputStream inputstream = connection.getInputStream();

                    //Coverity issue 18364 EMD, change if expecting a specific encodeing from yahoo.  Might want to consider UTF8
                    BufferedReader reader = new BufferedReader(
                            (new InputStreamReader(inputstream,
                                    Charset.defaultCharset())));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null)
                    {
                        result.append(line);
                    }
                    return result.toString();
                } catch (Exception e)
                {
                    error = e;
                }
                return null;
            }

            @Override
            protected void onPostExecute(String s)
            {
                callback.endingService();
                if (s == null && error != null)
                {
                    callback.serviceFailure(error);
                    return;
                }

                try
                {
                    JSONObject data = new JSONObject(s);
                    JSONObject quearyResults = data.optJSONObject("query");
                    int count = quearyResults.optInt("count");
                    if (count == 0)
                    {
                        callback.serviceFailure(new LocationWeatherException(
                                "No Weather: " + location));
                        return;
                    }
                    channel = new Channel();
                    channel.populate(quearyResults.optJSONObject("results")
                            .optJSONObject("channel"));
                    callback.serviceSuccess(channel);
                } catch (JSONException e)
                {
                    callback.serviceFailure(e);
                }

            }
        }.execute(location);
    }

    public void refreshWeather(){
        if(location != null){
            refreshWeather(location);
        }
    }

    public void refreshWeather(final String lat, final String lon)
    {
        // Set a default location and clear the channel data
        location = "Please Wait";
        channel = null;

        new AsyncTask<String, Void, String>()
        {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                callback.startingService();
            }

            @Override
            protected String doInBackground(String... strings)
            {
                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                String YQL = String
                        .format("select * from weather.forecast where woeid in (select woeid from geo.places(1) where text=\"(%s,%s)\")",
                                lat, lon);
                String endpoint = String
                        .format("https://query.yahooapis.com/v1/public/yql?q=%s&format=json",
                                Uri.encode(YQL));
                try
                {
                    // Create a URL connection to send the weather request
                    // Set parameters for the connection
                    URL url = new URL(endpoint);
                    URLConnection connection = url.openConnection();
                    connection.setConnectTimeout(1000);
                    connection.setReadTimeout(500);
                    connection.connect();
                    InputStream inputstream = connection.getInputStream();

                    //Coverity issue 18365 EMD, change if expecting a specific encodeing from yahoo.  Might want to consider UTF8
                    BufferedReader reader = new BufferedReader(
                            (new InputStreamReader(inputstream,
                                    Charset.defaultCharset())));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null)
                    {
                        result.append(line);
                    }
                    return result.toString();
                } catch (Exception e)
                {
                    error = e;
                }
                return null;
            }

            @Override
            protected void onPostExecute(String s)
            {
                callback.endingService();
                if (s == null && error != null)
                {
                    callback.serviceFailure(error);
                    return;
                }

                try
                {
                    JSONObject data = new JSONObject(s);
                    JSONObject quearyResults = data.optJSONObject("query");
                    int count = quearyResults.optInt("count");
                    if (count == 0)
                    {
                        callback.serviceFailure(new LocationWeatherException(
                                "No Weather: " + location));
                        return;
                    }
                    Channel local_channel = new Channel();
                    local_channel.populate(quearyResults.optJSONObject(
                            "results").optJSONObject("channel"));
                    String real_location = (local_channel.getLocation().getCity()
                            .getCity()
                            + ", "
                            + local_channel.getLocation().getRegion()
                            .getRegion());
                    refreshWeather(real_location);
                } catch (JSONException e)
                {
                    callback.serviceFailure(e);
                }
            }
        }.execute(location);
    }

    //Coverity issue 18368 Inner classes should be static if they do not need access to the containing classes variables.
    private static class LocationWeatherException extends Exception
    {
        LocationWeatherException(String detailMessage)
        {
            super(detailMessage);
        }
    }
}
