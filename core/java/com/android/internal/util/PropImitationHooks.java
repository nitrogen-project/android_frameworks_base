/*
 * Copyright (C) 2022 Paranoid Android
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.internal.util;

import android.app.Application;
import android.content.res.Resources;
import android.os.Build;
import android.util.Log;

import com.android.internal.R;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class PropImitationHooks {

    private static final String TAG = "PropImitationHooks";
    private static final boolean DEBUG = false;

    private static final String sCertifiedFp =
            Resources.getSystem().getString(R.string.config_certifiedFingerprint);
    private static final String sCertifiedModel =
            Resources.getSystem().getString(R.string.config_certifiedModel);

    private static final String sStockFp =
            Resources.getSystem().getString(R.string.config_stockFingerprint);

    private static final String PACKAGE_ARCORE = "com.google.ar.core";
    private static final String PACKAGE_FINSKY = "com.android.vending";
    private static final String PACKAGE_GMS = "com.google.android.gms";
    private static final String PROCESS_GMS_UNSTABLE = PACKAGE_GMS + ".unstable";

    private static final String PACKAGE_GPHOTOS = "com.google.android.apps.photos";
    private static final Map<String, Object> sP1Props = new HashMap<>();
    static {
        sP1Props.put("BRAND", "google");
        sP1Props.put("MANUFACTURER", "Google");
        sP1Props.put("DEVICE", "marlin");
        sP1Props.put("PRODUCT", "marlin");
        sP1Props.put("MODEL", "Pixel XL");
        sP1Props.put("FINGERPRINT", "google/marlin/marlin:10/QP1A.191005.007.A3/5972272:user/release-keys");
    }
    private static final String[] sFeaturesBlacklist = {
        "PIXEL_2017_PRELOAD",
        "PIXEL_2018_PRELOAD",
        "PIXEL_2019_MIDYEAR_PRELOAD",
        "PIXEL_2019_PRELOAD",
        "PIXEL_2020_EXPERIENCE",
        "PIXEL_2020_MIDYEAR_EXPERIENCE",
        "PIXEL_2021_EXPERIENCE",
        "PIXEL_2021_MIDYEAR_EXPERIENCE"
    };

    private static final String PACKAGE_VELVET = "com.google.android.quicksearchbox";
    private static final String PACKAGE_WALLPAPERS = "com.google.android.apps.wallpaper";
    private static final Map<String, Object> sP6Props = new HashMap<>();
    static {
        sP6Props.put("BRAND", "google");
        sP6Props.put("MANUFACTURER", "Google");
        sP6Props.put("DEVICE", "raven");
        sP6Props.put("PRODUCT", "raven");
        sP6Props.put("MODEL", "Pixel 6 Pro");
        sP6Props.put("FINGERPRINT", "google/raven/raven:13/TP1A.220624.021/8877034:user/release-keys");
    }

    private static final boolean sSpoofGapps =
            Resources.getSystem().getBoolean(R.bool.config_spoofGoogleApps);

    private static volatile boolean sIsGms = false;
    private static volatile boolean sIsFinsky = false;
    private static volatile boolean sIsPhotos = false;

    public static void setProps(Application app) {
        final String packageName = app.getPackageName();
        final String processName = app.getProcessName();

        if (packageName == null || processName == null) {
            return;
        }

        sIsGms = packageName.equals(PACKAGE_GMS) && processName.equals(PROCESS_GMS_UNSTABLE);
        sIsFinsky = packageName.equals(PACKAGE_FINSKY);
        sIsPhotos = sSpoofGapps && packageName.equals(PACKAGE_GPHOTOS);

        if (!sCertifiedFp.isEmpty() && sIsGms) {
            dlog("Setting certified fingerprint/model for GMS");
            setPropValue("FINGERPRINT", sCertifiedFp);
            setPropValue("MODEL", sCertifiedModel);
        } else if (!sStockFp.isEmpty() && packageName.equals(PACKAGE_ARCORE)) {
            dlog("Setting stock fingerprint for: " + packageName);
            setPropValue("FINGERPRINT", sStockFp);
        } else if (sIsPhotos) {
            dlog("Spoofing Pixel XL for Google Photos");
            sP1Props.forEach((k, v) -> setPropValue(k, v));
        } else if (sSpoofGapps && (packageName.equals(PACKAGE_VELVET)
                || packageName.equals(PACKAGE_WALLPAPERS))) {
            dlog("Spoofing Pixel 6 Pro for: " + packageName);
            sP6Props.forEach((k, v) -> setPropValue(k, v));
        }
    }

    private static void setPropValue(String key, Object value){
        try {
            dlog("Setting prop " + key + " to " + value.toString());
            Field field = Build.class.getDeclaredField(key);
            field.setAccessible(true);
            field.set(null, value);
            field.setAccessible(false);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Log.e(TAG, "Failed to set prop " + key, e);
        }
    }

    private static boolean isCallerSafetyNet() {
        return sIsGms && Arrays.stream(Thread.currentThread().getStackTrace())
                .anyMatch(elem -> elem.getClassName().contains("DroidGuard"));
    }

    public static void onEngineGetCertificateChain() {
        // Check stack for SafetyNet or Play Integrity
        if (isCallerSafetyNet() || sIsFinsky) {
            dlog("Blocked key attestation sIsGms=" + sIsGms + " sIsFinsky=" + sIsFinsky);
            throw new UnsupportedOperationException();
        }
    }

    public static boolean hasSystemFeature(String name, boolean def) {
        if (sIsPhotos && def &&
                Arrays.stream(sFeaturesBlacklist).anyMatch(name::contains)) {
            dlog("Blocked system feature " + name + " for Google Photos");
            return false;
        }
        return def;
    }

    public static void dlog(String msg) {
      if (DEBUG) Log.d(TAG, msg);
    }
}
