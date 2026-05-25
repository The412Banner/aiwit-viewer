package n1;

import com.google.common.base.Ascii;
import com.google.common.primitives.UnsignedBytes;
// (was: import com.mbridge.msdk.playercommon.exoplayer2.source.ExtractorMediaSource;) — constant inlined
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.concurrent.locks.ReentrantLock;
import u1.t;

/* JADX INFO: compiled from: EZJetterBuffer.java */
/* JADX INFO: loaded from: classes6.dex */
public class a {

    /* JADX INFO: renamed from: b, reason: collision with root package name */
    public byte[] f32166b;

    /* JADX INFO: renamed from: c, reason: collision with root package name */
    public byte[] f32167c;

    /* JADX INFO: renamed from: n, reason: collision with root package name */
    public InterfaceC0400a f32178n;

    /* JADX INFO: renamed from: a, reason: collision with root package name */
    public final String f32165a = "EZJetterBuffer";

    /* JADX INFO: renamed from: d, reason: collision with root package name */
    public long f32168d = 0;

    /* JADX INFO: renamed from: e, reason: collision with root package name */
    public long f32169e = 0;

    /* JADX INFO: renamed from: f, reason: collision with root package name */
    public b f32170f = null;

    /* JADX INFO: renamed from: g, reason: collision with root package name */
    public b f32171g = null;

    /* JADX INFO: renamed from: h, reason: collision with root package name */
    public ArrayList<c> f32172h = new ArrayList<>();

    /* JADX INFO: renamed from: i, reason: collision with root package name */
    public ArrayList<c> f32173i = new ArrayList<>();

    /* JADX INFO: renamed from: j, reason: collision with root package name */
    public ReentrantLock f32174j = new ReentrantLock();

    /* JADX INFO: renamed from: k, reason: collision with root package name */
    public ArrayList<b> f32175k = new ArrayList<>();

    /* JADX INFO: renamed from: l, reason: collision with root package name */
    public ReentrantLock f32176l = new ReentrantLock();

    /* JADX INFO: renamed from: m, reason: collision with root package name */
    public ArrayList<b> f32177m = new ArrayList<>();

    /* JADX INFO: renamed from: o, reason: collision with root package name */
    public Boolean f32179o = Boolean.FALSE;

    /* JADX INFO: renamed from: p, reason: collision with root package name */
    public int f32180p = 0;

    /* JADX INFO: renamed from: q, reason: collision with root package name */
    public int f32181q = 0;

    /* JADX INFO: renamed from: r, reason: collision with root package name */
    public long f32182r = 0;

    /* JADX INFO: renamed from: s, reason: collision with root package name */
    public int f32183s = 0;

    /* JADX INFO: renamed from: t, reason: collision with root package name */
    public int f32184t = 0;

    /* JADX INFO: renamed from: u, reason: collision with root package name */
    public int f32185u = 0;

    /* JADX INFO: renamed from: v, reason: collision with root package name */
    public int f32186v = 0;

    /* JADX INFO: renamed from: w, reason: collision with root package name */
    public ByteBuffer f32187w = ByteBuffer.allocate(1048576);

    /* JADX INFO: renamed from: n1.a$a, reason: collision with other inner class name */
    /* JADX INFO: compiled from: EZJetterBuffer.java */
    public interface InterfaceC0400a extends EventListener {
        void onParsedAudioFrame(b bVar);

        void onParsedAudioFrameForAmr(b bVar);

        void onParsedAudioFrameForG711(b bVar);

        void onParsedAudioFrameForILBC(b bVar);

        void onParsedAudioFrameForPcm(b bVar);

        void onParsedJpegFrame(b bVar);

        void onParsedVideoFormatDescription(byte[] bArr, byte[] bArr2);

        void onParsedVideoFrame(b bVar);
    }

    public static String a(byte[] bArr, int i8) {
        if (i8 > bArr.length || i8 == 0) {
            i8 = bArr.length;
        }
        StringBuilder sb = new StringBuilder(i8 * 2);
        for (int i9 = 0; i9 < i8; i9++) {
            sb.append(String.format("%02X ", new Integer(bArr[i9] & UnsignedBytes.MAX_VALUE)));
        }
        return sb.toString();
    }

    public void b() {
        this.f32170f = null;
        this.f32166b = null;
        this.f32167c = null;
        this.f32174j.lock();
        if (this.f32172h.size() > 0) {
            for (int size = this.f32172h.size() - 1; size >= 0; size--) {
                this.f32172h.remove(0);
            }
        }
        if (this.f32175k.size() > 0) {
            for (int size2 = this.f32175k.size() - 1; size2 >= 0; size2--) {
                this.f32175k.remove(0);
            }
        }
        this.f32174j.unlock();
        this.f32176l.lock();
        if (this.f32173i.size() > 0) {
            for (int size3 = this.f32173i.size() - 1; size3 >= 0; size3--) {
                this.f32173i.remove(0);
            }
        }
        if (this.f32177m.size() > 0) {
            for (int size4 = this.f32177m.size() - 1; size4 >= 0; size4--) {
                this.f32177m.remove(0);
            }
        }
        this.f32176l.unlock();
    }

