package java.io;

public abstract class Writer {
    protected Object lock;

    protected Writer() {
        this.lock = this;
    }

    protected Writer(Object lock) {
        if (lock == null) {
            throw new NullPointerException();
        }
        this.lock = lock;
    }

    public void write(int c) throws IOException {
        synchronized (lock) {
            char[] cb = new char[1];
            cb[0] = (char) c;
            write(cb, 0, 1);
        }
    }

    public void write(char[] cbuf) throws IOException {
        write(cbuf, 0, cbuf.length);
    }

    public abstract void write(char[] cbuf, int off, int len) throws IOException;

    public void write(String str) throws IOException {
        write(str, 0, str.length());
    }

    public void write(String str, int off, int len) throws IOException {
        synchronized (lock) {
            char[] cb = new char[len];
            str.getChars(off, off + len, cb, 0);
            write(cb, 0, len);
        }
    }

    public abstract void flush() throws IOException;

    public abstract void close() throws IOException;
}
