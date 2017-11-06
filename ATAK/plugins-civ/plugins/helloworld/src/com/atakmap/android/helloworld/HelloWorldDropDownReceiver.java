
package com.atakmap.android.helloworld;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.app.AlertDialog;
import android.widget.ArrayAdapter;
import android.widget.Button;
import com.atakmap.comms.CotServiceRemote;
import com.atakmap.comms.CotStreamListener;
import com.atakmap.comms.app.CotPortListActivity;;
import com.atakmap.android.chat.ChatManagerMapComponent;
import com.atakmap.android.contact.PluginConnector;
import com.atakmap.android.coordoverlay.CoordOverlayMapReceiver;
import com.atakmap.android.cot.CotMapComponent;
import com.atakmap.android.user.PlacePointTool;

import com.atakmap.android.importfiles.sort.ImportMissionPackageSort;
import android.os.Bundle;
import android.os.Environment;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;

import android.widget.Toast;

import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.javacodegeeks.android.contentprovidertest.BirthProvider;
import android.net.Uri;
import android.content.ContentValues;

import com.atakmap.comms.NetConnectString;
import com.atakmap.android.contact.Connector;
import com.atakmap.android.contact.IpConnector;
import com.atakmap.android.contact.Contacts;
import com.atakmap.android.contact.Contact;
import com.atakmap.android.contact.IndividualContact;
import java.util.List;

import android.os.SystemClock;
import com.atakmap.coremap.maps.time.CoordinatedTime;
import com.atakmap.android.ipc.AtakBroadcast;
import android.os.Bundle;
import com.atakmap.android.util.Circle;
import com.atakmap.android.maps.Ellipse;
import com.atakmap.android.maps.MapEventDispatcher;

import com.atakmap.android.emergency.tool.EmergencyManager;
import com.atakmap.android.emergency.tool.EmergencyType;
import com.atakmap.android.maps.MapActivity;
import com.atakmap.android.maps.MapComponent;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapEvent;
import com.atakmap.android.maps.PointMapItem;
import com.atakmap.android.maps.PointMapItem.OnPointChangedListener;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.dropdown.DropDown.OnStateListener;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.helloworld.plugin.R;
import com.atakmap.coremap.maps.coords.Altitude;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.coremap.maps.coords.GeoPointSource;
import android.view.View.OnClickListener;

import android.content.ComponentName;

import android.content.DialogInterface;

import android.graphics.Color;

import android.util.Base64;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;

import com.atakmap.android.routes.Route;
import com.atakmap.android.routes.RouteMapComponent;
import com.atakmap.android.routes.RouteMapReceiver;

import com.atakmap.map.elevation.ElevationManager;
import com.atakmap.map.elevation.ElevationData;


import com.atakmap.coremap.log.Log;

import java.util.UUID;

/** 
 * The DropDown Receiver should define the visual experience 
 * that a user might have while using this plugin.   At a 
 * basic level, the dropdown can be a view of your own design 
 * that is inflated.   Please be wary of the type of context 
 * you use.   As noted in the Map Component, there are two 
 * contexts - the plugin context and the atak context.   
 * When using the plugin context - you cannot build thing or 
 * post things to the ui thread.   You use the plugin context
 * to lookup resources contained specifically in the plugin.
 */
