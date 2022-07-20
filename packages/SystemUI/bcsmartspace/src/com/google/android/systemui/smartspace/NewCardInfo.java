package com.google.android.systemui.smartspace;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import com.android.systemui.smartspace.nano.SmartspaceProto.CardWrapper;
import com.android.systemui.smartspace.nano.SmartspaceProto.SmartspaceUpdate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class NewCardInfo {
    private final SmartspaceUpdate.SmartspaceCard mCard;
    private final Intent mIntent;
    private final boolean mIsPrimary;
    private final PackageInfo mPackageInfo;
    private final long mPublishTime;

    public NewCardInfo(
            SmartspaceUpdate.SmartspaceCard smartspaceCard,
            Intent intent,
            boolean z,
            long j,
            PackageInfo packageInfo) {
        mCard = smartspaceCard;
        mIsPrimary = z;
        mIntent = intent;
        mPublishTime = j;
        mPackageInfo = packageInfo;
    }

    public boolean isPrimary() {
        return mIsPrimary;
    }

    public Bitmap retrieveIcon(Context context) {
        SmartspaceUpdate.SmartspaceCard.Image image = mCard.icon;
        if (image == null) {
            return null;
        }
        Bitmap bitmap = (Bitmap) retrieveFromIntent(image.key, mIntent);
        if (bitmap != null) {
            return bitmap;
        }
        try {
        } catch (Exception unused) {
            Log.e(
                    "NewCardInfo",
                    "retrieving bitmap uri=" + image.uri + " gsaRes=" + image.gsaResourceName);
        }
        if (!TextUtils.isEmpty(image.uri)) {
            try {
                return MediaStore.Images.Media.getBitmap(
                        context.getContentResolver(), Uri.parse(image.uri));
            } catch (IOException e) {
                Log.e("NewCardInfo", "failed to get bitmap from uri");
            }
        }
        if (!TextUtils.isEmpty(image.gsaResourceName)) {
            Intent.ShortcutIconResource shortcutIconResource = new Intent.ShortcutIconResource();
            shortcutIconResource.packageName = "com.google.android.googlequicksearchbox";
            shortcutIconResource.resourceName = image.gsaResourceName;
            return createIconBitmap(shortcutIconResource, context);
        }
        return null;
    }

    public CardWrapper toWrapper(Context context) {
        CardWrapper cardWrapper = new CardWrapper();
        Bitmap retrieveIcon = retrieveIcon(context);
        if (retrieveIcon != null) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            retrieveIcon.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
            cardWrapper.icon = byteArrayOutputStream.toByteArray();
        }
        cardWrapper.card = mCard;
        cardWrapper.publishTime = mPublishTime;
        PackageInfo packageInfo = mPackageInfo;
        if (packageInfo != null) {
            cardWrapper.gsaVersionCode = packageInfo.versionCode;
            cardWrapper.gsaUpdateTime = packageInfo.lastUpdateTime;
        }
        return cardWrapper;
    }

    private static <T> T retrieveFromIntent(String str, Intent intent) {
        if (!TextUtils.isEmpty(str)) {
            return (T) intent.getParcelableExtra(str);
        }
        return null;
    }

    static Bitmap createIconBitmap(
            Intent.ShortcutIconResource shortcutIconResource, Context context) {
        try {
            Resources resourcesForApplication =
                    context.getPackageManager()
                            .getResourcesForApplication(shortcutIconResource.packageName);
            if (resourcesForApplication != null) {
                return BitmapFactory.decodeResource(
                        resourcesForApplication,
                        resourcesForApplication.getIdentifier(
                                shortcutIconResource.resourceName, null, null));
            }
        } catch (Exception unused) {
        }
        return null;
    }

    public int getUserId() {
        return mIntent.getIntExtra("uid", -1);
    }

    public boolean shouldDiscard() {
        SmartspaceUpdate.SmartspaceCard smartspaceCard = mCard;
        return smartspaceCard == null || smartspaceCard.shouldDiscard;
    }
}
