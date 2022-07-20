package com.google.android.systemui.smartspace;

import android.app.smartspace.SmartspaceAction;
import android.app.smartspace.SmartspaceTarget;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.Constraints;

import com.android.systemui.bcsmartspace.R;
import com.android.systemui.plugins.BcSmartspaceDataPlugin;

import com.google.android.systemui.smartspace.logging.BcSmartspaceCardLoggingInfo;

import java.util.Locale;

public class BcSmartspaceCardWeatherForecast extends BcSmartspaceCardSecondary {
    public BcSmartspaceCardWeatherForecast(Context context) {
        super(context);
    }

    public BcSmartspaceCardWeatherForecast(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        createWeatherForecastColumns();
    }

    @Override
    public boolean setSmartspaceActions(
            SmartspaceTarget smartspaceTarget,
            BcSmartspaceDataPlugin.SmartspaceEventNotifier smartspaceEventNotifier,
            BcSmartspaceCardLoggingInfo bcSmartspaceCardLoggingInfo) {
        SmartspaceAction baseAction = smartspaceTarget.getBaseAction();
        Bundle extras = baseAction == null ? null : baseAction.getExtras();
        boolean z = false;
        if (extras != null) {
            if (extras.containsKey("temperatureValues")) {
                setTemperatureValues(extras.getStringArray("temperatureValues"));
                z = true;
            }
            if (extras.containsKey("weatherIcons")) {
                setWeatherIcons((Bitmap[]) extras.get("weatherIcons"));
                z = true;
            }
            if (!extras.containsKey("timestamps")) {
                return z;
            }
            setTimestamps(extras.getStringArray("timestamps"));
            return true;
        }
        return false;
    }

    void setTemperatureValues(String[] strArr) {
        if (strArr == null) {
            Log.w("BcSmartspaceCardWeatherForecast", "Temperature values array is null.");
        } else if (getChildCount() < 4) {
            Log.w(
                    "BcSmartspaceCardWeatherForecast",
                    String.format(
                            Locale.US,
                            "Missing %d temperature value view(s) to update.",
                            Integer.valueOf(4 - getChildCount())));
        } else {
            if (strArr.length < 4) {
                Log.w(
                        "BcSmartspaceCardWeatherForecast",
                        String.format(
                                Locale.US,
                                "Missing %d temperature value(s). Hiding incomplete columns.",
                                Integer.valueOf(4 - strArr.length)));
                hideIncompleteColumns(4 - strArr.length);
            }
            int min = Math.min(4, strArr.length);
            for (int i = 0; i < min; i++) {
                TextView textView = (TextView) getChildAt(i).findViewById(R.id.temperature_value);
                if (textView == null) {
                    Log.w(
                            "BcSmartspaceCardWeatherForecast",
                            String.format(
                                    Locale.US,
                                    "Missing temperature value view to update at column: %d.",
                                    Integer.valueOf(i + 1)));
                    return;
                }
                textView.setText(strArr[i]);
            }
        }
    }

    void setWeatherIcons(Bitmap[] bitmapArr) {
        if (bitmapArr == null) {
            Log.w("BcSmartspaceCardWeatherForecast", "Weather icons array is null.");
        } else if (getChildCount() < 4) {
            Log.w(
                    "BcSmartspaceCardWeatherForecast",
                    String.format(
                            Locale.US,
                            "Missing %d weather icon view(s) to update.",
                            Integer.valueOf(4 - getChildCount())));
        } else {
            if (bitmapArr.length < 4) {
                Log.w(
                        "BcSmartspaceCardWeatherForecast",
                        String.format(
                                Locale.US,
                                "Missing %d weather icon(s). Hiding incomplete columns.",
                                Integer.valueOf(4 - bitmapArr.length)));
                hideIncompleteColumns(4 - bitmapArr.length);
            }
            int min = Math.min(4, bitmapArr.length);
            for (int i = 0; i < min; i++) {
                ImageView imageView = (ImageView) getChildAt(i).findViewById(R.id.weather_icon);
                if (imageView == null) {
                    Log.w(
                            "BcSmartspaceCardWeatherForecast",
                            String.format(
                                    Locale.US,
                                    "Missing weather logo view to update at column: %d.",
                                    Integer.valueOf(i + 1)));
                    return;
                }
                imageView.setImageBitmap(bitmapArr[i]);
            }
        }
    }

