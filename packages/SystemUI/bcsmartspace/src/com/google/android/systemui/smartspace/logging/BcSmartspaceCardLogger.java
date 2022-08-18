package com.google.android.systemui.smartspace.logging;

import android.util.Log;

import com.android.systemui.shared.system.SysUiStatsLog;

import com.google.android.systemui.smartspace.BcSmartSpaceUtil;
import com.google.android.systemui.smartspace.EventEnum;

public class BcSmartspaceCardLogger {
    private static final boolean IS_VERBOSE = BcSmartSpaceUtil.isLoggable("StatsLog");

    public static void log(
            EventEnum eventEnum, BcSmartspaceCardLoggingInfo bcSmartspaceCardLoggingInfo) {
        SysUiStatsLog.write(
                352,
                eventEnum.getId(),
                bcSmartspaceCardLoggingInfo.getInstanceId(),
                0,
                bcSmartspaceCardLoggingInfo.getDisplaySurface(),
                bcSmartspaceCardLoggingInfo.getRank(),
                bcSmartspaceCardLoggingInfo.getCardinality(),
                bcSmartspaceCardLoggingInfo.getFeatureType(),
                -1,
                0,
                0,
                bcSmartspaceCardLoggingInfo.getReceivedLatency(),
                BcSmartspaceCardLoggerUtil.convertSubcardInfoToBytes(
                        bcSmartspaceCardLoggingInfo.getSubcardInfo()));
        if (IS_VERBOSE) {
            Log.d(
                    "StatsLog",
                    String.format(
                            "\nLogged Smartspace event(%s), info(%s)",
                            eventEnum, bcSmartspaceCardLoggingInfo.toString()));
        }
    }
}
