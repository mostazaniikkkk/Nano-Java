package java.io;

public abstract class InputStream {
    public InputStream() {
    }

    public abstract int read() throws IOException;

    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    public int read(byte[] b, int off, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException();
        }
        if (off < 0 || len < 0 || off > b.length - len) {
            throw new IndexOutOfBoundsException();
        }
        if (len == 0) {
            return 0;
        }
        int c = read();
        if (c < 0) {
            return -1;
        }
        b[off] = (byte) c;
        int i = 1;
        try {
            for (; i < len; i++) {
                c = read();
                if (c < 0) {
                    break;
                }
                b[off + i] = (byte) c;
            }
        } catch (IOException e) {
            // Return what was read; the error will recur on the next call.
        }
        return i;
    }

    public long skip(long n) throws IOException {
        if (n <= 0) {
            return 0;
        }
        byte[] tmp = new byte[(int) Math.min(n, 512)];
        long left = n;
        while (left > 0) {
            int r = read(tmp, 0, (int) Math.min(left, tmp.length));
            if (r < 0) {
                break;
            }
            left -= r;
        }
        return n - left;
    }

    public int available() throws IOException {
        return 0;
    }

    public void close() throws IOException {
    }

    public synchronized void mark(int readlimit) {
    }

    public synchronized void reset() throws IOException {
        throw new IOException("mark/reset not supported");
    }

    public boolean markSupported() {
        return false;
    }
}
