package javax.microedition.lcdui;

/*
 * On-screen keyboard used by TextBox and the TextField editor: a text
 * area on top and a grid of keys below. The grid is navigated with the
 * direction keys and fire types the selected key; pointer taps on a key
 * type it too. Printable key codes (>= 32) are typed directly.
 */
final class Keyboard {
    /* Results of key() and pointer(). */
    static final int NONE = 0;
    static final int CHANGED = 1;
    static final int DONE = 2;

    /* Special key codes; printable keys use their character. */
    private static final int SHIFT = -1;
    private static final int DEL = -3;
    private static final int OK = -4;

    private static final int LOWER = 0;
    private static final int UPPER = 1;
    private static final int SYMBOLS = 2;

    private static final String[][] PAGES = {
        {"1234567890", "qwertyuiop", "asdfghjkl@", "zxcvbnm,.?"},
        {"1234567890", "QWERTYUIOP", "ASDFGHJKL@", "ZXCVBNM,.?"},
        {"1234567890", "!@#$%&*()_", "-+=/\\:;'\"~", "<>[]{}|^,."},
    };
    private static final String[] SHIFT_LABELS = {"ABC", "#+=", "abc"};

    private final TextBuffer buf;
    private final boolean showDone;
    private final TextWrap wrap = new TextWrap();

    private int builtFor = -1;
    private int mode;
    private int[][] codes;
    private int[][] units;
    private String[][] labels;
    private int totalUnits;
    private int row, col;

    /* Grid geometry from the last paint, in content coordinates. */
    private int gridTop = Integer.MAX_VALUE;
    private int cellH = 1;
    private int gridW = 1;

    Keyboard(TextBuffer buf, boolean showDone) {
        this.buf = buf;
        this.showDone = showDone;
    }

    private static int[] chars(String s) {
        int[] r = new int[s.length()];
        for (int i = 0; i < r.length; i++) {
            r[i] = s.charAt(i);
        }
        return r;
    }

