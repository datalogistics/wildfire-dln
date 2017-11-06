
package com.gmeci.atskservice.form.formobjects;

import com.gmeci.atskservice.R;

import android.view.View;
import android.content.Context;
import android.widget.CheckBox;

public class TriCheckField extends FormObject {

    private final CheckBox cb;

    private int state = 0;
    private final static int POS = 0;
    private final static int NEG = 1;
    private final static int UNK = 2;

    private void cycle() {
        state = (state + 1) % 3;
    }

    public TriCheckField(final Context c, final String key, String simpleName) {
        super(c, key);
        cb = new CheckBox(c);
        cb.setText(simpleName);
        if (prefs.contains(key)) {
            if (getBoolean(key, false)) {
                cb.setChecked(true);
                cb.setButtonDrawable(R.drawable.green);
                state = POS;
            } else {
                cb.setChecked(true);
                cb.setButtonDrawable(R.drawable.red);
                state = NEG;
            }
        } else {
            cb.setChecked(false);
            cb.setButtonDrawable(R.drawable.grey);
            state = UNK;
        }

        cb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                cycle();
                if (state == POS) {
                    cb.setChecked(true);
                    cb.setButtonDrawable(R.drawable.green);
                    putBoolean(key, true);
                } else if (state == NEG) {
                    cb.setChecked(true);
                    cb.setButtonDrawable(R.drawable.red);
                    putBoolean(key, false);
                } else {
                    cb.setButtonDrawable(R.drawable.grey);
                    prefs.edit().remove(key).commit();
                }
            }
        });

    }

    @Override
    protected View getViewImpl() {
        return cb;
    }

    @Override
    public void commit() {
    }

    @Override
    public String getText() {
        String val = "UNK";
        if (state == POS)
            val = "POS";
        else if (state == NEG)
            val = "NEG";

        return cb.getText().toString() + " " + val;
    }

}
