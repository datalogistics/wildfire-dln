
package com.gmeci.atskservice.pdf;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.WindowManager;

import com.gmeci.core.ATSKConstants;
import com.gmeci.core.SurveyData;
import com.gmeci.core.SurveyData.AZ_TYPE;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.AcroFields;
import com.lowagie.text.pdf.AcroFields.Item;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.RandomAccessFileOrArray;

import com.gmeci.helpers.PolygonHelper;
import com.gmeci.conversions.Conversions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import harmony.java.awt.Color;

public abstract class AbstractPDFFiller {

    protected static final int NOTIFICATION_ID = 10102;
    protected static final String TAG = "AbstractPDFFiller";
    protected static final double METERS_2_FEET = Conversions.M2F;
    protected static final double METERS_2_YARDS = METERS_2_FEET / 3;
    private static final String ATSK_BLANK_PDF_FOLDER_BASE = "/atsk/blank_pdfs";

    public AcroFields fields;
    public PdfStamper stamper;
    NotificationManager mNotifyManager;
    Context context;
    PdfReader reader;
    SurveyData survey;
    Builder mBuilder;
    String FullPDFPath = "";
    String OutputFileName = "";

    public static final char DEGREE = '\u00B0';

    public AbstractPDFFiller(final Context context) {
        this.context = context;
        mNotifyManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
    }

    private static String getFullPDFPath(final SurveyData survey) {
        //make sure we have a folder with the right name...

        String sType = survey.getType().toString();
        String sName = survey.getSurveyName();
        File f = new File(ATSKConstants.ATSK_SURVEY_FOLDER_BASE
                + File.separator
                + sType + File.separator + sName, GetFileName(sName, sType));
        if (!f.getParentFile().mkdirs())
            Log.e(TAG, "Failed to create PDF path: " + f.getParent());
        return f.getAbsolutePath();
    }

    protected static String GetFileName(String surveyName,
            String surveyType) {
        return String.format("%s_%s_%s.pdf", surveyName, surveyType,
                DateFormat.format("yyyyMMdd'T'hhmm", new Date()));
    }

    public static void BuildTypeOfPDF(Context context,
            SurveyData currentSurvey) {

        Log.d(TAG, "building pdf");
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
                context);

        AbstractPDFFiller filler;

        if (currentSurvey.getType() == AZ_TYPE.FARP) {
            filler = new FARPPDFFiller(context);
        } else if (currentSurvey.getType() == AZ_TYPE.LZ
                && currentSurvey.surveyIsSTOL()) {
            filler = new STOLPDFFiller(context);
        } else if (currentSurvey.getType() == AZ_TYPE.LZ) {
            filler = new LZPDFFiller(context);
        } else if (currentSurvey.getType() == AZ_TYPE.HLZ) {
            filler = new HLZPDFFiller(context);
        } else if (currentSurvey.getType() == AZ_TYPE.DZ) {
            filler = new DZPDFFiller(context);
        } else {
            filler = new LZPDFFiller(context);
        }

        String file = null;

