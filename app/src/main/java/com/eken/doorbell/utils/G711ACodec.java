package com.eken.doorbell.utils;

/* JADX INFO: loaded from: classes5.dex */
public class G711ACodec {
    public native byte[] g711aEncode(byte[] bArr);

    static {
        System.loadLibrary("g711a_jni");
    }
}