    public final void j() {
        for (int i8 = 0; i8 < this.f32173i.size(); i8++) {
            c cVar = this.f32173i.get(0);
            if (cVar != null) {
                byte[] bArrL = cVar.l();
                ByteBuffer byteBufferAllocate = ByteBuffer.allocate(bArrL.length);
                byteBufferAllocate.put(bArrL, 0, bArrL.length);
                b bVar = new b(byteBufferAllocate);
                bVar.f32193f = 1;
                if (0 == this.f32169e) {
                    this.f32169e = cVar.o();
                }
                bVar.f32189b = cVar.o() - this.f32169e;
                bVar.f32190c = cVar.m();
                bVar.f32191d = cVar.i();
                this.f32176l.lock();
                this.f32177m.add(bVar);
                this.f32176l.unlock();
                InterfaceC0400a interfaceC0400a = this.f32178n;
                if (interfaceC0400a != null) {
                    interfaceC0400a.onParsedAudioFrame(bVar);
                }
                this.f32173i.remove(0);
            }
        }
    }

    public final void m() {
        for (int i8 = 0; i8 < this.f32173i.size(); i8++) {
            c cVar = this.f32173i.get(0);
            if (cVar != null) {
                byte[] bArrL = cVar.l();
                ByteBuffer byteBufferAllocate = ByteBuffer.allocate(bArrL.length);
                byteBufferAllocate.put(bArrL, 0, bArrL.length);
                b bVar = new b(byteBufferAllocate);
                bVar.f32193f = 1;
                if (0 == this.f32169e) {
                    this.f32169e = cVar.o();
                }
                bVar.f32189b = cVar.o() - this.f32169e;
                bVar.f32190c = cVar.m();
                bVar.f32191d = cVar.i();
                this.f32176l.lock();
                this.f32177m.add(bVar);
                this.f32176l.unlock();
                InterfaceC0400a interfaceC0400a = this.f32178n;
                if (interfaceC0400a != null) {
                    interfaceC0400a.onParsedAudioFrameForAmr(bVar);
                }
                this.f32173i.remove(0);
            }
        }
    }

    public final void n() {
        for (int i8 = 0; i8 < this.f32173i.size(); i8++) {
            c cVar = this.f32173i.get(0);
            if (cVar != null) {
                byte[] bArrL = cVar.l();
                ByteBuffer byteBufferAllocate = ByteBuffer.allocate(bArrL.length);
                byteBufferAllocate.put(bArrL, 0, bArrL.length);
                b bVar = new b(byteBufferAllocate);
                bVar.f32193f = 1;
                if (0 == this.f32169e) {
                    this.f32169e = cVar.o();
                }
                bVar.f32189b = cVar.o() - this.f32169e;
                bVar.f32190c = cVar.m();
                bVar.f32191d = cVar.i();
                this.f32176l.lock();
                this.f32177m.add(bVar);
                this.f32176l.unlock();
                InterfaceC0400a interfaceC0400a = this.f32178n;
                if (interfaceC0400a != null) {
                    interfaceC0400a.onParsedAudioFrame(bVar);
                }
                this.f32173i.remove(0);
            }
        }
    }

    public final void o() {
        for (int i8 = 0; i8 < this.f32173i.size(); i8++) {
            c cVar = this.f32173i.get(0);
            if (cVar != null) {
                byte[] bArrL = cVar.l();
                ByteBuffer byteBufferAllocate = ByteBuffer.allocate(bArrL.length);
                byteBufferAllocate.put(bArrL, 0, bArrL.length);
                b bVar = new b(byteBufferAllocate);
                bVar.f32193f = 1;
                if (0 == this.f32169e) {
                    this.f32169e = cVar.o();
                }
                bVar.f32189b = cVar.o() - this.f32169e;
                bVar.f32190c = cVar.m();
                bVar.f32191d = cVar.i();
                this.f32176l.lock();
                this.f32177m.add(bVar);
                this.f32176l.unlock();
                InterfaceC0400a interfaceC0400a = this.f32178n;
                if (interfaceC0400a != null) {
                    interfaceC0400a.onParsedAudioFrameForG711(bVar);
                }
                this.f32173i.remove(0);
            }
        }
    }

    public final void r() {
        for (int i8 = 0; i8 < this.f32173i.size(); i8++) {
            c cVar = this.f32173i.get(0);
            if (cVar != null) {
                byte[] bArrL = cVar.l();
                ByteBuffer byteBufferAllocate = ByteBuffer.allocate(bArrL.length);
                byteBufferAllocate.put(bArrL, 0, bArrL.length);
                b bVar = new b(byteBufferAllocate);
                bVar.f32193f = 1;
                if (0 == this.f32169e) {
                    this.f32169e = cVar.o();
                }
                bVar.f32189b = cVar.o() - this.f32169e;
                bVar.f32190c = cVar.m();
                bVar.f32191d = cVar.i();
                this.f32176l.lock();
                this.f32177m.add(bVar);
                this.f32176l.unlock();
                InterfaceC0400a interfaceC0400a = this.f32178n;
                if (interfaceC0400a != null) {
                    interfaceC0400a.onParsedAudioFrameForILBC(bVar);
                }
                this.f32173i.remove(0);
            }
        }
    }

    public final void t() {
        for (int i8 = 0; i8 < this.f32173i.size(); i8++) {
            c cVar = this.f32173i.get(0);
            if (cVar != null) {
                byte[] bArrL = cVar.l();
                ByteBuffer byteBufferAllocate = ByteBuffer.allocate(bArrL.length);
                byteBufferAllocate.put(bArrL, 0, bArrL.length);
                b bVar = new b(byteBufferAllocate);
                bVar.f32193f = 1;
                if (0 == this.f32169e) {
                    this.f32169e = cVar.o();
                }
                bVar.f32189b = cVar.o() - this.f32169e;
                bVar.f32190c = cVar.m();
                bVar.f32191d = cVar.i();
                this.f32176l.lock();
                this.f32177m.add(bVar);
                this.f32176l.unlock();
                InterfaceC0400a interfaceC0400a = this.f32178n;
                if (interfaceC0400a != null) {
                    interfaceC0400a.onParsedAudioFrameForPcm(bVar);
                }
                this.f32173i.remove(0);
            }
        }
    }

