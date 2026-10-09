package com.nokia.mid.ui;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.LcduiBridge;
import javax.microedition.lcdui.game.Sprite;

/*
 * DirectGraphics on top of Graphics: pixel data is converted to ARGB and
 * drawn with drawRGB, manipulations become Sprite transforms. Primitive
 * colors ignore alpha except that fully transparent colors draw nothing.
 */
class DirectGraphicsImpl implements DirectGraphics {
    private final Graphics g;
    private int alpha = 0xff;

    DirectGraphicsImpl(Graphics g) {
        this.g = g;
    }

    public void setARGBColor(int argbColor) {
        alpha = argbColor >>> 24;
        g.setColor(argbColor & 0xffffff);
    }

    public int getAlphaComponent() {
        return alpha;
    }

    public int getNativePixelFormat() {
        return TYPE_USHORT_4444_ARGB;
    }

    /* Nokia rotations are counter-clockwise and applied before the flips. */
    static int transform(int manipulation) {
        int rotation = manipulation & ~(FLIP_HORIZONTAL | FLIP_VERTICAL);
        if (rotation < 0 || rotation % 90 != 0) {
            throw new IllegalArgumentException("invalid manipulation");
        }
        rotation %= 360;
        boolean h = (manipulation & FLIP_HORIZONTAL) != 0;
        boolean v = (manipulation & FLIP_VERTICAL) != 0;
        if (h && v) {
            switch (rotation) {
            case 0: return Sprite.TRANS_ROT180;
            case ROTATE_90: return Sprite.TRANS_ROT90;
            case ROTATE_180: return Sprite.TRANS_NONE;
            default: return Sprite.TRANS_ROT270;
            }
        }
        if (h) {
            switch (rotation) {
            case 0: return Sprite.TRANS_MIRROR;
            case ROTATE_90: return Sprite.TRANS_MIRROR_ROT90;
            case ROTATE_180: return Sprite.TRANS_MIRROR_ROT180;
            default: return Sprite.TRANS_MIRROR_ROT270;
            }
        }
        if (v) {
            switch (rotation) {
            case 0: return Sprite.TRANS_MIRROR_ROT180;
            case ROTATE_90: return Sprite.TRANS_MIRROR_ROT270;
            case ROTATE_180: return Sprite.TRANS_MIRROR;
            default: return Sprite.TRANS_MIRROR_ROT90;
            }
        }
        switch (rotation) {
        case 0: return Sprite.TRANS_NONE;
        case ROTATE_90: return Sprite.TRANS_ROT270;
        case ROTATE_180: return Sprite.TRANS_ROT180;
        default: return Sprite.TRANS_ROT90;
        }
    }

    public void drawImage(Image img, int x, int y, int anchor, int manipulation) {
        if (img == null) {
            throw new NullPointerException();
        }
        int t = transform(manipulation);
        if (t == Sprite.TRANS_NONE) {
            g.drawImage(img, x, y, anchor);
        } else {
            g.drawRegion(img, 0, 0, img.getWidth(), img.getHeight(), t, x, y, anchor);
        }
    }

    /* ---- Primitives ---- */

    private int savedColor;

    private boolean begin(int argb) {
        if ((argb >>> 24) == 0) {
            return false;
        }
        savedColor = g.getColor();
        g.setColor(argb & 0xffffff);
        return true;
    }

    private void end() {
        g.setColor(savedColor);
    }

    public void drawTriangle(int x1, int y1, int x2, int y2, int x3, int y3, int argbColor) {
        if (begin(argbColor)) {
            g.drawLine(x1, y1, x2, y2);
            g.drawLine(x2, y2, x3, y3);
            g.drawLine(x3, y3, x1, y1);
            end();
        }
    }

    public void fillTriangle(int x1, int y1, int x2, int y2, int x3, int y3, int argbColor) {
        if (begin(argbColor)) {
            g.fillTriangle(x1, y1, x2, y2, x3, y3);
            end();
        }
    }

