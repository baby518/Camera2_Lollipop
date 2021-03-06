package com.android.camera.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.*;

import com.android.camera.CameraActivity;
import com.android.camera.PhotoModule;
import com.android.camera.VideoModule;
import com.android.camera.app.CameraApp;
import com.android.camera.debug.Log;
import com.android.camera.util.CameraSettingsActivityHelper;
import com.android.camera2.R;
import com.android.ex.camera2.portability.CameraAgentFactory;
import com.android.ex.camera2.portability.CameraDeviceInfo;
import com.android.ex.camera2.portability.Size;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class CameraSettingsFragment_Plus extends PreferenceFragment implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String PREF_CATEGORY_RESOLUTION = "pref_category_resolution";
    public static final String PREF_CATEGORY_SWITCHER = "pref_category_switcher";
    public static final String PREF_CATEGORY_OPTION = "pref_category_option";
    private static final Log.Tag TAG = new Log.Tag("SettingsFragment");
    private static DecimalFormat sMegaPixelFormat = new DecimalFormat("##0.0");
    private String[] mCamcorderProfileNames;
    private CameraDeviceInfo mInfos;
    private String mPrefKey;
    private boolean mGetSubPrefAsRoot = true;
    private boolean mPreferencesRemoved = false;

    // Selected resolutions for the different cameras and sizes.
    private SettingsUtil.SelectedPictureSizes mOldPictureSizesBack;
    private SettingsUtil.SelectedPictureSizes mOldPictureSizesFront;
    private List<Size> mPictureSizesBack;
    private List<Size> mPictureSizesFront;
    private SettingsUtil.SelectedVideoQualities mVideoQualitiesBack;
    private SettingsUtil.SelectedVideoQualities mVideoQualitiesFront;

    /* ZhangChao time:2015-02-11,used for restore preferences. START ++++ */
    public static final String KEY_RESTORE = "pref_restore";
    public static final String MODULE_SCOPE_PREFIX = CameraActivity.MODULE_SCOPE_PREFIX;
    public static final String CAMERA_SCOPE_PREFIX = CameraActivity.CAMERA_SCOPE_PREFIX;
    public static final String[] MODULE_STRING_IDS = {
            PhotoModule.PHOTO_MODULE_STRING_ID,
            VideoModule.VIDEO_MODULE_STRING_ID
    };

    public static final String[] MODULE_STRING_IDS_UNIQUE = FILTER_DUP_IN_ARRAY(MODULE_STRING_IDS);

    /** filter duplicate items in array. */
    public static String[] FILTER_DUP_IN_ARRAY(String[] array) {
        List<String> list = new LinkedList<>();
        for (String str : array) {
            if (!list.contains(str)) list.add(str);
        }
        return list.toArray(new String[list.size()]);
    }
    /* ZhangChao time:2015-02-11,used for restore preferences. END ++++ */

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle arguments = getArguments();
        if (arguments != null) {
            mPrefKey = arguments.getString(CameraSettingsActivity.PREF_SCREEN_EXTRA);
        }
        Context context = this.getActivity().getApplicationContext();
        /* ZhangChao time:Dec 19, 2014,use my Custom. ORIG ++++ */
