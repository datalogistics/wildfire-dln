
package com.gmeci.hardware.parsers;

import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.gmeci.core.SurveyPoint;
import com.gmeci.conversions.Conversions;
import com.gmeci.hardware.ChecksumTest;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.SimpleTimeZone;

public class NMEAParser extends ParserBase {

    private static final String PTNL_NMEA_TYPE = "PTNL";
    private static final String GST_NMEA_TYPE = "GST";
    private static final String GGA_NMEA_TYPE = "GGA";
    private static final String GPRMC_NMEA_TYPE = "GPRMC";
    private static final String PLTIT_NMEA_TYPE = "PLTIT";
    private static final String EXTERNAL_SOURCE = "External";

    private static final int MESSAGE_TYPE_INDEX = 1;
    private static final int RANGE_INDEX = 2;
    private static final int RANGE_UNITS_INDEX = 3;
    private static final int AZIMUTH_INDEX = 4;
    private static final int INCLINATION_INDEX = 6;
    private static final int SLOPE_DISTANCE_INDEX = 8;
    private static final int SLOPE_DISTANCE_UNITS_INDEX = 9;

    private static final String TAG = "NMEAParser";
    private static final int BUFFER_SIZE = 1024;
    private static final int INVALID_INDEX = -1;
    public int GPSQualityInt = 0;
    SurveyPoint CurrentOwnshipPosition = new SurveyPoint();
    HashMap<Integer, RollingBufferItems> rollingBufferMap = new HashMap<Integer, RollingBufferItems>();
    int currentSize = 0;
    int TotalSize = 0;
    byte[] rollingBuffer;
    private boolean PTNLMessagePresent = false;

    static double Latitude2Decimal(String lat, String NS) {
        double degree, minute, total;
        if (lat.length() < 3)
            return 0.0;

        degree = Double.parseDouble(lat.substring(0, 2));
        minute = Double.parseDouble(lat.substring(2)) / 60d;
        total = degree + minute;
        if (NS.startsWith("S"))
            total = -total;
        return total;
    }

