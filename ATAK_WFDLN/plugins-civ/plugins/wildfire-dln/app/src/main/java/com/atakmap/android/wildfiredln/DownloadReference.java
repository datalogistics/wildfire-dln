package com.atakmap.android.wildfiredln;

import android.Manifest;
import android.app.DownloadManager;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
//import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.ImageButton;
import com.atakmap.android.wildfiredln.plugin.R;
import com.atakmap.coremap.maps.coords.GeoPoint;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
//import org.json.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;


import static java.lang.Thread.sleep;

import androidx.core.content.FileProvider;

public class DownloadReference
{
    private String url;
    private String name;
    private GeoPoint location = null;
    private long size;
    private Date timestamp;
    private WildfireDLN parent = null;
    private DownloadReference self;
    private boolean downloadInProgress = false;
    private boolean isLocal = false;
    public int progress;
    DownloadItemTask dtask = null;
    Thread dthread = null;
    Handler mHandler;

    private static String regex = "\\s*(\\d+\\-\\w+\\-\\d+ \\d+\\:\\d+)\\s+(\\d+)\\s*";
    private static Pattern pattern = Pattern.compile(regex);
    private static SimpleDateFormat dateformat = new SimpleDateFormat("dd-MMM-yyyy HH:mm"); //03-Nov-2021 20:27

    public static final String TAG = "DownloadReference";

    public DownloadReference(String url, String name, long size, Date timestamp)
    {
        self = this;
        this.url = url;
        this.name = name;
        this.size = size;
        this.timestamp = timestamp;
    }

    public void SetIsLocal(boolean local)
    {
        isLocal = local;
    }

    public boolean GetIsLocal()
    {
        return isLocal;
    }

    public boolean GetIsDownloadInProgress()
    {
        return downloadInProgress;
    }

    public void SetParent(WildfireDLN up)
    {
        parent = up;
        mHandler = new Handler(parent.GetContext().getMainLooper())
        {
            @Override
            public void handleMessage(Message inputMessage)
            {
                self.handleMessage(inputMessage);
            }
        };
    }

    private void handleMessage(Message inputMessage)
    {
        if(inputMessage.what == 0)//progress update
        {
            parent.UpdateDownloadProgress(self,inputMessage.arg1);
            progress = inputMessage.arg1;
        }
        else if(inputMessage.what == 1)//download complete
        {
            DownloadComplete((String)inputMessage.obj);
        }

        if(inputMessage.arg1 == 1)
        {
            Log.d(TAG,"Requesting File Update");
            parent.updateContent();
        }
    }

    public String GetName()
    {
        return name;
    }

    public String GetURL()
    {
        return url;
    }

    @Override
    public boolean equals(Object obj)
    {
        if(obj == null)
        {
            return false;
        }

        if(!DownloadReference.class.isAssignableFrom(obj.getClass()))
        {
            return false;
        }

        final DownloadReference dr = (DownloadReference)obj;

        /*if(!url.equals(dr.url))
        {
            return false;
        }*/

        if(!name.equals(dr.name))
        {
            return false;
        }

        if(!(size == dr.size))
        {
            Log.d(TAG,"File Sizes for downloads "+name+"("+url+"), "+dr.name+"("+dr.url+") are not equal: "+size+", "+dr.size);
            return false;
        }

        /*if(!timestamp.equals(dr.timestamp))
        {
            return false;
        }*/

        return true;
    }

