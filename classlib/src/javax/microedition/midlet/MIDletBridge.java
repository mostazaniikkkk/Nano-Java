package javax.microedition.midlet;

/**
 * Internal: drives the MIDlet life cycle for nanojava.MIDletRunner. Not
 * part of the MIDP API.
 */
public final class MIDletBridge {
    private static Listener listener;

    /** Notified when the MIDlet calls notifyDestroyed(). */
    public interface Listener {
        void destroyed(MIDlet m);
    }

    private MIDletBridge() {
    }

    public static void setListener(Listener l) {
        listener = l;
    }

    static void destroyed(MIDlet m) {
        if (listener != null) {
            listener.destroyed(m);
        }
    }

    public static void start(MIDlet m) throws MIDletStateChangeException {
        m.startApp();
    }

    public static void pause(MIDlet m) {
        m.pauseApp();
    }

    public static void destroy(MIDlet m, boolean unconditional) throws MIDletStateChangeException {
        m.destroyApp(unconditional);
    }
}
