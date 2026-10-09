package javax.microedition.lcdui;

import javax.microedition.lcdui.game.GameCanvas;

/**
 * Internal: lets nanojava.* and javax.microedition.lcdui.game reach
 * package-private parts of lcdui. Not part of the MIDP API.
 */
public final class LcduiBridge {
    private static GameKeys gameKeys;

    /** Implemented by GameCanvas to receive raw key state changes. */
    public interface GameKeys {
        /** Returns true if the key event must not reach keyPressed(). */
        boolean keyState(GameCanvas c, boolean down, int gameAction);
    }

    private LcduiBridge() {
    }

    public static void setGameKeys(GameKeys k) {
        gameKeys = k;
    }

    static boolean gameKeyState(Displayable d, int type, int code) {
        if (gameKeys == null) {
            return false;
        }
        if (type == Display.KEY_REPEAT) {
            return gameKeys.keyState((GameCanvas) d, true, 0) && Canvas.gameAction(code) != 0;
        }
        return gameKeys.keyState((GameCanvas) d, type == nanojava.Events.KEY_DOWN, Canvas.gameAction(code));
    }

    public static void flush(Image buffer, int x, int y, int w, int h) {
        Display.get().flush(buffer, x, y, w, h);
    }

    public static boolean isCurrent(Displayable d) {
        return Display.get().getCurrent() == d;
    }

    public static Graphics graphicsFor(Image img) {
        return new Graphics(img);
    }

    /** The image a Graphics draws into (the screen buffer for Canvas). */
    public static Image targetOf(Graphics g) {
        return g.target;
    }

    public static void resetGraphics(Graphics g) {
        g.reset();
    }

    public static void runEventLoop() {
        Display.get().run();
    }

    public static void stopEventLoop() {
        Display.get().stop();
    }
}
