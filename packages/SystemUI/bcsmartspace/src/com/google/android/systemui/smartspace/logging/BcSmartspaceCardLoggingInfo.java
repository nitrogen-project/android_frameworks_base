package com.google.android.systemui.smartspace.logging;

public class BcSmartspaceCardLoggingInfo {
    private final int mCardinality;
    private final int mDisplaySurface;
    private int mFeatureType;
    private final int mInstanceId;
    private final int mRank;
    private final int mReceivedLatency;
    private BcSmartspaceSubcardLoggingInfo mSubcardInfo;

    private BcSmartspaceCardLoggingInfo(Builder builder) {
        mInstanceId = builder.mInstanceId;
        mDisplaySurface = builder.mDisplaySurface;
        mRank = builder.mRank;
        mCardinality = builder.mCardinality;
        mFeatureType = builder.mFeatureType;
        mReceivedLatency = builder.mReceivedLatency;
        mSubcardInfo = builder.mSubcardInfo;
    }

    public int getInstanceId() {
        return mInstanceId;
    }

    public int getDisplaySurface() {
        return mDisplaySurface;
    }

    public int getRank() {
        return mRank;
    }

    public int getCardinality() {
        return mCardinality;
    }

    public int getFeatureType() {
        return mFeatureType;
    }

    public int getReceivedLatency() {
        return mReceivedLatency;
    }

    public BcSmartspaceSubcardLoggingInfo getSubcardInfo() {
        return mSubcardInfo;
    }

    public void setFeatureType(int i) {
        mFeatureType = i;
    }

    public void setSubcardInfo(BcSmartspaceSubcardLoggingInfo bcSmartspaceSubcardLoggingInfo) {
        mSubcardInfo = bcSmartspaceSubcardLoggingInfo;
    }

    public String toString() {
        return "instance_id = "
                + getInstanceId()
                + ", feature type = "
                + getFeatureType()
                + ", display surface = "
                + getDisplaySurface()
                + ", rank = "
                + getRank()
                + ", cardinality = "
                + getCardinality()
                + ", receivedLatencyMillis = "
                + getReceivedLatency()
                + ", subcardInfo = "
                + getSubcardInfo();
    }

    public static class Builder {
        private int mCardinality;
        private int mDisplaySurface = 1;
        private int mFeatureType;
        private int mInstanceId;
        private int mRank;
        private int mReceivedLatency;
        private BcSmartspaceSubcardLoggingInfo mSubcardInfo;

        public Builder setInstanceId(int i) {
            mInstanceId = i;
            return this;
        }

        public Builder setDisplaySurface(int i) {
            mDisplaySurface = i;
            return this;
        }

        public Builder setRank(int i) {
            mRank = i;
            return this;
        }

        public Builder setCardinality(int i) {
            mCardinality = i;
            return this;
        }

        public Builder setFeatureType(int i) {
            mFeatureType = i;
            return this;
        }

        public Builder setReceivedLatency(int i) {
            mReceivedLatency = i;
            return this;
        }

        public Builder setSubcardInfo(
                BcSmartspaceSubcardLoggingInfo bcSmartspaceSubcardLoggingInfo) {
            mSubcardInfo = bcSmartspaceSubcardLoggingInfo;
            return this;
        }

        public BcSmartspaceCardLoggingInfo build() {
            return new BcSmartspaceCardLoggingInfo(this);
        }
    }
}
