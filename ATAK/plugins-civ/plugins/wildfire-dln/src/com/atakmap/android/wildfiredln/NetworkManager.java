package com.atakmap.android.wildfiredln;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.Vector;

public class NetworkManager implements Runnable
{
    public static final String TAG = "NetworkManager";
    private WildfireDLN parent;
    private WeakReference<Context> mContextRef;
    private WifiManager wm;
    private Handler mHandler;
    private NetworkManager self;
    private boolean refreshRequested = false;

    public NetworkManager(WildfireDLN up, Context pluginContext)
    {
        self = this;
        parent = up;

        mContextRef = new WeakReference<Context>(pluginContext);
        wm = (WifiManager) pluginContext.getSystemService(Context.WIFI_SERVICE);

        mHandler = new Handler(parent.GetContext().getMainLooper())
        {
            @Override
            public void handleMessage(Message inputMessage)
            {
                self.handleMessage(inputMessage);
            }
        };
    }

    public void requestRefresh()
    {
        refreshRequested = true;
    }

    private void handleMessage(Message inputMessage)
    {
        if(inputMessage.what == 0)//refresh
        {
            Log.d(TAG, "Updating Download GUI");
            parent.UpdateReferences((Vector<DownloadReference>)inputMessage.obj);
        }
        else if(inputMessage.what == 1)//update
        {
            Log.d(TAG, "Updating Location GUI");
            parent.UpdateLocations((Vector<NodeReference>)inputMessage.obj);
        }
        else if(inputMessage.what == 2)//toggle refresh
        {
            if(inputMessage.arg1 == 0)
            {
                parent.toggleProgress(View.INVISIBLE);
            }
            else
            {
                parent.toggleProgress(View.VISIBLE);
            }
        }
        else if(inputMessage.what == 3)//update ips
        {
            Log.d(TAG, "Updating IP List");
            parent.UpdateIPs((Vector<String>)inputMessage.obj);
        }
    }

    private void toggleProgress(boolean state)
    {
        int v = state ? 1 : 0;

        Message m = mHandler.obtainMessage(2,v,0,null);
        m.sendToTarget();
    }

    private void updateReferences(Vector<DownloadReference> references)
    {
        Vector<DownloadReference> copies = new Vector<DownloadReference>();

        for(int i=0;i<references.size();i++)
        {
            copies.add(references.get(i).copy());
        }

        Message m = mHandler.obtainMessage(0,0,0,copies);
        m.sendToTarget();
    }

    private void updateNodes(Vector<NodeReference> nodes)
    {
        Vector<NodeReference> copies = new Vector<NodeReference>();

        for(int i=0;i<nodes.size();i++)
        {
            copies.add(nodes.get(i).copy());
        }

        Message m = mHandler.obtainMessage(1,0,0,copies);
        m.sendToTarget();
    }

    private void updateIPs(Vector<String> ips)
    {
        Vector<String> copies = new Vector<String>();

        for(int i=0;i<ips.size();i++)
        {
            copies.add(ips.get(i));
        }

        Message m = mHandler.obtainMessage(3,0,0,copies);
        m.sendToTarget();
    }