    private void outline(int[] xs, int xo, int[] ys, int yo, int n) {
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            g.drawLine(xs[xo + i], ys[yo + i], xs[xo + j], ys[yo + j]);
        }
    }

    private static void checkPolygon(int[] xs, int xo, int[] ys, int yo, int n) {
        if (xs == null || ys == null) {
            throw new NullPointerException();
        }
        if (n < 0 || xo < 0 || yo < 0 || xo > xs.length - n || yo > ys.length - n) {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    public void drawPolygon(int[] xPoints, int xOffset, int[] yPoints, int yOffset, int nPoints,
                            int argbColor) {
        checkPolygon(xPoints, xOffset, yPoints, yOffset, nPoints);
        if (nPoints > 0 && begin(argbColor)) {
            outline(xPoints, xOffset, yPoints, yOffset, nPoints);
            end();
        }
    }

    /* Even-odd scanline fill, plus the outline so edges are included. */
    public void fillPolygon(int[] xPoints, int xOffset, int[] yPoints, int yOffset, int nPoints,
                            int argbColor) {
        checkPolygon(xPoints, xOffset, yPoints, yOffset, nPoints);
        if (nPoints == 0 || !begin(argbColor)) {
            return;
        }
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (int i = 0; i < nPoints; i++) {
            minY = Math.min(minY, yPoints[yOffset + i]);
            maxY = Math.max(maxY, yPoints[yOffset + i]);
        }
        minY = Math.max(minY, g.getClipY());
        maxY = Math.min(maxY, g.getClipY() + g.getClipHeight() - 1);
        int[] xs = new int[nPoints];
        for (int y = minY; y <= maxY; y++) {
            int k = 0;
            for (int i = 0; i < nPoints; i++) {
                int j = i == 0 ? nPoints - 1 : i - 1;
                int x1 = xPoints[xOffset + i];
                int y1 = yPoints[yOffset + i];
                int x2 = xPoints[xOffset + j];
                int y2 = yPoints[yOffset + j];
                if (y1 == y2 || y < Math.min(y1, y2) || y >= Math.max(y1, y2)) {
                    continue;
                }
                int x = x1 + (y - y1) * (x2 - x1) / (y2 - y1);
                int m = k++;
                while (m > 0 && xs[m - 1] > x) {
                    xs[m] = xs[m - 1];
                    m--;
                }
                xs[m] = x;
            }
            for (int i = 0; i + 1 < k; i += 2) {
                g.fillRect(xs[i], y, xs[i + 1] - xs[i] + 1, 1);
            }
        }
        outline(xPoints, xOffset, yPoints, yOffset, nPoints);
        end();
    }

    /* ---- Pixel blocks ---- */

    private static void checkBlock(int length, int offset, int scan, int width, int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException();
        }
        if (width == 0 || height == 0) {
            return;
        }
        long first = offset;
        long last = (long) offset + (long) (height - 1) * scan + width - 1;
        if (Math.min(first, (long) offset + (long) (height - 1) * scan) < 0 || last >= length) {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    /* Draws a width x height ARGB block at (x, y) with a manipulation. */
    private void blit(int[] argb, int width, int height, int x, int y, int manipulation,
                      boolean processAlpha) {
        int t = transform(manipulation);
        if (width == 0 || height == 0) {
            return;
        }
        int dw = width;
        if (t != Sprite.TRANS_NONE) {
            int dh = height;
            if (t >= 4) {
                dw = height;
                dh = width;
            }
            int[] out = new int[argb.length];
            int i = 0;
            for (int sy = 0; sy < height; sy++) {
                for (int sx = 0; sx < width; sx++) {
                    int dx, dy;
                    switch (t) {
                    case Sprite.TRANS_MIRROR_ROT180: dx = sx; dy = height - 1 - sy; break;
                    case Sprite.TRANS_MIRROR: dx = width - 1 - sx; dy = sy; break;
                    case Sprite.TRANS_ROT180: dx = width - 1 - sx; dy = height - 1 - sy; break;
                    case Sprite.TRANS_MIRROR_ROT270: dx = sy; dy = sx; break;
                    case Sprite.TRANS_ROT90: dx = height - 1 - sy; dy = sx; break;
                    case Sprite.TRANS_ROT270: dx = sy; dy = width - 1 - sx; break;
                    default: dx = height - 1 - sy; dy = width - 1 - sx; break;
                    }
                    out[dy * dw + dx] = argb[i++];
                }
            }
            argb = out;
            height = dh;
        }
        g.drawRGB(argb, 0, dw, x, y, dw, height, processAlpha);
    }

    public void drawPixels(int[] pixels, boolean transparency, int offset, int scanlength, int x,
                           int y, int width, int height, int manipulation, int format) {
        if (pixels == null) {
            throw new NullPointerException();
        }
        if (format != TYPE_INT_888_RGB && format != TYPE_INT_8888_ARGB) {
            throw new IllegalArgumentException("unsupported format " + format);
        }
        checkBlock(pixels.length, offset, scanlength, width, height);
        boolean alphaUsed = transparency && format == TYPE_INT_8888_ARGB;
        int[] argb = new int[width * height];
        int i = 0;
        for (int row = 0; row < height; row++) {
            int p = offset + row * scanlength;
            for (int col = 0; col < width; col++) {
                int v = pixels[p + col];
                argb[i++] = alphaUsed ? v : v | 0xff000000;
            }
        }
        blit(argb, width, height, x, y, manipulation, alphaUsed);
    }

    private static int expand4(int v) {
        return (v & 0xf) * 17;
    }

    private static int expand5(int v) {
        v &= 31;
        return (v << 3) | (v >> 2);
    }

    static int shortToARGB(int p, int format) {
        switch (format) {
        case TYPE_USHORT_4444_ARGB:
            return expand4(p >> 12) << 24 | expand4(p >> 8) << 16 | expand4(p >> 4) << 8
                    | expand4(p);
        case TYPE_USHORT_444_RGB:
            return 0xff000000 | expand4(p >> 8) << 16 | expand4(p >> 4) << 8 | expand4(p);
        case TYPE_USHORT_555_RGB:
            return 0xff000000 | expand5(p >> 10) << 16 | expand5(p >> 5) << 8 | expand5(p);
        case TYPE_USHORT_1555_ARGB:
            return ((p & 0x8000) != 0 ? 0xff000000 : 0) | expand5(p >> 10) << 16
                    | expand5(p >> 5) << 8 | expand5(p);
        case TYPE_USHORT_565_RGB: {
            int g6 = (p >> 5) & 63;
            return 0xff000000 | expand5(p >> 11) << 16 | ((g6 << 2) | (g6 >> 4)) << 8 | expand5(p);
        }
        default:
            throw new IllegalArgumentException("unsupported format " + format);
        }
    }

    static int argbToShort(int c, int format) {
        int a = c >>> 24;
        int r = (c >> 16) & 0xff;
        int gr = (c >> 8) & 0xff;
        int b = c & 0xff;
        switch (format) {
        case TYPE_USHORT_4444_ARGB:
            return (a >> 4) << 12 | (r >> 4) << 8 | (gr >> 4) << 4 | (b >> 4);
        case TYPE_USHORT_444_RGB:
            return (r >> 4) << 8 | (gr >> 4) << 4 | (b >> 4);
        case TYPE_USHORT_555_RGB:
            return (r >> 3) << 10 | (gr >> 3) << 5 | (b >> 3);
        case TYPE_USHORT_1555_ARGB:
            return (a >= 0x80 ? 0x8000 : 0) | (r >> 3) << 10 | (gr >> 3) << 5 | (b >> 3);
        case TYPE_USHORT_565_RGB:
            return (r >> 3) << 11 | (gr >> 2) << 5 | (b >> 3);
        default:
            throw new IllegalArgumentException("unsupported format " + format);
        }
    }

    public void drawPixels(short[] pixels, boolean transparency, int offset, int scanlength,
                           int x, int y, int width, int height, int manipulation, int format) {
        if (pixels == null) {
            throw new NullPointerException();
        }
        argbToShort(0, format);
        checkBlock(pixels.length, offset, scanlength, width, height);
        boolean alphaUsed = transparency
                && (format == TYPE_USHORT_4444_ARGB || format == TYPE_USHORT_1555_ARGB);
        int[] argb = new int[width * height];
        int i = 0;
        for (int row = 0; row < height; row++) {
            int p = offset + row * scanlength;
            for (int col = 0; col < width; col++) {
                int v = shortToARGB(pixels[p + col], format);
                argb[i++] = alphaUsed ? v : v | 0xff000000;
            }
        }
        blit(argb, width, height, x, y, manipulation, alphaUsed);
    }

    /* Bits per pixel of a byte format. */
    private static int depth(int format) {
        switch (format) {
        case TYPE_BYTE_1_GRAY:
        case TYPE_BYTE_1_GRAY_VERTICAL:
            return 1;
        case TYPE_BYTE_2_GRAY:
            return 2;
        case TYPE_BYTE_4_GRAY:
            return 4;
        case TYPE_BYTE_8_GRAY:
        case TYPE_BYTE_332_RGB:
            return 8;
        default:
            throw new IllegalArgumentException("unsupported format " + format);
        }
    }

    /* Byte index and bit shift of pixel (col, row) in packed byte data. */
    private static int bitIndex(int format, int depth, int offset, int scan, int col, int row) {
        if (format == TYPE_BYTE_1_GRAY_VERTICAL) {
            return ((offset + (row >> 3) * scan + col) << 3) | (row & 7);
        }
        int bit = (offset + row * scan + col) * depth;
        return ((bit >> 3) << 3) | (8 - depth - (bit & 7));
    }

    private static int readPacked(byte[] data, int index, int depth) {
        return ((data[index >> 3] & 0xff) >> (index & 7)) & ((1 << depth) - 1);
    }

    private static void writePacked(byte[] data, int index, int depth, int value) {
        int mask = ((1 << depth) - 1) << (index & 7);
        int b = data[index >> 3] & ~mask;
        data[index >> 3] = (byte) (b | ((value << (index & 7)) & mask));
    }

    public void drawPixels(byte[] pixels, byte[] transparencyMask, int offset, int scanlength,
                           int x, int y, int width, int height, int manipulation, int format) {
        if (pixels == null) {
            throw new NullPointerException();
        }
        int depth = depth(format);
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException();
        }
        int[] argb = new int[width * height];
        int max = (1 << depth) - 1;
        int i = 0;
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                int index = bitIndex(format, depth, offset, scanlength, col, row);
                int v = readPacked(pixels, index, depth);
                int c;
                if (format == TYPE_BYTE_332_RGB) {
                    c = ((v >> 5) * 255 / 7) << 16 | (((v >> 2) & 7) * 255 / 7) << 8
                            | (v & 3) * 85;
                } else if (depth == 1) {
                    c = v != 0 ? 0 : 0xffffff;
                } else {
                    int l = v * 255 / max;
                    c = l << 16 | l << 8 | l;
                }
                boolean opaque = transparencyMask == null
                        || readPacked(transparencyMask, index, depth) != 0;
                argb[i++] = opaque ? c | 0xff000000 : c;
            }
        }
        blit(argb, width, height, x, y, manipulation, transparencyMask != null);
    }

    /* ---- Reading back ---- */

    private int[] read(int x, int y, int width, int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException();
        }
        int[] argb = new int[width * height];
        if (width > 0 && height > 0) {
            Image target = LcduiBridge.targetOf(g);
            target.getRGB(argb, 0, width, x + g.getTranslateX(), y + g.getTranslateY(),
                    width, height);
        }
        return argb;
    }

    public void getPixels(int[] pixels, int offset, int scanlength, int x, int y, int width,
                          int height, int format) {
        if (pixels == null) {
            throw new NullPointerException();
        }
        if (format != TYPE_INT_888_RGB && format != TYPE_INT_8888_ARGB) {
            throw new IllegalArgumentException("unsupported format " + format);
        }
        checkBlock(pixels.length, offset, scanlength, width, height);
        int[] argb = read(x, y, width, height);
        int i = 0;
        for (int row = 0; row < height; row++) {
            int p = offset + row * scanlength;
            for (int col = 0; col < width; col++) {
                int c = argb[i++];
                pixels[p + col] = format == TYPE_INT_888_RGB ? c & 0xffffff : c;
            }
        }
    }

    public void getPixels(short[] pixels, int offset, int scanlength, int x, int y, int width,
                          int height, int format) {
        if (pixels == null) {
            throw new NullPointerException();
        }
        argbToShort(0, format);
        checkBlock(pixels.length, offset, scanlength, width, height);
        int[] argb = read(x, y, width, height);
        int i = 0;
        for (int row = 0; row < height; row++) {
            int p = offset + row * scanlength;
            for (int col = 0; col < width; col++) {
                pixels[p + col] = (short) argbToShort(argb[i++], format);
            }
        }
    }

    public void getPixels(byte[] pixels, byte[] transparencyMask, int offset, int scanlength,
                          int x, int y, int width, int height, int format) {
        if (pixels == null) {
            throw new NullPointerException();
        }
        int depth = depth(format);
        int max = (1 << depth) - 1;
        int[] argb = read(x, y, width, height);
        int i = 0;
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                int c = argb[i++];
                int r = (c >> 16) & 0xff;
                int gr = (c >> 8) & 0xff;
                int b = c & 0xff;
                int v;
                if (format == TYPE_BYTE_332_RGB) {
                    v = (r >> 5) << 5 | (gr >> 5) << 2 | (b >> 6);
                } else {
                    int l = (r * 30 + gr * 59 + b * 11) / 100;
                    v = depth == 1 ? (l < 128 ? 1 : 0) : l * max / 255;
                }
                int index = bitIndex(format, depth, offset, scanlength, col, row);
                writePacked(pixels, index, depth, v);
                if (transparencyMask != null) {
                    writePacked(transparencyMask, index, depth, (c >>> 24) != 0 ? max : 0);
                }
            }
        }
    }
}
