package com.google.android.systemui.smartspace.logging;

public class BcSmartspaceCardMetadataLoggingInfo {
    private final int mCardTypeId;
    private final int mInstanceId;

    private BcSmartspaceCardMetadataLoggingInfo(Builder builder) {
        mInstanceId = builder.mInstanceId;
        mCardTypeId = builder.mCardTypeId;
    }

    public int getInstanceId() {
        return mInstanceId;
    }

    public int getCardTypeId() {
        return mCardTypeId;
    }

    public String toString() {
        return "BcSmartspaceCardMetadataLoggingInfo{mInstanceId="
                + mInstanceId
                + ", mCardTypeId="
                + mCardTypeId
                + '}';
    }

    public static class Builder {
        private int mCardTypeId;
        private int mInstanceId;

        public Builder setInstanceId(int i) {
            mInstanceId = i;
            return this;
        }

        public Builder setCardTypeId(int i) {
            mCardTypeId = i;
            return this;
        }

        public BcSmartspaceCardMetadataLoggingInfo build() {
            return new BcSmartspaceCardMetadataLoggingInfo(this);
        }
    }
}
