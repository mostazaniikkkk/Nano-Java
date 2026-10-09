package javax.microedition.lcdui;

public class Gauge extends Item {
    public static final int INDEFINITE = -1;
    public static final int CONTINUOUS_IDLE = 0;
    public static final int INCREMENTAL_IDLE = 1;
    public static final int CONTINUOUS_RUNNING = 2;
    public static final int INCREMENTAL_UPDATING = 3;

    private static final int BAR_H = 12;

    final boolean interactive;
    private int maxValue;
    private int value;
    private int phase;
    private int barWidth = 1;

    public Gauge(String label, boolean interactive, int maxValue, int initialValue) {
        super(label);
        this.interactive = interactive;
        setMaxValue(maxValue);
        setValue(initialValue);
    }

    public void setValue(int v) {
        if (maxValue == INDEFINITE) {
            if (v < CONTINUOUS_IDLE || v > INCREMENTAL_UPDATING) {
                throw new IllegalArgumentException();
            }
            if (v == INCREMENTAL_UPDATING) {
                phase++;
            }
        } else if (v < 0) {
            v = 0;
        } else if (v > maxValue) {
            v = maxValue;
        }
        value = v;
        repaint0();
    }

    public int getValue() {
        return value;
    }

    public void setMaxValue(int max) {
        if (max <= 0 && !(max == INDEFINITE && !interactive)) {
            throw new IllegalArgumentException();
        }
        int old = maxValue;
        maxValue = max;
        if (max == INDEFINITE) {
            if (old != INDEFINITE) {
                value = INCREMENTAL_IDLE;
            }
        } else if (old == INDEFINITE) {
            value = 0;
        } else if (value > max) {
            value = max;
        }
        repaint0();
    }

    public int getMaxValue() {
        return maxValue;
    }

    public boolean isInteractive() {
        return interactive;
    }

    boolean isFocusable() {
        return interactive || super.isFocusable();
    }

    int measureBody(int w) {
        return BAR_H + 2;
    }

    void paintBody(Graphics g, int w, int h, boolean focused) {
        int x = 0;
        int bw = w;
        if (interactive) {
            /* Arrows hint that left/right change the value. */
            g.setColor(focused ? Theme.HIGHLIGHT : Theme.BORDER);
            int cy = 1 + BAR_H / 2;
            g.fillTriangle(0, cy, 6, cy - 5, 6, cy + 5);
            g.fillTriangle(w - 1, cy, w - 7, cy - 5, w - 7, cy + 5);
            x = 10;
            bw = w - 20;
        }
        barWidth = Math.max(1, bw);
        g.setColor(Theme.FIELD_BG);
        g.fillRect(x, 1, bw, BAR_H);
        g.setColor(Theme.HIGHLIGHT);
        if (maxValue == INDEFINITE) {
            if (value == CONTINUOUS_RUNNING || value == INCREMENTAL_UPDATING) {
                int p = value == CONTINUOUS_RUNNING
                        ? (int) (System.currentTimeMillis() / 100) : phase;
                int seg = Math.max(8, bw / 5);
                int span = bw - seg;
                int pos = span <= 0 ? 0 : (p * 6) % (2 * span);
                if (pos > span) {
                    pos = 2 * span - pos;
                }
                g.fillRect(x + pos, 1, seg, BAR_H);
            }
        } else if (maxValue > 0) {
            g.fillRect(x, 1, (int) ((long) bw * value / maxValue), BAR_H);
        }
        g.setColor(focused ? Theme.HIGHLIGHT : Theme.BORDER);
        g.drawRect(x, 1, bw - 1, BAR_H - 1);
    }

    private void userSet(int v) {
        int old = value;
        setValue(v);
        if (value != old) {
            notifyStateChanged();
        }
    }

    boolean itemKey(int type, int code) {
        if (!interactive || type == nanojava.Events.KEY_UP) {
            return false;
        }
        int action = Canvas.gameAction(code);
        if (action == Canvas.LEFT) {
            userSet(value - 1);
            return true;
        }
        if (action == Canvas.RIGHT) {
            userSet(value + 1);
            return true;
        }
        return false;
    }

    boolean itemPointer(int type, int x, int y) {
        if (!interactive || type == nanojava.Events.POINTER_UP) {
            return interactive;
        }
        userSet((int) (((long) (x - 10) * maxValue + barWidth / 2) / barWidth));
        return true;
    }
}
