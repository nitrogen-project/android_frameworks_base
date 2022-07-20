package com.google.android.systemui.smartspace;

import android.content.Context;
import android.util.Log;

import com.google.protobuf.nano.MessageNano;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

public class ProtoStore {
    private final Context mContext;

    public ProtoStore(Context context) {
        mContext = context.getApplicationContext();
    }

    public void store(MessageNano messageNano, String str) {
        try {
            FileOutputStream openFileOutput = mContext.openFileOutput(str, 0);
            if (messageNano != null) {
                openFileOutput.write(MessageNano.toByteArray(messageNano));
            } else {
                Log.d("ProtoStore", "deleting " + str);
                mContext.deleteFile(str);
            }
            if (openFileOutput == null) {
                return;
            }
            openFileOutput.close();
        } catch (FileNotFoundException unused) {
            Log.d("ProtoStore", "file does not exist");
        } catch (Exception e) {
            Log.e("ProtoStore", "unable to write file", e);
        }
    }

    public <T extends MessageNano> boolean load(String str, T t) {
        File fileStreamPath = mContext.getFileStreamPath(str);
        try {
            FileInputStream fileInputStream = new FileInputStream(fileStreamPath);
            try {
                int length = (int) fileStreamPath.length();
                byte[] bArr = new byte[length];
                fileInputStream.read(bArr, 0, length);
                MessageNano.mergeFrom(t, bArr);
                fileInputStream.close();
                return true;
            } catch (Throwable th) {
                try {
                    fileInputStream.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
                throw th;
            }
        } catch (FileNotFoundException unused) {
            Log.d("ProtoStore", "no cached data");
            return false;
        } catch (Exception e) {
            Log.e("ProtoStore", "unable to load data", e);
            return false;
        }
    }
}
