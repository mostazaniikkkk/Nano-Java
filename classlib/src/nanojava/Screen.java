package nanojava;

import javax.microedition.lcdui.Image;

/** The physical display the platform gives the MIDlet. */
public final class Screen {
    private Screen() {
    }

    public static native int width();

    public static native int height();

    /** Shows a frame (the display's back buffer). */
    public static native void present(Image img);

    /** Labels of the soft key commands (null for none). */
    public static native void softLabels(String left, String right);
}
