package javax.microedition.lcdui;

/* Colors and metrics shared by the built-in screens. */
final class Theme {
    static final int BACKGROUND = 0xFFFFFF;
    static final int FOREGROUND = 0x000000;
    static final int HIGHLIGHT = 0x2060C0;
    static final int HIGHLIGHT_TEXT = 0xFFFFFF;
    static final int BORDER = 0x808080;
    static final int TITLE_BG = 0x203050;
    static final int TITLE_FG = 0xFFFFFF;
    static final int BAR_BG = 0x203050;
    static final int BAR_FG = 0xFFFFFF;
    static final int FOCUS_BG = 0xDCE6F8;
    static final int FIELD_BG = 0xFFFFFF;
    static final int BUTTON_BG = 0xE4E8F0;
    static final int LINK = 0x1040C0;
    static final int KEY_BG = 0xC8CED8;
    static final int KEY_FACE = 0xFFFFFF;
    static final int KEY_SPECIAL = 0xA8B2C2;

    static final Font FONT = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_MEDIUM);
    static final Font BOLD = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_MEDIUM);

    private Theme() {
    }

    static int titleHeight() {
        return BOLD.getHeight() + 4;
    }

    static int barHeight() {
        return FONT.getHeight() + 4;
    }

    static void drawTitle(Graphics g, String title, int w) {
        int h = titleHeight();
        g.setColor(TITLE_BG);
        g.fillRect(0, 0, w, h);
        if (title != null) {
            g.setColor(TITLE_FG);
            g.setFont(BOLD);
            g.drawString(title, 3, 2, Graphics.TOP | Graphics.LEFT);
        }
    }

    /* Soft key bar along the bottom edge. */
    static void drawSoftKeys(Graphics g, Displayable d, int w, int h) {
        String[] labels = Display.softLabels(d);
        drawSoftKeys(g, labels[0], labels[1], w, h);
    }

    static void drawSoftKeys(Graphics g, String left, String right, int w, int h) {
        int bh = barHeight();
        g.setColor(BAR_BG);
        g.fillRect(0, h - bh, w, bh);
        g.setColor(BAR_FG);
        g.setFont(FONT);
        if (left != null) {
            g.drawString(left, 3, h - bh + 2, Graphics.TOP | Graphics.LEFT);
        }
        if (right != null) {
            g.drawString(right, w - 3, h - bh + 2, Graphics.TOP | Graphics.RIGHT);
        }
    }
}
