package nanojava;

import java.io.OutputStream;

/** Backs System.out and System.err. */
public final class ConsoleOutputStream extends OutputStream {
    public native void write(int b);

    public void write(byte[] b, int off, int len) {
        writeBytes(b, off, len);
    }

    private native void writeBytes(byte[] b, int off, int len);
}
