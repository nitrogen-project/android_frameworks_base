/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.android.systemui.tuner;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.provider.Settings.System;
import android.support.v14.preference.PreferenceFragment;
import android.support.v7.preference.ListPreference;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.android.systemui.tuner.TunerService;
import com.android.systemui.tuner.TunerService.Tunable;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.systemui.R;

public class TunerFragment extends PreferenceFragment implements 
		Preference.OnPreferenceChangeListener {

    private static final String TAG = "TunerFragment";

    public static final String SETTING_SEEN_TUNER_WARNING = "seen_tuner_warning";
    private static final String KEY_SYSUI_QQS_COUNT = "sysui_qqs_count_key";
    public static final String NUM_QUICK_TILES = "sysui_qqs_count";

    private static final String WARNING_TAG = "tuner_warning";

    private static final int MENU_REMOVE = Menu.FIRST + 1;
    private ListPreference mSysuiQqsCount;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.tuner_prefs);

        if (Settings.Secure.getInt(getContext().getContentResolver(), SETTING_SEEN_TUNER_WARNING,
                0) == 0) {
            if (getFragmentManager().findFragmentByTag(WARNING_TAG) == null) {
                new TunerWarningFragment().show(getFragmentManager(), WARNING_TAG);
            }
        }

        mSysuiQqsCount = (ListPreference) findPreference(KEY_SYSUI_QQS_COUNT);
            if (mSysuiQqsCount != null) {
               mSysuiQqsCount.setOnPreferenceChangeListener(this);
               int SysuiQqsCount = TunerService.get(getContext()).getValue(NUM_QUICK_TILES, 5);
               mSysuiQqsCount.setValue(Integer.toString(SysuiQqsCount));
               mSysuiQqsCount.setSummary(mSysuiQqsCount.getEntry());
            }
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().setTitle(R.string.system_ui_tuner);

        MetricsLogger.visibility(getContext(), MetricsEvent.TUNER, true);
    }

    @Override
    public void onPause() {
        super.onPause();

        MetricsLogger.visibility(getContext(), MetricsEvent.TUNER, false);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mSysuiQqsCount) {
            String SysuiQqsCount = (String) newValue;
            int SysuiQqsCountValue = Integer.parseInt(SysuiQqsCount);
            TunerService.get(getContext()).setValue(NUM_QUICK_TILES, SysuiQqsCountValue);
            int SysuiQqsCountIndex = mSysuiQqsCount
                    .findIndexOfValue(SysuiQqsCount);
            mSysuiQqsCount
                    .setSummary(mSysuiQqsCount.getEntries()[SysuiQqsCountIndex]);
            return true;
          }

         return false;
     }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                getActivity().finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class TunerWarningFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getContext())
                    .setTitle(R.string.tuner_warning_title)
                    .setMessage(R.string.tuner_warning)
                    .setPositiveButton(R.string.got_it, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Settings.Secure.putInt(getContext().getContentResolver(),
                                    SETTING_SEEN_TUNER_WARNING, 1);
                        }
                    }).show();
        }
    }
}
