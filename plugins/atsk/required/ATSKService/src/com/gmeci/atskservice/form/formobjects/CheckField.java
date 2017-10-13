
package com.gmeci.atskservice.form.formobjects;

import com.gmeci.atskservice.R;

import android.widget.LinearLayout;

import android.view.View;
import android.content.Context;
import android.widget.EditText;
import android.widget.CheckBox;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.View.OnClickListener;

import java.util.*;

public class CheckField extends FormObject implements OnClickListener {

    private EditText tv;
    private final int length;
    private final String[] possibleValues;
    private final String simpleName;
    private final String sep;
    private final String uncheckedValue;

    public CheckField(final Context c,
            final String key,
            final int length,
            final String[] possibleValues,
            final String simpleName) {
        this(c, key, length, possibleValues, simpleName, "\n", "NONE");
    }

    public CheckField(final Context c,
            final String key,
            final int length,
            final String[] possibleValues,
            final String simpleName,
            final String sep, final String uncheckedValue) {
        super(c, key);
        this.length = length;
        this.possibleValues = possibleValues;
        this.simpleName = simpleName;
        this.sep = sep;
        this.uncheckedValue = uncheckedValue;

        tv = new EditText(c);
        tv.setEms(length);
        StringBuilder e = new StringBuilder("");
        Set<String> vals;
        int count = 0;
        try {
            vals = getStringSet(key, new HashSet<String>());
        } catch (Exception err) {
            vals = new HashSet<String>();
        }
        for (String value : possibleValues) {
            if (vals.contains(value)) {
                if (count != 0)
                    e.append(sep);
                count++;
                e.append(value);
            }
        }
        boolean b = getBoolean(key + "_custom_checked", false);
        if (b) {
            if (count != 0)
                e.append(sep);
            e.append(prefs.getString(key + "_custom", ""));
        }
        tv.setMinLines(1);
        if (e.length() == 0)
            tv.setText(uncheckedValue);
        else
            tv.setText(e);

        tv.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    onClick(v);
                    //v.clearFocus();
                }
            }
        });

        tv.setFocusableInTouchMode(true);

        tv.setOnClickListener(this);

    }

    @Override
    protected View getViewImpl() {
        return tv;
    }

    @Override
    public void commit() {
    }

    @Override
    public void onClick(View view) {
        final View ret = layoutInflater.inflate(R.layout.vertical_scrollable,
                null);
        LinearLayout content = (LinearLayout) ret.findViewById(R.id.content);
        Set<String> vals;
        try {
            vals = getStringSet(key, new HashSet<String>());
        } catch (Exception err) {
            vals = new HashSet<String>();
        }

        final CheckBox[] cboxes = new CheckBox[possibleValues.length];
        for (int i = 0; i < possibleValues.length; ++i) {
            CheckBox ch = new CheckBox(c);
            ch.setText(possibleValues[i]);
            if (vals.contains(possibleValues[i])) {
                ch.setChecked(true);
            }
            cboxes[i] = ch;
            content.addView(ch);

        }
        final CheckBox custom = new CheckBox(c);
        boolean b = getBoolean(key + "_custom_checked", false);
        custom.setChecked(b);
        custom.setText("Custom: ");

        content.addView(custom);

        final EditText et = (EditText) editText(key + "_custom", 10);
        et.setText(prefs.getString(key + "_custom", ""));
        content.addView(et);

        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(c);
        alertBuilder.setTitle(simpleName);
        alertBuilder.setView(ret)
                .setPositiveButton("Set",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                StringBuilder s = new StringBuilder("");
                                Set<String> save = new HashSet<String>();
                                int count = 0;
                                for (CheckBox cb : cboxes) {
                                    if (cb.isChecked()) {
                                        save.add(cb.getText().toString());
                                        if (count != 0)
                                            s.append(sep);
                                        count++;
                                        s.append(cb.getText());
                                    }
                                }
                                putStringSet(key, save);
                                prefs.edit()
                                        .putString(key + "_custom",
                                                et.getText().toString())
                                        .apply();
                                putBoolean(key + "_custom_checked",
                                        custom.isChecked());
                                if (custom.isChecked()) {
                                    if (count != 0)
                                        s.append(sep);
                                    s.append(et.getText().toString());
                                }

                                if (s.length() == 0)
                                    tv.setText(uncheckedValue);
                                else
                                    tv.setText(s);
                                tv.setMinLines(1);
                            }
                        }).setNegativeButton("Cancel", null);
        AlertDialog alert = alertBuilder.create();
        alert.show();

    }

    @Override
    public String getText() {
        return tv.getText().toString();
    }

}
