package com.google.android.systemui.smartspace;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.core.graphics.ColorUtils;

import com.android.systemui.bcsmartspace.R;

public class DoubleShadowTextView extends TextView {
    private final float mAmbientShadowBlur;
    private final int mAmbientShadowColor;
    private boolean mDrawShadow;
    private final float mKeyShadowBlur;
    private final int mKeyShadowColor;
    private final float mKeyShadowOffsetX;
    private final float mKeyShadowOffsetY;

    public DoubleShadowTextView(Context context) {
        this(context, null);
    }

    public DoubleShadowTextView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public DoubleShadowTextView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        updateDrawShadow(getCurrentTextColor());
        mKeyShadowBlur =
                context.getResources().getDimensionPixelSize(R.dimen.key_text_shadow_radius);
        mKeyShadowOffsetX =
                context.getResources().getDimensionPixelSize(R.dimen.key_text_shadow_dx);
        mKeyShadowOffsetY =
                context.getResources().getDimensionPixelSize(R.dimen.key_text_shadow_dy);
        mKeyShadowColor = context.getResources().getColor(R.color.key_text_shadow_color);
        mAmbientShadowBlur =
                context.getResources().getDimensionPixelSize(R.dimen.ambient_text_shadow_radius);
        mAmbientShadowColor = context.getResources().getColor(R.color.ambient_text_shadow_color);
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (!mDrawShadow) {
            getPaint().clearShadowLayer();
            super.onDraw(canvas);
            return;
        }
        getPaint().setShadowLayer(mAmbientShadowBlur, 0.0f, 0.0f, mAmbientShadowColor);
        super.onDraw(canvas);
        canvas.save();
        canvas.clipRect(
                getScrollX(),
                getScrollY() + getExtendedPaddingTop(),
                getScrollX() + getWidth(),
                getScrollY() + getHeight());
        getPaint()
                .setShadowLayer(
                        mKeyShadowBlur, mKeyShadowOffsetX, mKeyShadowOffsetY, mKeyShadowColor);
        super.onDraw(canvas);
        canvas.restore();
    }

    @Override
    public void setTextColor(int i) {
        super.setTextColor(i);
        updateDrawShadow(i);
    }

    private void updateDrawShadow(int i) {
        mDrawShadow = ColorUtils.calculateLuminance(i) > 0.5d;
    }
}
