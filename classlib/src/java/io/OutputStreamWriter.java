package java.io;

import nanojava.Encoding;

public class OutputStreamWriter extends Writer {
    private OutputStream out;
    private final int enc;

    public OutputStreamWriter(OutputStream os) {
        super(os);
        this.out = os;
        this.enc = Encoding.LATIN1;
    }

    public OutputStreamWriter(OutputStream os, String enc) throws UnsupportedEncodingException {
        super(os);
        if (enc == null) {
            throw new NullPointerException();
        }
        this.out = os;
        this.enc = Encoding.lookup(enc);
    }

    private void ensureOpen() throws IOException {
        if (out == null) {
            throw new IOException("Stream closed");
        }
    }

    public void write(int c) throws IOException {
        char[] cb = new char[1];
        cb[0] = (char) c;
        write(cb, 0, 1);
    }

    public void write(char[] cbuf, int off, int len) throws IOException {
        if (off < 0 || len < 0 || off > cbuf.length - len) {
            throw new IndexOutOfBoundsException();
        }
        synchronized (lock) {
            ensureOpen();
            byte[] b = Encoding.encode(cbuf, off, len, enc);
            out.write(b, 0, b.length);
        }
    }

    public void write(String str, int off, int len) throws IOException {
        if (off < 0 || len < 0 || off > str.length() - len) {
            throw new IndexOutOfBoundsException();
        }
        char[] cb = new char[len];
        str.getChars(off, off + len, cb, 0);
        write(cb, 0, len);
    }

    public void flush() throws IOException {
        synchronized (lock) {
            ensureOpen();
            out.flush();
        }
    }

    public void close() throws IOException {
        synchronized (lock) {
            if (out != null) {
                out.flush();
                out.close();
                out = null;
            }
        }
    }
}
