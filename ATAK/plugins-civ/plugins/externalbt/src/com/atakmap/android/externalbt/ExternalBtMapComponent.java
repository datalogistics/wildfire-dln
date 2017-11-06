
package com.atakmap.android.externalbt;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.coremap.maps.coords.Altitude;
import com.atakmap.coremap.maps.coords.AltitudeSource;
import com.atakmap.coremap.maps.coords.AltitudeReference;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.dropdown.DropDownMapComponent;
import com.atakmap.coremap.log.Log;
import android.content.Intent;

import java.util.Date;
import com.atakmap.coremap.conversions.ConversionFactors;

import com.atakmap.android.bluetooth.BluetoothConnection;
import com.atakmap.android.bluetooth.BluetoothManager;
import com.atakmap.android.bluetooth.BluetoothManager.BluetoothReaderFactory;
import com.atakmap.android.bluetooth.BluetoothReader;
import com.atakmap.android.bluetooth.BluetoothCotManager;
import com.atakmap.android.bluetooth.BluetoothASCIIClientConnection;

import com.atakmap.android.lrf.LocalRangeFinderInput;

import gnu.nmea.Packet;
import gnu.nmea.PacketGGA;
import gnu.nmea.PacketRMC;
import gnu.nmea.SentenceHandler;
import gnu.nmea.Geocoordinate;

import android.os.SystemClock;
import com.atakmap.coremap.maps.time.CoordinatedTime;
import com.atakmap.android.ipc.AtakBroadcast;
import android.os.Bundle;



public class ExternalBtMapComponent extends DropDownMapComponent {

    public static final String TAG = "ExternalBtMapComponent";

    public Context pluginContext;
    public MapView view;

    private PacketGGA _gga;
    private PacketRMC _rmc;

    public void onCreate(final Context context, Intent intent, final MapView view) {
        if (pluginContext == null) { 
            Log.d(TAG, "externalbt loaded");
            super.onCreate(context, intent, view);
            pluginContext = context;
            this.view = view;
            BluetoothManager.getInstance().addExternalBluetoothReader(new ExternalBtFactory());
        }

    }

    @Override
    protected void onDestroyImpl(Context context, MapView view) {
        super.onDestroyImpl(context, view);
    }


    /**
     * Factory implementation for creating readers for a specific device.
     */
    private class ExternalBtFactory implements BluetoothReaderFactory { 
        public boolean matches(BluetoothDevice device) { 
            Log.d(TAG, "searching for external bluetooth device for: " + device);
            if (device.getName().startsWith("TP360")) { 
                 return true;
            }
            return false;
        } 
        public BluetoothReader create(BluetoothDevice device) { 
            return new ExternalBluetoothReader(device);
        }
    } 


    /**
     * Implementation of the reader.   Meat and Potatoes of the implementation.
     */
    private class ExternalBluetoothReader extends BluetoothReader {
        public ExternalBluetoothReader(BluetoothDevice device) {
            super(device);
        }
        @Override
        public void onRead(final byte[] data) {
            String ascii = new String(data);
            Log.d(TAG, "just print out the line: " + ascii);
            // parse packet
            Packet packet = null;
            try { 
                packet = SentenceHandler.makePacket(ascii, false);
            } catch (Exception e) {
                // sentence handler throws an exception if it does not like the string
            }
            if (packet == null) {
               fireLRFUpdate(ascii);
            } else { 
                if (packet instanceof PacketGGA) {
                    _gga = (PacketGGA) packet;
                } else if (packet instanceof PacketRMC) {
                    _rmc = (PacketRMC) packet;
                    // send an update each time we get an RMC
                    if (_rmc != null) 
                        fireGPSUpdate(_rmc, _gga);
                }


            }
            




        }

        /**
         * Reuse existing classes.
         */
        @Override
        protected BluetoothConnection onInstantiateConnection(BluetoothDevice device) {
            return new BluetoothASCIIClientConnection(device,
                BluetoothConnection.MY_UUID_INSECURE);
        }

        /**
         * Reuse existing classes.
         */
        @Override
        public BluetoothCotManager getCotManager(MapView mapView) {
            BluetoothDevice device = connection.getDevice();
            return new BluetoothCotManager(this, mapView, 
                     device.getName().replace(" ", "").trim() + "." + device.getAddress(), device.getName());
        }
    }

