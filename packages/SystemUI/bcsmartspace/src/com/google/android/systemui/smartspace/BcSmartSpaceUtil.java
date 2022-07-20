package com.google.android.systemui.smartspace;

import android.app.PendingIntent;
import android.app.smartspace.SmartspaceAction;
import android.app.smartspace.SmartspaceTarget;
import android.app.smartspace.SmartspaceTargetEvent;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.CalendarContract;
import android.util.Log;
import android.view.View;

import com.android.systemui.bcsmartspace.R;
import com.android.systemui.plugins.BcSmartspaceDataPlugin;
import com.android.systemui.plugins.FalsingManager;

import com.google.android.systemui.smartspace.logging.BcSmartspaceCardLogger;
import com.google.android.systemui.smartspace.logging.BcSmartspaceCardLoggingInfo;

public final class BcSmartSpaceUtil {
    private static FalsingManager sFalsingManager;
    private static BcSmartspaceDataPlugin.IntentStarter sIntentStarter;

    public static void setOnClickListener(
            View view,
            SmartspaceTarget smartspaceTarget,
            SmartspaceAction smartspaceAction,
            String str,
            BcSmartspaceDataPlugin.SmartspaceEventNotifier smartspaceEventNotifier,
            BcSmartspaceCardLoggingInfo bcSmartspaceCardLoggingInfo) {
        setOnClickListener(
                view,
                smartspaceTarget,
                smartspaceAction,
                null,
                str,
                smartspaceEventNotifier,
                bcSmartspaceCardLoggingInfo,
                0);
    }

    public static void setOnClickListener(
            View view,
            SmartspaceTarget smartspaceTarget,
            SmartspaceAction smartspaceAction,
            String str,
            BcSmartspaceDataPlugin.SmartspaceEventNotifier smartspaceEventNotifier,
            BcSmartspaceCardLoggingInfo bcSmartspaceCardLoggingInfo,
            int i) {
        setOnClickListener(
                view,
                smartspaceTarget,
                smartspaceAction,
                null,
                str,
                smartspaceEventNotifier,
                bcSmartspaceCardLoggingInfo,
                i);
    }

