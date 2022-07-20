package com.google.android.systemui.smartspace;

import android.app.smartspace.SmartspaceAction;
import android.app.smartspace.SmartspaceTarget;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.systemui.bcsmartspace.R;
import com.android.systemui.plugins.BcSmartspaceDataPlugin;

import com.google.android.systemui.smartspace.logging.BcSmartspaceCardLoggingInfo;

public class BcSmartspaceCardSports extends BcSmartspaceCardSecondary {
    public BcSmartspaceCardSports(Context context) {
        super(context);
    }

    public BcSmartspaceCardSports(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
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
            if (extras.containsKey("matchTimeSummary")) {
                setMatchTimeSummaryText(extras.getString("matchTimeSummary"));
                z = true;
            }
            if (extras.containsKey("firstCompetitorScore")) {
                setFirstCompetitorScore(extras.getString("firstCompetitorScore"));
                z = true;
            }
            if (extras.containsKey("secondCompetitorScore")) {
                setSecondCompetitorScore(extras.getString("secondCompetitorScore"));
                z = true;
            }
            if (extras.containsKey("firstCompetitorLogo")) {
                setFirstCompetitorLogo((Bitmap) extras.get("firstCompetitorLogo"));
                z = true;
            }
            if (!extras.containsKey("secondCompetitorLogo")) {
                return z;
            }
            setSecondCompetitorLogo((Bitmap) extras.get("secondCompetitorLogo"));
            return true;
        }
        return false;
    }

    void setMatchTimeSummaryText(String str) {
        TextView textView = (TextView) findViewById(R.id.match_time_summary);
        if (textView == null) {
            Log.w("BcSmartspaceCardSports", "No match time summary view to update");
        } else {
            textView.setText(str);
        }
    }

    void setFirstCompetitorScore(String str) {
        TextView textView = (TextView) findViewById(R.id.first_competitor_score);
        if (textView == null) {
            Log.w("BcSmartspaceCardSports", "No first competitor logo view to update");
        } else {
            textView.setText(str);
        }
    }

    void setSecondCompetitorScore(String str) {
        TextView textView = (TextView) findViewById(R.id.second_competitor_score);
        if (textView == null) {
            Log.w("BcSmartspaceCardSports", "No second competitor logo view to update");
        } else {
            textView.setText(str);
        }
    }

    void setFirstCompetitorLogo(Bitmap bitmap) {
        ImageView imageView = (ImageView) findViewById(R.id.first_competitor_logo);
        if (imageView == null) {
            Log.w("BcSmartspaceCardSports", "No first competitor logo view to update");
        } else {
            imageView.setImageBitmap(bitmap);
        }
    }

    void setSecondCompetitorLogo(Bitmap bitmap) {
        ImageView imageView = (ImageView) findViewById(R.id.second_competitor_logo);
        if (imageView == null) {
            Log.w("BcSmartspaceCardSports", "No second competitor logo view to update");
        } else {
            imageView.setImageBitmap(bitmap);
        }
    }
}
