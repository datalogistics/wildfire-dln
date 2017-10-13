
package com.gmeci.atskservice.form;

import com.gmeci.atskservice.pdf.*;
import com.gmeci.atskservice.R;

import com.gmeci.atskservice.resolvers.AZURIConstants;
import com.gmeci.conversions.Conversions;

import com.gmeci.atskservice.resolvers.AZProviderClient;
import com.gmeci.core.SurveyPoint;
import java.util.Map.Entry;
import com.gmeci.core.ATSKConstants;
import com.gmeci.core.SurveyData;
import android.hardware.GeomagneticField;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import java.util.TimeZone;
import java.util.Locale;

import android.view.Window;

import com.gmeci.atskservice.resolvers.ObstructionProviderClient;
import com.gmeci.core.PointObstruction;

import com.gmeci.helpers.ObstructionHelper;
import android.database.Cursor;
import java.text.SimpleDateFormat;

import com.gmeci.atskservice.form.formobjects.ScreenHint;
import com.gmeci.atskservice.form.formobjects.GroupedField;
import com.gmeci.atskservice.form.formobjects.FormObject;
import com.gmeci.atskservice.form.formobjects.Label;
import com.gmeci.atskservice.form.formobjects.KeyLabel;
import com.gmeci.atskservice.form.formobjects.Form;
import com.gmeci.atskservice.form.formobjects.RadioField;
import com.gmeci.atskservice.form.formobjects.CheckField;
import com.gmeci.atskservice.form.formobjects.DependsBlock;

import android.widget.LinearLayout;

import android.widget.Button;

import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

import android.view.View.OnClickListener;

import java.util.*;
import android.util.Log;

import android.app.NotificationManager;
import android.support.v4.app.NotificationCompat;

