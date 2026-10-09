package nanojava;

import java.io.UnsupportedEncodingException;

/**
 * Character encodings supported by String, InputStreamReader and
 * OutputStreamWriter: ISO-8859-1 (the default), US-ASCII and UTF-8.
 */
public final class Encoding {
    public static final int LATIN1 = 0;
    public static final int ASCII = 1;
    public static final int UTF8 = 2;

    public static final String DEFAULT = "ISO-8859-1";

    private Encoding() {
    }

    public static int lookup(String enc) throws UnsupportedEncodingException {
        String e = enc.toUpperCase();
        if (e.equals("ISO-8859-1") || e.equals("ISO8859_1") || e.equals("ISO8859-1")
                || e.equals("LATIN1") || e.equals("ISO-LATIN-1")) {
            return LATIN1;
        }
        if (e.equals("UTF-8") || e.equals("UTF8")) {
            return UTF8;
        }
        if (e.equals("US-ASCII") || e.equals("ASCII") || e.equals("US_ASCII")) {
            return ASCII;
        }
        throw new UnsupportedEncodingException(enc);
    }

    public static char[] decode(byte[] b, int off, int len, String enc)
            throws UnsupportedEncodingException {
        return decode(b, off, len, lookup(enc));
    }

    public static char[] decodeDefault(byte[] b, int off, int len) {
        return decode(b, off, len, LATIN1);
    }

    public static char[] decode(byte[] b, int off, int len, int enc) {
        if (enc != UTF8) {
            char[] c = new char[len];
            for (int i = 0; i < len; i++) {
                int v = b[off + i] & 0xff;
                c[i] = (char) (enc == ASCII && v > 127 ? '?' : v);
            }
            return c;
        }
        char[] c = new char[len];
        int n = 0;
        int end = off + len;
        int i = off;
        while (i < end) {
            int b0 = b[i] & 0xff;
            if (b0 < 0x80) {
                c[n++] = (char) b0;
                i++;
            } else if ((b0 & 0xe0) == 0xc0 && i + 1 < end && (b[i + 1] & 0xc0) == 0x80) {
                c[n++] = (char) (((b0 & 0x1f) << 6) | (b[i + 1] & 0x3f));
                i += 2;
            } else if ((b0 & 0xf0) == 0xe0 && i + 2 < end && (b[i + 1] & 0xc0) == 0x80
                    && (b[i + 2] & 0xc0) == 0x80) {
                c[n++] = (char) (((b0 & 0x0f) << 12) | ((b[i + 1] & 0x3f) << 6) | (b[i + 2] & 0x3f));
                i += 3;
            } else {
                c[n++] = (char) 0xfffd;
                i++;
            }
        }
        if (n == len) {
            return c;
        }
        char[] r = new char[n];
        System.arraycopy(c, 0, r, 0, n);
        return r;
    }

    public static byte[] encode(char[] c, int off, int len, String enc)
            throws UnsupportedEncodingException {
        return encode(c, off, len, lookup(enc));
    }

    public static byte[] encodeDefault(char[] c, int off, int len) {
        return encode(c, off, len, LATIN1);
    }

    public static byte[] encode(char[] c, int off, int len, int enc) {
        if (enc != UTF8) {
            int max = enc == ASCII ? 127 : 255;
            byte[] b = new byte[len];
            for (int i = 0; i < len; i++) {
                char ch = c[off + i];
                b[i] = (byte) (ch > max ? '?' : ch);
            }
            return b;
        }
        int n = 0;
        for (int i = 0; i < len; i++) {
            char ch = c[off + i];
            n += ch < 0x80 ? 1 : ch < 0x800 ? 2 : 3;
        }
        byte[] b = new byte[n];
        int k = 0;
        for (int i = 0; i < len; i++) {
            char ch = c[off + i];
            if (ch < 0x80) {
                b[k++] = (byte) ch;
            } else if (ch < 0x800) {
                b[k++] = (byte) (0xc0 | (ch >> 6));
                b[k++] = (byte) (0x80 | (ch & 0x3f));
            } else {
                b[k++] = (byte) (0xe0 | (ch >> 12));
                b[k++] = (byte) (0x80 | ((ch >> 6) & 0x3f));
                b[k++] = (byte) (0x80 | (ch & 0x3f));
            }
        }
        return b;
    }
}
