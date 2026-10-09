package javax.microedition.lcdui;

/*
 * Drawing state lives in plain fields that the natives read directly:
 * translation (tx, ty), the clip in absolute target coordinates (cx1..cy2,
 * exclusive), the current color as RGB and as a packed pixel (15-bit
 * color, red in the low bits, bit 15 set).
 */
public class Graphics {
    public static final int HCENTER = 1;
    public static final int VCENTER = 2;
    public static final int LEFT = 4;
    public static final int RIGHT = 8;
    public static final int TOP = 16;
    public static final int BOTTOM = 32;
    public static final int BASELINE = 64;

    public static final int SOLID = 0;
    public static final int DOTTED = 1;

    Image target;
    int tx, ty;
    int cx1, cy1, cx2, cy2;
    int rgb;
    int pixel;
    int stroke;
    Font font;

    Graphics(Image target) {
        this.target = target;
        reset();
    }

    /* Restores the state a fresh paint() call starts with. */
    void reset() {
        tx = 0;
        ty = 0;
        cx1 = 0;
        cy1 = 0;
        cx2 = target.width;
        cy2 = target.height;
        setColor(0);
        stroke = SOLID;
        font = Font.getDefaultFont();
    }

    public void translate(int x, int y) {
        tx += x;
        ty += y;
    }

    public int getTranslateX() {
        return tx;
    }

    public int getTranslateY() {
        return ty;
    }

    public int getColor() {
        return rgb;
    }

    public int getRedComponent() {
        return (rgb >> 16) & 0xff;
    }

    public int getGreenComponent() {
        return (rgb >> 8) & 0xff;
    }

    public int getBlueComponent() {
        return rgb & 0xff;
    }

    public int getGrayScale() {
        return (getRedComponent() * 30 + getGreenComponent() * 59 + getBlueComponent() * 11) / 100;
    }

    public void setColor(int red, int green, int blue) {
        if ((red | green | blue) < 0 || red > 255 || green > 255 || blue > 255) {
            throw new IllegalArgumentException();
        }
        setColor((red << 16) | (green << 8) | blue);
    }

    public void setColor(int RGB) {
        rgb = RGB & 0xffffff;
        pixel = 0x8000 | ((rgb >> 19) & 31) | (((rgb >> 11) & 31) << 5) | (((rgb >> 3) & 31) << 10);
    }

    public void setGrayScale(int value) {
        if (value < 0 || value > 255) {
            throw new IllegalArgumentException();
        }
        setColor(value, value, value);
    }

    public Font getFont() {
        return font;
    }

    public void setStrokeStyle(int style) {
        if (style != SOLID && style != DOTTED) {
            throw new IllegalArgumentException();
        }
        stroke = style;
    }

    public int getStrokeStyle() {
        return stroke;
    }

    public void setFont(Font font) {
        this.font = font != null ? font : Font.getDefaultFont();
    }

    public int getClipX() {
        return cx1 - tx;
    }

    public int getClipY() {
        return cy1 - ty;
    }

    public int getClipWidth() {
        return cx2 - cx1;
    }

    public int getClipHeight() {
        return cy2 - cy1;
    }

    public void clipRect(int x, int y, int width, int height) {
        int x1 = x + tx;
        int y1 = y + ty;
        int x2 = x1 + width;
        int y2 = y1 + height;
        if (x1 > cx1) cx1 = x1;
        if (y1 > cy1) cy1 = y1;
        if (x2 < cx2) cx2 = x2;
        if (y2 < cy2) cy2 = y2;
        if (cx2 < cx1) cx2 = cx1;
        if (cy2 < cy1) cy2 = cy1;
    }

    public void setClip(int x, int y, int width, int height) {
        int x1 = x + tx;
        int y1 = y + ty;
        int x2 = x1 + width;
        int y2 = y1 + height;
        cx1 = Math.max(0, x1);
        cy1 = Math.max(0, y1);
        cx2 = Math.min(target.width, x2);
        cy2 = Math.min(target.height, y2);
        if (cx2 < cx1) cx2 = cx1;
        if (cy2 < cy1) cy2 = cy1;
    }

    public native void drawLine(int x1, int y1, int x2, int y2);

    public native void fillRect(int x, int y, int width, int height);

    public native void drawRect(int x, int y, int width, int height);

