package com.google.android.systemui.smartspace;

import android.app.smartspace.SmartspaceTarget;
import android.app.smartspace.SmartspaceTargetEvent;
import android.content.ComponentName;
import android.graphics.drawable.Drawable;
import android.os.Parcelable;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.viewpager.widget.PagerAdapter;

import com.android.internal.graphics.ColorUtils;
import com.android.launcher3.icons.GraphicsUtils;
import com.android.systemui.bcsmartspace.R;
import com.android.systemui.plugins.BcSmartspaceDataPlugin;

import com.google.android.systemui.smartspace.logging.BcSmartspaceCardLoggerUtil;
import com.google.android.systemui.smartspace.logging.BcSmartspaceCardLoggingInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class CardPagerAdapter extends PagerAdapter {
    private int mCurrentTextColor;
    private BcSmartspaceDataPlugin mDataProvider;
    private int mPrimaryTextColor;
    private final View mRoot;
    private List<SmartspaceTarget> mSmartspaceTargets = new ArrayList();
    private final List<SmartspaceTarget> mTargetsExcludingMediaAndHolidayAlarms = new ArrayList();
    private final List<SmartspaceTarget> mMediaTargets = new ArrayList();
    private boolean mHasOnlyDefaultDateCard = false;
    private final SparseArray<ViewHolder> mHolders = new SparseArray<>();
    private float mDozeAmount = 0.0f;
    private int mDozeColor = -1;
    private String mDndDescription = null;
    private Drawable mDndImage = null;
    private String mNextAlarmDescription = null;
    private Drawable mNextAlarmImage = null;
    private SmartspaceTarget mHolidayAlarmsTarget = null;

    public CardPagerAdapter(View view) {
        mRoot = view;
        int attrColor = GraphicsUtils.getAttrColor(view.getContext(), 16842806);
        mPrimaryTextColor = attrColor;
        mCurrentTextColor = attrColor;
    }

    @Override
    public int getCount() {
        return mSmartspaceTargets.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object obj) {
        return view == ((ViewHolder) obj).card;
    }

    @Override
    public void destroyItem(ViewGroup viewGroup, int i, Object obj) {
        ViewHolder viewHolder = (ViewHolder) obj;
        viewGroup.removeView(viewHolder.card);
        if (mHolders.get(i) == viewHolder) {
            mHolders.remove(i);
        }
    }

    public BcSmartspaceCard getCardAtPosition(int i) {
        ViewHolder viewHolder = mHolders.get(i);
        if (viewHolder == null) {
            return null;
        }
        return viewHolder.card;
    }

    public SmartspaceTarget getTargetAtPosition(int i) {
        if (mSmartspaceTargets.isEmpty() || i < 0 || i >= mSmartspaceTargets.size()) {
            return null;
        }
        return mSmartspaceTargets.get(i);
    }

    public List<SmartspaceTarget> getTargets() {
        return mSmartspaceTargets;
    }

    @Override
    public int getItemPosition(Object obj) {
        ViewHolder viewHolder = (ViewHolder) obj;
        SmartspaceTarget targetAtPosition = getTargetAtPosition(viewHolder.position);
        if (viewHolder.target == targetAtPosition) {
            return -1;
        }
        if (targetAtPosition == null
                || getFeatureType(targetAtPosition) != getFeatureType(viewHolder.target)
                || !Objects.equals(
                        targetAtPosition.getSmartspaceTargetId(),
                        viewHolder.target.getSmartspaceTargetId())) {
            return -2;
        }
        viewHolder.target = targetAtPosition;
        onBindViewHolder(viewHolder);
        return -1;
    }

    @Override
    public ViewHolder instantiateItem(ViewGroup viewGroup, int i) {
        SmartspaceTarget smartspaceTarget = mSmartspaceTargets.get(i);
        BcSmartspaceCard createBaseCard =
                createBaseCard(viewGroup, getFeatureType(smartspaceTarget));
        ViewHolder viewHolder = new ViewHolder(i, createBaseCard, smartspaceTarget);
        onBindViewHolder(viewHolder);
        viewGroup.addView(createBaseCard);
        mHolders.put(i, viewHolder);
        return viewHolder;
    }

    private int getFeatureType(SmartspaceTarget smartspaceTarget) {
        List actionChips = smartspaceTarget.getActionChips();
        int featureType = smartspaceTarget.getFeatureType();
        return (actionChips == null || actionChips.isEmpty())
                ? featureType
                : (featureType == 13 && actionChips.size() == 1) ? -2 : -1;
    }

    private BcSmartspaceCard createBaseCard(ViewGroup viewGroup, int i) {
        int i2;
        int i3;
        if (i == -2) {
            i2 = R.layout.smartspace_card_at_store;
        } else if (i == 1) {
            i2 = R.layout.smartspace_card_date;
        } else if (i == 20) {
            i2 = R.layout.smartspace_base_card_package_delivery;
        } else if (i == 30) {
            i2 = R.layout.smartspace_base_card_doorbell;
        } else {
            i2 = R.layout.smartspace_card;
        }
        LayoutInflater from = LayoutInflater.from(viewGroup.getContext());
        BcSmartspaceCard bcSmartspaceCard = (BcSmartspaceCard) from.inflate(i2, viewGroup, false);
        if (i == -2) {
            i3 = R.layout.smartspace_card_combination_at_store;
        } else if (i == -1) {
            i3 = R.layout.smartspace_card_combination;
        } else {
            if (i != 3) {
                if (i == 4) {
                    i3 = R.layout.smartspace_card_flight;
                } else if (i == 9) {
                    i3 = R.layout.smartspace_card_sports;
                } else if (i == 10) {
                    i3 = R.layout.smartspace_card_weather_forecast;
                } else if (i == 13) {
                    i3 = R.layout.smartspace_card_shopping_list;
                } else if (i == 14) {
                    i3 = R.layout.smartspace_card_loyalty;
                } else if (i != 18) {
                    i3 = (i == 20 || i == 30) ? R.layout.smartspace_card_doorbell : 0;
                }
            }
            i3 = R.layout.smartspace_card_generic_landscape_image;
        }
        if (i3 != 0) {
            bcSmartspaceCard.setSecondaryCard(
                    (BcSmartspaceCardSecondary)
                            from.inflate(i3, (ViewGroup) bcSmartspaceCard, false));
        }
        return bcSmartspaceCard;
    }

    private void onBindViewHolder(ViewHolder viewHolder) {
        BcSmartspaceDataPlugin.SmartspaceEventNotifier smartspaceEventNotifier;
        SmartspaceTarget smartspaceTarget = mSmartspaceTargets.get(viewHolder.position);
        BcSmartspaceCard bcSmartspaceCard = viewHolder.card;
        BcSmartspaceCardLoggingInfo build =
                new BcSmartspaceCardLoggingInfo.Builder()
                        .setInstanceId(InstanceId.create(smartspaceTarget))
                        .setFeatureType(smartspaceTarget.getFeatureType())
                        .setDisplaySurface(
                                BcSmartSpaceUtil.getLoggingDisplaySurface(
                                        mRoot.getContext().getPackageName(), mDozeAmount))
                        .setRank(viewHolder.position)
                        .setCardinality(mSmartspaceTargets.size())
                        .setSubcardInfo(
                                BcSmartspaceCardLoggerUtil.createSubcardLoggingInfo(
                                        smartspaceTarget))
                        .build();
        final BcSmartspaceDataPlugin bcSmartspaceDataPlugin = mDataProvider;
        if (bcSmartspaceDataPlugin == null) {
            smartspaceEventNotifier = null;
        } else {
            smartspaceEventNotifier = (smartspaceTargetEvent) -> {
                    bcSmartspaceDataPlugin.notifySmartspaceEvent(smartspaceTargetEvent);
            };
        }
        bcSmartspaceCard.setEventNotifier(smartspaceEventNotifier);
        BcSmartspaceCardLoggerUtil.forcePrimaryFeatureTypeAndInjectWeatherSubcard(
                build, smartspaceTarget, 39);
        boolean z = true;
        if (mSmartspaceTargets.size() <= 1) {
            z = false;
        }
        bcSmartspaceCard.setSmartspaceTarget(smartspaceTarget, build, z);
        bcSmartspaceCard.setPrimaryTextColor(mCurrentTextColor);
        bcSmartspaceCard.setDozeAmount(mDozeAmount);
        bcSmartspaceCard.setDnd(mDndImage, mDndDescription);
        bcSmartspaceCard.setNextAlarm(mNextAlarmImage, mNextAlarmDescription, mHolidayAlarmsTarget);
    }

    private boolean isHolidayAlarmsTarget(SmartspaceTarget smartspaceTarget) {
        return smartspaceTarget.getFeatureType() == 34;
    }

    public void setTargets(List<? extends Parcelable> list) {
        mTargetsExcludingMediaAndHolidayAlarms.clear();
        mHolidayAlarmsTarget = null;
        list.forEach(
                new Consumer() {
                    @Override
                    public final void accept(Object obj) {
                        SmartspaceTarget smartspaceTarget = (SmartspaceTarget) (Parcelable) obj;
                        if (isHolidayAlarmsTarget(smartspaceTarget)) {
                            mHolidayAlarmsTarget = smartspaceTarget;
                        } else {
                            mTargetsExcludingMediaAndHolidayAlarms.add(smartspaceTarget);
                        }
                    }
                });
        boolean z = true;
        if (mTargetsExcludingMediaAndHolidayAlarms.isEmpty()) {
            mTargetsExcludingMediaAndHolidayAlarms.add(
                    new SmartspaceTarget.Builder(
                                    "date_card_794317_92634",
                                    new ComponentName(mRoot.getContext(), CardPagerAdapter.class),
                                    mRoot.getContext().getUser())
                            .setFeatureType(1)
                            .build());
        }
        if (mTargetsExcludingMediaAndHolidayAlarms.size() != 1
                || mTargetsExcludingMediaAndHolidayAlarms.get(0).getFeatureType() != 1) {
            z = false;
        }
        mHasOnlyDefaultDateCard = z;
        updateTargetVisibility();
        notifyDataSetChanged();
    }

    public void setDataProvider(BcSmartspaceDataPlugin bcSmartspaceDataPlugin) {
        mDataProvider = bcSmartspaceDataPlugin;
    }

    public void setPrimaryTextColor(int i) {
        mPrimaryTextColor = i;
        setDozeAmount(mDozeAmount);
    }

    public void setDozeAmount(float f) {
        mCurrentTextColor = ColorUtils.blendARGB(mPrimaryTextColor, mDozeColor, f);
        mDozeAmount = f;
        updateTargetVisibility();
        refreshCardColors();
    }

    public float getDozeAmount() {
        return mDozeAmount;
    }

    public void setDnd(Drawable drawable, String str) {
        mDndImage = drawable;
        mDndDescription = str;
        refreshCards();
    }

    public void setNextAlarm(Drawable drawable, String str) {
        mNextAlarmImage = drawable;
        mNextAlarmDescription = str;
        refreshCards();
    }

    public void setMediaTarget(SmartspaceTarget smartspaceTarget) {
        mMediaTargets.clear();
        if (smartspaceTarget != null) {
            mMediaTargets.add(smartspaceTarget);
        }
        updateTargetVisibility();
    }

    public Drawable getNextAlarmImage() {
        return mNextAlarmImage;
    }

    public SmartspaceTarget getHolidayAlarmsTarget() {
        return mHolidayAlarmsTarget;
    }

    private void refreshCards() {
        for (int i = 0; i < mHolders.size(); i++) {
            onBindViewHolder(mHolders.get(i));
        }
    }

    private void refreshCardColors() {
        for (int i = 0; i < mHolders.size(); i++) {
            mHolders.get(i).card.setPrimaryTextColor(mCurrentTextColor);
            mHolders.get(i).card.setDozeAmount(mDozeAmount);
        }
    }

    private void updateTargetVisibility() {
        boolean z;
        if (mMediaTargets.isEmpty()) {
            mSmartspaceTargets = mTargetsExcludingMediaAndHolidayAlarms;
            notifyDataSetChanged();
            return;
        }
        float f = mDozeAmount;
        if (f == 0.0f || !(z = mHasOnlyDefaultDateCard)) {
            mSmartspaceTargets = mTargetsExcludingMediaAndHolidayAlarms;
            notifyDataSetChanged();
        } else if (f != 1.0f || !z) {
        } else {
            mSmartspaceTargets = mMediaTargets;
            notifyDataSetChanged();
        }
    }

    public static class ViewHolder {
        public final BcSmartspaceCard card;
        public final int position;
        public SmartspaceTarget target;

        ViewHolder(int i, BcSmartspaceCard bcSmartspaceCard, SmartspaceTarget smartspaceTarget) {
            position = i;
            card = bcSmartspaceCard;
            target = smartspaceTarget;
        }
    }
}
