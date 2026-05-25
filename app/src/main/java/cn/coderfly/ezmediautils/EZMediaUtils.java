package cn.coderfly.ezmediautils;

import android.content.res.AssetManager;
import android.graphics.Bitmap;

/* JADX INFO: loaded from: classes6.dex */
public class EZMediaUtils {
    public static native byte[] convertRGBToYUV(byte[] bArr, int i8, int i9, int i10, int i11);

    public static native Bitmap convertYUVToBitmap(byte[] bArr, int i8, int i9, int i10, int i11);

    public static native Bitmap decodeFrameWidthAsset(AssetManager assetManager, String str);

    public static native Bitmap decodeFrameWithData(byte[] bArr);

    public static native Bitmap decodeFrameWithFilePath(String str);

    public static native Bitmap decodeH265FrameWithData(byte[] bArr);

    public static native byte[] decryptAESData(byte[] bArr, String str, int i8);

    static {
        System.loadLibrary("avutil");
        System.loadLibrary("swresample");
        System.loadLibrary("swscale");
        System.loadLibrary("avcodec");
        System.loadLibrary("avformat");
        System.loadLibrary("avfilter");
        System.loadLibrary("EZMediaUtils");
    }
}
