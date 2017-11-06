
package com.atakmap.android.commout;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.PointMapItem;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.AbstractMapComponent;
import com.atakmap.android.user.PlacePointTool;
import com.atakmap.coremap.log.Log;
import com.atakmap.comms.SocketFactory;
import com.atakmap.comms.NetworkUtils;

import com.atakmap.android.maps.MapEvent;
import com.atakmap.android.maps.MapEventDispatcher;
import com.atakmap.android.maps.MapEventDispatcher.MapEventDispatchListener;
import com.atakmap.coremap.maps.coords.GeoPoint;

import java.util.Timer;
import java.util.TimerTask;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.util.Properties;
import java.util.Enumeration;
import java.util.Arrays;

/**
 *  This is an example program that intercepts all things being sent out from ATAK for
 *  for conversion purposes.  This also shows how one would take and monitor an entity in
 *  the MapView for generating a periodic message.
 */
public class CommoutMapComponent extends AbstractMapComponent {

    public static final String TAG = "CommoutMapComponent";

    private final int RATE = 3000;
    private final int receiveTimeout = RATE * 3;

    private final int retryTimeout = 1000;
    private final int ttl = 12;

    public Context pluginContext;
    public MapView _mapView;

    private String outputAddress = "233.1.1.1";
    private int outputPort = 8900;

    Timer timer;

    /**
     * In this example,
     * Set up the dispatch listeners for actions matching ITEM_SHARED and ITEM_PERSIST.  Both are
     * used within ATAK to designate when things are sent.   ITEM_SHARED is used verbatim while
     * ITEM_PERSIST is used only when the internal flag is set to false.
     * In addition, this example also sets of a timer which yacks out the PPLI or Ownship position
     * over the network.
     */
    @Override
    public void onCreate(final Context context, Intent intent, final MapView view) {
        _mapView = view;
        pluginContext = context;
        
        timer = new Timer("externalOwnship");
        timer.schedule(sendOwnship, 0, RATE);

        MapEventDispatcher dispatcher = _mapView.getMapEventDispatcher();
        dispatcher.addMapEventListener(MapEvent.ITEM_SHARED, sedl);
        dispatcher.addMapEventListener(MapEvent.ITEM_PERSIST, sedl);
        new Thread(new UDPListener()).start();
    }

    /**
     * Users cannot directly override onDestroy because there are specific bookkeeping internals that
     * onDestroy does.   Instead, use onDestroyImpl which is called after the bookkeeping is completed.
     * @param context
     * @param view
     */
    @Override
    protected void onDestroyImpl(Context context, MapView view) {
        if (timer != null) {
            timer.cancel();
            timer.purge();
            timer = null;
        }
    }

    /**
     * For the purposes of this example, onStart is not needed.
     * @param context
     * @param view
     */
    @Override
    public void onStart(Context context, MapView view) {
    }

    /**
     * For the purposes of this example, onStop is not needed.
     * @param context
     * @param view
     */
    @Override
    public void onStop(Context context, MapView view) {
    }

    /**
     * For the purposes of this example, onStart is not needed.
     * @param context
     * @param view
     */
    @Override
    public void onPause(Context context, MapView view) {
    }

    /**
     * For the purposes of this example, onResume is not needed.
     * @param context
     * @param view
     */
    @Override
    public void onResume(Context context, MapView view) {
    }


    /**
     * The task that is run periodically that will check the self marker and construct a message
     * for distribution out.
     */
    private final TimerTask sendOwnship = new TimerTask() {
        @Override
        public void run() {
            final Marker m = _mapView.getSelfMarker();

            if (m != null) {
                Log.d(TAG, "sending m: " + m.getUID());
                sendMessage(fromMapItem(m));
            }
        }
    };


    /**
     * Another device with this plugin would be responsible for listening for the messages.   This
     * is a sample loop that is listening for traffic, checking to see that it is from and external
     * device and processing the message.
     */
    public class UDPListener implements Runnable {

        private boolean cancelled = false;

        public void cancel() {
             cancelled = true;
        }

        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public void run() {
            Log.d(TAG, "starting the run");
            DatagramSocket socket = null;
            InetAddress address = null;
            SocketAddress sockAddr = null;


            try {
                // create socket and set properties
                address = InetAddress.getByName(outputAddress);
            } catch (UnknownHostException e1) {
                // unknown host error
                return;
            }
            final byte[] message = new byte[8 * 1024];
            final DatagramPacket p = new DatagramPacket(message, message.length);

            // run
            while(!cancelled) {
                try {
                    if (socket == null) {
                        socket = SocketFactory.getSocketFactory().createMulticastSocket(outputPort);
                        socket.setSoTimeout(receiveTimeout);

                        if (address.isMulticastAddress()) {
                            ((MulticastSocket)socket).joinGroup(address);
                        }
                    }

                    // receive packet
                    socket.receive(p);
                    // dumbest way to filter, remember this is a simple example
                    if (!p.getAddress().toString().contains(NetworkUtils.getIP())) {
                        byte[] b = p.getData();
                        String s = new String(b);
                        s = s.substring(0, p.getLength());
                        toMapItem(s.getBytes());
                        Log.d(TAG, "received: " + s);
                    } else { 
                        //byte[] b = p.getData();
                        //String s = new String(b);
                        //s = s.substring(0, p.getLength());
                        //Log.d(TAG, "received loopback: " + s);
                    }

                } catch (InterruptedIOException toe) {
                    Log.d(TAG, "interrupted exception occured: " + toe);

                    // timeout
                    socket.close();
                    socket = null;
                } catch (IOException e1) {
                    Log.d(TAG, "ioexception occured for udp receieve: " + e1);

                    e1.printStackTrace();
                    // receive error
                    try { Thread.sleep(retryTimeout); } catch (Exception e) { }
                    socket.close();
                    socket = null;
                } catch (Exception e2) {
                    Log.d(TAG, "exception occured for udp receive: " + e2);
                    e2.printStackTrace();

                    // general error
                    try { Thread.sleep(retryTimeout); } catch (Exception e) { }
                    socket.close();
                    socket = null;
                }
            }

            // clean up
            if (socket != null) {
                  try { socket.close(); } catch (Exception e) { }
            }
        }      
    }

