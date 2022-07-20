package com.google.android.systemui.smartspace;

import android.app.smartspace.SmartspaceAction;
import android.app.smartspace.SmartspaceTarget;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

import com.android.launcher3.icons.GraphicsUtils;
import com.android.systemui.bcsmartspace.R;
import com.android.systemui.plugins.BcSmartspaceDataPlugin;

import com.google.android.systemui.smartspace.logging.BcSmartspaceCardLoggingInfo;
import com.google.android.systemui.smartspace.logging.BcSmartspaceCardMetadataLoggingInfo;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class BcSmartspaceCard extends LinearLayout {
    private static final SmartspaceAction SHOW_ALARMS_ACTION =
            new SmartspaceAction.Builder("nextAlarmId", "Next alarm")
                    .setIntent(new Intent("android.intent.action.SHOW_ALARMS"))
                    .build();
    private DoubleShadowTextView mBaseActionIconSubtitleView;
    private IcuDateTextView mDateView;
    private ImageView mDndImageView;
    private float mDozeAmount;
    private BcSmartspaceDataPlugin.SmartspaceEventNotifier mEventNotifier;
    private ViewGroup mExtrasGroup;
    private DoubleShadowIconDrawable mIconDrawable;
    private int mIconTintColor;
    private BcSmartspaceCardLoggingInfo mLoggingInfo;
    private ImageView mNextAlarmImageView;
    private TextView mNextAlarmTextView;
    private BcSmartspaceCardSecondary mSecondaryCard;
    private TextView mSubtitleTextView;
    private SmartspaceTarget mTarget;
    private TextView mTitleTextView;
    private int mTopPadding;
    private boolean mUsePageIndicatorUi;

    public BcSmartspaceCard(Context context) {
        this(context, null);
    }

    public BcSmartspaceCard(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        mSecondaryCard = null;
        mIconTintColor = GraphicsUtils.getAttrColor(getContext(), 16842806);
        mDateView = null;
        mTitleTextView = null;
        mSubtitleTextView = null;
        mBaseActionIconSubtitleView = null;
        mExtrasGroup = null;
        mDndImageView = null;
        mNextAlarmImageView = null;
        mNextAlarmTextView = null;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mDateView = (IcuDateTextView) findViewById(R.id.date);
        mTitleTextView = (TextView) findViewById(R.id.title_text);
        mSubtitleTextView = (TextView) findViewById(R.id.subtitle_text);
        mBaseActionIconSubtitleView =
                (DoubleShadowTextView) findViewById(R.id.base_action_icon_subtitle);
        mExtrasGroup = (ViewGroup) findViewById(R.id.smartspace_extras_group);
        mTopPadding = getPaddingTop();
        ViewGroup viewGroup = mExtrasGroup;
        if (viewGroup != null) {
            mDndImageView = (ImageView) viewGroup.findViewById(R.id.dnd_icon);
            mNextAlarmImageView = (ImageView) mExtrasGroup.findViewById(R.id.alarm_icon);
            mNextAlarmTextView = (TextView) mExtrasGroup.findViewById(R.id.alarm_text);
        }
    }

    public void setEventNotifier(
            BcSmartspaceDataPlugin.SmartspaceEventNotifier smartspaceEventNotifier) {
        mEventNotifier = smartspaceEventNotifier;
    }

    public void setSmartspaceTarget(
            SmartspaceTarget smartspaceTarget,
            BcSmartspaceCardLoggingInfo bcSmartspaceCardLoggingInfo,
            boolean z) {
        String str;
        int i;
        mTarget = smartspaceTarget;
        SmartspaceAction headerAction = smartspaceTarget.getHeaderAction();
        SmartspaceAction baseAction = smartspaceTarget.getBaseAction();
        mLoggingInfo = bcSmartspaceCardLoggingInfo;
        mUsePageIndicatorUi = z;
        if (headerAction != null) {
            BcSmartspaceCardSecondary bcSmartspaceCardSecondary = mSecondaryCard;
            if (bcSmartspaceCardSecondary != null) {
                mSecondaryCard.setVisibility(
                        bcSmartspaceCardSecondary.setSmartspaceActions(
                                        smartspaceTarget,
                                        mEventNotifier,
                                        bcSmartspaceCardLoggingInfo)
                                ? View.VISIBLE
                                : View.GONE);
            }
            Drawable iconDrawable =
                    BcSmartSpaceUtil.getIconDrawable(headerAction.getIcon(), getContext());
            mIconDrawable =
                    iconDrawable == null
                            ? null
                            : new DoubleShadowIconDrawable(iconDrawable, getContext());
            CharSequence title = headerAction.getTitle();
            CharSequence subtitle = headerAction.getSubtitle();
            boolean z2 = smartspaceTarget.getFeatureType() == 1 || !TextUtils.isEmpty(title);
            boolean z3 = !TextUtils.isEmpty(subtitle);
            updateZenVisibility();
            if (!z2) {
                title = subtitle;
            }
            setTitle(title, headerAction.getContentDescription(), z2 != z3);
            if (!z2 || !z3) {
                subtitle = null;
            }
            setSubtitle(subtitle, headerAction.getContentDescription());
            updateIconTint();
        }
        if (baseAction != null && mBaseActionIconSubtitleView != null) {
            Drawable iconDrawable2 =
                    baseAction.getIcon() == null
                            ? null
                            : BcSmartSpaceUtil.getIconDrawable(baseAction.getIcon(), getContext());
            if (iconDrawable2 == null) {
                mBaseActionIconSubtitleView.setVisibility(View.INVISIBLE);
                mBaseActionIconSubtitleView.setOnClickListener(null);
                mBaseActionIconSubtitleView.setContentDescription(null);
            } else {
                iconDrawable2.setTintList(null);
                mBaseActionIconSubtitleView.setText(baseAction.getSubtitle());
                mBaseActionIconSubtitleView.setCompoundDrawablesRelative(
                        iconDrawable2, null, null, null);
                mBaseActionIconSubtitleView.setVisibility(View.VISIBLE);
                int subcardType = getSubcardType(baseAction);
                if (subcardType != -1) {
                    i = getClickedIndex(bcSmartspaceCardLoggingInfo, subcardType);
                } else {
                    Log.d(
                            "BcSmartspaceCard",
                            String.format(
                                    "Subcard expected but missing type. loggingInfo=%s,"
                                            + " baseAction=%s",
                                    bcSmartspaceCardLoggingInfo.toString(), baseAction.toString()));
                    i = 0;
                }
                BcSmartSpaceUtil.setOnClickListener(
                        mBaseActionIconSubtitleView,
                        smartspaceTarget,
                        baseAction,
                        "BcSmartspaceCard",
                        mEventNotifier,
                        bcSmartspaceCardLoggingInfo,
                        i);
                setFormattedContentDescription(
                        mBaseActionIconSubtitleView,
                        baseAction.getSubtitle(),
                        baseAction.getContentDescription());
            }
        }
        if (mDateView != null) {
            if (headerAction != null) {
                str = headerAction.getId();
            } else if (baseAction != null) {
                str = baseAction.getId();
            } else {
                str = UUID.randomUUID().toString();
            }
            BcSmartSpaceUtil.setOnClickListener(
                    mDateView,
                    smartspaceTarget,
                    new SmartspaceAction.Builder(str, "unusedTitle")
                            .setIntent(BcSmartSpaceUtil.getOpenCalendarIntent())
                            .build(),
                    "BcSmartspaceCard",
                    mEventNotifier,
                    bcSmartspaceCardLoggingInfo);
        }
        if (hasIntent(headerAction)) {
            BcSmartSpaceUtil.setOnClickListener(
                    this,
                    smartspaceTarget,
                    headerAction,
                    "BcSmartspaceCard",
                    mEventNotifier,
                    bcSmartspaceCardLoggingInfo,
                    (smartspaceTarget.getFeatureType() == 1
                                    && bcSmartspaceCardLoggingInfo.getFeatureType() == 39)
                            ? getClickedIndex(bcSmartspaceCardLoggingInfo, 1)
                            : 0);
        } else if (hasIntent(baseAction)) {
            BcSmartSpaceUtil.setOnClickListener(
                    this,
                    smartspaceTarget,
                    baseAction,
                    "BcSmartspaceCard",
                    mEventNotifier,
                    bcSmartspaceCardLoggingInfo);
        } else {
            BcSmartSpaceUtil.setOnClickListener(
                    this,
                    smartspaceTarget,
                    headerAction,
                    "BcSmartspaceCard",
                    mEventNotifier,
                    bcSmartspaceCardLoggingInfo);
        }
    }

    private int getClickedIndex(BcSmartspaceCardLoggingInfo bcSmartspaceCardLoggingInfo, int i) {
        if (bcSmartspaceCardLoggingInfo == null
                || bcSmartspaceCardLoggingInfo.getSubcardInfo() == null
                || bcSmartspaceCardLoggingInfo.getSubcardInfo().getSubcards() == null) {
            return 0;
        }
        List<BcSmartspaceCardMetadataLoggingInfo> subcards =
                bcSmartspaceCardLoggingInfo.getSubcardInfo().getSubcards();
        for (int i2 = 0; i2 < subcards.size(); i2++) {
            BcSmartspaceCardMetadataLoggingInfo bcSmartspaceCardMetadataLoggingInfo =
                    subcards.get(i2);
            if (bcSmartspaceCardMetadataLoggingInfo != null
                    && bcSmartspaceCardMetadataLoggingInfo.getCardTypeId() == i) {
                return i2 + 1;
            }
        }
        return 0;
    }

    private int getSubcardType(SmartspaceAction smartspaceAction) {
        if (smartspaceAction == null
                || smartspaceAction.getExtras() == null
                || smartspaceAction.getExtras().isEmpty()) {
            return -1;
        }
        return smartspaceAction.getExtras().getInt("subcardType", -1);
    }

    public void setSecondaryCard(BcSmartspaceCardSecondary bcSmartspaceCardSecondary) {
        mSecondaryCard = bcSmartspaceCardSecondary;
        if (getChildAt(1) != null) {
            removeViewAt(1);
        }
        if (bcSmartspaceCardSecondary != null) {
            LinearLayout.LayoutParams layoutParams =
                    new LinearLayout.LayoutParams(
                            0,
                            getResources()
                                    .getDimensionPixelSize(R.dimen.enhanced_smartspace_height));
            layoutParams.weight = 3.0f;
            layoutParams.setMarginStart(
                    getResources()
                            .getDimensionPixelSize(
                                    R.dimen.enhanced_smartspace_secondary_card_start_margin));
            layoutParams.setMarginEnd(
                    getResources()
                            .getDimensionPixelSize(
                                    R.dimen.enhanced_smartspace_secondary_card_end_margin));
            addView(bcSmartspaceCardSecondary, 1, layoutParams);
        }
    }

    public void setDozeAmount(float f) {
        mDozeAmount = f;
        BcSmartspaceCardSecondary bcSmartspaceCardSecondary = mSecondaryCard;
        if (bcSmartspaceCardSecondary != null) {
            bcSmartspaceCardSecondary.setAlpha(1.0f - f);
        }
        if (getTarget() != null
                && getTarget().getBaseAction() != null
                && getTarget().getBaseAction().getExtras() != null) {
            Bundle extras = getTarget().getBaseAction().getExtras();
            if (mTitleTextView != null && extras.getBoolean("hide_title_on_aod")) {
                mTitleTextView.setAlpha(1.0f - f);
            }
            if (mSubtitleTextView != null && extras.getBoolean("hide_subtitle_on_aod")) {
                mSubtitleTextView.setAlpha(1.0f - f);
            }
        }
        ImageView imageView = mDndImageView;
        if (imageView != null) {
            imageView.setAlpha(mDozeAmount);
        }
    }

    public void setPrimaryTextColor(int i) {
        TextView textView = mTitleTextView;
        if (textView != null) {
            textView.setTextColor(i);
        }
        IcuDateTextView icuDateTextView = mDateView;
        if (icuDateTextView != null) {
            icuDateTextView.setTextColor(i);
        }
        TextView textView2 = mSubtitleTextView;
        if (textView2 != null) {
            textView2.setTextColor(i);
        }
        DoubleShadowTextView doubleShadowTextView = mBaseActionIconSubtitleView;
        if (doubleShadowTextView != null) {
            doubleShadowTextView.setTextColor(i);
        }
        mIconTintColor = i;
        updateZenColors();
        updateIconTint();
    }

    @Override
    public AccessibilityNodeInfo createAccessibilityNodeInfo() {
        AccessibilityNodeInfo createAccessibilityNodeInfo = super.createAccessibilityNodeInfo();
        AccessibilityNodeInfoCompat.wrap(createAccessibilityNodeInfo).setRoleDescription(" ");
        return createAccessibilityNodeInfo;
    }

    void setTitle(CharSequence charSequence, CharSequence charSequence2, boolean z) {
        boolean z2;
        TextView textView = mTitleTextView;
        if (textView == null) {
            Log.w("BcSmartspaceCard", "No title view to update");
            return;
        }
        textView.setText(charSequence);
        SmartspaceAction headerAction = mTarget.getHeaderAction();
        Bundle extras = headerAction == null ? null : headerAction.getExtras();
        if (extras != null && extras.containsKey("titleEllipsize")) {
            String string = extras.getString("titleEllipsize");
            try {
                mTitleTextView.setEllipsize(TextUtils.TruncateAt.valueOf(string));
            } catch (IllegalArgumentException unused) {
                Log.w("BcSmartspaceCard", "Invalid TruncateAt value: " + string);
            }
        } else if (mTarget.getFeatureType() == 2
                && Locale.ENGLISH
                        .getLanguage()
                        .equals(mContext.getResources().getConfiguration().locale.getLanguage())) {
            mTitleTextView.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        } else {
            mTitleTextView.setEllipsize(TextUtils.TruncateAt.END);
        }
        boolean z3 = false;
        if (extras != null) {
            int i = extras.getInt("titleMaxLines");
            if (i != 0) {
                mTitleTextView.setMaxLines(i);
            }
            z2 = extras.getBoolean("disableTitleIcon");
        } else {
            z2 = false;
        }
        if (z && !z2) {
            z3 = true;
        }
        if (z3) {
            setFormattedContentDescription(mTitleTextView, charSequence, charSequence2);
        }
        mTitleTextView.setCompoundDrawablesRelative(z3 ? mIconDrawable : null, null, null, null);
    }

    void setSubtitle(CharSequence charSequence, CharSequence charSequence2) {
        TextView textView = mSubtitleTextView;
        if (textView == null) {
            Log.w("BcSmartspaceCard", "No subtitle view to update");
            return;
        }
        textView.setText(charSequence);
        mSubtitleTextView.setCompoundDrawablesRelative(
                TextUtils.isEmpty(charSequence) ? null : mIconDrawable, null, null, null);
        mSubtitleTextView.setMaxLines(
                (mTarget.getFeatureType() != 5 || mUsePageIndicatorUi) ? 1 : 2);
        setFormattedContentDescription(mSubtitleTextView, charSequence, charSequence2);
    }

    void updateIconTint() {
        SmartspaceTarget smartspaceTarget = mTarget;
        if (smartspaceTarget == null || mIconDrawable == null) {
            return;
        }
        boolean z = true;
        if (smartspaceTarget.getFeatureType() == 1) {
            z = false;
        }
        if (z) {
            mIconDrawable.setTint(mIconTintColor);
        } else {
            mIconDrawable.setTintList(null);
        }
    }

    void updateZenColors() {
        TextView textView = mNextAlarmTextView;
        if (textView != null) {
            textView.setTextColor(mIconTintColor);
        }
        updateTint(mNextAlarmImageView);
        updateTint(mDndImageView);
    }

    private void updateTint(ImageView imageView) {
        if (imageView == null || imageView.getDrawable() == null) {
            return;
        }
        imageView.getDrawable().setTint(mIconTintColor);
    }

    public void setDnd(Drawable drawable, String str) {
        ImageView imageView = mDndImageView;
        if (imageView == null) {
            return;
        }
        if (drawable == null) {
            imageView.setVisibility(View.GONE);
        } else {
            imageView.setImageDrawable(
                    new DoubleShadowIconDrawable(drawable.mutate(), getContext()));
            mDndImageView.setContentDescription(str);
            mDndImageView.setVisibility(View.VISIBLE);
        }
        updateZenVisibility();
    }

    public void setNextAlarm(Drawable drawable, String str, SmartspaceTarget smartspaceTarget) {
        ImageView imageView = mNextAlarmImageView;
        if (imageView == null || mNextAlarmTextView == null) {
            return;
        }
        if (drawable == null) {
            imageView.setVisibility(View.GONE);
            mNextAlarmTextView.setVisibility(View.GONE);
        } else {
            String maybeAppendHolidayInfoToNextAlarm =
                    maybeAppendHolidayInfoToNextAlarm(str, smartspaceTarget);
            mNextAlarmImageView.setImageDrawable(
                    new DoubleShadowIconDrawable(drawable.mutate(), getContext()));
            mNextAlarmImageView.setVisibility(View.VISIBLE);
            mNextAlarmTextView.setContentDescription(
                    getContext()
                            .getString(
                                    R.string.accessibility_next_alarm,
                                    maybeAppendHolidayInfoToNextAlarm));
            mNextAlarmTextView.setText(maybeAppendHolidayInfoToNextAlarm);
            mNextAlarmTextView.setVisibility(View.VISIBLE);
            setNextAlarmClickListener(mNextAlarmImageView, smartspaceTarget);
            setNextAlarmClickListener(mNextAlarmTextView, smartspaceTarget);
        }
        updateZenVisibility();
    }

    private void setNextAlarmClickListener(View view, SmartspaceTarget smartspaceTarget) {
        BcSmartspaceCardLoggingInfo bcSmartspaceCardLoggingInfo;
        if (smartspaceTarget == null) {
            bcSmartspaceCardLoggingInfo =
                    new BcSmartspaceCardLoggingInfo.Builder()
                            .setInstanceId(InstanceId.create("upcoming_alarm_card_94510_12684"))
                            .setFeatureType(23)
                            .setDisplaySurface(
                                    BcSmartSpaceUtil.getLoggingDisplaySurface(
                                            getContext().getPackageName(), mDozeAmount))
                            .build();
        } else {
            bcSmartspaceCardLoggingInfo =
                    new BcSmartspaceCardLoggingInfo.Builder()
                            .setInstanceId(InstanceId.create(smartspaceTarget))
                            .setFeatureType(smartspaceTarget.getFeatureType())
                            .setDisplaySurface(
                                    BcSmartSpaceUtil.getLoggingDisplaySurface(
                                            getContext().getPackageName(), mDozeAmount))
                            .build();
        }
        BcSmartSpaceUtil.setOnClickListener(
                view,
                smartspaceTarget,
                SHOW_ALARMS_ACTION,
                "BcSmartspaceCard",
                mEventNotifier,
                bcSmartspaceCardLoggingInfo);
    }

    private String maybeAppendHolidayInfoToNextAlarm(
            String str, SmartspaceTarget smartspaceTarget) {
        CharSequence holidayAlarmsText = getHolidayAlarmsText(smartspaceTarget);
        if (!TextUtils.isEmpty(holidayAlarmsText)) {
            return str + " · " + ((Object) holidayAlarmsText);
        }
        return str;
    }

    public static CharSequence getHolidayAlarmsText(SmartspaceTarget smartspaceTarget) {
        SmartspaceAction headerAction;
        if (smartspaceTarget == null
                || (headerAction = smartspaceTarget.getHeaderAction()) == null) {
            return null;
        }
        return headerAction.getTitle();
    }

    private void updateZenVisibility() {
        if (mExtrasGroup == null) {
            return;
        }
        ImageView imageView = mDndImageView;
        boolean z = true;
        int i = 0;
        boolean z2 = imageView != null && imageView.getVisibility() == 0;
        ImageView imageView2 = mNextAlarmImageView;
        boolean z3 = imageView2 != null && imageView2.getVisibility() == 0;
        if ((!z2 && !z3) || (mUsePageIndicatorUi && mTarget.getFeatureType() != 1)) {
            z = false;
        }
        int i2 = mTopPadding;
        if (!z) {
            mExtrasGroup.setVisibility(View.INVISIBLE);
            i = i2;
        } else {
            mExtrasGroup.setVisibility(View.VISIBLE);
            updateZenColors();
        }
        setPadding(getPaddingLeft(), i, getPaddingRight(), getPaddingBottom());
    }

    public SmartspaceTarget getTarget() {
        return mTarget;
    }

    private void setFormattedContentDescription(
            TextView textView, CharSequence charSequence, CharSequence charSequence2) {
        if (TextUtils.isEmpty(charSequence)) {
            charSequence = charSequence2;
        } else if (!TextUtils.isEmpty(charSequence2)) {
            charSequence =
                    mContext.getString(
                            R.string.generic_smartspace_concatenated_desc,
                            charSequence2,
                            charSequence);
        }
        textView.setContentDescription(charSequence);
    }

    private boolean hasIntent(SmartspaceAction smartspaceAction) {
        return (smartspaceAction == null
                        || (smartspaceAction.getIntent() == null
                                && smartspaceAction.getPendingIntent() == null))
                ? false
                : true;
    }
}
