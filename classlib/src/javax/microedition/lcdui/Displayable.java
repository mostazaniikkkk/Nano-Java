package javax.microedition.lcdui;

import java.util.Vector;

public abstract class Displayable {
    String title;
    Ticker ticker;
    final Vector commands = new Vector();
    CommandListener listener;
    boolean shown;

    Displayable() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String s) {
        title = s;
        repaintDisplayable();
    }

    public Ticker getTicker() {
        return ticker;
    }

    public void setTicker(Ticker ticker) {
        this.ticker = ticker;
        repaintDisplayable();
    }

    public boolean isShown() {
        return shown;
    }

    public void addCommand(Command cmd) {
        if (cmd == null) {
            throw new NullPointerException();
        }
        synchronized (commands) {
            if (commands.contains(cmd)) {
                return;
            }
            /* Keep commands ordered by priority, like most devices do. */
            int i = 0;
            while (i < commands.size()
                    && ((Command) commands.elementAt(i)).getPriority() <= cmd.getPriority()) {
                i++;
            }
            commands.insertElementAt(cmd, i);
        }
        repaintDisplayable();
    }

    public void removeCommand(Command cmd) {
        commands.removeElement(cmd);
        repaintDisplayable();
    }

    public void setCommandListener(CommandListener l) {
        listener = l;
    }

    public int getWidth() {
        return Display.screenWidth();
    }

    public int getHeight() {
        return Display.screenHeight();
    }

    protected void sizeChanged(int w, int h) {
    }

    /* ---- Hooks used by Display ---- */

    void repaintDisplayable() {
        if (shown) {
            Display.requestRepaint(this);
        }
    }

    /* Paints the whole displayable into g (whose clip is already set). */
    abstract void paintDisplayable(Graphics g);

    void showNotify0() {
    }

    void hideNotify0() {
    }

    /* Returns true if the key was consumed. */
    boolean keyEvent(int type, int keyCode) {
        return false;
    }

    void pointerEvent(int type, int x, int y) {
    }

    /* Whether the soft keys belong to this displayable (Canvas without
     * commands) instead of driving the command menu. */
    boolean wantsSoftKeys() {
        return false;
    }

    /* Commands of the focused item (Form), offered on the soft keys
     * before the displayable's own; null when there are none. */
    Vector itemCommands() {
        return null;
    }

    void fireCommand(Command c) {
        CommandListener l = listener;
        if (l != null) {
            l.commandAction(c, this);
        }
    }
}
