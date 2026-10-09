package javax.microedition.lcdui.game;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

public class Sprite extends Layer {
    public static final int TRANS_NONE = 0;
    public static final int TRANS_ROT90 = 5;
    public static final int TRANS_ROT180 = 3;
    public static final int TRANS_ROT270 = 6;
    public static final int TRANS_MIRROR = 2;
    public static final int TRANS_MIRROR_ROT90 = 7;
    public static final int TRANS_MIRROR_ROT180 = 1;
    public static final int TRANS_MIRROR_ROT270 = 4;

    private Image image;
    private int frameWidth;
    private int frameHeight;
    private int framesPerRow;
    private int rawFrames;
    private int[] sequence;
    private boolean customSequence;
    private int sequenceIndex;
    private int refX;
    private int refY;
    private int transform;
    private int collX;
    private int collY;
    private int collW;
    private int collH;

    public Sprite(Image image) {
        super(0, 0);
        if (image == null) {
            throw new NullPointerException();
        }
        init(image, image.getWidth(), image.getHeight());
    }

    public Sprite(Image image, int frameWidth, int frameHeight) {
        super(0, 0);
        init(image, frameWidth, frameHeight);
    }

    public Sprite(Sprite s) {
        super(0, 0);
        if (s == null) {
            throw new NullPointerException();
        }
        image = s.image;
        frameWidth = s.frameWidth;
        frameHeight = s.frameHeight;
        framesPerRow = s.framesPerRow;
        rawFrames = s.rawFrames;
        sequence = new int[s.sequence.length];
        System.arraycopy(s.sequence, 0, sequence, 0, sequence.length);
        customSequence = s.customSequence;
        sequenceIndex = s.sequenceIndex;
        refX = s.refX;
        refY = s.refY;
        transform = s.transform;
        collX = s.collX;
        collY = s.collY;
        collW = s.collW;
        collH = s.collH;
        x = s.x;
        y = s.y;
        width = s.width;
        height = s.height;
        visible = s.visible;
    }

    private void init(Image img, int fw, int fh) {
        setFrames(img, fw, fh);
        sequence = defaultSequence(rawFrames);
        collX = 0;
        collY = 0;
        collW = fw;
        collH = fh;
        updateBounds();
    }

    private void setFrames(Image img, int fw, int fh) {
        if (img == null) {
            throw new NullPointerException();
        }
        int iw = img.getWidth();
        int ih = img.getHeight();
        if (fw < 1 || fh < 1 || iw % fw != 0 || ih % fh != 0) {
            throw new IllegalArgumentException();
        }
        image = img;
        frameWidth = fw;
        frameHeight = fh;
        framesPerRow = iw / fw;
        rawFrames = framesPerRow * (ih / fh);
    }

    private static int[] defaultSequence(int n) {
        int[] s = new int[n];
        for (int i = 0; i < n; i++) {
            s[i] = i;
        }
        return s;
    }

    private void updateBounds() {
        if (transform >= 4) {
            width = frameHeight;
            height = frameWidth;
        } else {
            width = frameWidth;
            height = frameHeight;
        }
    }

    /* Position of untransformed frame pixel (px, py) in the transformed frame. */
    static int transformX(int px, int py, int t, int w, int h) {
        switch (t) {
        case TRANS_MIRROR:
        case TRANS_ROT180:
            return w - 1 - px;
        case TRANS_MIRROR_ROT270:
        case TRANS_ROT270:
            return py;
        case TRANS_ROT90:
        case TRANS_MIRROR_ROT90:
            return h - 1 - py;
        default:
            return px;
        }
    }

    static int transformY(int px, int py, int t, int w, int h) {
        switch (t) {
        case TRANS_MIRROR_ROT180:
        case TRANS_ROT180:
            return h - 1 - py;
        case TRANS_MIRROR_ROT270:
        case TRANS_ROT90:
            return px;
        case TRANS_ROT270:
        case TRANS_MIRROR_ROT90:
            return w - 1 - px;
        default:
            return py;
        }
    }

    public void defineReferencePixel(int x, int y) {
        refX = x;
        refY = y;
    }

    public void setRefPixelPosition(int x, int y) {
        this.x = x - transformX(refX, refY, transform, frameWidth, frameHeight);
        this.y = y - transformY(refX, refY, transform, frameWidth, frameHeight);
    }

    public int getRefPixelX() {
        return x + transformX(refX, refY, transform, frameWidth, frameHeight);
    }

    public int getRefPixelY() {
        return y + transformY(refX, refY, transform, frameWidth, frameHeight);
    }

