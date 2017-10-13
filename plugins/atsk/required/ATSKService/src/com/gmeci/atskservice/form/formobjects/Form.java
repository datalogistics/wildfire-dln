
package com.gmeci.atskservice.form.formobjects;

import com.gmeci.atskservice.R;

import android.widget.LinearLayout;

import android.view.View;
import android.content.Context;

public class Form extends FormObject {

    private FormObject[] objects;

    public Form(final Context c, final FormObject[] objects) {
        super(c, "form");
        this.objects = objects;
    }

    @Override
    protected View getViewImpl() {
        LinearLayout v = (LinearLayout) layoutInflater.inflate(
                R.layout.vertical, null);
        for (FormObject object : objects) {
            v.addView(object.getView());
        }
        return v;
    }

    @Override
    public void commit() {
        for (FormObject object : objects) {
            object.commit();
        }
    }

    @Override
    public String getText() {
        StringBuilder v = new StringBuilder("");
        for (FormObject object : objects) {
            String text = object.getText();
            if (text.length() > 0) {
                v.append(text);
                v.append("\n");
            }
        }
        return v.toString();
    }

}
