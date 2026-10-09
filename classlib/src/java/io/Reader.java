package java.io;

public abstract class Reader {
    protected Object lock;

    protected Reader() {
        this.lock = this;
    }

    protected Reader(Object lock) {
        if (lock == null) {
            throw new NullPointerException();
        }
        this.lock = lock;
    }

    public int read() throws IOException {
        char[] cb = new char[1];
        return read(cb, 0, 1) == -1 ? -1 : cb[0];
    }

    public int read(char[] cbuf) throws IOException {
        return read(cbuf, 0, cbuf.length);
    }

    public abstract int read(char[] cbuf, int off, int len) throws IOException;

    public long skip(long n) throws IOException {
        if (n < 0L) {
            throw new IllegalArgumentException("skip value is negative");
        }
        synchronized (lock) {
            char[] tmp = new char[(int) Math.min(n, 512)];
            long left = n;
            while (left > 0) {
                int r = read(tmp, 0, (int) Math.min(left, tmp.length));
                if (r == -1) {
                    break;
                }
                left -= r;
            }
            return n - left;
        }
    }

    public boolean ready() throws IOException {
        return false;
    }

    public boolean markSupported() {
        return false;
    }

    public void mark(int readAheadLimit) throws IOException {
        throw new IOException("mark() not supported");
    }

    public void reset() throws IOException {
        throw new IOException("reset() not supported");
    }

    public abstract void close() throws IOException;
}
