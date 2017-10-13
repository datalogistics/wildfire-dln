
package com.gmeci.atskservice.form.formobjects;

import com.gmeci.atskservice.R;
import android.widget.LinearLayout;

import android.view.View;
import android.content.Context;
import android.widget.TextView;
import android.view.View.OnClickListener;

public class Block extends FormObject {

    private String desc;
    private FormObject[] objects;

    public Block(final Context c, final String key, final String desc,
            final FormObject[] objects) {
        super(c, key);
        this.objects = objects;
        this.desc = desc;
    }

    @Override
    protected View getViewImpl() {
        LinearLayout v = (LinearLayout) layoutInflater.inflate(
                R.layout.vertical_block, null);

        TextView tv = (TextView) v.findViewById(R.id.title);
        tv.setText(desc);

        final LinearLayout sub =
                (LinearLayout) layoutInflater.inflate(R.layout.vertical, null);

        final TextView expand = (TextView) v.findViewById(R.id.expand);
        OnClickListener ll = new OnClickListener() {
            public void onClick(View v) {
                v.setSelected(!v.isSelected());
                putBoolean(key, v.isSelected());

                if (v.isSelected()) {
                    sub.setVisibility(View.VISIBLE);
                    expand.setText("\u25BC");
                } else {
                    sub.setVisibility(View.GONE);
                    expand.setText("\u25B6");
                }
            }
        };
        tv.setOnClickListener(ll);
        expand.setOnClickListener(ll);

        boolean vis = getBoolean(key, true);
        if (!vis) {
            sub.setVisibility(View.GONE);
            expand.setText("\u25B6");
        } else {
            expand.setText("\u25BC");
        }

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);

        layoutParams.setMargins(60, 5, 5, 5);

        for (FormObject object : objects) {
            sub.addView(object.getView());
        }
        v.addView(sub, layoutParams);

        return v;
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

    @Override
    public void commit() {
        for (FormObject object : objects) {
            object.commit();
        }
    }
}
