/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.camera;

import android.graphics.Rect;
import android.os.Looper;
import android.view.MotionEvent;
import android.widget.Toast;

import com.android.camera.app.AppController;
import com.android.camera.debug.Log;
import com.android.camera.util.UsageStatistics;
import com.android.camera2.R;
import com.android.ex.camera2.portability.CameraCapabilities;
import com.android.ex.camera2.portability.CameraCapabilities.FocusMode;

import java.util.List;

/** 
 * {@link FocusOverlayManager}
 */
public class FocusOverlayManager_Plus extends FocusOverlayManager {
    private static final Log.Tag TAG = new Log.Tag("FocusOverlayManager_Plus");
    private static final int DRAG_STATE_NONE = 0;
    private static final int DRAG_STATE_DRAGGING_FOCUS_UI = 1;
    private static final int DRAG_STATE_DRAGGING_METERING_UI = 2;
    private int dragState = DRAG_STATE_NONE;
    private boolean mInSeparateMode = false;
    private static final boolean ENABLE_DRAG_WHEN_LOCKED = true;
    /** if true, enable Separate Metering by click switcher. */
    private static final boolean ENABLE_SEPARATE_BY_SWITCHER = true;
    private static final int METERING_ACCURACY = 50;
    private int mLastMeteringX;
    private int mLastMeteringY;

    public interface FocusUI extends FocusOverlayManager.FocusUI {
        Rect getFocusSwitcherBoundIfShowed();
        Rect getFocusBoundIfShowed();
        Rect getMeteringBoundIfShowed();
        void setMeteringPosition(int x, int y, boolean isPassiveScan);
        /** Don't disappear Focus UI if isSeparating. */
        void setSeparating(boolean isSeparating);
        boolean isSeparating();
        /** just show focus switcher in SeparateMode.*/
        void setSeparateMode(boolean value);
        void onMeteringLock(boolean locked);
        /** if true, enable Separate Metering by click switcher. */
        void useFocusSwitcher(boolean use);
    }

    public interface Listener extends FocusOverlayManager.Listener {
        void setMeteringParameters();
    }
    
    public FocusOverlayManager_Plus(AppController appController, List<FocusMode> defaultFocusModes,
            CameraCapabilities capabilities, FocusOverlayManager.Listener listener, boolean mirror, Looper looper,
            FocusOverlayManager.FocusUI ui) {
        super(appController, defaultFocusModes, capabilities, listener, mirror, looper, ui);
        if (mUI instanceof FocusUI) {
            ((FocusUI) mUI).useFocusSwitcher(ENABLE_SEPARATE_BY_SWITCHER);
        }
    }

    @Override
    public CameraCapabilities.FocusMode getFocusMode(
            final CameraCapabilities.FocusMode currentFocusMode) {
        if (mUI instanceof FocusUI) {
            if (((FocusUI) mUI).isSeparating()) {
                Log.d(TAG, "getFocusMode returning AUTO because isSeparating.");
                return CameraCapabilities.FocusMode.AUTO;
            }
        }
        return super.getFocusMode(currentFocusMode);
    }

    @Override
    public void onSingleTapUp(int x, int y) {
        if (isSeparating() && (isInFocusIndicator(x, y) || isInMeteringIndicator(x, y))) {
            return;
        }
        super.onSingleTapUp(x, y);

        if (!ENABLE_SEPARATE_BY_SWITCHER) {
            if (mInSeparateMode) {
                if (mUI instanceof FocusUI) {
                    ((FocusUI) mUI).setSeparating(true);
                    ((FocusUI) mUI).setMeteringPosition(x, y, false);
                }
            }
        }
    }

    @Override
    public void onAutoFocus(boolean focused, boolean shutterButtonPressed) {
        super.onAutoFocus(focused, shutterButtonPressed);
        if (mUI instanceof FocusUI) {
            if (((FocusUI) mUI).isSeparating()) {
                Log.d(TAG, "removeMessages RESET_TOUCH_FOCUS because isSeparating.");
                mHandler.removeMessages(RESET_TOUCH_FOCUS);
            }
        }
    }