        try {
            file = filler.GeneratePDF(currentSurvey, mBuilder);
            Log.d(TAG, "finished building pdf: " + file);
            if (file != null)
                launchAdobe(context, file);
            else
                Log.e(TAG, "error building pdf file");
        } catch (Exception e) {
            Log.e(TAG, "error building pdf file: " + file, e);
        }

    }

    static public void launchAdobe(final Context context, final String file) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(new File(file)),
                    "application/pdf");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "error launching a pdf viewer", e);
        }
    }

    /**
     * Not exactly what I was hoping for, might revisit but remember to put
     * this permision back into the AndroidManifest.xml file.
     * android.permission.SYSTEM_ALERT_WINDOW
     */
    static public void showChooser(final Context context, final String file) {

        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(context);
        alertBuilder.setTitle("Post Production")
                .setMessage("PDF Generation has completed.")
                .setPositiveButton("Open in Adobe PDF",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                try {
                                    Intent intent = new Intent(
                                            Intent.ACTION_VIEW);
                                    intent.setDataAndType(
                                            Uri.fromFile(new File(file)),
                                            "application/pdf");
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    context.startActivity(intent);
                                } catch (Exception e) {
                                    Log.e(TAG, "error launching a pdf viewer",
                                            e);
                                }
                            }
                        })
                .setNeutralButton("Goto Folder",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                try {
                                    Intent intent = new Intent(
                                            Intent.ACTION_GET_CONTENT);
                                    Uri uri = Uri.parse(new File(file)
                                            .getParent());
                                    intent.setDataAndType(uri, "*/*");
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    context.startActivity(intent);
                                } catch (Exception e) {
                                    Log.e(TAG, "error launching a folder", e);
                                }
                            }
                        })
                .setNegativeButton("Continue",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {
                            }
                        });
        AlertDialog ad = alertBuilder.create();
        ad.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        ad.show();
    }

    final public String[] split(ArrayList<String> crParse, int number) {
        String[] retval = new String[] {
                "", ""
        };
        for (int i = 0; i < crParse.size(); ++i) {
            if (i < number)
                retval[0] += (crParse.get(i) + "\n");
            else
                retval[1] += (crParse.get(i) + "\n");
        }
        return retval;
    }

    private String GeneratePDF(SurveyData survey, Builder mBuilder) {
        if (survey.uid == null || survey.uid.length() < 1)
            survey.uid = "default";
        this.survey = survey;
        this.mBuilder = mBuilder;

        String fillablePDFPath = Environment.getExternalStorageDirectory()
                .getAbsolutePath()
                + ATSK_BLANK_PDF_FOLDER_BASE
                + File.separator
                + getBlankFileName(survey);

        Log.d(TAG, "constructing the blank pdf file: " + fillablePDFPath);

        mBuilder.setProgress(100, 10, false);
        mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());
        try {
            OutputFileName = SetupPDFBuilder(survey, mBuilder,
                    fillablePDFPath);

            fillPDFFromPreferences(survey);
            fillNonPreferenceParts();

            mBuilder.setProgress(100, 50, false);
            mBuilder.setContentText("Done Common PDF Filling");
            mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());

        } catch (IOException e) {
            Log.e(TAG, "error occurred writing the PDF file", e);
            return null;
        } catch (DocumentException e) {
            Log.e(TAG, "error occurred writing the PDF file", e);
            return null;
        } finally {
            mNotifyManager.cancel(NOTIFICATION_ID);
            try {
                stamper.close();
            } catch (Exception e) {
                Log.e(TAG, "error occurred closing the stamper", e);
            }
        }
        return OutputFileName;
    }

    private void fillPDFFromPreferences(final SurveyData survey)
            throws IOException, DocumentException {

        Map<String, Object> data = survey.data;

        // ensure the sitename entry populates the siteName2 entry
        if (data.containsKey("siteName"))
            data.put("siteName2", data.get("siteName"));

        Iterator<Entry<String, Object>> it = survey.data
                .entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, Object> next = it.next();
            final String currentKey = next.getKey();
            final Object val = next.getValue();

            // only use string values
            if (val instanceof String) {
                String currentVal = (String) next.getValue();

                if (currentVal.equalsIgnoreCase("null")) {
                    currentVal = "";
                }
                final String msg = " setting form fields '" + currentKey +
                        "' with '" + currentVal + "'";
                if (!fields.setField(currentKey, currentVal)) {
                    Log.e(TAG, "failed " + msg);
                } else {
                    Log.d(TAG, "success " + msg);
                }
            }
        }
    }

    private String SetupPDFBuilder(SurveyData survey, Builder mBuilder,
            String fillablePDFPath)
            throws IOException, DocumentException {

        reader = new PdfReader(new RandomAccessFileOrArray(fillablePDFPath,
                true, false), null);

        FullPDFPath = getFullPDFPath(survey);

        FileOutputStream pdf2BeFilledFOS = new FileOutputStream(FullPDFPath);
        stamper = new PdfStamper(reader, pdf2BeFilledFOS, '\0', true);
        mBuilder.setProgress(100, 30, false);
        mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());
        fields = stamper.getAcroFields();
        mBuilder.setProgress(100, 40, false);
        mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());
        return FullPDFPath;

    }

    String GetGradientString(final double gradient) {
        // TODO: What is 'gradient' supposed to be? slope, degrees, or radians?
        // atan() means we're converting a slope to radians but the percentage
        // formatting implies we want to convert to a slope, not from one
        //double d = Math.atan(gradient);
        String sign = "";
        if (gradient > 0)
            sign = "+";
        return String.format("%s%.1f%%", sign, gradient);
    }

    /**
     *  Provided a value usually in fractional feet, provide the correct integer value erroring
     *  on the side of safety.
     * @param f Value in feet
     * @return Rounded value as string
     */
    final String toRoundString(final double f) {
        return String.valueOf((int) Math.ceil(f - Conversions.THRESH));
    }

    final protected boolean set(final String name, String value)
            throws IOException, DocumentException {
        if (value == null || value.equalsIgnoreCase("null"))
            value = "";
        if (!fields.setField(name, value)) {
            Log.e(TAG, "failed to write: " + name + " with value: " + value);
            return false;
        }
        return true;
    }

    protected abstract String getBlankFileName(SurveyData surveyData);

    abstract protected void fillNonPreferenceParts() throws IOException,
            DocumentException;

    protected String GetApproval(final String ApprovalKey) {
        if (survey.data.containsKey(ApprovalKey)) {
            String ApprovalString = (String) survey.data
                    .get(ApprovalKey);
            if (ApprovalString != null) {
                boolean Approved = ApprovalString.compareToIgnoreCase("true") == 0;
                if (Approved)
                    return "A";
                else
                    return "D";
            }
        }
        return " ";
    }

    protected String getGridZone() {
        double RealCenter[] = Conversions.AROffset(survey.center.lat,
                survey.center.lon, survey.angle,
                PolygonHelper.GetOverrunOffset(false, survey));

        String GridZone = Conversions.GetMGRS(RealCenter[0],
                RealCenter[1]);
        if (GridZone != null && GridZone.length() > 3) {
            if (!Character.isDigit(GridZone.charAt(2)))
                GridZone = GridZone.substring(0, 3);
            else
                GridZone = GridZone.substring(0, 2);
        }
        return GridZone;
    }

    final protected String getYdsMetersString(final double distance) {
        return String.format(" %s yds / %s m",
                toRoundString(distance * METERS_2_YARDS),
                toRoundString(distance));

    }

    final protected String getAngleAndInverseString(final double angle) {
        //return String.format(" %.1f\u00b0 / %.1f\u00b0",
        //        FixAngle(angle), FixAngle(angle + 180));
        return String.format(" %.1f / %.1f",
                Conversions.deg360(angle), Conversions.deg360(angle + 180));
    }

    final protected String getAngleString(final double angle) {
        //return String.format(" %.1f\u00b0", FixAngle(angle));
        return String.format(" %.1f", Conversions.deg360(angle));
    }

    final protected String getFeetMSLString(final double lat, final double lon,
            final double elevation) {
        return String.format(
                "%s ft MSL",
                toRoundString(Conversions.ConvertHAEtoMSL(
                        lat, lon, elevation) * Conversions.M2F));
    }

    final protected String getFeetString(final double meters) {
        return String.format("%d ft",
                (int) Math.ceil(meters * METERS_2_FEET - Conversions.THRESH));
    }

    /**
     * Runway numbers and letters are determined from the approach direction.
     * The runway number is the whole number nearest one-tenth the magnetic azimuth
     * of the centerline of the runway, measured clockwise from the magnetic north.
     * If 00, then the runway should be 36.
     */
    final protected String getRunwayName(final double angle, final double lat,
            final double lon) {
        int Heading_Mod10 = (int) Math.round((Conversions.GetMagAngle(
                Conversions.deg360(angle), lat, lon) / 10f));
        if (Heading_Mod10 < 0)
            Heading_Mod10 += 36;
        if (Heading_Mod10 == 0)
            Heading_Mod10 = 36;

        return String.format("%02d", Heading_Mod10);
    }

}
