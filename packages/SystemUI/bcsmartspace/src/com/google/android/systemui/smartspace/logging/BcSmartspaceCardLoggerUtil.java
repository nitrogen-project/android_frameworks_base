package com.google.android.systemui.smartspace.logging;

import android.app.smartspace.SmartspaceAction;
import android.app.smartspace.SmartspaceTarget;

import com.android.systemui.smartspace.nano.SmartspaceProto.SmartSpaceCardMetadata;
import com.android.systemui.smartspace.nano.SmartspaceProto.SmartSpaceSubcards;

import com.google.android.systemui.smartspace.InstanceId;
import com.google.protobuf.nano.MessageNano;

import java.util.ArrayList;
import java.util.List;

public class BcSmartspaceCardLoggerUtil {
    public static byte[] convertSubcardInfoToBytes(
            BcSmartspaceSubcardLoggingInfo bcSmartspaceSubcardLoggingInfo) {
        if (bcSmartspaceSubcardLoggingInfo == null
                || bcSmartspaceSubcardLoggingInfo.getSubcards() == null
                || bcSmartspaceSubcardLoggingInfo.getSubcards().isEmpty()) {
            return null;
        }
        SmartSpaceSubcards smartSpaceSubcards = new SmartSpaceSubcards();
        smartSpaceSubcards.clickedSubcardIndex =
                bcSmartspaceSubcardLoggingInfo.getClickedSubcardIndex();
        List<BcSmartspaceCardMetadataLoggingInfo> subcards =
                bcSmartspaceSubcardLoggingInfo.getSubcards();
        ArrayList arrayList = new ArrayList();
        for (int i = 0; i < subcards.size(); i++) {
            BcSmartspaceCardMetadataLoggingInfo bcSmartspaceCardMetadataLoggingInfo =
                    subcards.get(i);
            SmartSpaceCardMetadata smartSpaceCardMetadata = new SmartSpaceCardMetadata();
            smartSpaceCardMetadata.instanceId = bcSmartspaceCardMetadataLoggingInfo.getInstanceId();
            smartSpaceCardMetadata.cardTypeId = bcSmartspaceCardMetadataLoggingInfo.getCardTypeId();
            arrayList.add(smartSpaceCardMetadata);
        }
        smartSpaceSubcards.subcards =
                (SmartSpaceCardMetadata[])
                        arrayList.toArray(new SmartSpaceCardMetadata[arrayList.size()]);
        return MessageNano.toByteArray(smartSpaceSubcards);
    }

    public static void forcePrimaryFeatureTypeAndInjectWeatherSubcard(
            BcSmartspaceCardLoggingInfo bcSmartspaceCardLoggingInfo,
            SmartspaceTarget smartspaceTarget,
            int i) {
        if (bcSmartspaceCardLoggingInfo == null
                || bcSmartspaceCardLoggingInfo.getFeatureType() != 1) {
            return;
        }
        bcSmartspaceCardLoggingInfo.setFeatureType(i);
        if ("date_card_794317_92634".equals(smartspaceTarget.getSmartspaceTargetId())) {
            return;
        }
        if (bcSmartspaceCardLoggingInfo.getSubcardInfo() == null) {
            bcSmartspaceCardLoggingInfo.setSubcardInfo(
                    new BcSmartspaceSubcardLoggingInfo.Builder()
                            .setClickedSubcardIndex(0)
                            .setSubcards(new ArrayList())
                            .build());
        }
        if (bcSmartspaceCardLoggingInfo.getSubcardInfo().getSubcards() == null) {
            bcSmartspaceCardLoggingInfo.getSubcardInfo().setSubcards(new ArrayList());
        }
        bcSmartspaceCardLoggingInfo
                .getSubcardInfo()
                .getSubcards()
                .add(
                        new BcSmartspaceCardMetadataLoggingInfo.Builder()
                                .setInstanceId(InstanceId.create(smartspaceTarget))
                                .setCardTypeId(1)
                                .build());
    }

    public static BcSmartspaceSubcardLoggingInfo createSubcardLoggingInfo(
            SmartspaceTarget smartspaceTarget) {
        if (smartspaceTarget == null
                || smartspaceTarget.getBaseAction() == null
                || smartspaceTarget.getBaseAction().getExtras() == null
                || smartspaceTarget.getBaseAction().getExtras().isEmpty()
                || smartspaceTarget.getBaseAction().getExtras().getInt("subcardType", -1) == -1) {
            return null;
        }
        SmartspaceAction baseAction = smartspaceTarget.getBaseAction();
        int create = InstanceId.create(baseAction.getExtras().getString("subcardId"));
        BcSmartspaceCardMetadataLoggingInfo build =
                new BcSmartspaceCardMetadataLoggingInfo.Builder()
                        .setInstanceId(create)
                        .setCardTypeId(baseAction.getExtras().getInt("subcardType"))
                        .build();
        ArrayList arrayList = new ArrayList();
        arrayList.add(build);
        return new BcSmartspaceSubcardLoggingInfo.Builder()
                .setSubcards(arrayList)
                .setClickedSubcardIndex(0)
                .build();
    }
}
