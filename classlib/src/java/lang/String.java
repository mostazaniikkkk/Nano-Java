package java.lang;

import java.io.UnsupportedEncodingException;

/*
 * The VM depends on the layout of the first three fields (value, offset,
 * count) and creates strings without running a constructor, so String must
 * not gain a static initializer.
 */
public final class String {
    private char[] value;
    private int offset;
    private int count;
    private int hash;

    public String() {
        value = new char[0];
    }

    public String(String original) {
        value = original.value;
        offset = original.offset;
        count = original.count;
    }

    public String(char[] value) {
        this(value, 0, value.length);
    }

    public String(char[] value, int offset, int count) {
        if (offset < 0 || count < 0 || offset > value.length - count) {
            throw new StringIndexOutOfBoundsException();
        }
        this.value = new char[count];
        System.arraycopy(value, offset, this.value, 0, count);
        this.count = count;
    }

    public String(byte[] bytes, int off, int len, String enc) throws UnsupportedEncodingException {
        if (enc == null) {
            throw new NullPointerException();
        }
        checkBytes(bytes, off, len);
        value = nanojava.Encoding.decode(bytes, off, len, enc);
        count = value.length;
    }

    public String(byte[] bytes, String enc) throws UnsupportedEncodingException {
        this(bytes, 0, bytes.length, enc);
    }

    public String(byte[] bytes, int off, int len) {
        checkBytes(bytes, off, len);
        value = nanojava.Encoding.decodeDefault(bytes, off, len);
        count = value.length;
    }

    public String(byte[] bytes) {
        this(bytes, 0, bytes.length);
    }

    public String(StringBuffer buffer) {
        String s = buffer.toString();
        value = s.value;
        offset = s.offset;
        count = s.count;
    }

    /* Shares the array; used by StringBuffer and String itself. */
    String(int offset, int count, char[] value) {
        this.value = value;
        this.offset = offset;
        this.count = count;
    }

    private static void checkBytes(byte[] bytes, int off, int len) {
        if (off < 0 || len < 0 || off > bytes.length - len) {
            throw new StringIndexOutOfBoundsException();
        }
    }

    public int length() {
        return count;
    }

    public char charAt(int index) {
        if (index < 0 || index >= count) {
            throw new StringIndexOutOfBoundsException(index);
        }
        return value[offset + index];
    }

    public void getChars(int srcBegin, int srcEnd, char[] dst, int dstBegin) {
        if (srcBegin < 0 || srcBegin > srcEnd || srcEnd > count) {
            throw new StringIndexOutOfBoundsException();
        }
        System.arraycopy(value, offset + srcBegin, dst, dstBegin, srcEnd - srcBegin);
    }

    public byte[] getBytes(String enc) throws UnsupportedEncodingException {
        if (enc == null) {
            throw new NullPointerException();
        }
        return nanojava.Encoding.encode(value, offset, count, enc);
    }

    public byte[] getBytes() {
        return nanojava.Encoding.encodeDefault(value, offset, count);
    }

    public boolean equals(Object anObject) {
        if (this == anObject) {
            return true;
        }
        if (!(anObject instanceof String)) {
            return false;
        }
        String other = (String) anObject;
        int n = count;
        if (n != other.count) {
            return false;
        }
        char[] v1 = value;
        char[] v2 = other.value;
        int i = offset;
        int j = other.offset;
        while (n-- != 0) {
            if (v1[i++] != v2[j++]) {
                return false;
            }
        }
        return true;
    }

    public boolean equalsIgnoreCase(String other) {
        return other != null && other.count == count && regionMatches(true, 0, other, 0, count);
    }

    public int compareTo(String other) {
        int n = Math.min(count, other.count);
        char[] v1 = value;
        char[] v2 = other.value;
        int i = offset;
        int j = other.offset;
        while (n-- != 0) {
            char c1 = v1[i++];
            char c2 = v2[j++];
            if (c1 != c2) {
                return c1 - c2;
            }
        }
        return count - other.count;
    }

    public boolean regionMatches(int toffset, String other, int ooffset, int len) {
        return regionMatches(false, toffset, other, ooffset, len);
    }

    public boolean regionMatches(boolean ignoreCase, int toffset, String other, int ooffset, int len) {
        if (ooffset < 0 || toffset < 0 || toffset > (long) count - len
                || ooffset > (long) other.count - len) {
            return false;
        }
        char[] ta = value;
        int to = offset + toffset;
        char[] pa = other.value;
        int po = other.offset + ooffset;
        while (len-- > 0) {
            char c1 = ta[to++];
            char c2 = pa[po++];
            if (c1 == c2) {
                continue;
            }
            if (ignoreCase) {
                char u1 = Character.toUpperCase(c1);
                char u2 = Character.toUpperCase(c2);
                if (u1 == u2 || Character.toLowerCase(u1) == Character.toLowerCase(u2)) {
                    continue;
                }
            }
            return false;
        }
        return true;
    }

    public boolean startsWith(String prefix, int toffset) {
        if (toffset < 0 || toffset > count - prefix.count) {
            return false;
        }
        char[] ta = value;
        int to = offset + toffset;
        char[] pa = prefix.value;
        int po = prefix.offset;
        int pc = prefix.count;
        while (--pc >= 0) {
            if (ta[to++] != pa[po++]) {
                return false;
            }
        }
        return true;
    }

