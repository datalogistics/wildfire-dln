
package com.gmeci.atsk.gradient.graphs;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import com.atakmap.coremap.log.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.atakmap.android.atsk.plugin.R;
import com.gmeci.core.ATSKConstants;
import com.gmeci.core.LZEdges;
import com.gmeci.core.SurveyPoint;
import com.gmeci.helpers.AZHelper;
import com.gmeci.helpers.GradientAnalysisOPCHelper;
import com.gmeci.helpers.GradientAnalysisOPCHelper.TransverseCalcHelper;
import com.gmeci.conversions.Conversions;

//import org.achartengine.ChartFactory;
//import org.achartengine.chart.CombinedXYChart.XYCombinedChartDef;
//import org.achartengine.chart.LineChart;
//import org.achartengine.model.XYMultipleSeriesDataset;
//import org.achartengine.model.XYSeries;
//import org.achartengine.renderer.XYMultipleSeriesRenderer;
//import org.achartengine.renderer.XYSeriesRenderer;

import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class AllTransverseGradientsGraphActivity extends
        GradientGraphActivityBase {

    private static final String TAG = "AllTransverseGradientsGraphActivity";
    Button ShowHideButton;
    int CurrentTransverseDisplayed = 0;
    int MaxTransverse = 0;
    TransverseDisplayAsyncTask tdat;

    LinearLayout gradVisLL;
    Button prevGradB;
    Button nextGradB;
    CheckBox gradCB;

    LinearLayout gradYMultiLL;
    Button gradYMultiNegB;
    Button gradYMultiPosB;
    TextView gradYMultiTV;

    int selGradIndex = 0;

    //    private ArrayList<GradientDataPoint> actualDataSeries;
    private ArrayList<GradientData> allGradientData = new ArrayList<GradientData>();

    //    private XYSeries actualDataSeries;
    //    private XYSeries boundariesSeries;
    //    private XYSeries boundariesSeries2;
    //    private XYSeries boundariesSeries3;
    //
    //    private XYSeriesRenderer mActualRenderer;
    //    private XYSeriesRenderer boundariesRenderer;
    //    private XYSeriesRenderer boundariesRenderer2;
    //
    //    private XYSeries maxMinusAllowedDataSeries, minMinusAllowedDataSeries,
    //            maxPlusAllowedDataSeries, minPlusAllowedDataSeries;
    //    private XYSeriesRenderer mMaxMinusRenderer, mMinMinusRenderer,
    //            mMaxPlusRenderer, mMinPlusRenderer;

    public AllTransverseGradientsGraphActivity(boolean legacy) {
        super(legacy);
    }

    public void show(final Context context, final String uid, final int index) {
        if (uid != null && uid.length() > 0 && uid.contains("_"))
            this.CurrentTransverseDisplayed = index;

        Context pluginContext = com.gmeci.atsk.resources.ATSKApplication
                .getInstance()
                .getPluginContext();

        LayoutInflater mInflater = LayoutInflater.from(pluginContext);
        view = mInflater.inflate(R.layout.gradient_transverse_main, null);
        init(context);

        //how many gradients exist?
        final Cursor TransversesCursor = gpc.GetAnalyzedGradients(
                CurrentSurveyUID,
                false);
        if (TransversesCursor != null) {
            MaxTransverse = TransversesCursor.getCount();
            actives = new boolean[MaxTransverse];
            for (int i = 0; i < actives.length; i++) {
                actives[i] = true;
            }
            TransversesCursor.close();
        }

        DisplayGradient(context);

        ShowHideButton = (Button) view.findViewById(R.id.show_hide);
        ShowHideButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Fix this!!");
            }
        });
        ShowHideButton.setVisibility(View.GONE);

        //how many gradients exist?
        //        final Cursor TransversesCursor = gpc.GetAnalyzedGradients(
        //                CurrentSurveyUID,
        //                false);
        //        if(legacy){
        //            if (TransversesCursor != null) {
        //                gradPrevNextLL = (LinearLayout) view.findViewById(R.id.gradPrevNextLL);
        //                gradPrevNextLL.setVisibility(View.VISIBLE);
        //
        //                NextButton = (Button) view.findViewById(R.id.next);
        //                NextButton.setOnClickListener(new OnClickListener() {
        //                    @Override
        //                    public void onClick(View v) {
        //                        CurrentTransverseDisplayed++;
        //                        CurrentTransverseDisplayed = CurrentTransverseDisplayed
        //                                % MaxTransverse;
        //                        DisplayGradient(context);
        //                    }
        //                });
        //
        //                PreviousButton = (Button) view.findViewById(R.id.previous);
        //                PreviousButton.setOnClickListener(new OnClickListener() {
        //                    @Override
        //                    public void onClick(View v) {
        //                        CurrentTransverseDisplayed--;
        //                        if (CurrentTransverseDisplayed < 0) {
        //                            CurrentTransverseDisplayed = MaxTransverse - 1;
        //                        }
        //                        DisplayGradient(context);
        //                    }
        //                });
        //                //            MaxTransverse = TransversesCursor.getCount();
        //                if (CurrentTransverseDisplayed > MaxTransverse
        //                        || CurrentTransverseDisplayed < 0) {
        //                    CurrentTransverseDisplayed = 0;
        //                }
        //                if (MaxTransverse < 2) {
        //                    NextButton.setEnabled(false);
        //                    PreviousButton.setEnabled(false);
        //                } else {
        //                    NextButton.setEnabled(true);
        //                    PreviousButton.setEnabled(true);
        //                }
        //                TransversesCursor.close();
        //            }
        //        }else{

        gradVisLL = (LinearLayout) view.findViewById(R.id.gradVisLL);
        gradVisLL.setVisibility(View.VISIBLE);

        prevGradB = (Button) view.findViewById(R.id.prevGradB);
        prevGradB.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (actives != null) {
                    decSelGradIndex();
                }
            }
        });

        nextGradB = (Button) view.findViewById(R.id.nextGradB);
        nextGradB.setOnClickListener(new OnClickListener() {
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

        gradYMultiLL = (LinearLayout) view.findViewById(R.id.gradYMultiLL);
        gradYMultiLL.setVisibility(View.VISIBLE);

        gradYMultiTV = (TextView) view.findViewById(R.id.gradYMultiTV);

        gradYMultiNegB = (Button) view.findViewById(R.id.gradYMultiNegB);
        gradYMultiNegB.setOnClickListener(new OnClickListener() {
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

        gradYMultiPosB = (Button) view.findViewById(R.id.gradYMultiPosB);
        gradYMultiPosB.setOnClickListener(new OnClickListener() {
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

        //        }

        // lets get the points we care about???

        AlertDialog.Builder ad = new AlertDialog.Builder(context);
        ad.setView(view);

        ChartSpot.removeAllViews();
        //        ChartSpot.addView(mSurfView);

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

    //    private void updateScaleY(){
    //        if(mSurfView != null){
    //            mSurfView.setScaleY(scaleY);
    //        }
    //    }

    protected void DisplayGradient(Context context) {
        setLabel("T" + CurrentTransverseDisplayed);
        tdat = new TransverseDisplayAsyncTask(context, CurrentSurveyUID);
        tdat.execute(CurrentSurveyUID);
    }

    class TransverseDisplayAsyncTask extends AsyncTask<String, Float, Float> {
        ProgressDialog pd;
        String CurrentSurveyUID;
        Context context;

        TransverseDisplayAsyncTask(Context context, String UID) {
            this.CurrentSurveyUID = UID;
            this.context = context;

            if (CurrentSurvey == null) {
                CurrentSurveyUID = azpc.getSetting(
                        ATSKConstants.CURRENT_SURVEY, TAG);
                CurrentSurvey = azpc.getAZ(CurrentSurveyUID, false);
            }

            if (CurrentSurvey != null) {
                String crit = CurrentSurvey.aircraft;
                if (crit == null || crit.isEmpty() || crit.equals("NONE")) {
                    Toast.makeText(context, "Warning: No LZ criteria set",
                            Toast.LENGTH_LONG).show();
                }
            }

        }

        @Override
        protected Float doInBackground(String... params) {

            if (CurrentSurvey != null) {

                //                TODO Log.d("ATSK","SURVEY AIRCRAFT TYPE: "+CurrentSurvey.aircraft);

                gaopc = new GradientAnalysisOPCHelper(
                        CurrentSurvey.getLength(false),
                        CurrentSurvey.width, gpc,
                        CurrentSurvey.angle, CurrentSurvey.center.lat,
                        CurrentSurvey.center.lon);

                //                SurveyPoint anchor = AZHelper.CalculateAnchorFromAZCenter(CurrentSurvey,
                //                        CurrentSurvey.center, CurrentSurvey.getApproachAnchor());

                SurveyPoint CenterApproach = AZHelper.CalculateCenterOfEdge(
                        CurrentSurvey, true);

                //for each gradient
                for (int i = 0; i < MaxTransverse; i++) {
                    LZEdges edges = gaopc.AnalyzeTransverseGradient(
                            GradientAnalysisOPCHelper
                                    .GetTransverseGradientUID(CurrentSurveyUID,
                                            i), CurrentSurvey);

                    double distance = CenterApproach.distanceTo(gaopc
                            .getCenterPoint());

                    allGradientData.add(new GradientData(DrawGradient(),
                            distance, edges));
                }

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

            mSurfView = new TransverseDiagramSurfaceView(
                    context, allGradientData, CurrentSurvey);
            mSurfView.setActives(actives);
            ChartSpot.addView(mSurfView);

        }

        private ArrayList<GradientDataPoint> DrawGradient() {//double AZWidth, double HeightAtMinRange_m) {
            ArrayList<GradientDataPoint> actualDataSeries = new ArrayList<GradientDataPoint>();

            TransverseCalcHelper[] maintained = gaopc.getMaintaineds();
            TransverseCalcHelper[] graded = gaopc.getGradeds();
            TransverseCalcHelper[] shoulder = gaopc.getShoulders();
            TransverseCalcHelper[] az = gaopc.getAZs();

            // Graph each point from left to right across transverse
            if (maintained != null)
                addGradientPoint(maintained[0], actualDataSeries);
            if (graded != null)
                addGradientPoint(graded[0], actualDataSeries);
            if (shoulder != null)
                addGradientPoint(shoulder[0], actualDataSeries);
            if (az != null) {
                addGradientPoint(az[0], actualDataSeries);
                addGradientPoint(az[1], actualDataSeries);
            }
            if (shoulder != null)
                addGradientPoint(shoulder[1], actualDataSeries);
            if (graded != null)
                addGradientPoint(graded[1], actualDataSeries);
            if (maintained != null)
                addGradientPoint(maintained[1], actualDataSeries);

            //            double Width_d2_ft = (float) (CurrentSurvey.width * Conversions.M2F) / 2;
            //
            //            boundariesSeries2.add((-1 * GradedDistance * Conversions.M2F) - .25, 0);
            //            if (GradedPoints != null && GradedPoints[0] != null
            //                    && GradedPoints[0].OutsidePoint != null) {
            //                boundariesSeries2.add((-1 * GradedDistance * Conversions.M2F), 2
            //                        * GradedPoints[0].OutsidePoint.getHAE() * Conversions.M2F);
            //                boundariesSeries2.add((-1 * ShoulderDistance * Conversions.M2F), 2
            //                        * GradedPoints[0].OutsidePoint.getHAE() * Conversions.M2F);
            //            }
            //            boundariesSeries2.add((-1 * ShoulderDistance * Conversions.M2F) + .25,
            //                    0);
            //
            //            boundariesSeries.add((-1 * Width_d2_ft) - .25, 0);
            //            if (GradedPoints != null && GradedPoints[0] != null
            //                    && GradedPoints[0].OutsidePoint != null) {
            //                boundariesSeries.add(-1 * Width_d2_ft, 2
            //                        * GradedPoints[0].OutsidePoint.getHAE() * Conversions.M2F);
            //                boundariesSeries.add(Width_d2_ft, 2
            //                        * GradedPoints[0].OutsidePoint.getHAE() * Conversions.M2F);
            //                boundariesSeries.add(Width_d2_ft + .25, 0);
            //            }
            //            boundariesSeries3.add((ShoulderDistance * Conversions.M2F) - .25, 0);
            //            if (GradedPoints != null && GradedPoints[0] != null
            //                    && GradedPoints[0].OutsidePoint != null) {
            //                boundariesSeries3.add((ShoulderDistance * Conversions.M2F), 2
            //                        * GradedPoints[0].OutsidePoint.getHAE() * Conversions.M2F);
            //                boundariesSeries3.add((GradedDistance * Conversions.M2F), 2
            //                        * GradedPoints[0].OutsidePoint.getHAE() * Conversions.M2F);
            //            }
            //            boundariesSeries3.add((GradedDistance * Conversions.M2F) + .25, 0);

            return actualDataSeries;
        }

        private void addGradientPoint(TransverseCalcHelper calc,
                List<GradientDataPoint> data) {
            if (calc != null) {
                SurveyPoint inPoint = calc.getInsidePoint();
                if (inPoint != null) {
                    data.add(new GradientDataPoint(
                            inPoint.speed * Conversions.M2F,
                            inPoint.getHAE() * Conversions.M2F));
                }
                SurveyPoint outPoint = calc.getOutsidePoint();
                if (outPoint != null) {
                    data.add(new GradientDataPoint(
                            outPoint.speed * Conversions.M2F,
                            outPoint.getHAE() * Conversions.M2F));
                }
            }
        }
    }

    public static class GradientData {
        private ArrayList<GradientDataPoint> points;
        private double distFromAnchor;
        private LZEdges edges;
        private boolean enabled = true;

        public GradientData(ArrayList<GradientDataPoint> points,
                double distFromAnchor, LZEdges edges) {
            this.points = points;
            this.distFromAnchor = distFromAnchor;
            this.edges = edges;
        }

        public ArrayList<GradientDataPoint> getPoints() {
            return points;
        }

        public double getDistFromAnchor() {
            return distFromAnchor;
        }

        public LZEdges getEdges() {
            return edges;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

}
