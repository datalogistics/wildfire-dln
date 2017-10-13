
package com.gmeci.atskservice.form.formobjects;

import com.gmeci.atskservice.R;
import android.widget.LinearLayout;

import android.view.View;
import android.content.Context;

public class DependsBlock extends FormObject {

    public interface Decider {
        boolean include();
    }

    private final FormObject[] children;
    private final Decider decider;

    public DependsBlock(final Context c, final String key,
            final FormObject[] children, Decider decider) {
        super(c, key);
        this.children = children;
        this.decider = decider;
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

        for (FormObject object : children) {
            sub.addView(object.getView());
        }
        v.addView(sub, layoutParams);

        return v;
    }

    @Override
    public String getText() {
        if (!decider.include())
            return "";

        StringBuilder v = new StringBuilder("");
        for (FormObject object : children) {
            String text = object.getText();
            if (text.length() > 0) {
                v.append(text);
                //v.append("\n");
            }
        }
        return v.toString();
    }

    @Override
    public void commit() {
        for (FormObject object : children) {
            object.commit();
        }
    }
}
