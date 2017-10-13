
package com.gmeci.atskservice.form.formobjects;

import com.gmeci.atskservice.R;

import android.widget.LinearLayout;

import android.view.View;
import android.content.Context;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.RadioButton;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.View.OnClickListener;

public class RadioField extends FormObject implements OnClickListener {

    private EditText tv;
    private final int length;
    private final String[] possibleValues;
    private final String simpleName;

    public RadioField(final Context c,
            final String key,
            final int length,
            final String[] possibleValues, final String simpleName) {
        super(c, key);
        this.length = length;
        this.possibleValues = possibleValues;
        this.simpleName = simpleName;

        tv = new EditText(c);
        tv.setEms(length);
        String val = prefs.getString(key, "");
        for (int i = 0; i < possibleValues.length; ++i) {
            if (val.equalsIgnoreCase(possibleValues[i])) {
                tv.setText(possibleValues[i]);
            }
        }
        tv.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    onClick(v);
                    //tv.clearFocus();
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
    public void onClick(View view) {
        final ScrollView scrollView = (ScrollView) layoutInflater.inflate(
                R.layout.vertical_scrollable, null);
        final LinearLayout ret = (LinearLayout) scrollView
                .findViewById(R.id.content);
        RadioGroup rg = new RadioGroup(c);
        String val = prefs.getString(key, "");
        final RadioButton[] rbList = new RadioButton[possibleValues.length];
        boolean found = false;
        for (int i = 0; i < rbList.length; ++i) {
            rbList[i] = new RadioButton(c);
            rbList[i].setText(possibleValues[i]);
            rg.addView(rbList[i]);
            if (val.equalsIgnoreCase(possibleValues[i])) {
                found = true;
                rbList[i].setChecked(true);
            }
        }
        final RadioButton custom = new RadioButton(c);
        custom.setText("Custom: ");
        rg.addView(custom);
        ret.addView(rg);
        final EditText et = (EditText) editText(key + "_custom", -1);
        if (!found) {
            custom.setChecked(true);
            et.setText(val);
        } else {
            et.setText("");
        }

        ret.addView(et);

        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(c);
        alertBuilder
                .setTitle("Override " + simpleName);
        alertBuilder.setView(scrollView)
                .setPositiveButton("Set",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                for (RadioButton rb : rbList) {
                                    if (rb.isChecked()) {
                                        String s = rb.getText().toString();
                                        tv.setText(s);
                                        prefs.edit().putString(key, s).commit();
                                    }
                                }
                                if (custom.isChecked()) {
                                    String s = et.getText().toString();
                                    prefs.edit().putString(key, s).commit();
                                    tv.setText(s);
                                }
                            }
                        }).setNegativeButton("Cancel", null);
        AlertDialog alert = alertBuilder.create();
        alert.show();
    }

    @Override
    public void commit() {
    }

    @Override
    public String getText() {
        return tv.getText().toString();
    }

}
