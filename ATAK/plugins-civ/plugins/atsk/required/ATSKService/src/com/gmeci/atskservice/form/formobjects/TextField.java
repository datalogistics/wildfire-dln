
package com.gmeci.atskservice.form.formobjects;

import android.view.View;
import android.content.Context;
import android.widget.TextView;
import android.widget.EditText;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.View.OnClickListener;
import android.graphics.Color;

public class TextField extends FormObject implements OnClickListener {

    private final String simpleName;
    private final EditText et;
    private final int length;
    private final boolean multiline;

    private void fill(final TextView tv, final String key) {
        String text = prefs.getString(key + "_override", null);
        String text_orig = prefs.getString(key, null);
        if (text == null || text.length() == 0
                || text.equalsIgnoreCase(text_orig)) {
            tv.setTextColor(Color.parseColor("#ffffff"));
            text = prefs.getString(key, "");
        } else {
            tv.setTextColor(Color.parseColor("#FFCCCC"));
        }
        tv.setText(text);
    }

    public TextField(final Context c, final String key, final int length,
            String simpleName) {
        this(c, key, length, simpleName, false);
    }

    public TextField(final Context c, final String key, final int length,
            String simpleName, final boolean multiline) {
        super(c, key);

        this.length = length;
        this.simpleName = simpleName;
        this.multiline = multiline;

        et = new EditText(c);
        if (length > 0)
            et.setEms(length);
        fill(et, key);

        et.setOnClickListener(this);
    }

    @Override
    protected View getViewImpl() {
        return et;
    }

    @Override
    public void onClick(View view) {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(c);
        alertBuilder
                .setTitle(simpleName);
        TextView o = (TextView) editText(key + "_override", -1);
        if (multiline) {
            o.setSingleLine(false);
            o.setHorizontalScrollBarEnabled(false);
        }

        alertBuilder
                .setView(o)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                fill(et, key);
                            }
                        })
                .setNeutralButton("Reset",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                prefs.edit().remove(key + "_override").commit();
                                fill(et, key);
                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {
                            }
                        });
        o.setText(prefs.getString(key + "_override", prefs.getString(key, "")));
        AlertDialog alert = alertBuilder.create();
        alert.show();
    }

    public void commit() {
        String val = prefs.getString(key + "_override",
                prefs.getString(key, ""));
        prefs.edit().remove(key + "_override").commit();
        prefs.edit().putString(key, val).commit();
    }

    @Override
    public String getText() {
        return et.getText().toString();
    }

}
