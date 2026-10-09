package javax.microedition.lcdui.game;

import java.util.Vector;
import javax.microedition.lcdui.Graphics;

/* Layer 0 is the one closest to the user; layers are painted last to first. */
public class LayerManager {
    private final Vector layers = new Vector();
    private int viewX;
    private int viewY;
    private int viewWidth = Integer.MAX_VALUE;
    private int viewHeight = Integer.MAX_VALUE;

    public LayerManager() {
    }

    public void append(Layer l) {
        if (l == null) {
            throw new NullPointerException();
        }
        layers.removeElement(l);
        layers.addElement(l);
    }

    public void insert(Layer l, int index) {
        if (l == null) {
            throw new NullPointerException();
        }
        int max = layers.contains(l) ? layers.size() - 1 : layers.size();
        if (index < 0 || index > max) {
            throw new IndexOutOfBoundsException();
        }
        layers.removeElement(l);
        layers.insertElementAt(l, index);
    }

    public Layer getLayerAt(int index) {
        if (index < 0 || index >= layers.size()) {
            throw new IndexOutOfBoundsException();
        }
        return (Layer) layers.elementAt(index);
    }

    public int getSize() {
        return layers.size();
    }

    public void remove(Layer l) {
        if (l == null) {
            throw new NullPointerException();
        }
        layers.removeElement(l);
    }

    public void setViewWindow(int x, int y, int width, int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException();
        }
        viewX = x;
        viewY = y;
        viewWidth = width;
        viewHeight = height;
    }

    /* Keeps x + size from overflowing in Graphics clip arithmetic. */
    private static int limit(int size) {
        return size > 0x3fffffff ? 0x3fffffff : size;
    }

    public void paint(Graphics g, int x, int y) {
        if (g == null) {
            throw new NullPointerException();
        }
        int clipX = g.getClipX();
        int clipY = g.getClipY();
        int clipW = g.getClipWidth();
        int clipH = g.getClipHeight();
        g.clipRect(x, y, limit(viewWidth), limit(viewHeight));
        int dx = x - viewX;
        int dy = y - viewY;
        g.translate(dx, dy);
        try {
            for (int i = layers.size() - 1; i >= 0; i--) {
                Layer l = (Layer) layers.elementAt(i);
                if (l.visible) {
                    l.paint(g);
                }
            }
        } finally {
            g.translate(-dx, -dy);
            g.setClip(clipX, clipY, clipW, clipH);
        }
    }
}
