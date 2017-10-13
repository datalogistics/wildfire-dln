
package com.gmeci.atskservice.form.formobjects;

import com.gmeci.atskservice.R;

import android.widget.LinearLayout;

import android.view.View;
import android.content.Context;

public class SimpleNameValue extends FormObject {

    private String longDesc;
    private String shortDesc;
    private TextField tv;

    public SimpleNameValue(final Context c, final String key, final String desc) {
        this(c, key, desc, desc, false);
    }

    public SimpleNameValue(final Context c, final String key,
            final String desc, boolean multiline) {
        this(c, key, desc, desc, multiline);
    }

    public SimpleNameValue(final Context c,
            final String key,
            final String longDesc,
            final String shortDesc, boolean multiline) {
        super(c, key);
        this.longDesc = longDesc;
        this.shortDesc = shortDesc;
        this.tv = new TextField(c, key, -1, shortDesc, multiline);
    }

    @Override
    public void commit() {
        tv.commit();
    }

    @Override
    public View getViewImpl() {
        LinearLayout v = (LinearLayout) layoutInflater.inflate(
                R.layout.vertical, null);

        v.addView(textView(longDesc));
        v.addView(tv.getView());
        return v;
    }

    @Override
    public String getText() {
        return longDesc + tv.getText() + "\n";
    }

}