    public void setFrame(int sequenceIndex) {
        if (sequenceIndex < 0 || sequenceIndex >= sequence.length) {
            throw new IndexOutOfBoundsException();
        }
        this.sequenceIndex = sequenceIndex;
    }

    public final int getFrame() {
        return sequenceIndex;
    }

    public int getRawFrameCount() {
        return rawFrames;
    }

    public int getFrameSequenceLength() {
        return sequence.length;
    }

    public void nextFrame() {
        sequenceIndex = (sequenceIndex + 1) % sequence.length;
    }

    public void prevFrame() {
        sequenceIndex = (sequenceIndex + sequence.length - 1) % sequence.length;
    }

    public final void paint(Graphics g) {
        if (g == null) {
            throw new NullPointerException();
        }
        if (!visible) {
            return;
        }
        int f = sequence[sequenceIndex];
        g.drawRegion(image, (f % framesPerRow) * frameWidth, (f / framesPerRow) * frameHeight,
                frameWidth, frameHeight, transform, x, y, Graphics.TOP | Graphics.LEFT);
    }

    public void setFrameSequence(int[] sequence) {
        if (sequence == null) {
            this.sequence = defaultSequence(rawFrames);
            customSequence = false;
        } else {
            if (sequence.length < 1) {
                throw new IllegalArgumentException();
            }
            int[] s = new int[sequence.length];
            for (int i = 0; i < s.length; i++) {
                if (sequence[i] < 0 || sequence[i] >= rawFrames) {
                    throw new ArrayIndexOutOfBoundsException();
                }
                s[i] = sequence[i];
            }
            this.sequence = s;
            customSequence = true;
        }
        sequenceIndex = 0;
    }

    public void setImage(Image img, int frameWidth, int frameHeight) {
        int rx = getRefPixelX();
        int ry = getRefPixelY();
        boolean resized = frameWidth != this.frameWidth || frameHeight != this.frameHeight;
        int oldFrames = rawFrames;
        setFrames(img, frameWidth, frameHeight);
        if (rawFrames >= oldFrames) {
            if (!customSequence) {
                sequence = defaultSequence(rawFrames);
            }
        } else {
            sequence = defaultSequence(rawFrames);
            customSequence = false;
            sequenceIndex = 0;
        }
        if (resized) {
            collX = 0;
            collY = 0;
            collW = frameWidth;
            collH = frameHeight;
        }
        updateBounds();
        setRefPixelPosition(rx, ry);
    }