    public void StartDownload(DownloadManager dmanager, final ImageButton ib)
    {
        if(!isLocal)//remote file to be downloaded
        {
            if (!downloadInProgress)
            {
                if(ib != null)
                {
                    ib.setImageResource(R.drawable.cancel_48x48);
                }
                downloadInProgress = true;
                Log.d(TAG,url);
                Log.d(TAG,name);
                Log.d(TAG,dmanager.toString());
                Log.d(TAG,mHandler.toString());
                Log.d(TAG,"State: "+Environment.getExternalStorageState());
                dtask = new DownloadItemTask(url, name, parent.getDownloadsDirectory(), dmanager, mHandler);
                Log.d(TAG,"Here");
                dthread = new Thread(dtask);
                dthread.start();
            }
            else
            {

                if (dtask != null)
                {
                    dtask.setCancelled();
                    dtask = null;
                }
            }
        }
        else//open file on device
        {
            Log.d(TAG,name);

            //Uri uri=Uri.fromFile(new File(url));
            //Log.d(TAG,uri.toString());
//            File temp = new File(url);
//            Uri uri = FileProvider.getUriForFile(parent.GetContext(),"com.atakmap.android.wildfiredln.fileprovider",temp);
//            Log.d(TAG,uri.toString());

            //Uri uri = Uri.parse("content://"+url);

            /*File internalDirectory = parent.GetContext().getExternalFilesDir(null);
            File wdlnDirectory = new File(internalDirectory,"/WDLN");
            File resourcesDirectory = new File(wdlnDirectory,"/Resources");
            Uri uri = FileProvider.getUriForFile(parent.GetContext(),"com.atakmap.android.wildfiredln.plugin.fileprovider",new File(resourcesDirectory,"wdlnCamera.png"));
            mtype = "image/png";



            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.setDataAndType(uri,mtype);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);

            List<ResolveInfo> resInfoList = parent.GetContext().getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
            for (ResolveInfo resolveInfo : resInfoList) {
                String packageName = resolveInfo.activityInfo.packageName;
                Log.d(TAG,packageName);
                parent.GetContext().grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                //parent.GetActivity().grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }

            parent.GetContext().startActivity(intent);
            Log.d(TAG,"Launched Activity");*/
            parent.displayFile(name);
        }
    }

    public String HumanReadableFileDescription()
    {
        String mtype = "file";

        //images
        if(url.endsWith("jpg"))
        {
            mtype = "image";
        }
        else if(url.endsWith("png"))
        {
            mtype = "image";
        }
        else if(url.endsWith("gif"))
        {
            mtype = "image";
        }
        else if(url.endsWith("bmp"))
        {
            mtype = "image";
        }
        else if(url.endsWith("webp"))
        {
            mtype = "image";
        }
        //videos
        else if(url.endsWith("3gp"))
        {
            mtype = "video";
        }
        else if(url.endsWith("mp4"))
        {
            mtype = "video";
        }
        else if(url.endsWith("ts"))
        {
            mtype = "video";
        }
        else if(url.endsWith("webm"))
        {
            mtype = "video";
        }

        return mtype;
    }

    public boolean IsLayer()
    {
        if(!GetIsLocal())
        {
            return false;
        }

        boolean islayer = false;

        islayer |= GetURL().endsWith("tif");


        return islayer;
    }

    public void DownloadComplete(String result)
    {
        downloadInProgress = false;

        if(parent != null)
        {
            if(result.equals("cancelled"))
            {
                parent.UpdateDownloadProgress(self,-1);
            }
            else if(result.equals("failed"))
            {
                parent.UpdateDownloadProgress(self,-2);
            }
            else
            {
                this.isLocal = true;
                this.url = result;
                parent.UpdateDownloadProgress(self,-3);
            }

        }
    }

    public DownloadReference copy()
    {
        DownloadReference c = new DownloadReference(url,name,size,timestamp);
        c.isLocal = isLocal;

        return c;
    }

    public class DownloadItemTask implements Runnable
    {

        //private static final String TAG = Constants.TAG + "nstask";

        private DownloadManager dmanager = null;
        private Handler mHandler;
        private String url, name;
        private long downloadID=-1L;
        private boolean isCancelled = false;
        private File downloadsDirectory;

        public DownloadItemTask(String url, String name, File downloadsDirectory, DownloadManager dm, Handler mHandler)
        {
            dmanager = dm;
            this.url = url;
            this.name = name;
            this.mHandler = mHandler;
            this.downloadsDirectory = downloadsDirectory;
        }

        public void setCancelled()
        {
            isCancelled = true;
        }

