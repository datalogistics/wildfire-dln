
package com.gmeci.atsk.gallery;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;

import com.atakmap.android.atsk.plugin.R;
import com.gmeci.atsk.export.ATSKMissionPackageManager;

import java.io.File;

/**
 * Dialog for exporting images from the gallery
 */
public class ATSKGalleryExportDialog implements DialogInterface {

    private static final String TAG = "ATSKGalleryExportDialog";
    private static final String GALLERY_MARKUP_PREF = "atsk_gallery_export_markup";
    private static final String GALLERY_PDF_PREF = "atsk_gallery_export_pdf";

    private final Context _context, _plugin;
    private final String _surveyUID;
    private final File[] _images;
    private AlertDialog _dialog;

    public ATSKGalleryExportDialog(Context context, Context plugin,
            String surveyUID) {
        this(context, plugin, surveyUID, null);
    }

    public ATSKGalleryExportDialog(Context context, Context plugin,
            String surveyUID, File[] images) {
        _context = context;
        _plugin = plugin;
        _surveyUID = surveyUID;
        _images = images;
    }

    public void show() {
        LayoutInflater layoutInflater = LayoutInflater.from(_plugin);
        final View dialogView = layoutInflater.inflate(
                R.layout.gallery_export_dialog, null);
        final CheckBox exportPDF = (CheckBox) dialogView
                .findViewById(R.id.cb_pdf_export);
        final CheckBox genMarkup = (CheckBox) dialogView
                .findViewById(R.id.cb_gen_markup);

        // Apply saved choices
        final SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(_context);
        genMarkup.setChecked(prefs.getBoolean(GALLERY_MARKUP_PREF, false));
        exportPDF.setChecked(prefs.getBoolean(GALLERY_PDF_PREF, false));

        AlertDialog.Builder adb = new AlertDialog.Builder(_context);
        adb.setTitle("Gallery Export");
        adb.setView(dialogView);
        adb.setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog,
                            int which) {
                        // Save choices
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putBoolean(GALLERY_MARKUP_PREF,
                                genMarkup.isChecked());
                        editor.putBoolean(GALLERY_PDF_PREF,
                                exportPDF.isChecked());
                        editor.apply();
                        // Begin export
                        ATSKMissionPackageManager.getInstance().saveGallery(
                                _surveyUID, _images, true,
                                genMarkup.isChecked(),
                                exportPDF.isChecked());
                        dismiss();
                    }
                });
        adb.setNegativeButton("Cancel", null);
        _dialog = adb.show();
    }

    @Override
    public void dismiss() {
        if (_dialog != null)
            _dialog.dismiss();
        _dialog = null;
    }

    @Override
    public void cancel() {
        if (_dialog != null)
            _dialog.cancel();
        _dialog = null;
    }
}
