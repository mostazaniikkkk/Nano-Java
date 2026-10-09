package javax.microedition.io;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/* A read-only connection to a jar resource. */
class ResourceConnection implements ContentConnection {
    private final byte[] data;
    private final int mode;
    private boolean closed;

    ResourceConnection(byte[] data, int mode) {
        this.data = data;
        this.mode = mode;
    }

    public String getType() {
        return null;
    }

    public String getEncoding() {
        return null;
    }

    public long getLength() {
        return data.length;
    }

    public InputStream openInputStream() throws IOException {
        if (closed) {
            throw new IOException("connection closed");
        }
        if ((mode & Connector.READ) == 0) {
            throw new IOException("not opened for reading");
        }
        return new ByteArrayInputStream(data);
    }

    public DataInputStream openDataInputStream() throws IOException {
        return new DataInputStream(openInputStream());
    }

    public OutputStream openOutputStream() throws IOException {
        throw new IOException("resources are read-only");
    }

    public DataOutputStream openDataOutputStream() throws IOException {
        return new DataOutputStream(openOutputStream());
    }

    public void close() {
        closed = true;
    }
}
