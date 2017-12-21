package com.atakmap.android.wildfiredln;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.atakmap.android.wildfiredln.plugin.R;

/**
 * Useful docstring
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

        Button refreshButton = (Button) templateView.findViewById(R.id.refreshButton);
        refreshButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                updateContent();
            }
        });

        updateContent();
    }

    public void updateContent() {
        //trackprogress("25");
        toggleProgress(View.VISIBLE);
        final TextView mTextView = (TextView) templateView.findViewById(R.id.text);

        final String oldText = mTextView.getText().toString();

        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(pluginContext);
        String url ="http://wdln-ferry-00/web";

        //trackprogress("50");

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.
                        mTextView.setText(oldText+response.substring(0,500));
                        handleResponse(response);
                        //trackprogress("100");
                        toggleProgress(View.GONE);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                mTextView.setText("Ferry unreachable.");
                toggleProgress(View.GONE);
                //trackprogress("100");
            }
        });

        // Add the request to the RequestQueue.
        queue.add(stringRequest);
        trackprogress("75");
    }

    private void handleResponse(String response) {
        int count = 0;
        while (response.contains("href")) {
            int idx = response.indexOf("href");
            response = response.substring(idx);
            idx = response.indexOf(">");
            String label = response.substring(0,idx);
            newTableRow(label,count);

            response = response.substring(idx);
            count += 1;
        }

    }

    private void newTableRow(String label, int count) {
        final TableLayout tableLayout = (TableLayout) templateView.findViewById(R.id.resourcesTable);
        TableRow row = new TableRow(pluginContext);
        TableRow.LayoutParams lp = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT);
        row.setLayoutParams(lp);
        TextView tv = new TextView(pluginContext);
        tv.setText(label);
        ImageButton ib = new ImageButton(pluginContext);
        ib.setImageResource(R.drawable.dl_48x48);
        row.addView(tv);
        row.addView(ib);
        tableLayout.addView(row,count);
    }

    private void toggleProgress(int v) {
        final ProgressBar pb = (ProgressBar) templateView.findViewById(R.id.refreshBar);
        pb.setVisibility(v);
    }

    private void trackprogress(String s) {
        final ProgressBar pb = (ProgressBar) templateView.findViewById(R.id.refreshBar);
        pb.setProgress(Integer.parseInt(s));
    }

}
