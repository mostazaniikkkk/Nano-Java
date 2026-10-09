package nanojava;

import javax.microedition.lcdui.LcduiBridge;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletBridge;
import javax.microedition.midlet.MIDletStateChangeException;

/** Starts a MIDlet and runs the display event loop on the boot thread. */
public final class MIDletRunner {
    private static MIDlet midlet;

    private MIDletRunner() {
    }

    /* "MIDlet-1: Name, /icon.png, com.example.Main" -> the class name. */
    private static String classFromManifest() {
        String entry = AppProperties.get("MIDlet-1");
        if (entry == null) {
            return null;
        }
        int last = entry.lastIndexOf(',');
        return entry.substring(last + 1).trim();
    }

    public static void run(String className) {
        if (className == null || className.length() == 0) {
            className = classFromManifest();
            if (className == null) {
                System.out.println("nanojava: no MIDlet-1 entry in the manifest");
                Boot.exit(1);
                return;
            }
        }
        MIDletBridge.setListener(new MIDletBridge.Listener() {
            public void destroyed(MIDlet m) {
                LcduiBridge.stopEventLoop();
            }
        });
        try {
            midlet = (MIDlet) Class.forName(className).newInstance();
        } catch (Throwable e) {
            System.out.println("nanojava: cannot create MIDlet " + className + ": " + e);
            Boot.exit(1);
            return;
        }
        try {
            MIDletBridge.start(midlet);
        } catch (MIDletStateChangeException e) {
            System.out.println("nanojava: MIDlet refused to start: " + e);
            Boot.exit(1);
            return;
        }
        LcduiBridge.runEventLoop();
        Boot.exit(0);
    }
}