    void setTimestamps(String[] strArr) {
        if (strArr == null) {
            Log.w("BcSmartspaceCardWeatherForecast", "Timestamps array is null.");
        } else if (getChildCount() < 4) {
            Log.w(
                    "BcSmartspaceCardWeatherForecast",
                    String.format(
                            Locale.US,
                            "Missing %d timestamp view(s) to update.",
                            Integer.valueOf(4 - getChildCount())));
        } else {
            if (strArr.length < 4) {
                Log.w(
                        "BcSmartspaceCardWeatherForecast",
                        String.format(
                                Locale.US,
                                "Missing %d timestamp(s). Hiding incomplete columns.",
                                Integer.valueOf(4 - strArr.length)));
                hideIncompleteColumns(4 - strArr.length);
            }
            int min = Math.min(4, strArr.length);
            for (int i = 0; i < min; i++) {
                TextView textView = (TextView) getChildAt(i).findViewById(R.id.timestamp);
                if (textView == null) {
                    Log.w(
                            "BcSmartspaceCardWeatherForecast",
                            String.format(
                                    Locale.US,
                                    "Missing timestamp view to update at column: %d.",
                                    Integer.valueOf(i + 1)));
                    return;
                }
                textView.setText(strArr[i]);
            }
        }
    }

    private void createWeatherForecastColumns() {
        View[] viewArr = new ConstraintLayout[4];
        for (int i = 0; i < 4; i++) {
            ConstraintLayout constraintLayout =
                    (ConstraintLayout)
                            ViewGroup.inflate(
                                    getContext(),
                                    R.layout.smartspace_card_weather_forecast_column,
                                    null);
            constraintLayout.setId(View.generateViewId());
            viewArr[i] = constraintLayout;
        }
        int i2 = 0;
        while (i2 < 4) {
            Constraints.LayoutParams layoutParams = new Constraints.LayoutParams(-2, 0);
            View view = viewArr[i2];
            ConstraintLayout constraintLayout2 =
                    i2 > 0 ? ((ConstraintLayout) viewArr[i2 - 1]) : null;
            ConstraintLayout constraintLayout3 =
                    i2 < 3 ? ((ConstraintLayout) viewArr[i2 + 1]) : null;
            if (i2 == 0) {
                layoutParams.startToStart = 0;
                layoutParams.horizontalChainStyle = 1;
            } else {
                layoutParams.startToEnd = constraintLayout2.getId();
            }
            if (i2 == 3) {
                layoutParams.endToEnd = 0;
            } else {
                layoutParams.endToStart = constraintLayout3.getId();
            }
            layoutParams.topToTop = 0;
            layoutParams.bottomToBottom = 0;
            addView(view, layoutParams);
            i2++;
        }
    }

    private void hideIncompleteColumns(int i) {
        int i2 = 1;
        if (getChildCount() < 4) {
            Log.w(
                    "BcSmartspaceCardWeatherForecast",
                    String.format(
                            Locale.US,
                            "Missing %d columns to update.",
                            Integer.valueOf(4 - getChildCount())));
            return;
        }
        int i3 = 3 - i;
        int i4 = 0;
        while (i4 < 4) {
            getChildAt(i4).setVisibility(i4 <= i3 ? View.VISIBLE : View.GONE);
            i4++;
        }
        ConstraintLayout.LayoutParams layoutParams =
                (ConstraintLayout.LayoutParams)
                        ((ConstraintLayout) getChildAt(0)).getLayoutParams();
        if (i != 0) {
            i2 = 0;
        }
        layoutParams.horizontalChainStyle = i2;
    }
}