    public void defineCollisionRectangle(int x, int y, int width, int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException();
        }
        collX = x;
        collY = y;
        collW = width;
        collH = height;
    }

    public void setTransform(int transform) {
        if (transform < 0 || transform > 7) {
            throw new IllegalArgumentException();
        }
        int rx = getRefPixelX();
        int ry = getRefPixelY();
        this.transform = transform;
        updateBounds();
        setRefPixelPosition(rx, ry);
    }

    /* ---- Collision detection ---- */

    /* The transformed collision rectangle in painter coordinates as
     * {x1, y1, x2, y2} (exclusive), or null if it is empty. */
    private int[] collisionBounds() {
        if (collW <= 0 || collH <= 0) {
            return null;
        }
        int t = transform;
        int ax = transformX(collX, collY, t, frameWidth, frameHeight);
        int ay = transformY(collX, collY, t, frameWidth, frameHeight);
        int bx = transformX(collX + collW - 1, collY + collH - 1, t, frameWidth, frameHeight);
        int by = transformY(collX + collW - 1, collY + collH - 1, t, frameWidth, frameHeight);
        int[] r = new int[4];
        r[0] = x + Math.min(ax, bx);
        r[1] = y + Math.min(ay, by);
        r[2] = x + Math.max(ax, bx) + 1;
        r[3] = y + Math.max(ay, by) + 1;
        return r;
    }

    /* Intersects r with the rectangle (x, y, w, h); returns false if empty. */
    static boolean intersect(int[] r, int x, int y, int w, int h) {
        if (x > r[0]) r[0] = x;
        if (y > r[1]) r[1] = y;
        if (x + w < r[2]) r[2] = x + w;
        if (y + h < r[3]) r[3] = y + h;
        return r[0] < r[2] && r[1] < r[3];
    }

    /* ARGB of the current transformed frame over the painter-coordinate
     * area r, which must lie inside the sprite's bounds. */
    private int[] pixels(int[] r) {
        int f = sequence[sequenceIndex];
        int fw = frameWidth;
        int fh = frameHeight;
        int[] frame = new int[fw * fh];
        image.getRGB(frame, 0, fw, (f % framesPerRow) * fw, (f / framesPerRow) * fh, fw, fh);
        int w = r[2] - r[0];
        int h = r[3] - r[1];
        int[] out = new int[w * h];
        int i = 0;
        for (int ly = r[1] - y; ly < r[3] - y; ly++) {
            for (int lx = r[0] - x; lx < r[2] - x; lx++) {
                int sx, sy;
                switch (transform) {
                case TRANS_MIRROR_ROT180: sx = lx; sy = fh - 1 - ly; break;
                case TRANS_MIRROR: sx = fw - 1 - lx; sy = ly; break;
                case TRANS_ROT180: sx = fw - 1 - lx; sy = fh - 1 - ly; break;
                case TRANS_MIRROR_ROT270: sx = ly; sy = lx; break;
                case TRANS_ROT90: sx = ly; sy = fh - 1 - lx; break;
                case TRANS_ROT270: sx = fw - 1 - ly; sy = lx; break;
                case TRANS_MIRROR_ROT90: sx = fw - 1 - ly; sy = fh - 1 - lx; break;
                default: sx = lx; sy = ly; break;
                }
                out[i++] = frame[sy * fw + sx];
            }
        }
        return out;
    }

    private static boolean overlap(int[] a, int[] b) {
        for (int i = 0; i < a.length; i++) {
            if ((a[i] & 0xff000000) != 0 && (b[i] & 0xff000000) != 0) {
                return true;
            }
        }
        return false;
    }

    public final boolean collidesWith(Sprite s, boolean pixelLevel) {
        if (!visible || !s.visible) {
            return false;
        }
        int[] r = collisionBounds();
        int[] o = s.collisionBounds();
        if (r == null || o == null || !intersect(r, o[0], o[1], o[2] - o[0], o[3] - o[1])) {
            return false;
        }
        if (!pixelLevel) {
            return true;
        }
        if (!intersect(r, x, y, width, height) || !intersect(r, s.x, s.y, s.width, s.height)) {
            return false;
        }
        return overlap(pixels(r), s.pixels(r));
    }

    public final boolean collidesWith(TiledLayer t, boolean pixelLevel) {
        if (!visible || !t.visible) {
            return false;
        }
        int[] r = collisionBounds();
        if (r == null || !intersect(r, t.x, t.y, t.width, t.height)) {
            return false;
        }
        if (pixelLevel && !intersect(r, x, y, width, height)) {
            return false;
        }
        int cw = t.getCellWidth();
        int ch = t.getCellHeight();
        int c1 = (r[0] - t.x) / cw;
        int c2 = (r[2] - 1 - t.x) / cw;
        int r1 = (r[1] - t.y) / ch;
        int r2 = (r[3] - 1 - t.y) / ch;
        for (int row = r1; row <= r2; row++) {
            for (int col = c1; col <= c2; col++) {
                int tile = t.staticTileAt(col, row);
                if (tile == 0) {
                    continue;
                }
                if (!pixelLevel) {
                    return true;
                }
                int cx = t.x + col * cw;
                int cy = t.y + row * ch;
                int[] cr = {r[0], r[1], r[2], r[3]};
                if (!intersect(cr, cx, cy, cw, ch)) {
                    continue;
                }
                int w = cr[2] - cr[0];
                int h = cr[3] - cr[1];
                int[] tp = new int[w * h];
                t.tileImage().getRGB(tp, 0, w, t.tileSourceX(tile) + cr[0] - cx,
                        t.tileSourceY(tile) + cr[1] - cy, w, h);
                if (overlap(pixels(cr), tp)) {
                    return true;
                }
            }
        }
        return false;
    }

    public final boolean collidesWith(Image image, int x, int y, boolean pixelLevel) {
        if (image == null) {
            throw new NullPointerException();
        }
        if (!visible) {
            return false;
        }
        int[] r = collisionBounds();
        if (r == null || !intersect(r, x, y, image.getWidth(), image.getHeight())) {
            return false;
        }
        if (!pixelLevel) {
            return true;
        }
        if (!intersect(r, this.x, this.y, width, height)) {
            return false;
        }
        int w = r[2] - r[0];
        int h = r[3] - r[1];
        int[] ip = new int[w * h];
        image.getRGB(ip, 0, w, r[0] - x, r[1] - y, w, h);
        return overlap(pixels(r), ip);
    }
}
