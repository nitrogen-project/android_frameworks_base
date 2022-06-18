package com.android.systemui.statusbar.policy;

import java.text.DecimalFormat;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.drawable.Drawable;
import android.graphics.PorterDuff.Mode;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.view.Gravity;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.os.Handler;
import android.os.UserHandle;
import android.os.Message;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.Spanned;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import com.android.systemui.R;

/*
*
* Seeing how an Integer object in java requires at least 16 Bytes, it seemed awfully wasteful
* to only use it for a single boolean. 32-bits is plenty of room for what we need it to do.
*
*/
public class NetworkTraffic extends TextView {

    private static final int INTERVAL = 1500; //ms
    private static final int KB = 1024;
    private static final int MB = KB * KB;
    private static final int GB = MB * KB;
    private static final String symbol = "/s";

    private final int mWidth;

    protected boolean mIsEnabled;
    private boolean mAttached;
    private long totalRxBytes;
    private long totalTxBytes;
    private long lastUpdateTime;
    private int mAutoHideThreshold;
    protected int mTintColor;

    private boolean mScreenOn = true;
    protected boolean mVisible = true;
    private ConnectivityManager mConnectivityManager;

    private Handler mTrafficHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            long timeDelta = SystemClock.elapsedRealtime() - lastUpdateTime;

            if (timeDelta < INTERVAL * .95) {
                if (msg.what != 1) {
                    // we just updated the view, nothing further to do
                    return;
                }
                if (timeDelta < 1) {
                    // Can't div by 0 so make sure the value displayed is minimal
                    timeDelta = Long.MAX_VALUE;
                }
            }
            lastUpdateTime = SystemClock.elapsedRealtime();

            // Calculate the data rate from the change in total bytes and time
            long newTotalRxBytes = TrafficStats.getTotalRxBytes();
            long newTotalTxBytes = TrafficStats.getTotalTxBytes();
            long rxData = newTotalRxBytes - totalRxBytes;
            long txData = newTotalTxBytes - totalTxBytes;

            if (shouldHide(rxData, txData, timeDelta)) {
                setText("");
                setVisibility(View.INVISIBLE);
                mVisible = false;
            } else if (shouldShowUpload(rxData, txData, timeDelta)) {
                // Show information for uplink if it's called for
                CharSequence output = formatOutput(timeDelta, txData, symbol);

                // Update view if there's anything new to show
                if (output != getText()) {
                    setText(output);
                }
                makeVisible();
            } else {
                // Add information for downlink if it's called for
                CharSequence output = formatOutput(timeDelta, rxData, symbol);

                // Update view if there's anything new to show
                if (output != getText()) {
                    setText(output);
                }
                makeVisible();
            }

