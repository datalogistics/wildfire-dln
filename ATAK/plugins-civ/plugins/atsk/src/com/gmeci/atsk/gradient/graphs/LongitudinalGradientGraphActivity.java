
package com.gmeci.atsk.gradient.graphs;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint.Align;
import android.os.AsyncTask;
import com.atakmap.coremap.log.Log;
import android.view.LayoutInflater;

import com.atakmap.android.atsk.plugin.R;
import com.gmeci.atsk.resources.ATSKApplication;
import com.gmeci.core.ATSKConstants;
import com.gmeci.core.SurveyPoint;
import com.gmeci.helpers.GradientAnalysisOPCHelper;
import com.gmeci.helpers.GradientAnalysisOPCHelper.IntervalGradientItem;
import com.gmeci.atskservice.resolvers.GradientDBItem;
import com.gmeci.constants.Constants;
import com.gmeci.helpers.AZHelper;
import com.gmeci.conversions.Conversions;

import org.achartengine.ChartFactory;
import org.achartengine.chart.BarChart;
import org.achartengine.chart.CombinedXYChart.XYCombinedChartDef;
import org.achartengine.chart.LineChart;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

public class LongitudinalGradientGraphActivity extends
        GradientGraphActivityBase {

    public static final String TAG = "LongitudinalGradientGraphActivity";
    private static final double INTERVAL_OFFSET = ATSKConstants.
            GRADIENT_SPACING_LONGITUDINAL_FT / 2;
    LongitudinalDisplayAsyncTask ldat;
    HashMap<Double, String> Problems;
    HashMap<Integer, IntervalGradientItem> Intervals;
    private XYSeries actualDataSeries, missingDataSeries,
            intervalGradientDataSeries;
    private XYSeriesRenderer mActualRenderer, missingDataRenderer,
            intervalGradientRenderer;
    private Context _context;
    private double _dp = 1.0d;
    float size18;

    private ArrayList<GradientDataPoint> longDataSeries;

    LinearLayout gradVisLL;
    Button prevGradB;
    Button nextGradB;
    CheckBox gradCB;

    LinearLayout gradYMultiLL;
    Button gradYMultiNegB;
    Button gradYMultiPosB;
    TextView gradYMultiTV;

    int selGradIndex = 0;

    public LongitudinalGradientGraphActivity(boolean legacy) {
        super(legacy);
    }

    public void show(final Context context) {

        _context = context;

        Context pluginContext = ATSKApplication
                .getInstance().getPluginContext();

        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        size18 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 18,
                metrics);
        _dp = metrics.density;

        LayoutInflater mInflater = LayoutInflater.from(pluginContext);
        view = mInflater.inflate(R.layout.gradient_longitudinal_main, null);
        init(_context);
        setLabel("L");
        DisplayGradient(_context);
        AlertDialog.Builder ad = new AlertDialog.Builder(_context);
        int width = metrics.widthPixels;
        int height = metrics.heightPixels;
        view.setMinimumWidth((int) (width * 0.90f));
        view.setMinimumHeight((int) (height * 0.80f));
        ad.setView(view);
        ad.create().show();

    }

    private void incSelGradIndex() {
        ++selGradIndex;
        if (selGradIndex >= actives.length) {
            selGradIndex = 0;//first index
        }
        gradCB.setText("" + selGradIndex);
        gradCB.setChecked(actives[selGradIndex]);
    }

    private void decSelGradIndex() {
        --selGradIndex;
        if (selGradIndex < 0) {
            selGradIndex = actives.length - 1;//last index
        }
        gradCB.setText("" + selGradIndex);
        gradCB.setChecked(actives[selGradIndex]);
    }

    private void AddLongitudinalBoundariesSeries() {

        intervalGradientDataSeries = new XYSeries("Interval", 1);
        intervalGradientRenderer = new XYSeriesRenderer();
        intervalGradientRenderer.setColor(Color.argb(200, 100, 140, 140));
        intervalGradientRenderer.setLineWidth(MAX_MIN_LINE_WIDTH);
        intervalGradientRenderer.setShowLegendItem(true);
        intervalGradientRenderer.setDisplayChartValues(false);
        NumberFormat numberFormat = new DecimalFormat("0.#");
        intervalGradientRenderer.setChartValuesTextSize(size18);

        intervalGradientRenderer.setChartValuesFormat(numberFormat);
        mDataset.addSeries(0, intervalGradientDataSeries);
        mRenderer.addSeriesRenderer(0, intervalGradientRenderer);
        mRenderer.setBarWidth(1);
        ListofLinesDrawn
                .add(new XYCombinedChartDef(BarChart.TYPE, GraphsDrawn));
        GraphsDrawn++;

        mRenderer.setYAxisMin(2
                * CurrentSurvey.edges.GradientThreshholdLZLonIntervalMax * -1,
                1);
        mRenderer.setYAxisMax(
                2 * CurrentSurvey.edges.GradientThreshholdLZLonIntervalMax, 1);

        missingDataSeries = new XYSeries("MISSING", 0);
        missingDataRenderer = new XYSeriesRenderer();
        missingDataRenderer.setColor(Color.argb(150, 200, 140, 40));
        missingDataRenderer.setLineWidth(MAX_MIN_LINE_WIDTH);
        missingDataRenderer.setShowLegendItem(true);
        mDataset.addSeries(1, missingDataSeries);//index?
        mRenderer.addSeriesRenderer(1, missingDataRenderer);//index??
        mRenderer.setBarWidth(1);
        ListofLinesDrawn
                .add(new XYCombinedChartDef(BarChart.TYPE, GraphsDrawn));
        GraphsDrawn++;

    }

    private void AddActualSeries() {
        actualDataSeries = new XYSeries("Actual", 0);
        mActualRenderer = new XYSeriesRenderer();
        mActualRenderer.setLineWidth(MAX_MIN_LINE_WIDTH * 1.5f);
        mActualRenderer.setChartValuesTextSize(size18);
        mActualRenderer.setFillBelowLine(false);
        mActualRenderer.setColor(GetGoodColor());
        mDataset.addSeries(2, actualDataSeries);
        mRenderer.addSeriesRenderer(2, mActualRenderer);

        ListofLinesDrawn
                .add(new XYCombinedChartDef(LineChart.TYPE, GraphsDrawn));
        GraphsDrawn++;

        longDataSeries = new ArrayList<GradientDataPoint>();

    }

    private void DrawGradient(double AZWidth, double HeightAtMinRange_m,
            String CurrentSurveyUID) {

        AddActualSeries();
        //get the line for the gradient...
        GradientDBItem gdbi = gpc.GetGradient(ATSKConstants.DEFAULT_GROUP,
                GradientAnalysisOPCHelper
                        .GetLongidudinalGradientUID(CurrentSurveyUID), true);
        if (gdbi == null) {
            // we should rerun the analysis... and try to get gradient again
            return;
        }
        List<SurveyPoint> points = gdbi.getPoints();
        //recalculate all ranges from approach center??
        SurveyPoint startPoint = Conversions.AROffset(CurrentSurvey.center,
                CurrentSurvey.angle + 180, (CurrentSurvey.getLength(false) / 2)
                        + CurrentSurvey.edges.ApproachOverrunLength_m);
        for (SurveyPoint nextPoint : points) {
            double Range_m = Conversions.CalculateRangem(startPoint.lat,
                    startPoint.lon, nextPoint.lat, nextPoint.lon);
            double rangeFt = Range_m * Conversions.M2F;
            double altFt = Conversions.ConvertHAEtoMSL(nextPoint.lat,
                    nextPoint.lon, nextPoint.getHAE()) * Conversions.M2F;
            actualDataSeries.add(rangeFt, altFt);
            longDataSeries.add(new GradientDataPoint(rangeFt, altFt));
        }

        // Sort by range from start of overrun
        Collections.sort(longDataSeries, LONGITUDINAL_SORT);

        actives = new boolean[longDataSeries.size()];
        for (int i = 0; i < actives.length; i++)
            actives[i] = true;
    }

    private void DrawMaxLongitudinalLZAllowed(double HeightAtMinRange_m) {
        // draw Max Allowed
        double maxSlope = CurrentSurvey.edges
                .GradientThreshholdLZLonOverallMax;
        double azLen = CurrentSurvey.getLength(false);
        AddLongitudinalBoundariesSeries();
        SurveyPoint appPoint = AZHelper.CalculateAnchorFromAZCenter(
                CurrentSurvey, CurrentSurvey.center,
                CurrentSurvey.getApproachAnchor());
        SurveyPoint depPoint = AZHelper.CalculateAnchorFromAZCenter(
                CurrentSurvey, CurrentSurvey.center,
                CurrentSurvey.getDepartureAnchor());
        double HeightAtEndMaxMinus_m = Conversions.ConvertHAEtoMSL(
                appPoint.lat, appPoint.lon, HeightAtMinRange_m
                        - ((azLen / 2) * maxSlope / 100.0f));
        double HeightAtEndMaxPlus_m = Conversions.ConvertHAEtoMSL(
                depPoint.lat, depPoint.lon, HeightAtMinRange_m
                        + ((azLen / 2) * maxSlope / 100.0f));

        Iterator<Entry<Integer, IntervalGradientItem>> intervalIT = Intervals
                .entrySet().iterator();
        while (intervalIT.hasNext()) {
            Map.Entry<Integer, IntervalGradientItem> pairs = intervalIT
                    .next();
            IntervalGradientItem igi = pairs.getValue();
            if (igi.gradient_deg < 90)
                intervalGradientDataSeries
                        .add(INTERVAL_OFFSET + igi.IntervalIndex
                                * ATSKConstants
                                .GRADIENT_SPACING_LONGITUDINAL_FT,
                                igi.gradient_deg);
        }

        Iterator<Entry<Double, String>> it = Problems.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Double, String> pairs = it
                    .next();
            if (pairs.getValue().equals(
                    Constants.GRADIENT_MISSING_DATA)) {
                //draw a hole
                double DistanceIn_m = pairs.getKey();
                missingDataSeries.add(DistanceIn_m * Conversions.M2F,
                        (HeightAtEndMaxPlus_m + 5) * Conversions.M2F);
                missingDataSeries
                        .add(150 + (DistanceIn_m * Conversions.M2F), 0);
            }

            Log.d(TAG, pairs.getKey() + " = " + pairs.getValue());
            it.remove(); // avoids a ConcurrentModificationException
        }

        mRenderer.setYAxisMax((HeightAtEndMaxPlus_m + 5) * Conversions.M2F, 0);
        mRenderer.setYAxisMin((HeightAtEndMaxMinus_m - 5) * Conversions.M2F, 0);
        mRenderer.setYAxisAlign(Align.LEFT, 0);
        mRenderer.setYAxisAlign(Align.RIGHT, 1);
        mRenderer.setYAxisMax(2, 1);
        mRenderer.setYAxisMin(-2, 1);

    }

    protected void DisplayGradient(Context context) {
        ldat = new LongitudinalDisplayAsyncTask(context, CurrentSurveyUID);
        ldat.execute(CurrentSurveyUID);
    }

    class LongitudinalDisplayAsyncTask extends AsyncTask<String, Float, Float> {
        ProgressDialog pd;
        final String CurrentSurveyUID;
        final Context context;

        LongitudinalDisplayAsyncTask(Context context, String UID) {
            this.CurrentSurveyUID = UID;
            this.context = context;
        }

        @Override
        protected Float doInBackground(String... params) {
            if (CurrentSurvey == null)
                return -1f;
            gaopc = new GradientAnalysisOPCHelper(
                    CurrentSurvey.getLength(false), CurrentSurvey.width,
                    gpc, CurrentSurvey.angle,
                    CurrentSurvey.center.lat, CurrentSurvey.center.lon);
            Problems = gaopc.AnalyzeLongitudinalGradient(
                    GradientAnalysisOPCHelper
                            .GetLongidudinalGradientUID(CurrentSurvey.uid),
                    ATSKConstants.GRADIENT_SPACING_LONGITUDINAL_FT
                            / Conversions.M2F,
                    CurrentSurvey);
            Intervals = gaopc.getGradientIntervalMap();
            return null;
        }

        @Override
        protected void onPostExecute(Float result) {
            String s = GradientAnalysisOPCHelper
                    .GetLongidudinalGradientUID(CurrentSurveyUID);
            if (s.contains("_"))
                s = s.split("_")[1];

            ChartTitleTV.setText(s);
            DrawGradientGraph();
            if (pd != null) {
                pd.dismiss();
            }
            super.onPostExecute(result);
        }

        @Override
        protected void onPreExecute() {
            pd = new ProgressDialog(context);
            pd.setTitle("Longitudinal Gradient...");
            pd.setMessage("Please wait.");
            pd.setCancelable(false);
            pd.setIndeterminate(true);
            pd.show();
            super.onPreExecute();
        }

        @Override
        protected void onProgressUpdate(Float... values) {
            super.onProgressUpdate(values);
        }

        private void DrawGradientGraph() {
            GraphsDrawn = 0;

            mDataset = new XYMultipleSeriesDataset();
            mRenderer = new XYMultipleSeriesRenderer(2);
            int length = mRenderer.getSeriesRendererCount();
            for (int i = 0; i < length; i++) {
                ((XYSeriesRenderer) mRenderer.getSeriesRendererAt(i))
                        .setFillPoints(true);
            }

            mRenderer.setYLabelsColor(0, Color.WHITE);
            mRenderer.setYLabelsColor(1, Color.argb(255, 100, 140, 140));
            mRenderer.setAxisTitleTextSize(size18);
            mRenderer.setLegendTextSize(size18);
            mRenderer.setZoomButtonsVisible(true);
            mRenderer.setYTitle("Interval Gradient", 1);
            mRenderer.setYTitle("Elevation (ft MSL)", 0);
            mRenderer.setYAxisAlign(Align.RIGHT, 1);
            mRenderer.setYLabelsAlign(Align.RIGHT, 1);
            mRenderer.setYLabelsAlign(Align.LEFT, 0);
            int[] margins = mRenderer.getMargins();
            margins[1] += _dp * 12;
            margins[3] += _dp * 12;
            mRenderer.setMargins(margins);
            mRenderer.setLabelsTextSize(size18);

            ListofLinesDrawn.clear();
            DrawMaxLongitudinalLZAllowed(gaopc.GetCenterHeightHAE());
            DrawGradient(CurrentSurvey.width, gaopc.GetCenterHeightHAE(),
                    CurrentSurvey.uid);
            XYCombinedChartDef[] types = new XYCombinedChartDef[ListofLinesDrawn
                    .size()];
            types = ListofLinesDrawn.toArray(types);
            mChart = ChartFactory.getCombinedXYChartView(context, mDataset,
                    mRenderer, types);
            ChartSpot.removeAllViews();

            if (legacy) {
                ChartSpot.addView(mChart);
            } else {
                //                if(!legacy){
                gradVisLL = (LinearLayout) view.findViewById(R.id.gradVisLL);
                gradVisLL.setVisibility(View.VISIBLE);

                prevGradB = (Button) view.findViewById(R.id.prevGradB);
                prevGradB.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (actives != null) {
                            decSelGradIndex();
                        }
                    }
                });

                nextGradB = (Button) view.findViewById(R.id.nextGradB);
                nextGradB.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (actives != null) {
                            incSelGradIndex();
                        }
                    }
                });

                gradCB = (CheckBox) view.findViewById(R.id.gradCB);
                gradCB.setChecked(actives[selGradIndex]);
                gradCB.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView,
                            boolean isChecked) {
                        if (actives != null) {
                            actives[selGradIndex] = isChecked;
                        }
                        refreshGraph();
                    }
                });

                gradYMultiLL = (LinearLayout) view
                        .findViewById(R.id.gradYMultiLL);
                gradYMultiLL.setVisibility(View.VISIBLE);

                gradYMultiTV = (TextView) view.findViewById(R.id.gradYMultiTV);

                gradYMultiNegB = (Button) view
                        .findViewById(R.id.gradYMultiNegB);
                gradYMultiNegB.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        scaleY -= 0.5f;
                        if (scaleY < 0) {
                            scaleY = 0;
                        }
                        gradYMultiTV.setText("x" + df.format(scaleY));
                        refreshGraph();
                    }
                });

                gradYMultiPosB = (Button) view
                        .findViewById(R.id.gradYMultiPosB);
                gradYMultiPosB.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        scaleY += 0.5f;
                        if (scaleY > maxScaleY) {
                            scaleY = maxScaleY;
                        }
                        gradYMultiTV.setText("x" + df.format(scaleY));
                        refreshGraph();
                    }
                });
                //                }

                mSurfView = new LongitudinalDiagramSurfaceView(
                        context, CurrentSurvey, longDataSeries,
                        CurrentSurvey.edges);

                mSurfView.setActives(actives);

                ChartSpot
                        .addView(mSurfView);
            }

        }

    }

}
