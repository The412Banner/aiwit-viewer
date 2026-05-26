package n1;

import java.nio.ByteBuffer;

/* JADX INFO: compiled from: FrameData.java */
/* JADX INFO: loaded from: classes6.dex */
public class b {

    /* JADX INFO: renamed from: a, reason: collision with root package name */
    public ByteBuffer f32188a;

    /* JADX INFO: renamed from: b, reason: collision with root package name */
    public long f32189b;

    /* JADX INFO: renamed from: c, reason: collision with root package name */
    public int f32190c;

    /* JADX INFO: renamed from: d, reason: collision with root package name */
    public int f32191d;

    /* JADX INFO: renamed from: e, reason: collision with root package name */
    public int f32192e = 0;

    /* JADX INFO: renamed from: f, reason: collision with root package name */
    public int f32193f;

    public static String a(byte[] bArr, int i8) {
        if (i8 > bArr.length || i8 == 0) {
            i8 = bArr.length;
        }
        StringBuilder sb = new StringBuilder(i8 * 2);
        for (int i9 = 0; i9 < i8; i9++) {
            sb.append(String.format("%02X ", new Integer(bArr[i9] & 0xFF)));
        }
        return sb.toString();
    }

    public ByteBuffer b() {
        return this.f32188a;
    }

    public String toString() {
        return "type:- flags:" + this.f32193f + " ts:" + this.f32189b + " size:" + this.f32188a.array().length + " bytes:" + a(this.f32188a.array(), 32);
    }

    public b(ByteBuffer byteBuffer) {
        this.f32188a = byteBuffer;
    }
}
