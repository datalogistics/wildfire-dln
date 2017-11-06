
package com.gmeci.atsk.obstructions.buttons;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.widget.EditText;

import com.atakmap.android.atsk.plugin.R;
import com.gmeci.atsk.resources.ATSKApplication;
import com.gmeci.core.ATSKConstants;
import com.gmeci.conversions.Conversions;

public class LRFConfigDialog {

    public static void ShowLRFConfigDialog(Context context) {
        final SharedPreferences gps_settings = PreferenceManager
                .getDefaultSharedPreferences(context);

        double Height_m = Float.parseFloat(gps_settings.getString(
                ATSKConstants.OBSTRUCTION_METHOD_LRF2GPS_OFFSET_HEIGHT_M, "2"));
        String HeightString = String.format("%.1f", Height_m * Conversions.M2F);

        final AlertDialog.Builder ad = new AlertDialog.Builder(context,
                android.R.style.Theme_Holo_Dialog);
        ad.setTitle("LRF Height above Ground(ft)");
        final EditText input = new EditText(context);
        input.setText(HeightString);

        input.setTextColor(ATSKConstants.LIGHT_BLUE);

        Context pluginContext = ATSKApplication
                .getInstance().getPluginContext();
        input.setBackgroundDrawable(pluginContext.getResources().getDrawable(
                R.drawable.fullborder_background));

        input.setInputType(InputType.TYPE_CLASS_NUMBER
                | InputType.TYPE_NUMBER_FLAG_DECIMAL
                | InputType.TYPE_NUMBER_FLAG_SIGNED);

        ad.setView(input);
        ad.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
                String HeightValue_ft = input.getText().toString();
                double Height_m = 0;
                try {
                    Height_m = (float) (Float.parseFloat(HeightValue_ft) / Conversions.M2F);
                } catch (NumberFormatException ex) {

                }
                gps_settings
                        .edit()
                        .putString(
                                ATSKConstants.OBSTRUCTION_METHOD_LRF2GPS_OFFSET_HEIGHT_M,
                                Height_m + "").apply();
            }
        });
        ad.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int arg1) {
                dialog.dismiss();
            }
        });

        ad.show();
    }
}
