package n1;

import android.text.TextUtils;
import cn.coderfly.ezmediautils.EZMediaUtils;
import com.eken.doorbell.bean.RectInfo;
import com.google.common.base.Ascii;
import com.google.common.primitives.UnsignedBytes;
// (was: import com.mbridge.msdk.MBridgeConstans;) — value inlined as "0"
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.json.JSONArray;
import org.json.JSONObject;
import u1.o;

/* JADX INFO: compiled from: RTPData.java */
/* JADX INFO: loaded from: classes6.dex */
public class c {

    /* JADX INFO: renamed from: a, reason: collision with root package name */
    public byte[] f32194a;

    /* JADX INFO: renamed from: b, reason: collision with root package name */
    public int f32195b;

    /* JADX INFO: renamed from: c, reason: collision with root package name */
    public long f32196c;

    /* JADX INFO: renamed from: d, reason: collision with root package name */
    public int f32197d = 0;

    /* JADX INFO: renamed from: e, reason: collision with root package name */
    public int f32198e = 0;

    /* JADX INFO: renamed from: f, reason: collision with root package name */
    public int f32199f;

    /* JADX INFO: renamed from: g, reason: collision with root package name */
    public int f32200g;

    /* JADX INFO: renamed from: h, reason: collision with root package name */
    public List<RectInfo> f32201h;

    public static byte a(String str) {
        if (str == null) {
            return (byte) 0;
        }
        int length = str.length();
        if (length != 4 && length != 8) {
            return (byte) 0;
        }
        int i8 = (length != 8 || str.charAt(0) == '0') ? Integer.parseInt(str, 2) : Integer.parseInt(str, 2) - 256;
        return (byte) i8;
    }

    public static int b(byte[] bArr) {
        int i8;
        int i9;
        if (bArr.length == 1) {
            return bArr[0] & UnsignedBytes.MAX_VALUE;
        }
        if (bArr.length == 2) {
            i8 = bArr[0] & UnsignedBytes.MAX_VALUE;
            i9 = (bArr[1] & UnsignedBytes.MAX_VALUE) << 8;
        } else if (bArr.length == 3) {
            i8 = (bArr[0] & UnsignedBytes.MAX_VALUE) | ((bArr[1] & UnsignedBytes.MAX_VALUE) << 8);
            i9 = (bArr[2] & UnsignedBytes.MAX_VALUE) << 16;
        } else {
            if (bArr.length != 4) {
                return 0;
            }
            i8 = (bArr[0] & UnsignedBytes.MAX_VALUE) | ((bArr[1] & UnsignedBytes.MAX_VALUE) << 8) | ((bArr[2] & UnsignedBytes.MAX_VALUE) << 16);
            i9 = (bArr[3] & UnsignedBytes.MAX_VALUE) << 24;
        }
        return i8 | i9;
    }

    public static byte p(int i8) {
        return (byte) i8;
    }

    public static int c(byte b8) {
        return b8 & UnsignedBytes.MAX_VALUE;
    }

    public static byte[] d(byte[] bArr, int i8, int i9, int i10) {
        byte[] bArr2 = new byte[12];
        bArr2[0] = p(128);
        byte bT = o.T(97);
        if (98 == i10) {
            bT = o.T(98);
        } else if (88 == i10) {
            bT = o.T(88);
        } else if (105 == i10) {
            bT = o.T(105);
        } else if (115 == i10) {
            bT = o.T(115);
        } else if (107 == i10) {
            bT = o.T(107);
        }
        bArr2[1] = o.d(o.q(bT).replaceFirst("0", "1"));
        byte[] bArrC = o.c(i9);
        bArr2[3] = bArrC[0];
        bArr2[2] = bArrC[1];
        byte[] bArrC2 = o.c(i8);
        bArr2[4] = bArrC2[0];
        bArr2[5] = bArrC2[1];
        bArr2[6] = bArrC2[2];
        bArr2[7] = bArrC2[3];
        byte[] bArrC3 = o.c(new Random().nextInt());
        bArr2[8] = bArrC3[0];
        bArr2[9] = bArrC3[1];
        bArr2[10] = bArrC3[2];
        bArr2[11] = bArrC3[3];
        System.arraycopy(bArr2, 0, bArr, 0, 12);
        return bArr;
    }

    public static String e(byte b8) {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append((b8 >> 7) & 1);
        stringBuffer.append((b8 >> 6) & 1);
        stringBuffer.append((b8 >> 5) & 1);
        stringBuffer.append((b8 >> 4) & 1);
        stringBuffer.append((b8 >> 3) & 1);
        stringBuffer.append((b8 >> 2) & 1);
        stringBuffer.append((b8 >> 1) & 1);
        stringBuffer.append(b8 & 1);
        return stringBuffer.toString();
    }

