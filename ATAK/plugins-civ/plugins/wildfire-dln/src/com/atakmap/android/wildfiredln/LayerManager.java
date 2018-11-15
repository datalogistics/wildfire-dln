package com.atakmap.android.wildfiredln;

import android.provider.ContactsContract;
import android.util.Log;

import com.atakmap.android.maps.MapView;
import com.atakmap.map.layer.Layers;
import com.atakmap.map.layer.raster.DatasetRasterLayer2;
import com.atakmap.map.layer.raster.LocalRasterDataStore;
import com.atakmap.map.layer.raster.PersistentRasterDataStore;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import static com.atakmap.android.maps.MapView.getMapView;

public class LayerManager
{
    public static final String TAG = PluginTemplateDropDownReceiver.class.getSimpleName();
    //private LocalRasterDataStore Datastore;
    private File layerDirectory;
    private HashMap<String, DatasetRasterLayer2> RasterLookup;
    private HashMap<String,LocalRasterDataStore> DatastoreLookup;

    public LayerManager(String filepath)
    {
        RasterLookup = new HashMap<String, DatasetRasterLayer2>();

        //layer test
        layerDirectory = new File(filepath+"/ATAK_WDLN_Layers");
        if(!layerDirectory.exists())
        {
            layerDirectory.mkdir();
        }

        //remove old data
        DeleteTree(layerDirectory);

        DatastoreLookup = new HashMap<String, LocalRasterDataStore>();
    }

    public boolean AddLayer(DownloadReference dr)
    {
        boolean succeed = true;
        Log.d(TAG, "Attempting to add raster to datastore: "+dr.GetURL());
        try
        {
            LocalRasterDataStore Datastore;
            final File dataStoreDb = new File(layerDirectory, dr.GetName()+".sqlite");
            Datastore = new PersistentRasterDataStore(dataStoreDb,layerDirectory);
            Datastore.refresh();

            File inf = new File(dr.GetURL());
            if(!Datastore.contains(inf))
            {
                Datastore.add(inf);
                DatasetRasterLayer2 layer = new DatasetRasterLayer2(dr.GetName(),Datastore,0);

                DatastoreLookup.put(dr.GetName(), Datastore);
                RasterLookup.put(dr.GetName(),layer);
                //getMapView().addLayer(MapView.RenderStack.MAP_SURFACE_OVERLAYS,layer);
                layer.setVisible(false);

                Log.d(TAG, "Successfully added raster to datastore");
                Log.d(TAG, "selections: "+layer.getSelectionOptions());
                Log.d(TAG, "geometry: "+layer.getGeometry("test.tif"));
                Log.d(TAG, "minres: "+layer.getMinimumResolution("test.tif"));
                Log.d(TAG, "maxres: "+layer.getMaximumResolution("test.tif"));
            }

        } catch (IOException e)
        {
            succeed = false;
            e.printStackTrace();
            Log.d(TAG, "Failed To Add Raster "+dr.GetName()+" To Datastore");
        }

        Layers.registerAll();

        return succeed;
    }

    public static void DeleteTree(File root)
    {
        String[] entries = root.list();
        for(String s: entries){
            File currentFile = new File(root.getPath(),s);

            if(currentFile.isDirectory())
            {
                DeleteTree(currentFile);
            }
            currentFile.delete();
        }
    }

    public boolean GetLayerVisibility(String name)
    {
        if(!RasterLookup.containsKey(name))
        {
            return false;
        }

        return RasterLookup.get(name).isVisible();
    }

    public void SetLayerVisibility(String name,boolean visibility)
    {
        Log.d(TAG, "Displaying Layer: "+name);
        DatasetRasterLayer2 layer = RasterLookup.get(name);
        layer.setVisible(visibility);

        if(visibility)
        {
            getMapView().addLayer(MapView.RenderStack.MAP_SURFACE_OVERLAYS,layer);
        }
        else
        {
            getMapView().removeLayer(MapView.RenderStack.MAP_SURFACE_OVERLAYS,layer);
        }
    }
}
