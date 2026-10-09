package javax.microedition.lcdui;

/* Elements and selection state shared by List and ChoiceGroup, plus the
 * painting of one element row. All rows have the same height. */
final class ChoiceModel {
    private static final int BOX = 11;

    final int type;
    private String[] strings = new String[4];
    private Image[] images = new Image[4];
    private Font[] fonts = new Font[4];
    private boolean[] selected = new boolean[4];
    private int size;
    private int fitPolicy;
    private int rowHeight = -1;

    ChoiceModel(int type) {
        this.type = type;
    }

    private boolean exclusive() {
        return type != Choice.MULTIPLE;
    }

    int size() {
        return size;
    }

    private void check(int i) {
        if (i < 0 || i >= size) {
            throw new IndexOutOfBoundsException();
        }
    }

    String getString(int i) {
        check(i);
        return strings[i];
    }

    Image getImage(int i) {
        check(i);
        return images[i];
    }

    int append(String s, Image img) {
        insert(size, s, img);
        return size - 1;
    }

    void insert(int i, String s, Image img) {
        if (i < 0 || i > size) {
            throw new IndexOutOfBoundsException();
        }
        if (s == null) {
            throw new NullPointerException();
        }
        if (size == strings.length) {
            int n = size * 2;
            String[] s2 = new String[n];
            Image[] i2 = new Image[n];
            Font[] f2 = new Font[n];
            boolean[] b2 = new boolean[n];
            System.arraycopy(strings, 0, s2, 0, size);
            System.arraycopy(images, 0, i2, 0, size);
            System.arraycopy(fonts, 0, f2, 0, size);
            System.arraycopy(selected, 0, b2, 0, size);
            strings = s2;
            images = i2;
            fonts = f2;
            selected = b2;
        }
        int tail = size - i;
        System.arraycopy(strings, i, strings, i + 1, tail);
        System.arraycopy(images, i, images, i + 1, tail);
        System.arraycopy(fonts, i, fonts, i + 1, tail);
        System.arraycopy(selected, i, selected, i + 1, tail);
        strings[i] = s;
        images[i] = img;
        fonts[i] = null;
        selected[i] = false;
        size++;
        if (size == 1 && exclusive()) {
            selected[0] = true;
        }
        rowHeight = -1;
    }

    void delete(int i) {
        check(i);
        boolean wasSelected = selected[i];
        int tail = size - i - 1;
        System.arraycopy(strings, i + 1, strings, i, tail);
        System.arraycopy(images, i + 1, images, i, tail);
        System.arraycopy(fonts, i + 1, fonts, i, tail);
        System.arraycopy(selected, i + 1, selected, i, tail);
        size--;
        strings[size] = null;
        images[size] = null;
        fonts[size] = null;
        selected[size] = false;
        if (wasSelected && exclusive() && size > 0) {
            selected[Math.min(i, size - 1)] = true;
        }
        rowHeight = -1;
    }

    void deleteAll() {
        for (int i = 0; i < size; i++) {
            strings[i] = null;
            images[i] = null;
            fonts[i] = null;
            selected[i] = false;
        }
        size = 0;
        rowHeight = -1;
    }

    void set(int i, String s, Image img) {
        check(i);
        if (s == null) {
            throw new NullPointerException();
        }
        strings[i] = s;
        images[i] = img;
        rowHeight = -1;
    }

    boolean isSelected(int i) {
        check(i);
        return selected[i];
    }

    int getSelectedIndex() {
        if (!exclusive()) {
            return -1;
        }
        for (int i = 0; i < size; i++) {
            if (selected[i]) {
                return i;
            }
        }
        return -1;
    }

    int getSelectedFlags(boolean[] out) {
        if (out == null) {
            throw new NullPointerException();
        }
        if (out.length < size) {
            throw new IllegalArgumentException();
        }
        int n = 0;
        for (int i = 0; i < out.length; i++) {
            out[i] = i < size && selected[i];
            if (out[i]) {
                n++;
            }
        }
        return n;
    }

