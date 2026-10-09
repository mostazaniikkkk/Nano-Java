package nanojava;

/** Access to files packaged with the application. */
public final class Resources {
    private Resources() {
    }

    /** Returns the contents of a jar entry (no leading slash), or null. */
    public static native byte[] read(String name);
}