    /**
     * Sends a message encoded as a byte[] over the wire
     * @param data the byte array to be sent.
     */
    private void sendMessage(final byte[] data) {
         Thread t = new Thread(new Runnable() {
                public void run() {
                    send(data);
                }
         });
         t.start();

    }


    /**
     * Implementation of a message sending construct.   This effectively squirts a byte array to the
     * network constrained by the maximal length of a UDP packet which on test networks can be as
     * high as 64kb.
     * @param data
     */
    private void send(final byte[] data) {

         MulticastSocket socket = null;
         DatagramPacket packet;

         InetAddress local = null;
         try {
             local = InetAddress.getByName(outputAddress);
         } catch (UnknownHostException e) {
             e.printStackTrace();
         }

         try {
             socket = SocketFactory.getSocketFactory().createMulticastSocket(outputPort); // status
             socket.setTimeToLive(ttl);
         } catch (IOException e) {
             e.printStackTrace();
         }

         if (socket != null) {
             packet = new DatagramPacket (data, data.length, local, outputPort); // command

             try {

                 socket.send(packet);
                 Log.d(TAG, "sending " + data + " to " + outputAddress + ":" + outputPort);
             } catch (IOException e) {
                e.printStackTrace();
             }
         }
         try {
             socket.close();
         } catch (Exception e) { }

     }

    /**
     * Implementation of a MapListener that will be called whenever a ITEM_PERSIST or ITEM_SHARED
     * is called within the system.
     */
    private final MapEventDispatcher.MapEventDispatchListener sedl = new MapEventDispatcher.MapEventDispatchListener() {
        @Override
        public void onMapEvent(final MapEvent event) {
            MapItem target = event.getItem();
            if (target == null)
                return;
            if (event.getType().equals(MapEvent.ITEM_PERSIST)) {
                Bundle b = event.getExtras();
                if (b != null && !b.getBoolean("internal")) { 
                    Log.d(TAG, "send via persist: " + target.getUID());
                    if (target instanceof PointMapItem) {
                        sendMessage(fromMapItem((PointMapItem)target));
                    } else {
                        Log.d(TAG, "example not yet currently supported");
                    }
                }
            } else if (event.getType().equals(MapEvent.ITEM_SHARED)) {
                Log.d(TAG, "shared: " + target.getUID());
                if (target instanceof PointMapItem) {
                    sendMessage(fromMapItem((PointMapItem)target));
                } else {
                    Log.d(TAG, "example not yet currently supported");
                }
            }
        }
    };


    /**
     * These two methods describe a protocol called SHB16, which serializes data within 
     * markers and allows for them to safely pass from one device to another.
     */

    private void toMapItem(byte[] data) {
        // very crude example
        String s = new String(data);
        String[] sarray = s.split("\\|");
        Log.d(TAG, "received: " + Arrays.toString(sarray));
        Log.d(TAG, "attempting to find: " + sarray[1]);
        MapItem mi = _mapView.getMapItem(sarray[1]);
        if (mi == null) {
            Log.d(TAG, "map item does not exist, creating: " + sarray[1]);
            PlacePointTool.MarkerCreator mc = new PlacePointTool.MarkerCreator(GeoPoint.parseGeoPoint(sarray[2]));
            mc.setUid(sarray[1]);
            mc.setCallsign(sarray[3]);
            mc.setType(sarray[0]);
            mc.showCotDetails(false);
            mc.setNeverPersist(true);
            Marker m = mc.placePoint();
            
        }

        if (mi instanceof PointMapItem) {
            PointMapItem pmi = (PointMapItem) mi;
            pmi.setPoint(GeoPoint.parseGeoPoint(sarray[2]));
            pmi.setMetaString("callsign", sarray[3]);

        }
    }

    private byte[] fromMapItem(PointMapItem mi) {
        String type = mi.getType();
        if (type.equalsIgnoreCase("self")) 
           type = "a-f-G";
     
        String retVal = type + "|" + mi.getUID() + "|" + mi.getPoint() + "|" + mi.getMetaString("callsign", "nonset");
        return retVal.getBytes();
    }




}
