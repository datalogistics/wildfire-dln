
package com.gmeci.atskservice.form.formobjects;

import com.gmeci.atskservice.R;
import android.widget.LinearLayout;

import android.view.View;
import android.content.Context;

public class NestBlock extends FormObject {

    private final FormObject parent;
    private final FormObject[] children;

    public NestBlock(final Context c, final String key,
            final FormObject parent, final FormObject[] children) {
        super(c, key);
        this.parent = parent;
        this.children = children;
    }

    @Override
    protected View getViewImpl() {
        LinearLayout v = (LinearLayout) layoutInflater.inflate(
                R.layout.vertical, null);

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);

        final LinearLayout sub =
                (LinearLayout) layoutInflater.inflate(R.layout.vertical, null);

        v.addView(parent.getView());

        layoutParams.setMargins(15, 5, 5, 5);

        for (FormObject object : children) {
            sub.addView(object.getView());
        }
        v.addView(sub, layoutParams);

        return v;
    }

    @Override
    public String getText() {
        StringBuilder v = new StringBuilder(fixMultiline(parent.getText()));

        for (FormObject object : children) {
            final String text = object.getText();
            if (text.length() > 0) {
                v.append("\n");
                v.append("\t\t");
                v.append(fixMultiline(text));
            }
        }
        return v.toString();
    }

    private String fixMultiline(String line) {
        if (line.endsWith("\n"))
            line = line.substring(0, line.length() - 1);
        return line.replaceAll("\n", "\n\t\t");
    }

    @Override
    public void commit() {
        parent.commit();
        for (FormObject object : children) {
            object.commit();
        }
    }
}
