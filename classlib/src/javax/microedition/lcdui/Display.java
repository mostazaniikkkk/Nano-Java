package javax.microedition.lcdui;

import java.util.Vector;
import javax.microedition.midlet.MIDlet;

import nanojava.Events;

/*
 * There is a single Display. Its event loop runs on the MIDlet's boot
 * thread: it delivers input, paints pending repaints and runs callSerially
 * work. All callbacks into the application and all painting happen while
 * holding LOCK, so they never overlap.
 */
public class Display {
    public static final int LIST_ELEMENT = 1;
    public static final int CHOICE_GROUP_ELEMENT = 2;
    public static final int ALERT = 3;

    public static final int COLOR_BACKGROUND = 0;
    public static final int COLOR_FOREGROUND = 1;
    public static final int COLOR_HIGHLIGHTED_BACKGROUND = 2;
    public static final int COLOR_HIGHLIGHTED_FOREGROUND = 3;
    public static final int COLOR_BORDER = 4;
    public static final int COLOR_HIGHLIGHTED_BORDER = 5;

    static final Object LOCK = new Object();
    static final int KEY_REPEAT = 6;

    private static final int REPEAT_DELAY = 400;
    private static final int REPEAT_RATE = 100;

    private static Display instance;
    private static int width;
    private static int height;

    private final Image screen;
    private final Graphics screenGraphics;
    private Displayable current;
    private volatile boolean repaintPending;
    private final Vector serial = new Vector();
    private CommandMenu menu;
    private boolean running = true;

    private int heldKey;
    private boolean keyHeld;
    private long nextRepeat;

    private Display() {
        screen = Image.createImage(width, height);
        screenGraphics = new Graphics(screen);
    }

    static synchronized Display get() {
        if (instance == null) {
            width = nanojava.Screen.width();
            height = nanojava.Screen.height();
            instance = new Display();
        }
        return instance;
    }

    static int screenWidth() {
        get();
        return width;
    }

    static int screenHeight() {
        get();
        return height;
    }

    public static Display getDisplay(MIDlet m) {
        if (m == null) {
            throw new NullPointerException();
        }
        return get();
    }

    public Displayable getCurrent() {
        return current;
    }

    public void setCurrent(Displayable next) {
        if (next == null || next == current) {
            return;
        }
        if (next instanceof Alert) {
            /* Return to what the alert covers; if that is another alert,
             * to where that one would have returned. */
            Displayable back = current;
            if (back instanceof Alert) {
                back = ((Alert) back).returnTo;
            }
            if (back == null) {
                show(next);
            } else {
                setCurrent((Alert) next, back);
            }
            return;
        }
        show(next);
    }

    public void setCurrent(Alert alert, Displayable next) {
        if (alert == null || next == null || next instanceof Alert) {
            throw (alert == null || next == null) ? (RuntimeException) new NullPointerException()
                    : new IllegalArgumentException();
        }
        alert.returnTo = next;
        show(alert);
    }

    public void setCurrentItem(Item item) {
        Screen owner = item.owner;
        if (owner == null) {
            throw new IllegalStateException();
        }
        setCurrent(owner);
        owner.focusItem(item);
    }

    void show(Displayable next) {
        synchronized (LOCK) {
            Displayable old = current;
            menu = null;
            if (old != null) {
                old.shown = false;
                old.hideNotify0();
            }
            current = next;
            next.shown = true;
            next.showNotify0();
        }
        requestRepaint(next);
    }

    public void callSerially(Runnable r) {
        serial.addElement(r);
        Events.wakeup();
    }

    public boolean flashBacklight(int duration) {
        return false;
    }

    public boolean vibrate(int duration) {
        if (duration < 0) {
            throw new IllegalArgumentException();
        }
        return false;
    }

    public boolean isColor() {
        return true;
    }

    public int numColors() {
        return 32768;
    }

    public int numAlphaLevels() {
        return 256;
    }

