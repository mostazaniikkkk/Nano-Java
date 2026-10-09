package javax.microedition.lcdui;

public abstract class CustomItem extends Item {
    protected static final int TRAVERSE_HORIZONTAL = 1;
    protected static final int TRAVERSE_VERTICAL = 2;
    protected static final int KEY_PRESS = 4;
    protected static final int KEY_RELEASE = 8;
    protected static final int KEY_REPEAT = 0x10;
    protected static final int POINTER_PRESS = 0x20;
    protected static final int POINTER_RELEASE = 0x40;
    protected static final int POINTER_DRAG = 0x80;
    protected static final int NONE = 0;

    private int contentW = -1;
    private int contentH = -1;
    private final int[] visRect = new int[4];
    private boolean traversing;

    protected CustomItem(String label) {
        super(label);
    }

    public int getGameAction(int keyCode) {
        return Canvas.gameAction(keyCode);
    }

    protected final int getInteractionModes() {
        return TRAVERSE_HORIZONTAL | TRAVERSE_VERTICAL | KEY_PRESS | KEY_RELEASE | KEY_REPEAT
                | POINTER_PRESS | POINTER_RELEASE | POINTER_DRAG;
    }

    protected abstract int getMinContentWidth();

    protected abstract int getMinContentHeight();

    protected abstract int getPrefContentWidth(int height);

    protected abstract int getPrefContentHeight(int width);

    protected void sizeChanged(int w, int h) {
    }

    protected final void invalidate() {
        invalidate0();
    }

    protected abstract void paint(Graphics g, int w, int h);

    protected final void repaint() {
        repaint0();
    }

    protected final void repaint(int x, int y, int w, int h) {
        repaint0();
    }

    protected boolean traverse(int dir, int viewportWidth, int viewportHeight, int[] visRect_inout) {
        return false;
    }

    protected void traverseOut() {
    }

    protected void keyPressed(int keyCode) {
    }

    protected void keyReleased(int keyCode) {
    }

    protected void keyRepeated(int keyCode) {
    }

    protected void pointerPressed(int x, int y) {
    }

    protected void pointerReleased(int x, int y) {
    }

    protected void pointerDragged(int x, int y) {
    }

    protected void showNotify() {
    }

    protected void hideNotify() {
    }

    /* ---- Item protocol ---- */

    int measureBody(int w) {
        int pw = lockedWidth >= 0 ? lockedWidth : getPrefContentWidth(lockedHeight >= 0 ? lockedHeight : -1);
        pw = Math.max(pw, getMinContentWidth());
        if (pw > w) {
            pw = w;
        }
        int ph = Math.max(getPrefContentHeight(pw), getMinContentHeight());
        if (pw != contentW || ph != contentH) {
            contentW = pw;
            contentH = ph;
            sizeChanged(pw, ph);
        }
        return ph;
    }

    void paintBody(Graphics g, int w, int h, boolean focused) {
        int tx = g.tx, ty = g.ty;
        int x1 = g.cx1, y1 = g.cy1, x2 = g.cx2, y2 = g.cy2;
        int cw = Math.min(contentW, w);
        int ch = contentH;
        g.clipRect(0, 0, cw, ch);
        g.setColor(0);
        g.setFont(Font.getDefaultFont());
        g.setStrokeStyle(Graphics.SOLID);
        try {
            if (g.cx2 > g.cx1 && g.cy2 > g.cy1) {
                paint(g, cw, ch);
            }
        } finally {
            g.tx = tx;
            g.ty = ty;
            g.cx1 = x1;
            g.cy1 = y1;
            g.cx2 = x2;
            g.cy2 = y2;
        }
    }

    boolean isFocusable() {
        return true;
    }

    boolean traverseItem(int dir, boolean entering) {
        if (!entering && !traversing) {
            return false;
        }
        Screen s = owner;
        int vw = s != null ? s.contentWidth() : Display.screenWidth();
        int vh = s != null ? s.contentHeight() : Display.screenHeight();
        visRect[0] = 0;
        visRect[1] = 0;
        visRect[2] = Math.max(0, contentW);
        visRect[3] = Math.max(0, contentH);
        traversing = traverse(dir == 0 ? NONE : dir, vw, vh, visRect);
        return traversing;
    }

    void traverseOutItem() {
        traversing = false;
        traverseOut();
    }

    boolean itemKey(int type, int code) {
        if (type == nanojava.Events.KEY_DOWN) {
            keyPressed(code);
        } else if (type == nanojava.Events.KEY_UP) {
            keyReleased(code);
        } else {
            keyRepeated(code);
        }
        return true;
    }

    boolean itemPointer(int type, int x, int y) {
        if (type == nanojava.Events.POINTER_DOWN) {
            pointerPressed(x, y);
        } else if (type == nanojava.Events.POINTER_UP) {
            pointerReleased(x, y);
        } else {
            pointerDragged(x, y);
        }
        return true;
    }

    void showItem() {
        showNotify();
    }

    void hideItem() {
        hideNotify();
    }
}