        @Override
        public void run() {
            String result = "";
            String tempname = name + ".wdln";

            Uri uri = Uri.parse(url);

            //File fileDirectory = parent.GetContext().getExternalMediaDirs(Environment.DIRECTORY_DOWNLOADS);
            File fileDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

            //File fileDirectory = new File(filepath,"/ATAK_Downloads");

            if (!fileDirectory.exists()) {
                Log.d(TAG,"THE DOWNLOAD DIRECTORY DOESN'T EXIST?!?!?!");
                fileDirectory.mkdir();
            }

            Log.d(TAG, url);
            Log.d(TAG, uri.toString());
            Log.d(TAG, name);
            Log.d(TAG, dmanager.toString());
            Log.d(TAG, mHandler.toString());
            Log.d(TAG, "Path: " + fileDirectory);
            Log.d(TAG, "State: " + Environment.getExternalStorageState());

            /*if (ContextCompat.checkSelfPermission(parent, Manifest.permission.WRITE_CALENDAR)
                    != PackageManager.PERMISSION_GRANTED) {
                // Permission is not granted
            }*/

            File tempfile = new File(fileDirectory,tempname);

            Log.d(TAG,"File "+tempfile.getAbsolutePath()+" exists? "+tempfile.exists());
            if(tempfile.exists())
            {
                Log.d(TAG,"Removing Existing File: "+tempfile.getAbsolutePath());
                tempfile.delete();
            }

            DownloadManager.Request request = new DownloadManager.Request(uri)
                    .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI)
                    .setTitle(name)
                    .setDescription("Wildfire-DLN File Transfer")
                    //.setDestinationInExternalFilesDir(parent.GetContext(),Environment.DIRECTORY_DOWNLOADS,name);
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,tempname);
                    //.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS.toString(),name);

            //Log.d(TAG,"Request: "+request);

            File exists = new File(downloadsDirectory,tempname);
            File newfile = new File(downloadsDirectory,name);
            //Log.d(TAG,exists.getAbsolutePath() +": "+ exists.isFile());

            downloadID = dmanager.enqueue(request);

            Log.d(TAG,tempfile.getAbsolutePath());
            Log.d(TAG,"HERE!!!!!!!!!!!!");


            //ContentResolver blarg = (parent.GetContext().getApplicationContext()).getContentResolver();
            //Log.d(TAG,">>"+blarg.toString());

            boolean running = true;
            boolean shouldupdate = false;

