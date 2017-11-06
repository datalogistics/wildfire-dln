
package com.gmeci.atsk.gradient.graphs;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;

import com.gmeci.atsk.az.lz.LZParser;
import com.gmeci.atsk.gradient.graphs.GradientGraphActivityBase.GradientDataPoint;
import com.gmeci.core.Criteria;
import com.gmeci.core.LZEdges;
import com.gmeci.core.SurveyData;
//import com.gmeci.core.SurveyPoint;

import java.text.DecimalFormat;
import java.util.ArrayList;

/**
 * Created by sommerj on 4/11/2016.
 */
public class TransverseDiagramSurfaceView extends DiagramSurfaceView {

    private ArrayList<AllTransverseGradientsGraphActivity.GradientData> actualDataSeries;
    private Criteria crit;

    //    private double surveyLength; //meters, no overruns

    //    public TransverseDiagramSurfaceView(Context context) {
    //        super(context);
    //    }
    public TransverseDiagramSurfaceView(
            Context context,
            ArrayList<AllTransverseGradientsGraphActivity.GradientData> actualDataSeries,
            SurveyData surveyData) {
        super(context, surveyData);
        this.actualDataSeries = actualDataSeries;
        //        this.surveyLength = surveyLength;

        crit = LZParser.getInstance().GetAircraftByName(surveyData.aircraft);

    }

    private void setActive(boolean[] actives) {

    }

