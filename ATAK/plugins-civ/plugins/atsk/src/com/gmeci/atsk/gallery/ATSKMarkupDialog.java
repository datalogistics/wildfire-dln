
package com.gmeci.atsk.gallery;

import android.graphics.Bitmap;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Toast;

import com.atakmap.android.atsk.plugin.R;
import com.atakmap.android.imagecapture.TiledCanvas;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;
import com.gmeci.atsk.ATSKFragmentManager;
import com.gmeci.atsk.ATSKMapComponent;
import com.gmeci.atsk.resources.ATSKImageDialog;

import java.io.File;

public class ATSKMarkupDialog extends ATSKImageDialog {

    private static final String TAG = "ATSKMarkupDialog";

    private TiledCanvas _output;

    protected final View.OnClickListener _saveClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            _output = null;
            cleanup();
            ATSKFragmentManager manager = ATSKMapComponent.getATSKFM();
            if (manager != null)
                manager.reloadActiveFragment();
            ATSKMarkupDialog.this.dismiss();
        }
    };

    @Override
    protected int getLayoutId() {
        return R.layout.image_markup_dialog;
    }

    @Override
    public void onViewCreated() {
        if (_saveBtn != null)
            _saveBtn.setOnClickListener(_saveClick);
    }

    @Override
    public void cleanup() {
        super.cleanup();
        if (_output != null) {
            Log.d(TAG, "Deleting markup output: " + _output.getFile());
            FileSystemUtils.deleteFile(_output.getFile());
            _output = null;
        }
    }

    @Override
    protected Bitmap loadBitmap() {
        String name = _inFile.getName();
        name = name.substring(0, name.lastIndexOf(".")) + "_markup"
                + name.substring(name.lastIndexOf("."));
        File outTmp = new File(_inFile.getParent(), name);
        _output = ATSKMarkup.applyMarkup(_inFile, outTmp,
                new ATSKMarkup.ProgressCallback() {
                    @Override
                    public boolean onProgress(int prog, int max) {
                        if (!_created || getActivity() == null) {
                            cleanup();
                            return false;
                        }
                        return true;
                    }
                });
        if (!_created || getActivity() == null) {
            cleanup();
            return null;
        }
        if (_output != null) {
            DisplayMetrics dm = getActivity().getResources()
                    .getDisplayMetrics();
            return _output.createThumbnail(dm.widthPixels, dm.heightPixels);
        } else {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getActivity(),
                            "Failed to apply markup to image.",
                            Toast.LENGTH_LONG).show();
                }
            });
        }
        return null;
    }

    @Override
    protected void beginSetup() {
        super.beginSetup();
        if (_created)
            _title.setText("Generating markup...");
    }

    @Override
    protected void loadFinished() {
        _title.setText(_inFile.getName());
    }
}