    public static void setOnClickListener(
            View view,
            final SmartspaceTarget smartspaceTarget,
            final SmartspaceAction smartspaceAction,
            final View.OnClickListener onClickListener,
            final String str,
            final BcSmartspaceDataPlugin.SmartspaceEventNotifier smartspaceEventNotifier,
            final BcSmartspaceCardLoggingInfo bcSmartspaceCardLoggingInfo,
            final int i) {
        if (view == null || smartspaceAction == null) {
            Log.e(str, "No tap action can be set up");
            return;
        }
        final boolean z =
                smartspaceAction.getExtras() != null
                        && smartspaceAction.getExtras().getBoolean("show_on_lockscreen");
        final boolean z2 =
                smartspaceAction.getIntent() == null && smartspaceAction.getPendingIntent() == null;
        BcSmartspaceDataPlugin.IntentStarter intentStarter = sIntentStarter;
        if (intentStarter == null) {
            intentStarter = defaultIntentStarter(str);
        }
        final BcSmartspaceDataPlugin.IntentStarter intentStarter2 = intentStarter;
        view.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public final void onClick(View view2) {
                        if (bcSmartspaceCardLoggingInfo != null) {
                            if (bcSmartspaceCardLoggingInfo.getSubcardInfo() != null) {
                                bcSmartspaceCardLoggingInfo
                                        .getSubcardInfo()
                                        .setClickedSubcardIndex(i);
                            }
                            BcSmartspaceCardLogger.log(
                                    BcSmartspaceEvent.SMARTSPACE_CARD_CLICK,
                                    bcSmartspaceCardLoggingInfo);
                        }
                        FalsingManager falsingManager = sFalsingManager;
                        if (falsingManager == null || !falsingManager.isFalseTap(1)) {
                            if (!z) {
                                intentStarter2.startFromAction(smartspaceAction, view2, z2);
                            }
                            if (onClickListener != null) {
                                onClickListener.onClick(view2);
                            }
                            if (smartspaceEventNotifier == null) {
                                Log.w(
                                        str,
                                        "Cannot notify target interaction smartspace event: event"
                                                + " notifier null.");
                            } else {
                                smartspaceEventNotifier.notifySmartspaceEvent(
                                        new SmartspaceTargetEvent.Builder(1)
                                                .setSmartspaceTarget(smartspaceTarget)
                                                .setSmartspaceActionId(smartspaceAction.getId())
                                                .build());
                            }
                        }
                    }
                });
    }

    public static String getDimensionRatio(Bundle bundle) {
        if (bundle != null
                && bundle.containsKey("imageRatioWidth")
                && bundle.containsKey("imageRatioHeight")) {
            int i = bundle.getInt("imageRatioWidth");
            int i2 = bundle.getInt("imageRatioHeight");
            if (i > 0 && i2 > 0) {
                return i + ":" + i2;
            }
        }
        return null;
    }

    public static Drawable getIconDrawable(Icon icon, Context context) {
        Drawable drawable;
        if (icon == null) {
            return null;
        }
        if (icon.getType() == 1 || icon.getType() == 5) {
            drawable = new BitmapDrawable(context.getResources(), icon.getBitmap());
        } else {
            drawable = icon.loadDrawable(context);
        }
        if (drawable != null) {
            int dimensionPixelSize =
                    context.getResources()
                            .getDimensionPixelSize(R.dimen.enhanced_smartspace_icon_size);
            drawable.setBounds(0, 0, dimensionPixelSize, dimensionPixelSize);
        }
        return drawable;
    }

    public static void setFalsingManager(FalsingManager falsingManager) {
        sFalsingManager = falsingManager;
    }

    public static void setIntentStarter(BcSmartspaceDataPlugin.IntentStarter intentStarter) {
        sIntentStarter = intentStarter;
    }

    private static BcSmartspaceDataPlugin.IntentStarter defaultIntentStarter(final String str) {
        return new BcSmartspaceDataPlugin.IntentStarter() {
            @Override
            public void startIntent(View view, Intent intent, boolean z) {
                try {
                    view.getContext().startActivity(intent);
                } catch (ActivityNotFoundException | NullPointerException | SecurityException e) {
                    Log.e(str, "Cannot invoke smartspace intent", e);
                }
            }

            @Override
            public void startPendingIntent(PendingIntent pendingIntent, boolean z) {
                try {
                    pendingIntent.send();
                } catch (PendingIntent.CanceledException e) {
                    Log.e(str, "Cannot invoke canceled smartspace intent", e);
                }
            }
        };
    }

    public static boolean isLoggable(String str) {
        return Log.isLoggable(str, 2);
    }

    public static int getLoggingDisplaySurface(String str, float f) {
        str.hashCode();
        if (!str.equals("com.google.android.apps.nexuslauncher")) {
            if (!str.equals("com.android.systemui")) {
                return 0;
            }
            if (f == 1.0f) {
                return 3;
            }
            return f == 0.0f ? 2 : -1;
        }
        return 1;
    }

    public static SmartspaceTarget createUpcomingAlarmTarget(
            ComponentName componentName, UserHandle userHandle) {
        return new SmartspaceTarget.Builder(
                        "upcoming_alarm_card_94510_12684", componentName, userHandle)
                .setFeatureType(23)
                .build();
    }

    public static Intent getOpenCalendarIntent() {
        return new Intent("android.intent.action.VIEW")
                .setData(
                        ContentUris.appendId(
                                        CalendarContract.CONTENT_URI.buildUpon().appendPath("time"),
                                        System.currentTimeMillis())
                                .build())
                .addFlags(270532608);
    }
}