    public synchronized void u() {
        this.f32185u = 0;
        this.f32186v = 0;
    }

    public void c() {
        this.f32174j.lock();
        this.f32172h.clear();
        this.f32175k.clear();
        this.f32174j.unlock();
        this.f32176l.lock();
        this.f32173i.clear();
        this.f32177m.clear();
        this.f32176l.unlock();
    }

    public b d() {
        this.f32176l.lock();
        if (this.f32177m.size() == 0) {
            this.f32176l.unlock();
            return null;
        }
        b bVar = this.f32177m.get(0);
        this.f32177m.remove(0);
        this.f32176l.unlock();
        return bVar;
    }

    public b e() {
        this.f32174j.lock();
        if (this.f32175k.size() == 0) {
            this.f32174j.unlock();
            return null;
        }
        b bVar = this.f32175k.get(0);
        this.f32175k.remove(0);
        this.f32174j.unlock();
        return bVar;
    }

    public void f(c cVar) {
        t.b("AAAA", "data type = " + cVar.i());
        int i8 = -2;
        int i9 = 0;
        if (96 == cVar.i()) {
            this.f32174j.lock();
            int size = this.f32172h.size();
            while (true) {
                if (i9 >= size) {
                    i8 = -1;
                    break;
                }
                c cVar2 = this.f32172h.get(i9);
                if (cVar2 != null) {
                    if (cVar.m() == cVar2.m()) {
                        break;
                    } else if (cVar.m() < cVar2.m()) {
                        i8 = i9;
                        break;
                    }
                }
                i9++;
            }
            if (i8 >= 0) {
                this.f32172h.add(i8, cVar);
                this.f32185u++;
            } else if (i8 == -1) {
                this.f32172h.add(cVar);
                this.f32185u++;
            }
            l();
            k();
            this.f32174j.unlock();
            return;
        }
        if (100 == cVar.i()) {
            this.f32174j.lock();
            int size2 = this.f32172h.size();
            while (true) {
                if (i9 >= size2) {
                    i8 = -1;
                    break;
                }
                c cVar3 = this.f32172h.get(i9);
                if (cVar3 != null) {
                    if (cVar.m() == cVar3.m()) {
                        break;
                    } else if (cVar.m() < cVar3.m()) {
                        i8 = i9;
                        break;
                    }
                }
                i9++;
            }
            if (i8 >= 0) {
                this.f32172h.add(i8, cVar);
                this.f32185u++;
            } else if (i8 == -1) {
                this.f32172h.add(cVar);
                this.f32185u++;
            }
            q();
            p();
            this.f32174j.unlock();
            return;
        }
        if (97 == cVar.i()) {
            this.f32176l.lock();
            int size3 = this.f32173i.size();
            while (true) {
                if (i9 >= size3) {
                    i8 = -1;
                    break;
                }
                c cVar4 = this.f32173i.get(i9);
                if (cVar.m() == cVar4.m()) {
                    break;
                }
                if (cVar.m() < cVar4.m()) {
                    i8 = i9;
                    break;
                }
                i9++;
            }
            if (i8 >= 0) {
                this.f32173i.add(i8, cVar);
            } else if (i8 == -1) {
                this.f32173i.add(cVar);
            }
            j();
            this.f32176l.unlock();
            return;
        }
        if (101 == cVar.i()) {
            this.f32176l.lock();
            int size4 = this.f32173i.size();
            while (true) {
                if (i9 >= size4) {
                    i8 = -1;
                    break;
                }
                c cVar5 = this.f32173i.get(i9);
                if (cVar.m() == cVar5.m()) {
                    break;
                }
                if (cVar.m() < cVar5.m()) {
                    i8 = i9;
                    break;
                }
                i9++;
            }
            if (i8 >= 0) {
                this.f32173i.add(i8, cVar);
            } else if (i8 == -1) {
                this.f32173i.add(cVar);
            }
            m();
            this.f32176l.unlock();
            return;
        }
        if (102 == cVar.i()) {
            this.f32174j.lock();
            int size5 = this.f32172h.size();
            while (true) {
                if (i9 >= size5) {
                    i8 = -1;
                    break;
                }
                c cVar6 = this.f32172h.get(i9);
                if (cVar6 != null) {
                    if (cVar.m() == cVar6.m()) {
                        break;
                    } else if (cVar.m() < cVar6.m()) {
                        i8 = i9;
                        break;
                    }
                }
                i9++;
            }
            if (i8 >= 0) {
                this.f32172h.add(i8, cVar);
                this.f32185u++;
            } else if (i8 == -1) {
                this.f32172h.add(cVar);
                this.f32185u++;
            }
            s();
            this.f32174j.unlock();
            return;
        }
        if (103 == cVar.i()) {
            this.f32176l.lock();
            int size6 = this.f32173i.size();
            while (true) {
                if (i9 >= size6) {
                    i8 = -1;
                    break;
                }
                c cVar7 = this.f32173i.get(i9);
                if (cVar.m() == cVar7.m()) {
                    break;
                }
                if (cVar.m() < cVar7.m()) {
                    i8 = i9;
                    break;
                }
                i9++;
            }
            if (i8 >= 0) {
                this.f32173i.add(i8, cVar);
            } else if (i8 == -1) {
                this.f32173i.add(cVar);
            }
            t();
            this.f32176l.unlock();
            return;
        }
        if (105 == cVar.i()) {
            this.f32176l.lock();
            int size7 = this.f32173i.size();
            while (true) {
                if (i9 >= size7) {
                    i8 = -1;
                    break;
                }
                c cVar8 = this.f32173i.get(i9);
                if (cVar.m() == cVar8.m()) {
                    break;
                }
                if (cVar.m() < cVar8.m()) {
                    i8 = i9;
                    break;
                }
                i9++;
            }
            if (i8 >= 0) {
                this.f32173i.add(i8, cVar);
            } else if (i8 == -1) {
                this.f32173i.add(cVar);
            }
            r();
            this.f32176l.unlock();
            return;
        }
        if (101 == cVar.i()) {
            this.f32176l.lock();
            int size8 = this.f32173i.size();
            while (true) {
                if (i9 >= size8) {
                    i8 = -1;
                    break;
                }
                c cVar9 = this.f32173i.get(i9);
                if (cVar.m() == cVar9.m()) {
                    break;
                }
                if (cVar.m() < cVar9.m()) {
                    i8 = i9;
                    break;
                }
                i9++;
            }
            if (i8 >= 0) {
                this.f32173i.add(i8, cVar);
            } else if (i8 == -1) {
                this.f32173i.add(cVar);
            }
            n();
            this.f32176l.unlock();
            return;
        }
        if (107 == cVar.i()) {
            this.f32176l.lock();
            int size9 = this.f32173i.size();
            while (true) {
                if (i9 >= size9) {
                    i8 = -1;
                    break;
                }
                c cVar10 = this.f32173i.get(i9);
                if (cVar.m() == cVar10.m()) {
                    break;
                }
                if (cVar.m() < cVar10.m()) {
                    i8 = i9;
                    break;
                }
                i9++;
            }
            if (i8 >= 0) {
                this.f32173i.add(i8, cVar);
            } else if (i8 == -1) {
                this.f32173i.add(cVar);
            }
            o();
            this.f32176l.unlock();
        }
    }