    static double Longitude2Decimal(String lon, String WE) {
        double degree, minute, total;
        if (lon.length() < 4)
            return 0.0;
        degree = Double.parseDouble(lon.substring(0, 3));
        minute = Double.parseDouble(lon.substring(3)) / 60d;
        total = degree + minute;
        if (WE.startsWith("W"))
            total = -total;
        return total;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        CurrentOwnshipPosition.circularError = 9999999;
        CurrentOwnshipPosition.linearError = 9999999;
    }

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        initService();
        return super.onStartCommand(intent, flags, startId);
    }

    public boolean ReadSerialData(byte[] data, int length, int StreamID) {
        if (!rollingBufferMap.containsKey(StreamID)) {
            RollingBufferItems newRollingBuffer = new RollingBufferItems();
            rollingBufferMap.put(StreamID, newRollingBuffer);
        }

        currentSize = rollingBufferMap.get(StreamID).currentSize;
        TotalSize = currentSize + length;
        rollingBuffer = rollingBufferMap.get(StreamID).rollingbuffer;
        System.arraycopy(data, 0, rollingBuffer, currentSize, length);

        int FirstIndex = INVALID_INDEX;
        int LastIndex = 0;
        while (LastIndex != INVALID_INDEX) {
            FirstIndex = getStartIndex(rollingBuffer, TotalSize);
            LastIndex = INVALID_INDEX;
            if (FirstIndex != INVALID_INDEX) {
                LastIndex = getLastIndex(rollingBuffer, TotalSize, FirstIndex);
                if (LastIndex != INVALID_INDEX) {
                    String CurrentNMEAMessage = new String(rollingBuffer,
                            FirstIndex, (LastIndex - FirstIndex));

                    //shift the current sentence out of the rolling buffer
                    if (LastIndex <= TotalSize) {
                        System.arraycopy(rollingBuffer, LastIndex,
                                rollingBuffer, 0, TotalSize - LastIndex);
                        rollingBufferMap.get(StreamID).rollingbuffer = rollingBuffer;
                        rollingBufferMap.get(StreamID).currentSize = TotalSize
                                - LastIndex;
                        if (TotalSize - LastIndex < 0) {
                            Log.d(TAG, "problem reading buffer");
                        }
                    }

                    try {
                        ParseNMEAMessage(CurrentNMEAMessage);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    //no end - buffer must not be long enough
                    //update stored buffer for next time around when more data is here
                    if (length - FirstIndex > BUFFER_SIZE * 3 / 4) {
                        //no end found and we're almost at a buffer overdflow - flush the buffer
                        FlushBuffer(StreamID);
                        return false;
                    }
                    System.arraycopy(rollingBuffer, FirstIndex, rollingBuffer,
                            0, length - FirstIndex);
                    rollingBufferMap.get(StreamID).rollingbuffer = rollingBuffer;
                    rollingBufferMap.get(StreamID).currentSize = TotalSize
                            - FirstIndex;
                }
            } else {
                FlushBuffer(StreamID);
                return false;
            }
            TotalSize = rollingBufferMap.get(StreamID).currentSize;
            rollingBuffer = rollingBufferMap.get(StreamID).rollingbuffer;
        }//done looping through all data

        return true;//probably will never hit this for NMEA data
    }

    private void FlushBuffer(int StreamID) {
        rollingBufferMap.get(StreamID).currentSize = 0;
        rollingBufferMap.get(StreamID).rollingbuffer = new byte[BUFFER_SIZE];//LOU proably don't need this
    }

    public void ParseNMEAMessage(String message) throws Exception {

        //Log.d(TAG, "sentence: " + message);
        String Checksum = message.substring(message.indexOf('*') + 1,
                message.indexOf('*') + 3);
        message = message.substring(message.indexOf('$'), message.indexOf('*'));
        String ChecksumSecTest = ChecksumTest.calculate(message);
        //COYNE why is this not equal
        if (!Checksum.equals(ChecksumSecTest)) {
            Log.e(TAG, "Checksum failed for the NMEA string: " + message);
            return;
        } else {
            if (message.contains(PTNL_NMEA_TYPE)) {
                //Log.d(TAG, "PTNL message");
                ParsePTNLMessage(message);
                PTNLMessagePresent = true;
                //Coyne have it so that the points get added to gpsPoints
                if (parserInterface != null)
                    parserInterface.NewOwnshipPoint(CurrentOwnshipPosition.lat,
                            CurrentOwnshipPosition.lon,
                            CurrentOwnshipPosition.getHAE(), EXTERNAL_SOURCE,
                            GPSQualityInt,
                            CurrentOwnshipPosition.circularError,
                            CurrentOwnshipPosition.linearError,
                            CurrentOwnshipPosition.course_true,
                            CurrentOwnshipPosition.speed, "NMEA",
                            CurrentOwnshipPosition.timestamp,
                            message);
            } else if (message.contains(GST_NMEA_TYPE)) {
                ParseGSTMessage(message);
                //Log.d(TAG, "GST message");
            } else if (message.contains(GGA_NMEA_TYPE)) {
                ParseGGAMessage(message);
                if (!PTNLMessagePresent && parserInterface != null) {
                    parserInterface.NewOwnshipPoint(CurrentOwnshipPosition.lat,
                            CurrentOwnshipPosition.lon,
                            CurrentOwnshipPosition.getHAE(), EXTERNAL_SOURCE,
                            GPSQualityInt,
                            CurrentOwnshipPosition.circularError,
                            CurrentOwnshipPosition.linearError,
                            CurrentOwnshipPosition.course_true,
                            CurrentOwnshipPosition.speed, "NMEA2",
                            CurrentOwnshipPosition.timestamp,
                            message);
                }
            } else if (message.contains(GPRMC_NMEA_TYPE)) {
                ParseGPRMC_Message(message);
            }
        }
        return;
    }

    /**
     *    Standard for the GST String from Trimble
     * https://www.trimble.com/OEM_ReceiverHelp/V4.44/en/NMEA-0183messages_GST.html
     *    ----------------------------------------
     * 0     Message ID $GPGST
     * 1     UTC of position fix
     * 2     RMS value of the pseudorange residuals; includes carrier 
     *          phase residuals during periods of RTK (float) and RTK 
     *          (fixed) processing
     * 3     Error ellipse semi-major axis 1 sigma error, in meters
     * 4     Error ellipse semi-minor axis 1 sigma error, in meters
     * 5     Error ellipse orientation, degrees from true north
     * 6     Latitude 1 sigma error, in meters
     * 7     Longitude 1 sigma error, in meters
     * 8     Height 1 sigma error, in meters
     * 9     The checksum data, always begins with *
     */
    public void ParseGSTMessage(String Message) {
        String[] NMEAMessage = Message.split(",");
        if (CurrentOwnshipPosition != null) {
            CurrentOwnshipPosition.linearError = Float
                    .parseFloat(NMEAMessage[4]);
            CurrentOwnshipPosition.circularError = Float
                    .parseFloat(NMEAMessage[3]);
            //System.out.println(Float.parseFloat(NMEAMessage[5]));
        }
    }

    public void ParseGGAMessage(String Message) throws Exception {
        //COYNE find out if there is a better way to do last point and keep error
        String[] NMEAMessage = Message.split(",");
        if (CurrentOwnshipPosition != null) {
            String LatitudeDirection = NMEAMessage[3];
            CurrentOwnshipPosition.lat = Latitude2Decimal(NMEAMessage[2],
                    LatitudeDirection);
            String LongitudeDirection = NMEAMessage[5];
            CurrentOwnshipPosition.lon = Longitude2Decimal(NMEAMessage[4],
                    LongitudeDirection);
            if (NMEAMessage.length > 15) {
                Log.d(TAG, "Trimble GGA detected quality: " + NMEAMessage[6]);
                GPSQualityInt = (int) Double.parseDouble(NMEAMessage[6]);
            } else {
                GPSQualityInt = (int) Double.parseDouble(NMEAMessage[4]);
            }
        }
    }

    /**
     *    Standard for the PTNL String from Trimble
     *https://www.trimble.com/OEM_ReceiverHelp/V4.44/en/NMEA-0183messages_PTNL_GGK.html
     *    -----------------------------------------
     *    0     Talker ID $PTNL
     *    1     Message ID GGK
     *    2     UTC time of position fix, in hhmmmss.ss format. Hours must be 
     *              two numbers, so may be padded. For example, 7 is shown as 07.
     *    3     UTC date of position fix, in ddmmyy format. Day must be two 
     *              numbers, so may be padded. For example, 8 is shown as 08.
     *    4     Latitude, in degrees and decimal minutes (dddmm.mmmmmmm)
     *    5     Direction of latitude:
     *       N: North
     *       S: South
     *    6     Longitude, in degrees and decimal minutes (dddmm.mmmmmmm). 
     *              Should contain three digits of ddd.
     *    7     Direction of longitude:
     *    E: East
     *    W: West
     *    8     GPS Quality indicator:
     *       0: Fix not available or invalid
     *       1: Autonomous GPS fix
     *       2: RTK float solution
     *       3: RTK fix solution
     *       4: Differential, code phase only solution (DGPS)
     *       5: SBAS solution – WAAS/EGNOS/MSAS
     *       6: RTK float or RTK location 3D Network solution
     *       7: RTK fixed 3D Network solution
     *       8: RTK float or RTK location 2D in a Network solution
     *       9: RTK fixed 2D Network solution
     *       10: OmniSTAR HP/XP solution
     *       11: OmniSTAR VBS solution
     *       12: Location RTK solution
     *       13: Beacon DGPS
     *    9     Number of satellites in fix
     *    10     Dilution of Precision of fix (DOP)
     *    11     Ellipsoidal height of fix (antenna height above 
     *              ellipsoid). Must start with EHT.
     *    12     M: ellipsoidal height is measured in meters
     *    13     The checksum data, always begins with *
     * Note – The PTNL,GGK message is longer than the NMEA-0183 standard of 
     * 80 characters.
     * Note – Even if a user-defined geoid model, or an inclined plane is loaded 
     * into the receiver, then the height output in the NMEA GGK string is always
     * an ellipsoid height, for example, EHT24.123.
     */

    public void ParsePTNLMessage(String Message) throws Exception {
        String[] NMEAMessage = Message.split(",");
        //COYNE find out if there is a better way to do last point and keep error
        String LatitudeDirection = NMEAMessage[5];
        CurrentOwnshipPosition.lat = Latitude2Decimal(NMEAMessage[4],
                LatitudeDirection);
        String LongitudeDirection = NMEAMessage[7];
        CurrentOwnshipPosition.lon = Longitude2Decimal(NMEAMessage[6],
                LongitudeDirection);
        GPSQualityInt = (int) Double.parseDouble(NMEAMessage[8]);

        CurrentOwnshipPosition.setHAE(Float.parseFloat(NMEAMessage[11]
                .replaceFirst("EHT", "")));

        Log.d(TAG, "parsing date: " + Message);
        try {
            Calendar c = new GregorianCalendar(new SimpleTimeZone(0, "GMT"),
                    Locale.ENGLISH);
            c.setTime(makeDate(NMEAMessage[2]));
            c.set(Calendar.YEAR,
                    Integer.parseInt(NMEAMessage[3].substring(4)) + 2000);
            c.set(Calendar.DAY_OF_MONTH,
                    Integer.parseInt(NMEAMessage[3].substring(2, 4)));
            c.set(Calendar.MONTH,
                    Integer.parseInt(NMEAMessage[3].substring(0, 2)) - 1);
            CurrentOwnshipPosition.timestamp = c.getTimeInMillis();
        } catch (Exception e) {
            Log.d(TAG, "problem parsing date: ", e);
        }
    }

    public void ParseGPRMC_Message(final String message) {
        //Log.d(TAG, "parsing GPRMC message: " + message);

        String[] NMEAMessage = message.split(",");
        if (NMEAMessage[2].equals("A")) {
            String LatitudeDirection = NMEAMessage[4];
            CurrentOwnshipPosition.lat = Latitude2Decimal(NMEAMessage[3],
                    LatitudeDirection);
            String LongitudeDirection = NMEAMessage[6];
            CurrentOwnshipPosition.lon = Longitude2Decimal(NMEAMessage[5],
                    LongitudeDirection);
            CurrentOwnshipPosition.speed = Float.parseFloat(NMEAMessage[7]);
            CurrentOwnshipPosition.course_true = Float
                    .parseFloat(NMEAMessage[8]);
        }
        //Log.d(TAG, "parsing date: " + message);
        try {
            Calendar c = new GregorianCalendar(new SimpleTimeZone(0, "GMT"),
                    Locale.ENGLISH);
            c.setTime(makeDate(NMEAMessage[1]));
            c.set(Calendar.YEAR,
                    Integer.parseInt(NMEAMessage[9].substring(4)) + 2000);
            c.set(Calendar.MONTH,
                    Integer.parseInt(NMEAMessage[9].substring(2, 4)) - 1);
            c.set(Calendar.DAY_OF_MONTH,
                    Integer.parseInt(NMEAMessage[9].substring(0, 2)));
            CurrentOwnshipPosition.timestamp = c.getTimeInMillis();
        } catch (Exception e) {
            Log.d(TAG, "problem parsing date: ", e);
        }

    }

    /**
     * Turn the 2nd fields into the time.
     */
    private Date makeDate(final String s) {

        double d = Double.parseDouble(s);
        int i = (int) d;
        Calendar c = new GregorianCalendar(new SimpleTimeZone(0, "GMT"),
                Locale.ENGLISH);
        c.setTime(new Date(0));
        c.set(Calendar.HOUR_OF_DAY, i / 10000);
        c.set(Calendar.MINUTE, (i % 10000) / 100);
        c.set(Calendar.SECOND, i % 100);
        c.set(Calendar.MILLISECOND, (int) ((d - i) * 1000));
        return c.getTime();
    }

    public int getStartIndex(byte[] inBuffer, int Length) {
        for (int i = 0; i < Length; i++) {
            int d = inBuffer[i];
            if (d == '$')
                return i;
        }
        return INVALID_INDEX;
    }

    public int getLastIndex(byte[] inBuffer, int Length, int offset) {
        for (int i = offset; i < Length; i++) {
            int d = inBuffer[i];
            if (d == '*') {
                if ((Length - i) > 2) {
                    return i + 3;
                }
            }
        }
        return INVALID_INDEX;
    }

    public boolean CheckBaudRate() {
        //Check BaudRate in Obstruction service then send to correct parser
        //for serial only
        return true;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    String GetParserName() {
        return "NMEA_Parser";
    }

    public static class RollingBufferItems {
        byte[] rollingbuffer = new byte[BUFFER_SIZE];
        int currentSize = 0;

        RollingBufferItems(int currentSize, byte[] rollingbuffer) {
            this.currentSize = currentSize;
            this.rollingbuffer = rollingbuffer;
        }

        RollingBufferItems() {
            this.currentSize = 0;
            this.rollingbuffer = new byte[BUFFER_SIZE];
        }
    }

}