    @Override
    public void resetTouchFocus() {
        if (!mInitialized) {
            return;
        }

        // Put focus indicator to the center. clear reset position
        mUI.clearFocus();
        // Initialize mFocusArea.
        mFocusArea = null;
        mMeteringArea = null;
//        mListener.setFocusParameters();

        if (mTouchCoordinate != null) {
            UsageStatistics.instance().tapToFocus(mTouchCoordinate,
                    0.001f * (System.currentTimeMillis() - mTouchTime));
            mTouchCoordinate = null;
        }

        // reset separate mode
        if (mUI instanceof FocusUI) {
            ((FocusUI) mUI).setSeparating(false);
        }

        if (getAeAwbLock()) {
            unlockAeAwbIfNeeded();
        }
        setMeteringUILock(false, false);
    }


    void lockAeAwbIfNeeded() {
        super.lockAeAwbIfNeeded();
        if (mInSeparateMode && isSeparating()) {
            setMeteringUILock(getAeAwbLock(), true);
        }
    }

    void unlockAeAwbIfNeeded() {
        super.unlockAeAwbIfNeeded();
        if (mInSeparateMode && isSeparating()) {
            setMeteringUILock(getAeAwbLock(), true);
        }
    }

    public boolean inSeparateMode() {
        return mInSeparateMode;
    }

    /** @param inSeparateMode set true to enable Separate Metering */
    public void setSeparateMode(boolean inSeparateMode) {
        // only work if MeteringAreaSupported.
        this.mInSeparateMode = inSeparateMode && mMeteringAreaSupported;
        if (mUI instanceof FocusUI) {
            ((FocusUI) mUI).setSeparateMode(mInSeparateMode);
        }
    }

    /**  */
    public void onMyTouchEvent(MotionEvent event) {
        if (!(mUI instanceof FocusUI)) {
            Log.w(TAG, "return because mUI is not FocusOverlayManager_Plus.FocusUI");
            return;
        }
        if (!mInSeparateMode) {
            Log.w(TAG, "return because SeparateMetering Preference is off");
            return;
        }

        final int action = event.getActionMasked();
        final int x = (int) event.getX();
        final int y = (int) event.getY();

        // avoid the point out of the previewRect.
        if (!mPreviewRect.contains(x, y)) return;

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                // touch FocusSwitcherIndicator to Enable it.
                // if xy is in metering rect, drag metering indicator.
                // if xy is in focus rect, drag focus indicator.
                if (!isSeparating() && isInFocusSwitcherIndicator(x, y)) {
                    ((FocusUI) mUI).setSeparating(true);
                    ((FocusUI) mUI).setMeteringPosition(x, y, false);
                } else if (isInMeteringIndicator(x, y) && isSeparating()) {
                    Log.d(TAG, "FocusOverlayManager onTouch in metering indicator");
                    dragState = DRAG_STATE_DRAGGING_METERING_UI;
                } else if (isInFocusIndicator(x, y) && isSeparating()) {
                    Log.d(TAG, "FocusOverlayManager onTouch in focus indicator");
                    dragState = DRAG_STATE_DRAGGING_FOCUS_UI;
                } else {
                    Log.d(TAG, "need click focus switcher first...");
                    dragState = DRAG_STATE_NONE;
                }
                break;
            case MotionEvent.ACTION_UP:
                // only set Focus Area on finger up.
                if (dragState == DRAG_STATE_DRAGGING_FOCUS_UI) {
                    onFocusAreaChanged(x, y);
                } else if (dragState == DRAG_STATE_DRAGGING_METERING_UI) {
                }
                dragState = DRAG_STATE_NONE;
                break;
            case MotionEvent.ACTION_MOVE:
                if (dragState == DRAG_STATE_DRAGGING_METERING_UI) {
                    ((FocusUI) mUI).setSeparating(true);
                    if (ENABLE_DRAG_WHEN_LOCKED || !getAeAwbLock()) {
                        // Use margin to set the metering indicator to the touched area.
                        ((FocusUI) mUI).setMeteringPosition(x, y, false);

                        /* ZhangChao time:2015-04-01,Don't set parameter every change,  use a filter. START ++++ */
                        if (!filterMeteringPointInAccuracy(x, y)) {
                        /* ZhangChao time:2015-04-01,Don't set parameter every change,  use a filter. END ---- */
                            // set Metering Area every point changed.
                            onMeteringAreaChanged(x, y);
                            mLastMeteringX = x;
                            mLastMeteringY = y;
                        }
                    }
                } else if (dragState == DRAG_STATE_DRAGGING_FOCUS_UI) {
                    ((FocusUI) mUI).setSeparating(true);
                    // Use margin to set the focus indicator to the touched area.
                    ((FocusUI) mUI).setFocusPosition(x, y, false);
                } else  {
                    dragState = DRAG_STATE_NONE;
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                dragState = DRAG_STATE_NONE;
                break;
        }
    }

