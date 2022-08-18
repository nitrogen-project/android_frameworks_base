package com.google.android.systemui.smartspace;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.smartspace.SmartspaceAction;
import android.app.smartspace.SmartspaceTarget;
import android.app.smartspace.SmartspaceTargetEvent;
import android.content.ComponentName;
import android.content.Context;
import android.database.ContentObserver;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Parcelable;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.viewpager.widget.ViewPager;

import com.android.systemui.bcsmartspace.R;
import com.android.systemui.plugins.BcSmartspaceDataPlugin;
import com.android.systemui.plugins.FalsingManager;

import com.google.android.systemui.smartspace.logging.BcSmartspaceCardLogger;
import com.google.android.systemui.smartspace.logging.BcSmartspaceCardLoggerUtil;
import com.google.android.systemui.smartspace.logging.BcSmartspaceCardLoggingInfo;

import java.time.DateTimeException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class BcSmartspaceView extends FrameLayout
        implements BcSmartspaceDataPlugin.SmartspaceTargetListener,
                BcSmartspaceDataPlugin.SmartspaceView {
    private static ArraySet<String> mLastReceivedTargets = new ArraySet<>();
    private static int sLastSurface = -1;
    private BcSmartspaceDataPlugin mDataProvider;
    private PageIndicator mPageIndicator;
    private List<? extends Parcelable> mPendingTargets;
    private Animator mRunningAnimation;
    private ViewPager mViewPager;
    private boolean mIsAodEnabled = false;
    private int mCardPosition = 0;
    private boolean mAnimateSmartspaceUpdate = false;
    private int mScrollState = 0;
    private final ContentObserver mAodObserver =
            new ContentObserver(new Handler()) {
                @Override
                public void onChange(boolean z) {
                    onSettingsChanged();
                }
            };
    private final CardPagerAdapter mAdapter = new CardPagerAdapter(this);
    private final ViewPager.OnPageChangeListener mOnPageChangeListener =
            new ViewPager.OnPageChangeListener() {
                private int mCurrentPosition = -1;

                @Override
                public void onPageScrollStateChanged(int i) {
                    mScrollState = i;
                    if (i != 0 || mPendingTargets == null) {
                        return;
                    }
                    BcSmartspaceView bcSmartspaceView = BcSmartspaceView.this;
                    onSmartspaceTargetsUpdated(mPendingTargets);
                    mPendingTargets = null;
                }

                @Override
                public void onPageScrolled(int i, float f, int i2) {
                    if (mPageIndicator != null) {
                        mPageIndicator.setPageOffset(i, f);
                    }
                }

                @Override
                public void onPageSelected(int i) {
                    mCardPosition = i;
                    BcSmartspaceCard cardAtPosition = mAdapter.getCardAtPosition(mCurrentPosition);
                    mCurrentPosition = i;
                    SmartspaceTarget targetAtPosition =
                            mAdapter.getTargetAtPosition(mCurrentPosition);
                    logSmartspaceEvent(
                            targetAtPosition,
                            mCurrentPosition,
                            BcSmartspaceEvent.SMARTSPACE_CARD_SEEN);
                    if (mDataProvider == null) {
                        Log.w(
                                "BcSmartspaceView",
                                "Cannot notify target hidden/shown smartspace events: data provider"
                                        + " null");
                        return;
                    }
                    if (cardAtPosition == null) {
                        Log.w(
                                "BcSmartspaceView",
                                "Cannot notify target hidden smartspace event: hidden card null.");
                    } else {
                        SmartspaceTarget target = cardAtPosition.getTarget();
                        if (target == null) {
                            Log.w(
                                    "BcSmartspaceView",
                                    "Cannot notify target hidden smartspace event: hidden card"
                                            + " smartspace target null.");
                        } else {
                            SmartspaceTargetEvent.Builder builder =
                                    new SmartspaceTargetEvent.Builder(3);
                            builder.setSmartspaceTarget(target);
                            SmartspaceAction baseAction = target.getBaseAction();
                            if (baseAction != null) {
                                builder.setSmartspaceActionId(baseAction.getId());
                            }
                            mDataProvider.notifySmartspaceEvent(builder.build());
                        }
                    }
                    if (targetAtPosition == null) {
                        Log.w(
                                "BcSmartspaceView",
                                "Cannot notify target shown smartspace event: shown card smartspace"
                                        + " target null.");
                        return;
                    }
                    SmartspaceTargetEvent.Builder builder2 = new SmartspaceTargetEvent.Builder(2);
                    builder2.setSmartspaceTarget(targetAtPosition);
                    SmartspaceAction baseAction2 = targetAtPosition.getBaseAction();
                    if (baseAction2 != null) {
                        builder2.setSmartspaceActionId(baseAction2.getId());
                    }
                    mDataProvider.notifySmartspaceEvent(builder2.build());
                }
            };

    public BcSmartspaceView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override
    public void onVisibilityAggregated(boolean z) {
        super.onVisibilityAggregated(z);
        BcSmartspaceDataPlugin bcSmartspaceDataPlugin = mDataProvider;
        if (bcSmartspaceDataPlugin != null) {
            bcSmartspaceDataPlugin.notifySmartspaceEvent(
                    new SmartspaceTargetEvent.Builder(z ? 6 : 7).build());
        }
    }

    public void logCurrentDisplayedCardSeen() {
        SmartspaceTarget targetAtPosition = mAdapter.getTargetAtPosition(mCardPosition);
        if (targetAtPosition == null) {
            Log.w("BcSmartspaceView", "Current card is not present in the Adapter; cannot log.");
        } else {
            logSmartspaceEvent(
                    targetAtPosition, mCardPosition, BcSmartspaceEvent.SMARTSPACE_CARD_SEEN);
        }
        if (mAdapter.getNextAlarmImage() != null) {
            SmartspaceTarget createUpcomingAlarmTarget =
                    BcSmartSpaceUtil.createUpcomingAlarmTarget(
                            new ComponentName(getContext(), getClass()), getContext().getUser());
            BcSmartspaceEvent bcSmartspaceEvent = BcSmartspaceEvent.SMARTSPACE_CARD_SEEN;
            logSmartspaceEvent(createUpcomingAlarmTarget, 0, bcSmartspaceEvent);
            SmartspaceTarget holidayAlarmsTarget = mAdapter.getHolidayAlarmsTarget();
            if (TextUtils.isEmpty(BcSmartspaceCard.getHolidayAlarmsText(holidayAlarmsTarget))) {
                return;
            }
            logSmartspaceEvent(holidayAlarmsTarget, 0, bcSmartspaceEvent);
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mViewPager = (ViewPager) findViewById(R.id.smartspace_card_pager);
        mPageIndicator = (PageIndicator) findViewById(R.id.smartspace_page_indicator);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mViewPager.setAdapter(mAdapter);
        mViewPager.addOnPageChangeListener(mOnPageChangeListener);
        mPageIndicator.setNumPages(mAdapter.getCount());
        try {
            getContext()
                    .getContentResolver()
                    .registerContentObserver(
                            Settings.Secure.getUriFor("doze_always_on"), false, mAodObserver, -1);
            mIsAodEnabled = isAodEnabled(getContext());
        } catch (Exception e) {
            Log.w("BcSmartspaceView", "Unable to register Doze Always on content observer.", e);
        }
        BcSmartspaceDataPlugin bcSmartspaceDataPlugin = mDataProvider;
        if (bcSmartspaceDataPlugin != null) {
            registerDataProvider(bcSmartspaceDataPlugin);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        getContext().getContentResolver().unregisterContentObserver(mAodObserver);
        BcSmartspaceDataPlugin bcSmartspaceDataPlugin = mDataProvider;
        if (bcSmartspaceDataPlugin != null) {
            bcSmartspaceDataPlugin.unregisterListener(this);
        }
    }

    @Override
    protected void onMeasure(int i, int i2) {
        int size = View.MeasureSpec.getSize(i2);
        int dimensionPixelSize =
                getContext()
                        .getResources()
                        .getDimensionPixelSize(R.dimen.enhanced_smartspace_height);
        if (size > 0 && size < dimensionPixelSize) {
            float f = size;
            float f2 = dimensionPixelSize;
            float f3 = f / f2;
            super.onMeasure(
                    View.MeasureSpec.makeMeasureSpec(
                            Math.round(View.MeasureSpec.getSize(i) / f3), 1073741824),
                    View.MeasureSpec.makeMeasureSpec(dimensionPixelSize, 1073741824));
            setScaleX(f3);
            setScaleY(f3);
            setPivotX(0.0f);
            setPivotY(f2 / 2.0f);
            return;
        }
        super.onMeasure(i, i2);
        setScaleX(1.0f);
        setScaleY(1.0f);
        resetPivot();
    }

    @Override
    public void registerDataProvider(BcSmartspaceDataPlugin bcSmartspaceDataPlugin) {
        mDataProvider = bcSmartspaceDataPlugin;
        bcSmartspaceDataPlugin.registerListener(this);
        mAdapter.setDataProvider(mDataProvider);
    }

    @Override
    public void onSmartspaceTargetsUpdated(List<? extends Parcelable> list) {
        int i;
        boolean z = true;
        if (mScrollState != 0 && mAdapter.getCount() > 1) {
            mPendingTargets = list;
            return;
        }
        if (getLayoutDirection() != 1) {
            z = false;
        }
        int currentItem = mViewPager.getCurrentItem();
        if (z) {
            i = mAdapter.getCount() - currentItem;
            ArrayList arrayList = new ArrayList(list);
            Collections.reverse(arrayList);
            list = arrayList;
        } else {
            i = currentItem;
        }
        BcSmartspaceCard cardAtPosition = mAdapter.getCardAtPosition(currentItem);
        mAdapter.setTargets(list);
        int count = mAdapter.getCount();
        if (z) {
            mViewPager.setCurrentItem(Math.max(0, Math.min(count - 1, count - i)), false);
        }
        PageIndicator pageIndicator = mPageIndicator;
        if (pageIndicator != null) {
            pageIndicator.setNumPages(count);
        }
        if (mAnimateSmartspaceUpdate) {
            animateSmartspaceUpdate(cardAtPosition);
        }
        for (int i2 = 0; i2 < count; i2++) {
            SmartspaceTarget targetAtPosition = mAdapter.getTargetAtPosition(i2);
            if (!mLastReceivedTargets.contains(targetAtPosition.getSmartspaceTargetId())) {
                logSmartspaceEvent(
                        targetAtPosition, i2, BcSmartspaceEvent.SMARTSPACE_CARD_RECEIVED);
                SmartspaceTargetEvent.Builder builder = new SmartspaceTargetEvent.Builder(8);
                builder.setSmartspaceTarget(targetAtPosition);
                SmartspaceAction baseAction = targetAtPosition.getBaseAction();
                if (baseAction != null) {
                    builder.setSmartspaceActionId(baseAction.getId());
                }
                mDataProvider.notifySmartspaceEvent(builder.build());
            }
        }
        mLastReceivedTargets.clear();
        mLastReceivedTargets.addAll(
                (Collection)
                        mAdapter.getTargets().stream()
                                .map(smartspaceTarget -> smartspaceTarget.getSmartspaceTargetId())
                                .collect(Collectors.toList()));
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public final int getCurrentCardTopPadding() {
        BcSmartspaceCard card = mAdapter.getCardAtPosition(getSelectedPage());
        if (card != null) {
            return card.getPaddingTop();
        }
        return 0;
    }

    @Override
    public final int getSelectedPage() {
        return mViewPager.getCurrentItem();
    }

    public void logSmartspaceEvent(
            SmartspaceTarget smartspaceTarget, int i, BcSmartspaceEvent bcSmartspaceEvent) {
        int i2;
        if (bcSmartspaceEvent == BcSmartspaceEvent.SMARTSPACE_CARD_RECEIVED) {
            try {
                i2 =
                        (int)
                                Instant.now()
                                        .minusMillis(smartspaceTarget.getCreationTimeMillis())
                                        .toEpochMilli();
            } catch (ArithmeticException | DateTimeException e) {
                Log.e(
                        "BcSmartspaceView",
                        "received_latency_millis will be -1 due to exception ",
                        e);
                i2 = -1;
            }
        } else {
            i2 = 0;
        }
        BcSmartspaceCardLoggingInfo build =
                new BcSmartspaceCardLoggingInfo.Builder()
                        .setInstanceId(InstanceId.create(smartspaceTarget))
                        .setFeatureType(smartspaceTarget.getFeatureType())
                        .setDisplaySurface(
                                BcSmartSpaceUtil.getLoggingDisplaySurface(
                                        getContext().getPackageName(), mAdapter.getDozeAmount()))
                        .setRank(i)
                        .setCardinality(mAdapter.getCount())
                        .setReceivedLatency(i2)
                        .setSubcardInfo(
                                BcSmartspaceCardLoggerUtil.createSubcardLoggingInfo(
                                        smartspaceTarget))
                        .build();
        BcSmartspaceCardLoggerUtil.forcePrimaryFeatureTypeAndInjectWeatherSubcard(
                build, smartspaceTarget, 39);
        BcSmartspaceCardLogger.log(bcSmartspaceEvent, build);
    }

    private void animateSmartspaceUpdate(final BcSmartspaceCard bcSmartspaceCard) {
        if (bcSmartspaceCard != null
                && mRunningAnimation == null
                && bcSmartspaceCard.getParent() == null) {
            final ViewGroup viewGroup = (ViewGroup) mViewPager.getParent();
            bcSmartspaceCard.measure(
                    View.MeasureSpec.makeMeasureSpec(mViewPager.getWidth(), 1073741824),
                    View.MeasureSpec.makeMeasureSpec(mViewPager.getHeight(), 1073741824));
            bcSmartspaceCard.layout(
                    mViewPager.getLeft(),
                    mViewPager.getTop(),
                    mViewPager.getRight(),
                    mViewPager.getBottom());
            AnimatorSet animatorSet = new AnimatorSet();
            float dimension =
                    getContext()
                            .getResources()
                            .getDimension(R.dimen.enhanced_smartspace_dismiss_margin);
            animatorSet.play(
                    ObjectAnimator.ofFloat(
                            bcSmartspaceCard,
                            View.TRANSLATION_Y,
                            0.0f,
                            (-getHeight()) - dimension));
            animatorSet.play(ObjectAnimator.ofFloat(bcSmartspaceCard, View.ALPHA, 1.0f, 0.0f));
            animatorSet.play(
                    ObjectAnimator.ofFloat(
                            mViewPager, View.TRANSLATION_Y, getHeight() + dimension, 0.0f));
            animatorSet.addListener(
                    new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationStart(Animator animator) {
                            viewGroup.getOverlay().add(bcSmartspaceCard);
                        }

                        @Override
                        public void onAnimationEnd(Animator animator) {
                            viewGroup.getOverlay().remove(bcSmartspaceCard);
                            mRunningAnimation = null;
                            mAnimateSmartspaceUpdate = false;
                        }
                    });
            mRunningAnimation = animatorSet;
            animatorSet.start();
        }
    }

    @Override
    public void setPrimaryTextColor(int i) {
        mAdapter.setPrimaryTextColor(i);
        mPageIndicator.setPrimaryColor(i);
    }

    @Override
    public void setDozeAmount(float f) {
        mPageIndicator.setAlpha(1.0f - f);
        mAdapter.setDozeAmount(f);
        int loggingDisplaySurface =
                BcSmartSpaceUtil.getLoggingDisplaySurface(
                        getContext().getPackageName(), mAdapter.getDozeAmount());
        if (loggingDisplaySurface == -1 || loggingDisplaySurface == sLastSurface) {
            return;
        }
        sLastSurface = loggingDisplaySurface;
        if (loggingDisplaySurface == 3 && !mIsAodEnabled) {
            return;
        }
        logCurrentDisplayedCardSeen();
    }

    @Override
    public void setIsDreaming(boolean z) {
        mAdapter.setIsDreaming(z);
    }

    @Override
    public void setIntentStarter(BcSmartspaceDataPlugin.IntentStarter intentStarter) {
        BcSmartSpaceUtil.setIntentStarter(intentStarter);
    }

    @Override
    public void setFalsingManager(FalsingManager falsingManager) {
        BcSmartSpaceUtil.setFalsingManager(falsingManager);
    }

    @Override
    public void setDnd(Drawable drawable, String str) {
        mAdapter.setDnd(drawable, str);
    }

    @Override
    public void setNextAlarm(Drawable drawable, String str) {
        mAdapter.setNextAlarm(drawable, str);
    }

    @Override
    public void setMediaTarget(SmartspaceTarget smartspaceTarget) {
        mAdapter.setMediaTarget(smartspaceTarget);
    }

    @Override
    public void setOnLongClickListener(View.OnLongClickListener onLongClickListener) {
        mViewPager.setOnLongClickListener(onLongClickListener);
    }

    public void onSettingsChanged() {
        mIsAodEnabled = isAodEnabled(getContext());
    }

    private static boolean isAodEnabled(Context context) {
        return Settings.Secure.getIntForUser(
                        context.getContentResolver(), "doze_always_on", 0, context.getUserId())
                == 1;
    }
}