    private void build() {
        int kind = buf.constraints & TextField.CONSTRAINT_MASK;
        builtFor = kind * 4 + mode;
        if (kind == TextField.NUMERIC || kind == TextField.DECIMAL || kind == TextField.PHONENUMBER) {
            int[] last;
            int[] bottom;
            if (kind == TextField.NUMERIC) {
                last = new int[] {'-', '0', DEL};
                bottom = showDone ? new int[] {OK} : null;
            } else if (kind == TextField.DECIMAL) {
                last = new int[] {'-', '0', '.'};
                bottom = showDone ? new int[] {DEL, OK} : new int[] {DEL};
            } else {
                last = new int[] {'*', '0', '#'};
                bottom = showDone ? new int[] {'+', DEL, OK} : new int[] {'+', DEL};
            }
            int n = bottom == null ? 4 : 5;
            codes = new int[n][];
            codes[0] = chars("123");
            codes[1] = chars("456");
            codes[2] = chars("789");
            codes[3] = last;
            if (bottom != null) {
                codes[4] = bottom;
            }
            totalUnits = 6;
            units = new int[n][];
            for (int r = 0; r < n; r++) {
                units[r] = new int[codes[r].length];
                for (int i = 0; i < units[r].length; i++) {
                    units[r][i] = 6 / codes[r].length;
                }
            }
        } else {
            String[] page = PAGES[mode];
            codes = new int[5][];
            units = new int[5][];
            for (int r = 0; r < 4; r++) {
                codes[r] = chars(page[r]);
                units[r] = new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1};
            }
            if (showDone) {
                codes[4] = new int[] {SHIFT, ' ', DEL, OK};
                units[4] = new int[] {2, 4, 2, 2};
            } else {
                codes[4] = new int[] {SHIFT, ' ', DEL};
                units[4] = new int[] {2, 6, 2};
            }
            totalUnits = 10;
        }
        labels = new String[codes.length][];
        for (int r = 0; r < codes.length; r++) {
            labels[r] = new String[codes[r].length];
            for (int i = 0; i < codes[r].length; i++) {
                int c = codes[r][i];
                labels[r][i] = c == DEL ? "Del" : c == OK ? "OK" : c == ' ' ? "Space"
                        : c > 0 ? String.valueOf((char) c) : null;
            }
        }
        row = Math.min(row, codes.length - 1);
        col = Math.min(col, codes[row].length - 1);
    }

    private void ensureBuilt() {
        int kind = buf.constraints & TextField.CONSTRAINT_MASK;
        if (builtFor != kind * 4 + mode) {
            build();
        }
    }

    private int unitStart(int r, int c) {
        int u = 0;
        for (int i = 0; i < c; i++) {
            u += units[r][i];
        }
        return u;
    }

    private int cellAtUnit(int r, int u) {
        int start = 0;
        for (int i = 0; i < units[r].length; i++) {
            start += units[r][i];
            if (u < start) {
                return i;
            }
        }
        return units[r].length - 1;
    }

    private void moveVertical(int d) {
        /* Keep the column under the center of the current key. */
        int center2 = unitStart(row, col) * 2 + units[row][col];
        row = (row + d + codes.length) % codes.length;
        col = cellAtUnit(row, center2 / 2);
    }

    int key(int type, int code) {
        if (type == nanojava.Events.KEY_UP || !buf.editable()) {
            return NONE;
        }
        ensureBuilt();
        switch (code) {
        case Canvas.KEY_UP:
            moveVertical(-1);
            return CHANGED;
        case Canvas.KEY_DOWN:
            moveVertical(1);
            return CHANGED;
        case Canvas.KEY_LEFT:
            col = (col + codes[row].length - 1) % codes[row].length;
            return CHANGED;
        case Canvas.KEY_RIGHT:
            col = (col + 1) % codes[row].length;
            return CHANGED;
        case Canvas.KEY_FIRE:
            int k = codes[row][col];
            if (type == Display.KEY_REPEAT && k != DEL) {
                return NONE;
            }
            return press(k);
        case 8:
            return buf.backspace() ? CHANGED : NONE;
        default:
            break;
        }
        if (code == '\n' || (code >= 32 && code <= 0xFFFF)) {
            return buf.typeChar((char) code) ? CHANGED : NONE;
        }
        return NONE;
    }

    private int press(int k) {
        if (k == SHIFT) {
            mode = (mode + 1) % 3;
            build();
        } else if (k == DEL) {
            buf.backspace();
        } else if (k == OK) {
            return DONE;
        } else {
            buf.typeChar((char) k);
        }
        return CHANGED;
    }

    /* Pointer event in content coordinates: a press selects a key, the
     * release on the same key types it. */
    int pointer(int type, int x, int y) {
        if (!buf.editable() || codes == null || y < gridTop) {
            return NONE;
        }
        int r = (y - gridTop) / cellH;
        if (r < 0 || r >= codes.length) {
            return NONE;
        }
        int c = cellAtUnit(r, Math.max(0, x) * totalUnits / gridW);
        if (type == nanojava.Events.POINTER_UP) {
            if (r == row && c == col) {
                return press(codes[r][c]);
            }
            return NONE;
        }
        row = r;
        col = c;
        return CHANGED;
    }

    /* Paints the text area and, when editable, the key grid below it. */
    void paint(Graphics g, int w, int h) {
        if (!buf.editable()) {
            paintText(g, 0, 0, w, h, false);
            gridTop = Integer.MAX_VALUE;
            return;
        }
        ensureBuilt();
        Font f = Theme.FONT;
        int fh = f.getHeight();
        int rows = codes.length;
        int ch = fh + 8;
        int textMin = fh * 2 + 10;
        if (rows * ch > h - textMin) {
            ch = Math.max(fh + 2, (h - textMin) / rows);
        }
        int gridH = rows * ch;
        gridTop = h - gridH;
        cellH = ch;
        gridW = w;
        paintText(g, 0, 0, w, gridTop, true);

        g.setColor(Theme.KEY_BG);
        g.fillRect(0, gridTop, w, gridH);
        g.setFont(f);
        for (int r = 0; r < rows; r++) {
            int y = gridTop + r * ch;
            int u = 0;
            for (int i = 0; i < codes[r].length; i++) {
                int x1 = u * w / totalUnits;
                u += units[r][i];
                int x2 = u * w / totalUnits;
                int code = codes[r][i];
                boolean sel = r == row && i == col;
                g.setColor(sel ? Theme.HIGHLIGHT : code < 0 || code == ' ' ? Theme.KEY_SPECIAL : Theme.KEY_FACE);
                g.fillRoundRect(x1 + 1, y + 1, x2 - x1 - 2, ch - 2, 6, 6);
                String label = code == SHIFT ? SHIFT_LABELS[mode] : labels[r][i];
                g.setColor(sel ? Theme.HIGHLIGHT_TEXT : Theme.FOREGROUND);
                g.drawString(label, (x1 + x2) / 2, y + (ch - fh) / 2, Graphics.TOP | Graphics.HCENTER);
            }
        }
    }

    /* Text box with the wrapped contents and the caret. */
    void paintText(Graphics g, int x, int y, int w, int h, boolean caret) {
        Font f = Theme.FONT;
        int fh = f.getHeight();
        g.setColor(Theme.BACKGROUND);
        g.fillRect(x, y, w, h);
        g.setColor(Theme.BORDER);
        g.drawRect(x + 2, y + 2, w - 5, h - 5);
        String s = buf.displayString();
        int lines = wrap.wrap(s, f, w - 12);
        int visible = Math.max(1, (h - 8) / fh);
        int caretLine = wrap.lineOf(buf.caret);
        int first = caretLine >= visible ? caretLine - visible + 1 : 0;
        if (!caret) {
            first = 0;
        }
        g.setFont(f);
        g.setColor(Theme.FOREGROUND);
        wrap.paint(g, x + 6, y + 4, fh, first, visible, false);
        if (caret && lines > 0) {
            int cx = x + 6 + wrap.xOf(buf.caret);
            int cy = y + 4 + (caretLine - first) * fh;
            g.setColor(Theme.HIGHLIGHT);
            g.fillRect(cx, cy, 2, fh);
        }
    }
}
