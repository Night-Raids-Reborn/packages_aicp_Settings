/*
 * Copyright (C) 2020 Yet Another AOSP Project
 * Copyright (C) 2022 FlamingoOS Project
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

package com.android.settings.sound;

import android.content.Context;
import android.media.AudioAttributes;
import android.os.UserHandle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settingslib.core.AbstractPreferenceController;
import com.aicp.gear.preference.SystemSettingSeekBarPreference;

/**
 * This class allows choosing a vibration pattern while ringing
 */
public class CustomVibrationPreferenceController extends AbstractPreferenceController
        implements Preference.OnPreferenceChangeListener {

    private static final String TAG = CustomVibrationPreferenceController.class.getSimpleName();

    private static final String KEY = "custom_vibration_pattern";
    private static final String KEY_CUSTOM_VIB1 = "custom_vibration_pattern1";
    private static final String KEY_CUSTOM_VIB2 = "custom_vibration_pattern2";
    private static final String KEY_CUSTOM_VIB3 = "custom_vibration_pattern3";

    private static final AudioAttributes VIBRATION_ATTRIBUTES = new AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
            .build();

    private static final int[] SEVEN_ELEMENTS_VIBRATION_AMPLITUDE = {
        0, // No delay before starting
        255, // Vibrate full amplitude
        0, // No amplitude while waiting
        255,
        0,
        255,
        0,
    };

    private final Vibrator mVibrator;

    private SystemSettingSeekBarPreference mCustomVib1;
    private SystemSettingSeekBarPreference mCustomVib2;
    private SystemSettingSeekBarPreference mCustomVib3;

    public CustomVibrationPreferenceController(Context context) {
        super(context);
        mVibrator = context.getSystemService(Vibrator.class);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mCustomVib1 = (SystemSettingSeekBarPreference) screen.findPreference(KEY_CUSTOM_VIB1);
        mCustomVib2 = (SystemSettingSeekBarPreference) screen.findPreference(KEY_CUSTOM_VIB2);
        mCustomVib3 = (SystemSettingSeekBarPreference) screen.findPreference(KEY_CUSTOM_VIB3);
        updateCustomVibPreferences();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mCustomVib1) {
            updateCustomVib(0, (Integer) newValue);
            return true;
        } else if (preference == mCustomVib2) {
            updateCustomVib(1, (Integer) newValue);
            return true;
        } else if (preference == mCustomVib3) {
            updateCustomVib(2, (Integer) newValue);
            return true;
        }
        return false;
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public boolean isAvailable() {
        return Utils.isVoiceCapable(mContext);
    }

    private void updateCustomVibPreferences() {
        final String value = Settings.System.getStringForUser(
            mContext.getContentResolver(),
            Settings.System.CUSTOM_RINGTONE_VIBRATION_PATTERN,
            UserHandle.USER_CURRENT
        );
        if (value != null && !value.isEmpty()) {
            try {
                final String[] customPattern = value.split(",", 3);
                mCustomVib1.setValue(Integer.parseInt(customPattern[0]));
                mCustomVib2.setValue(Integer.parseInt(customPattern[1]));
                mCustomVib3.setValue(Integer.parseInt(customPattern[2]));
            } catch (NumberFormatException e) {
                Log.e(TAG, "Failed to parse custom vibration pattern, falling back to default", e);
                fallbackToDefault();
            }
        } else { // set default
            fallbackToDefault();
        }
        mCustomVib1.setOnPreferenceChangeListener(this);
        mCustomVib2.setOnPreferenceChangeListener(this);
        mCustomVib3.setOnPreferenceChangeListener(this);
    }

    private void fallbackToDefault() {
        mCustomVib1.setValue(0);
        mCustomVib2.setValue(800);
        mCustomVib3.setValue(800);
        Settings.System.putStringForUser(
            mContext.getContentResolver(),
            Settings.System.CUSTOM_RINGTONE_VIBRATION_PATTERN,
            "0,800,800",
            UserHandle.USER_CURRENT
        );
    }

    private void updateCustomVib(int index, int value) {
        String pattern = Settings.System.getStringForUser(
            mContext.getContentResolver(),
            Settings.System.CUSTOM_RINGTONE_VIBRATION_PATTERN,
            UserHandle.USER_CURRENT
        );
        final String[] customPattern;
        if (pattern == null || pattern.isEmpty()) {
            customPattern = new String[] { "0", "800", "800" };
        } else {
            customPattern = pattern.split(",", 3);
        }
        customPattern[index] = String.valueOf(value);
        Settings.System.putStringForUser(
            mContext.getContentResolver(),
            Settings.System.CUSTOM_RINGTONE_VIBRATION_PATTERN,
            String.join(",", customPattern[0], customPattern[1], customPattern[2]),
            UserHandle.USER_CURRENT
        );
        previewPattern(customPattern);
    }

    private void previewPattern(String[] pattern) {
        final long[] customVibPattern = {
            0, // No delay before starting
            Long.parseLong(pattern[0]), // How long to vibrate
            400, // Delay
            Long.parseLong(pattern[1]), // How long to vibrate
            400, // Delay
            Long.parseLong(pattern[2]), // How long to vibrate
            400, // How long to wait before vibrating again
        };
        final VibrationEffect effect = VibrationEffect.createWaveform(customVibPattern,
                SEVEN_ELEMENTS_VIBRATION_AMPLITUDE, -1);
        if (mVibrator.hasVibrator()) {
            mVibrator.vibrate(effect, VIBRATION_ATTRIBUTES);
        }
    }
}
