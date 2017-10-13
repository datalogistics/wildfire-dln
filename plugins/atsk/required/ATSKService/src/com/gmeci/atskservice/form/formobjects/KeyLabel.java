
package com.gmeci.atskservice.form.formobjects;

import android.content.SharedPreferences;

import android.view.View;
import android.content.Context;
import android.widget.TextView;
import android.graphics.Color;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;

public class KeyLabel extends FormObject implements
        OnSharedPreferenceChangeListener {

    private final TextView tv;
    private final int length;

    public KeyLabel(final Context c, int length, String key) {
        super(c, key);
        this.length = length;

        String val = prefs.getString(key, key);
        tv = textView(val);

        if (length > 0)
            tv.setEms(length);

        tv.setTextColor(Color.parseColor("#ffffff"));

        prefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected View getViewImpl() {
        return tv;
    }

    @Override
    public void commit() {
    }

    @Override
    public String getText() {
        return tv.getText().toString();
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sp,
            final String k) {
        if (key.equalsIgnoreCase(k)) {
            tv.post(new Runnable() {
                public void run() {
                    tv.setText(sp.getString(k, k));
                }
            });
        } else if ((key + "_override").equalsIgnoreCase(k)) {

            final String pval = sp.getString(k, k);
            final String tval = tv.getText().toString();
            if (!pval.equalsIgnoreCase(tval)) {
                tv.post(new Runnable() {
                    public void run() {
                        tv.setText(sp.getString(k, k));
                        tv.setTextColor(Color.parseColor("#FFCCCC"));
                    }
                });
            }
        }
    }

}
