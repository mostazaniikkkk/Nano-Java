package demo;

import java.io.IOException;
import javax.microedition.lcdui.*;
import javax.microedition.lcdui.game.GameCanvas;
import javax.microedition.midlet.MIDlet;

/* Exercises the drawing API; tests/midlets compare its screenshot. */
public class DemoMIDlet extends MIDlet {
    static class Board extends GameCanvas implements Runnable {
        private final Image sprite;
        private int x = 100, y = 200;
        private int frames;

        Board() throws IOException {
            super(true);
            setFullScreenMode(true);
            sprite = Image.createImage("/arrow.png");
        }

        public void run() {
            Graphics g = getGraphics();
            while (frames < 30) {
                int keys = getKeyStates();
                if ((keys & LEFT_PRESSED) != 0) x -= 4;
                if ((keys & RIGHT_PRESSED) != 0) x += 4;
                if ((keys & UP_PRESSED) != 0) y -= 4;
                if ((keys & DOWN_PRESSED) != 0) y += 4;
                draw(g);
                flushGraphics();
                frames++;
                try {
                    Thread.sleep(30);
                } catch (InterruptedException e) {
                    return;
                }
            }
            System.out.println("frames " + frames + " pos " + x + "," + y);
        }

        private void draw(Graphics g) {
            int w = getWidth(), h = getHeight();
            g.setColor(0x102040);
            g.fillRect(0, 0, w, h);
            g.setColor(0xFF0000);
            g.fillRect(10, 10, 40, 30);
            g.setColor(0x00FF00);
            g.drawRect(60, 10, 40, 30);
            g.setColor(0xFFFF00);
            g.drawLine(110, 10, 150, 40);
            g.setColor(0x00FFFF);
            g.fillArc(160, 10, 40, 30, 0, 270);
            g.setColor(0xFF00FF);
            g.fillTriangle(10, 90, 50, 50, 60, 100);
            g.setColor(0xFFFFFF);
            g.drawRoundRect(70, 50, 60, 40, 16, 16);
            g.drawArc(140, 50, 50, 50, 0, 360);
            g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL));
            g.drawString("Small: Nano Java", 10, 110, Graphics.TOP | Graphics.LEFT);
            g.setFont(Font.getDefaultFont());
            g.drawString("Medium áéíñ 123", 10, 125, Graphics.TOP | Graphics.LEFT);
            g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_LARGE));
            g.drawString("Large Bold", w / 2, 145, Graphics.TOP | Graphics.HCENTER);
            for (int t = 0; t < 8; t++) {
                g.drawRegion(sprite, 0, 0, sprite.getWidth(), sprite.getHeight(), t,
                        10 + t * 28, 175, Graphics.TOP | Graphics.LEFT);
            }
            int[] rgb = new int[32 * 16];
            for (int i = 0; i < rgb.length; i++) {
                rgb[i] = ((i % 32) * 8) << 24 | 0xFFFFFF;
            }
            g.drawRGB(rgb, 0, 32, 10, 215, 32, 16, true);
            g.setColor(0xFF8000);
            g.fillRect(x, y, 16, 16);
        }
    }

    protected void startApp() {
        try {
            Board b = new Board();
            Display.getDisplay(this).setCurrent(b);
            new Thread(b).start();
        } catch (IOException e) {
            System.out.println("cannot load sprite: " + e.getMessage());
            notifyDestroyed();
        }
    }

    protected void pauseApp() {
    }

    protected void destroyApp(boolean unconditional) {
    }
}
