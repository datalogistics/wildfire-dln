
package com.gmeci.atsk.gradient.graphs;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;

import com.gmeci.conversions.Conversions;
import com.gmeci.core.ATSKConstants;
import com.gmeci.core.LZEdges;
import com.gmeci.core.SurveyData;
import com.gmeci.atsk.gradient.graphs.GradientGraphActivityBase.GradientDataPoint;

import java.text.DecimalFormat;
import java.util.ArrayList;

/**
 * Created by sommerj on 4/25/2016.
 */
public class LongitudinalDiagramSurfaceView extends DiagramSurfaceView {

    private ArrayList<GradientGraphActivityBase.GradientDataPoint> actualDataSeries;
    private LZEdges lzEdges;

    protected LongitudinalDiagramSurfaceView(
            Context context,
            SurveyData surveyData,
            ArrayList<GradientGraphActivityBase.GradientDataPoint> actualDataSeries,
            LZEdges lzEdges) {
        super(context, surveyData);
        this.actualDataSeries = actualDataSeries;
        this.lzEdges = lzEdges;
    }

    @Override
    public void show() {
        if (drawingThread != null && actualDataSeries != null
                && !actualDataSeries.isEmpty()) {

            final DecimalFormat df = new DecimalFormat("#0.0");

            GradientDataPoint pt = actualDataSeries.get(0);
            double ptx = pt.getX();
            double pty = pt.getY();
            GradientDataPoint nextPt = actualDataSeries.get(actualDataSeries
                    .size() - 1);
            double nptx = nextPt.getX();
            double npty = nextPt.getY();

            double slope = (npty - pty) / (nptx - ptx);
            String overallText = "Overall Gradient: " + df.format(slope) + "%";
            if (lzEdges.LongitudinalGradientOverall > lzEdges.GradientThreshholdLZLonOverallMax) {
                overallText = overallText + "  Out of Criteria!";
            }

            final String overallTextFinal = overallText;

            final String[] texts = new String[actualDataSeries.size() - 1];

            for (int i = 0; i < actualDataSeries.size(); i++) {
                if (i + 1 < actualDataSeries.size()) {
                    //                    if(actives[i]){
                    pt = actualDataSeries.get(i);
                    ptx = pt.getX();
                    pty = pt.getY();
                    nextPt = actualDataSeries.get(i + 1);
                    nptx = nextPt.getX();
                    npty = nextPt.getY();

                    slope = (npty - pty) / (nptx - ptx);
                    String slopeStr = df.format(slope) + "%";
                    if (Math.abs(slope) > lzEdges.GradientThreshholdLZLonIntervalMax) {
                        slopeStr = slopeStr + " Out of Criteria!";
                    }
                    texts[i] = slopeStr;
                    //                    }else{
                    //                        texts[i] = "";
                    //                    }

                }
            }

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

                    float buf = 80f;
                    float opW = w - (buf * 2);

                    if (overallTextFinal != null && !overallTextFinal.isEmpty()) {
                        bitCan.save();
                        bitCan.translate(buf, h * 0.9f);

                        Paint tempPaint;
                        if (overallTextFinal.endsWith(" Out of Criteria!")) {
                            tempPaint = badPaintText;
                        } else {
                            tempPaint = okPaintText;
                        }

                        tempPaint.setTextAlign(Paint.Align.LEFT);
                        bitCan.drawText(overallTextFinal, 0, 0, tempPaint);
                        if (scaleY != 1) {
                            bitCan.drawText(
                                    "Elevation Magnification " + scaleY,
                                    0,
                                    -tempPaint.ascent(),
                                    okPaintText);
                        }
                        tempPaint.setTextAlign(Paint.Align.CENTER);
                        bitCan.restore();
                    }

                    //translate to center
                    bitCan.save();
                    bitCan.translate(buf, h / 2);

                    //scale distance to per pixel
                    //                    float scaleLmpp = new Double(surveyData.getLength(true)
                    //                            / opW).floatValue();

                    //                    //TODO
                    //                    Log.d("LDSV_ATSK","LONGITUDINAL GRADIENT DATA POINTS");
                    //                    for(GradientDataPoint pt : actualDataSeries){
                    //                        Log.d("LDSV_ATSK","X: "+pt.getX() + "   Y: "+pt.getY());
                    //                    }
                    GradientDataPoint pt = actualDataSeries.get(0);

                    double minX = pt.getX();

                    double lenX = (actualDataSeries.get(
                            actualDataSeries.size() - 1).getX()
                            - minX);
                    float scaleX = (float) (opW / lenX);//pix/ft

                    double avgY = pt.getY();
                    double maxY = pt.getY();

                    //points for missing data visualization
                    String missingDataText = "Missing Data between: ";
                    boolean missingData = false;
                    final float[] missingPts = new float[(actualDataSeries
                            .size() + 1) * 4];//+2 segments for runway start and end
                    //test the first point
                    if (pt.getX() > ATSKConstants.GRADIENT_SPACING_LONGITUDINAL_FT) {
                        missingPts[0] = 0;
                        missingPts[1] = (float) (pt.getY() * scaleX * scaleY);
                        missingPts[2] = (float) (pt.getX() * scaleX);
                        missingPts[3] = (float) (pt.getY() * scaleX * scaleY);

                        missingDataText += ("Start, "
                                + (int) pt.getX() + "ft : ");

                        missingData = true;
                    }

                    //array for x,y,x,y coordinates for the spike lines
                    float[] spikeDubs = new float[actualDataSeries.size() * 4]; // indexes : x = i*2, y = x+1
                    spikeDubs[0] = (float) (pt.getX() * scaleX);
                    spikeDubs[1] = (float) (pt.getY() * scaleX * scaleY);
                    spikeDubs[2] = (float) (pt.getX() * scaleX);

                    Path gradPath = new Path();
                    gradPath.moveTo(
                            (float) (pt.getX()),
                            (float) (pt.getY()));

                    for (int i = 1; i < actualDataSeries.size(); i++) {
                        pt = actualDataSeries.get(i);
                        double pty = pt.getY();
                        avgY += pty;

                        if (pty > maxY) {
                            maxY = pty;
                        }

                        gradPath.lineTo((float) (pt.getX()),
                                (float) (pt.getY()));

                        int sindx = i * 4;
                        spikeDubs[sindx] = (float) (pt.getX() * scaleX);
                        spikeDubs[sindx + 1] = (float) (pt.getY() * scaleX
                                * scaleY);
                        spikeDubs[sindx + 2] = (float) (pt.getX() * scaleX);

                        //missing points
                        GradientDataPoint prevpt = actualDataSeries.get(i - 1);
                        int j = i * 4;
                        if ((pt.getX() - prevpt.getX()) > (ATSKConstants.GRADIENT_SPACING_LONGITUDINAL_FT)) {
                            missingPts[j] = (float) (prevpt.getX() * scaleX);
                            missingPts[j + 1] = (float) (prevpt.getY()
                                    * scaleX * scaleY);
                            missingPts[j + 2] = (float) (pt.getX() * scaleX);
                            missingPts[j + 3] = (float) (pt.getY() * scaleX
                                    * scaleY);

                            missingDataText += (int) prevpt.getX()
                                    + "ft, "
                                    + (int) pt.getX() + "ft : ";

                            missingData = true;
                        }

                    }

                    for (int i = 3; i < spikeDubs.length; i += 4) {
                        spikeDubs[i] = (float) ((maxY * scaleX * scaleY)
                                - buf);
                    }

                    //test the last point
                    double last = (surveyData.getLength(false) * Conversions.M2F)
                            - (pt.getX());
                    if (last > 0
                            && last > ATSKConstants.GRADIENT_SPACING_LONGITUDINAL_FT) {
                        missingPts[missingPts.length - 4] = (float) (pt
                                .getX() * scaleX);
                        missingPts[missingPts.length - 3] = (float) (pt
                                .getY() * scaleX * scaleY);
                        missingPts[missingPts.length - 2] = (float) (
                                surveyData.getLength(false) * Conversions.M2F
                                        * scaleX);
                        missingPts[missingPts.length - 1] = (float) (pt
                                .getY() * scaleX * scaleY);

                        missingDataText += (int) pt.getX() + "ft, End";

                        missingData = true;
                    }

                    avgY = (avgY/* * scaleX * scaleY*/)
                            / actualDataSeries.size();

                    //draw runway numbers
                    bitCan.drawText(getAngle() + "", -buf / 2, 0, backPaintText);
                    bitCan.drawText(getBackAngle() + "", opW + (buf / 2), 0,
                            backPaintText);

                    bitCan.save();
                    //                    bitCan.scale(scaleX, scaleX);
                    //                    float toTranslate = new Double(-avgY).floatValue();
                    //                    bitCan.translate(0, toTranslate);
                    bitCan.translate(0,
                            (float) (-avgY * scaleX * scaleY));

                    Matrix m = new Matrix();
                    m.preScale(scaleX, scaleX * scaleY);
                    gradPath.transform(m);
                    bitCan.drawPath(gradPath, okPaint);
                    //                    bitCan.restore();
                    //
                    //                    bitCan.save();
                    //                    bitCan.scale(scaleX, 1);
                    //                    bitCan.translate(0, toTranslate);
                    bitCan.drawLines(spikeDubs, backPaint);

                    bitCan.drawLines(missingPts, badPaint);

                    //                    Conversions.M2F

                    okPaintText.setTextAlign(Paint.Align.LEFT);
                    for (int i = 0; i <= texts.length; i++) {
                        if (actives[i]) {
                            int sindx = 2 + (4 * i);

                            int feetTxtInt = (int) (spikeDubs[sindx]
                                    / scaleX);

                            String txt = "STA+" + feetTxtInt;

                            bitCan.save();
                            bitCan.translate(spikeDubs[sindx],
                                    spikeDubs[sindx + 1]);
                            bitCan.rotate(-90);
                            bitCan.drawText(txt, 0, 0, okPaintText);

                            if (i > 0) {
                                String txt2 = texts[i - 1];
                                bitCan.translate(0,
                                        okPaintText.getFontMetrics().ascent
                                                * -1);
                                bitCan.drawText(txt2, 0, 0, okPaintText);
                            }

                            bitCan.restore();
                        }
                    }
                    okPaintText.setTextAlign(Paint.Align.CENTER);

                    bitCan.restore();

                    //translate back from center
                    bitCan.restore();

                    //text if there's mssing data
                    if (missingData) {
                        bitCan.save();
                        bitCan.translate(buf, h * 0.80f);
                        badPaintText.setTextAlign(Paint.Align.LEFT);
                        bitCan.drawText(missingDataText, 0, 0, badPaintText);
                        badPaintText.setTextAlign(Paint.Align.CENTER);
                        bitCan.restore();
                    }

                    //draw the bitmap to the screen
                    c.drawBitmap(b, new Matrix(), new Paint());

                    drawingThread.holder.unlockCanvasAndPost(c);
                }
            });
        }
    }
}