public class HelloWorldDropDownReceiver extends DropDownReceiver implements
        OnStateListener {

    public static final String TAG = "HelloWorldDropDownReceiver";

    public static final String SHOW_HELLO_WORLD = "com.atakmap.android.helloworld.SHOW_HELLO_WORLD";
    public static final String CHAT_HELLO_WORLD = "com.atakmap.android.helloworld.CHAT_HELLO_WORLD";
    public static final String SEND_HELLO_WORLD = "com.atakmap.android.helloworld.SEND_HELLO_WORLD";
    private final View helloView;
    private final Context pluginContext;
    private final Contact helloContact;

    private Route r;

    private CotServiceRemote csr;
    private boolean connected = false;
  
    CotServiceRemote.ConnectionListener cl = new CotServiceRemote.ConnectionListener() { 
        @Override
        public void onCotServiceConnected(Bundle fullServiceState) {
            Log.d(TAG, "onCotServiceConnected: ");
            connected = true;
        }

        @Override
        public void onCotServiceDisconnected() {
            Log.d(TAG, "onCotServiceDisconnected: ");
            connected = false;
        }

    };

    CotStreamListener csl;


    /**************************** CONSTRUCTOR *****************************/

    public HelloWorldDropDownReceiver(final MapView mapView,
            final Context context) {
        super(mapView);
        this.pluginContext = context;
        csr = new CotServiceRemote(); 
        csr.connect(cl);

        csl = new CotStreamListener(mapView.getContext(), TAG, null) { 
            @Override
            public void onCotOutputRemoved(Bundle bundle) {
                Log.d(TAG, "stream outputremoved");
            }
        
            @Override
            protected void enabled(CotPortListActivity.CotPort port,
                    boolean enabled) {
                Log.d(TAG, "stream enabled");
            }
        
            @Override
            protected void connected(CotPortListActivity.CotPort port,
                    boolean connected) {
                Log.d(TAG, "stream connected");
            }

           @Override
           public void onCotOutputUpdated(Bundle descBundle) {
                Log.d(TAG, "stream added/updated");
           }

        };
        

        // Note the inflater is using the plugin context.
        LayoutInflater inflater =
                (LayoutInflater) context
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);


        helloView = inflater.inflate(R.layout.hello_world_layout, null);
        // Add "Hello World" contact
        this.helloContact = addPluginContact(pluginContext.getString(
                R.string.hello_world));


        // The button bellow shows how one might go about 
        // programatically changing the size of the drop down.
        final Button smaller = (Button) helloView
                .findViewById(R.id.smallerButton);
        smaller.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                resize(THIRD_WIDTH, FULL_HEIGHT);
            }
        });


        // The button bellow shows how one might go about 
        // programatically changing the size of the drop down.
        final Button larger = (Button) helloView
                .findViewById(R.id.largerButton);
        larger.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                resize(FULL_WIDTH, HALF_HEIGHT);
            }
        });

        // The button bellow shows how one might go about 
        // programatically flying through a list of points.
        // In this case they are synthetically generated.  
        // They could just as easily be points on a route, etc.
        final Button fly = (Button) helloView.findViewById(R.id.fly);
        fly.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    public void run() {
                        getMapView().getMapController().zoomTo(.00001d, false);
                        for (int i = 0; i < 20; ++i) {
                            getMapView().getMapController().panTo(
                                    new GeoPoint(42, -79 - (double) i / 100),
                                    false);
                            try {
                                Thread.sleep(1000);
                            } catch (Exception e) {
                            }
                        }
                    }
                }).start();
            }
        });


        // The button bellow shows how one might go about 
        // overriding the button of a specific marker.   
        // The ATAK core does not allow someone to override 
        // all markers of a specific type with a new menu.  
        // This can be done by a combination of searching 
        // items on a map and replacing the menu on start,
        // and then listening for ITEM_ADDED for each 
        // additional placement of a new item.
        final Button wheel = (Button) helloView
                .findViewById(R.id.specialWheelMarker);
        wheel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                createUnit();
                
            }
        });


        
        // The button bellow shows how one might go about 
        // programmatically listing all routes on the map.
        final Button listRoutes = (Button) helloView
                .findViewById(R.id.listRoutes);
        listRoutes.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                RouteMapReceiver routeMapReceiver = getRouteMapReceiver();
                if (routeMapReceiver == null)
                    return;

                AlertDialog.Builder builderSingle = new AlertDialog.Builder(
                        mapView.getContext());
                builderSingle.setTitle("Select a Route");
                final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
                        pluginContext,
                        android.R.layout.select_dialog_singlechoice);

                for (Route route : routeMapReceiver.getCompleteRoutes()) {
                    arrayAdapter.add(route.getTitle());
                }
                builderSingle.setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                dialog.dismiss();
                            }
                        });
                builderSingle.setAdapter(arrayAdapter, null);
                builderSingle.show();
            }

        });

        // The button bellow shows how one might go about 
        // setting up a custom map widget.
        final Button showSearchIcon = (Button) helloView
                .findViewById(R.id.showSeachIcon);
        showSearchIcon.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "sending broadcast SHOW_MY_WACKY_SEARCH");
                Intent intent = new Intent("SHOW_MY_WACKY_SEARCH");
                com.atakmap.android.ipc.AtakBroadcast.getInstance()
                        .sendBroadcast(intent);

            }
        });

       
        // The button bellow shows how one might go about 
        // programatically add a route to the system. Adding
        // an array of points will be much faster than adding
        // them one at a time.
        final Button addRoute = (Button) helloView.findViewById(R.id.addXRoute);
        addRoute.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "creating a quick route");
                GeoPoint sp = getMapView().getPointWithElevation();
                r = new Route(getMapView(),
                        "My Route",
                        Color.WHITE, "CP",
                        UUID.randomUUID().toString());

                Marker m[] = new Marker[5];
                for (int i = 0; i < 5; ++i) {
                    GeoPoint x = new GeoPoint(sp.getLatitude() + (i * .0001),
                            sp.getLongitude(),
                            Altitude.UNKNOWN,
                            GeoPoint.CE90_UNKNOWN, GeoPoint.LE90_UNKNOWN,
                            GeoPointSource.UNKNOWN);

                    // the first call will trigger a refresh each time across all of the route points
                    //r.addMarker(Route.createWayPoint(x, UUID.randomUUID().toString())); 
                    m[i] = Route
                            .createWayPoint(x, UUID.randomUUID().toString());
                }
                r.addMarkers(0, m);

                MapGroup _mapGroup = getMapView().getRootGroup()
                        .findMapGroup("Route");
                _mapGroup.addItem(r);

                r.persist(getMapView().getMapEventDispatcher(), null,
                        this.getClass());
                Log.d(TAG, "route created");

            }
        });

        final Button reRoute = (Button) helloView.findViewById(R.id.reXRoute);
        reRoute.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (r == null) {
                    toast("No Route added during this run");
                    return;
                }

                GeoPoint sp = getMapView().getPointWithElevation();
                PointMapItem m[] = new PointMapItem[16];
                for (int i = 1; i < m.length; ++i) {
                    if (i % 2 == 0) {
                        GeoPoint x = new GeoPoint(sp.getLatitude()
                                - (i * .0001),
                                sp.getLongitude() + (i * .0001),
                                Altitude.UNKNOWN,
                                GeoPoint.CE90_UNKNOWN, GeoPoint.LE90_UNKNOWN,
                                GeoPointSource.UNKNOWN);

                        // the first call will trigger a refresh each time across all of the route points
                        //r.addMarker(2, Route.createWayPoint(x, UUID.randomUUID().toString()));
                        m[i - 1] = Route.createWayPoint(x, UUID.randomUUID()
                                .toString());
                    } else {
                        GeoPoint x = new GeoPoint(sp.getLatitude()
                                + (i * .0002),
                                sp.getLongitude() + (i * .0002),
                                Altitude.UNKNOWN,
                                GeoPoint.CE90_UNKNOWN, GeoPoint.LE90_UNKNOWN,
                                GeoPointSource.UNKNOWN);
                        m[i - 1] = Route.createControlPoint(x, UUID
                                .randomUUID().toString());
                    }
                }
                r.addMarkers(2, m);
            }
        });

        final Button emergency = (Button) helloView
                .findViewById(R.id.emergency);
        emergency.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                EmergencyManager.getInstance().initiateRepeat(
                        EmergencyType.NineOneOne, false);
            }
        });

        final Button noemergency = (Button) helloView
                .findViewById(R.id.no_emergency);
        noemergency.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                EmergencyManager.getInstance().cancelRepeat(
                        EmergencyType.NineOneOne, false);
            }
        });

        final Button rbCircle = (Button) helloView.findViewById(R.id.rbcircle);
        rbCircle.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                MapItem mi = getMapView().getMapItem("detect-ae:3e:ee");
                if (mi == null) {

                    PlacePointTool.MarkerCreator mc = new PlacePointTool.MarkerCreator(
                            new GeoPoint(32, -72));
                    mc.setUid("detect-ae:3e:ee");
                    mc.setCallsign("detect 1");
                    mc.setType("a-h-G");
                    mc.showCotDetails(false);
                    mc.setNeverPersist(true);
                    Marker m = mc.placePoint();

                    //createEllipse(m);
                    createCircle(m);

                } else {
                    toast("marker already placed");
                }
            }

        });

        final Button externalGps = (Button) helloView.findViewById(R.id.externalGps);
        externalGps.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Thread t = new Thread() { 
                    public void run() { 
                        runSim();
                    } 
                };
                t.start();
            }
        });

        final Button staleout = (Button) helloView
                .findViewById(R.id.staleoutMarker);
        staleout.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                PlacePointTool.MarkerCreator mc =
                    new PlacePointTool.MarkerCreator(getMapView().getCenterPoint());

                mc.showCotDetails(false);
                mc.setNeverPersist(true);
                mc.setType("a-f-A");
                mc.setCallsign("WT888");
                final Marker m = mc.placePoint();
                m.setMetaLong("lastUpdateTime", new CoordinatedTime().getMilliseconds());
                m.setMetaLong("autoStaleDuration", 20000);
                m.setMetaBoolean("movable", false);
                m.setTrack(280, 50);
                m.setMetaDouble("Speed", 50d);
                m.setStyle(m.getStyle()
                        | Marker.STYLE_ROTATE_HEADING_MASK);



            }
        });


        final Button surfaceAtCenter = (Button) helloView
                .findViewById(R.id.surfaceAtCenter);
        surfaceAtCenter.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                 ElevationManager.QueryParameters DSM_FILTER = 
                                     new ElevationManager.QueryParameters();
                 DSM_FILTER.elevationModel = ElevationData.MODEL_SURFACE;
                 ElevationManager.QueryParameters DTM_FILTER = 
                                     new ElevationManager.QueryParameters();
                 DTM_FILTER.elevationModel = ElevationData.MODEL_TERRAIN;

                 Altitude terrain;
                 Altitude surface;

                 GeoPoint point = mapView.getCenterPoint();
                 if (point != null) {
                   // pull terrain
                   terrain = ElevationManager.getElevation(
                             point.getLatitude(),
                             point.getLongitude(),
                             DTM_FILTER);
                   // pull surface
                   surface = ElevationManager.getElevation(
                             point.getLatitude(),
                             point.getLongitude(),
                             DSM_FILTER);

                   if (terrain != null)
                       toast("Terrain: " + terrain);
                   if (surface != null)
                       toast("Surface: " + surface);
                   }
            }

        });

        final Button fakeContentProvider = (Button) helloView
                .findViewById(R.id.fakeContentProvider);
        fakeContentProvider.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                manipulateFakeContentProvider();
            }
        });

        final Button pluginNotification = (Button) helloView
                .findViewById(R.id.pluginNotification);
        pluginNotification.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent startServiceIntent =
                    new Intent(
                            "com.atakmap.android.helloworld.notification.NotificationService");
            ComponentName name = getMapView().getContext().startService(
                    startServiceIntent);

            }
        });

        final Button addStream = (Button) helloView
                .findViewById(R.id.addStream);
        addStream.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "connected to the cotservice: " + connected);
                try { 
                    if (connected) {
                         final File dir = new File(Environment.getExternalStorageDirectory()
                                   .getPath() + "/serverconnections");
                         File[] listing = dir.listFiles();
                         if (listing != null) { 
                             for (File f : listing) { 
                                 Log.d(TAG, "found: " + f);

                                    ImportMissionPackageSort importer = new ImportMissionPackageSort(
                                            getMapView().getContext(),
                                            true, true,
                                            false);
                                    if (!importer.match(f)) {
                                        Toast.makeText(getMapView().getContext(), 
                                                "failure [1]: " + f, Toast.LENGTH_SHORT) .show();
                                    } else { 

                                        boolean success = importer.beginImport(f);
                                        if (success) {
                                            Toast.makeText(getMapView().getContext(), "success: " + f,
                                                    Toast.LENGTH_SHORT).show();
                                        } else { 
                                            Toast.makeText(getMapView().getContext(), "failure [2]: " + f,
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    }
    
                             
                             }
                         }
                    }
                } catch (Exception ioe) { 
                   Log.d(TAG, "error: ", ioe);
                }
            } 
        });

        final Button removeStream = (Button) helloView
                .findViewById(R.id.removeStream);
        removeStream.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "connected to the cotservice: " + connected);
                if (connected)
                    csr.removeStream("**");
            } 
        });

    }

    synchronized public void runSim() { 
        Marker item = getMapView().getSelfMarker();
        if (item != null) {

            final Bundle data = getMapView().getMapData();

            GeoPoint gp = new GeoPoint(-44.0, 22.0); // decimal degrees

            data.putDouble("mockLocationSpeed", 20);   // speed in meters per second
            data.putFloat("mockLocationAccuracy", 5f); // accuracy in meters

            data.putString("locationSourcePrefix", "mock");
            data.putBoolean("mockLocationAvailable", true);

            data.putString("mockLocationSource", "Hello World Plugin");
            data.putString("mockLocationSourceColor", "#FFAFFF00");
            data.putBoolean("mockLocationCallsignValid", true);

            data.putParcelable("mockLocation", gp);

            data.putLong("mockLocationTime", SystemClock.elapsedRealtime());

            data.putLong("mockGPSTime", new CoordinatedTime().getMilliseconds());  // time as reported by the gps device

            data.putInt("mockFixQuality", 2);

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

    /**
     * Slower of the two methods to create a circle, but more accurate.
     */
    public void createEllipse(final Marker marker) {
        final Ellipse _accuracyEllipse = new Ellipse(UUID.randomUUID()
                .toString());
        _accuracyEllipse.setCenterHeightWidth(marker.getPoint(), 20, 20);
        _accuracyEllipse.setFillColor(Color.argb(50, 238, 187, 255));
        _accuracyEllipse.setFillStyle(2);
        _accuracyEllipse.setStrokeColor(Color.GREEN);
        _accuracyEllipse.setStrokeWeight(4);
        _accuracyEllipse.setMetaString("shapeName", "Error Ellipse");
        _accuracyEllipse.setMetaBoolean("addToObjList", false);
        getMapView().getRootGroup().addItem(_accuracyEllipse);
        marker.addOnPointChangedListener(new OnPointChangedListener() {
            @Override
            public void onPointChanged(final PointMapItem item) {
                _accuracyEllipse.setCenterHeightWidth(new GeoPoint(
                        item.getPoint()), 20, 20);
            }
        });
        MapEventDispatcher dispatcher = getMapView().getMapEventDispatcher();
        dispatcher.addMapEventListener(MapEvent.ITEM_REMOVED,
                new MapEventDispatcher.MapEventDispatchListener() {
                    public void onMapEvent(MapEvent event) {
                        if (event.getType().equals(MapEvent.ITEM_REMOVED)) {
                            MapItem item = event.getItem();
                            if (item.getUID().equals(marker.getUID()))
                                getMapView().getRootGroup().removeItem(
                                        _accuracyEllipse);
                        }
                    }
                });



    }

    /**
     * Faster of the two methods to create a circle.
     */
    public void createCircle(final Marker marker) {
        final Circle _accuracyCircle = new Circle(marker.getPoint(), 20);

        _accuracyCircle.setFillColor(Color.argb(50, 238, 187, 255));
        //_accuracyCircle.setFillStyle(2);
        _accuracyCircle.setStrokeColor(Color.GREEN);
        _accuracyCircle.setStrokeWeight(4);
        _accuracyCircle.setMetaString("shapeName", "Error Ellipse");
        _accuracyCircle.setMetaBoolean("addToObjList", false);

        getMapView().getRootGroup().addItem(_accuracyCircle);
        marker.addOnPointChangedListener(new OnPointChangedListener() {
            @Override
            public void onPointChanged(final PointMapItem item) {
                _accuracyCircle.setCenterPoint(marker.getPoint());
                _accuracyCircle.setRadius(20);
            }
        });
        MapEventDispatcher dispatcher = getMapView().getMapEventDispatcher();
        dispatcher.addMapEventListener(MapEvent.ITEM_REMOVED,
                new MapEventDispatcher.MapEventDispatchListener() {
                    public void onMapEvent(MapEvent event) {
                        if (event.getType().equals(MapEvent.ITEM_REMOVED)) {
                            MapItem item = event.getItem();
                            if (item.getUID().equals(marker.getUID()))
                                getMapView().getRootGroup().removeItem(
                                        _accuracyCircle);
                        }
                    }
                });

    }

    private void manipulateFakeContentProvider() {
        // delete all the records and the table of the database provider
        String URL = "content://com.javacodegeeks.provider.Birthday/friends";
        Uri friends = Uri.parse(URL);
        int count = pluginContext.getContentResolver().delete(
                friends, null, null);
        String countNum = "Javacodegeeks: " + count + " records are deleted.";
        toast(countNum);

        String[] names = new String[] {
                "Joe", "Bob", "Sam", "Carol"
        };
        String[] dates = new String[] {
                "01/01/2001", "01/01/2002", "01/01/2003", "01/01/2004"
        };
        for (int i = 0; i < names.length; ++i) {
            ContentValues values = new ContentValues();
            values.put(BirthProvider.NAME, names[i]);
            values.put(BirthProvider.BIRTHDAY, dates[i]);
            Uri uri = pluginContext.getContentResolver().insert(
                    BirthProvider.CONTENT_URI, values);
            toast("Javacodegeeks: " + uri + " inserted!");
        }

    }

    /**************************** PUBLIC METHODS *****************************/

    @Override
    public void disposeImpl() {
        // Remove Hello World contact
        removeContact(this.helloContact);
    }

    /**************************** INHERITED METHODS *****************************/

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "showing hello world drop down");
        if (intent.getAction().equals(SHOW_HELLO_WORLD)) {
            showDropDown(helloView, HALF_WIDTH, FULL_HEIGHT,
                    FULL_WIDTH, HALF_HEIGHT, false);
            setAssociationKey("helloWorldPreference");
            List<Contact> allContacts = Contacts.getInstance().getAllContacts();
            for (Contact c : allContacts) {
                if (c instanceof IndividualContact)
                    Log.d(TAG, "Contact IP address: "
                            + getIpAddress((IndividualContact) c));
            }

        } else if (intent.getAction().equals(CHAT_HELLO_WORLD)) {
            // Chat message sent to Hello World contact
            Bundle cotMessage = intent.getBundleExtra(
                    ChatManagerMapComponent.PLUGIN_SEND_MESSAGE_EXTRA);

            String msg = cotMessage.getString("message");

            if (!FileSystemUtils.isEmpty(msg)) {
                // Display toast to show the message was received
                toast(helloContact.getName() + " received: " + msg);
            }
        } else if (intent.getAction().equals(SEND_HELLO_WORLD)) {
            // Sending CoT to Hello World contact

            // Map item UID
            String uid = intent.getStringExtra("targetUID");
            MapItem mapItem = getMapView().getRootGroup().deepFindUID(uid);
            if (mapItem != null) {
                // Display toast to show the CoT was received
                toast(helloContact.getName() + " received request to send: "
                        + CoordOverlayMapReceiver.getDisplayName(mapItem));
            }
        }
    }

    public NetConnectString getIpAddress(IndividualContact ic)
    {
        Connector ipConnector = ic.getConnector(IpConnector.CONNECTOR_TYPE);
        if (ipConnector != null)
        {
            String connectString = ipConnector.getConnectionString();
            return NetConnectString.fromString(connectString);
        } else
        {
            return null;
        }

    }

    @Override
    public void onDropDownSelectionRemoved() {
    }

    @Override
    public void onDropDownVisible(boolean v) {
    }

    @Override
    public void onDropDownSizeChanged(double width, double height) {
    }

    @Override
    public void onDropDownClose() {
    }

    /************************* Helper Methods *************************/

    private RouteMapReceiver getRouteMapReceiver() {

        // TODO: this code was copied from another plugin.
        // Not sure why we can't just callRouteMapReceiver.getInstance();
        MapActivity activity = (MapActivity) getMapView().getContext();
        MapComponent mc = activity.getMapComponent(RouteMapComponent.class);
        if (mc == null || !(mc instanceof RouteMapComponent)) {
            Log.w(TAG, "Unable to find route without RouteMapComponent");
            return null;
        }

        RouteMapComponent routeComponent = (RouteMapComponent) mc;
        return routeComponent.getRouteMapReceiver();
    }

    private void toast(String str) {
        Toast.makeText(getMapView().getContext(), str,
                Toast.LENGTH_LONG).show();
    }

    public void createUnit() {

        Marker m = new Marker(getMapView().getPointWithElevation(), UUID
                .randomUUID().toString());
        Log.d(TAG, "creating a new unit marker for: " + m.getUID());
        m.setType("a-f-G-U-C-I");
        m.setMetaBoolean("readiness", true);
        m.setMetaBoolean("archive", true);
        m.setMetaString("how", "h-g-i-g-o");
        m.setMetaBoolean("editable", true);
        m.setMetaBoolean("movable", true);
        m.setMetaBoolean("removable", true);
        m.setMetaString("entry", "user");
        m.setMetaString("callsign", "Test Marker");
        m.setTitle("Test Marker");
        m.setMetaString("menu", getMenu());

        MapGroup _mapGroup = getMapView().getRootGroup()
                .findMapGroup("Cursor on Target")
                .findMapGroup("Friendly");
        _mapGroup.addItem(m);

        m.persist(getMapView().getMapEventDispatcher(), null,
                this.getClass());

        Intent new_cot_intent = new Intent();
        new_cot_intent.setAction("com.atakmap.android.maps.COT_PLACED");
        new_cot_intent.putExtra("uid", m.getUID());
        com.atakmap.android.ipc.AtakBroadcast.getInstance().sendBroadcast(
                new_cot_intent);

    }


    /**
     * For plugins to have custom radial menus, we need to set the "menu" metadata to 
     * contain a well formed xml entry.   This only allows for reskinning of existing 
     * radial menus with icons and actions that already exist in ATAK.
     * In order to perform a completely custom radia menu instalation. You need to 
     * define the radial menu as below and then uuencode the sub elements such as 
     * images or instructions.
     */
    private String getMenu() {
        return "<menu buttonRadius='70' buttonSpan='36' buttonWidth='90' buttonBg='bgs/button.xml'>"
                +

                "<button angle='-90' icon='icons/close.png' onClick='actions/cancel.xml' />"
                +

                "<button icon='"
                + getItem("remove.png")
                + "' onClick='"
                + getItem("remove.xml")
                + "' disabled='!{${removable}}' />"
                +
                "<button onClick='actions/9line.xml' icon='icons/cas.png' prefKey='nineline_default_menu' submenu='"
                + getSubmenu(formSubmenu()) + "' disableSwap='false'/>" +

                "</menu>";

    }


    /**
     * This is an example of a completely custom xml definition for a menu.   It uses the
     * plaintext stringified version of the current menu language plus uuencoded images 
     * and actions.
     */
    public String getMenu2() {
        return "<menu buttonWidth='90' buttonSpan='90' buttonRadius='70' buttonBg='bgs/dark_button.xml'>"
                +
                "<button angle='0' disabled='!{${removable}}' onClick='"
                + getItem("actions/atsk/atsk_delete_obs.xml")
                + "' icon='icons/delete.png' /> <button onClick='"
                + getItem("actions/atsk/atsk_edit_obs.xml")
                + "' icon='icons/obstruction_edit.png'/> "
                + "<button onClick='actions/cancel.xml' icon='icons/close.png'/> <button onClick='"
                + getItem("actions/atsk/atsk_obs_info.xml")
                + "' icon='icons/info.png'/> </menu>";
    }

    public String formSubmenu() {
        return "<menu buttonRadius='65' buttonSpan='32.73' buttonWidth='60' buttonBg='bgs/button.xml'> <button onClick='"
                + getItem("remove.xml")
                + "' prefValue='cas' icon='"
                + getItem("removeblue.png")
                + "'/> <button onClick='"
                + getItem("remove.xml")
                + "' prefValue='cff' icon='"
                + getItem("removeyellow.png") + "'/> </menu>";
    }

    public String getItem(final String file) {
        try {
            InputStream is = pluginContext.getAssets().open(file);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            int size = 0;
            byte[] buffer = new byte[1024];

            while ((size = is.read(buffer, 0, 1024)) >= 0) {
                outputStream.write(buffer, 0, size);
            }
            is.close();
            buffer = outputStream.toByteArray();

            String data = new String(Base64.encode(buffer, Base64.URL_SAFE
                    | Base64.NO_WRAP));

            return "base64:/" + data;
        } catch (Exception e) {
            return "";
        }
    }

    public String getSubmenu(final String submenu) {
        try {
            byte[] buffer = submenu.getBytes();
            String data = new String(Base64.encode(buffer, Base64.URL_SAFE
                    | Base64.NO_WRAP));
            return "base64:/" + data;
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Add a plugin-specific contact to the contacts list
     * This contact fires an intent when a message is sent to it,
     * instead of using the default chat implementation
     * @param name Contact display name
     * @return New plugin contact
     */
    public Contact addPluginContact(String name) {

        // Add handler for messages
        HelloWorldContactHandler contactHandler
                = new HelloWorldContactHandler(pluginContext);
        CotMapComponent.getInstance().getContactConnectorMgr()
                .addContactHandler(contactHandler);

        // Create new contact with name and random UID
        IndividualContact contact = new IndividualContact(
                name, UUID.randomUUID().toString());

        // Add plugin connector which points to the intent action
        // that is fired when a message is sent to this contact
        contact.addConnector(new PluginConnector(CHAT_HELLO_WORLD));

        // Add IP connector so the contact shows up when sending CoT or files
        contact.addConnector(new IpConnector(SEND_HELLO_WORLD));

        // Set default connector to plugin connector
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                getMapView().getContext());
        prefs.edit().putString("contact.connector.default." + contact.getUID(),
                PluginConnector.CONNECTOR_TYPE).apply();

        // Add new contact to master contacts list
        Contacts.getInstance().addContact(contact);

        return contact;
    }

    /**
     * Remove a contact from the master contacts list
     * This will remove it from the contacts list drop-down
     * @param contact Contact object
     */
    public void removeContact(Contact contact) {
        Contacts.getInstance().removeContact(contact);
    }
}
