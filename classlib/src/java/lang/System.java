package java.lang;

import java.io.PrintStream;

public final class System {
    public static final PrintStream out = new PrintStream(new nanojava.ConsoleOutputStream());
    public static final PrintStream err = out;

    private System() {
    }

    public static native long currentTimeMillis();

    public static native void arraycopy(Object src, int srcPos, Object dst, int dstPos, int length);

    public static native int identityHashCode(Object x);

    public static String getProperty(String key) {
        if (key == null) {
            throw new NullPointerException();
        }
        if (key.length() == 0) {
            throw new IllegalArgumentException("key can't be empty");
        }
        return getProperty0(key);
    }

    private static native String getProperty0(String key);

    public static void exit(int status) {
        Runtime.getRuntime().exit(status);
    }

    public static void gc() {
        Runtime.getRuntime().gc();
    }
}