//        addPreferencesFromResource(R.xml.camera_preferences);
        /* ZhangChao time:Dec 19, 2014,use my Custom. START ++++ */
        addPreferencesFromResource(R.xml.camera_preferences_plus);
        /* ZhangChao time:Dec 19, 2014,use my Custom. END ++++ */

        // Allow the Helper to edit the full preference hierarchy, not the sub
        // tree we may show as root. See {@link #getPreferenceScreen()}.
        mGetSubPrefAsRoot = false;
        CameraSettingsActivityHelper.addAdditionalPreferences(this, context);
        mGetSubPrefAsRoot = true;

        mCamcorderProfileNames = getResources().getStringArray(R.array.camcorder_profile_names);
        mInfos = CameraAgentFactory
                .getAndroidCameraAgent(context, CameraAgentFactory.CameraApi.API_1)
                .getCameraDeviceInfo();
    }

    @Override
    public void onResume() {
        super.onResume();

        // Load the camera sizes.
        loadSizes();

        // Make sure to hide settings for cameras that don't exist on this
        // device.
        setVisibilities();

        // Put in the summaries for the currently set values.
        /* ZhangChao time:2015-02-11,use Category instead. ORIG ++++ */
//        final PreferenceScreen resolutionScreen =
//                (PreferenceScreen) findPreference(PREF_CATEGORY_RESOLUTION);
//        fillEntriesAndSummaries(resolutionScreen);
//        setPreferenceScreenIntent(resolutionScreen);
        /* ZhangChao time:2015-02-11,use Category instead. START ++++ */
        final PreferenceCategory resolutionCategory =
                (PreferenceCategory) findPreference(PREF_CATEGORY_RESOLUTION);
        fillEntriesAndSummaries(resolutionCategory);
        /* ZhangChao time:2015-02-11,use Category instead. END ---- */

        /* ZhangChao time:2015-02-10,add restore preferences function. START ++++ */
        Preference restorePref = findPreference(KEY_RESTORE);
        if (restorePref != null) {
            restorePref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    new AlertDialog.Builder(getActivity()).setTitle(R.string.restore_title)
                            .setMessage(R.string.restore_message)
                            .setPositiveButton(R.string.dialog_ok,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            restorePreferences(getSettingsManager());
                                            dialog.dismiss();
                                        }
                                    })
                            .setNegativeButton(R.string.dialog_cancel, null).show();
                    return true;
                }
            });
        }
        /* ZhangChao time:2015-02-10,add restore preferences function. END ---- */

        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    /**
     * Configure home-as-up for sub-screens.
     */
    private void setPreferenceScreenIntent(final PreferenceScreen preferenceScreen) {
        Intent intent = new Intent(getActivity(), CameraSettingsActivity.class);
        intent.putExtra(CameraSettingsActivity.PREF_SCREEN_EXTRA, preferenceScreen.getKey());
        preferenceScreen.setIntent(intent);
    }

    /* ZhangChao time:2015-02-11,used for restore preferences. START ++++ */
    private SettingsManager getSettingsManager() {
        Application application = getActivity().getApplication();
        if (application instanceof CameraApp) {
            return ((CameraApp) application).getSettingsManager();
        }
        return null;
    }
    private void restorePreferences(SettingsManager settingsManager) {
        final PreferenceCategory resolutionCategory =
                (PreferenceCategory) findPreference(PREF_CATEGORY_RESOLUTION);
        final PreferenceCategory switcherCategory =
                (PreferenceCategory) findPreference(PREF_CATEGORY_SWITCHER);
        final PreferenceCategory optionCategory =
                (PreferenceCategory) findPreference(PREF_CATEGORY_OPTION);

        restorePreferencesInSettingActivity(settingsManager, switcherCategory);
        restorePreferencesInSettingActivity(settingsManager, resolutionCategory);
        restorePreferencesInSettingActivity(settingsManager, optionCategory);

        restorePreferencesInGlobal(settingsManager, Keys_Plus.KEYS_FOR_RESTORE);

        if (mInfos != null) {
            // Back camera.
            int backCameraId = SettingsUtil.getCameraId(mInfos, SettingsUtil.CAMERA_FACING_BACK);
            // Front camera.
            int frontCameraId = SettingsUtil.getCameraId(mInfos, SettingsUtil.CAMERA_FACING_FRONT);

            restorePreferencesInCameraScope(settingsManager, backCameraId, Keys_Plus.KEYS_FOR_RESTORE);
            restorePreferencesInCameraScope(settingsManager, frontCameraId, Keys_Plus.KEYS_FOR_RESTORE);
        }

        for (String moduleId : MODULE_STRING_IDS_UNIQUE) {
            restorePreferencesInModuleScope(settingsManager, moduleId, Keys_Plus.KEYS_FOR_RESTORE);
        }
    }

    private void restorePreferencesInSettingActivity(SettingsManager settingsManager, PreferenceGroup group) {
        if (group == null) return;
        for (int i = 0; i < group.getPreferenceCount(); ++i) {
            Preference pref = group.getPreference(i);
            String key = pref.getKey();
            if (pref instanceof PreferenceGroup) {
                restorePreferencesInSettingActivity(settingsManager, (PreferenceGroup) pref);
            } else if (pref instanceof SwitchPreference) {
                ((SwitchPreference) pref).setChecked(settingsManager.getBooleanDefault(key));
            } else if (pref instanceof ListPreference) {
                String defaultValue = settingsManager.getStringDefault(key);
                if (defaultValue == null) {
                    // defaultValue is null, such as picture size, so set first index here;
                    defaultValue = ((ListPreference) pref).getEntryValues()[0].toString();
                }
                ((ListPreference) pref).setValue(defaultValue);
            }
        }
    }

    private void restorePreferencesInScope(SettingsManager settingsManager, String scope, String[] keys) {
        for (String key : keys) {
            String currentValue = settingsManager.getString(scope, key, null);
            if (currentValue != null) {
                settingsManager.setToDefault(scope, key);
            }
        }
    }

    private void restorePreferencesInGlobal(SettingsManager settingsManager, String[] keys) {
        restorePreferencesInScope(settingsManager, SettingsManager.SCOPE_GLOBAL, keys);
    }

    private void restorePreferencesInCameraScope(SettingsManager settingsManager, int cameraId, String[] keys) {
        String cameraScope = CAMERA_SCOPE_PREFIX + Integer.toString(cameraId);
        restorePreferencesInScope(settingsManager, cameraScope, keys);
    }

    private void restorePreferencesInModuleScope(SettingsManager settingsManager, String moduleId, String[] keys) {
        String moduleScope = MODULE_SCOPE_PREFIX + moduleId;
        restorePreferencesInScope(settingsManager, moduleScope, keys);
    }
    /* ZhangChao time:2015-02-11,used for restore preferences. END ++++ */

    /**
     * This override allows the CameraSettingsFragment to be reused for
     * different nested PreferenceScreens within the single camera
     * preferences XML resource. If the fragment is constructed with a
     * desired preference key (delivered via an extra in the creation
     * intent), it is used to look up the nested PreferenceScreen and
     * returned here.
     */
    @Override
    public PreferenceScreen getPreferenceScreen() {
        PreferenceScreen root = super.getPreferenceScreen();
        if (!mGetSubPrefAsRoot || mPrefKey == null || root == null) {
            return root;
        } else {
            PreferenceScreen match = findByKey(root, mPrefKey);
            if (match != null) {
                return match;
            } else {
                throw new RuntimeException("key " + mPrefKey + " not found");
            }
        }
    }

    private PreferenceScreen findByKey(PreferenceScreen parent, String key) {
        if (key.equals(parent.getKey())) {
            return parent;
        } else {
            for (int i = 0; i < parent.getPreferenceCount(); i++) {
                Preference child = parent.getPreference(i);
                if (child instanceof PreferenceScreen) {
                    PreferenceScreen match = findByKey((PreferenceScreen) child, key);
                    if (match != null) {
                        return match;
                    }
                }
            }
            return null;
        }
    }

    /**
     * Depending on camera availability on the device, this removes settings
     * for cameras the device doesn't have.
     */
    private void setVisibilities() {
        PreferenceGroup resolutions =
                (PreferenceGroup) findPreference(PREF_CATEGORY_RESOLUTION);
        if ((mPictureSizesBack == null) && !mPreferencesRemoved) {
            recursiveDelete(resolutions,
                    findPreference(Keys.KEY_PICTURE_SIZE_BACK));
            recursiveDelete(resolutions,
                    findPreference(Keys.KEY_VIDEO_QUALITY_BACK));
        }
        if ((mPictureSizesFront == null) && !mPreferencesRemoved) {
            recursiveDelete(resolutions,
                    findPreference(Keys.KEY_PICTURE_SIZE_FRONT));
            recursiveDelete(resolutions,
                    findPreference(Keys.KEY_VIDEO_QUALITY_FRONT));
        }
        mPreferencesRemoved = true;
    }

    /**
     * Recursively go through settings and fill entries and summaries of our
     * preferences.
     */
    private void fillEntriesAndSummaries(PreferenceGroup group) {
        for (int i = 0; i < group.getPreferenceCount(); ++i) {
            Preference pref = group.getPreference(i);
            if (pref instanceof PreferenceGroup) {
                fillEntriesAndSummaries((PreferenceGroup) pref);
            }
            setSummary(pref);
            setEntries(pref);
        }
    }

    /**
     * Recursively traverses the tree from the given group as the route and
     * tries to delete the preference. Traversal stops once the preference
     * was found and removed.
     */
    private boolean recursiveDelete(PreferenceGroup group, Preference preference) {
        if (group == null) {
            Log.d(TAG, "attempting to delete from null preference group");
            return false;
        }
        if (preference == null) {
            Log.d(TAG, "attempting to delete null preference");
            return false;
        }
        if (group.removePreference(preference)) {
            // Removal was successful.
            return true;
        }

        for (int i = 0; i < group.getPreferenceCount(); ++i) {
            Preference pref = group.getPreference(i);
            if (pref instanceof PreferenceGroup) {
                if (recursiveDelete((PreferenceGroup) pref, preference)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        setSummary(findPreference(key));
    }

    /**
     * Set the entries for the given preference. The given preference needs
     * to be a {@link ListPreference}
     */
    private void setEntries(Preference preference) {
        if (!(preference instanceof ListPreference)) {
            return;
        }

        ListPreference listPreference = (ListPreference) preference;
        if (listPreference.getKey().equals(Keys.KEY_PICTURE_SIZE_BACK)) {
            setEntriesForSelection(mPictureSizesBack, listPreference);
        } else if (listPreference.getKey().equals(Keys.KEY_PICTURE_SIZE_FRONT)) {
            setEntriesForSelection(mPictureSizesFront, listPreference);
        } else if (listPreference.getKey().equals(Keys.KEY_VIDEO_QUALITY_BACK)) {
            setEntriesForSelection(mVideoQualitiesBack, listPreference);
        } else if (listPreference.getKey().equals(Keys.KEY_VIDEO_QUALITY_FRONT)) {
            setEntriesForSelection(mVideoQualitiesFront, listPreference);
        }
    }

    /**
     * Set the summary for the given preference. The given preference needs
     * to be a {@link ListPreference}.
     */
    private void setSummary(Preference preference) {
        if (!(preference instanceof ListPreference)) {
            return;
        }

        ListPreference listPreference = (ListPreference) preference;
        if (listPreference.getKey().equals(Keys.KEY_PICTURE_SIZE_BACK)) {
            setSummaryForSelection(mOldPictureSizesBack, mPictureSizesBack, listPreference);
        } else if (listPreference.getKey().equals(Keys.KEY_PICTURE_SIZE_FRONT)) {
            setSummaryForSelection(mOldPictureSizesFront, mPictureSizesFront, listPreference);
        } else if (listPreference.getKey().equals(Keys.KEY_VIDEO_QUALITY_BACK)) {
            setSummaryForSelection(mVideoQualitiesBack, listPreference);
        } else if (listPreference.getKey().equals(Keys.KEY_VIDEO_QUALITY_FRONT)) {
            setSummaryForSelection(mVideoQualitiesFront, listPreference);
        } else {
            listPreference.setSummary(listPreference.getEntry());
        }
    }

    /**
     * Sets the entries for the given list preference.
     *
     * @param selectedSizes The possible S,M,L entries the user can
     *            choose from.
     * @param preference The preference to set the entries for.
     */
    private void setEntriesForSelection(List<Size> selectedSizes,
                                        ListPreference preference) {
        if (selectedSizes == null) {
            return;
        }

        String[] entries = new String[selectedSizes.size()];
        String[] entryValues = new String[selectedSizes.size()];
        for (int i = 0; i < selectedSizes.size(); i++) {
            Size size = selectedSizes.get(i);
            entries[i] = getSizeSummaryString(size);
            entryValues[i] = SettingsUtil.sizeToSetting(size);
        }
        preference.setEntries(entries);
        preference.setEntryValues(entryValues);
    }

    /**
     * Sets the entries for the given list preference.
     *
     * @param selectedQualities The possible S,M,L entries the user can
     *            choose from.
     * @param preference The preference to set the entries for.
     */
    private void setEntriesForSelection(SettingsUtil.SelectedVideoQualities selectedQualities,
                                        ListPreference preference) {
        if (selectedQualities == null) {
            return;
        }

        // Avoid adding double entries at the bottom of the list which
        // indicates that not at least 3 qualities are supported.
        ArrayList<String> entries = new ArrayList<String>();
        entries.add(mCamcorderProfileNames[selectedQualities.large]);
        if (selectedQualities.medium != selectedQualities.large) {
            entries.add(mCamcorderProfileNames[selectedQualities.medium]);
        }
        if (selectedQualities.small != selectedQualities.medium) {
            entries.add(mCamcorderProfileNames[selectedQualities.small]);
        }
        preference.setEntries(entries.toArray(new String[0]));
    }

    /**
     * Sets the summary for the given list preference.
     *
     * @param oldPictureSizes The old selected picture sizes for small medium and large
     * @param displayableSizes The human readable preferred sizes
     * @param preference The preference for which to set the summary.
     */
    private void setSummaryForSelection(SettingsUtil.SelectedPictureSizes oldPictureSizes,
                                        List<Size> displayableSizes, ListPreference preference) {
        if (oldPictureSizes == null) {
            return;
        }

        String setting = preference.getValue();
        Size selectedSize = oldPictureSizes.getFromSetting(setting, displayableSizes);

        preference.setSummary(getSizeSummaryString(selectedSize));
    }

    /**
     * Sets the summary for the given list preference.
     *
     * @param selectedQualities The selected video qualities.
     * @param preference The preference for which to set the summary.
     */
    private void setSummaryForSelection(SettingsUtil.SelectedVideoQualities selectedQualities,
                                        ListPreference preference) {
        if (selectedQualities == null) {
            return;
        }

        int selectedQuality = selectedQualities.getFromSetting(preference.getValue());
        preference.setSummary(mCamcorderProfileNames[selectedQuality]);
    }

    /**
     * This method gets the selected picture sizes for S,M,L and populates
     * {@link #mPictureSizesBack}, {@link #mPictureSizesFront},
     * {@link #mVideoQualitiesBack} and {@link #mVideoQualitiesFront}
     * accordingly.
     */
    private void loadSizes() {
        if (mInfos == null) {
            Log.w(TAG, "null deviceInfo, cannot display resolution sizes");
            return;
        }
        // Back camera.
        int backCameraId = SettingsUtil.getCameraId(mInfos, SettingsUtil.CAMERA_FACING_BACK);
        if (backCameraId >= 0) {
            List<Size> sizes = CameraPictureSizesCacher.getSizesForCamera(backCameraId,
                    this.getActivity().getApplicationContext());
            if (sizes != null) {
                mOldPictureSizesBack = SettingsUtil.getSelectedCameraPictureSizes(sizes,
                        backCameraId);
                mPictureSizesBack = ResolutionUtil
                        .getDisplayableSizesFromSupported(sizes, true);
            }
            mVideoQualitiesBack = SettingsUtil.getSelectedVideoQualities(backCameraId);
        } else {
            mPictureSizesBack = null;
            mVideoQualitiesBack = null;
        }

        // Front camera.
        int frontCameraId = SettingsUtil.getCameraId(mInfos, SettingsUtil.CAMERA_FACING_FRONT);
        if (frontCameraId >= 0) {
            List<Size> sizes = CameraPictureSizesCacher.getSizesForCamera(frontCameraId,
                    this.getActivity().getApplicationContext());
            if (sizes != null) {
                mOldPictureSizesFront= SettingsUtil.getSelectedCameraPictureSizes(sizes,
                        frontCameraId);
                mPictureSizesFront =
                        ResolutionUtil.getDisplayableSizesFromSupported(sizes, false);
            }
            mVideoQualitiesFront = SettingsUtil.getSelectedVideoQualities(frontCameraId);
        } else {
            mPictureSizesFront = null;
            mVideoQualitiesFront = null;
        }
    }

    /**
     * @param size The photo resolution.
     * @return A human readable and translated string for labeling the
     *         picture size in megapixels.
     */
    private String getSizeSummaryString(Size size) {
        Size approximateSize = ResolutionUtil.getApproximateSize(size);
        String megaPixels = sMegaPixelFormat.format((size.width() * size.height()) / 1e6);
        int numerator = ResolutionUtil.aspectRatioNumerator(approximateSize);
        int denominator = ResolutionUtil.aspectRatioDenominator(approximateSize);
        String result = getResources().getString(
                R.string.setting_summary_aspect_ratio_and_megapixels, numerator, denominator,
                megaPixels);
        return result;
    }
}