package com.atakmap.android.wildfiredln;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.atakmap.android.wildfiredln.plugin.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

public class WDLNTreePermissionsActivity extends Activity
{
    public static final String TAG = WDLNTreePermissionsActivity.class.getSimpleName();
    final WDLNTreePermissionsActivity that = this;

    Button grantButton;

    public static boolean go = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String package_name = getApplication().getPackageName();
        //setContentView(getApplication().getResources().getIdentifier("activity_new", "layout", package_name));
        setContentView(R.layout.permissions_layout);

        grantButton = (Button) findViewById(R.id.button);
        grantButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v)
            {
                String filepath = Environment.getExternalStorageDirectory().toString();
                File fileDirectory = new File(filepath+"/ATAK_Downloads");

                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, fileDirectory.getAbsoluteFile());
                intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
                //intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                that.startActivityForResult(intent, 1);
                Log.d(TAG,"Started Permissions Intent!");
            }
        });

        //closeButton = (Button) findViewById(R.id.dialog_close);
        /*closeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v)
            {
                that.finish();
            }
        });*/
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if(requestCode == 1 && resultCode == Activity.RESULT_OK)//image upload
        {
            Log.d(TAG,"Got Permissions Result");
            if(data != null) {
                /*SharedPreferences spref = this.getApplicationContext().getSharedPreferences(this.getString(R.string.preference_file_key), this.MODE_PRIVATE);
                SharedPreferences.Editor editor = spref.edit();
                Log.d(TAG,"Setting Shared Folder to "+data.toString());
                editor.putString(this.getString(R.string.shared_data_folder),data.toString());
                editor.commit();*/

                try {
                    Log.d(TAG, "Setting Shared Folder to " + data.getDataString());
                    //OutputStreamWriter outputStreamWriter = new OutputStreamWriter(this.openFileOutput(this.getString(R.string.preference_file_key), this.MODE_PRIVATE));
                    File savedir = new File(this.getExternalFilesDir(null),this.getString(R.string.preference_file_key));
                    Log.d(TAG,"Writing data to "+savedir.getAbsolutePath());
                    FileOutputStream outputStreamWriter = new FileOutputStream(savedir);
                    //OutputStreamWriter outputStreamWriter = new OutputStreamWriter(savedir);
                    outputStreamWriter.write(data.getDataString().getBytes(StandardCharsets.UTF_8));
                    outputStreamWriter.close();
                    Log.d(TAG,"Finished Writing File");

                    Log.d(TAG,this.getApplicationContext().toString());
                    Log.d(TAG,"Permissions count before saving is: "+this.getApplicationContext().getContentResolver().getPersistedUriPermissions().size());

                    this.getApplicationContext().getContentResolver().takePersistableUriPermission(Uri.parse(data.getDataString()), Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                    Log.d(TAG,"Permissions count after saving is: "+this.getApplicationContext().getContentResolver().getPersistedUriPermissions().size());
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            Log.d(TAG,"Permissions Exiting");
            WDLNTreePermissionsActivity.go = false;
            that.finish();
        }
        else
        {
            Log.d(TAG, "Error: Request Was " + requestCode + " and result was "+resultCode);
        }
    }
}