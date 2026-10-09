package java.lang;

public class Throwable {
    private String detailMessage;
    /* Frames captured by the VM: pairs of (method, pc) in a long[]. */
    private Object trace;

    public Throwable() {
        fillInStackTrace();
    }

    public Throwable(String message) {
        detailMessage = message;
        fillInStackTrace();
    }

    private native void fillInStackTrace();

    private native void printStackTrace0();

    public String getMessage() {
        return detailMessage;
    }

    public String toString() {
        String name = getClass().getName();
        String msg = getMessage();
        return msg != null ? name + ": " + msg : name;
    }

    public void printStackTrace() {
        printStackTrace0();
    }
}
