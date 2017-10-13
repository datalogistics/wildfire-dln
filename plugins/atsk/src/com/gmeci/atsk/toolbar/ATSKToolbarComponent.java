
package com.gmeci.atsk.toolbar;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.view.Menu;
import android.view.MenuItem;

import com.atakmap.android.maps.MapComponent;
import com.atakmap.android.maps.MapView;

public class ATSKToolbarComponent implements MapComponent {

    private static ATSKToolbar _toolbar;

    public static ATSKToolbar getToolbar() {
        return _toolbar;
    }

    @Override
    public void onCreate(Context context, Intent intent, MapView view) {
        _toolbar = new ATSKToolbar(view, context);
    }

    @Override
    public void onDestroy(Context context, MapView view) {
        _toolbar.dispose();
    }

    @Override
    public void onStart(Context context, MapView view) {

    }

    @Override
    public void onStop(Context context, MapView view) {

    }

    @Override
    public void onPause(Context context, MapView view) {

    }

    @Override
    public void onResume(Context context, MapView view) {

    }

    @Override
    public boolean onCreateOptionsMenu(Context context, Menu menu) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean onPrepareOptionsMenu(Context context, Menu menu) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(Context context, MenuItem item) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void onOptionsMenuClosed(Context context, Menu menu) {
        // TODO Auto-generated method stub

    }

    public void onConfigurationChanged(Configuration newConfig) {
    }

}