    void setSelectedIndex(int i, boolean sel) {
        check(i);
        if (exclusive()) {
            if (!sel) {
                return;
            }
            for (int j = 0; j < size; j++) {
                selected[j] = j == i;
            }
        } else {
            selected[i] = sel;
        }
    }

    void setSelectedFlags(boolean[] flags) {
        if (flags == null) {
            throw new NullPointerException();
        }
        if (flags.length < size) {
            throw new IllegalArgumentException();
        }
        if (exclusive()) {
            int first = -1;
            for (int i = 0; i < size && first < 0; i++) {
                if (flags[i]) {
                    first = i;
                }
            }
            if (size > 0) {
                setSelectedIndex(first < 0 ? 0 : first, true);
            }
        } else {
            for (int i = 0; i < size; i++) {
                selected[i] = flags[i];
            }
        }
    }

    void setFitPolicy(int p) {
        if (p < Choice.TEXT_WRAP_DEFAULT || p > Choice.TEXT_WRAP_OFF) {
            throw new IllegalArgumentException();
        }
        fitPolicy = p;
    }

    int getFitPolicy() {
        return fitPolicy;
    }

    void setFont(int i, Font f) {
        check(i);
        fonts[i] = f;
        rowHeight = -1;
    }

    Font getFont(int i) {
        check(i);
        return fonts[i] != null ? fonts[i] : Theme.FONT;
    }

    int rowHeight() {
        int h = rowHeight;
        if (h < 0) {
            h = Math.max(Theme.FONT.getHeight(), BOX);
            for (int i = 0; i < size; i++) {
                if (fonts[i] != null) {
                    h = Math.max(h, fonts[i].getHeight());
                }
                if (images[i] != null) {
                    h = Math.max(h, images[i].getHeight());
                }
            }
            h += 6;
            rowHeight = h;
        }
        return h;
    }

    /* Paints row i at (x, y), w pixels wide. indicator: draw the radio
     * button / check box of EXCLUSIVE and MULTIPLE choices. */
    void paintRow(Graphics g, int i, int x, int y, int w, boolean focused, boolean indicator) {
        if (i < 0 || i >= size) {
            return;
        }
        int rh = rowHeight();
        if (focused) {
            g.setColor(Theme.HIGHLIGHT);
            g.fillRect(x, y, w, rh);
        }
        int tx = x + 4;
        if (indicator && (type == Choice.EXCLUSIVE || type == Choice.MULTIPLE)) {
            int by = y + (rh - BOX) / 2;
            g.setColor(Theme.BACKGROUND);
            if (type == Choice.EXCLUSIVE) {
                g.fillArc(tx, by, BOX, BOX, 0, 360);
                g.setColor(Theme.BORDER);
                g.drawArc(tx, by, BOX - 1, BOX - 1, 0, 360);
                if (selected[i]) {
                    g.setColor(Theme.FOREGROUND);
                    g.fillArc(tx + 3, by + 3, BOX - 6, BOX - 6, 0, 360);
                }
            } else {
                g.fillRect(tx, by, BOX, BOX);
                g.setColor(Theme.BORDER);
                g.drawRect(tx, by, BOX - 1, BOX - 1);
                if (selected[i]) {
                    g.setColor(Theme.FOREGROUND);
                    for (int d = 0; d < 2; d++) {
                        g.drawLine(tx + 2, by + 5 + d, tx + 4, by + 7 + d);
                        g.drawLine(tx + 4, by + 7 + d, tx + 8, by + 3 + d);
                    }
                }
            }
            tx += BOX + 5;
        }
        Image img = images[i];
        if (img != null) {
            g.drawImage(img, tx, y + rh / 2, Graphics.LEFT | Graphics.VCENTER);
            tx += img.getWidth() + 4;
        }
        Font f = fonts[i] != null ? fonts[i] : Theme.FONT;
        g.setFont(f);
        g.setColor(focused ? Theme.HIGHLIGHT_TEXT : Theme.FOREGROUND);
        g.drawString(strings[i], tx, y + (rh - f.getHeight()) / 2, Graphics.TOP | Graphics.LEFT);
    }
}
