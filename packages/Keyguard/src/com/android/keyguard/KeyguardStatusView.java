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

package com.android.keyguard;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.graphics.Typeface;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.PorterDuff.Mode;
import android.os.UserHandle;
import android.provider.AlarmClock;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Slog;
import android.util.TypedValue;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextClock;
import android.widget.TextView;

import com.android.internal.widget.LockPatternUtils;

import java.util.Date;
import java.util.Locale;

public class KeyguardStatusView extends GridLayout implements
        OmniJawsClient.OmniJawsObserver {
    private static final boolean DEBUG = KeyguardConstants.DEBUG;
    private static final String TAG = "KeyguardStatusView";

    private final LockPatternUtils mLockPatternUtils;
    private final AlarmManager mAlarmManager;

    private TextView mAlarmStatusView;
    private TextClock mDateView;
    private TextClock mClockView;
    private TextView mOwnerInfo;
    private View mWeatherView;
    private TextView mWeatherCity;
    private ImageView mWeatherConditionImage;
    private Drawable mWeatherConditionDrawable;
    private TextView mWeatherCurrentTemp;
    private TextView mWeatherConditionText;
    private OmniJawsClient mWeatherClient;
    private OmniJawsClient.WeatherInfo mWeatherData;
    private boolean mWeatherEnabled;

    private boolean mShowWeather;
    private int mIconNameValue = -1;

    private int hideMode;
    private int currentVisibleNotifications;
    private int numberOfNotificationsToHide;
    private boolean showLocation;
    private SettingsObserver mSettingsObserver;

    //On the first boot, keyguard will start to receiver TIME_TICK intent.
    //And onScreenTurnedOff will not get called if power off when keyguard is not started.
    //Set initial value to false to skip the above case.
    private boolean enableRefresh = false;

    private KeyguardUpdateMonitorCallback mInfoCallback = new KeyguardUpdateMonitorCallback() {

        @Override
        public void onTimeChanged() {
            if (enableRefresh) {
                refresh();
            }
        }

        @Override
        public void onKeyguardVisibilityChanged(boolean showing) {
            if (showing) {
                if (DEBUG) Slog.v(TAG, "refresh statusview showing:" + showing);
                refresh();
                updateOwnerInfo();
            }
        }

        @Override
        public void onStartedWakingUp() {
            setEnableMarquee(true);
            enableRefresh = true;
            refresh();
        }

        @Override
        public void onFinishedGoingToSleep(int why) {
            setEnableMarquee(false);
            enableRefresh = false;
        }

        @Override
        public void onUserSwitchComplete(int userId) {
            refresh();
            updateOwnerInfo();
        }
    };

    public KeyguardStatusView(Context context) {
        this(context, null, 0);
    }

    public KeyguardStatusView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public KeyguardStatusView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mAlarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        mLockPatternUtils = new LockPatternUtils(getContext());
        mWeatherClient = new OmniJawsClient(mContext);
        mWeatherEnabled = mWeatherClient.isOmniJawsEnabled();
    }

    private void setEnableMarquee(boolean enabled) {
        if (DEBUG) Log.v(TAG, (enabled ? "Enable" : "Disable") + " transport text marquee");
        if (mAlarmStatusView != null) mAlarmStatusView.setSelected(enabled);
        if (mOwnerInfo != null) mOwnerInfo.setSelected(enabled);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mAlarmStatusView = (TextView) findViewById(R.id.alarm_status);
        mDateView = (TextClock) findViewById(R.id.date_view);
        mClockView = (TextClock) findViewById(R.id.clock_view);
        mDateView.setShowCurrentUserTime(true);
        mClockView.setShowCurrentUserTime(true);
        mOwnerInfo = (TextView) findViewById(R.id.owner_info);
        mWeatherView = findViewById(R.id.keyguard_weather_view);
        mWeatherCity = (TextView) findViewById(R.id.city);
        mWeatherConditionImage = (ImageView) findViewById(R.id.weather_image);
        mWeatherCurrentTemp = (TextView) findViewById(R.id.current_temp);
        mWeatherConditionText = (TextView) findViewById(R.id.condition);
        boolean shouldMarquee = KeyguardUpdateMonitor.getInstance(mContext).isDeviceInteractive();
        setEnableMarquee(shouldMarquee);
        refresh();
        updateOwnerInfo();

        // Disable elegant text height because our fancy colon makes the ymin value huge for no
        // reason.
        mClockView.setElegantTextHeight(false);
        mSettingsObserver = new SettingsObserver(new Handler());
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mClockView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.widget_big_font_size));
        // Some layouts like burmese have a different margin for the clock
        MarginLayoutParams layoutParams = (MarginLayoutParams) mClockView.getLayoutParams();
        layoutParams.bottomMargin = getResources().getDimensionPixelSize(
                R.dimen.bottom_text_spacing_digital);
        mClockView.setLayoutParams(layoutParams);
        mDateView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.widget_label_font_size));
        mOwnerInfo.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.widget_label_font_size));
    }

    private int getLockClockFont() {
        return Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.LOCK_CLOCK_FONTS, 4);
    }

    public void refreshTime() {
        mDateView.setFormat24Hour(Patterns.dateView);
        mDateView.setFormat12Hour(Patterns.dateView);

        mClockView.setFormat12Hour(Patterns.clockView12);
        mClockView.setFormat24Hour(Patterns.clockView24);
    }

    private void refresh() {
        AlarmManager.AlarmClockInfo nextAlarm =
                mAlarmManager.getNextAlarmClock(UserHandle.USER_CURRENT);
        Patterns.update(mContext, nextAlarm != null);

        refreshTime();
        refreshAlarmStatus(nextAlarm);
        updateSettings(false);
    }

    void refreshAlarmStatus(AlarmManager.AlarmClockInfo nextAlarm) {
        if (nextAlarm != null) {
            String alarm = formatNextAlarm(mContext, nextAlarm);
            mAlarmStatusView.setText(alarm);
            mAlarmStatusView.setContentDescription(
                    getResources().getString(R.string.keyguard_accessibility_next_alarm, alarm));
            mAlarmStatusView.setVisibility(View.VISIBLE);
        } else {
            mAlarmStatusView.setVisibility(View.GONE);
        }
    }

    public static String formatNextAlarm(Context context, AlarmManager.AlarmClockInfo info) {
        if (info == null) {
            return "";
        }
        String skeleton = DateFormat.is24HourFormat(context, ActivityManager.getCurrentUser())
                ? "EHm"
                : "Ehma";
        String pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), skeleton);
        return DateFormat.format(pattern, info.getTriggerTime()).toString();
    }

    private void updateOwnerInfo() {
        if (mOwnerInfo == null) return;
        String ownerInfo = getOwnerInfo();
        if (!TextUtils.isEmpty(ownerInfo)) {
            mOwnerInfo.setVisibility(View.VISIBLE);
            mOwnerInfo.setText(ownerInfo);
        } else {
            mOwnerInfo.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        KeyguardUpdateMonitor.getInstance(mContext).registerCallback(mInfoCallback);
        mWeatherEnabled = mWeatherClient.isOmniJawsEnabled();
        mWeatherClient.addObserver(this);
        mSettingsObserver.observe();
        queryAndUpdateWeather();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        KeyguardUpdateMonitor.getInstance(mContext).removeCallback(mInfoCallback);
        mWeatherClient.removeObserver(this);
        mWeatherClient.cleanupObserver();
        mSettingsObserver.unobserve();
    }

    private String getOwnerInfo() {
        String info = null;
        if (mLockPatternUtils.isDeviceOwnerInfoEnabled()) {
            // Use the device owner information set by device policy client via
            // device policy manager.
            info = mLockPatternUtils.getDeviceOwnerInfo();
        } else {
            // Use the current user owner information if enabled.
            final boolean ownerInfoEnabled = mLockPatternUtils.isOwnerInfoEnabled(
                    KeyguardUpdateMonitor.getCurrentUser());
            if (ownerInfoEnabled) {
                info = mLockPatternUtils.getOwnerInfo(KeyguardUpdateMonitor.getCurrentUser());
            }
        }
        return info;
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    @Override
    public void weatherUpdated() {
        queryAndUpdateWeather();
    }


    @Override
    public void weatherError() {
        // Do nothing
    }

    public void queryAndUpdateWeather() {
        try {
                if (mWeatherEnabled) {
                    mWeatherClient.queryWeather();
                    mWeatherData = mWeatherClient.getWeatherInfo();
                    mWeatherCity.setText(mWeatherData.city);
                    mWeatherConditionImage.setImageDrawable(
                        mWeatherClient.getWeatherConditionImage(mWeatherData.conditionCode));
                    mWeatherCurrentTemp.setText(mWeatherData.temp + mWeatherData.tempUnits);
                    mWeatherConditionText.setText(mWeatherData.condition);
                    mWeatherView.setVisibility(mShowWeather ? View.VISIBLE : View.GONE);
                    updateSettings(false);
                } else {
                    mWeatherCity.setText(null);
                    mWeatherConditionImage.setImageDrawable(mContext
                        .getResources().getDrawable(R.drawable.keyguard_weather_default_off));
                    mWeatherCurrentTemp.setText(null);
                    mWeatherConditionText.setText(null);
                    updateSettings(true);
                }
          } catch(Exception e) {
            // Do nothing
       }
    }

    private void updateSettings(boolean forceHide) {
        final ContentResolver resolver = getContext().getContentResolver();
        final Resources res = getContext().getResources();
        View weatherPanel = findViewById(R.id.weather_panel);
        TextView noWeatherInfo = (TextView) findViewById(R.id.no_weather_info_text);
        int primaryTextColor =
                res.getColor(R.color.keyguard_default_primary_text_color);
        // primaryTextColor with a transparency of 70%
        int secondaryTextColor = (179 << 24) | (primaryTextColor & 0x00ffffff);
        // primaryTextColor with a transparency of 50%
        int alarmTextAndIconColor = (128 << 24) | (primaryTextColor & 0x00ffffff);
        int defaultIconColor =
                res.getColor(R.color.keyguard_default_icon_color);
        int maxAllowedNotifications = 6;
        boolean forceHideByNumberOfNotifications = false;

        if (hideMode == 0) {
            if (currentVisibleNotifications > maxAllowedNotifications) {
                forceHideByNumberOfNotifications = true;
            }
        } else if (hideMode == 1) {
            if (currentVisibleNotifications >= numberOfNotificationsToHide) {
                forceHideByNumberOfNotifications = true;
            }
        }

        if (mWeatherView != null) {
            mWeatherView.setVisibility(
                (mShowWeather && !forceHideByNumberOfNotifications) ? View.VISIBLE : View.GONE);
        }
        if (forceHide) {
            if (noWeatherInfo != null) {
                noWeatherInfo.setVisibility(View.VISIBLE);
            }
            if (weatherPanel != null) {
                weatherPanel.setVisibility(View.GONE);
            }
            if (mWeatherConditionText != null) {
                mWeatherConditionText.setVisibility(View.GONE);
            }
        } else {
            if (noWeatherInfo != null) {
                noWeatherInfo.setVisibility(View.GONE);
            }
            if (weatherPanel != null) {
                weatherPanel.setVisibility(View.VISIBLE);
            }
            if (mWeatherConditionText != null) {
                mWeatherConditionText.setVisibility(View.VISIBLE);
            }
            if (mWeatherCity != null) {
                mWeatherCity.setVisibility(showLocation ? View.VISIBLE : View.INVISIBLE);
            }
        }

        mAlarmStatusView.setTextColor(alarmTextAndIconColor);
        mDateView.setTextColor(primaryTextColor);
        mClockView.setTextColor(primaryTextColor);

        if (noWeatherInfo != null) {
            noWeatherInfo.setTextColor(primaryTextColor);
        }
        if (mWeatherCity != null) {
            mWeatherCity.setTextColor(primaryTextColor);
        }
        if (mWeatherConditionText != null) {
            mWeatherConditionText.setTextColor(primaryTextColor);
        }
        if (mWeatherCurrentTemp != null) {
            mWeatherCurrentTemp.setTextColor(primaryTextColor);
        }

        boolean isPrimary = UserHandle.getCallingUserId() == UserHandle.USER_OWNER;
        int lockClockFont = isPrimary ? getLockClockFont() : 0;

        switch (lockClockFont) {
            case 0:
                mClockView.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
                break;
            case 1:
                mClockView.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
                break;
            case 2:
                mClockView.setTypeface(Typeface.create("sans-serif", Typeface.ITALIC));
                break;
            case 3:
                mClockView.setTypeface(Typeface.create("sans-serif", Typeface.BOLD_ITALIC));
                break;
            case 4:
                mClockView.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
                break;
            case 5:
                mClockView.setTypeface(Typeface.create("sans-serif-light", Typeface.ITALIC));
                break;
            case 6:
                mClockView.setTypeface(Typeface.create("sans-serif-thin", Typeface.NORMAL));
                break;
            case 7:
                mClockView.setTypeface(Typeface.create("sans-serif-thin", Typeface.ITALIC));
                break;
            case 8:
                mClockView.setTypeface(Typeface.create("sans-serif-condensed", Typeface.NORMAL));
                break;
            case 9:
                mClockView.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
                break;
            case 10:
                mClockView.setTypeface(Typeface.create("sans-serif-condensed", Typeface.ITALIC));
                break;
            case 11:
                mClockView.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD_ITALIC));
                break;
            case 12:
                mClockView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
                break;
            case 13:
                mClockView.setTypeface(Typeface.create("sans-serif-medium", Typeface.ITALIC));
                break;
            case 14:
                mClockView.setTypeface(Typeface.create("sans-serif-black", Typeface.NORMAL));
                break;
            case 15:
                mClockView.setTypeface(Typeface.create("sans-serif-black", Typeface.ITALIC));
                break;
            case 16:
                mClockView.setTypeface(Typeface.create("sans-serif-condensed-light", Typeface.NORMAL));
                break;
            case 17:
                mClockView.setTypeface(Typeface.create("sans-serif-condensed-light", Typeface.ITALIC));
                break;
        }

        Drawable[] drawables = mAlarmStatusView.getCompoundDrawablesRelative();
        Drawable alarmIcon = null;
        mAlarmStatusView.setCompoundDrawablesRelative(null, null, null, null);
        if (drawables[0] != null) {
            alarmIcon = drawables[0];
            alarmIcon.setColorFilter(alarmTextAndIconColor, Mode.MULTIPLY);
        }
        mAlarmStatusView.setCompoundDrawablesRelative(alarmIcon, null, null, null);
    }

    // DateFormat.getBestDateTimePattern is extremely expensive, and refresh is called often.
    // This is an optimization to ensure we only recompute the patterns when the inputs change.
    private static final class Patterns {
        static String dateView;
        static String clockView12;
        static String clockView24;
        static String cacheKey;

        static void update(Context context, boolean hasAlarm) {
            final Locale locale = Locale.getDefault();
            final Resources res = context.getResources();
            final String dateViewSkel = res.getString(hasAlarm
                    ? R.string.abbrev_wday_month_day_no_year_alarm
                    : R.string.abbrev_wday_month_day_no_year);
            final String clockView12Skel = res.getString(R.string.clock_12hr_format);
            final String clockView24Skel = res.getString(R.string.clock_24hr_format);
            final String key = locale.toString() + dateViewSkel + clockView12Skel + clockView24Skel;
            if (key.equals(cacheKey)) return;

            dateView = DateFormat.getBestDateTimePattern(locale, dateViewSkel);

            clockView12 = DateFormat.getBestDateTimePattern(locale, clockView12Skel);
            // CLDR insists on adding an AM/PM indicator even though it wasn't in the skeleton
            // format.  The following code removes the AM/PM indicator if we didn't want it.
            if (!clockView12Skel.contains("a")) {
                clockView12 = clockView12.replaceAll("a", "").trim();
            }

            clockView24 = DateFormat.getBestDateTimePattern(locale, clockView24Skel);

            // Use fancy colon.
            clockView24 = clockView24.replace(':', '\uee01');
            clockView12 = clockView12.replace(':', '\uee01');

            cacheKey = key;
        }
    }

    class SettingsObserver extends ContentObserver {
         SettingsObserver(Handler handler) {
             super(handler);
         }

         void observe() {
             ContentResolver resolver = mContext.getContentResolver();
             resolver.registerContentObserver(Settings.System.getUriFor(
                     Settings.System.LOCK_SCREEN_VISIBLE_NOTIFICATIONS), false, this, UserHandle.USER_ALL);
             resolver.registerContentObserver(Settings.System.getUriFor(
                     Settings.System.LOCK_SCREEN_WEATHER_NUMBER_OF_NOTIFICATIONS), false, this, UserHandle.USER_ALL);
             resolver.registerContentObserver(Settings.System.getUriFor(
                     Settings.System.LOCK_SCREEN_WEATHER_HIDE_PANEL), false, this, UserHandle.USER_ALL);
             resolver.registerContentObserver(Settings.System.getUriFor(
                     Settings.System.LOCK_SCREEN_WEATHER_CONDITION_ICON), false, this, UserHandle.USER_ALL);
             resolver.registerContentObserver(Settings.System.getUriFor(
                     Settings.System.LOCK_SCREEN_SHOW_WEATHER), false, this, UserHandle.USER_ALL);
             resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.OMNIJAWS_WEATHER_ICON_PACK), false, this, UserHandle.USER_ALL);
             resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.LOCK_SCREEN_SHOW_WEATHER_LOCATION), false, this, UserHandle.USER_ALL);

             update();
         }

         void unobserve() {
             ContentResolver resolver = mContext.getContentResolver();
             resolver.unregisterContentObserver(this);
         }

         @Override
         public void onChange(boolean selfChange, Uri uri) {
             if (uri.equals(Settings.System.getUriFor(
                     Settings.System.LOCK_SCREEN_WEATHER_NUMBER_OF_NOTIFICATIONS))) {
                 queryAndUpdateWeather();
             } else if (uri.equals(Settings.System.getUriFor(
                     Settings.System.LOCK_SCREEN_VISIBLE_NOTIFICATIONS))) {
                 queryAndUpdateWeather();
             } else if (uri.equals(Settings.System.getUriFor(
                     Settings.System.LOCK_SCREEN_WEATHER_HIDE_PANEL))) {
                 queryAndUpdateWeather();
             } else if (uri.equals(Settings.System.getUriFor(
                     Settings.System.LOCK_SCREEN_SHOW_WEATHER))) {
                 queryAndUpdateWeather();
             } else if (uri.equals(Settings.System.getUriFor(
                     Settings.System.OMNIJAWS_WEATHER_ICON_PACK))) {
                 queryAndUpdateWeather();
             }  else if (uri.equals(Settings.System.getUriFor(
                     Settings.System.LOCK_SCREEN_SHOW_WEATHER_LOCATION))) {
                 queryAndUpdateWeather();
             }
             update();
         }

         public void update() {
           ContentResolver resolver = mContext.getContentResolver();
           int currentUserId = ActivityManager.getCurrentUser();

           mShowWeather = Settings.System.getInt(resolver,
                Settings.System.LOCK_SCREEN_SHOW_WEATHER, 0) == 1;
           showLocation = Settings.System.getInt(resolver,
                Settings.System.LOCK_SCREEN_SHOW_WEATHER_LOCATION, 1) == 1;
           currentVisibleNotifications = Settings.System.getInt(resolver,
                Settings.System.LOCK_SCREEN_VISIBLE_NOTIFICATIONS, 0);
           hideMode = Settings.System.getInt(resolver,
                    Settings.System.LOCK_SCREEN_WEATHER_HIDE_PANEL, 0);
           numberOfNotificationsToHide = Settings.System.getInt(resolver,
                       Settings.System.LOCK_SCREEN_WEATHER_NUMBER_OF_NOTIFICATIONS, 4);

           queryAndUpdateWeather();
         }
     }
}
