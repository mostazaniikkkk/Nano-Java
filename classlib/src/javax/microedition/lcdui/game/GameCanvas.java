package javax.microedition.lcdui.game;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.LcduiBridge;

public abstract class GameCanvas extends Canvas {
    public static final int UP_PRESSED = 1 << Canvas.UP;
    public static final int DOWN_PRESSED = 1 << Canvas.DOWN;
    public static final int LEFT_PRESSED = 1 << Canvas.LEFT;
    public static final int RIGHT_PRESSED = 1 << Canvas.RIGHT;
    public static final int FIRE_PRESSED = 1 << Canvas.FIRE;
    public static final int GAME_A_PRESSED = 1 << Canvas.GAME_A;
    public static final int GAME_B_PRESSED = 1 << Canvas.GAME_B;
    public static final int GAME_C_PRESSED = 1 << Canvas.GAME_C;
    public static final int GAME_D_PRESSED = 1 << Canvas.GAME_D;

    static {
        LcduiBridge.setGameKeys(new LcduiBridge.GameKeys() {
            public boolean keyState(GameCanvas c, boolean down, int action) {
                if (action == 0) {
                    return c.suppressKeyEvents;
                }
                int bit = 1 << action;
                synchronized (c) {
                    if (down) {
                        c.held |= bit;
                        c.latched |= bit;
                    } else {
                        c.held &= ~bit;
                    }
                }
                return c.suppressKeyEvents;
            }
        });
    }

    private final Image buffer;
    private final boolean suppressKeyEvents;
    private int held;
    private int latched;

    protected GameCanvas(boolean suppressKeyEvents) {
        this.suppressKeyEvents = suppressKeyEvents;
        buffer = Image.createImage(getWidth(), getHeight());
    }

    protected Graphics getGraphics() {
        return LcduiBridge.graphicsFor(buffer);
    }

    public int getKeyStates() {
        synchronized (this) {
            int s = held | latched;
            latched = 0;
            return s;
        }
    }

    public void paint(Graphics g) {
        g.drawImage(buffer, 0, 0, Graphics.TOP | Graphics.LEFT);
    }

    public void flushGraphics(int x, int y, int width, int height) {
        if (LcduiBridge.isCurrent(this)) {
            LcduiBridge.flush(buffer, x, y, width, height);
        }
    }

    public void flushGraphics() {
        flushGraphics(0, 0, getWidth(), getHeight());
    }
}
