package com.atakmap.android.wildfiredln;

import android.util.Log;

import com.android.volley.NetworkError;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Vector;

import org.apache.commons.io.IOUtils;

public class NetworkWorker implements Runnable
{

    private String IPString;
    private InetAddress address;
    private boolean reachable;
    private boolean failed;
    private String hostName;
    private String responseString;
    private Vector<NodeReference> nreferences = null;
    private Vector<DownloadReference> references = null;

    public static final String TAG = "NetworkWorkerThread";

    public NetworkWorker(String ipstring)
    {
        IPString = ipstring;
    }
    @Override
    public void run()
    {
        try
        {
            /*if(IPString.contains("204.38.5.168"))
            {
                Log.d(TAG, "--------------------------------------------\n--------------------------------------------\n----------------------204.38.5.168----------------------");
                Log.d(TAG, ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>"+IPString);
            }*/


            address = InetAddress.getByName(IPString);
            reachable = address.isReachable(3000);
            hostName = address.getCanonicalHostName();


            if(reachable)
            {
                failed = false;
                //exnodes
                Log.d(TAG, "Trying HTTP connection to http://"+IPString + ":9000/exnodes?fields=name,size,id,created");
                URL url = new URL("http://"+IPString + ":9000/exnodes?fields=name,size,id,created");
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setConnectTimeout(3000);
                urlConnection.setReadTimeout(3000);

                try
                {
                    InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                    responseString = IOUtils.toString(in);
                    Log.d(TAG, "http://"+IPString + "/exnodes/ -- Response"+responseString);



                    references = DownloadReference.DownloadParser("http://"+IPString,responseString);
                }
                catch (ConnectException e)
                {
                    //Log.d(TAG, Log.getStackTraceString(e));
                    failed = true;
                }
                catch(IOException e)
                {
                    Log.d(TAG, Log.getStackTraceString(e));
                    failed = true;
                }
                finally
                {
                    urlConnection.disconnect();
                }

                if(!failed)
                {
                    long unixTime = System.currentTimeMillis() * 1000L;
                    unixTime -= 300000000;//node history limited to 10 minutes
                    //Log.d(TAG,"Current time is "+unixTime);

                    //nodes
                    Log.d(TAG, "Trying HTTP connection to http://" + IPString + ":9000/nodes?ts=gte="+unixTime);
                    url = new URL("http://" + IPString + ":9000/nodes?ts=gte="+unixTime);
                    urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setConnectTimeout(3000);
                    urlConnection.setReadTimeout(3000);

                    try
                    {
                        InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                        responseString = IOUtils.toString(in);
                        Log.d(TAG, "http://" + IPString + "/nodes?ts=gte="+unixTime+" -- Response" + responseString);


                        nreferences = NodeReference.NodeParser("http://" + IPString, responseString);
                    }
                    catch (ConnectException e)
                    {
                        //Log.d(TAG, Log.getStackTraceString(e));
                        failed = true;
                    }
                    catch (IOException e)
                    {
                        Log.d(TAG, Log.getStackTraceString(e));
                        failed = true;
                    }
                    finally
                    {
                        urlConnection.disconnect();
                    }
                }
            }
            else
            {
                /*if(IPString.contains("204.38.5.168"))
                {
                    Log.d(TAG, "Couldn't reach host computer");
                }*/
            }


        }
        catch (UnknownHostException e)
        {
            Log.d(TAG, Log.getStackTraceString(e));
        }
        catch (IOException e)
        {
            Log.d(TAG, Log.getStackTraceString(e));
        }

        /*if(IPString.contains("204.38.5.168"))
        {
            Log.d(TAG, "--------------------------------------------\n--------------------------------------------\n----------------------end-204.38.5.168----------------------");
        }*/
    }

    public boolean GetReachable()
    {
        return reachable;
    }

    public boolean GetValid()
    {
        return reachable & !failed;
    }

    public String GetHostName()
    {
        return hostName;
    }

    public String GetResponseString()
    {
        return responseString;
    }

    public Vector<DownloadReference> GetReferences()
    {
        return references;
    }

    public Vector<NodeReference> GetNodes()
    {
        return nreferences;
    }

}
