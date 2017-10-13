
package com.gmeci.atskservice.form.formobjects;

import com.gmeci.atskservice.R;

import android.view.View;
import android.content.Context;
import android.widget.TextView;

public class ScreenHint {

    public static class NewLine extends FormObject {

        public NewLine(final Context c) {
            super(c, "nl");
        }

        @Override
        protected View getViewImpl() {
            return new FlowLayout.ForceBreak(c);
        }

        @Override
        public void commit() {
        }

        @Override
        public String getText() {
            return "";
        }
    }

    public static class VisualLabel extends FormObject {

        TextView te;

        public VisualLabel(final Context c, String label) {
            super(c, "visuallabel");
            te = textView(label);
        }

        @Override
        protected View getViewImpl() {
            return te;
        }

        @Override
        public void commit() {
        }

        @Override
        public String getText() {
            return "";
        }
    }

    static public class Break extends FormObject {

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
            return "";
        }
    }

}
