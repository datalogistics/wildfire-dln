
package com.gmeci.atsk.gradient.graphs;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.os.AsyncTask;
import com.atakmap.coremap.log.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.atakmap.android.atsk.plugin.R;
import com.gmeci.atsk.resources.ATSKApplication;
import com.gmeci.core.ATSKConstants;
import com.gmeci.core.SurveyPoint;
import com.gmeci.helpers.GradientAnalysisOPCHelper;
import com.gmeci.helpers.GradientAnalysisOPCHelper.TransverseCalcHelper;
import com.gmeci.conversions.Conversions;

import org.achartengine.ChartFactory;
import org.achartengine.chart.CombinedXYChart.XYCombinedChartDef;
import org.achartengine.chart.LineChart;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.widget.LinearLayout;

public class TransverseGradientGraphActivity extends GradientGraphActivityBase {

    private static final String TAG = "TransverseGradientGraphActivity";
    LinearLayout gradPrevNextLL;
    Button NextButton;
    Button PreviousButton;
    Button ShowHideButton;
    int CurrentTransverseDisplayed = 0;
    int MaxTransverse = 0;
    double ShoulderDistance = 0;
    double GradedDistance = 0;
    TransverseDisplayAsyncTask tdat;

    private XYSeries actualDataSeries;
    private XYSeries boundariesSeries;
    private XYSeries boundariesSeries2;
    private XYSeries boundariesSeries3;

    private XYSeriesRenderer mActualRenderer;
    private XYSeriesRenderer boundariesRenderer;
    private XYSeriesRenderer boundariesRenderer2;

    private XYSeries maxMinusAllowedDataSeries, minMinusAllowedDataSeries,
            maxPlusAllowedDataSeries, minPlusAllowedDataSeries;
    private XYSeriesRenderer mMaxMinusRenderer, mMinMinusRenderer,
            mMaxPlusRenderer, mMinPlusRenderer;
    private float size18;

    public TransverseGradientGraphActivity(boolean legacy) {
        super(legacy);
    }

