package touchtest;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Graphics;
import javax.microedition.midlet.MIDlet;

/* A drawing board: pointer presses and drags leave marks, and the last
 * position is shown, to check touch input and coordinate mapping. */
public class TouchTest extends MIDlet {
    static class Board extends Canvas {
        private final int[] xs = new int[512];
        private final int[] ys = new int[512];
        private int n;
        private String last = "touch the screen";

        Board() {
            setFullScreenMode(true);
        }

        private void add(int x, int y, String what) {
            if (n < xs.length) {
                xs[n] = x;
                ys[n] = y;
                n++;
            }
            last = what + " " + x + "," + y;
            System.out.println(last);
            repaint();
        }

        protected void pointerPressed(int x, int y) {
            add(x, y, "press");
        }

        protected void pointerDragged(int x, int y) {
            add(x, y, "drag");
        }

        protected void pointerReleased(int x, int y) {
            last = "release " + x + "," + y;
            System.out.println(last);
            repaint();
        }

        protected void paint(Graphics g) {
            int w = getWidth(), h = getHeight();
            g.setColor(0x10203A);
            g.fillRect(0, 0, w, h);
            g.setColor(0x305080);
            for (int x = 0; x < w; x += 20) {
                g.drawLine(x, 0, x, h);
            }
            for (int y = 0; y < h; y += 20) {
                g.drawLine(0, y, w, y);
            }
            g.setColor(0xFFC040);
            for (int i = 0; i < n; i++) {
                g.fillArc(xs[i] - 4, ys[i] - 4, 9, 9, 0, 360);
            }
            g.setColor(0xFFFFFF);
            g.drawRect(0, 0, w - 1, h - 1);
            g.drawString(last, 4, 4, Graphics.TOP | Graphics.LEFT);
        }
    }

    protected void startApp() {
        Display.getDisplay(this).setCurrent(new Board());
    }

    protected void pauseApp() {
    }

    protected void destroyApp(boolean unconditional) {
    }
}
