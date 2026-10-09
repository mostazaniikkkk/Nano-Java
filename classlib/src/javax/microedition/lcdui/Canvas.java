package javax.microedition.lcdui;

public abstract class Canvas extends Displayable {
    public static final int UP = 1;
    public static final int DOWN = 6;
    public static final int LEFT = 2;
    public static final int RIGHT = 5;
    public static final int FIRE = 8;
    public static final int GAME_A = 9;
    public static final int GAME_B = 10;
    public static final int GAME_C = 11;
    public static final int GAME_D = 12;

    public static final int KEY_NUM0 = 48;
    public static final int KEY_NUM1 = 49;
    public static final int KEY_NUM2 = 50;
    public static final int KEY_NUM3 = 51;
    public static final int KEY_NUM4 = 52;
    public static final int KEY_NUM5 = 53;
    public static final int KEY_NUM6 = 54;
    public static final int KEY_NUM7 = 55;
    public static final int KEY_NUM8 = 56;
    public static final int KEY_NUM9 = 57;
    public static final int KEY_STAR = 42;
    public static final int KEY_POUND = 35;

    /* Device key codes for the directional keys and soft keys (the values
     * Nokia and Sony Ericsson phones use, which most games expect). */
    static final int KEY_UP = -1;
    static final int KEY_DOWN = -2;
    static final int KEY_LEFT = -3;
    static final int KEY_RIGHT = -4;
    static final int KEY_FIRE = -5;
    static final int KEY_SOFT_LEFT = -6;
    static final int KEY_SOFT_RIGHT = -7;

    /* Dirty region of a pending repaint, in canvas coordinates. */
    int dirtyX1, dirtyY1, dirtyX2, dirtyY2;
    boolean dirty;
    boolean fullScreen;

    protected Canvas() {
    }

    public boolean isDoubleBuffered() {
        return true;
    }

    public boolean hasPointerEvents() {
        return true;
    }

    public boolean hasPointerMotionEvents() {
        return true;
    }

    public boolean hasRepeatEvents() {
        return true;
    }

    public int getKeyCode(int gameAction) {
        switch (gameAction) {
        case UP: return KEY_UP;
        case DOWN: return KEY_DOWN;
        case LEFT: return KEY_LEFT;
        case RIGHT: return KEY_RIGHT;
        case FIRE: return KEY_FIRE;
        case GAME_A: return KEY_NUM1;
        case GAME_B: return KEY_NUM3;
        case GAME_C: return KEY_NUM7;
        case GAME_D: return KEY_NUM9;
        default: throw new IllegalArgumentException();
        }
    }

    public String getKeyName(int keyCode) {
        switch (keyCode) {
        case KEY_UP: return "Up";
        case KEY_DOWN: return "Down";
        case KEY_LEFT: return "Left";
        case KEY_RIGHT: return "Right";
        case KEY_FIRE: return "Select";
        case KEY_SOFT_LEFT: return "Soft1";
        case KEY_SOFT_RIGHT: return "Soft2";
        case KEY_STAR: return "*";
        case KEY_POUND: return "#";
        default:
            if (keyCode >= KEY_NUM0 && keyCode <= KEY_NUM9) {
                return String.valueOf((char) keyCode);
            }
            throw new IllegalArgumentException();
        }
    }

    public int getGameAction(int keyCode) {
        return gameAction(keyCode);
    }

    static int gameAction(int keyCode) {
        switch (keyCode) {
        case KEY_UP: case KEY_NUM2: return UP;
        case KEY_DOWN: case KEY_NUM8: return DOWN;
        case KEY_LEFT: case KEY_NUM4: return LEFT;
        case KEY_RIGHT: case KEY_NUM6: return RIGHT;
        case KEY_FIRE: case KEY_NUM5: return FIRE;
        case KEY_NUM1: return GAME_A;
        case KEY_NUM3: return GAME_B;
        case KEY_NUM7: return GAME_C;
        case KEY_NUM9: return GAME_D;
        default: return 0;
        }
    }

    public void setFullScreenMode(boolean mode) {
        if (fullScreen != mode) {
            fullScreen = mode;
            repaint();
        }
    }

    protected void keyPressed(int keyCode) {
    }

    protected void keyRepeated(int keyCode) {
    }

    protected void keyReleased(int keyCode) {
    }

    protected void pointerPressed(int x, int y) {
    }

    protected void pointerReleased(int x, int y) {
    }

    protected void pointerDragged(int x, int y) {
    }

    public final void repaint(int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        synchronized (this) {
            if (!dirty) {
                dirtyX1 = x;
                dirtyY1 = y;
                dirtyX2 = x + width;
                dirtyY2 = y + height;
                dirty = true;
            } else {
                if (x < dirtyX1) dirtyX1 = x;
                if (y < dirtyY1) dirtyY1 = y;
                if (x + width > dirtyX2) dirtyX2 = x + width;
                if (y + height > dirtyY2) dirtyY2 = y + height;
            }
        }
        if (shown) {
            Display.requestRepaint(this);
        }
    }

    public final void repaint() {
        repaint(0, 0, getWidth(), getHeight());
    }

    public final void serviceRepaints() {
        Display.serviceRepaints(this);
    }

    protected void showNotify() {
    }

    protected void hideNotify() {
    }

    protected abstract void paint(Graphics g);

    /* ---- Display hooks ---- */

    void paintDisplayable(Graphics g) {
        int x1, y1, x2, y2;
        synchronized (this) {
            if (dirty) {
                x1 = dirtyX1;
                y1 = dirtyY1;
                x2 = dirtyX2;
                y2 = dirtyY2;
            } else {
                x1 = 0;
                y1 = 0;
                x2 = getWidth();
                y2 = getHeight();
            }
            dirty = false;
        }
        g.setClip(x1, y1, x2 - x1, y2 - y1);
        paint(g);
    }

    void showNotify0() {
        repaint();
        showNotify();
    }

    void hideNotify0() {
        hideNotify();
    }

    boolean wantsSoftKeys() {
        return commands.size() == 0;
    }

    boolean keyEvent(int type, int keyCode) {
        switch (type) {
        case nanojava.Events.KEY_DOWN:
            keyPressed(keyCode);
            break;
        case nanojava.Events.KEY_UP:
            keyReleased(keyCode);
            break;
        default:
            keyRepeated(keyCode);
            break;
        }
        return true;
    }

    void pointerEvent(int type, int x, int y) {
        switch (type) {
        case nanojava.Events.POINTER_DOWN:
            pointerPressed(x, y);
            break;
        case nanojava.Events.POINTER_UP:
            pointerReleased(x, y);
            break;
        default:
            pointerDragged(x, y);
            break;
        }
    }
}
