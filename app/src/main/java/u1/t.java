package u1;

import android.util.Log;

/* JADX INFO: compiled from: LogUtil.java */
/* JADX INFO: loaded from: classes5.dex */
public class t {

    /* JADX INFO: renamed from: a, reason: collision with root package name */
    public static final boolean f33974a = e1.a.f27693a.booleanValue();

    public static void a(String str, String str2) {
        if (f33974a) {
            Log.d(str, str2);
        }
    }

    public static void b(String str, String str2) {
        if (f33974a) {
            Log.e(str, str2);
        }
    }

    public static void c(Exception exc) {
        if (f33974a) {
            exc.printStackTrace();
        }
    }

    public static void d(String str, String str2) {
        if (f33974a) {
            Log.i(str, str2);
        }
    }

    public static void e(String str, String str2) {
        if (f33974a) {
            Log.v(str, str2);
        }
    }
}