    public boolean startsWith(String prefix) {
        return startsWith(prefix, 0);
    }

    public boolean endsWith(String suffix) {
        return startsWith(suffix, count - suffix.count);
    }

    public int hashCode() {
        int h = hash;
        if (h == 0) {
            char[] v = value;
            int end = offset + count;
            for (int i = offset; i < end; i++) {
                h = 31 * h + v[i];
            }
            hash = h;
        }
        return h;
    }

    public int indexOf(int ch) {
        return indexOf(ch, 0);
    }

    public int indexOf(int ch, int fromIndex) {
        if (fromIndex < 0) {
            fromIndex = 0;
        }
        char[] v = value;
        int end = offset + count;
        for (int i = offset + fromIndex; i < end; i++) {
            if (v[i] == ch) {
                return i - offset;
            }
        }
        return -1;
    }

    public int lastIndexOf(int ch) {
        return lastIndexOf(ch, count - 1);
    }

    public int lastIndexOf(int ch, int fromIndex) {
        if (fromIndex >= count) {
            fromIndex = count - 1;
        }
        char[] v = value;
        for (int i = offset + fromIndex; i >= offset; i--) {
            if (v[i] == ch) {
                return i - offset;
            }
        }
        return -1;
    }

    public int indexOf(String str) {
        return indexOf(str, 0);
    }

    public int indexOf(String str, int fromIndex) {
        if (fromIndex < 0) {
            fromIndex = 0;
        }
        int n = str.count;
        if (n == 0) {
            return fromIndex <= count ? fromIndex : count;
        }
        char first = str.value[str.offset];
        int max = count - n;
        for (int i = fromIndex; i <= max; i++) {
            if (value[offset + i] == first && startsWith(str, i)) {
                return i;
            }
        }
        return -1;
    }

    public int lastIndexOf(String str) {
        return lastIndexOf(str, count);
    }

    public int lastIndexOf(String str, int fromIndex) {
        int max = count - str.count;
        if (fromIndex > max) {
            fromIndex = max;
        }
        for (int i = fromIndex; i >= 0; i--) {
            if (startsWith(str, i)) {
                return i;
            }
        }
        return -1;
    }

    public String substring(int beginIndex) {
        return substring(beginIndex, count);
    }

    public String substring(int beginIndex, int endIndex) {
        if (beginIndex < 0) {
            throw new StringIndexOutOfBoundsException(beginIndex);
        }
        if (endIndex > count) {
            throw new StringIndexOutOfBoundsException(endIndex);
        }
        if (beginIndex > endIndex) {
            throw new StringIndexOutOfBoundsException(endIndex - beginIndex);
        }
        if (beginIndex == 0 && endIndex == count) {
            return this;
        }
        return new String(offset + beginIndex, endIndex - beginIndex, value);
    }

    public String concat(String str) {
        if (str.count == 0) {
            return this;
        }
        char[] buf = new char[count + str.count];
        getChars(0, count, buf, 0);
        str.getChars(0, str.count, buf, count);
        return new String(0, buf.length, buf);
    }

    public String replace(char oldChar, char newChar) {
        if (oldChar == newChar || indexOf(oldChar) < 0) {
            return this;
        }
        char[] buf = new char[count];
        for (int i = 0; i < count; i++) {
            char c = value[offset + i];
            buf[i] = c == oldChar ? newChar : c;
        }
        return new String(0, count, buf);
    }

    public String toLowerCase() {
        char[] buf = null;
        for (int i = 0; i < count; i++) {
            char c = value[offset + i];
            char l = Character.toLowerCase(c);
            if (l != c && buf == null) {
                buf = toCharArray();
            }
            if (buf != null) {
                buf[i] = l;
            }
        }
        return buf == null ? this : new String(0, count, buf);
    }

    public String toUpperCase() {
        char[] buf = null;
        for (int i = 0; i < count; i++) {
            char c = value[offset + i];
            char u = Character.toUpperCase(c);
            if (u != c && buf == null) {
                buf = toCharArray();
            }
            if (buf != null) {
                buf[i] = u;
            }
        }
        return buf == null ? this : new String(0, count, buf);
    }

    public String trim() {
        int st = 0;
        int len = count;
        while (st < len && value[offset + st] <= ' ') {
            st++;
        }
        while (st < len && value[offset + len - 1] <= ' ') {
            len--;
        }
        return (st > 0 || len < count) ? substring(st, len) : this;
    }

    public String toString() {
        return this;
    }

    public char[] toCharArray() {
        char[] result = new char[count];
        System.arraycopy(value, offset, result, 0, count);
        return result;
    }

    public native String intern();

    public static String valueOf(Object obj) {
        return obj == null ? "null" : obj.toString();
    }

    public static String valueOf(char[] data) {
        return new String(data);
    }

    public static String valueOf(char[] data, int offset, int count) {
        return new String(data, offset, count);
    }

    public static String valueOf(boolean b) {
        return b ? "true" : "false";
    }

    public static String valueOf(char c) {
        char[] buf = {c};
        return new String(0, 1, buf);
    }

    public static String valueOf(int i) {
        return Integer.toString(i);
    }

    public static String valueOf(long l) {
        return Long.toString(l);
    }

    public static String valueOf(float f) {
        return Float.toString(f);
    }

    public static String valueOf(double d) {
        return Double.toString(d);
    }
}