    public int[] g() {
        return new int[]{this.f32186v, this.f32185u};
    }

    public b h() {
        b bVar;
        ByteBuffer byteBufferAllocate;
        if (this.f32166b == null || (bVar = this.f32170f) == null) {
            return null;
        }
        byte[] bArrArray = bVar.b().array();
        byte[] bArr = this.f32167c;
        if (bArr == null) {
            byteBufferAllocate = ByteBuffer.allocate(this.f32166b.length + bArrArray.length);
            byteBufferAllocate.put(this.f32166b);
        } else {
            byteBufferAllocate = ByteBuffer.allocate(this.f32166b.length + bArr.length + bArrArray.length);
            byteBufferAllocate.put(this.f32166b);
            byteBufferAllocate.put(this.f32167c);
        }
        byteBufferAllocate.put(bArrArray);
        b bVar2 = new b(byteBufferAllocate);
        bVar2.f32193f = 1;
        bVar2.f32189b = this.f32170f.f32189b;
        return bVar2;
    }

    public b i() {
        b bVar;
        ByteBuffer byteBufferAllocate;
        if (this.f32166b == null || (bVar = this.f32171g) == null) {
            return null;
        }
        byte[] bArrArray = bVar.b().array();
        byte[] bArr = this.f32167c;
        if (bArr == null) {
            byteBufferAllocate = ByteBuffer.allocate(this.f32166b.length + bArrArray.length);
            byteBufferAllocate.put(this.f32166b);
        } else {
            byteBufferAllocate = ByteBuffer.allocate(this.f32166b.length + bArr.length + bArrArray.length);
            byteBufferAllocate.put(this.f32166b);
            byteBufferAllocate.put(this.f32167c);
        }
        byteBufferAllocate.put(bArrArray);
        b bVar2 = new b(byteBufferAllocate);
        bVar2.f32193f = 1;
        bVar2.f32189b = this.f32171g.f32189b;
        return bVar2;
    }

