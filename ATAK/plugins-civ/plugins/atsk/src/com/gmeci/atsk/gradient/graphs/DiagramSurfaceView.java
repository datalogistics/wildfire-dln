
package com.gmeci.atsk.gradient.graphs;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.os.Handler;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.gmeci.core.SurveyData;

/**
 * Created by sommerj on 4/11/2016.
 */
public abstract class DiagramSurfaceView extends SurfaceView implements
        SurfaceHolder.Callback {

    protected DiagramDrawingThread drawingThread;

    protected Paint backPaint;
    protected Paint okPaint;
    protected Paint badPaint;
    protected Paint backPaintText;
    protected Paint okPaintText;
    protected Paint badPaintText;

    protected Bitmap b = null;

    protected SurveyData surveyData;

    protected float scaleY = 1f;

    protected boolean[] actives;

    protected DiagramSurfaceView(Context context, SurveyData surveyData) {
        super(context);

        getHolder().addCallback(this);

        this.surveyData = surveyData;

        backPaint = new Paint();
        backPaint.setColor(0x30000000);
        backPaint.setStrokeWidth(3);
        backPaint.setStyle(Paint.Style.STROKE);

        backPaintText = new Paint();
        backPaintText.setColor(0x30000000);
        backPaintText.setStrokeWidth(3);
        backPaintText.setTextSize(12);
        backPaintText.setTextAlign(Paint.Align.CENTER);

        okPaint = new Paint();
        okPaint.setColor(0xFF000000);
        okPaint.setStrokeWidth(1);
        okPaint.setStyle(Paint.Style.STROKE);

        okPaintText = new Paint();
        okPaintText.setColor(0xFF000000);
        okPaintText.setStrokeWidth(3);
        okPaintText.setTextSize(12);
        okPaintText.setTextAlign(Paint.Align.CENTER);

        badPaint = new Paint();
        badPaint.setColor(0xFFFF3030);
        badPaint.setStrokeWidth(3);
        badPaint.setStyle(Paint.Style.STROKE);

        badPaintText = new Paint();
        badPaintText.setColor(0xFFFF3030);
        badPaintText.setStrokeWidth(3);
        badPaintText.setTextSize(12);
        badPaintText.setTextAlign(Paint.Align.CENTER);
    }

    public abstract void show();

    public void setActives(boolean[] actives) {
        this.actives = actives;
    }

    public boolean[] getActives() {
        return actives;
    }

    public void setScaleY(float scaleY) {
        this.scaleY = scaleY;
    }

    public Bitmap getBackingBitmap() {
        return b;
    }

    /**
     * gets angle / 10 for diagram displays
     * @return survey angle / 10
     */
    protected int getAngle() {
        double ang = surveyData.angle;
        if (ang < 0) {
            ang += 360;
        }
        return (int) (ang) / 10;
    }

    /**
     * gets back angle / 10 for diagram displays
     * @return survey back angle / 10
     */
    protected int getBackAngle() {
        double ang = surveyData.angle;
        if (ang < 0) {
            ang += 360;
        }
        ang += 180;
        if (ang > 360) {
            ang = ang - 360;
        }
        return (int) (ang) / 10;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

        drawingThread = new DiagramDrawingThread(holder);

        show();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
            int height) {
        if (drawingThread != null) {
            drawingThread.surfaceChanged(holder, format, width, height);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        //todo clean up
        b = null;

        if (drawingThread != null) {
            drawingThread = null;
        }
    }

    protected static class DiagramDrawingThread extends Thread {

        public SurfaceHolder holder;
        public Handler handler;

        public DiagramDrawingThread(SurfaceHolder holder) {
            this.holder = holder;
            this.handler = new Handler();
        }

        public void surfaceChanged(SurfaceHolder holder, int format, int width,
                int height) {
            this.holder = holder;
        }

    }
}
