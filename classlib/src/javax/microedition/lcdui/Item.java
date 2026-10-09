package javax.microedition.lcdui;

import java.util.Vector;

/*
 * Base of the Form items. An item is laid out as an optional label (bold,
 * wrapped) above a body that each subclass measures and paints. Form calls
 * layout() to get the height for a given width, then paintItem() with the
 * origin at the item's top-left corner.
 */
public abstract class Item {
    public static final int LAYOUT_DEFAULT = 0;
    public static final int LAYOUT_LEFT = 1;
    public static final int LAYOUT_RIGHT = 2;
    public static final int LAYOUT_CENTER = 3;
    public static final int LAYOUT_TOP = 0x10;
    public static final int LAYOUT_BOTTOM = 0x20;
    public static final int LAYOUT_VCENTER = 0x30;
    public static final int LAYOUT_NEWLINE_BEFORE = 0x100;
    public static final int LAYOUT_NEWLINE_AFTER = 0x200;
    public static final int LAYOUT_SHRINK = 0x400;
    public static final int LAYOUT_EXPAND = 0x800;
    public static final int LAYOUT_VSHRINK = 0x1000;
    public static final int LAYOUT_VEXPAND = 0x2000;
    public static final int LAYOUT_2 = 0x4000;

    public static final int PLAIN = 0;
    public static final int HYPERLINK = 1;
    public static final int BUTTON = 2;

    private static final int LAYOUT_BITS = 0x7F33;

    /* Horizontal margin around the body and vertical padding. */
    static final int PAD = 4;
    static final int VPAD = 3;

    Screen owner;
    String label;
    int layout;
    final Vector commands = new Vector();
    Command defaultCommand;
    ItemCommandListener commandListener;
    int lockedWidth = -1;
    int lockedHeight = -1;

    /* Layout results: y in the owner's content, total height, parts. */
    int y;
    int height;
    int labelHeight;
    int bodyHeight;
    private TextWrap labelWrap;

    Item(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
        invalidate0();
    }

    public int getLayout() {
        return layout;
    }

    public void setLayout(int layout) {
        if ((layout & ~LAYOUT_BITS) != 0) {
            throw new IllegalArgumentException();
        }
        this.layout = layout;
        invalidate0();
    }

    public void addCommand(Command cmd) {
        if (cmd == null) {
            throw new NullPointerException();
        }
        synchronized (commands) {
            if (commands.contains(cmd)) {
                return;
            }
            int i = 0;
            while (i < commands.size()
                    && ((Command) commands.elementAt(i)).getPriority() <= cmd.getPriority()) {
                i++;
            }
            commands.insertElementAt(cmd, i);
        }
        invalidate0();
    }

    public void removeCommand(Command cmd) {
        commands.removeElement(cmd);
        if (cmd == defaultCommand) {
            defaultCommand = null;
        }
        invalidate0();
    }

    public void setDefaultCommand(Command cmd) {
        defaultCommand = cmd;
        if (cmd != null) {
            addCommand(cmd);
        }
    }

    public void setItemCommandListener(ItemCommandListener l) {
        commandListener = l;
    }

    public int getPreferredWidth() {
        return lockedWidth >= 0 ? lockedWidth : availWidth();
    }

    public int getPreferredHeight() {
        return lockedHeight >= 0 ? lockedHeight : layout(getPreferredWidth());
    }

    public void setPreferredSize(int width, int height) {
        if (width < -1 || height < -1) {
            throw new IllegalArgumentException();
        }
        lockedWidth = width;
        lockedHeight = height;
        invalidate0();
    }

    public int getMinimumWidth() {
        return Math.min(availWidth(), 2 * PAD + 16);
    }

    public int getMinimumHeight() {
        return layout(availWidth());
    }

    public void notifyStateChanged() {
        Screen s = owner;
        if (s instanceof Form) {
            ((Form) s).itemStateChanged(this);
        }
    }

    /* ---- Internal protocol with Form ---- */

    int availWidth() {
        Screen s = owner;
        return s != null ? s.contentWidth() : Display.screenWidth();
    }

    /* Lays the item out for width w; caches and returns its height. */
    int layout(int w) {
        int bw = w - 2 * PAD;
        labelHeight = 0;
        String l = label;
        if (l != null && l.length() > 0) {
            if (labelWrap == null) {
                labelWrap = new TextWrap();
            }
            labelHeight = labelWrap.wrap(l, Theme.BOLD, bw) * Theme.BOLD.getHeight() + 1;
        }
        bodyHeight = measureBody(bw);
        int h = labelHeight + bodyHeight + 2 * VPAD;
        if (lockedHeight > h) {
            h = lockedHeight;
        }
        height = h;
        return h;
    }

    /* Height of the body for width w. */
    abstract int measureBody(int w);

    /* Paints the body; the origin is its top-left corner. */
    abstract void paintBody(Graphics g, int w, int h, boolean focused);

    void paintItem(Graphics g, int w, boolean focused) {
        if (focused) {
            g.setColor(Theme.FOCUS_BG);
            g.fillRect(1, 0, w - 2, height);
            g.setColor(Theme.HIGHLIGHT);
            g.drawRect(1, 0, w - 3, height - 1);
        }
        int by = VPAD;
        if (labelHeight > 0 && labelWrap != null) {
            g.setFont(Theme.BOLD);
            g.setColor(Theme.FOREGROUND);
            labelWrap.paint(g, PAD, by, Theme.BOLD.getHeight());
            by += labelHeight;
        }
        g.translate(PAD, by);
        paintBody(g, w - 2 * PAD, bodyHeight, focused);
        g.translate(-PAD, -by);
    }

    /* x of something cw wide inside w, per the horizontal layout bits. */
    int alignX(int w, int cw) {
        switch (layout & LAYOUT_CENTER) {
        case LAYOUT_RIGHT: return w - cw;
        case LAYOUT_CENTER: return (w - cw) / 2;
        default: return 0;
        }
    }

    boolean isFocusable() {
        return commands.size() > 0;
    }

    /* Focus traversal inside the item (dir is Canvas.UP/DOWN, or 0 when
     * the focus is placed directly). entering: the focus just arrived.
     * Returns true if the item moved its internal focus (keeps focus). */
    boolean traverseItem(int dir, boolean entering) {
        return false;
    }

    void traverseOutItem() {
    }

    /* Key event while focused (all types); true if consumed. */
    boolean itemKey(int type, int code) {
        return false;
    }

    /* Pointer event in body coordinates while focused; true if consumed. */
    boolean itemPointer(int type, int x, int y) {
        return false;
    }

    /* Focus area relative to the item's top, for scrolling. */
    int focusTop() {
        return 0;
    }

    int focusBottom() {
        return height;
    }

    void showItem() {
    }

    void hideItem() {
    }

    /* Command run by fire: the default command, or the only command. */
    Command activationCommand() {
        if (defaultCommand != null) {
            return defaultCommand;
        }
        try {
            return commands.size() == 1 ? (Command) commands.elementAt(0) : null;
        } catch (ArrayIndexOutOfBoundsException e) {
            return null;
        }
    }

    void fireItemCommand(Command c) {
        ItemCommandListener l = commandListener;
        if (l != null) {
            l.commandAction(c, this);
        }
    }

    void repaint0() {
        Screen s = owner;
        if (s != null) {
            s.repaintDisplayable();
        }
    }

    void invalidate0() {
        Screen s = owner;
        if (s != null) {
            s.itemInvalidated();
        }
    }
}