    /* JADX WARN: Removed duplicated region for block: B:119:0x02c3  */
    /* JADX WARN: Removed duplicated region for block: B:122:0x02e0  */
    /* JADX WARN: Removed duplicated region for block: B:134:0x030a A[SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public final void k() {
        ByteBuffer byteBuffer;
        char c8;
        long j8;
        int i8;
        long j9;
        long j10;
        char c9;
        do {
            this.f32187w.clear();
            byteBuffer = this.f32187w;
            int size = this.f32172h.size();
            c8 = 0;
            int i9 = 0;
            boolean z7 = false;
            int length = 0;
            int iM = 0;
            long jO = 0;
            long jO2 = 0;
            long jO3 = 0;
            while (i9 < size) {
                c cVar = this.f32172h.get(i9);
                if (cVar != null) {
                    byte[] bArrL = cVar.l();
                    if (i9 == 0) {
                        jO2 = cVar.o();
                    } else {
                        jO3 = cVar.o();
                    }
                    int i10 = bArrL[c8] & Ascii.US;
                    i8 = size;
                    j9 = jO2;
                    if (23 >= i10) {
                        if (5 == i10) {
                            ByteBuffer byteBufferAllocate = ByteBuffer.allocate(bArrL.length + 4);
                            byteBufferAllocate.putInt(1);
                            byteBufferAllocate.put(bArrL, 0, bArrL.length);
                            b bVar = new b(byteBufferAllocate);
                            bVar.f32189b = cVar.o();
                            bVar.f32193f = 1;
                            bVar.f32192e = cVar.h();
                            if (0 == this.f32168d) {
                                this.f32168d = cVar.o();
                            }
                            bVar.f32189b = cVar.o() - this.f32168d;
                            this.f32170f = bVar;
                            this.f32175k.add(bVar);
                            InterfaceC0400a interfaceC0400a = this.f32178n;
                            if (interfaceC0400a != null) {
                                interfaceC0400a.onParsedVideoFrame(bVar);
                            }
                            this.f32179o = Boolean.TRUE;
                        } else if (!this.f32179o.booleanValue() && !z7) {
                            System.out.println("jetter: has no key frame drop single(type:)" + i10 + " packet");
                        } else if (this.f32179o.booleanValue()) {
                            if (byteBuffer != null) {
                                byteBuffer.clear();
                            }
                            ByteBuffer byteBufferAllocate2 = ByteBuffer.allocate(bArrL.length + 4);
                            byteBufferAllocate2.putInt(1);
                            byteBufferAllocate2.put(bArrL, 0, bArrL.length);
                            b bVar2 = new b(byteBufferAllocate2);
                            bVar2.f32189b = cVar.o();
                            bVar2.f32192e = cVar.h();
                            bVar2.f32193f = 0;
                            if (0 == this.f32168d) {
                                this.f32168d = cVar.o();
                            }
                            bVar2.f32189b = cVar.o() - this.f32168d;
                            this.f32175k.add(bVar2);
                            InterfaceC0400a interfaceC0400a2 = this.f32178n;
                            if (interfaceC0400a2 != null) {
                                interfaceC0400a2.onParsedVideoFrame(bVar2);
                            }
                        }
                        this.f32172h.remove(i9);
                    } else if (24 == i10) {
                        this.f32172h.remove(i9);
                    } else {
                        if (28 == i10) {
                            byte b8 = bArrL[1];
                            int i11 = b8 & Ascii.US;
                            j10 = jO3;
                            if (-2 == (b8 >> 6)) {
                                if (5 == i11) {
                                    this.f32179o = Boolean.FALSE;
                                    if (!z7) {
                                        z7 = true;
                                    }
                                } else if (1 == i11 && !this.f32179o.booleanValue() && !z7) {
                                    System.out.println("jetter: has no key frame drop slice frame");
                                    this.f32172h.remove(i9);
                                    this.f32186v++;
                                    jO2 = j9;
                                    jO3 = j10;
                                    c8 = 1;
                                    break;
                                }
                                iM = cVar.m();
                                jO = cVar.o();
                                if (length != 0) {
                                    byteBuffer.clear();
                                }
                                byteBuffer.putInt(1);
                                byteBuffer.put((byte) ((bArrL[1] & Ascii.US) | 96));
                                byteBuffer.put(bArrL, 2, bArrL.length - 2);
                                length = bArrL.length + 3;
                                jO3 = j10;
                                c9 = 0;
                            } else if ((b8 >> 6) == 0) {
                                if (1 == i11 && !this.f32179o.booleanValue() && !z7) {
                                    System.out.println("jetter: has no key frame drop slice frame");
                                    this.f32186v++;
                                    this.f32172h.remove(i9);
                                    jO2 = j9;
                                    jO3 = j10;
                                    c8 = 1;
                                    break;
                                }
                                if (jO != cVar.o()) {
                                    System.out.println("jetter: midd: no match slice");
                                } else if (length == 0) {
                                    System.out.println("jetter: midd: no start slice");
                                } else if (1 != cVar.m() - iM) {
                                    System.out.println("jetter: midd: no continue slice");
                                } else {
                                    iM = cVar.m();
                                    byteBuffer.put(bArrL, 2, bArrL.length - 2);
                                    length += bArrL.length - 2;
                                    jO3 = j10;
                                    c9 = 0;
                                }
                            } else if (1 == (b8 >> 6)) {
                                if (1 == i11 && !this.f32179o.booleanValue() && !z7) {
                                    System.out.println("jetter: has no key frame drop slice frame");
                                    this.f32186v++;
                                    this.f32172h.remove(i9);
                                    jO2 = j9;
                                    jO3 = j10;
                                    c8 = 1;
                                    break;
                                }
                                if (jO != cVar.o()) {
                                    System.out.println("jetter: tail: no match slice");
                                } else if (length == 0) {
                                    System.out.println("jetter: tail: no start slice");
                                } else if (1 != cVar.m() - iM) {
                                    System.out.println("jetter: tail: no continue slice");
                                } else {
                                    if (5 == i11) {
                                        this.f32179o = Boolean.TRUE;
                                    }
                                    int length2 = length + (bArrL.length - 2);
                                    cVar.m();
                                    byteBuffer.put(bArrL, 2, bArrL.length - 2);
                                    ByteBuffer byteBufferAllocate3 = ByteBuffer.allocate(length2);
                                    byteBufferAllocate3.put(byteBuffer.array(), 0, length2);
                                    byteBuffer.clear();
                                    b bVar3 = new b(byteBufferAllocate3);
                                    bVar3.f32189b = cVar.o();
                                    bVar3.f32192e = cVar.h();
                                    bVar3.f32193f = 0;
                                    if (5 == i11) {
                                        bVar3.f32193f = 1;
                                        if (cVar.h() == 1) {
                                            this.f32171g = bVar3;
                                        } else {
                                            this.f32170f = bVar3;
                                        }
                                    }
                                    if (0 == this.f32168d) {
                                        this.f32168d = cVar.o();
                                    }
                                    bVar3.f32189b = cVar.o() - this.f32168d;
                                    this.f32175k.add(bVar3);
                                    InterfaceC0400a interfaceC0400a3 = this.f32178n;
                                    if (interfaceC0400a3 != null) {
                                        interfaceC0400a3.onParsedVideoFrame(bVar3);
                                    }
                                    jO2 = j9;
                                    jO3 = j10;
                                    c8 = 1;
                                    j8 = jO3 - jO2;
                                    if (j8 > 500) {
                                        System.out.println("jetter: last_ts-first_ts=" + j8);
                                        jO = jO3;
                                    }
                                    if (jO <= 0) {
                                        for (int size2 = this.f32172h.size() - 1; size2 >= 0; size2--) {
                                            if (this.f32172h.get(size2).o() <= jO) {
                                                this.f32172h.remove(size2);
                                                this.f32186v++;
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            j10 = jO3;
                        }
                        c9 = 0;
                        jO3 = j10;
                    }
                    jO2 = j9;
                    c8 = 1;
                    break;
                }
                i8 = size;
                j9 = jO2;
                c9 = c8;
                i9++;
                c8 = c9;
                size = i8;
                jO2 = j9;
            }
            jO = 0;
            j8 = jO3 - jO2;
            if (j8 > 500) {
            }
            if (jO <= 0) {
            }
        } while (c8 != 0);
        if (byteBuffer != null) {
            byteBuffer.clear();
        }
    }

    public final void l() {
        for (int size = this.f32172h.size() - 1; size >= 0; size--) {
            c cVar = this.f32172h.get(size);
            if (cVar != null) {
                byte[] bArrL = cVar.l();
                if (24 == (bArrL[0] & Ascii.US)) {
                    try {
                        byte[] bArr = this.f32167c;
                        if (bArr == null || bArr.length == 0) {
                            int iB = c.b(new byte[]{bArrL[2], bArrL[1]});
                            byte[] bArr2 = new byte[iB + 4];
                            bArr2[0] = 0;
                            bArr2[1] = 0;
                            bArr2[2] = 0;
                            bArr2[3] = 1;
                            System.arraycopy(bArrL, 3, bArr2, 4, iB);
                            int iB2 = c.b(new byte[]{bArrL[iB + 4], bArrL[iB + 3]});
                            byte[] bArr3 = new byte[iB2 + 4];
                            bArr3[0] = 0;
                            bArr3[1] = 0;
                            bArr3[2] = 0;
                            bArr3[3] = 1;
                            System.arraycopy(bArrL, iB + 5, bArr3, 4, iB2);
                            this.f32166b = bArr2;
                            this.f32167c = bArr3;
                            InterfaceC0400a interfaceC0400a = this.f32178n;
                            if (interfaceC0400a != null) {
                                interfaceC0400a.onParsedVideoFormatDescription(bArr2, bArr3);
                            }
                        }
                        this.f32172h.remove(size);
                    } catch (Exception unused) {
                    }
                }
            }
        }
    }

    public final void p() {
        ByteBuffer byteBuffer;
        char c8;
        long jO;
        int i8;
        long j8;
        long j9;
        long jO2;
        char c9;
        int i9;
        do {
            this.f32187w.clear();
            byteBuffer = this.f32187w;
            int size = this.f32172h.size();
            c8 = 0;
            int i10 = 0;
            boolean z7 = false;
            int length = 0;
            int iM = 0;
            long jO3 = 0;
            long jO4 = 0;
            long jO5 = 0;
            while (i10 < size) {
                c cVar = this.f32172h.get(i10);
                if (cVar != null) {
                    byte[] bArrL = cVar.l();
                    if (i10 == 0) {
                        jO4 = cVar.o();
                    } else {
                        jO5 = cVar.o();
                    }
                    i8 = size;
                    byte b8 = bArrL[c8];
                    int i11 = (b8 & 254) >> 1;
                    j8 = jO4;
                    if (32 != i11 && 33 != i11 && 34 != i11) {
                        if (48 == i11) {
                            this.f32172h.remove(i10);
                        } else {
                            if (49 == i11) {
                                byte b9 = bArrL[2];
                                int i12 = b9 & 63;
                                if (-2 == (b9 >> 6)) {
                                    if (19 == i12) {
                                        this.f32179o = Boolean.FALSE;
                                        if (z7) {
                                            j9 = jO5;
                                            c9 = 0;
                                            jO5 = j9;
                                        } else {
                                            z7 = true;
                                        }
                                    } else if (!this.f32179o.booleanValue() && !z7) {
                                        System.out.println("jetter: has no key frame drop slice frame");
                                        this.f32172h.remove(i10);
                                    }
                                    this.f32183s = cVar.m();
                                    iM = cVar.m();
                                    jO3 = cVar.o();
                                    byteBuffer.clear();
                                    byteBuffer.putInt(1);
                                    byteBuffer.put((byte) ((bArrL[2] & 63) << 1));
                                    byteBuffer.put((byte) 1);
                                    byteBuffer.put(bArrL, 3, bArrL.length - 3);
                                    length = bArrL.length + 3;
                                    j9 = jO5;
                                    jO5 = j9;
                                    c9 = 0;
                                } else if ((b9 >> 6) != 0) {
                                    j9 = jO5;
                                    if (1 == (b9 >> 6)) {
                                        if (19 == i12 || this.f32179o.booleanValue() || z7) {
                                            if (jO3 != cVar.o()) {
                                                System.out.println("jetter: tail: no match slice");
                                            } else if (length == 0 || 0 == jO3) {
                                                System.out.println("jetter: tail: no start slice");
                                            } else if (1 != cVar.m() - iM) {
                                                System.out.println("jetter: tail: no continue slice");
                                            } else {
                                                if (19 == i12) {
                                                    this.f32179o = Boolean.TRUE;
                                                }
                                                int length2 = length + (bArrL.length - 3);
                                                this.f32184t = cVar.m();
                                                int iM2 = cVar.m();
                                                byteBuffer.put(bArrL, 3, bArrL.length - 3);
                                                ByteBuffer byteBufferAllocate = ByteBuffer.allocate(length2);
                                                byteBufferAllocate.put(byteBuffer.array(), 0, length2);
                                                b bVar = new b(byteBufferAllocate);
                                                bVar.f32190c = iM2;
                                                bVar.f32189b = cVar.o();
                                                bVar.f32192e = cVar.h();
                                                bVar.f32193f = 0;
                                                if (19 == i12) {
                                                    bVar.f32193f = 1;
                                                    this.f32170f = bVar;
                                                    this.f32180p = bVar.f32190c;
                                                    this.f32182r = bVar.f32189b;
                                                    this.f32181q = 0;
                                                    i9 = 0;
                                                } else {
                                                    i9 = (this.f32184t - this.f32183s) + 1;
                                                }
                                                if (0 == this.f32168d) {
                                                    this.f32168d = cVar.o();
                                                }
                                                InterfaceC0400a interfaceC0400a = this.f32178n;
                                                if (interfaceC0400a != null) {
                                                    if (19 == i12) {
                                                        interfaceC0400a.onParsedVideoFrame(bVar);
                                                    } else if (bVar.f32190c == this.f32180p + this.f32181q + i9) {
                                                        interfaceC0400a.onParsedVideoFrame(bVar);
                                                        this.f32181q += i9;
                                                    }
                                                }
                                                byteBuffer.clear();
                                                jO = cVar.o();
                                                jO4 = j8;
                                                jO5 = j9;
                                            }
                                            c9 = 0;
                                            jO5 = j9;
                                        } else {
                                            this.f32172h.remove(i10);
                                            jO4 = j8;
                                            jO5 = j9;
                                            jO = 0;
                                        }
                                    }
                                    jO5 = j9;
                                    c9 = 0;
                                } else if (19 == i12 || this.f32179o.booleanValue() || z7) {
                                    if (jO3 != cVar.o()) {
                                        PrintStream printStream = System.out;
                                        StringBuilder sb = new StringBuilder();
                                        j9 = jO5;
                                        sb.append("jetter: midd: no match slice tmp_ts: ");
                                        sb.append(jO3);
                                        sb.append(" getmTS:");
                                        sb.append(cVar.o());
                                        sb.append(" seq_no:");
                                        sb.append(iM);
                                        sb.append(" getmSQNum:");
                                        sb.append(cVar.m());
                                        printStream.println(sb.toString());
                                    } else {
                                        j9 = jO5;
                                        if (length == 0 || 0 == jO3) {
                                            System.out.println("jetter: midd: no start slice");
                                        } else if (1 != cVar.m() - iM) {
                                            System.out.println("jetter: midd: no continue slice tmp_ts: " + jO3 + " getmTS:" + cVar.o() + " seq_no:" + iM + " getmSQNum:" + cVar.m());
                                        } else {
                                            iM = cVar.m();
                                            byteBuffer.put(bArrL, 3, bArrL.length - 3);
                                            length += bArrL.length - 3;
                                            jO5 = j9;
                                            c9 = 0;
                                        }
                                    }
                                    c9 = 0;
                                    jO5 = j9;
                                } else {
                                    this.f32172h.remove(i10);
                                }
                                c8 = 1;
                                break;
                            }
                            j9 = jO5;
                            if (1 == i11) {
                                int i13 = b8 & 63;
                                if (19 != i13) {
                                    if (!this.f32179o.booleanValue() && !z7) {
                                        this.f32172h.remove(i10);
                                        jO4 = j8;
                                        jO5 = j9;
                                        jO = 0;
                                        c8 = 1;
                                        break;
                                    }
                                    jO2 = 0;
                                } else {
                                    this.f32179o = Boolean.TRUE;
                                    jO2 = cVar.o();
                                }
                                ByteBuffer byteBufferAllocate2 = ByteBuffer.allocate(bArrL.length + 4);
                                byteBufferAllocate2.putInt(1);
                                byteBufferAllocate2.put(bArrL, 0, bArrL.length);
                                b bVar2 = new b(byteBufferAllocate2);
                                bVar2.f32193f = 0;
                                if (19 == i13) {
                                    bVar2.f32193f = 1;
                                    this.f32170f = bVar2;
                                }
                                if (0 == this.f32168d) {
                                    this.f32168d = cVar.o();
                                }
                                bVar2.f32190c = cVar.m();
                                bVar2.f32189b = cVar.o() - this.f32168d;
                                bVar2.f32192e = cVar.h();
                                InterfaceC0400a interfaceC0400a2 = this.f32178n;
                                if (interfaceC0400a2 != null && bVar2.f32190c == this.f32180p + this.f32181q + 1) {
                                    interfaceC0400a2.onParsedVideoFrame(bVar2);
                                    this.f32181q++;
                                }
                                this.f32172h.remove(i10);
                                jO = jO2;
                                jO4 = j8;
                                jO5 = j9;
                                c8 = 1;
                                break;
                            }
                            c9 = 0;
                            jO5 = j9;
                        }
                        jO4 = j8;
                        jO = 0;
                        c8 = 1;
                        break;
                    }
                    j9 = jO5;
                    this.f32172h.remove(i10);
                    jO4 = j8;
                    jO5 = j9;
                    jO = 0;
                    c8 = 1;
                    break;
                }
                i8 = size;
                j8 = jO4;
                c9 = c8;
                i10++;
                c8 = c9;
                size = i8;
                jO4 = j8;
            }
            jO = 0;
            long j10 = jO5 - jO4;
            if (j10 > 500) {
                System.out.println("jetter: last_ts-first_ts=" + j10);
            } else {
                jO5 = jO;
            }
            if (jO5 > 0) {
                for (int size2 = this.f32172h.size() - 1; size2 >= 0; size2--) {
                    if (this.f32172h.get(size2).o() <= jO5) {
                        this.f32172h.remove(size2);
                        this.f32186v++;
                    }
                }
            }
        } while (c8 != 0);
        if (byteBuffer != null) {
            byteBuffer.clear();
        }
    }

    public final void q() {
        if (this.f32166b != null) {
            return;
        }
        int size = this.f32172h.size();
        for (int i8 = 0; i8 < size; i8++) {
            c cVar = this.f32172h.get(i8);
            if (cVar != null) {
                byte[] bArrL = cVar.l();
                if (48 == ((bArrL[0] & 254) >> 1)) {
                    try {
                        byte[] bArr = {bArrL[3], bArrL[2]};
                        int iB = c.b(bArr);
                        int i9 = iB + 4;
                        byte[] bArr2 = new byte[i9];
                        bArr2[0] = 0;
                        bArr2[1] = 0;
                        bArr2[2] = 0;
                        bArr2[3] = 1;
                        System.arraycopy(bArrL, 4, bArr2, 4, iB);
                        bArr[0] = bArrL[iB + 5];
                        bArr[1] = bArrL[i9];
                        int iB2 = c.b(bArr);
                        int i10 = iB2 + 4;
                        byte[] bArr3 = new byte[i10];
                        bArr3[0] = 0;
                        bArr3[1] = 0;
                        bArr3[2] = 0;
                        bArr3[3] = 1;
                        int i11 = iB + 6;
                        System.arraycopy(bArrL, i11, bArr3, 4, iB2);
                        int i12 = i11 + iB2;
                        bArr[0] = bArrL[i12 + 1];
                        bArr[1] = bArrL[i12];
                        int iB3 = c.b(bArr);
                        int i13 = iB3 + 4;
                        byte[] bArr4 = new byte[i13];
                        bArr4[0] = 0;
                        bArr4[1] = 0;
                        bArr4[2] = 0;
                        bArr4[3] = 1;
                        System.arraycopy(bArrL, i12 + 2, bArr4, 4, iB3);
                        ByteBuffer byteBufferAllocate = ByteBuffer.allocate(i9 + i10 + i13);
                        byteBufferAllocate.put(bArr2, 0, i9);
                        byteBufferAllocate.put(bArr3, 0, i10);
                        byteBufferAllocate.put(bArr4, 0, i13);
                        byte[] bArrArray = byteBufferAllocate.array();
                        this.f32166b = bArrArray;
                        InterfaceC0400a interfaceC0400a = this.f32178n;
                        if (interfaceC0400a != null) {
                            interfaceC0400a.onParsedVideoFormatDescription(bArrArray, this.f32167c);
                        }
                        this.f32172h.remove(i8);
                        return;
                    } catch (Exception e8) {
                        e8.printStackTrace();
                    }
                } else {
                    continue;
                }
            }
        }
    }

    public final void s() {
        this.f32187w.clear();
        ByteBuffer byteBuffer = this.f32187w;
        int size = this.f32172h.size();
        for (int i8 = 0; i8 < size; i8++) {
            c cVar = this.f32172h.get(i8);
            if (cVar != null) {
                int i9 = 1;
                if (cVar.g() == 1) {
                    long jO = cVar.o();
                    int iM = cVar.m();
                    System.out.println("idx :" + i8 + "ts :" + jO + "seq " + iM);
                    c cVar2 = null;
                    c cVar3 = null;
                    int i10 = 0;
                    int length = 0;
                    int i11 = 0;
                    for (int i12 = 0; i12 <= i8; i12++) {
                        c cVar4 = this.f32172h.get(i12);
                        if (cVar4.o() != jO || iM < cVar4.m()) {
                            i9 = 1;
                            this.f32186v++;
                        } else {
                            if (i10 == 0) {
                                i10 = i9;
                                cVar3 = cVar4;
                            }
                            if (cVar4.g() == i9) {
                                cVar2 = cVar4;
                            }
                            byte[] bArrL = cVar4.l();
                            i11++;
                            byteBuffer.put(bArrL, 0, bArrL.length);
                            length += bArrL.length;
                            i9 = 1;
                        }
                    }
                    for (int i13 = size - i9; i13 >= 0; i13--) {
                        if (this.f32172h.get(i13).o() <= jO) {
                            this.f32172h.remove(i13);
                        }
                    }
                    ByteBuffer byteBufferAllocate = ByteBuffer.allocate(length);
                    byteBufferAllocate.put(byteBuffer.array(), 0, length);
                    b bVar = new b(byteBufferAllocate);
                    bVar.f32190c = iM;
                    bVar.f32189b = jO;
                    if (this.f32178n == null || cVar2 == null || cVar3 == null || cVar2.m() - cVar3.m() != i11 - 1) {
                        return;
                    }
                    this.f32178n.onParsedJpegFrame(bVar);
                    System.out.println("jetter:enter single: " + bVar.f32190c + " ts:" + bVar.f32189b + " len:" + bVar.b().array().length);
                    return;
                }
            }
        }
    }

    public void v(InterfaceC0400a interfaceC0400a) {
        this.f32178n = interfaceC0400a;
    }
}
