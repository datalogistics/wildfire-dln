
package com.gmeci.atsk.resources;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.view.MotionEvent;
import android.view.View;
import com.atakmap.android.atsk.plugin.R;
import com.gmeci.core.ATSKConstants;

import java.util.ArrayList;
import java.util.List;

public class LCRButton extends ImageView {

    private CollectionSide currentCollectionSide = CollectionSide.LEFT;
    private List<OnChangedListener> _listeners = new ArrayList<OnChangedListener>();

    public enum CollectionSide {
        LEFT, CENTER, RIGHT
    }

    public LCRButton(Context context) {
        super(context);
        init();
    }

    public LCRButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public LCRButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        setImageResource(R.drawable.route_lrc_l);

        setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                //should check if we're currently collecting before changing this value...
                if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
                    double RealX = event.getX();
                    CollectionSide side;
                    if (RealX < view.getWidth() / 3f)
                        side = CollectionSide.LEFT;
                    else if (RealX > view.getWidth() * 2f / 3f)
                        side = CollectionSide.RIGHT;
                    else
                        side = CollectionSide.CENTER;
                    setSelectionSide(side);
                }
                return true;
            }
        });
    }

    public CollectionSide getCollectionSide() {
        return currentCollectionSide;
    }

    public void setSelectionSide(int cs) {
        if (cs == ATSKConstants.ANCHOR_APPROACH_LEFT)
            setSelectionSide(CollectionSide.LEFT);
        else if (cs == ATSKConstants.ANCHOR_APPROACH_RIGHT)
            setSelectionSide(CollectionSide.RIGHT);
        else
            setSelectionSide(CollectionSide.CENTER);
    }

    public void setSelectionSide(final CollectionSide cs) {
        boolean same = currentCollectionSide == cs;
        currentCollectionSide = cs;
        post(new Runnable() {
            public void run() {
                if (cs == CollectionSide.RIGHT) {
                    setImageResource(R.drawable.route_lrc_r);
                } else if (cs == CollectionSide.LEFT) {
                    setImageResource(R.drawable.route_lrc_l);
                } else {
                    setImageResource(R.drawable.route_lrc_c);
                }
            }

        });
        if (!same) {
            for (OnChangedListener ocl : _listeners)
                ocl.onSideChanged(this, cs);
        }
    }

    public void addOnChangedListener(OnChangedListener ocl) {
        if (!_listeners.contains(ocl))
            _listeners.add(ocl);
    }

    public void removeOnChangedListener(OnChangedListener ocl) {
        if (_listeners.contains(ocl))
            _listeners.remove(ocl);
    }

    public interface OnChangedListener {
        void onSideChanged(LCRButton lcrButton, CollectionSide side);
    }
}
