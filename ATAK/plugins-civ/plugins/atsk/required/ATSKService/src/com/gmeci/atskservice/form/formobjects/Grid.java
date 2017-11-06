
package com.gmeci.atskservice.form.formobjects;

import android.widget.GridLayout;

import android.view.View;
import android.content.Context;

public class Grid extends FormObject {

    FormObject[] objects;
    View v;

    public Grid(final Context c, FormObject[] objects, int columns) {
        super(c, "grid");
        this.objects = objects;
        v = horizontal(columns);
    }

    public View horizontal(int columns) {
        GridLayout gridLayout = new GridLayout(c);

        int row = 0;
        for (int i = 0; i < objects.length; ++i) {
            if (i != 0 && (i % columns) == 0)
                row++;
            GridLayout.Spec columnSpec = GridLayout.spec(i % columns,
                    GridLayout.BASELINE);
            GridLayout.Spec rowSpec = GridLayout.spec(row);

            gridLayout.addView(objects[i].getView(),
                    new GridLayout.LayoutParams(rowSpec, columnSpec));
        }
        return gridLayout;
    }

    @Override
    protected View getViewImpl() {
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
            v.append(object.getText());
            v.append("\n");
        }
        return v.toString();
    }

}
