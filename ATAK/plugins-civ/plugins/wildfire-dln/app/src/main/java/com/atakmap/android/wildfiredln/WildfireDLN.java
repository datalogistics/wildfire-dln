package com.atakmap.android.wildfiredln;
import android.app.Activity;
import android.app.DownloadManager;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.UriPermission;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.media.MediaMetadataRetriever;

import com.atakmap.android.image.ExifHelper;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.ipc.DocumentedExtra;
import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.missionpackage.file.MissionPackageManifest;
import com.atakmap.android.wildfiredln.plugin.R;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.map.layer.raster.DatasetRasterLayer2;
import com.atakmap.coremap.maps.assets.Icon;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import static android.media.MediaMetadataRetriever.METADATA_KEY_LOCATION;
import static com.atakmap.android.maps.MapView.getMapView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.FileProvider;
import androidx.documentfile.provider.DocumentFile;

import com.atakmap.android.maps.DefaultMapGroup;

import org.apache.sanselan.formats.tiff.TiffImageMetadata;

/**
 * WildfireDLN class
 */

public class WildfireDLN
{
    //NOTE: the prevalence of static variables here is due to the plugin GUI being destroyed/created by ATAK periodically.
    //Probably makes more sense to split some of this into a separate static class but this works for now.

    public static final String TAG = "WildfireDLN";

    public static final String SHOW_PLUGIN_TEMPLATE = "com.atakmap.android.wildfiredln.SHOW_PLUGIN_TEMPLATE";
    private View templateView;
    private Context pluginContext;
    private Context atakContext;
    private Activity pluginActivity;
    private static Vector<DownloadReference> downloadReferences;
    private static DownloadManager dmanager = null;
    private String[] menu;
    private static NetworkManager nManager = null;
    private static Thread nManagerThread = null;
    private static Vector<Marker> nodeMarkers = null;
    private static Vector<Marker> mediaMarkers = null;
    private static LayerManager lManager;
    private static Vector<String> validIPs = null;
    private static MapGroup wdlngroup = null;
    private WDLNReceiver wdlnreceiver = null;
    private Icon cameraIcon;
    private String DataFolder;
    private DocumentFile DataTree;
    private DocumentFile DataDownloads;
    private DocumentFile DataResources;
    SharedPreferences sharedPref = null;
    DatasetRasterLayer2 drl = null;

    /**************************** CONSTRUCTOR *****************************/
    public WildfireDLN(Context context, Context acxt, View templateView, Activity aa)
    {
        this.pluginContext = context;
        this.templateView = templateView;
        this.pluginActivity = aa;
        this.atakContext = acxt;

        //had to switch to plugin internal storage due to scoped permissions on newer android versions
        //which is inconvenient as the data will be lost if the plugin is uninstalled and it's harder for the user to get at it
        //independent from the plugin

        //Log.d(TAG,"App Download Folder: "+filepath.getAbsolutePath());

        File internalDirectory = pluginContext.getExternalFilesDir(null);

        File fileDirectory = new File(internalDirectory,"/ATAK_Downloads");

        Log.d(TAG,"Application Storage Directory is: "+fileDirectory.getAbsolutePath());

        if(!fileDirectory.exists())
        {
            fileDirectory.mkdir();
        }

        File wdlnDirectory = new File(internalDirectory,"/WDLN");
        if(!wdlnDirectory.exists())
        {
            wdlnDirectory.mkdir();
        }

        File resourcesDirectory = new File(wdlnDirectory,"/Resources");
        if(!resourcesDirectory.exists())
        {
            resourcesDirectory.mkdir();
        }


        /*Uri uri = FileProvider.getUriForFile(GetContext(),"com.atakmap.android.wildfiredln.plugin.fileprovider",new File(resourcesDirectory,"wdlnCamera.png"));

        Log.d(TAG,"Granting access");
        pluginContext.grantUriPermission("com.atakmap.app.civ",uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);
        Log.d(TAG,"Granted access");*/

        File cameraIconF = new File(resourcesDirectory,"wdlnCamera.png");

        //attempted to use SAF, couldn't figure out how to get permissions to the plugin
        //Calling the separate activity to request permissions results in that activity getting permissions and not the plugin
        //might be possible in future atak versions if they switch to the newer androidx activity type
        /*Log.d(TAG,"Preference String is "+pluginContext.getString(R.string.preference_file_key));

        DataFolder = getSharedDirectory();

        //sharedPref = pluginContext.getSharedPreferences(pluginContext.getString(R.string.preference_file_key),pluginContext.MODE_PRIVATE);
        //DataFolder = sharedPref.getString(pluginContext.getString(R.string.shared_data_folder),"");
        Log.d(TAG,"Existing Data Folder Is "+DataFolder);

        if(DataFolder.equals("") || !arePermissionsGranted(DataFolder))//request folder permissions
        {

            Intent customdialog = new Intent(pluginContext, WDLNTreePermissionsActivity.class);
            WDLNTreePermissionsActivity.go = true;
            Log.d(TAG,atakContext.getApplicationContext().toString());
            pluginActivity.startActivity(customdialog);

            Log.d(TAG,"Updating Shared Folder");

            int waitc = 0;
            while(WDLNTreePermissionsActivity.go)
            {
                try {
                    Thread.sleep(100);
                    Log.d(TAG,"Waiting For Permissions");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }

                waitc++;

                if(waitc>150)
                {
                    Log.d(TAG,"Timed out waiting for permissions");
                    break;
                }
            }

            DataFolder = getSharedDirectory();
            //DataFolder = sharedPref.getString(pluginContext.getString(R.string.shared_data_folder),"");
            Log.d(TAG,"Got new shared folder at "+DataFolder);
        }

        Log.d(TAG,"Permissions Granted Is: "+arePermissionsGranted(DataFolder));

        DataTree = DocumentFile.fromTreeUri(pluginContext, Uri.parse(DataFolder));

        DataDownloads = DataTree.createDirectory("ATAK_Downloads");
        DataResources = DataTree.createDirectory("Resources");

        DocumentFile cameraIconF = DataResources.createFile("image/png","wdlnCamera.png");
        //File cameraIconF = new File(cif.getUri());
        //cameraIconF = new File(resourcesDirectory,"wdlnCamera.png")*/

       // cameraIconF = new File(resourcesDirectory,"wdlnCamera.png");

        if(!cameraIconF.exists())
        {
            Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(),R.drawable.wdln_48x48);
            try {
                //ParcelFileDescriptor pfd = pluginContext.getContentResolver().openFileDescriptor(cameraIconF.getUri(),"w");

                FileOutputStream outStream;

                outStream = new FileOutputStream(cameraIconF);

                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream);

                outStream.flush();

                outStream.close();

            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }

