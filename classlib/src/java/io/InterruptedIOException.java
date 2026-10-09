package java.io;

public class InterruptedIOException extends IOException {
    /* Number of bytes transferred before the interruption. */
    public int bytesTransferred;

    public InterruptedIOException() {
        super();
    }

    public InterruptedIOException(String s) {
        super(s);
    }
}
