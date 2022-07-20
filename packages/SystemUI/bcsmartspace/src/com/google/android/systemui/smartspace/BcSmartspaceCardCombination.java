package com.google.android.systemui.smartspace;

import android.app.smartspace.SmartspaceAction;
import android.app.smartspace.SmartspaceTarget;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.android.systemui.bcsmartspace.R;
import com.android.systemui.plugins.BcSmartspaceDataPlugin;

import com.google.android.systemui.smartspace.logging.BcSmartspaceCardLoggingInfo;

import java.util.List;

public class BcSmartspaceCardCombination extends BcSmartspaceCardSecondary {
    protected ConstraintLayout mFirstSubCard;
    protected ConstraintLayout mSecondSubCard;

    public BcSmartspaceCardCombination(Context context) {
        super(context);
    }

    public BcSmartspaceCardCombination(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mFirstSubCard = (ConstraintLayout) findViewById(R.id.first_sub_card);
        mSecondSubCard = (ConstraintLayout) findViewById(R.id.second_sub_card);
    }

    @Override
    public boolean setSmartspaceActions(
            SmartspaceTarget smartspaceTarget,
            BcSmartspaceDataPlugin.SmartspaceEventNotifier smartspaceEventNotifier,
            BcSmartspaceCardLoggingInfo bcSmartspaceCardLoggingInfo) {
        SmartspaceAction smartspaceAction;
        List actionChips = smartspaceTarget.getActionChips();
        if (actionChips == null
                || actionChips.size() < 1
                || (smartspaceAction = (SmartspaceAction) actionChips.get(0)) == null) {
            return false;
        }
        ConstraintLayout constraintLayout = mFirstSubCard;
        boolean z =
                constraintLayout != null
                        && fillSubCard(
                                constraintLayout,
                                smartspaceTarget,
                                smartspaceAction,
                                smartspaceEventNotifier,
                                bcSmartspaceCardLoggingInfo);
        boolean z2 = actionChips.size() > 1 && actionChips.get(1) != null;
        boolean fillSubCard =
                z2
                        ? fillSubCard(
                                mSecondSubCard,
                                smartspaceTarget,
                                (SmartspaceAction) actionChips.get(1),
                                smartspaceEventNotifier,
                                bcSmartspaceCardLoggingInfo)
                        : true;
        if (getLayoutParams() instanceof LinearLayout.LayoutParams) {
            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) getLayoutParams();
            if (z2 && fillSubCard) {
                layoutParams.weight = 3.0f;
            } else {
                layoutParams.weight = 1.0f;
            }
            setLayoutParams(layoutParams);
        }
        return z && fillSubCard;
    }

    public boolean fillSubCard(
            ConstraintLayout constraintLayout,
            SmartspaceTarget smartspaceTarget,
            SmartspaceAction smartspaceAction,
            BcSmartspaceDataPlugin.SmartspaceEventNotifier smartspaceEventNotifier,
            BcSmartspaceCardLoggingInfo bcSmartspaceCardLoggingInfo) {
        boolean z;
        TextView textView = (TextView) constraintLayout.findViewById(R.id.sub_card_text);
        ImageView imageView = (ImageView) constraintLayout.findViewById(R.id.sub_card_icon);
        if (textView == null) {
            Log.w("BcSmartspaceCardCombination", "No sub-card text field to update");
            return false;
        } else if (imageView == null) {
            Log.w("BcSmartspaceCardCombination", "No sub-card image field to update");
            return false;
        } else {
            BcSmartSpaceUtil.setOnClickListener(
                    constraintLayout,
                    smartspaceTarget,
                    smartspaceAction,
                    "BcSmartspaceCardCombination",
                    smartspaceEventNotifier,
                    bcSmartspaceCardLoggingInfo);
            Drawable iconDrawable =
                    BcSmartSpaceUtil.getIconDrawable(smartspaceAction.getIcon(), getContext());
            boolean z2 = true;
            if (iconDrawable == null) {
                imageView.setVisibility(View.GONE);
                z = false;
            } else {
                imageView.setImageDrawable(iconDrawable);
                imageView.setVisibility(View.VISIBLE);
                z = true;
            }
            CharSequence title = smartspaceAction.getTitle();
            if (TextUtils.isEmpty(title)) {
                textView.setVisibility(View.GONE);
                z2 = z;
            } else {
                textView.setText(title);
                textView.setVisibility(View.VISIBLE);
            }
            constraintLayout.setContentDescription(
                    z2 ? smartspaceAction.getContentDescription() : null);
            if (z2) {
                constraintLayout.setVisibility(View.VISIBLE);
            } else {
                constraintLayout.setVisibility(View.GONE);
            }
            return z2;
        }
    }
}