        cameraIcon = new Icon(cameraIconF.getAbsolutePath());

        if(dmanager == null)
        {
            dmanager = (DownloadManager) aa.getSystemService(context.DOWNLOAD_SERVICE);
        }

        if(nodeMarkers == null)
        {
            nodeMarkers = new Vector<Marker>();
        }

        if(mediaMarkers == null)
        {
            mediaMarkers = new Vector<Marker>();
        }

        if(wdlngroup == null)
        {
            wdlngroup = new DefaultMapGroup("WDLN");
            MapGroup _mapGroup = getMapView().getRootGroup();
            _mapGroup.addGroup(wdlngroup);
        }

        wdlnreceiver = new WDLNReceiver(this);
        AtakBroadcast.DocumentedIntentFilter f = new AtakBroadcast.DocumentedIntentFilter();
        f.addAction(wdlnreceiver.WDLN_TEST, "TEST");
        f.addAction("com.atakmap.maps.images.DISPLAY","Intercept Display Intent");
        f.addAction(wdlnreceiver.WDLN_VIEW, "Fired after the quick-pic has been received by the image drop-down", new DocumentedExtra[]{new DocumentedExtra("SenderCallsign", "The callsign of the sender", false, String.class), new DocumentedExtra("MissionPackageManifest", "Mission package manifest containing quick-pic", false, MissionPackageManifest.class), new DocumentedExtra("NotificationId", "ID of MP received notification", false, Integer.class)});
        AtakBroadcast.getInstance().registerReceiver(wdlnreceiver,f);

