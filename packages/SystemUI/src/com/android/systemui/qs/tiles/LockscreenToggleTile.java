/*
 * Copyright (C) 2015 The CyanogenMod Project
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

package com.android.systemui.qs.tiles;

import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.widget.Toast;
import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.systemui.R;
import com.android.systemui.SystemUIApplication;
import com.android.systemui.keyguard.KeyguardViewMediator;
import com.android.systemui.qs.QSTile;
import com.android.systemui.statusbar.policy.KeyguardMonitor;

public class LockscreenToggleTile extends QSTile<QSTile.BooleanState>
        implements KeyguardMonitor.Callback {

    public static final String ACTION_APPLY_LOCKSCREEN_STATE =
            "com.android.systemui.qs.tiles.action.APPLY_LOCKSCREEN_STATE";

    private static final Intent LOCK_SCREEN_SETTINGS =
            new Intent("android.settings.LOCK_SCREEN_SETTINGS");

    private KeyguardViewMediator mKeyguardViewMediator;
    private KeyguardMonitor mKeyguard;
    private boolean mVolatileState;
    private boolean mKeyguardBound;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mKeyguardViewMediator != null) {
                mKeyguardBound = mKeyguardViewMediator.isKeyguardBound();
                applyLockscreenState();
                refreshState();
            }
        }
    };

    public LockscreenToggleTile(Host host) {
        super(host);

        mKeyguard = host.getKeyguardMonitor();
        mKeyguardViewMediator =
                ((SystemUIApplication)
                        mContext.getApplicationContext()).getComponent(KeyguardViewMediator.class);
        mVolatileState = true;
        mKeyguardBound = mKeyguardViewMediator.isKeyguardBound();
        applyLockscreenState();

        mContext.registerReceiver(mReceiver, new IntentFilter(ACTION_APPLY_LOCKSCREEN_STATE));
    }

    @Override
    public void setListening(boolean listening) {
        if (listening) {
            mKeyguard.addCallback(this);
        } else {
            mKeyguard.removeCallback(this);
        }
    }

    @Override
    public BooleanState newTileState() {
        return new BooleanState();
    }

    @Override
    public Intent getLongClickIntent() {
        return null;
    }

    @Override
    protected void handleClick() {
        if (!mKeyguard.isShowing() || !mKeyguard.isSecure()) {
            mVolatileState = !mVolatileState;
            applyLockscreenState();
            refreshState();
	}
    }

    @Override
    protected void handleLongClick() {
        mHost.startActivityDismissingKeyguard(LOCK_SCREEN_SETTINGS);
    }

    @Override
    public CharSequence getTileLabel() {
        return mContext.getString(R.string.quick_settings_lockscreen_label);
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        final boolean lockscreenEnforced = mKeyguardViewMediator.lockscreenEnforcedByDevicePolicy();
        final boolean lockscreenEnabled = lockscreenEnforced
                || mVolatileState
                || mKeyguardViewMediator.getKeyguardEnabledInternal();

        state.label = mHost.getContext().getString(lockscreenEnforced
                ? R.string.quick_settings_lockscreen_label_enforced
                : R.string.quick_settings_lockscreen_label);
        state.contentDescription = mHost.getContext().getString(lockscreenEnabled
                ? R.string.accessibility_quick_settings_lock_screen_on
                : R.string.accessibility_quick_settings_lock_screen_off);

        if (mKeyguard.isShowing() && mKeyguard.isSecure()) {
	    Drawable icon = mHost.getContext().getDrawable(R.drawable.ic_qs_lock_screen_on)
		    .mutate();
	    final int disabledColor = mHost.getContext().getColor(R.color.qs_tile_tint_unavailable);
	    icon.setTint(disabledColor);
	    state.icon = new DrawableIcon(icon);
	    state.label = new SpannableStringBuilder().append(state.label,
                    new ForegroundColorSpan(disabledColor),
	            SpannableStringBuilder.SPAN_INCLUSIVE_INCLUSIVE);
	    return;
	}

        state.value = lockscreenEnabled;
	state.icon = ResourceIcon.get(lockscreenEnabled
		? R.drawable.ic_qs_lock_screen_on
		: R.drawable.ic_qs_lock_screen_off);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.QS_PANEL;
    }

    @Override
    protected String composeChangeAnnouncement() {
        if (mState.value) {
            return mContext.getString(
                    R.string.accessibility_quick_settings_lock_screen_changed_on);
        } else {
            return mContext.getString(
                    R.string.accessibility_quick_settings_lock_screen_changed_off);
        }
    }

    @Override
    protected void handleDestroy() {
        super.handleDestroy();
        mContext.unregisterReceiver(mReceiver);
    }

    @Override
    public void onKeyguardChanged() {
        refreshState();
    }

    private void applyLockscreenState() {
        if (!mKeyguardBound) {
            // do nothing yet
            return;
        }

        mKeyguardViewMediator.setKeyguardEnabledInternal(mVolatileState);
    }
}