    public static c k(byte[] bArr, String str) {
        byte[] bArr2 = new byte[12];
        System.arraycopy(bArr, 0, bArr2, 0, 12);
        f(bArr2[1], 1);
        if (128 <= c(bArr[0]) && c(bArr[0]) <= 131) {
            c cVar = new c();
            byte b8 = bArr2[1];
            int i8 = b8 & Ascii.DEL;
            cVar.f32197d = i8;
            cVar.f32198e = bArr2[0] & 3;
            if (i8 != 96 && i8 != 97 && i8 != 95 && i8 != 100 && i8 != 101 && i8 != 102 && i8 != 103 && i8 != 105 && i8 != 107) {
                return null;
            }
            cVar.f32199f = (128 & b8) >> 7;
            cVar.f32195b = b(new byte[]{bArr2[3], bArr2[2]});
            cVar.f32196c = t(new byte[]{bArr2[4], bArr2[5], bArr2[6], bArr2[7]}, 0);
            cVar.s(t(new byte[]{bArr2[11], bArr2[10], bArr2[9], bArr2[8]}, 0));
            int length = bArr.length - 12;
            byte[] bArr3 = new byte[length];
            System.arraycopy(bArr, 12, bArr3, 0, length);
            if (TextUtils.isEmpty(str)) {
                cVar.r(bArr3);
            } else {
                try {
                    byte[] bArrDecryptAESData = EZMediaUtils.decryptAESData(bArr3, str, cVar.n());
                    cVar.r(bArrDecryptAESData);
                    if (cVar.f32197d == 95) {
                        JSONObject jSONObject = new JSONObject(new String(bArrDecryptAESData).replace("\\\"", "'"));
                        if (jSONObject.has("nn_info")) {
                            JSONObject jSONObject2 = jSONObject.getJSONObject("nn_info");
                            if (jSONObject2.has("obj_num") && jSONObject2.has("roi") && jSONObject2.getInt("obj_num") > 0) {
                                ArrayList arrayList = new ArrayList();
                                JSONArray jSONArray = jSONObject2.getJSONArray("roi");
                                if (jSONArray != null && jSONArray.length() > 0) {
                                    for (int i9 = 0; i9 < jSONArray.length(); i9++) {
                                        RectInfo rectInfo = new RectInfo();
                                        JSONObject jSONObject3 = jSONArray.getJSONObject(i9);
                                        rectInfo.setXOffset(jSONObject3.getInt("x"));
                                        rectInfo.setYOffset(jSONObject3.getInt("y"));
                                        rectInfo.setWidthOffset(jSONObject3.getInt("w"));
                                        rectInfo.setHeightOffset(jSONObject3.getInt("h"));
                                        rectInfo.setConf(jSONObject3.getInt("conf"));
                                        arrayList.add(rectInfo);
                                    }
                                    cVar.q(arrayList);
                                }
                            }
                        }
                    }
                } catch (Exception e8) {
                    e8.printStackTrace();
                }
            }
            return cVar;
        }
        return null;
    }

    public static int t(byte[] bArr, int i8) {
        return (bArr[i8 + 3] & UnsignedBytes.MAX_VALUE) | ((bArr[i8] & UnsignedBytes.MAX_VALUE) << 24) | ((bArr[i8 + 1] & UnsignedBytes.MAX_VALUE) << 16) | ((bArr[i8 + 2] & UnsignedBytes.MAX_VALUE) << 8);
    }

    public int g() {
        return this.f32199f;
    }

    public int h() {
        return this.f32198e;
    }

    public int i() {
        return this.f32197d;
    }

    public List<RectInfo> j() {
        return this.f32201h;
    }

    public byte[] l() {
        return this.f32194a;
    }

    public int m() {
        return this.f32195b;
    }

    public int n() {
        return this.f32200g;
    }

    public long o() {
        return this.f32196c;
    }

    public void q(List<RectInfo> list) {
        this.f32201h = list;
    }

    public void r(byte[] bArr) {
        this.f32194a = bArr;
    }

    public void s(int i8) {
        this.f32200g = i8;
    }

    public String toString() {
        return "type:" + this.f32197d + " seq_no:" + this.f32195b + " ts:" + this.f32196c + " size:" + this.f32194a.length;
    }

    public static int f(byte b8, int i8) {
        String str;
        String strE = e(b8);
        if (i8 == 1) {
            str = "0";
        } else if (i8 == 3) {
            str = "000";
        } else {
            str = "";
        }
        return c(a(str + strE.substring(i8)));
    }
}
