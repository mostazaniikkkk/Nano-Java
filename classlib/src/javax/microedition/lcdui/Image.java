package javax.microedition.lcdui;

import java.io.IOException;
import java.io.InputStream;

/*
 * Pixels are 15-bit colors (red in the low bits) in a short[]; images with
 * transparency carry an alpha byte per pixel. Mutable images are opaque.
 */
public class Image {
    int width;
    int height;
    short[] pixels;
    byte[] alpha;
    boolean mutable;

    Image() {
    }

    public static Image createImage(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException();
        }
        Image img = new Image();
        initBlank(img, width, height);
        img.mutable = true;
        return img;
    }

    public static Image createImage(Image source) {
        if (!source.mutable) {
            return source;
        }
        Image img = new Image();
        initRegion(img, source, 0, 0, source.width, source.height, 0);
        return img;
    }

    public static Image createImage(String name) throws IOException {
        if (name == null) {
            throw new NullPointerException();
        }
        byte[] data = nanojava.Resources.read(name.startsWith("/") ? name.substring(1) : name);
        if (data == null) {
            throw new IOException("resource not found: " + name);
        }
        Image img = new Image();
        if (!decode(img, data, 0, data.length)) {
            throw new IOException("cannot decode image: " + name);
        }
        return img;
    }

    public static Image createImage(byte[] imageData, int imageOffset, int imageLength) {
        Image img = new Image();
        if (!decode(img, imageData, imageOffset, imageLength)) {
            throw new IllegalArgumentException("cannot decode image");
        }
        return img;
    }

    public static Image createImage(Image image, int x, int y, int width, int height, int transform) {
        if (image == null) {
            throw new NullPointerException();
        }
        if (width <= 0 || height <= 0 || x < 0 || y < 0 || x + width > image.width
                || y + height > image.height || transform < 0 || transform > 7) {
            throw new IllegalArgumentException();
        }
        if (!image.mutable && transform == 0 && x == 0 && y == 0 && width == image.width
                && height == image.height) {
            return image;
        }
        Image img = new Image();
        initRegion(img, image, x, y, width, height, transform);
        return img;
    }

    public static Image createImage(InputStream stream) throws IOException {
        if (stream == null) {
            throw new NullPointerException();
        }
        byte[] buf = new byte[4096];
        int n = 0;
        for (;;) {
            if (n == buf.length) {
                byte[] nb = new byte[buf.length * 2];
                System.arraycopy(buf, 0, nb, 0, n);
                buf = nb;
            }
            int r = stream.read(buf, n, buf.length - n);
            if (r < 0) {
                break;
            }
            n += r;
        }
        Image img = new Image();
        if (!decode(img, buf, 0, n)) {
            throw new IOException("cannot decode image");
        }
        return img;
    }

    public static Image createRGBImage(int[] rgb, int width, int height, boolean processAlpha) {
        if (rgb == null) {
            throw new NullPointerException();
        }
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException();
        }
        Image img = new Image();
        initRGB(img, rgb, width, height, processAlpha);
        return img;
    }

    public Graphics getGraphics() {
        if (!mutable) {
            throw new IllegalStateException();
        }
        return new Graphics(this);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean isMutable() {
        return mutable;
    }

    public native void getRGB(int[] rgbData, int offset, int scanlength, int x, int y, int width, int height);

    private static native boolean decode(Image img, byte[] data, int off, int len);

    private static native void initRGB(Image img, int[] rgb, int w, int h, boolean alpha);

    private static native void initBlank(Image img, int w, int h);

    private static native void initRegion(Image dst, Image src, int x, int y, int w, int h, int transform);
}
