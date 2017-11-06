package com.atakmap.android.helloworld;

import android.content.Context;
import android.content.Intent;
import com.atakmap.android.ipc.AtakBroadcast.DocumentedIntentFilter;

import com.atakmap.android.maps.MapView;
import com.atakmap.android.cot.UIDHandler;
import com.atakmap.android.dropdown.DropDownMapComponent;

import com.atakmap.android.maps.Marker;
import com.atakmap.coremap.cot.event.CotDetail;

import com.atakmap.coremap.log.Log;
import com.atakmap.android.helloworld.plugin.R;
import com.atakmap.app.preferences.ToolsPreferenceFragment;


/**
 * This is an example of a MapComponent within the ATAK 
 * ecosphere.   A map component is the building block for all
 * activities within the system.   This defines a concrete 
 * thought or idea. 
 */
public class HelloWorldMapComponent extends DropDownMapComponent {

    public static final String TAG = "HelloWorldMapComponent";

    private Context pluginContext;
    private HelloWorldDropDownReceiver dropDown;

    public void onCreate(final Context context, Intent intent,
            final MapView view) {

        context.setTheme(R.style.ATAKPluginTheme);
        super.onCreate(context, intent, view);
        pluginContext = context;

        // In this example, a drop down receiver is the 
        // visual component within the ATAK system.  The 
        // trigger for this visual component is an intent.   
        // see the plugin.HelloWorldTool where that intent
        // is triggered.
        this.dropDown = new HelloWorldDropDownReceiver(view, context);

        // We use documented intent filters within the system
        // in order to automatically document all of the 
        // intents and their associated purposes.

        Log.d(TAG, "registering the show hello world filter");
        DocumentedIntentFilter ddFilter = new DocumentedIntentFilter();
        ddFilter.addAction(HelloWorldDropDownReceiver.SHOW_HELLO_WORLD,
                "Show the Hello World drop-down");
        ddFilter.addAction(HelloWorldDropDownReceiver.CHAT_HELLO_WORLD,
                "Chat message sent to the Hello World contact");
        ddFilter.addAction(HelloWorldDropDownReceiver.SEND_HELLO_WORLD,
                "Sending CoT to the Hello World contact");
        this.registerReceiver(context, this.dropDown, ddFilter);
        Log.d(TAG, "registered the show hello world filter");


        // in this case we also show how one can register
        // additional information to the uid detail handle when 
        // generating cursor on target.   Specifically the 
        // NETT-T service specification indicates the the 
        // details->uid should be filled in with an appropriate
        // attribute.   

        // add in the nett-t required uid entry.
        UIDHandler.getInstance().addAttributeInjector(
            new UIDHandler.AttributeInjector() { 
                  public void injectIntoDetail(Marker marker, CotDetail detail) { 
                      if (marker.getType().startsWith("a-f")) 
                          return;
                      detail.setAttribute("nett", "XX");
                  }
                  public void injectIntoMarker(CotDetail detail, Marker marker) { 
                      if (marker.getType().startsWith("a-f")) 
                          return;
                      String callsign = detail.getAttribute("nett");
                      if (callsign != null) 
                          marker.setMetaString("callsign", callsign);
                  }
        
            } );


        // for custom preferences
        ToolsPreferenceFragment
                .register(
                new ToolsPreferenceFragment.ToolPreference(
                        "Hello World Preferences",
                        "This is the sample preference for Hello World",
                        "helloWorldPreference",
                        context.getResources().getDrawable(
                                R.drawable.ic_launcher),
                        new HelloWorldPreferenceFragment(context)));

    }

    @Override
    protected void onDestroyImpl(Context context, MapView view) {
        Log.d(TAG, "calling on destroy");
        this.dropDown.dispose();
        ToolsPreferenceFragment.unregister("helloWorldPreference");
        super.onDestroyImpl(context, view);
    }
}