        Button refreshButton = (Button) templateView.findViewById(R.id.refreshButton);
        refreshButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                refreshContent();
            }
        });

        Button uploadButton = (Button) templateView.findViewById(R.id.uploadButton);
        uploadButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                uploadContent();
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
            lManager = new LayerManager(wdlnDirectory);
        }

        displayFile("blarg");
    }

    public void refreshContent()
    {
        if(nManager != null)
        {
            nManager.requestRefresh();
        }
    }

    public void updateContent()
    {
        if(nManager != null)
        {
            Log.d(TAG,"Requesting Network Manager Update");
            nManager.requestUpdate();
        }
    }

    public void displayFile(String file)
    {
        Intent customdialog = new Intent(pluginContext, WDLNContentViewActivity.class);

        Bundle b = new Bundle();

        b.putString("file", file);

        customdialog.putExtras(b);

        pluginActivity.startActivity(customdialog);
    }

    public void uploadContent()
    {
        Intent customdialog = new Intent(pluginContext, UploadChooserActivity.class);

        Bundle b = new Bundle();

        if(validIPs==null)
        {
            b.putStringArrayList("ips", new ArrayList<String>());
        }
        else
        {
            b.putStringArrayList("ips", new ArrayList<String>(validIPs));
        }

        customdialog.putExtras(b);

        pluginActivity.startActivity(customdialog);
        /*final CharSequence[] options = {"Images", "Videos", "Cancel"};
        AlertDialog.Builder builder = new AlertDialog.Builder(pluginActivity);
        builder.setTitle("Select From...");
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                if (options[item].equals("Images")) {
                    Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                    (pluginActivity).startActivityForResult(intent, 1);
                } else if (options[item].equals("Videos")) {
                    Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
                    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                    (pluginActivity).startActivityForResult(intent, 1);
                } else if (options[item].equals("Cancel")) {
                    dialog.dismiss();
                }
                dialog.dismiss();
            }
        });
        builder.show();*/
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

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        Vector<DownloadReference> mediareferences = new Vector<DownloadReference>();

        for(int i=0;i<downloadReferences.size();i++)
        {
            DownloadReference dr = downloadReferences.get(i);

            if(dr.GetIsLocal() && !dr.GetIsDownloadInProgress())
            {
                String file = dr.GetURL();
                boolean alreadyupdated = false;

                //check for geotagging
                if(file.endsWith("jpg") || file.endsWith("png") || file.endsWith("mp4") || file.endsWith("gif") ||
                        file.endsWith("webp") || file.endsWith("bmp") || file.endsWith("3gp") || file.endsWith("ts") ||
                        file.endsWith("webm"))
                {
                    FileInputStream inputStream = null;

                    try
                    {
                        inputStream = new FileInputStream(dr.GetURL());

                        try
                        {
                            Log.d(TAG, "Checking Metadata for: " + dr.GetURL() + " :: " + inputStream.getFD());
                            retriever.setDataSource(inputStream.getFD());

                            if (retriever.extractMetadata(METADATA_KEY_LOCATION) != null)
                            {

                                if(dr.SetLocation(retriever.extractMetadata(METADATA_KEY_LOCATION)))
                                {
                                    Log.d(TAG, "Location :: " + dr.GetLocation());
                                    mediareferences.add(dr);
                                    alreadyupdated = true;
                                }
                            }

                        } catch (Exception e)
                        {
                            Log.d(TAG, "Exception : " + e.getMessage());
                        } finally
                        {
                            if (retriever != null) {
                                retriever.release();
                            }
                        }

                    } catch (FileNotFoundException e)
                    {
                        Log.d(TAG, "Exception : " + e.getMessage());
                    } catch (IOException e)
                    {
                        Log.d(TAG, "Exception : " + e.getMessage());
                    } finally {
                        if (inputStream != null)
                        {
                            try
                            {
                                inputStream.close();
                            } catch (IOException e)
                            {
                                e.printStackTrace();
                            }
                        }
                    }
                }

                if(!alreadyupdated && (file.endsWith("tif") || file.endsWith("TIF")))
                {
                    Log.d(TAG, "Checking TIFF Metadata for: " + dr.GetURL());
                    TiffImageMetadata exif = ExifHelper.getExifMetadata(new File(FileSystemUtils.sanitizeWithSpacesAndSlashes(dr.GetURL())));
                    Log.d(TAG,"Got TIFF Metadata");
                    GeoPoint gp = exif != null ? ExifHelper.getLocation(exif) : null;
                    if (gp != null) {
                        Log.d(TAG, "Location 2: " + gp);
                        dr.SetLocation(gp);
                        Log.d(TAG, "Location :: " + dr.GetLocation());
                        mediareferences.add(dr);
                    }
                }
            }

            newTableRow(dr.GetName(), i,downloadReferences.get(i));

            if(dr.IsLayer() && !dr.GetIsDownloadInProgress())
            {
                Log.d(TAG,"Adding File To Layers");
                lManager.AddLayer(dr);
            }
        }

        retriever.release();

        tableLayout.invalidate();

        UpdateMediaLocations(mediareferences);
    }

    public void UpdateMediaLocations(Vector<DownloadReference> nodes)
    {
        MapGroup _mapGroup = getMapView().getRootGroup()
                .findMapGroup("WDLN");

        Vector<Marker> toRemove = new Vector<Marker>();

        Log.d(TAG, "HERE: "+mediaMarkers.size());

        //remove markers that didn't get updated
        for(int i=0; i<mediaMarkers.size();i++)
        {
            boolean found = false;
            Marker m = mediaMarkers.get(i);

            Log.d(TAG, "Considering: " + m.getMetaString("ID","") +" "+ m.getTitle());

            for(int j=0;j<nodes.size();j++)
            {
                DownloadReference n = nodes.get(j);
                if(m.getMetaString("ID","").equals(n.GetURL()) && m.getTitle().equals(n.GetName()))
                {
                    found = true;
                    Log.d(TAG, "Found Marker: " + m.getMetaString("ID","") +" "+ m.getTitle());
                }
            }
            if(!found)
            {
                Log.d(TAG, "Did Not Find Marker: " + m.getMetaString("ID","") +" "+ m.getTitle());
                _mapGroup.removeItem(m);
                toRemove.add(m);
            }

        }

        while(!toRemove.isEmpty())
        {
            Log.d(TAG, "Removing Marker");
            mediaMarkers.remove(toRemove.firstElement());
            toRemove.remove(0);
        }

        for(int i=0; i<nodes.size();i++)
        {
            DownloadReference n = nodes.get(i);

            //check if marker exists
            boolean exists = false;
            for (int j = 0; j < mediaMarkers.size() && !exists; j++)
            {
                Marker m = mediaMarkers.get(j);

                if (m.getMetaString("ID", "").equals(n.GetURL()) && m.getTitle().equals(n.GetName()))
                {
                    if (!n.HasLocation())
                    {
                        _mapGroup.removeItem(m);
                    }
                    else
                    {
                        if(n.LocationChanged(m.getPoint()))
                        {
                            m.setPoint(n.GetLocation());
                        }
                        exists = true;
                    }
                }
            }

            if (!exists && n.GetLocation() != null) {
                Log.d(TAG, "Creating Marker: " + n.GetName());
                //make a marker
                Marker m = new Marker(n.GetLocation(), n.GetName());
                //m.setVisible(true);
                //m.setTitle("testm");
                //getMapView().getRootGroup().addItem(testm);;
                //Marker m = new Marker(getMapView().getPointWithElevation(), UUID
                //        .randomUUID().toString());
                Log.d(TAG, "creating a new unit marker for: " + m.getUID());
                m.setType("b-i-x-i");
                //m.setType("a-f-G-E");
                m.getUID();
                //m.setMetaBoolean("readiness", true);
                //m.setMetaBoolean("archive", true);
                //m.setMetaString("how", "h-g-i-g-o");
                //m.setMetaBoolean("editable", true);
                m.setMetaBoolean("movable", false);
                //m.setMetaBoolean("removable", true);
                //m.setMetaString("entry", "user");
                //m.setMetaString("callsign", "Test Marker");
                m.setMetaString("remarks","This marker represents a geolocated "+n.HumanReadableFileDescription()+" on your device.\n\nClicking the camera icon will attempt to open the file with an appropriate application.\n\nAutogenerated by WildfireDLN");
                //Log.d(TAG, "Icon Before: " + m.getIcon());
                m.setIcon(cameraIcon);
                //Log.d(TAG, "Icon After: " + m.getIcon());
                m.setMetaString("ID", n.GetURL());
                m.setTitle(n.GetName());

                //m.setSummary("This marker was autogenerated by the Wildfire DLN plugin.");
                _mapGroup.addItem(m);

                /*Intent intent = new Intent("com.atakmap.android.images.NEW_IMAGE");
                intent.putExtra("path", n.GetLocation());
                intent.putExtra("uid", m.getUID());
                AtakBroadcast.getInstance().sendBroadcast(intent);*/

                /*Log.d(TAG, "Testing Broadcast " + wdlnreceiver.WDLN_TEST);
                intent = new Intent(wdlnreceiver.WDLN_TEST);
                AtakBroadcast.getInstance().sendBroadcast(intent);*/


                /*intent = (new Intent("com.atakmap.maps.images.DISPLAY")).putExtra("uid", m.getUID()).putExtra("UseMissionPackageToSend", false).putExtra("onReceiveAction", "com.atakmap.android.wildfiredln.WDLN_VIEW");
                AtakBroadcast.getInstance().sendBroadcast(intent);
                intent = new Intent("com.atakmap.android.maps.COT_RECENTLYPLACED");
                intent.putExtra("uid", m.getUID());
                AtakBroadcast.getInstance().sendBroadcast(intent);*/


                /*m.persist(getMapView().getMapEventDispatcher(), null,
                        this.getClass());*/ //Not Needed???

                mediaMarkers.add(m);

                //VideoMapComponent vid = new VideoMapComponent();

                /*Intent new_cot_intent = new Intent();
                new_cot_intent.setAction("com.atakmap.android.maps.COT_PLACED");
                new_cot_intent.putExtra("uid", m.getUID());
                com.atakmap.android.ipc.AtakBroadcast.getInstance().sendBroadcast(
                        new_cot_intent);*/
            }
        }
    }

    public void DisplayByUID(String uid)
    {
        for(int i=0; i<mediaMarkers.size();i++)
        {
            Marker m = mediaMarkers.get(i);
            if(m.getUID().equals(uid))
            {
                Log.d(TAG,"Found marker With UID "+m.getUID());

                for(int j=0;j<downloadReferences.size();j++) {
                    DownloadReference dr = downloadReferences.get(j);

                    if (m.getMetaString("ID", "").equals(dr.GetURL()) && m.getTitle().equals(dr.GetName()))
                    {
                        Log.d(TAG,"Found markers Download Reference "+dr.GetName());

                        dr.SetParent(this);
                        dr.StartDownload(dmanager,null);
                        break;
                    }
                }
                break;
            }
        }
    }

    public void UpdateLocations(Vector<NodeReference> nodes)
    {
        MapGroup _mapGroup = getMapView().getRootGroup()
                .findMapGroup("WDLN");

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

    public void UpdateIPs(Vector<String> ips)
    {
        validIPs = ips;
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

                if(dr.HasLocation())
                {
                    final GeoPoint gp = dr.GetLocation();
                    Log.d(TAG,"Setting New View Location: "+gp.toString());
                    centerButton.setOnClickListener(new View.OnClickListener()
                    {
                        public void onClick(View v)
                        {
                            getMapView().updateView(gp.getLatitude(),gp.getLongitude(),getMapView().getMapScale(),0,0,true);
                        }
                    });
                }
                else
                {
                    Log.d(TAG,"Resource does not have a location");
                }
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
            if (dr.IsLayer() || (!dr.IsLayer() && dr.HasLocation()))
            {
                row.addView(centerButton);
            }
        }
        //row.addView(viewButton);
        tableLayout.addView(row,count);

        final CheckBox checkBox = (CheckBox)templateView.findViewById(R.id.autoBox);

        if(checkBox.isChecked() && !dr.GetIsLocal() && !dr.GetIsDownloadInProgress())
        {
            DownloadByID(id, dlButton);
        }
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

                if(dlButton != null)
                {
                    dlButton.setImageResource(R.drawable.eye_closed_48x48);
                }
            }
            else
            {
                lManager.SetLayerVisibility(dr.GetName(),true);

                if(dlButton != null)
                {
                    dlButton.setImageResource(R.drawable.eye_open_48x48);
                }
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

    public Activity GetActivity(){return pluginActivity;}

    public Context GetApplicationContext(){return pluginActivity.getApplicationContext();}

    private boolean arePermissionsGranted(String uriString)
    {
        Log.d(TAG,pluginActivity.getApplicationContext().toString());
        List<UriPermission> persistedUriPermissions = pluginActivity.getApplicationContext().getContentResolver().getPersistedUriPermissions();

        Log.d(TAG,"There were "+persistedUriPermissions.size()+" persisted permissions");

        for(UriPermission p : persistedUriPermissions)
        {
            String ps = p.toString();

            Log.d(TAG,uriString+" : "+ps);

            if(uriString.equals(ps) && p.isWritePermission() && p.isReadPermission())
            {
                Log.d(TAG,"MATCHED!");
                return true;
            }
        }

        return false;
    }

    private String getSharedDirectory()
    {
        File savedir = new File(pluginContext.getExternalFilesDir(null),pluginContext.getString(R.string.preference_file_key));



        if(savedir.exists())
        {
            Log.d(TAG,"Reading Permissions from "+savedir.getAbsolutePath());

            BufferedReader reader = null;
            StringBuilder sb = new StringBuilder();

            try {
                FileInputStream fis = new FileInputStream(savedir);

                reader = new BufferedReader(new InputStreamReader(fis));
                String line = null;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            } catch (FileNotFoundException e)
            {
                e.printStackTrace();
                return "";
            } catch (IOException e) {
                e.printStackTrace();
                return "";
            } finally
            {
                if(reader != null)
                {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            return sb.toString();
        }

        Log.d(TAG,"Permissions Did Not Exist at "+savedir.getAbsolutePath());

        return "";
    }

    public File getDownloadsDirectory()
    {
        File internalDirectory = pluginContext.getExternalFilesDir(null);

        File fileDirectory = new File(internalDirectory,"/ATAK_Downloads");

        return fileDirectory;
    }
}
