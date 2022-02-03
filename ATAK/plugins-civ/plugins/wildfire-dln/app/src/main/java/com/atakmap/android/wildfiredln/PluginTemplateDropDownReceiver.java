
package com.atakmap.android.wildfiredln;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.wildfiredln.plugin.R;
import com.atakmap.android.dropdown.DropDown.OnStateListener;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.wildfiredln.WildfireDLN;

import com.atakmap.coremap.log.Log;

public class PluginTemplateDropDownReceiver extends DropDownReceiver implements
        OnStateListener {

    public static final String TAG = PluginTemplateDropDownReceiver.class.getSimpleName();

    public static final String SHOW_PLUGIN_TEMPLATE = "com.atakmap.android.wildfiredln.SHOW_PLUGIN_TEMPLATE";
    private final View templateView;
    private final Context pluginContext;
    private Activity pluginActivity;


    /**************************** CONSTRUCTOR *****************************/

    public PluginTemplateDropDownReceiver(final MapView mapView, final Context context) {
        super(mapView);
        this.pluginContext = context;
        LayoutInflater inflater = 
               (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        templateView = inflater.inflate(R.layout.plugin_template_layout, null);
    }

    /**************************** PUBLIC METHODS *****************************/

    public void disposeImpl() {
    }

    public void setActivity(Activity aa)
    {
        pluginActivity = aa;
    }

    /**************************** INHERITED METHODS *****************************/

    @Override
    public void onReceive(Context context, Intent intent) {
        
        Log.d(TAG, "showing plugin template drop down");
        if (intent.getAction().equals(SHOW_PLUGIN_TEMPLATE)) {
            
            showDropDown(templateView, HALF_WIDTH, FULL_HEIGHT, FULL_WIDTH, HALF_HEIGHT, false);

            Log.d(TAG,"pluginContext is "+pluginContext.toString());
            Log.d(TAG,"onReceive context is "+context.toString());

            WildfireDLN dlnInstance = new WildfireDLN(this.pluginContext, context, this.templateView, this.pluginActivity);

            //updateContent();

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
}