    public int getColor(int colorSpecifier) {
        switch (colorSpecifier) {
        case COLOR_BACKGROUND: return Theme.BACKGROUND;
        case COLOR_FOREGROUND: return Theme.FOREGROUND;
        case COLOR_HIGHLIGHTED_BACKGROUND: return Theme.HIGHLIGHT;
        case COLOR_HIGHLIGHTED_FOREGROUND: return Theme.HIGHLIGHT_TEXT;
        case COLOR_BORDER: return Theme.BORDER;
        case COLOR_HIGHLIGHTED_BORDER: return Theme.HIGHLIGHT;
        default: throw new IllegalArgumentException();
        }
    }

    public int getBorderStyle(boolean highlighted) {
        return highlighted ? Graphics.SOLID : Graphics.DOTTED;
    }

    public int getBestImageWidth(int imageType) {
        if (imageType < LIST_ELEMENT || imageType > ALERT) {
            throw new IllegalArgumentException();
        }
        return imageType == ALERT ? width : 16;
    }

    public int getBestImageHeight(int imageType) {
        if (imageType < LIST_ELEMENT || imageType > ALERT) {
            throw new IllegalArgumentException();
        }
        return imageType == ALERT ? height / 2 : 16;
    }

    /* ---- Repainting ---- */

    static void requestRepaint(Displayable d) {
        Display disp = instance;
        if (disp != null && d == disp.current) {
            disp.repaintPending = true;
            Events.wakeup();
        }
    }

    static void serviceRepaints(Displayable d) {
        Display disp = instance;
        if (disp != null && d == disp.current && disp.repaintPending) {
            disp.paint();
        }
    }

    private void paint() {
        synchronized (LOCK) {
            repaintPending = false;
            Displayable d = current;
            if (d == null) {
                return;
            }
            screenGraphics.reset();
            d.paintDisplayable(screenGraphics);
            if (menu != null) {
                screenGraphics.reset();
                menu.paint(screenGraphics, width, height);
            }
            nanojava.Screen.present(screen);
            updateSoftLabels(d);
        }
    }

    private String shownLeft, shownRight;

    /* Tells the platform which commands sit on the soft keys (shown on the
     * DS keypad), only when they change. */
    private void updateSoftLabels(Displayable d) {
        String[] labels = d.wantsSoftKeys() ? new String[2] : softLabels(d);
        if (!same(labels[0], shownLeft) || !same(labels[1], shownRight)) {
            shownLeft = labels[0];
            shownRight = labels[1];
            nanojava.Screen.softLabels(shownLeft, shownRight);
        }
    }

