
package com.atakmap.android.wxreport;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.atakmap.android.dropdown.DropDownMapComponent;
import com.atakmap.android.ipc.AtakBroadcast.DocumentedIntentFilter;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.wxreport.plugin.ContextHelperSingleton;
import com.atakmap.android.wxreport.plugin.R;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;
import com.atakmap.map.layer.raster.DatasetRasterLayer2;
import com.atakmap.map.layer.raster.LocalRasterDataStore;
import com.atakmap.map.layer.raster.PersistentRasterDataStore;
import com.atakmap.map.layer.raster.RasterLayer2;
import com.atakmap.map.layer.raster.service.OnlineImageryExtension;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

public class WxReportMapComponent extends DropDownMapComponent
{
    // XXX - exclude GEOMET sources for the time being as that data is hosted by
    //       the Canadian government
    private final static String[] XMLS_TO_DEPLOY =
    {
            //"CloudCover.xml",
            //"Precipitation.xml",
            "Radar.xml",
            //"Winds.xml",
    };

    private final static int XMLS_VERSION = 1;

    private RasterLayer2 overlayLayer;
    private LocalRasterDataStore dataStore;
    public static final String TAG = WxReportMapComponent.class.getSimpleName();

    public static final String DIRECTORY_PATH = "tools" + File.separator + "wx";
    public static final String USER_GUIDE = "Weather_Report_Plugin_User_Guide.pdf";

