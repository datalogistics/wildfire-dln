
package com.atakmap.android.elevation.dsm;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import com.atakmap.android.elevation.dsm.util.RasterLayerAdapter;
import com.atakmap.android.maps.AbstractMapComponent;
import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;
import com.atakmap.map.elevation.ElevationManager;
import com.atakmap.map.layer.raster.mosaic.MosaicDatabase2;

import android.content.Context;
import android.content.Intent;
import com.atakmap.android.ipc.AtakBroadcast.DocumentedIntentFilter;

public class DSMMapComponent extends AbstractMapComponent {
    public Context pluginContext;
    public static final String TAG = "DSMManagerMapComponent";
    
    private DSMManagerDropDownReceiver receiver;

    public void onCreate(Context context, Intent intent, MapView view) {
        pluginContext = context;

        DSMManager.initialize();
        
        // XXX - on background thread
        DSMManager.refresh();

        // register data spi with elevation service
        ElevationManager.registerDataSpi(DSMElevationData.SPI);
        // register DSM DB with elevation service
        ElevationManager.registerElevationSource(DSMManager.getDb().dsmmdb);
        
        this.receiver = new DSMManagerDropDownReceiver(view, context, RasterLayerAdapter.INSTANCE);
        this.registerReceiver(view.getContext(), this.receiver, new DocumentedIntentFilter(DSMManagerDropDownReceiver.SHOW_WX_REPORT));
    }

    @Override
    protected void onDestroyImpl(Context context, MapView view) {
        ElevationManager.unregisterElevationSource(DSMManager.getDb().dsmmdb);

        ElevationManager.unregisterDataSpi(DSMElevationData.SPI);
        
        DSMManager.teardown();
    }

    @Override
    public void onStart(Context context, MapView view) {}

    @Override
    public void onStop(Context context, MapView view) {}

    @Override
    public void onPause(Context context, MapView view) {}

    @Override
    public void onResume(Context context, MapView view) {}
}
