package com.eken.doorbell.p2p;

import android.content.Context;
import android.net.wifi.WifiManager;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.concurrent.locks.ReentrantLock;
import org.json.JSONException;
import u1.t;

/* JADX INFO: loaded from: classes5.dex */
public class P2PSession {
    static P2PSession mP2PSession;
    public boolean hasLogin = false;

    public interface P2PClientCall {
        void p2pConnected(String str, boolean z7);

        void p2pReceiveDataCall(String str, byte[] bArr, int i8) throws JSONException, IOException;
    }

    private native void cancel();

    public static String getLocalIpAddress(Context context) {
        try {
            return int2ip(((WifiManager) context.getSystemService("wifi")).getConnectionInfo().getIpAddress());
        } catch (Exception unused) {
            return getLocalIpAddress();
        }
    }

    private native void isEncrypt(int i8);

    private native void run(String str, String str2, int i8, String str3, int i9);

    public native void addListener(P2PClientCall p2PClientCall);

    public native void connectToPeer(String str);

    public native void disconnectToPeer(String str);

    public native int getNatType();

    public native int getSpeed();

    public void logoutP2P() {
        this.hasLogin = false;
        cancel();
        t.a(">>>p2p", "P2P服务器断开");
    }

    public native void removeListener(P2PClientCall p2PClientCall);

    public native int sendPANTILTData(byte[] bArr, int i8);

    public native int sendSpeakerData(byte[] bArr, int i8);

    static {
        System.loadLibrary("VCTP2P");
    }

    public static P2PSession getInstance(Context context) {
        if (mP2PSession == null) {
            mP2PSession = new P2PSession();
        }
        return mP2PSession;
    }

    public static String int2ip(int i8) {
        return (i8 & 255) + "." + ((i8 >> 8) & 255) + "." + ((i8 >> 16) & 255) + "." + ((i8 >> 24) & 255);
    }

    public void loginP2P(String str, String str2, int i8, String str3, int i9) {
        if (this.hasLogin) {
            return;
        }
        ReentrantLock reentrantLock = new ReentrantLock();
        try {
            reentrantLock.lock();
            t.a(">>>p2p", "连接的信息=apkID：" + str + "_serverIP：" + str2 + "_serverPort：" + i8 + "_stunIP：" + str3 + "_stunPort：" + i9);
            run(str, str2, i8, str3, i9);
            this.hasLogin = true;
        } catch (Exception unused) {
        } catch (Throwable th) {
            reentrantLock.unlock();
            throw th;
        }
        reentrantLock.unlock();
    }

    public void setEncrypt(boolean z7) {
        if (z7) {
            isEncrypt(1);
        } else {
            isEncrypt(0);
        }
    }

    public static String getLocalIpAddress() {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                Enumeration<InetAddress> inetAddresses = networkInterfaces.nextElement().getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    InetAddress inetAddressNextElement = inetAddresses.nextElement();
                    if (!inetAddressNextElement.isLoopbackAddress()) {
                        return inetAddressNextElement.getHostAddress().toString();
                    }
                }
            }
            return null;
        } catch (SocketException unused) {
            return null;
        }
    }
}
