
package com.gmeci.atskservice;

import android.app.Activity;
import android.content.res.AssetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbDevice;
import android.widget.TextView;
import android.widget.ScrollView;
import android.util.Log;
import java.lang.*;
import java.net.*;
import java.util.*;
import java.io.*;
import android.view.KeyEvent;

public class PdfSaver extends Activity {

    final static String TAG = "PdfSaver";

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    }

    public void onStart() {
        super.onStart();
        finish();

    }

    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if ((keyCode == KeyEvent.KEYCODE_BACK))
        {
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }

}
