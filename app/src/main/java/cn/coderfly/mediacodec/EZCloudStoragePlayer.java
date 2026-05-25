package cn.coderfly.mediacodec;

import android.view.Surface;
import java.util.EventListener;

/*
 * Native-method declarations for the libEZMediaPlayer.so cloud-TS player.
 *
 * This is a re-implementation of the public API surface that the AIWIT app
 * uses to talk to libEZMediaPlayer.so. The native library's exported
 * JNI symbols (`Java_cn_coderfly_mediacodec_EZCloudStoragePlayer_*`) require
 * this exact package path and class name to bind correctly. The .so itself
 * is a third-party "coderfly" library bundled by the AIWIT app; we copy it
 * into jniLibs/ at build time.
 *
 * Format flags (from the AIWIT app's own constants):
 *   DBCloudTSPlayerFormatFlagDefault = 0
 *   DBCloudTSPlayerFormatFlagTS2     = 1   ← AIWIT recordings live here
 *   DBCloudTSPlayerFormatFlagTS3     = 2
 *   DBCloudTSPlayerFormatFlagVIAv6TS = 3
 *   EZCloudStoragePlayerFormatFlagMP4 = 4
 *   DBCloudTSPlayerFormatFlagTS4     = 5
 */
public class EZCloudStoragePlayer {
    public static final int DBCloudTSPlayerFormatFlagDefault = 0;
    public static final int DBCloudTSPlayerFormatFlagTS2 = 1;
    public static final int DBCloudTSPlayerFormatFlagTS3 = 2;
    public static final int DBCloudTSPlayerFormatFlagTS4 = 5;
    public static final int DBCloudTSPlayerFormatFlagVIAv6TS = 3;
    public static final int EZCloudStoragePlayerFormatFlagMP4 = 4;

    private long objectPointer;

    public interface Listener extends EventListener {
        void onCached(float ratio);
        void onCheckoutInfo(int width, int height, float duration);
        void onComplete();
        void onError(int code, String message);
        void onPCMCallback(byte[] pcm, int len, float ts);
        void onPCMParamCallback(int sampleRate, int channels, int bitsPerSample);
        void onPerpare();
        void onPlaying(float position);
        void onRenderNNInfo(String info);
        void onStart();
        void onStop();
    }

    public static native long create(String cachePath, String unused1, String unused2, boolean unused3);
    public native void destroy();
    public native void pause(boolean paused);
    public native void play(String url, String unused1, String unused2);
    public native void playContinue(String url, String unused1, String unused2, String unused3);
    public native void readData(byte[] buf, int len);
    public native void setExceptedLength(long bytes);
    public native void setFormatFlag(int flag);
    public native void setListener(Listener listener);
    public native void setPKey(String pk);
    public native void setSurface(Surface surface);
    public native void setSurface2(Surface surface);
    public native void stop();

    static {
        System.loadLibrary("avutil");
        System.loadLibrary("swscale");
        System.loadLibrary("swresample");
        System.loadLibrary("avcodec");
        System.loadLibrary("avformat");
        System.loadLibrary("avfilter");
        System.loadLibrary("EZMediaPlayer");
    }

    public EZCloudStoragePlayer(String cachePath, String unused1, String unused2, boolean unused3) {
        this.objectPointer = create(cachePath, unused1, unused2, unused3);
    }

    public void release() {
        if (this.objectPointer == -1L) return;
        destroy();
        this.objectPointer = -1L;
    }
}
