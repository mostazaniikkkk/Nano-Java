package java.io;

public class DataInputStream extends InputStream implements DataInput {
    protected InputStream in;

    public DataInputStream(InputStream in) {
        this.in = in;
    }

    public int read() throws IOException {
        return in.read();
    }

    public final int read(byte[] b) throws IOException {
        return in.read(b, 0, b.length);
    }

    public final int read(byte[] b, int off, int len) throws IOException {
        return in.read(b, off, len);
    }

    public final void readFully(byte[] b) throws IOException {
        readFully(b, 0, b.length);
    }

    public final void readFully(byte[] b, int off, int len) throws IOException {
        if (len < 0) {
            throw new IndexOutOfBoundsException();
        }
        int n = 0;
        while (n < len) {
            int r = in.read(b, off + n, len - n);
            if (r < 0) {
                throw new EOFException();
            }
            n += r;
        }
    }

    public final int skipBytes(int n) throws IOException {
        int total = 0;
        while (total < n) {
            int s = (int) in.skip(n - total);
            if (s <= 0) {
                // skip() may return 0 without being at the end; probe with read().
                if (in.read() < 0) {
                    break;
                }
                s = 1;
            }
            total += s;
        }
        return total;
    }

    private int readByte0() throws IOException {
        int c = in.read();
        if (c < 0) {
            throw new EOFException();
        }
        return c;
    }

    public final boolean readBoolean() throws IOException {
        return readByte0() != 0;
    }

    public final byte readByte() throws IOException {
        return (byte) readByte0();
    }

    public final int readUnsignedByte() throws IOException {
        return readByte0();
    }

    public final short readShort() throws IOException {
        int a = readByte0();
        int b = readByte0();
        return (short) ((a << 8) | b);
    }

    public final int readUnsignedShort() throws IOException {
        int a = readByte0();
        int b = readByte0();
        return (a << 8) | b;
    }

    public final char readChar() throws IOException {
        int a = readByte0();
        int b = readByte0();
        return (char) ((a << 8) | b);
    }

    public final int readInt() throws IOException {
        int a = readByte0();
        int b = readByte0();
        int c = readByte0();
        int d = readByte0();
        return (a << 24) | (b << 16) | (c << 8) | d;
    }

    public final long readLong() throws IOException {
        long hi = readInt();
        long lo = readInt() & 0xffffffffL;
        return (hi << 32) | lo;
    }

    public final float readFloat() throws IOException {
        return Float.intBitsToFloat(readInt());
    }

    public final double readDouble() throws IOException {
        return Double.longBitsToDouble(readLong());
    }

    public final String readUTF() throws IOException {
        return readUTF(this);
    }

    /* Reads a 2-byte length followed by a string in Java modified UTF-8. */
    public static final String readUTF(DataInput in) throws IOException {
        int utflen = in.readUnsignedShort();
        byte[] b = new byte[utflen];
        in.readFully(b, 0, utflen);
        char[] c = new char[utflen];
        int n = 0;
        int i = 0;
        while (i < utflen) {
            int b0 = b[i] & 0xff;
            if (b0 < 0x80) {
                c[n++] = (char) b0;
                i++;
            } else if ((b0 & 0xe0) == 0xc0) {
                if (i + 1 >= utflen || (b[i + 1] & 0xc0) != 0x80) {
                    throw new UTFDataFormatException();
                }
                c[n++] = (char) (((b0 & 0x1f) << 6) | (b[i + 1] & 0x3f));
                i += 2;
            } else if ((b0 & 0xf0) == 0xe0) {
                if (i + 2 >= utflen || (b[i + 1] & 0xc0) != 0x80 || (b[i + 2] & 0xc0) != 0x80) {
                    throw new UTFDataFormatException();
                }
                c[n++] = (char) (((b0 & 0x0f) << 12) | ((b[i + 1] & 0x3f) << 6) | (b[i + 2] & 0x3f));
                i += 3;
            } else {
                throw new UTFDataFormatException();
            }
        }
        return new String(c, 0, n);
    }

    public long skip(long n) throws IOException {
        return in.skip(n);
    }

    public int available() throws IOException {
        return in.available();
    }

    public void close() throws IOException {
        in.close();
    }

    public synchronized void mark(int readlimit) {
        in.mark(readlimit);
    }

    public synchronized void reset() throws IOException {
        in.reset();
    }

    public boolean markSupported() {
        return in.markSupported();
    }
}
