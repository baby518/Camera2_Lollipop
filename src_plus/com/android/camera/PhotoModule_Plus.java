package com.android.camera;

import android.view.MotionEvent;
import android.view.View;
import com.android.camera.app.AppController;
import com.android.camera.app.MotionManager;
import com.android.camera.debug.Log;
import com.android.camera.settings.Keys_Plus;
import com.android.camera.settings.SettingsManager;
import com.android.camera2.R;
import com.android.ex.camera2.portability.CameraCapabilities;

import java.util.ArrayList;

/**
 * Created by ZhangChao on 15-6-3.
 */
public class PhotoModule_Plus extends PhotoModule implements PhotoController_Plus,
        FocusOverlayManager_Plus.Listener {
    private static final Log.Tag TAG = new Log.Tag("PhotoModule_Plus");
    /**
     * Constructs a new photo module.
     * @param app
     */
    public PhotoModule_Plus(AppController app) {
        super(app);
    }

    @Override
    protected void initializeFocusManager() {
        // Create FocusManager object. startPreview needs it.
        // if mFocusManager not null, reuse it
        // otherwise create a new instance
        if (mFocusManager != null) {
            mFocusManager.removeMessages();
        } else {
            mMirror = isCameraFrontFacing();
            String[] defaultFocusModesStrings = mActivity.getResources().getStringArray(
                    R.array.pref_camera_focusmode_default_array);
            ArrayList<CameraCapabilities.FocusMode> defaultFocusModes =
                    new ArrayList<CameraCapabilities.FocusMode>();
            CameraCapabilities.Stringifier stringifier = mCameraCapabilities.getStringifier();
            for (String modeString : defaultFocusModesStrings) {
                CameraCapabilities.FocusMode mode = stringifier.focusModeFromString(modeString);
                if (mode != null) {
                    defaultFocusModes.add(mode);
                }
            }
            mFocusManager =
                    new FocusOverlayManager_Plus(mAppController, defaultFocusModes,
                            mCameraCapabilities, this, mMirror, mActivity.getMainLooper(),
                            mUI.getFocusUI());
            MotionManager motionManager = getServices().getMotionManager();
            if (motionManager != null) {
                motionManager.addListener(mFocusManager);
            }
        }
        mAppController.addPreviewAreaSizeChangedListener(mFocusManager);
    }

    @Override
    public void setMeteringParameters() {
        setMeteringAreasIfSupported();

        if (mCameraDevice != null) {
            mCameraDevice.applySettings(mCameraSettings);
        }
    }

    @Override
    protected void onPreviewStarted() {
        super.onPreviewStarted();
        SettingsManager settingsManager = mActivity.getSettingsManager();
        checkSeparateMeteringPreference(settingsManager);
    }

    private void checkSeparateMeteringPreference(SettingsManager settingsManager) {
        boolean value = settingsManager.getBoolean(SettingsManager.SCOPE_GLOBAL,
                Keys_Plus.KEY_CAMERA_SEPARATE_METERING);
        Log.d(TAG, "checkSeparateMeteringPreference : " + value);
        if (mFocusManager != null && mFocusManager instanceof FocusOverlayManager_Plus) {
            boolean currentValue = ((FocusOverlayManager_Plus) mFocusManager).inSeparateMode();
            if (currentValue != value) mFocusManager.resetTouchFocus();
            ((FocusOverlayManager_Plus) mFocusManager).setSeparateMode(value);
        }
    }

    public void setSwipeEnabled(boolean enabled) {
        mAppController.getCameraAppUI().setSwipeEnabled(enabled);
    }

    /** implements PhotoController_Plus START +++ ***/
    @Override
    public void onMyTouchEvent(View view, MotionEvent event) {
        if (!(mFocusManager instanceof FocusOverlayManager_Plus)) {
            Log.w(TAG, "return because mFocusManager is not FocusOverlayManager_Plus");
            return;
        }
        if (!mFocusManager.mMeteringAreaSupported || !mFocusManager.mFocusAreaSupported) {
            return;
        }
        final int action = event.getActionMasked();
        final int x = (int) event.getX();
        final int y = (int) event.getY();
        if (action == MotionEvent.ACTION_DOWN && (isEventInIndicator(x, y))) {
            Log.d(TAG, "setSwipeEnabled false");
            setSwipeEnabled(false);
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            Log.d(TAG, "setSwipeEnabled true");
            setSwipeEnabled(true);
        }
        ((FocusOverlayManager_Plus) mFocusManager).onMyTouchEvent(event);
    }

    @Override
    public void onLongPress(MotionEvent event) {
        if (!(mFocusManager instanceof FocusOverlayManager_Plus)) {
            Log.w(TAG, "return because mFocusManager is not FocusOverlayManager_Plus");
            return;
        }
        final int x = (int) event.getX();
        final int y = (int) event.getY();
        if (isEventInFocusIndicator(x, y)) {
            ((FocusOverlayManager_Plus) mFocusManager).onFocusIndicatorLongPress();
        } else if (isEventInMeteringIndicator(x, y)) {
            ((FocusOverlayManager_Plus) mFocusManager).onMeteringIndicatorLongPress();
        } else {
            // do nothing.
        }
    }

    private boolean isEventInIndicator(int x, int y) {
        return ((FocusOverlayManager_Plus) mFocusManager).isInFocusIndicator(x, y)
                || ((FocusOverlayManager_Plus) mFocusManager).isInMeteringIndicator(x, y);
    }

    private boolean isEventInFocusIndicator(int x, int y) {
        return ((FocusOverlayManager_Plus) mFocusManager).isInFocusIndicator(x, y);
    }

    private boolean isEventInMeteringIndicator(int x, int y) {
        return ((FocusOverlayManager_Plus) mFocusManager).isInMeteringIndicator(x, y);
    }
    /** implements PhotoController_Plus END --- ***/
}
