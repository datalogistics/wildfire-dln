
package com.gmeci.atsk.obstructions.obstruction;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;

import com.atakmap.android.atsk.plugin.R;
import com.gmeci.atsk.resources.ATSKApplication;
import com.gmeci.core.ATSKConstants;
import com.gmeci.conversions.Conversions;

public class TwoPPlusDSide_OffsetDialog extends DialogFragment {

    String CurrentDisplayFormat;
    TwoPPlusDInterface tppdi;
    boolean TopCollected = false;
    Button SaveButton, CancelButton;
    CheckBox FeetCheck, MetersCheck, SideLeftCheck, SideRightCheck;
    boolean SideRight = false;
    EditText OffsetDistance;

    public void Initialize(TwoPPlusDInterface tppdi, boolean TopCollected) {
        this.tppdi = tppdi;
        this.TopCollected = TopCollected;
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        Context pluginContext = ATSKApplication
                .getInstance().getPluginContext();

        View view = LayoutInflater.from(pluginContext).inflate(
                R.layout.twopplusddialog, container);
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        //getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        SideLeftCheck = (CheckBox) view.findViewById(R.id.side_left);
        SideLeftCheck.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton button, boolean checked) {
                if (checked) {
                    SideRight = false;
                    SideRightCheck.setChecked(false);
                }
            }
        });
        SideRightCheck = (CheckBox) view.findViewById(R.id.side_right);
        SideRightCheck
                .setOnCheckedChangeListener(new OnCheckedChangeListener() {
                    public void onCheckedChanged(CompoundButton button,
                            boolean checked) {
                        if (checked) {
                            SideRight = true;
                            SideLeftCheck.setChecked(false);
                        }
                    }
                });

        SideLeftCheck.setChecked(true);
        FeetCheck = (CheckBox) view.findViewById(R.id.units_feet);
        FeetCheck.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton button, boolean checked) {
                if (checked) {
                    String OldDisplayFormat = CurrentDisplayFormat;
                    CurrentDisplayFormat = ATSKConstants.UNITS_FEET;
                    if (!OldDisplayFormat.equals(CurrentDisplayFormat))
                        UpdateUnits();
                } else {
                    if (!MetersCheck.isChecked())
                        FeetCheck.setChecked(true);
                }
            }
        });
        MetersCheck = (CheckBox) view.findViewById(R.id.units_m);
        MetersCheck.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton button, boolean checked) {
                if (checked) {
                    String OldDisplayFormat = CurrentDisplayFormat;
                    CurrentDisplayFormat = ATSKConstants.UNITS_METERS;
                    if (!OldDisplayFormat.equals(CurrentDisplayFormat))
                        UpdateUnits();

                } else {
                    if (!FeetCheck.isChecked())
                        MetersCheck.setChecked(true);
                }
            }
        });

        OffsetDistance = (EditText) view.findViewById(R.id.offset);

        SaveButton = (Button) view.findViewById(R.id.save_button);
        SaveButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                String OffsetString = OffsetDistance.getText().toString();

                double Offset_m = 0;
                try {
                    Offset_m = Float.parseFloat(OffsetString);
                } catch (NumberFormatException ex) {
                }

                if (CurrentDisplayFormat.equals(ATSKConstants.UNITS_FEET))
                    Offset_m /= Conversions.M2F;
                tppdi.RangeDirectionSelected(Offset_m, SideRight, TopCollected);
                TwoPPlusDSide_OffsetDialog.this.dismiss();

            }
        });

        //Create and Set Cancel button Listener
        CancelButton = (Button) view.findViewById(R.id.cancel_button);
        CancelButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                TwoPPlusDSide_OffsetDialog.this.dismiss();
            }
        });

        CurrentDisplayFormat = ATSKConstants.UNITS_FEET;
        UpdateUnits();

        return view;
    }

    private void UpdateUnits() {
        boolean ft = CurrentDisplayFormat.equals(ATSKConstants.UNITS_FEET);
        FeetCheck.setChecked(ft);
        MetersCheck.setChecked(!ft);
    }

    public interface TwoPPlusDInterface {
        void RangeDirectionSelected(double Range_m, Boolean ToRight,
                boolean TopCollected);

    }
}
