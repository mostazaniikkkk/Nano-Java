package java.lang;

public class Runtime {
    private static final Runtime instance = new Runtime();

    private Runtime() {
    }

    public static Runtime getRuntime() {
        return instance;
    }

    public native void exit(int status);

    public native long freeMemory();

    public native long totalMemory();

    public native void gc();
}
