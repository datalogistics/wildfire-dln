
package com.gmeci.atskservice.form.formobjects;

import com.gmeci.atskservice.R;

import android.view.View;
import android.content.Context;

public class Break extends FormObject {

    final boolean filled;

    public Break(final Context c, final boolean filled) {
        super(c, "break");
        this.filled = filled;
    }

    @Override
    protected View getViewImpl() {

        View v = layoutInflater.inflate(R.layout.visualbreak, null);
        if (!filled)
            v.setVisibility(View.INVISIBLE);
        return v;
    }

    public void commit() {
    }

    @Override
    public String getText() {
        return "\n";
    }

}