    public native void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight);

    public native void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight);

    public native void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle);

    public native void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle);

    public native void fillTriangle(int x1, int y1, int x2, int y2, int x3, int y3);

    private static boolean validTextAnchor(int anchor) {
        if (anchor == 0) {
            return true;
        }
        int h = anchor & (LEFT | RIGHT | HCENTER);
        int v = anchor & (TOP | BOTTOM | BASELINE);
        return (anchor & ~(LEFT | RIGHT | HCENTER | TOP | BOTTOM | BASELINE)) == 0
                && (h == LEFT || h == RIGHT || h == HCENTER)
                && (v == TOP || v == BOTTOM || v == BASELINE);
    }

    private int textX(int x, int width, int anchor) {
        if ((anchor & RIGHT) != 0) {
            return x - width;
        }
        if ((anchor & HCENTER) != 0) {
            return x - width / 2;
        }
        return x;
    }

    private int textY(int y, int anchor) {
        if ((anchor & BOTTOM) != 0) {
            return y - font.getHeight();
        }
        if ((anchor & BASELINE) != 0) {
            return y - font.getBaselinePosition();
        }
        return y;
    }

    public void drawString(String str, int x, int y, int anchor) {
        drawSubstring(str, 0, str.length(), x, y, anchor);
    }

    public void drawSubstring(String str, int offset, int len, int x, int y, int anchor) {
        if (str == null) {
            throw new NullPointerException();
        }
        if (offset < 0 || len < 0 || offset > str.length() - len) {
            throw new StringIndexOutOfBoundsException();
        }
        if (!validTextAnchor(anchor)) {
            throw new IllegalArgumentException();
        }
        int w = (anchor & (RIGHT | HCENTER)) != 0 ? font.substringWidth(str, offset, len) : 0;
        drawString0(str, offset, len, textX(x, w, anchor), textY(y, anchor));
    }

    public void drawChar(char character, int x, int y, int anchor) {
        char[] c = {character};
        drawChars(c, 0, 1, x, y, anchor);
    }

    public void drawChars(char[] data, int offset, int length, int x, int y, int anchor) {
        if (offset < 0 || length < 0 || offset > data.length - length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        if (!validTextAnchor(anchor)) {
            throw new IllegalArgumentException();
        }
        int w = (anchor & (RIGHT | HCENTER)) != 0 ? font.charsWidth(data, offset, length) : 0;
        drawChars0(data, offset, length, textX(x, w, anchor), textY(y, anchor));
    }

    private native void drawString0(String s, int off, int len, int x, int y);

    private native void drawChars0(char[] c, int off, int len, int x, int y);

    private static int anchorX(int x, int w, int anchor) {
        if ((anchor & RIGHT) != 0) {
            return x - w;
        }
        if ((anchor & HCENTER) != 0) {
            return x - w / 2;
        }
        return x;
    }

    private static int anchorY(int y, int h, int anchor) {
        if ((anchor & BOTTOM) != 0) {
            return y - h;
        }
        if ((anchor & VCENTER) != 0) {
            return y - h / 2;
        }
        return y;
    }

    private static boolean validImageAnchor(int anchor) {
        if (anchor == 0) {
            return true;
        }
        int h = anchor & (LEFT | RIGHT | HCENTER);
        int v = anchor & (TOP | BOTTOM | VCENTER);
        return (anchor & ~(LEFT | RIGHT | HCENTER | TOP | BOTTOM | VCENTER)) == 0
                && (h == LEFT || h == RIGHT || h == HCENTER)
                && (v == TOP || v == BOTTOM || v == VCENTER);
    }

    public void drawImage(Image img, int x, int y, int anchor) {
        if (img == null) {
            throw new NullPointerException();
        }
        if (!validImageAnchor(anchor)) {
            throw new IllegalArgumentException();
        }
        drawRegion0(img, 0, 0, img.width, img.height, 0,
                anchorX(x, img.width, anchor), anchorY(y, img.height, anchor));
    }

    public void drawRegion(Image src, int x_src, int y_src, int width, int height,
                           int transform, int x_dest, int y_dest, int anchor) {
        if (src == null) {
            throw new NullPointerException();
        }
        if (src.pixels == target.pixels) {
            throw new IllegalArgumentException("source and destination are the same image");
        }
        if (transform < 0 || transform > 7 || !validImageAnchor(anchor)) {
            throw new IllegalArgumentException();
        }
        int dw = transform >= 4 ? height : width;
        int dh = transform >= 4 ? width : height;
        drawRegion0(src, x_src, y_src, width, height, transform,
                anchorX(x_dest, dw, anchor), anchorY(y_dest, dh, anchor));
    }

    private native void drawRegion0(Image src, int sx, int sy, int w, int h, int transform, int x, int y);

    public void copyArea(int x_src, int y_src, int width, int height, int x_dest, int y_dest, int anchor) {
        /* MIDP forbids this on the display, but the display is an offscreen
         * buffer here and some games scroll with it, so allow it. */
        if (!validImageAnchor(anchor)) {
            throw new IllegalArgumentException();
        }
        copyArea0(x_src, y_src, width, height,
                anchorX(x_dest, width, anchor), anchorY(y_dest, height, anchor));
    }

    private native void copyArea0(int sx, int sy, int w, int h, int dx, int dy);

    public void drawRGB(int[] rgbData, int offset, int scanlength, int x, int y,
                        int width, int height, boolean processAlpha) {
        drawRGB0(rgbData, offset, scanlength, x, y, width, height, processAlpha);
    }

    private native void drawRGB0(int[] rgb, int offset, int scan, int x, int y, int w, int h, boolean alpha);

    public int getDisplayColor(int color) {
        int r = (color >> 16) & 0xf8;
        int g = (color >> 8) & 0xf8;
        int b = color & 0xf8;
        return (r | (r >> 5)) << 16 | (g | (g >> 5)) << 8 | (b | (b >> 5));
    }
}
