package com.atakmap.android.wildfiredln;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.atakmap.android.wildfiredln.plugin.R;

import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class UploadChooserActivity extends Activity
{

    public static final String TAG = UploadChooserActivity.class.getSimpleName();
    private ArrayList<String> ips;
    final UploadChooserActivity that = this;

    Button imageButton;
    Button videoButton;
    Button cancelButton;
    Button closeButton;
    AsyncTask uploadTask = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String package_name = getApplication().getPackageName();
        //setContentView(getApplication().getResources().getIdentifier("activity_new", "layout", package_name));
        setContentView(R.layout.upload_dialog_layout);

        Bundle b = getIntent().getExtras();
        ips = (ArrayList<String>)b.get("ips");
        //ips.add("204.38.5.195:8080/UploadFiles");

        refreshTable();

        imageButton = (Button) findViewById(R.id.dialog_image_upload);
        imageButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v)
            {
                Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                that.startActivityForResult(intent, 1);
            }
        });

        videoButton = (Button) findViewById(R.id.dialog_video_upload);
        videoButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v)
            {
                Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                that.startActivityForResult(intent, 2);
            }
        });

        cancelButton = (Button) findViewById(R.id.dialog_cancel);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v)
            {
                that.cancelTask();
            }
        });

        closeButton = (Button) findViewById(R.id.dialog_close);
        closeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v)
            {
                that.finish();
            }
        });
    }

    public void refreshTable()
    {
        final TableLayout tableLayout = (TableLayout) findViewById(R.id.dialog_table);
        tableLayout.removeAllViews();

        Log.d(TAG, "Refreshing IP List");

        for(int i=0;i<ips.size();i++)
        {
            final String s = ips.get(i)+":8080/upload";

            TableRow row = new TableRow(getApplicationContext());
            TableRow.LayoutParams lp = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT);
            row.setLayoutParams(lp);

            final Button ipButton = new Button(getApplicationContext());
            ipButton.setText(s);
            ipButton.setOnClickListener(new View.OnClickListener()
            {
                public void onClick(View v)
                {
                    ((TextView)findViewById(R.id.dialog_ip)).setText(s);
                }
            });

            row.addView(ipButton);
            tableLayout.addView(row,i);

            Log.d(TAG, "Added IP: "+s);
        }

        tableLayout.invalidate();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        String target = "http://"+((TextView)findViewById(R.id.dialog_ip)).getText().toString();

        if(requestCode == 1 && resultCode == Activity.RESULT_OK)//image upload
        {
            Uri uri = data.getData();
            Log.d(TAG, "image selected: " + uri);
            uploadTask = new UploadFileTask().execute(new ContentWrappper(target,uri));
            setButtonsState(false);
        }
        else if(requestCode == 2 && resultCode == Activity.RESULT_OK)//video upload
        {
            Uri uri = data.getData();
            Log.d(TAG, "video selected: " + uri);
            uploadTask = new UploadFileTask().execute(new ContentWrappper(target,uri));
            setButtonsState(false);
        }
        else
        {
            Log.d(TAG, "Error: Request Was " + requestCode + " and result was "+resultCode);
        }
    }

    private void setProgressPercent(int p)
    {
        ProgressBar pb = ((ProgressBar)findViewById(R.id.dialog_progress));
        TextView tv = ((TextView)findViewById(R.id.dialog_progress_text));
        Log.d(TAG, "Upload Progress: " + p);

        if(p==100)
        {
            Drawable progressDrawable = pb.getProgressDrawable().mutate();
            progressDrawable.setColorFilter(Color.GREEN, PorterDuff.Mode.MULTIPLY);
            pb.setProgressDrawable(progressDrawable);
            pb.setProgress(p);
            tv.setText("Complete");
        }
        else if(p>=0)
        {
            Drawable progressDrawable = pb.getProgressDrawable().mutate();
            progressDrawable.setColorFilter(Color.BLUE, PorterDuff.Mode.MULTIPLY);
            pb.setProgressDrawable(progressDrawable);
            pb.setProgress(p);
            tv.setText(""+p+"%");
        }
        else
        {
            Drawable progressDrawable = pb.getProgressDrawable().mutate();
            progressDrawable.setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN);
            pb.setProgressDrawable(progressDrawable);
            pb.setProgress(100);

            switch(p)
            {
                case -2:
                    tv.setText("Bad URL");
                    break;
                case -3:
                    tv.setText("Unable To Connect");
                    break;
                case -4:
                    tv.setText("Canceled By User");
                    break;
                case -400:
                    tv.setText("Bad Request");
                    break;
                case -404:
                    tv.setText("HTTP Not Found");
                    break;
                case -500:
                    tv.setText("Server Error");
                    break;
                default:
                    tv.setText("Unknown Error");

            }
        }
    }

    public void cancelTask()
    {
        if(uploadTask != null && !uploadTask.isCancelled())
        {
            uploadTask.cancel(false);
            setProgressPercent(-4);
        }

        setButtonsState(true);
    }

    public void setButtonsState(boolean state)
    {
        imageButton.setEnabled(state);
        videoButton.setEnabled(state);
        cancelButton.setEnabled(!state);
    }

    private class ContentWrappper
    {
        public String destination;
        public Uri source;

        public ContentWrappper(String d, Uri s)
        {
            destination = d;
            source = s;
        }
    }

    private class UploadFileTask extends AsyncTask<ContentWrappper, Integer, Boolean>
    {

        @Override
        protected Boolean doInBackground(ContentWrappper... cws)
        {
            int count = cws.length;

            for(int i=0;i<count;i++)
            {
                try
                {
                    URL url = new URL(cws[i].destination);
                    Uri f = cws[i].source;
                    String fname = "Unknown";
                    String fpath = "";
                    int size = 0;

                    if(f.getScheme().equals("content"))
                    {
                        Cursor cursor = getContentResolver().query(f,null,null,null,null);
                        if(cursor == null)
                        {
                            fname = f.getLastPathSegment();
                            fpath = f.getPath();
                        }
                        else
                        {
                            try
                            {
                                cursor.moveToFirst();
                                fname = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                                size = Integer.valueOf(cursor.getString(cursor.getColumnIndex(OpenableColumns.SIZE)));
                            } finally
                            {
                                cursor.close();
                            }
                        }
                    }

                    Log.d(TAG, "Filename: " + fname);

                    String boundary = "**|***|**";


                    HttpURLConnection client = (HttpURLConnection)url.openConnection();
                    client.setUseCaches(false);
                    client.setDoOutput(true);
                    client.setChunkedStreamingMode(0);
                    client.setRequestMethod("POST");
                    client.setRequestProperty("Connection","Keep-Alive");
                    client.setRequestProperty("Transfer-Encoding","chunked");
                    client.setRequestProperty("Cache-Control", "no-cache");
                    client.setRequestProperty(
                            "Content-Type", "multipart/form-data;boundary="+boundary);
                    /*client.setRequestProperty(
                            "Content-Type", "application/json; charset=UTF-8");*/

                    //Log.d(TAG, "Header sent");

                    OutputStream out = new DataOutputStream(client.getOutputStream());
                    out.write(("--"+boundary+"\r\n").getBytes());
                    out.write(("Content-Disposition: form-data; name=\""+fname
                              +"\";filename=\""+fname+"\"\r\n").getBytes());

                    //Log.d(TAG, "Filename sent");

                    //BufferedInputStream inf = new BufferedInputStream(new FileInputStream(new File(f.getPath())));
                    InputStream inf = getContentResolver().openInputStream(f);
                    Log.d(TAG, "Input Stream Created");
                    //IOUtils.copy(inf,out);

                    byte[] buffer = new byte[8192];
                    int len = inf.read(buffer);
                    long total = len;
                    while(len != -1)
                    {
                        out.write(buffer,0,len);
                        Log.d(TAG, "Wrote "+len+" bytes to output");
                        out.flush();

                        if(size>0)
                        {
                            publishProgress((int)((100*total)/size));
                        }
                        else
                        {
                            publishProgress(50);
                        }

                        len = inf.read(buffer);
                        total += len;

                        if(isCancelled())
                        {
                            publishProgress(-4);
                            client.disconnect();
                            return null;
                        }
                    }
                    Log.d(TAG, "Flushing Data");
                    out.flush();
                    inf.close();

                    //out.write("{\"key\":1}".getBytes("UTF-8"));

                    //Log.d(TAG, "output complete");

                    out.write(("\r\n--"+boundary+"--\r\n").getBytes());
                    out.flush();
                    out.close();


                    int status = client.getResponseCode();

                    if(status == 200)
                    {
                        publishProgress(100);
                    }
                    else
                    {
                        publishProgress(-status);
                    }

                    Log.d(TAG, "Footer Sent: "+status);
                    client.disconnect();


                }
                catch (MalformedURLException e)
                {
                    Log.d(TAG, "blarg");
                    Log.d(TAG, e.toString());
                    e.printStackTrace();
                    publishProgress(-2);
                }
                catch (UnknownHostException e)
                {
                    Log.d(TAG, "blarg");
                    Log.d(TAG, e.toString());
                    e.printStackTrace();
                    publishProgress(-3);
                }
                catch (IOException e)
                {
                    Log.d(TAG, "narg");
                    Log.d(TAG, e.toString());
                    e.printStackTrace();
                    publishProgress(-1);
                }
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... progress)
        {
            setProgressPercent(progress[0]);
        }

        @Override
        protected void onPostExecute(Boolean result)
        {
            setButtonsState(true);
        }
    }
}
