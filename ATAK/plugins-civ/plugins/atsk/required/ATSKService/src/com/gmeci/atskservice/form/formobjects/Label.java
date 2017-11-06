
package com.gmeci.atskservice.form.formobjects;

import android.view.View;
import android.content.Context;
import android.widget.TextView;

public class Label extends FormObject {

    private final String simpleName;
    private final TextView tv;
    private final int length;

    public Label(final Context c, int length, String simpleName) {
        super(c, "label");
        this.length = length;
        this.simpleName = simpleName;

        tv = textView(simpleName);

        if (length > 0)
            tv.setEms(length);
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

}
