package com.google.android.systemui.smartspace.logging;

import java.util.ArrayList;
import java.util.List;

public class BcSmartspaceSubcardLoggingInfo {
    private int mClickedSubcardIndex;
    private List<BcSmartspaceCardMetadataLoggingInfo> mSubcards;

    private BcSmartspaceSubcardLoggingInfo(Builder builder) {
        if (builder.mSubcards == null) {
            mSubcards = new ArrayList();
        } else {
            mSubcards = builder.mSubcards;
        }
        mClickedSubcardIndex = builder.mClickedSubcardIndex;
    }

    public List<BcSmartspaceCardMetadataLoggingInfo> getSubcards() {
        return mSubcards;
    }

    public void setSubcards(List<BcSmartspaceCardMetadataLoggingInfo> list) {
        mSubcards = list;
    }

    public int getClickedSubcardIndex() {
        return mClickedSubcardIndex;
    }

    public void setClickedSubcardIndex(int i) {
        mClickedSubcardIndex = i;
    }

    public String toString() {
        return "BcSmartspaceSubcardLoggingInfo{mSubcards="
                + mSubcards
                + ", mClickedSubcardIndex="
                + mClickedSubcardIndex
                + '}';
    }

    public static class Builder {
        private int mClickedSubcardIndex;
        private List<BcSmartspaceCardMetadataLoggingInfo> mSubcards;

        public Builder setSubcards(List<BcSmartspaceCardMetadataLoggingInfo> list) {
            mSubcards = list;
            return this;
        }

        public Builder setClickedSubcardIndex(int i) {
            mClickedSubcardIndex = i;
            return this;
        }

        public BcSmartspaceSubcardLoggingInfo build() {
            return new BcSmartspaceSubcardLoggingInfo(this);
        }
    }
}