    /** 
     *  Uses NMEA RMC: location, time, course, speed
     *  Uses NMEA GGA: altitude
     */

    private void fireGPSUpdate(final PacketRMC rmc, final PacketGGA gga) { 
        Marker item = view.getSelfMarker();
        if (item != null) {

            //pull location, time, course, speed from RMC
            Geocoordinate pos = rmc.getPosition();
            Date time = rmc.getDate();
            //pull altitude from GGA
            Altitude msl = (gga != null) ? new Altitude(
                    gga.getAltitude(), AltitudeReference.MSL, AltitudeSource.GPS)
                    : Altitude.UNKNOWN;
    
            final GeoPoint gp = new GeoPoint(pos.getLatitudeDegrees(),
                    pos.getLongitudeDegrees(), msl,
                    GeoPoint.CE90_UNKNOWN, GeoPoint.LE90_UNKNOWN);

            final float dilution = (float)gga.getDilution();
            final float trackAngle = (float)rmc.getTrackAngle();
            final int fixQuality = gga.getFixQuality();


            final Bundle data = view.getMapData();

            data.putDouble("mockLocationSpeed", (rmc.getGroundSpeed() / 1.85200));   // speed in meters per second
            data.putFloat("mockLocationAccuracy", dilution); // accuracy in meters
            data.putFloat("mockLocationBearing", trackAngle); 

            data.putString("locationSourcePrefix", "mock");
            data.putBoolean("mockLocationAvailable", true);

            data.putString("mockLocationSource", "Plugin Supplied GPS");
            data.putString("mockLocationSourceColor", "#FFAFFF00");
            data.putBoolean("mockLocationCallsignValid", true);

            data.putParcelable("mockLocation", gp);

            data.putLong("mockLocationTime", SystemClock.elapsedRealtime());

            data.putLong("mockGPSTime", new CoordinatedTime().getMilliseconds());  // time as reported by the gps device

            data.putInt("mockFixQuality", fixQuality);

            Intent gpsReceived = new Intent();

            gpsReceived
                    .setAction("com.atakmap.android.map.WR_GPS_RECEIVED");
            AtakBroadcast.getInstance().sendBroadcast(gpsReceived);

            Log.d(TAG,
                    "received gps for: " + gp
                            + " with a fix quality: " + 2 +
                            " setting last seen time: "
                            + data.getLong("mockLocationTime"));

        }

    }

    private void fireLRFUpdate(String line) { 
        Log.d(TAG, "processed line: " + line);

        if (!line.startsWith("$PLTIT,HV")) {
            return;
        }

        String[] data = line.split(",");
        String distanceString = data[2];
        String distanceUnits = data[3];
        String azimuthString = data[4];
        String inclinationString = data[6];

        double distance = getDistance(distanceString, distanceUnits);
        double azimuth = getAngle(azimuthString);
        double inclination = getAngle(inclinationString);

        try {
            Log.d(TAG, "received: " + line + "   values are  d: "
                    + distance + " " + "a: " + azimuth + "i: " + inclination);

            if (!Double.isNaN(distance) && !Double.isNaN(azimuth)
                    && !Double.isNaN(inclination)) {
                LocalRangeFinderInput.getInstance().onRangeFinderInfo("externalbt-lrf.55", 
                                                                      distance, azimuth, inclination);
            } else {
                Log.d(TAG, "error reading line: " + line
                        + "   values are  d: " + distance + " " + "a: "
                        + azimuth + "i: " + inclination);
            }
        } catch (Exception e) {
            Log.d(TAG, "error reading line: " + line, e);
        }
    }

    private double getDistance(String valString, String units) {
        try {
            double val = Double.parseDouble(valString);
            if (units.equals("F")) {
                val /= ConversionFactors.METERS_TO_FEET;
            } else if (units.equals("Y")) {
                val /= ConversionFactors.METERS_TO_YARDS;
            }
            return val;
        } catch (Exception e) {
            return Double.NaN;
        }

    }

    private double getAngle(String valString) {
        try {
            return Double.parseDouble(valString);
        } catch (Exception e) {
            return Double.NaN;
        }
    }

}
