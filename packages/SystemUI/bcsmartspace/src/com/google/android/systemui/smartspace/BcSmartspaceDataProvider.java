package com.google.android.systemui.smartspace;

import android.app.smartspace.SmartspaceTarget;
import android.app.smartspace.SmartspaceTargetEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.systemui.bcsmartspace.R;
import com.android.systemui.plugins.BcSmartspaceDataPlugin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class BcSmartspaceDataProvider implements BcSmartspaceDataPlugin {
    private final Set<BcSmartspaceDataPlugin.SmartspaceTargetListener> mSmartspaceTargetListeners =
            new HashSet();
    private final List<SmartspaceTarget> mSmartspaceTargets = new ArrayList();
    private Set<View> mViews = new HashSet();
    private Set<View.OnAttachStateChangeListener> mAttachListeners = new HashSet();
    private BcSmartspaceDataPlugin.SmartspaceEventNotifier mEventNotifier = null;
    private View.OnAttachStateChangeListener mStateChangeListener =
            new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(View view) {
                    mViews.add(view);
                    for (View.OnAttachStateChangeListener onAttachStateChangeListener :
                            mAttachListeners) {
                        onAttachStateChangeListener.onViewAttachedToWindow(view);
                    }
                }

                @Override
                public void onViewDetachedFromWindow(View view) {
                    mViews.remove(view);
                    view.removeOnAttachStateChangeListener(this);
                    for (View.OnAttachStateChangeListener onAttachStateChangeListener :
                            mAttachListeners) {
                        onAttachStateChangeListener.onViewDetachedFromWindow(view);
                    }
                }
            };

    @Override
    public void registerListener(
            BcSmartspaceDataPlugin.SmartspaceTargetListener smartspaceTargetListener) {
        mSmartspaceTargetListeners.add(smartspaceTargetListener);
        smartspaceTargetListener.onSmartspaceTargetsUpdated(mSmartspaceTargets);
    }

    @Override
    public void unregisterListener(
            BcSmartspaceDataPlugin.SmartspaceTargetListener smartspaceTargetListener) {
        mSmartspaceTargetListeners.remove(smartspaceTargetListener);
    }

    @Override
    public void registerSmartspaceEventNotifier(
            BcSmartspaceDataPlugin.SmartspaceEventNotifier smartspaceEventNotifier) {
        mEventNotifier = smartspaceEventNotifier;
    }

    @Override
    public void notifySmartspaceEvent(SmartspaceTargetEvent smartspaceTargetEvent) {
        BcSmartspaceDataPlugin.SmartspaceEventNotifier smartspaceEventNotifier = mEventNotifier;
        if (smartspaceEventNotifier != null) {
            smartspaceEventNotifier.notifySmartspaceEvent(smartspaceTargetEvent);
        }
    }

    @Override
    public BcSmartspaceDataPlugin.SmartspaceView getView(ViewGroup viewGroup) {
        View inflate =
                LayoutInflater.from(viewGroup.getContext())
                        .inflate(R.layout.smartspace_enhanced, viewGroup, false);
        inflate.addOnAttachStateChangeListener(mStateChangeListener);
        return (BcSmartspaceDataPlugin.SmartspaceView) inflate;
    }

    @Override
    public void addOnAttachStateChangeListener(
            View.OnAttachStateChangeListener onAttachStateChangeListener) {
        mAttachListeners.add(onAttachStateChangeListener);
        for (View view : mViews) {
            onAttachStateChangeListener.onViewAttachedToWindow(view);
        }
    }

    @Override
    public void onTargetsAvailable(List<SmartspaceTarget> list) {
        mSmartspaceTargets.clear();
        for (SmartspaceTarget smartspaceTarget : list) {
            if (smartspaceTarget.getFeatureType() != 15) {
                mSmartspaceTargets.add(smartspaceTarget);
            }
        }
        mSmartspaceTargetListeners.forEach(
                new Consumer() {
                    @Override
                    public final void accept(Object obj) {
                        ((BcSmartspaceDataPlugin.SmartspaceTargetListener) obj)
                                .onSmartspaceTargetsUpdated(mSmartspaceTargets);
                    }
                });
    }
}
