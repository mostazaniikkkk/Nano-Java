package java.io;

import nanojava.Encoding;

/*
 * Decodes bytes incrementally through a small internal buffer, so UTF-8
 * sequences may straddle reads of the underlying stream. Supplementary
 * characters (4-byte UTF-8) are returned as surrogate pairs.
 */
public class InputStreamReader extends Reader {
    private static final int BUF_SIZE = 256;

    private InputStream in;
    private final int enc;
    private final byte[] bbuf = new byte[BUF_SIZE];
    private int bpos;
    private int blen;
    private boolean eof;
    private int pendingLow = -1;

    public InputStreamReader(InputStream is) {
        super(is);
        this.in = is;
        this.enc = Encoding.LATIN1;
    }

    public InputStreamReader(InputStream is, String enc) throws UnsupportedEncodingException {
        super(is);
        if (enc == null) {
            throw new NullPointerException();
        }
        this.in = is;
        this.enc = Encoding.lookup(enc);
    }

    private void ensureOpen() throws IOException {
        if (in == null) {
            throw new IOException("Stream closed");
        }
    }

    /*
     * Tries to have at least n bytes buffered. When block is false, only
     * reads from the stream if it reports available bytes. Returns whether
     * n bytes are buffered.
     */
    private boolean fill(int n, boolean block) throws IOException {
        while (blen - bpos < n) {
            if (eof) {
                return false;
            }
            if (!block && in.available() <= 0) {
                return false;
            }
            if (bpos > 0) {
                System.arraycopy(bbuf, bpos, bbuf, 0, blen - bpos);
                blen -= bpos;
                bpos = 0;
            }
            int r = in.read(bbuf, blen, BUF_SIZE - blen);
            if (r < 0) {
                eof = true;
                return false;
            }
            blen += r;
        }
        return true;
    }

    /* Returns the next char, -1 at end of stream, or -2 if it would block. */
    private int decode(boolean block) throws IOException {
        if (pendingLow >= 0) {
            int c = pendingLow;
            pendingLow = -1;
            return c;
        }
        if (!fill(1, block)) {
            return eof && bpos >= blen ? -1 : -2;
        }
        int b0 = bbuf[bpos] & 0xff;
        if (enc != Encoding.UTF8 || b0 < 0x80) {
            bpos++;
            return (enc == Encoding.ASCII && b0 > 127) ? '?' : b0;
        }
        int need;
        int cp;
        if ((b0 & 0xe0) == 0xc0) {
            need = 1;
            cp = b0 & 0x1f;
        } else if ((b0 & 0xf0) == 0xe0) {
            need = 2;
            cp = b0 & 0x0f;
        } else if ((b0 & 0xf8) == 0xf0) {
            need = 3;
            cp = b0 & 0x07;
        } else {
            bpos++;
            return 0xfffd;
        }
        if (!fill(need + 1, block) && !eof) {
            return -2;
        }
        int have = blen - bpos;
        for (int i = 1; i <= need; i++) {
            if (i >= have || (bbuf[bpos + i] & 0xc0) != 0x80) {
                // Truncated or malformed: consume the valid prefix only.
                bpos += i;
                return 0xfffd;
            }
            cp = (cp << 6) | (bbuf[bpos + i] & 0x3f);
        }
        bpos += need + 1;
        if (cp >= 0x10000) {
            if (cp > 0x10ffff) {
                return 0xfffd;
            }
            cp -= 0x10000;
            pendingLow = 0xdc00 | (cp & 0x3ff);
            return 0xd800 | (cp >> 10);
        }
        return cp;
    }

    public int read() throws IOException {
        synchronized (lock) {
            ensureOpen();
            return decode(true);
        }
    }

    public int read(char[] cbuf, int off, int len) throws IOException {
        if (off < 0 || len < 0 || off > cbuf.length - len) {
            throw new IndexOutOfBoundsException();
        }
        synchronized (lock) {
            ensureOpen();
            if (len == 0) {
                return 0;
            }
            int c = decode(true);
            if (c < 0) {
                return -1;
            }
            cbuf[off] = (char) c;
            int n = 1;
            while (n < len) {
                c = decode(false);
                if (c < 0) {
                    break;
                }
                cbuf[off + n++] = (char) c;
            }
            return n;
        }
    }

    public boolean ready() throws IOException {
        synchronized (lock) {
            ensureOpen();
            return pendingLow >= 0 || bpos < blen || in.available() > 0;
        }
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

    public void close() throws IOException {
        synchronized (lock) {
            if (in != null) {
                in.close();
                in = null;
            }
        }
    }
}
