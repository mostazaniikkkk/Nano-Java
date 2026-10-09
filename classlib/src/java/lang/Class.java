package java.lang;

import java.io.InputStream;

public final class Class {
    /* The VM keeps a hidden pointer to its class structure in every
     * instance; instances are only created by the VM. */
    private Class() {
    }

    public static native Class forName(String className) throws ClassNotFoundException;

    public native Object newInstance() throws InstantiationException, IllegalAccessException;

    public native boolean isInstance(Object obj);

    public native boolean isAssignableFrom(Class cls);

    public native boolean isInterface();

    public native boolean isArray();

    public native String getName();

    public String toString() {
        return (isInterface() ? "interface " : "class ") + getName();
    }

    public InputStream getResourceAsStream(String name) {
        if (name == null) {
            throw new NullPointerException();
        }
        if (name.length() > 0 && name.charAt(0) == '/') {
            name = name.substring(1);
        } else {
            String cn = getName();
            int dot = cn.lastIndexOf('.');
            if (dot >= 0) {
                name = cn.substring(0, dot + 1).replace('.', '/') + name;
            }
        }
        byte[] data = nanojava.Resources.read(name);
        return data == null ? null : new java.io.ByteArrayInputStream(data);
    }
}