    @Override
    public void show() {
        if (drawingThread != null && actualDataSeries != null
                && !actualDataSeries.isEmpty()) {

            drawingThread.handler.post(new Runnable() {

                @Override
                public void run() {
                    Canvas c = drawingThread.holder.lockCanvas();

                    int h = c.getHeight();
                    int w = c.getWidth();

                    b = Bitmap.createBitmap(w, h,
                            Bitmap.Config.ARGB_8888);

                    Canvas bitCan = new Canvas(b);

                    bitCan.drawColor(Color.WHITE);

                    float runLhalf = (float) (crit.RunwayLength_m / 2);
                    float runWhalf = (float) (crit.RunwayWidth_m / 2);
                    float runShoulderWhalf = (float) (runWhalf
                            + crit.RunwayShoulderWidth_m);//runWhalf + 100f;
                    float runGradeWhalf = (float) (runShoulderWhalf
                            + crit.GradedAreaWidth_m);
                    float runMaintainedWhalf = (float) (runGradeWhalf
                            + crit.MaintainedAreaWidth_m);

                    float buf = 80f;
                    float scaleXRun = (w - (buf * 2))
                            / (runMaintainedWhalf * 2);
                    float scaleYRun = (h - (buf * 2)) / (runLhalf * 2);

                    runLhalf *= scaleYRun;
                    runWhalf *= scaleXRun;
                    runShoulderWhalf *= scaleXRun;
                    runGradeWhalf *= scaleXRun;
                    runMaintainedWhalf *= scaleXRun;

                    //translate to center
                    bitCan.save();
                    bitCan.translate(w / 2, h / 2);

                    float bottomYtext = (h / 2) - okPaintText.getTextSize();

                    if (scaleY != 1) {
                        float topYtext = okPaintText.getTextSize() - (h / 2);
                        bitCan.drawText("Elevation Magnification " + scaleY,
                                0,
                                topYtext,
                                okPaintText);
                    }

                    float RRx = 0f;
                    float RSx = 0f;
                    float RGx = 0f;
                    float RMx = 0f;

                    if (crit.RunwayWidth_m > 0) {
                        RRx = (runWhalf) / 2;
                        //draw runway
                        bitCan.drawRect(-runWhalf, -runLhalf, runWhalf,
                                runLhalf,
                                backPaint);
                        bitCan.drawLine(0, (buf - runLhalf), 0,
                                (runLhalf - buf),
                                backPaint);
                        bitCan.drawText(getAngle() + "", 0, (buf - runLhalf),
                                backPaintText);
                        bitCan.drawText(getBackAngle() + "", 0,
                                ((runLhalf - buf))
                                        + backPaintText.getTextSize(),
                                backPaintText);

                        //                    float LRx = (-runWhalf) / 2;
                        bitCan.drawText("LR", -RRx, bottomYtext, okPaintText);
                        bitCan.drawText("RR", RRx, bottomYtext, okPaintText);
                    }

                    //draw shoulders
                    if (crit.RunwayShoulderWidth_m > 0) {
                        bitCan.drawLine(runShoulderWhalf, -runLhalf,
                                runShoulderWhalf, runLhalf, backPaint);
                        bitCan.drawLine(-runShoulderWhalf, -runLhalf,
                                -runShoulderWhalf, runLhalf, backPaint);

                        RSx = (runShoulderWhalf + runWhalf) / 2;
                        //                        float LSx = (-runShoulderWhalf - runWhalf) / 2;
                        bitCan.drawText("LS", -RSx, bottomYtext, okPaintText);
                        bitCan.drawText("RS", RSx, bottomYtext, okPaintText);
                    }
                    //draw grades
                    if (crit.GradedAreaWidth_m > 0) {
                        bitCan.drawLine(runGradeWhalf, -runLhalf,
                                runGradeWhalf,
                                runLhalf, backPaint);
                        bitCan.drawLine(-runGradeWhalf, -runLhalf,
                                -runGradeWhalf,
                                runLhalf, backPaint);

                        RGx = (runGradeWhalf + runShoulderWhalf) / 2;
                        //                        float LGx = (-runGradeWhalf - runShoulderWhalf) / 2;
                        bitCan.drawText("LG", -RGx, bottomYtext, okPaintText);
                        bitCan.drawText("RG", RGx, bottomYtext, okPaintText);
                    }
                    //draw maintaineds
                    if (crit.MaintainedAreaWidth_m > 0) {
                        bitCan.drawLine(runMaintainedWhalf, -runLhalf,
                                runMaintainedWhalf, runLhalf, backPaint);
                        bitCan.drawLine(-runMaintainedWhalf, -runLhalf,
                                -runMaintainedWhalf, runLhalf, backPaint);

                        RMx = (runMaintainedWhalf + runGradeWhalf) / 2;
                        //                        float LMx = (-runMaintainedWhalf - runGradeWhalf) / 2;
                        //runway labels
                        bitCan.drawText("LM", -RMx, bottomYtext, okPaintText);
                        bitCan.drawText("RM", RMx, bottomYtext, okPaintText);
                    }

                    //scale distance to per pixel
                    double scaleLmpp = surveyData.getLength(false)
                            / (runLhalf * 2);

                    DecimalFormat df = new DecimalFormat("#0.0");

                    //                    LZParser.getInstance().GetAircraftNames();

                    //loop
                    for (int j = 0; j < actualDataSeries.size(); j++) {
                        AllTransverseGradientsGraphActivity.GradientData data = actualDataSeries
                                .get(j);
                        //                    }
                        //                    for (AllTransverseGradientsGraphActivity.GradientData data : actualDataSeries) {
                        if (actives[j]) {//data.isEnabled()){
                            //TESTING SINGLE GRADIENT
                            //AllTransverseGradientsGraphActivity.GradientData data = actualDataSeries.get(0);
                            //

                            //draw the gradient
                            double gradDistFromAnchor = data
                                    .getDistFromAnchor();//actualDataSeries.get(0);
                            ArrayList<GradientDataPoint> gradPoints = data
                                    .getPoints();
                            LZEdges edges = data.getEdges();

                            //                        if(gradPoints.size() == 16) {//8 sections, 2 points per section

                            //                            GradientDataPoint currPt = gradPoints.get(0);
                            //                            double avgY = currPt.getY();//figure out the average alt for normalized drawing
                            //
                            //                            float minXf = new Double(currPt.getX()).floatValue();
                            //                            float maxXf = minXf;
                            //
                            //                            Path gradPath = new Path();
                            //                            gradPath.moveTo(new Double(currPt.getX()).floatValue(),
                            //                                    new Double(currPt.getY()).floatValue());
                            //
                            //                            for (int i = 1; i < gradPoints.size(); i++) {
                            //                                currPt = gradPoints.get(i);
                            //                                float ptXf = new Double(currPt.getX()).floatValue();
                            //
                            //                                if (ptXf < minXf) {
                            //                                    minXf = ptXf;
                            //                                }
                            //                                if (ptXf > maxXf) {
                            //                                    maxXf = ptXf;
                            //                                }
                            //
                            //                                gradPath.lineTo(ptXf,
                            //                                        new Double(currPt.getY()).floatValue());
                            //                                //                            gradPath.lineTo(ptXf, new Double(currPt.getY()+(i*-10)).floatValue());
                            //
                            //                                avgY += currPt.getY();
                            //                            }
                            double avgY = 0d;

                            float minXf = Float.MAX_VALUE;
                            float maxXf = Float.MIN_VALUE;

                            for (GradientDataPoint currPt : gradPoints) {

                                double ptXf = currPt.getX();

                                if (ptXf < minXf)
                                    minXf = (float) ptXf;
                                if (ptXf > maxXf)
                                    maxXf = (float) ptXf;

                                avgY += currPt.getY();
                            }
                            avgY = (avgY) / gradPoints.size();

                            Path gradPath = new Path();

                            GradientDataPoint currPt = gradPoints.get(0);

                            gradPath.moveTo((float) currPt.getX(),
                                    (float) (currPt.getY() - avgY));

                            for (int i = 1; i < gradPoints.size(); i++) {
                                currPt = gradPoints.get(i);

                                gradPath.lineTo((float) currPt.getX(),
                                        (float) (currPt.getY() - avgY));
                            }

                            float longestGradientPix = 0f;
                            if ((minXf >= 0 && maxXf >= 0)
                                    || (minXf <= 0 && maxXf <= 0)) {
                                longestGradientPix = Math.abs(minXf - maxXf);
                            } else {
                                longestGradientPix = Math.abs(minXf)
                                        + Math.abs(maxXf);
                            }

                            //scale value as a pixel ratio
                            float scaleWfactor = (w - (buf * 2))
                                    / longestGradientPix;
                            //                            float scaleWfactor2 = new Double(edges.MaintainedAreaWidth_m/runMaintainedWhalf).floatValue();
                            //
                            //                            Log.d("TDSV_ATSK","SCALE FACTORS 1: "+scaleWfactor+" 2: "+scaleWfactor2);

                            //                        double appOverrun = surveyData.getMetaDouble("stdApproachOverrun", -1.0);

                            //translate to approximate gradient position on runway

                            //draw gradient
                            bitCan.save();
                            float toTranslate = (float) (runLhalf
                                    - ((gradDistFromAnchor) / scaleLmpp));
                            bitCan.translate(0, toTranslate);
                            Matrix m = new Matrix();
                            m.preScale(scaleWfactor, scaleWfactor * scaleY);
                            gradPath.transform(m);

                            //                        bitCan.scale(3f, 3f);
                            bitCan.drawPath(gradPath, okPaint);
                            bitCan.restore();

                            bitCan.save();
                            toTranslate = (float) (runLhalf
                                    - ((gradDistFromAnchor) / scaleLmpp));
                            bitCan.translate(0, toTranslate);

                            //draw gradient text
                            Paint tempPaint;

                            if (RMx > 0) {
                                if ((crit.MaintainedTransversePosMAX >= edges.LeftMaintainedAreaGradient
                                        && edges.LeftMaintainedAreaGradient >= crit.MaintainedTransversePosMIN)
                                        ||
                                        (crit.MaintainedTransverseNegMAX <= edges.LeftMaintainedAreaGradient
                                        && edges.LeftMaintainedAreaGradient <= crit.MaintainedTransverseNegMIN)) {
                                    tempPaint = okPaintText;
                                } else {
                                    tempPaint = badPaintText;
                                }

                                bitCan.drawText(
                                        df.format(edges.LeftMaintainedAreaGradient)
                                                + "",
                                        -RMx,
                                        (tempPaint.getTextSize()), tempPaint);
                            }

                            if (RGx > 0) {
                                if ((crit.GradedTransversePosMAX >= edges.LeftGradedAreaGradient
                                        && edges.LeftGradedAreaGradient >= crit.GradedTransversePosMIN)
                                        ||
                                        (crit.GradedTransverseNegMAX <= edges.LeftGradedAreaGradient
                                        && edges.LeftGradedAreaGradient <= crit.GradedTransverseNegMIN)) {
                                    tempPaint = okPaintText;
                                } else {
                                    tempPaint = badPaintText;
                                }

                                bitCan.drawText(
                                        df.format(edges.LeftGradedAreaGradient)
                                                + "", -RGx,
                                        (tempPaint.getTextSize()), tempPaint);
                            }

                            if (RSx > 0) {
                                if ((crit.ShoulderTransversePosMAX >= edges.LeftShoulderGradient
                                        && edges.LeftShoulderGradient >= crit.ShoulderTransversePosMIN)
                                        ||
                                        (crit.ShoulderTransverseNegMAX <= edges.LeftShoulderGradient
                                        && edges.LeftShoulderGradient <= crit.ShoulderTransverseNegMIN)) {
                                    tempPaint = okPaintText;
                                } else {
                                    tempPaint = badPaintText;
                                }

                                bitCan.drawText(
                                        df.format(edges.LeftShoulderGradient)
                                                + "", -RSx,
                                        (tempPaint.getTextSize()), tempPaint);
                            }

                            if (RRx > 0) {
                                if ((crit.RunwayTransversePosMAX >= edges.LeftHalfRunwayGradient
                                        && edges.LeftHalfRunwayGradient >= crit.RunwayTransversePosMIN)
                                        ||
                                        (crit.RunwayTransverseNegMAX <= edges.LeftHalfRunwayGradient
                                        && edges.LeftHalfRunwayGradient <= crit.RunwayTransverseNegMIN)) {
                                    tempPaint = okPaintText;
                                } else {
                                    tempPaint = badPaintText;
                                }

                                bitCan.drawText(
                                        df.format(edges.LeftHalfRunwayGradient)
                                                + "", -RRx,
                                        (tempPaint.getTextSize()), tempPaint);
                            }

                            if (RRx > 0) {
                                if ((crit.RunwayTransversePosMAX >= edges.RightHalfRunwayGradient
                                        && edges.RightHalfRunwayGradient >= crit.RunwayTransversePosMIN)
                                        ||
                                        (crit.RunwayTransverseNegMAX <= edges.RightHalfRunwayGradient
                                        && edges.RightHalfRunwayGradient <= crit.RunwayTransverseNegMIN)) {
                                    tempPaint = okPaintText;
                                } else {
                                    tempPaint = badPaintText;
                                }

                                bitCan.drawText(
                                        df.format(edges.RightHalfRunwayGradient)
                                                + "",
                                        RRx, (tempPaint.getTextSize()),
                                        tempPaint);
                            }

                            if (RSx > 0) {
                                if ((crit.ShoulderTransversePosMAX >= edges.RightShoulderGradient
                                        && edges.RightShoulderGradient >= crit.ShoulderTransversePosMIN)
                                        ||
                                        (crit.ShoulderTransverseNegMAX <= edges.RightShoulderGradient
                                        && edges.RightShoulderGradient <= crit.ShoulderTransverseNegMIN)) {
                                    tempPaint = okPaintText;
                                } else {
                                    tempPaint = badPaintText;
                                }
                                bitCan.drawText(
                                        df.format(edges.RightShoulderGradient)
                                                + "", RSx,
                                        (tempPaint.getTextSize()), tempPaint);
                            }

                            if (RGx > 0) {
                                if ((crit.GradedTransversePosMAX >= edges.RightGradedAreaGradient
                                        && edges.RightGradedAreaGradient >= crit.GradedTransversePosMIN)
                                        ||
                                        (crit.GradedTransverseNegMAX <= edges.RightGradedAreaGradient
                                        && edges.RightGradedAreaGradient <= crit.GradedTransverseNegMIN)) {
                                    tempPaint = okPaintText;
                                } else {
                                    tempPaint = badPaintText;
                                }
                                bitCan.drawText(
                                        df.format(edges.RightGradedAreaGradient)
                                                + "",
                                        RGx,
                                        (tempPaint.getTextSize()), tempPaint);
                            }

                            if (RMx > 0) {
                                if ((crit.MaintainedTransversePosMAX >= edges.RightMaintainedAreaGradient
                                        && edges.RightMaintainedAreaGradient >= crit.MaintainedTransversePosMIN)
                                        ||
                                        (crit.MaintainedTransverseNegMAX <= edges.RightMaintainedAreaGradient
                                        && edges.RightMaintainedAreaGradient <= crit.MaintainedTransverseNegMIN)) {
                                    tempPaint = okPaintText;
                                } else {
                                    tempPaint = badPaintText;
                                }
                                bitCan.drawText(
                                        df.format(edges.RightMaintainedAreaGradient)
                                                + "",
                                        RMx,
                                        (tempPaint.getTextSize()), tempPaint);
                            }

                            //                        Log.d("TDSF_ATSK", "LeftMaintainedAreaGradient: "+edges.LeftMaintainedAreaGradient);
                            //                        Log.d("TDSF_ATSK", "LeftGradedAreaGradient: "+edges.LeftGradedAreaGradient);
                            //                        Log.d("TDSF_ATSK", "LeftShoulderGradient: "+edges.LeftShoulderGradient);
                            //                        Log.d("TDSF_ATSK", "LeftHalfRunwayGradient: "+edges.LeftHalfRunwayGradient);
                            //                        Log.d("TDSF_ATSK", "RightHalfRunwayGradient: "+edges.RightHalfRunwayGradient);
                            //                        Log.d("TDSF_ATSK", "RightShoulderGradient: "+edges.RightShoulderGradient);
                            //                        Log.d("TDSF_ATSK", "RightGradedAreaGradient: "+edges.RightGradedAreaGradient);
                            //                        Log.d("TDSF_ATSK", "RightMaintainedAreaGradient: "+edges.RightMaintainedAreaGradient);

                            //translate back from gradient
                            bitCan.restore();
                            //                        }
                        }

                    }//end loop

                    //translate back from center
                    bitCan.restore();

                    //draw the bitmap to the screen
                    c.drawBitmap(b, new Matrix(), new Paint());

                    drawingThread.holder.unlockCanvasAndPost(c);
                }
            });
        }
    }
}
