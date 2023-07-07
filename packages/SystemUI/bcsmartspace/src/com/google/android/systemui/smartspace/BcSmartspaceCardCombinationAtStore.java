package com.google.android.systemui.smartspace;

import android.app.smartspace.SmartspaceAction;
import android.app.smartspace.SmartspaceTarget;
import android.content.Context;
import android.util.AttributeSet;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.android.systemui.bcsmartspace.R;
import com.android.systemui.plugins.BcSmartspaceDataPlugin;

import com.google.android.systemui.smartspace.logging.BcSmartspaceCardLoggingInfo;

import java.util.List;

public class BcSmartspaceCardCombinationAtStore extends BcSmartspaceCardCombination {
    public BcSmartspaceCardCombinationAtStore(Context context) {
        super(context);
    }

    public BcSmartspaceCardCombinationAtStore(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override
    public boolean setSmartspaceActions(
            SmartspaceTarget smartspaceTarget,
            BcSmartspaceDataPlugin.SmartspaceEventNotifier smartspaceEventNotifier,
            BcSmartspaceCardLoggingInfo bcSmartspaceCardLoggingInfo) {
        SmartspaceAction smartspaceAction;
        List actionChips = smartspaceTarget.getActionChips();
        if (actionChips == null
                || actionChips.isEmpty()
                || (smartspaceAction = (SmartspaceAction) actionChips.get(0)) == null) {
            return false;
        }
        ConstraintLayout constraintLayout = mFirstSubCard;
        boolean z =
                (constraintLayout instanceof BcSmartspaceCardShoppingList)
                        && ((BcSmartspaceCardShoppingList) constraintLayout)
                                .setSmartspaceActions(
                                        smartspaceTarget,
                                        smartspaceEventNotifier,
                                        bcSmartspaceCardLoggingInfo);
        ConstraintLayout constraintLayout2 = mSecondSubCard;
        boolean z2 =
                constraintLayout2 != null
                        && fillSubCard(
                                constraintLayout2,
                                smartspaceTarget,
                                smartspaceAction,
                                smartspaceEventNotifier,
                                bcSmartspaceCardLoggingInfo);
        if (z) {
            mFirstSubCard.setBackgroundResource(R.drawable.bg_smartspace_combination_sub_card);
        }
        return z && z2;
    }
}
