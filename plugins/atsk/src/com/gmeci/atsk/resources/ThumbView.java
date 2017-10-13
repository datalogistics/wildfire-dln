
package com.gmeci.atsk.resources;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.BaseAdapter;

import com.atakmap.android.image.FullImageView;

/**
 * Image view with proper bitmap recycling
 */

public class ThumbView extends FullImageView {

    private Bitmap _bmp;
    private BaseAdapter _adapter;

    public ThumbView(Context context) {
        super(context);
    }

    public ThumbView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ThumbView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setAdapter(BaseAdapter adapter) {
        _adapter = adapter;
    }

    public void dispose() {
        if (_bmp != null) {
            _bmp.recycle();
            _bmp = null;
        }
    }

    @Override
    public void setImageBitmap(Bitmap bmp) {
        if (_bmp != null && bmp != _bmp)
            _bmp.recycle();
        super.setImageBitmap(_bmp = bmp);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (_bmp != null && _bmp.isRecycled()) {
            super.setImageBitmap(_bmp = null);
            if (_adapter != null)
                _adapter.notifyDataSetChanged();
        }
        super.onDraw(canvas);
    }
}
