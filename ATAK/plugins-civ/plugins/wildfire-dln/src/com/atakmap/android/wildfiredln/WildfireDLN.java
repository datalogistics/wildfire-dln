package com.atakmap.android.wildfiredln;
import android.app.DownloadManager;

import android.content.Context;
import android.os.Environment;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.wildfiredln.plugin.R;
import com.atakmap.map.layer.raster.DatasetRasterLayer2;

import java.io.File;
import java.util.Vector;

import static android.content.Context.DOWNLOAD_SERVICE;
import static com.atakmap.android.maps.MapView.getMapView;

/**
 * WildfireDLN class
 */

public class WildfireDLN
{
    //NOTE: the prevalence of static variables here is due to the plugin GUI being destroyed/created by ATAK periodically.
    //Probably makes more sense to split some of this into a separate static class but this works for now.

    public static final String TAG = PluginTemplateDropDownReceiver.class.getSimpleName();

    public static final String SHOW_PLUGIN_TEMPLATE = "com.atakmap.android.wildfiredln.SHOW_PLUGIN_TEMPLATE";
    private View templateView;
    private Context pluginContext;
    private static Vector<DownloadReference> downloadReferences;
    private static DownloadManager dmanager = null;
    private String[] menu;
    private static NetworkManager nManager = null;
    private static Thread nManagerThread = null;
    private static Vector<Marker> nodeMarkers = null;
    private static LayerManager lManager;
    DatasetRasterLayer2 drl = null;

