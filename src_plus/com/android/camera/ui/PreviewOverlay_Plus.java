package com.android.camera.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import com.android.camera.debug.Log;

/**
 * Created by ZhangChao on 15-6-5.
 */
public class PreviewOverlay_Plus extends PreviewOverlay {
    private static final Log.Tag TAG = new Log.Tag("PreviewOverlay_Plus");
    private final ZoomRatioIndicator mZoomRatioIndicator = new ZoomRatioIndicator();

    public PreviewOverlay_Plus(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onPreviewAreaChanged(RectF previewArea) {
        super.onPreviewAreaChanged(previewArea);
        mZoomRatioIndicator.layout((int) previewArea.left, (int) previewArea.top,
                (int) previewArea.right, (int) previewArea.bottom);
    }
    
    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mZoomProcessor != null) {
            if (mZoomProcessor.isVisible()) {
                mZoomRatioIndicator.showZoomRatioUI();
            } else {
                mZoomRatioIndicator.hideZoomRatioUI();
            }
            mZoomRatioIndicator.setZoom(mZoomProcessor.mCurrentRatio);
            mZoomRatioIndicator.setAngle(mZoomProcessor.mFingerAngle);
            mZoomRatioIndicator.draw(canvas);
        }
    }

    /**
     * This class show zoom ratio value.
     */
    private class ZoomRatioIndicator {
        private final Log.Tag TAG = new Log.Tag("ZoomRatioIndicator");

        // Continuous Zoom level [0,1].
        private float mCurrentRatio;
        private String mCurrentZoomString;
        private final Paint mPaint;
        private int mCenterX;
        private int mCenterY;
        private boolean mVisible = false;
        private float mDegree;

        public ZoomRatioIndicator() {
            mPaint = new Paint();
            mPaint.setAntiAlias(true);
            mPaint.setColor(Color.WHITE);
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeCap(Paint.Cap.ROUND);
        }

        // Set current zoom ratio from Module.
        public void setZoom(float ratio) {
            mCurrentRatio = ratio;
            mCurrentZoomString = String.format("%.2fx", mCurrentRatio);
        }

        public void setAngle(double angle) {
            mDegree = (float) (180.0f / Math.PI * angle);
            mDegree = (360 - mDegree) % 360;
        }

        public void layout(int l, int t, int r, int b) {
            // TODO: Needs to be centered in preview TextureView
            mCenterX = (r - l) / 2;
            mCenterY = (b - t) / 2;
        }

        public void draw(Canvas canvas) {
            if (!mVisible) {
                return;
            }
            // Draw background.
            // Draw Zoom progress.
            mPaint.setAlpha(255);
            mPaint.setTextSize(36.0f);
            mPaint.setTextAlign(Paint.Align.CENTER);
            canvas.save();
//            canvas.rotate(mDegree, mCenterX, mCenterY);
            canvas.drawText(mCurrentZoomString, mCenterX, mCenterY, mPaint);
            canvas.restore();
        }

        public void showZoomRatioUI() {
            mVisible = true;
            invalidate();
        }

        public void hideZoomRatioUI() {
            mVisible = false;
            invalidate();
        }

        public boolean isVisible() {
            return mVisible;
        }
    }
}
