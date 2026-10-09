package nanojava;

/** The VM's input event queue. */
public final class Events {
    public static final int NONE = 0;
    public static final int KEY_DOWN = 1;
    public static final int KEY_UP = 2;
    public static final int POINTER_DOWN = 3;
    public static final int POINTER_UP = 4;
    public static final int POINTER_DRAG = 5;

    private Events() {
    }

    /**
     * Waits for the next event and returns its type, storing the key code
     * (or pointer x, y) in out[0..1]. Returns NONE when the timeout expires
     * or wakeup() was called. timeoutMs < 0 waits forever, 0 polls.
     */
    public static native int next(int[] out, int timeoutMs);

    /** Makes the event thread return from next() as soon as possible. */
    public static native void wakeup();
}
