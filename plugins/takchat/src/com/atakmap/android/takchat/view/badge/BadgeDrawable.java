
package com.atakmap.android.takchat.view.badge;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;

/**
 * Copy of same class in ATAK project due to context issue
 * Extended with Text Size and connectivity layer
 */
public class BadgeDrawable extends Drawable {

    public static final float DEFAULT_TEXT_SIZE = 14f;

    private float mTextSize;
    private final Paint mBadgePaint;
    private Boolean mConnected;
    private final Paint mConnectedPaint;
    private final Paint mDisconnectedPaint;
    private final Paint mTextPaint;
    private final Rect mTxtRect = new Rect();

    private String mCount = "";
    private boolean mDrawBadge = false;

    public BadgeDrawable(Context context) {
        mConnected = null;

        //mTextSize = context.getResources().getDimension(R.dimen.badge_text_size);
        mTextSize = DEFAULT_TEXT_SIZE * context.getResources().getDisplayMetrics().density;

        mBadgePaint = new Paint();
        mBadgePaint.setColor(Color.RED);
        mBadgePaint.setAntiAlias(true);
        mBadgePaint.setStyle(Paint.Style.FILL);

        mConnectedPaint = new Paint();
        mConnectedPaint.setColor(Color.GREEN);
        mConnectedPaint.setAntiAlias(true);
        mConnectedPaint.setStyle(Paint.Style.FILL);

        mDisconnectedPaint = new Paint();
        mDisconnectedPaint.setColor(Color.RED);
        mDisconnectedPaint.setAntiAlias(true);
        mDisconnectedPaint.setStyle(Paint.Style.FILL);

        mTextPaint = new Paint();
        mTextPaint.setColor(Color.WHITE);
        mTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
        mTextPaint.setTextSize(mTextSize);
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
    }

    @Override
    public void draw(Canvas canvas) {
        if(!mDrawBadge && mConnected == null){
            return;
        }

        Rect bounds = getBounds();
        float width = bounds.right - bounds.left;
        float height = bounds.bottom - bounds.top;

        // Position the badge in the top-right quadrant of the icon.
        float radius = ((Math.min(width, height) / 2) - 1) / 2;
        float centerX = width - radius - 1;
        float centerY = radius + 1;

        if (mDrawBadge) {
            // Draw badge circle.
            canvas.drawCircle(centerX, centerY, radius, mBadgePaint);

            // Draw badge count text inside the circle.
            mTextPaint.getTextBounds(mCount, 0, mCount.length(), mTxtRect);
            float textHeight = mTxtRect.bottom - mTxtRect.top;
            float textY = centerY + (textHeight / 2f);
            canvas.drawText(mCount, centerX, textY, mTextPaint);
        }

        //optionally draw connectivity indicator
        if(mConnected != null) {
            centerX = width - radius - 1;
            centerY = height - radius + 1;

            //radius a bit smaller
            float rad2 = radius * 0.75f;
            canvas.drawCircle(centerX, centerY, rad2,
                    mConnected ? mConnectedPaint : mDisconnectedPaint);
        }
    }

    /*
     * Sets the count (i.e notifications) to display.
     */
    public void setCount(int count) {
        mCount = Integer.toString(count);

        // Only draw a badge if there are notifications.
        mDrawBadge = count > 0;
        invalidateSelf();
    }

    public void setCount(int count, float textSize){
        //TODO any sync/threading issues?
        mTextSize = textSize;
        mTextPaint.setTextSize(mTextSize);

        setCount(count);
    }

    public void setCount(int count, Boolean bConnected) {
        mConnected = bConnected;
        setCount(count);
    }

    @Override
    public void setAlpha(int alpha) {
        // do nothing
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        // do nothing
    }

    @Override
    public int getOpacity() {
        return PixelFormat.UNKNOWN;
    }
}
