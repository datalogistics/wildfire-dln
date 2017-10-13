
package com.gmeci.atsk.az.hlz;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;

import com.atakmap.android.atsk.plugin.R;
import com.gmeci.atsk.resources.ATSKApplication;

public class ApproachDepartureFormatDialog extends DialogFragment {

    private static final String MAG = "MAG";
    private View _root;
    private LayoutInflater _inflater;

    private RadioGroup _formatGroup;
    private RadioButton _magButton, _trueButton;
    private ApproachDepartureFormatInterface _parent;

    private String _format;

    public void Initialize(ApproachDepartureFormatInterface parent,
            String currentFormat) {
        _parent = parent;
        _format = currentFormat;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        Context pluginContext = ATSKApplication
                .getInstance().getPluginContext();
        _inflater = LayoutInflater.from(pluginContext);
        _root = inflater.inflate(R.layout.appr_dep_format_dialog_view,
                container, false);
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        return _root;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        _magButton = (RadioButton) _root.findViewById(R.id.mag_button);
        _trueButton = (RadioButton) _root.findViewById(R.id.true_button);
        _formatGroup = (RadioGroup) _root.findViewById(R.id.choice_group);
        if (_format.equals(MAG))
            _magButton.setChecked(true);
        else
            _trueButton.setChecked(true);
        _formatGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                RadioButton btn = (RadioButton) group.findViewById(checkedId);
                _parent.FormatChanged(btn.getText().toString());
            }

        });

        _magButton.setButtonDrawable(R.drawable.checkbox_selector);
        _trueButton.setButtonDrawable(R.drawable.checkbox_selector);
    }

    public interface ApproachDepartureFormatInterface {
        void FormatChanged(String format);

        void HandJammed(String RAB);
    }
}
