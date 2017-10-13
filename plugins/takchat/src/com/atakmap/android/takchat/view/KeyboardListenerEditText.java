package com.atakmap.android.takchat.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;

import com.atakmap.coremap.log.Log;

/**
 * EditText that notifies listener of soft keyboard opening/closing
 * "Open" when focus is obtained
 * "Close" when focus is lost, or back button is clicked
 *
 * Created by byoung on 2/12/17.
 */
public class KeyboardListenerEditText extends EditText {

    private static final String TAG = "KeyboardListenerEditText";

    public interface KeyBoardListener {
        void keyboardShowing(boolean bShowing);
    }


    private Context context;
    private KeyBoardListener listener;

    public KeyboardListenerEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    public void setKeyboardListener(KeyBoardListener l) {
        this.listener = l;

//        this.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Log.d(TAG, "onClick");
//                listener.keyboardShowing(true);
//            }
//        });

        this.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (listener != null) {
                    Log.d(TAG, "onFocusChange: " + hasFocus);
                    listener.keyboardShowing(hasFocus);
                }
            }
        });
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            // User has pressed Back key. So hide the keyboard
//            InputMethodManager mgr = (InputMethodManager)
//                    context.getSystemService(Context.INPUT_METHOD_SERVICE);
//            mgr.hideSoftInputFromWindow(this.getWindowToken(), 0);

            if(listener != null){
                Log.d(TAG, "KEYCODE_BACK");
                listener.keyboardShowing(false);
            }

            this.clearFocus();
        }
        return false;
    }
}