package com.google.android.systemui.smartspace;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.icu.text.DateFormat;
import android.icu.text.DisplayContext;
import android.os.Handler;
import android.os.SystemClock;
import android.util.AttributeSet;

import com.android.systemui.bcsmartspace.R;

import java.util.Locale;
import java.util.Objects;

public class IcuDateTextView extends DoubleShadowTextView {
    private DateFormat mFormatter;
    private Handler mHandler;
    private final BroadcastReceiver mIntentReceiver;
    private String mText;
    private final Runnable mTicker;

    public IcuDateTextView(Context context) {
        this(context, null);
    }

    public IcuDateTextView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet, 0);
        mTicker =
                new Runnable() {
                    @Override
                    public final void run() {
                        onTimeTick();
                    }
                };
        mIntentReceiver =
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context2, Intent intent) {
                        onTimeChanged(
                                !"android.intent.action.TIME_TICK".equals(intent.getAction()));
                    }
                };
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.TIME_SET");
        intentFilter.addAction("android.intent.action.TIMEZONE_CHANGED");
        getContext().registerReceiver(mIntentReceiver, intentFilter);
        onTimeChanged(true);
        mHandler = new Handler();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mHandler != null) {
            getContext().unregisterReceiver(mIntentReceiver);
            mHandler = null;
        }
    }

    public void onTimeTick() {
        onTimeChanged(false);
        if (mHandler != null) {
            long uptimeMillis = SystemClock.uptimeMillis();
            mHandler.postAtTime(mTicker, uptimeMillis + (1000 - (uptimeMillis % 1000)));
        }
    }

    @Override
    public void onVisibilityAggregated(boolean z) {
        super.onVisibilityAggregated(z);
        Handler handler = mHandler;
        if (handler != null) {
            handler.removeCallbacks(mTicker);
            if (!z) {
                return;
            }
            mTicker.run();
        }
    }

    public void onTimeChanged(boolean z) {
        if (!isShown()) {
            return;
        }
        if (mFormatter == null || z) {
            DateFormat instanceForSkeleton =
                    DateFormat.getInstanceForSkeleton(
                            getContext().getString(R.string.smartspace_icu_date_pattern),
                            Locale.getDefault());
            mFormatter = instanceForSkeleton;
            instanceForSkeleton.setContext(DisplayContext.CAPITALIZATION_FOR_BEGINNING_OF_SENTENCE);
        }
        String format = mFormatter.format(Long.valueOf(System.currentTimeMillis()));
        if (Objects.equals(mText, format)) {
            return;
        }
        mText = format;
        setText(format);
        setContentDescription(format);
    }
}
