
package com.gmeci.atsk.export;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.atakmap.android.atsk.plugin.R;
import com.gmeci.atsk.gallery.ATSKGalleryExportDialog;
import com.gmeci.atsk.resources.ATSKApplication;
import com.gmeci.core.ATSKConstants;
import com.gmeci.atsk.resources.ATSKBaseFragment;
import com.gmeci.atsk.resources.ServiceConnectionManagerInterface;
import com.atakmap.coremap.log.Log;
import com.gmeci.core.SurveyPoint;

public class ATSKExportFragment extends ATSKBaseFragment implements
        ServiceConnectionManagerInterface {

    Button share;
    Button save;
    Button surveyExport;

    Button saveGallery;

    Button imageSurveyCapture;
    private View mView;

    Context atakContext;
    Context pluginContext;

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        pluginContext = ATSKApplication
                .getInstance().getPluginContext();
        atakContext = ATSKApplication
                .getInstance().getATAKContext();
        mView = LayoutInflater.from(pluginContext).inflate(
                R.layout.export_fragment_view, container,
                false);

        surveyExport = (Button) mView.findViewById(R.id.export_survey);
        share = (Button) mView.findViewById(R.id.share);
        save = (Button) mView.findViewById(R.id.save);
        saveGallery = (Button) mView.findViewById(R.id.save_gallery);
        imageSurveyCapture = (Button) mView.findViewById(R.id.image_cap_button);

        SetupButtonListeners();
        return mView;
    }

    public void exportFormat(final boolean file) {

        LayoutInflater layoutInflater = LayoutInflater.from(pluginContext);

        final View exportDialog = layoutInflater.inflate(
                R.layout.export_dialog_other, null);

        final RadioGroup rGroup = (RadioGroup) exportDialog
                .findViewById(R.id.choice_group);

        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(atakContext);
        alertBuilder
                .setTitle("Export");
        alertBuilder.setView(exportDialog)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                final int id = rGroup.getCheckedRadioButtonId();
                                final String uid = azpc.getSetting(
                                        ATSKConstants.CURRENT_SURVEY, TAG);
                                if (id == R.id.kml_format) {
                                    Log.d(TAG, "kml selection");
                                    if (file) {
                                        ATSKMissionPackageManager.getInstance()
                                                .saveSurveyAsKML(uid);
                                    } else {
                                    }
                                } else if (id == R.id.xml_format) {
                                    Log.d(TAG, "xml");
                                    if (file) {
                                        ATSKMissionPackageManager.getInstance()
                                                .saveSurveyAsXML(uid);
                                    } else {
                                    }
                                } else if (id == R.id.shp_format) {
                                    Log.d(TAG, "SHP");
                                    if (file)
                                        ATSKMissionPackageManager.getInstance()
                                                .saveSurveyAsSHP(uid);
                                } else {
                                    Log.d(TAG, "invalid selection");
                                }
                            }
                        }).setNegativeButton("Cancel", null);
        AlertDialog alert = alertBuilder.create();
        alert.show();

    }

    public void exportNative(final boolean file) {
        LayoutInflater layoutInflater = LayoutInflater.from(pluginContext);

        final View v = layoutInflater.inflate(R.layout.export_dialog, null);

        final CheckBox surveyCB = (CheckBox) v.findViewById(R.id.survey);
        final CheckBox obsCB = (CheckBox) v.findViewById(R.id.obstacles);
        final CheckBox gradCB = (CheckBox) v.findViewById(R.id.gradients);

        AlertDialog.Builder b = new AlertDialog.Builder(atakContext);
        b.setTitle(file ? "Save to File" : "Share over Mission Package");
        b.setView(v);
        b.setPositiveButton("OK", null);
        b.setNegativeButton("Cancel", null);
        final AlertDialog d = b.create();
        d.show();
        d.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(
                new OnClickListener() {
            @Override
            public void onClick(View v) {
                final String surveyUID = azpc.getSetting(ATSKConstants
                        .CURRENT_SURVEY, TAG);
                if (!surveyCB.isChecked() && !gradCB.isChecked()
                        && !obsCB.isChecked()) {
                    Toast.makeText(getActivity(), "No items selected",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                if (file)
                    ATSKMissionPackageManager.getInstance().save(surveyUID,
                            surveyCB.isChecked(), gradCB.isChecked(),
                            obsCB.isChecked());
                else
                    ATSKMissionPackageManager.getInstance().send(surveyUID,
                            surveyCB.isChecked(), gradCB.isChecked(),
                            obsCB.isChecked());
                d.dismiss();
            }
        });
    }

    private void SetupButtonListeners() {
        surveyExport.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                exportFormat(true);
            }
        });

        share.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                exportNative(false);
            }
        });
        save.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                exportNative(true);
            }
        });
        saveGallery.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                final String surveyUID =
                        azpc.getSetting(ATSKConstants.CURRENT_SURVEY, TAG);
                ATSKGalleryExportDialog dialog = new ATSKGalleryExportDialog(
                        atakContext, pluginContext, surveyUID);
                dialog.show();
            }
        });
        imageSurveyCapture.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ATSKMissionPackageManager.getInstance().imageSurveyCapture();
            }
        });

    }

    public void GotATSKServiceHandle() {
    }

    @Override
    public void shotApproved(SurveyPoint sp, double range_m, double az_deg,
            double el_deg, boolean TopCollected) {
    }

    @Override
    public void GotHardwareHandle() {

    }

}
