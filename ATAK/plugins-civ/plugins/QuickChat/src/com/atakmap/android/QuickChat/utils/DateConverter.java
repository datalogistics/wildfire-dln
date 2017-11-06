
package com.atakmap.android.QuickChat.utils;

import com.atakmap.coremap.log.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import com.atakmap.coremap.locale.LocaleUtil;

/**
 * Created by Scott Auman on 7/19/2016.
 * Util class to work with dates and time
 * @author Scott Auman
 */
public class DateConverter {

    private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
            "MM/dd/yyyy", LocaleUtil.getCurrent());
    private final String TAG = getClass().getSimpleName();

    /**Compares 2 dates and returns which date is latest
     * dates are parsed from pre defined string
     */
    public int compareDates(String date1, String date2) {

        Date d1, d2;

        if (date1 == null) {
            Log.d(TAG, date1 + " is null ");
            return 0; //no way they are the same date this is handled already so its
            //ok to send out 0 as the null default value
        }

        if (date2 == null) {
            Log.d(TAG, date2 + " is null ");
            return 0; //no way they are the same date this is handled already so its
            //ok to send out 0 as the null default value
        }

        try {
            d1 = simpleDateFormat.parse(date1);
            d2 = simpleDateFormat.parse(date2);
            return d1.compareTo(d2);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return 0; //no way they are the same date this is handled already so its
        //ok to send out 0 as the null default value
    }
}
