package javax.microedition.midlet;

public abstract class MIDlet {
    protected MIDlet() {
    }

    protected abstract void startApp() throws MIDletStateChangeException;

    protected abstract void pauseApp();

    protected abstract void destroyApp(boolean unconditional) throws MIDletStateChangeException;

    public final void notifyDestroyed() {
        MIDletBridge.destroyed(this);
    }

    public final void notifyPaused() {
    }

    public final void resumeRequest() {
    }

    public final String getAppProperty(String key) {
        if (key == null) {
            throw new NullPointerException();
        }
        return nanojava.AppProperties.get(key);
    }

    public final boolean platformRequest(String URL) throws javax.microedition.io.ConnectionNotFoundException {
        return false;
    }

    public final int checkPermission(String permission) {
        return 1;
    }
}
