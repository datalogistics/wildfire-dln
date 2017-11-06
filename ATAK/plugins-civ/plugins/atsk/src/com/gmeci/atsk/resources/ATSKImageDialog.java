
package com.gmeci.atsk.resources;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.atakmap.android.atsk.plugin.R;

import java.io.File;

/**
 * Generic image dialog
 */
public abstract class ATSKImageDialog extends DialogFragment {
    private static final String TAG = "ATSKImageDialog";

    // view components
    protected View _root;
    protected Button _saveBtn;
    protected Button _cancelBtn;
    protected TextView _title;
    protected ImageView _imgView;
    protected ProgressBar _loader;

    // protected fields
    protected Context _plugin;
    protected Bitmap _bmp;
    protected File _inFile;
    protected int _width, _height;

    protected boolean _created = false;
    protected Handler _viewHandler;
    protected Runnable _onCancel;

    protected final View.OnClickListener _cancelClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (_onCancel != null)
                _onCancel.run();
            cleanup();
            dismiss();
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        _created = false;
        _plugin = ATSKApplication.getInstance().getPluginContext();

        _root = LayoutInflater.from(_plugin)
                .inflate(getLayoutId(), container, false);
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getDialog().getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        DisplayMetrics metrics = _plugin.getResources().getDisplayMetrics();
        float margin = metrics.density * 10f;
        int width = metrics.widthPixels;
        int height = metrics.heightPixels;
        _width = (int) (width - margin);
        _height = (int) (height - margin);
        _root.setMinimumWidth(_width);
        _root.setMinimumHeight(_height);

        return _root;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        _title = (TextView) _root.findViewById(R.id.image_dlg_title);
        _imgView = (ImageView) _root.findViewById(R.id.image_dlg_bitmap);
        _loader = (ProgressBar) _root.findViewById(R.id.image_dlg_loader);
        _saveBtn = (Button) _root.findViewById(R.id.image_dlg_save);
        _cancelBtn = (Button) _root.findViewById(R.id.image_dlg_cancel);

        // Override this
        if (_saveBtn != null)
            _saveBtn.setOnClickListener(_cancelClick);
        if (_cancelBtn != null)
            _cancelBtn.setOnClickListener(_cancelClick);

        _viewHandler = new Handler();
        onViewCreated();
        _created = true;

        // Display existing input image
        beginSetup();
    }

    // Process extra views
    protected void onViewCreated() {
        // Override me
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        _created = false;
        _inFile = null;
        cleanup();
    }

    public void setOnCancelListener(Runnable r) {
        _onCancel = r;
    }

    // Return layout resource id here
    protected abstract int getLayoutId();

    // Return image bitmap here
    protected abstract Bitmap loadBitmap();

    // Image finished loading
    protected abstract void loadFinished();

    public void setupImage(Bitmap bmp) {
        cleanup();
        _bmp = bmp;
        beginSetup();
    }

    // Set input image and display (if dialog is ready)
    public void setupImage(File inFile) {
        _inFile = inFile;
        beginSetup();
    }

    protected void beginSetup() {
        if (_created) {
            _saveBtn.setEnabled(false);
            _title.setText("Loading image...");
            _imgView.setVisibility(View.GONE);
            _loader.setVisibility(View.VISIBLE);
            // Start markup processing
            new Thread(new Runnable() {
                public void run() {
                    if (_inFile != null) {
                        cleanup();
                        _bmp = loadBitmap();
                    }
                    // Show new bitmap in container
                    if (_created) {
                        _viewHandler.post(new Runnable() {
                            public void run() {
                                _loader.setVisibility(View.GONE);
                                _imgView.setVisibility(View.VISIBLE);
                                _imgView.setImageBitmap(_bmp);
                                _saveBtn.setEnabled(true);
                                loadFinished();
                            }
                        });
                    } else
                        cleanup();
                }
            }).start();
        }
    }

    protected void cleanup() {
        if (_bmp != null) {
            _bmp.recycle();
            _bmp = null;
        }
    }
}
