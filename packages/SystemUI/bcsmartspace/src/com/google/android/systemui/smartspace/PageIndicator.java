package com.google.android.systemui.smartspace;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.content.res.AppCompatResources;

import com.android.systemui.bcsmartspace.R;

public class PageIndicator extends LinearLayout {
    private int mPrimaryColor = getAttrColor(getContext(), 16842806);
    private int mCurrentPageIndex = -1;
    private int mNumPages = -1;

    public PageIndicator(Context context) {
        super(context);
    }

    public PageIndicator(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public PageIndicator(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    public PageIndicator(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
    }

    public void setNumPages(int i) {
        if (i <= 0) {
            Log.w("PageIndicator", "Total number of pages invalid: " + i + ". Assuming 1 page.");
            i = 1;
        }
        if (i < 2) {
            setVisibility(View.GONE);
            return;
        }
        setVisibility(View.VISIBLE);
        if (i == mNumPages) {
            return;
        }
        mNumPages = i;
        initializePageIndicators();
    }

    public void setPageOffset(int i, float f) {
        int i2 = (f > 0.0f ? 1 : (f == 0.0f ? 0 : -1));
        if (!(i2 == 0 && i == mCurrentPageIndex) && i >= 0 && i < getChildCount() - 1) {
            ImageView imageView = (ImageView) getChildAt(i);
            int i3 = i + 1;
            ImageView imageView2 = (ImageView) getChildAt(i3);
            if (imageView == null || imageView2 == null) {
                return;
            }
            imageView.setAlpha(((1.0f - f) * 0.6f) + 0.4f);
            imageView2.setAlpha((0.6f * f) + 0.4f);
            Context context = getContext();
            int i4 = R.string.accessibility_smartspace_page;
            Object[] objArr = new Object[2];
            objArr[0] = Integer.valueOf(((double) f) < 0.5d ? i3 : i + 2);
            objArr[1] = Integer.valueOf(mNumPages);
            setContentDescription(context.getString(i4, objArr));
            if (i2 != 0 && f < 0.99f) {
                return;
            }
            if (i2 != 0) {
                i = i3;
            }
            mCurrentPageIndex = i;
        }
    }

    private void initializePageIndicators() {
        LinearLayout.LayoutParams layoutParams;
        int childCount = getChildCount() - mNumPages;
        for (int i = 0; i < childCount; i++) {
            removeViewAt(0);
        }
        int dimensionPixelSize =
                getContext()
                        .getResources()
                        .getDimensionPixelSize(R.dimen.page_indicator_dot_margin);
        int i2 = 0;
        while (i2 < mNumPages) {
            ImageView imageView =
                    i2 < getChildCount() ? (ImageView) getChildAt(i2) : new ImageView(getContext());
            if (i2 < getChildCount()) {
                layoutParams = (LinearLayout.LayoutParams) imageView.getLayoutParams();
            } else {
                layoutParams = new LinearLayout.LayoutParams(-2, -2);
            }
            if (i2 == 0) {
                layoutParams.setMarginStart(0);
            } else {
                layoutParams.setMarginStart(dimensionPixelSize);
            }
            if (i2 == mNumPages - 1) {
                layoutParams.setMarginEnd(0);
            } else {
                layoutParams.setMarginEnd(dimensionPixelSize);
            }
            if (i2 < getChildCount()) {
                imageView.setLayoutParams(layoutParams);
            } else {
                Drawable drawable =
                        AppCompatResources.getDrawable(getContext(), R.drawable.page_indicator_dot);
                drawable.setTint(mPrimaryColor);
                imageView.setImageDrawable(drawable);
                addView(imageView, layoutParams);
            }
            int i3 = mCurrentPageIndex;
            if (i3 < 0) {
                mCurrentPageIndex = 0;
            } else {
                int i4 = mNumPages;
                if (i3 >= i4) {
                    mCurrentPageIndex = i4 - 1;
                }
            }
            imageView.setAlpha(i2 == mCurrentPageIndex ? 1.0f : 0.4f);
            i2++;
        }
        setContentDescription(
                getContext()
                        .getString(
                                R.string.accessibility_smartspace_page,
                                1,
                                Integer.valueOf(mNumPages)));
    }

    public void setPrimaryColor(int i) {
        mPrimaryColor = i;
        for (int i2 = 0; i2 < getChildCount(); i2++) {
            ((ImageView) getChildAt(i2)).getDrawable().setTint(mPrimaryColor);
        }
    }

    public static int getAttrColor(Context context, int i) {
        TypedArray obtainStyledAttributes = context.obtainStyledAttributes(new int[] {i});
        int color = obtainStyledAttributes.getColor(0, 0);
        obtainStyledAttributes.recycle();
        return color;
    }
}
