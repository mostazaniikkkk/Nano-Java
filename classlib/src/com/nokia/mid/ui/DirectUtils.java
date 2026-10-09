package com.nokia.mid.ui;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

public class DirectUtils {
    private DirectUtils() {
    }

    public static DirectGraphics getDirectGraphics(Graphics g) {
        if (g == null) {
            throw new NullPointerException();
        }
        return new DirectGraphicsImpl(g);
    }

    /* Mutable images are opaque here, so the alpha of argb is dropped. */
    public static Image createImage(int width, int height, int argb) {
        Image img = Image.createImage(width, height);
        Graphics g = img.getGraphics();
        g.setColor(argb & 0xffffff);
        g.fillRect(0, 0, width, height);
        return img;
    }

    public static Image createImage(byte[] imageData, int imageOffset, int imageLength) {
        return Image.createImage(imageData, imageOffset, imageLength);
    }
}
