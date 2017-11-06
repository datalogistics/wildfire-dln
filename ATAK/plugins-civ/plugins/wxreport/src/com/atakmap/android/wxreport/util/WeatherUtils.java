package com.atakmap.android.wxreport.util;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;

import com.atakmap.android.util.SpeedFormatter;
import com.atakmap.android.wxreport.data.Wind;
import com.atakmap.android.wxreport.plugin.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Scott Auman @Par Government on 9/19/2017.
 * Basic conversion until methods used
 */

public class WeatherUtils {

    public static String convertTempToCurrentFormat(Context c,int temp){
        if(PreferenceManager.getDefaultSharedPreferences(c).getString("weather_temp_key","0").equals("0")){
            //we are using fahrenheit as our unit
            return temp  +  "\u00B0" + "F";
        }
        return String.valueOf(convertFtoC(temp) +  "\u00B0" + "C");
    }


    public static Drawable findConditionImageByCode(Context pContext, int code) {

        //parse the code to and int
        switch (code) {
            case 0: //tornado
            case 1: //TS
            case 2: //hurricane
            case 3:
            case 4:
                return pContext.getResources().getDrawable(R.drawable.thunderstorms);
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
            case 10:
                return pContext.getResources().getDrawable(R.drawable.wintery_mix);
            case 11:
            case 12:
                return pContext.getResources().getDrawable(R.drawable.showers);
            case 13:
            case 14:
            case 15:
            case 16:
                return pContext.getResources().getDrawable(R.drawable.snow);
            case 17: //hail
            case 18: //sleet
            case 19:
            case 20:
            case 21:
            case 22:
            case 23:
            case 24:
                return pContext.getResources().getDrawable(R.drawable.windy);
            case 25:
                return pContext.getResources().getDrawable(R.drawable.cold);
            case 26:
                return pContext.getResources().getDrawable(R.drawable.cloudy);
            case 27:
                return pContext.getResources().getDrawable(R.drawable.night_partly_cloudy);
            case 28:
                return pContext.getResources().getDrawable(R.drawable.partly_cloudy);
            case 29:
                return pContext.getResources().getDrawable(R.drawable.night_partly_cloudy);
            case 30:
                return pContext.getResources().getDrawable(R.drawable.partly_cloudy);
            case 31:
                return pContext.getResources().getDrawable(R.drawable.night_clear);
            case 32:
                return pContext.getResources().getDrawable(R.drawable.sunny);
            case 33:
                return pContext.getResources().getDrawable(R.drawable.night_clear);
            case 34:
                return pContext.getResources().getDrawable(R.drawable.partly_cloudy);
            case 35:
                return pContext.getResources().getDrawable(R.drawable.sunny);
            case 36:
                return pContext.getResources().getDrawable(R.drawable.sunny);
            case 37:
            case 38:
            case 39:
                return pContext.getResources().getDrawable(R.drawable.thunderstorms);
            case 40:
                return pContext.getResources().getDrawable(R.drawable.showers);
            case 41:
            case 42:
            case 43:
                return pContext.getResources().getDrawable(R.drawable.snow);
            case 44:
                return pContext.getResources().getDrawable(R.drawable.partly_cloudy);
            case 45:
                return pContext.getResources().getDrawable(R.drawable.thunderstorms);
            case 46:
                return pContext.getResources().getDrawable(R.drawable.snow);
            case 47:
                return pContext.getResources().getDrawable(R.drawable.thunderstorms);
            default:
                //use sunny as the default if no code was parsed
                return pContext.getResources().getDrawable(R.drawable.sunny);
        }
    }

    public static String getDayOfWeek(String date){

        SimpleDateFormat spf = new SimpleDateFormat("EEEE", Locale.getDefault());
        SimpleDateFormat parser = new SimpleDateFormat("dd MMM yyyy",Locale.getDefault());
        Date d;
        Date t;
        try {
            t = parser.parse(parser.format(new Date()));
            d = parser.parse(date);
            if(t.equals(d)){
                return "Today";
            }
            return spf.format(d);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return "ERROR";
    }

    public static String getDate(String date){
        SimpleDateFormat spf = new SimpleDateFormat("MM-dd", Locale.getDefault());
        SimpleDateFormat parser = new SimpleDateFormat("dd MMM yyyy",Locale.getDefault());
        Date d;
        try {
            d = parser.parse(date);
            return spf.format(d);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return "ERROR";
    }



    private static int convertFtoC(int f){
        return (int) Math.round((f - 32) / 1.8);
    }

    private static int convertCtoF(int c){
        return (int) Math.round((c * 1.8) + 32);
    }

    public static String convertBearingIntoCardinal(Wind wind) {
        double bearing = wind.getDirection();
        if (bearing < 0 && bearing > -180) {
            // Normalize to [0,360]
            bearing = 360.0 + bearing;
        }
        if (bearing > 360 || bearing < -180) {
            return "Unknown";
        }

        String directions[] = {
                "N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
                "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW",
                "N"};
        return directions[(int) Math.floor(((bearing + 11.25) % 360) / 22.5)];
    }

    public static String convertSpeedToCurrentFormat(int speed) {
        return SpeedFormatter.getInstance().getSpeedFormatted(speed * 0.44704);
    }
}
