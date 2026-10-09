package javax.microedition.lcdui;

/*
 * Word wrapping of a string to a pixel width. The result is cached and
 * only recomputed when the text (by identity), font or width change, so
 * painting code may call wrap() on every frame without allocating.
 */
final class TextWrap {
    private String text;
    private Font font;
    private int width = -1;

    /* Lines: text.substring(starts[i], ends[i]), widths[i] pixels wide. */
    int count;
    int[] starts = new int[4];
    int[] ends = new int[4];
    int[] widths = new int[4];
    int maxWidth;

    /* Wraps s (null means "") and returns the number of lines (at least 1). */
    int wrap(String s, Font f, int w) {
        if (s == null) {
            s = "";
        }
        if (s == text && f == font && w == width) {
            return count;
        }
        text = s;
        font = f;
        width = w;
        count = 0;
        maxWidth = 0;
        int n = s.length();
        int pos = 0;
        while (pos < n) {
            int i = pos;
            int lineW = 0;
            int space = -1;
            int spaceW = 0;
            while (i < n) {
                char c = s.charAt(i);
                if (c == '\n') {
                    break;
                }
                int cw = f.charWidth(c);
                if (lineW + cw > w && i > pos) {
                    break;
                }
                if (c == ' ') {
                    space = i;
                    spaceW = lineW;
                }
                lineW += cw;
                i++;
            }
            if (i >= n || s.charAt(i) == '\n') {
                add(pos, i, lineW);
                pos = i + 1;
            } else if (s.charAt(i) == ' ') {
                add(pos, i, lineW);
                pos = i + 1;
            } else if (space > pos) {
                add(pos, space, spaceW);
                pos = space + 1;
            } else {
                add(pos, i, lineW);
                pos = i;
            }
        }
        if (count == 0 || s.charAt(n - 1) == '\n') {
            add(n, n, 0);
        }
        return count;
    }

    private void add(int start, int end, int w) {
        if (count == starts.length) {
            starts = grow(starts);
            ends = grow(ends);
            widths = grow(widths);
        }
        starts[count] = start;
        ends[count] = end;
        widths[count] = w;
        count++;
        if (w > maxWidth) {
            maxWidth = w;
        }
    }

    private static int[] grow(int[] a) {
        int[] b = new int[a.length * 2];
        System.arraycopy(a, 0, b, 0, a.length);
        return b;
    }

    /* Index of the line holding character offset pos (a caret position). */
    int lineOf(int pos) {
        for (int i = count - 1; i > 0; i--) {
            if (pos >= starts[i]) {
                return i;
            }
        }
        return 0;
    }

    /* Pixel x of character offset pos within its line. */
    int xOf(int pos) {
        int line = lineOf(pos);
        int end = Math.min(pos, ends[line]);
        int start = starts[line];
        return end > start ? font.substringWidth(text, start, end - start) : 0;
    }

    /* Draws lines [first, first + max) with their tops lineH apart; x is
     * the left edge, or the center when hcenter is set. */
    void paint(Graphics g, int x, int y, int lineH, int first, int max, boolean hcenter) {
        int clipTop = g.getClipY();
        int clipBottom = clipTop + g.getClipHeight();
        int last = Math.min(count, first + max);
        int anchor = Graphics.TOP | (hcenter ? Graphics.HCENTER : Graphics.LEFT);
        for (int i = first; i < last; i++) {
            int ly = y + (i - first) * lineH;
            if (ly >= clipBottom) {
                break;
            }
            if (ly + lineH > clipTop && ends[i] > starts[i]) {
                g.drawSubstring(text, starts[i], ends[i] - starts[i], x, ly, anchor);
            }
        }
    }

    void paint(Graphics g, int x, int y, int lineH) {
        paint(g, x, y, lineH, 0, count, false);
    }
}
