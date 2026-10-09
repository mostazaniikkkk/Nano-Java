package java.lang;

public final class Long {
    public static final long MIN_VALUE = 0x8000000000000000L;
    public static final long MAX_VALUE = 0x7fffffffffffffffL;

    private final long value;

    public Long(long value) {
        this.value = value;
    }

    public static String toString(long i, int radix) {
        if (radix < Character.MIN_RADIX || radix > Character.MAX_RADIX) {
            radix = 10;
        }
        char[] buf = new char[65];
        int pos = 64;
        boolean negative = i < 0;
        if (!negative) {
            i = -i;
        }
        while (i <= -radix) {
            buf[pos--] = Integer.digits[(int) (-(i % radix))];
            i = i / radix;
        }
        buf[pos] = Integer.digits[(int) (-i)];
        if (negative) {
            buf[--pos] = '-';
        }
        return new String(buf, pos, 65 - pos);
    }

    public static String toString(long i) {
        return toString(i, 10);
    }

    public static long parseLong(String s, int radix) throws NumberFormatException {
        if (s == null) {
            throw new NumberFormatException("null");
        }
        if (radix < Character.MIN_RADIX || radix > Character.MAX_RADIX) {
            throw new NumberFormatException("radix " + radix + " out of range");
        }
        long result = 0;
        boolean negative = false;
        int i = 0;
        int max = s.length();
        long limit;
        if (max == 0) {
            throw Integer.forInputString(s);
        }
        if (s.charAt(0) == '-') {
            negative = true;
            limit = MIN_VALUE;
            i++;
        } else {
            limit = -MAX_VALUE;
        }
        long multmin = limit / radix;
        if (i >= max) {
            throw Integer.forInputString(s);
        }
        while (i < max) {
            int digit = Character.digit(s.charAt(i++), radix);
            if (digit < 0 || result < multmin) {
                throw Integer.forInputString(s);
            }
            result *= radix;
            if (result < limit + digit) {
                throw Integer.forInputString(s);
            }
            result -= digit;
        }
        return negative ? result : -result;
    }

    public static long parseLong(String s) throws NumberFormatException {
        return parseLong(s, 10);
    }

    public long longValue() {
        return value;
    }

    public float floatValue() {
        return value;
    }

    public double doubleValue() {
        return value;
    }

    public String toString() {
        return toString(value);
    }

    public int hashCode() {
        return (int) (value ^ (value >>> 32));
    }

    public boolean equals(Object obj) {
        return obj instanceof Long && ((Long) obj).value == value;
    }
}
