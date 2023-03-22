/**
 * Copyright (C) 2019-2023 crDroid Android Project
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

package com.android.systemui.statusbar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.TrafficStats;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.text.Spanned;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.RelativeSizeSpan;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.TextView;

import android.provider.Settings;

import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.tuner.TunerService;

import java.text.DecimalFormat;
import java.util.HashMap;

public class NetworkTraffic extends TextView implements TunerService.Tunable {
    private static final String TAG = "NetworkTraffic";

    private static final int MODE_UPSTREAM_AND_DOWNSTREAM = 0;
    private static final int MODE_UPSTREAM_ONLY = 1;
    private static final int MODE_DOWNSTREAM_ONLY = 2;

    protected static final int LOCATION_DISABLED = 0;
    protected static final int LOCATION_STATUSBAR = 1;
    protected static final int LOCATION_QUICK_STATUSBAR = 2;

    private static final int MESSAGE_TYPE_PERIODIC_REFRESH = 0;
    private static final int MESSAGE_TYPE_UPDATE_VIEW = 1;
    private static final int MESSAGE_TYPE_ADD_NETWORK = 2;
    private static final int MESSAGE_TYPE_REMOVE_NETWORK = 3;

    private static final int Kilo = 1000;
    private static final int Mega = Kilo * Kilo;
    private static final int Giga = Mega * Kilo;

    private static final String NETWORK_TRAFFIC_LOCATION =
            Settings.Secure.NETWORK_TRAFFIC_LOCATION;
    private static final String NETWORK_TRAFFIC_MODE =
            Settings.Secure.NETWORK_TRAFFIC_MODE;
    private static final String NETWORK_TRAFFIC_AUTOHIDE =
            Settings.Secure.NETWORK_TRAFFIC_AUTOHIDE;
    private static final String NETWORK_TRAFFIC_AUTOHIDE_THRESHOLD =
            Settings.Secure.NETWORK_TRAFFIC_AUTOHIDE_THRESHOLD;
    private static final String NETWORK_TRAFFIC_UNITS =
            Settings.Secure.NETWORK_TRAFFIC_UNITS;
    private static final String NETWORK_TRAFFIC_REFRESH_INTERVAL =
            Settings.Secure.NETWORK_TRAFFIC_REFRESH_INTERVAL;
    private static final String NETWORK_TRAFFIC_HIDEARROW =
            Settings.Secure.NETWORK_TRAFFIC_HIDEARROW;

    protected int mLocation = LOCATION_DISABLED;
    private int mMode = MODE_UPSTREAM_AND_DOWNSTREAM;
    private int mSubMode = MODE_UPSTREAM_AND_DOWNSTREAM;
    protected boolean mIsActive;
    private boolean mTrafficActive;
    private long mTxBytes;
    private long mRxBytes;
    private long mLastTxBytes;
    private long mLastRxBytes;
    private long mLastUpdateTime;
    private boolean mAutoHide;
    private long mAutoHideThreshold;
    private int mUnits;
    protected int mIconTint = 0;
    protected int newTint = Color.WHITE;

    private Drawable mDrawable;

    private int mRefreshInterval = 2;

    private boolean mAttached;
    private boolean mHideArrows;

    protected boolean mVisible = true;

    private ConnectivityManager mConnectivityManager;
    private final Handler mTrafficHandler;

    private RelativeSizeSpan mSpeedRelativeSizeSpan = new RelativeSizeSpan(0.70f);
    private RelativeSizeSpan mUnitRelativeSizeSpan = new RelativeSizeSpan(0.65f);

    protected boolean mEnabled = false;
    private boolean mConnectionAvailable = true;
    private boolean mChipVisible;

    private final HashMap<Network, LinkProperties> mLinkPropertiesMap = new HashMap<>();
    // Used to indicate that the set of sources contributing
    // to current stats have changed.
    private boolean mNetworksChanged = true;

    public NetworkTraffic(Context context) {
        this(context, null);
    }

    public NetworkTraffic(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NetworkTraffic(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        mConnectivityManager =
                (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        mTrafficHandler = new Handler(mContext.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MESSAGE_TYPE_PERIODIC_REFRESH:
                        recalculateStats();
                        displayStatsAndReschedule();
                        break;

                    case MESSAGE_TYPE_UPDATE_VIEW:
                        displayStatsAndReschedule();
                        break;

                    case MESSAGE_TYPE_ADD_NETWORK:
                        final LinkPropertiesHolder lph = (LinkPropertiesHolder) msg.obj;
                        mLinkPropertiesMap.put(lph.getNetwork(), lph.getLinkProperties());
                        mNetworksChanged = true;
                        break;

                    case MESSAGE_TYPE_REMOVE_NETWORK:
                        mLinkPropertiesMap.remove((Network) msg.obj);
                        mNetworksChanged = true;
                        break;
                }
            }

            private void recalculateStats() {
                final long now = SystemClock.elapsedRealtime();
                long timeDelta = now - mLastUpdateTime; /* ms */

                if (timeDelta < mRefreshInterval * 1000 * 0.95f) {
                    return;
                }
                // Sum tx and rx bytes from all sources of interest
                long txBytes = 0;
                long rxBytes = 0;
                // Add interface stats
                for (LinkProperties linkProperties : mLinkPropertiesMap.values()) {
                    final String iface = linkProperties.getInterfaceName();
                    if (iface == null) {
                        continue;
                    }
                    final long ifaceTxBytes = TrafficStats.getTxBytes(iface);
                    final long ifaceRxBytes = TrafficStats.getRxBytes(iface);
                    txBytes += ifaceTxBytes;
                    rxBytes += ifaceRxBytes;
                }

                final long txBytesDelta = txBytes - mLastTxBytes;
                final long rxBytesDelta = rxBytes - mLastRxBytes;

                if (!mNetworksChanged && timeDelta > 0 && txBytesDelta >= 0 && rxBytesDelta >= 0) {
                    mTxBytes = (long) (txBytesDelta / (timeDelta / 1000f));
                    mRxBytes = (long) (rxBytesDelta / (timeDelta / 1000f));
                } else if (mNetworksChanged) {
                    mTxBytes = 0;
                    mRxBytes = 0;
                    mNetworksChanged = false;
                }
                mLastTxBytes = txBytes;
                mLastRxBytes = rxBytes;
                mLastUpdateTime = now;
            }

            private void displayStatsAndReschedule() {
                final boolean showUpstream =
                        mMode == MODE_UPSTREAM_ONLY || mMode == MODE_UPSTREAM_AND_DOWNSTREAM;
                final boolean showDownstream =
                        mMode == MODE_DOWNSTREAM_ONLY || mMode == MODE_UPSTREAM_AND_DOWNSTREAM;
                final boolean aboveThreshold = (showUpstream && mTxBytes > mAutoHideThreshold)
                        || (showDownstream && mRxBytes > mAutoHideThreshold);
                mIsActive = mAttached && mConnectionAvailable && (!mAutoHide || aboveThreshold);
                int submode = MODE_UPSTREAM_AND_DOWNSTREAM;
                final boolean trafficactive = (mTxBytes > 0 || mRxBytes > 0);

                clearHandlerCallbacks();

                if (mEnabled && mIsActive) {
                    CharSequence output = "";
                    if (showUpstream && showDownstream) {
                        if (mTxBytes > mRxBytes) {
                            output = formatOutput(mTxBytes);
                            submode = MODE_UPSTREAM_ONLY;
                        } else if (mTxBytes < mRxBytes) {
                            output = formatOutput(mRxBytes);
                            submode = MODE_DOWNSTREAM_ONLY;
                        } else {
                            output = formatOutput(mRxBytes);
                            submode = MODE_UPSTREAM_AND_DOWNSTREAM;
                        }
                    } else if (showDownstream) {
                        output = formatOutput(mRxBytes);
                    } else if (showUpstream) {
                        output = formatOutput(mTxBytes);
                    }

                    // Update view if there's anything new to show
                    if (output != getText()) {
                        setText(output);
                    }
                }

                updateVisibility();

                if (mVisible && (mSubMode != submode ||
                        mTrafficActive != trafficactive)) {
                    mSubMode = submode;
                    mTrafficActive = trafficactive;
                    setTrafficDrawable();
                }

                // Schedule periodic refresh
                if (mEnabled && mAttached) {
                    mTrafficHandler.sendEmptyMessageDelayed(MESSAGE_TYPE_PERIODIC_REFRESH,
                            mRefreshInterval * 1000);
                }
            }

            private CharSequence formatOutput(long speed) {
                DecimalFormat decimalFormat;
                String unit;
                String formatSpeed;
                SpannableString spanUnitString;
                SpannableString spanSpeedString;
                String gunit, munit, kunit;

                if (mUnits == 0) {
                    // speed is in bytes, convert to bits
                    speed = speed * 8;
                    gunit = mContext.getString(R.string.gigabitspersecond_short);
                    munit = mContext.getString(R.string.megabitspersecond_short);
                    kunit = mContext.getString(R.string.kilobitspersecond_short);
                } else {
                    gunit = mContext.getString(R.string.gigabytespersecond_short);
                    munit = mContext.getString(R.string.megabytespersecond_short);
                    kunit = mContext.getString(R.string.kilobytespersecond_short);
                }

                if (speed >= Giga) {
                    unit = gunit;
                    decimalFormat = new DecimalFormat("0.##");
                    formatSpeed = decimalFormat.format(speed / (float)Giga);
                } else if (speed >= 100 * Mega) {
                    decimalFormat = new DecimalFormat("##0");
                    unit = munit;
                    formatSpeed = decimalFormat.format(speed / (float)Mega);
                } else if (speed >= 10 * Mega) {
                    decimalFormat = new DecimalFormat("#0.#");
                    unit = munit;
                    formatSpeed = decimalFormat.format(speed / (float)Mega);
                } else if (speed >= Mega) {
                    decimalFormat = new DecimalFormat("0.##");
                    unit = munit;
                    formatSpeed = decimalFormat.format(speed / (float)Mega);
                } else if (speed >= 100 * Kilo) {
                    decimalFormat = new DecimalFormat("##0");
                    unit = kunit;
                    formatSpeed = decimalFormat.format(speed / (float)Kilo);
                } else if (speed >= 10 * Kilo) {
                    decimalFormat = new DecimalFormat("#0.#");
                    unit = kunit;
                    formatSpeed = decimalFormat.format(speed / (float)Kilo);
                } else {
                    decimalFormat = new DecimalFormat("0.##");
                    unit = kunit;
                    formatSpeed = decimalFormat.format(speed / (float)Kilo);
                }
                spanSpeedString = new SpannableString(formatSpeed);
                spanSpeedString.setSpan(mSpeedRelativeSizeSpan, 0, (formatSpeed).length(),
                        Spanned.SPAN_INCLUSIVE_INCLUSIVE);

                spanUnitString = new SpannableString(unit);
                spanUnitString.setSpan(mUnitRelativeSizeSpan, 0, (unit).length(),
                        Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                return TextUtils.concat(spanSpeedString, "\n", spanUnitString);
            }
        };
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (!mAttached) {
            mAttached = true;
            final TunerService tunerService = Dependency.get(TunerService.class);
            tunerService.addTunable(this, NETWORK_TRAFFIC_LOCATION);
            tunerService.addTunable(this, NETWORK_TRAFFIC_MODE);
            tunerService.addTunable(this, NETWORK_TRAFFIC_AUTOHIDE);
            tunerService.addTunable(this, NETWORK_TRAFFIC_AUTOHIDE_THRESHOLD);
            tunerService.addTunable(this, NETWORK_TRAFFIC_UNITS);
            tunerService.addTunable(this, NETWORK_TRAFFIC_REFRESH_INTERVAL);
            tunerService.addTunable(this, NETWORK_TRAFFIC_HIDEARROW);

            // Network tracking related variables
            final NetworkRequest request = new NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
                    .build();
            ConnectivityManager.NetworkCallback networkCallback =
                    new ConnectivityManager.NetworkCallback() {
                        @Override
                        public void onLinkPropertiesChanged(Network network,
                                LinkProperties linkProperties) {
                            Message msg = new Message();
                            msg.what = MESSAGE_TYPE_ADD_NETWORK;
                            msg.obj = new LinkPropertiesHolder(network, linkProperties);
                            mTrafficHandler.sendMessage(msg);
                        }

                        @Override
                        public void onLost(Network network) {
                            Message msg = new Message();
                            msg.what = MESSAGE_TYPE_REMOVE_NETWORK;
                            msg.obj = network;
                            mTrafficHandler.sendMessage(msg);
                        }
                    };
            ConnectivityManager.NetworkCallback defaultNetworkCallback =
                    new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network network) {
                    updateViews();
                }

                @Override
                public void onLost(Network network) {
                    updateViews();
                }
            };

            try {
                mConnectivityManager.registerNetworkCallback(request, networkCallback);
                mConnectivityManager.registerDefaultNetworkCallback(defaultNetworkCallback);
            } catch (Exception e) {
                // Do nothing
            }

            mConnectionAvailable = mConnectivityManager.getActiveNetworkInfo() != null;

            IntentFilter filter = new IntentFilter();
            filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            mContext.registerReceiver(mIntentReceiver, filter, null, mTrafficHandler);

            updateViews();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mAttached && !mEnabled) {
            clearHandlerCallbacks();
            mContext.unregisterReceiver(mIntentReceiver);
            Dependency.get(TunerService.class).removeTunable(this);
            mAttached = false;
        }
    }

    public void setChipVisibility(boolean enable) {
        if (mEnabled && mChipVisible != enable) {
            mChipVisible = enable;
            updateVisibility();
        }
    }

    protected void setEnabled() {
        mEnabled = mLocation == LOCATION_QUICK_STATUSBAR;
    }

    protected void updateVisibility() {
        boolean visible = mEnabled && mIsActive && getText() != ""
            && !mChipVisible;
        if (visible != mVisible) {
            mVisible = visible;
            setVisibility(mVisible ? VISIBLE : GONE);
        }
    }

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;
            if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                mConnectionAvailable = mConnectivityManager.getActiveNetworkInfo() != null;
                updateViews();
            }
        }
    };

    @Override
    public void onTuningChanged(String key, String newValue) {
        switch (key) {
            case NETWORK_TRAFFIC_LOCATION:
                mLocation =
                        TunerService.parseInteger(newValue, 0);
                setEnabled();
                if (mEnabled) {
                    setLines(2);
                    setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
                    setLineSpacing(0.80f, 0.80f);
                }
                updateViews();
                break;
            case NETWORK_TRAFFIC_MODE:
                mMode =
                        TunerService.parseInteger(newValue, 0);
                updateViews();
                setTrafficDrawable();
                break;
            case NETWORK_TRAFFIC_AUTOHIDE:
                mAutoHide =
                        TunerService.parseIntegerSwitch(newValue, false);
                updateViews();
                break;
            case NETWORK_TRAFFIC_AUTOHIDE_THRESHOLD:
                int autohidethreshold =
                        TunerService.parseInteger(newValue, 0);
                mAutoHideThreshold = autohidethreshold * Kilo; /* Convert kB to Bytes */
                updateViews();
                break;
            case NETWORK_TRAFFIC_UNITS:
                mUnits =
                        TunerService.parseInteger(newValue, 1);
                updateViews();
                break;
            case NETWORK_TRAFFIC_REFRESH_INTERVAL:
                mRefreshInterval =
                        TunerService.parseInteger(newValue, 2);
                updateViews();
                break;
            case NETWORK_TRAFFIC_HIDEARROW:
                mHideArrows =
                        TunerService.parseIntegerSwitch(newValue, false);
                if (!mHideArrows) {
                    setGravity(Gravity.END|Gravity.CENTER_VERTICAL);
                } else {
                    setGravity(Gravity.CENTER);
                }
                setTrafficDrawable();
                break;
            default:
                break;
        }
    }

    protected void updateViews() {
        if (mEnabled) {
            updateViewState();
        }
    }

    private void updateViewState() {
        mTrafficHandler.removeMessages(MESSAGE_TYPE_UPDATE_VIEW);
        mTrafficHandler.sendEmptyMessageDelayed(MESSAGE_TYPE_UPDATE_VIEW, 1000);
    }

    private void clearHandlerCallbacks() {
        mTrafficHandler.removeMessages(MESSAGE_TYPE_PERIODIC_REFRESH);
        mTrafficHandler.removeMessages(MESSAGE_TYPE_UPDATE_VIEW);
    }

    private void setTrafficDrawable() {
        final int drawableResId;
        final Drawable drawable;

        if (mHideArrows) {
            drawableResId = 0;
        } else if (!mTrafficActive) {
            drawableResId = R.drawable.stat_sys_network_traffic;
        } else if (mMode == MODE_UPSTREAM_ONLY || mSubMode == MODE_UPSTREAM_ONLY) {
            drawableResId = R.drawable.stat_sys_network_traffic_up;
        } else if (mMode == MODE_DOWNSTREAM_ONLY || mSubMode == MODE_DOWNSTREAM_ONLY) {
            drawableResId = R.drawable.stat_sys_network_traffic_down;
        } else if (mMode == MODE_UPSTREAM_AND_DOWNSTREAM) {
            drawableResId = R.drawable.stat_sys_network_traffic_updown;
        } else {
            drawableResId = 0;
        }
        drawable = drawableResId != 0 ? getResources().getDrawable(drawableResId) : null;
        if (mDrawable != drawable || mIconTint != newTint) {
            mDrawable = drawable;
            mIconTint = newTint;
            setCompoundDrawablesWithIntrinsicBounds(null, null, mDrawable, null);
            updateTrafficDrawable();
        }
    }

    public void setTint(int tint) {
        newTint = tint;
        // Wait for icon to be visible and tint to be changed
        if (mVisible && mIconTint != newTint) {
            mIconTint = newTint;
            updateTrafficDrawable();
        }
    }

    protected void updateTrafficDrawable() {
        if (mDrawable != null) {
            mDrawable.setColorFilter(mIconTint, PorterDuff.Mode.MULTIPLY);
        }
        setTextColor(mIconTint);
    }

    private static class LinkPropertiesHolder {
        private final Network mNetwork;
        private final LinkProperties mLinkProperties;

        public LinkPropertiesHolder(Network network, LinkProperties linkProperties) {
            mNetwork = network;
            mLinkProperties = linkProperties;
        }

        public Network getNetwork() {
            return mNetwork;
        }

        public LinkProperties getLinkProperties() {
            return mLinkProperties;
        }
    }
}
