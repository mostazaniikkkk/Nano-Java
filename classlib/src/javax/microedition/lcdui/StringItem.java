package javax.microedition.lcdui;

public class StringItem extends Item {
    private String text;
    private final int appearance;
    private Font font;
    private final TextWrap wrap = new TextWrap();

    public StringItem(String label, String text) {
        this(label, text, PLAIN);
    }

    public StringItem(String label, String text, int appearanceMode) {
        super(label);
        if (appearanceMode < PLAIN || appearanceMode > BUTTON) {
            throw new IllegalArgumentException();
        }
        this.text = text;
        appearance = appearanceMode;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
        invalidate0();
    }

    public int getAppearanceMode() {
        return appearance;
    }

    public void setFont(Font font) {
        this.font = font;
        invalidate0();
    }

    public Font getFont() {
        return font != null ? font : Theme.FONT;
    }

    private boolean button() {
        return appearance == BUTTON;
    }

    int measureBody(int w) {
        String t = text;
        if ((t == null || t.length() == 0) && !button()) {
            return 0;
        }
        Font f = getFont();
        int lines = wrap.wrap(t, f, button() ? w - 12 : w);
        return lines * f.getHeight() + (button() ? 8 : 0);
    }

    void paintBody(Graphics g, int w, int h, boolean focused) {
        String t = text;
        Font f = getFont();
        int fh = f.getHeight();
        wrap.wrap(t, f, button() ? w - 12 : w);
        g.setFont(f);
        if (button()) {
            int bw = Math.min(w, Math.max(wrap.maxWidth + 12, 40));
            int x = alignX(w, bw);
            g.setColor(focused ? Theme.HIGHLIGHT : Theme.BUTTON_BG);
            g.fillRoundRect(x, 0, bw, h, 6, 6);
            g.setColor(focused ? Theme.HIGHLIGHT : Theme.BORDER);
            g.drawRoundRect(x, 0, bw - 1, h - 1, 6, 6);
            g.setColor(focused ? Theme.HIGHLIGHT_TEXT : Theme.FOREGROUND);
            wrap.paint(g, x + bw / 2, 4, fh, 0, wrap.count, true);
            return;
        }
        if (t == null) {
            return;
        }
        boolean link = appearance == HYPERLINK;
        g.setColor(link ? Theme.LINK : Theme.FOREGROUND);
        wrap.paint(g, 0, 0, fh);
        if (link) {
            int base = f.getBaselinePosition() + 1;
            for (int i = 0; i < wrap.count; i++) {
                if (wrap.widths[i] > 0) {
                    g.drawLine(0, i * fh + base, wrap.widths[i] - 1, i * fh + base);
                }
            }
        }
    }
}
