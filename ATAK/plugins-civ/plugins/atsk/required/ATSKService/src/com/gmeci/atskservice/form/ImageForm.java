
package com.gmeci.atskservice.form;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import com.gmeci.atskservice.pdf.ImagePDFFiller;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Receive intent from ATAK to create PDF form with image listing
 */
public class ImageForm extends Activity {

    final static String TAG = "ImageForm";
    private File _pdf;
    private List<String> _imagePaths;
    private List<Integer> _imageOri;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent saveIntent = getIntent();
        if (saveIntent != null) {
            _pdf = (File) saveIntent.getSerializableExtra("pdf");
            _imagePaths = saveIntent.getStringArrayListExtra("images");
            if (saveIntent.hasExtra("orientations"))
                _imageOri = saveIntent.getIntegerArrayListExtra("orientations");
            else
                _imageOri = new ArrayList<Integer>();
        }
    }

    public void onStart() {
        super.onStart();
        if (_pdf != null && _imagePaths != null && !_imagePaths.isEmpty()) {
            List<File> images = new ArrayList<File>();
            for (String path : _imagePaths)
                images.add(new File(path));
            ImagePDFFiller pdf = new ImagePDFFiller(images, _imageOri);
            pdf.generate(_pdf);
        }
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
