package u1;

/**
 * Minimal stub of AIWIT's u1.o utility class with only the bit-twiddling
 * methods n1.c (the RTP/packet parser) calls into. The full AIWIT u1.o is
 * 880 lines of misc helpers; everything else lives in a different code path
 * we don't reach for live-view receive.
 */
public class o {
    /** Truncate an int to a byte. */
    public static byte T(int i8) {
        return (byte) i8;
    }

    /** Little-endian 4-byte representation of an int. */
    public static byte[] c(int i8) {
        return new byte[]{
            (byte) (i8 & 255),
            (byte) ((i8 >> 8) & 255),
            (byte) ((i8 >> 16) & 255),
            (byte) ((i8 >> 24) & 255),
        };
    }

    /** Parse a 4-char or 8-char binary string into a byte (signed wrap for 8). */
    public static byte d(String str) {
        if (str == null) return (byte) 0;
        int length = str.length();
        if (length != 4 && length != 8) return (byte) 0;
        int v = (length != 8 || str.charAt(0) == '0')
            ? Integer.parseInt(str, 2)
            : Integer.parseInt(str, 2) - 256;
        return (byte) v;
    }

    /** 8-character binary representation of a byte (MSB first). */
    public static String q(byte b8) {
        StringBuilder sb = new StringBuilder(8);
        sb.append((b8 >> 7) & 1);
        sb.append((b8 >> 6) & 1);
        sb.append((b8 >> 5) & 1);
        sb.append((b8 >> 4) & 1);
        sb.append((b8 >> 3) & 1);
        sb.append((b8 >> 2) & 1);
        sb.append((b8 >> 1) & 1);
        sb.append(b8 & 1);
        return sb.toString();
    }
}
