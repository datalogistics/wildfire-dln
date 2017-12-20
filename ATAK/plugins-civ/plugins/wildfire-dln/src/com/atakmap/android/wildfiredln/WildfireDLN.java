package com.atakmap.android.wildfiredln;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
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

import com.atakmap.coremap.log.Log;

/**
 * Created by staden on 12/20/17.
 */

public class WildfireDLN {
    public static final String TAG = PluginTemplateDropDownReceiver.class.getSimpleName();

    public static final String SHOW_PLUGIN_TEMPLATE = "com.atakmap.android.wildfiredln.SHOW_PLUGIN_TEMPLATE";
    private View templateView;
    private Context pluginContext;

    /**************************** CONSTRUCTOR *****************************/
    public WildfireDLN(Context context, View templateView){
        //super(mapView);
        this.pluginContext = context;
        this.templateView = templateView;

        Button refreshButton = (Button) templateView.findViewById(R.id.button);
        refreshButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                updateContent();
            }
        });

        updateContent();
    }

    public void updateContent() {
        final TextView mTextView = (TextView) templateView.findViewById(R.id.text);

        final String oldText = mTextView.getText().toString();

        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(pluginContext);
        String url ="http://wdln-ferry-00/web";

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.
                        mTextView.setText(oldText + response.substring(0,500));
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                mTextView.setText("That didn't work!");
            }
        });
        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

}
