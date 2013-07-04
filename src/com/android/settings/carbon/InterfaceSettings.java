/*
 * Copyright (C) 2012 The Carbon Project
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

package com.android.settings.carbon;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.provider.MediaStore;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.text.Spannable;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.IWindowManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.util.Helpers;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.notificationlight.ColorPickerView;
import com.android.settings.widgets.AlphaSeekBar;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InterfaceSettings extends SettingsPreferenceFragment
			implements Preference.OnPreferenceChangeListener {

    private static final int REQUEST_CODE_BG_WALLPAPER = 1024;

    private static final int LOCKSCREEN_BACKGROUND_COLOR_FILL = 0;
    private static final int LOCKSCREEN_BACKGROUND_CUSTOM_IMAGE = 1;
    private static final int LOCKSCREEN_BACKGROUND_DEFAULT_WALLPAPER = 2;

    public static final String TAG = "InterfaceSettings";
    private static final String KEY_LOCK_CLOCK = "lock_clock";
    private static final String PREF_CUSTOM_CARRIER_LABEL = "custom_carrier_label";
    private static final String KEY_RECENTS_RAM_BAR = "recents_ram_bar";
    private static final String KEY_RECENTS_RAM_CIRCLE = "recents_ram_circle";
    private static final String KEY_DUAL_PANE = "dual_pane";
    private static final String PREF_WAKEUP_WHEN_PLUGGED_UNPLUGGED = "wakeup_when_plugged_unplugged";
    private static final String PREF_NOTIFICATION_SHOW_WIFI_SSID = "notification_show_wifi_ssid";
    private static final String PREF_POWER_CRT_SCREEN_ON = "system_power_crt_screen_on";
    private static final String PREF_POWER_CRT_SCREEN_OFF = "system_power_crt_screen_off";
    private static final String PREF_USER_MODE_UI = "user_mode_ui";
    private static final String PREF_HIDE_EXTRAS = "hide_extras";
    private static final String KEY_LOW_BATTERY_WARNING_POLICY = "pref_low_battery_warning_policy";
    private static final String PREF_USE_ALT_RESOLVER = "use_alt_resolver";
    private static final String KEY_POWER_BUTTON_TORCH = "power_button_torch";
    private static final String PREF_SHOW_OVERFLOW = "show_overflow";
    private static final String KEY_BACKGROUND_PREF = "lockscreen_background";
    private static final String KEY_HARDWARE_KEYS = "hardware_keys";
    private static final String KEY_CLEAR_RECENTS_POSITION = "clear_recents_position";

    Preference mCustomLabel;
    Preference mRamBar;
    Preference mLcdDensity;
    CheckBoxPreference mDualPane;
    CheckBoxPreference mWakeUpWhenPluggedOrUnplugged;
    CheckBoxPreference mShowWifiName;
    CheckBoxPreference mCrtOff;
    CheckBoxPreference mCrtOn;
    CheckBoxPreference mHideExtras;
    CheckBoxPreference mRamCircle;
    ListPreference mUserModeUI;
    Context mContext;
    ListPreference mLowBatteryWarning;
    CheckBoxPreference mUseAltResolver;
    CheckBoxPreference mPowerButtonTorch;
    CheckBoxPreference mShowActionOverflow;
    ListPreference mCustomBackground;
    ListPreference mClearPosition;

    private File mWallpaperImage;
    private File mWallpaperTemporary;

    String mCustomLabelText = null;

    int newDensityValue;
    DensityChanger densityFragment;
    Configuration mCurConfig = new Configuration();

    private static ContentResolver mContentResolver;
    private boolean isCrtOffChecked = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getActivity();

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.interface_settings);
        PreferenceScreen prefSet = getPreferenceScreen();
        ContentResolver cr = mContext.getContentResolver();
        mContentResolver = getContentResolver();

       // respect device default configuration
        // true fades while false animates
        boolean electronBeamFadesConfig = mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_animateScreenLights);

        // use this to enable/disable crt on feature
        // crt only works if crt off is enabled
        // total system failure if only crt on is enabled
        isCrtOffChecked = Settings.System.getInt(cr,
                Settings.System.SYSTEM_POWER_ENABLE_CRT_OFF,
                electronBeamFadesConfig ? 0 : 1) == 1;

        mCrtOff = (CheckBoxPreference) findPreference(PREF_POWER_CRT_SCREEN_OFF);
        mCrtOff.setChecked(isCrtOffChecked);
        mCrtOff.setOnPreferenceChangeListener(this);

        mCrtOn = (CheckBoxPreference) findPreference(PREF_POWER_CRT_SCREEN_ON);
        mCrtOn.setChecked(Settings.System.getInt(cr,
                Settings.System.SYSTEM_POWER_ENABLE_CRT_ON, 0) == 1);
        mCrtOn.setEnabled(isCrtOffChecked);
        mCrtOn.setOnPreferenceChangeListener(this);

        mLcdDensity = findPreference("lcd_density_setup");
        String currentProperty = SystemProperties.get("ro.sf.lcd_density");
        try {
            newDensityValue = Integer.parseInt(currentProperty);
        } catch (Exception e) {
            getPreferenceScreen().removePreference(mLcdDensity);
        }

        mLcdDensity.setSummary(getResources().getString(R.string.current_lcd_density) + currentProperty);

        mCustomLabel = findPreference(PREF_CUSTOM_CARRIER_LABEL);
        updateCustomLabelTextSummary();

        mRamBar = findPreference(KEY_RECENTS_RAM_BAR);
        updateRamBar();

        mRamCircle = (CheckBoxPreference) findPreference(KEY_RECENTS_RAM_CIRCLE);
        mRamCircle.setChecked(Settings.System.getBoolean(mContext.getContentResolver(),
                Settings.System.RECENTS_RAM_CIRCLE, false));

        mClearPosition = (ListPreference) findPreference(KEY_CLEAR_RECENTS_POSITION);
        int ClearSide = Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.CLEAR_RECENTS_POSITION, 0);
        mClearPosition.setValue(String.valueOf(ClearSide));
        mClearPosition.setSummary(mClearPosition.getEntry());
        mClearPosition.setOnPreferenceChangeListener(this);

        mHideExtras = (CheckBoxPreference) findPreference(PREF_HIDE_EXTRAS);
        mHideExtras.setChecked(Settings.System.getBoolean(cr,
                        Settings.System.HIDE_EXTRAS_SYSTEM_BAR, false));

        mUserModeUI = (ListPreference) findPreference(PREF_USER_MODE_UI);
        int uiMode = Settings.System.getInt(cr,
                Settings.System.CURRENT_UI_MODE, 0);
        mUserModeUI.setValue(Integer.toString(Settings.System.getInt(cr,
                Settings.System.USER_UI_MODE, uiMode)));
        mUserModeUI.setOnPreferenceChangeListener(this);

        mDualPane = (CheckBoxPreference) findPreference(KEY_DUAL_PANE);
        boolean preferDualPane = getResources().getBoolean(
                com.android.internal.R.bool.preferences_prefer_dual_pane);
        boolean dualPaneMode = Settings.System.getInt(cr,
                Settings.System.DUAL_PANE_PREFS, (preferDualPane ? 1 : 0)) == 1;
        mDualPane.setChecked(dualPaneMode);

        mLowBatteryWarning = (ListPreference) findPreference(KEY_LOW_BATTERY_WARNING_POLICY);
        int lowBatteryWarning = Settings.System.getInt(getActivity().getContentResolver(),
                                    Settings.System.POWER_UI_LOW_BATTERY_WARNING_POLICY, 0);
        mLowBatteryWarning.setValue(String.valueOf(lowBatteryWarning));
        mLowBatteryWarning.setSummary(mLowBatteryWarning.getEntry());
        mLowBatteryWarning.setOnPreferenceChangeListener(this);

        mUseAltResolver = (CheckBoxPreference) findPreference(PREF_USE_ALT_RESOLVER);
        mUseAltResolver.setChecked(Settings.System.getBoolean(mContext.getContentResolver(),
                Settings.System.ACTIVITY_RESOLVER_USE_ALT, false));

        mPowerButtonTorch = (CheckBoxPreference) findPreference(KEY_POWER_BUTTON_TORCH);
        mPowerButtonTorch.setChecked((Settings.System.getInt(getActivity().
                getApplicationContext().getContentResolver(),
                Settings.System.POWER_BUTTON_TORCH, 0) == 1));

        mShowActionOverflow = (CheckBoxPreference) findPreference(PREF_SHOW_OVERFLOW);
        mShowActionOverflow.setChecked((Settings.System.getInt(getActivity().
                        getApplicationContext().getContentResolver(),
                        Settings.System.UI_FORCE_OVERFLOW_BUTTON, 0) == 1));

        mCustomBackground = (ListPreference) findPreference(KEY_BACKGROUND_PREF);
        mCustomBackground.setOnPreferenceChangeListener(this);
        updateCustomBackgroundSummary();

        mWallpaperImage = new File(getActivity().getFilesDir() + "/lockwallpaper");
        mWallpaperTemporary = new File(getActivity().getCacheDir() + "/lockwallpaper.tmp");

        mWakeUpWhenPluggedOrUnplugged = (CheckBoxPreference) findPreference(PREF_WAKEUP_WHEN_PLUGGED_UNPLUGGED);
        mWakeUpWhenPluggedOrUnplugged.setChecked(Settings.System.getBoolean(cr,
                        Settings.System.WAKEUP_WHEN_PLUGGED_UNPLUGGED, true));

        // hide option if device is already set to never wake up
        if(!mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_unplugTurnsOnScreen)) {
            ((PreferenceGroup) findPreference("advanced_options")).removePreference(mWakeUpWhenPluggedOrUnplugged);
        }

        mShowWifiName = (CheckBoxPreference) findPreference(PREF_NOTIFICATION_SHOW_WIFI_SSID);
        mShowWifiName.setChecked(Settings.System.getInt(cr,
                Settings.System.NOTIFICATION_SHOW_WIFI_SSID, 0) == 1);

        // Only show the hardware keys config on a device that does not have a navbar 	
        IWindowManager windowManager = IWindowManager.Stub.asInterface(
                ServiceManager.getService(Context.WINDOW_SERVICE));

        boolean hasNavBarByDefault = mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_showNavigationBar);

        if (hasNavBarByDefault) {
            // Let's assume they don't have hardware keys
            getPreferenceScreen().removePreference(findPreference(KEY_HARDWARE_KEYS));
        }

        // Dont display the lock clock preference if its not installed
        removePreferenceIfPackageNotInstalled(findPreference(KEY_LOCK_CLOCK));

        PackageManager pm = getPackageManager();
        boolean isMobileData = pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY);

        if (!Utils.isPhone(getActivity()) || !isMobileData) {
            // Nothing for tablets, large screen devices and non Wifi devices remove options
            getPreferenceScreen().removePreference(mShowWifiName);
        }

        if (isTabletUI(mContext)) {
        } else {
            mHideExtras.setEnabled(false);
        }

        setHasOptionsMenu(true);
    }

    private void updateCustomBackgroundSummary() {
        int resId;
        String value = Settings.System.getString(getContentResolver(),
                Settings.System.LOCKSCREEN_BACKGROUND);
        if (value == null) {
            resId = R.string.lockscreen_background_default_wallpaper;
            mCustomBackground.setValueIndex(LOCKSCREEN_BACKGROUND_DEFAULT_WALLPAPER);
        } else if (value.isEmpty()) {
            resId = R.string.lockscreen_background_custom_image;
            mCustomBackground.setValueIndex(LOCKSCREEN_BACKGROUND_CUSTOM_IMAGE);
        } else {
            resId = R.string.lockscreen_background_color_fill;
            mCustomBackground.setValueIndex(LOCKSCREEN_BACKGROUND_COLOR_FILL);
        }
        mCustomBackground.setSummary(getResources().getString(resId));
    }

    private void updateCustomLabelTextSummary() {
        mCustomLabelText = Settings.System.getString(getActivity().getContentResolver(),
                Settings.System.CUSTOM_CARRIER_LABEL);
        if (mCustomLabelText == null || mCustomLabelText.length() == 0) {
            mCustomLabel.setSummary(R.string.custom_carrier_label_notset);
        } else {
            mCustomLabel.setSummary(mCustomLabelText);
        }
    }

    private void updateRamBar() {
        int ramBarMode = Settings.System.getInt(getActivity().getApplicationContext().getContentResolver(),
                Settings.System.RECENTS_RAM_BAR_MODE, 0);
        if (ramBarMode != 0)
            mRamBar.setSummary(getResources().getString(R.string.ram_bar_color_enabled));
        else
            mRamBar.setSummary(getResources().getString(R.string.ram_bar_color_disabled));
    }

    private void openTransparencyDialog() {
        getFragmentManager().beginTransaction().add(new AdvancedTransparencyDialog(), null)
                .commit();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mCustomLabel) {
            AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
            alert.setTitle(R.string.custom_carrier_label_title);
            alert.setMessage(R.string.custom_carrier_label_explain);
            final EditText input = new EditText(getActivity());
            input.setText(mCustomLabelText != null ? mCustomLabelText : "");
            alert.setView(input);

            alert.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    String value = ((Spannable) input.getText()).toString();
                    Settings.System.putString(getActivity().getContentResolver(),
                            Settings.System.CUSTOM_CARRIER_LABEL, value);
                    updateCustomLabelTextSummary();
                    Intent i = new Intent();
                    i.setAction("com.android.settings.LABEL_CHANGED");
                    getActivity().getApplicationContext().sendBroadcast(i);
                }
            });
            alert.setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                }
            });
            alert.show();
        } else if (preference == mLcdDensity) {
            ((PreferenceActivity) getActivity())
            .startPreferenceFragment(new DensityChanger(), true);
            return true;
        } else if (preference == mRamCircle) {
            Settings.System.putBoolean(getActivity().getContentResolver(),
                    Settings.System.RECENTS_RAM_CIRCLE,
                    ((CheckBoxPreference) preference).isChecked());
            return true;
        } else if (preference == mDualPane) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.DUAL_PANE_PREFS,
                    ((CheckBoxPreference) preference).isChecked() ? 1 : 0);
            return true;
        } else if (preference == mHideExtras) {
            Settings.System.putBoolean(mContext.getContentResolver(),
                    Settings.System.HIDE_EXTRAS_SYSTEM_BAR,
                    ((CheckBoxPreference) preference).isChecked());
            return true;
        } else if (preference.getKey().equals("transparency_dialog")) {
            // getFragmentManager().beginTransaction().add(new
            // TransparencyDialog(), null).commit();
            openTransparencyDialog();
            return true;
        } else if (preference == mWakeUpWhenPluggedOrUnplugged) {
            Settings.System.putBoolean(getActivity().getContentResolver(),
                    Settings.System.WAKEUP_WHEN_PLUGGED_UNPLUGGED,
                    ((CheckBoxPreference) preference).isChecked());
            return true;
        } else if (preference == mShowWifiName) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.NOTIFICATION_SHOW_WIFI_SSID,
                    mShowWifiName.isChecked() ? 1 : 0);
            return true;
        } else if (preference == mUseAltResolver) {
            Settings.System.putBoolean(getActivity().getContentResolver(),
                    Settings.System.ACTIVITY_RESOLVER_USE_ALT,
                    ((CheckBoxPreference) preference).isChecked());
            return true;
        } else if (preference == mPowerButtonTorch) {
            boolean enabled = mPowerButtonTorch.isChecked();
            Settings.System.putInt(getContentResolver(), Settings.System.POWER_BUTTON_TORCH,
                    enabled ? 1 : 0);
            return true;
        } else if (preference == mShowActionOverflow) {
            boolean enabled = mShowActionOverflow.isChecked();
            Settings.System.putInt(getContentResolver(), Settings.System.UI_FORCE_OVERFLOW_BUTTON,
                    enabled ? 1 : 0);
            // Show toast appropriately 
            if (enabled) {
                Toast.makeText(getActivity(), R.string.show_overflow_toast_enable,
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getActivity(), R.string.show_overflow_toast_disable,
                        Toast.LENGTH_LONG).show();
            }
            return true;
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateRamBar();
    }

    @Override
    public void onPause() {
        super.onPause();
        updateRamBar();
    }

     @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_BG_WALLPAPER) {
            int hintId;

            if (resultCode == Activity.RESULT_OK) {
                if (mWallpaperTemporary.exists()) {
                    mWallpaperTemporary.renameTo(mWallpaperImage);
                }
                mWallpaperImage.setReadOnly();
                hintId = R.string.lockscreen_background_result_successful;
                Settings.System.putString(getContentResolver(),
                        Settings.System.LOCKSCREEN_BACKGROUND, "");
                updateCustomBackgroundSummary();
            } else {
                if (mWallpaperTemporary.exists()) {
                    mWallpaperTemporary.delete();
                }
                hintId = R.string.lockscreen_background_result_not_successful;
            }
            Toast.makeText(getActivity(),
                    getResources().getString(hintId), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final String key = preference.getKey();
         if (mCrtOff.equals(preference)) {
            isCrtOffChecked = ((Boolean) newValue).booleanValue();
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.SYSTEM_POWER_ENABLE_CRT_OFF,
                    (isCrtOffChecked ? 1 : 0));
            // if crt off gets turned off, crt on gets turned off and disabled
            if (!isCrtOffChecked) {
                Settings.System.putInt(getActivity().getContentResolver(),
                        Settings.System.SYSTEM_POWER_ENABLE_CRT_ON, 0);
                mCrtOn.setChecked(false);
            }
            mCrtOn.setEnabled(isCrtOffChecked);
            return true;
        } else if (mCrtOn.equals(preference)) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.SYSTEM_POWER_ENABLE_CRT_ON,
                    ((Boolean) newValue).booleanValue() ? 1 : 0);
            return true;
        } else if (preference == mUserModeUI) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.USER_UI_MODE, Integer.parseInt((String) newValue));
            Helpers.restartSystemUI();
            return true;
        } else if (preference == mLowBatteryWarning) {
            int lowBatteryWarning = Integer.valueOf((String) newValue);
            int index = mLowBatteryWarning.findIndexOfValue((String) newValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.POWER_UI_LOW_BATTERY_WARNING_POLICY,
                    lowBatteryWarning);
            mLowBatteryWarning.setSummary(mLowBatteryWarning.getEntries()[index]);
            return true;
        } else if (preference == mCustomBackground) {
            int selection = mCustomBackground.findIndexOfValue(newValue.toString());
            return handleBackgroundSelection(selection);
        } else if (preference == mClearPosition) {
            int side = Integer.valueOf((String) newValue);
            int index = mClearPosition.findIndexOfValue((String) newValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.CLEAR_RECENTS_POSITION, side);
            mClearPosition.setSummary(mClearPosition.getEntries()[index]);
            return true;
        }
        return false;
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.user_interface, menu);
    }

    public void copy(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        FileOutputStream out = new FileOutputStream(dst);
        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

    public static class AdvancedTransparencyDialog extends DialogFragment {
        private static final int KEYGUARD_ALPHA = 112;

        private static final int STATUSBAR_ALPHA = 0;
        private static final int STATUSBAR_KG_ALPHA = 1;
        private static final int NAVBAR_ALPHA = 2;
        private static final int NAVBAR_KG_ALPHA = 3;
        private static final int LOCKSCREEN_ALPHA = 4;
        boolean linkTransparencies = true;

        CheckBox mLinkCheckBox, mMatchStatusbarKeyguard, mMatchNavbarKeyguard;
        ViewGroup mNavigationBarGroup;
        TextView mSbLabel;
        AlphaSeekBar mSeekBars[] = new AlphaSeekBar[5];

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setShowsDialog(true);
            setRetainInstance(true);
            linkTransparencies = getSavedLinkedState();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            View layout = View.inflate(getActivity(), R.layout.dialog_transparency, null);
            mLinkCheckBox = (CheckBox) layout.findViewById(R.id.transparency_linked);
            mLinkCheckBox.setChecked(linkTransparencies);
            mNavigationBarGroup = (ViewGroup) layout.findViewById(R.id.navbar_layout);
            mSbLabel = (TextView) layout.findViewById(R.id.statusbar_label);
            mSeekBars[STATUSBAR_ALPHA] = (AlphaSeekBar) layout.findViewById(R.id.statusbar_alpha);
            mSeekBars[STATUSBAR_KG_ALPHA] = (AlphaSeekBar) layout
                    .findViewById(R.id.statusbar_keyguard_alpha);
            mSeekBars[NAVBAR_ALPHA] = (AlphaSeekBar) layout.findViewById(R.id.navbar_alpha);
            mSeekBars[NAVBAR_KG_ALPHA] = (AlphaSeekBar) layout
                    .findViewById(R.id.navbar_keyguard_alpha);
            mMatchStatusbarKeyguard = (CheckBox) layout.findViewById(R.id.statusbar_match_keyguard);
            mMatchNavbarKeyguard = (CheckBox) layout.findViewById(R.id.navbar_match_keyguard);
            mSeekBars[LOCKSCREEN_ALPHA] = (AlphaSeekBar) layout.findViewById(R.id.lockscreen_alpha);

            try {
                // restore any saved settings
                int alphas[] = new int[2];
                ContentResolver resolver = getActivity().getContentResolver();
                int lockscreen_alpha = Settings.System.getInt(resolver,
                        Settings.System.LOCKSCREEN_ALPHA_CONFIG, KEYGUARD_ALPHA);
                mSeekBars[LOCKSCREEN_ALPHA].setCurrentAlpha(lockscreen_alpha);
                String sbConfig = Settings.System.getString(resolver,
                        Settings.System.STATUS_BAR_ALPHA_CONFIG);
                if (sbConfig != null) {
                    String split[] = sbConfig.split(";");
                    alphas[0] = Integer.parseInt(split[0]);
                    alphas[1] = Integer.parseInt(split[1]);
                    mSeekBars[STATUSBAR_ALPHA].setCurrentAlpha(alphas[0]);
                    mSeekBars[STATUSBAR_KG_ALPHA].setCurrentAlpha(alphas[1]);
                    mMatchStatusbarKeyguard.setChecked(alphas[1] == lockscreen_alpha);
                    if (linkTransparencies) {
                        mSeekBars[NAVBAR_ALPHA].setCurrentAlpha(alphas[0]);
                        mSeekBars[NAVBAR_KG_ALPHA].setCurrentAlpha(alphas[1]);
                    } else {
                        String navConfig = Settings.System.getString(resolver,
                                Settings.System.NAVIGATION_BAR_ALPHA_CONFIG);
                        if (navConfig != null) {
                            split = navConfig.split(";");
                            alphas[0] = Integer.parseInt(split[0]);
                            alphas[1] = Integer.parseInt(split[1]);
                            mSeekBars[NAVBAR_ALPHA].setCurrentAlpha(alphas[0]);
                            mSeekBars[NAVBAR_KG_ALPHA].setCurrentAlpha(alphas[1]);
                            mMatchNavbarKeyguard.setChecked(alphas[1] == lockscreen_alpha);
                        }
                    }
                }
            } catch (Exception e) {
                resetSettings();
            }

            updateToggleState();
            mMatchStatusbarKeyguard.setOnCheckedChangeListener(mUpdateStatesListener);
            mMatchNavbarKeyguard.setOnCheckedChangeListener(mUpdateStatesListener);
            mLinkCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    linkTransparencies = isChecked;
                    saveSavedLinkedState(isChecked);
                    updateToggleState();
                }
            });

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setView(layout);
            builder.setTitle(getString(R.string.transparency_dialog_title));
            builder.setNegativeButton(R.string.cancel, null);
            builder.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Settings.System.putInt(mContentResolver,
                            Settings.System.LOCKSCREEN_ALPHA_CONFIG,
                            mSeekBars[LOCKSCREEN_ALPHA].getCurrentAlpha());
                    // update keyguard alpha
                    if (!mSeekBars[STATUSBAR_KG_ALPHA].isEnabled()) {
                        mSeekBars[STATUSBAR_KG_ALPHA].setCurrentAlpha(
                                mSeekBars[LOCKSCREEN_ALPHA].getCurrentAlpha());
                    }
                    if (!mSeekBars[NAVBAR_KG_ALPHA].isEnabled()) {
                        mSeekBars[NAVBAR_KG_ALPHA].setCurrentAlpha(
                                mSeekBars[LOCKSCREEN_ALPHA].getCurrentAlpha());
                    }
                    if (linkTransparencies) {
                        String config = mSeekBars[STATUSBAR_ALPHA].getCurrentAlpha() + ";" +
                                mSeekBars[STATUSBAR_KG_ALPHA].getCurrentAlpha();
                        Settings.System.putString(mContentResolver,
                                Settings.System.STATUS_BAR_ALPHA_CONFIG, config);
                        Settings.System.putString(mContentResolver,
                                Settings.System.NAVIGATION_BAR_ALPHA_CONFIG, config);
                    } else {
                        String sbConfig = mSeekBars[STATUSBAR_ALPHA].getCurrentAlpha() + ";" +
                                mSeekBars[STATUSBAR_KG_ALPHA].getCurrentAlpha();
                        Settings.System.putString(mContentResolver,
                                Settings.System.STATUS_BAR_ALPHA_CONFIG, sbConfig);

                        String nbConfig = mSeekBars[NAVBAR_ALPHA].getCurrentAlpha() + ";" +
                                mSeekBars[NAVBAR_KG_ALPHA].getCurrentAlpha();
                        Settings.System.putString(mContentResolver,
                                Settings.System.NAVIGATION_BAR_ALPHA_CONFIG, nbConfig);
                    }

                }
            });
            return builder.create();
        }

        private void resetSettings() {
            Settings.System.putString(getActivity().getContentResolver(),
                    Settings.System.STATUS_BAR_ALPHA_CONFIG, null);
            Settings.System.putString(getActivity().getContentResolver(),
                    Settings.System.NAVIGATION_BAR_ALPHA_CONFIG, null);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.LOCKSCREEN_ALPHA_CONFIG, KEYGUARD_ALPHA);
        }

        private void updateToggleState() {
            if (linkTransparencies) {
                mSbLabel.setText(R.string.transparency_dialog_transparency_sb_and_nv);
                mNavigationBarGroup.setVisibility(View.GONE);
            } else {
                mSbLabel.setText(R.string.transparency_dialog_statusbar);
                mNavigationBarGroup.setVisibility(View.VISIBLE);
            }

            mSeekBars[STATUSBAR_KG_ALPHA]
                    .setEnabled(!mMatchStatusbarKeyguard.isChecked());
            mSeekBars[NAVBAR_KG_ALPHA]
                    .setEnabled(!mMatchNavbarKeyguard.isChecked());

            // update keyguard alpha
            int lockscreen_alpha = Settings.System.getInt(getActivity().getContentResolver(),
                        Settings.System.LOCKSCREEN_ALPHA_CONFIG, KEYGUARD_ALPHA);
            if (!mSeekBars[STATUSBAR_KG_ALPHA].isEnabled()) {
                mSeekBars[STATUSBAR_KG_ALPHA].setCurrentAlpha(lockscreen_alpha);
            }
            if (!mSeekBars[NAVBAR_KG_ALPHA].isEnabled()) {
                mSeekBars[NAVBAR_KG_ALPHA].setCurrentAlpha(lockscreen_alpha);
            }
        }

        @Override
        public void onDestroyView() {
            if (getDialog() != null && getRetainInstance())
                getDialog().setDismissMessage(null);
            super.onDestroyView();
        }

        private CompoundButton.OnCheckedChangeListener mUpdateStatesListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                updateToggleState();
            }
        };

        private boolean getSavedLinkedState() {
            return getActivity().getSharedPreferences("transparency", Context.MODE_PRIVATE)
                    .getBoolean("link", true);
        }

        private void saveSavedLinkedState(boolean v) {
            getActivity().getSharedPreferences("transparency", Context.MODE_PRIVATE).edit()
                    .putBoolean("link", v).commit();
        }
    }

    private boolean removePreferenceIfPackageNotInstalled(Preference preference) {
        String intentUri = ((PreferenceScreen) preference).getIntent().toUri(1);
        Pattern pattern = Pattern.compile("component=([^/]+)/");
        Matcher matcher = pattern.matcher(intentUri);

        String packageName = matcher.find() ? matcher.group(1) : null;
        if (packageName != null) {
            try {
                getPackageManager().getPackageInfo(packageName, 0);
            } catch (NameNotFoundException e) {
                Log.e(TAG, "package " + packageName + " not installed, hiding preference.");
                getPreferenceScreen().removePreference(preference);
                return true;
            }
        }
        return false;
    }

    private boolean handleBackgroundSelection(int selection) {
        if (selection == LOCKSCREEN_BACKGROUND_COLOR_FILL) {
            final ColorPickerView colorView = new ColorPickerView(getActivity());
            int currentColor = Settings.System.getInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_BACKGROUND, -1);

            if (currentColor != -1) {
                colorView.setColor(currentColor);
            }
            colorView.setAlphaSliderVisible(true);

            new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.lockscreen_custom_background_dialog_title)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Settings.System.putInt(getContentResolver(),
                                    Settings.System.LOCKSCREEN_BACKGROUND, colorView.getColor());
                            updateCustomBackgroundSummary();
                        }
                    })
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .setView(colorView)
                    .show();
        } else if (selection == LOCKSCREEN_BACKGROUND_CUSTOM_IMAGE) {
            final Intent intent = new Intent(Intent.ACTION_GET_CONTENT, null);
            intent.setType("image/*");
            intent.putExtra("crop", "true");
            intent.putExtra("scale", true);
            intent.putExtra("scaleUpIfNeeded", false);
            intent.putExtra("outputFormat", Bitmap.CompressFormat.PNG.toString());

            final Display display = getActivity().getWindowManager().getDefaultDisplay();
            final Rect rect = new Rect();
            final Window window = getActivity().getWindow();

            window.getDecorView().getWindowVisibleDisplayFrame(rect);

            int statusBarHeight = rect.top;
            int contentViewTop = window.findViewById(Window.ID_ANDROID_CONTENT).getTop();
            int titleBarHeight = contentViewTop - statusBarHeight;
            boolean isPortrait = getResources().getConfiguration().orientation ==
                    Configuration.ORIENTATION_PORTRAIT;

            int width = display.getWidth();
            int height = display.getHeight() - titleBarHeight;

            intent.putExtra("aspectX", isPortrait ? width : height);
            intent.putExtra("aspectY", isPortrait ? height : width);

            try {
                mWallpaperTemporary.createNewFile();
                mWallpaperTemporary.setWritable(true, false);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mWallpaperTemporary));
                intent.putExtra("return-data", false);
                getActivity().startActivityFromFragment(this, intent, REQUEST_CODE_BG_WALLPAPER);
            } catch (IOException e) {
                // Do nothing here
            } catch (ActivityNotFoundException e) {
                // Do nothing here
            }
        } else if (selection == LOCKSCREEN_BACKGROUND_DEFAULT_WALLPAPER) {
            Settings.System.putString(getContentResolver(),
                    Settings.System.LOCKSCREEN_BACKGROUND, null);
            updateCustomBackgroundSummary();
            return true;
        }

        return false;
    }
}
