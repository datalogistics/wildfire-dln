
package com.gmeci.atskservice.form.formobjects;

import android.view.View;
import android.content.Context;
import android.widget.CheckBox;

public class SingleCheckField extends FormObject {

    private final CheckBox cb;

    public SingleCheckField(final Context c, final String key, String simpleName) {
        super(c, key);
        cb = new CheckBox(c);
        cb.setText(simpleName);
        boolean b = getBoolean(key, false);
        cb.setChecked(b);
        cb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                if (v instanceof CheckBox) {
                    final boolean b = ((CheckBox) v).isChecked();
                    putBoolean(key, b);
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
        return cb.getText().toString() + " " + cb.isChecked();
    }

}
