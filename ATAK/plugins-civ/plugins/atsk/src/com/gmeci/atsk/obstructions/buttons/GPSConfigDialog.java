
package com.gmeci.atsk.obstructions.buttons;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.widget.EditText;

import com.gmeci.core.ATSKConstants;
import com.gmeci.conversions.Conversions;

public class GPSConfigDialog {

    private static final double getFloat(SharedPreferences sp, String key,
            double dv) {
        try {
            return Float.parseFloat(sp.getString(key, dv + ""));
        } catch (NumberFormatException e) {
            return dv;
        }
    }

    public static void ShowGPSConfigDialog(Context context) {

        final SharedPreferences gps_settings = PreferenceManager
                .getDefaultSharedPreferences(context);
        double Height_m = getFloat(gps_settings,
                ATSKConstants.OBSTRUCTION_METHOD_GPS_HEIGHT_M, 2);

        String HeightString = String.format("%.1f", Height_m * Conversions.M2F);

        final AlertDialog.Builder ad = new AlertDialog.Builder(context,
                android.R.style.Theme_Holo_Dialog);
        ad.setTitle("Select GPS Height (ft)");
        final EditText input = new EditText(context);
        input.setText(HeightString);

        input.setTextColor(0xFF000000);
        input.setBackgroundColor(0xFFFFFFFF);
        input.setInputType(InputType.TYPE_CLASS_NUMBER
                | InputType.TYPE_NUMBER_FLAG_DECIMAL
                | InputType.TYPE_NUMBER_FLAG_SIGNED);

        ad.setView(input);
        ad.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int arg1) {
                dialog.dismiss();
            }
        });
        ad.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();

                double Offset_m = 0;
                try {
                    Offset_m = (float) (Float.parseFloat(input.getText()
                            .toString()) / Conversions.M2F);
                } catch (NumberFormatException ex) {

                }
                gps_settings
                        .edit()
                        .putString(
                                ATSKConstants.OBSTRUCTION_METHOD_GPS_HEIGHT_M,
                                Offset_m + "").apply();
            }
        });

        ad.show();
    }
}
