package java.io;

import nanojava.Encoding;

/* Never throws IOException; failures are reported through checkError(). */
public class PrintStream extends OutputStream {
    private OutputStream out;
    private boolean trouble;

    public PrintStream(OutputStream out) {
        if (out == null) {
            throw new NullPointerException();
        }
        this.out = out;
    }

    public void flush() {
        synchronized (this) {
            if (out == null) {
                return;
            }
            try {
                out.flush();
            } catch (IOException e) {
                trouble = true;
            }
        }
    }

    public void close() {
        synchronized (this) {
            if (out == null) {
                return;
            }
            try {
                out.close();
            } catch (IOException e) {
                trouble = true;
            }
            out = null;
        }
    }

    public boolean checkError() {
        flush();
        return trouble;
    }

    protected void setError() {
        trouble = true;
    }

    public void write(int b) {
        synchronized (this) {
            if (out == null) {
                trouble = true;
                return;
            }
            try {
                out.write(b);
            } catch (IOException e) {
                trouble = true;
            }
        }
    }

    public void write(byte[] buf, int off, int len) {
        synchronized (this) {
            if (out == null) {
                trouble = true;
                return;
            }
            try {
                out.write(buf, off, len);
            } catch (IOException e) {
                trouble = true;
            }
        }
    }

    private void writeChars(char[] c, int off, int len) {
        byte[] b = Encoding.encodeDefault(c, off, len);
        write(b, 0, b.length);
    }

    private void writeString(String s) {
        char[] c = s.toCharArray();
        writeChars(c, 0, c.length);
    }

    private void newLine() {
        write('\n');
    }

    public void print(boolean b) {
        writeString(b ? "true" : "false");
    }

    public void print(char c) {
        writeString(String.valueOf(c));
    }

    public void print(int i) {
        writeString(String.valueOf(i));
    }

    public void print(long l) {
        writeString(String.valueOf(l));
    }

    public void print(float f) {
        writeString(String.valueOf(f));
    }

    public void print(double d) {
        writeString(String.valueOf(d));
    }

    public void print(char[] s) {
        writeChars(s, 0, s.length);
    }

    public void print(String s) {
        writeString(s == null ? "null" : s);
    }

    public void print(Object obj) {
        writeString(String.valueOf(obj));
    }

    public void println() {
        newLine();
    }

    public void println(boolean x) {
        synchronized (this) {
            print(x);
            newLine();
        }
    }

    public void println(char x) {
        synchronized (this) {
            print(x);
            newLine();
        }
    }

    public void println(int x) {
        synchronized (this) {
            print(x);
            newLine();
        }
    }

    public void println(long x) {
        synchronized (this) {
            print(x);
            newLine();
        }
    }

    public void println(float x) {
        synchronized (this) {
            print(x);
            newLine();
        }
    }

    public void println(double x) {
        synchronized (this) {
            print(x);
            newLine();
        }
    }

    public void println(char[] x) {
        synchronized (this) {
            print(x);
            newLine();
        }
    }

    public void println(String x) {
        synchronized (this) {
            print(x);
            newLine();
        }
    }

    public void println(Object x) {
        synchronized (this) {
            print(x);
            newLine();
        }
    }
}
