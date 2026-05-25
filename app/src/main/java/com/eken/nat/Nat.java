package com.eken.nat;

/* JADX INFO: loaded from: classes2.dex */
public class Nat {
    public static native int getNatType();

    static {
        System.loadLibrary("NatType");
    }
}
