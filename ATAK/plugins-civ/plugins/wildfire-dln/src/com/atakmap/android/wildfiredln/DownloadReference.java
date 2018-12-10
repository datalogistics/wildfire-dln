package com.atakmap.android.wildfiredln;

import android.app.DownloadManager;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.ImageButton;
import com.atakmap.android.wildfiredln.plugin.R;

import java.io.File;
import java.util.Date;
import java.util.Vector;
import org.json.*;

import static java.lang.Thread.sleep;

public class DownloadReference
{
    private String url;
    private String name;
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
                dtask = new DownloadItemTask(url, name, dmanager, mHandler);
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
            Log.d(TAG,url);
            String mtype = "*/*";

            //images
            if(url.endsWith("jpg"))
            {
                mtype = "image/jpeg";
            }
            else if(url.endsWith("png"))
            {
                mtype = "image/png";
            }
            else if(url.endsWith("gif"))
            {
                mtype = "image/gif";
            }
            else if(url.endsWith("bmp"))
            {
                mtype = "image/bmp";
            }
            else if(url.endsWith("webp"))
            {
                mtype = "image/webp";
            }
            //videos
            else if(url.endsWith("3gp"))
            {
                mtype = "video/3gp";
            }
            else if(url.endsWith("mp4"))
            {
                mtype = "video/mp4";
            }
            else if(url.endsWith("ts"))
            {
                mtype = "video/ts";
            }
            else if(url.endsWith("webm"))
            {
                mtype = "video/webm";
            }

            Uri uri=Uri.fromFile(new File(url));
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.setDataAndType(uri,mtype);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            parent.GetContext().startActivity(intent);
        }
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

        public DownloadItemTask(String url, String name, DownloadManager dm, Handler mHandler)
        {
            dmanager = dm;
            this.url = url;
            this.name = name;
            this.mHandler = mHandler;
        }

        public void setCancelled()
        {
            isCancelled = true;
        }

        @Override
        public void run()
        {
            String result = "";


            Uri uri=Uri.parse(url);


            String filepath = Environment.getExternalStorageDirectory().toString();
            File fileDirectory = new File(filepath+"/ATAK_Downloads");
            if(!fileDirectory.exists())
            {
                fileDirectory.mkdir();
            }



            downloadID = dmanager.enqueue(new DownloadManager.Request(uri)
                    .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI)
                    .setTitle(name)
                    .setDescription("Wildfire-DLN File Transfer")
                    .setDestinationInExternalPublicDir("ATAK_Downloads",
                            name));

            Log.d(TAG,fileDirectory.getAbsolutePath());

            boolean running = true;

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

            Message m = mHandler.obtainMessage(1, 0,0,result);
            m.sendToTarget();
        }
    }

    static Vector<DownloadReference> DownloadParser(String host, String string2parse)
    {
        Vector<DownloadReference> references = new Vector<DownloadReference>();

        try
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
}