            while(running)
            {
                if(isCancelled)
                {
                    dmanager.remove(downloadID);
                    result = "cancelled";
                    running = false;
                }
                else
                {


                    Cursor c = dmanager.query(new DownloadManager.Query().setFilterById(downloadID));

                    if (c == null)
                    {
                        running = false;
                        result = "Download Missing";
                        Log.d(TAG, "No Such Download Exists");
                    }
                    else
                    {
                        c.moveToFirst();

                        int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));

                        if (status == DownloadManager.STATUS_SUCCESSFUL)
                        {
                            result = dmanager.getUriForDownloadedFile(downloadID).getPath();
                            Log.d(TAG,result);
                            running = false;

                            //copy downloaded file to plugin downloads folder
                            if(exists.isFile())
                            {
                                exists.delete();
                                Log.d(TAG, "Removing Old partial File");
                            }

                            if(newfile.isFile())
                            {
                                newfile.delete();
                                Log.d(TAG, "Removing Old File");
                                shouldupdate = true;
                            }

                            Log.d(TAG,"Copying "+tempfile.length()+" bytes");

                            try
                            {
                                InputStream in = new FileInputStream(tempfile);
                                OutputStream out = new FileOutputStream(exists);

                                byte[] buffer = new byte[1024];
                                int read;
                                while ((read = in.read(buffer)) != -1) {
                                    out.write(buffer, 0, read);
                                }
                                in.close();
                                in = null;

                                // write the output file
                                out.flush();
                                out.close();
                                out = null;

                                // delete the original file

                                Log.d(TAG,"Moved "+tempfile.getAbsolutePath()+" to "+exists.getAbsolutePath());
                                Log.d(TAG,"Final File Contained "+exists.length()+"/"+tempfile.length()+" bytes");
                                tempfile.delete();

                                exists.renameTo(newfile);

                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        else if (status == DownloadManager.STATUS_RUNNING)
                        {
                            Long downloaded = c.getLong(c.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                            int progress = (int) (downloaded * 100 / self.size);
                            Log.d(TAG, "Progress is: " + progress + "%");

                            Message m = mHandler.obtainMessage(0, progress,0,null);
                            m.sendToTarget();
                        }
                        else if (status == DownloadManager.STATUS_FAILED)
                        {
                            Log.d(TAG,"Failed to download "+uri);
                            result = "failed";
                            running = false;
                        }
                    }

                    try
                    {
                        sleep(250);
                    }
                    catch (InterruptedException e)
                    {
                        e.printStackTrace();
                    }
                }
            }

            int arg1 = shouldupdate ? 1 : 0;
            Message m = mHandler.obtainMessage(1, arg1, 0, result);
            m.sendToTarget();
        }
    }

    static Vector<DownloadReference> DownloadParser(String host, String string2parse)
    {
        Vector<DownloadReference> references = new Vector<DownloadReference>();

        //OLD JSON method for exnodes parsing
        /*try
        {
            JSONArray arr = new JSONArray(string2parse);

            for(int i=0;i<arr.length();i++)
            {
                JSONObject obj = arr.getJSONObject(i);
                String name = obj.getString("name");
                Long size = obj.getLong("size");
                String url = host + "/web/" + name;
                Date timestamp = new Date(obj.getLong("created"));

                DownloadReference d  = new DownloadReference(url,name,size,timestamp);
                references.add(d);
            }


        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }*/

        Document doc = Jsoup.parse(string2parse);
        Elements titles = doc.select("title");

        boolean indexcheck = false;
        for(Element e : titles)
        {
            if(e.hasText() && e.text().contains("Index of "))
            {
                indexcheck = true;
                break;
            }
        }

        Elements links = doc.select("a[href]");

        try {
            for (Element e : links) {
                String name = e.text();
                //String between = ((TextNode) e.childNode(1)).getWholeText();
                String between = e.nextSibling().toString();
                Log.d(TAG, "Between text for " + name + " was " + between);

                Matcher matcher = pattern.matcher(between);

                if(!matcher.lookingAt())
                {
                    Log.d(TAG,"Pattern <"+between+"> failed to match");
                    continue;
                }

                Log.d(TAG,"group 1: <"+matcher.group(1)+">");
                Log.d(TAG,"group 2: <"+matcher.group(2)+">");

                Long size = Long.parseLong(matcher.group(2));
                String url = host + "/web/" + name;
                Date timestamp = dateformat.parse(matcher.group(1));

                DownloadReference d = new DownloadReference(url, name, size, timestamp);
                references.add(d);
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

        //Log.d(TAG, Log.getStackTraceString(e));

        return references;
    }

    static void AddUnique(Vector<DownloadReference> master, Vector<DownloadReference> slave)
    {
        if(master != null && slave != null)
        {
            for (int i = 0; i < slave.size(); i++)
            {
                if (!master.contains(slave.get(i)))
                {
                    master.add(slave.get(i));
                }
            }
        }
    }

    static Vector<DownloadReference> UpdatePreserveExisting(Vector<DownloadReference> master, Vector<DownloadReference> slave)
    {
        Vector<DownloadReference> newlist = new Vector<DownloadReference>();

        for (int i = 0; i < master.size(); i++)
        {
            if (slave.contains(master.get(i)))
            {
                DownloadReference d = master.get(i);

                if(!d.GetIsDownloadInProgress())
                {
                    DownloadReference ds = slave.get(slave.indexOf(d));

                    d.SetIsLocal(ds.isLocal);
                    d.url = ds.url;
                }

                newlist.add(d);
            }
        }

        for (int i = 0; i < slave.size(); i++)
        {
            if (!master.contains(slave.get(i)))
            {
                newlist.add(slave.get(i));
            }
        }

        return newlist;
    }

    public boolean HasLocation()
    {
        return location != null;
    }

    public boolean SetLocation(String l)
    {
        int splitid = Math.max(l.lastIndexOf('+'),l.lastIndexOf('-'));
        double lat = Double.parseDouble(l.substring(0,splitid));
        double lon = Double.parseDouble(l.substring(splitid));

        Log.d(TAG,"Reference Lat/Lon is "+lat+','+lon);

        if(Double.isNaN(lat) || Double.isNaN(lon))
        {
            return false;
        }

        location = new GeoPoint(lat,lon);

        return true;
    }

    public void SetLocation(GeoPoint l)
    {
        location = l;
    }

    public GeoPoint GetLocation()
    {
        return location;
    }

    public boolean LocationChanged(GeoPoint p)
    {
        return p.getLatitude() != location.getLatitude() || p.getLongitude() != location.getLongitude();
    }
}
