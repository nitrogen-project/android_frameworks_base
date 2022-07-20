package com.google.android.systemui.smartspace;

import android.app.smartspace.SmartspaceAction;
import android.app.smartspace.SmartspaceTarget;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.systemui.bcsmartspace.R;
import com.android.systemui.plugins.BcSmartspaceDataPlugin;

import com.google.android.systemui.smartspace.logging.BcSmartspaceCardLoggingInfo;

public class BcSmartspaceCardLoyalty extends BcSmartspaceCardGenericImage {
    private TextView mCardPromptView;
    private ImageView mLoyaltyProgramLogoView;
    private TextView mLoyaltyProgramNameView;

    public BcSmartspaceCardLoyalty(Context context) {
        super(context);
    }

    public BcSmartspaceCardLoyalty(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override
    public boolean setSmartspaceActions(
            SmartspaceTarget smartspaceTarget,
            BcSmartspaceDataPlugin.SmartspaceEventNotifier smartspaceEventNotifier,
            BcSmartspaceCardLoggingInfo bcSmartspaceCardLoggingInfo) {
        super.setSmartspaceActions(
                smartspaceTarget, smartspaceEventNotifier, bcSmartspaceCardLoggingInfo);
        SmartspaceAction baseAction = smartspaceTarget.getBaseAction();
        Bundle extras = baseAction == null ? null : baseAction.getExtras();
        mImageView.setVisibility(View.GONE);
        mLoyaltyProgramLogoView.setVisibility(View.GONE);
        mLoyaltyProgramNameView.setVisibility(View.GONE);
        mCardPromptView.setVisibility(View.GONE);
        if (extras != null) {
            boolean containsKey = extras.containsKey("imageBitmap");
            if (extras.containsKey("cardPrompt")) {
                setCardPrompt(extras.getString("cardPrompt"));
                mCardPromptView.setVisibility(View.VISIBLE);
                if (containsKey) {
                    mImageView.setVisibility(View.VISIBLE);
                }
                return true;
            } else if (!extras.containsKey("loyaltyProgramName")) {
                if (containsKey) {
                    mLoyaltyProgramLogoView.setVisibility(View.VISIBLE);
                }
                return containsKey;
            } else {
                setLoyaltyProgramName(extras.getString("loyaltyProgramName"));
                mLoyaltyProgramNameView.setVisibility(View.VISIBLE);
                if (containsKey) {
                    mLoyaltyProgramLogoView.setVisibility(View.VISIBLE);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        mLoyaltyProgramLogoView = (ImageView) findViewById(R.id.loyalty_program_logo);
        mLoyaltyProgramNameView = (TextView) findViewById(R.id.loyalty_program_name);
        mCardPromptView = (TextView) findViewById(R.id.card_prompt);
    }

    @Override
    public void setImageBitmap(Bitmap bitmap) {
        super.setImageBitmap(bitmap);
        mLoyaltyProgramLogoView.setImageBitmap(bitmap);
    }

    void setCardPrompt(String str) {
        TextView textView = mCardPromptView;
        if (textView == null) {
            Log.w("BcSmartspaceCardLoyalty", "No card prompt view to update");
        } else {
            textView.setText(str);
        }
    }

    void setLoyaltyProgramName(String str) {
        TextView textView = mLoyaltyProgramNameView;
        if (textView == null) {
            Log.w("BcSmartspaceCardLoyalty", "No loyalty program name view to update");
        } else {
            textView.setText(str);
        }
    }
}