    public void onCreate(final Context context, Intent intent,
            final MapView view)
    {
        super.onCreate(context, intent, view);
        context.setTheme(R.style.ATAKPluginTheme);

        final SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(view.getContext());

        // check for any available overlays
        File overlaysDir = FileSystemUtils.getItem(DIRECTORY_PATH);
        if (!overlaysDir.exists())
        {
            // The wx directory does not exist on the device, create
            Log.d(TAG, "The wx directory is not present");
            boolean madeDir = overlaysDir.mkdir();
            Log.d(TAG,overlaysDir.getPath() + " was created = " + madeDir);
        }

        //make sure user guide is in wx directory
        String pdf = DIRECTORY_PATH + File.separator + USER_GUIDE;
        if (!FileSystemUtils.getItem(pdf).exists())
        {
            FileSystemUtils.copyFromAssetsToStorageFile(
                    context,
                    USER_GUIDE,
                    DIRECTORY_PATH + File.separator + USER_GUIDE,
                    false);
        }
        //make sure the weather files are in the wx directory
        final int xmlsVersion = prefs.getInt("wxreport.xmls-versions", 0);

        // Copy preconfigured weather files if needed
        for (String aXMLS_TO_DEPLOY : XMLS_TO_DEPLOY) {
            Log.d(TAG, "Checking for " + aXMLS_TO_DEPLOY
                    + " and copy if needed from assets");
            if (!FileSystemUtils.getItem(
                    DIRECTORY_PATH + File.separator + aXMLS_TO_DEPLOY)
                    .exists()
                    || (xmlsVersion != XMLS_VERSION)) {
                FileSystemUtils.copyFromAssetsToStorageFile(
                        context,
                        aXMLS_TO_DEPLOY,
                        DIRECTORY_PATH + File.separator + aXMLS_TO_DEPLOY,
                        false);
            }
        }

        // The XMLs have been deployed, update the recorded version
        prefs.edit().putInt("wxreport.xmls-versions", XMLS_VERSION).apply();

        // create a working directory where we will keep the data store
        // database and any other working files. we are considering the
        // working directory temporary for the time being, so if it already
        // existed delete it
        final File workingDir = new File(overlaysDir, "workingdir");
        if (!workingDir.exists()){
            boolean madeDir =  workingDir.mkdir();
            Log.d(TAG,workingDir.getPath() + " was created = " + madeDir);
        }


        // Indicate a datastore for SQL for rasters
        final File dataStoreDb = new File(workingDir, "datastore.sqlite");

        // Create a new RasterDataStore.  The data store will hold the
        // different weather overlays and will be used to create a
        // RasterLayer to be added to the map. Refresh to update latest.
        this.dataStore = new PersistentRasterDataStore(dataStoreDb, workingDir);
        this.dataStore.refresh();

        // iterate through the files in the 'wx' directory and try adding
        // them to the data store
        File[] files = overlaysDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile())
                    try {
                        // add the file (ignoring exceptions)
                        if (!this.dataStore.contains(file))
                            this.dataStore.add(file);
                    } catch (IOException ignored) {
                    }
            }
        }

        // create a new DatasetRasterLayer using the data store that holds
        // the overlays from the 'wx' directory. Set the limit to '0' to
        // allow any dataset that is appropriate for the current viewport
        // and resolution to be drawn
        this.overlayLayer = new DatasetRasterLayer2("Wx Overlays",
                this.dataStore, 0);
        OnlineImageryExtension offlineSvc = this.overlayLayer
                .getExtension(OnlineImageryExtension.class);
        if (offlineSvc != null)
            offlineSvc.setCacheAutoRefreshInterval(60000L);

        final Collection<String> opts = this.overlayLayer.getSelectionOptions();

        // set the initial transparency value on the various selections
        for (String opt : opts) {
            final float alpha = prefs.getFloat(
                    "wxreport.selection-transparency." + opt, 0.5f);
            this.overlayLayer.setTransparency(opt, alpha);
        }

        for (String opt : opts) {
            final boolean vis = prefs.getBoolean("wxreport.selection-visible."
                    + opt, true);
            this.overlayLayer.setVisible(opt, vis);
        }

        // add the RasterLayer to the map in the "surface overlays" stack.
        // this stack resides above the raster data layer but below point
        // and line overlays
        view.addLayer(MapView.RenderStack.MAP_SURFACE_OVERLAYS,
                this.overlayLayer);

        WxReportDropDownReceiver ddr = new WxReportDropDownReceiver(view,
                context, this.overlayLayer);
        ContextHelperSingleton.getInstance().setMapView(view);

        Log.d(TAG, "registering the show weather report filter");
        DocumentedIntentFilter ddFilter = new DocumentedIntentFilter();
        ddFilter.addAction(WxReportDropDownReceiver.SHOW_WX_REPORT);
        this.registerReceiver(context, ddr, ddFilter);
        Log.d(TAG, "registered the show weather report filter");
    }

    @Override
    protected void onDestroyImpl(Context context, MapView view) {
        super.onDestroyImpl(context, view);

        // remove the RasterLayer
        if (this.overlayLayer != null) {
            // record the current visibility and transparency states
            final Collection<String> opts = this.overlayLayer
                    .getSelectionOptions();

            final SharedPreferences.Editor prefs = PreferenceManager
                    .getDefaultSharedPreferences(view.getContext()).edit();

            // check for the selection transparency control on the layer
            for (String opt : opts) {
                final float alpha = this.overlayLayer.getTransparency(opt);
                prefs.putFloat("wxreport.selection-transparency." + opt, alpha);
            }

            for (String opt : opts) {
                final boolean vis = this.overlayLayer.isVisible(opt);
                prefs.putBoolean("wxreport.selection-visible." + opt, vis);
            }
            prefs.apply();

            view.removeLayer(MapView.RenderStack.MAP_SURFACE_OVERLAYS,
                    this.overlayLayer);
            this.overlayLayer = null;
        }

        // dispose the datastore and delete the working directory
        if (this.dataStore != null) {
            try {
                this.dataStore.dispose();
                File workingDir = FileSystemUtils
                        .getItem("tools/wx/workingdir");
                if (workingDir.exists()){
                    boolean deleted =  workingDir.delete();
                    Log.d(TAG,"Deleted working directory " + workingDir.getPath() + " " + deleted);
                }

            } catch (Throwable ignored) {
            }
        }
    }
}