    /**************************** CONSTRUCTOR *****************************/
    public WildfireDLN(Context context, View templateView)
    {
        this.pluginContext = context;
        this.templateView = templateView;

        String filepath = Environment.getExternalStorageDirectory().toString();
        File fileDirectory = new File(filepath+"/ATAK_Downloads");
        if(!fileDirectory.exists())
        {
            fileDirectory.mkdir();
        }

        if(dmanager == null)
        {
            dmanager = (DownloadManager) context.getSystemService(DOWNLOAD_SERVICE);
        }

        if(nodeMarkers == null)
        {
            nodeMarkers = new Vector<Marker>();
        }

        Button refreshButton = (Button) templateView.findViewById(R.id.refreshButton);
        refreshButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                updateContent();
            }
        });

        //start network manager
        if(nManager == null)
        {
            nManager = new NetworkManager(this, pluginContext);
            nManagerThread = new Thread(nManager);
            nManagerThread.start();
        }

        // updateContent();

        if(lManager == null)
        {
            lManager = new LayerManager(filepath);
        }
    }

    public void updateContent()
    {
        if(nManager != null)
        {
            nManager.requestRefresh();
        }
    }

    public void UpdateReferences(Vector<DownloadReference> references) {

        if(downloadReferences != null)
        {
            downloadReferences = DownloadReference.UpdatePreserveExisting(downloadReferences,references);
        }
        else
        {
            downloadReferences = references;
        }


        final TableLayout tableLayout = (TableLayout) templateView.findViewById(R.id.resourcesTable);
        tableLayout.removeAllViews();

        for(int i=0;i<downloadReferences.size();i++)
        {
            DownloadReference dr = downloadReferences.get(i);
            newTableRow(dr.GetName(), i,downloadReferences.get(i));

            if(dr.IsLayer())
            {
                lManager.AddLayer(dr);
            }
        }

        tableLayout.invalidate();
    }

    public void UpdateLocations(Vector<NodeReference> nodes)
    {
        MapGroup _mapGroup = getMapView().getRootGroup()
                .findMapGroup("Cursor on Target")
                .findMapGroup("Friendly");

        Vector<Marker> toRemove = new Vector<Marker>();

        //remove markers that didn't get updated
        for(int i=0; i<nodeMarkers.size();i++)
        {
            boolean found = false;
            Marker m = nodeMarkers.get(i);

            for(int j=0;j<nodes.size();j++)
            {
                NodeReference n = nodes.get(j);
                if(m.getMetaString("ID","").equals(n.getID()) && m.getTitle().equals(n.getName()))
                {
                    found = true;
                }
            }
            if(!found)
            {
                _mapGroup.removeItem(m);
                toRemove.add(m);
            }

        }

        while(!toRemove.isEmpty())
        {
            nodeMarkers.remove(toRemove.firstElement());
            toRemove.remove(0);
        }

        for(int i=0; i<nodes.size();i++)
        {
            NodeReference n = nodes.get(i);

            //check if marker exists
            boolean exists = false;
            for(int j=0;j<nodeMarkers.size() && !exists;j++)
            {
                Marker m = nodeMarkers.get(j);

                if(m.getMetaString("ID","").equals(n.getID()) && m.getTitle().equals(n.getName()))
                {
                    if(n.getLocation() == null)
                    {
                        _mapGroup.removeItem(m);
                    }
                    else
                    {
                        m.setPoint(n.getLocation());
                        exists = true;
                    }
                }
            }

            if(!exists && n.getLocation() != null)
            {
                Log.d(TAG, "Creating Marker: " + n.getName());
                //make a marker
                Marker m = new Marker(n.getLocation(), n.getName());
                //m.setVisible(true);
                //m.setTitle("testm");
                //getMapView().getRootGroup().addItem(testm);;
                //Marker m = new Marker(getMapView().getPointWithElevation(), UUID
                //        .randomUUID().toString());
                Log.d(TAG, "creating a new unit marker for: " + m.getUID());
                m.setType("a-f-G-E");
                //m.setMetaBoolean("readiness", true);
                //m.setMetaBoolean("archive", true);
                //m.setMetaString("how", "h-g-i-g-o");
                //m.setMetaBoolean("editable", true);
                m.setMetaBoolean("movable", false);
                //m.setMetaBoolean("removable", true);
                //m.setMetaString("entry", "user");
                //m.setMetaString("callsign", "Test Marker");
                m.setMetaString("ID", n.getID());
                m.setTitle(n.getName());
                _mapGroup.addItem(m);


                m.persist(getMapView().getMapEventDispatcher(), null,
                        this.getClass());

                nodeMarkers.add(m);

                /*Intent new_cot_intent = new Intent();
                new_cot_intent.setAction("com.atakmap.android.maps.COT_PLACED");
                new_cot_intent.putExtra("uid", m.getUID());
                com.atakmap.android.ipc.AtakBroadcast.getInstance().sendBroadcast(
                        new_cot_intent);*/
            }
        }
    }

    private void newTableRow(String label, int count,DownloadReference dr)
    {
        final TableLayout tableLayout = (TableLayout) templateView.findViewById(R.id.resourcesTable);
        TableRow row = new TableRow(pluginContext);
        TableRow.LayoutParams lp = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT);
        row.setLayoutParams(lp);

        TextView tv = new TextView(pluginContext);
        tv.setText(label);
        tv.setMaxWidth(500);
        tv.setHorizontallyScrolling(true);
        tv.setMovementMethod(new ScrollingMovementMethod());

        final ImageButton dlButton = new ImageButton(pluginContext);
        final ImageButton centerButton = new ImageButton(pluginContext);
        centerButton.setImageResource(R.drawable.center_48x48);

        if(dr.GetIsLocal())
        {
            if(dr.IsLayer())
            {
                if(lManager.GetLayerVisibility(dr.GetName()))
                {
                    dlButton.setImageResource(R.drawable.eye_open_48x48);
                }
                else
                {
                    dlButton.setImageResource(R.drawable.eye_closed_48x48);
                }

            }
            else
            {
                dlButton.setImageResource(R.drawable.open_48_48);
            }
        }
        else
        {
            if(dr.GetIsDownloadInProgress())
            {
                dlButton.setImageResource(R.drawable.cancel_48x48);
            }
            else
            {
                dlButton.setImageResource(R.drawable.dl_48x48);
            }
        }

        final int id = count;
        dlButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                DownloadByID(id, dlButton);
            }
        });

        if(dr.GetIsLocal())
        {
            if (dr.IsLayer())
            {
                final String mapstring = dr.GetName();
                centerButton.setOnClickListener(new View.OnClickListener()
                {
                    public void onClick(View v)
                    {
                        lManager.ZoomToLayer(mapstring);
                    }
                });
            }
        }

        ProgressBar pb = new ProgressBar(pluginContext,null,android.R.attr.progressBarStyleHorizontal);
        pb.setMax(100);
        pb.setProgress(0);
        if(dr.GetIsDownloadInProgress())
        {
            pb.setVisibility(View.VISIBLE);
            pb.setProgress(dr.progress);
        }
        else
        {
            pb.setVisibility(View.INVISIBLE);
        }

        pb.setTag("PB-"+count);

        LinearLayout ll = new LinearLayout(pluginContext);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.addView(tv);
        ll.addView(pb);

        row.addView(ll);
        row.addView(dlButton);

        if(dr.GetIsLocal())
        {
            if (dr.IsLayer())
            {
                row.addView(centerButton);
            }
        }
        //row.addView(viewButton);
        tableLayout.addView(row,count);
    }

    public void toggleProgress(int v)
    {
        final ProgressBar pb = (ProgressBar) templateView.findViewById(R.id.refreshBar);
        pb.setVisibility(v);
    }

    public void DownloadByID(int id, ImageButton dlButton)
    {
        Log.d(TAG, "Downloading: "+downloadReferences.get(id).GetName()+" From: "+downloadReferences.get(id).GetURL());
        DownloadReference dr = downloadReferences.get(id);
        dr.SetParent(this);

        if(dr.IsLayer())
        {
            if(lManager.GetLayerVisibility(dr.GetName()))
            {
                lManager.SetLayerVisibility(dr.GetName(),false);
                dlButton.setImageResource(R.drawable.eye_closed_48x48);
            }
            else
            {
                lManager.SetLayerVisibility(dr.GetName(),true);
                dlButton.setImageResource(R.drawable.eye_open_48x48);
            }
        }
        else
        {
            dr.StartDownload(dmanager, dlButton);
        }
    }


    public void UpdateDownloadProgress(DownloadReference dr, Integer progress)
    {
        int id = downloadReferences.indexOf(dr);
        Log.d(TAG, "ID is: "+id);

        if(id>=0)
        {
            final TableLayout tableLayout = (TableLayout) templateView.findViewById(R.id.resourcesTable);
            TableRow row = (TableRow) tableLayout.getChildAt(id);
            LinearLayout ll = (LinearLayout) row.getChildAt(0);

            ProgressBar pb = (ProgressBar) ll.getChildAt(1);
            Log.d(TAG, "TAG is: "+pb.getTag());

            if(progress >= 0)
            {
                pb.setVisibility(View.VISIBLE);
                pb.setProgress(progress);
            }
            else
            {
                pb.setVisibility(View.INVISIBLE);
                ImageButton btn = (ImageButton) row.getChildAt(1);
                if(progress == -1 || progress == -2)
                {
                    btn.setImageResource(R.drawable.dl_48x48);
                }
                else if(progress == -3)
                {
                    btn.setImageResource(R.drawable.open_48_48);
                }
            }

            tableLayout.invalidate();
        }
    }

    public Context GetContext()
    {
        return pluginContext;
    }
}
