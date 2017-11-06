
package com.gmeci.atskservice.form.formobjects;

import android.content.SharedPreferences;

import android.view.LayoutInflater;
import android.widget.LinearLayout;

import android.view.View;
import android.content.Context;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.CheckBox;
import android.preference.PreferenceManager;

import android.text.TextWatcher;
import android.text.Editable;

import java.util.*;

abstract public class FormObject {

    protected final Context c;
    protected final String key;
    protected final LayoutInflater layoutInflater;
    protected final SharedPreferences prefs;

    public TextView textView(String text) {
        TextView tv = new TextView(c);
        tv.setText(text);
        tv.setFocusableInTouchMode(true);
        return tv;
    }

    /**
     * ATSK saves everything as strings.
     */
    final protected boolean getBoolean(final String s, final boolean def) {
        try {
            return prefs.getString(s, def ? "true" : "false").equalsIgnoreCase(
                    "true");
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    final protected Set<String> getStringSet(final String s,
            final Set<String> def) {
        try {
            String v = prefs.getString(s, null);
            if (v == null)
                return def;
            Set<String> ret = new HashSet<String>();
            String[] vals = v.split("____");
            for (String val : vals) {
                ret.add(val);
            }
            return ret;

        } catch (Exception e) {
            e.printStackTrace();
            return def;
        }
    }

    final void putStringSet(final String s, final Set<String> vals) {
        StringBuilder escape = new StringBuilder("");
        for (String val : vals) {
            if (escape.length() > 0)
                escape.append("____");
            escape.append(val);
        }
        prefs.edit().putString(s, escape.toString()).apply();

    }

    /**
     * ATSK saves everything as strings.
     */
    final protected void putBoolean(final String s, final boolean def) {
        try {
            prefs.edit().putString(s, def ? "true" : "false").apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected FormObject(final Context c, final String key) {
        this.c = c;
        this.key = key;
        this.layoutInflater = LayoutInflater.from(c);
        this.prefs = PreferenceManager.getDefaultSharedPreferences(c);
    }

    protected abstract View getViewImpl();

    public final String getKey() {
        return key;
    }

    final public View getView() {
        View v = getViewImpl();
        if (v instanceof CheckBox) {
        } else if (v instanceof LinearLayout) {
        } else {
            v.setFocusableInTouchMode(true);
            v.setFocusable(false);
        }
        return v;
    }

    public abstract void commit();

    public abstract String getText();

    public static View horizontalFlow(Context c, View[] views) {
        FlowLayout ret = new FlowLayout(c);
        ret.setFocusableInTouchMode(true);

        for (View v : views) {
            ret.addView(v);
        }
        return ret;
    }

    public View editText(final String key, final int length) {
        final EditText et = new EditText(c);
        et.setFocusableInTouchMode(true);
        String text = prefs.getString(key, "");
        if (length > 0)
            et.setEms(length);
        et.setText(text);
        et.setSingleLine(true);
        et.addTextChangedListener(new TextWatcher() {
            public void onTextChanged(CharSequence s, int start, int before,
                    int count) {
            }

            public void beforeTextChanged(CharSequence s, int start, int count,
                    int after) {
            }

            public void afterTextChanged(Editable s) {
                prefs.edit().putString(key, s.toString()).apply();
            }
        });

        return et;
    }

}
