package javax.microedition.io;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/*
 * There is no networking. The only protocol is "resource:" (or a plain
 * path), which reads files packaged in the jar.
 */
public class Connector {
    public static final int READ = 1;
    public static final int WRITE = 2;
    public static final int READ_WRITE = 3;

    private Connector() {
    }

    public static Connection open(String name) throws IOException {
        return open(name, READ_WRITE, false);
    }

    public static Connection open(String name, int mode) throws IOException {
        return open(name, mode, false);
    }

    public static Connection open(String name, int mode, boolean timeouts) throws IOException {
        if (name == null) {
            throw new IllegalArgumentException("null name");
        }
        if (mode != READ && mode != WRITE && mode != READ_WRITE) {
            throw new IllegalArgumentException("invalid mode");
        }
        String path;
        if (name.startsWith("resource://")) {
            path = name.substring("resource://".length());
        } else if (name.startsWith("resource:")) {
            path = name.substring("resource:".length());
        } else {
            int colon = name.indexOf(':');
            int slash = name.indexOf('/');
            if (colon > 0 && (slash < 0 || colon < slash)) {
                throw new ConnectionNotFoundException("unsupported protocol: " + name);
            }
            path = name;
        }
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        byte[] data = nanojava.Resources.read(path);
        if (data == null) {
            throw new ConnectionNotFoundException("not found: " + name);
        }
        return new ResourceConnection(data, mode);
    }

    public static DataInputStream openDataInputStream(String name) throws IOException {
        return new DataInputStream(openInputStream(name));
    }

    public static DataOutputStream openDataOutputStream(String name) throws IOException {
        return new DataOutputStream(openOutputStream(name));
    }

    public static InputStream openInputStream(String name) throws IOException {
        Connection c = open(name, READ);
        if (!(c instanceof InputConnection)) {
            c.close();
            throw new IllegalArgumentException("not an input connection: " + name);
        }
        return ((InputConnection) c).openInputStream();
    }

    public static OutputStream openOutputStream(String name) throws IOException {
        Connection c = open(name, WRITE);
        if (!(c instanceof OutputConnection)) {
            c.close();
            throw new IllegalArgumentException("not an output connection: " + name);
        }
        return ((OutputConnection) c).openOutputStream();
    }
}
