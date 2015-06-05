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

import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import com.android.camera.debug.Log;

public class PhotoUI_Plus extends PhotoUI {
    private static final Log.Tag TAG = new Log.Tag("PhotoUI_Plus");
    
    public PhotoUI_Plus(CameraActivity activity, PhotoController controller, View parent) {
        super(activity, controller, parent);
    }

    private final View.OnTouchListener mPreviewOnTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (!(mController instanceof PhotoController_Plus)) {
                Log.w(TAG, "return because mController is not PhotoController_Plus");
                return false;
            }
            ((PhotoController_Plus) mController).onMyTouchEvent(v, event);
            return true;
        }
    };

    @Override
    public View.OnTouchListener getTouchListener() {
        return mPreviewOnTouchListener;
    }

    private final GestureDetector.OnGestureListener mPreviewGestureListener
            = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onSingleTapUp(MotionEvent ev) {
            mController.onSingleTapUp(null, (int) ev.getX(), (int) ev.getY());
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            if (!(mController instanceof PhotoController_Plus)) {
                Log.w(TAG, "return because mController is not PhotoController_Plus");
                return;
            }
            ((PhotoController_Plus) mController).onLongPress(e);
        }
    };

    @Override
    public GestureDetector.OnGestureListener getGestureListener() {
        return mPreviewGestureListener;
    }

    @Override
    protected void showCapturedImageForReview(byte[] jpegData, int orientation, boolean mirror) {
        /* ZhangChao time:2015-03-26,don't mirror, show review as same as jpegData. START ++++ */
        super.showCapturedImageForReview(jpegData, orientation, false);
        /* ZhangChao time:2015-03-26,don't mirror, show review as same as jpegData. END ---- */
    }
}
