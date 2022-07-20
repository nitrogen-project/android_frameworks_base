package com.google.android.systemui.smartspace;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;

import com.android.internal.graphics.ColorUtils;
import com.android.systemui.bcsmartspace.R;

public class DoubleShadowIconDrawable extends LayerDrawable {
    private Drawable mIconDrawable;
    private Drawable mShadowDrawable;

    public DoubleShadowIconDrawable(Drawable drawable, Context context) {
        super(new Drawable[0]);
        int dimensionPixelSize =
                context.getResources().getDimensionPixelSize(R.dimen.enhanced_smartspace_icon_size);
        generateIconAndShadow(
                drawable, context, dimensionPixelSize, dimensionPixelSize, dimensionPixelSize);
    }

    private void generateIconAndShadow(Drawable drawable, Context context, int i, int i2, int i3) {
        Bitmap createBitmap = Bitmap.createBitmap(i3, i3, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(createBitmap);
        drawable.setBounds(0, 0, i, i2);
        int i4 = i3 / 2;
        canvas.translate(((-i) / 2) + i4, ((-i2) / 2) + i4);
        drawable.draw(canvas);
        mIconDrawable = new BitmapDrawable(context.getResources(), createBitmap);
        Drawable generateShadowDrawable = generateShadowDrawable(createBitmap, context);
        mShadowDrawable = generateShadowDrawable;
        addLayer(generateShadowDrawable);
        addLayer(mIconDrawable);
        setBounds(0, 0, i3, i3);
    }

    private static Drawable generateShadowDrawable(Bitmap bitmap, Context context) {
        Bitmap createBitmap =
                Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(createBitmap);
        float dimensionPixelSize =
                context.getResources().getDimensionPixelSize(R.dimen.ambient_text_shadow_radius);
        float dimensionPixelSize2 =
                context.getResources().getDimensionPixelSize(R.dimen.key_text_shadow_radius);
        float dimensionPixelSize3 =
                context.getResources().getDimensionPixelSize(R.dimen.key_text_shadow_dx);
        float dimensionPixelSize4 =
                context.getResources().getDimensionPixelSize(R.dimen.key_text_shadow_dy);
        int[] iArr = new int[2];
        Paint paint = new Paint(3);
        Paint paint2 = new Paint(3);
        if (dimensionPixelSize != 0.0f) {
            paint.setMaskFilter(new BlurMaskFilter(dimensionPixelSize, BlurMaskFilter.Blur.NORMAL));
            Bitmap extractAlpha = bitmap.extractAlpha(paint, iArr);
            paint2.setAlpha(64);
            canvas.drawBitmap(extractAlpha, iArr[0], iArr[1], paint2);
        }
        if (dimensionPixelSize2 != 0.0f) {
            paint.setMaskFilter(
                    new BlurMaskFilter(dimensionPixelSize2, BlurMaskFilter.Blur.NORMAL));
            Bitmap extractAlpha2 = bitmap.extractAlpha(paint, iArr);
            paint2.setAlpha(72);
            canvas.drawBitmap(
                    extractAlpha2,
                    iArr[0] + dimensionPixelSize3,
                    iArr[1] + dimensionPixelSize4,
                    paint2);
        }
        return new BitmapDrawable(context.getResources(), createBitmap);
    }

    @Override
    public void setTint(int i) {
        Drawable drawable = mIconDrawable;
        if (drawable != null) {
            drawable.setTint(i);
        }
        Drawable drawable2 = mShadowDrawable;
        if (drawable2 != null) {
            drawable2.setAlpha(ColorUtils.calculateLuminance(i) > 0.5d ? 255 : 0);
        }
    }
}