            // Post delayed message to refresh in ~1000ms
            totalRxBytes = newTotalRxBytes;
            totalTxBytes = newTotalTxBytes;
            clearHandlerCallbacks();
            mTrafficHandler.postDelayed(mRunnable, INTERVAL);
        }

        private CharSequence formatOutput(long timeDelta, long data, String symbol) {
            long speed = (long)(data / (timeDelta / 1000F));

            return formatDecimal(speed);
        }

        private CharSequence formatDecimal(long speed) {
            DecimalFormat decimalFormat;
            String unit;
            String formatSpeed;
            SpannableString spanUnitString;
            SpannableString spanSpeedString;

            if (speed >= 1000 * MB) {
                unit = "GB";
                decimalFormat = new DecimalFormat("0.00");
                formatSpeed =  decimalFormat.format(speed / (float)GB);
            } else if (speed >= 100 * MB) {
                decimalFormat = new DecimalFormat("000");
                unit = "MB";
                formatSpeed =  decimalFormat.format(speed / (float)MB);
            } else if (speed >= 10 * MB) {
                decimalFormat = new DecimalFormat("00.0");
                unit = "MB";
                formatSpeed =  decimalFormat.format(speed / (float)MB);
            } else if (speed >= 1000 * KB) {
                decimalFormat = new DecimalFormat("0.00");
                unit = "MB";
                formatSpeed =  decimalFormat.format(speed / (float)MB);
            } else if (speed >= 100 * KB) {
                decimalFormat = new DecimalFormat("000");
                unit = "KB";
                formatSpeed =  decimalFormat.format(speed / (float)KB);
            } else if (speed >= 10 * KB) {
                decimalFormat = new DecimalFormat("00.0");
                unit = "KB";
                formatSpeed =  decimalFormat.format(speed / (float)KB);
            } else {
                decimalFormat = new DecimalFormat("0.00");
                unit = "KB";
                formatSpeed = decimalFormat.format(speed / (float)KB);
            }
            spanSpeedString = new SpannableString(formatSpeed);
            spanSpeedString.setSpan(getSpeedRelativeSizeSpan(), 0, (formatSpeed).length(),
                    Spanned.SPAN_INCLUSIVE_INCLUSIVE);

            spanUnitString = new SpannableString(unit + symbol);
            spanUnitString.setSpan(getUnitRelativeSizeSpan(), 0, (unit + symbol).length(),
                    Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            return TextUtils.concat(spanSpeedString, "\n", spanUnitString);
        }

        private boolean shouldHide(long rxData, long txData, long timeDelta) {
            long speedRxKB = (long)(rxData / (timeDelta / 1000f)) / KB;
            long speedTxKB = (long)(txData / (timeDelta / 1000f)) / KB;
            return !getConnectAvailable() ||
                    (speedRxKB < mAutoHideThreshold &&
                    speedTxKB < mAutoHideThreshold);
        }

        private boolean shouldShowUpload(long rxData, long txData, long timeDelta) {
            long speedRxKB = (long)(rxData / (timeDelta / 1000f)) / KB;
            long speedTxKB = (long)(txData / (timeDelta / 1000f)) / KB;

            return (speedTxKB > speedRxKB);
        }
    };

    protected boolean restoreViewQuickly() {
        return getConnectAvailable() && mAutoHideThreshold == 0;
    }

    protected void makeVisible() {
        setVisibility(View.VISIBLE);
        mVisible = true;
    }

    /*
     *  @hide
     */
    public NetworkTraffic(Context context) {
        this(context, null);
    }

    /*
     *  @hide
     */
    public NetworkTraffic(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /*
     *  @hide
     */
    public NetworkTraffic(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        final Resources resources = getResources();
        mTintColor = getCurrentTextColor();
        mWidth = resources.getDimensionPixelSize(R.dimen.network_traffic_width);
        setMode();
        Handler mHandler = new Handler();
        mConnectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        SettingsObserver settingsObserver = new SettingsObserver(mHandler);
        settingsObserver.observe();
        update();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!mAttached) {
            mAttached = true;
            IntentFilter filter = new IntentFilter();
            filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            filter.addAction(Intent.ACTION_SCREEN_ON);
            mContext.registerReceiver(mIntentReceiver, filter, null, getHandler());
        }
        update();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mAttached) {
            mContext.unregisterReceiver(mIntentReceiver);
            mAttached = false;
        }
    }

    protected RelativeSizeSpan getSpeedRelativeSizeSpan() {
        return new RelativeSizeSpan(0.80f);
    }

    protected RelativeSizeSpan getUnitRelativeSizeSpan() {
        return new RelativeSizeSpan(0.75f);
    }

    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            mTrafficHandler.sendEmptyMessage(0);
        }
    };

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System
                    .getUriFor(Settings.System.NETWORK_TRAFFIC_STATE), false,
                    this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System
                    .getUriFor(Settings.System.NETWORK_TRAFFIC_AUTOHIDE_THRESHOLD), false,
                    this, UserHandle.USER_ALL);
        }

        /*
         *  @hide
         */
        @Override
        public void onChange(boolean selfChange) {
            setMode();
            update();
        }
    }

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;
            if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION) && mScreenOn) {
                update();
            } else if (action.equals(Intent.ACTION_SCREEN_ON)) {
                mScreenOn = true;
                update();
            } else if (action.equals(Intent.ACTION_SCREEN_OFF)) {
                mScreenOn = false;
                clearHandlerCallbacks();
            }
        }
    };

    private boolean getConnectAvailable() {
        NetworkInfo network = (mConnectivityManager != null) ? mConnectivityManager.getActiveNetworkInfo() : null;
        return network != null;
    }

    protected void update() {
        if (mIsEnabled) {
            if (mAttached) {
                totalRxBytes = TrafficStats.getTotalRxBytes();
                totalTxBytes = TrafficStats.getTotalTxBytes();
                mTrafficHandler.sendEmptyMessage(1);
            }
            if (mAutoHideThreshold == 0)
                makeVisible();
            return;
        }
        clearHandlerCallbacks();
        setVisibility(View.GONE);
        mVisible = false;
    }

    protected void setMode() {
        ContentResolver resolver = mContext.getContentResolver();
        mIsEnabled = Settings.System.getIntForUser(resolver,
                Settings.System.NETWORK_TRAFFIC_STATE, 0,
                UserHandle.USER_CURRENT) == 1;
        mAutoHideThreshold = Settings.System.getIntForUser(resolver,
                Settings.System.NETWORK_TRAFFIC_AUTOHIDE_THRESHOLD, 1,
                UserHandle.USER_CURRENT);
        setGravity(Gravity.CENTER);
        setMaxLines(2);
        setSpacingAndFonts();
        updateTrafficDrawable();
        setWidth(mWidth);
    }

    private void clearHandlerCallbacks() {
        mTrafficHandler.removeCallbacks(mRunnable);
        mTrafficHandler.removeMessages(0);
        mTrafficHandler.removeMessages(1);
    }

    protected void updateTrafficDrawable() {
        setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        setTextColor(mTintColor);
    }

    protected void setSpacingAndFonts() {
        setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        setLineSpacing(0.80f, 0.80f);
    }

    public void onDensityOrFontScaleChanged() {
        setSpacingAndFonts();
        update();
    }

    public void setTintColor(int color) {
        mTintColor = color;
        updateTrafficDrawable();
    }
}
