
package com.gmeci.atsk.az.currentsurvey;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.widget.EditText;

import android.support.v4.app.FragmentManager;

import com.atakmap.android.atsk.plugin.R;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.gmeci.atsk.gallery.ATSKGalleryUtils;
import com.gmeci.atsk.resources.ATSKApplication;
import com.gmeci.core.ATSKConstants;
import com.gmeci.core.SurveyData;
import com.gmeci.atsk.az.farp.FARPInfoDlg;
import com.gmeci.atsk.az.hlz.HLZInfoDialog;
import com.gmeci.atsk.az.lz.RunwayInfoDialog;
import com.gmeci.atsk.resources.ATSKDialogManager;
import com.gmeci.atsk.resources.ATSKDialogManager.ConfirmInterface;
import com.gmeci.atskservice.resolvers.AZProviderClient;
import com.atakmap.coremap.log.Log;

public class SurveyOptionsDialog {

    private static final String TAG = "SurveyOptionsDialog";
    private final Context _context;
    private final AZProviderClient _azpc;
    private String _surveyUID = "";
    private final View _root;

    private AlertDialog _dialog;

    public void setAlertDialog(AlertDialog ad) {
        _dialog = ad;
    }

    final ConfirmInterface DeleteCBRConfirmInterface = new ConfirmInterface() {
        @Override
        public void ConfirmResponse(String Type, boolean Confirmed) {
            if (Confirmed) {
                // Delete survey from database
                _azpc.deleteAZ(_surveyUID);
                // Delete associated image gallery
                FileSystemUtils.deleteDirectory(
                        ATSKGalleryUtils.getImageDir(_surveyUID), false);
                // Update interface
                if (_surveyUID.equals(_azpc.getSetting(
                        ATSKConstants.CURRENT_SURVEY, TAG)))
                    handleCurrentSurveyDelete();
                closeDialog();
            }
        }
    };

    public View getView() {
        return _root;
    }

