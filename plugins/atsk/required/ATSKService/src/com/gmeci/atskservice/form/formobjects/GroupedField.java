
package com.gmeci.atskservice.form.formobjects;

import com.gmeci.atskservice.R;

import android.widget.LinearLayout;

import android.view.View;
import android.view.ViewGroup;
import android.content.Context;

public class GroupedField extends FormObject {

    private final FormObject[] objects;
    private final boolean wrap;

    public GroupedField(final Context c,
            final FormObject[] objects,
            final boolean wrap) {
        super(c, "grouped");
        this.objects = objects;
        this.wrap = wrap;
    }

    @Override
    public void commit() {
        for (FormObject object : objects)
            object.commit();
    }

    @Override
    protected View getViewImpl() {
        ViewGroup v;
        if (wrap) {
            v = new FlowLayout(c);
        } else {
            v = (LinearLayout) layoutInflater
                    .inflate(R.layout.horizontal, null);
        }

        for (FormObject object : objects)
            v.addView(object.getView());

        return v;
    }

    @Override
    public String getText() {
        StringBuilder v = new StringBuilder("");
        for (FormObject object : objects)
            v.append(object.getText());
        v.append("\n");
        return v.toString();
    }

}
