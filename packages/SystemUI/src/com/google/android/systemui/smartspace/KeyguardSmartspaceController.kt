package com.google.android.systemui.smartspace

import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.flags.FeatureFlags

import com.google.android.systemui.smartspace.KeyguardMediaViewController
import com.google.android.systemui.smartspace.KeyguardZenAlarmViewController

import javax.inject.Inject

@SysUISingleton
class KeyguardSmartspaceController @Inject constructor(
    private val featureFlags: FeatureFlags,
    private val zenController: KeyguardZenAlarmViewController,
    private val mediaController: KeyguardMediaViewController,
) {
    init {
        if (featureFlags.isSmartspaceEnabled()) {
            mediaController.init()
            zenController.init()
        }
    }
}