    /** filter some point too close. */
    private boolean filterMeteringPointInAccuracy(int x, int y) {
        double distances = Math.sqrt((mLastMeteringX - x) * (mLastMeteringX - x)
                + (mLastMeteringY - y) * (mLastMeteringY - y));
        return distances < METERING_ACCURACY;
    }

    protected boolean isSeparating() {
        return (mUI instanceof FocusUI) && ((FocusUI) mUI).isSeparating();
    }

    protected boolean isInFocusSwitcherIndicator(int x, int y) {
        return ((FocusUI) mUI).getFocusSwitcherBoundIfShowed().contains(x, y);
    }

    protected boolean isInFocusIndicator(int x, int y) {
        return ((FocusUI) mUI).getFocusBoundIfShowed().contains(x, y);
    }
    
    protected boolean isInMeteringIndicator(int x, int y) {
        return ((FocusUI) mUI).getMeteringBoundIfShowed().contains(x, y);
    }

    /** copy focus logic from {@link FocusOverlayManager#onSingleTapUp(int, int)} */
    private void onFocusAreaChanged(int x, int y) {
        Log.d(TAG, "onFocusAreaChanged");
        // Let users be able to cancel previous touch focus.
        if ((getFocusAreas() != null) && (mState == STATE_FOCUSING ||
                mState == STATE_SUCCESS || mState == STATE_FAIL)) {
            // copy logic from cancelAutoFocus();
            // just cancel focus, don't cancel focus UI.
            mListener.cancelAutoFocus();
            mUI.resumeFaceDetection();
            mState = STATE_IDLE;
            mFocusLocked = false;
            mHandler.removeMessages(RESET_TOUCH_FOCUS);
        }

        if (mFocusAreaSupported) {
            initializeFocusAreas(x, y);
        }

        // Stop face detection because we want to specify focus and metering area.
        mListener.stopFaceDetection();
        // Set the focus area and metering area.
        mListener.setFocusParameters();
        if (mFocusAreaSupported) {
            autoFocus();
        } else {  // Just show the indicator in all other cases.
            updateFocusUI();
        }
    }

    private void onMeteringAreaChanged(int x, int y) {
        if (ENABLE_DRAG_WHEN_LOCKED && getAeAwbLock()) {
            unlockAeAwbIfNeeded();
        }
        if (mMeteringAreaSupported) {
            initializeMeteringAreas(x, y);
        }
        if (mListener instanceof Listener) {
            ((Listener) mListener).setMeteringParameters();
        }
    }

    public void onFocusIndicatorLongPress() {
        Log.d(TAG, "onFocusIndicatorLongPress.");
    }

    public void onMeteringIndicatorLongPress() {
        if (!mLockAeAwbNeeded) {
            Log.d(TAG, "return because not support AE Lock.");
            return;
        }
        boolean lock = getAeAwbLock();
        Log.d(TAG, "onMeteringIndicatorLongPress current lock : " + lock);
        if (lock) {
            unlockAeAwbIfNeeded();
        } else {
            lockAeAwbIfNeeded();
        }
    }

    private void setMeteringUILock(boolean locked, boolean needToast) {
        if (mUI instanceof FocusUI) {
            ((FocusUI) mUI).onMeteringLock(locked);
            if (needToast) {
                int toastId = locked ? R.string.toast_auto_exposure_lock_on
                        : R.string.toast_auto_exposure_lock_off;
                Toast.makeText(mAppController.getAndroidContext(), toastId, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
