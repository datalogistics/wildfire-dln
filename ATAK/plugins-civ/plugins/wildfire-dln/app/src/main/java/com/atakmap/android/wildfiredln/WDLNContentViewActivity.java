package com.atakmap.android.wildfiredln;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.core.content.FileProvider;

import com.atakmap.android.wildfiredln.plugin.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class WDLNContentViewActivity extends Activity
{

    public static final String TAG = WDLNContentViewActivity.class.getSimpleName();
    private String file;
    final WDLNContentViewActivity that = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String package_name = getApplication().getPackageName();

        Bundle b = getIntent().getExtras();
        file = (String)b.get("file");

        String mtype = "*/*";

        //images
        if(file.endsWith("jpg"))
        {
            mtype = "image/jpeg";
        }
        else if(file.endsWith("png"))
        {
            mtype = "image/png";
        }
        else if(file.endsWith("gif"))
        {
            mtype = "image/gif";
        }
        else if(file.endsWith("bmp"))
        {
            mtype = "image/bmp";
        }
        else if(file.endsWith("webp"))
        {
            mtype = "image/webp";
        }
        //videos
        else if(file.endsWith("3gp"))
        {
            mtype = "video/3gp";
        }
        else if(file.endsWith("mp4"))
        {
            mtype = "video/mp4";
        }
        else if(file.endsWith("ts"))
        {
            mtype = "video/ts";
        }
        else if(file.endsWith("webm"))
        {
            mtype = "video/webm";
        }

        Log.d(TAG,file+" "+mtype);

        File internalDirectory = this.getExternalFilesDir(null);
        File downloadsDirectory = new File(internalDirectory,"/ATAK_Downloads");
        File shareFile = new File(downloadsDirectory,file);

        if(!shareFile.exists())
        {
            Log.d(TAG,"Cannot share file that does not exist: "+shareFile.getAbsolutePath());
            this.finish();
        }
        else {

            Uri uri = FileProvider.getUriForFile(this, "com.atakmap.android.wildfiredln.plugin.fileprovider", shareFile);

            Log.d(TAG, "Granting access to " + downloadsDirectory.getAbsolutePath() + "/" + uri.toString());
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.setDataAndType(uri, mtype);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);

            List<ResolveInfo> resInfoList = this.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
            for (ResolveInfo resolveInfo : resInfoList) {
                String packageName = resolveInfo.activityInfo.packageName;
                Log.d(TAG, packageName);
                this.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                //parent.GetActivity().grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }

            this.startActivityForResult(intent, 1);
            Log.d(TAG, "Granted access");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if(requestCode == 1)
        {
            Log.d(TAG,"Done File Sharing");
            this.finish();
        }
    }
}
