/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.camera.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import com.android.camera.FocusOverlayManager_Plus;
import com.android.camera.debug.Log;
import com.android.camera2.R;

/**
 * Displays a focus indicator.
 */
public class FocusAndMeteringOverlay_Plus extends FocusOverlay implements
        FocusOverlayManager_Plus.FocusUI {
    private static final Log.Tag TAG = new Log.Tag("FocusAndMeteringOverlay_Plus");
    private final Drawable mFocusSwitcherIndicator;
    private final Drawable mMeteringIndicator;
    private final Drawable mMeteringIndicatorLocked;
    private final Rect mFocusOuterRingBounds = new Rect();
    private final Rect mFocusIndicatorBounds = new Rect();
    private final Rect mFocusSwitcherBounds = new Rect();
    private final Rect mMeteringBounds = new Rect();
    private final int mMeteringIndicatorSize;
    private final int mFocusSwitcherIndicatorSize;
    /** true if focus area and metering area is separated. */
    private boolean mIsSeparating;
    /** true if SeparateMetering Preference is on And MeteringArea Supported. */
    private boolean mIsSeparateMode = false;
    private boolean mIsMeteringLock = false;

    /** if true, enable Separate Metering by click switcher. */
    private boolean mUseFocusSwitcher = false;
    
    public FocusAndMeteringOverlay_Plus(Context context, AttributeSet attrs) {
        super(context, attrs);
        mFocusSwitcherIndicator = getResources().getDrawable(R.drawable.focus_touch_switcher);
        mMeteringIndicator = getResources().getDrawable(R.drawable.metering_touch);
        mMeteringIndicatorLocked = getResources().getDrawable(R.drawable.metering_touch_lock);
        mMeteringIndicatorSize = getResources().getDimensionPixelSize(R.dimen.metering_indicator_size);
        mFocusSwitcherIndicatorSize = getResources().getDimensionPixelSize(R.dimen.focus_switcher_indicator_size);
    }

    @Override
    public void setSeparateMode(boolean value) {
        this.mIsSeparateMode = value;
    }

    @Override
    public void setSeparating(boolean isSeparating) {
        if (mIsSeparating != isSeparating) {
            int resId = R.drawable.focus_ring_touch_inner;
            Rect bound = mFocusIndicator.copyBounds();
            mFocusIndicator = getResources().getDrawable(resId);
            mFocusIndicator.setBounds(bound);
            mIsSeparating = isSeparating;
            invalidate();
        }
    }

    @Override
    public boolean isSeparating() {
        return mIsSeparating;
    }

    @Override
    /** called by user drag focus indicator. */
    public void setFocusPosition(int x, int y, boolean isPassiveScan) {
        super.setFocusPosition(x, y, isPassiveScan, 0, 0);
        mFocusOuterRingBounds.set(mFocusOuterRing.copyBounds());
        mFocusIndicatorBounds.set(mFocusIndicator.copyBounds());
    }
    /** called by user singleTap screen.
     *  @param aFsize just used for show debug UI.
     *  @param aEsize just used for show debug UI. */
    @Override
    public void setFocusPosition(int x, int y, boolean isPassiveScan, int aFsize, int aEsize) {
        super.setFocusPosition(x, y, isPassiveScan, aFsize, aEsize);
        mFocusOuterRingBounds.set(mFocusOuterRing.copyBounds());
        mFocusIndicatorBounds.set(mFocusIndicator.copyBounds());
        // show FocusSwitcher just on onSingleTapUp --> auto focus.
        if (!isPassiveScan && mIsSeparateMode && mUseFocusSwitcher)  {
            setFocusSwitcherPosition(x, y);
        } else {
            mFocusSwitcherIndicator.setBounds(new Rect());
            mMeteringIndicator.setBounds(new Rect());
            mMeteringIndicatorLocked.setBounds(new Rect());
            invalidate();
        }
    }

    @Override
    public void setMeteringPosition(int x, int y, boolean isPassiveScan) {
        mMeteringBounds.set(x - mMeteringIndicatorSize / 2, y - mMeteringIndicatorSize / 2,
                x + mMeteringIndicatorSize / 2, y + mMeteringIndicatorSize / 2);
        mMeteringIndicator.setBounds(mMeteringBounds);
        mMeteringIndicatorLocked.setBounds(mMeteringBounds);
        invalidate();
    }

    public void setFocusSwitcherPosition(int x, int y) {
        int left = x + mFocusOuterRingSize / 4;
        int top  = y - mFocusOuterRingSize / 4 - mFocusSwitcherIndicatorSize;
        int right = left + mFocusSwitcherIndicatorSize;
        int bottom = top + mFocusSwitcherIndicatorSize;

        mFocusSwitcherBounds.set(left, top, right, bottom);
        mFocusSwitcherIndicator.setBounds(mFocusSwitcherBounds);
        invalidate();
    }
    
    @Override
    public void clearFocus() {
        setSeparating(false);
        super.clearFocus();
        invalidate();
    }

    @Override
    public void onDraw(Canvas canvas) {
        // always show FocusIndicator and MeteringIndicator when separating.
        if (mIsSeparating) {
            mShowIndicator = true;
        }
        super.onDraw(canvas);
        if (mShowIndicator) {
            if (!mIsSeparating) {
                mFocusSwitcherIndicator.draw(canvas);
            } else {
                if (mIsMeteringLock) {
                    mMeteringIndicatorLocked.draw(canvas);
                } else {
                    mMeteringIndicator.draw(canvas);
                }
            }
        }
    }

    @Override
    public Rect getFocusSwitcherBoundIfShowed() {
        return mShowIndicator ? mFocusSwitcherBounds : new Rect();
    }
    
    /** used for drag. */
    @Override
    public Rect getFocusBoundIfShowed() {
        return mShowIndicator ? mFocusOuterRingBounds/*mFocusIndicatorBounds*/ : new Rect();
    }
    
    /** used for drag. */
    @Override
    public Rect getMeteringBoundIfShowed() {
        return mShowIndicator ? mMeteringBounds : new Rect();
    }

    @Override
    public void onMeteringLock(boolean locked) {
        mIsMeteringLock = locked;
        invalidate();
    }

    @Override
    public void useFocusSwitcher(boolean use) {
        mUseFocusSwitcher = use;
    }
}
