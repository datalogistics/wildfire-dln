
package com.gmeci.atsk.az.hlz;

import android.app.ActionBar.LayoutParams;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Path;
import android.graphics.Paint.Style;
import android.graphics.DashPathEffect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.gmeci.core.PointObstruction;
import com.gmeci.helpers.AZHelper;
import com.gmeci.conversions.Conversions;

public class QuadrantView extends View {

    private final Paint approachPaint = new Paint();
    private final Paint bluePaint = new Paint();
    private final Paint blackPaint = new Paint();
    private final Paint departurePaint = new Paint();
    private final Paint textPaint = new Paint();
    private final Paint ltPaint = new Paint();
    private final Paint gtPaint = new Paint();
    private final RectF oval = new RectF();
    private final Path _path = new Path();

    private double apprAngle_rad;
    private double depAngle_rad;
    private final int[] _center = new int[2];
    private final float[] _hsv = new float[3];
    private PointObstruction[][] _badQuads = new PointObstruction[4][];
    private AngleCalculatedInterface _angleConsumer;

    public QuadrantView(Context context) {
        super(context);
        setupColors();
        setLayoutParams(new LayoutParams(getWidth(), getWidth()));
    }

    public QuadrantView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setupColors();
        setLayoutParams(new LayoutParams(getWidth(), getWidth()));
    }

    public QuadrantView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setupColors();
        setLayoutParams(new LayoutParams(getWidth(), getWidth()));
    }

    public void InitView(AngleCalculatedInterface angleConsumer) {
        _angleConsumer = angleConsumer;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int w = this.getMeasuredWidth();
        setMeasuredDimension(w, w);
    }

    @Override
    public void onDraw(Canvas can) {
        super.onDraw(can);
        //Determine where the center should be based on the device size we're using
        _center[0] = getWidth() / 2;
        _center[1] = getWidth() / 2;
        double radius = getRadius();

        /*Handle the drawing of circle*/
        can.drawCircle(_center[0], _center[1], (float) radius, blackPaint);
        double PositionAt45 = (float) (radius * Math
                .cos(45 * Conversions.DEG2RAD));
        //Shade any bad quadrants
        oval.set((float) (_center[0] - radius),
                (float) (_center[1] - radius),
                (float) (_center[0] + radius),
                (float) (_center[1] + radius));
        if (_badQuads != null) {
            for (int i = 0; i < _badQuads.length; i++) {
                if (_badQuads[i] == null)
                    continue;
                int ang;
                switch (i) {
                    default:
                    case AZHelper.TOP:
                        ang = -135;
                        break;
                    case AZHelper.BOTTOM:
                        ang = 45;
                        break;
                    case AZHelper.LEFT:
                        ang = 135;
                        break;
                    case AZHelper.RIGHT:
                        ang = -45;
                        break;
                }
                Paint p = _badQuads[i][0].speed > AZHelper
                        .TEN_TO_ONE_ELEV_DEG ? ltPaint : gtPaint;
                can.drawArc(oval, ang, 90, true, p);
                String ratio = Conversions.ConvertGlideSlopeAngleToRatio(
                        _badQuads[i][0].speed);
                double tRad = Math.toRadians(ang + 135);

                Color.colorToHSV(p.getColor(), _hsv);
                _hsv[2] = 1f;
                textPaint.setColor(Color.HSVToColor(_hsv));
                can.drawText(ratio,
                        (float) (_center[0] + Math.sin(tRad) * (radius / 2)),
                        (float) (_center[1] - Math.cos(tRad) * (radius / 2)),
                        textPaint);
            }
        }
        //top left to bottom right
        can.drawLine((float) (_center[0] + PositionAt45),
                (float) (_center[0] + PositionAt45),
                (float) (_center[0] + -1 * PositionAt45),
                (float) (_center[0] + -1 * PositionAt45), bluePaint);
        can.drawLine((float) (_center[0] + PositionAt45),
                (float) (_center[0] + -1 * PositionAt45),
                (float) (_center[0] + -1 * PositionAt45),
                (float) (_center[0] + PositionAt45), bluePaint);

        //Draw the approach and departure lines
        double approachEndX = _center[0] - (radius * 1.1)
                * (Math.cos(apprAngle_rad));
        double approachEndY = _center[1] + (radius * 1.1)
                * (Math.sin(apprAngle_rad));
        double departureEndX = _center[0] + (radius * 1.1)
                * (Math.cos(depAngle_rad));
        double departureEndY = _center[1] - (radius * 1.1)
                * (Math.sin(depAngle_rad));

        _path.moveTo((float) _center[0], (float) _center[1]);
        _path.lineTo((float) approachEndX, (float) approachEndY);
        can.drawPath(_path, approachPaint);
        _path.reset();

        _path.moveTo((float) _center[0], (float) _center[1]);
        _path.lineTo((float) departureEndX, (float) departureEndY);
        can.drawPath(_path, departurePaint);
        _path.reset();
    }

    public float getRadius() {
        return (getWidth() / 2f) * 0.9f;
    }

    private void setupColors() {
        approachPaint.setColor(Color.parseColor("#ff00ffff"));
        approachPaint.setStyle(Style.STROKE);
        approachPaint.setStrokeWidth(10);
        approachPaint.setPathEffect(new DashPathEffect(new float[] {
                5, 5
        }, 0));

        departurePaint.setColor(Color.parseColor("#ffff0000"));
        departurePaint.setStyle(Style.STROKE);
        departurePaint.setStrokeWidth(10);
        departurePaint.setPathEffect(new DashPathEffect(new float[] {
                5, 5
        }, 5));

        bluePaint.setColor(Color.parseColor("#ff0d749a"));
        bluePaint.setStrokeWidth(6);
        blackPaint.setColor(Color.BLACK);
        ltPaint.setColor(0xFF99990F);
        gtPaint.setColor(0xFF0F990F);

        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(32f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
    }

    public void UpdateDrawing(double approach, double departure,
            PointObstruction[][] badQuads) {
        apprAngle_rad = (90 - approach) * Conversions.DEG2RAD;
        depAngle_rad = (90 - departure) * Conversions.DEG2RAD;

        _badQuads = badQuads;
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        int action = event.getActionMasked();

        //get the angle from the middle
        double StartX = getWidth() / 2f;
        double StartY = getWidth() / 2f;

        double StopX = event.getX();//- this.getLeft();
        double StopY = event.getY();//- this.getLeft();

        // Ignore touch events outside circle
        if (Math.hypot(StopX - StartX, StopY - StartY) > getRadius())
            return false;

        double Angle = 90 - (float) (Math.atan2(
                StopY - StartY, StopX - StartX) * 180f / Math.PI);
        return _angleConsumer.NewAngle(-1 * Angle, action);
    }

    interface AngleCalculatedInterface {
        boolean NewAngle(double NewAngle_deg_t, int action);
    }

}
