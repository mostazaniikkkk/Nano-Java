package java.lang;

public final class StringBuffer {
    private char[] value;
    private int count;
    /* Set when toString() shared the array; the next write copies it. */
    private boolean shared;

    public StringBuffer() {
        this(16);
    }

    public StringBuffer(int length) {
        if (length < 0) {
            throw new NegativeArraySizeException();
        }
        value = new char[length];
    }

    public StringBuffer(String str) {
        this(str.length() + 16);
        append(str);
    }

    public int length() {
        return count;
    }

    public int capacity() {
        return value.length;
    }

    private void ensure(int minimum) {
        if (minimum > value.length || shared) {
            int n = value.length * 2 + 2;
            if (n < minimum) {
                n = minimum;
            }
            char[] nv = new char[n];
            System.arraycopy(value, 0, nv, 0, count);
            value = nv;
            shared = false;
        }
    }

    public synchronized void ensureCapacity(int minimumCapacity) {
        if (minimumCapacity > value.length) {
            ensure(minimumCapacity);
        }
    }

    public synchronized void setLength(int newLength) {
        if (newLength < 0) {
            throw new StringIndexOutOfBoundsException(newLength);
        }
        ensure(newLength);
        for (int i = count; i < newLength; i++) {
            value[i] = 0;
        }
        count = newLength;
    }

    public synchronized char charAt(int index) {
        if (index < 0 || index >= count) {
            throw new StringIndexOutOfBoundsException(index);
        }
        return value[index];
    }

    public synchronized void getChars(int srcBegin, int srcEnd, char[] dst, int dstBegin) {
        if (srcBegin < 0 || srcBegin > srcEnd || srcEnd > count) {
            throw new StringIndexOutOfBoundsException();
        }
        System.arraycopy(value, srcBegin, dst, dstBegin, srcEnd - srcBegin);
    }

    public synchronized void setCharAt(int index, char ch) {
        if (index < 0 || index >= count) {
            throw new StringIndexOutOfBoundsException(index);
        }
        ensure(count);
        value[index] = ch;
    }

    public StringBuffer append(Object obj) {
        return append(String.valueOf(obj));
    }

    public synchronized StringBuffer append(String str) {
        if (str == null) {
            str = "null";
        }
        int len = str.length();
        ensure(count + len);
        str.getChars(0, len, value, count);
        count += len;
        return this;
    }

    public StringBuffer append(char[] str) {
        return append(str, 0, str.length);
    }

    public synchronized StringBuffer append(char[] str, int offset, int len) {
        ensure(count + len);
        System.arraycopy(str, offset, value, count, len);
        count += len;
        return this;
    }

    public StringBuffer append(boolean b) {
        return append(b ? "true" : "false");
    }

    public synchronized StringBuffer append(char c) {
        ensure(count + 1);
        value[count++] = c;
        return this;
    }

    public StringBuffer append(int i) {
        return append(Integer.toString(i));
    }

    public StringBuffer append(long l) {
        return append(Long.toString(l));
    }

    public StringBuffer append(float f) {
        return append(Float.toString(f));
    }

    public StringBuffer append(double d) {
        return append(Double.toString(d));
    }

    public synchronized StringBuffer delete(int start, int end) {
        if (end > count) {
            end = count;
        }
        if (start < 0 || start > end) {
            throw new StringIndexOutOfBoundsException();
        }
        int len = end - start;
        if (len > 0) {
            ensure(count);
            System.arraycopy(value, end, value, start, count - end);
            count -= len;
        }
        return this;
    }

    public synchronized StringBuffer deleteCharAt(int index) {
        if (index < 0 || index >= count) {
            throw new StringIndexOutOfBoundsException(index);
        }
        return delete(index, index + 1);
    }

    public StringBuffer insert(int offset, Object obj) {
        return insert(offset, String.valueOf(obj));
    }

    public synchronized StringBuffer insert(int offset, String str) {
        if (offset < 0 || offset > count) {
            throw new StringIndexOutOfBoundsException();
        }
        if (str == null) {
            str = "null";
        }
        int len = str.length();
        ensure(count + len);
        System.arraycopy(value, offset, value, offset + len, count - offset);
        str.getChars(0, len, value, offset);
        count += len;
        return this;
    }

    public StringBuffer insert(int offset, char[] str) {
        return insert(offset, new String(str));
    }

    public StringBuffer insert(int offset, boolean b) {
        return insert(offset, b ? "true" : "false");
    }

    public StringBuffer insert(int offset, char c) {
        return insert(offset, String.valueOf(c));
    }

    public StringBuffer insert(int offset, int i) {
        return insert(offset, Integer.toString(i));
    }

    public StringBuffer insert(int offset, long l) {
        return insert(offset, Long.toString(l));
    }

    public StringBuffer insert(int offset, float f) {
        return insert(offset, Float.toString(f));
    }

    public StringBuffer insert(int offset, double d) {
        return insert(offset, Double.toString(d));
    }

    public synchronized StringBuffer reverse() {
        ensure(count);
        for (int i = 0, j = count - 1; i < j; i++, j--) {
            char c = value[i];
            value[i] = value[j];
            value[j] = c;
        }
        return this;
    }

    public synchronized String toString() {
        shared = true;
        return new String(0, count, value);
    }
}
