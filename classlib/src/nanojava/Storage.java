package nanojava;

/**
 * Persistent key/value blobs in the platform's data directory. Writes are
 * atomic per key. Used by javax.microedition.rms.
 */
public final class Storage {
    private Storage() {
    }

    /** Returns the stored bytes, or null if the key does not exist. */
    public static native byte[] read(String key);

    /** Stores data under key, replacing any previous value. */
    public static native boolean write(String key, byte[] data);

    public static native boolean delete(String key);

    /** All existing keys. */
    public static native String[] list();
}