    public SurveyOptionsDialog(final Context context,
            final FragmentManager fragmentManager,
            final String surveyUID, final AZProviderClient azpc) {
        _surveyUID = surveyUID;
        _azpc = azpc;
        _context = context;

        Context pluginContext = ATSKApplication
                .getInstance().getPluginContext();
        LayoutInflater inflater = LayoutInflater.from(pluginContext);
        _root = inflater.inflate(
                R.layout.current_survey_options_dialog, null);

        final Button currentButton = (Button) _root
                .findViewById(R.id.current_button_current);
        currentButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                azpc.putSetting(ATSKConstants.CURRENT_SURVEY, _surveyUID,
                        "SurveyOptionsDlg");
                closeDialog();
            }
        });
        currentButton.setTextColor(ATSKConstants.LIGHT_BLUE);

        if (isCurrentSurvey())
            currentButton.setVisibility(View.GONE);

        final Button deleteButton = (Button) _root
                .findViewById(R.id.current_button_delete);
        deleteButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                try {
                    showDeleteAlertDialog(azpc.getAZName(_surveyUID));
                } catch (Exception e) {
                    closeDialog();
                }
            }
        });
        deleteButton.setTextColor(ATSKConstants.LIGHT_BLUE);

        final Button renameButton = (Button) _root
                .findViewById(R.id.current_button_rename);
        renameButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                try {
                    showRenameDialog(_surveyUID, azpc.getAZName(_surveyUID));
                } catch (Exception e) {
                    closeDialog();
                }
            }
        });
        renameButton.setTextColor(ATSKConstants.LIGHT_BLUE);

        final Button cloneButton = (Button) _root
                .findViewById(R.id.current_button_clone);
        cloneButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                try {
                    showCloneDialog(_surveyUID, azpc.getAZName(_surveyUID));
                } catch (Exception e) {
                    closeDialog();
                }
            }
        });
        cloneButton.setTextColor(ATSKConstants.LIGHT_BLUE);

        Button detailButton = (Button) _root
                .findViewById(R.id.current_button_details);
        detailButton.setTextColor(ATSKConstants.LIGHT_BLUE);
        detailButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                //need to get the current survey
                try {
                    if (!showAZDetails(azpc, _surveyUID, fragmentManager))
                        Toast.makeText(context, "Invalid Survey Selected",
                                Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    closeDialog();
                }
            }
        });

    }

    protected void showDeleteAlertDialog(String selectedSurveyName) {

        ATSKDialogManager.ShowConfirmDialog(_context, "Warning",
                " Would you like to delete survey: " + selectedSurveyName,
                "Delete", DeleteCBRConfirmInterface);
    }

    private void handleCurrentSurveyDelete() {

        final Cursor nextNewSurveyCursor = _azpc.getAllSurveys();
        if (nextNewSurveyCursor != null && nextNewSurveyCursor.getCount() > 0) {
            nextNewSurveyCursor.moveToFirst();
            _azpc.putSetting(ATSKConstants.CURRENT_SURVEY,
                    _azpc.getSurveyUID(nextNewSurveyCursor), "SurveyOptionsDlg");
        }
        if (nextNewSurveyCursor != null)
            nextNewSurveyCursor.close();
    }

    protected void showRenameDialog(final String uid, final String name) {

        AlertDialog.Builder builder = new AlertDialog.Builder(_context);
        final EditText et = new EditText(_context);
        et.setText(name);
        et.setSingleLine();
        builder.setTitle("Rename Survey").setView(et)
                .setPositiveButton("Rename",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                String newname = et.getText().toString();
                                if (!name.equals(newname)) {
                                    _dialog.setTitle(newname);
                                    _azpc.renameAZ(uid, newname);
                                }
                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {
                            }
                        });
        builder.create().show();
    }

    protected void showCloneDialog(final String uid, final String name) {

        AlertDialog.Builder builder = new AlertDialog.Builder(_context);
        builder.setTitle("Rename Survey")
                .setMessage("Are you sure you want to clone " + name)
                .setPositiveButton("Clone",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                SurveyData sd = _azpc.getAZ(uid, false);
                                if (sd != null) {
                                    sd.uid = java.util.UUID.randomUUID()
                                            .toString();
                                    sd.setSurveyName(sd.getSurveyName()
                                            + " Copy");
                                    boolean b = _azpc.NewAZ(sd);
                                    Log.d(TAG, "copy survey name complete: "
                                            + b);
                                }
                            }
                        })
                .setNeutralButton("Clone Inverse",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                SurveyData sd = _azpc.getAZ(uid, false);
                                if (sd != null) {
                                    sd.uid = java.util.UUID.randomUUID()
                                            .toString();
                                    sd.setSurveyName(sd.getSurveyName()
                                            + " Copy Inverse");
                                    sd.angle = sd.angle + 180;
                                    boolean b = _azpc.NewAZ(sd);
                                    Log.d(TAG, "copy survey name complete: "
                                            + b);
                                }
                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {
                            }
                        });
        builder.create().show();
    }

    private boolean isCurrentSurvey() {
        return _azpc.getSetting(ATSKConstants.CURRENT_SURVEY, TAG)
                .equals(_surveyUID);

    }

    private void closeDialog() {
        if (_dialog != null)
            _dialog.cancel();
    }

    public static boolean showAZDetails(AZProviderClient azpc,
            String surveyUID,
            FragmentManager manager) {
        SurveyData survey = azpc.getAZ(surveyUID, false);
        if (survey == null || survey.getType() == null)
            return false;
        if (survey.getType() == SurveyData.AZ_TYPE.LZ) {
            RunwayInfoDialog runwayInfoDialog = new RunwayInfoDialog();
            runwayInfoDialog.setupDialog(surveyUID, azpc, null);
            runwayInfoDialog.show(manager, "runwayInfoDialog");
        } else if (survey.getType() == SurveyData.AZ_TYPE.HLZ
                || survey.getType() == SurveyData.AZ_TYPE.DZ) {
            HLZInfoDialog InfoDialog = new HLZInfoDialog();
            InfoDialog.setupDialog(surveyUID, azpc, null);
            InfoDialog.show(manager, "hlzInfoDialog");
        } else if (survey.getType() == SurveyData.AZ_TYPE.FARP) {
            FARPInfoDlg InfoDialog = new FARPInfoDlg();
            InfoDialog.setupDialog(surveyUID, azpc, null);
            InfoDialog.show(manager, "farpInfoDialog");
        }
        return true;
    }

}