abstract public class AbstractFormActivity extends Activity implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    protected static final int NOTIFICATION_ID = 10102;

    protected static final String TAG = "AbstractFormActivity";
    protected Context c;
    protected SharedPreferences prefs;
    protected Form form;

    protected AZProviderClient azpc;

    protected String currentSurveyUID;
    protected SurveyData survey;
    protected ObstructionProviderClient opc;

    public static final char DEGREE = '\u00B0';

    protected static final double METERS_2_FEET = 3.28084f;
    protected static final double METERS_2_YARDS = METERS_2_FEET / 3;

    // when parsing a __variable__, we will substitute in a registered FormObject
    // once with the key of variable.  
    private HashMap<String, FormObject> handlers = new HashMap<String, FormObject>();

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences prefs,
            final String key) {

        try {
            final String val = prefs.getString(key, null);

            if (survey == null)
                return;

            if (val == null) {
                survey.data.remove(key);

            } else {
                survey.data.put(key, val);
            }
        } catch (Exception e) {
            Log.d(TAG, "error occurred with shared preference key=" + key, e);
        }

    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // remove the title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.general_form);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        c = this;

        azpc = new AZProviderClient(this);
        azpc.Start();

        opc = new ObstructionProviderClient(this);
        opc.Start();

        currentSurveyUID = azpc.getSetting(ATSKConstants.CURRENT_SURVEY, TAG);
        survey = azpc.getAZ(currentSurveyUID, true);

        // clear the current preferences so no cross contamination occurs
        prefs.edit().clear().commit();
        fillPreferencesFromCurrentSurveyPrefMap();

        prefs.registerOnSharedPreferenceChangeListener(this);

        LinearLayout ll = (LinearLayout) findViewById(R.id.list);
        form = createForm();
        ll.addView(form.getView());

        Button b = (Button) findViewById(R.id.pdf);
        b.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                fullCommit();
                checkAndWarn(c);
            }
        });

    }

    protected void checkAndWarn(final Context context) {
        final SharedPreferences _prefs = PreferenceManager
                .getDefaultSharedPreferences(context);
        boolean displayHint = _prefs
                .getString("atak.hint.missingadobe", "true").equals("true");

        if (!isInstalled(context, "com.adobe.reader") && displayHint) {

            View v = LayoutInflater.from(context)
                    .inflate(R.layout.hint_screen, null);
            TextView tv = (TextView) v.findViewById(R.id.message);
            tv.setText("It is recommended that you use the official Acrobat Reader.\nViewing the documents in other Android PDF applications may not work properly.");
            final CheckBox cb = (CheckBox) v.findViewById(R.id.showAgain);

            new AlertDialog.Builder(context)
                    .setTitle("Acrobat Reader Missing")
                    .setView(v)
                    .setCancelable(false)
                    .setPositiveButton("OK",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                        int id) {
                                    _prefs.edit()
                                            .putString(
                                                    "atak.hint.missingadobe",
                                                    "" + !cb.isChecked())
                                            .commit();
                                    GeneratePDF();
                                }
                            }).create().show();
        } else {
            GeneratePDF();
        }
    }

    private boolean isInstalled(Context context, String pkg) {
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(pkg, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            Log.d(TAG, "Package not installed: " + pkg, e);
        }
        return false;
    }

    public void GeneratePDF() {
        try {

            NotificationManager mNotifyManager;

            mNotifyManager = (NotificationManager) this
                    .getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
                    this);
            mBuilder.setContentTitle("ATSK Building IMAGE PDF")
                    .setContentText(
                            String.format("%s PDF In Progress",
                                    survey.getSurveyName()))
                    .setSmallIcon(R.drawable.navigation_export);
            mBuilder.setProgress(100, 5, false);
            mNotifyManager.notify(NOTIFICATION_ID,
                    mBuilder.build());

            mNotifyManager.cancel(NOTIFICATION_ID);
            // legacy call for now, replace correctly
            AbstractPDFFiller.BuildTypeOfPDF(this, survey);

        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }

    /**
     * Handlers are a one shot deal, so when they are registered, and used they are 
     * no longer able to be used.   In order to display in two different locations you 
     * must add, use and the add again.   Likely this is in error as this would allow 
     * the user to independently modify the same preference value in two different 
     * locations.   Handlers are consumed by the line parser.
     */
    final public void addHandler(final FormObject formObject) {
        String key = formObject.getKey();
        if (handlers.containsKey(key)) {
            Log.d(TAG, "overriding existing key, probably an error"
                    + formObject.getKey());
        }
        handlers.put(key, formObject);
    }

    /**
     * for use only by the line parser.
     */
    final private FormObject useHandler(final String key) {
        return handlers.remove(key);
    }

    final protected void record(final String key, final String value) {
        prefs.edit().putString(key, value).commit();
    }

    /******
     * Methods Lifted from ATSKService.java
     */

    static public String BuildObstructionRAB(SurveyData surveyData,
            PointObstruction obstruction) {
        double RAB[] = Conversions.CalculateRangeAngle(
                surveyData.center.lat, surveyData.center.lon,
                obstruction.lat, obstruction.lon);
        return String.format("%s @%.0f ft ",
                Conversions.GetCardinalDirection(RAB[1]), RAB[0]
                        * Conversions.M2F);
    }

    static public String BuildObstructionDescription(
            PointObstruction obstruction) {
        String description = obstruction.type;
        if (obstruction.remark != null
                && obstruction.remark.length() > 0) {
            description = obstruction.type + " "
                    + obstruction.remark;
        }
        String HeightString = String.format(" %.0f '",
                obstruction.height * Conversions.M2F);
        return description + HeightString;
    }

    // returns a list of all hazards INSIDE the AZ as a FE compliant string
    public String[] getInsideHazards() {
        Cursor filteredCursor = ObstructionHelper
                .GetAZFilteredPointCursor(survey, opc, true);
        if (filteredCursor != null) {

            List<String> values = ObstructionHelper
                    .GetAZFilteredObstructionStrings(opc,
                            filteredCursor, survey, true);
            filteredCursor.close();
            if (values == null) {
                values = new ArrayList<String>();
            }

            List<String> obstructions = ObstructionHelper
                    .GetAZLineObstructionStrings(opc, survey, true);

            if (obstructions != null)
                values.addAll(obstructions);

            String[] retval = new String[values.size()];
            values.toArray(retval);
            return retval;
        } else {
            return new String[0];
        }

    }

    // returns a list of all hazards OUTSIDE the AZ as a FE compliant string
    // outside defined within 1km (OBSTRUCTION_LIMIT_m)
    public String[] getOutsideHazards() {
        Cursor filteredCursor = ObstructionHelper
                .GetAZFilteredPointCursor(survey, opc, false);
        if (filteredCursor != null) {
            List<String> values = ObstructionHelper
                    .GetAZFilteredObstructionStrings(opc,
                            filteredCursor, survey, false);
            filteredCursor.close();

            if (values == null) {
                values = new ArrayList<String>();
            }
            List<String> obstructions = ObstructionHelper
                    .GetAZLineObstructionStrings(opc, survey, false);

            if (obstructions != null)
                values.addAll(obstructions);

            String[] retval = new String[values.size()];
            values.toArray(retval);
            return retval;
        } else {
            return new String[0];
        }
    }

    public String[] getObservedHazards() {
        Cursor distressCursor = ObstructionHelper
                .GetDistressFilteredPointCursor(opc, survey);
        if (distressCursor != null) {
            List<String> values = ObstructionHelper
                    .GetDistressStrings(survey, distressCursor);
            distressCursor.close();
            if (values == null) {
                values = new ArrayList<String>();
            }

            String[] retval = new String[values.size()];
            values.toArray(retval);
            return retval;
        } else {
            return new String[0];
        }
    }

    public String getTodaysDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return (sdf.format(new Date()).toUpperCase(Locale.US));
    }

    final public String getMagString() {
        float alt = (float) survey.center.getHAE();
        if (alt == SurveyPoint.Altitude.INVALID)
            alt = 0f;

        GeomagneticField geoField = new GeomagneticField(
                (float) survey.center.lat,
                (float) survey.center.lon,
                alt,
                System.currentTimeMillis());
        String val = "E";
        if (geoField.getDeclination() < 0)
            val = "W";

        String MagVarString = String.format("%.1f",
                Math.abs(geoField.getDeclination())) + DEGREE + val;
        return MagVarString;
    }

    /**
     * Returns the magnetic change over a period of 1 year, starting with the current time.
     * @return the value of magnetic change in minutes.
     */
    final public String getMagChange() {
        float alt = (float) survey.center.getHAE();
        if (alt == SurveyPoint.Altitude.INVALID)
            alt = 0f;

        GeomagneticField CurrentgeoField = new GeomagneticField(
                (float) survey.center.lat,
                (float) survey.center.lon,
                alt,
                System.currentTimeMillis());

        GeomagneticField FuturegeoField = new GeomagneticField(
                (float) survey.center.lat,
                (float) survey.center.lon,
                alt,
                System.currentTimeMillis() +
                        (1000 * 60 * 60 * 24));

        double Difference = FuturegeoField.getDeclination()
                - CurrentgeoField.getDeclination();

        Difference = Difference * 365;

        String pos = "E";
        if (Difference < 0)
            pos = "W";

        return String.format("%.4f",
                Math.abs(Difference * 60)) + "' " + pos;
    }

    final protected String getYdsMetersString(final double distance) {
        return String.format(" %s yds / %s m",
                toRoundString(distance * METERS_2_YARDS),
                toRoundString(distance));

    }

    final protected String getAngleString(final double angle) {
        return String.format(" %.1f\u00b0", Conversions.deg360(angle));
    }

    final protected String getAngleAndInverseString(final double angle) {
        return String.format(" %.1f\u00b0 / %.1f\u00b0",
                Conversions.deg360(angle), Conversions.deg360(angle + 180));

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

    final String toRoundString(final double f) {
        return String.valueOf((int) Math.ceil(f - Conversions.THRESH));
    }

    /*
     * This is called second, the values of preferences of the activity are overwritten
     * with the values stored in currentSurveyPrefMap
     */
    private void fillPreferencesFromCurrentSurveyPrefMap() {

        Editor prefEditor = prefs.edit();
        Iterator<java.util.Map.Entry<String, Object>> it =
                survey.data.entrySet().iterator();

        while (it.hasNext()) {
            Entry<String, Object> next = it.next();
            String currentKey = next.getKey();
            Object currentVal = next.getValue();
            // forms are the only user of the preferences, so make sure to 
            // only save off string values
            if (currentVal instanceof String) {
                String val = (String) currentVal;
                if (val != null) {
                    prefEditor.putString(currentKey, val);
                }
            }
        }
        prefEditor.remove("AZ_NAME");
        prefEditor.commit();
    }

    @Override
    final public void onPause() {
        fullCommit();
        super.onPause();
    }

    @Override
    final public void onDestroy() {
        fullCommit();
        azpc.Stop();
        opc.Stop();
        super.onDestroy();
    }

    private void fullCommit() {

        // commit the values of the form, then generate the blocks
        form.commit();

        generateBlocks();

        if (survey != null) {
            Log.d(TAG, "committing the values to the survey");
            azpc.UpdateAZ(survey, AZURIConstants.AZ_REMARKS_UPDATED, true);
        }
    }

    final public String lus(int resource) {
        return c.getString(resource);
    }

    final public String[] lusa(final int resource) {
        return c.getResources().getStringArray(resource);
    }

    final public FormObject createHazardBlock(final FormObject fo,
            final String key,
            final String[] values) {

        final CheckField cf = new CheckField(c, key, 10, values, "Hazard");
        DependsBlock.Decider d = new DependsBlock.Decider() {
            public boolean include() {
                return !cf.getText().startsWith("NONE");
            }
        };
        DependsBlock form = new DependsBlock(c, "_none_",
                new FormObject[] {
                        fo, cf
                }, d);
        return form;

    }

    final public String[] concat(final String[] a1, final String[] a2) {
        String[] ret = new String[a1.length + a2.length];
        for (int i = 0; i < a1.length; ++i)
            ret[i] = a1[i];
        for (int i = a1.length; i < ret.length; ++i)
            ret[i] = a2[i - a1.length];
        return ret;
    }

    final public void initMag() {
        addHandler(new RadioField(c, "dzElevDerivedFrom", 10,
                lusa(R.array.gpsource), "Elevation Derived"));

        addHandler(new RadioField(c, "dzMarginOfError", 10, lusa(R.array.he),
                "MarginOfError"));

        record("dzMagneticVar", getMagString());
        addHandler(new KeyLabel(c, 4, "dzMagneticVar"));

        record("dzMagChange", getMagChange());
        addHandler(new KeyLabel(c, 5, "dzMagChange"));

        record("dzMagDerivedDate", getTodaysDate());
        addHandler(new KeyLabel(c, 8, "dzMagDerivedDate"));
    }

    /**
     * Needs to construct both the main form and set the rb block and penetrations
     * block if applicable.   For FARP, there are additional blocks.
     */
    abstract public Form createForm();

    abstract public void generateBlocks();

    final public FormObject parseLine(int resource) {
        final String s = lus(resource);
        return parseLine(s);
    }

    final public FormObject parseLine(final String s) {

        if (s.contains("__")) {
            String[] vals = s.split("__");
            List<FormObject> foList = new ArrayList<FormObject>();

            for (int i = 0; i < vals.length; ++i) {
                if (vals[i].equalsIgnoreCase("$vnl")) {
                    foList.add(new ScreenHint.NewLine(c));
                } else {
                    FormObject fo = useHandler(vals[i]);
                    if (fo == null) {
                        if (prefs.contains(vals[i])) {
                            foList.add(new KeyLabel(c, -1, vals[i]));
                        } else {
                            foList.add(new Label(c, -1, vals[i]));
                        }
                    } else {
                        foList.add(fo);
                    }
                }
            }
            FormObject[] foArray = new FormObject[foList.size()];
            foList.toArray(foArray);
            return new GroupedField(c, foArray, true);

        } else
            return new Label(c, -1, s);

    }

    /**
     * Provided a text block, return two text blocks broke on a provided line count.   If the line 
     * count provided is too short, then the array will contain return[1] == null.
     */
    final protected String[] split(String text, int count) {
        String[] lines = text.split("\n");

        String[] ret = new String[2];

        String s = "";
        for (int i = 0; i < lines.length; ++i) {
            if (i != 0)
                s += "\n";
            s += lines[i];

            if (i < count) {
                ret[0] = s;
            } else if (i == count) {
                ret[0] = s;
                s = "";
            } else {
                ret[1] = s;
            }
        }
        return ret;
    }
}