    public void show(final Context context, final String uid, final int index) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        size18 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 18,
                metrics);

        if (uid != null && uid.length() > 0 && uid.contains("_"))
            this.CurrentTransverseDisplayed = index;

        Context pluginContext = ATSKApplication
                .getInstance()
                .getPluginContext();

        LayoutInflater mInflater = LayoutInflater.from(pluginContext);
        view = mInflater.inflate(R.layout.gradient_transverse_main, null);
        init(context);
        DisplayGradient(context);

        gradPrevNextLL = (LinearLayout) view.findViewById(R.id.gradPrevNextLL);
        gradPrevNextLL.setVisibility(View.VISIBLE);

        NextButton = (Button) view.findViewById(R.id.next);
        NextButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                CurrentTransverseDisplayed++;
                CurrentTransverseDisplayed = CurrentTransverseDisplayed
                        % MaxTransverse;
                DisplayGradient(context);
            }
        });
        PreviousButton = (Button) view.findViewById(R.id.previous);
        PreviousButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                CurrentTransverseDisplayed--;
                if (CurrentTransverseDisplayed < 0) {
                    CurrentTransverseDisplayed = MaxTransverse - 1;
                }
                DisplayGradient(context);
            }
        });
        ShowHideButton = (Button) view.findViewById(R.id.show_hide);
        ShowHideButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Fix this!!");
            }
        });
        ShowHideButton.setVisibility(View.GONE);

        //how many gradients exist?
        final Cursor TransversesCursor = gpc.GetAnalyzedGradients(
                CurrentSurveyUID,
                false);
        if (TransversesCursor != null) {
            MaxTransverse = TransversesCursor.getCount();
            if (CurrentTransverseDisplayed > MaxTransverse
                    || CurrentTransverseDisplayed < 0) {
                CurrentTransverseDisplayed = 0;
            }
            if (MaxTransverse < 2) {
                NextButton.setEnabled(false);
                PreviousButton.setEnabled(false);
            } else {
                NextButton.setEnabled(true);
                PreviousButton.setEnabled(true);
            }
            TransversesCursor.close();
        }
        // lets get the points we care about???

        AlertDialog.Builder ad = new AlertDialog.Builder(context);
        ad.setView(view);
        ad.create().show();

    }

    private void AddTransverseMaxMinBoundariesSeries() {
        maxMinusAllowedDataSeries = new XYSeries("MAX-");
        mMaxMinusRenderer = new XYSeriesRenderer();
        mMaxMinusRenderer.setColor(Color.argb(150, 240, 40, 40));
        mMaxMinusRenderer.setLineWidth(MAX_MIN_LINE_WIDTH);
        mMaxMinusRenderer.setShowLegendItem(false);
        mDataset.addSeries(maxMinusAllowedDataSeries);
        mRenderer.addSeriesRenderer(mMaxMinusRenderer);
        ListofLinesDrawn
                .add(new XYCombinedChartDef(LineChart.TYPE, GraphsDrawn));
        GraphsDrawn++;

        minMinusAllowedDataSeries = new XYSeries("MIN-");
        mMinMinusRenderer = new XYSeriesRenderer();
        mMinMinusRenderer.setColor(Color.argb(150, 240, 40, 40));
        mMinMinusRenderer.setLineWidth(MAX_MIN_LINE_WIDTH);
        mMinMinusRenderer.setShowLegendItem(false);
        mDataset.addSeries(minMinusAllowedDataSeries);
        mRenderer.addSeriesRenderer(mMinMinusRenderer);
        ListofLinesDrawn
                .add(new XYCombinedChartDef(LineChart.TYPE, GraphsDrawn));
        GraphsDrawn++;

        maxPlusAllowedDataSeries = new XYSeries("MAX+");
        mMaxPlusRenderer = new XYSeriesRenderer();
        mMaxPlusRenderer.setColor(Color.argb(150, 240, 40, 40));
        mMaxPlusRenderer.setLineWidth(MAX_MIN_LINE_WIDTH);
        mMaxPlusRenderer.setShowLegendItem(false);
        mDataset.addSeries(maxPlusAllowedDataSeries);
        mRenderer.addSeriesRenderer(mMaxPlusRenderer);
        ListofLinesDrawn
                .add(new XYCombinedChartDef(LineChart.TYPE, GraphsDrawn));
        GraphsDrawn++;

        minPlusAllowedDataSeries = new XYSeries("MIN+");
        mMinPlusRenderer = new XYSeriesRenderer();
        mMinPlusRenderer.setColor(Color.argb(150, 240, 40, 40));
        mMinPlusRenderer.setLineWidth(MAX_MIN_LINE_WIDTH);
        mMinPlusRenderer.setShowLegendItem(false);
        mDataset.addSeries(minPlusAllowedDataSeries);
        mRenderer.addSeriesRenderer(mMinPlusRenderer);
        ListofLinesDrawn
                .add(new XYCombinedChartDef(LineChart.TYPE, GraphsDrawn));
        GraphsDrawn++;

    }

    private void AddActualSeries() {
        actualDataSeries = new XYSeries("Actual");
        mActualRenderer = new XYSeriesRenderer();
        mActualRenderer.setLineWidth(MAX_MIN_LINE_WIDTH * 1.5f);
        mActualRenderer.setChartValuesTextSize(size18);
        mActualRenderer.setColor(0xFFFFFFFF);
        mDataset.addSeries(actualDataSeries);
        mRenderer.addSeriesRenderer(mActualRenderer);
        ListofLinesDrawn
                .add(new XYCombinedChartDef(LineChart.TYPE, GraphsDrawn));
        GraphsDrawn++;

        int BoundaryColor = Color.argb(60, 100, 140, 140);
        int BoundaryColorG = Color.argb(50, 80, 120, 120);
        boundariesSeries = new XYSeries("LZ");
        boundariesRenderer = new XYSeriesRenderer();
        boundariesRenderer.setLineWidth(MAX_MIN_LINE_WIDTH * 1.5f);
        boundariesRenderer.setChartValuesTextSize(size18);
        boundariesRenderer.setFillBelowLine(true);
        boundariesRenderer.setFillBelowLineColor(BoundaryColor);
        boundariesRenderer.setColor(BoundaryColor);
        mDataset.addSeries(boundariesSeries);
        mRenderer.addSeriesRenderer(boundariesRenderer);
        mRenderer.setBarWidth(1);
        mRenderer.setLabelsTextSize(size18);
        ListofLinesDrawn
                .add(new XYCombinedChartDef(LineChart.TYPE, GraphsDrawn));
        GraphsDrawn++;

        boundariesSeries2 = new XYSeries("Graded");
        boundariesRenderer2 = new XYSeriesRenderer();
        boundariesRenderer2.setLineWidth(MAX_MIN_LINE_WIDTH * 1.5f);
        boundariesRenderer2.setChartValuesTextSize(size18);
        boundariesRenderer2.setFillBelowLine(true);
        boundariesRenderer2.setFillBelowLineColor(BoundaryColorG);
        boundariesRenderer2.setColor(BoundaryColor);
        mDataset.addSeries(boundariesSeries2);
        mRenderer.addSeriesRenderer(boundariesRenderer2);
        mRenderer.setBarWidth(1);
        ListofLinesDrawn
                .add(new XYCombinedChartDef(LineChart.TYPE, GraphsDrawn));
        GraphsDrawn++;

        boundariesSeries3 = new XYSeries("Graded");
        boundariesRenderer2 = new XYSeriesRenderer();
        boundariesRenderer2.setLineWidth(MAX_MIN_LINE_WIDTH * 1.5f);
        boundariesRenderer2.setChartValuesTextSize(size18);
        boundariesRenderer2.setFillBelowLine(true);
        mMaxMinusRenderer.setShowLegendItem(false);
        boundariesRenderer2.setFillBelowLineColor(BoundaryColorG);
        boundariesRenderer2.setColor(BoundaryColor);
        mDataset.addSeries(boundariesSeries3);
        mRenderer.addSeriesRenderer(boundariesRenderer2);
        mRenderer.setBarWidth(1);
        ListofLinesDrawn
                .add(new XYCombinedChartDef(LineChart.TYPE, GraphsDrawn));
        GraphsDrawn++;
    }

    private void DrawGradient(double AZWidth, double HeightAtMinRange_m) {
        TransverseCalcHelper[] maintained = gaopc.getMaintaineds();
        TransverseCalcHelper[] graded = gaopc.getGradeds();
        TransverseCalcHelper[] shoulder = gaopc.getShoulders();
        TransverseCalcHelper[] az = gaopc.getAZs();

        AddActualSeries();

        addGradientPoint(maintained, 0);
        addGradientPoint(graded, 0);
        addGradientPoint(shoulder, 0);
        addGradientPoint(az, 0);
        addGradientPoint(az, 1);
        addGradientPoint(shoulder, 1);
        addGradientPoint(graded, 1);
        addGradientPoint(maintained, 1);

        double Width_d2_ft = (float) (CurrentSurvey.width * Conversions.M2F) / 2;
        double boundAlt = Double.NaN;
        if (graded != null && graded[0] != null) {
            SurveyPoint gradedOut = graded[0].getOutsidePoint();
            if (gradedOut != null)
                boundAlt = 2 * gradedOut.getHAE() * Conversions.M2F;
        }

        boundariesSeries2.add((-1 * GradedDistance * Conversions.M2F) - .25, 0);
        if (!Double.isNaN(boundAlt)) {
            boundariesSeries2.add((-1 * GradedDistance * Conversions.M2F), 2
                    * boundAlt);
            boundariesSeries2.add((-1 * ShoulderDistance * Conversions.M2F), 2
                    * boundAlt);
        }
        boundariesSeries2.add((-1 * ShoulderDistance * Conversions.M2F) + .25,
                0);

        boundariesSeries.add((-1 * Width_d2_ft) - .25, 0);
        if (!Double.isNaN(boundAlt)) {
            boundariesSeries.add(-1 * Width_d2_ft, boundAlt);
            boundariesSeries.add(Width_d2_ft, boundAlt);
            boundariesSeries.add(Width_d2_ft + .25, 0);
        }
        boundariesSeries3.add((ShoulderDistance * Conversions.M2F) - .25, 0);
        if (!Double.isNaN(boundAlt)) {
            boundariesSeries3.add((ShoulderDistance * Conversions.M2F),
                    boundAlt);
            boundariesSeries3.add((GradedDistance * Conversions.M2F), boundAlt);
        }
        boundariesSeries3.add((GradedDistance * Conversions.M2F) + .25, 0);

    }

    private void addGradientPoint(TransverseCalcHelper[] calc, int index) {
        if (calc != null && calc[index] != null && calc[index].isValid()) {
            SurveyPoint inPoint = calc[index].getInsidePoint(), outPoint = calc[index]
                    .getOutsidePoint();
            double inRange = inPoint.speed * Conversions.M2F, inAlt = inPoint
                    .getHAE() * Conversions.M2F, outRange = outPoint.speed
                    * Conversions.M2F, outAlt = outPoint.getHAE()
                    * Conversions.M2F;
            if (index == 0) {
                actualDataSeries.add(outRange, outAlt);
                actualDataSeries.add(inRange, inAlt);
            } else {
                actualDataSeries.add(inRange, inAlt);
                actualDataSeries.add(outRange, outAlt);
            }
        }
    }

    private void DrawMaxTransverseLZAllowed(double AZWidth,
            double HeightAtMinRange_m) {
        // draw Max Allowed
        AddTransverseMaxMinBoundariesSeries();
        // LZ
        this.maxMinusAllowedDataSeries.add(0 * Conversions.M2F,
                HeightAtMinRange_m * Conversions.M2F);
        this.minMinusAllowedDataSeries.add(0 * Conversions.M2F,
                HeightAtMinRange_m * Conversions.M2F);
        this.maxPlusAllowedDataSeries.add(0 * Conversions.M2F,
                HeightAtMinRange_m * Conversions.M2F);
        this.minPlusAllowedDataSeries.add(0 * Conversions.M2F,
                HeightAtMinRange_m * Conversions.M2F);

        double HeightAtEdgeOfLZMaxMinus_m = HeightAtMinRange_m
                + ((AZWidth / 2)
                        * CurrentSurvey.edges.GradientThreshholdLZTransMaxMinus / 100.0f);
        double HeightAtEdgeOfLZMinMinus_m = HeightAtMinRange_m
                + ((AZWidth / 2)
                        * CurrentSurvey.edges.GradientThreshholdLZTransMinMinus / 100.0f);
        double HeightAtEdgeOfLZMaxPlus_m = HeightAtMinRange_m
                + ((AZWidth / 2)
                        * CurrentSurvey.edges.GradientThreshholdLZTransMaxPlus / 100.0f);
        double HeightAtEdgeOfLZMinPlus_m = HeightAtMinRange_m
                + ((AZWidth / 2)
                        * CurrentSurvey.edges.GradientThreshholdLZTransMinPlus / 100.0f);
        double EdgeOfLZX = AZWidth / 2;

        this.maxMinusAllowedDataSeries.add(EdgeOfLZX * Conversions.M2F,
                HeightAtEdgeOfLZMaxMinus_m * Conversions.M2F);
        this.maxMinusAllowedDataSeries.add(EdgeOfLZX * Conversions.M2F * -1,
                HeightAtEdgeOfLZMaxMinus_m * Conversions.M2F);

        this.minMinusAllowedDataSeries.add(EdgeOfLZX * Conversions.M2F,
                HeightAtEdgeOfLZMinMinus_m * Conversions.M2F);
        this.minMinusAllowedDataSeries.add(EdgeOfLZX * Conversions.M2F * -1,
                HeightAtEdgeOfLZMinMinus_m * Conversions.M2F);

        this.maxPlusAllowedDataSeries.add(EdgeOfLZX * Conversions.M2F,
                HeightAtEdgeOfLZMaxPlus_m * Conversions.M2F);
        this.maxPlusAllowedDataSeries.add(EdgeOfLZX * Conversions.M2F * -1,
                HeightAtEdgeOfLZMaxPlus_m * Conversions.M2F);

        this.minPlusAllowedDataSeries.add(EdgeOfLZX * Conversions.M2F,
                HeightAtEdgeOfLZMinPlus_m * Conversions.M2F);
        this.minPlusAllowedDataSeries.add(EdgeOfLZX * Conversions.M2F * -1,
                HeightAtEdgeOfLZMinPlus_m * Conversions.M2F);

        // SHOULDER
        double EdgeOfShoulder = EdgeOfLZX + CurrentSurvey.edges.ShoulderWidth_m;
        double HeightAtEdgeOfShoulderMaxMinus_m = HeightAtEdgeOfLZMaxMinus_m
                + (CurrentSurvey.edges.ShoulderWidth_m
                        * CurrentSurvey.edges.GradientThreshholdShoulderTransMaxMinus / 100f);
        double HeightAtEdgeOfShoulderMinMinus_m = HeightAtEdgeOfLZMinMinus_m
                + (CurrentSurvey.edges.ShoulderWidth_m
                        * CurrentSurvey.edges.GradientThreshholdShoulderTransMinMinus / 100f);
        double HeightAtEdgeOfShoulderMaxPlus_m = HeightAtEdgeOfLZMaxPlus_m
                + (CurrentSurvey.edges.ShoulderWidth_m
                        * CurrentSurvey.edges.GradientThreshholdShoulderTransMaxPlus / 100f);
        double HeightAtEdgeOfShoulderMinPlus_m = HeightAtEdgeOfLZMinPlus_m
                + (CurrentSurvey.edges.ShoulderWidth_m
                        * CurrentSurvey.edges.GradientThreshholdShoulderTransMinPlus / 100f);

        this.maxMinusAllowedDataSeries.add(EdgeOfShoulder * Conversions.M2F,
                HeightAtEdgeOfShoulderMaxMinus_m * Conversions.M2F);
        this.maxMinusAllowedDataSeries.add(EdgeOfShoulder * Conversions.M2F
                * -1, HeightAtEdgeOfShoulderMaxMinus_m * Conversions.M2F);

        this.minMinusAllowedDataSeries.add(EdgeOfShoulder * Conversions.M2F,
                HeightAtEdgeOfShoulderMinMinus_m * Conversions.M2F);
        this.minMinusAllowedDataSeries.add(EdgeOfShoulder * Conversions.M2F
                * -1, HeightAtEdgeOfShoulderMinMinus_m * Conversions.M2F);

        this.maxPlusAllowedDataSeries.add(EdgeOfShoulder * Conversions.M2F,
                HeightAtEdgeOfShoulderMaxPlus_m * Conversions.M2F);
        this.maxPlusAllowedDataSeries.add(
                EdgeOfShoulder * Conversions.M2F * -1,
                HeightAtEdgeOfShoulderMaxPlus_m * Conversions.M2F);

        this.minPlusAllowedDataSeries.add(EdgeOfShoulder * Conversions.M2F,
                HeightAtEdgeOfShoulderMinPlus_m * Conversions.M2F);
        this.minPlusAllowedDataSeries.add(
                EdgeOfShoulder * Conversions.M2F * -1,
                HeightAtEdgeOfShoulderMinPlus_m * Conversions.M2F);

        // GRADED
        double EdgeOfGraded = EdgeOfShoulder
                + CurrentSurvey.edges.GradedAreaWidth_m;
        double HeightAtEdgeOfGradedMaxMinus_m = HeightAtEdgeOfShoulderMaxMinus_m
                + (CurrentSurvey.edges.GradedAreaWidth_m
                        * CurrentSurvey.edges.GradientThreshholdGradedTransMaxMinus / 100f);
        double HeightAtEdgeOfGradedMinMinus_m = HeightAtEdgeOfShoulderMinMinus_m
                + (CurrentSurvey.edges.GradedAreaWidth_m
                        * CurrentSurvey.edges.GradientThreshholdGradedTransMinMinus / 100f);
        double HeightAtEdgeOfGradedMaxPlus_m = HeightAtEdgeOfShoulderMaxPlus_m
                + (CurrentSurvey.edges.GradedAreaWidth_m
                        * CurrentSurvey.edges.GradientThreshholdGradedTransMaxPlus / 100f);
        double HeightAtEdgeOfGradedMinPlus_m = HeightAtEdgeOfShoulderMinPlus_m
                + (CurrentSurvey.edges.GradedAreaWidth_m
                        * CurrentSurvey.edges.GradientThreshholdGradedTransMinPlus / 100f);

        this.maxMinusAllowedDataSeries.add(EdgeOfGraded * Conversions.M2F,
                HeightAtEdgeOfGradedMaxMinus_m * Conversions.M2F);
        this.maxMinusAllowedDataSeries.add(EdgeOfGraded * Conversions.M2F * -1,
                HeightAtEdgeOfGradedMaxMinus_m * Conversions.M2F);

        this.minMinusAllowedDataSeries.add(EdgeOfGraded * Conversions.M2F,
                HeightAtEdgeOfGradedMinMinus_m * Conversions.M2F);
        this.minMinusAllowedDataSeries.add(EdgeOfGraded * Conversions.M2F * -1,
                HeightAtEdgeOfGradedMinMinus_m * Conversions.M2F);

        this.maxPlusAllowedDataSeries.add(EdgeOfGraded * Conversions.M2F,
                HeightAtEdgeOfGradedMaxPlus_m * Conversions.M2F);
        this.maxPlusAllowedDataSeries.add(EdgeOfGraded * Conversions.M2F * -1,
                HeightAtEdgeOfGradedMaxPlus_m * Conversions.M2F);

        this.minPlusAllowedDataSeries.add(EdgeOfGraded * Conversions.M2F,
                HeightAtEdgeOfGradedMinPlus_m * Conversions.M2F);
        this.minPlusAllowedDataSeries.add(EdgeOfGraded * Conversions.M2F * -1,
                HeightAtEdgeOfGradedMinPlus_m * Conversions.M2F);

        // Maintained
        double EdgeOfMaintained = EdgeOfGraded
                + CurrentSurvey.edges.MaintainedAreaWidth_m;
        ShoulderDistance = EdgeOfShoulder;
        GradedDistance = EdgeOfGraded;
        double HeightAtEdgeOfMaintainedMaxMinus_m = HeightAtEdgeOfGradedMaxMinus_m
                + (CurrentSurvey.edges.MaintainedAreaWidth_m
                        * CurrentSurvey.edges.GradientThreshholdMaintainedTransMaxMinus / 100f);
        double HeightAtEdgeOfMaintainedMinMinus_m = HeightAtEdgeOfGradedMinMinus_m
                + (CurrentSurvey.edges.MaintainedAreaWidth_m
                        * CurrentSurvey.edges.GradientThreshholdMaintainedTransMinMinus / 100f);
        double HeightAtEdgeOfMaintainedMaxPlus_m = HeightAtEdgeOfGradedMaxPlus_m
                + (CurrentSurvey.edges.MaintainedAreaWidth_m
                        * CurrentSurvey.edges.GradientThreshholdMaintainedTransMaxPlus / 100f);
        double HeightAtEdgeOfMaintainedMinPlus_m = HeightAtEdgeOfGradedMinPlus_m
                + (CurrentSurvey.edges.MaintainedAreaWidth_m
                        * CurrentSurvey.edges.GradientThreshholdMaintainedTransMinPlus / 100f);

        Log.d(TAG, "Height Max " + HeightAtEdgeOfMaintainedMaxMinus_m
                + " moves from " + HeightAtEdgeOfGradedMaxMinus_m);
        Log.d(TAG, "Height Max " + HeightAtEdgeOfMaintainedMinMinus_m
                + " moves from " + HeightAtEdgeOfGradedMinMinus_m);

        this.maxMinusAllowedDataSeries.add(EdgeOfMaintained * Conversions.M2F,
                HeightAtEdgeOfMaintainedMaxMinus_m * Conversions.M2F);
        this.maxMinusAllowedDataSeries.add(EdgeOfMaintained * Conversions.M2F
                * -1, HeightAtEdgeOfMaintainedMaxMinus_m * Conversions.M2F);

        this.minMinusAllowedDataSeries.add(EdgeOfMaintained * Conversions.M2F,
                HeightAtEdgeOfMaintainedMinMinus_m * Conversions.M2F);
        this.minMinusAllowedDataSeries.add(EdgeOfMaintained * Conversions.M2F
                * -1, HeightAtEdgeOfMaintainedMinMinus_m * Conversions.M2F);

        this.maxPlusAllowedDataSeries.add(EdgeOfMaintained * Conversions.M2F,
                HeightAtEdgeOfMaintainedMaxPlus_m * Conversions.M2F);
        this.maxPlusAllowedDataSeries.add(EdgeOfMaintained * Conversions.M2F
                * -1, HeightAtEdgeOfMaintainedMaxPlus_m * Conversions.M2F);

        this.minPlusAllowedDataSeries.add(EdgeOfMaintained * Conversions.M2F,
                HeightAtEdgeOfMaintainedMinPlus_m * Conversions.M2F);
        this.minPlusAllowedDataSeries.add(EdgeOfMaintained * Conversions.M2F
                * -1, HeightAtEdgeOfMaintainedMinPlus_m * Conversions.M2F);

        mRenderer.setYAxisMax((HeightAtEdgeOfMaintainedMaxPlus_m + 5)
                * Conversions.M2F);
        mRenderer.setYAxisMin((HeightAtEdgeOfMaintainedMaxMinus_m - 5)
                * Conversions.M2F);
    }

    protected void DisplayGradient(Context context) {
        setLabel("T" + CurrentTransverseDisplayed);
        tdat = new TransverseDisplayAsyncTask(context, CurrentSurveyUID);
        tdat.execute(CurrentSurveyUID);
    }

    class TransverseDisplayAsyncTask extends AsyncTask<String, Float, Float> {
        ProgressDialog pd;
        String CurrentSurveyUID;
        final Context context;

        TransverseDisplayAsyncTask(Context context, String UID) {
            this.CurrentSurveyUID = UID;
            this.context = context;
        }

        @Override
        protected Float doInBackground(String... params) {
            if (CurrentSurvey == null) {
                CurrentSurveyUID = azpc.getSetting(
                        ATSKConstants.CURRENT_SURVEY, TAG);
                CurrentSurvey = azpc.getAZ(CurrentSurveyUID, false);
            }
            if (CurrentSurvey != null) {
                gaopc = new GradientAnalysisOPCHelper(
                        CurrentSurvey.getLength(false),
                        CurrentSurvey.width, gpc,
                        CurrentSurvey.angle, CurrentSurvey.center.lat,
                        CurrentSurvey.center.lon);
                gaopc.AnalyzeTransverseGradient(GradientAnalysisOPCHelper
                        .GetTransverseGradientUID(CurrentSurveyUID,
                                CurrentTransverseDisplayed),
                        CurrentSurvey);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Float result) {
            String TypeString = "";

            String CurrentUID = GradientAnalysisOPCHelper
                    .GetTransverseGradientUID(CurrentSurveyUID,
                            CurrentTransverseDisplayed);

            String Type = gpc.GetGradientType(CurrentUID);
            if (Type.contains(ATSKConstants.GRADIENT_HIDDEN_MODIFIER)) {
                //                if(Type.contains(ATSKConstants.GRADIENT_GOOD_MODIFIER))
                //                    TypeString = "(good/hidden)";
                //                else
                //                    TypeString = "(bad/hidden)";
            } else {
                //                if(Type.contains(ATSKConstants.GRADIENT_GOOD_MODIFIER))
                //                    TypeString = "(good)";
                //                else
                //                    TypeString = "(bad)";
            }

            if (CurrentUID.contains("_")) {
                int i = CurrentUID.indexOf("_") + 1;

                CurrentUID = CurrentUID.substring(i);
            }
            String TitleString = String.format("%s %s", CurrentUID, TypeString);
            ChartTitleTV.setText(TitleString);
            DrawGradientGraph();
            if (pd != null) {
                pd.dismiss();
            }
            super.onPostExecute(result);
        }

        @Override
        protected void onPreExecute() {
            pd = new ProgressDialog(context);
            pd.setTitle("Transverse Gradient...");
            pd.setMessage("Please wait.");
            pd.setCancelable(false);
            pd.setIndeterminate(true);
            pd.show();
            super.onPreExecute();
        }

        @Override
        protected void onProgressUpdate(Float... values) {
            // 
            super.onProgressUpdate(values);
        }

        private void DrawGradientGraph() {
            GraphsDrawn = 0;

            mDataset = new XYMultipleSeriesDataset();
            mRenderer = new XYMultipleSeriesRenderer();
            ListofLinesDrawn.clear();
            DrawMaxTransverseLZAllowed(CurrentSurvey.width,
                    gaopc.GetCenterHeightHAE());
            DrawGradient(CurrentSurvey.width, gaopc.GetCenterHeightHAE());
            XYCombinedChartDef[] types = new XYCombinedChartDef[ListofLinesDrawn
                    .size()];
            types = ListofLinesDrawn.toArray(types);
            mChart = ChartFactory.getCombinedXYChartView(context, mDataset,
                    mRenderer, types);
            ChartSpot.removeAllViews();
            ChartSpot.addView(mChart);
        }

    }

}
