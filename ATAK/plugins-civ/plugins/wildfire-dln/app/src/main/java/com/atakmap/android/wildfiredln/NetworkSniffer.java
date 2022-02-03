package com.atakmap.android.wildfiredln;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Environment;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.TableLayout;

import com.atakmap.android.wildfiredln.plugin.R;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.Vector;

public class NetworkSniffer implements NetworkAsyncResponse
{

    public static final String TAG = PluginTemplateDropDownReceiver.class.getSimpleName();
    private WildfireDLN parent;

    public NetworkSniffer(WildfireDLN up, Context pluginContext)
    {
        parent = up;


        NetworkSniffTask nettask = new NetworkSniffTask(pluginContext, this);
        nettask.execute();
    }

    @Override
    public void AsyncTask(Vector<DownloadReference> references) {

        parent.UpdateReferences(references);
    }

    public class NetworkSniffTask extends AsyncTask<Void, Void, Vector<DownloadReference>> {

        //private static final String TAG = Constants.TAG + "nstask";

        private NetworkAsyncResponse delegate = null;

        private WeakReference<Context> mContextRef;
        WifiManager wm;

        public NetworkSniffTask(Context context, NetworkAsyncResponse responseObject) {
            mContextRef = new WeakReference<Context>(context);
            delegate = responseObject;
            wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        }

        @Override
        protected Vector<DownloadReference> doInBackground(Void... voids) {
            Vector<DownloadReference> references = new Vector<DownloadReference>();
            //results += "Let's sniff the network\n";

            try {
                Context context = mContextRef.get();

                if (context != null) {

                    //results += context+"\n";
                    ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo activeNetwork = cm.getActiveNetworkInfo();


                    WifiInfo connectionInfo = wm.getConnectionInfo();
                    int ipAddress = connectionInfo.getIpAddress();
                    String ipString = Formatter.formatIpAddress(ipAddress);


                    //results += "activeNetwork" + String.valueOf(activeNetwork) + "\n";
                    //results += "ipString: " + String.valueOf(ipString) + "\n";
                    Log.d(TAG, "activeNetwork: " + String.valueOf(activeNetwork));
                    Log.d(TAG, "ipString: " + String.valueOf(ipString));

                    String prefix = ipString.substring(0, ipString.lastIndexOf(".") + 1);
                    //results += "prefix: " + prefix + "\n";
                    Log.d(TAG, "prefix: " + prefix);

                    String[] ipaddresses = new String[255];
                    Thread[] threads = new Thread[255];
                    NetworkWorker[] runners = new NetworkWorker[255];
                    String[] hnames = new String[255];
                    Boolean[] reachable = new Boolean[255];

                    for (int i = 0; i < 255; i++) {
                        ipaddresses[i] = prefix + String.valueOf(i);
                        runners[i] = new NetworkWorker(ipaddresses[i]);
                        threads[i] = new Thread(runners[i]);
                        threads[i].start();
                    }

                    for(int i=0;i<255;i++)
                    {
                        threads[i].join();

                        reachable[i] = runners[i].GetReachable();
                        hnames[i] = runners[i].GetHostName();

                        if (reachable[i]) {
                            //results += "Host: " + String.valueOf(hnames[i]) + "(" + String.valueOf(ipaddresses[i]) + ") is reachable!\n";
                            Log.i(TAG, "Host: " + String.valueOf(hnames[i]) + "(" + String.valueOf(ipaddresses[i]) + ") is reachable!");

                            if (runners[i].GetReferences() != null) {
                                //results += runners[i].GetResponseString() + "\n";
                                references.addAll(runners[i].GetReferences());
                            }
                        }
                    }
                }
            } catch (Throwable t) {
                //results += "Well that's not good.\n";
                //results += Log.getStackTraceString(t);
                //Log.e(TAG,e.)
                Log.e(TAG, "Network Connection Failed!", t);
            }

            //results += "Done";

            Vector<DownloadReference> internals = new Vector<DownloadReference>();

            String path = Environment.getExternalStorageDirectory().toString()+"/ATAK_Downloads";
            Log.d(TAG,path);
            File directory = new File(path);
            File[] internalFiles = directory.listFiles();

            Log.d(TAG,"Files Found: "+internalFiles.length);

            for(int i=0; i<internalFiles.length;i++)
            {
                File ifile = internalFiles[i];
                DownloadReference dref = new DownloadReference(ifile.getAbsolutePath(),ifile.getName(),ifile.length(),new Date(ifile.lastModified()));
                dref.SetIsLocal(true);
                internals.add(dref);
            }

            for(int i=0; i<references.size();i++)
            {
                if(!internals.contains(references.get(i)))
                {
                    internals.add(references.get(i));
                }
            }

            return internals;
        }

        protected void onPostExecute(Vector<DownloadReference> references)
        {
            delegate.AsyncTask(references);
        }
    }
}
