package java.io;

public class DataOutputStream extends OutputStream implements DataOutput {
    protected OutputStream out;

    public DataOutputStream(OutputStream out) {
        this.out = out;
    }

    public void write(int b) throws IOException {
        out.write(b);
    }

    public void write(byte[] b, int off, int len) throws IOException {
        out.write(b, off, len);
    }

    public void flush() throws IOException {
        out.flush();
    }

    public void close() throws IOException {
        out.close();
    }

    public final void writeBoolean(boolean v) throws IOException {
        out.write(v ? 1 : 0);
    }

    public final void writeByte(int v) throws IOException {
        out.write(v);
    }

    public final void writeShort(int v) throws IOException {
        out.write(v >>> 8);
        out.write(v);
    }

    public final void writeChar(int v) throws IOException {
        out.write(v >>> 8);
        out.write(v);
    }

    public final void writeInt(int v) throws IOException {
        byte[] b = new byte[4];
        b[0] = (byte) (v >>> 24);
        b[1] = (byte) (v >>> 16);
        b[2] = (byte) (v >>> 8);
        b[3] = (byte) v;
        out.write(b, 0, 4);
    }

    public final void writeLong(long v) throws IOException {
        byte[] b = new byte[8];
        for (int i = 0; i < 8; i++) {
            b[i] = (byte) (v >>> (56 - 8 * i));
        }
        out.write(b, 0, 8);
    }

    public final void writeFloat(float v) throws IOException {
        writeInt(Float.floatToIntBits(v));
    }

    public final void writeDouble(double v) throws IOException {
        writeLong(Double.doubleToLongBits(v));
    }

    public final void writeChars(String s) throws IOException {
        int len = s.length();
        byte[] b = new byte[len * 2];
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            b[2 * i] = (byte) (c >>> 8);
            b[2 * i + 1] = (byte) c;
        }
        out.write(b, 0, b.length);
    }

    /* Writes a 2-byte length followed by the string in Java modified UTF-8. */
    public final void writeUTF(String str) throws IOException {
        int len = str.length();
        int utflen = 0;
        for (int i = 0; i < len; i++) {
            char c = str.charAt(i);
            utflen += (c >= 0x0001 && c <= 0x007f) ? 1 : c <= 0x07ff ? 2 : 3;
        }
        if (utflen > 65535) {
            throw new UTFDataFormatException();
        }
        byte[] b = new byte[utflen + 2];
        int k = 0;
        b[k++] = (byte) (utflen >>> 8);
        b[k++] = (byte) utflen;
        for (int i = 0; i < len; i++) {
            char c = str.charAt(i);
            if (c >= 0x0001 && c <= 0x007f) {
                b[k++] = (byte) c;
            } else if (c <= 0x07ff) {
                b[k++] = (byte) (0xc0 | (c >> 6));
                b[k++] = (byte) (0x80 | (c & 0x3f));
            } else {
                b[k++] = (byte) (0xe0 | (c >> 12));
                b[k++] = (byte) (0x80 | ((c >> 6) & 0x3f));
                b[k++] = (byte) (0x80 | (c & 0x3f));
            }
        }
        out.write(b, 0, k);
    }
}