    @Override
    public void run()
    {
        boolean running = true;

        String state = "refresh";
        String ipString = "";
        String prefix = "";
        int waitcounter = 0;
        boolean connected = false;

        Vector<String> validIPs = new Vector<String>();
        Vector<DownloadReference> masterReferences = new Vector<DownloadReference>();

        while (running)
        {
            Context context = mContextRef.get();

            if (context != null)
            {

                ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

                if(wm.isWifiEnabled() && activeNetwork!=null && activeNetwork.isConnected())
                {
                    WifiInfo connectionInfo = wm.getConnectionInfo();
                    int ipAddress = connectionInfo.getIpAddress();
                    String ipStringC = Formatter.formatIpAddress(ipAddress);
                    prefix = ipStringC.substring(0, ipStringC.lastIndexOf(".") + 1);

                    if (!ipStringC.equals(ipString))
                    {
                        ipString = ipStringC;

                        try
                        {
                            Thread.sleep(3000);
                        } catch (InterruptedException e)
                        {
                            e.printStackTrace();
                        }

                        state = "refresh";
                    }

                    connected = true;
                    Log.d(TAG, "WiFi is connected");
                }
                else
                {
                    connected = false;
                    Log.d(TAG, "WiFi is unconnected");
                }

                if (state.equals("refresh")) //refresh all connections
                {
                    toggleProgress(true);
                    Log.d(TAG, "Refreshing Connections");
                    validIPs.clear();

                    Vector<DownloadReference> temporaryReferences = new Vector<DownloadReference>();
                    Vector<DownloadReference> internalReferences = new Vector<DownloadReference>();
                    Vector<NodeReference> nodes = new Vector<NodeReference>();

                    if(connected)
                    {
                        Log.d(TAG, "starting threads");
                        String[] ipaddresses = new String[255];
                        Thread[] threads = new Thread[255];
                        NetworkWorker[] runners = new NetworkWorker[255];
                        String[] hnames = new String[255];
                        Boolean[] reachable = new Boolean[255];

                        for (int i = 0; i < 255; i++)
                        {
                            ipaddresses[i] = prefix + String.valueOf(i);
                            runners[i] = new NetworkWorker(ipaddresses[i]);
                            threads[i] = new Thread(runners[i]);
                            threads[i].start();
                        }

                        for (int i = 0; i < 255; i++)
                        {
                            try
                            {
                                Log.d(TAG, "Joining Thread " +i);
                                threads[i].join();

                                if (runners[i].GetReachable())
                                {
                                    Log.d(TAG, "" + ipaddresses[i] + " is reachable.");
                                }

                                reachable[i] = runners[i].GetValid();
                                hnames[i] = runners[i].GetHostName();

                                if (reachable[i])
                                {
                                    Log.d(TAG, "" + ipaddresses[i] + " is valid.");
                                    validIPs.add(ipaddresses[i]);
                                    //Log.i(TAG, "Host: " + String.valueOf(hnames[i]) + "(" + String.valueOf(ipaddresses[i]) + ") is reachable!");

                                    if (runners[i].GetReferences() != null)
                                    {
                                        DownloadReference.AddUnique(temporaryReferences, runners[i].GetReferences());
                                        NodeReference.AddUnique(nodes, runners[i].GetNodes());
                                    }
                                }
                            } catch (InterruptedException e)
                            {
                                e.printStackTrace();
                            }
                        }
                    }

                    //Scan for files stored on the device
                    String path = Environment.getExternalStorageDirectory().toString() + "/ATAK_Downloads";
                    File directory = new File(path);

                    if(!directory.exists())
                    {
                        directory.mkdirs();
                    }

                    Log.d(TAG, "Searching Directory " +path);

                    File[] internalFiles = directory.listFiles();

                    Log.d(TAG, "Files Found: " + internalFiles.length);

                    for (int i = 0; i < internalFiles.length; i++)
                    {
                        File ifile = internalFiles[i];
                        DownloadReference dref = new DownloadReference(ifile.getAbsolutePath(), ifile.getName(), ifile.length(), new Date(ifile.lastModified()));
                        dref.SetIsLocal(true);
                        internalReferences.add(dref);
                    }

                    DownloadReference.AddUnique(internalReferences, temporaryReferences);
                    masterReferences = DownloadReference.UpdatePreserveExisting(masterReferences, internalReferences);

                    Log.d(TAG, "Updating Information: " + masterReferences.size() + " files");
                    updateReferences(masterReferences);
                    Log.d(TAG, "Updating Nodes: " + nodes.size() + " found");
                    updateNodes(nodes);
                    updateIPs(validIPs);
                    toggleProgress(false);
                    refreshRequested = false;
                    state = "wait";
                    waitcounter = 5;
                }
                else if(state.equals("wait"))
                {
                    if(refreshRequested)
                    {
                        state = "refresh";
                    }
                    else
                    {
                        waitcounter -= 1;

                        if (waitcounter == 0)
                        {
                            state = "update";
                        }
                    }
                }
                else if (state.equals("update"))
                {
                    toggleProgress(true);
                    int ipcount = validIPs.size();


                    String[] ipaddresses = new String[ipcount];
                    Thread[] threads = new Thread[ipcount];
                    NetworkWorker[] runners = new NetworkWorker[ipcount];
                    String[] hnames = new String[ipcount];
                    Boolean[] reachable = new Boolean[ipcount];

                    Vector<DownloadReference> temporaryReferences = new Vector<DownloadReference>();
                    Vector<DownloadReference> internalReferences = new Vector<DownloadReference>();
                    Vector<NodeReference> nodes = new Vector<NodeReference>();

                    if(connected)
                    {
                        Log.d(TAG, "u:wifi is connected");
                        for (int i = 0; i < ipcount; i++)
                        {
                            ipaddresses[i] = validIPs.get(i);
                            runners[i] = new NetworkWorker(ipaddresses[i]);
                            threads[i] = new Thread(runners[i]);
                            threads[i].start();
                        }

                        for (int i = 0; i < ipcount; i++)
                        {
                            try
                            {
                                Log.d(TAG, "u:joining thread "+i);
                                threads[i].join();
                                reachable[i] = runners[i].GetValid();
                                hnames[i] = runners[i].GetHostName();

                                if (reachable[i])
                                {
                                    if (runners[i].GetReferences() != null)
                                    {
                                        DownloadReference.AddUnique(temporaryReferences, runners[i].GetReferences());
                                        NodeReference.AddUnique(nodes, runners[i].GetNodes());
                                    }
                                } else
                                {
                                    validIPs.remove(ipaddresses[i]);
                                }
                            } catch (InterruptedException e)
                            {
                                e.printStackTrace();
                            }
                        }
                    }

                    String path = Environment.getExternalStorageDirectory().toString() + "/ATAK_Downloads";
                    File directory = new File(path);

                    if(!directory.exists())
                    {
                        directory.mkdirs();
                    }

                    File[] internalFiles = directory.listFiles();

                    Log.d(TAG, "u:Files Found: " + internalFiles.length);

                    for (int i = 0; i < internalFiles.length; i++)
                    {
                        File ifile = internalFiles[i];
                        DownloadReference dref = new DownloadReference(ifile.getAbsolutePath(), ifile.getName(), ifile.length(), new Date(ifile.lastModified()));
                        dref.SetIsLocal(true);
                        internalReferences.add(dref);
                    }

                    DownloadReference.AddUnique(internalReferences, temporaryReferences);
                    masterReferences = DownloadReference.UpdatePreserveExisting(masterReferences, internalReferences);

                    Log.d(TAG, "u:Updating Information: " + masterReferences.size() + " files");
                    updateReferences(masterReferences);
                    Log.d(TAG, "u:Updating Nodes: " + nodes.size() + " found");
                    updateNodes(nodes);
                    updateIPs(validIPs);
                    toggleProgress(false);
                    state = "wait";
                    waitcounter = 5;
                }
            }

            try
            {
                Thread.sleep(1000);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }

        }
    }
}
