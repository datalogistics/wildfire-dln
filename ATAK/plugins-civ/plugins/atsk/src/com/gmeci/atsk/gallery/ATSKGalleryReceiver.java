
package com.gmeci.atsk.gallery;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.atakmap.coremap.log.Log;

/**
 * Receive capture intent
 */
public class ATSKGalleryReceiver extends BroadcastReceiver {

    private static final String TAG = "ATSKGalleryReceiver";
    private ATSKGalleryFragment _galleryFragment;

    public void setGallery(ATSKGalleryFragment galleryFragment) {
        _galleryFragment = galleryFragment;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (_galleryFragment == null)
            return;
        String action = intent.getAction();
        Bundle extras = intent.getExtras();

        if (action.equals(ATSKGalleryUtils.IMG_CAPTURE)) {
            Log.d(TAG, "Received image capture result");
            _galleryFragment.refresh();
        } else if (action.equals(ATSKGalleryUtils.ACTIVITY_FINISHED)) {
            if (extras != null) {
                final int requestCode = extras.getInt("requestCode");
                final int resultCode = extras.getInt("resultCode");
                if (requestCode == ATSKGalleryUtils.IMG_IMPORT_CODE) {
                    Log.d(TAG, "Received activity finished: "
                            + requestCode + " -> " + resultCode);
                    Intent data = extras.getParcelable("data");
                    if (data != null)
                        _galleryFragment.importImage(data.getData());
                }
            }
        }
    }
}
