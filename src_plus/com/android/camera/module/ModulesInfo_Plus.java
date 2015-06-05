package com.android.camera.module;

import android.content.Context;
import com.android.camera.CaptureModule;
import com.android.camera.PhotoModule_Plus;
import com.android.camera.app.AppController;
import com.android.camera.app.ModuleManager;
import com.android.camera2.R;

/**
 * Created by ZhangChao on 15-6-3.
 */
public class ModulesInfo_Plus extends ModulesInfo {

    public static void setupModules(Context context, ModuleManager moduleManager) {
        int photoModuleId = context.getResources().getInteger(R.integer.camera_mode_photo);
        registerPhotoModule(moduleManager, photoModuleId);
        moduleManager.setDefaultModuleIndex(photoModuleId);
        registerVideoModule(moduleManager, context.getResources()
                .getInteger(R.integer.camera_mode_video));
    }

    private static void registerPhotoModule(ModuleManager moduleManager, final int moduleId) {
        moduleManager.registerModule(new ModuleManager.ModuleAgent() {
            @Override
            public int getModuleId() {
                return moduleId;
            }

            @Override
            public boolean requestAppForCamera() {
                // The PhotoModule requests the old app camere, while the new
                // capture module is using OneCamera. At some point we'll
                // refactor all modules to use OneCamera, then the new module
                // doesn't have to manage it itself.
                return !ENABLE_CAPTURE_MODULE;
            }

            @Override
            public ModuleController createModule(AppController app) {
                return ENABLE_CAPTURE_MODULE ? new CaptureModule(app) : new PhotoModule_Plus(app);
            }
        });
    }
}