    private static boolean same(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    /* GameCanvas.flushGraphics: copy its buffer to the screen right away. */
    void flush(Image buffer, int x, int y, int w, int h) {
        synchronized (LOCK) {
            Graphics g = screenGraphics;
            g.reset();
            g.setClip(x, y, w, h);
            g.drawImage(buffer, 0, 0, Graphics.TOP | Graphics.LEFT);
            if (menu != null) {
                g.reset();
                menu.paint(g, width, height);
            }
            nanojava.Screen.present(screen);
            if (current != null) {
                updateSoftLabels(current);
            }
        }
    }

    /* ---- Event loop ---- */

    void stop() {
        running = false;
        Events.wakeup();
    }

    void run() {
        int[] ev = new int[2];
        while (running) {
            if (repaintPending) {
                paint();
            }
            while (serial.size() > 0) {
                Runnable r = (Runnable) serial.elementAt(0);
                serial.removeElementAt(0);
                synchronized (LOCK) {
                    r.run();
                }
            }
            int timeout = -1;
            if (repaintPending || serial.size() > 0) {
                timeout = 0;
            } else if (keyHeld) {
                timeout = (int) Math.max(1, nextRepeat - System.currentTimeMillis());
            }
            int type = Events.next(ev, timeout);
            if (type == Events.NONE) {
                if (keyHeld && System.currentTimeMillis() >= nextRepeat) {
                    nextRepeat = System.currentTimeMillis() + REPEAT_RATE;
                    key(KEY_REPEAT, heldKey);
                }
                continue;
            }
            switch (type) {
            case Events.KEY_DOWN:
                keyHeld = true;
                heldKey = ev[0];
                nextRepeat = System.currentTimeMillis() + REPEAT_DELAY;
                key(type, ev[0]);
                break;
            case Events.KEY_UP:
                if (heldKey == ev[0]) {
                    keyHeld = false;
                }
                key(type, ev[0]);
                break;
            default:
                pointer(type, ev[0], ev[1]);
                break;
            }
        }
    }

    private void key(int type, int code) {
        synchronized (LOCK) {
            Displayable d = current;
            if (d == null) {
                return;
            }
            boolean suppressed = d instanceof javax.microedition.lcdui.game.GameCanvas
                    && LcduiBridge.gameKeyState(d, type, code);
            if (menu != null) {
                if (type != Events.KEY_UP) {
                    Command c = menu.key(code);
                    if (menu.closed) {
                        menu = null;
                        repaintPending = true;
                    }
                    if (c != null) {
                        d.fireCommand(c);
                    }
                    repaintPending = true;
                }
                return;
            }
            boolean soft = code == Canvas.KEY_SOFT_LEFT || code == Canvas.KEY_SOFT_RIGHT;
            if (soft && !d.wantsSoftKeys()) {
                if (type == Events.KEY_DOWN) {
                    softKey(d, code == Canvas.KEY_SOFT_LEFT);
                }
                return;
            }
            if (!suppressed || Canvas.gameAction(code) == 0) {
                d.keyEvent(type, code);
            }
        }
    }

    /* Soft key assignment: the right key gets the first "negative"
     * command (Back, Exit, ...), or the second command when there are just
     * two and neither is negative; the left key gets the remaining command,
     * or opens a menu when several remain. A Form's focused item adds its
     * own commands ahead of the screen's. */
    static Vector[] softKeys(Displayable d) {
        Vector right = new Vector();
        Vector left = new Vector();
        Vector items = d.itemCommands();
        int ni = items == null ? 0 : items.size();
        Vector cmds = d.commands;
        for (int i = 0; i < ni + cmds.size(); i++) {
            Command c = (Command) (i < ni ? items.elementAt(i) : cmds.elementAt(i - ni));
            if (right.size() == 0 && c.isNegative()) {
                right.addElement(c);
            } else {
                left.addElement(c);
            }
        }
        if (right.size() == 0 && left.size() == 2) {
            right.addElement(left.elementAt(1));
            left.removeElementAt(1);
        }
        return new Vector[] {left, right};
    }

    private void softKey(Displayable d, boolean leftKey) {
        Vector[] keys = softKeys(d);
        Vector cmds = keys[leftKey ? 0 : 1];
        if (cmds.size() == 1) {
            d.fireCommand((Command) cmds.elementAt(0));
        } else if (cmds.size() > 1) {
            menu = new CommandMenu(cmds);
            repaintPending = true;
        }
    }

    /* Labels for the soft keys of a displayable, {left, right}; null when a
     * key has no command. */
    static String[] softLabels(Displayable d) {
        Vector[] keys = softKeys(d);
        String[] labels = new String[2];
        for (int i = 0; i < 2; i++) {
            if (keys[i].size() == 1) {
                labels[i] = ((Command) keys[i].elementAt(0)).getLabel();
            } else if (keys[i].size() > 1) {
                labels[i] = "Menu";
            }
        }
        return labels;
    }

    private void pointer(int type, int x, int y) {
        synchronized (LOCK) {
            Displayable d = current;
            if (d != null && menu == null) {
                d.pointerEvent(type, x, y);
            }
        }
    }
}
